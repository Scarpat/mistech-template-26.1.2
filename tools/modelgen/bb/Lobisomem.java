// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class Lobisomem<T extends livingEntinty> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "lobisomem"), "main");
	private final ModelPart Perna;
	private final ModelPart Perna2;
	private final ModelPart Braço;
	private final ModelPart Braço2;
	private final ModelPart Torso;
	private final ModelPart Cabeça;
	private final ModelPart Orelha;
	private final ModelPart Orelha2;
	private final ModelPart Rabo;

	public Lobisomem(ModelPart root) {
		this.Perna = root.getChild("Perna");
		this.Perna2 = root.getChild("Perna2");
		this.Braço = root.getChild("Braço");
		this.Braço2 = root.getChild("Braço2");
		this.Torso = root.getChild("Torso");
		this.Cabeça = root.getChild("Cabeça");
		this.Orelha = this.Cabeça.getChild("Orelha");
		this.Orelha2 = this.Cabeça.getChild("Orelha2");
		this.Rabo = root.getChild("Rabo");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Perna = partdefinition.addOrReplaceChild("Perna", CubeListBuilder.create().texOffs(18, 35).addBox(-7.0F, -1.0F, -3.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = Perna.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -6.3F, -0.2F, -0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r2 = Perna.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -5.0F, -0.1F, -0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r3 = Perna.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.3F, 1.0F, 0.6981F, 0.0F, 0.0F));

		PartDefinition Perna2 = partdefinition.addOrReplaceChild("Perna2", CubeListBuilder.create().texOffs(28, 36).addBox(-7.0F, -1.0F, -3.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, 24.0F, 0.0F));

		PartDefinition cube_r4 = Perna2.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 35).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -6.3F, -0.2F, -0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r5 = Perna2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(14, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -5.0F, -0.1F, -0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r6 = Perna2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.3F, 1.0F, 0.6981F, 0.0F, 0.0F));

		PartDefinition Braço = partdefinition.addOrReplaceChild("Braço", CubeListBuilder.create(), PartPose.offset(10.0F, 17.0F, -5.0F));

		PartDefinition cube_r7 = Braço.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(38, 36).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -7.3F, 1.8F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r8 = Braço.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(28, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -6.7F, 2.6F, -1.789F, 0.0F, 0.0F));

		PartDefinition cube_r9 = Braço.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(32, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.3F, 1.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r10 = Braço.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(30, 21).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 0.8F, -0.3F, 1.3963F, 0.0F, 0.0F));

		PartDefinition Braço2 = partdefinition.addOrReplaceChild("Braço2", CubeListBuilder.create(), PartPose.offset(2.0F, 17.0F, -5.0F));

		PartDefinition cube_r11 = Braço2.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(18, 39).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -7.3F, 1.8F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r12 = Braço2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -6.7F, 2.6F, -1.789F, 0.0F, 0.0F));

		PartDefinition cube_r13 = Braço2.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(32, 11).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.3F, 1.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r14 = Braço2.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(42, 25).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 0.8F, -0.3F, 1.3963F, 0.0F, 0.0F));

		PartDefinition Torso = partdefinition.addOrReplaceChild("Torso", CubeListBuilder.create(), PartPose.offset(-1.0F, 16.0F, 0.0F));

		PartDefinition cube_r15 = Torso.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -1.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.8F, -3.3F, 0.4276F, 0.0F, 0.0F));

		PartDefinition cube_r16 = Torso.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(16, 16).addBox(-1.0F, -4.0F, 0.0F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.8F, -2.3F, 0.5585F, 0.0F, 0.0F));

		PartDefinition cube_r17 = Torso.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(16, 8).addBox(-1.0F, -2.0F, -1.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6981F, 0.0F, 0.0F));

		PartDefinition Cabeça = partdefinition.addOrReplaceChild("Cabeça", CubeListBuilder.create(), PartPose.offset(-1.7F, 9.4F, -6.8F));

		PartDefinition cube_r18 = Cabeça.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(10, 35).addBox(0.0F, -8.0F, 1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 8).addBox(-1.0F, -5.0F, 1.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7F, -1.2F, 3.0F, 1.9548F, 0.0F, 0.0F));

		PartDefinition cube_r19 = Cabeça.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(28, 30).addBox(-1.0F, -4.0F, 0.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7F, 0.8F, 3.0F, 0.9948F, 0.0F, 0.0F));

		PartDefinition Orelha = Cabeça.addOrReplaceChild("Orelha", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3054F));

		PartDefinition cube_r20 = Orelha.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(40, 30).addBox(0.0F, -7.0F, 1.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5149F, 0.0F, 0.0F));

		PartDefinition cube_r21 = Orelha.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(36, 42).addBox(0.0F, -6.0F, 1.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -1.0F, 0.5149F, 0.0F, 0.0F));

		PartDefinition Orelha2 = Cabeça.addOrReplaceChild("Orelha2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.5F, -0.9F, 0.0F, 0.0F, 0.0F, 0.3054F));

		PartDefinition cube_r22 = Orelha2.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(0, 41).addBox(0.0F, -7.0F, 1.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5149F, 0.0F, 0.0F));

		PartDefinition cube_r23 = Orelha2.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(42, 42).addBox(0.0F, -6.0F, 1.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -2.0F, -1.0F, 0.5149F, 0.0F, 0.0F));

		PartDefinition Rabo = partdefinition.addOrReplaceChild("Rabo", CubeListBuilder.create(), PartPose.offset(-1.0F, 16.0F, 4.0F));

		PartDefinition cube_r24 = Rabo.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(26, 40).addBox(0.0F, -2.0F, -1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6981F, 0.0F, 0.0F));

		PartDefinition cube_r25 = Rabo.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(14, 30).addBox(-1.0F, -3.0F, -2.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.9F, -0.5672F, 0.0F, 0.0F));

		PartDefinition cube_r26 = Rabo.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(30, 16).addBox(-1.0F, -3.0F, -2.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.1F, 1.4F, -1.2654F, 0.0F, 0.0F));

		PartDefinition cube_r27 = Rabo.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(42, 21).addBox(0.0F, -3.0F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.1F, 1.4F, -1.7017F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(livingEntinty entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Perna.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Perna2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Braço.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Braço2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Cabeça.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Rabo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}