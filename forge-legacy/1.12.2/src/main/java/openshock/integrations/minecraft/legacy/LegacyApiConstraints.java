package openshock.integrations.minecraft.legacy;

/** OpenShock control API constraints, pure Java 8 so they can be tested offline. */
public final class LegacyApiConstraints {
    private LegacyApiConstraints() {}

    public static final int MIN_DURATION_MS = 300;
    public static final int MAX_DURATION_MS = 30000;
    public static final int MAX_CUSTOM_NAME_CHARS = 64;
    private static final String NAME_SUFFIX = " (ShockCraft-Forge-1.12.2)";

    public static boolean validDuration(int durationMs) {
        return durationMs >= MIN_DURATION_MS && durationMs <= MAX_DURATION_MS;
    }

    /** Keep the origin suffix while respecting the backend's 64-character limit. */
    public static String customName(String reason) {
        String safeReason = reason == null ? "" : reason;
        int room = MAX_CUSTOM_NAME_CHARS - NAME_SUFFIX.codePointCount(0, NAME_SUFFIX.length());
        if (safeReason.codePointCount(0, safeReason.length()) > room) {
            safeReason = safeReason.substring(0, safeReason.offsetByCodePoints(0, room));
        }
        return safeReason + NAME_SUFFIX;
    }
}
