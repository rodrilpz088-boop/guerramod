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

    /** Con lluvia se forman charcos, sobre todo en los huecos y zonas bajas del terreno. */
    private static void charcos(ServerLevel ow, RandomSource r) {
        BlockState charco = es.guerramod.ciudad.BloquesCiudad.CHARCO.get().defaultBlockState();
        int intentos = ow.isThundering() ? 4 : 2;
        for (ServerPlayer p : ow.players()) {
            for (int i = 0; i < intentos; i++) {
                int x = p.getBlockX() + r.nextInt(81) - 40, z = p.getBlockZ() + r.nextInt(81) - 40;
                if (!ow.hasChunkAt(new BlockPos(x, p.getBlockY(), z))) continue;
                BlockPos arriba = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (!ow.isRainingAt(arriba)) continue;
                // hueco: los cuatro vecinos están igual o más altos
                int h = arriba.getY(), altos = 0;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    if (ow.getHeight(Heightmap.Types.MOTION_BLOCKING, x + d.getStepX(), z + d.getStepZ()) >= h) altos++;
                }
                float prob = altos == 4 ? 0.22f : altos == 3 ? 0.06f : 0.012f;
                if (r.nextFloat() < prob) es.guerramod.clima.CharcoBlock.intentar(ow, arriba, charco);
            }
        }
    }

    /**
     * Cada tick: columnas al azar alrededor de cada jugador (muchas más mientras nieva, para que se vea cuajar). En cada
     * una la nieve cuaja, se funde o el agua se hiela (ver {@link Nieve}).
     */
    public static void tick(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        RandomSource r = ow.random;
        int n = es.guerramod.incendios.Clima.get(server).ventisca() > 0 ? 90 : ow.isRaining() ? 45 : 8;
        for (ServerPlayer p : ow.players()) {
            for (int i = 0; i < n; i++) {
                int x = p.getBlockX() + r.nextInt(97) - 48, z = p.getBlockZ() + r.nextInt(97) - 48;
                if (!ow.hasChunkAt(new BlockPos(x, p.getBlockY(), z))) continue;
                BlockPos arriba = Nieve.cima(ow, x, z);
                Nieve.columna(ow, arriba, Meteo.en(ow, arriba), r);
            }
        }
    }
}
