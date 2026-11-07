package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.UniqueItem;
import java.util.ArrayList;
import java.util.Map;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeStats;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import static jp.aquafactory.apprenticecodex.gametest.equipment.armor.ElementMaidenTestSupport.assertElementMaidenDynamicSchoolPowerBonuses;
import static jp.aquafactory.apprenticecodex.gametest.equipment.armor.ElementMaidenTestSupport.assertElementMaidenSchoolPowerBonusAmount;
import static jp.aquafactory.apprenticecodex.gametest.equipment.armor.ElementMaidenTestSupport.assertNoElementMaidenDynamicSchoolPower;
import static jp.aquafactory.apprenticecodex.gametest.equipment.armor.ElementMaidenTestSupport.createElementMaidenRobeSchoolPowerSpellbook;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.describeModifiers;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.sumModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

final class ElementMaidenRobeGameTestScenarios {
    private ElementMaidenRobeGameTestScenarios() {}

    public static void elementMaidenRobeKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.elementMaidenRobeSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                    ArmorItem.Type.CHESTPLATE, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item instanceof UniqueItem,
                        "Element Maiden Robe " + armorType + " should be a unique item");
                helper.assertTrue(stack.getRarity() == Rarity.EPIC,
                        "Element Maiden Robe " + armorType + " rarity should be epic");
                helper.assertTrue(item.getMaterial().value().defense().get(armorType).equals(ArmorMaterials.IRON.value().defense().get(armorType)),
                        "Element Maiden Robe " + armorType + " defense should match iron");
                helper.assertTrue(Math.abs(item.getMaterial().value().toughness()) < 1.0e-6F,
                        "Element Maiden Robe " + armorType + " toughness should be 0");
                helper.assertTrue(item.getEnchantmentValue(stack) == ElementMaidenRobeStats.enchantmentValue(),
                        "Element Maiden Robe " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Element Maiden Robe " + armorType + " should repair with mithril scrap");

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(
                        modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(maxManaAttribute)),
                        AttributeModifier.Operation.ADD_VALUE
                );
                helper.assertTrue(Math.abs(maxManaBonus - ElementMaidenRobeStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(
                        modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(spellPowerAttribute)),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Element Maiden Robe " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.element_maiden_robe.desc".equals(contents.getKey())),
                        "Element Maiden Robe " + armorType + " should show its common lang desc key");
            }

            var chestplate = (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get();
            var chestStack = new ItemStack(chestplate);
            chestplate.initializeSpellContainer(chestStack);
            var initialContainer = ISpellContainer.get(chestStack);
            helper.assertTrue(initialContainer != null
                            && initialContainer.getSpellAtIndex(0).getSpell() == SpellRegistry.DIVINE_POSSESSION.get(),
                    "Element Maiden Robe chestplate should initialize Divine Possession as its imbue spell");
        });
    }

    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                // GameTest では SpellConfig 経由の school 解決が Evocation に寄ることがあるため、
                // 分配ルール自体は SchoolRegistry から直接検証する.
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 4,
                        SchoolRegistry.ICE.get(), 3
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.14D,
                        "Element Maiden Robe should distribute empty slots to the strongest spellbook school");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.06D,
                        "Element Maiden Robe should keep lower spellbook school share");

                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_school_power_distribution_test");
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()));

                equipCurio(player, Curios.SPELLBOOK_SLOT,
                        createElementMaidenRobeSchoolPowerSpellbook(helper,
                                io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));

                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElementMaidenRobeItem,
                        "Element Maiden Robe dynamic test player is not wearing the robe");
                var resolvedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(player, 0.20D);
                helper.assertTrue(!resolvedBonuses.isEmpty(),
                        "Element Maiden Robe dynamic test could not resolve spellbook schools from Curios slot");
                helper.assertTrue(Math.abs(ApprenticeCodexServerConfig.elementMaidenRobeSchoolSpellPowerBonus() - 0.20D) < 1.0e-9D,
                        "Element Maiden Robe dynamic test config override did not apply");
                var appliedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertElementMaidenDynamicSchoolPowerBonuses(helper, player, appliedBonuses,
                        "Element Maiden Robe should apply Curios spellbook-derived school spell power");
            }
        });
    }

    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 3,
                        SchoolRegistry.ICE.get(), 3,
                        SchoolRegistry.NATURE.get(), 1
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        AttributeRegistry.NATURE_SPELL_POWER.get(),
                        0.02D,
                        "Element Maiden Robe should floor smaller spellbook school shares to 1% units");
            }
        });
    }

    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "element_maiden_robe_school_power_ignore_hand_test");
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
            player.setItemInHand(InteractionHand.MAIN_HAND, createElementMaidenRobeSchoolPowerSpellbook(helper, fire));

            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe should ignore spell containers outside the Curios spellbook slot");
            }

            equipCurio(player, Curios.SPELLBOOK_SLOT,
                    createElementMaidenRobeSchoolPowerSpellbook(helper, fire));
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.0D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe school spell power config 0 should disable the dynamic bonus");
            }
        });
    }

    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
                var ice = SpellRegistry.FROST_RUNE.get();

                var archivistsPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_archivists_source_test");
                archivistsPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                ArchivistsGrimoire.setUpgradeCount(grimoireStack, 2);
                var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
                inventory.setStackInSlot(0, createSpellScroll(fire));
                inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT, createSpellScroll(ice));
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 0);
                equipCurio(archivistsPlayer, Curios.SPELLBOOK_SLOT, grimoireStack);

                var firstArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!firstArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the selected Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, firstArchivistsBonuses,
                        "Element Maiden Robe should read the selected Archivists Grimoire page");
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
                var secondArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!secondArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the new Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, secondArchivistsBonuses,
                        "Element Maiden Robe should apply the new Archivists Grimoire page bonus");

                var enderPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                        "element_maiden_robe_ender_source_test");
                enderPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var enderData = Capabilities.getEnderGrimoireSpellbookOrNull(enderPlayer);
                helper.assertTrue(enderData != null, "Ender Grimoire school spell power test is missing player capability");
                var mutable = ISpellContainer.create(15, true, true).mutableCopy();
                helper.assertTrue(mutable.addSpellAtIndex(fire, 1, 0, false),
                        "Failed to prepare Ender Grimoire fire spell");
                enderData.setSpellContainer(mutable.toImmutable());
                equipCurio(enderPlayer, Curios.SPELLBOOK_SLOT,
                        new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()));

                var enderBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(enderPlayer);
                helper.assertTrue(!enderBonuses.isEmpty(),
                        "Element Maiden Robe should resolve Ender Grimoire spells from the player capability");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, enderPlayer, enderBonuses,
                        "Element Maiden Robe should read Ender Grimoire spells from the player capability");
            }
        });
    }
}
