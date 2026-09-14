package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.List;
import java.util.UUID;

final class SpellCastParryingRingMalumScenarios {
    private SpellCastParryingRingMalumScenarios() {}

    static void windows(GameTestHelper h) {
        try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
            for (var item : staffs()) {
                for (var hand : InteractionHand.values()) {
                    var player = player(h, item, hand, true);
                    // 最短チャージ後の保持も、使用開始からの受付時間で判定する。
                    BowGameTestSupport.equipCurio(player, "charm", new ItemStack(ItemRegistry.MANA_SOUL_TRANSDUCER.get()));
                    player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get()).setBaseValue(100);
                    start(h, player, hand);
                    var source = front(h, player);
                    h.assertTrue(attack(player, source), "Staff must parry at charge start");
                    player.advanceUse(10);
                    h.assertTrue(player.getTicksUsingItem() == 10, "Actual item use must reach the boundary");
                    h.assertTrue(attack(player, source), "Fully charged held staff must parry at the boundary");
                    h.assertTrue(attack(player, source), "Parrying must not consume the window");
                    h.assertTrue(MalumPlayerDataCapability.getCapability(player).reserveStaffChargeHandler.chargeCount == 0, "Parry must not spend staff charges");
                    h.assertTrue(player.getUseItem().getDamageValue() == 0, "Parry must not damage staff");
                    player.advanceUse(1);
                    h.assertFalse(attack(player, source), "Staff must not parry after the window");
                    player.releaseUsingItem();
                    h.assertFalse(attack(player, source), "Released staff must not parry");
                    player.getCooldowns().removeCooldown(item);
                    start(h, player, hand);
                    h.assertTrue(attack(player, source), "Starting again must reset the parry window");
                    player.releaseUsingItem();
                    h.assertTrue(MalumPlayerDataCapability.getCapability(player).reserveStaffChargeHandler.chargeCount == 0, "Incomplete charge cancellation must remain free");
                    h.assertFalse(attack(player, source), "Canceled charge must not parry");
                    player.discard();
                }
            }
        }
    }

    static void defense(GameTestHelper h) {
        try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
            var player = player(h, com.sammy.malum.registry.common.item.ItemRegistry.MNEMONIC_HEX_STAFF.get(), InteractionHand.MAIN_HAND, false);
            start(h, player, InteractionHand.MAIN_HAND);
            h.assertFalse(attack(player, front(h, player)), "Staff without ring must not parry");
            player.discard();
            player = player(h, com.sammy.malum.registry.common.item.ItemRegistry.MNEMONIC_HEX_STAFF.get(), InteractionHand.MAIN_HAND, true);
            start(h, player, InteractionHand.MAIN_HAND);
            h.assertTrue(attack(player, front(h, player)), "Equipped ring must parry staff charging");
            var attacker = BowGameTestSupport.createEquipmentTestPlayer(h.getLevel(), player.blockPosition(), "malum_parry_back");
            attacker.setPos(player.getX(), player.getY(), player.getZ() - 3);
            h.assertFalse(attack(player, h.getLevel().damageSources().mobAttack(attacker)), "Back attack must not be parried");
            attacker.setPos(player.getX(), player.getY(), player.getZ() + 3);
            h.assertFalse(attack(player, CombatTools.getDamageSource(h.getLevel(), attacker, DamageTypes.UNITE_LUNA)), "Shield bypass must not be parried");
            var arrow = new Arrow(EntityType.ARROW, h.getLevel());
            arrow.setPos(player.getX(), player.getY() + 1, player.getZ() + 3);
            h.getLevel().addFreshEntity(arrow);
            h.assertTrue(attack(player, h.getLevel().damageSources().arrow(arrow, attacker)), "Front projectile must be parried");
            h.assertTrue(arrow.isRemoved(), "Parried projectile must be discarded");
            player.stopUsingItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            player.startUsingItem(InteractionHand.MAIN_HAND);
            h.assertFalse(attack(player, front(h, player)), "Ordinary bow must not gain staff parrying");
            player.discard();
        }
    }

    static void release(GameTestHelper h) {
        for (var item : staffs()) {
            var player = player(h, item, InteractionHand.MAIN_HAND, true);
            player.getCooldowns().addCooldown(item, 100);
            item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertFalse(player.isUsingItem(), "Cooling down staff must reject use");
            h.assertFalse(attack(player, front(h, player)), "Rejected staff use must not parry");
            player.getCooldowns().removeCooldown(item);
            start(h, player, InteractionHand.MAIN_HAND);
            player.releaseUsingItem();
            h.assertFalse(attack(player, front(h, player)), "Released staff must not parry");
            player.discard();
        }
    }

    private static List<Item> staffs() {
        return List.of(com.sammy.malum.registry.common.item.ItemRegistry.MNEMONIC_HEX_STAFF.get(), com.sammy.malum.registry.common.item.ItemRegistry.EROSION_SCEPTER.get(), com.sammy.malum.registry.common.item.ItemRegistry.STAFF_OF_THE_AURIC_FLAME.get());
    }

    private static StaffPlayer player(GameTestHelper h, Item item, InteractionHand hand, boolean ring) {
        var player = new StaffPlayer(h.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.setYRot(0);
        player.setXRot(0);
        h.getLevel().addFreshEntity(player);
        player.setItemInHand(hand, new ItemStack(item));
        // FakePlayer の通常tickに依存せず、使用処理が必要とする属性を明示する。
        player.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry.MAGIC_DAMAGE.get()).setBaseValue(5);
        if (ring) equip(player);
        return player;
    }

    private static void equip(FakePlayer player) {
        BowGameTestSupport.equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.RING_SLOT,
                new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
    }

    private static void start(GameTestHelper h, StaffPlayer player, InteractionHand hand) {
        player.getItemInHand(hand).getItem().use(h.getLevel(), player, hand);
        h.assertTrue(player.isUsingItem(), "Staff use must start actual item charging");
    }

    private static DamageSource front(GameTestHelper h, StaffPlayer player) {
        var attacker = BowGameTestSupport.createEquipmentTestPlayer(h.getLevel(), player.blockPosition(), "malum_parry_front");
        attacker.setPos(player.getX(), player.getY(), player.getZ() + 3);
        return h.getLevel().damageSources().mobAttack(attacker);
    }

    private static boolean attack(FakePlayer player, DamageSource source) {
        var event = new LivingAttackEvent(player, source, 4);
        MinecraftForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    private static final class StaffPlayer extends FakePlayer {
        StaffPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "malum_parry"));
        }

        void advanceUse(int ticks) {
            // 残り時間の直接書き換えではなく、vanilla の使用tickとMalumのonUseTickを通す。
            for (int i = 0; i < ticks && isUsingItem(); i++) updateUsingItem(getUseItem());
        }
    }
}
