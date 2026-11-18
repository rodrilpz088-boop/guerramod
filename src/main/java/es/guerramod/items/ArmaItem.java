package es.guerramod.items;

import es.guerramod.armas.DefArma;
import es.guerramod.armas.Disparos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Arma de fuego realista: cargador, recarga, modos de fuego.
 * Clic derecho (mantener) = apuntar. Clic izquierdo = disparar. R = recargar. V = modo de fuego.
 */
public class ArmaItem extends Item {
    public final DefArma def;

    public ArmaItem(DefArma def) {
        super(new Item.Properties().stacksTo(1));
        this.def = def;
    }

    // ------------------------------------------------------------------ estado guardado en el item

    public static int getBalas(ItemStack st) {
        return st.getTag() == null ? 0 : st.getTag().getInt("balas");
    }

    public static void setBalas(ItemStack st, int n) {
        st.getOrCreateTag().putInt("balas", n);
    }

    /** true = semiautomático (también para armas que solo tienen semiautomático). */
    public boolean esSemi(ItemStack st) {
        if (!def.automatica()) return true;
        return st.getTag() != null && st.getTag().getBoolean("semi");
    }

    public static boolean recargando(ItemStack st) {
        return st.getTag() != null && st.getTag().contains("recFin");
    }

    public static long recargaInicio(ItemStack st) {
        return st.getTag() == null ? 0 : st.getTag().getLong("recIni");
    }

    public static long recargaFin(ItemStack st) {
        return st.getTag() == null ? 0 : st.getTag().getLong("recFin");
    }

    public static void quitarRecarga(ItemStack st) {
        CompoundTag t = st.getTag();
        if (t != null) {
            t.remove("recFin");
            t.remove("recIni");
        }
    }

    // ------------------------------------------------------------------ apuntar (mantener clic derecho)

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        if (mano != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(p.getItemInHand(mano));
        p.startUsingItem(mano);
        return InteractionResultHolder.consume(p.getItemInHand(mano));
    }

    @Override
    public int getUseDuration(ItemStack st) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack st) {
        return UseAnim.NONE;
    }

    // ------------------------------------------------------------------ el clic izquierdo no golpea ni rompe

    @Override
    public boolean canAttackBlock(BlockState st, Level level, BlockPos pos, Player p) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack st, LivingEntity e) {
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack st, Player p, Entity e) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack viejo, ItemStack nuevo, boolean cambioRanura) {
        return cambioRanura || viejo.getItem() != nuevo.getItem();
    }

    // ------------------------------------------------------------------ recarga en curso

    @Override
    public void inventoryTick(ItemStack st, Level level, Entity e, int ranura, boolean seleccionada) {
        if (level.isClientSide || !recargando(st)) return;
        if (!seleccionada || !(e instanceof ServerPlayer p)) {
            quitarRecarga(st);
            return;
        }
        if (level.getGameTime() >= recargaFin(st)) Disparos.completarRecarga(p, st, this);
    }

    // ------------------------------------------------------------------ barra de munición

    @Override
    public boolean isBarVisible(ItemStack st) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack st) {
        return Math.round(13.0f * getBalas(st) / def.cargador());
    }

    @Override
    public int getBarColor(ItemStack st) {
        return getBalas(st) == 0 ? 0xD03030 : 0xE6C35A;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Cargador: " + getBalas(st) + " / " + def.cargador()).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Munición: ").append(def.municion().get().getDescription()).withStyle(ChatFormatting.GRAY));
        if (!def.explosiva()) {
            tip.add(Component.literal("Daño: " + fmt(def.dano()) + (def.proyectiles() > 1 ? " x" + def.proyectiles() : "")
                    + " · alcance eficaz " + (int) def.alcance() + " m").withStyle(ChatFormatting.GRAY));
        }
        if (def.automatica()) tip.add(Component.literal("Modo: " + (esSemi(st) ? "semiautomático" : "automático") + " (V)")
                .withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Clic dcho: apuntar · Clic izdo: disparar · R: recargar").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(float f) {
        return f == (int) f ? String.valueOf((int) f) : String.valueOf(f);
    }

    // ------------------------------------------------------------------ postura de los brazos en tercera persona

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity e, InteractionHand mano, ItemStack st) {
                if (e.isUsingItem() && e.getUseItem() == st) return HumanoidModel.ArmPose.BOW_AND_ARROW;
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }
}
