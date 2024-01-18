package com.kongare;

import com.kongare.mixin.LevelRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;


public class EngravedModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(EngravedMod.RADIO_MENU, RadioScreen::new);
		MenuScreens.register(EngravedMod.ALBUM_JUKEBOX_MENU, AlbumJukeboxScreen::new);
		MenuScreens.register(EngravedMod.ETCHING_MENU, EtchingScreen::new);

		ColorProviderRegistry.ITEM.register((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EngravedMod.BLANK_MUSIC_DISC);
		ColorProviderRegistry.ITEM.register((stack, index) -> index == 0 ? EngravedMusicDiscItem.getDiscColor(stack) : EngravedMusicDiscItem.getPattern(stack).isColorable() ? index == 1 ? EngravedMusicDiscItem.getLabelPrimaryColor(stack) : index == 2 ? EngravedMusicDiscItem.getLabelSecondaryColor(stack) : -1 : -1, EngravedMod.ENGRAVED_MUSIC_DISC);

		ItemProperties.register(EngravedMod.ENGRAVED_MUSIC_DISC, new ResourceLocation(EngravedMod.MOD_ID, "pattern"), (stack, level, entity, i) -> EngravedMusicDiscItem.getPattern(stack).ordinal() / 10F);
		//ItemProperties.register(EngravedMod.ENGRAVED_MUSIC_DISC, )
		ClientPlayNetworking.registerGlobalReceiver(EngravedMod.SEND_URL_PACKET,
				(client, handler, buf, sender)->{
				String url = buf.readComponent().getString();
				client.execute( () -> {
					if (Minecraft.getInstance().screen instanceof RadioScreen) {
						RadioScreen screen = (RadioScreen) Minecraft.getInstance().screen;
						screen.receiveUrl(url);
					}});
		});
		ClientPlayNetworking.registerGlobalReceiver(EngravedMod.INVALID_URL_PACKET,
				(client, handler, buf, sender)->{
				String reason = buf.readUtf(32767);
					client.execute( () -> {
						if (Minecraft.getInstance().screen instanceof EtchingScreen) {
							EtchingScreen screen = (EtchingScreen) Minecraft.getInstance().screen;
							screen.setReason(reason);
						}});
				});

		ClientPlayNetworking.registerGlobalReceiver(EngravedMod.PLAY_MUSIC_PACKET,
				(client, handler, buf, sender)->{

				ItemStack itm = buf.readItem();
				BlockPos pos = buf.readBlockPos();

					client.execute( () -> {

						SoundManager soundManager = Minecraft.getInstance().getSoundManager();
						Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
						SoundInstance soundInstance = playingRecords.get(pos);
						if (soundInstance != null) {
							soundManager.stop(soundInstance);
							playingRecords.remove(pos);
						}

						TrackData[] td = PlayableRecord.getStackMusic(itm).orElseGet(() -> new TrackData[0]);
						if (td.length == 0)
							return;

						SoundTracker.playBlockRecord(pos, td, 0);
					});
				});

		ClientPlayNetworking.registerGlobalReceiver(EngravedMod.SET_TRACK_PACKET,
				(client, handler, buf, sender)->{
					int a1 = buf.readVarInt();
					int a2 = buf.readVarInt();
					client.execute( () -> {
						if (Minecraft.getInstance().screen instanceof RadioScreen) {
							AlbumJukeboxScreen screen = (AlbumJukeboxScreen) Minecraft.getInstance().screen;
							BlockPos pos = screen.getMenu().getPos();
							if (screen.getMenu().setPlayingTrack(Minecraft.getInstance().level, a1,a2)) {
								AlbumJukeboxBlockEntity entity = (AlbumJukeboxBlockEntity) Objects.requireNonNull
										(Minecraft.getInstance().level.getBlockEntity(pos));
								SoundTracker.playAlbum(entity, entity.getBlockState(), Minecraft.getInstance().level, pos, true);
							}
						}});
				});

	}
}