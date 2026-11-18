package es.guerramod.entidades;

import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModEtiquetas;
import es.guerramod.registro.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;

/** Bala: hace daño, rompe cristales y, a veces, bloques blandos (no los blindados). */
public class BalaEntity extends ThrowableItemProjectile {
    private float dano = 6.0f;
    private float poder = 1.0f;

    public BalaEntity(EntityType<? extends BalaEntity> tipo, Level level) {
        super(tipo, level);
    }

    public BalaEntity(Level level, LivingEntity tirador, float dano, float poder) {
        super(ModEntidades.BALA.get(), tirador, level);
        this.dano = dano;
        this.poder = poder;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROYECTIL_BALA.get();
    }

    @Override
    protected float getGravity() {
        return 0.004f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 80) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        super.onHitEntity(r);
        Entity e = r.getEntity();
        if (level().isClientSide) return;
        e.hurt(damageSources().thrown(this, getOwner()), dano);
        e.invulnerableTime = 0;
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockPos pos = r.getBlockPos();
        BlockState st = sl.getBlockState(pos);
        if (st.is(ModEtiquetas.A_PRUEBA_DE_BALAS)) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.05, 0.05, 0.05, 0.2);
            return;
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
        if (rompible(st, pos) && puedeRomper(pos)) sl.destroyBlock(pos, false, getOwner());
    }

    private boolean rompible(BlockState st, BlockPos pos) {
        if (st.isAir()) return false;
        if (st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES) || st.is(BlockTags.LEAVES)
                || st.is(BlockTags.ICE) || st.is(BlockTags.FLOWER_POTS) || st.is(BlockTags.FLOWERS)) return true;
        float dureza = st.getDestroySpeed(level(), pos);
        if (dureza < 0) return false;
        float azar = random.nextFloat();
        if (dureza <= 0.8f) return azar < 0.30f * poder;
        if (dureza <= 2.0f) return azar < 0.05f * poder;
        return false;
    }

    /** Respeta las fronteras: solo rompe en territorio libre, propio o enemigo en guerra. */
    private boolean puedeRomper(BlockPos pos) {
        RegistroPaises reg = RegistroPaises.de(level());
        Pais atacante = getOwner() instanceof Player p ? reg.paisDeJugador(p.getUUID()) : null;
        return reg.puedeAfectar(atacante, level(), pos);
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("dano", dano);
        t.putFloat("poder", poder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        dano = t.getFloat("dano");
        poder = t.getFloat("poder");
    }
}
