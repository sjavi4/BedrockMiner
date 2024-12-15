package me.autobot.bedrockminer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseTorchBlock.class)
public interface MixinGetterBaseTorchBlock {

    @Invoker("canSurvive")
    boolean getCanSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos);

}
