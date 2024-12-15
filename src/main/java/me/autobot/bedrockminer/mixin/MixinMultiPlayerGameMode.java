package me.autobot.bedrockminer.mixin;

import me.autobot.bedrockminer.Task;
import me.autobot.bedrockminer.helper.InventoryHelper;
import me.autobot.bedrockminer.helper.PlayerLookHelper;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {


    @Inject(at = @At("HEAD"), method = "tick")
    public void onTick(CallbackInfo ci) {
        if (!Task.TASKS.isEmpty()) {
            PlayerLookHelper.onTick();
            InventoryHelper.cacheInventory();
        }

        for (Task t : Task.TASKS.values()) {
            t.tick();
        }
    }

}
