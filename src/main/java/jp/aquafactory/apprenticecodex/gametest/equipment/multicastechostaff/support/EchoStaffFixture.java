package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class EchoStaffFixture {
    public static final String TEMPLATE = "gametest/echo_staff";

    private EchoStaffFixture() {
    }

    public static FakePlayer manualPlayer(GameTestHelper helper, String name) {
        var scope = TargetingTestFixture.of(helper);
        var player = scope.track(createEquipmentTestPlayer(helper, new BlockPos(48, 6, 48), name));
        player.setNoGravity(true);
        scope.assertInside(player);
        // callback単位の詠唱検証では未登録に保ち、Level側の詠唱更新・マナ回復と手動更新を重ねない。
        scope.onClose(() -> MulticastEchoStaffCastHelper.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player)));
        scope.onClose(() -> MagicData.getPlayerMagicData(player).getPlayerRecasts().removeAll(RecastResult.COMMAND));
        return player;
    }

    public static FakePlayer registeredPlayer(GameTestHelper helper, String name) {
        return TargetingTestFixture.of(helper).add(manualPlayer(helper, name));
    }

    public static Zombie target(GameTestHelper helper, FakePlayer player, Vec3 offset) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(zombie != null, "Fixture zombie must be created");
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.setPersistenceRequired();
        zombie.moveTo(player.position().add(offset));
        // template内の登録・空間検索を前提検査し、周辺chunkの強制読込には頼らない。
        return TargetingTestFixture.of(helper).add(zombie);
    }

    public static void completeCast(GameTestHelper helper, FakePlayer player, ItemStack stack, AbstractSpell spell,
                                    int level, int amplifier, float mana) {
        TargetingTestFixture.of(helper).run(() -> completeMulticastEchoStaffCast(helper.getLevel(), player, stack, spell, level, amplifier, mana));
    }

    public static void beginCast(GameTestHelper helper, FakePlayer player, ItemStack stack, AbstractSpell spell,
                                 int level, int amplifier, float mana) {
        TargetingTestFixture.of(helper).run(() -> beginMulticastEchoStaffCast(helper.getLevel(), player, stack, spell, level, amplifier, mana));
    }

    public static void finishCast(GameTestHelper helper, FakePlayer player, AbstractSpell spell, int level) {
        TargetingTestFixture.of(helper).run(() -> finishStartedSpellCast(helper.getLevel(), player, spell, level));
    }

    public static void tickEcho(GameTestHelper helper, FakePlayer player) {
        // ownerを公開しない一時entityも、このplayerの同期callback中の生成物として捕捉する。
        TargetingTestFixture.of(helper).run(() -> MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player)));
    }
}
