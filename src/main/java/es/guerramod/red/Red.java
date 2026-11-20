package es.guerramod.red;

import es.guerramod.GuerraMod;
import es.guerramod.armas.Disparos;
import es.guerramod.entidades.AvionEntity;
import es.guerramod.pais.DatosPaises;
import es.guerramod.pais.RegistroPaises;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Paquetes de red: sincronizar países al cliente y acciones del avión al servidor. */
public final class Red {
    private static final String VERSION = "3";
    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Red() {}

    public static void registrar() {
        int id = 0;
        CANAL.messageBuilder(SyncPaises.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPaises::encode).decoder(SyncPaises::decode).consumerMainThread(SyncPaises::handle).add();
        CANAL.messageBuilder(AccionAvion.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AccionAvion::encode).decoder(AccionAvion::decode).consumerMainThread(AccionAvion::handle).add();
        CANAL.messageBuilder(SyncClima.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncClima::encode).decoder(SyncClima::decode).consumerMainThread(SyncClima::handle).add();
        CANAL.messageBuilder(AccionArma.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AccionArma::encode).decoder(AccionArma::decode).consumerMainThread(AccionArma::handle).add();
        CANAL.messageBuilder(SyncTemblor.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTemblor::encode).decoder(SyncTemblor::decode).consumerMainThread(SyncTemblor::handle).add();
        CANAL.messageBuilder(SyncTiempo.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTiempo::encode).decoder(SyncTiempo::decode).consumerMainThread(SyncTiempo::handle).add();
    }

    public static void sincronizarTodos(MinecraftServer server) {
        CANAL.send(PacketDistributor.ALL.noArg(), new SyncPaises(DatosPaises.get(server).registro.guardar()));
    }

    public static void sincronizarClima(MinecraftServer server, float angulo, float velocidad, int riesgo) {
        CANAL.send(PacketDistributor.ALL.noArg(), new SyncClima(angulo, velocidad, riesgo));
    }

    public static void sincronizarClima(ServerPlayer p) {
        es.guerramod.incendios.Clima c = es.guerramod.incendios.Clima.get(p.server);
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), new SyncClima(c.angulo, c.velocidad, c.riesgo(p.level(), p.blockPosition())));
    }

    public static void enviarTiempo(ServerPlayer p, es.guerramod.incendios.Meteo m, es.guerramod.incendios.Clima c) {
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), new SyncTiempo((float) m.temperatura(), (float) m.humedad(),
                (float) m.nubes(), (float) m.niebla(), (float) m.turbidez(), (float) c.calentamiento(), (float) c.co2, c.cambio, m.zona(),
                es.guerramod.desastres.Desastres.granizoTicks > 0 ? es.guerramod.desastres.Desastres.granizoCm : 0f));
    }

    public static void enviarTemblor(ServerPlayer p, float intensidad) {
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), new SyncTemblor(intensidad));
    }

    /** Servidor -> cliente: cuánto tiembla el suelo donde está el jugador (0..1). */
    public record SyncTemblor(float intensidad) {
        public static void encode(SyncTemblor m, FriendlyByteBuf b) {
            b.writeFloat(m.intensidad);
        }

        public static SyncTemblor decode(FriendlyByteBuf b) {
            return new SyncTemblor(b.readFloat());
        }

        public static void handle(SyncTemblor m, Supplier<NetworkEvent.Context> ctx) {
            es.guerramod.clima.TiempoCliente.temblor = m.intensidad;
            ctx.get().setPacketHandled(true);
        }
    }

    /** Servidor -> cliente: tiempo en la posición del jugador (cielo, nubes, niebla, minimapa). */
    public record SyncTiempo(float t, float h, float nubes, float niebla, float turb, float anom, float co2, boolean cambio, String zona,
                             float granizo) {
        public static void encode(SyncTiempo m, FriendlyByteBuf b) {
            b.writeFloat(m.t);
            b.writeFloat(m.h);
            b.writeFloat(m.nubes);
            b.writeFloat(m.niebla);
            b.writeFloat(m.turb);
            b.writeFloat(m.anom);
            b.writeFloat(m.co2);
            b.writeBoolean(m.cambio);
            b.writeUtf(m.zona, 64);
            b.writeFloat(m.granizo);
        }

        public static SyncTiempo decode(FriendlyByteBuf b) {
            return new SyncTiempo(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                    b.readFloat(), b.readBoolean(), b.readUtf(64), b.readFloat());
        }

        public static void handle(SyncTiempo m, Supplier<NetworkEvent.Context> ctx) {
            es.guerramod.clima.TiempoCliente.temperatura = m.t;
            es.guerramod.clima.TiempoCliente.humedad = m.h;
            es.guerramod.clima.TiempoCliente.nubes = m.nubes;
            es.guerramod.clima.TiempoCliente.niebla = m.niebla;
            es.guerramod.clima.TiempoCliente.turbidez = m.turb;
            es.guerramod.clima.TiempoCliente.anomalia = m.anom;
            es.guerramod.clima.TiempoCliente.co2 = m.co2;
            es.guerramod.clima.TiempoCliente.cambio = m.cambio;
            es.guerramod.clima.TiempoCliente.zona = m.zona;
            es.guerramod.clima.TiempoCliente.granizo = m.granizo;
            es.guerramod.clima.TiempoCliente.recibido = true;
            ctx.get().setPacketHandled(true);
        }
    }

    public static void sincronizar(ServerPlayer p) {
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPaises(DatosPaises.get(p.server).registro.guardar()));
    }

    /** Servidor -> cliente: lista completa de países y territorios. */
    public record SyncPaises(CompoundTag datos) {
        public static void encode(SyncPaises m, FriendlyByteBuf b) {
            b.writeNbt(m.datos);
        }

        public static SyncPaises decode(FriendlyByteBuf b) {
            CompoundTag t = b.readNbt();
            return new SyncPaises(t == null ? new CompoundTag() : t);
        }

        public static void handle(SyncPaises m, Supplier<NetworkEvent.Context> ctx) {
            RegistroPaises.CLIENTE = RegistroPaises.cargar(m.datos);
            ctx.get().setPacketHandled(true);
        }
    }

    /** Cliente -> servidor: 0 = disparar ametralladora, 1 = soltar bomba. */
    public record AccionAvion(int accion) {
        public static void encode(AccionAvion m, FriendlyByteBuf b) {
            b.writeVarInt(m.accion);
        }

        public static AccionAvion decode(FriendlyByteBuf b) {
            return new AccionAvion(b.readVarInt());
        }

        public static void handle(AccionAvion m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null && p.getVehicle() instanceof AvionEntity avion) {
                if (m.accion == 0) avion.disparar(p);
                else if (m.accion == 1) avion.soltarBomba(p);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Cliente -> servidor: 0 = disparar, 1 = recargar, 2 = cambiar modo de fuego. */
    public record AccionArma(int accion, boolean moviendose) {
        public static final int DISPARAR = 0, RECARGAR = 1, MODO = 2;

        public static void encode(AccionArma m, FriendlyByteBuf b) {
            b.writeVarInt(m.accion);
            b.writeBoolean(m.moviendose);
        }

        public static AccionArma decode(FriendlyByteBuf b) {
            return new AccionArma(b.readVarInt(), b.readBoolean());
        }

        public static void handle(AccionArma m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null && p.isAlive()) {
                switch (m.accion) {
                    case DISPARAR -> Disparos.disparar(p, m.moviendose);
                    case RECARGAR -> Disparos.recargar(p);
                    case MODO -> Disparos.cambiarModo(p);
                    default -> { }
                }
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Servidor -> cliente: viento y riesgo de incendio (para el minimapa). */
    public record SyncClima(float angulo, float velocidad, int riesgo) {
        public static void encode(SyncClima m, FriendlyByteBuf b) {
            b.writeFloat(m.angulo);
            b.writeFloat(m.velocidad);
            b.writeVarInt(m.riesgo);
        }

        public static SyncClima decode(FriendlyByteBuf b) {
            return new SyncClima(b.readFloat(), b.readFloat(), b.readVarInt());
        }

        public static void handle(SyncClima m, Supplier<NetworkEvent.Context> ctx) {
            es.guerramod.incendios.Clima.clienteAngulo = m.angulo;
            es.guerramod.incendios.Clima.clienteVelocidad = m.velocidad;
            es.guerramod.incendios.Clima.clienteRiesgo = m.riesgo;
            ctx.get().setPacketHandled(true);
        }
    }
}
