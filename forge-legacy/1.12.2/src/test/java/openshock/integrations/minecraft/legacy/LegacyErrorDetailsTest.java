package openshock.integrations.minecraft.legacy;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class LegacyErrorDetailsTest {
    private static ByteArrayInputStream body(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void showsOnlyGenericStatusAndSafeCorrelationIdentifiers() {
        String json = "{\"title\":\"Internal Server Error\",\"detail\":\"secret-token=ABC123\","
            + "\"traceId\":\"00-abcdef0123456789-abcdef0123456789-01\"}";
        String summary = LegacyErrorDetails.describe(500, "application/problem+json",
            body(json), "ab1234-cd5", null);

        assertTrue(summary.contains("HTTP 500"));
        assertTrue(summary.contains("trace=00-abcdef0123456789-abcdef0123456789-01"));
        assertTrue(summary.contains("CF-Ray=ab1234-cd5"));
        assertFalse(summary.contains("secret-token"));
        assertFalse(summary.contains("ABC123"));
    }

    @Test
    public void neverEchoesArbitraryJsonTitlesOrIdentifiers() {
        String json = "{\"title\":\"Bearer secretshouldneverappear\","
            + "\"traceId\":\"token with whitespace\"}";
        String summary = LegacyErrorDetails.describe(500, "application/json",
            body(json), "spoofed header with spaces", "id with spaces");
        assertEquals("HTTP 500", summary);
    }

    @Test
    public void toleratesEmptyOrBrokenResponse() {
        assertEquals("HTTP 502", LegacyErrorDetails.describe(502, null, null, null, null));
        assertEquals("HTTP 500", LegacyErrorDetails.describe(500,
            "application/problem+json", body("{not json"), null, null));
    }
}
