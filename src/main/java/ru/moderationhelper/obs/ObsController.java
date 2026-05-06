package ru.moderationhelper.obs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.config.ModConfig;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal OBS WebSocket v5 controller.
 * It supports authentication, StartRecord and StopRecord only.
 */
public final class ObsController {
    private final ModConfig config;
    private ObsSocket socket;
    private CompletableFuture<Boolean> identifiedFuture;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Instant recordingStartedAt;

    public ObsController(ModConfig config) {
        this.config = config;
    }

    public boolean isRecording() {
        return recording.get();
    }

    public long getRecordingSeconds() {
        if (!recording.get() || recordingStartedAt == null) return 0;
        return Duration.between(recordingStartedAt, Instant.now()).toSeconds();
    }

    public void startRecording() {
        if (!config.obsEnabled) {
            ModerationHelperClient.sendClientMessage("OBS-интеграция выключена в конфиге.");
            return;
        }
        if (sendRequest("StartRecord")) {
            recording.set(true);
            recordingStartedAt = Instant.now();
        }
    }

    public void stopRecording() {
        if (!config.obsEnabled) {
            recording.set(false);
            recordingStartedAt = null;
            return;
        }
        boolean sent = sendRequest("StopRecord");
        // Даже если OBS уже закрыт, локальный таймер надо убрать, чтобы HUD не зависал.
        recording.set(false);
        recordingStartedAt = null;
        if (!sent) {
            ModerationHelperClient.LOGGER.warn("OBS stop request was not sent; local timer was cleared");
        }
    }

    private synchronized boolean sendRequest(String requestType) {
        try {
            ensureConnected();
            boolean identified = identifiedFuture.get(5, TimeUnit.SECONDS);
            if (!identified || socket == null || !socket.isOpen()) {
                ModerationHelperClient.sendClientMessage("OBS недоступен: не удалось пройти идентификацию.");
                return false;
            }

            JsonObject data = new JsonObject();
            data.addProperty("requestType", requestType);
            data.addProperty("requestId", UUID.randomUUID().toString());
            data.add("requestData", new JsonObject());

            JsonObject packet = new JsonObject();
            packet.addProperty("op", 6);
            packet.add("d", data);
            socket.send(packet.toString());
            return true;
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("OBS request failed: {}", requestType, e);
            ModerationHelperClient.sendClientMessage("OBS недоступен: " + e.getMessage());
            return false;
        }
    }

    private void ensureConnected() throws Exception {
        if (socket != null && socket.isOpen() && identifiedFuture != null && identifiedFuture.isDone()) {
            return;
        }
        identifiedFuture = new CompletableFuture<>();
        URI uri = new URI("ws://" + config.obsHost + ":" + config.obsPort);
        socket = new ObsSocket(uri);
        if (!socket.connectBlocking(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("нет соединения с " + uri);
        }
    }

    private final class ObsSocket extends WebSocketClient {
        private ObsSocket(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            ModerationHelperClient.LOGGER.info("Connected to OBS WebSocket");
        }

        @Override
        public void onMessage(String message) {
            try {
                JsonObject packet = JsonParser.parseString(message).getAsJsonObject();
                int op = packet.get("op").getAsInt();
                JsonObject d = packet.has("d") && packet.get("d").isJsonObject()
                        ? packet.getAsJsonObject("d")
                        : new JsonObject();

                if (op == 0) { // Hello
                    sendIdentify(d);
                } else if (op == 2) { // Identified
                    identifiedFuture.complete(true);
                } else if (op == 7) { // RequestResponse
                    handleRequestResponse(d);
                }
            } catch (Exception e) {
                ModerationHelperClient.LOGGER.warn("Invalid OBS packet", e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            ModerationHelperClient.LOGGER.info("OBS WebSocket closed: {} {}", code, reason);
            if (identifiedFuture != null && !identifiedFuture.isDone()) identifiedFuture.complete(false);
        }

        @Override
        public void onError(Exception ex) {
            ModerationHelperClient.LOGGER.warn("OBS WebSocket error", ex);
            if (identifiedFuture != null && !identifiedFuture.isDone()) identifiedFuture.complete(false);
        }

        private void sendIdentify(JsonObject helloData) throws Exception {
            JsonObject identify = new JsonObject();
            identify.addProperty("rpcVersion", 1);

            if (helloData.has("authentication") && helloData.get("authentication").isJsonObject()) {
                JsonObject auth = helloData.getAsJsonObject("authentication");
                String challenge = string(auth.get("challenge"));
                String salt = string(auth.get("salt"));
                if (config.obsPassword != null && !config.obsPassword.isBlank()) {
                    identify.addProperty("authentication", createAuth(config.obsPassword, salt, challenge));
                }
            }

            JsonObject packet = new JsonObject();
            packet.addProperty("op", 1);
            packet.add("d", identify);
            send(packet.toString());
        }

        private void handleRequestResponse(JsonObject data) {
            JsonObject status = data.has("requestStatus") ? data.getAsJsonObject("requestStatus") : new JsonObject();
            boolean ok = status.has("result") && status.get("result").getAsBoolean();
            String requestType = data.has("requestType") ? data.get("requestType").getAsString() : "unknown";
            if (!ok) {
                String comment = status.has("comment") ? status.get("comment").getAsString() : "unknown error";
                ModerationHelperClient.sendClientMessage("OBS: " + requestType + " не выполнен: " + comment);
            }
        }
    }

    private static String createAuth(String password, String salt, String challenge) throws Exception {
        String secret = base64sha256(password + salt);
        return base64sha256(secret + challenge);
    }

    private static String base64sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static String string(JsonElement element) {
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }
}
