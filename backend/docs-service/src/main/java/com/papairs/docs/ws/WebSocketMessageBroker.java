package com.papairs.docs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.OT.AppliedOp;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles WebSocket communication for document collaboration.
 * Manages message serialization and broadcasting to multiple clients.
 */
@Component
public class WebSocketMessageBroker {
    private static final Logger logger = Logger.getLogger(WebSocketMessageBroker.class.getName());
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a message to a specific WebSocket session.
     */
    public void sendToSession(WebSocketSession session, AppliedOp message) {
        if (!session.isOpen()) {
            logger.warning("Attempted to send message to closed session");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send message to session", e);
        }
    }

    /**
     * Broadcasts a message to all active sessions in a document.
     */
    public void broadcastToDocument(DocumentSession document, AppliedOp message) {
        Set<WebSocketSession> sessions = document.getActiveSessions();
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            
            // Use a copy of sessions to avoid concurrent modification
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, 
                            "Failed to send message to session: " + session.getId(), e);
                    }
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to serialize message for broadcast", e);
        }
    }

    /**
     * Broadcasts a message to all sessions except the sender.
     */
    public void broadcastToOthers(DocumentSession document, AppliedOp message, WebSocketSession sender) {
        Set<WebSocketSession> sessions = document.getActiveSessions();
        if (sessions.size() <= 1) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            
            sessions.stream()
                .filter(session -> !session.equals(sender) && session.isOpen())
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, 
                            "Failed to send message to session: " + session.getId(), e);
                    }
                });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to serialize message for broadcast", e);
        }
    }
}