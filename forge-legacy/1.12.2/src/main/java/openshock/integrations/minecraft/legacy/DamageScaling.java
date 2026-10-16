package openshock.integrations.minecraft.legacy;

/**
 * Damage-based intensity mapping with user-configured endpoints. No physical
 * output occurs here: this pure function just returns the requested percentage.
 */
public final class DamageScaling {
    private DamageScaling() {}

    /**
     * @param lostHearts positive health lost, expressed in hearts (2 HP = 1 heart)
     * @param minimumIntensity lowest requested percentage
     * @param maximumIntensity highest requested percentage (configured on Limits)
     * @param fullIntensityHearts lost hearts needed to reach the configured maximum
     */
    public static int linear(float lostHearts, int minimumIntensity, int maximumIntensity,
                             float fullIntensityHearts) {
        int max = Math.max(0, Math.min(100, maximumIntensity));
        int min = Math.max(0, Math.min(max, minimumIntensity));
        float reachMaxAt = Float.isFinite(fullIntensityHearts) && fullIntensityHearts > 0F
            ? fullIntensityHearts : 10F;
        float safeHearts = Float.isFinite(lostHearts)
            ? Math.max(0F, lostHearts) : 0F;
        float fraction = Math.min(1F, safeHearts / reachMaxAt);
        return Math.max(min, Math.min(max, Math.round(min + (max - min) * fraction)));
    }
}
