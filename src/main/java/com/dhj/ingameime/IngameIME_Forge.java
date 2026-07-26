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

    /**
     * 把 {} 占位符依次替成参数。
     *
     * 单趟 StringBuilder。旧实现用 replaceFirst("\\{}", ...) 逐个参数扫一遍，除了每个参数
     * 编译一次正则，还有个坑：如果某个参数的字符串本身包含 {}，下一轮从头扫描时
     * 会把它当成占位符填掉。IME 候选词/预编辑文本是输入法给的任意内容，这个坑能真碰上。
     */
    public static String format(String message, Object... params) {
        if (message == null || params == null || params.length == 0) return message;
        StringBuilder result = new StringBuilder(message.length() + 16 * params.length);
        int next = 0;
        int i = 0;
        while (i < message.length()) {
            if (next < params.length && message.charAt(i) == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                result.append(String.valueOf(params[next++]));
                i += 2;
            } else {
                result.append(message.charAt(i++));
            }
        }
        return result.toString();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
