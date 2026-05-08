package jp.aquafactory.apprenticecodex.item.curios.endergrimoire;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EnderGrimoire extends Item implements ICurioItem, ISpellbook, IPresetSpellContainer, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.ender_grimoire.desc_";
    private static final AttributeContainer[] SPELLBOOK_ATTRIBUTES = {
            new AttributeContainer(
                    AttributeRegistry.MAX_MANA,
                    200,
                    AttributeModifier.Operation.ADDITION
            )
    };

    public EnderGrimoire() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // 編集画面を開かせるため、右クリック装備は許容しない.
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, targetPlayer) -> new EnderGrimoireInscriptionMenu(containerId, inventory),
                    Component.translatable("container.apprenticecodex.ender_grimoire_inscription")
            ));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        if (!Curios.SPELLBOOK_SLOT.equals(slotContext.identifier())) {
            return ICurioItem.super.getAttributeModifiers(slotContext, uuid, stack);
        }

        // 能力値補正はここで処理して表示もする.
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var attributeContainer : SPELLBOOK_ATTRIBUTES) {
            var modifierName = String.format("%s_%s", Curios.SPELLBOOK_SLOT, slotContext.index());
            builder.put(attributeContainer.attribute().get(), attributeContainer.createModifier(modifierName));
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, Level context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        var player = MinecraftInstanceHelper.getPlayer();
        if (player != null) {
            player.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(spellbookData -> {
                var spellList = spellbookData.getSpellContainer();
                lines.add(Component.translatable("tooltip.irons_spellbooks.spellbook_spell_count", spellList.getMaxSpellCount()).withStyle(ChatFormatting.GRAY));

                var activeSpellSlots = spellList.getActiveSpells();
                if (activeSpellSlots.isEmpty()) {
                    lines.add(Component.translatable("item.apprenticecodex.special_spellbook.inscribe_hint").withStyle(ChatFormatting.GRAY));
                }
                if (!activeSpellSlots.isEmpty()) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable("tooltip.irons_spellbooks.press_to_cast", Component.keybind("key.irons_spellbooks.spellbook_cast")).withStyle(ChatFormatting.GOLD));
                    lines.add(Component.empty());
                    lines.add(Component.translatable("tooltip.irons_spellbooks.spellbook_tooltip").withStyle(ChatFormatting.GRAY));

                    SpellSelectionManager spellSelectionManager = ClientMagicData.getSpellSelectionManager();
                    if (spellSelectionManager == null) {
                        return;
                    }

                    for (int i = 0; i < activeSpellSlots.size(); i++) {
                        var spellText = TooltipsUtils.getTitleComponent(activeSpellSlots.get(i).spellData(), (LocalPlayer) player).setStyle(Style.EMPTY);
                        var option = spellSelectionManager.getSpellSlot(spellSelectionManager.getSelectionIndex());

                        if (MinecraftInstanceHelper.getPlayer() != null
                                && Utils.getPlayerSpellbookStack(MinecraftInstanceHelper.getPlayer()) == itemStack
                                && option != null
                                && option.slot.equals(Curios.SPELLBOOK_SLOT)
                                && option.slotIndex == i) {
                            var shiftMessage = TooltipsUtils.formatActiveSpellTooltip(itemStack, spellSelectionManager.getSelectedSpellData(), CastSource.SPELLBOOK, (LocalPlayer) player);
                            shiftMessage.remove(0);
                            TooltipsUtils.addShiftTooltip(
                                    lines,
                                    Component.literal("> ").append(spellText).withStyle(ChatFormatting.YELLOW),
                                    shiftMessage.stream().map(component -> Component.literal(" ").append(component)).collect(Collectors.toList())
                            );
                        } else {
                            lines.add(Component.literal(" ").append(spellText.withStyle(Style.EMPTY.withColor(0x8888fe))));
                        }
                    }
                }
            });
        }

        super.appendHoverText(itemStack, context, lines, flag);
    }

    @NotNull
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundRegistry.EQUIP_SPELL_BOOK.get(), 1.0f, 1.0f);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
    }
}
