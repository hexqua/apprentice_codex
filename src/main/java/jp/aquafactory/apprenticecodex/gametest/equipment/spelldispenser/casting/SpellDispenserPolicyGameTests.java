package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserPolicyCases.denials;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserPolicyGameTests {
    private SpellDispenserPolicyGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_policy_config")
    public static void storedScrollIsRevalidatedBeforeSpendingManaOrStartingCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper);
                 var baseline = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(true, false, List.of(), 1.0D)) {
                var pos = new BlockPos(2, 1, 2);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                var block = scope.block((SpellDispenserBlockEntity) helper.getBlockEntity(pos));
                var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scroll = createSpellScroll(spell);
                scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Policy source scroll"));
                block.getInventory().setStackInSlot(0, scroll.copy());
                block.setCurrentMana(900);
                for (var denial : denials()) {
                    var storedBefore = block.getUpdateTag(helper.getLevel().registryAccess());
                    var countBefore = scope.entities(MagicMissileProjectile.class).size();
                    try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                            denial.enabled(), denial.allowlist(), List.of(), 1.0D)) {
                        var result = scope.capture(block::tryActivate);
                        helper.assertTrue(!result.succeeded() && result.validation().failureReason() == denial.reason(),
                                "Stored scroll should be rejected at activation: " + denial.name());
                        helper.assertTrue(storedBefore.equals(block.getUpdateTag(helper.getLevel().registryAccess()))
                                        && !block.isCoolingDown() && !block.hasActiveContinuousCast(),
                                "Rejected activation should preserve assets, mana and cooldown: " + denial.name());
                        helper.assertTrue(scope.entities(MagicMissileProjectile.class).size() == countBefore,
                                "Rejected activation should not create a projectile: " + denial.name());
                    }
                    var manaBefore = block.getCurrentMana();
                    var allowed = scope.capture(block::tryActivate);
                    helper.assertTrue(allowed.succeeded() && allowed.reachedOnCast()
                                    && scope.entities(MagicMissileProjectile.class).size() == countBefore + 1,
                            "Restoring permission should actually cast the stored spell: " + denial.name());
                    helper.assertTrue(block.getCurrentMana() == manaBefore - spell.getManaCost(1)
                                    && ItemStack.matches(scroll, block.getInventory().getStackInSlot(0)),
                            "Allowed activation should spend mana once and preserve its scroll: " + denial.name());
                    block.clearCooldown();
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_policy_config")
    public static void permissionChangesStopAnAlreadyRunningContinuousCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper);
                 var baseline = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(true, false, List.of(), 1.0D)) {
                var index = 0;
                for (var denial : denials()) {
                    var pos = new BlockPos(1 + 2 * index++, 1, 2);
                    helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                    helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));
                    var block = scope.block((SpellDispenserBlockEntity) helper.getBlockEntity(pos));
                    var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
                    var scroll = createSpellScroll(spell);
                    block.getInventory().setStackInSlot(0, scroll.copy());
                    block.setOwnerProfile(createSpellDispenserOwnerProfile("policy_continuous"));
                    block.setCurrentMana(900);
                    var started = scope.capture(block::tryActivate);
                    helper.assertTrue(started.succeeded() && block.hasActiveContinuousCast(),
                            "Continuous policy fixture should start before permission changes");
                    var manaBefore = block.getCurrentMana();
                    try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                            denial.enabled(), denial.allowlist(), List.of(), 1.0D)) {
                        scope.run(() -> SpellDispenserBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos),
                                helper.getBlockState(pos), block));
                        helper.assertFalse(block.hasActiveContinuousCast(),
                                "Running continuous cast should stop after permission changes: " + denial.name());
                        helper.assertTrue(block.getCurrentMana() == manaBefore
                                        && ItemStack.matches(scroll, block.getInventory().getStackInSlot(0)),
                                "Permission cancellation should not spend additional mana or consume the scroll: " + denial.name());
                        scope.assertReleased(helper, "Permission cancellation left owned anchors: " + denial.name());
                    }
                }
            }
        });
    }
}
