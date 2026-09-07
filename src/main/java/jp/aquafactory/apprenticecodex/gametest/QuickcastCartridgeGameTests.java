package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.network.packet.ClientQuickcastCartridgePacket;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.SlotContext;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class QuickcastCartridgeGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void cartridgeStorageAndBench(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        var item = (QuickcastScrollCartridge) stack.getItem();
        var instant = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var longSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANTIS_LEAP.get();
        helper.assertTrue(QuickcastScrollCartridge.getEnabledCalibrationScrollSlotCount(stack) == 1,
                "Cartridge must start with one scroll slot");
        helper.assertFalse(item.trySetCalibrationAdjustment(stack, 0, new ItemStack(Items.DIAMOND)),
                "Only Scrollwoven Parchment may expand cartridge slots");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(item.trySetCalibrationAdjustment(stack, i, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Repeated parchment upgrades must be accepted");
        }
        helper.assertTrue(QuickcastScrollCartridge.getEnabledCalibrationScrollSlotCount(stack) == 4,
                "Three upgrades must provide four scroll slots");
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(instant));
        QuickcastScrollCartridge.setCalibrationScroll(stack, 3, createSpellScroll(longSpell));
        item.setSneakSelectionIndex(stack, 3);
        helper.assertTrue(QuickcastScrollCartridge.getSelectedSpellData(stack).getSpell() == longSpell,
                "Cartridge must allow selecting a long spell");
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_bench");
        var menu = createSpellCalibrationBenchMenuWithTarget(player, stack);
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 4, "Bench must expose cartridge scroll slots");
        helper.assertTrue(menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(createSpellScroll(longSpell)),
                "Bench must accept non-instant spells");
        item.trySetCalibrationAdjustment(stack, 2, ItemStack.EMPTY);
        helper.assertTrue(QuickcastScrollCartridge.getSelectedScrollIndex(stack) == 0,
                "Disabling the selected slot must select the first usable scroll");
        helper.assertFalse(QuickcastScrollCartridge.getCalibrationScroll(stack, 3).isEmpty(),
                "Disabled scroll contents must be retained");
        var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) stack.saveOptional(helper.getLevel().registryAccess()));
        helper.assertTrue(QuickcastScrollCartridge.getSelectedSpellData(restored).getSpell() == instant,
                "Selection must survive item serialization");
        helper.assertFalse(QuickcastScrollCartridge.getCalibrationScroll(restored, 3).isEmpty(),
                "Disabled contents must survive item serialization");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeEquipmentAndWheel(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_wheel");
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        var item = (QuickcastScrollCartridge) stack.getItem();
        for (String slot : new String[]{"back", "belt"}) {
            helper.assertTrue(stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", slot))),
                    "Cartridge must support back and belt slots");
        }
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "back", stack);
        helper.assertFalse(item.canEquip(new SlotContext("belt", player, 0, false, true), stack.copy()),
                "A second equipped cartridge must be rejected");
        item.trySetCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()));
        QuickcastScrollCartridge.setCalibrationScroll(stack, 1, createSpellScroll(SpellRegistry.FIREBOLT_SPELL.get()));
        var entries = new SpellSelectionManager(player).getSpellsForSlot(QuickcastCartridgeCasting.SLOT);
        helper.assertTrue(entries.size() == 1 && entries.getFirst().spellData.getSpell() == spell,
                "Only the selected scroll may enter the wheel");
        var option = new SpellSelectionManager(player).getAllSpells().stream()
                .filter(entry -> QuickcastCartridgeCasting.SLOT.equals(entry.slot)).findFirst().orElseThrow();
        helper.assertTrue(option.getCastSource() == CastSource.SPELLBOOK,
                "Cartridge wheel entry must use spellbook mana and cooldown rules");
        equipCurio(player, "back", ItemStack.EMPTY);
        helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(QuickcastCartridgeCasting.SLOT).isEmpty(),
                "Unequipped cartridges must not add wheel spells");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeBypassAndPowerLifecycle(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_power");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setMana(1000);
        var cooldowns = magic.getPlayerCooldowns();
        double normalPower = player.getAttributeValue(AttributeRegistry.SPELL_POWER);
        try {
            for (int remaining : new int[]{0, 50, 100}) {
                cooldowns.clearCooldowns();
                if (remaining > 0) cooldowns.addCooldown(spell, 100, remaining);
                var packet = new ClientQuickcastCartridgePacket(spell.getSpellResource(), new BlockTargetData(), 0, 0);
                helper.assertTrue(ClientQuickcastCartridgePacket.handleOnServer(packet, player),
                        "Dedicated key must start casting even on cooldown");
                helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_POWER)
                        - normalPower * (1 - remaining / 100.0)) < 0.00001,
                        "Final spell power must reflect the initial remaining cooldown fraction");
                cooldowns.tick(20);
                QuickcastCartridgeCasting.validate(player);
                helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_POWER)
                        - normalPower * (1 - remaining / 100.0)) < 0.00001,
                        "Penalty must not decrease during casting");
                spell.onServerCastComplete(player.level(), 1, player, magic, true);
                helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_POWER) - normalPower) < 0.00001,
                        "Completion or cancellation must remove the temporary power penalty");
            }
            cooldowns.addCooldown(spell, 100, 50);
            helper.assertFalse(spell.canBeCastedBy(1, CastSource.SPELLBOOK, magic, player).isSuccess(),
                    "Cooldown bypass must not escape the cartridge initiation call");
        } finally {
            Utils.serverSideCancelCast(player);
            QuickcastCartridgeCasting.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeFailurePreservesCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_failure");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.getPlayerCooldowns().addCooldown(spell, 100, 70);
        double normalPower = player.getAttributeValue(AttributeRegistry.SPELL_POWER);
        magic.setMana(0);
        helper.assertFalse(QuickcastCartridgeCasting.initiate(player), "Cartridge must still require mana");
        helper.assertTrue(magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId()).getCooldownRemaining() == 70,
                "Failed initiation must preserve the existing cooldown");
        helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == normalPower,
                "Failed initiation must remove power penalty");
        magic.setMana(1000);
        Consumer<SpellPreCastEvent> reject = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(reject);
        try {
            helper.assertFalse(QuickcastCartridgeCasting.initiate(player), "Precast cancellation must remain effective");
            helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == normalPower,
                    "Event rejection must remove power penalty");
            helper.assertTrue(magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId()).getCooldownRemaining() == 70,
                    "Event rejection must retain cooldown");
        } finally {
            NeoForge.EVENT_BUS.unregister(reject);
            QuickcastCartridgeCasting.clear(player);
        }
        equipCurio(player, "back", ItemStack.EMPTY);
        helper.assertFalse(QuickcastCartridgeCasting.initiate(player), "Unequipped items must not authorize casting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeWheelUsesSpecialCastAndConsumesMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_wheel_cast");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "belt", stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setMana(1000);
        magic.getPlayerCooldowns().addCooldown(spell, 100, 50);
        var manager = new SpellSelectionManager(player);
        var selection = manager.getAllSpells().stream().filter(entry -> QuickcastCartridgeCasting.SLOT.equals(entry.slot))
                .findFirst().orElseThrow();
        try {
            helper.assertTrue(Utils.serverSideInitiateQuickCast(player, selection.globalIndex),
                    "Wheel quickcast must route cartridge entry through cooldown bypass");
            helper.assertTrue(magic.getCastSource() == CastSource.SPELLBOOK,
                    "Wheel cast must consume mana as a spellbook cast");
            float before = magic.getMana();
            spell.castSpell(player.level(), 1, player, magic.getCastSource(), true);
            spell.onServerCastComplete(player.level(), 1, player, magic, false);
            helper.assertTrue(magic.getMana() < before, "Successful cartridge cast must spend mana");
            var cooldown = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            helper.assertTrue(cooldown != null && cooldown.getCooldownRemaining() == cooldown.getSpellCooldown(),
                    "Successful cartridge cast must restart the shared cooldown");
            helper.assertTrue(Utils.serverSideInitiateCast(player), "Selected wheel entry may immediately cast again");
            equipCurio(player, "belt", ItemStack.EMPTY);
            QuickcastCartridgeCasting.validate(player);
            helper.assertFalse(magic.isCasting(), "Unequipping must cancel the cartridge cast");
            helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == 1,
                    "Unequipping must remove the cartridge power penalty");
        } finally {
            Utils.serverSideCancelCast(player);
            QuickcastCartridgeCasting.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeLongAndContinuousCasts(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_cast_types");
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        try {
            for (var spell : new AbstractSpell[]{SpellRegistry.FIREBALL_SPELL.get(), SpellRegistry.FIRE_BREATH_SPELL.get()}) {
                QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
                magic.setMana(1000);
                magic.getPlayerCooldowns().addCooldown(spell, 100, 50);
                helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "Long and continuous spells must start on cooldown");
                helper.assertTrue(magic.getCastType() == spell.getCastType(), "Cartridge must retain the original cast type");
                helper.assertTrue(magic.getCastDurationRemaining() == spell.getEffectiveCastTime(1, player),
                        "Cartridge must retain normal cast duration");
                float before = magic.getMana();
                spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, false);
                if (spell.getCastType() == CastType.CONTINUOUS) {
                    spell.onServerCastTick(player.level(), 1, player, magic);
                    spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, false);
                    helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == 0.5,
                            "Continuous activations must retain the initial penalty");
                }
                helper.assertTrue(magic.getMana() < before, "Normal cast activations must spend mana");
                spell.onServerCastComplete(player.level(), 1, player, magic, true);
                helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == 1,
                        "Long and continuous completion must remove the penalty");
            }
        } finally {
            Utils.serverSideCancelCast(player);
            QuickcastCartridgeCasting.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeKeepsExistingRecastState(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_recast");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        // 他の発動元が既に作成した再詠唱状態を模し、開始時に回数を補充しないことを確認する。
        var recast = new io.redspace.ironsspellbooks.capabilities.magic.RecastInstance(
                spell.getSpellId(), 1, 3, 200, CastSource.SPELLBOOK, null);
        magic.getPlayerRecasts().forceAddRecast(recast);
        int remainingBefore = recast.getRemainingRecasts();
        magic.setMana(0);
        try {
            helper.assertTrue(QuickcastCartridgeCasting.initiate(player), "Existing recast must retain normal mana exemption");
            helper.assertTrue(magic.getPlayerRecasts().getRemainingRecastsForSpell(spell) == remainingBefore,
                    "Cartridge initiation must not replace existing recast count");
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            spell.onServerCastComplete(player.level(), 1, player, magic, false);
            helper.assertTrue(magic.getPlayerRecasts().getRemainingRecastsForSpell(spell) == remainingBefore - 1,
                    "Cartridge cast must decrement the existing recast normally");
        } finally {
            Utils.serverSideCancelCast(player);
            magic.getPlayerRecasts().removeAll(io.redspace.ironsspellbooks.capabilities.magic.RecastResult.DEATH);
            QuickcastCartridgeCasting.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cartridgeAllowsPowerIndependentAssistWings(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "cartridge_assist_wings");
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.ASSIST_WINGS.get();
        var stack = new ItemStack(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get());
        QuickcastScrollCartridge.setCalibrationScroll(stack, 0, createSpellScroll(spell));
        equipCurio(player, "back", stack);
        var magic = MagicData.getPlayerMagicData(player);
        player.setOnGround(true);
        magic.setMana(1000);
        magic.getPlayerCooldowns().addCooldown(spell, 100, 100);
        try {
            helper.assertTrue(QuickcastCartridgeCasting.initiate(player),
                    "Power-independent spells must remain castable at full cooldown");
            helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) == 0,
                    "Full cooldown must apply a 100 percent power reduction");
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(player.getDeltaMovement().y > 0, "Assist Wings must still produce its jump at zero spell power");
            spell.onServerCastComplete(player.level(), 1, player, magic, false);
        } finally {
            Utils.serverSideCancelCast(player);
            QuickcastCartridgeCasting.clear(player);
        }
        helper.succeed();
    }
}
