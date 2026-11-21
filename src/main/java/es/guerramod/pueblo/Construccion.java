package es.guerramod.pueblo;

import es.guerramod.ciudad.Edificio;
import es.guerramod.ciudad.Plano;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Obras de los aldeanos. Las casas son las mismas que trae Minecraft en sus pueblos (llanura, desierto, sabana, taiga,
 * nieve), y en las ciudades también bloques de pisos. Se levantan bloque a bloque: cuantos más constructores trabajen
 * en la obra, antes se termina.
 */
public final class Construccion {
    private Construccion() {}

    /** Un paso de la obra. st == null: cimentación (rellena hacia abajo hasta tierra firme). */
    public record Pieza(BlockPos pos, BlockState st) {}

    public static final class Obra {
        public String tipo; // "plantilla", "edificio", "huerto"
        public String id;
        public BlockPos origen;
        public Rotation rot = Rotation.NONE;
        public int hecho;
        public BoundingBox caja;
        public BlockPos puerta;
        public int camas;
        List<Pieza> piezas;

        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putString("tipo", tipo);
            t.putString("id", id);
            t.putLong("origen", origen.asLong());
            t.putInt("rot", rot.ordinal());
            t.putInt("hecho", hecho);
            t.putIntArray("caja", new int[]{caja.minX(), caja.minY(), caja.minZ(), caja.maxX(), caja.maxY(), caja.maxZ()});
            if (puerta != null) t.putLong("puerta", puerta.asLong());
            t.putInt("camas", camas);
            return t;
        }

        static Obra cargar(CompoundTag t) {
            Obra o = new Obra();
            o.tipo = t.getString("tipo");
            o.id = t.getString("id");
            o.origen = BlockPos.of(t.getLong("origen"));
            o.rot = Rotation.values()[Math.floorMod(t.getInt("rot"), 4)];
            o.hecho = t.getInt("hecho");
            int[] a = t.getIntArray("caja");
            o.caja = a.length == 6 ? new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5]) : new BoundingBox(o.origen);
            if (t.contains("puerta")) o.puerta = BlockPos.of(t.getLong("puerta"));
            o.camas = t.getInt("camas");
            return o;
        }

        public BlockPos centro() {
            return caja.getCenter();
        }

        public int total() {
            return piezas == null ? 0 : piezas.size();
        }

        public float progreso() {
            return piezas == null || piezas.isEmpty() ? 0 : Math.min(1f, hecho / (float) piezas.size());
        }
    }

    // ====================================================================== plantillas de Minecraft

    private static final Map<String, List<ResourceLocation>> CASAS = new HashMap<>();
    private static Field paletas;

    private static List<ResourceLocation> lista(String estilo, int pequenas, int medianas, int grandes) {
        List<ResourceLocation> l = new ArrayList<>();
        for (int i = 1; i <= pequenas; i++) l.add(new ResourceLocation("minecraft", "village/" + estilo + "/houses/" + estilo + "_small_house_" + i));
        for (int i = 1; i <= medianas; i++) l.add(new ResourceLocation("minecraft", "village/" + estilo + "/houses/" + estilo + "_medium_house_" + i));
        for (int i = 1; i <= grandes; i++) l.add(new ResourceLocation("minecraft", "village/" + estilo + "/houses/" + estilo + "_big_house_" + i));
        return l;
    }

    /** Estilo de las casas según el bioma del pueblo. */
    public static String estilo(ServerLevel l, BlockPos p) {
        String b = l.getBiome(p).unwrapKey().map(k -> k.location().getPath()).orElse("plains");
        if (b.contains("desert") || b.contains("badlands")) return "desert";
        if (b.contains("savanna")) return "savanna";
        if (b.contains("snowy") || b.contains("frozen") || b.contains("ice")) return "snowy";
        if (b.contains("taiga") || b.contains("grove") || b.contains("windswept")) return "taiga";
        return "plains";
    }

    private static List<ResourceLocation> casasDe(ServerLevel l, String estilo) {
        return CASAS.computeIfAbsent(estilo, e -> {
            List<ResourceLocation> todas = switch (e) {
                case "desert" -> lista("desert", 8, 3, 0);
                case "savanna" -> lista("savanna", 8, 2, 0);
                case "taiga" -> lista("taiga", 5, 4, 0);
                case "snowy" -> lista("snowy", 8, 3, 0);
                default -> lista("plains", 8, 2, 1);
            };
            List<ResourceLocation> ok = new ArrayList<>();
            for (ResourceLocation r : todas) if (l.getStructureManager().get(r).isPresent()) ok.add(r);
            if (ok.isEmpty() && !e.equals("plains")) return casasDe(l, "plains");
            return ok;
        });
    }

    @SuppressWarnings("unchecked")
    static List<StructureTemplate.StructureBlockInfo> bloquesDe(StructureTemplate t) {
        try {
            if (paletas == null) {
                for (Field f : StructureTemplate.class.getDeclaredFields()) {
                    if (f.getType() != List.class) continue;
                    f.setAccessible(true);
                    Object v = f.get(t);
                    if (v instanceof List<?> li && !li.isEmpty() && li.get(0) instanceof StructureTemplate.Palette) {
                        paletas = f;
                        break;
                    }
                }
            }
            if (paletas == null) return List.of();
            List<StructureTemplate.Palette> p = (List<StructureTemplate.Palette>) paletas.get(t);
            return p.isEmpty() ? List.of() : p.get(0).blocks();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static BlockState estadoFinal(CompoundTag nbt) {
        if (nbt == null) return Blocks.AIR.defaultBlockState();
        String s = nbt.getString("final_state");
        if (s.isEmpty()) return Blocks.AIR.defaultBlockState();
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, true).blockState();
        } catch (Exception e) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static boolean macizo(BlockState s) {
        return s.isSolid() && !(s.getBlock() instanceof DoorBlock) && !(s.getBlock() instanceof BedBlock) && !s.is(BlockTags.TRAPDOORS);
    }

    /** Ordena: primero despejar (de arriba abajo), después cimientos, después de abajo arriba, lo frágil al final de cada planta. */
    private static void ordenar(List<Pieza> aire, List<Pieza> cimientos, List<Pieza> solidos, List<Pieza> salida) {
        aire.sort(Comparator.comparingInt((Pieza a) -> -a.pos.getY()));
        solidos.sort(Comparator.comparingInt((Pieza a) -> a.pos.getY() * 2 + (macizo(a.st) ? 0 : 1)));
        salida.addAll(aire);
        salida.addAll(cimientos);
        salida.addAll(solidos);
    }

    private static List<Pieza> piezasPlantilla(ServerLevel l, Obra o) {
        Optional<StructureTemplate> ot = l.getStructureManager().get(new ResourceLocation(o.id));
        if (ot.isEmpty()) return null;
        StructureTemplate t = ot.get();
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(o.rot);
        List<Pieza> aire = new ArrayList<>(), cim = new ArrayList<>(), sol = new ArrayList<>();
        java.util.Set<BlockPos> ocupado = new java.util.HashSet<>();
        for (StructureTemplate.StructureBlockInfo i : bloquesDe(t)) {
            BlockState st = i.state();
            if (st.is(Blocks.STRUCTURE_VOID)) continue;
            if (st.is(Blocks.JIGSAW)) st = estadoFinal(i.nbt());
            BlockPos p = StructureTemplate.calculateRelativePosition(s, i.pos()).offset(o.origen);
            st = st.rotate(o.rot);
            ocupado.add(p);
            if (st.isAir()) aire.add(new Pieza(p, st));
            else {
                sol.add(new Pieza(p, st));
                if (i.pos().getY() == 0) cim.add(new Pieza(p.below(), null));
            }
        }
        despejarCaja(o.caja, ocupado, aire);
        List<Pieza> out = new ArrayList<>();
        ordenar(aire, cim, sol, out);
        return out;
    }

    /** Todo lo que hay dentro de la obra (y un poco por encima) se despeja: árboles, hierba, nieve... */
    private static void despejarCaja(BoundingBox c, java.util.Set<BlockPos> ocupado, List<Pieza> aire) {
        BlockState a = Blocks.AIR.defaultBlockState();
        for (int y = c.minY() + 1; y <= c.maxY() + 4; y++)
            for (int x = c.minX(); x <= c.maxX(); x++)
                for (int z = c.minZ(); z <= c.maxZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!ocupado.contains(p)) aire.add(new Pieza(p, a));
                }
    }

    private static List<Pieza> piezasEdificio(Obra o) {
        Edificio e;
        try {
            e = Edificio.valueOf(o.id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        Plano plano = e.plano();
        List<Pieza> aire = new ArrayList<>(), cim = new ArrayList<>(), sol = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> en : plano.bloques().entrySet()) {
            BlockPos lp = en.getKey();
            BlockPos w = Plano.aMundo(o.origen, o.rot, lp.getX(), lp.getY(), lp.getZ());
            if (en.getValue().isAir()) aire.add(new Pieza(w, en.getValue()));
            else {
                sol.add(new Pieza(w, en.getValue().rotate(o.rot)));
                if (lp.getY() == 0) cim.add(new Pieza(w.below(), null));
            }
        }
        List<Pieza> out = new ArrayList<>();
        ordenar(aire, cim, sol, out);
        return out;
    }

    /** Huerto de 9×9 con su acequia en medio, cultivos variados y compostador. */
    private static List<Pieza> piezasHuerto(Obra o) {
        List<Pieza> out = new ArrayList<>();
        BlockPos c = o.origen;
        BlockState[] cultivos = {Blocks.WHEAT.defaultBlockState(), Blocks.CARROTS.defaultBlockState(), Blocks.POTATOES.defaultBlockState(),
                Blocks.BEETROOTS.defaultBlockState()};
        int tipo = Math.floorMod(c.getX() * 31 + c.getZ() * 17, cultivos.length);
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos s = c.offset(dx, 0, dz);
                out.add(new Pieza(s.above(), Blocks.AIR.defaultBlockState()));
                out.add(new Pieza(s.above(2), Blocks.AIR.defaultBlockState()));
            }
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos s = c.offset(dx, 0, dz);
                out.add(new Pieza(s.below(), null));
                boolean borde = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                if (borde) {
                    out.add(new Pieza(s, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,
                            Math.abs(dx) == 5 ? Direction.Axis.Z : Direction.Axis.X)));
                } else if (dx == 0 && dz == 0) {
                    out.add(new Pieza(s, Blocks.WATER.defaultBlockState()));
                } else if (dx == 0) {
                    out.add(new Pieza(s, Blocks.DIRT_PATH.defaultBlockState()));
                } else {
                    out.add(new Pieza(s, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7)));
                    out.add(new Pieza(s.above(), cultivos[(tipo + (dx > 0 ? 1 : 0)) % cultivos.length]));
                }
            }
        out.add(new Pieza(c.offset(0, 1, 5), Blocks.COMPOSTER.defaultBlockState()));
        out.add(new Pieza(c.offset(0, 1, -5), Blocks.LANTERN.defaultBlockState()));
        return out;
    }

    /** Árbol de luces como el de la Puerta del Sol: un cono de malla de luces con la estrella arriba. */
    private static List<Pieza> piezasArbol(Obra o) {
        List<Pieza> out = new ArrayList<>();
        BlockPos c = o.origen;
        BlockState malla = Plano.mod("malla_luces", Blocks.GLOWSTONE), estrella = Plano.mod("estrella_navidad", Blocks.SEA_LANTERN);
        int alto = 14;
        for (int y = 1; y <= alto; y++) {
            double r = 4.6 * (1.0 - (y - 1) / (double) alto) + 0.3;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= r + 0.25 && d >= r - 0.85) out.add(new Pieza(c.offset(dx, y, dz), malla));
                }
        }
        out.add(new Pieza(c.offset(0, alto + 1, 0), estrella));
        return out;
    }

    /** Sitio para el árbol de Navidad, en la plaza. */
    public static Obra planearArbol(ServerLevel l, Pueblos.Pueblo pu, RandomSource r) {
        for (int intento = 0; intento < 20; intento++) {
            double ang = r.nextDouble() * Math.PI * 2, rad = 7 + r.nextDouble() * 8;
            int x = pu.centro.getX() + (int) (Math.cos(ang) * rad), z = pu.centro.getZ() + (int) (Math.sin(ang) * rad);
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            boolean ok = true;
            for (int dx = -5; dx <= 5 && ok; dx++)
                for (int dz = -5; dz <= 5 && ok; dz++) {
                    int h = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
                    if (Math.abs(h - y) > 1) ok = false;
                }
            if (!ok) continue;
            Obra o = new Obra();
            o.tipo = "arbol";
            o.id = "arbol";
            o.origen = new BlockPos(x, y, z);
            o.caja = new BoundingBox(x - 5, y, z - 5, x + 5, y + 16, z + 5);
            return o;
        }
        return null;
    }

    static List<Pieza> piezas(ServerLevel l, Obra o) {
        if (o.piezas == null) {
            o.piezas = switch (o.tipo) {
                case "plantilla" -> piezasPlantilla(l, o);
                case "edificio" -> piezasEdificio(o);
                case "huerto" -> piezasHuerto(o);
                case "arbol" -> piezasArbol(o);
                default -> null;
            };
            if (o.piezas == null) o.piezas = new ArrayList<>();
        }
        return o.piezas;
    }

    // ====================================================================== elegir solar

    private static boolean suelo(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.STONE) || s.is(Blocks.TERRACOTTA) || s.is(Blocks.RED_SAND) || s.is(Blocks.CLAY) || s.is(Blocks.MUD)
                || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.SANDSTONE);
    }

    /** Altura del suelo natural (el bloque de tierra de arriba) o Integer.MIN_VALUE si no vale (agua, construido...). */
    public static int alturaSuelo(ServerLevel l, int x, int z) {
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos p = new BlockPos(x, y, z);
        BlockState s = l.getBlockState(p);
        while ((s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.canBeReplaced()) && y > l.getMinBuildHeight() + 1 && s.getFluidState().isEmpty()) {
            p = p.below();
            y--;
            s = l.getBlockState(p);
        }
        if (!s.getFluidState().isEmpty()) return Integer.MIN_VALUE;
        if (!suelo(s)) return Integer.MIN_VALUE;
        return y;
    }

    private static boolean libre(ServerLevel l, Pueblos.Pueblo pu, BoundingBox caja, int[] ySuelo) {
        if (pu.ocupado(caja)) return false;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        List<Integer> alturas = new ArrayList<>();
        for (int x = caja.minX(); x <= caja.maxX(); x += 2)
            for (int z = caja.minZ(); z <= caja.maxZ(); z += 2) {
                int h = alturaSuelo(l, x, z);
                if (h == Integer.MIN_VALUE) return false;
                min = Math.min(min, h);
                max = Math.max(max, h);
                alturas.add(h);
            }
        if (max - min > 4) return false;
        alturas.sort(Integer::compare);
        ySuelo[0] = alturas.get(alturas.size() / 2);
        return true;
    }

    private static Direction haciaCentro(BlockPos desde, BlockPos centro) {
        int dx = centro.getX() - desde.getX(), dz = centro.getZ() - desde.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockPos jigsaw(StructureTemplate t, StructurePlaceSettings s, BlockPos origen, Direction[] frente) {
        for (StructureTemplate.StructureBlockInfo i : bloquesDe(t)) {
            if (!i.state().is(Blocks.JIGSAW)) continue;
            Direction f = i.state().getValue(BlockStateProperties.ORIENTATION).front();
            if (f.getAxis().isVertical()) continue;
            frente[0] = s.getRotation().rotate(f);
            return StructureTemplate.calculateRelativePosition(s, i.pos()).offset(origen);
        }
        return null;
    }

    /** Busca sitio para una casa de Minecraft alrededor del pueblo, con la puerta mirando al centro. */
    public static Obra planearCasa(ServerLevel l, Pueblos.Pueblo pu, RandomSource r) {
        List<ResourceLocation> casas = casasDe(l, estilo(l, pu.centro));
        if (casas.isEmpty()) return null;
        int radioMax = Math.min(90, 22 + 3 * pu.casas.size());
        for (int intento = 0; intento < 30; intento++) {
            ResourceLocation rl = casas.get(r.nextInt(casas.size()));
            StructureTemplate t = l.getStructureManager().get(rl).orElse(null);
            if (t == null) continue;
            double ang = r.nextDouble() * Math.PI * 2;
            double rad = 12 + r.nextDouble() * (radioMax - 12);
            BlockPos guia = pu.centro.offset((int) (Math.cos(ang) * rad), 0, (int) (Math.sin(ang) * rad));
            // la rotación que deja la puerta (el conector del camino) mirando al centro
            Rotation mejor = Rotation.values()[r.nextInt(4)];
            double mejorDot = -2;
            for (Rotation rot : Rotation.values()) {
                StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot);
                Direction[] f = new Direction[1];
                if (jigsaw(t, s, guia, f) == null) break;
                Direction h = haciaCentro(guia, pu.centro);
                double dot = f[0] == h ? 1 : f[0] == h.getOpposite() ? -1 : 0;
                if (dot > mejorDot) {
                    mejorDot = dot;
                    mejor = rot;
                }
            }
            StructurePlaceSettings s = new StructurePlaceSettings().setRotation(mejor);
            BoundingBox caja = t.getBoundingBox(s, guia);
            int[] y = new int[1];
            if (!libre(l, pu, caja, y)) continue;
            BlockPos origen = new BlockPos(guia.getX(), y[0], guia.getZ());
            Obra o = new Obra();
            o.tipo = "plantilla";
            o.id = rl.toString();
            o.origen = origen;
            o.rot = mejor;
            o.caja = t.getBoundingBox(s, origen);
            Direction[] f = new Direction[1];
            BlockPos j = jigsaw(t, s, origen, f);
            o.puerta = j != null ? j.relative(f[0]) : o.caja.getCenter().atY(origen.getY());
            return o;
        }
        return null;
    }

    /** En las ciudades: casas modernas y bloques de pisos (con sus camas, para que se muden los aldeanos). */
    public static Obra planearEdificio(ServerLevel l, Pueblos.Pueblo pu, RandomSource r, Edificio e) {
        Plano plano = e.plano();
        int radioMax = Math.min(110, 30 + 3 * pu.casas.size());
        for (int intento = 0; intento < 30; intento++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double rad = 18 + r.nextDouble() * (radioMax - 18);
            BlockPos guia = pu.centro.offset((int) (Math.cos(ang) * rad), 0, (int) (Math.sin(ang) * rad));
            Direction mirando = haciaCentro(guia, pu.centro).getOpposite();
            Rotation rot = Plano.rotacion(mirando);
            BoundingBox caja = cajaPlano(plano, guia, rot);
            int[] y = new int[1];
            if (!libre(l, pu, caja, y)) continue;
            BlockPos origen = new BlockPos(guia.getX(), y[0], guia.getZ());
            Obra o = new Obra();
            o.tipo = "edificio";
            o.id = e.name();
            o.origen = origen;
            o.rot = rot;
            o.caja = cajaPlano(plano, origen, rot);
            o.puerta = Plano.aMundo(origen, rot, 0, 0, -3);
            o.camas = e == Edificio.BLOQUE_PISOS ? 18 : e == Edificio.RASCACIELOS ? 40 : 3;
            return o;
        }
        return null;
    }

    private static BoundingBox cajaPlano(Plano plano, BlockPos origen, Rotation rot) {
        BoundingBox b = null;
        for (BlockPos lp : plano.bloques().keySet()) {
            BlockPos w = Plano.aMundo(origen, rot, lp.getX(), lp.getY(), lp.getZ());
            b = b == null ? new BoundingBox(w) : b.encapsulate(w);
        }
        return b == null ? new BoundingBox(origen) : b;
    }

    /** Huerto cerca del centro. */
    public static Obra planearHuerto(ServerLevel l, Pueblos.Pueblo pu, RandomSource r) {
        for (int intento = 0; intento < 25; intento++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double rad = 14 + r.nextDouble() * Math.min(50, 16 + 2 * pu.casas.size());
            BlockPos guia = pu.centro.offset((int) (Math.cos(ang) * rad), 0, (int) (Math.sin(ang) * rad));
            BoundingBox caja = new BoundingBox(guia.getX() - 5, guia.getY() - 2, guia.getZ() - 5, guia.getX() + 5, guia.getY() + 3, guia.getZ() + 5);
            int[] y = new int[1];
            if (!libre(l, pu, caja, y)) continue;
            Obra o = new Obra();
            o.tipo = "huerto";
            o.id = "huerto";
            o.origen = new BlockPos(guia.getX(), y[0], guia.getZ());
            o.caja = new BoundingBox(guia.getX() - 5, y[0] - 1, guia.getZ() - 5, guia.getX() + 5, y[0] + 2, guia.getZ() + 5);
            o.puerta = o.origen.offset(0, 0, 6);
            return o;
        }
        return null;
    }

    // ====================================================================== trabajar

    private static final BlockState CIMIENTO = Blocks.COBBLESTONE.defaultBlockState();

    /**
     * Coloca hasta n bloques de la obra. Lo que ya está bien no cuenta, así que despejar el aire es rápido.
     * Devuelve true cuando la obra está terminada.
     */
    public static boolean trabajar(ServerLevel l, Obra o, int n) {
        List<Pieza> ps = piezas(l, o);
        int puestos = 0, mirados = 0;
        BlockPos ultimo = null;
        BlockState ultimoSt = null;
        while (o.hecho < ps.size() && puestos < n && mirados < n * 40) {
            Pieza p = ps.get(o.hecho++);
            mirados++;
            if (!l.isLoaded(p.pos)) {
                o.hecho--;
                return false;
            }
            if (p.st == null) {
                // cimentación: rellena hacia abajo hasta tierra firme
                BlockState abajo = o.tipo.equals("huerto") ? Blocks.DIRT.defaultBlockState() : CIMIENTO;
                for (int d = 0; d < 12; d++) {
                    BlockPos q = p.pos.below(d);
                    BlockState s = l.getBlockState(q);
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS))) break;
                    l.setBlock(q, abajo, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    puestos++;
                }
                continue;
            }
            BlockState ahora = l.getBlockState(p.pos);
            if (ahora == p.st) continue;
            if (p.st.isAir() && ahora.isAir()) continue;
            if (p.st.isAir() && (ahora.is(Blocks.BEDROCK))) continue;
            l.setBlock(p.pos, p.st, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            puestos++;
            if (!p.st.isAir()) {
                ultimo = p.pos;
                ultimoSt = p.st;
            }
        }
        if (ultimo != null) {
            if (l.random.nextInt(2) == 0) {
                l.playSound(null, ultimo, ultimoSt.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7f, 0.85f + l.random.nextFloat() * 0.3f);
            }
            l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ultimoSt), ultimo.getX() + 0.5, ultimo.getY() + 0.5, ultimo.getZ() + 0.5,
                    4, 0.3, 0.3, 0.3, 0.05);
        }
        if (o.hecho >= ps.size()) {
            terminar(l, o);
            return true;
        }
        return false;
    }

    private static void terminar(ServerLevel l, Obra o) {
        // conectar vallas, cristales, puertas y escaleras con sus vecinos
        for (Pieza p : o.piezas) {
            if (p.st == null || p.st.isAir()) continue;
            BlockState s = l.getBlockState(p.pos);
            BlockState n = Block.updateFromNeighbourShapes(s, l, p.pos);
            if (n != s) l.setBlock(p.pos, n, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        if (o.camas > 0) amueblarCamas(l, o.caja, o.camas);
        l.playSound(null, o.centro(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    /** Pone camas en los huecos libres de un edificio (dos bloques de suelo con aire encima, lejos de las puertas). */
    static void amueblarCamas(ServerLevel l, BoundingBox c, int cuantas) {
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        List<BlockPos> puestas = new ArrayList<>();
        Block[] colores = {Blocks.RED_BED, Blocks.BLUE_BED, Blocks.WHITE_BED, Blocks.LIGHT_GRAY_BED, Blocks.GREEN_BED, Blocks.YELLOW_BED};
        for (int y = c.minY() + 1; y <= c.maxY() - 1 && puestas.size() < cuantas; y++)
            for (int x = c.minX() + 1; x <= c.maxX() - 1 && puestas.size() < cuantas; x++)
                buscar:
                for (int z = c.minZ() + 1; z <= c.maxZ() - 1 && puestas.size() < cuantas; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    for (BlockPos q : puestas) if (q.distManhattan(p) < 4) continue buscar;
                    for (Direction d : dirs) {
                        BlockPos h = p.relative(d);
                        if (!libreParaCama(l, p) || !libreParaCama(l, h)) continue;
                        // pegada a una pared por la cabecera y sin puertas cerca
                        if (!l.getBlockState(h.relative(d)).isSolid()) continue;
                        if (cercaDePuerta(l, p) || cercaDePuerta(l, h)) continue;
                        Block b = colores[Math.floorMod(x * 7 + z * 13 + y, colores.length)];
                        BlockState pie = b.defaultBlockState().setValue(BedBlock.FACING, d).setValue(BedBlock.PART, BedPart.FOOT);
                        l.setBlock(h, pie.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
                        l.setBlock(p, pie, Block.UPDATE_ALL);
                        puestas.add(p);
                        continue buscar;
                    }
                }
    }

    private static boolean libreParaCama(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getBlockState(p.below()).isSolid();
    }

    private static boolean cercaDePuerta(ServerLevel l, BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
            if (l.getBlockState(q).getBlock() instanceof DoorBlock) return true;
        }
        return false;
    }
}
