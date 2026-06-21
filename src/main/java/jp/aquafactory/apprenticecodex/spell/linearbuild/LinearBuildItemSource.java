package jp.aquafactory.apprenticecodex.spell.linearbuild;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface LinearBuildItemSource {
    Component label();

    boolean shouldNotifyRetrieved();

    boolean hasMatchingItem(ItemStack template);

    boolean consumeOne(ItemStack template);
}
