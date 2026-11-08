package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport;
import jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring.SpellCastParryingRingDefenseEvent;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongTargetGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongDamageGameTests {
    private ElementalBowLongDamageGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void damagePreservesBowDrawAndRingNeverParries(GameTestHelper h) {
        var players = new ArrayList<FakePlayer>();
        var spells = List.of(SpellRegistry.LUNAR_AIM.get(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
        try (var config = useDamageTestConfig(h)) {
            for (var spell : spells) for (var hand : InteractionHand.values()) {
                var player = damageablePlayer(h, hand, spell);
                players.add(player);
                // 毒の初期時刻による中断回避と無敵時間が、被弾検証を隠さないようにする。
                player.tickCount = 10;
                BowGameTestSupport.equipCurio(player, Curios.RING_SLOT,
                        new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
                h.assertTrue(SpellCastParryingRingDefenseEvent.isEquippedBy(player),
                        "Bow damage test must equip the ring");
                var stack = player.getItemInHand(hand);
                h.assertTrue(stack.getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                        "Magic bow draw must start");
                hitBowFromFront(h, player);
                h.assertTrue(player.isUsingItem(), "Damage at draw start must preserve bow use");
                if (spell.getCastType() == CastType.LONG) {
                    h.assertTrue(ElementalBowPendingCast.isManagedCast(player), "Damage must preserve managed LONG casting");
                }
                assertUnspent(h, player, stack);
            }
        } catch (RuntimeException | Error failure) {
            players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            throw failure;
        }
        h.runAfterDelay(5, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    ElementalBowPendingCast.tick(player);
                    hitBowFromFront(h, player);
                    h.assertTrue(player.isUsingItem(), "Damage during charging must preserve bow use");
                }
            } catch (RuntimeException | Error failure) {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
                throw failure;
            }
        });
        h.runAfterDelay(30, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    var stack = player.getUseItem();
                    ElementalBowPendingCast.tick(player);
                    hitBowFromFront(h, player);
                    h.assertTrue(player.isUsingItem(), "Damage after charging must preserve bow use");
                    int[] casts = {0};
                    Consumer<SpellOnCastEvent> listener = event -> { if (event.getEntity() == player) casts[0]++; };
                    MinecraftForge.EVENT_BUS.addListener(listener);
                    try {
                        float manaBeforeRelease = MagicData.getPlayerMagicData(player).getMana();
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration() - 30);
                        h.assertTrue(casts[0] == 1 && stack.getDamageValue() == 1,
                                "Damaged bow draw must fire exactly once on release");
                        h.assertTrue(player.getInventory().getItem(2).getCount() == 2,
                                "Release must consume exactly one arrow");
                        h.assertTrue(MagicData.getPlayerMagicData(player).getMana() < manaBeforeRelease
                                        && ElementalBowOverheatManager.getState(player).active(),
                                "Release must consume mana and apply heat: mana=" + MagicData.getPlayerMagicData(player).getMana()
                                        + ", heat=" + ElementalBowOverheatManager.getState(player)
                                        + ", spell=" + ElementalBow.getDisplayedSpellProfile(stack));
                    } finally {
                        MinecraftForge.EVENT_BUS.unregister(listener);
                    }
                }
                h.succeed();
            } finally {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            }
        });
    }

    static void hitBowFromFront(GameTestHelper h, FakePlayer player) {
        var attacker = EntityType.ZOMBIE.create(h.getLevel());
        attacker.setPos(player.position().add(player.getLookAngle().scale(3)));
        player.invulnerableTime = 0;
        float health = player.getHealth();
        h.assertTrue(player.hurt(h.getLevel().damageSources().mobAttack(attacker), 2),
                "The ring must not parry a frontal hit during bow drawing");
        h.assertTrue(player.getHealth() < health, "Bow interruption protection must not prevent damage");
        attacker.discard();
    }

    @GameTest(template = TEMPLATE)
    public static void damageProtectionIgnoresSpellOverrideButRequiresBowOwnership(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            for (var hand : InteractionHand.values()) {
                var spell = new LifecycleSpell();
                var player = damageablePlayer(h, hand, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
                player.tickCount = 10;
                // レジストリ全体を変更せず、被ダメージ経路にも override を持つ同一の魔法を渡す。
                var magic = new MagicData(player) {
                    @Override public SpellData getCastingSpell() {
                        return new SpellData(spell, 1);
                    }
                };
                // Iron's 3.16.3 の Forge 版は attachment ではなく LivingEntity の追加fieldへ保持する。
                try {
                    var field = LivingEntity.class.getDeclaredField("irons_spellbooks$magicData");
                    field.setAccessible(true);
                    field.set(player, magic);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Unable to install the casting probe", exception);
                }
                magic.setMana(1000);
                var stack = player.getItemInHand(hand);
                try {
                    h.assertTrue(ElementalBowPendingCast.begin(player, stack, hand, spell, 1, 20), "Probe draw must start");
                    h.assertTrue(spell.canBeInterrupted(player), "Probe must explicitly allow interruption");
                    hitBowFromFront(h, player);
                    h.assertTrue(ElementalBowPendingCast.isManagedCast(player), "Spell override must not cancel bow drawing");
                    h.assertTrue(ElementalBowPendingCast.release(player, stack, spell, 1, 20), "Damaged probe must release");
                    h.assertTrue(spell.casts == 1 && spell.cancelled == 0, "Protected probe must cast without cancellation");
                    // 弓の所持だけでは通常詠唱の中断やリング防御を変更しない。
                    magic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                    hitBowFromFront(h, player);
                    h.assertFalse(magic.isCasting(), "Ordinary casting while holding a bow must still be interrupted");
                } finally {
                    ElementalBowPendingCast.cancel(player);
                    magic.resetCastingState();
                    player.discard();
                }
                // 同一 tick の装備検索キャッシュを避け、最初から指輪を装備した別プレイヤーで比較する。
                var defender = damageablePlayer(h, hand, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
                var defenderMagic = MagicData.getPlayerMagicData(defender);
                try {
                    defender.tickCount = 10;
                    BowGameTestSupport.equipCurio(defender, Curios.RING_SLOT,
                            new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
                    h.assertTrue(SpellCastParryingRingDefenseEvent.isEquippedBy(defender),
                            "Ordinary casting control must equip the ring");
                    defenderMagic.getSyncedData();
                    defenderMagic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                    var attacker = EntityType.ZOMBIE.create(h.getLevel());
                    attacker.setPos(defender.position().add(defender.getLookAngle().scale(3)));
                    float health = defender.getHealth();
                    defender.hurt(h.getLevel().damageSources().mobAttack(attacker), 2);
                    h.assertTrue(defender.getHealth() == health && defenderMagic.isCasting(),
                            "The ring must still parry ordinary casting while a bow is held");
                    attacker.discard();
                } finally {
                    defenderMagic.resetCastingState();
                    defender.discard();
                }
            }
        }
        h.succeed();
    }
}
