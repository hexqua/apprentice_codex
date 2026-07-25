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
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.shield.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffRightClickItemEvent;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
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
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.ammo.BowAmmoConsumptionNotification;
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
final class SpellcasterQuiverGameTestScenarios {
    private SpellcasterQuiverGameTestScenarios() {
    }
    static void spellcasterQuiverUsesBackSlotAndCapsStoredArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            helper.assertTrue(quiverStack.is(CURIOS_BACK),
                    "Spellcaster Quiver should be tagged for the Curios back slot");

            var firstInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 300));
            var secondInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 300));
            helper.assertTrue(firstInsert == 300, "Spellcaster Quiver should store the full first stack");
            helper.assertTrue(secondInsert == 212,
                    "Spellcaster Quiver should stop at 512 arrows but inserted " + secondInsert);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 512,
                    "Spellcaster Quiver should cap total storage at 512");

            var removalOrderQuiver = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.ARROW, 32));
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.SPECTRAL_ARROW, 7));
            var removed = SpellcasterQuiver.removeOneStack(removalOrderQuiver);
            helper.assertTrue(removed.is(Items.SPECTRAL_ARROW) && removed.getCount() == 7,
                    "Spellcaster Quiver should remove the smallest stored arrow stack first");
        });
    }
    static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_pickup_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var itemEntity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.ARROW, 12));
            SpellcasterQuiverPickupEvent.onEntityItemPickup(new ItemEntityPickupEvent.Pre(player, itemEntity));

            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 12,
                    "Equipped Spellcaster Quiver should auto-store picked up arrows");
            helper.assertTrue(itemEntity.isRemoved(),
                    "Spellcaster Quiver pickup handling should finish the ItemEntity when all arrows were stored");
        });
    }
    static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(bowStack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start drawing when only the equipped Spellcaster Quiver provides arrows");

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Elemental Bow should consume the equipped Spellcaster Quiver arrow first");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }

    static void elementalBowMagicModeUsesConfiguredSpellcasterQuiverCatalyst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementalBowMagicArrowCatalystItemsOverrideForGameTest(
                    List.of("minecraft:arrow")
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_quiver_catalyst_test");
                var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowShotSelection(bowStack, "magic", SchoolRegistry.FIRE_RESOURCE);
                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

                var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
                SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 3));
                SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 1));
                equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Elemental Bow quiver catalyst test could not resolve player mana data");
                magicData.setMana(250.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction(),
                        "Elemental Bow magic mode should accept the configured quiver catalyst: " + result.getResult());
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                                - jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow.READY_DRAW_TICKS
                );
                player.stopUsingItem();

                var normalView = findElementalBowSelectionView(player, bowStack, "arrow", null);
                helper.assertTrue(normalView != null && "2".equals(normalView.badgeText()),
                        "Elemental Bow magic mode should consume one configured normal arrow from Spellcaster Quiver");
                var spectralView = findElementalBowSelectionView(
                        player,
                        bowStack,
                        "special",
                        ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow")
                );
                helper.assertTrue(spectralView != null && "1".equals(spectralView.badgeText()),
                        "Elemental Bow magic mode should leave the unconfigured spectral arrow in Spellcaster Quiver");
            }
        });
    }

    static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_selection_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 3));
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var normalView = findElementalBowSelectionView(player, bowStack, "arrow", null);
            helper.assertTrue(normalView != null, "Elemental Bow should expose normal arrow selection");
            helper.assertTrue(normalView != null && "3".equals(normalView.badgeText()),
                    "Elemental Bow selection badge should count normal arrows stored in Spellcaster Quiver");

            var view = findElementalBowSelectionView(player, bowStack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));
            helper.assertTrue(view != null, "Elemental Bow should expose spectral arrow selection from Spellcaster Quiver contents");
            helper.assertTrue(view != null && "2".equals(view.badgeText()),
                    "Elemental Bow selection badge should count Spellcaster Quiver arrows");
        });
    }
    static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_quiver_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when Spellcaster Quiver provides arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Vanilla Bow should consume the Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Vanilla Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }
    static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_held_special_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when only the held special arrow should be selected: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Vanilla Bow should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Vanilla Bow should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }
    static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_normal_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when normal arrows exist outside the quiver: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Vanilla Bow should consume the lone normal arrow before more numerous Spellcaster Quiver special arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow should not consume Spellcaster Quiver special arrows while a normal arrow existed");
        });
    }
    static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_infinity_quiver_test");
            var bowStack = new ItemStack(Items.BOW);
            bowStack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow Infinity fallback should stop at normal arrow mode and leave Spellcaster Quiver special arrows untouched");
        });
    }
    static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_held_special_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing when a held special arrow exists: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Elemental Bow vanilla mode should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Elemental Bow vanilla mode should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }
    static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            bowStack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Elemental Bow vanilla mode Infinity fallback should leave Spellcaster Quiver special arrows untouched");
        });
    }
    static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while a bow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once bow use ends");
        });
    }
    static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_focus_staffbow_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.startUsingItem(InteractionHand.MAIN_HAND);

            helper.assertTrue(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should activate while the item is being held");
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while Focus Staffbow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should stop once use ends");
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once Focus Staffbow use ends");
        });
    }
    static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start when Spellcaster Quiver holds the catalyst arrow but got " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player));
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 1,
                    "Focus Staffbow should consume the equipped Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Focus Staffbow should leave loose inventory arrows untouched while the quiver still has arrows");
        });
    }

    static void bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_ammo_notification_count_test");
            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.HEALING.value());
            healingArrow.setCount(4);
            var poisonArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.POISON.value());
            poisonArrow.setCount(3);
            player.getInventory().setItem(1, healingArrow.copy());
            player.setItemInHand(InteractionHand.OFF_HAND, poisonArrow.copy());

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(
                    quiverStack,
                    PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.HEALING.value()).copyWithCount(2)
            );
            SpellcasterQuiver.store(
                    quiverStack,
                    PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.POISON.value()).copyWithCount(5)
            );
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var packet = BowAmmoConsumptionNotification.createPacket(
                    player,
                    ItemRegistry.ELEMENTAL_BOW.getId(),
                    healingArrow
            );
            helper.assertTrue(packet.sourceId().equals(ItemRegistry.ELEMENTAL_BOW.getId().toString()),
                    "Bow ammo notification should preserve the source weapon id");
            helper.assertTrue(ItemStack.isSameItemSameComponents(packet.iconStack(), healingArrow)
                            && packet.iconStack().getCount() == 1,
                    "Bow ammo notification should preserve the consumed tipped arrow as a single icon");
            helper.assertTrue(packet.remainingCount() == 6L,
                    "Bow ammo notification should total matching inventory and quiver arrows while excluding different potion NBT");
        });
    }

    static void focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_ammo_result_test");
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var consumed = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.ARROW_CATALYST
            );
            helper.assertTrue(consumed.successful() && consumed.consumedArrow()
                            && consumed.consumedStack().is(Items.ARROW)
                            && consumed.consumedStack().getCount() == 1,
                    "Focus Staffbow ammo result should preserve the arrow that was actually consumed");
            helper.assertTrue(BowAmmoConsumptionNotification.countRemaining(player, consumed.consumedStack()) == 4L,
                    "Focus Staffbow notification count should include the remaining quiver and inventory arrows");

            var bypassed = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.BYPASS
            );
            helper.assertTrue(bypassed.successful() && !bypassed.consumedArrow(),
                    "Focus Staffbow creative, Synthesis, and disabled-requirement bypasses should not report arrow consumption");

            var rejected = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.NONE
            );
            helper.assertTrue(!rejected.successful() && !rejected.consumedArrow(),
                    "Focus Staffbow missing-ammo results should fail without reporting arrow consumption");
        });
    }
}
