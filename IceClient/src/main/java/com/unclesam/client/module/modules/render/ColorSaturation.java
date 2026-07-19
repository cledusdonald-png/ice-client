package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.NumberSetting;

/**
 * World colour saturation boost. Applied in a mixin on {@code EntityRenderer}.
 */
public class ColorSaturation extends Module {

    private final NumberSetting amount = addNumber("Saturation", 1.2, 0.5, 2.5, 0.05);

    public ColorSaturation() {
        super("Color Saturation", "Boost world colour saturation", ModuleCategory.MECHANIC);
    }

    public float getAmount() {
        return (float) amount.get();
    }
}
