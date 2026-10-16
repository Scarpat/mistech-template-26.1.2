package com.mistech.mistechmod.realm.dream.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Vigia REM: lustre de olhos que gira devagar; no alarme para e os olhos encaram. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class RemWatcherModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "rem_watcher"), "main");
    private final ModelPart crown, thread0, thread1, thread2, thread3, thread4, thread5;

    public RemWatcherModel(ModelPart root) {
        super(root);
        this.crown = root.getChild("crown");
        this.thread0 = this.crown.getChild("thread_0");
        this.thread1 = this.crown.getChild("thread_1");
        this.thread2 = this.crown.getChild("thread_2");
        this.thread3 = this.crown.getChild("thread_3");
        this.thread4 = this.crown.getChild("thread_4");
        this.thread5 = this.crown.getChild("thread_5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_crown = root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(24, 30).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 1.0F).texOffs(0, 34).addBox(-6.0F, 0.0F, 5.0F, 12.0F, 1.0F, 1.0F).texOffs(26, 0).addBox(-6.0F, 0.0F, -5.0F, 1.0F, 1.0F, 10.0F).texOffs(0, 13).addBox(5.0F, 0.0F, -5.0F, 1.0F, 1.0F, 10.0F).texOffs(26, 34).addBox(-6.0F, 0.0F, -0.5F, 12.0F, 1.0F, 1.0F).texOffs(0, 0).addBox(-0.5F, 0.0F, -6.0F, 1.0F, 1.0F, 12.0F).texOffs(0, 30).addBox(-5.5F, -3.0F, -5.5F, 1.0F, 3.0F, 1.0F).texOffs(4, 30).addBox(4.5F, -3.0F, -5.5F, 1.0F, 3.0F, 1.0F).texOffs(8, 30).addBox(-5.5F, -3.0F, 4.5F, 1.0F, 3.0F, 1.0F).texOffs(12, 30).addBox(4.5F, -3.0F, 4.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p_chain = p_crown.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(40, 24).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(44, 24).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(48, 24).addBox(-0.5F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(52, 24).addBox(-0.5F, -16.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_thread_0 = p_crown.addOrReplaceChild("thread_0", CubeListBuilder.create().texOffs(16, 30).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(-5.5F, 1.0F, -5.5F));
        PartDefinition p_eye_0 = p_thread_0.addOrReplaceChild("eye_0", CubeListBuilder.create().texOffs(26, 13).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition p_thread_1 = p_crown.addOrReplaceChild("thread_1", CubeListBuilder.create().texOffs(36, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offset(5.5F, 1.0F, -5.5F));
        PartDefinition p_eye_1 = p_thread_1.addOrReplaceChild("eye_1", CubeListBuilder.create().texOffs(38, 13).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition p_thread_2 = p_crown.addOrReplaceChild("thread_2", CubeListBuilder.create().texOffs(56, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(-5.5F, 1.0F, 5.5F));
        PartDefinition p_eye_2 = p_thread_2.addOrReplaceChild("eye_2", CubeListBuilder.create().texOffs(50, 13).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition p_thread_3 = p_crown.addOrReplaceChild("thread_3", CubeListBuilder.create().texOffs(20, 30).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(5.5F, 1.0F, 5.5F));
        PartDefinition p_eye_3 = p_thread_3.addOrReplaceChild("eye_3", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 2.0F, 0.0F));
        PartDefinition p_thread_4 = p_crown.addOrReplaceChild("thread_4", CubeListBuilder.create().texOffs(22, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offset(0.5F, 1.0F, -5.5F));
        PartDefinition p_eye_4 = p_thread_4.addOrReplaceChild("eye_4", CubeListBuilder.create().texOffs(12, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p_thread_5 = p_crown.addOrReplaceChild("thread_5", CubeListBuilder.create().texOffs(60, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.5F, 1.0F, 5.5F));
        PartDefinition p_eye_5 = p_thread_5.addOrReplaceChild("eye_5", CubeListBuilder.create().texOffs(24, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        // gira devagar; no alarme para e todos os olhos se inclinam para a frente
        this.crown.yRot = state.alert ? 0.0F : age * 0.02F;
        this.crown.y += Mth.sin(age * 0.05F) * 0.6F;
        ModelPart[] threads = {this.thread0, this.thread1, this.thread2, this.thread3, this.thread4, this.thread5};
        for (int i = 0; i < threads.length; i++) {
            threads[i].xRot = state.alert ? -0.55F : Mth.sin(age * 0.05F + i * 1.3F) * 0.12F;
            threads[i].zRot = state.alert ? 0.0F : Mth.cos(age * 0.04F + i * 0.9F) * 0.1F;
        }
    }
}
