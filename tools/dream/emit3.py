"""Monta as classes de modelo da fase 3 a partir do createBodyLayer gerado por mobs3.py."""
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


def emit(cls, name, layer_name, doc, fields, ctor, anim):
    layer = open(f'build/dream/{layer_name}_layer.txt', encoding='utf-8').read()
    src = (HEAD + f'/** {doc} Geometria e textura: {{@code tools/dream/mobs3.py}}. */\n'
           f'public class {cls} extends EntityModel<DreamMobRenderState> {{\n'
           f'    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "{name}"), "main");\n'
           f'    private final ModelPart {fields};\n\n'
           f'    public {cls}(ModelPart root) {{\n        super(root);\n{ctor}    }}\n\n{layer}\n'
           f'    @Override\n    public void setupAnim(DreamMobRenderState state) {{\n        super.setupAnim(state);\n'
           f'        float age = state.ageInTicks;\n{anim}    }}\n}}\n')
    open(D + cls + '.java', 'w', encoding='utf-8').write(src)


emit('CloudWhaleModel', 'cloud_whale', 'cloud_whale', 'Baleia-Nuvem: o rabo ondula em onda, as nadadeiras remam devagar.',
     'body, tail, tailEnd, flukes, rightFin, leftFin',
     '''        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.tailEnd = this.tail.getChild("tail_end");
        this.flukes = this.tailEnd.getChild("flukes");
        this.rightFin = this.body.getChild("right_fin");
        this.leftFin = this.body.getChild("left_fin");
''',
     '''        float w = age * 0.07F;
        this.body.xRot = Mth.sin(w) * 0.04F;
        this.tail.xRot = Mth.sin(w - 0.8F) * 0.12F;
        this.tailEnd.xRot = Mth.sin(w - 1.6F) * 0.18F;
        this.flukes.xRot = Mth.sin(w - 2.4F) * 0.25F;
        this.rightFin.zRot += Mth.sin(w * 0.8F) * 0.18F;
        this.leftFin.zRot -= Mth.sin(w * 0.8F) * 0.18F;
''')

emit('SkyFishModel', 'sky_fish', 'sky_fish_0', 'Peixe-do-Ar: rabo abanando, barbatanas batendo.',
     'body, tail, rightFin, leftFin',
     '''        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.rightFin = this.body.getChild("right_fin");
        this.leftFin = this.body.getChild("left_fin");
''',
     '''        float speed = 1.0F + state.walkAnimationSpeed * 2.0F;
        this.tail.yRot = Mth.sin(age * 0.6F * speed) * 0.5F;
        this.body.yRot = -Mth.sin(age * 0.6F * speed) * 0.08F;
        this.rightFin.zRot += Mth.sin(age * 0.9F) * 0.3F;
        this.leftFin.zRot -= Mth.sin(age * 0.9F) * 0.3F;
''')

emit('PrismMothModel', 'prism_moth', 'prism_moth', 'Mariposa-Vitral: asas de vitral batendo, abdômen de filamento balançando.',
     'body, head, rightWing, leftWing, rightHind, leftHind, abdomen, rightAntenna, leftAntenna',
     '''        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
        this.rightHind = this.body.getChild("right_hind_wing");
        this.leftHind = this.body.getChild("left_hind_wing");
        this.abdomen = this.body.getChild("abdomen");
        this.rightAntenna = this.head.getChild("right_antenna");
        this.leftAntenna = this.head.getChild("left_antenna");
''',
     '''        float flap = 0.25F + Mth.sin(age * 1.3F) * 0.7F;
        this.rightWing.zRot = flap;
        this.leftWing.zRot = -flap;
        this.rightHind.zRot = flap * 0.8F;
        this.leftHind.zRot = -flap * 0.8F;
        this.abdomen.xRot += Mth.sin(age * 0.2F) * 0.1F;
        this.rightAntenna.zRot += Mth.sin(age * 0.15F) * 0.1F;
        this.leftAntenna.zRot -= Mth.sin(age * 0.15F) * 0.1F;
        this.body.y += Mth.sin(age * 0.25F) * 0.8F;
''')

emit('EchoModel', 'echo', 'echo', 'Eco: anda como você; as cabeças-fantasma tremem em leque, como foto de longa exposição.',
     'head, ghostR, ghostL, ghostR2, ghostL2, rightArm, leftArm, rightLeg, leftLeg',
     '''        this.head = root.getChild("head");
        this.ghostR = this.head.getChild("ghost_head_r");
        this.ghostL = this.head.getChild("ghost_head_l");
        this.ghostR2 = this.head.getChild("ghost_head_r2");
        this.ghostL2 = this.head.getChild("ghost_head_l2");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
''',
     '''        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float j = Mth.sin(age * 0.9F) * 0.06F + (Mth.sin(age * 0.13F) > 0.95F ? 0.2F : 0.0F);
        this.ghostR.zRot -= j;
        this.ghostL.zRot += j;
        this.ghostR2.zRot -= j * 1.6F;
        this.ghostL2.zRot += j * 1.6F;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
        // braços parados, pendurados: é só um reflexo de quem anda
        this.rightArm.zRot = 0.05F;
        this.leftArm.zRot = -0.05F;
''')

watcher_fields = 'crown, ' + ', '.join('thread%d' % i for i in range(6))
watcher_ctor = '        this.crown = root.getChild("crown");\n' + ''.join(
    '        this.thread%d = this.crown.getChild("thread_%d");\n' % (i, i) for i in range(6))
watcher_anim = '''        // gira devagar; no alarme para e todos os olhos se inclinam para a frente
        this.crown.yRot = state.alert ? 0.0F : age * 0.02F;
        this.crown.y += Mth.sin(age * 0.05F) * 0.6F;
        ModelPart[] threads = {this.thread0, this.thread1, this.thread2, this.thread3, this.thread4, this.thread5};
        for (int i = 0; i < threads.length; i++) {
            threads[i].xRot = state.alert ? -0.55F : Mth.sin(age * 0.05F + i * 1.3F) * 0.12F;
            threads[i].zRot = state.alert ? 0.0F : Mth.cos(age * 0.04F + i * 0.9F) * 0.1F;
        }
'''
emit('RemWatcherModel', 'rem_watcher', 'rem_watcher', 'Vigia REM: lustre de olhos que gira devagar; no alarme para e os olhos encaram.',
     watcher_fields, watcher_ctor, watcher_anim)

emit('MemoryModel', 'memory', 'memory', 'Memória: flutua devagar, a barra do vestido balança e a luz da gaiola pulsa.',
     'body, skirt, rightArm, leftArm, light',
     '''        this.body = root.getChild("body");
        this.skirt = this.body.getChild("skirt");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.light = this.body.getChild("cage").getChild("light");
''',
     '''        this.body.y += Mth.sin(age * 0.08F) * 1.0F;
        this.body.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F;
        this.skirt.xRot = Mth.sin(age * 0.06F) * 0.08F + state.walkAnimationSpeed * 0.3F;
        this.rightArm.xRot = Mth.sin(age * 0.05F) * 0.08F;
        this.leftArm.xRot = -Mth.sin(age * 0.05F) * 0.08F;
        float pulse = 1.0F + Mth.sin(age * 0.3F) * 0.08F + Mth.sin(age * 1.7F) * 0.03F;
        this.light.xScale = this.light.yScale = this.light.zScale = pulse;
''')

emit('TheDreamerModel', 'the_dreamer', 'the_dreamer', 'O Sonhador: o véu estrelado ondula, as mãos regem uma canção de ninar e os objetos orbitam a cabeça.',
     'head, veil, veil2, veil3, veil4, rightHand, leftHand, halo',
     '''        this.head = root.getChild("head");
        this.veil = this.head.getChild("veil");
        this.veil2 = this.veil.getChild("veil_2");
        this.veil3 = this.veil2.getChild("veil_3");
        this.veil4 = this.veil3.getChild("veil_4");
        this.rightHand = root.getChild("right_hand");
        this.leftHand = root.getChild("left_hand");
        this.halo = root.getChild("halo");
''',
     '''        float w = age * (state.awake ? 0.1F : 0.05F);
        this.head.y += Mth.sin(w) * 1.2F;
        this.head.zRot = Mth.sin(w * 0.6F) * 0.05F;
        this.veil.xRot = Mth.sin(w - 0.6F) * 0.08F;
        this.veil2.xRot = Mth.sin(w - 1.2F) * 0.12F;
        this.veil3.xRot = Mth.sin(w - 1.8F) * 0.16F;
        this.veil4.xRot = Mth.sin(w - 2.4F) * 0.2F;
        this.veil2.zRot = Mth.cos(w - 1.2F) * 0.06F;
        this.veil4.zRot = Mth.cos(w - 2.4F) * 0.12F;
        // mãos regendo: sobem e descem fora de fase; acordado, viram para a frente como garras
        this.rightHand.y += Mth.sin(w * 1.3F) * 3.0F;
        this.leftHand.y += Mth.sin(w * 1.3F + Mth.PI) * 3.0F;
        this.rightHand.zRot = Mth.sin(w * 1.3F) * 0.25F;
        this.leftHand.zRot = -Mth.sin(w * 1.3F + Mth.PI) * 0.25F;
        if (state.awake) {
            this.rightHand.xRot = -0.9F;
            this.leftHand.xRot = -0.9F;
        }
        if (state.attackTime > 0.0F) {
            float a = Mth.sin(state.attackTime * Mth.PI);
            this.rightHand.z -= a * 8.0F;
            this.leftHand.z -= a * 8.0F;
        }
        this.halo.yRot = age * (state.awake ? 0.08F : 0.035F);
''')
print('ok')
