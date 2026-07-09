package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
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
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
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
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverPickupEvent;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastEvent;
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
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
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

final class SpellCalibrationEquipmentGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private SpellCalibrationEquipmentGameTestScenarios() {
    }

    static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.PHOTON_SIPHON.get();
            var stack = createInitializedPresetStack(item);
            var spellContainer = ISpellContainer.get(stack);

            helper.assertFalse(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Photon Siphon should not block external imbue as a UniqueItem");
            helper.assertTrue(spellContainer != null, "Photon Siphon default spell container is null");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon should still start with locked Mana Charge");
        });
    }

    static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "photon_siphon_calibration_repair_test");
            var item = (PhotonSiphon) ItemRegistry.PHOTON_SIPHON.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            var legacyReplacementStack = createInitializedPresetStack(item);
            applyLegacyLockedReplacement(helper, legacyReplacementStack, replacementSpell, 1);
            createSpellCalibrationBenchMenuWithTarget(player, legacyReplacementStack);
            var repairedReplacementContainer = ISpellContainer.get(legacyReplacementStack);
            helper.assertTrue(repairedReplacementContainer != null,
                    "Photon Siphon repaired replacement spell container is null");
            assertSpellData(helper, repairedReplacementContainer, 0, replacementSpell, 1, false,
                    "Photon Siphon Calibration Bench repair should unlock legacy non-default replacement spells");

            var defaultStack = createInitializedPresetStack(item);
            createSpellCalibrationBenchMenuWithTarget(player, defaultStack);
            var defaultContainer = ISpellContainer.get(defaultStack);
            helper.assertTrue(defaultContainer != null, "Photon Siphon default spell container is null after Calibration Bench check");
            assertSpellData(helper, defaultContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon Calibration Bench repair should not unlock the default Mana Charge");
        });
    }

    static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_target_slots_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);

            var emptyAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet);
            helper.assertTrue(emptyAmuletMenu.hasCalibrationTarget(),
                    "Empty Autocast Amulet should be accepted by Spell Calibration Bench");
            helper.assertTrue(emptyAmuletMenu.hasAutocastAmulet(),
                    "Autocast Amulet should be treated as a stored adjustment target");
            helper.assertTrue(emptyAmuletMenu.isAdjustmentSlotEnabled(0),
                    "Autocast Amulet should expose adjustment slots");
            helper.assertFalse(emptyAmuletMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Autocast Amulet should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(emptyAmuletMenu.getScrollItem(0).isEmpty(),
                    "Empty Autocast Amulet should not expose a scroll");

            var imbuedAmulet = new ItemStack(autocastAmulet);
            AutocastAmulet.setCalibrationScroll(imbuedAmulet, 0, createSpellScroll(SpellRegistry.SENSE_EVIL.get()));
            var imbuedAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedAmulet);
            helper.assertTrue(imbuedAmuletMenu.getScrollItem(0)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Imbued Autocast Amulet should expose a removable scroll");
            var longAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(longAmulet);
            var longAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, longAmulet);
            helper.assertTrue(longAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANTIS_LEAP.get())),
                    "Autocast Amulet should accept long scrolls even before Silver Ring adjustment");
            longAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(createSpellScroll(SpellRegistry.MANTIS_LEAP.get()));
            helper.assertTrue(longAmuletMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Autocast Amulet should warn that long spells cannot auto-cast before Silver Ring adjustment");
            longAmuletMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                    .set(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertFalse(longAmuletMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Autocast Amulet should clear the long spell warning after Silver Ring adjustment");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var emptyBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(emptyBlade);
            var bladeMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(emptyBlade),
                    "Mana Force Blade should be accepted by Spell Calibration Bench because it shows Can be Imbued");

            var imbuedBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(imbuedBlade);
            setSingleUnlockedSpell(helper, imbuedBlade,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(imbuedBlade),
                    "Imbued Mana Force Blade should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            var imbuedBladeMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedBlade);
            helper.assertFalse(imbuedBladeMenu.hasOperationalImbueTarget(),
                    "Mana Force Blade should still be unsupported by Calibration Bench operations");
            helper.assertTrue(imbuedBladeMenu.hasTargetSpellAt(0),
                    "Imbued Mana Force Blade spell should be visible for unsupported slot hints");
            helper.assertTrue(imbuedBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).getItem().isEmpty(),
                    "Unsupported Calibration Bench targets should not expose a real removable scroll");
            helper.assertTrue(imbuedBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1).isEmpty(),
                    "Unsupported Calibration Bench targets should not allow scroll extraction");

            var emptyEnchantressRobe = new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get());
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should remain unsupported by Calibration Bench operations");

            helper.assertFalse(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get())),
                    "Enchantress Hat should not be accepted by Spell Calibration Bench");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var healScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            var suitHoodMenu = createSpellCalibrationBenchMenuWithTarget(player, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            helper.assertTrue(suitHoodMenu.hasMagiAgentSuit(),
                    "Magi Agent Suit hood should be accepted by Spell Calibration Bench");
            helper.assertTrue(suitHoodMenu.isAdjustmentSlotEnabled(0),
                    "Magi Agent Suit should enable its first adjustment slot");
            helper.assertFalse(suitHoodMenu.isAdjustmentSlotEnabled(1),
                    "Magi Agent Suit should not enable more than one adjustment slot");
            helper.assertTrue(suitHoodMenu.getEnabledScrollSlotCount() == 0,
                    "Magi Agent Suit non-chest pieces should not expose scroll slots");
            helper.assertTrue(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START).mayPlace(fireRune),
                    "Magi Agent Suit should accept school runes in the first adjustment slot");
            helper.assertFalse(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START + 1).mayPlace(fireRune),
                    "Magi Agent Suit should reject school runes outside the first adjustment slot");
            helper.assertFalse(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                    "Magi Agent Suit non-chest pieces should reject scroll placement");

            var mithrilFreecastStaffMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get())
            );
            helper.assertTrue(mithrilFreecastStaffMenu.hasMithrilFreecastStaff(),
                    "Mithril Freecast Staff should be accepted by Spell Calibration Bench as an adjustment target");
            helper.assertTrue(mithrilFreecastStaffMenu.isAdjustmentSlotEnabled(0),
                    "Mithril Freecast Staff should expose adjustment slots");
            helper.assertTrue(mithrilFreecastStaffMenu.getEnabledScrollSlotCount() == 0,
                    "Mithril Freecast Staff should not expose scroll slots at the Spell Calibration Bench");
            helper.assertTrue(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .mayPlace(fireRune),
                    "Mithril Freecast Staff should accept school runes in adjustment slots");
            helper.assertTrue(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .mayPlace(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())),
                    "Mithril Freecast Staff should accept Silver Ring adjustments");
            helper.assertFalse(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                    "Mithril Freecast Staff should reject scroll placement");

            var gauntletWithFreecastAdjustmentMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get())
            );
            var playerInventoryMenuSlot = SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START
                    + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
            player.getInventory().setItem(9, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()));
            var quickMovedFreecastStaff = gauntletWithFreecastAdjustmentMenu.quickMoveStack(player, playerInventoryMenuSlot);
            helper.assertTrue(quickMovedFreecastStaff.is(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    "Shift-clicked Mithril Freecast Staff should move while Scrollcaster Gauntlet is the target");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(),
                    "Shift-clicked Mithril Freecast Staff should leave the player inventory");
            helper.assertTrue(gauntletWithFreecastAdjustmentMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .getItem()
                            .is(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    "Shift-clicked Mithril Freecast Staff should enter a Scrollcaster Gauntlet adjustment slot");

            var suitCoat = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get());
            var suitCoatMenu = createSpellCalibrationBenchMenuWithTarget(player, suitCoat);
            helper.assertTrue(suitCoatMenu.getEnabledScrollSlotCount() == 1,
                    "Magi Agent Suit coat should expose one scroll slot");
            suitCoatMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START).set(fireRune.copy());
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem
                            .getCalibrationAdjustment(suitCoat, 0)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    "Magi Agent Suit coat should store a school rune through the Spell Calibration Bench");
            suitCoatMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(healScroll.copy());
            assertStackHasSpell(helper, suitCoat, io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(), 1,
                    "Magi Agent Suit coat should accept scroll imbue at the Spell Calibration Bench");

            var presetStaffMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    createInitializedPresetStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get())
            );
            helper.assertTrue(presetStaffMenu.getScrollItem(0).isEmpty(),
                    "Copper Swingcast Staff preset spell should not expose a removable scroll");
            helper.assertTrue(presetStaffMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())),
                    "Copper Swingcast Staff preset slot should accept a replacement scroll");
            var uninitializedPresetStaff = new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
            ISpellContainer.remove(uninitializedPresetStaff);
            helper.assertFalse(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                    "Prepared Copper Swingcast Staff test stack should not have spell_container");
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(uninitializedPresetStaff),
                    "Uninitialized preset spell containers should be accepted by Spell Calibration Bench");
            createSpellCalibrationBenchMenuWithTarget(player, uninitializedPresetStaff);
            helper.assertTrue(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                    "Spell Calibration Bench should initialize accepted preset spell containers");

            var spellcastersFlaskMenu = createSpellcasterWorkbenchMenuWithSingleInput(
                    player,
                    new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get())
            );
            var spellcastersFlaskResult = spellcastersFlaskMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
            helper.assertTrue(spellcastersFlaskResult.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask should keep the Workbench particle toggle result");
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(spellcastersFlaskResult),
                    "Spellcaster's Flask Workbench result should toggle particles off from the default state");

            var alchemistsFlask = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var defaultAlchemistsFlask = new ItemStack(alchemistsFlask);
            alchemistsFlask.initializeSpellContainer(defaultAlchemistsFlask);
            var alchemistsFlaskMenu = createSpellCalibrationBenchMenuWithTarget(player, defaultAlchemistsFlask);
            helper.assertTrue(alchemistsFlaskMenu.getScrollItem(0).isEmpty(),
                    "Alchemist's Flask preset Extract should not expose a removable scroll");

            var satelliteFollowcastMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
            );
            helper.assertTrue(satelliteFollowcastMenu.hasSatelliteFollowcastAmulet(),
                    "Satellite Followcast Amulet should be treated as a stored adjustment target");
            helper.assertTrue(satelliteFollowcastMenu.isAdjustmentSlotEnabled(0),
                    "Satellite Followcast Amulet should expose adjustment slots");
            helper.assertFalse(satelliteFollowcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Satellite Followcast Amulet should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(satelliteFollowcastMenu.getEnabledScrollSlotCount() == SatelliteFollowcastAmulet.MIN_SPELL_SLOTS,
                    "Satellite Followcast Amulet should start with one enabled scroll slot");
            helper.assertTrue(satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())),
                    "Satellite Followcast Amulet should accept profiled continuous scrolls even before Silver Ring adjustment");
            satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));
            helper.assertTrue(satelliteFollowcastMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Satellite Followcast Amulet should warn that continuous spells cannot followcast before Silver Ring adjustment");
            satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                    .set(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertFalse(satelliteFollowcastMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Satellite Followcast Amulet should clear the continuous spell warning after Silver Ring adjustment");

            var fourSlotSatellite = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            for (var slot = 0; slot < SatelliteFollowcastAmulet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
                SatelliteFollowcastAmulet.setCalibrationAdjustment(
                        fourSlotSatellite,
                        slot,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
                );
            }
            var fourSlotSatelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, fourSlotSatellite);
            helper.assertTrue(fourSlotSatelliteMenu.getEnabledScrollSlotCount() == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                    "Satellite Followcast Amulet should expose four scroll slots after three slot upgrades");

            var smashcastMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get())
            );
            helper.assertFalse(smashcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Smashcast Scepter should expose Calibration Bench spell restriction tooltip lines");
            helper.assertFalse(smashcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANA_CHARGE.get())),
                    "Smashcast Scepter should reject CONTINUOUS scrolls in the Spell Calibration Bench");
        });
    }

    static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_imbue_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var senseEvil = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();

            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);
            var emptyAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet);
            emptyAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(mageLight));
            assertAutocastSpellData(helper, emptyAmulet, 0, mageLight, 1,
                    "Calibration-imbued Autocast Amulet should contain mage_light");

            var twoSlotAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(twoSlotAmulet);
            AutocastAmulet.setCalibrationAdjustment(
                    twoSlotAmulet,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            AutocastAmulet.setCalibrationScroll(twoSlotAmulet, 0, createSpellScroll(senseEvil));
            var twoSlotAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 1).set(createSpellScroll(mageLight));
            helper.assertTrue(AutocastAmulet.getImbuedSpells(twoSlotAmulet).size() == 2,
                    "Calibration imbue should add a second Autocast Amulet spell");
            assertAutocastSpellData(helper, twoSlotAmulet, 0, senseEvil, 1,
                    "Calibration imbue should keep the existing Autocast Amulet spell");
            assertAutocastSpellData(helper, twoSlotAmulet, 1, mageLight, 1,
                    "Calibration imbue should add mage_light to the empty Autocast Amulet slot");

            var removedScroll = twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(removedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should return a scroll when removing an Autocast Amulet spell");
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).onTake(player, removedScroll);
            helper.assertTrue(AutocastAmulet.getSpellDataAt(twoSlotAmulet, 0) == SpellData.EMPTY
                            && AutocastAmulet.getSpellDataAt(twoSlotAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should not compact spell slots while removing a scroll");
            createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            helper.assertTrue(AutocastAmulet.getSpellDataAt(twoSlotAmulet, 0) == SpellData.EMPTY
                            && AutocastAmulet.getSpellDataAt(twoSlotAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should preserve empty spell slots when opening an existing target");

            var satelliteAmulet = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            var satelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, satelliteAmulet);
            satelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(mageLight));
            assertSatelliteSpellData(helper, satelliteAmulet, 0, mageLight, 1,
                    "Calibration-imbued Satellite Followcast Amulet should contain mage_light");

            SatelliteFollowcastAmulet.setCalibrationAdjustment(
                    satelliteAmulet,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var twoSlotSatelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, satelliteAmulet);
            twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 1)
                    .set(createSpellScroll(magicMissile));
            helper.assertTrue(SatelliteFollowcastAmulet.getImbuedSpells(satelliteAmulet).size() == 2,
                    "Calibration imbue should add a second Satellite Followcast Amulet spell");
            var removedSatelliteScroll = twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(removedSatelliteScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should return a scroll when removing a Satellite Followcast Amulet spell");
            twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).onTake(player, removedSatelliteScroll);
            helper.assertTrue(SatelliteFollowcastAmulet.getSpellDataAt(satelliteAmulet, 0) == SpellData.EMPTY
                            && SatelliteFollowcastAmulet.getSpellDataAt(satelliteAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should not compact Satellite Followcast Amulet slots while removing a scroll");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var unsupportedMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            var manaForceBladeStack = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(manaForceBladeStack);
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(manaForceBladeStack),
                    "Calibration Bench should accept Can be Imbued targets for unsupported-operation hints");

            var externalSpellContainerStack = new ItemStack(Items.DIAMOND_SWORD);
            ISpellContainer.set(externalSpellContainerStack, ISpellContainer.create(1, false, false));
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(externalSpellContainerStack),
                    "Calibration Bench should accept items that show Iron's Can be Imbued tooltip");
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(externalSpellContainerStack),
                    "Generic external ISpellContainer items should remain unsupported by Calibration Bench operations");

            var magicMissileScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(manaForceBladeStack, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject non-extractable Can be Imbued targets");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(externalSpellContainerStack, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject generic external ISpellContainer items");

            var invokeCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_INVOKE_CARD.get();
            var imbuedInvokeCard = invokeCard.createArcaneAnvilImbueResult(
                    new ItemStack(invokeCard),
                    new SpellData(heal, 1)
            );
            var invokeCardMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedInvokeCard);
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(imbuedInvokeCard),
                    "Spell Invoke Card should not be supported for Calibration Bench extraction");
            helper.assertTrue(SpellCalibrationImbueHelper.createScrollForSlot(imbuedInvokeCard, 0).isEmpty(),
                    "Spell Invoke Card should not create a scroll through Calibration Bench extraction");
            helper.assertTrue(invokeCardMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1).isEmpty(),
                    "Spell Invoke Card should not allow scroll extraction from the Calibration Bench menu");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(imbuedInvokeCard, 0, magicMissileScroll),
                    "Spell Invoke Card should not accept Calibration Bench scroll replacement");
            assertStackHasSpell(helper, imbuedInvokeCard, heal, 1,
                    "Blocked Calibration Bench extraction should keep the Spell Invoke Card spell");

            var autonomyCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_AUTONOMY_CARD.get();
            var imbuedAutonomyCard = autonomyCard.createArcaneAnvilImbueResult(
                    new ItemStack(autonomyCard),
                    new SpellData(heal, 1)
            );
            helper.assertTrue(SpellCalibrationImbueHelper.createScrollForSlot(imbuedAutonomyCard, 0).isEmpty(),
                    "Spell Autonomy Card should not create a scroll through Calibration Bench extraction");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(imbuedAutonomyCard, 0, magicMissileScroll),
                    "Spell Autonomy Card should not accept Calibration Bench scroll replacement");

            var illuminateStellarStaff = createInitializedPresetStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get());
            helper.assertFalse(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(illuminateStellarStaff),
                    "Calibration Bench should reject UniqueItem imbue targets");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(illuminateStellarStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject UniqueItem imbue targets");
            helper.assertFalse(SpellCalibrationImbueHelper.setScrollAt(illuminateStellarStaff, 0, magicMissileScroll.copy()),
                    "Calibration Bench should not directly set spells on UniqueItem targets");

            var crystalBladedStaff = createInitializedPresetStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            helper.assertFalse(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(crystalBladedStaff),
                    "Calibration Bench should keep Crystal Bladed Staff unsupported");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(crystalBladedStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should not expose Crystal Bladed Staff replacement");

            var mithrilFreecastStaff = createInitializedPresetStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(mithrilFreecastStaff),
                    "Calibration Bench should accept Mithril Freecast Staff as an adjustment target");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(mithrilFreecastStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject direct spell insertion into Mithril Freecast Staff");
            helper.assertFalse(SpellCalibrationImbueHelper.setScrollAt(mithrilFreecastStaff, 0, magicMissileScroll.copy()),
                    "Calibration Bench should not directly set spells on Mithril Freecast Staff");

            var disallowedSpellMenu = createSpellCalibrationBenchMenuWithTarget(player, new ItemStack(autocastAmulet));
            helper.assertFalse(disallowedSpellMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANA_CHARGE.get())),
                    "Calibration Bench should not accept a spell rejected by the target item");

            var spellAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var spellAmplifierMenu = createSpellCalibrationBenchMenuWithTarget(player, spellAmplifier);
            spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, spellAmplifier, heal, 1,
                    "Calibration Bench should imbue generic extractable Spell Amplifiers");
            helper.assertTrue(spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should extract generic Spell Amplifier spells");

            var circlet = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var circletMenu = createSpellCalibrationBenchMenuWithTarget(player, circlet);
            circletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, circlet, heal, 1,
                    "Calibration Bench should imbue tag-allowed extractable Curios");
        });
    }

    static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var item = (MithrilFreecastStaff) stack.getItem();
            item.initializeSpellContainer(stack);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertFalse(stack.getItem() instanceof RestrictedSpellImbuableItem,
                    "Mithril Freecast Staff should not expose the restricted imbue API");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Mithril Freecast Staff should reject Arcane Anvil spell imbuing"
            );
        });
    }

    static void mithrilFreecastStaffCooldownUsesSelectedSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mithril_freecast_selected_cooldown_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Mithril Freecast Staff selected cooldown test could not resolve player magic data");
            magicData.setMana(1000.0F);

            var staff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var staffItem = (MithrilFreecastStaff) staff.getItem();
            staffItem.initializeSpellContainer(staff);
            MithrilFreecastStaff.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));

            var normalSwordCooldown = MagicManager.getEffectiveSpellCooldown(magicMissile, player, CastSource.SWORD);
            var selectedSourceCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(selectedSourceCooldown == normalSwordCooldown,
                    "Scrollcaster Gauntlet selected SWORD source should use the normal SWORD cooldown: "
                            + selectedSourceCooldown + " / sword " + normalSwordCooldown);
            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == magicMissile,
                    "Mithril Freecast Staff cooldown test should resolve the selected offhand spell");
            var selectedStack = SpellSelectionStackResolver.resolveSelectionStack(player, selection.slot);
            helper.assertTrue(ItemStack.isSameItemSameComponents(selectedStack, gauntlet),
                    "Mithril Freecast Staff cooldown test should resolve the selected offhand source stack");
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    magicMissile,
                    selection.getCastSource()
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                        "Mithril Freecast Staff should use the selected source cooldown but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
            }

            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            magicData.setPlayerCastingItem(staff.copy());
            var spellbookBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SPELLBOOK
            );
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    heal,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                        heal,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                var expectedCooldown = spellbookBaseCooldown + heal.getEffectiveCastTime(1, player);
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should use selected SPELLBOOK cooldown plus long cast time but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            var grimoire = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoire, 1);
            new ArchivistsGrimoire.ScrollInventory(grimoire, helper.getLevel().registryAccess())
                    .setStackInSlot(0, createSpellScroll(SpellRegistry.BOUND_BOW.get()));
            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoire);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    0
            ));
            magicData.getPlayerCooldowns().removeCooldown(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Mithril Freecast Staff should immediately trigger silver-ring Bound Bow from the spellbook slot");
            var boundBowRecast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(boundBowRecast != null,
                    "Mithril Freecast Staff silver-ring Bound Bow should create a recast before cooldown");
            magicData.getPlayerRecasts().removeRecast(boundBowRecast, RecastResult.USED_ALL_RECASTS);
            var expectedBoundBowCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                    player,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            var actualBoundBowCooldown = magicData.getPlayerCooldowns()
                    .getSpellCooldowns()
                    .get(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(
                    actualBoundBowCooldown != null
                            && actualBoundBowCooldown.getCooldownRemaining() == expectedBoundBowCooldown,
                    "Mithril Freecast Staff should keep the selected source cooldown for Bound Bow recast cooldown but got "
                            + (actualBoundBowCooldown == null ? "none" : actualBoundBowCooldown.getCooldownRemaining())
                            + " / expected " + expectedBoundBowCooldown
            );
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    player.getUUID(),
                    staff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            magicData.setPlayerCastingItem(grimoire.copy());
            var retainedRecastCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), player, CastSource.SWORD),
                    SpellRegistry.BOUND_BOW.get(),
                    player,
                    CastSource.SWORD
            );
            NeoForge.EVENT_BUS.post(retainedRecastCooldownEvent);
            helper.assertTrue(retainedRecastCooldownEvent.getEffectiveCooldown() == expectedBoundBowCooldown,
                    "Mithril Freecast Staff should consume retained recast cooldown source without relying on current casting item but got "
                            + retainedRecastCooldownEvent.getEffectiveCooldown()
                            + " / expected " + expectedBoundBowCooldown);
            helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            ).isEmpty(), "Mithril Freecast Staff should clear resolved retained recast cooldown source");
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    player.getUUID(),
                    staff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            MithrilFreecastStaffCastContext.clearPendingCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            );
            helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            ).isEmpty(), "Mithril Freecast Staff should clear retained pending cooldown source without relying on current casting item");
            var timeoutPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                    "mithril_freecast_recast_timeout_cleanup_test");
            var timeoutMagicData = MagicData.getPlayerMagicData(timeoutPlayer);
            helper.assertTrue(timeoutMagicData != null,
                    "Mithril Freecast Staff timeout cleanup test could not resolve player magic data");
            var timeoutStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    timeoutPlayer.getUUID(),
                    timeoutStaff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            var timeoutBoundBowRecast = new RecastInstance(
                    SpellRegistry.BOUND_BOW.get().getSpellId(),
                    1,
                    2,
                    20,
                    CastSource.SWORD,
                    null
            );
            timeoutMagicData.getPlayerRecasts().forceAddRecast(timeoutBoundBowRecast);
            timeoutMagicData.getPlayerRecasts().removeRecast(timeoutBoundBowRecast, RecastResult.TIMEOUT);
            var swordBoundBowCooldown = MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), timeoutPlayer, CastSource.SWORD);
            var timeoutExpectedBoundBowCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                    timeoutPlayer,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(timeoutExpectedBoundBowCooldown != swordBoundBowCooldown,
                    "Mithril Freecast Staff timeout cleanup test needs SPELLBOOK and SWORD cooldowns to differ");
            timeoutMagicData.setPlayerCastingItem(new ItemStack(Items.STICK));
            var staleTimeoutCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    swordBoundBowCooldown,
                    SpellRegistry.BOUND_BOW.get(),
                    timeoutPlayer,
                    CastSource.SWORD
            );
            NeoForge.EVENT_BUS.post(staleTimeoutCooldownEvent);
            helper.assertTrue(staleTimeoutCooldownEvent.getEffectiveCooldown() == swordBoundBowCooldown,
                    "Mithril Freecast Staff should clear retained source after recast timeout but got "
                            + staleTimeoutCooldownEvent.getEffectiveCooldown() + " / expected " + swordBoundBowCooldown);

            var spellbookMagicMissileCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    magicMissile,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(spellbookMagicMissileCooldown != normalSwordCooldown,
                    "Mithril Freecast Staff stale pending test needs SPELLBOOK and SWORD cooldowns to differ");
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    magicMissile,
                    CastSource.SPELLBOOK
            )) {
                var immediateCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                magicData.setPlayerCastingItem(staff.copy());
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(immediateCooldownEvent);
                helper.assertTrue(immediateCooldownEvent.getEffectiveCooldown() == spellbookMagicMissileCooldown,
                        "Mithril Freecast Staff should apply selected SPELLBOOK cooldown immediately but got "
                                + immediateCooldownEvent.getEffectiveCooldown() + " / expected " + spellbookMagicMissileCooldown);
                MithrilFreecastStaffCastContext.retainUntilCooldown(
                        player.getUUID(),
                        staff,
                        magicMissile,
                        CastSource.SPELLBOOK
                );
            }
            var stalePendingCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    normalSwordCooldown,
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            magicData.setPlayerCastingItem(staff.copy());
            MithrilFreecastStaffCastEvent.onSpellCooldownAdded(stalePendingCooldownEvent);
            helper.assertTrue(stalePendingCooldownEvent.getEffectiveCooldown() == normalSwordCooldown,
                    "Mithril Freecast Staff should not retain stale selected source after instant cooldown but got "
                            + stalePendingCooldownEvent.getEffectiveCooldown() + " / expected " + normalSwordCooldown);

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var harvestMoon = SpellRegistry.HARVEST_MOON.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    harvestMoon,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(harvestMoon, player, CastSource.SWORD),
                        harvestMoon,
                        player,
                        CastSource.SWORD
                );
                NeoForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        harvestMoon,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should keep CraftsmansDelight on the selected SPELLBOOK cooldown but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    thermalProcess,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(thermalProcess, player, CastSource.SWORD),
                        thermalProcess,
                        player,
                        CastSource.SWORD
                );
                NeoForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        thermalProcess,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should keep Thermal Process on the selected SPELLBOOK cooldown with Magi boots and CraftsmansDelight but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            var artisanSmash = SpellRegistry.ARTISAN_SMASH.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    artisanSmash,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(artisanSmash, player, CastSource.SWORD),
                        artisanSmash,
                        player,
                        CastSource.SWORD
                );
                NeoForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        artisanSmash,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should not re-reduce Magi boots cooldown after adding long cast time but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
            helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Mithril Freecast Staff should initiate the selected instant offhand spell");
            var delayedCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    normalSwordCooldown,
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            MithrilFreecastStaffCastEvent.onSpellCooldownAdded(delayedCooldownEvent);
            helper.assertTrue(delayedCooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                    "Mithril Freecast Staff should keep the selected source cooldown until delayed cooldown but got "
                            + delayedCooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
        });
    }

    private static void assertAutocastSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = AutocastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
    }

    private static void assertSatelliteSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = SatelliteFollowcastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
    }
}
