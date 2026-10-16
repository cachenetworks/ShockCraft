package openshock.integrations.minecraft.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

/** Forge 1.12.2 implementation. All physical requests are opt-in and client-side. */
@Mod(modid = LegacyShockCraft.MOD_ID, name = "ShockCraft (Forge Legacy)",
    version = LegacyShockCraft.VERSION, acceptedMinecraftVersions = "[1.12.2]",
    acceptableRemoteVersions = "*", clientSideOnly = true,
    guiFactory = "openshock.integrations.minecraft.legacy.LegacyModGuiFactory")
public final class LegacyShockCraft {
    public static final String MOD_ID = "shockcraft";
    public static final String VERSION = "2.0.0-beta.1-legacy.1";
    public static final Logger LOG = LogManager.getLogger("ShockCraft Legacy");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        LegacyConfig.load();
        LegacyConfig.ensureAccountTemplate();
        ClientEvents events = new ClientEvents();
        ClientRegistry.registerKeyBinding(events.openSettings);
        MinecraftForge.EVENT_BUS.register(events);
        LOG.info("ShockCraft Forge 1.12.2 initialized. Physical output is {}.",
            LegacyConfig.enabled ? "enabled in local config" : "disabled by default");
    }

    private static final class ClientEvents {
        final KeyBinding openSettings = new KeyBinding("Open ShockCraft settings",
            Keyboard.KEY_O, "ShockCraft");
        EntityPlayerSP previousPlayer;
        float previousHealth;
        int previousLevel;
        boolean wasDead;
        int previousHurtTime;
        long debugTicks;

        /**
         * Visible even when no damage event fires: confirms the mod is loaded,
         * client ticks are running and the current saved settings are active.
         * This never displays the API token, device IDs or full account URL.
         */
        @SubscribeEvent
        public void renderDiagnostics(RenderGameOverlayEvent.Text event) {
            if (!LegacyConfig.showDamageDiagnostics) return;
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            if (player == null) return;
            String playerState = player.isCreative() || player.isSpectator()
                ? "creative/spectator - ignored"
                : mc.isGamePaused() ? "paused" : "watching";
            event.getLeft().add("§aShockCraft 1.12.2 §7| §f" + playerState
                + " §7| ticks: " + debugTicks);
            event.getLeft().add("§7HP §f" + hearts(player.getHealth() / 2.0F)
                + " hearts §7| damage " + (LegacyConfig.onDamage ? "§aON" : "§cOFF")
                + " §7| output " + (LegacyConfig.enabled ? "§aON" : "§cOFF")
                + " §7| Min damage " + LegacyConfig.minDamageHearts + " hearts");
            event.getLeft().add("§7Last: §f" + LegacyOpenShock.lastDiagnostic());
        }

        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            debugTicks++;
            openIfPressed(mc);

            EntityPlayerSP player = mc.player;
            // Keep baselines across temporary screens. An inventory does NOT
            // pause multiplayer, and a paused singleplayer game cannot take hits.
            if (player == null) {
                previousPlayer = null;
                return;
            }
            if (mc.isGamePaused()) return;
            if (player.isCreative() || player.isSpectator()) {
                previousPlayer = null;
                return;
            }
            if (player != previousPlayer) {
                previousPlayer = player;
                previousHealth = player.getHealth();
                previousLevel = player.experienceLevel;
                previousHurtTime = player.hurtTime;
                wasDead = false;
                LegacyOpenShock.showDiagnostic("Client tracking started; hurt a survival player to test");
                return;
            }
            float health = player.getHealth();
            int level = player.experienceLevel;
            boolean dead = player.isDead || health <= 0.0F;
            if (dead && !wasDead && LegacyConfig.onDeath) {
                LegacyOpenShock.SubmitResult result = LegacyOpenShock.fire(
                    "Death: " + damageName(player.getLastDamageSource()), LegacyConfig.deathIntensity);
                LegacyOpenShock.showDiagnostic("Death detected: " + result.message);
            } else if (health < previousHealth) {
                // Every real health decrease counts when the threshold is zero,
                // including weak PvP hits and lethal damage if death-trigger is off.
                float heartsLost = (previousHealth - health) / 2.0F;
                if (heartsLost > 0) {
                    if (!LegacyConfig.onDamage) {
                        LegacyOpenShock.showDiagnostic("Lost " + hearts(heartsLost)
                            + " hearts; On damage is OFF in Triggers");
                    } else if (heartsLost < LegacyConfig.minDamageHearts) {
                        LegacyOpenShock.showDiagnostic("Detected " + hearts(heartsLost)
                            + " hearts lost, below Min damage " + LegacyConfig.minDamageHearts);
                    } else {
                        int intensity = LegacyConfig.linearDamage
                            ? DamageScaling.linear(heartsLost, LegacyConfig.damageIntensityMin,
                                LegacyConfig.damageIntensity, LegacyConfig.linearFullHearts)
                            : LegacyConfig.damageIntensity;
                        String label = "Damage (" + hearts(heartsLost)
                            + " hearts): " + damageName(player.getLastDamageSource());
                        LegacyOpenShock.SubmitResult result = LegacyOpenShock.fire(label, intensity);
                        LegacyOpenShock.showDiagnostic("Damage " + hearts(heartsLost) + " hearts: "
                            + result.message + (result == LegacyOpenShock.SubmitResult.QUEUED
                                ? " (" + intensity + "%)" : ""));
                    }
                }
            } else if (player.hurtTime > 0 && previousHurtTime <= 0) {
                // A hurt animation alone is NOT an accurate amount of damage.
                // Never fire a physical request without confirmed health loss.
                LegacyOpenShock.showDiagnostic("Hurt animation detected, but HP did not decrease"
                    + " (absorption/invulnerability/health sync)");
            }
            if (!dead && level > previousLevel && LegacyConfig.onLevelUp) {
                LegacyOpenShock.fire("XP Level Up (" + (level - previousLevel) + ")",
                    LegacyConfig.levelIntensity);
            }
            previousHealth = health;
            previousLevel = level;
            previousHurtTime = player.hurtTime;
            wasDead = dead;
        }

        /**
         * Key input events fire as Minecraft processes a press. Keeping the
         * tick check as a fallback covers older Forge/modded input setups.
         */
        @SubscribeEvent
        public void keyInput(InputEvent.KeyInputEvent event) {
            openIfPressed(Minecraft.getMinecraft());
        }

        private void openIfPressed(Minecraft mc) {
            if (openSettings.isPressed() && mc.currentScreen == null) {
                mc.displayGuiScreen(new LegacyConfigScreen(null));
            }
        }

        @SubscribeEvent
        public void received(ClientChatReceivedEvent event) {
            if (event.getType() == ChatType.CHAT) chat(event.getMessage().getUnformattedText());
        }

        @SubscribeEvent
        public void sent(ClientChatEvent event) {
            chat(event.getMessage());
        }

        private void chat(String message) {
            if (!LegacyConfig.onChat || LegacyConfig.chatPhrase.trim().isEmpty()) return;
            if (message.toLowerCase(java.util.Locale.ROOT).contains(
                LegacyConfig.chatPhrase.toLowerCase(java.util.Locale.ROOT))) {
                LegacyOpenShock.fire("Chat Phrase", LegacyConfig.chatIntensity);
            }
        }

        private static String hearts(float value) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }

        private static String damageName(DamageSource source) {
            // 1.12.2 multiplayer clients commonly have no attacker/source
            // information. Don't invent a PvP attacker from proximity.
            if (source == null) return "source not reported by server";
            String kind = source.getDamageType();
            return source.getTrueSource() == null ? kind :
                source.getTrueSource().getName() + " (" + kind + ")";
        }
    }
}
