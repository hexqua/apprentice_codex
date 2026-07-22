package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
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
import jp.aquafactory.apprenticecodex.item.continuouscast.ContinuousCastDurationSimulation;
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
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmash;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellEntity;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleEntity;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
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
final class FocusStaffbowGameTestScenarios {
    private FocusStaffbowGameTestScenarios() {
    }
    static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_reject_test");
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when used from offhand but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when offhand use is rejected");
        });
    }
    static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_selection_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow offhand selection test could not resolve player mana data");
            if (magicData != null) {
                magicData.setMana(100.0F);
            }

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var selection = selectionManager.getSelection();
            helper.assertTrue(selection != null
                            && io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(selection.slot),
                    "Focus Staffbow offhand selection test should resolve offhand spell selection but got " + selection);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() != net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow mainhand use should remain available even when selected spell slot is offhand but got "
                            + result.getResult());
        });
    }
    static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_summon_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should enter pending cast for LONG summon spells but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should keep a pending cast state while charging");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                    "Focus Staffbow should expose the summon weapon during pending charge");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should not consume its catalyst arrow before the LONG cast completes");
            helper.assertTrue(player.isUsingItem(), "Focus Staffbow should still be in use while the summon weapon is pending");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow pending state should clear after the charged cast completes");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow charged cast should clear simulated additional cast data after completion");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow should consume exactly one catalyst arrow after the LONG cast completes");
        });
    }

    static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_artisan_radius_test");
        var spell = (ArtisanSmash) jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARTISAN_SMASH.get();
        var spellLevel = 1;
        var magicData = MagicData.getPlayerMagicData(player);
        var baseSpellPower = spell.getSpellPower(spellLevel, player);
        var baseSplashRadius = Math.min(2.0F + baseSpellPower / 600.0F, 8.0F);

        helper.succeedIf(() -> {
            var launcher = spell.onCastNoWeapon(helper.getLevel(), spellLevel, player, magicData);
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPowerAttribute != null,
                    "Focus Staffbow Artisan Smash test could not resolve spell power attribute");
            var modifier = new AttributeModifier(
                    FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                    2.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            var expectedSplashRadius = -1.0F;
            try {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                    spellPowerAttribute.addTransientModifier(modifier);
                }
                expectedSplashRadius = Math.min(2.0F + spell.getSpellPower(spellLevel, player) / 600.0F, 8.0F);
                spell.onCastCompleteWithWeapon(helper.getLevel(), spellLevel, player, magicData, false, launcher);
            } finally {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                }
            }

            var shells = helper.getLevel().getEntitiesOfClass(
                    ArtisanSmashShellEntity.class,
                    new AABB(player.position(), player.position()).inflate(32.0D)
            );
            helper.assertTrue(shells.size() == 1,
                    "Focus Staffbow Artisan Smash test should spawn exactly one shell but got " + shells.size());
            var actualSplashRadius = shells.get(0).getSplashRadius();
            helper.assertTrue(actualSplashRadius > baseSplashRadius + 0.01F,
                    "Artisan Smash splash radius should not stay at the pre-charge value: " + actualSplashRadius);
            helper.assertTrue(Math.abs(actualSplashRadius - expectedSplashRadius) < 0.01F,
                    "Artisan Smash splash radius should use charged spell power. expected="
                            + expectedSplashRadius + ", actual=" + actualSplashRadius);
        });
    }

    static void focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();

            var mantisPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "focus_staffbow_mantis_leap_power_test");
            var mantisSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANTIS_LEAP.get();
            var mantisMagicData = MagicData.getPlayerMagicData(mantisPlayer);
            mantisSpell.onServerPreCast(level, 1, mantisPlayer, mantisMagicData);
            var mantisBlades = getOwnedSummonWeapons(helper, mantisPlayer, MantisLeapBladeEntity.class);
            helper.assertTrue(mantisBlades.size() == 1,
                    "Focus Staffbow Mantis Leap test should create one pre-cast blade");
            var mantisBlade = mantisBlades.get(0);
            var baseMantisDamage = mantisBlade.getDamageForGameTest();
            castWithDoubledSpellPower(mantisPlayer,
                    () -> mantisSpell.castSpell(level, 1, mantisPlayer, CastSource.SWORD, true));
            helper.assertTrue(mantisBlade.getDamageForGameTest() > baseMantisDamage * 1.5F,
                    "Mantis Leap should reevaluate damage with charged spell power at cast time");

            var slashPlayer = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 0),
                    "focus_staffbow_slash_blade_power_test");
            var slashSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get();
            var slashMagicData = MagicData.getPlayerMagicData(slashPlayer);
            slashSpell.onServerPreCast(level, 1, slashPlayer, slashMagicData);
            var katanas = getOwnedSummonWeapons(helper, slashPlayer, SlashBladeKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow Slash Blade test should create one pre-cast katana");
            var katana = katanas.get(0);
            var baseSlashDamage = katana.getDamageForGameTest();
            castWithDoubledSpellPower(slashPlayer,
                    () -> slashSpell.castSpell(level, 1, slashPlayer, CastSource.SWORD, true));
            helper.assertTrue(katana.getDamageForGameTest() > baseSlashDamage * 1.5F,
                    "Slash Blade should reevaluate damage with charged spell power at cast time");

            var precisionPlayer = createEquipmentTestPlayer(helper, new BlockPos(6, 2, 0),
                    "focus_staffbow_precision_jack_power_test");
            var precisionSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.PRECISION_JACK.get();
            var precisionMagicData = MagicData.getPlayerMagicData(precisionPlayer);
            precisionSpell.onServerPreCast(level, 1, precisionPlayer, precisionMagicData);
            var knives = getOwnedSummonWeapons(helper, precisionPlayer, PrecisionJackKnifeEntity.class);
            helper.assertTrue(knives.size() == 1,
                    "Focus Staffbow Precision Jack test should create one pre-cast knife");
            var knife = knives.get(0);
            var baseLootingBonus = knife.getLootingBonus();
            var baseDuplicateChance = knife.getDuplicateDropChancePercent();
            castWithDoubledSpellPower(precisionPlayer,
                    () -> precisionSpell.castSpell(level, 1, precisionPlayer, CastSource.SWORD, true));
            helper.assertTrue(knife.getLootingBonus() > baseLootingBonus,
                    "Precision Jack should reevaluate looting with charged spell power at cast time");
            helper.assertTrue(knife.getDuplicateDropChancePercent() > baseDuplicateChance,
                    "Precision Jack should reevaluate duplicate chance with charged spell power at cast time");
        });
    }

    private static void castWithDoubledSpellPower(net.minecraft.world.entity.LivingEntity caster, Runnable cast) {
        var spellPowerAttribute = caster.getAttribute(AttributeRegistry.SPELL_POWER);
        if (spellPowerAttribute == null) {
            throw new IllegalStateException("Spell power attribute is unavailable");
        }
        var modifier = new AttributeModifier(
                FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        spellPowerAttribute.addTransientModifier(modifier);
        try {
            cast.run();
        } finally {
            spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        }
    }

    static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_cancel_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow cancel test should start a pending cast but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                helper.assertTrue(
                        getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                        "Focus Staffbow cancel test should spawn the summon weapon during pending charge"
                )
        );
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - (jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player) - 1)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow cancel test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should clear the pending state when released before the required charge");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).isEmpty(),
                    "Focus Staffbow should remove the simulated summon weapon when the charge is cancelled");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow should clear simulated additional cast data when the charge is cancelled");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should keep its catalyst arrow when the LONG cast is cancelled early");
        });
    }
    static void focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "focus_staffbow_lethal_assault_wait_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LETHAL_ASSAULT.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow Lethal Assault should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(12, () -> {
            var rifles = getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1,
                    "Focus Staffbow should keep one Lethal Assault rifle visible while charging");
            helper.assertFalse(rifles.get(0).hasStartedFiringForGameTest(),
                    "Focus Staffbow Lethal Assault rifle should stay idle before release");
        });
        helper.runAtTickTime(13, () -> bowStack.getItem().releaseUsing(
                bowStack,
                helper.getLevel(),
                player,
                bowStack.getUseDuration(player) - 12
        ));
        helper.runAtTickTime(14, () -> {
            var rifles = getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1 && rifles.get(0).hasStartedFiringForGameTest(),
                    "Focus Staffbow Lethal Assault rifle should start firing after release completes the cast");
            helper.succeed();
        });
    }
    static void focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "focus_staffbow_higanbana_wait_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.HIGANBANA.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(1000.0F);
        var baseDamage = (1.0F + spell.getSpellPower(1, player) / 100.0F)
                * ApprenticeCodexServerConfig.damageMultiplier(
                jp.aquafactory.apprenticecodex.config.DamageMultiplierKey.HIGANBANA);
        var summonedPosition = new Vec3[1];
        var summonedYaw = new float[1];

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow Higanbana should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow Higanbana should create one pre-cast katana");
            summonedPosition[0] = katanas.get(0).position();
            summonedYaw[0] = katanas.get(0).getYRot();
            player.setPos(player.getX() + 4.0D, player.getY(), player.getZ() + 4.0D);
            player.setYRot(player.getYRot() + 90.0F);
        });
        helper.runAtTickTime(30, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow should keep one Higanbana katana visible while charging");
            var katana = katanas.get(0);
            helper.assertTrue(katana.getRemainingSlashCount() == 0,
                    "Focus Staffbow Higanbana should stay idle before release");
            helper.assertTrue(katana.position().distanceTo(summonedPosition[0]) < 1.0E-6D
                            && Math.abs(katana.getYRot() - summonedYaw[0]) < 1.0E-4F,
                    "Focus Staffbow Higanbana should stay at its summoned position while charging");
        });
        helper.runAtTickTime(45, () -> bowStack.getItem().releaseUsing(
                bowStack,
                helper.getLevel(),
                player,
                bowStack.getUseDuration(player) - 44
        ));
        helper.runAtTickTime(46, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1 && katanas.get(0).getRemainingSlashCount() == 4,
                    "Focus Staffbow Higanbana should start its four-slash sequence after release");
            helper.assertTrue(katanas.get(0).getDamageForGameTest() > baseDamage,
                    "Focus Staffbow Higanbana should use charged spell power for damage");
            helper.succeed();
        });
    }

    static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_hold_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(10000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous test should store a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous test should keep Iron's casting state active after start");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should consume one catalyst arrow as soon as casting starts");
            helper.assertTrue(player.isUsingItem(),
                    "Focus Staffbow continuous test should keep the player in use state while held");
        });
        helper.runAtTickTime(3, () -> {
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous multiplier test could not resolve spell power attribute");
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            helper.assertTrue(modifier != null && modifier.amount() > 0.0D,
                    "Focus Staffbow continuous multiplier should start rising immediately after cast start");
        });
        helper.runAtTickTime(101, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous duration test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should stay active past the spell's normal duration cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep Iron's casting state active past the normal duration cap");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var elapsedTicks = continuousState.getElapsedTicks(player.level().getGameTime());
            var expectedRemaining = ContinuousCastDurationSimulation.computeRemaining(
                    continuousState.requiredCastTicks, elapsedTicks
            );
            helper.assertTrue(magicData.getCastDuration() == continuousState.requiredCastTicks,
                    "Focus Staffbow continuous cast should expose the spell's base cast duration");
            helper.assertTrue(magicData.getCastDurationRemaining() == expectedRemaining,
                    "Focus Staffbow continuous cast remaining duration should decrease monotonically: "
                            + magicData.getCastDurationRemaining() + " expected " + expectedRemaining);
            helper.assertTrue(magicData.getCastDurationRemaining() < 10,
                    "Focus Staffbow continuous cast should have passed Iron's normal remaining-duration stop window: " + magicData.getCastDurationRemaining());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous midpoint test could not resolve spell power attribute");
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(actualAmount - (expectedMultiplier - 1.0D)) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should match the fixed early-stage curve: " + actualAmount);
            helper.assertTrue(Math.abs(expectedMultiplier - 1.5D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should reach 1.5x after 100 ticks: " + expectedMultiplier);
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should not keep consuming arrows while the button stays held");
        });
        helper.runAtTickTime(251, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous cap test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should remain active after reaching the 2x cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep running after reaching the 2x cap while mana remains");
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous cap test could not resolve spell power attribute");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(expectedMultiplier - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should cap at 2.0x after 250 ticks: " + expectedMultiplier);
            helper.assertTrue(Math.abs(actualAmount - 1.0D) < 1.0e-9D,
                    "Focus Staffbow continuous spell power bonus should stop at +100%: " + actualAmount);
        });
        helper.runAtTickTime(252, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 251
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast state should clear after releasing the button");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous release should clear Iron's casting state");
        });
    }


    static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_arrow_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when no catalyst arrow is available but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state without a catalyst arrow");
        });
    }
    static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_standard_time_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(300.0F);

        var castTimeReductionAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION);
        helper.assertTrue(castTimeReductionAttribute != null,
                "Focus Staffbow continuous standard time test could not resolve cast time reduction attribute");
        if (castTimeReductionAttribute != null) {
            castTimeReductionAttribute.addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "focus_staffbow_continuous_standard_time_test"),
                    0.75D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous standard time test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous standard time test should store a CONTINUOUS cast state");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            helper.assertTrue(continuousState.requiredCastTicks == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should ignore cast-time attributes and use the spell's base castTime");
            helper.assertTrue(magicData.getCastDuration() == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should sync Iron's cast duration with the base castTime");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 2
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous standard time test should clear after release");
        });
    }
    static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(15.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous mana test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous mana test should start with a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous mana test should still be casting immediately after start");
        });
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana stop test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast should stop once it cannot pay the next tick's mana cost");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous mana stop should clear Iron's casting state");
            helper.assertTrue(magicData.getMana() >= 0.0F,
                    "Focus Staffbow continuous mana stop should not drive mana below zero: " + magicData.getMana());
            helper.assertTrue(magicData.getMana() <= 15.0F,
                    "Focus Staffbow continuous mana stop consumed an unexpected amount of mana: " + magicData.getMana());
        });
    }
    static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_instant_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow instant base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 40.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow instant test should start charging immediately but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow instant immediate release should only consume base mana: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow instant cast should consume one catalyst arrow on release");
        });
    }
    static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_short_long_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow short LONG base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 60.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow short LONG test should enter pending charge but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow short LONG test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).chargeBaselineTicks
                            == jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.MINIMUM_OVERCHARGE_BASELINE_TICKS,
                    "Focus Staffbow short LONG test should clamp the overcharge baseline to one second");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - spell.getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow short LONG release should stay at base mana within the first second: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow short LONG cast should still consume only one catalyst arrow after completion");
        });
    }
    static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var settings = new jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings(
                    4.0D,
                    3.0D,
                    20,
                    1.0D,
                    0.5D
            );
            var pendingMaxTicks = 20L + 20L * 2L + 20L * 3L;
            var pendingMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computePendingChargeMultiplier(pendingMaxTicks, 20, settings);
            helper.assertTrue(Math.abs(pendingMultiplier - 4.0D) < 1.0e-9D,
                    "Focus Staffbow pending config should reach custom max within the fixed existing time window: "
                            + pendingMultiplier);

            var continuousMidpoint = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(100L, settings);
            helper.assertTrue(Math.abs(continuousMidpoint - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach the midpoint at 100 ticks: " + continuousMidpoint);
            var continuousMax = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(250L, settings);
            helper.assertTrue(Math.abs(continuousMax - 3.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach custom max at 250 ticks: " + continuousMax);

            var manaCost = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeScaledManaCost(10, 4.0D, settings);
            helper.assertTrue(manaCost == 20,
                    "Focus Staffbow mana config should apply multiplier and exponent before flooring: " + manaCost);
        });
    }

    static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_base_mana_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow mana gate test could not resolve player mana data");
            magicData.setMana(spell.getManaCost(1) - 1.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should still fail immediately when base mana is insufficient but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when even base mana is missing");
        });
    }

    static void focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                true,
                false,
                1.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_arrow_config_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start without arrows when arrow catalysts are disabled but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow arrow-disabled config should not create or consume arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    false,
                    true,
                    true,
                    1.0D,
                    List.of(),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_config_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get(), 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(1000.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject continuous spells when disabled but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject disabled continuous casts before consuming arrows");
            }
        });
    }

    static void focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                false,
                true,
                1.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_config_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(spell.getManaCost(1));

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan-disabled test should still start when base mana is available but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                var spellData = Capabilities.getSpellDataOrNull(player);
                helper.assertTrue(spellData != null, "Focus Staffbow loan-disabled test lost spell data capability");
                helper.assertTrue(spellData != null
                                && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).hasOutstandingLoan(),
                        "Focus Staffbow should not create loan state when loan is disabled");
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject disabled loan before consuming arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowLoanRatioConfigRejectsExcessBorrowing(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                true,
                true,
                0.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_ratio_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(spell.getManaCost(1));

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan-ratio test should still start when base mana is available but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                var spellData = Capabilities.getSpellDataOrNull(player);
                helper.assertTrue(spellData != null, "Focus Staffbow loan-ratio test lost spell data capability");
                helper.assertTrue(spellData != null
                                && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).hasOutstandingLoan(),
                        "Focus Staffbow should not create loan state above the configured ratio");
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject loan-ratio overflow before consuming arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowSpellDenylistBlocksBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    1.0D,
                    List.of(spell.getSpellId()),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_denylist_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject denylisted spells but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject denylisted spells before consuming arrows");
            }
        });
    }

    static void focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mageLight = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get();
            var linearBuild = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LINEAR_BUILD.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "focus_staffbow_precast_power_reject_test");
            helper.assertTrue(mageLight instanceof IChargecastStaffbowIncompatibleSpell
                            && FocusStaffbow.rejectsSpell(mageLight),
                    "Focus Staffbow should reject Mage Light through the pre-cast spell-power marker");
            helper.assertTrue(linearBuild instanceof IChargecastStaffbowIncompatibleSpell
                            && FocusStaffbow.rejectsSpell(linearBuild),
                    "Focus Staffbow should reject Linear Build through the pre-cast spell-power marker");
            assertTranslatableKey(
                    helper,
                    FocusStaffbow.createRejectedSpellMessage(mageLight.getDisplayName(player)),
                    "ui.apprenticecodex.focus_staffbow.reject_spell",
                    "Focus Staffbow should use its permanent rejection message"
            );

            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, mageLight, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject Mage Light but got " + result.getResult());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should reject Mage Light before consuming arrows");
        });
    }

    static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    1.0D,
                    List.of(),
                    true,
                    List.of("irons_spellbooks:magic_arrow")
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_allowlist_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject spells missing from the allowlist but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject allowlist misses before consuming arrows");
            }
        });
    }
    static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_repay_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow loan test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability before release");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).requiredCastTicks == 0,
                    "Focus Staffbow loan test should treat INSTANT spells as zero required cast ticks");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 60
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability after cast");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            var expectedLoanMana = baseManaCost * 3.0F;
            helper.assertTrue(Math.abs(loanState.remainingLoanMana - expectedLoanMana) < 1.0F,
                    "Focus Staffbow loan test should create three base-cost worth of debt at x2 but got "
                            + loanState.remainingLoanMana + " expected " + expectedLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan test should leave current mana at zero after borrowed cast: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow borrowed cast should still consume exactly one catalyst arrow");
            magicData.setMana(10.0F);
            jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager.tickLoanRepayment(player);
            helper.assertTrue(Math.abs(loanState.remainingLoanMana - (expectedLoanMana - 10.0F)) < 1.0F,
                    "Focus Staffbow loan repay test should consume recovered mana into the debt first but got "
                            + loanState.remainingLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan repay test should keep displayed mana at zero while debt remains: " + magicData.getMana());
            helper.succeed();
        });
    }
    static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_creative_overcharge_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow creative overcharge test could not resolve player mana data");
        magicData.setMana(17.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow creative overcharge test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow creative overcharge test lost spell data capability");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            helper.assertFalse(loanState.hasOutstandingLoan(),
                    "Focus Staffbow creative overcharge test should not create loan mana");
            helper.assertTrue(Math.abs(magicData.getMana() - 17.0F) < 1.0e-4F,
                    "Focus Staffbow creative overcharge test should leave mana unchanged but got " + magicData.getMana());
            helper.succeed();
        });
    }
    static void focusStaffbowCreativeContinuousReleaseSkipsCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_creative_continuous_cooldown_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow creative continuous cooldown test requires MagicData");
        magicData.setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow creative continuous cooldown test should start casting");
        });
        helper.runAtTickTime(3, () -> {
            var originalCreativeCooldown = io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get();
            try {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(false);
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 2
                );
            } finally {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            }
        });
        helper.runAtTickTime(4, () -> {
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Focus Staffbow creative CONTINUOUS release should respect disabled creative cooldowns");
            helper.succeed();
        });
    }

    static void focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 2, 0),
                    "focus_staffbow_creative_interruption_cooldown_test"
            );
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(
                    helper,
                    amplifierStack,
                    jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(),
                    1
            );
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));

            var interruptedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow creative interruption test requires MagicData");
            magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
            magicData.initiateCast(interruptedSpell, 1, 60, CastSource.SPELLBOOK, "gametest");

            var originalCreativeCooldown = io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get();
            net.minecraft.world.InteractionResultHolder<ItemStack> result;
            try {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(false);
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
                result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            } finally {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            }
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should accept input after interrupting a different creative cast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(interruptedSpell),
                    "Focus Staffbow creative interruption should respect disabled creative cooldowns");
        });
    }

    static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan block test could not resolve spell data capability");
            if (spellData != null) {
                spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, state -> state.addLoan(7.0F));
            }

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject new casts while borrowed mana remains but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not remain in use state while a loan blocks casting");
        });
    }
    static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_cooldown_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(200.0F);
            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null, "Focus Staffbow cooldown test could not resolve the selected spell");
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject use while the selected spell is on cooldown but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state while spell cooldown blocks casting");
        });
    }
    static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow.createLoanBlockedMessage(5.1F);
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.loan_mana",
                    "Focus Staffbow loan block message should use the dedicated translation key"
            );
        });
    }
    static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow.createInsufficientArrowMessage();
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.insufficient_arrow",
                    "Focus Staffbow insufficient arrow message should use the dedicated translation key"
            );
        });
    }

    static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_special_arrow_reject_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject special arrows that are not in arrowCatalystItems but got " + result.getResult());
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Focus Staffbow should not consume an unconfigured special arrow");
        });
    }

    static void focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    List.of("minecraft:spectral_arrow"),
                    3.0D,
                    2.0D,
                    20,
                    2.0D,
                    1.0D,
                    1.0D,
                    List.of(),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_special_arrow_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var configuredSpecialArrowId = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
                helper.assertTrue(ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds().contains(configuredSpecialArrowId),
                        "Focus Staffbow arrowCatalystItems override should contain " + configuredSpecialArrowId);
                helper.assertTrue(
                        BowCastAmmoResolver.resolveFocusStaffbowAmmoRoute(
                                player,
                                bowStack,
                                true,
                                ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds()
                        ) == BowCastAmmoResolver.FocusStaffbowAmmoRoute.ARROW_CATALYST,
                        "Focus Staffbow should resolve configured special arrow as arrow catalyst"
                );
                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction(),
                        "Focus Staffbow should start when a configured special arrow is available but got " + result.getResult());
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                );
                helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                        "Focus Staffbow should consume the configured special arrow");
            }
        });
    }
    static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_synthesis_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        bowStack.enchant(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS),
                1
        );
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start without arrows when Synthesis is enchanted but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() ->
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow Synthesis path should not require or consume a catalyst arrow")
        );
    }
    static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var synthesis = enchantmentLookup.getOrThrow(Enchantments.SYNTHESIS);
            var infinity = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var transcendence = enchantmentLookup.getOrThrow(Enchantments.TRANSCENDENCE);
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var item = (FocusStaffbow) stack.getItem();

            helper.assertTrue(stack.getItem().supportsEnchantment(stack, synthesis),
                    "Focus Staffbow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(synthesis)),
                    "Focus Staffbow should accept Synthesis from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, synthesis),
                    "Focus Staffbow should allow Synthesis through anvil merges");

            helper.assertFalse(stack.getItem().supportsEnchantment(stack, infinity),
                    "Focus Staffbow should reject Infinity at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(infinity)),
                    "Focus Staffbow should reject Infinity from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, infinity),
                    "Focus Staffbow should reject Infinity through anvil merges");

            helper.assertFalse(stack.getItem().supportsEnchantment(stack, transcendence),
                    "Focus Staffbow should reject Transcendence at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(transcendence)),
                    "Focus Staffbow should reject Transcendence from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, transcendence),
                    "Focus Staffbow should reject Transcendence through anvil merges");

            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_HAUNTED)).orElse(null);
            helper.assertTrue(haunted != null, "Missing malum:haunted enchantment");
            if (haunted != null) {
                helper.assertTrue(stack.getItem().supportsEnchantment(stack, haunted),
                        "Focus Staffbow should allow malum:haunted at the enchanting table");
                helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(haunted)),
                        "Focus Staffbow should allow malum:haunted from enchanted books");
                helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, haunted),
                        "Focus Staffbow should allow malum:haunted through anvil merges");
            }

            var animated = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_ANIMATED)).orElse(null);
            helper.assertTrue(animated != null, "Missing malum:animated enchantment");
            if (animated != null) {
                helper.assertTrue(stack.getItem().supportsEnchantment(stack, animated),
                        "Focus Staffbow should allow malum:animated at the enchanting table");
                helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(animated)),
                        "Focus Staffbow should allow malum:animated from enchanted books");
                helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, animated),
                        "Focus Staffbow should allow malum:animated through anvil merges");
            }
        });
    }
    static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var modifiers = toModifierMultimap(stack.getAttributeModifiers());
            var componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_VALUE
            ) - 3.0D) < 1.0e-9D, "Focus Staffbow attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Focus Staffbow attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get((Holder<Attribute>) io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ) - 0.10D) < 1.0e-9D, "Focus Staffbow spell power regression: " + describeModifiers(modifiers));
            assertModifierAmount(helper, componentModifiers, Attributes.ATTACK_SPEED.value(), EquipmentSlotGroup.MAINHAND, -3.0D,
                    AttributeModifier.Operation.ADD_VALUE, "Focus Staffbow attack speed component regression");
        });
    }
}
