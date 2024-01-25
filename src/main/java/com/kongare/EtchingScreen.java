package com.kongare;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Jackson
 */
public class EtchingScreen extends AbstractContainerScreen<EtchingMenu> implements ContainerListener {

    private static final ResourceLocation TEXTURE = new ResourceLocation(EngravedMod.MOD_ID, "textures/gui/container/etching_table.png");
    private static final Component INVALID_URL = Component.translatable("screen." + EngravedMod.MOD_ID + ".etching_table.error.invalid_url");
    private static final Component CANNOT_CREATE = Component.translatable("screen." + EngravedMod.MOD_ID + ".etching_table.error.cannot_create");
    private static final Component CANNOT_CREATE_MISSING_DISC = Component.translatable("screen." + EngravedMod.MOD_ID + ".etching_table.error.cannot_create.missing_disc").withStyle(ChatFormatting.GRAY);
    private static final Component CANNOT_CREATE_MISSING_LABEL = Component.translatable("screen." + EngravedMod.MOD_ID + ".etching_table.error.cannot_create.missing_label").withStyle(ChatFormatting.GRAY);

    private ItemStack discStack;
    private ItemStack labelStack;
    private EditBox url;
    private int urlTicks;
    private String oldUrl;
    private String invalidReason;
    private boolean displayLabels;

    public EtchingScreen(EtchingMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 180;
        this.inventoryLabelY += 14;

        this.discStack = ItemStack.EMPTY;
        this.labelStack = ItemStack.EMPTY;

        this.invalidReason = "";
    }
    private void sendtoServerSetUrl(String url) {
        FriendlyByteBuf pkt = PacketByteBufs.create().writeComponent(Component.literal( url) );
        ClientPlayNetworking.send(EngravedMod.SEND_URL_PACKET, pkt);
    }
    @Override
    protected void init() {
        super.init();
        //this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.url = new EditBox(this.font, this.leftPos + 11, this.topPos + 25, 154, 16, this.url, Component.translatable("container." + EngravedMod.MOD_ID + ".etching_table.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32500);
        this.url.setResponder(s -> {
            if (!Objects.equals(this.oldUrl, s) && this.urlTicks <= 0)
                sendtoServerSetUrl("");
              //  EtchedMessages.PLAY.sendToServer(new ServerboundSetUrlPacket(""));
            this.urlTicks = 8;
        });
        this.url.setCanLoseFocus(true);
        this.addWidget(this.url);
        this.menu.addSlotListener(this);
    }

    @Override
    public void removed() {
        super.removed();
        //this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void containerTick() {
        //this.url.tick();
        if (this.urlTicks > 0) {
            this.urlTicks--;
            if (this.urlTicks <= 0 && !Objects.equals(this.oldUrl, this.url.getValue())) {
                this.oldUrl = this.url.getValue();
                sendtoServerSetUrl(this.url.getValue());
               // EtchedMessages.PLAY.sendToServer(new ServerboundSetUrlPacket(this.url.getValue()));
            }
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu abstractContainerMenu, int slot, ItemStack stack) {
        if (slot == 0) {
            if (this.discStack.isEmpty() && !stack.isEmpty())
                this.url.setValue("");
            PlayableRecord.getStackAlbum(stack).ifPresent(track -> this.url.setValue(track.getUrl()));
            this.discStack = stack;
        }

        if (slot == 1) {
            this.labelStack = stack;
        }

        boolean editable = this.discStack.getItem() == EngravedMod.ENGRAVED_MUSIC_DISC || (!this.discStack.isEmpty() && !this.labelStack.isEmpty());
        this.url.setEditable(editable);
        this.url.setVisible(editable);
        this.url.setFocused(editable);
        this.setFocused(editable ? this.url : null);

        this.displayLabels = !this.discStack.isEmpty() && !this.labelStack.isEmpty();
    }

    @Override
    public void dataChanged(AbstractContainerMenu abstractContainerMenu, int index, int value) {
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        super.render(gui, mouseX, mouseY, partialTicks);
        this.url.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        boolean isEtched = this.discStack.getItem() ==  EngravedMod.ENGRAVED_MUSIC_DISC;
        List<FormattedCharSequence> reasonLines = new ArrayList<>();
        if (!isEtched && !this.discStack.isEmpty() && this.labelStack.isEmpty()) {
            reasonLines.add(CANNOT_CREATE.getVisualOrderText());
            reasonLines.add(CANNOT_CREATE_MISSING_LABEL.getVisualOrderText());
        } else if (!isEtched && this.discStack.isEmpty() && !this.labelStack.isEmpty()) {
            reasonLines.add(CANNOT_CREATE.getVisualOrderText());
            reasonLines.add(CANNOT_CREATE_MISSING_DISC.getVisualOrderText());
        } else if ((!this.url.getValue().isEmpty() && !TrackData.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty()) {
            reasonLines.add(INVALID_URL.getVisualOrderText());
            if (!this.invalidReason.isEmpty())
                reasonLines.addAll(this.font.split(Component.literal(this.invalidReason).withStyle(ChatFormatting.GRAY), 200));
        }

        if (mouseX >= this.leftPos + 83 && mouseX < this.leftPos + 110 && mouseY >= this.topPos + 44 && mouseY < this.topPos + 61) {
            gui.renderTooltip(this.font, reasonLines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float f, int mouseX, int mouseY) {
        //this.renderBackground(gui,mouseX,mouseY,f);

        //RenderSystem.setShaderTexture(0, TEXTURE);
        gui.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if ((!this.url.getValue().isEmpty() && !TrackData.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty() || (this.discStack.getItem() != EngravedMod.ENGRAVED_MUSIC_DISC && ((!this.discStack.isEmpty() && this.labelStack.isEmpty()) || (this.discStack.isEmpty() && !this.labelStack.isEmpty()))))
            gui.blit(TEXTURE, this.leftPos + 83, this.topPos + 44, 0, 226, 27, 17);

        gui.blit(TEXTURE, this.leftPos + 9, this.topPos + 21, 0, (this.discStack.getItem() == EngravedMod.ENGRAVED_MUSIC_DISC || (!this.discStack.isEmpty() && !this.labelStack.isEmpty()) ? 180 : 196), 158, 16);

        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;
                RenderSystem.setShaderTexture(0, TEXTURE);

                int u = index == this.menu.getLabelIndex() ? 14 : mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 ? 28 : 0;

                gui.blit(TEXTURE, x, y, u, 212, 14, 14);
                this.renderLabel(gui, x, y, index);
            }
        }
    }

    private void renderLabel(GuiGraphics gui, int x, int y, int index) {
        if (this.labelStack.isEmpty() || this.discStack.isEmpty())
            return;

        EngravedMusicDiscItem.LabelPattern pattern = EngravedMusicDiscItem.LabelPattern.values()[index];
        int primaryLabelColor = 0xFFFFFF;
        int secondaryLabelColor = primaryLabelColor;
        if (this.labelStack.getItem() instanceof MusicLabelItem) {
            primaryLabelColor = MusicLabelItem.getLabelColor(this.labelStack);
            secondaryLabelColor = primaryLabelColor;
        }

        /*else if (this.labelStack.getItem() instanceof ComplexMusicLabelItem) {
            primaryLabelColor = ComplexMusicLabelItem.getPrimaryColor(this.labelStack);
            secondaryLabelColor = ComplexMusicLabelItem.getSecondaryColor(this.labelStack);
        }*/

        if (pattern.isColorable())
            RenderSystem.setShaderColor((float) (primaryLabelColor >> 16 & 255) / 255.0F, (float) (primaryLabelColor >> 8 & 255) / 255.0F, (float) (primaryLabelColor & 255) / 255.0F, 1.0F);

        Pair<ResourceLocation, ResourceLocation> textures = pattern.getTextures();
        RenderSystem.setShaderTexture(0, textures.getLeft());

        //ShapeRenderer.setShader(GameRenderer::getPositionColorTexShader);
        //RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        //ShapeRenderer.drawRectWithTexture(poseStack, x, y, 1, 1, 14, 14, 14, 14, 16, 16);
        gui.blit(textures.getLeft(),x,x+14,y,y+14, 0, 14,14,1,1     ,16,16);
        if (!pattern.isSimple()) {
            if (pattern.isColorable())
                RenderSystem.setShaderColor((float) (secondaryLabelColor >> 16 & 255) / 255.0F, (float) (secondaryLabelColor >> 8 & 255) / 255.0F, (float) (secondaryLabelColor & 255) / 255.0F, 1.0F);

            RenderSystem.setShaderTexture(0, textures.getRight());
            gui.blit(textures.getRight(),x,x+14,y,y+14, 0, 14,14,1,1     ,16,16);
            //ShapeRenderer.drawRectWithTexture(poseStack, x, y, 1, 1, 14, 14, 14, 14, 16, 16);
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int i) {
        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;

                if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 && this.menu.getLabelIndex() != index) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, i);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return this.url.keyPressed(i, j, k) || (this.url.isFocused() && this.url.isVisible() && i != 256) || super.keyPressed(i, j, k);
    }

    public void setReason(String exception) {
        this.invalidReason = exception;
    }
}
