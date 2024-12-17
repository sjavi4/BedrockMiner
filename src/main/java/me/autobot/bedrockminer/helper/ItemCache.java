package me.autobot.bedrockminer.helper;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;

import static me.autobot.bedrockminer.helper.Consts.*;

public class ItemCache {
    public int totalCount;
    public final int leastCount;
    public final List<ItemStack> cachedItems = new ArrayList<>();

    private final Inventory inventory;
    private final Item item;
    public final int preferHotbarSlot;
    public final boolean optional;
    public boolean pass = true;
    public ItemCache(Inventory inventory, Item item, int leastCount, int preferHotbar, boolean optional) {
        totalCount = inventory.countItem(item);
        this.inventory = inventory;
        this.item = item;
        this.optional = optional;
        preferHotbarSlot = preferHotbar;
        this.leastCount = leastCount;
        for (ItemStack i : inventory.items) {
            if (i.is(item)) {
                cachedItems.add(i);
            }
        }
        if (!optional && (totalCount < leastCount || cachedItems.isEmpty())) {
            pass = false;
            return;
        }

        pickaxe();
        moveToHotbar();
    }
    public void updateStatus() {
        cachedItems.clear();
        for (ItemStack i : inventory.items) {
            if (i.is(item)) {
                cachedItems.add(i);
            }
        }
        totalCount = inventory.countItem(item);
        if (!optional && (cachedItems.isEmpty() || totalCount < leastCount)) {
            pass = false;
        }
    }
    public void moveToHotbar() {
        if (cachedItems.isEmpty()) {
            pass = false;
            return;
        }
        ItemStack itemStack = cachedItems.getFirst();
        int slot = inventory.items.indexOf(itemStack);

        gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot, preferHotbarSlot, ClickType.SWAP, player);
    }

    public void tryMoveToHotbar() {
        if (!inventory.getItem(preferHotbarSlot).is(item)) {
            moveToHotbar();
            updateStatus();
        }
    }
    public void switchTo() {
        inventory.setSelectedHotbarSlot(preferHotbarSlot);
        connection.send(new ServerboundSetCarriedItemPacket(inventory.selected));
    }

    public void restock() {
        if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
            return;
        }
        ItemStack hand = inventory.getItem(preferHotbarSlot);
        for (ItemStack i : cachedItems) {
            if (i == hand) {
                continue;
            }
            int button = i.getCount() + hand.getCount() <= hand.getMaxStackSize() ? 0 : 1;

            int index = inventory.items.indexOf(i);
            if (index < 9) {
                index = index + 36;
            }
            gameMode.handleInventoryMouseClick(player.containerMenu.containerId, index, button, ClickType.PICKUP, player);
            gameMode.handleInventoryMouseClick(player.containerMenu.containerId, preferHotbarSlot + 36, 0, ClickType.PICKUP, player);
            break;
        }
    }


    private void pickaxe() {
        if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
            int level = 0;
            for (ItemStack i : cachedItems) {
                for (Object2IntMap.Entry<Holder<Enchantment>> e : i.getEnchantments().entrySet()) {
                    if (e.getKey().is(Enchantments.EFFICIENCY)) {
                        level = e.getIntValue();
                        break;
                    }
                }
                cachedItems.clear();
                cachedItems.add(i);
                break;
            }
            if (level < 5) {
                pass = false;
            }
        }
    }
}
