package com.mistech.mistechmod.entity;

import java.util.List;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Gato Preto (familiar básico, nasce do ritual "Chamado do Gato Preto"). Usa o gato do jogo (espanta creepers e phantoms,
 * senta com botão direito, dá presente de manhã quando o dono dorme). Alimentado (bacalhau/salmão cru = 10 min cada):
 * <ul>
 * <li>+1 de Sorte para o dono a até 16 blocos;</li>
 * <li>Presente da Meia-Noite: 1 item por noite (loot table {@code mistech:gameplay/black_cat_gift});</li>
 * <li>Sete Vidas: se o dono for morrer a até 32 blocos, fica com 1 coração + Absorção; gasta 1 das 7 vidas.
 * Sem vidas o gato continua como familiar, mas não salva mais.</li>
 * </ul>
 */
public class BlackCatEntity extends Cat {
    public static final int FOOD_TICKS = 10 * 60 * 20;
    public static final int MAX_FOOD_TICKS = 16 * FOOD_TICKS;
    public static final int MAX_LIVES = 7;
    public static final double LUCK_RANGE = 16.0;
    public static final double SAVE_RANGE = 32.0;
    public static final ResourceKey<LootTable> GIFT_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mistech.MODID, "gameplay/black_cat_gift"));

    private int fedTicks;
    private int lives = MAX_LIVES;
    private long lastGiftDay = -1;

    public BlackCatEntity(EntityType<? extends BlackCatEntity> type, Level level) {
        super(type, level);
    }

    public void bindTo(Player player) {
        this.level().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).get(CatVariants.ALL_BLACK)
                .ifPresent(variant -> this.setComponent(DataComponents.CAT_VARIANT, variant));
        this.tame(player);
        this.setPersistenceRequired();
        this.fedTicks = FOOD_TICKS;
        this.lives = MAX_LIVES;
    }

    public boolean isFed() {
        return this.fedTicks > 0;
    }

    public int lives() {
        return this.lives;
    }

    public static int countOwned(ServerLevel level, UUID owner) {
        return level.getEntities(RitualContent.BLACK_CAT.get(), cat -> owner.equals(OwlEntity.ownerId(cat))).size();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // não cruza; comida é tratada em mobInteract
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (this.isTame() && this.isOwnedBy(player) && super.isFood(held)) {
            if (!this.level().isClientSide()) {
                if (this.fedTicks + FOOD_TICKS > MAX_FOOD_TICKS) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.familiar.full"));
                    return InteractionResult.FAIL;
                }
                this.fedTicks += FOOD_TICKS;
                held.consume(1, player);
                this.heal(2.0F);
                this.playEatingSound();
                player.sendOverlayMessage(Component.translatable("message.mistech.black_cat.status", (this.fedTicks + 1199) / 1200, this.lives));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isTame() || this.tickCount % 20 != 0 || !this.isFed()) {
            return;
        }
        if (!(this.getOwner() instanceof Player owner)) {
            return;
        }
        this.fedTicks -= 20;
        if (owner.distanceToSqr(this) <= LUCK_RANGE * LUCK_RANGE) {
            owner.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
        }
        // Presente da Meia-Noite (18000 = meia-noite)
        long time = server.getDefaultClockTime();
        long day = Math.floorDiv(time, 24000L);
        long inDay = Math.floorMod(time, 24000L);
        if (inDay >= 18000L && inDay < 19000L && day != this.lastGiftDay) {
            this.lastGiftDay = day;
            this.dropFromGiftLootTable(server, GIFT_LOOT, (level, stack) -> level.addFreshEntity(
                    new ItemEntity(level, this.getX(), this.getY() + 0.3, this.getZ(), stack)));
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.5, this.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Sete Vidas: tenta salvar o dono de uma morte. @return true se salvou. */
    public static boolean trySave(ServerLevel level, Player owner) {
        List<? extends BlackCatEntity> cats = level.getEntities(RitualContent.BLACK_CAT.get(),
                cat -> owner.getUUID().equals(OwlEntity.ownerId(cat)) && cat.isFed() && cat.lives > 0
                        && cat.distanceToSqr(owner) <= SAVE_RANGE * SAVE_RANGE);
        if (cats.isEmpty()) {
            return false;
        }
        BlackCatEntity cat = cats.get(0);
        cat.lives--;
        owner.setHealth(2.0F);
        owner.removeAllEffects();
        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 1));
        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 1));
        level.sendParticles(ParticleTypes.SOUL, owner.getX(), owner.getY() + 1.0, owner.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, cat.getX(), cat.getY() + 0.4, cat.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        cat.playSound(SoundEvents.SOUL_ESCAPE.value(), 1.5F, 0.8F);
        owner.sendOverlayMessage(Component.translatable("message.mistech.black_cat.saved", cat.lives));
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FedTicks", this.fedTicks);
        output.putInt("Lives", this.lives);
        output.putLong("LastGiftDay", this.lastGiftDay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fedTicks = input.getIntOr("FedTicks", 0);
        this.lives = input.getIntOr("Lives", MAX_LIVES);
        this.lastGiftDay = input.getLongOr("LastGiftDay", -1L);
    }
}
