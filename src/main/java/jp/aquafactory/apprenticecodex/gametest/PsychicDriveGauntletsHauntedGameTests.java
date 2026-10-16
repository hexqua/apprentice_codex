package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // optional MODの実効果をserver側で検証する。
public final class PsychicDriveGauntletsHauntedGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private PsychicDriveGauntletsHauntedGameTests() {}

    private static Holder<Enchantment> haunted(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(MalumCompatibility.HAUNTED).orElseThrow();
    }

    private static FakePlayer player(GameTestHelper helper, InteractionHand hand, int level) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_haunted");
        var stack = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
        if (level > 0) stack.enchant(haunted(helper), level);
        player.setItemInHand(hand, stack);
        // worldへ追加しないFakePlayerでは装備更新の代わりに主手の実効値を与える。
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(hand == InteractionHand.MAIN_HAND ? 4 : 1);
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        player.resetAttackStrengthTicker();
        return player;
    }

    private static Cow target(GameTestHelper helper, FakePlayer player) {
        var cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        cow.setPos(PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 1.2));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(100);
        cow.setHealth(100);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static PsychicDriveGauntletsArm launch(FakePlayer player, InteractionHand hand, float charge) {
        var arm = new PsychicDriveGauntletsArm(true);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        arm.launch(player, start.add(0, 0, 6), 1, 0, hand, charge);
        return arm;
    }

    @GameTest(template = TEMPLATE)
    public static void hauntedUsesWeaponSnapshotOncePerHitInEitherHand(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) { helper.succeed(); return; }
        for (var hand : InteractionHand.values()) {
            for (float charge : new float[] {0, 1}) {
                var player = player(helper, hand, 2);
                var cow = target(helper, player);
                var sources = new ArrayList<DamageSource>();
                Consumer<LivingIncomingDamageEvent> listener = event -> {
                    if (event.getEntity() == cow && event.getSource().is(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS_MAGIC)) {
                        sources.add(event.getSource());
                        helper.assertTrue(event.getAmount() == 2, "Haunted must use the launched level without attack cooldown scaling");
                    }
                };
                NeoForge.EVENT_BUS.addListener(listener);
                try {
                    var arm = launch(player, hand, charge);
                    // 同一stackへの付与変更は攻撃を取り消さず、飛行中のsnapshotの独立性を検証できる。
                    player.getItemInHand(hand).enchant(haunted(helper), 1);
                    var main = player.getMainHandItem();
                    var off = player.getOffhandItem();
                    var mainBefore = main.copy();
                    var offBefore = off.copy();
                    for (int tick = 1; tick <= 18; tick++) arm.tick(player, 1, tick);
                    int hits = charge == 1 ? 4 : 1;
                    helper.assertTrue(sources.size() == hits && Math.abs(cow.getHealth() - (100 - hits * 6)) < 1.0e-5,
                            "Each physical hit must add exactly one Haunted bonus, hand=" + hand + ", charge=" + charge);
                    for (var source : sources) {
                        helper.assertTrue(source.getEntity() == player && source.getDirectEntity() == player
                                        && source.getSourcePosition() != null && !source.getSourcePosition().equals(player.position()),
                                "Haunted must preserve attacker attribution and contact position");
                    }
                    helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off
                                    && ItemStack.matches(mainBefore, main) && ItemStack.matches(offBefore, off),
                            "Haunted must never mutate either equipped item");
                } finally {
                    NeoForge.EVENT_BUS.unregister(listener);
                    cow.discard();
                    player.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedPhysicalHitDoesNotApplyHaunted(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) { helper.succeed(); return; }
        var player = player(helper, InteractionHand.MAIN_HAND, 2);
        var cow = target(helper, player);
        int[] magicHits = {0};
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() != cow) return;
            if (event.getSource().is(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS)) event.setCanceled(true);
            if (event.getSource().is(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS_MAGIC)) magicHits[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var arm = launch(player, InteractionHand.MAIN_HAND, 1);
            for (int tick = 1; tick <= 18; tick++) arm.tick(player, 1, tick);
            helper.assertTrue(cow.getHealth() == 100 && magicHits[0] == 0,
                    "A rejected physical hit must not bypass protection through Haunted");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            cow.discard();
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandAttackDoesNotBorrowMainhandHaunted(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) { helper.succeed(); return; }
        var player = player(helper, InteractionHand.OFF_HAND, 0);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(haunted(helper), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        // 主手の補正を副手計算で除去できるよう、基礎値へ合算せず装備modifierとして再現する。
        var damage = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE));
        sword.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) damage.addOrUpdateTransientModifier(modifier);
        });
        var cow = target(helper, player);
        try {
            var arm = launch(player, InteractionHand.OFF_HAND, 0);
            for (int tick = 1; tick <= 18; tick++) arm.tick(player, 1, tick);
            helper.assertTrue(Math.abs(cow.getHealth() - 96) < 1.0e-5,
                    "An unenchanted offhand gauntlet must not use another weapon's Haunted: health=" + cow.getHealth());
        } finally {
            cow.discard();
            player.discard();
        }
        helper.succeed();
    }
}
