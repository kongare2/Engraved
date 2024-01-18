package com.kongare;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Ocelot
 */
public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(EngravedMod.MOD_ID, "textures/gui/radio.png");

    private boolean canEdit;
    private EditBox url;

    public RadioScreen(RadioMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 39;
    }
    @Override
    protected void init() {
        super.init();
       // this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.url = new EditBox(this.font, this.leftPos + 10, this.topPos + 21, 154, 16, this.url, Component.translatable("container." + EngravedMod.MOD_ID + ".radio.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32500);
        this.url.setVisible(this.canEdit);
        this.url.setCanLoseFocus(false);
        this.addRenderableWidget(this.url);

        this.addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, button -> {
            //Component.literal
            FriendlyByteBuf pkt = PacketByteBufs.create().writeComponent(MutableComponent.create(new LiteralContents(this.url.getValue())));
            ClientPlayNetworking.send(EngravedMod.SEND_URL_PACKET, pkt);
            this.minecraft.setScreen(null);
        })
                .pos((this.width - this.imageWidth) / 2,(this.height - this.imageHeight) / 2 + this.imageHeight + 5)
                .size(this.imageWidth,20)
                .build());
    }

    @Override
    public void removed() {
        super.removed();
        //this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void containerTick() {
        this.url.tick();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float f, int mouseX, int mouseY) {
        gui.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        gui.blit(TEXTURE, this.leftPos + 8, this.topPos + 18, 0, this.canEdit ? 39 : 53, 160, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title,  this.titleLabelX, this.titleLabelY, 4210752 , false);
        //this.font.draw(poseStack, this.title, (float) this.titleLabelX, (float) this.titleLabelY, 4210752);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return this.url.keyPressed(i, j, k) || (this.url.isFocused() && this.url.isVisible() && i != 256) || super.keyPressed(i, j, k);
    }

    public void receiveUrl(String url) {
        this.canEdit = true;
        this.url.setVisible(true);
        this.url.setValue(url);
        this.setFocused(this.url);
        this.url.setFocused(true);
    }
}
