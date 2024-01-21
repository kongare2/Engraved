package com.kongare;

import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ContainerItem {

    static boolean isSameItem(ItemStack stack, ItemStack other) {
        return ItemStack.isSameItemSameTags(stack, other);
        //return stack.getItem() == other.getItem() && ItemStack.tagMatches(stack, other);
    }

    static int findSlotMatchingItem(Inventory inventory, ItemStack stack) {
        for(int i = 0; i < inventory.items.size(); ++i) {
            ItemStack slotStack = inventory.items.get(i);
            if (!slotStack.isEmpty() && isSameItem(stack, slotStack)) {
                return i;
            }
        }

        return -1;
    }
}
