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
        float n = Atmosfera.niebla;
        if (n < 0.02f) return;
        float lejos = e.getFarPlaneDistance();
        float objetivo = Math.max(12f, lejos * 0.09f);
        float nuevo = lejos + (objetivo - lejos) * (float) Math.pow(n, 0.7);
        e.setFarPlaneDistance(nuevo);
        e.setNearPlaneDistance(nuevo * (0.02f + 0.25f * (1 - n)));
        e.setCanceled(true);
    }
}
