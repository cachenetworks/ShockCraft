package openshock.integrations.minecraft.legacy;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DamageScalingTest {
    @Test public void lostHeartsScaleProportionally() {
        assertEquals(5, DamageScaling.linear(0F, 5, 20, 10F));
        assertEquals(6, DamageScaling.linear(.5F, 5, 20, 10F));
        assertEquals(13, DamageScaling.linear(5F, 5, 20, 10F));
        assertEquals(20, DamageScaling.linear(10F, 5, 20, 10F));
        assertEquals(20, DamageScaling.linear(17F, 5, 20, 10F));
    }

    @Test public void weakerHitsNeverProduceMoreIntensity() {
        int previous = 0;
        for (int i = 0; i <= 100; i++) {
            int current = DamageScaling.linear(i / 10F, 5, 35, 8F);
            assertTrue(current >= previous && current <= 35);
            previous = current;
        }
    }

    @Test public void respectsConfiguredMaxAndHandlesInvalidInputs() {
        assertEquals(10, DamageScaling.linear(100F, 80, 10, 10F));
        assertEquals(5, DamageScaling.linear(Float.NaN, 5, 20, 10F));
        assertEquals(20, DamageScaling.linear(10F, 5, 20, Float.NaN));
        assertEquals(100, DamageScaling.linear(100F, 0, 150, 10F));
    }
}
