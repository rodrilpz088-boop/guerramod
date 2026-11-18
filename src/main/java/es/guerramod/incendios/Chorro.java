package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Chorro de agua o polvo: recorre una parábola y apaga lo que toca. */
public final class Chorro {
    private Chorro() {}

    public static int rociar(ServerLevel level, LivingEntity p, double alcance, double radio, double caida, int mojado, double empuje) {
        Vec3 o = p.getEyePosition().add(0, -0.2, 0);
        Vec3 d = p.getLookAngle();
        int n = 0;
        for (double t = 1.0; t <= alcance; t += 1.0) {
            Vec3 pt = o.add(d.scale(t)).add(0, -caida * t * t, 0);
            n += Fuego.apagarZona(level, pt, radio + t * 0.06, mojado);
            if (empuje > 0) {
                for (Entity e : level.getEntities(p, new AABB(pt, pt).inflate(0.8), x -> x instanceof LivingEntity)) {
                    e.push(d.x * empuje, 0.05, d.z * empuje);
                    e.hurtMarked = true;
                }
            }
            BlockPos bp = BlockPos.containing(pt);
            if (level.getBlockState(bp).isSolidRender(level, bp)) {
                level.sendParticles(ParticleTypes.SPLASH, pt.x, pt.y, pt.z, 6, 0.3, 0.1, 0.3, 0.1);
                break;
            }
        }
        return n;
    }

    /** Partículas del chorro en el cliente (agua o polvo extintor). */
    public static void particulas(Level level, LivingEntity p, boolean agua, double fuerza) {
        Vec3 d = p.getLookAngle();
        Vec3 lado = d.cross(new Vec3(0, 1, 0));
        if (lado.lengthSqr() < 1.0E-4) lado = new Vec3(1, 0, 0);
        lado = lado.normalize();
        Vec3 o = p.getEyePosition().add(d.scale(0.7)).add(lado.scale(0.25)).add(0, -0.25, 0);
        ParticleOptions part = agua ? new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WATER.defaultBlockState()) : ParticleTypes.CLOUD;
        for (int i = 0; i < (agua ? 6 : 4); i++) {
            double r = 0.06;
            level.addParticle(part, o.x, o.y, o.z, d.x * fuerza + (level.random.nextDouble() - 0.5) * r,
                    d.y * fuerza + (level.random.nextDouble() - 0.5) * r, d.z * fuerza + (level.random.nextDouble() - 0.5) * r);
        }
        if (agua && level.random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SPLASH, o.x, o.y, o.z, d.x * 0.5, 0.1, d.z * 0.5);
        }
    }
}
