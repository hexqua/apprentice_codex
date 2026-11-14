package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SpectrumEnchantmentScenarios {
    private static final String CINDER = "apprenticecodex:arcane_cinder";
    private static final String SHARD = "apprenticecodex:crystalline_arcane_shard";
    private static final List<BookCase> BOOKS = List.of(
            new BookCase("surge", "purple", CINDER, "spectrum:storm_stone", 8, 1, 200),
            new BookCase("attunement", "green", CINDER, "spectrum:storm_stone", 8, 1, 200),
            new BookCase("tense", "magenta", CINDER, "minecraft:sugar", 8, 1, 200),
            new BookCase("alacrity", "yellow", CINDER, "minecraft:sugar", 8, 1, 200),
            new BookCase("reflux", "black", CINDER, "irons_spellbooks:arcane_essence", 8, 1, 400),
            new BookCase("reservoir", "cyan", CINDER, "irons_spellbooks:arcane_essence", 8, 1, 400),
            new BookCase("transcendence", "purple", CINDER, "apprenticecodex:wisdom_shard", 1, 0, 0),
            new BookCase("wisdom", "purple", "minecraft:experience_bottle", SHARD, 6, 1, 200),
            new BookCase("plunder", "light_blue", SHARD, "spectrum:four_leaf_clover", 6, 2, 200),
            new BookCase("red_energy", "red", "minecraft:redstone", "apprenticecodex:comfort_berries", 6, 2, 400),
            new BookCase("glow_energy", "yellow", "minecraft:glowstone_dust", "apprenticecodex:comfort_berries", 4, 2, 400),
            new BookCase("synthesis", "red", "spectrum:quitoxic_powder", CINDER, 1, 0, 0)
    );

    private SpectrumEnchantmentScenarios() {
    }

    static void reload(GameTestHelper helper, boolean spectrumLoaded) {
        var definitions = new HashMap<ResourceLocation, JsonElement>();
        var resources = helper.getLevel().getServer().getResourceManager();
        for (var test : BOOKS) {
            for (var kind : List.of("enchanter", "enchantment_upgrade")) {
                if (kind.equals("enchantment_upgrade") && test.cap() == 1) continue;
                var recipeId = id("spectrum/" + kind + "/" + test.enchantment());
                var resourceId = id("recipes/" + recipeId.getPath() + ".json");
                try (var reader = resources.getResource(resourceId).orElseThrow().openAsReader()) {
                    definitions.put(recipeId, JsonParser.parseReader(reader));
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read recipe: " + resourceId, exception);
                }
            }
        }
        var manager = new ReloadableRecipes();
        // 新規managerへ実resourceを2回適用し、Forge条件とSpectrumの段階展開を含む再読込を確認する。
        for (int reload = 0; reload < 2; reload++) {
            manager.reload(definitions, resources);
            int expected = spectrumLoaded ? 12 + BOOKS.stream().mapToInt(test -> test.cap() - 1).sum() : 0;
            long actual = manager.getRecipes().stream().filter(recipe ->
                    recipe.getId().getNamespace().equals(ApprenticeCodex.MODID)
                            || (recipe.getClass().getName().equals("de.dafuqs.spectrum.recipe.enchantment_upgrade.EnchantmentUpgradeRecipe")
                            && BOOKS.stream().anyMatch(test -> value(recipe, "getEnchantment") == test.enchantmentType()))).count();
            helper.assertTrue(actual == expected,
                    "Recipe reload must preserve the expected stage count: reload=" + reload
                            + " expected=" + expected + " actual=" + actual);
        }
    }

    static void books(GameTestHelper helper) {
        for (var test : BOOKS) {
            var recipe = recipe(helper, "enchanter/" + test.enchantment());
            var input = new SimpleContainer(10);
            input.setItem(0, new ItemStack(Items.BOOK));
            input.setItem(1, gem(helper, 100));
            for (int i = 0; i < 2; i++) {
                input.setItem(2 + i * 4, item(test.pigment()));
                input.setItem(3 + i * 4, item(test.pigment()));
                input.setItem(4 + i * 4, item(test.material1()));
                input.setItem(5 + i * 4, item(test.material2()));
            }
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Book recipe must match: " + test);
            helper.assertTrue((int) value(recipe, "getRequiredExperience") == 100
                            && (int) value(recipe, "getCraftingTime") == 300,
                    "Book recipe must require 100 XP and 300 ticks: " + test);
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK) && result.getCount() == 1
                            && EnchantmentHelper.getEnchantments(result).equals(Map.of(test.enchantmentType(), 1)),
                    "Book must contain only the requested level-one enchantment: " + test);
            helper.assertTrue(result.getTag() != null && result.getTag().getList("StoredEnchantments", Tag.TAG_COMPOUND)
                            .getCompound(0).contains("lvl", Tag.TAG_SHORT),
                    "Stored enchantment level must use a short NBT tag: " + test);
            input.setItem(4, new ItemStack(Items.DIRT));
            helper.assertFalse(recipe.matches(input, helper.getLevel()), "Wrong material must reject book: " + test);
            if (test.cap() == 1)
                helper.assertTrue(upgrades(helper, test).isEmpty(), "Single-level enchantment must not upgrade: " + test);
        }
        for (var omitted : List.of("large_mug", "guzzle", "shell", "synchronization", "neutralization")) {
            for (var kind : List.of("enchanter/", "enchantment_upgrade/")) {
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("spectrum/" + kind + omitted)).isEmpty(),
                        "Excluded enchantment must not have a Spectrum recipe: " + omitted);
            }
        }
    }

    static void upgrades(GameTestHelper helper) {
        for (var test : BOOKS) {
            if (test.cap() == 1) continue;
            var stages = upgrades(helper, test);
            helper.assertTrue(stages.size() == test.cap() - 1, "All upgrade stages must be registered: " + test);
            for (int level = 1; level < test.cap(); level++) {
                int destination = level + 1;
                var recipe = stages.stream().filter(stage -> (int) value(stage, "getEnchantmentDestinationLevel") == destination)
                        .findFirst().orElseThrow(() -> new IllegalStateException("Missing upgrade stage: " + test + " to " + destination));
                int count = test.initialCount() * (1 << (level - 1));
                int xp = test.initialXp() * (1 << (level - 1));
                helper.assertTrue((int) value(recipe, "getRequiredItemCount") == count * 8
                                && (int) value(recipe, "getRequiredExperience") == xp,
                        "Upgrade must preserve pigment and XP costs: " + test + " to " + destination);
                var input = upgradeInput(helper, test, level, count, xp);
                helper.assertTrue(recipe.matches(input, helper.getLevel()), "Exact resources must allow upgrade: " + test);
                input.getItem(2).shrink(1);
                helper.assertFalse(recipe.matches(input, helper.getLevel()), "One missing pigment must reject upgrade: " + test);
                input = upgradeInput(helper, test, level, count, xp - 1);
                helper.assertFalse(recipe.matches(input, helper.getLevel()), "One missing XP must reject upgrade: " + test);
                input = upgradeInput(helper, test, level, count, xp);
                input.setItem(2, new ItemStack(Items.DIRT, count));
                helper.assertFalse(recipe.matches(input, helper.getLevel()), "Wrong pigment must reject upgrade: " + test);

                var enchanter = enchanter(helper);
                prepare(enchanter, upgradeInput(helper, test, level, count, xp), true);
                // 上流の完成処理で保存本とXPを確認する。通常tickのマルチブロック処理とは区別する。
                call(enchanter.getClass(), null, "craftEnchantmentUpgradeRecipe",
                        new Class<?>[]{Level.class, enchanter.getClass(), recipe.getClass()}, helper.getLevel(), enchanter, recipe);
                var inventory = (Container) enchanter;
                helper.assertTrue(EnchantmentHelper.getEnchantments(inventory.getItem(0)).get(test.enchantmentType()) == destination,
                        "Completion must increase the stored book level: " + test);
                helper.assertTrue(storedXp(inventory.getItem(1)) == 0, "Completion must consume the required XP: " + test);
            }
        }
    }

    static void overenchanting(GameTestHelper helper) {
        for (var test : BOOKS) {
            if (test.cap() == 1) continue;
            int normalMax = test.enchantmentType().getMaxLevel();
            var enchanter = enchanter(helper);
            for (int source : new int[]{normalMax - 1, normalMax, test.cap() - 1, test.cap()}) {
                int dataLevel = Math.min(source, test.cap() - 1);
                for (boolean unlocked : new boolean[]{false, true}) {
                    prepare(enchanter, upgradeInput(helper, test, source,
                            test.initialCount() * (1 << (dataLevel - 1)), test.initialXp() * (1 << (dataLevel - 1))), unlocked);
                    setField(enchanter, "currentRecipe", null);
                    call(enchanter.getClass(), null, "calculateCurrentRecipe",
                            new Class<?>[]{Level.class, enchanter.getClass()}, helper.getLevel(), enchanter);
                    boolean expected = source < test.cap() && (unlocked || source < normalMax);
                    helper.assertTrue((field(enchanter, "currentRecipe") != null) == expected,
                            "Enchanter gate mismatch: " + test + " source=" + source + " normalMax=" + normalMax
                                    + " unlocked=" + unlocked + " expected=" + expected);
                }
            }
        }
    }

    private static SimpleContainer upgradeInput(GameTestHelper helper, BookCase test, int level, int count, int xp) {
        var input = new SimpleContainer(10);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(test.enchantmentType(), level));
        input.setItem(0, book);
        input.setItem(1, gem(helper, xp));
        for (int i = 2; i < 10; i++) input.setItem(i, item(test.pigment()).copyWithCount(count));
        return input;
    }

    @SuppressWarnings("unchecked")
    private static Recipe<Container> recipe(GameTestHelper helper, String path) {
        return (Recipe<Container>) helper.getLevel().getRecipeManager().byKey(id("spectrum/" + path))
                .orElseThrow(() -> new IllegalStateException("Missing Spectrum recipe: " + path));
    }

    @SuppressWarnings("unchecked")
    private static List<Recipe<Container>> upgrades(GameTestHelper helper, BookCase test) {
        // 1.8.15は各段階を別IDへ展開し、途中の段階は上流の名前空間へ移る。
        return helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getClass().getName().equals("de.dafuqs.spectrum.recipe.enchantment_upgrade.EnchantmentUpgradeRecipe"))
                .filter(recipe -> value(recipe, "getEnchantment") == test.enchantmentType())
                .map(recipe -> (Recipe<Container>) recipe).toList();
    }

    private static ItemStack gem(GameTestHelper helper, int xp) {
        var gem = item("spectrum:knowledge_gem");
        gem.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        call(type("de.dafuqs.spectrum.api.item.ExperienceStorageItem"), null, "addStoredExperience",
                new Class<?>[]{ItemStack.class, int.class}, gem, xp);
        helper.assertTrue(storedXp(gem) == xp, "Knowledge gem must store the requested XP: " + xp);
        return gem;
    }

    private static int storedXp(ItemStack gem) {
        return (int) call(type("de.dafuqs.spectrum.api.item.ExperienceStorageItem"), null, "getStoredExperience",
                new Class<?>[]{ItemStack.class}, gem);
    }

    private static BlockEntity enchanter(GameTestHelper helper) {
        try {
            var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("spectrum:enchanter"));
            if (block == null) throw new IllegalStateException("Missing Spectrum enchanter block");
            var enchanter = (BlockEntity) type("de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity")
                    .getConstructor(BlockPos.class, BlockState.class).newInstance(helper.absolutePos(new BlockPos(2, 2, 2)),
                            block.defaultBlockState());
            enchanter.setLevel(helper.getLevel());
            value(enchanter, "calculateUpgrades");
            return enchanter;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create Spectrum enchanter fixture", exception);
        }
    }

    private static void prepare(BlockEntity enchanter, SimpleContainer input, boolean unlocked) {
        var inventory = (Container) enchanter;
        inventory.setItem(0, input.getItem(0).copy());
        inventory.setItem(1, input.getItem(1).copy());
        // 1.8.15のsetItemは皿を含む仮インベントリを再構築するため、中央slotの設定後に接続する。
        setField(enchanter, "virtualInventoryIncludingBowlStacks", input);
        setField(enchanter, "canOwnerOverenchant", unlocked);
    }

    private static ItemStack item(String id) {
        var key = ResourceLocation.parse(id);
        var item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null) throw new IllegalStateException("Missing recipe item: " + id);
        return new ItemStack(item);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    // Connector変換後の型へ実行時だけ接続し、通常構成・datagenへFabric型を持ち込まない。
    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing Spectrum class: " + name, exception);
        }
    }

    private static Object value(Object target, String method) {
        return call(target.getClass(), target, method, new Class<?>[0]);
    }

    private static Object call(Class<?> owner, Object target, String method, Class<?>[] parameters, Object... arguments) {
        try {
            var reflected = owner.getDeclaredMethod(method, parameters);
            reflected.setAccessible(true);
            return reflected.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke Spectrum method: " + owner.getName() + "." + method, exception);
        }
    }

    private static Field reflectedField(Object target, String name) {
        try {
            var reflected = target.getClass().getDeclaredField(name);
            reflected.setAccessible(true);
            return reflected;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Missing Spectrum field: " + name, exception);
        }
    }

    private static Object field(Object target, String name) {
        try {
            return reflectedField(target, name).get(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read Spectrum field: " + name, exception);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            reflectedField(target, name).set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot write Spectrum field: " + name, exception);
        }
    }

    private record BookCase(String enchantment, String color, String material1, String material2, int cap,
                            int initialCount, int initialXp) {
        String pigment() {
            return "spectrum:" + color + "_pigment";
        }

        Enchantment enchantmentType() {
            var type = ForgeRegistries.ENCHANTMENTS.getValue(id(enchantment));
            if (type == null) throw new IllegalStateException("Missing recipe enchantment: " + enchantment);
            return type;
        }
    }

    private static final class ReloadableRecipes extends RecipeManager {
        ReloadableRecipes() {
            super(ICondition.IContext.EMPTY);
        }

        void reload(Map<ResourceLocation, JsonElement> definitions, ResourceManager resources) {
            apply(definitions, resources, InactiveProfiler.INSTANCE);
        }
    }
}
