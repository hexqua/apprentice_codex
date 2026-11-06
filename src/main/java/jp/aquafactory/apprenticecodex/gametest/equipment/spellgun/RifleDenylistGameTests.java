package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RifleDenylistGameTests {
    private RifleDenylistGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void multipurposeDenylistRejectsStoredSpellWithoutSpendingResources(GameTestHelper helper) {
        assertDenylistCast(helper, false);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void fullautoDenylistRejectsStoredSpellWithoutSpendingResources(GameTestHelper helper) {
        assertDenylistCast(helper, true);
    }

    private static void assertDenylistCast(GameTestHelper helper, boolean fullauto) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "rifle_denylist");
        var stack = new ItemStack(fullauto ? ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get()
                : ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var ammo = fullauto ? ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get() : ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().add(new ItemStack(ammo, 2));
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(1000);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var lookup = helper.getLevel().registryAccess();
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1));
        try (var multipurpose = ApprenticeCodexServerConfig.useMultipurposeStaffrifleSpellDenylistOverrideForGameTest(List.of());
             var automatic = ApprenticeCodexServerConfig.useFullautoRapidcastSpellrifleSpellDenylistOverrideForGameTest(List.of())) {
            if (fullauto) {
                FullautoRapidcastSpellrifleScrollStorage.set(stack, 0, scroll, lookup);
            } else {
                MultipurposeStaffrifleScrollStorage.set(stack, 0, scroll, lookup);
            }
            float manaBefore = magic.getMana();
            // 保存後の設定変更も発動時に再評価し、拒否だけで資源や詠唱状態を変えない。
            try (var denied = deny(fullauto, spell.getSpellId())) {
                helper.assertFalse(trigger(stack, player), "Denylisted stored spell must not initiate");
                helper.assertTrue(magic.getMana() == manaBefore, "Denied cast must preserve mana");
                helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), ammo) == 2,
                        "Denied cast must preserve ammunition");
                helper.assertFalse(magic.isCasting(), "Denied cast must not start casting");
                helper.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Denied cast must not start cooldown");
            }
            // 自分の設定を戻せば、他方のライフルだけを禁止していても同じ収納物を使用できる。
            try (var otherDenied = deny(!fullauto, spell.getSpellId())) {
                helper.assertTrue(trigger(stack, player), "Restored rifle must cast independently of the other denylist");
                spell.castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
                helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), ammo) == 1,
                        "Accepted cast must consume exactly one round");
                helper.assertTrue(fullauto ? magic.getMana() < manaBefore : magic.getMana() == manaBefore,
                        "Accepted cast must retain the rifle's mana policy");
                var stored = fullauto ? FullautoRapidcastSpellrifleScrollStorage.get(stack, 0, lookup)
                        : MultipurposeStaffrifleScrollStorage.get(stack, 0, lookup);
                helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, stored), "Denylist changes must preserve the scroll");
            }
        }
        helper.succeed();
    }

    private static ApprenticeCodexServerConfig.GameTestConfigOverride deny(boolean fullauto, String spellId) {
        return fullauto ? ApprenticeCodexServerConfig.useFullautoRapidcastSpellrifleSpellDenylistOverrideForGameTest(List.of(spellId))
                : ApprenticeCodexServerConfig.useMultipurposeStaffrifleSpellDenylistOverrideForGameTest(List.of(spellId));
    }

    private static boolean trigger(ItemStack stack, ServerPlayer player) {
        return stack.getItem() instanceof FullautoRapidcastSpellrifle rifle
                ? rifle.tryTriggerSelectedSpell(player, false)
                : ((MultipurposeStaffrifle) stack.getItem()).tryTriggerSelectedSpell(player, false);
    }
}
