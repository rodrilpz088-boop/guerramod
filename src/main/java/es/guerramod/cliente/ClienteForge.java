package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.entidades.AvionEntity;
import es.guerramod.items.ArmaItem;
import es.guerramod.red.Red;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
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
        boolean conArma = mc.player.getMainHandItem().getItem() instanceof ArmaItem;
        while (Teclas.DISPARAR.consumeClick()) {
            // en el avión dispara (arriba, con isDown); con un arma en la mano, recarga
            if (!enAvion && conArma && mc.screen == null) {
                Red.CANAL.sendToServer(new Red.AccionArma(Red.AccionArma.RECARGAR, false));
            }
        }
        while (Teclas.MODO.consumeClick()) {
            if (conArma) Red.CANAL.sendToServer(new Red.AccionArma(Red.AccionArma.MODO, false));
        }
        while (Teclas.BOMBA.consumeClick()) {
            if (enAvion) Red.CANAL.sendToServer(new Red.AccionAvion(1));
        }
    }
}
