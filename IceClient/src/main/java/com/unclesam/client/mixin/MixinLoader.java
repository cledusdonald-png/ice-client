package com.unclesam.client.mixin;

import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.Map;

/**
 * Coremod entry point: boots Mixin early enough to patch vanilla classes.
 *
 * <p>Ice shipped without mixins; this was added so FreeLook can intercept mouse
 * look and swap the camera rotation, neither of which has a Forge event.
 */
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.Name("IceClient")
@IFMLLoadingPlugin.SortingIndex(1001)
public class MixinLoader implements IFMLLoadingPlugin {

    public MixinLoader() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.iceclient.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    /**
     * FML puts coremod jars on ignore/reparse lists so {@code @Mod} discovery
     * skips them. Clearing both lets the same jar load as a mod too.
     */
    @Override
    public void injectData(Map<String, Object> data) {
        File location = (File) data.get("coremodLocation");
        if (location == null) return;
        String name = location.getName();
        CoreModManager.getIgnoredMods().remove(name);
        CoreModManager.getReparseableCoremods().remove(name);
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
