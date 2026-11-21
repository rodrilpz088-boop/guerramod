package es.guerramod.navidad;

import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.vivienda.PlacaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/**
 * Luces de Navidad al estilo de Madrid: mallas de luces para el árbol cónico de la plaza, estrella, lluvia de luces,
 * cortinas, guirnaldas, adornos de farola, arcos de calle y bolas gigantes. Todas parpadean (textura animada).
 */
public final class BloquesNavidad {
    private BloquesNavidad() {}

    private static final Categoria C = Categoria.NAVIDAD;

    private static BlockBehaviour.Properties luces(int luz) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.4f).sound(SoundType.GLASS).noOcclusion()
                .lightLevel(s -> luz).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).emissiveRendering((s, l, p) -> true);
    }

    /** Bloque sin colisión con forma propia (lluvia de luces, estrella). */
    public static class Adorno extends Block {
        private final VoxelShape forma;

        public Adorno(Properties p, VoxelShape forma) {
            super(p);
            this.forma = forma;
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return forma;
        }
    }

    public static final RegistryObject<Block> MALLA_LUCES = ModBloques.reg("malla_luces", C, () -> new Block(luces(15)));
    public static final RegistryObject<Block> MALLA_LUCES_AZUL = ModBloques.reg("malla_luces_azul", C, () -> new Block(luces(14)));
    public static final RegistryObject<Block> MALLA_LUCES_MULTICOLOR = ModBloques.reg("malla_luces_multicolor", C, () -> new Block(luces(15)));
    public static final RegistryObject<Block> ESTRELLA_NAVIDAD = ModBloques.reg("estrella_navidad", C,
            () -> new Adorno(luces(15), Block.box(2, 0, 2, 14, 16, 14)));
    public static final RegistryObject<Block> LLUVIA_LUCES = ModBloques.reg("lluvia_luces", C,
            () -> new Adorno(luces(12).noCollission(), Block.box(1, 0, 1, 15, 16, 15)));
    public static final RegistryObject<Block> CORTINA_LUCES = ModBloques.reg("cortina_luces", C,
            () -> new PlacaBlock(luces(12).noCollission(), 1, 0, 16, false));
    public static final RegistryObject<Block> GUIRNALDA_LUCES = ModBloques.reg("guirnalda_luces", C,
            () -> new PlacaBlock(luces(10).noCollission(), 1, 6, 12, false));
    public static final RegistryObject<Block> ADORNO_FAROLA = ModBloques.reg("adorno_farola", C,
            () -> new PlacaBlock(luces(12).noCollission(), 1, 0, 16, false));
    public static final RegistryObject<Block> ARCO_LUCES = ModBloques.reg("arco_luces", C,
            () -> new PlacaBlock(luces(15).noCollission(), 1, 0, 16, false));
    public static final RegistryObject<Block> CARTEL_FELIZ_NAVIDAD = ModBloques.reg("cartel_feliz_navidad", C,
            () -> new PlacaBlock(luces(15).noCollission(), 1, 2, 14, false));
    public static final RegistryObject<Block> BOLA_NAVIDAD_ROJA = ModBloques.reg("bola_navidad_roja", C,
            () -> new Adorno(luces(7), Block.box(1, 0, 1, 15, 16, 15)));
    public static final RegistryObject<Block> BOLA_NAVIDAD_DORADA = ModBloques.reg("bola_navidad_dorada", C,
            () -> new Adorno(luces(7), Block.box(1, 0, 1, 15, 16, 15)));

    public static void iniciar() {
    }
}
