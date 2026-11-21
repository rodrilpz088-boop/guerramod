package es.guerramod.invierno;

import es.guerramod.registro.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Trineo: una barca que desliza por la nieve y el hielo (casi sin rozamiento) y cuesta abajo coge velocidad. */
public class TrineoEntity extends Boat {
    public TrineoEntity(EntityType<? extends Boat> t, Level l) {
        super(t, l);
    }

    public TrineoEntity(Level l, double x, double y, double z) {
        this(ModEntidades.TRINEO.get(), l);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public Item getDropItem() {
        return ItemsInvierno.TRINEO.get();
    }

    private boolean enNieve() {
        BlockPos p = BlockPos.containing(getX(), getY() - 0.05, getZ());
        BlockState s = level().getBlockState(p), d = level().getBlockState(p.below());
        return s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW) || d.is(Blocks.SNOW_BLOCK) || d.is(Blocks.SNOW)
                || s.is(net.minecraft.tags.BlockTags.ICE) || s.is(ItemsInvierno.NIEVE_PISADA.get());
    }

    @Override
    public float getGroundFriction() {
        return enNieve() ? 0.985f : 0.45f;
    }

    @Override
    public void tick() {
        super.tick();
        // cuesta abajo: si delante el suelo está más bajo, el trineo acelera
        if (enNieve() && onGround()) {
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 0.0004) {
                Vec3 dir = new Vec3(v.x, 0, v.z).normalize();
                BlockPos delante = BlockPos.containing(getX() + dir.x * 1.5, getY() - 0.5, getZ() + dir.z * 1.5);
                if (level().getBlockState(delante).isAir() || level().getBlockState(delante).canBeReplaced()) {
                    setDeltaMovement(v.add(dir.scale(0.035)));
                }
            }
        }
    }
}
