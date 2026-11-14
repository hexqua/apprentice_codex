package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import de.dafuqs.spectrum.api.item.ExperienceStorageItem;
import de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity;
import de.dafuqs.spectrum.inventories.EnchanterInventory;
import de.dafuqs.spectrum.recipe.enchanter.EnchanterRecipe;
import de.dafuqs.spectrum.recipe.enchanter.EnchantmentUpgradeRecipe;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

final class SpectrumEnchantmentScenarios {
    private static final String CINDER = "apprenticecodex:arcane_cinder";
    private static final String SHARD = "apprenticecodex:crystalline_arcane_shard";
    private static final List<BookCase> BOOKS = List.of(
            new BookCase("surge", "purple", CINDER, "spectrum:storm_stone", 8),
            new BookCase("attunement", "green", CINDER, "spectrum:storm_stone", 8),
            new BookCase("tense", "magenta", CINDER, "minecraft:sugar", 8),
            new BookCase("alacrity", "yellow", CINDER, "minecraft:sugar", 8),
            new BookCase("reflux", "black", CINDER, "irons_spellbooks:arcane_essence", 8),
            new BookCase("reservoir", "cyan", CINDER, "irons_spellbooks:arcane_essence", 8),
            new BookCase("transcendence", "purple", CINDER, "apprenticecodex:wisdom_shard", 1),
            new BookCase("wisdom", "purple", "minecraft:experience_bottle", SHARD, 6),
            new BookCase("plunder", "light_blue", SHARD, "spectrum:four_leaf_clover", 6),
            new BookCase("red_energy", "red", "minecraft:redstone", "apprenticecodex:comfort_berries", 6),
            new BookCase("glow_energy", "yellow", "minecraft:glowstone_dust", "apprenticecodex:comfort_berries", 4),
            new BookCase("synthesis", "red", "spectrum:quitoxic_powder", CINDER, 1)
    );

    private SpectrumEnchantmentScenarios() {}

    static void books(GameTestHelper helper) {
        for (var test : BOOKS) {
            var holder = recipe(helper, "enchanter/" + test.enchantment());
            helper.assertTrue(holder.value() instanceof EnchanterRecipe, "Wrong book recipe type: " + holder.id());
            var recipe = (EnchanterRecipe) holder.value();
            var inventory = new EnchanterInventory();
            inventory.setItem(0, new ItemStack(Items.BOOK));
            inventory.setItem(1, gem(helper, 1000));
            for (int i = 0; i < 2; i++) {
                inventory.setItem(2 + i * 4, item(test.pigment()));
                inventory.setItem(3 + i * 4, item(test.pigment()));
                inventory.setItem(4 + i * 4, item(test.material1()));
                inventory.setItem(5 + i * 4, item(test.material2()));
            }
            helper.assertTrue(recipe.matches(inventory.createInput(), helper.getLevel()),
                    "Book recipe must match its nine ingredients: " + holder.id());
            var result = recipe.assemble(inventory.createInput(), helper.getLevel().registryAccess());
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK) && result.getCount() == 1,
                    "Recipe must produce one enchanted book: " + holder.id());
            var enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(test.key());
            helper.assertTrue(enchantments != null && enchantments.size() == 1 && enchantments.getLevel(enchantment) == 1,
                    "Book must contain only the requested level-one enchantment: " + holder.id());
            inventory.setItem(4, new ItemStack(Items.DIRT));
            helper.assertFalse(recipe.matches(inventory.createInput(), helper.getLevel()),
                    "Wrong material must reject book creation: " + holder.id());
            if (test.cap() == 1) {
                helper.assertTrue(helper.getLevel().getRecipeManager()
                                .byKey(id("spectrum/enchantment_upgrade/" + test.enchantment())).isEmpty(),
                        "Single-level enchantment must not have an upgrade recipe: " + test.enchantment());
            }
        }
        for (var omitted : List.of("large_mug", "guzzle", "shell", "synchronization", "neutralization")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("spectrum/enchanter/" + omitted)).isEmpty(),
                    "Excluded enchantment must not have a Spectrum book recipe: " + omitted);
        }
    }

    static void upgrades(GameTestHelper helper) {
        for (var test : BOOKS) {
            if (test.cap() == 1) continue;
            var recipe = (EnchantmentUpgradeRecipe) recipe(helper, "enchantment_upgrade/" + test.enchantment()).value();
            for (int level = 1; level < test.cap(); level++) {
                var data = recipe.getForSourceLevel(level);
                helper.assertTrue(data.countPerBowl() > 0 && data.countPerBowl() <= 64,
                        "Upgrade must fit eight bowls: " + test.enchantment() + " level=" + level);
                var inventory = upgradeInput(helper, test, level, data.countPerBowl(), data.experience());
                helper.assertTrue(recipe.matches(inventory.createInput(), helper.getLevel()),
                        "Exact resources must allow upgrade: " + test.enchantment() + " level=" + level);
                inventory.getItem(2).shrink(1);
                helper.assertFalse(recipe.matches(inventory.createInput(), helper.getLevel()),
                        "One missing pigment must reject upgrade: " + test.enchantment() + " level=" + level);
                inventory = upgradeInput(helper, test, level, data.countPerBowl(), data.experience() - 1);
                helper.assertFalse(recipe.matches(inventory.createInput(), helper.getLevel()),
                        "One missing experience point must reject upgrade: " + test.enchantment() + " level=" + level);
                inventory = upgradeInput(helper, test, level, data.countPerBowl(), data.experience());
                inventory.setItem(2, new ItemStack(Items.DIRT, data.countPerBowl()));
                helper.assertFalse(recipe.matches(inventory.createInput(), helper.getLevel()),
                        "Wrong pigment must reject upgrade: " + test.enchantment());

                var enchanter = new Harness(helper);
                enchanter.prepare(upgradeInput(helper, test, level, data.countPerBowl(), data.experience()), true);
                // 材料境界はmatchesで検証し、完成処理は上流の実装を直接呼んで本とXPの変化を観測する。
                enchanter.craftEnchantmentUpgradeRecipe(helper.getLevel(), recipe);
                var stored = enchanter.getItem(0).get(DataComponents.STORED_ENCHANTMENTS);
                helper.assertTrue(stored != null && stored.getLevel(recipe.getEnchantment()) == level + 1,
                        "Upgrade completion must increase the stored book level: " + test.enchantment());
                helper.assertTrue(ExperienceStorageItem.getStoredExperience(enchanter.getItem(1)) == 0,
                        "Upgrade completion must consume the required experience: " + test.enchantment());
            }
        }
    }

    static void overenchanting(GameTestHelper helper) {
        for (var test : BOOKS) {
            if (test.cap() == 1) continue;
            var holder = recipe(helper, "enchantment_upgrade/" + test.enchantment());
            var recipe = (EnchantmentUpgradeRecipe) holder.value();
            int normalMax = recipe.getEnchantment().value().getMaxLevel();
            var enchanter = new Harness(helper);
            for (int sourceLevel : new int[]{normalMax - 1, normalMax, test.cap() - 1, test.cap()}) {
                int dataLevel = Math.min(sourceLevel, test.cap() - 1);
                var data = recipe.getForSourceLevel(dataLevel);
                for (boolean unlocked : new boolean[]{false, true}) {
                    enchanter.prepare(upgradeInput(helper, test, sourceLevel, data.countPerBowl(), data.experience()), unlocked);
                    boolean expected = sourceLevel < test.cap() && (unlocked || sourceLevel < normalMax);
                    helper.assertTrue(enchanter.selects(holder) == expected,
                            "Enchanter gate mismatch: " + test.enchantment() + " source=" + sourceLevel
                                    + " unlocked=" + unlocked + " expected=" + expected);
                }
            }
        }
    }

    private static EnchanterInventory upgradeInput(GameTestHelper helper, BookCase test, int level, int count, int xp) {
        var inventory = new EnchanterInventory();
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(test.key()), level);
        inventory.setItem(0, book);
        inventory.setItem(1, gem(helper, xp));
        for (int i = 2; i < 10; i++) inventory.setItem(i, item(test.pigment()).copyWithCount(count));
        return inventory;
    }

    private static RecipeHolder<?> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager().byKey(id("spectrum/" + path))
                .orElseThrow(() -> new IllegalStateException("Missing Spectrum recipe: " + path));
    }

    private static ItemStack gem(GameTestHelper helper, int xp) {
        var gem = item("spectrum:knowledge_gem");
        // 高段階のXPは未強化の宝石容量を超えるため、通常のEfficiency Vで容量を確保する。
        gem.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY), 5);
        ExperienceStorageItem.addStoredExperience(helper.getLevel().registryAccess(), gem, xp);
        helper.assertTrue(ExperienceStorageItem.getStoredExperience(gem) == xp,
                "The knowledge gem fixture must store the requested experience: " + xp);
        return gem;
    }

    private static ItemStack item(String id) {
        var key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Missing recipe item: " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    private record BookCase(String enchantment, String color, String material1, String material2, int cap) {
        String pigment() { return "spectrum:" + color + "_pigment"; }
        ResourceKey<Enchantment> key() { return ResourceKey.create(Registries.ENCHANTMENT, id(enchantment)); }
    }

    private static final class Harness extends EnchanterBlockEntity {
        Harness(GameTestHelper helper) {
            super(helper.absolutePos(new BlockPos(2, 2, 2)),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse("spectrum:enchanter")).defaultBlockState());
            setLevel(helper.getLevel());
            calculateUpgrades(helper.getLevel());
        }

        void prepare(EnchanterInventory inventory, boolean unlocked) {
            virtualInventory = inventory;
            items.set(0, inventory.getItem(0).copy());
            items.set(1, inventory.getItem(1).copy());
            // 所有者進捗の保存済み判定を切り替え、上流装置の実際のレシピ拒否境界を検証する。
            canOwnerOverenchant = unlocked;
        }

        boolean selects(RecipeHolder<?> recipe) {
            try {
                // privateな装置判定だけへ接続する。マルチブロック描画・通常tickの再現とは区別する。
                Field current = EnchanterBlockEntity.class.getDeclaredField("currentRecipe");
                current.setAccessible(true);
                current.set(this, null);
                Method calculate = EnchanterBlockEntity.class.getDeclaredMethod("calculateCurrentRecipe", Level.class, EnchanterBlockEntity.class);
                calculate.setAccessible(true);
                calculate.invoke(null, getLevel(), this);
                var selected = (RecipeHolder<?>) current.get(this);
                return selected != null && selected.id().equals(recipe.id());
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot inspect Spectrum enchanter recipe gate", exception);
            }
        }

        @Override
        public void updateInClientWorld() {
            // worldへ登録しない検証用装置なので、client同期は不要。
        }
    }
}
