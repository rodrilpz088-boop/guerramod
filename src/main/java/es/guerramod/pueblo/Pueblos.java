package es.guerramod.pueblo;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pueblos con su nombre, su centro, sus casas, calles, farolas, huertos y su situación (paz o guerra). */
public final class Pueblos extends SavedData {
    private static final String[] PRE = {"Villanueva", "San Martín", "Santa María", "Torrejón", "Castillejo", "Fuentelapeña", "Valdemoro",
            "Puebla", "Navalcarnero", "Arroyomolinos", "Casarrubios", "Villaverde", "Aldeanueva", "Robledo", "Valverde", "Torrelaguna",
            "Mirabueno", "Peñalba", "Cabanillas", "Pozuelo"};
    private static final String[] SUF = {"del Río", "de la Sierra", "del Monte", "de Arriba", "del Campo", "de la Vega", "del Valle",
            "de los Pinos", "de Abajo", "del Rey", "de Henares", "del Tajo", "de la Jara", "de Gredos"};

    public static final class Pueblo {
        public UUID id;
        public String nombre;
        public BlockPos centro;
        public final List<BoundingBox> casas = new ArrayList<>();
        public final LongArrayList calles = new LongArrayList();
        public final LongArrayList farolas = new LongArrayList();
        public final LongArrayList luces = new LongArrayList();
        public final LongArrayList huertos = new LongArrayList();
        public final LongArrayList puertas = new LongArrayList();
        public final List<Construccion.Obra> obras = new ArrayList<>();
        public UUID enemigo = null;
        public UUID pertenece = null;
        public long inicioGuerra = 0;
        public int victorias = 0;
        public boolean arbolNavidad = false;

        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            t.putString("nombre", nombre);
            t.putLong("centro", centro.asLong());
            ListTag lc = new ListTag();
            for (BoundingBox b : casas) {
                CompoundTag c = new CompoundTag();
                c.putIntArray("caja", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
                lc.add(c);
            }
            t.put("casas", lc);
            t.put("calles", new LongArrayTag(calles.toLongArray()));
            t.put("farolas", new LongArrayTag(farolas.toLongArray()));
            t.put("luces", new LongArrayTag(luces.toLongArray()));
            t.put("huertos", new LongArrayTag(huertos.toLongArray()));
            t.put("puertas", new LongArrayTag(puertas.toLongArray()));
            ListTag lo = new ListTag();
            for (Construccion.Obra o : obras) lo.add(o.guardar());
            t.put("obras", lo);
            if (enemigo != null) t.putUUID("enemigo", enemigo);
            if (pertenece != null) t.putUUID("pertenece", pertenece);
            t.putLong("inicioGuerra", inicioGuerra);
            t.putInt("victorias", victorias);
            t.putBoolean("arbol", arbolNavidad);
            return t;
        }

        static Pueblo cargar(CompoundTag t) {
            Pueblo p = new Pueblo();
            p.id = t.getUUID("id");
            p.nombre = t.getString("nombre");
            p.centro = BlockPos.of(t.getLong("centro"));
            for (Tag e : t.getList("casas", Tag.TAG_COMPOUND)) {
                int[] a = ((CompoundTag) e).getIntArray("caja");
                if (a.length == 6) p.casas.add(new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5]));
            }
            for (long l : t.getLongArray("calles")) p.calles.add(l);
            for (long l : t.getLongArray("farolas")) p.farolas.add(l);
            for (long l : t.getLongArray("luces")) p.luces.add(l);
            for (long l : t.getLongArray("huertos")) p.huertos.add(l);
            for (long l : t.getLongArray("puertas")) p.puertas.add(l);
            for (Tag e : t.getList("obras", Tag.TAG_COMPOUND)) p.obras.add(Construccion.Obra.cargar((CompoundTag) e));
            if (t.hasUUID("enemigo")) p.enemigo = t.getUUID("enemigo");
            if (t.hasUUID("pertenece")) p.pertenece = t.getUUID("pertenece");
            p.inicioGuerra = t.getLong("inicioGuerra");
            p.victorias = t.getInt("victorias");
            p.arbolNavidad = t.getBoolean("arbol");
            return p;
        }

        public boolean ocupado(BoundingBox b) {
            for (BoundingBox c : casas) if (c.inflatedBy(3).intersects(b)) return true;
            for (Construccion.Obra o : obras) if (o.caja.inflatedBy(3).intersects(b)) return true;
            return false;
        }

        public boolean esCiudad(int poblacion) {
            return poblacion >= 16 || casas.size() >= 14;
        }
    }

    private final Map<UUID, Pueblo> pueblos = new LinkedHashMap<>();

    public static Pueblos get(ServerLevel l) {
        return l.getServer().overworld().getDataStorage().computeIfAbsent(Pueblos::cargar, Pueblos::new, "guerramod_pueblos");
    }

    static Pueblos cargar(CompoundTag t) {
        Pueblos ps = new Pueblos();
        for (Tag e : t.getList("pueblos", Tag.TAG_COMPOUND)) {
            Pueblo p = Pueblo.cargar((CompoundTag) e);
            ps.pueblos.put(p.id, p);
        }
        return ps;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag l = new ListTag();
        for (Pueblo p : pueblos.values()) l.add(p.guardar());
        t.put("pueblos", l);
        return t;
    }

    public Pueblo pueblo(UUID id) {
        return id == null ? null : pueblos.get(id);
    }

    public List<Pueblo> todos() {
        return new ArrayList<>(pueblos.values());
    }

    public Pueblo cercano(BlockPos pos, double radio) {
        Pueblo mejor = null;
        double md = radio * radio;
        for (Pueblo p : pueblos.values()) {
            double d = p.centro.distSqr(pos);
            if (d < md) {
                md = d;
                mejor = p;
            }
        }
        return mejor;
    }

    public Pueblo cercanoOCrear(ServerLevel l, BlockPos pos) {
        Pueblo p = cercano(pos, 110);
        if (p != null) return p;
        p = new Pueblo();
        p.id = UUID.randomUUID();
        RandomSource r = l.random;
        p.nombre = PRE[r.nextInt(PRE.length)] + " " + SUF[r.nextInt(SUF.length)];
        // el centro: la campana del pueblo si hay una cerca
        BlockPos centro = pos;
        var poi = l.getPoiManager().findClosest(h -> h.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING), pos, 64,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        if (poi.isPresent()) centro = poi.get();
        p.centro = centro;
        pueblos.put(p.id, p);
        setDirty();
        return p;
    }

    public void quitar(UUID id) {
        pueblos.remove(id);
        setDirty();
    }
}
