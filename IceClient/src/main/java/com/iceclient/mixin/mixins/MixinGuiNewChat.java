package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.BetterChat;
import com.iceclient.module.modules.misc.ChatCopyTargets;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    private int iceclient$scrollback(int original) {
        BetterChat m = iceclient$module();
        return m == null || !m.isEnabled() ? original : m.scrollback();
    }

    /**
     * Prefixes the timestamp here rather than in {@code setChatLine} because
     * {@code refreshChat} re-feeds stored components back through that method --
     * prefixing there would stack another timestamp on every resize.
     */
    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, index = 1)
    private IChatComponent iceclient$timestamp(IChatComponent original) {
        BetterChat m = iceclient$module();
        if (m == null || !m.isEnabled() || !m.timestamps() || original == null) {
            return original;
        }

        String stamp = new SimpleDateFormat(m.timestampFormat()).format(new Date());
        return new ChatComponentText(m.timestampColor() + "[" + stamp + "] §r").appendSibling(original);
    }

    @Inject(method = "getChatWidth", at = @At("HEAD"), cancellable = true)
    private void iceclient$chatWidth(CallbackInfoReturnable<Integer> cir) {
        BetterChat m = iceclient$module();
        if (m != null && m.isEnabled() && m.customSize()) {
            cir.setReturnValue(Integer.valueOf(m.chatWidth()));
        }
    }

    @Inject(method = "getChatHeight", at = @At("HEAD"), cancellable = true)
    private void iceclient$chatHeight(CallbackInfoReturnable<Integer> cir) {
        BetterChat m = iceclient$module();
        if (m != null && m.isEnabled() && m.customSize()) {
            // Focused and unfocused get separate heights in vanilla; mirroring
            // that keeps an open chat tall without a huge idle overlay.
            cir.setReturnValue(Integer.valueOf(
                    ((GuiNewChat) (Object) this).getChatOpen() ? m.chatHeightFocused() : m.chatHeightUnfocused()));
        }
    }

    private static BetterChat iceclient$module() {
        return (BetterChat) ModuleManager.getByName("BetterChat");
    }

    // --- Per-line copy icons -------------------------------------------------
    //
    // Only this class can see the drawn-line list and the chat scale, so the
    // icon geometry is computed here and recorded in ChatCopyTargets for the
    // click handler in MixinGuiChat to look up. Computing the row positions the
    // same way drawChat does is what keeps each icon aligned with its line --
    // an icon a few pixels off its row is worse than none.

    @Shadow private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;

    @Shadow public boolean getChatOpen() { return false; }
    @Shadow public int getChatWidth() { return 0; }
    @Shadow public int getChatHeight() { return 0; }
    @Shadow public float getChatScale() { return 1.0F; }
    @Shadow public int getLineCount() { return 0; }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void iceclient$copyIcons(int updateCounter, CallbackInfo ci) {
        ChatCopyTargets.clear();

        BetterChat m = iceclient$module();
        if (m == null || !m.isEnabled() || !m.copyOnClick() || !m.showCopyIcon()) return;

        // Icons only make sense with the chat open, since that is the only time
        // it can be clicked. This also matches vanilla, which only makes lines
        // interactive while open.
        if (!getChatOpen() || drawnChatLines.isEmpty()) return;

        float scale = getChatScale();
        int visible = Math.min(getLineCount(), drawnChatLines.size());

        // The right edge of the chat box, in GUI pixels, mirroring the width
        // drawChat uses: ceil(chatWidth / scale) + 4, scaled back up.
        int localWidth = MathHelper.ceiling_float_int((float) getChatWidth() / scale) + 4;
        int boxRight = 2 + Math.round(localWidth * scale);

        int iconW = BetterChat.COPY_ICON_W;
        int iconLeft = boxRight + 1;

        for (int i = 0; i < visible; i++) {
            int index = i + scrollPos;
            if (index < 0 || index >= drawnChatLines.size()) continue;

            ChatLine line = drawnChatLines.get(index);
            if (line == null) continue;

            // drawChat draws row i at local y in [-i*9 - 9, -i*9], under a
            // translate(2, 20) scale(scale). Convert both edges to GUI pixels.
            int rowTop = 20 + Math.round((-i * 9 - 9) * scale);
            int rowBottom = 20 + Math.round((-i * 9) * scale);

            String text = line.getChatComponent().getUnformattedText();
            if (text == null || text.isEmpty()) continue;

            iceclient$drawCopyGlyph(iconLeft, rowTop + 1);
            ChatCopyTargets.add(rowTop, rowBottom, iconLeft, iconLeft + iconW, text);
        }
    }

    /** Two overlapping sheets, drawn from rectangles so no font glyph is needed. */
    private static void iceclient$drawCopyGlyph(int x, int y) {
        Gui.drawRect(x, y - 1, x + BetterChat.COPY_ICON_W, y + 9, 0x90000000);

        int back = 0xFFAAAAAA;
        Gui.drawRect(x + 4, y + 0, x + 9, y + 1, back);
        Gui.drawRect(x + 4, y + 0, x + 5, y + 5, back);
        Gui.drawRect(x + 8, y + 0, x + 9, y + 5, back);

        Gui.drawRect(x + 1, y + 3, x + 7, y + 8, 0xFFFFFFFF);
        Gui.drawRect(x + 2, y + 4, x + 6, y + 7, 0xFF303030);
    }
}
