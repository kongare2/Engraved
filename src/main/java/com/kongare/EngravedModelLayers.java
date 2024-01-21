package com.kongare;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class EngravedModelLayers {
    public static final ModelLayerLocation MINECART_JUKEBOX = create("minecart_jukebox");

    public static ModelLayerLocation create(String model) {
        return create(model, "main");
    }

    public static ModelLayerLocation create(String model, String layer) {
        return new ModelLayerLocation(new ResourceLocation(EngravedMod.MOD_ID, model), layer);
    }
}
