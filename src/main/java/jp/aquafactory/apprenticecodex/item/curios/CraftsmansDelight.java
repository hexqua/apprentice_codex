package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.HashMap;
import java.util.List;

public class CraftsmansDelight extends Item implements ICurioItem {
    private static final float ENCHANTMENT_SWITCH_MANA_COST = 100f;
    private final String slotIdentifier;

    public CraftsmansDelight() {
        super(new Item.Properties().stacksTo(1));
        slotIdentifier = Curios.RING_SLOT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();

        if (level.isClientSide) {
            return;
        }

        ApprenticeCodex.LOGGER.debug("Equip: CraftsmansDelight");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();

        if (level.isClientSide) {
            return;
        }

        ApprenticeCodex.LOGGER.debug("Unequip: CraftsmansDelight");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();

        if (level.isClientSide) {
            return;
        }

        // TODO: implement ring effect logic.
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return tooltips;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, usedHand);
        }

        if (!level.isClientSide) {
            if (!consumeManaForSneakUse(player, stack)) {
                return InteractionResultHolder.fail(stack);
            }

            AudioTools.playSoundFromEntity(level, player, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS);
            applySneakUseEnchantment(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return !slotContext.entity().isShiftKeyDown();
    }

    private static void applySneakUseEnchantment(ItemStack stack) {
        var enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (enchantments.isEmpty()) {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return;
        }

        var hasNonFortune = enchantments.keySet().stream()
                .anyMatch(enchantment -> !enchantment.equals(Enchantments.BLOCK_FORTUNE));

        enchantments.clear();
        EnchantmentHelper.setEnchantments(enchantments, stack);

        if (hasNonFortune) {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return;
        }

        stack.enchant(Enchantments.SILK_TOUCH, 1);
    }

    private static boolean consumeManaForSneakUse(Player player, ItemStack stack) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < ENCHANTMENT_SWITCH_MANA_COST) {
            sendManaLackMessage(player, stack);
            return false;
        }

        // addManaは内部で結局setを実行しているので-をaddしてもちゃんとマイナスされる.
        magicData.addMana(-ENCHANTMENT_SWITCH_MANA_COST);
        return true;
    }

    private static void sendManaLackMessage(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.irons_spellbooks.cast_error_mana", stack.getHoverName()).withStyle(ChatFormatting.RED)
        ));
    }
}
