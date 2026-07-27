# IngameIME for FishModLoader (Minecraft 1.6.4-MITE)

[English / [简体中文](README.zh_CN.md)]

IngameIME is a client-side mod that brings input method support to Minecraft. This fork ports the core functionality of [IngameIME-1.12.2](https://github.com/DHJComical/IngameIME-1.12.2) to **Minecraft 1.6.4-MITE + FishModLoader 3.4.2**.

- **✓** Chinese input
- **✓** IME candidate window
- **✓** Small - window/Full - screen Support
- **✓** No input-method lock-up during gameplay

> When no GUI is open, IngameIME automatically deactivates the native input context to prevent Chinese IMEs from intercepting gameplay keys such as WASD and Space. Therefore, even if the system is currently in Chinese input mode, players can still move, sprint, and control the game normally after closing chat or other input screens, without having movement keys swallowed by the input method.

## Showcase

- **Ask IngameIME to render the candidate window and preedit text in-game (default)**

Set this in `config/ingameime.json`:

```json
"uiless": {
  "Windows": { "value": true }
}
```

![](show-1.png)

- **Use the native Windows candidate window**

```json
"uiless": {
  "Windows": { "value": false }
}
```

![](show-2.png)

## Requirements

- Windows
- Minecraft `1.6.4-MITE`
- FishModLoader `>=3.4.0`
- Java 17 or newer

## Build

```bash
./gradlew build
```

Build output:

```text
build/libs/IngameIME-1.0.0.jar
```

Launch the development client:

```bash
./gradlew runClient
```

## Configuration

The config file is generated at:

```text
config/ingameime.json
```

It uses FishModLoader's own JSON config format. Each entry is an object with a
`value` field and an optional `_comment` describing it. Colors are ARGB
hexadecimal strings such as `"0xFF000000"` — they are stored as strings because
values like `0xEBEBEBEB` exceed the range of a signed 32-bit integer and would be
unreadable in decimal.

| Category | Key | Type | Default | Meaning |
| --- | --- | --- | --- | --- |
| `api` | `Windows` | string | `TextServiceFramework` | Input method backend. `TextServiceFramework` (recommended) or `Imm32` (legacy fallback). |
| `uiless` | `Windows` | boolean | `true` | `true` renders the candidate list in-game; `false` uses the native Windows candidate window. |
| `general` | `TurnOffOnMouseMove` | boolean | `false` | Turn the input method off when the mouse moves. |
| `modetext` | `AlphaMode` | string | `A` | Indicator text shown in alphanumeric mode. |
| `modetext` | `NativeMode` | string | `中` | Indicator text shown in native-language mode. |
| `debug` | `DebugLog` | boolean | `false` | Print normal debug logs. |
| `debug` | `VerboseLog` | boolean | `false` | Print verbose troubleshooting logs (IME callbacks, candidates, preedit, caret positions). Very noisy. |
| `theme` | `TextColor` | color | `0xFF000000` | Preedit/candidate text color. |
| `theme` | `BackgroundColor` | color | `0xEBEBEBEB` | Widget background color. |
| `theme` | `IndexColor` | color | `0xFF555555` | Candidate index number color. |
| `theme` | `SelectedBackgroundColor` | color | `0xEBEBEBEB` | Background color of the selected candidate. |
| `theme` | `CursorColor` | color | `0xFF000000` | Preedit caret color. |
| `theme` | `BorderColor` | color | `0x80000000` | Widget border color. |
| `theme` | `Padding` | int | `3` | Widget padding. |
| `theme` | `CandidatePadding` | int | `5` | Candidate entry padding. |
| `theme` | `BorderWidth` | int | `1` | Widget border width. Set to `0` to disable the border. |

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

## Architecture

The mod hooks the game through Mixins and an AccessWidener. There is no coremod
and no runtime reflection against Minecraft members.

| Hook | Location | Purpose |
| --- | --- | --- |
| `MinecraftMixin` | `displayGuiScreen` HEAD/RETURN | Drive the IME state machine on screen close/open. |
| `MinecraftMixin` | `toggleFullscreen` HEAD/RETURN | Destroy and recreate the input context, since the HWND changes. |
| `MinecraftMixin` | `runTick` RETURN | Client tick end: drain callback/commit queues, poll toggle key and mouse move. |
| `MinecraftImposedChatMixin` | `openChat`, `closeImposedChat` | MITE's chat lifecycle (see below). |
| `EntityRendererMixin` | `updateCameraAndRender` RETURN | Draw the preedit/candidate overlay. |
| `GuiTextFieldMixin` | `setFocused` HEAD | Track focus changes to activate/deactivate the IME. |
| `GuiTextFieldMixin` | `textboxKeyTyped` HEAD | Suppress IME-emitted key-name sequences. |
| `GuiContainerCreativeMixin` | `keyTyped` HEAD | Arm control-key detection for the creative search box. |
| `ChatAllowedCharactersMixin` | `isAllowedCharacter`, `filerAllowedCharacters` | Allow non-ASCII text while rejecting control characters. |
| `FontRendererMixin` | `getCharWidth`, `renderStringAtPos` | Keep CJK on the unicode-glyph path (see below). |
| `GuiScreenAccessor` | `@Invoker` on `keyTyped` | Fallback path that feeds committed text to the current screen. |

`ingameime.accesswidener` widens the private fields that the controls read for
caret positioning (`GuiTextField`, `GuiScreenBook`, `GuiEditSign`) plus the two
private `GuiScreenBook` methods used for book editing.

`Internal` is responsible for native library loading, HWND discovery, IME
activation state, callback queues, commit filtering, and forced inactive state
during gameplay.

### Why the overlay is drawn in `updateCameraAndRender`

The original Forge port injected before `checkGLError("Post render")` in
`Minecraft.runGameLoop()`. In 1.6.4 that call site sits **after**
`Display.update()`, i.e. after the buffer swap. Injecting at the RETURN of
`EntityRenderer.updateCameraAndRender()` instead puts the overlay right after the
current screen is drawn and still before the swap, so it is reliably visible.

### MITE's imposed chat

MITE does not open chat through `displayGuiScreen()`. `Minecraft.openChat(GuiChat)`
writes the screen into a separate `imposed_gui_chat` field and calls
`setWorldAndResolution` directly; closing goes through `closeImposedChat()`.
`currentScreen` is never touched.

So anything keyed on `currentScreen != null` treats chat as "no screen open",
which disabled the IME in chat entirely: the state machine returned early, the
tick handler forced the context inactive every tick, and the overlay refused to
draw. `ActiveScreen` resolves the union of `currentScreen` and `imposed_gui_chat`,
and `MinecraftImposedChatMixin` supplies the open/close notifications that
`displayGuiScreen` never sends for chat.

## Notes on the runtime name mapping

FishModLoader remaps the game jar `official` → `named` at launch
(`net.xiaoyu233.fml.relaunch.Launch`), so Minecraft classes and members keep
readable names at runtime. Mod code is **not** remapped and no refmap is
generated, which is why the Mixin targets in this repository use plain named
strings such as `runTick` and `net/minecraft/GuiTextField`.

Note that MITE uses a flat `net.minecraft.*` package: it is
`net.minecraft.GuiTextField`, not `net.minecraft.client.gui.GuiTextField`.

## Troubleshooting

**`ArrayIndexOutOfBoundsException` in `FontRenderer.getCharWidth` / `renderDefaultChar`**

Fixed by `FontRendererMixin`. If you see it again, that mixin failed to apply.

The cause: vanilla `FontRenderer` resolves a character by its index in
`ChatAllowedCharacters.allowedCharacters`, then uses that index against a
256-entry `charWidth[]` and the `ascii.png` atlas; a negative index falls through
to the unicode-glyph path. Vanilla's `font.txt` is exactly 144 characters, so CJK
always missed and rendered via `unicode_page_XX.png`.

FishModLoader's `fix.AllowedCharFix` swaps in its own `font.txt` — the same first
144 characters plus roughly 28,000 CJK ones. CJK then *does* resolve to an index,
so indices 144-223 silently drew the wrong glyph from `ascii.png`, and anything at
224 or above overflowed:

```text
java.lang.ArrayIndexOutOfBoundsException: Index 16469 out of bounds for length 256
```

(`中` sits at index 16437 in FML's `font.txt`; 16437 + 32 = 16469.)

This fires on this mod's candidate window and equally on stock screens such as the
language list, the resource-pack list, and the create-world name field. The mixin
redirects both `indexOf` call sites so any index at or beyond 144 returns `-1`,
restoring the unicode-glyph path. ASCII and Latin-1 behaviour is untouched, and
the game jar ships all 222 `unicode_page_*.png` files plus `glyph_sizes.bin`, so
CJK renders correctly.

**The candidate window does not appear**

Check that `uiless.Windows` is `true`. When it is `false` the native Windows
candidate window is used instead of the in-game overlay.

**No IME at all, and the log says the native library failed to load**

The mod ships `IngameIME_Java-{x64,x86,arm64}.dll` and loads the one matching
`os.arch`. Only Windows is supported; on other platforms the log reports the
unsupported platform and the mod stays inert.

## Credits

Based on the IngameIME project and its native JNI bindings. Credits listed in the mod metadata:

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
