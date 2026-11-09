package jp.aquafactory.apprenticecodex.gametest.equipment.flask;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.AlchemistsFlaskAdvancementEvent;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.extract.Extract;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.crafting.RecipeTestSupport.createCraftingContainer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTooltipKeyAbsent;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTooltipKeyUsesColor;

public final class FlaskScenarios {
    private FlaskScenarios() {}

    static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var emptyFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Spellcaster's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Spellcaster's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Spellcaster's Flask rejected a lingering potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, splashPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a splash potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, lingeringPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a lingering potion");
        });
    }

    static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(
                    PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION),
                    1,
                    0
            );
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_drink_test"));

            var result = flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            helper.assertTrue(result.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Drinking the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(result) == 0,
                    "Drinking the last dose did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(result));
            helper.assertTrue(SpellcastersFlask.getStoredItem(result).isEmpty(),
                    "Drinking the last dose left StoredItem behind");
            helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                    "Drinking the flask did not apply the stored potion effect");
        });
    }

    static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var flask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(storedPotion, 1, 2);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_glow_tradeoff_test"));

            flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            var appliedEffect = player.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(appliedEffect != null, "Drinking the flask did not apply regeneration");
            helper.assertTrue(appliedEffect != null && appliedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Glow Energy should still amplify drunk flask effects");
            helper.assertTrue(appliedEffect != null
                            && appliedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * (1.0F / 3.0F))),
                    "Glow Energy should reduce drunk flask duration by 1 / (1 + level)");
        });
    }

    static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var twoDoseFlask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(storedPotion, 2, 0);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_mismatch_drink_test"));

            twoDoseFlask.getItem().finishUsingItem(twoDoseFlask, helper.getLevel(), player);

            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(twoDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should consume two doses");
            helper.assertTrue(SpellcastersFlask.getStoredItem(twoDoseFlask).isEmpty(),
                    "Mismatched Spellcaster's Flask should clear StoredItem when extra consumption empties it");

            var oneDoseFlask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(storedPotion, 1, 0);
            oneDoseFlask.getItem().finishUsingItem(oneDoseFlask, helper.getLevel(), player);
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(oneDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should still work with one remaining dose");
        });
    }

    static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );

            var emptiedFlask = SpellcastersFlask.copyAfterExtractingDoses(flask, 1);

            helper.assertTrue(emptiedFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Extracting the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(emptiedFlask) == 0,
                    "Batch extraction did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(emptiedFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(emptiedFlask).isEmpty(),
                    "Batch extraction left StoredItem behind");
        });
    }

    static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var flask = SpellDispenserManaTestSupport.createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var craftingContainer = createCraftingContainer(flask, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should match a filled flask and glass bottle");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result,
                            createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get())),
                    "Spellcaster's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Spellcaster's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Spellcaster's Flask extract recipe left StoredItem behind");
        });
    }

    static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Alchemist's Flask did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Alchemist's Flask spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Alchemist's Flask spell slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Alchemist's Flask should not expose the imbued spell in the spell wheel");

            var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != SpellData.EMPTY, "Alchemist's Flask has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.EXTRACT.get(),
                    "Alchemist's Flask preset spell mismatch: " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
            helper.assertTrue(spellData.getLevel() == 1,
                    "Alchemist's Flask preset spell level mismatch: " + (spellData == SpellData.EMPTY ? -1 : spellData.getLevel()));
            helper.assertTrue(!spellData.canRemove(), "Alchemist's Flask preset spell should stay locked by default");
        });
    }

    static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Alchemist's Flask should allow instant spell imbuing");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.COMPOUND_PHIAL.get(), 1),
                    "Alchemist's Flask should allow long spell imbuing");
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Alchemist's Flask should allow continuous spell imbuing");
        });
    }

    static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var emptyFlask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Alchemist's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Alchemist's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Alchemist's Flask rejected a lingering potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, simpleElixir),
                    "Alchemist's Flask rejected a Simple Elixir");
        });
    }

    static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            assertTooltipKeyUsesColor(
                    helper,
                    SpellDispenserManaTestSupport.createFilledSpellcastersFlask(splashPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Spellcaster's Flask should warn for stored splash potions"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    createFilledAlchemistsFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Alchemist's Flask should warn for stored regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    SpellDispenserManaTestSupport.createFilledSpellcastersFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Spellcaster's Flask should not warn for regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Alchemist's Flask should not warn for Simple Elixir"
            );
        });
    }

    static void flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should auto-fill an empty Spellcaster's Flask with supported splash potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should auto-fill an empty Alchemist's Flask with supported regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should auto-fill an empty Spellcaster's Flask with regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should auto-fill an empty Alchemist's Flask with splash potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            simpleElixir),
                    "Atelier Station should keep non-vanilla Alchemist's Flask items outside mismatch penalties");
        });
    }

    static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 16,
                    "Alchemist's Flask base capacity mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));

            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                flask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
                helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 20,
                        "Alchemist's Flask Large Mug bonus mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));
            }

            var filledFlask = SpellcastersFlask.copyWithAddedDoses(flask, splashPotion, 16);
            helper.assertTrue(!filledFlask.isEmpty(), "Alchemist's Flask failed to store sixteen splash potion doses");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(filledFlask) == 16,
                    "Alchemist's Flask stored dose count mismatch: " + SpellcastersFlask.getStoredDoseCount(filledFlask));

            var recipe = (SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var craftingContainer = createCraftingContainer(
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()), splashPotion, 1),
                    new ItemStack(Items.GLASS_BOTTLE)
            );

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should accept Alchemist's Flask");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result, splashPotion),
                    "Alchemist's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Alchemist's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Alchemist's Flask extract recipe left StoredItem behind");
        });
    }

    static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (AlchemistsFlaskSmithingRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"))
                    .orElseThrow();

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
            var spellcastersFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
            if (EnchantmentRegistry.GUZZLE.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GUZZLE.get(), 2);
            }
            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
            }
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            var filledSpellcastersFlask = SpellcastersFlask.copyWithAddedDoses(spellcastersFlask, normalPotion, 2);
            filledSpellcastersFlask = SpellcastersFlask.copyWithToggledEffectParticles(filledSpellcastersFlask);
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(filledSpellcastersFlask),
                    "Spellcaster's Flask test input should start with suppressed particles");
            var smithingContainer = new SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    filledSpellcastersFlask,
                    new ItemStack(Items.GUNPOWDER)
            );

            helper.assertTrue(recipe.matches(smithingContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept a filled Spellcaster's Flask");

            var convertedFlask = recipe.assemble(smithingContainer, helper.getLevel().registryAccess());
            helper.assertTrue(convertedFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask smithing recipe returned the wrong result item");
            helper.assertTrue(ISpellContainer.isSpellContainer(convertedFlask),
                    "Alchemist's Flask smithing recipe should preserve the preset spell container");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(convertedFlask) == 2,
                    "Alchemist's Flask smithing recipe should preserve stored dose count");

            var convertedStoredItem = SpellcastersFlask.getStoredItem(convertedFlask);
            helper.assertTrue(convertedStoredItem.is(Items.POTION),
                    "Filled Spellcaster's Flask should keep a regular potion as a mismatch-usable potion");
            helper.assertTrue(PotionUtils.getPotion(convertedStoredItem) == PotionUtils.getPotion(normalPotion),
                    "Converted Alchemist's Flask should keep the original potion type");
            helper.assertTrue(!SpellcastersFlask.isEffectParticlesSuppressed(convertedFlask),
                    "Alchemist's Flask smithing recipe should reset suppressed particles");

            var convertedEnchantments = EnchantmentHelper.getEnchantments(convertedFlask);
            helper.assertTrue(!EnchantmentRegistry.GUZZLE.isPresent()
                            || !convertedEnchantments.containsKey(EnchantmentRegistry.GUZZLE.get()),
                    "Alchemist's Flask smithing recipe should drop only Guzzle");
            helper.assertTrue(!EnchantmentRegistry.LARGE_MUG.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.LARGE_MUG.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Large Mug");
            helper.assertTrue(!EnchantmentRegistry.RED_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.RED_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Red Energy");
            helper.assertTrue(!EnchantmentRegistry.GLOW_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.GLOW_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Glow Energy");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = new SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), simpleElixir, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept Simple Elixir");
            var simpleElixirResult = recipe.assemble(simpleElixirContainer, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.isSameItemSameTags(SpellcastersFlask.getStoredItem(simpleElixirResult), simpleElixir),
                    "Alchemist's Flask smithing recipe should keep Simple Elixir unchanged");

            var fireAle = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_ALE.get());
            var fireAleContainer = new SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), fireAle, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(!recipe.matches(fireAleContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should reject unsupported stored items such as Fire Ale");
        });
    }

    static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (AlchemistsFlaskTippedArrowRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"))
                    .orElseThrow();

            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var splashFlask = createFilledAlchemistsFlask(splashPotion, 2, 1);
            var splashContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    splashFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(splashContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a splash potion flask");

            var splashResult = recipe.assemble(splashContainer, helper.getLevel().registryAccess());
            var splashRemainingFlask = recipe.getRemainingItems(splashContainer).get(4);

            helper.assertTrue(splashResult.is(Items.TIPPED_ARROW) && splashResult.getCount() == 8,
                    "Alchemist's Flask tipped arrow recipe should return eight tipped arrows");
            helper.assertTrue(PotionUtils.getPotion(splashResult) == PotionUtils.getPotion(splashPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored splash potion");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(splashRemainingFlask) == 1,
                    "Alchemist's Flask tipped arrow recipe should consume exactly one dose");

            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.HEALING);
            var lingeringFlask = createFilledAlchemistsFlask(lingeringPotion, 1, 0);
            var lingeringContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    lingeringFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(lingeringContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a lingering potion flask");
            var lingeringResult = recipe.assemble(lingeringContainer, helper.getLevel().registryAccess());
            var lingeringRemainingFlask = recipe.getRemainingItems(lingeringContainer).get(4);

            helper.assertTrue(PotionUtils.getPotion(lingeringResult) == PotionUtils.getPotion(lingeringPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored lingering potion");
            helper.assertTrue(lingeringRemainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask tipped arrow recipe should return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(lingeringRemainingFlask) == 0,
                    "Alchemist's Flask tipped arrow recipe should empty the flask after the last dose");
            helper.assertTrue(SpellcastersFlask.getStoredItem(lingeringRemainingFlask).isEmpty(),
                    "Alchemist's Flask tipped arrow recipe should clear StoredItem after the last dose");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            helper.assertTrue(!recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should reject Simple Elixir");
        });
    }

    static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), jp.aquafactory.apprenticecodex.registry.PotionRegistry.INTELLIGENCE.get());
            var craftingContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(splashPotion, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            var craftedStack = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 8), jp.aquafactory.apprenticecodex.registry.PotionRegistry.INTELLIGENCE.get());
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/craft_tipped_arrow_by_flask")
            );

            helper.assertTrue(advancement != null, "Missing advancement for flask tipped arrow crafting");
            helper.assertTrue(
                    AlchemistsFlaskAdvancementEvent.shouldAward(craftedStack, craftingContainer),
                    "Crafting tipped arrows with Alchemist's Flask should satisfy the advancement award conditions"
            );
        });
    }

    static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_hand_test");
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.HEALING);
            var magicData = MagicData.getPlayerMagicData(player);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(splashPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when the main hand flask is filled");
            helper.assertTrue(magicData.getAdditionalCastData() instanceof Extract.ExtractCastData,
                    "Extract should store cast data for the selected flask");
            var mainCastData = (Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(mainCastData.hand() == InteractionHand.MAIN_HAND,
                    "Extract should prefer the main hand filled flask");
            helper.assertTrue(ItemStack.isSameItemSameTags(mainCastData.storedItem(), splashPotion),
                    "Extract selected the wrong stored item from the main hand flask");

            magicData.setAdditionalCastData(null);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when only the offhand flask is filled");
            var offhandCastData = (Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(offhandCastData.hand() == InteractionHand.OFF_HAND,
                    "Extract should fall back to the offhand filled flask when the main hand flask is empty");
            helper.assertTrue(ItemStack.isSameItemSameTags(offhandCastData.storedItem(), lingeringPotion),
                    "Extract selected the wrong stored item from the offhand flask");
        });
    }

    static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when no Alchemist's Flask is held");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when the only Alchemist's Flask is empty");
        });
    }

    static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_cast_projectile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.REGENERATION);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a lingering potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var lingeringProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(lingeringProjectile.getItem().is(Items.LINGERING_POTION),
                    "Extract should throw a lingering potion when the flask stores a lingering potion");
            helper.assertTrue(AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 1,
                    "Extract should consume exactly one dose from the casting flask");

            lingeringProjectile.discard();
            magicData.setAdditionalCastData(null);

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION);
            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(normalPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a force-splash normal potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var forcedSplashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(forcedSplashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw normal potion contents as a splash potion");
            helper.assertTrue(AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 0,
                    "Extract should consume two doses when force-splashing a normal potion");
            forcedSplashProjectile.discard();
            magicData.setAdditionalCastData(null);

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(simpleElixir, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a Simple Elixir cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var splashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(splashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw Simple Elixir contents as a splash potion");
            helper.assertTrue(AbstractPotionFlaskItem.getStoredDoseCount(player.getOffhandItem()) == 1,
                    "Extract should consume exactly one offhand dose after a successful cast");
        });
    }

    static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.REGENERATION);
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            flask = SpellcastersFlask.copyWithAddedDoses(flask, storedPotion, 1);

            var thrownPotion = AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 1);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var extractedEffect = PotionUtils.getMobEffects(thrownPotion).get(0);

            helper.assertTrue(thrownPotion.is(Items.SPLASH_POTION),
                    "Extract should preserve non-lingering contents as splash potions");
            helper.assertTrue(extractedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Extract should add both Glow Energy and Extract amplification bonuses");
            helper.assertTrue(extractedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * 1.25F * 0.5F)),
                    "Extract should keep spell-side amplify while Glow Energy halves the Red Energy adjusted duration");
        });
    }

    static FakePlayer createExtractPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static ExtractPotionProjectileEntity getSingleExtractProjectile(
            GameTestHelper helper,
            FakePlayer owner
    ) {
        var projectiles = helper.getLevel().getEntitiesOfClass(
                ExtractPotionProjectileEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                projectile -> projectile.getOwner() == owner
        );
        helper.assertTrue(projectiles.size() == 1,
                "Expected exactly one Extract projectile but found " + projectiles.size());
        return projectiles.get(0);
    }

    public static void assertPotionEffect(
            GameTestHelper helper,
            Potion potion,
            String expectedPotionId,
            int expectedDuration,
            int expectedAmplifier
    ) {
        var potionId = ResourceLocation.parse(expectedPotionId);
        helper.assertTrue(BuiltInRegistries.POTION.get(potionId) == potion,
                "Missing potion registry entry: " + potionId);

        var effects = potion.getEffects();
        helper.assertTrue(effects.size() == 1,
                "Potion " + potionId + " should have exactly one effect but got " + effects.size());

        var effect = effects.isEmpty() ? null : effects.get(0);
        helper.assertTrue(effect != null && effect.getEffect() == EffectRegistry.MANA_REGENERATION.get(),
                "Potion " + potionId + " should grant mana regeneration");
        helper.assertTrue(effect != null && effect.getDuration() == expectedDuration,
                "Potion " + potionId + " duration regression: "
                        + (effect == null ? "missing" : effect.getDuration()));
        helper.assertTrue(effect != null && effect.getAmplifier() == expectedAmplifier,
                "Potion " + potionId + " amplifier regression: "
                        + (effect == null ? "missing" : effect.getAmplifier()));
    }

    static ItemStack createFilledAlchemistsFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    public static ItemStack createFilledSpellcastersFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }
}
