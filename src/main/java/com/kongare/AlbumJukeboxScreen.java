package com.kongare;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author Ocelot
 */
public class AlbumJukeboxScreen extends AbstractContainerScreen<AlbumJukeboxMenu> {

    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/dispenser.png");
    private static final Component NOW_PLAYING = Component.translatable("screen." + EngravedMod.MOD_ID + ".album_jukebox.now_playing").withStyle(ChatFormatting.YELLOW);

    private int playingIndex;
    private int playingTrack;

    public AlbumJukeboxScreen(AlbumJukeboxMenu dispenserMenu, Inventory inventory, Component component) {
        super(dispenserMenu, inventory, component);
    }

    private void update(boolean next) {
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized())
            return;

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity) || !((AlbumJukeboxBlockEntity) blockEntity).isPlaying())
            return;

        AlbumJukeboxBlockEntity albumJukebox = (AlbumJukeboxBlockEntity) blockEntity;
        int oldIndex = albumJukebox.getPlayingIndex();
        int oldTrack = albumJukebox.getTrack();
        if (next) {
            albumJukebox.next();
        } else {
            albumJukebox.previous();
        }

        if (((albumJukebox.getPlayingIndex() == oldIndex && albumJukebox.getTrack() != oldTrack) || albumJukebox.recalculatePlayingIndex(!next)) && albumJukebox.getPlayingIndex() != -1) {
            SoundTracker.playAlbum(albumJukebox, albumJukebox.getBlockState(), level, this.menu.getPos(), true);
            FriendlyByteBuf pkt = PacketByteBufs.create().writeVarInt(albumJukebox.getPlayingIndex()).writeVarInt(albumJukebox.getTrack());
            ClientPlayNetworking.send(EngravedMod.SET_TRACK_PACKET, pkt);
            //EtchedMessages.PLAY.sendToServer(new SetAlbumJukeboxTrackPacket(albumJukebox.getPlayingIndex(), albumJukebox.getTrack()));
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonPadding = 6;
        Component last = Component.literal("Last");
        Component next = Component.literal("Next");
        Font font = Minecraft.getInstance().font;
        this.addRenderableWidget(new Button.Builder(last,b -> this.update(false))
                .pos(this.leftPos + 7 + (54 - font.width(last)) / 2 - buttonPadding,this.topPos + 33)
                .size(font.width(last) + 2 * buttonPadding, 20).build());

        this.addRenderableWidget(new Button.Builder(next,b -> this.update(true))
                .pos(this.leftPos + 115 + (54 - font.width(last)) / 2 - buttonPadding, this.topPos + 33)
                .size(font.width(next) + 2 * buttonPadding, 20).build());

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics gui, int i, int j, float f) {
        this.renderBackground(gui);
        super.render(gui, i, j, f);
        this.renderTooltip(gui, i, j);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void renderBg(GuiGraphics gui, float f, int i, int j) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        gui.blit(CONTAINER_LOCATION, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);

        this.playingIndex = -1;
        this.playingTrack = 0;
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized())
            return;

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity))
            return;

        this.playingIndex = ((AlbumJukeboxBlockEntity) blockEntity).getPlayingIndex();
        this.playingTrack = ((AlbumJukeboxBlockEntity) blockEntity).getTrack();
        if (this.playingIndex != -1) {
            int x = this.playingIndex % 3;
            int y = this.playingIndex / 3;
            gui.fillGradient(guiLeft + 62 + x * 18, guiTop + 17 + y * 18, guiLeft + 78 + x * 18, guiTop + 33 + y * 18, 0x3CF6FF00, 0x3CF6FF00);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int i, int j) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> tooltip = this.getTooltipFromItem(Minecraft.getInstance(), stack);
            if (this.hoveredSlot.index == this.playingIndex) {
                if (this.playingTrack >= 0 && PlayableRecord.getStackTrackCount(stack) > 0) {
                    Optional<TrackData[]> optional = PlayableRecord.getStackMusic(stack).filter(tracks -> this.playingTrack < tracks.length);
                    if (optional.isPresent()) {
                        TrackData track = optional.get()[this.playingTrack];
                        tooltip.add(NOW_PLAYING.copy().append(": ").append(track.getDisplayName()).append(" (" + (this.playingTrack + 1) + "/" + optional.get().length + ")"));
                    } else {
                        tooltip.add(NOW_PLAYING);
                    }
                } else {
                    tooltip.add(NOW_PLAYING);
                }
            }
            gui.renderComponentTooltip(this.font, tooltip, i, j);
        }
    }
}
