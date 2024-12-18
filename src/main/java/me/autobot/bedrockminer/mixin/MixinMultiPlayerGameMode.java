package me.autobot.bedrockminer.mixin;

import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.helper.ItemCache;
import me.autobot.bedrockminer.helper.PlayerLookHelper;
import me.autobot.bedrockminer.task.Task;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.autobot.bedrockminer.helper.Consts.*;
@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {


    @Inject(at = @At("HEAD"), method = "tick")
    public void onTick(CallbackInfo ci) {
        if (!Setting.ENABLE) {
            Task.TASKS.clear();
            itemCacheMap.clear();
        }
        Task.TASKS.values().removeIf(task -> task.remove);
        if (!Task.TASKS.isEmpty()) {
            if (itemCacheMap.isEmpty()) {
                ItemCache diamondPickaxeCache = new ItemCache(player.getInventory(), Items.DIAMOND_PICKAXE, 1, 0, false);
                if (diamondPickaxeCache.pass) {
                    itemCacheMap.put(Items.DIAMOND_PICKAXE, diamondPickaxeCache);
                }
                ItemCache netheritePickaxeCache = new ItemCache(player.getInventory(), Items.NETHERITE_PICKAXE, 1, 0, false);
                if (netheritePickaxeCache.pass) {
                    itemCacheMap.put(Items.DIAMOND_PICKAXE, netheritePickaxeCache);
                }
                itemCacheMap.put(Items.PISTON, new ItemCache(player.getInventory(), Items.PISTON, 2, 5, false));
                itemCacheMap.put(Items.REDSTONE_TORCH, new ItemCache(player.getInventory(), Items.REDSTONE_TORCH, 1, 6, false));
                itemCacheMap.put(Items.SLIME_BLOCK, new ItemCache(player.getInventory(), Items.SLIME_BLOCK, 0, 7, true));
            }
            boolean invalid = false;
            for (ItemCache cache : itemCacheMap.values()) {
                if (!cache.pass) {
                    if (cache.optional) {
                        continue;
                    }
                    invalid = true;
                    break;
                }
                cache.updateStatus();
                cache.tryMoveToHotbar();
                if (player.getInventory().getItem(cache.preferHotbarSlot).getCount() < cache.leastCount) {
                    cache.restock();
                }
                if (!cache.optional && player.getInventory().getItem(cache.preferHotbarSlot).getCount() < cache.leastCount) {
                    invalid = true;
                    break;
                }
            }
            if (invalid) {
                Task.TASKS.clear();
                itemCacheMap.clear();
                PlayerLookHelper.reset();
                sendMsg("Fail to Cache Inventory", true);
                return;
            }
//            InventoryHelper.cacheInventory();
//            for (InventoryHelper.Type type : InventoryHelper.Type.values()) {
//                if (type.getSlot() == -1 || type.getItem() == null) {
//                    Task.TASKS.clear();
//                    return;
//                }
//                if (type.getItem().is(Items.PISTON) && type.getItem().getCount() < 2) {
//                    Task.TASKS.clear();
//                    return;
//                }
//                if (!type.getItem().is(Items.SLIME_BLOCK) && type.getItem().getCount() < 1) {
//                    Task.TASKS.clear();
//                    return;
//                }
//            }
        }
        // on order
        for (Task t : Task.TASKS.values()) {
            t.tick();
            break;
        }
    }
}
