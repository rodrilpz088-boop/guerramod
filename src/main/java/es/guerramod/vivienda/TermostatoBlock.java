package es.guerramod.vivienda;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Termostato de pared: clic derecho para ver la temperatura de la casa, la de fuera y qué hacen la calefacción y el
 * aire. Agachado + clic derecho cambia la temperatura deseada (16 a 28 °C): la calefacción y el aire se paran al llegar.
 */
public class TermostatoBlock extends PlacaBlock {
    public static final IntegerProperty CONSIGNA = IntegerProperty.create("consigna", 16, 28);

    public TermostatoBlock(Properties p) {
        super(p, 1, 4, 12, false);
        registerDefaultState(defaultBlockState().setValue(CONSIGNA, 21));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(CONSIGNA);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        if (l instanceof ServerLevel sl) {
            if (pl.isShiftKeyDown()) {
                int c = s.getValue(CONSIGNA) + 1;
                if (c > 28) c = 16;
                sl.setBlock(p, s.setValue(CONSIGNA, c), Block.UPDATE_ALL);
                sl.playSound(null, p, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.6f);
                pl.displayClientMessage(Component.literal("Termostato: " + c + " °C").withStyle(ChatFormatting.GOLD), true);
            } else {
                Termometros.informar(sl, pl, p.relative(s.getValue(FACING)), true);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
