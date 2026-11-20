package es.guerramod.ciudad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Obras en marcha: el edificio se levanta solo, planta a planta, en unos segundos. */
public final class Obras {
    private Obras() {}

    private record Paso(BlockPos pos, BlockState st) {}

    private static final class Obra {
        ServerLevel level;
        List<Paso> despejar = new ArrayList<>(), bloques = new ArrayList<>();
        int i, j;
        Edificio edificio;
        UUID jugador;
        BlockPos centro;
    }

    private static final List<Obra> OBRAS = new ArrayList<>();
    private static final int DESPEJAR_POR_TICK = 2500, BLOQUES_POR_TICK = 260;

    public static void encolar(ServerLevel level, BlockPos origen, Direction mirando, Edificio e, ServerPlayer p) {
        Plano plano = e.plano();
        Rotation rot = Plano.rotacion(mirando);
        Obra o = new Obra();
        o.level = level;
        o.edificio = e;
        o.jugador = p.getUUID();
        o.centro = Plano.aMundo(origen, rot, 0, 0, plano.fondo() / 2);
        BlockState cimiento = Blocks.STONE.defaultBlockState();
        for (Map.Entry<BlockPos, BlockState> en : plano.bloques.entrySet()) {
            BlockPos l = en.getKey();
            BlockPos w = Plano.aMundo(origen, rot, l.getX(), l.getY(), l.getZ());
            if (en.getValue().isAir()) {
                o.despejar.add(new Paso(w, en.getValue()));
                continue;
            }
            o.bloques.add(new Paso(w, en.getValue().rotate(rot)));
            if (l.getY() == 0) {
                // cimentación hasta el terreno firme
                for (int d = 1; d <= 14; d++) {
                    BlockPos q = w.below(d);
                    BlockState sq = level.getBlockState(q);
                    if (!(sq.isAir() || sq.canBeReplaced() || !sq.getFluidState().isEmpty() || sq.is(net.minecraft.tags.BlockTags.LEAVES))) break;
                    o.bloques.add(new Paso(q, cimiento));
                }
            }
        }
        // primero lo de abajo, para que se vea crecer
        o.despejar.sort(Comparator.comparingInt((Paso a) -> -a.pos.getY()));
        o.bloques.sort(Comparator.comparingInt((Paso a) -> a.pos.getY()));
        OBRAS.add(o);
    }

    public static void tick(MinecraftServer server) {
        if (OBRAS.isEmpty()) return;
        Iterator<Obra> it = OBRAS.iterator();
        while (it.hasNext()) {
            Obra o = it.next();
            ServerLevel l = o.level;
            int hechos = 0;
            while (o.i < o.despejar.size() && hechos < DESPEJAR_POR_TICK) {
                Paso p = o.despejar.get(o.i++);
                if (!l.getBlockState(p.pos).isAir()) l.setBlock(p.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                hechos++;
            }
            if (o.i < o.despejar.size()) continue;
            int n = 0;
            while (o.j < o.bloques.size() && n < BLOQUES_POR_TICK) {
                Paso p = o.bloques.get(o.j++);
                l.setBlock(p.pos, p.st, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                n++;
            }
            if (n > 0 && l.getGameTime() % 4 == 0) {
                Paso p = o.bloques.get(Math.max(0, o.j - 1));
                l.playSound(null, p.pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 0.8f + l.random.nextFloat() * 0.4f);
                l.sendParticles(ParticleTypes.CLOUD, p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0.01);
            }
            if (o.j >= o.bloques.size()) {
                terminar(o);
                it.remove();
            }
        }
    }

    private static void terminar(Obra o) {
        ServerLevel l = o.level;
        // conectar cristales, barrotes, vallas, puertas...
        for (Paso p : o.bloques) {
            BlockState s = l.getBlockState(p.pos);
            BlockState n = Block.updateFromNeighbourShapes(s, l, p.pos);
            if (n != s) l.setBlock(p.pos, n, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        l.playSound(null, o.centro, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        ServerPlayer p = l.getServer().getPlayerList().getPlayer(o.jugador);
        if (p != null) {
            p.sendSystemMessage(Component.literal("✔ " + o.edificio.nombre + " terminado" + (o.edificio.contamina
                    ? ". Sus chimeneas emiten CO2: con muchas el mundo se calienta (/clima)" : ""))
                    .withStyle(o.edificio.contamina ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }
    }

    public static boolean ocupado() {
        return OBRAS.size() >= 4;
    }
}
