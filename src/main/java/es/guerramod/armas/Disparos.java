package es.guerramod.armas;

import es.guerramod.entidades.BalaEntity;
import es.guerramod.entidades.CoheteEntity;
import es.guerramod.items.ArmaItem;
import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Lógica de disparo y recarga en el servidor. */
public final class Disparos {
    private static final Map<UUID, Long> ULTIMO_DISPARO = new HashMap<>();
    private static final List<SonidoPendiente> PENDIENTES = new ArrayList<>();

    private record SonidoPendiente(long cuando, UUID jugador, SoundEvent sonido, float volumen) {}

    private Disparos() {}

    public static boolean apuntando(ServerPlayer p, ArmaItem arma) {
        return p.isUsingItem() && p.getUseItem().getItem() == arma;
    }

    // ------------------------------------------------------------------ disparar

    public static void disparar(ServerPlayer p, boolean moviendose) {
        ItemStack st = p.getMainHandItem();
        if (!(st.getItem() instanceof ArmaItem arma)) return;
        DefArma d = arma.def;
        ServerLevel level = p.serverLevel();
        long ahora = level.getGameTime();
        Long ult = ULTIMO_DISPARO.get(p.getUUID());
        if (ult != null && ahora - ult < Math.max(1, d.cadencia() - 1)) return;
        boolean creativo = p.getAbilities().instabuild;

        if (ArmaItem.recargando(st)) {
            // con escopeta se puede interrumpir la recarga para disparar
            if (d.porUnidad() && ArmaItem.getBalas(st) > 0) ArmaItem.quitarRecarga(st);
            else return;
        }
        int balas = ArmaItem.getBalas(st);
        if (balas <= 0) {
            ULTIMO_DISPARO.put(p.getUUID(), ahora + 4);
            level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), ModSonidos.VACIO.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            if (tieneMunicion(p, d) || creativo) iniciarRecarga(p, st, arma);
            return;
        }
        ULTIMO_DISPARO.put(p.getUUID(), ahora);
        if (!creativo) ArmaItem.setBalas(st, balas - 1);

        boolean apunta = apuntando(p, arma);
        float disp = apunta ? d.dispApuntando() : d.dispCadera();
        if (moviendose) disp = disp * (apunta ? 1.4f : 1.6f) + 0.3f;
        if (!p.onGround()) disp = disp * 2.5f + 1.0f;
        if (p.isCrouching()) disp *= 0.6f;

        Vec3 ojo = p.getEyePosition();
        for (int i = 0; i < d.proyectiles(); i++) {
            Projectile pr;
            if (d.explosiva()) {
                pr = new CoheteEntity(level, p);
            } else {
                pr = new BalaEntity(level, p, d.dano(), d.alcance());
            }
            pr.setPos(ojo.x, ojo.y - 0.1, ojo.z);
            pr.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, d.velocidad(), disp);
            level.addFreshEntity(pr);
        }

        float tono = 0.95f + level.random.nextFloat() * 0.1f;
        level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), d.sonido().get(), SoundSource.PLAYERS, d.volumen(), tono);
        if (d.cerrojo() != null) PENDIENTES.add(new SonidoPendiente(ahora + 6, p.getUUID(), d.cerrojo().get(), 1.0f));

        efectos(level, p, d);
    }

    /** Fogonazo, humo y casquillo expulsado (lo ven todos). */
    private static void efectos(ServerLevel level, ServerPlayer p, DefArma d) {
        Vec3 mira = p.getLookAngle();
        Vec3 derecha = mira.cross(new Vec3(0, 1, 0));
        if (derecha.lengthSqr() < 1.0E-4) derecha = new Vec3(1, 0, 0);
        derecha = derecha.normalize();
        Vec3 boca = p.getEyePosition().add(mira.scale(1.1)).add(derecha.scale(0.25)).add(0, -0.18, 0);
        level.sendParticles(ParticleTypes.SMOKE, boca.x, boca.y, boca.z, d.explosiva() ? 12 : 3, 0.04, 0.04, 0.04, 0.01);
        level.sendParticles(ParticleTypes.SMALL_FLAME, boca.x, boca.y, boca.z, 1, 0.0, 0.0, 0.0, 0.0);
        if (d.explosiva()) {
            // chorro trasero del lanzacohetes
            Vec3 atras = p.getEyePosition().subtract(mira.scale(1.2));
            level.sendParticles(ParticleTypes.CLOUD, atras.x, atras.y, atras.z, 15, 0.2, 0.2, 0.2, 0.05);
            return;
        }
        Item casquillo = d == Armas.ESCOPETA ? ModItems.CARTUCHO_VACIO.get() : ModItems.CASQUILLO.get();
        Vec3 expulsion = p.getEyePosition().add(mira.scale(0.5)).add(derecha.scale(0.3)).add(0, -0.25, 0);
        Vec3 vel = derecha.scale(0.18).add(0, 0.12, 0).add(mira.scale(-0.02));
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(casquillo)),
                expulsion.x, expulsion.y, expulsion.z, 0, vel.x, vel.y, vel.z, 1.0);
        PENDIENTES.add(new SonidoPendiente(level.getGameTime() + 14, p.getUUID(), ModSonidos.CASQUILLO.get(), 0.35f));
    }

    // ------------------------------------------------------------------ recarga

    public static boolean tieneMunicion(ServerPlayer p, DefArma d) {
        Item m = d.municion().get();
        for (ItemStack s : p.getInventory().items) if (s.is(m)) return true;
        return p.getOffhandItem().is(m);
    }

    public static void iniciarRecarga(ServerPlayer p, ItemStack st, ArmaItem arma) {
        DefArma d = arma.def;
        if (ArmaItem.recargando(st) || ArmaItem.getBalas(st) >= d.cargador()) return;
        if (!p.getAbilities().instabuild && !tieneMunicion(p, d)) {
            p.displayClientMessage(Component.literal("Sin munición: necesitas ").append(d.municion().get().getDescription())
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        long ahora = p.level().getGameTime();
        st.getOrCreateTag().putLong("recIni", ahora);
        st.getOrCreateTag().putLong("recFin", ahora + d.recarga());
        SoundEvent s = d.porUnidad() ? ModSonidos.CARTUCHO.get() : ModSonidos.CARGADOR_FUERA.get();
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 0.9f, 1.0f);
        if (!d.porUnidad()) {
            PENDIENTES.add(new SonidoPendiente(ahora + Math.max(2, d.recarga() * 6 / 10), p.getUUID(), ModSonidos.CARGADOR_DENTRO.get(), 0.9f));
        }
    }

    public static void completarRecarga(ServerPlayer p, ItemStack st, ArmaItem arma) {
        DefArma d = arma.def;
        ArmaItem.quitarRecarga(st);
        int balas = ArmaItem.getBalas(st);
        int falta = d.porUnidad() ? 1 : d.cargador() - balas;
        int puestas = p.getAbilities().instabuild ? falta : sacarMunicion(p, d.municion().get(), falta);
        ArmaItem.setBalas(st, balas + puestas);
        if (puestas <= 0) return;
        if (d.porUnidad()) {
            if (ArmaItem.getBalas(st) < d.cargador()) {
                iniciarRecarga(p, st, arma);
            } else if (d.cerrojo() != null) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), d.cerrojo().get(), SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        } else {
            SoundEvent fin = d.cerrojo() != null ? d.cerrojo().get() : ModSonidos.MONTAR.get();
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), fin, SoundSource.PLAYERS, 0.9f, 1.0f);
        }
    }

    private static int sacarMunicion(ServerPlayer p, Item m, int cuantas) {
        int sacadas = 0;
        List<ItemStack> pilas = new ArrayList<>(p.getInventory().items);
        pilas.add(p.getOffhandItem());
        for (ItemStack s : pilas) {
            if (sacadas >= cuantas) break;
            if (!s.is(m)) continue;
            int n = Math.min(s.getCount(), cuantas - sacadas);
            s.shrink(n);
            sacadas += n;
        }
        return sacadas;
    }

    public static void cambiarModo(ServerPlayer p) {
        ItemStack st = p.getMainHandItem();
        if (!(st.getItem() instanceof ArmaItem arma) || !arma.def.automatica()) return;
        boolean semi = !arma.esSemi(st);
        st.getOrCreateTag().putBoolean("semi", semi);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSonidos.MODO.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        p.displayClientMessage(Component.literal("Modo de fuego: " + (semi ? "SEMIAUTOMÁTICO" : "AUTOMÁTICO"))
                .withStyle(ChatFormatting.YELLOW), true);
    }

    public static void recargar(ServerPlayer p) {
        ItemStack st = p.getMainHandItem();
        if (st.getItem() instanceof ArmaItem arma) iniciarRecarga(p, st, arma);
    }

    // ------------------------------------------------------------------ sonidos con retraso

    public static void tick(MinecraftServer server) {
        if (PENDIENTES.isEmpty()) return;
        Iterator<SonidoPendiente> it = PENDIENTES.iterator();
        while (it.hasNext()) {
            SonidoPendiente s = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(s.jugador());
            if (p == null) {
                it.remove();
                continue;
            }
            if (p.level().getGameTime() >= s.cuando()) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s.sonido(), SoundSource.PLAYERS, s.volumen(),
                        0.9f + p.level().random.nextFloat() * 0.2f);
                it.remove();
            }
        }
    }

    public static void olvidar(UUID jugador) {
        ULTIMO_DISPARO.remove(jugador);
    }
}
