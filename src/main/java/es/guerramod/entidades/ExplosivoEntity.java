package es.guerramod.entidades;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Proyectil que explota al chocar (cohetes, granadas, bombas). */
public abstract class ExplosivoEntity extends ThrowableItemProjectile {
    protected ExplosivoEntity(EntityType<? extends ExplosivoEntity> tipo, Level level) {
        super(tipo, level);
    }

    protected ExplosivoEntity(EntityType<? extends ExplosivoEntity> tipo, LivingEntity tirador, Level level) {
        super(tipo, tirador, level);
    }

    protected abstract float potencia();

    /** Si deja estela de humo. */
    protected boolean estela() {
        return false;
    }

    protected int vidaMaxima() {
        return 400;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (estela()) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
            }
        } else if (tickCount > vidaMaxima()) {
            explotar();
        }
    }

    protected void explotar() {
        if (isRemoved()) return;
        level().explode(this, getX(), getY(), getZ(), potencia(), Level.ExplosionInteraction.TNT);
        discard();
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide) explotar();
    }
}
