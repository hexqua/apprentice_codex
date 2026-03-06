package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ArcanumInAJarItem;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.offhand.CopperSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.GoldSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.IronSpellAmplifier;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.GrimoireManifest;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.ScarletThirst;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

    public static final DeferredHolder<Item, Item> SKY_EDGE_SWORD = simple("sky_edge_sword");
    public static final DeferredHolder<Item, Item> COMMENCE_FIRE_RIFLE = simple("commence_fire_rifle");
    public static final DeferredHolder<Item, Item> QUICK_ARMS_HANDGUN = simple("quick_arms_handgun");
    public static final DeferredHolder<Item, Item> BREACHING_ENEMY_SHOTGUN = simple("breaching_enemy_shotgun");
    public static final DeferredHolder<Item, Item> THERMAL_PROCESS_THROWER = simple("thermal_process_thrower");
    public static final DeferredHolder<Item, Item> FLY_SWATTER_LAUNCHER = simple("fly_swatter_launcher");
    public static final DeferredHolder<Item, Item> APPRENTICE_DESK = block("apprentice_desk", BlockRegistry.APPRENTICE_DESK);
    public static final DeferredHolder<Item, Item> ARCANUM_IN_A_JAR =
            ITEMS.register("arcanum_in_a_jar",
                    () -> new ArcanumInAJarItem(BlockRegistry.ARCANUM_IN_A_JAR.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> SCARLET_THIRST =
            ITEMS.register("scarlet_thirst", ScarletThirst::new);
    public static final DeferredHolder<Item, Item> CRAFTSMANS_DELIGHT =
            ITEMS.register("craftsmans_delight", CraftsmansDelight::new);
    public static final DeferredHolder<Item, Item> PROTECTION_SPELL_SUPPORTER =
            ITEMS.register("protection_spell_supporter", ProtectionSpellSupporter::new);
    public static final DeferredHolder<Item, Item> ENDER_GRIMOIRE =
            ITEMS.register("ender_grimoire", EnderGrimoire::new);
    public static final DeferredHolder<Item, Item> IRON_SPELL_AMPLIFIER =
            ITEMS.register("iron_spell_amplifier", IronSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> COPPER_SPELL_AMPLIFIER =
            ITEMS.register("copper_spell_amplifier", CopperSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> GOLD_SPELL_AMPLIFIER =
            ITEMS.register("gold_spell_amplifier", GoldSpellAmplifier::new);
    public static final DeferredHolder<Item, Item> PHOTON_SIPHON =
            ITEMS.register("photon_siphon", PhotonSiphon::new);
    public static final DeferredHolder<Item, Item> GRIMOIRE_MANIFEST =
            ITEMS.register("grimoire_manifest", GrimoireManifest::new);
    public static final DeferredHolder<Item, Item> PASTEL_STAFF =
            ITEMS.register("pastel_staff", PastelStaff::new);
}

