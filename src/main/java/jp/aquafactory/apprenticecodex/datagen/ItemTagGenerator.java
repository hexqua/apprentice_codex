package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemTagGenerator extends ItemTagsProvider {
    private static TagKey<Item> createTag(String namespace, String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
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
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");

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

        // 自前の抽象クラスを継承しないアイテムは後の自動収集から漏れるのでここで直接指定する.
        ironsUpgradeWhitelist.add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get()
        );

        var malumSoulHunterWeaponTag = tag(MALUM_SOUL_HUNTER_WEAPON);
        malumSoulHunterWeaponTag.add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                ItemRegistry.ZENITH_STAFF.get(),
                // Malum の soul_hunter_weapon 実発動判定は main hand を見るため、
                // offhand 専用品はタグ対象から外し、main hand で攻撃成立する盾だけ明示的に残す。
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );

        var tomagicReversalWeaponTag = tag(TOMAGIC_REVERSAL_WEAPON);
        tomagicReversalWeaponTag.add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                ItemRegistry.ZENITH_STAFF.get()
        );

        // 所謂魔法武器全般を自動で登録するようにする.
        for (RegistryObject<Item> itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
            if (item instanceof AbstractOffhandMagicItem
                    || item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {

                // Iron's 側で upgrade 判定を見るタグは実アイテム列挙しかできないため、
                // ここで抽象クラス継承アイテムを自動収集して追加漏れを防ぐ。
                ironsUpgradeWhitelist.add(item);

                tomagicReversalWeaponTag.add(item);
            }

            if (item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {
                // 他の武器互換系も登録する.
                // 1.21.1申し送り事項:CrystalBladedStaffは1.21.1だとStaffItemで登録が漏れるため、別途登録.
                // (重複するとタグが2回出てしまうので1.20.1では個別登録していない)
                malumSoulHunterWeaponTag.add(item);
            }
        }
        tag(CURIOS_SPELLBOOK).add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.EXPLORERS_CODEX.get(),
                ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get()
        );

        // 指輪.
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
                ItemRegistry.AUTOCAST_AMULET.get(),
                ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()
        );
        tag(CURIOS_HEAD).add(
                ItemRegistry.ASHEN_CIRCLET.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(CURIOS_CHARM).add(ItemRegistry.MANA_SHIELD_CHARM.get());
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

        // 魔法召喚武器はアイテムとして性能を持たずダミーにしか使っていないため、JEIでも表示しないようにする.
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
