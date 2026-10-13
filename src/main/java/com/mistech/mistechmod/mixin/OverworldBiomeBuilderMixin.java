package com.mistech.mistechmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mistech.mistechmod.lore.AncestralForestGate;
import com.mistech.mistechmod.lore.LoreContent;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

/**
 * Põe a Mata Ancestral no mundo normal: metade (estranheza negativa) das selvas mais úmidas (temperatura 3, umidade 4)
 * e das florestas escuras (temperatura 2, umidade 4) vira Mata Ancestral. As variantes (selva de bambu) ficam como estão.
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @Inject(method = "pickMiddleBiome", at = @At("HEAD"), cancellable = true)
    private void mistech$ancestralForest(int temperature, int humidity, Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir) {
        if (!AncestralForestGate.enabled()) {
            return;
        }
        if (humidity == 4 && (temperature == 2 || temperature == 3) && weirdness.max() < 0) {
            cir.setReturnValue(LoreContent.ANCESTRAL_FOREST);
        }
    }
}
