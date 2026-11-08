package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGaze;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeManager;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound.ApprenticeCodexServantGazeGameTests.equipGreaterConjurersTalisman;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.LIFECYCLE_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.activate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ServantGazeLifecycleGameTests {
    private ServantGazeLifecycleGameTests() {}

    static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRecastCreatesAndRemovesSingleStaff(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_recast");
        var magicData = MagicData.getPlayerMagicData(player);
        servantGaze().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(findStaffs(helper, player).size() == 1,
                    "First Servant Gaze cast should create exactly one staff");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "First Servant Gaze cast should create an active recast");

            servantGaze().castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze recast should remove its staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "Servant Gaze recast should finish the active recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Servant Gaze recast should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void directCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_direct_counterspell");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(2, () -> {
            var staffs = findStaffs(helper, player);
            helper.assertTrue(staffs.size() == 1,
                    "Servant Gaze should expose exactly one staff to Counterspell");
            staffs.get(0).onAntiMagic(magicData);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Direct Counterspell should remove the Servant Gaze staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                    "Direct Counterspell should remove the Servant Gaze recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Direct Counterspell should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void ownerCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_owner_counterspell");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 1, 5.0F, 20.0, 15);
        var recast = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before owner Counterspell");

        magicData.getPlayerRecasts().removeRecast(recast, RecastResult.COUNTERSPELL);

        helper.assertTrue(findStaffs(helper, player).isEmpty(),
                "Counterspelling the owner should remove the Servant Gaze staff");
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(servantGaze()),
                "Counterspelling the owner should remove the Servant Gaze recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                "Counterspelling the owner should start the normal cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTimeoutStartsCooldownWithoutTalisman(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_timeout");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze should remove its staff after the fixed duration");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Servant Gaze timeout should start cooldown without the talisman");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTalismanSkipsCooldownOnlyOnTimeout(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_talisman_timeout");
        var magicData = MagicData.getPlayerMagicData(player);
        equipGreaterConjurersTalisman(player);
        activate(helper, player, 1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Servant Gaze should still remove its staff when the talisman suppresses cooldown");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(servantGaze()),
                    "Greater Conjurer's Talisman should suppress cooldown only after Servant Gaze timeout");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRelocationRecreatesStaffWithoutResettingRecast(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_relocation");
        var magicData = MagicData.getPlayerMagicData(player);
        activate(helper, player, 200, 3, 9.0F, 12.0, 25);
        var recast = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before relocation");
        var oldUuid = ((ServantGaze.ServantGazeCastData) recast.getCastData()).getStaffUuid();
        var remaining = recast.getTicksRemaining();

        ServantGazeManager.relocate(player);

        var relocated = magicData.getPlayerRecasts().getRecastInstance(servantGaze().getSpellId());
        helper.assertTrue(relocated == recast, "Servant Gaze relocation should preserve the active recast");
        helper.assertTrue(relocated.getTicksRemaining() == remaining,
                "Servant Gaze relocation should preserve the remaining duration");
        var castData = (ServantGaze.ServantGazeCastData) relocated.getCastData();
        helper.assertTrue(!oldUuid.equals(castData.getStaffUuid()),
                "Servant Gaze relocation should bind the recast to a newly created staff");
        helper.assertTrue(findStaffs(helper, player).size() == 1,
                "Servant Gaze relocation should leave exactly one staff");
        helper.succeed();
    }

    static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        // 構造端では従者の杖が未追跡の隣 chunk へ出るため、召喚位置も構造内に収まる中央へ置く。
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        player.setPos(position.x, position.y, position.z);
        var manaRegen = player.getAttribute(AttributeRegistry.MANA_REGEN.get());
        if (manaRegen != null) manaRegen.setBaseValue(0.0D);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static Zombie createZombie(GameTestHelper helper, BlockPos position, float health) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) throw new IllegalStateException("Failed to create Servant Gaze test zombie");
        var absolute = helper.absoluteVec(Vec3.atBottomCenterOf(position));
        zombie.setPos(absolute.x, absolute.y, absolute.z);
        zombie.setNoAi(true);
        zombie.setHealth(health);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    static List<ServantGazeStaffEntity> findStaffs(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel().getEntitiesOfClass(ServantGazeStaffEntity.class,
                new AABB(player.position(), player.position()).inflate(32.0),
                staff -> staff.getOwner() != null && player.getUUID().equals(staff.getOwner().getUUID()));
    }

    static ServantGaze servantGaze() {
        return (ServantGaze) SpellRegistry.SERVANT_GAZE.get();
    }
}
