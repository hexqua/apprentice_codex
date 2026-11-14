package jp.aquafactory.apprenticecodex.gametest.spell.counterspell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarManager;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowCoreEntity;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class NonAttackableCounterspellScenarios {
    private NonAttackableCounterspellScenarios() {}

    static void autoMagnet(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 3), "magnet_counter_owner");
        AutoMagnetFamiliarManager.activate(owner, 4.0, 0.0);
        try {
            var familiars = helper.getLevel().getEntitiesOfClass(AutoMagnetFamiliarEntity.class,
                    owner.getBoundingBox().inflate(4), entity -> entity.getOwner() == owner);
            helper.assertTrue(familiars.size() == 1, "AutoMagnet must spawn one managed familiar before Counterspell");
            castAt(helper, familiars.getFirst());
            var state = Capabilities.getSpellDataOrNull(owner).get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
            helper.assertFalse(state.active || state.getFamiliarUuid() != null,
                    "Counterspell must clear the non-attackable familiar's managed state");
        } finally {
            AutoMagnetFamiliarManager.deactivate(owner);
        }
        helper.succeed();
    }

    static void lethalAssault(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 3), "rifle_counter_owner");
        castAt(helper, new LethalAssaultRifleEntity(EntityRegistry.LETHAL_ASSAULT_RIFLE.get(), helper.getLevel(), owner));
        helper.succeed();
    }

    static void echoArrowCore(GameTestHelper helper) {
        castAt(helper, new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), helper.getLevel()));
        helper.succeed();
    }

    private static void castAt(GameTestHelper helper, Entity target) {
        var caster = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "construct_counter_caster");
        caster.setYRot(0);
        caster.setXRot(0);
        target.setPos(caster.getEyePosition().add(0, -target.getBbHeight() * 0.5, 2));
        if (!target.isAddedToLevel()) {
            helper.getLevel().addFreshEntity(target);
        }
        try {
            helper.assertFalse(target.isAttackable(), "Counterspell fixture must reject ordinary attacks");
            // 解除callbackの直呼びではなく、Iron'sの対象選択から通して通常攻撃との独立性を守る。
            castCounterspell(caster);
            helper.assertTrue(target.isRemoved(), "Counterspell raycast must remove the non-attackable construct");
        } finally {
            target.discard();
        }
    }

    private static void castCounterspell(FakePlayer caster) {
        SpellRegistry.COUNTERSPELL_SPELL.get().onCast(caster.level(), 1, caster,
                CastSource.SPELLBOOK, MagicData.getPlayerMagicData(caster));
    }
}
