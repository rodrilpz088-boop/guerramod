package es.guerramod.ciudad;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

import static es.guerramod.ciudad.Plano.AIRE;
import static es.guerramod.ciudad.Plano.escalon;
import static es.guerramod.ciudad.Plano.mod;

/** Edificios que se pueden construir con los planos. */
public enum Edificio {
    CASA("Casa unifamiliar", "casa de dos aguas con tejado de teja", false, Edificio::casa),
    BLOQUE_PISOS("Bloque de pisos", "6 plantas con balcones", false, Edificio::bloquePisos),
    RASCACIELOS("Rascacielos", "22 plantas de cristal y helipuerto", false, Edificio::rascacielos),
    NAVE("Nave almacén", "nave logística con lucernarios", false, Edificio::nave),
    FABRICA("Fábrica", "nave con cubierta de dientes de sierra y 2 chimeneas: EMITE CO2", true, Edificio::fabrica),
    CENTRAL("Central térmica de carbón", "2 chimeneas de 47 m y torre de refrigeración: EMITE MUCHO CO2", true, Edificio::central),
    AEROGENERADOR("Aerogenerador", "energía limpia, sin emisiones", false, Edificio::aerogenerador),
    CAPTURA_CO2("Planta de captura de CO2", "absorbe CO2 del aire y frena el calentamiento", false, Edificio::capturaCo2);

    public final String nombre, descripcion;
    public final boolean contamina;
    private final Consumer<Plano> diseno;
    private Plano cache;

    Edificio(String nombre, String descripcion, boolean contamina, Consumer<Plano> diseno) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.contamina = contamina;
        this.diseno = diseno;
    }

    /** El plano completo (se calcula una vez). */
    public synchronized Plano plano() {
        if (cache == null) {
            Plano p = new Plano();
            diseno.accept(p);
            cache = p;
        }
        return cache;
    }

    // ------------------------------------------------------------------ materiales
    private static BlockState blanco() { return mod("gotele_blanco", Blocks.WHITE_CONCRETE); }
    private static BlockState ladrillo() { return mod("ladrillo_caravista", Blocks.BRICKS); }
    private static BlockState granito() { return mod("granito_avila", Blocks.STONE_BRICKS); }
    private static BlockState parquet() { return mod("parquet_roble", Blocks.OAK_PLANKS); }
    private static BlockState gres() { return mod("gres_gris", Blocks.SMOOTH_STONE); }
    private static BlockState marmol() { return mod("marmol_blanco", Blocks.QUARTZ_BLOCK); }
    private static BlockState hormigon() { return mod("hormigon_pulido", Blocks.LIGHT_GRAY_CONCRETE); }
    private static BlockState teja() { return mod("teja_arabe", Blocks.BRICKS); }
    private static BlockState ventana() { return mod("ventana_aluminio", Blocks.GLASS_PANE); }
    private static BlockState led() { return mod("led_techo", Blocks.GLOWSTONE); }
    private static BlockState s(Block b) { return b.defaultBlockState(); }

    private static BlockState escaleraMano() {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
    }

    // ------------------------------------------------------------------ casa
    private static void casa(Plano p) {
        p.caja(-6, 1, -2, 6, 12, 9, AIRE);
        p.caja(-5, 0, 0, 5, 0, 8, parquet());
        p.caja(-1, 0, -2, 1, 0, -1, gres()); // camino de entrada
        p.paredes(-5, 1, 0, 5, 4, 8, blanco());
        p.paredes(-5, 1, 0, 5, 1, 8, granito()); // zócalo de granito
        for (int y = 1; y <= 4; y++) { // esquinas de piedra
            p.set(-5, y, 0, granito()); p.set(5, y, 0, granito()); p.set(-5, y, 8, granito()); p.set(5, y, 8, granito());
        }
        for (int x : new int[]{-3, -2, 2, 3}) for (int y = 2; y <= 3; y++) { p.set(x, y, 0, ventana()); p.set(x, y, 8, ventana()); }
        for (int z : new int[]{2, 3, 6, 7}) for (int y = 2; y <= 3; y++) { p.set(-5, y, z, ventana()); p.set(5, y, z, ventana()); }
        p.puerta(0, 1, 0, mod("puerta_roble", Blocks.OAK_DOOR), false);
        // tabique con puerta y dormitorio al fondo
        p.caja(-4, 1, 5, 4, 4, 5, blanco());
        p.puerta(0, 1, 5, mod("puerta_blanca", Blocks.BIRCH_DOOR), true);
        // techo y tejado a dos aguas (caballete paralelo a la fachada)
        p.caja(-5, 5, 0, 5, 5, 8, blanco());
        for (int k = 0; k <= 4; k++) {
            int y = 5 + k;
            for (int x = -6; x <= 6; x++) {
                p.set(x, y, -1 + k, escalon(Blocks.BRICK_STAIRS, Direction.NORTH));
                p.set(x, y, 9 - k, escalon(Blocks.BRICK_STAIRS, Direction.SOUTH));
            }
            for (int z = k; z <= 8 - k; z++) { p.set(-5, y, z, blanco()); p.set(5, y, z, blanco()); }
        }
        for (int x = -6; x <= 6; x++) p.set(x, 9, 4, teja());
        p.caja(3, 7, 6, 3, 11, 6, ladrillo()); // chimenea de la casa (decorativa)
        // interior
        p.set(-2, 4, 2, led()); p.set(2, 4, 2, led()); p.set(0, 4, 7, led());
        p.set(-3, 1, 2, mod("mesa_comedor", Blocks.OAK_PLANKS));
        p.set(4, 1, 1, mod("planta_interior", Blocks.POTTED_FERN));
        p.set(-4, 1, 7, mod("comoda", Blocks.BOOKSHELF));
        for (int x = -2; x <= 2; x++) for (int z = 6; z <= 7; z++) p.set(x, 1, z, mod("alfombra_beige", Blocks.WHITE_CARPET));
    }

    // ------------------------------------------------------------------ bloque de pisos
    private static void bloquePisos(Plano p) {
        p.caja(-7, 1, -3, 7, 30, 11, AIRE);
        for (int f = 0; f < 6; f++) {
            int y0 = f * 4;
            p.caja(-6, y0, 0, 6, y0, 10, f == 0 ? gres() : hormigon());
            p.paredes(-6, y0, 0, 6, y0, 10, blanco());
            p.paredes(-6, y0 + 1, 0, 6, y0 + 3, 10, ladrillo());
            for (int x : new int[]{-4, -3, 3, 4}) for (int y = y0 + 2; y <= y0 + 3; y++) { p.set(x, y, 0, ventana()); p.set(x, y, 10, ventana()); }
            for (int z : new int[]{3, 4, 6, 7}) for (int y = y0 + 2; y <= y0 + 3; y++) { p.set(-6, y, z, ventana()); p.set(6, y, z, ventana()); }
            p.set(-3, y0 + 3, 5, led()); p.set(3, y0 + 3, 5, led());
            if (f > 0) {
                // balcones con barandilla y puerta balconera
                for (int[] lado : new int[][]{{-5, -2}, {2, 5}}) {
                    for (int x = lado[0]; x <= lado[1]; x++) {
                        p.set(x, y0, -1, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                        p.set(x, y0 + 1, -2, s(Blocks.IRON_BARS));
                    }
                    p.set(lado[0], y0 + 1, -1, s(Blocks.IRON_BARS));
                    p.set(lado[1], y0 + 1, -1, s(Blocks.IRON_BARS));
                }
                p.puerta(-2, y0 + 1, 0, mod("puerta_cristal", Blocks.OAK_DOOR), false);
                p.puerta(2, y0 + 1, 0, mod("puerta_cristal", Blocks.OAK_DOOR), true);
            }
        }
        for (int y = 1; y <= 24; y++) p.set(5, y, 9, escaleraMano()); // escalera de mano en la esquina del fondo
        p.puerta(0, 1, 0, mod("puerta_cristal", Blocks.OAK_DOOR), false);
        p.set(-1, 3, 0, s(Blocks.GLASS)); p.set(1, 3, 0, s(Blocks.GLASS));
        // cubierta plana con peto, depósito y antena
        p.caja(-6, 24, 0, 6, 24, 10, hormigon());
        p.set(5, 24, 9, escaleraMano());
        p.set(5, 25, 9, AIRE);
        p.paredes(-6, 25, 0, 6, 25, 10, blanco());
        p.caja(-4, 25, 6, -3, 26, 7, s(Blocks.IRON_BLOCK));
        p.caja(3, 25, 3, 3, 29, 3, s(Blocks.LIGHTNING_ROD));
    }

    // ------------------------------------------------------------------ rascacielos
    private static void rascacielos(Plano p) {
        p.caja(-8, 1, -2, 8, 97, 15, AIRE);
        BlockState cristal = s(Blocks.LIGHT_BLUE_STAINED_GLASS), pilar = s(Blocks.LIGHT_GRAY_CONCRETE), nucleo = s(Blocks.GRAY_CONCRETE);
        for (int f = 0; f < 22; f++) {
            int y0 = f * 4;
            p.caja(-7, y0, 0, 7, y0, 14, f == 0 ? marmol() : pilar);
            p.paredes(-7, y0 + 1, 0, 7, y0 + 3, 14, cristal);
            for (int y = y0 + 1; y <= y0 + 3; y++) {
                for (int c : new int[]{-7, -3, 3, 7}) { p.set(c, y, 0, pilar); p.set(c, y, 14, pilar); }
                for (int c : new int[]{0, 7, 14}) { p.set(-7, y, c, pilar); p.set(7, y, c, pilar); }
            }
            // núcleo de comunicaciones con escalera
            p.paredes(-1, y0 + 1, 6, 1, y0 + 3, 8, nucleo);
            p.set(0, y0 + 1, 6, AIRE); p.set(0, y0 + 2, 6, AIRE);
            for (int[] l : new int[][]{{-4, 3}, {4, 3}, {-4, 11}, {4, 11}}) p.set(l[0], y0 + 3, l[1], led());
        }
        for (int y = 1; y <= 87; y++) p.set(0, y, 7, escaleraMano());
        // vestíbulo
        p.puerta(-1, 1, 0, mod("puerta_cristal", Blocks.OAK_DOOR), false);
        p.puerta(1, 1, 0, mod("puerta_cristal", Blocks.OAK_DOOR), true);
        p.set(0, 1, 0, s(Blocks.GLASS)); p.set(0, 2, 0, s(Blocks.GLASS));
        // azotea con helipuerto y antena
        p.caja(-7, 88, 0, 7, 88, 14, nucleo);
        p.set(0, 88, 7, escaleraMano());
        p.paredes(-7, 89, 0, 7, 89, 14, pilar);
        for (int z = 4; z <= 10; z++) { p.set(-3, 88, z, s(Blocks.YELLOW_CONCRETE)); p.set(3, 88, z, s(Blocks.YELLOW_CONCRETE)); }
        for (int x = -2; x <= 2; x++) p.set(x, 88, 7, s(Blocks.YELLOW_CONCRETE));
        p.set(0, 88, 7, escaleraMano());
        p.caja(6, 89, 13, 6, 95, 13, s(Blocks.IRON_BARS));
        p.set(6, 96, 13, s(Blocks.SHROOMLIGHT));
    }

    // ------------------------------------------------------------------ nave almacén
    private static void nave(Plano p) {
        p.caja(-11, 1, -1, 11, 12, 15, AIRE);
        p.caja(-10, 0, 0, 10, 0, 14, hormigon());
        p.paredes(-10, 1, 0, 10, 3, 14, s(Blocks.GRAY_CONCRETE));
        p.paredes(-10, 4, 0, 10, 7, 14, mod("pared_gris", Blocks.LIGHT_GRAY_CONCRETE));
        p.caja(-10, 8, 0, 10, 8, 14, s(Blocks.LIGHT_GRAY_CONCRETE));
        for (int x : new int[]{-6, 0, 6}) for (int z = 2; z <= 12; z++) p.set(x, 8, z, s(Blocks.GLASS));
        p.caja(-3, 1, 0, 3, 5, 0, AIRE); // muelle de carga
        p.puerta(7, 1, 0, mod("puerta_blanca", Blocks.BIRCH_DOOR), false);
        for (int x = -8; x <= -5; x++) for (int z = 4; z <= 12; z += 2) { p.set(x, 1, z, s(Blocks.BARREL)); p.set(x, 2, z, s(Blocks.BARREL)); }
        for (int x = 5; x <= 8; x++) for (int z = 4; z <= 12; z += 2) p.set(x, 1, z, s(Blocks.CHEST));
        for (int x = -6; x <= 6; x += 6) for (int z = 4; z <= 10; z += 6) p.set(x, 7, z, led());
    }

    // ------------------------------------------------------------------ fábrica
    private static void fabrica(Plano p) {
        p.caja(-13, 1, -1, 13, 33, 19, AIRE);
        p.caja(-12, 0, 0, 12, 0, 18, hormigon());
        p.paredes(-12, 1, 0, 12, 3, 18, ladrillo());
        p.paredes(-12, 4, 0, 12, 9, 18, s(Blocks.LIGHT_GRAY_CONCRETE));
        for (int x = -11; x <= 11; x++) if (x % 4 != 0) for (int y = 6; y <= 7; y++) { p.set(x, y, 0, ventana()); p.set(x, y, 18, ventana()); }
        for (int z = 1; z <= 17; z++) if (z % 4 != 0) for (int y = 6; y <= 7; y++) { p.set(-12, y, z, ventana()); p.set(12, y, z, ventana()); }
        // cubierta en dientes de sierra con lucernarios (la luz entra por el lado del norte)
        for (int zz = 0; zz <= 12; zz += 6) {
            for (int k = 0; k <= 3; k++) {
                int z = zz + k, y = 10 + k;
                for (int x = -12; x <= 12; x++) p.set(x, y, z, escalon(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
                for (int yy = 10; yy < y; yy++) { p.set(-12, yy, z, s(Blocks.LIGHT_GRAY_CONCRETE)); p.set(12, yy, z, s(Blocks.LIGHT_GRAY_CONCRETE)); }
            }
            for (int y = 10; y <= 13; y++) for (int x = -12; x <= 12; x++) p.set(x, y, zz + 4, (x == -12 || x == 12) ? s(Blocks.LIGHT_GRAY_CONCRETE) : s(Blocks.GLASS));
            for (int x = -12; x <= 12; x++) if (zz + 5 <= 18) p.set(x, 10, zz + 5, s(Blocks.LIGHT_GRAY_CONCRETE));
        }
        for (int x = -12; x <= 12; x++) p.set(x, 10, 18, s(Blocks.LIGHT_GRAY_CONCRETE));
        // puertas
        p.caja(-2, 1, 0, 2, 5, 0, AIRE);
        p.puerta(8, 1, 0, mod("puerta_blanca", Blocks.BIRCH_DOOR), false);
        // maquinaria
        for (int x = -9; x <= 9; x += 3) {
            p.set(x, 1, 5, Plano.mirando(s(Blocks.BLAST_FURNACE), Direction.SOUTH));
            p.set(x, 1, 11, Plano.mirando(s(Blocks.SMOKER), Direction.SOUTH));
            p.set(x + 1, 1, 5, s(Blocks.ANVIL));
            p.set(x + 1, 1, 11, s(Blocks.CRAFTING_TABLE));
        }
        // depósitos
        p.cilindro(-3, 15, 2, 1, 7, s(Blocks.WHITE_CONCRETE), true);
        p.cilindro(-3, 15, 2, 8, 8, s(Blocks.WHITE_CONCRETE), false);
        for (int x = -6; x <= 6; x += 6) for (int z = 3; z <= 15; z += 6) p.set(x, 9, z, led());
        // dos chimeneas de ladrillo de 28 m
        for (int cx : new int[]{-9, 9}) chimenea(p, cx, 15, 1, 27, ladrillo(), ladrillo(), BloquesCiudad.CHIMENEA_INDUSTRIAL.get().defaultBlockState());
    }

    private static void chimenea(Plano p, int cx, int cz, int y1, int y2, BlockState a, BlockState b, BlockState boca) {
        for (int y = y1; y <= y2; y++) {
            BlockState m = ((y / 5) % 2 == 0) ? a : b;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) == 2 && y > y1 + 3) continue; // más fina arriba
                p.set(cx + dx, y, cz + dz, m);
            }
        }
        p.set(cx, y2 + 1, cz, boca);
    }

    // ------------------------------------------------------------------ central térmica
    private static void central(Plano p) {
        p.caja(-18, 1, -1, 18, 50, 25, AIRE);
        p.caja(-17, 0, 0, 17, 0, 24, s(Blocks.GRAY_CONCRETE));
        // sala de calderas
        p.paredes(-17, 1, 2, -3, 16, 22, s(Blocks.LIGHT_GRAY_CONCRETE));
        p.paredes(-17, 8, 2, -3, 8, 22, s(Blocks.GRAY_CONCRETE));
        for (int x = -16; x <= -4; x++) if (x % 3 != 0) for (int y = 12; y <= 13; y++) { p.set(x, y, 2, ventana()); p.set(x, y, 22, ventana()); }
        p.caja(-17, 17, 2, -3, 17, 22, s(Blocks.GRAY_CONCRETE));
        p.caja(-12, 1, 2, -8, 5, 2, AIRE);
        for (int x = -15; x <= -5; x += 5) for (int z = 6; z <= 18; z += 6) p.set(x, 16, z, led());
        for (int x = -15; x <= -6; x += 3) p.set(x, 1, 12, Plano.mirando(s(Blocks.BLAST_FURNACE), Direction.SOUTH));
        // chimeneas de 47 m a franjas rojas y blancas
        for (int cx : new int[]{-13, -7}) chimenea(p, cx, 19, 1, 46, s(Blocks.WHITE_CONCRETE), s(Blocks.RED_CONCRETE),
                BloquesCiudad.CHIMENEA_CENTRAL.get().defaultBlockState());
        // torre de refrigeración (hiperboloide de hormigón): solo suelta vapor de agua
        double cx = 8.5, cz = 12;
        for (int y = 1; y <= 32; y++) {
            double r = 3.5 * Math.sqrt(1 + Math.pow((y - 24) / 12.0, 2));
            p.cilindro(cx, cz, r, y, y, s(Blocks.WHITE_CONCRETE), true);
        }
        BlockState vapor = BloquesCiudad.CHIMENEA_VAPOR.get().defaultBlockState();
        p.set(8, 30, 12, vapor); p.set(9, 30, 12, vapor); p.set(8, 30, 11, vapor); p.set(9, 30, 13, vapor);
        // carbón y transformadores
        for (int x = -2; x <= 0; x++) for (int z = 4; z <= 9; z++) { p.set(x, 1, z, s(Blocks.COAL_BLOCK)); if (x == -1) p.set(x, 2, z, s(Blocks.COAL_BLOCK)); }
        for (int x = 4; x <= 14; x += 5) {
            p.caja(x, 1, 0, x + 1, 2, 1, s(Blocks.IRON_BLOCK));
            p.set(x, 3, 0, s(Blocks.LIGHTNING_ROD)); p.set(x + 1, 3, 1, s(Blocks.LIGHTNING_ROD));
        }
    }

    // ------------------------------------------------------------------ aerogenerador
    private static void aerogenerador(Plano p) {
        p.caja(-16, 1, -1, 16, 54, 4, AIRE);
        p.caja(-2, 0, 0, 2, 0, 4, s(Blocks.GRAY_CONCRETE));
        p.caja(-1, 1, 1, 1, 6, 3, s(Blocks.WHITE_CONCRETE));
        p.caja(0, 7, 2, 0, 36, 2, s(Blocks.WHITE_CONCRETE));
        p.caja(0, 36, 1, 0, 37, 4, s(Blocks.WHITE_CONCRETE)); // góndola
        p.set(0, 37, 0, s(Blocks.LIGHT_GRAY_CONCRETE)); // buje
        p.set(0, 38, 4, s(Blocks.SHROOMLIGHT)); // baliza
        for (double a : new double[]{90, 210, 330}) {
            double r = Math.toRadians(a);
            for (int t = 1; t <= 15; t++) {
                int x = (int) Math.round(Math.cos(r) * t), y = 37 + (int) Math.round(Math.sin(r) * t);
                p.set(x, y, -1, t <= 11 ? s(Blocks.WHITE_CONCRETE) : s(Blocks.SMOOTH_QUARTZ));
            }
        }
        p.set(0, 37, -1, s(Blocks.LIGHT_GRAY_CONCRETE));
    }

    // ------------------------------------------------------------------ captura de CO2
    private static void capturaCo2(Plano p) {
        p.caja(-9, 1, -1, 8, 10, 9, AIRE);
        p.caja(-8, 0, 0, 7, 0, 8, s(Blocks.GRAY_CONCRETE));
        BlockState captador = BloquesCiudad.CAPTADOR_CO2.get().defaultBlockState();
        for (int x0 = -8; x0 <= 4; x0 += 4) {
            p.paredes(x0, 1, 1, x0 + 3, 5, 7, s(Blocks.LIGHT_GRAY_CONCRETE));
            p.caja(x0, 6, 1, x0 + 3, 6, 7, s(Blocks.GRAY_CONCRETE));
            for (int x = x0 + 1; x <= x0 + 2; x++) for (int y = 2; y <= 4; y++) { p.set(x, y, 1, captador); p.set(x, y, 7, captador); }
        }
        p.caja(-8, 1, -1, 7, 1, -1, s(Blocks.IRON_BARS));
    }
}
