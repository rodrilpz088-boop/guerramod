package es.guerramod.desastres;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import es.guerramod.GuerraMod;
import es.guerramod.registro.ModBloques;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Agua de riada: agua marrón y turbia cargada de barro, como la de una inundación real. Bajo ella apenas se ve a un par
 * de metros. Se comporta como el agua (se nada, apaga el fuego, ahoga...) y corre igual.
 */
public final class AguaTurbia {
    private AguaTurbia() {}

    public static final DeferredRegister<FluidType> TIPOS = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, GuerraMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDOS = DeferredRegister.create(ForgeRegistries.FLUIDS, GuerraMod.MODID);

    /** Color del agua con barro (ARGB). */
    public static final int COLOR = 0xFF8A6A42;

    public static final RegistryObject<FluidType> TIPO = TIPOS.register("agua_turbia", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.guerramod.agua_turbia").canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true)
            .supportsBoating(true).canConvertToSource(false).fallDistanceModifier(0f).motionScale(0.016).density(1150).viscosity(1400)
            .temperature(288).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> c) {
            c.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return new ResourceLocation("block/water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return new ResourceLocation("block/water_flow");
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return new ResourceLocation("block/water_overlay");
                }

                @Override
                public int getTintColor() {
                    return COLOR;
                }

                @Override
                public Vector3f modifyFogColor(Camera cam, float parcial, ClientLevel level, int distancia, float oscuro, Vector3f color) {
                    return new Vector3f(0.27f, 0.20f, 0.12f);
                }

                @Override
                public void modifyFogRender(Camera cam, FogRenderer.FogMode modo, float distancia, float parcial, float cerca, float lejos, FogShape forma) {
                    RenderSystem.setShaderFogStart(0.0f);
                    RenderSystem.setShaderFogEnd(3.5f);
                }
            });
        }
    });

    public static final RegistryObject<FlowingFluid> FUENTE = FLUIDOS.register("agua_turbia", () -> new ForgeFlowingFluid.Source(propiedades()));
    public static final RegistryObject<FlowingFluid> CORRIENTE = FLUIDOS.register("agua_turbia_corriente", () -> new ForgeFlowingFluid.Flowing(propiedades()));
    public static final RegistryObject<LiquidBlock> BLOQUE = ModBloques.BLOQUES.register("agua_turbia",
            () -> new LiquidBlock(FUENTE, BlockBehaviour.Properties.copy(Blocks.WATER).mapColor(MapColor.DIRT)));

    private static ForgeFlowingFluid.Properties propiedades() {
        return new ForgeFlowingFluid.Properties(TIPO, FUENTE, CORRIENTE).block(BLOQUE).slopeFindDistance(4).levelDecreasePerBlock(1)
                .tickRate(5).explosionResistance(100f);
    }
}
