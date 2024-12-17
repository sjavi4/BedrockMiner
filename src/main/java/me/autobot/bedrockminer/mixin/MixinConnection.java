package me.autobot.bedrockminer.mixin;


import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.helper.Consts;
import me.autobot.bedrockminer.task.Task;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Inject(at = @At(value = "TAIL"), method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V")
    public void onDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        Consts.gameMode = null;
        Consts.connection = null;
        Consts.chatListener = null;
        Consts.localPlayer = null;
        Consts.itemCacheMap.clear();
        Task.TASKS.clear();
        Setting.ENABLE = false;
    }
}
