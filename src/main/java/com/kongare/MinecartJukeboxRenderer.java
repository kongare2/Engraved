package com.kongare;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class MinecartJukeboxRenderer<T extends AbstractMinecart> extends MinecartRenderer<T> {

    public MinecartJukeboxRenderer(EntityRendererProvider.Context context) {
        super(context, EngravedModelLayers.MINECART_JUKEBOX);
    }
}
