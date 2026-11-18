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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tira LED que se pega a cualquier cara (suelo, pared o techo). */
public class TiraLedBlock extends DirectionalBlock implements Luminaria {
    private static final VoxelShape ARRIBA = Block.box(0, 0, 6, 16, 1, 10);
    private static final VoxelShape ABAJO = Block.box(0, 15, 6, 16, 16, 10);
    private static final VoxelShape NORTE = Block.box(0, 6, 15, 16, 10, 16);
    private static final VoxelShape SUR = Block.box(0, 6, 0, 16, 10, 1);
    private static final VoxelShape ESTE = Block.box(0, 6, 0, 1, 10, 16);
    private static final VoxelShape OESTE = Block.box(15, 6, 0, 16, 10, 16);

    public TiraLedBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(BlockStateProperties.LIT, true).setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, BlockStateProperties.LIT, COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return switch (st.getValue(FACING)) {
            case DOWN -> ABAJO;
            case NORTH -> NORTE;
            case SOUTH -> SUR;
            case EAST -> ESTE;
            case WEST -> OESTE;
            default -> ARRIBA;
        };
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, st.cycle(BlockStateProperties.LIT), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }
}
