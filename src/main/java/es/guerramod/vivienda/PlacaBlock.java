package es.guerramod.vivienda;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placa colgada en la pared (carteles, paneles, adornos de luces). Mira hacia donde está el jugador al ponerla; el
 * modelo está dibujado mirando al norte con la placa pegada al lado sur del bloque.
 */
public class PlacaBlock extends HorizontalDirectionalBlock {
    private final VoxelShape[] formas = new VoxelShape[4];
    private final boolean colision;

    public PlacaBlock(Properties p, double grosor, double y0, double y1, boolean colision) {
        super(p);
        this.colision = colision;
        formas[Direction.NORTH.get2DDataValue()] = Block.box(0, y0, 16 - grosor, 16, y1, 16);
        formas[Direction.SOUTH.get2DDataValue()] = Block.box(0, y0, 0, 16, y1, grosor);
        formas[Direction.WEST.get2DDataValue()] = Block.box(16 - grosor, y0, 0, 16, y1, 16);
        formas[Direction.EAST.get2DDataValue()] = Block.box(0, y0, 0, grosor, y1, 16);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        Direction d = c.getClickedFace();
        if (d.getAxis().isVertical()) d = c.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, d);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return formas[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return colision ? getShape(s, l, p, c) : Shapes.empty();
    }
}
