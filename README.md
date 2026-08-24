# 🔗 Social Network Microservices Platform

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.x-blue.svg)](https://spring.io/projects/spring-cloud)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-red.svg)]()
[![Neo4j](https://img.shields.io/badge/Neo4j-GraphDB-008cc1.svg)](https://neo4j.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Streaming-231f20.svg)](https://kafka.apache.org/)
[![Eureka](https://img.shields.io/badge/Eureka%20Server-Discovery-brightgreen.svg)](https://spring.io/projects/spring-cloud-netflix)
[![Zipkin](https://img.shields.io/badge/Zipkin-Distributed%20Tracing-FF6600.svg)](https://zipkin.io/)
[![Micrometer](https://img.shields.io/badge/Micrometer-Observability-000000.svg)](https://micrometer.io/)
[![Resilience4J](https://img.shields.io/badge/Resilience4J-Fault%20Tolerance-6DB33F.svg)](https://resilience4j.readme.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36.svg)](https://maven.apache.org/)
[![Gateway Auth](https://img.shields.io/badge/Authentication-JWT%20Gateway-green.svg)]()

> "Your network is your net worth — but only if the system behind it can actually route a request."

A **LinkedIn-inspired Social Networking Platform** built as a distributed microservices system with **Spring Boot** and the **Spring Cloud** ecosystem. It models real social-graph problems — first-degree and second-degree connections, posts, real-time notifications, and media uploads — on top of a **Neo4j graph database** and an **event-driven Kafka backbone**, all fronted by a single authenticated API Gateway, with **Zipkin + Micrometer** wired in for full distributed tracing and **Resilience4J** wired in so individual service slowdowns don't cascade across the system.

---

## 🏗️ Architecture Overview

<!-- Replace with your architecture diagram screenshot -->
<img width="1660" height="947" alt="ChatGPT Image Aug 2, 2026, 12_31_21 AM" src="https://github.com/user-attachments/assets/408fc6c9-cc86-4e59-82ef-a1034f47cafb" />

The system follows a **distributed microservices architecture** — every client request enters through the API Gateway, gets authenticated once, and is routed to the appropriate downstream service via Eureka service discovery.

---

## 🔐 Authentication Flow

The platform uses **stateless JWT authentication enforced entirely at the API Gateway**, so no downstream service ever has to validate a token itself.

### 1. Signup / Login
A user signs up or logs in through the public `user-service` route (the only route the gateway does *not* protect). On success, the server issues a signed **JWT access token** back to the client.

### 2. Gateway-Level Authentication Filter
Every subsequent request — to any service except `user-service` — passes through a custom **Authentication Gateway Filter** before it's allowed downstream:

1. Read the `Authorization` header from the incoming request.
2. Extract the `Bearer` token from that header.
3. Validate the token's signature and expiry.
4. If invalid → immediately return **`401 Unauthorized`** and stop the request.
5. If valid → extract the `userId` from the token claims and **mutate the outgoing request**, attaching it as a new header: `X-User-Id: <userId>`.

This filter is wired into `application.yml` and attached to every service route except the public auth endpoints — the user service stays open since it's what issues the token in the first place.

### 3. Reading the User Identity Downstream
Once a request reaches a downstream service (posts, connections, notifications, uploader), it already carries a trusted `X-User-Id` header — no re-authentication needed. Services can read it two ways:

- **Quick way** — `@RequestHeader("X-User-Id") Long id` directly in the controller.
- **Scalable way** — a `ContextHolder` backed by `ThreadLocal`, which holds the userId for the entire request lifecycle and is accessible anywhere in the call stack without threading it through every method signature.

### 4. Request Interceptor Lifecycle
Each service registers a `HandlerInterceptor` with three hooks:

| Hook | When it runs | Purpose |
|------|---------------|---------|
| `preHandle` | Before the controller | Reads `X-User-Id` from the header and stores it in the `ContextHolder` |
| `postHandle` | After the controller returns | Skipped if an exception was thrown |
| `afterCompletion` | **Always**, even on exception | Clears the `ThreadLocal` so it never leaks into the next request on a reused thread |

The interceptor does nothing until it's explicitly registered via `WebMvcConfigurer#addInterceptors(...)` — once registered, Spring MVC runs it automatically on every incoming request.

**Why this matters:** the gateway is the single choke point for authentication — it validates the JWT once, so downstream services only ever trust the `X-User-Id` header. Because thread pools reuse threads across requests, skipping `afterCompletion` would leak one user's identity into another user's request on the same thread — which is exactly what this pattern is designed to prevent.

---

## 📝 Post Creation Flow — A Look Inside

Creating a post isn't a single-service write — it fans out across the system. Here's the actual flow from `PostService`:

```java
@Override
public PostDto createPost(PostCreateRequestDto requestDto) {
    Long userId = AuthContextHolder.getCurrentUserId(); // never trust userId from the request body

    Boolean exists = userServiceClient.userExists(userId);
    if (exists == null || !exists) {
        throw new BadRequestException("User account not found");
    }

    List<String> imageurls = uploadImagesIfPresent(requestDto.getFiles());

    Post post = new Post();
    post.setUserId(userId);
    post.setContent(requestDto.getContent());
    post.setImageUrls(imageurls); // adjust to however you actually populate this

    List<PersonDto> personDtoList = connectionsServiceClient.getFirstDegreeConnections();

    post = postRepository.save(post);

    for(PersonDto person: personDtoList) { // send notification to each connection
        PostCreated postCreated = PostCreated.builder()
                .postId(post.getId())
                .content(post.getContent())
                .userId(person.getUserId())
                .ownerUserId(userId)
                .build();
        postCreatedKafkaTemplate.send("post_created_topic", postCreated);
    }
    return enrich(post);
}
```

**What actually happens under the hood, per request:**

1. `AuthContextHolder` — reads the trusted `userId` set by the gateway's auth filter (never trusts a userId from the request body).
2. `user-service` (via OpenFeign) — verifies the account actually exists before writing anything.
3. `uploader-service` (via OpenFeign, only when the post includes images) — uploads media before the post is persisted.
4. `connections-service` (via OpenFeign) — fetches the first-degree connection list for that user.
5. `posts-service` — persists the post.
6. `notification-service` (via Kafka, async) — one `post_created_topic` event is published **per connection**, fanning out the notification without blocking the response.

This is exactly the kind of call chain that's invisible from the outside and painful to debug across five services with plain logs — which is why tracing was the next thing to build, and exactly why it's also the riskiest call in the system from a resilience standpoint (see below).

---

## 🛡️ Resilience4J — Failing Gracefully

Tracing showed *where* a request goes. Resilience4J is about making sure a slow or failing dependency along that path doesn't take the rest of the system down with it. Rather than wrapping every call in the same blanket policy, each endpoint gets the pattern that actually matches its failure mode:

### 1. Circuit Breaker on Create Post
`createPost` is the riskiest call in the system, because of what it triggers internally: a call out to **connections-service** to build the notification fan-out list, and — whenever the post includes an image — a call out to **uploader-service** before the post is even saved. If either of those dependencies gets slow or goes down, that shouldn't be able to hang or take down Create Post along with it. A **Circuit Breaker** watches the failure/slow-call rate on that chain and trips open once it crosses the configured threshold, short-circuiting further calls immediately (instead of letting them queue up and time out) and periodically probing the dependency again to see if it's recovered.

### 2. Retry on Get Post
Get Post is a simple downstream read — the likely failure mode is a short, transient blip, not a structural outage. A **Retry** policy re-attempts the call a configured number of times (with backoff) before surfacing an error, so a one-off hiccup doesn't turn into a failed request for the user.

### 3. Retry on Get Notifications
Same reasoning applied to `notification-service`: fetching notifications is a lightweight read, so a **Retry** policy is the right tool — no need for the overhead of a circuit breaker on a call this low-risk.

| Endpoint | Pattern | Why |
|---|---|---|
| `POST /api/v1/posts/create` | Circuit Breaker | Fans out to connections-service (always) and uploader-service (if the post has images) — cascading-failure risk |
| `GET /api/v1/posts/{id}` | Retry | Simple read, transient-failure risk only |
| `GET /api/v1/notifications/{userId}` | Retry | Simple read, transient-failure risk only |

---

## ✨ Features

### Core Microservices Features
- 🔍 **Service Discovery** with Netflix Eureka
- 🌐 **API Gateway** with Spring Cloud Gateway
- 🔄 **Inter-service Communication** using OpenFeign
- ⚖️ **Load Balancing** across service instances

### Social Graph Features
- 🕸️ **Neo4j Graph Database** for modeling the social connection graph
- 🤝 **First-Degree Connections** — direct connections lookup
- 🌐 **Second-Degree Connections** — friends-of-friends traversal via graph queries
- 📝 **Post Creation & Retrieval**
- 👤 **Rich User Profiles**

### Event-Driven Features
- 📨 **Apache Kafka** for asynchronous, event-driven notifications
- 🔔 **Real-time Notification Delivery** when another user interacts with your content or profile
- 📊 **Kafbat UI** for inspecting Kafka topics, partitions, and consumer groups

### Observability Features
- 📈 **Zipkin** — distributed tracing across every service in a request's call chain (gateway → posts → user/connections/notification)
- 📐 **Micrometer** — instrumentation layer that captures and exports trace/span data from each Spring Boot service to Zipkin
- 🧭 **Dependency Graph View** — visualizes which services call which, generated straight from real trace data

### Resilience Features
- 🛡️ **Circuit Breaker** on Create Post — protects against cascading failures from connections-service / uploader-service
- 🔁 **Retry** on Get Post — absorbs transient read failures with backoff
- 🔁 **Retry** on Get Notifications — absorbs transient read failures with backoff

### Media & Infrastructure
- 📤 **Uploader Service** for media/file handling
- 🔐 **JWT Authentication** enforced at the Gateway
- 📝 **Centralized Logging** with the ELK Stack *(coming soon)*
- 🚦 **Rate Limiter + Redis** at the API Gateway *(coming soon)*

---

## 🛠️ Technologies Used

| Category | Technology | Purpose |
|----------|------------|---------|
| **Framework** | Spring Boot 3.x | Microservice foundation |
| **Cloud** | Spring Cloud 2023.x | Microservice patterns |
| **Service Discovery** | Netflix Eureka | Service registration & discovery |
| **API Gateway** | Spring Cloud Gateway | Request routing, filtering & auth |
| **Communication** | OpenFeign | Declarative REST clients |
| **Graph Database** | Neo4j | Social connection graph (1st/2nd degree) |
| **Event Streaming** | Apache Kafka | Async notification delivery |
| **Kafka Monitoring** | Kafbat UI | Topic/consumer inspection |
| **Security** | Spring Security + JWT | Gateway-level authentication |
| **Distributed Tracing** | Zipkin | Request tracing across services |
| **Metrics/Tracing Bridge** | Micrometer | Instruments each service and ships spans to Zipkin |
| **Resilience** | Resilience4J | Circuit Breaker (Create Post) + Retry (Get Post, Get Notifications) |
| **Rate Limiting** | Redis *(planned)* | Gateway-level rate limiting |
| **Logging** | ELK Stack *(planned)* | Centralized logging |
| **Build Tool** | Maven | Dependency management |

---

## 🚀 Getting Started

### Prerequisites
- ☕ **Java 17+**
- 📦 **Maven 3.8+**
- 🕸️ **Neo4j** instance running
- 📨 **Kafka** broker running
- 📈 **Zipkin** server running
- 🐳 *(Optional)* ELK Stack

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/ARONAGENT/<your-repo-name>.git
   cd social-network-microservices
   ```

2. **Start Infrastructure Services**
   ```bash
   # Start Eureka Server
   cd eureka-service
   mvn spring-boot:run

   # Start API Gateway
   cd ../api-gateway
   mvn spring-boot:run
   ```

3. **Start Business Services**
   ```bash
   # Start User Service
   cd user-service
   mvn spring-boot:run

   # Start Posts Service
   cd ../posts-service
   mvn spring-boot:run

   # Start Connections Service
   cd ../connections-service
   mvn spring-boot:run

   # Start Notification Service
   cd ../notification-service
   mvn spring-boot:run

   # Start Uploader Service
   cd ../uploader-service
   mvn spring-boot:run
   ```

4. **Start Monitoring**
   ```bash
   # Start Zipkin
   java -jar zipkin-server-3.5.1-exec.jar

   # Start ELK Stack (coming soon)
   for Elastic  -> run elastic.bat
   for Kibana   -> run kibana.bat
   for Logstash -> run logstash -f logstash.conf
   ```

---

## 📦 Services

| Service | Port | Purpose |
|---------|------|---------|
| **API Gateway** | `1010` | Single entry point, routing & JWT auth filter |
| **User Service** | `9020` | Signup/login, profiles, identity |
| **Posts Service** | `9010` | Create & fetch posts (Circuit Breaker + Retry) |
| **Connections Service** | `9030` | 1st/2nd-degree connection graph (Neo4j) |
| **Notification Service** | `9040` | Kafka-driven real-time notifications (Retry on fetch) |
| **Uploader Service** | `9050` | Media/file upload handling |

### Eureka Dashboard — Registered Instances

| Application | AMIs | Availability Zones | Status |
|---|---|---|---|
| API-GATEWAY | n/a (1) | (1) | UP (1) — `api-gateway:1010` |
| CONNECTIONS-SERVICE | n/a (1) | (1) | UP (1) — `connections-service:9030` |
| NOTIFICATION-SERVICE | n/a (1) | (1) | UP (1) — `notification-service:9040` |
| POSTS-SERVICE | n/a (1) | (1) | UP (1) — `posts-service:9010` |
| UPLOADER-SERVICE | n/a (1) | (1) | UP (1) — `uploader-service:9050` |
| USER-SERVICE | n/a (1) | (1) | UP (1) — `user-service:9020` |

---

## 📚 API Endpoints (Sample)

#### User Service
```http
POST /api/v1/users/signup
POST /api/v1/users/login
GET  /api/v1/users/profile/{id}
```

#### Posts Service
```http
POST /api/v1/posts/create
GET  /api/v1/posts/{id}
GET  /api/v1/posts/all
```

#### Connections Service
```http
GET /api/v1/connections/first-degree/{userId}
GET /api/v1/connections/second-degree/{userId}
```

#### Notification Service
```http
GET /api/v1/notifications/{userId}
```

---

## 📸 Screenshots

**1. User Login**

<img width="1372" height="902" alt="1 Login" src="https://github.com/user-attachments/assets/d5a7898e-20ad-4638-b054-5b39a036342a" />

<br><br>

**2. Get Detailed Profile**

<img width="1431" height="956" alt="2 get My Profile" src="https://github.com/user-attachments/assets/689b93ee-ca05-490f-86a4-7544fc5ecc34" />

<br><br>

**3. Get Post**

<img width="1433" height="956" alt="3 getPost" src="https://github.com/user-attachments/assets/967da271-9ced-4427-97c6-e243e04ad00f" />

<br><br>

**4. Create Post**

<img width="1440" height="961" alt="4 post_created" src="https://github.com/user-attachments/assets/a3c363c1-30e9-4fd1-8747-7d65a451111f" />

<br><br>

**5. Get Connections — First-Degree**

<img width="1445" height="927" alt="5 Get First_Degree Connnections" src="https://github.com/user-attachments/assets/7f71377c-fb82-44fd-be0d-debb0bb905a7" />

<br><br>

**6. Get Connections — Second-Degree**

<img width="1438" height="906" alt="6 Get Second Degree Connections" src="https://github.com/user-attachments/assets/9c81c36c-3876-4a00-a196-520b6d7a31cc" />

<br><br>

**7. Another User Logs In to See Notification**

<img width="1432" height="870" alt="8 Another user is Login" src="https://github.com/user-attachments/assets/8b023d66-9623-4ea5-82f5-77efab51e54d" />

<br><br>

**8. Get Notifications**

<img width="1447" height="982" alt="9 get Notifications" src="https://github.com/user-attachments/assets/4de0be36-e5e2-410b-81b2-c4a60747d927" />

<br><br>

**9. Neo4j Graph DB — All Data**

<img width="1918" height="1022" alt="7 Internally neo4j connection graph" src="https://github.com/user-attachments/assets/8a5bdccb-e66f-4090-b486-f4c4d606e592" />

<br><br>

**10. Kafbat UI**

<img width="1917" height="1022" alt="10 kafbat UI working fine and get all Topics and events" src="https://github.com/user-attachments/assets/2f63ba5f-7229-4960-8ed9-bef438452dcd" />

<br><br>

**11. Eureka Discovery Client**

<img width="1918" height="1022" alt="11 Eureks Service " src="https://github.com/user-attachments/assets/565f6351-681d-4719-abd2-4a030174408a" />

<br><br>

**12. Zipkin — Trace for "Create Post" Request**

<img width="1918" height="1012" alt="13   Zipkin Trace - When Post Create Api Hit" src="https://github.com/user-attachments/assets/0b15376d-4dda-4313-aaba-11918ea3e6df" />

<br><br>

**13. Zipkin — Dependency Graph for "Post Created"**

<img width="1918" height="1017" alt="14  dependencies Graph Of Zipkin" src="https://github.com/user-attachments/assets/de9220a8-3a14-49a7-9e0a-e0f41cf08cca" />

<br><br>

**14. Zipkin — API Gateway Traces & Internal Posts-Service Traces**

<table>
  <tr>
    <td align="center">
      <img width="380" alt="Api-gateway Detail Traces" src="https://github.com/user-attachments/assets/2d826ae8-b620-4899-8bb6-474a4292f640" /><br>
      <sub><b>API Gateway Traces</b></sub>
    </td>
    <td align="center">
      <img width="380" alt="Post Service Traces When Post is Created" src="https://github.com/user-attachments/assets/aaa54e0c-c2ea-49fa-9a0e-ec6014afe193" /><br>
      <sub><b>Posts-Service Internal Traces</b></sub>
    </td>
  </tr>
</table>

<br><br>

**15. Resilience4J — Circuit Breaker on Create Post**
*(shows the breaker tripping/half-open behavior when connections-service or uploader-service is unavailable)*

<!-- Add screenshot: circuit breaker state/metrics for the Create Post endpoint -->
<img width="1385" height="817" alt="17 Circuit Breaker for Post" src="https://github.com/user-attachments/assets/a9665271-af33-4716-ab3a-ef22a16bbdf0" />

<br><br>

**16. Resilience4J — Retry on Get Post**
*(shows retry attempts/backoff kicking in on a transient failure)*

<!-- Add screenshot: retry attempts for the Get Post endpoint -->
<img width="1240" height="533" alt="18 Retries for Post Services" src="https://github.com/user-attachments/assets/b0403a42-7f16-47b3-ad5c-97fa5186710e" />


<br><br>

**17. Resilience4J — Retry on Get Notifications**
*(shows retry attempts/backoff kicking in on a transient failure)*

<!-- Add screenshot: retry attempts for the Get Notifications endpoint -->
<img width="1390" height="555" alt="19 Retries for Notification Service " src="https://github.com/user-attachments/assets/710afc16-5481-4110-8b90-3460f520b6a0" />


<br><br>

**18. ELK Stack — Kibana / Elasticsearch / Logstash** *(coming soon)*

![ELK Stack](./screenshots/16-elk-stack.png)

<br><br>

**19. Frontend UI**

<img width="1312" height="872" alt="12 Frontend view" src="https://github.com/user-attachments/assets/99b93ffb-dc5b-43d3-a405-2ca7366c6be5" />

<br><br>

---

## 🎯 Learning Outcomes

This project demonstrates:
- ✅ **Microservices Architecture** best practices
- ✅ **Graph modeling** with Neo4j for social connections
- ✅ **Event-driven design** with Apache Kafka
- ✅ **Gateway-centralized authentication** with JWT
- ✅ **Service discovery & routing** with Eureka + Spring Cloud Gateway
- ✅ **Distributed tracing** with Zipkin + Micrometer across a real multi-service call chain
- ✅ **Fault tolerance** with Resilience4J — matching Circuit Breaker vs. Retry to each endpoint's actual failure mode
- ✅ **Distributed system** challenges and solutions
- ☑️ Rate limiting with Redis at the API Gateway *(in progress)*
- ☑️ Centralized logging with ELK *(in progress)*

## 🚀 Future Enhancements

- [ ] Rate Limiter + Redis at the API Gateway level
- [ ] ELK stack centralized logging
- [ ] Kubernetes deployment
- [ ] Dockerize all services
- [ ] Database-per-service isolation for non-graph data
- [ ] API versioning strategy
- [ ] Automated testing pipeline

---

## 💬 A Few Thoughts on Building This

> "A network isn't just nodes and edges — it's people. Get the graph right, and the product almost writes itself."

> "Every notification is a promise: something happened, and someone should know. Kafka just makes sure that promise gets kept, even under load."

> "Authenticate once at the door, trust everywhere inside — that's the whole philosophy of a gateway."

> "You don't really know what your microservices are doing to each other until you can see the trace. Zipkin turned five black boxes into one readable timeline."

> "Tracing tells you where a request went. Resilience4J decides what happens when part of that path stops cooperating."

---

## 👨‍💻 Author

**Rohan Uke**
Backend Developer | Java & Spring Boot Enthusiast

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/aronagent/)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/ARONAGENT)
[![Portfolio](https://img.shields.io/badge/Portfolio-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://portfolio-aronagent.onrender.com/)

---

## ⭐ Show your support

Give a ⭐️ if this project helped you understand event-driven, graph-backed, resilient microservices architecture!

## 📞 Support

If you have any questions or need help with the project, please:
1. Check the **Issues** page
2. Create a new issue if your question isn't already answered
3. Contact me via [LinkedIn](https://www.linkedin.com/in/aronagent/) or check out my [Portfolio](https://portfolio-aronagent.onrender.com/)

---

*Built with ❤️ using Spring Boot, Spring Cloud, Neo4j, Apache Kafka, Zipkin & Resilience4J*
