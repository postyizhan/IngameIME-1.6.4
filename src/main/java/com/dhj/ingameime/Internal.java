package com.dhj.ingameime;

import com.dhj.ingameime.config.Config;
import ingameime.*;
import net.minecraft.Minecraft;
import net.minecraft.ScaledResolution;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Internal {
    public static boolean LIBRARY_LOADED = false;
    public static InputContext InputCtx = null;
    private static boolean ACTIVATED = false;
    private static boolean GAMEPLAY_INACTIVE_LOGGED = false;
    private static boolean GAMEPLAY_INACTIVE_FAILURE_LOGGED = false;
    private static boolean GAMEPLAY_INACTIVE_CREATE_ATTEMPTED = false;
    private static int CONTEXT_GENERATION = 0;
    private static int ACTIVATION_GENERATION = 0;

    private static PreEditCallbackImpl preEditCallbackProxy = null;
    private static CommitCallbackImpl commitCallbackProxy = null;
    private static CandidateListCallbackImpl candidateListCallbackProxy = null;
    private static InputModeCallbackImpl inputModeCallbackProxy = null;
    private static PreEditCallback preEditCallback = null;
    private static CommitCallback commitCallback = null;
    private static CandidateListCallback candidateListCallback = null;
    private static InputModeCallback inputModeCallback = null;
    private static final Queue<Runnable> CALLBACK_QUEUE = new ConcurrentLinkedQueue<Runnable>();
    private static final Queue<CommitText> COMMIT_QUEUE = new ConcurrentLinkedQueue<CommitText>();
    private static final long CONTROL_TYPED_SEQUENCE_TIMEOUT_MS = 1200L;
    /** 复用的 PreEditRect 与上次下发的值，用于避开每帧重复的 JNI 调用与原生内存分配。 */
    private static PreEditRect preEditRect = null;
    private static int lastRectX = Integer.MIN_VALUE;
    private static int lastRectY = Integer.MIN_VALUE;
    private static int lastRectWidth = Integer.MIN_VALUE;
    private static int lastRectHeight = Integer.MIN_VALUE;
    private static Object controlTypedSequenceField = null;
    private static StringBuilder controlTypedSequence = new StringBuilder();
    private static long controlTypedSequenceStartedAt = 0L;

    /**
     * 把打包在 jar 里的原生库释放到临时目录并加载。
     *
     * 用固定文件名而不是 createTempFile：Windows 上 System.load 会一直持有文件句柄，
     * deleteOnExit 删不掉，旧实现会在 %TEMP% 里每启动一次就留下一个 1~2MB 的 dll。
     * 固定名 + 已存在则复用（写入失败往往意味着文件正被另一个实例占用）。
     */
    private static void tryLoadLibrary(String libName) {
        if (LIBRARY_LOADED) return;
        InputStream lib = null;
        try {
            lib = IngameIME.class.getClassLoader().getResourceAsStream(libName);
            if (lib == null) throw new RuntimeException("Required library resource does not exist");
            Path path = new File(System.getProperty("java.io.tmpdir"), "IngameIME-Native-" + libName).toPath();
            try {
                Files.copy(lib, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // 写不进去通常是已有其他实例加载了它；只要文件在就继续试着加载。
                if (!Files.exists(path)) throw e;
                IngameIME_Fish.LOG.debug("Reusing existing native library at " + path);
            }
            System.load(path.toString());
            LIBRARY_LOADED = true;
            IngameIME_Fish.LOG.info("Library [" + libName + "] has loaded!");
        } catch (Throwable e) {
            IngameIME_Fish.LOG.warn("Try to load library [" + libName + "] but failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (lib != null) try { lib.close(); } catch (Throwable ignored) {}
        }
    }

    private static long getWindowHandle_LWJGL2() {
        try {
            Method getImplementation = Display.class.getDeclaredMethod("getImplementation");
            getImplementation.setAccessible(true);
            Object impl = getImplementation.invoke(null);
            if (impl == null) return 0;
            IngameIME_Fish.LOG.info("Display implementation class: " + impl.getClass().getName());

            try {
                Method getHwnd = impl.getClass().getDeclaredMethod("getHwnd");
                getHwnd.setAccessible(true);
                Object hwnd = getHwnd.invoke(impl);
                if (hwnd instanceof Long) return (Long) hwnd;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                java.lang.reflect.Field hwndField = impl.getClass().getDeclaredField("hwnd");
                hwndField.setAccessible(true);
                Object hwnd = hwndField.get(impl);
                if (hwnd instanceof Long) return (Long) hwnd;
            } catch (NoSuchFieldException ignored) {
            }
        } catch (Throwable e) {
            IngameIME_Fish.LOG.warn("Failed to get window handle via LWJGL2", e);
        }
        return 0;
    }

    private static long getWindowHandle() {
        long hWnd = getWindowHandle_LWJGL2();
        if (hWnd == 0) IngameIME_Fish.LOG.error("Failed to obtain LWJGL2 HWND");
        else IngameIME_Fish.LOG.info("Successfully obtained HWND: 0x" + Long.toHexString(hWnd));
        return hWnd;
    }

    public static void destroyInputCtx() {
        ACTIVATED = false;
        CONTEXT_GENERATION++;
        ACTIVATION_GENERATION++;
        GAMEPLAY_INACTIVE_CREATE_ATTEMPTED = false;
        GAMEPLAY_INACTIVE_LOGGED = false;
        GAMEPLAY_INACTIVE_FAILURE_LOGGED = false;
        CALLBACK_QUEUE.clear();
        COMMIT_QUEUE.clear();
        clearOverlayState();
        resetPreEditRectCache();
        if (InputCtx == null) return;
        IngameIME_Fish.logVerboseInfo("Destroying InputContext contextGeneration={}, activationGeneration={}", Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION));
        try {
            InputCtx.setCallback((PreEditCallback) null);
            InputCtx.setCallback((CommitCallback) null);
            InputCtx.setCallback((CandidateListCallback) null);
            InputCtx.setCallback((InputModeCallback) null);
        } catch (Throwable ignored) {
        }
        try {
            InputCtx.delete();
        } catch (Throwable ignored) {
        }
        InputCtx = null;
        preEditCallback = null;
        commitCallback = null;
        candidateListCallback = null;
        inputModeCallback = null;
        preEditCallbackProxy = null;
        commitCallbackProxy = null;
        candidateListCallbackProxy = null;
        inputModeCallbackProxy = null;
        IngameIME_Fish.LOG.info("InputContext has destroyed!");
    }

    private static void clearOverlayState() {
        try {
            ClientProxy.Screen.PreEdit.setContent(null, -1);
            ClientProxy.Screen.CandidateList.setContent(null, -1);
            ClientProxy.Screen.WInputMode.setActive(false);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isCurrentContext(int contextGeneration, int activationGeneration) {
        return InputCtx != null && ACTIVATED && CONTEXT_GENERATION == contextGeneration && ACTIVATION_GENERATION == activationGeneration;
    }

    private static boolean shouldApplyCallback(String type, int contextGeneration, int activationGeneration) {
        boolean current = isCurrentContext(contextGeneration, activationGeneration);
        if (!current) {
            IngameIME_Fish.logVerboseInfo("Dropped stale IME {} callback contextGeneration={}, activationGeneration={} currentContextGeneration={}, currentActivationGeneration={}, activated={}, hasContext={}",
                    type, Integer.valueOf(contextGeneration), Integer.valueOf(activationGeneration),
                    Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION),
                    Boolean.valueOf(ACTIVATED), Boolean.valueOf(InputCtx != null));
        }
        return current;
    }

    public static void createInputCtx() {
        if (!LIBRARY_LOADED) return;
        try {
            IngameIME_Fish.LOG.info("Using IngameIME-Native: " + InputContext.getVersion());
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("Failed to query native version", t);
        }
        if (!Display.isCreated()) {
            IngameIME_Fish.LOG.warn("Display is not created yet, deferring InputContext creation");
            return;
        }
        long hWnd = getWindowHandle();
        if (hWnd == 0) {
            IngameIME_Fish.LOG.error("InputContext could not init as HWND is NULL");
            return;
        }
        try {
            boolean effectiveUiLess = Config.UiLess_Windows || Minecraft.getMinecraft().isFullScreen();
            API api = "TextServiceFramework".equals(Config.API_Windows) ? API.TextServiceFramework : API.Imm32;
            InputCtx = IngameIME.CreateInputContextWin32(hWnd, api, effectiveUiLess);
            IngameIME_Fish.LOG.info("InputContext has created!");
            IngameIME_Fish.logVerboseInfo("Created InputContext contextGeneration={}, activationGeneration={}, api={}, uiless={}", Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION), api, Boolean.valueOf(effectiveUiLess));
        } catch (Throwable t) {
            IngameIME_Fish.LOG.error("Failed to create InputContext", t);
            return;
        }

        registerCallbacks();
    }

    private static void registerCallbacks() {
        preEditCallbackProxy = new PreEditCallbackImpl() {
            @Override
            protected void call(final CompositionState state, PreEditContext context) {
                final int contextGeneration = CONTEXT_GENERATION;
                final int activationGeneration = ACTIVATION_GENERATION;
                final String content;
                final int selStart;
                final int selEnd;
                try {
                    content = context == null ? null : context.getContent();
                    selStart = context == null ? -1 : context.getSelStart();
                    selEnd = context == null ? -1 : context.getSelEnd();
                } catch (Throwable e) {
                    IngameIME_Fish.LOG.error("Exception while copying preedit callback data", e);
                    return;
                }
                IngameIME_Fish.logVerboseInfo("IME preedit callback contextGeneration={}, activationGeneration={}, state={}, content={}, selStart={}, selEnd={}",
                        Integer.valueOf(contextGeneration), Integer.valueOf(activationGeneration), state, content, Integer.valueOf(selStart), Integer.valueOf(selEnd));
                CALLBACK_QUEUE.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!shouldApplyCallback("preedit", contextGeneration, activationGeneration)) return;
                        try {
                            if (state == CompositionState.Begin) ClientProxy.Screen.WInputMode.setActive(false);
                            String displayContent = filterPreEditText(content);
                            if (content != displayContent && (content == null || !content.equals(displayContent))) {
                                IngameIME_Fish.logVerboseInfo("Filtered control-token IME preedit state={}, original={}, filtered={}",
                                        state, describeCommitText(content), describeCommitText(displayContent));
                            }
                            ClientProxy.Screen.PreEdit.setContent(displayContent, displayContent == null ? -1 : selStart);
                            IngameIME_Fish.logVerboseInfo("IME preedit applied state={}, content={}, cursor={}", state, displayContent, Integer.valueOf(displayContent == null ? -1 : selStart));
                        } catch (Throwable e) {
                            IngameIME_Fish.LOG.error("Exception during preedit callback", e);
                        }
                    }
                });
            }
        };
        preEditCallback = new PreEditCallback(preEditCallbackProxy);

        commitCallbackProxy = new CommitCallbackImpl() {
            @Override
            protected void call(String text) {
                int contextGeneration = CONTEXT_GENERATION;
                int activationGeneration = ACTIVATION_GENERATION;
                if (text != null && text.length() > 0) {
                    IngameIME_Fish.logVerboseInfo("IME commit queued contextGeneration={}, activationGeneration={}: {}",
                            Integer.valueOf(contextGeneration), Integer.valueOf(activationGeneration), text);
                    COMMIT_QUEUE.add(new CommitText(contextGeneration, activationGeneration, text));
                }
            }
        };
        commitCallback = new CommitCallback(commitCallbackProxy);

        candidateListCallbackProxy = new CandidateListCallbackImpl() {
            @Override
            protected void call(CandidateListState state, CandidateListContext context) {
                final int contextGeneration = CONTEXT_GENERATION;
                final int activationGeneration = ACTIVATION_GENERATION;
                final ArrayList<String> candidates;
                final int selection;
                try {
                    candidates = context == null ? null : new ArrayList<String>(context.getCandidates());
                    selection = context == null ? -1 : context.getSelection();
                } catch (Throwable e) {
                    IngameIME_Fish.LOG.error("Exception while copying candidate callback data", e);
                    return;
                }
                IngameIME_Fish.logVerboseInfo("IME candidate callback contextGeneration={}, activationGeneration={}, state={}, selection={}, candidates={}",
                        Integer.valueOf(contextGeneration), Integer.valueOf(activationGeneration), state, Integer.valueOf(selection), candidates);
                CALLBACK_QUEUE.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!shouldApplyCallback("candidate", contextGeneration, activationGeneration)) return;
                        try {
                            ClientProxy.Screen.CandidateList.setContent(candidates, selection);
                            IngameIME_Fish.logVerboseInfo("IME candidate applied selection={}, candidates={}", Integer.valueOf(selection), candidates);
                        } catch (Throwable e) {
                            IngameIME_Fish.LOG.error("Exception during candidate callback", e);
                        }
                    }
                });
            }
        };
        candidateListCallback = new CandidateListCallback(candidateListCallbackProxy);

        inputModeCallbackProxy = new InputModeCallbackImpl() {
            @Override
            protected void call(final InputMode mode) {
                final int contextGeneration = CONTEXT_GENERATION;
                final int activationGeneration = ACTIVATION_GENERATION;
                IngameIME_Fish.logVerboseInfo("IME input-mode callback contextGeneration={}, activationGeneration={}, mode={}",
                        Integer.valueOf(contextGeneration), Integer.valueOf(activationGeneration), mode);
                CALLBACK_QUEUE.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!shouldApplyCallback("input-mode", contextGeneration, activationGeneration)) return;
                        try {
                            ClientProxy.Screen.WInputMode.setMode(mode);
                            IngameIME_Fish.logVerboseInfo("IME input-mode applied mode={}", mode);
                        } catch (Throwable e) {
                            IngameIME_Fish.LOG.error("Exception during input mode callback", e);
                        }
                    }
                });
            }
        };
        inputModeCallback = new InputModeCallback(inputModeCallbackProxy);

        InputCtx.setCallback(preEditCallback);
        InputCtx.setCallback(commitCallback);
        InputCtx.setCallback(candidateListCallback);
        InputCtx.setCallback(inputModeCallback);
    }

    public static void updatePreEditRectFromGui(int x, int y, int width, int height) {
        if (!LIBRARY_LOADED || InputCtx == null || !ACTIVATED || !Display.isCreated()) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.gameSettings == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
            ScaledResolution scaled = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
            int scaledWidth = scaled.getScaledWidth();
            int scaledHeight = scaled.getScaledHeight();
            if (scaledWidth <= 0 || scaledHeight <= 0) return;
            if (x < 0 || y < 0) {
                if (Config.VerboseLog) {
                    IngameIME_Fish.logVerboseInfo("Skipped PreEditRect update for invalid gui=({},{} {}x{}) scaled={}x{} display={}x{}",
                            Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height),
                            Integer.valueOf(scaledWidth), Integer.valueOf(scaledHeight), Integer.valueOf(mc.displayWidth), Integer.valueOf(mc.displayHeight));
                }
                return;
            }

            int guiLeft = clamp(x, 0, scaledWidth);
            int guiTop = clamp(y, 0, scaledHeight);
            int guiRight = clamp(x + Math.max(1, width), 0, scaledWidth);
            int guiBottom = clamp(y + Math.max(1, height), 0, scaledHeight);
            if (guiRight <= guiLeft) guiRight = Math.min(scaledWidth, guiLeft + 1);
            if (guiBottom <= guiTop) guiBottom = Math.min(scaledHeight, guiTop + 1);

            int nativeLeft = scaleGuiToDisplayFloor(guiLeft, scaledWidth, mc.displayWidth);
            int nativeTop = scaleGuiToDisplayFloor(guiTop, scaledHeight, mc.displayHeight);
            int nativeRight = scaleGuiToDisplayCeil(guiRight, scaledWidth, mc.displayWidth);
            int nativeBottom = scaleGuiToDisplayCeil(guiBottom, scaledHeight, mc.displayHeight);

            int nativeWidth = Math.max(1, nativeRight - nativeLeft);
            int nativeHeight = Math.max(1, nativeBottom - nativeTop);

            // 矩形未变就不下发：这个方法游戏中每帧多次被调（渲染钩子 + tick 末尾），
            // setPreEditRect 是 JNI 调用，无条件转发是纯浪费。
            if (nativeLeft == lastRectX && nativeTop == lastRectY
                    && nativeWidth == lastRectWidth && nativeHeight == lastRectHeight) {
                return;
            }

            // 复用同一个 SWIG 对象：旧实现每帧 new PreEditRect()，原生内存只靠 finalize() 回收。
            if (preEditRect == null) preEditRect = new PreEditRect();
            preEditRect.setX(nativeLeft);
            preEditRect.setY(nativeTop);
            preEditRect.setWidth(nativeWidth);
            preEditRect.setHeight(nativeHeight);
            InputCtx.setPreEditRect(preEditRect);

            lastRectX = nativeLeft;
            lastRectY = nativeTop;
            lastRectWidth = nativeWidth;
            lastRectHeight = nativeHeight;

            if (Config.VerboseLog) {
                IngameIME_Fish.logVerboseInfo("Updated PreEditRect gui=({},{} {}x{}) scaled={}x{} display={}x{} native=({},{} {}x{})",
                        Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height),
                        Integer.valueOf(scaledWidth), Integer.valueOf(scaledHeight), Integer.valueOf(mc.displayWidth), Integer.valueOf(mc.displayHeight),
                        Integer.valueOf(nativeLeft), Integer.valueOf(nativeTop), Integer.valueOf(nativeWidth), Integer.valueOf(nativeHeight));
            }
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("Failed to update IME preedit rect", t);
        }
    }

    /** 丢弃缓存的 PreEditRect 与去重状态。InputContext 重建时必须调用。 */
    private static void resetPreEditRectCache() {
        lastRectX = lastRectY = lastRectWidth = lastRectHeight = Integer.MIN_VALUE;
        if (preEditRect != null) {
            try {
                preEditRect.delete();
            } catch (Throwable ignored) {
            }
            preEditRect = null;
        }
    }

    private static int scaleGuiToDisplayFloor(int value, int scaledSize, int displaySize) {
        return (int) Math.floor((double) value * (double) displaySize / (double) scaledSize);
    }

    private static int scaleGuiToDisplayCeil(int value, int scaledSize, int displaySize) {
        return (int) Math.ceil((double) value * (double) displaySize / (double) scaledSize);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static void loadLibrary() {
        boolean isWindows = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS;
        if (!isWindows) {
            IngameIME_Fish.LOG.error("Unsupported platform: " + LWJGLUtil.getPlatformName());
            return;
        }
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.indexOf("aarch64") >= 0 || arch.indexOf("arm64") >= 0) {
            tryLoadLibrary("IngameIME_Java-arm64.dll");
            tryLoadLibrary("IngameIME_Java-x64.dll");
            tryLoadLibrary("IngameIME_Java-x86.dll");
        } else if (arch.indexOf("64") >= 0) {
            tryLoadLibrary("IngameIME_Java-x64.dll");
            tryLoadLibrary("IngameIME_Java-arm64.dll");
            tryLoadLibrary("IngameIME_Java-x86.dll");
        } else {
            tryLoadLibrary("IngameIME_Java-x86.dll");
            tryLoadLibrary("IngameIME_Java-x64.dll");
            tryLoadLibrary("IngameIME_Java-arm64.dll");
        }
        if (!LIBRARY_LOADED) IngameIME_Fish.LOG.error("Unsupported arch: " + System.getProperty("os.arch"));
    }

    public static void drainCallbackQueue() {
        Runnable task;
        while ((task = CALLBACK_QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Throwable e) {
                IngameIME_Fish.LOG.error("Exception while draining IME callback task", e);
            }
        }
    }

    public static void drainCommitQueue() {
        CommitText commit;
        while ((commit = COMMIT_QUEUE.poll()) != null) {
            if (!shouldApplyCallback("commit", commit.contextGeneration, commit.activationGeneration)) continue;
            try {
                String text = filterCommittedText(commit.text);
                if (text == null || text.length() == 0) {
                    IngameIME_Fish.logVerboseInfo("Skipped non-text IME commit contextGeneration={}, activationGeneration={}: {}",
                            Integer.valueOf(commit.contextGeneration), Integer.valueOf(commit.activationGeneration), describeCommitText(commit.text));
                    continue;
                }
                IngameIME_Fish.logVerboseInfo("IME commit draining contextGeneration={}, activationGeneration={} into {}: {}",
                        Integer.valueOf(commit.contextGeneration), Integer.valueOf(commit.activationGeneration), IMStates.getActiveControl().getClass().getName(), text);
                IMStates.getActiveControl().writeText(text);
            } catch (Throwable e) {
                IngameIME_Fish.LOG.error("Exception while writing committed text", e);
            }
        }
    }

    private static String filterCommittedText(String text) {
        String filtered = filterControlTokenText(text);
        if (text != filtered && (text == null || !text.equals(filtered))) {
            IngameIME_Fish.logVerboseInfo("Filtered control-token IME commit original={}, filtered={}",
                    describeCommitText(text), describeCommitText(filtered));
        }
        return filtered;
    }

    private static String filterPreEditText(String text) {
        if (text == null || text.length() == 0) return text;
        return isOnlyNamedControlTokens(text) ? null : text;
    }

    private static String filterControlTokenText(String text) {
        if (text == null || text.length() == 0) return null;

        // Some Windows IMEs can emit key names (for example creative search ESC+CR)
        // as the whole commit/preedit payload. Only suppress payloads made entirely
        // from those control-token names; never remove matching words from mixed text,
        // because pinyin/Chinese commits and ordinary punctuation must pass through.
        StringBuilder result = null;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isTextCommitChar(ch)) {
                if (result != null) result.append(ch);
            } else if (result == null) {
                result = new StringBuilder(text.length());
                result.append(text.substring(0, i));
            }
        }
        String withoutNativeControls = result == null ? text : result.toString();
        return isOnlyNamedControlTokens(withoutNativeControls) ? null : withoutNativeControls;
    }

    private static boolean isTextCommitChar(char ch) {
        return !Character.isISOControl(ch) && ch != 127;
    }

    public static String filterAllowedCharacters(String text) {
        if (text == null || text.length() == 0) return text;
        StringBuilder result = null;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isAllowedTextCharacter(ch)) {
                if (result != null) result.append(ch);
            } else if (result == null) {
                result = new StringBuilder(text.length());
                result.append(text.substring(0, i));
            }
        }
        String filtered = result == null ? text : result.toString();
        if (filtered.length() != text.length()) {
            IngameIME_Fish.logVerboseInfo("Filtered ChatAllowedCharacters string original={}, filtered={}", describeCommitText(text), describeCommitText(filtered));
        }
        return filtered;
    }

    private static boolean isAllowedTextCharacter(char ch) {
        return ch != 0 && ch != 167 && !Character.isISOControl(ch) && ch != 127;
    }

    public static void onGuiScreenKeyTyped(char typedChar, int keyCode) {
        if (!getActivated()) return;
        armControlTypedSequence(null, typedChar, keyCode);
    }

    public static boolean shouldSuppressGuiTextFieldKeyTyped(Object textField, char typedChar, int keyCode) {
        if (!getActivated() || textField == null) {
            resetControlTypedSequence();
            return false;
        }

        long now = System.currentTimeMillis();
        if (isControlActionKey(keyCode)) {
            armControlTypedSequence(textField, typedChar, keyCode);
            return false;
        }

        if (controlTypedSequenceField != null && controlTypedSequenceField != textField) {
            resetControlTypedSequence();
            return false;
        }

        if (controlTypedSequenceField == null) controlTypedSequenceField = textField;

        if (now - controlTypedSequenceStartedAt > CONTROL_TYPED_SEQUENCE_TIMEOUT_MS) {
            resetControlTypedSequence();
            return false;
        }

        if (keyCode != Keyboard.KEY_NONE || (!isPotentialNamedControlTokenChar(typedChar) && !isNamedControlSeparator(typedChar))) {
            resetControlTypedSequence();
            return false;
        }

        controlTypedSequence.append(typedChar);
        String sequence = controlTypedSequence.toString();
        if (isNamedControlTokenPrefixSequence(sequence)) {
            IngameIME_Fish.logVerboseInfo("Suppressed possible IME control-token keyTyped char='{}' keyCode={} sequence='{}'", String.valueOf(typedChar), Integer.valueOf(keyCode), sequence);
            return true;
        }

        resetControlTypedSequence();
        return false;
    }

    private static void armControlTypedSequence(Object textField, char typedChar, int keyCode) {
        if (!isControlActionKey(keyCode)) return;
        controlTypedSequenceField = textField;
        controlTypedSequence.setLength(0);
        controlTypedSequenceStartedAt = System.currentTimeMillis();
        IngameIME_Fish.logVerboseInfo("Armed IME control-token keyTyped suppression keyCode={} charCode={}", Integer.valueOf(keyCode), Integer.valueOf((int) typedChar));
    }

    private static boolean isControlActionKey(int keyCode) {
        return keyCode == Keyboard.KEY_ESCAPE
                || keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_NUMPADENTER
                || keyCode == Keyboard.KEY_TAB;
    }

    private static void resetControlTypedSequence() {
        controlTypedSequenceField = null;
        controlTypedSequence.setLength(0);
        controlTypedSequenceStartedAt = 0L;
    }

    private static boolean isPotentialNamedControlTokenChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    private static boolean isNamedControlTokenPrefixSequence(String text) {
        if (text == null || text.length() == 0) return false;
        ArrayList<String> tokens = splitNamedControlTokens(text);
        if (tokens.isEmpty()) return true;
        for (int i = 0; i < tokens.size(); i++) {
            if (!isNamedControlTokenPrefix(tokens.get(i))) return false;
        }
        return true;
    }

    private static boolean isNamedControlTokenPrefix(String text) {
        if (text == null || text.length() == 0) return false;
        String normalized = text.toLowerCase();
        return "esc".startsWith(normalized)
                || "escape".startsWith(normalized)
                || "enter".startsWith(normalized)
                || "return".startsWith(normalized)
                || "tab".startsWith(normalized)
                || "cr".startsWith(normalized)
                || "lf".startsWith(normalized)
                || "crlf".startsWith(normalized);
    }

    private static boolean isOnlyNamedControlTokens(String text) {
        if (text == null) return false;
        ArrayList<String> tokens = splitNamedControlTokens(text);
        if (tokens.isEmpty()) return text.trim().length() == 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (!isNamedControlToken(tokens.get(i))) return false;
        }
        return true;
    }

    private static ArrayList<String> splitNamedControlTokens(String text) {
        ArrayList<String> tokens = new ArrayList<String>();
        int tokenStart = -1;
        for (int i = 0; i <= text.length(); i++) {
            char ch = i < text.length() ? text.charAt(i) : 0;
            if (i < text.length() && isNamedControlTokenChar(ch)) {
                if (tokenStart < 0) tokenStart = i;
            } else {
                if (tokenStart >= 0) {
                    tokens.add(text.substring(tokenStart, i));
                    tokenStart = -1;
                }
                if (i < text.length() && !isNamedControlSeparator(ch)) {
                    tokens.add(String.valueOf(ch));
                }
            }
        }
        return tokens;
    }

    private static boolean isNamedControlTokenChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '\\';
    }

    private static boolean isNamedControlSeparator(char ch) {
        return Character.isWhitespace(ch)
                || ch == '+'
                || ch == ','
                || ch == ';'
                || ch == '/'
                || ch == '|'
                || ch == '_'
                || ch == '-'
                || ch == ':'
                || ch == '.'
                || ch == '('
                || ch == ')'
                || ch == '['
                || ch == ']'
                || ch == '{'
                || ch == '}';
    }

    private static boolean isNamedControlToken(String token) {
        String normalized = token == null ? "" : token.trim().toLowerCase();
        return "esc".equals(normalized)
                || "escape".equals(normalized)
                || "enter".equals(normalized)
                || "return".equals(normalized)
                || "tab".equals(normalized)
                || "cr".equals(normalized)
                || "lf".equals(normalized)
                || "crlf".equals(normalized)
                || "\\r".equals(normalized)
                || "\\n".equals(normalized)
                || "\\t".equals(normalized);
    }

    private static String describeCommitText(String text) {
        if (text == null) return "null";
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) builder.append(' ');
            builder.append("U+");
            String hex = Integer.toHexString(text.charAt(i)).toUpperCase();
            for (int pad = hex.length(); pad < 4; pad++) builder.append('0');
            builder.append(hex);
        }
        builder.append("] '").append(text).append('\'');
        return builder.toString();
    }

    private static final class CommitText {
        final int contextGeneration;
        final int activationGeneration;
        final String text;

        CommitText(int contextGeneration, int activationGeneration, String text) {
            this.contextGeneration = contextGeneration;
            this.activationGeneration = activationGeneration;
            this.text = text;
        }
    }

    public static boolean getActivated() {
        return InputCtx != null && ACTIVATED;
    }

    public static void ensureInactiveForGameplay() {
        if (!LIBRARY_LOADED || !Display.isCreated()) return;
        if (InputCtx == null) {
            if (GAMEPLAY_INACTIVE_CREATE_ATTEMPTED) return;
            GAMEPLAY_INACTIVE_CREATE_ATTEMPTED = true;
            createInputCtx();
            if (InputCtx == null) return;
        }
        if (!ACTIVATED && GAMEPLAY_INACTIVE_LOGGED) return;

        try {
            InputCtx.setActivated(false);
            if (ACTIVATED) ACTIVATION_GENERATION++;
            ACTIVATED = false;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            clearOverlayState();
            if (!GAMEPLAY_INACTIVE_LOGGED) {
                IngameIME_Fish.logVerboseInfo("InputContext forced inactive for gameplay, contextGeneration={}, activationGeneration={}",
                        Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION));
                GAMEPLAY_INACTIVE_LOGGED = true;
            }
            GAMEPLAY_INACTIVE_CREATE_ATTEMPTED = false;
            GAMEPLAY_INACTIVE_FAILURE_LOGGED = false;
        } catch (Throwable t) {
            ACTIVATED = false;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            clearOverlayState();
            if (!GAMEPLAY_INACTIVE_FAILURE_LOGGED) {
                IngameIME_Fish.LOG.warn("Failed to force IME inactive for gameplay", t);
                GAMEPLAY_INACTIVE_FAILURE_LOGGED = true;
            }
        }
    }

    public static void setActivated(boolean activated) {
        if (activated) {
            if (ACTIVATED && InputCtx != null) return;
            if (InputCtx == null) createInputCtx();
            if (InputCtx == null) return;
            if (activateInputCtx()) return;
            IngameIME_Fish.LOG.warn("Recreating InputContext after activation failure");
            destroyInputCtx();
            createInputCtx();
            if (InputCtx != null) activateInputCtx();
            return;
        }

        if (InputCtx == null) {
            ACTIVATED = false;
            ACTIVATION_GENERATION++;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            clearOverlayState();
            return;
        }
        try {
            InputCtx.setActivated(false);
            IngameIME_Fish.logDebugInfo("IM active state: {}", false);
        } catch (Throwable t) {
            IngameIME_Fish.LOG.error("Failed to deactivate IME", t);
        } finally {
            ACTIVATED = false;
            ACTIVATION_GENERATION++;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            clearOverlayState();
            IngameIME_Fish.logVerboseInfo("InputContext kept for reuse after deactivation, contextGeneration={}, activationGeneration={}",
                    Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION));
        }
    }

    private static boolean activateInputCtx() {
        try {
            ACTIVATION_GENERATION++;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            // 重新激活后原生侧可能已丢弃旧矩形，强制下一帧重发。
            lastRectX = lastRectY = lastRectWidth = lastRectHeight = Integer.MIN_VALUE;
            InputCtx.setActivated(true);
            ACTIVATED = true;
            GAMEPLAY_INACTIVE_LOGGED = false;
            GAMEPLAY_INACTIVE_FAILURE_LOGGED = false;
            GAMEPLAY_INACTIVE_CREATE_ATTEMPTED = false;
            IngameIME_Fish.logDebugInfo("IM active state: {}", true);
            IngameIME_Fish.logVerboseInfo("InputContext activated, contextGeneration={}, activationGeneration={}",
                    Integer.valueOf(CONTEXT_GENERATION), Integer.valueOf(ACTIVATION_GENERATION));
            return true;
        } catch (Throwable t) {
            ACTIVATED = false;
            ACTIVATION_GENERATION++;
            CALLBACK_QUEUE.clear();
            COMMIT_QUEUE.clear();
            clearOverlayState();
            IngameIME_Fish.LOG.error("Failed to activate IME", t);
            return false;
        }
    }
}
