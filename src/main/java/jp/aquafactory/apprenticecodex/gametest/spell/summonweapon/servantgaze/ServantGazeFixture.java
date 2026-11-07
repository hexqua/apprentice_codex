package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support.SummonWeaponTestSupport;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGaze;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeManager;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

final class ServantGazeFixture {
    static final String TEMPLATE = "gametest/servant_gaze";
    static final String LIFECYCLE_ISOLATED_BATCH = "apprenticecodex.servant_gaze_lifecycle_isolated";
    static final String TARGETING_ISOLATED_BATCH = "apprenticecodex.servant_gaze_targeting_isolated";
    final GameTestHelper helper;
    final TargetingTestFixture scope;
    final FakePlayer player;

    ServantGazeFixture(GameTestHelper helper, String name) {
        this.helper = helper;
        scope = TargetingTestFixture.of(helper);
        player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        // 半径20の探索と杖の目線オフセットを含めて領域内へ収める。
        player.setPos(helper.absoluteVec(new Vec3(32.5, 6, 32.5)));
        player.setNoGravity(true);
        var manaRegen = player.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) manaRegen.setBaseValue(0);
        SummonWeaponTestSupport.registerPlayer(helper, player);
    }

    Zombie zombie(BlockPos scenarioRelative, float health) {
        var target = EntityType.HUSK.create(helper.getLevel());
        if (target == null) throw new IllegalStateException("Failed to create Servant Gaze test target");
        target.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(scenarioRelative.offset(30, 4, 30))));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setHealth(health);
        return scope.add(target);
    }

    List<ServantGazeStaffEntity> staffs() {
        var staffs = scope.entities(ServantGazeStaffEntity.class).stream().filter(staff -> staff.getOwner() == player).toList();
        staffs.forEach(scope::assertVisible);
        return staffs;
    }

    void activate(int duration, int spellLevel,
                  float damage, double radius, int manaCost) {
        var magicData = MagicData.getPlayerMagicData(player);
        var castData = new ServantGaze.ServantGazeCastData(spellLevel, damage, radius, manaCost);
        var staff = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), helper.getLevel(), player);
        ServantGazeManager.initialize(player, staff, duration, castData);
        scope.add(staff);
        var recast = new RecastInstance(spell().getSpellId(), spellLevel, 2, duration,
                CastSource.SPELLBOOK, castData);
        magicData.getPlayerRecasts().addRecast(recast, magicData);
    }

    void equipGreaterConjurersTalisman() {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Servant Gaze talisman test"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0,
                new ItemStack(ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    static ServantGaze spell() {
        return (ServantGaze) SpellRegistry.SERVANT_GAZE.get();
    }
}
