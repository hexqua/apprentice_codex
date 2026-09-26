package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SpectralWingState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSurfaceScanner;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ShootingStarMantleGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ShootingStarMantleGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void persistentEnergySurvivesReloadAndModeIntervals(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        helper.assertTrue(MantleEnergy.read(stack).energy() == 100 && !stack.isDamageableItem(), "New mantle must have 100 energy and no durability");
        new MantleEnergy(1, false, 36).save(stack);
        var reloaded = ItemStack.parse(helper.getLevel().registryAccess(), stack.save(helper.getLevel().registryAccess())).orElseThrow();
        MantleEnergy.read(reloaded).tickUse().save(reloaded);
        helper.assertTrue(MantleEnergy.read(reloaded).energy() == 1, "Partial flight consumption must survive serialization");
        var exhausted = MantleEnergy.read(reloaded).tickUse();
        exhausted.save(reloaded);
        helper.assertTrue(exhausted.energy() == 0 && exhausted.recovering(), "Completed flight interval must deplete and lock the mantle");
        var restored = ItemStack.parse(helper.getLevel().registryAccess(), reloaded.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(MantleEnergy.read(restored).recovering(), "Recovery lock must survive serialization");
        for (int i = 0; i < 9; i++) { exhausted = exhausted.recharge(); }
        helper.assertTrue(exhausted.energy() == 90 && !exhausted.usable(), "Partial recovery must stay locked");
        helper.assertTrue(exhausted.recharge().usable(), "Only full recovery must unlock flight");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void recoveryChargesFullCostAndHonorsInsufficientMana(GameTestHelper helper) {
        var player = player(helper, "mantle_recovery");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        var magic = MagicData.getPlayerMagicData(player);
        new MantleEnergy(99, false, 7).save(stack);
        magic.setMana(49);
        helper.assertFalse(ShootingStarMantleRuntime.recharge(player, stack, 50), "Insufficient mana must not partially pay");
        helper.assertTrue(MantleEnergy.read(stack).energy() == 99 && magic.getMana() == 49, "Failed recovery must change neither resource");
        magic.setMana(50);
        helper.assertTrue(ShootingStarMantleRuntime.recharge(player, stack, 50), "Full cost must allow a partial final refill");
        helper.assertTrue(magic.getMana() == 0 && MantleEnergy.read(stack).energy() == 100, "Final refill must still cost 50 mana");
        new MantleEnergy(90, true, 7).save(stack);
        magic.setMana(49);
        helper.assertFalse(ShootingStarMantleRuntime.recharge(player, stack, 50), "Recovery mode must require its entire cost");
        magic.setMana(50);
        ShootingStarMantleRuntime.recharge(player, stack, 50);
        helper.assertTrue(MantleEnergy.read(stack).usable() && magic.getMana() == 0, "Full recovery must unlock with full payment");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverConsumesAndEquipmentDataUpdatesDoNotCancelIt(GameTestHelper helper) {
        var player = player(helper, "mantle_consumption");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Equipped mantle must enter hover");
        for (int i = 0; i < 19; i++) {
            ShootingStarMantleRuntime.tick(player);
            ShootingStarMantleRuntime.refreshEquipment(player);
        }
        helper.assertTrue(ShootingStarMantleRuntime.isHovering(player) && MantleEnergy.read(stack).energy() == 100, "Energy data changes must not cancel hover");
        ShootingStarMantleRuntime.toggle(player);
        ShootingStarMantleRuntime.toggle(player);
        for (int i = 0; i < 21; i++) ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 99, "Toggling must not discard partial consumption");
        new MantleEnergy(1, false, 39).save(stack);
        ShootingStarMantleRuntime.tick(player);
        helper.assertFalse(ShootingStarMantleRuntime.isHovering(player), "Depletion must stop hover immediately");
        helper.assertTrue("error_recovery_mode".equals(ShootingStarMantleRuntime.castError(player)), "Depleted mantle must reject recasting");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void idleRecoveryUsesTenTicksAndStopsWhileHovering(GameTestHelper helper) {
        var player = player(helper, "mantle_recovery_timer");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        new MantleEnergy(50, false, 0).save(stack);
        MagicData.getPlayerMagicData(player).setMana(100);
        for (int i = 0; i < 9; i++) ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 50, "Idle recovery must wait ten ticks");
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 60, "Tenth tick must restore exactly ten energy");
        ShootingStarMantleRuntime.toggle(player);
        for (int i = 0; i < 10; i++) ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 60, "Hover must not recharge");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void impulseEnforcesEnergySequenceAndActiveWindow(GameTestHelper helper) {
        var player = player(helper, "mantle_impulse");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        ShootingStarMantleRuntime.toggle(player);
        new MantleEnergy(11, false, 0).save(stack);
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 1, Float.NaN, 0), "Non-finite direction must be rejected");
        helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 2, 100, 100), "Finite direction must be bounded and accepted");
        helper.assertTrue(MantleEnergy.read(stack).energy() == 1, "Impulse must charge exactly ten");
        new MantleEnergy(100, false, 0).save(stack);
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 2, 1, 0), "Duplicate request must be rejected");
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 3, 1, 0), "Impulse must not restart during its five ticks");
        for (int i = 0; i < 5; i++) ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 4, 0, 0), "Impulse may start again after its window");
        helper.assertTrue(ShootingStarMantleRuntime.state(player).dashDirection.dot(MantleMovement.direction(0, 0, player.getYRot())) > 0.999, "Missing input must use backward direction");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void depletedImpulseFinishesBeforeRecovery(GameTestHelper helper) {
        var player = player(helper, "mantle_last_impulse");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        // 発動時枯渇と、通常消費が推進中に追いつく場合を同じ終了規則で検証する。
        for (int initial : new int[]{1, 9, 10, 11}) {
            new MantleEnergy(initial, false, 39).save(stack);
            MagicData.getPlayerMagicData(player).setMana(100);
            helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Usable mantle must enter hover");
            helper.assertTrue(ShootingStarMantleRuntime.impulse(player, initial * 10L, 1, 0), "Any positive usable energy must allow impulse");
            for (int i = 0; i < 4; i++) {
                ShootingStarMantleRuntime.tick(player);
                ShootingStarMantleRuntime.refreshEquipment(player);
                helper.assertTrue(ShootingStarMantleRuntime.isHovering(player), "Depletion must not interrupt the accepted impulse");
            }
            helper.assertTrue(MantleEnergy.read(stack).energy() == 0 && MantleEnergy.read(stack).recovering(), "Normal consumption must continue during impulse");
            helper.assertTrue(MagicData.getPlayerMagicData(player).getMana() == 100, "Recovery must not run during impulse");
            helper.assertFalse(ShootingStarMantleRuntime.impulse(player, initial * 10L + 1, 1, 0), "Depleted impulse must not restart");
            ShootingStarMantleRuntime.tick(player);
            helper.assertFalse(ShootingStarMantleRuntime.isHovering(player), "Depleted hover must end after five ticks");
            helper.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() == 0, "Depleted impulse must stop horizontal momentum");
            for (int i = 0; i < 9; i++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 0, "Recovery timer must start after impulse ends");
            ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 10 && MantleEnergy.read(stack).recovering(), "Recovery must begin on the tenth idle tick and remain locked");
            helper.assertFalse(ShootingStarMantleRuntime.impulse(player, initial * 10L + 2, 1, 0), "Partial recovery must not allow another impulse");
        }
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void depletedImpulseStillHonorsEquipmentAndConflict(GameTestHelper helper) {
        var player = player(helper, "mantle_impulse_cancel");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        new MantleEnergy(1, false, 0).save(stack);
        ShootingStarMantleRuntime.toggle(player);
        ShootingStarMantleRuntime.impulse(player, 0, 1, 0);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(!ShootingStarMantleRuntime.isHovering(player) && ShootingStarMantleRuntime.state(player).dashTicks == 0, "Competing elytra must cancel even a depleted impulse");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        new MantleEnergy(1, false, 0).save(stack);
        ShootingStarMantleRuntime.toggle(player);
        ShootingStarMantleRuntime.impulse(player, 1, 1, 0);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", ItemStack.EMPTY);
        ShootingStarMantleRuntime.refreshEquipment(player);
        helper.assertTrue(!ShootingStarMantleRuntime.isHovering(player) && ShootingStarMantleRuntime.state(player).dashTicks == 0, "Unequip must cancel even a depleted impulse");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void competingWingsAndChestElytraTakePriority(GameTestHelper helper) {
        var player = player(helper, "mantle_conflict");
        ShootingStarMantleRuntime.toggle(player);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertFalse(ShootingStarMantleRuntime.state(player).hovering, "Chest elytra must stop hover");
        helper.assertTrue(player.isFallFlying(), "Mantle must not stop the priority elytra flight");
        helper.assertTrue("conflict".equals(ShootingStarMantleRuntime.castError(player)), "Chest elytra must reject hover cast");
        player.stopFallFlying();
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffectRegistry.ANGEL_WINGS.get()), 100));
        helper.assertTrue(ShootingStarMantleRuntime.conflict(player), "Angel Wings must take priority");
        player.removeAllEffects();
        var spellData = Objects.requireNonNull(Capabilities.getSpellDataOrNull(player), "Missing spell data for mantle conflict test");
        spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, state -> { state.active = true; state.startedBySpell = true; });
        helper.assertTrue(ShootingStarMantleRuntime.conflict(player), "Spectral Wing must take priority");
        spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, SpectralWingState::reset);
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unequipAndReplacementCancelHover(GameTestHelper helper) {
        var player = player(helper, "mantle_equipment");
        ShootingStarMantleRuntime.toggle(player);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        ShootingStarMantleRuntime.refreshEquipment(player);
        helper.assertFalse(ShootingStarMantleRuntime.isHovering(player), "Replacing the physical mantle must stop hover");
        ShootingStarMantleRuntime.toggle(player);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", ItemStack.EMPTY);
        ShootingStarMantleRuntime.refreshEquipment(player);
        helper.assertTrue("not_found_mantle".equals(ShootingStarMantleRuntime.castError(player)), "Unequipped casts must be rejected regardless of source");
        helper.assertFalse(ShootingStarMantleRuntime.isHovering(player), "Unequip must stop hover");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wheelAndCosmeticsRequireFunctionalMantle(GameTestHelper helper) {
        var player = player(helper, "mantle_wheel");
        helper.assertTrue(new SpellSelectionManager(player).getAllSpells().stream().anyMatch(option -> option.spellData.getSpell() == SpellRegistry.WAVERING_STAR.get()), "Equipped mantle must add its spell to the wheel");
        var back = CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("back");
        back.getCosmeticStacks().setStackInSlot(0, ShootingStarMantleRuntime.findEquipped(player).copy());
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", ItemStack.EMPTY);
        helper.assertTrue(ShootingStarMantleRuntime.findEquipped(player).isEmpty(), "Cosmetic mantle must not supply flight");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void heightTracksExternalMotionAndIgnoresJitter(GameTestHelper helper) {
        helper.assertTrue(MantleMovement.movingTicks(Vec3.ZERO, new Vec3(0.001, 0, 0), 0) == 0, "Subthreshold jitter must not lower height");
        int remaining = MantleMovement.movingTicks(Vec3.ZERO, new Vec3(0.1, 0, 0), 0);
        helper.assertTrue(remaining == 10, "External displacement must lower height for ten ticks");
        helper.assertTrue(MantleMovement.vertical(0, 3, 0, 0) > 0 && MantleMovement.vertical(0, 3, 0, remaining) < 0, "Movement must switch target from five to two blocks");
        for (int i = 0; i < 10; i++) remaining = MantleMovement.movingTicks(Vec3.ZERO, Vec3.ZERO, remaining);
        helper.assertTrue(remaining == 0, "Height must return after ten still ticks");
        helper.assertTrue(MantleMovement.vertical(-2, 100, Double.NaN, 0) >= -0.49, "Missing terrain must use slow falling");
        helper.assertTrue(MantleMovement.vertical(0.4, 8, 0, remaining) == 0.4,
                "External upward velocity must not be capped by the moving hover target");
        var pos = new BlockPos(1, 1, 1);
        // basic_floorの検査領域上端より高く探索するため、探索列の空気を明示する。
        for (int i = 1; i <= 7; i++) helper.setBlock(pos.above(i), Blocks.AIR);
        for (var block : new Block[]{Blocks.STONE, Blocks.WATER, Blocks.LAVA}) {
            helper.setBlock(pos, block);
            var absolute = helper.absolutePos(pos);
            var surface = BroomSurfaceScanner.findSurfaceBelow(helper.getLevel(), absolute.getX() + 0.5, absolute.getY() + 6, absolute.getZ() + 0.5, 16, true);
            helper.assertTrue(surface.isPresent() && surface.get().y() == absolute.getY() + 1,
                    "Ground and both fluids must be found from five blocks above their surface: block=" + block + ", surface=" + surface + ", expected=" + (absolute.getY() + 1));
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalUpwardMotionDecaysAndReturnsToHoverTarget(GameTestHelper helper) {
        var player = player(helper, "mantle_external_ascent");
        clearAscentColumn(helper);
        var surface = helper.absolutePos(new BlockPos(1, 0, 1)).getY() + 1;
        player.setPos(player.getX(), surface + 6, player.getZ());
        ShootingStarMantleRuntime.toggle(player);
        player.setDeltaMovement(0, 0.4, 0);
        double startY = player.getY();
        MantleMovement.travel(player, Vec3.ZERO, ShootingStarMantleRuntime.state(player));
        helper.assertTrue(player.getY() > startY + 0.39 && player.getDeltaMovement().y < 0.4,
                "External ascent must move above the hover target and retain decaying momentum");
        for (int tick = 0; tick < 12; tick++) {
            MantleMovement.travel(player, Vec3.ZERO, ShootingStarMantleRuntime.state(player));
        }
        helper.assertTrue(player.getDeltaMovement().y < 0,
                "Ascent must stop and return toward the hover target after external thrust ends");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void manaThrusterClimbsDuringHoverAndCoastsAfterRelease(GameTestHelper helper) {
        var player = player(helper, "mantle_thruster_ascent");
        clearAscentColumn(helper);
        var surface = helper.absolutePos(new BlockPos(1, 0, 1)).getY() + 1;
        player.setPos(player.getX(), surface + 6, player.getZ());
        ShootingStarMantleRuntime.toggle(player);
        ApprenticeCodexGameTestScenarios.equipCurio(player, CuriosSlotConstants.FEET,
                new ItemStack(ItemRegistry.MANA_THRUSTER.get()));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mantle and Mana Thruster test needs player mana data");
        magicData.setMana(100);
        player.setOnGround(false);
        double startY = player.getY();
        try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
            ManaThrusterFlightManager.setJumpInput(player, true);
            for (int tick = 0; tick < 6; tick++) {
                MantleMovement.travel(player, Vec3.ZERO, ShootingStarMantleRuntime.state(player));
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }
            helper.assertTrue(player.getY() > startY + 0.5,
                    "Mana Thruster must climb even above the mantle's stationary hover target");
            helper.assertTrue(Math.abs(magicData.getMana() - 70) < 1.0e-4,
                    "Mana Thruster must pay mana for every successful ascent tick");
            ManaThrusterFlightManager.setJumpInput(player, false);
            double releaseY = player.getY();
            for (int tick = 0; tick < 12; tick++) {
                MantleMovement.travel(player, Vec3.ZERO, ShootingStarMantleRuntime.state(player));
            }
            helper.assertTrue(player.getDeltaMovement().y < 0 && player.getY() > releaseY,
                    "Released thrust must coast briefly, then return to slow descent");
            helper.assertTrue(Math.abs(magicData.getMana() - 70) < 1.0e-4,
                    "Released Mana Thruster must stop charging mana");
        } finally {
            ManaThrusterFlightManager.clear(player);
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serverAcceptsExternalAscentWhileHoverIsPaid(GameTestHelper helper) {
        var player = serverPlayer(helper);
        clearAscentColumn(helper);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("back", 0,
                new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        ShootingStarMantleRuntime.refreshEquipment(player);
        var surface = helper.absolutePos(new BlockPos(1, 0, 1)).getY() + 1;
        player.setPos(player.getX(), surface + 6, player.getZ());
        helper.getLevel().addNewPlayer(player);
        try {
            helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Mantle hover must activate before external ascent");
            var startY = player.getY();
            player.connection.resetPosition();
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(player.getX(), startY + 0.1, player.getZ(), false));
            helper.assertTrue(player.getY() > startY + 0.09,
                    "A small external upward movement must not be clamped to the hover target");
            new MantleEnergy(1, false, 39).save(ShootingStarMantleRuntime.findEquipped(player));
            ShootingStarMantleRuntime.tick(player);
            helper.assertFalse(ShootingStarMantleRuntime.isHovering(player),
                    "Server energy depletion must revoke the hover flight allowance");
        } finally {
            ShootingStarMantleRuntime.clear(player);
            player.discard();
        }
        helper.succeed();
    }

    private static void clearAscentColumn(GameTestHelper helper) {
        // basic_floorの天井barrierが上昇テストの経路に入らないようにする。
        for (int y = 1; y <= 18; y++) {
            helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(1, y, 1)), Blocks.AIR.defaultBlockState());
        }
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), name);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        ShootingStarMantleRuntime.refreshEquipment(player);
        player.setOnGround(false);
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void mantleStartsAndMaintainsFlightWithoutChestArmor(GameTestHelper helper) {
        var player = player(helper, "mantle_elytra");
        helper.assertTrue(player.tryToStartFallFlying(), "Mantle must start elytra flight without chest armor");
        player.aiStep();
        helper.assertTrue(player.isFallFlying(), "LivingEntity must maintain mantle flight without chest armor");
        ShootingStarMantleRuntime.toggle(player);
        helper.assertFalse(player.isFallFlying(), "Hover activation must end elytra flight");
        helper.assertFalse(player.tryToStartFallFlying(), "Hover must block elytra restart");
        ShootingStarMantleRuntime.toggle(player);
        helper.assertFalse(player.isFallFlying(), "Ending hover must not automatically restart elytra flight");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void impulseTravelsFiveBlocksAndRespectsWalls(GameTestHelper helper) {
        var player = player(helper, "mantle_collision");
        var start = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        // 移動列はこのtestの領域内で明示し、構造物の天井等に依存しない。
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, 0), Blocks.AIR);
        }
        player.setPos(start);
        player.setDeltaMovement(Vec3.ZERO);
        var state = ShootingStarMantleRuntime.state(player);
        state.dashDirection = new Vec3(1, 0, 0);
        state.dashTicks = 5;
        for (int i = 0; i < 5; i++) MantleMovement.travel(player, Vec3.ZERO, state);
        helper.assertTrue(Math.abs(player.getX() - start.x - 5) < 0.001, "Unobstructed impulse must travel five blocks");
        helper.assertTrue(state.dashTicks == 0 && player.getDeltaMovement().horizontalDistanceSqr() == 0, "Impulse must stop after five ticks");
        player.setPos(start);
        player.setDeltaMovement(Vec3.ZERO);
        state.lastPosition = null;
        state.dashTicks = 5;
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(2, y, 0), Blocks.STONE);
        for (int i = 0; i < 5; i++) MantleMovement.travel(player, Vec3.ZERO, state);
        helper.assertTrue(player.getX() < start.x + 2, "Impulse must not move through a solid wall");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void alternateMovementContextsCancelHover(GameTestHelper helper) {
        var player = player(helper, "mantle_context");
        ShootingStarMantleRuntime.toggle(player);
        player.getAbilities().flying = true;
        ShootingStarMantleRuntime.tick(player);
        helper.assertFalse(ShootingStarMantleRuntime.state(player).hovering, "Creative flight must cancel hover");
        player.getAbilities().flying = false;
        ShootingStarMantleRuntime.clear(player);
        var rider = serverPlayer(helper);
        var inventory = CuriosApi.getCuriosInventory(rider).orElseThrow();
        inventory.setEquippedCurio("back", 0, new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        helper.assertTrue(ShootingStarMantleRuntime.toggle(rider), "Rider must first enter hover");
        var pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(rider.startRiding(pig, true), "Test player must actually mount the vehicle");
        ShootingStarMantleRuntime.tick(rider);
        helper.assertFalse(ShootingStarMantleRuntime.state(rider).hovering, "Riding must cancel hover");
        rider.stopRiding();
        pig.discard();
        ShootingStarMantleRuntime.clear(rider);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void multipleBackSlotsUseOnlyFirstMantle(GameTestHelper helper) {
        var player = player(helper, "mantle_multiple");
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.addTransientSlotModifier("back", ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/mantle_back"), 1, AttributeModifier.Operation.ADD_VALUE);
        var first = ShootingStarMantleRuntime.findEquipped(player);
        var second = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        inventory.setEquippedCurio("back", 1, second);
        ShootingStarMantleRuntime.toggle(player);
        for (int i = 0; i < 20; i++) ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(MantleEnergy.read(first).energy() == 100 && MantleEnergy.read(first).spentTicks() == 20
                && MantleEnergy.read(second).energy() == 100, "Only the first mantle must be active");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        // FakePlayerはstartRidingを常に拒否するため、乗車検証には接続付きServerPlayerが必要。
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "mantle_rider");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 3, 1.5)));
        return player;
    }
}
