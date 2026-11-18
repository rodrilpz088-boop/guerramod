package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Granada de mano. */
public class GranadaEntity extends ExplosivoEntity {
    public GranadaEntity(EntityType<? extends GranadaEntity> tipo, Level level) {
        super(tipo, level);
    }

    public GranadaEntity(Level level, LivingEntity tirador) {
        super(ModEntidades.GRANADA.get(), tirador, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRANADA.get();
    }

    @Override
    protected float getGravity() {
        return 0.05f;
    }

    @Override
    protected float potencia() {
        return 2.5f;
    }
}
