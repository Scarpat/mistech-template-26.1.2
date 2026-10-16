package com.mistech.mistechmod.realm.dream.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamRealmContent;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.OreContent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * Gnomo Mercador (vilarejos gnomos do Sonho Comum): troca coisas do sonho por esmeraldas e compra o que os gnomos gostam.
 * Usa a tela de troca do jogo; cada mercador sorteia 3 compras e 4 vendas da lista.
 */
public class GnomeMerchantEntity extends AbstractVillager {
    public GnomeMerchantEntity(EntityType<? extends GnomeMerchantEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.4));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading()) {
            if (!this.level().isClientSide()) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private record Trade(Supplier<? extends ItemLike> cost, int costCount, Supplier<? extends ItemLike> result, int resultCount, int uses) {
        MerchantOffer offer() {
            return new MerchantOffer(new ItemCost(this.cost.get(), this.costCount), new ItemStack(this.result.get(), this.resultCount), this.uses, 2, 0.05F);
        }
    }

    private static final List<Trade> BUYS = List.of(
            new Trade(() -> GnomeContent.YELLOW_MUSHROOM_ITEM.get(), 6, () -> Items.EMERALD, 1, 12),
            new Trade(() -> DreamRealmContent.CLOUD_WOOL.get(), 16, () -> Items.EMERALD, 1, 12),
            new Trade(() -> Items.SWEET_BERRIES, 20, () -> Items.EMERALD, 1, 12),
            new Trade(() -> Items.HONEYCOMB, 6, () -> Items.EMERALD, 1, 8),
            new Trade(() -> GnomeContent.GNOME_BAIT.get(), 2, () -> Items.EMERALD, 1, 8));
    private static final List<Trade> SELLS = List.of(
            new Trade(() -> Items.EMERALD, 1, () -> DreamRealmContent.PUFFBALL.get(), 3, 12),
            new Trade(() -> Items.EMERALD, 1, () -> DreamRealmContent.DREAMFLOWER.get(), 4, 12),
            new Trade(() -> Items.EMERALD, 2, () -> DreamRealmContent.PRISM_MUSHROOM.get(), 3, 8),
            new Trade(() -> Items.EMERALD, 2, () -> DreamRealmContent.FRACTAL_SAPLING.get(), 1, 6),
            new Trade(() -> Items.EMERALD, 3, () -> DreamRealmContent.BOUNCY_CLOUD.get(), 4, 8),
            new Trade(() -> Items.EMERALD, 1, () -> DreamRealmContent.DREAM_POPPY.get(), 4, 12),
            new Trade(() -> Items.EMERALD, 4, () -> GnomeContent.GNOME_STAFF.get(), 1, 2),
            new Trade(() -> Items.EMERALD, 6, () -> OreContent.RAW_MITHRIL.get(), 1, 4));

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();
        pick(offers, BUYS, 3);
        pick(offers, SELLS, 4);
    }

    private void pick(MerchantOffers offers, List<Trade> pool, int n) {
        List<Trade> list = new ArrayList<>(pool);
        for (int i = 0; i < n && !list.isEmpty(); i++) {
            offers.add(list.remove(this.random.nextInt(list.size())).offer());
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), 2 + this.random.nextInt(3)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public float getVoicePitch() {
        return 1.7F;
    }
}
