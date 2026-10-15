package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsCharge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsChargeMovement;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsUseSession;
import jp.aquafactory.apprenticecodex.network.packet.ClientPsychicDriveGauntletsUsePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsUseStatePacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録される。
public final class PsychicDriveGauntletsChargeGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private PsychicDriveGauntletsChargeGameTests() {}

    private static ItemStack gauntlets() { return new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()); }

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_charge");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlets());
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        // 未追加のFakePlayerでは装備属性更新を行わず、通常装備時の実効値を与える。
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(8);
        return player;
    }

    private static Cow target(GameTestHelper helper, Vec3 position) {
        var cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        cow.setPos(position);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(100);
        cow.setHealth(100);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    @GameTest(template = TEMPLATE)
    public static void duplicateCountsHotbarAndPhysicalOffhandOnly(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(9, gauntlets());
        helper.assertTrue(!PsychicDriveGauntletsSelection.duplicate(player), "Storage inventory must allow spares");
        player.getInventory().setItem(8, gauntlets());
        player.getInventory().getItem(8).set(DataComponents.CUSTOM_NAME, Component.literal("Spare"));
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.DISABLED,
                "A nonselected hotbar copy must disable all gauntlets");
        helper.assertTrue(!new PsychicDriveGauntletsAttackState().input(player, Input.PRESS, 0),
                "Duplicate gauntlets must reject left attacks");
        helper.assertTrue(!PsychicDriveGauntletsSelection.canUse(player, InteractionHand.MAIN_HAND),
                "Duplicate gauntlets must reject right attacks");
        player.getInventory().setItem(8, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlets());
        helper.assertTrue(PsychicDriveGauntletsSelection.duplicate(player), "Physical offhand must count toward duplicates");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue(!PsychicDriveGauntletsSelection.duplicate(player)
                        && PsychicDriveGauntletsSelection.canUse(player, InteractionHand.OFF_HAND),
                "A lone offhand gauntlet must remain usable with a mainhand sword");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mainhandRequiresCompletelyEmptyPhysicalOffhand(GameTestHelper helper) {
        var player = player(helper);
        helper.assertTrue(PsychicDriveGauntletsSelection.canUse(player, InteractionHand.MAIN_HAND),
                "An empty offhand must enable mainhand use");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!PsychicDriveGauntletsSelection.canUse(player, InteractionHand.MAIN_HAND),
                "Even an item with no use action must disable mainhand use");
        helper.assertTrue(new PsychicDriveGauntletsAttackState().input(player, Input.PRESS, 0),
                "A nonempty offhand must not disable a lone mainhand gauntlet's left attack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void duplicateUseConsumesOnlyGauntletOperation(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTION));
        player.getInventory().setItem(8, gauntlets());
        var potion = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(potion.getResult().consumesAction() && player.isUsingItem(),
                "Duplicate gauntlets must not prevent a mainhand potion from being used first");
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        var sword = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!sword.getResult().consumesAction(), "A vanilla sword must pass use to the offhand");
        var rejected = player.getOffhandItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(rejected.getResult().consumesAction() && !player.isUsingItem(),
                "Offhand duplicate error must consume use without starting a charge");
        var attack = new AttackEntityEvent(player,
                target(helper, player.getEyePosition().add(0, -0.5, 2)));
        PsychicDriveGauntletsAttackEvents.attackEntity(attack);
        helper.assertTrue(!attack.isCanceled(), "Duplicate gauntlets must not suppress the mainhand sword's attack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatHiddenOffhandIsSheathedAndStillCounts(GameTestHelper helper) {
        if (ModList.get().isLoaded("bettercombat")) {
            var weapon = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker"));
            var player = ApprenticeCodexGameTestScenarios.createBetterCombatHiddenOffhandPlayer(helper,
                    new ItemStack(weapon), gauntlets(), "psychic_hidden_offhand");
            helper.assertTrue(player.getOffhandItem().isEmpty(), "Two-handed weapon must hide the logical offhand");
            helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.SHEATHED
                            && !PsychicDriveGauntletsSelection.canUse(player, InteractionHand.OFF_HAND),
                    "A lone hidden physical gauntlet must be sheathed and unusable");
            player.getInventory().setItem(8, gauntlets());
            helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.DISABLED,
                    "Hidden physical offhand must still disable a second hotbar gauntlet");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargingReservesLeftAndAllowsOnlyOtherArm(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 0) && state.left().phase() == Phase.CHARGING,
                "Use must reserve the left arm first");
        helper.assertTrue(state.input(player, Input.PRESS, 0) && state.right().phase() == Phase.OUTBOUND,
                "Left attack must still launch the unreserved right arm");
        state.input(player, Input.RELEASE, 0);
        helper.assertTrue(!state.input(player, Input.PRESS, 0), "Attack must not steal the charging arm");
        helper.assertTrue(state.releaseUse(player, 20) && state.left().snapshot().charge() == 1,
                "The reserved left arm must launch at full charge on release");
        helper.assertTrue(!state.releaseUse(player, 20), "A second release must not launch another attack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void busyStartCannotRecoverUntilReleased(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        state.input(player, Input.PRESS, 0);
        state.input(player, Input.RELEASE, 0);
        state.input(player, Input.PRESS, 0);
        helper.assertTrue(!state.beginUse(player, InteractionHand.MAIN_HAND, 0), "Busy arms must reject use start");
        for (int tick = 1; tick <= 25; tick++) state.tick(player, tick);
        helper.assertTrue(!state.beginUse(player, InteractionHand.MAIN_HAND, 25),
                "Returning arms must not revive the rejected use press");
        state.releaseUse(player, 25);
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 26), "A new press must be accepted after release");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeThresholdAndCapUseServerElapsedTicks(GameTestHelper helper) {
        var player = player(helper);
        for (int duration : new int[] {0, 4, 5, 19, 20, 21, 200}) {
            var state = new PsychicDriveGauntletsAttackState();
            state.beginUse(player, InteractionHand.MAIN_HAND, 100);
            helper.assertTrue(state.releaseUse(player, 100 + duration) == (duration >= 5),
                    "Release threshold mismatch for elapsed ticks " + duration);
            if (duration >= 5) {
                var snapshot = state.left().snapshot();
                helper.assertTrue(Math.abs(snapshot.charge() - Math.min(duration, 20) / 20F) < 1.0e-6,
                        "Charge must be linear and capped for elapsed ticks " + duration);
                helper.assertTrue(snapshot.destination().distanceTo(player.getEyePosition()) <= 12.000001,
                        "Server aim must stay within the maximum charge range");
            } else helper.assertTrue(!state.active(), "A short charge must release its reserved arm without firing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void duplicateAndSourceChangesAbortWithoutReviving(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        state.beginUse(player, InteractionHand.MAIN_HAND, 0);
        state.input(player, Input.PRESS, 0);
        var cow = target(helper, player.getEyePosition().add(0, -0.5, 3));
        player.getInventory().setItem(8, gauntlets());
        state.tick(player, 1);
        helper.assertTrue(!state.active() && cow.getHealth() == 100,
                "Duplicates must discard charge and flight before any invisible hit");
        player.getInventory().setItem(8, ItemStack.EMPTY);
        helper.assertTrue(!state.input(player, Input.REPEAT, 30)
                        && !state.beginUse(player, InteractionHand.MAIN_HAND, 30),
                "Resolving duplicates must not revive either held input");
        state.releaseUse(player, 30);
        state.beginUse(player, InteractionHand.MAIN_HAND, 31);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlets());
        state.tick(player, 40);
        helper.assertTrue(!state.releaseUse(player, 51) && !state.active(),
                "Replacing the source with an identical item must cancel the charge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandDamageReplacesWeaponModifiersWithoutMutatingPlayer(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        var damage = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE));
        damage.setBaseValue(1);
        sword.forEachModifier(EquipmentSlot.MAINHAND, (type, modifier) -> {
            if (type.equals(Attributes.ATTACK_DAMAGE)) damage.addOrUpdateTransientModifier(modifier);
        });
        damage.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "charge_test_flat"), 3, Operation.ADD_VALUE));
        damage.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "charge_test_base"), 0.5, Operation.ADD_MULTIPLIED_BASE));
        damage.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "charge_test_total"), 0.2, Operation.ADD_MULTIPLIED_TOTAL));
        double original = damage.getValue();
        helper.assertTrue(Math.abs(PsychicDriveGauntletsCharge.attackDamage(player, player.getOffhandItem()) - 19.8) < 1.0e-5,
                "Offhand damage must replace sword bonus with gauntlet bonus and preserve attribute operation order");
        helper.assertTrue(damage.getValue() == original && player.getMainHandItem() == sword,
                "Calculating offhand damage must not mutate equipment or player attributes");
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(!state.input(player, Input.PRESS, 0) && state.beginUse(player, InteractionHand.OFF_HAND, 0),
                "Offhand gauntlets must allow use without supplying left attacks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeMultipliesEnchantedDamageAndRespectsResistance(GameTestHelper helper) {
        var player = player(helper);
        var sharpness = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SHARPNESS);
        player.getMainHandItem().enchant(sharpness, 3);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var cow = target(helper, start.add(0, -0.5, 2));
        Objects.requireNonNull(cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(1);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, start.add(0, 0, 12), 1, 0, InteractionHand.MAIN_HAND, 1);
        arm.tick(player, 1, 1);
        helper.assertTrue(Math.abs(cow.getHealth() - 85) < 1.0e-6,
                "Full charge must multiply enchanted damage ten by one and a half");
        helper.assertTrue(cow.getDeltaMovement().lengthSqr() < 1.0e-12,
                "Full knockback resistance must block the extra charge knockback");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullChargeAddsOneKnockbackLevelAfterSuccessfulHit(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var cow = target(helper, start.add(0, -0.5, 2));
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, start.add(0, 0, 12), 1, 0, InteractionHand.MAIN_HAND, 1);
        arm.tick(player, 1, 1);
        helper.assertTrue(cow.getDeltaMovement().z >= 0.49,
                "Full charge must supply one enchantment level of forward knockback");
        var movement = cow.getDeltaMovement();
        var second = new PsychicDriveGauntletsArm(true);
        second.launch(player, start.add(0, 0, 12), 1, 1, InteractionHand.MAIN_HAND, 1);
        second.tick(player, 1, 2);
        helper.assertTrue(cow.getDeltaMovement().equals(movement), "Iframe rejection must not add charge knockback");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rangeExtensionPreservesNormalizedFlightTime(GameTestHelper helper) {
        var start = Vec3.ZERO;
        for (float charge : new float[] {0, 0.25F, 0.95F, 1}) {
            double range = PsychicDriveGauntletsCharge.range(charge);
            var end = new Vec3(0, 0, range);
            for (double tick : new double[] {0, 0.5, 1, 2, 3}) {
                double progress = PsychicDriveGauntletsMotion.outbound(start, end, tick, charge).z / range;
                double normal = PsychicDriveGauntletsMotion.outbound(start, new Vec3(0, 0, 6), tick).z / 6;
                helper.assertTrue(Math.abs(progress - normal) < 1.0e-7,
                        "Extended range must preserve normalized flight time at charge " + charge);
            }
        }
        var player = player(helper);
        player.setXRot(-90);
        helper.assertTrue(Math.abs(PsychicDriveGauntletsRaycast.aim(player, 12).point().distanceTo(player.getEyePosition()) - 12) < 1.0e-6,
                "Full-charge empty aim must reach twelve blocks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void independentUseIgnoresVanillaUseAndDuplicatePackets(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        var hand = InteractionHand.MAIN_HAND;
        player.getMainHandItem().use(helper.getLevel(), player, hand);
        helper.assertTrue(!player.isUsingItem(), "Vanilla use must never start a gauntlet charge");
        helper.assertTrue(!state.useInput(player, UseAction.RELEASE, hand, 1, 0),
                "Release without a start must do nothing");
        helper.assertTrue(state.useInput(player, UseAction.START, hand, 1, 100), "Independent start must succeed");
        state.tick(player, 110);
        helper.assertTrue(state.charging() && !player.isUsingItem(), "Charge must survive without vanilla use state");
        helper.assertTrue(state.useInput(player, UseAction.START, hand, 1, 119), "Duplicate start must preserve charge");
        state.useInput(player, UseAction.RELEASE, InteractionHand.OFF_HAND, 1, 120);
        state.useInput(player, UseAction.CANCEL, hand, 2, 120);
        helper.assertTrue(state.charging(), "Wrong hand or session must not end a charge");
        state.useInput(player, UseAction.RELEASE, hand, 1, 120);
        helper.assertTrue(state.left().phase() == Phase.OUTBOUND && state.left().snapshot().charge() == 1,
                "Duplicate start must not reset server elapsed time");
        var launched = state.left().snapshot();
        state.useInput(player, UseAction.RELEASE, hand, 1, 121);
        state.useInput(player, UseAction.START, hand, 1, 122);
        helper.assertTrue(launched.equals(state.left().snapshot()) && !state.charging(),
                "Ended sessions must not fire twice or restart");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedAndCanceledSessionsRequireNewPress(GameTestHelper helper) {
        var player = player(helper);
        var hand = InteractionHand.MAIN_HAND;
        var state = new PsychicDriveGauntletsAttackState();
        player.getInventory().setItem(8, gauntlets());
        helper.assertTrue(!state.useInput(player, UseAction.START, hand, 1, 0), "Duplicate source must reject start");
        player.getInventory().setItem(8, ItemStack.EMPTY);
        helper.assertTrue(!state.useInput(player, UseAction.START, hand, 1, 20)
                        && !state.useInput(player, UseAction.START, hand, 2, 20),
                "A rejected held press must not recover before a terminal input");
        state.useInput(player, UseAction.CANCEL, hand, 1, 21);
        helper.assertTrue(state.useInput(player, UseAction.START, hand, 2, 22), "New press must recover");
        state.useInput(player, UseAction.CANCEL, hand, 2, 42);
        state.useInput(player, UseAction.RELEASE, hand, 2, 43);
        helper.assertTrue(!state.active(), "Canceled full charge must never launch on a late release");
        helper.assertTrue(state.useInput(player, UseAction.START, hand, 3, 44), "Next press must charge");
        player.setItemInHand(hand, gauntlets());
        state.useInput(player, UseAction.RELEASE, hand, 3, 64);
        helper.assertTrue(!state.active(), "Source replacement must reject release even before the next server tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void independentChargeDoesNotStopOtherItems(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        var gauntlet = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTION));
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        state.useInput(player, UseAction.START, InteractionHand.OFF_HAND, 1, 0);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        state.tick(player, 20);
        helper.assertTrue(!state.charging() && player.isUsingItem(),
                "Other item use must cancel the charge without stopping that item");
        state.useInput(player, UseAction.RELEASE, InteractionHand.OFF_HAND, 1, 21);
        helper.assertTrue(!state.active(), "Canceled charge must not fire after other item use");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeMovementStacksAndAlwaysRemovesOnlyItsOwnModifier(GameTestHelper helper) {
        var player = player(helper);
        var speed = Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED));
        var other = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "charge_other_speed_test");
        speed.addTransientModifier(new AttributeModifier(other, 0.5, Operation.ADD_MULTIPLIED_TOTAL));
        double original = speed.getValue();
        PsychicDriveGauntletsChargeMovement.update(player, true);
        PsychicDriveGauntletsChargeMovement.update(player, true);
        helper.assertTrue(Math.abs(speed.getValue() - original * 0.8) < 1.0e-6,
                "Charge slowdown must stack once with other movement modifiers");
        PsychicDriveGauntletsChargeMovement.update(player, false);
        helper.assertTrue(Math.abs(speed.getValue() - original) < 1.0e-6 && speed.hasModifier(other),
                "Charge end must restore movement without removing other modifiers");
        PsychicDriveGauntletsChargeMovement.update(player, true);
        player.setHealth(0);
        PsychicDriveGauntletsChargeMovement.update(player, true);
        helper.assertTrue(Math.abs(speed.getValue() - original) < 1.0e-6, "Death must remove charge slowdown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lifecycleEventsRemoveIndependentChargeMovement(GameTestHelper helper) {
        var player = player(helper);
        var speed = Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED));
        double original = speed.getValue();
        for (int reason = 0; reason < 3; reason++) {
            long session = reason + 1;
            PsychicDriveGauntletsAttackEvents.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, session);
            helper.assertTrue(Math.abs(speed.getValue() - original * 0.8) < 1.0e-6,
                    "Accepted server input must apply charge slowdown");
            switch (reason) {
                case 0 -> PsychicDriveGauntletsAttackEvents.dimension(
                        new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
                case 1 -> PsychicDriveGauntletsAttackEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
                case 2 -> PsychicDriveGauntletsAttackEvents.death(new LivingDeathEvent(player, player.damageSources().generic()));
                default -> throw new IllegalStateException("Unexpected lifecycle test case");
            }
            helper.assertTrue(Math.abs(speed.getValue() - original) < 1.0e-6,
                    "Lifecycle cleanup must remove the charge movement modifier");
            PsychicDriveGauntletsAttackEvents.useInput(player, UseAction.RELEASE, InteractionHand.MAIN_HAND, session);
            helper.assertTrue(Math.abs(speed.getValue() - original) < 1.0e-6,
                    "Late release after cleanup must not restore charge movement");
        }
        PsychicDriveGauntletsAttackEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void menusAndOtherItemUseRejectIndependentStarts(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        player.containerMenu = ChestMenu.oneRow(1, player.getInventory());
        helper.assertTrue(!state.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, 1, 0),
                "Open container must reject an independent start");
        state.useInput(player, UseAction.CANCEL, InteractionHand.MAIN_HAND, 1, 1);
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(state.useInput(player, UseAction.START, InteractionHand.MAIN_HAND, 2, 2),
                "New press after closing the menu must succeed");
        player.containerMenu = ChestMenu.oneRow(2, player.getInventory());
        state.useInput(player, UseAction.RELEASE, InteractionHand.MAIN_HAND, 2, 22);
        helper.assertTrue(!state.active(), "Opening a menu must reject release before the next state tick");
        player.containerMenu = player.inventoryMenu;
        var gauntlet = player.getMainHandItem();
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTION));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(!state.useInput(player, UseAction.START, InteractionHand.OFF_HAND, 3, 23)
                        && player.isUsingItem(),
                "Active item use must reject charge start without stopping the item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void delayedUseResponsesCannotChangeAnotherPress(GameTestHelper helper) {
        var session = new PsychicDriveGauntletsUseSession();
        long first = session.begin(100);
        session.end();
        session.accept(first, true);
        helper.assertTrue(!session.active(), "Late acceptance must not revive a released press");
        long second = session.begin(200);
        session.accept(first, false);
        helper.assertTrue(session.active() && session.elapsed(205) == 5,
                "Old rejection must not cancel or retime a new press");
        session.accept(second, false);
        helper.assertTrue(!session.active(), "Matching rejection must end the current charge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void independentUsePacketsPreserveSessionAndHand(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        try {
            for (var action : UseAction.values()) {
                for (var hand : InteractionHand.values()) {
                    var packet = new ClientPsychicDriveGauntletsUsePacket(action, hand, 123456789L);
                    ClientPsychicDriveGauntletsUsePacket.STREAM_CODEC.encode(buffer, packet);
                    helper.assertTrue(packet.equals(ClientPsychicDriveGauntletsUsePacket.STREAM_CODEC.decode(buffer)),
                            "Use input must preserve action, hand and session");
                }
            }
            for (boolean charging : new boolean[] {false, true}) {
                var packet = new SyncPsychicDriveGauntletsUseStatePacket(123456789L, charging);
                SyncPsychicDriveGauntletsUseStatePacket.STREAM_CODEC.encode(buffer, packet);
                helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsUseStatePacket.STREAM_CODEC.decode(buffer)),
                        "Use response must preserve session and charge state");
            }
            helper.assertTrue(buffer.readableBytes() == 0, "Use codecs must consume complete payloads");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargePacketsPreserveDisplayModeAndSpeed(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        state.beginUse(player, InteractionHand.MAIN_HAND, 100);
        state.tick(player, 119);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        try {
            for (var mode : Mode.values()) {
                var packet = new SyncPsychicDriveGauntletsPacket(player.getUUID(), false,
                        state.right().snapshot(), state.left().snapshot(), mode);
                SyncPsychicDriveGauntletsPacket.STREAM_CODEC.encode(buffer, packet);
                helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsPacket.STREAM_CODEC.decode(buffer)),
                        "Charge snapshots and authoritative display mode must survive packet encoding");
            }
            helper.assertTrue(buffer.readableBytes() == 0, "Charge packet decoding must consume the entire payload");
        } finally { buffer.release(); }
        helper.succeed();
    }
}
