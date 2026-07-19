package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.BetterChat;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Copies the chat line under the cursor to the clipboard.
 *
 * <p>Uses {@code GuiNewChat.getChatComponent}, which is the same lookup vanilla
 * uses to decide what you clicked -- so the line copied is exactly the line you
 * are pointing at, including when the chat is scrolled.
 *
 * <p>Injected at HEAD and cancelling, because vanilla's own handler runs click
 * events on the component. Letting it run as well would both copy the line and
 * open whatever link it carried.
 */
@Mixin(GuiChat.class)
public class MixinGuiChat {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void iceclient$copyLine(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 0) return;

        BetterChat m = (BetterChat) ModuleManager.getByName("BetterChat");
        if (m == null || !m.isEnabled() || !m.copyOnClick()) return;

        if (m.copyNeedsShift()
                && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        IChatComponent line = mc.ingameGUI.getChatGUI().getChatComponent(
                Mouse.getX(), Mouse.getY());
        if (line == null) return;

        String text = line.getUnformattedText();
        if (m.copyStripColors()) {
            text = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        }

        if (text == null || text.isEmpty()) return;

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
