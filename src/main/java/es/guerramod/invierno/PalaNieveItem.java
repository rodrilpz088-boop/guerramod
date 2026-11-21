package es.guerramod.invierno;

import es.guerramod.clima.Nieve;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;

/** Pala quitanieves: clic derecho en la nieve y despeja un trozo de 3×3 (te da bolas de nieve). */
public class PalaNieveItem extends Item {
    public PalaNieveItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Clic derecho en la nieve: despeja 3×3").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        BlockPos p = c.getClickedPos();
        BlockState s = l.getBlockState(p);
        if (!s.is(Blocks.SNOW) && !s.is(Blocks.SNOW_BLOCK) && !s.is(Blocks.POWDER_SNOW)) return InteractionResult.PASS;
        if (l instanceof ServerLevel sl) {
            int capas = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int x = p.getX() + dx, z = p.getZ() + dz;
                    BlockPos a = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                    if (Math.abs(a.getY() - p.getY()) > 2) continue;
                    // quita hasta un bloque de nieve por columna en cada palada
                    for (int k = 0; k < 8; k++) {
                        BlockState t = sl.getBlockState(a);
                        if (t.is(Blocks.SNOW)) {
                            capas += t.getValue(SnowLayerBlock.LAYERS);
                            sl.removeBlock(a, false);
                            break;
                        } else if (sl.getBlockState(a.below()).is(Blocks.SNOW_BLOCK) || sl.getBlockState(a.below()).is(Blocks.POWDER_SNOW)) {
                            sl.removeBlock(a.below(), false);
                            capas += 8;
                            break;
                        } else break;
                    }
                    if (sl.getBlockState(a.below()).is(Blocks.GRASS_BLOCK)) {
                        sl.setBlockAndUpdate(a.below(), sl.getBlockState(a.below()).setValue(net.minecraft.world.level.block.SnowyDirtBlock.SNOWY, false));
                    }
                }
            if (capas > 0) {
                sl.playSound(null, p, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                sl.playSound(null, p, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 0.8f, 1f);
                Player pl = c.getPlayer();
                if (pl != null) {
                    ItemStack bolas = new ItemStack(Items.SNOWBALL, Math.max(1, capas / 4));
                    if (!pl.addItem(bolas)) pl.drop(bolas, false);
                    c.getItemInHand().hurtAndBreak(1, pl, e -> e.broadcastBreakEvent(c.getHand()));
                }
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
