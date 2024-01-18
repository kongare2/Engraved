package com.kongare;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class PlayableRecordItem extends Item implements PlayableRecord {

    private static final Component ALBUM = Component.translatable("item." + EngravedMod.MOD_ID + ".etched_music_disc.album").withStyle(ChatFormatting.DARK_GRAY);

    public PlayableRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD))
            return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        if (!this.getMusic(stack).isPresent())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            Player player = ctx.getPlayer();
            BlockEntity var8 = level.getBlockEntity(pos);
            if (var8 instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity jukeboxBlockEntity = (JukeboxBlockEntity)var8;
                jukeboxBlockEntity.setFirstItem(stack.copy());
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }

            FriendlyByteBuf data = PacketByteBufs.create().writeItem(stack.copy()).writeBlockPos(pos);

            for (ServerPlayer p : PlayerLookup.around((ServerLevel)level,
                    new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 64)) {

                ServerPlayNetworking.send(p, EngravedMod.PLAY_MUSIC_PACKET, data);
            }

            // new ClientboundPlayMusicPacket(stack.copy(), pos));

            stack.shrink(1);
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        this.getAlbum(stack).ifPresent(track -> {
            list.add(track.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            Component brand = SoundSourceManager.getBrandText(track.getUrl())
                    .map(component -> Component.literal("  ").append(component.copy()))
                    .<Component>map(component -> getTrackCount(stack) > 1 ? component.append(" ").append(ALBUM) : component)
                    .orElse(getTrackCount(stack) > 1 ? ALBUM : Component.empty());
            list.add(brand);
        });
    }

}
