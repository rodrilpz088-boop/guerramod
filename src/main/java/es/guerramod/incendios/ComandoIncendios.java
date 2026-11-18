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
                        }))));
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        Clima cl = Clima.get(c.getSource().getServer());
        ServerLevel l = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        int rg = cl.riesgo(l, pos);
        c.getSource().sendSuccess(() -> Component.literal(String.format("Viento del %s a %d km/h · %.1f días sin llover · Riesgo: %s · Rayos: %s",
                Clima.procedencia(cl.angulo), Math.round(cl.velocidad), cl.diasSinLluvia(l), Clima.nombreRiesgo(rg),
                cl.naturales ? "sí" : "no")), false);
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
