package es.guerramod.clima;

import es.guerramod.GuerraMod;
import es.guerramod.incendios.Clima;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estaciones del año: primavera, verano, otoño e invierno, de 200 días cada una (un año = 800 días).
 * <ul>
 *   <li>Temperatura: el máximo llega a finales de julio y el mínimo a finales de enero (retraso respecto al sol).</li>
 *   <li>Duración del día como en Madrid (40° N): unas 15 h de luz en el solsticio de verano y 9 h en el de invierno.
 *       El sol sube más en verano (73°) que en invierno (26°).</li>
 *   <li>Lluvias: veranos secos con tormentas al final, otoños y primaveras lluviosos, inviernos húmedos (nieve si hace frío).</li>
 *   <li>Navidad (primeros 40 días del invierno): ola de frío y más precipitación, así que nieva incluso donde normalmente no.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Estaciones {
    private Estaciones() {}

    public static final int DIAS_ESTACION = 200;
    public static final int DIAS_ANIO = DIAS_ESTACION * 4;
    public static final String[] NOMBRES = {"Primavera", "Verano", "Otoño", "Invierno"};
    public static final double LATITUD = 40.4;
    private static double acumulado = 0;

    // ------------------------------------------------------------------ calendario
    public static int dia(long tiempoDia, int desfase) {
        return (int) Math.floorMod(tiempoDia / 24000L + desfase, (long) DIAS_ANIO);
    }

    public static int dia(ServerLevel l) {
        return dia(l.getServer().overworld().getDayTime(), Clima.get(l.getServer()).desfaseDias);
    }

    public static int estacion(int dia) {
        return Math.min(3, dia / DIAS_ESTACION);
    }

    public static int diaEnEstacion(int dia) {
        return dia % DIAS_ESTACION + 1;
    }

    public static String nombre(int dia) {
        return NOMBRES[estacion(dia)];
    }

    public static boolean navidad(int dia) {
        return dia >= 600 && dia < 640;
    }

    /** +1 en pleno verano (finales de julio), -1 en pleno invierno (finales de enero). */
    public static double ciclo(int dia) {
        return Math.cos(2 * Math.PI * (dia - 270) / DIAS_ANIO);
    }

    /** +1 en el solsticio de verano, -1 en el de invierno. */
    public static double cicloLuz(int dia) {
        return Math.cos(2 * Math.PI * (dia - 200) / DIAS_ANIO);
    }

    public static double horasDeLuz(int dia) {
        return 12.0 + 2.95 * cicloLuz(dia);
    }

    /** Declinación del sol (grados): +23,44 en el solsticio de verano. */
    public static double declinacion(int dia) {
        return 23.44 * Math.sin(2 * Math.PI * dia / DIAS_ANIO);
    }

    /** Ángulo del recorrido del sol respecto a la vertical (latitud - declinación). */
    public static double inclinacionSol(int dia) {
        return LATITUD - declinacion(dia);
    }

    // ------------------------------------------------------------------ dormir en la cama (con el tiempo propio)
    @SubscribeEvent
    public static void antesDelTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        MinecraftServer s = e.getServer();
        if (s == null) return;
        ServerLevel ow = s.overworld();
        Clima c = Clima.get(s);
        if (!c.tiempoPropio || ow.players().isEmpty()) return;
        int dormidos = 0;
        for (ServerPlayer p : ow.players()) if (p.isSleepingLongEnough()) dormidos++;
        int pct = ow.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (dormidos > 0 && dormidos * 100 >= ow.players().size() * Math.max(1, pct)) {
            long t = ow.getDayTime();
            ow.setDayTime(t + 24000 - t % 24000);
            if (ow.isRaining()) ow.setWeatherParameters(0, 0, false, false);
            enviarHora(s);
        }
    }

    // ------------------------------------------------------------------ cada tick
    public static void tick(MinecraftServer s) {
        ServerLevel ow = s.overworld();
        Clima c = Clima.get(s);
        GameRules.BooleanValue ciclo = ow.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        if (c.tiempoPropio) {
            // llevamos nosotros el reloj: días largos en verano y cortos en invierno
            if (ciclo.get()) ciclo.set(false, s);
            int d = dia(ow.getDayTime(), c.desfaseDias);
            double luz = horasDeLuz(d);
            boolean deDia = ow.getDayTime() % 24000 < 12000;
            acumulado += deDia ? 12.0 / luz : 12.0 / (24.0 - luz);
            long paso = (long) Math.floor(acumulado);
            acumulado -= paso;
            if (paso > 0) ow.setDayTime(ow.getDayTime() + paso);
            if (s.getTickCount() % 10 == 0) enviarHora(s);
        }
        if (s.getTickCount() % 600 == 0) tiempoDeLaEstacion(ow, c, ow.random);
    }

    private static void enviarHora(MinecraftServer s) {
        ServerLevel ow = s.overworld();
        ClientboundSetTimePacket p = new ClientboundSetTimePacket(ow.getGameTime(), ow.getDayTime(), true);
        for (ServerPlayer j : ow.players()) j.connection.send(p);
    }

    /** Lluvias según la estación y avisos del calendario. */
    private static void tiempoDeLaEstacion(ServerLevel ow, Clima c, RandomSource r) {
        int d = dia(ow.getDayTime(), c.desfaseDias);
        int est = estacion(d);
        boolean llueve = ow.isRaining();
        if (est == 1) {
            // verano: seco; a finales, tormentas cortas de calor
            if (llueve && !ow.isThundering() && r.nextFloat() < 0.35f) ow.setWeatherParameters(9000 + r.nextInt(12000), 0, false, false);
            if (!llueve && d > 330 && r.nextFloat() < 0.012f) ow.setWeatherParameters(0, 2400 + r.nextInt(2400), true, true);
        } else if (!llueve) {
            float p = est == 3 ? (navidad(d) ? 0.07f : 0.03f) : 0.025f;
            if (r.nextFloat() < p) ow.setWeatherParameters(0, 6000 + r.nextInt(9000), true, false);
        }
        // avisos de cambio de estación y Navidad (una vez al día, a primera hora)
        int ultimo = c.ultimoDiaAvisado;
        if (ultimo != d && ow.getDayTime() % 24000 < 2000) {
            c.ultimoDiaAvisado = d;
            c.setDirty();
            String msg = null;
            if (d % DIAS_ESTACION == 0) msg = "Empieza " + (est == 2 ? "el otoño" : "la " + NOMBRES[est].toLowerCase());
            if (d == 608) msg = "¡Feliz Navidad!";
            if (d == 625) msg = "¡Feliz Año Nuevo!";
            if (msg != null) {
                for (ServerPlayer p : ow.players()) {
                    p.sendSystemMessage(Component.literal("☀ " + msg).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
            }
        }
    }
}
