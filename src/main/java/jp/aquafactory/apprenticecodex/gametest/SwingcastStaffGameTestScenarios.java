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
import io.redspace.ironsspellbooks.api.spells.CastResult;
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
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
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
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
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
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlash;
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
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class SwingcastStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private SwingcastStaffGameTestScenarios() {
    }
    static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Swingcast Staff did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Swingcast Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(),
                    "Copper Swingcast Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Swingcast Staff preset spell level mismatch: " + spellData.getLevel());
        });
    }
    static void crystalBladedStaffStartsWithHiddenManaSlash(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(item instanceof SwingTriggeredMagicItem,
                    "Crystal Bladed Staff should trigger imbued spells on swing");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff spell container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff preset spell should stay hidden from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Crystal Bladed Staff should start with locked Mana Slash");
        });
    }

    static void crystalBladedStaffMissSwingCastsManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_miss_cast");
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setYHeadRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff miss cast test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true),
                "Crystal Bladed Staff miss trigger should be accepted"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff miss swing should cast Mana Slash but got " + magicData.getCastingSpellId());
            helper.assertTrue(ItemStack.isSameItemSameComponents(magicData.getPlayerCastingItem(), stack),
                    "Crystal Bladed Staff miss swing should cast with the staff stack");
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Crystal Bladed Staff miss swing should mark the mainhand casting slot");
            helper.succeed();
        });
    }

    static void crystalBladedStaffHitSwingDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff hit suppression test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true),
                "Crystal Bladed Staff hit suppression trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "Crystal Bladed Staff hit suppression test should deal direct player attack damage");

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff hit swing should not cast Mana Slash");
            helper.succeed();
        });
    }

    static void crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_vanilla_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff vanilla hit test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff vanilla hit trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, target));

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff vanilla hit should not cast Mana Slash");
            helper.succeed();
        });
    }

    static void crystalBladedStaffDelayedHitDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_delayed_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff delayed hit test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff delayed hit trigger should be accepted"
        );
        helper.runAfterDelay(1, () -> CrystalBladedStaffAttackContextManager.recordRecentCrystalBladedStaffHit(
                player,
                InteractionHand.MAIN_HAND
        ));

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff delayed hit should not cast Mana Slash");
            helper.succeed();
        });
    }

    static void crystalBladedStaffMissTriggerDoesNotUseSwappedStack(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var originalStack = new ItemStack(item);
        item.initializeSpellContainer(originalStack);
        var swappedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var swappedStack = createLegacyCrystalBladedStaffContainer(swappedSpell, 1, false);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_swap_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff swapped stack test could not resolve player mana data");
        magicData.setMana(1000.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff swapped stack miss trigger should be accepted"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, swappedStack);

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff swapped stack should not cast the original Mana Slash");
            helper.assertTrue(!swappedSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff swapped stack should not cast the replacement staff spell");
            helper.succeed();
        });
    }

    static void crystalBladedStaffPendingMissTriggerKeepsEarlierHand(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var mainSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var mainStack = createLegacyCrystalBladedStaffContainer(mainSpell, 1, false);
        var offhandStack = new ItemStack(item);
        item.initializeSpellContainer(offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_dual_pending");
        player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff dual pending test could not resolve player mana data");
        magicData.setMana(1000.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 1),
                "Crystal Bladed Staff mainhand miss trigger should be accepted"
        );
        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.OFF_HAND, true, 10),
                "Crystal Bladed Staff offhand miss trigger should be accepted without overwriting mainhand"
        );

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(mainSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff mainhand pending trigger should cast first but got "
                            + magicData.getCastingSpellId());
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(
                            magicData.getCastingEquipmentSlot()),
                    "Crystal Bladed Staff mainhand pending trigger should mark the mainhand casting slot");
            helper.succeed();
        });
    }

    static void crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var offhandStack = new ItemStack(item);
        item.initializeSpellContainer(offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_offhand_miss");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff offhand miss test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.OFF_HAND, true, 2),
                "Crystal Bladed Staff offhand miss trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "Crystal Bladed Staff offhand miss test should deal mainhand player attack damage");

        helper.succeedWhen(() -> {
            helper.assertTrue(SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Mainhand hit should not suppress offhand Crystal Bladed Staff miss cast but got "
                            + magicData.getCastingSpellId());
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(
                            magicData.getCastingEquipmentSlot()),
                    "Offhand Crystal Bladed Staff miss cast should mark the offhand casting slot");
            helper.succeed();
        });
    }

    static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = createLegacyCrystalBladedStaffContainer(SpellRegistry.MANA_SLASH.get(), 1, true);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_preset");

            item.inventoryTick(stack, helper.getLevel(), player, 0, true);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff rescued preset container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff legacy preset should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Crystal Bladed Staff legacy preset should stay locked after rescue");
        });
    }
    static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = createLegacyCrystalBladedStaffContainer(SpellRegistry.MANA_SLASH.get(), 1, true);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_offhand");

            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            item.inventoryTick(stack, helper.getLevel(), player, 40, false);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Offhand Crystal Bladed Staff rescued preset container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Offhand Crystal Bladed Staff legacy preset should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Offhand Crystal Bladed Staff legacy preset should stay locked after rescue");
        });
    }
    static void crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var stack = createLegacyCrystalBladedStaffContainer(replacementSpell, 1, false);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_replacement");

            item.inventoryTick(stack, helper.getLevel(), player, 0, true);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff rescued replacement container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff legacy replacement should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Crystal Bladed Staff legacy replacement should stay removable after rescue");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Crystal Bladed Staff legacy replacement should remain extractable after rescue");
        });
    }
    static void manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_offhand_damage");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, stack);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Slash offhand damage test could not resolve player mana data");
        magicData.setMana(100.0F);

        try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), stack, SpellRegistry.MANA_SLASH.get())) {
            SpellRegistry.MANA_SLASH.get().onCast(
                    helper.getLevel(),
                    1,
                    player,
                    CastSource.SWORD,
                    magicData
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close Mana Slash swingcast context.", e);
        }

        helper.succeedWhen(() -> {
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    ManaSlashProjectileEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.size() == 1,
                    "Mana Slash offhand damage test should spawn exactly one projectile but got " + projectiles.size());

            var damageTag = new CompoundTag();
            projectiles.get(0).saveWithoutId(damageTag);
            var actualDamage = damageTag.getFloat("Damage");
            var expectedDamage = Math.max(
                    1.0F,
                    ManaSlash.resolveCatalystWeaponDamage(player, stack)
                            * SpellRegistry.MANA_SLASH.get().getSpellPower(1, player) / 100.0F
            ) * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MANA_SLASH);
            helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-4F,
                    "Mana Slash offhand damage should use offhand catalyst attack damage: expected "
                            + expectedDamage + " but got " + actualDamage);
        });
    }
    static void manaSlashCatalystDamageUsesStackAttributeModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_stack_attribute");
            var catalystStack = new ItemStack(Items.STICK);
            catalystStack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                            ApprenticeCodex.MODID,
                                            "mana_slash_gametest_stack_attack_damage"
                                    ),
                                    20.0D,
                                    AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.MAINHAND
                    )
                    .build());

            var resolvedDamage = ManaSlash.resolveCatalystWeaponDamage(player, catalystStack);
            helper.assertTrue(Math.abs(resolvedDamage - 21.0F) < 1.0e-4F,
                    "Mana Slash catalyst damage should include stack AttributeModifiers NBT: " + resolvedDamage);
        });
    }

    static void manaSlashCatalystDamageUsesApplicableSlotGroups(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_slot_group_attribute");
            var catalystStack = new ItemStack(Items.STICK);
            catalystStack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                            ApprenticeCodex.MODID,
                                            "mana_slash_gametest_hand_attack_damage"
                                    ),
                                    20.0D,
                                    AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HAND
                    )
                    .add(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                            ApprenticeCodex.MODID,
                                            "mana_slash_gametest_any_attack_damage"
                                    ),
                                    5.0D,
                                    AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.ANY
                    )
                    .build());

            var resolvedDamage = ManaSlash.resolveCatalystWeaponDamage(player, catalystStack);
            helper.assertTrue(Math.abs(resolvedDamage - 26.0F) < 1.0e-4F,
                    "Mana Slash catalyst damage should include HAND and ANY slot group modifiers: " + resolvedDamage);
        });
    }

    static void manaSlashCatalystDamageAppliesAttributeEventOnce(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_attribute_event");
            var catalystStack = new ItemStack(Items.STICK);
            catalystStack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                            ApprenticeCodex.MODID,
                                            "mana_slash_gametest_event_base_attack_damage"
                                    ),
                                    20.0D,
                                    AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.MAINHAND
                    )
                    .build());

            var eventCalls = new AtomicInteger();
            Consumer<ItemAttributeModifierEvent> listener = event -> {
                if (event.getItemStack() == catalystStack) {
                    eventCalls.incrementAndGet();
                    event.addModifier(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                            ApprenticeCodex.MODID,
                                            "mana_slash_gametest_event_attack_damage"
                                    ),
                                    5.0D,
                                    AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.MAINHAND
                    );
                }
            };

            NeoForge.EVENT_BUS.addListener(listener);
            try {
                var resolvedDamage = ManaSlash.resolveCatalystWeaponDamage(player, catalystStack);
                helper.assertTrue(eventCalls.get() == 1,
                        "Mana Slash catalyst damage should fire ItemAttributeModifierEvent once but got " + eventCalls.get());
                helper.assertTrue(Math.abs(resolvedDamage - 26.0F) < 1.0e-4F,
                        "Mana Slash catalyst damage should include NBT and one event modifier: " + resolvedDamage);
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        });
    }

    static void manaSlashDamageMultiplierAppliesAfterMinimumDamage(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_low_multiplier");
        var catalystStack = new ItemStack(Items.STICK);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Slash low multiplier test could not resolve player mana data");
        magicData.setMana(100.0F);

        try (var configOverride = ApprenticeCodexServerConfig.useDamageMultiplierOverrideForGameTest(
                DamageMultiplierKey.MANA_SLASH,
                0.5D
        );
             var ignored = SwingcastStaffCastContext.open(player.getUUID(), catalystStack, SpellRegistry.MANA_SLASH.get())) {
            SpellRegistry.MANA_SLASH.get().onCast(
                    helper.getLevel(),
                    1,
                    player,
                    CastSource.SWORD,
                    magicData
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close Mana Slash low multiplier test context.", e);
        }

        helper.succeedWhen(() -> {
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    ManaSlashProjectileEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.size() == 1,
                    "Mana Slash low multiplier test should spawn exactly one projectile but got " + projectiles.size());

            var damageTag = new CompoundTag();
            projectiles.get(0).saveWithoutId(damageTag);
            var actualDamage = damageTag.getFloat("Damage");
            helper.assertTrue(Math.abs(actualDamage - 0.5F) < 1.0e-4F,
                    "Mana Slash low damage multiplier should apply after minimum damage: expected 0.5 but got "
                            + actualDamage);
        });
    }
    static void manaSlashAllowsNonSwingcastPrecondition(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_non_swingcast");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Slash non-swingcast test could not resolve player mana data");
            magicData.setMana(100.0F);

            var castResult = SpellRegistry.MANA_SLASH.get().canBeCastedBy(1, CastSource.SWORD, magicData, player);
            helper.assertTrue(castResult.isSuccess(),
                    "Mana Slash should keep non-swingcast cast paths available");
        });
    }
    static void manaSlashRequiresSwingcastCatalystWhenContextIsActive(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_missing_catalyst");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Slash missing catalyst test could not resolve player mana data");
            magicData.setMana(100.0F);

            CastResult castResult;
            try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), ItemStack.EMPTY, SpellRegistry.MANA_SLASH.get())) {
                castResult = SpellRegistry.MANA_SLASH.get().canBeCastedBy(1, CastSource.SWORD, magicData, player);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to close Mana Slash swingcast context.", e);
            }

            helper.assertTrue(castResult.type == CastResult.Type.FAILURE,
                    "Mana Slash should fail before swingcast when swingcast catalyst is missing");
            helper.assertTrue(castResult.message != null,
                    "Mana Slash missing catalyst failure should provide a message");
            var contents = castResult.message.getContents();
            helper.assertTrue(contents instanceof TranslatableContents,
                    "Mana Slash missing catalyst message should be translatable");
            var translatableContents = (TranslatableContents) contents;
            helper.assertTrue("ui.apprenticecodex.mana_slash.missing_catalyst".equals(translatableContents.getKey()),
                    "Mana Slash missing catalyst message should use the dedicated translation key");
        });
    }
    static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var initialContainer = ISpellContainer.get(stack);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            helper.assertTrue(initialContainer != null, "Copper Swingcast Staff spell container is null");
            assertSpellData(helper, initialContainer, 0, io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, true,
                    "Copper Swingcast Staff preset spell should remain locked");

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var normalizedContainer = ISpellContainer.get(stack);
            helper.assertTrue(normalizedContainer != null, "Copper Swingcast Staff normalized spell container is null");
            assertSpellData(helper, normalizedContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff replacement spell should be removable");
            helper.assertTrue(normalizedContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff replacement spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Iron Swingcast Staff imbued spell should remain extractable after save/load");
        });
    }

    static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff preset-equivalent save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain extractable after save/load");
        });
    }
    static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Swingcast Staff should stay cleared after save/load");
        });
    }
    static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff legacy locked replacement should be recovered after save/load");
        });
    }
    static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var instantSpell = SpellRegistry.AUTO_MAGNET.get();
            var longSpell = SpellRegistry.ARCANE_BLAST.get();
            var continuousSpell = SpellRegistry.BULLET_STREAM.get();

            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Iron Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Copper Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.SILVER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Silver Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.GOLD_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Gold Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Diamond Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.NETHERITE_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Netherite Swingcast Staff"
            );
        });
    }

    private static ItemStack createLegacyCrystalBladedStaffContainer(
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            int spellLevel,
            boolean locked
    ) {
        var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
        var mutable = ISpellContainer.create(1, true, false).mutableCopy();
        mutable.addSpellAtIndex(spell, spellLevel, 0, locked);
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

}
