package es.guerramod.items;

import es.guerramod.entidades.AvionEntity;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Coloca un avión de combate pilotable. */
public class AvionItem extends Item {
    public AvionItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Player p = ctx.getPlayer();
        AvionEntity avion = new AvionEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (p != null) {
            avion.setYRot(p.getYRot());
            Pais pais = RegistroPaises.de(level).paisDeJugador(p.getUUID());
            if (pais != null) avion.setColor(pais.color);
        }
        if (!level.noCollision(avion, avion.getBoundingBox())) {
            if (p != null) p.displayClientMessage(Component.literal("No cabe aquí").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(avion);
        ItemStack st = ctx.getItemInHand();
        if (p == null || !p.getAbilities().instabuild) st.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clic derecho en el suelo para colocarlo, clic derecho para subir").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("W acelera · S frena · ratón para dirigir · Shift para bajar").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("R ametralladora · B bomba (necesita Bombas aéreas)").withStyle(ChatFormatting.DARK_GRAY));
    }
}
