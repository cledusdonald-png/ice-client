package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.BetterChat;
import com.iceclient.module.modules.misc.ChatCopyTargets;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Click-to-copy chat lines.
 *
 * <p>The per-line copy icons are drawn by {@code MixinGuiNewChat}, which is the
 * only place with the drawn-line geometry, and recorded in
 * {@link ChatCopyTargets}. This handler just hit-tests a click against those:
 * clicking an icon copies that exact line, and a shift-click anywhere on a line
 * copies it too.
 *
 * <p>Injected at HEAD and cancelling when it copies, because vanilla's own
 * handler would otherwise also fire the line's click event and open any link it
 * carried.
 */
@Mixin(GuiChat.class)
public class MixinGuiChat {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void iceclient$copyLine(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 0) return;

        BetterChat m = (BetterChat) ModuleManager.getByName("BetterChat");
        if (m == null || !m.isEnabled() || !m.copyOnClick()) return;

        // Icon click copies unconditionally -- that is what the visible target
        // is for. A click elsewhere copies the line only when the Shift rule is
        // met, so an ordinary click still follows links and server clickables.
        String text = ChatCopyTargets.iconAt(mouseX, mouseY);

        if (text == null) {
            boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (m.copyNeedsShift() && !shift) return;

            text = ChatCopyTargets.rowAt(mouseX, mouseY);
        }

        if (text == null || text.isEmpty()) return;

        if (m.copyStripColors()) {
            text = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        }

        if (text.isEmpty()) return;

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            NotificationManager.post("Copied", trim(text), Notification.Type.SUCCESS, 1500L);
        } catch (Throwable t) {
            NotificationManager.post("Copy failed", "Clipboard unavailable", Notification.Type.ERROR);
        }

        ci.cancel();
    }

    /** Keeps the toast readable when the copied line is a wall of text. */
    private static String trim(String s) {
        return s.length() <= 40 ? s : s.substring(0, 37) + "...";
    }
}
