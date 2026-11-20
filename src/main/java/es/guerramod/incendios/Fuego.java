package es.guerramod.incendios;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * El "motor" del fuego: qué arde, con qué facilidad se propaga (combustible, viento, pendiente, sequía, lluvia),
 * cómo queda el terreno al quemarse y cómo se apaga.
 */
public final class Fuego {
    private Fuego() {}

    // ================================================================== suelo mojado (agua reciente)

    private static final Map<ResourceKey<Level>, Long2LongOpenHashMap> MOJADO = new HashMap<>();

    public static void mojar(Level level, BlockPos centro, int radio, int ticks) {
        Long2LongOpenHashMap m = MOJADO.computeIfAbsent(level.dimension(), k -> new Long2LongOpenHashMap());
        long hasta = level.getGameTime() + ticks;
        for (int x = -radio; x <= radio; x++)
            for (int z = -radio; z <= radio; z++)
                for (int y = -2; y <= 2; y++) {
                    if (x * x + z * z > radio * radio + 1) continue;
                    m.put(BlockPos.asLong(centro.getX() + x, centro.getY() + y, centro.getZ() + z), hasta);
                }
        if (m.size() > 200_000) m.clear();
    }

    public static boolean mojado(Level level, BlockPos p) {
        Long2LongOpenHashMap m = MOJADO.get(level.dimension());
        if (m == null) return false;
        long hasta = m.get(p.asLong());
        if (hasta == 0) return false;
        if (hasta < level.getGameTime()) {
            m.remove(p.asLong());
            return false;
        }
        return true;
    }

    public static boolean llueve(Level level, BlockPos p) {
        return level.isRainingAt(p) || level.isRainingAt(p.above());
    }

    // ================================================================== combustible

    public static boolean esPasto(BlockState s) {
        return s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.DEAD_BUSH) || s.is(BlockTags.FLOWERS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.CROPS)
                || s.is(BloquesIncendio.PASTO_SECO.get()) || s.is(BlockTags.SAPLINGS);
    }

    /** Suelo que arde (la hierba de encima, la paja...). */
    public static double sueloQuemable(BlockState abajo) {
        // el césped arde muy rápido: es el combustible fino que lleva el fuego forestal
        if (abajo.is(Blocks.GRASS_BLOCK)) return 0.95;
        if (abajo.is(Blocks.PODZOL) || abajo.is(Blocks.MYCELIUM)) return 0.8;
        if (abajo.is(Blocks.MOSS_BLOCK) || abajo.is(Blocks.MOSS_CARPET)) return 0.85;
        if (abajo.is(Blocks.HAY_BLOCK)) return 0.95;
        if (abajo.is(BlockTags.LEAVES)) return 0.6;
        if (abajo.is(Blocks.FARMLAND)) return 0.15;
        return 0.0;
    }

    public static boolean cabeLlama(BlockState s) {
        if (s.isAir()) return true;
        if (!s.getFluidState().isEmpty() || s.getBlock() instanceof es.guerramod.clima.CharcoBlock) return false;
        return esPasto(s) || (s.canBeReplaced() && !(s.getBlock() instanceof LlamaBlock));
    }

    /** Cuánto combustible hay para una llama en esta posición (0 = nada, 1 = mucho). */
    public static double combustible(Level level, BlockPos pos) {
        BlockState aqui = level.getBlockState(pos);
        double f = sueloQuemable(level.getBlockState(pos.below()));
        if (aqui.is(BloquesIncendio.PASTO_SECO.get())) f += 0.75;
        else if (aqui.is(Blocks.DEAD_BUSH)) f += 0.6;
        else if (esPasto(aqui)) f += 0.45;
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            BlockPos np = pos.relative(d);
            BlockState n = level.getBlockState(np);
            if (n.isAir() || n.getBlock() instanceof LlamaBlock) continue;
            int inflam = n.getFlammability(level, np, d.getOpposite());
            if (inflam > 0) f += inflam / 260.0;
        }
        return Math.min(1.6, f);
    }

    /** Se puede quemar ahí ahora mismo (sin lluvia ni agua reciente). */
    public static boolean seco(ServerLevel level, BlockPos p) {
        return !llueve(level, p) && !mojado(level, p) && !mojado(level, p.below());
    }

    // ================================================================== prender (sin probabilidad: ya decidido)

    /** Pone a arder lo que haya en pos si puede arder. Devuelve true si empezó a arder. */
    public static boolean encender(ServerLevel level, BlockPos pos, boolean grande) {
        if (!level.isLoaded(pos)) return false;
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof LlamaBlock || st.getBlock() instanceof TroncoArdiendoBlock) return false;
        if (st.is(BlockTags.LEAVES)) {
            level.setBlock(pos, BloquesIncendio.LLAMA.get().defaultBlockState().setValue(LlamaBlock.COPA, true)
                    .setValue(LlamaBlock.GRANDE, true), Block.UPDATE_ALL);
            return true;
        }
        if (st.is(BlockTags.LOGS_THAT_BURN)) {
            BlockState t = BloquesIncendio.TRONCO_ARDIENDO.get().defaultBlockState();
            if (st.hasProperty(RotatedPillarBlock.AXIS)) t = t.setValue(RotatedPillarBlock.AXIS, st.getValue(RotatedPillarBlock.AXIS));
            level.setBlock(pos, t, Block.UPDATE_ALL);
            return true;
        }
        if (!cabeLlama(st) || combustible(level, pos) < 0.05) return false;
        BlockState abajo = level.getBlockState(pos.below());
        if (st.is(Blocks.SNOW) || abajo.is(Blocks.SNOW_BLOCK) || abajo.is(Blocks.POWDER_SNOW)) return false;
        level.setBlock(pos, BloquesIncendio.LLAMA.get().defaultBlockState().setValue(LlamaBlock.GRANDE, grande), Block.UPDATE_ALL);
        return true;
    }

    /** Lo compatible con versiones anteriores: prende con una probabilidad dada si está seco. */
    public static boolean prender(ServerLevel level, BlockPos pos, RandomSource r, double prob, BlockPos desde) {
        return seco(level, pos) && r.nextDouble() < prob && encender(level, pos, false);
    }

    /** Factor de combustible para la velocidad: césped corto 0,7, con hierba 1, pasto seco 1,15. */
    private static double factorCombustible(double comb) {
        return Math.min(1.25, comb / 1.4);
    }

    /** Efecto de la pendiente (McArthur: dobla cada 10° cuesta arriba). */
    private static double pendiente(int dy, double distH) {
        if (dy == 0) return 1.0;
        double grados = Math.toDegrees(Math.atan2(Math.abs(dy), distH));
        return dy > 0 ? Math.min(4.0, Math.exp(0.069 * grados)) : Math.max(0.5, Math.exp(-0.069 * grados * 0.5));
    }

    /** Probabilidad de que el fuego recorra "dist" metros a "ros" m/s en dt segundos (proceso de Poisson). */
    private static boolean llega(RandomSource r, double ros, double dist, double dt) {
        if (ros <= 0) return false;
        return r.nextDouble() < 1 - Math.exp(-ros * dt / dist);
    }

    /**
     * Propagación física desde un foco durante dt segundos. Calcula la velocidad del fuego hacia cada vecino
     * (elipse de viento, pendiente, tipo de combustible) y lo prende con la probabilidad correspondiente.
     * Devuelve la velocidad máxima de cabeza (m/s) para que la llama sepa cuánto debe durar.
     *
     * @param origenCopa true si el foco está en las hojas (fuego de copas)
     * @param potencia   1 = llama normal, más para troncos o llamas grandes
     */
    public static double extender(ServerLevel level, BlockPos origen, RandomSource r, double dt, boolean origenCopa, double potencia) {
        Meteo m = Meteo.en(level, origen);
        Clima c = Clima.get(level.getServer());
        double vx = c.vx(), vz = c.vz();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                double distH = Math.sqrt(dx * dx + dz * dz);
                double cos = (dx * vx + dz * vz) / distH;
                BlockPos t = origen.offset(dx, 0, dz);
                BlockState st = level.getBlockState(t);
                int dy = 0;
                // escalón: si al lado hay suelo, el césped está encima; si hay un hueco, está debajo
                if (!cabeLlama(st) && !st.is(BlockTags.LEAVES) && !st.is(BlockTags.LOGS_THAT_BURN)) {
                    t = t.above();
                    st = level.getBlockState(t);
                    dy = 1;
                } else if (cabeLlama(st) && st.isAir() && sueloQuemable(level.getBlockState(t.below())) <= 0
                        && cabeLlama(level.getBlockState(t.below()))) {
                    t = t.below();
                    st = level.getBlockState(t);
                    dy = -1;
                }
                intentar(level, t, st, r, m, cos, distH, dy, dt, origenCopa, potencia);
                // copas de árbol a la altura de las llamas o justo encima
                BlockPos arriba = origen.offset(dx, 1, dz);
                BlockState sa = level.getBlockState(arriba);
                if (sa.is(BlockTags.LEAVES) && seco(level, arriba) && llega(r, m.rosCopa(cos) * 0.6 * potencia, 1.4, dt)) {
                    encender(level, arriba, true);
                }
            }
        }
        // hacia arriba: hojas o tronco justo encima de la llama (escalera de combustible)
        BlockPos up = origen.above();
        BlockState su = level.getBlockState(up);
        if (su.is(BlockTags.LEAVES) && seco(level, up) && llega(r, m.rosCopa(1) * 0.8 * potencia, 1, dt)) encender(level, up, true);
        else if (su.is(BlockTags.LOGS_THAT_BURN) && seco(level, up) && llega(r, 0.03 * m.factorHumedad() * potencia, 1, dt)) encender(level, up, true);
        return m.rosCabeza();
    }

    private static void intentar(ServerLevel level, BlockPos t, BlockState st, RandomSource r, Meteo m, double cos, double distH,
                                 int dy, double dt, boolean origenCopa, double potencia) {
        if (!level.isLoaded(t) || !seco(level, t)) return;
        double dist = Math.sqrt(distH * distH + dy * dy);
        if (st.is(BlockTags.LEAVES)) {
            if (llega(r, m.rosCopa(cos) * potencia, dist, dt)) encender(level, t, true);
            return;
        }
        if (st.is(BlockTags.LOGS_THAT_BURN)) {
            // los troncos tardan en prender (combustible grueso)
            if (llega(r, 0.025 * m.factorHumedad() * potencia * (origenCopa ? 1.5 : 1), dist, dt)) encender(level, t, true);
            return;
        }
        if (!cabeLlama(st)) return;
        double comb = combustible(level, t);
        if (comb < 0.05) return;
        double ros = m.rosDireccion(cos) * factorCombustible(comb) * pendiente(dy, distH) * potencia;
        // desde una copa cae material ardiendo al suelo
        if (origenCopa) ros = Math.max(ros, 0.02 * m.factorHumedad());
        if (llega(r, ros, dist, dt)) encender(level, t, m.alturaLlama(ros) > 1.6);
    }

    /** Compatibilidad: propagación durante 1 segundo. */
    public static void propagar(ServerLevel level, BlockPos origen, RandomSource r, double intensidad, int intentos) {
        extender(level, origen, r, 1.0, false, intensidad);
    }

    /** Pavesas: brasas que el viento lleva lejos y pueden empezar focos nuevos. */
    public static void pavesa(ServerLevel level, BlockPos origen, RandomSource r) {
        Clima c = Clima.get(level.getServer());
        if (c.velocidad < 15) return;
        double dist = 4 + r.nextDouble() * (6 + c.velocidad * 0.35);
        double ang = Math.toRadians(c.angulo + r.nextGaussian() * 18);
        int x = origen.getX() + (int) Math.round(Math.cos(ang) * dist);
        int z = origen.getZ() + (int) Math.round(Math.sin(ang) * dist);
        if (!level.isLoaded(new BlockPos(x, origen.getY(), z))) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos destino = new BlockPos(x, y, z);
        // la pavesa vuela (partículas a lo largo del camino)
        Vec3 a = Vec3.atCenterOf(origen).add(0, 1.5, 0), b = Vec3.atCenterOf(destino);
        for (int i = 0; i <= 6; i++) {
            Vec3 p = a.lerp(b, i / 6.0).add(0, Math.sin(Math.PI * i / 6.0) * 3, 0);
            level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
        }
        Meteo m = Meteo.en(level, destino);
        double prob = 0.7 * m.factorHumedad() * m.factorCurado();
        prender(level, destino, r, prob, origen);
        // en copa de árbol
        BlockState arriba = level.getBlockState(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, destino).below());
        if (arriba.is(BlockTags.LEAVES)) prender(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, destino).below(), r, prob * 0.8, origen);
    }

    // ================================================================== consumir (cuando la llama termina)

    public static void quemarSuelo(ServerLevel level, BlockPos llama) {
        BlockPos b = llama.below();
        BlockState s = level.getBlockState(b);
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.FARMLAND) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.ROOTED_DIRT)) {
            level.setBlock(b, BloquesIncendio.TIERRA_QUEMADA.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (s.is(Blocks.HAY_BLOCK) || s.is(Blocks.MOSS_CARPET)) {
            level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Quema lo de alrededor de una llama que se apaga sola (madera, lana...). */
    public static void carbonizarVecinos(ServerLevel level, BlockPos pos, RandomSource r) {
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            BlockState n = level.getBlockState(np);
            if (n.isAir() || n.is(BlockTags.LEAVES) || n.is(BlockTags.LOGS) || n.getBlock() instanceof LlamaBlock) continue;
            int inflam = n.getFlammability(level, np, d.getOpposite());
            if (inflam <= 0 || r.nextFloat() > 0.35f) continue;
            if (n.is(BlockTags.PLANKS) || n.is(BlockTags.WOODEN_STAIRS) || n.is(BlockTags.WOODEN_SLABS)) {
                level.setBlock(np, r.nextFloat() < 0.7f ? BloquesIncendio.MADERA_QUEMADA.get().defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (n.isCollisionShapeFullBlock(level, np) && inflam < 30) {
                level.setBlock(np, BloquesIncendio.MADERA_QUEMADA.get().defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(np, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // ================================================================== apagar

    public static boolean esFuego(BlockState s) {
        return s.getBlock() instanceof LlamaBlock || s.getBlock() instanceof TroncoArdiendoBlock || s.getBlock() instanceof RescoldoBlock
                || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT));
    }

    /** Apaga un bloque. Devuelve true si había fuego. */
    public static boolean apagar(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof LlamaBlock) {
            if (!s.getValue(LlamaBlock.COPA) && s.getValue(LlamaBlock.EDAD) >= 3) quemarSuelo(level, p);
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else if (s.getBlock() instanceof TroncoArdiendoBlock) {
            level.setBlock(p, BloquesIncendio.TRONCO_QUEMADO.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS)), Block.UPDATE_ALL);
        } else if (s.getBlock() instanceof RescoldoBlock) {
            level.setBlock(p, BloquesIncendio.CENIZA.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) {
            CampfireBlock.dowse(null, level, p, s);
            level.setBlock(p, s.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
        } else {
            return false;
        }
        level.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0.02);
        return true;
    }

    /** Apaga todo el fuego en una esfera, apaga a los que arden y deja el suelo mojado un rato. */
    public static int apagarZona(ServerLevel level, Vec3 centro, double radio, int ticksMojado) {
        int n = 0;
        int ri = (int) Math.ceil(radio);
        BlockPos c = BlockPos.containing(centro);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-ri, -ri, -ri), c.offset(ri, ri, ri))) {
            if (p.distToCenterSqr(centro) > radio * radio) continue;
            if (apagar(level, p)) n++;
        }
        for (Entity e : level.getEntities((Entity) null, new AABB(centro, centro).inflate(radio), Entity::isOnFire)) e.clearFire();
        if (ticksMojado > 0) mojar(level, c, Math.max(1, ri), ticksMojado);
        if (n > 0 && level.random.nextInt(3) == 0) {
            level.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f + level.random.nextFloat() * 0.4f);
        }
        return n;
    }

    // ================================================================== humo visible desde lejos

    public static void columnaHumo(ServerLevel level, BlockPos p, RandomSource r, boolean grande) {
        Clima c = Clima.get(level.getServer());
        double vx = c.vx() * c.fuerza() * 0.06, vz = c.vz() * c.fuerza() * 0.06;
        for (ServerPlayer jugador : level.players()) {
            if (jugador.blockPosition().distSqr(p) > 200 * 200) continue;
            level.sendParticles(jugador, grande ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    p.getX() + 0.5 + r.nextGaussian() * 0.3, p.getY() + 0.8, p.getZ() + 0.5 + r.nextGaussian() * 0.3,
                    0, vx, 0.07, vz, 1.0);
        }
    }

    // ================================================================== fuego normal de Minecraft -> fuego realista

    private static final List<Object[]> COLA = new ArrayList<>();

    public static void encolarConversion(Level level, BlockPos pos) {
        synchronized (COLA) {
            if (COLA.size() < 5000) COLA.add(new Object[]{level.dimension(), pos.immutable()});
        }
    }

    @SuppressWarnings("unchecked")
    public static void procesarCola(net.minecraft.server.MinecraftServer server) {
        List<Object[]> copia;
        synchronized (COLA) {
            if (COLA.isEmpty()) return;
            copia = new ArrayList<>(COLA);
            COLA.clear();
        }
        for (Object[] o : copia) {
            ServerLevel level = server.getLevel((ResourceKey<Level>) o[0]);
            BlockPos p = (BlockPos) o[1];
            if (level == null || !level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.FIRE)) continue;
            BlockState abajo = level.getBlockState(p.below());
            boolean natural = sueloQuemable(abajo) > 0 || abajo.is(Blocks.DIRT) || abajo.is(Blocks.COARSE_DIRT);
            boolean arbol = false;
            for (Direction d : Direction.values()) {
                BlockState n = level.getBlockState(p.relative(d));
                if (n.is(BlockTags.LEAVES) || n.is(BlockTags.LOGS_THAT_BURN)) arbol = true;
            }
            if (natural || arbol) level.setBlock(p, BloquesIncendio.LLAMA.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
