package es.guerramod.clima;

import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import es.guerramod.red.Red;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Efectos del clima en el mundo: la nieve y el hielo se derriten con el calor, y el tiempo se manda a los jugadores. */
public final class EfectosClima {
    private EfectosClima() {}

    /** Cada segundo: el tiempo local a cada jugador del mundo normal. */
    public static void enviarTiempo(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        Clima c = Clima.get(server);
        for (ServerPlayer p : ow.players()) {
            Meteo m = Meteo.en(ow, p.blockPosition());
            Red.enviarTiempo(p, m, c);
        }
    }

    /** Cada tick: algunas columnas al azar alrededor de cada jugador; si hace calor la nieve y el hielo se funden. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 2 != 0) return;
        ServerLevel ow = server.overworld();
        RandomSource r = ow.random;
        for (ServerPlayer p : ow.players()) {
            for (int i = 0; i < 4; i++) {
                int x = p.getBlockX() + r.nextInt(97) - 48, z = p.getBlockZ() + r.nextInt(97) - 48;
                if (!ow.hasChunkAt(new BlockPos(x, p.getBlockY(), z))) continue;
                BlockPos arriba = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                double t = Meteo.en(ow, arriba).temperatura();
                if (t < 4.5) continue;
                // probabilidad de fundirse: más calor, más rápido; el sol de mediodía también ayuda
                double prob = Math.min(1.0, (t - 4.5) / 14.0);
                if (r.nextDouble() > prob) continue;
                BlockState s = ow.getBlockState(arriba);
                BlockPos debajo = arriba.below();
                BlockState sd = ow.getBlockState(debajo);
                if (s.is(Blocks.SNOW)) {
                    int capas = s.getValue(SnowLayerBlock.LAYERS);
                    ow.setBlock(arriba, capas > 1 ? s.setValue(SnowLayerBlock.LAYERS, capas - 1) : Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL);
                } else if (sd.is(Blocks.SNOW_BLOCK) || sd.is(Blocks.POWDER_SNOW)) {
                    if (t > 8) ow.setBlock(debajo, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), Block.UPDATE_ALL);
                } else if (sd.is(Blocks.ICE) || sd.is(Blocks.FROSTED_ICE)) {
                    if (t > 6) ow.setBlock(debajo, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                } else if (sd.is(Blocks.PACKED_ICE) && t > 14 && r.nextInt(6) == 0) {
                    ow.setBlock(debajo, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
