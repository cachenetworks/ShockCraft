package openshock.integrations.minecraft.legacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyApiConstraintsTest {
    private static final String SUFFIX = " (ShockCraft-Forge-1.12.2)";

    @Test
    public void longDamageReasonFitsBackendColumnAndKeepsAttribution() {
        String result = LegacyApiConstraints.customName(
            "Damage (0.9 hearts): source not reported by server");
        assertEquals(64, result.codePointCount(0, result.length()));
        assertTrue(result.startsWith("Damage (0.9 hearts):"));
        assertTrue(result.endsWith(SUFFIX));
    }

    @Test
    public void shortReasonIsNotTruncated() {
        assertEquals("Chat Phrase" + SUFFIX,
            LegacyApiConstraints.customName("Chat Phrase"));
    }

    @Test
    public void unicodeCodePointsAreNotSplit() {
        String input = "😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀";
        String result = LegacyApiConstraints.customName(input);
        assertEquals(64, result.codePointCount(0, result.length()));
        assertTrue(result.endsWith(SUFFIX));
        assertFalse(result.contains("\uFFFD"));
    }

    @Test
    public void durationBoundsPreventInvalidApiRequests() {
        assertFalse(LegacyApiConstraints.validDuration(100));
        assertFalse(LegacyApiConstraints.validDuration(299));
        assertTrue(LegacyApiConstraints.validDuration(300));
        assertTrue(LegacyApiConstraints.validDuration(30000));
        assertFalse(LegacyApiConstraints.validDuration(30001));
    }
}
