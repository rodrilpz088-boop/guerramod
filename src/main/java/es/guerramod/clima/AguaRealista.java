package es.guerramod.clima;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import es.guerramod.GuerraMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Color realista del agua de cada zona (en vez del azul eléctrico de Minecraft): el mar profundo es azul petróleo, el
 * tropical turquesa, los ríos verdosos, los pantanos de color té y el agua helada azul acero. También cambia el color
 * del agua vista desde dentro (más oscura y verdosa, como en la realidad).
 */
public record AguaRealista(HolderSet<Biome> biomas) implements BiomeModifier {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZADORES =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, GuerraMod.MODID);

    public static final RegistryObject<Codec<AguaRealista>> CODEC = SERIALIZADORES.register("agua_realista",
            () -> RecordCodecBuilder.create(b -> b.group(Biome.LIST_CODEC.fieldOf("biomes").forGetter(AguaRealista::biomas))
                    .apply(b, AguaRealista::new)));

    @Override
    public void modify(Holder<Biome> biome, Phase fase, ModifiableBiomeInfo.BiomeInfo.Builder b) {
        if (fase != Phase.AFTER_EVERYTHING || !biomas.contains(biome)) return;
        int[] c = colores(biome.unwrapKey().map(k -> k.location().getPath()).orElse(""));
        b.getSpecialEffects().waterColor(c[0]).waterFogColor(c[1]);
    }

    /** {color del agua, color bajo el agua}. */
    static int[] colores(String k) {
        return switch (k) {
            case "ocean", "deep_ocean" -> new int[]{0x2F6E86, 0x0C2C3A};
            case "lukewarm_ocean", "deep_lukewarm_ocean" -> new int[]{0x2E8B92, 0x0F3A40};
            case "warm_ocean" -> new int[]{0x34B3B0, 0x15514F};
            case "cold_ocean", "deep_cold_ocean" -> new int[]{0x2C5670, 0x0A2231};
            case "frozen_ocean", "deep_frozen_ocean", "frozen_river", "snowy_beach" -> new int[]{0x3E6478, 0x13293A};
            case "beach" -> new int[]{0x3A9196, 0x154145};
            case "river" -> new int[]{0x3F7464, 0x18362C};
            case "swamp" -> new int[]{0x5B5E36, 0x232714};
            case "mangrove_swamp" -> new int[]{0x4F6040, 0x1E2A18};
            case "jungle", "sparse_jungle", "bamboo_jungle" -> new int[]{0x3E7462, 0x173528};
            case "desert", "badlands", "eroded_badlands", "wooded_badlands", "savanna", "savanna_plateau", "windswept_savanna" ->
                    new int[]{0x4A8A8A, 0x1B3B3B};
            case "mushroom_fields" -> new int[]{0x4A6E78, 0x1D3138};
            case "lush_caves" -> new int[]{0x3B8C7E, 0x163A33};
            case "dripstone_caves", "deep_dark" -> new int[]{0x2E5560, 0x0B1D22};
            default -> new int[]{0x3A7080, 0x122F38}; // lagos y charcas
        };
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
