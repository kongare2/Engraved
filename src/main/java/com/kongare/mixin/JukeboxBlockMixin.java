package com.kongare.mixin;

import com.kongare.PlayableRecord;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin extends BaseEntityBlock {

    protected JukeboxBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "getAnalogOutputSignal", at = @At("TAIL"), cancellable = true)
    public void getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity be) {
            ItemStack record = be.getFirstItem();
            if (!(record.getItem() instanceof RecordItem) && record.getItem() instanceof PlayableRecord)
                cir.setReturnValue(15);
        }
    }

}
