package com.mistech.mistechmod.spellcraft.client.fx;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.fx.VfxContent;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Partículas próprias das magias: todas brilham no escuro e levam a cor do elemento. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class VfxParticles {
    private VfxParticles() {}

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Base: cor do elemento, brilho total, some aos poucos. */
    abstract static class Glow extends SingleQuadParticle {
        protected final float baseSize;

        Glow(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite, ColorParticleOption color) {
            super(level, x, y, z, xd, yd, zd, sprite);
            this.setColor(color.getRed(), color.getGreen(), color.getBlue());
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.baseSize = this.quadSize;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }

        protected float life(float partialTick) {
            return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        }
    }

    static final class Mote extends Glow {
        Mote(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, xd, yd, zd, s, c);
            this.lifetime = 12 + this.random.nextInt(10);
            this.quadSize = 0.12F + this.random.nextFloat() * 0.08F;
            this.friction = 0.9F;
            this.gravity = 0.0F;
            this.hasPhysics = false;
        }

        @Override
        public float getQuadSize(float partialTick) {
            return this.quadSize * (1.0F - life(partialTick) * 0.8F);
        }

        @Override
        public void tick() {
            super.tick();
            this.setAlpha(1.0F - life(0) * 0.6F);
        }
    }

    static final class Glyph extends Glow {
        Glyph(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, xd, yd, zd, s, c);
            this.lifetime = 24 + this.random.nextInt(16);
            this.quadSize = 0.16F + this.random.nextFloat() * 0.06F;
            this.friction = 0.92F;
            this.gravity = -0.005F;
            this.hasPhysics = false;
            this.roll = this.random.nextFloat() * 0.6F - 0.3F;
            this.oRoll = this.roll;
        }

        @Override
        public void tick() {
            super.tick();
            float t = life(0);
            this.setAlpha(t < 0.15F ? t / 0.15F : 1.0F - Math.max(0.0F, (t - 0.6F) / 0.4F));
        }
    }

    static final class Spark extends Glow {
        Spark(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, xd, yd, zd, s, c);
            this.lifetime = 8 + this.random.nextInt(8);
            this.quadSize = 0.1F + this.random.nextFloat() * 0.08F;
            this.friction = 0.82F;
            this.gravity = 0.06F;
            this.hasPhysics = true;
            this.roll = this.random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;
        }

        @Override
        public float getQuadSize(float partialTick) {
            return this.quadSize * (1.0F - life(partialTick));
        }
    }

    static final class Ember extends Glow {
        Ember(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, xd, yd, zd, s, c);
            this.lifetime = 20 + this.random.nextInt(20);
            this.quadSize = 0.07F + this.random.nextFloat() * 0.06F;
            this.friction = 0.94F;
            this.gravity = -0.012F;
            this.hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd += (this.random.nextFloat() - 0.5F) * 0.01F;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.01F;
            this.setAlpha((1.0F - life(0)) * (0.6F + 0.4F * this.random.nextFloat()));
        }
    }

    static final class Shard extends Glow {
        private final float spin;

        Shard(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, xd, yd, zd, s, c);
            this.lifetime = 24 + this.random.nextInt(16);
            this.quadSize = 0.14F + this.random.nextFloat() * 0.1F;
            this.friction = 0.96F;
            this.gravity = 1.0F;
            this.hasPhysics = true;
            this.spin = (this.random.nextFloat() - 0.5F) * 0.6F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            if (!this.onGround) {
                this.roll += this.spin;
            }
            float t = life(0);
            this.setAlpha(t > 0.7F ? 1.0F - (t - 0.7F) / 0.3F : 1.0F);
        }
    }

    /** Onda de choque deitada: o raio final vem na velocidade x (contagem 0 no sendParticles). */
    static final class Wave extends Glow {
        private final float radius;

        Wave(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c) {
            super(l, x, y, z, 0, 0, 0, s, c);
            this.radius = (float) Math.max(0.5, xd);
            this.lifetime = 10 + (int) Math.min(10, this.radius);
            this.xd = this.yd = this.zd = 0;
            this.hasPhysics = false;
            this.gravity = 0.0F;
        }

        @Override
        public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
            return (rotation, camera, partialTick) -> rotation.identity().rotateX(-Mth.HALF_PI);
        }

        @Override
        public float getQuadSize(float partialTick) {
            float t = life(partialTick);
            return this.radius * (1.0F - (1.0F - t) * (1.0F - t));
        }

        @Override
        public void tick() {
            super.tick();
            this.setAlpha(1.0F - life(0));
        }
    }

    interface Factory {
        Particle make(ClientLevel l, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite s, ColorParticleOption c);
    }

    private static ParticleProvider<ColorParticleOption> provider(SpriteSet sprites, Factory factory) {
        return (ColorParticleOption options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) ->
                factory.make(level, x, y, z, xd, yd, zd, sprites.get(random), options);
    }

    @SubscribeEvent
    static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VfxContent.MOTE.get(), sprites -> provider(sprites, Mote::new));
        event.registerSpriteSet(VfxContent.GLYPH.get(), sprites -> provider(sprites, Glyph::new));
        event.registerSpriteSet(VfxContent.SPARK.get(), sprites -> provider(sprites, Spark::new));
        event.registerSpriteSet(VfxContent.EMBER.get(), sprites -> provider(sprites, Ember::new));
        event.registerSpriteSet(VfxContent.SHARD.get(), sprites -> provider(sprites, Shard::new));
        event.registerSpriteSet(VfxContent.WAVE.get(), sprites -> provider(sprites, Wave::new));
    }
}
