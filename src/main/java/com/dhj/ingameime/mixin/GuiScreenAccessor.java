package com.dhj.ingameime.mixin;

import net.minecraft.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 GuiScreen.keyTyped。
 *
 * 原 Forge 版走反射（getDeclaredMethod("keyTyped") + setAccessible）。这里换成 @Invoker：
 * 编译期就能校验签名，且不依赖方法名字符串。
 *
 * 不用 AccessWidener 放宽 keyTyped：GuiScreen 的大量子类各自以 protected 覆写它，
 * 把父类改成 public 会造成可见性冲突。@Invoker 生成的访问器仍走虚分派，
 * 因此对 GuiChat 这类子类调用时命中的是子类实现。
 */
@Mixin(GuiScreen.class)
public interface GuiScreenAccessor {
    @Invoker("keyTyped")
    void ingameime$keyTyped(char typedChar, int keyCode);
}
