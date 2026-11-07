package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MithrilFreecastStaffCalibrationGameTests {
    private MithrilFreecastStaffCalibrationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Mithril Freecast Staff should reject Arcane Anvil spell imbuing"
            );
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void selectedOffhandAndSpellbookSourcesUseTheirOwnCooldowns(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var magicData = fixture.magicData;
                var staff = fixture.staff;
                var magicMissile = fixture.magicMissile;
                var gauntlet = fixture.gauntlet;
                var normalSwordCooldown = MagicManager.getEffectiveSpellCooldown(magicMissile, player, CastSource.SWORD);
                var selectedSourceCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                helper.assertTrue(selectedSourceCooldown == normalSwordCooldown,
                        "Scrollcaster Gauntlet selected SWORD source should use the normal SWORD cooldown: "
                                + selectedSourceCooldown + " / sword " + normalSwordCooldown);
                var selection = new SpellSelectionManager(player).getSelection();
                helper.assertTrue(selection != null && selection.spellData.getSpell() == magicMissile,
                        "Mithril Freecast Staff cooldown test should resolve the selected offhand spell");
                var selectedStack = SpellSelectionStackResolver.resolveSelectionStack(player, selection.slot);
                helper.assertTrue(ItemStack.isSameItemSameComponents(selectedStack, gauntlet),
                        "Mithril Freecast Staff cooldown test should resolve the selected offhand source stack");
                magicData.setPlayerCastingItem(staff.copy());
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        magicMissile,
                        selection.getCastSource()
                )) {
                    var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                            normalSwordCooldown,
                            magicMissile,
                            player,
                            CastSource.SWORD
                    );
                    MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                    helper.assertTrue(cooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                            "Mithril Freecast Staff should use the selected source cooldown but got "
                                    + cooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
                }

                var heal = SpellRegistry.HEAL_SPELL.get();
                magicData.setPlayerCastingItem(staff.copy());
                var spellbookBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                        heal,
                        player,
                        CastSource.SPELLBOOK
                );
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        heal,
                        CastSource.SPELLBOOK
                )) {
                    var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                            MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                            heal,
                            player,
                            CastSource.SWORD
                    );
                    MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                    var expectedCooldown = spellbookBaseCooldown + heal.getEffectiveCastTime(1, player);
                    helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                            "Mithril Freecast Staff should use selected SPELLBOOK cooldown plus long cast time but got "
                                    + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
                }

            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantCooldownDoesNotLeaveStaleSelectedSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var magicData = fixture.magicData;
                var staff = fixture.staff;
                var magicMissile = fixture.magicMissile;
                var normalSwordCooldown = MagicManager.getEffectiveSpellCooldown(magicMissile, player, CastSource.SWORD);
                var spellbookMagicMissileCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                        magicMissile,
                        player,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(spellbookMagicMissileCooldown != normalSwordCooldown,
                        "Mithril Freecast Staff stale pending test needs SPELLBOOK and SWORD cooldowns to differ");
                try (var ignored = MithrilFreecastStaffCastContext.open(
                        player.getUUID(),
                        staff,
                        magicMissile,
                        CastSource.SPELLBOOK
                )) {
                    var immediateCooldownEvent = new SpellCooldownAddedEvent.Pre(
                            normalSwordCooldown,
                            magicMissile,
                            player,
                            CastSource.SWORD
                    );
                    magicData.setPlayerCastingItem(staff.copy());
                    MithrilFreecastStaffCastEvent.onSpellCooldownAdded(immediateCooldownEvent);
                    helper.assertTrue(immediateCooldownEvent.getEffectiveCooldown() == spellbookMagicMissileCooldown,
                            "Mithril Freecast Staff should apply selected SPELLBOOK cooldown immediately but got "
                                    + immediateCooldownEvent.getEffectiveCooldown() + " / expected " + spellbookMagicMissileCooldown);
                    MithrilFreecastStaffCastContext.retainUntilCooldown(
                            player.getUUID(),
                            staff,
                            magicMissile,
                            CastSource.SPELLBOOK
                    );
                }
                var stalePendingCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                magicData.setPlayerCastingItem(staff.copy());
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(stalePendingCooldownEvent);
                helper.assertTrue(stalePendingCooldownEvent.getEffectiveCooldown() == normalSwordCooldown,
                        "Mithril Freecast Staff should not retain stale selected source after instant cooldown but got "
                                + stalePendingCooldownEvent.getEffectiveCooldown() + " / expected " + normalSwordCooldown);

            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void swingRetainsSelectedSourceUntilDelayedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new MithrilCastFixture(helper)) {
                var player = fixture.player;
                var magicData = fixture.magicData;
                var staff = fixture.staff;
                var staffItem = fixture.staffItem;
                var magicMissile = fixture.magicMissile;
                var normalSwordCooldown = MagicManager.getEffectiveSpellCooldown(magicMissile, player, CastSource.SWORD);
                var selectedSourceCooldown = normalSwordCooldown;
                magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
                helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                        "Mithril Freecast Staff should initiate the selected instant offhand spell");
                helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                                player.getUUID(), staff, magicMissile).isPresent(),
                        "Accepted swing should retain the selected source until its delayed cooldown");
                var delayedCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(delayedCooldownEvent);
                helper.assertTrue(delayedCooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                        "Mithril Freecast Staff should keep the selected source cooldown until delayed cooldown but got "
                                + delayedCooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
            }
        });
    }
}
