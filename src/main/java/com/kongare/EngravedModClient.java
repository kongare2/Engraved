package com.kongare;

import com.kongare.mixin.LevelRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.mixin.registry.sync.client.ItemModelsMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public class EngravedModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(EngravedMod.RADIO_MENU, RadioScreen::new);
		MenuScreens.register(EngravedMod.ALBUM_JUKEBOX_MENU, AlbumJukeboxScreen::new);
		MenuScreens.register(EngravedMod.ETCHING_MENU, EtchingScreen::new);

		ColorProviderRegistry.ITEM.register((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EngravedMod.BLANK_MUSIC_DISC);
		ColorProviderRegistry.ITEM.register((stack, index) -> index == 0 ? EngravedMusicDiscItem.getDiscColor(stack) : EngravedMusicDiscItem.getPattern(stack).isColorable() ? index == 1 ? EngravedMusicDiscItem.getLabelPrimaryColor(stack) : index == 2 ? EngravedMusicDiscItem.getLabelSecondaryColor(stack) : -1 : -1, EngravedMod.ENGRAVED_MUSIC_DISC);

		EntityModelLayerRegistry.registerModelLayer(EngravedModelLayers.MINECART_JUKEBOX, MinecartModel::createBodyLayer);
		EntityRendererRegistry.register(EngravedMod.MINECART_JUKEBOX, MinecartJukeboxRenderer::new);

		ItemProperties.register(EngravedMod.ENGRAVED_MUSIC_DISC, new ResourceLocation(EngravedMod.MOD_ID, "pattern"), (stack, level, entity, i) -> EngravedMusicDiscItem.getPattern(stack).ordinal() / 10F);

		ItemProperties.register(EngravedMod.BOOMBOX_ITEM, new ResourceLocation(EngravedMod.MOD_ID, "playing"), (stack, level, entity, i) -> {
			if (!(entity instanceof Player))
				return 0;
			InteractionHand hand = BoomboxItem.getPlayingHand(entity);
			return hand != null && stack == entity.getItemInHand(hand) ? 1 : 0;
		});

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


		ClientPlayNetworking.registerGlobalReceiver(EngravedMod.PLAY_ENTITY_MUSIC_PACKET,
				(client, handler, buf, sender)->{
					EntityMusicAction action = buf.readEnum(EntityMusicAction.class);
					ItemStack record = null;
					if (action != EntityMusicAction.STOP)
						record = buf.readItem();
					int entityid = buf.readVarInt();

					final ItemStack rrecord = record;
					client.execute( () -> {
						SoundManager soundManager = Minecraft.getInstance().getSoundManager();
						SoundInstance soundInstance = SoundTracker.getEntitySound(entityid);
						ClientLevel level = Minecraft.getInstance().level;

						if (soundInstance != null) {
							if (soundInstance instanceof StopListeningSound)
								((StopListeningSound) soundInstance).stopListening();
							if (action == EntityMusicAction.RESTART && soundManager.isActive(soundInstance))
								return;
							SoundTracker.setEntitySound(entityid, null);
						}

						if (action == EntityMusicAction.STOP)
							return;

						Entity entity = level.getEntity(entityid);
						if (entity == null) {
							//LOGGER.error("Server sent sound for nonexistent entity: " + entityId);
							return;
						}

						if (!PlayableRecord.isPlayableRecord(rrecord)) {
							//LOGGER.error("Server sent invalid music disc: " + record);
							return;
						}

						Optional<? extends SoundInstance> sound = ((PlayableRecord) rrecord.getItem()).createEntitySound(rrecord, entity, 0);
						if (!sound.isPresent()) {
							//LOGGER.error("Server sent invalid music disc: " + record);
							return;
						}

						SoundInstance entitySound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(() -> {
							SoundTracker.setEntitySound(entityid, null);
							SoundTracker.playEntityRecord(rrecord, entityid, 1, false);
						}));

						SoundTracker.setEntitySound(entityid, entitySound);


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