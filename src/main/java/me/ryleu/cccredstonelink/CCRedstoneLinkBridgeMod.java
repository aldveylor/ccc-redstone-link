package me.ryleu.cccredstonelink;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(value="cccredstonelink")
public class CCRedstoneLinkBridgeMod {
    public static final String MOD_ID = "cccredstonelink";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CCRedstoneLinkBridgeMod(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        modBus.addListener(this::addCreativeTabs);
        modBus.addListener(this::registerCapabilities);
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModItems.REDSTONE_LINK_BRIDGE.get());
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModBlockEntities.REDSTONE_LINK_BRIDGE.get(),
                (blockEntity, side) -> new RedstoneLinkBridgePeripheral(blockEntity)
        );
    }
}

