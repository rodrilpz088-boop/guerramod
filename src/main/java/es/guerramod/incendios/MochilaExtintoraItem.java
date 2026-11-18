package es.guerramod.incendios;

import es.guerramod.items.MaterialArmadura;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Mochila extintora forestal (a la espalda): 20 litros de agua para la manguera cuando no hay boca de incendio. */
public class MochilaExtintoraItem extends ArmorItem {
    public static final int CAPACIDAD = 1200;

    public MochilaExtintoraItem(Properties p) {
        super(MaterialArmadura.BOMBERO, Type.CHESTPLATE, p);
    }

    public static int agua(ItemStack st) {
        return st.getTag() == null ? 0 : st.getTag().getInt("agua");
    }

    public static boolean gastar(ItemStack st, int n) {
        int a = agua(st);
        if (a < n) return false;
        st.getOrCreateTag().putInt("agua", a - n);
        return true;
    }

    public static void llenar(ItemStack st) {
        st.getOrCreateTag().putInt("agua", CAPACIDAD);
    }

    @Override
    public boolean isBarVisible(ItemStack st) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack st) {
        return Math.round(13.0f * agua(st) / CAPACIDAD);
    }

    @Override
    public int getBarColor(ItemStack st) {
        return 0x3A8CFF;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Agua: " + agua(st) * 20 / CAPACIDAD + " / 20 litros").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("Ponla en el pecho; la manguera la usa si no está conectada").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Rellénala: métete en agua con la manguera y Shift + clic dcho").withStyle(ChatFormatting.DARK_GRAY));
    }
}
