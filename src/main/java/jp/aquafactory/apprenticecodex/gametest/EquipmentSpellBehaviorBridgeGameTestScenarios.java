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
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitCooldownEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
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
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
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
import net.minecraft.world.level.block.AmethystClusterBlock;
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

final class EquipmentSpellBehaviorBridgeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentSpellBehaviorBridgeGameTestScenarios() {
    }
    static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCastingMoveSpeedAdjustment(helper, 0.0D, 0.8D, "No external bonus should keep full cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.25D, 0.55D, "Diamond-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.50D, 0.30D, "Netherite-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.75D, 0.05D, "Small remaining headroom should stay positive");
            assertCastingMoveSpeedAdjustment(helper, 0.80D, 0.0D, "Exact cap should stop adding more casting move speed");
            assertCastingMoveSpeedAdjustment(helper, 1.10D, 0.0D, "External overshoot should not become a negative correction");
        });
    }
    static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "long_stride_base_bonus_test");
            var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeed != null, "LongStride base bonus test could not resolve movement speed attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY, 200, 0));
            helper.assertTrue(movementSpeed != null, "LongStride base bonus test lost movement speed attribute after addEffect");

            var actualAmount = movementSpeed.getModifiers().stream()
                    .filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .mapToDouble(AttributeModifier::amount)
                    .sum();
            helper.assertTrue(Math.abs(actualAmount - 0.15D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.15 but got " + actualAmount);
        });
    }
    static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dynamic_casting_movespeed_rebalance_test");
            var effectHolder = EffectRegistry.LONG_STRIDE_MOBILITY;
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.value();
            var castingMoveSpeed = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED);
            helper.assertTrue(castingMoveSpeed != null,
                    "Dynamic casting mobility test could not resolve the CASTING_MOVESPEED attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effectHolder, 200, 0));
            helper.assertTrue(castingMoveSpeed != null, "Dynamic casting mobility test lost CASTING_MOVESPEED after addEffect");
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should initially fill the full cancellation headroom"
            );

            castingMoveSpeed.addTransientModifier(new AttributeModifier(
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    0.5D,
                    AttributeModifier.Operation.ADD_VALUE
            ));
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    0.3D,
                    "Dynamic casting mobility effect should shrink after an external casting move speed bonus is added"
            );

            castingMoveSpeed.removeModifier(CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID);
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should recover once the external casting move speed bonus is removed"
            );
        });
    }
    static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = new ItemStack(ItemRegistry.COMFORT_BERRIES.get()).getFoodProperties(null);
            helper.assertTrue(foodProperties != null, "Comfort Berries should remain edible");
            helper.assertTrue(foodProperties != null && foodProperties.nutrition() == 1,
                    "Comfort Berries nutrition regression: " + (foodProperties == null ? "null" : foodProperties.nutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.saturation() - 2.4f) < 1.0e-6F,
                    "Comfort Berries saturation regression: "
                            + (foodProperties == null ? "null" : foodProperties.saturation()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Berries should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<net.minecraft.world.food.FoodProperties.PossibleEffect>of()
                    : foodProperties.effects().stream()
                    .filter(effectPair -> effectPair.effect().getEffect() == EffectRegistry.MANA_REGENERATION)
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Berries should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.effect().getDuration() == 20 * 10,
                    "Comfort Berries mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.effect().getAmplifier() == 2,
                    "Comfort Berries mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.probability() - 1.0f) < 1.0e-6F,
                    "Comfort Berries mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.probability()));
        });
    }
    static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = new ItemStack(ItemRegistry.COMFORT_SANDWICH.get()).getFoodProperties(null);
            helper.assertTrue(foodProperties != null, "Comfort Sandwich should be edible");
            helper.assertTrue(foodProperties != null && foodProperties.nutrition() == 7,
                    "Comfort Sandwich nutrition regression: " + (foodProperties == null ? "null" : foodProperties.nutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.saturation() - 22.4f) < 1.0e-6F,
                    "Comfort Sandwich saturation regression: "
                            + (foodProperties == null ? "null" : foodProperties.saturation()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Sandwich should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<net.minecraft.world.food.FoodProperties.PossibleEffect>of()
                    : foodProperties.effects().stream()
                    .filter(effectPair -> effectPair.effect().getEffect() == EffectRegistry.MANA_REGENERATION)
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Sandwich should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.effect().getDuration() == 20 * 60,
                    "Comfort Sandwich mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.effect().getAmplifier() == 0,
                    "Comfort Sandwich mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.probability() - 1.0f) < 1.0e-6F,
                    "Comfort Sandwich mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.probability()));
        });
    }
    static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "mana_regeneration_test"));
            var manaRegenAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN);
            helper.assertTrue(manaRegenAttribute != null, "Player is missing the mana regen attribute");

            var baseValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(!Double.isNaN(baseValue) && baseValue > 0.0D,
                    "Mana regen base value must be positive for regression testing: " + baseValue);

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.MANA_REGENERATION, 20 * 30, 0));
            var levelOneValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(Math.abs(levelOneValue - (baseValue * 1.25D)) < 1.0e-9D,
                    "Mana Regeneration Lv1 regression: expected " + (baseValue * 1.25D) + " but got " + levelOneValue);

            player.removeEffect(EffectRegistry.MANA_REGENERATION);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.MANA_REGENERATION, 20 * 30, 1));
            var levelTwoValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(Math.abs(levelTwoValue - (baseValue * 1.50D)) < 1.0e-9D,
                    "Mana Regeneration Lv2 regression: expected " + (baseValue * 1.50D) + " but got " + levelTwoValue);
        });
    }
    static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertPotionEffect(helper, PotionRegistry.MEDITATION.get(), "apprenticecodex:meditation", 20 * 60 * 3, 0);
            assertPotionEffect(helper, PotionRegistry.LONG_MEDITATION.get(), "apprenticecodex:long_meditation", 20 * 60 * 8, 0);
            assertPotionEffect(helper, PotionRegistry.STRONG_MEDITATION.get(), "apprenticecodex:strong_meditation", 20 * 90, 1);
        });
    }
    static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_external_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
            var spectralHammerSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get();

            var touchDigManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.TOUCH_DIG_SPELL_ID,
                    1,
                    15,
                    touchDigSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(touchDigManaEvent);
            helper.assertTrue(touchDigManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Touch Dig mana to 8 but got " + touchDigManaEvent.getManaCost());
            var expectedTouchDigBaseCooldown = Math.max(1, touchDigSpell.getSpellCooldown() / 3);
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(touchDigSpell.getSpellCooldown(), player) == expectedTouchDigBaseCooldown,
                    "CraftsmansDelight should reduce Touch Dig base cooldown to one third before player modifiers");

            var touchDigCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    10,
                    touchDigSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(touchDigCooldownEvent);
            helper.assertTrue(touchDigCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(touchDigSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Touch Dig cooldown through the reduced cooldown helper");

            var spectralHammerManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.SPECTRAL_HAMMER_SPELL_ID,
                    1,
                    15,
                    spectralHammerSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(spectralHammerManaEvent);
            helper.assertTrue(spectralHammerManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Spectral Hammer mana to 8 but got " + spectralHammerManaEvent.getManaCost());
            var expectedSpectralHammerBaseCooldown = Math.max(1, spectralHammerSpell.getSpellCooldown() / 3);
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(spectralHammerSpell.getSpellCooldown(), player) == expectedSpectralHammerBaseCooldown,
                    "CraftsmansDelight should reduce Spectral Hammer base cooldown to one third before player modifiers");

            var spectralHammerCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    40,
                    spectralHammerSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(spectralHammerCooldownEvent);
            helper.assertTrue(spectralHammerCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(spectralHammerSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Spectral Hammer cooldown through the reduced cooldown helper");
        });
    }

    static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_apprentice_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.HARVEST_MOON.get(), 60, "Harvest Moon");
            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.EARTH_FORGE.get(), 20, "Earth Forge");
        });
    }

    static void craftsmansDelightScrollcasterGauntletCooldownKeepsItemPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "craftsmans_scrollcaster_cooldown_policy_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var spell = SpellRegistry.HARVEST_MOON.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown test could not resolve player magic data");
            magicData.setPlayerCastingItem(gauntlet.copy());

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    gauntlet
            );
            helper.assertTrue(expectedCooldown < io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                            spell,
                            player,
                            CastSource.SWORD
                    ),
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown should be reduced from the normal sword cooldown");

            var craftsmansFirstEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            helper.assertTrue(craftsmansFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "CraftsmansDelight -> Scrollcaster Gauntlet cooldown order should keep the reduced gauntlet cooldown but got "
                            + craftsmansFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            var scrollcasterFirstEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            helper.assertTrue(scrollcasterFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "Scrollcaster Gauntlet -> CraftsmansDelight cooldown order should keep the reduced gauntlet cooldown but got "
                            + scrollcasterFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_cooldown_test");
            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == baseCooldown,
                    "Magi Agent Suit Boots should not reduce cooldown while unequipped");

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var bootsEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(bootsEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
            );
            helper.assertTrue(expectedCooldown < baseCooldown,
                    "Magi Agent Suit Boots expected cooldown should be shorter than base cooldown");
            helper.assertTrue(bootsEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should reduce target spell cooldown but got "
                            + bootsEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void magiAgentSuitBootsCooldownKeepsCraftsmansDelightBestValue(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_craftsmans_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var spell = SpellRegistry.THERMAL_PROCESS.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(cooldownEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
            );
            var bootsOnlyCooldown = Math.max(1, spell.getSpellCooldown() / 2);
            helper.assertTrue(expectedCooldown < bootsOnlyCooldown,
                    "CraftsmansDelight should remain the stronger Thermal Process cooldown reduction");
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots and CraftsmansDelight should keep the strongest cooldown but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_additive_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));

            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var bootsCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
            );
            var extraCooldown = 37;
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    baseCooldown + extraCooldown,
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            MagiAgentSuitCooldownEvent.onSpellCooldownAdded(cooldownEvent);

            var expectedCooldown = bootsCooldown + extraCooldown;
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should keep additive cooldown components but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90) == 90,
                    "No limited cooldown candidate should keep the base cooldown");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 45, 30) == 30,
                    "Limited cooldown candidates should choose only the strongest reduction");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 0, -1, 60) == 60,
                    "Invalid limited cooldown candidates should be ignored");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(0, 1) == 0,
                    "Zero base cooldown should stay zero");
        });
    }

    static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = new TouchDigSpell();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_range_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(0, 23, 0));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(-90.0f);
            player.setYHeadRot(0.0f);
            player.setYBodyRot(0.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var uniqueInfo = spell.getUniqueInfo(1, player).stream()
                    .map(Component::getString)
                    .collect(Collectors.joining(", "));
            helper.assertTrue(uniqueInfo.contains("16"),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped but got: " + uniqueInfo);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped"
                            + " [equipped=" + CraftsmansDelight.isEquippedBy(player)
                            + ", range=" + CraftsmansDelight.getTouchDigRange(player)
                            + ", info=" + uniqueInfo + "]");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }
    static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fortune = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_ring_enchant_merge_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(fortune, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(fortune, 3));

            var mergedFortuneTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedFortuneTool,
                            net.minecraft.world.item.enchantment.Enchantments.FORTUNE
                    ) == 3,
                    "Touch Dig should prefer the higher Fortune level from the ring");

            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(silkTouch, 1));
            var equippedRing = getEquippedCraftsmansDelight(player);
            var mergedSilkTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedSilkTool,
                            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                    ) == 1,
                    "Touch Dig should inherit Silk Touch from the ring"
                            + " [equippedRingSilk=" + jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                    equippedRing,
                                    net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                            )
                            + ", mergedSilk=" + jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                    mergedSilkTool,
                                    net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                            )
                            + "]");
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedSilkTool,
                            net.minecraft.world.item.enchantment.Enchantments.FORTUNE
                    ) == 0,
                    "Touch Dig should drop Fortune when Silk Touch is present");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }
    static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_bare_hand_ring_enchant_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(silkTouch, 1);
            equipRingCurio(player, ringStack);

            var synthesizedTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertFalse(synthesizedTool.isEmpty(),
                    "Touch Dig should synthesize a mining tool when the caster is bare-handed but the ring has mining enchantments");
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            synthesizedTool,
                            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                    ) == 1,
                    "Touch Dig should copy Silk Touch onto the synthesized bare-hand tool");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }
    static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_ring_enchant_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(silkTouch, 1));

            var targetPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 12, 1)));
            hammer.setPos(hammerPos.x, hammerPos.y, hammerPos.z);
            helper.getLevel().addFreshEntity(hammer);

            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(targetPos).inflate(2.0D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should not drop cobblestone");
        });
    }

    static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var enchantmentLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_crystal_harvest_test");
            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(silkTouch, 1);
            equipRingCurio(player, ringStack);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist with CraftsmansDelight should harvest mature crystals growing from budding amethyst");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist with CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(hasItemEntityWithin(level, Blocks.AMETHYST_CLUSTER.asItem(), Vec3.atCenterOf(clusterPos), 1.5D),
                        "Heavenly Fist with ring Silk Touch should drop the crystal block itself");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistWithoutCraftsmansDelightLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_no_crystal_harvest_test");

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist without CraftsmansDelight should leave the crystal intact");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_crystal_source_guard_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var supportPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = supportPos.east();
            level.setBlock(supportPos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(supportPos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist with CraftsmansDelight should skip crystals not attached to harvest sources");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_immature_bud_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var budPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(budPos, Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(budPos).is(Blocks.LARGE_AMETHYST_BUD),
                        "Heavenly Fist with CraftsmansDelight should skip immature amethyst buds");
                helper.succeed();
            });
        });
    }

    private static BlockState matureAmethystCluster(Direction facing) {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    private static void spawnHeavenlyFist(ServerLevel level, LivingEntity owner, Vec3 center, float radius) {
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, radius, 0);
        level.addFreshEntity(fist);
    }

    static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var existingItemPos = Vec3.atCenterOf(logPos);
            var existingItem = new ItemEntity(
                    level,
                    existingItemPos.x,
                    existingItemPos.y,
                    existingItemPos.z,
                    new ItemStack(Items.COBBLESTONE)
            );
            level.addFreshEntity(existingItem);

            var job = new TinyLumberjackJob(originPos, 1, player);
            job.tick(level);

            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should move new log drops to the initial chopped block while CraftsmansDelight is equipped");
            helper.assertTrue(!existingItem.isRemoved() && existingItem.position().distanceToSqr(existingItemPos) < 0.01D,
                    "Tiny Lumberjack drop moving should not move ItemEntities that existed before the block break");
            helper.succeed();
        });
    }

    static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_unequip_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var job = new TinyLumberjackJob(originPos, 1, player);
            equipRingCurio(player, ItemStack.EMPTY);
            job.tick(level);

            helper.assertFalse(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should stop moving job drops after CraftsmansDelight is unequipped");
            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(logPos), 1.25D),
                    "Tiny Lumberjack should leave log drops near the broken block when CraftsmansDelight is not currently equipped");
            helper.succeed();
        });
    }

    static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_penetrated_armor_test");
            var armor = player.getAttribute(Attributes.ARMOR);
            var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            helper.assertTrue(armor != null, "Player is missing armor attribute");
            helper.assertTrue(toughness != null, "Player is missing armor toughness attribute");

            armor.setBaseValue(10.0D);
            toughness.setBaseValue(8.0D);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR, 100, 0));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 8.0D) < 1.0E-6D,
                    "Penetrated Armor I should reduce armor by 20%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor should reduce armor toughness by 100%");

            player.removeEffect(EffectRegistry.PENETRATED_ARMOR);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR, 100, 3));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 2.0D) < 1.0E-6D,
                    "Penetrated Armor IV should reduce armor by 80%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor toughness reduction should not depend on amplifier");

            var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                    helper.getLevel(),
                    player,
                    DamageTypes.WORLD_FLATTER
            );
            helper.assertTrue(source.is(DamageTypes.WORLD_FLATTER),
                    "World Flatter damage source should use apprenticecodex:world_flatter");
            helper.assertTrue(!source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "World Flatter should no longer use apprenticecodex:bypasses_iframe");
            helper.assertTrue(!source.is(DamageTypeTags.BYPASSES_COOLDOWN),
                    "World Flatter should no longer bypass vanilla cooldown i-frame");
        });
    }

    static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_block_filter_test");
            var pos = helper.absolutePos(new BlockPos(0, 2, 0));

            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should target pickaxe-mineable stone");
            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
                    "World Flatter should target shovel-mineable dirt");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.GLASS.defaultBlockState(), Blocks.GLASS.defaultBlockState()),
                    "World Flatter should reject glass because it has no specific pickaxe/shovel tool tag");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()),
                    "World Flatter should reject axe-mineable logs");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()),
                    "World Flatter should reject unbreakable blocks");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should not splash unrelated ore blocks from a non-ore center");
        });
    }

    static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_single_target_owner");
            owner.setYRot(0.0F);
            owner.setXRot(0.0F);

            var target = helper.spawn(EntityType.SHEEP, new BlockPos(0, 2, 4));
            var bystander = helper.spawn(EntityType.SHEEP, new BlockPos(1, 2, 4));
            target.setNoAi(true);
            bystander.setNoAi(true);
            var targetHealth = target.getHealth();
            var bystanderHealth = bystander.getHealth();

            var weapon = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, owner);
            weapon.setDamage(4.0F);
            weapon.setPenetratedArmorAmplifier(1);
            weapon.setToolSpeed(4.0F);
            weapon.updateOwnerTarget(level, new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    target.getBoundingBox().getCenter(),
                    target,
                    null
            ));

            for (var i = 0; i < 14; ++i) {
                target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
                weapon.tickOnServer(level);
            }
            helper.assertTrue(Math.abs(target.getHealth() - targetHealth) < 1.0E-6F,
                    "World Flatter should not damage an entity before the 15 tick attach completes");

            target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
            weapon.tickOnServer(level);
            helper.assertTrue(target.getHealth() < targetHealth,
                    "World Flatter should damage the attached moving target after 15 ticks");
            helper.assertTrue(target.hasEffect(EffectRegistry.PENETRATED_ARMOR),
                    "World Flatter should apply Penetrated Armor after successful damage");
            helper.assertTrue(Math.abs(bystander.getHealth() - bystanderHealth) < 1.0E-6F,
                    "World Flatter should not damage nearby non-target entities");
        });
    }
}
