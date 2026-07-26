# IngameIME for Minecraft Forge 1.6.4

[English / [简体中文](README.zh_CN.md)]

IngameIME is a client-side mod that brings input method support to Minecraft. This fork ports the core functionality of [IngameIME-1.12.2](https://github.com/DHJComical/IngameIME-1.12.2) to **Minecraft 1.6.4 + Forge 9.11.1.1345**.

- **✓** Chinese input
- **✓** IME candidate window
- **✓** Small - window/Full - screen Support
- **✓** No input-method lock-up during gameplay

> When no GUI is open, IngameIME automatically deactivates the native input context to prevent Chinese IMEs from intercepting gameplay keys such as WASD and Space. Therefore, even if the system is currently in Chinese input mode, players can still move, sprint, and control the game normally after closing chat or other input screens, without having movement keys swallowed by the input method.

## Showcase

- **Ask IngameIME to render the candidate window and preedit text in-game (default)**

Set this in the config file:

```cfg
uiless {
    # Config if render in-game candidate list.
    B:Windows=true
}
```

![](show-1.png)

- **Use the native Windows candidate window**

Set this in the config file:

```cfg
uiless {
    # Config if render in-game candidate list.
    B:Windows=false
}
```

![](show-2.png)

## Current status

Verified in this port:

- Minecraft Forge 1.6.4 client loads correctly
- The coremod transformer loads correctly from the jar manifest
- Windows native IME libraries load correctly
- TSF/Imm32 input contexts can be created, activated, deactivated, and reused
- Vanilla `GuiTextField` input works, including chat and the creative-mode search box
- Sign input works
- Writable book input works
- Pinyin/preedit text can be displayed near the caret
- Candidate windows are rendered in-game by default
- The IME context is reset when switching between fullscreen and windowed mode
- Control characters are filtered so ESC/Enter no longer insert abnormal characters into the creative-mode search box

This 1.6.4 MVP **does not include**:

- The theme editor and config GUI from the 1.12.2 version
- Broad third-party mod text-field compatibility layers
- Backporting newer mixin resources or the mixin architecture

## Requirements

- Windows
- Minecraft `1.6.4`
- Forge `1.6.4-9.11.1.1345`
- Java 8, for building and running the legacy Forge client

This project still compiles to Java 7 bytecode:

```text
sourceCompatibility = 1.7
targetCompatibility = 1.7
```

The reason is that Forge 1.6.4 uses ASM 4.1 during mod discovery, which cannot reliably parse Java 8 class files.

## Build

> This assumes you are using Zulu 8 installed at the default path.

Use Java 8. Do not use Java 21 or newer:

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew clean assemble --console=plain --no-daemon
```

Build output:

```text
build/libs/IngameIME-x.x.x-1.6.4.jar
```

Launch the development client:

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew runClient --console=plain --no-daemon
```

This repository ships a built-in CME patch launcher, so `runClient` / `runServer` work out of the box on Java 8u20 and newer without manually dropping LegacyJavaFixer into `runs/main/client/mods/`. See "CME patch for the dev environment" below.

### CME patch for the dev environment (`src/launchPatch/`)

On JVMs running **Java 8u20 or newer**, the stock 1.6.4 launch chain always dies with:

```text
java.util.ConcurrentModificationException
    at java.util.ArrayList$Itr.checkForComodification(ArrayList.java:911)
    at java.util.ArrayList$Itr.remove(ArrayList.java:875)
    at net.minecraft.launchwrapper.Launch.launch(Launch.java:114)
```

Three things combine to cause it:

1. `Launch.launch()` in launchwrapper 1.8 calls `tweaker.acceptOptions(...)` inside the loop body and then `it.remove()` — the iterator stays alive across the callback.
2. `CoreModManager.sortTweakList()` in 1.6.4 FML performs a bare `Collections.sort(tweakers, cmp)` on exactly the list that iterator is walking.
3. [JDK-8030848](https://bugs.openjdk.org/browse/JDK-8030848) changed `Collections.sort` in **8u20**: it used to copy into a temporary array, sort, and write back without touching `modCount`; after delegating to `List.sort()`, `ArrayList.sort()` unconditionally does `modCount++` at the end.

So `it.remove()` throws a CME in `checkForComodification()`. The cutoff is 8u20, not "Java 8" — early builds such as 8u5 still run. Forge fixed this in the 1.7.10 era (`sortTweakList` switched to `Arrays.sort` plus `set` write-back); 1.6.4 was already out of maintenance and never got it.

This repository fixes it on the launchwrapper side instead: `src/launchPatch/java/dev/launchfix/CmeSafeLaunch.java` is an equivalent implementation of `Launch` that replaces "hold an iterator across the callback" with "take the head of the list each round, then remove it by reference", so sorting or mutating the list inside a callback is safe. Wiring:

```groovy
sourceSets.register('launchPatch')
sourceSets.launchPatch.compileClasspath = sourceSets.main.compileClasspath
sourceSets.main.runtimeClasspath += sourceSets.launchPatch.output

// inside the run blocks
environment.put("mainClass", "dev.launchfix.CmeSafeLaunch")
```

Three details worth knowing:

- **Why the env `mainClass` instead of the run DSL `mainClass`**: slime-launcher's `LegacyDev` only kicks in when the outer main class starts with `net.minecraftforge.legacydev.`. It reads the env `mainClass` to find the real entry point and supplies `--tweakClass`/`--version`/`--assetsDir`. Changing the DSL `mainClass` bypasses the whole legacydev path, dropping `--tweakClass` and falling back to `VanillaTweaker`.
- **Why append to `main.runtimeClasspath`**: the run task JVM classpath is the slime-launcher tool jar plus `main.runtimeClasspath`. The run DSL `classpath` has **whole-value override** semantics, so using it evicts slime-launcher itself and the run fails with a missing main class `net.minecraftforge.launcher.Main`.
- **Why a separate sourceSet**: the `jar` task packages `sourceSets.main.output`; the patch lives in `launchPatch` with a separate output directory, so it **never reaches the published jar** (verified). Appending to `runtimeClasspath` only affects run/test, not the `jar` inputs. It also keeps FML from scanning it as a mod class in dev.

**Distribution note**: this patch only covers the dev `runClient`/`runServer`. Players running this mod on a production Forge install with 8u20+ still need [`legacyjavafixer-1.0.jar`](https://github.com/MinecraftForge/LegacyJavaFixer), which patches `sortTweakList` on the FML side — the other end of the same bug.

## Configuration

The config file is generated at:

```text
config/ingameime.cfg
```

The config file uses Forge 1.6.4's `Configuration` format:

- `S:` means string
- `B:` means boolean, and only `true` or `false` is valid
- `I:` means integer
- Colors use ARGB hexadecimal format, for example `0xFF000000`

<details>
<summary>Full config file example (click to expand)</summary>

```cfg
# Configuration file

####################
# api
####################

api {
    # The input method API used on Windows.
    # Available values:
    # - TextServiceFramework: recommended. Uses TSF and works well with modern Windows IMEs.
    # - Imm32: legacy IMM32 backend, provided as a compatibility fallback.
    S:Windows=TextServiceFramework
}


####################
# debug
####################

debug {
    # Whether to print normal debug logs.
    # Recommended value for daily use: false.
    # Set to true when troubleshooting.
    B:DebugLog=false

    # Whether to print more verbose troubleshooting logs.
    # This records IME callbacks, candidate windows, preedit text, caret positions, and more.
    # It can produce a large amount of log output.
    # Recommended value for daily use: false.
    # Enable it only when you need to submit logs or diagnose an issue.
    B:VerboseLog=false
}


####################
# general
####################

general {
    # Whether to automatically turn off the input method when the mouse moves.
    # true: turn off IME after mouse movement.
    # false: mouse movement does not affect IME state. This is the recommended default.
    B:TurnOffOnMouseMove=false
}


####################
# modetext
####################

modetext {
    # Text shown for English/alphabet mode.
    S:AlphaMode=A

    # Text shown for Chinese/native input mode.
    S:NativeMode=中
}


####################
# theme
####################

theme {
    # Background color for preedit text/candidate windows, in ARGB.
    S:BackgroundColor=0xEBEBEBEB

    # Border color, in ARGB.
    S:BorderColor=0x80000000

    # Border width, in game GUI pixels.
    I:BorderWidth=1

    # Candidate window padding, in game GUI pixels.
    I:CandidatePadding=5

    # Preedit caret color, in ARGB.
    S:CursorColor=0xFF000000

    # Candidate index color, in ARGB.
    S:IndexColor=0xFF555555

    # Generic widget padding, in game GUI pixels.
    I:Padding=3

    # Background color for the selected candidate item, in ARGB.
    S:SelectedBackgroundColor=0xEBEBEBEB

    # Text color, in ARGB.
    S:TextColor=0xFF000000
}


####################
# uiless
####################

uiless {
    # Windows candidate-window rendering mode.
    # true: default. Ask IngameIME to render the candidate window and preedit text in-game.
    # false: use the native Windows candidate window while IngameIME still renders preedit text in-game;
    #        this can be used as a fallback if the in-game candidate window behaves incorrectly.
    B:Windows=true
}
```

</details>

## Features

### Vanilla text fields

Supports vanilla `GuiTextField`, including:

- Chat input box
- Creative-mode item search box
- Other screens based on vanilla `GuiTextField`

The creative-mode search box has extra handling for ESC and Enter:

- Pressing ESC should not insert control characters or `esc` text
- Pressing Enter should not insert control characters or `cr` text
- Normal Chinese input should still be committed into the search box

### Signs and books

This port includes 1.6.4-specific controls:

- `SignControl`: supports sign input
- `BookControl`: supports writable book input

### Gameplay input state

When no GUI is open, IngameIME keeps the native input context inactive to prevent Chinese IMEs from swallowing gameplay keys such as WASD.

## Maintainer notes

- `IngameIMETransformer` replaces newer mixin hooks with LaunchWrapper-era ASM injection.
- `Internal` is responsible for native library loading, HWND discovery, IME activation state, callback queues, commit filtering, and forced inactive state during gameplay.
- The controls supported by the current MVP are:
  - `VanillaTextFieldControl`
  - `SignControl`
  - `BookControl`
- `ChatAllowedCharacters` is patched to allow non-ASCII text while rejecting control characters such as ESC and Enter.
- Keep generated class files compatible with Java 7. Java 7 bytecode (class version 51) is a hard ceiling because 1.6.4 FML's bundled ASM 4.1 cannot parse Java 8 bytecode (52) and would throw `IllegalArgumentException`, dropping the whole mod. This constraint applies to the **compiled output** only, not to the JDK used to run Gradle.
- Do not directly introduce the 1.12.2 mixin architecture; the 1.6.4 port should continue using the existing coremod/ASM approach.
- `src/launchPatch/` serves the dev runtime only and never ends up in the published jar; do not move it into `src/main/`.

## Troubleshooting

**`ConcurrentModificationException` at `net.minecraft.launchwrapper.Launch.launch` on startup**
You are on 8u20+ and the CME patch launcher is not being used. Check that the run blocks in `build.gradle` still contain `environment.put("mainClass", "dev.launchfix.CmeSafeLaunch")` and that `sourceSets.main.runtimeClasspath += sourceSets.launchPatch.output` is still present.

**`runServer` fails with `UnsatisfiedLinkError: no lwjgl in java.library.path`**
The server run block uses the client `FMLTweaker`. Its `getLaunchTarget()` returns `net.minecraft.client.main.Main`, so `runServer` actually starts the client and dies on the LWJGL native libraries. Use `FMLServerTweaker` instead.

**FML logs `Unable to read a class file correctly` / `IllegalArgumentException`**
There is a jar with Java 8+ bytecode on the classpath that ASM 4.1 cannot read. FML skips the entry and continues, which is usually harmless; if the dropped entry is this mod, check whether `-source/-target` was bumped to 1.8.

## Credits

Based on the IngameIME project and its native JNI bindings. Credits listed in the mod metadata:

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
