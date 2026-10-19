package openshock.integrations.minecraft.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import java.util.Collections;
import java.util.Set;

/**
 * Makes Minecraft's main menu -> Mods -> ShockCraft -> Config work.
 * It remains available when the O keybind conflicts with another mod.
 */
public final class LegacyModGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraft) { }

    @Override
    public boolean hasConfigGui() { return true; }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        return new LegacyConfigScreen(parent);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
