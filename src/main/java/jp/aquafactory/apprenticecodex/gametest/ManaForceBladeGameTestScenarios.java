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
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeGuardLogic;
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

final class ManaForceBladeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ManaForceBladeGameTestScenarios() {
    }
    static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, stack, spell, 1);
            stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.ATTUNEMENT), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            var upgradeKey = findUpgradeKeyForPowerAttribute(attunementAttribute);
            helper.assertTrue(upgradeKey != null,
                    "Mana Force Blade test could not resolve a matching upgrade orb for " + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute));

            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    upgradeKey,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    stack.getItem().getDefaultAttributeModifiers(stack)
            );
            NeoForge.EVENT_BUS.post(event);
            var modifiers = toModifierMultimap(event.build());

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.09D,
                    "Mana Force Blade Attunement and matching upgrade should merge into one display modifier"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }

    static void manaForceBladeAppliesSurgeAndAttunementAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();

            var surgeStack = new ItemStack(item);
            item.initializeSpellContainer(surgeStack);
            surgeStack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SURGE), 1);
            var surgeModifiers = toModifierMultimap(surgeStack.getAttributeModifiers());
            assertSingleModifierAmount(
                    helper,
                    surgeModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.02D,
                    "Mana Force Blade Surge should add spell power"
                            + " modifiers=" + describeModifiers(surgeModifiers)
            );

            var effectiveSurgeSpellPower = sumEffectiveModifierAmount(
                    surgeStack,
                    EquipmentSlot.MAINHAND,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(effectiveSurgeSpellPower - 0.02D) < 1.0e-9D,
                    "Mana Force Blade Surge should apply spell power in main hand"
                            + " amount=" + effectiveSurgeSpellPower
                            + " modifiers=" + describeModifiers(surgeModifiers));

            var attunementStack = new ItemStack(item);
            item.initializeSpellContainer(attunementStack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, attunementStack, spell, 1);
            attunementStack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.ATTUNEMENT), 1);
            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(attunementStack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade Attunement test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade Attunement test could not resolve the spell power attribute: " + imbuedSchool.getId());
            var attunementModifiers = toModifierMultimap(attunementStack.getAttributeModifiers());
            assertSingleModifierAmount(
                    helper,
                    attunementModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.04D,
                    "Mana Force Blade Attunement should add imbued school spell power"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " modifiers=" + describeModifiers(attunementModifiers)
            );
            var effectiveAttunementSpellPower = sumEffectiveModifierAmount(
                    attunementStack,
                    EquipmentSlot.MAINHAND,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(effectiveAttunementSpellPower - 0.04D) < 1.0e-9D,
                    "Mana Force Blade Attunement should apply school spell power in main hand"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " amount=" + effectiveAttunementSpellPower
                            + " modifiers=" + describeModifiers(attunementModifiers));
        });
    }

    static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_attack_mana_once_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Mana Force Blade attack mana test could not resolve player mana data");
            magicData.setMana(100.0F);

            var firstTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            var secondTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            item.hurtEnemy(stack, firstTarget, player);
            item.hurtEnemy(stack, secondTarget, player);

            var expectedMana = 100.0F
                    - jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(player, stack);
            helper.assertTrue(Math.abs(magicData.getMana() - expectedMana) < 1.0e-4F,
                    "Mana Force Blade should spend attack mana once per tick even when multiple targets are hit"
                            + " expected=" + expectedMana
                            + " actual=" + magicData.getMana());
        });
    }

    static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_config_formula_test");
            var spellPower = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPower != null,
                    "Mana Force Blade config formula test could not resolve spell power attribute");
            if (spellPower != null) {
                spellPower.setBaseValue(1.5D);
            }

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade config formula test could not resolve imbued school");
            var schoolPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(schoolPowerAttribute != null,
                    "Mana Force Blade config formula test could not resolve school power attribute");
            var schoolPower = schoolPowerAttribute == null
                    ? null
                    : player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute));
            helper.assertTrue(schoolPower != null,
                    "Mana Force Blade config formula test could not resolve player school power instance");
            if (schoolPower != null) {
                schoolPower.setBaseValue(1.2D);
            }

            var baseDamage = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackDamage(stack);
            var damageMultiplier = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 1.0F);
            helper.assertTrue(Math.abs(damageMultiplier - 1.8F) < 1.0e-4F,
                    "Mana Force Blade should multiply spell power and school power for imbued damage but got "
                            + damageMultiplier);
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.5F) - 0.9F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale should directly scale the final school multiplier");
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.0F) - 1.0F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale 0 should disable imbued damage changes");

            var fullManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 1.0F);
            helper.assertTrue(Math.abs(fullManaCost - baseDamage * 3.0F * 1.8F) < 1.0e-4F,
                    "Mana Force Blade full school mana scale should follow final imbued damage: " + fullManaCost);

            var halfSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.5F, 1.0F);
            helper.assertTrue(Math.abs(halfSchoolManaCost - baseDamage * 3.0F * 1.4F) < 1.0e-4F,
                    "Mana Force Blade half school mana scale should only halve the school-derived increase: "
                            + halfSchoolManaCost);

            var noSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.0F, 1.0F);
            helper.assertTrue(Math.abs(noSchoolManaCost - baseDamage * 3.0F) < 1.0e-4F,
                    "Mana Force Blade school mana scale 0 should ignore school multiplier for mana cost: "
                            + noSchoolManaCost);

            var disabledManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 0.0F);
            helper.assertTrue(disabledManaCost == 0.0F,
                    "Mana Force Blade imbue damage scale 0 should also disable hit mana cost");
        });
    }

    static void manaForceBladeReleaseCooldownUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_release_cooldown_config_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    7,
                    0,
                    0
            )) {
                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
            }

            helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                    "Mana Force Blade release should apply server-configured cooldown");

            var disabledCooldownStack = new ItemStack(item);
            item.initializeSpellContainer(disabledCooldownStack);
            var disabledCooldownPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 1),
                    "mana_force_blade_release_cooldown_disabled_test");
            disabledCooldownPlayer.setItemInHand(InteractionHand.MAIN_HAND, disabledCooldownStack);
            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    0,
                    0,
                    0
            )) {
                item.releaseUsing(
                        disabledCooldownStack,
                        helper.getLevel(),
                        disabledCooldownPlayer,
                        item.getUseDuration(disabledCooldownStack, disabledCooldownPlayer)
                );
            }
            helper.assertFalse(disabledCooldownPlayer.getCooldowns().isOnCooldown(item),
                    "Mana Force Blade release cooldown config 0 should disable release cooldown");
        });
    }

    static void manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_release_cooldown_grace_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            try (var ignored = ApprenticeCodexServerConfig.useManaForceBladeCooldownConfigOverrideForGameTest(
                    7,
                    40,
                    1
            )) {
                ManaForceBladeGuardLogic.tryHandleGuard(player, stack, player.damageSources().generic(), true, false);

                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
                helper.assertFalse(player.getCooldowns().isOnCooldown(item),
                        "Mana Force Blade perfect guard grace should skip release cooldown once");

                item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack, player));
                helper.assertTrue(player.getCooldowns().isOnCooldown(item),
                        "Mana Force Blade perfect guard grace should not skip release cooldown more than once");
            }
        });
    }

    static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            var requiredExtraEnchantments = registryIdSet(
                    Enchantments.SURGE,
                    Enchantments.ATTUNEMENT,
                    Enchantments.WISDOM,
                    Enchantments.TRANSCENDENCE
            );
            addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, requiredExtraEnchantments);
            helper.assertFalse(stack.getItem() instanceof NonDamageableAnvilMergeItem,
                    "Mana Force Blade should not keep the non-damageable anvil merge hook");
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredExtraEnchantments,
                    null,
                    "Mana Force Blade"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    registryIdSet(Enchantments.REFLUX, Enchantments.RESERVOIR),
                    null,
                    "Mana Force Blade should reject mana pool/recovery enchantments"
            );
        });
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
}
