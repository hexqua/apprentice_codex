package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.item.ManaForceReleaseDeviceServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseDevice;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class ManaForceReleaseDeviceGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ManaForceReleaseDeviceGameTests() {}

    // FakePlayer.tick は空実装なので、実際のLivingEntityのアイテム使用処理だけを進める。
    private static final class DevicePlayer extends FakePlayer {
        DevicePlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "mana_device_test")); }
        void tickUse() { if (isUsingItem()) updateUsingItem(getUseItem()); }
    }

    private static DevicePlayer player(GameTestHelper helper, Item item) {
        var player = new DevicePlayer(helper.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(BlockPos.containing(1, 18, 1).getCenter()));
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA.get())).setBaseValue(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        MagicData.getPlayerMagicData(player).setMana(100);
        return player;
    }

    private static ManaForceReleaseDevice novice() {
        return (ManaForceReleaseDevice) ItemRegistry.NOVICE_MANA_FORCE_RELEASE_DEVICE.get();
    }

    @GameTest(template = TEMPLATE)
    public static void tierCurvesAndResistancePreserveIntent(GameTestHelper helper) {
        var novice = novice();
        var adept = (ManaForceReleaseDevice) ItemRegistry.ADEPT_MANA_FORCE_RELEASE_DEVICE.get();
        var master = (ManaForceReleaseDevice) ItemRegistry.MASTER_MANA_FORCE_RELEASE_DEVICE.get();
        helper.assertTrue(novice.damage(300, 1, Values.DEFAULT) == 18 && adept.damage(300, 1, Values.DEFAULT) == 17,
                "Low-capacity tier inversion must remain intentional");
        helper.assertTrue(Math.abs(novice.damage(301, 1, Values.DEFAULT) - 18.005F) < 0.001F
                        && adept.damage(500, 1, Values.DEFAULT) == 25
                        && Math.abs(adept.damage(501, 1, Values.DEFAULT) - 25.01F) < 0.001F,
                "Soft caps must be continuous and use the correct excess mana");
        helper.assertTrue(master.damage(3000, 2, Values.DEFAULT) == 210,
                "Master must have no damage soft cap");
        helper.assertTrue(novice.resistanceMultiplier(2) < 1 && master.resistanceMultiplier(2) == 1
                        && master.resistanceMultiplier(0.5) > 1
                        && master.resistanceMultiplier(0.5) == novice.resistanceMultiplier(0.5),
                "Master must ignore only increased spell resistance");
        var configured = new Values(2, 3, 4, 100, 200);
        helper.assertTrue(novice.damage(200, 2, configured) == 34 && adept.damage(200, 1, configured) == 39,
                "Server multipliers and configurable soft caps must affect damage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void startChecksManaOffhandAndExistingCast(GameTestHelper helper) {
        var player = player(helper, novice());
        var data = MagicData.getPlayerMagicData(player);
        data.setMana(19.99F);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.isUsingItem(), "Mana below twenty percent must not start charging");
        data.setMana(20);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "Exactly twenty percent must start charging");
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(novice()));
        novice().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(!player.isUsingItem(), "Offhand use must be rejected");
        data.initiateCast(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 20, CastSource.SPELLBOOK,
                SpellSelectionManager.MAINHAND);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.isUsingItem(), "An existing spell cast must take priority");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullChargeFiresOnceWithCurrentManaAndIdCooldown(GameTestHelper helper) {
        var player = player(helper, novice());
        var stack = player.getMainHandItem();
        var data = MagicData.getPlayerMagicData(player);
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION.get())).setBaseValue(50);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 39; tick++) player.tickUse();
        helper.assertTrue(data.getMana() == 100 && stack.getDamageValue() == 0 && player.isUsingItem(),
                "Casting speed must not shorten the fixed forty-tick charge");
        data.setMana(60);
        player.tickUse();
        helper.assertTrue(data.getMana() == 0 && stack.getDamageValue() == 1 && !player.isUsingItem(),
                "Full charge must consume current mana and one durability, then stop using");
        helper.assertTrue(player.getCooldowns().isOnCooldown(new ItemStack(novice()).getItem())
                        && !player.getCooldowns().isOnCooldown(ItemRegistry.ADEPT_MANA_FORCE_RELEASE_DEVICE.get()),
                "Cooldown must cover all stacks of the same ID, but no other tier");
        for (int tick = 0; tick < 40; tick++) player.tickUse();
        helper.assertTrue(stack.getDamageValue() == 1, "A finished charge must not fire repeatedly");
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.isUsingItem(), "Cooldown must reject immediate restart");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationPenaltiesAreLimitedToManaDepletion(GameTestHelper helper) {
        var player = player(helper, novice());
        var stack = player.getMainHandItem();
        var data = MagicData.getPlayerMagicData(player);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 10; tick++) player.tickUse();
        player.releaseUsingItem();
        helper.assertTrue(data.getMana() == 100 && stack.getDamageValue() == 0
                        && !player.getCooldowns().isOnCooldown(novice()),
                "Manual release must have no cost or penalty");
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        data.setMana(19);
        player.tickUse();
        helper.assertTrue(!player.isUsingItem() && data.getMana() == 19 && stack.getDamageValue() == 0
                        && player.getCooldowns().isOnCooldown(novice()),
                "Mana depletion must cancel immediately with only the cooldown penalty");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void normalSpellInterruptsChargeWithoutPenalty(GameTestHelper helper) {
        var player = player(helper, novice());
        var data = MagicData.getPlayerMagicData(player);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        helper.assertTrue(spell.attemptInitiateCast(player.getMainHandItem(), 1, helper.getLevel(), player,
                        CastSource.SPELLBOOK, true, SpellSelectionManager.MAINHAND),
                "Normal spell must be allowed to interrupt device charging");
        helper.assertTrue(!player.isUsingItem() && data.isCasting()
                        && !player.getCooldowns().isOnCooldown(novice()) && data.getMana() == 100,
                "Spell initiation must stop charging without spending device mana or adding a penalty");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rayDamageIgnoresSwordEnchantmentsAndStopsAtBlocks(GameTestHelper helper) {
        var player = player(helper, novice());
        player.setYRot(0);
        player.setXRot(0);
        var target = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.position().add(0, 0, 5));
        helper.getLevel().addFreshEntity(target);
        var sharpness = Enchantments.SHARPNESS;
        player.getMainHandItem().enchant(sharpness, 5);
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER.get())).setBaseValue(100);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 40; tick++) player.tickUse();
        helper.assertTrue(Math.abs(target.getHealth() - 12) < 0.01F,
                "Special attack must use eight magic damage, ignoring armor, Sharpness, and school power");
        target.invulnerableTime = 0;
        helper.getLevel().setBlockAndUpdate(BlockPos.containing(player.getEyePosition().add(0, 0, 2)),
                Blocks.STONE.defaultBlockState());
        player.getCooldowns().removeCooldown(novice());
        MagicData.getPlayerMagicData(player).setMana(100);
        novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 40; tick++) player.tickUse();
        helper.assertTrue(Math.abs(target.getHealth() - 12) < 0.01F,
                "A blocking wall must prevent damage even with the wide targeting ray");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wideFallbackDamagesOffsetTargetAndKeepsThinPriority(GameTestHelper helper) {
        var player = player(helper, novice());
        player.setYRot(0);
        player.setXRot(0);
        var offset = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        var direct = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        offset.setNoAi(true);
        offset.setNoGravity(true);
        offset.setPos(player.position().add(1, 0, 4));
        direct.setNoAi(true);
        direct.setNoGravity(true);
        direct.setPos(player.position().add(0, 0, 6));
        helper.getLevel().addFreshEntity(offset);
        helper.getLevel().addFreshEntity(direct);
        try {
            novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 40; tick++) player.tickUse();
            helper.assertTrue(direct.getHealth() < direct.getMaxHealth() && offset.getHealth() == offset.getMaxHealth(),
                    "The device must prioritize a thin-ray target over a closer wide-only target");
            direct.discard();
            player.getCooldowns().removeCooldown(novice());
            MagicData.getPlayerMagicData(player).setMana(100);
            novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 40; tick++) player.tickUse();
            helper.assertTrue(Math.abs(offset.getHealth() - 12) < 0.01F
                            && MagicData.getPlayerMagicData(player).getMana() == 0
                            && player.getMainHandItem().getDamageValue() == 2,
                    "The wide fallback must deal damage with the normal full mana and durability costs");
        } finally {
            offset.discard();
            direct.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void durabilityRepairEnchantmentsAndMalumTags(GameTestHelper helper) {
        var player = player(helper, novice());
        for (var item : new Item[]{novice(), ItemRegistry.ADEPT_MANA_FORCE_RELEASE_DEVICE.get(),
                ItemRegistry.MASTER_MANA_FORCE_RELEASE_DEVICE.get()}) {
            var stack = new ItemStack(item);
            helper.assertTrue(stack.isDamageableItem() && stack.canPerformAction(ToolActions.SWORD_SWEEP)
                            && stack.is(ManaForceReleaseDevice.MALUM_SOUL_HUNTER_WEAPON)
                            && stack.is(ManaForceReleaseDevice.MALUM_MAGIC_CAPABLE_WEAPON),
                    "Each device must be durable, sweep-capable, and Malum-compatible");
            helper.assertTrue(item.isValidRepairItem(stack,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())),
                    "All devices must accept arcane essence as repair material");
            for (var key : List.of(Enchantments.SHARPNESS, Enchantments.SWEEPING_EDGE,
                    Enchantments.MOB_LOOTING, Enchantments.UNBREAKING, Enchantments.MENDING)) {
                helper.assertTrue(key.canApplyAtEnchantingTable(stack),
                        "Sword and durability enchantments must be supported");
            }
            stack.mineBlock(helper.getLevel(), Blocks.STONE.defaultBlockState(), player.blockPosition(), player);
            helper.assertTrue(stack.getDamageValue() == 0, "Mining must not consume durability");
            helper.assertTrue(!item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Crafting materials must not replace the agreed arcane essence repair material");
            var target = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
            item.hurtEnemy(stack, target, player);
            helper.assertTrue(stack.getDamageValue() == 1, "A normal melee hit must consume one durability");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void specialAttackRespectsUnbreaking(GameTestHelper helper) {
        var player = player(helper, novice());
        var stack = player.getMainHandItem();
        var unbreaking = Enchantments.UNBREAKING;
        stack.enchant(unbreaking, 3);
        player.getRandom().setSeed(12345);
        for (int shot = 0; shot < 30; shot++) {
            player.getCooldowns().removeCooldown(novice());
            MagicData.getPlayerMagicData(player).setMana(100);
            novice().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 40; tick++) player.tickUse();
        }
        helper.assertTrue(stack.getDamageValue() > 0 && stack.getDamageValue() < 30,
                "Seeded repeated shots must apply vanilla Unbreaking durability reduction");
        helper.succeed();
    }
}
