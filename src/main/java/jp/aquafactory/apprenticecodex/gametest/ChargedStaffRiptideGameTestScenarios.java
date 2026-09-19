package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.ArrayList;
import java.util.Objects;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargedTwinBladeStaffServerConfig;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

final class ChargedStaffRiptideGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ChargedStaffRiptideGameTestScenarios() {
    }

    static void upkeepAndExhaustion(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_riptide_upkeep");
        var stack = equip(player, 3);
        var data = MagicData.getPlayerMagicData(player);
        data.setMana(140);
        helper.runAtTickTime(1, () -> chargeAndRelease(player, stack));
        // FakePlayerはworldで自動tickされない。aiStepを実行して本番の維持・衝突hookを通す。
        for (int tick = 2; tick <= 62; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                if (current == 2) {
                    stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
                }
                player.setPos(helper.absoluteVec(new Vec3(0.5, 100, 0.5)));
                player.setYRot(current % 2 == 0 ? 90 : 0);
                player.setXRot(0);
                player.setOnGround(false);
                // この試験は空中維持と課金を調べる。FakePlayerの手動aiStepでは周辺構造に衝突し得るため、衝突は別試験へ分離する。
                player.noPhysics = true;
                player.horizontalCollision = false;
                player.aiStep();
                if (current <= 60) {
                    helper.assertTrue(player.isAutoSpinAttack(), "Sustained Riptide ended before mana exhaustion: tick="
                            + current + ", mana=" + data.getMana() + ", maintenance="
                            + ChargedTwinBladeStaffRiptide.isMaintenanceInput(player) + ", using="
                            + player.isUsingItem() + ", sameStack=" + (player.getMainHandItem() == stack)
                            + ", horizontalCollision=" + player.horizontalCollision);
                    int payments = current < 21 ? 0 : (current - 21) / 10 + 1;
                    helper.assertTrue(Math.abs(data.getMana() - (90 - payments * 20)) < 0.01,
                            "Riptide upkeep mismatch: tick=" + current + ", mana=" + data.getMana() + ", expected=" + (90 - payments * 20));
                    // 同一tickの再評価で課金も推進も重複させない。
                    var velocity = player.getDeltaMovement();
                    ChargedTwinBladeStaffRiptide.tick(player);
                    helper.assertTrue(player.getDeltaMovement().equals(velocity),
                            "Riptide must not apply propulsion twice in one tick");
                    helper.assertTrue(Math.abs(data.getMana() - (90 - payments * 20)) < 0.01,
                            "Riptide charged upkeep twice in one tick");
                } else {
                    helper.assertTrue(!player.isAutoSpinAttack(), "Insufficient upkeep mana must end Riptide");
                    helper.assertTrue(ChargedTwinBladeStaffRiptide.isMaintenanceInput(player),
                            "Interrupted maintenance must remain latched until release");
                    data.setMana(100);
                    player.releaseUsingItem();
                    helper.assertTrue(data.getMana() == 100 && !player.isAutoSpinAttack(),
                            "Releasing interrupted maintenance must not reactivate Riptide");
                    helper.succeed();
                }
            });
        }
    }

    static void independentCostsAndFreeUpkeep(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_riptide_costs");
        var stack = equip(player, 1);
        var data = MagicData.getPlayerMagicData(player);
        data.setMana(10);
        helper.runAtTickTime(1, () -> {
            try (var ignored = config(7, 3)) {
                chargeAndRelease(player, stack);
                helper.assertTrue(data.getMana() == 3, "Initial Riptide cost must use its own setting");
                stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            }
        });
        for (int tick = 2; tick <= 32; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                try (var ignored = config(7, current <= 21 ? 3 : 0)) {
                    ChargedTwinBladeStaffRiptide.tick(player);
                    if (current == 21) {
                        helper.assertTrue(data.getMana() == 0 && player.isAutoSpinAttack(),
                                "An exact upkeep balance must pay successfully");
                    }
                    if (current == 32) {
                        helper.assertTrue(data.getMana() == 0 && player.isAutoSpinAttack(),
                                "Reloaded zero upkeep must sustain Riptide without mana");
                        helper.assertTrue(Math.abs(player.getDeltaMovement().length() - 0.8) < 0.001,
                                "Riptide I must sustain at its own speed");
                        player.releaseUsingItem();
                        helper.assertTrue(!player.isAutoSpinAttack(), "Releasing maintenance must stop Riptide immediately");
                        helper.succeed();
                    }
                }
            });
        }
    }

    static void collisionAndFreshCharge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_riptide_collision");
            var stack = equip(player, 1);
            var data = MagicData.getPlayerMagicData(player);
            data.setMana(200);
            chargeAndRelease(player, stack);
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            var target = EntityType.COW.create(helper.getLevel());
            Objects.requireNonNull(target, "Could not create collision target");
            target.setPos(player.position());
            helper.getLevel().addFreshEntity(target);
            try {
                ((LivingEntityAccessor) player).apprenticecodex$checkAutoSpinAttack(player.getBoundingBox(), player.getBoundingBox());
                helper.assertTrue(!player.isAutoSpinAttack(), "Entity collision must stop sustained Riptide");
                ChargedTwinBladeStaffRiptide.tick(player);
                helper.assertTrue(!player.isAutoSpinAttack(), "Held maintenance must not revive a collision-ended spin");
                stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(ChargedTwinBladeStaffRiptide.isMaintenanceInput(player),
                        "Repeated use during the same held input must not turn interrupted maintenance into a charge");
                player.releaseUsingItem();
                helper.assertTrue(data.getMana() == 150, "Collision release must not pay another initial cost: mana=" + data.getMana());
            } finally {
                target.discard();
            }
            chargeAndRelease(player, stack);
            helper.assertTrue(player.isAutoSpinAttack() && data.getMana() == 100,
                    "A fresh charge after interruption must reactivate Riptide normally");
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            player.horizontalCollision = true;
            ((LivingEntityAccessor) player).apprenticecodex$checkAutoSpinAttack(player.getBoundingBox(), player.getBoundingBox());
            helper.assertTrue(!player.isAutoSpinAttack(), "Horizontal collision must stop sustained Riptide");
            player.releaseUsingItem();
            helper.assertTrue(data.getMana() == 100, "Wall collision release must not reactivate Riptide");
        });
    }

    static void equipmentAndCreativeBoundaries(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_riptide_boundaries");
            var stack = equip(player, 2);
            player.getAbilities().instabuild = true;
            MagicData.getPlayerMagicData(player).setMana(0);
            chargeAndRelease(player, stack);
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            ChargedTwinBladeStaffRiptide.tick(player);
            helper.assertTrue(player.isAutoSpinAttack(), "Creative players must sustain Riptide without mana");
            helper.assertTrue(player.getDeltaMovement().length() > 1.0 && player.getDeltaMovement().length() < 2.25,
                    "Riptide II must smoothly approach cruise speed from its initial impulse");
            player.releaseUsingItem();
            player.startAutoSpinAttack(20);
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!ChargedTwinBladeStaffRiptide.isMaintenanceInput(player),
                    "A spin started outside this staff must not enter maintenance");
            player.stopUsingItem();
            chargeAndRelease(player, stack);
            player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
            helper.assertTrue(!stack.getItem().use(player.level(), player, InteractionHand.OFF_HAND).getResult().consumesAction(),
                    "Offhand use must not activate maintenance");
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            ChargedTwinBladeStaffRiptide.tick(player);
            helper.assertTrue(!player.isAutoSpinAttack(), "Switching the physical mainhand stack must end maintenance");
        });
    }

    static void cruiseSpeedAndSteering(GameTestHelper helper) {
        // 初動、巡航、旋回を同じ使用セッションで検証し、上限突破と鉛直方向も含める。
        for (int level : new int[]{1, 2, 3, 5}) {
            for (int pitch : new int[]{0, -45, -90}) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_cruise_" + level + "_" + pitch);
                var stack = equip(player, level);
                player.getAbilities().instabuild = true;
                player.setXRot(pitch);
                player.setYRot(0);
                player.setOnGround(false);
                chargeAndRelease(player, stack);
                double initialSpeed = 3.0 * (1 + level) / 4.0;
                helper.assertTrue(Math.abs(player.getDeltaMovement().length() - initialSpeed) < 1.0e-5,
                        "Cruise changes must not alter the initial Riptide impulse");
                stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
                for (int tick = 1; tick <= 80; tick++) {
                    int current = tick;
                    helper.runAtTickTime(tick, () -> {
                        ChargedTwinBladeStaffRiptide.tick(player);
                        var velocity = player.getDeltaMovement();
                        if (current == 1) {
                            helper.assertTrue(velocity.length() < initialSpeed && velocity.length() > (12 + level * 4) / 20.0,
                                    "Maintenance must approach the target without immediately clamping the launch speed");
                        }
                        if (current == 40 || current == 80) {
                            helper.assertTrue(velocity.distanceTo(player.getLookAngle().scale((12 + level * 4) / 20.0)) < 0.001,
                                    "Cruise velocity must converge to the level target for level=" + level + ", pitch=" + pitch);
                        }
                        if (current == 40) {
                            player.setYRot(90);
                        }
                        if (current == 41 && pitch == 0) {
                            helper.assertTrue(velocity.x < 0 && velocity.z > 0,
                                    "Steering must retain momentum instead of instantly snapping to the new direction");
                        }
                        // FakePlayerの手動更新でも通常空中の減速・重力を挟み、上昇不能や速度低下を検出する。
                        player.setDeltaMovement(velocity.subtract(0, 0.08D, 0).multiply(0.91, 0.98, 0.91));
                    });
                }
            }
        }
        helper.runAtTickTime(81, helper::succeed);
    }

    static void tooltipStylesAndCosts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_riptide_tooltip");
            var stack = equip(player, 1);
            stack.enchant(Enchantments.CHANNELING, 1);
            try (var ignored = config(73, Integer.MAX_VALUE)) {
                var lines = new ArrayList<Component>();
                stack.getItem().appendHoverText(stack, helper.getLevel(), lines, TooltipFlag.NORMAL);
                helper.assertTrue(lines.size() == 3, "Riptide must show only three item-description lines, even with Channeling");
                for (int i = 0; i < 3; i++) {
                    helper.assertTrue(lines.get(i).getContents() instanceof TranslatableContents,
                            "Riptide tooltip must use translated components");
                    var content = (TranslatableContents) lines.get(i).getContents();
                    helper.assertTrue(content.getKey().equals("item.apprenticecodex.charged_twin_blade_staff.desc.reptide_" + (i + 1)),
                            "Riptide tooltip must use the new keys");
                    helper.assertTrue(Objects.equals(lines.get(i).getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.GRAY)),
                            "Riptide description must be gray");
                    if (i < 2) {
                        var number = (Component) content.getArgs()[0];
                        helper.assertTrue(Objects.equals(number.getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.AQUA)),
                                "Only Riptide mana numbers must be aqua");
                        helper.assertTrue(number.getString().equals(i == 0 ? "73" : "4294967294"),
                                "Riptide tooltip must show configured initial and per-second upkeep costs without overflow");
                    }
                }
            }
        });
    }

    private static ApprenticeCodexServerConfig.GameTestConfigOverride config(int initial, int sustain) {
        return ApprenticeCodexServerConfig.useChargedTwinBladeStaffConfigOverrideForGameTest(
                new ChargedTwinBladeStaffServerConfig.Values(initial, sustain));
    }

    private static ItemStack equip(Player player, int level) {
        // MagicData#setManaは最大マナでclampするため、複数回の支払いを検証できる容量を明示する。
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA.get())).setBaseValue(500);
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        stack.enchant(Enchantments.RIPTIDE, level);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    private static void chargeAndRelease(Player player, ItemStack stack) {
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        ((LivingEntityAccessor) player).apprenticecodex$setUseItemRemaining(stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS);
        player.releaseUsingItem();
    }
}
