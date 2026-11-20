package es.guerramod.desastres;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import es.guerramod.incendios.Clima;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /desastre tornado [EF 0-5] · terremoto [magnitud] · granizo [cm] · inundacion · olacalor [días] · parar
 * /desastre naturales true|false   (que pasen solos)
 */
public final class ComandoDesastre {
    private ComandoDesastre() {}

    private static ServerLevel mundo(CommandContext<CommandSourceStack> c) {
        return c.getSource().getServer().overworld();
    }

    private static BlockPos pos(CommandContext<CommandSourceStack> c) {
        return BlockPos.containing(c.getSource().getPosition());
    }

    private static int ok(CommandContext<CommandSourceStack> c, String t) {
        c.getSource().sendSuccess(() -> Component.literal(t).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("desastre").requires(s -> s.hasPermission(2))
                .then(Commands.literal("tornado")
                        .executes(c -> {
                            Desastres.tornado(mundo(c), pos(c), 2);
                            return 1;
                        })
                        .then(Commands.argument("ef", IntegerArgumentType.integer(0, 5)).executes(c -> {
                            Desastres.tornado(mundo(c), pos(c), IntegerArgumentType.getInteger(c, "ef"));
                            return 1;
                        })))
                .then(Commands.literal("terremoto")
                        .executes(c -> {
                            Desastres.terremoto(mundo(c), pos(c).offset(20, 0, 15), 6.0);
                            return 1;
                        })
                        .then(Commands.argument("magnitud", FloatArgumentType.floatArg(3, 9.5f)).executes(c -> {
                            Desastres.terremoto(mundo(c), pos(c).offset(20, 0, 15), FloatArgumentType.getFloat(c, "magnitud"));
                            return 1;
                        })))
                .then(Commands.literal("granizo")
                        .executes(c -> {
                            Desastres.granizo(mundo(c), 3, 2400);
                            return 1;
                        })
                        .then(Commands.argument("cm", FloatArgumentType.floatArg(0.5f, 10)).executes(c -> {
                            Desastres.granizo(mundo(c), FloatArgumentType.getFloat(c, "cm"), 2400);
                            return 1;
                        })))
                .then(Commands.literal("inundacion").executes(c -> {
                    Desastres.riada(mundo(c));
                    return 1;
                }))
                .then(Commands.literal("olacalor")
                        .executes(c -> {
                            Desastres.olaCalor(mundo(c), 3);
                            return 1;
                        })
                        .then(Commands.argument("dias", FloatArgumentType.floatArg(0.5f, 30)).executes(c -> {
                            Desastres.olaCalor(mundo(c), FloatArgumentType.getFloat(c, "dias"));
                            return 1;
                        })))
                .then(Commands.literal("parar").executes(c -> {
                    Desastres.parar(mundo(c));
                    return ok(c, "Todos los desastres detenidos (el agua de las riadas irá bajando)");
                }))
                .then(Commands.literal("naturales").then(Commands.argument("activar", BoolArgumentType.bool()).executes(c -> {
                    Clima cl = Clima.get(c.getSource().getServer());
                    cl.desastres = BoolArgumentType.getBool(c, "activar");
                    cl.setDirty();
                    return ok(c, cl.desastres ? "Desastres naturales ACTIVADOS (tornados y granizo en tormentas, terremotos raros, riadas, olas de calor)"
                            : "Desastres naturales DESACTIVADOS");
                }))));
    }
}
