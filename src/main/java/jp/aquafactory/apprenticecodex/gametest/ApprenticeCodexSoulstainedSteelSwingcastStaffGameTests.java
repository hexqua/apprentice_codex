package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaffConfigState;
import jp.aquafactory.apprenticecodex.network.packet.SyncSoulstainedSteelSwingcastStaffConfigPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSoulstainedSteelSwingcastStaffGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final ResourceLocation HEX_BOLT = ResourceLocation.fromNamespaceAndPath("malum", "hex_bolt");
    private static final ResourceLocation SPIRIT_INFUSION_RECIPE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "malum/spirit_infusion/soulstained_steel_swingcast_staff");
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
            var spiritInfusionRecipe = helper.getLevel().getRecipeManager().byKey(SPIRIT_INFUSION_RECIPE);
            if (ModList.get().isLoaded("malum")) {
                helper.assertTrue(spiritInfusionRecipe.isPresent(),
                        "Soulstained Steel Swingcast Staff Spirit Infusion recipe should be loaded with Malum");
            } else {
                helper.assertTrue(spiritInfusionRecipe.isEmpty(),
                        "Soulstained Steel Swingcast Staff Spirit Infusion recipe should not be loaded without Malum");
            }

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
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(19.99F) == 0,
                    "Less than 20 mana should not fire a Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(20.0F) == 1,
                    "20 mana should fire one Mnemonic Blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(59.99F) == 2,
                    "Less than 60 mana should fire two Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(60.0F) == 3,
                    "60 mana should fire three Mnemonic Blades");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(100.0F) == 3,
                    "Mnemonic Blade burst size should be capped at three");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(1.0D) - 20.0D) < 1.0e-6D,
                    "Charge Recovery Rate 1.0 should cost 20 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.5D) - 40.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0.5 should cost 40 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.01D) - 2000.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0.01 should cost 2000 mana per blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(0.0D) - 2000.0D) < 1.0e-6D,
                    "Charge Recovery Rate 0 should clamp to 0.01");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(19.99F, 20.0D) == 0,
                    "Mana below the dynamic cost should not fire a blade");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(20.0F, 20.0D) == 1,
                    "Mana equal to the dynamic cost should fire one blade");
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaff.resolveManaCost(12.5D, 0.5D) - 25.0D)
                            < 1.0e-6D,
                    "Configured base mana cost should be scaled by Charge Recovery Rate");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveBladeCount(0.0F, 0.0D) == 3,
                    "Zero configured mana cost should fire the full burst without mana");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(20.0D, 1.0D) == 60L,
                    "Default tooltip cost should show 60 mana for the full burst");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(20.0D, 0.5D) == 120L,
                    "Tooltip cost should reflect Charge Recovery Rate scaling");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(20.0D, 0.7D) == 86L,
                    "Fractional full-burst mana cost should round upward in the tooltip");
            helper.assertTrue(SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(0.0D, 0.5D) == 0L,
                    "Zero configured mana cost should resolve to the hidden tooltip sentinel");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffConfigSyncPreservesManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            SyncSoulstainedSteelSwingcastStaffConfigPacket.encode(
                    new SyncSoulstainedSteelSwingcastStaffConfigPacket(12.5D),
                    buffer
            );
            var decoded = SyncSoulstainedSteelSwingcastStaffConfigPacket.decode(buffer);
            helper.assertTrue(Math.abs(decoded.manaCostPerBlade() - 12.5D) < 1.0e-9D,
                    "Soulstained Steel Swingcast Staff config sync should preserve fractional mana cost");

            SoulstainedSteelSwingcastStaffConfigState.setManaCostPerBlade(7.5D);
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaffConfigState.manaCostPerBlade() - 7.5D)
                            < 1.0e-9D,
                    "Client config state should retain the synchronized mana cost");
            SoulstainedSteelSwingcastStaffConfigState.reset();
            helper.assertTrue(Math.abs(SoulstainedSteelSwingcastStaffConfigState.manaCostPerBlade() - 20.0D)
                            < 1.0e-9D,
                    "Client config state reset should restore the default mana cost");

            try (var ignored = ApprenticeCodexServerConfig
                    .useSoulstainedSteelSwingcastStaffConfigOverrideForGameTest(12.5D)) {
                helper.assertTrue(Math.abs(
                                ApprenticeCodexServerConfig.soulstainedSteelSwingcastStaffManaCostPerBlade() - 12.5D
                        ) < 1.0e-9D,
                        "Soulstained Steel Swingcast Staff should read the configured base mana cost");
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffRightClickUsesSelectedSpellAndDefersToPriorityOffhand(
            GameTestHelper helper
    ) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get());
            var amplifier = new ItemStack(ItemRegistry.SOULSTAINED_STEEL_SPELL_AMPLIFIER.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ISpellContainer.createImbuedContainer(spell, 1, amplifier);

            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 4, 0),
                    "soulstained_staff_right_click"
            );
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifier);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Soulstained Steel Swingcast Staff right-click test requires MagicData");
            magicData.setMana(1000.0F);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));

            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Soulstained Steel Swingcast Staff should resolve the spell imbued into the offhand amplifier");
            var result = staff.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Soulstained Steel Swingcast Staff right click should cast the selected spell but got "
                            + result.getResult());
            helper.assertTrue(ItemStack.isSameItemSameComponents(magicData.getPlayerCastingItem(), staff),
                    "Soulstained Steel Swingcast Staff should remain the casting item");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Soulstained Steel Swingcast Staff right click should mark the mainhand casting slot");
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Soulstained Steel Swingcast Staff right click should not create an Iron's spell container");

            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            var deferredResult = staff.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(deferredResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Soulstained Steel Swingcast Staff should defer to a priority offhand item but got "
                            + deferredResult.getResult());
            helper.assertFalse(magicData.isCasting(),
                    "Soulstained Steel Swingcast Staff should not cast before a priority offhand item");
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
                    MalumMnemonicBladeBridge.getChargeRecoveryRate(player)) - 40.0D) < 1.0e-6D,
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
        magicData.setMana(45.0F);
        var before = countHexBolts(helper);

        var triggered = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
        if (!MalumMnemonicBladeBridge.isAvailable()) {
            helper.assertFalse(triggered, "Soulstained Steel Swingcast Staff should stay inert without Malum");
            helper.assertTrue(Math.abs(magicData.getMana() - 45.0F) < 1.0e-6F,
                    "Missing Malum should not consume mana");
            helper.succeed();
            return;
        }

        helper.assertTrue(triggered, "Soulstained Steel Swingcast Staff should fire when Malum is loaded");
        helper.assertTrue(Math.abs(magicData.getMana() - 5.0F) < 1.0e-6F,
                "Two Mnemonic Blades should consume 40 mana");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(countHexBolts(helper) - before == 2,
                    "45 mana should spawn exactly two Malum Mnemonic Blades");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffZeroConfigFiresFreeFullBurst(GameTestHelper helper) {
        var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 4, 0),
                "soulstained_staff_free_burst"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Soulstained Steel Swingcast Staff free-burst test requires MagicData");
        magicData.setMana(0.0F);
        var before = countHexBolts(helper);

        boolean triggered;
        boolean repeatedInSameTick;
        try (var ignored = ApprenticeCodexServerConfig
                .useSoulstainedSteelSwingcastStaffConfigOverrideForGameTest(0.0D)) {
            triggered = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
            repeatedInSameTick = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
        }
        if (!MalumMnemonicBladeBridge.isAvailable()) {
            helper.assertFalse(triggered, "Soulstained Steel Swingcast Staff should stay inert without Malum");
            helper.succeed();
            return;
        }

        helper.assertTrue(triggered, "Zero configured mana cost should allow a free full burst");
        helper.assertFalse(repeatedInSameTick,
                "A client-provided charge bypass should not allow two free bursts in the same server tick");
        helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-6F,
                "A free full burst should not consume mana");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(countHexBolts(helper) - before == 3,
                    "Zero configured mana cost should spawn the full three-blade burst");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSwingcastStaffRejectsOverlappingBursts(GameTestHelper helper) {
        var item = (SoulstainedSteelSwingcastStaff) ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get();
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 4, 0),
                "soulstained_staff_burst_interval"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Soulstained Steel Swingcast Staff interval test requires MagicData");
        magicData.setMana(1000.0F);

        var firstTriggered = item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true);
        if (!MalumMnemonicBladeBridge.isAvailable()) {
            helper.assertFalse(firstTriggered, "Soulstained Steel Swingcast Staff should stay inert without Malum");
            helper.succeed();
            return;
        }
        helper.assertTrue(firstTriggered, "The first Mnemonic Blade burst should fire");

        helper.runAfterDelay(SoulstainedSteelSwingcastStaff.MIN_BURST_INTERVAL_TICKS - 1, () ->
                helper.assertFalse(
                        item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                        "A second burst should stay blocked until the previous 0/3/6-tick burst completes"
                ));
        helper.runAfterDelay(SoulstainedSteelSwingcastStaff.MIN_BURST_INTERVAL_TICKS, () -> {
            helper.assertTrue(
                    item.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "A second burst should be allowed after the seven-tick server interval"
            );
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
