package com.mistech.mistechmod.item.mithril;

import java.util.function.Consumer;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyData;
import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Vínculo com constelação (Oricalco e Oricalco Puro): clicar com a peça na Mesa de Astrologia grava a próxima
 * constelação COMPLETA (clicar de novo troca; Shift+clique desfaz). Cada peça dá uma fração do bônus total da
 * constelação, no mesmo atributo dela (25% no Oricalco, 35% no Oricalco Puro). Peças com a mesma constelação somam.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GearConstellation {
    public static final double ORICHALCUM_SHARE = 0.25;
    public static final double PURE_SHARE = 0.35;

    private GearConstellation() {}

    public static int get(ItemStack gear) {
        return gear.getOrDefault(MistechComponents.GEAR_CONSTELLATION.get(), -1);
    }

    public static double share(GearTier tier) {
        return tier == GearTier.PURE_ORICHALCUM ? PURE_SHARE : ORICHALCUM_SHARE;
    }

    /** Valor do bônus que a peça dá (no atributo da constelação). */
    public static double bonus(ItemStack gear) {
        int c = get(gear);
        if (c < 0 || c >= Constellations.ALL.size() || !MithrilGear.tier(gear).bindsConstellation()) {
            return 0.0;
        }
        Constellations.Constellation constellation = Constellations.get(c);
        return constellation.perk().perStar() * constellation.stars().size() * share(MithrilGear.tier(gear));
    }

    /** Chamado no {@link ItemAttributeModifierEvent} (peça já conferida: revestida e com energia). */
    static void addModifiers(ItemAttributeModifierEvent event, ItemStack gear, EquipmentSlotGroup group) {
        double amount = bonus(gear);
        if (amount == 0.0) {
            return;
        }
        Constellations.Perk perk = Constellations.get(get(gear)).perk();
        Identifier id = Identifier.fromNamespaceAndPath(Mistech.MODID, "gear_constellation." + group.getSerializedName());
        event.addModifier(perk.attribute(), new AttributeModifier(id, amount, perk.operation()), group);
    }

    public static void tooltip(ItemStack gear, Consumer<Component> builder) {
        if (!MithrilGear.isMithril(gear) || !MithrilGear.tier(gear).bindsConstellation()) {
            return;
        }
        int c = get(gear);
        if (c < 0 || c >= Constellations.ALL.size()) {
            builder.accept(Component.translatable("item.mistech.gear.constellation_none").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        Constellations.Constellation constellation = Constellations.get(c);
        builder.accept(Component.translatable("item.mistech.gear.constellation", constellation.name()).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.literal("  ").append(Component.translatable("astrology.mistech.perk." + constellation.id(),
                constellation.perk().format(bonus(gear)))).withStyle(ChatFormatting.DARK_AQUA));
    }

    @SubscribeEvent
    static void onUseTable(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!MithrilGear.isMithril(stack) || !MithrilGear.tier(stack).bindsConstellation()
                || !event.getLevel().getBlockState(event.getPos()).is(Mistech.ASTROLOGY_TABLE.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Player player = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (player.isShiftKeyDown()) {
            if (get(stack) >= 0) {
                stack.remove(MistechComponents.GEAR_CONSTELLATION.get());
                player.sendOverlayMessage(Component.translatable("message.mistech.gear.constellation_cleared"));
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            return;
        }
        AstrologyData data = player.getData(MistechAttachments.ASTROLOGY);
        int size = Constellations.ALL.size();
        int current = get(stack);
        for (int step = 1; step <= size; step++) {
            int next = Math.floorMod(current + step, size);
            if (data.countIn(next) >= Constellations.get(next).stars().size()) {
                if (next == current) {
                    break; // só tem uma: já está nela
                }
                stack.set(MistechComponents.GEAR_CONSTELLATION.get(), next);
                player.sendOverlayMessage(Component.translatable("message.mistech.gear.constellation_bound",
                        Constellations.get(next).name()));
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.5F);
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.04);
                return;
            }
        }
        if (current >= 0) {
            player.sendOverlayMessage(Component.translatable("message.mistech.gear.constellation_bound", Constellations.get(current).name()));
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.constellation_amulet.none"));
    }
}
