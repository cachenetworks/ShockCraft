# ShockCraft: Classic Forge support

This is a **separate legacy compatibility port**; it does not change any existing
Fabric/NeoForge build. Minecraft 1.12.2 needs Java 8, ForgeGradle 2.3, older MCP
names and Forge events. Adding 1.12.2 to the modern Stonecutter matrix does not work.

## First implementation: Forge 1.12.2

- Minecraft 1.12.2, Forge 14.23.5.2864
- Java 8 and Gradle 4.10.3
- Client-side only; no server installation needed
- Optional damage, death, level-up, and chat-phrase OpenShock triggers
- Press O (remappable) to open a native settings screen
- Configurable intensity, duration, damage threshold, cooldown
- Physical output disabled by default and limited to a 50% intensity maximum,
  2-second duration and 3-second minimum cooldown
- No credentials inside a modpack's game instance

**Status: source-only prototype.** The separate CI build and actual in-game launch
must pass before this should be published or described as a working release.
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

### Configure

Launch Forge 1.12.2 with the mod once to generate a credential template.
Close the game before editing these files.

Windows account file:
%APPDATA%\OpenShock\ShockCraft\legacy-1.12.2.properties

Linux account file:
~/.config/OpenShock/ShockCraft/legacy-1.12.2.properties

macOS account file:
~/Library/Application Support/OpenShock/ShockCraft/legacy-1.12.2.properties

Fill in:

~~~properties
apiBaseUrl=https://api.openshock.app
apiToken=YOUR_PRIVATE_API_TOKEN
shockerIds=00000000-0000-0000-0000-000000000000
~~~

Replace the placeholder with your actual shocker UUID. For multiple IDs, separate
with commas. Never publish your real token. Restart the game, press O to configure
trigger toggles, and explicitly enable physical output only with the wearer's consent.

Other options are saved in the instance at config/ShockCraft-legacy.properties.
The token remains in your personal account directory, outside exported modpacks.
It is not encrypted: secure your operating-system login.

### Other Forge versions: implementation tracker

This table is **a roadmap, not a list of working jars**. Every supported version
requires its own matching Forge/Java/toolchain, API adaptation, and real client test.

| Minecraft | Planned Forge versions | Status |
|---|---|---|
| 1.12 | 1.12.2 | Source added; build and game tests pending |
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
