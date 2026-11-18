package es.guerramod.pais;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/** Acciones de gobierno: presidentes, ministros, elecciones, guerra y paz. */
public final class Gobierno {
    public static final List<String> CARGOS = List.of(
            "defensa", "economia", "exteriores", "interior", "sanidad", "educacion", "justicia", "transporte");

    private static final String[] NOMBRES = {
            "Alfonso", "Carmen", "Pedro", "Lucía", "Javier", "Elena", "Manuel", "Rosa", "Francisco", "Isabel",
            "Antonio", "Pilar", "Luis", "Marta", "Fernando", "Teresa", "Miguel", "Sofía", "Ramón", "Ana"};
    private static final String[] APELLIDOS = {
            "García", "Martín", "López", "Sánchez", "Pérez", "Gómez", "Ruiz", "Díaz", "Moreno", "Muñoz",
            "Álvarez", "Romero", "Navarro", "Torres", "Domínguez", "Vázquez", "Castro", "Ortega", "Rubio", "Molina"};

    private static final Random RND = new Random();

    private Gobierno() {}

    public static String nombreAleatorio() {
        return NOMBRES[RND.nextInt(NOMBRES.length)] + " " + APELLIDOS[RND.nextInt(APELLIDOS.length)];
    }

    public static String cargoBonito(String cargo) {
        return switch (cargo) {
            case "defensa" -> "Ministro de Defensa";
            case "economia" -> "Ministro de Economía";
            case "exteriores" -> "Ministro de Exteriores";
            case "interior" -> "Ministro del Interior";
            case "sanidad" -> "Ministro de Sanidad";
            case "educacion" -> "Ministro de Educación";
            case "justicia" -> "Ministro de Justicia";
            case "transporte" -> "Ministro de Transporte";
            default -> "Ministro de " + cargo;
        };
    }

    public static MutableComponent color(String texto, int color) {
        return Component.literal(texto).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    /** Aldeano más cercano al jugador (máx. 8 bloques). */
    @Nullable
    public static Villager aldeanoCercano(ServerPlayer jugador) {
        List<Villager> lista = jugador.level().getEntitiesOfClass(Villager.class, jugador.getBoundingBox().inflate(8));
        Villager mejor = null;
        double d = Double.MAX_VALUE;
        for (Villager v : lista) {
            double dd = v.distanceToSqr(jugador);
            if (dd < d) {
                d = dd;
                mejor = v;
            }
        }
        return mejor;
    }

    /** Pone nombre visible a un aldeano con el color del país y evita que desaparezca. */
    public static void bautizarAldeano(Villager v, String titulo, Pais pais) {
        v.setCustomName(color(titulo, pais.color));
        v.setCustomNameVisible(true);
        v.setPersistenceRequired();
        pais.ciudadanos.add(v.getUUID());
    }

    public static void quitarCargosDe(Pais pais, UUID aldeano) {
        pais.ministros.values().removeIf(m -> m.aldeano().equals(aldeano));
    }

    public static void nombrarPresidenteAldeano(Pais pais, Villager v) {
        quitarCargosDe(pais, v.getUUID());
        String nombre = nombreAleatorio();
        pais.tipoPresidente = Pais.PRES_ALDEANO;
        pais.presidente = v.getUUID();
        pais.nombrePresidente = nombre;
        bautizarAldeano(v, "Presidente " + nombre, pais);
    }

    public static void nombrarPresidenteJugador(Pais pais, ServerPlayer jugador) {
        pais.tipoPresidente = Pais.PRES_JUGADOR;
        pais.presidente = jugador.getUUID();
        pais.nombrePresidente = jugador.getGameProfile().getName();
    }

    public static void nombrarMinistro(Pais pais, String cargo, Villager v) {
        if (v.getUUID().equals(pais.presidente)) {
            pais.tipoPresidente = Pais.PRES_NINGUNO;
            pais.presidente = null;
            pais.nombrePresidente = "";
        }
        quitarCargosDe(pais, v.getUUID());
        String nombre = nombreAleatorio();
        pais.ministros.put(cargo, new Pais.Ministro(v.getUUID(), nombre));
        bautizarAldeano(v, cargoBonito(cargo) + " " + nombre, pais);
    }

    /**
     * Elecciones: gana al azar un jugador del país conectado o un aldeano ciudadano cercano.
     * Devuelve el texto del ganador.
     */
    public static Component elecciones(ServerLevel level, Pais pais, Vec3 centro) {
        List<Object> candidatos = new ArrayList<>();
        MinecraftServer server = level.getServer();
        for (UUID u : pais.miembros) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) candidatos.add(p);
        }
        for (Villager v : level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(centro, centro).inflate(64))) {
            Pais dueno = RegistroPaises.de(level).dueno(level, v.blockPosition());
            if (pais.ciudadanos.contains(v.getUUID()) || dueno == pais) candidatos.add(v);
        }
        if (candidatos.isEmpty()) return Component.literal("No hay candidatos.").withStyle(ChatFormatting.RED);
        Object ganador = candidatos.get(RND.nextInt(candidatos.size()));
        if (ganador instanceof ServerPlayer p) {
            nombrarPresidenteJugador(pais, p);
        } else {
            nombrarPresidenteAldeano(pais, (Villager) ganador);
        }
        DatosPaises.cambiado(server);
        MutableComponent msg = Component.literal("Elecciones en ").append(pais.nombreColoreado())
                .append(Component.literal(": gana " + pais.nombrePresidente + " (" + candidatos.size() + " candidatos)"));
        server.getPlayerList().broadcastSystemMessage(msg, false);
        return msg;
    }

    public static void declararGuerra(MinecraftServer server, Pais atacante, Pais defensor) {
        atacante.enemigos.add(defensor.id);
        defensor.enemigos.add(atacante.id);
        atacante.ofertasPaz.remove(defensor.id);
        defensor.ofertasPaz.remove(atacante.id);
        DatosPaises.cambiado(server);
        Component titulo = Component.literal("¡GUERRA!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component sub = atacante.nombreColoreado().append(Component.literal(" declara la guerra a ").withStyle(ChatFormatting.WHITE))
                .append(defensor.nombreColoreado());
        anunciar(server, titulo, sub, SoundEvents.WITHER_SPAWN);
        server.getPlayerList().broadcastSystemMessage(Component.literal("⚔ ").append(sub.copy()), false);
    }

    public static void firmarPaz(MinecraftServer server, Pais a, Pais b) {
        a.enemigos.remove(b.id);
        b.enemigos.remove(a.id);
        a.ofertasPaz.remove(b.id);
        b.ofertasPaz.remove(a.id);
        DatosPaises.cambiado(server);
        Component titulo = Component.literal("PAZ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        Component sub = a.nombreColoreado().append(Component.literal(" y ").withStyle(ChatFormatting.WHITE)).append(b.nombreColoreado())
                .append(Component.literal(" firman la paz").withStyle(ChatFormatting.WHITE));
        anunciar(server, titulo, sub, SoundEvents.PLAYER_LEVELUP);
        server.getPlayerList().broadcastSystemMessage(Component.literal("☮ ").append(sub.copy()), false);
    }

    public static void anunciar(MinecraftServer server, Component titulo, Component sub, SoundEvent sonido) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(titulo));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.playNotifySound(sonido, SoundSource.MASTER, 0.8f, 1.0f);
        }
    }

    /** Envía un mensaje a todos los miembros conectados de un país. */
    public static void avisarPais(MinecraftServer server, Pais pais, Component msg) {
        for (UUID u : pais.miembros) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) p.sendSystemMessage(msg);
        }
    }

    /** Cuando muere un aldeano con cargo. */
    public static void alMorir(MinecraftServer server, Entity muerto) {
        RegistroPaises r = RegistroPaises.servidor();
        UUID u = muerto.getUUID();
        boolean cambio = false;
        for (Pais p : r.paises.values()) {
            if (u.equals(p.presidente) && Pais.PRES_ALDEANO.equals(p.tipoPresidente)) {
                avisarPais(server, p, Component.literal("☠ El presidente " + p.nombrePresidente + " ha muerto. El cargo está vacante.")
                        .withStyle(ChatFormatting.RED));
                p.tipoPresidente = Pais.PRES_NINGUNO;
                p.presidente = null;
                p.nombrePresidente = "";
                cambio = true;
            }
            Iterator<Map.Entry<String, Pais.Ministro>> it = p.ministros.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Pais.Ministro> e = it.next();
                if (e.getValue().aldeano().equals(u)) {
                    avisarPais(server, p, Component.literal("☠ El " + cargoBonito(e.getKey()) + " " + e.getValue().nombre() + " ha muerto.")
                            .withStyle(ChatFormatting.RED));
                    it.remove();
                    cambio = true;
                }
            }
            if (p.ciudadanos.remove(u)) cambio = true;
        }
        if (cambio) DatosPaises.cambiado(server);
    }
}
