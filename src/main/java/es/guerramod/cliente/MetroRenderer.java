package es.guerramod.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import es.guerramod.GuerraMod;
import es.guerramod.metro.MetroEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Dibuja el vagón de metro (un modelo de bloque grande) en lugar de la vagoneta. */
public class MetroRenderer extends MinecartRenderer<MetroEntity> {
    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(GuerraMod.MODID, "metro"), "main");
    private static final float ESCALA = 1.75f / 0.75f;

    public MetroRenderer(EntityRendererProvider.Context c) {
        super(c, CAPA);
        this.shadowRadius = 1.2f;
    }

    /** La vagoneta no se ve: solo el vagón. */
    public static LayerDefinition crearCapa() {
        return LayerDefinition.create(new MeshDefinition(), 64, 32);
    }

    @Override
    protected void renderMinecartContents(MetroEntity e, float pt, BlockState st, PoseStack ps, MultiBufferSource buf, int luz) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.scale(ESCALA, ESCALA, ESCALA);
        ps.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(st, ps, buf, luz, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
