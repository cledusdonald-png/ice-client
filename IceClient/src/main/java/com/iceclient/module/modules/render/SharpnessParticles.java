package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.NumberSetting;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Cosmetic: puffs crit-style particles off a target when you hit it with a
 * sharpness-enchanted weapon, scaled to the enchant level. Purely visual --
 * client-side particles, no effect on damage.
 */
public class SharpnessParticles extends Module {

    private final NumberSetting perLevel = addNumber("Particles / Level", 3, 1, 10, 1);

    public SharpnessParticles() {
        super("Sharpness Particles", "Particles when hitting with a sharp weapon", ModuleCategory.MECHANIC);
    }

    // 1.8.9 exposes entityPlayer/target as public fields on the event.
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entityPlayer != mc.thePlayer || event.target == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, held);
        if (level <= 0) return;

        Entity t = event.target;
        int count = level * perLevel.getInt();
        for (int i = 0; i < count; i++) {
            double ox = (mc.theWorld.rand.nextDouble() - 0.5) * t.width;
            double oy = mc.theWorld.rand.nextDouble() * t.height;
            double oz = (mc.theWorld.rand.nextDouble() - 0.5) * t.width;
            mc.theWorld.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    t.posX + ox, t.posY + oy, t.posZ + oz,
                    (mc.theWorld.rand.nextDouble() - 0.5) * 0.2,
                    mc.theWorld.rand.nextDouble() * 0.2,
                    (mc.theWorld.rand.nextDouble() - 0.5) * 0.2);
        }
    }
}
