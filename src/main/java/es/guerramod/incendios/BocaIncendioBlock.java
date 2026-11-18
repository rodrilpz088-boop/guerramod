package es.guerramod.incendios;

import es.guerramod.bloques.hogar.MuebleBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hidrante (boca de incendio de columna) o BIE de pared. Agua ilimitada: conecta la manguera con Shift + clic derecho.
 * La BIE además te da su manguera si no llevas ninguna.
 */
public class BocaIncendioBlock extends MuebleBlock {
    private final boolean bie;

    public BocaIncendioBlock(Properties p, double[][] forma, boolean bie) {
        super(p, forma, -1, 0, Accion.NINGUNA);
        this.bie = bie;
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        ItemStack enMano = p.getItemInHand(mano);
        if (enMano.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                p.setItemInHand(mano, ItemUtils.createFilledResult(enMano, p, new ItemStack(Items.WATER_BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (enMano.getItem() instanceof MangueraItem) {
            if (!level.isClientSide) MangueraItem.conectar(enMano, level, pos, p);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (enMano.isEmpty() && bie) {
            if (!level.isClientSide) {
                boolean tiene = p.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof MangueraItem);
                if (tiene) {
                    p.displayClientMessage(Component.literal("Ya llevas una manguera: pulsa la BIE con ella para conectarla")
                            .withStyle(ChatFormatting.GRAY), true);
                } else {
                    ItemStack m = new ItemStack(BloquesIncendio.MANGUERA.get());
                    MangueraItem.conectar(m, level, pos, p);
                    p.setItemInHand(mano, m);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 1.0f, 0.8f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            p.displayClientMessage(Component.literal("Conecta una manguera haciendo clic con ella aquí. Con un cubo lo llenas de agua.")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
