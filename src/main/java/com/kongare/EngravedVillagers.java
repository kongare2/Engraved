package com.kongare;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.kongare.mixin.PoiTypesInvoker;
import com.kongare.mixin.StructureTemplatePoolAccessor;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EngravedVillagers {

    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation("minecraft", "empty"));
    public static final PoiType BARD_POI = registerPoiType("bard", PoiTypesInvoker.invokeGetBlockStates(Blocks.NOTE_BLOCK));

    public static final VillagerProfession BARD =
            Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,
            new ResourceLocation(EngravedMod.MOD_ID,"bard"),
            new VillagerProfession("bard", holder -> holder.value().equals(BARD_POI),holder -> holder.value().equals(BARD_POI), ImmutableSet.of(), ImmutableSet.of(), null));

    public static void init() {

    }
    public static PoiType registerPoiType(String name, Set<BlockState> matchingStates) {
        ResourceKey<PoiType> resourceKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, new ResourceLocation(EngravedMod.MOD_ID, name));
        PoiType registry = Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, resourceKey, new PoiType(matchingStates, 1, 1));
        PoiTypesInvoker.invokeRegisterBlockStates(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(resourceKey), matchingStates);
        return registry;
    }

    public static void fillTradeData() {
        VillagerTrades.ItemListing[] oceanographerLevel1 = new VillagerTrades.ItemListing[]{
                new VillagerTrades.EmeraldForItems(Items.MUSIC_DISC_11,1,4,20),
                new VillagerTrades.EmeraldForItems(Items.MUSIC_DISC_13,1,4,20),
                new VillagerTrades.EmeraldForItems(Items.MUSIC_DISC_CAT,1,4,20),
                new VillagerTrades.EmeraldForItems(Items.MUSIC_DISC_OTHERSIDE,1,4,20),
                new VillagerTrades.EmeraldForItems(Items.NOTE_BLOCK,2,16,2),
                new VillagerTrades.ItemsForEmeralds(EngravedMod.MUSIC_LABEL, 1,10,16,1),
        };

        VillagerTrades.ItemListing[] oceanographerLevel2 = new VillagerTrades.ItemListing[]{
                new VillagerTrades.ItemsForEmeralds(EngravedMod.BLANK_MUSIC_DISC, 1,1,16,15),
                new VillagerTrades.ItemsForEmeralds(EngravedMod.ETCHING_TABLE, 1,1,16,15),
        };

        VillagerTrades.ItemListing[] oceanographerLevel3 = new VillagerTrades.ItemListing[]{
                new VillagerTrades.ItemsForEmeralds(Blocks.CLAY, 6,1,16,2),
                new VillagerTrades.ItemsForEmeralds(Blocks.HAY_BLOCK, 12,1,8,2),
                new VillagerTrades.ItemsForEmeralds(Blocks.WHITE_WOOL, 8,1,32,4),
                new VillagerTrades.ItemsForEmeralds(Blocks.BONE_BLOCK, 24,1,8,4),
                new VillagerTrades.ItemsForEmeralds(Blocks.PACKED_ICE, 36,1,4,8),
                new VillagerTrades.ItemsForEmeralds(Blocks.GOLD_BLOCK, 48,1,2,10)
        };

        VillagerTrades.ItemListing[] oceanographerLevel4 = new VillagerTrades.ItemListing[]{
                new VillagerTrades.ItemsForEmeralds(Items.JUKEBOX, 26, 1, 4, 30),
                new VillagerTrades.ItemsForEmeralds(EngravedMod.MINECART_JUKEBOX_ITEM, 28, 1, 4, 30),
                new VillagerTrades.ItemsForEmeralds(EngravedMod.ALBUM_JUKEBOX_ITEM, 30, 1, 4, 30)
        };

        VillagerTrades.ItemListing[] oceanographerLevel5 = new VillagerTrades.ItemListing[]{
                new VillagerTrades.EmeraldForItems(Items.DIAMOND,1,8,40),
                new VillagerTrades.EmeraldForItems(Items.AMETHYST_SHARD,8,10,40),
        };

        VillagerTrades.TRADES.put(BARD,new Int2ObjectOpenHashMap<>(ImmutableMap.of(1,oceanographerLevel1,2,oceanographerLevel2,3,oceanographerLevel3,4,oceanographerLevel4,5,oceanographerLevel5)));
    }

    public static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry, Registry<StructureProcessorList> processorListRegistry, ResourceLocation poolRL, String nbtPieceRL, int weight) {
        Holder<StructureProcessorList> emptyProcessorList = processorListRegistry.getHolderOrThrow(EMPTY_PROCESSOR_LIST_KEY);

        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return;

        SinglePoolElement piece = SinglePoolElement.single(nbtPieceRL, emptyProcessorList).apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++) {
            ((StructureTemplatePoolAccessor) pool).getTemplates().add(piece);
        }

        List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(((StructureTemplatePoolAccessor) pool).getRawTemplates());
        listOfPieceEntries.add(new Pair<>(piece, weight));
        ((StructureTemplatePoolAccessor) pool).setRawTemplates(listOfPieceEntries);
    }

    public static void registerJigsaws(MinecraftServer server) {
        Registry<StructureTemplatePool> templatePoolRegistry = server.registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
        Registry<StructureProcessorList> processorListRegistry = server.registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

        ResourceLocation plainsPoolLocation = new ResourceLocation("minecraft:village/plains/houses");
        ResourceLocation desertPoolLocation = new ResourceLocation("minecraft:village/desert/houses");
        ResourceLocation savannaPoolLocation = new ResourceLocation("minecraft:village/savanna/houses");
        ResourceLocation snowyPoolLocation = new ResourceLocation("minecraft:village/snowy/houses");
        ResourceLocation taigaPoolLocation = new ResourceLocation("minecraft:village/taiga/houses");

        addBuildingToPool(templatePoolRegistry, processorListRegistry, plainsPoolLocation, "engraved:village/plains/plains_bard", 2);
        addBuildingToPool(templatePoolRegistry, processorListRegistry, taigaPoolLocation, "engraved:village/taiga/taiga_bard", 4);
        addBuildingToPool(templatePoolRegistry, processorListRegistry, savannaPoolLocation, "engravd:village/savanna/savanna_bard", 4);
        addBuildingToPool(templatePoolRegistry, processorListRegistry, snowyPoolLocation, "engraved:village/snowy/snowy_bard", 4);
        addBuildingToPool(templatePoolRegistry, processorListRegistry, desertPoolLocation, "engraved:village/desert/desert_bard", 2);
    }
}
