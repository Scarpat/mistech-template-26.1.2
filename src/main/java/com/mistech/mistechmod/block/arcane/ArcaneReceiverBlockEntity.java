package com.mistech.mistechmod.block.arcane;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Só se registra na rede quando carrega e sai dela quando é removido/descarregado. */
public class ArcaneReceiverBlockEntity extends BlockEntity {
    public ArcaneReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ARCANE_RECEIVER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ArcaneNetwork.addReceiver(this.level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            ArcaneNetwork.removeReceiver(this.level, this.worldPosition);
        }
        super.setRemoved();
    }
}
