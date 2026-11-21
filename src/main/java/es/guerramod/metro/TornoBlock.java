package es.guerramod.metro;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Torno de acceso: pasa el billete (clic derecho con él) y se abre unos segundos. */
public class TornoBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape COLUMNA_NS = Block.box(0, 0, 3, 4, 16, 13), COLUMNA_EO = Block.box(3, 0, 0, 13, 16, 4);
    private static final VoxelShape PUERTA_NS = Block.box(4, 4, 7, 16, 16, 9), PUERTA_EO = Block.box(7, 4, 4, 9, 16, 16);

    public TornoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
    }

    private static boolean ns(BlockState s) {
        return s.getValue(FACING).getAxis() == Direction.Axis.Z;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape col = ns(s) ? COLUMNA_NS : COLUMNA_EO;
        return s.getValue(OPEN) ? col : Shapes.or(col, ns(s) ? PUERTA_EO : PUERTA_NS);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape col = ns(s) ? COLUMNA_NS : COLUMNA_EO;
        if (s.getValue(OPEN)) return col;
        VoxelShape puerta = ns(s) ? Block.box(4, 0, 7, 16, 24, 9) : Block.box(7, 0, 4, 9, 24, 16);
        return Shapes.or(col, puerta);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        if (s.getValue(OPEN)) return InteractionResult.PASS;
        ItemStack st = pl.getItemInHand(h);
        if (!st.is(BloquesMetro.BILLETE.get())) {
            if (!l.isClientSide) {
                pl.displayClientMessage(Component.literal("Necesitas un billete de metro (máquina de billetes)").withStyle(ChatFormatting.RED), true);
                l.playSound(null, p, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        if (!l.isClientSide) {
            if (!pl.getAbilities().instabuild) st.shrink(1);
            l.setBlock(p, s.setValue(OPEN, true), Block.UPDATE_ALL);
            l.scheduleTick(p, this, 60);
            l.playSound(null, p, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.7f, 1.6f);
            l.playSound(null, p, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6f, 1.3f);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (s.getValue(OPEN)) {
            l.setBlock(p, s.setValue(OPEN, false), Block.UPDATE_ALL);
            l.playSound(null, p, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 1.3f);
        }
    }
}
