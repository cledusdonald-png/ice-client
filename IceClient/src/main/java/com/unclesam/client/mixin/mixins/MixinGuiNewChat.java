package com.unclesam.client.mixin.mixins;

import com.unclesam.client.module.ModuleManager;
import com.unclesam.client.module.modules.misc.BetterChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * BetterChat: longer scrollback, timestamps, and a chat box sized past vanilla's
 * limits.
 *
 * <p>All three live in {@link GuiNewChat}'s internals with no Forge hook:
 * the 100-line cap is a literal inside {@code setChatLine}, and the box
 * dimensions come from {@code getChatWidth}/{@code getChatHeight}, which clamp
 * to the vanilla slider range.
 */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    /**
     * Vanilla trims both the raw and the wrapped line lists to 100 entries.
     * {@link ModifyConstant} rewrites every {@code 100} in the method, which is
     * exactly the two trim loops -- there are no other hundreds in there.
     *
     * <p>Returning the vanilla value when the module is off keeps memory
     * behaviour identical rather than merely large.
     */
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int unclesam$scrollback(int original) {
        BetterChat m = unclesam$module();
        return m == null || !m.isEnabled() ? original : m.scrollback();
    }

    /**
     * Prefixes the timestamp here rather than in {@code setChatLine} because
     * {@code refreshChat} re-feeds stored components back through that method --
     * prefixing there would stack another timestamp on every resize.
     */
    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, index = 1)
    private IChatComponent unclesam$timestamp(IChatComponent original) {
        BetterChat m = unclesam$module();
        if (m == null || !m.isEnabled() || !m.timestamps() || original == null) {
            return original;
        }

        String stamp = new SimpleDateFormat(m.timestampFormat()).format(new Date());
        return new ChatComponentText(m.timestampColor() + "[" + stamp + "] §r").appendSibling(original);
    }

    @Inject(method = "getChatWidth", at = @At("HEAD"), cancellable = true)
    private void unclesam$chatWidth(CallbackInfoReturnable<Integer> cir) {
        BetterChat m = unclesam$module();
        if (m != null && m.isEnabled() && m.customSize()) {
            cir.setReturnValue(Integer.valueOf(m.chatWidth()));
        }
    }

    @Inject(method = "getChatHeight", at = @At("HEAD"), cancellable = true)
    private void unclesam$chatHeight(CallbackInfoReturnable<Integer> cir) {
        BetterChat m = unclesam$module();
        if (m != null && m.isEnabled() && m.customSize()) {
            // Focused and unfocused get separate heights in vanilla; mirroring
            // that keeps an open chat tall without a huge idle overlay.
            cir.setReturnValue(Integer.valueOf(
                    ((GuiNewChat) (Object) this).getChatOpen() ? m.chatHeightFocused() : m.chatHeightUnfocused()));
        }
    }

    private static BetterChat unclesam$module() {
        return (BetterChat) ModuleManager.getByName("BetterChat");
    }
}
