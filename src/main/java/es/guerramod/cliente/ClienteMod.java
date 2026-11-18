package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.registro.ModEntidades;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registro de cosas del cliente (bus del mod). */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClienteMod {
    private ClienteMod() {}

    @SubscribeEvent
    public static void teclas(RegisterKeyMappingsEvent e) {
        e.register(Teclas.MINIMAPA);
        e.register(Teclas.DISPARAR);
        e.register(Teclas.BOMBA);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntidades.BALA.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.5f, true));
        e.registerEntityRenderer(ModEntidades.COHETE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2f, true));
        e.registerEntityRenderer(ModEntidades.GRANADA.get(), ThrownItemRenderer::new);
        e.registerEntityRenderer(ModEntidades.BOMBA_AEREA.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.5f, false));
        e.registerEntityRenderer(ModEntidades.MISIL.get(), ctx -> new ThrownItemRenderer<>(ctx, 2.5f, true));
        e.registerEntityRenderer(ModEntidades.AVION.get(), ctx -> new AvionRenderer<>(ctx, 1.6f, AvionRenderer.TEXTURA_CAZA));
        e.registerEntityRenderer(ModEntidades.BOMBARDERO.get(), ctx -> new AvionRenderer<>(ctx, 2.2f, AvionRenderer.TEXTURA_BOMBARDERO));
    }

    @SubscribeEvent
    public static void capas(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(AvionModel.CAPA, AvionModel::crearCapa);
    }

    @SubscribeEvent
    public static void hud(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("minimapa", Minimapa.INSTANCIA);
    }
}
