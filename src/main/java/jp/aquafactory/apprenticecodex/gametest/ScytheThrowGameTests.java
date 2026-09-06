package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.*;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityDamageMemoryAccessor;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheThrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void thrownSchoolColorSurvivesItemChangesAndRecallPacket(GameTestHelper h) {
        var p = player(h, 200);
        var stack = p.getMainHandItem();
        var plain = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        plain.prepare(p, stack, 0, 2.5);
        h.assertTrue(plain.getTrailColor() == ScytheThrowEntity.DEFAULT_TRAIL_COLOR, "Unimbued scythe must use the fallback color");
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBOLT_SPELL.get();
        var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(1, true, false).mutableCopy();
        h.assertTrue(container.addSpell(spell, 1, true), "Test spell must fit the scythe");
        io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(stack, container.toImmutable());
        var thrown = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        thrown.prepare(p, stack, 0, 2.5);
        int expected = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolTintColor(spell.getSchoolType());
        io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(stack,
                io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(1, true, false));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(thrown.getTrailColor() == expected, "Thrown color must retain the launch spell after the original item changes");
        var packet = new jp.aquafactory.apprenticecodex.network.packet.ScytheRecallEffectPacket(Vec3.ZERO, new Vec3(3, 4, 5), thrown.getTrailColor());
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var codec = jp.aquafactory.apprenticecodex.network.packet.ScytheRecallEffectPacket.STREAM_CODEC;
            codec.encode(buffer, packet);
            h.assertTrue(codec.decode(buffer).equals(packet), "Recall packet must preserve endpoints and school color");
        } finally { buffer.release(); }
        h.succeed();
    }

    static FakePlayer player(GameTestHelper h, float mana) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "scythe_throw"));
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        p.setYRot(0); p.setXRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
        p.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(10000);
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        MagicData.getPlayerMagicData(p).setMana(mana);
        return p;
    }

    private static void begin(GameTestHelper h, FakePlayer p) {
        ScytheThrowManager.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void longChargeClampsRangeAndDistanceBoundaryRecalls(GameTestHelper h) {
        var p = player(h, 200);
        // テンプレートの境界バリアより上で射程を検証する。
        p.setPos(h.absoluteVec(new Vec3(2.5, 12, 2.5)));
        p.setXRot(-90);
        var origin = p.getEyePosition();
        begin(h, p);
        h.runAfterDelay(41, () -> {
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null, "Long charge must launch");
            for (int i = 0; i < 20; i++) entity.tick();
            h.assertTrue(entity.isHovering() && Math.abs(entity.position().distanceTo(origin) - 10) < 0.001,
                    "Charge above forty ticks must stop at ten blocks in twenty ticks: distance=" + entity.position().distanceTo(origin)
                            + ", hovering=" + entity.isHovering());
            p.setPos(entity.position().add(24, 0, 0));
            entity.checkOwner();
            h.assertTrue(entity.isRemoved(), "Exactly twenty-four blocks must recall");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void freeThrowHoverAndDeathClearTransientState(GameTestHelper h) {
        var p = player(h, 0);
        begin(h, p);
        h.runAfterDelay(10, () -> {
            try (var ignored = jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig.useSpellReaperScytheConfigOverrideForGameTest(
                    new jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig.Values(200, 40, 10, 0, 0))) {
                ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
                var entity = ScytheThrowManager.active(p);
                h.assertTrue(entity != null, "Zero-cost throw must work without mana");
                for (int i = 0; i < 10; i++) entity.tick();
                h.assertTrue(!entity.isRemoved() && MagicData.getPlayerMagicData(p).getMana() == 0, "Zero upkeep must allow hovering");
                p.setHealth(0);
                entity.checkOwner();
                h.assertTrue(entity.isRemoved() && !ScytheThrowManager.isThrown(p.getMainHandItem()), "Death must clear the original stack");
            }
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void wallBlocksOutwardHitButAllowsReturnAndContactIsConsumed(GameTestHelper h) {
        var p = player(h, 200);
        p.setPos(h.absoluteVec(new Vec3(2.5, 12, 2.5)));
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 12, 5));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        // 横幅の端が薄い壁へ入り込んでも、往路の遮蔽判定で反対側を攻撃しない。
        h.setBlock(new BlockPos(2, 12, 4), Blocks.STONE);
        h.setBlock(new BlockPos(2, 13, 4), Blocks.STONE);
        begin(h, p);
        h.runAfterDelay(10, () -> {
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null, "Wall-directed throw must launch");
            for (int i = 0; i < 5; i++) entity.tick();
            h.assertTrue(entity.isHovering() && target.getHealth() == target.getMaxHealth(), "Outward blade must not damage through a wall: health="
                    + target.getHealth() + ", hovering=" + entity.isHovering());
            // 帰還先を壁の反対側へ移し、同じ壁を貫通する帰還攻撃を確認する。
            p.setPos(h.absoluteVec(new Vec3(2.5, 12, 7.5)));
            entity.recall();
            h.assertTrue(target.getHealth() < target.getMaxHealth(), "Return must damage through the wall");
            float after = target.getHealth();
            entity.recall();
            h.assertTrue(target.getHealth() == after, "Repeated recall must not repeat damage");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void chargeNineTicksAndOffhandDoNotSpendMana(GameTestHelper h) {
        var p = player(h, 100);
        p.setItemInHand(InteractionHand.OFF_HAND, p.getMainHandItem().copy());
        // Better Combatは両手武器装備時にgetOffhandItemを空へ置換するため、対象アイテムを直接指定する。
        h.assertTrue(ScytheThrowManager.use(h.getLevel(), p, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
                "Offhand throw must be rejected");
        begin(h, p);
        h.runAfterDelay(9, () -> {
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            h.assertTrue(ScytheThrowManager.active(p) == null, "Nine ticks must not throw");
            h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == 100, "Short charge must not spend mana");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tenTicksChargesOnceAndHandSwapRecalls(GameTestHelper h) {
        var p = player(h, 100);
        var stack = p.getMainHandItem();
        begin(h, p);
        h.runAfterDelay(10, () -> {
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null, "Ten ticks with exact mana must throw");
            h.assertTrue(Math.abs(entity.getY() - (p.getEyeY() - 0.5)) < 0.001,
                    "Throw must start below the eyes at hand height");
            h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == 0, "Throw must spend exactly 100 mana");
            h.assertTrue(ScytheThrowManager.isThrown(stack), "Thrown stack must be marked");
            final double[] speed = {0};
            stack.forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> {
                if (a.equals(Attributes.ATTACK_SPEED)) speed[0] += m.amount();
            });
            h.assertTrue(speed[0] == 0, "Thrown weapon must not retain its speed penalty");
            ScytheThrowManager.release(h.getLevel(), p, stack);
            h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == 0, "Duplicate release must not charge again");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.OFF_HAND, stack);
            entity.checkOwner();
            h.assertTrue(entity.isRemoved() && !ScytheThrowManager.isThrown(stack), "Hand swap must recall and restore the stack");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void manaLostWhileChargingRejectsThrow(GameTestHelper h) {
        var p = player(h, 100);
        begin(h, p);
        h.runAfterDelay(10, () -> {
            MagicData.getPlayerMagicData(p).setMana(99);
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            h.assertTrue(ScytheThrowManager.active(p) == null && MagicData.getPlayerMagicData(p).getMana() == 99,
                    "Server must reject a throw after mana is lost during charge");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void hoveringSpendsManaAndStopsAtInsufficientBalance(GameTestHelper h) {
        var p = player(h, 109);
        begin(h, p);
        h.runAfterDelay(10, () -> {
            ScytheThrowManager.release(h.getLevel(), p, p.getUseItem()); p.stopUsingItem();
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null, "Throw must start");
            for (int i = 0; i < 5; i++) entity.tick();
            h.assertTrue(entity.isHovering() && MagicData.getPlayerMagicData(p).getMana() == 9, "Flight must not charge upkeep");
            for (int i = 0; i < 3; i++) entity.tick();
            h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == 0 && !entity.isRemoved(), "Exact upkeep balance permits its tick");
            entity.tick();
            h.assertTrue(entity.isRemoved() && !ScytheThrowManager.isThrown(p.getMainHandItem()), "Insufficient upkeep recalls");
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void sweepUsesThinHorizontalShapeAndRejectsDiagonalCorners(GameTestHelper h) {
        h.assertTrue(ScytheThrowEntity.intersectsSweep(new AABB(4, -0.02, 4, 5, 1, 5), Vec3.ZERO, new Vec3(10, 0, 10)),
                "Return sweep must hit targets along the diagonal");
        h.assertFalse(ScytheThrowEntity.intersectsSweep(new AABB(0, 0, 8, 1, 1, 9), Vec3.ZERO, new Vec3(10, 0, 10)),
                "Broad-phase corner must not be hit");
        h.assertFalse(ScytheThrowEntity.intersectsSweep(new AABB(4, 0.1, 4, 5, 1, 5), Vec3.ZERO, new Vec3(10, 0, 10)),
                "Targets above the thin horizontal blade must be missed");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unscaledDamageIgnoresIronsAttributesAndPreservesArmor(GameTestHelper h) {
        var p = player(h, 100);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(200);
        p.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(50);
        target.getAttribute(AttributeRegistry.SPELL_RESIST).setBaseValue(50);
        var source = h.getLevel().damageSources().thrown(new net.minecraft.world.entity.projectile.Snowball(h.getLevel(), p), p);
        CombatTools.applyUnscaledDamage(target, 10, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
        h.assertTrue(Math.abs(target.getHealth() - 190) < 0.01, "Unscaled damage must ignore Iron's power and resistance");
        target.invulnerableTime = 0;
        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
        CombatTools.applyUnscaledDamage(target, 10, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
        h.assertTrue(target.getHealth() > 180, "Unscaled damage must still respect armor");
        h.assertFalse(CombatTools.applyUnscaledDamage(p, 10, source, CombatTools.KnockbackTypes.DEFAULT), "Owner must be protected");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damagePairRespectsIframeAndContinuousRestoresMemory(GameTestHelper h) {
        var p = player(h, 100);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(200);
        var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        var memory = (LivingEntityDamageMemoryAccessor) target;
        target.invulnerableTime = 20;
        memory.apprenticecodex$setLastHurt(10);
        ScytheThrowDamage.hit(h.getLevel(), projectile, p, target, p.getMainHandItem(), 10, 2, false);
        h.assertTrue(target.getHealth() == 200, "Rejected physical hit must not produce magic damage");
        target.invulnerableTime = 0;
        ScytheThrowDamage.hit(h.getLevel(), projectile, p, target, p.getMainHandItem(), 10, 2, false);
        h.assertTrue(Math.abs(target.getHealth() - 188) < 0.01, "Physical and magic must each hit once");
        h.assertTrue(memory.apprenticecodex$getLastHurt() == 10, "Magic must preserve physical damage memory");
        int iframe = target.invulnerableTime;
        var before = target.getHealth();
        ScytheThrowDamage.hit(h.getLevel(), projectile, p, target, p.getMainHandItem(), 10, 2, true);
        h.assertTrue(Math.abs(before - target.getHealth() - 1.2f) < 0.01, "Continuous hit must scale both components to ten percent");
        h.assertTrue(target.invulnerableTime == iframe && memory.apprenticecodex$getLastHurt() == 10,
                "Continuous damage must preserve normal iframe memory");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sharpnessAffectsDamageWithoutFireAspectOrDurability(GameTestHelper h) {
        var p = player(h, 100);
        var stack = p.getMainHandItem();
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.SHARPNESS), 2);
        stack.enchant(registry.getOrThrow(Enchantments.FIRE_ASPECT), 2);
        var target = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), h.getLevel());
        float before = target.getHealth();
        ScytheThrowDamage.hit(h.getLevel(), projectile, p, target, stack, 10, 0, false);
        h.assertTrue(before - target.getHealth() > 10, "Sharpness must increase thrown physical damage");
        h.assertFalse(target.isOnFire(), "Throw must not apply Fire Aspect");
        h.assertTrue(stack.getDamageValue() == 0, "Throw must not consume durability");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hauntedContributesOnlyWhenMalumIsAvailable(GameTestHelper h) {
        var p = player(h, 100);
        if (MalumSpellReaperScytheBridge.isAvailable()) {
            var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            p.getMainHandItem().enchant(registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("malum", "haunted"))), 2);
            // FakePlayer.tick()は空なので、実スタックから得た装備補正を明示的に適用する。
            p.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                var instance = p.getAttribute(attribute);
                if (instance != null) instance.addOrUpdateTransientModifier(modifier);
            });
            h.assertTrue(MalumSpellReaperScytheBridge.throwMagicDamage(p) >= 2, "Haunted must contribute to the captured magic attribute");
        } else {
            h.assertTrue(MalumSpellReaperScytheBridge.throwMagicDamage(p) == 0, "No Malum must produce no magic component");
        }
        h.succeed();
    }
}
