package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSoulstainedSteelSwingcastStaffGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final ResourceLocation HEX_BOLT = ResourceLocation.fromNamespaceAndPath("malum", "hex_bolt");
    private static final ResourceLocation CHARGE_RECOVERY_RATE_TEST_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soulstained_staff_charge_recovery_rate_test");
    private static final ResourceLocation CHARGE_RECOVERY_RATE =
            ResourceLocation.fromNamespaceAndPath("malum", "charge_recovery_rate");

    private ApprenticeCodexSoulstainedSteelSwingcastStaffGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffExposesRequestedItemContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertFalse(stack.isDamageableItem(), "Soulstained Steel Swingcast Staff should have infinite durability");
            helper.assertTrue(stack.getRarity() == Rarity.COMMON,
                    "Soulstained Steel Swingcast Staff should have common rarity");
            helper.assertTrue(item.getEnchantmentValue(stack) == SoulstainedSteelSwingcastStaff.ENCHANTMENT_VALUE,
                    "Soulstained Steel Swingcast Staff enchantment value should match the Soulstained Steel Spell Amplifier");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Soulstained Steel Swingcast Staff should not use an Iron's spell container");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(item.builtInRegistryHolder().key().location()).isEmpty(),
                    "Soulstained Steel Swingcast Staff should not have a recipe yet");

            var modifiers = item.getDefaultAttributeModifiers(stack).modifiers();
            var attackDamage = modifiers.stream()
                    .filter(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                            && entry.slot() == EquipmentSlotGroup.MAINHAND)
                    .mapToDouble(entry -> entry.modifier().amount())
                    .sum();
            var attackSpeed = modifiers.stream()
                    .filter(entry -> entry.attribute().equals(Attributes.ATTACK_SPEED)
                            && entry.slot() == EquipmentSlotGroup.MAINHAND)
                    .mapToDouble(entry -> entry.modifier().amount())
                    .sum();
            helper.assertTrue(Math.abs(1.0D + attackDamage - 3.0D) < 1.0e-6D,
                    "Soulstained Steel Swingcast Staff displayed attack damage should be 3");
            helper.assertTrue(Math.abs(4.0D + attackSpeed - 1.6D) < 1.0e-6D,
                    "Soulstained Steel Swingcast Staff displayed attack speed should be 1.6");

            var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.Default.NORMAL);
            helper.assertTrue(tooltip.size() == 3,
                    "Soulstained Steel Swingcast Staff should append one ability line after the common weapon help");
            helper.assertTrue(tooltip.get(2).getContents() instanceof TranslatableContents contents
                            && "item.apprenticecodex.soulstained_steel_swingcast_staff.desc".equals(contents.getKey()),
                    "Soulstained Steel Swingcast Staff ability tooltip should occupy the Swingcast common-help position");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffResolvesBladeCountFromMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(9.99F) == 0,
                    "Less than 10 mana should not fire a Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(10.0F) == 1,
                    "10 mana should fire one Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(29.99F) == 2,
                    "Less than 30 mana should fire two Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(30.0F) == 3,
                    "30 mana should fire three Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(100.0F) == 3,
                    "Mnemonic Blade burst size should be capped at three");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(1.0D) - 10.0D) < 1.0e-6D,
                    "Charge Recovery Rate 1.0 should cost 10 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.5D) - 20.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0.5 should cost 20 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.01D) - 1000.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0.01 should cost 1000 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.0D) - 1000.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0 should clamp to 0.01");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(19.99F, 20.0D) == 0,
                    "Mana below the dynamic cost should not fire a blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(20.0F, 20.0D) == 1,
                    "Mana equal to the dynamic cost should fire one blade");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffReadsMalumChargeRecoveryRate(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 4, 0),
                "soulstained_staff_charge_recovery"
        );
        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(CHARGE_RECOVERY_RATE)
                .map(BuiltInRegistries.ATTRIBUTE::wrapAsHolder)
                .map(player::getAttribute)
                .orElse(null);
        if (attribute == null) {
            helper.succeed();
            return;
        }

        attribute.addTransientModifier(new AttributeModifier(
                CHARGE_RECOVERY_RATE_TEST_MODIFIER,
                -0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        helper.succeedIf(() -> {
            helper.assertTrue(Math.abs(MalumMnemonicBladeBridge.getChargeRecoveryRate(player) - 0.5D) < 1.0e-6D,
                    "Malum Charge Recovery Rate should resolve the -50% modifier as 0.5");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(
                    MalumMnemonicBladeBridge.getChargeRecoveryRate(player)) - 20.0D) < 1.0e-6D,
                    "Soulstained Steel Swingcast Staff should use the resolved Charge Recovery Rate");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffFiresAvailableMnemonicBlades(GameTestHelper helper) {
        var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
        var stack = new ItemStack(item);
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 4, 0),
                "soulstained_staff_burst"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Soulstained Steel Swingcast Staff test requires MagicData");
        magicData.setMana(25.0F);
        var before = countHexBolts(helper);

        var triggered = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
        if (!MalumMnemonicBladeBridge.isAvailable()) {
            helper.assertFalse(triggered, "Soulstained Steel Swingcast Staff should stay inert without Malum");
            helper.assertTrue(Math.abs(magicData.getMana() - 25.0F) < 1.0e-6F,
                    "Missing Malum should not consume mana");
            helper.succeed();
            return;
        }

        helper.assertTrue(triggered, "Soulstained Steel Swingcast Staff should fire when Malum is loaded");
        helper.assertTrue(Math.abs(magicData.getMana() - 5.0F) < 1.0e-6F,
                "Two Mnemonic Blades should consume 20 mana");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(countHexBolts(helper) - before == 2,
                    "25 mana should spawn exactly two Malum Mnemonic Blades");
            helper.succeed();
        });
    }

    private static int countHexBolts(GameTestHelper helper) {
        var count = 0;
        for (var entity : helper.getLevel().getAllEntities()) {
            if (HEX_BOLT.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
                count++;
            }
        }
        return count;
    }
}
