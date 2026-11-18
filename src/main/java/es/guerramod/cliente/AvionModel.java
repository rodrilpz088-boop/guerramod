package es.guerramod.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import es.guerramod.GuerraMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Modelo 3D del avión (fuselaje, cabina, alas, cola y hélice que gira). */
public class AvionModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(GuerraMod.MODID, "avion"), "main");

    private final ModelPart raiz;
    private final ModelPart helice;

    public AvionModel(ModelPart raiz) {
        this.raiz = raiz;
        this.helice = raiz.getChild("helice");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition r = malla.getRoot();
        r.addOrReplaceChild("fuselaje", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -14, 8, 8, 28), PartPose.ZERO);
        r.addOrReplaceChild("cabina", CubeListBuilder.create().texOffs(74, 0).addBox(-3, -8, -6, 6, 4, 8), PartPose.ZERO);
        r.addOrReplaceChild("alas", CubeListBuilder.create().texOffs(0, 36).addBox(-18, -1, -4, 36, 1, 8), PartPose.ZERO);
        r.addOrReplaceChild("cola", CubeListBuilder.create().texOffs(0, 46).addBox(-7, -2, 10, 14, 1, 5), PartPose.ZERO);
        r.addOrReplaceChild("deriva", CubeListBuilder.create().texOffs(40, 46).addBox(-0.5f, -10, 9, 1, 7, 6), PartPose.ZERO);
        r.addOrReplaceChild("buje", CubeListBuilder.create().texOffs(110, 0).addBox(-1.5f, -1.5f, -16, 3, 3, 2), PartPose.ZERO);
        r.addOrReplaceChild("ruedas", CubeListBuilder.create().texOffs(90, 20)
                .addBox(-6, 4, -6, 2, 3, 2).addBox(4, 4, -6, 2, 3, 2).addBox(-1, 4, 10, 2, 2, 2), PartPose.ZERO);
        r.addOrReplaceChild("helice", CubeListBuilder.create()
                        .texOffs(74, 14).addBox(-0.5f, -7, -0.5f, 1, 14, 1)
                        .texOffs(80, 14).addBox(-7, -0.5f, -0.5f, 14, 1, 1),
                PartPose.offset(0, 0, -16.5f));
        return LayerDefinition.create(malla, 128, 64);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        helice.zRot = ageInTicks * 1.3f;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int luz, int overlay, float r, float g, float b, float a) {
        raiz.render(ps, vc, luz, overlay, r, g, b, a);
    }
}
