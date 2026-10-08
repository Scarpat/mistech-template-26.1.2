package com.mistech.mistechmod.client;

import java.util.List;

import com.mistech.mistechmod.network.ConfectorRecipesPayload;

/** Lista de runas do Confector recebida do servidor (as receitas não existem no cliente). */
public final class ClientConfectorRecipes {
    private static List<ConfectorRecipesPayload.Entry> entries = List.of();

    private ClientConfectorRecipes() {}

    public static void set(List<ConfectorRecipesPayload.Entry> list) {
        entries = List.copyOf(list);
    }

    public static List<ConfectorRecipesPayload.Entry> get() {
        return entries;
    }
}
