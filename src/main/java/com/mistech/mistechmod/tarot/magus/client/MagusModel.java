package com.mistech.mistechmod.tarot.magus.client;

import java.util.Map;

import com.mistech.mistechmod.tarot.magus.MagusElement;
import com.mistech.mistechmod.tarot.magus.MagusEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * As cinco formas do Mago num modelo só (geometria em {@link MagusGeometry}); mostra a forma atual e anima.
 * Terra = colosso de pedra e raiz; Ar = harpia de lâminas; Água = leviatã com a taça nas costas; Fogo = demônio com o
 * bastão fundido ao braço; Quintessência = o Mago humanoide, alto, com o ∞ e as quatro ferramentas girando em volta.
 */
public class MagusModel extends EntityModel<MagusRenderState> {
    /** Escala de desenho de cada forma (Terra, Ar, Água, Fogo, Quintessência). */
    static final float[] FORM_SCALE = {1.25F, 1.1F, 1.3F, 1.1F, 1.2F};
    private static final float PI = Mth.PI;

    private final Map<String, ModelPart> p;
    private final ModelPart[] forms;

    public MagusModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.p = MagusGeometry.bodyParts(root);
        this.forms = new ModelPart[] {this.p.get("earth"), this.p.get("air"), this.p.get("water"), this.p.get("fire"), this.p.get("quint")};
    }

    private ModelPart g(String name) {
        return this.p.get(name);
    }

    @Override
    public void setupAnim(MagusRenderState s) {
        super.setupAnim(s);
        int form = Mth.clamp(s.form, 0, 4);
        for (int i = 0; i < 5; i++) {
            this.forms[i].visible = i == form;
        }
        ModelPart root = this.forms[form];
        float scale = FORM_SCALE[form] * (s.aspect ? 0.6F : 1.0F);
        // metamorfose: encolhe girando, troca, cresce de novo
        if (s.action == MagusEntity.MORPH) {
            float t = Mth.clamp(s.actionTime / 24.0F, 0.0F, 1.0F);
            float k = t < 0.5F ? Mth.lerp(t * 2.0F, 1.0F, 0.2F) : Mth.lerp((t - 0.5F) * 2.0F, 0.2F, 1.0F);
            scale *= k;
            root.yRot += t * PI * 4.0F;
        } else if (s.action == MagusEntity.SPLIT) {
            float t = Mth.clamp(s.actionTime / 40.0F, 0.0F, 1.0F);
            scale *= 1.0F + t * 0.3F;
            root.yRot += t * t * PI * 6.0F;
        } else if (s.action == MagusEntity.REFORM) {
            scale *= 0.01F;
        }
        root.xScale = root.yScale = root.zScale = scale;
        float t = s.ageInTicks;
        float a = s.actionTime;
        switch (MagusElement.byId(form)) {
            case EARTH -> this.earth(s, t, a);
            case AIR -> this.air(s, t, a);
            case WATER -> this.water(s, t, a);
            case FIRE -> this.fire(s, t, a);
            case QUINTESSENCE -> this.quint(s, t, a);
        }
    }

    private static float ease(float a, float start, float end) {
        float x = Mth.clamp((a - start) / (end - start), 0.0F, 1.0F);
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------------ TERRA
    private void earth(MagusRenderState s, float t, float a) {
        float walk = s.walkAnimationPos, amt = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        ModelPart torso = g("earth_torso"), head = g("earth_head"), jaw = g("earth_jaw");
        ModelPart ar = g("earth_arm_r"), al = g("earth_arm_l"), fr = g("earth_fore_r"), fl = g("earth_fore_l");
        // respiração pesada
        torso.xRot += Mth.sin(t * 0.06F) * 0.03F;
        g("earth_waist").zRot += Mth.sin(walk * 0.5F) * 0.06F * amt;
        head.yRot += Mth.clamp(s.yRot, -40, 40) * Mth.DEG_TO_RAD * 0.7F;
        head.xRot += s.xRot * Mth.DEG_TO_RAD * 0.4F;
        jaw.xRot += 0.05F + Mth.sin(t * 0.05F) * 0.04F;
        // andar de gorila: braços alternados apoiando os punhos
        ar.xRot += Mth.cos(walk * 0.5F) * 0.45F * amt + Mth.sin(t * 0.05F) * 0.03F;
        al.xRot += -Mth.cos(walk * 0.5F) * 0.45F * amt - Mth.sin(t * 0.05F) * 0.03F;
        switch (s.action) {
            case MagusEntity.SLAM -> {
                float up = ease(a, 0, 10), down = ease(a, 10, 13);
                float arm = -2.6F * up + 2.9F * down;
                ar.xRot += arm;
                al.xRot += arm;
                fr.xRot += -0.4F * up + 0.4F * down;
                fl.xRot += -0.4F * up + 0.4F * down;
                torso.xRot += -0.25F * up + 0.55F * down;
                jaw.xRot += 0.5F * up;
            }
            case MagusEntity.STOMP -> {
                float up = ease(a, 0, 12), down = ease(a, 12, 15);
                g("earth_waist").xRot += -0.35F * up + 0.45F * down;
                ar.zRot += 0.9F * up - 0.9F * down;
                al.zRot += -0.9F * up + 0.9F * down;
                ar.xRot += -1.2F * up + 1.4F * down;
                al.xRot += -1.2F * up + 1.4F * down;
                jaw.xRot += 0.6F * up;
                head.xRot += -0.3F * up;
            }
            case MagusEntity.THROW -> {
                float back = ease(a, 0, 14), fwd = ease(a, 14, 18);
                ar.xRot += -3.0F * back + 1.9F * fwd;
                ar.zRot += 0.2F * back;
                torso.yRot += 0.4F * back - 0.8F * fwd;
                fr.xRot += -0.6F * back + 0.6F * fwd;
            }
            case MagusEntity.STUN -> {
                head.xRot += 0.5F;
                head.zRot += Mth.sin(t * 0.3F) * 0.15F;
                torso.xRot += 0.3F;
                ar.xRot += 0.4F;
                al.xRot += 0.4F;
                jaw.xRot += 0.35F;
            }
            case MagusEntity.ROAR -> {
                head.xRot += -0.4F * ease(a, 0, 8);
                jaw.xRot += 0.7F * ease(a, 0, 8);
            }
            default -> {}
        }
        // cristais vibram de leve
        for (int i = 0; i < 5; i++) {
            g("earth_crystal_" + i).zRot += Mth.sin(t * 0.2F + i) * 0.015F;
        }
    }

    // ------------------------------------------------------------------ AR
    private void air(MagusRenderState s, float t, float a) {
        ModelPart body = g("air_body"), head = g("air_head");
        float flap = Mth.sin(t * 0.25F);
        body.y += Mth.sin(t * 0.1F) * 1.5F;
        body.xRot += 0.15F + Mth.sin(t * 0.1F) * 0.04F;
        head.yRot += Mth.clamp(s.yRot, -50, 50) * Mth.DEG_TO_RAD;
        head.xRot += s.xRot * Mth.DEG_TO_RAD * 0.5F;
        g("air_beak_low").xRot += 0.1F + Mth.sin(t * 0.13F) * 0.08F;
        float spread = 0.0F, sweep = 0.0F;
        switch (s.action) {
            case MagusEntity.CAST -> {
                spread = 0.5F * ease(a, 0, 6) - 0.5F * ease(a, 8, 14);
                g("air_beak_low").xRot += 0.5F * ease(a, 0, 6);
            }
            case MagusEntity.BREATH -> body.yRot += a * 0.55F;
            case MagusEntity.SLAM -> sweep = 1.2F * ease(a, 0, 8) - 1.6F * ease(a, 8, 12);
            case MagusEntity.BLINK -> spread = -0.9F;
            default -> {}
        }
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1 : 1;
            String sd = side == 0 ? "r" : "l";
            ModelPart wing = g("air_wing_" + sd);
            wing.zRot += sx * (flap * 0.28F - spread);
            wing.yRot += sx * -sweep;
            g("air_wingtip_" + sd).zRot += sx * Mth.sin(t * 0.25F - 0.6F) * 0.25F;
            for (int i = 0; i < 7; i++) {
                ModelPart f = g("air_feather_" + sd + i);
                f.xRot += Mth.sin(t * 0.25F - i * 0.3F) * 0.08F;
            }
            ModelPart arm = g("air_arm_" + sd);
            arm.xRot += Mth.sin(t * 0.12F + side) * 0.12F - (s.action == MagusEntity.CAST ? 0.9F * ease(a, 0, 6) : 0);
            g("air_claws_" + sd).xRot += Mth.sin(t * 0.3F + side) * 0.15F;
        }
        g("air_tail1").xRot += Mth.sin(t * 0.12F) * 0.12F;
        g("air_tail2").xRot += Mth.sin(t * 0.12F - 0.5F) * 0.15F;
        g("air_tail3").xRot += Mth.sin(t * 0.12F - 1.0F) * 0.2F;
        g("air_tail4").xRot += Mth.sin(t * 0.12F - 1.5F) * 0.25F;
        g("air_tail2").zRot += Mth.sin(t * 0.09F) * 0.12F;
        g("air_tail3").zRot += Mth.sin(t * 0.09F - 0.6F) * 0.16F;
    }

    // ------------------------------------------------------------------ ÁGUA
    private void water(MagusRenderState s, float t, float a) {
        ModelPart body = g("water_body"), jaw = g("water_jaw"), head = g("water_head");
        body.y += Mth.sin(t * 0.08F) * 1.2F;
        body.zRot += Mth.sin(t * 0.05F) * 0.03F;
        head.yRot += Mth.clamp(s.yRot, -30, 30) * Mth.DEG_TO_RAD * 0.6F;
        jaw.xRot += Mth.sin(t * 0.06F) * 0.08F;
        g("water_lure").xRot += Mth.sin(t * 0.1F) * 0.15F;
        g("water_lure2").xRot += Mth.sin(t * 0.1F - 0.5F) * 0.2F;
        g("water_lure").zRot += Mth.sin(t * 0.07F) * 0.12F;
        for (int i = 0; i < 6; i++) {
            float ph = i * 1.05F;
            g("water_tent" + i + "_1").xRot += Mth.sin(t * 0.12F + ph) * 0.15F;
            g("water_tent" + i + "_2").xRot += Mth.sin(t * 0.12F + ph - 0.7F) * 0.25F;
            g("water_tent" + i + "_3").xRot += Mth.sin(t * 0.12F + ph - 1.4F) * 0.35F;
            g("water_tent" + i + "_1").zRot += Mth.cos(t * 0.1F + ph) * 0.1F;
        }
        for (int side = 0; side < 2; side++) {
            String sd = side == 0 ? "r" : "l";
            float sx = side == 0 ? -1 : 1;
            g("water_arm_" + sd).xRot += Mth.sin(t * 0.1F + side * PI) * 0.2F;
            g("water_hand_" + sd).zRot += sx * Mth.sin(t * 0.15F) * 0.2F;
        }
        switch (s.action) {
            case MagusEntity.CAST -> {
                float k = ease(a, 0, 8) - ease(a, 12, 18);
                g("water_arm_r").xRot += -1.6F * k;
                g("water_arm_l").xRot += -1.6F * k;
                jaw.xRot += 0.4F * k;
                g("water_lure_tip").xScale = g("water_lure_tip").yScale = g("water_lure_tip").zScale = 1.0F + k * 0.8F;
            }
            case MagusEntity.ROAR -> {
                float k = ease(a, 0, 12) - ease(a, 22, 30);
                body.xRot += -0.25F * k;
                jaw.xRot += 0.8F * k;
                g("water_arm_r").zRot += 0.8F * k;
                g("water_arm_l").zRot += -0.8F * k;
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ FOGO
    private void fire(MagusRenderState s, float t, float a) {
        float walk = s.walkAnimationPos, amt = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        ModelPart torso = g("fire_torso"), head = g("fire_head"), jaw = g("fire_jaw");
        ModelPart ar = g("fire_arm_r"), al = g("fire_arm_l");
        torso.xRot += Mth.sin(t * 0.08F) * 0.04F;
        head.yRot += Mth.clamp(s.yRot, -45, 45) * Mth.DEG_TO_RAD;
        head.xRot += s.xRot * Mth.DEG_TO_RAD * 0.5F;
        jaw.xRot += Mth.sin(t * 0.09F) * 0.05F;
        g("fire_thigh_r").xRot += Mth.cos(walk * 0.6F) * 0.5F * amt;
        g("fire_thigh_l").xRot += -Mth.cos(walk * 0.6F) * 0.5F * amt;
        g("fire_tail1").yRot += Mth.sin(t * 0.1F) * 0.25F;
        g("fire_tail2").yRot += Mth.sin(t * 0.1F - 0.6F) * 0.3F;
        ModelPart crown = g("fire_crown");
        crown.yScale = 1.0F + Mth.sin(t * 0.6F) * 0.15F;
        ModelPart tip = g("fire_staff_tip");
        float flick = 1.0F + Mth.sin(t * 0.7F) * 0.1F;
        tip.xScale = tip.yScale = tip.zScale = flick;
        al.xRot += Mth.sin(t * 0.07F) * 0.1F - Mth.cos(walk * 0.6F) * 0.3F * amt;
        ar.xRot += Mth.cos(walk * 0.6F) * 0.2F * amt;
        switch (s.action) {
            case MagusEntity.CAST -> {
                float k = ease(a, 0, 10) - ease(a, 22, 30);
                ar.xRot += -2.6F * k;
                g("fire_fore_r").xRot += 0.8F * k;
                head.xRot += -0.4F * k;
                jaw.xRot += 0.4F * k;
                tip.xScale = tip.yScale = tip.zScale = flick * (1.0F + 0.6F * k);
            }
            case MagusEntity.BREATH -> {
                float k = ease(a, 0, 8) - ease(a, 44, 50);
                jaw.xRot += 0.8F * k;
                head.xRot += 0.15F * k;
                torso.xRot += 0.15F * k;
            }
            case MagusEntity.THROW -> {
                float back = ease(a, 0, 10), fwd = ease(a, 10, 14);
                ar.xRot += -2.4F * back + 1.6F * fwd;
                torso.yRot += 0.3F * back - 0.6F * fwd;
            }
            case MagusEntity.STOMP -> {
                float up = ease(a, 0, 10), down = ease(a, 10, 13);
                ar.xRot += -2.0F * up + 2.4F * down;
                g("fire_fore_r").xRot += 0.6F * down;
                torso.xRot += -0.2F * up + 0.4F * down;
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ QUINTESSÊNCIA
    private void quint(MagusRenderState s, float t, float a) {
        ModelPart body = g("quint_body"), head = g("quint_head"), skirt = g("quint_skirt");
        ModelPart ar = g("quint_arm_r"), al = g("quint_arm_l"), fr = g("quint_fore_r"), fl = g("quint_fore_l");
        float bob = Mth.sin(t * 0.07F) * 1.2F;
        this.forms[4].y += -4.0F + bob;
        head.yRot += Mth.clamp(s.yRot, -50, 50) * Mth.DEG_TO_RAD;
        head.xRot += s.xRot * Mth.DEG_TO_RAD * 0.5F;
        g("quint_cape").xRot += 0.06F + Mth.sin(t * 0.08F) * 0.05F;
        skirt.zRot += Mth.sin(t * 0.05F) * 0.02F;
        g("quint_skirt2").xRot += Mth.sin(t * 0.06F) * 0.03F;
        // halo ∞ e as quatro ferramentas girando
        ModelPart halo = g("quint_halo");
        halo.yRot += t * 0.03F;
        halo.y += Mth.sin(t * 0.1F) * 0.6F;
        ModelPart orbit = g("quint_orbit");
        orbit.yRot += t * 0.025F;
        orbit.y += Mth.sin(t * 0.05F) * 2.0F;
        String[] tools = {"quint_tool_pentacle", "quint_tool_sword", "quint_tool_cup", "quint_tool_wand"};
        for (int i = 0; i < 4; i++) {
            ModelPart tool = g(tools[i]);
            tool.yRot += -t * 0.025F + t * 0.04F;
            tool.y += Mth.sin(t * 0.09F + i * 1.6F) * 1.5F;
        }
        // pose da carta: braço direito para o céu com a varinha, esquerdo apontando para a terra
        ar.xRot += -2.75F + Mth.sin(t * 0.06F) * 0.05F;
        ar.zRot += -0.45F;
        al.xRot += -0.35F;
        al.zRot += -0.3F + Mth.sin(t * 0.06F) * 0.04F;
        switch (s.action) {
            case MagusEntity.CAST -> {
                float k = ease(a, 0, 6) - ease(a, Math.max(10, a > 60 ? 62 : 14), Math.max(16, a > 60 ? 70 : 20));
                ar.xRot += 1.3F * k;
                al.xRot += -1.1F * k;
                al.zRot += 0.3F * k;
            }
            case MagusEntity.RAY -> {
                float k = ease(a, 0, 10) - ease(a, 120, 130);
                ar.xRot += 1.6F * k;
                body.yRot += Mth.sin(a * 0.05F) * 0.4F * k;
            }
            case MagusEntity.SWING -> {
                float up = ease(a, 0, 8), down = ease(a, 8, 12);
                ar.xRot += 0.3F * up + 1.6F * down;
                ar.yRot += -0.8F * down;
                body.yRot += 0.3F * up - 0.6F * down;
            }
            case MagusEntity.INVERT -> {
                float k = ease(a, 0, 12);
                ar.xRot += 2.75F * k;
                ar.zRot += 1.85F * k;
                al.xRot += 0.35F * k;
                al.zRot += -1.2F * k;
                body.yRot += a * 0.08F;
            }
            case MagusEntity.ROAR -> {
                float k = ease(a, 0, 15) - ease(a, 40, 50);
                al.xRot += -2.4F * k;
                al.zRot += 0.3F * k;
                head.xRot += -0.3F * k;
            }
            default -> {}
        }
        fr.xRot += -0.15F;
        fl.xRot += -0.1F;
    }
}
