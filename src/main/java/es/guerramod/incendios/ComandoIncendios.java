package es.guerramod.incendios;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * /incendios info                     riesgo, viento, días sin llover
 * /incendios naturales on|off          rayos secos que provocan incendios (op)
 * /incendios provocar                 prende donde miras (op)
 * /incendios rayo                     tormenta seca cerca (op)
 * /incendios viento <grados> <km/h>   fija el viento (op)
 * /incendios apagar <radio>           apaga todo en el radio (op)
 * /incendios temperatura <°C>|auto    fija la temperatura de las zonas templadas; las demás guardan su diferencia (op)
 * /incendios humedad <%>|auto         fija la humedad del aire (op)
 * /incendios sequia <días>            días sin llover (lo seca que está la hierba) (op)
 * /incendios oladecalor               42 °C, 10 % de humedad, 30 días de sequía y viento de 35 km/h (op)
 * /incendios normal                   vuelve al tiempo automático (op)
 */
public final class ComandoIncendios {
    private ComandoIncendios() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("incendios")
                .executes(ComandoIncendios::info)
                .then(Commands.literal("info").executes(ComandoIncendios::info))
                .then(Commands.literal("naturales").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("activar", BoolArgumentType.bool()).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.naturales = BoolArgumentType.getBool(c, "activar");
                            cl.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal("Incendios naturales por rayos: " + (cl.naturales ? "SÍ" : "NO")), true);
                            return 1;
                        })))
                .then(Commands.literal("provocar").requires(s -> s.hasPermission(2)).executes(ComandoIncendios::provocar))
                .then(Commands.literal("rayo").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean ok = false;
                    for (int i = 0; i < 10 && !ok; i++) ok = Clima.rayoSeco(p.serverLevel(), p.blockPosition(), p.getRandom());
                    if (!ok) c.getSource().sendFailure(Component.literal("El rayo no ha encontrado vegetación que prender"));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("viento").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("grados", FloatArgumentType.floatArg(0, 360))
                                .then(Commands.argument("kmh", FloatArgumentType.floatArg(0, 80)).executes(c -> {
                                    Clima cl = Clima.get(c.getSource().getServer());
                                    cl.angulo = FloatArgumentType.getFloat(c, "grados");
                                    cl.velocidad = FloatArgumentType.getFloat(c, "kmh");
                                    cl.setDirty();
                                    es.guerramod.red.Red.sincronizarClima(c.getSource().getServer(), cl.angulo, cl.velocidad, 2);
                                    c.getSource().sendSuccess(() -> Component.literal("Viento del " + Clima.procedencia(cl.angulo) + " a "
                                            + Math.round(cl.velocidad) + " km/h"), true);
                                    return 1;
                                }))))
                .then(Commands.literal("apagar").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("radio", IntegerArgumentType.integer(1, 64)).executes(c -> {
                            int r = IntegerArgumentType.getInteger(c, "radio");
                            ServerLevel l = c.getSource().getLevel();
                            int n = Fuego.apagarZona(l, c.getSource().getPosition(), r, 2400);
                            c.getSource().sendSuccess(() -> Component.literal("Apagados " + n + " focos").withStyle(ChatFormatting.AQUA), true);
                            return n;
                        })))
                .then(Commands.literal("temperatura").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("auto").executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.tempFija = Float.NaN;
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, "Temperatura automática (según bioma, hora del día, altura y lluvia)");
                        }))
                        .then(Commands.argument("grados", FloatArgumentType.floatArg(-30, 60)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.tempFija = FloatArgumentType.getFloat(c, "grados");
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, String.format("Temperatura fijada en %.0f °C en las zonas templadas (las zonas frías y de nieve quedan más frescas, los desiertos más calurosos)", cl.tempFija));
                        })))
                .then(Commands.literal("humedad").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("auto").executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.humFija = Float.NaN;
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, "Humedad automática (según bioma, hora del día y lluvia)");
                        }))
                        .then(Commands.argument("porcentaje", FloatArgumentType.floatArg(1, 100)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            cl.humFija = FloatArgumentType.getFloat(c, "porcentaje");
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, String.format("Humedad del aire fijada en %.0f %% (si llueve sube igualmente)", cl.humFija));
                        })))
                .then(Commands.literal("sequia").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("dias", FloatArgumentType.floatArg(0, 365)).executes(c -> {
                            Clima cl = Clima.get(c.getSource().getServer());
                            float dias = FloatArgumentType.getFloat(c, "dias");
                            cl.ponerSequia(c.getSource().getServer().overworld(), dias);
                            cl.cambiado(c.getSource().getServer());
                            return ok(c, String.format("La hierba lleva %.0f días sin lluvia", dias));
                        })))
                .then(Commands.literal("oladecalor").requires(s -> s.hasPermission(2)).executes(c -> {
                    var srv = c.getSource().getServer();
                    Clima cl = Clima.get(srv);
                    cl.tempFija = 42;
                    cl.humFija = 10;
                    cl.velocidad = Math.max(cl.velocidad, 35);
                    cl.ponerSequia(srv.overworld(), 30);
                    srv.overworld().setWeatherParameters(24000 * 5, 0, false, false);
                    cl.cambiado(srv);
                    return ok(c, "Ola de calor: 42 °C, 10 % de humedad, 30 días sin llover y viento de "
                            + Math.round(cl.velocidad) + " km/h. Peligro extremo de incendio.");
                }))
                .then(Commands.literal("normal").requires(s -> s.hasPermission(2)).executes(c -> {
                    Clima cl = Clima.get(c.getSource().getServer());
                    cl.tempFija = Float.NaN;
                    cl.humFija = Float.NaN;
                    cl.cambiado(c.getSource().getServer());
                    return ok(c, "Tiempo automático otra vez (temperatura y humedad según bioma, hora y lluvia)");
                })));
    }

    private static int ok(CommandContext<CommandSourceStack> c, String texto) {
        c.getSource().sendSuccess(() -> Component.literal(texto).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        Clima cl = Clima.get(c.getSource().getServer());
        ServerLevel l = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        Meteo m = Meteo.en(l, pos);
        c.getSource().sendSuccess(() -> Component.literal(String.format(
                "%.0f °C%s · %.0f %% humedad%s · viento del %s a %.0f km/h · %.1f días sin llover · hierba seca %.0f %% · "
                        + "avance en pasto %.1f m/min · riesgo %s · rayos: %s",
                m.temperatura(), Float.isNaN(cl.tempFija) ? "" : " (fijada)", m.humedad(),
                Float.isNaN(cl.humFija) ? "" : " (fijada)", Clima.procedencia(cl.angulo), m.viento(), cl.diasSinLluvia(l), m.curado(),
                m.rosCabeza() * 60, Clima.nombreRiesgo(m.riesgo()), cl.naturales ? "sí" : "no")), false);
        return 1;
    }

    private static int provocar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        HitResult hit = p.pick(200, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            c.getSource().sendFailure(Component.literal("Mira a un sitio con vegetación"));
            return 0;
        }
        BlockHitResult bh = (BlockHitResult) hit;
        BlockPos pos = bh.getBlockPos().above();
        ServerLevel l = p.serverLevel();
        boolean hoja = l.getBlockState(bh.getBlockPos()).is(net.minecraft.tags.BlockTags.LEAVES);
        if (hoja) pos = bh.getBlockPos();
        l.setBlock(pos, BloquesIncendio.LLAMA.get().defaultBlockState().setValue(LlamaBlock.COPA, hoja).setValue(LlamaBlock.GRANDE, hoja),
                Block.UPDATE_ALL);
        c.getSource().sendSuccess(() -> Component.literal("Incendio provocado").withStyle(ChatFormatting.RED), true);
        return 1;
    }
}
