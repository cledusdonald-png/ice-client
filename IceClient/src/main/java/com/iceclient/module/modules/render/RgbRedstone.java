package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;

/**
 * RGB / chroma tint on redstone wire. The actual colour swap is done in a
 * mixin on {@code BlockRedstoneWire}; this module owns the toggle and knobs.
 */
public class RgbRedstone extends Module {

    private final NumberSetting speed = addNumber("Speed", 4, 1, 20, 1);
    private final NumberSetting saturation = addNumber("Saturation", 1, 0.2, 1, 0.05);
    private final BooleanSetting poweredOnly = addBool("Powered Only", false);

    public RgbRedstone() {
        super("RGB Redstone", "Chroma tint on redstone dust", ModuleCategory.MECHANIC);
    }

    public float getSpeed() {
        return (float) speed.get();
    }

    public float getSaturation() {
        return (float) saturation.get();
    }

    public boolean isPoweredOnly() {
        return poweredOnly.get();
    }
}
