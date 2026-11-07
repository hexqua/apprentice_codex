package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.createSpellDispenserMovementHarness;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.invokeCreateGameTestHook;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.skipWhenCreateMissing;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.startCreateSpellDispenserMovement;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.trackHarness;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserPolicyCases.denials;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserPolicyGameTests {
    private CreateDispenserPolicyGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_policy_config")
    public static void mountedStoredScrollIsRevalidatedWithoutSpendingManaOnRejection(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) return;
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper);
                 var baseline = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(true, false, List.of(), 1.0D)) {
                var pos = helper.absolutePos(new BlockPos(2, 1, 2));
                var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
                for (var denial : denials()) {
                    var inventory = new ItemStackHandler(1);
                    var scroll = createSpellScroll(spell);
                    scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Mounted policy source scroll"));
                    inventory.setStackInSlot(0, scroll.copy());
                    var harness = trackHarness(scope, createSpellDispenserMovementHarness(helper.getLevel(), pos,
                            inventory, null, 300, null, false));
                    startCreateSpellDispenserMovement(scope, harness);
                    var before = copyData(harness);
                    var countBefore = scope.entities(MagicMissileProjectile.class).size();
                    try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                            denial.enabled(), denial.allowlist(), List.of(), 1.0D)) {
                        scope.run(() -> invokeCreateGameTestHook("visitNewPosition", new Class<?>[]{Object.class, BlockPos.class}, harness, pos));
                        helper.assertTrue(scope.entities(MagicMissileProjectile.class).size() == countBefore
                                        && before.equals(copyData(harness)) && ItemStack.matches(scroll, inventory.getStackInSlot(0)),
                                "Rejected mounted activation should preserve source and mana without firing: " + denial.name());
                        helper.assertFalse((boolean) invokeCreateGameTestHook("isCoolingDown", new Class<?>[]{Object.class}, harness),
                                "Rejected mounted activation should not start cooldown: " + denial.name());
                    }
                    scope.run(() -> invokeCreateGameTestHook("visitNewPosition", new Class<?>[]{Object.class, BlockPos.class}, harness, pos));
                    helper.assertTrue(scope.entities(MagicMissileProjectile.class).size() == countBefore + 1,
                            "Restoring permission should fire from the mounted entry point: " + denial.name());
                    helper.assertTrue(copyData(harness).getInt("CurrentMana") == 300 - spell.getManaCost(1)
                                    && ItemStack.matches(scroll, inventory.getStackInSlot(0)),
                            "Allowed mounted activation should spend mana once and retain components: " + denial.name());
                }
            }
        });
    }

    private static CompoundTag copyData(Object harness) {
        return (CompoundTag) invokeCreateGameTestHook("copySpellDispenserData", new Class<?>[]{Object.class}, harness);
    }
}
