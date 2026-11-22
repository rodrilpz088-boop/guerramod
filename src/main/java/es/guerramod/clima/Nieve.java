package es.guerramod.clima;

import es.guerramod.GuerraMod;
import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Nieve que se acumula de verdad: mientras nieva va cuajando capa a capa, cuanto más fuerte y más tiempo nieva más
 * gruesa se hace (sin límite: puede enterrar casas enteras), rellena primero
 * los huecos, el viento la amontona detrás de los obstáculos (ventisqueros) y en las ramas solo aguanta un poco. Con
 * calor se funde de arriba abajo (más rápido al sol y con lluvia). Andar por nieve profunda cuesta (salvo con raquetas)
 * y con esquís se va más rápido.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Nieve {
    private Nieve() {}

    /** ¿Está nevando ahora mismo en esa columna? */
    public static boolean nievaEn(ServerLevel l, BlockPos arriba, double t) {
        if (!l.isRaining() || t >= 1.0 || !l.canSeeSky(arriba)) return false;
        Biome b = l.getBiome(arriba).value();
        return b.hasPrecipitation();
    }

    /** Intensidad de la nevada (0..1). */
    public static double intensidad(ServerLevel l, double t) {
        double i = l.isThundering() ? 1.0 : 0.6;
        if (t < -15) i *= 0.7; // con mucho frío la nieve es fina y seca
        if (Estaciones.navidad(Estaciones.dia(l))) i *= 1.2;
        double v = Clima.get(l.getServer()).ventisca();
        if (v > 0) i = Math.max(i, 1.0) * (1 + 1.2 * v); // ventisca: nieva a lo bestia
        return i;
    }

    /** Grosor de nieve (en capas de 1/8 de bloque) de la columna, mirando hacia abajo desde arriba. */
    public static int espesor(Level l, BlockPos arriba) {
        int capas = 0;
        BlockState s = l.getBlockState(arriba);
        if (s.is(Blocks.SNOW)) capas += s.getValue(SnowLayerBlock.LAYERS);
        BlockPos p = arriba.below();
        for (int i = 0; i < 6 && l.getBlockState(p).is(Blocks.SNOW_BLOCK); i++) {
            capas += 8;
            p = p.below();
        }
        return capas;
    }

    /** Sin límite: mientras siga nevando la nieve sigue creciendo (metros y metros). */
    private static int maximo(Meteo m) {
        return Integer.MAX_VALUE;
    }

    /** Superficie de la columna en capas (altura*8 + capas de nieve), para comparar con los vecinos. */
    private static int superficie(ServerLevel l, int x, int z) {
        BlockPos a = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        BlockState s = l.getBlockState(a);
        return a.getY() * 8 + (s.is(Blocks.SNOW) ? s.getValue(SnowLayerBlock.LAYERS) : 0);
    }

    /** Añade una capa de nieve a la columna (si cabe). Devuelve true si ha puesto nieve. */
    public static boolean anadirCapa(ServerLevel l, BlockPos arriba, int max) {
        BlockState s = l.getBlockState(arriba);
        BlockPos debajo = arriba.below();
        BlockState sd = l.getBlockState(debajo);
        if (sd.is(BlockTags.LEAVES)) {
            // en las ramas solo aguanta un poco
            if (s.isAir()) {
                l.setBlock(arriba, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
                return true;
            }
            if (s.is(Blocks.SNOW) && s.getValue(SnowLayerBlock.LAYERS) < 2) {
                l.setBlock(arriba, s.setValue(SnowLayerBlock.LAYERS, 2), Block.UPDATE_ALL);
                return true;
            }
            return false;
        }
        if (espesor(l, arriba) >= max) return false;
        if (s.is(Blocks.SNOW)) {
            int c = s.getValue(SnowLayerBlock.LAYERS);
            if (c < 8) {
                l.setBlock(arriba, s.setValue(SnowLayerBlock.LAYERS, c + 1), Block.UPDATE_ALL);
                return true;
            }
            // capa llena: se compacta en un bloque de nieve y sigue cuajando encima
            if (l.getBlockState(arriba.above()).isAir()) {
                l.setBlock(arriba, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                l.setBlock(arriba.above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
                return true;
            }
            return false;
        }
        if ((s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty() && !s.is(Blocks.SNOW)))
                && Blocks.SNOW.defaultBlockState().canSurvive(l, arriba)) {
            l.setBlock(arriba, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Quita una capa de nieve de arriba. */
    public static boolean quitarCapa(ServerLevel l, BlockPos arriba, boolean tambienBloques) {
        BlockState s = l.getBlockState(arriba);
        if (s.is(Blocks.SNOW)) {
            int c = s.getValue(SnowLayerBlock.LAYERS);
            if (c > 1) l.setBlock(arriba, s.setValue(SnowLayerBlock.LAYERS, c - 1), Block.UPDATE_ALL);
            else l.setBlock(arriba, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        BlockPos d = arriba.below();
        BlockState sd = l.getBlockState(d);
        if (tambienBloques && sd.is(Blocks.SNOW_BLOCK)) {
            l.setBlock(d, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Una columna al azar: cuaja, se funde o se hiela. Lo llama EfectosClima muchas veces por segundo. */
    static void columna(ServerLevel l, BlockPos arriba, Meteo m, RandomSource r) {
        double t = m.temperatura();
        if (t < 1.0) {
            if (nievaEn(l, arriba, t)) {
                double prob = 0.55 * intensidad(l, t);
                // rellena primero los huecos: si los vecinos están más altos, cuaja más
                int yo = superficie(l, arriba.getX(), arriba.getZ()), suma = 0;
                for (Direction d : Direction.Plane.HORIZONTAL) suma += superficie(l, arriba.getX() + d.getStepX(), arriba.getZ() + d.getStepZ());
                double dif = suma / 4.0 - yo;
                prob *= Math.max(0.25, Math.min(2.5, 1 + 0.15 * dif));
                // ventisqueros: detrás de un obstáculo (a sotavento) se amontona; en lo alto expuesto el viento se la lleva
                Clima c = Clima.get(l.getServer());
                if (c.velocidad > 15) {
                    double ang = Math.toRadians(c.angulo);
                    int ux = arriba.getX() - (int) Math.round(Math.cos(ang)), uz = arriba.getZ() - (int) Math.round(Math.sin(ang));
                    int barlovento = superficie(l, ux, uz);
                    double v = Math.min(1.0, (c.velocidad - 15) / 40.0);
                    if (barlovento > yo + 4) prob *= 1 + 1.5 * v;
                    else if (dif < -2) prob *= 1 - 0.6 * v;
                }
                if (r.nextDouble() < prob) anadirCapa(l, arriba, maximo(m));
            }
            BlockPos debajo = arriba.below();
            if (t < -2 && l.getBlockState(debajo).is(Blocks.WATER) && l.getFluidState(debajo).isSource() && l.getBlockState(arriba).isAir()
                    && r.nextFloat() < Math.min(0.6f, (float) (-t) * 0.05f)) {
                l.setBlock(debajo, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            }
            return;
        }
        // deshielo: más calor más rápido; el sol de mediodía y la lluvia lo aceleran
        double prob = (t - 1.0) / 9.0;
        long hora = l.getDayTime() % 24000;
        if (hora > 1000 && hora < 11000 && !l.isRaining()) prob += 0.12 * (1 - m.nubes());
        if (l.isRaining() && l.canSeeSky(arriba)) prob += 0.2;
        if (r.nextDouble() > Math.min(1.0, prob)) return;
        BlockState s = l.getBlockState(arriba);
        BlockPos debajo = arriba.below();
        BlockState sd = l.getBlockState(debajo);
        if (s.is(Blocks.SNOW) || sd.is(Blocks.SNOW_BLOCK) && t > 2.5) {
            quitarCapa(l, arriba, t > 2.5);
        } else if (sd.is(Blocks.POWDER_SNOW) && t > 6) {
            l.setBlock(debajo, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 6), Block.UPDATE_ALL);
        } else if ((sd.is(Blocks.ICE) || sd.is(Blocks.FROSTED_ICE)) && t > 4) {
            l.setBlock(debajo, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        } else if (sd.is(Blocks.PACKED_ICE) && t > 14 && r.nextInt(6) == 0) {
            l.setBlock(debajo, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ====================================================================== andar por la nieve

    /** Capas de nieve en las que se hunde una entidad (0 = ninguna). */
    public static int hundido(LivingEntity e) {
        Level l = e.level();
        BlockPos p = e.blockPosition();
        BlockState s = l.getBlockState(p);
        int n = s.is(Blocks.SNOW) ? s.getValue(SnowLayerBlock.LAYERS) : 0;
        BlockState d = l.getBlockState(p.below());
        if (d.is(Blocks.SNOW_BLOCK)) n += 4; // nieve profunda: se hunde la pierna
        else if (d.is(Blocks.SNOW) && d.getValue(SnowLayerBlock.LAYERS) >= 6) n += 2;
        return n;
    }

    private static boolean lleva(LivingEntity e, String id) {
        var k = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.getItemBySlot(EquipmentSlot.FEET).getItem());
        return k != null && k.getNamespace().equals(GuerraMod.MODID) && k.getPath().equals(id);
    }

    private static void frenar(LivingEntity e) {
        if (!e.onGround() || e.isPassenger() || e.isInWater()) return;
        int n = hundido(e);
        if (n == 0) return;
        Vec3 v = e.getDeltaMovement();
        double f;
        if (lleva(e, "esquis")) f = 1.07;
        else if (lleva(e, "raquetas_nieve")) f = 1.0;
        else f = 1.0 - Math.min(0.4, 0.035 * n);
        if (f != 1.0) e.setDeltaMovement(v.x * f, v.y, v.z * f);
    }

    @SubscribeEvent
    public static void jugador(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        Player p = e.player;
        if (p.isCreative() || p.isSpectator() || p.getAbilities().flying) return;
        frenar(p);
    }

    @SubscribeEvent
    public static void criatura(LivingEvent.LivingTickEvent e) {
        LivingEntity v = e.getEntity();
        if (v instanceof Player || v.level().isClientSide() || v.tickCount % 2 != 0) return;
        frenar(v);
    }
}
