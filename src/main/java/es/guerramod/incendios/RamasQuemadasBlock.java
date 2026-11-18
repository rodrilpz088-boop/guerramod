package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Ramas quemadas de la copa: con el tiempo se caen. */
public class RamasQuemadasBlock extends Block {
    public RamasQuemadasBlock(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if (r.nextInt(level.isRainingAt(pos.above()) ? 8 : 25) == 0) level.removeBlock(pos, false);
    }
}
