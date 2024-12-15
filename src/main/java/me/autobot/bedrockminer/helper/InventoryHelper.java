package me.autobot.bedrockminer.helper;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class InventoryHelper {

    public static void cacheInventory() {
        for (Type type : Type.values()) {
            if (stillValid(type)) {
                continue;
            }
            type.reset();

            Player player = Minecraft.getInstance().player;
            Inventory inventory = player.getInventory();

            if (player.getMainHandItem() == type.item) {
                type.updateSlot(inventory.selected);
                continue;
            }

            var list = inventory.items;
            for (ItemStack itemStack : list) {
                if (type == Type.PICKAXE) {
                    if (itemStack.is(Items.DIAMOND_PICKAXE) || itemStack.is(Items.NETHERITE_PICKAXE)) {
                        int level = 0;
                        for (Object2IntMap.Entry<Holder<Enchantment>> e : itemStack.getEnchantments().entrySet()) {
                            if (e.getKey().is(Enchantments.EFFICIENCY)) {
                                level = e.getIntValue();
                                break;
                            }
                        }
                        if (level >= 5) {
                            type.item = itemStack;
                            type.slot = list.indexOf(itemStack);
                        }
                    }
                    continue;
                }
                if (itemStack.is(type.checkItem)) {
                    type.item = itemStack;
                    type.slot = list.indexOf(itemStack);
                }
            }

        }

    }

    private static boolean stillValid(Type type) {
        if (type.item == null || type.slot == -1) {
            return false;
        }
        return Minecraft.getInstance().player.getInventory().getItem(type.slot) == type.item;
    }

    public static void setItemInHand(int slot, ItemStack item) {
        if (slot == -1 || item == null) {
            Minecraft.getInstance().getChatListener().handleSystemMessage(Component.literal("Item Not found"), true);
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Inventory inventory = client.player.getInventory();

        if (inventory.selected == slot && inventory.getItem(slot) == item) {
            return;
        }
        if (Inventory.isHotbarSlot(slot)) {
            inventory.setSelectedHotbarSlot(slot);
        } else {
            client.gameMode.handlePickItem(slot);
        }
        client.getConnection().send(new ServerboundSetCarriedItemPacket(inventory.selected));
    }
    public enum Type {

        PISTON(Items.PISTON), TORCH(Items.REDSTONE_TORCH), PICKAXE(null), SLIME(Items.SLIME_BLOCK);


        int slot = -1;
        ItemStack item = null;

        final Item checkItem;

        Type(Item checkItem) {
            this.checkItem = checkItem;
        }

        public void reset() {
            slot = -1;
            item = null;
        }

        public int getSlot() {
            return slot;
        }
        public ItemStack getItem() {
            return item;
        }
        public void updateSlot(int slot) {
            this.slot = slot;
        }
    }

}
