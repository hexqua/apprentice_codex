package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCapability;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReapingSkill;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

// optional型はGameTest入口クラスから分離し、Epic Fightがある場合だけ読み込む。
final class ScytheEpicFightTestHelper {
    private record Context(FakePlayer player, ServerPlayerPatch patch, SkillContainer container, EpicFightSpellReapingSkill skill) {
        void start() { patch.startSkillHolding(skill); }
        void release() { container.requestCasting(patch, new net.minecraft.nbt.CompoundTag()); }
        void clean() { skill.abort(patch); var entity = ScytheThrowManager.active(player); if (entity != null) entity.discard(); }
    }

    private static Context context(GameTestHelper h, int mana) {
        var player = ScytheThrowGameTests.player(h, mana);
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        h.assertTrue(patch != null, "Epic Fight must attach a server player patch");
        patch.toEpicFightMode(false);
        var skill = EpicFightSpellReaperScytheCompat.SPELL_REAPING.get();
        var container = patch.getSkill(SkillSlots.WEAPON_INNATE);
        container.setSkill(skill);
        return new Context(player, patch, container, skill);
    }

    static void capabilityAndRightClickIsolation(GameTestHelper h) {
        var c = context(h, 1000);
        try {
            var stack = c.player.getMainHandItem();
            var cap = EpicFightCapabilities.getItemStackCapability(stack);
            h.assertTrue(cap instanceof EpicFightSpellReaperScytheCapability, "Scythe must use its dedicated capability");
            h.assertTrue(cap.getInnateSkill(c.patch, stack) == c.skill, "Scythe must expose Spell Reaping");
            h.assertTrue(cap.getAutoAttackMotion(c.patch).size() >= 4, "Greatsword normal, dash and air attacks must remain");
            h.assertTrue(c.skill.getResourceType() == Skill.Resource.NONE, "Spell Reaping must not require weapon charge");
            for (boolean battle : new boolean[]{true, false}) {
                if (!battle) c.patch.toVanillaMode(false);
                for (var hand : InteractionHand.values()) {
                    h.assertTrue(stack.getItem().use(h.getLevel(), c.player, hand).getResult() == InteractionResult.PASS,
                            "Scythe right click must be empty in both modes and hands");
                }
                stack.getItem().releaseUsing(stack, h.getLevel(), c.player, 0);
                h.assertFalse(c.player.isUsingItem() || ScytheThrowManager.active(c.player) != null,
                        "Right click and release must not start a throw");
            }
            h.assertFalse(c.skill.isExecutableState(c.patch), "Non-battle mode must reject the innate");
            h.assertTrue(EpicFightSpellReapingSkill.distance(6) == 2.5 && EpicFightSpellReapingSkill.distance(30) == 10
                    && EpicFightSpellReapingSkill.distance(60) == 10, "Charge range must scale and clamp");
        } finally { c.clean(); }
        h.succeed();
    }

    static void equalEntityIdsKeepIndependentCharges(GameTestHelper h) {
        var first = context(h, 1000);
        var second = context(h, 1000);
        // 実clientを起動せず、統合serverで衝突するEntity.equals/hashCodeの条件を再現する。
        second.player.setId(first.player.getId());
        h.assertTrue(first.player != second.player && first.player.equals(second.player),
                "Distinct player instances must reproduce the integrated-server entity ID collision");
        first.start();
        second.start();
        second.skill.abort(second.patch);
        h.runAfterDelay(6, () -> {
            try {
                for (int tick = 0; tick < 6; tick++) {
                    first.patch.getAnimator().tick();
                    first.container.update();
                    first.skill.validateHolding(first.patch);
                }
                h.assertTrue(first.patch.isHoldingSkill(first.skill),
                        "Canceling an equal-ID player must not cancel or erase another player's charge");
                first.release();
                h.assertTrue(ScytheThrowManager.active(first.player) != null,
                        "The original player's charge must retain its stack and launch");
                h.assertTrue(MagicData.getPlayerMagicData(first.player).getMana() == 900,
                        "The surviving charge must spend mana once");
                h.assertTrue(ScytheThrowManager.active(second.player) == null
                                && MagicData.getPlayerMagicData(second.player).getMana() == 1000,
                        "The canceled equal-ID player must not launch or spend mana");
            } finally { first.clean(); second.clean(); }
            h.succeed();
        });
    }

    static void chargeLaunchRecallAndMana(GameTestHelper h) {
        var c = context(h, 1000);
        c.start();
        h.assertFalse(c.player.isUsingItem(), "Innate charge must not start vanilla item use");
        h.runAfterDelay(6, () -> {
            try {
                c.release();
                h.assertTrue(ScytheThrowManager.active(c.player) != null, "Minimum charge must launch");
                h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 900, "Throw must charge mana exactly once");
                c.release();
                h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 900, "Duplicate release must not launch twice");
                MagicData.getPlayerMagicData(c.player).setMana(0);
                c.start();
                h.assertTrue(ScytheThrowManager.active(c.player) == null, "Recall must succeed without mana or charge");
                c.release();
                h.assertTrue(ScytheThrowManager.active(c.player) == null, "Recall key release must not rethrow");
            } finally { c.clean(); }
            h.succeed();
        });
    }

    static void acceptedAttackRecallsButRejectedAirAttackDoesNot(GameTestHelper h) {
        var c = context(h, 1000);
        try {
            ScytheThrowManager.launchNormal(c.player, c.player.getMainHandItem(), 10);
            var thrown = ScytheThrowManager.active(c.player);
            var combo = c.patch.getSkill(SkillSlots.COMBO_ATTACKS);
            combo.setSkill(yesman.epicfight.registry.entries.EpicFightSkills.COMBO_ATTACKS.get());
            // 標準はResource.NONE。プレイヤー限定の消費イベントでスタミナ設定時の拒否経路も通す。
            c.patch.getEventListener().registerEvent(yesman.epicfight.api.event.EpicFightEventHooks.Player.CONSUME_SKILL,
                    event -> {
                        if (event.getSkill() == combo.getSkill()) {
                            event.setResourceType(Skill.Resource.STAMINA);
                            event.setAmount(5.0F);
                        }
                    }, yesman.epicfight.api.event.IdentifierProvider.constant(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("apprenticecodex", "scythe_attack_test")));
            c.player.setOnGround(false);
            c.player.setDeltaMovement(0, 0.5, 0);
            c.patch.setStamina(0);
            combo.getSkill().executeOnServer(combo, new net.minecraft.nbt.CompoundTag());
            h.assertFalse(thrown.isRemoved(), "Rejected air attack must not recall the scythe");
            c.patch.setStamina(c.patch.getMaxStamina());
            combo.getSkill().executeOnServer(combo, new net.minecraft.nbt.CompoundTag());
            h.assertTrue(thrown.isRemoved(), "Accepted air attack must recall before the motion starts");
        } finally { c.clean(); }
        h.succeed();
    }

    static void interruptedChargesNeverLaunch(GameTestHelper h) {
        var swapped = context(h, 1000);
        var canceled = context(h, 1000);
        var mode = context(h, 1000);
        swapped.start(); canceled.start(); mode.start();
        h.runAfterDelay(6, () -> {
            try {
                swapped.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
                swapped.skill.validateHolding(swapped.patch);
                canceled.skill.cancelOnServer(canceled.container, null);
                mode.patch.toVanillaMode(false);
                EpicFightSpellReaperScytheCompat.tick(mode.player);
                for (var c : new Context[]{swapped, canceled, mode}) {
                    c.release();
                    h.assertTrue(ScytheThrowManager.active(c.player) == null, "Interrupted or stale release must not launch");
                    h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 1000, "Interrupted charge must not spend mana");
                }
                mode.patch.toEpicFightMode(false);
                ScytheThrowManager.launchNormal(mode.player, mode.player.getMainHandItem(), 10);
                mode.patch.toVanillaMode(false);
                EpicFightSpellReaperScytheCompat.tick(mode.player);
                h.assertTrue(ScytheThrowManager.active(mode.player) == null, "Mode switch must recall an existing throw");
            } finally { swapped.clean(); canceled.clean(); mode.clean(); }
            h.succeed();
        });
    }

    static void chargeBoundariesAndInsufficientMana(GameTestHelper h) {
        var shortCharge = context(h, 1000);
        var exhausted = context(h, 0);
        var overtime = context(h, 1000);
        shortCharge.start(); exhausted.start(); overtime.start();
        h.runAfterDelay(5, () -> {
            shortCharge.release();
            h.assertTrue(ScytheThrowManager.active(shortCharge.player) == null, "Five ticks must not launch");
            shortCharge.clean();
        });
        h.runAfterDelay(6, () -> {
            exhausted.release();
            h.assertTrue(ScytheThrowManager.active(exhausted.player) == null, "Insufficient mana must not launch");
            exhausted.clean();
        });
        h.runAfterDelay(61, () -> {
            try {
                overtime.release();
                h.assertTrue(ScytheThrowManager.active(overtime.player) == null, "Holding timeout must cancel without throwing");
            } finally { overtime.clean(); }
            h.succeed();
        });
    }

    static void malumInstantSkillsAndRecallPriority(GameTestHelper h) {
        if (!ModList.get().isLoaded("malum")) { h.succeed(); return; }
        var c = context(h, 1000);
        try {
            var stack = c.player.getMainHandItem();
            ScytheReboundGameTests.enchant(h, stack, "rebound", 1);
            c.start();
            h.assertTrue(ScytheThrowManager.active(c.player) != null && ScytheThrowManager.active(c.player).isRebound(),
                    "Rebound innate must throw immediately");
            c.release();
            ScytheReboundGameTests.enchant(h, stack, "ascension", 1);
            c.start();
            h.assertTrue(ScytheThrowManager.active(c.player) == null, "Recall must take priority over Ascension");
            c.release();
            h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 900, "Recall must not spend Ascension mana");
            c.start();
            h.assertTrue(c.player.getDeltaMovement().y > 0, "Ascension must launch on the server");
            h.assertTrue(ScytheThrowManager.active(c.player) == null, "Ascension must win over forced Rebound");
            h.assertFalse(c.player.getCooldowns().isOnCooldown(stack.getItem()), "Innate Ascension must not add item cooldown");
            h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 700, "Ascension must spend mana exactly once");
            c.release();
            c.start(); c.release();
            h.assertTrue(MagicData.getPlayerMagicData(c.player).getMana() == 500, "A new press may immediately repeat Ascension");
        } finally { c.clean(); }
        h.succeed();
    }
}
