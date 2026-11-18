package es.guerramod.items;

import es.guerramod.radiacion.DatosRadiacion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Contador Geiger: mide la radiación. Llevándolo en la mano hace clics en zonas radiactivas. */
public class GeigerItem extends Item {
    public GeigerItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        if (level instanceof ServerLevel sl) {
            float nivel = DatosRadiacion.get(sl.getServer()).intensidad(sl, p.position());
            ChatFormatting c = nivel <= 0 ? ChatFormatting.GREEN : nivel < 0.4f ? ChatFormatting.YELLOW : ChatFormatting.RED;
            String texto = nivel <= 0 ? "☢ Radiación: normal" : "☢ Radiación: " + Math.round(nivel * 100) + "%";
            p.displayClientMessage(Component.literal(texto).withStyle(c), true);
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(mano), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Mide la radiación tras una explosión nuclear").withStyle(ChatFormatting.GRAY));
    }
}
