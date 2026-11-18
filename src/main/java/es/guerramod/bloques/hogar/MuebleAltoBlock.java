package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Mueble de dos bloques de alto (frigorífico, armario ropero, sombrilla...). */
public class MuebleAltoBlock extends MuebleBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private final VoxelShape[] formasArriba;

    public MuebleAltoBlock(Properties p, double[][] abajo, double[][] arriba, int huecos) {
        super(p, abajo, -1, huecos, Accion.NINGUNA);
        this.formasArriba = Formas.crear(arriba);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return super.getStateForPlacement(ctx).setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState st, @Nullable LivingEntity quien, ItemStack item) {
        level.setBlock(pos.above(), st.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState st, Direction dir, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos posVecino) {
        DoubleBlockHalf half = st.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
            return vecino.is(this) && vecino.getValue(HALF) != half ? st : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(st, dir, vecino, level, pos, posVecino);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState st, LevelReader level, BlockPos pos) {
        if (st.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState abajo = level.getBlockState(pos.below());
            return abajo.is(this) && abajo.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState st, Player p) {
        if (!level.isClientSide && p.isCreative() && st.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos abajo = pos.below();
            BlockState sa = level.getBlockState(abajo);
            if (sa.is(this) && sa.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(abajo, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                level.levelEvent(p, 2001, abajo, Block.getId(sa));
            }
        }
        super.playerWillDestroy(level, pos, st, p);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        int i = st.getValue(FACING).get2DDataValue();
        return st.getValue(HALF) == DoubleBlockHalf.UPPER ? formasArriba[i] : formas[i];
    }

    @Override
    protected BlockPos posBase(BlockState st, BlockPos pos) {
        return st.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return st.getValue(HALF) == DoubleBlockHalf.LOWER ? super.newBlockEntity(pos, st) : null;
    }
}
