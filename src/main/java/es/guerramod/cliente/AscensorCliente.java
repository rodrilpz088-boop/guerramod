package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.red.Red;
import es.guerramod.vivienda.Ascensores;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Detecta saltar / agacharse encima de un ascensor y se lo dice al servidor. */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AscensorCliente {
    private AscensorCliente() {}

    private static boolean saltaba, agachado;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return;
        boolean salta = mc.options.keyJump.isDown(), agacha = mc.options.keyShift.isDown();
        BlockPos pie = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        if (Ascensores.esAscensor(p.level().getBlockState(pie))) {
            if (salta && !saltaba) Red.CANAL.sendToServer(new Red.Ascensor(true));
            if (agacha && !agachado) Red.CANAL.sendToServer(new Red.Ascensor(false));
        }
        saltaba = salta;
        agachado = agacha;
    }
}
