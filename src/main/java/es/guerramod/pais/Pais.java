package es.guerramod.pais;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.*;

/** Un país: nombre, color, miembros, gobierno, ciudadanos aldeanos y relaciones de guerra. */
public class Pais {
    public static final String PRES_NINGUNO = "ninguno";
    public static final String PRES_JUGADOR = "jugador";
    public static final String PRES_ALDEANO = "aldeano";

    public final String id;
    public String nombre;
    public int color;
    public UUID fundador;
    public final Set<UUID> miembros = new LinkedHashSet<>();
    public final Map<UUID, String> nombresMiembros = new HashMap<>();

    public String tipoPresidente = PRES_NINGUNO;
    public UUID presidente;
    public String nombrePresidente = "";

    /** cargo -> ministro (aldeano). */
    public final Map<String, Ministro> ministros = new LinkedHashMap<>();
    public final Set<UUID> ciudadanos = new HashSet<>();

    public final Set<String> enemigos = new HashSet<>();
    public final Set<String> ofertasPaz = new HashSet<>();

    /** Registro de cruces fronterizos (no se guarda en disco). */
    public final Deque<String> registroFronteras = new ArrayDeque<>();

    public record Ministro(UUID aldeano, String nombre) {}

    public Pais(String id, String nombre, int color, UUID fundador) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.fundador = fundador;
    }

    public boolean enGuerraCon(String otro) {
        return enemigos.contains(otro);
    }

    public boolean esMiembro(UUID jugador) {
        return miembros.contains(jugador);
    }

    /** Puede gobernar: el fundador o el presidente si es un jugador. */
    public boolean puedeGobernar(UUID jugador) {
        return jugador.equals(fundador) || (PRES_JUGADOR.equals(tipoPresidente) && jugador.equals(presidente));
    }

    public MutableComponent nombreColoreado() {
        return Component.literal(nombre).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true));
    }

    public void anotarCruce(String texto) {
        registroFronteras.addFirst(texto);
        while (registroFronteras.size() > 10) registroFronteras.removeLast();
    }

    public CompoundTag guardar() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putString("nombre", nombre);
        t.putInt("color", color);
        if (fundador != null) t.putUUID("fundador", fundador);
        ListTag mi = new ListTag();
        for (UUID u : miembros) {
            CompoundTag m = new CompoundTag();
            m.putUUID("u", u);
            m.putString("n", nombresMiembros.getOrDefault(u, "?"));
            mi.add(m);
        }
        t.put("miembros", mi);
        t.putString("tipoPresidente", tipoPresidente);
        if (presidente != null) t.putUUID("presidente", presidente);
        t.putString("nombrePresidente", nombrePresidente);
        ListTag mins = new ListTag();
        for (Map.Entry<String, Ministro> e : ministros.entrySet()) {
            CompoundTag m = new CompoundTag();
            m.putString("cargo", e.getKey());
            m.putUUID("u", e.getValue().aldeano());
            m.putString("n", e.getValue().nombre());
            mins.add(m);
        }
        t.put("ministros", mins);
        ListTag ciu = new ListTag();
        for (UUID u : ciudadanos) {
            CompoundTag m = new CompoundTag();
            m.putUUID("u", u);
            ciu.add(m);
        }
        t.put("ciudadanos", ciu);
        t.put("enemigos", lista(enemigos));
        t.put("ofertasPaz", lista(ofertasPaz));
        return t;
    }

    public static Pais cargar(CompoundTag t) {
        Pais p = new Pais(t.getString("id"), t.getString("nombre"), t.getInt("color"),
                t.hasUUID("fundador") ? t.getUUID("fundador") : null);
        for (Tag x : t.getList("miembros", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) x;
            UUID u = m.getUUID("u");
            p.miembros.add(u);
            p.nombresMiembros.put(u, m.getString("n"));
        }
        p.tipoPresidente = t.getString("tipoPresidente");
        if (p.tipoPresidente.isEmpty()) p.tipoPresidente = PRES_NINGUNO;
        if (t.hasUUID("presidente")) p.presidente = t.getUUID("presidente");
        p.nombrePresidente = t.getString("nombrePresidente");
        for (Tag x : t.getList("ministros", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) x;
            p.ministros.put(m.getString("cargo"), new Ministro(m.getUUID("u"), m.getString("n")));
        }
        for (Tag x : t.getList("ciudadanos", Tag.TAG_COMPOUND)) {
            p.ciudadanos.add(((CompoundTag) x).getUUID("u"));
        }
        for (Tag x : t.getList("enemigos", Tag.TAG_STRING)) p.enemigos.add(x.getAsString());
        for (Tag x : t.getList("ofertasPaz", Tag.TAG_STRING)) p.ofertasPaz.add(x.getAsString());
        return p;
    }

    private static ListTag lista(Collection<String> c) {
        ListTag l = new ListTag();
        for (String s : c) l.add(StringTag.valueOf(s));
        return l;
    }
}
