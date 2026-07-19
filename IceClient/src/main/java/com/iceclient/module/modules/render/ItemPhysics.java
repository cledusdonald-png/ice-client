package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;

/**
 * Flat / physics-style dropped items. The rotation and bob are patched in a
 * mixin on {@code RenderEntityItem}.
 */
public class ItemPhysics extends Module {

    private final NumberSetting spinSpeed = addNumber("Spin Speed", 1, 0, 3, 0.1);
    private final BooleanSetting flat = addBool("Flat On Ground", true);
    private final NumberSetting bob = addNumber("Bob Amount", 0, 0, 1, 0.05);

    public ItemPhysics() {
        super("Item Physics", "Physics-style dropped item rendering", ModuleCategory.MECHANIC);
    }

    public float getSpinSpeed() {
        return (float) spinSpeed.get();
    }

    public boolean isFlat() {
        return flat.get();
    }

    public float getBob() {
        return (float) bob.get();
    }
}
