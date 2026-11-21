package es.guerramod.pueblo;

import es.guerramod.GuerraMod;
import es.guerramod.ciudad.Edificio;
import es.guerramod.ciudad.Plano;
import es.guerramod.clima.Estaciones;
import es.guerramod.incendios.Fuego;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El trabajo de los aldeanos según el oficio de su DNI. Construyen casas cuando faltan camas, hacen huertos, calles y
 * farolas, apagan fuegos, defienden el pueblo y van a la guerra. El representante reparte los oficios que hagan falta.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Trabajos {
    private Trabajos() {}

    private static final ArrayDeque<Villager> NUEVOS = new ArrayDeque<>();
    private static final Map<UUID, Censo> CENSOS = new HashMap<>();
    private static final Map<UUID, Long> GOLPE = new HashMap<>();
    private static final Map<UUID, LivingEntity> OBJETIVO = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> RUTAS = new HashMap<>();
    private static final Map<UUID, Integer> FILA_ESCANEO = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> FUEGOS = new HashMap<>();
    private static final Map<UUID, BlockPos> FAROLA = new HashMap<>();

    /** Aldeanos de un pueblo, contados cada segundo. */
    public static final class Censo {
        public final List<Villager> todos = new ArrayList<>();
        public final List<Villager> adultos = new ArrayList<>();
        public final EnumMap<Rol, List<Villager>> roles = new EnumMap<>(Rol.class);
        public long camas;

        List<Villager> de(Rol r) {
            return roles.computeIfAbsent(r, k -> new ArrayList<>());
        }
    }

    public static Censo censo(UUID pueblo) {
        return CENSOS.getOrDefault(pueblo, new Censo());
    }

    // ====================================================================== eventos

    @SubscribeEvent
    public static void unirse(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Villager v)) return;
        if (e.getLevel().dimension() != Level.OVERWORLD) return;
        NUEVOS.add(v);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = e.getServer();
        ServerLevel l = s.overworld();
        for (int i = 0; i < 12 && !NUEVOS.isEmpty(); i++) {
            Villager v = NUEVOS.poll();
            if (v.isAlive() && v.level() == l) Dni.asegurar(l, v);
        }
        long t = l.getGameTime();
        if (t % 20 == 0) contar(l);
        if (t % 5 != 0) return;
        Pueblos ps = Pueblos.get(l);
        boolean dia = (l.getDayTime() % 24000L) < 12300L;
        for (Pueblos.Pueblo p : ps.todos()) {
            if (!l.isLoaded(p.centro) || l.getNearestPlayer(p.centro.getX(), p.centro.getY(), p.centro.getZ(), 220, false) == null) continue;
            Censo c = censo(p.id);
            try {
                if (dia) {
                    obras(l, p, c, t);
                    calles(l, p, c);
                    farolas(l, p, c);
                }
                bomberos(l, p, c);
                if (t % 20 == 0) planear(l, ps, p, c);
                if (t % 200 == 0) representante(l, p, c);
            } catch (Exception ex) {
                GuerraMod.LOGGER.warn("Pueblo {}: {}", p.nombre, ex.toString());
            }
        }
        if (t % 40 == 0) guerras(l, ps, t);
    }

    private static void contar(ServerLevel l) {
        CENSOS.clear();
        OBJETIVO.entrySet().removeIf(en -> !en.getValue().isAlive() || en.getValue().isRemoved());
        for (Villager v : l.getEntities(EntityType.VILLAGER, v -> v.isAlive() && Dni.tiene(v))) {
            UUID id = Dni.pueblo(v);
            if (id == null) continue;
            Censo c = CENSOS.computeIfAbsent(id, k -> new Censo());
            c.todos.add(v);
            if (!v.isBaby()) {
                c.adultos.add(v);
                c.de(Dni.rol(v)).add(v);
            }
        }
        Pueblos ps = Pueblos.get(l);
        for (Pueblos.Pueblo p : ps.todos()) {
            if (!l.isLoaded(p.centro)) continue;
            Censo c = CENSOS.computeIfAbsent(p.id, k -> new Censo());
            c.camas = l.getPoiManager().getCountInRange(h -> h.is(PoiTypes.HOME), p.centro, 110, PoiManager.Occupancy.ANY);
        }
    }

    // ====================================================================== moverse

    static void ir(Villager v, BlockPos destino, float velocidad) {
        Brain<Villager> b = v.getBrain();
        b.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(destino, velocidad, 2));
    }

    static boolean cerca(Villager v, BlockPos p, double d) {
        return v.distanceToSqr(Vec3.atBottomCenterOf(p)) < d * d;
    }

    private static void trabajar(Villager v) {
        if (v.tickCount % 3 == 0) v.swing(InteractionHand.MAIN_HAND);
    }

    // ====================================================================== constructores y agricultores

    private static void obras(ServerLevel l, Pueblos.Pueblo p, Censo c, long t) {
        if (p.obras.isEmpty()) return;
        List<Villager> cons = c.de(Rol.CONSTRUCTOR), agri = c.de(Rol.AGRICULTOR), elec = c.de(Rol.FAROLERO);
        List<Construccion.Obra> casas = new ArrayList<>(), huertos = new ArrayList<>(), arboles = new ArrayList<>();
        for (Construccion.Obra o : p.obras) {
            if (o.tipo.equals("huerto")) huertos.add(o);
            else if (o.tipo.equals("arbol")) arboles.add(o);
            else casas.add(o);
        }
        avanzar(l, p, casas, cons, t);
        avanzar(l, p, huertos, agri, t);
        avanzar(l, p, arboles, elec, t);
    }

    private static void avanzar(ServerLevel l, Pueblos.Pueblo p, List<Construccion.Obra> obras, List<Villager> gente, long t) {
        if (obras.isEmpty() || gente.isEmpty()) return;
        int[] presentes = new int[obras.size()];
        for (int i = 0; i < gente.size(); i++) {
            Villager v = gente.get(i);
            Construccion.Obra o = obras.get(i % obras.size());
            BlockPos c = o.centro();
            // cada uno a un lado de la obra
            int lado = (i / obras.size()) % 4;
            BlockPos sitio = switch (lado) {
                case 0 -> new BlockPos(o.caja.minX() - 1, o.origen.getY() + 1, c.getZ());
                case 1 -> new BlockPos(o.caja.maxX() + 1, o.origen.getY() + 1, c.getZ());
                case 2 -> new BlockPos(c.getX(), o.origen.getY() + 1, o.caja.minZ() - 1);
                default -> new BlockPos(c.getX(), o.origen.getY() + 1, o.caja.maxZ() + 1);
            };
            if (t % 20 == 0) ir(v, sitio, 0.6f);
            double dx = Math.max(Math.max(o.caja.minX() - v.getX(), v.getX() - o.caja.maxX()), 0);
            double dz = Math.max(Math.max(o.caja.minZ() - v.getZ(), v.getZ() - o.caja.maxZ()), 0);
            if (dx * dx + dz * dz < 64) {
                presentes[i % obras.size()]++;
                trabajar(v);
                v.getLookControl().setLookAt(Vec3.atCenterOf(c));
            }
        }
        for (int i = 0; i < obras.size(); i++) {
            if (presentes[i] == 0) continue;
            Construccion.Obra o = obras.get(i);
            if (Construccion.trabajar(l, o, 2 * presentes[i])) terminada(l, p, o);
        }
    }

    private static void terminada(ServerLevel l, Pueblos.Pueblo p, Construccion.Obra o) {
        p.obras.remove(o);
        String que;
        switch (o.tipo) {
            case "huerto" -> {
                p.huertos.add(o.origen.asLong());
                p.casas.add(o.caja);
                que = "un huerto nuevo";
            }
            case "arbol" -> {
                for (Construccion.Pieza pz : Construccion.piezas(l, o)) if (pz.st() != null && !pz.st().isAir()) p.luces.add(pz.pos().asLong());
                p.arbolNavidad = true;
                que = "el árbol de luces de Navidad";
            }
            default -> {
                p.casas.add(o.caja);
                que = o.tipo.equals("edificio") ? "un " + Edificio.valueOf(o.id).nombre.toLowerCase() : "una casa nueva";
            }
        }
        if (o.puerta != null && !o.tipo.equals("arbol")) p.puertas.add(o.puerta.asLong());
        Pueblos.get(l).setDirty();
        avisar(l, o.centro(), 120, Component.literal("🏗 " + p.nombre + ": han terminado " + que + " (" + p.casas.size() + " construcciones)")
                .withStyle(ChatFormatting.GREEN), false);
    }

    /** Decide qué hay que construir: casas si faltan camas, huertos para los agricultores, el árbol de Navidad... */
    private static void planear(ServerLevel l, Pueblos ps, Pueblos.Pueblo p, Censo c) {
        RandomSource r = l.random;
        int pob = c.todos.size();
        long casasEnObra = p.obras.stream().filter(o -> o.tipo.equals("plantilla") || o.tipo.equals("edificio")).count();
        int cons = c.de(Rol.CONSTRUCTOR).size();
        boolean faltanCamas = c.camas < pob + 2 + cons / 2;
        int maxObras = Math.min(4, Math.max(1, (cons + 2) / 3));
        if (cons > 0 && faltanCamas && casasEnObra < maxObras) {
            Construccion.Obra o = null;
            if (p.esCiudad(pob) && r.nextFloat() < 0.5f) {
                Edificio e = pob >= 45 && r.nextFloat() < 0.25f ? Edificio.RASCACIELOS : r.nextFloat() < 0.7f ? Edificio.BLOQUE_PISOS : Edificio.CASA;
                o = Construccion.planearEdificio(l, p, r, e);
            }
            if (o == null) o = Construccion.planearCasa(l, p, r);
            if (o != null) {
                p.obras.add(o);
                ps.setDirty();
                avisar(l, o.centro(), 96, Component.literal("🏗 " + p.nombre + ": empiezan una obra nueva (" + c.camas + " camas para " + pob + " vecinos)")
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
        int agri = c.de(Rol.AGRICULTOR).size();
        long huertosEnObra = p.obras.stream().filter(o -> o.tipo.equals("huerto")).count();
        if (agri > 0 && huertosEnObra == 0 && p.huertos.size() < Math.max(1, (agri + 1) / 2) + 1) {
            Construccion.Obra o = Construccion.planearHuerto(l, p, r);
            if (o != null) {
                p.obras.add(o);
                ps.setDirty();
            }
        }
        int dia = Estaciones.dia(l);
        boolean navidad = Estaciones.navidad(dia);
        if (navidad && !p.arbolNavidad && pob >= 4 && !c.de(Rol.FAROLERO).isEmpty()
                && p.obras.stream().noneMatch(o -> o.tipo.equals("arbol"))) {
            Construccion.Obra o = Construccion.planearArbol(l, p, r);
            if (o != null) {
                p.obras.add(o);
                ps.setDirty();
            }
        }
        if (!navidad) {
            p.obras.removeIf(o -> o.tipo.equals("arbol"));
            if (p.arbolNavidad && p.luces.isEmpty()) p.arbolNavidad = false;
        }
    }

    // ====================================================================== peones camineros

    private static boolean esCalle(BlockState s) {
        if (s.is(Blocks.DIRT_PATH)) return true;
        String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock()).getPath();
        return id.startsWith("asfalto") || id.startsWith("acera") || id.equals("adoquin_granito");
    }

    private static List<BlockPos> ruta(BlockPos desde, BlockPos hasta) {
        List<BlockPos> r = new ArrayList<>();
        int x = desde.getX(), z = desde.getZ();
        int sx = Integer.signum(hasta.getX() - x), sz = Integer.signum(hasta.getZ() - z);
        r.add(new BlockPos(x, 0, z));
        while (z != hasta.getZ()) {
            z += sz;
            r.add(new BlockPos(x, 0, z));
        }
        while (x != hasta.getX()) {
            x += sx;
            r.add(new BlockPos(x, 0, z));
        }
        return r;
    }

    private static void calles(ServerLevel l, Pueblos.Pueblo p, Censo c) {
        List<Villager> cam = c.de(Rol.CAMINERO);
        if (cam.isEmpty()) return;
        boolean ciudad = p.esCiudad(c.todos.size());
        List<BlockPos> r = RUTAS.get(p.id);
        if (r == null || r.isEmpty()) {
            if (!p.puertas.isEmpty()) {
                BlockPos puerta = BlockPos.of(p.puertas.removeLong(0));
                Pueblos.get(l).setDirty();
                r = ruta(puerta, p.centro);
                RUTAS.put(p.id, r);
            } else if (ciudad) {
                // en las ciudades, asfaltan los caminos de tierra
                for (int i = 0; i < 40 && !p.calles.isEmpty(); i++) {
                    BlockPos q = BlockPos.of(p.calles.getLong(l.random.nextInt(p.calles.size())));
                    if (l.isLoaded(q) && l.getBlockState(q).is(Blocks.DIRT_PATH)) {
                        Villager v = cam.get(0);
                        ir(v, q.above(), 0.6f);
                        if (cerca(v, q, 6)) {
                            l.setBlock(q, Plano.mod("asfalto", Blocks.GRAY_CONCRETE), Block.UPDATE_ALL);
                            trabajar(v);
                        }
                        break;
                    }
                }
                return;
            } else return;
        }
        for (Villager v : cam) {
            if (r.isEmpty()) break;
            BlockPos paso = r.get(0);
            int y = Construccion.alturaSuelo(l, paso.getX(), paso.getZ());
            BlockPos donde = new BlockPos(paso.getX(), y == Integer.MIN_VALUE ? v.getBlockY() : y + 1, paso.getZ());
            ir(v, donde, 0.6f);
            if (!cerca(v, donde, 5)) continue;
            trabajar(v);
            r.remove(0);
            boolean ejeZ = r.isEmpty() || r.get(0).getX() == paso.getX();
            int ancho = ciudad ? 2 : 1;
            for (int k = -ancho; k <= ancho; k++) {
                int x = paso.getX() + (ejeZ ? k : 0), z = paso.getZ() + (ejeZ ? 0 : k);
                pavimentar(l, p, x, z, ciudad, Math.abs(k) == ancho && ciudad);
            }
        }
    }

    private static void pavimentar(ServerLevel l, Pueblos.Pueblo p, int x, int z, boolean ciudad, boolean acera) {
        if (!Construccion.cargado(l, x, z)) return;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos q = new BlockPos(x, y, z);
        BlockState s = l.getBlockState(q);
        if (esCalle(s)) {
            if (!ciudad || !s.is(Blocks.DIRT_PATH)) return;
        } else {
            boolean tierra = s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)
                    || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.MUD) || s.is(Blocks.CLAY);
            if (!tierra) return;
        }
        BlockState nuevo = ciudad ? (acera ? Plano.mod("acera", Blocks.SMOOTH_STONE) : Plano.mod("asfalto", Blocks.GRAY_CONCRETE))
                : Blocks.DIRT_PATH.defaultBlockState();
        l.setBlock(q, nuevo, Block.UPDATE_ALL);
        BlockState arriba = l.getBlockState(q.above());
        if (!arriba.isAir() && arriba.canBeReplaced() && arriba.getFluidState().isEmpty()) l.setBlock(q.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (p.calles.size() < 8000) p.calles.add(q.asLong());
    }

    // ====================================================================== electricistas: farolas y luces de Navidad

    private static boolean farolaCerca(Pueblos.Pueblo p, BlockPos q, int d) {
        for (int i = 0; i < p.farolas.size(); i++) if (BlockPos.of(p.farolas.getLong(i)).distSqr(q) < d * d) return true;
        return false;
    }

    private static void farolas(ServerLevel l, Pueblos.Pueblo p, Censo c) {
        List<Villager> el = c.de(Rol.FAROLERO);
        if (el.isEmpty()) return;
        boolean navidad = Estaciones.navidad(Estaciones.dia(l));
        // fuera de Navidad desmontan las luces
        if (!navidad && !p.luces.isEmpty()) {
            Villager v = el.get(0);
            for (int k = 0; k < 6 && !p.luces.isEmpty(); k++) {
                BlockPos q = BlockPos.of(p.luces.removeLong(p.luces.size() - 1));
                if (!l.isLoaded(q)) continue;
                String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(l.getBlockState(q).getBlock()).getPath();
                if (id.contains("navidad") || id.contains("luces") || id.equals("adorno_farola")) l.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                ir(v, q, 0.6f);
            }
            trabajar(v);
            if (p.luces.isEmpty()) p.arbolNavidad = false;
            Pueblos.get(l).setDirty();
            return;
        }
        if (p.obras.stream().anyMatch(o -> o.tipo.equals("arbol"))) return; // primero montan el árbol
        boolean ciudad = p.esCiudad(c.todos.size());
        for (Villager v : el) {
            BlockPos obj = FAROLA.get(v.getUUID());
            if (obj == null) {
                obj = buscarSitioFarola(l, p, navidad);
                if (obj == null) continue;
                FAROLA.put(v.getUUID(), obj);
            }
            ir(v, obj.above(), 0.6f);
            if (!cerca(v, obj, 4)) {
                if (l.random.nextInt(200) == 0) FAROLA.remove(v.getUUID()); // inalcanzable
                continue;
            }
            trabajar(v);
            FAROLA.remove(v.getUUID());
            if (farolaCerca(p, obj, 3)) {
                // es una farola: le pone el adorno de Navidad
                ponerAdorno(l, p, obj);
            } else {
                ponerFarola(l, p, obj, ciudad);
            }
        }
    }

    /** Un sitio junto a una calle, a más de 10 bloques de otra farola. En Navidad, también farolas sin adornar. */
    private static BlockPos buscarSitioFarola(ServerLevel l, Pueblos.Pueblo p, boolean navidad) {
        RandomSource r = l.random;
        if (navidad && !p.farolas.isEmpty() && r.nextBoolean()) {
            BlockPos f = BlockPos.of(p.farolas.getLong(r.nextInt(p.farolas.size())));
            if (!p.luces.contains(f.above(3).asLong())) return f;
        }
        for (int i = 0; i < 30 && !p.calles.isEmpty(); i++) {
            BlockPos q = BlockPos.of(p.calles.getLong(r.nextInt(p.calles.size())));
            if (!l.isLoaded(q) || farolaCerca(p, q, 10)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int x = q.getX() + d.getStepX(), z = q.getZ() + d.getStepZ();
                if (!Construccion.cargado(l, x, z)) continue;
                int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos s = new BlockPos(x, y, z);
                BlockState st = l.getBlockState(s);
                if (esCalle(st) || !st.isSolid() || !st.getFluidState().isEmpty()) continue;
                if (Math.abs(y - q.getY()) > 1) continue;
                boolean libre = true;
                for (int k = 1; k <= 5; k++) if (!l.getBlockState(s.above(k)).canBeReplaced()) libre = false;
                if (libre) return s;
            }
        }
        return null;
    }

    private static void ponerFarola(ServerLevel l, Pueblos.Pueblo p, BlockPos suelo, boolean ciudad) {
        if (ciudad) {
            BlockState poste = Plano.mod("poste_senal", Blocks.IRON_BARS);
            for (int k = 1; k <= 3; k++) l.setBlock(suelo.above(k), poste, Block.UPDATE_ALL);
            l.setBlock(suelo.above(4), Plano.mod("farola_fernandina", Blocks.LANTERN), Block.UPDATE_ALL);
        } else {
            l.setBlock(suelo.above(1), Blocks.SPRUCE_FENCE.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(suelo.above(2), Blocks.SPRUCE_FENCE.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(suelo.above(3), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        }
        l.playSound(null, suelo, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1f, 1f);
        p.farolas.add(suelo.asLong());
        Pueblos.get(l).setDirty();
    }

    private static void ponerAdorno(ServerLevel l, Pueblos.Pueblo p, BlockPos suelo) {
        BlockPos q = suelo.above(3);
        // el adorno cuelga del poste, por el lado de la calle
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos a = q.relative(d);
            BlockState abajo = l.getBlockState(new BlockPos(a.getX(), suelo.getY(), a.getZ()));
            if (!esCalle(abajo) || !l.getBlockState(a).isAir()) continue;
            BlockState adorno = Plano.mod("adorno_farola", Blocks.SEA_LANTERN);
            if (adorno.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                adorno = adorno.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, d);
            }
            l.setBlock(a, adorno, Block.UPDATE_ALL);
            p.luces.add(a.asLong());
            break;
        }
        p.luces.add(q.asLong()); // marca: esta farola ya está adornada
        Pueblos.get(l).setDirty();
    }

    // ====================================================================== bomberos

    private static void bomberos(ServerLevel l, Pueblos.Pueblo p, Censo c) {
        List<Villager> bom = c.de(Rol.BOMBERO);
        if (bom.isEmpty()) return;
        List<BlockPos> fuegos = FUEGOS.computeIfAbsent(p.id, k -> new ArrayList<>());
        // escanea el pueblo poco a poco (radio 48) buscando llamas en la superficie
        int fila = FILA_ESCANEO.getOrDefault(p.id, 0);
        int R = 48;
        for (int k = 0; k < 12; k++) {
            int z = p.centro.getZ() - R + fila;
            for (int x = p.centro.getX() - R; x <= p.centro.getX() + R; x++) {
                if (!Construccion.cargado(l, x, z)) continue;
                int h = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int y = h - 2; y <= h + 1; y++) {
                    BlockPos q = new BlockPos(x, y, z);
                    if (Fuego.esFuego(l.getBlockState(q)) && fuegos.size() < 24 && !fuegos.contains(q)) fuegos.add(q);
                }
            }
            fila = (fila + 1) % (2 * R + 1);
        }
        FILA_ESCANEO.put(p.id, fila);
        fuegos.removeIf(q -> !l.isLoaded(q) || !Fuego.esFuego(l.getBlockState(q)));
        if (fuegos.isEmpty()) return;
        for (Villager v : bom) {
            BlockPos mejor = null;
            double md = Double.MAX_VALUE;
            for (BlockPos q : fuegos) {
                double d = v.distanceToSqr(Vec3.atCenterOf(q));
                if (d < md) {
                    md = d;
                    mejor = q;
                }
            }
            if (mejor == null) break;
            ir(v, mejor, 0.75f);
            if (md < 7 * 7) {
                v.getLookControl().setLookAt(Vec3.atCenterOf(mejor));
                Vec3 o = v.getEyePosition(), d = Vec3.atCenterOf(mejor).subtract(o);
                for (int i = 1; i <= 6; i++) {
                    Vec3 pp = o.add(d.scale(i / 6.0));
                    l.sendParticles(ParticleTypes.SPLASH, pp.x, pp.y, pp.z, 3, 0.1, 0.1, 0.1, 0.05);
                }
                l.sendParticles(ParticleTypes.CLOUD, mejor.getX() + 0.5, mejor.getY() + 0.6, mejor.getZ() + 0.5, 6, 0.6, 0.3, 0.6, 0.02);
                Fuego.apagarZona(l, Vec3.atCenterOf(mejor), 3.5, 600);
                trabajar(v);
            }
        }
    }

    // ====================================================================== representante

    private static void representante(ServerLevel l, Pueblos.Pueblo p, Censo c) {
        if (c.de(Rol.REPRESENTANTE).isEmpty()) return;
        List<Villager> libres = new ArrayList<>(c.de(Rol.NINGUNO));
        if (libres.isEmpty()) return;
        int pob = c.adultos.size();
        boolean guerra = p.enemigo != null;
        EnumMap<Rol, Integer> objetivo = new EnumMap<>(Rol.class);
        boolean faltanCamas = c.camas < c.todos.size() + 2;
        objetivo.put(Rol.CONSTRUCTOR, faltanCamas ? Math.max(1, pob / 5) : (p.esCiudad(pob) ? 1 : 0));
        objetivo.put(Rol.AGRICULTOR, Math.max(1, pob / 5));
        objetivo.put(Rol.DEFENSA, guerra ? Math.max(2, pob / 3) : Math.max(1, pob / 7));
        objetivo.put(Rol.CAMINERO, pob >= 4 ? 1 + pob / 25 : 0);
        objetivo.put(Rol.FAROLERO, pob >= 6 ? 1 + pob / 30 : 0);
        objetivo.put(Rol.BOMBERO, pob >= 7 ? 1 + pob / 20 : 0);
        objetivo.put(Rol.VENDEDOR, pob >= 5 ? Math.max(1, pob / 6) : 0);
        objetivo.put(Rol.POLICIA, pob >= 8 ? 1 + pob / 12 : 0);
        Rol[] prioridad = guerra
                ? new Rol[]{Rol.DEFENSA, Rol.CONSTRUCTOR, Rol.AGRICULTOR, Rol.BOMBERO, Rol.CAMINERO, Rol.FAROLERO, Rol.POLICIA, Rol.VENDEDOR}
                : new Rol[]{Rol.CONSTRUCTOR, Rol.AGRICULTOR, Rol.DEFENSA, Rol.CAMINERO, Rol.BOMBERO, Rol.POLICIA, Rol.FAROLERO, Rol.VENDEDOR};
        Villager rep = c.de(Rol.REPRESENTANTE).get(0);
        for (Rol r : prioridad) {
            int falta = objetivo.get(r) - c.de(r).size();
            while (falta-- > 0 && !libres.isEmpty()) {
                Villager v = libres.remove(libres.size() - 1);
                asignar(l, v, r);
                c.de(Rol.NINGUNO).remove(v);
                c.de(r).add(v);
                avisar(l, rep.blockPosition(), 48, Component.literal("🗳 " + rep.getName().getString() + " (representante de " + p.nombre
                        + ") nombra a " + Dni.nombreCompleto(Dni.datos(v)) + ": ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(r.nombre).withStyle(s -> s.withColor(r.color))), false);
            }
        }
        // lo que sobra, al comercio
        while (!libres.isEmpty() && pob >= 3) {
            Villager v = libres.remove(libres.size() - 1);
            asignar(l, v, Rol.VENDEDOR);
        }
    }

    // ====================================================================== asignar oficio

    private static final VillagerProfession[] COMERCIOS = {VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CARTOGRAPHER,
            VillagerProfession.CLERIC, VillagerProfession.FISHERMAN, VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER,
            VillagerProfession.LIBRARIAN, VillagerProfession.MASON, VillagerProfession.SHEPHERD, VillagerProfession.TOOLSMITH,
            VillagerProfession.WEAPONSMITH};

    private static Block puesto(VillagerProfession p) {
        if (p == VillagerProfession.ARMORER) return Blocks.BLAST_FURNACE;
        if (p == VillagerProfession.BUTCHER) return Blocks.SMOKER;
        if (p == VillagerProfession.CARTOGRAPHER) return Blocks.CARTOGRAPHY_TABLE;
        if (p == VillagerProfession.CLERIC) return Blocks.BREWING_STAND;
        if (p == VillagerProfession.FISHERMAN) return Blocks.BARREL;
        if (p == VillagerProfession.FLETCHER) return Blocks.FLETCHING_TABLE;
        if (p == VillagerProfession.LEATHERWORKER) return Blocks.CAULDRON;
        if (p == VillagerProfession.LIBRARIAN) return Blocks.LECTERN;
        if (p == VillagerProfession.MASON) return Blocks.STONECUTTER;
        if (p == VillagerProfession.SHEPHERD) return Blocks.LOOM;
        if (p == VillagerProfession.TOOLSMITH) return Blocks.SMITHING_TABLE;
        if (p == VillagerProfession.WEAPONSMITH) return Blocks.GRINDSTONE;
        return Blocks.COMPOSTER;
    }

    /** Pone el puesto de trabajo en la plaza del mercado (alrededor del centro del pueblo). */
    private static void ponerPuesto(ServerLevel l, Pueblos.Pueblo p, Block b) {
        RandomSource r = l.random;
        for (int i = 0; i < 30; i++) {
            double ang = r.nextDouble() * Math.PI * 2, rad = 4 + r.nextDouble() * 9;
            int x = p.centro.getX() + (int) Math.round(Math.cos(ang) * rad), z = p.centro.getZ() + (int) Math.round(Math.sin(ang) * rad);
            if (!Construccion.cargado(l, x, z)) continue;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos q = new BlockPos(x, y, z);
            if (!l.getBlockState(q.below()).isSolid() || !l.getBlockState(q).canBeReplaced() || !l.getBlockState(q.above()).isAir()) continue;
            l.setBlock(q, b.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
    }

    public static void asignar(ServerLevel l, Villager v, Rol r) {
        Rol antes = Dni.rol(v);
        Dni.ponerRol(v, r);
        CompoundTagNombre.poner(v, r);
        // herramienta en la mano (los aldeanos la llevan con los brazos cruzados)
        ItemStack mano = switch (r) {
            case DEFENSA -> new ItemStack(Items.IRON_SWORD);
            case POLICIA -> new ItemStack(es.guerramod.policia.ItemsPolicia.PORRA.get());
            case CONSTRUCTOR -> new ItemStack(Items.IRON_SHOVEL);
            case AGRICULTOR -> new ItemStack(Items.IRON_HOE);
            case CAMINERO -> new ItemStack(Items.IRON_PICKAXE);
            case FAROLERO -> new ItemStack(Items.LANTERN);
            case BOMBERO -> new ItemStack(Items.WATER_BUCKET);
            case REPRESENTANTE -> new ItemStack(Items.WRITABLE_BOOK);
            default -> ItemStack.EMPTY;
        };
        v.setItemSlot(EquipmentSlot.MAINHAND, mano);
        v.setDropChance(EquipmentSlot.MAINHAND, 0f);
        VillagerData d = v.getVillagerData();
        if (r == Rol.AGRICULTOR && d.getProfession() != VillagerProfession.FARMER) {
            v.setVillagerData(d.setProfession(VillagerProfession.FARMER));
            v.setVillagerXp(Math.max(1, v.getVillagerXp()));
        } else if (r == Rol.VENDEDOR && (d.getProfession() == VillagerProfession.NONE || d.getProfession() == VillagerProfession.NITWIT
                || d.getProfession() == VillagerProfession.FARMER)) {
            VillagerProfession pr = COMERCIOS[l.random.nextInt(COMERCIOS.length)];
            v.setVillagerData(d.setProfession(pr));
            v.setVillagerXp(Math.max(1, v.getVillagerXp()));
            Pueblos.Pueblo pu = Pueblos.get(l).pueblo(Dni.pueblo(v));
            if (pu != null) ponerPuesto(l, pu, puesto(pr));
        } else if (antes != r && (antes == Rol.AGRICULTOR || antes == Rol.VENDEDOR) && v.getVillagerXp() <= 1) {
            v.setVillagerData(d.setProfession(VillagerProfession.NONE));
            v.setVillagerXp(0);
        }
        OBJETIVO.remove(v.getUUID());
        FAROLA.remove(v.getUUID());
        v.playCelebrateSound();
    }

    /** El nombre que se ve encima de la cabeza: nombre, apellido y oficio. */
    static final class CompoundTagNombre {
        static void poner(Villager v, Rol r) {
            var t = Dni.datos(v);
            MutableComponent n = Component.literal(t.getString("nombre") + " " + t.getString("ap1"));
            if (r != Rol.NINGUNO) n.append(Component.literal(" · " + r.nombre).withStyle(s -> s.withColor(r.color)));
            v.setCustomName(n);
        }
    }

    // ====================================================================== defensa y guerra

    @SubscribeEvent
    public static void vivir(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof Villager v) || v.level().isClientSide() || v.isBaby()) return;
        Rol rol = Dni.rol(v);
        if (rol != Rol.DEFENSA && rol != Rol.POLICIA) return;
        boolean policia = rol == Rol.POLICIA;
        ServerLevel l = (ServerLevel) v.level();
        Pueblos.Pueblo p = Pueblos.get(l).pueblo(Dni.pueblo(v));
        if (p == null) return;
        LivingEntity obj = OBJETIVO.get(v.getUUID());
        if (v.tickCount % 10 == 0 || (obj != null && !obj.isAlive())) {
            obj = buscarObjetivo(l, v, p, policia);
            if (obj == null) OBJETIVO.remove(v.getUUID());
            else OBJETIVO.put(v.getUUID(), obj);
        }
        Brain<Villager> b = v.getBrain();
        if (obj != null) {
            // un soldado no huye: olvida el miedo y va a por el enemigo
            b.eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
            b.eraseMemory(MemoryModuleType.HURT_BY);
            b.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            b.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(obj, 0.75f, 1));
            v.getLookControl().setLookAt(obj);
            long ahora = l.getGameTime();
            if (v.distanceToSqr(obj) < 2.6 * 2.6 && ahora - GOLPE.getOrDefault(v.getUUID(), 0L) >= 18) {
                GOLPE.put(v.getUUID(), ahora);
                v.swing(InteractionHand.MAIN_HAND);
                float dano = v.getMainHandItem().is(Items.IRON_SWORD) ? 6f : policia ? 5f : 3f;
                if (policia) obj.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
                if (obj.hurt(v.damageSources().mobAttack(v), dano)) {
                    obj.knockback(0.45, v.getX() - obj.getX(), v.getZ() - obj.getZ());
                    l.playSound(null, obj.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8f, 1f);
                }
            }
        } else if (policia) {
            // patrulla: de calle en calle por el pueblo (de día)
            if (v.tickCount % 160 == 0 && (l.getDayTime() % 24000L) < 12300L) {
                BlockPos destino;
                if (!p.calles.isEmpty()) destino = BlockPos.of(p.calles.getLong(l.random.nextInt(p.calles.size()))).above();
                else destino = p.centro.offset(l.random.nextInt(41) - 20, 0, l.random.nextInt(41) - 20);
                if (Construccion.cargado(l, destino.getX(), destino.getZ())) ir(v, destino, 0.55f);
            }
        } else if (p.enemigo != null && v.tickCount % 20 == 0) {
            // en guerra: marchan hacia el pueblo enemigo por etapas
            Pueblos.Pueblo en = Pueblos.get(l).pueblo(p.enemigo);
            if (en != null) {
                Vec3 dir = Vec3.atCenterOf(en.centro).subtract(v.position());
                double dist = dir.horizontalDistance();
                if (dist > 6) {
                    Vec3 paso = v.position().add(dir.normalize().scale(Math.min(20, dist)));
                    int x = (int) Math.floor(paso.x), z = (int) Math.floor(paso.z);
                    if (Construccion.cargado(l, x, z)) {
                        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        b.eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
                        b.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPos(x, y, z), 0.7f, 2));
                    }
                }
            }
        } else if (v.tickCount % 100 == 0 && v.distanceToSqr(Vec3.atCenterOf(p.centro)) > 50 * 50) {
            ir(v, p.centro, 0.6f); // vuelve a casa
        }
    }

    private static LivingEntity buscarObjetivo(ServerLevel l, Villager v, Pueblos.Pueblo p, boolean policia) {
        LivingEntity mejor = null;
        double md = Double.MAX_VALUE;
        AABB caja = v.getBoundingBox().inflate(18, 6, 18);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, caja, e -> e.isAlive() && e != v)) {
            boolean enemigo = false;
            if (e instanceof Enemy && !(e instanceof Creeper)) enemigo = e.distanceToSqr(Vec3.atCenterOf(p.centro)) < 70 * 70 || p.enemigo != null;
            else if (!policia && p.enemigo != null && e instanceof Villager o && Dni.tiene(o) && p.enemigo.equals(Dni.pueblo(o)) && !o.isBaby()) {
                enemigo = Dni.rol(o) == Rol.DEFENSA || o.distanceToSqr(Vec3.atCenterOf(p.centro)) > 60 * 60;
            }
            if (!enemigo) continue;
            double d = v.distanceToSqr(e);
            if (d < md) {
                md = d;
                mejor = e;
            }
        }
        return mejor;
    }

    /** Declara la guerra entre dos pueblos: los dos reclutan soldados y salen a pelear. */
    public static void declararGuerra(ServerLevel l, Pueblos.Pueblo a, Pueblos.Pueblo b) {
        a.enemigo = b.id;
        b.enemigo = a.id;
        a.inicioGuerra = b.inicioGuerra = l.getGameTime();
        for (Pueblos.Pueblo p : new Pueblos.Pueblo[]{a, b}) {
            Censo c = censo(p.id);
            int quiere = Math.max(2, c.adultos.size() / 3);
            Rol[] orden = {Rol.NINGUNO, Rol.VENDEDOR, Rol.CAMINERO, Rol.AGRICULTOR, Rol.CONSTRUCTOR};
            int tiene = c.de(Rol.DEFENSA).size();
            for (Rol r : orden) {
                for (Villager v : new ArrayList<>(c.de(r))) {
                    if (tiene >= quiere) break;
                    asignar(l, v, Rol.DEFENSA);
                    tiene++;
                }
            }
        }
        Pueblos.get(l).setDirty();
        avisarTodos(l, Component.literal("⚔ ¡GUERRA! " + a.nombre + " declara la guerra a " + b.nombre).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        l.playSound(null, b.centro, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 64f, 1f);
        l.playSound(null, a.centro, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 64f, 1f);
    }

    public static void paz(ServerLevel l, Pueblos.Pueblo a) {
        Pueblos ps = Pueblos.get(l);
        Pueblos.Pueblo b = ps.pueblo(a.enemigo);
        a.enemigo = null;
        if (b != null) b.enemigo = null;
        ps.setDirty();
        avisarTodos(l, Component.literal("🕊 Paz firmada entre " + a.nombre + (b != null ? " y " + b.nombre : "")).withStyle(ChatFormatting.AQUA));
    }

    private static void guerras(ServerLevel l, Pueblos ps, long t) {
        for (Pueblos.Pueblo a : ps.todos()) {
            if (a.enemigo == null) continue;
            Pueblos.Pueblo b = ps.pueblo(a.enemigo);
            if (b == null || !a.id.equals(b.enemigo)) {
                a.enemigo = null;
                ps.setDirty();
                continue;
            }
            if (a.id.compareTo(b.id) > 0) continue; // cada guerra una vez
            if (!l.isLoaded(a.centro) || !l.isLoaded(b.centro) || t - a.inicioGuerra < 600) continue;
            int sa = (int) censo(a.id).de(Rol.DEFENSA).stream().filter(LivingEntity::isAlive).count();
            int sb = (int) censo(b.id).de(Rol.DEFENSA).stream().filter(LivingEntity::isAlive).count();
            if (sa > 0 && sb > 0) continue;
            if (sa == 0 && sb == 0) {
                paz(l, a);
                continue;
            }
            Pueblos.Pueblo gana = sa > 0 ? a : b, pierde = sa > 0 ? b : a;
            gana.enemigo = pierde.enemigo = null;
            pierde.pertenece = gana.id;
            gana.victorias++;
            ps.setDirty();
            avisarTodos(l, Component.literal("🏳 " + pierde.nombre + " se rinde. Ahora pertenece a " + gana.nombre + " (" + gana.victorias
                    + (gana.victorias == 1 ? " victoria)" : " victorias)")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (ServerPlayer pl : l.players()) {
                if (pl.distanceToSqr(Vec3.atCenterOf(gana.centro)) < 250 * 250 || pl.distanceToSqr(Vec3.atCenterOf(pierde.centro)) < 250 * 250) {
                    pl.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                            Component.literal("Victoria de " + gana.nombre).withStyle(ChatFormatting.GOLD)));
                    pl.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                            Component.literal(pierde.nombre + " se rinde")));
                }
            }
            l.playSound(null, gana.centro, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 8f, 1f);
        }
    }

    // ====================================================================== avisos

    static void avisar(ServerLevel l, BlockPos donde, double radio, Component msg, boolean barra) {
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(Vec3.atCenterOf(donde)) < radio * radio) {
                if (barra) p.displayClientMessage(msg, true);
                else p.sendSystemMessage(msg);
            }
        }
    }

    static void avisarTodos(ServerLevel l, Component msg) {
        for (ServerPlayer p : l.getServer().getPlayerList().getPlayers()) p.sendSystemMessage(msg);
    }

    /** Limpia lo que se queda colgando cuando se descargan aldeanos. */
    public static void olvidar(UUID aldeano) {
        OBJETIVO.remove(aldeano);
        GOLPE.remove(aldeano);
        FAROLA.remove(aldeano);
    }
}
