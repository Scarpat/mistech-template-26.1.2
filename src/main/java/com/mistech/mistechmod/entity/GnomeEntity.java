package com.mistech.mistechmod.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.staff.GnomeStaffItem;
import com.mistech.mistechmod.registry.GnomeContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Gnomo: aparece quando a Isca de Gnomo queima numa fogueira. Selvagem, é neutro: passeia e observa,
 * mas se apanhar revida com o cajado. Ao morrer derruba o Cajado do Gnomo e o Chapéu de Gnomo vermelho.
 *
 * <p><b>Familiar</b>: dar uma Maçã Dourada a um gnomo selvagem tem 1/3 de chance de domá-lo (máx. 3 por jogador,
 * +1 com o Amuleto do Gnomo). O familiar segue o dono (teleporta se ficar longe); agachado + mão vazia = fica/segue
 * (ficar marca o centro do trabalho, raio 8). Come maçãs (botão direito com maçã guarda até 16); cada maçã dá 5 min
 * de trabalho. Sem comida fica "preguiçoso": só segue. A profissão vem do item dado a ele (botão direito com o item;
 * mão vazia devolve):
 * nada = Companheiro (Sorte para o dono), enxada = Jardineiro, baú = Coletor, espada = Guarda, Cajado do Gnomo = Mago.
 */
public class GnomeEntity extends PathfinderMob {
    public static final int MAX_FAMILIARS = 3;
    public static final int WORK_RADIUS = 8;
    public static final int APPLE_WORK_TICKS = 5 * 60 * 20;
    public static final int MAX_APPLES = 16;

    public enum Profession { COMPANION, GARDENER, COLLECTOR, GUARD, MAGE }

    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(GnomeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAYING = SynchedEntityData.defineId(GnomeEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID owner;
    private @Nullable BlockPos workCenter;
    private int apples;
    private int workTicks;
    private ItemStack pouch = ItemStack.EMPTY; // o que o Coletor está carregando
    private int castCooldown;

    public GnomeEntity(EntityType<? extends GnomeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAMED, false);
        builder.define(STAYING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MageGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return GnomeEntity.this.profession() != Profession.MAGE && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new WorkGoal(this));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !GnomeEntity.this.isTamed() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                LivingEntity attacker = GnomeEntity.this.getLastHurtByMob();
                return !(attacker != null && attacker.getUUID().equals(GnomeEntity.this.owner)) && super.canUse();
            }
        });
        // Guarda e Mago defendem o dono de criaturas hostis
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target, level) -> target instanceof Enemy && this.isDefender() && this.hasFood() && this.nearOwner(16)));
    }

    // ----- estado -----

    public boolean isTamed() {
        return this.entityData.get(TAMED);
    }

    public boolean isStaying() {
        return this.entityData.get(STAYING);
    }

    public @Nullable Player getOwnerPlayer() {
        return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
    }

    public boolean isOwnedBy(Player player) {
        return this.owner != null && this.owner.equals(player.getUUID());
    }

    public Profession profession() {
        ItemStack held = this.getMainHandItem();
        if (held.isEmpty()) {
            return Profession.COMPANION;
        }
        if (held.is(ItemTags.HOES)) {
            return Profession.GARDENER;
        }
        if (held.is(Items.CHEST)) {
            return Profession.COLLECTOR;
        }
        if (held.is(GnomeContent.GNOME_STAFF.get())) {
            return Profession.MAGE;
        }
        if (held.is(ItemTags.SWORDS)) {
            return Profession.GUARD;
        }
        return Profession.COMPANION;
    }

    private boolean isDefender() {
        Profession p = this.profession();
        return this.isTamed() && (p == Profession.GUARD || p == Profession.MAGE);
    }

    private boolean hasFood() {
        return this.workTicks > 0 || this.apples > 0;
    }

    private boolean nearOwner(double range) {
        Player owner = this.getOwnerPlayer();
        return owner != null && owner.distanceToSqr(this) <= range * range;
    }

    /** Gasta tempo de trabalho; come uma maçã quando acaba. @return false se está sem comida. */
    private boolean spendWork(int ticks) {
        if (this.workTicks <= 0 && this.apples > 0) {
            this.apples--;
            this.workTicks = APPLE_WORK_TICKS;
            this.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.6F);
        }
        if (this.workTicks <= 0) {
            return false;
        }
        this.workTicks -= ticks;
        return true;
    }

    private BlockPos center() {
        if (this.isStaying() && this.workCenter != null) {
            return this.workCenter;
        }
        Player owner = this.getOwnerPlayer();
        return owner != null ? owner.blockPosition() : this.blockPosition();
    }

    // ----- domar e interagir -----

    public static int familiarCount(ServerLevel level, UUID owner) {
        int count = 0;
        for (GnomeEntity gnome : level.getEntities(GnomeContent.GNOME.get(), g -> owner.equals(g.owner))) {
            count++;
        }
        return count;
    }

    public static int familiarLimit(Player player) {
        return MAX_FAMILIARS + (Accessories.has(player, Accessory.GNOME_AMULET) ? 1 : 0);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // O cliente não sabe quem é o dono. Se respondesse PASS aqui, o jogo repetiria o clique com a outra mão
        // e o servidor receberia 2 cliques (ex.: entrega a ferramenta e, com a mão vazia, devolve na hora).
        if (!(this.level() instanceof ServerLevel server)) {
            return this.isTamed() || held.is(Items.GOLDEN_APPLE) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (this.isTamed() && hand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS; // familiar só responde à mão principal
        }

        if (!this.isTamed()) {
            if (!held.is(Items.GOLDEN_APPLE)) {
                return InteractionResult.PASS;
            }
            if (familiarCount(server, player.getUUID()) >= familiarLimit(player)) {
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.limit", familiarLimit(player)));
                return InteractionResult.FAIL;
            }
            held.consume(1, player);
            if (this.random.nextInt(3) == 0) {
                this.tame(player);
                server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.8, this.getZ(), 7, 0.3, 0.3, 0.3, 0.0);
                this.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0F, 1.7F);
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.tamed"));
            } else {
                server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 7, 0.3, 0.3, 0.3, 0.0);
                this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.7F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.isOwnedBy(player)) {
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.APPLE)) {
            int take = Math.min(held.getCount(), MAX_APPLES - this.apples);
            if (take > 0) {
                this.apples += take;
                held.consume(take, player);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.6F);
            }
            player.sendOverlayMessage(this.status());
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                boolean stay = !this.isStaying();
                this.entityData.set(STAYING, stay);
                this.workCenter = stay ? this.blockPosition() : null;
                this.getNavigation().stop();
                player.sendOverlayMessage(Component.translatable(stay ? "message.mistech.familiar.stay" : "message.mistech.familiar.follow"));
                return InteractionResult.SUCCESS;
            }
            ItemStack tool = this.getMainHandItem();
            if (!tool.isEmpty()) {
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                player.setItemInHand(hand, tool);
                this.dropPouch(server);
            }
            player.sendOverlayMessage(this.status());
            return InteractionResult.SUCCESS;
        }
        // qualquer outro item: vira a ferramenta do familiar (devolve a antiga)
        ItemStack old = this.getMainHandItem();
        this.setItemSlot(EquipmentSlot.MAINHAND, held.split(1));
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        if (!old.isEmpty()) {
            player.getInventory().placeItemBackInInventory(old);
        }
        this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
        player.sendOverlayMessage(this.status());
        return InteractionResult.SUCCESS;
    }

    private void tame(Player player) {
        this.owner = player.getUUID();
        this.entityData.set(TAMED, true);
        this.setPersistenceRequired();
        this.setTarget(null);
        this.getNavigation().stop();
        // a ferramenta do gnomo selvagem não conta: começa de mãos vazias (Companheiro)
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public Component status() {
        Component job = Component.translatable("familiar.mistech.profession." + this.profession().name().toLowerCase(java.util.Locale.ROOT));
        int minutes = (this.workTicks + 1199) / 1200 + this.apples * 5;
        return this.hasFood()
                ? Component.translatable("message.mistech.familiar.status", job, minutes)
                : Component.translatable("message.mistech.familiar.lazy", job);
    }

    // ----- tick -----

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isTamed() || this.tickCount % 20 != 0) {
            return;
        }
        Player owner = this.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        if (this.castCooldown > 0) {
            this.castCooldown -= 20;
        }
        // Companheiro: Sorte para o dono
        if (this.profession() == Profession.COMPANION && this.nearOwner(8) && this.hasFood()) {
            this.spendWork(20);
            owner.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
        }
        // Amuleto do Gnomo: familiares perto curam o dono (1 coração a cada 10 s)
        if (this.tickCount % 200 == 0 && this.nearOwner(8) && Accessories.has(owner, Accessory.GNOME_AMULET)
                && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(2.0F);
            server.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2.0, owner.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
    }

    // ----- Mago -----

    private void castAt(ServerLevel level, LivingEntity target) {
        ItemStack staff = this.getMainHandItem();
        RuneElement rune = GnomeStaffItem.getSelected(staff);
        int cost = rune == RuneElement.ENERGY ? 2_000 : 500;
        if (GnomeStaffItem.getEnergy(staff) < cost || !this.spendWork(40)) {
            return;
        }
        GnomeStaffItem.setEnergy(staff, GnomeStaffItem.getEnergy(staff) - cost);
        this.swing(InteractionHand.MAIN_HAND);
        Vec3 from = this.getEyePosition();
        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        if (rune == RuneElement.FIRE) {
            SmallFireball ball = new SmallFireball(level, this, dir.scale(0.8));
            ball.setPos(from.add(dir.scale(0.6)));
            level.addFreshEntity(ball);
            this.playSound(SoundEvents.BLAZE_SHOOT, 0.7F, 1.5F);
            this.castCooldown = 40;
            return;
        }
        if (rune == RuneElement.ENERGY) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(false);
                level.addFreshEntity(bolt);
            }
            this.castCooldown = 120;
            return;
        }
        float damage = 4.0F;
        if (rune == RuneElement.ICE) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
            damage = 3.0F;
        } else if (rune == RuneElement.LIGHT) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            damage = target.isInvertedHealAndHarm() ? 7.0F : 5.0F;
        }
        target.hurtServer(level, this.damageSources().indirectMagic(this, this.getOwnerPlayer() != null ? this.getOwnerPlayer() : this), damage);
        for (int i = 1; i <= 8; i++) {
            Vec3 p = from.add(target.getEyePosition().subtract(from).scale(i / 8.0));
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.6F, 1.6F);
        this.castCooldown = 40;
    }

    // ----- Jardineiro e Coletor -----

    /** Procura uma plantação madura perto do centro. */
    private @Nullable BlockPos findRipeCrop(ServerLevel level) {
        BlockPos center = this.center();
        for (int i = 0; i < 24; i++) {
            BlockPos pos = center.offset(this.random.nextInt(WORK_RADIUS * 2 + 1) - WORK_RADIUS, this.random.nextInt(3) - 1,
                    this.random.nextInt(WORK_RADIUS * 2 + 1) - WORK_RADIUS);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                return pos;
            }
        }
        return null;
    }

    private void harvest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, this, this.getMainHandItem());
        level.destroyBlock(pos, false, this);
        // replanta com uma das sementes colhidas
        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!replanted && drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == crop) {
                drop.shrink(1);
                level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
                replanted = true;
            }
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        this.swing(InteractionHand.MAIN_HAND);
    }

    private @Nullable ItemEntity findLooseItem(ServerLevel level) {
        BlockPos c = this.center();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(WORK_RADIUS),
                item -> item.isAlive() && !item.hasPickUpDelay());
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double d = item.distanceToSqr(this);
            if (d < bestDist) {
                best = item;
                bestDist = d;
            }
        }
        return best;
    }

    private @Nullable BlockPos findStorage(ServerLevel level) {
        BlockPos c = this.center();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-WORK_RADIUS, -2, -WORK_RADIUS), c.offset(WORK_RADIUS, 2, WORK_RADIUS))) {
            if (level.getBlockEntity(pos) != null && level.getCapability(Capabilities.Item.BLOCK, pos, null) != null) {
                double d = pos.distSqr(this.blockPosition());
                if (d < bestDist) {
                    best = pos.immutable();
                    bestDist = d;
                }
            }
        }
        return best;
    }

    private boolean store(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, net.minecraft.core.Direction.UP);
        if (handler == null || this.pouch.isEmpty()) {
            return false;
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(this.pouch), this.pouch.getCount(), tx);
            tx.commit();
        }
        this.pouch.shrink(inserted);
        return inserted > 0;
    }

    private void dropPouch(ServerLevel level) {
        if (!this.pouch.isEmpty()) {
            this.spawnAtLocation(level, this.pouch);
            this.pouch = ItemStack.EMPTY;
        }
    }

    // ----- salvar -----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.owner != null) {
            output.store("Owner", UUIDUtil.CODEC, this.owner);
        }
        output.putBoolean("Staying", this.isStaying());
        if (this.workCenter != null) {
            output.store("WorkCenter", BlockPos.CODEC, this.workCenter);
        }
        output.putInt("Apples", this.apples);
        output.putInt("WorkTicks", this.workTicks);
        if (!this.pouch.isEmpty()) {
            output.store("Pouch", ItemStack.OPTIONAL_CODEC, this.pouch);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(TAMED, this.owner != null);
        this.entityData.set(STAYING, input.getBooleanOr("Staying", false));
        this.workCenter = input.read("WorkCenter", BlockPos.CODEC).orElse(null);
        this.apples = input.getIntOr("Apples", 0);
        this.workTicks = input.getIntOr("WorkTicks", 0);
        this.pouch = input.read("Pouch", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.dropPouch(level);
        if (this.apples > 0) {
            this.spawnAtLocation(level, new ItemStack(Items.APPLE, this.apples));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.isTamed() && super.removeWhenFarAway(distance);
    }

    // voz de gnomo: sons de aldeão bem mais agudos
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.7F;
    }

    // ================= objetivos =================

    /** Segue o dono; teleporta para perto se ficar a mais de 16 blocos. */
    static class FollowOwnerGoal extends Goal {
        private final GnomeEntity gnome;
        private int repath;

        FollowOwnerGoal(GnomeEntity gnome) {
            this.gnome = gnome;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = this.gnome.getOwnerPlayer();
            return this.gnome.isTamed() && !this.gnome.isStaying() && owner != null && !owner.isSpectator()
                    && this.gnome.distanceToSqr(owner) > 16.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = this.gnome.getOwnerPlayer();
            return owner != null && !this.gnome.isStaying() && this.gnome.distanceToSqr(owner) > 5.0;
        }

        @Override
        public void stop() {
            this.gnome.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player owner = this.gnome.getOwnerPlayer();
            if (owner == null) {
                return;
            }
            this.gnome.getLookControl().setLookAt(owner, 10.0F, this.gnome.getMaxHeadXRot());
            if (--this.repath > 0) {
                return;
            }
            this.repath = 10;
            if (this.gnome.distanceToSqr(owner) > 16.0 * 16.0) {
                BlockPos near = owner.blockPosition().offset(this.gnome.random.nextInt(5) - 2, 0, this.gnome.random.nextInt(5) - 2);
                if (this.gnome.level().getBlockState(near).isAir() && this.gnome.level().getBlockState(near.below()).isSolid()) {
                    this.gnome.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
                    this.gnome.getNavigation().stop();
                    return;
                }
            }
            this.gnome.getNavigation().moveTo(owner, 1.2);
        }
    }

    /** Mago: lança magias do cajado nos inimigos. */
    static class MageGoal extends Goal {
        private final GnomeEntity gnome;

        MageGoal(GnomeEntity gnome) {
            this.gnome = gnome;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.gnome.getTarget();
            return this.gnome.isTamed() && this.gnome.profession() == Profession.MAGE && target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.gnome.getTarget();
            if (target == null) {
                return;
            }
            this.gnome.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = this.gnome.distanceToSqr(target);
            if (dist > 10.0 * 10.0 || !this.gnome.getSensing().hasLineOfSight(target)) {
                this.gnome.getNavigation().moveTo(target, 1.1);
            } else {
                this.gnome.getNavigation().stop();
                if (this.gnome.castCooldown <= 0 && this.gnome.level() instanceof ServerLevel level) {
                    this.gnome.castAt(level, target);
                    if (this.gnome.castCooldown <= 0) {
                        this.gnome.castCooldown = 40; // sem energia: tenta de novo depois
                    }
                }
            }
        }
    }

    /** Jardineiro e Coletor. */
    static class WorkGoal extends Goal {
        private final GnomeEntity gnome;
        private @Nullable BlockPos cropTarget;
        private @Nullable ItemEntity itemTarget;
        private @Nullable BlockPos storage;
        private int searchDelay;

        WorkGoal(GnomeEntity gnome) {
            this.gnome = gnome;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.gnome.isTamed() || !this.gnome.hasFood() || --this.searchDelay > 0
                    || !(this.gnome.level() instanceof ServerLevel level)) {
                return false;
            }
            this.searchDelay = 20;
            Profession p = this.gnome.profession();
            if (p == Profession.GARDENER) {
                this.cropTarget = this.gnome.findRipeCrop(level);
                return this.cropTarget != null;
            }
            if (p == Profession.COLLECTOR) {
                if (!this.gnome.pouch.isEmpty()) {
                    this.storage = this.gnome.findStorage(level);
                    return this.storage != null;
                }
                this.itemTarget = this.gnome.findLooseItem(level);
                return this.itemTarget != null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.cropTarget != null || (this.itemTarget != null && this.itemTarget.isAlive()) || this.storage != null;
        }

        @Override
        public void stop() {
            this.cropTarget = null;
            this.itemTarget = null;
            this.storage = null;
        }

        @Override
        public void tick() {
            if (!(this.gnome.level() instanceof ServerLevel level)) {
                return;
            }
            Vec3 goal = this.cropTarget != null ? Vec3.atCenterOf(this.cropTarget)
                    : this.storage != null ? Vec3.atCenterOf(this.storage)
                    : this.itemTarget != null ? this.itemTarget.position() : null;
            if (goal == null) {
                return;
            }
            if (this.gnome.distanceToSqr(goal) > 2.2 * 2.2) {
                if (this.gnome.getNavigation().isDone()) {
                    this.gnome.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0);
                }
                return;
            }
            this.gnome.getNavigation().stop();
            if (this.cropTarget != null) {
                if (this.gnome.spendWork(100)) {
                    this.gnome.harvest(level, this.cropTarget);
                }
                this.cropTarget = null;
            } else if (this.storage != null) {
                if (!this.gnome.store(level, this.storage)) {
                    this.gnome.dropPouch(level); // baú cheio: deixa no chão
                }
                this.storage = null;
            } else if (this.itemTarget != null) {
                ItemStack stack = this.itemTarget.getItem();
                if (this.gnome.pouch.isEmpty() && this.gnome.spendWork(40)) {
                    this.gnome.pouch = stack.copy();
                    this.itemTarget.discard();
                    this.gnome.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.4F);
                }
                this.itemTarget = null;
            }
        }
    }
}
