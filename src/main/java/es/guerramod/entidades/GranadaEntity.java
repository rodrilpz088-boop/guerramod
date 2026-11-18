package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Granada de fragmentación: rebota, rueda y explota a los 4 segundos de lanzarla. */
public class GranadaEntity extends ExplosivoEntity {
    private static final int ESPOLETA = 80;
    private boolean parada;

    public GranadaEntity(EntityType<? extends GranadaEntity> tipo, Level level) {
        super(tipo, level);
    }

    public GranadaEntity(Level level, LivingEntity tirador) {
        super(ModEntidades.GRANADA.get(), tirador, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRANADA.get();
    }

    @Override
    protected float getGravity() {
        return parada ? 0.0f : 0.05f;
    }

    @Override
    protected float potencia() {
        return 3.0f;
    }

    @Override
    protected int vidaMaxima() {
        return ESPOLETA;
    }

    @Override
    public void tick() {
        if (parada && level().getBlockState(blockPosition().below()).isAir() && level().getBlockState(blockPosition()).isAir()) {
            parada = false; // si quitan el suelo, vuelve a caer
        }
        super.tick();
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.01, 0);
        }
    }

    @Override
    protected void onHit(HitResult r) {
        if (level().isClientSide) return;
        if (r.getType() == HitResult.Type.BLOCK) rebotar((BlockHitResult) r);
        else if (r.getType() == HitResult.Type.ENTITY) {
            EntityHitResult er = (EntityHitResult) r;
            er.getEntity().hurt(damageSources().thrown(this, getOwner()), 1.0f);
            setDeltaMovement(getDeltaMovement().scale(-0.2));
        }
    }

    private void rebotar(BlockHitResult r) {
        Vec3 v = getDeltaMovement();
        Vec3 n = Vec3.atLowerCornerOf(r.getDirection().getNormal());
        // reflejo con pérdida de energía (restitución 0,35) y rozamiento tangencial
        Vec3 normal = n.scale(v.dot(n));
        Vec3 tangente = v.subtract(normal);
        Vec3 nueva = tangente.scale(0.6).subtract(normal.scale(0.35));
        Vec3 hit = r.getLocation();
        setPos(hit.x + n.x * 0.12, hit.y + n.y * 0.12, hit.z + n.z * 0.12);
        if (nueva.lengthSqr() < 0.0025 && n.y > 0.5) {
            setDeltaMovement(Vec3.ZERO);
            parada = true;
        } else {
            setDeltaMovement(nueva);
            if (v.lengthSqr() > 0.01) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.6f, 1.5f);
            }
        }
    }
}
