package jp.aquafactory.apprenticecodex.gametest.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.PlunderLootingEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class OverlevelEnchantmentGameTests {
    private OverlevelEnchantmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void wisdomAndPlunderContinueAboveNormalMaximum(GameTestHelper helper) {
        var player = PlayerTestSupport.createEquipmentTestPlayer(helper, new BlockPos(2, 2, 2), "overlevel_loot");
        var victim = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        for (int level : new int[]{3, 6}) {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            enchant(helper, stack, Enchantments.WISDOM, level);
            enchant(helper, stack, Enchantments.PLUNDER, level);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var event = new LivingExperienceDropEvent(victim, player, 100);
            WisdomExperienceDropEvent.onLivingExperienceDrop(event);
            helper.assertTrue(event.getDroppedExperience() == (level == 3 ? 160 : 220),
                    "Wisdom must continue scaling through VI: level=" + level + " XP=" + event.getDroppedExperience());
            var params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, victim)
                    .withParameter(LootContextParams.ORIGIN, victim.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().playerAttack(player))
                    .create(LootContextParamSets.ENTITY);
            var context = new LootContext.Builder(params).create(Optional.empty());
            var looting = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            helper.assertTrue(PlunderLootingEvent.applyLootingLevel(0, looting, context) == level,
                    "Plunder must pass overlevel through Looting conversion: " + level);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void redAndGlowEnergyContinueAboveNormalMaximum(GameTestHelper helper) {
        var potion = PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, Potions.REGENERATION.value());
        var original = PotionContentsHelper.getMobEffects(potion).getFirst();
        for (int redLevel : new int[]{4, 6}) {
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            enchant(helper, flask, Enchantments.RED_ENERGY, redLevel);
            flask = SpellcastersFlask.copyWithAddedDoses(flask, potion, 1);
            var effect = PotionContentsHelper.getMobEffects(AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 0)).getFirst();
            helper.assertTrue(effect.getDuration() == Math.round(original.getDuration() * (redLevel == 4 ? 2.0F : 2.5F)),
                    "Red Energy must extend duration through VI: " + redLevel);
        }
        for (int glowLevel : new int[]{3, 4}) {
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            enchant(helper, flask, Enchantments.GLOW_ENERGY, glowLevel);
            flask = SpellcastersFlask.copyWithAddedDoses(flask, potion, 1);
            var effect = PotionContentsHelper.getMobEffects(AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 0)).getFirst();
            helper.assertTrue(effect.getAmplifier() == original.getAmplifier() + glowLevel,
                    "Glow Energy must increase amplifier through IV: " + glowLevel);
            helper.assertTrue(effect.getDuration() == Math.max(1, Math.round(original.getDuration() / (float) (1 + glowLevel))),
                    "Overlevel Glow Energy must retain its duration penalty: " + glowLevel);
        }
        helper.succeed();
    }

    private static void enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);
    }
}
