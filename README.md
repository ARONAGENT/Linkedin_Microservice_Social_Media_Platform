# 🔗 Social Network Microservices Platform

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.x-blue.svg)](https://spring.io/projects/spring-cloud)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-red.svg)]()
[![Neo4j](https://img.shields.io/badge/Neo4j-GraphDB-008cc1.svg)](https://neo4j.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Streaming-231f20.svg)](https://kafka.apache.org/)
[![Eureka](https://img.shields.io/badge/Eureka%20Server-Discovery-brightgreen.svg)](https://spring.io/projects/spring-cloud-netflix)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36.svg)](https://maven.apache.org/)
[![Gateway Auth](https://img.shields.io/badge/Authentication-JWT%20Gateway-green.svg)]()

> "Your network is your net worth — but only if the system behind it can actually route a request."

A **LinkedIn-inspired Social Networking Platform** built as a distributed microservices system with **Spring Boot** and the **Spring Cloud** ecosystem. It models real social-graph problems — first-degree and second-degree connections, posts, real-time notifications, and media uploads — on top of a **Neo4j graph database** and an **event-driven Kafka backbone**, all fronted by a single authenticated API Gateway.

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

## ✨ Features

### Core Microservices Features
- 🔍 **Service Discovery** with Netflix Eureka
- 🌐 **API Gateway** with Spring Cloud Gateway
- 🔄 **Inter-service Communication** using OpenFeign
- 🛡️ **Circuit Breaker Pattern** with Resilience4J
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

### Media & Infrastructure
- 📤 **Uploader Service** for media/file handling
- 🔐 **JWT Authentication** enforced at the Gateway
- 📈 **Distributed Tracing** with Zipkin *(coming soon)*
- 📝 **Centralized Logging** with the ELK Stack *(coming soon)*

---

## 🛠️ Technologies Used

| Category | Technology | Purpose |
|----------|------------|---------|
| **Framework** | Spring Boot 3.x | Microservice foundation |
| **Cloud** | Spring Cloud 2023.x | Microservice patterns |
| **Service Discovery** | Netflix Eureka | Service registration & discovery |
| **API Gateway** | Spring Cloud Gateway | Request routing, filtering & auth |
| **Communication** | OpenFeign | Declarative REST clients |
| **Resilience** | Resilience4J | Circuit breaker, retry |
| **Graph Database** | Neo4j | Social connection graph (1st/2nd degree) |
| **Event Streaming** | Apache Kafka | Async notification delivery |
| **Kafka Monitoring** | Kafbat UI | Topic/consumer inspection |
| **Security** | Spring Security + JWT | Gateway-level authentication |
| **Monitoring** | Zipkin *(planned)* | Distributed tracing |
| **Logging** | ELK Stack *(planned)* | Centralized logging |
| **Build Tool** | Maven | Dependency management |

---

## 🚀 Getting Started

### Prerequisites
- ☕ **Java 17+**
- 📦 **Maven 3.8+**
- 🕸️ **Neo4j** instance running
- 📨 **Kafka** broker running
- 🐳 *(Optional)* ELK Stack & Zipkin Server

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

4. **Start Monitoring (Optional)**
   ```bash
   # Start Zipkin
   java -jar zipkin-server-3.5.1-exec.jar

   # Start ELK Stack
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
| **Posts Service** | `9010` | Create & fetch posts |
| **Connections Service** | `9030` | 1st/2nd-degree connection graph (Neo4j) |
| **Notification Service** | `9040` | Kafka-driven real-time notifications |
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

**12. Zipkin Tracing** *(coming soon)*

![Zipkin](./screenshots/13-zipkin.png)
<br><br>

**13. ELK Stack — Kibana / Elasticsearch / Logstash** *(coming soon)*

![ELK Stack](./screenshots/14-elk-stack.png)
<br><br>

**14. Frontend UI**

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
- ✅ **Distributed system** challenges and solutions
- ☑️ Observability with Zipkin & ELK *(in progress)*

## 🚀 Future Enhancements

- [ ] Zipkin distributed tracing integration
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

---

## 👨‍💻 Author

**Rohan Uke**
Backend Developer | Java & Spring Boot Enthusiast

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/rohan-uke)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/ARONAGENT)

---

## ⭐ Show your support

Give a ⭐️ if this project helped you understand event-driven, graph-backed microservices architecture!

## 📞 Support

If you have any questions or need help with the project, please:
1. Check the **Issues** page
2. Create a new issue if your question isn't already answered
3. Contact me via [LinkedIn](https://linkedin.com/in/rohan-uke)

---

*Built with ❤️ using Spring Boot, Spring Cloud, Neo4j & Apache Kafka*
