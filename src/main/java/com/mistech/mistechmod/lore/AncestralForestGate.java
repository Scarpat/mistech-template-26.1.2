package com.mistech.mistechmod.lore;

/**
 * Liga/desliga a troca de bioma da Mata Ancestral. Fica desligada enquanto o jogo monta os registros só-vanilla
 * ({@code VanillaRegistries.createLookup}, na validação da inicialização e no datagen), porque ali o bioma do mod
 * não existe e o jogo quebraria com "Unreferenced key: mistech:ancestral_forest".
 */
public final class AncestralForestGate {
    private static final ThreadLocal<Integer> VANILLA_ONLY = ThreadLocal.withInitial(() -> 0);

    private AncestralForestGate() {}

    public static void enterVanilla() {
        VANILLA_ONLY.set(VANILLA_ONLY.get() + 1);
    }

    public static void leaveVanilla() {
        VANILLA_ONLY.set(Math.max(0, VANILLA_ONLY.get() - 1));
    }

    public static boolean enabled() {
        return VANILLA_ONLY.get() == 0;
    }
}
