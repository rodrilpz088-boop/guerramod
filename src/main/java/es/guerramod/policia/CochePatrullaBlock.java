package es.guerramod.policia;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Coche patrulla aparcado (decorativo, 3 bloques de largo; se pone en el centro). Clic derecho: enciende o apaga las
 * luces azules y la sirena.
 */
public class CochePatrullaBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CochePatrullaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
    }

    private static final java.util.Map<BlockPos, Long> SIRENA = new java.util.HashMap<>();

    /** Sirena de dos tonos durante 8 segundos al encender las luces. */
    @Override
    public void tick(BlockState s, net.minecraft.server.level.ServerLevel l, BlockPos p, net.minecraft.util.RandomSource r) {
        Long ini = SIRENA.get(p);
        if (ini == null || !s.getValue(LIT)) {
            SIRENA.remove(p);
            return;
        }
        long t = l.getGameTime() - ini;
        if (t > 160) {
            SIRENA.remove(p);
            return;
        }
        boolean alto = (t / 10) % 2 == 0;
        l.playSound(null, p, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 3f, alto ? 1.45f : 1.05f);
        l.playSound(null, p, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.BLOCKS, 1.5f, alto ? 1.45f : 1.05f);
        l.scheduleTick(p, this, 10);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        if (!l.isClientSide) {
            boolean on = !s.getValue(LIT);
            l.setBlock(p, s.setValue(LIT, on), Block.UPDATE_ALL);
            if (on) {
                SIRENA.put(p.immutable(), l.getGameTime());
                l.scheduleTick(p, this, 1);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
