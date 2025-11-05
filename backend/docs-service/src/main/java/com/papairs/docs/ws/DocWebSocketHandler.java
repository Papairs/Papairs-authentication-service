package com.papairs.docs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.Message;
import com.papairs.docs.model.OT.AppliedOp;
import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.util.OtTransform;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DocWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    private static class DocState {
        String content = "";
        int version = 0;
        final List<Op> history = new ArrayList<>();
        final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    }

    private final Map<String, DocState> docs = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // no-op
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Message msg = mapper.readValue(message.getPayload(), Message.class);
        String docId = (msg.docId == null || msg.docId.isBlank()) ? "default" : msg.docId;
        DocState doc = docs.computeIfAbsent(docId, k -> new DocState());

        if ("join".equals(msg.action)) {
            doc.sessions.add(session);
            AppliedOp snap = new AppliedOp();
            snap.type = "snapshot";
            snap.version = doc.version;
            snap.content = doc.content;
            session.sendMessage(new TextMessage(mapper.writeValueAsString(snap)));
            return;
        }

        if ("op".equals(msg.action) && msg.op != null) {
            Op incoming = msg.op;

            // Transform against all ops since client's base version
            for (int i = incoming.baseVersion; i < doc.version; i++) {
                incoming = OtTransform.transform(incoming, doc.history.get(i));
            }

            // Apply
            if ("insert".equals(incoming.type)) {
                InsertOp ins = (InsertOp) incoming;
                int p = clamp(ins.pos, 0, doc.content.length());
                doc.content = doc.content.substring(0, p) + ins.text + doc.content.substring(p);
            } else if ("delete".equals(incoming.type)) {
                DeleteOp del = (DeleteOp) incoming;
                int start = clamp(del.pos, 0, doc.content.length());
                int end = clamp(start + del.length, 0, doc.content.length());
                doc.content = doc.content.substring(0, start) + doc.content.substring(end);
            }

            // Commit history
            doc.history.add(cloneOp(incoming));
            doc.version++;

            // Broadcast
            AppliedOp out = new AppliedOp();
            out.type = incoming.type;
            out.version = doc.version;
            out.clientId = incoming.clientId;
            out.opId = incoming.opId;
            out.pos = incoming.pos;
            if (incoming instanceof InsertOp) out.text = ((InsertOp) incoming).text;
            if (incoming instanceof DeleteOp) out.length = ((DeleteOp) incoming).length;

            broadcast(doc, out);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        docs.values().forEach(d -> d.sessions.remove(session));
    }

    private void broadcast(DocState doc, AppliedOp out) throws IOException {
        String json = mapper.writeValueAsString(out);
        synchronized (doc.sessions) {
            for (WebSocketSession s : doc.sessions) {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            }
        }
    }

    private int clamp(int x, int a, int b) { return Math.max(a, Math.min(b, x)); }

    private Op cloneOp(Op op) {
        if (op instanceof InsertOp ins) {
            InsertOp c = new InsertOp();
            c.type = "insert"; c.pos = ins.pos; c.baseVersion = ins.baseVersion;
            c.clientId = ins.clientId; c.opId = ins.opId; c.text = ins.text;
            return c;
        } else if (op instanceof DeleteOp del) {
            DeleteOp c = new DeleteOp();
            c.type = "delete"; c.pos = del.pos; c.baseVersion = del.baseVersion;
            c.clientId = del.clientId; c.opId = del.opId; c.length = del.length;
            return c;
        }
        return op;
    }
}
