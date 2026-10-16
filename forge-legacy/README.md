# ShockCraft: Classic Forge support

This is a **separate legacy compatibility port**; it does not change any existing
Fabric/NeoForge build. Minecraft 1.12.2 needs Java 8, ForgeGradle 2.3, older MCP
names and Forge events. Adding 1.12.2 to the modern Stonecutter matrix does not work.

## First implementation: Forge 1.12.2

- Minecraft 1.12.2, built against Forge 14.23.5.2847 (FG2.3 userdev artifact)
- Intended for newer 1.12.2 Forge installs too, once tested in game
- Java 8 and Gradle 4.10.3
- Client-side only; no server installation needed
- Optional damage, death, level-up, and chat-phrase OpenShock triggers
- Press O in-world (remappable), or use Mods > ShockCraft > Config from the main menu
- Account/Triggers/Limits tabs edit API URL, token, UUIDs and other settings in-game
- Configurable intensity, duration, damage threshold, cooldown
- Physical output disabled by default. The port no longer imposes the prior
  extra 50% / 2-second / 3-second hard caps; it validates request-field bounds.
  Set your own intensity and duration conservatively.
- No credentials inside a modpack's game instance

**Status: CI-compiling prototype.** The Forge 1.12.2 Gradle build succeeded and
produced a jar on GitHub Actions. An actual in-game launch and behavior test
are still required before this should be published as a working release.
Modern shocker picker, full YACL UI, fine-grained damage filtering, overlays,
collars/remotes, and networked effects are not ported.

### Build

Install a Java 8 JDK and Gradle 4.10.3, then run:

~~~sh
cd forge-legacy/1.12.2
gradle --no-daemon clean build
~~~

The production jar will be placed in build/libs. The root gradlew is NOT
compatible with legacy ForgeGradle; do not build this folder with it.

### Configure entirely in game

1. Install the compiled JAR in the Minecraft Forge 1.12.2 instance.
2. At the main menu, open **Mods > ShockCraft > Config**. This is
   available even if the O key conflicts with another mod. Alternatively
   press **O** (remappable in Controls) while playing a world.
3. On **Account**, enter your API URL (default https://api.openshock.app).
   Click **Edit token** and paste your own token created in the OpenShock
   dashboard. The mod does not generate tokens. Never share yours.
4. Enter your shocker UUID(s), separated by commas for multiple devices.
5. Set preferred event toggles under **Triggers**; turn on **Linear damage**
   under **Scaling** and choose the starting intensity and how many lost hearts
   reach the maximum. Under **Limits**, "Damage intensity" is the maximum for
   linear scaling (or the fixed intensity with linear mode off).
   Duration and cooldown are also configurable. Set "Min damage" to 0 for
   weak PvP hits; older config files keep any previously saved threshold.
6. Enable **Physical output** on Account only with the device wearer's
   explicit consent. It is OFF by default.
7. Press **Save**. **Cancel/ESC** discards unsaved changes.

The account token is hidden in the UI unless the Edit token button is used.
It is saved to a separate OS-user-owned account file, not the instance:
Windows: %APPDATA%\OpenShock\ShockCraft\legacy-1.12.2.properties
Linux: ~/.config/OpenShock/ShockCraft/legacy-1.12.2.properties
macOS: ~/Library/Application Support/OpenShock/ShockCraft/legacy-1.12.2.properties

Other behavior settings are saved in the game's
config/ShockCraft-legacy.properties file. Manual file editing is not needed.
The account token is still plaintext on disk (not encrypted).

### Linear damage scaling and multiplayer attribution

Default linear mode maps lost hearts to an intensity between the configured
minimum (default 5%) and damage maximum (default 20%). Default saturation is
10 hearts: half a heart produces approximately 6%, five hearts approximately
13%, and ten hearts 20%. The result is clamped to the chosen range, never
exceeds the user's configured damage maximum, and scales intensity **only**
(not duration). Disable Linear damage to use the configured fixed intensity.

Forge 1.12.2 multiplayer often does not synchronize attacker identity or
damage source to the client. ShockCraft observes actual health loss, but labels
a missing attacker as "source not reported by server", rather than guessing
that a nearby player was responsible. A server-side bridge would be needed
for reliable PvP attacker names.

### Other Forge versions: implementation tracker

This table is **a roadmap, not a list of working jars**. Every supported version
requires its own matching Forge/Java/toolchain, API adaptation, and real client test.

| Minecraft | Planned Forge versions | Status |
|---|---|---|
| 1.12 | 1.12.2 | Build passed; in-game tests pending |
| 1.13 | 1.13.2 | Pending |
| 1.14 | 1.14.2, 1.14.3, 1.14.4 | Pending |
| 1.15 | 1.15, 1.15.1, 1.15.2 | Pending |
| 1.16 | 1.16.1, 1.16.2, 1.16.3, 1.16.4, 1.16.5 | Pending |
| 1.17 | 1.17.1 | Pending |
| 1.18 | 1.18, 1.18.1, 1.18.2 | Pending |
| 1.19 | 1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4 | Pending |
| 1.20 | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.6 | Pending |
| 1.21 | 1.21, 1.21.1, 1.21.3 through 1.21.11 (published Forge builds) | Pending |
| 26.x | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | Pending |

Latest official Forge version when this was prepared: Minecraft 26.3,
Forge 66.0.12, dated 2026-10-14.

Recommended implementation order: finish and test 1.12.2, then 1.16.5,
1.18.2, 1.19.4, 1.20.1, 1.21.1 and 26.3; then fill the patch-version
gaps. Separate jars per Minecraft version are necessary; NeoForge jars are
not Forge-compatible.

### Privacy, safety and attribution

Only the client/wearer can enable ShockCraft. Servers and other players cannot
enable it remotely. Legacy API requests require HTTPS with valid certificates,
do not forward tokens through redirects, and do not queue delayed shocks.
This project retains the upstream AGPL-3.0 license and notices.
