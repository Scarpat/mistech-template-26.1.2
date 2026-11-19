package com.mistech.mistechmod.spellcraft.client.fx;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.fx.VfxContent;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Desenhos dos efeitos oníricos. Os índices seguem a ordem de {@code assets/mistech/particles/dream.json}, gerado por
 * {@code tools/spellcraft/gen_dream_vfx.py}: mudou lá, mude aqui.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class DreamSprites {
    public static final int MOTH = 0;      // 2 quadros (asas abertas, meio fechadas)
    public static final int ASH = 2;
    public static final int EYE = 3;       // 5 quadros (fechado -> arregalado)
    public static final int DRIP = 8;
    public static final int SLIT = 9;      // 5 quadros (fio -> pálpebra aberta com céu dentro)
    public static final int STAR = 14;
    public static final int NUMERAL = 15;  // 12 algarismos romanos (I..XII)
    public static final int GRAIN = 27;
    public static final int FLOWER = 28;   // 5 quadros (broto -> aberta)
    public static final int PETAL = 33;
    public static final int WHEEL = 34;    // 2 quadros (olhos abertos, fechados)
    public static final int FEATHER = 36;
    public static final int GLOW = 37;
    public static final int BIGMOTH = 38;  // 2 quadros
    public static final int DANCER = 40;   // 2 quadros (gente de fogo dançando)
    public static final int SUN = 42;      // 2 quadros (olho fechado, aberto)
    public static final int EGG = 44;      // 3 quadros (inteiro, rachado, aberto)
    public static final int HAND = 47;     // 2 quadros (aberta, fechando)
    public static final int MOUTH = 49;    // 2 quadros (aberta, fechada)
    public static final int PUDDLE = 51;
    public static final int BIGEYE = 52;   // 5 quadros (fechado -> arregalado)
    public static final int SHARD = 57;    // 2 (caco com olho refletido, caco liso)
    public static final int MASK = 59;     // 2 (dormindo, acordada)
    public static final int FROST = 61;
    public static final int BIGMASK = 62;  // 2
    public static final int FISH = 64;     // 2 (nadando; olha para a direita)
    public static final int BUBBLE = 66;
    public static final int RIPPLE = 67;
    public static final int JELLY = 68;    // 2 (aberta, fechada)
    public static final int WHALE = 70;    // 2 (olha para a direita)
    public static final int DROP = 72;
    public static final int TOOTH = 73;
    public static final int HEAD = 74;     // 2 (dormindo, acordada)
    public static final int PEBBLE = 76;   // 2
    public static final int CRACK = 78;    // começa na borda esquerda do desenho
    public static final int CRANE = 79;    // 2 (asas para cima, para baixo; olha para a direita)
    public static final int SWIRL = 81;
    public static final int PAPER = 82;
    public static final int NEEDLE = 83;   // ponta para a direita
    public static final int THREAD = 84;   // linha horizontal, emenda de ponta a ponta
    public static final int KNOT = 85;
    public static final int HOURGLASS = 86;
    public static final int WATCH = 87;    // 3 (ponteiros em horas diferentes)
    public static final int KEY = 90;
    public static final int VINE = 91;     // cipó horizontal, emenda de ponta a ponta
    public static final int LEAF = 92;
    public static final int SHROOM = 93;   // 3 (crescendo)
    public static final int BRAMBLE = 96;
    public static final int POD = 97;      // 2 (fechada, aberta)
    public static final int TREE = 99;
    public static final int PUFF = 100;    // 2 (fantasma de vapor; redondo, achatado)
    public static final int SHEEP = 102;   // 2 (em pé, pulando; olha para a direita)
    public static final int CANDLE = 104;  // 2 (chama tremendo)
    public static final int SOUL = 106;
    public static final int GEAR = 107;
    public static final int OWL = 108;     // 2 (olhos fechados, arregalados)
    public static final int COIN = 110;    // 3 (cara, meio de lado, de lado)
    public static final int COUNT = 113;

    private static @Nullable SpriteSet sprites;

    private DreamSprites() {}

    static boolean ready() {
        return sprites != null;
    }

    static TextureAtlasSprite get(int index) {
        return sprites.get(Math.max(0, Math.min(COUNT - 1, index)), COUNT - 1);
    }

    @SubscribeEvent
    static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VfxContent.DREAM.get(), set -> {
            sprites = set;
            // /particle mistech:dream: só um brilho, para a partícula registrada não ficar sem fábrica
            return (options, level, x, y, z, xd, yd, zd, random) -> new DreamParticle(level, new Vec3(x, y, z), GLOW, 12, DreamParticle.BILLBOARD,
                    (p, age) -> p.size(0.15F).alpha(1.0F - age / 12.0F));
        });
    }
}
