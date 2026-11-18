package es.guerramod.items;

import es.guerramod.bloques.hogar.Luminaria;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Mando de las luces LED: cambia el color de todas las luces cercanas o las enciende/apaga. */
public class MandoLedItem extends Item {
    private static final int RADIO = 12;

    public MandoLedItem(Properties p) {
        super(p);
    }

    private void pulsar(Level level, Player p, ItemStack st) {
        if (level.isClientSide) return;
        if (p.isShiftKeyDown()) {
            Boolean ahora = Luminaria.estado(level, p.blockPosition(), RADIO);
            if (ahora == null) {
                p.displayClientMessage(Component.literal("No hay luces LED cerca").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            Luminaria.controlar(level, p.blockPosition(), RADIO, !ahora, null);
            p.displayClientMessage(Component.literal(ahora ? "Luces apagadas" : "Luces encendidas").withStyle(ChatFormatting.YELLOW), true);
        } else {
            int color = (st.getOrCreateTag().getInt("color") + 1) % Luminaria.RGB.length;
            st.getOrCreateTag().putInt("color", color);
            int n = Luminaria.controlar(level, p.blockPosition(), RADIO, true, color);
            p.displayClientMessage(Component.literal("Color LED: ").append(Component.literal(Luminaria.NOMBRES_COLOR[color])
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Luminaria.RGB[color])))).append(" (" + n + " luces)"), true);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.4f, 2.0f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        pulsar(level, p, st);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() != null) pulsar(ctx.getLevel(), ctx.getPlayer(), ctx.getItemInHand());
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clic derecho: siguiente color").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Shift + clic derecho: encender/apagar").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Controla las luces a " + RADIO + " bloques").withStyle(ChatFormatting.DARK_GRAY));
    }
}
