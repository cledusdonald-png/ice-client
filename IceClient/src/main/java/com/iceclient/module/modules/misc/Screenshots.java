package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileFilter;

/**
 * Screenshot manager: toast on capture, auto-copy the path, open the folder.
 *
 * <p>1.8.9's Forge has no screenshot event (that arrives in 1.12), and vanilla
 * takes the shot inside {@code Minecraft.dispatchKeypresses} before any hook we
 * could reach. Rather than mixin into that for a purely cosmetic feature, this
 * polls the screenshots directory for a file newer than the last one it saw --
 * which also catches shots taken by OptiFine or a bound external key, not just
 * F2.
 */
public class Screenshots extends Module {

   /** Polling every tick is plenty; screenshots are a human-speed event. */
   private static final long SETTLE_MS = 400L;

   private final BooleanSetting toast = this.addBool("Toast on capture", true);
   private final BooleanSetting copyPath = this.addBool("Copy path to clipboard", false);
   private final BooleanSetting chatLink = this.addBool("Clickable chat message", true);

   private File dir;
   private long newestSeen;

   public Screenshots() {
      super("Screenshots", "Toasts and shortcuts when you take a screenshot", ModuleCategory.GENERAL);
   }

   protected void onEnable() {
      this.dir = new File(this.mc.mcDataDir, "screenshots");
      // Baseline against the current newest file, otherwise enabling the module
      // immediately announces whatever screenshot you took last week.
      this.newestSeen = this.newestTimestamp();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.dir == null) {
         return;
      }

      File latest = this.newestFile();
      if(latest == null || latest.lastModified() <= this.newestSeen) {
         return;
      }

      // PNG encoding is not atomic -- the file appears before it is finished
      // being written. Waiting for it to stop changing avoids handing the user
      // a path to a half-written image.
      if(System.currentTimeMillis() - latest.lastModified() < SETTLE_MS) {
         return;
      }

      this.newestSeen = latest.lastModified();
      this.announce(latest);
   }

   private void announce(File file) {
      if(this.toast.get()) {
         NotificationManager.post("Screenshot saved", file.getName(), Notification.Type.SUCCESS);
      }

      if(this.copyPath.get()) {
         try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                  .setContents(new StringSelection(file.getAbsolutePath()), null);
         } catch (Throwable t) {
            // Headless or a locked clipboard -- not worth interrupting the user.
         }
      }

      if(this.chatLink.get() && this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(
               EnumChatFormatting.AQUA + "[Ice] " + EnumChatFormatting.RESET
                     + "Saved " + EnumChatFormatting.GREEN + file.getName()));
      }

   }

   /** Opens the screenshots folder in the OS file browser. */
   public void openFolder() {
      if(this.dir == null || !this.dir.isDirectory()) {
         return;
      }

      try {
         Desktop.getDesktop().open(this.dir);
      } catch (Throwable t) {
         NotificationManager.post("Screenshots", "Could not open folder", Notification.Type.ERROR);
      }

   }

   private File newestFile() {
      if(this.dir == null || !this.dir.isDirectory()) {
         return null;
      }

      File[] files = this.dir.listFiles(new FileFilter() {
         public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase().endsWith(".png");
         }
      });

      if(files == null || files.length == 0) {
         return null;
      }

      File best = files[0];
      for(File f : files) {
         if(f.lastModified() > best.lastModified()) {
            best = f;
         }
      }

      return best;
   }

   private long newestTimestamp() {
      File f = this.newestFile();
      return f == null ? System.currentTimeMillis() : f.lastModified();
   }
}
