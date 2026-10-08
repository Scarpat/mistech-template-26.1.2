package com.mistech.mistechmod.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Relé e Selo: só se registram no {@link ArchiveRegistry} enquanto estão carregados. */
public class ArchiveMemberBlockEntity extends BlockEntity {
    private final ArchiveRegistry.Kind kind;

    public ArchiveMemberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ArchiveRegistry.Kind kind) {
        super(type, pos, state);
        this.kind = kind;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.add(this.level, this.kind, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            ArchiveRegistry.remove(this.level, this.kind, this.worldPosition);
        }
        super.setRemoved();
    }
}
