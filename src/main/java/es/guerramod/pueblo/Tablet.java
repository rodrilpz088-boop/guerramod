package es.guerramod.pueblo;

import es.guerramod.GuerraMod;
import es.guerramod.red.Red;
import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tablet municipal: clic derecho a un aldeano para ver su DNI y darle un oficio. Clic derecho al aire para ver el
 * pueblo, elegirlo y declarar la guerra a otro pueblo elegido antes (o firmar la paz). Todo sin comandos.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Tablet extends Item {
    public static final RegistryObject<Item> TABLET = registrar();

    private static RegistryObject<Item> registrar() {
        RegistryObject<Item> r = ModItems.ITEMS.register("tablet_municipal", () -> new Tablet(new Item.Properties().stacksTo(1)));
        Categoria.CIUDAD.lista.add(r);
        return r;
    }

    public static void iniciar() {
    }

    /** Pueblo elegido por cada jugador para la guerra. */
    private static final Map<UUID, UUID> ELEGIDO = new HashMap<>();

    public static final int ROL = 0, ELEGIR = 1, GUERRA = 2, PAZ = 3, INDEPENDENCIA = 4;

    public Tablet(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Clic derecho a un aldeano: su DNI y su oficio").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Clic derecho al aire: el pueblo, guerra y paz").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide && p instanceof ServerPlayer sp) {
            ServerLevel sl = sp.server.overworld();
            Pueblos.Pueblo pu = l == sl ? Pueblos.get(sl).cercano(p.blockPosition(), 140) : null;
            if (pu == null) {
                p.displayClientMessage(Component.literal("No hay ningún pueblo cerca. Habla con un aldeano para registrarlo.")
                        .withStyle(ChatFormatting.YELLOW), true);
            } else {
                abrir(sp, null, pu);
            }
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(h), l.isClientSide);
    }

    @SubscribeEvent
    public static void clicAldeano(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof Villager v) || !e.getItemStack().is(TABLET.get())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer sp && v.level() == sp.server.overworld()) {
            ServerLevel l = (ServerLevel) v.level();
            Dni.asegurar(l, v);
            abrir(sp, v, Pueblos.get(l).pueblo(Dni.pueblo(v)));
        }
    }

    // ====================================================================== datos para la pantalla

    public static void abrir(ServerPlayer p, @Nullable Villager v, @Nullable Pueblos.Pueblo pu) {
        Red.abrirFicha(p, v == null ? -1 : v.getId(), datos(p, v, pu));
    }

    private static CompoundTag datos(ServerPlayer jugador, @Nullable Villager v, @Nullable Pueblos.Pueblo pu) {
        ServerLevel l = jugador.server.overworld();
        CompoundTag t = new CompoundTag();
        if (v != null) {
            CompoundTag d = Dni.datos(v).copy();
            d.putString("numeroTxt", Dni.numero(d));
            long hoy = l.getDayTime() / 24000L;
            d.putString("fechaTxt", Dni.fecha(d.getLong("nacimiento")));
            d.putInt("edad", (int) Math.max(0, (hoy - d.getLong("nacimiento")) / 800L));
            d.putString("profesion", v.getVillagerData().getProfession().name());
            d.putInt("nivel", v.getVillagerData().getLevel());
            d.putBoolean("menor", v.isBaby());
            d.putFloat("vida", v.getHealth());
            t.put("dni", d);
        }
        if (pu != null) {
            Pueblos ps = Pueblos.get(l);
            Trabajos.Censo c = Trabajos.censo(pu.id);
            CompoundTag p = new CompoundTag();
            p.putString("nombre", pu.nombre);
            p.putInt("poblacion", c.todos.size());
            p.putInt("adultos", c.adultos.size());
            p.putLong("camas", c.camas);
            p.putInt("casas", pu.casas.size());
            p.putInt("calles", pu.calles.size());
            p.putInt("farolas", pu.farolas.size());
            p.putInt("huertos", pu.huertos.size());
            p.putBoolean("ciudad", pu.esCiudad(c.todos.size()));
            p.putInt("victorias", pu.victorias);
            ListTag obras = new ListTag();
            for (Construccion.Obra o : pu.obras) {
                Construccion.piezas(l, o);
                String nom = switch (o.tipo) {
                    case "huerto" -> "Huerto";
                    case "arbol" -> "Árbol de luces";
                    case "edificio" -> es.guerramod.ciudad.Edificio.valueOf(o.id).nombre;
                    default -> "Casa " + o.id.substring(o.id.lastIndexOf('/') + 1).replace("_", " ")
                            .replace("small house", "pequeña").replace("medium house", "mediana").replace("big house", "grande")
                            .replace("plains", "de llanura").replace("desert", "del desierto").replace("savanna", "de sabana")
                            .replace("taiga", "de taiga").replace("snowy", "de nieve");
                };
                obras.add(StringTag.valueOf(nom + ": " + Math.round(o.progreso() * 100) + "%"));
            }
            p.put("obras", obras);
            int[] roles = new int[Rol.values().length];
            for (Rol r : Rol.values()) roles[r.ordinal()] = c.de(r).size();
            p.putIntArray("roles", roles);
            if (pu.enemigo != null && ps.pueblo(pu.enemigo) != null) p.putString("enemigo", ps.pueblo(pu.enemigo).nombre);
            if (pu.pertenece != null && ps.pueblo(pu.pertenece) != null) p.putString("pertenece", ps.pueblo(pu.pertenece).nombre);
            UUID el = ELEGIDO.get(jugador.getUUID());
            Pueblos.Pueblo elegido = ps.pueblo(el);
            if (elegido != null) {
                p.putString("elegido", elegido.nombre);
                p.putBoolean("esElegido", elegido == pu);
                p.putInt("distElegido", (int) Math.sqrt(elegido.centro.distSqr(pu.centro)));
            }
            t.put("pueblo", p);
        }
        return t;
    }

    // ====================================================================== acciones desde la pantalla

    public static void accion(ServerPlayer p, int entidad, int accion, int valor) {
        if (!p.getMainHandItem().is(TABLET.get()) && !p.getOffhandItem().is(TABLET.get())) return;
        ServerLevel l = p.server.overworld();
        if (p.level() != l) return;
        Pueblos ps = Pueblos.get(l);
        Villager v = null;
        Pueblos.Pueblo pu;
        if (entidad >= 0) {
            Entity e = l.getEntity(entidad);
            if (!(e instanceof Villager vv) || vv.distanceToSqr(p) > 16 * 16) return;
            v = vv;
            pu = ps.pueblo(Dni.pueblo(v));
        } else {
            pu = ps.cercano(p.blockPosition(), 140);
        }
        switch (accion) {
            case ROL -> {
                if (v == null || v.isBaby()) return;
                Rol r = Rol.de(valor);
                Trabajos.asignar(l, v, r);
                p.displayClientMessage(Component.literal(Dni.nombreCompleto(Dni.datos(v)) + " ahora es ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(r.nombre).withStyle(s -> s.withColor(r.color))), true);
            }
            case ELEGIR -> {
                if (pu == null) return;
                ELEGIDO.put(p.getUUID(), pu.id);
                p.displayClientMessage(Component.literal("Elegido " + pu.nombre + ". Ahora ve a otro pueblo y declárale la guerra desde la tablet.")
                        .withStyle(ChatFormatting.GOLD), false);
            }
            case GUERRA -> {
                Pueblos.Pueblo a = ps.pueblo(ELEGIDO.get(p.getUUID()));
                if (a == null || pu == null || a == pu) {
                    p.displayClientMessage(Component.literal("Primero elige un pueblo y después, en otro pueblo, declara la guerra.")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
                if (a.enemigo != null) Trabajos.paz(l, a);
                if (pu.enemigo != null) Trabajos.paz(l, pu);
                Trabajos.declararGuerra(l, a, pu);
                ELEGIDO.remove(p.getUUID());
            }
            case PAZ -> {
                if (pu != null && pu.enemigo != null) Trabajos.paz(l, pu);
            }
            case INDEPENDENCIA -> {
                if (pu != null && pu.pertenece != null) {
                    pu.pertenece = null;
                    ps.setDirty();
                    Trabajos.avisarTodos(l, Component.literal("🎉 " + pu.nombre + " vuelve a ser independiente").withStyle(ChatFormatting.AQUA));
                }
            }
            default -> {
                return;
            }
        }
        abrir(p, v, pu);
    }
}
