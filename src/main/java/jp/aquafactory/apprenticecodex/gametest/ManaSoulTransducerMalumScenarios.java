package jp.aquafactory.apprenticecodex.gametest;

import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

final class ManaSoulTransducerMalumScenarios {
    private ManaSoulTransducerMalumScenarios() {}
    static void payments(GameTestHelper h) {
        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "transducer_staff");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof AbstractStaffItem staff)) continue;
            var stack = new ItemStack(staff);
            player.stopUsingItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry.MAGIC_DAMAGE.get()).setBaseValue(5);
            BowGameTestSupport.equipCurio(player, "charm", new ItemStack(ItemRegistry.MANA_SOUL_TRANSDUCER.get()));
            player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()).setBaseValue(1000);
            // スロット拡張で複数装備された場合を、テスト用の二つ目のスロットで再現する。
            BowGameTestSupport.equipCurio(player, "ring", new ItemStack(ItemRegistry.MANA_SOUL_TRANSDUCER.get()));
            var mana = MagicData.getPlayerMagicData(player);
            var charges = MalumPlayerDataCapability.getCapability(player).reserveStaffChargeHandler;
            charges.chargeProgress = 123;
            for (int reserve : new int[]{0, 2}) {
                charges.chargeCount = reserve;
                mana.setMana(160);
                player.startUsingItem(InteractionHand.MAIN_HAND);
                staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
                h.assertTrue(mana.getMana() == 80, "Successful shot must pay once");
                h.assertTrue(charges.chargeCount == reserve, "Paid shot must preserve reserves");
                h.assertFalse(player.getCooldowns().isOnCooldown(staff), "Paid shot must avoid cooldown");
                h.assertTrue(charges.chargeProgress == 123, "Paid shot must preserve recovery progress");
            }
            charges.chargeCount = 1;
            mana.setMana(79);
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
            h.assertTrue(charges.chargeCount == 0, "Insufficient mana must spend reserves");
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
            h.assertTrue(player.getCooldowns().isOnCooldown(staff), "Empty reserves must cause cooldown");
            MalumStaffChargeBridge.clearHeldCooldowns(player);
            h.assertTrue(player.getCooldowns().isOnCooldown(staff), "Insufficient mana must preserve cooldown");
            mana.setMana(160);
            MalumStaffChargeBridge.clearHeldCooldowns(player);
            h.assertFalse(player.getCooldowns().isOnCooldown(staff), "Payment must clear existing cooldown");
            h.assertTrue(mana.getMana() == 80, "Cooldown removal must cost mana");
            MalumStaffChargeBridge.clearHeldCooldowns(player);
            h.assertTrue(mana.getMana() == 80, "Cleared cooldown must not charge again");
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
            h.assertTrue(mana.getMana() == 0, "Shot after removal must pay separately");
            mana.setMana(160);
            int damage = stack.getDamageValue();
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack));
            h.assertTrue(mana.getMana() == 160, "Canceled charge must not spend mana");
            h.assertTrue(stack.getDamageValue() == damage, "Canceled charge must not damage staff");
            player.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry.MAGIC_DAMAGE.get()).setBaseValue(0);
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
            h.assertTrue(mana.getMana() == 160, "Failed shot must not spend mana");
            player.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry.MAGIC_DAMAGE.get()).setBaseValue(5);
            var castAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get());
            castAttribute.setBaseValue(1.5);
            int ticks = jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerLogic.chargeTicks(staff.chargeDuration, 1.5, 0.8);
            mana.setMana(160);
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - ticks + 1);
            h.assertTrue(mana.getMana() == 160, "Charge below shortened boundary must not fire");
            staff.onUseTick(h.getLevel(), player, stack, staff.getUseDuration(stack) - ticks);
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - ticks);
            h.assertTrue(mana.getMana() == 80, "Shortened boundary must fire");
            h.assertTrue(stack.getDamageValue() > damage, "Paid shots must still damage the staff");
            castAttribute.setBaseValue(1);
            // 同じ種類を両手に持つ場合も、解除は種類ごとに一回だけ支払う。
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(staff));
            player.getCooldowns().addCooldown(staff, 100);
            mana.setMana(160);
            MalumStaffChargeBridge.clearHeldCooldowns(player);
            h.assertTrue(mana.getMana() == 80, "Same staff in both hands must only pay once");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var charms = inventory.getStacksHandler("charm").orElseThrow().getStacks();
            charms.setStackInSlot(0, ItemStack.EMPTY);
            BowGameTestSupport.equipCurio(player, "ring", ItemStack.EMPTY);
            mana.setMana(160);
            charges.chargeCount = 1;
            staff.releaseUsing(stack, h.getLevel(), player, staff.getUseDuration(stack) - 100);
            h.assertTrue(mana.getMana() == 160 && charges.chargeCount == 0, "Unequipped shot must use Malum reserves");
        }
        player.discard();
    }
}
