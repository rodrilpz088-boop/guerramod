package es.guerramod.cliente.cielo;

import es.guerramod.GuerraMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Color y densidad de la niebla: la bruma del horizonte toma el color real del cielo (dorada al atardecer, azul de
 * noche) y con aire saturado aparece niebla de verdad (valles y pantanos al amanecer, montañas dentro de las nubes).
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NieblaEventos {
    private NieblaEventos() {}

    private static boolean aplica(Camera cam) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!CieloMundo.activo(level)) return false;
        if (cam.getFluidInCamera() != FogType.NONE) return false;
        Entity e = cam.getEntity();
        return !(e instanceof LivingEntity le && (le.hasEffect(MobEffects.BLINDNESS) || le.hasEffect(MobEffects.DARKNESS)));
    }

    @SubscribeEvent
    public static void color(ViewportEvent.ComputeFogColor e) {
        Camera cam = e.getCamera();
        if (!aplica(cam)) return;
        try {
            ClientLevel level = Minecraft.getInstance().level;
            Atmosfera.actualizar(level, (float) e.getPartialTick(), cam.getPosition().y);
            Vector3f mira = cam.getLookVector();
            float[] c = Atmosfera.colorNiebla(mira.x(), mira.z());
            float v = ventisca(cam);
            if (v > 0) {
                // ventisca: todo blanco grisáceo (más oscuro de noche)
                float luz = (float) Math.max(0.12, Math.min(1, (Atmosfera.sy + 0.08) / 0.35));
                float b = 0.78f * luz + 0.05f;
                c = new float[]{c[0] + (b - c[0]) * v, c[1] + (b * 1.02f - c[1]) * v, c[2] + (b * 1.07f - c[2]) * v};
            }
            e.setRed(c[0]);
            e.setGreen(c[1]);
            e.setBlue(c[2]);
        } catch (Throwable t) {
            CieloMundo.desactivado = true;
            GuerraMod.LOGGER.error("Niebla realista desactivada por un error", t);
        }
    }

    @SubscribeEvent
    public static void distancia(ViewportEvent.RenderFog e) {
        if (e.getMode() != FogRenderer.FogMode.FOG_TERRAIN || !aplica(e.getCamera())) return;
        float v = ventisca(e.getCamera());
        if (v > 0) {
            // ventisca: apenas se ve a 15-25 bloques
            float lejos = e.getFarPlaneDistance();
            float objetivo = 14f + 14f * (1 - v);
            float nuevo = lejos + (objetivo - lejos) * v;
            e.setFarPlaneDistance(nuevo);
            e.setNearPlaneDistance(0f);
            e.setCanceled(true);
            return;
        }
        float n = Atmosfera.niebla;
        if (n < 0.02f) return;
        float lejos = e.getFarPlaneDistance();
        float objetivo = Math.max(12f, lejos * 0.09f);
        float nuevo = lejos + (objetivo - lejos) * (float) Math.pow(n, 0.7);
        e.setFarPlaneDistance(nuevo);
        e.setNearPlaneDistance(nuevo * (0.02f + 0.25f * (1 - n)));
        e.setCanceled(true);
    }

    /** Fuerza de la ventisca donde está la cámara (0 si está bajo techo o no nieva). */
    static float ventisca(Camera cam) {
        float v = es.guerramod.clima.TiempoCliente.ventisca;
        if (v <= 0 || !Lluvia.nevando()) return 0;
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) return 0;
        net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(cam.getPosition());
        int cielo = l.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
        return v * Math.min(1f, cielo / 12f);
    }
}
