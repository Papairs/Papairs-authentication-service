package com.papairs.docs.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        // Increase max string length to 100MB (default is 20MB)
        StreamReadConstraints streamReadConstraints = StreamReadConstraints
                .builder()
                .maxStringLength(100_000_000)
                .build();
                
        objectMapper.getFactory().setStreamReadConstraints(streamReadConstraints);
        
        return objectMapper;
    }
}
