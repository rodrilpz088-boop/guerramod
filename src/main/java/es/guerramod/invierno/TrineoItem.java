package es.guerramod.invierno;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Coloca el trineo donde miras (como una barca). */
public class TrineoItem extends Item {
    public TrineoItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        BlockHitResult hit = getPlayerPOVHitResult(l, p, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(st);
        if (!l.isClientSide) {
            var v = hit.getLocation();
            TrineoEntity t = new TrineoEntity(l, v.x, v.y, v.z);
            t.setYRot(p.getYRot());
            if (!l.noCollision(t, t.getBoundingBox())) return InteractionResultHolder.fail(st);
            l.addFreshEntity(t);
            if (!p.getAbilities().instabuild) st.shrink(1);
        }
        p.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }
}
