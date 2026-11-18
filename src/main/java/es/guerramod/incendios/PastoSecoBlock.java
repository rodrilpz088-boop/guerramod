package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pasto seco: hierba alta agostada del verano. Arde muchísimo. */
public class PastoSecoBlock extends BushBlock {
    private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 13, 14);

    public PastoSecoBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }
}
