package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.entidades.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntidades {
    public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GuerraMod.MODID);

    public static final RegistryObject<EntityType<BalaEntity>> BALA = ENTIDADES.register("bala",
            () -> EntityType.Builder.<BalaEntity>of(BalaEntity::new, MobCategory.MISC).sized(0.15f, 0.15f)
                    .clientTrackingRange(6).updateInterval(1).build("bala"));
    public static final RegistryObject<EntityType<CoheteEntity>> COHETE = ENTIDADES.register("cohete",
            () -> EntityType.Builder.<CoheteEntity>of(CoheteEntity::new, MobCategory.MISC).sized(0.3f, 0.3f)
                    .clientTrackingRange(8).updateInterval(1).build("cohete"));
    public static final RegistryObject<EntityType<GranadaEntity>> GRANADA = ENTIDADES.register("granada",
            () -> EntityType.Builder.<GranadaEntity>of(GranadaEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(6).updateInterval(2).build("granada"));
    public static final RegistryObject<EntityType<BombaAereaEntity>> BOMBA_AEREA = ENTIDADES.register("bomba_aerea",
            () -> EntityType.Builder.<BombaAereaEntity>of(BombaAereaEntity::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(10).updateInterval(1).build("bomba_aerea"));
    public static final RegistryObject<EntityType<MisilEntity>> MISIL = ENTIDADES.register("misil",
            () -> EntityType.Builder.<MisilEntity>of(MisilEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(16).updateInterval(1).build("misil"));
    public static final RegistryObject<EntityType<AvionEntity>> AVION = ENTIDADES.register("avion",
            () -> EntityType.Builder.<AvionEntity>of(AvionEntity::new, MobCategory.MISC).sized(2.6f, 1.2f)
                    .clientTrackingRange(16).updateInterval(1).build("avion"));
    public static final RegistryObject<EntityType<BombarderoEntity>> BOMBARDERO = ENTIDADES.register("bombardero",
            () -> EntityType.Builder.<BombarderoEntity>of(BombarderoEntity::new, MobCategory.MISC).sized(3.0f, 1.3f)
                    .clientTrackingRange(16).updateInterval(1).build("bombardero"));

    public static final RegistryObject<EntityType<AsientoEntity>> ASIENTO = ENTIDADES.register("asiento",
            () -> EntityType.Builder.<AsientoEntity>of(AsientoEntity::new, MobCategory.MISC).sized(0.001f, 0.001f)
                    .clientTrackingRange(10).updateInterval(20).noSummon().build("asiento"));

    public static final RegistryObject<EntityType<es.guerramod.incendios.HidroavionEntity>> HIDROAVION = ENTIDADES.register("hidroavion",
            () -> EntityType.Builder.<es.guerramod.incendios.HidroavionEntity>of(es.guerramod.incendios.HidroavionEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.3f).clientTrackingRange(16).updateInterval(1).build("hidroavion"));

    private ModEntidades() {}

    public static final RegistryObject<EntityType<es.guerramod.desastres.TornadoEntity>> TORNADO = ENTIDADES.register("tornado",
            () -> EntityType.Builder.<es.guerramod.desastres.TornadoEntity>of(es.guerramod.desastres.TornadoEntity::new, MobCategory.MISC)
                    .sized(4.0f, 30.0f).clientTrackingRange(32).updateInterval(2).fireImmune().build("tornado"));
}
