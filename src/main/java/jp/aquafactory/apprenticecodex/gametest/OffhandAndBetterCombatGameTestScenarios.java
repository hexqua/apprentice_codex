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

final class OffhandAndBetterCombatGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private OffhandAndBetterCombatGameTestScenarios() {
    }
    static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Spell Amplifier did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Spell Amplifier spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Spell Amplifier has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.SHOCK.get(),
                    "Copper Spell Amplifier preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Spell Amplifier preset spell level mismatch: " + spellData.getLevel());

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Copper Spell Amplifier imbued school could not be resolved");

            // school ID の厳密一致ではなく、解決された spell power 属性に補正が積まれることを確認する.
            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Copper Spell Amplifier could not resolve spell power attribute for stacking: " + imbuedSchool.getId());

            assertModifierAmount(helper, item.getDefaultAttributeModifiers(stack), resolvedSpellPower, 0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Copper Spell Amplifier spell power bonus regression");

            var enchantmentRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentRegistry.getOrThrow(Enchantments.ATTUNEMENT), 1);
            assertModifierAmount(helper, item.getDefaultAttributeModifiers(stack), resolvedSpellPower, 0.14D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Copper Spell Amplifier + Attunement stacking regression");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable after save/load");
        });
    }

    static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.getMaxDamage() == ReflectcastShield.DURABILITY,
                    "Reflectcast Shield durability should be " + ReflectcastShield.DURABILITY + " but got " + stack.getMaxDamage());
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(2.9F, true) == 0,
                    "Reflectcast Shield should keep sub-threshold successful guard durability at zero");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(3.0F, true) == 1,
                    "Reflectcast Shield should clamp successful guard durability to one");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(12.75F, true) == 1,
                    "Reflectcast Shield should keep high-damage successful guard durability at one");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(3.0F, false) == 4,
                    "Reflectcast Shield should keep vanilla shield durability cost when the spell cannot trigger");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(12.75F, false) == 13,
                    "Reflectcast Shield should keep vanilla high-damage durability cost when the spell cannot trigger");

            helper.assertFalse(ReflectcastShield.isDurabilityConsumptionSuppressed(stack, 100L),
                    "Reflectcast Shield should not suppress durability before a cost is recorded");
            ReflectcastShield.rememberDurabilityConsumed(stack, 100L);
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(customData != null,
                    "Reflectcast Shield should record the durability suppression tick in custom data");
            var tag = customData.copyTag();
            helper.assertTrue(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 100L),
                    "Reflectcast Shield should suppress durability on the recorded tick");
            helper.assertTrue(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 110L),
                    "Reflectcast Shield should suppress durability through the ten tick window");
            helper.assertFalse(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 111L),
                    "Reflectcast Shield should allow durability after the ten tick window");
        });
    }
    static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var diamondItem = ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get();
            var diamondStack = new ItemStack(diamondItem);
            assertModifierAmount(
                    helper,
                    diamondItem.getDefaultAttributeModifiers(diamondStack),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.value(),
                    0.25D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Diamond Spell Amplifier casting move speed bonus regression"
            );

            var netheriteItem = ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get();
            var netheriteStack = new ItemStack(netheriteItem);
            assertModifierAmount(
                    helper,
                    netheriteItem.getDefaultAttributeModifiers(netheriteStack),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.value(),
                    0.50D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Netherite Spell Amplifier casting move speed bonus regression"
            );
        });
    }
    static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()),
                    "Ender Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get()),
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "Elemental Bow should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get()),
                    "AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.PHOTON_SIPHON.get()),
                    "Direct AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    "AbstractSpellGunItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    "Crystal Bladed Staff should remain upgradeable as a right-click magic weapon");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get()),
                    "Indirect AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()),
                    "New swing magic weapon descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    "Charged Twin Blade Staff should be upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    "Mana Force Blade should be upgradeable via explicit whitelist entry");

            var shieldStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertFalse(shieldStack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                    "Reflectcast Shield should not be in the upgrade whitelist");
            helper.assertFalse(Utils.canBeUpgraded(shieldStack),
                    "Reflectcast Shield should remain excluded from the upgrade system");
        });
    }
    static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.UniteLunaStaff) ItemRegistry.UNITE_LUNA_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Unite Luna Staff did not initialize a spell container");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Unite Luna Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Unite Luna Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA.get(),
                    "Unite Luna Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Unite Luna Staff preset spell level mismatch: " + spellData.getLevel());

            var modifiers = item.getDefaultAttributeModifiers(stack);
            assertModifierAmount(helper, modifiers, Attributes.ATTACK_DAMAGE.value(), EquipmentSlotGroup.MAINHAND, 12.0D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff attack damage regression");
            assertModifierAmount(helper, modifiers, Attributes.ATTACK_SPEED.value(), EquipmentSlotGroup.MAINHAND, -3.2D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff attack speed regression");
            assertModifierAmount(helper, modifiers, Attributes.ENTITY_INTERACTION_RANGE.value(), EquipmentSlotGroup.MAINHAND, 0.5D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff entity reach regression");
            assertModifierAmount(helper, modifiers, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Unite Luna Staff spell power regression");
            assertModifierAmount(helper, modifiers, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Unite Luna Staff holy spell power regression");
            assertModifierAmount(
                    helper,
                    stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                    Attributes.ATTACK_SPEED.value(),
                    EquipmentSlotGroup.MAINHAND,
                    -3.2D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Unite Luna Staff attack speed component regression"
            );
        });
    }
    static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var stack = new ItemStack(item);
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(stack, item.getDefaultAttributeModifiers(stack));
            NeoForge.EVENT_BUS.post(event);
            var upgradedModifiers = event.build();

            assertModifierAmount(
                    helper,
                    upgradedModifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.value(),
                    50.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Offhand upgrade bridge regression: expected +50 max mana from mainhand-stored upgrade but got "
                            + upgradeData
            );
        });
    }
    static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat regression test");

            var spellbreakerAttributes = net.bettercombat.logic.WeaponRegistry.getAttributes(new ItemStack(spellbreaker));
            helper.assertTrue(spellbreakerAttributes != null && spellbreakerAttributes.isTwoHanded(),
                    "Better Combat spellbreaker should resolve as a two-handed weapon but got " + spellbreakerAttributes);

            var amplifierStack = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var amplifierEvent = new ItemAttributeModifierEvent(
                    amplifierStack,
                    amplifierStack.getItem().getDefaultAttributeModifiers(amplifierStack)
            );
            NeoForge.EVENT_BUS.post(amplifierEvent);
            var amplifierModifiers = toModifierMultimap(amplifierEvent.build());

            var spellPowerBonus = sumModifierAmount(
                    amplifierModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(spellPowerBonus - 0.05D) < 1.0e-9D,
                    "Iron Spell Amplifier should expose +0.05 spell power in offhand modifiers but got "
                            + spellPowerBonus + " modifiers=" + describeModifiers(amplifierModifiers));
        });
    }

    static void betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var swordStack = new ItemStack(Items.DIAMOND_SWORD);
            var gauntletStack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(swordStack) != null,
                    "Better Combat diamond sword attributes should be present for offhand Gauntlet test");
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(gauntletStack) != null,
                    "Better Combat Scrollcaster Gauntlet attributes should be present for offhand Gauntlet test");

            var swordMainPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_offhand_gauntlet_no_dual_test");
            swordMainPlayer.setItemInHand(InteractionHand.MAIN_HAND, swordStack);
            swordMainPlayer.setItemInHand(InteractionHand.OFF_HAND, gauntletStack.copy());

            helper.assertFalse(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(swordMainPlayer),
                    "Offhand-only Scrollcaster Gauntlet should not make a normal mainhand weapon dual wield");
            var secondSwordAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(swordMainPlayer, 1);
            helper.assertTrue(secondSwordAttack != null && !secondSwordAttack.isOffHand(),
                    "Offhand-only Scrollcaster Gauntlet should keep Better Combat attacks on mainhand but got "
                            + secondSwordAttack);

            var dualGauntletPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_dual_gauntlet_stays_dual_test");
            dualGauntletPlayer.setItemInHand(InteractionHand.MAIN_HAND, gauntletStack.copy());
            dualGauntletPlayer.setItemInHand(InteractionHand.OFF_HAND, gauntletStack.copy());

            helper.assertTrue(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(dualGauntletPlayer),
                    "Two Scrollcaster Gauntlets should keep the previous Better Combat dual wield behavior");
            var secondGauntletAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(dualGauntletPlayer, 1);
            helper.assertTrue(secondGauntletAttack != null && secondGauntletAttack.isOffHand(),
                    "Dual Scrollcaster Gauntlets should still select offhand on the second attack but got "
                            + secondGauntletAttack);
        });
    }

    static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            var ironAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            ironAmplifier.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            var rescuedIronModifiers = BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(ironAmplifier);

            var rescuedSpellPowerBonus = sumModifierAmount(
                    rescuedIronModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(rescuedSpellPowerBonus - 0.07D) < 1.0e-9D,
                    "Better Combat rescue should keep Iron Spell Amplifier + Surge at +0.07 spell power but got "
                            + rescuedSpellPowerBonus + " modifiers=" + describeModifiers(rescuedIronModifiers));

            var copperAmplifier = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            copperAmplifier.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            var rescuedCopperModifiers = BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(copperAmplifier);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(copperAmplifier);
            helper.assertTrue(imbuedSchool != null,
                    "Copper Spell Amplifier rescue test could not resolve imbued school");
            var imbuedSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Copper Spell Amplifier rescue test could not resolve school spell power attribute");

            var rescuedAttunementBonus = sumModifierAmount(
                    rescuedCopperModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(rescuedAttunementBonus - 0.14D) < 1.0e-9D,
                    "Better Combat rescue should keep Copper Spell Amplifier base + Attunement at +0.14 but got "
                            + rescuedAttunementBonus + " modifiers=" + describeModifiers(rescuedCopperModifiers));
        });
    }
    static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "better_combat_hidden_offhand_attribute_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker but returned " + player.getOffhandItem());

            var physicalOffhand = BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
            helper.assertTrue(
                    physicalOffhand.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "Physical offhand resolver should keep Silver Spell Amplifier but got " + physicalOffhand
            );
            helper.assertTrue(
                    BetterCombatOffhandAttributeRescueCompat.isRescueActive(player),
                    "Better Combat rescue should stay active while physical offhand stack exists"
            );

            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var expectedMaxManaBonus = sumModifierAmount(
                    BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(physicalOffhand).get(maxManaAttribute),
                    AttributeModifier.Operation.ADD_VALUE
            );
            helper.assertTrue(expectedMaxManaBonus > 0.0D,
                    "Silver Spell Amplifier Better Combat rescue should provide positive max mana but got "
                            + expectedMaxManaBonus);

            var maxManaInstance = player.getAttribute(maxManaAttribute);
            helper.assertTrue(maxManaInstance != null,
                    "FakePlayer is missing max mana attribute for Better Combat rescue test");
            var baseMaxMana = maxManaInstance.getValue();
            BetterCombatOffhandAttributeRescueCompat.sync(player);
            var rescuedMaxMana = maxManaInstance.getValue();
            helper.assertTrue(Math.abs((rescuedMaxMana - baseMaxMana) - expectedMaxManaBonus) < 1.0e-9D,
                    "Better Combat rescue should restore Silver Spell Amplifier max mana by "
                            + expectedMaxManaBonus + " but changed from " + baseMaxMana + " to " + rescuedMaxMana);

            BetterCombatOffhandAttributeRescueCompat.clear(player);
        });
    }
    static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat spell rescue test");

            var copperAmplifier = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var expectedSpell = ISpellContainer.get(copperAmplifier).getSpellAtIndex(0);
            helper.assertTrue(expectedSpell != SpellData.EMPTY,
                    "Copper Spell Amplifier should expose a fixed offhand spell for Better Combat spell rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    copperAmplifier,
                    "better_combat_hidden_offhand_spell_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker spell rescue but returned "
                            + player.getOffhandItem());

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var offhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND);
            helper.assertTrue(offhandSelections.size() == 1,
                    "Better Combat spell rescue should add exactly one fixed offhand spell but got "
                            + offhandSelections.size() + " selections=" + offhandSelections);

            var rescuedSpell = selectionManager.getSpellForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            );
            helper.assertTrue(
                    rescuedSpell != SpellData.EMPTY
                            && rescuedSpell.getSpell().equals(expectedSpell.getSpell())
                            && rescuedSpell.getLevel() == expectedSpell.getLevel(),
                    "Better Combat spell rescue should restore Copper Spell Amplifier fixed spell "
                            + expectedSpell + " but got " + rescuedSpell
            );
        });
    }
    static void betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat Scrollcaster test");

            var expectedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(expectedSpell));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    gauntlet,
                    "better_combat_hidden_scrollcaster_spell_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker Scrollcaster test but returned "
                            + player.getOffhandItem());
            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .isRescueActive(player),
                    "Scrollcaster Gauntlet should not join the Better Combat attribute rescue path"
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat
                            .isRescueActive(player),
                    "Scrollcaster Gauntlet should join only the Better Combat magic-holder rescue path"
            );

            var resolvedStack =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat
                            .getResolvedHeldStack(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedStack.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    "Scrollcaster resolver should return the physical offhand gauntlet but got " + resolvedStack);

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var offhandSelections = selectionManager.getSpellsForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND
            );
            helper.assertTrue(offhandSelections.size() == 1,
                    "Better Combat Scrollcaster rescue should add exactly one selected offhand spell but got "
                            + offhandSelections.size() + " selections=" + offhandSelections);

            var rescuedSpell = selectionManager.getSpellForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            );
            helper.assertTrue(
                    rescuedSpell != SpellData.EMPTY
                            && rescuedSpell.getSpell().equals(expectedSpell)
                            && rescuedSpell.getLevel() == 1,
                    "Better Combat Scrollcaster rescue should restore selected spell "
                            + expectedSpell.getSpellResource() + " but got " + rescuedSpell
            );
        });
    }
    static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractOffhandMagicItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Offhand Magic Item");

            for (var stack : stacks) {
                // Malum の soul_shatter_capable_weapon は main hand 前提なので、
                // offhand 系は 1.21.1 でも従来の enchant 面を維持する前提で固定する。
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedOffhandEnchantments(),
                        expectedOffhandEnchantments(),
                        expectedOffhandEnchantments(),
                        expectedBookEnchantments,
                        expectedOffhandEnchantments(),
                        "Offhand Magic Item " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                    );
            }
        });
    }
    static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var expectedEnchantments = expectedEnchantedCircletEnchantments();
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    allRegisteredEnchantmentIds(helper.getLevel().registryAccess()),
                    expectedEnchantments,
                    "Enchanted Circlet"
            );
        });
    }
    static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) stack.getItem();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD,
                    helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    Attributes.ATTACK_DAMAGE,
                    -0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet attack damage penalty regression"
            );

            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ALACRITY), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.REFLUX), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.RESERVOIR), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.TENSE), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Enchanted Circlet imbued school could not be resolved");

            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Enchanted Circlet could not resolve spell power attribute for Attunement: " + imbuedSchool.getId());

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION,
                    0.02D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Alacrity regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Reflux regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA,
                    20.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Enchanted Circlet Reservoir regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER,
                    0.02D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Surge regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(resolvedSpellPower),
                    0.04D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Attunement regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Tense regression"
            );
        });
    }
    static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Enchanted Circlet should be extractable in Spellcaster Workbench");
            helper.assertFalse(new ItemStack(ItemRegistry.ASHEN_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Ashen Circlet should remain non-extractable in Spellcaster Workbench");
        });
    }
    static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "enchanted_circlet_wisdom_test"));
            var baseExperience = 20;

            var withoutCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0)), player, baseExperience);
            NeoForge.EVENT_BUS.post(withoutCirclet);
            helper.assertTrue(withoutCirclet.getDroppedExperience() == baseExperience,
                    "Wisdom baseline should stay unchanged without enchanted circlet");

            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            circletStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var withCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0)), player, baseExperience);
            NeoForge.EVENT_BUS.post(withCirclet);
            helper.assertTrue(withCirclet.getDroppedExperience() == 21,
                    "Enchanted Circlet Wisdom should match armor rate (+5% at level 1) but got " + withCirclet.getDroppedExperience());

            var roundedUp = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0)), player, 1);
            NeoForge.EVENT_BUS.post(roundedUp);
            helper.assertTrue(roundedUp.getDroppedExperience() == 2,
                    "Wisdom should round enemy experience up from 1 to 2 at +5% but got " + roundedUp.getDroppedExperience());
        });
    }
    static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            var baselinePlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_baseline_test"));
            var baselineExperience = createBlockDropsExperienceEvent(level, new BlockPos(0, 2, 0), state, baselinePlayer, ItemStack.EMPTY, 3);
            WisdomExperienceDropEvent.onBlockDrops(baselineExperience);
            helper.assertTrue(baselineExperience.getDroppedExperience() == 3,
                    "Block experience should stay unchanged without Wisdom but got " + baselineExperience.getDroppedExperience());

            var curioPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_curio_test"));
            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(curioPlayer)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for block wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var roundedCurioExperience = createBlockDropsExperienceEvent(level, new BlockPos(1, 2, 0), state, curioPlayer, ItemStack.EMPTY, 1);
            WisdomExperienceDropEvent.onBlockDrops(roundedCurioExperience);
            helper.assertTrue(roundedCurioExperience.getDroppedExperience() == 2,
                    "Curio Wisdom should round block experience up from 1 to 2 at +5% but got " + roundedCurioExperience.getDroppedExperience());

            var heldPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_held_test"));
            var spellGunStack = new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
            spellGunStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);
            heldPlayer.setItemInHand(InteractionHand.MAIN_HAND, spellGunStack);

            var heldExperience = createBlockDropsExperienceEvent(level, new BlockPos(2, 2, 0), state, heldPlayer, spellGunStack, 3);
            WisdomExperienceDropEvent.onBlockDrops(heldExperience);
            helper.assertTrue(heldExperience.getDroppedExperience() == 4,
                    "Held Wisdom should increase block experience from 3 to 4 at +20% but got " + heldExperience.getDroppedExperience());
        });
    }
}
