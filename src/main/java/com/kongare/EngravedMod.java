package com.kongare;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class EngravedMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("engraved-mod");
	public static final String MOD_ID = "engraved";
	public static final ResourceLocation SEND_URL_PACKET = new ResourceLocation(MOD_ID,"send_url");
	public static final ResourceLocation SET_TRACK_PACKET = new ResourceLocation(MOD_ID, "set_track");
	public static final ResourceLocation EDIT_MUSIC_LABEL_PACKET = new ResourceLocation(MOD_ID, "edit_music_label");
	public static final ResourceLocation PLAY_MUSIC_PACKET = new ResourceLocation(MOD_ID,"play_music");
	public static final ResourceLocation INVALID_URL_PACKET = new ResourceLocation(MOD_ID, "invalid_url");
	public static final ResourceLocation PLAY_ENTITY_MUSIC_PACKET = new ResourceLocation(MOD_ID, "play_entity_music");
	public static final Block RADIO_BLOCK = new RadioBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX).noOcclusion());
	public static final Block ALBUM_JUKEBOX_BLOCK = new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX).noOcclusion());
	public static final Block ETCHING_TABLE = new EtchingTableBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
	public static final SoundEvent UI_ETCHER_TAKE_RESULT = Registry.register(BuiltInRegistries.SOUND_EVENT,
															new ResourceLocation(MOD_ID, "ui.etching_table.take_result"),
															SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID,"ui.etching_table.take_result")));
	public static Item RADIO_ITEM;
	public static Item ALBUM_JUKEBOX_ITEM;
	public static Item ETCHING_TABLE_ITEM;

	public static Item BLANK_MUSIC_DISC = Registry.register(BuiltInRegistries.ITEM,
															new ResourceLocation(MOD_ID, "blank_music_disc"),
			 												new BlankMusicDiscItem(new Item.Properties()));

	public static Item ENGRAVED_MUSIC_DISC =Registry.register(BuiltInRegistries.ITEM,
															new ResourceLocation(MOD_ID,"engraved_music_disc"),
															new EngravedMusicDiscItem(new Item.Properties().stacksTo(1)));
	public static Item MUSIC_LABEL = Registry.register(BuiltInRegistries.ITEM,
														new ResourceLocation(MOD_ID,"music_label"),
														new MusicLabelItem(new Item.Properties()));

	public static final Item MINECART_JUKEBOX_ITEM = Registry.register(BuiltInRegistries.ITEM,
														new ResourceLocation(MOD_ID,"minecart_jukebox"),
														new MinecartJukeboxItem(new Item.Properties().stacksTo(1)));

	public static final Item BOOMBOX_ITEM = Registry.register(BuiltInRegistries.ITEM,
									new ResourceLocation(MOD_ID, "boombox"),
									new BoomboxItem(new Item.Properties().stacksTo(1)));
	public static final EntityType<MinecartJukebox> MINECART_JUKEBOX =
			Registry.register(BuiltInRegistries.ENTITY_TYPE, "minecart_jukebox", EntityType.Builder.<MinecartJukebox>of(MinecartJukebox::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("minecart_jukebox"));


	public static MenuType<RadioMenu> RADIO_MENU =
			Registry.register(BuiltInRegistries.MENU,
					new ResourceLocation(MOD_ID,"radio"),
					new MenuType<>(RadioMenu::new, FeatureFlags.VANILLA_SET));

	public static MenuType<AlbumJukeboxMenu> ALBUM_JUKEBOX_MENU =
			Registry.register(BuiltInRegistries.MENU,
					new ResourceLocation(MOD_ID,"album_jukebox"),
					new MenuType<>(AlbumJukeboxMenu::new, FeatureFlags.VANILLA_SET));

	public static MenuType<EtchingMenu> ETCHING_MENU =
			Registry.register(BuiltInRegistries.MENU,
					new ResourceLocation(MOD_ID,"etching_table"),
					new MenuType<>(EtchingMenu::new, FeatureFlags.VANILLA_SET));

	public static final BlockEntityType<RadioBlockEntity> RADIO_ENTITY_TYPE =
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
					new ResourceLocation(MOD_ID,"radio"),
					FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, RADIO_BLOCK).build() );
	public static final BlockEntityType<AlbumJukeboxBlockEntity> ALBUM_JUKEBOX_ENTITY_TYPE =
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
					new ResourceLocation(MOD_ID,"album_jukebox"),
					FabricBlockEntityTypeBuilder.create(AlbumJukeboxBlockEntity::new, ALBUM_JUKEBOX_BLOCK).build() );
	@Override
	public void onInitialize() {

		Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID,"radio"), RADIO_BLOCK);
		Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID,"album_jukebox"), ALBUM_JUKEBOX_BLOCK);
		Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID,"etching_table"), ETCHING_TABLE);

		RADIO_ITEM = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID,"radio"),
							new BlockItem(RADIO_BLOCK, new FabricItemSettings()));

		ALBUM_JUKEBOX_ITEM = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID,"album_jukebox"),
				new BlockItem(ALBUM_JUKEBOX_BLOCK, new FabricItemSettings()));

		ETCHING_TABLE_ITEM = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID,"etching_table"),
				new BlockItem(ETCHING_TABLE, new FabricItemSettings()));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register((itemGroup) -> {
			itemGroup.accept(RADIO_ITEM);
			itemGroup.accept(ALBUM_JUKEBOX_ITEM);
			itemGroup.accept(BLANK_MUSIC_DISC);
			itemGroup.accept(MUSIC_LABEL);
			itemGroup.accept(ETCHING_TABLE_ITEM);
			itemGroup.accept(MINECART_JUKEBOX_ITEM);
			itemGroup.accept(BOOMBOX_ITEM);
		});

		EngravedVillagers.init();
		EngravedVillagers.fillTradeData();
		ServerLifecycleEvents.SERVER_STARTING.register(EngravedVillagers::registerJigsaws);

		ServerPlayNetworking.registerGlobalReceiver(EngravedMod.SEND_URL_PACKET,(server, player, handler, buf, responseSender) ->{
			String url = buf.readComponent().getString();
			server.execute(() -> {
				if (player.containerMenu instanceof RadioMenu) {
					RadioMenu menu = (RadioMenu) player.containerMenu;
					menu.setUrl(url);
				} else if (player.containerMenu instanceof EtchingMenu) {
					EtchingMenu menu = (EtchingMenu) player.containerMenu;
					menu.setUrl(url);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(EngravedMod.SET_TRACK_PACKET,(server, player, handler, buf, responseSender) -> {
			int a1 = buf.readVarInt();
			int a2 = buf.readVarInt();
			server.execute(() -> {
				if (player.containerMenu instanceof AlbumJukeboxMenu) {
					AlbumJukeboxMenu menu = (AlbumJukeboxMenu) player.containerMenu;
					if (menu.setPlayingTrack(player.level, a1,a2)) {
						FriendlyByteBuf pkt = PacketByteBufs.create().writeVarInt(a1).writeVarInt(a2);
						ServerPlayNetworking.send(player, EngravedMod.SET_TRACK_PACKET, pkt);
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(EngravedMod.EDIT_MUSIC_LABEL_PACKET,(server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			String author = buf.readUtf(128);
			String title = buf.readUtf(128);
			server.execute(() -> {
				if (!Inventory.isHotbarSlot(slot) && slot != 40)
					return;
				ItemStack labelStack = player.getInventory().getItem(slot);
				SimpleMusicLabelItem.setTitle(labelStack, StringUtils.normalizeSpace(title));
				SimpleMusicLabelItem.setAuthor(labelStack, StringUtils.normalizeSpace(author));
			});
		});


	}
}