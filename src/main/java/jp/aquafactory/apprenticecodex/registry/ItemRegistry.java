package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.*;
import jp.aquafactory.apprenticecodex.item.apprenticedesk.CrudeInkItem;
import jp.aquafactory.apprenticecodex.item.apprenticedesk.PartiallyUsedInkItem;
import jp.aquafactory.apprenticecodex.item.armor.*;
import jp.aquafactory.apprenticecodex.item.blockitem.*;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundBowItem;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRing;
import jp.aquafactory.apprenticecodex.item.curios.circlets.AshenCirclet;
import jp.aquafactory.apprenticecodex.item.curios.circlets.EnchantedCirclet;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharm;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadget;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThruster;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet.AbsorptionAmplifyAmulet;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.explorerscodex.ExplorersCodex;
import jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook.IsekaiTravelGuidebook;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring.SpellCastParryingRing;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.magicitem.GrimoireManifest;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.item.magicitem.WoodenWand;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.offhand.CopperSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.DiamondSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.item.offhand.GoldSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.IronSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.NetheriteSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.offhand.SilverSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.ScarletThirst;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeSheathItem;
import jp.aquafactory.apprenticecodex.item.pastelstaff.PastelStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.SpellAutonomyCard;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.SpellInvokeCard;
import jp.aquafactory.apprenticecodex.item.spellgun.CopperSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.DiamondSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.GoldSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.spellgun.IronSpellcasterGun;
import jp.aquafactory.apprenticecodex.item.swingstaff.CopperSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.DiamondSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.GoldSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.NetheriteSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.SilverSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaff;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemRegistry {
    private ItemRegistry() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ApprenticeCodex.MODID);

    private static RegistryObject<Item> simple(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> block(String id, RegistryObject<Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> armor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ApprenticeMageRobeItem(type));
    }

    private static RegistryObject<Item> enchantressArmor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new EnchantressRobeItem(type));
    }

    private static RegistryObject<Item> soulcollectorArmor(String id, ArmorItem.Type type) {
        // Malumの導入に関係なく登録はする.
        return ITEMS.register(id, () -> new SoulcollectorRobeItem(type));
    }

    private static RegistryObject<Item> stealthRuneArmor(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new StealthRuneArmorItem(type));
    }

    private static RegistryObject<Item> chromaticMagiaDress(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ChromaticMagiaDressItem(type));
    }

    private static RegistryObject<Item> elementMaidenRobe(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ElementMaidenRobeItem(type));
    }

    private static RegistryObject<Item> magiAgentSuit(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new MagiAgentSuitItem(type));
    }

    public static final RegistryObject<Item> SKY_EDGE_SWORD = simple("sky_edge_sword");
    public static final RegistryObject<Item> BOUND_SWORD = ITEMS.register("bound_sword", BoundSwordItem::new);
    public static final RegistryObject<Item> BOUND_BOW = ITEMS.register("bound_bow", BoundBowItem::new);
    public static final RegistryObject<Item> COMMENCE_FIRE_RIFLE = simple("commence_fire_rifle");
    public static final RegistryObject<Item> QUICK_ARMS_HANDGUN = simple("quick_arms_handgun");
    public static final RegistryObject<Item> BREACHING_ENEMY_SHOTGUN = simple("breaching_enemy_shotgun");
    public static final RegistryObject<Item> SILENT_ASSASSIN_RIFLE = simple("silent_assassin_rifle");
    public static final RegistryObject<Item> LETHAL_ASSAULT_RIFLE = simple("lethal_assault_rifle");
    public static final RegistryObject<Item> DUAL_ACROBAT_SMG = simple("dual_acrobat_smg");
    public static final RegistryObject<Item> THERMAL_PROCESS_THROWER = simple("thermal_process_thrower");
    public static final RegistryObject<Item> FLY_SWATTER_LAUNCHER = simple("fly_swatter_launcher");
    public static final RegistryObject<Item> ARTISAN_SMASH_LAUNCHER = simple("artisan_smash_launcher");
    public static final RegistryObject<Item> ARCANE_CINDER = ITEMS.register("arcane_cinder", ArcaneCinderItem::new);
    public static final RegistryObject<Item> WISDOM_SHARD = ITEMS.register("wisdom_shard", WisdomShardItem::new);
    public static final RegistryObject<Item> SPELL_EXTRACT_SHARD = ITEMS.register("spell_extract_shard", SpellExtractShard::new);

    public static final RegistryObject<Item> COMFORT_BERRIES =
            ITEMS.register("comfort_berries", () -> new ItemNameBlockItem(
                    BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(1)
                            .saturationMod(1.2f)
                            .alwaysEat()
                            .effect(() -> new MobEffectInstance(EffectRegistry.MANA_REGENERATION.get(), 20 * 10, 2), 1.0f)
                            .build())
            ));

    public static final RegistryObject<Item> COMFORT_SANDWICH =
            ITEMS.register("comfort_sandwich", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(7)
                    .saturationMod(1.6f)
                    .alwaysEat()
                    .effect(() -> new MobEffectInstance(EffectRegistry.MANA_REGENERATION.get(), 20 * 60), 1.0f)
                    .build())));

    public static final RegistryObject<Item> SPELLSTAINED_ARCANE_INGOT = simple("spellstained_arcane_ingot");
    public static final RegistryObject<Item> ARCANE_PROPELLANT_CHARGE =
            ITEMS.register("arcane_propellant_charge", RoundIncompleteMaterialItem::new);
    public static final RegistryObject<Item> SPELL_BULLET_HEAD =
            ITEMS.register("spell_bullet_head", RoundIncompleteMaterialItem::new);
    public static final RegistryObject<Item> SPELL_BULLET_MOLD =
            ITEMS.register("spell_bullet_mold", RoundIncompleteMaterialItem::new);
    public static final RegistryObject<Item> SPELL_CASING_MOLD =
            ITEMS.register("spell_casing_mold", RoundIncompleteMaterialItem::new);
    public static final RegistryObject<Item> INCOMPLETE_SPELLCASTER_ROUND =
            ITEMS.register("incomplete_spellcaster_round", RoundIncompleteMaterialItem::new);
    public static final RegistryObject<Item> EMPTY_RAPID_SPELLCASTER_CASING =
            ITEMS.register("empty_rapid_spellcaster_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> EMPTY_BASIC_SPELLCASTER_CASING =
            ITEMS.register("empty_basic_spellcaster_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> EMPTY_ARCANE_SPELLCASTER_CASING =
            ITEMS.register("empty_arcane_spellcaster_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> EMPTY_ADVANCED_SPELLCASTER_CASING =
            ITEMS.register("empty_advanced_spellcaster_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> EMPTY_SPELL_DOMINATOR_CASING =
            ITEMS.register("empty_spell_dominator_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> EMPTY_MULTI_PURPOSE_SPELL_CASING =
            ITEMS.register("empty_multi_purpose_spell_casing", SpellcasterRoundItem::new);
    public static final RegistryObject<Item> RAPID_SPELLCASTER_ROUND =
            ITEMS.register("rapid_spellcaster_round",
                    () -> new SpellcasterRoundItem(EMPTY_RAPID_SPELLCASTER_CASING));
    public static final RegistryObject<Item> BASIC_SPELLCASTER_ROUND =
            ITEMS.register("basic_spellcaster_round",
                    () -> new SpellcasterRoundItem(EMPTY_BASIC_SPELLCASTER_CASING));
    public static final RegistryObject<Item> ARCANE_SPELLCASTER_ROUND =
            ITEMS.register("arcane_spellcaster_round",
                    () -> new SpellcasterRoundItem(EMPTY_ARCANE_SPELLCASTER_CASING));
    public static final RegistryObject<Item> ADVANCED_SPELLCASTER_ROUND =
            ITEMS.register("advanced_spellcaster_round",
                    () -> new SpellcasterRoundItem(EMPTY_ADVANCED_SPELLCASTER_CASING));
    public static final RegistryObject<Item> SPELL_DOMINATOR_ROUND =
            ITEMS.register("spell_dominator_round",
                    () -> new SpellcasterRoundItem(EMPTY_SPELL_DOMINATOR_CASING));
    public static final RegistryObject<Item> MULTI_PURPOSE_SPELL_ROUND =
            ITEMS.register("multi_purpose_spell_round",
                    () -> new SpellcasterRoundItem(
                            EMPTY_MULTI_PURPOSE_SPELL_CASING,
                            "item.apprenticecodex.multi_purpose_spell_round.desc"
                    ));
    public static final RegistryObject<Item> SPELL_INVOKE_CARD =
            ITEMS.register("spell_invoke_card", SpellInvokeCard::new);
    public static final RegistryObject<Item> SPELL_AUTONOMY_CARD =
            ITEMS.register("spell_autonomy_card", SpellAutonomyCard::new);
    public static final RegistryObject<Item> APPRENTICE_MAGE_SCARF =
            armor("apprentice_mage_scarf", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> APPRENTICE_MAGE_TORSO =
            armor("apprentice_mage_torso", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> APPRENTICE_MAGE_LEGGINGS =
            armor("apprentice_mage_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> APPRENTICE_MAGE_BOOTS =
            armor("apprentice_mage_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> ENCHANTRESS_HAT =
            enchantressArmor("enchantress_hat", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> ENCHANTRESS_ROBE =
            enchantressArmor("enchantress_robe", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> ENCHANTRESS_LEGGINGS =
            enchantressArmor("enchantress_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> ENCHANTRESS_BOOTS =
            enchantressArmor("enchantress_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> SOULCOLLECTOR_HAT =
            soulcollectorArmor("soulcollector_hat", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> SOULCOLLECTOR_ROBE =
            soulcollectorArmor("soulcollector_robe", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> SOULCOLLECTOR_LEGGINGS =
            soulcollectorArmor("soulcollector_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> SOULCOLLECTOR_BOOTS =
            soulcollectorArmor("soulcollector_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> STEALTH_RUNE_ARMOR_HEAD =
            stealthRuneArmor("stealth_rune_armor_head", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> STEALTH_RUNE_ARMOR_BODY =
            stealthRuneArmor("stealth_rune_armor_body", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> STEALTH_RUNE_ARMOR_LEG =
            stealthRuneArmor("stealth_rune_armor_leg", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> STEALTH_RUNE_ARMOR_FOOT =
            stealthRuneArmor("stealth_rune_armor_foot", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> CHROMATIC_MAGIA_DRESS_HAT =
            chromaticMagiaDress("chromatic_magia_dress_hat", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHROMATIC_MAGIA_DRESS_COAT =
            chromaticMagiaDress("chromatic_magia_dress_coat", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CHROMATIC_MAGIA_DRESS_LEGGINGS =
            chromaticMagiaDress("chromatic_magia_dress_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> CHROMATIC_MAGIA_DRESS_BOOTS =
            chromaticMagiaDress("chromatic_magia_dress_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> ELEMENT_MAIDEN_ROBE_RIBBON =
            elementMaidenRobe("element_maiden_robe_ribbon", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> ELEMENT_MAIDEN_ROBE_ROBE =
            elementMaidenRobe("element_maiden_robe_robe", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> ELEMENT_MAIDEN_ROBE_LEGGINGS =
            elementMaidenRobe("element_maiden_robe_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> ELEMENT_MAIDEN_ROBE_BOOTS =
            elementMaidenRobe("element_maiden_robe_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> MAGI_AGENT_SUIT_HOOD =
            magiAgentSuit("magi_agent_suit_hood", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> MAGI_AGENT_SUIT_COAT =
            magiAgentSuit("magi_agent_suit_coat", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> MAGI_AGENT_SUIT_LEGGINGS =
            magiAgentSuit("magi_agent_suit_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> MAGI_AGENT_SUIT_BOOTS =
            magiAgentSuit("magi_agent_suit_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> APPRENTICE_DESK =
            ITEMS.register("apprentice_desk",
                    () -> new ApprenticeDeskItem(BlockRegistry.APPRENTICE_DESK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRUDE_INK =
            ITEMS.register("crude_ink", CrudeInkItem::new);
    public static final RegistryObject<Item> PARTIALLY_USED_INK =
            ITEMS.register("partially_used_ink", PartiallyUsedInkItem::new);
    public static final RegistryObject<Item> SPELLCASTER_WORKBENCH =
            ITEMS.register("spellcaster_workbench",
                    () -> new SpellcasterWorkbenchItem(BlockRegistry.SPELLCASTER_WORKBENCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_CALIBRATION_BENCH =
            ITEMS.register("spell_calibration_bench",
                    () -> new SpellCalibrationBenchItem(BlockRegistry.SPELL_CALIBRATION_BENCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_DISPENSER =
            ITEMS.register("spell_dispenser",
                    () -> new SpellDispenserItem(BlockRegistry.SPELL_DISPENSER.get(), new Item.Properties()));
    public static final RegistryObject<Item> CREATIVE_SPELL_DISPENSER =
            ITEMS.register("creative_spell_dispenser",
                    () -> new SpellDispenserItem(BlockRegistry.CREATIVE_SPELL_DISPENSER.get(), new Item.Properties(), true));
    public static final RegistryObject<Item> ARCANUM_IN_A_JAR =
            ITEMS.register("arcanum_in_a_jar",
                    () -> new ArcanumInAJarItem(BlockRegistry.ARCANUM_IN_A_JAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ESSENCE_SMOKER = block("essence_smoker", BlockRegistry.ESSENCE_SMOKER);
    public static final RegistryObject<Item> ATELIER_STATION =
            ITEMS.register("atelier_station",
                    () -> new AtelierStationItem(BlockRegistry.ATELIER_STATION.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCARLET_THIRST =
            ITEMS.register("scarlet_thirst", ScarletThirst::new);
    public static final RegistryObject<Item> CRAFTSMANS_DELIGHT =
            ITEMS.register("craftsmans_delight", CraftsmansDelight::new);
    public static final RegistryObject<Item> PROTECTION_SPELL_SUPPORTER =
            ITEMS.register("protection_spell_supporter", ProtectionSpellSupporter::new);
    public static final RegistryObject<Item> SPELLCASTER_AMMO_POUCH =
            ITEMS.register("spellcaster_ammo_pouch", SpellcasterAmmoPouch::new);
    public static final RegistryObject<Item> SPELLCASTER_QUIVER =
            ITEMS.register("spellcaster_quiver", SpellcasterQuiver::new);
    public static final RegistryObject<Item> ABSORPTION_AMPLIFY_AMULET =
            ITEMS.register("absorption_amplify_amulet", AbsorptionAmplifyAmulet::new);
    public static final RegistryObject<Item> AUTOCAST_AMULET =
            ITEMS.register("autocast_amulet", AutocastAmulet::new);
    public static final RegistryObject<Item> SATELLITE_FOLLOWCAST_AMULET =
            ITEMS.register("satellite_followcast_amulet", SatelliteFollowcastAmulet::new);
    public static final RegistryObject<Item> MANA_THRUSTER =
            ITEMS.register("mana_thruster", ManaThruster::new);
    public static final RegistryObject<Item> MAGI_COMPRESSOR_GADGET =
            ITEMS.register("magi_compressor_gadget", MagiCompressorGadget::new);
    public static final RegistryObject<Item> JUMPCAST_CHARM =
            ITEMS.register("jumpcast_charm", JumpcastCharm::new);
    public static final RegistryObject<Item> SPELL_CAST_PARRYING_RING =
            ITEMS.register("spell_cast_parrying_ring", SpellCastParryingRing::new);
    public static final RegistryObject<Item> ATTACKCAST_RING =
            ITEMS.register("attackcast_ring", AttackcastRing::new);
    public static final RegistryObject<Item> ASHEN_CIRCLET =
            ITEMS.register("ashen_circlet", AshenCirclet::new);
    public static final RegistryObject<Item> ENCHANTED_CIRCLET =
            ITEMS.register("enchanted_circlet", EnchantedCirclet::new);
    public static final RegistryObject<Item> MANA_SHIELD_CHARM =
            ITEMS.register("mana_shield_charm", ManaShieldCharm::new);
    public static final RegistryObject<Item> ENDER_GRIMOIRE =
            ITEMS.register("ender_grimoire", EnderGrimoire::new);
    public static final RegistryObject<Item> ARCHIVISTS_GRIMOIRE =
            ITEMS.register("archivists_grimoire", ArchivistsGrimoire::new);
    public static final RegistryObject<Item> EXPLORERS_CODEX =
            ITEMS.register("explorers_codex", ExplorersCodex::new);
    public static final RegistryObject<Item> ISEKAI_TRAVEL_GUIDEBOOK =
            ITEMS.register("isekai_travel_guidebook", IsekaiTravelGuidebook::new);
    public static final RegistryObject<Item> SPELLSTAINED_RUNIC_TABLET =
            ITEMS.register("spellstained_runic_tablet", SpellStainedRunicTablet::new);
    public static final RegistryObject<Item> IRON_SPELLCASTER_GUN =
            ITEMS.register("iron_spellcaster_gun", IronSpellcasterGun::new);
    public static final RegistryObject<Item> COPPER_SPELLCASTER_GUN =
            ITEMS.register("copper_spellcaster_gun", CopperSpellcasterGun::new);
    public static final RegistryObject<Item> GOLD_SPELLCASTER_GUN =
            ITEMS.register("gold_spellcaster_gun", GoldSpellcasterGun::new);
    public static final RegistryObject<Item> DIAMOND_SPELLCASTER_GUN =
            ITEMS.register("diamond_spellcaster_gun", DiamondSpellcasterGun::new);
    public static final RegistryObject<Item> IRON_SPELL_AMPLIFIER =
            ITEMS.register("iron_spell_amplifier", IronSpellAmplifier::new);
    public static final RegistryObject<Item> COPPER_SPELL_AMPLIFIER =
            ITEMS.register("copper_spell_amplifier", CopperSpellAmplifier::new);
    public static final RegistryObject<Item> GOLD_SPELL_AMPLIFIER =
            ITEMS.register("gold_spell_amplifier", GoldSpellAmplifier::new);
    public static final RegistryObject<Item> DIAMOND_SPELL_AMPLIFIER =
            ITEMS.register("diamond_spell_amplifier", DiamondSpellAmplifier::new);
    public static final RegistryObject<Item> SILVER_SPELL_AMPLIFIER =
            ITEMS.register("silver_spell_amplifier", SilverSpellAmplifier::new);
    public static final RegistryObject<Item> NETHERITE_SPELL_AMPLIFIER =
            ITEMS.register("netherite_spell_amplifier", NetheriteSpellAmplifier::new);
    public static final RegistryObject<Item> PHOTON_SIPHON =
            ITEMS.register("photon_siphon", PhotonSiphon::new);
    public static final RegistryObject<Item> EXPLORERS_CANE =
            ITEMS.register("explorers_cane", ExplorersCane::new);
    public static final RegistryObject<Item> SPELLCASTERS_FLASK =
            ITEMS.register("spellcasters_flask", SpellcastersFlask::new);
    public static final RegistryObject<Item> ALCHEMISTS_FLASK =
            ITEMS.register("alchemists_flask", AlchemistsFlask::new);
    public static final RegistryObject<Item> GRIMOIRE_MANIFEST =
            ITEMS.register("grimoire_manifest", GrimoireManifest::new);
    public static final RegistryObject<Item> WOODEN_WAND =
            ITEMS.register("wooden_wand", WoodenWand::new);
    public static final RegistryObject<Item> PASTEL_STAFF =
            ITEMS.register("pastel_staff", PastelStaff::new);
    public static final RegistryObject<Item> MULTICAST_ECHO_STAFF =
            ITEMS.register("multicast_echo_staff", MulticastEchoStaff::new);
    public static final RegistryObject<Item> ZENITH_STAFF =
            ITEMS.register("zenith_staff", ZenithStaff::new);
    public static final RegistryObject<Item> FOCUS_STAFFBOW =
            ITEMS.register("focus_staffbow", FocusStaffbow::new);
    public static final RegistryObject<Item> SMASHCAST_SCEPTER =
            ITEMS.register("smashcast_scepter", SmashcastScepter::new);
    public static final RegistryObject<Item> MULTIPURPOSE_STAFFRIFLE =
            ITEMS.register("multipurpose_staffrifle", MultipurposeStaffrifle::new);
    public static final RegistryObject<Item> SCROLLCASTER_GAUNTLET =
            ITEMS.register("scrollcaster_gauntlet", ScrollcasterGauntlet::new);
    public static final RegistryObject<Item> CHARGECAST_CATALYSTBOOK =
            ITEMS.register("chargecast_catalystbook", ChargecastCatalystbook::new);
    public static final RegistryObject<Item> STORAGE_STABILIZER =
            ITEMS.register("storage_stabilizer", StorageStabilizer::new);
    public static final RegistryObject<Item> LUMINOUS_DEVICE =
            ITEMS.register("luminous_device", LuminousDevice::new);
    public static final RegistryObject<Item> CIRCUIT_HEAT_STAFF =
            ITEMS.register("circuit_heat_staff", CircuitHeatStaff::new);
    public static final RegistryObject<Item> CHARGED_TWIN_BLADE_STAFF =
            ITEMS.register("charged_twin_blade_staff", ChargedTwinBladeStaff::new);
    public static final RegistryObject<Item> MANA_FORCE_BLADE =
            ITEMS.register("mana_force_blade", ManaForceBlade::new);
    public static final RegistryObject<Item> MANA_FORCE_BLADE_SHEATH =
            ITEMS.register("mana_force_blade_sheath", ManaForceBladeSheathItem::new);
    public static final RegistryObject<Item> SPELL_SIDE_EDGE =
            ITEMS.register("spell_side_edge", SpellSideEdge::new);
    public static final RegistryObject<Item> SPELL_SIDE_EDGE_MIRROR =
            ITEMS.register("spell_side_edge_mirror", SpellSideEdgeMirror::new);
    public static final RegistryObject<Item> SPELLCHARGED_GREATSWORD =
            ITEMS.register("spellcharged_greatsword", SpellchargedGreatsword::new);
    public static final RegistryObject<Item> COPPER_SWINGCAST_STAFF =
            ITEMS.register("copper_swingcast_staff", CopperSwingcastStaff::new);
    public static final RegistryObject<Item> IRON_SWINGCAST_STAFF =
            ITEMS.register("iron_swingcast_staff", IronSwingcastStaff::new);
    public static final RegistryObject<Item> SILVER_SWINGCAST_STAFF =
            ITEMS.register("silver_swingcast_staff", SilverSwingcastStaff::new);
    public static final RegistryObject<Item> GOLD_SWINGCAST_STAFF =
            ITEMS.register("gold_swingcast_staff", GoldSwingcastStaff::new);
    public static final RegistryObject<Item> DIAMOND_SWINGCAST_STAFF =
            ITEMS.register("diamond_swingcast_staff", DiamondSwingcastStaff::new);
    public static final RegistryObject<Item> NETHERITE_SWINGCAST_STAFF =
            ITEMS.register("netherite_swingcast_staff", NetheriteSwingcastStaff::new);
    public static final RegistryObject<Item> MITHRIL_FREECAST_STAFF =
            ITEMS.register("mithril_freecast_staff", MithrilFreecastStaff::new);
    public static final RegistryObject<Item> REVOLVERCAST_STAFF =
            ITEMS.register("revolvercast_staff", RevolvercastStaff::new);
    public static final RegistryObject<Item> CRYSTAL_BLADED_STAFF =
            ITEMS.register("crystal_bladed_staff", CrystalBladedStaff::new);
    public static final RegistryObject<Item> ILLUMINATE_STELLAR_STAFF =
            ITEMS.register("illuminate_stellar_staff", IlluminateStellarStaff::new);
    public static final RegistryObject<Item> UNITE_LUNA_STAFF =
            ITEMS.register("unite_luna_staff", UniteLunaStaff::new);
    public static final RegistryObject<Item> ELEMENTAL_BOW =
            ITEMS.register("elemental_bow", ElementalBow::new);
    public static final RegistryObject<Item> REFLECTCAST_SHIELD =
            ITEMS.register("reflectcast_shield", ReflectcastShield::new);
    public static final RegistryObject<Item> PARRYCAST_BUCKLER =
            ITEMS.register("parrycast_buckler", ParrycastBuckler::new);
    public static final RegistryObject<Item> BULWARK_GREATSHIELD =
            ITEMS.register("bulwark_greatshield", BulwarkGreatshield::new);
}
