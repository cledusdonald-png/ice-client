package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.BetterChat;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
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

    /**
     * Draws a copy glyph beside whichever chat line the cursor is on.
     *
     * <p>Shift-click alone is invisible -- there is no way to discover it. The
     * icon sits just left of the chat area so it never covers message text, and
     * only on the hovered line, so the chat is not permanently gutter-ed.
     */
    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void iceclient$copyIcon(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        BetterChat m = (BetterChat) ModuleManager.getByName("BetterChat");
        if (m == null || !m.isEnabled() || !m.copyOnClick() || !m.showCopyIcon()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY()) == null) return;

        int x = 2;
        int y = mouseY - 4;

        // Drawn from rectangles rather than a glyph. The obvious characters for
        // "copy" are outside the range Minecraft's default font ships, so a
        // glyph renders as an empty box unless Force Unicode happens to be on.
        Gui.drawRect(x, y - 1, x + BetterChat.COPY_ICON_W, y + 9, 0x90000000);

        int back = 0xFFAAAAAA;
        int front = 0xFFFFFFFF;

        // Back sheet: outline only, offset up and right.
        Gui.drawRect(x + 4, y + 0, x + 9, y + 1, back);
        Gui.drawRect(x + 4, y + 0, x + 5, y + 5, back);
        Gui.drawRect(x + 8, y + 0, x + 9, y + 5, back);

        // Front sheet: solid, so the two read as stacked pages.
        Gui.drawRect(x + 1, y + 3, x + 7, y + 8, front);
        Gui.drawRect(x + 2, y + 4, x + 6, y + 7, 0xFF303030);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void iceclient$copyLine(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 0) return;

        BetterChat m = (BetterChat) ModuleManager.getByName("BetterChat");
        if (m == null || !m.isEnabled() || !m.copyOnClick()) return;

        // Clicking the icon copies without Shift; that is the whole point of
        // having a visible target. Anywhere else still honours the setting.
        boolean onIcon = m.showCopyIcon() && mouseX >= 2 && mouseX <= 2 + BetterChat.COPY_ICON_W;

        if (!onIcon && m.copyNeedsShift()
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
