package es.guerramod.entidades;

import es.guerramod.GuerraMod;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModEtiquetas;
import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Bala con física realista: gravedad real (9,8 m/s²), rozamiento del aire, pérdida de daño con la distancia,
 * disparos a la cabeza (x2) y a las piernas (x0,7), atraviesa cristales y hojas, rebota en superficies duras
 * con ángulo rasante y rompe bloques blandos. Los bloques antibalas la detienen.
 */
public class BalaEntity extends ThrowableItemProjectile {
    public static final ResourceKey<DamageType> TIPO_DANO =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GuerraMod.MODID, "bala"));

    private float dano = 6.0f;
    private float alcance = 50.0f;
    private int rebotes;
    private Vec3 origen;

    public BalaEntity(EntityType<? extends BalaEntity> tipo, Level level) {
        super(tipo, level);
    }

    public BalaEntity(Level level, LivingEntity tirador, float dano, float alcance) {
        super(ModEntidades.BALA.get(), tirador, level);
        this.dano = dano;
        this.alcance = alcance;
        this.origen = tirador.getEyePosition();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROYECTIL_BALA.get();
    }

    @Override
    protected float getGravity() {
        return 0.0245f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (tickCount > 100 || !level().isLoaded(blockPosition()))) discard();
    }

    private float danoSegunDistancia() {
        if (origen == null) return dano;
        double d = position().distanceTo(origen);
        if (d <= alcance) return dano;
        double f = 1.0 - (d - alcance) / (alcance * 2.0);
        return dano * (float) Math.max(0.35, f);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (!(level() instanceof ServerLevel sl)) return;
        Entity e = r.getEntity();
        float cantidad = danoSegunDistancia();
        Vec3 impacto = r.getLocation();
        if (e instanceof LivingEntity le) {
            double y = impacto.y;
            if (y >= le.getEyeY() - 0.22) {
                cantidad *= 2.0f; // tiro a la cabeza
                sl.playSound(null, le.getX(), le.getEyeY(), le.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.3f);
            } else if (y < le.getY() + le.getBbHeight() * 0.4) {
                cantidad *= 0.7f; // piernas
            }
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    impacto.x, impacto.y, impacto.z, 10, 0.1, 0.1, 0.1, 0.15);
        }
        e.invulnerableTime = 0;
        DamageSource fuente = new DamageSource(sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TIPO_DANO),
                this, getOwner());
        e.hurt(fuente, cantidad);
        if (e instanceof LivingEntity le) {
            // las balas apenas empujan: quitamos casi todo el retroceso que da el juego
            le.setDeltaMovement(le.getDeltaMovement().multiply(0.4, 1.0, 0.4));
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        if (!(level() instanceof ServerLevel sl)) return;
        BlockPos pos = r.getBlockPos();
        BlockState st = sl.getBlockState(pos);
        Vec3 hit = r.getLocation();

        if (st.is(ModEtiquetas.A_PRUEBA_DE_BALAS)) {
            sl.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.25);
            sl.playSound(null, pos, ModSonidos.REBOTE.get(), SoundSource.BLOCKS, 0.7f, 1.0f + random.nextFloat() * 0.3f);
            discard();
            return;
        }

        // cristales, hojas y similares: los atraviesa rompiéndolos
        if (atravesable(st)) {
            if (puedeRomper(pos)) sl.destroyBlock(pos, false, getOwner());
            setDeltaMovement(getDeltaMovement().scale(0.8));
            if (getDeltaMovement().lengthSqr() < 1.0) discard();
            return;
        }

        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), hit.x, hit.y, hit.z, 8, 0.08, 0.08, 0.08, 0.12);
        sl.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 2, 0.02, 0.02, 0.02, 0.01);
        sl.playSound(null, pos, st.getSoundType(sl, pos, this).getHitSound(), SoundSource.BLOCKS, 0.6f, 1.6f);

        float dureza = st.getDestroySpeed(sl, pos);

        // rebote con ángulo rasante en superficies duras
        Vec3 v = getDeltaMovement();
        Vec3 n = Vec3.atLowerCornerOf(r.getDirection().getNormal());
        double coseno = Math.abs(v.normalize().dot(n));
        if (dureza >= 1.5f && coseno < 0.26 && rebotes < 2 && random.nextFloat() < 0.6f) {
            rebotes++;
            Vec3 reflejo = v.subtract(n.scale(2 * v.dot(n))).scale(0.55);
            setDeltaMovement(reflejo);
            setPos(hit.x + n.x * 0.05, hit.y + n.y * 0.05, hit.z + n.z * 0.05);
            dano *= 0.5f;
            sl.playSound(null, pos, ModSonidos.REBOTE.get(), SoundSource.BLOCKS, 1.0f, 0.9f + random.nextFloat() * 0.4f);
            return;
        }

        // bloques blandos: probabilidad de romperlos según potencia
        if (dureza >= 0 && puedeRomper(pos)) {
            float potencia = dano / 8.0f;
            float azar = random.nextFloat();
            if ((dureza <= 0.6f && azar < 0.45f * potencia) || (dureza <= 1.5f && azar < 0.06f * potencia)) {
                sl.destroyBlock(pos, false, getOwner());
            }
        }
        discard();
    }

    private static boolean atravesable(BlockState st) {
        return st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES) || st.is(BlockTags.LEAVES)
                || st.is(BlockTags.ICE) || st.is(BlockTags.FLOWER_POTS) || st.is(BlockTags.FLOWERS)
                || st.is(BlockTags.REPLACEABLE);
    }

    /** Respeta las fronteras: solo rompe en territorio libre, propio o enemigo en guerra. */
    private boolean puedeRomper(BlockPos pos) {
        RegistroPaises reg = RegistroPaises.de(level());
        Pais atacante = getOwner() instanceof Player p ? reg.paisDeJugador(p.getUUID()) : null;
        return reg.puedeAfectar(atacante, level(), pos);
    }

    @Override
    protected void onHit(HitResult r) {
        if (r.getType() == HitResult.Type.ENTITY) onHitEntity((EntityHitResult) r);
        else if (r.getType() == HitResult.Type.BLOCK) onHitBlock((BlockHitResult) r);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("dano", dano);
        t.putFloat("alcance", alcance);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        dano = t.getFloat("dano");
        alcance = t.getFloat("alcance");
    }
}
