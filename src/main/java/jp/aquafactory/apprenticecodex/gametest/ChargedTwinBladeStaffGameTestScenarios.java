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

final class ChargedTwinBladeStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ChargedTwinBladeStaffGameTestScenarios() {
    }

    static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ChargedTwinBladeStaff) ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get();
            var stack = new ItemStack(item);
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ATTACK_DAMAGE,
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
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Charged Twin Blade Staff melee damage upgrade should be a single display modifier"
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }
    static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedChargedTwinBladeStaffEnchantments(helper.getLevel().registryAccess(), stack),
                    "Charged Twin Blade Staff"
            );
        });
    }
    static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var modifiers = toModifierMultimap(stack.getAttributeModifiers());
            var componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            var componentModifierMap = toModifierMultimap(componentModifiers);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_VALUE
            ) - 10.0D) < 1.0e-9D, "Charged Twin Blade Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get((Holder<Attribute>) io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ) - 0.10D) < 1.0e-9D, "Charged Twin Blade Staff spell power regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    componentModifierMap.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed component regression: "
                    + describeModifiers(componentModifierMap));
        });
    }
    static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var baseStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_damage_test");
            var genericTarget = new ArmorStand(level, 0.0D, 2.0D, 0.0D);
            var undeadTarget = EntityType.ZOMBIE.create(level);
            var arthropodTarget = EntityType.SPIDER.create(level);
            var aquaticTarget = EntityType.DROWNED.create(level);
            helper.assertTrue(undeadTarget != null, "Charged Twin Blade Staff damage test could not create undead target");
            helper.assertTrue(arthropodTarget != null, "Charged Twin Blade Staff damage test could not create arthropod target");
            helper.assertTrue(aquaticTarget != null, "Charged Twin Blade Staff damage test could not create aquatic target");

            var damageSource = level.damageSources().playerAttack(player);
            var baseDamage = ChargedTwinBladeStaff.resolveThrownDamage(baseStack);
            helper.assertTrue(Math.abs(baseDamage - 11.0D) < 1.0e-9D,
                    "Charged Twin Blade Staff base thrown damage regression: " + baseDamage);

            var sharpnessStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            sharpnessStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    sharpnessStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff sharpness thrown damage regression"
            );

            var smiteStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            smiteStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    undeadTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite fallback damage regression"
            );

            var baneStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            baneStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    arthropodTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane fallback damage regression"
            );

            var impalingStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            impalingStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.IMPALING), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    aquaticTarget,
                    damageSource,
                    "Charged Twin Blade Staff impaling thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff impaling fallback damage regression"
            );
        });
    }
    static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_throw_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff normal throw should consume 100 mana but left " + magicData.getMana());
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(), "Charged Twin Blade Staff throw did not spawn its projectile");
        });
    }
    static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_loyalty_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var loyalty = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
        stack.enchant(loyalty, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff loyalty mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - 100.0F / 3.0F)) < 1.0e-3F,
                "Charged Twin Blade Staff loyalty mana discount regressed: " + magicData.getMana()));
    }
    static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_riptide_test");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var riptide = helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
            stack.enchant(riptide, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff riptide test could not resolve player mana data");
            magicData.setMana(50.0F);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
            );
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff riptide should consume 50 mana on dry ground");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.01D,
                    "Charged Twin Blade Staff riptide should propel the player even without rain or water");
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff riptide should not spawn a projectile");
        });
    }
    static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var historyResolved = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(historyResolved.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should prefer recent flight history: " + historyResolved);

            var shortHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(0.001D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(0.0D, 0.0D, 1.0D)
            );
            helper.assertTrue(shortHistoryFallback.distanceTo(new Vec3(0.0D, 0.0D, 1.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history is too short: " + shortHistoryFallback);

            var reversedHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(-4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(reversedHistoryFallback.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history reverses initial throw: " + reversedHistoryFallback);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_cast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact cast test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var forward = new Vec3(0.0D, 0.0D, 1.0D);

            var instantPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, instantPayload, impactPos, forward
                    ),
                    "Charged Twin Blade Staff impact manager failed to cast an INSTANT payload"
            );
            var instantProjectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!instantProjectiles.isEmpty(),
                    "Charged Twin Blade Staff INSTANT impact cast did not spawn Magic Missile projectiles");

            var longPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "compound_phial"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, longPayload, impactPos, forward
                    ),
                    "Charged Twin Blade Staff impact manager failed to cast a LONG payload"
            );
            var longProjectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, new AABB(impactPos, impactPos).inflate(12.0D));
            helper.assertTrue(!longProjectiles.isEmpty(),
                    "Charged Twin Blade Staff LONG impact cast did not spawn Compound Phial projectiles");
        });
    }

    static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_busy_impact_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff busy impact test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff impact manager should cast through the RemoteOwner busy fallback"
            );

            helper.assertTrue(magicData.isCasting(),
                    "Charged Twin Blade Staff busy fallback should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Charged Twin Blade Staff busy fallback should preserve the original spell id");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(impactSpell),
                    "Charged Twin Blade Staff busy fallback should apply the impact spell cooldown");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff busy impact cast did not spawn Magic Missile projectiles");
        });
    }

    static void chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_busy_cooldown_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff busy cooldown test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, impactSpell, CastSource.SWORD);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff busy fallback should not bypass owner cooldowns"
            );
            helper.assertTrue(magicData.isCasting(),
                    "Charged Twin Blade Staff cooldown rejection should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Charged Twin Blade Staff cooldown rejection should preserve the original spell id");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff cooldown rejection should not spawn Magic Missile projectiles");
        });
    }

    static void spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_throwable_card_busy_impact_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Throwable Card busy impact test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    AbstractSpellThrowableCardItem.CASTING_SLOT
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Spell Throwable Card impact manager should cast through the RemoteOwner busy fallback"
            );

            helper.assertTrue(magicData.isCasting(),
                    "Spell Throwable Card busy fallback should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Spell Throwable Card busy fallback should preserve the original spell id");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Spell Throwable Card busy impact cast did not spawn Magic Missile projectiles");
        });
    }

    static void chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_remote_denylist_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff remote denylist test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var forward = new Vec3(0.0D, 0.0D, 1.0D);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    spell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            try (var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                    true,
                    List.of(spell.getSpellResource().toString())
            )) {
                helper.assertFalse(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, payload, impactPos, forward
                        ),
                        "Charged Twin Blade Staff should not fall back when Remote Owner Cast is denylisted"
                );
            }

            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff Remote Owner denylist should prevent Magic Missile projectiles");
        });
    }

    static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_self_profile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff self profile test could not resolve player mana data");
            magicData.setMana(200.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff self profile failed to cast Oakskin"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff self profile should apply Oakskin to the real player");
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_remote_owner_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative RemoteOwner profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use RemoteOwner profile with zero mana"
            );
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff creative RemoteOwner profile should spawn Magic Missile projectiles");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative RemoteOwner profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_staff_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative staff profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use staff profile with zero mana"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff creative staff profile should apply Oakskin to the real player");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative staff profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead test could not resolve player mana data");
            magicData.setMana(500.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead profile failed its initial cast"
            );
            var summons = level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            );
            helper.assertTrue(!summons.isEmpty(),
                    "Charged Twin Blade Staff Raise Dead profile should summon mobs near the impact");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should register recast on the real player");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should not add a normal cooldown for a recast spell");
            summons.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_recast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead recast test could not resolve player mana data");
            magicData.setMana(500.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead recast setup failed"
            );
            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead should not recast while an initial recast is active"
            );
            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_selection_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead selection test could not resolve player mana data");
            magicData.setMana(500.0F);

            var amplifierStack = new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var mutable = ISpellContainer.create(2, true, false).mutableCopy();
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 0, false),
                    "Failed to prepare first wheel spell for Raise Dead selection regression");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(), 1, 1, false),
                    "Failed to prepare Raise Dead wheel spell for selection regression");
            ISpellContainer.set(amplifierStack, mutable.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            magicData.getSyncedData().setSpellSelection(new io.redspace.ironsspellbooks.gui.overlays.SpellSelection(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    1
            ));

            var beforeSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(beforeSelection != null
                            && beforeSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead selection regression setup should select Raise Dead but got " + beforeSelection);

            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead selection regression failed its initial cast"
            );

            var afterSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(afterSelection != null
                            && afterSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead impact cast should preserve the selected wheel spell but got " + afterSelection);
            var recastPayload = jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload.capture(afterSelection, player);
            helper.assertFalse(recastPayload.isPresent(),
                    "Raise Dead active recast should not fall back to a different wheel spell payload");

            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_unprofiled_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff unprofiled test could not resolve player mana data");
            magicData.setMana(500.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff should reject spells without a RemoteOwner impact profile"
            );
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff continuous impact test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath"),
                1,
                CastSource.SWORD.name(),
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );

        helper.runAtTickTime(1, () -> helper.assertTrue(
                jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                ),
                "Charged Twin Blade Staff impact manager failed to start a CONTINUOUS payload"
        ));
        helper.succeedWhen(() -> {
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(impactPos, impactPos).inflate(16.0D));
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff CONTINUOUS impact cast did not spawn Fire Breath projectiles");
            var anchorOwner = projectiles.stream()
                    .map(FireBreathProjectile::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "Charged Twin Blade Staff CONTINUOUS impact cast should use a Remote Owner anchor for Fire Breath owner tracking");
            helper.assertTrue(anchorOwner.get().getDisplayName().getString().equals(player.getDisplayName().getString()),
                    "Remote Owner anchor should expose the player name for death messages");
        });
    }

    static void chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_remote_continuous_profile_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff RemoteOwner-only continuous test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                spell.getSpellResource(),
                1,
                CastSource.SWORD.name(),
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );

        helper.runAtTickTime(1, () -> {
            try (var ignoredRemoteProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(spell),
                    remotePlayerGeometryProfile(false)
            ))) {
                helper.assertTrue(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                        ),
                        "Charged Twin Blade Staff should start RemoteOwner CONTINUOUS casts without a Spell Dispenser profile"
                );
            }
        });
        helper.succeedWhen(() -> {
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(impactPos, impactPos).inflate(16.0D));
            helper.assertTrue(!projectiles.isEmpty(),
                    "RemoteOwner-only CONTINUOUS impact cast did not spawn Fire Breath projectiles");
            var anchorOwner = projectiles.stream()
                    .map(FireBreathProjectile::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "RemoteOwner-only CONTINUOUS impact cast should keep Fire Breath owned by a Remote Owner anchor");
        });
    }

    static void chargedTwinBladeStaffContinuousThrowableCardUsesCardCooldownPolicy(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_card_continuous_cooldown_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff card continuous cooldown test could not resolve player mana data");
        magicData.setMana(500.0F);
        var cardStack = new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                spell.getSpellResource(),
                1,
                CastSource.SWORD.name(),
                AbstractSpellThrowableCardItem.CASTING_SLOT
        );

        var cardPolicyCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                spell,
                player,
                CastSource.SWORD,
                cardStack
        );
        var emptyStackCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                spell,
                player,
                CastSource.SWORD,
                ItemStack.EMPTY
        );
        helper.assertTrue(cardPolicyCooldown > emptyStackCooldown,
                "Throwable Card cooldown regression needs a visible policy difference: "
                        + cardPolicyCooldown + " / empty " + emptyStackCooldown);

        helper.runAtTickTime(1, () -> helper.assertTrue(
                jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        level, player, cardStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                ),
                "Charged Twin Blade Staff impact manager failed to start a Throwable Card CONTINUOUS payload"
        ));

        helper.succeedWhen(() -> {
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            helper.assertTrue(cooldown != null,
                    "Throwable Card CONTINUOUS impact cast has not finished its cooldown yet");
            var remainingCooldown = cooldown.getCooldownRemaining();
            helper.assertTrue(remainingCooldown > emptyStackCooldown,
                    "Throwable Card CONTINUOUS cooldown used the empty-stack weapon imbue policy: "
                            + remainingCooldown + " / empty " + emptyStackCooldown);
            helper.assertTrue(remainingCooldown <= cardPolicyCooldown,
                    "Throwable Card CONTINUOUS cooldown exceeded the card policy cooldown: "
                            + remainingCooldown + " / card " + cardPolicyCooldown);
        });
    }

    static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact fail test could not resolve player mana data");
            magicData.setMana(0.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff impact manager should skip casts when the owner cannot pay the spell mana"
            );
        });
    }
}
