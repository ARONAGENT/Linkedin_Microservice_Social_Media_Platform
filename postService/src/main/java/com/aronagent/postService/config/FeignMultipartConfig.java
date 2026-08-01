package com.aronagent.postService.config;

import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

/**
 * NOTE: deliberately NOT annotated with @Configuration.
 * Feign client configuration classes must stay outside normal component
 * scanning (or be excluded from it) so Spring doesn't treat this as a
 * global @Configuration and accidentally apply it to every bean in the
 * app context. It's wired in explicitly via @FeignClient(configuration = ...).
 */
@Slf4j
public class FeignMultipartConfig {

    @Bean
    public Encoder feignFormEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        // Wraps the default Spring encoder so Feign can properly serialize
        // MultipartFile / List<MultipartFile> as real multipart/form-data
        // parts when forwarding a request to another service.
        log.warn(">>> feignFormEncoder bean created for UploaderServiceClient <<<");
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    // TEMPORARY DEBUG AID — logs the full outgoing request/response for this
    // client (headers + body) so we can see exactly what multipart body
    // Feign actually sends. Remove once the upload issue is confirmed fixed.
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}