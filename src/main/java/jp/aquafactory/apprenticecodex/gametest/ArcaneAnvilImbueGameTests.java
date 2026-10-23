package jp.aquafactory.apprenticecodex.gametest;

import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRing;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharm;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharmCastManager;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.shield.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArcaneAnvilImbueGameTests {
    private ArcaneAnvilImbueGameTests() {
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void incompatibleSpellsSurviveAnvilNormalizationAndReload(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "anvil_incompatible_reload");
        try {
            for (var item : List.of(ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get(), ItemRegistry.REFLECTCAST_SHIELD.get(),
                    ItemRegistry.ATTACKCAST_RING.get(), ItemRegistry.JUMPCAST_CHARM.get(),
                    ItemRegistry.SPELL_INVOKE_CARD.get())) {
                var target = (RestrictedSpellImbuableItem) item;
                var spell = SpellRegistry.getEnabledSpells().stream()
                        .filter(candidate -> !target.canImbueSpell(candidate, 1)).findFirst().orElseThrow();
                var result = imbue(player, initializedStack(item), spell);
                assertStoredSpell(helper, result, spell);
                helper.assertFalse(target.acceptsCalibrationSpell(ISpellContainer.get(result).getSpellAtIndex(0)),
                        "Calibration must still reject the incompatible spell for " + item);
                target.normalizeImbuedSpellContainer(result);
                assertStoredSpell(helper, result, spell);
                var restored = ItemStack.parseOptional(player.registryAccess(),
                        (CompoundTag) result.saveOptional(player.registryAccess()));
                SpellCalibrationImbueHelper.prepareTarget(restored);
                item.inventoryTick(restored, helper.getLevel(), player, 0, false);
                assertStoredSpell(helper, restored, spell);
                if (!restored.is(ItemRegistry.SPELL_INVOKE_CARD.get())) {
                    var extracted = SpellCalibrationImbueHelper.removeScrollAt(restored, 0);
                    assertStoredSpell(helper, extracted, spell);
                } else {
                    helper.assertTrue(SpellCalibrationImbueHelper.removeScrollAt(restored, 0).isEmpty(),
                            "Cards must retain their scroll-duplication protection");
                }
            }
            for (var item : List.of(ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get())) {
                var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
                var result = imbue(player, initializedStack(item), spell);
                // preset復元が不適合を理由に上書き記録を捨てないことも確認する。
                ISpellContainer.set(result, ISpellContainer.create(1, false, false));
                SpellCalibrationImbueHelper.prepareTarget(result);
                assertStoredSpell(helper, result, spell);
                var replacement = SpellRegistry.MAGIC_MISSILE_SPELL.get();
                assertStoredSpell(helper, imbue(player, result, replacement), replacement);
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void specialEquipmentRemainsProtectedAndWeaponsCannotExpandSlots(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "anvil_special_protection");
        try {
            for (var item : List.of(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get(),
                    ItemRegistry.AUTOCAST_AMULET.get(), ItemRegistry.ELEMENTAL_BOW.get(),
                    ItemRegistry.MITHRIL_FREECAST_STAFF.get(), ItemRegistry.REVOLVERCAST_STAFF.get(),
                    ItemRegistry.SCROLLCASTER_GAUNTLET.get(), ItemRegistry.SHOOTING_STAR_MANTLE.get(),
                    ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get(), ItemRegistry.CHARGECAST_CATALYSTBOOK.get())) {
                var stack = initializedStack(item);
                var locked = ISpellContainer.create(1, false, false).mutableCopy();
                locked.addSpellAtIndex(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 0, true);
                ISpellContainer.set(stack, locked.toImmutable());
                helper.assertTrue(imbue(player, stack, SpellRegistry.MAGIC_MISSILE_SPELL.get()).isEmpty(),
                        "Arcane Anvil must protect dedicated storage for " + item);
            }
            for (var item : List.of(ItemRegistry.MANA_FORCE_BLADE.get(), ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
                var menu = new ArcaneAnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
                menu.getSlot(0).set(initializedStack(item));
                menu.getSlot(1).set(new ItemStack(
                        io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()));
                menu.createResult();
                helper.assertTrue(menu.getSlot(2).getItem().isEmpty(),
                        "Vanilla Iron's slot upgrades must not expand weapons: " + item);
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void incompatibleSpellWarningsAreRedOrderedAndReadOnly(GameTestHelper helper) {
        for (var item : List.of(ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.IRON_SWINGCAST_STAFF.get(), ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(), ItemRegistry.ATTACKCAST_RING.get(), ItemRegistry.JUMPCAST_CHARM.get())) {
            var target = (RestrictedSpellImbuableItem) item;
            var stack = target.createArcaneAnvilImbueResult(initializedStack(item),
                    new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1));
            var before = stack.copy();
            var lines = tooltip(stack);
            boolean gun = item instanceof AbstractSpellGunItem;
            var key = item instanceof SmashcastScepter
                    ? "item.apprenticecodex.spellgun.tooltip.invalid_spell.smashcast"
                    : "item.apprenticecodex.spellgun.tooltip.invalid_spell";
            int warning = indexOf(lines, key);
            helper.assertTrue(warning >= 0 && lines.stream().filter(line -> hasKey(line, key)).count() == 1,
                    "Incompatible spell warning must occur exactly once for " + item);
            var warningColor = item instanceof SmashcastScepter ? ChatFormatting.YELLOW : ChatFormatting.RED;
            helper.assertTrue(Objects.equals(TextColor.fromLegacyFormat(warningColor),
                            lines.get(warning).getStyle().getColor()),
                    "Incompatible spell warning must use the item's specified color");
            String preceding = gun ? "item.apprenticecodex.common.spellgun.desc_2"
                    : "item.apprenticecodex.swingcast.common.desc";
            if (item instanceof ICurioItem) {
                preceding = item.getDescriptionId() + ".desc";
            } else if (item instanceof SmashcastScepter) {
                preceding = item.getDescriptionId() + ".desc_2";
            }
            if (gun && ModList.get().isLoaded("epicfight")) {
                preceding = "item.apprenticecodex.common.spellgun.epicfight.offhand_warning";
            }
            helper.assertTrue(indexOf(lines, preceding) >= 0 && warning > indexOf(lines, preceding),
                    "Warning must follow normal item descriptions");
            if (item instanceof ICurioItem || item instanceof SmashcastScepter) {
                helper.assertTrue(warning == indexOf(lines, preceding) + 1,
                        "Warning must appear immediately after the final normal description");
            }
            if (!(item instanceof ICurioItem)) {
                helper.assertTrue(warning < indexOf(lines, "item.apprenticecodex.spellgun.tooltip.hint"),
                        "Warning must precede common help");
            }
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, stack),
                    "Reading the warning must not mutate the stored spell");
            var compatible = target.createArcaneAnvilImbueResult(stack,
                    new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1));
            helper.assertTrue(indexOf(tooltip(compatible), key) == -1
                            && indexOf(tooltip(initializedStack(item)), key) == -1,
                    "Compatible and initial equipment must not display an invalid-spell warning");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void incompatibleSpellsCannotTriggerOrConsumeResources(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "anvil_incompatible_cast");
        try {
            player.getInventory().add(new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 16));
            player.getInventory().add(new ItemStack(ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(), 16));
            for (var item : List.of(ItemRegistry.GOLD_SPELLCASTER_GUN.get(), ItemRegistry.IRON_SWINGCAST_STAFF.get())) {
                var stack = imbue(player, initializedStack(item), SpellRegistry.FIRE_BREATH_SPELL.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var before = stack.copy();
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(100.0F);
                float mana = magicData.getMana();
                var inventory = new ArrayList<ItemStack>();
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    inventory.add(player.getInventory().getItem(slot).copy());
                }
                boolean cast = item instanceof AbstractSpellGunItem gun
                        ? gun.tryTriggerImbuedSpell(player, InteractionHand.MAIN_HAND, null)
                        : ((AbstractSwingcastStaffItem) item).tryTriggerImbuedSpellOnSwing(player, true);
                helper.assertFalse(cast, "Incompatible imbued spells must never trigger");
                helper.assertTrue(magicData.getMana() == mana && !magicData.isCasting()
                                && ItemStack.isSameItemSameComponents(before, stack),
                        "Rejected casts must preserve mana, durability, and spell data");
                for (int slot = 0; slot < inventory.size(); slot++) {
                    helper.assertTrue(ItemStack.isSameItemSameComponents(inventory.get(slot),
                                    player.getInventory().getItem(slot)),
                            "Rejected casts must preserve ammunition and inventory");
                }
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void incompatibleAccessoryAndSmashSpellsSendDedicatedErrors(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createAssistWingsRider(helper,
                new BlockPos(0, 2, 0), "invalid_accessory_errors");
        var messages = new ArrayList<Component>();
        var previous = player.connection;
        var channel = captureActionbars(player, messages);
        try {
            var longSpell = SpellRegistry.getEnabledSpells().stream()
                    .filter(spell -> spell.getCastType() == CastType.LONG).findFirst().orElseThrow();
            for (var item : List.of(ItemRegistry.SMASHCAST_SCEPTER.get(), ItemRegistry.ATTACKCAST_RING.get(),
                    ItemRegistry.JUMPCAST_CHARM.get())) {
                var target = (RestrictedSpellImbuableItem) item;
                for (var spell : List.of(SpellRegistry.FIRE_BREATH_SPELL.get(), longSpell)) {
                    if (target.canImbueSpell(spell, 1)) {
                        continue;
                    }
                    var stack = target.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(spell, 1));
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    if (item instanceof JumpcastCharm) {
                        CuriosApi.getCuriosInventory(player).orElseThrow()
                                .setEquippedCurio(CuriosSlotConstants.FEET, 0, stack);
                        player.setOnGround(false);
                    }
                    var snapshot = stack.copy();
                    var magicData = Objects.requireNonNull(MagicData.getPlayerMagicData(player));
                    magicData.setMana(100.0F);
                    messages.clear();
                    boolean cast;
                    String key;
                    if (item instanceof SmashcastScepter scepter) {
                        cast = scepter.tryCastSmashSpell(player, stack, 5.0F);
                        key = "ui.apprenticecodex.smashcast.cannot_cast";
                    } else if (item instanceof AttackcastRing ring) {
                        cast = ring.tryTriggerImbuedSpell(player, stack, "gametest");
                        key = "ui.apprenticecodex.swingcast.cannot_swing_cast";
                    } else {
                        cast = JumpcastCharmCastManager.tryCast(player);
                        key = "ui.apprenticecodex.jumpcast.cannot_long_cast";
                    }
                    helper.assertFalse(cast, "Incompatible accessory and smash spells must not cast");
                    helper.assertTrue(messages.size() == 1 && hasKey(messages.getFirst(), key),
                            "Rejected cast must send exactly one actionbar with the specified translation key: " + key);
                    var contents = (TranslatableContents) messages.getFirst().getContents();
                    helper.assertTrue(contents.getArgs().length == 1
                                    && contents.getArgs()[0] instanceof Component name
                                    && name.getString().equals(spell.getDisplayName(player).getString())
                                    && Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.RED),
                                    messages.getFirst().getStyle().getColor()),
                            "Invalid-spell message must be red and name the rejected spell");
                    helper.assertTrue(magicData.getMana() == 100.0F && !magicData.isCasting()
                                    && !magicData.getPlayerCooldowns().isOnCooldown(spell)
                                    && ItemStack.isSameItemSameComponents(snapshot, stack),
                            "Rejected cast must preserve mana, cooldown, durability, and the stored spell");
                }
            }
        } finally {
            player.connection = previous;
            player.discard();
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void smashcastWarningDoesNotPreventOrdinaryContinuousCasting(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "smashcast_ordinary_cast");
        var magic = Objects.requireNonNull(MagicData.getPlayerMagicData(player));
        try {
            var item = (SmashcastScepter) ItemRegistry.SMASHCAST_SCEPTER.get();
            var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
            var stack = item.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(spell, 1));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            magic.setMana(1000.0F);
            magic.getSyncedData().learnSpell(spell, false);
            magic.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.MAINHAND, 0));
            helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(SpellSelectionManager.MAINHAND)
                            .stream().anyMatch(option -> option.spellData.getSpell().equals(spell)),
                    "Scepter must expose its continuous spell to ordinary selection");
            helper.assertFalse(item.canImbueSpell(spell, 1), "Continuous spell must remain ineligible for smash casting");
            helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()
                            && magic.isCasting() && magic.getCastingSpellId().equals(spell.getSpellId()),
                    "Scepter must still start an ordinary right-click continuous cast");
        } finally {
            magic.resetCastingState();
            player.stopUsingItem();
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void shieldWarningsTrackAdjustmentsWithoutAddingWheelSelections(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "shield_imbue_warnings");
        try {
            for (var item : List.of(ItemRegistry.REFLECTCAST_SHIELD.get(), ItemRegistry.PARRYCAST_BUCKLER.get(),
                    ItemRegistry.BULWARK_GREATSHIELD.get())) {
                var shield = (AbstractImbueShieldItem) item;
                boolean bulwark = item == ItemRegistry.BULWARK_GREATSHIELD.get();
                for (var spell : List.of(SpellRegistry.MAGIC_MISSILE_SPELL.get(), SpellRegistry.FIRE_BREATH_SPELL.get())) {
                    var stack = shield.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(spell, 1));
                    boolean continuous = spell == SpellRegistry.FIRE_BREATH_SPELL.get();
                    assertShieldWarning(helper, stack, bulwark != continuous);
                    helper.assertFalse(ISpellContainer.get(stack).isSpellWheel(),
                            "Imbue shields must keep their spell containers hidden from the wheel");
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
                    var selections = new SpellSelectionManager(player);
                    helper.assertTrue(selections.getSpellsForSlot(SpellSelectionManager.MAINHAND).isEmpty()
                                    && selections.getSpellsForSlot(SpellSelectionManager.OFFHAND).isEmpty(),
                            "No shield may add its imbued spell to either hand's selection options");
                }
                if (!bulwark) {
                    var longSpell = SpellRegistry.getEnabledSpells().stream()
                            .filter(spell -> spell.getCastType() == CastType.LONG && spell.getRecastCount(1, null) <= 0)
                            .findFirst().orElseThrow();
                    var stack = shield.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(longSpell, 1));
                    assertShieldWarning(helper, stack, true);
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())),
                            "Silver Ring must be accepted by the shield");
                    assertShieldWarning(helper, stack, false);
                    var continuous = shield.createArcaneAnvilImbueResult(stack,
                            new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1));
                    assertShieldWarning(helper, continuous, item == ItemRegistry.PARRYCAST_BUCKLER.get());
                    SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
                    assertShieldWarning(helper, stack, true);
                }
                var invalidSpell = bulwark ? SpellRegistry.MAGIC_MISSILE_SPELL.get() : SpellRegistry.FIRE_BREATH_SPELL.get();
                var wisdom = shield.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(invalidSpell, 1));
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(wisdom, 0,
                                new ItemStack(ItemRegistry.WISDOM_SHARD.get())),
                        "Wisdom Shard must enable selected-spell casting");
                assertShieldWarning(helper, wisdom, false);
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.arcane_anvil_imbue")
    public static void shieldsReportIncompatibleSpellsWithoutSpendingResources(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createAssistWingsRider(helper,
                new BlockPos(0, 2, 0), "shield_imbue_errors");
        var messages = new ArrayList<Component>();
        var previous = player.connection;
        var channel = captureActionbars(player, messages);
        try {
            var longSpell = SpellRegistry.getEnabledSpells().stream()
                    .filter(spell -> spell.getCastType() == CastType.LONG && spell.getRecastCount(1, null) <= 0)
                    .findFirst().orElseThrow();
            var recastSpell = SpellRegistry.getEnabledSpells().stream()
                    .filter(spell -> spell.getRecastCount(1, null) > 0).findFirst().orElseThrow();
            for (var item : List.of(ItemRegistry.REFLECTCAST_SHIELD.get(), ItemRegistry.PARRYCAST_BUCKLER.get(),
                    ItemRegistry.BULWARK_GREATSHIELD.get())) {
                var spells = item == ItemRegistry.BULWARK_GREATSHIELD.get()
                        ? List.of(SpellRegistry.MAGIC_MISSILE_SPELL.get(), longSpell)
                        : List.of(SpellRegistry.FIRE_BREATH_SPELL.get(), longSpell, recastSpell);
                for (var spell : spells) {
                    var shield = (AbstractImbueShieldItem) item;
                    var stack = shield.createArcaneAnvilImbueResult(initializedStack(item), new SpellData(spell, 1));
                    player.setItemInHand(InteractionHand.OFF_HAND, stack);
                    var magic = Objects.requireNonNull(MagicData.getPlayerMagicData(player));
                    magic.setMana(1000.0F);
                    float manaBefore = magic.getMana();
                    var snapshot = stack.copy();
                    messages.clear();
                    String key;
                    if (item == ItemRegistry.REFLECTCAST_SHIELD.get()) {
                        helper.assertFalse(ReflectcastShieldRuntime.tryTriggerSpell(player, stack, InteractionHand.OFF_HAND),
                                "Reflectcast must reject spells without the required adjustments");
                        key = "ui.apprenticecodex.reflectcast.cannot_cast";
                    } else if (item instanceof ParrycastBuckler buckler) {
                        buckler.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
                        helper.assertTrue(buckler.handlePerfectGuard(player, stack, InteractionHand.OFF_HAND),
                                "Parrycast test must reach a successful guard");
                        key = "ui.apprenticecodex.parrycast.cannot_cast";
                    } else {
                        BulwarkGreatshieldRuntime.beginUse(player);
                        BulwarkGreatshieldRuntime.tryStartContinuousCast(player, stack, InteractionHand.OFF_HAND);
                        BulwarkGreatshieldRuntime.tryStartContinuousCast(player, stack, InteractionHand.OFF_HAND);
                        key = "ui.apprenticecodex.shieldcast.cannot_cast";
                    }
                    helper.assertTrue(messages.size() == 1 && hasKey(messages.getFirst(), key)
                                    && Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.RED), messages.getFirst().getStyle().getColor()),
                            "Shield must send one red actionbar for an incompatible spell: " + key);
                    var context = item + " / " + spell.getSpellId();
                    helper.assertTrue(magic.getMana() == manaBefore,
                            "Rejected shield casting must preserve mana: " + context + " / " + magic.getMana());
                    helper.assertFalse(magic.isCasting(), "Rejected shield casting must not start casting: " + context);
                    helper.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell),
                            "Rejected shield casting must not add cooldown: " + context);
                    helper.assertTrue(stack.getDamageValue() == snapshot.getDamageValue(),
                            "Rejected shield casting must preserve durability: " + context);
                    helper.assertTrue(ISpellContainer.get(stack).equals(ISpellContainer.get(snapshot)),
                            "Rejected shield casting must preserve the stored spell: " + context);
                    player.stopUsingItem();
                    ReflectcastShieldRuntime.clear(player);
                    BulwarkGreatshieldRuntime.clear(player);
                }
            }
        } finally {
            ReflectcastShieldRuntime.clear(player);
            BulwarkGreatshieldRuntime.clear(player);
            player.connection = previous;
            player.discard();
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }

    private static void assertShieldWarning(GameTestHelper helper, ItemStack stack, boolean expected) {
        var snapshot = stack.copy();
        var lines = tooltip(stack);
        var key = "item.apprenticecodex.spellgun.tooltip.invalid_spell";
        int warning = indexOf(lines, key);
        helper.assertTrue((warning >= 0) == expected, "Shield warning must match its current casting configuration: " + stack);
        if (expected) {
            helper.assertTrue(Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.RED), lines.get(warning).getStyle().getColor())
                            && warning == indexOf(lines, stack.getItem().getDescriptionId() + ".cast_default") + 1
                            && warning < indexOf(lines, "item.apprenticecodex.spellgun.tooltip.hint"),
                    "Shield warning must be red, after its description, and before hidden details");
        }
        helper.assertTrue(ItemStack.isSameItemSameComponents(snapshot, stack), "Shield warning lookup must be read-only");
    }

    private static EmbeddedChannel captureActionbars(ServerPlayer player, List<Component> messages) {
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(player.serverLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(@NotNull Packet<?> packet) {
                captureActionbar(packet);
            }

            @Override
            public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {
                captureActionbar(packet);
            }

            // displayClientMessage は送信完了 callback 付き、Jumpcast は callback なしで送信する。
            private void captureActionbar(Packet<?> packet) {
                if (packet instanceof ClientboundSetActionBarTextPacket actionbar) {
                    messages.add(actionbar.text());
                } else if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay()) {
                    messages.add(chat.content());
                }
            }
        };
        return channel;
    }

    private static ItemStack initializedStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer preset) {
            preset.initializeSpellContainer(stack);
        } else {
            ISpellContainer.set(stack, ISpellContainer.create(1, false, false));
        }
        return stack;
    }

    private static ItemStack imbue(ServerPlayer player, ItemStack stack, AbstractSpell spell) {
        var menu = new ArcaneAnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(stack);
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scroll);
        menu.getSlot(1).set(scroll);
        menu.createResult();
        return menu.getSlot(2).getItem().copy();
    }

    private static void assertStoredSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell) {
        helper.assertFalse(stack.isEmpty(), "Imbue result must not be empty");
        var container = ISpellContainer.get(stack);
        helper.assertTrue(container != null && container.getSpellAtIndex(0).getSpell().equals(spell)
                        && container.getSpellAtIndex(0).getLevel() == 1,
                "Normalization and save/load must retain the imbued spell");
    }

    private static List<Component> tooltip(ItemStack stack) {
        var lines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        if (stack.getItem() instanceof ICurioItem curio) {
            return curio.getSlotsTooltip(lines, Item.TooltipContext.EMPTY, stack);
        }
        return lines;
    }

    private static int indexOf(List<Component> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            if (hasKey(lines.get(i), key)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasKey(Component line, String key) {
        return line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                || line.getSiblings().stream().anyMatch(sibling -> hasKey(sibling, key));
    }
}
