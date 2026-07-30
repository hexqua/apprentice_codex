package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeConfigState;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeGuardLogic;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

final class ManaForceBladeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ManaForceBladeGameTestScenarios() {
    }
    static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, stack, spell, 1);
            stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.ATTUNEMENT), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            var upgradeKey = findUpgradeKeyForPowerAttribute(attunementAttribute);
            helper.assertTrue(upgradeKey != null,
                    "Mana Force Blade test could not resolve a matching upgrade orb for " + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute));

            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    upgradeKey,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    stack.getItem().getDefaultAttributeModifiers(stack)
            );
            NeoForge.EVENT_BUS.post(event);
            var modifiers = toModifierMultimap(event.build());

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D + AttributeEnchantmentType.ATTUNEMENT.amountPerLevel(),
                    "Mana Force Blade Attunement and matching upgrade should merge into one display modifier"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }

    static void manaForceBladeAppliesSurgeAndAttunementAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();

            var surgeStack = new ItemStack(item);
            item.initializeSpellContainer(surgeStack);
            surgeStack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SURGE), 1);
            var surgeModifiers = toModifierMultimap(surgeStack.getAttributeModifiers());
            assertSingleModifierAmount(
                    helper,
                    surgeModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.02D,
                    "Mana Force Blade Surge should add spell power"
                            + " modifiers=" + describeModifiers(surgeModifiers)
            );

            var effectiveSurgeSpellPower = sumEffectiveModifierAmount(
                    surgeStack,
                    EquipmentSlot.MAINHAND,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(effectiveSurgeSpellPower - 0.02D) < 1.0e-9D,
                    "Mana Force Blade Surge should apply spell power in main hand"
                            + " amount=" + effectiveSurgeSpellPower
                            + " modifiers=" + describeModifiers(surgeModifiers));

            var attunementStack = new ItemStack(item);
            item.initializeSpellContainer(attunementStack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, attunementStack, spell, 1);
            attunementStack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.ATTUNEMENT), 1);
            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(attunementStack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade Attunement test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade Attunement test could not resolve the spell power attribute: " + imbuedSchool.getId());
            var attunementModifiers = toModifierMultimap(attunementStack.getAttributeModifiers());
            assertSingleModifierAmount(
                    helper,
                    attunementModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    AttributeEnchantmentType.ATTUNEMENT.amountPerLevel(),
                    "Mana Force Blade Attunement should add imbued school spell power"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " modifiers=" + describeModifiers(attunementModifiers)
            );
            var effectiveAttunementSpellPower = sumEffectiveModifierAmount(
                    attunementStack,
                    EquipmentSlot.MAINHAND,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(effectiveAttunementSpellPower
                            - AttributeEnchantmentType.ATTUNEMENT.amountPerLevel()) < 1.0e-9D,
                    "Mana Force Blade Attunement should apply school spell power in main hand"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " amount=" + effectiveAttunementSpellPower
                            + " modifiers=" + describeModifiers(attunementModifiers));
        });
    }

    static void manaForceBladeSharpnessTooltipDamageScalesWithImbue(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_sharpness_tooltip_test");
            var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);

            var baseStack = new ItemStack(item);
            item.initializeSpellContainer(baseStack);
            var baseTooltipDamage = resolveManaForceBladeAttackDamageTooltip(helper, player, baseStack);

            var sharpnessStack = new ItemStack(item);
            item.initializeSpellContainer(sharpnessStack);
            sharpnessStack.enchant(sharpness, 1);
            var sharpnessTooltipDamage = resolveManaForceBladeAttackDamageTooltip(helper, player, sharpnessStack);
            var expectedSharpnessDamage = ManaForceBlade.resolveBladeAttackDamage(sharpnessStack);
            helper.assertTrue(Math.abs(sharpnessTooltipDamage - expectedSharpnessDamage) < 1.0e-4F,
                    "Mana Force Blade Sharpness tooltip should include unconditional damage enchantment"
                            + " expected=" + expectedSharpnessDamage
                            + " actual=" + sharpnessTooltipDamage
                            + " lines=" + describeTooltipLines(sharpnessStack, player));
            helper.assertTrue(sharpnessTooltipDamage >= baseTooltipDamage + 1.0F,
                    "Mana Force Blade Sharpness I should add at least +1 attack damage in tooltip"
                            + " base=" + baseTooltipDamage
                            + " sharpness=" + sharpnessTooltipDamage);

            var spellPower = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPower != null,
                    "Mana Force Blade Sharpness tooltip test could not resolve spell power attribute");
            if (spellPower != null) {
                spellPower.setBaseValue(1.5D);
            }

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            var imbuedBaseStack = new ItemStack(item);
            item.initializeSpellContainer(imbuedBaseStack);
            setSingleUnlockedSpell(helper, imbuedBaseStack, spell, 1);

            var imbuedSharpnessStack = new ItemStack(item);
            item.initializeSpellContainer(imbuedSharpnessStack);
            setSingleUnlockedSpell(helper, imbuedSharpnessStack, spell, 1);
            imbuedSharpnessStack.enchant(sharpness, 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(imbuedSharpnessStack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade Sharpness tooltip test could not resolve imbued school");
            var schoolPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(schoolPowerAttribute != null,
                    "Mana Force Blade Sharpness tooltip test could not resolve school power attribute");
            var schoolPower = schoolPowerAttribute == null
                    ? null
                    : player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute));
            helper.assertTrue(schoolPower != null,
                    "Mana Force Blade Sharpness tooltip test could not resolve player school power instance");
            if (schoolPower != null) {
                schoolPower.setBaseValue(1.2D);
            }

            var imbuedBaseTooltipDamage = resolveManaForceBladeAttackDamageTooltip(helper, player, imbuedBaseStack);
            var imbuedSharpnessTooltipDamage = resolveManaForceBladeAttackDamageTooltip(helper, player, imbuedSharpnessStack);
            var expectedImbuedSharpnessDamage = ManaForceBlade.resolveFinalAttackDamage(
                    player,
                    imbuedSharpnessStack,
                    ManaForceBladeConfigState.imbueDamageMultiplierScale()
            );
            helper.assertTrue(Math.abs(imbuedSharpnessTooltipDamage - expectedImbuedSharpnessDamage) < 1.0e-4F,
                    "Mana Force Blade imbued Sharpness tooltip should keep Sharpness inside the damage multiplier"
                            + " expected=" + expectedImbuedSharpnessDamage
                            + " actual=" + imbuedSharpnessTooltipDamage
                            + " lines=" + describeTooltipLines(imbuedSharpnessStack, player));

            var damageMultiplier = ManaForceBlade.resolveDamageMultiplier(
                    player,
                    imbuedSharpnessStack,
                    ManaForceBladeConfigState.imbueDamageMultiplierScale()
            );
            helper.assertTrue(Math.abs((imbuedSharpnessTooltipDamage - imbuedBaseTooltipDamage) - damageMultiplier) < 1.0e-4F,
                    "Mana Force Blade imbued tooltip should scale Sharpness I by the imbued damage multiplier"
                            + " base=" + imbuedBaseTooltipDamage
                            + " sharpness=" + imbuedSharpnessTooltipDamage
                            + " multiplier=" + damageMultiplier);
        });
    }

    static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_attack_mana_once_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Mana Force Blade attack mana test could not resolve player mana data");
            magicData.setMana(100.0F);

            var firstTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            var secondTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            item.hurtEnemy(stack, firstTarget, player);
            item.hurtEnemy(stack, secondTarget, player);

            var expectedMana = 100.0F
                    - jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackManaCost(player, stack);
            helper.assertTrue(Math.abs(magicData.getMana() - expectedMana) < 1.0e-4F,
                    "Mana Force Blade should spend attack mana once per tick even when multiple targets are hit"
                            + " expected=" + expectedMana
                            + " actual=" + magicData.getMana());
        });
    }

    static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_config_formula_test");
            var spellPower = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPower != null,
                    "Mana Force Blade config formula test could not resolve spell power attribute");
            if (spellPower != null) {
                spellPower.setBaseValue(1.5D);
            }

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade config formula test could not resolve imbued school");
            var schoolPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(schoolPowerAttribute != null,
                    "Mana Force Blade config formula test could not resolve school power attribute");
            var schoolPower = schoolPowerAttribute == null
                    ? null
                    : player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute));
            helper.assertTrue(schoolPower != null,
                    "Mana Force Blade config formula test could not resolve player school power instance");
            if (schoolPower != null) {
                schoolPower.setBaseValue(1.2D);
            }

            var baseDamage = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackDamage(stack);
            var damageMultiplier = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveDamageMultiplier(player, stack, 1.0F);
            helper.assertTrue(Math.abs(damageMultiplier - 1.8F) < 1.0e-4F,
                    "Mana Force Blade should multiply spell power and school power for imbued damage but got "
                            + damageMultiplier);
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.5F) - 0.9F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale should directly scale the final school multiplier");
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.0F) - 1.0F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale 0 should disable imbued damage changes");

            var fullManaCost = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 1.0F);
            helper.assertTrue(Math.abs(fullManaCost - baseDamage * 3.0F * 1.8F) < 1.0e-4F,
                    "Mana Force Blade full school mana scale should follow final imbued damage: " + fullManaCost);

            var halfSchoolManaCost = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.5F, 1.0F);
            helper.assertTrue(Math.abs(halfSchoolManaCost - baseDamage * 3.0F * 1.4F) < 1.0e-4F,
                    "Mana Force Blade half school mana scale should only halve the school-derived increase: "
                            + halfSchoolManaCost);

            var noSchoolManaCost = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.0F, 1.0F);
            helper.assertTrue(Math.abs(noSchoolManaCost - baseDamage * 3.0F) < 1.0e-4F,
                    "Mana Force Blade school mana scale 0 should ignore school multiplier for mana cost: "
                            + noSchoolManaCost);

            var disabledManaCost = jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 0.0F);
            helper.assertTrue(disabledManaCost == 0.0F,
                    "Mana Force Blade imbue damage scale 0 should also disable hit mana cost");
        });
    }

    static void manaForceBladeReleaseCooldownUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_release_cooldown_config_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    7,
                    0,
                    0
            )) {
                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
            }

            helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                    "Mana Force Blade release should apply server-configured cooldown");

            var disabledCooldownStack = new ItemStack(item);
            item.initializeSpellContainer(disabledCooldownStack);
            var disabledCooldownPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 1),
                    "mana_force_blade_release_cooldown_disabled_test");
            disabledCooldownPlayer.setItemInHand(InteractionHand.MAIN_HAND, disabledCooldownStack);
            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    0,
                    0,
                    0
            )) {
                item.releaseUsing(
                        disabledCooldownStack,
                        helper.getLevel(),
                        disabledCooldownPlayer,
                        item.getUseDuration(disabledCooldownStack, disabledCooldownPlayer)
                );
            }
            helper.assertFalse(disabledCooldownPlayer.getCooldowns().isOnCooldown(item),
                    "Mana Force Blade release cooldown config 0 should disable release cooldown");
        });
    }

    static void manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_release_cooldown_grace_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    7,
                    40,
                    1
            )) {
                ManaForceBladeGuardLogic.tryHandleGuard(player, stack, player.damageSources().generic(), true, false);

                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
                helper.assertFalse(player.getCooldowns().isOnCooldown(item),
                        "Mana Force Blade perfect guard grace should skip release cooldown once");

                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
                helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                        "Mana Force Blade perfect guard grace should not skip release cooldown more than once");
            }
        });
    }

    static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            var requiredExtraEnchantments = registryIdSet(
                    Enchantments.SURGE,
                    Enchantments.ATTUNEMENT,
                    Enchantments.WISDOM,
                    Enchantments.TRANSCENDENCE
            );
            addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, requiredExtraEnchantments);
            helper.assertFalse(stack.getItem() instanceof NonDamageableAnvilMergeItem,
                    "Mana Force Blade should not keep the non-damageable anvil merge hook");
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredExtraEnchantments,
                    null,
                    "Mana Force Blade"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    registryIdSet(Enchantments.REFLUX, Enchantments.RESERVOIR),
                    null,
                    "Mana Force Blade should reject mana pool/recovery enchantments"
            );
        });
    }

    private static double sumEffectiveModifierAmount(
            ItemStack stack,
            EquipmentSlot slot,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        var total = new double[1];
        stack.forEachModifier(slot, (actualAttribute, modifier) -> {
            if (actualAttribute.equals(attribute) && modifier.operation() == operation) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static float resolveManaForceBladeAttackDamageTooltip(
            GameTestHelper helper,
            Player player,
            ItemStack stack
    ) {
        var tooltipLines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.Default.NORMAL);
        for (var line : tooltipLines) {
            var translatableContents = findFirstTranslatableContents(line);
            if (translatableContents == null || !isAttackDamageTooltipLine(translatableContents)) {
                continue;
            }

            var damage = parseTooltipNumber(translatableContents.getArgs()[0]);
            helper.assertTrue(damage != null,
                    "Mana Force Blade attack damage tooltip should expose a numeric damage value"
                            + " line=" + line
                            + " lines=" + tooltipLines);
            return damage;
        }

        helper.assertTrue(false,
                "Mana Force Blade tooltip should contain a mainhand attack damage line"
                        + " lines=" + tooltipLines);
        return 0.0F;
    }

    @Nullable
    private static TranslatableContents findFirstTranslatableContents(Component component) {
        if (component.getContents() instanceof TranslatableContents translatableContents) {
            return translatableContents;
        }

        for (var sibling : component.getSiblings()) {
            var translatableContents = findFirstTranslatableContents(sibling);
            if (translatableContents != null) {
                return translatableContents;
            }
        }
        return null;
    }

    private static boolean isAttackDamageTooltipLine(TranslatableContents translatableContents) {
        if (!"attribute.modifier.equals.0".equals(translatableContents.getKey())) {
            return false;
        }

        var args = translatableContents.getArgs();
        if (args.length < 2 || !(args[1] instanceof Component attributeName)) {
            return false;
        }
        return attributeName.getContents() instanceof TranslatableContents attributeNameContents
                && Attributes.ATTACK_DAMAGE.value().getDescriptionId().equals(attributeNameContents.getKey());
    }

    @Nullable
    private static Float parseTooltipNumber(Object value) {
        var text = value instanceof Component component ? component.getString() : String.valueOf(value);
        try {
            return Float.parseFloat(text.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String describeTooltipLines(ItemStack stack, Player player) {
        return stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL).stream()
                .map(Component::getString)
                .collect(Collectors.joining(" | "));
    }
}
