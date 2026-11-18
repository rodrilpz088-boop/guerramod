package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Persiana enrollable: clic derecho para subirla (se recoge en el cajón) o bajarla. También con redstone. */
public class PersianaBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape[] BAJADA = Formas.crear(new double[][]{{0, 0, 7, 16, 16, 9}});
    private static final VoxelShape[] SUBIDA = Formas.crear(new double[][]{{0, 12, 6, 16, 16, 10}});

    public PersianaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, BlockStateProperties.OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return (st.getValue(BlockStateProperties.OPEN) ? SUBIDA : BAJADA)[st.getValue(FACING).get2DDataValue()];
    }

    private void poner(Level level, BlockPos pos, BlockState st, boolean abierta) {
        if (st.getValue(BlockStateProperties.OPEN) == abierta) return;
        level.setBlock(pos, st.setValue(BlockStateProperties.OPEN, abierta), Block.UPDATE_ALL);
        level.playSound(null, pos, abierta ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.7f);
        // las persianas de encima y debajo (misma ventana) se mueven juntas
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN}) {
            BlockPos otra = pos.relative(d);
            BlockState so = level.getBlockState(otra);
            if (so.is(this) && so.getValue(FACING) == st.getValue(FACING)) poner(level, otra, so, abierta);
        }
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) poner(level, pos, st, !st.getValue(BlockStateProperties.OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState st, Level level, BlockPos pos, Block b, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean senal = level.hasNeighborSignal(pos);
        if (senal) poner(level, pos, st, true);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return st.getValue(BlockStateProperties.OPEN);
    }
}
