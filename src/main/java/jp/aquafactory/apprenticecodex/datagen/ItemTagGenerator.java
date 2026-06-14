package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
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
    private static final TagKey<Item> CURIOS_FEET = createTag("curios", "feet");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = createTag("create", "contraption_controlled");
    private static final TagKey<Item> MINECRAFT_HEAD_ARMOR = createTag("minecraft", "head_armor");
    private static final TagKey<Item> MINECRAFT_CHEST_ARMOR = createTag("minecraft", "chest_armor");
    private static final TagKey<Item> MINECRAFT_LEG_ARMOR = createTag("minecraft", "leg_armor");
    private static final TagKey<Item> MINECRAFT_FOOT_ARMOR = createTag("minecraft", "foot_armor");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_BOW = createTag("minecraft", "enchantable/bow");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_MACE = createTag("minecraft", "enchantable/mace");
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
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(),
                ItemRegistry.UNITE_LUNA_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.ZENITH_STAFF.get()
        );
        // Iron's Spells の JEI は Imbue 候補収集時に spell_container 未初期化スタックを落とすため、
        // 後付系Curiosアクセ は whitelist へ明示登録して JEI 上でも Arcane Anvil 対象として拾わせる。
        tag(IRONS_IMBUE_WHITELIST).add(
                ItemRegistry.AUTOCAST_AMULET.get(),
                ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()
        );

        var ironsUpgradeWhitelist = tag(IRONS_UPGRADE_WHITELIST);
        var malumMagicCapableWeaponTag = tag(MALUM_MAGIC_CAPABLE_WEAPON);
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
        var vanillaMaceEnchantableTag = tag(MINECRAFT_ENCHANTABLE_MACE);
        var vanillaTridentEnchantableTag = tag(MINECRAFT_ENCHANTABLE_TRIDENT);
        var vanillaDurabilityEnchantableTag = tag(MINECRAFT_ENCHANTABLE_DURABILITY);
        ironsUpgradeWhitelist.add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.ZENITH_STAFF.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get()
        );
        // Focus Staffbow は StaffItem 継承に依存せず Staff 相当の主手エンチャ面を持たせたいので、
        // sword 系 tag と Malum 互換 tag、個別付与の Wisdom を明示追加する。
        malumMagicCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        tomagicReversalWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        wisdomEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        synthesisEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());

        // Zenith Staff は通常 staff として扱いつつ、機能する主手武器向けの互換 enchant/tag だけ明示登録する。
        malumMagicCapableWeaponTag.add(ItemRegistry.ZENITH_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.ZENITH_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.ZENITH_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.ZENITH_STAFF.get());
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.ZENITH_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.ZENITH_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.ZENITH_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.ZENITH_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.ZENITH_STAFF.get());

        // Multicast Echo Staff は StaffItem 継承だが、1.21.1 の tag 駆動 enchant と互換 MOD 判定にも明示登録する。
        malumMagicCapableWeaponTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.MULTICAST_ECHO_STAFF.get());

        // Smashcast Scepter はメイス相当の武器として、sword/sharp_weapon には入れずバニラメイスと同じ面を通す。
        tomagicReversalWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        transcendenceEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        wisdomEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        plunderEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        vanillaMaceEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        malumMagicCapableWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.SMASHCAST_SCEPTER.get());

        // Circuit Heat Staff は剣相当の主手杖として扱い、耐久系を除いた近接武器 enchant と互換 tag を通す。
        malumMagicCapableWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.CIRCUIT_HEAT_STAFF.get());

        // Scrollcaster Gauntlet は術式調整台でのみ enchant を同期するが、
        // 互換 MOD と 1.21.1 の enchantment JSON が見る item tag は主手武器相当へ揃える。
        malumMagicCapableWeaponTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        tomagicReversalWeaponTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        surgeEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        attunementEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        transcendenceEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        wisdomEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        plunderEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.SCROLLCASTER_GAUNTLET.get());

        // Multipurpose Staffrifle は main hand で射撃攻撃する武器なので、Malum の主手武器 tag へ明示登録する。
        tag(MAGIC_ITEM_ENCHANTABLE).add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        tag(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE).add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        surgeEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        wisdomEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        plunderEnchantableTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());

        // Charged Twin Blade Staff は剣/トライデント両面の enchant を許可するが、耐久系と超越は除外する。
        malumMagicCapableWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        tomagicReversalWeaponTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        vanillaTridentEnchantableTag.add(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());

        // Mana Force Blade は耐久値を持つ剣として扱い、専用魔法 enchant も JSON/tag 面で通す。
        malumMagicCapableWeaponTag.add(ItemRegistry.MANA_FORCE_BLADE.get());
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

        // Bound Sword は一時生成アイテムだが、1.21.1 の enchantment JSON/tag 判定にも剣相当として参加させる。
        vanillaSwordEnchantableTag.add(ItemRegistry.BOUND_SWORD.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.BOUND_SWORD.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.BOUND_SWORD.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.BOUND_SWORD.get());
        vanillaDurabilityEnchantableTag.add(ItemRegistry.BOUND_SWORD.get());
        // Iron's 側の upgrade 判定タグは実アイテム列挙なので、抽象基底クラス継承分を自動収集して取りこぼしを防ぐ。
        for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
            var isSmashcastScepter = item == ItemRegistry.SMASHCAST_SCEPTER.get();
            if (item instanceof AbstractOffhandMagicItem
                    || item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {

                // Iron's 側で upgrade 判定を見るタグは実アイテム列挙しかできないため、
                // ここで抽象クラス継承アイテムを自動収集して追加漏れを防ぐ。
                ironsUpgradeWhitelist.add(item);

                tomagicReversalWeaponTag.add(item);
            }

            if (item instanceof AbstractSpellGunItem || (item instanceof AbstractRightClickMagicWeaponItem && !isSmashcastScepter)) {
                // 他の武器互換系も登録する.
                malumSoulShatterCapableWeaponTag.add(item);

                // 1.21.1 では Wisdom / Transcendence の適用可否が enchantment JSON 側の item tag で決まる。
                // Java 側で Transcendence を拒否する武器は tag 側からも外し、適用面をそろえる。
                if (item instanceof AbstractRightClickMagicWeaponItem) {
                    // 1.21.1 の Haunted / Animated は magic_capable_weapon タグ基準なので、
                    // 主手用魔法武器は tag と Java 側判定を同じ面に揃える。
                    malumMagicCapableWeaponTag.add(item);
                    if (!(item instanceof MithrilFreecastStaff) && !(item instanceof RevolvercastStaff)) {
                        transcendenceEnchantableTag.add(item);
                    }
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
        // 1.21.1 のバニラ防具 enchant は item tag 基準になったため、通常防具相当の分類へ入れる.
        tag(MINECRAFT_HEAD_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()
        );
        tag(MINECRAFT_CHEST_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()
        );
        tag(MINECRAFT_LEG_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()
        );
        tag(MINECRAFT_FOOT_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
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
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get(),
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
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
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
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        wisdomEnchantableTag.add(
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
        );
        transcendenceEnchantableTag.add(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get());
        surgeEnchantableTag.add(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get());
        attunementEnchantableTag.add(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get());
        // Elemental Bow は bow tag に参加させ、バニラ弓と同じ enchantment JSON 面を使う。
        tag(MINECRAFT_ENCHANTABLE_BOW).add(ItemRegistry.ELEMENTAL_BOW.get());
        tag(MINECRAFT_ENCHANTABLE_BOW).add(ItemRegistry.BOUND_BOW.get());
        vanillaDurabilityEnchantableTag.add(ItemRegistry.BOUND_BOW.get());
        // Elemental Bow は 1.21.1 でも Wisdom / Transcendence / Plunder / Synthesis を個別許可したいが、
        // spell_gun_enchantable に混ぜると Attunement まで通ってしまうため専用タグ側へ明示追加する。
        transcendenceEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        wisdomEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        plunderEnchantableTag.addTag(SPELL_GUN_ENCHANTABLE).add(ItemRegistry.ELEMENTAL_BOW.get());
        synthesisEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        // 1.21.1 のバニラ enchantment JSON は Fortune / Silk Touch を mining_loot タグで判定する.
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.PASTEL_STAFF.get());
        malumMagicCapableWeaponTag.add(ItemRegistry.PASTEL_STAFF.get());
        wisdomEnchantableTag.add(ItemRegistry.PASTEL_STAFF.get());
        // 1.21.1 の Spirit Plunder は soul_shatter_capable_weapon 経由で supported_items を見ている。
        // Java 側の許可だけでは definition 判定を通らないため、Malum 側 tag も同じ面へ揃える。
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
        // spell gun 専用 enchant は offhand 補助具を巻き込まないように個別タグで分離する.
        tag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
        );
        // 1.21.1 の enchantment JSON は supported_items tag を直接参照するため、
        // 飲用専用の Guzzle と共通 flask enchant 群を分離して誤適用を防ぐ。
        tag(DRINKABLE_FLASK_ENCHANTABLE).add(ItemRegistry.SPELLCASTERS_FLASK.get());
        // 錬金術師のフラスコは Large/Red/Glow/Transcendence のみを許可したいので、
        // spell container 系や Wisdom と混線しない専用タグで分離する。
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
        // 付呪魔女防具は 1.20.1 と同様に叡智のみを許可し、専用エンチャ系タグから外す.
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

        tag(CURIOS_RING).add(
                ItemRegistry.SCARLET_THIRST.get(),
                ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                ItemRegistry.SPELL_CAST_PARRYING_RING.get()
        );
        tag(CURIOS_BELT).add(
                ItemRegistry.PROTECTION_SPELL_SUPPORTER.get(),
                ItemRegistry.SPELLCASTER_AMMO_POUCH.get(),
                ItemRegistry.MAGI_COMPRESSOR_GADGET.get()
        );
        tag(CURIOS_BACK).add(ItemRegistry.SPELLCASTER_QUIVER.get());
        tag(CURIOS_NECKLACE).add(
                ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get(),
                ItemRegistry.AUTOCAST_AMULET.get(),
                ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()
        );
        tag(CURIOS_HEAD).add(
                ItemRegistry.ASHEN_CIRCLET.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(CURIOS_CHARM).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        tag(CURIOS_FEET).add(
                ItemRegistry.MANA_THRUSTER.get(),
                ItemRegistry.JUMPCAST_CHARM.get()
        );

        tag(CREATE_CONTRAPTION_CONTROLLED).add(
                ItemRegistry.SPELL_DISPENSER.get(),
                ItemRegistry.CREATIVE_SPELL_DISPENSER.get()
        );
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
                net.minecraft.world.item.Items.MACE,
                ItemRegistry.SMASHCAST_SCEPTER.get()
        );
        tag(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
        );
        tag(TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS).add(
                net.minecraft.world.item.Items.ENCHANTED_BOOK
        );
        tag(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SCHOOL_RUNE_DENYLIST);
        tag(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
        );
        tag(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()
        );
        tag(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS).add(
                net.minecraft.world.item.Items.PAPER
        );
        tag(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS).add(
                net.minecraft.world.item.Items.BLACK_DYE,
                net.minecraft.world.item.Items.INK_SAC,
                net.minecraft.world.item.Items.GLOW_INK_SAC
        );
        tag(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS).add(
                net.minecraft.world.item.Items.ENDER_EYE
        );

        // 魔法召喚武器はダミー用途のため、JEIなどのレシピビューアから除外する.
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
