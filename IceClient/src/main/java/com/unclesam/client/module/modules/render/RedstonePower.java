package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws each redstone wire's signal strength on top of it.
 *
 * <p>Redstone power runs 0-15 (16 levels): 15 straight off a block/torch,
 * losing one per block travelled, 0 = unpowered. Colour ramps dark red (weak)
 * to bright (strong) so you can read a line's falloff at a glance.
 *
 * <p>The scan walks a box around you, so it runs on a timer and caches rather
 * than re-walking every frame.
 */
public class RedstonePower extends Module {

    private final NumberSetting range = addNumber("Range", 16, 4, 48, 2);
    private final NumberSetting scanTicks = addNumber("Rescan (ticks)", 5, 1, 40, 1);
    private final BooleanSetting hideZero = addBool("Hide Unpowered", true);
    private final NumberSetting textScale = addNumber("Text Scale", 1, 0.5, 3, 0.1);

    private final Map<BlockPos, Integer> wires = new LinkedHashMap<>();
    private int ticks;

    public RedstonePower() {
        super("Redstone Power", "Shows redstone signal strength (0-15)", ModuleCategory.MECHANIC);
    }

    @Override
    protected void onDisable() {
        wires.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (++ticks < scanTicks.getInt()) return;
        ticks = 0;
        scan();
    }

    private void scan() {
        wires.clear();
        int r = range.getInt();
        BlockPos me = mc.thePlayer.getPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = me.add(x, y, z);
                    if (pos.getY() < 1 || pos.getY() > 255) continue;
                    if (!mc.theWorld.isBlockLoaded(pos, false)) continue;

                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (!(state.getBlock() instanceof BlockRedstoneWire)) continue;

                    int power = (Integer) state.getValue(BlockRedstoneWire.POWER);
                    if (hideZero.get() && power == 0) continue;
                    wires.put(pos, power);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!isEnabled() || wires.isEmpty()) return;

        for (Map.Entry<BlockPos, Integer> e : new ArrayList<>(wires.entrySet())) {
            BlockPos p = e.getKey();
            int power = e.getValue();

            // dark red at 0 -> bright at 15
            float t = power / 15f;
            int color = ColorUtil.withAlpha(ColorUtil.interpolate(
                    new Color(90, 0, 0).getRGB(), new Color(255, 80, 80).getRGB(), t), 255);

            WorldRenderUtil.text3d(String.valueOf(power),
                    p.getX() + 0.5, p.getY() + 0.15, p.getZ() + 0.5,
                    color, (float) (0.014 * textScale.get()));
        }
    }
}
