package com.dhj.ingameime.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class IngameIMETransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "com/dhj/ingameime/core/IngameIMEHooks";
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        // name 是未反混淆名(生产环境为 avf/atv/... ,开发环境同 transformedName),
        // transformedName 覆盖 MCP/SRG 名。两者合起来已能唯一识别目标类,
        // 不需要再对每个类做一次完整 ASM 解析去读 cn.name——1.6.4 光原版就有 2200+ 个类。
        if (matches(name, transformedName, "net.minecraft.client.gui.GuiTextField", "avf")) {
            return transformGuiTextField(basicClass);
        }
        if (matches(name, transformedName, "net.minecraft.client.Minecraft", "atv")) {
            return transformMinecraft(basicClass);
        }
        if (matches(name, transformedName, "net.minecraft.client.gui.inventory.GuiContainerCreative", "axm")) {
            return transformGuiContainerCreative(basicClass);
        }
        if (matches(name, transformedName, "net.minecraft.util.ChatAllowedCharacters", "v")) {
            return transformChatAllowedCharacters(basicClass);
        }
        if (matches(name, transformedName, "net.minecraft.client.gui.GuiScreenBook", "axf")) {
            return widenAccess(basicClass, BOOK_WIDEN_METHODS, BOOK_WIDEN_DESCS);
        }
        if (matches(name, transformedName, "net.minecraft.client.gui.inventory.GuiEditSign", "axy")) {
            return widenAccess(basicClass, null, null);
        }
        return basicClass;
    }

    private static boolean matches(String name, String transformedName, String deobfName, String obfName) {
        return deobfName.equals(transformedName) || deobfName.equals(name) || obfName.equals(name);
    }

    /**
     * 生产环境的访问放宽。
     *
     * 开发期由 at/accesstransformer.cfg 处理(FG 会重新 patch/recompile dev 依赖),但 FML 9.11
     * 既没有 ModAccessTransformer 也不认 FMLAT manifest 属性,生产环境只能在 coremod 里做。
     * 两边必须覆盖同一批成员,否则 dev 能编译、进游戏 IllegalAccessError。
     *
     * 字段一律全放宽:字段没有虚分派,放宽不改变任何解析语义,也就不必维护混淆名表。
     * 方法按名字白名单放宽,且只针对 private 方法——把 protected 方法改成 public 会在子类
     * 仍为 protected 时产生可见性冲突,private 方法不可能被覆写,没有这个风险。
     */
    private byte[] widenAccess(byte[] bytes, String[][] methodNameVariants, String[] methodDescs) {
        ClassNode cn = read(bytes);
        boolean changed = widenFields(cn);
        if (methodNameVariants != null) {
            for (Object method : cn.methods) {
                MethodNode mn = (MethodNode) method;
                if ((mn.access & ACC_PRIVATE) == 0) continue;
                if (!matchesAnyVariant(mn.name, mn.desc, methodNameVariants, methodDescs)) continue;
                mn.access = toPublic(mn.access);
                changed = true;
            }
        }
        return changed ? write(cn) : bytes;
    }

    private boolean widenFields(ClassNode cn) {
        boolean changed = false;
        for (Object field : cn.fields) {
            FieldNode fn = (FieldNode) field;
            int widened = toPublic(fn.access);
            if (widened != fn.access) {
                fn.access = widened;
                changed = true;
            }
        }
        return changed;
    }

    private static int toPublic(int access) {
        return (access & ~(ACC_PRIVATE | ACC_PROTECTED)) | ACC_PUBLIC;
    }

    private static boolean matchesAnyVariant(String name, String desc, String[][] variants, String[] descs) {
        for (int i = 0; i < variants.length; i++) {
            if (!descs[i].equals(desc)) continue;
            String[] names = variants[i];
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(name)) return true;
            }
        }
        return false;
    }

    /**
     * GuiScreenBook 需要放宽的私有方法：追加文本(MCP 未给可读名)与按钮刷新。
     *
     * 每项顺序为 MCP / SRG / 混淆名。必须同时比对 desc：混淆后 axf 里叫 "b" 的方法不只一个
     * (还有 keyTypedInBook 的 (CI)V)，单靠名字会误伤。
     */
    private static final String[][] BOOK_WIDEN_METHODS = new String[][]{
            {"func_74160_b", "func_74160_b", "b"},
            {"updateButtons", "func_74161_g", "h"},
    };
    private static final String[] BOOK_WIDEN_DESCS = new String[]{
            "(Ljava/lang/String;)V",
            "()V",
    };

    private byte[] transformGuiTextField(byte[] bytes) {
        ClassNode cn = read(bytes);
        // 字段放宽：VanillaTextFieldControl 直接读 fontRenderer/xPos/yPos/width/height/lineScrollOffset。
        boolean changed = widenFields(cn);
        changed |= injectGuiTextFieldHooks(cn);
        return changed ? write(cn) : bytes;
    }

    private boolean injectGuiTextFieldHooks(ClassNode cn) {
        boolean changed = false;
        for (Object method : cn.methods) {
            MethodNode mn = (MethodNode) method;
            if (("setFocused".equals(mn.name) || "func_73796_b".equals(mn.name) || "b".equals(mn.name)) && "(Z)V".equals(mn.desc)) {
                InsnList inject = new InsnList();
                inject.add(new VarInsnNode(ALOAD, 0));
                inject.add(new VarInsnNode(ILOAD, 1));
                inject.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "onGuiTextFieldSetFocused", "(Ljava/lang/Object;Z)V"));
                mn.instructions.insert(inject);
                changed = true;
            } else if (("textboxKeyTyped".equals(mn.name) || "func_73802_a".equals(mn.name) || "a".equals(mn.name)) && "(CI)Z".equals(mn.desc)) {
                injectGuiTextFieldKeyTypedFilter(mn);
                changed = true;
            }
        }
        return changed;
    }

    private void injectGuiTextFieldKeyTypedFilter(MethodNode mn) {
        LabelNode passThrough = new LabelNode();
        InsnList inject = new InsnList();
        inject.add(new VarInsnNode(ALOAD, 0));
        inject.add(new VarInsnNode(ILOAD, 1));
        inject.add(new VarInsnNode(ILOAD, 2));
        inject.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "shouldSuppressGuiTextFieldKeyTyped", "(Ljava/lang/Object;CI)Z"));
        inject.add(new JumpInsnNode(IFEQ, passThrough));
        inject.add(new InsnNode(ICONST_1));
        inject.add(new InsnNode(IRETURN));
        inject.add(passThrough);
        mn.instructions.insert(inject);
    }

    private byte[] transformMinecraft(byte[] bytes) {
        ClassNode cn = read(bytes);
        boolean changed = false;
        for (Object method : cn.methods) {
            MethodNode mn = (MethodNode) method;
            if (isDisplayGuiScreen(mn)) {
                injectDisplayGuiScreen(mn);
                changed = true;
            } else if (isToggleFullscreen(mn)) {
                injectToggleFullscreen(mn);
                changed = true;
            } else if (isRunGameLoop(mn)) {
                changed |= injectRenderOverlay(mn);
            }
        }
        return changed ? write(cn) : bytes;
    }

    private byte[] transformGuiContainerCreative(byte[] bytes) {
        ClassNode cn = read(bytes);
        boolean changed = false;
        for (Object method : cn.methods) {
            MethodNode mn = (MethodNode) method;
            if (("keyTyped".equals(mn.name) || "func_73869_a".equals(mn.name) || "a".equals(mn.name)) && "(CI)V".equals(mn.desc)) {
                InsnList inject = new InsnList();
                inject.add(new VarInsnNode(ILOAD, 1));
                inject.add(new VarInsnNode(ILOAD, 2));
                inject.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "onGuiScreenKeyTyped", "(CI)V"));
                mn.instructions.insert(inject);
                changed = true;
            }
        }
        return changed ? write(cn) : bytes;
    }

    private byte[] transformChatAllowedCharacters(byte[] bytes) {
        ClassNode cn = read(bytes);
        boolean changed = false;
        for (Object method : cn.methods) {
            MethodNode mn = (MethodNode) method;
            if (isAllowedCharacter(mn)) {
                resetMethodBody(mn);
                LabelNode reject = new LabelNode();
                mn.instructions.add(new VarInsnNode(ILOAD, 0));
                mn.instructions.add(new JumpInsnNode(IFEQ, reject));
                mn.instructions.add(new VarInsnNode(ILOAD, 0));
                mn.instructions.add(new IntInsnNode(BIPUSH, 167));
                mn.instructions.add(new JumpInsnNode(IF_ICMPEQ, reject));
                mn.instructions.add(new VarInsnNode(ILOAD, 0));
                mn.instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "isISOControl", "(C)Z"));
                mn.instructions.add(new JumpInsnNode(IFNE, reject));
                mn.instructions.add(new InsnNode(ICONST_1));
                mn.instructions.add(new InsnNode(IRETURN));
                mn.instructions.add(reject);
                mn.instructions.add(new InsnNode(ICONST_0));
                mn.instructions.add(new InsnNode(IRETURN));
                changed = true;
            } else if (isFilterAllowedCharacters(mn)) {
                resetMethodBody(mn);
                mn.instructions.add(new VarInsnNode(ALOAD, 0));
                mn.instructions.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "filterAllowedCharacters", "(Ljava/lang/String;)Ljava/lang/String;"));
                mn.instructions.add(new InsnNode(ARETURN));
                changed = true;
            }
        }
        return changed ? write(cn) : bytes;
    }

    private boolean isDisplayGuiScreen(MethodNode mn) {
        return (("displayGuiScreen".equals(mn.name) || "func_71373_a".equals(mn.name))
                && "(Lnet/minecraft/client/gui/GuiScreen;)V".equals(mn.desc))
                || ("a".equals(mn.name) && "(Lawe;)V".equals(mn.desc));
    }

    private boolean isToggleFullscreen(MethodNode mn) {
        return ("toggleFullscreen".equals(mn.name) || "func_71352_k".equals(mn.name) || "j".equals(mn.name)) && "()V".equals(mn.desc);
    }

    private boolean isRunGameLoop(MethodNode mn) {
        return ("runGameLoop".equals(mn.name) || "func_71411_J".equals(mn.name) || "S".equals(mn.name)) && "()V".equals(mn.desc);
    }

    private boolean isAllowedCharacter(MethodNode mn) {
        return ("isAllowedCharacter".equals(mn.name) || "func_71566_a".equals(mn.name) || "a".equals(mn.name)) && "(C)Z".equals(mn.desc);
    }

    private boolean isFilterAllowedCharacters(MethodNode mn) {
        return ("filerAllowedCharacters".equals(mn.name) || "func_71565_a".equals(mn.name) || "a".equals(mn.name))
                && "(Ljava/lang/String;)Ljava/lang/String;".equals(mn.desc);
    }

    private void injectDisplayGuiScreen(MethodNode mn) {
        InsnList head = new InsnList();
        head.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "onGuiScreenClosing", "()V"));
        mn.instructions.insert(head);

        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == RETURN) {
                InsnList beforeReturn = new InsnList();
                beforeReturn.add(new VarInsnNode(ALOAD, 1));
                beforeReturn.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "onGuiScreenDisplayed", "(Ljava/lang/Object;)V"));
                mn.instructions.insertBefore(insn, beforeReturn);
            }
        }
    }

    private void injectToggleFullscreen(MethodNode mn) {
        InsnList head = new InsnList();
        head.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "beforeToggleFullscreen", "()V"));
        mn.instructions.insert(head);

        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == RETURN) {
                InsnList beforeReturn = new InsnList();
                beforeReturn.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "afterToggleFullscreen", "()V"));
                mn.instructions.insertBefore(insn, beforeReturn);
            }
        }
    }

    private boolean injectRenderOverlay(MethodNode mn) {
        ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            if ((insn.getOpcode() == INVOKEVIRTUAL || insn.getOpcode() == INVOKESPECIAL) && insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (isMinecraftOwner(min.owner)
                        && ("checkGLError".equals(min.name) || "func_71361_d".equals(min.name) || "c".equals(min.name))
                        && "(Ljava/lang/String;)V".equals(min.desc)
                        && isPostRenderCheckGlErrorCall(insn)) {
                    InsnList inject = new InsnList();
                    inject.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "renderOverlay", "()V"));
                    mn.instructions.insertBefore(findStartForCheckGlErrorCall(insn), inject);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMinecraftOwner(String owner) {
        return MINECRAFT.equals(owner) || "atv".equals(owner);
    }

    private boolean isPostRenderCheckGlErrorCall(AbstractInsnNode invoke) {
        AbstractInsnNode p = previousMeaningful(invoke);
        return p instanceof LdcInsnNode && "Post render".equals(((LdcInsnNode) p).cst);
    }

    private AbstractInsnNode findStartForCheckGlErrorCall(AbstractInsnNode invoke) {
        AbstractInsnNode p = previousMeaningful(invoke);
        // Pattern before call is usually ALOAD 0, LDC "Post render", INVOKEVIRTUAL/INVOKESPECIAL.
        if (p != null && p.getOpcode() == LDC) {
            AbstractInsnNode receiver = previousMeaningful(p);
            if (receiver != null) return receiver;
        }
        return invoke;
    }

    private AbstractInsnNode previousMeaningful(AbstractInsnNode insn) {
        AbstractInsnNode p = insn == null ? null : insn.getPrevious();
        while (p instanceof LabelNode || p instanceof LineNumberNode || p instanceof FrameNode) {
            p = p.getPrevious();
        }
        return p;
    }

    /**
     * 清空方法体，准备整体重写。
     *
     * 除了指令和 try/catch，还必须清掉 localVariables：dev jar 带 LocalVariableTable，
     * 而 LVT 条目引用的 LabelNode 已经不在指令流里了。ASM 4.1 对此宽容（写出
     * length=0 的条目），但新版 ASM 会招
     * "Label offset position has not been resolved yet"。
     * maxStack/maxLocals 不用管，write() 用 COMPUTE_MAXS 重算。
     */
    private static void resetMethodBody(MethodNode mn) {
        mn.instructions.clear();
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
    }

    private ClassNode read(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        return cn;
    }

    private byte[] write(ClassNode cn) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
