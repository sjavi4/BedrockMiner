package me.autobot.bedrockminer.mixin;

import me.autobot.bedrockminer.helper.PlayerLookHelper;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerboundMovePlayerPacket.class)
public class MixinServerboundMovePlayerPacket {

    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static float modifyLookYaw(float yaw) {
        return PlayerLookHelper.onModifyLookYaw(yaw);
    }

    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static float modifyLookPitch(float pitch) {
        return PlayerLookHelper.onModifyLookPitch(pitch);
    }
}
