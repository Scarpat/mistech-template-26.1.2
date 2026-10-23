package com.mistech.mistechmod.compat;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * Bloco (block entity) que sabe se descrever em poucas linhas: progresso, energia, tanque, situação...
 * Usado pela integração com o Jade (as linhas são montadas no servidor e mandadas ao cliente),
 * mas não depende de nenhum mod: qualquer block entity do Mistech pode implementar.
 */
public interface InfoProvider {
    /** Só no servidor. Linhas curtas, já traduzíveis ({@code Component.translatable}). */
    void addInfo(List<Component> lines);

    /** Texto "Progresso: N%" padrão. */
    static Component progress(int value, int max) {
        int percent = max <= 0 ? 0 : Math.min(100, value * 100 / max);
        return Component.translatable("info.mistech.progress", percent);
    }

    static Component energy(int amount, int capacity) {
        return Component.translatable("info.mistech.energy", String.format(java.util.Locale.ROOT, "%,d", amount),
                String.format(java.util.Locale.ROOT, "%,d", capacity));
    }

    static Component seconds(String key, int ticks) {
        return Component.translatable(key, String.format(java.util.Locale.ROOT, "%.0f", Math.ceil(ticks / 20.0)));
    }
}
