# IngameIME for Minecraft Forge 1.6.4

IngameIME is an input-method bridge for Minecraft. This fork ports the core IngameIME client behavior to **Minecraft 1.6.4 + Forge 9.11.1.1345** so Chinese and other IME-based text input can be used in fullscreen or windowed Minecraft.

This 1.6.4 port uses a Forge coremod/ASM transformer instead of the mixin-based architecture used by newer IngameIME versions.

## Status

Current scope: **core stable 1.6.4 port**.

Verified in this port:

- Minecraft Forge 1.6.4 client loading
- Coremod transformer loading from the jar manifest
- Windows native IME library loading
- TSF/Imm32 input context creation and activation lifecycle
- Vanilla `GuiTextField` input, including chat and creative-search text fields
- Sign input
- Writable book input
- Preedit overlay rendering near the caret
- Native Windows candidate UI by default
- Fullscreen/windowed IME context reset handling
- Control-character filtering so creative-search ESC/Enter does not insert text

Not included in this 1.6.4 MVP:

- The 1.12.2 theme editor/config GUI
- Broad third-party text-field compatibility layers
- Backporting newer mixin resources

## Requirements

- Windows
- Minecraft `1.6.4`
- Forge `1.6.4-9.11.1.1345`
- Java 8 for building and running the legacy Forge client

The project compiles source/target as Java 7 bytecode because Forge 1.6.4 uses ASM 4.1 during mod discovery and cannot parse Java 8 class files reliably.

## Build

Use Java 8, not Java 21+:

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew clean assemble --console=plain --no-daemon
```

Output jar:

```text
build/libs/IngameIME-1.0.0-1.6.4.jar
```

For development launch:

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew runClient --console=plain --no-daemon
```

## Installation

Copy the release jar to the Forge 1.6.4 instance `mods/` directory:

```text
IngameIME-1.0.0-1.6.4.jar
```

The jar manifest includes the required coremod attributes:

```text
FMLCorePlugin: com.dhj.ingameime.core.IngameIMECorePlugin
FMLCorePluginContainsFMLMod: true
ForceLoadAsMod: true
```

## Configuration

The config file is generated at:

```text
config/ingameime.cfg
```

Important options:

```text
S:Windows=TextServiceFramework
B:Windows=false
B:DebugLog=false
B:VerboseLog=false
```

Recommended defaults:

- `S:Windows=TextServiceFramework` — use Windows TSF backend.
- `B:Windows=false` under the `uiless` category — use the native Windows candidate window while IngameIME renders the preedit text in-game.
- Set `B:DebugLog=true` and `B:VerboseLog=true` only when troubleshooting.

`B:Windows=true` under `uiless` asks IngameIME to render candidate UI in-game. This is useful as a fallback, especially for fullscreen/native UI edge cases, but the native Windows candidate UI is the preferred default for this port.

## Notes for maintainers

- `IngameIMETransformer` replaces the newer mixin hooks with LaunchWrapper-era ASM patches.
- `Internal` owns native library loading, HWND lookup, IME activation, callback queues, commit filtering, and gameplay inactive suppression.
- `VanillaTextFieldControl`, `SignControl`, and `BookControl` are the supported controls in this MVP.
- `ChatAllowedCharacters` is patched to allow non-ASCII text while rejecting control characters such as ESC and Enter.
- Keep generated class files Java-7-compatible.

## Credits

Based on the IngameIME project and its native JNI bindings. Credits listed in mod metadata:

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
