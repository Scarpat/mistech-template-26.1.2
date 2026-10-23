package com.mistech.mistechmod.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyField;
import com.mistech.mistechmod.block.AstralPillarBlock;
import com.mistech.mistechmod.block.IncenseHolderBlock;
import com.mistech.mistechmod.block.WritingTableBlock;
import com.mistech.mistechmod.block.confector.ConfectorPart;
import com.mistech.mistechmod.block.confector.RuneConfectorBlock;
import com.mistech.mistechmod.block.ritual.ImpureAltarBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.SanctuaryStructure;
import com.mistech.mistechmod.registry.AltarContent;
import com.mistech.mistechmod.registry.MachineContent;
import com.mistech.mistechmod.registry.MurkyWood;
import com.mistech.mistechmod.registry.RitualContent;
import com.mistech.mistechmod.ritual.AltarStructure;
import com.mistech.mistechmod.tarot.TarotField;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plantas dos multiblocos para o holograma da Pedra Guia (agachado + botão direito no bloco "âncora").
 * Só lê blocos (funciona no cliente). Cada âncora pode ter várias plantas (clicar de novo troca).
 * <p>
 * Âncoras: Mesa de Escrita (Astrologia, Tarologia, Confector), Mesa de Astrologia (Pilares Astrais), Altar Impuro
 * (círculo do tier atual e do próximo), Fonte do Santuário, Bloco de Magia Impura (Pilar Astral, Mineradora).
 */
public final class MultiblockPlans {
    private MultiblockPlans() {}

    /** Uma peça: posição, o que vale e o que mostrar. {@code optional} = recomendada, não obrigatória. */
    public record Piece(BlockPos pos, BiPredicate<Level, BlockPos> ok, ItemStack icon, Component name, boolean optional) {
        public boolean isOk(Level level) {
            return this.ok.test(level, this.pos);
        }
    }

    public record Plan(Component title, List<Piece> pieces) {
        public int missing(Level level) {
            int n = 0;
            for (Piece p : this.pieces) {
                if (!p.optional() && !p.isOk(level)) {
                    n++;
                }
            }
            return n;
        }
    }

    /** O bloco é âncora de algum multibloco? (vale nos dois lados, para cancelar o clique igual). */
    public static boolean isAnchor(BlockState state) {
        Block b = state.getBlock();
        return b instanceof WritingTableBlock || b instanceof ImpureAltarBlock || state.is(Mistech.ASTROLOGY_TABLE.get())
                || state.is(PurityContent.SANCTUARY_FONT.get()) || state.is(Mistech.IMPURE_MAGIC_BLOCK.get())
                || state.is(com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE.get());
    }

    public static List<Plan> plans(Level level, BlockPos anchor) {
        BlockState state = level.getBlockState(anchor);
        List<Plan> list = new ArrayList<>();
        Block b = state.getBlock();
        if (b instanceof WritingTableBlock) {
            list.add(field("multiblock.mistech.astrology", anchor, AstrologyField.PATTERN, AstrologyField.RADIUS, 'L', Blocks.LAPIS_BLOCK, 'O', Blocks.GOLD_BLOCK, '\0', null));
            list.add(tarot(anchor));
            Direction facing = state.getValue(WritingTableBlock.FACING);
            list.add(confector(anchor, facing, false));
            list.add(confector(anchor, facing, true));
        } else if (state.is(Mistech.ASTROLOGY_TABLE.get())) {
            list.add(astralPillarsAroundTable(anchor));
        } else if (b instanceof ImpureAltarBlock altar) {
            int tier = altar.tier();
            list.add(altar(anchor, tier));
            if (tier < 3) {
                list.add(altar(anchor, tier + 1));
            }
        } else if (state.is(PurityContent.SANCTUARY_FONT.get())) {
            list.add(sanctuary(anchor));
        } else if (state.is(com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE.get())) {
            List<Piece> pieces = new ArrayList<>();
            for (var part : com.mistech.mistechmod.machine.forge.CrusherForgeStructure.parts(anchor,
                    state.getValue(com.mistech.mistechmod.machine.MachineBlock.FACING))) {
                pieces.add(block(part.pos(), part.block()));
            }
            list.add(new Plan(Component.translatable("block.mistech.crusher_forge"), pieces));
        } else if (state.is(Mistech.IMPURE_MAGIC_BLOCK.get())) {
            list.add(astralPillar(anchor));
            list.add(miner(anchor));
        }
        return list;
    }

    // ----- peças -----

    private static Piece block(BlockPos pos, Block block) {
        return new Piece(pos.immutable(), (l, p) -> l.getBlockState(p).is(block), new ItemStack(block), block.getName(), false);
    }

    private static Piece optional(Piece p) {
        return new Piece(p.pos(), p.ok(), p.icon(), p.name(), true);
    }

    // ----- plantas -----

    private static Plan field(String key, BlockPos table, String[] pattern, int radius, char c1, Block b1, char c2, Block b2,
                              char c3, Block b3) {
        List<Piece> pieces = new ArrayList<>();
        BlockPos center = table.below();
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length(); col++) {
                char ch = pattern[row].charAt(col);
                Block expected = ch == c1 ? b1 : ch == c2 ? b2 : ch == c3 && b3 != null ? b3 : null;
                if (expected != null) {
                    pieces.add(block(center.offset(col - radius, 0, row - radius), expected));
                }
            }
        }
        return new Plan(Component.translatable(key), pieces);
    }

    private static Plan tarot(BlockPos table) {
        Plan base = field("multiblock.mistech.tarot", table, TarotField.PATTERN, TarotField.RADIUS, 'A', Blocks.AMETHYST_BLOCK,
                'O', Blocks.OBSIDIAN, 'C', Blocks.CRYING_OBSIDIAN);
        List<Piece> pieces = new ArrayList<>(base.pieces());
        for (int i = 0; i < TarotField.PEDESTALS; i++) {
            pieces.add(optional(block(TarotField.pedestalPos(table, i), RitualContent.RITUAL_PEDESTAL.get())));
        }
        return new Plan(base.title(), pieces);
    }

    private static Plan confector(BlockPos table, Direction facing, boolean mirrored) {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(block(RuneConfectorBlock.partPos(table, facing, mirrored, ConfectorPart.BARREL), Mistech.SPATIAL_BARREL.get()));
        pieces.add(block(RuneConfectorBlock.partPos(table, facing, mirrored, ConfectorPart.TABLE_TANK), Mistech.RUNIC_TANK.get()));
        pieces.add(block(RuneConfectorBlock.partPos(table, facing, mirrored, ConfectorPart.BARREL_TANK), Mistech.RUNIC_TANK.get()));
        return new Plan(Component.translatable(mirrored ? "multiblock.mistech.confector_left" : "multiblock.mistech.confector_right"), pieces);
    }

    private static Plan astralPillarsAroundTable(BlockPos table) {
        List<Piece> pieces = new ArrayList<>();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = table.relative(dir, 2);
            pieces.add(new Piece(pos, (l, p) -> {
                BlockState s = l.getBlockState(p);
                return s.is(Mistech.ASTRAL_PILLAR.get()) && s.getValue(AstralPillarBlock.PART) == AstralPillarBlock.Part.BOTTOM;
            }, new ItemStack(Mistech.ASTRAL_PILLAR_ITEM.get()), Mistech.ASTRAL_PILLAR.get().getName(), true));
        }
        return new Plan(Component.translatable("multiblock.mistech.astral_link"), pieces);
    }

    private static Plan astralPillar(BlockPos top) {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(block(top.below(), Blocks.LAPIS_BLOCK));
        BlockPos bottom = top.below(2);
        pieces.add(new Piece(bottom, (l, p) -> l.getBlockState(p).is(MurkyWood.LOG.get()) || l.getBlockState(p).is(MurkyWood.WOOD.get()),
                new ItemStack(MurkyWood.LOG.get()), MurkyWood.LOG.get().getName(), false));
        return new Plan(Component.translatable("block.mistech.astral_pillar"), pieces);
    }

    private static Plan miner(BlockPos top) {
        List<Piece> pieces = new ArrayList<>();
        BlockPos center = top.below();
        pieces.add(block(center, Mistech.SPATIAL_BARREL.get()));
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            pieces.add(block(center.relative(dir), MachineContent.TUNNELER.get()));
        }
        return new Plan(Component.translatable("block.mistech.miner"), pieces);
    }

    private static Plan altar(BlockPos altar, int tier) {
        List<Piece> pieces = new ArrayList<>();
        for (BlockPos p : AltarStructure.chalk(altar)) {
            pieces.add(block(p, RitualContent.CHALK_MARK.get()));
        }
        if (tier >= 2) {
            for (BlockPos p : AltarStructure.impureChalk(altar)) {
                pieces.add(block(p, RitualContent.IMPURE_CHALK_MARK.get()));
            }
        }
        for (BlockPos p : AltarStructure.pedestals(altar, tier)) {
            pieces.add(block(p, RitualContent.RITUAL_PEDESTAL.get()));
        }
        Block holder = Mistech.INCENSE_HOLDER.get();
        for (BlockPos p : AltarStructure.holders(altar, tier)) {
            pieces.add(new Piece(p.immutable(), (l, q) -> {
                BlockState s = l.getBlockState(q);
                return s.getBlock() instanceof IncenseHolderBlock && s.getValue(IncenseHolderBlock.LIT) && s.getValue(IncenseHolderBlock.MAGIC);
            }, new ItemStack(holder), Component.translatable("multiblock.mistech.piece.magic_incense_holder"), false));
        }
        if (tier >= 2) {
            for (BlockPos p : AltarStructure.obelisks(altar)) {
                pieces.add(block(p, AltarContent.MITHRIL_OBELISK.get()));
            }
        }
        if (tier >= 3) {
            for (BlockPos p : AltarStructure.pillars(altar)) {
                pieces.add(new Piece(p.immutable(), (l, q) -> {
                    BlockState s = l.getBlockState(q);
                    return s.is(Mistech.ASTRAL_PILLAR.get()) && s.getValue(AstralPillarBlock.PART) == AstralPillarBlock.Part.BOTTOM;
                }, new ItemStack(Mistech.ASTRAL_PILLAR_ITEM.get()), Mistech.ASTRAL_PILLAR.get().getName(), false));
            }
            Block brazier = AltarContent.ORICHALCUM_BRAZIER.get();
            for (BlockPos p : AltarStructure.braziers(altar)) {
                pieces.add(new Piece(p.immutable(), (l, q) -> {
                    BlockState s = l.getBlockState(q);
                    return s.is(brazier) && s.getValue(com.mistech.mistechmod.block.ritual.OrichalcumBrazierBlock.LIT);
                }, new ItemStack(brazier), Component.translatable("multiblock.mistech.piece.lit_brazier"), false));
            }
        }
        return new Plan(Component.translatable("multiblock.mistech.altar", tier), pieces);
    }

    private static Plan sanctuary(BlockPos font) {
        List<Piece> pieces = new ArrayList<>();
        for (BlockPos p : SanctuaryStructure.pedestals(font)) {
            pieces.add(block(p, RitualContent.RITUAL_PEDESTAL.get()));
        }
        for (BlockPos p : SanctuaryStructure.waters(font)) {
            pieces.add(new Piece(p.immutable(), (l, q) -> l.getFluidState(q).is(FluidTags.WATER) && l.getFluidState(q).isSource(),
                    new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), Component.translatable("multiblock.mistech.piece.water_source"), false));
        }
        for (BlockPos p : SanctuaryStructure.obelisks(font)) {
            pieces.add(block(p, AltarContent.MITHRIL_OBELISK.get()));
        }
        return new Plan(Component.translatable("block.mistech.sanctuary_font"), pieces);
    }
}
