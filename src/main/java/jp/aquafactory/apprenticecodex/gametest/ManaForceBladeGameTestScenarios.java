package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.registries.ForgeRegistries;

final class ManaForceBladeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ManaForceBladeGameTestScenarios() {
    }

    static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, stack, spell, 1);
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            var upgradeKey = findUpgradeKeyForPowerAttribute(attunementAttribute);
            helper.assertTrue(upgradeKey != null,
                    "Mana Force Blade test could not resolve a matching upgrade orb for " + ForgeRegistries.ATTRIBUTES.getKey(attunementAttribute));

            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    upgradeKey,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    EquipmentSlot.MAINHAND,
                    item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            assertSingleModifierAmount(
                    helper,
                    event.getModifiers().get(attunementAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.09D,
                    "Mana Force Blade Attunement and matching upgrade should merge into one display modifier"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + ForgeRegistries.ATTRIBUTES.getKey(attunementAttribute)
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers())
            );
        });
    }
    static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
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
                    - jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(player, stack);
            helper.assertTrue(Math.abs(magicData.getMana() - expectedMana) < 1.0e-4F,
                    "Mana Force Blade should spend attack mana once per tick even when multiple targets are hit"
                            + " expected=" + expectedMana
                            + " actual=" + magicData.getMana());
        });
    }
    static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_config_formula_test");
            var spellPower = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
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
            var schoolPower = schoolPowerAttribute == null ? null : player.getAttribute(schoolPowerAttribute);
            helper.assertTrue(schoolPower != null,
                    "Mana Force Blade config formula test could not resolve player school power instance");
            if (schoolPower != null) {
                schoolPower.setBaseValue(1.2D);
            }

            var baseDamage = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackDamage(stack);
            var damageMultiplier = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 1.0F);
            helper.assertTrue(Math.abs(damageMultiplier - 1.8F) < 1.0e-4F,
                    "Mana Force Blade should multiply spell power and school power for imbued damage but got "
                            + damageMultiplier);
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.5F) - 0.9F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale should directly scale the final school multiplier");
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.0F) - 1.0F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale 0 should disable imbued damage changes");

            var fullManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 1.0F);
            helper.assertTrue(Math.abs(fullManaCost - baseDamage * 3.0F * 1.8F) < 1.0e-4F,
                    "Mana Force Blade full school mana scale should follow final imbued damage: " + fullManaCost);

            var halfSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.5F, 1.0F);
            helper.assertTrue(Math.abs(halfSchoolManaCost - baseDamage * 3.0F * 1.4F) < 1.0e-4F,
                    "Mana Force Blade half school mana scale should only halve the school-derived increase: "
                            + halfSchoolManaCost);

            var noSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.0F, 1.0F);
            helper.assertTrue(Math.abs(noSchoolManaCost - baseDamage * 3.0F) < 1.0e-4F,
                    "Mana Force Blade school mana scale 0 should ignore school multiplier for mana cost: "
                            + noSchoolManaCost);

            var disabledManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 0.0F);
            helper.assertTrue(disabledManaCost == 0.0F,
                    "Mana Force Blade imbue damage scale 0 should also disable hit mana cost");
        });
    }
    static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedManaForceBladeEnchantments(stack),
                    "Mana Force Blade"
            );
        });
    }
}
