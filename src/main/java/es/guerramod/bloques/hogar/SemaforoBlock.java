package es.guerramod.bloques.hogar;

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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Semáforo que funciona solo. Todos los semáforos del mundo van sincronizados con la hora del juego:
 * verde 30 s, ámbar 3 s, rojo 33 s. Clic derecho cambia entre fase A y fase B (la B está en rojo cuando la A
 * está en verde), así se montan cruces. El de peatones se pone verde cuando los coches tienen rojo.
 */
public class SemaforoBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty LUZ = IntegerProperty.create("luz", 0, 2); // 0 rojo, 1 ámbar, 2 verde
    public static final BooleanProperty FASE_B = BooleanProperty.create("fase_b");
    private static final int VERDE = 600, AMBAR = 60, CICLO = 1320;
    private static final VoxelShape[] FORMA = Formas.crear(new double[][]{{7, 0, 7, 9, 16, 9}, {4.5, 1, 5, 11.5, 16, 9}});

    public final boolean peatonal;

    public SemaforoBlock(Properties p, boolean peatonal) {
        super(p);
        this.peatonal = peatonal;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LUZ, 0).setValue(FASE_B, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LUZ, FASE_B);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState st = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return st.setValue(LUZ, luz(ctx.getLevel(), st));
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return FORMA[st.getValue(FACING).get2DDataValue()];
    }

    private int luz(Level level, BlockState st) {
        boolean b = st.getValue(FASE_B);
        // el semáforo de peatones va al revés que el de coches de su misma fase
        if (peatonal) b = !b;
        long t = (level.getGameTime() + (b ? VERDE + AMBAR : 0)) % CICLO;
        int l = t < VERDE ? 2 : t < VERDE + AMBAR ? 1 : 0;
        if (peatonal && l == 1) l = 0;
        return l;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState st, Level level, BlockPos pos, BlockState viejo, boolean moviendo) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        int l = luz(level, st);
        if (l != st.getValue(LUZ)) {
            level.setBlock(pos, st.setValue(LUZ, l), Block.UPDATE_ALL);
            if (peatonal && l == 2) level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.3f, 1.8f);
        }
        level.scheduleTick(pos, this, 10);
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState n = st.cycle(FASE_B);
            n = n.setValue(LUZ, luz(level, n));
            level.setBlock(pos, n, Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 2);
            p.displayClientMessage(Component.literal("Semáforo en fase " + (n.getValue(FASE_B) ? "B" : "A")
                    + " (pon la calle que cruza en la otra fase)").withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }
}
