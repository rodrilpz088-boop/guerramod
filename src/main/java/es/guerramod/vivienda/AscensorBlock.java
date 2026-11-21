package es.guerramod.vivienda;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Suelo de la cabina del ascensor. */
public class AscensorBlock extends Block {
    public AscensorBlock(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable BlockGetter l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Uno por planta, en la misma columna").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Salta para subir, agáchate para bajar").withStyle(ChatFormatting.GRAY));
    }
}
