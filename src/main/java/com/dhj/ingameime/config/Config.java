package com.dhj.ingameime.config;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;

import java.io.File;

public class Config {
    private static Configuration config;

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

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            config.load();
        }
        sync();
    }

    public static void sync() {
        API_Windows = config.get("api", "Windows", API_Windows, "Available: TextServiceFramework, Imm32").getString();
        if (!"TextServiceFramework".equals(API_Windows) && !"Imm32".equals(API_Windows)) {
            API_Windows = "TextServiceFramework";
        }
        UiLess_Windows = config.get("uiless", "Windows", UiLess_Windows, "True asks IngameIME to render the candidate UI in-game. False uses the native Windows IME candidate UI.").getBoolean(UiLess_Windows);
        TurnOffOnMouseMove = config.get("general", "TurnOffOnMouseMove", TurnOffOnMouseMove, "Turn off Input Method on mouse move.").getBoolean(TurnOffOnMouseMove);
        AlphaModeText = config.get("modetext", "AlphaMode", AlphaModeText, "Text to display when in Alpha mode.").getString();
        NativeModeText = config.get("modetext", "NativeMode", NativeModeText, "Text to display when in Native mode.").getString();
        DebugLog = config.get("debug", "DebugLog", DebugLog, "Config if print debug log.").getBoolean(DebugLog);
        VerboseLog = config.get("debug", "VerboseLog", VerboseLog, "Config if print verbose troubleshooting log.").getBoolean(VerboseLog);

        TextColor = parseColor(config.get("theme", "TextColor", toHex(TextColor), "ARGB hex color, for example 0xFF000000"), TextColor);
        BackgroundColor = parseColor(config.get("theme", "BackgroundColor", toHex(BackgroundColor), "ARGB hex color"), BackgroundColor);
        IndexColor = parseColor(config.get("theme", "IndexColor", toHex(IndexColor), "ARGB hex color"), IndexColor);
        SelectedBackgroundColor = parseColor(config.get("theme", "SelectedBackgroundColor", toHex(SelectedBackgroundColor), "ARGB hex color"), SelectedBackgroundColor);
        CursorColor = parseColor(config.get("theme", "CursorColor", toHex(CursorColor), "ARGB hex color"), CursorColor);
        BorderColor = parseColor(config.get("theme", "BorderColor", toHex(BorderColor), "ARGB hex color"), BorderColor);
        Padding = config.get("theme", "Padding", Padding, "Widget padding").getInt(Padding);
        CandidatePadding = config.get("theme", "CandidatePadding", CandidatePadding, "Candidate widget padding").getInt(CandidatePadding);
        BorderWidth = config.get("theme", "BorderWidth", BorderWidth, "Widget border width").getInt(BorderWidth);

        if (config.hasChanged()) config.save();
    }

    private static int parseColor(Property property, int fallback) {
        try {
            return (int) Long.decode(property.getString()).longValue();
        } catch (Throwable t) {
            property.set(toHex(fallback));
            return fallback;
        }
    }

    private static String toHex(int color) {
        return String.format("0x%08X", color);
    }
}
