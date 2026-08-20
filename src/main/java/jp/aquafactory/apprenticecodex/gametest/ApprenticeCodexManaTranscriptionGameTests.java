package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manatranscription.ManaTranscription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexManaTranscriptionGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexManaTranscriptionGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void extractionConsumesPointsAndUsesVanillaRepairCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_extract");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(unbreaking, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WRITABLE_BOOK));
            player.giveExperiencePoints(1_000);
            var experienceBefore = player.totalExperience;

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should accept an enchanted mainhand and blank writable book");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(target).isEmpty(),
                    "Mana Transcription should remove the selected enchantment from the target");
            helper.assertTrue(target.getOrDefault(DataComponents.REPAIR_COST, 0) == 1,
                    "Mana Transcription should increase repair cost with the vanilla formula");
            var resultEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(player.getOffhandItem());
            helper.assertTrue(player.getOffhandItem().is(Items.ENCHANTED_BOOK)
                            && resultEnchantments.getLevel(unbreaking) == 1,
                    "Mana Transcription should replace the writable book with the selected enchanted book");
            helper.assertTrue(player.totalExperience == experienceBefore - 16,
                    "Unbreaking I should cost the cumulative XP required for level 2");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatTwoHandedWeaponUsesPhysicalOffhandBook(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var player = createPlayer(helper, "mana_transcription_better_combat");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            target.enchant(unbreaking, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.getInventory().offhand.set(0, new ItemStack(Items.WRITABLE_BOOK));
            player.giveExperiencePoints(1_000);

            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide the physical offhand book from logical hand access");

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should accept a Better Combat-hidden physical offhand book");

            // SpellOnCastEvent は呪文本体の完了処理より先に剣を充填し、同期用の copy をメインハンドへ再設定する。
            SpellchargedGreatsword.addCharge(
                    target,
                    helper.getLevel().getGameTime(),
                    SpellchargedGreatsword.computeChargeGainTicks(spell, 1)
            );
            player.setItemSlot(EquipmentSlot.MAINHAND, target.copy());
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var physicalOffhand = player.getInventory().offhand.get(0);
            helper.assertTrue(physicalOffhand.is(Items.ENCHANTED_BOOK)
                            && EnchantmentHelper.getEnchantmentsForCrafting(physicalOffhand).getLevel(unbreaking) == 1,
                    "Mana Transcription should replace the physical offhand book with the enchanted result");
            helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(player.getMainHandItem()).isEmpty(),
                    "Mana Transcription should remove the enchantment from the two-handed target");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void curseOverridesHigherNonCurseWeight(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_curse");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var vanishing = enchantments.getOrThrow(Enchantments.VANISHING_CURSE);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(vanishing, 1);
            target.enchant(unbreaking, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WRITABLE_BOOK));
            player.giveExperiencePoints(100_000);

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should accept curse extraction");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var targetEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(target);
            var resultEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(player.getOffhandItem());
            helper.assertTrue(targetEnchantments.getLevel(vanishing) == 0
                            && targetEnchantments.getLevel(unbreaking) == 3,
                    "A curse should be extracted before a higher-weight non-curse enchantment");
            helper.assertTrue(resultEnchantments.getLevel(vanishing) == 1,
                    "The extracted book should contain the curse");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tiedCandidatesChooseExactlyOneOnServer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_tie");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(unbreaking, 1);
            target.enchant(sharpness, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WRITABLE_BOOK));
            player.giveExperiencePoints(1_000);

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Equal weighted enchantments should be valid server-side candidates");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var remaining = EnchantmentHelper.getEnchantmentsForCrafting(target);
            var extracted = EnchantmentHelper.getEnchantmentsForCrafting(player.getOffhandItem());
            var extractedUnbreaking = extracted.getLevel(unbreaking) == 1;
            var extractedSharpness = extracted.getLevel(sharpness) == 2;
            helper.assertTrue(extractedUnbreaking ^ extractedSharpness,
                    "A server-authoritative tie should extract exactly one candidate");
            helper.assertTrue(remaining.size() == 1
                            && (remaining.getLevel(unbreaking) == 1 ^ remaining.getLevel(sharpness) == 2),
                    "The unselected tied candidate should remain on the target");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void stackedWritableBooksInsertOutputWithoutReplacingStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_stacked_books");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(unbreaking, 1);
            var writableBooks = new ItemStack(Items.WRITABLE_BOOK, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, writableBooks);
            player.giveExperiencePoints(1_000);

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "A modded writable-book stack should remain a valid operation item");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(player.getOffhandItem().is(Items.WRITABLE_BOOK)
                            && player.getOffhandItem().getCount() == 1,
                    "A stacked writable book should consume one item without replacing the offhand stack");
            helper.assertTrue(player.getInventory().items.stream().anyMatch(stack ->
                            stack.is(Items.ENCHANTED_BOOK)
                                    && EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(unbreaking) == 1),
                    "The enchanted-book output should be inserted into the inventory");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void repairCostResetConsumesCatalystAndExperience(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_reset");
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.set(DataComponents.REPAIR_COST, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SPELLSTAINED_DIAMOND.get()));
            player.giveExperiencePoints(1_000);
            var experienceBefore = player.totalExperience;

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should accept a tagged reset catalyst");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(target.getOrDefault(DataComponents.REPAIR_COST, 0) == 0,
                    "Mana Transcription should clear the repair cost");
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Survival repair-cost reset should consume one catalyst");
            helper.assertTrue(player.totalExperience == experienceBefore - 27,
                    "Repair cost 3 should cost the cumulative XP required for level 3");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void extractionRejectsWrittenAndSingleEnchantmentBooks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_rejections");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(unbreaking, 1);
            var writtenBook = new ItemStack(Items.WRITABLE_BOOK);
            writtenBook.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(
                    Filterable.passThrough("draft")
            )));
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, writtenBook);
            player.giveExperiencePoints(1_000);

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should reject a writable book with non-empty raw text");

            var enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            enchantedBook.enchant(unbreaking, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, enchantedBook);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WRITABLE_BOOK));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should reject an enchanted book with one enchantment");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void creativeResetKeepsCatalystAndExperience(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_creative_reset");
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.set(DataComponents.REPAIR_COST, 7);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SPELLSTAINED_DIAMOND.get()));

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Creative Mana Transcription should ignore experience requirements");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(target.getOrDefault(DataComponents.REPAIR_COST, 0) == 0,
                    "Creative Mana Transcription should still mutate the target");
            helper.assertTrue(player.getOffhandItem().getCount() == 1,
                    "Creative Mana Transcription should not consume the reset catalyst");
            helper.assertTrue(player.totalExperience == 0,
                    "Creative Mana Transcription should not consume experience");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void changedHandsFailCompletionWithoutMutation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_swap");
            var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.enchant(unbreaking, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, target);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WRITABLE_BOOK));
            player.giveExperiencePoints(1_000);
            var experienceBefore = player.totalExperience;

            var spell = spell();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Transcription should start before the hand swap");
            player.setItemInHand(InteractionHand.MAIN_HAND, target.copy());
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(player.getMainHandItem()).getLevel(unbreaking) == 1,
                    "An identical replacement stack should still fail the identity lock");
            helper.assertTrue(player.totalExperience == experienceBefore,
                    "A completion-time hand mismatch should not consume experience");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void castTimeIgnoresReductionAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createPlayer(helper, "mana_transcription_cast_time");
            var attribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
            helper.assertTrue(attribute != null, "Cast-time reduction attribute should be available");
            if (attribute != null) {
                attribute.setBaseValue(10.0D);
            }
            var spell = spell();
            helper.assertTrue(spell.getEffectiveCastTime(1, player) == spell.getCastTime(1),
                    "Mana Transcription should keep its configured five-second cast time");
        });
    }

    private static ManaTranscription spell() {
        return (ManaTranscription) SpellRegistry.MANA_TRANSCRIPTION.get();
    }

    private static net.neoforged.neoforge.common.util.FakePlayer createPlayer(GameTestHelper helper, String name) {
        return ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), name
        );
    }
}
