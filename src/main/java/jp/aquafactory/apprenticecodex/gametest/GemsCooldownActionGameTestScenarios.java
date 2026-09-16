package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsjewelry.core.data.Bonus;
import io.redspace.ironsjewelry.core.data.BonusInstance;
import io.redspace.ironsjewelry.core.data.JewelryData;
import io.redspace.ironsjewelry.core.data.PartIngredient;
import io.redspace.ironsjewelry.core.data.PatternDefinition;
import io.redspace.ironsjewelry.core.data.PlayerData;
import io.redspace.ironsjewelry.core.data.QualityScalar;
import io.redspace.ironsjewelry.event.JewelryBonusEvents;
import io.redspace.ironsjewelry.registry.BonusTypeRegistry;
import io.redspace.ironsjewelry.registry.IronsJewelryRegistries;
import io.redspace.ironsjewelry.registry.JewelryTypeRegistry;
import io.redspace.ironsjewelry.registry.ParameterTypeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.gems.AdvanceSpellCooldownsAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class GemsCooldownActionGameTestScenarios {
    private static final String LONG_SPELL = "irons_spellbooks:firebolt";
    private static final String SHORT_SPELL = "irons_spellbooks:heal";

    private GemsCooldownActionGameTestScenarios() {
    }

    static void verifyScalingAndExpiry(GameTestHelper helper) {
        var player = player(helper);
        var cooldowns = cooldowns(player);
        var action = materialAction(helper);
        double[] qualities = {1, 2, 3, 10, 2.75, -1};
        int[] advances = {10, 15, 20, 20, 18, 0};
        for (int i = 0; i < qualities.length; i++) {
            cooldowns.addCooldown(LONG_SPELL, 200, 100);
            action.apply(helper.getLevel(), qualities[i], true, player, player);
            helper.assertTrue(remaining(cooldowns) == 100 - advances[i],
                    "Material cooldown advance should scale, clamp and truncate at quality " + qualities[i]);
            helper.assertTrue(cooldowns.getSpellCooldowns().get(LONG_SPELL).getSpellCooldown() == 200,
                    "Advancing time must preserve the original cooldown duration");
        }
        cooldowns.addCooldown(SHORT_SPELL, 50, 15);
        action.apply(helper.getLevel(), 2, true, player, player);
        helper.assertTrue(remaining(cooldowns) == 85 && !cooldowns.getSpellCooldowns().containsKey(SHORT_SPELL),
                "All active spells should advance and a cooldown ending exactly at zero should be removed");
        cooldowns.addCooldown(SHORT_SPELL, 50, 1);
        action.apply(helper.getLevel(), 2, true, player, player);
        helper.assertTrue(!cooldowns.getSpellCooldowns().containsKey(SHORT_SPELL),
                "Advancing past a cooldown's remaining time should remove it");
        new AdvanceSpellCooldownsAction(new QualityScalar(40)).apply(helper.getLevel(), 1, true, player, player);
        helper.assertTrue(remaining(cooldowns) == 30, "An action without a data cap must allow more than twenty ticks");

        var bonus = new BonusInstance(BonusTypeRegistry.ON_ATTACK_BONUS.get(), 2, Map.of(), Optional.empty());
        var tooltip = (TranslatableContents) action.formatTooltip(bonus, true).getContents();
        helper.assertTrue(tooltip.getKey().equals("action.apprenticecodex.advance_spell_cooldowns.self")
                        && ((Component) tooltip.getArgs()[0]).getString().equals("0.75"),
                "The tooltip must retain subsecond precision and select the self translation");
        var parameter = ParameterTypeRegistry.ACTION_PARAMETER.get();
        var runnable = parameter.resolve(IronsJewelryRegistries.materialRegistry(helper.getLevel().registryAccess())
                .get(materialId()).bonusParameters()).orElseThrow();
        helper.assertTrue(parameter.getValueDescriptionId(runnable).orElseThrow()
                        .equals("action.apprenticecodex.advance_spell_cooldowns.name"),
                "Gems must resolve the registered action name for material descriptions");
    }

    static void verifyTargetsAndNoOps(GameTestHelper helper) {
        var wearer = player(helper);
        var target = player(helper);
        var wearerCooldowns = cooldowns(wearer);
        var targetCooldowns = cooldowns(target);
        wearerCooldowns.addCooldown(LONG_SPELL, 100);
        targetCooldowns.addCooldown(LONG_SPELL, 100);
        var action = materialAction(helper);
        action.apply(helper.getLevel(), 1, true, wearer, target);
        helper.assertTrue(remaining(wearerCooldowns) == 90 && remaining(targetCooldowns) == 100,
                "Self targeting must only change the wearer");
        action.apply(helper.getLevel(), 1, false, wearer, target);
        helper.assertTrue(remaining(wearerCooldowns) == 90 && remaining(targetCooldowns) == 90,
                "Entity targeting must only change the target player");
        var nonPlayer = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        action.apply(helper.getLevel(), 1, false, wearer, nonPlayer);
        for (int ticks : new int[]{0, -10}) {
            new AdvanceSpellCooldownsAction(new QualityScalar(ticks)).apply(helper.getLevel(), 1, true, wearer, target);
        }
        helper.assertTrue(remaining(wearerCooldowns) == 90 && remaining(targetCooldowns) == 90,
                "Non-player targets and non-positive advances must not change spell cooldowns");
        wearerCooldowns.clearCooldowns();
        action.apply(helper.getLevel(), 1, true, wearer, target);
        helper.assertTrue(!wearerCooldowns.hasCooldownsActive(), "An empty cooldown set must remain empty");
        wearerCooldowns.addCooldown(LONG_SPELL, 100);
        helper.assertTrue(remaining(wearerCooldowns) == 100, "Later cooldowns must not inherit an earlier advance");
        var bonus = new BonusInstance(BonusTypeRegistry.ON_ATTACK_BONUS.get(), 1, Map.of(), Optional.empty());
        helper.assertTrue(((TranslatableContents) action.formatTooltip(bonus, false).getContents()).getKey()
                        .equals("action.apprenticecodex.advance_spell_cooldowns.entity"),
                "Entity targeting should use the target player translation");
    }

    static void verifyAttackTrigger(GameTestHelper helper) {
        var wearer = player(helper);
        var access = helper.getLevel().registryAccess();
        var material = IronsJewelryRegistries.materialRegistry(access).getHolder(materialId()).orElseThrow();
        var part = IronsJewelryRegistries.partRegistry(access).holders()
                .filter(holder -> holder.value().canUseMaterial(material)).findFirst().orElseThrow();
        // 品質2・再発動40tickのパターンで、読み込まれた素材から実際のJewelryDataを構築する。
        var bonus = new Bonus(BonusTypeRegistry.ON_ATTACK_BONUS.get(), 1,
                Optional.of(new QualityScalar(40)), Map.of());
        var pattern = Holder.direct(new PatternDefinition("gametest.gems_cooldown", JewelryTypeRegistry.RING.get(),
                List.of(new PartIngredient(part, 1, 0, List.of(bonus))), Optional.empty(), true, 2));
        var jewelry = new JewelryData(pattern, Map.of(part, material));
        helper.assertTrue(jewelry.isValid() && jewelry.getBonuses().size() == 1,
                "The trigger fixture must be valid jewelry with one material-derived bonus");
        var stack = new ItemStack(JewelryTypeRegistry.RING.get().item());
        JewelryData.set(stack, jewelry);
        var ringSlots = CuriosApi.getCuriosInventory(wearer).orElseThrow().getCurios().get("ring").getStacks();
        ringSlots.setStackInSlot(0, stack);
        try {
            var cooldowns = cooldowns(wearer);
            cooldowns.addCooldown(LONG_SPELL, 100);
            wearer.getCooldowns().addCooldown(Items.STICK, 100);
            var victim = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
            var source = helper.getLevel().damageSources().playerAttack(wearer);
            JewelryBonusEvents.onLivingDamaged(new LivingIncomingDamageEvent(victim, new DamageContainer(source, 1)));
            helper.assertTrue(remaining(cooldowns) == 85, "The Gems attack trigger must run the material action");
            var playerData = PlayerData.get(wearer);
            // 上流は同tick内の複数ボーナスを許容し、tick経過後から再発動を抑止する。
            playerData.tickCooldowns(1);
            helper.assertTrue(playerData.isOnCooldown(bonus.bonusType()), "Gems should start its own trigger cooldown");
            JewelryBonusEvents.onLivingDamaged(new LivingIncomingDamageEvent(victim, new DamageContainer(source, 1)));
            helper.assertTrue(remaining(cooldowns) == 85, "The active Gems cooldown must suppress another action");
            actionWithoutTriggerCooldown(helper, wearer);
            helper.assertTrue(playerData.isOnCooldown(bonus.bonusType()) && wearer.getCooldowns().isOnCooldown(Items.STICK),
                    "Spell advancement must not advance Gems or vanilla item cooldowns");
            playerData.tickCooldowns(39);
            helper.assertFalse(playerData.isOnCooldown(bonus.bonusType()), "Gems cooldown should end after its own duration");
            JewelryBonusEvents.onLivingDamaged(new LivingIncomingDamageEvent(victim, new DamageContainer(source, 1)));
            helper.assertTrue(remaining(cooldowns) == 60, "The action must run again after the Gems cooldown expires");
        } finally {
            ringSlots.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    private static void actionWithoutTriggerCooldown(GameTestHelper helper, FakePlayer wearer) {
        materialAction(helper).apply(helper.getLevel(), 1, true, wearer, wearer);
    }

    private static AdvanceSpellCooldownsAction materialAction(GameTestHelper helper) {
        var material = IronsJewelryRegistries.materialRegistry(helper.getLevel().registryAccess()).get(materialId());
        var runnable = ParameterTypeRegistry.ACTION_PARAMETER.get().resolve(material.bonusParameters()).orElseThrow();
        helper.assertTrue(runnable.targetSelf(), "Emberstained netherite must target the wearer");
        var action = (AdvanceSpellCooldownsAction) runnable.action();
        helper.assertTrue(action.ticks().equals(new QualityScalar(10, 5, 0, Optional.of(20d))),
                "Loaded material must configure base ten, scalar five, minimum zero and maximum twenty ticks");
        return action;
    }

    private static ResourceLocation materialId() {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "emberstained_netherite");
    }

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gems_cooldown"));
    }

    private static PlayerCooldowns cooldowns(FakePlayer player) {
        return MagicData.getPlayerMagicData(player).getPlayerCooldowns();
    }

    private static int remaining(PlayerCooldowns cooldowns) {
        return cooldowns.getSpellCooldowns().get(LONG_SPELL).getCooldownRemaining();
    }
}
