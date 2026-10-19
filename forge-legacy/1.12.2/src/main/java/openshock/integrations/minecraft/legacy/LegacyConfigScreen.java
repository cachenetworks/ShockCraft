package openshock.integrations.minecraft.legacy;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Self-contained 1.12.2 GUI: account details, triggers and limits can all be edited
 * without touching a file. ESC/Cancel discards changes. Save is explicit.
 */
public final class LegacyConfigScreen extends GuiScreen {
    private enum Page { ACCOUNT, TRIGGERS, SCALING, LIMITS }

    private final GuiScreen parent;
    private Page page = Page.ACCOUNT;
    private final List<GuiTextField> fields = new ArrayList<>();

    // Keep a draft across page changes; never modify the active config until Save.
    private String url = LegacyConfig.apiBaseUrl;
    private String token = LegacyConfig.apiToken;
    private String ids = LegacyConfig.shockerIds;
    private String phrase = LegacyConfig.chatPhrase;
    private boolean enabled = LegacyConfig.enabled;
    private boolean onDamage = LegacyConfig.onDamage;
    private boolean linearDamage = LegacyConfig.linearDamage;
    private boolean diagnostics = LegacyConfig.showDamageDiagnostics;
    private int linearMinIntensity = LegacyConfig.damageIntensityMin;
    private float linearFullHearts = LegacyConfig.linearFullHearts;
    private boolean onDeath = LegacyConfig.onDeath;
    private boolean onLevel = LegacyConfig.onLevelUp;
    private boolean onChat = LegacyConfig.onChat;

    private int damageIntensity = LegacyConfig.damageIntensity;
    private int deathIntensity = LegacyConfig.deathIntensity;
    private int levelIntensity = LegacyConfig.levelIntensity;
    private int chatIntensity = LegacyConfig.chatIntensity;
    private int durationMs = LegacyConfig.durationMs;
    private int cooldownMs = LegacyConfig.cooldownMs;
    private float minDamageHearts = LegacyConfig.minDamageHearts;

    private GuiTextField urlField, tokenField, idsField, phraseField;
    private GuiTextField[] limits;
    private GuiTextField scaleMinField, scaleHeartsField;
    private boolean editingToken;
    private String message = "";
    private int messageColor = 0xFFFF66;

    public LegacyConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        fields.clear();
        buttonList.clear();
        final int mid = width / 2;
        final int contentWidth = Math.min(312, width - 22);
        final int left = mid - contentWidth / 2;

        int tabWidth = Math.min(80, (width - 22) / 4);
        int tabsLeft = mid - tabWidth * 2;
        buttonList.add(new GuiButton(1, tabsLeft, 34, tabWidth, 20, "Account"));
        buttonList.add(new GuiButton(2, tabsLeft + tabWidth, 34, tabWidth, 20, "Triggers"));
        buttonList.add(new GuiButton(4, tabsLeft + tabWidth * 2, 34, tabWidth, 20, "Scaling"));
        buttonList.add(new GuiButton(3, tabsLeft + tabWidth * 3, 34, tabWidth, 20, "Limits"));

        // Every tab shares the bottom controls; never save a half-typed token
        // just because a user navigated between sections.
        int bottom = height - 27;
        buttonList.add(new GuiButton(90, mid - 103, bottom, 100, 20, "Save"));
        buttonList.add(new GuiButton(91, mid + 3, bottom, 100, 20, "Cancel"));

        urlField = null;
        tokenField = null;
        idsField = null;
        phraseField = null;
        limits = null;
        scaleMinField = null;
        scaleHeartsField = null;

        if (page == Page.ACCOUNT) {
            urlField = field(20, left, 81, contentWidth, url, 256);
            tokenField = field(21, left, 120, contentWidth - 75, token, 1024);
            tokenField.setVisible(editingToken);
            idsField = field(22, left, 158, contentWidth, ids, 1024);
            buttonList.add(new GuiButton(30, left + contentWidth - 71, 120, 71, 20,
                editingToken ? "Hide" : "Edit token"));
            buttonList.add(new GuiButton(31, left, 181, contentWidth - 91, 20,
                enabled ? "Physical output: ENABLED" : "Physical output: DISABLED"));
            buttonList.add(new GuiButton(32, left + contentWidth - 87, 181, 87, 20,
                "Check API"));
        } else if (page == Page.TRIGGERS) {
            int toggleWidth = contentWidth;
            buttonList.add(new GuiButton(40, left, 67, toggleWidth, 20, toggle("On damage", onDamage)));
            buttonList.add(new GuiButton(41, left, 91, toggleWidth, 20, toggle("On death", onDeath)));
            buttonList.add(new GuiButton(42, left, 115, toggleWidth, 20, toggle("On XP level up", onLevel)));
            buttonList.add(new GuiButton(43, left, 139, toggleWidth, 20, toggle("On chat phrase", onChat)));
            phraseField = field(23, left, 177, contentWidth, phrase, 128);
        } else if (page == Page.SCALING) {
            buttonList.add(new GuiButton(44, left, 69, contentWidth, 20,
                toggle("Linear intensity for lost hearts", linearDamage)));
            scaleMinField = field(60, left, 115, contentWidth, Integer.toString(linearMinIntensity), 3);
            scaleHeartsField = field(61, left, 155, contentWidth, Float.toString(linearFullHearts), 6);
            buttonList.add(new GuiButton(45, left, 185, contentWidth, 20,
                toggle("Damage diagnostics", diagnostics)));
        } else {
            limits = new GuiTextField[7];
            int columnWidth = (contentWidth - 12) / 2;
            int right = left + columnWidth + 12;
            limits[0] = field(50, left, 80, columnWidth, Integer.toString(damageIntensity), 3);
            limits[1] = field(51, right, 80, columnWidth, Integer.toString(deathIntensity), 3);
            limits[2] = field(52, left, 111, columnWidth, Integer.toString(levelIntensity), 3);
            limits[3] = field(53, right, 111, columnWidth, Integer.toString(chatIntensity), 3);
            limits[4] = field(54, left, 142, columnWidth, Integer.toString(durationMs), 5);
            limits[5] = field(55, right, 142, columnWidth, Integer.toString(cooldownMs), 6);
            limits[6] = field(56, left, 173, columnWidth, Float.toString(minDamageHearts), 6);
        }
    }

    private GuiTextField field(int id, int x, int y, int w, String value, int maxChars) {
        GuiTextField input = new GuiTextField(id, fontRenderer, x, y, w, 19);
        input.setMaxStringLength(maxChars);
        input.setText(value == null ? "" : value);
        fields.add(input);
        return input;
    }

    private static String toggle(String title, boolean selected) {
        return title + ": " + (selected ? "ON" : "OFF");
    }

    private void captureCurrentFields() {
        if (urlField != null) {
            url = urlField.getText().trim();
            token = tokenField.getText().trim();
            ids = idsField.getText().trim();
        }
        if (phraseField != null) phrase = phraseField.getText();
    }

    private void changePage(Page next) {
        captureCurrentFields();
        // Limit inputs are validated when navigating off them, before discarding
        // their text; otherwise a typo could silently turn into a previous value.
        if (page == Page.LIMITS && !captureLimits()) return;
        if (page == Page.SCALING && !captureScaling()) return;
        page = next;
        message = "";
        initGui();
    }

    private static Integer parseInt(GuiTextField field, int min, int max) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean captureLimits() {
        if (limits == null) return true;
        Integer[] values = new Integer[6];
        final int[] min = {0, 0, 0, 0, LegacyApiConstraints.MIN_DURATION_MS, 0};
        final int[] max = {100, 100, 100, 100, 30000, 60000};
        for (int i = 0; i < values.length; i++) {
            values[i] = parseInt(limits[i], min[i], max[i]);
            if (values[i] == null) {
                message = "Invalid value: intensity 0-100, duration 300-30000, cooldown 0-60000.";

                messageColor = 0xFF7777;
                return false;
            }
        }
        float hearts;
        try {
            hearts = Float.parseFloat(limits[6].getText().trim());
        } catch (NumberFormatException e) {
            hearts = Float.NaN;
        }
        if (!Float.isFinite(hearts) || hearts < 0.0F || hearts > 20.0F) {
            message = "Minimum damage must be between 0 and 20 hearts.";
            messageColor = 0xFF7777;
            return false;
        }
        damageIntensity = values[0];
        deathIntensity = values[1];
        levelIntensity = values[2];
        chatIntensity = values[3];
        durationMs = values[4];
        cooldownMs = values[5];
        minDamageHearts = hearts;
        return true;
    }

    private boolean captureScaling() {
        if (scaleMinField == null) return true;
        Integer min = parseInt(scaleMinField, 0, 100);
        if (min == null) {
            message = "Minimum linear intensity must be between 0 and 100%.";
            messageColor = 0xFF7777;
            return false;
        }
        float fullHearts;
        try {
            fullHearts = Float.parseFloat(scaleHeartsField.getText().trim());
        } catch (NumberFormatException e) {
            fullHearts = Float.NaN;
        }
        if (!Float.isFinite(fullHearts) || fullHearts < 0.5F || fullHearts > 100F) {
            message = "Full strength after 0.5 to 100 hearts of damage.";
            messageColor = 0xFF7777;
            return false;
        }
        linearMinIntensity = min;
        linearFullHearts = fullHearts;
        return true;
    }

    private void save() {
        captureCurrentFields();
        if (!captureLimits() || !captureScaling()) return;
        if (linearDamage && linearMinIntensity > damageIntensity) {
            page = Page.SCALING;
            message = "Linear minimum must not exceed Damage intensity (maximum).";
            messageColor = 0xFF7777;
            initGui();
            return;
        }
        try {
            URI parsed = URI.create(url);
            if (!"https".equalsIgnoreCase(parsed.getScheme()) ||
                parsed.getHost() == null || parsed.getUserInfo() != null) {
                throw new IllegalArgumentException("Invalid HTTPS URL");
            }
        } catch (IllegalArgumentException e) {
            page = Page.ACCOUNT;
            message = "Enter a valid HTTPS OpenShock API URL.";
            messageColor = 0xFF7777;
            initGui();
            return;
        }
        // The device endpoint only accepts UUIDs; fail visibly instead of
        // silently dropping typos from a list of shockers.
        for (String entry : ids.split(",")) {
            if (entry.trim().isEmpty()) continue;
            if (!entry.trim().matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                page = Page.ACCOUNT;
                message = "Shocker IDs must be UUIDs, separated by commas.";
                messageColor = 0xFF7777;
                initGui();
                return;
            }
        }
        if (enabled && (token.isEmpty() || ids.isEmpty())) {
            page = Page.ACCOUNT;
            message = "Enter an API token and a shocker ID before enabling output.";
            messageColor = 0xFF7777;
            initGui();
            return;
        }

        // Save account credentials first: never claim success when its file
        // could not be written and immediately used by the API client.
        if (!LegacyConfig.saveAccount(url, token, ids)) {
            message = "Account settings could not be saved. Check folder permissions.";
            messageColor = 0xFF7777;
            return;
        }
        LegacyConfig.enabled = enabled;
        LegacyConfig.onDamage = onDamage;
        LegacyConfig.linearDamage = linearDamage;
        LegacyConfig.showDamageDiagnostics = diagnostics;
        LegacyConfig.damageIntensityMin = linearMinIntensity;
        LegacyConfig.linearFullHearts = linearFullHearts;
        LegacyConfig.onDeath = onDeath;
        LegacyConfig.onLevelUp = onLevel;
        LegacyConfig.onChat = onChat;
        LegacyConfig.chatPhrase = phrase;
        LegacyConfig.damageIntensity = damageIntensity;
        LegacyConfig.deathIntensity = deathIntensity;
        LegacyConfig.levelIntensity = levelIntensity;
        LegacyConfig.chatIntensity = chatIntensity;
        LegacyConfig.durationMs = durationMs;
        LegacyConfig.cooldownMs = cooldownMs;
        LegacyConfig.minDamageHearts = minDamageHearts;

        if (!LegacyConfig.saveBehavior()) {
            message = "Behavior settings could not be saved. Check folder permissions.";
            messageColor = 0xFF7777;
            return;
        }
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: changePage(Page.ACCOUNT); return;
            case 2: changePage(Page.TRIGGERS); return;
            case 3: changePage(Page.LIMITS); return;
            case 4: changePage(Page.SCALING); return;
            case 30:
                editingToken = !editingToken;
                tokenField.setVisible(editingToken);
                tokenField.setFocused(editingToken);
                button.displayString = editingToken ? "Hide" : "Edit token";
                return;
            case 31:
                enabled = !enabled;
                button.displayString = enabled ? "Physical output: ENABLED" : "Physical output: DISABLED";
                return;
            case 32:
                captureCurrentFields();
                message = "Checking token via read-only GET (no control sent)...";
                messageColor = 0xAACCFF;
                LegacyOpenShock.checkConnection(url, token, result -> {
                    message = result;
                    messageColor = result.startsWith("API token accepted")
                        ? 0x77FF77 : 0xFF7777;
                });
                return;
            case 44:
                linearDamage = !linearDamage;
                button.displayString = toggle("Linear intensity for lost hearts", linearDamage);
                return;
            case 45:
                diagnostics = !diagnostics;
                button.displayString = toggle("Damage diagnostics", diagnostics);
                return;
            case 40: onDamage = !onDamage; button.displayString = toggle("On damage", onDamage); return;
            case 41: onDeath = !onDeath; button.displayString = toggle("On death", onDeath); return;
            case 42: onLevel = !onLevel; button.displayString = toggle("On XP level up", onLevel); return;
            case 43: onChat = !onChat; button.displayString = toggle("On chat phrase", onChat); return;
            case 90: save(); return;
            case 91: mc.displayGuiScreen(parent); return;
            default: return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        // Fields, not Minecraft's global shortcuts, should receive the typing.
        for (GuiTextField field : fields) {
            if (field.getVisible() && field.textboxKeyTyped(typedChar, keyCode)) return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : fields) {
            if (field.getVisible()) field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : fields) field.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        final int mid = width / 2;
        final int contentWidth = Math.min(312, width - 22);
        final int left = mid - contentWidth / 2;

        drawCenteredString(fontRenderer, "ShockCraft - Forge 1.12.2", mid, 13, 0xFFFFFF);
        if (page == Page.ACCOUNT) {
            fontRenderer.drawString("OpenShock API URL (HTTPS)", left, 67, 0xDDDDDD);
            fontRenderer.drawString("API token (stored outside your Minecraft folder)", left, 106, 0xDDDDDD);
            fontRenderer.drawString("Shocker UUID(s) - separate multiple with commas", left, 144, 0xDDDDDD);
            if (!editingToken) {
                fontRenderer.drawString(token.isEmpty() ? "[No API token saved - click Edit token]"
                    : "[Token hidden - click Edit token]", left + 4, 126, 0xAAAAAA);
            }
            if (message.isEmpty()) {
                drawCenteredString(fontRenderer,
                    "Set up account first. Physical output is OFF until you enable it.",
                    mid, height - 43, 0xAACCFF);
            }
        } else if (page == Page.TRIGGERS) {
            fontRenderer.drawString("Chat trigger phrase (received or sent chat)", left, 164, 0xDDDDDD);
            if (message.isEmpty()) drawCenteredString(fontRenderer,
                "Choose which events can trigger OpenShock.", mid, height - 43, 0xAACCFF);
        } else if (page == Page.SCALING) {
            fontRenderer.drawString("Starting shock intensity (%)", left, 101, 0xDDDDDD);
            fontRenderer.drawString("Maximum reached after this many lost hearts", left, 141, 0xDDDDDD);
            if (message.isEmpty()) {
                drawCenteredString(fontRenderer, "Maximum set under Limits > Damage intensity.", mid, 65, 0xAACCFF);
            }
        } else {
            int col = (contentWidth - 12) / 2;
            int right = left + col + 12;
            fontRenderer.drawString("Damage intensity (%)", left, 68, 0xDDDDDD);
            fontRenderer.drawString("Death intensity (%)", right, 68, 0xDDDDDD);
            fontRenderer.drawString("XP intensity (%)", left, 99, 0xDDDDDD);
            fontRenderer.drawString("Chat intensity (%)", right, 99, 0xDDDDDD);
            fontRenderer.drawString("Duration (ms)", left, 130, 0xDDDDDD);
            fontRenderer.drawString("Cooldown (ms)", right, 130, 0xDDDDDD);
            fontRenderer.drawString("Min damage (0 = all hits)", left, 161, 0xDDDDDD);
            if (message.isEmpty()) drawCenteredString(fontRenderer,
                "Range: duration 300-30000ms, intensity 0-100%, cooldown >= 0.",
                mid, height - 43, 0xAACCFF);
        }

        for (GuiTextField field : fields) {
            if (field.getVisible()) field.drawTextBox();
        }
        if (!message.isEmpty()) {
            // Long server trace IDs must wrap rather than disappear off-screen.
            fontRenderer.drawSplitString(message, left, height - 55, contentWidth, messageColor);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }
}
