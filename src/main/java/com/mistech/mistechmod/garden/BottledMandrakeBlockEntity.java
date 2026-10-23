package com.mistech.mistechmod.garden;

import com.mistech.mistechmod.machine.GnomeBoost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Mandrágora Engarrafada: guarda quanto tempo de "adubo" ela ainda tem.
 * Farinha de Osso = 2 min, Bloco de Osso = 18 min, máximo 30 min. Enquanto tem, a cada segundo cada planta num raio de
 * {@link #RADIUS} blocos (de 2 abaixo a 1 acima do pote) tem 1/{@link #CHANCE} de receber um tick de crescimento extra
 * (~3× mais rápido). Com um Gnomo Engarrafado alimentado por perto ela canta mais forte (1/{@link #GNOME_CHANCE}).
 * É um Container de 1 espaço só para funis colocarem farinha de osso (nada fica guardado).
 */
public class BottledMandrakeBlockEntity extends BlockEntity implements Container {
    public static final int BONE_MEAL_TICKS = 2 * 60 * 20;
    public static final int BONE_BLOCK_TICKS = 9 * BONE_MEAL_TICKS;
    public static final int MAX_TICKS = 30 * 60 * 20;
    public static final int RADIUS = 4;
    public static final int CHANCE = 30;
    public static final int GNOME_CHANCE = 20;

    private int fedTicks;

    public BottledMandrakeBlockEntity(BlockPos pos, BlockState state) {
        super(GardenContent.BOTTLED_MANDRAKE_BE.get(), pos, state);
    }

    /** Quanto tempo (ticks) o adubo dá; 0 se ela não come isso. */
    public static int foodTicks(ItemStack stack) {
        if (stack.is(Items.BONE_MEAL)) {
            return BONE_MEAL_TICKS;
        }
        if (stack.is(Items.BONE_BLOCK)) {
            return BONE_BLOCK_TICKS;
        }
        return 0;
    }

    public boolean canEat(ItemStack stack) {
        int ticks = foodTicks(stack);
        if (ticks <= 0) {
            return false;
        }
        // o bloco de osso entra se ainda não estiver cheia (o que sobra se perde)
        return ticks >= BONE_BLOCK_TICKS ? this.fedTicks < MAX_TICKS : this.fedTicks + ticks <= MAX_TICKS;
    }

    public void feed(ItemStack stack) {
        this.fedTicks = Math.min(MAX_TICKS, this.fedTicks + foodTicks(stack));
        this.setChanged();
        if (this.level instanceof ServerLevel server) {
            BlockPos pos = this.worldPosition;
            server.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.7F, 0.8F);
            server.playSound(null, pos, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.BLOCKS, 0.6F, 0.6F);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.0);
            this.updateState();
        }
    }

    public Component statusMessage() {
        if (this.fedTicks <= 0) {
            return Component.translatable("message.mistech.bottled_mandrake.hungry");
        }
        int minutes = (this.fedTicks + 1199) / 1200;
        return Component.translatable("message.mistech.bottled_mandrake.fed", minutes);
    }

    public int comparatorSignal() {
        return this.fedTicks <= 0 ? 0 : 1 + Math.round(14.0F * this.fedTicks / MAX_TICKS);
    }

    /** O que a canção da mandrágora faz crescer: plantações, mudas, cana, cacto, fungo do Nether, bagas, caules... */
    public static boolean isPlant(BlockState state) {
        return state.isRandomlyTicking() && !state.is(Blocks.GRASS_BLOCK) && !state.is(BlockTags.NYLIUM)
                && (state.getBlock() instanceof BonemealableBlock || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)
                        || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.NETHER_WART) || state.is(Blocks.BAMBOO)
                        || state.is(Blocks.KELP));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BottledMandrakeBlockEntity mandrake) {
        if (mandrake.fedTicks > 0) {
            mandrake.fedTicks--;
            if (mandrake.fedTicks % 200 == 0) {
                mandrake.setChanged();
            }
            if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
                sing(server, pos);
            }
        }
        mandrake.updateState();
    }

    /** Um "verso" da canção: ticks extras nas plantas em volta. */
    private static void sing(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        int chance = GnomeBoost.extraStep(level, pos) ? GNOME_CHANCE : CHANCE;
        int r = RADIUS;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 1, r))) {
            if (random.nextInt(chance) != 0) {
                continue;
            }
            BlockState plant = level.getBlockState(target);
            if (!isPlant(plant)) {
                continue;
            }
            plant.randomTick(level, target, random);
            if (random.nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                        2, 0.25, 0.2, 0.25, 0.0);
            }
        }
        // filhotes de animais em volta crescem mais rápido (+3 s de idade por verso, ~4× mais rápido)
        for (net.minecraft.world.entity.AgeableMob baby : level.getEntitiesOfClass(net.minecraft.world.entity.AgeableMob.class,
                new net.minecraft.world.phys.AABB(pos).inflate(r, 2, r), m -> m.isBaby() && m.isAlive())) {
            baby.ageUp(3);
            if (random.nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, baby.getX(), baby.getY() + baby.getBbHeight(), baby.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        }
        if (random.nextInt(12) == 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 0.5F + random.nextFloat() * 0.3F);
        }
    }

    /** Liga/desliga a aparência (FED) conforme o adubo. */
    private void updateState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        boolean fed = this.fedTicks > 0;
        BlockState state = this.getBlockState();
        if (state.hasProperty(BottledMandrakeBlock.FED) && state.getValue(BottledMandrakeBlock.FED) != fed) {
            this.level.setBlock(this.worldPosition, state.setValue(BottledMandrakeBlock.FED, fed), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fedTicks = input.getIntOr("FedTicks", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("FedTicks", this.fedTicks);
    }

    // ----- Container: funil coloca farinha de osso e ela come -----

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        for (int i = 0; i < stack.getCount() && this.canEat(stack); i++) {
            this.feed(stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.canEat(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
    }
}
