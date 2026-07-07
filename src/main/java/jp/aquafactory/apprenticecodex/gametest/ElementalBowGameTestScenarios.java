package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastAnchorMode;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCasterMode;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaFluidHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserVariant;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellThrowableCardServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.event.ErrandMageVillagerTradesEvent;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import jp.aquafactory.apprenticecodex.event.ScrollcasterGauntletGrindstoneEvent;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaffRightClickItemEvent;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffPendingAdvance;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffManaCostEvent;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffPowerHelper;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import jp.aquafactory.apprenticecodex.item.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverPickupEvent;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWings;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsManager;
import jp.aquafactory.apprenticecodex.spell.divinepossession.DivinePossessionPowerHelper;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarManager;
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.harvestmoon.HarvestMoon;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceBurst;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileEntity;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShield;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldDefenseEvent;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldShieldEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxCounterSpellEvent;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackBlockClassifier;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonEntity;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressStats;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeStats;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.LootConditionRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeStats;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import jp.aquafactory.apprenticecodex.utility.ProcessingRecipeDenylist;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static jp.aquafactory.apprenticecodex.gametest.BowGameTestSupport.*;
final class ElementalBowGameTestScenarios {
    private ElementalBowGameTestScenarios() {
    }

    static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            assertReferenceItemEnchantmentsWithRequiredExtras(
                    helper,
                    stack,
                    new ItemStack(Items.BOW),
                    requiredElementalBowExtraEnchantments(),
                    "Elemental Bow"
            );
        });
    }
    static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_view_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var regenerationArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
            var regenerationId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(regenerationArrow));
            helper.assertTrue(healingId != null && regenerationId != null,
                    "Elemental Bow selection view test could not resolve tipped arrow potion ids");
            var availablePotionIds = new LinkedHashSet<ResourceLocation>();
            if (healingId != null) {
                availablePotionIds.add(healingId);
            }
            if (regenerationId != null) {
                availablePotionIds.add(regenerationId);
            }
            var expectedPotionOrder = java.util.stream.StreamSupport.stream(BuiltInRegistries.POTION.spliterator(), false)
                    .map(BuiltInRegistries.POTION::getKey)
                    .filter(id -> id != null && availablePotionIds.contains(id))
                    .toList();

            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));
            player.getInventory().setItem(2, healingArrow);
            player.getInventory().setItem(3, regenerationArrow);

            var views = ElementalBow.getAvailableSelectionViews(player, stack);
            var actualSelections = views.stream()
                    .map(ApprenticeCodexGameTestScenarios::describeElementalBowSelectionView)
                    .toList();
            var expectedSelections = new ArrayList<String>();
            expectedSelections.add("normal");
            expectedSelections.add("arrow");
            expectedSelections.add("special:minecraft:spectral_arrow");
            for (var potionId : expectedPotionOrder) {
                expectedSelections.add("special:" + potionId);
            }
            expectedSelections.add("magic:" + SchoolRegistry.FIRE_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.ENDER_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(actualSelections.equals(expectedSelections),
                    "Elemental Bow selection view order mismatch: expected=" + expectedSelections + ", actual=" + actualSelections);
            helper.assertTrue(views.get(0).iconStack().is(Items.BOW),
                    "Elemental Bow vanilla mode selection should render as a bow icon");
            helper.assertTrue(views.get(0).badgeText() == null,
                    "Elemental Bow vanilla mode selection should not show an ammo badge");
            helper.assertTrue(views.get(1).iconStack().is(Items.ARROW),
                    "Elemental Bow arrow-only selection should render as an arrow icon");
            helper.assertTrue("∞".equals(views.get(1).badgeText()),
                    "Elemental Bow arrow-only selection should show infinity while Infinity is enchanted: " + views.get(1).badgeText());

            var fireView = views.stream()
                    .filter(view -> "magic".equals(view.selection().shotMode()) && SchoolRegistry.FIRE_RESOURCE.equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(fireView != null, "Elemental Bow selection view should include Fire magic");
            if (fireView != null) {
                helper.assertTrue(fireView.iconKind() == ElementalBow.SelectionIconKind.SPELL,
                        "Elemental Bow magic selection should render as a spell icon");
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get().getSpellIconResource().equals(fireView.spellIcon()),
                        "Elemental Bow Fire magic selection should use the Fire Arrow spell icon");
            }
        });
    }
    static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(ElementalBow.getInventoryOverlayView(stack) == null,
                    "Elemental Bow normal mode should not expose an inventory overlay");

            setElementalBowShotSelection(stack, "arrow", null);
            var arrowOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(arrowOverlay != null,
                    "Elemental Bow arrow-only selection should expose an inventory overlay");
            if (arrowOverlay != null) {
                helper.assertTrue(arrowOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow arrow-only selection should render as an item overlay");
                helper.assertTrue(arrowOverlay.iconStack().is(Items.ARROW),
                        "Elemental Bow arrow-only selection should render the arrow icon");
            }

            var spectralArrowId = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
            setElementalBowShotSelection(stack, "special", spectralArrowId);
            var spectralOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(spectralOverlay != null,
                    "Elemental Bow spectral selection should expose an inventory overlay");
            if (spectralOverlay != null) {
                helper.assertTrue(spectralOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow spectral selection should render as an item overlay");
                helper.assertTrue(spectralOverlay.iconStack().is(Items.SPECTRAL_ARROW),
                        "Elemental Bow spectral selection should render the spectral arrow icon");
            }

            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
            helper.assertTrue(healingId != null,
                    "Elemental Bow overlay test could not resolve the healing arrow potion id");
            if (healingId != null) {
                setElementalBowShotSelection(stack, "special", healingId);
                var tippedOverlay = ElementalBow.getInventoryOverlayView(stack);
                helper.assertTrue(tippedOverlay != null,
                        "Elemental Bow tipped arrow selection should expose an inventory overlay");
                if (tippedOverlay != null) {
                    helper.assertTrue(tippedOverlay.iconStack().is(Items.TIPPED_ARROW),
                            "Elemental Bow tipped arrow selection should render a tipped arrow icon");
                    helper.assertTrue(PotionContentsHelper.getPotion(tippedOverlay.iconStack()) == net.minecraft.world.item.alchemy.Potions.HEALING.value(),
                            "Elemental Bow tipped arrow overlay should keep the selected potion");
                }
            }

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            var fireOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(fireOverlay != null,
                    "Elemental Bow magic selection should expose an inventory overlay");
            if (fireOverlay != null) {
                helper.assertTrue(fireOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow magic selection should render as an item overlay");
                helper.assertTrue(fireOverlay.iconStack().is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Elemental Bow Fire mode should render the Fire rune icon");
            }
        });
    }
    static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_overheat_overlay_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        setElementalBowMode(stack, "fire");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.runAtTickTime(1, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow overheat overlay test should expose the Fire magic selection");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should not be overheated before any cast");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection should start with an empty overheat overlay");
            }

            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    40
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    20
            );

            var overheatedFireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(overheatedFireView != null && overheatedFireView.overheatActive(),
                    "Elemental Bow Fire selection should report active overheat immediately after cast");
            if (overheatedFireView != null) {
                helper.assertTrue(overheatedFireView.overheatFillRatio() == 1.0F,
                        "Elemental Bow Fire selection should start with a full overheat overlay: " + overheatedFireView.overheatFillRatio());
            }
        });

        helper.runAtTickTime(11, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null && fireView.overheatActive(),
                    "Elemental Bow Fire selection should still be overheated mid-cooldown");
            if (fireView != null) {
                helper.assertTrue(Mth.equal(fireView.overheatFillRatio(), 0.75F),
                        "Elemental Bow Fire selection should decay based on its own cooldown: " + fireView.overheatFillRatio());
            }

            var natureView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureView != null && natureView.overheatActive(),
                    "Elemental Bow Nature selection should track its own overheat independently");
            if (natureView != null) {
                helper.assertTrue(Mth.equal(natureView.overheatFillRatio(), 0.5F),
                        "Elemental Bow Nature selection should show its shorter cooldown independently: " + natureView.overheatFillRatio());
            }

            var enderView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderView != null, "Elemental Bow overheat overlay test should expose the Ender magic selection");
            if (enderView != null) {
                helper.assertFalse(enderView.overheatActive(),
                        "Elemental Bow Ender selection should stay inactive when untouched");
                helper.assertTrue(enderView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Ender selection should not show an overheat overlay");
            }
        });

        helper.runAtTickTime(42, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow Fire selection should remain in the selection list after cooldown");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should clear overheat after cooldown expires");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection overlay should be empty after cooldown expires");
            }
        });

        helper.runAtTickTime(43, helper::succeed);
    }

    static void elementalBowClampsPersistedFutureOverheat(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_future_overheat_test");

        helper.runAtTickTime(1, () -> {
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    40
            );
            var schoolTag = player.getPersistentData()
                    .getCompound("ApprenticeCodexElementalBowOverheat")
                    .getCompound(SchoolRegistry.FIRE_RESOURCE.toString());
            schoolTag.putLong("ExpireGameTime", player.level().getGameTime() + 72000L);

            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(
                    player,
                    SchoolRegistry.FIRE_RESOURCE
            );

            helper.assertTrue(state.expireGameTime() <= player.level().getGameTime() + 40L,
                    "Elemental Bow stored overheat should be clamped to the last applied duration");
            helper.assertTrue(schoolTag.getLong("ExpireGameTime") == state.expireGameTime(),
                    "Elemental Bow persistent overheat NBT should be rewritten after clamping");
            helper.succeed();
        });
    }

    static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_empty_selection_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            var selectedViews = ElementalBow.getAvailableSelectionViews(player, stack);
            var spectralView = selectedViews.stream()
                    .filter(view -> "special".equals(view.selection().shotMode())
                            && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(spectralView != null, "Elemental Bow should keep the empty current special selection in the UI");
            if (spectralView != null) {
                helper.assertTrue("0".equals(spectralView.badgeText()),
                        "Elemental Bow empty current special selection should show 0 ammo");
                helper.assertTrue(spectralView.badgeColor() == 0xFF5555,
                        "Elemental Bow empty current special selection should render its ammo count in red");
            }

            setElementalBowShotSelection(stack, "normal", null);
            var normalViews = ElementalBow.getAvailableSelectionViews(player, stack);
            helper.assertTrue(normalViews.stream().noneMatch(view ->
                            "special".equals(view.selection().shotMode())
                                    && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId())),
                    "Elemental Bow should drop the empty special selection after another mode is chosen");
            var arrowView = normalViews.stream()
                    .filter(view -> "arrow".equals(view.selection().shotMode()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(arrowView != null, "Elemental Bow should always expose the arrow-only selection");
            if (arrowView != null) {
                helper.assertTrue("0".equals(arrowView.badgeText()),
                        "Elemental Bow arrow-only selection should show 0 ammo while empty");
                helper.assertTrue(arrowView.badgeColor() == 0xFF5555,
                        "Elemental Bow arrow-only selection should render empty ammo in red even while another mode is selected");
            }
        });
    }
    static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_mana_gate_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow mana gate test could not resolve player mana data");
            magicData.setMana(0.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow should fail to start drawing when mana is insufficient: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow should not enter use state without enough mana");
        });
    }
    static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowMode", "fire"));

            item.initializeSpellContainer(stack);

            assertElementalBowSelection(helper, stack, null, null,
                    "Elemental Bow should clear unresolved legacy mode values back to normal mode");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container after falling back to normal mode");
        });
    }

    static void elementalBowSynchronizesSpellContainerToCurrentMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            setElementalBowMode(stack, SchoolRegistry.FIRE_RESOURCE.toString());
            item.initializeSpellContainer(stack);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Elemental Bow should expose a spell container outside NONE mode");
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Elemental Bow should keep its derived spell out of the spell wheel");
            assertSpellData(
                    helper,
                    spellContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    1,
                    true,
                    "Elemental Bow should sync Fire mode into a locked spell container"
            );
        });
    }

    static void elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var power = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);
            var flame = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FLAME);
            var transcendence = enchantmentLookup.getOrThrow(Enchantments.TRANSCENDENCE);
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.enchant(power, 2);
            stack.enchant(transcendence, 1);
            stack.enchant(flame, 1);

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            item.initializeSpellContainer(stack);
            helper.assertTrue(getEnchantmentLevel(stack, power) == 2,
                    "Elemental Bow spell container test should preserve POWER II on the stack");
            helper.assertTrue(getEnchantmentLevel(stack, flame) == 1,
                    "Elemental Bow spell container test should preserve FLAME I on the stack");
            helper.assertTrue(getEnchantmentLevel(stack, transcendence) == 1,
                    "Elemental Bow spell container test should preserve TRANSCENDENCE I on the stack");
            var fireMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireMode != null, "Elemental Bow Fire mode should resolve from the loaded mode definitions");
            var expectedFireLevel = fireMode != null ? fireMode.resolveSpellLevel(stack) : 1;
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow should expose a displayed spell profile in Fire mode");
            helper.assertTrue(fireProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    "Elemental Bow Fire mode should resolve Fire Arrow");
            helper.assertTrue(fireProfile.spellLevel() == expectedFireLevel,
                    "Elemental Bow Fire mode display level should stay in sync with the loaded mode resolver but got " + fireProfile.spellLevel());
            var fireContainer = ISpellContainer.get(stack);
            helper.assertTrue(fireContainer != null, "Elemental Bow Fire mode should keep a synced spell container");
            helper.assertTrue(fireContainer != null && !fireContainer.isSpellWheel(),
                    "Elemental Bow Fire mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    fireContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    expectedFireLevel,
                    true,
                    "Elemental Bow Fire mode container should stay in sync with the loaded mode resolver"
            );

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            item.initializeSpellContainer(stack);
            var enderMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderMode != null, "Elemental Bow Ender mode should resolve from the loaded mode definitions");
            var expectedEnderLevel = enderMode != null ? enderMode.resolveSpellLevel(stack) : 1;
            var enderProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(enderProfile != null, "Elemental Bow should expose a displayed spell profile in Ender mode");
            helper.assertTrue(enderProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    "Elemental Bow Ender mode should resolve Magic Arrow");
            helper.assertTrue(enderProfile.spellLevel() == expectedEnderLevel,
                    "Elemental Bow Ender mode display level should stay in sync with the loaded mode resolver but got " + enderProfile.spellLevel());
            var enderContainer = ISpellContainer.get(stack);
            helper.assertTrue(enderContainer != null, "Elemental Bow Ender mode should keep a synced spell container");
            helper.assertTrue(enderContainer != null && !enderContainer.isSpellWheel(),
                    "Elemental Bow Ender mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    enderContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    expectedEnderLevel,
                    true,
                    "Elemental Bow Ender mode container should stay in sync with the loaded mode resolver"
            );

            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("ElementalBowMode"));
            item.initializeSpellContainer(stack);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container in NONE mode");
        });
    }

    static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_spell_wheel_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, SchoolRegistry.FIRE_RESOURCE.toString());
            ((ElementalBow) stack.getItem()).initializeSpellContainer(stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var mainhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND);
            helper.assertTrue(mainhandSelections.isEmpty(),
                    "Elemental Bow should not add its derived spell to the mainhand spell wheel: " + mainhandSelections);
            helper.assertTrue(selectionManager.getSelection() == null,
                    "Elemental Bow should not create a selected spell from its derived container");
        });
    }

    static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, SchoolRegistry.FIRE_RESOURCE.toString());
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Elemental Bow should reject Arcane Anvil spell imbuing regardless of scroll spell"
            );
        });
    }

    static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = ElementalBow.createInsufficientManaMessage(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    null
            );
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.irons_spellbooks.cast_error_mana",
                    "Elemental Bow mana error should use Iron's cast_error_mana key"
            );
        });
    }
    static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_partial_release_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow partial release test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow should start drawing when mana and ammo are available: " + useResult.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 19);
            helper.assertTrue(stack.getDamageValue() == 0, "Elemental Bow should not lose durability before full draw");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should not consume arrows before full draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana before full draw: " + magicData.getMana());
        });
    }
    static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start vanilla draw with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow should enter use state for Infinity vanilla draw");
        });
    }
    static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_special_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with only special arrows available: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow vanilla mode should consume the special arrow that vanilla resolution selected");
        });
    }
    static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_only_mode_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow arrow-only mode should fail when only special arrows are available: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow arrow-only mode should not enter use state without normal arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Elemental Bow arrow-only mode should not consume special arrows");
            assertElementalBowSelection(helper, stack, "arrow", null,
                    "Elemental Bow arrow-only mode should keep its selection while empty");
        });
    }
    static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow arrow-only mode should enter use state for Infinity draw");
        });
    }
    static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow magic mode should fail to start without ammo even with Infinity: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow magic mode should not enter use state without ammo");
        });
    }
    static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var synthesis = enchantmentLookup.getOrThrow(Enchantments.SYNTHESIS);
            var infinity = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var mending = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());

            helper.assertTrue(stack.getItem().supportsEnchantment(stack, synthesis),
                    "Elemental Bow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(synthesis)),
                    "Elemental Bow should accept Synthesis from enchanted books");
            helper.assertTrue(synthesis.value().canEnchant(stack),
                    "Elemental Bow should be included in the Synthesis supported_items tag");
            helper.assertFalse(Enchantment.areCompatible(synthesis, infinity),
                    "Synthesis should be incompatible with Infinity");
            helper.assertFalse(Enchantment.areCompatible(synthesis, mending),
                    "Synthesis should be incompatible with Mending");

            assertTooltipKeyAt(helper, stack, 0, "item.apprenticecodex.elemental_bow.mode",
                    "Elemental Bow should always show the current mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.desc", ChatFormatting.GRAY,
                    "Elemental Bow should always show the description tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment",
                    "Elemental Bow should not show spell ammo tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show Infinity spell tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_synthesis",
                    "Elemental Bow should not show Synthesis spell tooltip while not in magic mode");

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            assertTooltipKeyAt(helper, stack, 1, "item.apprenticecodex.elemental_bow.desc",
                    "Elemental Bow should show the description below the mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment", ChatFormatting.YELLOW,
                    "Elemental Bow should show the no-enchantment spell tooltip in magic mode");

            var infinityStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(infinityStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            infinityStack.enchant(infinity, 1);
            assertTooltipKeyUsesColor(helper, infinityStack, "item.apprenticecodex.elemental_bow.spell.with_infinity", ChatFormatting.YELLOW,
                    "Elemental Bow should show the Infinity spell tooltip in magic mode");

            var synthesisStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(synthesisStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            synthesisStack.enchant(synthesis, 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should show the Synthesis spell tooltip in magic mode");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.with_synthesis",
                    "Elemental Bow should no longer show the legacy Synthesis tooltip key");

            synthesisStack.enchant(infinity, 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should prefer the Synthesis spell tooltip when Synthesis and Infinity are both present");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show the Infinity spell tooltip when Synthesis is also present");
        });
    }
    static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var synthesis = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_empty_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(synthesis, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows when Synthesis is enchanted: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 1,
                    "Elemental Bow Synthesis magic shot should still damage the bow after a successful cast");
            helper.assertTrue(magicData.getMana() < initialMana,
                    "Elemental Bow Synthesis magic shot should still consume spell mana");
        });
    }
    static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var synthesis = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_ammo_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(synthesis, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis ammo test could not resolve player mana data");
            magicData.setMana(250.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start with Synthesis while arrows are present: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow Synthesis magic shot should not consume arrows even when arrows are available");
        });
    }
    static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            item.initializeSpellContainer(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should not expose a spell container outside magic mode");
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null,
                    "Elemental Bow should not expose a displayed spell profile outside magic mode");
        });
    }
    static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_helper_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var cooldownAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION);
            helper.assertTrue(cooldownAttribute != null, "Elemental Bow cooldown helper test could not resolve cooldown attribute");
            cooldownAttribute.addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "elemental_bow_cooldown_helper_test"),
                    0.35D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            var helperCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            var vanillaCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(helperCooldown == vanillaCooldown,
                    "Elemental Bow cooldown helper should keep Iron's sword multiplier path: "
                            + helperCooldown + " / expected " + vanillaCooldown);

            var spellbookCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(spellbookCooldown > helperCooldown,
                    "Elemental Bow cooldown helper should still reflect the SWORD cooldown multiplier: "
                            + helperCooldown + " / spellbook " + spellbookCooldown);
        });
    }
    static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var expectedStoredCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 0,
                    "Elemental Bow should suppress elemental arrow cooldowns but got " + cooldownEvent.getEffectiveCooldown());
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    ) == expectedStoredCooldown,
                    "Elemental Bow should store the helper cooldown for overheat timing"
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(
                    160,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get(),
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == 160,
                    "Elemental Bow cooldown suppression should not affect unrelated spells");
        });
    }
    static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_mana_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow overheat mana test could not resolve player mana data");

            var item = (ElementalBow) stack.getItem();
            item.initializeSpellContainer(stack);
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow overheat mana test should resolve the active Fire profile");
            var fireArrow = fireProfile != null
                    ? fireProfile.spell()
                    : io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var baseMana = fireProfile != null ? fireProfile.spell().getManaCost(fireProfile.spellLevel()) : fireArrow.getManaCost(1);

            magicData.setMana(300.0F);
            var initialMana = magicData.getMana();

            magicData.setPlayerCastingItem(stack.copy());
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            fireArrow.getSpellCooldown()
                    )
            );

            var extraMana = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    baseMana
            );
            helper.assertTrue(extraMana > 0.0F, "Elemental Bow should charge extra mana once Fire overheat is active");

            var overheatedUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(overheatedUseResult.getResult().consumesAction(),
                    "Elemental Bow should still allow a second overheated draw: " + overheatedUseResult.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            var manaAfterOverheatedShot = magicData.getMana();
            helper.assertTrue(Math.abs(manaAfterOverheatedShot - (initialMana - baseMana - extraMana)) < 1.0e-3F,
                    "Elemental Bow overheated shot consumed the wrong mana: " + manaAfterOverheatedShot);
            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active() && state.chainDepth() >= 2,
                    "Elemental Bow overheated shot should keep Fire overheat active and deepen the chain: " + state);
        });
    }
    static void elementalBowOverheatTracksSchoolsSeparately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_school_test");
            var fireStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(fireStack, "magic", SchoolRegistry.FIRE_RESOURCE);

            var natureStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(natureStack, "magic", SchoolRegistry.NATURE_RESOURCE);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow school overheat test could not resolve player mana data");

            magicData.setPlayerCastingItem(fireStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            160,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    )
            );

            magicData.setPlayerCastingItem(natureStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            120,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.POISON_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.NATURE_RESOURCE,
                            0
                    )
            );

            var fireState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireState.active() && fireState.chainDepth() == 1,
                    "Elemental Bow fire overheat should stay isolated at depth 1: " + fireState);

            var natureState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureState.active() && natureState.chainDepth() == 1,
                    "Elemental Bow nature overheat should stay isolated at depth 1: " + natureState);

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                            player,
                            SchoolRegistry.ENDER_RESOURCE,
                            10.0F
                    ) == 0.0F,
                    "Elemental Bow should not leak overheat into untouched schools"
            );
        });
    }
    static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_refresh_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
        var magicData = MagicData.getPlayerMagicData(player);
        var firstExpire = new java.util.concurrent.atomic.AtomicLong();
        var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
        var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                fireArrow,
                player,
                CastSource.SWORD
        );

        helper.assertTrue(magicData != null, "Elemental Bow overheat refresh test could not resolve player mana data");

        helper.runAtTickTime(1, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );
            firstExpire.set(jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE).expireGameTime());
        });

        helper.runAtTickTime(40, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );

            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active(), "Elemental Bow repeated cast should keep fire overheat active");
            helper.assertTrue(state.chainDepth() == 2, "Elemental Bow repeated cast should raise overheat chain depth to 2: " + state.chainDepth());
            helper.assertTrue(state.expireGameTime() > firstExpire.get(),
                    "Elemental Bow repeated cast should refresh overheat expiry but got " + state.expireGameTime() + " <= " + firstExpire.get());
            helper.assertTrue(state.expireGameTime() - helper.getLevel().getGameTime() == expectedCooldown,
                    "Elemental Bow repeated cast should reset overheat duration from the latest cast");
        });

        helper.runAtTickTime(41, helper::succeed);
    }
    static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.5D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_draw_config_test");
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Elemental Bow draw config test could not resolve player mana data");
                magicData.setMana(300.0F);
                var initialMana = magicData.getMana();

                helper.assertTrue(ElementalBow.resolveMagicRequiredDrawTicks(stack) == 30,
                        "Elemental Bow required draw ticks should use profile ticks and server multiplier");
                var shortUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(shortUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should start drawing: " + shortUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 29);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 0,
                        "Elemental Bow should not fire before configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 2,
                        "Elemental Bow should not consume arrows before configured draw ticks");
                helper.assertTrue(magicData.getMana() == initialMana,
                        "Elemental Bow should not consume mana before configured draw ticks");

                var readyUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(readyUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should restart drawing: " + readyUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 30);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 1,
                        "Elemental Bow should fire at configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                        "Elemental Bow should consume one arrow after configured draw ticks");
                helper.assertTrue(magicData.getMana() < initialMana,
                        "Elemental Bow should consume spell mana after configured draw ticks");
            }
        });
    }


    static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.5D,
                    0.25D,
                    1.0D,
                    0,
                    0,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_mana_config_test");
                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100
                );

                var extraMana = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100.0F
                );
                helper.assertTrue(Math.abs(extraMana - 75.0F) < 1.0e-3F,
                        "Elemental Bow additional mana should use its server config but got " + extraMana);
            }
        });
    }


    static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.20D,
                    0.08D,
                    2.0D,
                    30,
                    50,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_duration_config_test");

                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        10
                );
                var minState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(minState.active()
                                && minState.expireGameTime() - helper.getLevel().getGameTime() == 30,
                        "Elemental Bow overheat duration should use configured minimum: " + minState);

                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.clear(player, SchoolRegistry.FIRE_RESOURCE);
                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100
                );
                var capState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(capState.active()
                                && capState.expireGameTime() - helper.getLevel().getGameTime() == 50,
                        "Elemental Bow overheat duration should use configured cap: " + capState);
            }
        });
    }


    static void elementalBowPowerSpellLevelBonusUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0,
                    0.5D
            )) {
                var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
                var stack = new ItemStack(item);
                var power = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);
                stack.enchant(power, 3);
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);

                item.initializeSpellContainer(stack);

                var fireMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(fireMode != null, "Elemental Bow power config test should resolve Fire mode");
                var powerBonus = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.resolvePowerArrowSpellLevelBonus(stack);
                helper.assertTrue(powerBonus == 1,
                        "Elemental Bow Power III should add floor(3 * 0.5) spell levels but got " + powerBonus);
                var expectedLevel = fireMode == null ? 1 : Mth.clamp(1 + powerBonus, fireMode.spell().getMinLevel(), fireMode.spell().getMaxLevel());
                var profile = ElementalBow.getDisplayedSpellProfile(stack);
                helper.assertTrue(profile != null, "Elemental Bow power config test should expose a displayed spell profile");
                helper.assertTrue(profile != null && profile.spellLevel() == expectedLevel,
                        "Elemental Bow Power spell level should use the configured bonus before spell level clamp but got "
                                + (profile == null ? "null" : profile.spellLevel()));
            }
        });
    }
}
