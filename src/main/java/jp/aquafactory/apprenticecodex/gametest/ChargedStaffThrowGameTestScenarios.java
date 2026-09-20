package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargedTwinBladeStaffServerConfig;
import jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientConfigState;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.network.packet.SyncChargedTwinBladeStaffConfigPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

final class ChargedStaffThrowGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ChargedStaffThrowGameTestScenarios() {
    }

    static void configuredCostsAndBoundaries(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_throw_config");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var data = MagicData.getPlayerMagicData(player);
            var area = player.getBoundingBox().inflate(8);
            // 同期コール内だけ設定を上書きし、同じバッチの他テストへ漏らさない。
            for (int cost : new int[]{73, 0}) {
                try (var ignored = config(cost)) {
                    data.setMana(cost);
                    stack.getItem().releaseUsing(stack, player.level(), player,
                            stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS);
                    helper.assertTrue(data.getMana() == 0, "Throw must consume the configured exact balance");
                    var projectiles = helper.getLevel().getEntitiesOfClass(ChargedTwinBladeStaffThrownEntity.class, area,
                            entity -> entity.getOwner() == player);
                    helper.assertTrue(projectiles.size() == 1, "Configured throw must spawn one projectile, including zero cost");
                    projectiles.forEach(ChargedTwinBladeStaffThrownEntity::discard);
                }
            }
            try (var ignored = config(73)) {
                data.setMana(72);
                stack.getItem().releaseUsing(stack, player.level(), player,
                        stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS);
                helper.assertTrue(data.getMana() == 72 && helper.getLevel().getEntitiesOfClass(
                                ChargedTwinBladeStaffThrownEntity.class, area, entity -> entity.getOwner() == player).isEmpty(),
                        "Insufficient mana must not consume mana or spawn a projectile");
                stack.enchant(Enchantments.LOYALTY, 2);
                stack.getItem().releaseUsing(stack, player.level(), player,
                        stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS);
                helper.assertTrue(Math.abs(data.getMana() - (72 - 73 / 3.0F)) < 0.001,
                        "Loyalty must divide the configured cost without rounding actual consumption");
                helper.getLevel().getEntitiesOfClass(ChargedTwinBladeStaffThrownEntity.class, area,
                        entity -> entity.getOwner() == player).forEach(ChargedTwinBladeStaffThrownEntity::discard);
                player.getAbilities().instabuild = true;
                data.setMana(0);
                stack.getItem().releaseUsing(stack, player.level(), player,
                        stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS);
                helper.assertTrue(data.getMana() == 0 && helper.getLevel().getEntitiesOfClass(
                                ChargedTwinBladeStaffThrownEntity.class, area, entity -> entity.getOwner() == player).size() == 1,
                        "Creative throws must work without mana");
            }
        });
    }

    static void configSyncAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var values = new ChargedTwinBladeStaffServerConfig.Values(7, 3, 73);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            var previous = ChargedTwinBladeStaffClientConfigState.values();
            try {
                SyncChargedTwinBladeStaffConfigPacket.encode(new SyncChargedTwinBladeStaffConfigPacket(values), buffer);
                var decoded = SyncChargedTwinBladeStaffConfigPacket.decode(buffer);
                helper.assertTrue(decoded.values().equals(values) && !buffer.isReadable(),
                        "Staff config packet must round-trip all costs");
                ChargedTwinBladeStaffClientConfigState.set(decoded.values());
                helper.assertTrue(ChargedTwinBladeStaffClientConfigState.values().throwManaCost() == 73,
                        "Client config must retain the synchronized throw cost");
                ChargedTwinBladeStaffClientConfigState.reset();
                helper.assertTrue(ChargedTwinBladeStaffClientConfigState.values().equals(ChargedTwinBladeStaffServerConfig.Values.DEFAULT),
                        "Disconnect reset must restore all default staff costs");
            } finally {
                ChargedTwinBladeStaffClientConfigState.set(previous);
                buffer.release();
            }
            try (var ignored = config(73)) {
                var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
                stack.enchant(Enchantments.LOYALTY, 2);
                var lines = new ArrayList<Component>();
                stack.getItem().appendHoverText(stack, helper.getLevel(), lines, TooltipFlag.NORMAL);
                var line = lines.get(0);
                helper.assertTrue(Objects.equals(line.getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.GRAY)),
                        "Throw description must be gray");
                var content = (TranslatableContents) line.getContents();
                helper.assertTrue(content.getKey().equals("item.apprenticecodex.charged_twin_blade_staff.desc.throwable"),
                        "Throw description must retain its translation key");
                var number = (Component) content.getArgs()[0];
                helper.assertTrue(number.getString().equals("25") && Objects.equals(number.getStyle().getColor(),
                                TextColor.fromLegacyFormat(ChatFormatting.AQUA)),
                        "Only the rounded-up configured mana number must be aqua");
            }
        });
    }

    static void channelingDoesNotSummonLightning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_no_lightning");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            stack.enchant(Enchantments.CHANNELING, 1);
            // 天候は同期的に復元し、自然落雷や別テストの天候依存と競合させない。
            float rain = level.getRainLevel(1);
            float thunder = level.getThunderLevel(1);
            var column = helper.absolutePos(new BlockPos(1, 0, 1));
            var position = new Vec3(column.getX() + 0.5D,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 2.0D,
                    column.getZ() + 0.5D);
            var area = new AABB(BlockPos.containing(position)).inflate(8);
            var target = Objects.requireNonNull(EntityType.COW.create(level));
            target.setPos(position);
            level.addFreshEntity(target);
            try {
                level.setRainLevel(1);
                level.setThunderLevel(1);
                helper.assertTrue(level.isThundering() && level.canSeeSky(BlockPos.containing(position)),
                        "Lightning regression requires thunder and an exposed impact position: thunder="
                                + level.isThundering() + ", sky=" + level.canSeeSky(BlockPos.containing(position))
                                + ", y=" + position.y);
                int initialBolts = level.getEntitiesOfClass(LightningBolt.class, area).size();
                for (boolean entityImpact : new boolean[]{false, true}) {
                    var projectile = new ChargedTwinBladeStaffThrownEntity(EntityRegistry.CHARGED_TWIN_BLADE_STAFF_THROWN.get(),
                            level, player, stack, ChargedTwinBladeStaffSpellPayload.EMPTY);
                    projectile.setPos(position);
                    level.addFreshEntity(projectile);
                    try {
                        // 実際の着弾経路を通し、entity側のエンチャント後処理も検証する。
                        var method = ChargedTwinBladeStaffThrownEntity.class.getDeclaredMethod(
                                entityImpact ? "onHitEntity" : "onHitBlock",
                                entityImpact ? EntityHitResult.class : BlockHitResult.class);
                        method.setAccessible(true);
                        method.invoke(projectile, entityImpact ? new EntityHitResult(target)
                                : new BlockHitResult(position, Direction.UP, BlockPos.containing(position), false));
                        helper.assertTrue(projectile.isImpacted(), "Staff must finish both block and entity impacts");
                        helper.assertTrue(level.getEntitiesOfClass(LightningBolt.class, area).size() == initialBolts,
                                "Channeling staff impacts must not summon lightning during thunder");
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException("Could not invoke staff impact", exception);
                    } finally {
                        projectile.discard();
                    }
                }
            } finally {
                target.discard();
                level.setRainLevel(rain);
                level.setThunderLevel(thunder);
            }
        });
    }

    static void entityImpactKeepsHitPositionWithoutFollowing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "staff_impact_position");
            var origin = helper.absoluteVec(new Vec3(1, 100, 1));
            // 実際の飛翔判定を通し、中央への命中とかすめる命中の両方で座標を確認する。
            for (int scenario = 0; scenario < 4; ++scenario) {
                var target = Objects.requireNonNull(EntityType.COW.create(level));
                target.setPos(origin);
                target.setNoAi(true);
                target.setNoGravity(true);
                level.addFreshEntity(target);
                var projectile = new ChargedTwinBladeStaffThrownEntity(EntityRegistry.CHARGED_TWIN_BLADE_STAFF_THROWN.get(),
                        level, player, new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                        ChargedTwinBladeStaffSpellPayload.EMPTY);
                double xOffset = scenario == 1 ? target.getBbWidth() / 2.0D + 0.15D : 0;
                var start = origin.add(xOffset, 0.8D, -2);
                projectile.setPos(start);
                projectile.shoot(0, 0, 1, 3, 0);
                level.addFreshEntity(projectile);
                var originalHitPosition = new Vec3(start.x, start.y, target.getBoundingBox().minZ - (double) 0.3F);
                boolean moveTargetAroundStart = scenario == 2;
                boolean moveTargetAlongPath = scenario == 3;
                // イベント中に交点が消える場合と別の交点ができる場合の両方で、元の交点を維持する。
                Consumer<ProjectileImpactEvent> impactListener = event -> {
                    if (event.getProjectile() != projectile) {
                        return;
                    }
                    helper.assertTrue(event.getRayTraceResult().getLocation().distanceToSqr(originalHitPosition) < 1.0E-8D,
                            "Impact event must receive the original flight intersection");
                    if (moveTargetAroundStart) {
                        target.setPos(origin.add(0, 0, -2));
                        var bounds = target.getBoundingBox().inflate(0.3F);
                        helper.assertTrue(bounds.contains(start)
                                        && bounds.clip(start, start.add(projectile.getDeltaMovement())).isEmpty(),
                                "Regression setup must move the target around the staff with no entry intersection");
                    } else if (moveTargetAlongPath) {
                        target.setPos(origin.add(0, 0, 0.5D));
                        var movedIntersection = target.getBoundingBox().inflate(0.3F)
                                .clip(start, start.add(projectile.getDeltaMovement()));
                        helper.assertTrue(movedIntersection.isPresent()
                                        && movedIntersection.get().distanceToSqr(originalHitPosition) > 0.1D,
                                "Regression setup must produce a different intersection after the target moves");
                    }
                };
                MinecraftForge.EVENT_BUS.addListener(impactListener);
                try {
                    projectile.tick();
                    helper.assertTrue(projectile.isImpacted(), "Staff must hit the target through the flight collision path");
                    var expected = originalHitPosition.add(0, 0, -0.01D);
                    helper.assertTrue(projectile.position().distanceToSqr(expected) < 1.0E-8D,
                            "Staff must remain at the flight intersection instead of the target's feet: " + projectile.position());
                    var fixedPosition = projectile.position();
                    target.setPos(origin.add(2, 1, 2));
                    target.setYRot(90);
                    target.yBodyRot = 90;
                    for (int tick = 0; tick < 3; ++tick) {
                        projectile.tick();
                    }
                    helper.assertTrue(projectile.position().equals(fixedPosition) && projectile.getDeltaMovement().equals(Vec3.ZERO),
                            "Impacted staff must stay at its world position after the target moves and turns");
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(impactListener);
                    projectile.discard();
                    target.discard();
                }
            }
        });
    }

    private static ApprenticeCodexServerConfig.GameTestConfigOverride config(int throwCost) {
        return ApprenticeCodexServerConfig.useChargedTwinBladeStaffConfigOverrideForGameTest(
                new ChargedTwinBladeStaffServerConfig.Values(50, 20, throwCost));
    }
}
