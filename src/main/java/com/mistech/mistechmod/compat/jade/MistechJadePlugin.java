package com.mistech.mistechmod.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.compat.InfoProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Integração com o Jade: qualquer block entity do Mistech que implementa {@link InfoProvider} mostra suas linhas
 * (progresso, tanque, redstone, melhorias...). Energia e itens o Jade já mostra sozinho pelas capabilities.
 * As linhas são montadas no servidor e chegam como componentes de texto. Só carregada se o Jade estiver instalado.
 */
@WailaPlugin
public class MistechJadePlugin implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath(Mistech.MODID, "info");
    private static final String KEY = "mistech_info";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, Block.class);
    }

    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof InfoProvider provider)) {
                return;
            }
            List<Component> lines = new ArrayList<>();
            provider.addInfo(lines);
            if (!lines.isEmpty()) {
                data.store(KEY, ComponentSerialization.CODEC.listOf(),
                        accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), lines);
            }
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof InfoProvider;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains(KEY)) {
                return;
            }
            accessor.getServerData().read(KEY, ComponentSerialization.CODEC.listOf(),
                    accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE)).ifPresent(tooltip::addAll);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
