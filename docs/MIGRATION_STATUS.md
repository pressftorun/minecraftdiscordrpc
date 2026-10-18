# World Presence: Fabric 1.21.11 → NeoForge 26.3

Source of truth: local World Presence 1.5.2 sources, preserved in
`.tools/port-reference-1.5.2-1.21.11.zip` (SHA-1
`73C9F2399955ABBB4FE5E7B58A447C3E410B2F3C`). No license is present;
this is a local port without publication. Metadata uses All Rights Reserved.
The older 26.3 Fabric snapshot was consulted only for Minecraft API names.

Target: Minecraft 26.3, NeoForge 26.3.0.90, ModDevGradle 2.0.148,
Java 25, Gradle 9.6.0. No Fabric API or Mod Menu dependency.

| Area | Adaptation |
| --- | --- |
| Build / metadata | ModDevGradle, neoforge.mods.toml, client-only @Mod |
| Config path | FMLPaths.CONFIGDIR, same world-presence.json format |
| Sessions | ClientPlayerNetworkEvent.LoggingIn / LoggingOut |
| Sampling | ClientTickEvent.Post, same one-second cadence |
| Shutdown | GameShuttingDownEvent closes IPC |
| Settings | IConfigScreenFactory in NeoForge mod list |
| Minecraft 26.3 | GuiGraphicsExtractor, extractRenderState, centeredText, setScreenAndShow, getOverworldClockTime |
| RPC / text / config persistence | Existing implementation and self-tests retained |

Preserved: Russian text, main-menu activity, session timer, server address,
biome and dimension names, time-of-day boundaries, image key, ID validation,
atomic config writes, reconnect and probe cancellation. Respawn and dimension
changes do not restart the session timer.

Verification on 2026-10-18:

- `gradlew.bat build`: passed on Java 25.
- Existing self-tests passed: text/time boundaries, config persistence, Windows named-pipe
  handshake, UTF-8, ping/pong, menu, reconnect, ID change, shutdown, ID acceptance/
  rejection, unavailable Discord, timeout and cancellation. No activity sent to real Discord.
- Isolated NeoForge client smoke test passed (`BUILD SUCCESSFUL`): the registered
  IConfigScreenFactory opened ConfigScreen; it rendered/ticked for ten seconds,
  returned to the original onboarding screen and stopped the client normally.
- Screenshot inspected: `.tools/run-26.3-neoforge/screenshots/2026-10-18_17.59.29.png`.
  Russian text, field and buttons render correctly.
- Initial startup crashed in native SDL3.dll (Windows APPCRASH, 0xC000041D).
  Disabling earlyWindowControl in the isolated profile's config/fml.toml allowed
  startup. Backup: `.tools/fml-26.3-before-smoke.toml`. No player profile changed.
- Replaced deprecated logoFile metadata with iconFile after the client warning.
- Smoke helper source and Gradle init script live under .tools only and are not
  part of the release JAR. Repeat: `gradlew.bat -I .tools/neoforge-smoke.init.gradle runClient`.
- World telemetry, respawn/dimension transitions and actual Discord visibility
  remain unverified in game; the RPC protocol is covered by named-pipe tests.

Build (PowerShell):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot'
$env:GRADLE_USER_HOME = 'C:/Users/pressftorun/.gradle'
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$((Resolve-Path .tools/tmp).Path)"
.\gradlew.bat build
```

Development client: `.\gradlew.bat runClient`; isolated game directory:
`.tools/run-26.3-neoforge`. Keep Application ID empty during smoke tests.

Official references:
- https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle
- https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml
- https://github.com/neoforged/NeoForge/tree/26.3.x

Release artifact: `build/libs/world-presence-1.5.3+26.3-neoforge.jar`.
SHA-256: `fc7e8072fd10ea3170194efedd2da8bd145d3c07b104a17610571714dd4a857c`.
JAR contents checked: NeoForge metadata expanded, Java class major 69 (Java 25),
no Fabric metadata, Mod Menu class, smoke helper or bundled Minecraft classes.
