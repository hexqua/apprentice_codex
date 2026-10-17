package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsjewelry.core.data.Bonus;
import io.redspace.ironsjewelry.core.data.JewelryData;
import io.redspace.ironsjewelry.core.data.PartIngredient;
import io.redspace.ironsjewelry.core.data.PatternDefinition;
import io.redspace.ironsjewelry.registry.BonusTypeRegistry;
import io.redspace.ironsjewelry.registry.IronsJewelryRegistries;
import io.redspace.ironsjewelry.registry.JewelryTypeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunAmmoConsumption;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.event.CuriosEventHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class GemsBulletRuneGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private GemsBulletRuneGameTestScenarios() {
    }

    static void verifyEquippedRunes(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var material = IronsJewelryRegistries.materialRegistry(access).getHolder(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bullet_rune")).orElseThrow();
        var part = IronsJewelryRegistries.partRegistry(access).getHolder(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "rune_inscription")).orElseThrow();
        helper.assertTrue(part.value().canUseMaterial(material), "Bullet rune must fit the Spellbooks rune slot");
        helper.assertTrue(material.value().quality() == 1.5 && material.value().bonusParameters().size() == 1,
                "Bullet rune must have quality 1.5 and only an attribute bonus");
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bullet_rune");
        var slots = CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("ring").getStacks();
        var handler = new CuriosEventHandler();
        var bonus = new Bonus(BonusTypeRegistry.ATTRIBUTE_BONUS.get(), 1, Optional.empty(), Map.of());
        try (var ignored = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(0.5, true)) {
            for (double quality : new double[]{1, 3, 10}) {
                // 品質の倍率とルーン自身の素材品質を混同しないよう、品質担当なしで係数を固定する。
                var pattern = Holder.direct(new PatternDefinition("gametest.bullet_rune", JewelryTypeRegistry.RING.get(),
                        List.of(new PartIngredient(part, 1, 0, List.of(bonus))), Optional.empty(), true, quality));
                var ring = new ItemStack(JewelryTypeRegistry.RING.get().item());
                JewelryData.set(ring, new JewelryData(pattern, Map.of(part, material)));
                slots.setStackInSlot(0, ring);
                handler.tick(new EntityTickEvent.Post(player));
                assertChance(helper, SpellgunAmmoConsumption.consumeChance(player), 1 - quality * 0.05);
            }
            slots.setStackInSlot(1, slots.getStackInSlot(0).copy());
            handler.tick(new EntityTickEvent.Post(player));
            assertChance(helper, SpellgunAmmoConsumption.consumeChance(player), 0.25);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            assertChance(helper, SpellgunAmmoConsumption.consumeChance(player), 0.125);
            // 実在するパターンでも素材・台座からボーナスを構築できることを確認する。
            var realPattern = IronsJewelryRegistries.patternRegistry(access).getHolder(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "rune_inscribed_ring")).orElseThrow();
            var band = realPattern.value().partForQuality().orElseThrow();
            var bandMaterial = IronsJewelryRegistries.materialRegistry(access).holders()
                    .filter(holder -> band.value().canUseMaterial(holder)).findFirst().orElseThrow();
            var realData = new JewelryData(realPattern, Map.of(band, bandMaterial, part, material));
            helper.assertTrue(realData.isValid() && realData.getBonuses().size() == 1,
                    "The actual rune-inscribed ring must accept bullet rune and produce its attribute");
            helper.assertTrue(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get().getStyle(false) == ChatFormatting.BLUE,
                    "Negative ammo consumption modifiers must be beneficial");
        } finally {
            slots.setStackInSlot(0, ItemStack.EMPTY);
            slots.setStackInSlot(1, ItemStack.EMPTY);
            handler.tick(new EntityTickEvent.Post(player));
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        assertChance(helper, SpellgunAmmoConsumption.consumeChance(player), 1);
    }

    private static void assertChance(GameTestHelper helper, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-8,
                "Equipped rune consumption mismatch: expected=" + expected + " actual=" + actual);
    }
}
