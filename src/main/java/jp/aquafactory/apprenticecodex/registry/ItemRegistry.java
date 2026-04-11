package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ArcaneCinderItem;
import jp.aquafactory.apprenticecodex.item.ArcanumInAJarItem;
import jp.aquafactory.apprenticecodex.item.AtelierStationItem;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.GrimoireManifest;
import jp.aquafactory.apprenticecodex.item.IlluminateStellarStaff;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.SpellcasterWorkbenchItem;
import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.UniteLunaStaff;
import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.curios.circlets.AshenCirclet;
import jp.aquafactory.apprenticecodex.item.curios.circlets.EnchantedCirclet;
import jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet.AbsorptionAmplifyAmulet;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.explorerscodex.ExplorersCodex;
import jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook.IsekaiTravelGuidebook;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.curios.ScarletThirst;
import jp.aquafactory.apprenticecodex.item.offhand.CopperSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.DiamondSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.item.offhand.GoldSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.IronSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.NetheriteSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.offhand.SilverSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.spellgun.CopperSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.DiamondSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.GoldSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.IronSpellcasterGun;
import net.minecraft.core.registries.Registries;
import jp.aquafactory.apprenticecodex.item.swingstaff.CopperSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.DiamondSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.GoldSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.NetheriteSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.SilverSwingcastStaff;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ItemRegistry {
    private ItemRegistry() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ApprenticeCodex.MODID);

    private static DeferredHolder<Item, Item> simple(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> block(String id, Supplier<? extends Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> armor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ApprenticeMageRobeItem(type));
    }

    private static DeferredHolder<Item, Item> enchantressArmor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new EnchantressRobeItem(type));
    }

    private static DeferredHolder<Item, Item> stealthRuneArmor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new StealthRuneArmorItem(type));
    }

    public static final DeferredHolder<Item, Item> SKY_EDGE_SWORD = simple("sky_edge_sword");
    public static final DeferredHolder<Item, Item> COMMENCE_FIRE_RIFLE = simple("commence_fire_rifle");
    public static final DeferredHolder<Item, Item> QUICK_ARMS_HANDGUN = simple("quick_arms_handgun");
    public static final DeferredHolder<Item, Item> BREACHING_ENEMY_SHOTGUN = simple("breaching_enemy_shotgun");
    public static final DeferredHolder<Item, Item> THERMAL_PROCESS_THROWER = simple("thermal_process_thrower");
    public static final DeferredHolder<Item, Item> FLY_SWATTER_LAUNCHER = simple("fly_swatter_launcher");
    public static final DeferredHolder<Item, Item> ARCANE_CINDER = ITEMS.register("arcane_cinder", ArcaneCinderItem::new);
    public static final DeferredHolder<Item, Item> COMFORT_BERRIES =
            ITEMS.register("comfort_berries", () -> new ItemNameBlockItem(
                    BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(1.2f)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(EffectRegistry.MANA_REGENERATION, 20 * 30), 1.0f)
                            .build())
            ));
    public static final DeferredHolder<Item, Item> SPELLSTAINED_ARCANE_INGOT = simple("spellstained_arcane_ingot");
    public static final DeferredHolder<Item, Item> EMPTY_RAPID_SPELLCASTER_CASING =
            ITEMS.register("empty_rapid_spellcaster_casing", () -> new SpellcasterRoundItem());
    public static final DeferredHolder<Item, Item> EMPTY_BASIC_SPELLCASTER_CASING =
            ITEMS.register("empty_basic_spellcaster_casing", () -> new SpellcasterRoundItem());
    public static final DeferredHolder<Item, Item> EMPTY_ARCANE_SPELLCASTER_CASING =
            ITEMS.register("empty_arcane_spellcaster_casing", () -> new SpellcasterRoundItem());
    public static final DeferredHolder<Item, Item> EMPTY_ADVANCED_SPELLCASTER_CASING =
            ITEMS.register("empty_advanced_spellcaster_casing", () -> new SpellcasterRoundItem());
    public static final DeferredHolder<Item, Item> EMPTY_SPELL_DOMINATOR_CASING =
            ITEMS.register("empty_spell_dominator_casing", () -> new SpellcasterRoundItem());
    public static final DeferredHolder<Item, Item> RAPID_SPELLCASTER_ROUND =
            ITEMS.register("rapid_spellcaster_round",
                    () -> new SpellcasterRoundItem(() -> EMPTY_RAPID_SPELLCASTER_CASING.get()));
    public static final DeferredHolder<Item, Item> BASIC_SPELLCASTER_ROUND =
            ITEMS.register("basic_spellcaster_round",
                    () -> new SpellcasterRoundItem(() -> EMPTY_BASIC_SPELLCASTER_CASING.get()));
    public static final DeferredHolder<Item, Item> ARCANE_SPELLCASTER_ROUND =
            ITEMS.register("arcane_spellcaster_round",
                    () -> new SpellcasterRoundItem(() -> EMPTY_ARCANE_SPELLCASTER_CASING.get()));
    public static final DeferredHolder<Item, Item> ADVANCED_SPELLCASTER_ROUND =
            ITEMS.register("advanced_spellcaster_round",
                    () -> new SpellcasterRoundItem(() -> EMPTY_ADVANCED_SPELLCASTER_CASING.get()));
    public static final DeferredHolder<Item, Item> SPELL_DOMINATOR_ROUND =
            ITEMS.register("spell_dominator_round",
                    () -> new SpellcasterRoundItem(() -> EMPTY_SPELL_DOMINATOR_CASING.get()));
    public static final DeferredHolder<Item, Item> APPRENTICE_MAGE_SCARF =
            armor("apprentice_mage_scarf", ArmorItem.Type.HELMET);
    public static final DeferredHolder<Item, Item> APPRENTICE_MAGE_TORSO =
            armor("apprentice_mage_torso", ArmorItem.Type.CHESTPLATE);
    public static final DeferredHolder<Item, Item> APPRENTICE_MAGE_LEGGINGS =
            armor("apprentice_mage_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredHolder<Item, Item> APPRENTICE_MAGE_BOOTS =
            armor("apprentice_mage_boots", ArmorItem.Type.BOOTS);
    public static final DeferredHolder<Item, Item> ENCHANTRESS_HAT =
            enchantressArmor("enchantress_hat", ArmorItem.Type.HELMET);
    public static final DeferredHolder<Item, Item> ENCHANTRESS_ROBE =
            enchantressArmor("enchantress_robe", ArmorItem.Type.CHESTPLATE);
    public static final DeferredHolder<Item, Item> ENCHANTRESS_LEGGINGS =
            enchantressArmor("enchantress_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredHolder<Item, Item> ENCHANTRESS_BOOTS =
            enchantressArmor("enchantress_boots", ArmorItem.Type.BOOTS);
    public static final DeferredHolder<Item, Item> STEALTH_RUNE_ARMOR_HEAD =
            stealthRuneArmor("stealth_rune_armor_head", ArmorItem.Type.HELMET);
    public static final DeferredHolder<Item, Item> STEALTH_RUNE_ARMOR_BODY =
            stealthRuneArmor("stealth_rune_armor_body", ArmorItem.Type.CHESTPLATE);
    public static final DeferredHolder<Item, Item> STEALTH_RUNE_ARMOR_LEG =
            stealthRuneArmor("stealth_rune_armor_leg", ArmorItem.Type.LEGGINGS);
    public static final DeferredHolder<Item, Item> STEALTH_RUNE_ARMOR_FOOT =
            stealthRuneArmor("stealth_rune_armor_foot", ArmorItem.Type.BOOTS);
    public static final DeferredHolder<Item, Item> APPRENTICE_DESK = block("apprentice_desk", BlockRegistry.APPRENTICE_DESK);
    public static final DeferredHolder<Item, Item> SPELLCASTER_WORKBENCH =
            ITEMS.register("spellcaster_workbench",
                    () -> new SpellcasterWorkbenchItem(BlockRegistry.SPELLCASTER_WORKBENCH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SPELL_DISPENSER = block("spell_dispenser", BlockRegistry.SPELL_DISPENSER);
    public static final DeferredHolder<Item, Item> ARCANUM_IN_A_JAR =
            ITEMS.register("arcanum_in_a_jar",
                    () -> new ArcanumInAJarItem(BlockRegistry.ARCANUM_IN_A_JAR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ESSENCE_SMOKER = block("essence_smoker", BlockRegistry.ESSENCE_SMOKER);
    public static final DeferredHolder<Item, Item> ATELIER_STATION =
            ITEMS.register("atelier_station",
                    () -> new AtelierStationItem(BlockRegistry.ATELIER_STATION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SCARLET_THIRST =
            ITEMS.register("scarlet_thirst", ScarletThirst::new);
    public static final DeferredHolder<Item, Item> CRAFTSMANS_DELIGHT =
            ITEMS.register("craftsmans_delight", CraftsmansDelight::new);
    public static final DeferredHolder<Item, Item> PROTECTION_SPELL_SUPPORTER =
            ITEMS.register("protection_spell_supporter", ProtectionSpellSupporter::new);
    public static final DeferredHolder<Item, Item> SPELLCASTER_AMMO_POUCH =
            ITEMS.register("spellcaster_ammo_pouch", SpellcasterAmmoPouch::new);
    public static final DeferredHolder<Item, Item> ABSORPTION_AMPLIFY_AMULET =
            ITEMS.register("absorption_amplify_amulet", AbsorptionAmplifyAmulet::new);
    public static final DeferredHolder<Item, Item> ASHEN_CIRCLET =
            ITEMS.register("ashen_circlet", AshenCirclet::new);
    public static final DeferredHolder<Item, Item> ENCHANTED_CIRCLET =
            ITEMS.register("enchanted_circlet", EnchantedCirclet::new);
    public static final DeferredHolder<Item, Item> ENDER_GRIMOIRE =
            ITEMS.register("ender_grimoire", EnderGrimoire::new);
    public static final DeferredHolder<Item, Item> EXPLORERS_CODEX =
            ITEMS.register("explorers_codex", ExplorersCodex::new);
    public static final DeferredHolder<Item, Item> ISEKAI_TRAVEL_GUIDEBOOK =
            ITEMS.register("isekai_travel_guidebook", IsekaiTravelGuidebook::new);
    public static final DeferredHolder<Item, Item> SPELLSTAINED_RUNIC_TABLET =
            ITEMS.register("spellstained_runic_tablet", SpellStainedRunicTablet::new);
    public static final DeferredHolder<Item, Item> IRON_SPELLCASTER_GUN =
            ITEMS.register("iron_spellcaster_gun", IronSpellcasterGun::new);
    public static final DeferredHolder<Item, Item> COPPER_SPELLCASTER_GUN =
            ITEMS.register("copper_spellcaster_gun", CopperSpellcasterGun::new);
    public static final DeferredHolder<Item, Item> GOLD_SPELLCASTER_GUN =
            ITEMS.register("gold_spellcaster_gun", GoldSpellcasterGun::new);
    public static final DeferredHolder<Item, Item> DIAMOND_SPELLCASTER_GUN =
            ITEMS.register("diamond_spellcaster_gun", DiamondSpellcasterGun::new);
    public static final DeferredHolder<Item, Item> IRON_SPELL_AMPLIFIER =
            ITEMS.register("iron_spell_amplifier", IronSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> COPPER_SPELL_AMPLIFIER =
            ITEMS.register("copper_spell_amplifier", CopperSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> GOLD_SPELL_AMPLIFIER =
            ITEMS.register("gold_spell_amplifier", GoldSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> DIAMOND_SPELL_AMPLIFIER =
            ITEMS.register("diamond_spell_amplifier", DiamondSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> SILVER_SPELL_AMPLIFIER =
            ITEMS.register("silver_spell_amplifier", SilverSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> NETHERITE_SPELL_AMPLIFIER =
            ITEMS.register("netherite_spell_amplifier", NetheriteSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> PHOTON_SIPHON =
            ITEMS.register("photon_siphon", PhotonSiphon::new);
    public static final DeferredHolder<Item, Item> EXPLORERS_CANE =
            ITEMS.register("explorers_cane", ExplorersCane::new);
    public static final DeferredHolder<Item, Item> SPELLCASTERS_FLASK =
            ITEMS.register("spellcasters_flask", SpellcastersFlask::new);
    public static final DeferredHolder<Item, Item> GRIMOIRE_MANIFEST =
            ITEMS.register("grimoire_manifest", GrimoireManifest::new);
    public static final DeferredHolder<Item, Item> PASTEL_STAFF =
            ITEMS.register("pastel_staff", PastelStaff::new);
    public static final DeferredHolder<Item, Item> COPPER_SWINGCAST_STAFF =
            ITEMS.register("copper_swingcast_staff", CopperSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> IRON_SWINGCAST_STAFF =
            ITEMS.register("iron_swingcast_staff", IronSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> SILVER_SWINGCAST_STAFF =
            ITEMS.register("silver_swingcast_staff", SilverSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> GOLD_SWINGCAST_STAFF =
            ITEMS.register("gold_swingcast_staff", GoldSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> DIAMOND_SWINGCAST_STAFF =
            ITEMS.register("diamond_swingcast_staff", DiamondSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> NETHERITE_SWINGCAST_STAFF =
            ITEMS.register("netherite_swingcast_staff", NetheriteSwingcastStaff::new);
    public static final DeferredHolder<Item, Item> CRYSTAL_BLADED_STAFF =
            ITEMS.register("crystal_bladed_staff", CrystalBladedStaff::new);
    public static final DeferredHolder<Item, Item> ILLUMINATE_STELLAR_STAFF =
            ITEMS.register("illuminate_stellar_staff", IlluminateStellarStaff::new);
    public static final DeferredHolder<Item, Item> UNITE_LUNA_STAFF =
            ITEMS.register("unite_luna_staff", UniteLunaStaff::new);
    public static final DeferredHolder<Item, Item> REFLECTCAST_SHIELD =
            ITEMS.register("reflectcast_shield", ReflectcastShield::new);
}
