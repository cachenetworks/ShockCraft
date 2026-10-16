package openshock.integrations.minecraft.legacy;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

/** Native 1.12.2 configuration screen: press O in game to open. */
public final class LegacyConfigScreen extends GuiScreen {
    private final GuiScreen parent;

    public LegacyConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int w = Math.min(260, width - 24);
        int x = (width - w) / 2;
        add(1, x, 43, w, toggle("Physical output (opt-in)", LegacyConfig.enabled));
        add(2, x, 68, w, toggle("Shock on damage", LegacyConfig.onDamage));
        add(3, x, 93, w, toggle("Shock on death", LegacyConfig.onDeath));
        add(4, x, 118, w, toggle("Shock on XP level up", LegacyConfig.onLevelUp));
        add(5, x, 143, w, toggle("Shock on chat phrase", LegacyConfig.onChat));
        add(6, x, 187, w, "Save & Done");
    }

    private void add(int id, int x, int y, int w, String title) {
        buttonList.add(new GuiButton(id, x, y, w, 20, title));
    }

    private static String toggle(String text, boolean value) {
        return text + ": " + (value ? "ON" : "OFF");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: LegacyConfig.enabled = !LegacyConfig.enabled; break;
            case 2: LegacyConfig.onDamage = !LegacyConfig.onDamage; break;
            case 3: LegacyConfig.onDeath = !LegacyConfig.onDeath; break;
            case 4: LegacyConfig.onLevelUp = !LegacyConfig.onLevelUp; break;
            case 5: LegacyConfig.onChat = !LegacyConfig.onChat; break;
            case 6:
                LegacyConfig.saveBehavior();
                mc.displayGuiScreen(parent);
                return;
            default: return;
        }
        LegacyConfig.saveBehavior();
        initGui();
    }

    @Override
    public void onGuiClosed() {
        LegacyConfig.saveBehavior();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "ShockCraft - Forge 1.12.2", width / 2, 13, 0xFFFFFF);
        drawCenteredString(fontRenderer, "Physical actions are local and OFF by default.", width / 2, 27, 0xFFCC66);
        if (LegacyConfig.apiToken.isEmpty() || LegacyConfig.shockerIds.trim().isEmpty()) {
            drawCenteredString(fontRenderer, "Configure your token and shocker UUID(s) in:", width / 2, 217, 0xFFCC66);
            drawCenteredString(fontRenderer, "OpenShock/ShockCraft/legacy-1.12.2.properties", width / 2, 230, 0xFFFFFF);
        } else {
            drawCenteredString(fontRenderer, "Credentials found; press O to reopen.", width / 2, 223, 0x77FF77);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
