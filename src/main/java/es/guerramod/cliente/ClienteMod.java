package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.registro.ModEntidades;
import es.guerramod.bloques.hogar.Luminaria;
import es.guerramod.registro.ModBloques;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.registries.RegistryObject;
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

    /** Cielo, sol, luna, estrellas y nubes realistas en el mundo normal. */
    @SubscribeEvent
    public static void cielo(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent e) {
        e.register(net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD_EFFECTS, new es.guerramod.cliente.cielo.CieloMundo());
    }

    @SubscribeEvent
    public static void teclas(RegisterKeyMappingsEvent e) {
        e.register(Teclas.MINIMAPA);
        e.register(Teclas.DISPARAR);
        e.register(Teclas.BOMBA);
        e.register(Teclas.MODO);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntidades.BALA.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.5f, true));
        e.registerEntityRenderer(ModEntidades.COHETE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2f, true));
        e.registerEntityRenderer(ModEntidades.GRANADA.get(), ThrownItemRenderer::new);
        e.registerEntityRenderer(ModEntidades.BOMBA_AEREA.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.5f, false));
        e.registerEntityRenderer(ModEntidades.MISIL.get(), ctx -> new ThrownItemRenderer<>(ctx, 2.5f, true));
        e.registerEntityRenderer(ModEntidades.ASIENTO.get(), NoopRenderer::new);
        e.registerEntityRenderer(ModEntidades.HIDROAVION.get(), ctx -> new AvionRenderer<>(ctx, 2.4f, AvionRenderer.TEXTURA_HIDROAVION));
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
        e.registerAboveAll("armas", HudArmas.INSTANCIA);
    }

    /** Las luces LED se tiñen con su color (y se ven oscuras apagadas). */
    @SubscribeEvent
    public static void coloresBloques(RegisterColorHandlersEvent.Block e) {
        Block[] leds = ModBloques.LEDS.stream().map(RegistryObject::get).toArray(Block[]::new);
        e.register((st, nivel, pos, tinte) -> {
            if (tinte != 0) return -1;
            if (st.hasProperty(BlockStateProperties.LIT) && !st.getValue(BlockStateProperties.LIT)) return 0x3A3A3A;
            return st.hasProperty(Luminaria.COLOR) ? Luminaria.RGB[st.getValue(Luminaria.COLOR)] : 0xFFFFFF;
        }, leds);
    }

    @SubscribeEvent
    public static void coloresItems(RegisterColorHandlersEvent.Item e) {
        Block[] leds = ModBloques.LEDS.stream().map(RegistryObject::get).toArray(Block[]::new);
        e.register((st, tinte) -> tinte == 0 ? 0xF2F6FF : -1, leds);
    }
}
