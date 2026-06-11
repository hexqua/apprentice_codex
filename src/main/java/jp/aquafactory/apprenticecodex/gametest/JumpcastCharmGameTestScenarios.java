package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharm;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharmCastManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.function.Consumer;

final class JumpcastCharmGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final TagKey<Item> CURIOS_FEET = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.FEET)
    );

    private JumpcastCharmGameTestScenarios() {
    }

    static void jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.JUMPCAST_CHARM.get());
            var charm = (JumpcastCharm) stack.getItem();
            charm.initializeSpellContainer(stack);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(stack.is(CURIOS_FEET), "Jumpcast Charm should be tagged for the Curios feet slot");
            helper.assertTrue(stack.getItem() instanceof JumpcastCharm,
                    "Jumpcast Charm should resolve to the dedicated curio item implementation");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Jumpcast Charm should expose exactly one hidden spell slot");
            helper.assertTrue(charm.canImbueSpell(
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                            1
                    ),
                    "Jumpcast Charm should accept INSTANT spells");
            helper.assertTrue(charm.canImbueSpell(
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get(),
                            1
                    ),
                    "Jumpcast Charm should accept LONG spells");
            helper.assertFalse(charm.canImbueSpell(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_CHARGE.get(), 1),
                    "Jumpcast Charm should reject CONTINUOUS spells");
            helper.assertTrue(SpellCalibrationImbueHelper.isSupportedTarget(stack),
                    "Jumpcast Charm should be supported by Spell Calibration Bench operations");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "jumpcast_charm_calibration_test");
            var menu = createSpellCalibrationBenchMenuWithTarget(player, stack);
            menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            assertStackHasSpell(
                    helper,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1,
                    "Calibration-imbued Jumpcast Charm should contain magic_missile"
            );
        });
    }

    static void jumpcastCharmInstantSpellCastsInAirAndConsumesMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
            var player = createJumpcastPlayer(helper, "jumpcast_charm_instant_test", spell, 1);
            var magicData = magicData(helper, player, "instant");
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(200.0F);
            var manaBefore = magicData.getMana();

            helper.assertTrue(JumpcastCharmCastManager.tryCast(player),
                    "Jumpcast Charm should cast an imbued INSTANT spell while airborne");
            helper.assertTrue(magicData.getMana() < manaBefore,
                    "Jumpcast Charm INSTANT cast should consume mana");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Jumpcast Charm INSTANT cast should add spell cooldown");
        });
    }

    static void jumpcastCharmLongSpellCompletesImmediatelyAndExtendsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var player = createJumpcastPlayer(helper, "jumpcast_charm_long_test", spell, 1);
            var magicData = magicData(helper, player, "long");
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(500.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));

            helper.assertTrue(JumpcastCharmCastManager.tryCast(player),
                    "Jumpcast Charm should cast an imbued LONG spell while airborne");
            helper.assertFalse(magicData.isCasting(),
                    "Jumpcast Charm LONG cast should complete immediately");

            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    getEquippedJumpcastCharm(player)
            ) + spell.getEffectiveCastTime(1, player);
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Jumpcast Charm LONG cooldown should add the original cast time but got "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
        });
    }

    static void jumpcastCharmInsufficientManaFailsWithoutSpending(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
            var player = createJumpcastPlayer(helper, "jumpcast_charm_low_mana_test", spell, 1);
            var magicData = magicData(helper, player, "low mana");
            magicData.getSyncedData().learnSpell(spell, false);
            var lowMana = Math.max(0.0F, spell.getManaCost(1) - 1.0F);
            magicData.setMana(lowMana);

            helper.assertFalse(JumpcastCharmCastManager.tryCast(player),
                    "Jumpcast Charm should fail when mana is insufficient");
            helper.assertTrue(Math.abs(magicData.getMana() - lowMana) < 1.0e-4F,
                    "Jumpcast Charm should not spend mana on failed casts: " + magicData.getMana());
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Jumpcast Charm should not add cooldown on failed casts");
        });
    }

    static void jumpcastCharmBlockedMovementContextsDoNotCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_ground_blocked_test", player -> player.setOnGround(true));
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_instabuild_blocked_test",
                    player -> player.getAbilities().instabuild = true);
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_mayfly_blocked_test",
                    player -> player.getAbilities().mayfly = true);
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_flying_blocked_test",
                    player -> player.getAbilities().flying = true);
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_swimming_blocked_test", player -> player.setSwimming(true));
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_ladder_blocked_test", player -> {
                var ladderPos = player.blockPosition();
                helper.getLevel().setBlock(ladderPos.relative(Direction.NORTH), Blocks.STONE.defaultBlockState(), 3);
                helper.getLevel().setBlock(ladderPos,
                        Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), 3);
            });
            assertBlockedContextDoesNotCast(helper, "jumpcast_charm_water_blocked_test", player -> {
                placeAbsoluteFluidTestBasin(helper.getLevel(), player.blockPosition(), Blocks.WATER.defaultBlockState());
                player.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014D);
            });
        });
    }

    private static void assertBlockedContextDoesNotCast(
            GameTestHelper helper,
            String profileName,
            Consumer<FakePlayer> configurePlayer
    ) {
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
        var player = createJumpcastPlayer(helper, profileName, spell, 1);
        var magicData = magicData(helper, player, profileName);
        magicData.getSyncedData().learnSpell(spell, false);
        magicData.setMana(200.0F);
        var manaBefore = magicData.getMana();
        player.setDeltaMovement(Vec3.ZERO);
        configurePlayer.accept(player);

        helper.assertFalse(JumpcastCharmCastManager.tryCast(player),
                "Jumpcast Charm should not cast in blocked context " + profileName);
        helper.assertTrue(Math.abs(magicData.getMana() - manaBefore) < 1.0e-4F,
                "Jumpcast Charm should not spend mana in blocked context " + profileName + ": " + magicData.getMana());
        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Jumpcast Charm should not add cooldown in blocked context " + profileName);
    }

    private static FakePlayer createJumpcastPlayer(
            GameTestHelper helper,
            String profileName,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            int spellLevel
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setOnGround(false);
        var stack = new ItemStack(ItemRegistry.JUMPCAST_CHARM.get());
        var charm = (JumpcastCharm) stack.getItem();
        charm.initializeSpellContainer(stack);
        setSingleUnlockedSpell(helper, stack, spell, spellLevel);
        equipCurio(player, CuriosSlotConstants.FEET, stack);
        return player;
    }

    private static ItemStack getEquippedJumpcastCharm(FakePlayer player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof JumpcastCharm))
                .map(top.theillusivec4.curios.api.SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    private static MagicData magicData(
            GameTestHelper helper,
            FakePlayer player,
            String label
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Jumpcast Charm " + label + " test could not resolve player mana data");
        return magicData;
    }
}
