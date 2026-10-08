package com.mistech.mistechmod.folklore.client;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.folklore.BoitataEntity;
import com.mistech.mistechmod.folklore.CurupiraEntity;
import com.mistech.mistechmod.folklore.HeadlessMuleEntity;
import com.mistech.mistechmod.folklore.RavenEntity;
import com.mistech.mistechmod.folklore.SaciEntity;
import com.mistech.mistechmod.folklore.WerewolfEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.model.EquipmentClientInfo;

/** Renderizadores das criaturas do folclore. */
public final class FolkRenderers {
    private FolkRenderers() {}

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    static <S extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<S>>
    EyesLayer<S, M> glow(MobRenderer<?, S, M> parent, String texture) {
        RenderType type = RenderTypes.eyes(tex(texture));
        return new EyesLayer<>(parent) {
            @Override
            public RenderType renderType() {
                return type;
            }
        };
    }

    // ----- Saci -----
    public static class Saci extends MobRenderer<SaciEntity, FolkRenderState, SaciModel> {
        private static final Identifier TEXTURE = tex("saci");

        public Saci(EntityRendererProvider.Context context) {
            super(context, new SaciModel(context.bakeLayer(SaciModel.LAYER)), 0.3F);
            this.addLayer(glow(this, "saci_glow")); // brasa do cachimbo
        }

        @Override
        public FolkRenderState createRenderState() {
            return new FolkRenderState();
        }

        @Override
        public void extractRenderState(SaciEntity entity, FolkRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.sitting = entity.isInSittingPose();
        }

        @Override
        public Identifier getTextureLocation(FolkRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Curupira (cabelo brilhando) -----
    public static class Curupira extends MobRenderer<CurupiraEntity, FolkRenderState, CurupiraModel> {
        private static final Identifier TEXTURE = tex("curupira");

        public Curupira(EntityRendererProvider.Context context) {
            super(context, new CurupiraModel(context.bakeLayer(CurupiraModel.LAYER)), 0.35F);
            this.addLayer(glow(this, "curupira_glow"));
        }

        @Override
        public FolkRenderState createRenderState() {
            return new FolkRenderState();
        }

        @Override
        public void extractRenderState(CurupiraEntity entity, FolkRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.sitting = entity.isInSittingPose();
        }

        @Override
        public Identifier getTextureLocation(FolkRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Corvo -----
    public static class Raven extends MobRenderer<RavenEntity, ParrotRenderState, RavenModel> {
        private static final Identifier TEXTURE = tex("raven");

        public Raven(EntityRendererProvider.Context context) {
            super(context, new RavenModel(context.bakeLayer(RavenModel.LAYER)), 0.3F);
        }

        @Override
        public ParrotRenderState createRenderState() {
            return new ParrotRenderState();
        }

        @Override
        public void extractRenderState(RavenEntity entity, ParrotRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            float flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
            float flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
            state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
            state.pose = ParrotModel.getPose(entity);
        }

        @Override
        public Identifier getTextureLocation(ParrotRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Lobisomem (olhos brilhando) -----
    public static class Werewolf extends MobRenderer<WerewolfEntity, WerewolfRenderState, WerewolfModel> {
        private static final Identifier TEXTURE = tex("werewolf");

        public Werewolf(EntityRendererProvider.Context context) {
            super(context, new WerewolfModel(context.bakeLayer(WerewolfModel.LAYER)), 0.6F);
            this.addLayer(glow(this, "werewolf_eyes"));
        }

        /** O modelo do Blockbench tem ~1,4 bloco; o lobisomem anda com ~2,3. */
        public static final float SCALE = 1.6F;

        @Override
        protected void scale(WerewolfRenderState state, PoseStack poseStack) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }

        @Override
        public WerewolfRenderState createRenderState() {
            return new WerewolfRenderState();
        }

        @Override
        public void extractRenderState(WerewolfEntity entity, WerewolfRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.attackAnim = entity.getAttackAnim(partialTicks);
        }

        @Override
        public Identifier getTextureLocation(WerewolfRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Boitatá: cabeça + corpo de segmentos ao longo do rastro -----
    public static class Boitata extends MobRenderer<BoitataEntity, BoitataRenderState, BoitataModel> {
        private static final Identifier TEXTURE = tex("boitata");
        private static final Identifier SEGMENT_TEXTURE = tex("boitata_segment");
        private static final float SCALE = 2.0F;
        private static final int FULL_BRIGHT = 0xF000F0;
        private final BoitataSegmentModel segment;

        public Boitata(EntityRendererProvider.Context context) {
            super(context, new BoitataModel(context.bakeLayer(BoitataModel.LAYER)), 1.2F);
            this.segment = new BoitataSegmentModel(context.bakeLayer(BoitataSegmentModel.LAYER));
        }

        @Override
        public BoitataRenderState createRenderState() {
            return new BoitataRenderState();
        }

        /** O corpo se estende bem além da cabeça: não some quando a cabeça sai da tela. */
        @Override
        protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(BoitataEntity entity) {
            return entity.getBoundingBox().inflate(BoitataEntity.SEGMENTS * BoitataEntity.SPACING);
        }

        @Override
        protected int getBlockLightLevel(BoitataEntity entity, BlockPos pos) {
            return 15;
        }

        @Override
        public void extractRenderState(BoitataEntity entity, BoitataRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            Vec3 head = entity.getPosition(partialTicks);
            List<Vec3> segs = entity.segments(head);
            int n = segs.size();
            state.segments = new Vec3[n];
            state.segmentYaw = new float[n];
            state.segmentPitch = new float[n];
            Vec3 prev = head;
            for (int i = 0; i < n; i++) {
                Vec3 s = segs.get(i);
                state.segments[i] = s.subtract(head);
                Vec3 d = prev.subtract(s);
                state.segmentYaw[i] = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
                state.segmentPitch[i] = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
                prev = s;
            }
            state.rage = entity.getHealth() < entity.getMaxHealth() / 2 ? 1.0F : 0.0F;
        }

        @Override
        protected void scale(BoitataRenderState state, PoseStack poseStack) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }

        @Override
        protected void setupRotations(BoitataRenderState state, PoseStack poseStack, float bodyRot, float scale) {
            super.setupRotations(state, poseStack, bodyRot, scale);
            poseStack.translate(0.0F, 0.5F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }

        @Override
        public void submit(BoitataRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            RenderType type = this.segment.renderType(SEGMENT_TEXTURE);
            int n = state.segments.length;
            for (int i = 0; i < n; i++) {
                Vec3 rel = state.segments[i];
                float s = SCALE * (1.0F - 0.55F * i / Math.max(1, n - 1));
                poseStack.pushPose();
                poseStack.translate(rel.x, rel.y + 0.6 * s, rel.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-state.segmentYaw[i]));
                poseStack.mulPose(Axis.XP.rotationDegrees(-state.segmentPitch[i]));
                poseStack.scale(-s, -s, s);
                collector.submitModel(this.segment, state, poseStack, type, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
                poseStack.popPose();
            }
        }

        @Override
        public Identifier getTextureLocation(BoitataRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Mula sem Cabeça: corpo de mula carbonizado, pescoço cortado em brasa, chamas no lugar da cabeça e crina de fogo -----
    public static class HeadlessModel extends DonkeyModel {
        public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
                new net.minecraft.client.model.geom.ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "headless_mule"), "main");
        private final ModelPart flames;
        private final ModelPart flameA;
        private final ModelPart flameB;
        private final ModelPart mane;

        public HeadlessModel(ModelPart root) {
            super(root);
            this.flames = this.headParts.getChild("flames");
            this.flameA = this.flames.getChild("flame_a");
            this.flameB = this.flames.getChild("flame_b");
            this.mane = this.headParts.getChild("fire_mane");
        }

        /** A malha da mula do jogo (com os baús), trocando a cabeça por um toco de pescoço com fogo. */
        public static net.minecraft.client.model.geom.builders.LayerDefinition createBodyLayer() {
            net.minecraft.client.model.geom.builders.MeshDefinition mesh =
                    net.minecraft.client.model.animal.equine.AbstractEquineModel.createBodyMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE);
            net.minecraft.client.model.geom.builders.PartDefinition root = mesh.getRoot();
            net.minecraft.client.model.geom.builders.PartDefinition body = root.getChild("body");
            net.minecraft.client.model.geom.builders.CubeListBuilder chest = net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                    .texOffs(26, 21).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F);
            body.addOrReplaceChild("left_chest", chest,
                    net.minecraft.client.model.geom.PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F));
            body.addOrReplaceChild("right_chest", chest,
                    net.minecraft.client.model.geom.PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
            // pescoço mais curto, cortado no topo (a face de cima é a carne em brasa)
            net.minecraft.client.model.geom.builders.PartDefinition neck = root.addOrReplaceChild("head_parts",
                    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 35).addBox(-2.05F, -4.0F, -2.0F, 4.0F, 10.0F, 7.0F),
                    net.minecraft.client.model.geom.PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
            // duas labaredas cruzadas saindo do corte (ficam sempre em pé, ver setupAnim)
            net.minecraft.client.model.geom.builders.PartDefinition flames = neck.addOrReplaceChild("flames",
                    net.minecraft.client.model.geom.builders.CubeListBuilder.create(),
                    net.minecraft.client.model.geom.PartPose.offset(0.0F, -4.0F, 1.5F));
            net.minecraft.client.model.geom.builders.CubeListBuilder flame = net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-4.0F, -12.0F, 0.0F, 8.0F, 12.0F, 0.0F);
            flames.addOrReplaceChild("flame_a", flame, net.minecraft.client.model.geom.PartPose.ZERO);
            flames.addOrReplaceChild("flame_b", flame, net.minecraft.client.model.geom.PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F));
            // crina de fogo na nuca, puxada para trás
            neck.addOrReplaceChild("fire_mane",
                    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(16, 0).addBox(0.0F, 0.0F, 0.0F, 0.0F, 10.0F, 4.0F),
                    net.minecraft.client.model.geom.PartPose.offsetAndRotation(0.0F, -4.0F, 5.0F, -0.25F, 0.0F, 0.0F));
            return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh, 64, 64)
                    .apply(net.minecraft.client.model.geom.builders.MeshTransformer.scaling(DonkeyModel.MULE_SCALE));
        }

        @Override
        public void setupAnim(DonkeyRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            // as chamas sobem retas, não importa a inclinação do pescoço
            this.flames.xRot = -this.headParts.xRot;
            this.flames.yScale = 1.0F + 0.14F * Mth.sin(t * 0.8F) + 0.06F * Mth.sin(t * 2.3F);
            this.flames.xScale = 1.0F - 0.06F * Mth.sin(t * 1.1F + 1.0F);
            this.flames.zScale = this.flames.xScale;
            this.flameA.yRot = 0.18F * Mth.sin(t * 0.55F);
            this.flameB.yRot = (float) (Math.PI / 2) + 0.18F * Mth.sin(t * 0.6F + 2.0F);
            // a crina tremula e deita para trás quando ela corre
            this.mane.xRot = -0.25F - state.walkAnimationSpeed * 0.5F + 0.08F * Mth.sin(t * 0.9F);
            this.mane.zScale = 1.0F + 0.15F * Mth.sin(t * 1.3F);
        }
    }

    public static class HeadlessSaddleModel extends EquineSaddleModel {
        public HeadlessSaddleModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EquineRenderState state) {
            super.setupAnim(state);
            this.headParts.visible = false;
        }
    }

    public static class HeadlessMule extends AbstractHorseRenderer<HeadlessMuleEntity, DonkeyRenderState, DonkeyModel> {
        private static final Identifier TEXTURE = tex("headless_mule");

        public HeadlessMule(EntityRendererProvider.Context context) {
            this(context, new HeadlessModel(context.bakeLayer(HeadlessModel.LAYER)));
        }

        private HeadlessMule(EntityRendererProvider.Context context, HeadlessModel model) {
            super(context, model, model);
            this.addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.MULE_SADDLE,
                    state -> state.saddle, new HeadlessSaddleModel(context.bakeLayer(ModelLayers.MULE_SADDLE)), null));
            RenderType glow = RenderTypes.eyes(tex("headless_mule_glow"));
            this.addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return glow;
                }
            });
        }

        @Override
        public DonkeyRenderState createRenderState() {
            return new DonkeyRenderState();
        }

        @Override
        public void extractRenderState(HeadlessMuleEntity entity, DonkeyRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.hasChest = entity.hasChest();
        }

        @Override
        public Identifier getTextureLocation(DonkeyRenderState state) {
            return TEXTURE;
        }
    }
}
