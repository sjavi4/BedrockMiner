package me.autobot.bedrockminer.mixin;


import me.autobot.bedrockminer.helper.Consts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Shadow public abstract Connection getConnection();

    @Shadow private ClientLevel level;

    @Inject(at = @At(value = "TAIL"), method = "handleLogin")
    public void onJoinWorld(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Consts.gameMode = minecraft.gameMode;
        Consts.connection = getConnection();
        Consts.chatListener = minecraft.getChatListener();
        Consts.localPlayer = minecraft.player;
        Consts.player = minecraft.player;
        Consts.level = level;
    }

}
