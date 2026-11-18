package es.guerramod.entidades;

import es.guerramod.radiacion.Nuclear;
import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Misil balístico guiado: sube en vertical y luego cae sobre el objetivo. Puede ser nuclear. */
public class MisilEntity extends ThrowableItemProjectile {
    @Nullable
    private BlockPos objetivo;
    private boolean nuclear;
    private int fase;
    private double alturaCrucero;

    public MisilEntity(EntityType<? extends MisilEntity> tipo, Level level) {
        super(tipo, level);
    }

    public MisilEntity(Level level, LivingEntity tirador, BlockPos objetivo, boolean nuclear) {
        super(ModEntidades.MISIL.get(), tirador, level);
        this.objetivo = objetivo;
        this.nuclear = nuclear;
        this.alturaCrucero = Math.min(level.getMaxBuildHeight() - 8, Math.max(tirador.getY(), objetivo.getY()) + 70);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MISIL_BALISTICO.get();
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (objetivo == null) {
                explotar();
                return;
            }
            if (fase == 0) {
                setDeltaMovement(0, 1.2, 0);
                if (getY() >= alturaCrucero || tickCount > 160) fase = 1;
            } else {
                Vec3 dir = Vec3.atCenterOf(objetivo).subtract(position());
                double dist = dir.length();
                if (dist < 1.5) {
                    explotar();
                    return;
                }
                setDeltaMovement(dir.normalize().scale(Math.min(dist, 2.5)));
            }
            if (tickCount > 1600) {
                explotar();
                return;
            }
        } else {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() - 0.5, getZ(), 0, 0.01, 0);
            level().addParticle(ParticleTypes.FLAME, getX(), getY() - 0.4, getZ(), 0, -0.1, 0);
        }
        super.tick();
    }

    private void explotar() {
        if (isRemoved() || !(level() instanceof ServerLevel sl)) return;
        if (nuclear) {
            Nuclear.detonar(sl, position(), getOwner());
        } else {
            sl.explode(this, getX(), getY(), getZ(), 6.0f, Level.ExplosionInteraction.TNT);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide && tickCount > 2) explotar();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (objetivo != null) t.putLong("objetivo", objetivo.asLong());
        t.putBoolean("nuclear", nuclear);
        t.putInt("fase", fase);
        t.putDouble("altura", alturaCrucero);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        objetivo = t.contains("objetivo") ? BlockPos.of(t.getLong("objetivo")) : null;
        nuclear = t.getBoolean("nuclear");
        fase = t.getInt("fase");
        alturaCrucero = t.getDouble("altura");
    }
}
