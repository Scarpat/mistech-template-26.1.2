package com.mistech.mistechmod.beyond;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Guarda os pontos de experiência do Coletor de Ecos. */
public class EchoCollectorBlockEntity extends BlockEntity {
    public static final int MAX_XP = 20_000;
    public static final int XP_PER_BOTTLE = 10;
    /** Raio de escuta do Catalisador de Sculk vanilla. */
    public static final int CATALYST_RANGE = 8;

    private int stored;

    public EchoCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BeyondContent.ECHO_COLLECTOR_BE.get(), pos, state);
    }

    public int stored() {
        return this.stored;
    }

    public void add(int points) {
        this.set(Math.min(MAX_XP, this.stored + points));
    }

    /** Tira {@code points} se tiver. */
    public boolean take(int points) {
        if (points <= 0 || this.stored < points) {
            return false;
        }
        this.set(this.stored - points);
        return true;
    }

    private void set(int value) {
        this.stored = value;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            int fullness = value <= 0 ? 0 : Math.max(1, (int) Math.ceil(4.0 * value / MAX_XP));
            BlockState state = this.getBlockState();
            if (state.hasProperty(EchoCollectorBlock.FULLNESS) && state.getValue(EchoCollectorBlock.FULLNESS) != fullness) {
                this.level.setBlock(this.worldPosition, state.setValue(EchoCollectorBlock.FULLNESS, fullness), Block.UPDATE_ALL);
            }
            this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
            // o cliente precisa saber quanto tem (a Mesa de Astrologia mostra se o coletor paga)
            this.level.sendBlockUpdated(this.worldPosition, state, this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Raio em volta da Mesa de Astrologia em que os coletores pagam o XP das estrelas. */
    public static final int ASTROLOGY_RANGE = 8;

    /** Soma da experiência dos Coletores a até {@link #ASTROLOGY_RANGE} blocos de {@code center}. */
    public static int storedNear(net.minecraft.world.level.Level level, BlockPos center) {
        int total = 0;
        for (EchoCollectorBlockEntity collector : near(level, center)) {
            total += collector.stored;
        }
        return total;
    }

    /** Tira {@code points} dos coletores perto (do primeiro ao último). @return true se conseguiu tudo. */
    public static boolean takeNear(net.minecraft.world.level.Level level, BlockPos center, int points) {
        if (storedNear(level, center) < points) {
            return false;
        }
        for (EchoCollectorBlockEntity collector : near(level, center)) {
            int take = Math.min(points, collector.stored);
            if (take > 0) {
                collector.set(collector.stored - take);
                points -= take;
            }
            if (points <= 0) {
                break;
            }
        }
        return true;
    }

    private static java.util.List<EchoCollectorBlockEntity> near(net.minecraft.world.level.Level level, BlockPos center) {
        java.util.List<EchoCollectorBlockEntity> list = new java.util.ArrayList<>();
        int r = ASTROLOGY_RANGE;
        for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
            for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof EchoCollectorBlockEntity collector && be.getBlockPos().distSqr(center) <= r * r) {
                        list.add(collector);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Um mob morreu em {@code deathPos} soltando {@code xp}: se houver um Catalisador de Sculk a até 8 blocos com um Coletor
     * encostado (e com espaço), o coletor guarda a experiência.
     */
    public static boolean capture(ServerLevel level, BlockPos deathPos, int xp) {
        int r = CATALYST_RANGE;
        for (int cx = (deathPos.getX() - r) >> 4; cx <= (deathPos.getX() + r) >> 4; cx++) {
            for (int cz = (deathPos.getZ() - r) >> 4; cz <= (deathPos.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be.getType() != BlockEntityType.SCULK_CATALYST || be.getBlockPos().distSqr(deathPos) > r * r) {
                        continue;
                    }
                    for (Direction dir : Direction.values()) {
                        BlockPos next = be.getBlockPos().relative(dir);
                        if (level.getBlockEntity(next) instanceof EchoCollectorBlockEntity collector && collector.stored < MAX_XP) {
                            collector.add(xp);
                            level.sendParticles(ParticleTypes.SCULK_SOUL, deathPos.getX() + 0.5, deathPos.getY() + 1.0, deathPos.getZ() + 0.5,
                                    6, 0.3, 0.4, 0.3, 0.02);
                            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, next.getX() + 0.5, next.getY() + 1.05, next.getZ() + 0.5,
                                    4, 0.25, 0.05, 0.25, 0.01);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Quebrado: a experiência guardada sai em bolinhas. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel server && this.stored > 0) {
            net.minecraft.world.entity.ExperienceOrb.award(server, pos.getCenter(), this.stored);
            this.stored = 0;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.getIntOr("StoredXp", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("StoredXp", this.stored);
    }
}
