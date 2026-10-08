package com.mistech.mistechmod.ritual;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.ritual.ImpureAltarBlockEntity;
import com.mistech.mistechmod.entity.BlackCatEntity;
import com.mistech.mistechmod.entity.OwlEntity;
import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Efeitos no mundo dos rituais (campo "action" da receita). */
public final class RitualActions {
    public static final int HARVEST_RADIUS = 12;
    public static final long WARD_TICKS = 3L * 24000L;

    private RitualActions() {}

    /** Confere antes de começar. @return mensagem de erro ou null se pode. */
    public static @Nullable Component check(String action, ServerLevel level, BlockPos altar, @Nullable Player player) {
        switch (action) {
            case "summon_owl", "summon_black_cat" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
                boolean owl = action.equals("summon_owl");
                int owned = owl ? OwlEntity.countOwned(level, player.getUUID()) : BlackCatEntity.countOwned(level, player.getUUID());
                if (owned > 0) {
                    return Component.translatable("message.mistech.ritual.familiar_exists",
                            Component.translatable(owl ? "entity.mistech.owl" : "entity.mistech.black_cat"));
                }
            }
            case "full_moon" -> {
                if (isFullMoonNight(level, altar)) {
                    return Component.translatable("message.mistech.ritual.already_full_moon");
                }
            }
            case "clear_sky" -> {
                if (!level.isRaining()) {
                    return Component.translatable("message.mistech.ritual.not_raining");
                }
            }
            case "astral_rebirth" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
            }
            case "realign_sun", "realign_moon", "realign_rising" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
                if (!(level.getBlockEntity(altar) instanceof ImpureAltarBlockEntity be)) {
                    return null;
                }
                int sign = realignSign(be.pedestalItems());
                if (sign < 0) {
                    return Component.translatable("message.mistech.natal.realign_token");
                }
                com.mistech.mistechmod.astrology.NatalChart.Part part = realignPart(action);
                if (com.mistech.mistechmod.astrology.NatalService.ensureRolled(player).sign(part) == sign) {
                    return Component.translatable("message.mistech.natal.realign_same", part.displayName(),
                            com.mistech.mistechmod.astrology.NatalChart.signName(sign));
                }
            }
            case "ascend_altar_2", "ascend_altar_3" -> {
                int want = action.endsWith("2") ? 1 : 2;
                if (!(level.getBlockEntity(altar) instanceof ImpureAltarBlockEntity be) || be.tier() != want) {
                    return Component.translatable("message.mistech.ritual.wrong_altar_tier", want);
                }
                // as peças do próximo tier já precisam estar no lugar
                AltarStructure.Result next = AltarStructure.checkAscension(level, altar, want + 1);
                if (!next.ok()) {
                    BlockPos bad = next.pos();
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, bad.getX() + 0.5, bad.getY() + 0.8, bad.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
                    return Component.translatable("message.mistech.ritual.ascension_needs",
                            Component.translatable("message.mistech.ritual.structure." + next.problem().name().toLowerCase(java.util.Locale.ROOT),
                                    bad.getX(), bad.getY(), bad.getZ())).withStyle(net.minecraft.ChatFormatting.RED);
                }
            }
            default -> {
                ItemStack catalyst = level.getBlockEntity(altar) instanceof com.mistech.mistechmod.block.ritual.SingleItemBlockEntity be ? be.getStored() : null;
                return com.mistech.mistechmod.celestial.CelestialRituals.check(action, level, altar, player, catalyst);
            }
        }
        return null;
    }

    public static void run(String action, ServerLevel level, BlockPos altar, ImpureAltarBlockEntity altarEntity, @Nullable Player player) {
        switch (action) {
            case "summon_owl" -> {
                OwlEntity owl = RitualContent.OWL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (owl != null && player != null) {
                    owl.snapTo(altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    owl.bindTo(player);
                    level.addFreshEntity(owl);
                    poof(level, altar);
                }
            }
            case "summon_black_cat" -> {
                BlackCatEntity cat = RitualContent.BLACK_CAT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (cat != null && player != null) {
                    cat.snapTo(altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    cat.bindTo(player);
                    level.addFreshEntity(cat);
                    poof(level, altar);
                }
            }
            case "storm" -> level.getServer().setWeatherParameters(0, 12000, true, true);
            case "clear_sky" -> level.getServer().setWeatherParameters(24000, 0, false, false);
            case "sunrise" -> moveClockTo(level, 0L);
            case "nightfall" -> moveClockTo(level, 13000L);
            case "full_moon" -> {
                // pula até o anoitecer da próxima lua cheia (ciclo de 8 dias, lua cheia = dia 0)
                moveClockTo(level, 13000L, 24000L * net.minecraft.world.level.MoonPhase.COUNT);
                level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, 50, 0.4, 2.0, 0.4, 0.05);
                level.playSound(null, altar, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.6F);
            }
            case "harvest_blessing" -> harvest(level, altar, player);
            case "protection_seal" -> altarEntity.setWard(level.getGameTime() + WARD_TICKS);
            case "astral_rebirth" -> {
                if (player != null) {
                    com.mistech.mistechmod.astrology.NatalService.rebirth(player);
                    level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 80, 0.8, 2.0, 0.8, 0.08);
                    level.playSound(null, altar, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.6F);
                    player.sendSystemMessage(Component.translatable("message.mistech.natal.rebirth")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
            }
            case "realign_sun", "realign_moon", "realign_rising" -> {
                int sign = realignSign(altarEntity.lastPedestals());
                if (player != null && sign >= 0) {
                    com.mistech.mistechmod.astrology.NatalChart.Part part = realignPart(action);
                    com.mistech.mistechmod.astrology.NatalChart chart = com.mistech.mistechmod.astrology.NatalService.setSign(player, part, sign);
                    // o Mapa Astral do catalisador volta atualizado
                    altarEntity.setStored(com.mistech.mistechmod.astrology.NatalService.copyOf(player, chart));
                    level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 60, 0.6, 2.0, 0.6, 0.06);
                    level.playSound(null, altar, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
                    player.sendSystemMessage(Component.translatable("message.mistech.natal.realigned", part.displayName(),
                            com.mistech.mistechmod.astrology.NatalChart.signName(sign)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
            }
            case "ascend_altar_2", "ascend_altar_3" -> {
                int newTier = altarEntity.ascend();
                level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 60, 0.6, 1.5, 0.6, 0.1);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 80, 1.5, 1.0, 1.5, 0.1);
                level.playSound(null, altar, net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.mistech.ritual.ascended", newTier)
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
            }
            default -> com.mistech.mistechmod.celestial.CelestialRituals.run(action, level, altar, altarEntity, player);
        }
    }

    private static com.mistech.mistechmod.astrology.NatalChart.Part realignPart(String action) {
        return switch (action) {
            case "realign_moon" -> com.mistech.mistechmod.astrology.NatalChart.Part.MOON;
            case "realign_rising" -> com.mistech.mistechmod.astrology.NatalChart.Part.RISING;
            default -> com.mistech.mistechmod.astrology.NatalChart.Part.SUN;
        };
    }

    /** Signo escolhido pelo item raro da constelação num pedestal (-1 se não tiver, ou se tiver de dois signos). */
    private static int realignSign(java.util.List<ItemStack> pedestals) {
        int found = -1;
        for (ItemStack stack : pedestals) {
            int sign = com.mistech.mistechmod.astrology.NatalService.signOfToken(stack);
            if (sign >= 0) {
                if (found >= 0 && found != sign) {
                    return -1;
                }
                found = sign;
            }
        }
        return found;
    }

    private static void poof(ServerLevel level, BlockPos altar) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.WITCH, altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
    }

    /** Avança o relógio do mundo até a próxima hora {@code target} (0 = amanhecer, 13000 = anoitecer). */
    private static void moveClockTo(ServerLevel level, long target) {
        moveClockTo(level, target, 24000L);
    }

    /** Avança o relógio até a próxima posição {@code target} dentro de um ciclo de {@code period} ticks. */
    private static void moveClockTo(ServerLevel level, long target, long period) {
        level.dimensionType().defaultClock().ifPresent(clock -> {
            long now = level.getServer().clockManager().getTotalTicks(clock);
            long inCycle = Math.floorMod(now, period);
            long delta = Math.floorMod(target - inCycle, period);
            if (delta == 0) {
                delta = period;
            }
            level.getServer().clockManager().addTicks(clock, (int) delta);
        });
    }

    private static boolean isFullMoonNight(ServerLevel level, BlockPos altar) {
        return level.isDarkOutside() && level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE,
                altar) == net.minecraft.world.level.MoonPhase.FULL_MOON;
    }

    private static void harvest(ServerLevel level, BlockPos altar, @Nullable Player player) {
        int r = HARVEST_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-r, -3, -r), altar.offset(r, 3, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop) {
                if (!crop.isMaxAge(state)) {
                    level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
                }
            } else if (state.is(BlockTags.SAPLINGS) && state.getBlock() instanceof BonemealableBlock sapling) {
                BlockPos immutable = pos.immutable();
                for (int i = 0; i < 6 && level.getBlockState(immutable).is(state.getBlock()); i++) {
                    sapling.performBonemeal(level, level.getRandom(), immutable, level.getBlockState(immutable));
                }
            }
        }
        for (Animal animal : level.getEntitiesOfClass(Animal.class, new AABB(altar).inflate(r, 4, r))) {
            if (animal.getAge() == 0 && animal.canFallInLove()) {
                animal.setInLove(player);
            }
        }
    }

}
