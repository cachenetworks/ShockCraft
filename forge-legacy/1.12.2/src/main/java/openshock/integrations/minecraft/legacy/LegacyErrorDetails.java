package openshock.integrations.minecraft.legacy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Makes backend errors understandable without copying arbitrary server output,
 * device UUIDs, request bodies or credentials into Minecraft's chat or logs.
 */
public final class LegacyErrorDetails {
    private LegacyErrorDetails() { }

    public static String describe(int status, String contentType, InputStream body,
                                  String cfRay, String requestId) {
        StringBuilder out = new StringBuilder("HTTP ").append(status);

        if (body != null && contentType != null
            && contentType.toLowerCase(java.util.Locale.ROOT).contains("json")) {
            try {
                JsonElement parsed = new JsonParser().parse(readLimited(body));
                if (parsed.isJsonObject()) {
                    JsonObject problem = parsed.getAsJsonObject();
                    String title = property(problem, "title");
                    // Only well-known generic titles. Error text from upstream
                    // is untrusted and may contain identifying or secret data.
                    if ("Internal Server Error".equalsIgnoreCase(title)
                        || "An error occurred while processing your request.".equalsIgnoreCase(title)) {
                        out.append(" (server error)");
                    }
                    String trace = safeIdentifier(property(problem, "traceId"));
                    if (trace.isEmpty()) trace = safeIdentifier(property(problem, "requestId"));
                    if (!trace.isEmpty()) out.append(" trace=").append(trace);
                }
            } catch (RuntimeException | IOException ignored) {
                // An HTML, empty or broken response is still a valid failure.
            }
        }
        String ray = safeIdentifier(cfRay);
        if (!ray.isEmpty()) out.append(" CF-Ray=").append(ray);
        String request = safeIdentifier(requestId);
        if (!request.isEmpty()) out.append(" request=").append(request);
        return out.toString();
    }

    private static String property(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString() : "";
    }

    private static String safeIdentifier(String input) {
        if (input == null || input.length() > 70) return "";
        return input.matches("[A-Za-z0-9._:-]+") ? input : "";
    }

    private static String readLimited(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int remaining = 4096;
        int read;
        while (remaining > 0 && (read = stream.read(buf, 0, Math.min(remaining, buf.length))) != -1) {
            output.write(buf, 0, read);
            remaining -= read;
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
