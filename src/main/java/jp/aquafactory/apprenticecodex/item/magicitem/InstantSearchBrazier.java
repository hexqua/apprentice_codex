package jp.aquafactory.apprenticecodex.item.magicitem;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.magicitem.client.InstantSearchBrazierConfigState;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSummoning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class InstantSearchBrazier extends Item {
    private static final int ADDITIONAL_RANGE_PER_ITEM = 0;

    public InstantSearchBrazier() {
        super(new Item.Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        var failure = SearchBeaconSummoning.validate(serverLevel, serverPlayer);
        if (failure != SearchBeaconSummoning.Failure.NONE) {
            var message = failure == SearchBeaconSummoning.Failure.ALREADY_ACTIVE
                    ? Component.translatable("ui.apprenticecodex.search_beacon.entity.already_active")
                    : Component.translatable("ui.apprenticecodex.cant_place", stack.getHoverName());
            serverPlayer.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        var consumedItem = serverPlayer.getAbilities().instabuild ? ItemStack.EMPTY : stack.copyWithCount(1);
        var beacon = SearchBeaconSummoning.summon(
                serverLevel,
                serverPlayer,
                ApprenticeCodexServerConfig.instantSearchBrazierInitialRange(),
                ADDITIONAL_RANGE_PER_ITEM,
                consumedItem
        );
        if (beacon == null) {
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.playSound(null, beacon.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        lines.add(Component.translatable(getDescriptionId() + ".desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                getDescriptionId() + ".desc_2",
                InstantSearchBrazierConfigState.initialRange()
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_3").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, lines, flag);
    }
}
