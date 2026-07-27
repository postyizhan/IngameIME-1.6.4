package com.dhj.ingameime;

import com.dhj.ingameime.config.Config;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.config.ConfigRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * FishModLoader 入口。
 *
 * 原 Forge 版的 @Mod + @SidedProxy + FMLPreInitializationEvent 在这里退化成单个
 * ModInitializer：本模组是纯客户端模组（fml.mod.json 里 environment=client），
 * 不需要 CommonProxy/ClientProxy 这套双端分派。
 */
public class IngameIME_Fish implements ModInitializer {
    public static final Logger LOG = LogManager.getLogger(Tags.MOD_NAME);

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

    @Override
    public Optional<ConfigRegistry> createConfig() {
        return Optional.of(Config.createRegistry());
    }

    @Override
    public void onInitialize() {
        // 入口在 Minecraft 构造之前触发（见 FML 的 ClientEntrypointMixin），
        // 此时 Display/HWND 都还不存在，所以只加载原生库，InputContext 首次激活时才创建。
        ClientProxy.init();
    }
}
