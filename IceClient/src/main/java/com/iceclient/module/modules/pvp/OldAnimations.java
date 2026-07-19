package com.iceclient.module.modules.pvp;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Orbit OldAnimations — all 1.7 animation toggles. */
public class OldAnimations extends Module {

    public final BooleanSetting blockhit = addBool("Block Hit", true);
    public final BooleanSetting heartFlash = addBool("Heart Flash", true);
    public final BooleanSetting armorDamage = addBool("Armor Damage", true);
    public final BooleanSetting sneak = addBool("Sneak", true);
    public final BooleanSetting blockbreak = addBool("Block Break", true);
    public final BooleanSetting itemheld = addBool("Item Held", true);
    public final BooleanSetting itemanimation = addBool("Item Animation", true);
    public final BooleanSetting debugHitbox = addBool("Debug Hitbox", false);

    public OldAnimations() {
        super("OldAnimations", "1.7-style animations", ModuleCategory.COMBAT);
    }

    public boolean anyEnabled() {
        return isEnabled() && (blockhit.get() || heartFlash.get() || armorDamage.get()
                || sneak.get() || blockbreak.get() || itemheld.get() || itemanimation.get());
    }
}
