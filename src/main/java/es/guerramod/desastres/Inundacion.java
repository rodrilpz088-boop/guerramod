package es.guerramod.desastres;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/**
 * Inundaciones realistas. Con mucha lluvia el nivel de ríos y lagos sube poco a poco (más deprisa con tormenta) y el agua
 * se extiende por todo el terreno más bajo que el nuevo nivel que esté conectado con el cauce: vegas, campos, caminos,
 * calles y el interior de las casas con la puerta abierta. Las lomas, los muros y los diques la frenan. El frente avanza
 * columna a columna desde el río hacia fuera, así que se ve llegar el agua. Arrastra lo que flota (objetos, animales,
 * jugadores) hacia las zonas más bajas, se lleva cultivos, flores y antorchas, y cuando deja de llover el nivel baja
 * despacio y deja barro.
 */
public final class Inundacion extends SavedData {
    private static final int MAX_BLOQUES = 60000, MAX_COLUMNAS = 40000;
    private static final int TICKS_SUBIDA = 2400, TICKS_BAJADA = 3000;

    // guardado
    final LongArrayList agua = new LongArrayList();
    final IntArrayList base = new IntArrayList();
    int nivel = 0, lluvia = 0, ticksNivel = 0;

    // en memoria
    private final LongOpenHashSet aguaSet = new LongOpenHashSet();
    private final Long2IntOpenHashMap baseColumna = new Long2IntOpenHashMap();
    private final ArrayDeque<long[]> frente = new ArrayDeque<>();
    private final LongOpenHashSet visto = new LongOpenHashSet();
    private final ArrayDeque<Long> retirar = new ArrayDeque<>();
    private boolean migrado = false;
    private int ultimosCauces = 0;

    public static Inundacion get(ServerLevel l) {
        return l.getDataStorage().computeIfAbsent(Inundacion::cargar, Inundacion::new, "guerramod_inundacion");
    }

    static Inundacion cargar(CompoundTag t) {
        Inundacion f = new Inundacion();
        long[] a = t.getLongArray("agua");
        int[] b = t.getIntArray("base");
        for (int i = 0; i < a.length && i < b.length; i++) f.registrar(a[i], b[i]);
        f.nivel = t.getInt("nivel");
        f.lluvia = t.getInt("lluvia");
        f.ticksNivel = t.getInt("ticksNivel");
        return f;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.put("agua", new LongArrayTag(agua.toLongArray()));
        t.put("base", new IntArrayTag(base.toIntArray()));
        t.putInt("nivel", nivel);
        t.putInt("lluvia", lluvia);
        t.putInt("ticksNivel", ticksNivel);
        return t;
    }

    private static long col(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private void registrar(long pos, int y0) {
        if (!aguaSet.add(pos)) return;
        agua.add(pos);
        base.add(y0);
        BlockPos p = BlockPos.of(pos);
        baseColumna.put(col(p.getX(), p.getZ()), y0);
    }

    // ------------------------------------------------------------------ control
    /** Provoca una inundación ya: devuelve cuántos puntos de río o lago ha encontrado cerca de los jugadores. */
    public static int provocar(ServerLevel l) {
        Inundacion f = get(l);
        f.lluvia = Math.max(f.lluvia, 9000);
        l.setWeatherParameters(0, 18000, true, true);
        f.nivel = Math.max(f.nivel, 2);
        f.subir(l);
        f.setDirty();
        return f.ultimosCauces;
    }

    public static void parar(ServerLevel l) {
        Inundacion f = get(l);
        f.lluvia = 0;
        f.ticksNivel = 0;
        f.nivel = 0;
        f.frente.clear();
        f.visto.clear();
        f.prepararRetirada();
        f.setDirty();
    }

    public static boolean activa(ServerLevel l) {
        return get(l).nivel > 0;
    }

    // ------------------------------------------------------------------ cada tick
    public static void tick(ServerLevel l) {
        Inundacion f = get(l);
        if (!f.migrado) {
            f.migrar(l);
            f.migrado = true;
        }
        RandomSource r = l.random;
        boolean llueve = l.isRaining(), tormenta = l.isThundering();
        if (llueve) f.lluvia = Math.min(30000, f.lluvia + (tormenta ? 3 : 1));
        else f.lluvia = Math.max(0, f.lluvia - 2);
        int maximo = tormenta ? 7 : 4;
        if (llueve && f.lluvia > 7000) {
            f.ticksNivel += tormenta ? 2 : 1;
            if (f.ticksNivel >= TICKS_SUBIDA && f.nivel < maximo) {
                f.ticksNivel = 0;
                f.subir(l);
            }
        } else if (!llueve && f.nivel > 0) {
            if (++f.ticksNivel >= TICKS_BAJADA) {
                f.ticksNivel = 0;
                f.bajar(l);
            }
        }
        if (f.nivel > 0) {
            if (l.getGameTime() % 200 == 0) f.sembrar(l, r);
            f.expandir(l, 160);
            if (l.getGameTime() % 2 == 0) f.corriente(l);
        }
        if (!f.retirar.isEmpty()) f.quitar(l, r, 160);
        if (l.getGameTime() % 400 == 0) f.setDirty();
    }

    /** El agua puesta por la versión anterior de las riadas se retira. */
    private void migrar(ServerLevel l) {
        Desastres.Riada vieja = Desastres.Riada.get(l.getServer());
        if (vieja.agua.isEmpty()) return;
        for (long p : vieja.agua) retirar.add(p);
        vieja.agua.clear();
        vieja.setDirty();
    }

    private void subir(ServerLevel l) {
        nivel++;
        if (nivel == 1) Desastres.alerta(l, BlockPos.ZERO, 0, "Aviso por inundaciones", "los ríos y lagos empiezan a desbordarse");
        else if (nivel == 3) Desastres.alerta(l, BlockPos.ZERO, 0, "Inundación grave", "el agua sigue subiendo e inunda las zonas bajas");
        else if (nivel == 6) Desastres.alerta(l, BlockPos.ZERO, 0, "Inundación extrema", "evacúa las zonas bajas y sube a lo alto");
        // el agua vuelve a avanzar desde el cauce con el nuevo nivel
        visto.clear();
        frente.clear();
        sembrar(l, l.random);
        setDirty();
    }

    private void bajar(ServerLevel l) {
        nivel--;
        frente.clear();
        visto.clear();
        prepararRetirada();
        if (nivel == 0) Desastres.alerta(l, BlockPos.ZERO, 0, "Fin de la inundación", "el agua vuelve a su cauce y deja barro");
        setDirty();
    }

    /** Apunta para quitar (de arriba abajo) el agua que queda por encima del nivel actual. */
    private void prepararRetirada() {
        LongArrayList quedan = new LongArrayList();
        IntArrayList basesQuedan = new IntArrayList();
        java.util.ArrayList<Long> fuera = new java.util.ArrayList<>();
        for (int i = 0; i < agua.size(); i++) {
            long p = agua.getLong(i);
            int y = BlockPos.getY(p);
            if (y > base.getInt(i) + nivel) fuera.add(p);
            else {
                quedan.add(p);
                basesQuedan.add(base.getInt(i));
            }
        }
        fuera.sort((a, b) -> Integer.compare(BlockPos.getY(b), BlockPos.getY(a)));
        retirar.addAll(fuera);
        agua.clear();
        base.clear();
        aguaSet.clear();
        for (int i = 0; i < quedan.size(); i++) registrar(quedan.getLong(i), basesQuedan.getInt(i));
        if (nivel <= 0) baseColumna.clear();
    }

    /**
     * Puntos de partida: el agua de ríos, lagos y pantanos cerca de los jugadores (barrido cada 4 bloques hasta 128 de
     * distancia) y lo ya inundado. Donde no hay cauce, la lluvia se acumula en los hoyos y zonas bajas.
     */
    private void sembrar(ServerLevel l, RandomSource r) {
        int cauces = 0;
        for (ServerPlayer p : l.players()) {
            int px = p.getBlockX(), pz = p.getBlockZ();
            int off = r.nextInt(4);
            int hoyos = 0;
            for (int dx = -128 + off; dx <= 128; dx += 4) {
                for (int dz = -128 + off; dz <= 128; dz += 4) {
                    if (dx * dx + dz * dz > 128 * 128) continue;
                    int x = px + dx, z = pz + dz;
                    long c = col(x, z);
                    if (visto.contains(c) || !l.hasChunkAt(new BlockPos(x, 64, z))) continue;
                    if (baseColumna.containsKey(c)) {
                        frente.add(new long[]{c, baseColumna.get(c), 99});
                        continue;
                    }
                    int ws = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    BlockPos w = new BlockPos(x, ws, z);
                    if (l.getBlockState(w).is(Blocks.WATER) && l.getFluidState(w).isSource()) {
                        if (l.getBiome(w).is(BiomeTags.IS_OCEAN) || l.getBiome(w).is(BiomeTags.IS_DEEP_OCEAN)) continue;
                        frente.add(new long[]{c, ws, 99});
                        cauces++;
                    } else if (nivel >= 2 && hoyos < 40) {
                        // hoyo: casi todo alrededor está más alto -> ahí se junta el agua de lluvia
                        int h = l.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                        int altos = 0;
                        for (int ox = -2; ox <= 2; ox += 2) for (int oz = -2; oz <= 2; oz += 2) {
                            if (ox == 0 && oz == 0) continue;
                            if (l.getHeight(Heightmap.Types.OCEAN_FLOOR, x + ox, z + oz) - 1 > h) altos++;
                        }
                        if (altos >= 6 && l.isRainingAt(new BlockPos(x, h + 1, z))) {
                            frente.add(new long[]{c, h, 1});
                            hoyos++;
                        }
                    }
                }
            }
        }
        ultimosCauces = cauces;
    }

    private static boolean fragil(BlockState s) {
        return s.canBeReplaced() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.CROPS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.TORCH)
                || s.is(Blocks.WALL_TORCH) || s.is(BlockTags.WOOL_CARPETS) || s.is(Blocks.SNOW) || s.is(Blocks.SUGAR_CANE)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.BUTTONS) || s.is(Blocks.REDSTONE_WIRE) || s.is(Blocks.LEVER);
    }

    /** Altura del suelo de una columna (lo más alto que es firme por debajo del nivel del agua; los árboles no cuentan). */
    private static int suelo(ServerLevel l, int x, int z, int yF, int y0) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = yF; y >= y0 - 8; y--) {
            m.set(x, y, z);
            BlockState s = l.getBlockState(m);
            if (s.isAir() || s.is(Blocks.WATER) || fragil(s) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) continue;
            if (s.blocksMotion()) return y;
        }
        return y0 - 9;
    }

    private void expandir(ServerLevel l, int columnas) {
        int puestos = 0;
        while (columnas > 0 && !frente.isEmpty() && puestos < 400) {
            long[] e = frente.poll();
            long c = e[0];
            int y0 = (int) e[1];
            int tope = e.length > 2 ? (int) e[2] : 99;
            if (visto.contains(c) || visto.size() > MAX_COLUMNAS) continue;
            visto.add(c);
            int x = (int) (c >> 32), z = (int) c;
            BlockPos ref = new BlockPos(x, y0, z);
            if (!l.hasChunkAt(ref) || lejos(l, x, z)) continue;
            if (l.getBiome(ref).is(BiomeTags.IS_OCEAN)) continue;
            columnas--;
            int yF = y0 + Math.min(nivel, tope);
            int g = suelo(l, x, z, yF, y0);
            if (g >= yF) continue; // loma, muro o dique: aquí se para
            for (int y = g + 1; y <= yF && agua.size() < MAX_BLOQUES; y++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = l.getBlockState(p);
                if (s.is(Blocks.WATER) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) continue;
                if (!s.isAir()) {
                    if (!fragil(s)) continue;
                    l.destroyBlock(p, true); // el agua se lleva cultivos, flores, antorchas...
                }
                l.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                registrar(p.asLong(), y0);
                puestos++;
                if (y == yF && l.random.nextInt(6) == 0) {
                    l.sendParticles(ParticleTypes.SPLASH, x + 0.5, y + 1, z + 0.5, 6, 0.4, 0.1, 0.4, 0.05);
                }
            }
            if (l.getBlockState(new BlockPos(x, g, z)).is(Blocks.FARMLAND)) l.setBlockAndUpdate(new BlockPos(x, g, z), Blocks.MUD.defaultBlockState());
            baseColumna.put(c, y0);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                long n = col(x + d.getStepX(), z + d.getStepZ());
                if (!visto.contains(n)) frente.add(new long[]{n, y0, tope});
            }
        }
    }

    private static boolean lejos(ServerLevel l, int x, int z) {
        for (ServerPlayer p : l.players()) {
            double dx = p.getX() - x, dz = p.getZ() - z;
            if (dx * dx + dz * dz < 120 * 120) return false;
        }
        return true;
    }

    /** La corriente arrastra lo que está en el agua de la inundación hacia lo más bajo. */
    private void corriente(ServerLevel l) {
        for (ServerPlayer jugador : l.players()) {
            for (Entity e : l.getEntities(jugador, jugador.getBoundingBox().inflate(40, 10, 40), en -> en.isInWater())) {
                arrastrar(l, e);
            }
            arrastrar(l, jugador);
        }
    }

    private void arrastrar(ServerLevel l, Entity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (!e.isInWater()) return;
        BlockPos b = e.blockPosition();
        if (!aguaSet.contains(b.asLong()) && !aguaSet.contains(b.below().asLong())) return;
        int x = b.getX(), z = b.getZ();
        // pendiente del terreno (el fondo, sin contar el agua)
        double gx = l.getHeight(Heightmap.Types.OCEAN_FLOOR, x - 2, z) - l.getHeight(Heightmap.Types.OCEAN_FLOOR, x + 2, z);
        double gz = l.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z - 2) - l.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z + 2);
        double n = Math.sqrt(gx * gx + gz * gz);
        if (n < 0.01) return;
        double fuerza = (e instanceof ItemEntity ? 0.05 : 0.025) * Math.min(1.5, 0.5 + nivel * 0.25);
        Vec3 m = e.getDeltaMovement();
        e.setDeltaMovement(m.x + gx / n * fuerza, m.y, m.z + gz / n * fuerza);
        e.hurtMarked = true;
    }

    /** Quita el agua que sobra, de arriba abajo, y deja barro. */
    private void quitar(ServerLevel l, RandomSource r, int n) {
        for (int i = 0; i < n && !retirar.isEmpty(); i++) {
            BlockPos p = BlockPos.of(retirar.poll());
            if (!l.hasChunkAt(p)) continue;
            BlockState s = l.getBlockState(p);
            if (s.is(Blocks.WATER) && l.getFluidState(p).isSource()) {
                l.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                BlockState abajo = l.getBlockState(p.below());
                if (r.nextFloat() < 0.18f && (abajo.is(Blocks.GRASS_BLOCK) || abajo.is(Blocks.DIRT) || abajo.is(Blocks.FARMLAND))) {
                    l.setBlockAndUpdate(p.below(), Blocks.MUD.defaultBlockState());
                } else if (r.nextFloat() < 0.05f && abajo.is(Blocks.GRASS_BLOCK)) {
                    l.setBlockAndUpdate(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
        }
        setDirty();
    }
}
