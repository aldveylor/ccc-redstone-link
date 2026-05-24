package me.ryleu.cccredstonelink;

import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.item.ItemStack;

/**
 * CC:Tweaked peripheral for the Redstone Link Bridge block.
 *
 * <h2>Lua API</h2>
 *
 * <h3>getLinkSignal(freq1, freq2 [, color1 [, color2]])</h3>
 * <p>Returns the current signal strength (0–15) on the Create redstone-link
 * network identified by the two frequency items and their optional dye colors.
 *
 * <h3>sendLinkSignal(freq1, freq2, strength [, color1 [, color2]])</h3>
 * <p>Transmits a signal on the specified network. {@code strength} is clamped
 * to the range 0–15.
 *
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>freq1 / freq2</b> – Item registry IDs, e.g.
 *       {@code "minecraft:leather_chestplate"}. These match the items you
 *       would physically place in the two frequency slots of a Create
 *       Redstone Link.</li>
 *   <li><b>color1 / color2</b> – Optional 24-bit RGB integers (0x000000–0xFFFFFF).
 *       This is the same value stored in the {@code DYED_COLOR} component of a
 *       dyed leather-armor piece, so any color the cauldron-dyeing system
 *       can produce is valid here. Pass {@code nil} or omit to leave that slot
 *       uncolored. A colored frequency only connects to another link whose
 *       <em>same</em> slot carries the <em>same</em> RGB value, matching
 *       Create's own {@code (item, color)} network-key logic.</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>
 * local bridge = peripheral.find("redstone_link_bridge")
 *
 * -- Plain item frequencies (backward-compatible)
 * local s = bridge.getLinkSignal("minecraft:diamond", "minecraft:emerald")
 *
 * -- Dyed leather chestplate as a frequency, hex-literal RGB
 * bridge.sendLinkSignal(
 *     "minecraft:leather_chestplate",
 *     "minecraft:leather_helmet",
 *     15,
 *     0xFF3344,
 *     0x33AAFF)
 *
 * -- Only the first slot colored; second slot uncolored
 * local s2 = bridge.getLinkSignal(
 *     "minecraft:leather_chestplate",
 *     "minecraft:leather_helmet",
 *     0xFF3344)
 * </pre>
 */
public class RedstoneLinkBridgePeripheral implements IPeripheral {

    private final RedstoneLinkBridgeBlockEntity blockEntity;
    private final Set<IComputerAccess> computers;

    public RedstoneLinkBridgePeripheral(RedstoneLinkBridgeBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.computers = new java.util.concurrent.CopyOnWriteArraySet<>();
    }

    @Override
    public @NonNull String getType() {
        return "redstone_link_bridge";
    }

    @Override
    public void attach(IComputerAccess computer) {
        this.computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        this.computers.remove(computer);
    }

    // -------------------------------------------------------------------------
    // Lua functions
    // -------------------------------------------------------------------------

    private static ItemStack frequencySpecToItemStack(Object frequency) throws LuaException {
        if (frequency instanceof String s) {
            return RedstoneLinkBridgeBlockEntity.fromFrequencyId(s);
        } else if (frequency instanceof Map<?,?> stack) {
            String id;
            Integer color = null;
            try {
                if (!stack.containsKey("id")) {
                    throw new LuaException("Invalid frequency specification: missing required 'id' key");
                }
                id = (String) stack.get("id");
                stack.remove("id"); // avoid confusion with the item ID in the string case
                if (stack.containsKey("color")) {
                    color = ((Number) stack.get("color")).intValue();
                    stack.remove("color");
                }
            } catch (ClassCastException e) {
                throw new LuaException("Invalid frequency specification: expected string or {id=string, color=number} table");
            }
            if (!stack.isEmpty()) {
                throw new LuaException("Invalid frequency specification: unrecognized keys " + stack.keySet());
            }
            return RedstoneLinkBridgeBlockEntity.fromFrequencySpec(id, color);
        } else {
            throw new LuaException("Invalid frequency specification: expected string or {id=string, color=number} table");
        }
    }

    private static Map<String, Object> itemStackToLuaTable(ItemStack stack) {
        Map<String, Object> table = new java.util.HashMap<>();
        table.put("id", stack.getItem().toString());
        Integer color = RedstoneLinkBridgeBlockEntity.getDyeColorRgb(stack);
        if (color != -1) {
            table.put("color", color);
        }
        return table;
    }

    @LuaFunction(mainThread = true)
    public final int getLinkSignal(
        Object frequency1,
        Object frequency2
    ) throws LuaException {
        ItemStack first = frequencySpecToItemStack(frequency1);
        ItemStack last  = frequencySpecToItemStack(frequency2);
        return blockEntity.getLinkSignal(first, last);
    }

    @LuaFunction(mainThread = true)
    public final void sendLinkSignal(
        Object frequency1,
        Object frequency2,
        int strength
    ) throws LuaException {
        ItemStack first = frequencySpecToItemStack(frequency1);
        ItemStack last  = frequencySpecToItemStack(frequency2);
        blockEntity.sendLinkSignal(first, last, strength);
    }

    @LuaFunction(mainThread = true)
    public final void hookLinkSignal(
        Object frequency1,
        Object frequency2
    ) throws LuaException {
        ItemStack first = frequencySpecToItemStack(frequency1);
        ItemStack last  = frequencySpecToItemStack(frequency2);
        blockEntity.setLinkListener(first, last, (signal) -> {
            for (IComputerAccess computer : computers) {
                computer.queueEvent("redstone_link_signal_changed", itemStackToLuaTable(first), itemStackToLuaTable(last), signal);
            }
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // IPeripheral
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (!(other instanceof RedstoneLinkBridgePeripheral that)) return false;
        return this.blockEntity == that.blockEntity;
    }

    @Override
    public Object getTarget() {
        return blockEntity;
    }
}
