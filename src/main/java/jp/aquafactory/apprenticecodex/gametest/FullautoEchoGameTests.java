package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import net.minecraft.world.level.GameType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoCasting;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoConfigState;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRateLimiter;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.network.packet.SyncFullautoEchoConfigPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FullautoEchoGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.fullauto_echo";

    private static ModConfigSpec.BooleanValue enabled() {
        return ApprenticeCodexServerConfig.SPEC.getValues().get("Items.FullautoRapidcastSpellrifle.echoCastEnabled");
    }

    private static ModConfigSpec.DoubleValue multiplier(String kind) {
        return ApprenticeCodexServerConfig.SPEC.getValues().get("Items.FullautoRapidcastSpellrifle.echoCast" + kind + "Multiplier");
    }

    private record Settings(boolean enabled, double damage, double mana) implements AutoCloseable {
        static Settings defaults() {
            var old = new Settings(FullautoEchoGameTests.enabled().get(), multiplier("Damage").get(), multiplier("ManaCost").get());
            FullautoEchoGameTests.enabled().set(true);
            multiplier("Damage").set(1.0);
            multiplier("ManaCost").set(2.0);
            return old;
        }

        @Override
        public void close() {
            FullautoEchoGameTests.enabled().set(enabled);
            multiplier("Damage").set(damage);
            multiplier("ManaCost").set(mana);
        }
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 30, 1), name);
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(20000);
        player.setYRot(0);
        player.setXRot(0);
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get()), player.registryAccess()),
                "Echo staff must be accepted");
        player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 64));
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(10000);
        return player;
    }

    private static boolean start(ServerPlayer player, AbstractSpell spell) {
        var stack = player.getMainHandItem();
        var magic = MagicData.getPlayerMagicData(player);
        magic.resetCastingState();
        magic.getPlayerCooldowns().getSpellCooldowns().clear();
        FullautoRapidcastSpellrifleRateLimiter.clear(player);
        FullautoRapidcastSpellrifleScrollStorage.set(stack, 0,
                SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)), player.registryAccess());
        return ((FullautoRapidcastSpellrifle) stack.getItem()).tryTriggerSelectedSpell(player, false);
    }

    // FakePlayerの次tick発動を明示実行し、開始時だけのコンテキストでは通らない経路を検証する。
    private static void finishInstant(ServerPlayer player, AbstractSpell spell) {
        var magic = MagicData.getPlayerMagicData(player);
        spell.castSpell(player.level(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void adjustmentCanBeDisabledWithoutDeletingStaff(GameTestHelper helper) {
        try (var settings = Settings.defaults()) {
            var player = player(helper, "echo_adjustment");
            var stack = player.getMainHandItem();
            var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
            var staff = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
            helper.assertFalse(rifle.trySetCalibrationAdjustment(stack, 1, staff, player.registryAccess()), "Duplicate staff must fail");
            enabled().set(false);
            helper.assertTrue(FullautoEchoCasting.hasStaff(stack, player.registryAccess()), "Disabling must preserve the stored staff");
            helper.assertFalse(rifle.getCalibrationAdjustmentProfile(stack).rules().stream().anyMatch(rule -> rule.accepts(staff)),
                    "Disabled adjustment must be absent from the profile");
            helper.assertFalse(rifle.trySetCalibrationAdjustment(stack, 1, staff, player.registryAccess()), "Disabled staff must not be insertable");
            helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY, player.registryAccess()), "Disabled staff must remain removable");
            enabled().set(true);
            helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 2, staff, player.registryAccess()), "Re-enabling must restore acceptance");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void allSpellsPayEchoManaAndInsufficientManaCannotCast(GameTestHelper helper) {
        var missile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var shock = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
        try (var settings = Settings.defaults();
             var profiles = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(missile.getSpellResource(), MulticastEchoStaffAttackProfile.DEFAULT))) {
            var player = player(helper, "echo_mana");
            var magic = MagicData.getPlayerMagicData(player);
            for (var spell : List.of(missile, shock)) {
                var cost = spell.getManaCost(1);
                magic.setMana(cost * 2 - 1);
                helper.assertFalse(start(player, spell), "Both supported and unsupported spells must require doubled mana");
                helper.assertTrue(magic.getMana() == cost * 2 - 1, "Rejected cast must not consume mana");
                magic.setMana(1000);
                helper.assertTrue(start(player, spell), "Funded spell must start");
                finishInstant(player, spell);
                helper.assertTrue(magic.getMana() == 1000 - cost * 2, "Every spell must pay doubled mana exactly once");
            }
            multiplier("ManaCost").set(2.5);
            magic.setMana(1000);
            helper.assertTrue(start(player, shock), "Fractional cost cast must start");
            finishInstant(player, shock);
            helper.assertTrue(magic.getMana() == 1000 - Math.ceil(shock.getManaCost(1) * 2.5), "Fractional cost must round up");
            enabled().set(false);
            magic.setMana(1000);
            helper.assertTrue(start(player, shock), "Disabled adjustment must allow normal casting");
            finishInstant(player, shock);
            helper.assertTrue(magic.getMana() == 1000 - shock.getManaCost(1), "Disabled adjustment must remove the mana penalty");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            enabled().set(true);
            try (var hood = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(1, true)) {
                magic.setMana(1000);
                helper.assertTrue(start(player, shock), "Hood cast must start");
                finishInstant(player, shock);
                helper.assertTrue(magic.getMana() == 1000, "Hood mana exemption must survive the multiplier");
            }
            helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(30), p -> p.getOwner() == player).forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void firstAndRepeatedShotsBypassIframesWithIndependentDamageConfig(GameTestHelper helper) {
        try (var settings = Settings.defaults();
             var staffSettings = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(false, 9)) {
            var player = player(helper, "echo_damage");
            var target = spawnPositionedZombie(helper.getLevel(), player.position().add(0, 0, 2));
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            target.setHealth(1000);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
            try {
                float[] damage = new float[3];
                for (int i = 0; i < 3; i++) {
                    enabled().set(i != 0);
                    target.invulnerableTime = i == 0 ? 0 : 20;
                    float before = target.getHealth();
                    helper.assertTrue(start(player, spell), "Shock must start");
                    finishInstant(player, spell);
                    damage[i] = before - target.getHealth();
                }
                helper.assertTrue(damage[0] > 0 && Math.abs(damage[1] - damage[0] * 0.5F) < 0.02F
                        && Math.abs(damage[2] - damage[1]) < 0.02F, "First and later echo hits must deal half damage through existing iframes");
                multiplier("Damage").set(0.5);
                target.invulnerableTime = 20;
                float before = target.getHealth();
                helper.assertTrue(start(player, spell), "Adjusted damage cast must start");
                finishInstant(player, spell);
                helper.assertTrue(Math.abs(before - target.getHealth() - damage[0] * 0.25F) < 0.02F,
                        "Rifle multiplier must combine with the profile, independently of staff settings");
            } finally {
                target.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void delayedProjectileRetainsOriginAfterSwitchingItems(GameTestHelper helper) {
        try (var settings = Settings.defaults();
             var staffSettings = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(false, 9)) {
            var player = player(helper, "echo_projectile");
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            helper.assertTrue(start(player, spell), "Missile must start");
            finishInstant(player, spell);
            var projectiles = helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(30), p -> p.getOwner() == player);
            helper.assertTrue(!projectiles.isEmpty(), "Missile must create tracked projectiles");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var source = CombatTools.getDamageSource(helper.getLevel(), projectiles.getFirst(), player, DamageTypes.SHOCK);
            helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(player, 8, source).baseAmount() == 4,
                    "Delayed projectile must retain rifle configuration after switching items");
            enabled().set(false);
            helper.assertTrue(MulticastEchoStaffAttackHandler.adjustCombatDamage(player, 8, source).baseAmount() == 8,
                    "Disabling must stop adjustments on tracked projectiles");
            projectiles.forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void echoPreservesFreeRecastsAndCreativeExemption(GameTestHelper helper) {
        boolean oldCreativeCost = ServerConfigs.CREATIVE_MANA_COST.get();
        try (var settings = Settings.defaults()) {
            var player = player(helper, "echo_free_cast");
            var magic = MagicData.getPlayerMagicData(player);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get();
            magic.getPlayerRecasts().addRecast(new RecastInstance(spell.getSpellId(), 1, 2, 100, CastSource.SWORD, null), magic);
            magic.setMana(0);
            player.getInventory().items.stream().filter(stack -> stack.is(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()))
                    .forEach(stack -> stack.setCount(0));
            helper.assertTrue(start(player, spell), "Recasts must remain possible without mana or ammunition");
            finishInstant(player, spell);
            helper.assertTrue(magic.getMana() == 0, "Recasts must not acquire a mana cost");
            helper.assertFalse(FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), player.getMainHandItem(), spell),
                    "Recast context must be cleared after the actual cast, not before its effects");
            // 別の魔法を使い、recast免除ではなくCreative免除を検証する。
            var missile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ServerConfigs.CREATIVE_MANA_COST.set(false);
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            helper.assertTrue(start(player, missile), "Creative exemption must bypass the increased mana gate");
            finishInstant(player, missile);
            helper.assertTrue(magic.getMana() == 0, "Creative exemption must prevent actual mana consumption");
            helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(30),
                    projectile -> projectile.getOwner() == player).forEach(Entity::discard);
        } finally {
            ServerConfigs.CREATIVE_MANA_COST.set(oldCreativeCost);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void configSyncPreservesDisplayPercentagesAndResets(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        boolean previousEnabled = FullautoEchoConfigState.enabled();
        double previousMultiplier = FullautoEchoConfigState.manaMultiplier();
        try {
            SyncFullautoEchoConfigPacket.STREAM_CODEC.encode(buffer, new SyncFullautoEchoConfigPacket(true, 2.5));
            var decoded = SyncFullautoEchoConfigPacket.STREAM_CODEC.decode(buffer);
            FullautoEchoConfigState.set(decoded.enabled(), decoded.manaMultiplier());
            helper.assertTrue(FullautoEchoConfigState.enabled() && FullautoEchoConfigState.manaMultiplier() == 2.5,
                    "Sync must preserve enabled state and fractional multiplier");
            var increase = (TranslatableContents) CalibrationAdjustmentEffects.gainEchoCast(FullautoEchoConfigState.manaMultiplier()).get(1).getContents();
            helper.assertTrue(((Number) increase.getArgs()[0]).longValue() == 150, "250 percent total must display as +150 percent in JEI");
            FullautoEchoConfigState.reset();
            helper.assertFalse(FullautoEchoConfigState.enabled(), "Disconnect must hide the adjustment until the next sync");
            helper.assertTrue(FullautoEchoConfigState.manaMultiplier() == 2, "Disconnect must clear the previous server multiplier");
        } finally {
            buffer.release();
            FullautoEchoConfigState.set(previousEnabled, previousMultiplier);
        }
        helper.succeed();
    }
}
