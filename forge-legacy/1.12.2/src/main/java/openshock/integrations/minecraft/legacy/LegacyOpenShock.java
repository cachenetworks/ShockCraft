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

/** Async Java 8 HTTPS client; no TLS bypass or delayed backlog of device actions. */
public final class LegacyOpenShock {
    private LegacyOpenShock() {}
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "shockcraft-legacy-api");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static long lastSentAt;

    /** Invoked exclusively from client game events, not the HTTP worker thread. */
    public static void fire(String reason, int requestedIntensity) {
        if (!LegacyConfig.enabled || LegacyConfig.apiToken.isEmpty()
            || LegacyConfig.shockerIds.trim().isEmpty() || requestedIntensity <= 0) return;
        final long now = System.currentTimeMillis();
        if (now - lastSentAt < Math.max(3000, LegacyConfig.cooldownMs)) return;
        if (!BUSY.compareAndSet(false, true)) return;
        lastSentAt = now;

        final int intensity = Math.max(1, Math.min(50, requestedIntensity));
        final int durationMs = Math.max(100, Math.min(2000, LegacyConfig.durationMs));
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
                        LegacyShockCraft.LOG.warn("OpenShock returned HTTP {}", code);
                    }
                    InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                    if (stream != null) stream.close();
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                // Never write credentials, headers, shocker IDs or payloads into game logs.
                LegacyShockCraft.LOG.warn("OpenShock request failed ({})", e.getClass().getSimpleName());
            } finally {
                BUSY.set(false);
            }
        });
    }
}
