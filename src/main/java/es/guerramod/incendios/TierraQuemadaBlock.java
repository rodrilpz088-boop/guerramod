package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Tierra quemada: negra, no arde (hace de cortafuegos natural) y con el tiempo vuelve a salir la hierba. */
public class TierraQuemadaBlock extends Block {
    public TierraQuemadaBlock(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if (!level.getBlockState(pos.above()).isAir() && !(level.getBlockState(pos.above()).getBlock() instanceof CenizaBlock)) return;
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        int prob = level.isRainingAt(pos.above()) ? 60 : 160;
        if (r.nextInt(prob) == 0) level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
    }
}
