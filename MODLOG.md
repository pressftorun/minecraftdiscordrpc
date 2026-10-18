# World Presence

- Target: Minecraft Java 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Java 25.
- Route: client-only Fabric events; no mixins or server installation.
- Discord: local Windows named-pipe IPC, Gson already provided by Minecraft.
- Session timer resets on disconnect; dimension changes keep the session.
- Build completed (Gradle build task); runnable formatting checks passed.
- Isolated runClient loaded Minecraft 26.3, Fabric and world_presence 1.0.0;
  initializer created run/config/world-presence.json and reported the missing Application ID.
- Live Discord status and in-world telemetry not verified: no Application ID supplied.
- Windows Java selector failed with default temp settings. Explicit
  -Djdk.net.unixdomain.tmpdir=E:/codexprojects/discordrpcminecraft/.tools/tmp fixed Gradle startup.
- Minecraft emitted Windows performance counter warnings unrelated to this mod.
- Test client subsequently exited with native Windows error 0xC0000005 during
  resource loading; no hs_err or Minecraft crash report found in the workspace.
  In-game verification remains incomplete. A separate final build succeeded.
- No user saves or launcher profiles modified.

## 1.1.0

- Optional Mod Menu 21.0.0 integration uses native Minecraft Screen/EditBox/Button.
- Application ID can be validated, saved or cleared in-game. Replaces the IPC client
  immediately, retaining the world session timer; missing ID no longer skips event setup.
- Outside a world, RPC publishes «В главном меню» with no old world stats/timestamp.
- Added runnable checks for Application ID and the menu payload.
- Player nickname added to both world and menu activity, with checks.
- 1.1.0 build succeeded; selfTest passed for ID validation, nickname, menu payload,
  time formatting and Discord text length. Configuration screen compiles against
  Minecraft 26.3 and Mod Menu 21.0.0 APIs.
- Live Discord and visual screen verification remain pending (no supplied Application ID;
  previous isolated client launch encountered native 0xC0000005).

## 1.1.1 — correctness audit

- User-facing presence uses plain text and | separators, without emoji.

- Confirmed with a real Windows named-pipe fixture: RandomAccessFile normalized the
  extended path prefix away, so the old IPC path could not connect. Device namespace
  \\.\pipe\ fixes it.
- Confirmed with jcmd Thread.print: synchronous read, write and close on the same
  Windows handle deadlocked. Replaced the reader thread with one worker polling
  available() (PeekNamedPipe); only available bytes are read. Shutdown is queued on
  the worker and never waits on the Minecraft thread. No new runtime dependency.
- IPC now assembles fragmented frames, bounds frame sizes, coalesces repeated snapshots,
  handles malformed input without losing the scheduled task, and reconnects after errors.
- Session start changes only on JOIN/DISCONNECT, not transient null world/player states.
- Reserved space for biome in the details line when the server address is long.
- ID paste no longer truncates at 20 characters. Reject leading-zero and >uint64 IDs.
- Config is replaced through a sibling temporary file; a failed write preserves prior data.
- Game clock conversion handles extreme long values without overflow.
- Final build succeeded. Tests passed: text/time boundaries; config save/replacement,
  disable, invalid ID and failed save; Windows IPC handshake with fragmented frames,
  UTF-8, ping/pong, world→menu, disconnect/reconnect, ID replacement and shutdown.
- Native game rendering and real Discord display were not reverified; the previous
  native client crash remains an external verification limitation, not a diagnosed cause.

## 1.2.0

- User chose to keep two compound text lines after clarification of Discord's layout.
- First field: nickname + singleplayer/server. Second field: biome + health + food + game time.
- The existing Paint-style cover is bundled as assets/world_presence/icon.png and referenced
  by fabric.mod.json for Mod Menu. No new generated image or external upload needed.

## 1.3.0

- Added Check ID button to Mod Menu settings. Checks the unsaved draft using a
  separate handshake-only DiscordIpc connection, sharing the production frame parser.
- READY confirms acceptance; CLOSE/ERROR reports rejection with code; absence and
  transport errors remain distinct from invalid IDs. Timeout after 10 seconds.
- No test activity is sent. Editing the draft or leaving the screen cancels the
  future and closes the probe worker. UI reads results on the Minecraft thread.
- Added isolated pipe scenarios for READY, CLOSE rejection, ERROR rejection,
  unavailable Discord, timeout and cancellation, asserting no activity publication.

## 1.4.0

- Added block X/Y/Z from the same player.blockPosition() snapshot used for biome.
- Reserved text budget for coordinates, health, food and time; shorten biome when needed.
- Menu and world RPC now request assets.large_image=rpc_cover. The user must upload
  this asset in the matching Discord application's Rich Presence Art Assets section;
  General Information app icon is separate. Local Mod Menu icon stays bundled.
- Added checks for negative/border coordinates, length bounds and cover asset references.

## 1.4.1

- Sample world data every second instead of every 15 seconds. The existing IPC
  worker sends changed snapshots on its next 100 ms poll; unchanged snapshots keep
  the existing 15-second heartbeat. Reconnection timing remains unchanged.

## 1.5.0

- Removed coordinates, health and food from activity collection and text.
- Added vanilla dimension names and fixed Russian biome names from the local
  Minecraft 26.3 Russian language asset, independent of the selected game language.
  Unknown or modded IDs use Russian fallback labels.
- Translated mod display name, cover tooltip and remaining settings labels to Russian.
- Preserved nickname, server, game clock, session timer and one-second sampling.

## 1.5.1

- Replaced numeric game clock with Russian time-of-day labels: morning 0–5999,
  day 6000–11999, evening 12000–13999, night 14000–23999 ticks.
- Added checks at every boundary, day wrap, negative and extreme tick values.

## 1.5.2+1.21.11 — 2026-10-16

- Ported local 1.5.1 from Minecraft 26.3 to Minecraft 1.21.11 / Fabric.
- Loom 1.17.1 remapping plugin with official Mojang mappings; Fabric API
  0.141.6+1.21.11, optional Mod Menu 17.0.1, Java 21 toolchain.
- Adapted settings to Screen.render / GuiGraphics.drawCenteredString and
  game time to Level.getDayTime. Kept ResourceKey.identifier, RPC text,
  session lifecycle, config format and Windows IPC behavior.
- Baseline and target build passed, including all existing self-tests.
- Isolated JDK 21 dev client loaded the mod and Mod Menu, initialized rendering
  and shut down successfully. No real Discord activity was published.
- Release metadata and Java 21 bytecode checked. Settings/world rendering and
  actual Discord visibility remain manually unverified; screenshot capture failed.
- Source archive, SHA hashes, adaptation matrix and test evidence are recorded
  in docs/MIGRATION_STATUS.md. No publication performed.

## 2026-10-18: NeoForge 26.3 port
Source 1.5.2+1.21.11 archived in .tools/port-reference-1.5.2-1.21.11.zip.
Route: NeoForge loader API; ModDevGradle 2.0.148, NeoForge 26.3.0.90, Java 25.
Replaced Fabric lifecycle/tick/network events and Mod Menu integration with NeoForge equivalents. Updated 26.3 GUI/time API calls. Existing RPC/text/config behavior retained.
Verification pending; see docs/MIGRATION_STATUS.md.

First NeoForge build compiled mod successfully; test compilation initially lacked game dependencies. Fixed with neoForge.addModdingDependenciesTo sourceSets.test. Full build and all existing self-tests passed. Client smoke script adapted to client.gui.screen()/overlay() in 26.3; assets downloading.
Client verification: first launch crashed in native SDL3.dll (Windows APPCRASH 0xC000041D). Disabled earlyWindowControl in isolated .tools profile (backup .tools/fml-26.3-before-smoke.toml); no user profile changed. Fixed deprecated metadata logoFile → iconFile. Smoke script initially waited for TitleScreen but fresh profiles show AccessibilityOnboardingScreen; now opens settings after resource load and validates return to the actual parent. Registered NeoForge config factory, rendering, ticking and return passed; screenshot inspected at .tools/run-26.3-neoforge/screenshots/2026-10-18_17.59.29.png. Client stopped normally, BUILD SUCCESSFUL. Final normal build excludes .tools/smoke-src. In-world behavior and real Discord display remain unverified.
Release JAR inspected: only NeoForge metadata, no Fabric/Mod Menu/PortSmoke or Minecraft classes; Java major 69; SHA-256 fc7e8072fd10ea3170194efedd2da8bd145d3c07b104a17610571714dd4a857c.
