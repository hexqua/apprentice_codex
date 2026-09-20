package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class ManaShieldShellResidualGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void fractionalAbsorptionProtectsLongCasting(GameTestHelper helper) {
        verifyCasting(helper);
        helper.succeed();
    }

    private static void verifyCasting(GameTestHelper helper) {
        var spell = SpellRegistry.FIREBALL_SPELL.get();
        helper.assertTrue(spell.getCastType() == CastType.LONG, "Test spell must use LONG casting");
        for (boolean shell : new boolean[]{false, true}) {
            for (float mana : new float[]{0, 50, 75, 800}) {
                var player = player(helper, "shield_cast");
                // 生成直後(tickCount <= 1)はIron'sの未設定の毒時刻と一致し、被弾中断が抑止される。
                player.tickCount = 100;
                if (shell) {
                    equipShell(player);
                } else {
                    CuriosApi.getCuriosInventory(player).resolve().orElseThrow().setEquippedCurio(
                            CuriosSlotConstants.CHARM, 0, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
                }
                var magic = MagicData.getPlayerMagicData(player);
                magic.setMana(mana);
                magic.initiateCast(spell, 1, 40, CastSource.SPELLBOOK, "mainhand");
                helper.assertTrue(magic.isCasting() && spell.canBeInterrupted(player),
                        "Test must begin with an interruptible active cast");
                int[] downstream = {0};
                Consumer<LivingAttackEvent> listener = event -> {
                    if (event.getEntity() == player) downstream[0]++;
                };
                MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingAttackEvent.class, listener);
                float health = player.getHealth();
                try {
                    // 難易度補正のない詠唱中断対象のダメージで、端数の吸収を比較する。
                    player.hurt(player.damageSources().lava(), 0.5F);
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(listener);
                }
                boolean absorbed = mana > (shell ? 50 : 0);
                helper.assertTrue(magic.isCasting() == absorbed,
                        "Only full absorption must protect LONG casting: shell=" + shell + ", mana=" + mana
                                + ", downstream=" + downstream[0] + ", health=" + player.getHealth()
                                + ", previousHealth=" + health + ", duration=" + magic.getCastDurationRemaining()
                                + ", spell=" + magic.getCastingSpell().getSpell().getSpellId()
                                + ", interruptible=" + magic.getCastingSpell().getSpell().canBeInterrupted(player));
                helper.assertTrue(absorbed ? downstream[0] == 0 && player.getHealth() == health
                                : downstream[0] > 0 && player.getHealth() < health,
                        "Fully absorbed fractional hits must not reach downstream damage handlers");
                helper.assertTrue(magic.getMana() == Math.max(0, mana - (shell ? 75 : 25)),
                        "A fractional hit must charge one whole absorption step plus activation when applicable");
            }
        }
    }

    @GameTest(template = TEMPLATE)
    public static void shellForwardsTyrosScaleDamageAndZeroAbsorptionEndpoints(GameTestHelper helper) {
        for (float mana : new float[]{0, 49, 50, 800}) {
            var player = player(helper, "shell_ratio");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var protection = net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION;
            for (var armor : player.getArmorSlots()) armor.enchant(protection, 4);
            player.doTick();
            equipShell(player);
            MagicData.getPlayerMagicData(player).setMana(mana);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            player.setHealth(1000);
            float[] forwarded = {0};
            Consumer<LivingAttackEvent> listener = event -> {
                if (event.getEntity() == player) forwarded[0] = event.getAmount();
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingAttackEvent.class, listener);
            LivingAttackEvent event;
            try {
                event = postLivingAttackEventForGameTest(player, player.damageSources().lava(), 240);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            // 防御19・Protection IV×4では240→73.2672。800マナなら30点吸収し、残存割合を240へ掛ける。
            float expected = mana == 800 ? 240 * (73.2672F - 30) / 73.2672F : 240;
            helper.assertTrue(event.isCanceled() == (mana == 800),
                    "Forge must reenter only when Shell changes the incoming damage");
            helper.assertTrue(Math.abs(forwarded[0] - expected) < 0.001,
                    "Shell must forward the unblocked proportion: mana=" + mana + ", damage=" + forwarded[0]);
            helper.assertTrue(MagicData.getPlayerMagicData(player).getMana() == 0,
                    "This hit must exhaust the available mana");
            // 耐火のMagi Agent Suitは溶岩の通常耐久消費を受けず、Shell発動分だけを消費する。
            int residualWear = 0;
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == (mana >= 50 ? 60 : 0) + residualWear,
                    "Shell activation wear must occur even when activation leaves no absorption budget: mana=" + mana
                            + ", expected=" + ((mana >= 50 ? 60 : 0) + residualWear)
                            + ", actual=" + player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shellResidualUsesArmorProtectionAndAdditionalDurability(GameTestHelper helper) {
        var player = player(helper, "shell_residual");
        var control = player(helper, "shell_control");
        equipShell(player);
        // 発動費用と2ダメージ分を渡し、防具軽減後に残った割合を通常被弾と比較する。
        MagicData.getPlayerMagicData(player).setMana(100);
        var source = player.damageSources().lava();
        helper.assertFalse(source.is(DamageTypeTags.BYPASSES_ARMOR),
                "Test damage must enter Shell's armor mitigation branch");
        float reduced = CombatRules.getDamageAfterAbsorb(12,
                player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        reduced = CombatRules.getDamageAfterMagicAbsorb(reduced,
                EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source));
        float residual = 12 * (reduced - 2) / reduced;
        control.hurt(source, residual);
        int[] incomingCalls = {0};
        float[] forwarded = {0};
        Consumer<LivingAttackEvent> listener = event -> {
            if (event.getEntity() == player) {
                incomingCalls[0]++;
                forwarded[0] = event.getAmount();
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingAttackEvent.class, listener);
        try {
            player.hurt(source, 12);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(incomingCalls[0] == 1 && Math.abs(forwarded[0] - residual) < 0.001,
                "Shell must expose proportional residual damage exactly once to downstream handlers");
        helper.assertTrue(Math.abs(player.getHealth() - control.getHealth()) < 0.001,
                "Shell residual must receive the same defenses as an ordinary hit: shell="
                        + player.getHealth() + ", control=" + control.getHealth() + ", residual=" + residual);
        helper.assertTrue(player.getHealth() > player.getMaxHealth() - residual,
                "Armor and Protection must mitigate the proportional incoming damage");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()
                        == 3 + control.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(),
                "Shell must apply its initial armor wear plus normal residual armor wear");
        helper.assertTrue(MagicData.getPlayerMagicData(player).getMana() == 0,
                "Shell must spend its activation cost and two absorption steps once");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shellLethalResidualUsesTotemAndDeathDrops(GameTestHelper helper) {
        var survivor = player(helper, "shell_totem");
        equipShell(survivor);
        survivor.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        MagicData.getPlayerMagicData(survivor).setMana(50);
        survivor.hurt(survivor.damageSources().lava(), 200);
        helper.assertTrue(survivor.isAlive() && survivor.getOffhandItem().isEmpty(),
                "Totem must save a player from lethal Shell residual damage");

        var victim = player(helper, "shell_death");
        equipShell(victim);
        MagicData.getPlayerMagicData(victim).setMana(50);
        boolean[] observed = new boolean[2];
        Consumer<LivingDeathEvent> death = event -> {
            if (event.getEntity() == victim) observed[0] = true;
        };
        Consumer<LivingDropsEvent> drops = event -> {
            if (event.getEntity() == victim) observed[1] = true;
        };
        MinecraftForge.EVENT_BUS.addListener(death);
        MinecraftForge.EVENT_BUS.addListener(drops);
        try {
            victim.hurt(victim.damageSources().lava(), 200);
            helper.assertTrue(victim.isDeadOrDying() && observed[0] && observed[1],
                    "Lethal Shell residual must invoke normal death and drop events");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(death);
            MinecraftForge.EVENT_BUS.unregister(drops);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shellResidualPreservesMalumSoulWard(GameTestHelper helper) {
        if (ModList.get().isLoaded("malum")) {
            MalumScenario.verify(helper);
        }
        helper.succeed();
    }

    // optional MODの型は、導入確認後に呼ぶクラスに隔離する。
    private static final class MalumScenario {
        private static void verify(GameTestHelper helper) {
            // Malum 1.6.7にはGeasがないため、Soul Wardへ通常の残ダメージが届くことを検証する。
            var player = player(helper, "shell_ward");
            var control = player(helper, "shell_ward_control");
            for (var subject : new ServerPlayer[]{player, control}) {
                subject.getAttribute(com.sammy.malum.registry.common.AttributeRegistry.SOUL_WARD_CAP.get()).setBaseValue(20);
                com.sammy.malum.common.capability.MalumPlayerDataCapability.getCapability(subject).soulWardHandler.soulWard = 20;
            }
            equipShell(player);
            MagicData.getPlayerMagicData(player).setMana(100);
            var source = player.damageSources().lava();
            helper.assertFalse(source.is(DamageTypeTags.BYPASSES_ARMOR),
                    "Soul Ward comparison must exercise Shell's proportional armor branch");
            float reduced = CombatRules.getDamageAfterAbsorb(12, player.getArmorValue(),
                    (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            reduced = CombatRules.getDamageAfterMagicAbsorb(reduced,
                    EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source));
            control.hurt(source, 12 * (reduced - 2) / reduced);
            player.hurt(source, 12);
            var ward = com.sammy.malum.common.capability.MalumPlayerDataCapability.getCapability(player).soulWardHandler;
            var controlWard = com.sammy.malum.common.capability.MalumPlayerDataCapability.getCapability(control).soulWardHandler;
            helper.assertTrue(ward.soulWard < 20 && Math.abs(ward.soulWard - controlWard.soulWard) < 0.001,
                    "Soul Ward must process proportional Shell residual like an ordinary hit: actual="
                            + ward.soulWard + ", control=" + controlWard.soulWard);
            helper.assertTrue(Math.abs(player.getHealth() - control.getHealth()) < 0.001,
                    "Shell residual must preserve Soul Ward health mitigation");

            var fractional = player(helper, "shell_ward_fraction");
            equipShell(fractional);
            MagicData.getPlayerMagicData(fractional).setMana(800);
            fractional.hurt(fractional.damageSources().lava(), 12);
            helper.assertTrue(MagicData.getPlayerMagicData(fractional).getMana() == 500,
                    "Shell must charge activation plus ten steps including the final fractional point");
            helper.assertTrue(fractional.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 3,
                    "Full absorption must apply only initial Shell armor wear");
        }
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var player = createAssistWingsRider(helper, new BlockPos(2, 2, 2), name);
        var chest = new ItemStack(Items.IRON_CHESTPLATE);
        chest.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        // 装備属性とスポーン無敵を実プレイ相当へ進めてからhurt経路を検証する。
        for (int tick = 0; tick < 61; tick++) player.tick();
        player.doTick();
        helper.assertTrue(player.getArmorValue() == 6, "Test chestplate attributes must be active");
        player.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(1000);
        player.invulnerableTime = 0;
        return player;
    }

    private static void equipShell(ServerPlayer player) {
        var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .setEquippedCurio(CuriosSlotConstants.CHARM, 0, charm);
    }
}
