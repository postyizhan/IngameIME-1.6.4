package com.dhj.ingameime;

import com.dhj.ingameime.config.Config;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.util.logging.Logger;

@Mod(
        modid = Tags.MOD_ID,
        version = Tags.VERSION,
        name = Tags.MOD_NAME,
        acceptedMinecraftVersions = "[1.6.4]"
)
public class IngameIME_Forge {
    public static final Logger LOG = Logger.getLogger(Tags.MOD_NAME);

    @SidedProxy(clientSide = "com.dhj.ingameime.ClientProxy", serverSide = "com.dhj.ingameime.CommonProxy")
    public static CommonProxy proxy;

    public static void logDebugInfo(String message, Object... params) {
        if (Config.DebugLog) {
            LOG.info(format(message, params));
        }
    }

    public static void logVerboseInfo(String message, Object... params) {
        if (Config.VerboseLog) {
            LOG.info(format(message, params));
        }
    }

    public static String format(String message, Object... params) {
        if (params == null || params.length == 0) return message;
        String result = message;
        for (Object param : params) {
            result = result.replaceFirst("\\{}", java.util.regex.Matcher.quoteReplacement(String.valueOf(param)));
        }
        return result;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
