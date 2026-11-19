package com.mistech.mistechmod.spellcraft.client.fx;

import org.joml.Quaternionf;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Peça dos efeitos oníricos: um desenho (sprite {@code dream_*}) sem física, inteiramente dirigido por um roteiro
 * chamado a cada tick ({@link Script}), que decide posição, tamanho, transparência, quadro e orientação. Entre um
 * tick e outro tudo é interpolado, então o movimento sai liso. Pode ficar de frente para a câmera, em pé (gira só no
 * eixo vertical) ou com orientação fixa no mundo (desenhado dos dois lados).
 */
public final class DreamParticle extends SingleQuadParticle {
    @FunctionalInterface
    public interface Script {
        void run(DreamParticle p, int age);
    }

    public static final int BILLBOARD = 0;
    public static final int UPRIGHT = 1;
    public static final int FIXED = 2;
    /** Esticado ao longo de um eixo do mundo ({@link #along}) e girando em volta dele para encarar a câmera: fios, agulhas, raios. */
    public static final int AXIS = 3;
    private static final int FULL_BRIGHT = 0xF000F0;

    private final Script script;
    private final int facing;
    private final Quaternionf orient = new Quaternionf();
    private final Quaternionf oOrient = new Quaternionf();
    private final org.joml.Vector3f axis = new org.joml.Vector3f(1, 0, 0);
    private float size;
    private float oSize;
    private float oAlpha;
    private int frame;
    /** Números aleatórios fixos da peça (0 a 1), para variar o roteiro. */
    public final float r1;
    public final float r2;
    public final float r3;

    DreamParticle(ClientLevel level, Vec3 pos, int frame, int lifetime, int facing, Script script) {
        super(level, pos.x, pos.y, pos.z, DreamSprites.get(frame));
        this.frame = frame;
        this.lifetime = lifetime;
        this.facing = facing;
        this.script = script;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.r1 = this.random.nextFloat();
        this.r2 = this.random.nextFloat();
        this.r3 = this.random.nextFloat();
        this.size = 0.2F;
        script.run(this, 0);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oSize = this.size;
        this.oAlpha = this.alpha;
        this.oRoll = this.roll;
        this.oOrient.set(this.orient);
    }

    // ------------------------------------------------------------------ o que o roteiro pode mexer

    public DreamParticle at(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public DreamParticle at(Vec3 p) {
        return at(p.x, p.y, p.z);
    }

    public Vec3 pos() {
        return new Vec3(this.x, this.y, this.z);
    }

    /** Meio lado do desenho, em blocos (0,5 = um bloco inteiro de largura). */
    public DreamParticle size(float half) {
        this.size = half;
        return this;
    }

    public DreamParticle alpha(float a) {
        this.alpha = Mth.clamp(a, 0.0F, 1.0F);
        return this;
    }

    public DreamParticle frame(int index) {
        if (index != this.frame) {
            this.frame = index;
            this.setSprite(DreamSprites.get(index));
        }
        return this;
    }

    public DreamParticle roll(float radians) {
        this.roll = radians;
        return this;
    }

    public DreamParticle tint(int rgb) {
        this.setColor((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F);
        return this;
    }

    /** Orientação no mundo (só no modo {@link #FIXED}): a frente do desenho é o eixo +Z local. */
    public DreamParticle orient(Quaternionf q) {
        this.orient.set(q);
        return this;
    }

    /** Vira a frente do desenho para a direção dada. */
    public DreamParticle face(Vec3 dir) {
        double len = dir.length();
        if (len > 1.0E-4) {
            float yaw = (float) Math.atan2(dir.x, dir.z);
            float pitch = (float) -Math.asin(Mth.clamp(dir.y / len, -1.0, 1.0));
            this.orient.rotationY(yaw).rotateX(pitch);
        }
        return this;
    }

    /** Modo {@link #AXIS}: a direita do desenho aponta para {@code dir}. */
    public DreamParticle along(Vec3 dir) {
        if (dir.lengthSqr() > 1.0E-8) {
            this.axis.set((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        }
        return this;
    }

    public void kill() {
        this.remove();
    }

    public int lifetime() {
        return this.lifetime;
    }

    // ------------------------------------------------------------------ partícula

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oSize = this.size;
        this.oAlpha = this.alpha;
        this.oRoll = this.roll;
        this.oOrient.set(this.orient);
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.script.run(this, this.age);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        float a = Mth.lerp(partialTick, this.oAlpha, this.alpha);
        float s = Mth.lerp(partialTick, this.oSize, this.size);
        if (a <= 0.004F || s <= 0.0005F) {
            return;
        }
        Vec3 cam = camera.position();
        float px = (float) (Mth.lerp((double) partialTick, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp((double) partialTick, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp((double) partialTick, this.zo, this.z) - cam.z());
        Quaternionf q = new Quaternionf();
        if (this.facing == AXIS) {
            // x = eixo; z = direção até a câmera, tirando a parte ao longo do eixo; y = z × x
            org.joml.Vector3f x = this.axis;
            org.joml.Vector3f z = new org.joml.Vector3f(-px, -py, -pz);
            z.sub(new org.joml.Vector3f(x).mul(z.dot(x)));
            if (z.lengthSquared() < 1.0E-6F) {
                z.set(x.y, -x.x, 0.0F);
                if (z.lengthSquared() < 1.0E-6F) {
                    z.set(0.0F, 0.0F, 1.0F);
                }
            }
            z.normalize();
            org.joml.Vector3f y = new org.joml.Vector3f(z).cross(x);
            q.setFromNormalized(new org.joml.Matrix3f(x, y, z));
        } else if (this.facing == FIXED) {
            this.oOrient.slerp(this.orient, partialTick, q);
        } else if (this.facing == UPRIGHT) {
            SingleQuadParticle.FacingCameraMode.LOOKAT_Y.setRotation(q, camera, partialTick);
        } else {
            SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(q, camera, partialTick);
        }
        float r = Mth.lerp(partialTick, this.oRoll, this.roll);
        if (r != 0.0F) {
            q.rotateZ(r);
        }
        int color = ARGB.colorFromFloat(a, this.rCol, this.gCol, this.bCol);
        state.add(this.getLayer(), px, py, pz, q.x, q.y, q.z, q.w, s, this.getU0(), this.getU1(), this.getV0(), this.getV1(), color, FULL_BRIGHT);
        if (this.facing == FIXED) {
            // o verso: mesma peça virada, com a textura espelhada para o desenho não inverter
            q.rotateY(Mth.PI);
            state.add(this.getLayer(), px, py, pz, q.x, q.y, q.z, q.w, s, this.getU1(), this.getU0(), this.getV0(), this.getV1(), color, FULL_BRIGHT);
        }
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return FULL_BRIGHT;
    }
}
