package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AttributeEnchantmentEffectGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double EPSILON = 1.0e-9D;
    private static final ResourceLocation CURIO_SLOT_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest_head");

    private static final Set<AttributeEnchantmentType> ALL_ATTRIBUTE_ENCHANTMENTS =
            Set.of(AttributeEnchantmentType.values());
    private static final Set<AttributeEnchantmentType> BASIC_MAGIC_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );
    private static final Set<AttributeEnchantmentType> STAFFRIFLE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.SURGE,
            AttributeEnchantmentType.TENSE
    );

    private AttributeEnchantmentEffectGameTestScenarios() {
    }

    static void attributeEnchantmentsApplyInMainhand(GameTestHelper helper) {
        assertEffectsForTarget(helper, TargetKind.MAINHAND);
    }

    static void attributeEnchantmentsApplyInOffhand(GameTestHelper helper) {
        assertEffectsForTarget(helper, TargetKind.OFFHAND);
    }

    static void attributeEnchantmentsApplyOnArmor(GameTestHelper helper) {
        assertEffectsForTarget(helper, TargetKind.ARMOR);
    }

    static void attributeEnchantmentsApplyInCurios(GameTestHelper helper) {
        assertEffectsForTarget(helper, TargetKind.CURIO);
    }

    static void attributeEnchantmentsMergeWithExistingAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0));
            var cases = createCases(helper);
            var schoolPowerAttributes = resolveSchoolPowerAttributes();
            var coveredTypes = EnumSet.noneOf(AttributeEnchantmentType.class);

            for (var equipmentCase : cases) {
                for (var type : equipmentCase.enchantments()) {
                    var stacks = createLevelStacks(helper, equipmentCase, type);
                    var baseModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelZero());
                    var levelOneModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelOne());
                    var levelTwoModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelTwo());
                    var attribute = resolveEffectAttribute(
                            helper,
                            equipmentCase,
                            type,
                            stacks,
                            baseModifiers,
                            levelOneModifiers,
                            schoolPowerAttributes
                    );
                    var baseMatching = matchingModifiers(baseModifiers, attribute, type.operation());
                    if (baseMatching.isEmpty()) {
                        continue;
                    }

                    var baseAmount = sumAmounts(baseMatching);
                    assertSingleMergedModifier(
                            helper,
                            equipmentCase,
                            type,
                            levelOneModifiers,
                            attribute,
                            baseAmount + type.amountPerLevel(),
                            1
                    );
                    assertSingleMergedModifier(
                            helper,
                            equipmentCase,
                            type,
                            levelTwoModifiers,
                            attribute,
                            baseAmount + type.amountPerLevel() * 2.0D,
                            2
                    );
                    coveredTypes.add(type);
                }
            }

            helper.assertTrue(coveredTypes.equals(EnumSet.allOf(AttributeEnchantmentType.class)),
                    "Missing candidates for merging with existing attributes: missing="
                            + EnumSet.complementOf(coveredTypes) + ", covered=" + coveredTypes);
        });
    }

    private static void assertEffectsForTarget(GameTestHelper helper, TargetKind targetKind) {
        helper.succeedIf(() -> {
            var wearer = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0));
            var schoolPowerAttributes = resolveSchoolPowerAttributes();
            var targetCases = createCases(helper).stream()
                    .filter(equipmentCase -> equipmentCase.target().kind() == targetKind)
                    .toList();
            helper.assertFalse(targetCases.isEmpty(), "No attribute enchantment test cases found for: " + targetKind);

            for (var equipmentCase : targetCases) {
                for (var type : equipmentCase.enchantments()) {
                    assertEffect(helper, wearer, equipmentCase, type, schoolPowerAttributes);
                }
            }
        });
    }

    private static void assertEffect(
            GameTestHelper helper,
            LivingEntity wearer,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type,
            Set<Holder<Attribute>> schoolPowerAttributes
    ) {
        var stacks = createLevelStacks(helper, equipmentCase, type);
        var baseModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelZero());
        var levelOneModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelOne());
        var levelTwoModifiers = equipmentCase.target().getModifiers(wearer, stacks.levelTwo());
        var attribute = resolveEffectAttribute(
                helper,
                equipmentCase,
                type,
                stacks,
                baseModifiers,
                levelOneModifiers,
                schoolPowerAttributes
        );

        assertOnlyExpectedAmountChanged(
                helper,
                equipmentCase,
                type,
                ModifierSnapshot.from(baseModifiers),
                ModifierSnapshot.from(levelOneModifiers),
                attribute,
                type.amountPerLevel(),
                1
        );
        assertOnlyExpectedAmountChanged(
                helper,
                equipmentCase,
                type,
                ModifierSnapshot.from(baseModifiers),
                ModifierSnapshot.from(levelTwoModifiers),
                attribute,
                type.amountPerLevel() * 2.0D,
                2
        );

        for (var wrongSlot : EquipmentSlot.values()) {
            if (equipmentCase.target().usesVanillaSlot(wrongSlot)) {
                continue;
            }
            assertUnchangedInWrongSlot(helper, equipmentCase, type, stacks, wrongSlot);
        }
    }

    private static Holder<Attribute> resolveEffectAttribute(
            GameTestHelper helper,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type,
            LevelStacks stacks,
            Multimap<Holder<Attribute>, AttributeModifier> baseModifiers,
            Multimap<Holder<Attribute>, AttributeModifier> levelOneModifiers,
            Set<Holder<Attribute>> schoolPowerAttributes
    ) {
        if (type != AttributeEnchantmentType.ATTUNEMENT) {
            var attribute = type.resolveAttribute(stacks.levelZero());
            helper.assertTrue(attribute != null,
                    describeCase(equipmentCase, type) + " could not resolve the target attribute");
            return attribute;
        }

        // GameTestでは注入元のschoolがEvocationとして復元される場合があるため、school IDを期待値にしない。
        // 同一Stackのレベル差分から、実際に増えた登録済みschool power Attributeを追跡する。
        var baseSnapshot = ModifierSnapshot.from(baseModifiers);
        var levelOneSnapshot = ModifierSnapshot.from(levelOneModifiers);
        var candidates = schoolPowerAttributes.stream()
                .filter(attribute -> amountsDifferBy(
                        baseSnapshot,
                        levelOneSnapshot,
                        new ModifierKey(attribute, type.operation()),
                        type.amountPerLevel()
                ))
                .toList();
        helper.assertTrue(candidates.size() == 1,
                describeCase(equipmentCase, type)
                        + " did not resolve exactly one increased school power attribute: candidates="
                        + describeAttributes(candidates)
                        + ", base=" + describeModifiers(baseModifiers)
                        + ", level1=" + describeModifiers(levelOneModifiers));
        return candidates.get(0);
    }

    private static void assertOnlyExpectedAmountChanged(
            GameTestHelper helper,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type,
            ModifierSnapshot base,
            ModifierSnapshot enchanted,
            Holder<Attribute> expectedAttribute,
            double expectedDelta,
            int level
    ) {
        var expectedKey = new ModifierKey(expectedAttribute, type.operation());
        var changedKeys = changedAmountKeys(base, enchanted);
        helper.assertTrue(changedKeys.equals(Set.of(expectedKey)),
                describeCase(equipmentCase, type) + " level=" + level
                        + " changed unexpected attributes: expected=" + describeKey(expectedKey)
                        + ", changed=" + changedKeys.stream().map(AttributeEnchantmentEffectGameTestScenarios::describeKey).toList()
                        + ", base=" + base.describe() + ", enchanted=" + enchanted.describe());
        helper.assertTrue(amountsDifferBy(base, enchanted, expectedKey, expectedDelta),
                describeCase(equipmentCase, type) + " level=" + level
                        + " has an incorrect increase: expectedDelta=" + expectedDelta
                        + ", actualDelta=" + (enchanted.amount(expectedKey) - base.amount(expectedKey)));
    }

    private static void assertUnchangedInWrongSlot(
            GameTestHelper helper,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type,
            LevelStacks stacks,
            EquipmentSlot wrongSlot
    ) {
        var base = ModifierSnapshot.from(modifiersForSlot(stacks.levelZero(), wrongSlot));
        var levelOne = ModifierSnapshot.from(modifiersForSlot(stacks.levelOne(), wrongSlot));
        var levelTwo = ModifierSnapshot.from(modifiersForSlot(stacks.levelTwo(), wrongSlot));
        helper.assertTrue(base.sameAs(levelOne) && base.sameAs(levelTwo),
                describeCase(equipmentCase, type) + " was applied to the wrong equipment slot: wrongSlot=" + wrongSlot
                        + ", base=" + base.describe() + ", level1=" + levelOne.describe()
                        + ", level2=" + levelTwo.describe());
    }

    private static void assertSingleMergedModifier(
            GameTestHelper helper,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type,
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            Holder<Attribute> attribute,
            double expectedAmount,
            int level
    ) {
        var matching = matchingModifiers(modifiers, attribute, type.operation());
        helper.assertTrue(matching.size() == 1,
                describeCase(equipmentCase, type) + " level=" + level
                        + " was not merged into one modifier with the existing attribute: attribute="
                        + BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()) + ", matching=" + matching
                        + ", modifiers=" + describeModifiers(modifiers));
        helper.assertTrue(Math.abs(matching.get(0).amount() - expectedAmount) < EPSILON,
                describeCase(equipmentCase, type) + " level=" + level
                        + " has an incorrect merged amount: expected=" + expectedAmount
                        + ", actual=" + matching.get(0).amount());
    }

    private static LevelStacks createLevelStacks(
            GameTestHelper helper,
            EquipmentCase equipmentCase,
            AttributeEnchantmentType type
    ) {
        var base = createInitializedStack(equipmentCase);
        var levelOne = base.copy();
        var levelTwo = base.copy();
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var enchantment = enchantments.getOrThrow(type.enchantmentKey());
        levelOne.enchant(enchantment, 1);
        levelTwo.enchant(enchantment, 2);
        return new LevelStacks(base, levelOne, levelTwo);
    }

    private static ItemStack createInitializedStack(EquipmentCase equipmentCase) {
        var stack = createInitializedPresetStack(equipmentCase.item());
        if (equipmentCase.item() instanceof ScrollcasterGauntlet) {
            ScrollcasterGauntlet.setCalibrationScroll(
                    stack,
                    0,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            ScrollcasterGauntlet.setSelectedScrollIndex(stack, 0);
        }
        if (equipmentCase.enchantments().contains(AttributeEnchantmentType.ATTUNEMENT)
                && MagicTools.getImbuedSpellSchool(stack) == null) {
            ISpellContainer.createImbuedContainer(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(),
                    1,
                    stack
            );
        }
        return stack;
    }

    private static List<EquipmentCase> createCases(GameTestHelper helper) {
        var cases = new ArrayList<EquipmentCase>();
        var coveredItems = new HashSet<Item>();
        var registeredItems = ItemRegistry.ITEMS.getEntries().stream()
                .map(holder -> holder.get())
                .sorted(Comparator.comparing(item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item))))
                .toList();

        for (var item : registeredItems) {
            if (item instanceof AbstractSpellGunItem) {
                addCase(cases, coveredItems, item, ModifierTarget.mainhand(), ALL_ATTRIBUTE_ENCHANTMENTS);
            } else if (item instanceof AbstractSwingcastStaffItem) {
                addCase(cases, coveredItems, item, ModifierTarget.mainhand(), BASIC_MAGIC_ENCHANTMENTS);
            } else if (item instanceof AbstractOffhandMagicItem) {
                addCase(cases, coveredItems, item, ModifierTarget.offhand(), ALL_ATTRIBUTE_ENCHANTMENTS);
            } else if (item instanceof StealthRuneArmorItem && item instanceof ArmorItem armorItem) {
                addCase(cases, coveredItems, item, ModifierTarget.armor(armorItem.getType().getSlot()),
                        BASIC_MAGIC_ENCHANTMENTS);
            }
        }

        addCase(cases, coveredItems, ItemRegistry.MITHRIL_FREECAST_STAFF.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.REVOLVERCAST_STAFF.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(), ModifierTarget.mainhand(),
                STAFFRIFLE_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.SCROLLCASTER_GAUNTLET.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.CHARGECAST_CATALYSTBOOK.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.SPELL_SIDE_EDGE.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get(), ModifierTarget.mainhand(),
                BASIC_MAGIC_ENCHANTMENTS);
        addCase(cases, coveredItems, ItemRegistry.BULWARK_GREATSHIELD.get(), ModifierTarget.offhand(), Set.of(
                AttributeEnchantmentType.REFLUX,
                AttributeEnchantmentType.RESERVOIR
        ));
        addCase(cases, coveredItems, ItemRegistry.PARRYCAST_BUCKLER.get(), ModifierTarget.offhand(), Set.of(
                AttributeEnchantmentType.ALACRITY,
                AttributeEnchantmentType.TENSE
        ));
        addCase(cases, coveredItems, ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                ModifierTarget.armor(EquipmentSlot.CHEST), Set.of(
                        AttributeEnchantmentType.SURGE,
                        AttributeEnchantmentType.ATTUNEMENT
                ));
        addCase(cases, coveredItems, ItemRegistry.ENCHANTED_CIRCLET.get(), ModifierTarget.curio(),
                ALL_ATTRIBUTE_ENCHANTMENTS);

        var unclassified = registeredItems.stream()
                .filter(item -> item instanceof AttributeEnchantmentPolicy)
                .filter(item -> !coveredItems.contains(item))
                .filter(item -> !(item instanceof ManaForceBlade))
                .filter(item -> !(item instanceof ElementMaidenRobeItem robe) || robe.hasImbueSlot())
                .map(item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item)))
                .toList();
        helper.assertTrue(unclassified.isEmpty(),
                "Unclassified items remain in the attribute enchantment effect tests: " + unclassified);
        return cases;
    }

    private static void addCase(
            List<EquipmentCase> cases,
            Set<Item> coveredItems,
            Item item,
            ModifierTarget target,
            Set<AttributeEnchantmentType> enchantments
    ) {
        if (coveredItems.add(item)) {
            cases.add(new EquipmentCase(item, target, enchantments));
        }
    }

    private static Set<Holder<Attribute>> resolveSchoolPowerAttributes() {
        var attributes = new LinkedHashSet<Holder<Attribute>>();
        for (var school : SchoolRegistry.REGISTRY.stream().toList()) {
            var attribute = MagicTools.resolveSchoolPowerAttribute(school);
            if (attribute != null && attribute != AttributeRegistry.SPELL_POWER.value()) {
                attributes.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
            }
        }
        return Set.copyOf(attributes);
    }

    private static List<AttributeModifier> matchingModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        return modifiers.get(attribute).stream()
                .filter(modifier -> modifier.operation() == operation)
                .toList();
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> modifiersForSlot(
            ItemStack stack,
            EquipmentSlot slot
    ) {
        var builder = com.google.common.collect.ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        for (var entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.slot().test(slot)) {
                builder.put(entry.attribute(), entry.modifier());
            }
        }
        return builder.build();
    }

    private static double sumAmounts(Collection<AttributeModifier> modifiers) {
        return modifiers.stream().mapToDouble(AttributeModifier::amount).sum();
    }

    private static boolean amountsDifferBy(
            ModifierSnapshot base,
            ModifierSnapshot enchanted,
            ModifierKey key,
            double expectedDelta
    ) {
        return Math.abs((enchanted.amount(key) - base.amount(key)) - expectedDelta) < EPSILON;
    }

    private static Set<ModifierKey> changedAmountKeys(ModifierSnapshot base, ModifierSnapshot enchanted) {
        var keys = new LinkedHashSet<ModifierKey>();
        keys.addAll(base.values().keySet());
        keys.addAll(enchanted.values().keySet());
        keys.removeIf(key -> Math.abs(base.amount(key) - enchanted.amount(key)) < EPSILON);
        return keys;
    }

    private static String describeCase(EquipmentCase equipmentCase, AttributeEnchantmentType type) {
        return "item=" + BuiltInRegistries.ITEM.getKey(equipmentCase.item())
                + ", target=" + equipmentCase.target()
                + ", enchantment=" + type.enchantmentKey().location();
    }

    private static String describeAttributes(Collection<Holder<Attribute>> attributes) {
        return attributes.stream().map(attribute -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()))
                .toList().toString();
    }

    private static String describeKey(ModifierKey key) {
        return BuiltInRegistries.ATTRIBUTE.getKey(key.attribute().value()) + "@" + key.operation();
    }

    private enum TargetKind {
        MAINHAND,
        OFFHAND,
        ARMOR,
        CURIO
    }

    private record EquipmentCase(
            Item item,
            ModifierTarget target,
            Set<AttributeEnchantmentType> enchantments
    ) {
    }

    private record ModifierTarget(TargetKind kind, @Nullable EquipmentSlot slot) {
        static ModifierTarget mainhand() {
            return new ModifierTarget(TargetKind.MAINHAND, EquipmentSlot.MAINHAND);
        }

        static ModifierTarget offhand() {
            return new ModifierTarget(TargetKind.OFFHAND, EquipmentSlot.OFFHAND);
        }

        static ModifierTarget armor(EquipmentSlot slot) {
            return new ModifierTarget(TargetKind.ARMOR, slot);
        }

        static ModifierTarget curio() {
            return new ModifierTarget(TargetKind.CURIO, null);
        }

        Multimap<Holder<Attribute>, AttributeModifier> getModifiers(LivingEntity wearer, ItemStack stack) {
            if (kind != TargetKind.CURIO) {
                return modifiersForSlot(stack, slot);
            }
            var curio = (ICurioItem) stack.getItem();
            var context = new SlotContext(CuriosSlotConstants.HEAD, wearer, 0, false, true);
            return curio.getAttributeModifiers(context, CURIO_SLOT_ID, stack);
        }

        boolean usesVanillaSlot(EquipmentSlot candidate) {
            return slot == candidate;
        }
    }

    private record LevelStacks(ItemStack levelZero, ItemStack levelOne, ItemStack levelTwo) {
    }

    private record ModifierKey(Holder<Attribute> attribute, AttributeModifier.Operation operation) {
    }

    private record ModifierValue(double amount, int count) {
    }

    private record ModifierSnapshot(Map<ModifierKey, ModifierValue> values) {
        static ModifierSnapshot from(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
            var mutable = new LinkedHashMap<ModifierKey, ModifierValue>();
            for (var entry : modifiers.entries()) {
                var key = new ModifierKey(entry.getKey(), entry.getValue().operation());
                var previous = mutable.getOrDefault(key, new ModifierValue(0.0D, 0));
                mutable.put(key, new ModifierValue(
                        previous.amount() + entry.getValue().amount(),
                        previous.count() + 1
                ));
            }
            return new ModifierSnapshot(Map.copyOf(mutable));
        }

        double amount(ModifierKey key) {
            return values.getOrDefault(key, new ModifierValue(0.0D, 0)).amount();
        }

        boolean sameAs(ModifierSnapshot other) {
            var keys = new LinkedHashSet<ModifierKey>();
            keys.addAll(values.keySet());
            keys.addAll(other.values.keySet());
            for (var key : keys) {
                var left = values.getOrDefault(key, new ModifierValue(0.0D, 0));
                var right = other.values.getOrDefault(key, new ModifierValue(0.0D, 0));
                if (left.count() != right.count() || Math.abs(left.amount() - right.amount()) >= EPSILON) {
                    return false;
                }
            }
            return true;
        }

        String describe() {
            return values.entrySet().stream()
                    .map(entry -> describeKey(entry.getKey()) + "=" + entry.getValue().amount()
                            + "x" + entry.getValue().count())
                    .toList()
                    .toString();
        }
    }
}
