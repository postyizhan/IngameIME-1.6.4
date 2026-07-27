package com.dhj.ingameime.config;

import com.dhj.ingameime.Tags;
import net.xiaoyu233.fml.config.Codec;
import net.xiaoyu233.fml.config.ConfigCategory;
import net.xiaoyu233.fml.config.ConfigEntry;
import net.xiaoyu233.fml.config.ConfigRegistry;
import net.xiaoyu233.fml.config.ConfigRoot;
import net.xiaoyu233.fml.util.FieldReference;

import java.io.File;

/**
 * 配置。
 *
 * 原 Forge 版用 net.minecraftforge.common.Configuration 读写 .cfg；FML 没有这套 API，
 * 换成 FML 自带的 ConfigRegistry（JSON，落在 config/ingameime.json）。
 *
 * 对外仍暴露一批 public static 字段：其余代码每帧都在读 Config.TextColor 这类值，
 * 保持静态字段可以避免在渲染热路径里走 FieldReference.get() 的额外跳转，
 * 也让本次移植不必改动 gui/ 与 control/ 下的调用点。真正的存储在下面的
 * FieldReference 里，reload 时由 sync() 拷进静态字段。
 */
public class Config {
    private static final int CONFIG_VERSION = 1;

    public static String API_Windows = "TextServiceFramework";
    public static boolean UiLess_Windows = true;
    public static boolean TurnOffOnMouseMove = false;
    public static String AlphaModeText = "A";
    public static String NativeModeText = "中";
    public static boolean DebugLog = false;
    public static boolean VerboseLog = false;

    public static int TextColor = 0xFF000000;
    public static int BackgroundColor = 0xEBEBEBEB;
    public static int IndexColor = 0xFF555555;
    public static int SelectedBackgroundColor = 0xEBEBEBEB;
    public static int CursorColor = 0xFF000000;
    public static int BorderColor = 0x80000000;
    public static int Padding = 3;
    public static int CandidatePadding = 5;
    public static int BorderWidth = 1;

    private static final FieldReference<String> REF_API_WINDOWS = new FieldReference<String>(API_Windows);
    private static final FieldReference<Boolean> REF_UILESS_WINDOWS = new FieldReference<Boolean>(UiLess_Windows);
    private static final FieldReference<Boolean> REF_TURN_OFF_ON_MOUSE_MOVE = new FieldReference<Boolean>(TurnOffOnMouseMove);
    private static final FieldReference<String> REF_ALPHA_MODE_TEXT = new FieldReference<String>(AlphaModeText);
    private static final FieldReference<String> REF_NATIVE_MODE_TEXT = new FieldReference<String>(NativeModeText);
    private static final FieldReference<Boolean> REF_DEBUG_LOG = new FieldReference<Boolean>(DebugLog);
    private static final FieldReference<Boolean> REF_VERBOSE_LOG = new FieldReference<Boolean>(VerboseLog);

    // 颜色用字符串存：JSON 里 0xEBEBEBEB 超出 int 正数范围，写成十进制既不可读也容易被
    // 手改成越界值。存 "0xEBEBEBEB" 这种 ARGB 十六进制，读的时候用 Long.decode 再截成 int。
    private static final FieldReference<String> REF_TEXT_COLOR = new FieldReference<String>(toHex(TextColor));
    private static final FieldReference<String> REF_BACKGROUND_COLOR = new FieldReference<String>(toHex(BackgroundColor));
    private static final FieldReference<String> REF_INDEX_COLOR = new FieldReference<String>(toHex(IndexColor));
    private static final FieldReference<String> REF_SELECTED_BACKGROUND_COLOR = new FieldReference<String>(toHex(SelectedBackgroundColor));
    private static final FieldReference<String> REF_CURSOR_COLOR = new FieldReference<String>(toHex(CursorColor));
    private static final FieldReference<String> REF_BORDER_COLOR = new FieldReference<String>(toHex(BorderColor));
    private static final FieldReference<Integer> REF_PADDING = new FieldReference<Integer>(Padding);
    private static final FieldReference<Integer> REF_CANDIDATE_PADDING = new FieldReference<Integer>(CandidatePadding);
    private static final FieldReference<Integer> REF_BORDER_WIDTH = new FieldReference<Integer>(BorderWidth);

    private static final ConfigRoot ROOT = new ConfigRoot(CONFIG_VERSION).withComment("IngameIME 配置文件")
            .addEntry(new ConfigCategory("api").withComment("输入法 API")
                    .addEntry(new ConfigEntry<String>("Windows", Codec.STRING, REF_API_WINDOWS.get(), REF_API_WINDOWS)
                            .withComment("可选: TextServiceFramework, Imm32")))
            .addEntry(new ConfigCategory("uiless").withComment("候选窗绘制")
                    .addEntry(new ConfigEntry<Boolean>("Windows", Codec.BOOLEAN, REF_UILESS_WINDOWS.get(), REF_UILESS_WINDOWS)
                            .withComment("true 由 IngameIME 在游戏内绘制候选列表; false 使用 Windows 原生候选窗")))
            .addEntry(new ConfigCategory("general").withComment("通用")
                    .addEntry(new ConfigEntry<Boolean>("TurnOffOnMouseMove", Codec.BOOLEAN, REF_TURN_OFF_ON_MOUSE_MOVE.get(), REF_TURN_OFF_ON_MOUSE_MOVE)
                            .withComment("鼠标移动时关闭输入法")))
            .addEntry(new ConfigCategory("modetext").withComment("输入模式指示文本")
                    .addEntry(new ConfigEntry<String>("AlphaMode", Codec.STRING, REF_ALPHA_MODE_TEXT.get(), REF_ALPHA_MODE_TEXT)
                            .withComment("英文模式显示的文本"))
                    .addEntry(new ConfigEntry<String>("NativeMode", Codec.STRING, REF_NATIVE_MODE_TEXT.get(), REF_NATIVE_MODE_TEXT)
                            .withComment("本地语言模式显示的文本")))
            .addEntry(new ConfigCategory("debug").withComment("调试")
                    .addEntry(new ConfigEntry<Boolean>("DebugLog", Codec.BOOLEAN, REF_DEBUG_LOG.get(), REF_DEBUG_LOG)
                            .withComment("输出调试日志"))
                    .addEntry(new ConfigEntry<Boolean>("VerboseLog", Codec.BOOLEAN, REF_VERBOSE_LOG.get(), REF_VERBOSE_LOG)
                            .withComment("输出详细排查日志")))
            .addEntry(new ConfigCategory("theme").withComment("主题")
                    .addEntry(new ConfigEntry<String>("TextColor", Codec.STRING, REF_TEXT_COLOR.get(), REF_TEXT_COLOR)
                            .withComment("ARGB 十六进制颜色, 例如 0xFF000000"))
                    .addEntry(new ConfigEntry<String>("BackgroundColor", Codec.STRING, REF_BACKGROUND_COLOR.get(), REF_BACKGROUND_COLOR)
                            .withComment("ARGB 十六进制颜色"))
                    .addEntry(new ConfigEntry<String>("IndexColor", Codec.STRING, REF_INDEX_COLOR.get(), REF_INDEX_COLOR)
                            .withComment("ARGB 十六进制颜色"))
                    .addEntry(new ConfigEntry<String>("SelectedBackgroundColor", Codec.STRING, REF_SELECTED_BACKGROUND_COLOR.get(), REF_SELECTED_BACKGROUND_COLOR)
                            .withComment("ARGB 十六进制颜色"))
                    .addEntry(new ConfigEntry<String>("CursorColor", Codec.STRING, REF_CURSOR_COLOR.get(), REF_CURSOR_COLOR)
                            .withComment("ARGB 十六进制颜色"))
                    .addEntry(new ConfigEntry<String>("BorderColor", Codec.STRING, REF_BORDER_COLOR.get(), REF_BORDER_COLOR)
                            .withComment("ARGB 十六进制颜色"))
                    .addEntry(new ConfigEntry<Integer>("Padding", Codec.INTEGER, REF_PADDING.get(), REF_PADDING)
                            .withComment("控件内边距"))
                    .addEntry(new ConfigEntry<Integer>("CandidatePadding", Codec.INTEGER, REF_CANDIDATE_PADDING.get(), REF_CANDIDATE_PADDING)
                            .withComment("候选项内边距"))
                    .addEntry(new ConfigEntry<Integer>("BorderWidth", Codec.INTEGER, REF_BORDER_WIDTH.get(), REF_BORDER_WIDTH)
                            .withComment("控件边框宽度")));

    public static ConfigRegistry createRegistry() {
        return new ConfigRegistry(ROOT, new File(Tags.MOD_ID + ".json")).setReloadRun(new Runnable() {
            @Override
            public void run() {
                sync();
            }
        });
    }

    /** 把 FieldReference 里的值拷进静态字段。由 ConfigRegistry 在每次 reload 后调用。 */
    public static void sync() {
        API_Windows = REF_API_WINDOWS.get();
        if (!"TextServiceFramework".equals(API_Windows) && !"Imm32".equals(API_Windows)) {
            API_Windows = "TextServiceFramework";
        }
        UiLess_Windows = REF_UILESS_WINDOWS.get().booleanValue();
        TurnOffOnMouseMove = REF_TURN_OFF_ON_MOUSE_MOVE.get().booleanValue();
        AlphaModeText = REF_ALPHA_MODE_TEXT.get();
        NativeModeText = REF_NATIVE_MODE_TEXT.get();
        DebugLog = REF_DEBUG_LOG.get().booleanValue();
        VerboseLog = REF_VERBOSE_LOG.get().booleanValue();

        TextColor = parseColor(REF_TEXT_COLOR, TextColor);
        BackgroundColor = parseColor(REF_BACKGROUND_COLOR, BackgroundColor);
        IndexColor = parseColor(REF_INDEX_COLOR, IndexColor);
        SelectedBackgroundColor = parseColor(REF_SELECTED_BACKGROUND_COLOR, SelectedBackgroundColor);
        CursorColor = parseColor(REF_CURSOR_COLOR, CursorColor);
        BorderColor = parseColor(REF_BORDER_COLOR, BorderColor);
        Padding = REF_PADDING.get().intValue();
        CandidatePadding = REF_CANDIDATE_PADDING.get().intValue();
        BorderWidth = REF_BORDER_WIDTH.get().intValue();
    }

    private static int parseColor(FieldReference<String> ref, int fallback) {
        try {
            return (int) Long.decode(ref.get()).longValue();
        } catch (Throwable t) {
            ref.set(toHex(fallback));
            return fallback;
        }
    }

    private static String toHex(int color) {
        return String.format("0x%08X", Integer.valueOf(color));
    }
}
