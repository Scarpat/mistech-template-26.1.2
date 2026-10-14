package com.mistech.mistechmod.block;

import com.mistech.mistechmod.registry.MistechFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Capability de fluido (Essência Mágica Impura) para blocos que guardam a essência no estado do bloco, para golens com
 * Runa da Água, canos de outros mods etc. Mesmo esquema do caldeirão do NeoForge: muda o estado dentro da transação e só
 * avisa vizinhos/cliente no commit.
 * <ul>
 * <li>Extrator de Essência: só tira. Cada nível = 1 frasco = 250 mB; cheio (3 níveis) dá um balde inteiro (1000 mB), como à mão.
 * No Nether a essência sai Ígnea (não é fluido), então lá não tira nada.</li>
 * <li>Compressor de Magia: só coloca. 250 mB (um frasco) enche o buraco.</li>
 * </ul>
 */
public final class EssenceFluidHandlers {
    public static final int PER_LEVEL = 250;
    public static final int FULL = 1000;

    private EssenceFluidHandlers() {}

    private static FluidResource essence() {
        return FluidResource.of(MistechFluids.IMPURE_ESSENCE.get());
    }

    /** Base: um bloco cujo estado guarda o conteúdo. */
    private abstract static class StateHandler extends SnapshotJournal<BlockState> implements ResourceHandler<FluidResource> {
        protected final Level level;
        protected final BlockPos pos;

        StateHandler(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        protected BlockState state() {
            return this.level.getBlockState(this.pos);
        }

        protected void setState(BlockState state, TransactionContext tx) {
            this.updateSnapshots(tx);
            this.level.setBlock(this.pos, state, 0);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        protected BlockState createSnapshot() {
            return this.state();
        }

        @Override
        protected void revertToSnapshot(BlockState snapshot) {
            this.level.setBlock(this.pos, snapshot, 0);
        }

        @Override
        protected void onRootCommit(BlockState original) {
            BlockState now = this.state();
            if (now == original || now.getBlock() != original.getBlock()) {
                return;
            }
            this.level.setBlock(this.pos, original, 0);
            this.level.setBlockAndUpdate(this.pos, now);
            this.committed(original, now);
        }

        protected void committed(BlockState before, BlockState after) {}
    }

    /** Extrator de Essência: tira por nível (250 mB) ou o balde inteiro quando cheio. */
    public static final class Extractor extends StateHandler {
        public Extractor(Level level, BlockPos pos) {
            super(level, pos);
        }

        private int levelOf(BlockState state) {
            if (!state.hasProperty(EssenceExtractorBlock.LEVEL) || this.level.dimension() == Level.NETHER) {
                return 0;
            }
            return state.getValue(EssenceExtractorBlock.LEVEL);
        }

        private static int amountFor(int lvl) {
            return lvl >= EssenceExtractorBlock.MAX_LEVEL ? FULL : lvl * PER_LEVEL;
        }

        @Override
        public FluidResource getResource(int index) {
            return this.levelOf(this.state()) > 0 ? essence() : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return amountFor(this.levelOf(this.state()));
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return FULL;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.equals(essence());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
            BlockState state = this.state();
            int lvl = this.levelOf(state);
            if (lvl <= 0 || amount <= 0 || !resource.equals(essence())) {
                return 0;
            }
            int taken;
            int newLevel;
            if (lvl >= EssenceExtractorBlock.MAX_LEVEL && amount >= FULL) {
                taken = FULL;
                newLevel = 0;
            } else {
                int levels = Math.min(lvl, amount / PER_LEVEL);
                if (levels <= 0) {
                    return 0;
                }
                taken = levels * PER_LEVEL;
                newLevel = lvl - levels;
            }
            this.setState(state.setValue(EssenceExtractorBlock.LEVEL, newLevel), tx);
            return taken;
        }
    }

    /** Compressor de Magia: aceita um frasco (250 mB) de essência quando o buraco está vazio. */
    public static final class Compressor extends StateHandler {
        public Compressor(Level level, BlockPos pos) {
            super(level, pos);
        }

        private boolean filled(BlockState state) {
            return !state.hasProperty(MagicCompressorBlock.FILLED) || state.getValue(MagicCompressorBlock.FILLED);
        }

        @Override
        public FluidResource getResource(int index) {
            return this.filled(this.state()) ? essence() : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.filled(this.state()) ? PER_LEVEL : 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return PER_LEVEL;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.equals(essence());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
            BlockState state = this.state();
            if (amount < PER_LEVEL || this.filled(state) || !resource.equals(essence())) {
                return 0;
            }
            this.setState(state.setValue(MagicCompressorBlock.FILLED, true), tx);
            return PER_LEVEL;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
            return 0;
        }

        @Override
        protected void committed(BlockState before, BlockState after) {
            if (this.level.getBlockEntity(this.pos) instanceof MagicCompressorBlockEntity compressor) {
                compressor.setIgneous(false);
            }
            this.level.playSound(null, this.pos, net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }
}
