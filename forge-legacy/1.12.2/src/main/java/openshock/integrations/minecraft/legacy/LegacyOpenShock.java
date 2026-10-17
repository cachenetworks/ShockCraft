package openshock.integrations.minecraft.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Async Java 8 HTTPS client; no TLS bypass or delayed backlog of device actions. */
public final class LegacyOpenShock {
    private LegacyOpenShock() {}
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "shockcraft-legacy-api");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean CHECK_BUSY = new AtomicBoolean(false);
    private static final ExecutorService CHECKS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "shockcraft-api-check");
        thread.setDaemon(true);
        return thread;
    });
    // After HTTP 5xx, wait before allowing another physical request.
    // A failed API response does not prove the device did not receive a command.
    private static volatile long backendErrorUntil = 0L;
    private static long lastSentAt;

    // Persistent local telemetry for the debug HUD. No account secrets.
    private static volatile String lastDiagnostic = "No API request yet";
    private static volatile long lastDiagnosticAt;
    private static volatile String lastHealthEvent = "Waiting for damage";
    private static volatile long lastHealthEventAt;

    public static String lastDiagnostic() { return lastDiagnostic; }
    public static long lastDiagnosticAt() { return lastDiagnosticAt; }
    public static String lastHealthEvent() { return lastHealthEvent; }
    public static long lastHealthEventAt() { return lastHealthEventAt; }

    /** 5xx lockout is separate from Minecraft's damage event detection. */
    public static long backendHoldSeconds() {
        long remaining = backendErrorUntil - System.currentTimeMillis();
        return remaining <= 0 ? 0L : (remaining + 999L) / 1000L;
    }

    /** Records detection even if physical output is OFF or the server is unavailable. */
    public static void reportHealthEvent(String description) {
        lastHealthEvent = description;
        lastHealthEventAt = System.currentTimeMillis();
    }

    public enum SubmitResult {
        QUEUED("request queued"),
        DISABLED("physical output OFF"),
        NO_ACCOUNT("API token or shocker ID missing"),
        ZERO_INTENSITY("intensity set to 0"),
        COOLDOWN("cooldown active"),
        BUSY("previous request still in progress"),
        BACKEND_ERROR("server error; physical commands paused temporarily");

        public final String message;
        SubmitResult(String message) { this.message = message; }
    }

    /** Local debug status, never includes API keys, shocker IDs or payloads. */
    public static void showDiagnostic(String message) {
        lastDiagnostic = message;
        lastDiagnosticAt = System.currentTimeMillis();
        if (!LegacyConfig.showDamageDiagnostics) return;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.sendStatusMessage(
                    new TextComponentString("ShockCraft: " + message), true);
            }
        });
    }

    /** Invoked exclusively from client game events, not the HTTP worker thread. */
    public static SubmitResult fire(String reason, int requestedIntensity) {
        if (!LegacyConfig.enabled) return SubmitResult.DISABLED;
        if (LegacyConfig.apiToken.isEmpty() || LegacyConfig.shockerIds.trim().isEmpty())
            return SubmitResult.NO_ACCOUNT;
        if (requestedIntensity <= 0) return SubmitResult.ZERO_INTENSITY;
        final long now = System.currentTimeMillis();
        if (now < backendErrorUntil) return SubmitResult.BACKEND_ERROR;
        if (now - lastSentAt < Math.max(0, LegacyConfig.cooldownMs)) return SubmitResult.COOLDOWN;
        if (!BUSY.compareAndSet(false, true)) return SubmitResult.BUSY;
        lastSentAt = now;

        final int intensity = Math.max(1, Math.min(100, requestedIntensity));
        final int durationMs = Math.max(100, Math.min(30000, LegacyConfig.durationMs));
        final String baseUrl = LegacyConfig.apiBaseUrl;
        final String token = LegacyConfig.apiToken;
        final String ids = LegacyConfig.shockerIds;
        EXECUTOR.execute(() -> {
            try {
                URI uri = URI.create(baseUrl.replaceAll("/+$", "") + "/2/shockers/control");
                // Only validated HTTPS endpoints; never send the token to an HTTP redirect.
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || uri.getHost() == null) {
                    LegacyShockCraft.LOG.warn("Configure a valid HTTPS OpenShock API URL");
                    return;
                }
                JsonArray shocks = new JsonArray();
                for (String entry : ids.split(",")) {
                    String id = entry.trim();
                    if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
                        continue;
                    JsonObject shock = new JsonObject();
                    shock.addProperty("id", id);
                    shock.addProperty("type", "Shock");
                    shock.addProperty("intensity", intensity);
                    shock.addProperty("duration", durationMs);
                    shocks.add(shock);
                }
                if (shocks.size() == 0) {
                    LegacyShockCraft.LOG.warn("Add a valid shocker UUID before enabling output");
                    return;
                }
                JsonObject payload = new JsonObject();
                payload.add("shocks", shocks);
                payload.addProperty("customName", reason + " (ShockCraft-Forge-1.12.2)");
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("OpenShockToken", token);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("User-Agent", "ShockCraft-Forge-1.12.2");
                    conn.setDoOutput(true);
                    try (OutputStream stream = conn.getOutputStream()) {
                        stream.write(bytes);
                    }
                    int code = conn.getResponseCode();
                    if (code / 100 == 2) {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            if (Minecraft.getMinecraft().player != null) {
                                Minecraft.getMinecraft().player.sendStatusMessage(
                                    new TextComponentString("OpenShock: " + reason), true);
                            }
                        });
                    } else {
                        try (InputStream errorBody = conn.getErrorStream()) {
                            String details = LegacyErrorDetails.describe(
                                code, conn.getContentType(), errorBody,
                                conn.getHeaderField("CF-Ray"),
                                conn.getHeaderField("X-Request-ID"));
                            LegacyShockCraft.LOG.warn("OpenShock control request: {}", details);
                            showDiagnostic("API control: " + details);
                        }
                        if (code >= 500) {
                            // Do not immediately send another command after ambiguous 5xx.
                            backendErrorUntil = System.currentTimeMillis() + 60000L;
                        }
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                // Never write credentials, headers, shocker IDs or payloads into game logs.
                LegacyShockCraft.LOG.warn("OpenShock request failed ({})", e.getClass().getSimpleName());
                showDiagnostic("OpenShock request error (" + e.getClass().getSimpleName() + ")");
            } finally {
                BUSY.set(false);
            }
        });
        return SubmitResult.QUEUED;
    }

    /** Read-only authentication probe. Does not send a shock or any control. */
    public static void checkConnection(String baseUrl, String apiToken, Consumer<String> callback) {
        if (apiToken.trim().isEmpty()) {
            callback.accept("No API token entered");
            return;
        }
        if (!CHECK_BUSY.compareAndSet(false, true)) {
            callback.accept("API check is already running");
            return;
        }
        CHECKS.execute(() -> {
            String status;
            try {
                URI url = URI.create(baseUrl.trim().replaceAll("/+$", "") + "/1/shockers/own");
                if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null
                    || url.getUserInfo() != null) {
                    throw new IllegalArgumentException("HTTPS URL required");
                }
                HttpURLConnection conn = (HttpURLConnection) url.toURL().openConnection();
                try {
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("OpenShockToken", apiToken);
                    conn.setRequestProperty("User-Agent", "ShockCraft-Forge-1.12.2");
                    int response = conn.getResponseCode();
                    if (response == 200) {
                        status = "API token accepted (HTTP 200); no control sent";
                    } else if (response == 401 || response == 403) {
                        status = "API token denied (HTTP " + response + "); check permissions";
                    } else {
                        try (InputStream body = conn.getErrorStream()) {
                            status = "API check: " + LegacyErrorDetails.describe(
                                response, conn.getContentType(), body,
                                conn.getHeaderField("CF-Ray"),
                                conn.getHeaderField("X-Request-ID"));
                        }
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                status = "API check failed: " + e.getClass().getSimpleName();
            } finally {
                CHECK_BUSY.set(false);
            }
            final String result = status;
            Minecraft.getMinecraft().addScheduledTask(() -> callback.accept(result));
        });
    }
}
