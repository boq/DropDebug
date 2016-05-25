package openmods.dropdebug;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.IEventListener;

public class EventStateTracker {

    private boolean isTracking;

    private int trackedBusId;

    private boolean conflict;

    private boolean isEventCancelled;

    private final Map<EntityItem, ItemStack> lastDrops = Maps.newIdentityHashMap();

    public synchronized void startTracking(int busId, PlayerDropsEvent event) {
        if (isTracking) {
            Log.warn("Already tracking on bus %d", busId);
            conflict = true;
        } else {
            conflict = false;
        }
        isTracking = true;
        isEventCancelled = false;

        trackedBusId = busId;

        Log.info("Started tracking event %s (%X) on bus %d", event, System.identityHashCode(event), busId);
        Log.info("Player: %s", event.entityPlayer);
        Log.info("Source: %s", event.source.func_151519_b(event.entityPlayer).getUnformattedText());
        Log.info("Looting: %s", event.lootingLevel);
        Log.info("RecentlyHit: %s", event.recentlyHit);
        Log.info("SpecialDropValue: %s", event.specialDropValue);

        Log.info("Initial drops: ");
        printDrops(event.drops);

        lastDrops.clear();
        storeNewDrops(event.drops);
    }

    private void storeNewDrops(final Iterable<EntityItem> drops) {
        for (EntityItem item : drops) {
            final ItemStack stack = item.getEntityItem();
            lastDrops.put(item, stack != null ? stack.copy() : null);
        }
    }

    private static void printDrops(final Iterable<EntityItem> drops) {
        int i = 0;
        for (EntityItem e : drops)
            Log.info("\tDrop %d: %d(%s) -> %s", i++, e.getEntityId(), e.getClass(), e.getEntityItem());
    }

    private void checkBusId(int busId) {
        if (busId != trackedBusId) {
            Log.warn("Bus changed from %s to %s", trackedBusId, busId);
            trackedBusId = busId;
            conflict = true;
        }
    }

    private void checkIsTracking(int busId) {
        if (!isTracking) {
            Log.warn("Not tracking, but 'phased' called (bus %d)", busId);
            conflict = true;
        }
    }

    public synchronized void setEventPhase(int busId, EventPriority priority, PlayerDropsEvent event) {
        checkIsTracking(busId);
        checkBusId(busId);

        Log.info("Event %s on bus %d phase changed to %s", event, busId, priority);
    }

    public synchronized void trackEvent(int busId, IEventListener listener, PlayerDropsEvent event) {
        Log.info("Called listener %s on event %s, bus %d", listener, event, busId);
        checkIsTracking(busId);
        checkBusId(busId);

        if (isEventCancelled != event.isCanceled()) {
            Log.info("Event cancelled state changed: %s -> %s", isEventCancelled, event.isCanceled());
            isEventCancelled = event.isCanceled();
        }

        Set<EntityItem> currentDrops = Sets.newIdentityHashSet();
        currentDrops.addAll(event.drops);

        List<EntityItem> newDrops = ImmutableList.copyOf(Sets.difference(currentDrops, lastDrops.keySet()));
        List<EntityItem> removedDrops = ImmutableList.copyOf(Sets.difference(lastDrops.keySet(), currentDrops));
        List<EntityItem> commonsDrops = ImmutableList.copyOf(Sets.intersection(lastDrops.keySet(), currentDrops));

        if (!newDrops.isEmpty()) {
            Log.info("New drops:");
            printDrops(newDrops);
            storeNewDrops(newDrops);
        }

        if (!removedDrops.isEmpty()) {
            Log.info("Removed drops:");
            printDrops(removedDrops);
            for (EntityItem removedDrop : removedDrops)
                lastDrops.remove(removedDrop);
        }

        for (EntityItem drop : commonsDrops) {
            final ItemStack newStack = drop.getEntityItem();
            final ItemStack oldStack = lastDrops.get(drop);
            if (!ItemStack.areItemStacksEqual(newStack, oldStack)) {
                Log.info("Item on entity %d changed, '%s' -> '%s'", drop.getEntityId(), printStack(oldStack), printStack(newStack));
                lastDrops.put(drop, newStack != null ? newStack.copy() : null);
            }
        }
    }

    private static String printStack(ItemStack stack) {
        if (stack == null)
            return "<null>";

        final NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        return tag.toString();
    }

    public synchronized void stopTracking(int busId, PlayerDropsEvent event) {
        checkIsTracking(busId);
        checkBusId(busId);

        if (conflict)
            Log.severe("Concurrency problem, results may not be correct. Try again");

        this.isTracking = false;

        Log.info("Final drops: ");
        printDrops(event.drops);

        if (event.isCanceled())
            Log.info("Event cancelled!");

        lastDrops.clear();
        Log.info("Stopped tracking event %s (%d) on bus %d", event, System.identityHashCode(event), busId);
    }

}
