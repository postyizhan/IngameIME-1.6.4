package com.dhj.ingameime.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class IngameIMETransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "com/dhj/ingameime/core/IngameIMEHooks";
    private static final String GUI_TEXT_FIELD = "net/minecraft/client/gui/GuiTextField";
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final String GUI_CONTAINER_CREATIVE = "net/minecraft/client/gui/inventory/GuiContainerCreative";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        String className = transformedName != null ? transformedName : name;
        ClassNode cn;
        if ("net.minecraft.client.gui.GuiTextField".equals(className) || "avf".equals(className)) {
            return transformGuiTextField(basicClass);
        }
        if ("net.minecraft.client.Minecraft".equals(className) || "atv".equals(className)) {
            return transformMinecraft(basicClass);
        }
        if ("net.minecraft.client.gui.inventory.GuiContainerCreative".equals(className) || "axm".equals(className)) {
            return transformGuiContainerCreative(basicClass);
        }
        if ("net.minecraft.util.ChatAllowedCharacters".equals(className) || "v".equals(className)) {
            return transformChatAllowedCharacters(basicClass);
        }
        cn = read(basicClass);
        if (GUI_TEXT_FIELD.equals(cn.name) || "avf".equals(cn.name)) {
            byte[] transformed = transformGuiTextField(cn);
            return transformed == null ? basicClass : transformed;
        }
        if (MINECRAFT.equals(cn.name) || "atv".equals(cn.name)) {
            byte[] transformed = transformMinecraft(cn);
            return transformed == null ? basicClass : transformed;
        }
        if (GUI_CONTAINER_CREATIVE.equals(cn.name) || "axm".equals(cn.name)) {
            byte[] transformed = transformGuiContainerCreative(cn);
            return transformed == null ? basicClass : transformed;
        }
        if ("net/minecraft/util/ChatAllowedCharacters".equals(cn.name) || "v".equals(cn.name)) {
            byte[] transformed = transformChatAllowedCharacters(cn);
            return transformed == null ? basicClass : transformed;
        }
        return basicClass;
    }

    private byte[] transformGuiTextField(byte[] bytes) {
        byte[] transformed = transformGuiTextField(read(bytes));
        return transformed == null ? bytes : transformed;
    }

    private byte[] transformGuiTextField(ClassNode cn) {
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
        return changed ? write(cn) : null;
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
        byte[] transformed = transformMinecraft(read(bytes));
        return transformed == null ? bytes : transformed;
    }

    private byte[] transformGuiContainerCreative(byte[] bytes) {
        byte[] transformed = transformGuiContainerCreative(read(bytes));
        return transformed == null ? bytes : transformed;
    }

    private byte[] transformGuiContainerCreative(ClassNode cn) {
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
        return changed ? write(cn) : null;
    }

    private byte[] transformMinecraft(ClassNode cn) {
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
        return changed ? write(cn) : null;
    }

    private byte[] transformChatAllowedCharacters(byte[] bytes) {
        byte[] transformed = transformChatAllowedCharacters(read(bytes));
        return transformed == null ? bytes : transformed;
    }

    private byte[] transformChatAllowedCharacters(ClassNode cn) {
        boolean changed = false;
        for (Object method : cn.methods) {
            MethodNode mn = (MethodNode) method;
            if (isAllowedCharacter(mn)) {
                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
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
                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
                mn.instructions.add(new VarInsnNode(ALOAD, 0));
                mn.instructions.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "filterAllowedCharacters", "(Ljava/lang/String;)Ljava/lang/String;"));
                mn.instructions.add(new InsnNode(ARETURN));
                changed = true;
            }
        }
        return changed ? write(cn) : null;
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
