package com.mistech.mistechmod.entity;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.registry.AccessoryContent;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechWear;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * Rei Gnomo: fica no trono da casa no centro da Dimensão dos Gnomos. Não anda, não apanha.
 * Clique = trocas (tela de troca de aldeão). Agachado + clique = pede para voltar para casa.
 * As trocas ficam todas em {@link #buildOffers()}.
 */
public class GnomeKingEntity extends PathfinderMob implements Merchant {
    private @Nullable Player tradingPlayer;
    private @Nullable MerchantOffers offers;

    public GnomeKingEntity(EntityType<? extends GnomeKingEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
    }

    /** As trocas do Rei. Moedas dos gnomos: maçãs, ouro e o que se minera na mina deles. */
    protected MerchantOffers buildOffers() {
        MerchantOffers list = new MerchantOffers();
        // Gnomo Engarrafado (um ajudante pronto): 16 maçãs + 1 Pote de Vidro
        list.add(new MerchantOffer(new ItemCost(Items.APPLE, 16), Optional.of(new ItemCost(Mistech.GLASS_JAR_ITEM.get(), 1)),
                new ItemStack(GnomeContent.BOTTLED_GNOME_ITEM.get()), 6, 10, 0.05F));
        // Cajado do Gnomo: 8 barras de ouro + 1 Runa de Energia
        list.add(new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 8), Optional.of(new ItemCost(Mistech.ENERGY_RUNE.get(), 1)),
                new ItemStack(GnomeContent.GNOME_STAFF.get()), 1, 30, 0.05F));
        // Chapéu de Gnomo: 24 pepitas de ouro
        list.add(new MerchantOffer(new ItemCost(Items.GOLD_NUGGET, 24), new ItemStack(MistechWear.GNOME_HAT.get()), 3, 10, 0.05F));
        // Amuleto do Gnomo: 4 maçãs douradas
        list.add(new MerchantOffer(new ItemCost(Items.GOLDEN_APPLE, 4), new ItemStack(AccessoryContent.GNOME_AMULET.get()), 1, 30, 0.05F));
        // Minérios da mina: 24 ferro bruto -> 2 diamantes; 16 ouro bruto -> 1 esmeralda... de gnomo: 3 lápis-lazúli por cobre
        list.add(new MerchantOffer(new ItemCost(Items.RAW_IRON, 24), new ItemStack(Items.DIAMOND, 2), 8, 15, 0.05F));
        list.add(new MerchantOffer(new ItemCost(Items.RAW_COPPER, 16), new ItemStack(Items.LAPIS_LAZULI, 12), 12, 5, 0.05F));
        // Magia: 6 Resíduos Mágicos -> 1 Fragmento de Magia Impura
        list.add(new MerchantOffer(new ItemCost(Mistech.MAGIC_RESIDUE.get(), 6), new ItemStack(Mistech.IMPURE_MAGIC_SHARD.get()), 8, 15, 0.05F));
        // Sementes raras: 8 Cogumelos Amarelados -> 4 sementes de café e 4 de beladona
        list.add(new MerchantOffer(new ItemCost(GnomeContent.YELLOW_MUSHROOM_ITEM.get(), 8), new ItemStack(Mistech.COFFEE_SEEDS.get(), 4), 8, 5, 0.05F));
        list.add(new MerchantOffer(new ItemCost(GnomeContent.YELLOW_MUSHROOM_ITEM.get(), 8), new ItemStack(Mistech.BELLADONNA_SEEDS.get(), 4), 8, 5, 0.05F));
        return list;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.tradingPlayer != null) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mistech.gnome_king.farewell"));
                this.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0F, 0.7F);
                GnomeRealm.sendHome(serverPlayer);
            } else {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        // parado no trono
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        // o estoque do Rei volta a cada meio dia
        if (!this.level().isClientSide() && this.offers != null && this.tickCount % 12000 == 0) {
            this.offers.forEach(MerchantOffer::resetUses);
        }
        if (this.tradingPlayer != null && (!this.tradingPlayer.isAlive() || this.tradingPlayer.distanceToSqr(this) > 64.0)) {
            this.tradingPlayer = null;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ----- Merchant -----

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = this.buildOffers();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.8F);
    }

    @Override
    public void notifyTradeUpdated(ItemStack itemStack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tradingPlayer == player && this.isAlive() && player.distanceToSqr(this) <= 64.0;
    }
}
