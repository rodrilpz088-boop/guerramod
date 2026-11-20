package es.guerramod.desastres;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Inundaciones realistas en todas partes. Con lluvia fuerte el agua se acumula y sube por todo el terreno: primero en lo
 * más bajo de cada zona (hoyos, vaguadas, calles, campos llanos, cauces y la costa) y después cada vez más alto. Las zonas
 * bajas del paisaje reciben toda el agua que escurre y se inundan mucho; en lo alto apenas se encharca, porque el agua
 * baja por las laderas. Es agua de riada (marrón, con barro). Sube poco a poco, arrastra lo que flota hacia lo más bajo,
 * se lleva cultivos, flores y antorchas, y cuando para de llover baja despacio, de arriba abajo, y deja barro.
 */
public final class Inundacion extends SavedData {
    private static final int MAX_BLOQUES = 90000;
    private static final int TICKS_SUBIDA = 2400, TICKS_BAJADA = 3000;
    private static final int RADIO = 96, RADIO_MIN = 12;

    // guardado: posición del agua puesta y con qué nivel hace falta (para quitarla al bajar)
    final LongArrayList agua = new LongArrayList();
    final IntArrayList necesita = new IntArrayList();
    int nivel = 0, lluvia = 0, ticksNivel = 0;

    // en memoria
    private final LongOpenHashSet aguaSet = new LongOpenHashSet();
    private final ArrayDeque<long[]> cola = new ArrayDeque<>();
    private final ArrayDeque<Long> retirar = new ArrayDeque<>();
    private boolean migrado = false;

    public static Inundacion get(ServerLevel l) {
        return l.getDataStorage().computeIfAbsent(Inundacion::cargar, Inundacion::new, "guerramod_inundacion");
    }

    static Inundacion cargar(CompoundTag t) {
        Inundacion f = new Inundacion();
        long[] a = t.getLongArray("agua");
        int[] b = t.getIntArray("necesita");
        for (int i = 0; i < a.length && i < b.length; i++) f.registrar(a[i], b[i]);
        if (t.contains("base")) for (long p : a) f.retirar.add(p); // agua de la versión anterior
        f.nivel = t.getInt("nivel");
        f.lluvia = t.getInt("lluvia");
        f.ticksNivel = t.getInt("ticksNivel");
        return f;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.put("agua", new LongArrayTag(agua.toLongArray()));
        t.put("necesita", new IntArrayTag(necesita.toIntArray()));
        t.putInt("nivel", nivel);
        t.putInt("lluvia", lluvia);
        t.putInt("ticksNivel", ticksNivel);
        return t;
    }

    private void registrar(long pos, int n) {
        if (!aguaSet.add(pos)) return;
        agua.add(pos);
        necesita.add(n);
    }

    private static BlockState aguaTurbia() {
        return AguaTurbia.BLOQUE.get().defaultBlockState();
    }

    // ------------------------------------------------------------------ control
    /** Provoca una inundación ya (nivel 3). Devuelve cuántas columnas de terreno va a revisar el agua. */
    public static int provocar(ServerLevel l) {
        Inundacion f = get(l);
        f.lluvia = Math.max(f.lluvia, 9000);
        l.setWeatherParameters(0, 18000, true, true);
        f.nivel = Math.max(f.nivel, 2);
        f.subir(l);
        f.setDirty();
        return f.cola.size();
    }

    public static void parar(ServerLevel l) {
        Inundacion f = get(l);
        f.lluvia = 0;
        f.ticksNivel = 0;
        f.nivel = 0;
        f.cola.clear();
        f.prepararRetirada();
        f.setDirty();
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
            if (l.getGameTime() % 600 == 0) f.planificar(l); // por si te mueves a otra zona
            f.llenar(l, r, 320);
            if (l.getGameTime() % 2 == 0) f.corriente(l);
        }
        if (!f.retirar.isEmpty()) f.quitar(l, r, 180);
        if (l.getGameTime() % 400 == 0) f.setDirty();
    }

    /** El agua de las versiones anteriores se retira. */
    private void migrar(ServerLevel l) {
        Desastres.Riada vieja = Desastres.Riada.get(l.getServer());
        if (!vieja.agua.isEmpty()) {
            for (long p : vieja.agua) retirar.add(p);
            vieja.agua.clear();
            vieja.setDirty();
        }
    }

    private void subir(ServerLevel l) {
        nivel++;
        if (nivel == 1) Desastres.alerta(l, BlockPos.ZERO, 0, "Aviso por inundaciones", "el agua empieza a acumularse en las zonas bajas");
        else if (nivel == 3) Desastres.alerta(l, BlockPos.ZERO, 0, "Inundación grave", "el agua sigue subiendo por todas partes");
        else if (nivel == 6) Desastres.alerta(l, BlockPos.ZERO, 0, "Inundación extrema", "evacúa las zonas bajas y sube a lo alto");
        planificar(l);
        setDirty();
    }

    private void bajar(ServerLevel l) {
        nivel--;
        cola.clear();
        prepararRetirada();
        if (nivel > 0) planificar(l);
        else Desastres.alerta(l, BlockPos.ZERO, 0, "Fin de la inundación", "el agua se retira y deja barro");
        setDirty();
    }

    /** Apunta para quitar (de arriba abajo) el agua que ya no corresponde al nivel actual. */
    private void prepararRetirada() {
        LongArrayList quedan = new LongArrayList();
        IntArrayList nq = new IntArrayList();
        List<Long> fuera = new ArrayList<>();
        for (int i = 0; i < agua.size(); i++) {
            if (necesita.getInt(i) > nivel) fuera.add(agua.getLong(i));
            else {
                quedan.add(agua.getLong(i));
                nq.add(necesita.getInt(i));
            }
        }
        fuera.sort((a, b) -> Integer.compare(BlockPos.getY(b), BlockPos.getY(a)));
        retirar.addAll(fuera);
        agua.clear();
        necesita.clear();
        aguaSet.clear();
        for (int i = 0; i < quedan.size(); i++) registrar(quedan.getLong(i), nq.getInt(i));
    }

    /**
     * Calcula, alrededor de cada jugador, hasta dónde llega el agua en cada columna: la base es lo más bajo de su entorno
     * (12 bloques) y la subida es mayor cuanto más baja esté esa zona respecto al paisaje (hasta 96 bloques).
     * Ordena las columnas de más baja a más alta, para que el agua aparezca primero en lo hondo.
     */
    private void planificar(ServerLevel l) {
        cola.clear();
        List<long[]> lista = new ArrayList<>();
        LongOpenHashSet hechas = new LongOpenHashSet();
        for (ServerPlayer p : l.players()) {
            int px = p.getBlockX(), pz = p.getBlockZ();
            int n = RADIO * 2 + 1;
            int[] h = new int[n * n];
            int regional = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = px - RADIO + i, z = pz - RADIO + j;
                    int v = l.hasChunkAt(new BlockPos(x, 64, z)) ? l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1 : Integer.MAX_VALUE;
                    h[i * n + j] = v;
                    if (v != Integer.MAX_VALUE && v < regional) regional = v;
                }
            }
            if (regional == Integer.MAX_VALUE) continue;
            // mínimo en celdas de 4x4 y luego en el entorno de cada columna
            int nc = (n + 3) / 4;
            int[] celda = new int[nc * nc];
            java.util.Arrays.fill(celda, Integer.MAX_VALUE);
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                int c = (i / 4) * nc + j / 4;
                celda[c] = Math.min(celda[c], h[i * n + j]);
            }
            int rc = RADIO_MIN / 4;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int di = i - RADIO, dj = j - RADIO;
                    if (di * di + dj * dj > RADIO * RADIO) continue;
                    int suelo = h[i * n + j];
                    if (suelo == Integer.MAX_VALUE) continue;
                    int x = px + di, z = pz + dj;
                    long c = BlockPos.asLong(x, 0, z);
                    if (!hechas.add(c)) continue;
                    int base = Integer.MAX_VALUE;
                    int ci = i / 4, cj = j / 4;
                    for (int a = Math.max(0, ci - rc); a <= Math.min(nc - 1, ci + rc); a++)
                        for (int b = Math.max(0, cj - rc); b <= Math.min(nc - 1, cj + rc); b++) base = Math.min(base, celda[a * nc + b]);
                    // lo alto escurre: allí sube mucho menos
                    double factor = Math.max(0.15, Math.min(1.0, 1.0 - (base - regional) / 14.0));
                    int tope = base + (int) Math.floor(nivel * factor + 0.0001);
                    if (suelo >= tope) continue;
                    lista.add(new long[]{x, z, base, (long) (factor * 1000), suelo});
                }
            }
        }
        lista.sort((a, b) -> Long.compare(a[4], b[4]));
        cola.addAll(lista);
    }

    private static boolean fragil(BlockState s) {
        return s.canBeReplaced() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.CROPS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.TORCH)
                || s.is(Blocks.WALL_TORCH) || s.is(BlockTags.WOOL_CARPETS) || s.is(Blocks.SNOW) || s.is(Blocks.SUGAR_CANE)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.BUTTONS) || s.is(Blocks.REDSTONE_WIRE) || s.is(Blocks.LEVER);
    }

    private static boolean esAgua(BlockState s) {
        return s.getFluidState().is(FluidTags.WATER) && s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
    }

    /** Suelo de una columna: lo más alto que es firme por debajo del agua (los árboles no cuentan). */
    private static int suelo(ServerLevel l, int x, int z, int tope, int base) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = tope; y >= base - 12; y--) {
            m.set(x, y, z);
            BlockState s = l.getBlockState(m);
            if (s.isAir() || esAgua(s) || fragil(s) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) continue;
            if (s.blocksMotion()) return y;
        }
        return base - 13;
    }

    private void llenar(ServerLevel l, RandomSource r, int columnas) {
        int puestos = 0;
        while (columnas-- > 0 && !cola.isEmpty() && puestos < 450 && agua.size() < MAX_BLOQUES) {
            long[] e = cola.poll();
            int x = (int) e[0], z = (int) e[1], base = (int) e[2];
            double factor = e[3] / 1000.0;
            if (!l.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int tope = base + (int) Math.floor(nivel * factor + 0.0001);
            int g = suelo(l, x, z, tope, base);
            if (g >= tope) continue;
            boolean frente = false;
            for (int y = g + 1; y <= tope; y++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = l.getBlockState(p);
                if (esAgua(s) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) continue;
                if (!s.isAir()) {
                    if (!fragil(s)) continue;
                    l.destroyBlock(p, true); // el agua se lleva cultivos, flores, antorchas...
                }
                l.setBlock(p, aguaTurbia(), Block.UPDATE_ALL);
                int need = (int) Math.ceil((y - base) / Math.max(0.15, factor) - 0.0001);
                registrar(p.asLong(), Math.max(1, need));
                puestos++;
                frente = y == tope;
            }
            if (l.getBlockState(new BlockPos(x, g, z)).is(Blocks.FARMLAND)) l.setBlockAndUpdate(new BlockPos(x, g, z), Blocks.MUD.defaultBlockState());
            if (frente && r.nextInt(10) == 0) {
                l.sendParticles(ParticleTypes.SPLASH, x + 0.5, tope + 1.0, z + 0.5, 8, 0.45, 0.1, 0.45, 0.05);
                if (r.nextInt(6) == 0) l.playSound(null, x + 0.5, tope + 0.5, z + 0.5, SoundEvents.WATER_AMBIENT, SoundSource.AMBIENT, 0.6f, 0.8f);
            }
        }
    }

    /** La corriente arrastra lo que está en el agua de la inundación hacia lo más bajo. */
    private void corriente(ServerLevel l) {
        for (ServerPlayer jugador : l.players()) {
            for (Entity e : l.getEntities(jugador, jugador.getBoundingBox().inflate(40, 10, 40), Entity::isInWater)) arrastrar(l, e);
            arrastrar(l, jugador);
        }
    }

    private void arrastrar(ServerLevel l, Entity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (!e.isInWater()) return;
        BlockPos b = e.blockPosition();
        if (!aguaSet.contains(b.asLong()) && !aguaSet.contains(b.below().asLong())) return;
        int x = b.getX(), z = b.getZ();
        double gx = l.getHeight(Heightmap.Types.OCEAN_FLOOR, x - 2, z) - l.getHeight(Heightmap.Types.OCEAN_FLOOR, x + 2, z);
        double gz = l.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z - 2) - l.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z + 2);
        double n = Math.sqrt(gx * gx + gz * gz);
        if (n < 0.01) return;
        double fuerza = (e instanceof ItemEntity ? 0.05 : 0.025) * Math.min(1.6, 0.5 + nivel * 0.2);
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
            boolean nuestra = s.is(AguaTurbia.BLOQUE.get()) || (s.is(Blocks.WATER) && l.getFluidState(p).isSource());
            if (nuestra) {
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
