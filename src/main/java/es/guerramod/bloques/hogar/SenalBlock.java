package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Señal de tráfico sobre poste. Mira hacia quien la coloca. Se apila encima de postes. */
public class SenalBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape[] CONTORNO = Formas.crear(new double[][]{{7, 0, 7, 9, 16, 9}, {1, 1, 6, 15, 15, 7}});
    private static final VoxelShape[] CHOQUE = Formas.crear(new double[][]{{7, 0, 7, 9, 16, 9}});

    public SenalBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return CONTORNO[st.getValue(FACING).get2DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return CHOQUE[st.getValue(FACING).get2DDataValue()];
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }
}
