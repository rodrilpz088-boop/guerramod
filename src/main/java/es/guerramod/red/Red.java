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
    private static final String VERSION = "1";
    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Red() {}

    public static void registrar() {
        int id = 0;
        CANAL.messageBuilder(SyncPaises.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPaises::encode).decoder(SyncPaises::decode).consumerMainThread(SyncPaises::handle).add();
        CANAL.messageBuilder(AccionAvion.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AccionAvion::encode).decoder(AccionAvion::decode).consumerMainThread(AccionAvion::handle).add();
        CANAL.messageBuilder(AccionArma.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AccionArma::encode).decoder(AccionArma::decode).consumerMainThread(AccionArma::handle).add();
    }

    public static void sincronizarTodos(MinecraftServer server) {
        CANAL.send(PacketDistributor.ALL.noArg(), new SyncPaises(DatosPaises.get(server).registro.guardar()));
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
}
