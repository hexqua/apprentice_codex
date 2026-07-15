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
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
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
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
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

final class EquipmentSpellGunGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentSpellGunGameTestScenarios() {
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var gold = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var diamond = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var instantRecastSpell = SpellRegistry.HIGANBANA.get();
            var longRecastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            var continuousSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();

            helper.assertFalse(iron.canImbueSpell(instantRecastSpell, 1),
                    "Iron Spellcaster Gun should continue rejecting recast spells");
            helper.assertTrue(gold.canImbueSpell(instantRecastSpell, 1),
                    "Gold Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(instantRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(longRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow long recast spell imbuing");
            helper.assertFalse(diamond.canImbueSpell(continuousSpell, 1),
                    "Diamond Spellcaster Gun should continue rejecting continuous spells");
        });
    }
    static void spellcasterGunAbilityTooltipUsesInstantLongCastOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    false,
                    "Iron Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    true,
                    "Copper Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                    false,
                    "Gold Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(),
                    true,
                    "Diamond Spellcaster Gun"
            );
        });
    }
    static void reflectcastShieldCastRestrictionsFollowCalibration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            var item = (jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield) stack.getItem();
            var defaultTooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), defaultTooltipLines, TooltipFlag.Default.NORMAL);
            helper.assertTrue(containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_default"),
                    "Reflectcast Shield should show the imbued-spell cast tooltip by default");
            helper.assertFalse(containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield." + "hint")
                            || containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_" + "hint"),
                    "Reflectcast Shield should not show removed legacy tooltip keys");
            var defaultAbilityLines = collectReflectcastAbilityTooltipLines(helper, item, stack);
            helper.assertTrue(containsTranslatableKey(defaultAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_none"),
                    "Reflectcast Shield should show the empty ability line without Silver Ring");
            var defaultRestrictionLines = item.getImbueRestrictionTooltipLines(stack);
            helper.assertTrue(defaultRestrictionLines.size() == 2
                            && containsTranslatableKey(defaultRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only")
                            && containsTranslatableKey(defaultRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_no_recast"),
                    "Reflectcast Shield should show instant-only and no-recast restrictions without Silver Ring");
            helper.assertFalse(item.supportsManaBypass(SpellRegistry.SENSE_EVIL.get()),
                    "Reflectcast Shield should consume normal spell mana");
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.SENSE_EVIL.get(), 1),
                    "Reflectcast Shield should use instant spells without calibration");
            helper.assertFalse(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Reflectcast Shield should require Silver Ring for long spells");
            helper.assertFalse(item.canUseConfiguredSpell(stack,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Reflectcast Shield should require Silver Ring for continuous spells");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 0, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 1, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.hasSilverRing(stack),
                    "Reflectcast Shield should store Silver Ring calibration");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.hasWisdomShard(stack),
                    "Reflectcast Shield should store Wisdom Shard calibration alongside Silver Ring");
            var wisdomTooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), wisdomTooltipLines, TooltipFlag.Default.NORMAL);
            helper.assertTrue(containsTranslatableKey(wisdomTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_wisdom"),
                    "Wisdom Shard should switch Reflectcast Shield to the selected-spell cast tooltip");
            helper.assertFalse(containsTranslatableKey(wisdomTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_default"),
                    "Wisdom Shard should hide Reflectcast Shield's imbued-spell cast tooltip");
            var silverAbilityLines = collectReflectcastAbilityTooltipLines(helper, item, stack);
            helper.assertTrue(containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_long_to_instant")
                            && containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_hold_continuous")
                            && containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_extend_cooldown"),
                    "Silver Ring should show all Reflectcast Shield ability lines");
            var silverRestrictionLines = item.getImbueRestrictionTooltipLines(stack);
            helper.assertTrue(silverRestrictionLines.size() == 1
                            && containsTranslatableKey(silverRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_no_recast"),
                    "Silver Ring should leave only the no-recast restriction");
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Silver Ring should allow long spells");
            helper.assertTrue(item.canUseConfiguredSpell(stack,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Silver Ring should allow continuous spells");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "reflectcast_shield_calibration_test");
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
            for (var slot = 0; slot < 3; slot++) {
                helper.assertTrue(menu.isAdjustmentSlotEnabled(slot),
                        "Reflectcast Shield adjustment slot should be enabled: " + slot);
            }

            var castStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    castStack, 0, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            var castContainer = ISpellContainer.create(1, false, false).mutableCopy();
            castContainer.addSpellAtIndex(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false);
            ISpellContainer.set(castStack, castContainer.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, castStack);
            castStack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(player.getUseItemRemainingTicks() == castStack.getUseDuration(player),
                    "Reflectcast Shield should keep vanilla shield block preparation time");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Reflectcast Shield continuous cast requires MagicData");
            magicData.setMana(1000.0F);
            var longSpell = SpellRegistry.MANTIS_LEAP.get();
            var baseCooldown = 80;
            magicData.setPlayerCastingItem(castStack);
            var longCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    baseCooldown, longSpell, player, CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime
                    .onSpellCooldownAdded(longCooldownEvent);
            helper.assertTrue(longCooldownEvent.getEffectiveCooldown()
                            == baseCooldown + longSpell.getEffectiveCastTime(1, player),
                    "Reflectcast Shield should extend LONG cooldown by its effective cast time");
            magicData.setPlayerCastingItem(ItemStack.EMPTY);
            var manaBeforeCast = magicData.getMana();
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "A valid block trigger should start Reflectcast continuous casting immediately");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime
                            .shouldBypassMagicManager(magicData),
                    "Reflectcast continuous casting should bypass Iron's standard cast tick");
            helper.assertTrue(magicData.getMana() < manaBeforeCast,
                    "Reflectcast continuous casting should consume normal spell mana");
            helper.assertFalse(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "Additional blocks should not restart an active continuous cast");
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.finishUse(player);
            helper.assertFalse(magicData.isCasting(),
                    "Releasing Reflectcast Shield should clear its continuous casting state");
            player.stopUsingItem();
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.clear(player);
        });
    }

    static void spellgunServerConfigDefaultsMatchCurrentHardcodedValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunMaxInstantImbueCooldownTicks() == 20 * 5,
                    "Iron Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunOverriddenSpellCooldownTicks() == 10,
                    "Iron Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunMaxInstantImbueCooldownTicks() == 20 * 10,
                    "Copper Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunOverriddenSpellCooldownTicks() == 20,
                    "Copper Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunMaxInstantImbueCooldownTicks() == 20 * 20,
                    "Gold Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunOverriddenSpellCooldownTicks() == 40,
                    "Gold Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.diamondSpellgunMaxInstantImbueCooldownTicks() == 20 * 30,
                    "Diamond Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.diamondSpellgunOverriddenSpellCooldownTicks() == 80,
                    "Diamond Spellcaster Gun cast cooldown default changed");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_default_config_test");
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 10,
                    "Iron Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()), spell, 200, 20,
                    "Copper Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 200, 40,
                    "Gold Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()), spell, 200, 80,
                    "Diamond Spellcaster Gun should keep its default cast cooldown");
        });
    }
    static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    0,
                    10,
                    20 * 10,
                    20,
                    20 * 20,
                    40,
                    20 * 30,
                    80
            ))) {
                var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
                var cooldownLimitedSpell = SpellRegistry.SEARCH_BEACON.get();
                helper.assertTrue(cooldownLimitedSpell.getSpellCooldown() > 20 * 5,
                        "Search Beacon should remain above Iron Spellcaster Gun's default cooldown limit");
                helper.assertTrue(iron.canImbueSpell(cooldownLimitedSpell, 1),
                        "Iron Spellcaster Gun maxInstantImbueCooldownTicks=0 should disable only the cooldown limit");
                helper.assertFalse(iron.canImbueSpell(SpellRegistry.HIGANBANA.get(), 1),
                        "Iron Spellcaster Gun should still reject recast spells when only the cooldown limit is disabled");
            }
        });
    }
    static void spellgunZeroCastCooldownConfigForcesZeroCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    20 * 5,
                    0,
                    20 * 10,
                    0,
                    20 * 20,
                    0,
                    20 * 30,
                    0
            ))) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_zero_cooldown_config_test");
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Iron Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
                assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Diamond Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
            }
        });
    }
    static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            applyRestrictedImbueNormalization(helper, stack, item, spell, 1);

            var player = createArcherMultiplePlayer(helper, new BlockPos(0, 12, 0), "spellgun_recast_ammo_bypass_test");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Selected offhand Spellcaster Gun should consume the input even when its cast fails without ammo");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(magicData.isCasting(),
                    "Failed initial Archer Multiple cast should not start casting");
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);

            var recastUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(recastUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should allow recast without ammo");

            var ammoStack = new ItemStack(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 1);
            player.getInventory().add(ammoStack);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);
            magicData.setPlayerCastingItem(stack);
            NeoForge.EVENT_BUS.post(new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SWORD
            ));
            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get()
            ) == 1, "Recast Spellcaster Gun cast should not consume ammo from the cast event");
        });
    }

    static void spellgunHandUseContractDoesNotFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var imbuedStack = createInitializedPresetStack(item);
            applyRestrictedImbueNormalization(
                    helper,
                    imbuedStack,
                    item,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1
            );
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_hand_contract_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, imbuedStack);

            var mainHandUse = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertFalse(mainHandUse.getResult().consumesAction(),
                    "Mainhand Spellgun right-click should always pass without casting");
            helper.assertFalse(RightClickSpellItemHelper.hasMainHandRightClickBehavior(player, imbuedStack),
                    "Mainhand Spellgun should expose no right-click behavior to offhand magic items");

            var emptyStack = new ItemStack(item);
            ISpellContainer.set(emptyStack, ISpellContainer.create(1, false, false));
            player.setItemInHand(InteractionHand.OFF_HAND, emptyStack);
            var offhandUse = item.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(offhandUse.getResult().consumesAction(),
                    "Selected offhand Spellgun should consume right-click even when it is not imbued");
        });
    }

    static void spellgunCastAttemptPreservesExistingCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            applyRestrictedImbueNormalization(
                    helper,
                    stack,
                    item,
                    SpellRegistry.ARCHER_MULTIPLE.get(),
                    1
            );
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_existing_cast_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
            var activeSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            magicData.initiateCast(
                    activeSpell,
                    1,
                    activeSpell.getEffectiveCastTime(1, player),
                    CastSource.SPELLBOOK,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(item.tryTriggerImbuedSpell(player, InteractionHand.MAIN_HAND, null),
                    "Spellgun should reject a cast while another spell is already casting");
            helper.assertTrue(magicData.isCasting(),
                    "Rejected Spellgun cast should preserve the existing casting state");
            helper.assertTrue(activeSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Rejected Spellgun cast should preserve the existing spell id");
        });
    }

    private static void assertSpellgunAbilityTooltipKeys(
            GameTestHelper helper,
            AbstractSpellGunItem item,
            boolean expectInstantLongCast,
            String itemName
    ) {
        var lines = collectSpellgunAbilityTooltipLines(helper, item);
        var hasInstantLongCast = containsTranslatableKey(
                lines,
                "item.apprenticecodex.spellgun.tooltip.ability_long_to_instant"
        );
        var hasReduceCast = containsTranslatableKey(
                lines,
                "item.apprenticecodex.spellgun.tooltip." + "ability_reduce_" + "cast"
        );
        helper.assertTrue(hasInstantLongCast == expectInstantLongCast,
                itemName + " instant LONG cast tooltip mismatch");
        helper.assertFalse(hasReduceCast,
                itemName + " should not show removed reduce-cast tooltip key");
    }

    @SuppressWarnings("unchecked")
    private static List<Component> collectSpellgunAbilityTooltipLines(GameTestHelper helper, AbstractSpellGunItem item) {
        try {
            var method = AbstractSpellGunItem.class.getDeclaredMethod("collectSpellGunAbilityTooltipSection");
            method.setAccessible(true);
            return (List<Component>) method.invoke(item);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Spellgun ability tooltip reflection failed: " + exception);
            return List.of();
        }
    }

    private static boolean containsTranslatableKey(List<Component> lines, String key) {
        return lines.stream().anyMatch(component ->
                component.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey())
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Component> collectReflectcastAbilityTooltipLines(
            GameTestHelper helper,
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield item,
            ItemStack stack
    ) {
        try {
            var method = jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.class
                    .getDeclaredMethod("getImbueShieldAbilityTooltipSection", ItemStack.class);
            method.setAccessible(true);
            return (List<Component>) method.invoke(item, stack);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Reflectcast Shield ability tooltip reflection failed: " + exception);
            return List.of();
        }
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable after save/load");
        });
    }
    static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Spellcaster Gun should stay cleared after save/load");
        });
    }
    static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun legacy locked replacement should be recovered after save/load");
        });
    }
}
