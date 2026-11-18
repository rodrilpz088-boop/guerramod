package es.guerramod.eventos;

import es.guerramod.GuerraMod;
import es.guerramod.armas.Disparos;
import es.guerramod.entidades.BalaEntity;
import es.guerramod.pais.ComandoPais;
import es.guerramod.pais.DatosPaises;
import es.guerramod.pais.Gobierno;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.radiacion.DatosRadiacion;
import es.guerramod.red.Red;
import es.guerramod.registro.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reglas del servidor: fronteras, protección de territorio, avisos, radiación, chaleco antibalas. */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EventosServidor {
    /** Último país en el que estaba cada jugador ("" = territorio libre). */
    private static final Map<UUID, String> ULTIMO_TERRITORIO = new HashMap<>();

    private EventosServidor() {}

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent e) {
        ComandoPais.registrar(e.getDispatcher());
    }

    @SubscribeEvent
    public static void entrar(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        RegistroPaises reg = RegistroPaises.servidor();
        Pais pais = reg.paisDeJugador(p.getUUID());
        if (pais != null) {
            pais.nombresMiembros.put(p.getUUID(), p.getGameProfile().getName());
            DatosPaises.get(p.server).setDirty();
        }
        Red.sincronizar(p);
        p.sendSystemMessage(Component.literal("⚔ Naciones en Guerra: escribe ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("/pais").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" para empezar. Tecla M: minimapa.").withStyle(ChatFormatting.GOLD)));
    }

    @SubscribeEvent
    public static void salir(PlayerEvent.PlayerLoggedOutEvent e) {
        ULTIMO_TERRITORIO.remove(e.getEntity().getUUID());
        Disparos.olvidar(e.getEntity().getUUID());
    }

    // ---------------- Protección de territorio ----------------

    /** Un jugador puede modificar si: creativo, zona libre, su país o país enemigo en guerra. */
    public static boolean puedeModificar(Player p, Level level, BlockPos pos) {
        if (p.getAbilities().instabuild) return true;
        RegistroPaises reg = RegistroPaises.de(level);
        return reg.puedeAfectar(reg.paisDeJugador(p.getUUID()), level, pos);
    }

    private static void avisoProtegido(Player p, Level level, BlockPos pos) {
        Pais dueno = RegistroPaises.de(level).dueno(level, pos);
        if (dueno != null) {
            p.displayClientMessage(Component.literal("Territorio de ").withStyle(ChatFormatting.RED).append(dueno.nombreColoreado())
                    .append(Component.literal(": solo en guerra puedes atacarlo").withStyle(ChatFormatting.RED)), true);
        }
    }

    @SubscribeEvent
    public static void romper(BlockEvent.BreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || !(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!puedeModificar(p, level, e.getPos())) {
            e.setCanceled(true);
            avisoProtegido(p, level, e.getPos());
        }
    }

    @SubscribeEvent
    public static void colocar(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!puedeModificar(p, level, e.getPos())) {
            e.setCanceled(true);
            avisoProtegido(p, level, e.getPos());
        }
    }

    @SubscribeEvent
    public static void abrirCofre(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        if (level.isClientSide) return;
        if (level.getBlockEntity(e.getPos()) instanceof Container && !puedeModificar(e.getEntity(), level, e.getPos())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            avisoProtegido(e.getEntity(), level, e.getPos());
        }
    }

    /** Las explosiones causadas por jugadores respetan las fronteras (solo dañan en guerra). */
    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate e) {
        Level level = e.getLevel();
        if (level.isClientSide) return;
        LivingEntity causante = e.getExplosion().getIndirectSourceEntity();
        if (!(causante instanceof Player p) || p.getAbilities().instabuild) return;
        RegistroPaises reg = RegistroPaises.de(level);
        Pais atacante = reg.paisDeJugador(p.getUUID());
        e.getAffectedBlocks().removeIf(pos -> !reg.puedeAfectar(atacante, level, pos));
    }

    // ---------------- Gobierno ----------------

    @SubscribeEvent
    public static void muerte(LivingDeathEvent e) {
        if (e.getEntity() instanceof Villager v && !v.level().isClientSide) {
            Gobierno.alMorir(v.getServer(), v);
        }
    }

    /** El chaleco antibalas reduce a la mitad el daño de las balas; el casco, un 20 % más. */
    @SubscribeEvent
    public static void dano(LivingHurtEvent e) {
        if (!(e.getSource().getDirectEntity() instanceof BalaEntity)) return;
        LivingEntity v = e.getEntity();
        float f = 1.0f;
        if (v.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CHALECO_ANTIBALAS.get())) f *= 0.5f;
        if (v.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CASCO_MILITAR.get())) f *= 0.8f;
        e.setAmount(e.getAmount() * f);
    }

    // ---------------- Cruces de frontera ----------------

    @SubscribeEvent
    public static void tickJugador(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        RegistroPaises reg = RegistroPaises.servidor();
        Pais aqui = reg.dueno(p.level(), p.blockPosition());
        String id = aqui == null ? "" : aqui.id;
        String antes = ULTIMO_TERRITORIO.put(p.getUUID(), id);
        if (antes == null || antes.equals(id)) return;

        Pais mio = reg.paisDeJugador(p.getUUID());
        MinecraftServer server = p.server;
        if (aqui == null) {
            p.displayClientMessage(Component.literal("Territorio libre").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        MutableComponent msg;
        if (mio == aqui) {
            msg = Component.literal("Bienvenido a casa: ").withStyle(ChatFormatting.GREEN).append(aqui.nombreColoreado());
        } else if (mio != null && mio.enGuerraCon(aqui.id)) {
            msg = Component.literal("⚔ TERRITORIO ENEMIGO: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD).append(aqui.nombreColoreado());
        } else {
            msg = Component.literal("Entras en ").withStyle(ChatFormatting.YELLOW).append(aqui.nombreColoreado());
        }
        p.displayClientMessage(msg, true);

        if (mio != aqui) {
            String origen = mio == null ? "sin país" : mio.nombre;
            aqui.anotarCruce(p.getGameProfile().getName() + " (" + origen + ") en " + p.getBlockX() + ", " + p.getBlockZ());
            boolean enemigo = mio != null && mio.enGuerraCon(aqui.id);
            Gobierno.avisarPais(server, aqui, Component.literal((enemigo ? "⚠ ¡ENEMIGO! " : "") + p.getGameProfile().getName()
                    + (mio == null ? "" : " de " + mio.nombre) + " ha cruzado la frontera en " + p.getBlockX() + ", " + p.getBlockZ())
                    .withStyle(enemigo ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    }

    // ---------------- Radiación ----------------

    @SubscribeEvent
    public static void tickServidor(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Disparos.tick(server);
        if (server.getTickCount() % 20 != 0) return;
        DatosRadiacion.get(server).tick(server);
    }
}
