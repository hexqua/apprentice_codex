package jp.aquafactory.apprenticecodex.gametest.spell.archermultiple;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipGreaterConjurersTalisman;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareSummonedEntityIsolationArea;

public final class ArcherMultipleScenarios {
    private ArcherMultipleScenarios() {}

    static final UUID ARCHER_MULTIPLE_SUMMON_DAMAGE_TEST_MODIFIER_ID =
            UUID.fromString("0ba0d6e2-80a3-4d08-b2e8-8892c3cf6834");
    private static final List<Entity> FIXTURE_ENTITIES = new ArrayList<>();

    static void archerMultipleInitialCastUsesFixedDurationAndSerializableLifecycle(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            var player = createFixturePlayer(helper, "archer_multiple_fixed_duration_test");
            castArcherMultiple(helper, player, 5);

            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var recast = MagicData.getPlayerMagicData(player).getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "Archer Multiple should create a recast instance on initial cast");
            helper.assertTrue(recast.getTicksToLive() == 600,
                    "Archer Multiple duration should remain fixed at 600 ticks for every spell level");
            requireFourBows(helper, player);
            helper.assertTrue(recast.getCastData() instanceof ArcherMultiple.ArcherMultipleCastData,
                    "Archer Multiple should use its own lifecycle cast data");
            var castData = (ArcherMultiple.ArcherMultipleCastData) recast.getCastData();
            helper.assertTrue(castData.getBowUuids().size() == 4,
                    "Archer Multiple cast data should track every summoned bow");
            helper.assertTrue(helper.getLevel().dimension().location().equals(castData.getDimension()),
                    "Archer Multiple cast data should track the summon dimension");

            var nbtRestored = new ArcherMultiple.ArcherMultipleCastData();
            nbtRestored.deserializeNBT(castData.serializeNBT());
            assertArcherMultipleCastDataEquals(helper, castData, nbtRestored, "NBT");

            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                castData.writeToBuffer(buffer);
                var bufferRestored = new ArcherMultiple.ArcherMultipleCastData();
                bufferRestored.readFromBuffer(buffer);
                assertArcherMultipleCastDataEquals(helper, castData, bufferRestored, "network buffer");
            } finally {
                buffer.release();
            }
            helper.succeed();
        });
    }

    static void archerMultipleTimeoutWithGreaterConjurersTalismanKeepsCooldown(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createFixturePlayer(helper, "archer_multiple_greater_conjurer_timeout_test");
            equipGreaterConjurersTalisman(player);
            castArcherMultiple(helper, player, 1);
            var bows = requireFourBows(helper, player);

            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "Archer Multiple should create a recast instance on initial cast");
            magicData.getPlayerRecasts().removeRecast(recast,
                    RecastResult.TIMEOUT);

            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple timeout should remove the active recast");
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).isEmpty(),
                    "Archer Multiple timeout should remove every summoned bow");
            helper.assertTrue(bows.stream().allMatch(Entity::isRemoved),
                    "Timeout should discard the original four bows");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Greater Conjurer's Talisman must not suppress Archer Multiple cooldown");
            helper.succeed();
        });
    }

    static void archerMultipleManualRecastRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            var player = createFixturePlayer(helper, "archer_multiple_manual_recast_test");
            castArcherMultiple(helper, player, 1);
            var bows = requireFourBows(helper, player);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);

            spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);

            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple manual recast should consume the active recast");
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).isEmpty(),
                    "Archer Multiple manual recast should remove every summoned bow");
            helper.assertTrue(bows.stream().allMatch(Entity::isRemoved),
                    "Manual recast should discard the original four bows");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Archer Multiple manual recast should start its normal cooldown");
            helper.succeed();
        });
    }

    static void archerMultipleCounterspellRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            var player = createFixturePlayer(helper, "archer_multiple_counterspell_test");
            castArcherMultiple(helper, player, 1);
            var bows = requireFourBows(helper, player);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "Archer Multiple counterspell test requires an active recast");

            // Iron'sのSummonManagerはCOUNTERSPELL時にrecastを復元するが、Archer Multipleは
            // 小型で個別解除が困難な4本を一括解除できる対抗手段として、意図的に保護対象外とする。
            magicData.getPlayerRecasts().removeRecast(recast,
                    RecastResult.COUNTERSPELL);

            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple should intentionally consume its recast instead of restoring it like SummonManager summons");
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).isEmpty(),
                    "Counterspell should intentionally remove every Archer Multiple bow");
            helper.assertTrue(bows.stream().allMatch(Entity::isRemoved),
                    "Counterspell should discard the original four bows");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Counterspell should start Archer Multiple's normal cooldown");
            helper.succeed();
        });
    }

    static void archerMultipleLongTeleportKeepsFollowing(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            var player = createFixturePlayer(helper, "archer_multiple_teleport_test");
            castArcherMultiple(helper, player, 1);
            var bows = requireFourBows(helper, player);

            player.setPos(player.getX() + 96.0D, player.getY(), player.getZ());
            bows.forEach(bow -> bow.tickOnServer(helper.getLevel()));

            helper.assertTrue(bows.stream().allMatch(bow -> !bow.isRemoved() && bow.distanceToSqr(player) < 64.0D),
                    "Archer Multiple bows should immediately resume following after a long same-dimension teleport");
            helper.assertTrue(MagicData.getPlayerMagicData(player).getPlayerRecasts()
                            .hasRecastForSpell(SpellRegistry.ARCHER_MULTIPLE.get()),
                    "A same-dimension teleport should not consume the Archer Multiple recast");
            helper.succeed();
        });
    }

    static void archerMultipleDamageUsesSummonDamageAttribute(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> verifySummonDamage(helper));
    }

    private static void verifySummonDamage(GameTestHelper helper) {
        var player = createFixturePlayer(helper, "archer_multiple_summon_damage_test");
        var baseTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 12, 0));
        FIXTURE_ENTITIES.add(baseTarget);
        baseTarget.setNoAi(true);
        var baseBow = createArcherMultipleDamageTestBow(helper, player, 4.0F);
        var baseHealth = baseTarget.getHealth();
        fireArcherMultipleForGameTest(baseBow, baseTarget, helper.getLevel());
        var baseDamage = baseHealth - baseTarget.getHealth();
        baseTarget.discard();

        player.getAttribute(AttributeRegistry.SUMMON_DAMAGE.get()).addTransientModifier(new AttributeModifier(
                ARCHER_MULTIPLE_SUMMON_DAMAGE_TEST_MODIFIER_ID,
                "Archer Multiple Summon Damage test",
                0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
        var boostedTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 12, 0));
        FIXTURE_ENTITIES.add(boostedTarget);
        boostedTarget.setNoAi(true);
        var boostedBow = createArcherMultipleDamageTestBow(helper, player, 4.0F);
        var boostedHealth = boostedTarget.getHealth();
        fireArcherMultipleForGameTest(boostedBow, boostedTarget, helper.getLevel());
        var boostedDamage = boostedHealth - boostedTarget.getHealth();

        helper.assertTrue(baseDamage > 0.0F, "Archer Multiple should damage the baseline target");
        helper.assertTrue(boostedDamage > baseDamage + 0.1F,
                "Archer Multiple should retain Summon Damage scaling: " + boostedDamage + " <= " + baseDamage);
        helper.succeed();
    }

    static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        whenArcherMultipleChunksReady(helper, () -> {
            prepareSummonedEntityIsolationArea(helper, new BlockPos(0, 12, 0));
            var player = createFixturePlayer(helper, "archer_multiple_all_bows_removed_test");
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);

            castArcherMultiple(helper, player, 1);
            var bows = requireFourBows(helper, player);
            bows.forEach(bow -> bow.remove(Entity.RemovalReason.DISCARDED));
            // 操作は一度だけ実行し、待機中は同じ生成物の終了結果だけを観測する。
            helper.succeedWhen(() -> {
                helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                        "Archer Multiple recast should end once every summoned bow has disappeared");
                helper.assertTrue(bows.stream().allMatch(Entity::isRemoved)
                                && getOwnedArcherMultipleBows(helper, player).isEmpty(),
                        "Archer Multiple bows should all be gone after the forced removal");
                helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                        "Archer Multiple should start its normal cooldown when every summoned bow disappears");
            });
        });
    }

    private static void whenArcherMultipleChunksReady(GameTestHelper helper, Runnable action) {
        // ownerと周囲の弓を登録するchunkだけを待ち、tick 0での公開を前提にしない。
        // structure配置で読込済みの領域内なので、追加の強制読込ticketは作らない。
        GameTestFixtureSupport.whenEntityChunksReady(helper,
                new BlockPos(-3, 0, -3), new BlockPos(3, 0, 3), action);
    }

    private static FakePlayer createFixturePlayer(GameTestHelper helper, String profileName) {
        var player = createArcherMultiplePlayer(helper, new BlockPos(0, 12, 0), profileName);
        FIXTURE_ENTITIES.add(player);
        return player;
    }

    private static List<ArcherMultipleBowEntity> requireFourBows(GameTestHelper helper, FakePlayer player) {
        var bows = getOwnedArcherMultipleBows(helper, player);
        FIXTURE_ENTITIES.addAll(bows);
        helper.assertTrue(bows.size() == 4,
                "Archer Multiple must expose four owned bows before verification: actual=" + bows.size()
                        + ", tick=" + helper.getTick() + ", owner=" + player.position());
        return bows;
    }

    static void cleanupFixtures() {
        // 全体timeoutやassert失敗でもAfterBatchは呼ばれる。結果の判定が終わるまで生成物を保持する。
        try {
            for (var entity : FIXTURE_ENTITIES) {
                if (entity instanceof FakePlayer player && !player.isRemoved()) {
                    ArcherMultipleManager.cancel(player, RecastResult.COUNTERSPELL);
                }
            }
        } finally {
            FIXTURE_ENTITIES.forEach(Entity::discard);
            FIXTURE_ENTITIES.clear();
        }
    }

    static void assertArcherMultipleCastDataEquals(
            GameTestHelper helper,
            ArcherMultiple.ArcherMultipleCastData expected,
            ArcherMultiple.ArcherMultipleCastData actual,
            String transport
    ) {
        helper.assertTrue(expected.getBowUuids().equals(actual.getBowUuids()),
                "Archer Multiple " + transport + " should preserve bow UUIDs");
        helper.assertTrue(Objects.equals(expected.getDimension(), actual.getDimension()),
                "Archer Multiple " + transport + " should preserve the summon dimension");
    }

    static ArcherMultipleBowEntity createArcherMultipleDamageTestBow(
            GameTestHelper helper,
            FakePlayer owner,
            float damage
    ) {
        var bow = new ArcherMultipleBowEntity(
                EntityRegistry.ARCHER_MULTIPLE_BOW.get(),
                helper.getLevel(),
                owner,
                0,
                1
        );
        bow.setDamage(damage);
        return bow;
    }

    static void fireArcherMultipleForGameTest(
            ArcherMultipleBowEntity bow,
            Entity target,
            Level level
    ) {
        try {
            var method = ArcherMultipleBowEntity.class.getDeclaredMethod("fire", Entity.class, Level.class);
            method.setAccessible(true);
            method.invoke(bow, target, level);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Archer Multiple fire for GameTest", exception);
        }
    }

    public static FakePlayer createArcherMultiplePlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // 独自lifecycleがownerとrecastの整合性をlevel上でも検証するため、
        // Archer Multipleの消滅・追従テストではFakePlayerもワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static void castArcherMultiple(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static List<ArcherMultipleBowEntity> getOwnedArcherMultipleBows(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                ArcherMultipleBowEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                bow -> {
                    var summonOwner = bow.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }
}
