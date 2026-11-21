package es.guerramod.cliente;

import com.mojang.datafixers.util.Pair;
import es.guerramod.GuerraMod;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/** El trineo usa la forma de la barca con su propia madera pintada de rojo. */
public class TrineoRenderer extends BoatRenderer {
    private static final ResourceLocation TEXTURA = new ResourceLocation(GuerraMod.MODID, "textures/entity/trineo.png");
    private final Pair<ResourceLocation, ListModel<Boat>> recurso;

    public TrineoRenderer(EntityRendererProvider.Context c) {
        super(c, false);
        recurso = Pair.of(TEXTURA, new BoatModel(c.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK))));
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat b) {
        return recurso;
    }
}
