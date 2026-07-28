package chatapplication.chat_app;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatHandler extends TextWebSocketHandler {

    // Separate waiting queues per mode ("text" or "video")
    private static final Queue<WebSocketSession> waitingQueueText = new ConcurrentLinkedQueue<>();
    private static final Queue<WebSocketSession> waitingQueueVideo = new ConcurrentLinkedQueue<>();

    private static final Map<String, WebSocketSession> activePairs = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionMode = new ConcurrentHashMap<>(); // sessionId -> "text"/"video"

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New connection: " + session.getId());
        SessionLogger.logSessionStart(session.getId());
        // Don't pair yet — wait for the client to send a "join" message with their chosen mode
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());
        String type = node.get("type").asText();

        if ("join".equals(type)) {
            String mode = node.get("mode").asText(); // "text" or "video"
            sessionMode.put(session.getId(), mode);
            pairUser(session, mode);
            return;
        }

        if ("next".equals(type)) {
            handleNext(session);
            return;
        }

        if ("signal".equals(type)) {
            JsonNode payload = node.get("payload");
            if (payload != null && payload.get("kind") != null && "offer".equals(payload.get("kind").asText())) {
                SessionLogger.logVideoUsed(session.getId());
            }
        }

        WebSocketSession partner = activePairs.get(session.getId());
        if (partner != null && partner.isOpen()) {
            partner.sendMessage(new TextMessage(message.getPayload()));
        } else if ("chat".equals(type)) {
            sendSystem(session, "Stranger disconnected. Click Next to find someone new.");
        }
    }

    private synchronized void pairUser(WebSocketSession session, String mode) throws IOException {
        Queue<WebSocketSession> queue = "video".equals(mode) ? waitingQueueVideo : waitingQueueText;
        WebSocketSession partner = queue.poll();

        // Make sure the partner is still open AND still waiting for the same mode
        while (partner != null && !partner.isOpen()) {
            partner = queue.poll();
        }

        if (partner == null) {
            queue.add(session);
            sendSystem(session, "Waiting for a stranger...");
        } else {
            activePairs.put(session.getId(), partner);
            activePairs.put(partner.getId(), session);

            sendSystem(session, "Stranger connected! Say hi.");
            sendSystem(partner, "Stranger connected! Say hi.");
        }
    }

    private synchronized void handleNext(WebSocketSession session) throws IOException {
        WebSocketSession partner = activePairs.remove(session.getId());
        if (partner != null) {
            activePairs.remove(partner.getId());
            if (partner.isOpen()) {
                sendSystem(partner, "Stranger has left the chat.");
                sendEndCall(partner);
                String partnerMode = sessionMode.getOrDefault(partner.getId(), "text");
                (("video".equals(partnerMode)) ? waitingQueueVideo : waitingQueueText).add(partner);
            }
        }

        String myMode = sessionMode.getOrDefault(session.getId(), "text");
        pairUser(session, myMode);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Connection closed: " + session.getId());
        SessionLogger.logSessionEnd(session.getId());

        waitingQueueText.remove(session);
        waitingQueueVideo.remove(session);
        sessionMode.remove(session.getId());

        WebSocketSession partner = activePairs.remove(session.getId());
        if (partner != null) {
            activePairs.remove(partner.getId());
            if (partner.isOpen()) {
                sendSystem(partner, "Stranger has disconnected.");
                sendEndCall(partner);
                String partnerMode = sessionMode.getOrDefault(partner.getId(), "text");
                (("video".equals(partnerMode)) ? waitingQueueVideo : waitingQueueText).add(partner);
            }
        }
    }

    private void sendSystem(WebSocketSession session, String text) throws IOException {
        if (session.isOpen()) {
            String json = mapper.writeValueAsString(Map.of("type", "system", "text", text));
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendEndCall(WebSocketSession session) throws IOException {
        if (session.isOpen()) {
            String json = mapper.writeValueAsString(Map.of("type", "endcall"));
            session.sendMessage(new TextMessage(json));
        }
    }
}