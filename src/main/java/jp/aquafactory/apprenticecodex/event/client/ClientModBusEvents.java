package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.antimanaarrow.AntiManaArrowRenderer;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerScreen;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationScreen;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerParticlePaletteCache;
import jp.aquafactory.apprenticecodex.block.magneticstabilityanchor.MagneticStabilityAnchorBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchScreen;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserScreen;
import jp.aquafactory.apprenticecodex.compat.arsnouveau.ArsNouveauLuminousDeviceCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.compat.patchouli.PatchouliBuiltinTemplateSupport;
import jp.aquafactory.apprenticecodex.compat.sodiumdynamiclights.SodiumDynamicLightsLuminousDeviceCompat;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.apprenticedesk.PartiallyUsedInkState;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.item.magicitem.client.WoodenWandClientRenderState;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticle;
import jp.aquafactory.apprenticecodex.particle.AdditiveRhombusParticle;
import jp.aquafactory.apprenticecodex.particle.ImpactTremorBlockParticle;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireInscriptionScreen;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireScreen;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseScreen;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouchTooltip;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceTooltip;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticle;
import jp.aquafactory.apprenticecodex.particle.ReticleDotParticle;
import jp.aquafactory.apprenticecodex.particle.SmashcastDustPillarParticle;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.renderer.ManaForceBladeSheathLayer;
import jp.aquafactory.apprenticecodex.renderer.curio.AshenCircletCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.BroomCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.CircletCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.MagiCompressorGadgetCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.ManaThrusterCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.SpellcasterAmmoPouchCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.curio.SpellcasterQuiverCurioRenderer;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteManager;
import jp.aquafactory.apprenticecodex.renderer.entity.FloatmountBroomRenderer;
import jp.aquafactory.apprenticecodex.renderer.entity.HoverrideBroomRenderer;
import jp.aquafactory.apprenticecodex.renderer.tooltip.CalibrationAdjustmentClientTooltipComponent;
import jp.aquafactory.apprenticecodex.renderer.tooltip.SpellcasterAmmoPouchClientTooltipComponent;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerRenderer;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamRenderer;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowRenderer;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashLauncherRenderer;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellRenderer;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingRenderer;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarRenderer;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretRenderer;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemyShotgunRenderer;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunRenderer;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleRenderer;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkRenderer;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileRenderer;
import jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownRenderer;
import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.SpellThrowableCardRenderer;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsCoreRenderer;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsWingRenderer;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatSmgRenderer;
import jp.aquafactory.apprenticecodex.spell.combustionjet.CombustionJetWaveRenderer;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushWingRenderer;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherRenderer;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffRenderer;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffRenderer;
import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRuneTrapBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinKatanaRenderer;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinSlashProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudRenderer;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelRenderer;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomRenderer;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistRenderer;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaRenderer;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarRenderer;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerRenderer;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleRenderer;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.WizardlampLanternBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileRenderer;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeRenderer;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutRenderer;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaRenderer;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldShieldRenderer;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockRenderer;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamRenderer;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxWeaponryRenderer;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeRenderer;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunRenderer;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconRenderer;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltRenderer;
import jp.aquafactory.apprenticecodex.spell.silentassassin.SilentAssassinRifleRenderer;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaRenderer;
import jp.aquafactory.apprenticecodex.spell.spectralwing.SpectralWingLayer;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcessThrowerRenderer;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackSawRenderer;
import jp.aquafactory.apprenticecodex.spell.tirovolley.TiroVolleyMusketRenderer;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemRenderer;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonRenderer;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillRenderer;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorRenderer;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorRenderer;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskScreen;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.concurrent.CompletableFuture;

public final class ClientModBusEvents {
    private ClientModBusEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientModBusEvents::onClientSetup);
        modEventBus.addListener(ClientModBusEvents::onReloadListeners);
        modEventBus.addListener(ClientModBusEvents::registerParticleProviders);
        modEventBus.addListener(ClientModBusEvents::registerRenderers);
        modEventBus.addListener(ClientModBusEvents::addLayers);
        modEventBus.addListener(ClientModBusEvents::registerAdditionalModels);
        modEventBus.addListener(ClientModBusEvents::registerTooltipComponentFactories);
        modEventBus.addListener(ClientModBusEvents::registerItemColors);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("patchouli")) {
            event.enqueueWork(PatchouliBuiltinTemplateSupport::registerBuiltinTemplates);
        }
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.APPRENTICE_DESK.get(), ApprenticeDeskScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.SPELLCASTER_WORKBENCH.get(), SpellcasterWorkbenchScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.SPELL_CALIBRATION_BENCH.get(), SpellCalibrationBenchScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.SPELL_DISPENSER.get(), SpellDispenserScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.ATELIER_STATION.get(), AtelierStationScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.ALCHEMY_BREWER.get(), AlchemyBrewerScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.ENDER_GRIMOIRE_INSCRIPTION.get(), EnderGrimoireInscriptionScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.ARCHIVISTS_GRIMOIRE.get(), ArchivistsGrimoireScreen::new));
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.SPELLCASTER_ACCESSORY_CASE.get(), SpellcasterAccessoryCaseScreen::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.ENDER_GRIMOIRE.get(), SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.ARCHIVISTS_GRIMOIRE.get(), SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.EXPLORERS_CODEX.get(), SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(), SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get(), SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.SPELLCASTER_AMMO_POUCH.get(), SpellcasterAmmoPouchCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.SPELLCASTER_QUIVER.get(), SpellcasterQuiverCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.FLOATMOUNT_BROOM.get(), BroomCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.HOVERRIDE_BROOM.get(), BroomCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.ASHEN_CIRCLET.get(), AshenCircletCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.ENCHANTED_CIRCLET.get(), CircletCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.MANA_THRUSTER.get(), ManaThrusterCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(ItemRegistry.MAGI_COMPRESSOR_GADGET.get(), MagiCompressorGadgetCurioRenderer::new));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.REFLECTCAST_SHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, living, seed) -> living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0f : 0.0f
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.PARRYCAST_BUCKLER.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, living, seed) -> {
                    var using = living != null && living.isUsingItem() && living.getUseItem() == stack;
                    if (living != null) {
                        // ItemStack の NBT を描画状態に使うと装備同期が使用の再開始を誘発するため、描画時の実状態だけを保持する。
                        ParrycastBuckler.observeClientUseAnimation(stack, living, using, living.level().getGameTime());
                    }
                    return using ? 1.0F : 0.0F;
                }
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.BULWARK_GREATSHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, living, seed) -> living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, living, seed) -> living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0f : 0.0f
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.SPELLCASTERS_FLASK.get(),
                ResourceLocation.withDefaultNamespace("filled"),
                (stack, level, living, seed) -> SpellcastersFlask.isFilled(stack) ? 1.0F : 0.0F
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.ALCHEMISTS_FLASK.get(),
                ResourceLocation.withDefaultNamespace("filled"),
                (stack, level, living, seed) -> SpellcastersFlask.isFilled(stack) ? 1.0F : 0.0F
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.STORAGE_STABILIZER.get(),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "selected_spell"),
                (stack, level, living, seed) -> StorageStabilizer.getSelectedSpellIndex(stack)
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.WOODEN_WAND.get(),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_cooldown"),
                // Iron's の同期状態はローカル本人の描画だけに使い、他者やドロップ品へ流用しない。
                (stack, level, living, seed) ->
                        WoodenWandClientRenderState.isImbuedSpellOnCooldown(stack, living) ? 1.0F : 0.0F
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.PARTIALLY_USED_INK.get(),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "ink_rarity"),
                // 1.20.1 の NBT モデル判定は client 接着コードとし、状態の解釈自体は共通ヘルパーへ寄せる。
                (stack, level, living, seed) -> PartiallyUsedInkState.getModelProperty(stack)
        ));
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ResourceLocation.withDefaultNamespace("throwing"),
                (stack, level, living, seed) -> living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F
        ));
        event.enqueueWork(ClientModBusEvents::registerBoundBowItemProperties);
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(ArsNouveauLuminousDeviceCompat.MOD_ID)) {
                ArsNouveauLuminousDeviceCompat.register();
            }
        });
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(SodiumDynamicLightsLuminousDeviceCompat.MOD_ID)) {
                SodiumDynamicLightsLuminousDeviceCompat.register();
            }
        });
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)) {
                BetterCombatClientCompat.register();
            }
        });
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)) {
                EpicFightClientCompat.register();
            }
        });
    }

    private static void registerBoundBowItemProperties() {
        ItemProperties.register(
                ItemRegistry.BOUND_BOW.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, living, seed) -> living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F
        );
        ItemProperties.register(
                ItemRegistry.BOUND_BOW.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, living, seed) -> {
                    if (living == null || living.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration() - living.getUseItemRemainingTicks()) / 20.0F;
                }
        );
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MagiCompressorGadgetCurioRenderer.EQUIPPED_MODEL);
    }

    private static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, manager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                CompletableFuture.completedFuture((Void) null)
                        .thenCompose(barrier::wait)
                        .thenRunAsync(() -> {
                            ExtrudedSpriteManager.clear();
                            EssenceSmokerParticlePaletteCache.clear();
                        }, gameExecutor)
        );
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.RETICLE_DOT.get(), ReticleDotParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.ADDITIVE_CIRCLE.get(),
                sprites -> new AdditiveGlowParticle.Provider(sprites, AdditiveGlowParticle.Preset.CIRCLE));
        event.registerSpriteSet(ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                sprites -> new AdditiveRhombusParticle.Provider(sprites, AdditiveRhombusParticle.Preset.RHOMBUS));
        event.registerSpriteSet(ParticleRegistry.ADDITIVE_SPARK.get(),
                sprites -> new AdditiveGlowParticle.Provider(sprites, AdditiveGlowParticle.Preset.SPARK));
        event.registerSpriteSet(ParticleRegistry.MUZZLE_FLASH.get(), MuzzleFlashParticle.Provider::new);
        event.registerSpecial(ParticleRegistry.IMPACT_TREMOR_BLOCK.get(), new ImpactTremorBlockParticle.Provider());
        event.registerSpecial(ParticleRegistry.SMASHCAST_DUST_PILLAR.get(), new SmashcastDustPillarParticle.Provider());
    }

    private static void registerTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CalibrationAdjustmentTooltip.class, CalibrationAdjustmentClientTooltipComponent::new);
        event.register(SpellcasterAmmoPouchTooltip.class, SpellcasterAmmoPouchClientTooltipComponent::new);
        event.register(LuminousDeviceTooltip.class, SpellcasterAmmoPouchClientTooltipComponent::new);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(SpellcastersFlask::getItemTintColor, ItemRegistry.SPELLCASTERS_FLASK.get(), ItemRegistry.ALCHEMISTS_FLASK.get());
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? 0x000000 : -1,
                ItemRegistry.CRUDE_INK.get()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        var defaultPlayerRenderer = event.getSkin("default");
        if (defaultPlayerRenderer != null) {
            defaultPlayerRenderer.addLayer(new SpectralWingLayer(defaultPlayerRenderer));
            defaultPlayerRenderer.addLayer(new ManaForceBladeSheathLayer(defaultPlayerRenderer));
        }

        var slimPlayerRenderer = event.getSkin("slim");
        if (slimPlayerRenderer != null) {
            slimPlayerRenderer.addLayer(new SpectralWingLayer(slimPlayerRenderer));
            slimPlayerRenderer.addLayer(new ManaForceBladeSheathLayer(slimPlayerRenderer));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.ALCHEMY_BREWER.get(), AlchemyBrewerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.ATELIER_STATION.get(), AtelierStationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), ArcanumInAJarBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.ESSENCE_SMOKER.get(), EssenceSmokerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.FROST_RUNE_TRAP.get(), FrostRuneTrapBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MAGE_LIGHT_TORCH.get(), MageLightTorchBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WIZARDLAMP_LANTERN.get(), WizardlampLanternBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MAGNETIC_STABILITY_ANCHOR.get(), MagneticStabilityAnchorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.PERSONAL_SHELF_CHEST.get(), PersonalShelfChestBlockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.RIFT_HOLE.get(), RiftHoleBlockEntityRenderer::new);

        event.registerEntityRenderer(EntityRegistry.SKY_EDGE_PROJECTILE.get(), SkyEdgeProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), InscribeIceDaggerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ANCHOR_BLINK_DAGGER.get(), AnchorBlinkDaggerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MANA_FORCE_BLADE_PROJECTILE.get(), ManaForceBladeProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCHER_MULTIPLE_BOW.get(), ArcherMultipleBowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMMENCE_FIRE_RIFLE.get(), CommenceFireRifleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), CompoundPhialProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.EXTRACT_POTION_PROJECTILE.get(), ExtractPotionProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.QUICK_ARMS_HANDGUN.get(), QuickArmsHandgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BREACHING_ENEMY_SHOTGUN.get(), BreachingEnemyShotgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SILENT_ASSASSIN_RIFLE.get(), SilentAssassinRifleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.LETHAL_ASSAULT_RIFLE.get(), LethalAssaultRifleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BULLET_STREAM_MINIGUN.get(), BulletStreamMinigunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.DUAL_ACROBAT_SMG.get(), DualAcrobatSmgRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TIRO_VOLLEY_MUSKET.get(), TiroVolleyMusketRenderer::new);
        event.registerEntityRenderer(EntityRegistry.THERMAL_PROCESS_THROWER.get(), ThermalProcessThrowerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CHARGED_TWIN_BLADE_STAFF_THROWN.get(), ChargedTwinBladeStaffThrownRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SPELL_INVOKE_CARD.get(), SpellThrowableCardRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SPELL_AUTONOMY_CARD.get(), SpellThrowableCardRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GRACED_RAIN_CLOUD.get(), GracedRainCloudRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TINY_LUMBERJACK_SAW.get(), TinyLumberjackSawRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCANE_BEAM.get(), ArcaneBeamRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SHOCK_BOLT.get(), ShockBoltRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MANA_SLASH_PROJECTILE.get(), ManaSlashProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLY_SWATTER_LAUNCHER.get(), FlySwatterLauncherRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), FlySwatterProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARTISAN_SMASH_LAUNCHER.get(), ArtisanSmashLauncherRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARTISAN_SMASH_SHELL.get(), ArtisanSmashShellRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ASSIST_WINGS_WING.get(), AssistWingsWingRenderer::new);
        event.registerEntityRenderer(EntityRegistry.DEMICREATOR_WINGS_CORE.get(), DemicreatorWingsCoreRenderer::new);
        event.registerEntityRenderer(EntityRegistry.DEMICREATOR_WINGS_WING.get(), DemicreatorWingsWingRenderer::new);
        event.registerEntityRenderer(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), AutoMagnetFamiliarRenderer::new);
        event.registerEntityRenderer(EntityRegistry.AUTO_TURRET.get(), AutoTurretRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FIELD_OVERSEER_STAFF.get(), FieldOverseerStaffRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SERVANT_GAZE_STAFF.get(), ServantGazeStaffRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SERVANT_GAZE_PROJECTILE.get(), ServantGazeProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TOTEM_OF_PERMAFROST_TOTEM.get(), TotemOfPermafrostTotemRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMPANION_TRUNK.get(), CompanionTrunkRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HEALING_BLOOM.get(), HealingBloomRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HEAVENLY_FIST_FIST.get(), HeavenlyFistFistRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ILLUMINATE_STELLAR_STAR.get(), IlluminateStellarStarRenderer::new);
        event.registerEntityRenderer(EntityRegistry.UNITE_LUNA_MOON.get(), UniteLunaMoonRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), MagicSpearMissileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), MysticShieldProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MYSTIC_SHIELD_SHIELD.get(), MysticShieldShieldRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FEATHER_RUSH_PROJECTILE.get(), FeatherRushProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FEATHER_RUSH_WING.get(), FeatherRushWingRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WORLD_FLATTER_DRILL.get(), WorldFlatterDrillRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GRIND_RUNNER_WHEEL.get(), GrindRunnerWheelRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SLASH_BLADE_KATANA.get(), SlashBladeKatanaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MOON_LIGHT_KATANA.get(), MoonLightKatanaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MOON_LIGHT_CHARGE_CUT.get(), MoonLightChargeCutRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HIGANBANA_KATANA.get(), HiganbanaKatanaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MANTIS_LEAP_BLADE.get(), MantisLeapBladeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.PRECISION_JACK_KNIFE.get(), PrecisionJackKnifeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.PHALANX_WEAPONRY.get(), PhalanxWeaponryRenderer::new);
        event.registerEntityRenderer(EntityRegistry.PHALANX_CHARGE_BEAM.get(), PhalanxChargeBeamRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SEARCH_BEACON.get(), SearchBeaconRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SPELL_DISPENSER_ANCHOR.get(), SpellDispenserAnchorRenderer::new);
        event.registerEntityRenderer(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), RemoteOwnerCastAnchorRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FUJIN_KATANA.get(), FujinKatanaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FUJIN_SLASH_PROJECTILE.get(), FujinSlashProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLOATMOUNT_BROOM.get(), FloatmountBroomRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HOVERRIDE_BROOM.get(), HoverrideBroomRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ANTI_MANA_ARROW.get(), AntiManaArrowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMBUSTION_JET_WAVE.get(), CombustionJetWaveRenderer::new);
    }
}
