package com.mistech.mistechmod.spellcraft.fx;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.SpellFx;
import com.mojang.math.Transformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Efeitos 3D das magias com display entities do vanilla (blocos e itens que giram, crescem e se movem com interpolação
 * suave no cliente). Os campos do Display são privados, então tudo é ajustado como o comando /data faz: salvar em NBT,
 * mudar e carregar de volta. As exibições são temporárias: somem sozinhas e, se sobrarem num salvamento, são apagadas
 * ao carregar.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class Vfx {
    public static final String TAG = "mistech_vfx";
    private static final Set<UUID> LIVE = new HashSet<>();

    private Vfx() {}

    // ------------------------------------------------------------------ transformações

    public static Quaternionf rotX(float deg) {
        return new Quaternionf().rotateX((float) Math.toRadians(deg));
    }

    public static Quaternionf rotY(float deg) {
        return new Quaternionf().rotateY((float) Math.toRadians(deg));
    }

    public static Quaternionf rotZ(float deg) {
        return new Quaternionf().rotateZ((float) Math.toRadians(deg));
    }

    /** Rotação que leva o eixo +Y até a direção dada. */
    public static Quaternionf alignY(Vec3 dir) {
        Vector3f d = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d);
    }

    public static Transformation tf(Vector3f translation, Quaternionf left, Vector3f scale, Quaternionf right) {
        return new Transformation(translation, left, scale, right);
    }

    /** Bloco centrado no ponto da entidade, girado pelo centro. */
    public static Transformation centered(float sx, float sy, float sz, Quaternionf rot) {
        Vector3f half = new Vector3f(-sx / 2, -sy / 2, -sz / 2);
        rot.transform(half);
        return new Transformation(half, rot, new Vector3f(sx, sy, sz), new Quaternionf());
    }

    /** Bloco que cresce do chão (base no ponto), com seção centrada e inclinação {@code rot}. */
    public static Transformation standing(float w, float h, float d, Quaternionf rot) {
        Vector3f off = new Vector3f(-w / 2, 0, -d / 2);
        rot.transform(off);
        return new Transformation(off, rot, new Vector3f(w, h, d), new Quaternionf());
    }

    /** Item deitado no chão (círculos, ondas): {@code size} de diâmetro, girado {@code spin} graus no próprio plano. */
    public static Transformation flat(float size, float spin, float lift) {
        return new Transformation(new Vector3f(0, lift, 0), rotX(-90), new Vector3f(size, size, size), rotZ(spin));
    }

    // ------------------------------------------------------------------ criar, animar, remover

    private static CompoundTag save(Display display, ServerLevel level) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        display.saveWithoutId(out);
        return out.buildResult();
    }

    private static void load(Display display, ServerLevel level, CompoundTag tag) {
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    private static <T extends Display> T spawn(ServerLevel level, EntityType<T> type, Vec3 pos, CompoundTag extra, int life) {
        T display = type.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) {
            return null;
        }
        display.setPos(pos.x, pos.y, pos.z);
        CompoundTag tag = save(display, level);
        tag.merge(extra);
        load(display, level, tag);
        display.addTag(TAG);
        LIVE.add(display.getUUID());
        level.addFreshEntity(display);
        if (life > 0) {
            remove(level, display, life);
        }
        return display;
    }

    private static CompoundTag base(Transformation t, boolean glow) {
        CompoundTag tag = new CompoundTag();
        tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, t).getOrThrow());
        CompoundTag light = new CompoundTag();
        light.putInt("sky", 15);
        light.putInt("block", 15);
        if (glow) {
            tag.put("brightness", light);
        }
        tag.putFloat("view_range", 1.5F);
        tag.putInt("teleport_duration", 2);
        return tag;
    }

    /** Bloco 3D (com {@code glow}, sempre aceso). */
    public static Display.BlockDisplay block(ServerLevel level, Vec3 pos, BlockState state, Transformation t, int life, boolean glow) {
        CompoundTag tag = base(t, glow);
        tag.put(Display.BlockDisplay.TAG_BLOCK_STATE, NbtUtils.writeBlockState(state));
        return spawn(level, EntityType.BLOCK_DISPLAY, pos, tag, life);
    }

    /** Item de efeito tingido com a cor do elemento; {@code billboard} = sempre de frente para a câmera. */
    public static Display.ItemDisplay item(ServerLevel level, Vec3 pos, ItemStack stack, Transformation t, int life, boolean billboard) {
        CompoundTag tag = base(t, true);
        tag.put("item", ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
        tag.putString("item_display", "fixed");
        if (billboard) {
            tag.putString("billboard", "center");
        }
        return spawn(level, EntityType.ITEM_DISPLAY, pos, tag, life);
    }

    public static ItemStack tinted(net.minecraft.world.item.Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(rgb)));
        return stack;
    }

    /** Anima até a transformação dada, em {@code duration} ticks, depois de {@code delay} ticks. */
    public static void animate(ServerLevel level, Display display, int delay, int duration, Transformation to) {
        if (display == null) {
            return;
        }
        SpellFx.later(level, delay, () -> {
            if (!display.isAlive()) {
                return;
            }
            CompoundTag tag = save(display, level);
            tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, to).getOrThrow());
            tag.putInt("interpolation_duration", Math.max(0, duration));
            tag.putInt("start_interpolation", 0);
            load(display, level, tag);
        });
    }

    /** Move a exibição (o cliente desliza suavemente em 2 ticks). */
    public static void move(Display display, Vec3 pos) {
        if (display != null && display.isAlive()) {
            display.teleportTo(pos.x, pos.y, pos.z);
        }
    }

    public static void remove(ServerLevel level, Display display, int delay) {
        if (display == null) {
            return;
        }
        SpellFx.later(level, delay, () -> {
            LIVE.remove(display.getUUID());
            display.discard();
        });
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().entityTags().contains(TAG) && !LIVE.contains(event.getEntity().getUUID())) {
            event.setCanceled(true); // sobrou de um salvamento: some
        }
    }

    // ------------------------------------------------------------------ materiais por elemento

    /** Bloco que "é" o elemento, para pedras, espinhos, colunas e raios. */
    public static BlockState material(RuneElement element) {
        return switch (element) {
            case FIRE -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case ICE -> Blocks.PACKED_ICE.defaultBlockState();
            case WATER -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case EARTH -> Blocks.COARSE_DIRT.defaultBlockState();
            case AIR, VAPOR -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
            case ENERGY -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case LIGHT -> Blocks.GLOWSTONE.defaultBlockState();
            case DARKNESS, VOID -> Blocks.BLACK_STAINED_GLASS.defaultBlockState();
            case TIME -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case SPACE -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            case LIFE -> Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
            case MAGMA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case CLOUD -> Blocks.WHITE_WOOL.defaultBlockState();
            case SOUL -> Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            case VIGIL -> Blocks.BONE_BLOCK.defaultBlockState();
            case ECHO -> Blocks.SCULK.defaultBlockState();
            case LUCK -> Blocks.EMERALD_BLOCK.defaultBlockState();
            case ARCANE -> Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
        };
    }

    /** Bloco brilhante de "luz pura" do elemento (núcleos, raios finos). */
    public static BlockState glowMaterial(RuneElement element) {
        return switch (element) {
            case FIRE, MAGMA -> Blocks.SHROOMLIGHT.defaultBlockState();
            case ICE, WATER, AIR, VAPOR, CLOUD -> Blocks.SEA_LANTERN.defaultBlockState();
            case LIGHT, ENERGY, LUCK -> Blocks.GLOWSTONE.defaultBlockState();
            case DARKNESS, VOID, ECHO -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case SOUL -> Blocks.SOUL_LANTERN.defaultBlockState();
            case LIFE -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            case TIME, SPACE, ARCANE -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
            default -> Blocks.SEA_LANTERN.defaultBlockState();
        };
    }

    // ------------------------------------------------------------------ peças prontas

    /** Círculo mágico deitado que aparece girando, gira enquanto dura e some encolhendo. */
    public static void circle(ServerLevel level, Vec3 center, RuneElement element, double radius, int life) {
        float size = (float) (radius * 2.0);
        Display d = item(level, center.add(0, 0.06, 0), tinted(VfxContent.VFX_CIRCLE.get(), element.color()), flat(0.01F, 0, 0), life + 2, false);
        animate(level, d, 1, 5, flat(size, 60, 0));
        int steps = Math.max(1, (life - 6) / 20);
        for (int i = 0; i < steps; i++) {
            animate(level, d, 6 + i * 20, 20, flat(size, 60 + 90 * (i + 1), 0));
        }
        animate(level, d, Math.max(6, life - 5), 5, flat(0.01F, 60 + 90 * (steps + 1), 0));
    }

    /** Relógio mágico (Tempo): igual ao círculo, mas com ponteiros. */
    public static void clock(ServerLevel level, Vec3 center, RuneElement element, double radius, int life, float lift) {
        float size = (float) (radius * 2.0);
        Display d = item(level, center.add(0, lift, 0), tinted(VfxContent.VFX_CLOCK.get(), element.color()), flat(0.01F, 0, 0), life + 2, false);
        animate(level, d, 1, 5, flat(size, 0, 0));
        int steps = Math.max(1, (life - 6) / 10);
        for (int i = 0; i < steps; i++) {
            animate(level, d, 6 + i * 10, 10, flat(size, -45 * (i + 1), 0));
        }
        animate(level, d, Math.max(6, life - 5), 5, flat(0.01F, -45 * (steps + 1), 0));
    }

    /** Onda de choque: anel deitado que abre de {@code from} até {@code to} de raio. */
    public static void shockwave(ServerLevel level, Vec3 center, RuneElement element, double to, int ticks) {
        Display d = item(level, center.add(0, 0.12, 0), tinted(VfxContent.VFX_RING.get(), element.color()), flat(0.5F, 0, 0), ticks + 1, false);
        animate(level, d, 1, ticks - 1, flat((float) (to * 2.0), 40, 0));
        level.sendParticles(VfxContent.wave(element.color()), center.x, center.y + 0.1, center.z, 0, to, 0, 0, 1.0);
    }

    /** Anel que se fecha (sucção). */
    public static void implode(ServerLevel level, Vec3 center, RuneElement element, double from, int ticks) {
        Display d = item(level, center.add(0, 0.15, 0), tinted(VfxContent.VFX_RING.get(), element.color()), flat((float) (from * 2.0), 0, 0), ticks + 1, false);
        animate(level, d, 1, ticks - 1, flat(0.1F, -60, 0));
    }

    /** Anel que sobe ao redor de algo. */
    public static void risingRing(ServerLevel level, Vec3 base, RuneElement element, double radius, double height, int ticks) {
        Display d = item(level, base.add(0, 0.1, 0), tinted(VfxContent.VFX_RING.get(), element.color()), flat((float) (radius * 2), 0, 0), ticks + 1, false);
        animate(level, d, 1, ticks - 1, new Transformation(new Vector3f(0, (float) height, 0), rotX(-90),
                new Vector3f((float) (radius * 1.3), (float) (radius * 1.3), (float) (radius * 1.3)), rotZ(120)));
    }

    /** Anel de vento de frente para a direção, que voa para a frente abrindo. */
    public static void windRing(ServerLevel level, Vec3 origin, Vec3 look, RuneElement element, double distance, int ticks) {
        Quaternionf face = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f((float) look.x, (float) look.y, (float) look.z).normalize());
        Display d = item(level, origin, tinted(VfxContent.VFX_RING.get(), element.color()),
                new Transformation(new Vector3f(), face, new Vector3f(0.6F, 0.6F, 0.6F), new Quaternionf()), ticks + 1, false);
        Vector3f end = new Vector3f((float) (look.x * distance), (float) (look.y * distance), (float) (look.z * distance));
        animate(level, d, 1, ticks - 1, new Transformation(end, face, new Vector3f(3.0F, 3.0F, 3.0F), rotZ(90)));
    }

    /** Clarão esférico no ponto: uma esfera de luz que estoura e some. */
    public static void flare(ServerLevel level, Vec3 pos, RuneElement element, float size, int ticks) {
        Display d = item(level, pos, tinted(VfxContent.VFX_ORB.get(), element.color()), centeredItem(size * 0.4F), ticks + 1, true);
        animate(level, d, 1, Math.max(1, ticks / 3), centeredItem(size));
        animate(level, d, 1 + ticks / 3, ticks - ticks / 3, centeredItem(0.01F));
    }

    public static Transformation centeredItem(float size) {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(size, size, size), new Quaternionf());
    }

    /** Espinhos (gelo, pedra, cristal) que brotam do chão em volta do centro e afundam depois. */
    public static void spikes(ServerLevel level, Vec3 center, BlockState state, int count, double radius, double height, int life) {
        for (int i = 0; i < count; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(level.getRandom().nextDouble());
            Vec3 p = ground(level, center.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            float h = (float) (height * (0.5 + level.getRandom().nextDouble() * 0.6));
            float w = 0.25F + level.getRandom().nextFloat() * 0.25F;
            Quaternionf tilt = new Quaternionf().rotateY((float) a).rotateX((float) (Math.toRadians(8 + level.getRandom().nextInt(22))));
            int delay = level.getRandom().nextInt(4);
            Display d = block(level, p.subtract(0, 0.3, 0), state, standing(w, 0.01F, w, tilt), life + delay + 6, false);
            animate(level, d, 1 + delay, 3, standing(w, h, w, tilt));
            animate(level, d, life + delay, 5, standing(w, 0.01F, w, tilt));
        }
    }

    /** Coluna de bloco que sobe do chão e desce de novo. */
    public static Display pillar(ServerLevel level, Vec3 pos, BlockState state, float width, float height, int life) {
        Vec3 p = ground(level, pos);
        Display d = block(level, p.subtract(0, 0.2, 0), state, standing(width, 0.01F, width, new Quaternionf()), life + 6, false);
        animate(level, d, 1, 3, standing(width, height, width, new Quaternionf()));
        animate(level, d, life, 5, standing(width, 0.01F, width, new Quaternionf()));
        return d;
    }

    /** Raio reto de luz entre dois pontos (bloco fino esticado e aceso). */
    public static void beam(ServerLevel level, Vec3 from, Vec3 to, BlockState state, float width, int life) {
        Vec3 delta = to.subtract(from);
        float len = (float) delta.length();
        if (len < 0.05F) {
            return;
        }
        Quaternionf q = alignY(delta);
        Display d = block(level, from, state, standing(width * 0.2F, len, width * 0.2F, q), life + 2, true);
        animate(level, d, 1, 2, standing(width, len, width, q));
        animate(level, d, Math.max(3, life - 3), 3, standing(0.01F, len, 0.01F, q));
    }

    /** Ponto do chão logo abaixo (ou acima, se estiver enterrado) da posição. */
    public static Vec3 ground(ServerLevel level, Vec3 p) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(p));
        if (Math.abs(top.getY() - p.y) <= 4) {
            return new Vec3(p.x, top.getY(), p.z);
        }
        BlockPos b = BlockPos.containing(p);
        for (int dy = 0; dy < 6; dy++) {
            if (!level.getBlockState(b.below(dy + 1)).isAir()) {
                return new Vec3(p.x, b.getY() - dy, p.z);
            }
        }
        return p;
    }

    // ------------------------------------------------------------------ tela

    /** Tremor de câmera para quem está perto (mais forte quanto mais perto). */
    public static void shake(ServerLevel level, Vec3 center, double radius, float strength, int ticks) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(center);
            if (d <= radius) {
                float s = (float) (strength * (1.0 - d / radius * 0.7));
                PacketDistributor.sendToPlayer(p, new ScreenFxPayload(s, ticks, 0, 0));
            }
        }
    }

    /** Clarão colorido na tela de quem está perto. */
    public static void flash(ServerLevel level, Vec3 center, double radius, int rgb, int ticks) {
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(center) <= radius) {
                PacketDistributor.sendToPlayer(p, new ScreenFxPayload(0, 0, rgb, ticks));
            }
        }
    }
}
