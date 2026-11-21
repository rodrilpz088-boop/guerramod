package es.guerramod.metro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import javax.annotation.Nullable;
import java.util.List;

/** Vagón de metro: ponlo en una vía. Arranca solo hacia donde miras y para en las estaciones. */
public class VagonItem extends Item {
    public VagonItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Ponlo en una vía: va solo y para en las estaciones").withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.literal("Estación: bloque «señal de parada» debajo de la vía").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        BlockPos p = c.getClickedPos();
        BlockState s = l.getBlockState(p);
        if (!s.is(BlockTags.RAILS)) return InteractionResult.FAIL;
        if (!l.isClientSide) {
            RailShape forma = s.getBlock() instanceof BaseRailBlock r ? s.getValue(r.getShapeProperty()) : RailShape.NORTH_SOUTH;
            double dy = forma.isAscending() ? 0.5 : 0.0;
            MetroEntity m = new MetroEntity(l, p.getX() + 0.5, p.getY() + 0.0625 + dy, p.getZ() + 0.5);
            Direction d = c.getHorizontalDirection();
            m.arrancar(d.getStepX(), d.getStepZ());
            if (c.getItemInHand().hasCustomHoverName()) m.setCustomName(c.getItemInHand().getHoverName());
            l.addFreshEntity(m);
        }
        c.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
