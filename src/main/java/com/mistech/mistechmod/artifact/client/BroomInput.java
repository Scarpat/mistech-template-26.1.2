package com.mistech.mistechmod.artifact.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;

/** Só no cliente: lê as teclas de movimento do jogador local para a vassoura. */
public final class BroomInput {
    private BroomInput() {}

    /** {frente (-1..1), lado (-1..1, positivo = esquerda), subir (0/1)}. */
    public static float[] read() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return new float[] {0.0F, 0.0F, 0.0F};
        }
        Vec2 move = player.input.getMoveVector();
        return new float[] {move.y, move.x, player.input.keyPresses.jump() ? 1.0F : 0.0F};
    }
}
