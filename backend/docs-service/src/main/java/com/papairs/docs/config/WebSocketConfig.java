package com.papairs.docs.config;

import com.papairs.docs.ws.DocWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DocWebSocketHandler handler;

    public WebSocketConfig(DocWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(handler, "/ws/doc")
            .setAllowedOriginPatterns("*") // Allow all origins for development
            .withSockJS() // Add SockJS fallback support
            .setStreamBytesLimit(512 * 1024) // Increase message size limit
            .setHttpMessageCacheSize(1000)
            .setHeartbeatTime(25000);
    }
}
