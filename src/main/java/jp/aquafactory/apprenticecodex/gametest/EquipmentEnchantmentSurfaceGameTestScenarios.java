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
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellchargedGreatswordServerConfig;
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
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressCastEvent;
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
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitStats;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
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
import net.minecraft.nbt.DoubleTag;
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

final class EquipmentEnchantmentSurfaceGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentEnchantmentSurfaceGameTestScenarios() {
    }

    static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    ItemStack.EMPTY,
                    spell,
                    "scrollcaster_gauntlet_offhand_empty_mainhand_test"
            );
            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    new ItemStack(Items.STONE_SWORD),
                    spell,
                    "scrollcaster_gauntlet_offhand_stone_sword_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.PASTEL_STAFF.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_staff_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_casting_item_test"
            );

            var emptyGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var emptyPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_offhand_empty_selection_test");
            emptyPlayer.setItemInHand(InteractionHand.OFF_HAND, emptyGauntlet);
            var emptyResult = emptyGauntlet.getItem().use(helper.getLevel(), emptyPlayer, InteractionHand.OFF_HAND);
            helper.assertTrue(emptyResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Scrollcaster Gauntlet offhand use without a selected scroll should pass but got "
                            + emptyResult.getResult());
        });
    }

    static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(Items.SHIELD),
                    "scrollcaster_gauntlet_mainhand_offhand_shield_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_elemental_bow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    createIronAutoloaderCrossbowStack(helper),
                    "scrollcaster_gauntlet_mainhand_offhand_autoloader_crossbow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_spellgun_test"
            );
        });
    }

    private static void assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
            GameTestHelper helper,
            ItemStack offhandStack,
            String profileName
    ) {
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack.copy());
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Scrollcaster Gauntlet mainhand offhand priority test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Scrollcaster Gauntlet mainhand use should pass to supported offhand use item "
                        + offhandStack + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet mainhand use should not cast before supported offhand use item "
                        + offhandStack);
    }

    private static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != Items.AIR,
                "Missing irons_spellbooks:autoloader_crossbow for Scrollcaster Gauntlet offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }

    static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractSpellGunItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Spell Gun");

            for (var stack : stacks) {
                var expectedEnchantments = expectedSpellGunEnchantments(stack);
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedBookEnchantments,
                        expectedEnchantments,
                        "Spell Gun " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                );
            }
        });
    }
    static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON),
                    "Reflectcast Shield is missing malum:soul_shatter_capable_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedReflectcastShieldEnchantments(helper.getLevel().registryAccess(), stack),
                    "Reflectcast Shield"
            );
        });
    }
    static void spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            helper.assertTrue(stack.getMaxDamage() == SpellchargedGreatsword.DURABILITY,
                    "Spellcharged Greatsword durability should be " + SpellchargedGreatsword.DURABILITY
                            + " but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == SpellchargedGreatsword.ENCHANTMENT_VALUE,
                    "Spellcharged Greatsword enchantability should be " + SpellchargedGreatsword.ENCHANTMENT_VALUE
                            + " but got " + item.getEnchantmentValue(stack));

            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D,
                    "Spellcharged Greatsword attack damage modifier should display as 8 damage"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D,
                    "Spellcharged Greatsword attack speed modifier should display as 1.1 speed"
            );
            assertSpellchargedGreatswordEntityReach(
                    helper,
                    item,
                    stack,
                    ApprenticeCodexServerConfig.spellchargedGreatswordConfig().normalEntityReachBonus(),
                    "Spellcharged Greatsword entity reach modifier should match the normal server config"
            );
            assertUpgradeable(helper, stack, "Spellcharged Greatsword should remain upgradeable");
            assertMainhandUpgradeBridge(helper, stack, "Spellcharged Greatsword should keep mainhand upgrade modifiers");

            helper.assertTrue(stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON),
                    "Spellcharged Greatsword is missing malum:soul_shatter_capable_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellchargedGreatswordEnchantments(helper.getLevel().registryAccess(), stack),
                    "Spellcharged Greatsword"
            );
        });
    }
    static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            var gameTime = helper.getLevel().getGameTime();

            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(20, 20) == 20.0D,
                    "Spellcharged Greatsword should halve charge gain at 40 ticks or less");
            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(80, 200) == 200.0D,
                    "Spellcharged Greatsword charge gain should be capped to 200 ticks");

            SpellchargedGreatsword.addCharge(stack, gameTime, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime, 200.0D, 1,
                    "Spellcharged Greatsword should reach level 1 at 200 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 2.0D, -0.0D,
                    "Spellcharged Greatsword level 1 attributes");

            SpellchargedGreatsword.addCharge(stack, gameTime, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime, 400.0D, 2,
                    "Spellcharged Greatsword should reach level 2 at 400 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 4.0D, -0.2D,
                    "Spellcharged Greatsword level 2 attributes");

            SpellchargedGreatsword.addCharge(stack, gameTime, 400.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime + 100L, 800.0D, 3,
                    "Spellcharged Greatsword should keep full charge during the 5 second decay delay");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 8.0D, -0.5D,
                    "Spellcharged Greatsword level 3 attributes");

            helper.assertTrue(
                    Math.abs(SpellchargedGreatsword.getEffectiveChargeTicks(stack, gameTime + 101L) - 796.0D) < 1.0e-9D,
                    "Spellcharged Greatsword should decay 4 charge ticks per tick after the delay");
            helper.assertFalse(SpellchargedGreatsword.refreshDecay(stack, gameTime + 299L),
                    "Spellcharged Greatsword should not reset before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack, gameTime + 299L) == 3,
                    "Spellcharged Greatsword decay should not lower level before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.refreshDecay(stack, gameTime + 300L),
                    "Spellcharged Greatsword should reset when decay reaches zero");
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime + 300L, 0.0D, 0,
                    "Spellcharged Greatsword should clear charge and level after full decay");

            var staleLevelStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(staleLevelStack, gameTime, 800.0D);
            SpellchargedGreatsword.addCharge(staleLevelStack, gameTime + 300L, 20.0D);
            assertSpellchargedGreatswordChargeState(helper, staleLevelStack, gameTime + 300L, 20.0D, 0,
                    "Spellcharged Greatsword should not restore a stale level after charge fully decays");

            var inventoryTickPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_inventory_decay_test");
            var inventoryTickStack = new ItemStack(item);
            var fullyDecayedChargeTime = helper.getLevel().getGameTime()
                    - SpellchargedGreatsword.DECAY_DELAY_TICKS
                    - (long) (SpellchargedGreatsword.MAX_CHARGE_TICKS / SpellchargedGreatsword.DECAY_TICKS_PER_TICK);
            SpellchargedGreatsword.addCharge(inventoryTickStack, fullyDecayedChargeTime, 800.0D);
            inventoryTickPlayer.getInventory().setItem(9, inventoryTickStack);
            item.inventoryTick(inventoryTickStack, helper.getLevel(), inventoryTickPlayer, 9, false);
            assertSpellchargedGreatswordChargeState(helper, inventoryTickStack, helper.getLevel().getGameTime(), 0.0D, 0,
                    "Spellcharged Greatsword should clear stale charge while ticking outside the main hand");

            var staleAttributeStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(staleAttributeStack, fullyDecayedChargeTime, 800.0D);
            assertSpellchargedGreatswordAttackAttributes(helper, item, staleAttributeStack, 0.0D, 0.0D,
                    "Spellcharged Greatsword stale charge should not apply attack attributes before inventory sync");
        });
    }

    static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var level = helper.getLevel();

            var levelOneStack = new ItemStack(item);
            var levelOnePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_one_overcharge_test");
            SpellchargedGreatsword.addCharge(levelOneStack, level.getGameTime(), 200.0D);
            item.releaseUsing(
                    levelOneStack,
                    level,
                    levelOnePlayer,
                    item.getUseDuration(levelOneStack, levelOnePlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(levelOneStack),
                    "Spellcharged Greatsword level 1 should not enter overcharge mode");

            var levelTwoStack = new ItemStack(item);
            var levelTwoPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_two_overcharge_test");
            SpellchargedGreatsword.addCharge(levelTwoStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    levelTwoStack,
                    level,
                    levelTwoPlayer,
                    item.getUseDuration(levelTwoStack, levelTwoPlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(levelTwoStack),
                    "Spellcharged Greatsword level 2 should enter overcharge mode");
            assertSpellchargedGreatswordChargeState(helper, levelTwoStack, level.getGameTime(), 0.0D, 0,
                    "Spellcharged Greatsword overcharge should consume stored charge");
            assertSpellchargedGreatswordAttackAttributes(helper, item, levelTwoStack, 0.0D, 0.2D,
                    "Spellcharged Greatsword overcharge configured attributes");
            assertSpellchargedGreatswordEntityReach(
                    helper,
                    item,
                    levelTwoStack,
                    ApprenticeCodexServerConfig.spellchargedGreatswordConfig().overchargeEntityReachBonus(),
                    "Spellcharged Greatsword overcharge entity reach should match the server config"
            );

            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(
                            levelTwoStack,
                            level.getGameTime() + SpellchargedGreatsword.OVERCHARGE_LEVEL_2_DURATION_TICKS
                    ),
                    "Spellcharged Greatsword level 2 overcharge should end after 10 seconds");
            var expiredOverchargeStack = levelTwoStack.copy();
            CustomData.update(DataComponents.CUSTOM_DATA, expiredOverchargeStack, tag ->
                    tag.putLong("SpellchargedGreatswordOverchargeEndGameTime", level.getGameTime() - 1L));
            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(expiredOverchargeStack),
                    "Spellcharged Greatsword should not treat expired overcharge NBT as active");
            item.inventoryTick(expiredOverchargeStack, level, levelTwoPlayer, 0, false);
            assertSpellchargedGreatswordAttackAttributes(helper, item, expiredOverchargeStack, 0.0D, 0.0D,
                    "Spellcharged Greatsword expired overcharge should not use overcharge attributes");

            var levelThreeStack = new ItemStack(item);
            var levelThreePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_three_overcharge_test");
            SpellchargedGreatsword.addCharge(levelThreeStack, level.getGameTime(), 800.0D);
            item.releaseUsing(
                    levelThreeStack,
                    level,
                    levelThreePlayer,
                    item.getUseDuration(levelThreeStack, levelThreePlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(
                            levelThreeStack,
                            level.getGameTime() + SpellchargedGreatsword.OVERCHARGE_LEVEL_2_DURATION_TICKS
                    ),
                    "Spellcharged Greatsword level 3 overcharge should last longer than level 2");
        });
    }

    static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_overcharge_release_test");

        var staleLevelStack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, staleLevelStack);
        SpellchargedGreatsword.addCharge(staleLevelStack,
                level.getGameTime() - SpellchargedGreatsword.DECAY_DELAY_TICKS - 100L, 400.0D);
        var staleResult = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(staleResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Spellcharged Greatsword should not start overcharge activation after charge fully decays");

        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        SpellchargedGreatsword.addCharge(stack, level.getGameTime() - SpellchargedGreatsword.DECAY_DELAY_TICKS - 20L,
                500.0D);
        var decayedCharge = SpellchargedGreatsword.getEffectiveChargeTicks(stack, level.getGameTime());

        var result = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                "Spellcharged Greatsword overcharge activation use should consume interaction");
        item.onUseTick(
                level,
                player,
                stack,
                item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
        );
        helper.assertTrue(Math.abs(
                SpellchargedGreatsword.getEffectiveChargeTicks(stack,
                        level.getGameTime() + SpellchargedGreatsword.DECAY_DELAY_TICKS) - decayedCharge) < 1.0e-9D,
                "Spellcharged Greatsword overcharge activation hold should pause normal charge decay"
        );
        helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(stack),
                "Spellcharged Greatsword should not enter overcharge before release");

        item.releaseUsing(
                stack,
                level,
                player,
                item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
        );
        helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(stack),
                "Spellcharged Greatsword should enter overcharge when activation hold is released");

        var overchargedUse = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(overchargedUse.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Spellcharged Greatsword overcharge mode should not provide a right-click function");
        helper.succeed();
    }

    static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_sweeping_bonus_test");
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));

            var stack = new ItemStack(item);
            var sweepingEdge = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, stack) == 1,
                    "Spellcharged Greatsword should act as Sweeping Edge I without the enchantment");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, stack, 0.5D,
                    "Spellcharged Greatsword should apply Sweeping Edge I ratio without the enchantment");
            assertAabbClose(helper, item.getSweepHitBox(stack, player, target),
                    target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D),
                    "Spellcharged Greatsword normal sweep hitbox");

            var enchantedStack = new ItemStack(item);
            var enchantedStackEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantedStackEnchantments.set(sweepingEdge, 3);
            EnchantmentHelper.setEnchantments(enchantedStack, enchantedStackEnchantments.toImmutable());
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, enchantedStack) == 4,
                    "Spellcharged Greatsword should add one virtual Sweeping Edge level normally");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, enchantedStack, 0.8D,
                    "Spellcharged Greatsword should apply actual plus virtual Sweeping Edge ratio normally");

            var overchargedStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(overchargedStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    overchargedStack,
                    level,
                    player,
                    item.getUseDuration(overchargedStack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(overchargedStack),
                    "Spellcharged Greatsword test stack should enter overcharge mode");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 3,
                    "Overcharged Spellcharged Greatsword should act as Sweeping Edge III without the enchantment");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, overchargedStack, 0.75D,
                    "Overcharged Spellcharged Greatsword should apply Sweeping Edge III ratio without the enchantment");
            assertAabbClose(helper, item.getSweepHitBox(overchargedStack, player, target),
                    target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D),
                    "Spellcharged Greatsword overcharge sweep hitbox");
            CustomData.update(DataComponents.CUSTOM_DATA, overchargedStack, tag ->
                    tag.putLong("SpellchargedGreatswordOverchargeEndGameTime", level.getGameTime() - 1L));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 1,
                    "Expired overcharge Spellcharged Greatsword should use the normal Sweeping Edge bonus");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, overchargedStack, 0.5D,
                    "Expired overcharge Spellcharged Greatsword should apply normal Sweeping Edge ratio");
            assertAabbClose(helper, item.getSweepHitBox(overchargedStack, player, target),
                    target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D),
                    "Expired overcharge Spellcharged Greatsword should use the normal sweep hitbox");

            var enchantedOverchargedStack = new ItemStack(item);
            var enchantedOverchargedStackEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantedOverchargedStackEnchantments.set(sweepingEdge, 3);
            EnchantmentHelper.setEnchantments(enchantedOverchargedStack, enchantedOverchargedStackEnchantments.toImmutable());
            SpellchargedGreatsword.addCharge(enchantedOverchargedStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    enchantedOverchargedStack,
                    level,
                    player,
                    item.getUseDuration(enchantedOverchargedStack, player)
                            - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(
                    EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, enchantedOverchargedStack) == 6,
                    "Overcharged Spellcharged Greatsword should add three virtual Sweeping Edge levels"
            );
            assertSpellchargedGreatswordSweepingDamageRatio(helper, enchantedOverchargedStack, 6.0D / 7.0D,
                    "Overcharged Spellcharged Greatsword should apply actual plus virtual Sweeping Edge ratio");
        });
    }

    static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var config = new SpellchargedGreatswordServerConfig.Values(
                    11.0D,
                    -0.11D,
                    12.0D,
                    -0.22D,
                    13.0D,
                    -0.33D,
                    21.0D,
                    0.44D,
                    0.75D,
                    1.5D,
                    2,
                    5
            );

            try (var ignored = ApprenticeCodexServerConfig.useSpellchargedGreatswordConfigOverrideForGameTest(config)) {
                var sweepingEdge = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE);
                var levelOneStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelOneStack, 0L, 200.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelOneStack, 11.0D, -0.11D,
                        "Spellcharged Greatsword level 1 configured attributes");
                assertSpellchargedGreatswordEntityReach(helper, item, levelOneStack, 0.75D,
                        "Spellcharged Greatsword normal configured entity reach");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, levelOneStack) == 2,
                        "Spellcharged Greatsword normal Sweeping Edge bonus should use server config");

                var levelTwoStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelTwoStack, 0L, 400.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelTwoStack, 12.0D, -0.22D,
                        "Spellcharged Greatsword level 2 configured attributes");

                var levelThreeStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelThreeStack, 0L, 800.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelThreeStack, 13.0D, -0.33D,
                        "Spellcharged Greatsword level 3 configured attributes");

                var overchargedStack = new ItemStack(item);
                var overchargedPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_config_overcharge_test");
                SpellchargedGreatsword.addCharge(overchargedStack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        overchargedStack,
                        helper.getLevel(),
                        overchargedPlayer,
                        item.getUseDuration(overchargedStack, overchargedPlayer)
                                - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );
                assertSpellchargedGreatswordAttackAttributes(helper, item, overchargedStack, 21.0D, 0.44D,
                        "Spellcharged Greatsword overcharge configured attributes");
                assertSpellchargedGreatswordEntityReach(helper, item, overchargedStack, 1.5D,
                        "Spellcharged Greatsword overcharge configured entity reach");
                helper.assertTrue(
                        EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 5,
                        "Spellcharged Greatsword overcharge Sweeping Edge bonus should use server config"
                );
            }
        });
    }

    static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            try {
                var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_bettercombat_attributes_test");
                var normalStack = new ItemStack(item);
                var overchargedStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(overchargedStack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        overchargedStack,
                        helper.getLevel(),
                        player,
                        item.getUseDuration(overchargedStack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );

                var weaponRegistry = Class.forName("net.bettercombat.logic.WeaponRegistry");
                var getAttributes = weaponRegistry.getMethod("getAttributes", ItemStack.class);
                var normalAttributes = getAttributes.invoke(null, normalStack);
                var chargedAttributes = getAttributes.invoke(null, overchargedStack);

                helper.assertTrue(readBetterCombatAttackRange(normalAttributes) == 3.0D,
                        "Spellcharged Greatsword normal Better Combat attack range should come from the base file");
                helper.assertTrue(readBetterCombatAttackRange(chargedAttributes) == 3.5D,
                        "Spellcharged Greatsword overcharge Better Combat attack range should come from the charged file");
                helper.assertTrue(readBetterCombatAttackCount(chargedAttributes) == 2,
                        "Spellcharged Greatsword overcharge Better Combat attacks should come from the charged file");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to inspect Better Combat weapon attributes", exception);
            }
        });
    }

    static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            try {
                var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_epicfight_innate_test");
                var stack = new ItemStack(item);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);

                helper.assertTrue(hasExpectedSpellchargedGreatswordEpicFightSkills(player, stack),
                        "Spellcharged Greatsword Epic Fight skills should be Sweeping Edge and Swordmaster");
                assertSpellchargedGreatswordEpicFightOverchargedCollider(
                        helper,
                        item,
                        "Spellcharged Greatsword overcharge Epic Fight collider"
                );
                helper.assertTrue(setEpicFightSweepingEdgeCharge(player, 0),
                        "Spellcharged Greatsword Epic Fight test could not resolve weapon innate skill container");

                SpellchargedGreatsword.addCharge(stack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        stack,
                        helper.getLevel(),
                        player,
                        item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );
                helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(stack),
                        "Spellcharged Greatsword Epic Fight test stack should enter overcharge mode");

                invokeSpellchargedGreatswordEpicFightTick(player);

                var expectedStack = getEpicFightSweepingEdgeMaxStack();
                helper.assertTrue(getEpicFightSweepingEdgeCharge(player) == expectedStack,
                        "Spellcharged Greatsword overcharge should refill Epic Fight Sweeping Edge charge to "
                                + expectedStack);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to inspect Spellcharged Greatsword Epic Fight capability",
                        exception);
            }
        });
    }

    static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.ARCANE_BLAST.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_mainhand_charge_test");
            var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);

            postSpellOnCast(player, spell, 1);
            var chargedMainhand = player.getMainHandItem();
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            chargedMainhand,
                            helper.getLevel().getGameTime()) > 0.0D,
                    "Spellcharged Greatsword should charge from spell casts while held in mainhand");

            var offhandOnly = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandOnly);
            postSpellOnCast(player, spell, 1);
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            player.getOffhandItem(),
                            helper.getLevel().getGameTime()) == 0.0D,
                    "Spellcharged Greatsword should not charge from offhand-only casts");
        });
    }

    static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        var spell = SpellRegistry.FORCE_FIELD.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_continuous_refresh_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellcharged Greatsword continuous test could not resolve magic data");
        magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));

        var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
        SpellchargedGreatsword.addCharge(greatsword, helper.getLevel().getGameTime(), 400.0D);
        player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);
        magicData.initiateCast(spell, 1, 200, CastSource.SPELLBOOK, "gametest");
        postSpellOnCast(player, spell, 1);

        var firstChargeTime = helper.getLevel().getGameTime();
        var chargeAfterFirstCast = SpellchargedGreatsword.getEffectiveChargeTicks(
                player.getMainHandItem(),
                firstChargeTime
        );
        helper.assertTrue(chargeAfterFirstCast >= 400.0D,
                "Spellcharged Greatsword should keep prepared charge after the first CONTINUOUS cast event");
        var levelAfterFirstCast = SpellchargedGreatsword.getChargeLevel(player.getMainHandItem(), firstChargeTime);

        helper.runAtTickTime(SpellchargedGreatsword.DECAY_DELAY_TICKS + 10, () -> {
            var beforeRefreshTime = helper.getLevel().getGameTime();
            var chargeBeforeRefresh = SpellchargedGreatsword.getEffectiveChargeTicks(
                    player.getMainHandItem(),
                    beforeRefreshTime
            );
            helper.assertTrue(chargeBeforeRefresh < chargeAfterFirstCast,
                    "Spellcharged Greatsword test should reach the old decay window before the duplicate event");

            postSpellOnCast(player, spell, 1);
            assertSpellchargedGreatswordChargeState(
                    helper,
                    player.getMainHandItem(),
                    beforeRefreshTime,
                    chargeBeforeRefresh,
                    levelAfterFirstCast,
                    "Spellcharged Greatsword duplicate CONTINUOUS event should snapshot decayed charge"
            );
            helper.succeed();
        });
    }

    static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_recast_refresh_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellcharged Greatsword recast test could not resolve magic data");

        var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
        SpellchargedGreatsword.addCharge(greatsword, helper.getLevel().getGameTime(), 400.0D);
        player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                spell.getSpellId(),
                1,
                2,
                1000,
                CastSource.SPELLBOOK,
                null
        ), magicData);

        helper.runAtTickTime(SpellchargedGreatsword.DECAY_DELAY_TICKS + 10, () -> {
            var beforeRefreshTime = helper.getLevel().getGameTime();
            var chargeBeforeRefresh = SpellchargedGreatsword.getEffectiveChargeTicks(
                    player.getMainHandItem(),
                    beforeRefreshTime
            );
            helper.assertTrue(chargeBeforeRefresh < 400.0D,
                    "Spellcharged Greatsword test should reach the old decay window before the Recast event");

            postSpellOnCast(player, spell, 1);
            assertSpellchargedGreatswordChargeState(
                    helper,
                    player.getMainHandItem(),
                    beforeRefreshTime,
                    chargeBeforeRefresh,
                    2,
                    "Spellcharged Greatsword Recast event should snapshot decayed charge"
            );
            helper.succeed();
        });
    }

    static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spellcasters Flask",
                item -> item instanceof SpellcastersFlask,
                expectedFlaskEnchantments()
        ));
    }

    private static void assertSpellchargedGreatswordChargeState(
            GameTestHelper helper,
            ItemStack stack,
            long gameTime,
            double expectedChargeTicks,
            int expectedChargeLevel,
            String message
    ) {
        var actualChargeTicks = SpellchargedGreatsword.getEffectiveChargeTicks(stack, gameTime);
        helper.assertTrue(Math.abs(actualChargeTicks - expectedChargeTicks) < 1.0e-9D,
                message + ": expected charge " + expectedChargeTicks + " but got " + actualChargeTicks);
        helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack, gameTime) == expectedChargeLevel,
                message + ": expected level " + expectedChargeLevel + " but got "
                        + SpellchargedGreatsword.getChargeLevel(stack, gameTime));
    }

    private static void assertSpellchargedGreatswordAttackAttributes(
            GameTestHelper helper,
            SpellchargedGreatsword item,
            ItemStack stack,
            double expectedDamageBonus,
            double expectedSpeedBonus,
            String message
    ) {
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                item.getDefaultAttributeModifiers(stack),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " item defaults"
        );
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " component"
        );
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                stack.getAttributeModifiers(),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " stack tooltip"
        );
    }

    private static void assertSpellchargedGreatswordAttackAttributeModifiers(
            GameTestHelper helper,
            ItemAttributeModifiers attributeModifiers,
            double expectedDamageBonus,
            double expectedSpeedBonus,
            String message
    ) {
        var modifiers = toModifierMultimap(attributeModifiers);
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_DAMAGE),
                VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D + expectedDamageBonus,
                message + " attack damage"
        );
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_SPEED),
                VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D + expectedSpeedBonus,
                message + " attack speed"
        );
    }

    private static void assertSpellchargedGreatswordEntityReach(
            GameTestHelper helper,
            SpellchargedGreatsword item,
            ItemStack stack,
            double expectedReachBonus,
            String message
    ) {
        var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
        assertSingleModifierAmount(
                helper,
                modifiers.get(Attributes.ENTITY_INTERACTION_RANGE),
                AttributeModifier.Operation.ADD_VALUE,
                expectedReachBonus,
                message
        );
    }

    private static void assertSpellchargedGreatswordSweepingDamageRatio(
            GameTestHelper helper,
            ItemStack stack,
            double expectedRatio,
            String message
    ) {
        var actualSlotRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlot.MAINHAND,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualSlotRatio - expectedRatio) < 1.0e-9D,
                message + " for main hand slot: expected " + expectedRatio + " but got " + actualSlotRatio);

        var actualMainHandGroupRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlotGroup.MAINHAND,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualMainHandGroupRatio - expectedRatio) < 1.0e-9D,
                message + " for main hand tooltip group: expected " + expectedRatio + " but got "
                        + actualMainHandGroupRatio);

        var actualAnyGroupRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlotGroup.ANY,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualAnyGroupRatio) < 1.0e-9D,
                message + " should not appear in the generic equipped tooltip group: got " + actualAnyGroupRatio);
    }

    private static double sumEffectiveModifierAmount(
            ItemStack stack,
            EquipmentSlot slot,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        var total = new double[1];
        stack.forEachModifier(slot, (actualAttribute, modifier) -> {
            if (actualAttribute.equals(attribute) && modifier.operation() == operation) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static double sumEffectiveModifierAmount(
            ItemStack stack,
            EquipmentSlotGroup slotGroup,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        var total = new double[1];
        stack.forEachModifier(slotGroup, (actualAttribute, modifier) -> {
            if (actualAttribute.equals(attribute) && modifier.operation() == operation) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static void assertAabbClose(GameTestHelper helper, AABB actual, AABB expected, String message) {
        helper.assertTrue(Math.abs(actual.minX - expected.minX) < 1.0e-9D
                        && Math.abs(actual.minY - expected.minY) < 1.0e-9D
                        && Math.abs(actual.minZ - expected.minZ) < 1.0e-9D
                        && Math.abs(actual.maxX - expected.maxX) < 1.0e-9D
                        && Math.abs(actual.maxY - expected.maxY) < 1.0e-9D
                        && Math.abs(actual.maxZ - expected.maxZ) < 1.0e-9D,
                message + ": expected " + expected + " but got " + actual);
    }

    private static double readBetterCombatAttackRange(Object attributes) throws ReflectiveOperationException {
        return ((Number) attributes.getClass().getMethod("attackRange").invoke(attributes)).doubleValue();
    }

    private static int readBetterCombatAttackCount(Object attributes) throws ReflectiveOperationException {
        return ((Object[]) attributes.getClass().getMethod("attacks").invoke(attributes)).length;
    }

    private static boolean hasExpectedSpellchargedGreatswordEpicFightSkills(Object player, ItemStack stack)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return (boolean) compatClass.getMethod(
                "hasExpectedSpellchargedGreatswordSkills",
                net.minecraft.server.level.ServerPlayer.class,
                ItemStack.class
        ).invoke(null, player, stack);
    }

    private static boolean setEpicFightSweepingEdgeCharge(Object player, int stack)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return (boolean) compatClass.getMethod(
                "setSweepingEdgeCharge",
                net.minecraft.server.level.ServerPlayer.class,
                int.class
        ).invoke(null, player, stack);
    }

    private static int getEpicFightSweepingEdgeCharge(Object player) throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return ((Number) compatClass.getMethod(
                "getSweepingEdgeCharge",
                net.minecraft.server.level.ServerPlayer.class
        ).invoke(null, player)).intValue();
    }

    private static int getEpicFightSweepingEdgeMaxStack() throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return ((Number) compatClass.getMethod("getSweepingEdgeMaxStack").invoke(null)).intValue();
    }

    private static void assertSpellchargedGreatswordEpicFightOverchargedCollider(
            GameTestHelper helper,
            Item item,
            String message
    ) throws ReflectiveOperationException {
        var collider = getSpellchargedGreatswordOverchargedEpicFightCollider(item);
        helper.assertTrue(collider != null, message + " should be created from Epic Fight collider API");

        var serialized = serializeEpicFightCollider(collider);
        helper.assertTrue(serialized.getInt("number") == 3,
                message + " should keep the Epic Fight greatsword multi collider count");

        var size = serialized.getList("size", 6);
        assertDouble(helper, size.getDouble(0), 0.5D, message + " size x");
        assertDouble(helper, size.getDouble(1), 0.8D, message + " size y");
        assertDouble(helper, size.getDouble(2), 1.5D, message + " size z");

        var center = serialized.getList("center", 6);
        assertDouble(helper, center.getDouble(0), 0.0D, message + " center x");
        assertDouble(helper, center.getDouble(1), 0.0D, message + " center y");
        assertDouble(helper, center.getDouble(2), -1.5D, message + " center z");
    }

    private static Object getSpellchargedGreatswordOverchargedEpicFightCollider(Item item)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return compatClass.getDeclaredMethod("getOverchargedWeaponCollider", Item.class).invoke(null, item);
    }

    private static CompoundTag serializeEpicFightCollider(Object collider) throws ReflectiveOperationException {
        var multiColliderClass = collider.getClass().getSuperclass();
        var colliderClass = multiColliderClass.getSuperclass();
        var number = ((Number) readField(multiColliderClass, collider, "numberOfColliders")).intValue();
        var center = (Vec3) readField(colliderClass, collider, "modelCenter");
        var colliders = (List<?>) readField(multiColliderClass, collider, "colliders");
        var firstObbCollider = colliders.getFirst();
        var modelVertices = (Object[]) readField(firstObbCollider.getClass(), firstObbCollider, "modelVertices");
        var size = (Vec3) modelVertices[1];

        var result = new CompoundTag();
        result.putInt("number", number);
        result.put("center", doubleList(center.x, center.y, center.z));
        result.put("size", doubleList(size.x, size.y, size.z));
        return result;
    }

    private static Object readField(Class<?> owner, Object target, String name) throws ReflectiveOperationException {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static ListTag doubleList(double... values) {
        var list = new ListTag();
        for (double value : values) {
            list.add(DoubleTag.valueOf(value));
        }
        return list;
    }

    private static void assertDouble(GameTestHelper helper, double actualValue, double expected, String message) {
        helper.assertTrue(Math.abs(actualValue - expected) < 1.0e-9D,
                message + ": expected " + expected + " but got " + actualValue);
    }

    private static void invokeSpellchargedGreatswordEpicFightTick(Object player) throws ReflectiveOperationException {
        var eventClass = Class.forName(
                "jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatswordEpicFightEvents"
        );
        eventClass.getMethod("onPlayerTick", PlayerTickEvent.Post.class).invoke(
                null,
                new PlayerTickEvent.Post((net.minecraft.world.entity.player.Player) player)
        );
    }

    static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Alchemists Flask",
                item -> item instanceof AlchemistsFlask,
                expectedAlchemistsFlaskEnchantments()
        ));
    }
    static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertApprenticeEnchantmentFlags(helper, Enchantments.ALACRITY, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.REFLUX, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.RESERVOIR, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SURGE, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.ATTUNEMENT, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.TENSE, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.WISDOM, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.PLUNDER, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.TRANSCENDENCE, true, false, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.GUZZLE, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.LARGE_MUG, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.RED_ENERGY, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.GLOW_ENERGY, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SYNTHESIS, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SHELL, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SYNCHRONIZATION, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.NEUTRALIZATION, false, true, false, false);
        });
    }
    static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var function = EnchantRandomlyFunction.randomApplicableEnchantment(helper.getLevel().registryAccess()).build();
            var seenApprenticeEnchantments = new LinkedHashSet<ResourceLocation>();
            var excludedEnchantments = new LinkedHashSet<>(expectedFlaskEnchantments());
            excludedEnchantments.add(Enchantments.SYNTHESIS.location());

            for (long seed = 0L; seed < 4096L; ++seed) {
                var result = function.apply(new ItemStack(Items.BOOK), createEmptyLootContext(helper, seed));
                var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK),
                        "Random applicable enchantment loot should convert books into enchanted books");
                helper.assertTrue(enchantments.size() == 1,
                        "Random applicable enchantment loot should apply exactly one enchantment: " + enchantments);

                for (var enchantment : enchantments.keySet()) {
                    var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
                    if (enchantmentId == null || !ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
                        continue;
                    }

                    helper.assertFalse(excludedEnchantments.contains(enchantmentId),
                            "Random applicable enchantment loot included excluded enchantment: " + enchantmentId + " at seed " + seed);
                    seenApprenticeEnchantments.add(enchantmentId);
                }
            }

            var expectedEnchantments = expectedRandomBookLootEnchantments();
            helper.assertTrue(seenApprenticeEnchantments.containsAll(expectedEnchantments),
                    "Random applicable enchantment loot lost apprentice enchantments: "
                            + describeEnchantmentDifference(expectedEnchantments, seenApprenticeEnchantments));
        });
    }
    static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(
                    helper,
                    "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    stack -> expectedEnchantressRobeEnchantments(helper.getLevel().registryAccess(), stack)
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    stack -> expectedStealthRuneArmorEnchantments(helper.getLevel().registryAccess(), stack)
            );
            assertCategoryEnchantments(
                    helper,
                    "Chromatic Magia Dress",
                    item -> item instanceof ChromaticMagiaDressItem,
                    stack -> expectedChromaticMagiaDressEnchantments(helper.getLevel().registryAccess(), stack)
            );
            assertCategoryEnchantments(
                    helper,
                    "Element Maiden Robe",
                    item -> item instanceof ElementMaidenRobeItem,
                    stack -> expectedElementMaidenRobeEnchantments(helper.getLevel().registryAccess(), stack)
            );
            assertCategoryEnchantments(
                    helper,
                    "Magi Agent Suit",
                    item -> item instanceof MagiAgentSuitItem,
                    stack -> expectedMagiAgentSuitEnchantments(helper.getLevel().registryAccess(), stack)
            );
        });
    }

    static void scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var expectedTaggedEnchantments = new LinkedHashSet<>(Set.of(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "attunement"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "plunder"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "surge"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "transcendence"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom"),
                    ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
                    ResourceLocation.withDefaultNamespace("fire_aspect"),
                    ResourceLocation.withDefaultNamespace("knockback"),
                    ResourceLocation.withDefaultNamespace("looting"),
                    ResourceLocation.withDefaultNamespace("sharpness"),
                    ResourceLocation.withDefaultNamespace("smite"),
                    ResourceLocation.withDefaultNamespace("sweeping_edge")
            ));
            addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedTaggedEnchantments);
            addExpectedMalumSpiritPlunderIfPresent(stack, expectedTaggedEnchantments);
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedTaggedEnchantments,
                    expectedTaggedEnchantments,
                    expectedTaggedEnchantments,
                    Set.of(),
                    Set.of(),
                    "Scrollcaster Gauntlet"
            );

            ScrollcasterGauntlet.setCalibrationScroll(
                    stack,
                    0,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get())
            );
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ALACRITY), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.REFLUX), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.RESERVOIR), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.TENSE), 1);

            var modifiers = toModifierMultimap(stack.getItem().getDefaultAttributeModifiers(stack));
            var epicFightLoaded = ModList.get().isLoaded(EpicFightCompat.MOD_ID);
            var expectedAttackDamageBonus = epicFightLoaded ? 2.0D : 5.0D;
            var expectedAttackSpeedBonus = epicFightLoaded ? 0.0D : -2.2D;
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    expectedAttackDamageBonus,
                    "Scrollcaster Gauntlet attack damage modifier should match the loaded combat environment"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    expectedAttackSpeedBonus,
                    "Scrollcaster Gauntlet attack speed modifier should match the loaded combat environment"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.02D,
                    "Scrollcaster Gauntlet Alacrity modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Scrollcaster Gauntlet Reflux modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA),
                    AttributeModifier.Operation.ADD_VALUE,
                    20.0D,
                    "Scrollcaster Gauntlet Reservoir modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.07D,
                    "Scrollcaster Gauntlet base + Surge spell power modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Scrollcaster Gauntlet Tense modifier changed"
            );

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Scrollcaster Gauntlet test could not resolve the selected spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Scrollcaster Gauntlet test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.04D,
                    "Scrollcaster Gauntlet Attunement modifier changed"
            );
        });
    }

    static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var expectedSpellPower = ApprenticeCodexServerConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                    ArmorItem.Type.CHESTPLATE, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                    ArmorItem.Type.LEGGINGS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(
                        modifiers.get(maxManaAttribute),
                        AttributeModifier.Operation.ADD_VALUE
                );
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(
                        modifiers.get(spellPowerAttribute),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == (armorType == ArmorItem.Type.CHESTPLATE),
                        "Apprentice Mage Robe " + armorType + " imbue surface regression");
            }
        });
    }

    static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var lightningSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get().getSchoolType()
            );
            helper.assertTrue(lightningSpellPowerAttribute != null,
                    "Enchantress Robe test could not resolve lightning school spell power attribute");
            var lightningSpellPowerHolder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(lightningSpellPowerAttribute);
            var expectedSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(maxManaBonus - EnchantressRobeStats.MAX_MANA_BONUS_PER_PIECE) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Enchantress Robe " + armorType + " imbue surface regression: hasImbueSlot="
                                + item.hasImbueSlot() + " stack=" + stack);

                var lightningSpellPowerBonus = sumModifierAmount(
                        modifiers.get(lightningSpellPowerHolder),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(lightningSpellPowerBonus) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " should not gain school spell power before imbue: "
                                + describeModifiers(modifiers));
            }
        });
    }
    static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Enchantress Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Enchantress Robe chestplate test could not resolve school spell power attribute");

            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
            var globalSpellPowerBonus = sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            var expectedGlobalSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            helper.assertTrue(Math.abs(globalSpellPowerBonus - expectedGlobalSpellPower) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep configured spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }

    static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var expectedSpellPower = ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().value().defense().get(armorType).equals(ArmorMaterials.IRON.value().defense().get(armorType)),
                        "Chromatic Magia Dress " + armorType + " defense should match iron");
                helper.assertTrue(Math.abs(item.getMaterial().value().toughness() - 1.0F) < 1.0e-6F,
                        "Chromatic Magia Dress " + armorType + " toughness should be 1");
                helper.assertTrue(item.getEnchantmentValue(stack) == ChromaticMagiaDressStats.enchantmentValue(),
                        "Chromatic Magia Dress " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Chromatic Magia Dress " + armorType + " should repair with mithril scrap");

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(maxManaBonus - 125.0D) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Chromatic Magia Dress " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && (item.getDescriptionId() + ".desc").equals(contents.getKey())),
                        "Chromatic Magia Dress " + armorType + " should show its lang desc key");
            }
        });
    }
    static void elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.elementMaidenRobeSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                    ArmorItem.Type.CHESTPLATE, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                        "Element Maiden Robe " + armorType + " should be a unique item");
                helper.assertTrue(stack.getRarity() == Rarity.EPIC,
                        "Element Maiden Robe " + armorType + " rarity should be epic");
                helper.assertTrue(item.getMaterial().value().defense().get(armorType).equals(ArmorMaterials.LEATHER.value().defense().get(armorType)),
                        "Element Maiden Robe " + armorType + " defense should match leather");
                helper.assertTrue(Math.abs(item.getMaterial().value().toughness() - 4.0F) < 1.0e-6F,
                        "Element Maiden Robe " + armorType + " toughness should be 4");
                helper.assertTrue(item.getEnchantmentValue(stack) == ElementMaidenRobeStats.enchantmentValue(),
                        "Element Maiden Robe " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Element Maiden Robe " + armorType + " should repair with mithril scrap");

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(
                        modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(maxManaAttribute)),
                        AttributeModifier.Operation.ADD_VALUE
                );
                helper.assertTrue(Math.abs(maxManaBonus - ElementMaidenRobeStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(
                        modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(spellPowerAttribute)),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Element Maiden Robe " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.element_maiden_robe.desc".equals(contents.getKey())),
                        "Element Maiden Robe " + armorType + " should show its common lang desc key");
            }

            var chestplate = (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get();
            var chestStack = new ItemStack(chestplate);
            chestplate.initializeSpellContainer(chestStack);
            var initialContainer = ISpellContainer.get(chestStack);
            helper.assertTrue(initialContainer != null
                            && initialContainer.getSpellAtIndex(0).getSpell() == SpellRegistry.DIVINE_POSSESSION.get(),
                    "Element Maiden Robe chestplate should initialize Divine Possession as its imbue spell");
            var ballLightning = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
            ISpellContainer.createImbuedContainer(ballLightning, 1, chestStack);

            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            chestStack.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            chestStack.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);

            var imbuedSchool = MagicTools.getImbuedSpellSchool(chestStack);
            helper.assertTrue(imbuedSchool != null,
                    "Element Maiden Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Element Maiden Robe chestplate test could not resolve school spell power attribute");

            var enchantedModifiers = toModifierMultimap(chestplate.getDefaultAttributeModifiers(chestStack));
            var enchantedGlobalSpellPower = sumModifierAmount(
                    enchantedModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(spellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(enchantedGlobalSpellPower
                            - (expectedSpellPower + ElementMaidenRobeStats.SURGE_SPELL_POWER_PER_LEVEL)) < 1.0e-9D,
                    "Element Maiden Robe chestplate should add Surge spell power: " + describeModifiers(enchantedModifiers));

            var attunementSpellPower = sumModifierAmount(
                    enchantedModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(attunementSpellPower
                            - ElementMaidenRobeStats.ATTUNEMENT_SPELL_POWER_PER_LEVEL) < 1.0e-9D,
                    "Element Maiden Robe chestplate should add Attunement school spell power: "
                            + describeModifiers(enchantedModifiers));
        });
    }

    static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var fireSpellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER;
            var expectedSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSpellPowerBonus();
            var expectedSchoolSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSchoolSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(),
                    ArmorItem.Type.CHESTPLATE, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().value().defense().get(armorType) == expectedMagiAgentSuitDefense(armorType),
                        "Magi Agent Suit " + armorType + " defense changed");
                helper.assertTrue(item.getEnchantmentValue(stack) == MagiAgentSuitStats.enchantmentValue(),
                        "Magi Agent Suit " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                        ),
                        "Magi Agent Suit " + armorType + " should repair with magic cloth");
                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Magi Agent Suit " + armorType + " imbue surface regression");

                var hintLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), hintLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(hintLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.rune_hint".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its rune hint before calibration");

                MagiAgentSuitItem.setCalibrationAdjustment(
                        stack,
                        0,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
                );
                helper.assertTrue(MagiAgentSuitItem.getCalibrationAdjustment(stack, 0)
                                .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Magi Agent Suit " + armorType + " should store the calibration rune");

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(maxManaBonus - MagiAgentSuitStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                var schoolSpellPowerBonus = sumModifierAmount(modifiers.get(fireSpellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(schoolSpellPowerBonus - expectedSchoolSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " school rune spell power regression: " + describeModifiers(modifiers));

                var toughnessBonus = sumModifierAmount(modifiers.get(Attributes.ARMOR_TOUGHNESS), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(toughnessBonus - expectedMagiAgentSuitToughness(armorType)) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " toughness regression: " + describeModifiers(modifiers));

                var tunedLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tunedLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tunedLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.school_rune".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its tuned school tooltip");
            }
        });
    }

    private static int expectedMagiAgentSuitDefense(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET, BOOTS -> 3;
            case CHESTPLATE, LEGGINGS -> 6;
            case BODY -> throw new IllegalArgumentException("Magi Agent Suit does not use body armor type");
        };
    }

    private static double expectedMagiAgentSuitToughness(ArmorItem.Type armorType) {
        return armorType == ArmorItem.Type.LEGGINGS ? 2.0D : 1.0D;
    }

    static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                // GameTest では SpellConfig 経由の school 解決が Evocation に寄ることがあるため、
                // 分配ルール自体は SchoolRegistry から直接検証する.
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 4,
                        SchoolRegistry.ICE.get(), 3
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.14D,
                        "Element Maiden Robe should distribute empty slots to the strongest spellbook school");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.06D,
                        "Element Maiden Robe should keep lower spellbook school share");

                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_school_power_distribution_test");
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()));

                equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                        createElementMaidenRobeSchoolPowerSpellbook(helper,
                                io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));

                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElementMaidenRobeItem,
                        "Element Maiden Robe dynamic test player is not wearing the robe");
                var resolvedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(player, 0.20D);
                helper.assertTrue(!resolvedBonuses.isEmpty(),
                        "Element Maiden Robe dynamic test could not resolve spellbook schools from Curios slot");
                helper.assertTrue(Math.abs(ApprenticeCodexServerConfig.elementMaidenRobeSchoolSpellPowerBonus() - 0.20D) < 1.0e-9D,
                        "Element Maiden Robe dynamic test config override did not apply");
                var appliedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertElementMaidenDynamicSchoolPowerBonuses(helper, player, appliedBonuses,
                        "Element Maiden Robe should apply Curios spellbook-derived school spell power");
            }
        });
    }

    static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 3,
                        SchoolRegistry.ICE.get(), 3,
                        SchoolRegistry.NATURE.get(), 1
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER.get(),
                        0.02D,
                        "Element Maiden Robe should floor smaller spellbook school shares to 1% units");
            }
        });
    }

    static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "element_maiden_robe_school_power_ignore_hand_test");
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
            player.setItemInHand(InteractionHand.MAIN_HAND, createElementMaidenRobeSchoolPowerSpellbook(helper, fire));

            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe should ignore spell containers outside the Curios spellbook slot");
            }

            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    createElementMaidenRobeSchoolPowerSpellbook(helper, fire));
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.0D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe school spell power config 0 should disable the dynamic bonus");
            }
        });
    }

    static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
                var ice = SpellRegistry.FROST_RUNE.get();

                var archivistsPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_archivists_source_test");
                archivistsPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                ArchivistsGrimoire.setUpgradeCount(grimoireStack, 2);
                var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
                inventory.setStackInSlot(0, createSpellScroll(fire));
                inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT, createSpellScroll(ice));
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 0);
                equipCurio(archivistsPlayer, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoireStack);

                var firstArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!firstArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the selected Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, firstArchivistsBonuses,
                        "Element Maiden Robe should read the selected Archivists Grimoire page");
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
                var secondArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!secondArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the new Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, secondArchivistsBonuses,
                        "Element Maiden Robe should apply the new Archivists Grimoire page bonus");

                var enderPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                        "element_maiden_robe_ender_source_test");
                enderPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var enderData = Capabilities.getEnderGrimoireSpellbookOrNull(enderPlayer);
                helper.assertTrue(enderData != null, "Ender Grimoire school spell power test is missing player capability");
                var mutable = ISpellContainer.create(15, true, true).mutableCopy();
                helper.assertTrue(mutable.addSpellAtIndex(fire, 1, 0, false),
                        "Failed to prepare Ender Grimoire fire spell");
                enderData.setSpellContainer(mutable.toImmutable());
                equipCurio(enderPlayer, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                        new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()));

                var enderBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(enderPlayer);
                helper.assertTrue(!enderBonuses.isEmpty(),
                        "Element Maiden Robe should resolve Ender Grimoire spells from the player capability");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, enderPlayer, enderBonuses,
                        "Element Maiden Robe should read Ender Grimoire spells from the player capability");
            }
        });
    }

    static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var expectedSpellPower = ApprenticeCodexServerConfig.stealthRuneArmorSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                    ArmorItem.Type.CHESTPLATE, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                    ArmorItem.Type.LEGGINGS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                    ArmorItem.Type.BOOTS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Stealth Rune Armor " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && ("item." + ApprenticeCodex.MODID + ".stealth_rune_armor.desc").equals(contents.getKey())),
                        "Stealth Rune Armor " + armorType + " should show its lang desc key");
            }
        });
    }

    static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "chromatic_magia_dress_history_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Chromatic Magia Dress test could not resolve player mana data");

            var helmet = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
            var chestplate = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var leggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            var boots = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            player.setItemSlot(EquipmentSlot.LEGS, leggings);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            var schoolSpellPowerBonusPerHistory =
                    ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();

            var longSpell = SpellRegistry.COMPOUND_PHIAL.get();
            for (int i = 0; i < 21; ++i) {
                postSpellOnCast(player, longSpell, 1);
            }
            assertSchoolSpellPowerBonus(helper, helmet, EquipmentSlot.HEAD, longSpell,
                    20.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress helmet should keep only the latest 20 LONG histories");
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, longSpell, 0.0D,
                    "Chromatic Magia Dress chestplate should ignore non-recast LONG spells");

            var continuousSpell = SpellRegistry.FORCE_FIELD.get();
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record CONTINUOUS spells");

            var heldContinuousLeggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            player.setItemSlot(EquipmentSlot.LEGS, heldContinuousLeggings);
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record only once during one CONTINUOUS hold");

            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new PlayerTickEvent.Post(player));
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    2.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record a new CONTINUOUS hold after the previous cast ends");
            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new PlayerTickEvent.Post(player));

            var instantSpell = SpellRegistry.MANA_SLASH.get();
            postSpellOnCast(player, instantSpell, 1);
            assertSchoolSpellPowerBonus(helper, boots, EquipmentSlot.FEET, instantSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress boots should record INSTANT spells");

            var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should record initial recast-capable casts");

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    recastSpell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should ignore casts while the same spell is in Recast");
        });
    }
    private static Set<ResourceLocation> expectedSpellchargedGreatswordEnchantments(
            net.minecraft.core.RegistryAccess registryAccess,
            ItemStack stack
    ) {
        var expectedEnchantments = new LinkedHashSet<>(collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
        ));
        expectedEnchantments.add(Enchantments.WISDOM.location());
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }
}
