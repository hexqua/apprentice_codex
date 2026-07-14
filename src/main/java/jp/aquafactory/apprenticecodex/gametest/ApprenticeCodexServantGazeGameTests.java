package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
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
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexServantGazeGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    // 周辺 Entity を扱う GameTest 同士の並列干渉を避け、ライフサイクル系と攻撃対象系も分離する。
    private static final String LIFECYCLE_ISOLATED_BATCH = "apprenticecodex.servant_gaze_lifecycle_isolated";
    private static final String TARGETING_ISOLATED_BATCH = "apprenticecodex.servant_gaze_targeting_isolated";

    private ApprenticeCodexServantGazeGameTests() {}

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTogglesSinglePersistentStaff(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_toggle");
        helper.assertTrue(ServantGazeManager.toggle(player, 1, 5.0F, 20.0, 15),
                "First Servant Gaze cast should activate the staff");

        helper.runAtTickTime(2, () -> {
            ServantGazeManager.ensureActive(player);
            var staffs = findStaffs(helper, player);
            helper.assertTrue(staffs.size() == 1,
                    "Servant Gaze should keep exactly one staff for its owner");
            helper.assertTrue(Math.abs(staffs.get(0).getY() - player.getY()) < 1.0e-4D,
                    "Servant Gaze staff base height should match its owner's Y position");

            helper.assertFalse(ServantGazeManager.toggle(player, 1, 5.0F, 20.0, 15),
                    "Second Servant Gaze cast should deactivate the staff");
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Deactivating Servant Gaze should remove its staff");
            helper.assertFalse(Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE).active,
                    "Servant Gaze state should be inactive after toggling off");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void counterspellDisablesServantGazeWithoutRespawn(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_counterspell");
        ServantGazeManager.activate(player, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(2, () -> {
            var staffs = findStaffs(helper, player);
            helper.assertTrue(staffs.size() == 1,
                    "Servant Gaze should expose exactly one staff to Counterspell");
            var staff = staffs.get(0);
            helper.assertTrue(staff instanceof AntiMagicSusceptible,
                    "Servant Gaze staff should be directly targetable by Counterspell");
            ((AntiMagicSusceptible) staff).onAntiMagic(MagicData.getPlayerMagicData(player));
        });
        helper.runAtTickTime(4, () -> {
            ServantGazeManager.ensureActive(player);
            helper.assertTrue(findStaffs(helper, player).isEmpty(),
                    "Counterspell should remove the Servant Gaze staff without immediate respawn");
            helper.assertFalse(Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE).active,
                    "Counterspell should disable the persistent Servant Gaze state");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = TARGETING_ISOLATED_BATCH, timeoutTicks = 100)
    public static void servantGazeConsumesManaPerHighestHealthTarget(GameTestHelper helper) {
        var player = createPlayer(helper, "servant_gaze_targeting");
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(20.0F);
        var low = createZombie(helper, new BlockPos(0, 2, 4), 5.0F);
        var middle = createZombie(helper, new BlockPos(3, 2, 1), 10.0F);
        var high = createZombie(helper, new BlockPos(4, 2, 3), 15.0F);
        ServantGazeManager.activate(player, 1, 2.0F, 4.0, 10);

        helper.runAfterDelay(55, () -> {
            helper.assertTrue(magicData.getMana() == 0.0F,
                    "Servant Gaze should consume one mana payment for each selected target");
            helper.assertTrue(low.getHealth() == 5.0F,
                    "Servant Gaze should leave the lower-health third target unselected when mana is insufficient");
            helper.assertTrue(middle.getHealth() < 10.0F && high.getHealth() < 15.0F,
                    "Servant Gaze should attack the two highest-current-health visible targets");
            helper.succeed();
        });
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        // 構造端では従者の杖が未追跡の隣 chunk へ出るため、召喚位置も構造内に収まる中央へ置く。
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        player.setPos(position.x, position.y, position.z);
        var manaRegen = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) manaRegen.setBaseValue(0.0D);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(GameTestHelper helper, BlockPos position, float health) {
        // 未選択対象の体力不変を検証するため、日光ダメージを受けないHuskを使う。
        var zombie = EntityType.HUSK.create(helper.getLevel());
        if (zombie == null) throw new IllegalStateException("Failed to create Servant Gaze test zombie");
        var absolute = helper.absoluteVec(Vec3.atBottomCenterOf(position));
        zombie.setPos(absolute.x, absolute.y, absolute.z);
        zombie.setNoAi(true);
        zombie.setHealth(health);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static java.util.List<ServantGazeStaffEntity> findStaffs(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel().getEntitiesOfClass(ServantGazeStaffEntity.class,
                new AABB(player.position(), player.position()).inflate(32.0),
                staff -> staff.getOwner() != null && player.getUUID().equals(staff.getOwner().getUUID()));
    }
}
