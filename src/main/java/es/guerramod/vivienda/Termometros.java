package es.guerramod.vivienda;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Termómetro exterior de pared y termómetro digital de mano. */
public final class Termometros {
    private Termometros() {}

    static ChatFormatting color(double t) {
        return t < -10 ? ChatFormatting.DARK_AQUA : t < 5 ? ChatFormatting.AQUA : t < 18 ? ChatFormatting.WHITE : t < 27 ? ChatFormatting.GREEN
                : t < 35 ? ChatFormatting.GOLD : ChatFormatting.RED;
    }

    /** Muestra la lectura (en el chat, para que se pueda leer con calma). */
    public static void informar(ServerLevel l, Player pl, BlockPos donde, boolean termostato) {
        Interior.Lectura r = Interior.leer(l, donde);
        pl.sendSystemMessage(Component.literal(termostato ? "🌡 Termostato" : "🌡 Termómetro").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (r.dentro()) {
            pl.sendSystemMessage(Component.literal(String.format("  Dentro: %.1f °C", r.interior())).withStyle(color(r.interior()))
                    .append(Component.literal(r.consigna() >= 0 ? "  (termostato a " + r.consigna() + " °C)" : "").withStyle(ChatFormatting.GRAY)));
            pl.sendSystemMessage(Component.literal(String.format("  Fuera: %.1f °C · sensación %.0f °C · %.0f %% · viento %.0f km/h", r.exterior(),
                    r.sensacion(), r.humedad(), r.viento())).withStyle(color(r.exterior())));
            String eq = r.radiadores() + (r.radiadores() == 1 ? " radiador" : " radiadores") + (r.caldera() ? " con caldera" : "")
                    + " · " + r.chimeneas() + (r.chimeneas() == 1 ? " chimenea encendida" : " chimeneas encendidas")
                    + " · " + r.aires() + " aire" + (r.aires() == 1 ? "" : "s") + " acondicionado" + (r.aires() == 1 ? "" : "s") + " encendido" + (r.aires() == 1 ? "" : "s");
            pl.sendSystemMessage(Component.literal("  " + r.estado() + " — " + eq).withStyle(ChatFormatting.GRAY));
        } else {
            pl.sendSystemMessage(Component.literal(String.format("  Exterior: %.1f °C", r.exterior())).withStyle(color(r.exterior())));
            pl.sendSystemMessage(Component.literal(String.format("  Sensación térmica %.0f °C · humedad %.0f %% · viento %.0f km/h", r.sensacion(),
                    r.humedad(), r.viento())).withStyle(color(r.sensacion())));
        }
        l.playSound(null, donde, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.8f);
    }

    /** Termómetro de exterior (de esfera) para colgar en la fachada. */
    public static class Exterior extends PlacaBlock {
        public Exterior(Properties p) {
            super(p, 1, 2, 14, false);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l instanceof ServerLevel sl) informar(sl, pl, p.relative(s.getValue(FACING)), false);
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
    }

    /** Termómetro digital de mano: mide donde estás. */
    public static class Digital extends Item {
        public Digital(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
            if (l instanceof ServerLevel sl) informar(sl, p, BlockPos.containing(p.getEyePosition()), false);
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(h), l.isClientSide);
        }
    }
}
