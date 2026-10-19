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

### Damage trigger troubleshooting

When **Damage diagnostics** is enabled (Scaling tab), a compact HUD is
displayed in survival mode with the current HP, whether `On damage` and
`Physical output` are enabled, a growing client-tick counter, and the last
detection result. This HUD does not contain API tokens or shocker IDs. Test
with F1 (hide HUD) **off**. If the HUD/tick counter does not appear, check that
the updated jar is actually installed, there is only one ShockCraft jar in
`mods/`, and the Forge client loaded the mod.

The new diagnostics run even with `On damage` disabled, so it can explain why
a health decrease was ignored. Hurt animations with no measurable loss of
hearts are diagnostic-only; they never send physical output. A dropped
`/kill` health value or death with `On death` enabled uses the separate
death trigger, not necessarily the damage trigger. Keep physical output OFF
while determining which event fires, and test API requests with a mock service
before enabling a physical device.


The Scaling tab has a **Damage diagnostics** toggle, enabled by default
during this beta. After damage in a world, the action bar explains whether
health loss was detected, a legacy saved minimum damage threshold filtered
the hit, the account/physical-output setting disabled sending, intensity is
0%, the request was held by cooldown, or the API refused the request.
Turn diagnostics off when finished testing.

**Old settings are intentionally preserved.** A 1-heart threshold saved by
a previous build remains at 1 until you visit **Limits > Min damage** and
set it to `0`. Weak player punches can otherwise be filtered out. The
mod now also samples health while an inventory is open in multiplayer;
opening an inventory no longer resets its damage detection state.

A successful Gradle build and mock/automated tests do not replace an
in-game test on your Forge 1.12.2 client. Do not share your API token in
logs or screenshots.

### Troubleshoot an HTTP 500 without sending another shock

A `500` means the API received the control request but failed to process
it successfully. **A 500 does not prove whether a physical device acted.**
Avoid repeating control requests with a device connected just to test it.

Go to **Mods > ShockCraft > Config > Account > Check API**.
This sends an authenticated **GET /1/shockers/own** request only, with
**no device-control command**. It can distinguish a working API token
from a backend problem limited to the control endpoint:

- **HTTP 200:** token and read-only API request work; the physical
  control endpoint or backend processing still needs investigation.
- **HTTP 401/403:** check the API token and access permissions.
- **HTTP 500:** even the read-only endpoint failed; the backend may
  have a broader issue.
- **Connection error:** check your configured HTTPS API URL or network.

After a control error, the diagnostic HUD can display a short request trace
ID or CF-Ray header from the response. This is safe to report to a backend
administrator, unlike an API token or shocker ID. Server response bodies
are never printed unfiltered. Physical requests are paused for 60 seconds
after a 5xx; they are **never retried automatically**.

### Diagnosing "one death and then nothing"

Death deliberately fires once per **life**. On respawn, the detector starts a
fresh health baseline. A previous **HTTP 500** is more important: the backend
could have received the control even while returning an error, so ShockCraft
holds further physical requests for 60 seconds and never automatically retries.

The debug HUD now has **two separate lines**:

- **Last damage:** shows damage/death/hurt animation detection even when
  physical requests are disabled, in cooldown, or held after an HTTP 500.
- **Last API result:** preserves the last server response, including safe
  trace/CF-Ray identifiers. The HUD also shows the remaining 5xx hold time.

For a safe test, turn **Physical output OFF**, keep **Damage diagnostics ON**,
then take ordinary (nonlethal) health damage in Survival. You should see
"Lost X hearts -> physical output OFF". If it says "Hurt animation; no HP
loss", check absorption/invulnerability and watch HP in the HUD. If the HUD
sees damage but the API shows HTTP 500, the Minecraft damage detector is
working and the remaining failure is at the OpenShock control path.

For real-device safety, do not repeatedly send physical control commands
while the API is returning 500. A 500 is not proof that the device did nothing.

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
