package jp.aquafactory.apprenticecodex.gametest;

import com.sammy.malum.common.data.attachment.StaffAbilityData;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerEvents;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerLogic;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaSoulTransducerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ManaSoulTransducerGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void blackCrystalManualAdvancementLoadsWithoutDisplay(GameTestHelper helper) {
        var advancement = helper.getLevel().getServer().getAdvancements().get(
                AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED);
        helper.assertTrue(advancement != null, "Missing Black Crystal manual advancement");
        helper.assertTrue(advancement.value().display().isEmpty(),
                "Black Crystal manual advancement must not appear in the advancement UI");
        helper.assertTrue(advancement.value().criteria().containsKey(
                        AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED_CRITERION),
                "Black Crystal manual advancement must contain the criterion awarded by the packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void transferUsesPositiveBonusesAndSoftCap(GameTestHelper helper) {
        for (double coefficient : new double[]{0, 0.8, 1}) {
            close(helper, ManaSoulTransducerLogic.durationModifier(0.5, coefficient), 0, "Cast penalty must not transfer");
            close(helper, ManaSoulTransducerLogic.recoveryModifier(0.5, coefficient), 0, "Recovery penalty must not transfer");
            close(helper, ManaSoulTransducerLogic.durationModifier(1, coefficient), 0, "Baseline cast attribute");
            close(helper, ManaSoulTransducerLogic.recoveryModifier(1, coefficient), 0, "Baseline cooldown attribute");
            close(helper, ManaSoulTransducerLogic.durationModifier(1.5, coefficient), -0.5 * coefficient, "Cast transfer");
            close(helper, ManaSoulTransducerLogic.durationModifier(2, coefficient), -0.75 * coefficient, "Soft capped cast transfer");
            close(helper, ManaSoulTransducerLogic.recoveryModifier(1.5, coefficient), 0.5 * coefficient, "Recovery transfer");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void selectionRequiresEquipmentAndOptionalMod(GameTestHelper helper) {
        var player = player(helper, "transducer_selection");
        var spell = SpellRegistry.SOUL_CONVERSION.get();
        var manager = new SpellSelectionManager(player);
        var event = new SpellSelectionManager.SpellSelectionEvent(player, manager);
        ManaSoulTransducerEvents.onSelection(event);
        var selections = manager.getSpellsForSlot(ManaSoulTransducerEvents.SPELL_SELECTION_SLOT);
        helper.assertValueEqual(selections.size(), MalumStaffChargeBridge.isAvailable() ? 1 : 0, "Unique equipment spell selection");
        if (!selections.isEmpty()) helper.assertTrue(selections.getFirst().getCastSource() == CastSource.SPELLBOOK, "Transducer must use spellbook cooldowns");
        if (!MalumStaffChargeBridge.isAvailable()) {
            helper.assertFalse(spell.isEnabled(), "Soul Conversion must be disabled without Malum");
            // 設定の既定値を戻しても、導入条件による強制無効化を突破できない。
            boolean previous = spell.getDefaultConfig().enabled;
            try {
                spell.getDefaultConfig().enabled = true;
                helper.assertFalse(spell.isEnabled(), "Default config override must not enable missing compatibility");
                helper.assertFalse(io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells().contains(spell), "Research list must exclude Soul Conversion");
            } finally {
                spell.getDefaultConfig().enabled = previous;
            }
        }
        unequip(player);
        helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(ManaSoulTransducerEvents.SPELL_SELECTION_SLOT).isEmpty(), "Unequipped spell must disappear");
        helper.assertFalse(spell.checkPreCastConditions(player.level(), 1, player, MagicData.getPlayerMagicData(player)), "Unequipped cast must fail");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void malumAttributesComposeAndCleanUp(GameTestHelper helper) {
        if (MalumStaffChargeBridge.isAvailable()) MalumCases.attributes(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soulConversionRecoversSharedDebtWithStandardCost(GameTestHelper helper) {
        if (MalumStaffChargeBridge.isAvailable()) MalumCases.cast(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void completedRecoveryAndEquipmentLossDoNotChargeMana(GameTestHelper helper) {
        if (MalumStaffChargeBridge.isAvailable()) MalumCases.abort(helper);
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), name);
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(2000);
        ApprenticeCodexGameTestScenarios.equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SOUL_TRANSDUCER.get()));
        return player;
    }

    private static void unequip(ServerPlayer player) {
        // setEquippedCurioは空スタックを装備できないため、解除は実スロットへ直接反映する。
        CuriosApi.getCuriosInventory(player).orElseThrow()
                .getCurios().get(CuriosSlotConstants.CHARM).getStacks().setStackInSlot(0, ItemStack.EMPTY);
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.000001, message + ": expected " + expected + ", got " + actual);
    }

    // optional MOD未導入時に外部クラスを解決しないよう、専用ケースを遅延ロードする。
    private static final class MalumCases {
        static void attributes(GameTestHelper helper) {
            var player = player(helper, "transducer_attributes");
            var duration = player.getAttribute(MalumAttributes.CHARGE_DURATION);
            var recovery = player.getAttribute(MalumAttributes.CHARGE_RECOVERY_RATE);
            duration.setBaseValue(1);
            recovery.setBaseValue(1);
            var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "transducer_test_existing");
            duration.addTransientModifier(new AttributeModifier(id, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            recovery.addTransientModifier(new AttributeModifier(id, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).setBaseValue(1.5);
            player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.5);
            ManaSoulTransducerEvents.updateAttributes(player);
            ManaSoulTransducerEvents.updateAttributes(player);
            close(helper, duration.getValue(), 0.3, "Existing Malum duration must be multiplied once");
            close(helper, recovery.getValue(), 1.9, "Existing Malum recovery must receive additive bonus once");
            player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).setBaseValue(0.5);
            ManaSoulTransducerEvents.updateAttributes(player);
            close(helper, duration.getValue(), 0.5, "Reduced Iron attribute must remove the bonus without penalty");
            unequip(player);
            ManaSoulTransducerEvents.updateAttributes(player);
            close(helper, recovery.getValue(), 1.5, "Unequip must preserve unrelated modifiers");
        }

        static void cast(GameTestHelper helper) {
            var player = player(helper, "transducer_cast");
            var type = MalumAttachmentTypes.STAFF_ABILITIES;
            // 手持ち容量ゼロでも、以前の高容量の杖が残した消費量を全て回復する。
            player.setData(type, new StaffAbilityData(18, 70));
            var magic = MagicData.getPlayerMagicData(player);
            magic.setMana(1000);
            var spell = SpellRegistry.SOUL_CONVERSION.get();
            helper.assertTrue(spell.checkPreCastConditions(player.level(), 1, player, magic), "Empty hands must permit shared charge recovery");
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertValueEqual(player.getData(type).getStaffChargeDebt(), 0, "All shared debt must recover");
            close(helper, magic.getMana(), 500, "Standard mana payment must occur once");
            helper.assertTrue(magic.getPlayerCooldowns().isOnCooldown(spell), "Spellbook cooldown must start");
            helper.assertFalse(spell.checkPreCastConditions(player.level(), 1, player, magic), "Full recovery must reject another cast");
        }

        static void abort(GameTestHelper helper) {
            var player = player(helper, "transducer_abort");
            var type = MalumAttachmentTypes.STAFF_ABILITIES;
            var spell = SpellRegistry.SOUL_CONVERSION.get();
            var magic = MagicData.getPlayerMagicData(player);
            magic.setMana(1000);
            player.setData(type, new StaffAbilityData(1, 0));
            helper.assertTrue(spell.checkPreCastConditions(player.level(), 1, player, magic), "Partial debt should permit casting");
            helper.assertTrue(spell.attemptInitiateCast(ItemStack.EMPTY, 1, player.level(), player, CastSource.SPELLBOOK,
                    true, ManaSoulTransducerEvents.SPELL_SELECTION_SLOT), "Equipped player must be able to initiate LONG casting");
            player.getData(type).tickData(player);
            spell.onServerCastTick(player.level(), 1, player, magic);
            helper.assertFalse(magic.isCasting(), "Natural recovery must cancel active LONG casting");
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            close(helper, magic.getMana(), 1000, "Natural recovery before payment must not consume mana");
            helper.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Cancelled recovery must not start cooldown");
            player.setData(type, new StaffAbilityData(3, 50));
            unequip(player);
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            close(helper, magic.getMana(), 1000, "Equipment loss before payment must not consume mana");
            helper.assertValueEqual(player.getData(type).getStaffChargeDebt(), 3, "Equipment loss must prevent recovery");
        }
    }
}
