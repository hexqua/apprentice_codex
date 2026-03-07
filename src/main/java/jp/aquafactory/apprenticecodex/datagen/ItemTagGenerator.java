package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemTagGenerator extends ItemTagsProvider {
    private static TagKey<Item> createTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final TagKey<Item> IRONS_STAFF = createTag("irons_spellbooks", "staff");
    private static final TagKey<Item> IRONS_UPGRADE_WHITELIST = createTag("irons_spellbooks", "upgrade_whitelist");
    private static final TagKey<Item> CURIOS_RING = createTag("curios", "ring");
    private static final TagKey<Item> CURIOS_BELT = createTag("curios", "belt");
    private static final TagKey<Item> CURIOS_NECKLACE = createTag("curios", "necklace");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_MINING_LOOT = createTag("minecraft", "enchantable/mining_loot");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_MAGIC_ENCHANTABLE;

    public ItemTagGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTagLookup,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTagLookup, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(IRONS_STAFF).add(ItemRegistry.PASTEL_STAFF.get());
        tag(IRONS_UPGRADE_WHITELIST).add(ItemRegistry.ENDER_GRIMOIRE.get());
        tag(CURIOS_SPELLBOOK).add(ItemRegistry.ENDER_GRIMOIRE.get());
        // 1.21.1 のバニラ enchantment JSON は Fortune / Silk Touch を mining_loot タグで判定する.
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.PASTEL_STAFF.get());
        tag(MALUM_SOUL_HUNTER_WEAPON).add(ItemRegistry.PASTEL_STAFF.get());
        tag(TOMAGIC_REVERSAL_WEAPON).add(ItemRegistry.PASTEL_STAFF.get());
        tag(OFFHAND_MAGIC_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get()
        );

        // 指輪.
        tag(CURIOS_RING).add(
                ItemRegistry.SCARLET_THIRST.get(),
                ItemRegistry.CRAFTSMANS_DELIGHT.get()
        );
        tag(CURIOS_BELT).add(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get());
        tag(CURIOS_NECKLACE).add(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get());

        // 魔法召喚武器はダミー用途のため、JEIなどのレシピビューアから除外する.
        tag(HIDDEN_FROM_RECIPE_VIEWERS).add(
                ItemRegistry.SKY_EDGE_SWORD.get(),
                ItemRegistry.COMMENCE_FIRE_RIFLE.get(),
                ItemRegistry.QUICK_ARMS_HANDGUN.get(),
                ItemRegistry.BREACHING_ENEMY_SHOTGUN.get(),
                ItemRegistry.FLY_SWATTER_LAUNCHER.get(),
                ItemRegistry.THERMAL_PROCESS_THROWER.get()
        );
    }
}
