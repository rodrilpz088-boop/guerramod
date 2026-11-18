package es.guerramod.items;

import es.guerramod.entidades.GranadaEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GranadaItem extends Item {
    public GranadaItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.5f);
        if (!level.isClientSide) {
            GranadaEntity g = new GranadaEntity(level, p);
            g.setItem(st.copyWithCount(1));
            g.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, 1.3f, 1.0f);
            level.addFreshEntity(g);
        }
        p.getCooldowns().addCooldown(this, 20);
        if (!p.getAbilities().instabuild) st.shrink(1);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }
}
