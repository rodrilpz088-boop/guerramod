package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.clima.TiempoCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lo que se ve y se oye de los desastres: la cámara tiembla con los terremotos y cae granizo. */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DesastresCliente {
    private DesastresCliente() {}

    private static float temblor = 0;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        Player p = mc.player;
        if (l == null || p == null || mc.isPaused()) return;
        temblor += (TiempoCliente.temblor - temblor) * 0.15f;
        RandomSource r = l.random;
        if (temblor > 0.05f && l.getGameTime() % 8 == 0) {
            l.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 0.12f * temblor, 0.25f + r.nextFloat() * 0.1f, false);
            for (int i = 0; i < (int) (temblor * 12); i++) {
                l.addParticle(ParticleTypes.ASH, p.getX() + r.nextGaussian() * 6, p.getY() + 3 + r.nextDouble() * 3, p.getZ() + r.nextGaussian() * 6, 0, -0.05, 0);
            }
        }
        float g = TiempoCliente.granizo;
        if (g > 0 && l.isRaining()) {
            int n = (int) (8 + g * 5);
            for (int i = 0; i < n; i++) {
                double x = p.getX() + r.nextGaussian() * 10, z = p.getZ() + r.nextGaussian() * 10;
                BlockPos col = BlockPos.containing(x, p.getY() + 14, z);
                if (!l.isRainingAt(l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col))) continue;
                l.addParticle(ParticleTypes.ITEM_SNOWBALL, x, p.getY() + 10 + r.nextDouble() * 8, z, r.nextGaussian() * 0.05, -1.4, r.nextGaussian() * 0.05);
            }
            if (r.nextFloat() < 0.6f) {
                double x = p.getX() + r.nextGaussian() * 4, z = p.getZ() + r.nextGaussian() * 4;
                l.playLocalSound(x, p.getY(), z, SoundEvents.STONE_HIT, SoundSource.WEATHER, 0.25f + g * 0.05f, 1.5f + r.nextFloat() * 0.4f, false);
            }
        }
    }

    @SubscribeEvent
    public static void camara(ViewportEvent.ComputeCameraAngles e) {
        if (temblor < 0.003f) return;
        double t = (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0) + e.getPartialTick();
        float a = temblor * temblor * 4.5f + temblor * 0.8f;
        float px = (float) (Math.sin(t * 1.9) * 0.6 + Math.sin(t * 4.3 + 1.2) * 0.4);
        float py = (float) (Math.sin(t * 2.3 + 0.7) * 0.6 + Math.sin(t * 5.1) * 0.4);
        float pr = (float) (Math.sin(t * 1.4 + 2.1) * 0.7 + Math.sin(t * 3.7) * 0.3);
        e.setPitch(e.getPitch() + px * a);
        e.setYaw(e.getYaw() + py * a * 0.8f);
        e.setRoll(e.getRoll() + pr * a * 0.9f);
    }
}
