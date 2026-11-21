package es.guerramod.ciudad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plano de un edificio en coordenadas locales: x = a lo ancho (derecha positiva), y = altura (0 = suelo),
 * z = fondo (0 = fachada principal, mirando hacia quien lo construye). Los bloques con orientación se escriben como si
 * el jugador mirase al norte: la fachada mira al SUR y el fondo del edificio queda al NORTE.
 */
public final class Plano {
    final Map<BlockPos, BlockState> bloques = new LinkedHashMap<>();
    int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE,
            minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

    public static final BlockState AIRE = Blocks.AIR.defaultBlockState();

    /** Bloque del mod por nombre (o el de repuesto si no existe). */
    public static BlockState mod(String id, Block repuesto) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("guerramod", id));
        return b == null || b == Blocks.AIR ? repuesto.defaultBlockState() : b.defaultBlockState();
    }

    public void set(int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        bloques.remove(p); // así el orden de colocación sigue el último cambio
        bloques.put(p, s);
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
    }

    public BlockState get(int x, int y, int z) {
        return bloques.get(new BlockPos(x, y, z));
    }

    /** Caja maciza (incluye los extremos). */
    public void caja(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }

    /** Solo las cuatro paredes de la caja. */
    public void paredes(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        int ax = Math.min(x1, x2), bx = Math.max(x1, x2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = ax; x <= bx; x++) {
                set(x, y, az, s);
                set(x, y, bz, s);
            }
            for (int z = az; z <= bz; z++) {
                set(ax, y, z, s);
                set(bx, y, z, s);
            }
        }
    }

    /** Cilindro (hueco o macizo) de eje vertical. */
    public void cilindro(double cx, double cz, double r, int y1, int y2, BlockState s, boolean hueco) {
        int R = (int) Math.ceil(r) + 1;
        for (int y = y1; y <= y2; y++)
            for (int x = (int) Math.floor(cx) - R; x <= (int) Math.ceil(cx) + R; x++)
                for (int z = (int) Math.floor(cz) - R; z <= (int) Math.ceil(cz) + R; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (hueco ? Math.abs(d - r) < 0.55 : d <= r + 0.3) set(x, y, z, s);
                }
    }

    /** Puerta de dos bloques en la fachada (mirando al jugador). */
    public void puerta(int x, int y, int z, BlockState puerta, boolean bisagraDerecha) {
        if (!puerta.hasProperty(DoorBlock.HALF)) puerta = Blocks.OAK_DOOR.defaultBlockState();
        BlockState p = puerta.setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, bisagraDerecha ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        set(x, y, z, p.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, p.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Escalón de tejado: sube hacia el fondo (NORTE) o hacia la fachada (SUR). */
    public static BlockState escalon(Block escalera, Direction haciaDondeSube) {
        return escalera.defaultBlockState().setValue(StairBlock.FACING, haciaDondeSube);
    }

    /** Mueble orientado mirando a la dirección dada (si tiene orientación). */
    public static BlockState mirando(BlockState s, Direction d) {
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
        return s;
    }

    /** Rotación que lleva "norte" a la dirección en la que mira el jugador. */
    public static Rotation rotacion(Direction mirando) {
        return switch (mirando) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Coordenada local -> mundo. */
    public static BlockPos aMundo(BlockPos origen, Rotation rot, int x, int y, int z) {
        return origen.offset(new BlockPos(x, y, -z).rotate(rot));
    }

    /** Todos los bloques del plano (coordenadas locales). */
    public Map<BlockPos, BlockState> bloques() {
        return bloques;
    }

    public int ancho() {
        return maxX - minX + 1;
    }

    public int fondo() {
        return maxZ - minZ + 1;
    }

    public int alto() {
        return maxY - minY + 1;
    }
}
