package com.kongare.mixin;

import com.google.common.base.Suppliers;
import com.kongare.EntityRecordSoundInstance;
import com.kongare.PlayableRecord;
import com.kongare.TrackData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(RecordItem.class)
public abstract class RecordItemMixin extends Item implements PlayableRecord {

    @Shadow
    public abstract SoundEvent getSound();

    @Unique
    private final Supplier<TrackData[]> track = Suppliers.memoize(() -> {
        Component desc = Component.translatable(this.getDescriptionId() + ".desc");

        String[] parts = desc.getString().split("-", 2);
        if (parts.length < 2)
            return new TrackData[]{new TrackData(this.getSound().getLocation().toString(), "Minecraft", desc)};
        return new TrackData[]{new TrackData(this.getSound().getLocation().toString(), parts[0].trim(), Component.literal(parts[1].trim()).withStyle(desc.getStyle()))};
    });

    private RecordItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canPlay(ItemStack stack) {
        return true;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<? extends SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track) {
        if (track != 0 || !(stack.getItem() instanceof RecordItem))
            return Optional.empty();

        if (PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ()))
            PlayableRecord.showMessage(((RecordItem) stack.getItem()).getDisplayName());
        return Optional.of(new EntityRecordSoundInstance(((RecordItem) stack.getItem()).getSound(), entity));
    }



    @Override
    public Optional<TrackData[]> getMusic(ItemStack stack) {
        return Optional.of(this.track.get());
    }

    @Override
    public Optional<TrackData> getAlbum(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public int getTrackCount(ItemStack stack) {
        return 1;
    }
}
