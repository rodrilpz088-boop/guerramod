package es.guerramod.policia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rotativo azul de emergencia (destella). */
public class LuzEmergenciaBlock extends Block {
    private static final VoxelShape FORMA = Block.box(4, 0, 4, 12, 7, 12);

    public LuzEmergenciaBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORMA;
    }
}
