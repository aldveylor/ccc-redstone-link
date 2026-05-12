package me.ryleu.cccredstonelink;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static me.ryleu.cccredstonelink.CCRedstoneLinkBridgeMod.MOD_ID;

public final class ModItems {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> REDSTONE_LINK_BRIDGE = ITEMS.register("redstone_link_bridge", () -> new BlockItem(ModBlocks.REDSTONE_LINK_BRIDGE.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

