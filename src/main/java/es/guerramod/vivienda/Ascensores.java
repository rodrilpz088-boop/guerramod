package es.guerramod.vivienda;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ascensor: pon un bloque de ascensor en cada planta, en la misma columna. Encima de uno, salta para subir a la planta
 * de arriba o agáchate para bajar. Suena el timbre y se ve en qué planta estás.
 */
public final class Ascensores {
    private Ascensores() {}

    private static final Map<UUID, Long> ULTIMO = new HashMap<>();

    public static boolean esAscensor(BlockState s) {
        return s.getBlock() instanceof AscensorBlock;
    }

    public static void mover(ServerPlayer p, boolean subir) {
        ServerLevel l = p.serverLevel();
        long t = l.getGameTime();
        if (t - ULTIMO.getOrDefault(p.getUUID(), 0L) < 8) return;
        BlockPos pie = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        if (!esAscensor(l.getBlockState(pie))) return;
        int paso = subir ? 1 : -1;
        for (int d = 2; d <= 96; d++) {
            BlockPos q = pie.above(d * paso);
            if (q.getY() < l.getMinBuildHeight() || q.getY() >= l.getMaxBuildHeight()) break;
            if (!esAscensor(l.getBlockState(q))) continue;
            if (!l.getBlockState(q.above()).getCollisionShape(l, q.above()).isEmpty()
                    || !l.getBlockState(q.above(2)).getCollisionShape(l, q.above(2)).isEmpty()) continue;
            ULTIMO.put(p.getUUID(), t);
            l.playSound(null, pie, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.25f, 1.6f);
            p.teleportTo(p.getX(), q.getY() + 1.0, p.getZ());
            p.fallDistance = 0;
            l.playSound(null, q, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.7f, 1.2f);
            l.playSound(null, q, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5f, 0.9f);
            p.displayClientMessage(Component.literal((subir ? "▲ " : "▼ ") + "Planta " + planta(l, q)), true);
            return;
        }
    }

    /** Número de planta: cuántos ascensores hay por debajo en la columna (la baja es la 0). */
    private static int planta(ServerLevel l, BlockPos q) {
        int n = 0;
        for (int d = 2; d <= 256; d++) {
            BlockPos b = q.below(d);
            if (b.getY() < l.getMinBuildHeight()) break;
            if (esAscensor(l.getBlockState(b))) n++;
        }
        return n;
    }
}
