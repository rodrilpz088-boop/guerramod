package es.guerramod.invierno;

import es.guerramod.clima.Nieve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;

/** Sal gruesa para carreteras: clic derecho en el suelo y funde la nieve y el hielo de alrededor (radio 4). */
public class SalItem extends Item {
    public SalItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Clic derecho: funde la nieve y el hielo (radio 4)").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        if (l instanceof ServerLevel sl) {
            BlockPos p = c.getClickedPos();
            int r = 4, hechos = 0;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    BlockPos a = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p.offset(dx, 0, dz));
                    if (Math.abs(a.getY() - p.getY()) > 4) continue;
                    for (int k = 0; k < 24 && Nieve.quitarCapa(sl, sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, a), true); k++) hechos++;
                    BlockPos b = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, a).below();
                    if (sl.getBlockState(b).is(Blocks.ICE) || sl.getBlockState(b).is(Blocks.FROSTED_ICE)) {
                        sl.setBlockAndUpdate(b, Blocks.WATER.defaultBlockState());
                        hechos++;
                    }
                    if (sl.random.nextInt(3) == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_CONCRETE_POWDER.defaultBlockState()),
                                a.getX() + 0.5, a.getY() + 0.2, a.getZ() + 0.5, 3, 0.4, 0.1, 0.4, 0.05);
                    }
                }
            sl.playSound(null, p, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 1.4f);
            if (hechos > 0 && c.getPlayer() != null && !c.getPlayer().getAbilities().instabuild) c.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
