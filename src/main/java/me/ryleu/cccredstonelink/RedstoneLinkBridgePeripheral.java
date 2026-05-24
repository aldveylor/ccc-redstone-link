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
 * <h3>getLinkSignal(freq1, freq2)</h3>
 * <p>Returns the current signal strength (0–15) on the Create redstone-link
 * network identified by the two frequency items and their optional dye colors.
 * If you want to query the signal strength continuously, consider using {@code hookLinkSignal} 
 * instead to avoid busy-waiting in a loop.
 *
 * <h3>sendLinkSignal(freq1, freq2, strength)</h3>
 * <p>Transmits a signal on the specified network. {@code strength} is clamped
 * to the range 0–15.
 * 
 * <h3>hookLinkSignal(freq1, freq2)</h3>
 * <p>Registers a listener for changes in the signal strength on the specified
 * network. Whenever the signal strength changes, an event named
 * "redstone_link_signal_changed" will be queued on the attached ComputerCraft
 * computer(s) with the following arguments:
 * <ul>
 *  <li>{@code frequency1} (table) – the first frequency item as a Lua table with keys "id" and optional "color"</li>
 *  <li>{@code frequency2} (table) – the second frequency item as a Lua table with keys "id" and optional "color"</li>
 *  <li>{@code signal} (number) – the new signal strength (0–15)</li>
 * </ul>
 * <b>Note:</b> if frequency is an empty string, it will be parsed as "minecraft:air".
 *
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>freq1 / freq2</b> – Item registry IDs or a lua table describing the item, e.g.
 *       {@code "minecraft:leather_chestplate"}, {@code {id="minecraft:leather_helmet", color=0xFF3344}}.
 *       These match the items you would physically place in the two frequency slots of a Create
 *       Redstone Link.</li>
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
 *     {id="minecraft:leather_chestplate", color=0xFF3344},
 *     {id="minecraft:leather_helmet", color=0x33AAFF},
 *     15)
 *
 * -- Only the first slot colored; second slot uncolored
 * local s2 = bridge.getLinkSignal(
 *     {id="minecraft:leather_chestplate", color=0xFF3344},
 *     {id="minecraft:leather_helmet", color=0x33AAFF}
 * )
 * 
 * -- Reacting to signal changes with an event listener
 * bridge.hookLinkSignal(
 *     {id="minecraft:leather_chestplate", color=0xFF3344},
 *     {id="minecraft:leather_helmet", color=0x33AAFF}
 * )
 * while true do
 *     local event, freq1, freq2, signal = os.pullEvent("redstone_link_signal_changed")
 *     print(string.format("Signal on link %s/%s changed to %d", freq1.id, freq2.id, signal))
 * end
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

    @LuaFunction(mainThread = true)
    public final Map<Object, Integer> getLinkSignalBatch(Map<?, ?> frequencies) throws LuaException {
        // Object is a lua table {freqency1, frequency2}, as map or list
        Map<Object, Integer> output = new java.util.HashMap<>();
        int i = 0;
        for (Map.Entry<?, ?> entry : frequencies.entrySet()) {
            Object freq = entry.getValue();
            // Debug output
            if (!(freq instanceof Map<?,?> m) || !m.containsKey(1.0) || !m.containsKey(2.0)) {
                throw new LuaException("Invalid frequency pair specification: expected { [1]=frequency1, [2]=frequency2 }");
            }
            output.put(entry.getKey(), getLinkSignal(m.get(1.0), m.get(2.0)));
        }
        return output;
    }

    @LuaFunction(mainThread = true)
    public final void sendLinkSignalBatch(Map<?, ?> entries) throws LuaException {
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            Object entryObj = entry.getValue();
            if (!(entryObj instanceof Map<?,?> m) || !m.containsKey(1.0) || !m.containsKey(2.0) || !m.containsKey("strength")) {
                throw new LuaException("Invalid batch entry specification: expected { [1]=frequency1, [2]=frequency2, strength=number }");
            }
            sendLinkSignal(m.get(1.0), m.get(2.0), ((Number) m.get("strength")).intValue());
        }
    }

    @LuaFunction(mainThread = true)
    public final void hookLinkSignalBatch(Map<?, ?> entries) throws LuaException {
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            Object entryObj = entry.getValue();
            if (!(entryObj instanceof Map<?,?> m) || !m.containsKey(1.0) || !m.containsKey(2.0)) {
                throw new LuaException("Invalid batch entry specification: expected { [1]=frequency1, [2]=frequency2 }");
            }
            hookLinkSignal(m.get(1.0), m.get(2.0));
        }
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
