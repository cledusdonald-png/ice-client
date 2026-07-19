package com.iceclient.mixin.mixins;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.render.ItemPhysics;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Physics-style item drops: faster spin when {@link ItemPhysics} is on.
 */
@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem {

    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItem;getAge()I"))
    private int iceclient$spinAge(EntityItem item) {
        Module m = ModuleManager.getByName("Item Physics");
        if (m == null || !m.isEnabled()) return item.getAge();
        return (int) (item.getAge() * ((ItemPhysics) m).getSpinSpeed());
    }
}
