package com.iceclient.module.modules.hud;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ScoreboardModule extends Module {
   private static final int SIDEBAR_SLOT = 1;
   private final NumberSetting scale = (NumberSetting)this.addSetting(new NumberSetting("Scale", 1.0D, 0.5D, 2.0D, 0.05D));
   private final BooleanSetting hideNumbers = (BooleanSetting)this.addSetting(new BooleanSetting("Hide red numbers", false));
   private final BooleanSetting background = (BooleanSetting)this.addSetting(new BooleanSetting("Background", true));
   private ScoreObjective hidden;

   public ScoreboardModule() {
      super("Scoreboard", "Resize the server\'s scoreboard", ModuleCategory.GENERAL, 0);
   }

   private Scoreboard scoreboard() {
      return this.mc.theWorld == null?null:this.mc.theWorld.getScoreboard();
   }

   protected void onDisable() {
      this.restore();
   }

   private void restore() {
      Scoreboard sb = this.scoreboard();
      if(sb != null && this.hidden != null) {
         sb.setObjectiveInDisplaySlot(1, this.hidden);
      }

      this.hidden = null;
   }

   @SubscribeEvent
   public void onPre(Pre event) {
      if(event.type == ElementType.ALL) {
         Scoreboard sb = this.scoreboard();
         if(sb != null) {
            if(!this.isEnabled()) {
               this.restore();
            } else {
               ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
               if(obj != null) {
                  this.hidden = obj;
                  sb.setObjectiveInDisplaySlot(1, (ScoreObjective)null);
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onPost(Post event) {
      if(event.type == ElementType.ALL) {
         ScoreObjective obj = this.hidden;
         this.restore();
         if(this.isEnabled() && obj != null && this.mc.thePlayer != null) {
            this.draw(obj);
         }
      }
   }

   private void draw(ScoreObjective objective) {
      Scoreboard scoreboard = objective.getScoreboard();
      if(scoreboard != null) {
         List<Score> scores = new ArrayList();

         for(Score score : scoreboard.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if(name != null && !name.startsWith("#")) {
               scores.add(score);
            }
         }

         if(!((List)scores).isEmpty()) {
            if(scores.size() > 15) {
               scores = scores.subList(scores.size() - 15, scores.size());
            }

            float s = (float)this.scale.get();
            ScaledResolution res = new ScaledResolution(this.mc);
            int virtualW = (int)((float)res.getScaledWidth() / s);
            int virtualH = (int)((float)res.getScaledHeight() / s);
            String title = objective.getDisplayName();
            int lineH = this.mc.fontRendererObj.FONT_HEIGHT;
            int maxWidth = this.mc.fontRendererObj.getStringWidth(title);

            for(Score score : scores) {
               ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
               String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
               int width = this.mc.fontRendererObj.getStringWidth(line);
               if(!this.hideNumbers.get()) {
                  width += this.mc.fontRendererObj.getStringWidth(" " + score.getScorePoints());
               }

               maxWidth = Math.max(maxWidth, width);
            }

            int totalH = scores.size() * lineH;
            int baseline = virtualH / 2 + totalH / 3;
            int pad = 3;
            int left = virtualW - maxWidth - pad;
            int right = virtualW - pad + 2;
            GlStateManager.pushMatrix();
            GlStateManager.scale(s, s, 1.0F);
            int index = 0;

            for(Score score : scores) {
               ++index;
               ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
               String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
               String points = EnumChatFormatting.RED + "" + score.getScorePoints();
               int y = baseline - index * lineH;
               if(this.background.get()) {
                  Gui.drawRect(left - 2, y, right, y + lineH, 1342177280);
               }

               this.mc.fontRendererObj.drawString(name, left, y, -1);
               if(!this.hideNumbers.get()) {
                  this.mc.fontRendererObj.drawString(points, right - this.mc.fontRendererObj.getStringWidth(points), y, -1);
               }

               if(index == scores.size()) {
                  if(this.background.get()) {
                     Gui.drawRect(left - 2, y - lineH - 1, right, y - 1, 1610612736);
                     Gui.drawRect(left - 2, y - 1, right, y, 1342177280);
                  }

                  this.mc.fontRendererObj.drawString(title, left + maxWidth / 2 - this.mc.fontRendererObj.getStringWidth(title) / 2, y - lineH, -1);
               }
            }

            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }
}
