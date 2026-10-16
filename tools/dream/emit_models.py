"""Monta CountingSheepModel/BogeymanModel/ShadeModel com o createBodyLayer gerado por mobs.py."""
D = 'src/main/java/com/mistech/mistechmod/realm/dream/client/'
HEAD = '''package com.mistech.mistechmod.realm.dream.client;

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

'''
def emit(cls, name, doc, fields, ctor, anim):
    layer = open(f'build/dream/{name}_layer.txt', encoding='utf-8').read()
    src = (HEAD + f'/** {doc} Geometria e textura: {{@code tools/dream/mobs.py}}. */\n'
           f'public class {cls} extends EntityModel<DreamMobRenderState> {{\n'
           f'    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "{name}"), "main");\n'
           f'    private final ModelPart {fields};\n\n'
           f'    public {cls}(ModelPart root) {{\n        super(root);\n{ctor}    }}\n\n{layer}\n'
           f'    @Override\n    public void setupAnim(DreamMobRenderState state) {{\n        super.setupAnim(state);\n{anim}    }}\n}}\n')
    open(D + cls + '.java', 'w', encoding='utf-8').write(src)

emit('CountingSheepModel', 'counting_sheep',
     'Ovelha-de-Contar: uma nuvem de lã com rostinho e pezinhos. Tosquiada, sobra o corpinho rosado e a cabeça encosta nele.',
     'body, wool, tail, head, legFr, legFl, legBr, legBl',
     '''        this.body = root.getChild("body");
        this.wool = root.getChild("wool");
        this.tail = this.wool.getChild("tail");
        this.head = root.getChild("head");
        this.legFr = root.getChild("leg_fr");
        this.legFl = root.getChild("leg_fl");
        this.legBr = root.getChild("leg_br");
        this.legBl = root.getChild("leg_bl");
''',
     '''        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed, age = state.ageInTicks;
        // pezinhos rápidos; a nuvem quica e "respira"
        this.legFr.xRot = Mth.cos(pos * 1.2F) * 1.2F * speed;
        this.legBl.xRot = this.legFr.xRot;
        this.legFl.xRot = Mth.cos(pos * 1.2F + Mth.PI) * 1.2F * speed;
        this.legBr.xRot = this.legFl.xRot;
        float bob = Math.abs(Mth.sin(pos * 0.6F)) * 1.2F * speed;
        float breathe = Mth.sin(age * 0.08F) * 0.025F;
        this.wool.y -= bob;
        this.wool.xScale = this.wool.zScale = 1.0F + breathe;
        this.wool.yScale = 1.0F - breathe;
        this.body.y -= bob;
        this.head.y -= bob;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.zRot = Mth.sin(age * 0.05F) * 0.08F;
        this.tail.yRot = Mth.sin(age * 0.3F) * 0.3F;
        this.wool.visible = !state.sheared;
        this.body.visible = state.sheared;
        if (state.sheared) {
            // sem a lã a cabeça encosta no corpinho
            this.head.z += 4.0F;
            this.head.y += 1.5F;
        }
''')

emit('BogeymanModel', 'bogeyman',
     'Bicho-Papão: fera corcunda, bocão de orelha a orelha, chifre torto e o cobertor da cama nas costas. Na luz se encolhe e cobre o rosto.',
     'rightLeg, leftLeg, torso, quilt, head, jaw, rightArm, leftArm',
     '''        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.torso = root.getChild("torso");
        this.quilt = this.torso.getChild("quilt");
        this.head = this.torso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
''',
     '''        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed, age = state.ageInTicks;
        // anda meio de quatro: braço e perna opostos juntos
        this.rightLeg.xRot = Mth.cos(pos * 0.55F) * 0.9F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.55F + Mth.PI) * 0.9F * speed;
        this.rightArm.xRot = -0.95F + Mth.cos(pos * 0.55F + Mth.PI) * 0.8F * speed;
        this.leftArm.xRot = -0.95F + Mth.cos(pos * 0.55F) * 0.8F * speed;
        this.rightArm.zRot = 0.1F + Mth.sin(age * 0.06F) * 0.05F;
        this.leftArm.zRot = -0.1F - Mth.sin(age * 0.06F) * 0.05F;
        // cabeça inquieta, dentes batendo
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
        this.head.xRot = -0.95F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.zRot = Mth.sin(age * 0.07F) * 0.12F;
        this.jaw.xRot = 0.15F + Math.max(0.0F, Mth.sin(age * 0.35F)) * 0.12F;
        this.quilt.xRot = -0.12F - Mth.sin(pos * 0.55F) * 0.15F * speed - Mth.sin(age * 0.05F) * 0.04F;
        this.torso.xRot = 0.95F;
        if (state.attackTime > 0.0F) {
            float a = Mth.sin(state.attackTime * Mth.PI);
            this.rightArm.xRot -= a * 1.4F;
            this.leftArm.xRot -= a * 1.4F;
            this.jaw.xRot += a * 0.7F;
        }
        if (state.bright) {
            // na luz forte: se encolhe todo e cobre o rosto com os braços
            this.torso.xRot = 1.25F;
            this.head.xRot = -0.6F;
            this.rightArm.xRot = -2.3F;
            this.leftArm.xRot = -2.3F;
            this.rightArm.zRot = -0.45F;
            this.leftArm.zRot = 0.45F;
            this.jaw.xRot = 0.0F;
        }
''')

emit('ShadeModel', 'shade',
     'Sombra: figura alta de capuz e máscara de porcelana, flutua sem pernas. Encarada, congela com os dedos estendidos para você.',
     'head, body, skirt, rightArm, leftArm',
     '''        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.skirt = this.body.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
''',
     '''        float speed = state.walkAnimationSpeed, age = state.ageInTicks;
        if (state.watched) {
            // congelada: cabeça torta, um braço estendido para você, a capa parada no ar
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
            this.head.zRot = 0.35F;
            this.rightArm.xRot = -1.5F;
            this.rightArm.zRot = 0.05F;
            this.leftArm.xRot = -0.2F;
            this.leftArm.zRot = -0.15F;
            this.skirt.xRot = 0.25F;
            return;
        }
        // flutua: sobe e desce devagar, a capa ondula e se arrasta para trás quando anda
        float hover = Mth.sin(age * 0.08F) * 0.8F;
        this.head.y += hover;
        this.body.y += hover;
        this.rightArm.y += hover;
        this.leftArm.y += hover;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(age * 0.11F) * 0.06F;
        this.skirt.xRot = 0.35F * speed + Mth.sin(age * 0.1F) * 0.06F;
        this.skirt.zRot = Mth.sin(age * 0.07F) * 0.05F;
        this.rightArm.xRot = 0.25F * speed + Mth.sin(age * 0.08F) * 0.05F;
        this.leftArm.xRot = 0.25F * speed - Mth.sin(age * 0.08F) * 0.05F;
        this.rightArm.zRot = 0.06F;
        this.leftArm.zRot = -0.06F;
''')
print('ok')
