package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.on_hit_handlers.FrozenShrapnelOnHit;
import io.redspace.irons_artifice.registry.EntityRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeClientTooltips;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeManaEvents;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.SpellboundArmsModuleEvents;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
import jp.aquafactory.apprenticecodex.network.packet.SyncSpellboundArmsModuleConfigPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

final class SpellboundArmsModuleGameTestScenarios {
    private SpellboundArmsModuleGameTestScenarios() {}

    static void powerStacksAndSchoolSelection(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(SpellboundArmsModuleServerConfig.DEFAULTS)) {
            attribute(player, AttributeRegistry.SPELL_POWER, 1.25);
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 1.5);
            attribute(player, AttributeRegistry.ICE_SPELL_POWER, 2);
            for (int count = 0; count <= 3; count++) {
                var gun = gun(count, 0, true);
                var profile = compose(player, gun);
                // Scattershot自身の威力倍率を先に計算し、装着数による倍率は一度だけ掛ける。
                double base = profile.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 1.25;
                close(h, profile.value(ShotComponents.DAMAGE), base * (count == 0 ? 1 : 1 + 1.5 * (0.5 + (count - 1) * 0.15)), "Composed damage must include the diminished spell bonus and other modifiers");
                if (count > 0) h.assertTrue(profile.peek(SpellboundArmsModuleEvents.POWER).school() == SchoolRegistry.ICE.get(), "Highest school must be selected");
            }
            attribute(player, AttributeRegistry.ICE_SPELL_POWER, 1.5);
            h.assertTrue(SpellboundArmsModuleEvents.power(player).school() == SchoolRegistry.FIRE.get(), "School ties must be ordered by identifier");
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 1);
            attribute(player, AttributeRegistry.ICE_SPELL_POWER, 0.75);
            h.assertTrue(SpellboundArmsModuleEvents.power(player).school() == null, "No school at or below neutral must be selected");
            close(h, SpellboundArmsModuleEvents.power(player).multiplier(), 1.25, "General power must work without school");
            attribute(player, AttributeRegistry.SPELL_POWER, 1);
            var single = compose(player, gun(1, 0, false));
            var stacked = compose(player, gun(3, 0, false));
            close(h, single.value(ShotComponents.DAMAGE), stacked.value(ShotComponents.DAMAGE), "Neutral power must not grant a stacking damage bonus");
            h.assertTrue(IronsArtificeManaEvents.cost(single, false) == IronsArtificeManaEvents.cost(stacked, false),
                    "Stacking must not independently multiply mana cost");
            attribute(player, AttributeRegistry.SPELL_POWER, 0);
            try (var ignoredZeroDamage = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(new Values(1, 0, 1, 0.25))) {
                var zero = compose(player, gun(1, 0, true));
                close(h, zero.value(ShotComponents.DAMAGE), 0, "Full power penalty must permit a zero damage multiplier");
                h.assertTrue(IronsArtificeManaEvents.cost(zero, false) == (int) Math.ceil(zero.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 1.25),
                        "Zero final damage must preserve the normal mana portion without division");
            }
        } finally { player.discard(); }
        h.succeed();
    }

    static void resistanceAndFiredSnapshot(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(SpellboundArmsModuleServerConfig.DEFAULTS)) {
            attribute(player, AttributeRegistry.SPELL_POWER, 1.25);
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 2);
            var profile = compose(player, gun(2, 1, false));
            double composed = profile.value(ShotComponents.DAMAGE);
            attribute(player, AttributeRegistry.SPELL_POWER, 4);
            attribute(player, AttributeRegistry.ICE_SPELL_POWER, 4);
            h.assertTrue(profile.peek(SpellboundArmsModuleEvents.POWER).school() == SchoolRegistry.FIRE.get(), "Fired school must survive attribute changes");
            var bullet = bullet(h, player, profile.copy());
            var target = target(h, player);
            try {
                attribute(target, AttributeRegistry.SPELL_RESIST, 1.5);
                attribute(target, AttributeRegistry.FIRE_MAGIC_RESIST, 1.5);
                double resist = DamageSources.getResist(target, SchoolRegistry.FIRE.get());
                double damage = bullet.resolveDamage();
                bullet.hit(new EntityHitResult(target, target.getEyePosition()));
                close(h, 1000 - target.getHealth(), damage * resist * 2, "Headshot must follow one school resistance application");
                for (DamageSource source : List.of(target.damageSources().indirectMagic(bullet, player),
                        target.damageSources().explosion(bullet, player),
                        io.redspace.irons_artifice.damage.DamageSources.soul(h.getLevel(), bullet, player))) {
                    target.setHealth(1000);
                    target.invulnerableTime = 0;
                    target.hurt(source, 10);
                    close(h, 1000 - target.getHealth(), 10 * resist,
                            "Each secondary damage must receive resistance exactly once: " + source.getMsgId());
                }
                // 貫通の後続対象・散弾profileへ前の対象の耐性を持ち込まない。
                var other = target(h, player);
                try {
                    double otherResistance = DamageSources.getResist(other, SchoolRegistry.FIRE.get());
                    bullet.hit(new EntityHitResult(other, other.position().add(0, 0.3, 0)));
                    close(h, 1000 - other.getHealth(), damage * otherResistance, "Another hit must use its own target resistance");
                } finally { other.discard(); }
                close(h, profile.value(ShotComponents.DAMAGE), composed, "Resistance must not mutate shared damage");
            } finally { bullet.discard(); target.discard(); }
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 1);
            attribute(player, AttributeRegistry.ICE_SPELL_POWER, 1);
            var general = compose(player, gun(1, 0, false));
            var plainBullet = bullet(h, player, general);
            var plainTarget = target(h, player);
            try {
                attribute(plainTarget, AttributeRegistry.SPELL_RESIST, 2);
                attribute(plainTarget, AttributeRegistry.FIRE_MAGIC_RESIST, 4);
                plainTarget.hurt(io.redspace.irons_artifice.damage.DamageSources.bullet(h.getLevel(), plainBullet, player), 10);
                close(h, 1000 - plainTarget.getHealth(), 10 * DamageSources.getResist(plainTarget, null), "Null school must use only general resistance");
            } finally { plainBullet.discard(); plainTarget.discard(); }
        } finally { player.discard(); }
        h.succeed();
    }

    static void combinedManaIsAtomic(GameTestHelper h) throws Exception {
        var player = player(h);
        try (var ignored = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(new Values(0.5, 0.15, 0.1001, 0.25));
             var ignoredCommence = ApprenticeCodexServerConfig.overrideCommenceMagiModuleForGameTest(new CommenceMagiModuleServerConfig.Values(100, 25, 7))) {
            attribute(player, AttributeRegistry.SPELL_POWER, 2.5);
            var gun = gun(2, 2, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, gun);
            var profile = compose(player, gun);
            double base = profile.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 1.25;
            int count = Math.max(1, (int) Math.round(profile.value(ShotComponents.PROJECTILE_COUNT)));
            double spellboundCost = base * 0.1001 + base * 1.5 * 0.65 * 0.25;
            int cost = (int) Math.ceil(spellboundCost + 7);
            h.assertTrue(IronsArtificeManaEvents.cost(profile, false) == cost, "Combined mana must round up once after summation");
            profile.modifyValue(ShotComponents.PROJECTILE_COUNT, new ValueModifier(10, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
            h.assertTrue(IronsArtificeManaEvents.cost(profile, false) == cost, "Projectile count must not affect firing mana cost");
            // 通常部分にも他modifierを反映する。Commenceの固定消費は威力によって増やさない。
            profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(100, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
            h.assertTrue(IronsArtificeManaEvents.cost(profile, false) == (int) Math.ceil(spellboundCost * 101 + 7), "Other damage modifiers must affect normal and magical mana portions");
            var noCommence = compose(player, gun(2, 0, true));
            h.assertTrue(IronsArtificeManaEvents.cost(noCommence, false) == (int) Math.ceil(spellboundCost), "Commence headshot bonus must not enter damage-based mana cost");
            var sameAmount = compose(player, gun(2, 0, true));
            var ownBoost = sameAmount.peek(SpellboundArmsModuleEvents.DAMAGE_BOOST);
            sameAmount.modifyValue(ShotComponents.DAMAGE, new ValueModifier(ownBoost.amount(), ownBoost.operation(), ownBoost.type()));
            h.assertTrue(IronsArtificeManaEvents.cost(sameAmount, false) == (int) Math.ceil(spellboundCost * (1 + ownBoost.amount())), "Equal-valued damage modifiers must remain in the normal portion");
            mana(player, cost - 1);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)) == FireOutcome.EVENT_CANCELLED, "Total mana shortage must cancel the entire volley");
            close(h, MagicData.getPlayerMagicData(player).getMana(), cost - 1, "Rejected volley must not consume any mana");
            h.assertTrue(MagazineContents.get(gun).count() == 5 && FireDelayState.remaining(player) == 0 && bullets(h, player).isEmpty(), "Rejected volley must preserve ammo, delay and entities");
            h.assertTrue(player.actionbar.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("ui.apprenticecodex.irons_artifice.insufficient_mana")
                    && player.actionbar.getStyle().getColor().getValue() == ChatFormatting.RED.getColor(), "Failure must use the common red actionbar");
            mana(player, cost);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Exactly enough total mana must permit firing");
            close(h, MagicData.getPlayerMagicData(player).getMana(), 0, "Volley must charge the total once");
            h.assertTrue(bullets(h, player).size() == count, "All projectiles must spawn together");
            bullets(h, player).forEach(Bullet::discard);
            FireDelayState.clear(player);
            mana(player, cost);
            Consumer<GunAboutToShootEvent> cancel = event -> { if (event.getEntity() == player) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
            try {
                h.assertTrue(!GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "External cancellation must reject firing");
                close(h, MagicData.getPlayerMagicData(player).getMana(), cost, "External cancellation must preserve total mana");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            mana(player, 0);
            h.assertTrue(GunplayManager.tryFire(player, new Vec3(0, 0, 1)).fired(), "Creative must remain exempt from combined mana");
        } finally { bullets(h, player).forEach(Bullet::discard); player.discard(); }
        h.succeed();
    }

    static void nestedSoulDamageAndChildProfiles(GameTestHelper h) throws Exception {
        var player = player(h);
        var target = target(h, player);
        try (var ignored = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(SpellboundArmsModuleServerConfig.DEFAULTS)) {
            attribute(player, AttributeRegistry.SPELL_POWER, 1.25);
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 2);
            attribute(target, AttributeRegistry.SPELL_RESIST, 1.5);
            attribute(target, AttributeRegistry.FIRE_MAGIC_RESIST, 1.5);
            var profile = compose(player, gun(2, 0, false));
            var bullet = bullet(h, player, profile);
            double damage = bullet.resolveDamage();
            double resistance = DamageSources.getResist(target, SchoolRegistry.FIRE.get());
            profile.components().set(ShotComponents.SOUL_DAMAGE, Value.of(0.5));
            bullet.hit(new EntityHitResult(target, target.getEyePosition()));
            close(h, 1000 - target.getHealth(), damage * 1.5 * resistance,
                    "Nested soul and primary damage must each receive resistance once");
            // 上流が既に増幅済みdamageを子弾にコピーするため、命中時に魔法威力を再適用しない。
            profile.components().set(ShotComponents.SOUL_DAMAGE, Value.of(0));
            new FrozenShrapnelOnHit().onHit(h.getLevel(), bullet,
                    new EntityHitResult(target, target.getEyePosition()), new HitEntityAccumulator());
            var children = bullets(h, player).stream().filter(child -> child != bullet).toList();
            h.assertTrue(children.size() == FrozenShrapnelOnHit.SHRAPNEL_COUNT, "Frozen shrapnel must spawn child bullets");
            for (var child : children) {
                h.assertTrue(child.getProfile().peek(SpellboundArmsModuleEvents.POWER).school() == SchoolRegistry.FIRE.get(),
                        "Child bullets must inherit the fired school");
                close(h, child.getProfile().value(ShotComponents.DAMAGE), damage * FrozenShrapnelOnHit.DAMAGE_FRACTION,
                        "Child damage must retain the upstream fraction without multiplying power again");
                target.setHealth(1000);
                target.invulnerableTime = 0;
                target.hurt(io.redspace.irons_artifice.damage.DamageSources.bullet(h.getLevel(), child, player), child.resolveDamage());
                close(h, 1000 - target.getHealth(), child.resolveDamage() * resistance,
                        "Child damage must receive inherited school resistance once");
            }
        } finally { bullets(h, player).forEach(Bullet::discard); target.discard(); player.discard(); }
        h.succeed();
    }

    static void tooltipsColorsAndConfigSync(GameTestHelper h) throws Exception {
        var player = player(h);
        var previous = SpellboundArmsModuleConfigState.values();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try (var ignored = ApprenticeCodexServerConfig.overrideSpellboundArmsModuleForGameTest(new Values(0.5, 0.15, 1, 0.25))) {
            var values = new Values(0.5, 0.15, 1, 0.25);
            SyncSpellboundArmsModuleConfigPacket.STREAM_CODEC.encode(buffer, new SyncSpellboundArmsModuleConfigPacket(values));
            SpellboundArmsModuleConfigState.set(SyncSpellboundArmsModuleConfigPacket.STREAM_CODEC.decode(buffer).values());
            h.assertTrue(SpellboundArmsModuleConfigState.values().equals(values), "Config codec must preserve all four coefficients");
            var module = new ItemStack(ItemRegistry.SPELLBOUND_ARMS_MODULE.get());
            var lines = new ArrayList<Component>();
            module.getItem().appendHoverText(module, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
            var args = translatable(lines.get(2).getSiblings().getFirst()).getArgs();
            h.assertTrue(args[0].equals("100") && args[1].equals("0"), "Unavailable player must use neutral tooltip arguments");
            attribute(player, AttributeRegistry.SPELL_POWER, 1.25);
            IronsArtificeClientTooltips.onTooltip(new ItemTooltipEvent(module, player, lines, TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
            args = translatable(lines.get(2).getSiblings().getFirst()).getArgs();
            h.assertTrue(args[0].equals("112.5") && args[1].equals("3.8"), "Tooltip must show the diminished initial and additional bonus percentages");
            attribute(player, AttributeRegistry.FIRE_SPELL_POWER, 2);
            var gun = gun(1, 1, false);
            new GunContainer(gun).setItem(2, new ItemStack(artificeItem("frozen_jacket_modifier")));
            var profile = compose(player, gun);
            h.assertTrue(profile.peek(ShotComponents.MUZZLE_FLASH).tints().size() == 2, "School and other modifier muzzle tints must coexist");
            h.assertTrue(profile.peek(ShotComponents.PARTICLE_TRAIL).getParticles().size() == 2, "School trail must coexist with other modifier trail and omit default");
            var gunLines = new ArrayList<Component>();
            gunLines.add(Component.literal(" ").append(Component.translatable("irons_artifice.tooltip.ammo_capacity", 5)));
            gunLines.add(Component.translatable("irons_artifice.tooltip.modifier_count", 5));
            IronsArtificeClientTooltips.onTooltip(new ItemTooltipEvent(gun, player, gunLines, TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
            var manaLine = translatable(gunLines.get(1).getSiblings().getFirst());
            h.assertTrue(manaLine.getKey().equals("item.apprenticecodex.irons_artifice.tooltip.mana_cost"), "Mana must appear immediately after ammo capacity");
            var expected = Component.literal(Integer.toString(IronsArtificeManaEvents.cost(profile, true))).getString();
            h.assertTrue(((Component) manaLine.getArgs()[0]).getString().equals(expected), "Displayed total must match shared mana calculation");
            SpellboundArmsModuleConfigState.set(new Values(0.5, 0.15, 0, 0));
            lines.clear();
            module.getItem().appendHoverText(module, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
            h.assertTrue(lines.size() == 5, "Zero coefficient must hide only the mana description");
            SpellboundArmsModuleConfigState.reset();
            h.assertTrue(SpellboundArmsModuleConfigState.values().equals(SpellboundArmsModuleServerConfig.DEFAULTS), "Disconnect reset must restore defaults");
        } finally { buffer.release(); SpellboundArmsModuleConfigState.set(previous); player.discard(); }
        h.succeed();
    }

    static void malumExposureIncludesLethalHits(GameTestHelper h) {
        if (!ModList.get().isLoaded("malum")) { h.succeed(); return; }
        SpellboundArmsModuleMalumGameTestScenarios.run(h);
    }

    static TestPlayer player(GameTestHelper h) {
        var player = new TestPlayer(h.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 30, 1)));
        h.getLevel().addFreshEntity(player);
        return player;
    }

    private static void attribute(LivingEntity entity, Holder<Attribute> key, double value) {
        Objects.requireNonNull(entity.getAttribute(key)).setBaseValue(value);
    }

    private static void mana(FakePlayer player, float value) {
        attribute(player, AttributeRegistry.MAX_MANA, 10000);
        MagicData.getPlayerMagicData(player).setMana(value);
    }

    static ItemStack gun(int spellbound, int commence, boolean scatter) {
        var gun = new ItemStack(artificeItem("flintlock"));
        MagazineContents.set(gun, new MagazineContents(5));
        var slots = new GunContainer(gun);
        int i = 0;
        for (; i < spellbound; i++) slots.setItem(i, new ItemStack(ItemRegistry.SPELLBOUND_ARMS_MODULE.get()));
        for (int n = 0; n < commence; n++) slots.setItem(i++, new ItemStack(ItemRegistry.COMMENCE_MAGI_MODULE.get()));
        if (scatter) slots.setItem(i, new ItemStack(artificeItem("scattershot_modifier")));
        return gun;
    }

    static ShotProfile compose(LivingEntity player, ItemStack gun) {
        return GunplayManager.compose(player, ((GunItem) gun.getItem()).getGun(), gun);
    }

    private static Item artificeItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", path));
    }

    static Cow target(GameTestHelper h, LivingEntity shooter) {
        var target = Objects.requireNonNull(EntityType.COW.create(h.getLevel()));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(shooter.position().add(0, 0, 5));
        attribute(target, Attributes.MAX_HEALTH, 1000);
        target.setHealth(1000);
        h.getLevel().addFreshEntity(target);
        return target;
    }

    static TestBullet bullet(GameTestHelper h, LivingEntity shooter, ShotProfile profile) {
        var bullet = new TestBullet(h.getLevel());
        bullet.setOwner(shooter);
        bullet.applyProfile(profile);
        bullet.setPos(shooter.position());
        bullet.setDeltaMovement(new Vec3(0, 0, 1));
        h.getLevel().addFreshEntity(bullet);
        return bullet;
    }

    private static List<Bullet> bullets(GameTestHelper h, LivingEntity shooter) {
        return h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(shooter.position(), 40, 40, 40), b -> b.getOwner() == shooter);
    }

    private static TranslatableContents translatable(Component line) {
        return (TranslatableContents) line.getContents();
    }

    static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected=" + expected + " actual=" + actual);
    }

    static final class TestPlayer extends FakePlayer {
        private Component actionbar;
        private TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "spellbound_test")); }
        @Override
        public void displayClientMessage(Component message, boolean actionbar) { if (actionbar) this.actionbar = message; }
    }

    static final class TestBullet extends Bullet {
        private TestBullet(ServerLevel level) { super(EntityRegistry.BULLET.get(), level); }
        void hit(HitResult result) { super.onHit(result); }
    }
}
