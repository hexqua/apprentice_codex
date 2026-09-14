package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.data.attachment.StaffAbilityData;
import com.sammy.malum.core.handlers.GeasEffectHandler;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import com.sammy.malum.registry.common.item.MalumItems;
import com.sammy.malum.registry.common.magic.MalumGeasEffectTypes;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.UUID;

final class SpellCastParryingRingMalumScenarios {
    private SpellCastParryingRingMalumScenarios() {}

    static void windows(GameTestHelper h) {
        try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
            for (var item : staffs()) {
                for (var hand : InteractionHand.values()) {
                    var player = player(h, item, hand, true);
                    // 短縮後の満充填保持も検証するため、必要時間を2 tickに固定する。
                    player.getAttribute(MalumAttributes.CHARGE_DURATION).setBaseValue(0.1);
                    start(h, player, hand);
                    var source = front(h, player);
                    h.assertTrue(attack(player, source), "Staff must parry at charge start");
                    player.advanceUse(10);
                    h.assertValueEqual(player.getTicksUsingItem(), 10, "Actual item use must reach the boundary");
                    h.assertTrue(attack(player, source), "Fully charged held staff must parry at the boundary");
                    h.assertTrue(attack(player, source), "Parrying must not consume the window");
                    h.assertValueEqual(player.getData(MalumAttachmentTypes.STAFF_ABILITIES).getStaffChargeDebt(), 0, "Parry must not spend staff charges");
                    h.assertValueEqual(player.getUseItem().getDamageValue(), 0, "Parry must not damage staff");
                    player.advanceUse(1);
                    h.assertFalse(attack(player, source), "Staff must not parry after the window");
                    player.releaseUsingItem();
                    h.assertFalse(attack(player, source), "Released staff must not parry");
                    player.setData(MalumAttachmentTypes.STAFF_ABILITIES, new StaffAbilityData(0, 0));
                    start(h, player, hand);
                    h.assertTrue(attack(player, source), "Starting again must reset the parry window");
                    player.releaseUsingItem();
                    h.assertValueEqual(player.getData(MalumAttachmentTypes.STAFF_ABILITIES).getStaffChargeDebt(), 0, "Incomplete charge cancellation must remain free");
                    h.assertFalse(attack(player, source), "Canceled charge must not parry");
                    player.discard();
                }
            }
        }
    }

    static void defense(GameTestHelper h) {
        try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
            var player = player(h, MalumItems.MNEMONIC_HEX_STAFF.get(), InteractionHand.MAIN_HAND, false);
            start(h, player, InteractionHand.MAIN_HAND);
            h.assertFalse(attack(player, front(h, player)), "Staff without ring must not parry");
            player.discard();
            player = player(h, MalumItems.MNEMONIC_HEX_STAFF.get(), InteractionHand.MAIN_HAND, true);
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
        try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
            for (var item : staffs()) {
                var player = player(h, item, InteractionHand.MAIN_HAND, true);
                var source = front(h, player);
                player.setData(MalumAttachmentTypes.STAFF_ABILITIES, new StaffAbilityData(6, 0));
                item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.assertFalse(player.isUsingItem(), "Empty staff charges must reject use");
                h.assertFalse(attack(player, source), "Rejected staff use must not parry");
                player.setData(MalumAttachmentTypes.STAFF_ABILITIES, new StaffAbilityData(0, 0));
                player.getAttribute(MalumAttributes.CHARGE_DURATION).setBaseValue(0);
                item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.assertFalse(attack(player, source), "Instant staff use must not parry even before use state ends");
                player.stopUsingItem();
                player.setData(MalumAttachmentTypes.STAFF_ABILITIES, new StaffAbilityData(0, 0));
                player.getAttribute(MalumAttributes.CHARGE_DURATION).setBaseValue(0.1);
                GeasEffectHandler.addGeasEffect(player, MalumGeasEffectTypes.OATH_OF_THE_OVEREAGER_FIST.get());
                start(h, player, InteractionHand.MAIN_HAND);
                h.assertTrue(attack(player, source), "Automatic release staff must parry before firing");
                player.advanceUse(3);
                h.assertFalse(player.isUsingItem(), "Geas must automatically release fully charged staff");
                h.assertFalse(attack(player, source), "Automatically released staff must not parry");
                player.discard();
            }
        }
    }

    private static List<Item> staffs() {
        return List.of(MalumItems.MNEMONIC_HEX_STAFF.get(), MalumItems.EROSION_SCEPTER.get(), MalumItems.UNWINDING_CHAOS.get());
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
        player.getAttribute(MalumAttributes.CHARGE_CAPACITY).setBaseValue(2);
        player.getAttribute(MalumAttributes.CHARGE_DURATION).setBaseValue(1);
        player.getAttribute(team.lodestar.lodestone.registry.common.LodestoneAttributes.MAGIC_DAMAGE).setBaseValue(5);
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
        var event = new LivingIncomingDamageEvent(player, new DamageContainer(source, 4));
        NeoForge.EVENT_BUS.post(event);
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
