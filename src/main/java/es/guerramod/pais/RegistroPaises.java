package es.guerramod.pais;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.*;

/**
 * Todos los países y los chunks que reclama cada uno.
 * El servidor guarda la copia buena (DatosPaises) y el cliente recibe una copia (CLIENTE) para el minimapa.
 * Los territorios solo existen en el Overworld.
 */
public class RegistroPaises {
    /** Copia en el cliente, la rellena el paquete de sincronización. */
    public static RegistroPaises CLIENTE = new RegistroPaises();

    public final Map<String, Pais> paises = new LinkedHashMap<>();
    public final Map<Long, String> territorio = new HashMap<>();

    /** Devuelve el registro correcto según el lado (cliente o servidor). */
    public static RegistroPaises de(Level level) {
        if (level.isClientSide) return CLIENTE;
        return DatosPaises.get(level.getServer()).registro;
    }

    public static RegistroPaises servidor() {
        return DatosPaises.get(ServerLifecycleHooks.getCurrentServer()).registro;
    }

    public static String crearId(String nombre) {
        String s = Normalizer.normalize(nombre.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "pais" : s;
    }

    @Nullable
    public Pais get(String id) {
        return paises.get(id);
    }

    @Nullable
    public Pais paisDeJugador(UUID jugador) {
        for (Pais p : paises.values()) if (p.esMiembro(jugador)) return p;
        return null;
    }

    @Nullable
    public Pais duenoChunk(long chunk) {
        String id = territorio.get(chunk);
        return id == null ? null : paises.get(id);
    }

    @Nullable
    public Pais dueno(Level level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return null;
        return duenoChunk(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    public int numChunks(String id) {
        int n = 0;
        for (String s : territorio.values()) if (s.equals(id)) n++;
        return n;
    }

    public boolean enGuerra(@Nullable Pais a, @Nullable Pais b) {
        return a != null && b != null && a.enGuerraCon(b.id);
    }

    /**
     * Si un jugador (o nadie) del país "atacante" puede romper bloques en esa posición.
     * Se puede en zonas libres, en tu propio país y en países con los que estás en guerra.
     */
    public boolean puedeAfectar(@Nullable Pais atacante, Level level, BlockPos pos) {
        Pais dueno = dueno(level, pos);
        if (dueno == null) return true;
        if (atacante == null) return false;
        return atacante == dueno || atacante.enGuerraCon(dueno.id);
    }

    public void borrarPais(String id) {
        Pais p = paises.remove(id);
        territorio.values().removeIf(s -> s.equals(id));
        for (Pais o : paises.values()) {
            o.enemigos.remove(id);
            o.ofertasPaz.remove(id);
        }
    }

    public CompoundTag guardar() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Pais p : paises.values()) l.add(p.guardar());
        t.put("paises", l);
        // territorio: lista paralela de chunks y país
        ListTag terr = new ListTag();
        Map<String, List<Long>> porPais = new HashMap<>();
        for (Map.Entry<Long, String> e : territorio.entrySet())
            porPais.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        for (Map.Entry<String, List<Long>> e : porPais.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("pais", e.getKey());
            long[] arr = new long[e.getValue().size()];
            for (int i = 0; i < arr.length; i++) arr[i] = e.getValue().get(i);
            c.putLongArray("chunks", arr);
            terr.add(c);
        }
        t.put("territorio", terr);
        return t;
    }

    public static RegistroPaises cargar(CompoundTag t) {
        RegistroPaises r = new RegistroPaises();
        for (Tag x : t.getList("paises", Tag.TAG_COMPOUND)) {
            Pais p = Pais.cargar((CompoundTag) x);
            r.paises.put(p.id, p);
        }
        for (Tag x : t.getList("territorio", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            String id = c.getString("pais");
            if (!r.paises.containsKey(id)) continue;
            for (long l : c.getLongArray("chunks")) r.territorio.put(l, id);
        }
        return r;
    }
}
