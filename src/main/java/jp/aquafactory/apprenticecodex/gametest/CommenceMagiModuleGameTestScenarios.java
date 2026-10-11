package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.registry.EntityRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.CommenceMagiModuleEvents;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleModifier;
import jp.aquafactory.apprenticecodex.network.packet.SyncCommenceMagiModuleConfigPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

final class CommenceMagiModuleGameTestScenarios {
    private CommenceMagiModuleGameTestScenarios() {
    }

    static void headshotStacksAfterStandardDamage(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
            for (int modules = 0; modules <= 3; modules++) {
                var profile = profile(player, modules);
                // 上流の加算・乗算、弾速減衰、散弾の威力分割の後で頭倍率を掛ける。
                profile.components().set(ShotComponents.DAMAGE, Value.of(10));
                profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(5, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
                profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(0.5, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
                profile.components().set(ShotComponents.PROJECTILE_COUNT, Value.of(3));
                double multiplier = CommenceMagiModuleServerConfig.DEFAULTS.headshotMultiplier(modules);
                assertDamage(h, player, profile, true, multiplier);
                assertDamage(h, player, profile, false, 1);
                close(h, profile.value(ShotComponents.DAMAGE), 22.5, "Headshot must not mutate shared shot damage");
            }
            var target = target(h, player);
            try {
                double halfMargin = Math.max(target.getBoundingBox().getYsize() / 3, 0.5) / 2;
                h.assertTrue(!CombatTools.isHeadShot(target, target.getEyePosition().add(0, -halfMargin, 0)), "Lower headshot boundary must be excluded");
                h.assertTrue(!CombatTools.isHeadShot(target, target.getEyePosition().add(0, halfMargin, 0)), "Upper headshot boundary must be excluded");
                h.assertTrue(CombatTools.isHeadShot(target, target.getEyePosition()), "Eye position must qualify");
            } finally { target.discard(); }
        } finally { cleanup(h, player); }
        h.succeed();
    }

    static void piercingAndPelletsKeepIndependentHits(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
            var profile = profile(player, 1);
            profile.components().set(ShotComponents.PIERCING, Value.of(3));
            var bullet = bullet(h, player, profile.copy());
            var first = target(h, player);
            var second = target(h, player);
            try {
                float base = bullet.resolveDamage();
                bullet.hit(new EntityHitResult(first, first.getEyePosition()));
                close(h, 1000 - first.getHealth(), base * 2, "First piercing target must receive headshot bonus");
                bullet.hit(new EntityHitResult(second, second.position().add(0, 0.3, 0)));
                close(h, 1000 - second.getHealth(), base, "Body hit after piercing must not inherit headshot bonus");
                assertDamage(h, player, profile.copy(), false, 1);
                var other = target(h, player);
                try {
                    other.hurt(other.damageSources().indirectMagic(bullet, player), base);
                    close(h, 1000 - other.getHealth(), base, "Secondary damage must not receive headshot bonus");
                    second.invulnerableTime = 0;
                    second.hurt(DamageSources.bullet(h.getLevel(), bullet, player), base);
                    close(h, 1000 - second.getHealth(), base * 2, "Later bullet damage must not reuse consumed impact");
                } finally { other.discard(); }
            } finally { bullet.discard(); first.discard(); second.discard(); }
        } finally { cleanup(h, player); }
        h.succeed();
    }

    static void insufficientManaCancelsBeforeSideEffects(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
            var gun = equip(player, 2);
            mana(player, 29);
            for (int i = 0; i < 2; i++) {
                h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EVENT_CANCELLED, "Insufficient mana must cancel normal firing");
                h.assertTrue(MagazineContents.get(gun).count() == 5 && FireDelayState.remaining(player) == 0, "Canceled shot must preserve ammo and fire delay");
                h.assertTrue(bullets(h, player).isEmpty(), "Canceled shot must not spawn bullets");
                close(h, MagicData.getPlayerMagicData(player).getMana(), 29, "Canceled shot must preserve mana");
                h.assertTrue(player.lastActionbar != null && player.lastActionbar.getContents() instanceof TranslatableContents error
                        && error.getKey().equals("ui.apprenticecodex.irons_artifice.insufficient_mana")
                        && player.lastActionbar.getStyle().getColor().getValue() == ChatFormatting.RED.getColor(),
                        "Insufficient mana must display the translated red actionbar error");
            }
            equip(player, 0);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Unmodified gun must still fire");
            close(h, MagicData.getPlayerMagicData(player).getMana(), 29, "Unmodified gun must not consume mana");
        } finally { cleanup(h, player); }
        h.succeed();
    }

    static void manaChargedOnceAndFiredProfileSurvivesChanges(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
            var gun = equip(player, 2);
            var container = new GunContainer(gun);
            container.setItem(2, new ItemStack(artificeItem("scattershot_modifier")));
            mana(player, 60);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "First shot must fire");
            close(h, MagicData.getPlayerMagicData(player).getMana(), 30, "Pellets and duplicate modules must charge only once");
            h.assertTrue(bullets(h, player).size() > 1, "Scattershot must produce multiple bullets");
            FireDelayState.clear(player);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Exactly enough mana must permit another shot");
            close(h, MagicData.getPlayerMagicData(player).getMana(), 0, "Second shot must exhaust mana");
            var firedProfile = bullets(h, player).getFirst().getProfile();
            container.removeItemNoUpdate(0);
            container.removeItemNoUpdate(1);
            container.setChanged();
            try (var changed = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(new Values(0, 0, 0))) {
                assertDamage(h, player, firedProfile, true, 2.25);
            }
            FireDelayState.clear(player);
            equip(player, 1);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EVENT_CANCELLED, "Exhausted mana must block subsequent shots");
        } finally { cleanup(h, player); }
        h.succeed();
    }

    static void freeShotsAndExemptShooters(GameTestHelper h) throws Exception {
        var player = player(h);
        var mob = Objects.requireNonNull(EntityType.ZOMBIE.create(h.getLevel()));
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(new Values(150, 40, 0))) {
            equip(player, 2);
            mana(player, 0);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Zero mana cost must allow firing");
            assertDamage(h, player, profile(player, 2), true, 2.9);
            FireDelayState.clear(player);
            try (var paid = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
                player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Creative players must be exempt");
                close(h, MagicData.getPlayerMagicData(player).getMana(), 0, "Creative firing must not consume mana");
                mob.setPos(player.position().add(2, 0, 0));
                mob.setNoAi(true);
                mob.setItemInHand(InteractionHand.MAIN_HAND, gun(1));
                h.getLevel().addFreshEntity(mob);
                h.assertTrue(GunplayManager.tryFire(mob, new Vec3(0, 0, 1)).fired(), "Mobs must be exempt from mana requirements");
                var mobBullets = h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(mob.position(), 20, 20, 20), b -> b.getOwner() == mob);
                h.assertTrue(!mobBullets.isEmpty() && mobBullets.getFirst().getProfile().peek(CommenceMagiModuleModifier.HEADSHOT_MULTIPLIER) == 2, "Mob bullets must retain the headshot modifier");
                mobBullets.forEach(Bullet::discard);
            }
        } finally { mob.discard(); cleanup(h, player); }
        h.succeed();
    }

    static void canceledShotAndImpactHaveNoEffect(GameTestHelper h) throws Exception {
        var player = player(h);
        Consumer<GunAboutToShootEvent> cancelShot = event -> { if (event.getEntity() == player) event.setCanceled(true); };
        Consumer<BulletImpactEvent> cancelImpact = event -> { if (event.getBullet().getOwner() == player) event.setCanceled(true); };
        try (var ignored = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(CommenceMagiModuleServerConfig.DEFAULTS)) {
            var gun = equip(player, 1);
            mana(player, 60);
            MagazineContents.set(gun, MagazineContents.EMPTY);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EMPTY_MAGAZINE, "Empty magazine must prevent firing");
            close(h, MagicData.getPlayerMagicData(player).getMana(), 60, "Empty magazine must not consume mana");
            MagazineContents.set(gun, new MagazineContents(5));
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancelShot);
            try {
                h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EVENT_CANCELLED, "External cancellation must prevent firing");
                close(h, MagicData.getPlayerMagicData(player).getMana(), 60, "External cancellation must not consume mana");
            } finally { NeoForge.EVENT_BUS.unregister(cancelShot); }
            var bullet = bullet(h, player, profile(player, 1));
            var target = target(h, player);
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancelImpact);
            try {
                bullet.hit(new EntityHitResult(target, target.getEyePosition()));
                close(h, target.getHealth(), 1000, "Canceled impact must not hurt target");
                h.assertTrue(bullet.getProfile().peek(CommenceMagiModuleEvents.IMPACT) == null, "Canceled impact must leave no headshot state");
            } finally { NeoForge.EVENT_BUS.unregister(cancelImpact); bullet.discard(); target.discard(); }
        } finally { cleanup(h, player); }
        h.succeed();
    }

    static void tooltipAndConfigSyncPreserveValues(GameTestHelper h) {
        var previous = CommenceMagiModuleConfigState.values();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var values = new Values(150, 40, 7);
            SyncCommenceMagiModuleConfigPacket.STREAM_CODEC.encode(buffer, new SyncCommenceMagiModuleConfigPacket(values));
            var decoded = SyncCommenceMagiModuleConfigPacket.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.values().equals(values), "Config packet must preserve all values");
            CommenceMagiModuleConfigState.set(decoded.values());
            var lines = tooltip();
            h.assertTrue(lines.size() == 5 && lines.get(1).getContents() instanceof TranslatableContents heading
                    && heading.getKey().equals("irons_artifice.tooltip.when_used_as_modifier")
                    && lines.get(1).getStyle().getColor().getValue() == ChatFormatting.GRAY.getColor(), "Modifier must use the standard heading");
            h.assertTrue(lines.get(2).getString().startsWith(" ") && lines.get(2).getSiblings().getFirst().getContents() instanceof TranslatableContents initial
                    && initial.getArgs()[0].equals(150), "Initial bonus must be indented and use synced config");
            h.assertTrue(lines.get(3).getString().startsWith(" ") && lines.get(3).getSiblings().getFirst().getContents() instanceof TranslatableContents additional
                    && additional.getArgs()[0].equals(40), "Additional bonus must use synced config");
            h.assertTrue(lines.get(2).getSiblings().getFirst().getStyle().getColor().getValue() == ChatFormatting.GREEN.getColor(), "Bonus description must be green");
            h.assertTrue(lines.get(4).getSiblings().getFirst().getStyle().getColor().getValue() == ChatFormatting.RED.getColor(), "Mana description must be red");
            CommenceMagiModuleConfigState.set(new Values(150, 40, 0));
            h.assertTrue(tooltip().size() == 4, "Zero cost must hide the mana description");
            CommenceMagiModuleConfigState.reset();
            h.assertTrue(CommenceMagiModuleConfigState.values().equals(CommenceMagiModuleServerConfig.DEFAULTS), "Logout reset must restore defaults");
        } finally { buffer.release(); CommenceMagiModuleConfigState.set(previous); }
        h.succeed();
    }

    private static List<Component> tooltip() {
        var lines = new ArrayList<Component>();
        ItemRegistry.COMMENCE_MAGI_MODULE.get().appendHoverText(new ItemStack(ItemRegistry.COMMENCE_MAGI_MODULE.get()), Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        return lines;
    }

    private static TestPlayer player(GameTestHelper h) {
        var player = new TestPlayer(h.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 30, 1)));
        h.getLevel().addFreshEntity(player);
        return player;
    }

    private static void mana(FakePlayer player, float amount) {
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA)).setBaseValue(1000);
        MagicData.getPlayerMagicData(player).setMana(amount);
    }

    private static ItemStack gun(int modules) {
        var gun = new ItemStack(artificeItem("flintlock"));
        MagazineContents.set(gun, new MagazineContents(5));
        var slots = new GunContainer(gun);
        for (int i = 0; i < modules; i++) slots.setItem(i, new ItemStack(ItemRegistry.COMMENCE_MAGI_MODULE.get()));
        return gun;
    }

    private static ItemStack equip(FakePlayer player, int modules) {
        var gun = gun(modules);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return gun;
    }

    private static ShotProfile profile(FakePlayer player, int modules) {
        var gun = equip(player, modules);
        return GunplayManager.compose(player, ((GunItem) gun.getItem()).getGun(), gun);
    }

    private static Item artificeItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", path));
    }

    private static Cow target(GameTestHelper h, LivingEntity shooter) {
        var cow = Objects.requireNonNull(EntityType.COW.create(h.getLevel()));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPos(shooter.position().add(0, 0, 5));
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
        cow.setHealth(1000);
        h.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static TestBullet bullet(GameTestHelper h, LivingEntity player, ShotProfile profile) {
        var bullet = new TestBullet(h.getLevel());
        bullet.setOwner(player);
        bullet.applyProfile(profile);
        bullet.setPos(player.position());
        bullet.setDeltaMovement(new Vec3(0, 0, 1));
        return bullet;
    }

    private static void assertDamage(GameTestHelper h, FakePlayer player, ShotProfile profile, boolean headshot, double multiplier) {
        var target = target(h, player);
        var bullet = bullet(h, player, profile);
        try {
            double expected = bullet.resolveDamage() * multiplier;
            bullet.hit(new EntityHitResult(target, headshot ? target.getEyePosition() : target.position().add(0, 0.3, 0)));
            close(h, 1000 - target.getHealth(), expected, "Direct hit damage must match the configured headshot multiplier");
        } finally { bullet.discard(); target.discard(); }
    }

    private static List<Bullet> bullets(GameTestHelper h, LivingEntity player) {
        return h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 40, 40, 40), bullet -> bullet.getOwner() == player);
    }

    private static void cleanup(GameTestHelper h, FakePlayer player) {
        bullets(h, player).forEach(Bullet::discard);
        player.discard();
    }

    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected=" + expected + " actual=" + actual);
    }

    private static final class TestPlayer extends FakePlayer {
        private Component lastActionbar;

        private TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "magi_module_test"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionbar) {
            if (actionbar) lastActionbar = message;
        }
    }

    // 保護メソッドへの入口だけを公開し、上流のイベント・本体damage・命中効果を全て実行する。
    private static final class TestBullet extends Bullet {
        private TestBullet(ServerLevel level) {
            super(EntityRegistry.BULLET.get(), level);
        }

        private void hit(HitResult result) {
            super.onHit(result);
        }
    }
}
