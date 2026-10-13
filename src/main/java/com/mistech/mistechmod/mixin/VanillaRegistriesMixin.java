package com.mistech.mistechmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mistech.mistechmod.lore.AncestralForestGate;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;

/**
 * Enquanto o jogo monta os registros só-vanilla (validação na inicialização, datagen), a Mata Ancestral não existe
 * nesse conjunto; se o {@link OverworldBiomeBuilderMixin} a devolvesse ali, o jogo quebra com
 * "Unreferenced key: mistech:ancestral_forest". Então desligamos a troca durante {@code createLookup}.
 */
@Mixin(VanillaRegistries.class)
public abstract class VanillaRegistriesMixin {
    @Inject(method = "createLookup", at = @At("HEAD"))
    private static void mistech$enterVanilla(CallbackInfoReturnable<HolderLookup.Provider> cir) {
        AncestralForestGate.enterVanilla();
    }

    @Inject(method = "createLookup", at = @At("RETURN"))
    private static void mistech$leaveVanilla(CallbackInfoReturnable<HolderLookup.Provider> cir) {
        AncestralForestGate.leaveVanilla();
    }
}
