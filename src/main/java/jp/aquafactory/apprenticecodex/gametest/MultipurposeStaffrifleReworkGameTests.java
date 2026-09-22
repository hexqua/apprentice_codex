package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleAdsMovement;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleRateLimiter;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleRecoil;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.spellgun.RifleSpellTooltipData;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MultipurposeStaffrifleReworkGameTests extends ApprenticeCodexGameTestScenarios {
    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeAdsMovementHonorsConfigAndSprintPriority(GameTestHelper helper) {
        ModConfigSpec.DoubleValue multiplier = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.MultipurposeStaffrifle.adsMovementSpeedMultiplier");
        double previous = multiplier.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_ads_movement");
        var rifle = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var otherId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_other_movement");
        speed.addTransientModifier(new AttributeModifier(otherId, 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        double baseline = speed.getValue();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
            multiplier.set(0.7D);
            // バニラの使用状態を開始せず、射撃中にも維持するADS入力の契約を確認する。
            MultipurposeStaffrifleAdsMovement.update(player, true);
            MultipurposeStaffrifleAdsMovement.update(player, true);
            helper.assertTrue(Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS must apply the configured multiplier once, preserving other modifiers");
            multiplier.set(0.0D);
            player.setSprinting(true);
            MultipurposeStaffrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(speed.getValue() == 0.0D, "Zero ADS multiplier must disable movement");
            helper.assertTrue(!player.isSprinting(), "ADS must suppress sprinting even at zero movement multiplier");
            MultipurposeStaffrifleAdsMovement.update(player, false);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D,
                    "Releasing ADS must restore movement even at zero multiplier");
            multiplier.set(1.0D);
            MultipurposeStaffrifleAdsMovement.update(player, true);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D, "Multiplier one must preserve speed");
            multiplier.set(0.7D);
            MultipurposeStaffrifleAdsMovement.update(player, true);
            player.setSprinting(true);
            MultipurposeStaffrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.isSprinting() && Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS must stop sprinting and preserve configured slowdown");
            MultipurposeStaffrifleAdsMovement.update(player, false);
            player.setSprinting(true);
            MultipurposeStaffrifleAdsMovement.update(player, true);
            helper.assertTrue(!player.isSprinting() && Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS requests during sprint must enter ADS and stop sprinting");
            MultipurposeStaffrifleAdsMovement.update(player, false);
            player.setSprinting(true);
            MultipurposeStaffrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.isSprinting(), "Releasing ADS must allow sprinting again");
            player.setSprinting(false);
            MultipurposeStaffrifleAdsMovement.update(player, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, rifle);
            player.setSprinting(true);
            MultipurposeStaffrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            MultipurposeStaffrifleAdsMovement.update(player, true);
            helper.assertTrue(player.isSprinting(), "Offhand ADS requests must not cancel sprinting");
            player.setSprinting(false);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D,
                    "Switching away must remove slowdown and offhand ADS requests must be ignored");
            helper.assertTrue(speed.hasModifier(otherId), "Cleanup must preserve unrelated modifiers");
        } finally {
            multiplier.set(previous);
            MultipurposeStaffrifleAdsMovement.update(player, false);
            speed.removeModifier(otherId);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wisdomDisablesSlotsWithoutDestroyingOrLockingScrolls(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_storage");
        var lookup = helper.getLevel().registryAccess();
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var rifle = (MultipurposeStaffrifle) stack.getItem();
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1));
        scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Owned scroll"));
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 4, "Base capacity must be four");
        var upgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        for (var slot = 1; slot <= 3; slot++) {
            helper.assertTrue(menu.getSlot(slot).mayPlace(upgrade), "Slot upgrades must be repeatable");
            menu.getSlot(slot).set(upgrade.copy());
            helper.assertTrue(menu.getEnabledScrollSlotCount() == 4 + slot * 2, "Each upgrade must add two slots");
        }
        menu.getSlot(4).set(scroll.copy());
        menu.getSlot(7).set(scroll.copy());
        menu.getSlot(13).set(scroll.copy());
        rifle.setSneakSelectionIndex(stack, 3);
        var wisdom = new ItemStack(ItemRegistry.WISDOM_SHARD.get());
        helper.assertTrue(menu.getSlot(1).mayPlace(wisdom), "Wisdom must coexist with filled scrolls and slot upgrades");
        menu.getSlot(1).set(wisdom);
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 0, "Wisdom must override all slot additions");
        helper.assertFalse(rifle.isSneakSelectionUiEnabled(stack), "Wheel mode must disable local selection");
        helper.assertTrue(MultipurposeStaffrifle.getSelectedSpellData(stack, lookup) == SpellData.EMPTY,
                "Disabled stored spell must not be available");
        var restored = ItemStack.parseOptional(lookup, (CompoundTag) stack.saveOptional(lookup));
        helper.assertTrue(MultipurposeStaffrifleScrollStorage.selected(restored) == 3, "Selection must survive wheel mode and serialization");
        helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, MultipurposeStaffrifleScrollStorage.get(restored, 9, lookup)),
                "Disabled scroll components must survive serialization");
        helper.assertFalse(ISpellContainer.isSpellContainer(restored), "Rifle must not inject spells into the wheel");
        helper.assertFalse(menu.getSlot(13).mayPlace(scroll), "Disabled slot must reject new scrolls");
        helper.assertTrue(menu.getSlot(13).mayPickup(player), "Disabled scroll must remain extractable");
        helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, menu.getSlot(13).remove(1)), "Extraction must preserve components");
        helper.assertTrue(MultipurposeStaffrifleScrollStorage.get(stack, 9, lookup).isEmpty(), "Extraction must remove the saved scroll");
        menu.getSlot(1).set(ItemStack.EMPTY);
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 8 && rifle.getSneakSelectionIndex(stack) == 3,
                "Removing Wisdom must restore capacity and selection");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverRingEnablesStoredLongSpellAndAddsEffectiveCastTime(GameTestHelper helper) throws Exception {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_long");
        var lookup = helper.getLevel().registryAccess();
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var rifle = (MultipurposeStaffrifle) stack.getItem();
        var spell = SpellRegistry.FIREBALL_SPELL.get();
        var data = new SpellData(spell, 1);
        var state = rifle.evaluateCalibrationImbue(stack, 0, data, lookup);
        helper.assertTrue(state.canInsert() && !state.isUsable(), "LONG must be storable without Silver Ring but unusable");
        rifle.trySetCalibrationAdjustment(stack, 0, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), lookup);
        helper.assertTrue(rifle.evaluateCalibrationImbue(stack, 0, data, lookup).isUsable(), "Silver Ring must permit LONG");
        helper.assertFalse(rifle.evaluateCalibrationImbue(stack, 0, new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1), lookup).canInsert(),
                "CONTINUOUS insertion must always be rejected");
        MultipurposeStaffrifleScrollStorage.set(stack, 0, SpellCalibrationImbueHelper.createScroll(data), lookup);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(0);
        player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 2));
        helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "LONG must cast immediately at zero mana");
        helper.assertTrue(magic.getMana() == 0 && !magic.isCasting(), "LONG completion must not wait or consume mana");
        helper.assertFalse(MultipurposeStaffrifleCastContext.isActiveFor(player.getUUID(), stack, spell),
                "Completed LONG must not leave a pending mana or ammunition context");
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 1,
                "LONG must consume exactly one round");
        var speed = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
        var original = speed.getBaseValue();
        try {
            for (var bonus : new double[]{0, 0.5}) {
                speed.setBaseValue(original + bonus);
                magic.setPlayerCastingItem(stack);
                magic.initiateCast(spell, 1, 0, CastSource.SWORD, SpellSelectionManager.MAINHAND);
                for (var recast : new boolean[]{false, true}) {
                    var event = new SpellCooldownAddedEvent.Pre(1000, spell, player, CastSource.SWORD);
                    try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), stack, spell, recast)) {
                        MultipurposeStaffrifleCastEvent.onSpellCooldownAdded(event);
                    }
                    helper.assertTrue(event.getEffectiveCooldown() == 1000 + (recast ? 0 : spell.getEffectiveCastTime(1, player)),
                            "Surcharge must respect cast speed and must not repeat for recasts");
                }
            }
        } finally {
            speed.setBaseValue(original);
            magic.resetCastingState();
        }
        rifle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY, lookup);
        helper.assertFalse(MultipurposeStaffrifleScrollStorage.get(stack, 0, lookup).isEmpty(), "Removing Silver Ring must preserve LONG scroll");
        helper.assertTrue(MultipurposeStaffrifle.getSelectedSpellData(stack, lookup) == SpellData.EMPTY, "Unsupported saved LONG must be disabled");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wheelModeDoesNotInheritStoredSpellOrTranscendence(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_wheel");
        var lookup = helper.getLevel().registryAccess();
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var rifle = (MultipurposeStaffrifle) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(0);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        MultipurposeStaffrifleScrollStorage.set(stack, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)), lookup);
        stack.enchant(lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.TRANSCENDENCE), 1);
        helper.assertTrue(MultipurposeStaffrifle.resolveCastSpellData(player, stack).getLevel() == 2,
                "Stored scroll must receive Transcendence once");
        // 両手武器でオフハンド選択を抑止する Better Combat 環境でも、装備魔法書から選択する。
        var wheelStack = createElementMaidenRobeSchoolPowerSpellbook(helper);
        equipCurio(player, Curios.SPELLBOOK_SLOT, wheelStack);
        rifle.trySetCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get()), lookup);
        helper.assertTrue(MultipurposeStaffrifle.resolveCastSpellData(player, stack) == SpellData.EMPTY,
                "Wheel mode must not fall back to stored scrolls");
        setWheelSpell(wheelStack, spell);
        magic.getSyncedData().setSpellSelection(new SpellSelection(Curios.SPELLBOOK_SLOT, 0));
        helper.assertTrue(MultipurposeStaffrifle.resolveCastSpellData(player, stack) != SpellData.EMPTY,
                "Equipped spellbook must provide the selected wheel spell");
        helper.assertTrue(MultipurposeStaffrifle.resolveCastSpellData(player, stack).getLevel() == 1,
                "Wheel spell must not receive the rifle's Transcendence");
        var beforeTooltip = stack.copy();
        var tooltip = RifleSpellTooltipData.read(stack, player, lookup);
        helper.assertTrue(tooltip.castSource() == CastSource.SPELLBOOK && tooltip.selectedSpell().getLevel() == 1,
                "Wisdom tooltip must retain the book source and must not add rifle Transcendence");
        helper.assertTrue(tooltip.slots().size() == 1 && !tooltip.slots().getFirst().usable(),
                "Wisdom tooltip must retain the disabled internal scroll");
        helper.assertTrue(ItemStack.isSameItemSameComponents(beforeTooltip, stack),
                "Wisdom tooltip must not mutate the rifle");
        helper.assertTrue(new SpellSelectionManager(player).getAllSpells().size() == 1, "Stored spell must not be added to wheel");
        player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 2));
        helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "Wheel INSTANT must cast at zero mana");
        helper.assertTrue(magic.getCastSource() == CastSource.SPELLBOOK, "Wisdom must use the selected source for actual casting");
        spell.castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, magic.getCastSource(), true);
        magic.resetCastingState();
        MultipurposeStaffrifleRateLimiter.clear(player);
        setWheelSpell(wheelStack, SpellRegistry.FIREBALL_SPELL.get());
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, false), "Wheel LONG must require Silver Ring");
        rifle.trySetCalibrationAdjustment(stack, 1, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), lookup);
        setWheelSpell(wheelStack, SpellRegistry.FIRE_BREATH_SPELL.get());
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, false), "Wheel CONTINUOUS must remain rejected");
        setWheelSpell(wheelStack, SpellRegistry.FIREBALL_SPELL.get());
        helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "Wheel LONG must cast with Silver Ring");
        helper.assertTrue(magic.getMana() == 0 && !magic.isCasting(), "Wheel LONG must complete without mana");
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 0,
                "Only the two successful casts may consume ammunition");
        helper.succeed();
    }

    private static void setWheelSpell(ItemStack stack, AbstractSpell spell) {
        var container = ISpellContainer.create(1, true, false).mutableCopy();
        container.addSpellAtIndex(spell, 1, 0, false);
        ISpellContainer.set(stack, container.toImmutable());
    }

    @GameTest(template = "gametest/basic_floor")
    public static void recoilAndAdjustmentsRetainSingleShotContract(GameTestHelper helper) {
        var recoil = new MultipurposeStaffrifleRecoil();
        for (var shot = 0; shot < 5; shot++) {
            var now = shot * 200_000_000L;
            helper.assertTrue(Math.abs(MultipurposeStaffrifleRecoil.shotPitch(true) - 0.675F) < 0.001F, "ADS must reduce every shot equally");
            recoil.addImpulse(MultipurposeStaffrifleRecoil.shotPitch(false), now);
            helper.assertTrue(Math.abs(recoil.advanceImpulses(now + 100_000_000) - 1.5F) < 0.001F, "Repeated shots must not build recoil");
            helper.assertTrue(recoil.advanceImpulses(now + 150_000_000) == 0, "View must not recover automatically");
        }
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var rifle = (MultipurposeStaffrifle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        rifle.trySetCalibrationAdjustment(stack, 0, new ItemStack(Items.SPYGLASS), lookup);
        rifle.trySetCalibrationAdjustment(stack, 1, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()), lookup);
        helper.assertTrue(MultipurposeStaffrifle.hasSpyglass(stack, lookup) && MultipurposeStaffrifle.hasRecoveryRune(stack, lookup),
                "Scope and recoil removal must coexist");
        stack.enchant(lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SURGE), 3);
        helper.assertTrue(rifle.getDefaultAttributeModifiers(stack).modifiers().isEmpty(), "Legacy SURGE must not restore spell power");
        helper.assertTrue(Enchantments.getLevel(stack, Enchantments.SURGE) == 3, "Legacy enchantment data must be retained");
        helper.succeed();
    }
}
