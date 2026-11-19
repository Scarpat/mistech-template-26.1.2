package com.mistech.mistechmod.extraplanar.client;

import com.mistech.mistechmod.extraplanar.CodedBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Ponte do lado do cliente (só é carregada quando o clique acontece no cliente). */
public final class ExtraplanarClient {
    private ExtraplanarClient() {}

    public static void openCodeScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof CodedBlockEntity coded) {
            minecraft.setScreen(new ExtraplanarCodeScreen(pos, coded.getCode()));
        }
    }
}
