package jp.aquafactory.apprenticecodex.gametest;

import com.sammy.malum.core.handlers.GeasEffectHandler;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import com.sammy.malum.registry.common.magic.MalumGeasEffectTypes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
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
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class ManaShieldShellResidualGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void shellForwardsTyrosScaleDamageAndZeroAbsorptionEndpoints(GameTestHelper helper) {
        for (float mana : new float[]{0, 49, 50, 800}) {
            var player = player(helper, "shell_ratio");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var protection = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
            for (var armor : player.getArmorSlots()) armor.enchant(protection, 4);
            player.doTick();
            equipShell(player);
            MagicData.getPlayerMagicData(player).setMana(mana);
            var health = player.getHealth();
            var event = postLivingAttackEventForGameTest(player, player.damageSources().lava(), 240);
            // 防御19・Protection IV×4では240→73.2672。800マナなら30点吸収し、残存割合を240へ掛ける。
            float expected = mana == 800 ? 240 * (73.2672F - 30) / 73.2672F : 240;
            helper.assertFalse(event.isCanceled(), "Tyros-scale damage must continue when incompletely absorbed");
            helper.assertTrue(Math.abs(event.getAmount() - expected) < 0.001,
                    "Shell must forward the unblocked proportion: mana=" + mana + ", damage=" + event.getAmount());
            helper.assertTrue(player.getHealth() == health,
                    "Posting an incoming event alone must never directly damage the player");
            helper.assertTrue(MagicData.getPlayerMagicData(player).getMana() == 0,
                    "This hit must exhaust the available mana");
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == (mana >= 50 ? 60 : 0),
                    "Shell activation wear must occur even when activation leaves no absorption budget");
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
        float reduced = CombatRules.getDamageAfterAbsorb(player, 12, source,
                player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        reduced = CombatRules.getDamageAfterMagicAbsorb(reduced,
                EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source));
        float residual = 12 * (reduced - 2) / reduced;
        control.hurt(source, residual);
        int[] incomingCalls = {0};
        float[] forwarded = {0};
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == player) {
                incomingCalls[0]++;
                forwarded[0] = event.getAmount();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingIncomingDamageEvent.class, listener);
        try {
            player.hurt(source, 12);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(incomingCalls[0] == 1 && Math.abs(forwarded[0] - residual) < 0.001,
                "Shell must forward proportional damage in one original event without reentering hurt");
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
        NeoForge.EVENT_BUS.addListener(death);
        NeoForge.EVENT_BUS.addListener(drops);
        try {
            victim.hurt(victim.damageSources().lava(), 200);
            helper.assertTrue(victim.isDeadOrDying() && observed[0] && observed[1],
                    "Lethal Shell residual must invoke normal death and drop events");
        } finally {
            NeoForge.EVENT_BUS.unregister(death);
            NeoForge.EVENT_BUS.unregister(drops);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shellResidualPreservesMalumMagicConversionAndSoulWard(GameTestHelper helper) {
        if (ModList.get().isLoaded("malum")) {
            MalumScenario.verify(helper);
        }
        helper.succeed();
    }

    // optional MODの型は、導入確認後に呼ぶクラスに隔離する。
    private static final class MalumScenario {
        private static void verify(GameTestHelper helper) {
            var player = player(helper, "shell_ward");
            helper.assertTrue(GeasEffectHandler.addGeasEffect(player,
                    MalumGeasEffectTypes.PACT_OF_THE_ARCANAPHAGE.get()), "Arcanaphage pact must equip");
            helper.assertTrue(GeasEffectHandler.addGeasEffect(player,
                    MalumGeasEffectTypes.PACT_OF_RECIPROCATION.get()), "Reciprocation pact must equip");
            player.getAttribute(MalumAttributes.SOUL_WARD_CAPACITY).setBaseValue(100);
            var ward = player.getData(MalumAttachmentTypes.SOUL_WARD);
            ward.setSoulWard(100);
            equipShell(player);
            MagicData.getPlayerMagicData(player).setMana(50);
            player.hurt(player.damageSources().lava(), 200);
            helper.assertTrue(ward.getSoulWard() < 100,
                    "Soul Ward must process Shell residual through Arcanaphage conversion");
            helper.assertTrue(player.isAlive(), "Soul Ward must protect against otherwise lethal Shell residual");

            // マナが残る端数貫通では、変換前後の二重発動を枯渇クールダウンで隠せない。
            var fractional = player(helper, "shell_ward_fraction");
            helper.assertTrue(GeasEffectHandler.addGeasEffect(fractional,
                    MalumGeasEffectTypes.PACT_OF_THE_ARCANAPHAGE.get()), "Arcanaphage pact must equip");
            equipShell(fractional);
            MagicData.getPlayerMagicData(fractional).setMana(800);
            fractional.hurt(fractional.damageSources().lava(), 12);
            helper.assertTrue(MagicData.getPlayerMagicData(fractional).getMana() == 525,
                    "Magic conversion must charge Shell only once: 50 activation plus nine 25-mana steps");
            helper.assertTrue(fractional.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 4,
                    "Magic conversion must not duplicate Shell armor wear on fractional penetration");
        }
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var player = createAssistWingsRider(helper, new BlockPos(2, 2, 2), name);
        var chest = new ItemStack(Items.IRON_CHESTPLATE);
        chest.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION), 4);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        // 装備属性とスポーン無敵を実プレイ相当へ進めてからhurt経路を検証する。
        for (int tick = 0; tick < 61; tick++) player.tick();
        player.doTick();
        helper.assertTrue(player.getArmorValue() == 6, "Test chestplate attributes must be active");
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(1000);
        player.invulnerableTime = 0;
        return player;
    }

    private static void equipShell(ServerPlayer player) {
        var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        charm.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHELL), 1);
        CuriosApi.getCuriosInventory(player).orElseThrow()
                .setEquippedCurio(CuriosSlotConstants.CHARM, 0, charm);
    }
}
