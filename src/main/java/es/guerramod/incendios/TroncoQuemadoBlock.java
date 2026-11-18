package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tronco carbonizado. Con los años se pudre y se cae (empezando por arriba). Donde estaba la base del árbol
 * a veces vuelve a brotar uno nuevo. La lluvia lo acelera.
 */
public class TroncoQuemadoBlock extends RotatedPillarBlock {
    public TroncoQuemadoBlock(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        // solo se cae el de más arriba de la columna
        if (level.getBlockState(pos.above()).getBlock() instanceof TroncoQuemadoBlock) return;
        boolean lluvia = level.isRainingAt(pos.above());
        if (r.nextInt(lluvia ? 90 : 300) != 0) return;
        BlockState abajo = level.getBlockState(pos.below());
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.6f, 0.6f);
        if (abajo.is(BlockTags.DIRT) && r.nextFloat() < 0.35f) {
            level.setBlock(pos, (r.nextBoolean() ? Blocks.OAK_SAPLING : Blocks.BIRCH_SAPLING).defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // las ramas quemadas de alrededor caen con él
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            if (level.getBlockState(np).getBlock() instanceof RamasQuemadasBlock && r.nextBoolean()) level.removeBlock(np, false);
        }
    }
}
