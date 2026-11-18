package es.guerramod.radiacion;

import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Explosión nuclear: cráter, onda expansiva, fuego, hongo de humo y zona radiactiva. */
public final class Nuclear {
    public static final int RADIO_CRATER = 16;
    public static final int RADIO_DANO = 48;
    public static final int RADIO_RADIACION = 90;
    public static final int DURACION_RADIACION = 20 * 60 * 10; // 10 minutos

    private Nuclear() {}

    public static void detonar(ServerLevel level, Vec3 c, @Nullable Entity autor) {
        RegistroPaises reg = RegistroPaises.de(level);
        Pais atacante = autor instanceof Player p ? reg.paisDeJugador(p.getUUID()) : null;
        RandomSource rnd = level.random;
        BlockPos centro = BlockPos.containing(c);

        // 1) Cráter. Los bloques con resistencia >= 600 (hormigón reforzado, puerta de búnker...) aguantan.
        int r = RADIO_CRATER;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r - 6; x <= r + 6; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r - 6; z <= r + 6; z++) {
                    double d = Math.sqrt(x * x + y * y * 1.6 + z * z) + rnd.nextDouble() * 2.0;
                    if (d > r + 6) continue;
                    m.set(centro.getX() + x, centro.getY() + y, centro.getZ() + z);
                    if (!level.isLoaded(m)) continue;
                    BlockState st = level.getBlockState(m);
                    if (st.isAir()) continue;
                    if (st.getDestroySpeed(level, m) < 0) continue;
                    if (st.getBlock().getExplosionResistance() >= 600) continue;
                    if (!reg.puedeAfectar(atacante, level, m)) continue;
                    if (d <= r) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        quemar(level, m, st, rnd);
                    }
                }
            }
        }

        // 2) Onda expansiva sobre las entidades
        AABB zona = new AABB(c, c).inflate(RADIO_DANO);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, zona)) {
            double d = e.position().distanceTo(c);
            if (d > RADIO_DANO) continue;
            float dano = (float) (120.0 * (1.0 - d / RADIO_DANO)) + 4.0f;
            if (DatosRadiacion.enBunker(level, e.blockPosition())) dano *= 0.1f;
            e.hurt(level.damageSources().explosion(null, autor), dano);
            e.setSecondsOnFire(8);
            Vec3 empuje = e.position().subtract(c).normalize().scale(2.5 * (1.0 - d / RADIO_DANO));
            e.push(empuje.x, 0.5 + empuje.y * 0.3, empuje.z);
        }

        // 3) Efectos: sonido, destello, hongo nuclear
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 32.0f, 0.4f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 32.0f, 0.5f);
        level.explode(null, c.x, c.y, c.z, 8.0f, Level.ExplosionInteraction.NONE);
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(c);
            if (d > 400) continue;
            if (d < 150 && p.level().canSeeSky(p.blockPosition())) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, c.x, c.y, c.z, 20, 8, 4, 8, 0);
            for (int i = 0; i < 45; i++) {
                double alto = c.y + i;
                double ancho = i < 35 ? 2.0 + i * 0.05 : 6.0 + (i - 35) * 1.2;
                level.sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, c.x, alto, c.z,
                        i < 35 ? 6 : 30, ancho, 0.6, ancho, 0.01);
            }
            level.sendParticles(p, ParticleTypes.LAVA, true, c.x, c.y + 2, c.z, 80, 10, 4, 10, 0.5);
        }

        // 4) Zona radiactiva
        DatosRadiacion.get(level.getServer()).anadir(level, c, RADIO_RADIACION, DURACION_RADIACION);
        level.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("☢ Detonación nuclear en " + centro.getX() + ", " + centro.getZ()
                        + ". Zona radiactiva de " + RADIO_RADIACION + " bloques durante 10 minutos.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GREEN), false);
    }

    /** Borde del cráter: árboles fuera, tierra quemada y fuego. */
    private static void quemar(ServerLevel level, BlockPos.MutableBlockPos m, BlockState st, RandomSource rnd) {
        if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || st.is(BlockTags.PLANKS) || st.is(BlockTags.FLOWERS)
                || st.is(BlockTags.REPLACEABLE) || !st.getFluidState().isEmpty()) {
            level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (st.is(BlockTags.DIRT) || st.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(m, rnd.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            BlockPos arriba = m.above();
            if (rnd.nextInt(6) == 0 && level.getBlockState(arriba).isAir()) {
                level.setBlock(arriba, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (rnd.nextInt(3) == 0 && st.getBlock().getExplosionResistance() < 10) {
            level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
