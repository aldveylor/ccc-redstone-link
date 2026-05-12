package me.ryleu.cccredstonelink;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static me.ryleu.cccredstonelink.CCRedstoneLinkBridgeMod.MOD_ID;

public final class ModBlockEntities {
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneLinkBridgeBlockEntity>> REDSTONE_LINK_BRIDGE = BLOCK_ENTITY_TYPES.register("redstone_link_bridge", () -> BlockEntityType.Builder.of(RedstoneLinkBridgeBlockEntity::new, ModBlocks.REDSTONE_LINK_BRIDGE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}

