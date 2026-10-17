package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;

// optional型は入口クラスから隔離し、MOD未導入時には解決しない。
final class PsychicDriveGauntletsEpicFightTestHelper {
    private PsychicDriveGauntletsEpicFightTestHelper() {}
    static void check(GameTestHelper helper) {
        var player = ScytheThrowGameTests.player(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        helper.assertTrue(patch != null, "Epic Fight must attach a player patch");
        patch.toEpicFightMode(false);
        helper.assertTrue(EpicFightCapabilities.getItemStackCapability(player.getMainHandItem())
                instanceof RangedWeaponCapability, "Gauntlets must use a non-melee capability");
        var combo = patch.getSkill(SkillSlots.COMBO_ATTACKS);
        combo.setSkill(EpicFightSkills.COMBO_ATTACKS.get());
        var blocked = new SkillCastEvent(patch, combo, new CompoundTag());
        blocked.setStateExecutable(true);
        blocked.setSkillExecutable(true);
        EpicFightEventHooks.Player.CAST_SKILL.postWithListener(blocked, patch.getEventListener());
        helper.assertTrue(blocked.isCanceled(), "Epic Fight basic attack must be blocked with mainhand gauntlets");
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var normal = new SkillCastEvent(patch, combo, new CompoundTag());
        normal.setStateExecutable(true);
        normal.setSkillExecutable(true);
        EpicFightEventHooks.Player.CAST_SKILL.postWithListener(normal, patch.getEventListener());
        helper.assertTrue(!normal.isCanceled(), "Offhand gauntlets must not block the mainhand basic attack");
        helper.succeed();
    }
}
