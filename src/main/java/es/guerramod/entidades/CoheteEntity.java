package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Cohete del lanzacohetes. */
public class CoheteEntity extends ExplosivoEntity {
    public CoheteEntity(EntityType<? extends CoheteEntity> tipo, Level level) {
        super(tipo, level);
    }

    public CoheteEntity(Level level, LivingEntity tirador) {
        super(ModEntidades.COHETE.get(), tirador, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.COHETE.get();
    }

    @Override
    protected float getGravity() {
        return 0.003f;
    }

    @Override
    protected float potencia() {
        return 3.5f;
    }

    @Override
    protected boolean estela() {
        return true;
    }

    @Override
    protected int vidaMaxima() {
        return 200;
    }
}
