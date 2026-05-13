package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
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
    private static final TagKey<Item> IRONS_IMBUE_WHITELIST = createTag("irons_spellbooks", "imbue_whitelist");
    private static final TagKey<Item> IRONS_UPGRADE_WHITELIST = createTag("irons_spellbooks", "upgrade_whitelist");
    private static final TagKey<Item> CURIOS_RING = createTag("curios", "ring");
    private static final TagKey<Item> CURIOS_BACK = createTag("curios", "back");
    private static final TagKey<Item> CURIOS_BELT = createTag("curios", "belt");
    private static final TagKey<Item> CURIOS_CHARM = createTag("curios", "charm");
    private static final TagKey<Item> CURIOS_HEAD = createTag("curios", "head");
    private static final TagKey<Item> CURIOS_NECKLACE = createTag("curios", "necklace");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = createTag("create", "contraption_controlled");
    private static final TagKey<Item> MINECRAFT_HEAD_ARMOR = createTag("minecraft", "head_armor");
    private static final TagKey<Item> MINECRAFT_CHEST_ARMOR = createTag("minecraft", "chest_armor");
    private static final TagKey<Item> MINECRAFT_LEG_ARMOR = createTag("minecraft", "leg_armor");
    private static final TagKey<Item> MINECRAFT_FOOT_ARMOR = createTag("minecraft", "foot_armor");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_BOW = createTag("minecraft", "enchantable/bow");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_SWORD = createTag("minecraft", "enchantable/sword");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_FIRE_ASPECT = createTag("minecraft", "enchantable/fire_aspect");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_SHARP_WEAPON = createTag("minecraft", "enchantable/sharp_weapon");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_WEAPON = createTag("minecraft", "enchantable/weapon");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_TRIDENT = createTag("minecraft", "enchantable/trident");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_DURABILITY = createTag("minecraft", "enchantable/durability");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_EQUIPPABLE = createTag("minecraft", "enchantable/equippable");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_MINING_LOOT = createTag("minecraft", "enchantable/mining_loot");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_VANISHING = createTag("minecraft", "enchantable/vanishing");
    private static final TagKey<Item> MALUM_MAGIC_CAPABLE_WEAPON = createTag("malum", "magic_capable_weapon");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = createTag("malum", "soul_shatter_capable_weapon");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> MAGIC_ITEM_ENCHANTABLE = Enchantments.MAGIC_ITEM_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_CONTAINER_MAGIC_ENCHANTABLE = Enchantments.SPELL_CONTAINER_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_GUN_ENCHANTABLE = Enchantments.SPELL_GUN_ENCHANTABLE;
    private static final TagKey<Item> SURGE_ENCHANTABLE = Enchantments.SURGE_ENCHANTABLE;
    private static final TagKey<Item> ATTUNEMENT_ENCHANTABLE = Enchantments.ATTUNEMENT_ENCHANTABLE;
    private static final TagKey<Item> DRINKABLE_FLASK_ENCHANTABLE = Enchantments.DRINKABLE_FLASK_ENCHANTABLE;
    private static final TagKey<Item> ALCHEMISTS_FLASK_ENCHANTABLE = Enchantments.ALCHEMISTS_FLASK_ENCHANTABLE;
    private static final TagKey<Item> FLASK_ENCHANTABLE = Enchantments.FLASK_ENCHANTABLE;
    private static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE = Enchantments.TRANSCENDENCE_ENCHANTABLE;
    private static final TagKey<Item> WISDOM_ENCHANTABLE = Enchantments.WISDOM_ENCHANTABLE;
    private static final TagKey<Item> PLUNDER_ENCHANTABLE = Enchantments.PLUNDER_ENCHANTABLE;
    private static final TagKey<Item> SYNTHESIS_ENCHANTABLE = Enchantments.SYNTHESIS_ENCHANTABLE;
    private static final TagKey<Item> MANA_SHIELD_CHARM_ENCHANTABLE = Enchantments.MANA_SHIELD_CHARM_ENCHANTABLE;

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
        tag(IRONS_STAFF).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(),
                ItemRegistry.UNITE_LUNA_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get()
        );
        // Iron's Spells 縺ｮ JEI 縺ｯ Imbue 蛟呵｣懷庶髮・凾縺ｫ spell_container 譛ｪ蛻晄悄蛹悶せ繧ｿ繝・け繧定誠縺ｨ縺吶◆繧√・
        // Autocast Amulet 縺ｯ whitelist 縺ｸ譏守､ｺ逋ｻ骭ｲ縺励※ JEI 荳翫〒繧・Arcane Anvil 蟇ｾ雎｡縺ｨ縺励※諡ｾ繧上○繧九・
        tag(IRONS_IMBUE_WHITELIST).add(ItemRegistry.AUTOCAST_AMULET.get());

        var ironsUpgradeWhitelist = tag(IRONS_UPGRADE_WHITELIST);
        var malumMagicCapableWeaponTag = tag(MALUM_MAGIC_CAPABLE_WEAPON);
        var malumSoulHunterWeaponTag = tag(MALUM_SOUL_HUNTER_WEAPON);
        var malumSoulShatterCapableWeaponTag = tag(MALUM_SOUL_SHATTER_CAPABLE_WEAPON);
        var tomagicReversalWeaponTag = tag(TOMAGIC_REVERSAL_WEAPON);
        var transcendenceEnchantableTag = tag(TRANSCENDENCE_ENCHANTABLE);
        var wisdomEnchantableTag = tag(WISDOM_ENCHANTABLE);
        var plunderEnchantableTag = tag(PLUNDER_ENCHANTABLE);
        var synthesisEnchantableTag = tag(SYNTHESIS_ENCHANTABLE);
        var surgeEnchantableTag = tag(SURGE_ENCHANTABLE);
        var attunementEnchantableTag = tag(ATTUNEMENT_ENCHANTABLE);
        var vanillaSwordEnchantableTag = tag(MINECRAFT_ENCHANTABLE_SWORD);
        var vanillaFireAspectEnchantableTag = tag(MINECRAFT_ENCHANTABLE_FIRE_ASPECT);
        var vanillaSharpWeaponEnchantableTag = tag(MINECRAFT_ENCHANTABLE_SHARP_WEAPON);
        var vanillaWeaponEnchantableTag = tag(MINECRAFT_ENCHANTABLE_WEAPON);
        var vanillaTridentEnchantableTag = tag(MINECRAFT_ENCHANTABLE_TRIDENT);
        var vanillaDurabilityEnchantableTag = tag(MINECRAFT_ENCHANTABLE_DURABILITY);
        ironsUpgradeWhitelist.add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get()
        );
        // Focus Staffbow 縺ｯ StaffItem 邯呎価縺ｫ萓晏ｭ倥○縺・Staff 逶ｸ蠖薙・荳ｻ謇九お繝ｳ繝√Ε髱｢繧呈戟縺溘○縺溘＞縺ｮ縺ｧ縲・
        // sword 邉ｻ tag 縺ｨ Malum 莠呈鋤 tag縲∝句挨莉倅ｸ弱・ Wisdom 繧呈・遉ｺ霑ｽ蜉縺吶ｋ縲・
        malumMagicCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        tomagicReversalWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        wisdomEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        synthesisEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());

        malumSoulHunterWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        tomagicReversalWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        wisdomEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        plunderEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());

        // Circuit Heat Staff 縺ｯ蜑｣逶ｸ蠖薙・荳ｻ謇区摶縺ｨ縺励※謇ｱ縺・∬蝉ｹ・ｳｻ繧帝勁縺・◆霑第磁豁ｦ蝎ｨ enchant 縺ｨ莠呈鋤 tag 繧帝壹☆縲・
        malumMagicCapableWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());

        // Multipurpose Staffrifle 縺ｯ main hand 縺ｧ蟆・茶謾ｻ謦・☆繧区ｭｦ蝎ｨ縺ｪ縺ｮ縺ｧ縲｀alum 縺ｮ荳ｻ謇区ｭｦ蝎ｨ tag 縺ｸ譏守､ｺ逋ｻ骭ｲ縺吶ｋ縲・
        tag(MAGIC_ITEM_ENCHANTABLE).add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        tag(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE).add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        surgeEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        wisdomEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        plunderEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());

        // Charged Twin Blade Staff 縺ｯ蜑｣/繝医Λ繧､繝・Φ繝井ｸ｡髱｢縺ｮ enchant 繧定ｨｱ蜿ｯ縺吶ｋ縺後∬蝉ｹ・ｳｻ縺ｨ雜・ｶ翫・髯､螟悶☆繧九・
        malumMagicCapableWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaTridentEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());

        // Mana Force Blade 縺ｯ閠蝉ｹ・､繧呈戟縺､蜑｣縺ｨ縺励※謇ｱ縺・∝ｰら畑鬲疲ｳ・enchant 繧・JSON/tag 髱｢縺ｧ騾壹☆縲・
        malumMagicCapableWeaponTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        tomagicReversalWeaponTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        surgeEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        attunementEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        transcendenceEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        wisdomEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        vanillaDurabilityEnchantableTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
        // Iron's 蛛ｴ縺ｮ upgrade 蛻､螳壹ち繧ｰ縺ｯ螳溘い繧､繝・Β蛻玲嫌縺ｪ縺ｮ縺ｧ縲∵歓雎｡蝓ｺ蠎輔け繝ｩ繧ｹ邯呎価蛻・ｒ閾ｪ蜍募庶髮・＠縺ｦ蜿悶ｊ縺薙⊂縺励ｒ髦ｲ縺舌・
        for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
            if (item instanceof AbstractOffhandMagicItem
                    || item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {

                // Iron's 蛛ｴ縺ｧ upgrade 蛻､螳壹ｒ隕九ｋ繧ｿ繧ｰ縺ｯ螳溘い繧､繝・Β蛻玲嫌縺励°縺ｧ縺阪↑縺・◆繧√・
                // 縺薙％縺ｧ謚ｽ雎｡繧ｯ繝ｩ繧ｹ邯呎価繧｢繧､繝・Β繧定・蜍募庶髮・＠縺ｦ霑ｽ蜉貍上ｌ繧帝亟縺舌・
                ironsUpgradeWhitelist.add(item);

                tomagicReversalWeaponTag.add(item);
            }

            if (item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {
                // 莉悶・豁ｦ蝎ｨ莠呈鋤邉ｻ繧ら匳骭ｲ縺吶ｋ.
                malumSoulHunterWeaponTag.add(item);
                malumSoulShatterCapableWeaponTag.add(item);

                // 1.21.1 縺ｧ縺ｯ Wisdom / Transcendence 縺ｮ驕ｩ逕ｨ蜿ｯ蜷ｦ縺・enchantment JSON 蛛ｴ縺ｮ item tag 縺ｧ豎ｺ縺ｾ繧九・
                // 蜿ｳ繧ｯ繝ｪ繝・け鬲疲ｳ墓ｭｦ蝎ｨ縺ｯ Java 蛛ｴ縺ｧ荳｡繧ｨ繝ｳ繝√Ε繧定ｨｱ蜿ｯ縺励※縺・ｋ縺溘ａ縲》ag 蛛ｴ繧ょ酔縺倬擇縺ｫ謠・∴繧九・
                if (item instanceof AbstractRightClickMagicWeaponItem) {
                    // 1.21.1 縺ｮ Haunted / Animated 縺ｯ magic_capable_weapon 繧ｿ繧ｰ蝓ｺ貅悶↑縺ｮ縺ｧ縲・
                    // 荳ｻ謇狗畑鬲疲ｳ墓ｭｦ蝎ｨ縺ｯ tag 縺ｨ Java 蛛ｴ蛻､螳壹ｒ蜷後§髱｢縺ｫ謠・∴繧九・
                    malumMagicCapableWeaponTag.add(item);
                    transcendenceEnchantableTag.add(item);
                    wisdomEnchantableTag.add(item);
                    vanillaSwordEnchantableTag.add(item);
                    vanillaFireAspectEnchantableTag.add(item);
                    vanillaSharpWeaponEnchantableTag.add(item);
                    vanillaWeaponEnchantableTag.add(item);
                }
            }
        }
        tag(CURIOS_SPELLBOOK).add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.EXPLORERS_CODEX.get(),
                ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get()
        );
        // 1.21.1 縺ｮ繝舌ル繝ｩ髦ｲ蜈ｷ enchant 縺ｯ item tag 蝓ｺ貅悶↓縺ｪ縺｣縺溘◆繧√・壼ｸｸ髦ｲ蜈ｷ逶ｸ蠖薙・蛻・｡槭∈蜈･繧後ｋ.
        tag(MINECRAFT_HEAD_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()
        );
        tag(MINECRAFT_CHEST_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()
        );
        tag(MINECRAFT_LEG_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()
        );
        tag(MINECRAFT_FOOT_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
        );
        vanillaDurabilityEnchantableTag.add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        tag(MINECRAFT_ENCHANTABLE_EQUIPPABLE).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
        );
        tag(MINECRAFT_ENCHANTABLE_VANISHING).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        // Elemental Bow 縺ｯ bow tag 縺ｫ蜿ょ刈縺輔○縲√ヰ繝九Λ蠑薙→蜷後§ enchantment JSON 髱｢繧剃ｽｿ縺・・
        tag(MINECRAFT_ENCHANTABLE_BOW).add(ItemRegistry.ELEMENTAL_BOW.get());
        // Elemental Bow 縺ｯ 1.21.1 縺ｧ繧・Wisdom / Transcendence / Plunder / Synthesis 繧貞句挨險ｱ蜿ｯ縺励◆縺・′縲・
        // spell_gun_enchantable 縺ｫ豺ｷ縺懊ｋ縺ｨ Attunement 縺ｾ縺ｧ騾壹▲縺ｦ縺励∪縺・◆繧∝ｰら畑繧ｿ繧ｰ蛛ｴ縺ｸ譏守､ｺ霑ｽ蜉縺吶ｋ縲・
        transcendenceEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        wisdomEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        plunderEnchantableTag.addTag(SPELL_GUN_ENCHANTABLE).add(ItemRegistry.ELEMENTAL_BOW.get());
        synthesisEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        // 1.21.1 縺ｮ繝舌ル繝ｩ enchantment JSON 縺ｯ Fortune / Silk Touch 繧・mining_loot 繧ｿ繧ｰ縺ｧ蛻､螳壹☆繧・
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.PASTEL_STAFF.get());
        malumMagicCapableWeaponTag.add(ItemRegistry.PASTEL_STAFF.get());
        tag(MALUM_SOUL_HUNTER_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        // 1.21.1 縺ｮ Spirit Plunder 縺ｯ soul_hunter_weapon 縺ｧ縺ｯ縺ｪ縺・soul_shatter_capable_weapon 邨檎罰縺ｧ supported_items 繧定ｦ九※縺・ｋ縲・
        // Java 蛛ｴ縺ｮ險ｱ蜿ｯ縺縺代〒縺ｯ definition 蛻､螳壹ｒ騾壹ｉ縺ｪ縺・◆繧√｀alum 蛛ｴ tag 繧ょ酔縺倬擇縺ｸ謠・∴繧九・
        tag(MALUM_SOUL_SHATTER_CAPABLE_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        tag(TOMAGIC_REVERSAL_WEAPON).add(ItemRegistry.PASTEL_STAFF.get());
        tag(MAGIC_ITEM_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(),
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get(),
                ItemRegistry.SILVER_SPELL_AMPLIFIER.get(),
                ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get(),
                ItemRegistry.EXPLORERS_CANE.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
        );
        // 1.21.1 縺ｮ enchantment JSON 縺ｯ supported_items tag 繧堤峩謗･蜿ら・縺吶ｋ縺溘ａ縲・
        // 鬟ｲ逕ｨ蟆ら畑縺ｮ Guzzle 縺ｨ蜈ｱ騾・flask enchant 鄒､繧貞・髮｢縺励※隱､驕ｩ逕ｨ繧帝亟縺舌・
        tag(DRINKABLE_FLASK_ENCHANTABLE).add(ItemRegistry.SPELLCASTERS_FLASK.get());
        // 骭ｬ驥題｡灘ｸｫ縺ｮ繝輔Λ繧ｹ繧ｳ縺ｯ Large/Red/Glow/Transcendence 縺ｮ縺ｿ繧定ｨｱ蜿ｯ縺励◆縺・・縺ｧ縲・
        // spell container 邉ｻ繧・Wisdom 縺ｨ豺ｷ邱壹＠縺ｪ縺・ｰら畑繧ｿ繧ｰ縺ｧ蛻・屬縺吶ｋ縲・
        tag(ALCHEMISTS_FLASK_ENCHANTABLE).add(ItemRegistry.ALCHEMISTS_FLASK.get());
        tag(FLASK_ENCHANTABLE)
                .addTag(DRINKABLE_FLASK_ENCHANTABLE)
                .addTag(ALCHEMISTS_FLASK_ENCHANTABLE);
        tag(MANA_SHIELD_CHARM_ENCHANTABLE).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        wisdomEnchantableTag.addTag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.ENCHANTED_CIRCLET.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
        );
        tag(OFFHAND_MAGIC_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get(),
                ItemRegistry.SILVER_SPELL_AMPLIFIER.get(),
                ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get(),
                ItemRegistry.EXPLORERS_CANE.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        // 莉伜測鬲泌･ｳ髦ｲ蜈ｷ縺ｯ 1.20.1 縺ｨ蜷梧ｧ倥↓蜿｡譎ｺ縺ｮ縺ｿ繧定ｨｱ蜿ｯ縺励∝ｰら畑繧ｨ繝ｳ繝√Ε邉ｻ繧ｿ繧ｰ縺九ｉ螟悶☆.
        tag(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE);
        tag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE)
                .addTag(SPELL_GUN_ENCHANTABLE);
        surgeEnchantableTag.addTag(MAGIC_ITEM_ENCHANTABLE);
        attunementEnchantableTag.addTag(SPELL_CONTAINER_MAGIC_ENCHANTABLE);
        transcendenceEnchantableTag
                .addTag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .addTag(ALCHEMISTS_FLASK_ENCHANTABLE);

        // 謖・ｼｪ.
        tag(CURIOS_RING).add(
                ItemRegistry.SCARLET_THIRST.get(),
                ItemRegistry.CRAFTSMANS_DELIGHT.get()
        );
        tag(CURIOS_BELT).add(
                ItemRegistry.PROTECTION_SPELL_SUPPORTER.get(),
                ItemRegistry.SPELLCASTER_AMMO_POUCH.get()
        );
        tag(CURIOS_BACK).add(ItemRegistry.SPELLCASTER_QUIVER.get());
        tag(CURIOS_NECKLACE).add(
                ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get(),
                ItemRegistry.AUTOCAST_AMULET.get()
        );
        tag(CURIOS_HEAD).add(
                ItemRegistry.ASHEN_CIRCLET.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(CURIOS_CHARM).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        tag(CREATE_CONTRAPTION_CONTROLLED).add(ItemRegistry.SPELL_DISPENSER.get());
        tag(TagRegistry.Items.SPELLCASTER_AMMO_POUCH_STORABLE).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get(),
                ItemRegistry.RAPID_SPELLCASTER_ROUND.get(),
                ItemRegistry.BASIC_SPELLCASTER_ROUND.get(),
                ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(),
                ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(),
                ItemRegistry.SPELL_DOMINATOR_ROUND.get(),
                ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()
        );
        tag(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()
        );
        tag(TagRegistry.Items.SPELLCASTER_QUIVER_STORABLE).add(
                net.minecraft.world.item.Items.ARROW,
                net.minecraft.world.item.Items.SPECTRAL_ARROW,
                net.minecraft.world.item.Items.TIPPED_ARROW
        );
        tag(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE).add(
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS).add(
                ItemRegistry.SMASHCAST_SCEPTER.get()
        );

        // 鬲疲ｳ募小蝟壽ｭｦ蝎ｨ縺ｯ繝繝溘・逕ｨ騾斐・縺溘ａ縲゛EI縺ｪ縺ｩ縺ｮ繝ｬ繧ｷ繝斐ン繝･繝ｼ繧｢縺九ｉ髯､螟悶☆繧・
        tag(HIDDEN_FROM_RECIPE_VIEWERS).add(
                ItemRegistry.SKY_EDGE_SWORD.get(),
                ItemRegistry.COMMENCE_FIRE_RIFLE.get(),
                ItemRegistry.QUICK_ARMS_HANDGUN.get(),
                ItemRegistry.BREACHING_ENEMY_SHOTGUN.get(),
                ItemRegistry.SILENT_ASSASSIN_RIFLE.get(),
                ItemRegistry.FLY_SWATTER_LAUNCHER.get(),
                ItemRegistry.THERMAL_PROCESS_THROWER.get()
        );
    }
}
