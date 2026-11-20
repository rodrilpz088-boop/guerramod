package es.guerramod.clima;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * /clima                     tiempo aquí, CO2 y calentamiento
 * /clima cambio on|off       activa o desactiva el cambio climático (op)
 * /clima co2 <ppm>           pone el CO2 (op)
 * /clima calentar <°C>       pone el calentamiento global directamente (op)
 * /clima nubes <%>|auto      fija la nubosidad (op)
 * /clima reiniciar           CO2 420 ppm, sin calentamiento ni humo (op)
 */
public final class ComandoClima {
    private ComandoClima() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("clima")
                .executes(ComandoClima::info)
                .then(Commands.literal("info").executes(ComandoClima::info))
                .then(Commands.literal("cambio").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("activar", BoolArgumentType.bool()).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.cambio = BoolArgumentType.getBool(c, "activar");
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, cl.cambio ? "Cambio climático ACTIVADO: las fábricas, centrales e incendios emiten CO2 y el mundo se calienta"
                                    : "Cambio climático DESACTIVADO: clima estable");
                        })))
                .then(Commands.literal("co2").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("ppm", FloatArgumentType.floatArg(280, 3000)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.co2 = FloatArgumentType.getFloat(c, "ppm");
                            cl.cambiado(c.getSource().getServer());
                            double eq = Clima.SENSIBILIDAD * Math.log(cl.co2 / 420.0) / Math.log(2);
                            return ok(c, String.format("CO2 en %.0f ppm. El mundo irá hacia %+.1f °C en unos días de juego", cl.co2, eq));
                        })))
                .then(Commands.literal("calentar").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("grados", FloatArgumentType.floatArg(-3, 10)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.anomalia = FloatArgumentType.getFloat(c, "grados");
                            cl.co2 = 420.0 * Math.pow(2, cl.anomalia / Clima.SENSIBILIDAD);
                            cl.cambio = true;
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, String.format("Calentamiento global de %+.1f °C (CO2 %.0f ppm). Las zonas frías se calientan casi el doble",
                                    cl.anomalia, cl.co2));
                        })))
                .then(Commands.literal("nubes").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("auto").executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.nubesFija = Float.NaN;
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, "Nubes automáticas");
                        }))
                        .then(Commands.argument("porcentaje", FloatArgumentType.floatArg(0, 100)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.nubesFija = FloatArgumentType.getFloat(c, "porcentaje");
                            cl.nubes = cl.nubesFija / 100.0;
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, String.format("Nubosidad fijada en %.0f %%", cl.nubesFija));
                        })))
                .then(Commands.literal("reiniciar").requires(s -> s.hasPermission(2)).executes(c -> {
                    Clima cl = Clima.get(c.getSource().getServer());
                    cl.co2 = 420;
                    cl.anomalia = 0;
                    cl.aerosol = 0;
                    cl.cambiado(c.getSource().getServer());
                    return ok(c, "Clima reiniciado: 420 ppm de CO2, sin calentamiento ni humo");
                })));
    }

    private static int ok(CommandContext<CommandSourceStack> c, String texto) {
        c.getSource().sendSuccess(() -> Component.literal(texto).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    public static String cielo(double nubes, double niebla, boolean lluvia) {
        if (lluvia) return "lluvia";
        if (niebla > 0.5) return "niebla";
        if (niebla > 0.2) return "neblina";
        if (nubes < 0.12) return "despejado";
        if (nubes < 0.35) return "poco nuboso";
        if (nubes < 0.65) return "nubes y claros";
        if (nubes < 0.88) return "nuboso";
        return "cubierto";
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        MinecraftServer srv = c.getSource().getServer();
        Clima cl = Clima.get(srv);
        ServerLevel l = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        Meteo m = Meteo.en(l, pos);
        c.getSource().sendSuccess(() -> Component.literal("— " + m.zona() + " —").withStyle(ChatFormatting.AQUA), false);
        c.getSource().sendSuccess(() -> Component.literal(String.format(
                "%.1f °C · humedad %.0f %% · punto de rocío %.1f °C · %s · viento %.0f km/h",
                m.temperatura(), m.humedad(), m.puntoRocio(), cielo(m.nubes(), m.niebla(), llueve(l, pos)), m.viento())), false);
        String calor = cl.cambio ? String.format("%+.2f °C", cl.anomalia) : "desactivado";
        c.getSource().sendSuccess(() -> Component.literal(String.format(
                "CO2 %.0f ppm · calentamiento global %s · humo en el aire %.0f %% · riesgo de incendio %s",
                cl.co2, calor, Math.min(100, cl.aerosol * 100), Clima.nombreRiesgo(m.riesgo())))
                .withStyle(cl.calentamiento() > 1.5 ? ChatFormatting.RED : cl.calentamiento() > 0.5 ? ChatFormatting.GOLD : ChatFormatting.GREEN), false);
        return 1;
    }

    private static boolean llueve(ServerLevel l, BlockPos pos) {
        return es.guerramod.incendios.Fuego.llueve(l, pos);
    }
}
