package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Bomba que sueltan los aviones. */
public class BombaAereaEntity extends ExplosivoEntity {
    public BombaAereaEntity(EntityType<? extends BombaAereaEntity> tipo, Level level) {
        super(tipo, level);
    }

    public BombaAereaEntity(Level level, Vec3 pos, Vec3 velocidad, @Nullable Entity dueno) {
        super(ModEntidades.BOMBA_AEREA.get(), level);
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(velocidad);
        setOwner(dueno);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOMBA_AEREA.get();
    }

    @Override
    protected float getGravity() {
        return 0.06f;
    }

    @Override
    protected float potencia() {
        return 5.0f;
    }
}
