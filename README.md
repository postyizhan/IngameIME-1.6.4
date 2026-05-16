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
- Keep generated class files compatible with Java 7.
- Do not directly introduce the 1.12.2 mixin architecture; the 1.6.4 port should continue using the existing coremod/ASM approach.

## Credits

Based on the IngameIME project and its native JNI bindings. Credits listed in the mod metadata:

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
