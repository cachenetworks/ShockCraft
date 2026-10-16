package openshock.integrations.minecraft.legacy;

import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Properties;

/** Instance-local behavior, with credentials stored separately in the user's own profile. */
public final class LegacyConfig {
    private LegacyConfig() {}
    public static boolean enabled = false;
    public static boolean onDamage = true, onDeath = true, onLevelUp = false, onChat = false;
    public static boolean linearDamage = true;
    // Detailed local troubleshooting messages; never displays credentials.
    public static boolean showDamageDiagnostics = true;
    public static int damageIntensityMin = 5;
    public static float linearFullHearts = 10.0F;
    public static int damageIntensity = 20, deathIntensity = 30, levelIntensity = 15, chatIntensity = 15;
    public static int durationMs = 500, cooldownMs = 3000;
    public static float minDamageHearts = 0.0F;
    public static String chatPhrase = "shock me";
    public static String apiBaseUrl = "https://api.openshock.app", apiToken = "", shockerIds = "";

    public static Path behaviorPath() {
        return Minecraft.getMinecraft().gameDir.toPath().resolve("config")
            .resolve("ShockCraft-legacy.properties");
    }

    public static Path accountPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        String base;
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            base = appdata != null && !appdata.isEmpty() ? appdata
                : Paths.get(home, "AppData", "Roaming").toString();
        } else if (os.contains("mac")) {
            base = Paths.get(home, "Library", "Application Support").toString();
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            base = xdg != null && Paths.get(xdg).isAbsolute()
                ? xdg : Paths.get(home, ".config").toString();
        }
        return Paths.get(base, "OpenShock", "ShockCraft", "legacy-1.12.2.properties");
    }

    public static void load() {
        Properties p = read(behaviorPath());
        enabled = bool(p, "enabled", false);
        onDamage = bool(p, "onDamage", true);
        linearDamage = bool(p, "linearDamage", true);
        showDamageDiagnostics = bool(p, "showDamageDiagnostics", true);
        damageIntensityMin = number(p, "damageIntensityMin", 5, 0, 100);
        onDeath = bool(p, "onDeath", true);
        onLevelUp = bool(p, "onLevelUp", false);
        onChat = bool(p, "onChat", false);
        chatPhrase = p.getProperty("chatPhrase", "shock me");
        damageIntensity = number(p, "damageIntensity", 20, 0, 100);
        deathIntensity = number(p, "deathIntensity", 30, 0, 100);
        levelIntensity = number(p, "levelIntensity", 15, 0, 100);
        chatIntensity = number(p, "chatIntensity", 15, 0, 100);
        durationMs = number(p, "durationMs", 500, 100, 30000);
        cooldownMs = number(p, "cooldownMs", 3000, 0, 60000);
        try {
            minDamageHearts = Math.max(0F, Math.min(20F,
                Float.parseFloat(p.getProperty("minDamageHearts", "0.0"))));
            if (!Float.isFinite(minDamageHearts)) minDamageHearts = 0F;
        } catch (NumberFormatException ex) {
            minDamageHearts = 0.0F;
        }
        try {
            linearFullHearts = Float.parseFloat(p.getProperty("linearFullHearts", "10.0"));
            if (!Float.isFinite(linearFullHearts)) linearFullHearts = 10F;
            linearFullHearts = Math.max(0.5F, Math.min(100F, linearFullHearts));
        } catch (NumberFormatException ex) {
            linearFullHearts = 10.0F;
        }
        Properties account = read(accountPath());
        apiBaseUrl = account.getProperty("apiBaseUrl", apiBaseUrl).trim();
        apiToken = account.getProperty("apiToken", "").trim();
        shockerIds = account.getProperty("shockerIds", "").trim();
    }

    public static boolean saveBehavior() {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("onDamage", String.valueOf(onDamage));
        p.setProperty("linearDamage", String.valueOf(linearDamage));
        p.setProperty("showDamageDiagnostics", String.valueOf(showDamageDiagnostics));
        p.setProperty("damageIntensityMin", String.valueOf(damageIntensityMin));
        p.setProperty("linearFullHearts", String.valueOf(linearFullHearts));
        p.setProperty("onDeath", String.valueOf(onDeath));
        p.setProperty("onLevelUp", String.valueOf(onLevelUp));
        p.setProperty("onChat", String.valueOf(onChat));
        p.setProperty("chatPhrase", chatPhrase);
        p.setProperty("damageIntensity", String.valueOf(damageIntensity));
        p.setProperty("deathIntensity", String.valueOf(deathIntensity));
        p.setProperty("levelIntensity", String.valueOf(levelIntensity));
        p.setProperty("chatIntensity", String.valueOf(chatIntensity));
        p.setProperty("durationMs", String.valueOf(durationMs));
        p.setProperty("cooldownMs", String.valueOf(cooldownMs));
        p.setProperty("minDamageHearts", String.valueOf(minDamageHearts));
        return write(behaviorPath(), p, false);
    }

    /** Safely persist account details outside the Minecraft instance. */
    public static boolean saveAccount(String url, String token, String ids) {
        Properties p = new Properties();
        p.setProperty("apiBaseUrl", url);
        p.setProperty("apiToken", token);
        p.setProperty("shockerIds", ids);
        if (!write(accountPath(), p, true)) return false;
        apiBaseUrl = url;
        apiToken = token;
        shockerIds = ids;
        return true;
    }

    /** Generate an empty template but never overwrite a token the user already supplied. */
    public static void ensureAccountTemplate() {
        Path path = accountPath();
        if (Files.exists(path)) return;
        Properties p = new Properties();
        p.setProperty("apiBaseUrl", "https://api.openshock.app");
        p.setProperty("apiToken", "");
        p.setProperty("shockerIds", "");
        write(path, p, true);
    }

    private static Properties read(Path path) {
        Properties p = new Properties();
        if (Files.exists(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                p.load(stream);
            } catch (IOException e) {
                LegacyShockCraft.LOG.warn("Could not read ShockCraft settings at {}", path);
            }
        }
        return p;
    }

    private static boolean write(Path path, Properties p, boolean privateFile) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream stream = Files.newOutputStream(path)) {
                p.store(stream, "ShockCraft Forge 1.12.2");
            }
            if (privateFile) {
                try {
                    Files.setPosixFilePermissions(path,
                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) { /* Windows ACLs */ }
            }
            return true;
        } catch (IOException | SecurityException ex) {
            LegacyShockCraft.LOG.error("Could not write ShockCraft settings at {}", path);
            return false;
        }
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(fallback)));
    }

    private static int number(Properties p, String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max,
                Integer.parseInt(p.getProperty(key, String.valueOf(fallback)))));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
