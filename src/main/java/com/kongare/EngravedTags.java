package com.kongare;


import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class EngravedTags {

    public static final TagKey<Block> AUDIO_PROVIDER =
            TagKey.create(Registries.BLOCK, new ResourceLocation(EngravedMod.MOD_ID, "audio_providers"));
            //TagRegistry.bindBlock(new ResourceLocation(EngravedMod.MOD_ID, "audio_providers"));
}
