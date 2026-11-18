package es.guerramod.bloques.hogar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

/** Interruptor de pared: enciende o apaga todas las luces de la habitación (8 bloques). */
public class InterruptorBlock extends HorizontalDirectionalBlock {
    private static final double[][] CAJA = {{5.5, 4.5, 15, 10.5, 11.5, 16}};
    private final VoxelShape[] formas = Formas.crear(CAJA);

    public InterruptorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction cara = ctx.getClickedFace();
        Direction f = cara.getAxis().isHorizontal() ? cara : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, f);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return formas[st.getValue(FACING).get2DDataValue()];
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            Boolean ahora = Luminaria.estado(level, pos, 8);
            boolean encender = ahora == null || !ahora;
            int n = Luminaria.controlar(level, pos, 8, encender, null);
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, encender), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, encender ? 1.5f : 1.2f);
            if (n == 0) p.displayClientMessage(Component.literal("No hay luces cerca (8 bloques)").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
