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

final class AutocastAmuletGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private AutocastAmuletGameTestScenarios() {
    }
    static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var spellContainer = ISpellContainer.get(stack);
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var necklaceTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT)
            );

            helper.assertTrue(spellContainer != null, "Autocast Amulet default spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Autocast Amulet default slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Autocast Amulet should stay hidden from the spell wheel");
            helper.assertTrue(stack.is(necklaceTag),
                    "Autocast Amulet should be tagged as curios:necklace");
            helper.assertTrue(item.canImbueSpell(apprenticeSpell, 1),
                    "Autocast Amulet should allow sense_evil by default");
            helper.assertTrue(item.canImbueSpell(ironsHeal, 1),
                    "Autocast Amulet should allow Iron's heal by default");
            helper.assertFalse(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Autocast Amulet should reject non-allowlisted spells");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(apprenticeSpell.getSpellResource()),
                    "Autocast Amulet allowlist should contain sense_evil");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(ironsHeal.getSpellResource()),
                    "Autocast Amulet allowlist should contain Iron's heal");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().size() == 19,
                    "Autocast Amulet default allowlist size mismatch: " + AutocastAmuletSpellListManager.getAllowlist().size());
        });
    }

    static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var mutable = ISpellContainer.create(5, false, false).mutableCopy();

            helper.assertTrue(mutable.addSpellAtIndex(apprenticeSpell, 1, 0, false),
                    "Failed to prepare allowlisted sense_evil for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 1, false),
                    "Failed to prepare blocked magic_missile for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsHeal, 1, 2, false),
                    "Failed to prepare allowlisted heal for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsGreaterHeal, 1, 3, false),
                    "Failed to prepare allowlisted greater_heal for Autocast Amulet normalization test");
            ISpellContainer.set(stack, mutable.toImmutable());

            item.normalizeImbuedSpellContainer(stack);

            var normalized = ISpellContainer.get(stack);
            helper.assertTrue(normalized != null, "Autocast Amulet normalized spell container is null");
            helper.assertTrue(normalized != null && normalized.getMaxSpellCount() == 3,
                    "Autocast Amulet normalization should clamp slot count to 3 but got " + (normalized == null ? -1 : normalized.getMaxSpellCount()));
            helper.assertTrue(normalized != null && normalized.getActiveSpellCount() == 3,
                    "Autocast Amulet normalization should keep only 3 allowlisted spells but got " + (normalized == null ? -1 : normalized.getActiveSpellCount()));
            assertSpellData(helper, normalized, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet normalization should preserve the first allowlisted spell");
            assertSpellData(helper, normalized, 1, ironsHeal, 1, false,
                    "Autocast Amulet normalization should compact later allowlisted spells");
            assertSpellData(helper, normalized, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet normalization should preserve allowlisted order after filtering");

            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(1) - 1.0D) < 1.0e-9D,
                    "Autocast Amulet single-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(2) - 1.44D) < 1.0e-9D,
                    "Autocast Amulet two-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(3) - 1.96D) < 1.0e-9D,
                    "Autocast Amulet three-spell mana multiplier regression");
            helper.assertTrue(AutocastAmulet.getScaledManaCost(ironsHeal, 1, 3) == 59,
                    "Autocast Amulet scaled mana cost should round heal to 59 at 3 active spells");
        });
    }
    static void autocastAmuletSpellSlotUpgradeStopsAtThreeAndKeepsOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(apprenticeSpell, 1)
            );

            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the first lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsHeal, 1));
            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the second lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsGreaterHeal, 1));

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Autocast Amulet upgraded spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 3,
                    "Autocast Amulet spell slot upgrade should stop at 3 slots");
            assertSpellData(helper, spellContainer, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet slot upgrade should preserve the first spell");
            assertSpellData(helper, spellContainer, 1, ironsHeal, 1, false,
                    "Autocast Amulet slot upgrade should preserve the second spell");
            assertSpellData(helper, spellContainer, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet slot upgrade should append the third spell at the tail");
            helper.assertTrue(item.createSpellSlotUpgradeResult(stack, upgradeItem).isEmpty(),
                    "Autocast Amulet should reject a fourth spell slot upgrade");
        });
    }
    static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    3,
                    new SpellData(apprenticeSpell, 1),
                    new SpellData(ironsHeal, 1),
                    new SpellData(ironsGreaterHeal, 1)
            );
            var spellContainer = ISpellContainer.get(stack);

            helper.assertTrue(spellContainer != null, "Autocast Amulet workbench extraction spell container is null");
            var extractionIndex = item.getWorkbenchSpellExtractionIndex(stack, spellContainer);
            helper.assertTrue(extractionIndex == 2,
                    "Autocast Amulet workbench extraction should target the last filled slot but got " + extractionIndex);
            helper.assertTrue(item.canRemoveWorkbenchSpell(stack, spellContainer, extractionIndex, spellContainer.getSpellAtIndex(extractionIndex)),
                    "Autocast Amulet should allow removing its tail spell in Spellcaster Workbench");

            var mutable = spellContainer.mutableCopy();
            helper.assertTrue(mutable.removeSpellAtIndex(extractionIndex),
                    "Autocast Amulet tail spell should be removable from the mutable container");
            ISpellContainer.set(stack, mutable.toImmutable());
            item.normalizeImbuedSpellContainer(stack);

            var remaining = ISpellContainer.get(stack);
            helper.assertTrue(remaining != null, "Autocast Amulet remaining spell container is null after extraction");
            helper.assertTrue(remaining != null && remaining.getMaxSpellCount() == 3,
                    "Autocast Amulet should preserve max slot count after extraction");
            helper.assertTrue(remaining != null && remaining.getActiveSpellCount() == 2,
                    "Autocast Amulet should keep the first two spells after tail extraction");
            assertSpellData(helper, remaining, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet should keep the first spell after tail extraction");
            assertSpellData(helper, remaining, 1, ironsHeal, 1, false,
                    "Autocast Amulet should keep the second spell after tail extraction");
            helper.assertTrue(remaining != null && remaining.getSpellAtIndex(2) == SpellData.EMPTY,
                    "Autocast Amulet should clear only the tail spell slot after extraction");
        });
    }
    static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_first_interval_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(200.0F);
            equipNecklaceCurio(player, stack);

            runAutocastAmuletServerTick(player, 19);
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet should stay idle before the first 20 tick interval");
            runAutocastAmuletServerTick(player, 20);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet should start casting charge on the first castable interval after equip");
            helper.assertTrue(spell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet should start the imbued charge spell on the first castable interval");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet should cast charge at the imbued spell level");
            helper.succeed();
        });
    }
    static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_mana_retry_test");

        helper.runAtTickTime(1, () -> {
            var expensiveSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var fallbackSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var expensiveCost = AutocastAmulet.getScaledManaCost(expensiveSpell, 1, 2);
            var fallbackCost = AutocastAmulet.getScaledManaCost(fallbackSpell, 1, 2);
            helper.assertTrue(expensiveCost > fallbackCost,
                    "Autocast Amulet mana retry test requires the first spell to cost more mana than the fallback spell");

            var stack = createAutocastAmuletStack(
                    helper,
                    2,
                    new SpellData(expensiveSpell, 1),
                    new SpellData(fallbackSpell, 1)
            );
            equipNecklaceCurio(player, stack);
            var equippedStack = getEquippedAutocastAmulet(player);

            var magicData = MagicData.getPlayerMagicData(player);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 8.0F));
            magicData.getSyncedData().learnSpell(expensiveSpell, false);
            magicData.getSyncedData().learnSpell(fallbackSpell, false);
            magicData.setMana(fallbackCost);

            runAutocastAmuletServerTick(player, 20);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should stop immediately when the first spell lacks mana");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell in the blocked mana sequence");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) == 80L,
                    "Autocast Amulet mana retry should wait exactly 60 ticks after the failed sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) == 0,
                    "Autocast Amulet mana retry should skip the errored slot once on the delayed retry");

            runAutocastAmuletServerTick(player, 40);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should not retry again before the delayed retry sequence");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell before the delayed retry sequence");

            runAutocastAmuletServerTick(player, 80);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet delayed retry should skip the errored slot and cast the fallback spell");
            helper.assertTrue(fallbackSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet delayed retry should cast the next spell after skipping the errored slot");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet delayed retry should cast charge at the imbued spell level");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) < 0L,
                    "Autocast Amulet should clear the delayed retry state after consuming the one-shot skipped sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) < 0,
                    "Autocast Amulet should clear the skipped slot marker after the delayed retry sequence");
            helper.succeed();
        });
    }
    static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_creative_mana_test");

        helper.runAtTickTime(1, () -> {
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            equipNecklaceCurio(player, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(0.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            runAutocastAmuletServerTick(player, 20);
            var cooldownInstance = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet creative test should still cast greater_heal with zero mana");
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet creative LONG cast should still complete immediately");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Autocast Amulet creative cast should not consume mana but got " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet creative cast should still add spell cooldown");
            helper.assertTrue(cooldownInstance != null && cooldownInstance.getSpellCooldown() == expectedCooldown,
                    "Autocast Amulet creative cast should store the helper cooldown amount but got "
                            + (cooldownInstance == null ? "null" : cooldownInstance.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }
    static void autocastAmuletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_cooldown_test");
            var stack = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Autocast Amulet cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            var ironsSwordCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    ironsSwordCooldown,
                    spell,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Autocast Amulet cooldown event should use the helper cooldown amount but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == ironsSwordCooldown,
                    "Autocast Amulet cooldown event should keep Iron's sword multiplier path but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + ironsSwordCooldown);
        });
    }
    static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_long_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(300.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            helper.assertTrue(invokeAutocastBeginCast(
                            player,
                            magicData,
                            stack,
                            new SpellData(spell, 1),
                            1,
                            "necklace_0",
                            AutocastAmulet.getScaledManaCost(spell, 1, 1)
                    ),
                    "Autocast Amulet should start greater_heal from the auto-cast path");
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet should resolve greater_heal immediately from the auto-cast path");
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(),
                    "Autocast Amulet LONG cast should complete immediately instead of leaving the player casting");
            helper.succeed();
        });
    }
    static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");

            controller.queueCooldownCast(100L, spellId, icon, 1300);

            var active = controller.getActiveNotification();
            helper.assertTrue(active != null, "Autocast Amulet notification controller should show the cast notification immediately");
            if (active != null) {
                helper.assertTrue(active.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST,
                        "Autocast Amulet cast notification should use the CAST kind");
                helper.assertTrue(active.displaySeconds() == 65,
                        "Autocast Amulet cast notification should display the rounded cooldown seconds");
                helper.assertTrue("65s".equals(active.displayText()),
                        "Autocast Amulet cast notification text should include the seconds suffix");
            }

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 3,
                    "Autocast Amulet 65 second cooldown should schedule 60/30/10 notifications but got " + scheduled.size());
            if (scheduled.size() == 3) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 200L && scheduled.get(0).entry().displaySeconds() == 60,
                        "Autocast Amulet 60 second notification should trigger when 60 seconds remain");
                helper.assertTrue(scheduled.get(1).triggerTick() == 800L && scheduled.get(1).entry().displaySeconds() == 30,
                        "Autocast Amulet 30 second notification should trigger when 30 seconds remain");
                helper.assertTrue(scheduled.get(2).triggerTick() == 1200L && scheduled.get(2).entry().displaySeconds() == 10,
                        "Autocast Amulet 10 second notification should trigger when 10 seconds remain");
            }
        });
    }
    static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");

            controller.queueCooldownCast(0L, spellId, icon, 500);

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 1,
                    "Autocast Amulet 25 second cooldown should only schedule the 10 second notification but got " + scheduled.size());
            if (scheduled.size() == 1) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 300L,
                        "Autocast Amulet 25 second cooldown should trigger the 10 second notification after 15 seconds");
                helper.assertTrue(scheduled.get(0).entry().displaySeconds() == 10,
                        "Autocast Amulet short cooldown should keep the 10 second label");
            }
        });
    }
    static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var healId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var healIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");
            var chargeId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var chargeIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");
            var manaLowId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heal");
            var manaLowIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/heal.png");
            var delayedId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath");
            var delayedIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/fire_breath.png");

            controller.queueCooldownCast(0L, healId, healIcon, 1300);
            controller.queueCooldownCast(1L, chargeId, chargeIcon, 800);
            helper.assertTrue(controller.getPendingQueueSize() == 1,
                    "Autocast Amulet overlapping cast notifications should queue instead of drawing together");

            controller.advance(30L);
            var secondCast = controller.getActiveNotification();
            helper.assertTrue(secondCast != null && secondCast.spellId().equals(chargeId),
                    "Autocast Amulet queued cast notification should appear after the first cast display finishes");

            controller.queueManaLow(30L, manaLowId, manaLowIcon);
            controller.advance(60L);
            var manaLow = controller.getActiveNotification();
            helper.assertTrue(manaLow != null
                            && manaLow.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.MANA_LOW
                            && "MP!".equals(manaLow.displayText()),
                    "Autocast Amulet mana-low notification should use the dedicated minimal overlay text");

            controller.queueCooldownCast(85L, delayedId, delayedIcon, 400);
            controller.advance(100L);
            var stillBlockedByQueue = controller.getActiveNotification();
            helper.assertTrue(stillBlockedByQueue != null
                            && stillBlockedByQueue.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST
                            && stillBlockedByQueue.spellId().equals(delayedId),
                    "Autocast Amulet threshold notification should wait until earlier queued notifications finish");

            controller.advance(130L);
            var delayedThreshold = controller.getActiveNotification();
            helper.assertTrue(delayedThreshold != null
                            && delayedThreshold.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.THRESHOLD
                            && "60s".equals(delayedThreshold.displayText()),
                    "Autocast Amulet delayed threshold notification should keep the original 60 second label");
        });
    }

    static void autocastAmuletNotificationControllerUpdatesLinearBuildRemaining(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var linearController = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var linearId = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "linear_build");
            var castId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var castIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");

            linearController.updateLinearBuildRemaining(0L, linearId, new ItemStack(Items.FERN), "10");
            linearController.updateLinearBuildRemaining(5L, linearId, new ItemStack(Items.FERN), "9");
            linearController.advance(34L);

            var activeLinear = linearController.getActiveNotification();
            helper.assertTrue(activeLinear != null
                            && activeLinear.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.LINEAR_BUILD_REMAINING
                            && "9".equals(activeLinear.displayText()),
                    "Linear Build remaining notification should update the active entry and refresh its display duration");

            var queuedController = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            queuedController.queueCooldownCast(0L, castId, castIcon, 1200);
            queuedController.updateLinearBuildRemaining(1L, linearId, new ItemStack(Items.FERN), "10");
            queuedController.updateLinearBuildRemaining(2L, linearId, new ItemStack(Items.FERN), "9");
            helper.assertTrue(queuedController.getPendingQueueSize() == 1,
                    "Linear Build remaining notifications should keep only the latest pending entry");

            queuedController.advance(30L);
            var queuedLinear = queuedController.getActiveNotification();
            helper.assertTrue(queuedLinear != null
                            && queuedLinear.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.LINEAR_BUILD_REMAINING
                            && "9".equals(queuedLinear.displayText()),
                    "Linear Build remaining notification should show the latest queued value after earlier notifications finish");
        });
    }
}
