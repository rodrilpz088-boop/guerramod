package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.entidades.AvionEntity;
import es.guerramod.items.ArmaItem;
import es.guerramod.red.Red;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del cliente durante el juego: teclas y mira del francotirador. */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClienteForge {
    private static int cdDisparo;

    private ClienteForge() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (Teclas.MINIMAPA.consumeClick()) {
            Minimapa.visible = !Minimapa.visible;
            mc.player.displayClientMessage(Component.literal(Minimapa.visible ? "Minimapa activado" : "Minimapa oculto"), true);
        }
        boolean enAvion = mc.player.getVehicle() instanceof AvionEntity;
        if (cdDisparo > 0) cdDisparo--;
        // la ametralladora dispara mientras mantienes la tecla
        if (enAvion && Teclas.DISPARAR.isDown() && cdDisparo == 0) {
            Red.CANAL.sendToServer(new Red.AccionAvion(0));
            cdDisparo = 3;
        }
        while (Teclas.DISPARAR.consumeClick()) {
            // se gestiona arriba con isDown
        }
        while (Teclas.BOMBA.consumeClick()) {
            if (enAvion) Red.CANAL.sendToServer(new Red.AccionAvion(1));
        }
    }

    @SubscribeEvent
    public static void mira(ComputeFovModifierEvent e) {
        if (e.getPlayer().isShiftKeyDown() && e.getPlayer().getMainHandItem().getItem() instanceof ArmaItem arma && arma.esFrancotirador()) {
            e.setNewFovModifier(0.2f);
        }
    }
}
