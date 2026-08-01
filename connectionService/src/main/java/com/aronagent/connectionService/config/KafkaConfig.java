package com.aronagent.connectionService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic sendConnectionRequest() {
        return new NewTopic("send_connection_request", 3, (short) 1);
    }

    @Bean
    public NewTopic accept_connection_request() {
        return new NewTopic("accept_connection_request", 3, (short) 1);
    }
}
