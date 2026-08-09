package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomSurfaceScanner;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.network.packet.SyncFloatmountBroomConfigPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FloatmountBroomGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private FloatmountBroomGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registrationsResolveDedicatedTypes(GameTestHelper helper) {
        helper.assertTrue(ItemRegistry.FLOATMOUNT_BROOM.get() instanceof FloatmountBroomItem,
                "Floatmount Broom item should use its dedicated implementation");
        var broom = EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel());
        helper.assertTrue(broom instanceof FloatmountBroomEntity,
                "Floatmount Broom entity type should create its dedicated entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultItemPlacementDoesNotCreateAVisibleCustomName(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_default_placement");
        var broom = placeBroomFromItem(helper, player, new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        helper.assertFalse(broom.hasCustomName(), "Default item name must not become a custom name");
        helper.assertFalse(broom.isCustomNameVisible(), "Default broom must not show a nameplate");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void namedItemPlacementCopiesVisibleNameAndEntitySaveRetainsIt(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_named_placement");
        var expectedName = Component.literal("Zephyr").withStyle(ChatFormatting.AQUA);
        var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        stack.set(DataComponents.CUSTOM_NAME, expectedName);
        var broom = placeBroomFromItem(helper, player, stack);
        helper.assertTrue(expectedName.equals(broom.getCustomName()), "Placed broom should copy the item name");
        helper.assertTrue(broom.isCustomNameVisible(), "Named broom should show its nameplate");

        var saved = new CompoundTag();
        broom.saveWithoutId(saved);
        var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(expectedName.equals(loaded.getCustomName()), "Saved broom should retain its name");
        helper.assertTrue(loaded.isCustomNameVisible(), "Saved broom should retain nameplate visibility");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyOnePlayerMayRideAndOccupiedBroomCannotBeRecovered(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var first = player(helper, "floatmount_broom_first_rider");
        var second = player(helper, "floatmount_broom_second_rider");

        helper.assertTrue(first.startRiding(broom, true), "First player should be able to ride the broom");
        broom.positionRider(first);
        var riderAttachmentY = first.getY() + first.getVehicleAttachmentPoint(broom).y;
        helper.assertTrue(Math.abs(riderAttachmentY
                        - (broom.getY() + FloatmountBroomEntity.RIDER_ATTACHMENT_Y)) < 1.0e-6D,
                "Rider vehicle attachment should match the configured broom model height");
        helper.assertTrue(broom.getControllingPassenger() == first,
                "The sole player passenger should control the broom");
        helper.assertFalse(second.startRiding(broom), "Second player should not be able to ride the occupied broom");

        first.setShiftKeyDown(true);
        broom.interact(first, InteractionHand.MAIN_HAND);
        helper.assertFalse(broom.isRemoved(), "Occupied broom must not be recovered");
        helper.assertFalse(first.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Occupied recovery must not grant an item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingRecoveryReturnsFreshItemEvenInCreative(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_recovery");
        player.getAbilities().instabuild = true;
        player.setShiftKeyDown(true);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(broom.isRemoved(), "Recovered broom should be removed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Creative recovery should grant a fresh broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingRecoveryKeepsOnlyNameAndRedeploysWithResetState(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var expectedName = Component.literal("Restored Broom").withStyle(ChatFormatting.GOLD);
            var broom = spawnBroom(helper, 1.5D);
            broom.setCustomName(expectedName);
            broom.setCustomNameVisible(true);
            broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
            var damagedState = new CompoundTag();
            broom.saveWithoutId(damagedState);
            damagedState.putBoolean("EmergencyLanding", true);
            broom.load(damagedState);
            helper.assertTrue(broom.isDamaged(), "Recovery setup should use a damaged broom");
            helper.assertTrue(broom.isManaEmergencyLanding(), "Recovery setup should use an emergency broom");

            var player = player(helper, "floatmount_broom_named_recovery");
            player.setShiftKeyDown(true);
            broom.interact(player, InteractionHand.MAIN_HAND);
            var recovered = findBroomInInventory(helper, player);
            helper.assertTrue(expectedName.equals(recovered.get(DataComponents.CUSTOM_NAME)),
                    "Sneaking recovery should copy the entity custom name");

            var redeployStack = recovered.copy();
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            var redeployed = placeBroomFromItem(helper, player, redeployStack);
            helper.assertTrue(expectedName.equals(redeployed.getCustomName()), "Redeployed broom should retain its name");
            helper.assertTrue(redeployed.getDamage() == 0, "Redeployed broom should reset damage");
            helper.assertFalse(redeployed.isDamaged(), "Redeployed broom should reset damaged state");
            helper.assertFalse(redeployed.isManaEmergencyLanding(), "Redeployed broom should reset emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nameTagDoesNotRenameBroom(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_name_tag");
        player.getAbilities().instabuild = true;
        var nameTag = new ItemStack(Items.NAME_TAG);
        nameTag.set(DataComponents.CUSTOM_NAME, Component.literal("Rejected Name"));
        player.setItemInHand(InteractionHand.MAIN_HAND, nameTag);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertFalse(broom.hasCustomName(), "Name tags must not rename Floatmount Broom entities");
        helper.assertTrue(nameTag.getCount() == 1, "Unsupported name tag use must not consume the item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageScalesAndRecoversAtTenTickIntervals(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_damage_recovery");
            var source = helper.getLevel().damageSources().playerAttack(player);

            broom.hurt(source, 1.0F);
            helper.assertTrue(broom.getDamage() == 50,
                    "One point of incoming damage should add fifty broom damage");
            // Entity.tickの手動呼び出しではLevel側のtickCount更新が入らないため、境界値を明示する。
            broom.tickCount = 9;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 50,
                    "Damage must not recover before the ten tick interval");
            broom.tickCount = 10;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 0,
                    "The configured amount should recover at the ten tick interval");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tooltipUsesSyncedManaThresholdAndExpectedControls(GameTestHelper helper) {
        FloatmountBroomConfigState.setNormalFlightManaThreshold(321);
        try {
            var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
            var lines = new ArrayList<Component>();
            stack.getItem().appendHoverText(
                    stack, Item.TooltipContext.of(helper.getLevel()), lines, TooltipFlag.Default.NORMAL
            );

            helper.assertTrue(lines.size() == 4, "Floatmount Broom should have four tooltip lines");
            assertTooltipLine(helper, lines, 0, "item.apprenticecodex.floatmount_broom.desc_1", 1);
            assertTooltipLine(helper, lines, 1, "item.apprenticecodex.floatmount_broom.desc_2", 2);
            assertTooltipLine(helper, lines, 2, "item.apprenticecodex.floatmount_broom.desc_3", 1);
            assertTooltipLine(helper, lines, 3, "item.apprenticecodex.floatmount_broom.desc_4", 0);

            var firstArgs = ((TranslatableContents) lines.get(0).getContents()).getArgs();
            var secondArgs = ((TranslatableContents) lines.get(1).getContents()).getArgs();
            assertComponentKey(helper, firstArgs[0], "key.use", "Placement control should use the use key");
            assertComponentKey(helper, secondArgs[0], "key.sneak", "Retrieval control should start with sneak");
            assertComponentKey(helper, secondArgs[1], "key.use", "Retrieval control should end with use");

            var manaArg = ((TranslatableContents) lines.get(2).getContents()).getArgs()[0];
            helper.assertTrue(manaArg instanceof Component, "Mana threshold should be supplied as a styled component");
            if (manaArg instanceof Component manaComponent) {
                helper.assertTrue("321".equals(manaComponent.getString()),
                        "Tooltip should use the synchronized mana threshold");
                helper.assertTrue(manaComponent.getStyle().getColor() != null
                                && manaComponent.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
                        "Mana threshold should be aqua");
            }
            helper.succeed();
        } finally {
            FloatmountBroomConfigState.reset();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void configSyncPacketPreservesManaThreshold(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncFloatmountBroomConfigPacket.encode(new SyncFloatmountBroomConfigPacket(4321), buffer);
        var decoded = SyncFloatmountBroomConfigPacket.decode(buffer);
        helper.assertTrue(decoded.normalFlightManaThreshold() == 4321,
                "Floatmount Broom config sync should preserve the mana threshold");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damageIFrameIncludesAcceptedTickAndReopensAtTickTen(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var source = helper.getLevel().damageSources().generic();

        helper.assertTrue(broom.hurt(source, 1.0F), "The first damage hit should be accepted");
        helper.assertFalse(broom.hurt(source, 1.0F),
                "The accepted tick itself should be inside the broom damage i-frame");

        helper.runAfterDelay(9, () -> helper.assertFalse(broom.hurt(source, 1.0F),
                "A normal hit at T+9 should still be rejected"));
        helper.runAfterDelay(10, () -> {
            var damageBeforeHit = broom.getDamage();
            helper.assertTrue(broom.hurt(source, 1.0F), "A normal hit at T+10 should be accepted");
            helper.assertTrue(broom.getDamage() == damageBeforeHit + 50,
                    "The T+10 hit should apply the configured damage conversion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameZeroSettingDisablesRejection(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 0, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var source = helper.getLevel().damageSources().generic();

            helper.assertTrue(broom.hurt(source, 1.0F), "The first hit should be accepted");
            helper.assertTrue(broom.hurt(source, 1.0F), "A zero-tick i-frame should accept a same-tick hit");
            helper.assertTrue(broom.getDamage() == 100,
                    "Disabling the i-frame should apply both same-tick hits");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ignoredAndZeroDamageDoNotInteractWithDamageIFrame(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(
                1000, 0, 10, Set.of(DamageTypes.GENERIC.location()), 100, 50,
                1.0D, 1.0D, 1.5D
        );
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_iframe_owner");
            var ignoredSource = helper.getLevel().damageSources().generic();
            var normalSource = helper.getLevel().damageSources().playerAttack(player);

            helper.assertTrue(broom.hurt(normalSource, 0.01F),
                    "A hit converted to zero broom damage should retain the existing hurt result");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "A configured ignored damage type should pass without starting an i-frame");
            helper.assertTrue(broom.hurt(normalSource, 1.0F),
                    "Normal damage should still be accepted after zero and ignored damage");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "Ignored damage should pass through an active i-frame");
            helper.assertFalse(broom.hurt(normalSource, 1.0F),
                    "Ignored damage must not clear the active normal-damage i-frame");
            helper.assertTrue(broom.getDamage() == 150,
                    "Only the three positive accepted hits should change broom damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameIsNotPersisted(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 10, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var source = helper.getLevel().damageSources().generic();
            var broom = spawnBroom(helper, 1.5D);
            broom.hurt(source, 1.0F);
            var saved = new CompoundTag();
            broom.saveWithoutId(saved);

            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.hurt(source, 1.0F),
                    "Loading should not restore the volatile damage i-frame");
            helper.assertTrue(loaded.getDamage() == 100,
                    "The loaded broom should retain damage while accepting a new hit immediately");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lavaDamagesBroomWithoutPersistentFire(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnBroom(helper, 0.2D);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(broom.getDamage() > 0, "Lava contact should still damage the broom");
            helper.assertFalse(broom.isOnFire(), "The broom should clear persistent fire after contact damage");
            helper.assertTrue(broom.getRemainingFireTicks() <= 0,
                    "Persistent fire ticks should not remain on the broom");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void combatToolsTargetsOnlyPvpHarmableEnemyPlayerBrooms(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        helper.assertTrue(server != null, "Floatmount Broom PvP test requires a server");
        var attacker = new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), "broom_pvp_attacker"), ClientInformation.createDefault());
        // 降車時のteleport処理も通るため、騎乗者には接続を備えたGameTestのmock playerを使う。
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        var broom = spawnBroom(helper, 1.5D);
        var scoreboard = level.getScoreboard();
        var team = scoreboard.addPlayerTeam("broom_pvp_policy");
        var previousPvp = server.isPvpAllowed();

        try {
            server.setPvpAllowed(true);
            helper.assertTrue(rider.startRiding(broom, true), "PvP target rider should mount the broom");
            helper.assertTrue(CombatTools.isValidCombatTarget(broom, attacker),
                    "An enemy player broom should be a combat target while PvP is enabled");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, rider),
                    "A rider must not target their own root vehicle");

            scoreboard.addPlayerToTeam(attacker.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(rider.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Friendly-fire protection should also protect the ridden broom");
            team.setAllowFriendlyFire(true);
            helper.assertTrue(CombatTools.isValidCombatTarget(broom, attacker),
                    "Friendly-fire-enabled teammates should expose the ridden broom");

            scoreboard.removePlayerFromTeam(attacker.getScoreboardName(), team);
            scoreboard.removePlayerFromTeam(rider.getScoreboardName(), team);
            server.setPvpAllowed(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Server PvP disablement should protect the ridden broom");

            server.setPvpAllowed(true);
            rider.stopRiding();
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "An unoccupied broom should not become a general combat target");
            helper.assertTrue(rider.startRiding(broom, true), "Rider should remount for owner type validation");
            var zombie = EntityType.ZOMBIE.create(level);
            helper.assertTrue(zombie != null, "PvP owner type test should create a zombie");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, zombie),
                    "A non-player combat owner should not special-target the broom");
        } finally {
            rider.stopRiding();
            server.setPvpAllowed(previousPvp);
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void maximumDamageIsPersistentAndPreventsMounting(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_damaged");
            helper.assertTrue(player.startRiding(broom, true), "Damage test player should mount directly");
            broom.hurt(helper.getLevel().damageSources().playerAttack(player), 20.0F);

            helper.assertTrue(broom.isDamaged(), "Maximum damage should enter the damaged state");
            helper.assertFalse(broom.isRemoved(), "Normal damage must not itemize the broom");
            helper.assertTrue(player.getVehicle() == broom, "Damage must not eject the current rider immediately");
            helper.assertTrue(broom.isForcedLanding(), "Damaged broom must enter forced landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Damaged broom must always require dismount confirmation");
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged broom should remain at maximum damage");
            for (var tick = 0; tick < 20; tick++) {
                broom.tick();
            }
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged state must disable natural recovery");
            player.stopRiding();
            helper.assertFalse(player.startRiding(broom), "Damaged broom must reject riders");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isDamaged(), "Damaged state must persist in entity NBT");
            helper.assertTrue(loaded.getDamage() == loaded.getMaxDamage(),
                    "Loaded damaged broom should retain maximum damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damagedBroomItemizesOnlyBelowWorldBottomRegardlessOfDamageSource(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var expectedName = Component.literal("Void Survivor").withStyle(ChatFormatting.LIGHT_PURPLE);
            var broom = spawnBroom(helper, 1.5D);
            broom.setCustomName(expectedName);
            broom.setCustomNameVisible(true);
            broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
            helper.assertTrue(broom.isDamaged(), "Void damage may damage the broom like any other source");
            helper.assertFalse(broom.isRemoved(),
                    "Void damage source alone must never be used as the itemization condition");

            broom.setPos(broom.getX(), helper.getLevel().getMinBuildHeight() - 1.0D, broom.getZ());
            var dropPos = broom.position();
            broom.tick();
            helper.assertTrue(broom.isRemoved(), "Damaged broom below world bottom should itemize");
            helper.assertTrue(countDroppedBrooms(helper, dropPos) == 1,
                    "World-bottom itemization should drop exactly one fresh broom");
            var dropped = findDroppedBroom(helper, dropPos).getItem();
            helper.assertTrue(expectedName.equals(dropped.get(DataComponents.CUSTOM_NAME)),
                    "World-bottom itemization should preserve the broom custom name");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mountingRequiresConfiguredManaButCreativeIsExempt(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_mount_mana");
            var magicData = magicData(helper, player);
            magicData.setMana(99.0F);

            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(player.isPassenger(), "Player below the normal flight threshold must not mount");

            magicData.setMana(100.0F);
            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == broom, "Player at the normal flight threshold should mount");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Mounting must not consume mana");

            player.stopRiding();
            var creativeBroom = spawnBroom(helper, 1.5D);
            player.getAbilities().instabuild = true;
            magicData.setMana(0.0F);
            creativeBroom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == creativeBroom, "Creative players must mount with zero mana");
            creativeBroom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            creativeBroom.tick();
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Creative broom movement must not consume mana");
            helper.assertFalse(creativeBroom.isManaEmergencyLanding(),
                    "Creative broom movement must not enter emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void movementInputsUseConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            assertMovementManaCost(helper, "floatmount_broom_horizontal_cost", 1.0F,
                    0.0F, 1.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_ascending_cost", 2.0F,
                    0.0F, 0.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_combined_cost", 3.5F,
                    0.0F, -1.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_turning_free", 0.0F,
                    1.0F, 0.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_descending_free", 0.0F,
                    0.0F, 0.0F, false, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serverTeleportWithoutReportedPoweredInputRemainsFree(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_server_teleport");
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Server teleport test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false);

            // client権威のvehicle位置差だけでは、惰性・server再配置・他MODの外力と偽装移動を区別できない。
            // 正規playerへの誤課金を避けるため、申告された動力入力がない再配置は課金対象にしない。
            // その結果、改造clientによる入力の過少申告も防げないことを設計上受容する。
            var beforeTeleport = broom.position();
            broom.teleportRelative(1.0D, 1.0D, 0.0D);
            helper.assertTrue(broom.position().distanceToSqr(beforeTeleport) > 1.0D,
                    "Server teleport should move the mounted broom horizontally and upward");

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana() - 0.5F) < 1.0e-4F,
                    "Server teleport without reported powered input must not consume mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Server teleport without reported powered input must not trigger emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nonControllingPlayerCannotSubmitPaidMovementInput(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var rider = player(helper, "floatmount_broom_controller");
            var nonController = player(helper, "floatmount_broom_non_controller");
            var riderMagicData = magicData(helper, rider);
            riderMagicData.setMana(100.0F);
            helper.assertTrue(rider.startRiding(broom, true), "Controller test rider should mount directly");

            broom.acceptServerInput(nonController, 0.0F, 1.0F, true, false);
            broom.tick();

            helper.assertTrue(Math.abs(riderMagicData.getMana() - 100.0F) < 1.0e-4F,
                    "A non-controlling player must not consume the rider's mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "A non-controlling player must not trigger the rider's emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void depletionEmergencyRecoveryAndSavedStateFollowServerRules(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_emergency");
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Emergency test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "A movement cost above the remaining mana should consume the remainder");
            helper.assertTrue(broom.isManaEmergencyLanding(), "Movement depletion should enter emergency landing");

            magicData.setMana(10.0F);
            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                    "Emergency movement must not consume mana");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isManaEmergencyLanding(), "Emergency landing must persist in entity NBT");

            magicData.setMana(100.0F);
            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Reaching the normal flight threshold should recover normal flight");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Recovery tick must not immediately consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalDepletionWaitsForPaidInputAndEmergencyForcesDismountWarning(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_external_depletion");
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);
            helper.assertTrue(player.startRiding(broom, true), "External depletion test player should mount directly");

            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "External zero mana must not trigger emergency landing while idle");
            var warningState = new CompoundTag();
            broom.saveWithoutId(warningState);
            helper.assertTrue(warningState.getBoolean("LowManaWarningShown"),
                    "Low mana warning latch should be stored on the broom entity");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);
            broom.tick();
            helper.assertTrue(broom.isManaEmergencyLanding(),
                    "Paid movement attempted at zero mana should trigger emergency landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Emergency landing must force dismount confirmation even near the ground");

            FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
            var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
            FloatmountBroomDismountEvents.onDismount(first);
            helper.assertTrue(first.isCanceled(), "First emergency dismount should be canceled");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomUsesCoastDamping(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        broom.setDeltaMovement(0.2D, 0.0D, 0.0D);

        broom.tick();

        helper.assertTrue(Math.abs(broom.getDeltaMovement().x - 0.17D) < 1.0e-6D,
                "Unoccupied broom should retain eighty-five percent of horizontal speed per tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damagedUnoccupiedBroomRisesOutOfLava(GameTestHelper helper) {
        var level = helper.getLevel();
        var lava = helper.absolutePos(TEST_POS);
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(), Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(2), Blocks.LAVA.defaultBlockState());

        var broom = spawnBroom(helper, 0.2D);
        broom.hurt(level.damageSources().fellOutOfWorld(), 20.0F);
        broom.setDeltaMovement(0.0D, -0.15D, 0.0D);
        var startY = broom.getY();

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(broom.isDamaged(), "Lava recovery must not clear the damaged state");
            helper.assertFalse(broom.isVehicle(), "Lava recovery test broom must remain unoccupied");
            helper.assertTrue(broom.getDeltaMovement().y > 0.0D,
                    "Damaged unoccupied broom should reverse its downward motion while in lava");
            helper.assertTrue(broom.getY() > startY,
                    "Damaged unoccupied broom should rise toward the lava surface");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void surfaceScannerTreatsSolidWaterAndLavaAsHoverSurfaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var solid = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(2, 1, 1));
        var lava = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(lava.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());

        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, solid.getX() + 0.5D, solid.getY() + 2.0D, solid.getZ() + 0.5D, 3, true).isPresent(),
                "Solid collision shape should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, water.getX() + 0.5D, water.getY() + 2.0D, water.getZ() + 0.5D, 3, true).isPresent(),
                "Water should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, true).isPresent(),
                "Lava should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                        level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, false).isEmpty(),
                "Lava must make the safe dismount scan fail even with solid ground beneath it");

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(lava.getX() + 0.5D, lava.getY() + 1.5D, lava.getZ() + 0.5D);
        helper.assertTrue(broom.isDangerousDismount(),
                "Lava below the broom must always be classified as a dangerous dismount surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountUsesBroomLeftSurfaceInsteadOfPassengerView(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = player(helper, "floatmount_broom_safe_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Safe dismount test player should mount the broom");
        player.setYRot(180.0F);

        var target = broom.getDismountLocationForPassenger(player);
        var minimumSeparation = (broom.getBbWidth() + player.getBbWidth()) / 2.0D;
        helper.assertTrue(target.x - broom.getX() > minimumSeparation,
                "Dismount target should clear both bounding boxes on the broom's left side");
        helper.assertTrue(Math.abs(target.z - broom.getZ()) < 1.0E-5D,
                "Passenger view must not rotate the broom-relative dismount side");
        helper.assertTrue(Math.abs(target.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount should resolve directly to the preferred-side surface");
        helper.assertFalse(broom.isDangerousDismount(),
                "A valid preferred-side surface below two blocks should be safe");

        player.stopRiding();
        helper.assertTrue(Math.abs(player.getY() - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Server dismount should place the rider directly on the resolved surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountUsesPreferredSideInsteadOfBroomCenter(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = player(helper, "floatmount_broom_cliff_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Cliff dismount test player should mount the broom");
        helper.assertTrue(broom.isDangerousDismount(),
                "Ground below the broom must not make a preferred-side cliff safe");

        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        helper.assertFalse(broom.isDangerousDismount(),
                "Preferred-side ground should allow a safe dismount even when the broom center is over a cliff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountRequiresSurfaceBelowTwoBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, leftGround.getY() + 3.0D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = player(helper, "floatmount_broom_height_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Height boundary test player should mount the broom");

        helper.assertTrue(broom.isDangerousDismount(),
                "A surface exactly two blocks below should require dangerous dismount confirmation");
        var dangerousTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(dangerousTarget.y - broom.getY()) < 1.0E-5D,
                "Dangerous dismount must not warp the rider down to the surface");

        broom.setPos(broom.getX(), leftGround.getY() + 2.99D, broom.getZ());
        helper.assertFalse(broom.isDangerousDismount(),
                "A surface just under two blocks below should allow a direct safe dismount");
        var safeTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(safeTarget.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount below the threshold should resolve to the server surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountRequiresReleaseAndSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = player(helper, "floatmount_broom_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());
        helper.assertTrue(broom.isDangerousDismount(), "High broom should be classified as dangerous");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(held);
        helper.assertTrue(held.isCanceled(), "Holding sneak must not confirm dismount");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, false);
        helper.assertTrue(player.getVehicle() == broom, "Releasing sneak should not dismount by itself");
        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        helper.assertFalse(player.isPassenger(), "Second press within thirty ticks should dismount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dangerousDismountNeverTreatsHeldSneakAsSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = player(helper, "floatmount_broom_held_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Held dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        helper.runAfterDelay(FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS + 2, () -> {
            FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
            var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
            FloatmountBroomDismountEvents.onDismount(held);
            helper.assertTrue(held.isCanceled(), "Held sneak must remain canceled after the confirmation window");
            helper.assertTrue(player.getVehicle() == broom, "Held sneak must never dismount the rider");
            helper.succeed();
        });
    }

    private static FloatmountBroomEntity spawnBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static void assertTooltipLine(GameTestHelper helper, List<Component> lines, int index,
                                          String expectedKey, int expectedArgumentCount) {
        helper.assertTrue(lines.get(index).getStyle().getColor() != null
                        && lines.get(index).getStyle().getColor().getValue() == ChatFormatting.GRAY.getColor(),
                "Tooltip line " + index + " should be gray");
        helper.assertTrue(lines.get(index).getContents() instanceof TranslatableContents,
                "Tooltip line " + index + " should be translatable");
        if (lines.get(index).getContents() instanceof TranslatableContents contents) {
            helper.assertTrue(expectedKey.equals(contents.getKey()),
                    "Tooltip line " + index + " has an unexpected translation key");
            helper.assertTrue(contents.getArgs().length == expectedArgumentCount,
                    "Tooltip line " + index + " has an unexpected argument count");
        }
    }

    private static void assertComponentKey(GameTestHelper helper, Object argument,
                                           String expectedKey, String message) {
        helper.assertTrue(argument instanceof Component, message + " (argument is not a component)");
        if (argument instanceof Component component) {
            helper.assertTrue(component.getContents() instanceof KeybindContents contents
                            && expectedKey.equals(contents.getName()), message);
        }
    }

    private static FloatmountBroomEntity placeBroomFromItem(GameTestHelper helper, Player player, ItemStack stack) {
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var brooms = helper.getLevel().getEntitiesOfClass(
                FloatmountBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        );
        helper.assertTrue(brooms.size() == 1, "Broom item use should place exactly one broom entity");
        return brooms.getFirst();
    }

    private static ItemStack findBroomInInventory(GameTestHelper helper, Player player) {
        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(ItemRegistry.FLOATMOUNT_BROOM.get())) {
                return stack;
            }
        }
        helper.fail("Recovered broom item was not found in the player inventory");
        return ItemStack.EMPTY;
    }

    private static Player player(GameTestHelper helper, String name) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static MagicData magicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Floatmount Broom test could not resolve player mana data");
        return magicData;
    }

    private static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending
    ) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, playerName);
        var magicData = magicData(helper, player);
        magicData.setMana(200.0F);
        helper.assertTrue(player.startRiding(broom, true), playerName + " should mount directly");
        broom.acceptServerInput(player, strafe, forward, ascending, descending);

        broom.tick();

        helper.assertTrue(Math.abs(magicData.getMana() - (200.0F - expectedCost)) < 1.0e-4F,
                playerName + " used an unexpected mana cost: " + magicData.getMana());
        player.stopRiding();
    }

    private static int countDroppedBrooms(GameTestHelper helper, net.minecraft.world.phys.Vec3 center) {
        return droppedBrooms(helper, center)
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static ItemEntity findDroppedBroom(GameTestHelper helper, net.minecraft.world.phys.Vec3 center) {
        var drops = droppedBrooms(helper, center);
        helper.assertTrue(drops.size() == 1, "Expected exactly one dropped broom item entity");
        return drops.getFirst();
    }

    private static java.util.List<ItemEntity> droppedBrooms(GameTestHelper helper,
                                                             net.minecraft.world.phys.Vec3 center) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(center, 4.0D, 4.0D, 4.0D),
                item -> item.getItem().is(ItemRegistry.FLOATMOUNT_BROOM.get()));
    }
}
