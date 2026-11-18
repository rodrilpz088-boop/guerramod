package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Sofá por módulos: si pones varios en fila se unen y desaparecen los brazos del medio. */
public class SofaBlock extends MuebleBlock {
    public static final BooleanProperty IZQ = BooleanProperty.create("izq");
    public static final BooleanProperty DER = BooleanProperty.create("der");

    public SofaBlock(Properties p, double[][] forma, double asiento) {
        super(p, forma, asiento, 0, Accion.NINGUNA);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(IZQ, false).setValue(DER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(IZQ, DER);
    }

    private BlockState unir(BlockState st, LevelAccessor level, BlockPos pos) {
        Direction f = st.getValue(FACING);
        BlockState izq = level.getBlockState(pos.relative(f.getCounterClockWise()));
        BlockState der = level.getBlockState(pos.relative(f.getClockWise()));
        return st.setValue(IZQ, izq.getBlock() instanceof SofaBlock && izq.getValue(FACING) == f)
                .setValue(DER, der.getBlock() instanceof SofaBlock && der.getValue(FACING) == f);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return unir(super.getStateForPlacement(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState st, Direction dir, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos posVecino) {
        return unir(st, level, pos);
    }
}
