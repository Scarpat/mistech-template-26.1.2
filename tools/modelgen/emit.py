import sys, shutil
sys.path.insert(0,'/home/claude/gen')
import saci, raven, curupira, werewolf, boitata
OUT='/home/claude/w/src/main/java/com/mistech/mistechmod/folklore/client/'
TEX='/home/claude/w/src/main/resources/assets/mistech/textures/entity/'

def path(M, name):
    p=[q for q in M.parts if q.name==name][0]
    chain=[]
    while p is not None:
        chain.append(p.name); p=p.parent
    return 'root' + ''.join(f'.getChild("{n}")' for n in reversed(chain))

HEADER='''package com.mistech.mistechmod.folklore.client;

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
%s
'''

def emit(M, cls, state, layer, parts, anim, doc, extra_import=''):
    M.pack()
    fields='\n'.join(f'    private final ModelPart {camel(p)};' for p in parts)
    init='\n'.join(f'        this.{camel(p)} = {path(M,p)};' for p in parts)
    java = HEADER % extra_import + f'''
/** {doc} Geometria e textura geradas por script (gen/{M.name}.py). */
public class {cls} extends EntityModel<{state}> {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "{layer}"), "main");

{fields}

    public {cls}(ModelPart root) {{
        super(root);
{init}
    }}

{M.java()}
    @Override
    public void setupAnim({state} state) {{
        super.setupAnim(state);
{anim}
    }}
}}
'''
    java = java.replace('PartDefinition p_', 'PartDefinition p_')
    open(OUT+cls+'.java','w').write(java)

def camel(n):
    a=n.split('_'); return a[0]+''.join(x.title() for x in a[1:])

# ---------- Saci ----------
M=saci.m(); M.pack(); M.paint(TEX+'saci.png', TEX+'saci_glow.png')
emit(M,'SaciModel','FolkRenderState','saci',
 ['body','shorts','leg','head','cap_tip','cap_tip2','right_arm','left_arm'],
'''        float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.5F);
        float pos = state.walkAnimationPos;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // pula numa perna só
        float hop = Math.abs(Mth.sin(pos * 0.6662F)) * 2.5F * speed;
        float drop = state.sitting ? 4.0F : 0.0F;
        for (ModelPart p : new ModelPart[] {this.body, this.shorts, this.leg, this.head, this.rightArm, this.leftArm}) {
            p.y += drop - hop;
        }
        this.leg.xRot = state.sitting ? -1.4F : -0.35F * hop / 2.5F;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F) * 1.1F * speed;
        this.leftArm.xRot = -this.rightArm.xRot;
        this.rightArm.zRot = 0.12F + Mth.sin(state.ageInTicks * 0.09F) * 0.05F;
        this.leftArm.zRot = -this.rightArm.zRot;
        // a ponta do gorro balança
        this.capTip.zRot = Mth.sin(state.ageInTicks * 0.1F) * 0.15F;
        this.capTip2.zRot = Mth.sin(state.ageInTicks * 0.1F + 0.6F) * 0.25F;''',
 'Saci: menino de uma perna só, gorro vermelho de ponta e cachimbo no canto da boca.')

# ---------- Curupira ----------
M=curupira.m(); M.pack(); M.paint(TEX+'curupira.png', TEX+'curupira_glow.png')
emit(M,'CurupiraModel','FolkRenderState','curupira',
 ['body','skirt','head','flame_a','flame_b','flame_c','right_arm','left_arm','right_leg','left_leg'],
'''        float speed = state.walkAnimationSpeed;
        float pos = state.walkAnimationPos;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 1.0F * speed;
        this.skirt.xRot = Math.abs(Mth.cos(pos * 0.6662F)) * 0.08F * speed;
        if (state.sitting) {
            for (ModelPart p : new ModelPart[] {this.body, this.skirt, this.head, this.rightArm, this.leftArm}) {
                p.y += 5.0F;
            }
            this.rightLeg.y += 5.0F;
            this.leftLeg.y += 5.0F;
            this.rightLeg.xRot = -1.4F;
            this.leftLeg.xRot = -1.4F;
            this.rightLeg.yRot = 0.3F;
            this.leftLeg.yRot = -0.3F;
        }
        // o cabelo de fogo tremula
        float t = state.ageInTicks;
        this.flameA.yScale = 1.0F + Mth.sin(t * 0.45F) * 0.18F;
        this.flameB.yScale = 1.0F + Mth.sin(t * 0.5F + 1.3F) * 0.22F;
        this.flameC.yScale = 1.0F + Mth.sin(t * 0.4F + 2.1F) * 0.18F;
        this.flameB.zRot += Mth.sin(t * 0.2F) * 0.08F;''',
 'Curupira: menino de cabelo de fogo, saia de folhas e pés virados para trás.')

# ---------- Lobisomem (modelo do Blockbench do usuário + textura dele) ----------
import bbparse, shutil as _sh
from repivot import repivot
M=bbparse.parse('/home/claude/gen/bb/Lobisomem.java',64,64,'werewolf')
P={p.name:p for p in M.parts}
# articulações no quadril e nos ombros (a pose parada não muda)
repivot(P['Perna'],(-3,15.5,0)); repivot(P['Perna2'],(3,15.5,0))
repivot(P['Braço'],(4,8,-3.2)); repivot(P['Braço2'],(-4,8,-3.2))
for old,new_ in (('Perna','right_leg'),('Perna2','left_leg'),('Braço','left_arm'),('Braço2','right_arm'),('Torso','torso'),
                 ('Cabeça','head'),('Orelha','right_ear'),('Orelha2','left_ear'),('Rabo','tail')):
    P[old].name=new_
M.pack=lambda: None   # UV do Blockbench
_sh.copy('/home/claude/gen/bb/lobisomem.png', TEX+'werewolf.png')
_sh.copy('/home/claude/gen/out/lobo_eyes.png', TEX+'werewolf_eyes.png')
emit(M,'WerewolfModel','WerewolfRenderState','werewolf',
 ['torso','head','right_ear','left_ear','right_arm','left_arm','right_leg','left_leg','tail'],
'''        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float pos = state.walkAnimationPos;
        float t = state.ageInTicks;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
        this.torso.xRot += Mth.sin(t * 0.08F) * 0.03F;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 0.9F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 0.9F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 0.7F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 0.7F * speed;
        if (state.attackAnim > 0.0F) {
            float swing = Mth.sin(state.attackAnim * Mth.PI);
            this.rightArm.xRot -= swing * 1.4F;
            this.leftArm.xRot -= swing * 1.1F;
            this.head.xRot += swing * 0.3F;
        }
        // orelhas mexendo de vez em quando
        float twitch = Mth.sin(t * 0.07F) > 0.93F ? 0.25F : 0.0F;
        this.rightEar.zRot -= twitch;
        this.leftEar.zRot += twitch * 0.6F;
        this.tail.yRot = Mth.sin(t * 0.12F) * 0.2F + Mth.cos(pos * 0.6662F) * 0.25F * speed;''',
 'Lobisomem (lobo-guará): modelo do Blockbench feito pelo usuário; pivôs de pernas e braços movidos para o quadril e os ombros.')

# ---------- Corvo (mesmas poses da Coruja) ----------
M=raven.m(); M.pack(); M.paint(TEX+'raven.png')
emit(M,'RavenModel','ParrotRenderState','raven',
 ['body','head','left_wing','right_wing','tail','left_leg','right_leg'],
'''        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float bob = state.flapAngle * 0.3F;
        switch (state.pose) {
            case SITTING -> {
                for (ModelPart p : new ModelPart[] {this.body, this.head, this.leftWing, this.rightWing, this.tail}) {
                    p.y += 1.0F;
                }
                this.leftLeg.visible = false;
                this.rightLeg.visible = false;
            }
            case FLYING -> {
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.body.xRot = 0.45F;
                this.leftWing.zRot = -1.2F - state.flapAngle * 1.2F;
                this.rightWing.zRot = 1.2F + state.flapAngle * 1.2F;
                this.leftLeg.xRot = 0.9F;
                this.rightLeg.xRot = 0.9F;
                for (ModelPart p : new ModelPart[] {this.body, this.head, this.leftWing, this.rightWing, this.tail}) {
                    p.y += bob;
                }
            }
            default -> {
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
                this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI) * 1.2F * state.walkAnimationSpeed;
                this.leftWing.zRot = -0.05F - state.flapAngle * 0.5F;
                this.rightWing.zRot = 0.05F + state.flapAngle * 0.5F;
                // o corvo inclina a cabeça de lado de vez em quando
                this.head.zRot = Mth.sin(state.ageInTicks * 0.06F) > 0.9F ? -0.4F : 0.0F;
            }
        }''',
 'Corvo: pássaro preto de brilho azulado, bico grosso e cauda em leque.',
 'import net.minecraft.client.renderer.entity.state.ParrotRenderState;\n')

# ---------- Boitatá ----------
H=boitata.head(); H.pack(); H.paint(TEX+'boitata.png')
emit(H,'BoitataModel','BoitataRenderState','boitata',
 ['jaw','crest_a','crest_b','crest_c','right_whisker','left_whisker'],
'''        float t = state.ageInTicks;
        this.jaw.xRot += 0.15F + Mth.sin(t * 0.15F) * 0.15F + state.rage * 0.25F;
        this.crestA.yScale = 1.0F + Mth.sin(t * 0.5F) * 0.2F;
        this.crestB.yScale = 1.0F + Mth.sin(t * 0.45F + 1.0F) * 0.25F;
        this.crestC.yScale = 1.0F + Mth.sin(t * 0.55F + 2.0F) * 0.2F;
        this.rightWhisker.yRot += Mth.sin(t * 0.12F) * 0.25F;
        this.leftWhisker.yRot -= Mth.sin(t * 0.12F) * 0.25F;''',
 'Boitatá (cabeça): cabeça de cobra de fogo com olhos enormes e crista de chamas.')
S=boitata.segment(); S.pack(); S.paint(TEX+'boitata_segment.png')
emit(S,'BoitataSegmentModel','BoitataRenderState','boitata_segment',['fin'],
'''        this.fin.yScale = 1.0F + Mth.sin(state.ageInTicks * 0.4F) * 0.2F;''',
 'Boitatá (segmento do corpo), desenhado várias vezes ao longo do rastro.')
shutil.copy('/home/claude/gen/out/headless_mule.png', TEX+'headless_mule.png')
shutil.copy('/home/claude/gen/out/headless_mule_glow.png', TEX+'headless_mule_glow.png')
print('ok')
