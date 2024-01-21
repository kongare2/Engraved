package com.kongare.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.kongare.BoomboxItem;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin extends Entity {

    public ClientLivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (this.level.isClientSide()) {
            BoomboxItem.onLivingEntityUpdate((LivingEntity) (Object) this);
        }
    }
}
