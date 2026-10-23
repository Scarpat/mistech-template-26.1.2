package com.mistech.mistechmod.spellcraft;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.Random;
import java.util.function.Supplier;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Santuário de Elemento: plataforma circular (raio 6) no material do elemento, quatro colunas, altar no centro com o baú
 * (loot {@code chests/shrine_<elemento>}) e um gerador de criaturas do elemento aos pés do altar — o "guardião".
 */
public final class ElementShrinePlan {
    private record Theme(Block base, Block accent, Block light, Supplier<EntityType<?>> guardian) {}

    private static Theme theme(RuneElement element) {
        return switch (element) {
            case FIRE -> new Theme(Blocks.NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.SHROOMLIGHT, () -> EntityType.BLAZE);
            case ICE -> new Theme(Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SEA_LANTERN, () -> EntityType.STRAY);
            case WATER -> new Theme(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN, () -> EntityType.DROWNED);
            case EARTH -> new Theme(Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT, Blocks.GLOWSTONE, () -> EntityType.SILVERFISH);
            case AIR -> new Theme(Blocks.SMOOTH_QUARTZ, Blocks.WHITE_CONCRETE, Blocks.SEA_LANTERN, () -> EntityType.VEX);
            case ENERGY -> new Theme(Blocks.WAXED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.GLOWSTONE, () -> EntityType.CREEPER);
            case LIGHT -> new Theme(Blocks.QUARTZ_BRICKS, Blocks.GLOWSTONE, Blocks.GLOWSTONE, () -> EntityType.SKELETON);
            case DARKNESS -> new Theme(Blocks.DEEPSLATE_BRICKS, Blocks.BLACKSTONE, Blocks.SOUL_LANTERN, () -> EntityType.CAVE_SPIDER);
            case TIME -> new Theme(Blocks.END_STONE_BRICKS, Blocks.AMETHYST_BLOCK, Blocks.END_ROD, () -> EntityType.ENDERMITE);
            default -> new Theme(Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.END_ROD, () -> EntityType.ENDERMAN); // espaço
        };
    }

    public static final RuneElement[] ELEMENTS = {RuneElement.FIRE, RuneElement.ICE, RuneElement.WATER, RuneElement.EARTH, RuneElement.AIR,
            RuneElement.ENERGY, RuneElement.LIGHT, RuneElement.DARKNESS, RuneElement.TIME, RuneElement.SPACE};

    private final Blueprint bp = new Blueprint();
    private final int cx, f, cz;

    public ElementShrinePlan(RuneElement element, long seed, int cx, int y0, int cz) {
        this.cx = cx;
        this.f = y0;
        this.cz = cz;
        Random rnd = new Random(seed);
        Theme t = theme(element);
        Palette base = Palette.mix(80, st(t.base()), 70, st(t.accent()), 30);
        com.mistech.mistechmod.lore.LairTerrain.flatten(this.bp, cx, cz, this.f, 7, 5, base);
        // plataforma com anel de destaque
        this.bp.shape(cx - 7, this.f, cz - 7, cx + 7, this.f, cz + 7, (x, y, z) -> Math.hypot(x - cx, z - cz) <= 6.5, base);
        this.bp.shape(cx - 7, this.f, cz - 7, cx + 7, this.f, cz + 7, (x, y, z) -> {
            double d = Math.hypot(x - cx, z - cz);
            return d > 3.5 && d <= 4.5;
        }, Palette.of(st(t.accent())));
        this.bp.air(cx - 7, this.f + 1, cz - 7, cx + 7, this.f + 8, cz + 7);
        // quatro colunas com luz no topo
        int[][] corners = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] c : corners) {
            for (int y = 1; y <= 4; y++) {
                this.bp.block(cx + c[0], this.f + y, cz + c[1], st(t.base()));
            }
            this.bp.block(cx + c[0], this.f + 5, cz + c[1], st(t.light()));
        }
        // altar de 3x3 (um degrau) com o baú
        this.bp.fill(cx - 1, this.f + 1, cz - 1, cx + 1, this.f + 1, cz + 1, Palette.of(st(t.base())));
        this.bp.block(cx, this.f + 2, cz, st(t.light()));
        this.bp.chest(cx, this.f + 2, cz - 1, Direction.SOUTH, Blueprint.loot("chests/shrine_" + element.id()));
        // guardião: gerador aos pés do altar
        @SuppressWarnings("unchecked")
        java.util.function.Supplier<EntityType<net.minecraft.world.entity.Mob>> guardian =
                () -> (EntityType<net.minecraft.world.entity.Mob>) (EntityType<?>) t.guardian().get();
        this.bp.mob(cx + 0.5, this.f + 1, cz + 3.5, guardian, mob -> {
            mob.setCustomName(net.minecraft.network.chat.Component.translatable("entity.mistech.shrine_guardian",
                    net.minecraft.network.chat.Component.translatable(element.translationKey())).withColor(element.color()));
            mob.setGlowingTag(true);
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(90.0);
                mob.setHealth(90.0F);
            }
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(
                        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + 4.0);
            }
        });
        this.bp.block(cx - 1, this.f + 1, cz + 3, st(t.accent()));
        this.bp.block(cx + 1, this.f + 1, cz + 3, st(t.accent()));
        // runas decorativas no chão (marcadores do elemento)
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6.0 + rnd.nextDouble() * 0.2;
            this.bp.block(cx + (int) Math.round(Math.cos(a) * 5.5), this.f + 1, cz + (int) Math.round(Math.sin(a) * 5.5), st(t.light()));
        }
    }

    public Blueprint blueprint() {
        return this.bp;
    }
}
