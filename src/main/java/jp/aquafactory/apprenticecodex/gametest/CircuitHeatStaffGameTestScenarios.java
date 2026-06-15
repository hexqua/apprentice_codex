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
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffCoolingHandler;
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

final class CircuitHeatStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private CircuitHeatStaffGameTestScenarios() {
    }

    static void circuitHeatStaffKeepsExpectedStatsAndEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var item = (CircuitHeatStaff) stack.getItem();
            var modifiers = item.getDefaultAttributeModifiers(stack);

            assertModifierAmount(
                    helper,
                    modifiers,
                    Attributes.ATTACK_DAMAGE.value(),
                    EquipmentSlotGroup.MAINHAND,
                    3.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Circuit Heat Staff attack damage modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    Attributes.ATTACK_SPEED.value(),
                    EquipmentSlotGroup.MAINHAND,
                    -3.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Circuit Heat Staff attack speed modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff spell power modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff fire spell power modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff lightning spell power modifier changed"
            );
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Circuit Heat Staff should not expose an imbue spell container");

            CircuitHeatStaff.startStaffOverheat(stack, helper.getLevel(), 20 * 45);
            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(stack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == 20 * 45,
                    "Circuit Heat Staff item overheat should keep the requested duration: "
                            + remainingOverheatTicks);

            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedCircuitHeatStaffEnchantments(helper.getLevel().registryAccess(), stack),
                    "Circuit Heat Staff"
            );
        });
    }

    static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var baseManaCost = 100;
            var step = 1;

            var referenceAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 10);
            helper.assertTrue(referenceAdditionalMana == 20,
                    "Circuit Heat Staff skipped 10 seconds should keep the old step-1 extra mana: "
                            + referenceAdditionalMana);

            var shortAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 5);
            helper.assertTrue(shortAdditionalMana == 10,
                    "Circuit Heat Staff skipped 5 seconds should halve the step-1 extra mana: "
                            + shortAdditionalMana);

            var longAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 40);
            helper.assertTrue(longAdditionalMana == 80,
                    "Circuit Heat Staff skipped 40 seconds should quadruple the step-1 extra mana: "
                            + longAdditionalMana);

            var noSkippedCooldownAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 0);
            helper.assertTrue(noSkippedCooldownAdditionalMana == 0,
                    "Circuit Heat Staff should not add mana when no cooldown is skipped: "
                            + noSkippedCooldownAdditionalMana);
        });
    }

    static void circuitHeatStaffAdditionalManaUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    100,
                    0.50D,
                    0.25D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var additionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                        .getAdditionalManaCost(100, 2, 50);
                helper.assertTrue(additionalMana == 100,
                        "Circuit Heat Staff extra mana should use server config multipliers: " + additionalMana);
            }
        });
    }

    static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_overheat_duration_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGIC_SPEAR.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff overheat duration test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.setPlayerCastingItem(staffStack);

            var castCooldownTicks = 20 * 120;
            var skippedCooldownTicks = 20 * 40;
            var expectedOverheatTicks = castCooldownTicks + skippedCooldownTicks;
            var plannedManaCost = Math.max(1, spell.getManaCost(1));
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost,
                    expectedOverheatTicks
            );

            magicData.setMana(plannedManaCost);
            var event = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(event);

            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == expectedOverheatTicks,
                    "Circuit Heat Staff item overheat should use cast cooldown plus skipped cooldown: "
                            + remainingOverheatTicks + " / expected " + expectedOverheatTicks);

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
        });
    }

    static void circuitHeatStaffOverheatDurationUsesServerMinTicks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.0D,
                    0.0D,
                    0,
                    List.of(),
                    0.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_overheat_min_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                var baseManaCost = context.spell().getManaCost(1);
                context.magicData().setMana(baseManaCost);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                        "Circuit Heat Staff min overheat config test should cast but got " + result.getResult());
                context.magicData().setPlayerCastingItem(context.staffStack());
                postCircuitHeatStaffSpellOnCastEvent(context, baseManaCost);

                var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(
                        context.staffStack(),
                        helper.getLevel()
                );
                helper.assertTrue(remainingOverheatTicks == 20 * 10,
                        "Circuit Heat Staff item overheat should use configured minimum: " + remainingOverheatTicks);
            }
        });
    }

    static void circuitHeatStaffOverheatDurationUsesServerCapTicks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.0D,
                    0.0D,
                    0,
                    List.of(),
                    100.0D,
                    0,
                    40,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_overheat_cap_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                var baseManaCost = context.spell().getManaCost(1);
                context.magicData().setMana(baseManaCost);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                        "Circuit Heat Staff cap overheat config test should cast but got " + result.getResult());
                context.magicData().setPlayerCastingItem(context.staffStack());
                postCircuitHeatStaffSpellOnCastEvent(context, baseManaCost);

                var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(
                        context.staffStack(),
                        helper.getLevel()
                );
                helper.assertTrue(remainingOverheatTicks == 40,
                        "Circuit Heat Staff item overheat should use configured cap: " + remainingOverheatTicks);
            }
        });
    }

    static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_base_mana_gate_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff mana gate test could not resolve player mana data");
            var baseManaCost = spell.getManaCost(1);
            magicData.setMana(baseManaCost - 1.0F);

            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Circuit Heat Staff mana gate test could not resolve the selected spell: " + selection);
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Circuit Heat Staff should fail forced casts when base mana is insufficient but got " + result.getResult());
            helper.assertTrue(Math.abs(magicData.getMana() - (baseManaCost - 1.0F)) < 1.0e-4F,
                    "Circuit Heat Staff base mana failure should not mutate mana: " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff should restore the original cooldown after base mana failure");
            helper.assertFalse(jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                            .getState(player, spell.getSpellId()).active(),
                    "Circuit Heat Staff should not store bypass overheat state after base mana failure");
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff item should not enter overheat cooldown after base mana failure");
        });
    }

    static void circuitHeatStaffCooldownLimitBlocksBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    1,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_cooldown_limit_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                context.magicData().setMana(context.spell().getManaCost(1) * 10.0F);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Circuit Heat Staff should fail cooldown bypass above server limit but got " + result.getResult());
                helper.assertTrue(context.magicData().getPlayerCooldowns().isOnCooldown(context.spell()),
                        "Circuit Heat Staff should keep cooldown when server limit blocks bypass");
                helper.assertFalse(CircuitHeatStaff.isStaffOverheated(context.staffStack(), helper.getLevel()),
                        "Circuit Heat Staff should not overheat when server limit blocks bypass");
            }
        });
    }

    static void circuitHeatStaffSpellDenylistBlocksBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_SLASH.get();
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(spell.getSpellId()),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_spell_denylist_config_test",
                        spell
                );
                context.magicData().setMana(spell.getManaCost(1) * 10.0F);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Circuit Heat Staff should fail cooldown bypass for denied spells but got " + result.getResult());
                helper.assertTrue(context.magicData().getPlayerCooldowns().isOnCooldown(spell),
                        "Circuit Heat Staff should keep cooldown when spell denylist blocks bypass");
                helper.assertFalse(CircuitHeatStaff.isStaffOverheated(context.staffStack(), helper.getLevel()),
                        "Circuit Heat Staff should not overheat when spell denylist blocks bypass");
            }
        });
    }

    static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_continuous_mana_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff continuous mana test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.getSyncedData();
            magicData.initiateCast(
                    spell,
                    1,
                    spell.getCastTime(1),
                    CastSource.SPELLBOOK,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            magicData.setPlayerCastingItem(staffStack);

            var baseManaCost = spell.getManaCost(1);
            var plannedManaCost = baseManaCost
                    + jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, 1, 20 * 10);
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost * 3.0F,
                    60,
                    true
            );

            magicData.setMana(plannedManaCost * 3.0F);
            var firstEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(firstEvent);
            helper.assertTrue(firstEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous first tick should use overheated mana cost: " + firstEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should not enter item overheat while overheated continuous mana can still be paid");

            magicData.setMana(plannedManaCost + 5.0F);
            var secondEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(secondEvent);
            helper.assertTrue(secondEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous later tick should keep overheated mana cost: " + secondEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should still avoid item overheat while continuous mana remains above the overheated cost");

            magicData.setMana(plannedManaCost);
            var depletionEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(depletionEvent);
            helper.assertTrue(depletionEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous depletion tick should keep overheated mana cost: " + depletionEvent.getManaCost());
            helper.assertTrue(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should enter item overheat when the overheated continuous cost depletes mana");
            helper.assertTrue(
                    CircuitHeatStaff.formatOverheatManaCostForDisplay(spell, plannedManaCost).equals(plannedManaCost * 2 + "/s"),
                    "Circuit Heat Staff continuous warning should display per-second mana"
            );

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_recast_neutral_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff recast test could not resolve player mana data");
            magicData.setMana(0.0F);

            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            var stateBefore = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateBefore.active() && stateBefore.chainDepth() == 2,
                    "Circuit Heat Staff recast setup should start from bypass chain depth 2 but got " + stateBefore);

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Circuit Heat Staff recast setup should create an active recast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff recast setup should not leave a normal cooldown");
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 100);
            var staffOverheatBefore = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(staffOverheatBefore > 0,
                    "Circuit Heat Staff recast setup should start from item overheat cooldown");

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                    "Circuit Heat Staff recast should start through the recast-neutral path during item overheat but got "
                            + result.getResult());
            var stateAfterUse = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterUse.active()
                            && stateAfterUse.chainDepth() == stateBefore.chainDepth()
                            && stateAfterUse.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast use should not mutate bypass state: " + stateAfterUse
                            + " / before " + stateBefore);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast use should not consume mana before cast resolution: " + magicData.getMana());

            spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast resolution should keep Iron's no-mana recast behavior: " + magicData.getMana());
            helper.assertTrue(CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel()) == staffOverheatBefore,
                    "Circuit Heat Staff recast should ignore existing item overheat without clearing or refreshing it");
            var stateAfterCast = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterCast.active()
                            && stateAfterCast.chainDepth() == stateBefore.chainDepth()
                            && stateAfterCast.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast resolution should not mutate bypass state: " + stateAfterCast
                            + " / before " + stateBefore);

            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);

            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
            }

            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime while dropped: " + itemEntity.getAge());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff water-source cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(waterPos).isAir(),
                    "Circuit Heat Staff water-source cooling should consume the source after three cycles");
        });
    }

    static void circuitHeatStaffDropCoolingDisabledByServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    false,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks > 20 * 55,
                        "Circuit Heat Staff cooling should not reduce while disabled by server config: " + remainingTicks);
                helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                        "Circuit Heat Staff cooling should not consume water while disabled by server config");
            }
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnItem(helper, waterPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks > 20 * 55,
                    "Circuit Heat Staff should not use flowing water for cooling: " + remainingTicks);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    false,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks <= 20 * 30,
                        "Circuit Heat Staff water-source cooling should still reduce when consumption is disabled: "
                                + remainingTicks);
                helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                        "Circuit Heat Staff water-source cooling should keep water when consumption is disabled");
            }
        });
    }

    static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 2,
                    "Circuit Heat Staff cauldron cooling should consume one water level after three cycles: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var cauldronPos = new BlockPos(0, 2, 0);
            helper.setBlock(
                    cauldronPos,
                    Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
            );

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    false
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var state = helper.getBlockState(cauldronPos);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks <= 20 * 30,
                        "Circuit Heat Staff cauldron cooling should still reduce when consumption is disabled: "
                                + remainingTicks);
                helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                        "Circuit Heat Staff cauldron cooling should keep water level when consumption is disabled: "
                                + state);
            }
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff powder snow cooling should not consume powder snow block");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.POWDER_SNOW_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    "Circuit Heat Staff powder snow cauldron cooling should not consume cauldron level: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER);

        var itemEntity = spawnItem(helper, waterPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                    "Circuit Heat Staff should not consume water when it is not overheated");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime in powder snow even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff should not change powder snow when it is not overheated");
            helper.succeed();
        });
    }

    private static void runDropCoolingProcesses(ItemEntity itemEntity, int processCount) {
        for (var i = 1; i <= processCount; ++i) {
            itemEntity.tickCount = i * ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingProcessIntervalTicks();
            CircuitHeatStaffCoolingHandler.onEntityItemUpdate(itemEntity.getItem(), itemEntity);
        }
    }
}
