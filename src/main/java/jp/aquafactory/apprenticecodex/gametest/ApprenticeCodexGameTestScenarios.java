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
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.mobs.SummonedZombie;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellThrowableCardServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.LinearBuildServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import jp.aquafactory.apprenticecodex.event.ErrandMageVillagerTradesEvent;
import jp.aquafactory.apprenticecodex.loot.RandomSpellImbueHelper;
import jp.aquafactory.apprenticecodex.item.shield.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet.AbsorptionAmplifyAmulet;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeProjectileEntity;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffPendingAdvance;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletFreecastContext;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeHelper;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffManaCostEvent;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffPowerHelper;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.LootConditionRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWings;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurret;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetCollectionMode;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarManager;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWings;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsManager;
import jp.aquafactory.apprenticecodex.spell.divinepossession.DivinePossessionPowerHelper;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobat;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatCounterSpellEvent;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatSmgEntity;
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerManager;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceBurst;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuild;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileEntity;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceInput;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShield;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldDefenseEvent;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldShieldEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlock;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxCounterSpellEvent;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEye;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEyeBodyControlEvent;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrost;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemEntity;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonEntity;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.Wizardlamp;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.WizardlampLanternBlock;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.ProcessingRecipeDenylist;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import io.netty.buffer.Unpooled;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class ApprenticeCodexGameTestScenarios {
    static final double SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE = 1.5D;

    static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "combat_policy_self_owner");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Default combat policy should protect the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.ALLOW_SELF_PROTECT_ALLIES),
                    "Self-damage combat policy should allow damage to the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, null, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Ownerless damage should not infer protection");
        });
    }

    static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var server = level.getServer();
            helper.assertTrue(server != null, "Combat policy PvP test requires a server");
            var owner = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_owner"));
            var target = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_target"));
            var scoreboard = level.getScoreboard();
            var team = scoreboard.addPlayerTeam("codex_policy");
            var previousPvp = server.isPvpAllowed();
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(target.getScoreboardName(), team);

            try {
                server.setPvpAllowed(true);
                team.setAllowFriendlyFire(false);
                helper.assertTrue(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Team members should be protected while friendly fire is disabled");

                team.setAllowFriendlyFire(true);
                helper.assertFalse(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Team members should not be protected while friendly fire is enabled");

                scoreboard.removePlayerFromTeam(owner.getScoreboardName(), team);
                scoreboard.removePlayerFromTeam(target.getScoreboardName(), team);
                server.setPvpAllowed(false);
                helper.assertTrue(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Players should be protected while server PvP is disabled");
            } finally {
                server.setPvpAllowed(previousPvp);
                scoreboard.removePlayerTeam(team);
            }
        });
    }

    static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "combat_policy_vehicle_owner");
            var passenger = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "combat_policy_vehicle_passenger");
            level.addFreshEntity(owner);
            level.addFreshEntity(passenger);
            var boat = new Boat(level, owner.getX(), owner.getY(), owner.getZ());
            level.addFreshEntity(boat);
            helper.assertTrue(owner.startRiding(boat, true), "Combat policy test owner should board the boat");
            helper.assertTrue(passenger.startRiding(boat, true), "Combat policy test passenger should board the boat");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            boat, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "The ridden vehicle should be protected");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            passenger, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Passengers sharing the root vehicle should be protected");

            var wolf = new Wolf(EntityType.WOLF, level);
            wolf.setOwnerUUID(owner.getUUID());
            wolf.setTame(true);
            level.addFreshEntity(wolf);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            wolf, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Owned tame animals should be protected");

            var summon = new SummonedZombie(
                    io.redspace.ironsspellbooks.registries.EntityRegistry.SUMMONED_ZOMBIE.get(), level);
            level.addFreshEntity(summon);
            SummonManager.setOwner(summon, owner);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            summon, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Iron's summons should be protected from their summoner");
        });
    }

    static final String REQUIRED_OPTIONAL_MODS_PROPERTY = "apprenticecodex.requiredOptionalMods";
    static final String VANILLA_NAMESPACE = "minecraft";
    static final String CREATE_MOD_ID = "create";
    static final String CREATE_GAMETEST_HOOKS_CLASS =
            "jp.aquafactory.apprenticecodex.gametest.create.CreateGameTestHooks";
    static final String FARMERS_DELIGHT_MOD_ID = "farmersdelight";
    static final String LODESTONE_MOD_ID = "lodestone";
    static final String MALUM_MOD_ID = "malum";
    static final ResourceLocation FARMERS_DELIGHT_TOMATO_BLOCK =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomatoes");
    static final ResourceLocation FARMERS_DELIGHT_TOMATO_ITEM =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomato");
    static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_proficiency");
    static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );
    static final TagKey<Item> IRONS_STAFF = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staff")
    );
    static final TagKey<Item> SPELLCASTER_WORKBENCH_EXTRACTABLE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench_extractable")
    );
    static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("traveloptics", "can_cast_reversal")
    );
    static final TagKey<Item> CURIOS_BACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BACK)
    );
    static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );
    static final UUID FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");
    static final UUID CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID =
            UUID.fromString("04a46352-a09b-44fb-b504-92ab5f69f969");
    static final UUID ZENITH_STAFF_SCHOOL_POWER_TEST_MODIFIER_ID =
            UUID.fromString("dc11d258-0a7d-4e1e-a0c6-74754fb91d25");
    static final UUID TOTEM_OF_PERMAFROST_SUMMON_DAMAGE_TEST_MODIFIER_ID =
            UUID.fromString("7543bb74-30b3-4e0e-9d73-6e4d71d1e218");
    static final UUID VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID =
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    static final UUID VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();
    static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();
    static final ResourceLocation MALUM_SPIRIT_PLUNDER = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");
    static final ResourceLocation MALUM_REPLENISHING = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");

    ApprenticeCodexGameTestScenarios() {
    }

    static void requiredOptionalModsAreLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var requiredModsProperty = System.getProperty(REQUIRED_OPTIONAL_MODS_PROPERTY, "").trim();
            if (requiredModsProperty.isEmpty()) {
                return;
            }

            var missingModIds = Arrays.stream(requiredModsProperty.split(","))
                    .map(String::trim)
                    .filter(modId -> !modId.isEmpty())
                    .filter(modId -> !ModList.get().isLoaded(modId))
                    .toList();
            helper.assertTrue(missingModIds.isEmpty(),
                    "Required optional MODs are missing for this GameTest run: " + missingModIds);
        });
    }

    static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertForgeRegistryEntries(helper, "item", net.minecraftforge.registries.ForgeRegistries.ITEMS, ItemRegistry.ITEMS.getEntries());
            assertForgeRegistryEntries(helper, "block", net.minecraftforge.registries.ForgeRegistries.BLOCKS, BlockRegistry.BLOCKS.getEntries());
            assertForgeRegistryEntries(helper, "block entity", net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES, BlockEntityRegistry.BLOCK_ENTITY_TYPES.getEntries());
            assertForgeRegistryEntries(helper, "entity", net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES, EntityRegistry.ENTITIES.getEntries());
            assertForgeRegistryEntries(helper, "mob effect", net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS, EffectRegistry.EFFECTS.getEntries());
            assertForgeRegistryEntries(helper, "enchantment", net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS, EnchantmentRegistry.ENCHANTMENTS.getEntries());
            assertForgeRegistryEntries(helper, "attribute", net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES, ApprenticeAttributeRegistry.ATTRIBUTES.getEntries());
            assertForgeRegistryEntries(helper, "recipe serializer", net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, RecipeRegistry.RECIPE_SERIALIZERS.getEntries());
            assertForgeRegistryEntries(helper, "point of interest type", net.minecraftforge.registries.ForgeRegistries.POI_TYPES, PoiTypeRegistry.POI_TYPES.getEntries());
            assertForgeRegistryEntries(helper, "villager profession", net.minecraftforge.registries.ForgeRegistries.VILLAGER_PROFESSIONS, VillagerProfessionRegistry.VILLAGER_PROFESSIONS.getEntries());

            assertBuiltinRegistryEntries(helper, "potion", BuiltInRegistries.POTION, PotionRegistry.POTIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe type", BuiltInRegistries.RECIPE_TYPE, RecipeRegistry.RECIPE_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "creative tab", BuiltInRegistries.CREATIVE_MODE_TAB, CreativeTabRegistry.TABS.getEntries());
            assertBuiltinRegistryEntries(helper, "loot condition type", BuiltInRegistries.LOOT_CONDITION_TYPE, LootConditionRegistry.LOOT_CONDITION_TYPES.getEntries());

            var apprenticeDeskPoi = PoiTypes.forState(BlockRegistry.APPRENTICE_DESK.get().defaultBlockState()).orElse(null);
            helper.assertTrue(apprenticeDeskPoi != null, "Apprentice Desk POI state mapping is missing");
            helper.assertTrue(apprenticeDeskPoi != null && apprenticeDeskPoi.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    "Apprentice Desk resolved to unexpected POI: " + apprenticeDeskPoi);
            helper.assertTrue(apprenticeDeskPoi != null && apprenticeDeskPoi.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                    "Apprentice Desk POI is missing minecraft:acquirable_job_site");
            helper.assertTrue(BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfessionRegistry.ERRAND_MAGE_KEY.location()) == VillagerProfessionRegistry.ERRAND_MAGE.get(),
                    "Errand Mage profession is missing from BuiltInRegistries");

            for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
                var spell = spellEntry.get();
                var spellId = spell.getSpellResource();
                helper.assertTrue(spellId != null, "Spell id is null: " + spellEntry.getId());
                helper.assertTrue(ApprenticeCodex.MODID.equals(spellId.getNamespace()), "Spell namespace mismatch: " + spellId);
                helper.assertTrue(spell.getSchoolType() != null, "Spell school is null: " + spellId);
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.REGISTRY.get().getValue(spellId) == spell,
                        "Spell registry lookup failed: " + spellId);
            }

            var assignedDefinitions = SchoolAffinityRegistry.getDefinitions().stream()
                    .filter(definition -> SchoolAffinityRegistry.getAssignedSchool(definition.slotIndex()).isPresent())
                    .toList();
            helper.assertFalse(assignedDefinitions.isEmpty(), "No School Affinity assignments were resolved");
            helper.assertFalse(SchoolAffinityRegistry.getBrewingDefinitionsByCatalyst().isEmpty(), "No School Affinity catalysts were resolved");
            assertSearchBeaconTarget(helper, Items.BLAZE_ROD, "irons_spellbooks:pyromancer_tower");
            assertSearchBeaconTarget(helper, Items.EMERALD, "irons_spellbooks:evoker_fort");
            assertSearchBeaconTarget(helper, Items.POISONOUS_POTATO, "irons_spellbooks:mangrove_hut");
            assertSearchBeaconTarget(helper, Items.SCULK_SENSOR, "minecraft:ancient_city");

            var divinePearl = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "divine_pearl"));
            helper.assertTrue(divinePearl != null, "irons_spellbooks:divine_pearl is not registered");
            var villageDefinition = divinePearl != null
                    ? SearchBeaconTargetManager.getDefinition(new ItemStack(divinePearl))
                    : null;
            helper.assertTrue(villageDefinition != null, "SearchBeacon target missing for irons_spellbooks:divine_pearl");
            helper.assertTrue(
                    villageDefinition != null
                            && villageDefinition.targets().contains(new SearchBeaconTargetList.TargetReference(true, ResourceLocation.withDefaultNamespace("village"))),
                    "SearchBeacon divine pearl target should point to #minecraft:village"
            );

            for (var definition : assignedDefinitions) {
                helper.assertTrue(BuiltInRegistries.MOB_EFFECT.get(definition.effectId()) == definition.effect(),
                        "Missing School Affinity effect: " + definition.effectId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.basePotionId()) == definition.basePotion(),
                        "Missing School Affinity potion: " + definition.basePotionId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.longPotionId()) == definition.longPotion(),
                        "Missing School Affinity potion: " + definition.longPotionId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.strongPotionId()) == definition.strongPotion(),
                        "Missing School Affinity potion: " + definition.strongPotionId());
            }
        });
    }

    static void ownSpellsUniqueInfoAcceptsNullCaster(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var failures = new ArrayList<String>();
            for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
                var spell = spellEntry.get();
                var spellId = String.valueOf(spell.getSpellResource());
                for (var spellLevel = spell.getMinLevel(); spellLevel <= spell.getMaxLevel(); spellLevel++) {
                    try {
                        var uniqueInfo = spell.getUniqueInfo(spellLevel, null);
                        if (uniqueInfo == null) {
                            failures.add(spellId + " level " + spellLevel + " returned null");
                        } else if (uniqueInfo.stream().anyMatch(Objects::isNull)) {
                            failures.add(spellId + " level " + spellLevel + " returned a null component");
                        }
                    } catch (RuntimeException exception) {
                        failures.add(spellId + " level " + spellLevel + " threw "
                                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
                    }
                }
            }

            helper.assertTrue(failures.isEmpty(),
                    "Apprentice spell getUniqueInfo must accept null caster: " + String.join("; ", failures));
        });
    }

    static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pottedComfortBerryBush = (FlowerPotBlock) BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get();
            helper.assertTrue(pottedComfortBerryBush.getContent() == BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    "Potted Comfort Berry Bush should contain the Comfort Berry Bush block");
            helper.assertTrue(pottedComfortBerryBush.defaultBlockState().getLightEmission(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))) == 10,
                    "Potted Comfort Berry Bush should emit light level 10");

            var potPos = new BlockPos(1, 1, 1);
            var absolutePotPos = helper.absolutePos(potPos);
            helper.setBlock(potPos, Blocks.FLOWER_POT);

            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "comfort_berry_pot_test"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.COMFORT_BERRIES.get()));
            var result = helper.getLevel().getBlockState(absolutePotPos).use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolutePotPos), Direction.UP, absolutePotPos, false)
            );

            helper.assertTrue(result.consumesAction(), "Comfort Berries should be accepted by a vanilla Flower Pot");
            helper.assertBlockPresent(BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get(), potPos);
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "Potted Comfort Berries should consume one berry item outside creative mode");
        });
    }

    static void comfortBerriesHaveAppleTierCompostChance(GameTestHelper helper) {
        helper.succeedIf(() -> helper.assertTrue(Float.compare(
                        ComposterBlock.COMPOSTABLES.getFloat(ItemRegistry.COMFORT_BERRIES.get()),
                        0.65F
                ) == 0,
                "Comfort Berries should have the apple-tier compost chance of 0.65"));
    }

    static void assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(GameTestHelper helper) {
        helper.succeedIf(() -> helper.assertTrue(
                new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()).is(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS),
                "Smashcast Scepter should be tagged as an Assist Wings only-jump item"
        ));
    }

    static void manaMendingRequiresDamagedHeldItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_empty_test");
            var magicData = MagicData.getPlayerMagicData(player);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should reject empty hands");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            magicData.setAdditionalCastData(null);
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should reject undamaged damageable items");
        });
    }

    static void manaMendingRepairsMainHandDamagedItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_mainhand_test");
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(5);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept a damaged main-hand item");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(sword.getDamageValue() == 4,
                    "Mana Mending should repair 1 durability at level 1 but damage is " + sword.getDamageValue());
        });
    }

    static void manaMendingPrefersOffhandDamagedItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_offhand_test");
            var mainHandSword = new ItemStack(Items.DIAMOND_SWORD);
            var offhandPickaxe = new ItemStack(Items.IRON_PICKAXE);
            mainHandSword.setDamageValue(5);
            offhandPickaxe.setDamageValue(5);
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHandSword);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandPickaxe);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept damaged items in both hands");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(offhandPickaxe.getDamageValue() == 4,
                    "Mana Mending should repair the offhand item first but offhand damage is "
                            + offhandPickaxe.getDamageValue());
            helper.assertTrue(mainHandSword.getDamageValue() == 5,
                    "Mana Mending should not repair the main-hand item while offhand is the locked target");
        });
    }

    static void manaMendingCraftsmansDelightBoostsRepairAndClearsRepairCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_craftsmans_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamageValue(3);
            pickaxe.setRepairCost(7);
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept a damaged item while CraftsmansDelight is equipped");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            helper.assertTrue(pickaxe.getDamageValue() == 2,
                    "First CraftsmansDelight-boosted repair tick should repair 1 durability and carry 0.5");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(pickaxe.getDamageValue() == 0,
                    "Second CraftsmansDelight-boosted repair tick should spend the carried fraction and finish repair");
            helper.assertTrue(pickaxe.getBaseRepairCost() == 0,
                    "CraftsmansDelight Mana Mending completion should reset repair cost");
        });
    }
    static void assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_regular_air_test");
            player.setOnGround(false);
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 6.0F;

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && movement.y > 0.59D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings should preserve horizontal movement while applying the regular air jump");
            helper.assertTrue(player.hasImpulse,
                    "Assist Wings should mark the regular air jump as an impulse");
            helper.assertFalse(player.hurtMarked,
                    "Assist Wings should not force a full velocity sync back to the casting ServerPlayer");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset fall distance on a regular air jump");
            helper.assertTrue(getAssistWingsDoneJump(player) == 1,
                    "Regular air casts should consume one Assist Wings air jump");
            helper.assertTrue(countActiveAssistWingsWings(helper, player) == 1,
                    "Regular Assist Wings casts should keep one wing entity");
            assertAssistWingsCastDataRoundTrips(helper, magicData, true);
        });
    }

    static void assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 2, 0), "assist_wings_smashcast_ground_test");
            player.setOnGround(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 5.0F;

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Assist Wings should allow a ground cast with Smashcast Scepter");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && movement.y > 0.59D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings should preserve horizontal movement during a Smashcast Scepter ground jump");
            helper.assertFalse(player.hurtMarked,
                    "Smashcast Scepter ground jumps should not force a full velocity sync back to the casting ServerPlayer");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset fall distance at takeoff");
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "Ground casts with a tagged item should not consume an air jump");
            helper.assertTrue(countActiveAssistWingsWings(helper, player) == 1,
                    "Assist Wings should keep its wing entity while a tagged item is held");

            var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            if (!(helper.getLevel().getEntity(state.localEntityId) instanceof AssistWingsWingEntity wing)) {
                helper.fail("Assist Wings should register its wing entity in spell state");
                return;
            }
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "A tagged main-hand item should block Assist Wings fall protection");
            helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING),
                    "Assist Wings should not use the vanilla Slow Falling effect");
            assertAssistWingsCastDataRoundTrips(helper, magicData, true);
        });
    }

    static void assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_offhand_protection_test");
            player.setOnGround(false);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 7.0F;

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());
            wing.tickOnServer(helper.getLevel());

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && Math.abs(movement.y + 0.2D) < 0.0001D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "A tagged offhand item should leave falling movement unchanged");
            helper.assertTrue(player.fallDistance == 7.0F,
                    "A tagged offhand item should preserve accumulated fall distance");
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "A tagged offhand item should block Assist Wings fall protection");
            helper.assertFalse(wing.isRemoved(),
                    "Blocking fall protection from the offhand should not discard the wing");
        });
    }

    static void assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_restore_protection_test");
            player.setOnGround(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "The tagged main-hand item should initially block fall protection");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setDeltaMovement(0.12D, -0.3D, -0.08D);
            player.fallDistance = 8.0F;
            player.setShiftKeyDown(true);
            wing.tickOnServer(helper.getLevel());

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && Math.abs(movement.y + 0.3D) < 0.0001D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings fall protection should not change movement while sneaking");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Restored fall protection should reset accumulated fall distance while sneaking");
            helper.assertFalse(wing.isFallProtectionBlocked(),
                    "Removing the tagged item should restore the normal wing state");
            helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING),
                    "Assist Wings fall protection should not add the vanilla Slow Falling effect");

            player.setShiftKeyDown(false);
            player.setDeltaMovement(-0.07D, -0.45D, 0.09D);
            player.fallDistance = 12.0F;
            wing.tickOnServer(helper.getLevel());
            movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x + 0.07D) < 0.0001D
                            && Math.abs(movement.y + 0.45D) < 0.0001D
                            && Math.abs(movement.z - 0.09D) < 0.0001D,
                    "Assist Wings fall protection should not change regular falling movement");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset regular accumulated fall distance");
        });
    }

    static void assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 2, 0), "assist_wings_tagged_landing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 2, wing.getId());

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            player.setOnGround(false);
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Tagged items should not bypass the normal Assist Wings air-jump limit");

            player.setOnGround(true);
            for (var i = 0; i < 10; ++i) {
                wing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(wing.isRemoved(),
                    "The wing should ignore landing during its first 10 ticks");
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Landing during the removal grace should preserve Assist Wings air jumps");

            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isRemoved(),
                    "The wing should detect landing after its 10 tick removal grace");
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "Landing with a tagged item should reset Assist Wings air jumps");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setOnGround(false);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "A normal midair Assist Wings cast should be available after the tagged landing reset");
        });
    }
    static void assistWingsWaterRemovalUsesGraceAndResetsAirJumps(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            helper.setBlock(waterPos, Blocks.WATER);

            var submergedPlayer = createAssistWingsPlayer(helper, waterPos, "assist_wings_water_removal_test");
            submergedPlayer.setOnGround(false);
            var submergedWing = new AssistWingsWingEntity(
                    EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), submergedPlayer);
            helper.getLevel().addFreshEntity(submergedWing);
            setAssistWingsState(submergedPlayer, 2, submergedWing.getId());

            for (var i = 0; i < 10; ++i) {
                submergedWing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(submergedWing.isRemoved(),
                    "Water contact should not remove Assist Wings during its first 10 ticks");
            helper.assertTrue(getAssistWingsDoneJump(submergedPlayer) == 2,
                    "Water contact during the removal grace should preserve Assist Wings air jumps");

            submergedWing.tickOnServer(helper.getLevel());
            helper.assertTrue(submergedWing.isRemoved(),
                    "Water contact should remove Assist Wings after its 10 tick removal grace");
            var submergedState = Capabilities.getSpellDataOrNull(submergedPlayer)
                    .get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            helper.assertTrue(submergedState.localEntityId == -1 && submergedState.doneJump == 0,
                    "Water removal should clear the managed wing and reset Assist Wings air jumps");

            var leavingWaterPos = new BlockPos(3, 2, 0);
            helper.setBlock(leavingWaterPos, Blocks.WATER);
            var leavingPlayer = createAssistWingsPlayer(helper, leavingWaterPos, "assist_wings_leave_water_test");
            leavingPlayer.setOnGround(false);
            var leavingWing = new AssistWingsWingEntity(
                    EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), leavingPlayer);
            helper.getLevel().addFreshEntity(leavingWing);
            setAssistWingsState(leavingPlayer, 1, leavingWing.getId());

            for (var i = 0; i < 5; ++i) {
                leavingWing.tickOnServer(helper.getLevel());
            }
            var dryPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(6, 4, 0)));
            leavingPlayer.setPos(dryPos.x, dryPos.y, dryPos.z);
            for (var i = 0; i < 6; ++i) {
                leavingWing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(leavingWing.isRemoved(),
                    "Leaving water during the removal grace should keep Assist Wings active");
            helper.assertTrue(getAssistWingsDoneJump(leavingPlayer) == 1,
                    "Leaving water during the removal grace should preserve Assist Wings air jumps");
        });
    }

    static void assistWingsLavaContactDoesNotRemoveWing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lavaPos = new BlockPos(0, 2, 0);
            helper.setBlock(lavaPos, Blocks.LAVA);
            var player = createAssistWingsPlayer(helper, lavaPos, "assist_wings_lava_contact_test");
            player.setOnGround(false);
            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 2, wing.getId());

            for (var i = 0; i < 11; ++i) {
                wing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(wing.isRemoved(),
                    "Lava contact should not remove Assist Wings");
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Lava contact should not reset Assist Wings air jumps");
        });
    }

    static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var unknownMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city"),
                    1L
            );
            var searchedMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "village_plains"),
                    2L
            );
            var unknownResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            unknownMarker,
                            BlockPos.ZERO,
                            SearchBeaconState.StructureKnowledge.UNKNOWN,
                            4.0
                    )
            ));
            var knownOnlyResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            searchedMarker,
                            new BlockPos(8, 0, 0),
                            SearchBeaconState.StructureKnowledge.SEARCHED,
                            64.0
                    )
            ));

            helper.assertFalse(
                    SearchBeaconSearchService.shouldRefundOfferedItems(unknownResult),
                    "SearchBeacon should not refund items when it found an unknown structure"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(knownOnlyResult),
                    "SearchBeacon should refund items when it only found known structures"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(new SearchBeaconSearchService.SearchResult(List.of())),
                    "SearchBeacon should refund items when it found nothing"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(null),
                    "SearchBeacon should refund items before any result is available"
            );
        });
    }
    static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        var deskPos = new BlockPos(1, 1, 0);
        var absoluteDeskPos = helper.absolutePos(deskPos);
        helper.setBlock(deskPos, BlockRegistry.APPRENTICE_DESK.get());
        helper.getLevel().setDayTime(2000L);

        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        villager.setVillagerData(new VillagerData(villager.getVillagerData().getType(), VillagerProfession.NONE, 1));
        villager.refreshBrain(helper.getLevel());
        villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(helper.getLevel().dimension(), absoluteDeskPos));

        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isAlive(), "Villager died before claiming the Apprentice Desk");
            helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfessionRegistry.ERRAND_MAGE.get(),
                    "Villager did not become Errand Mage: " + villager.getVillagerData().getProfession());
            var jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
            helper.assertTrue(jobSite != null, "Villager did not store a job site");
            helper.assertTrue(jobSite != null && absoluteDeskPos.equals(jobSite.pos()),
                    "Villager claimed unexpected job site: " + jobSite);
        });
    }
    static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var templatePoolRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);

            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/plains/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/desert/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/savanna/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/snowy/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/snowy/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/taiga/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/taiga/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    3
            );
        });
    }
    static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var structureTemplateManager = helper.getLevel().getStructureManager();
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/plains/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/desert/villagers"),
                    new BlockPos(2, 1, 4)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/savanna/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/snowy/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/snowy/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/taiga/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/taiga/villagers"),
                    new BlockPos(3, 1, 5)
            );

            assertErrandMageHouseLootTableExists(helper);
        });
    }

    static void randomSpellImbueCreatesLockedLevelOneWand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wand = new ItemStack(ItemRegistry.WOODEN_WAND.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    wand,
                    List.of(spell.getSpellResource()),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(1L)
            );

            helper.assertTrue(result.is(ItemRegistry.WOODEN_WAND.get()),
                    "Random spell imbue unexpectedly used its fallback: " + result);
            helper.assertTrue(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue did not create a spell container");
            var spellContainer = ISpellContainer.get(result);
            helper.assertTrue(spellContainer != null,
                    "Random spell imbue did not expose its spell container");
            if (spellContainer == null) {
                return;
            }
            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellContainer.getMaxSpellCount() == 1
                            && spellContainer.getActiveSpellCount() == 1
                            && !spellContainer.isSpellWheel()
                            && !spellContainer.mustEquip(),
                    "Random spell imbue created an unexpected spell container: " + spellContainer);
            helper.assertTrue(spellData.getSpell() == spell
                            && spellData.getLevel() == 1
                            && spellData.isLocked(),
                    "Random spell imbue did not create a locked level one spell: " + spellData);
        });
    }

    static void randomSpellImbueUsesOnlyConfiguredEnabledSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var allowedSpells = List.of(magicMissile, heal);
            var spellIds = allowedSpells.stream().map(AbstractSpell::getSpellResource).toList();
            var random = RandomSource.create(2L);

            for (var attempt = 0; attempt < 32; attempt++) {
                var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                        new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                        spellIds,
                        ItemRegistry.CRUDE_INK.get(),
                        random
                );
                helper.assertTrue(result.is(ItemRegistry.WOODEN_WAND.get()),
                        "Random spell imbue unexpectedly used its fallback: " + result);
                var spellContainer = ISpellContainer.get(result);
                var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
                helper.assertTrue(spellData != SpellData.EMPTY && allowedSpells.contains(spellData.getSpell()),
                        "Random spell imbue selected a spell outside its configured candidates: " + spellData);
                helper.assertTrue(spellData.getSpell().isEnabled(),
                        "Random spell imbue selected a disabled spell: " + spellData);
            }
        });
    }

    static void randomSpellImbueFallsBackWhenNoSpellIsEligible(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                    List.of(),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(3L)
            );

            helper.assertTrue(result.is(ItemRegistry.CRUDE_INK.get()),
                    "Random spell imbue did not use Crude Ink for an empty candidate list: " + result);
            helper.assertFalse(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue copied a spell container to its fallback item");
        });
    }

    static void randomSpellImbueFallsBackForUnknownSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                    List.of(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "missing_spell")),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(4L)
            );

            helper.assertTrue(result.is(ItemRegistry.CRUDE_INK.get()),
                    "Random spell imbue did not use Crude Ink for an unknown spell id: " + result);
            helper.assertFalse(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue copied a spell container to its fallback item for an unknown spell id");
        });
    }

    static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var taggedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            taggedScroll.getOrCreateTag().putString("apprenticecodex_test", "tagged");
            var taggedScrollCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            taggedScrollCost.getOrCreateTag().putString("apprenticecodex_test", "cost");
            var scrollOffer = new net.minecraft.world.item.trading.MerchantOffer(
                    taggedScrollCost,
                    new ItemStack(Items.EMERALD, 16),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    5,
                    0.05F
            );
            helper.assertTrue(scrollOffer.satisfiedBy(taggedScroll, new ItemStack(Items.EMERALD, 16)),
                    "Tagged scroll should satisfy the errand mage ink trade even when the saved cost stack has tags");

            var trades = createEmptyVillagerTrades();
            ErrandMageVillagerTradesEvent.onVillagerTrades(
                    new VillagerTradesEvent(trades, VillagerProfessionRegistry.ERRAND_MAGE.get())
            );
            var tarnishedCrownOffer = createOffers(trades.get(4), 0L).stream()
                    .filter(offer -> offer.getBaseCostA().is(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(tarnishedCrownOffer != null, "Errand Mage level 4 should buy Tarnished Crowns");
            helper.assertTrue(ErrandMageTradeManager.shouldIgnorePaymentTags(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()),
                    "Errand Mage Tarnished Crown trade should ignore payment tags");
            var taggedTarnishedCrown = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            taggedTarnishedCrown.getOrCreateTag().putString("apprenticecodex_test", "tagged");
            helper.assertTrue(tarnishedCrownOffer.satisfiedBy(taggedTarnishedCrown, ItemStack.EMPTY),
                    "Tagged Tarnished Crown should satisfy the current errand mage buy trade");
        });
    }
    static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var trades = createEmptyVillagerTrades();
            ErrandMageVillagerTradesEvent.onVillagerTrades(
                    new VillagerTradesEvent(trades, VillagerProfessionRegistry.ERRAND_MAGE.get())
            );

            var level1Offers = createOffers(trades.get(1), 0L);
            helper.assertFalse(hasBaseCostItem(level1Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.FROZEN_BONE_SHARD.get()),
                    "Errand Mage level 1 trades should not buy Frozen Bone Shards");
            assertContainsOffer(helper, level1Offers,
                    new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 32),
                    ItemStack.EMPTY,
                    new ItemStack(Items.EMERALD),
                    16,
                    "Errand Mage level 1 trades should buy Rapid Spellcaster Rounds");

            var level2Listings = trades.get(2);
            var exclusiveItems = new LinkedHashSet<Item>();
            var level2Offers = createOffers(level2Listings, 0L);
            helper.assertTrue(countBaseCostItems(level2Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get(),
                            io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_PEARL.get()) == 1,
                    "Errand Mage level 2 trades should choose either Shriving Stone or Divine Pearl");
            var exclusiveRandom = RandomSource.create(0L);
            var exclusiveListing = level2Listings.get(0);
            for (var attempt = 0; attempt < 64 && exclusiveItems.size() < 2; attempt++) {
                var offer = exclusiveListing.getOffer(null, exclusiveRandom);
                helper.assertTrue(offer != null, "Errand Mage level 2 exclusive trade should create an offer");
                exclusiveItems.add(offer.getBaseCostA().getItem());
            }
            helper.assertTrue(exclusiveItems.size() == 2,
                    "Errand Mage level 2 exclusive trade should be able to choose both configured items");
            assertContainsOffer(helper, level2Offers,
                    new ItemStack(Items.EMERALD),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 8),
                    12,
                    "Errand Mage level 2 trades should always sell Basic Spellcaster Rounds");

            assertContainsOffer(helper, createOffers(trades.get(4), 0L),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()),
                    ItemStack.EMPTY,
                    new ItemStack(Items.EMERALD, 2),
                    16,
                    "Errand Mage level 4 trades should buy Tarnished Crowns for two Emeralds");
            assertContainsOffer(helper, createOffers(trades.get(4), 0L),
                    new ItemStack(Items.EMERALD, 32),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()),
                    12,
                    "Errand Mage level 4 trades should sell Spellstained Arcane Ingots");
            assertContainsOffer(helper, createOffers(trades.get(5), 0L),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    new ItemStack(Items.EMERALD, 16),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    "Errand Mage level 5 trades should sell Common Ink for Scrolls");
            assertContainsOffer(helper, createOffers(trades.get(5), 0L),
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.WRITABLE_BOOK),
                    new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()),
                    3,
                    "Errand Mage level 5 trades should sell Isekai Travel Guidebooks");
        });
    }
    static void customRecipeDataIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_transfer"),
                    RecipeRegistry.SPELLCASTERS_FLASK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"),
                    RecipeRegistry.SPELLCASTERS_FLASK_EXTRACT_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"),
                    RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get(), net.minecraft.world.item.crafting.RecipeType.SMITHING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"),
                    RecipeRegistry.ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER.get(), net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_cane_lodestone_bind"),
                    RecipeRegistry.EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"),
                    RecipeRegistry.EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "comfort_sandwich"),
                    net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE,
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            var guidebookRecipeId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "isekai_travel_guidebook");
            helper.assertTrue(recipeManager.byKey(guidebookRecipeId).isEmpty(),
                    "Isekai Travel Guidebook crafting recipe should not be loaded: " + guidebookRecipeId);

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder"),
                    RecipeRegistry.ESSENCE_SMOKER_SERIALIZER.get(), RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone"),
                    RecipeRegistry.GRIND_RUNNER_SERIALIZER.get(), RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/multi_purpose_spell_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/multi_purpose_spell_round_recycle"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemist_cauldron/brew_isekai_travel_guidebook_to_common_ink"),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_SERIALIZER.get(),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get());

            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).isEmpty(),
                    "No Essence Smoker recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).isEmpty(),
                    "No Grind Runner recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).isEmpty(),
                    "No Spellcaster Workbench recipes were loaded");

            if (ModList.get().isLoaded(CREATE_MOD_ID)) {
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_bullet_mold"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_shaped"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_casing_mold"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_shaped"));
                assertShapedRecipeCenterIngredientMatches(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_casing_mold"),
                        new ItemStack(ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()),
                        new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()),
                        "Spell Casing Mold recipe should use the empty casing tag");
                assertShapedRecipeCenterIngredientMatches(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_bullet_mold"),
                        new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()),
                        new ItemStack(ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get()),
                        "Spell Bullet Mold recipe should use Rapid Spellcaster Round");
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/mixing/arcane_propellant_charge"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "mixing"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/crushing/crystalline_arcane_shard"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "crushing"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/haunting/comfort_berries"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "haunting"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/spell_bullet_head"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_rapid_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_basic_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_arcane_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_advanced_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_spell_dominator_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_multi_purpose_spell_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/rapid_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/basic_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/arcane_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/advanced_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/spell_dominator_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/multi_purpose_spell_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
            }
        });
    }

    static void spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var invokeCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_INVOKE_CARD.get();
            var autonomyCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_AUTONOMY_CARD.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var raiseDead = io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get();

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false),
                    requireSpellId(raiseDead), remotePlayerGeometryProfile(false)
            ))) {
                helper.assertTrue(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should accept RemoteOwner profile spells");
                helper.assertTrue(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should accept RemoteOwner profile spells");
                helper.assertFalse(invokeCard.canImbueSpell(raiseDead, 1),
                        "Spell Invoke Card should reject recast spells when the profile does not allow initial recast");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(raiseDead), remotePlayerGeometryProfile(true)
            ))) {
                helper.assertTrue(invokeCard.canImbueSpell(raiseDead, 1),
                        "Spell Invoke Card should accept summon recasts controlled by SummonedEntitiesCastData");
                helper.assertTrue(autonomyCard.canImbueSpell(raiseDead, 1),
                        "Spell Autonomy Card should accept summon recasts controlled by SummonedEntitiesCastData");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of())) {
                helper.assertFalse(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should reject spells without a supported impact profile");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false)
            )); var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                    true,
                    List.of(mageLight.getSpellResource().toString())
            )) {
                helper.assertTrue(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should ignore runtime RemoteOwner denylist during Imbue");
                helper.assertTrue(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should ignore runtime RemoteOwner denylist during Imbue");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of());
                 var ignoredDispenserProfiles = SpellDispenserSpellProfileManager.useProfilesForGameTest(Map.of(
                         requireSpellId(mageLight), SpellDispenserSpellProfile.DEFAULT
                 ))) {
                helper.assertFalse(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should not use Spell Dispenser profiles as fallback");
                helper.assertFalse(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should not use Spell Dispenser profiles as fallback");
            }
        });
    }

    static void spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            helper.assertTrue(new ItemStack(Items.PAPER).is(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                    "Paper should be registered as a Spell Throwable Card paper ingredient");
            helper.assertTrue(new ItemStack(Items.BLACK_DYE).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Black dye should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.INK_SAC).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Ink sac should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.GLOW_INK_SAC).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Glow ink sac should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.ENDER_EYE).is(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                    "Eye of ender should be registered as a Spell Autonomy Card material");

            try (var ignored = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false)
            ))) {
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.BLACK_DYE),
                        16,
                        mageLight,
                        "Spell Invoke Card paper recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.INK_SAC),
                        16,
                        mageLight,
                        "Spell Invoke Card ink sac recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.GLOW_INK_SAC),
                        16,
                        mageLight,
                        "Spell Invoke Card glow ink sac recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), 16),
                        new ItemStack(Items.BLACK_DYE),
                        16,
                        mageLight,
                        "Spell Invoke Card rewrite recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(Items.PAPER, 8),
                        new ItemStack(Items.ENDER_EYE),
                        8,
                        mageLight,
                        "Spell Autonomy Card paper recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), 8),
                        new ItemStack(Items.ENDER_EYE),
                        8,
                        mageLight,
                        "Spell Autonomy Card rewrite recipe"
                );
                assertSpellThrowableCardWorkbenchButtonAcceptsSplitStacks(helper, mageLight);
                assertSpellThrowableCardWorkbenchButtonAppendsToActiveDynamicRecipe(helper, mageLight);
            }

            try (var configOverride = ApprenticeCodexServerConfig.useSpellThrowableCardConfigOverrideForGameTest(
                    new SpellThrowableCardServerConfig.Values(5, 3));
                 var ignored = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                         requireSpellId(mageLight), remotePlayerGeometryProfile(false)
                 ))) {
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 5),
                        new ItemStack(Items.BLACK_DYE),
                        5,
                        mageLight,
                        "Spell Invoke Card configured count recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(Items.PAPER, 3),
                        new ItemStack(Items.ENDER_EYE),
                        3,
                        mageLight,
                        "Spell Autonomy Card configured count recipe"
                );
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of())) {
                assertSpellThrowableCardWorkbenchCantImbue(
                        helper,
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.BLACK_DYE),
                        createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                        "Spell Invoke Card should show a blocked result for unsupported scrolls"
                );
            }
        });
    }

    static void assertSpellThrowableCardWorkbenchRecipe(
            GameTestHelper helper,
            Item resultItem,
            ItemStack baseStack,
            ItemStack catalystStack,
            int expectedCount,
            AbstractSpell spell,
            String context
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), context.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
        var scrollStack = createSpellScroll(spell);
        var menu = createSpellcasterWorkbenchMenuWithInputs(player, baseStack.copy(), catalystStack.copy(), scrollStack.copy());

        var preview = menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(preview.is(resultItem) && preview.getCount() == expectedCount,
                context + " should preview the expected card stack: " + preview);
        assertStackHasSpell(helper, preview, spell, 1, context + " preview should be imbued");

        var crafted = menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
        helper.assertTrue(crafted.is(resultItem) && crafted.getCount() == expectedCount,
                context + " should craft the expected card stack: " + crafted);
        assertStackHasSpell(helper, crafted, spell, 1, context + " result should be imbued");
        helper.assertTrue(countInputItem(menu, baseStack.getItem()) == 0,
                context + " should consume the base stack");
        helper.assertTrue(countInputItem(menu, catalystStack.getItem()) == 0,
                context + " should consume the catalyst");
        helper.assertTrue(hasMatchingInputStack(menu, scrollStack),
                context + " should leave the scroll in the Workbench inputs");
    }

    static void assertSpellThrowableCardWorkbenchButtonAcceptsSplitStacks(GameTestHelper helper, AbstractSpell spell) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_invoke_card_split_stack_button");
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        player.getInventory().setItem(0, new ItemStack(Items.PAPER, 8));
        player.getInventory().setItem(1, new ItemStack(Items.PAPER, 8));
        player.getInventory().setItem(2, new ItemStack(Items.BLACK_DYE));
        player.getInventory().setItem(3, createSpellScroll(spell));

        helper.assertTrue(menu.clickMenuButton(player, findSelectableIconIndex(helper, menu, ItemRegistry.SPELL_INVOKE_CARD.get())),
                "Spell Invoke Card button should accept split paper stacks");
        var preview = menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(preview.is(ItemRegistry.SPELL_INVOKE_CARD.get()) && preview.getCount() == 16,
                "Spell Invoke Card button should preview from split paper stacks: " + preview);
        helper.assertTrue(countInputItem(menu, Items.PAPER) == 16,
                "Spell Invoke Card button should move both split paper stacks into inputs");
    }

    static void assertSpellThrowableCardWorkbenchButtonAppendsToActiveDynamicRecipe(GameTestHelper helper, AbstractSpell spell) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_invoke_card_append_button");
        var scrollStack = createSpellScroll(spell);
        var menu = createSpellcasterWorkbenchMenuWithInputs(
                player,
                new ItemStack(Items.PAPER, 16),
                new ItemStack(Items.BLACK_DYE),
                scrollStack.copy()
        );
        player.getInventory().setItem(0, new ItemStack(Items.PAPER, 16));
        player.getInventory().setItem(1, new ItemStack(Items.BLACK_DYE));

        helper.assertTrue(menu.clickMenuButton(player, findSelectableIconIndex(helper, menu, ItemRegistry.SPELL_INVOKE_CARD.get())),
                "Spell Invoke Card button should append to the active dynamic recipe");
        helper.assertTrue(countInputItem(menu, Items.PAPER) == 32,
                "Spell Invoke Card button should append one paper batch without returning existing inputs");
        helper.assertTrue(countInputItem(menu, Items.BLACK_DYE) == 2,
                "Spell Invoke Card button should append one catalyst batch without returning existing inputs");
        helper.assertTrue(hasMatchingInputStack(menu, scrollStack),
                "Spell Invoke Card button should keep the existing scroll input while appending");
    }

    static int findSelectableIconIndex(GameTestHelper helper, SpellcasterWorkbenchMenu menu, Item item) {
        var icons = menu.getSelectableIcons();
        for (var index = 0; index < icons.size(); ++index) {
            if (icons.get(index).is(item)) {
                return index;
            }
        }
        helper.fail("Missing Spellcaster Workbench selectable icon for " + item);
        return -1;
    }

    static void assertSpellThrowableCardWorkbenchCantImbue(
            GameTestHelper helper,
            ItemStack baseStack,
            ItemStack catalystStack,
            ItemStack scrollStack,
            String context
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), context.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
        var menu = createSpellcasterWorkbenchMenuWithInputs(player, baseStack, catalystStack, scrollStack);
        helper.assertTrue(menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                context + ": result slot should stay empty");
        helper.assertTrue(menu.isBlockedBySpellThrowableCardCantImbue(),
                context + ": menu should expose the card imbue block reason");
    }

    static int countInputItem(SpellcasterWorkbenchMenu menu, Item item) {
        var count = 0;
        for (var slotIndex = 0; slotIndex < SpellcasterWorkbenchMenu.INPUT_SLOT_COUNT; ++slotIndex) {
            var stack = menu.getSlot(slotIndex).getItem();
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static boolean hasMatchingInputStack(SpellcasterWorkbenchMenu menu, ItemStack expectedStack) {
        for (var slotIndex = 0; slotIndex < SpellcasterWorkbenchMenu.INPUT_SLOT_COUNT; ++slotIndex) {
            if (ItemStack.isSameItemSameTags(menu.getSlot(slotIndex).getItem(), expectedStack)) {
                return true;
            }
        }
        return false;
    }

    static RemoteOwnerCastProfile remotePlayerGeometryProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    static ResourceLocation requireSpellId(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        if (spellId == null) {
            throw new IllegalStateException("Missing spell id for " + spell);
        }
        return spellId;
    }

    static void processingRecipeDenylistsRejectConfiguredRecipeIds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();
            var spellcasterWorkbenchRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round");
            var essenceSmokerRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder");
            var grindRunnerRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone");
            var heavenlyFistCreateRecipeId = ResourceLocation.fromNamespaceAndPath(
                    CREATE_MOD_ID, "pressing/sugar_cane");
            var deniedBlastingRecipeId = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "iron_ingot_from_blasting_iron_ore");
            var fallbackSmeltingRecipeId = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "iron_ingot_from_smelting_iron_ore");

            try (var ignored = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                    List.of(spellcasterWorkbenchRecipeId.toString()),
                    List.of(essenceSmokerRecipeId.toString()),
                    List.of(grindRunnerRecipeId.toString()),
                    List.of(heavenlyFistCreateRecipeId.toString()),
                    List.of(deniedBlastingRecipeId.toString())
            )) {
                var spellcasterWorkbenchRecipe = recipeManager
                        .getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.getId().equals(spellcasterWorkbenchRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(spellcasterWorkbenchRecipe != null, "Missing Spellcaster Workbench test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(spellcasterWorkbenchRecipe),
                        "Spellcaster Workbench denylist did not reject configured recipe");

                var essenceSmokerRecipe = recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.getId().equals(essenceSmokerRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(essenceSmokerRecipe != null, "Missing Essence Smoker test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(essenceSmokerRecipe),
                        "Essence Smoker denylist did not reject configured recipe");

                var grindRunnerRecipe = recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.getId().equals(grindRunnerRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(grindRunnerRecipe != null, "Missing Grind Runner test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(grindRunnerRecipe),
                        "Grind Runner denylist did not reject configured recipe");

                helper.assertTrue(ApprenticeCodexServerConfig.isHeavenlyFistCreateRecipeDenied(heavenlyFistCreateRecipeId),
                        "Heavenly Fist Create denylist did not reject configured recipe");

                var ironOreInput = new SimpleContainer(new ItemStack(Items.IRON_ORE));
                var thermalProcessRecipe = ProcessingRecipeDenylist.findThermalProcessRecipe(
                        recipeManager, ironOreInput, helper.getLevel()
                );
                helper.assertTrue(thermalProcessRecipe.isPresent(),
                        "Thermal Process denylist should fall back to an allowed cooking recipe");
                helper.assertFalse(thermalProcessRecipe.get().getId().equals(deniedBlastingRecipeId),
                        "Thermal Process selected a denied blasting recipe");
                helper.assertTrue(thermalProcessRecipe.get().getId().equals(fallbackSmeltingRecipeId),
                        "Thermal Process fallback recipe mismatch: " + thermalProcessRecipe.get().getId());
            }
        });
    }

    static void heavenlyFistCreatePressingDenylistLeavesDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var deniedRecipeId = ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "pressing/sugar_cane");
        var override = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                List.of(),
                List.of(),
                List.of(),
                List.of(deniedRecipeId.toString()),
                List.of()
        );
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.SUGAR_CANE)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getDepotItem", level, helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.SUGAR_CANE),
                        "Heavenly Fist should leave denied Create pressing inputs untouched: " + result);
                helper.succeed();
            } finally {
                override.close();
            }
        });
    }

    static void grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.AMETHYST_CLUSTER));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(Items.AMETHYST_SHARD),
                "Grind Runner should process Create crushing recipes without Craftsman's Delight"
        ));
    }

    static void grindRunnerProcessesCreateMillingRecipes(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var wheatFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "wheat_flour"));
        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.WHEAT));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(wheatFlour),
                "Grind Runner should process Create milling recipes after crushing misses"
        ));
    }

    static void grindRunnerPrefersCreateCrushingBeforeMilling(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.GRAVEL));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(Items.SAND),
                "Grind Runner should prefer Create crushing over Create milling for overlapping inputs"
        ));
    }

    static void grindRunnerProcessesCreateDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(2, 1, 0);
        var harness = startGrindRunnerCreateBlockProcess(helper, targetPos);
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(targetPos),
                new ItemStack(Items.GRAVEL)
        );

        helper.runAtTickTime(12, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getDepotItem", helper.getLevel(), helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.SAND), "Grind Runner should process Create Depot items: " + result);
            } finally {
                harness.discard();
            }
            helper.succeed();
        });
    }

    static void grindRunnerLeavesCreateChuteItemsUnprocessed(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos.below(), Blocks.STONE);
        var harness = startGrindRunnerCreateBlockProcess(helper, targetPos);
        invokeCreateGameTestHookVoid(
                "placeChuteWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(targetPos),
                new ItemStack(Items.GRAVEL)
        );

        helper.runAtTickTime(12, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getChuteItem", helper.getLevel(), helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.GRAVEL), "Grind Runner should not process enclosed Create Chute items: " + result);
            } finally {
                harness.discard();
            }
            helper.succeed();
        });
    }

    private static GrindRunnerProcessHarness startSingleGrindRunnerItemProcess(GameTestHelper helper, ItemStack inputStack) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "grind_runner_processing_test");
        helper.getLevel().addFreshEntity(owner);
        var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), helper.getLevel(), owner);
        wheel.setGrindItemPerSecond(20.0F);
        helper.getLevel().addFreshEntity(wheel);

        var itemEntity = spawnNoGravityItem(helper, new BlockPos(2, 2, 0), inputStack.copyWithCount(1));
        return new GrindRunnerProcessHarness(owner, wheel, itemEntity);
    }

    private static GrindRunnerCreateBlockHarness startGrindRunnerCreateBlockProcess(GameTestHelper helper, BlockPos targetPos) {
        var owner = createEquipmentTestPlayer(helper, targetPos.above(), "grind_runner_create_block_processing_test");
        helper.getLevel().addFreshEntity(owner);
        var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), helper.getLevel(), owner);
        wheel.setGrindItemPerSecond(20.0F);
        helper.getLevel().addFreshEntity(wheel);
        return new GrindRunnerCreateBlockHarness(owner, wheel);
    }

    private static void assertProcessedGrindRunnerOutput(
            GameTestHelper helper,
            GrindRunnerProcessHarness harness,
            Predicate<ItemStack> outputPredicate,
            String message
    ) {
        var result = harness.itemEntity().isAlive() ? harness.itemEntity().getItem().copy() : ItemStack.EMPTY;
        try {
            helper.assertTrue(outputPredicate.test(result), message + ": " + result);
        } finally {
            harness.discard();
        }
        helper.succeed();
    }

    private record GrindRunnerProcessHarness(FakePlayer owner, GrindRunnerWheelEntity wheel, ItemEntity itemEntity) {
        private void discard() {
            itemEntity.discard();
            wheel.discard();
            owner.discard();
        }
    }

    private record GrindRunnerCreateBlockHarness(FakePlayer owner, GrindRunnerWheelEntity wheel) {
        private void discard() {
            wheel.discard();
            owner.discard();
        }
    }

    private static void invokeCreateGameTestHookVoid(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            hookClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: " + methodName, exception);
        }
    }

    private static ItemStack invokeCreateGameTestHookItemStack(String methodName, ServerLevel level, BlockPos pos) {
        return invokeCreateGameTestHookItemStack(
                methodName,
                new Class<?>[]{ServerLevel.class, BlockPos.class},
                level,
                pos
        );
    }

    private static ItemStack invokeCreateGameTestHookItemStack(
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            var result = hookClass.getMethod(methodName, parameterTypes).invoke(null, args);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: " + methodName, exception);
        }
    }

    private static void placeCreateBasinWithItems(ServerLevel level, BlockPos pos, ItemStack... stacks) {
        invokeCreateGameTestHookVoid(
                "placeBasinWithItems",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack[].class},
                level,
                pos,
                stacks
        );
    }

    private static int getCreateBasinItemCount(ServerLevel level, BlockPos pos, ItemStack prototype) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            var result = hookClass.getMethod("getBasinItemCount", ServerLevel.class, BlockPos.class, ItemStack.class)
                    .invoke(null, level, pos, prototype);
            return result instanceof Integer count ? count : 0;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: getBasinItemCount", exception);
        }
    }

    static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var emptyFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Spellcaster's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Spellcaster's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Spellcaster's Flask rejected a lingering potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, splashPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a splash potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, lingeringPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a lingering potion");
        });
    }
    static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
                    PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION),
                    1,
                    0
            );
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_drink_test"));

            var result = flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            helper.assertTrue(result.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Drinking the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(result) == 0,
                    "Drinking the last dose did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(result));
            helper.assertTrue(SpellcastersFlask.getStoredItem(result).isEmpty(),
                    "Drinking the last dose left StoredItem behind");
            helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                    "Drinking the flask did not apply the stored potion effect");
        });
    }
    static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var flask = createFilledSpellcastersFlask(storedPotion, 1, 2);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_glow_tradeoff_test"));

            flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            var appliedEffect = player.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(appliedEffect != null, "Drinking the flask did not apply regeneration");
            helper.assertTrue(appliedEffect != null && appliedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Glow Energy should still amplify drunk flask effects");
            helper.assertTrue(appliedEffect != null
                            && appliedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * (1.0F / 3.0F))),
                    "Glow Energy should reduce drunk flask duration by 1 / (1 + level)");
        });
    }
    static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var twoDoseFlask = createFilledSpellcastersFlask(storedPotion, 2, 0);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_mismatch_drink_test"));

            twoDoseFlask.getItem().finishUsingItem(twoDoseFlask, helper.getLevel(), player);

            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(twoDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should consume two doses");
            helper.assertTrue(SpellcastersFlask.getStoredItem(twoDoseFlask).isEmpty(),
                    "Mismatched Spellcaster's Flask should clear StoredItem when extra consumption empties it");

            var oneDoseFlask = createFilledSpellcastersFlask(storedPotion, 1, 0);
            oneDoseFlask.getItem().finishUsingItem(oneDoseFlask, helper.getLevel(), player);
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(oneDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should still work with one remaining dose");
        });
    }
    static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );

            var emptiedFlask = SpellcastersFlask.copyAfterExtractingDoses(flask, 1);

            helper.assertTrue(emptiedFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Extracting the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(emptiedFlask) == 0,
                    "Batch extraction did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(emptiedFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(emptiedFlask).isEmpty(),
                    "Batch extraction left StoredItem behind");
        });
    }
    static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var flask = createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var craftingContainer = createCraftingContainer(flask, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should match a filled flask and glass bottle");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result,
                            createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get())),
                    "Spellcaster's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Spellcaster's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Spellcaster's Flask extract recipe left StoredItem behind");
        });
    }
    static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Alchemist's Flask did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Alchemist's Flask spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Alchemist's Flask spell slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Alchemist's Flask should not expose the imbued spell in the spell wheel");

            var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != SpellData.EMPTY, "Alchemist's Flask has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.EXTRACT.get(),
                    "Alchemist's Flask preset spell mismatch: " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
            helper.assertTrue(spellData.getLevel() == 1,
                    "Alchemist's Flask preset spell level mismatch: " + (spellData == SpellData.EMPTY ? -1 : spellData.getLevel()));
            helper.assertTrue(!spellData.canRemove(), "Alchemist's Flask preset spell should stay locked by default");
        });
    }
    static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Alchemist's Flask should allow instant spell imbuing");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.COMPOUND_PHIAL.get(), 1),
                    "Alchemist's Flask should allow long spell imbuing");
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Alchemist's Flask should allow continuous spell imbuing");
        });
    }
    static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var emptyFlask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Alchemist's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Alchemist's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Alchemist's Flask rejected a lingering potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, simpleElixir),
                    "Alchemist's Flask rejected a Simple Elixir");
        });
    }
    static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            assertTooltipKeyUsesColor(
                    helper,
                    createFilledSpellcastersFlask(splashPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Spellcaster's Flask should warn for stored splash potions"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    createFilledAlchemistsFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Alchemist's Flask should warn for stored regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    createFilledSpellcastersFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Spellcaster's Flask should not warn for regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Alchemist's Flask should not warn for Simple Elixir"
            );
        });
    }
    static void flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should auto-fill an empty Spellcaster's Flask with supported splash potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should auto-fill an empty Alchemist's Flask with supported regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should auto-fill an empty Spellcaster's Flask with regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should auto-fill an empty Alchemist's Flask with splash potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            simpleElixir),
                    "Atelier Station should keep non-vanilla Alchemist's Flask items outside mismatch penalties");
        });
    }
    static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 16,
                    "Alchemist's Flask base capacity mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));

            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                flask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
                helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 20,
                        "Alchemist's Flask Large Mug bonus mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));
            }

            var filledFlask = SpellcastersFlask.copyWithAddedDoses(flask, splashPotion, 16);
            helper.assertTrue(!filledFlask.isEmpty(), "Alchemist's Flask failed to store sixteen splash potion doses");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(filledFlask) == 16,
                    "Alchemist's Flask stored dose count mismatch: " + SpellcastersFlask.getStoredDoseCount(filledFlask));

            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var craftingContainer = createCraftingContainer(
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()), splashPotion, 1),
                    new ItemStack(Items.GLASS_BOTTLE)
            );

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should accept Alchemist's Flask");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result, splashPotion),
                    "Alchemist's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Alchemist's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Alchemist's Flask extract recipe left StoredItem behind");
        });
    }
    static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"))
                    .orElseThrow();

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var spellcastersFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
            if (EnchantmentRegistry.GUZZLE.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GUZZLE.get(), 2);
            }
            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
            }
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            var filledSpellcastersFlask = SpellcastersFlask.copyWithAddedDoses(spellcastersFlask, normalPotion, 2);
            filledSpellcastersFlask = SpellcastersFlask.copyWithToggledEffectParticles(filledSpellcastersFlask);
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(filledSpellcastersFlask),
                    "Spellcaster's Flask test input should start with suppressed particles");
            var smithingContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    filledSpellcastersFlask,
                    new ItemStack(Items.GUNPOWDER)
            );

            helper.assertTrue(recipe.matches(smithingContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept a filled Spellcaster's Flask");

            var convertedFlask = recipe.assemble(smithingContainer, helper.getLevel().registryAccess());
            helper.assertTrue(convertedFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask smithing recipe returned the wrong result item");
            helper.assertTrue(ISpellContainer.isSpellContainer(convertedFlask),
                    "Alchemist's Flask smithing recipe should preserve the preset spell container");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(convertedFlask) == 2,
                    "Alchemist's Flask smithing recipe should preserve stored dose count");

            var convertedStoredItem = SpellcastersFlask.getStoredItem(convertedFlask);
            helper.assertTrue(convertedStoredItem.is(Items.POTION),
                    "Filled Spellcaster's Flask should keep a regular potion as a mismatch-usable potion");
            helper.assertTrue(PotionUtils.getPotion(convertedStoredItem) == PotionUtils.getPotion(normalPotion),
                    "Converted Alchemist's Flask should keep the original potion type");
            helper.assertTrue(!SpellcastersFlask.isEffectParticlesSuppressed(convertedFlask),
                    "Alchemist's Flask smithing recipe should reset suppressed particles");

            var convertedEnchantments = EnchantmentHelper.getEnchantments(convertedFlask);
            helper.assertTrue(!EnchantmentRegistry.GUZZLE.isPresent()
                            || !convertedEnchantments.containsKey(EnchantmentRegistry.GUZZLE.get()),
                    "Alchemist's Flask smithing recipe should drop only Guzzle");
            helper.assertTrue(!EnchantmentRegistry.LARGE_MUG.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.LARGE_MUG.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Large Mug");
            helper.assertTrue(!EnchantmentRegistry.RED_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.RED_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Red Energy");
            helper.assertTrue(!EnchantmentRegistry.GLOW_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.GLOW_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Glow Energy");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), simpleElixir, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept Simple Elixir");
            var simpleElixirResult = recipe.assemble(simpleElixirContainer, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.isSameItemSameTags(SpellcastersFlask.getStoredItem(simpleElixirResult), simpleElixir),
                    "Alchemist's Flask smithing recipe should keep Simple Elixir unchanged");

            var fireAle = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_ALE.get());
            var fireAleContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), fireAle, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(!recipe.matches(fireAleContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should reject unsupported stored items such as Fire Ale");
        });
    }
    static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"))
                    .orElseThrow();

            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashFlask = createFilledAlchemistsFlask(splashPotion, 2, 1);
            var splashContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    splashFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(splashContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a splash potion flask");

            var splashResult = recipe.assemble(splashContainer, helper.getLevel().registryAccess());
            var splashRemainingFlask = recipe.getRemainingItems(splashContainer).get(4);

            helper.assertTrue(splashResult.is(Items.TIPPED_ARROW) && splashResult.getCount() == 8,
                    "Alchemist's Flask tipped arrow recipe should return eight tipped arrows");
            helper.assertTrue(PotionUtils.getPotion(splashResult) == PotionUtils.getPotion(splashPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored splash potion");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(splashRemainingFlask) == 1,
                    "Alchemist's Flask tipped arrow recipe should consume exactly one dose");

            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var lingeringFlask = createFilledAlchemistsFlask(lingeringPotion, 1, 0);
            var lingeringContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    lingeringFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(lingeringContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a lingering potion flask");
            var lingeringResult = recipe.assemble(lingeringContainer, helper.getLevel().registryAccess());
            var lingeringRemainingFlask = recipe.getRemainingItems(lingeringContainer).get(4);

            helper.assertTrue(PotionUtils.getPotion(lingeringResult) == PotionUtils.getPotion(lingeringPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored lingering potion");
            helper.assertTrue(lingeringRemainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask tipped arrow recipe should return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(lingeringRemainingFlask) == 0,
                    "Alchemist's Flask tipped arrow recipe should empty the flask after the last dose");
            helper.assertTrue(SpellcastersFlask.getStoredItem(lingeringRemainingFlask).isEmpty(),
                    "Alchemist's Flask tipped arrow recipe should clear StoredItem after the last dose");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            helper.assertTrue(!recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should reject Simple Elixir");
        });
    }
    static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), PotionRegistry.INTELLIGENCE.get());
            var craftingContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(splashPotion, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            var craftedStack = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 8), PotionRegistry.INTELLIGENCE.get());
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/craft_tipped_arrow_by_flask")
            );

            helper.assertTrue(advancement != null, "Missing advancement for flask tipped arrow crafting");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.event.AlchemistsFlaskAdvancementEvent.shouldAward(craftedStack, craftingContainer),
                    "Crafting tipped arrows with Alchemist's Flask should satisfy the advancement award conditions"
            );
        });
    }
    static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_hand_test");
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var magicData = MagicData.getPlayerMagicData(player);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(splashPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when the main hand flask is filled");
            helper.assertTrue(magicData.getAdditionalCastData() instanceof jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData,
                    "Extract should store cast data for the selected flask");
            var mainCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(mainCastData.hand() == InteractionHand.MAIN_HAND,
                    "Extract should prefer the main hand filled flask");
            helper.assertTrue(ItemStack.isSameItemSameTags(mainCastData.storedItem(), splashPotion),
                    "Extract selected the wrong stored item from the main hand flask");

            magicData.setAdditionalCastData(null);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when only the offhand flask is filled");
            var offhandCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(offhandCastData.hand() == InteractionHand.OFF_HAND,
                    "Extract should fall back to the offhand filled flask when the main hand flask is empty");
            helper.assertTrue(ItemStack.isSameItemSameTags(offhandCastData.storedItem(), lingeringPotion),
                    "Extract selected the wrong stored item from the offhand flask");
        });
    }
    static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when no Alchemist's Flask is held");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when the only Alchemist's Flask is empty");
        });
    }
    static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_cast_projectile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a lingering potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var lingeringProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(lingeringProjectile.getItem().is(Items.LINGERING_POTION),
                    "Extract should throw a lingering potion when the flask stores a lingering potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 1,
                    "Extract should consume exactly one dose from the casting flask");

            lingeringProjectile.discard();
            magicData.setAdditionalCastData(null);

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(normalPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a force-splash normal potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var forcedSplashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(forcedSplashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw normal potion contents as a splash potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 0,
                    "Extract should consume two doses when force-splashing a normal potion");
            forcedSplashProjectile.discard();
            magicData.setAdditionalCastData(null);

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(simpleElixir, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a Simple Elixir cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var splashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(splashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw Simple Elixir contents as a splash potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getOffhandItem()) == 1,
                    "Extract should consume exactly one offhand dose after a successful cast");
        });
    }
    static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            flask = SpellcastersFlask.copyWithAddedDoses(flask, storedPotion, 1);

            var thrownPotion = jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 1);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var extractedEffect = PotionUtils.getMobEffects(thrownPotion).get(0);

            helper.assertTrue(thrownPotion.is(Items.SPLASH_POTION),
                    "Extract should preserve non-lingering contents as splash potions");
            helper.assertTrue(extractedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Extract should add both Glow Energy and Extract amplification bonuses");
            helper.assertTrue(extractedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * 1.25F * 0.5F)),
                    "Extract should keep spell-side amplify while Glow Energy halves the Red Energy adjusted duration");
        });
    }






































































    static void compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var owner = EntityType.SHEEP.create(level);
        helper.assertTrue(owner != null, "Failed to create Compound Phial owner target");
        owner.setNoAi(true);
        var ownerPos = helper.absoluteVec(new Vec3(2.0D, 2.0D, 3.0D));
        owner.moveTo(ownerPos.x, ownerPos.y, ownerPos.z, 0.0F, 0.0F);
        level.addFreshEntity(owner);

        var fullDamageTarget = EntityType.SHEEP.create(level);
        helper.assertTrue(fullDamageTarget != null, "Failed to create Compound Phial full-damage target");
        fullDamageTarget.setNoAi(true);
        var fullDamageTargetPos = helper.absoluteVec(new Vec3(3.2D, 2.0D, 2.5D));
        fullDamageTarget.moveTo(fullDamageTargetPos.x, fullDamageTargetPos.y, fullDamageTargetPos.z, 0.0F, 0.0F);
        level.addFreshEntity(fullDamageTarget);

        var falloffTarget = EntityType.SHEEP.create(level);
        helper.assertTrue(falloffTarget != null, "Failed to create Compound Phial falloff target");
        falloffTarget.setNoAi(true);
        var falloffTargetPos = helper.absoluteVec(new Vec3(4.7D, 2.0D, 2.5D));
        falloffTarget.moveTo(falloffTargetPos.x, falloffTargetPos.y, falloffTargetPos.z, 0.0F, 0.0F);
        level.addFreshEntity(falloffTarget);

        var ownerHealth = owner.getHealth();
        var fullDamageTargetHealth = fullDamageTarget.getHealth();
        var falloffTargetHealth = falloffTarget.getHealth();

        var projectilePos = helper.absoluteVec(new Vec3(2.5D, 4.0D, 2.5D));
        var projectile = new CompoundPhialProjectileEntity(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), level, owner);
        projectile.setPos(projectilePos.x, projectilePos.y, projectilePos.z);
        projectile.setDeltaMovement(0.0D, -0.8D, 0.0D);
        projectile.setDamage(4.0F);
        projectile.setSplashRadius(2.0F);
        projectile.setPotionColorRandom(level);
        level.addFreshEntity(projectile);

        helper.runAtTickTime(8, () -> {
            helper.assertTrue(projectile.isRemoved(), "Compound Phial projectile did not impact during the test");

            var ownerTaken = ownerHealth - owner.getHealth();
            var fullDamageTaken = fullDamageTargetHealth - fullDamageTarget.getHealth();
            var falloffTaken = falloffTargetHealth - falloffTarget.getHealth();

            helper.assertTrue(ownerTaken > 0.0F, "Compound Phial should still hit its owner inside the splash");
            helper.assertTrue(Math.abs(fullDamageTaken - ownerTaken) < 0.1F,
                    "Compound Phial full-damage band should apply equal damage to nearby targets: target="
                            + fullDamageTaken + ", owner=" + ownerTaken);
            helper.assertTrue(falloffTaken >= fullDamageTaken * 0.6F - 0.1F,
                    "Compound Phial falloff target should keep at least 60% damage: target="
                            + falloffTaken + ", full=" + fullDamageTaken);
            helper.assertTrue(falloffTaken < fullDamageTaken,
                    "Compound Phial falloff target should still take less than full-band damage");
            helper.assertTrue(Math.abs(falloffTaken - Math.round(falloffTaken)) > 0.05F,
                    "Compound Phial falloff damage should not be rounded to whole damage steps: " + falloffTaken);
            helper.succeed();
        });
    }
    static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 0), BlockRegistry.MAGE_LIGHT_TORCH.get(), BlockEntityRegistry.MAGE_LIGHT_TORCH.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 2, 0), BlockRegistry.WIZARDLAMP_LANTERN.get(), BlockEntityRegistry.WIZARDLAMP_LANTERN.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 0), BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.RIFT_HOLE.get(), BlockEntityRegistry.RIFT_HOLE.get());
            placeAndAssertBlockEntity(helper, new BlockPos(3, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 1), BlockRegistry.ESSENCE_SMOKER.get(), BlockEntityRegistry.ESSENCE_SMOKER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 1), BlockRegistry.ATELIER_STATION.get(), BlockEntityRegistry.ATELIER_STATION.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 1), BlockRegistry.SPELL_DISPENSER.get(), BlockEntityRegistry.SPELL_DISPENSER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(3, 1, 1), BlockRegistry.CREATIVE_SPELL_DISPENSER.get(), BlockEntityRegistry.SPELL_DISPENSER.get());
            var level = helper.getLevel();
            for (var entityEntry : EntityRegistry.ENTITIES.getEntries()) {
                var entity = entityEntry.get().create(level);
                helper.assertTrue(entity != null, "Entity instantiation failed: " + entityEntry.getId());
            }
        });
    }

    static void spellCalibrationBenchStoresScrollsOnGauntlet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_storage_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var firstScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(menu.getSlot(0).mayPlace(gauntlet), "Scrollcaster Gauntlet should be accepted in the gauntlet slot");
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)), "Non-gauntlet items should be rejected from the gauntlet slot");
            helper.assertTrue(!menu.getSlot(1).mayPlace(lesserUpgrade), "Adjustment slots should be disabled without a gauntlet");
            helper.assertTrue(lesserUpgrade.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES),
                    "Lesser spell slot upgrade should be tagged as a Scrollcaster Gauntlet slot upgrade");

            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(menu.isScrollSlotEnabled(0), "Scroll slot 0 should be enabled by default");
            helper.assertTrue(menu.isScrollSlotEnabled(3), "Scroll slot 3 should be enabled by default");
            helper.assertTrue(!menu.isScrollSlotEnabled(4), "Scroll slot 4 should be locked before adding an upgrade");
            helper.assertTrue(!menu.getSlot(8).mayPlace(firstScroll.copy()), "Locked scroll slots should reject insertion");

            menu.getSlot(1).set(lesserUpgrade);
            helper.assertTrue(menu.isScrollSlotEnabled(5), "One lesser slot upgrade should unlock six scroll slots");
            helper.assertTrue(!menu.isScrollSlotEnabled(6), "One lesser slot upgrade should not unlock the seventh scroll slot");
            helper.assertTrue(menu.getSlot(9).mayPlace(firstScroll.copy()), "Newly unlocked scroll slot should accept scrolls");
            menu.getSlot(9).set(firstScroll);

            menu.getSlot(1).set(ItemStack.EMPTY);
            helper.assertTrue(!menu.isScrollSlotEnabled(5), "Removing an upgrade should disable its extra scroll slots");
            helper.assertTrue(menu.getSlot(9).hasItem(), "Disabled scroll slot should keep its existing scroll");
            helper.assertTrue(menu.getSlot(9).mayPickup(player), "Disabled scroll slot should still allow pickup");
            helper.assertTrue(!menu.getSlot(9).mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())),
                    "Disabled scroll slot should reject new scroll insertion");
            helper.assertTrue(!ScrollcasterGauntlet.getCalibrationScroll(gauntlet, 5).isEmpty(),
                    "Scroll should be stored on the gauntlet NBT");

            menu.removed(player);
            helper.assertTrue(player.getInventory().contains(gauntlet),
                    "Closing the Spell Calibration Bench should return the gauntlet to the player");
        });
    }

    static void scrollcasterGauntletSelectedScrollDrivesImbuedSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            helper.assertTrue(gauntlet.getItem() instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Scrollcaster Gauntlet should be a UniqueItem to block Arcane Anvil imbue tooltips and normal imbue");
            assertTooltipKeyAt(helper, gauntlet, 0, "item.apprenticecodex.right_click_magic_weapon.desc",
                    "Scrollcaster Gauntlet should show offhand priority tooltip first");
            assertTooltipKeyAt(helper, gauntlet, 1, "item.apprenticecodex.right_click_magic_weapon.item_type",
                    "Scrollcaster Gauntlet should show offhand priority item type tooltip second");
            assertTooltipKeyAt(helper, gauntlet, 2, "item.apprenticecodex.scrollcaster_gauntlet.desc",
                    "Scrollcaster Gauntlet should show selected spell cast tooltip third");
            ScrollcasterGauntlet.refreshSelectedSpellContainer(gauntlet);
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet),
                    "Empty Scrollcaster Gauntlet should not expose a spell container");

            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 3, createSpellScroll(magicMissile));

            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == 3,
                    "First inserted scroll should become the selected gauntlet scroll");
            var selectionViews = ScrollcasterGauntlet.getSelectionViews(gauntlet);
            helper.assertTrue(selectionViews.size() == ScrollcasterGauntlet.BASE_CALIBRATION_SCROLL_SLOT_COUNT,
                    "Scrollcaster Gauntlet selection UI should expose every enabled slot");
            helper.assertTrue(!selectionViews.get(0).selectable() && selectionViews.get(3).selectable(),
                    "Scrollcaster Gauntlet selection UI should keep empty slots visible");
            helper.assertTrue(selectionViews.get(3).displayName().getString().endsWith(" 1"),
                    "Scrollcaster Gauntlet selection label should append the spell level number");
            helper.assertTrue(Objects.equals(
                            selectionViews.get(3).displayName().getStyle().getColor(),
                            magicMissile.getSchoolType().getDisplayName().getStyle().getColor()
                    ),
                    "Scrollcaster Gauntlet selection label should use the spell school color");
            var spellContainer = ISpellContainer.get(gauntlet);
            helper.assertTrue(spellContainer != null, "Selected Scrollcaster Gauntlet spell container is null");
            helper.assertTrue(spellContainer.isSpellWheel(), "Selected Scrollcaster Gauntlet spell should be visible to Iron's spell wheel");
            helper.assertFalse(spellContainer.mustEquip(), "Held Scrollcaster Gauntlet spell should not require an armor/curio slot");
            assertSpellData(helper, spellContainer, 0, magicMissile, 1, false,
                    "Selected Scrollcaster Gauntlet spell mismatch");
            helper.assertFalse(Utils.canImbue(gauntlet),
                    "Scrollcaster Gauntlet should not be treated as Arcane Anvil imbue equipment");
            helper.assertTrue(Utils.handleShriving(gauntlet).isEmpty(),
                    "Scrollcaster Gauntlet exposed spell should not be removable through Shriving Stone");
            ISpellContainer.createImbuedContainer(magicMissile, 1, gauntlet);
            helper.assertTrue(ISpellContainer.get(gauntlet).getSpellAtIndex(0).isLocked(),
                    "Legacy Scrollcaster Gauntlet projection setup should create a locked spell for this test");
            ScrollcasterGauntlet.refreshSelectedSpellContainer(gauntlet);
            assertSpellData(helper, ISpellContainer.get(gauntlet), 0, magicMissile, 1, false,
                    "Scrollcaster Gauntlet should repair legacy locked projection spells");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 1, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 1);
            assertSpellData(helper, ISpellContainer.get(gauntlet), 0, heal, 1, false,
                    "Changing Scrollcaster Gauntlet index should change the exposed spell");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_right_click_resolver_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var resolvedRightClickSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedRightClickSpell.isPresent(),
                    "Scrollcaster Gauntlet right-click resolver should find the selected gauntlet spell");
            helper.assertTrue(resolvedRightClickSpell.get().spellData().getSpell() == heal,
                    "Scrollcaster Gauntlet right-click resolver should use the gauntlet-selected spell");
            helper.assertTrue("scrollcaster_gauntlet_selected".equals(resolvedRightClickSpell.get().resolutionPath()),
                    "Scrollcaster Gauntlet right-click resolver should expose its dedicated resolution path");
            var storageStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, storageStabilizer);
            var resolvedStorageStabilizerSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedStorageStabilizerSpell.isPresent(),
                    "Storage Stabilizer right-click resolver should find the selected stabilizer spell");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Storage Stabilizer right-click resolver should use the selected Personal Shelf spell");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().hand() == InteractionHand.MAIN_HAND,
                    "Storage Stabilizer right-click resolver should mark the main hand");
            helper.assertTrue("storage_stabilizer_selected".equals(resolvedStorageStabilizerSpell.get().resolutionPath()),
                    "Storage Stabilizer right-click resolver should expose its dedicated resolution path");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().spellData().getSpell() instanceof IClientBlockTargetingSpell,
                    "Storage Stabilizer Personal Shelf should stay eligible for client block target sync");
            var storageStabilizerViews = StorageStabilizer.getSelectionViews(storageStabilizer);
            helper.assertTrue(storageStabilizerViews.size() == 3,
                    "Storage Stabilizer selection UI should expose all three storage spells");
            helper.assertTrue(storageStabilizerViews.get(2).selectionIndex() == 2
                            && storageStabilizerViews.get(2).selectable(),
                    "Storage Stabilizer should expose its third spell as a selectable common UI entry");
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 2);
            var companionTrunkContainer = ISpellContainer.get(storageStabilizer);
            helper.assertTrue(companionTrunkContainer != null,
                    "Storage Stabilizer Companion Trunk spell container is null");
            if (companionTrunkContainer != null) {
                assertSpellData(helper, companionTrunkContainer, 0, SpellRegistry.COMPANION_TRUNK.get(), 1, true,
                        "Storage Stabilizer should project Companion Trunk level 1 as a locked spell");
            }
            assertStorageStabilizerDisplayName(helper, storageStabilizer, SpellRegistry.COMPANION_TRUNK.get(),
                    "Storage Stabilizer default name should include Companion Trunk");
            storageStabilizer.setHoverName(Component.literal("Storage Test"));
            helper.assertTrue("Storage Test".equals(storageStabilizer.getHoverName().getString()),
                    "Storage Stabilizer custom name should hide its spell name");
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 1);
            helper.assertTrue("Storage Test".equals(storageStabilizer.getHoverName().getString()),
                    "Storage Stabilizer custom name should remain after changing its spell selection");
            var invalidStorageStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(invalidStorageStabilizer, 99);
            helper.assertTrue(StorageStabilizer.getSelectedSpellIndex(invalidStorageStabilizer) == 0,
                    "Storage Stabilizer invalid selection should fall back to Summon Ender Chest");
            var companionTrunkStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(companionTrunkStabilizer, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, companionTrunkStabilizer);
            var resolvedCompanionTrunkSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedCompanionTrunkSpell.isPresent()
                            && resolvedCompanionTrunkSpell.get().spellData().getSpell() == SpellRegistry.COMPANION_TRUNK.get(),
                    "Storage Stabilizer right-click resolver should use the selected Companion Trunk spell");
            var inertMainHand = new ItemStack(Items.STICK);
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, storageStabilizer);
            var resolvedOffhandStorageStabilizerSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.isPresent(),
                    "Storage Stabilizer right-click resolver should find the selected offhand stabilizer spell");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Storage Stabilizer right-click resolver should use the offhand selected Personal Shelf spell");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Storage Stabilizer right-click resolver should mark the offhand");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().spellData().getSpell() instanceof IClientBlockTargetingSpell,
                    "Offhand Storage Stabilizer Personal Shelf should stay eligible for client block target sync");
            var emptyMainHandGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, emptyMainHandGauntlet);
            var resolvedOffhandStorageAfterEmptyGauntlet = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.isPresent(),
                    "Right-click resolver should find the offhand stabilizer spell after an empty mainhand gauntlet");
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the offhand stabilizer spell after an empty mainhand gauntlet");
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the offhand after an empty mainhand gauntlet");
            var emptyMainHandSpellGun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, emptyMainHandSpellGun);
            var resolvedMainHandSpellGunRightClick = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedMainHandSpellGunRightClick.isEmpty(),
                    "Mainhand Spellgun should not resolve a right-click spell before vanilla advances to the offhand");
            var resolvedOffhandStorageAfterEmptySpellGun = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.isPresent(),
                    "Right-click resolver should find the offhand stabilizer spell after an empty mainhand spell gun");
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the offhand stabilizer spell after an empty mainhand spell gun");
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the offhand after an empty mainhand spell gun");
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
            var resolvedOffhandGauntletSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandGauntletSpell.isPresent(),
                    "Scrollcaster Gauntlet right-click resolver should find the selected offhand gauntlet spell");
            helper.assertTrue(resolvedOffhandGauntletSpell.get().spellData().getSpell() == heal,
                    "Scrollcaster Gauntlet right-click resolver should use the offhand gauntlet-selected spell");
            helper.assertTrue(resolvedOffhandGauntletSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Scrollcaster Gauntlet right-click resolver should mark the offhand");
            var offhandSpellGun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            ISpellContainer.createImbuedContainer(SpellRegistry.PERSONAL_SHELF.get(), 1, offhandSpellGun);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandSpellGun);
            var resolvedOffhandSpellGunSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandSpellGunSpell.isPresent(),
                    "Spellcaster Gun right-click resolver should find the offhand gun spell");
            helper.assertTrue(resolvedOffhandSpellGunSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Spellcaster Gun right-click resolver should use the offhand gun spell container");
            helper.assertTrue(resolvedOffhandSpellGunSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Spellcaster Gun right-click resolver should mark the offhand");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            var resolvedPriorityOffhandGunOverWeapon = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.isPresent(),
                    "Right-click resolver should find the priority offhand gun spell over a deferring magic weapon");
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the priority offhand gun spell over a deferring magic weapon");
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the priority offhand gun over a deferring magic weapon");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var resolvedPriorityOffhandGunOverGauntlet = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.isPresent(),
                    "Right-click resolver should find the priority offhand gun spell over a deferring gauntlet");
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the priority offhand gun spell over a deferring gauntlet");
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the priority offhand gun over a deferring gauntlet");
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, createSpellScroll(SpellRegistry.PERSONAL_SHELF.get()));
            var resolvedOffhandScrollSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandScrollSpell.isPresent(),
                    "Scroll right-click resolver should find the offhand scroll spell");
            helper.assertTrue(resolvedOffhandScrollSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Scroll right-click resolver should use the offhand scroll spell container");
            helper.assertTrue(resolvedOffhandScrollSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Scroll right-click resolver should mark the offhand");
            var offhandStaff = new ItemStack(ItemRegistry.ZENITH_STAFF.get());
            var mutableStaffSpells = ISpellContainer.create(1, true, false).mutableCopy();
            mutableStaffSpells.addSpellAtIndex(SpellRegistry.PERSONAL_SHELF.get(), 1, 0, false);
            ISpellContainer.set(offhandStaff, mutableStaffSpells.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, offhandStaff);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet cooldown test could not resolve player mana data");
            magicData.getSyncedData().setSpellSelection(new io.redspace.ironsspellbooks.gui.overlays.SpellSelection(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            ));
            var resolvedOffhandCastingItemSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandCastingItemSpell.isPresent(),
                    "CastingItem right-click resolver should find the selected offhand spell");
            helper.assertTrue(resolvedOffhandCastingItemSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "CastingItem right-click resolver should use the offhand spell selection");
            helper.assertTrue(resolvedOffhandCastingItemSpell.get().hand() == InteractionHand.OFF_HAND,
                    "CastingItem right-click resolver should mark the offhand");
            player.setItemInHand(InteractionHand.MAIN_HAND, createSpellScroll(magicMissile));
            player.setItemInHand(InteractionHand.OFF_HAND, storageStabilizer);
            var resolvedMainHandPrioritySpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedMainHandPrioritySpell.isPresent(),
                    "Right-click resolver should still resolve the mainhand spell item first");
            helper.assertTrue(resolvedMainHandPrioritySpell.get().spellData().getSpell() == magicMissile,
                    "Right-click resolver should not prefer offhand spell sources over a mainhand spell item");
            helper.assertTrue(resolvedMainHandPrioritySpell.get().hand() == InteractionHand.MAIN_HAND,
                    "Right-click resolver should mark the prioritized mainhand spell");
            magicData.setPlayerCastingItem(gauntlet.copy());
            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Scrollcaster Gauntlet cooldown event should use the helper cooldown amount but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            magicData.setPlayerCastingItem(ItemStack.EMPTY);
            var controlEvent = new SpellCooldownAddedEvent.Pre(
                    160,
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == 160,
                    "Scrollcaster Gauntlet cooldown event should not affect non-gauntlet casts");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 1, ItemStack.EMPTY);
            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == 3,
                    "Removing the selected scroll should normalize to the first remaining scroll");
            assertSpellData(helper, ISpellContainer.get(gauntlet), 0, magicMissile, 1, false,
                    "Normalized Scrollcaster Gauntlet spell mismatch");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 3, ItemStack.EMPTY);
            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == -1,
                    "Removing every scroll should clear the selected gauntlet index");
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet),
                    "Removing every scroll should clear the exposed gauntlet spell container");
        });
    }

    static void scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var baseViews = ScrollcasterGauntlet.getSelectionViews(gauntlet);
            helper.assertTrue(baseViews.size() == ScrollcasterGauntlet.BASE_CALIBRATION_SCROLL_SLOT_COUNT,
                    "Empty Scrollcaster Gauntlet selection UI should expose every base slot");
            helper.assertTrue(baseViews.stream().noneMatch(jp.aquafactory.apprenticecodex.item.SneakSelectionView::selectable),
                    "Empty Scrollcaster Gauntlet selection UI should expose only empty slots");
            var emptyState = jp.aquafactory.apprenticecodex.item.SneakSelectionState.open(
                    InteractionHand.MAIN_HAND,
                    baseViews,
                    ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet)
            );
            helper.assertTrue(emptyState.selectableViewCount() == 0 && emptyState.move(1) == emptyState,
                    "Empty Scrollcaster Gauntlet selection UI should stay open without a movable selection");

            var expandedGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lesserUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
            );
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            expandedGauntlet,
                            0,
                            lesserUpgrade
                    ),
                    "Failed to add a slot upgrade to the empty Scrollcaster Gauntlet");

            var expandedViews = ScrollcasterGauntlet.getSelectionViews(expandedGauntlet);
            helper.assertTrue(expandedViews.size()
                            == ScrollcasterGauntlet.getEnabledCalibrationScrollSlotCount(expandedGauntlet),
                    "Empty Scrollcaster Gauntlet selection UI should track its enabled slot count");
            helper.assertTrue(expandedViews.size() > baseViews.size(),
                    "Slot upgrade should add empty slots to the Scrollcaster Gauntlet selection UI");
            helper.assertTrue(expandedViews.stream().noneMatch(jp.aquafactory.apprenticecodex.item.SneakSelectionView::selectable),
                    "Expanded empty Scrollcaster Gauntlet selection UI should keep every slot empty");
        });
    }

    static void scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var gauntletItem = (ScrollcasterGauntlet) gauntlet.getItem();
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var fireBreath = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_freecast_swing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet freecast swing test could not resolve player magic data");
            magicData.setMana(1000.0F);

            helper.assertFalse(gauntletItem.canTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND),
                    "Scrollcaster Gauntlet should not be treated as swing-triggerable before freecast adjustment");
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet should not swing-cast without a Mithril Freecast Staff adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(gauntlet, 0, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()));
            helper.assertTrue(ScrollcasterGauntlet.hasFreecastStaffAdjustment(gauntlet),
                    "Scrollcaster Gauntlet should detect its Mithril Freecast Staff adjustment");
            helper.assertTrue(gauntletItem.canTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND),
                    "Scrollcaster Gauntlet should be treated as swing-triggerable after freecast adjustment");
            assertTooltipKeyAt(helper, gauntlet, 3, "item.apprenticecodex.freecast.common.desc",
                    "Freecast-adjusted Scrollcaster Gauntlet should show the generic freecast tooltip");

            helper.assertTrue(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet should swing-cast the selected instant spell with a Mithril Freecast Staff adjustment");
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                    "Scrollcaster Gauntlet freecast should cast with the gauntlet stack");
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Scrollcaster Gauntlet freecast should mark the mainhand casting slot");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(fireBreath));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should reject continuous spells like Mithril Freecast Staff");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 1.0F));
            helper.assertTrue(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should allow long spells with only a Mithril Freecast Staff adjustment");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(heal.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should reject spells that are already on cooldown");
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            magicData.setPlayerCastingItem(gauntlet.copy());
            var baseCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SWORD
            );
            var normalEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(normalEvent);
            helper.assertTrue(normalEvent.getEffectiveCooldown() == baseCooldown,
                    "Normal Scrollcaster Gauntlet casts should keep the base sword cooldown");

            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, heal)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                        heal,
                        player,
                        CastSource.SWORD
                );
                ScrollcasterGauntletCastEvent.onSpellCooldownAdded(freecastEvent);
                var expectedCooldown = baseCooldown + heal.getEffectiveCastTime(1, player);
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should extend long spell cooldown by cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(thermalProcess));
            magicData.setPlayerCastingItem(gauntlet.copy());
            var craftsmansBaseCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    thermalProcess,
                    player,
                    CastSource.SWORD
            );
            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, thermalProcess)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(thermalProcess, player, CastSource.SWORD),
                        thermalProcess,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(freecastEvent);
                var expectedCooldown = gauntletItem.resolveFreecastSwingCooldownTicks(
                        player,
                        gauntlet,
                        thermalProcess,
                        craftsmansBaseCooldown
                );
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should keep CraftsmansDelight cooldown and long cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var artisanSmash = SpellRegistry.ARTISAN_SMASH.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(artisanSmash));
            magicData.setPlayerCastingItem(gauntlet.copy());
            var artisanBaseCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    artisanSmash,
                    player,
                    CastSource.SWORD
            );
            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, artisanSmash)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(artisanSmash, player, CastSource.SWORD),
                        artisanSmash,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(freecastEvent);
                var expectedCooldown = gauntletItem.resolveFreecastSwingCooldownTicks(
                        player,
                        gauntlet,
                        artisanSmash,
                        artisanBaseCooldown
                );
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should not re-reduce Magi boots cooldown after adding long cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }
        });
    }

    static void scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat.MOD_ID)) {
                return;
            }

            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    gauntlet,
                    0,
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get())
            );

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_epicfight_mirror_swing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet Epic Fight mirror swing test could not resolve player magic data");
            magicData.setMana(1000.0F);

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge
                            .shouldMirrorMainhand(player, InteractionHand.OFF_HAND),
                    "Epic Fight mirrored offhand state should resolve from a mainhand Gauntlet and empty offhand"
            );
            var resolvedHand = jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                    .resolveSwingMagicTriggerHand(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedHand == InteractionHand.MAIN_HAND,
                    "Epic Fight mirrored offhand swing should use the mainhand Gauntlet but resolved " + resolvedHand);

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                            .triggerSwingMagicFromAttackPhase(player, InteractionHand.OFF_HAND, -1, 0),
                    "Epic Fight mirrored offhand attack should trigger the mainhand Gauntlet Swingcast"
            );
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                    "Epic Fight mirrored Gauntlet Swingcast should cast with the mainhand Gauntlet stack");
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Epic Fight mirrored Gauntlet Swingcast should mark the mainhand casting slot");
        });
    }

    static void scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat.MOD_ID)) {
                return;
            }

            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var fallbackStaff = new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_epicfight_fallback_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            player.setItemInHand(InteractionHand.OFF_HAND, fallbackStaff);

            var resolvedHand = jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                    .resolveAvailableSwingMagicTriggerHand(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedHand == InteractionHand.OFF_HAND,
                    "Epic Fight timed swing trigger should fall back from an unadjusted mainhand Gauntlet to the offhand Swingcast item but resolved "
                            + resolvedHand);
        });
    }

    static void spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_calibration_freecast_adjustment_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var freecastStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());

            helper.assertFalse(menu.getSlot(1).mayPlace(freecastStaff),
                    "Mithril Freecast Staff adjustment should be rejected without a calibration target");
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(menu.getSlot(1).mayPlace(freecastStaff),
                    "Scrollcaster Gauntlet should accept a Mithril Freecast Staff adjustment");
            menu.getSlot(1).set(freecastStaff.copy());
            helper.assertTrue(ScrollcasterGauntlet.hasFreecastStaffAdjustment(gauntlet),
                    "Mithril Freecast Staff adjustment should be stored on the gauntlet");
            helper.assertFalse(menu.getSlot(2).mayPlace(freecastStaff),
                    "Scrollcaster Gauntlet should reject duplicate Mithril Freecast Staff adjustments");
            helper.assertFalse(menu.getSlot(2).mayPlace(silverRing),
                    "Scrollcaster Gauntlet should reject Silver Ring adjustments");

            menu.getSlot(1).set(ItemStack.EMPTY);
            menu.getSlot(0).set(staff);
            helper.assertFalse(menu.getSlot(1).mayPlace(freecastStaff),
                    "Revolvercast Staff should not accept a Mithril Freecast Staff adjustment");
            helper.assertTrue(menu.getSlot(1).mayPlace(silverRing),
                    "Revolvercast Staff should accept Silver Ring adjustments");
        });
    }

    static void spellCalibrationBenchStoresScrollsOnRevolvercastStaff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolvercast_staff_storage_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var recoveryRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            var firstScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var longScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            var continuousScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());

            helper.assertTrue(menu.getSlot(0).mayPlace(staff),
                    "Revolvercast Staff should be accepted in the Spell Calibration Bench target slot");
            menu.getSlot(0).set(staff);
            helper.assertTrue(menu.isScrollSlotEnabled(0), "Revolvercast Staff scroll slot 0 should be enabled by default");
            helper.assertTrue(menu.isScrollSlotEnabled(3), "Revolvercast Staff scroll slot 3 should be enabled by default");
            helper.assertFalse(menu.isScrollSlotEnabled(4),
                    "Revolvercast Staff scroll slot 4 should be locked before adding an upgrade");
            helper.assertFalse(menu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Revolvercast Staff should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(menu.getSlot(1).mayPlace(lesserUpgrade),
                    "Revolvercast Staff should accept slot upgrade adjustments");
            helper.assertTrue(menu.getSlot(1).mayPlace(recoveryRune),
                    "Revolvercast Staff should accept Recovery Rune adjustments");
            helper.assertTrue(menu.getSlot(1).mayPlace(silverRing),
                    "Revolvercast Staff should accept Silver Ring adjustments");
            helper.assertFalse(menu.getSlot(1).mayPlace(enchantedBook),
                    "Revolvercast Staff should reject enchantment book adjustments");

            menu.getSlot(1).set(recoveryRune);
            helper.assertTrue(RevolvercastStaff.hasRecoveryRune(staff),
                    "Revolvercast Staff should enter skip mode after storing a Recovery Rune");
            helper.assertFalse(menu.getSlot(2).mayPlace(recoveryRune),
                    "Revolvercast Staff should reject a duplicate Recovery Rune adjustment");

            menu.getSlot(2).set(lesserUpgrade);
            helper.assertTrue(menu.isScrollSlotEnabled(5),
                    "One lesser slot upgrade should unlock six Revolvercast Staff scroll slots");
            helper.assertFalse(menu.isScrollSlotEnabled(6),
                    "One lesser slot upgrade should not unlock the seventh Revolvercast Staff scroll slot");
            helper.assertTrue(menu.getSlot(9).mayPlace(firstScroll.copy()),
                    "Newly unlocked Revolvercast Staff scroll slot should accept scrolls");
            helper.assertTrue(menu.getSlot(9).mayPlace(longScroll.copy()),
                    "Revolvercast Staff should accept long scroll storage so Silver Ring can enable it later");
            helper.assertFalse(menu.getSlot(9).mayPlace(continuousScroll),
                    "Revolvercast Staff should reject unsupported continuous scrolls in the Spell Calibration Bench");
            menu.getSlot(9).set(firstScroll);
            helper.assertFalse(RevolvercastStaff.getCalibrationScroll(staff, 5).isEmpty(),
                    "Scroll should be stored on the Revolvercast Staff NBT");
        });
    }

    static void revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            RevolvercastStaff.refreshSelectedSpellContainer(staff);
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Empty Revolvercast Staff should not expose a spell container");

            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
            RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
            RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));

            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "First enabled Revolvercast Staff scroll should become selected");
            var spellContainer = ISpellContainer.get(staff);
            helper.assertTrue(spellContainer != null, "Selected Revolvercast Staff spell container is null");
            helper.assertTrue(spellContainer.isSpellWheel(),
                    "Selected Revolvercast Staff spell should be visible to Iron's spell wheel");
            assertSpellData(helper, spellContainer, 0, magicMissile, 1, false,
                    "Initial Revolvercast Staff selected spell mismatch");

            helper.assertTrue(RevolvercastStaff.advanceToNextValidScrollIndex(staff),
                    "Revolvercast Staff should advance to the next valid scroll");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff should skip empty scroll slots while advancing");
            assertSpellData(helper, ISpellContainer.get(staff), 0, heal, 1, false,
                    "Advanced Revolvercast Staff selected spell mismatch");

            RevolvercastStaff.setCalibrationScroll(staff, 3, createSpellScroll(fireball));
            RevolvercastStaff.setCalibrationScroll(staff, 2, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 3,
                    "Invalid Revolvercast Staff index should normalize to the next valid scroll");
            assertSpellData(helper, ISpellContainer.get(staff), 0, fireball, 1, false,
                    "Normalized Revolvercast Staff selected spell mismatch");

            RevolvercastStaff.setCalibrationScroll(staff, 3, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Removing the selected last scroll should wrap to the first valid scroll");
            RevolvercastStaff.setCalibrationScroll(staff, 0, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == -1,
                    "Removing every Revolvercast Staff scroll should clear the selected index");
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Removing every Revolvercast Staff scroll should clear the exposed spell container");
        });
    }

    static void revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
            RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
            RevolvercastStaff.setSelectedScrollIndex(staff, 0);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "revolvercast_staff_cooldown_failure_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Revolvercast Staff cooldown failure test could not resolve player magic data");
            magicData.setMana(1000.0F);
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);

            helper.assertFalse(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Revolvercast Staff should fail to swing-cast a spell that is on cooldown");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Normal Revolvercast Staff mode should stay on a failed cooldown spell");

            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get())
            );
            helper.assertFalse(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Skip mode Revolvercast Staff should still fail to cast a spell that is on cooldown");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Skip mode Revolvercast Staff should advance after a cooldown failure");
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());
        });
    }

    static void revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(fireball));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_success_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff pending success test could not resolve player magic data");
        magicData.setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            helper.assertTrue(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Revolvercast Staff should successfully initiate the selected spell");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should not advance in the same tick as cast initiation");

            RevolvercastStaffPendingAdvance.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should wait until the next game tick before advancing");
        });

        helper.runAtTickTime(3, () -> {
            RevolvercastStaffPendingAdvance.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff should advance after the successful cast completes and a tick passes");
            helper.succeed();
        });
    }

    static void revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(fireball));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_unrelated_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff unrelated completion test could not resolve player magic data");

        RevolvercastStaffPendingAdvance.reserve(player, InteractionHand.MAIN_HAND, staff, fireball, 0);
        var unrelatedMagicData = new MagicData();
        unrelatedMagicData.setPlayerCastingItem(new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()));
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, fireball, unrelatedMagicData, false);
        magicData.setPlayerCastingItem(staff);
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, fireball, magicData, false);

        helper.runAtTickTime(2, () -> {
            RevolvercastStaffPendingAdvance.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff pending advance should survive unrelated RemoteOwner cast completion");
            helper.succeed();
        });
    }

    static void revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_cancel_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff pending cancel test could not resolve player magic data");

        RevolvercastStaffPendingAdvance.reserve(player, InteractionHand.MAIN_HAND, staff, magicMissile, 0);
        magicData.setPlayerCastingItem(staff);
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, magicMissile, magicData, true);

        helper.runAtTickTime(2, () -> {
            RevolvercastStaffPendingAdvance.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should discard pending advancement when the cast is cancelled");
            helper.succeed();
        });
    }

    static void revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var staff = (RevolvercastStaff) stack.getItem();
            var modifiers = staff.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.10D,
                    "Revolvercast Staff general spell power modifier changed"
            );
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
            );
            modifiers = staff.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.15D,
                    "Fire rune should replace Revolvercast Staff general spell power with a stronger fire spell power bonus"
            );
            helper.assertTrue(modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()).isEmpty(),
                    "Fire-tuned Revolvercast Staff should not keep its general spell power modifier");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Revolvercast Staff should reject Arcane Anvil spell imbuing"
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = SpellRegistry.ARCANE_BLAST.get();
            helper.assertTrue(staff.canImbueSpell(magicMissile, 1),
                    "Revolvercast Staff should accept instant spells by default");
            helper.assertFalse(staff.canImbueSpell(heal, 1),
                    "Revolvercast Staff should reject long spells without Silver Ring");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            helper.assertTrue(RevolvercastStaff.canSwingCastSpell(stack, heal),
                    "Silver Ring should enable Revolvercast Staff long swing-cast support");
            helper.assertFalse(staff.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Revolvercast Staff should reject continuous spells");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "revolvercast_staff_cooldown_mode_test");
            var longSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var currentCooldown = 80;
            var resolvedCooldown = staff.resolveSwingcastCooldownTicks(player, stack, longSpell, currentCooldown);
            helper.assertTrue(resolvedCooldown == currentCooldown + longSpell.getEffectiveCastTime(1, player),
                    "Revolvercast Staff long spell cooldown should be extended like Diamond Swingcast Staff");
        });
    }

    static void scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var state = Blocks.STONE.defaultBlockState();
            var pos = new BlockPos(0, 1, 0);
            var survivalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_survival_block_attack_test");
            var creativePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_creative_block_attack_test");

            creativePlayer.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);

            helper.assertTrue(gauntlet.getItem().canAttackBlock(state, helper.getLevel(), pos, survivalPlayer),
                    "Scrollcaster Gauntlet should keep normal block attacks outside creative mode");
            helper.assertFalse(gauntlet.getItem().canAttackBlock(state, helper.getLevel(), pos, creativePlayer),
                    "Scrollcaster Gauntlet should block creative mode block attacks like vanilla swords");
        });
    }


    static void spellCalibrationBenchAdjustmentSlotsValidateInputs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_adjustment_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var iceRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get());
            var arcaneRuneItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_rune"));
            helper.assertTrue(arcaneRuneItem != null, "irons_spellbooks:arcane_rune is not registered");
            var arcaneRune = new ItemStack(arcaneRuneItem);
            var fireRuneId = ForgeRegistries.ITEMS.getKey(fireRune.getItem());
            var arcaneRuneId = ForgeRegistries.ITEMS.getKey(arcaneRune.getItem());
            helper.assertTrue(fireRuneId != null, "irons_spellbooks:fire_rune should have a registry id");
            helper.assertTrue(arcaneRuneId != null, "irons_spellbooks:arcane_rune should have a registry id");
            var enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var recoveryRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());

            helper.assertTrue(lesserUpgrade.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES),
                    "Lesser spell slot upgrade should be tagged as a Scrollcaster Gauntlet slot upgrade");
            player.getInventory().setItem(9, recoveryRune.copy());
            var quickMovedRecoveryRune = menu.quickMoveStack(
                    player,
                    SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT
            );
            helper.assertTrue(quickMovedRecoveryRune.is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    "Recovery Rune shift-click without a target should fall back to normal inventory movement");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty()
                            && player.getInventory().getItem(0).is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    "Recovery Rune should move from main inventory to hotbar when no Calibration Bench target is present");
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(fireRune)
                            .filter(school -> SchoolRegistry.FIRE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Fire rune should resolve to the fire school without a whitelist entry"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(arcaneRune).isEmpty(),
                    "Non-school runes should not resolve to a scrollcaster school"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(arcaneRuneId, Map.of(arcaneRuneId, SchoolRegistry.FIRE_RESOURCE))
                            .filter(school -> SchoolRegistry.FIRE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Manual rune override should resolve a rune that automatic lookup cannot resolve"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(fireRuneId, Map.of(fireRuneId, SchoolRegistry.ICE_RESOURCE))
                            .filter(school -> SchoolRegistry.ICE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Manual rune override should take precedence over automatic rune lookup"
            );
            helper.assertTrue(menu.getSlot(1).mayPlace(fireRune), "School rune should be accepted as a calibration adjustment");
            helper.assertFalse(menu.getSlot(2).mayPlace(enchantedBook),
                    "Enchanted books should no longer be accepted as calibration adjustments");
            helper.assertTrue(menu.getSlot(3).mayPlace(lesserUpgrade), "Tagged slot upgrades should be accepted as a calibration adjustment");
            helper.assertTrue(!menu.getSlot(1).mayPlace(arcaneRune), "Arcane rune should not be treated as a scrollcaster school rune");

            menu.getSlot(1).set(fireRune);
            helper.assertTrue(!menu.getSlot(2).mayPlace(iceRune), "Only one school rune should be accepted at a time");
            helper.assertFalse(menu.getSlot(2).mayPlace(enchantedBook),
                    "Enchanted books should remain rejected after a rune is present");
            helper.assertTrue(!SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(gauntlet, 0).isEmpty(),
                    "Adjustment item should be stored on the gauntlet NBT");
        });
    }

    static void spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_school_power_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());

            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.05D, 0.0D, 0.0D,
                    "Uncalibrated Scrollcaster Gauntlet should keep its base spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet).isEmpty(),
                    "Uncalibrated Scrollcaster Gauntlet should not expose an inventory rune overlay");

            menu.getSlot(0).set(gauntlet);
            menu.getSlot(1).set(fireRune);
            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.0D, 0.10D, 0.0D,
                    "Fire rune should replace base spell power with fire spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    "Fire-calibrated Scrollcaster Gauntlet should expose the Fire rune inventory overlay");
            var fireSchool = SchoolRegistry.getSchool(SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireSchool != null, "Fire school should be registered");
            assertTooltipKeyArgumentUsesColor(
                    helper,
                    gauntlet,
                    "item.apprenticecodex.scrollcaster_gauntlet.school_rune",
                    0,
                    fireSchool.getDisplayName().getStyle().getColor(),
                    "School rune tooltip should keep the resolved school color"
            );

            menu.getSlot(1).set(ItemStack.EMPTY);
            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.05D, 0.0D, 0.0D,
                    "Removing the school rune should restore base spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet).isEmpty(),
                    "Removing the school rune should remove the inventory rune overlay");
            assertTooltipKeyAbsent(helper, gauntlet, "item.apprenticecodex.scrollcaster_gauntlet.school_rune",
                    "Removing the school rune should remove the school rune tooltip");

            var staleGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(staleGauntlet, 0, fireRune);
            staleGauntlet.getOrCreateTagElement("SpellCalibration")
                    .putString("SchoolPowerSchool", SchoolRegistry.ICE_RESOURCE.toString());
            assertScrollcasterGauntletSpellPower(helper, staleGauntlet, 0.0D, 0.0D, 0.10D,
                    "Stale school power should reflect the stored school before bench refresh");

            var refreshMenu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(1, 1, 0));
            refreshMenu.getSlot(0).set(staleGauntlet);
            assertScrollcasterGauntletSpellPower(helper, staleGauntlet, 0.0D, 0.10D, 0.0D,
                    "Placing the gauntlet on the bench should refresh the stored rune school");
        });
    }

    static void scrollcasterGauntletUsesNormalEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var item = gauntlet.getItem();
            helper.assertTrue(item.isEnchantable(gauntlet),
                    "Scrollcaster Gauntlet should be enchantable");
            helper.assertTrue(item.getEnchantmentValue(gauntlet) == 22,
                    "Scrollcaster Gauntlet enchantability should be 22");

            for (var supported : List.of(
                    Enchantments.SHARPNESS,
                    Enchantments.BLOCK_FORTUNE,
                    EnchantmentRegistry.WISDOM.get(),
                    EnchantmentRegistry.ALACRITY.get()
            )) {
                helper.assertTrue(item.canApplyAtEnchantingTable(gauntlet, supported),
                        "Scrollcaster Gauntlet enchanting table should support "
                                + ForgeRegistries.ENCHANTMENTS.getKey(supported));
                helper.assertTrue(item.isBookEnchantable(
                                gauntlet,
                                createEnchantedBook(new EnchantmentInstance(supported, 1))
                        ),
                        "Scrollcaster Gauntlet anvil should support "
                                + ForgeRegistries.ENCHANTMENTS.getKey(supported));
            }
            for (var rejected : List.of(
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING,
                    EnchantmentRegistry.PLUNDER.get()
            )) {
                helper.assertFalse(item.canApplyAtEnchantingTable(gauntlet, rejected),
                        "Scrollcaster Gauntlet should reject " + ForgeRegistries.ENCHANTMENTS.getKey(rejected));
            }
            helper.assertTrue(item instanceof NonDamageableAnvilMergeItem,
                    "Scrollcaster Gauntlet should use the non-damageable anvil merge path");
            helper.assertTrue(((NonDamageableAnvilMergeItem) item)
                            .isAnvilMergeEnchantmentAllowed(gauntlet, Enchantments.SHARPNESS),
                    "Scrollcaster Gauntlet anvil merge should accept Sharpness");

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                for (var enchantmentId : List.of(MALUM_SPIRIT_PLUNDER, MALUM_HAUNTED, MALUM_ANIMATED)) {
                    var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                    helper.assertTrue(enchantment != null,
                            "Malum enchantment should be registered: " + enchantmentId);
                    helper.assertTrue(item.canApplyAtEnchantingTable(gauntlet, enchantment),
                            "Scrollcaster Gauntlet should support compatible Malum enchantment " + enchantmentId);
                }
            }
        });
    }

    static void nonDamageableAnvilMergeRejectsStoredRightContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "non_damageable_anvil_stored_contents_test");
            var left = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var right = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            right.enchant(Enchantments.SHARPNESS, 1);

            var emptyMerge = NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player);
            helper.assertTrue(emptyMerge != null,
                    "Non-damageable anvil merge should accept an empty right input");

            var leftAdjustment = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            CalibrationAdjustmentStorage.set(
                    left,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    leftAdjustment
            );
            var leftContentMerge = NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player);
            helper.assertTrue(leftContentMerge != null,
                    "Non-damageable anvil merge should accept stored contents on the left input");
            helper.assertTrue(!CalibrationAdjustmentStorage.get(
                    leftContentMerge.output(),
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT
            ).isEmpty(), "Non-damageable anvil merge should preserve stored left contents");

            CalibrationAdjustmentStorage.set(
                    right,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    leftAdjustment
            );
            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) == null,
                    "Non-damageable anvil merge should reject a right input with a calibration adjustment");
            CalibrationAdjustmentStorage.set(
                    right,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

            var disabledStoredSlot = ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT - 1;
            ScrollcasterGauntlet.setCalibrationScroll(
                    right,
                    disabledStoredSlot,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) == null,
                    "Non-damageable anvil merge should reject a right input with a stored scroll in a disabled slot");
            ScrollcasterGauntlet.setCalibrationScroll(right, disabledStoredSlot, ItemStack.EMPTY);

            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) != null,
                    "Non-damageable anvil merge should recover after clearing stored right contents");
        });
    }

    static void scrollcasterGauntletKeepsLegacyEnchantmentsAndAllowsBookExtraction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_legacy_enchantment_test");
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var legacyBook = createEnchantedBook(new EnchantmentInstance(Enchantments.SHARPNESS, 2));
            gauntlet.enchant(Enchantments.SHARPNESS, 2);
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    legacyBook
            );

            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(gauntlet.getEnchantmentLevel(Enchantments.SHARPNESS) == 2,
                    "Opening the Calibration Bench should preserve existing gauntlet enchantments");
            helper.assertTrue(menu.getSlot(1).mayPickup(player),
                    "Legacy enchanted books should remain extractable from disabled adjustment rules");
            var extractedBook = menu.getSlot(1).remove(1);
            helper.assertTrue(EnchantmentHelper.getEnchantments(extractedBook)
                            .getOrDefault(Enchantments.SHARPNESS, 0) == 2,
                    "Extracted legacy books should preserve their enchantment");
            helper.assertFalse(menu.getSlot(1).mayPlace(extractedBook),
                    "Extracted enchanted books should not be insertable again");
            helper.assertTrue(gauntlet.getEnchantmentLevel(Enchantments.SHARPNESS) == 2,
                    "Extracting a legacy book should not remove the gauntlet enchantment");
        });
    }

    static void scrollcasterGauntletGrindstoneRemovesEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_grindstone_test");
            var grindstonePos = new BlockPos(0, 1, 0);
            helper.setBlock(grindstonePos, Blocks.GRINDSTONE);
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            gauntlet.enchant(Enchantments.SHARPNESS, 1);
            var menu = new GrindstoneMenu(
                    0,
                    player.getInventory(),
                    net.minecraft.world.inventory.ContainerLevelAccess.create(
                            helper.getLevel(),
                            helper.absolutePos(grindstonePos)
                    )
            );

            menu.getSlot(0).set(gauntlet);
            var output = menu.getSlot(2).getItem();
            helper.assertTrue(output.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    "Scrollcaster Gauntlet should expose a grindstone output");
            helper.assertTrue(output.getEnchantmentLevel(Enchantments.SHARPNESS) == 0,
                    "Scrollcaster Gauntlet grindstone output should remove Sharpness");
        });
    }

    static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(0, 1, 0), 0, 0, 0);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(1, 1, 0), 3, 0, 3);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(2, 1, 0), ArcanumInAJarBlockEntity.MAX_STORED_PARAMETER, 0, 8);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(3, 1, 0), 0, 5, 0);
        });
    }

    static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertAtelierStationComparatorOutput(helper, new BlockPos(0, 1, 0), 0, false, 0);
            assertAtelierStationComparatorOutput(helper, new BlockPos(1, 1, 0), AtelierStationBlockEntity.MILLIBUCKETS_PER_USE, false, 1);
            assertAtelierStationComparatorOutput(helper, new BlockPos(2, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT / 2, false, 8);
            assertAtelierStationComparatorOutput(helper, new BlockPos(3, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT, false, 15);
            assertAtelierStationComparatorOutput(helper, new BlockPos(4, 1, 0), 0, true, 0);
        });
    }



    static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var apprenticeEnabledSpells = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells().stream()
                    .filter(ApprenticeCodexGameTestScenarios::isApprenticeSpell)
                    .toList();
            var creativeTabSpells = CreativeTabRegistry.getCreativeTabSpells();

            helper.assertFalse(creativeTabSpells.isEmpty(), "No apprentice spells were exported to the creative tab");
            helper.assertTrue(creativeTabSpells.size() == apprenticeEnabledSpells.size(),
                    "Creative tab spell count mismatch: expected " + apprenticeEnabledSpells.size() + " but got " + creativeTabSpells.size());

            var schoolOrder = new LinkedHashMap<ResourceLocation, Integer>();
            var orderIndex = 0;
            for (var schoolType : io.redspace.ironsspellbooks.api.registry.SchoolRegistry.REGISTRY.get().getValues()) {
                schoolOrder.putIfAbsent(schoolType.getId(), orderIndex++);
            }

            var previousSchoolIndex = -1;
            for (AbstractSpell spell : creativeTabSpells) {
                var spellId = spell.getSpellResource();
                helper.assertTrue(spellId != null, "Creative tab spell id is null");
                var schoolType = spell.getSchoolType();
                helper.assertTrue(schoolType != null, "Creative tab spell school is null: " + spellId);

                var schoolIndex = schoolOrder.getOrDefault(schoolType.getId(), Integer.MAX_VALUE);
                helper.assertTrue(previousSchoolIndex <= schoolIndex,
                        "Creative tab spell order is mixed across schools at " + spellId + " (" + schoolType.getId() + ")");
                previousSchoolIndex = schoolIndex;
            }
        });
    }

    static void zenithStaffUsesStrongestSchoolPowerAndManaPenalty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var zenithStaffId = ForgeRegistries.ITEMS.getKey(ItemRegistry.ZENITH_STAFF.get());
            helper.assertTrue(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "zenith_staff").equals(zenithStaffId),
                    "Zenith Staff is not registered with the expected id: " + zenithStaffId);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "zenith_staff_power_test");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.ZENITH_STAFF.get()));

            var firePowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get());
            helper.assertTrue(firePowerAttribute != null, "Zenith Staff test player is missing fire spell power attribute");
            firePowerAttribute.addTransientModifier(new AttributeModifier(
                    ZENITH_STAFF_SCHOOL_POWER_TEST_MODIFIER_ID,
                    "apprenticecodex.zenith_staff.gametest.fire_power",
                    0.5D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));

            var fireSchool = SchoolRegistry.FIRE.get();
            var iceSchool = SchoolRegistry.ICE.get();
            var snapshot = ZenithStaffPowerHelper.resolvePowerSnapshot(player);
            helper.assertTrue(snapshot.hasSchoolBonus(), "Zenith Staff should detect the fire school bonus");
            helper.assertTrue(snapshot.isStrongest(fireSchool), "Zenith Staff should treat fire as the strongest school");
            helper.assertTrue(snapshot.bonusPercent() == 50,
                    "Zenith Staff should report +50% school bonus but got " + snapshot.bonusPercent());

            var expectedPower = fireSchool.getPowerFor(player);
            var resolvedIcePower = DivinePossessionPowerHelper.resolveSchoolPower(iceSchool, player);
            helper.assertTrue(Math.abs(resolvedIcePower - expectedPower) < 1.0e-9D,
                    "Zenith Staff should cast ice with fire's strongest school power");

            try (var ignored = ApprenticeCodexServerConfig.useZenithStaffManaCostMultiplierOverrideForGameTest(3.0D)) {
                var iceManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_ice",
                        1,
                        10,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(iceManaEvent);
                helper.assertTrue(iceManaEvent.getManaCost() == 30,
                        "Zenith Staff should triple non-strongest school mana cost");

                var fireManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_fire",
                        1,
                        10,
                        fireSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(fireManaEvent);
                helper.assertTrue(fireManaEvent.getManaCost() == 10,
                        "Zenith Staff should not increase strongest school mana cost");

                var iceSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.ICE_SPIKES_SPELL.get();
                var iceBaseManaCost = iceSpell.getManaCost(1);
                var iceRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(iceBaseManaCost);
                helper.assertTrue(iceRequiredManaCost > iceBaseManaCost,
                        "Zenith Staff pre-cast test needs increased mana cost");

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Zenith Staff pre-cast test could not resolve player mana data");
                magicData.setMana(Math.max(0, iceRequiredManaCost - 1));
                var insufficientPreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(insufficientPreCastEvent);
                helper.assertTrue(insufficientPreCastEvent.isCanceled(),
                        "Zenith Staff should cancel non-strongest school pre-cast when increased mana cost is unaffordable");

                magicData.setMana(iceRequiredManaCost);
                var affordablePreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(affordablePreCastEvent);
                helper.assertFalse(affordablePreCastEvent.isCanceled(),
                        "Zenith Staff should allow non-strongest school pre-cast when increased mana cost is affordable");

                var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
                helper.assertTrue(ZenithStaffPowerHelper.shouldIncreaseManaCost(player, recastSpell.getSchoolType()),
                        "Zenith Staff recast pre-cast test needs mana gate to apply");
                magicData.getPlayerRecasts().addRecast(new RecastInstance(
                        recastSpell.getSpellId(),
                        1,
                        2,
                        100,
                        CastSource.SPELLBOOK,
                        null
                ), magicData);
                var recastRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(recastSpell.getManaCost(1));
                magicData.setMana(Math.max(0, recastRequiredManaCost - 1));
                var recastPreCastEvent = new SpellPreCastEvent(
                        player,
                        recastSpell.getSpellId(),
                        1,
                        recastSpell.getSchoolType(),
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(recastPreCastEvent);
                helper.assertFalse(recastPreCastEvent.isCanceled(),
                        "Zenith Staff should not cancel active recasts with its pre-cast mana gate");

                equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
                var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
                assertZenithPreCastUsesDiscountedManaGate(
                        helper,
                        player,
                        magicData,
                        touchDigSpell,
                        CraftsmansDelight.applyManaCostDiscount(touchDigSpell.getManaCost(1), player),
                        "CraftsmansDelight Touch Dig"
                );

                equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()));
                var mysticShieldSpell = SpellRegistry.MYSTIC_SHIELD.get();
                assertZenithPreCastUsesDiscountedManaGate(
                        helper,
                        player,
                        magicData,
                        mysticShieldSpell,
                        jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter
                                .applyManaCostDiscount(mysticShieldSpell.getManaCost(1), player),
                        "ProtectionSpellSupporter Mystic Shield"
                );

                var fireSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
                magicData.setMana(Math.max(0, ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(fireSpell.getManaCost(1)) - 1));
                var strongestPreCastEvent = new SpellPreCastEvent(
                        player,
                        fireSpell.getSpellId(),
                        1,
                        fireSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(strongestPreCastEvent);
                helper.assertFalse(strongestPreCastEvent.isCanceled(),
                        "Zenith Staff should not cancel strongest school pre-cast with its mana multiplier gate");

                player.addEffect(new MobEffectInstance(EffectRegistry.DIVINE_POSSESSION.get(), 100, 0));
                var divineManaEvent = new SpellOnCastEvent(
                        player,
                        "apprenticecodex:zenith_staff_gametest_divine",
                        1,
                        10,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellCast(divineManaEvent);
                helper.assertTrue(divineManaEvent.getManaCost() == 10,
                        "Divine Possession should suppress Zenith Staff's mana cost increase");

                magicData.setMana(Math.max(0, iceRequiredManaCost - 1));
                var divinePreCastEvent = new SpellPreCastEvent(
                        player,
                        iceSpell.getSpellId(),
                        1,
                        iceSchool,
                        CastSource.SPELLBOOK
                );
                ZenithStaffManaCostEvent.onSpellPreCast(divinePreCastEvent);
                helper.assertFalse(divinePreCastEvent.isCanceled(),
                        "Divine Possession should suppress Zenith Staff's pre-cast mana gate");
            }
        });
    }

    static void assertZenithPreCastUsesDiscountedManaGate(
            GameTestHelper helper,
            FakePlayer player,
            MagicData magicData,
            AbstractSpell spell,
            int discountedManaCost,
            String context
    ) {
        helper.assertTrue(ZenithStaffPowerHelper.shouldIncreaseManaCost(player, spell.getSchoolType()),
                context + " test needs Zenith Staff mana gate to apply");
        var rawRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(spell.getManaCost(1));
        var discountedRequiredManaCost = ZenithStaffManaCostEvent.applyZenithManaCostMultiplier(discountedManaCost);
        helper.assertTrue(discountedRequiredManaCost < rawRequiredManaCost,
                context + " test needs a lower discounted Zenith mana cost");

        magicData.setMana(discountedRequiredManaCost);
        var affordablePreCastEvent = new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                1,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        ZenithStaffManaCostEvent.onSpellPreCast(affordablePreCastEvent);
        helper.assertFalse(affordablePreCastEvent.isCanceled(),
                "Zenith Staff should allow " + context + " pre-cast at discounted increased mana cost");

        magicData.setMana(Math.max(0, discountedRequiredManaCost - 1));
        var insufficientPreCastEvent = new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                1,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        ZenithStaffManaCostEvent.onSpellPreCast(insufficientPreCastEvent);
        helper.assertTrue(insufficientPreCastEvent.isCanceled(),
                "Zenith Staff should still cancel " + context + " pre-cast below discounted increased mana cost");
    }

    static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        var level = helper.getLevel();
        var casterPos = createRemoteIsolationOrigin(helper, new BlockPos(0, 14, 0), 768, 96);
        prepareAbsoluteIsolationPlatform(level, casterPos);
        var caster = createSenseEvilPlayer(level, casterPos, "sense_evil_spawner_cube_test");
        var spell = (SenseEvil) SpellRegistry.SENSE_EVIL.get();
        var range = getSenseEvilRange(spell, caster, 1);
        var diagonalOffset = Mth.floor(range * 0.75);

        helper.assertTrue(Math.sqrt(2.0 * diagonalOffset * diagonalOffset) > range,
                "Diagonal test offset must stay outside the old spherical spawner range");

        var zombieCenter = caster.getBoundingBox().getCenter().add(diagonalOffset, 0.0, diagonalOffset);
        prepareAbsoluteIsolationTargetPlatform(level, zombieCenter);
        var zombie = spawnPositionedZombie(level, zombieCenter);
        var spawnerPos = caster.blockPosition().offset(diagonalOffset, 0, diagonalOffset);
        placeZombieSpawner(level, spawnerPos);

        helper.runAtTickTime(5, () -> {
            var highlights = collectSenseEvilHighlights(spell, level, 1, caster);
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(BlockPos.containing(zombie.getBoundingBox().getCenter())), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should still detect entities at the shared diagonal cube offset");
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(spawnerPos), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should detect spawners at the same diagonal cube offset as entities");
            helper.succeed();
        });
    }
    static void apprenticeCurioBonusLootTableContainsAllThreeItems(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAllItems(
                helper,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magic_items/basic_curios_bonus"),
                createEmptyLootParams(helper),
                256,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
    }
    static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
                createChestLootParams(helper),
                2048,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
    }
    static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAllItems(
                helper,
                ResourceLocation.withDefaultNamespace("chests/spawn_bonus_chest"),
                createChestLootParams(helper),
                1,
                List.of(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())
        ));
    }
    static void ironsStructureLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/generic_magic_treasure"),
                createChestLootParams(helper),
                512,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
    }
    static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockedSpells = getNonLootableApprenticeSpells();
            var defaultSpellFilter = new io.redspace.ironsspellbooks.loot.SpellFilter();
            var applicableSpells = defaultSpellFilter.getApplicableSpells();

            for (var blockedSpell : blockedSpells) {
                var spellId = blockedSpell.getSpellResource();
                helper.assertTrue(!blockedSpell.allowLooting(),
                        "Non-lootable apprentice spell unexpectedly allows loot generation: " + spellId);
                helper.assertFalse(applicableSpells.contains(blockedSpell),
                        "Default loot spell filter still contains blocked apprentice spell: " + spellId);
            }
        });
    }
    static void genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableNeverGeneratesBlockedSpells(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/generic_magic_treasure"),
                createChestLootParams(helper),
                512,
                getNonLootableApprenticeSpells()
        ));
    }
    static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var item = (io.redspace.ironsspellbooks.item.UniqueSpellBook) stack.getItem();
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                    "Isekai Travel Guidebook did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Isekai Travel Guidebook spell container is null");
            helper.assertTrue(spellContainer.getMaxSpellCount() == 2,
                    "Isekai Travel Guidebook spell slot count mismatch: " + spellContainer.getMaxSpellCount());

            var firstSpell = spellContainer.getSpellAtIndex(0);
            var secondSpell = spellContainer.getSpellAtIndex(1);
            helper.assertTrue(firstSpell != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Isekai Travel Guidebook first spell is empty");
            helper.assertTrue(secondSpell != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Isekai Travel Guidebook second spell is empty");
            helper.assertTrue(firstSpell.getSpell() == SpellRegistry.HEALING_BLOOM.get(),
                    "Isekai Travel Guidebook first spell mismatch: " + firstSpell.getSpell().getSpellResource());
            helper.assertTrue(secondSpell.getSpell() == SpellRegistry.COMPANION_TRUNK.get(),
                    "Isekai Travel Guidebook second spell mismatch: " + secondSpell.getSpell().getSpellResource());

            var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0));
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    pig,
                    0,
                    false,
                    true
            );
            var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
            helper.assertTrue(modifiers.isEmpty(),
                    "Isekai Travel Guidebook should not add spellbook attributes: " + describeModifiers(modifiers));
        });
    }

    static void spellStainedRunicTabletUsesDefaultServerConfigValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
            var stack = createSpellStainedRunicTabletStack(
                    helper,
                    spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON),
                    spellEntry(SpellRegistry.FROST_RUNE.get(), SpellRarity.UNCOMMON),
                    spellEntry(SpellRegistry.THERMAL_PROCESS.get(), SpellRarity.RARE),
                    spellEntry(SpellRegistry.FORCE_FIELD.get(), SpellRarity.EPIC),
                    spellEntry(SpellRegistry.PALETTE_SHIFT.get(), SpellRarity.LEGENDARY),
                    spellEntry(SpellRegistry.QUICK_ARMS.get(), SpellRarity.COMMON)
            );
            var slotContext = createSpellbookSlotContext(helper);
            var expected = resolveExpectedSpellStainedRunicTabletAttributes(helper, stack);

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    expected.maxMana(),
                    AttributeModifier.Operation.ADDITION,
                    "Spell-stained Runic Tablet max mana default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(),
                    expected.generalSpellPower(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet general spell power default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(),
                    expected.cooldownReduction(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet cooldown reduction default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(),
                    expected.castTimeReduction(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet cast time reduction should not start below duplicate threshold"
            );
            for (var entry : expected.schoolSpellPower().entrySet()) {
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        entry.getKey(),
                        entry.getValue(),
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet school spell power default config mismatch"
                );
            }
        });
    }

    static void spellStainedRunicTabletAcceptsNegativeServerConfigValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var values = new SpellStainedRunicTabletServerConfig.Values(
                    sameRarityBonuses(-2.0D),
                    sameRarityBonuses(-0.07D),
                    0.05D,
                    0.05D,
                    sameRarityBonuses(-0.08D),
                    new SpellStainedRunicTabletServerConfig.ScalingBonus(1, -0.25D, 0.0D),
                    new SpellStainedRunicTabletServerConfig.ScalingBonus(1, -0.50D, 0.0D)
            );

            try (var ignored = ApprenticeCodexServerConfig.useSpellStainedRunicTabletConfigOverrideForGameTest(values)) {
                var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
                var stack = createSpellStainedRunicTabletStack(
                        helper,
                        spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON)
                );
                var slotContext = createSpellbookSlotContext(helper);

                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                        -2.0D,
                        AttributeModifier.Operation.ADDITION,
                        "Spell-stained Runic Tablet negative max mana config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(),
                        -0.08D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative general spell power config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(),
                        -0.25D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative cooldown reduction config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(),
                        -0.50D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative cast time reduction config mismatch"
                );
                assertSpellStainedRunicTabletSchoolPower(
                        helper,
                        item,
                        slotContext,
                        stack,
                        SpellRegistry.HIGANBANA.get(),
                        -0.07D
                );
            }
        });
    }

    static void spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellStainedRunicTabletThresholdScenario(helper, 0.04D, 0.05D, 0.05D, 0.0D,
                    "Spell-stained Runic Tablet should ignore positive school spell power below threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, 0.05D, 0.05D, 0.05D, 0.05D,
                    "Spell-stained Runic Tablet should apply positive school spell power at threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, -0.04D, 0.05D, 0.05D, 0.0D,
                    "Spell-stained Runic Tablet should ignore negative school spell power below threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, -0.05D, 0.05D, 0.05D, -0.05D,
                    "Spell-stained Runic Tablet should apply negative school spell power at threshold");
        });
    }

    static void assertSpellStainedRunicTabletThresholdScenario(
            GameTestHelper helper,
            double configuredSchoolSpellPower,
            double minimumAppliedPositiveBonus,
            double minimumAppliedNegativePenalty,
            double expectedSchoolSpellPower,
            String message
    ) {
        var values = new SpellStainedRunicTabletServerConfig.Values(
                sameRarityBonuses(0.0D),
                sameRarityBonuses(configuredSchoolSpellPower),
                minimumAppliedPositiveBonus,
                minimumAppliedNegativePenalty,
                sameRarityBonuses(0.0D),
                new SpellStainedRunicTabletServerConfig.ScalingBonus(99, 0.0D, 0.0D),
                new SpellStainedRunicTabletServerConfig.ScalingBonus(99, 0.0D, 0.0D)
        );

        try (var ignored = ApprenticeCodexServerConfig.useSpellStainedRunicTabletConfigOverrideForGameTest(values)) {
            var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
            var stack = createSpellStainedRunicTabletStack(
                    helper,
                    spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON)
            );
            assertSpellStainedRunicTabletSchoolPower(
                    helper,
                    item,
                    createSpellbookSlotContext(helper),
                    stack,
                    SpellRegistry.HIGANBANA.get(),
                    expectedSchoolSpellPower,
                    message
            );
        }
    }

    static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            explorersCodexStack.setHoverName(Component.literal("Codex transfer check"));
            explorersCodexStack.setRepairCost(7);
            var expectedUpgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    explorersCodexStack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.OFFHAND.getName()
            );
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.UNBREAKING, 1), explorersCodexStack);

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Explorer's Codex + Isekai Travel Guidebook should match the transfer recipe");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ItemRegistry.EXPLORERS_CODEX.get()),
                    "Transfer recipe should return Explorer's Codex but got " + ForgeRegistries.ITEMS.getKey(result.getItem()));
            helper.assertTrue("Codex transfer check".equals(result.getHoverName().getString()),
                    "Explorer's Codex custom name was not preserved: " + result.getHoverName().getString());
            helper.assertTrue(result.getBaseRepairCost() == 7,
                    "Explorer's Codex repair cost was not preserved: " + result.getBaseRepairCost());
            helper.assertTrue(UpgradeData.getUpgradeData(result).equals(expectedUpgradeData),
                    "Explorer's Codex upgrade data was not preserved: " + UpgradeData.getUpgradeData(result));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, result) == 1,
                    "Explorer's Codex enchantments were not preserved");

            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Transferred Explorer's Codex slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            assertSpellData(helper, resultSpellContainer, 0, SpellRegistry.ASSIST_WINGS.get(), 1, true, "Transferred Explorer's Codex first spell mismatch");
            assertSpellData(helper, resultSpellContainer, 1, SpellRegistry.TERRA_RESONANCE.get(), 1, true, "Transferred Explorer's Codex second spell mismatch");
            assertSpellData(helper, resultSpellContainer, 2, SpellRegistry.SENSE_EVIL.get(), 1, true, "Transferred Explorer's Codex third spell mismatch");
            assertSpellData(helper, resultSpellContainer, 3, SpellRegistry.REMOTE_EYE.get(), 1, true, "Transferred Explorer's Codex fourth spell mismatch");
            assertSpellData(helper, resultSpellContainer, 4, SpellRegistry.HEALING_BLOOM.get(), 1, true, "Transferred Explorer's Codex transferred Healing Bloom mismatch");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true, "Transferred Explorer's Codex transferred Companion Trunk mismatch");
        });
    }
    static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(5);
            helper.assertTrue(mutable.addSpell(SpellRegistry.HEALING_BLOOM.get(), 1, true),
                    "Failed to prepare duplicate Healing Bloom on Explorer's Codex");
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Recipe should still match when one guidebook spell is already present");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Duplicate-ignore result slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getIndexForSpell(SpellRegistry.HEALING_BLOOM.get()) == 4,
                    "Healing Bloom should remain single and keep its prepared slot");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true,
                    "Companion Trunk should still be transferred when Healing Bloom is already present");
        });
    }
    static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(14);
            fillSpellContainerToActiveCount(helper, mutable, 14);
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertFalse(recipe.matches(craftingContainer, helper.getLevel()),
                    "Recipe should reject Explorer's Codex when transferred spells would exceed 15 slots");
            helper.assertTrue(recipe.assemble(craftingContainer, helper.getLevel().registryAccess()).isEmpty(),
                    "Overflow recipe assembly should return empty result");
        });
    }
    static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);

            var rejected = inventory.insertItem(0, new ItemStack(Items.DIAMOND), false);
            helper.assertTrue(!rejected.isEmpty() && rejected.is(Items.DIAMOND),
                    "Archivist's Grimoire should reject non-scroll insertion");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty(),
                    "Rejected non-scroll should not remain in Archivist's Grimoire inventory");

            inventory.setStackInSlot(5, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            var restoredInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            assertScrollSpell(
                    helper,
                    restoredInventory.getStackInSlot(5),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Archivist's Grimoire lost persisted scroll data"
            );

            restoredInventory.setStackInSlot(6, new ItemStack(Items.DIAMOND));
            var cleanedInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            helper.assertTrue(cleanedInventory.getStackInSlot(6).isEmpty(),
                    "Archivist's Grimoire should purge non-scroll stacks from saved inventory data");
        });
    }
    static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 2 + 4,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 5 + 1,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            helper.assertTrue(ArchivistsGrimoire.ensureSelectedRowHasScroll(grimoireStack),
                    "Archivist's Grimoire should find a populated row from an empty selected row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire should normalize empty selected row to the first populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1),
                    "Archivist's Grimoire should move forward to the next populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire forward navigation skipped the wrong rows");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1),
                    "Archivist's Grimoire should wrap forward to the first populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire forward wrap did not land on the populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, -1),
                    "Archivist's Grimoire should wrap backward to the last populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire backward wrap did not land on the populated row");

            helper.assertFalse(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 0),
                    "Archivist's Grimoire should ignore zero row delta");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire zero delta should not change the selected row");
        });
    }
    static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT + 8,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 4 + 3,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 0 mismatch"
            );
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 8),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 8 mismatch"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3) == SpellData.EMPTY,
                    "Archivist's Grimoire should not expose spells from another row");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, -1) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for negative visible slots");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, ArchivistsGrimoire.COLUMN_COUNT) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for out-of-range visible slots");

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(),
                    1,
                    "Archivist's Grimoire should expose the newly selected row"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0) == SpellData.EMPTY,
                    "Archivist's Grimoire should hide previous row spells after row change");
        });
    }

    static void archivistsGrimoireLockedRowsHideLegacyScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 2,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            ArchivistsGrimoire.setSelectedRow(grimoireStack, 2);

            helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(grimoireStack) == 1,
                    "NBT-less Archivist's Grimoire should start with one unlocked row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 0,
                    "Locked selected rows should normalize into the unlocked row range");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0) == SpellData.EMPTY,
                    "Locked-row legacy scrolls must not be exposed as usable spells");
            helper.assertFalse(ArchivistsGrimoire.ensureSelectedRowHasScroll(grimoireStack),
                    "Locked-row legacy scrolls must not make the selected row usable");
            helper.assertTrue(ArchivistsGrimoire.hasScrollInLockedSlot(grimoireStack),
                    "Locked-row legacy scroll should be detectable for warnings");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_locked_slot_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, grimoireStack);
            var menu = new jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireMenu(
                    0,
                    player.getInventory(),
                    InteractionHand.MAIN_HAND
            );
            var lockedSlot = menu.getSlot(ArchivistsGrimoire.COLUMN_COUNT * 2);
            helper.assertFalse(lockedSlot.mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())),
                    "Locked Archivist's Grimoire slots should reject new scroll insertion");
            helper.assertTrue(lockedSlot.mayPickup(player),
                    "Locked Archivist's Grimoire slots should still allow legacy scroll extraction");
        });
    }

    static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Empty Archivist's Grimoire should show the special inscription hint");

            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, false,
                    "Archivist's Grimoire with a stored spell should hide the special inscription hint");

            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2, ItemStack.EMPTY);
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Archivist's Grimoire should show the special inscription hint again after becoming empty");
        });
    }

    static void archivistsGrimoireTooltipWarnsAboutLockedScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT + 2,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );

            assertTooltipTranslationKey(
                    helper,
                    grimoireStack,
                    "item.apprenticecodex.archivists_grimoire.tooltip.warning_legacy_slot",
                    true,
                    "Archivist's Grimoire should warn when a locked slot stores a scroll"
            );

            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2, ItemStack.EMPTY);
            assertTooltipTranslationKey(
                    helper,
                    grimoireStack,
                    "item.apprenticecodex.archivists_grimoire.tooltip.warning_legacy_slot",
                    false,
                    "Archivist's Grimoire should remove the locked-slot warning after the scroll is removed"
            );
        });
    }

    static void archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_upgrade_test");
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );

            var menu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    grimoireStack,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
            );
            var result = menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
            helper.assertFalse(result.isEmpty(), "Archivist's Grimoire row upgrade should produce a result");
            helper.assertTrue(ArchivistsGrimoire.getUpgradeCount(result) == 1,
                    "Archivist's Grimoire row upgrade should increment the upgrade count");
            helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(result) == 2,
                    "Archivist's Grimoire row upgrade should unlock exactly one additional row");
            assertScrollSpell(
                    helper,
                    new ArchivistsGrimoire.ScrollInventory(result).getStackInSlot(ArchivistsGrimoire.COLUMN_COUNT),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Archivist's Grimoire row upgrade should preserve stored scrolls"
            );
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty()
                            && menu.getSlot(1).getItem().isEmpty()
                            && menu.getSlot(2).getItem().isEmpty(),
                    "Archivist's Grimoire row upgrade should consume all three inputs");

            var maxedGrimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(maxedGrimoireStack, 5);
            var maxedMenu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    maxedGrimoireStack,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
            );
            helper.assertTrue(maxedMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Maxed Archivist's Grimoire should not expose another upgrade result");
            helper.assertTrue(maxedMenu.isBlockedByArchivistsGrimoireMaxSlotReached(),
                    "Maxed Archivist's Grimoire should expose a max-slot warning state");
        });
    }

    static void archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useArchivistsGrimoireConfigOverrideForGameTest(
                    new ArchivistsGrimoireServerConfig.Values(4, 2)
            )) {
                var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                helper.assertTrue(ApprenticeCodexServerConfig.archivistsGrimoireInitialRows() == 4,
                        "Archivist's Grimoire initialRows override mismatch");
                helper.assertTrue(ApprenticeCodexServerConfig.archivistsGrimoireEffectiveMaxRows() == 4,
                        "Archivist's Grimoire maxRows lower than initialRows should resolve to initialRows");
                helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(grimoireStack) == 4,
                        "Archivist's Grimoire should use the resolved row bounds");
                helper.assertFalse(ArchivistsGrimoire.canUpgrade(grimoireStack),
                        "Archivist's Grimoire should not upgrade when resolved maxRows equals initialRows");
            }
        });
    }

    static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var spellbookTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT)
            );
            helper.assertTrue(grimoireStack.is(spellbookTag),
                    "Archivist's Grimoire should be tagged for the Curios spellbook slot");
            assertUpgradeable(helper, grimoireStack,
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");

            var item = (ArchivistsGrimoire) ItemRegistry.ARCHIVISTS_GRIMOIRE.get();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    grimoireStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    200.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Archivist's Grimoire spellbook-slot max mana bonus regression"
            );

            var wrongSlotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.CHARM,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 1)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    wrongSlotContext,
                    grimoireStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    0.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Archivist's Grimoire should not add spellbook max mana outside the spellbook slot"
            );
        });
    }
    static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get()));
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 1,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 4,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())
            );
            ArchivistsGrimoire.setSelectedRow(grimoireStack, 3);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_selection_test");
            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoireStack);

            var manager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var spellbookOptions = manager.getSpellsForSlot(io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT);
            helper.assertTrue(spellbookOptions.size() == 2,
                    "Archivist's Grimoire should expose exactly the two spells in the selected row but got " + spellbookOptions.size());
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 1
                                    && option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Magic Missile at visible slot 1");
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 4
                                    && option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Fire Breath at visible slot 4");
            helper.assertFalse(spellbookOptions.stream().anyMatch(option ->
                            option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get()),
                    "Archivist's Grimoire spell selection should not expose spells outside the selected row");
        });
    }

































    static void assertHeldWisdomBlockExperience(
            GameTestHelper helper,
            ServerLevel level,
            BlockState state,
            Item item,
            int baseExperience,
            int expectedExperience,
            String itemName
    ) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), itemName.toLowerCase().replace(' ', '_') + "_wisdom_test"));
        var stack = new ItemStack(item);
        stack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        var event = new BlockEvent.BreakEvent(level, new BlockPos(5, 2, 0), state, player);
        event.setExpToDrop(baseExperience);
        WisdomExperienceDropEvent.onBlockBreak(event);
        helper.assertTrue(event.getExpToDrop() == expectedExperience,
                itemName + " Wisdom should increase block experience from " + baseExperience
                        + " to " + expectedExperience + " but got " + event.getExpToDrop());
    }



    static void assertCraftsmansDelightBasicDiscountOnly(
            GameTestHelper helper,
            FakePlayer player,
            AbstractSpell spell,
            int baseManaCost,
            String spellName
    ) {
        if (!(spell instanceof ICraftsmansDelightAffectedSpell affectedSpell)) {
            helper.fail(spellName + " should opt into CraftsmansDelight support");
            return;
        }

        helper.assertFalse(affectedSpell.isCraftsmansDelightBreakSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight break speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightProcessSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight process speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightCastingMobilityEnabled(),
                spellName + " should keep CraftsmansDelight casting mobility disabled");
        helper.assertTrue(CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spell.getSpellId()),
                spellName + " should be a CraftsmansDelight mana discount target");
        helper.assertTrue(CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell),
                spellName + " should be a CraftsmansDelight cooldown reduction target");

        var manaEvent = new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                1,
                baseManaCost,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        CraftsmansDelightManaCostDiscountEvent.onSpellCast(manaEvent);
        var expectedManaCost = Math.max(1, Math.round(baseManaCost * 0.5f));
        helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                spellName + " mana cost should be reduced to " + expectedManaCost + " but got " + manaEvent.getManaCost());

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                spell.getSpellCooldown(),
                spell,
                player,
                CastSource.SPELLBOOK
        );
        CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown()
                        == CraftsmansDelight.getReducedEffectiveCooldown(spell, player, CastSource.SPELLBOOK),
                spellName + " cooldown should route through the reduced cooldown helper");
    }

































    static void multipurposeStaffrifleServerDenylistBlocksSpecialCastSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            helper.assertFalse(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                    "Multipurpose Staffrifle should not deny Magic Missile by default");

            try (var ignored = ApprenticeCodexServerConfig.useMultipurposeStaffrifleSpellDenylistOverrideForGameTest(
                    List.of(spell.getSpellResource().toString())
            )) {
                helper.assertTrue(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                        "Multipurpose Staffrifle server denylist should deny Magic Missile");
            }

            helper.assertFalse(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                    "Multipurpose Staffrifle server denylist override should be restored");
        });
    }

    static void initialSpellContainerHelperSkipsUnavailablePresetSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var unboundSpell = RegistryObject.<AbstractSpell, AbstractSpell>create(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "missing_initial_spell_for_gametest"),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY,
                    ApprenticeCodex.MODID
            );
            var unboundStack = new ItemStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            InitialSpellContainerHelper.setInitialContainer(unboundStack, 1, true, false, unboundSpell, 1);
            var unboundContainer = ISpellContainer.get(unboundStack);
            helper.assertTrue(unboundContainer != null, "Initial helper should create a container for unbound RegistryObject spells");
            helper.assertTrue(unboundContainer.getActiveSpellCount() == 0,
                    "Initial helper should skip unbound RegistryObject preset spells");

            var stack = new ItemStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            InitialSpellContainerHelper.setInitialContainer(
                    stack,
                    1,
                    true,
                    false,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.none(),
                    1
            );
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Initial helper should still create an empty spell container");
            helper.assertTrue(spellContainer.getActiveSpellCount() == 0,
                    "Initial helper should skip unavailable preset spells");

            var enabledStack = new ItemStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            var spell = SpellRegistry.MANIFESTATION_GRIMOIRE.get();
            InitialSpellContainerHelper.setInitialContainer(enabledStack, 1, true, false, spell, 1);
            var enabledContainer = ISpellContainer.get(enabledStack);
            helper.assertTrue(enabledContainer != null, "Initial helper should create enabled preset spell container");
            assertSpellData(helper, enabledContainer, 0, spell, 1, true,
                    "Initial helper should keep enabled preset spells locked");
        });
    }





    static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_delay_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "Multicast Echo Staff should consume EchoSpell after the normal cast succeeds");
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Normal cast should consume mana before delayed multicast: " + magicData.getMana());
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Normal cast cooldown should be suppressed until multicast finishes");
        });

        helper.runAtTickTime(2, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Multicast Echo Staff should not fire before the configured delay");
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost * 2.0F)) < 1.0e-4F,
                    "Delayed multicast should consume mana once: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Multicast Echo Staff should apply the final penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffCreativeCastSkipsFinalCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_creative_cooldown_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> completeMulticastEchoStaffCast(
                helper.getLevel(),
                player,
                staffStack,
                spell,
                spellLevel,
                0,
                manaCost * 3.0F
        ));
        helper.runAtTickTime(2, () ->
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player))
        );
        helper.runAtTickTime(4, () -> {
            var originalCreativeCooldown = io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get();
            try {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(false);
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            } finally {
                io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
            }
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Multicast Echo Staff creative finalization should respect disabled creative cooldowns");
            helper.succeed();
        });
    }

    static void multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_mana_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 1;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Insufficient mana interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Insufficient mana interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffItemChangeEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_item_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Item change interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Item change interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLogoutEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_logout_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            MulticastEchoStaffCastHelper.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));

            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Logout interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Logout interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_cast_time_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.ARCANE_BLAST.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Multicast Echo Staff should add skipped cast time to final cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_fortify_indicator_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FORTIFY_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var magicData = MagicData.getPlayerMagicData(player);
            var targetAreas = helper.getLevel().getEntitiesOfClass(
                    TargetedAreaEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    targetArea -> !targetArea.isRemoved()
            );

            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Repeated Fortify multicast should clear temporary cast data after sending cast data to the client");
            helper.assertTrue(targetAreas.isEmpty(),
                    "Repeated Fortify multicast should discard temporary target area indicators: " + targetAreas.size());
            helper.succeed();
        });
    }

    static void multicastEchoStaffInvalidInstantCastKeepsEchoSpell(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_invalid_instant_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.ACUPUNCTURE_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        player.setYRot(-90.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 3.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Acupuncture should find the target before it is removed");
            target.discard();
            player.setYRot(90.0F);

            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(player.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "Invalid instant casts should not consume EchoSpell");
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should only pay the normal cast mana cost: " + magicData.getMana());
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should not start delayed multicast: " + magicData.getMana());
            helper.succeed();
        });
    }

    static void multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 40, 0);
        prepareWideSearchIsolationArea(helper, playerPos);
        var player = createEquipmentTestPlayer(helper, playerPos, "multicast_echo_staff_invalid_long_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 41.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should find the elevated target before it is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL.get());
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "Long cast test should remove EchoSpell before the normal cast completes");
            target.discard();
            // 同じ batch の高所テストにいる entity を遅延再詠唱が拾わないよう、空だけを見る.
            player.setYRot(90.0F);
            player.setYHeadRot(90.0F);
            player.setYBodyRot(90.0F);
            player.setXRot(-90.0F);

            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should not spend mana on delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Invalid long cast should keep the normal cooldown instead of the multicast penalty: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 40, 0);
        prepareWideSearchIsolationArea(helper, playerPos);
        var player = createEquipmentTestPlayer(helper, playerPos, "multicast_echo_staff_long_context_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 41.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should store a valid target before EchoSpell is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL.get());
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "Long cast context test should remove EchoSpell before the normal cast completes");

            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Valid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
            magicData.setAdditionalCastData(null);
        });

        helper.succeedWhen(() -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost * 2.0F)) < 1.0e-4F,
                    "Valid long cast should use the start EchoSpell context for delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Valid long cast should apply the multicast penalty cooldown from the start context: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Delayed multicast pre-cast data should be restored after the repeated cast");
        });
    }

    static void multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_duration_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    MulticastEchoStaffMobEffectProfile.DEFAULT_DURATION_EXTENSION
            ))) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 0))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getDuration() == 120,
                    "Duplicate multicast mob effect should extend duration by 50% of attempted duration: "
                            + (result == null ? "null" : result.getDuration()));
        });
    }

    static void multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_amp_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            var profile = new MulticastEchoStaffMobEffectProfile(0.0D, 0, 6000, 0.5D, 0, 2);
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 1))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getAmplifier() == 2,
                    "Duplicate multicast mob effect should stack amplifier from attempted level: "
                            + (result == null ? "null" : result.getAmplifier()));
        });
    }

    static void multicastEchoStaffMobEffectProfileIgnoresMissingProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_missing_profile_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of())) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 0))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getDuration() == 100,
                    "Missing multicast mob effect profile should leave vanilla duplicate handling unchanged: "
                            + (result == null ? "null" : result.getDuration()));
        });
    }

    static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_direct_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.SHOCK.get();
            var profile = new MulticastEchoStaffAttackProfile(0.25D, true, false, true, 0, 1);
            var source = CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.SHOCK);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> {
                            target.invulnerableTime = 20;
                            CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                        }
                );
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 4.0F,
                    "Direct CombatTools multicast damage should use the profile multiplier before resistance: " + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Direct CombatTools multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }

    static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_projectile_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.MANA_SLASH.get();
            var profile = new MulticastEchoStaffAttackProfile(0.5D, true, true, false, 20, 1);
            var projectile = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), helper.getLevel(), player);
            var source = CombatTools.getDamageSource(helper.getLevel(), projectile, player, DamageTypes.MANA_SLASH);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> helper.getLevel().addFreshEntity(projectile)
                );
                target.invulnerableTime = 20;
                CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 6.0F,
                    "Tracked projectile multicast damage should use the profile multiplier after cast context closes: "
                            + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Tracked projectile multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }

    static void multicastEchoStaffCooldownCapLimitsAdjustedCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_cap_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        var cooldownCapTicks = 300;
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            override[0] = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    cooldownCapTicks,
                    10
            );
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            try {
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                var magicData = MagicData.getPlayerMagicData(player);
                var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());

                helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == cooldownCapTicks,
                        "Multicast Echo Staff adjusted cooldown should be capped: "
                                + (cooldown == null ? "null" : cooldown.getSpellCooldown()));
                helper.succeed();
            } finally {
                if (override[0] != null) {
                    override[0].close();
                }
            }
        });
    }

    static void multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_base_above_cap_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        var cooldownCapTicks = 10;
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            override[0] = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    cooldownCapTicks,
                    10
            );
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            try {
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                var magicData = MagicData.getPlayerMagicData(player);
                var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
                var expectedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);

                helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                        "Multicast Echo Staff should preserve original cooldowns above the cap: "
                                + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                                + " / expected " + expectedCooldown);
                helper.succeed();
            } finally {
                if (override[0] != null) {
                    override[0].close();
                }
            }
        });
    }

    static void echoCastStopsAtConfiguredMulticastLimit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    12000,
                    1
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "echo_cast_max_limit_test");
                var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
                var spell = SpellRegistry.ECHO_CAST.get();
                var magicData = MagicData.getPlayerMagicData(player);
                player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

                helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "Echo Cast should allow the cast that reaches the configured maximum");
                spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
                var effect = player.getEffect(EffectRegistry.ECHO_SPELL.get());
                helper.assertTrue(effect != null && effect.getAmplifier() == 0,
                        "Echo Cast should store the maximum amplifier for a one-cast limit");
                helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "Echo Cast should reject casts while already at the configured maximum");
            }
        });
    }

    static void prepareMulticastEchoStaffCast(
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.setPlayerCastingItem(staffStack);
        magicData.setMana(mana);
        player.addEffect(new MobEffectInstance(EffectRegistry.ECHO_SPELL.get(), 200, amplifier));
    }

    static void completeMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        beginMulticastEchoStaffCast(level, player, staffStack, spell, spellLevel, amplifier, mana);
        finishStartedSpellCast(level, player, spell, spellLevel);
    }

    static void beginMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        prepareMulticastEchoStaffCast(player, staffStack, spell, spellLevel, amplifier, mana);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(staffStack);
        spell.onServerPreCast(level, spellLevel, player, magicData);
    }

    static void finishStartedSpellCast(
            ServerLevel level,
            FakePlayer player,
            AbstractSpell spell,
            int spellLevel
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        spell.castSpell(level, spellLevel, player, CastSource.SPELLBOOK, true);
        spell.onServerCastComplete(level, spellLevel, player, magicData, false);
    }

    static int expectedMulticastEchoStaffCooldown(
            AbstractSpell spell,
            ServerPlayer player,
            CastSource castSource,
            int amplifier
    ) {
        var baseCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        var cooldownCapTicks = ApprenticeCodexServerConfig.multicastEchoStaffCooldownCapTicks();
        if (baseCooldown > cooldownCapTicks) {
            return baseCooldown;
        }

        var cooldownComponent = (amplifier + 2)
                * ApprenticeCodexServerConfig.multicastEchoStaffCooldownMultiplier()
                * baseCooldown;
        var castTimeComponent = (amplifier + 1)
                * ApprenticeCodexServerConfig.multicastEchoStaffCastTimeCooldownMultiplier()
                * spell.getEffectiveCastTime(1, player);
        return Math.min(cooldownCapTicks, (int) Math.ceil(cooldownComponent + castTimeComponent));
    }











    static CircuitHeatStaffBypassTestContext createCircuitHeatStaffBypassTestContext(
            GameTestHelper helper,
            String playerName,
            AbstractSpell spell
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), playerName);
        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Circuit Heat Staff config test could not resolve player mana data");
        io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.SPELLBOOK);
        return new CircuitHeatStaffBypassTestContext(player, staffStack, magicData, spell);
    }

    static void postCircuitHeatStaffSpellOnCastEvent(CircuitHeatStaffBypassTestContext context, int manaCost) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                context.player(),
                context.spell().getSpellId(),
                1,
                manaCost,
                context.spell().getSchoolType(),
                CastSource.SPELLBOOK
        ));
    }

    record CircuitHeatStaffBypassTestContext(
            ServerPlayer player,
            ItemStack staffStack,
            MagicData magicData,
            AbstractSpell spell
    ) {
    }











    static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState();
            var pos = new BlockPos(0, 2, 0);
            var shape = state.getShape(level, pos, CollisionContext.empty());
            helper.assertTrue(shape.isEmpty(),
                    "Healing Bloom light outline should be empty so it cannot be removed by hand");
            helper.assertTrue(state.getLightEmission(level, pos) == 11,
                    "Healing Bloom light should now emit light level 11");
        });
    }
    static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var pos = new BlockPos(0, 2, 0);
            helper.setBlock(pos, BlockRegistry.HEALING_BLOOM_LIGHT.get());

            for (int i = 0; i < 25; ++i) {
                if (level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get())) {
                    var blockEntity = helper.getBlockEntity(pos);
                    helper.assertTrue(blockEntity instanceof HealingBloomLightBlockEntity,
                            "Healing Bloom light is missing its block entity");
                    HealingBloomLightBlockEntity.serverTick(
                            level,
                            pos,
                            level.getBlockState(pos),
                            (HealingBloomLightBlockEntity) blockEntity
                    );
                }
            }

            helper.assertTrue(!level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get()),
                    "Healing Bloom light should self-clean without a bloom");
        });
    }
    static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_owner_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(new BlockPos(0, 2, 0));
            helper.assertTrue(bloom.shouldBeSaved(), "Healing Bloom should now be saved with the world");
            helper.assertTrue(bloom.hurt(level.damageSources().playerAttack(owner), 2.0f),
                    "Healing Bloom should now accept damage from its owner");
        });
    }
    static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
        var level = helper.getLevel();
        var relativeAnchorPos = new BlockPos(0, 2, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_root_loss_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(bloom);

        helper.runAtTickTime(1, () -> helper.setBlock(relativeAnchorPos.below(), Blocks.AIR));
        helper.runAtTickTime(3, () -> {
            var blooms = level.getEntitiesOfClass(HealingBloomEntity.class, new net.minecraft.world.phys.AABB(anchorPos).inflate(1.5));
            helper.assertTrue(!blooms.isEmpty(),
                    "Healing Bloom should remain as a dead entity for a short time instead of silently disappearing when its root is lost");
            helper.assertTrue(blooms.stream().allMatch(entity -> !entity.isAlive() || entity.isDeadOrDying()),
                    "Healing Bloom should enter its death state when its root is lost");
            helper.succeed();
        });
    }
    static void healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeAnchorPos = new BlockPos(0, 2, 0);
            var anchorPos = helper.absolutePos(relativeAnchorPos);
            helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_death_drop_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(anchorPos);
            bloom.setBloomMaxHealth(10.0f);
            setHealingBloomFruitCount(bloom, 3);
            bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(bloom);

            killHealingBloom(level, bloom);

            helper.assertTrue(!level.getBlockState(anchorPos).is(BlockRegistry.COMFORT_BERRY_BUSH.get()),
                    "Healing Bloom death should not plant a Comfort Berry Bush");
            helper.assertTrue(countFreshItemDrops(level, ItemRegistry.COMFORT_BERRIES.get(), anchorPos, 1.5D) == 3,
                    "Healing Bloom death should drop exactly the stored Comfort Berries");
        });
    }
    static void healingBloomImmediateDeathDropsNothingAndPlantsNoBush(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeAnchorPos = new BlockPos(0, 2, 0);
            var anchorPos = helper.absolutePos(relativeAnchorPos);
            helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_immediate_death_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(anchorPos);
            bloom.setBloomMaxHealth(10.0f);
            bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(bloom);

            killHealingBloom(level, bloom);

            helper.assertTrue(!level.getBlockState(anchorPos).is(BlockRegistry.COMFORT_BERRY_BUSH.get()),
                    "Immediate Healing Bloom death should not plant a Comfort Berry Bush");
            helper.assertTrue(countFreshItemDrops(level, ItemRegistry.COMFORT_BERRIES.get(), anchorPos, 1.5D) == 0,
                    "Immediate Healing Bloom death should not drop Comfort Berries before fruit has grown");
        });
    }
    static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        // 同 batch の他 Healing Bloom から再生オーラを受けないよう、高所へ隔離する。
        var relativeAnchorPos = new BlockPos(0, 20, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        prepareHighIsolationPlatform(helper, relativeAnchorPos);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_regen_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.setHealth(5.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(bloom);

        helper.runAtTickTime(45, () -> {
            helper.assertFalse(bloom.hasEffect(MobEffects.REGENERATION),
                    "Healing Bloom should not grant its own regeneration effect to itself");
            helper.assertTrue(Math.abs(bloom.getHealth() - 5.0f) < 0.01f,
                    "Healing Bloom should not recover before its low-speed natural heal ticks");
        });
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(Math.abs(bloom.getHealth() - 6.0f) < 0.01f,
                    "Healing Bloom should recover exactly one point from low-speed natural healing after 80 ticks: "
                            + bloom.getHealth());
            helper.succeed();
        });
    }
    static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB);

            castHealingBloom(helper, owner, 1, anchorPos, false);

            var bloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(Math.abs(bloom.getY() - (helper.absolutePos(anchorPos).below().getY() + 0.5)) < 0.01,
                    "Healing Bloom should now sit on top of a slab support instead of refusing the placement");
        });
    }
    static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_recast_same_owner_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, owner, 1, firstAnchor, false);
            var firstBloom = getSingleLivingHealingBloom(helper, owner);

            castHealingBloom(helper, owner, 1, secondAnchor, false);

            var blooms = getOwnedHealingBlooms(helper, owner);
            helper.assertTrue(blooms.size() == 1,
                    "Healing Bloom should still allow only one active bloom for the same owner");
            helper.assertTrue(blooms.get(0) == firstBloom,
                    "Healing Bloom should keep the original bloom when recast without sneaking");
            helper.assertTrue(firstBloom.blockPosition().equals(helper.absolutePos(firstAnchor)),
                    "Healing Bloom should remain at the original anchor after a blocked recast");
        });
    }
    static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, firstOwner).size() == 1,
                    "The first owner should keep exactly one Healing Bloom");
            helper.assertTrue(getOwnedHealingBlooms(helper, secondOwner).size() == 1,
                    "A different owner should be able to place a separate Healing Bloom");
        });
    }
    static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_missing_state_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);

            var spellData = Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null,
                    "Healing Bloom stale-state test could not resolve spell data capability");
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, state -> state.setBloomUuid(UUID.randomUUID()));

            castHealingBloom(helper, owner, 1, anchorPos, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, owner).size() == 1,
                    "A missing managed Healing Bloom should not block recasting for the same owner");
        });
    }
    static void healingBloomOfflineDeathDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_offline_death_recast_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            var deadBloomUuid = prepareHealingBloomDeadWhileOwnerOffline(helper, owner, firstAnchor);

            castHealingBloom(helper, owner, 1, secondAnchor, false);

            var currentBloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(secondAnchor)),
                    "A Healing Bloom that died while its owner was offline should not block normal recasting");
            assertManagedHealingBloomUuid(helper, owner, currentBloom.getUUID(),
                    "Healing Bloom stale state should be replaced by the newly placed bloom");
            helper.assertTrue(!deadBloomUuid.equals(currentBloom.getUUID()),
                    "The recast Healing Bloom should not reuse the dead bloom UUID");
        });
    }
    static void healingBloomSneakCastRecoversOfflineDeathState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_offline_death_sneak_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var replacementAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(replacementAnchor.below(), Blocks.STONE);

            var deadBloomUuid = prepareHealingBloomDeadWhileOwnerOffline(helper, owner, firstAnchor);

            castHealingBloom(helper, owner, 1, replacementAnchor, true);

            var currentBloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(replacementAnchor)),
                    "Sneak casting should recover a stale Healing Bloom state left by offline death");
            assertManagedHealingBloomUuid(helper, owner, currentBloom.getUUID(),
                    "Sneak casting should replace the stale Healing Bloom UUID with the new bloom UUID");
            helper.assertTrue(!deadBloomUuid.equals(currentBloom.getUUID()),
                    "The replacement Healing Bloom should not reuse the dead bloom UUID");
        });
    }
    static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_force_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_force_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var replacementAnchor = new BlockPos(2, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(replacementAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            var previousBloom = getSingleLivingHealingBloom(helper, firstOwner);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);
            var otherOwnersBloom = getSingleLivingHealingBloom(helper, secondOwner);

            castHealingBloom(helper, firstOwner, 1, replacementAnchor, true);

            var currentBloom = getSingleLivingHealingBloom(helper, firstOwner);
            helper.assertTrue(currentBloom != previousBloom,
                    "Sneak casting should create a new Healing Bloom for the owner");
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(replacementAnchor)),
                    "The replacement Healing Bloom should appear at the new anchor");
            helper.assertTrue(!previousBloom.isAlive() || previousBloom.isDeadOrDying(),
                    "The previous Healing Bloom should enter its death state after the replacement bloom is created");
            helper.assertTrue(otherOwnersBloom.isAlive() && otherOwnersBloom.blockPosition().equals(helper.absolutePos(secondAnchor)),
                    "Replacing your own Healing Bloom should not affect blooms owned by other players");
        });
    }

    static void setHealingBloomFruitCount(HealingBloomEntity bloom, int fruitCount) {
        var tag = new CompoundTag();
        bloom.addAdditionalSaveData(tag);
        tag.putInt("FruitCount", fruitCount);
        bloom.readAdditionalSaveData(tag);
    }

    static UUID prepareHealingBloomDeadWhileOwnerOffline(GameTestHelper helper, FakePlayer owner, BlockPos anchorPos) {
        castHealingBloom(helper, owner, 1, anchorPos, false);
        var bloom = getSingleLivingHealingBloom(helper, owner);
        var bloomUuid = bloom.getUUID();
        assertManagedHealingBloomUuid(helper, owner, bloomUuid,
                "Healing Bloom offline-death setup should start with a managed bloom");

        // オフライン中の死亡では owner を ServerPlayer として引けず、即時解除されない state が残る。
        clearHealingBloomCachedOwner(bloom);
        killHealingBloom(helper.getLevel(), bloom);
        assertManagedHealingBloomUuid(helper, owner, bloomUuid,
                "Healing Bloom state should still contain the dead bloom UUID until the owner recasts");
        return bloomUuid;
    }

    static void clearHealingBloomCachedOwner(HealingBloomEntity bloom) {
        var tag = new CompoundTag();
        bloom.addAdditionalSaveData(tag);
        bloom.readAdditionalSaveData(tag);
    }

    static void assertManagedHealingBloomUuid(GameTestHelper helper, FakePlayer owner, UUID expectedUuid, String message) {
        var spellData = Capabilities.getSpellDataOrNull(owner);
        helper.assertTrue(spellData != null, "Healing Bloom state assertion could not resolve spell data capability");
        helper.assertTrue(expectedUuid.equals(spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE).getBloomUuid()),
                message);
    }

    static void killHealingBloom(ServerLevel level, HealingBloomEntity bloom) {
        bloom.setHealth(0.0f);
        bloom.die(level.damageSources().genericKill());
    }

    static void archerMultipleTimeoutWithGreaterConjurersTalismanSkipsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createArcherMultiplePlayer(helper, playerPos, "archer_multiple_greater_conjurer_timeout_test");
            equipGreaterConjurersTalisman(player);

            castArcherMultiple(helper, player, 1);

            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "Archer Multiple should create a recast instance on initial cast");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should remain active before timeout completion");

            magicData.getPlayerRecasts().removeRecast(recast, io.redspace.ironsspellbooks.capabilities.magic.RecastResult.TIMEOUT);

            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should be removed after timeout completion");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Greater Conjurer's Talisman should suppress Archer Multiple cooldown when the recast ends by timeout");
        });
    }
    static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 12, 0);
        prepareSummonedEntityIsolationArea(helper, playerPos);
        var player = createArcherMultiplePlayer(helper, playerPos, "archer_multiple_all_bows_removed_test");
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        var removalStarted = new java.util.concurrent.atomic.AtomicBoolean(false);

        helper.runAtTickTime(1, () -> castArcherMultiple(helper, player, 1));
        helper.succeedWhen(() -> {
            if (!removalStarted.get()) {
                var bows = getOwnedArcherMultipleBows(helper, player);
                helper.assertTrue(bows.size() == 4,
                        "Archer Multiple should summon all bows before the removal test starts");
                bows.forEach(bow -> bow.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED));
                removalStarted.set(true);
            }
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should end once every summoned bow has disappeared");
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).isEmpty(),
                    "Archer Multiple bows should all be gone after the forced removal");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Archer Multiple should start its normal cooldown when every summoned bow disappears");
        });
    }
    static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_vanilla_menu_test");
        var shelfPos = new BlockPos(0, 1, 0);
        placeAndAssertBlockEntity(helper, shelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
        shelf.setShelfData(player, false, Direction.NORTH);
        shelf.setLifeRange(10.0);

        helper.runAtTickTime(1, () -> {
            var personalInventory = player.getCapability(Capabilities.PERSONAL_INVENTORY)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Personal Shelf GameTest"));

            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            for (var slot = 1; slot < 54; ++slot) {
                personalInventory.getHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            player.getInventory().setItem(0, new ItemStack(Items.DIRT));

            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open a menu for its owner");
            helper.assertTrue(player.containerMenu instanceof ChestMenu chestMenu && chestMenu.getRowCount() == 6,
                    "Personal Shelf should expose a vanilla six-row chest menu");
            var chestMenu = (ChestMenu) player.containerMenu;
            helper.assertTrue(chestMenu.getSlot(0).getItem().is(Items.DIAMOND),
                    "Personal Shelf chest menu should read from the opener's personal inventory");

            var quickMoved = chestMenu.quickMoveStack(player, 81);
            helper.assertTrue(quickMoved.isEmpty(),
                    "Full Personal Shelf quick move should fail cleanly instead of looping");
            helper.assertTrue(player.getInventory().getItem(0).is(Items.DIRT),
                    "Failed Personal Shelf quick move should leave the player's stack in place");
            helper.succeed();
        });
    }

    static void personalShelfSynchronizesExportModeBlockState(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(1, 2, 1), "personal_shelf_export_state_test");
        var normalShelfPos = new BlockPos(0, 1, 0);
        var exportShelfPos = new BlockPos(2, 1, 0);

        placeAndAssertBlockEntity(helper, normalShelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(),
                BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        placeAndAssertBlockEntity(helper, exportShelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(),
                BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());

        var normalShelf = getPersonalShelfBlockEntity(helper, helper.absolutePos(normalShelfPos));
        normalShelf.setShelfData(player, false, Direction.NORTH);
        normalShelf.setLifeRange(10.0);

        var exportShelf = getPersonalShelfBlockEntity(helper, helper.absolutePos(exportShelfPos));
        exportShelf.setShelfData(player, true, Direction.NORTH);
        exportShelf.setLifeRange(10.0);

        helper.runAtTickTime(1, () -> {
            helper.assertBlockProperty(normalShelfPos, PersonalShelfChestBlock.EXPORT_MODE, false);
            helper.assertBlockProperty(exportShelfPos, PersonalShelfChestBlock.EXPORT_MODE, true);
            helper.succeed();
        });
    }

    static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_expire_close_test");
        var shelfPos = new BlockPos(0, 1, 0);
        var absoluteShelfPos = castPersonalShelf(helper, player, shelfPos, false, Direction.NORTH);
        helper.runAtTickTime(1, () -> {
            var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open before the expiration check");
            helper.assertTrue(player.containerMenu instanceof ChestMenu,
                    "Personal Shelf should still be using ChestMenu during the expiration check");
            // 現在の Personal Shelf は tick 寿命ではなく、所有者が維持範囲外に出た時に失効する。
            var awayPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(10, 2, 0)));
            player.setPos(awayPos.x, awayPos.y, awayPos.z);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(player.containerMenu == player.inventoryMenu,
                    "Expired Personal Shelf should close the opened menu");
            helper.assertBlockNotPresent(BlockRegistry.PERSONAL_SHELF_CHEST.get(), shelfPos);
        });
    }
    static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.DIAMOND));
            trunk.moveTo(player.getX() + 5.0, player.getY(), player.getZ(), 0.0f, 0.0f);

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.isAlive(), "Companion Trunk should stay active when recalled with items inside");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 1
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 1,
                    "Companion Trunk should be recalled within one block of the caster");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should keep its items after recall");
        });
    }
    static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.EMERALD));
            trunk.moveTo(player.getX() + 1.0, player.getY(), player.getZ(), 0.0f, 0.0f);
            var before = trunk.position();

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.position().distanceTo(before) < 0.01,
                    "Companion Trunk should not move when recast while already within two blocks");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should stay loaded after the failed dismiss");
        });
    }
    static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
            var trunkPos = new BlockPos(0, 2, 0);
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.DIAMOND, 3));

            trunk.dropAllContentsAndDiscard();

            var chestPos = findCompanionTrunkChest(helper, trunkPos);
            helper.assertTrue(chestPos != null, "Companion Trunk should create a vanilla chest when there is space nearby");
            helper.assertBlockPresent(Blocks.CHEST, chestPos);

            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
            helper.assertTrue(blockEntity instanceof ChestBlockEntity, "Death chest should use the vanilla chest block entity");
            helper.assertTrue(blockEntity instanceof ChestBlockEntity chest && containsItem(chest, Items.DIAMOND, 3),
                    "Death chest should receive the Companion Trunk inventory");
        });
    }
    static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
        var trunkPos = new BlockPos(0, 2, 0);
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    helper.setBlock(trunkPos.offset(x, y, z), Blocks.STONE);
                }
            }
        }

        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 4, 0));
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.EMERALD, 2));

            trunk.dropAllContentsAndDiscard();

            helper.assertTrue(findCompanionTrunkChest(helper, trunkPos) == null,
                    "Companion Trunk should not create a chest when every candidate position is blocked");
            helper.assertItemEntityPresent(Items.EMERALD, trunkPos, 2.5);
        });
    }
    static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        var trunkPos = new BlockPos(0, 2, 0);
        helper.setBlock(trunkPos, Blocks.LAVA);
        var player = createCompanionTrunkPlayer(helper, new BlockPos(1, 2, 0));
        var trunk = createCompanionTrunk(helper, player, trunkPos);
        trunk.setCompanionMaxHealth(10.0f);
        var initialHealth = trunk.getHealth();

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(trunk.isInLava(),
                    "Companion Trunk should be touching lava during the fire immunity test");
            helper.assertTrue(Math.abs(trunk.getHealth() - initialHealth) < 0.0001f,
                    "Companion Trunk should ignore environmental lava damage");
            helper.assertFalse(trunk.isOnFire(),
                    "Companion Trunk should not appear ignited while touching lava");
            helper.assertTrue(trunk.getRemainingFireTicks() <= 0,
                    "Companion Trunk should not gain fire ticks from touching lava");
            var belowWorld = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, -2, 0)));
            trunk.moveTo(belowWorld.x, belowWorld.y, belowWorld.z, 0.0f, 0.0f);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.isAlive(), "Companion Trunk should survive lava and void rescue");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 2
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 2,
                    "Companion Trunk should return near its owner after falling below the world");
        });
    }

    static void companionTrunkClimbsOneBlockStepWhenFollowingOwner(GameTestHelper helper) {
        // GameTest の管理範囲外に tick 必須のエンティティを生成すると、隣接 chunk が entity-ticking にならず、
        // tickCount=0 のまま timeout する flaky テストになり得る。対処はエンティティと移動経路を
        // template が管理する水平範囲の正の相対座標へ収めることであり、setChunkForced 等の
        // chunk 強制 load は使用しない。
        // 強制 load は誤ったテスト配置でも動作させて症状を隠し、本来のテスト環境と異なる条件を作るためである。
        var trunkPos = new BlockPos(0, 12, 2);
        var ownerPos = new BlockPos(4, 13, 2);
        for (var x = 0; x <= 4; ++x) {
            for (var z = 1; z <= 3; ++z) {
                helper.setBlock(new BlockPos(x, 11, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 12, z), x >= 2 ? Blocks.STONE : Blocks.AIR);
                helper.setBlock(new BlockPos(x, 13, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 14, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 15, z), Blocks.AIR);
            }
        }

        var owner = createCompanionTrunkPlayer(helper, ownerPos);
        var trunk = createCompanionTrunk(helper, owner, trunkPos);
        var absoluteOwnerPos = helper.absolutePos(ownerPos);

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(),
                    "Companion Trunk should land on the raised step after following its owner: "
                            + describeCompanionTrunkMovement(trunk));
            helper.assertTrue(trunk.blockPosition().getY() >= absoluteOwnerPos.getY(),
                    "Companion Trunk should climb onto the one block step while following its owner: "
                            + describeCompanionTrunkMovement(trunk));
        });
    }

    static void companionTrunkLandingDoesNotTrampleFarmland(GameTestHelper helper) {
        var farmlandPos = new BlockPos(0, 11, 0);
        var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 12, 1));
        helper.setBlock(farmlandPos, Blocks.FARMLAND);
        helper.setBlock(farmlandPos.above(), Blocks.AIR);
        helper.setBlock(farmlandPos.above(2), Blocks.AIR);
        helper.setBlock(farmlandPos.above(3), Blocks.AIR);
        var trunk = createCompanionTrunk(helper, owner, farmlandPos.above(4));

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(), "Companion Trunk should land on the farmland test block");
            helper.assertBlockPresent(Blocks.FARMLAND, farmlandPos);
        });
    }

    static void riftHoleDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimensionId = helper.getLevel().dimension().location().toString();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(currentDimensionId),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(helper.getLevel().dimension().location()),
                    "RiftHole dimension denylist should reject the current dimension"
            );
        }
        helper.succeed();
    }

    static void riftHoleDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void riftHoleDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "demicreator_wings_dimension_deny_test");
        var spell = SpellRegistry.DEMICREATOR_WINGS.get();
        var magicData = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension denylist should reject the current dimension"
            );
            helper.assertFalse(
                    spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "DemicreatorWings pre-cast should reject a denied dimension"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionRestrictionAllowsCloseCast(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "demicreator_wings_close_dimension_test");
        var spell = (DemicreatorWings) SpellRegistry.DEMICREATOR_WINGS.get();
        var magicData = MagicData.getPlayerMagicData(player);

        try {
            DemicreatorWingsManager.activate(player, 1, CastSource.SPELLBOOK, magicData, spell);
            try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                    List.of(currentDimension.toString()),
                    false,
                    List.of()
            )) {
                helper.assertTrue(
                        spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "DemicreatorWings close cast should remain allowed in a denied dimension"
                );
                helper.assertTrue(
                        magicData.getAdditionalCastData() instanceof DemicreatorWings.DemicreatorWingsCastData castData
                                && castData.isCloseCast(),
                        "DemicreatorWings close cast should mark close cast data"
                );
            }
        } finally {
            DemicreatorWingsManager.deactivate(player, true);
        }
        helper.succeed();
    }

    static void remoteEyeDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "remote_eye_dimension_deny_test");
        var spell = SpellRegistry.REMOTE_EYE.get();
        var magicData = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension denylist should reject the current dimension"
            );
            helper.assertFalse(
                    spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "RemoteEye pre-cast should reject a denied dimension"
            );
        }
        helper.succeed();
    }

    static void remoteEyeDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void remoteEyeDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void remoteEyeSanitizerKeepsStoredLongDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "remote_eye_long_duration_repair_test");
            var activeUntilGameTime = level.getGameTime() + 1200L;

            Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                state.activeUntilGameTime = activeUntilGameTime;
                state.activeDurationTicks = 1200L;
                state.anchorX = player.getX();
                state.anchorY = player.getY();
                state.anchorZ = player.getZ();
                state.anchorYaw = player.getYRot();
                state.anchorPitch = player.getXRot();
            }));

            RemoteEyeBodyControlEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var state = getRemoteEyeState(player);
            helper.assertTrue(state.activeUntilGameTime == activeUntilGameTime,
                    "RemoteEye sanitizer should not clamp active state below the stored cast duration");
        });
    }

    static void remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "remote_eye_legacy_duration_repair_test");
            var expectedActiveUntilGameTime = level.getGameTime() + RemoteEye.PERSISTED_STATE_REPAIR_MAX_ACTIVE_TICKS;

            Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                state.activeUntilGameTime = expectedActiveUntilGameTime + 1200L;
                state.activeDurationTicks = 0L;
                state.anchorX = player.getX();
                state.anchorY = player.getY();
                state.anchorZ = player.getZ();
                state.anchorYaw = player.getYRot();
                state.anchorPitch = player.getXRot();
            }));

            RemoteEyeBodyControlEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var state = getRemoteEyeState(player);
            helper.assertTrue(state.activeUntilGameTime == expectedActiveUntilGameTime,
                    "RemoteEye legacy state should still use the 30 second persisted repair fallback");
        });
    }

    static void absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "absorption_zero_delay_repair_test");

            try (var ignored = ApprenticeCodexServerConfig.useAbsorptionAmplifyAmuletConfigOverrideForGameTest(8.0D, 0)) {
                equipNecklaceCurio(player, new ItemStack(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()));
                player.setAbsorptionAmount(0.0F);

                Capabilities.withSpellData(player, data -> data.edit(
                        CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE,
                        state -> {
                            state.initialized = true;
                            state.recoveryResumeGameTime = level.getGameTime()
                                    + ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks()
                                    + 1200L;
                            state.nextRecoveryGameTime = level.getGameTime();
                            state.nextProcGameTime = level.getGameTime();
                            state.lastKnownAbsorption = 0.0F;
                        }
                ));

                tickEquippedAbsorptionAmplifyAmulet(player);

                var state = getAbsorptionAmplifyAmuletState(player);
                helper.assertTrue(state.recoveryResumeGameTime == level.getGameTime(),
                        "AbsorptionAmplifyAmulet zero recovery delay should repair resume time to the current tick");
                helper.assertTrue(player.getAbsorptionAmount() == 1.0F,
                        "AbsorptionAmplifyAmulet should recover immediately after zero-delay repair");
            }
        });
    }

    static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mirage_avoidance_window_test");
        var spell = SpellRegistry.MIRAGE_AVOIDANCE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        MirageAvoidanceInput.setPending(player, 0.0F, -1.0F);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);

        var state = getMirageAvoidanceState(player);
        helper.assertTrue(state.activeUntilGameTime == level.getGameTime() + MirageAvoidanceEvents.EFFECT_DURATION_TICKS,
                "MirageAvoidance should keep a 25 tick active window");
        helper.assertTrue(state.invulnerableUntilGameTime == level.getGameTime() + MirageAvoidanceEvents.INVULNERABLE_TICKS,
                "MirageAvoidance should keep only 15 ticks of invulnerability");
        helper.assertTrue(Math.abs(state.movementForward) < 1.0E-4F && state.movementStrafe < -0.99F,
                "MirageAvoidance should store the activation input direction");
        helper.assertFalse(
                spell.checkPreCastConditions(level, 1, player, magicData),
                "MirageAvoidance should reject recast while the effect is active"
        );

        var blockedAttack = postLivingAttackEventForGameTest(player, level.damageSources().lava(), 4.0F);
        helper.assertTrue(blockedAttack.isCanceled(), "MirageAvoidance should cancel all damage during the invulnerability window");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.INVULNERABLE_TICKS;
            s.invulnerableUntilGameTime = level.getGameTime();
            s.activeUntilGameTime = level.getGameTime() + 30;
        }));

        var vulnerableAttack = postLivingAttackEventForGameTest(player, level.damageSources().lava(), 4.0F);
        helper.assertFalse(vulnerableAttack.isCanceled(), "MirageAvoidance should allow normal damage after tick 20");
        var fallAttack = postLivingAttackEventForGameTest(player, level.damageSources().fall(), 4.0F);
        helper.assertFalse(fallAttack.isCanceled(), "MirageAvoidance should allow fall damage after invulnerability ends");
        helper.succeed();
    }

    static void mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 5, 0), "mirage_avoidance_motion_test");
        var spell = SpellRegistry.MIRAGE_AVOIDANCE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        MirageAvoidanceInput.setPending(player, 1.0F, 0.0F);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);
        player.setDeltaMovement(0.3D, -0.5D, 0.2D);
        player.fallDistance = 8.0F;

        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        var startupMovement = player.getDeltaMovement();
        helper.assertTrue(startupMovement.lengthSqr() < 1.0E-6D,
                "MirageAvoidance freeze startup should remove movement");
        helper.assertTrue(player.fallDistance == 8.0F,
                "MirageAvoidance should not reset fall distance before sliding starts");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.FREEZE_TICKS - 2;
            s.activeUntilGameTime = level.getGameTime() + 30;
        }));
        player.setDeltaMovement(0.0D, -0.5D, 0.0D);
        player.fallDistance = 8.0F;
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        var slideMovement = player.getDeltaMovement();
        var horizontalSpeedSqr = slideMovement.x * slideMovement.x + slideMovement.z * slideMovement.z;
        helper.assertTrue(horizontalSpeedSqr > 0.01D,
                "MirageAvoidance should slide after startup");
        helper.assertTrue(slideMovement.y >= -0.081D,
                "MirageAvoidance slide should clamp falling speed");
        helper.assertTrue(player.fallDistance == 0.0F,
                "MirageAvoidance should keep fall distance reset while sliding");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.VULNERABLE_RECOVERY_START_TICK;
            s.activeUntilGameTime = level.getGameTime() + 5;
        }));
        player.fallDistance = 6.0F;
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        helper.assertTrue(player.fallDistance == 6.0F,
                "MirageAvoidance should stop resetting fall distance during recovery");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.EFFECT_DURATION_TICKS;
            s.activeUntilGameTime = level.getGameTime();
        }));
        player.fallDistance = 7.0F;
        MirageAvoidanceEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        helper.assertTrue(player.fallDistance == 7.0F,
                "MirageAvoidance should not reset fall distance after the effect ends");
        helper.succeed();
    }

    static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var matureCropPos = new BlockPos(3, 2, 0);
        var immatureCropPos = new BlockPos(4, 2, 0);
        // age リセット収穫の検証が目的。
        // 小麦系は GameTest/FakePlayer 環境だと明るさ・耕地維持・更新順のノイズを受けやすいため、
        // ここでは Soul Sand だけで生存条件を満たせるネザーウォートで収穫ロジック自体を検証する。
        helper.setBlock(matureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(immatureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(matureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
        helper.setBlock(immatureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 2));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(3, () -> {
            helper.assertBlockProperty(matureCropPos, NetherWartBlock.AGE, 0);
            helper.assertBlockProperty(immatureCropPos, NetherWartBlock.AGE, 2);
            helper.assertItemEntityPresent(Items.NETHER_WART, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.NETHER_WART, matureCropPos, 1.5);
            helper.succeed();
        });
    }
    static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var tomatoPos = new BlockPos(3, 2, 0);

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        helper.setBlock(tomatoPos.below(), Blocks.FARMLAND);
        helper.setBlock(tomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.runAtTickTime(3, () -> {
            var harvestedState = helper.getBlockState(tomatoPos);
            helper.assertTrue(harvestedState.is(tomatoBlock), "Farmer's Delight tomato should remain planted after HarvestMoon");
            helper.assertTrue(getIntegerPropertyValue(helper, harvestedState, "age") == 0,
                    "Farmer's Delight tomato should reset to age 0 after HarvestMoon but got " + harvestedState);
            helper.assertItemEntityPresent(tomatoItem, casterPos, 1.5);
            helper.assertItemEntityNotPresent(tomatoItem, tomatoPos, 1.5);
            helper.succeed();
        });
    }
    static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var baseTomatoPos = new BlockPos(3, 2, 0);
        var ropeTomatoPos = baseTomatoPos.above();

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.setBlock(baseTomatoPos.below(), Blocks.FARMLAND);
        // rope 付きトマトは上段で天井扱いになりやすいため、GameTest でも補助光を置く。
        helper.setBlock(baseTomatoPos.east(), Blocks.GLOWSTONE);
        helper.setBlock(baseTomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 0));
        helper.setBlock(
                ropeTomatoPos,
                withBooleanProperty(
                        helper,
                        withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3),
                        "ropelogged",
                        true
                )
        );

        // HarvestMoon が依存している右クリック経路そのものを直接通し、
        // 成熟した rope 付き上段トマトを収穫しても rope 状態が壊れないことを検証する。
        var result = BlockTools.useBlockByPlayerMainHand(helper.getLevel(), player, helper.absolutePos(ropeTomatoPos), new ItemStack(Items.STICK));
        helper.assertTrue(result.consumesAction(), "Farmer's Delight rope tomato block use should consume the action but got " + result);

        var baseState = helper.getBlockState(baseTomatoPos);
        var ropeState = helper.getBlockState(ropeTomatoPos);
        helper.assertTrue(baseState.is(tomatoBlock), "Lower tomato support should remain planted after right click harvest but got " + baseState);
        helper.assertTrue(getIntegerPropertyValue(helper, baseState, "age") == 0,
                "Lower tomato support should stay at age 0 after right click harvest but got " + baseState);
        helper.assertTrue(ropeState.is(tomatoBlock),
                "Harvested rope tomato should stay planted after right click harvest but got " + ropeState);
        helper.assertTrue(getBooleanPropertyValue(helper, ropeState, "ropelogged"),
                "Right click harvest should preserve Farmer's Delight tomato rope state but got " + ropeState);
        helper.assertTrue(getIntegerPropertyValue(helper, ropeState, "age") == 0,
                "Harvested rope tomato should reset to age 0 after right click harvest but got " + ropeState);
        helper.assertItemEntityPresent(tomatoItem, ropeTomatoPos, 1.5);
        helper.succeed();
    }
    static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var stemPos = new BlockPos(2, 2, 1);
        var fruitPos = new BlockPos(3, 2, 1);
        helper.setBlock(stemPos.below(), Blocks.FARMLAND);
        helper.setBlock(stemPos, Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(fruitPos, Blocks.MELON);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.MELON, fruitPos);
            helper.assertBlockPresent(Blocks.MELON_STEM, stemPos);
            helper.assertItemEntityPresent(Items.MELON_SLICE, casterPos, 1.5);
        });
    }
    static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var bambooBase = new BlockPos(4, 2, 1);
        helper.setBlock(bambooBase.below(), Blocks.DIRT);
        helper.setBlock(bambooBase, Blocks.BAMBOO);
        for (var offset = 1; offset <= 6; ++offset) {
            helper.setBlock(bambooBase.above(offset), Blocks.BAMBOO);
        }

        var cropPositions = new ArrayList<BlockPos>();
        for (var x = 2; x <= 9; ++x) {
            for (var z = -4; z <= 4; ++z) {
                var pos = new BlockPos(x, 3, z);
                if (pos.getX() == bambooBase.getX() && pos.getZ() == bambooBase.getZ()) {
                    continue;
                }
                cropPositions.add(pos);
                helper.setBlock(pos.below(), Blocks.SOUL_SAND);
                helper.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
            }
        }
        helper.assertTrue(cropPositions.size() > 64, "HarvestMoon tick budget test requires more than 64 crops");

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(2, () -> helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) > 0,
                "HarvestMoon should leave some crops for later ticks"));

        helper.succeedWhen(() -> {
            helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) == 0,
                    "HarvestMoon should eventually harvest every queued mature crop");
            helper.assertBlockPresent(Blocks.BAMBOO, bambooBase);
            for (var offset = 1; offset <= 6; ++offset) {
                helper.assertBlockNotPresent(Blocks.BAMBOO, bambooBase.above(offset));
            }
            helper.assertItemEntityPresent(Items.BAMBOO, casterPos, 1.5);
        });
    }
    static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var kelpBase = new BlockPos(3, 2, 0);
        helper.setBlock(kelpBase.below(), Blocks.DIRT);
        helper.setBlock(kelpBase, Blocks.KELP_PLANT);
        for (var offset = 1; offset <= 5; ++offset) {
            helper.setBlock(kelpBase.above(offset), Blocks.KELP_PLANT);
        }
        helper.setBlock(kelpBase.above(6), Blocks.KELP);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            var baseState = helper.getBlockState(kelpBase);
            helper.assertTrue(baseState.is(Blocks.KELP) || baseState.is(Blocks.KELP_PLANT),
                    "HarvestMoon should keep the bottom kelp block");
            for (var offset = 1; offset <= 6; ++offset) {
                var state = helper.getBlockState(kelpBase.above(offset));
                helper.assertTrue(!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT),
                        "HarvestMoon should remove upper kelp blocks even outside the initial Y slice");
            }
            helper.assertItemEntityPresent(Items.KELP, casterPos, 1.5);
        });
    }

    static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).below().getY() + 0.5D;
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should sit on the slab support top: " + turret.getY());
        });
    }

    static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_stairs_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH)
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).getY();
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should accept a normal stair as support: " + turret.getY());
        });
    }

    static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 6, 0), "auto_turret_fall_test");
        var anchorPos = new BlockPos(0, 6, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        helper.getLevel().addFreshEntity(owner);
        castAutoTurret(helper, owner, 1, anchorPos);
        var turret = getSingleLivingAutoTurret(helper, owner);
        var initialY = turret.getY();

        helper.runAtTickTime(1, () -> helper.setBlock(anchorPos.below(), Blocks.AIR));

        helper.succeedWhen(() -> {
            helper.assertTrue(turret.isAlive() && !turret.isRemoved(),
                    "AutoTurret should remain alive while falling after support removal");
            helper.assertTrue(turret.getY() < initialY - 0.05D,
                    "AutoTurret should fall below its anchored position after support removal: " + turret.getY());
        });
    }

    static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret restock should restore ammo to the initial count: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 13.0F) < 1.0E-4F,
                    "AutoTurret restock should spend configured mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_full_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret full-ammo restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret full-ammo restock should not change ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 30.0F) < 1.0E-4F,
                    "AutoTurret full-ammo restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_insufficient_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(16.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret insufficient-mana restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 2,
                    "AutoTurret insufficient-mana restock should not restore ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 16.0F) < 1.0E-4F,
                    "AutoTurret insufficient-mana restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_discard_delay_test");
        var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 3, 10);
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.setMana(10.0F);
        turret.setDamage(100.0F);
        turret.setRestBulletCount(1);
        level.addFreshEntity(owner);
        helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 4));

        helper.runAtTickTime(60, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret should remain alive during the ammo depletion grace period");
            helper.assertTrue(turret.getRestBulletCount() <= 0,
                    "AutoTurret should have spent its final shot before restock: " + turret.getRestBulletCount());
            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock during discard delay should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 3,
                    "AutoTurret restock should restore ammo during discard delay: " + turret.getRestBulletCount());
        });

        helper.runAtTickTime(140, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret restock should clear the ammo depletion discard timer");
            helper.succeed();
        });
    }

    private static AutoTurretEntity createAutoTurretRestockTestEntity(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos localPos,
            int initialBulletCount,
            int restockManaCost
    ) {
        var level = helper.getLevel();
        var absolutePos = helper.absolutePos(localPos);
        var center = Vec3.atBottomCenterOf(absolutePos);
        var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), level);
        turret.setOwner(owner);
        turret.setAnchorPos(absolutePos);
        turret.setDamage(4.0F);
        turret.setRestockData(initialBulletCount, restockManaCost);
        turret.setTurretMaxHealth(20.0F);
        turret.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(turret);
        return turret;
    }

    private static void castAutoTurret(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (AutoTurret) SpellRegistry.AUTO_TURRET.get();
        var castData = new AutoTurret.AutoTurretCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    private static AutoTurretEntity getSingleLivingAutoTurret(GameTestHelper helper, FakePlayer owner) {
        var turrets = new java.util.ArrayList<AutoTurretEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof AutoTurretEntity turret
                    && turret.isAlive()
                    && turret.getOwner() != null
                    && owner.getUUID().equals(turret.getOwner().getUUID())) {
                turrets.add(turret);
            }
        }
        helper.assertTrue(turrets.size() == 1, "Expected exactly one living AutoTurret but found " + turrets.size());
        return turrets.get(0);
    }

    static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_fall_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialY = staff.getY();

        helper.runAtTickTime(5, () -> helper.setBlock(new BlockPos(0, 1, 0), Blocks.AIR));
        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isAlive() && !staff.isRemoved(),
                    "FieldOverseer staff should remain alive after losing support");
            helper.assertTrue(staff.getY() < initialY - 0.05D,
                    "FieldOverseer staff should fall after losing support: " + staff.getY());
        });
    }

    static void fieldOverseerCastDataRoundTripsPlacementAndIdentity(GameTestHelper helper) {
        var source = new FieldOverseer.FieldOverseerCastData();
        var expectedPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var expectedStaffUuid = UUID.randomUUID();
        var expectedDimension = helper.getLevel().dimension().location();
        var sourceTag = new CompoundTag();
        sourceTag.putLong("Position", expectedPosition.asLong());
        sourceTag.putUUID("Staff", expectedStaffUuid);
        sourceTag.putString("Dimension", expectedDimension.toString());
        source.deserializeNBT(sourceTag);

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        source.writeToBuffer(buffer);

        var restored = new FieldOverseer.FieldOverseerCastData();
        restored.readFromBuffer(buffer);
        var restoredTag = restored.serializeNBT();
        helper.assertTrue(restoredTag.getLong("Position") == expectedPosition.asLong(),
                "FieldOverseer cast data should preserve placement through network serialization");
        helper.assertTrue(restoredTag.getUUID("Staff").equals(expectedStaffUuid),
                "FieldOverseer cast data should preserve the managed staff UUID");
        helper.assertTrue(restoredTag.getString("Dimension").equals(expectedDimension.toString()),
                "FieldOverseer cast data should preserve the managed dimension");
        helper.succeed();
    }

    static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_owner_damage_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialHealth = staff.getHealth();

        var damaged = staff.hurt(owner.damageSources().playerAttack(owner), 5.0F);
        helper.assertFalse(damaged, "FieldOverseer staff should reject owner damage");
        helper.assertTrue(Math.abs(staff.getHealth() - initialHealth) < 0.0001F,
                "FieldOverseer staff health changed after owner damage");
        helper.succeed();
    }

    static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_persistence_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);

        helper.assertTrue(staff.shouldBeSaved(),
                "FieldOverseer staff should survive chunk save and reload during its summon duration");
        helper.assertFalse(staff.removeWhenFarAway(Double.MAX_VALUE),
                "FieldOverseer staff should not despawn when the owner moves far away");
        var savedStaff = new CompoundTag();
        staff.saveWithoutId(savedStaff);
        helper.assertTrue(savedStaff.contains("ExpirationGameTime"),
                "FieldOverseer staff should persist its absolute expiration time");
        helper.succeed();
    }

    static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_recast_remove_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            var staff = createDurationBoundFieldOverseer(
                    helper, owner, 1, anchorPos, ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
            var magicData = MagicData.getPlayerMagicData(owner);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer should create recast data after placement");

            ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get())
                    .castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);

            helper.assertTrue(staff.isRemoved(),
                    "FieldOverseer manual recast should remove the placed staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer manual recast should consume the active recast");
        });
    }

    static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_timeout_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 1);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer should have active recast data before timeout");

        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isRemoved(), "FieldOverseer timeout should remove the placed staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer timeout should consume the active recast");
        });
    }

    static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_unloaded_cancel_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 200);
        var savedStaff = new CompoundTag();
        staff.saveWithoutId(savedStaff);
        staff.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);

        FieldOverseerManager.cancel(owner);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should remove recast data while the staff is unloaded");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should apply the normal cooldown");

        var restored = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), helper.getLevel());
        restored.load(savedStaff);
        helper.getLevel().addFreshEntity(restored);
        helper.succeedWhen(() -> helper.assertTrue(restored.isRemoved(),
                "FieldOverseer loaded after cancellation should discard itself before acting"));
    }

    static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_destroy_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 200);
        var magicData = MagicData.getPlayerMagicData(owner);

        staff.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);

        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should remove its matching recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should apply the normal cooldown");
        helper.succeed();
    }

    static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "field_overseer_attack_test");
        var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN.get());
        if (manaRegen != null) {
            manaRegen.setBaseValue(0.0D);
        }
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        ownerMagicData.setMana(75.0F);
        var anchorPos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        // 並列実行中の別構造内の敵を拾わないよう、このテストでは構造内に収まる射程へ絞る.
        staff.configure(anchorPos, 4.0F, 4.0D, 40, 100.0F, 20);

        // 日光や構造外への落下で攻撃対象から外れないよう、床内にハスクを配置する.
        var highHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 2, 2));
        var mediumHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
        var lowHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 2, 2));
        highHealthTarget.setHealth(20.0F);
        mediumHealthTarget.setHealth(10.0F);
        lowHealthTarget.setHealth(5.0F);

        helper.runAtTickTime(52, () -> {
            helper.assertTrue(highHealthTarget.getHealth() < 20.0F,
                    "FieldOverseer should strike the highest-current-health target first");
            helper.assertTrue(Math.abs(mediumHealthTarget.getHealth() - 10.0F) < 0.0001F,
                    "FieldOverseer second strike should wait three ticks");
            helper.assertTrue(Math.abs(lowHealthTarget.getHealth() - 5.0F) < 0.0001F,
                    "FieldOverseer should not strike the lower-health target before its sequence turn");
        });
        helper.runAtTickTime(58, () -> {
            helper.assertTrue(mediumHealthTarget.getHealth() < 10.0F,
                    "FieldOverseer should perform the second strike three ticks after the first");
        });
        helper.runAtTickTime(62, () -> {
            helper.assertTrue(lowHealthTarget.getHealth() < 5.0F,
                    "FieldOverseer should perform the third strike six ticks after the first");
            helper.assertTrue(Math.abs(staff.getCurrentMana() - 100.0F) < 0.0001F,
                    "FieldOverseer should receive two 20 mana transfers after spending one attack cost");
            helper.assertTrue(Math.abs(ownerMagicData.getMana() - 35.0F) < 0.0001F,
                    "FieldOverseer should drain the same amount from the owner: " + ownerMagicData.getMana());
            helper.succeed();
        });
    }

    private static FieldOverseerStaffEntity createFieldOverseerTestEntity(
            GameTestHelper helper, FakePlayer owner, BlockPos anchorPos, float maxMana, int attackManaCost) {
        return createDurationBoundFieldOverseer(
                helper, owner, 1, anchorPos, maxMana, attackManaCost,
                ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
    }

    private static FieldOverseerStaffEntity createDurationBoundFieldOverseer(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos, int duration) {
        return createDurationBoundFieldOverseer(
                helper, owner, spellLevel, helper.absolutePos(anchorPos), 100.0F, 40, duration);
    }

    private static FieldOverseerStaffEntity createDurationBoundFieldOverseer(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos,
            float maxMana, int attackManaCost, int duration) {
        var level = helper.getLevel();
        level.addFreshEntity(owner);
        var center = Vec3.atBottomCenterOf(anchorPos);
        var staff = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), level);
        staff.configure(anchorPos, 4.0F, 24.0D, attackManaCost, maxMana, 20);
        staff.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        var castData = new FieldOverseer.FieldOverseerCastData();
        FieldOverseerManager.initialize(owner, staff, anchorPos, duration, castData);
        level.addFreshEntity(staff);
        var spell = (FieldOverseer) SpellRegistry.FIELD_OVERSEER.get();
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                spell.getSpellId(), spellLevel, spell.getRecastCount(spellLevel, owner),
                duration, CastSource.SPELLBOOK, castData), magicData);
        helper.assertFalse(SummonManager.getSummons(owner).contains(staff.getUUID()),
                "FieldOverseer should not register itself with Iron's SummonManager");
        return staff;
    }

    static void totemOfPermafrostCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_slab_test");
            owner.setYRot(45.0F);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));

            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var totem = getSingleLivingTotemOfPermafrost(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).below().getY() + 0.5D;
            helper.assertTrue(Math.abs(totem.getY() - expectedY) < 0.01D,
                    "TotemOfPermafrost should sit on the slab support top: " + totem.getY());
            helper.assertTrue(Mth.degreesDifferenceAbs(totem.getYRot(), owner.getYRot() + 180.0F) < 0.01F,
                    "TotemOfPermafrost should face back toward the caster: " + totem.getYRot());
        });
    }

    static void totemOfPermafrostFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 6, 0), "totem_permafrost_fall_test");
        var anchorPos = new BlockPos(0, 6, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        helper.getLevel().addFreshEntity(owner);
        castTotemOfPermafrost(helper, owner, 1, anchorPos);
        var totem = getSingleLivingTotemOfPermafrost(helper, owner);
        var initialY = totem.getY();

        helper.runAtTickTime(1, () -> helper.setBlock(anchorPos.below(), Blocks.AIR));

        helper.succeedWhen(() -> {
            helper.assertTrue(totem.isAlive() && !totem.isRemoved(),
                    "TotemOfPermafrost should remain alive while falling after support removal");
            helper.assertTrue(totem.getY() < initialY - 0.05D,
                    "TotemOfPermafrost should fall below its anchored position after support removal: " + totem.getY());
        });
    }

    static void totemOfPermafrostInvalidPlacementCreatesNoRecast(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 10, 0), "totem_permafrost_invalid_test");
        owner.setXRot(-90.0F);
        var magicData = MagicData.getPlayerMagicData(owner);
        var anchorPos = new BlockPos(0, 5, 0);
        helper.setBlock(anchorPos.below(), Blocks.AIR);

        castTotemOfPermafrost(helper, owner, 1, anchorPos);

        helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                "TotemOfPermafrost should not spawn without support");
        helper.assertTrue(!magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.TOTEM_OF_PERMAFROST.get()),
                "TotemOfPermafrost should not create recast data when placement fails");
        helper.succeed();
    }

    static void totemOfPermafrostRecastRemovesPlacedTotem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_recast_remove_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);
            var magicData = MagicData.getPlayerMagicData(owner);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.TOTEM_OF_PERMAFROST.get()),
                    "TotemOfPermafrost should create recast data after placement");

            ((TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get())
                    .onCast(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost recast should remove the placed totem");
        });
    }

    static void totemOfPermafrostMissingTotemRecastIsNoop(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_missing_recast_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);
            var totem = getSingleLivingTotemOfPermafrost(helper, owner);
            totem.discard();

            var magicData = MagicData.getPlayerMagicData(owner);
            ((TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get())
                    .onCast(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost missing recast should not recreate or require the old totem");
        });
    }

    static void totemOfPermafrostTimeoutRemovesPlacedTotem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_timeout_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var magicData = MagicData.getPlayerMagicData(owner);
            var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.TOTEM_OF_PERMAFROST.get().getSpellId());
            helper.assertTrue(recast != null, "TotemOfPermafrost should have active recast data before timeout");

            magicData.getPlayerRecasts().removeRecast(recast, io.redspace.ironsspellbooks.capabilities.magic.RecastResult.TIMEOUT);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost timeout should remove the placed totem");
        });
    }

    static void totemOfPermafrostGreaterConjurersTalismanSkipsTimeoutCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_talisman_timeout_test");
            equipGreaterConjurersTalisman(owner);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var spell = SpellRegistry.TOTEM_OF_PERMAFROST.get();
            var magicData = MagicData.getPlayerMagicData(owner);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "TotemOfPermafrost should have active recast data before talisman timeout");

            magicData.getPlayerRecasts().removeRecast(recast, io.redspace.ironsspellbooks.capabilities.magic.RecastResult.TIMEOUT);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost talisman timeout should still remove the placed totem");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "TotemOfPermafrost talisman timeout should remove the active recast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Greater Conjurer's Talisman should suppress TotemOfPermafrost cooldown when the recast ends by timeout");
        });
    }

    static void totemOfPermafrostGreaterConjurersTalismanSkipsManualRecastCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_talisman_manual_test");
            equipGreaterConjurersTalisman(owner);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var spell = (TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get();
            var magicData = MagicData.getPlayerMagicData(owner);
            spell.castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost manual recast with talisman should remove the placed totem");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "TotemOfPermafrost manual recast with talisman should remove the active recast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Greater Conjurer's Talisman should suppress TotemOfPermafrost cooldown after manual recast");
        });
    }

    static void totemOfPermafrostPulseUsesSummonDamageAttribute(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -3), "totem_permafrost_summon_damage_test");
        level.addFreshEntity(owner);
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);

        var baseDamageTotem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 4.0F, 0);
        var baseTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        baseTarget.setNoAi(true);
        var baseTargetHealth = baseTarget.getHealth();
        pulseTotemOfPermafrostForGameTest(helper, baseDamageTotem, owner);
        var baseDamage = baseTargetHealth - baseTarget.getHealth();
        baseDamageTotem.discard();
        baseTarget.discard();

        owner.getAttribute(AttributeRegistry.SUMMON_DAMAGE.get()).addTransientModifier(new AttributeModifier(
                TOTEM_OF_PERMAFROST_SUMMON_DAMAGE_TEST_MODIFIER_ID,
                "TotemOfPermafrost Summon Damage test",
                0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        var boostedDamageTotem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 4.0F, 0);
        var boostedTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        boostedTarget.setNoAi(true);
        var boostedTargetHealth = boostedTarget.getHealth();
        pulseTotemOfPermafrostForGameTest(helper, boostedDamageTotem, owner);
        var boostedDamage = boostedTargetHealth - boostedTarget.getHealth();

        helper.assertTrue(baseDamage > 0.0F, "TotemOfPermafrost pulse should damage the baseline target");
        helper.assertTrue(boostedDamage > baseDamage + 0.1F,
                "TotemOfPermafrost pulse should scale with Summon Damage: " + boostedDamage + " <= " + baseDamage);
        helper.succeed();
    }

    static void totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -3), "totem_permafrost_pulse_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        level.addFreshEntity(owner);

        var totem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 100.0F, 2);
        var visibleTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        var blockedTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        var outsideTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 0));
        visibleTarget.setNoAi(true);
        blockedTarget.setNoAi(true);
        outsideTarget.setNoAi(true);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 1), Blocks.STONE);

        var visibleHealth = visibleTarget.getHealth();
        var blockedHealth = blockedTarget.getHealth();
        var outsideHealth = outsideTarget.getHealth();

        helper.runAtTickTime(1, () -> {
            pulseTotemOfPermafrostForGameTest(helper, totem, owner);

            helper.assertTrue(!totem.isRemoved(), "TotemOfPermafrost test totem should still exist before pulse assertions");
            helper.assertTrue(visibleTarget.getHealth() < visibleHealth,
                    "TotemOfPermafrost pulse should damage a visible target");
            helper.assertTrue(visibleTarget.getTicksFrozen() >= 40,
                    "TotemOfPermafrost pulse should increase frozen ticks after successful damage");
            var slowness = visibleTarget.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slowness != null && slowness.getAmplifier() == 2,
                    "TotemOfPermafrost pulse should apply configured slowness after successful damage");
            helper.assertTrue(Math.abs(blockedTarget.getHealth() - blockedHealth) < 1.0E-4F,
                    "TotemOfPermafrost pulse should not damage blocked targets");
            helper.assertTrue(blockedTarget.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null,
                    "TotemOfPermafrost pulse should not slow blocked targets");
            helper.assertTrue(Math.abs(outsideTarget.getHealth() - outsideHealth) < 1.0E-4F,
                    "TotemOfPermafrost pulse should not damage targets outside the AABB");
            helper.succeed();
        });
    }

    private static void pulseTotemOfPermafrostForGameTest(
            GameTestHelper helper,
            TotemOfPermafrostTotemEntity totem,
            FakePlayer owner
    ) {
        try {
            var method = TotemOfPermafrostTotemEntity.class.getDeclaredMethod(
                    "pulse",
                    ServerLevel.class,
                    net.minecraft.world.entity.LivingEntity.class
            );
            method.setAccessible(true);
            method.invoke(totem, helper.getLevel(), owner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke TotemOfPermafrost pulse for GameTest", exception);
        }
    }

    private static TotemOfPermafrostTotemEntity createTotemOfPermafrostTestEntity(
            ServerLevel level,
            FakePlayer owner,
            BlockPos absoluteAnchorPos,
            float damage,
            int slownessAmplifier
    ) {
        var center = Vec3.atBottomCenterOf(absoluteAnchorPos);
        var totem = new TotemOfPermafrostTotemEntity(EntityRegistry.TOTEM_OF_PERMAFROST_TOTEM.get(), level);
        totem.setOwner(owner);
        totem.setAnchorPos(absoluteAnchorPos);
        totem.setDamage(damage);
        totem.setSlownessAmplifier(slownessAmplifier);
        totem.setRadius(3.0D);
        totem.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(totem);
        return totem;
    }

    private static void castTotemOfPermafrost(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get();
        var castData = new TotemOfPermafrost.TotemOfPermafrostCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    private static java.util.List<TotemOfPermafrostTotemEntity> getOwnedTotemOfPermafrost(GameTestHelper helper, FakePlayer owner) {
        var totems = new java.util.ArrayList<TotemOfPermafrostTotemEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof TotemOfPermafrostTotemEntity totem
                    && totem.isAlive()
                    && totem.getOwner() != null
                    && owner.getUUID().equals(totem.getOwner().getUUID())) {
                totems.add(totem);
            }
        }
        return totems;
    }

    private static TotemOfPermafrostTotemEntity getSingleLivingTotemOfPermafrost(GameTestHelper helper, FakePlayer owner) {
        var totems = getOwnedTotemOfPermafrost(helper, owner);
        helper.assertTrue(totems.size() == 1, "Expected exactly one living TotemOfPermafrost but found " + totems.size());
        return totems.get(0);
    }

    static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_owner_test");
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0);
        var item = new ItemEntity(level, owner.getX() + 2.0, owner.getY(), owner.getZ(), new ItemStack(Items.IRON_INGOT));
        level.addFreshEntity(owner);
        level.addFreshEntity(item);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            helper.assertFalse(item.isRemoved(), "AutoMagnet test item should remain as an entity before player pickup");
            helper.assertTrue(item.position().distanceToSqr(owner.position()) <= 0.001,
                    "AutoMagnet should collect items when no Solegnolia blocks it");
            helper.succeed();
        });
    }

    static void autoMagnetNormalModeCollectsWhileStanding(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.NORMAL, false, true,
                "auto_magnet_normal_standing_test");
    }

    static void autoMagnetNormalModeStopsWhileCrouching(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.NORMAL, true, false,
                "auto_magnet_normal_crouching_test");
    }

    static void autoMagnetReverseModeStopsWhileStanding(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.REVERSE, false, false,
                "auto_magnet_reverse_standing_test");
    }

    static void autoMagnetReverseModeCollectsWhileCrouching(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.REVERSE, true, true,
                "auto_magnet_reverse_crouching_test");
    }

    static void autoMagnetRecastSwitchesModeAndStopsSameMode(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_recast_mode_test");
            level.addFreshEntity(owner);

            AutoMagnetFamiliarManager.activate(owner, 4.0D, 0.0D, AutoMagnetCollectionMode.NORMAL);
            var initialFamiliars = getOwnedAutoMagnetFamiliars(helper, owner);
            helper.assertTrue(initialFamiliars.size() == 1,
                    "AutoMagnet should spawn exactly one familiar before mode switch");
            var familiar = initialFamiliars.get(0);

            var switched = AutoMagnetFamiliarManager.toggle(owner, 6.0D, 0.0D, AutoMagnetCollectionMode.REVERSE);
            helper.assertTrue(switched, "AutoMagnet should switch mode instead of deactivating");
            var switchedFamiliars = getOwnedAutoMagnetFamiliars(helper, owner);
            helper.assertTrue(switchedFamiliars.size() == 1,
                    "AutoMagnet mode switch should keep exactly one familiar");
            helper.assertTrue(switchedFamiliars.get(0) == familiar,
                    "AutoMagnet mode switch should reuse the existing familiar");
            helper.assertTrue(familiar.getCollectionMode() == AutoMagnetCollectionMode.REVERSE,
                    "AutoMagnet familiar should use reverse mode after switch");
            helper.assertTrue(Math.abs(familiar.getPickupRange() - 6.0D) < 1.0E-4D,
                    "AutoMagnet mode switch should update the familiar range");

            var deactivated = AutoMagnetFamiliarManager.toggle(owner, 6.0D, 0.0D, AutoMagnetCollectionMode.REVERSE);
            helper.assertFalse(deactivated, "AutoMagnet same-mode recast should deactivate");
            helper.assertTrue(getOwnedAutoMagnetFamiliars(helper, owner).isEmpty(),
                    "AutoMagnet same-mode recast should discard the familiar");
            helper.succeed();
        });
    }

    private static void assertAutoMagnetCollectionMode(GameTestHelper helper, AutoMagnetCollectionMode mode,
                                                       boolean crouching, boolean shouldCollect, String profileName) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        setCrouchingForAutoMagnetTest(owner, crouching);
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0, mode);
        var item = new ItemEntity(level, owner.getX() + 2.0, owner.getY(), owner.getZ(), new ItemStack(Items.IRON_INGOT));
        var orb = new ExperienceOrb(level, owner.getX() + 2.0, owner.getY(), owner.getZ() + 1.0, 3);
        level.addFreshEntity(owner);
        level.addFreshEntity(item);
        level.addFreshEntity(orb);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            var itemCollected = item.position().distanceToSqr(owner.position()) <= 0.001D;
            var orbCollected = orb.position().distanceToSqr(owner.position()) <= 0.001D;
            helper.assertTrue(itemCollected == shouldCollect,
                    "AutoMagnet item collection mode mismatch. mode=" + mode + ", crouching=" + crouching);
            helper.assertTrue(orbCollected == shouldCollect,
                    "AutoMagnet orb collection mode mismatch. mode=" + mode + ", crouching=" + crouching);
            helper.assertTrue(familiar.isCollectionBlocked() != shouldCollect,
                    "AutoMagnet collection blocked flag should match mode stop state");
            helper.succeed();
        });
    }

    private static void setCrouchingForAutoMagnetTest(FakePlayer player, boolean crouching) {
        player.setShiftKeyDown(crouching);
        player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
    }

    static void mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_front_test");
            player.setYRot(0.0f);
            player.setXRot(0.0f);
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var source = CombatTools.getDamageSource(level, attacker, DamageTypes.SHOCK);
            var spell = beginMysticShieldCast(level, player, 1);

            var firstAttack = postLivingAttackEventForGameTest(player, source, 8.0f);
            var duplicateAttack = postLivingAttackEventForGameTest(player, source, 8.0f);
            helper.assertTrue(firstAttack.isCanceled(), "Mystic Shield should block a front attack");
            helper.assertTrue(duplicateAttack.isCanceled(), "Mystic Shield should still block a repeated front attack");

            spell.onCast(level, 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
            completeMysticShieldCast(level, player, 1, false);
            var projectiles = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, player.getBoundingBox().inflate(4.0));
            helper.assertTrue(projectiles.size() == 1,
                    "Mystic Shield should release exactly one stored projectile but got " + projectiles.size());
            var expectedDamage = 8.0f * spell.getReflectDamageMultiplier(1, player);
            helper.assertTrue(Math.abs(projectiles.get(0).getDamageForGameTest() - expectedDamage) < 1.0e-4f,
                    "Mystic Shield should ignore duplicate same-source accumulation: expected="
                            + expectedDamage + ", actual=" + projectiles.get(0).getDamageForGameTest());
            helper.succeed();
        });
    }

    static void mysticShieldReflectsStoredDamageAfterNonFrontCancel(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_side_test");
            player.setYRot(0.0f);
            player.setXRot(0.0f);
            var frontAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var sideAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            beginMysticShieldCast(level, player, 1);

            var frontAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, frontAttacker, DamageTypes.SHOCK),
                    6.0f
            );
            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield should store front damage before a later cancel");

            var sideAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, sideAttacker, DamageTypes.SHOCK),
                    4.0f
            );
            helper.assertFalse(sideAttack.isCanceled(), "Mystic Shield should not block a side attack");

            completeMysticShieldCast(level, player, 1, true);
            var projectiles = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, player.getBoundingBox().inflate(4.0));
            helper.assertTrue(projectiles.size() == 1,
                    "Mystic Shield should release stored reflection even after a non-front cancel but got " + projectiles.size());
            helper.succeed();
        });
    }

    static void mysticShieldUsesYawWhenLookPitchIsVertical(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_vertical_pitch_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(-90.0f);
            var frontAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var sideAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            beginMysticShieldCast(level, player, 1);

            var frontAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, frontAttacker, DamageTypes.SHOCK),
                    6.0f
            );
            var sideAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, sideAttacker, DamageTypes.SHOCK),
                    6.0f
            );

            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield should use yaw to block front attacks at vertical pitch");
            helper.assertFalse(sideAttack.isCanceled(), "Mystic Shield should still reject side attacks at vertical pitch");
            helper.succeed();
        });
    }

    static void mysticShieldReceivesProtectionSpellSupporterBenefits(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_supporter_test");
            var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
            var baseMultiplier = spell.getReflectDamageMultiplier(1, player);
            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()));
            helper.assertTrue(Math.abs(spell.getReflectDamageMultiplier(1, player) - baseMultiplier * 2.0f) < 1.0e-6f,
                    "Protection Spell Supporter should double Mystic Shield reflection damage");

            var manaEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporterManaCostDiscountEvent.onSpellCast(manaEvent);
            var expectedManaCost = Math.max(1, Math.round(spell.getManaCost(1) * 0.5f));
            helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                    "Protection Spell Supporter should halve Mystic Shield mana cost to "
                            + expectedManaCost + " but got " + manaEvent.getManaCost());
        });
    }

    static MysticShield beginMysticShieldCast(ServerLevel level, FakePlayer player, int spellLevel) {
        var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(1000.0f);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(new ItemStack(Items.STICK));
        spell.onCast(level, spellLevel, player, CastSource.SPELLBOOK, magicData);
        return spell;
    }

    static void completeMysticShieldCast(ServerLevel level, FakePlayer player, int spellLevel, boolean cancelled) {
        var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
        var magicData = MagicData.getPlayerMagicData(player);
        spell.onServerCastComplete(level, spellLevel, player, magicData, cancelled);
    }

    static DualAcrobat beginDualAcrobatCast(ServerLevel level, FakePlayer player, int spellLevel) {
        var spell = (DualAcrobat) SpellRegistry.DUAL_ACROBAT.get();
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(1000.0f);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(new ItemStack(Items.STICK));
        spell.onCast(level, spellLevel, player, CastSource.SPELLBOOK, magicData);
        return spell;
    }

    static @Nullable DualAcrobatSmgEntity findDualAcrobatSmg(ServerLevel level, Player player) {
        var weapons = level.getEntitiesOfClass(
                DualAcrobatSmgEntity.class,
                new AABB(player.position(), player.position()).inflate(16.0D),
                weapon -> {
                    var owner = weapon.getOwner();
                    return !weapon.isRemoved() && owner != null && owner.getUUID().equals(player.getUUID());
                }
        );
        return weapons.isEmpty() ? null : weapons.get(0);
    }

    static void earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(GameTestHelper helper) {
        var centerPos = new BlockPos(2, 3, 2);
        var sourceWaterPos = centerPos;
        var flowingWaterPos = centerPos.east();
        var seagrassPos = centerPos.west();
        var waterloggedStairPos = centerPos.north();
        var lavaPos = centerPos.offset(2, 0, 2);
        var changedToLavaPos = centerPos.offset(2, 0, -2);

        for (var x = -1; x <= 5; x++) {
            for (var z = -1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, centerPos.getY() - 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, centerPos.getY(), z), Blocks.STONE);
            }
        }
        helper.setBlock(seagrassPos.below(), Blocks.DIRT);
        helper.setBlock(sourceWaterPos, Blocks.WATER);
        helper.setBlock(flowingWaterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        helper.setBlock(seagrassPos, Blocks.SEAGRASS);
        helper.setBlock(changedToLavaPos, Blocks.WATER);
        helper.setBlock(
                waterloggedStairPos,
                Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.WATERLOGGED, true)
        );
        helper.setBlock(lavaPos, Blocks.LAVA);

        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 4, 1), "earth_forge_water_replace_test");
        helper.runAtTickTime(1, () -> castEarthForge(helper, player, centerPos, Direction.UP, 3));
        helper.runAtTickTime(2, () -> helper.setBlock(changedToLavaPos, Blocks.LAVA));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.DIRT, sourceWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, flowingWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, seagrassPos);
            helper.assertBlockPresent(Blocks.OAK_STAIRS, waterloggedStairPos);
            helper.assertBlockProperty(waterloggedStairPos, StairBlock.WATERLOGGED, true);
            helper.assertBlockPresent(Blocks.LAVA, lavaPos);
            helper.assertBlockPresent(Blocks.LAVA, changedToLavaPos);
        });
    }

    static void blockToolsTemporaryUseKeepsOneCountStackAndHands(GameTestHelper helper) {
        var placePos = new BlockPos(2, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 4, 1), "block_tools_temporary_use_test");
        var originalMainHand = new ItemStack(Items.STICK);
        var inventoryDirt = new ItemStack(Items.DIRT, 32);
        var virtualDirt = new ItemStack(Items.DIRT);

        helper.setBlock(placePos.below(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand.copy());
        player.getInventory().setItem(9, inventoryDirt.copy());

        helper.runAtTickTime(1, () -> {
            var effectiveVirtualDirt = BlockTools.copyForTemporaryUse(virtualDirt);
            helper.assertTrue(
                    effectiveVirtualDirt.is(Items.DIRT) && effectiveVirtualDirt.getCount() == 2,
                    "Temporary stack normalization should leave one item after a single placement"
            );
            helper.assertTrue(
                    virtualDirt.is(Items.DIRT) && virtualDirt.getCount() == 1,
                    "Temporary stack normalization should not mutate its source"
            );

            var result = BlockTools.useItemOnBlockByPlayerMainHand(
                    helper.getLevel(),
                    player,
                    helper.absolutePos(placePos),
                    virtualDirt,
                    Direction.UP
            );

            helper.assertTrue(result.consumesAction(), "Temporary dirt use should consume action");
            helper.assertBlockPresent(Blocks.DIRT, placePos);
            helper.assertTrue(
                    ItemStack.isSameItemSameTags(player.getMainHandItem(), originalMainHand)
                            && player.getMainHandItem().getCount() == originalMainHand.getCount(),
                    "Temporary use should restore original main hand"
            );
            helper.assertTrue(
                    virtualDirt.is(Items.DIRT) && virtualDirt.getCount() == 1,
                    "Temporary one-count stack should not be consumed"
            );
            var storedDirt = player.getInventory().getItem(9);
            helper.assertTrue(
                    storedDirt.is(Items.DIRT) && storedDirt.getCount() == inventoryDirt.getCount(),
                    "Inventory dirt should stay in its original slot"
            );
            helper.succeed();
        });
    }

    static void linearBuildPlacesUntilPlayerAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_axis_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(1, 3, 2));
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should consume exactly the blocks it placed from main hand");
        });
    }

    static void linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(4, 2, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_initial_axis_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.UP);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, placePos);
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should try and consume one block even when the first placement touches the player Y axis");
        });
    }

    static void linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(4, 3, 2);
            var firstPlacePos = new BlockPos(4, 4, 2);
            var secondPlacePos = new BlockPos(4, 5, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_upward_same_y_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.UP);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, firstPlacePos);
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, secondPlacePos);
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                    "Linear Build should place only one block when extending upward from a block on the player's Y axis");
        });
    }

    static void linearBuildPrefersOffhandBlockTemplate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_offhand_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 3));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.DIRT) && player.getMainHandItem().getCount() == 4,
                    "Linear Build should not use the main hand block when the offhand holds a block");
            helper.assertTrue(player.getOffhandItem().is(Items.OAK_PLANKS) && player.getOffhandItem().getCount() == 1,
                    "Linear Build should consume the selected offhand block template");
        });
    }

    static void wizardlampUsesClientCellAndClampsItWithoutLineOfSight(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "wizardlamp_target_test");
            var spell = (Wizardlamp) SpellRegistry.WIZARDLAMP.get();
            var eyePosition = player.getEyePosition(1.0F);

            var inRangePos = BlockPos.containing(eyePosition.add(2.0, 0.0, 0.0));
            var inRangeTarget = new BlockTargetData();
            inRangeTarget.setTarget(inRangePos, Direction.UP, Vec3.atCenterOf(inRangePos), inRangePos, Direction.DOWN);
            helper.assertTrue(
                    Wizardlamp.resolveClientPlacePos(level, player, inRangeTarget, 6.0).orElseThrow().equals(inRangePos),
                    "Wizardlamp should keep an in-range client placement cell"
            );

            var farPos = BlockPos.containing(eyePosition.add(12.0, 0.0, 0.0));
            var farTarget = new BlockTargetData();
            farTarget.setTarget(farPos, Direction.UP, Vec3.atCenterOf(farPos), farPos, Direction.DOWN);
            var offset = Vec3.atCenterOf(farPos).subtract(eyePosition);
            var expectedPos = BlockPos.containing(eyePosition.add(offset.normalize().scale(6.0)));
            var wallPos = BlockPos.containing(eyePosition.add(offset.normalize().scale(2.0)));
            level.setBlockAndUpdate(wallPos, Blocks.STONE.defaultBlockState());

            BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), farTarget);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Wizardlamp test could not resolve player magic data");
            helper.assertTrue(
                    spell.checkPreCastConditions(level, 1, player, magicData),
                    "Wizardlamp should accept a replaceable clamped cell without requiring line of sight"
            );
            spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(
                    level.getBlockState(expectedPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Wizardlamp should place its lantern at the maximum-range cell along the client vector"
            );
            helper.assertTrue(
                    level.getBlockState(expectedPos.below()).isAir(),
                    "Wizardlamp lantern should remain placed without a supporting block"
            );
            helper.assertTrue(
                    !level.getBlockState(farPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Wizardlamp should not trust an out-of-range placement cell directly"
            );
        });
    }

    static void blockPlacementSpellsRespectForgePlaceEvent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var wizardlampPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 1), "wizardlamp_place_event_test");
            var mageLightPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 3), "mage_light_place_event_test");
            var linearBuildPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(2, 3, 2), "linear_build_place_event_test");
            var wizardlampPos = helper.absolutePos(new BlockPos(2, 3, 1));
            var mageLightPos = helper.absolutePos(new BlockPos(2, 3, 3));
            var linearBuildPos = new BlockPos(3, 3, 2);
            var wizardlampEvents = new AtomicInteger();
            var mageLightEvents = new AtomicInteger();
            var linearBuildEvents = new AtomicInteger();

            java.util.function.Consumer<BlockEvent.EntityPlaceEvent> cancelListener = event -> {
                if (event.getEntity() == wizardlampPlayer
                        && event.getPlacedBlock().is(BlockRegistry.WIZARDLAMP_LANTERN.get())) {
                    wizardlampEvents.incrementAndGet();
                    event.setCanceled(true);
                } else if (event.getEntity() == mageLightPlayer
                        && event.getPlacedBlock().is(BlockRegistry.MAGE_LIGHT_TORCH.get())) {
                    mageLightEvents.incrementAndGet();
                    event.setCanceled(true);
                } else if (event.getEntity() == linearBuildPlayer
                        && event.getPlacedBlock().is(Blocks.OAK_PLANKS)) {
                    linearBuildEvents.incrementAndGet();
                    event.setCanceled(true);
                }
            };

            MinecraftForge.EVENT_BUS.addListener(cancelListener);
            try {
                var wizardlamp = (Wizardlamp) SpellRegistry.WIZARDLAMP.get();
                var wizardlampTarget = new BlockTargetData();
                wizardlampTarget.setTarget(
                        wizardlampPos,
                        Direction.UP,
                        Vec3.atCenterOf(wizardlampPos),
                        wizardlampPos,
                        Direction.DOWN
                );
                BlockTargetingHelper.setPendingServerTarget(
                        wizardlampPlayer, wizardlamp.getSpellResource(), wizardlampTarget);
                var wizardlampMagicData = MagicData.getPlayerMagicData(wizardlampPlayer);
                helper.assertTrue(wizardlamp.checkPreCastConditions(
                                level, 1, wizardlampPlayer, wizardlampMagicData),
                        "Wizardlamp pre-cast check should accept the prepared target");
                wizardlamp.onCast(level, 1, wizardlampPlayer, CastSource.SPELLBOOK, wizardlampMagicData);

                var mageLight = (MageLight) SpellRegistry.MAGE_LIGHT.get();
                var mageLightTarget = new BlockTargetData();
                mageLightTarget.setTarget(
                        mageLightPos,
                        Direction.UP,
                        Vec3.atCenterOf(mageLightPos),
                        mageLightPos,
                        Direction.DOWN
                );
                BlockTargetingHelper.setPendingServerTarget(
                        mageLightPlayer, mageLight.getSpellResource(), mageLightTarget);
                var mageLightMagicData = MagicData.getPlayerMagicData(mageLightPlayer);
                helper.assertTrue(mageLight.checkPreCastConditions(
                                level, 1, mageLightPlayer, mageLightMagicData),
                        "Mage Light pre-cast check should accept the prepared target");
                mageLight.onCast(level, 1, mageLightPlayer, CastSource.SPELLBOOK, mageLightMagicData);

                linearBuildPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 2));
                helper.setBlock(new BlockPos(4, 3, 2), Blocks.STONE);
                castLinearBuild(helper, linearBuildPlayer, new BlockPos(4, 3, 2), Direction.WEST);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(cancelListener);
            }

            helper.assertTrue(wizardlampEvents.get() == 1,
                    "Wizardlamp should fire one Forge EntityPlaceEvent");
            helper.assertTrue(mageLightEvents.get() == 1,
                    "Mage Light should fire one Forge EntityPlaceEvent");
            helper.assertTrue(linearBuildEvents.get() == 1,
                    "Linear Build should keep using the Forge block placement event path");
            helper.assertBlockNotPresent(BlockRegistry.WIZARDLAMP_LANTERN.get(), new BlockPos(2, 3, 1));
            helper.assertBlockNotPresent(BlockRegistry.MAGE_LIGHT_TORCH.get(), new BlockPos(2, 3, 3));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, linearBuildPos);
            helper.assertTrue(linearBuildPlayer.getMainHandItem().getCount() == 2,
                    "Canceled Linear Build placement should not consume its source block");
        });
    }

    static void wizardlampLanternHasLanternCollisionAndNoDrops(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var absolutePos = helper.absolutePos(new BlockPos(2, 3, 2));
            var state = BlockRegistry.WIZARDLAMP_LANTERN.get().defaultBlockState();
            level.setBlockAndUpdate(absolutePos, state);

            helper.assertTrue(state.canSurvive(level, absolutePos),
                    "Wizardlamp lantern should survive without support");
            helper.assertTrue(!state.getCollisionShape(level, absolutePos).isEmpty(),
                    "Wizardlamp lantern should have a collision shape");
            var outlineShape = state.getShape(level, absolutePos);
            var collisionShape = state.getCollisionShape(level, absolutePos);
            helper.assertTrue(
                    Math.abs(outlineShape.min(Direction.Axis.Y) - WizardlampLanternBlock.FLOATING_OFFSET) < 0.0001D,
                    "Wizardlamp lantern outline should be raised with its rendered model"
            );
            helper.assertTrue(
                    Math.abs(collisionShape.min(Direction.Axis.Y) - WizardlampLanternBlock.FLOATING_OFFSET) < 0.0001D,
                    "Wizardlamp lantern collision should be raised with its rendered model"
            );
            helper.assertTrue(state.getLightEmission() == 15,
                    "Wizardlamp lantern should emit maximum block light");

            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "wizardlamp_break_test");
            var bareHandProgress = state.getDestroyProgress(player, level, absolutePos);
            helper.assertTrue(Math.abs(state.getDestroySpeed(level, absolutePos) - 0.3F) < 0.0001F,
                    "Wizardlamp lantern hardness should produce a 30-tick bare-hand break time");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            var pickaxeProgress = state.getDestroyProgress(player, level, absolutePos);
            helper.assertTrue(pickaxeProgress > bareHandProgress,
                    "Wizardlamp lantern should break faster with a pickaxe");

            var drops = Block.getDrops(
                    state,
                    level,
                    absolutePos,
                    level.getBlockEntity(absolutePos),
                    player,
                    player.getMainHandItem()
            );
            helper.assertTrue(drops.isEmpty(), "Wizardlamp lantern should not drop items");
        });
    }

    static void linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_luminous_device_source_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 2));
            player.setItemInHand(InteractionHand.OFF_HAND, deviceStack);
            player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.GLOWSTONE, 2)));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 2));
            helper.assertTrue(LuminousDevice.getStoredCount(deviceStack, new ItemStack(Items.GLOWSTONE)) == 0,
                    "Linear Build should consume matching Luminous Device contents before Shulker contents");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.GLOWSTONE),
                    "Linear Build consumption should preserve the zero-count selected template");
            var shulkerStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
            helper.assertTrue(shulkerStack.is(Items.GLOWSTONE) && shulkerStack.getCount() == 2,
                    "Linear Build should leave lower-priority Shulker contents untouched");
            helper.assertTrue(player.getMainHandItem().is(Items.DIRT) && player.getMainHandItem().getCount() == 4,
                    "An offhand Luminous Device selection should take priority over the main-hand block");
        });
    }

    static void linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var zeroTargetPos = new BlockPos(5, 3, 2);
            var zeroSelectionPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_zero_luminous_selection_test");
            zeroSelectionPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var zeroSelectionDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(zeroSelectionDevice, new ItemStack(Items.GLOWSTONE));
            LuminousDevice.consumeOneStored(zeroSelectionDevice, new ItemStack(Items.GLOWSTONE));
            zeroSelectionPlayer.setItemInHand(InteractionHand.OFF_HAND, zeroSelectionDevice);
            zeroSelectionPlayer.getInventory().setItem(10, new ItemStack(Items.GLOWSTONE, 2));
            helper.setBlock(zeroTargetPos, Blocks.STONE);

            castLinearBuild(helper, zeroSelectionPlayer, zeroTargetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 2));
            helper.assertTrue(zeroSelectionPlayer.getMainHandItem().is(Items.DIRT),
                    "A zero-count selected item should still be resolved instead of falling back to main hand");
            helper.assertTrue(zeroSelectionPlayer.getInventory().getItem(10).isEmpty(),
                    "A zero-count selection should use matching blocks from another source");

            var fallbackTargetPos = new BlockPos(5, 3, 5);
            var fallbackPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 5),
                    "linear_build_empty_luminous_fallback_test");
            fallbackPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE, 2));
            fallbackPlayer.setItemInHand(
                    InteractionHand.OFF_HAND,
                    new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get())
            );
            helper.setBlock(fallbackTargetPos, Blocks.STONE);

            castLinearBuild(helper, fallbackPlayer, fallbackTargetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 5));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 5));
            helper.assertTrue(fallbackPlayer.getMainHandItem().isEmpty(),
                    "An empty offhand Luminous Device should fall back to the main-hand block");
        });
    }

    static void linearBuildConsumesReplaceablePlacedBlockTemplate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(3, 3, 2), "linear_build_replaceable_block_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FERN));
            helper.setBlock(targetPos, Blocks.STONE);
            helper.setBlock(placePos.below(), Blocks.GRASS_BLOCK);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.FERN, placePos);
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should consume replaceable block templates that stay replaceable after placement");
        });
    }

    static void linearBuildRejectsLargeAndDenylistedTemplates(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(Items.OAK_DOOR),
                    "linear_build_oak_door_reject_test",
                    "Linear Build should reject door templates"
            );
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(Items.WHITE_BED),
                    "linear_build_white_bed_reject_test",
                    "Linear Build should reject bed templates"
            );

            var inscriptionTable = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table")
            );
            helper.assertTrue(inscriptionTable != null, "irons_spellbooks:inscription_table is not registered");
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(inscriptionTable.asItem()),
                    "linear_build_inscription_table_reject_test",
                    "Linear Build should reject denylisted Inscription Table templates"
            );
        });
    }

    static void linearBuildRejectsOffhandTemplateWithoutMainHandFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_offhand_reject_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_DOOR));
            helper.setBlock(targetPos, Blocks.STONE);

            assertLinearBuildPreCastRejected(helper, player, targetPos, Direction.WEST,
                    "Linear Build should reject the offhand template before checking main hand fallback");

            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, placePos);
            helper.assertBlockNotPresent(Blocks.OAK_DOOR, placePos);
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should not consume the main hand fallback block");
            helper.assertTrue(player.getOffhandItem().is(Items.OAK_DOOR) && player.getOffhandItem().getCount() == 1,
                    "Linear Build should not consume the rejected offhand template");
        });
    }

    static void linearBuildSkipsBlockedPositionsByDefault(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(6, 3, 2);
            var blockedPos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_skip_blocked_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
            helper.setBlock(targetPos, Blocks.STONE);
            helper.setBlock(blockedPos, Blocks.COBBLESTONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 3, 2));
            helper.assertBlockPresent(Blocks.COBBLESTONE, blockedPos);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(1, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should skip blocked positions without consuming a block for them");
        });
    }

    static void linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(true, true, true)
            )) {
                var targetPos = new BlockPos(6, 3, 2);
                var blockedPos = new BlockPos(4, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_abort_blocked_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
                helper.setBlock(targetPos, Blocks.STONE);
                helper.setBlock(blockedPos, Blocks.COBBLESTONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 3, 2));
                helper.assertBlockPresent(Blocks.COBBLESTONE, blockedPos);
                helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should keep the legacy stop-on-failure behavior when configured");
            }
        });
    }

    static void linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_trunk_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_SLAB));
            var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
            helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
            trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_SLAB, 2));
            helper.setBlock(targetPos, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_SLAB, new BlockPos(4, 3, 2));
            helper.assertBlockProperty(new BlockPos(4, 3, 2), SlabBlock.TYPE, SlabType.TOP);
            helper.assertBlockPresent(Blocks.OAK_SLAB, new BlockPos(3, 3, 2));
            helper.assertBlockProperty(new BlockPos(3, 3, 2), SlabBlock.TYPE, SlabType.TOP);
            helper.assertTrue(trunkInventory.getHandler().getStackInSlot(0).isEmpty(),
                    "Linear Build should consume matching blocks from Companion Trunk before the held stack");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_SLAB) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave the held stack untouched while Companion Trunk has enough blocks");
        });
    }

    static void linearBuildCopiesBottomSlabType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_SLAB),
                    Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    "linear_build_bottom_slab_test"
            );

            helper.assertBlockPresent(Blocks.OAK_SLAB, placePos);
            helper.assertBlockProperty(placePos, SlabBlock.TYPE, SlabType.BOTTOM);
        });
    }

    static void linearBuildDoesNotCopyDoubleSlabType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_SLAB),
                    Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                    "linear_build_double_slab_test"
            );

            helper.assertBlockPresent(Blocks.OAK_SLAB, placePos);
            helper.assertTrue(helper.getBlockState(placePos).getValue(SlabBlock.TYPE) != SlabType.DOUBLE,
                    "Linear Build should not create a double slab from one slab item");
        });
    }

    static void linearBuildCopiesFurnaceFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.FURNACE),
                    Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST),
                    "linear_build_furnace_facing_test"
            );

            helper.assertBlockPresent(Blocks.FURNACE, placePos);
            helper.assertBlockProperty(placePos, AbstractFurnaceBlock.FACING, Direction.EAST);
        });
    }

    static void linearBuildCopiesLogAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_LOG),
                    Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
                    "linear_build_log_axis_test"
            );

            helper.assertBlockPresent(Blocks.OAK_LOG, placePos);
            helper.assertBlockProperty(placePos, RotatedPillarBlock.AXIS, Direction.Axis.X);
        });
    }

    static void linearBuildCopiesPistonFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.PISTON),
                    Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP),
                    "linear_build_piston_facing_test"
            );

            helper.assertBlockPresent(Blocks.PISTON, placePos);
            helper.assertBlockProperty(placePos, DirectionalBlock.FACING, Direction.UP);
        });
    }

    static void linearBuildCopiesSpellDispenserFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(ItemRegistry.SPELL_DISPENSER.get()),
                    BlockRegistry.SPELL_DISPENSER.get().defaultBlockState().setValue(SpellDispenser.FACING, Direction.EAST),
                    "linear_build_spell_dispenser_facing_test"
            );

            helper.assertBlockPresent(BlockRegistry.SPELL_DISPENSER.get(), placePos);
            helper.assertBlockProperty(placePos, SpellDispenser.FACING, Direction.EAST);
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(placePos)) != null,
                    "Linear Build should keep Spell Dispenser block entity initialization");
        });
    }

    static void linearBuildKeepsShulkerBlockEntityTagContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    createShulkerWithItem(new ItemStack(Items.DIAMOND, 3)),
                    Blocks.SHULKER_BOX.defaultBlockState(),
                    "linear_build_shulker_contents_test"
            );

            helper.assertBlockPresent(Blocks.SHULKER_BOX, placePos);
            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(placePos));
            helper.assertTrue(blockEntity instanceof ShulkerBoxBlockEntity,
                    "Linear Build should place a Shulker Box block entity");
            var storedStack = ((ShulkerBoxBlockEntity) blockEntity).getItem(0);
            helper.assertTrue(storedStack.is(Items.DIAMOND) && storedStack.getCount() == 3,
                    "Linear Build should preserve Shulker Box BlockEntityTag contents");
        });
    }

    static void linearBuildCopiesStairFacingAndHalfOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_STAIRS),
                    Blocks.OAK_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.SOUTH)
                            .setValue(StairBlock.HALF, Half.TOP)
                            .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT),
                    "linear_build_stair_shape_test"
            );

            helper.assertBlockPresent(Blocks.OAK_STAIRS, placePos);
            helper.assertBlockProperty(placePos, StairBlock.FACING, Direction.SOUTH);
            helper.assertBlockProperty(placePos, StairBlock.HALF, Half.TOP);
            helper.assertTrue(helper.getBlockState(placePos).getValue(StairBlock.SHAPE) != StairsShape.INNER_LEFT,
                    "Linear Build should not copy stair connection shape");
        });
    }

    static void linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_creative_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 1));
            var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
            helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
            trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should not consume the held block in creative mode");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.is(Items.OAK_PLANKS) && trunkStack.getCount() == 2,
                    "Linear Build should not retrieve blocks from storage in creative mode");
        });
    }

    static void linearBuildConsumesPersonalShelfWithoutNearbyShelf(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_personal_shelf_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            var personalInventory = player.getCapability(Capabilities.PERSONAL_INVENTORY)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Linear Build GameTest"));
            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(personalInventory.getHandler().getStackInSlot(0).isEmpty(),
                    "Linear Build should consume Personal Shelf inventory without requiring a nearby shelf block");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Personal Shelf has enough blocks");
        });
    }

    static void linearBuildPrefersEnderChestBeforeCreateToolbox(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var toolboxPos = new BlockPos(0, 2, 0);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_ender_before_toolbox_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            player.getEnderChestInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);
            invokeCreateGameTestHookVoid(
                    "placeToolboxWithItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    new ItemStack(Items.OAK_PLANKS, 2)
            );

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(player.getEnderChestInventory().getItem(0).isEmpty(),
                    "Linear Build should consume Ender Chest blocks before Create Toolbox blocks");
            var toolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    0
            );
            helper.assertTrue(toolboxStack.is(Items.OAK_PLANKS) && toolboxStack.getCount() == 2,
                    "Linear Build consumed Create Toolbox before Ender Chest");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Ender Chest has enough blocks");
        });
    }

    static void linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(5, 3, 2);
        var toolboxPos = new BlockPos(0, 2, 0);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_toolbox_before_trunk_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
        helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
        trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(targetPos, Blocks.STONE);
        invokeCreateGameTestHookVoid(
                "placeToolboxWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(toolboxPos),
                new ItemStack(Items.OAK_PLANKS, 2)
        );

        helper.runAtTickTime(2, () -> {
            castLinearBuild(helper, player, targetPos, Direction.WEST);

            var toolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    0
            );
            helper.assertTrue(toolboxStack.isEmpty(),
                    "Linear Build should consume matching blocks from placed Create Toolbox before Companion Trunk");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.is(Items.OAK_PLANKS) && trunkStack.getCount() == 2,
                    "Linear Build consumed Companion Trunk before placed Create Toolbox");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Create Toolbox has enough blocks");
            helper.succeed();
        });
    }

    static void linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(5, 3, 2);
        var placedToolboxPos = new BlockPos(0, 2, 0);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_inventory_toolbox_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
        helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
        trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(targetPos, Blocks.STONE);
        invokeCreateGameTestHookVoid(
                "placeToolboxWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(placedToolboxPos),
                new ItemStack(Items.DIRT, 2)
        );
        var inventoryToolbox = invokeCreateGameTestHookItemStack(
                "createToolboxStackWithItem",
                new Class<?>[]{ItemStack.class},
                new ItemStack(Items.OAK_PLANKS, 2)
        );
        player.getInventory().setItem(10, inventoryToolbox);

        helper.runAtTickTime(2, () -> {
            castLinearBuild(helper, player, targetPos, Direction.WEST);

            var placedToolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(placedToolboxPos),
                    0
            );
            helper.assertTrue(placedToolboxStack.is(Items.DIRT) && placedToolboxStack.getCount() == 2,
                    "Linear Build should skip placed Create Toolbox when it lacks the matching block");
            var inventoryToolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxStackItem",
                    new Class<?>[]{ItemStack.class, int.class},
                    player.getInventory().getItem(10),
                    0
            );
            helper.assertTrue(inventoryToolboxStack.is(Items.OAK_PLANKS) && inventoryToolboxStack.getCount() == 2,
                    "Linear Build should ignore inventory Create Toolbox stacks");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.isEmpty(),
                    "Linear Build should fall back to Companion Trunk after placed Create Toolbox misses");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Companion Trunk has enough blocks");
            helper.succeed();
        });
    }

    static void linearBuildShulkerSourceFollowsServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(false, false, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_shulker_config_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should use held blocks when Shulker sources are disabled");
                var disabledShulkerStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
                helper.assertTrue(disabledShulkerStack.is(Items.OAK_PLANKS) && disabledShulkerStack.getCount() == 2,
                        "Linear Build should not consume Shulker contents while Shulker sources are disabled");
            }

            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 4);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 4), "linear_build_shulker_enabled_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(getNestedContainerItem(player.getInventory().getItem(10), 0).isEmpty(),
                        "Linear Build should consume Shulker contents while Shulker sources are enabled");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should leave held stack untouched while enabled Shulker has enough blocks");
            }
        });
    }

    static void linearBuildShulkerSourceKeepsSlotAfterPartialConsume(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_shulker_slot_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
                player.getInventory().setItem(10, createShulkerWithItem(10, new ItemStack(Items.OAK_PLANKS, 64)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                var shulker = player.getInventory().getItem(10);
                var movedStack = getShulkerSlotItem(shulker, 0);
                helper.assertTrue(movedStack.isEmpty(),
                        "Linear Build should not move partially consumed Shulker contents to slot 0");
                var remainingStack = getShulkerSlotItem(shulker, 10);
                helper.assertTrue(remainingStack.is(Items.OAK_PLANKS) && remainingStack.getCount() == 62,
                        "Linear Build should keep partially consumed Shulker contents in their original slot");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should leave held stack untouched while Shulker has enough blocks");
            }
        });
    }

    static void linearBuildBundleSourceFollowsServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(false, true, false)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_bundle_config_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createBundleWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should use held blocks when Bundle sources are disabled");
                var disabledBundleStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
                helper.assertTrue(disabledBundleStack.is(Items.OAK_PLANKS) && disabledBundleStack.getCount() == 2,
                        "Linear Build should not consume Bundle contents while Bundle sources are disabled");
            }

            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 4);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 4), "linear_build_bundle_enabled_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createBundleWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(getNestedContainerItem(player.getInventory().getItem(10), 0).isEmpty(),
                        "Linear Build should consume Bundle contents while Bundle sources are enabled");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should leave held stack untouched while enabled Bundle has enough blocks");
            }
        });
    }

    private static BlockPos placeSingleLinearBuildBlock(
            GameTestHelper helper,
            ItemStack blockStack,
            BlockState targetState,
            String profileName
    ) {
        var targetPos = new BlockPos(5, 3, 2);
        var placePos = new BlockPos(4, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(3, 3, 2), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, blockStack);
        helper.setBlock(targetPos, targetState);

        castLinearBuild(helper, player, targetPos, Direction.WEST);

        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "Linear Build should consume exactly one block for the single placement");
        return placePos;
    }

    private static void assertLinearBuildRejectsTemplate(
            GameTestHelper helper,
            ItemStack blockStack,
            String profileName,
            String message
    ) {
        var targetPos = new BlockPos(5, 3, 2);
        var placePos = new BlockPos(4, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, blockStack.copy());
        helper.setBlock(targetPos, Blocks.STONE);

        assertLinearBuildPreCastRejected(helper, player, targetPos, Direction.WEST, message);

        helper.assertBlockNotPresent(Block.byItem(blockStack.getItem()), placePos);
        helper.assertTrue(
                ItemStack.isSameItemSameTags(player.getMainHandItem(), blockStack)
                        && player.getMainHandItem().getCount() == blockStack.getCount(),
                message + " without consuming the template"
        );
    }

    private static void assertLinearBuildPreCastRejected(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos targetPos,
            Direction hitFace,
            String message
    ) {
        var spell = (LinearBuild) SpellRegistry.LINEAR_BUILD.get();
        var absoluteTargetPos = helper.absolutePos(targetPos);
        var targetData = new BlockTargetData();
        targetData.setTarget(
                absoluteTargetPos,
                hitFace,
                Vec3.atCenterOf(absoluteTargetPos),
                absoluteTargetPos.relative(hitFace),
                hitFace.getOpposite()
        );
        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Linear Build rejection test could not resolve player magic data");
        helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData), message);
    }

    private static ItemStack createShulkerWithItem(ItemStack stack) {
        return createShulkerWithItem(0, stack);
    }

    private static ItemStack createShulkerWithItem(int slot, ItemStack stack) {
        var shulker = new ItemStack(Items.SHULKER_BOX);
        var items = new ListTag();
        var entry = stack.save(new CompoundTag());
        entry.putByte("Slot", (byte) slot);
        items.add(entry);
        shulker.getOrCreateTagElement("BlockEntityTag").put("Items", items);
        return shulker;
    }

    private static ItemStack createBundleWithItem(ItemStack stack) {
        var bundle = new ItemStack(Items.BUNDLE);
        var items = new ListTag();
        items.add(stack.save(new CompoundTag()));
        bundle.getOrCreateTag().put("Items", items);
        return bundle;
    }

    private static ItemStack getNestedContainerItem(ItemStack containerStack, int slot) {
        var tag = containerStack.getTag();
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        var blockEntityTag = tag.getCompound("BlockEntityTag");
        var items = blockEntityTag.isEmpty()
                ? tag.getList("Items", Tag.TAG_COMPOUND)
                : blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(items.getCompound(slot));
    }

    private static ItemStack getShulkerSlotItem(ItemStack shulker, int slot) {
        var blockEntityTag = shulker.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) {
            return ItemStack.EMPTY;
        }
        var items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        for (var i = 0; i < items.size(); ++i) {
            var entry = items.getCompound(i);
            if (entry.contains("Slot", Tag.TAG_BYTE) && entry.getByte("Slot") == (byte) slot) {
                return ItemStack.of(entry);
            }
        }
        return ItemStack.EMPTY;
    }

    static void dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "dual_acrobat_startup_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
            target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
            target.setHealth(200.0F);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");
            helper.assertTrue(weapon.getStartupTicksRemaining() == DualAcrobatSmgEntity.STARTUP_TICKS,
                    "Dual Acrobat should start with the full startup duration");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS, () -> {
                helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-6f,
                        "Dual Acrobat should not deal damage before completing ten startup ticks");
                helper.assertTrue(weapon.getStartupTicksRemaining() > 0,
                        "Dual Acrobat should still be starting before the tenth elapsed entity tick");
            });

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 3L, () -> {
                helper.assertTrue(target.getHealth() < initialHealth,
                        "Dual Acrobat should start damaging targets after startup completes");
                var healthAfterStartup = target.getHealth();
                target.invulnerableTime = 20;
                target.setDeltaMovement(Vec3.ZERO);

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 7L, () -> {
                    helper.assertTrue(target.getHealth() < healthAfterStartup,
                            "Dual Acrobat should keep firing during CONTINUOUS casting and bypass damage i-frames");
                    var movement = target.getDeltaMovement();
                    helper.assertTrue(Math.abs(movement.x) < 1.0e-6 && Math.abs(movement.z) < 1.0e-6,
                            "Dual Acrobat shots should not knock targets back");
                    helper.assertFalse(weapon.isRemoved(),
                            "Dual Acrobat SMGs should remain while CONTINUOUS casting is active");
                    var healthBeforeSustainedContinuousFire = target.getHealth();

                    helper.runAtTickTime(30, () -> {
                        helper.assertTrue(target.getHealth() < healthBeforeSustainedContinuousFire,
                                "Dual Acrobat should continue firing throughout a sustained CONTINUOUS cast");
                        helper.assertFalse(weapon.isRemoved(),
                                "Dual Acrobat SMGs should remain active throughout a sustained CONTINUOUS cast");
                        spell.onServerCastComplete(level, 1, player, magicData, true);
                        helper.succeed();
                    });
                });
            });
        });
    }

    static void dualAcrobatCompletionDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "dual_acrobat_completion_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
            target.setNoAi(true);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(target.getHealth() < initialHealth,
                        "Dual Acrobat should be firing before normal completion is tested");
                spell.onServerCastComplete(level, 1, player, magicData, false);
                helper.assertTrue(weapon.isRemoved(),
                        "Completing Dual Acrobat should discard its SMGs immediately");
                var healthAtCompletion = target.getHealth();

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 10L, () -> {
                    helper.assertTrue(Math.abs(target.getHealth() - healthAtCompletion) < 1.0e-6f,
                            "Completed Dual Acrobat should not deal damage after its SMGs are discarded");
                    helper.succeed();
                });
            });
        });
    }

    static void dualAcrobatCancelledInterruptionDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "dual_acrobat_cancelled_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
            target.setNoAi(true);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");

            helper.runAtTickTime(4, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() > 0,
                        "Cancelled Dual Acrobat should still be in startup before interruption");
                spell.onServerCastComplete(level, 1, player, magicData, true);
                helper.assertTrue(weapon.isRemoved(),
                        "Cancelling Dual Acrobat should discard its SMGs immediately");

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 3L, () -> {
                    helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-6f,
                            "Cancelled Dual Acrobat should not start firing after the interruption");
                    helper.succeed();
                });
            });
        });
    }

    static void prepareDualAcrobatShootingLane(GameTestHelper helper) {
        // basic_floor の 5x3x5 内に収め、並行 test の射線や地形を変更しない。
        for (var x = 1; x <= 3; ++x) {
            for (var z = 0; z <= 4; ++z) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.AIR);
            }
        }
    }

    static void dualAcrobatCounterspellInterruptDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 2), "dual_acrobat_counter_target_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var counterCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 4), "dual_acrobat_counter_caster_test");
            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should have an SMG pair before Counterspell");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() == 0,
                        "Counterspell regression should exercise Dual Acrobat while it is firing");
                var counterspell = new CounterSpellEvent(counterCaster, player);
                DualAcrobatCounterSpellEvent.onCounterSpell(counterspell);
                helper.assertTrue(weapon.isRemoved(),
                        "Counterspell should discard the target's Dual Acrobat SMGs immediately");
                spell.onServerCastComplete(level, 1, player, magicData, true);
                helper.succeed();
            });
        });
    }

    static void dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var weaponOwner = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 2), "dual_acrobat_other_owner_test");
            var counterTarget = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 2), "dual_acrobat_unrelated_target_test");
            var counterCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 2, 4), "dual_acrobat_unrelated_counter_caster_test");
            var spell = beginDualAcrobatCast(level, weaponOwner, 1);
            var magicData = MagicData.getPlayerMagicData(weaponOwner);
            var weapon = findDualAcrobatSmg(level, weaponOwner);
            helper.assertTrue(weapon != null, "Dual Acrobat should have an SMG pair before unrelated Counterspell");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() == 0,
                        "Other-owner Counterspell regression should exercise a firing Dual Acrobat");
                var counterspell = new CounterSpellEvent(counterCaster, counterTarget);
                DualAcrobatCounterSpellEvent.onCounterSpell(counterspell);

                helper.assertFalse(weapon.isRemoved(),
                        "Counterspell should not discard a nearby other owner's Dual Acrobat SMGs");
                spell.onServerCastComplete(level, 1, weaponOwner, magicData, true);
                helper.succeed();
            });
        });
    }

    static void counterspellCompatMagicMobEffectsAreMagicMobEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertMagicMobEffect(helper, EffectRegistry.ARCANE_CHARGE.get(), "ArcaneCharge");
            assertMagicMobEffect(helper, EffectRegistry.SENSE_SENSOR.get(), "SenseSensor");
            assertMagicMobEffect(helper, EffectRegistry.ECHO_SPELL.get(), "EchoSpell");
            assertMagicMobEffect(helper, EffectRegistry.MIST_FORM.get(), "MistFormEffect");
            assertMagicMobEffect(helper, EffectRegistry.PALETTE_RECEPTION.get(), "PaletteReception");
            assertMagicMobEffect(helper, EffectRegistry.SPECTRAL_WING.get(), "SpectralWingEffect");
        });
    }

    static void assertMagicMobEffect(GameTestHelper helper, MobEffect effect, String effectName) {
        helper.assertTrue(effect instanceof MagicMobEffect,
                effectName + " should be removable by Counterspell as MagicMobEffect");
    }

    static void spectralWingEffectRemovalClearsFlightState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 8, 0), "spectral_wing_counterspell_test");
            SpellRegistry.SPECTRAL_WING.get().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Spectral Wing test player should have Codex spell data");
            var state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
            helper.assertTrue(state.active && state.startedBySpell,
                    "Spectral Wing cast should activate flight state before effect removal");
            helper.assertTrue(player.hasEffect(EffectRegistry.SPECTRAL_WING.get()),
                    "Spectral Wing cast should apply visual MagicMobEffect before removal");

            player.removeEffect(EffectRegistry.SPECTRAL_WING.get());
            jp.aquafactory.apprenticecodex.spell.spectralwing.SpectralWingFlightEvent.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player)
            );

            state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
            helper.assertFalse(player.hasEffect(EffectRegistry.SPECTRAL_WING.get()),
                    "Removing Spectral Wing effect should not be refreshed from stale state");
            helper.assertFalse(state.active || state.startedBySpell || state.launchGraceTicks != 0 || state.waterGraceTicks != 0,
                    "Removing Spectral Wing effect should clear flight state");
            helper.assertFalse(player.isFallFlying(),
                    "Removing Spectral Wing effect should stop fall flying");
        });
    }

    static void counterspellCompatMagicConstructsDeactivateSafely(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "counterspell_construct_caster_test");
            var counterMagicData = MagicData.getPlayerMagicData(counterCaster);

            var assistOwner = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_antimagic_owner_test");
            var assistWing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), level, assistOwner);
            level.addFreshEntity(assistWing);
            setAssistWingsState(assistOwner, 1, assistWing.getId());
            ((AntiMagicSusceptible) assistWing).onAntiMagic(counterMagicData);
            var assistState = Capabilities.getSpellDataOrNull(assistOwner).get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            helper.assertTrue(assistWing.isRemoved(), "Assist Wings wing should be discarded by anti-magic");
            helper.assertTrue(assistState.localEntityId == -1 && assistState.doneJump == 1,
                    "Assist Wings anti-magic should clear only the managed wing id");

            var magnetOwner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "auto_magnet_antimagic_owner_test");
            AutoMagnetFamiliarManager.activate(magnetOwner, 6.0D, 0.0D);
            var familiar = level.getEntitiesOfClass(AutoMagnetFamiliarEntity.class, magnetOwner.getBoundingBox().inflate(4.0D))
                    .stream()
                    .filter(entity -> entity.getOwner() == magnetOwner)
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(familiar != null, "AutoMagnet anti-magic test should spawn a familiar");
            ((AntiMagicSusceptible) familiar).onAntiMagic(counterMagicData);
            var magnetState = Capabilities.getSpellDataOrNull(magnetOwner).get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
            helper.assertTrue(familiar.isRemoved(), "AutoMagnet familiar should be discarded by anti-magic");
            helper.assertFalse(magnetState.active || magnetState.getFamiliarUuid() != null,
                    "AutoMagnet anti-magic should clear active state and familiar UUID");

            var demicreatorOwner = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 0), "demicreator_wings_antimagic_owner_test");
            var demicreatorSpell = (DemicreatorWings) SpellRegistry.DEMICREATOR_WINGS.get();
            var demicreatorMagicData = MagicData.getPlayerMagicData(demicreatorOwner);
            DemicreatorWingsManager.activate(demicreatorOwner, 1, CastSource.SPELLBOOK, demicreatorMagicData, demicreatorSpell);
            DemicreatorWingsManager.ensureFlightGranted(demicreatorOwner);
            var core = DemicreatorWingsManager.getManagedCore(demicreatorOwner);
            helper.assertTrue(core instanceof AntiMagicSusceptible, "Demicreator Wings core should be anti-magic susceptible");
            ((AntiMagicSusceptible) core).onAntiMagic(counterMagicData);
            var demicreatorState = Capabilities.getSpellDataOrNull(demicreatorOwner).get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
            helper.assertFalse(demicreatorState.active || demicreatorState.coreEntityId >= 0 || demicreatorState.wingEntityId >= 0 || demicreatorState.grantedFlight,
                    "Demicreator Wings anti-magic should reset managed state");
            helper.assertFalse(demicreatorOwner.getAbilities().mayfly || demicreatorOwner.getAbilities().flying,
                    "Demicreator Wings anti-magic should strip granted flight");
            helper.assertFalse(demicreatorMagicData.getPlayerRecasts().hasRecastForSpell(demicreatorSpell),
                    "Demicreator Wings anti-magic should remove its recast");

        });
    }

    static void healingBloomAntiMagicUsesDeathCleanup(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_antimagic_owner_test");
        var anchorPos = new BlockPos(0, 2, 0);
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        castHealingBloom(helper, owner, 1, anchorPos, false);
        var bloom = getSingleLivingHealingBloom(helper, owner);
        setHealingBloomFruitCount(bloom, 2);
        bloom.setOwner(owner);
        level.setBlockAndUpdate(absoluteAnchorPos.above(), BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState());

        var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "healing_bloom_antimagic_caster_test");
        ((AntiMagicSusceptible) bloom).onAntiMagic(MagicData.getPlayerMagicData(counterCaster));

        helper.runAtTickTime(3, () -> {
            var spellData = Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null, "Healing Bloom anti-magic test should resolve owner spell data");
            helper.assertTrue(getOwnedHealingBlooms(helper, owner).isEmpty(),
                    "Healing Bloom anti-magic should leave no living managed bloom");
            helper.assertTrue(spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE).getBloomUuid() == null,
                    "Healing Bloom anti-magic should clear managed bloom UUID");
            helper.assertTrue(!level.getBlockState(absoluteAnchorPos.above()).is(BlockRegistry.HEALING_BLOOM_LIGHT.get()),
                    "Healing Bloom anti-magic should remove its light block");
            var droppedBerries = level.getEntitiesOfClass(ItemEntity.class, new AABB(absoluteAnchorPos).inflate(1.5D)).stream()
                    .filter(itemEntity -> itemEntity.getItem().is(ItemRegistry.COMFORT_BERRIES.get()))
                    .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                    .sum();
            helper.assertTrue(droppedBerries == 2,
                    "Healing Bloom anti-magic should use normal death drops for stored fruit");
            helper.succeed();
        });
    }

    static void counterspellCompatProjectilesFizzleHarmlessly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "counterspell_projectile_fizzle_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Counterspell projectile harmless target");
            spawnCounterspellTestEntity(helper, target, new Vec3(2.5D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var compoundPhial = new CompoundPhialProjectileEntity(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), level, caster);
            compoundPhial.setDamage(20.0F);
            compoundPhial.setSplashRadius(4.0F);
            compoundPhial.setPotionColorRandom(level);
            spawnCounterspellTestEntity(helper, compoundPhial, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, compoundPhial, "Compound Phial");

            var extract = new ExtractPotionProjectileEntity(EntityRegistry.EXTRACT_POTION_PROJECTILE.get(), level, caster);
            extract.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.POISON));
            spawnCounterspellTestEntity(helper, extract, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, extract, "Extract potion");

            var flySwatter = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), level, caster);
            flySwatter.setDamage(20.0F);
            flySwatter.setRadius(4.0F);
            spawnCounterspellTestEntity(helper, flySwatter, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, flySwatter, "Fly Swatter");

            var stellar = new IlluminateStellarStarEntity(EntityRegistry.ILLUMINATE_STELLAR_STAR.get(), level, caster);
            stellar.setDamage(20.0F);
            stellar.setDriftProfile(new Vec3(1.0D, 0.0D, 0.0D));
            stellar.setFallbackTarget(target);
            spawnCounterspellTestEntity(helper, stellar, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, stellar, "Illuminate Stellar");

            var manaSlash = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), level, caster);
            manaSlash.setDamage(20.0F);
            manaSlash.shoot(new Vec3(1.0D, 0.0D, 0.0D));
            spawnCounterspellTestEntity(helper, manaSlash, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, manaSlash, "Mana Slash");

            var mysticShield = new MysticShieldProjectileEntity(EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), level, caster);
            mysticShield.setDamage(20.0F);
            mysticShield.shoot(new Vec3(1.0D, 0.0D, 0.0D));
            spawnCounterspellTestEntity(helper, mysticShield, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, mysticShield, "Mystic Shield projectile");

            var skyEdge = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, caster);
            skyEdge.setDamage(20.0F);
            skyEdge.setProjectileVelocity(new Vec3(1.0D, 0.0D, 0.0D), 1.0D);
            spawnCounterspellTestEntity(helper, skyEdge, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, skyEdge, "Sky Edge");

            var inscribeIce = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), level, caster);
            inscribeIce.setDamage(20.0F);
            inscribeIce.setBurstDamage(20.0F);
            inscribeIce.setProjectileVelocity(new Vec3(1.0D, 0.0D, 0.0D), 1.0D);
            spawnCounterspellTestEntity(helper, inscribeIce, new Vec3(2.5D, 2.0D, 2.5D));
            assertAntiMagicDiscard(helper, caster, inscribeIce, "Inscribe Ice dagger");

            assertHealthUnchanged(helper, target, targetHealth,
                    "Counterspell projectile fizzle should not damage nearby targets");
            helper.assertFalse(target.hasEffect(MobEffects.POISON),
                    "Counterspell projectile fizzle should not apply potion effects");
        });
    }

    static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_collision_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        // 中心はブロックの外側を通るが、幅 0.25 の当たり箱だけが z=2 の面を擦る軌道。
        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var clearStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 4.5D));
        var grazingProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var clearProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, clearStart);

        helper.runAtTickTime(2, () -> {
            grazingProjectiles.forEach((name, projectile) -> helper.assertTrue(projectile.isRemoved(),
                    name + " should treat a bounding-box block graze as an impact"));
            clearProjectiles.forEach((name, projectile) -> {
                helper.assertFalse(projectile.isRemoved(),
                        name + " should remain active without a physical collision");
                helper.assertTrue(projectile.position().distanceToSqr(clearStart) > 1.0D,
                        name + " should preserve unobstructed straight movement");
                projectile.discard();
            });
            helper.succeed();
        });
    }

    static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_cancelled_collision_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var centerHitStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 2.5D));
        var grazingProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var centerHitProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, centerHitStart);
        var cancelledProjectiles = new LinkedHashSet<Projectile>();
        cancelledProjectiles.addAll(grazingProjectiles.values());
        cancelledProjectiles.addAll(centerHitProjectiles.values());
        var impactCount = new AtomicInteger();
        java.util.function.Consumer<ProjectileImpactEvent> impactListener = event -> {
            if (cancelledProjectiles.contains(event.getProjectile())
                    && event.getRayTraceResult() instanceof BlockHitResult) {
                impactCount.incrementAndGet();
                // Forge 1.20.1 が維持する旧 MOD 向けの実キャンセル経路を検証する。
                ((net.minecraftforge.eventbus.api.Event) event).setCanceled(true);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(impactListener);
        helper.runAtTickTime(2, () -> {
            try {
                helper.assertTrue(impactCount.get() >= cancelledProjectiles.size(),
                        "Each center or bounding-box block impact should fire ProjectileImpactEvent: "
                                + impactCount.get() + " / " + cancelledProjectiles.size());
                cancelledProjectiles.forEach(projectile -> {
                    helper.assertFalse(projectile.isRemoved(),
                            projectile.getType() + " should continue after a canceled block impact");
                    helper.assertTrue(projectile.getX() > grazingStart.x + 2.0D,
                            projectile.getType() + " should preserve movement after a canceled block impact");
                    helper.assertTrue(projectile.getDeltaMovement().x > 1.0D,
                            projectile.getType() + " should preserve velocity after a canceled block impact");
                    projectile.discard();
                });
                helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(impactListener);
            }
        });
    }

    static void straightProjectilesRespectNonDefaultBlockImpactResults(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_impact_result_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var skipEntityProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var stopAtCurrentProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var stopWithoutDamageProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var testedProjectiles = new LinkedHashSet<Projectile>();
        testedProjectiles.addAll(skipEntityProjectiles.values());
        testedProjectiles.addAll(stopAtCurrentProjectiles.values());
        testedProjectiles.addAll(stopWithoutDamageProjectiles.values());
        var impactCount = new AtomicInteger();
        java.util.function.Consumer<ProjectileImpactEvent> impactListener = event -> {
            if (!(event.getRayTraceResult() instanceof BlockHitResult)) {
                return;
            }
            if (skipEntityProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            } else if (stopAtCurrentProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);
            } else if (stopWithoutDamageProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(impactListener);
        helper.runAtTickTime(2, () -> {
            try {
                helper.assertTrue(impactCount.get() >= testedProjectiles.size(),
                        "Each non-default block impact should fire ProjectileImpactEvent: "
                                + impactCount.get() + " / " + testedProjectiles.size());
                testedProjectiles.forEach(projectile -> helper.assertTrue(projectile.isRemoved(),
                        projectile.getType() + " should stop for a non-default block impact result"));
                helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(impactListener);
            }
        });
    }

    private static Map<String, Projectile> spawnStraightProjectileCollisionTestSet(
            ServerLevel level,
            LivingEntity caster,
            Vec3 position
    ) {
        var direction = new Vec3(1.0D, 0.0D, 0.0D);
        var speed = 1.55D;
        var projectiles = new LinkedHashMap<String, Projectile>();

        var skyEdge = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, caster);
        skyEdge.setProjectileVelocity(direction, speed);
        projectiles.put("Sky Edge", skyEdge);

        var inscribeIce = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), level, caster);
        inscribeIce.setProjectileVelocity(direction, speed);
        projectiles.put("Inscribe Ice dagger", inscribeIce);

        var manaForceBlade = new ManaForceBladeProjectileEntity(
                EntityRegistry.MANA_FORCE_BLADE_PROJECTILE.get(), level, caster);
        manaForceBlade.setProjectileVelocity(direction, speed);
        projectiles.put("Mana Force Blade projectile", manaForceBlade);

        var mysticShield = new MysticShieldProjectileEntity(
                EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), level, caster);
        mysticShield.shoot(direction);
        projectiles.put("Mystic Shield projectile", mysticShield);

        projectiles.values().forEach(projectile -> {
            projectile.setPos(position);
            level.addFreshEntity(projectile);
        });
        return projectiles;
    }

    static void magicSpearAntiMagicBurstDoesNotRestart(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "magic_spear_antimagic_repeat_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Magic Spear anti-magic target");
            spawnCounterspellTestEntity(helper, target, new Vec3(2.5D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var spear = new MagicSpearMissileEntity(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), level, caster);
            spear.setup(20.0F, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), target);
            spawnCounterspellTestEntity(helper, spear, new Vec3(2.5D, 2.0D, 2.5D));

            var magicData = MagicData.getPlayerMagicData(caster);
            ((AntiMagicSusceptible) spear).onAntiMagic(magicData);
            helper.assertTrue(spear.isBursting(), "Magic Spear should enter harmless burst state after anti-magic");

            for (var i = 0; i < 7 && !spear.isRemoved(); ++i) {
                spear.tick();
            }

            ((AntiMagicSusceptible) spear).onAntiMagic(magicData);
            helper.assertTrue(spear.isBursting(), "Repeated anti-magic should not leave harmless burst state");

            for (var i = 0; i < 7 && !spear.isRemoved(); ++i) {
                spear.tick();
            }

            helper.assertTrue(spear.isRemoved(),
                    "Repeated anti-magic should not restart Magic Spear harmless burst lifetime");
            assertHealthUnchanged(helper, target, targetHealth,
                    "Magic Spear harmless anti-magic burst should not damage nearby targets");
        });
    }

    static void uniteLunaAntiMagicAmplifiesBurst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "unite_luna_antimagic_test");
            var target = EntityType.SHEEP.create(level);
            helper.assertTrue(target != null, "Failed to create Unite Luna anti-magic target");
            target.setNoAi(true);
            spawnCounterspellTestEntity(helper, target, new Vec3(10.0D, 2.0D, 2.5D));
            var targetHealth = target.getHealth();

            var moon = new UniteLunaMoonEntity(EntityRegistry.UNITE_LUNA_MOON.get(), level, caster);
            moon.setDamage(2.0F);
            spawnCounterspellTestEntity(helper, moon, new Vec3(2.5D, 2.0D, 2.5D));

            helper.assertTrue(moon instanceof AntiMagicSusceptible,
                    "Unite Luna moon should implement AntiMagicSusceptible");
            ((AntiMagicSusceptible) moon).onAntiMagic(MagicData.getPlayerMagicData(caster));

            helper.assertTrue(moon.getBurstKind() == UniteLunaMoonEntity.BURST_KIND_EXPLOSION,
                    "Unite Luna anti-magic should force explosion burst");
            helper.assertTrue(Math.abs(moon.getBurstCubeSize() - 22.0F) < 0.001F,
                    "Unite Luna anti-magic burst cube size should be doubled max: " + moon.getBurstCubeSize());
            var damageTaken = targetHealth - target.getHealth();
            helper.assertTrue(Math.abs(damageTaken - 6.0F) < 0.1F,
                    "Unite Luna anti-magic should triple damage inside doubled range: " + damageTaken);

            var healthAfterFirstBurst = target.getHealth();
            ((AntiMagicSusceptible) moon).onAntiMagic(MagicData.getPlayerMagicData(caster));
            helper.assertTrue(Math.abs(target.getHealth() - healthAfterFirstBurst) < 0.001F,
                    "Repeated Unite Luna anti-magic should not reapply burst damage");
        });
    }

    static void counterspellCompatSpecialPlayerTargetBehaviors(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();

            var mysticCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_counterspell_target_test");
            mysticCaster.setYRot(0.0f);
            mysticCaster.setXRot(0.0f);
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            beginMysticShieldCast(level, mysticCaster, 1);
            var frontAttack = postLivingAttackEventForGameTest(
                    mysticCaster,
                    CombatTools.getDamageSource(level, attacker, DamageTypes.SHOCK),
                    8.0f
            );
            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield setup should store front damage before Counterspell");

            var mysticShieldEntity = level.getEntitiesOfClass(
                            MysticShieldShieldEntity.class,
                            mysticCaster.getBoundingBox().inflate(4.0D)
                    ).stream()
                    .filter(entity -> entity.getOwner() == mysticCaster)
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(mysticShieldEntity != null, "Mystic Shield cast should spawn a shield entity before Counterspell");
            helper.assertFalse(mysticShieldEntity instanceof AntiMagicSusceptible,
                    "Mystic Shield shield entity should not be a direct Counterspell target");

            var counterCaster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 5), "mystic_shield_counterspell_caster_test");
            var mysticCounterspell = new CounterSpellEvent(counterCaster, mysticCaster);
            MysticShieldDefenseEvent.onCounterSpell(mysticCounterspell);
            helper.assertFalse(mysticCounterspell.isCanceled(),
                    "Mystic Shield should not cancel the CounterSpellEvent itself");
            helper.assertTrue(mysticShieldEntity.isFading(),
                    "Mystic Shield should break its shield entity immediately when Counterspell hits the caster");
            completeMysticShieldCast(level, mysticCaster, 1, true);
            var reflected = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, mysticCaster.getBoundingBox().inflate(4.0D));
            helper.assertTrue(reflected.isEmpty(),
                    "Mystic Shield interrupted by Counterspell should clear stored damage without firing a reflection");

            var phalanxTarget = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 0), "phalanx_counterspell_target_test");
            phalanxTarget.setYRot(0.0f);
            phalanxTarget.setXRot(0.0f);
            phalanxTarget.addEffect(new MobEffectInstance(
                    EffectRegistry.PHALANX_STANCE.get(),
                    20,
                    PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            var phalanxFrontCaster = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 3), "phalanx_counterspell_front_test");
            var frontCounterspell = new CounterSpellEvent(phalanxFrontCaster, phalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(frontCounterspell);
            helper.assertTrue(frontCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should cancel Counterspell from the front");

            var phalanxBackCaster = createEquipmentTestPlayer(helper, new BlockPos(4, 2, -3), "phalanx_counterspell_back_test");
            var backCounterspell = new CounterSpellEvent(phalanxBackCaster, phalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(backCounterspell);
            helper.assertFalse(backCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should not cancel Counterspell from behind");

            var normalPhalanxTarget = createEquipmentTestPlayer(helper, new BlockPos(8, 2, 0), "phalanx_counterspell_normal_target_test");
            normalPhalanxTarget.setYRot(0.0f);
            normalPhalanxTarget.addEffect(new MobEffectInstance(
                    EffectRegistry.PHALANX_STANCE.get(),
                    20,
                    PhalanxStance.FIXED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            var normalFrontCaster = createEquipmentTestPlayer(helper, new BlockPos(8, 2, 3), "phalanx_counterspell_normal_front_test");
            var normalCounterspell = new CounterSpellEvent(normalFrontCaster, normalPhalanxTarget);
            PhalanxCounterSpellEvent.onCounterSpell(normalCounterspell);
            helper.assertFalse(normalCounterspell.isCanceled(),
                    "Unenhanced Phalanx stance should not cancel Counterspell");

            var phalanxMysticTarget = createTrackedEquipmentTestPlayer(helper, new BlockPos(12, 2, 0), "mystic_shield_phalanx_cancel_target_test");
            phalanxMysticTarget.setYRot(0.0f);
            phalanxMysticTarget.setXRot(0.0f);
            phalanxMysticTarget.addEffect(new MobEffectInstance(
                    EffectRegistry.PHALANX_STANCE.get(),
                    20,
                    PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER,
                    false,
                    false,
                    true
            ));
            beginMysticShieldCast(level, phalanxMysticTarget, 1);
            var phalanxMysticAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 2, 3));
            var phalanxMysticFrontAttack = postLivingAttackEventForGameTest(
                    phalanxMysticTarget,
                    CombatTools.getDamageSource(level, phalanxMysticAttacker, DamageTypes.SHOCK),
                    8.0f
            );
            helper.assertTrue(phalanxMysticFrontAttack.isCanceled(),
                    "Mystic Shield setup should store front damage before a Phalanx-canceled Counterspell");

            var phalanxMysticCounterCaster = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 5), "mystic_shield_phalanx_cancel_caster_test");
            var phalanxCanceledCounterspell = new CounterSpellEvent(phalanxMysticCounterCaster, phalanxMysticTarget);
            PhalanxCounterSpellEvent.onCounterSpell(phalanxCanceledCounterspell);
            helper.assertTrue(phalanxCanceledCounterspell.isCanceled(),
                    "Enhanced Phalanx stance should cancel Counterspell before Mystic Shield observes it");
            MysticShieldDefenseEvent.onCounterSpell(phalanxCanceledCounterspell);
            completeMysticShieldCast(level, phalanxMysticTarget, 1, false);
            var reflectedAfterCanceledCounterspell = level.getEntitiesOfClass(
                    MysticShieldProjectileEntity.class,
                    phalanxMysticTarget.getBoundingBox().inflate(4.0D)
            );
            helper.assertTrue(reflectedAfterCanceledCounterspell.size() == 1,
                    "Mystic Shield should keep stored reflection when Counterspell is canceled but got "
                            + reflectedAfterCanceledCounterspell.size());
        });
    }

    static void assertAntiMagicDiscard(GameTestHelper helper, FakePlayer caster, net.minecraft.world.entity.Entity entity, String entityName) {
        helper.assertTrue(entity instanceof AntiMagicSusceptible,
                entityName + " should implement AntiMagicSusceptible");
        ((AntiMagicSusceptible) entity).onAntiMagic(MagicData.getPlayerMagicData(caster));
        helper.assertTrue(entity.isRemoved(), entityName + " should be discarded by anti-magic");
    }

    static void assertHealthUnchanged(GameTestHelper helper, net.minecraft.world.entity.LivingEntity target, float expectedHealth, String message) {
        helper.assertTrue(Math.abs(target.getHealth() - expectedHealth) < 0.001F,
                message + ": expected=" + expectedHealth + ", actual=" + target.getHealth());
    }

    static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            helper.setBlock(new BlockPos(2, 3, 1), Blocks.GRASS);
            helper.setBlock(new BlockPos(4, 3, 1), Blocks.STONE);

            var start = helper.absoluteVec(new Vec3(0.5D, 3.5D, 1.5D));
            var arcaneBeam = new ArcaneBeamEntity(EntityRegistry.ARCANE_BEAM.get(), level);
            arcaneBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            arcaneBeam.updateLength(8.0F, level);
            assertClose(helper, arcaneBeam.getLength(), 3.5D, 0.01D,
                    "Arcane Beam should ignore grass collision and stop at stone");

            var phalanxBeam = new PhalanxChargeBeamEntity(EntityRegistry.PHALANX_CHARGE_BEAM.get(), level);
            phalanxBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            phalanxBeam.setup(level, 8.0F, 0.15F, 1.0F);
            assertClose(helper, phalanxBeam.getLength(), 3.5D, 0.01D,
                    "Phalanx Charge beam should ignore grass collision and stop at stone");
        });
    }

    static void spawnCounterspellTestEntity(GameTestHelper helper, net.minecraft.world.entity.Entity entity, Vec3 localPos) {
        var absolutePos = helper.absoluteVec(localPos);
        entity.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, 0.0F, 0.0F);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
    }

    static void inscribeIceCastUsesShortThrowJob(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "inscribe_ice_fan_test");
        player.setYRot(0.0F);
        player.setXRot(-30.0F);

        var spell = (InscribeIce) SpellRegistry.INSCRIBE_ICE.get();
        spell.onCast(level, 5, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

        var expectedCount = spell.getProjectileCount(5, player);
        var firstWave = level.getEntitiesOfClass(
                InscribeIceDaggerEntity.class,
                player.getBoundingBox().inflate(4.0D),
                projectile -> projectile.getOwner() == player
        );
        helper.assertTrue(firstWave.size() > 0 && firstWave.size() < expectedCount,
                "Inscribe Ice should spawn the first dagger wave immediately without creating all daggers: "
                        + firstWave.size() + " / " + expectedCount);

        var expectedSpawnPosition = player.getEyePosition()
                .add(player.getLookAngle().normalize().scale(0.4D))
                .add(0.0D, -0.25D, 0.0D);
        for (var projectile : firstWave) {
            assertInscribeIceDaggerLaunch(helper, projectile);
            helper.assertTrue(projectile.position().distanceToSqr(expectedSpawnPosition) < 0.08D,
                    "Inscribe Ice dagger should spawn near the centered magic missile style launch position");
        }

        helper.runAfterDelay(3, () -> {
            var spawned = level.getEntitiesOfClass(
                    InscribeIceDaggerEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    projectile -> projectile.getOwner() == player
            );
            helper.assertTrue(spawned.size() == expectedCount,
                    "Inscribe Ice short throw job should finish within 3 ticks: "
                            + spawned.size() + " / " + expectedCount);

            var sorted = spawned.stream()
                    .sorted(Comparator.comparingDouble(projectile -> -projectile.getDeltaMovement().x))
                    .toList();
            for (var projectile : sorted) {
                assertInscribeIceDaggerLaunch(helper, projectile);
            }

            helper.assertTrue(sorted.get(0).getDeltaMovement().x > 0.01D,
                    "Inscribe Ice should include the caster's right side of the fan");
            helper.assertTrue(sorted.get(sorted.size() - 1).getDeltaMovement().x < -0.01D,
                    "Inscribe Ice should include the caster's left side of the fan");
            assertInscribeIceCenterDaggerLaunchesForward(helper, player, sorted);
            helper.succeed();
        });
    }

    static void assertInscribeIceCenterDaggerLaunchesForward(GameTestHelper helper, LivingEntity caster,
                                                             List<InscribeIceDaggerEntity> projectiles) {
        var centerProjectile = projectiles.stream()
                .min(Comparator.comparingDouble(projectile -> Math.abs(projectile.getDeltaMovement().x)))
                .orElseThrow();
        var movement = centerProjectile.getDeltaMovement();
        var expectedForward = caster.getLookAngle().normalize();
        var actualForward = movement.normalize();
        helper.assertTrue(Math.abs(movement.x) < 1.0E-6D,
                "Inscribe Ice center dagger should not receive yaw jitter: " + movement);
        helper.assertTrue(actualForward.dot(expectedForward) > 0.999D,
                "Inscribe Ice center dagger should follow the caster's exact look direction: " + movement);
    }

    static void assertInscribeIceDaggerLaunch(GameTestHelper helper, InscribeIceDaggerEntity projectile) {
        helper.assertTrue(projectile.isNoGravity(), "Inscribe Ice dagger should not use gravity");
        helper.assertTrue(projectile.getDeltaMovement().y > 0.1D,
                "Inscribe Ice dagger should follow the caster's upward look direction");

        var speed = projectile.getDeltaMovement().length();
        helper.assertTrue(speed >= InscribeIceDaggerEntity.SPEED * 0.91D
                        && speed <= InscribeIceDaggerEntity.SPEED * 1.09D,
                "Inscribe Ice dagger speed should stay near 70% of Sky Edge baseline: " + speed);
    }

    static void notchedFrozenStacksAndBurstsOnThirdStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "notched_frozen_owner_test");
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            setMaxHealthForDamageTest(target, 100.0F);

            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);
            assertNotchedFrozen(helper, target, 0);
            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);
            assertNotchedFrozen(helper, target, 1);
            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);

            helper.assertFalse(target.hasEffect(EffectRegistry.NOTCHED_FROZEN.get()),
                    "Inscribe Ice should remove Notched Frozen when the third application succeeds");
            helper.assertTrue(target.getHealth() < 100.0F,
                    "Inscribe Ice burst should damage the detonated target");
        });
    }

    static void notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var effect = EffectRegistry.NOTCHED_FROZEN.get();
            var modifier = effect.getAttributeModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_MAGIC_RESIST.get());

            helper.assertTrue(modifier == null, "Notched Frozen should not modify ice spell resistance while disabled");

            target.setTicksFrozen(40);
            effect.applyEffectTick(target, 0);
            helper.assertTrue(target.getTicksFrozen() == 42,
                    "Notched Frozen should offset natural frozen tick decay when freezing is already active");

            target.setTicksFrozen(0);
            effect.applyEffectTick(target, 0);
            helper.assertTrue(target.getTicksFrozen() == 0,
                    "Notched Frozen should not start freezing by itself");
        });
    }

    static void inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(GameTestHelper helper) {
        helper.runAfterDelay(3, () -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "inscribe_ice_burst_owner_test");
            var directOrigin = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            var directTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            var chainOrigin = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 0));
            var chainTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 0));
            var playerTarget = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "inscribe_ice_burst_player_test");

            setMaxHealthForDamageTest(directOrigin, 100.0F);
            setMaxHealthForDamageTest(directTarget, 100.0F);
            setMaxHealthForDamageTest(chainOrigin, 100.0F);
            setMaxHealthForDamageTest(chainTarget, 100.0F);
            playerTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D);
            playerTarget.setHealth(100.0F);

            InscribeIceBurst.burstFromDagger(level, directOrigin, owner, owner, 10.0F);
            InscribeIceBurst.burstChain(level, chainOrigin, owner, owner, 5.0F, new java.util.HashSet<>());

            var directLoss = 100.0F - directTarget.getHealth();
            var chainLoss = 100.0F - chainTarget.getHealth();
            helper.assertTrue(directLoss > 0.0F && chainLoss > 0.0F,
                    "Inscribe Ice direct and chained bursts should both damage nearby mobs");
            helper.assertTrue(Math.abs(chainLoss * 2.0F - directLoss) < 0.25F,
                    "Inscribe Ice chained burst damage should be half of direct burst damage: direct="
                            + directLoss + ", chain=" + chainLoss);
            helper.assertTrue(playerTarget.getHealth() == 100.0F,
                    "Inscribe Ice burst should not damage players in the blast area");
            helper.succeed();
        });
    }

    static void assertNotchedFrozen(GameTestHelper helper, LivingEntity target, int expectedAmplifier) {
        var instance = target.getEffect(EffectRegistry.NOTCHED_FROZEN.get());
        helper.assertTrue(instance != null, "Target should have Notched Frozen");
        helper.assertTrue(instance != null && instance.getAmplifier() == expectedAmplifier,
                "Notched Frozen amplifier mismatch: expected=" + expectedAmplifier
                        + ", actual=" + (instance == null ? "missing" : instance.getAmplifier()));
        helper.assertTrue(instance != null && instance.getDuration() == 20 * 15,
                "Notched Frozen should refresh to 15 seconds");
        helper.assertTrue(instance != null && !instance.isVisible(),
                "Notched Frozen should suppress vanilla potion particles");
    }

    static void setMaxHealthForDamageTest(LivingEntity target, float health) {
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        target.setHealth(health);
    }

    static void heavenlyFistImpactAabbAppliesDamageAndGravityBound(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_owner_test");
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        target.setNoAi(true);
        setMaxHealthForDamageTest(target, 100.0F);

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 2.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            helper.assertTrue(target.getHealth() < 100.0F,
                    "Heavenly Fist should damage CombatTarget inside its locked AABB");
            helper.assertTrue(target.hasEffect(EffectRegistry.GRAVITY_BOUND.get()),
                    "Heavenly Fist should apply Gravity Bound to damaged CombatTarget");
            helper.succeed();
        });
    }

    static void heavenlyFistImpactDamagesNonLivingCombatTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_crystal_owner_test");
        var crystal = helper.spawn(EntityType.END_CRYSTAL, new BlockPos(2, 2, 0));

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 2.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            helper.assertTrue(crystal.isRemoved() || !crystal.isAlive(),
                    "Heavenly Fist should damage non-Living CombatTarget inside its locked AABB");
            helper.succeed();
        });
    }

    static void heavenlyFistImpactDoesNotTrackMovedTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_lock_owner_test");
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        target.setNoAi(true);
        setMaxHealthForDamageTest(target, 100.0F);

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 1.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(5, () -> target.moveTo(helper.absolutePos(new BlockPos(7, 2, 0)).getCenter()));
        helper.runAtTickTime(28, () -> {
            helper.assertTrue(Math.abs(target.getHealth() - 100.0F) < 0.01F,
                    "Heavenly Fist should not chase a target after locking the impact position");
            helper.assertFalse(target.hasEffect(EffectRegistry.GRAVITY_BOUND.get()),
                    "Heavenly Fist should not apply Gravity Bound to a moved-out target");
            helper.succeed();
        });
    }

    static void heavenlyFistProcessesCreateDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_depot_owner_test");
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.SUGAR_CANE)
        );

        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 2.0F, 1);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            var result = invokeCreateGameTestHookItemStack("getDepotItem", level, helper.absolutePos(targetPos));
            helper.assertTrue(result.is(Items.PAPER), "Heavenly Fist should process Create Depot items: " + result);
            helper.succeed();
        });
    }

    static void heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var farItemPos = targetPos.offset(3, 0, 0);
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_drop_area_owner_test");
        spawnNoGravityItem(helper, farItemPos, new ItemStack(Items.SUGAR_CANE));

        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 4.0F, 1);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            var farItemCenter = helper.absoluteVec(Vec3.atCenterOf(farItemPos));
            helper.assertTrue(hasItemEntityWithin(level, Items.SUGAR_CANE, farItemCenter, 0.75D),
                    "Heavenly Fist should leave dropped Create inputs outside its 3x2x3 process area");
            helper.assertFalse(hasItemEntityWithin(level, Items.PAPER, farItemCenter, 0.75D),
                    "Heavenly Fist should not process dropped Create items outside its 3x2x3 process area");
            helper.succeed();
        });
    }

    static void heavenlyFistProcessesCreateBasinCompacting(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR),
                new ItemStack(cinderFlour)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
            helper.assertTrue(resultCount == 1, "Heavenly Fist should process Create Basin compacting once: " + resultCount);
            helper.succeed();
        });
    }

    static void heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG, 2),
                new ItemStack(Items.SUGAR, 2),
                new ItemStack(cinderFlour, 2)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
            var remainingCinderFlourCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(cinderFlour));
            helper.assertTrue(resultCount == 1, "Heavenly Fist should spend one budget per Basin recipe: " + resultCount);
            helper.assertTrue(remainingCinderFlourCount == 1,
                    "Heavenly Fist should leave the second compacting input set when budget is one: " + remainingCinderFlourCount);
            helper.succeed();
        });
    }

    static void heavenlyFistCreateCompactingDenylistLeavesBasinItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var deniedRecipeId = ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "compacting/blaze_cake");
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        var override = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                List.of(),
                List.of(),
                List.of(),
                List.of(deniedRecipeId.toString()),
                List.of()
        );
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR),
                new ItemStack(cinderFlour)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            try {
                var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
                var remainingCinderFlourCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(cinderFlour));
                helper.assertTrue(resultCount == 0,
                        "Heavenly Fist should not process denied Create Basin compacting recipes: " + resultCount);
                helper.assertTrue(remainingCinderFlourCount == 1,
                        "Heavenly Fist should leave denied compacting inputs untouched: " + remainingCinderFlourCount);
                helper.succeed();
            } finally {
                override.close();
            }
        });
    }

    static void heavenlyFistSkipsCreateBasinCompressionCrafting(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var zincIngot = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "zinc_ingot"));
        var zincBlock = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "zinc_block"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(zincIngot, 9)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var zincBlockCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(zincBlock));
            var zincIngotCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(zincIngot));
            helper.assertTrue(zincBlockCount == 0, "Heavenly Fist should not run Create Basin compression crafting: " + zincBlockCount);
            helper.assertTrue(zincIngotCount == 9, "Heavenly Fist should leave compression crafting inputs untouched: " + zincIngotCount);
            helper.succeed();
        });
    }

    private static void spawnHeavenlyFistForCreateProcess(GameTestHelper helper, BlockPos targetPos, int maxProcessCount) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_process_owner_test");
        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 2.0F, maxProcessCount);
        level.addFreshEntity(fist);
    }

    static void gravityBoundPullsAirborneTargetsDown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var phantom = helper.spawn(EntityType.PHANTOM, new BlockPos(0, 6, 0));
            phantom.setNoAi(true);
            phantom.setDeltaMovement(0.0D, 0.6D, 0.0D);

            EffectRegistry.GRAVITY_BOUND.get().applyEffectTick(phantom, 9);

            helper.assertTrue(phantom.getDeltaMovement().y <= -1.25D,
                    "Gravity Bound should force airborne targets downward regardless of amplifier: "
                            + phantom.getDeltaMovement().y);
        });
    }

    static void mistFormAppliesEffectAndFixedAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_attribute_test");
            var spell = (jp.aquafactory.apprenticecodex.spell.mistform.MistForm) SpellRegistry.MIST_FORM.get();
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            var instance = player.getEffect(EffectRegistry.MIST_FORM.get());
            helper.assertTrue(instance != null, "Mist Form cast should apply the Mist Form effect");
            var expectedDuration = Math.round(10 * 20 * spell.getSpellPower(1, player) / 100.0F);
            helper.assertTrue(instance != null && instance.getDuration() == expectedDuration,
                    "Mist Form effect duration should use existing spell power duration: "
                            + (instance == null ? "null" : instance.getDuration()) + " / " + expectedDuration);

            var effect = EffectRegistry.MIST_FORM.get();
            assertMistFormModifierAmount(helper, effect, Attributes.MOVEMENT_SPEED,
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                    jp.aquafactory.apprenticecodex.effect.MistFormEffect.MOVEMENT_SPEED_BONUS,
                    "Mist Form should provide fixed movement speed");
            assertMistFormModifierAmount(helper, effect, net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(),
                    AttributeModifier.Operation.ADDITION,
                    jp.aquafactory.apprenticecodex.effect.MistFormEffect.STEP_HEIGHT_ADDITION,
                    "Mist Form should provide fixed step assist");
            assertMistFormModifierAmount(helper, effect,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_MAGIC_RESIST.get(),
                    AttributeModifier.Operation.ADDITION,
                    jp.aquafactory.apprenticecodex.effect.MistFormEffect.SCHOOL_RESIST_WEAKNESS,
                    "Mist Form should provide fixed fire weakness");
            assertMistFormModifierAmount(helper, effect,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_MAGIC_RESIST.get(),
                    AttributeModifier.Operation.ADDITION,
                    jp.aquafactory.apprenticecodex.effect.MistFormEffect.SCHOOL_RESIST_WEAKNESS,
                    "Mist Form should provide fixed holy weakness");
        });
    }

    static void mistFormSuppressesAwarenessWithinThirtyTwoBlocks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_awareness_test");
            var nearZombie = createTargetingZombie(helper, new BlockPos(4, 2, 0), player, "near");
            var farZombie = createTargetingZombie(helper, new BlockPos(36, 2, 0), player, "far");

            SpellRegistry.MIST_FORM.get().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            helper.assertTrue(nearZombie.getTarget() == null,
                    "Mist Form should clear nearby mob target within 32 blocks");
            helper.assertTrue(nearZombie.getLastHurtByMob() == null,
                    "Mist Form should clear nearby mob last hurt by mob within 32 blocks");
            helper.assertTrue(farZombie.getTarget() == player,
                    "Mist Form should not clear mob target outside 32 blocks");
            helper.assertTrue(farZombie.getLastHurtByMob() == player,
                    "Mist Form should not clear mob last hurt by mob outside 32 blocks");
        });
    }

    static void mistFormDamageToLivingTargetRemovesEffect(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_damage_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            var target = createTargetingZombie(helper, new BlockPos(2, 2, 0), player, "damage_target");

            target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);

            helper.assertFalse(player.hasEffect(EffectRegistry.MIST_FORM.get()),
                    "Mist Form should be removed when the caster damages a living target");
        });
    }

    static void mistFormSlowsFallingWithoutAmplifierScaling(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 8, 0), "mist_form_fall_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 9, false, false, true));
            player.fallDistance = 12.0F;
            player.hurtMarked = false;
            player.setDeltaMovement(0.12D, -0.7D, -0.08D);

            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player)
            );

            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.08D) < 1.0E-9D,
                    "Mist Form should clamp falling speed without amplifier scaling: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.12D) < 1.0E-9D
                            && Math.abs(player.getDeltaMovement().z + 0.08D) < 1.0E-9D,
                    "Mist Form slow falling should preserve horizontal movement: " + player.getDeltaMovement());
            helper.assertFalse(player.hurtMarked,
                    "Mist Form slow falling should not force velocity sync that overwrites client horizontal input");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Mist Form should reset fall distance while slowing descent");
        });
    }

    static void mistFormStandsOnLiquidAndSneakSinks(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            // 各検査は同期的に完了するため、隣接 test の領域へ出ず中央の basin を再利用する.
            var basinPlayerPos = new BlockPos(2, 3, 2);
            var waterWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_water_walk_test");
            var waterSupportPos = waterWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), waterSupportPos, Blocks.WATER.defaultBlockState());
            waterWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            waterWalker.setDeltaMovement(0.1D, -0.2D, 0.0D);
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, waterWalker)
            );
            helper.assertTrue(waterWalker.onGround() && waterWalker.getDeltaMovement().y == 0.0D,
                    "Mist Form should hold the player on liquid surface without downward motion: onGround="
                            + waterWalker.onGround()
                            + ", y=" + waterWalker.getY()
                            + ", dy=" + waterWalker.getDeltaMovement().y);
            helper.assertTrue(Math.abs(waterWalker.getDeltaMovement().x - 0.1D) < 1.0E-9D,
                    "Mist Form liquid standing should preserve horizontal movement on water");

            var sneakingWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_sneak_sink_test");
            sneakingWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            sneakingWalker.setShiftKeyDown(true);
            sneakingWalker.setOnGround(false);
            sneakingWalker.setDeltaMovement(0.0D, -0.2D, 0.0D);
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, sneakingWalker)
            );
            helper.assertFalse(sneakingWalker.onGround(),
                    "Mist Form should not hold the player on liquid while sneaking");

            var lavaWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_lava_walk_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), lavaWalker.blockPosition().below(), Blocks.LAVA.defaultBlockState());
            lavaWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            lavaWalker.setDeltaMovement(0.0D, -0.2D, 0.0D);
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, lavaWalker)
            );
            helper.assertTrue(lavaWalker.onGround() && !lavaWalker.isInLava(),
                    "Mist Form should stand on lava by avoiding liquid contact, not by granting fire resistance");

            var flowingWaterWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_flowing_water_walk_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), flowingWaterWalker.blockPosition().below(),
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
            flowingWaterWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            flowingWaterWalker.setDeltaMovement(0.1D, -0.2D, 0.0D);
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, flowingWaterWalker)
            );
            helper.assertTrue(flowingWaterWalker.onGround()
                            && flowingWaterWalker.getDeltaMovement().y == 0.0D
                            && Math.abs(flowingWaterWalker.getDeltaMovement().x - 0.1D) < 1.0E-9D,
                    "Mist Form should stand on flowing liquid without crushing horizontal movement: "
                            + flowingWaterWalker.getDeltaMovement());

            var swimmer = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_swimming_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), swimmer.blockPosition(), Blocks.WATER.defaultBlockState());
            swimmer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            swimmer.setDeltaMovement(0.0D, 0.2D, 0.0D);
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, swimmer)
            );
            helper.assertFalse(swimmer.onGround(),
                    "Mist Form should not force liquid standing while the player is touching liquid");
            helper.assertTrue(Math.abs(swimmer.getDeltaMovement().y - 0.2D) < 1.0E-9D,
                    "Mist Form should preserve upward swimming movement while touching liquid: "
                            + swimmer.getDeltaMovement());

            var cooldownWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_fluid_cooldown_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), cooldownWalker.blockPosition(), Blocks.WATER.defaultBlockState());
            cooldownWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            cooldownWalker.tickCount = 100;
            helper.assertFalse(jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should disable liquid standing immediately after touching liquid");

            helper.getLevel().setBlock(cooldownWalker.blockPosition(), Blocks.AIR.defaultBlockState(), 3);
            placeAbsoluteFluidTestBasin(helper.getLevel(), cooldownWalker.blockPosition().below(), Blocks.WATER.defaultBlockState());
            // ここでは再接触ではなく、20 tick 経過後に液体立ちが再有効化されることだけを検証する。
            var cooldownSafePos = Vec3.atBottomCenterOf(cooldownWalker.blockPosition().above(2));
            cooldownWalker.setPos(cooldownSafePos.x, cooldownSafePos.y, cooldownSafePos.z);
            cooldownWalker.tickCount = 120;
            helper.assertFalse(jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should keep liquid standing disabled for 20 ticks after leaving liquid");
            cooldownWalker.tickCount = 121;
            helper.assertTrue(jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should re-enable liquid standing after the 20 tick liquid-contact delay");
            helper.succeed();
        });
    }

    static void mistFormPassesTaggedBlocksAndRejectsGlass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_collision_normal_test");
            var mistPlayer = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "mist_form_collision_pass_test");
            mistPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            for (var sample : MIST_FORM_PASSABLE_COLLISION_SAMPLES) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, sample.state(), normalPlayer),
                        "Mist Form passable sample should still collide without Mist Form: " + sample.name());
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, sample.state(), mistPlayer),
                        "Mist Form should remove collision from passable sample: " + sample.name());
            }

            helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.GLASS.defaultBlockState(), mistPlayer),
                    "Mist Form should not remove glass collision");
        });
    }

    static void mistFormPassableBlockDenylistBlocksIdsAndTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_collision_deny_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            try (var ignored = ApprenticeCodexServerConfig.useMistFormPassableBlockDenylistOverrideForGameTest(
                    List.of("minecraft:iron_bars")
            )) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.IRON_BARS.defaultBlockState(), player),
                        "Mist Form server denylist should block a configured block ID");
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, Blocks.OAK_LEAVES.defaultBlockState(), player),
                        "Mist Form server denylist should not block unrelated passable blocks");
            }

            try (var ignored = ApprenticeCodexServerConfig.useMistFormPassableBlockDenylistOverrideForGameTest(
                    List.of("#minecraft:leaves")
            )) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.OAK_LEAVES.defaultBlockState(), player),
                        "Mist Form server denylist should block a configured block tag");
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, Blocks.IRON_BARS.defaultBlockState(), player),
                        "Mist Form server denylist should not block unrelated passable IDs");
            }
        });
    }

    static void mistFormWaterloggedPassableBlockDoesNotSnapUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 3, 0), "mist_form_waterlogged_passable_test");
            var waterloggedTrapdoorPos = player.blockPosition();
            helper.getLevel().setBlock(
                    waterloggedTrapdoorPos,
                    Blocks.OAK_TRAPDOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.TrapDoorBlock.WATERLOGGED, true),
                    3
            );
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            player.setDeltaMovement(0.0D, -0.2D, 0.0D);

            var yBeforeTick = player.getY();
            jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player)
            );

            helper.assertTrue(Math.abs(player.getY() - yBeforeTick) < 1.0E-9D,
                    "Mist Form should not snap upward while touching fluid inside a passable waterlogged block");
        });
    }

    static void mistFormIgnoresTaggedMovementRestrictions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mist_form_stuck_normal_test");
            var mistPlayer = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 0),
                    "mist_form_stuck_ignore_test");
            mistPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            for (var sample : MIST_FORM_MOVEMENT_RESTRICTION_SAMPLES) {
                resetPlayerPosition(helper, normalPlayer, new BlockPos(0, 2, 0));
                resetPlayerPosition(helper, mistPlayer, new BlockPos(3, 2, 0));

                var normalMove = moveAfterStuckInBlock(normalPlayer, sample.state(), sample.motionMultiplier());
                var mistMove = moveAfterStuckInBlock(mistPlayer, sample.state(), sample.motionMultiplier());

                helper.assertTrue(normalMove <= sample.motionMultiplier().x + 0.01D,
                        "Movement restriction sample should slow normal player: " + sample.name()
                                + " move=" + normalMove);
                helper.assertTrue(mistMove > 0.99D,
                        "Mist Form should ignore movement restriction sample: " + sample.name()
                                + " move=" + mistMove);
            }
        });
    }

    static void mistFormMovementRestrictionIgnoreKeepsBlockEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var powderPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mist_form_powder_effect_test");
            powderPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            var powderPos = powderPlayer.blockPosition();
            helper.getLevel().setBlock(powderPos, Blocks.POWDER_SNOW.defaultBlockState(), 3);
            Blocks.POWDER_SNOW.defaultBlockState().entityInside(helper.getLevel(), powderPos, powderPlayer);
            helper.assertTrue(powderPlayer.isInPowderSnow,
                    "Mist Form should ignore powder snow movement restriction without removing powder snow state");

            // makeStuckInBlock だけを止めるため、entityInside の後続処理が消えないことを粉雪で代表確認する。
        });
    }

    static void assertMistFormModifierAmount(
            GameTestHelper helper,
            net.minecraft.world.effect.MobEffect effect,
            Attribute attribute,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var modifier = effect.getAttributeModifiers().get(attribute);
        helper.assertTrue(modifier != null, message + ": missing modifier for " + attribute.getDescriptionId());
        if (modifier == null) {
            return;
        }
        helper.assertTrue(modifier.getOperation() == operation,
                message + ": expected operation " + operation + " but got " + modifier.getOperation());
        helper.assertTrue(Math.abs(effect.getAttributeModifierValue(0, modifier) - expectedAmount) < 1.0E-9D,
                message + ": expected level 0 amount " + expectedAmount);
        helper.assertTrue(Math.abs(effect.getAttributeModifierValue(9, modifier) - expectedAmount) < 1.0E-9D,
                message + ": expected level 9 amount to remain " + expectedAmount);
    }

    static boolean isCollisionShapeEmptyForPlayer(GameTestHelper helper, BlockState state, Player player) {
        return state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), CollisionContext.of(player)).isEmpty();
    }

    static final List<MistFormCollisionSample> MIST_FORM_PASSABLE_COLLISION_SAMPLES = List.of(
            new MistFormCollisionSample("fence", Blocks.OAK_FENCE.defaultBlockState()),
            new MistFormCollisionSample("fence_gate", Blocks.OAK_FENCE_GATE.defaultBlockState()),
            new MistFormCollisionSample("door", Blocks.OAK_DOOR.defaultBlockState()),
            new MistFormCollisionSample("iron_bars", Blocks.IRON_BARS.defaultBlockState()),
            new MistFormCollisionSample("trapdoor", Blocks.OAK_TRAPDOOR.defaultBlockState()),
            new MistFormCollisionSample("leaves", Blocks.OAK_LEAVES.defaultBlockState())
    );

    record MistFormCollisionSample(String name, BlockState state) {
    }

    static double moveAfterStuckInBlock(Player player, BlockState state, Vec3 motionMultiplier) {
        player.setDeltaMovement(Vec3.ZERO);
        var beforeX = player.getX();
        player.makeStuckInBlock(state, motionMultiplier);
        player.move(MoverType.SELF, new Vec3(1.0D, 0.0D, 0.0D));
        return player.getX() - beforeX;
    }

    static void resetPlayerPosition(GameTestHelper helper, Player player, BlockPos pos) {
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    static final List<MistFormMovementRestrictionSample> MIST_FORM_MOVEMENT_RESTRICTION_SAMPLES = List.of(
            new MistFormMovementRestrictionSample(
                    "cobweb",
                    Blocks.COBWEB.defaultBlockState(),
                    new Vec3(0.25D, 0.05D, 0.25D)
            ),
            new MistFormMovementRestrictionSample(
                    "powder_snow",
                    Blocks.POWDER_SNOW.defaultBlockState(),
                    new Vec3(0.9D, 1.5D, 0.9D)
            ),
            new MistFormMovementRestrictionSample(
                    "sweet_berry_bush",
                    Blocks.SWEET_BERRY_BUSH.defaultBlockState(),
                    new Vec3(0.8D, 0.75D, 0.8D)
            )
    );

    record MistFormMovementRestrictionSample(String name, BlockState state, Vec3 motionMultiplier) {
    }

    static net.minecraft.world.entity.monster.Zombie createTargetingZombie(
            GameTestHelper helper,
            BlockPos pos,
            Player target,
            String name
    ) {
        var level = helper.getLevel();
        var zombie = new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, level);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        zombie.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        zombie.setCustomName(Component.literal(name));
        zombie.setTarget(target);
        zombie.setLastHurtByMob(target);
        level.addFreshEntity(zombie);
        return zombie;
    }

    static void placeAbsoluteFluidTestBasin(ServerLevel level, BlockPos fluidPos, BlockState fluidState) {
        level.setBlock(fluidPos.below(), Blocks.STONE.defaultBlockState(), 3);
        for (var direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(fluidPos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
        }
        level.setBlock(fluidPos, fluidState, 3);
    }

    static FakePlayer createHarvestMoonPlayer(GameTestHelper helper, BlockPos pos, ItemStack mainHandStack) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "harvest_moon_test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        return player;
    }

    static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static ItemEntity spawnItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    static void placeWaterTestBasin(GameTestHelper helper, BlockPos waterPos) {
        helper.setBlock(waterPos.below(), Blocks.STONE);
        for (var direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(waterPos.relative(direction), Blocks.STONE);
        }
    }

    static ItemEntity spawnNoGravityItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setNoGravity(true);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    static FakePlayer createEquipmentTestPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        return player;
    }

    static void prepareManaMendingProcessTick(MagicData magicData) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(1);
        accessor.apprenticecodex$setCastDuration(200);
        accessor.apprenticecodex$setCastDurationRemaining(190);
        accessor.apprenticecodex$setCastSource(CastSource.SPELLBOOK);
        accessor.apprenticecodex$setCastType(CastType.CONTINUOUS);
    }

    static void setFocusStaffbowArrowCatalyst(FakePlayer player, ItemStack arrowStack) {
        player.getInventory().setItem(1, arrowStack.copy());
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useFocusStaffbowConfigOverrideForGameTest(
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        return ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
                enableContinuousFocusedCast,
                enableManaLoan,
                enableArrowCatalystRequirement,
                List.of("minecraft:arrow"),
                3.0D,
                2.0D,
                20,
                2.0D,
                1.0D,
                pendingMaxLoanManaRatio,
                spellDenylist,
                enableSpellAllowlist,
                spellAllowlist
        );
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks,
            double powerArrowSpellLevelBonusPerLevel
    ) {
        return ApprenticeCodexServerConfig.useElementalBowConfigOverrideForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
    }

    static int getFocusStaffbowArrowCount(Player player) {
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        for (var stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    static void assertManaShieldCharmEquipped(GameTestHelper helper, ServerPlayer player, String context) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Mana Shield Charm " + context + " test"));
        helper.assertTrue(curiosInventory.isEquipped(ItemRegistry.MANA_SHIELD_CHARM.get()),
                "Mana Shield Charm should be recognized as equipped in Curios during " + context + " test");
        helper.assertTrue(curiosInventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()).isPresent(),
                "Mana Shield Charm should be discoverable via findFirstCurio during " + context + " test");
    }

    static net.minecraftforge.event.entity.living.LivingAttackEvent postLivingAttackEventForGameTest(
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        var event = new net.minecraftforge.event.entity.living.LivingAttackEvent(player, source, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    static net.minecraftforge.event.entity.living.LivingFallEvent postLivingFallEventForGameTest(
            ServerPlayer player,
            float distance,
            float damageMultiplier
    ) {
        var event = new net.minecraftforge.event.entity.living.LivingFallEvent(player, distance, damageMultiplier);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    static void equipRingCurio(FakePlayer player, ItemStack ringStack) {
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.RING_SLOT, ringStack);
    }

    static void equipNecklaceCurio(FakePlayer player, ItemStack necklaceStack) {
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, necklaceStack);
    }

    static void tickEquippedAbsorptionAmplifyAmulet(FakePlayer player) {
        var slotResult = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()))
                .orElseThrow(() -> new IllegalStateException("Missing equipped Absorption Amplify Amulet for GameTest"));
        if (slotResult.stack().getItem() instanceof AbsorptionAmplifyAmulet amulet) {
            amulet.curioTick(slotResult.slotContext(), slotResult.stack());
        }
    }

    static FakePlayer createTrackedEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = createEquipmentTestPlayer(helper, pos, profileName);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static ItemStack getEquippedAutocastAmulet(FakePlayer player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof AutocastAmulet).stream()
                        .findFirst()
                        .map(top.theillusivec4.curios.api.SlotResult::stack)
                        .orElseThrow(() -> new IllegalStateException("Missing equipped Autocast Amulet for GameTest")))
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Autocast Amulet GameTest"));
    }

    static ManaShieldCharmState getManaShieldCharmState(Player player) {
        return player.getCapability(Capabilities.SPELL_DATA)
                .map(data -> data.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE))
                .orElseThrow(() -> new IllegalStateException("Missing spell data for Mana Shield Charm GameTest"));
    }

    static MirageAvoidanceState getMirageAvoidanceState(Player player) {
        return player.getCapability(Capabilities.SPELL_DATA)
                .map(data -> data.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE))
                .orElseThrow(() -> new IllegalStateException("Missing spell data for MirageAvoidance GameTest"));
    }

    static RemoteEyeState getRemoteEyeState(Player player) {
        return player.getCapability(Capabilities.SPELL_DATA)
                .map(data -> data.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE))
                .orElseThrow(() -> new IllegalStateException("Missing spell data for RemoteEye GameTest"));
    }

    static AbsorptionAmplifyAmuletState getAbsorptionAmplifyAmuletState(Player player) {
        return player.getCapability(Capabilities.SPELL_DATA)
                .map(data -> data.get(CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE))
                .orElseThrow(() -> new IllegalStateException("Missing spell data for Absorption Amplify Amulet GameTest"));
    }

    static void invokeTouchDigDestroyBlock(TouchDigSpell spell, Level level, BlockPos pos, Player player) {
        try {
            var method = TouchDigSpell.class.getDeclaredMethod("doDestroyBlock", Level.class, BlockPos.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            method.invoke(spell, level, pos, player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Touch Dig destroy helper for GameTest", exception);
        }
    }

    static List<ItemEntity> getFreshItemDrops(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(radius),
                itemEntity -> itemEntity.getAge() <= 1
        );
    }

    static int countFreshItemDrops(ServerLevel level, Item item, BlockPos pos, double radius) {
        return getFreshItemDrops(level, pos, radius).stream()
                .filter(itemEntity -> itemEntity.getItem().is(item))
                .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                .sum();
    }

    static boolean hasItemEntityWithin(ServerLevel level, Item item, Vec3 pos, double radius) {
        return !level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos, pos).inflate(radius),
                itemEntity -> !itemEntity.isRemoved() && itemEntity.getItem().is(item)
        ).isEmpty();
    }

    static FakePlayer createAssistWingsPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static int getAssistWingsDoneJump(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return -1;
        }
        return spellData.get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE).doneJump;
    }

    static void setAssistWingsState(Player player, int doneJump, int localEntityId) {
        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, state -> {
            state.doneJump = doneJump;
            state.localEntityId = localEntityId;
        }));
    }

    static void assertAssistWingsCastDataRoundTrips(GameTestHelper helper, MagicData magicData, boolean expectedJumpApplied) {
        if (!(magicData.getAdditionalCastData() instanceof AssistWings.AssistWingsCastData source)) {
            helper.fail("Assist Wings should store serializable cast data for client movement");
            return;
        }
        helper.assertTrue(source.jumpApplied() == expectedJumpApplied,
                "Assist Wings cast data should report whether the server applied the jump");

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        source.writeToBuffer(buffer);
        var restoredFromBuffer = new AssistWings.AssistWingsCastData();
        restoredFromBuffer.readFromBuffer(buffer);
        helper.assertTrue(restoredFromBuffer.jumpApplied() == expectedJumpApplied,
                "Assist Wings cast data should preserve the jump result through network serialization");

        var restoredFromNbt = new AssistWings.AssistWingsCastData();
        restoredFromNbt.deserializeNBT(source.serializeNBT());
        helper.assertTrue(restoredFromNbt.jumpApplied() == expectedJumpApplied,
                "Assist Wings cast data should preserve the jump result through NBT serialization");
    }

    static int countActiveAssistWingsWings(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(
                AssistWingsWingEntity.class,
                new AABB(player.position(), player.position()).inflate(16.0D),
                wing -> !wing.isRemoved()
                        && wing.getOwner() != null
                        && wing.getOwner().getUUID().equals(player.getUUID())
        ).size();
    }

    static FakePlayer createExtractPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static FakePlayer createBetterCombatHiddenOffhandPlayer(
            GameTestHelper helper,
            ItemStack mainHandStack,
            ItemStack offhandStack,
            String profileName
    ) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 0)));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());

        // Better Combat に空扱いされても inventory.offhand[0] 自体は保持されるので、
        // 救済系テストは実スロットへ直接積んで隠蔽前提を再現する。
        player.getInventory().offhand.set(0, offhandStack.copy());
        return player;
    }

    static void castHarvestMoon(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.HARVEST_MOON.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static void castEarthForge(GameTestHelper helper, FakePlayer player, BlockPos centerPos, Direction effectDirection, int radius) {
        var spell = (EarthForge) SpellRegistry.EARTH_FORGE.get();
        var absoluteCenterPos = helper.absolutePos(centerPos);
        var tag = new CompoundTag();
        tag.putInt("CenterX", absoluteCenterPos.getX());
        tag.putInt("CenterY", absoluteCenterPos.getY());
        tag.putInt("CenterZ", absoluteCenterPos.getZ());
        tag.putInt("EffectDirection", effectDirection.get3DDataValue());
        tag.putInt("Radius", radius);

        var castData = new EarthForge.EarthForgeCastData();
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
    }

    static void castLinearBuild(GameTestHelper helper, FakePlayer player, BlockPos targetPos, Direction hitFace) {
        var spell = (LinearBuild) SpellRegistry.LINEAR_BUILD.get();
        var absoluteTargetPos = helper.absolutePos(targetPos);
        var targetData = new BlockTargetData();
        targetData.setTarget(
                absoluteTargetPos,
                hitFace,
                Vec3.atCenterOf(absoluteTargetPos),
                absoluteTargetPos.relative(hitFace),
                hitFace.getOpposite()
        );
        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Linear Build test could not resolve player magic data");
        helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                "Linear Build pre-cast check should accept the prepared block target");
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
        spell.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);
    }

    static FakePlayer createSenseEvilPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static FakePlayer createSenseEvilPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        level.addFreshEntity(player);
        return player;
    }

    static net.minecraft.world.entity.LivingEntity spawnPositionedZombie(ServerLevel level, Vec3 targetCenter) {
        forceLoadChunk(level, BlockPos.containing(targetCenter));
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create zombie for SenseEvil GameTest");
        }
        // SenseEvil の到達距離検証では移動や落下で座標がぶれると誤検知しやすいため固定する。
        zombie.setNoAi(true);
        zombie.moveTo(targetCenter.x, targetCenter.y - zombie.getBbHeight() * 0.5, targetCenter.z, 0.0f, 0.0f);
        level.addFreshEntity(zombie);
        return zombie;
    }

    static void placeZombieSpawner(ServerLevel level, BlockPos pos) {
        forceLoadChunk(level, pos);
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
            throw new IllegalStateException("Failed to place spawner for SenseEvil GameTest at " + pos);
        }
        spawner.setEntityId(EntityType.ZOMBIE, level.getRandom());
        spawner.setChanged();
    }

    static void forceLoadChunk(ServerLevel level, BlockPos pos) {
        var chunkX = SectionPos.blockToSectionCoord(pos.getX());
        var chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        level.setChunkForced(chunkX, chunkZ, true);
        level.getChunk(chunkX, chunkZ);
    }

    static double getSenseEvilRange(SenseEvil spell, net.minecraft.world.entity.LivingEntity caster, int spellLevel) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("getRange", int.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            return (double) method.invoke(spell, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read SenseEvil range for GameTest", exception);
        }
    }

    @SuppressWarnings("unchecked")
    static List<SenseEvilHighlightsPacket.TargetData> collectSenseEvilHighlights(
            SenseEvil spell,
            ServerLevel level,
            int spellLevel,
            net.minecraft.world.entity.LivingEntity caster
    ) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("collectHighlights", ServerLevel.class, int.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            return (List<SenseEvilHighlightsPacket.TargetData>) method.invoke(spell, level, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to collect SenseEvil highlights for GameTest", exception);
        }
    }

    static void assertSenseEvilHighlightPresent(
            GameTestHelper helper,
            List<SenseEvilHighlightsPacket.TargetData> highlights,
            Vec3 expectedPosition,
            double tolerance,
            String message
    ) {
        var found = highlights.stream()
                .anyMatch(target -> target.position().distanceTo(expectedPosition) <= tolerance);
        helper.assertTrue(found, message + " / expected near " + expectedPosition + " but got " + highlights);
    }

    static FakePlayer createHealingBloomPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static void castHealingBloom(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos, boolean forceReplace) {
        var spell = (HealingBloom) SpellRegistry.HEALING_BLOOM.get();
        var castData = new HealingBloom.HealingBloomCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        tag.putBoolean("ForceReplace", forceReplace);
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    static java.util.List<HealingBloomEntity> getOwnedHealingBlooms(GameTestHelper helper, FakePlayer owner) {
        var blooms = new java.util.ArrayList<HealingBloomEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof HealingBloomEntity bloom
                    && bloom.isAlive()
                    && owner.getUUID().equals(bloom.getOwnerUuid())) {
                blooms.add(bloom);
            }
        }
        return blooms;
    }

    static HealingBloomEntity getSingleLivingHealingBloom(GameTestHelper helper, FakePlayer owner) {
        var blooms = getOwnedHealingBlooms(helper, owner);
        helper.assertTrue(blooms.size() == 1, "Expected exactly one living Healing Bloom but found " + blooms.size());
        return blooms.get(0);
    }

    static FakePlayer createArcherMultiplePlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // SummonManager は owner を level lookup で引き直して recast cleanup するため、
        // Archer Multiple の summon 消滅テストでは FakePlayer もワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    static void castArcherMultiple(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static List<ArcherMultipleBowEntity> getOwnedArcherMultipleBows(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                ArcherMultipleBowEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                bow -> {
                    var summonOwner = bow.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    static <T extends jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity> List<T> getOwnedSummonWeapons(
            GameTestHelper helper,
            FakePlayer owner,
            Class<T> weaponType
    ) {
        return helper.getLevel().getEntitiesOfClass(
                weaponType,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                weapon -> {
                    var summonOwner = weapon.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    static FakePlayer createCompanionTrunkPlayer(GameTestHelper helper, BlockPos pos) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "companion_trunk_test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static void prepareWideSearchIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    static void prepareMiningSpellIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    static void prepareSummonedEntityIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    static void prepareHighIsolationPlatform(GameTestHelper helper, BlockPos centerPos) {
        // basic_floor は 5x3x5 と小さく、batch 近接配置の地形へ探索やレイが吸われやすい。
        // 足場を高所へ自前で作って、各テストが自分の 5x5 領域だけを参照するように固定する。
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                helper.setBlock(new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z), Blocks.STONE);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z), Blocks.AIR);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z), Blocks.AIR);
            }
        }
    }

    static BlockPos createRemoteIsolationOrigin(GameTestHelper helper, BlockPos relativePos, int xOffset, int zOffset) {
        return helper.absolutePos(relativePos).offset(xOffset, 0, zOffset);
    }

    static void prepareAbsoluteIsolationPlatform(ServerLevel level, BlockPos centerPos) {
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                var floorPos = new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z);
                var lowerAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z);
                var upperAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z);
                forceLoadChunk(level, floorPos);
                level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(lowerAirPos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(upperAirPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    static void prepareAbsoluteIsolationTargetPlatform(ServerLevel level, Vec3 targetCenter) {
        var floorPos = BlockPos.containing(targetCenter.x, targetCenter.y - 1.0D, targetCenter.z);
        forceLoadChunk(level, floorPos);
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floorPos.above(2), Blocks.AIR.defaultBlockState(), 3);
    }

    static FakePlayer createPersonalShelfPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // owner lookup と openers の close 対象探索が server 側の player list / level lookup を使うため、
        // Personal Shelf の GameTest では FakePlayer もワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static BlockPos castPersonalShelf(GameTestHelper helper, FakePlayer player, BlockPos shelfPos, boolean exportMode, Direction exportFacing) {
        var spell = (PersonalShelf) SpellRegistry.PERSONAL_SHELF.get();
        var castData = new PersonalShelf.PersonalShelfCastData();
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteShelfPos.getX());
        tag.putInt("PositionY", absoluteShelfPos.getY());
        tag.putInt("PositionZ", absoluteShelfPos.getZ());
        tag.putBoolean("ExportMode", exportMode);
        tag.putInt("ExportFacing", exportFacing.get3DDataValue());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
        return absoluteShelfPos;
    }

    static PersonalShelfChestBlockEntity getPersonalShelfBlockEntity(GameTestHelper helper, BlockPos absoluteShelfPos) {
        var blockEntity = helper.getLevel().getBlockEntity(absoluteShelfPos);
        helper.assertTrue(blockEntity instanceof PersonalShelfChestBlockEntity,
                "Expected Personal Shelf block entity but found " + blockEntity);
        return (PersonalShelfChestBlockEntity) blockEntity;
    }

    static void castCompanionTrunk(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.COMPANION_TRUNK.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity getSingleExtractProjectile(
            GameTestHelper helper,
            FakePlayer owner
    ) {
        var projectiles = helper.getLevel().getEntitiesOfClass(
                jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                projectile -> projectile.getOwner() == owner
        );
        helper.assertTrue(projectiles.size() == 1,
                "Expected exactly one Extract projectile but found " + projectiles.size());
        return projectiles.get(0);
    }

    static CompanionTrunkEntity createCompanionTrunk(GameTestHelper helper, FakePlayer owner, BlockPos pos) {
        var trunk = new CompanionTrunkEntity(EntityRegistry.COMPANION_TRUNK.get(), helper.getLevel(), owner);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        trunk.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(trunk);
        return trunk;
    }

    static String describeCompanionTrunkMovement(CompanionTrunkEntity trunk) {
        return "blockPos=" + trunk.blockPosition()
                + ", position=" + trunk.position()
                + ", delta=" + trunk.getDeltaMovement()
                + ", onGround=" + trunk.onGround()
                + ", tickCount=" + trunk.tickCount;
    }

    static CompanionTrunkEntity getSingleCompanionTrunk(GameTestHelper helper, FakePlayer owner) {
        var trunks = helper.getLevel().getEntitiesOfClass(
                CompanionTrunkEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                trunk -> owner.getUUID().equals(trunk.getOwnerUuid())
        );
        helper.assertTrue(trunks.size() == 1, "Expected exactly one Companion Trunk but found " + trunks.size());
        return trunks.get(0);
    }

    static BlockPos findCompanionTrunkChest(GameTestHelper helper, BlockPos center) {
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    var candidate = center.offset(x, y, z);
                    if (helper.getBlockState(candidate).is(Blocks.CHEST)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    static boolean containsItem(ChestBlockEntity chest, Item item, int count) {
        for (var slot = 0; slot < chest.getContainerSize(); ++slot) {
            var stack = chest.getItem(slot);
            if (stack.is(item) && stack.getCount() == count) {
                return true;
            }
        }
        return false;
    }

    static int countMatureHarvestMoonPlants(GameTestHelper helper, List<BlockPos> cropPositions) {
        var count = 0;
        for (var pos : cropPositions) {
            var state = helper.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                ++count;
                continue;
            }
            if (state.is(Blocks.NETHER_WART)
                    && state.hasProperty(NetherWartBlock.AGE)
                    && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
                ++count;
            }
        }
        return count;
    }

    static Block requireForgeBlock(GameTestHelper helper, ResourceLocation id) {
        var block = ForgeRegistries.BLOCKS.getValue(id);
        helper.assertTrue(block != null, "Missing required block for GameTest: " + id);
        return block;
    }

    static Item requireForgeItem(GameTestHelper helper, ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        helper.assertTrue(item != null, "Missing required item for GameTest: " + id);
        return item;
    }

    static BlockState withIntegerProperty(GameTestHelper helper, BlockState state, String propertyName, int value) {
        var property = findIntegerProperty(helper, state, propertyName);
        helper.assertTrue(property.getPossibleValues().contains(value),
                "Property " + propertyName + " does not accept " + value + " on " + state);
        return state.setValue(property, value);
    }

    static int getIntegerPropertyValue(GameTestHelper helper, BlockState state, String propertyName) {
        return state.getValue(findIntegerProperty(helper, state, propertyName));
    }

    static BlockState withBooleanProperty(GameTestHelper helper, BlockState state, String propertyName, boolean value) {
        return state.setValue(findBooleanProperty(helper, state, propertyName), value);
    }

    static boolean getBooleanPropertyValue(GameTestHelper helper, BlockState state, String propertyName) {
        return state.getValue(findBooleanProperty(helper, state, propertyName));
    }

    static IntegerProperty findIntegerProperty(GameTestHelper helper, BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && integerProperty.getName().equals(propertyName)) {
                return integerProperty;
            }
        }
        helper.fail("Missing integer property " + propertyName + " on " + state);
        throw new IllegalStateException("Unreachable after helper.fail");
    }

    static BooleanProperty findBooleanProperty(GameTestHelper helper, BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && booleanProperty.getName().equals(propertyName)) {
                return booleanProperty;
            }
        }
        helper.fail("Missing boolean property " + propertyName + " on " + state);
        throw new IllegalStateException("Unreachable after helper.fail");
    }

    static boolean isApprenticeSpell(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        return spellId != null && ApprenticeCodex.MODID.equals(spellId.getNamespace());
    }

    static void assertChargedTwinBladeStaffThrownDamage(
            GameTestHelper helper,
            ItemStack stack,
            MobType mobType,
            double expectedDamage,
            String failureMessage
    ) {
        var actualDamage = ChargedTwinBladeStaff.resolveThrownDamage(stack, mobType);
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-9D,
                failureMessage + ": mobType=" + mobType + ", expected=" + expectedDamage + ", actual=" + actualDamage);
    }

    static Set<ResourceLocation> expectedRandomBookLootEnchantments() {
        return registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER
        );
    }

    static float getEquippedAttributeTotal(Player player, Attribute attribute) {
        var total = 0.0F;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            total += (float) stack.getAttributeModifiers(slot).get(attribute).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
        }
        return total;
    }

    static void equipProtectionIvIronArmor(ServerPlayer player) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            player.setItemSlot(slot, armorStack);
        }
    }

    static float findDamageForArmorReducedTarget(float armor, float toughness, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterAbsorb(high, armor, toughness) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterAbsorb(mid, armor, toughness);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    static float findDamageForMagicReducedTarget(int protection, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterMagicAbsorb(high, protection) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(mid, protection);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    static float resolveExpectedBarrierManaAfterHitForGameTest(float incomingDamage, float availableMana) {
        var remainingDamage = incomingDamage;
        var remainingMana = availableMana;
        var manaPerDamage = ApprenticeCodexServerConfig.manaShieldCharmManaPerDamage();

        if (manaPerDamage <= 0.0F) {
            return remainingMana;
        }

        while (remainingDamage >= 1.0F) {
            if (remainingMana >= manaPerDamage) {
                remainingDamage -= 1.0F;
                remainingMana -= manaPerDamage;
                continue;
            }
            if (remainingMana > 0.0F) {
                remainingDamage -= 1.0F;
                remainingMana = 0.0F;
            }
            break;
        }

        return Math.max(remainingMana, 0.0F);
    }

    static float resolveExpectedSynchronizationManaAfterHitForGameTest(
            float incomingDamage,
            float availableMana,
            int protection
    ) {
        var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(incomingDamage, protection);
        var remainingMitigatedDamage = Math.max(incomingDamage - reducedDamage, 0.0F);
        var remainingMana = availableMana;
        var synchronizationManaPerDamage = ApprenticeCodexServerConfig.manaShieldCharmSynchronizationManaPerDamage();

        if (synchronizationManaPerDamage > 0.0F) {
            while (remainingMitigatedDamage >= 1.0F && remainingMana >= synchronizationManaPerDamage) {
                remainingMitigatedDamage -= 1.0F;
                remainingMana -= synchronizationManaPerDamage;
            }
        } else {
            remainingMitigatedDamage = 0.0F;
        }

        if (remainingMitigatedDamage >= 1.0F) {
            return 0.0F;
        }

        return resolveExpectedBarrierManaAfterHitForGameTest(reducedDamage, remainingMana);
    }

    static int countWholeDamageStepsForGameTest(float damage) {
        var remainingDamage = damage;
        var count = 0;
        while (remainingDamage >= 1.0F) {
            remainingDamage -= 1.0F;
            ++count;
        }
        return count;
    }

    static void assertClose(
            GameTestHelper helper,
            double actual,
            double expected,
            double tolerance,
            String failureMessage
    ) {
        helper.assertTrue(Math.abs(actual - expected) <= tolerance,
                failureMessage + ": expected=" + expected + ", actual=" + actual);
    }

    static Set<ResourceLocation> registryIdSet(RegistryObject<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            var id = enchantment.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = stack.getTag();
        var actualShotMode = tag != null && tag.contains("ElementalBowShotMode")
                ? tag.getString("ElementalBowShotMode")
                : null;
        ResourceLocation actualSelectionId = null;
        if (tag != null) {
            if ("magic".equals(actualShotMode) && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            } else if (tag.contains("ElementalBowAmmoSelection")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowAmmoSelection"));
            } else if (actualShotMode == null && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            }
        }
        helper.assertTrue(
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, expectedSelectionId),
                message + ": expected shotMode=" + expectedShotMode + ", selection=" + expectedSelectionId
                        + " but got shotMode=" + actualShotMode + ", selection=" + actualSelectionId
        );
    }

    static String describeElementalBowSelectionView(ElementalBow.ModeSelectionView view) {
        return view.selection().selectionId() == null
                ? view.selection().shotMode()
                : view.selection().shotMode() + ":" + view.selection().selectionId();
    }

    @Nullable
    static ElementalBow.ModeSelectionView findElementalBowSelectionView(ServerPlayer player, ItemStack stack,
                                                                                String shotMode, @Nullable ResourceLocation selectionId) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        var tag = stack.getOrCreateTag();
        tag.putString("ElementalBowShotMode", shotMode);
        if ("magic".equals(shotMode)) {
            if (selectionId != null) {
                tag.putString("ElementalBowMode", selectionId.toString());
            }
            tag.remove("ElementalBowAmmoSelection");
            return;
        }

        if (selectionId != null) {
            tag.putString("ElementalBowAmmoSelection", selectionId.toString());
        } else {
            tag.remove("ElementalBowAmmoSelection");
        }
        tag.remove("ElementalBowMode");
    }

    static void assertTranslatableKey(GameTestHelper helper, Component component, String expectedKey, String message) {
        var contents = component.getContents();
        helper.assertTrue(contents instanceof TranslatableContents,
                message + " (component was not translatable: " + component + ")");
        if (contents instanceof TranslatableContents translatableContents) {
            helper.assertTrue(expectedKey.equals(translatableContents.getKey()),
                    message + " (expected=" + expectedKey + ", actual=" + translatableContents.getKey() + ")");
        }
    }

    static void assertStorageStabilizerDisplayName(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            String message
    ) {
        var displayName = stack.getHoverName();
        helper.assertTrue(displayName.getContents() instanceof TranslatableContents,
                message + " (display name was not translatable: " + displayName + ")");
        if (!(displayName.getContents() instanceof TranslatableContents contents)) {
            return;
        }

        helper.assertTrue("item.apprenticecodex.storage_stabilizer.with_spell".equals(contents.getKey()),
                message + " (unexpected translation key: " + contents.getKey() + ")");
        var args = contents.getArgs();
        helper.assertTrue(args.length == 2,
                message + " (unexpected argument count: " + args.length + ")");
        if (args.length != 2 || !(args[0] instanceof Component baseName) || !(args[1] instanceof Component spellName)) {
            return;
        }

        assertTranslatableKey(helper, baseName, "item.apprenticecodex.storage_stabilizer",
                message + " (unexpected base item name)");
        helper.assertTrue(expectedSpell.getDisplayName(null).getString().equals(spellName.getString()),
                message + " (unexpected spell name: " + spellName.getString() + ")");
    }

    static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        helper.assertTrue(tooltipLines.size() > index,
                message + " (tooltip line count=" + tooltipLines.size() + ")");
        assertTranslatableKey(helper, tooltipLines.get(index), expectedKey, message);
    }

    static void assertTooltipKeyUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            ChatFormatting expectedColor,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var matchingLine = tooltipLines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()))
                .findFirst();
        helper.assertTrue(matchingLine.isPresent(),
                message + " (missing tooltip key=" + expectedKey + ")");
        if (matchingLine.isPresent()) {
            var expectedTextColor = TextColor.fromLegacyFormat(expectedColor);
            helper.assertTrue(Objects.equals(expectedTextColor, matchingLine.get().getStyle().getColor()),
                    message + " (expected=" + expectedTextColor + ", actual="
                            + matchingLine.get().getStyle().getColor() + ")");
        }
    }

    static void assertTooltipKeyArgumentUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            int argumentIndex,
            @Nullable TextColor expectedColor,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var matchingLine = tooltipLines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()))
                .findFirst();
        helper.assertTrue(matchingLine.isPresent(),
                message + " (missing tooltip key=" + expectedKey + ")");
        if (matchingLine.isEmpty()) {
            return;
        }

        var contents = (TranslatableContents) matchingLine.get().getContents();
        var args = contents.getArgs();
        helper.assertTrue(args.length > argumentIndex,
                message + " (argument count=" + args.length + ")");
        if (args.length <= argumentIndex) {
            return;
        }

        helper.assertTrue(args[argumentIndex] instanceof Component,
                message + " (argument was not a component: " + args[argumentIndex] + ")");
        if (args[argumentIndex] instanceof Component component) {
            helper.assertTrue(Objects.equals(expectedColor, component.getStyle().getColor()),
                    message + " (expected=" + expectedColor + ", actual="
                            + component.getStyle().getColor() + ")");
        }
    }

    static void assertTooltipKeyAbsent(GameTestHelper helper, ItemStack stack, String key, String message) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }

    static ItemStack createEnchantedBook(EnchantmentInstance... enchantments) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        for (var enchantment : enchantments) {
            EnchantedBookItem.addEnchantment(book, enchantment);
        }
        return book;
    }

    static String describeEnchantmentDifference(
            Set<ResourceLocation> expectedEnchantments,
            Set<ResourceLocation> actualEnchantments
    ) {
        var missingEnchantments = new LinkedHashSet<>(expectedEnchantments);
        missingEnchantments.removeAll(actualEnchantments);

        var unexpectedEnchantments = new LinkedHashSet<>(actualEnchantments);
        unexpectedEnchantments.removeAll(expectedEnchantments);

        return "missing=" + missingEnchantments + ", unexpected=" + unexpectedEnchantments;
    }

    static void assertSwingcastStaffTier(
            GameTestHelper helper,
            AbstractSwingcastStaffItem item,
            Set<SpellGunCastType> expectedCastTypes,
            SwingcastCooldownMode expectedCooldownMode,
            AbstractSpell instantSpell,
            AbstractSpell longSpell,
            AbstractSpell continuousSpell,
            String itemName
    ) {
        var tier = item.getSwingcastStaffTier();
        helper.assertTrue(tier.supportedCastTypes().equals(expectedCastTypes),
                itemName + " cast type regression: expected " + expectedCastTypes + " but got " + tier.supportedCastTypes());
        helper.assertTrue(tier.swingcastCooldownMode() == expectedCooldownMode,
                itemName + " cooldown mode regression: expected " + expectedCooldownMode + " but got " + tier.swingcastCooldownMode());

        var allowsLong = expectedCastTypes.contains(SpellGunCastType.LONG);
        helper.assertTrue(item.canImbueSpell(instantSpell, 1),
                itemName + " should allow instant spell imbuing");
        helper.assertTrue(item.canImbueSpell(longSpell, 1) == allowsLong,
                itemName + " long spell imbue regression: expected " + allowsLong);
        helper.assertFalse(item.canImbueSpell(continuousSpell, 1),
                itemName + " should continue rejecting continuous spells");
    }

    static void assertSpellgunCooldownAdjustment(
            GameTestHelper helper,
            ServerPlayer player,
            ItemStack stack,
            AbstractSpell spell,
            int originalCooldownTicks,
            int expectedCooldownTicks,
            String message
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellgun cooldown test could not resolve player mana data");
        magicData.setPlayerCastingItem(stack);

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                originalCooldownTicks,
                spell,
                player,
                CastSource.SWORD
        );
        SpellGunCastEvent.onSpellCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldownTicks,
                message + ": expected " + expectedCooldownTicks + " but got " + cooldownEvent.getEffectiveCooldown());
    }

    static void assertModifierAmount(
            GameTestHelper helper,
            AbstractOffhandMagicItem item,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = sumModifierAmount(
                item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack).get(attribute),
                operation
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack)));
    }

    static void assertCurioModifierAmount(
            GameTestHelper helper,
            top.theillusivec4.curios.api.type.capability.ICurioItem item,
            top.theillusivec4.curios.api.SlotContext slotContext,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
        var actualAmount = sumModifierAmount(modifiers.get(attribute), operation);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(modifiers));
    }

    static void assertSpellStainedRunicTabletSchoolPower(
            GameTestHelper helper,
            SpellStainedRunicTablet item,
            top.theillusivec4.curios.api.SlotContext slotContext,
            ItemStack stack,
            AbstractSpell spell,
            double expectedAmount
    ) {
        assertSpellStainedRunicTabletSchoolPower(
                helper,
                item,
                slotContext,
                stack,
                spell,
                expectedAmount,
                "Spell-stained Runic Tablet school spell power mismatch for " + spell.getSpellResource()
        );
    }

    static void assertSpellStainedRunicTabletSchoolPower(
            GameTestHelper helper,
            SpellStainedRunicTablet item,
            top.theillusivec4.curios.api.SlotContext slotContext,
            ItemStack stack,
            AbstractSpell spell,
            double expectedAmount,
            String message
    ) {
        var attribute = MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(attribute != null, "Could not resolve school spell power attribute for " + spell.getSpellResource());
        assertCurioModifierAmount(
                helper,
                item,
                slotContext,
                stack,
                attribute,
                expectedAmount,
                AttributeModifier.Operation.MULTIPLY_BASE,
                message
        );
    }

    static ItemStack createSpellStainedRunicTabletStack(GameTestHelper helper, SpellEntry... entries) {
        var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                "Spell-stained Runic Tablet did not initialize a spell container");

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Spell-stained Runic Tablet spell container is null");
        helper.assertTrue(spellContainer.getMaxSpellCount() >= entries.length,
                "Spell-stained Runic Tablet test needs " + entries.length + " slots but got "
                        + spellContainer.getMaxSpellCount());

        var mutable = spellContainer.mutableCopy();
        for (var index = 0; index < entries.length; ++index) {
            var entry = entries[index];
            var level = entry.spell().getMinLevelForRarity(entry.rarity());
            helper.assertTrue(level > 0,
                    "Cannot prepare " + entry.rarity() + " rarity for " + entry.spell().getSpellResource());
            helper.assertTrue(mutable.addSpellAtIndex(entry.spell(), level, index, false),
                    "Failed to add Spell-stained Runic Tablet test spell " + entry.spell().getSpellResource()
                            + " at index " + index);
        }
        ISpellContainer.set(stack, mutable.toImmutable());

        var preparedContainer = ISpellContainer.get(stack);
        helper.assertTrue(preparedContainer != null, "Prepared Spell-stained Runic Tablet spell container is null");
        for (var index = 0; index < entries.length; ++index) {
            var spellData = preparedContainer.getSpellAtIndex(index);
            var expected = entries[index];
            helper.assertTrue(spellData.getSpell() == expected.spell(),
                    "Prepared Spell-stained Runic Tablet spell mismatch at index " + index
                            + ": expected " + expected.spell().getSpellResource()
                            + " but got " + spellData.getSpell().getSpellResource() + " " + spellData.getRarity());
        }

        return stack;
    }

    static ItemStack createElementMaidenRobeSchoolPowerSpellbook(GameTestHelper helper, AbstractSpell... spells) {
        var item = io.redspace.ironsspellbooks.registries.ItemRegistry.DIAMOND_SPELL_BOOK.get();
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Element Maiden Robe test spellbook did not initialize a spell container");
        helper.assertTrue(spellContainer.getMaxSpellCount() >= spells.length,
                "Element Maiden Robe test spellbook needs " + spells.length + " slots but got "
                        + spellContainer.getMaxSpellCount());

        var mutable = spellContainer.mutableCopy();
        for (var index = 0; index < spells.length; ++index) {
            helper.assertTrue(mutable.addSpellAtIndex(spells[index], 1, index, false),
                    "Failed to add Element Maiden Robe test spell " + spells[index].getSpellResource()
                            + " at index " + index);
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

    static void assertElementMaidenSchoolPowerBonusAmount(
            GameTestHelper helper,
            Map<Attribute, Double> bonuses,
            Attribute attribute,
            double expectedAmount,
            String message
    ) {
        var actualAmount = bonuses.getOrDefault(attribute, 0.0D);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " attribute=" + ForgeRegistries.ATTRIBUTES.getKey(attribute)
                        + " bonuses=" + bonuses);
    }

    static void assertElementMaidenDynamicSchoolPowerBonuses(
            GameTestHelper helper,
            Player player,
            Map<Attribute, Double> expectedBonuses,
            String message
    ) {
        if (expectedBonuses.isEmpty()) {
            assertAllElementMaidenRobeStacksHaveNoStoredSchoolPower(helper, player, message);
            return;
        }

        var targetArmors = findElementMaidenDynamicBonusTargets(player);
        helper.assertTrue(!targetArmors.isEmpty(), message + ": missing Element Maiden Robe bonus target armor");
        var totalAmounts = new LinkedHashMap<Attribute, Double>();
        for (var targetArmor : targetArmors) {
            var stack = targetArmor.stack();
            var storedBonuses = ElementMaidenRobeItem.getSpellbookSchoolPowerBonuses(stack);
            for (var entry : expectedBonuses.entrySet()) {
                assertElementMaidenSchoolPowerBonusAmount(helper, storedBonuses, entry.getKey(), entry.getValue(),
                        message + " on " + targetArmor.slot());
            }

            var item = (ElementMaidenRobeItem) stack.getItem();
            var modifiers = item.getAttributeModifiers(targetArmor.slot(), stack);
            for (var entry : expectedBonuses.entrySet()) {
                var actualAmount = sumModifierAmount(
                        modifiers.get(entry.getKey()),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(actualAmount - entry.getValue()) < 1.0e-9D,
                        message + ": expected armor attribute " + entry.getValue() + " but got " + actualAmount
                                + " on " + targetArmor.slot()
                                + " attribute=" + ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())
                                + " modifiers=" + describeModifiers(modifiers));
                totalAmounts.merge(entry.getKey(), actualAmount, Double::sum);
            }

            for (var entry : storedBonuses.entrySet()) {
                helper.assertTrue(expectedBonuses.containsKey(entry.getKey()),
                        message + ": unexpected stored bonus on " + targetArmor.slot() + " "
                                + ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())
                                + " amount=" + entry.getValue());
            }
        }

        for (var entry : expectedBonuses.entrySet()) {
            var expectedTotal = entry.getValue() * targetArmors.size();
            var actualTotal = totalAmounts.getOrDefault(entry.getKey(), 0.0D);
            helper.assertTrue(Math.abs(actualTotal - expectedTotal) < 1.0e-9D,
                    message + ": expected stacked armor attribute " + expectedTotal + " but got " + actualTotal
                            + " attribute=" + ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())
                            + " equipped robes=" + targetArmors.size());
        }
    }

    static void assertNoElementMaidenDynamicSchoolPower(
            GameTestHelper helper,
            Player player,
            String message
    ) {
        assertElementMaidenDynamicSchoolPowerBonuses(helper, player, Map.of(), message);
    }

    static List<EquippedArmorStack> findElementMaidenDynamicBonusTargets(Player player) {
        var result = new java.util.ArrayList<EquippedArmorStack>();
        for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ElementMaidenRobeItem) {
                result.add(new EquippedArmorStack(slot, stack));
            }
        }
        return result;
    }

    static void assertAllElementMaidenRobeStacksHaveNoStoredSchoolPower(
            GameTestHelper helper,
            Player player,
            String message
    ) {
        assertOtherElementMaidenRobeStacksHaveNoStoredSchoolPower(helper, player, ItemStack.EMPTY, message);
    }

    static void assertOtherElementMaidenRobeStacksHaveNoStoredSchoolPower(
            GameTestHelper helper,
            Player player,
            ItemStack excludedStack,
            String message
    ) {
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || stack == excludedStack || !(stack.getItem() instanceof ElementMaidenRobeItem)) {
                continue;
            }

            var storedBonuses = ElementMaidenRobeItem.getSpellbookSchoolPowerBonuses(stack);
            helper.assertTrue(storedBonuses.isEmpty(),
                    message + ": unexpected stored school spell power on " + slot + " " + storedBonuses);
        }
    }

    record EquippedArmorStack(EquipmentSlot slot, ItemStack stack) {
    }

    static ExpectedSpellStainedRunicTabletAttributes resolveExpectedSpellStainedRunicTabletAttributes(
            GameTestHelper helper,
            ItemStack stack
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing Spell-stained Runic Tablet spell container for expected attributes");
        var values = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();
        double maxMana = 0.0D;
        double generalSpellPower = 0.0D;
        var schoolSpellPower = new LinkedHashMap<Attribute, Double>();
        var schoolSpellCounts = new LinkedHashMap<String, Integer>();

        for (var spellSlot : spellContainer.getActiveSpells()) {
            var spellData = spellSlot.spellData();
            var rarity = spellData.getRarity();
            var schoolType = spellData.getSpell().getSchoolType();
            maxMana += values.maxMana().forRarity(rarity);
            generalSpellPower += values.generalSpellPower().forRarity(rarity);
            schoolSpellCounts.merge(schoolType.getId().toString(), 1, Integer::sum);

            var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
            if (schoolAttribute != null) {
                schoolSpellPower.merge(schoolAttribute, values.schoolSpellPower().forRarity(rarity), Double::sum);
            }
        }

        var topSchoolCount = schoolSpellCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        return new ExpectedSpellStainedRunicTabletAttributes(
                maxMana,
                generalSpellPower,
                filterExpectedSpellStainedRunicTabletSchoolPower(schoolSpellPower, values),
                values.cooldownReduction().resolve(schoolSpellCounts.size()),
                values.castTimeReduction().resolve(topSchoolCount)
        );
    }

    static Map<Attribute, Double> filterExpectedSpellStainedRunicTabletSchoolPower(
            Map<Attribute, Double> schoolSpellPower,
            SpellStainedRunicTabletServerConfig.Values values
    ) {
        var filtered = new LinkedHashMap<Attribute, Double>();
        for (var entry : schoolSpellPower.entrySet()) {
            if (shouldExpectSpellStainedRunicTabletSchoolPower(entry.getValue(), values)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    static boolean shouldExpectSpellStainedRunicTabletSchoolPower(
            double amount,
            SpellStainedRunicTabletServerConfig.Values values
    ) {
        if (amount > 0.0D) {
            return amount >= values.minimumAppliedPositiveBonus();
        }
        if (amount < 0.0D) {
            return Math.abs(amount) >= values.minimumAppliedNegativePenalty();
        }
        return false;
    }

    static top.theillusivec4.curios.api.SlotContext createSpellbookSlotContext(GameTestHelper helper) {
        return new top.theillusivec4.curios.api.SlotContext(
                io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0)),
                0,
                false,
                true
        );
    }

    static SpellEntry spellEntry(AbstractSpell spell, SpellRarity rarity) {
        return new SpellEntry(spell, rarity);
    }

    static SpellStainedRunicTabletServerConfig.RarityBonuses sameRarityBonuses(double value) {
        return new SpellStainedRunicTabletServerConfig.RarityBonuses(value, value, value, value, value, value);
    }

    record SpellEntry(AbstractSpell spell, SpellRarity rarity) {
    }

    record ExpectedSpellStainedRunicTabletAttributes(
            double maxMana,
            double generalSpellPower,
            Map<Attribute, Double> schoolSpellPower,
            double cooldownReduction,
            double castTimeReduction
    ) {
    }

    static void assertScrollcasterGauntletSpellPower(
            GameTestHelper helper,
            ItemStack stack,
            double expectedGlobalSpellPower,
            double expectedFireSpellPower,
            double expectedIceSpellPower,
            String message
    ) {
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var globalSpellPower = sumModifierAmount(
                modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        var fireSpellPower = sumModifierAmount(
                modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        var iceSpellPower = sumModifierAmount(
                modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        helper.assertTrue(Math.abs(globalSpellPower - expectedGlobalSpellPower) < 1.0e-9D
                        && Math.abs(fireSpellPower - expectedFireSpellPower) < 1.0e-9D
                        && Math.abs(iceSpellPower - expectedIceSpellPower) < 1.0e-9D,
                message
                        + ": expected global/fire/ice="
                        + expectedGlobalSpellPower + "/" + expectedFireSpellPower + "/" + expectedIceSpellPower
                        + " but got "
                        + globalSpellPower + "/" + fireSpellPower + "/" + iceSpellPower
                        + " modifiers=" + describeModifiers(modifiers));
    }

    static void assertCastingMoveSpeedAdjustment(
            GameTestHelper helper,
            double externalBonus,
            double expectedAmount,
            String message
    ) {
        var actualAmount = CastingMoveSpeedAdjustment.computeAvailableBonus(externalBonus);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount + " for external bonus " + externalBonus);
    }

    static void assertCastingMoveSpeedModifierAmount(
            GameTestHelper helper,
            net.minecraft.world.entity.ai.attributes.AttributeInstance attributeInstance,
            @org.jetbrains.annotations.Nullable UUID excludedModifierId,
            double expectedAmount,
            String message
    ) {
        var actualAmount = attributeInstance.getModifiers().stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .filter(modifier -> excludedModifierId == null || !excludedModifierId.equals(modifier.getId()))
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + attributeInstance.getModifiers());
    }

    static void assertUpgradeable(GameTestHelper helper, ItemStack stack, String message) {
        helper.assertTrue(stack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                message + " (missing upgrade whitelist tag on " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
        helper.assertTrue(Utils.canBeUpgraded(stack),
                message + " (Utils.canBeUpgraded returned false for " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
    }

    static UpgradeData createUpgradeData(
            RegistryAccess registryAccess,
            ItemStack stack,
            net.minecraft.resources.ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> upgradeKey,
            String slotName
    ) {
        io.redspace.ironsspellbooks.api.backwards_compat.UpgradeTypeCache.doCache(registryAccess);
        var upgradeRegistry = registryAccess.registryOrThrow(io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY);
        var upgradeHolder = upgradeRegistry.getHolder(upgradeKey)
                .orElseThrow(() -> new IllegalStateException("Missing upgrade orb type: " + upgradeKey.location()));
        var upgradeData = new UpgradeData(java.util.Map.of(upgradeHolder, 1), slotName);
        UpgradeData.set(stack, upgradeData);
        return upgradeData;
    }

    @Nullable
    static net.minecraft.resources.ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> findUpgradeKeyForPowerAttribute(
            Attribute spellPowerAttribute
    ) {
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.FIRE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ICE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.HOLY_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ENDER_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ENDER_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.EVOCATION_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.NATURE_SPELL_POWER;
        }
        return null;
    }

    static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    static void assertSingleModifierAmount(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifiers = modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                message + ": expected exactly one " + operation + " modifier but got " + matchingModifiers);
        var actualAmount = matchingModifiers.get(0).getAmount();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    static void assertModifierWithId(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            UUID expectedId,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifier = modifiers.stream()
                .filter(modifier -> expectedId.equals(modifier.getId()))
                .findFirst();
        helper.assertTrue(matchingModifier.isPresent(),
                message + ": missing modifier " + expectedId + " in " + modifiers);
        var modifier = matchingModifier.get();
        helper.assertTrue(modifier.getOperation() == operation
                        && Math.abs(modifier.getAmount() - expectedAmount) < 1.0e-9D,
                message + ": expected " + operation + " " + expectedAmount + " but got " + modifier);
    }

    static void postSpellOnCast(ServerPlayer player, AbstractSpell spell, int spellLevel) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getManaCost(spellLevel),
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        ));
    }

    static void assertSchoolSpellPowerBonus(
            GameTestHelper helper,
            ItemStack stack,
            EquipmentSlot slot,
            AbstractSpell spell,
            double expectedAmount,
            String message
    ) {
        var attribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(attribute != null, "Could not resolve school spell power attribute for " + spell.getSpellId());
        var actualAmount = sumModifierAmount(
                stack.getAttributeModifiers(slot).get(attribute),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected=" + expectedAmount
                        + ", actual=" + actualAmount
                        + ", modifiers=" + describeModifiers(stack.getAttributeModifiers(slot)));
    }

    static String describeModifiers(com.google.common.collect.Multimap<Attribute, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()) + "="
                        + entry.getValue().getAmount() + "@" + entry.getValue().getOperation())
                .collect(Collectors.joining(", "));
    }

    static void placeAndAssertBlockEntity(
            GameTestHelper helper,
            BlockPos pos,
            net.minecraft.world.level.block.Block block,
            net.minecraft.world.level.block.entity.BlockEntityType<?> expectedType
    ) {
        helper.setBlock(pos, block);
        helper.assertBlockPresent(block, pos);

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity != null, "Missing block entity for " + BuiltInRegistries.BLOCK.getKey(block));
        helper.assertTrue(blockEntity.getType() == expectedType,
                "Block entity type mismatch for " + BuiltInRegistries.BLOCK.getKey(block) + ": " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    static void assertArcanumInAJarComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedParameterCount,
            int remainingOperationCount,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ARCANUM_IN_A_JAR.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof ArcanumInAJarBlockEntity,
                "Arcanum in a Jar block entity was not created");

        var tag = new CompoundTag();
        tag.putInt("StoredParameterCount", storedParameterCount);
        tag.putInt("RemainingOperationCount", remainingOperationCount);
        blockEntity.load(tag);

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Arcanum in a Jar should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Arcanum in a Jar comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    static void assertAtelierStationComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedFluidAmount,
            boolean insertInventoryFlask,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ATELIER_STATION.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof AtelierStationBlockEntity,
                "Atelier Station block entity was not created");

        if (blockEntity instanceof AtelierStationBlockEntity atelierStation) {
            if (storedFluidAmount > 0) {
                var tag = new CompoundTag();
                var storedFluidList = new ListTag();
                var storedFluidTag = new CompoundTag();
                var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION),
                        net.minecraft.world.item.alchemy.Potions.REGENERATION);
                storedFluidTag.put("Item", storedPotion.save(new CompoundTag()));
                storedFluidTag.putInt("Amount", storedFluidAmount);
                storedFluidList.add(storedFluidTag);
                tag.put("StoredFluids", storedFluidList);
                blockEntity.load(tag);
            }

            if (insertInventoryFlask) {
                var flask = createFilledSpellcastersFlask(
                        PotionUtils.setPotion(new ItemStack(Items.POTION),
                                net.minecraft.world.item.alchemy.Potions.REGENERATION),
                        1,
                        0
                );
                atelierStation.getFlaskInventory().setStackInSlot(0, flask);
            }
        }

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Atelier Station should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Atelier Station comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    static void assertRecipeLoaded(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            net.minecraft.world.item.crafting.RecipeSerializer<?> expectedSerializer,
            net.minecraft.world.item.crafting.RecipeType<?> expectedType
    ) {
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe != null, "Missing recipe: " + recipeId);
        helper.assertTrue(recipe.getSerializer() == expectedSerializer,
                "Recipe serializer mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        if (expectedType != null) {
            helper.assertTrue(recipe.getType() == expectedType,
                    "Recipe type mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()));
        }
    }

    static void assertRecipeLoadedWithSerializerId(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            ResourceLocation expectedSerializerId
    ) {
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe != null, "Missing recipe: " + recipeId);
        var actualSerializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        helper.assertTrue(expectedSerializerId.equals(actualSerializerId),
                "Recipe serializer mismatch for " + recipeId + ": expected " + expectedSerializerId + " but got " + actualSerializerId);
    }

    static void assertShapedRecipeCenterIngredientMatches(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            ItemStack acceptedStack,
            ItemStack rejectedStack,
            String message
    ) {
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe,
                "Expected shaped recipe for " + recipeId);
        if (!(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shapedRecipe)) {
            return;
        }

        var ingredients = shapedRecipe.getIngredients();
        helper.assertTrue(ingredients.size() > 4, "Recipe center ingredient missing for " + recipeId);
        if (ingredients.size() <= 4) {
            return;
        }

        var centerIngredient = ingredients.get(4);
        helper.assertTrue(centerIngredient.test(acceptedStack),
                message + ": accepted stack did not match");
        helper.assertFalse(centerIngredient.test(rejectedStack),
                message + ": rejected stack unexpectedly matched");
    }

    static void assertSearchBeaconTarget(GameTestHelper helper, Item item, String expectedTarget) {
        var definition = SearchBeaconTargetManager.getDefinition(new ItemStack(item));
        helper.assertTrue(definition != null, "SearchBeacon target missing for " + BuiltInRegistries.ITEM.getKey(item));
        helper.assertTrue(
                definition != null
                        && definition.targets().contains(new SearchBeaconTargetList.TargetReference(false, ResourceLocation.parse(expectedTarget))),
                "SearchBeacon target mismatch for " + BuiltInRegistries.ITEM.getKey(item)
        );
    }

    static void assertVillageHousePoolContains(
            GameTestHelper helper,
            Registry<StructureTemplatePool> templatePoolRegistry,
            ResourceLocation poolId,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId,
            int expectedWeight
    ) {
        var pool = templatePoolRegistry.get(poolId);
        helper.assertTrue(pool != null, "Missing village house pool: " + poolId);

        var rawTemplates = ((StructureTemplatePoolAccessor) pool).apprenticecodex$getRawTemplates();
        var matchingRawEntries = rawTemplates.stream()
                .filter(pair -> isMatchingSinglePoolElement(pair.getFirst(), expectedStructureId, expectedProcessorId))
                .toList();

        helper.assertTrue(matchingRawEntries.size() == 1,
                "Expected exactly one Errand Mage entry in " + poolId + " but found " + matchingRawEntries.size());
        helper.assertTrue(
                !matchingRawEntries.isEmpty() && matchingRawEntries.get(0).getSecond() == expectedWeight,
                "Unexpected Errand Mage weight in " + poolId + ": " + (matchingRawEntries.isEmpty() ? "missing" : matchingRawEntries.get(0).getSecond())
        );

        long expandedMatchCount = pool.getShuffledTemplates(RandomSource.create(0L)).stream()
                .filter(element -> isMatchingSinglePoolElement(element, expectedStructureId, expectedProcessorId))
                .count();
        helper.assertTrue(expandedMatchCount == expectedWeight,
                "Expanded template count mismatch in " + poolId + ": " + expandedMatchCount);
    }

    static boolean isMatchingSinglePoolElement(
            StructurePoolElement element,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId
    ) {
        if (!(element instanceof SinglePoolElement singlePoolElement)) {
            return false;
        }

        var accessor = (SinglePoolElementAccessor) singlePoolElement;
        var structureId = accessor.apprenticecodex$getTemplate().left().orElse(null);
        var processorId = accessor.apprenticecodex$getProcessors().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return expectedStructureId.equals(structureId) && expectedProcessorId.equals(processorId);
    }

    static void assertVillageHouseTemplateLoadsWithRequiredBlocks(
            GameTestHelper helper,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager,
            ResourceLocation structureId,
            ResourceLocation expectedVillagerPool,
            BlockPos expectedLootChestPos
    ) {
        var template = structureTemplateManager.get(structureId).orElse(null);
        helper.assertTrue(template != null, "Missing village house template: " + structureId);

        var jigsawBlocks = template != null
                ? template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true)
                : List.<StructureTemplate.StructureBlockInfo>of();
        helper.assertTrue(jigsawBlocks.size() == 2,
                "Unexpected jigsaw count for " + structureId + ": " + jigsawBlocks.size());

        boolean hasBottom = false;
        boolean hasEntrance = false;
        for (var jigsawBlock : jigsawBlocks) {
            var nbt = jigsawBlock.nbt();
            helper.assertTrue(nbt != null,
                    "Village house jigsaw is missing NBT: " + structureId + " at " + jigsawBlock.pos());
            if (nbt == null) {
                continue;
            }

            var name = ResourceLocation.tryParse(nbt.getString("name"));
            var target = ResourceLocation.tryParse(nbt.getString("target"));
            var pool = ResourceLocation.tryParse(nbt.getString("pool"));
            if (ResourceLocation.withDefaultNamespace("bottom").equals(name)
                    && ResourceLocation.withDefaultNamespace("bottom").equals(target)
                    && expectedVillagerPool.equals(pool)) {
                hasBottom = true;
            }
            if (ResourceLocation.withDefaultNamespace("building_entrance").equals(name)
                    && ResourceLocation.withDefaultNamespace("building_entrance").equals(target)
                    && ResourceLocation.withDefaultNamespace("empty").equals(pool)) {
                hasEntrance = true;
            }
        }

        helper.assertTrue(hasBottom, "Village house is missing villager spawn jigsaw: " + structureId);
        helper.assertTrue(hasEntrance, "Village house is missing building entrance jigsaw: " + structureId);

        var lootChests = template != null
                ? template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST, true)
                : List.<StructureTemplate.StructureBlockInfo>of();
        helper.assertTrue(lootChests.size() == 1,
                "Unexpected loot chest count for " + structureId + ": " + lootChests.size());
        if (lootChests.size() != 1) {
            return;
        }

        var lootChest = lootChests.get(0);
        helper.assertTrue(expectedLootChestPos.equals(lootChest.pos()),
                "Unexpected loot chest position for " + structureId + ": " + lootChest.pos());
        var chestNbt = lootChest.nbt();
        helper.assertTrue(chestNbt != null,
                "Village house loot chest is missing NBT: " + structureId + " at " + lootChest.pos());
        if (chestNbt != null) {
            helper.assertTrue("minecraft:chest".equals(chestNbt.getString("id")),
                    "Village house loot chest has an unexpected block entity id: " + structureId);
            helper.assertTrue("apprenticecodex:chests/errand_mage_house".equals(chestNbt.getString("LootTable")),
                    "Village house loot chest has an unexpected loot table: " + structureId);
        }
    }

    static LootParams createChestLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 1, 0)))
                .create(LootContextParamSets.CHEST);
    }

    static void assertErrandMageHouseLootTableExists(GameTestHelper helper) {
        var lootTableId = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "chests/errand_mage_house"
        );
        var lootTable = helper.getLevel().getServer().getLootData().getLootTable(lootTableId);
        helper.assertTrue(lootTable != LootTable.EMPTY,
                "Errand Mage house loot table is missing: " + lootTableId);
    }

    static LootParams createEmptyLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
    }

    static LootContext createEmptyLootContext(GameTestHelper helper, long seed) {
        return new LootContext.Builder(createEmptyLootParams(helper))
                .withOptionalRandomSeed(seed)
                .create(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/random_applicable_enchantment"));
    }

    static List<AbstractSpell> getNonLootableApprenticeSpells() {
        return List.of(
                SpellRegistry.EXTRACT.get(),
                SpellRegistry.UNITE_LUNA.get(),
                SpellRegistry.ILLUMINATE_STELLAR.get(),
                SpellRegistry.MANIFESTATION_GRIMOIRE.get(),
                SpellRegistry.MANA_SLASH.get()
        );
    }

    static void assertLootTableGeneratesAllItems(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var remainingItems = expectedItems.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> remainingItems.remove(stack.getItem()));

        helper.assertTrue(remainingItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate expected items within " + attempts + " attempts: "
                        + remainingItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    static void assertLootTableGeneratesAnyItem(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var seenItems = new LinkedHashSet<Item>();
        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            if (expectedItems.contains(stack.getItem())) {
                seenItems.add(stack.getItem());
            }
        });

        helper.assertTrue(!seenItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate any of the expected items within " + attempts + " attempts: "
                        + expectedItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    static void assertLootTableNeverGeneratesBlockedSpells(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<AbstractSpell> blockedSpells
    ) {
        var blockedSpellIds = blockedSpells.stream()
                .map(AbstractSpell::getSpellResource)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var seenBlockedSpells = new LinkedHashSet<ResourceLocation>();
        var scrollSampleCount = new AtomicInteger();

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            var spell = getScrollSpell(stack);
            if (spell == null) {
                return;
            }

            scrollSampleCount.incrementAndGet();
            var spellId = spell.getSpellResource();
            if (spellId != null && blockedSpellIds.contains(spellId)) {
                seenBlockedSpells.add(spellId);
            }
        });

        helper.assertTrue(scrollSampleCount.get() > 0,
                "Loot table " + lootTableId + " did not generate any spell scrolls within " + attempts + " attempts");
        helper.assertTrue(seenBlockedSpells.isEmpty(),
                "Loot table " + lootTableId + " generated blocked apprentice scrolls: " + seenBlockedSpells);
    }

    static void sampleLootTable(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            java.util.function.Consumer<ItemStack> stackConsumer
    ) {
        var lootTable = helper.getLevel().getServer().getLootData().getLootTable(lootTableId);
        for (var i = 0; i < attempts; i++) {
            lootTable.getRandomItems(lootParams, stackConsumer);
        }
    }

    static ExplorersCodexGuidebookTransferRecipe getExplorersCodexGuidebookTransferRecipe(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"))
                .orElse(null);
        helper.assertTrue(recipe instanceof ExplorersCodexGuidebookTransferRecipe,
                "Missing Explorer's Codex guidebook transfer recipe: " + recipe);
        return (ExplorersCodexGuidebookTransferRecipe) recipe;
    }

    static ItemStack createSpellScroll(AbstractSpell spell) {
        return createSpellScroll(spell, 1);
    }

    static ItemStack createSpellScroll(AbstractSpell spell, int level) {
        var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }

    static void assertScrollcasterGauntletOffhandUseCasts(
            GameTestHelper helper,
            ItemStack mainHandStack,
            AbstractSpell spell,
            String profileName
    ) {
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Scrollcaster Gauntlet offhand use test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "Scrollcaster Gauntlet offhand use should cast through the offhand when main hand does not consume use but got "
                        + result.getResult());
        helper.assertTrue(magicData.isCasting(), "Scrollcaster Gauntlet offhand use should start casting");
        helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(magicData.getCastingEquipmentSlot()),
                "Scrollcaster Gauntlet offhand use should mark the offhand casting slot but got "
                        + magicData.getCastingEquipmentSlot());
        helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                "Scrollcaster Gauntlet offhand use should cast with the offhand gauntlet stack");
    }

    static void assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
            GameTestHelper helper,
            ItemStack mainHandStack,
            AbstractSpell gauntletSpell,
            String profileName
    ) {
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(gauntletSpell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Scrollcaster Gauntlet offhand defer test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Scrollcaster Gauntlet offhand use should defer when main hand is a spell item but got "
                        + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet offhand use should not start a second cast while main hand owns spell use");
        helper.assertTrue(magicData.getPlayerCastingItem().isEmpty(),
                "Scrollcaster Gauntlet offhand defer should not set a casting item");
    }

    static void assertArchivistsGrimoireInscribeHintTooltip(
            GameTestHelper helper,
            ItemStack stack,
            boolean expected,
            String message
    ) {
        assertTooltipTranslationKey(helper, stack, "item.apprenticecodex.special_spellbook.inscribe_hint", expected, message);
    }

    static void assertTooltipTranslationKey(
            GameTestHelper helper,
            ItemStack stack,
            String translationKey,
            boolean expected,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var hasKey = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents translatableContents
                        && translationKey.equals(translatableContents.getKey()));
        helper.assertTrue(hasKey == expected, message);
    }

    static void assertScrollSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            String message
    ) {
        helper.assertTrue(stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                message + " (stack is not a scroll: " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + " (scroll spell container is null)");
        assertSpellData(helper, spellContainer.getSpellAtIndex(0), expectedSpell, 1, message);
    }

    static @Nullable AbstractSpell getScrollSpell(ItemStack stack) {
        if (!stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        return spellData.getSpell();
    }

    static ItemStack createInstantManaPotion(net.minecraft.world.item.alchemy.Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    static FluidStack createIronsManaPotionFluid(net.minecraft.world.item.alchemy.Potion potion, int amountMb) {
        var fluid = io.redspace.ironsspellbooks.fluids.PotionFluid.from(PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
        fluid.setAmount(amountMb);
        return fluid;
    }

    static FluidStack createCreateManaPotionFluid(net.minecraft.world.item.alchemy.Potion potion, int amountMb) {
        var createPotion = ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("create", "potion"));
        if (createPotion == null) {
            return FluidStack.EMPTY;
        }

        var fluid = new FluidStack(createPotion, amountMb);
        var tag = fluid.getOrCreateTag();
        tag.putString("Potion", BuiltInRegistries.POTION.getKey(potion).toString());
        tag.putString("Bottle", "REGULAR");
        return fluid;
    }

    static void assertPotionEffect(
            GameTestHelper helper,
            net.minecraft.world.item.alchemy.Potion potion,
            String expectedPotionId,
            int expectedDuration,
            int expectedAmplifier
    ) {
        var potionId = ResourceLocation.parse(expectedPotionId);
        helper.assertTrue(BuiltInRegistries.POTION.get(potionId) == potion,
                "Missing potion registry entry: " + potionId);

        var effects = potion.getEffects();
        helper.assertTrue(effects.size() == 1,
                "Potion " + potionId + " should have exactly one effect but got " + effects.size());

        var effect = effects.isEmpty() ? null : effects.get(0);
        helper.assertTrue(effect != null && effect.getEffect() == EffectRegistry.MANA_REGENERATION.get(),
                "Potion " + potionId + " should grant mana regeneration");
        helper.assertTrue(effect != null && effect.getDuration() == expectedDuration,
                "Potion " + potionId + " duration regression: "
                        + (effect == null ? "missing" : effect.getDuration()));
        helper.assertTrue(effect != null && effect.getAmplifier() == expectedAmplifier,
                "Potion " + potionId + " amplifier regression: "
                        + (effect == null ? "missing" : effect.getAmplifier()));
    }

    static ItemStack createFilledSpellcastersFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    static ItemStack createFilledAlchemistsFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    static ItemStack createInitializedPresetStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
        return stack;
    }

    static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithSingleInput(Player player, ItemStack stack) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        return menu;
    }

    static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithInputs(Player player, ItemStack first, ItemStack second) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        return menu;
    }

    static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithInputs(Player player, ItemStack first, ItemStack second,
                                                                                     ItemStack third) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        menu.getSlot(2).set(third);
        return menu;
    }

    static SpellCalibrationBenchMenu createSpellCalibrationBenchMenuWithTarget(Player player, ItemStack stack) {
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
        return menu;
    }

    static SpellCalibrationBenchMenu createSpellCalibrationBenchMenu(GameTestHelper helper, Player player,
                                                                             BlockPos pos) {
        helper.setBlock(pos, BlockRegistry.SPELL_CALIBRATION_BENCH.get());
        return new SpellCalibrationBenchMenu(
                0,
                player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos))
        );
    }

    static void assertStackHasSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": missing spell container");
        if (spellContainer == null) {
            return;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            var spellData = spellContainer.getSpellAtIndex(index);
            if (spellData != SpellData.EMPTY
                    && spellData.getSpell() == expectedSpell
                    && spellData.getLevel() == expectedLevel) {
                return;
            }
        }

        helper.assertTrue(false, message + ": expected spell was not found");
    }

    static void applyRestrictedImbueNormalization(
            GameTestHelper helper,
            ItemStack stack,
            RestrictedSpellImbuableItem item,
            AbstractSpell spell,
            int spellLevel
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before restricted imbue normalization test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before restricted imbue normalization test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare unlocked spell data before restricted imbue normalization test");
        ISpellContainer.set(stack, mutable.toImmutable());
        item.normalizeImbuedSpellContainer(stack);
    }

    static void setSingleUnlockedSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before Focus Staffbow spell setup");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before Focus Staffbow spell setup");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare Focus Staffbow spell setup");
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    static void applyPresetSpellExtraction(GameTestHelper helper, ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before preset extraction test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before preset extraction test");
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.rememberCleared(stack);
    }

    static void applyLegacyLockedReplacement(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before legacy replacement test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before legacy replacement test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, true),
                "Failed to prepare legacy locked replacement spell");
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.clearRememberedState(stack);
    }

    static ItemStack roundTripItemStack(ItemStack stack) {
        return ItemStack.of(stack.save(new CompoundTag()));
    }

    static void repairPresetSpellContainerStateIfNeeded(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            spellGunItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractRightClickMagicWeaponItem magicWeaponItem) {
            magicWeaponItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbueShieldItem imbueShieldItem) {
            imbueShieldItem.repairPresetSpellContainerStateIfNeeded(stack);
        }
    }

    static void assertClearedSpellContainer(GameTestHelper helper, ItemStack stack, String message) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": spell container is null");
        helper.assertTrue(spellContainer.getActiveSpellCount() <= 0,
                message + ": expected no active spells but got " + spellContainer.getActiveSpellCount());
        helper.assertTrue(spellContainer.getSpellAtIndex(0) == SpellData.EMPTY,
                message + ": slot 0 unexpectedly contains " + spellContainer.getSpellAtIndex(0).getSpell().getSpellResource());
    }

    static CraftingContainer createCraftingContainer(ItemStack... stacks) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };

        var container = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < stacks.length; ++i) {
            container.setItem(i, stacks[i]);
        }
        return container;
    }

    static void fillSpellContainerToActiveCount(
            GameTestHelper helper,
            io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable mutable,
            int targetActiveCount
    ) {
        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            if (mutable.getActiveSpellCount() >= targetActiveCount) {
                break;
            }
            if (mutable.getIndexForSpell(spell) >= 0) {
                continue;
            }

            helper.assertTrue(mutable.addSpell(spell, 1, false),
                    "Failed to prepare overflow test spell: " + spell.getSpellResource());
        }

        helper.assertTrue(mutable.getActiveSpellCount() == targetActiveCount,
                "Failed to prepare overflow Explorer's Codex: expected " + targetActiveCount + " active spells but got "
                        + mutable.getActiveSpellCount());
    }

    static ItemStack createAutocastAmuletStack(GameTestHelper helper, int spellSlotCount, SpellData... spells) {
        var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
        var stack = item.getDefaultInstance();
        for (var slot = 0; slot < spellSlotCount - AutocastAmulet.MIN_SPELL_SLOTS; ++slot) {
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    slot,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
        }

        helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                "Autocast Amulet setup should not expose Iron's SpellContainer");
        helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == spellSlotCount,
                "Failed to prepare Autocast Amulet with " + spellSlotCount + " enabled spell slots");

        for (var slot = 0; slot < spells.length; ++slot) {
            var spellData = spells[slot];
            helper.assertTrue(item.canImbueSpell(spellData),
                    "Autocast Amulet rejected setup spell " + spellData.getSpell().getSpellResource());
            AutocastAmulet.setCalibrationScroll(stack, slot, createSpellScroll(spellData.getSpell(), spellData.getLevel()));
        }

        return stack;
    }

    static List<AutoMagnetFamiliarEntity> getOwnedAutoMagnetFamiliars(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                AutoMagnetFamiliarEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                familiar -> {
                    var summonOwner = familiar.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    static boolean invokeAutocastBeginCast(
            ServerPlayer player,
            MagicData magicData,
            ItemStack stack,
            SpellData spellData,
            int spellLevel,
            String castingSlot,
            int scaledManaCost
    ) {
        try {
            var method = AutocastAmuletAutoCastEvent.class.getDeclaredMethod(
                    "beginAutoCast",
                    ServerPlayer.class,
                    MagicData.class,
                    ItemStack.class,
                    SpellData.class,
                    int.class,
                    String.class,
                    int.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(null, player, magicData, stack, spellData, spellLevel, castingSlot, scaledManaCost);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Autocast Amulet auto-cast helper for GameTest", exception);
        }
    }

    static void runAutocastAmuletServerTick(FakePlayer player, int tickCount) {
        player.tickCount = tickCount;
        AutocastAmuletAutoCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onPlayerTick(
                new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)
        );
    }

    static void assertSpellData(
            GameTestHelper helper,
            SpellData spellData,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell data is empty)");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
    }

    static void assertSpellData(
            GameTestHelper helper,
            ISpellContainer spellContainer,
            int index,
            AbstractSpell expectedSpell,
            int expectedLevel,
            boolean expectedLocked,
            String message
    ) {
        var spellData = spellContainer.getSpellAtIndex(index);
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell slot is empty at index " + index + ")");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
        helper.assertTrue(spellData.isLocked() == expectedLocked,
                message + " (locked mismatch: " + spellData.isLocked() + ")");
    }

    static Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>> createEmptyVillagerTrades() {
        var trades = new Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>>();
        for (var level = 1; level <= 5; level++) {
            trades.put(level, new ArrayList<>());
        }
        return trades;
    }

    static List<MerchantOffer> createOffers(List<VillagerTrades.ItemListing> listings, long randomSeed) {
        var random = RandomSource.create(randomSeed);
        var offers = new ArrayList<MerchantOffer>();
        for (var listing : listings) {
            var offer = listing.getOffer(null, random);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    static void assertContainsOffer(GameTestHelper helper, List<MerchantOffer> offers,
                                            ItemStack costA, ItemStack costB, ItemStack result,
                                            int maxUses, String message) {
        helper.assertTrue(offers.stream().anyMatch(offer -> offerMatches(offer, costA, costB, result, maxUses)), message);
    }

    static boolean offerMatches(MerchantOffer offer, ItemStack costA, ItemStack costB, ItemStack result, int maxUses) {
        return stackMatches(offer.getBaseCostA(), costA)
                && stackMatches(offer.getCostB(), costB)
                && stackMatches(offer.getResult(), result)
                && offer.getMaxUses() == maxUses;
    }

    static boolean stackMatches(ItemStack actual, ItemStack expected) {
        if (expected.isEmpty()) {
            return actual.isEmpty();
        }

        return actual.is(expected.getItem()) && actual.getCount() == expected.getCount();
    }

    static boolean hasBaseCostItem(List<MerchantOffer> offers, Item item) {
        return offers.stream().anyMatch(offer -> offer.getBaseCostA().is(item));
    }

    static int countBaseCostItems(List<MerchantOffer> offers, Item... items) {
        var count = 0;
        for (var offer : offers) {
            for (var item : items) {
                if (offer.getBaseCostA().is(item)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    static <T> void assertForgeRegistryEntries(
            GameTestHelper helper,
            String registryName,
            IForgeRegistry<T> registry,
            Collection<? extends RegistryObject<? extends T>> entries
    ) {
        for (var entry : entries) {
            var id = entry.getId();
            helper.assertTrue(id != null, "Missing " + registryName + " id");
            helper.assertTrue(registry.getValue(id) == entry.get(),
                    "Missing " + registryName + " registry entry: " + id);
        }
    }

    static <T> void assertBuiltinRegistryEntries(
            GameTestHelper helper,
            String registryName,
            Registry<T> registry,
            Collection<? extends RegistryObject<? extends T>> entries
    ) {
        for (var entry : entries) {
            var id = entry.getId();
            helper.assertTrue(id != null, "Missing " + registryName + " id");
            helper.assertTrue(registry.get(id) == entry.get(),
                    "Missing " + registryName + " registry entry: " + id);
        }
    }
}
