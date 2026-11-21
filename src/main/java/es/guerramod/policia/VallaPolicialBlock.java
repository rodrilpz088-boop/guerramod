package es.guerramod.policia;

import es.guerramod.vivienda.PlacaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Valla metálica de contención con la placa «POLICÍA» (no se salta: colisión de bloque y medio). */
public class VallaPolicialBlock extends PlacaBlock {
    public VallaPolicialBlock(Properties p) {
        super(p, 2, 0, 16, true);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 13, 16, 24, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 24, 3);
            case WEST -> Block.box(13, 0, 0, 16, 24, 16);
            default -> Block.box(0, 0, 0, 3, 24, 16);
        };
    }
}
