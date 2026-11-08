package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.assertCleared;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.assertUnspent;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongTargetGameTests {
    private ElementalBowLongTargetGameTests() {}

    // Forge 1.20.1 の通常地形で地下に埋まらないよう、空中検証は相対高度を220上げる。
    static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void lunarAimLocksAtDrawAndHoldsUntilRelease(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.LUNAR_AIM.get(), InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void sacredArrowLocksAtDrawInOffhand(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void offhandHotbarChangesPreserveTargetAndDrawTime(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void arrowVolleyPreservesPreconditionTarget(GameTestHelper helper) {
        holdsTarget(helper, io.redspace.ironsspellbooks.api.registry.SpellRegistry.ARROW_VOLLEY_SPELL.get(), InteractionHand.MAIN_HAND);
    }

    static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand) {
        holdsTarget(h, spell, hand, false);
    }

    static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand, boolean changeHotbar) {
        var player = player(h, hand, spell);
        var stack = player.getItemInHand(hand);
        var magic = MagicData.getPlayerMagicData(player);
        var target = EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.position().add(0, 0, 5));
        target.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT.get(), 200));
        h.getLevel().addFreshEntity(target);
        begin(h, player, hand);
        var captured = magic.getAdditionalCastData();
        h.assertTrue(captured != null, "Drawing must create targeting data before release");
        if (captured instanceof LunarAimCastData lunar) h.assertTrue(target.getUUID().equals(lunar.targetId()), "Lunar Aim must lock at draw start");
        if (captured instanceof SacredArrowCastData sacred) h.assertTrue(target.getUUID().equals(sacred.targetId()), "Sacred Arrow must lock at draw start");
        player.setYRot(180);
        int ticks = ElementalBow.resolveMagicRequiredDrawTicks(stack) + 10;
        long startedAt = h.getLevel().getGameTime();
        // FakePlayer は world のプレイヤー一覧に入れず、実際の server tick ごとに弓の tick を駆動する。
        for (int i = 1; i < ticks; i++) h.runAfterDelay(i, () -> {
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                // 引き始めと異なるスロットのまま保持・発射し、詠唱時間や対象がリセットされないことを確認する。
                if (changeHotbar) player.getInventory().selected = 3 + (int) (h.getLevel().getGameTime() % 2);
                ElementalBowPendingCast.tick(player);
                h.assertTrue(magic.isCasting() && magic.getAdditionalCastData() == captured,
                        "Holding must preserve the original cast without automatic completion");
                h.assertTrue(magic.getCastDurationRemaining() == Math.max(0,
                                ElementalBow.resolveMagicRequiredDrawTicks(stack) - (h.getLevel().getGameTime() - startedAt)),
                        "Holding must preserve elapsed draw time across hotbar changes");
                assertUnspent(h, player, stack);
            }
        });
        h.runAfterDelay(ticks, () -> {
            int[] casts = {0};
            Consumer<SpellOnCastEvent> listener = event -> {
                if (event.getEntity() != player) return;
                casts[0]++;
                h.assertTrue(magic.getAdditionalCastData() == captured, "Release must not reacquire the target");
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                int elapsed = (int) (h.getLevel().getGameTime() - startedAt);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration() - elapsed);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration() - elapsed);
                h.assertTrue(casts[0] == 1 && stack.getDamageValue() == 1, "Release must cast and damage exactly once");
                h.assertTrue(player.getInventory().getItem(2).getCount() == 2, "Release must consume exactly one arrow");
                h.assertTrue(magic.getMana() < 1000 && ElementalBowOverheatManager.getState(player).active(), "Successful release must consume mana and apply heat");
                assertCleared(h, player);
                h.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Held casting must bypass normal cooldown");
                h.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                ElementalBowPendingCast.cancel(player);
                target.discard();
            }
        });
    }

    static FakePlayer player(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(h, new BlockPos(2, 300, 2), "bow_long");
        return preparePlayer(h, hand, spell, player);
    }

    static FakePlayer preparePlayer(GameTestHelper h, InteractionHand hand, AbstractSpell spell, FakePlayer player) {
        player.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(2000);
        MagicData.getPlayerMagicData(player).setMana(1000);
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        ElementalBow.setCalibrationScroll(stack, 0, BowGameTestSupport.createSpellScroll(spell), h.getLevel().registryAccess());
        { var tag = stack.getOrCreateTag();
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(0).toString());
        }
        player.setItemInHand(hand, stack);
        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
        return player;
    }

    static void begin(GameTestHelper h, FakePlayer player, InteractionHand hand) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            h.assertTrue(player.getItemInHand(hand).getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                    "LONG bow draw must begin");
            h.assertTrue(ElementalBowPendingCast.isPending(player), "Drawing must own a pending cast");
        }
    }
}
