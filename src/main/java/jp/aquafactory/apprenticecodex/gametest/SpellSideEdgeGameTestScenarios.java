package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceSpellLevelEvent;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeOffhandAttributeBridge;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlink;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancer;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

final class SpellSideEdgeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double TOLERANCE = 1.0e-9D;

    private SpellSideEdgeGameTestScenarios() {
    }

    static void spellSideEdgeStartsWithEdgeDancerAndExpectedStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellSideEdge) ItemRegistry.SPELL_SIDE_EDGE.get();
            var stack = item.getDefaultInstance();
            helper.assertTrue(stack.getMaxDamage() == 1561,
                    "Spell Side Edge durability should be 1561 but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == 22,
                    "Spell Side Edge enchantability should be 22 but got " + item.getEnchantmentValue(stack));
            helper.assertTrue(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Spell Side Edge should be a UniqueItem");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Spell Side Edge did not initialize a spell container");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Spell Side Edge should have exactly one spell slot");
            var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != SpellData.EMPTY
                            && spellData.getSpell() == SpellRegistry.EDGE_DANCER.get()
                            && spellData.getLevel() == 1
                            && spellData.isLocked(),
                    "Spell Side Edge should start with locked Edge Dancer Lv1 but got " + spellData);

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION,
                    3.0D,
                    "Spell Side Edge attack damage modifier should display as 4 damage"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION,
                    -1.6D,
                    "Spell Side Edge attack speed modifier should display as 2.4 speed"
            );

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_side_edge_use_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Spell Side Edge should keep vanilla sword right-click behavior but got "
                            + useResult.getResult());
        });
    }

    static void spellSideEdgeKeepsExpectedTagsAndEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get());
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Spell Side Edge is missing malum:soul_hunter_weapon");
            assertUpgradeable(helper, stack, "Spell Side Edge should accept Iron's upgrade orbs");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellSideEdgeEnchantments(helper.getLevel().registryAccess(), stack),
                    "Spell Side Edge"
            );
        });
    }

    static void spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            var maxMana = AttributeRegistry.MAX_MANA.get();
            var mainhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(spellPower, modifier("main_spell_power", 0.10D, AttributeModifier.Operation.MULTIPLY_BASE))
                    .put(maxMana, modifier("main_max_mana", 100.0D, AttributeModifier.Operation.ADDITION))
                    .build();
            var offhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(spellPower, modifier("offhand_spell_power", 0.05D, AttributeModifier.Operation.MULTIPLY_BASE))
                    .put(maxMana, modifier("offhand_max_mana", 150.0D, AttributeModifier.Operation.ADDITION))
                    .build();

            var bridgedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(mainhandModifiers, offhandModifiers);
            assertSingleModifierAmount(
                    helper,
                    bridgedModifiers.get(spellPower),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Spell Side Edge bridge should add only the missing higher spell power amount"
            );
            helper.assertTrue(bridgedModifiers.get(maxMana).isEmpty(),
                    "Spell Side Edge bridge should not add max mana when offhand amount is already higher: "
                            + describeModifiers(bridgedModifiers));
        });
    }

    static void spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mainhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.MOVEMENT_SPEED,
                            modifier("main_speed_total", 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL))
                    .build();
            var offhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.MOVEMENT_SPEED,
                            modifier("offhand_speed_total", 0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL))
                    .build();

            var skippedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(mainhandModifiers, offhandModifiers);
            helper.assertTrue(skippedModifiers.isEmpty(),
                    "Spell Side Edge bridge should skip duplicated MULTIPLY_TOTAL attributes but got "
                            + describeModifiers(skippedModifiers));

            var copiedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(
                    mainhandModifiers,
                    ImmutableMultimap.of()
            );
            assertSingleModifierAmount(
                    helper,
                    copiedModifiers.get(Attributes.MOVEMENT_SPEED),
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                    0.10D,
                    "Spell Side Edge bridge should copy MULTIPLY_TOTAL when offhand has no matching attribute"
            );
        });
    }

    static void spellSideEdgeBridgeIgnoresVanillaAttackAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            var mainhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.ATTACK_DAMAGE,
                            modifier("main_attack_damage", 3.0D, AttributeModifier.Operation.ADDITION))
                    .put(Attributes.ATTACK_SPEED,
                            modifier("main_attack_speed", -1.6D, AttributeModifier.Operation.ADDITION))
                    .put(spellPower, modifier("main_spell_power", 0.10D, AttributeModifier.Operation.MULTIPLY_BASE))
                    .build();

            var bridgedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(
                    mainhandModifiers,
                    ImmutableMultimap.of()
            );
            helper.assertTrue(bridgedModifiers.get(Attributes.ATTACK_DAMAGE).isEmpty(),
                    "Spell Side Edge bridge should not copy vanilla attack damage: "
                            + describeModifiers(bridgedModifiers));
            helper.assertTrue(bridgedModifiers.get(Attributes.ATTACK_SPEED).isEmpty(),
                    "Spell Side Edge bridge should not copy vanilla attack speed: "
                            + describeModifiers(bridgedModifiers));
            assertSingleModifierAmount(
                    helper,
                    bridgedModifiers.get(spellPower),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.10D,
                    "Spell Side Edge bridge should still copy spellcasting attributes"
            );
        });
    }

    static void spellSideEdgeBridgeIncludesStackAttributeModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            var stack = new ItemStack(Items.STICK);
            stack.addAttributeModifier(
                    spellPower,
                    modifier("stack_spell_power", 0.12D, AttributeModifier.Operation.MULTIPLY_BASE),
                    EquipmentSlot.MAINHAND
            );

            var bridgedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(stack);
            assertSingleModifierAmount(
                    helper,
                    bridgedModifiers.get(spellPower),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.12D,
                    "Spell Side Edge bridge should include stack AttributeModifiers NBT"
            );
        });
    }

    static void spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_side_edge_bridge_amount_resync_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()));

            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var spellPowerInstance = player.getAttribute(spellPowerAttribute);
            helper.assertTrue(spellPowerInstance != null,
                    "Spell Side Edge bridge amount resync test could not resolve player spell power attribute");

            player.setItemInHand(InteractionHand.OFF_HAND, stackWithSpellPowerModifier(0.05D));
            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var initialAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(initialAmount - 0.05D) < TOLERANCE,
                    "Spell Side Edge bridge should apply initial stack AttributeModifiers amount, got "
                            + initialAmount + " modifiers=" + spellPowerInstance.getModifiers());

            player.setItemInHand(InteractionHand.OFF_HAND, stackWithSpellPowerModifier(0.12D));
            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var resyncedAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(resyncedAmount - 0.12D) < TOLERANCE,
                    "Spell Side Edge bridge should replace stale stack AttributeModifiers amount, got "
                            + resyncedAmount + " modifiers=" + spellPowerInstance.getModifiers());
        });
    }

    static void spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_side_edge_bridge_sync_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()));

            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var spellPowerInstance = player.getAttribute(spellPowerAttribute);
            helper.assertTrue(spellPowerInstance != null,
                    "Spell Side Edge bridge test could not resolve player spell power attribute");

            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var bridgedAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(bridgedAmount - 0.05D) < TOLERANCE,
                    "Spell Side Edge bridge should copy Unite Luna Staff mainhand spell power while held, got "
                            + bridgedAmount + " modifiers=" + spellPowerInstance.getModifiers());

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AIR));
            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var clearedAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(clearedAmount) < TOLERANCE,
                    "Spell Side Edge bridge should clear copied modifiers when unequipped, got "
                            + clearedAmount + " modifiers=" + spellPowerInstance.getModifiers());
        });
    }

    static void edgeDancerRequiresMainhandSpellSideEdge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_condition_test");
            var magicData = resolveMagicData(helper, player);
            var spell = edgeDancer();

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Edge Dancer should not cast with empty hands");

            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Edge Dancer should not cast from an offhand Spell Side Edge");

            player.setItemInHand(InteractionHand.MAIN_HAND,
                    ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Edge Dancer should cast only while Spell Side Edge is in the main hand");
        });
    }

    static void edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_empty_offhand_test");
            var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());

            var mirror = player.getOffhandItem();
            helper.assertTrue(SpellSideEdgeMirror.isGeneratedMirror(mirror),
                    "Edge Dancer should generate a Spell Side Edge Mirror in an empty offhand");
            helper.assertTrue(player.getMainHandItem() == mainhand,
                    "Edge Dancer should keep the original mainhand Spell Side Edge stack in place");

            var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE);
            helper.assertTrue(state.active && !state.hadStoredOffhand(),
                    "Edge Dancer state should be active without a stored offhand item");

            EdgeDancerManager.deactivate(player, true);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Edge Dancer should restore an originally empty offhand to empty");
            helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                    "Edge Dancer state should be inactive after deactivation");
        });
    }

    static void edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_copy_test");
            var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
            mainhand.enchant(Enchantments.SHARPNESS, 3);
            mainhand.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
            mainhand.getOrCreateTag().putString("apprenticecodex:test_copy_tag", "copied");
            player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());

            var mirror = player.getOffhandItem();
            helper.assertTrue(SpellSideEdgeMirror.isGeneratedMirror(mirror),
                    "Edge Dancer should generate a managed Mirror");
            helper.assertTrue(mirror.getEnchantmentLevel(Enchantments.SHARPNESS) == 3,
                    "Spell Side Edge Mirror should copy enchantments from the mainhand item");
            helper.assertTrue(mirror.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get()) == 1,
                    "Spell Side Edge Mirror should copy Transcendence from the mainhand item");
            helper.assertTrue("copied".equals(mirror.getOrCreateTag().getString("apprenticecodex:test_copy_tag")),
                    "Spell Side Edge Mirror should copy non-spell NBT from the mainhand item");

            assertSpellData(helper, ISpellContainer.get(mirror), 0, SpellRegistry.ANCHOR_BLINK.get(), 1, true,
                    "Spell Side Edge Mirror should replace its imbued spell with Anchor Blink");
            assertSpellData(helper, ISpellContainer.get(mainhand), 0, SpellRegistry.EDGE_DANCER.get(), 1, true,
                    "Mainhand Spell Side Edge should keep Edge Dancer");
            var levelEvent = new ModifySpellLevelEvent(SpellRegistry.ANCHOR_BLINK.get(), player, 1, 1);
            TranscendenceSpellLevelEvent.onModifySpellLevel(levelEvent);
            helper.assertTrue(levelEvent.getLevel() == 2,
                    "Offhand Spell Side Edge Mirror should apply Transcendence to Anchor Blink");

            EdgeDancerManager.deactivate(player, true);
        });
    }

    static void edgeDancerRestoresOccupiedOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_restore_offhand_test");
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());

            helper.assertTrue(SpellSideEdgeMirror.isGeneratedMirror(player.getOffhandItem()),
                    "Edge Dancer should replace an occupied offhand with the Mirror");
            var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE);
            helper.assertTrue(state.hadStoredOffhand() && state.getStoredOffhandStack().is(Items.SHIELD),
                    "Edge Dancer state should store the replaced offhand item");

            EdgeDancerManager.deactivate(player, true);
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Edge Dancer should restore the replaced offhand item");
        });
    }

    static void edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_main_loss_test");
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());
            var droppedMainhand = player.getMainHandItem().copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            EdgeDancerManager.validateActiveMirrorLocation(player);

            helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                    "Edge Dancer should deactivate when Spell Side Edge leaves the player inventory");
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Edge Dancer should restore the offhand when the main Spell Side Edge is lost");
            helper.assertTrue(SpellSideEdge.isSpellSideEdge(droppedMainhand),
                    "The dropped Spell Side Edge should remain a normal item stack");
        });
    }

    static void edgeDancerMainhandDropCancelsAndOnlyDeactivates(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_main_drop_test");
            var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());
            var handled = EdgeDancerManager.handlePlayerAction(
                    player,
                    new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.DROP_ITEM,
                            BlockPos.ZERO,
                            Direction.DOWN,
                            0
                    )
            );

            helper.assertTrue(handled,
                    "Dropping the mainhand Spell Side Edge during Edge Dancer should be cancelled");
            helper.assertTrue(SpellSideEdge.isSpellSideEdge(player.getMainHandItem()),
                    "Cancelled Edge Dancer drop should keep Spell Side Edge in the main hand");
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Cancelled Edge Dancer drop should restore the original offhand item");
            helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                    "Cancelled Edge Dancer drop should deactivate the effect");
        });
    }

    static void edgeDancerOffhandSwapCancelsAndOnlyDeactivates(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "edge_dancer_offhand_swap_test");
            var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), edgeDancer());
            var handled = EdgeDancerManager.handlePlayerAction(
                    player,
                    new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            BlockPos.ZERO,
                            Direction.DOWN,
                            0
                    )
            );

            helper.assertTrue(handled,
                    "Swapping offhand during Edge Dancer should be cancelled");
            helper.assertTrue(SpellSideEdge.isSpellSideEdge(player.getMainHandItem()),
                    "Cancelled Edge Dancer offhand swap should keep Spell Side Edge in the main hand");
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Cancelled Edge Dancer offhand swap should restore the original offhand item");
            helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                    "Cancelled Edge Dancer offhand swap should deactivate the effect");
        });
    }

    static void spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = SpellSideEdgeMirror.create(UUID.randomUUID(),
                    ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());

            var modifiers = stack.getAttributeModifiers(EquipmentSlot.OFFHAND);
            helper.assertTrue(modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty(),
                    "Spell Side Edge Mirror offhand should not stack vanilla attack damage on the player");
            helper.assertTrue(modifiers.get(Attributes.ATTACK_SPEED).isEmpty(),
                    "Spell Side Edge Mirror offhand should not stack vanilla attack speed on the player");

            var bridgedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(stack);
            helper.assertTrue(bridgedModifiers.get(Attributes.ATTACK_DAMAGE).isEmpty(),
                    "Spell Side Edge bridge should not copy Mirror mainhand attack damage: "
                            + describeModifiers(bridgedModifiers));
            helper.assertTrue(bridgedModifiers.get(Attributes.ATTACK_SPEED).isEmpty(),
                    "Spell Side Edge bridge should not copy Mirror mainhand attack speed: "
                            + describeModifiers(bridgedModifiers));
        });
    }

    static void spellSideEdgeMirrorPairBypassesVanillaTargetIframe(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (isCombatOverhaulLoaded()) {
                return;
            }

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_side_edge_iframe_pair_test");
            equipSpellSideEdgePair(player);
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));

            helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                    "Initial player attack should apply vanilla i-frame setup");
            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target));
            helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                    "Spell Side Edge pair should let the recorded vanilla target ignore i-frames");
        });
    }

    static void spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_side_edge_iframe_no_mirror_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));

            helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                    "Initial no-mirror player attack should apply vanilla i-frame setup");
            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target));
            helper.assertFalse(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                    "Spell Side Edge without Mirror should not bypass vanilla i-frames");
        });
    }

    static void spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (isCombatOverhaulLoaded()) {
                return;
            }

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_side_edge_iframe_target_only_test");
            equipSpellSideEdgePair(player);
            var primaryTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            var otherTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));

            helper.assertTrue(primaryTarget.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                    "Initial primary target hit should apply vanilla i-frame setup");
            helper.assertTrue(otherTarget.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                    "Initial other target hit should apply vanilla i-frame setup");

            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, primaryTarget));
            helper.assertFalse(otherTarget.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                    "Non-recorded sweep-like target should keep vanilla i-frames");
            helper.assertTrue(primaryTarget.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                    "Recorded primary target should still consume the pending i-frame bypass");
        });
    }

    static void spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!isCombatOverhaulLoaded()) {
                return;
            }

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_side_edge_iframe_combat_overhaul_test");
            equipSpellSideEdgePair(player);
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));

            helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F),
                    "Initial combat-overhaul player attack should apply vanilla i-frame setup");
            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target));
            helper.assertFalse(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                    "Spell Side Edge i-frame bypass should be disabled with BetterCombat or EpicFight loaded");
        });
    }

    static void anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "anchor_blink_cast_condition_test");
            var magicData = resolveMagicData(helper, player);
            var spell = anchorBlink();

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Anchor Blink should not cast without a generated Spell Side Edge Mirror");

            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get()));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Anchor Blink should require the managed Edge Dancer Mirror, not a loose Mirror item");

            var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, magicData, edgeDancer());

            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Anchor Blink should cast while the managed Mirror is in the offhand");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Anchor Blink should restore the original offhand through Edge Dancer deactivation");
            helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                            .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                    "Anchor Blink should end Edge Dancer when the dagger is thrown");
            var daggers = helper.getLevel().getEntitiesOfClass(
                    AnchorBlinkDaggerEntity.class,
                    player.getBoundingBox().inflate(8.0D)
            );
            helper.assertTrue(daggers.size() == 1,
                    "Anchor Blink should spawn exactly one anchor dagger but got " + daggers.size());
            helper.assertTrue(daggers.get(0).getDamageForTesting() > 0.0F,
                    "Anchor Blink dagger should carry spell damage");
        });
    }

    static void anchorBlinkReadyAnchorOnlyMatchesOwner(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "anchor_blink_owner_test");
            var otherPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                    "anchor_blink_other_owner_test");
            var dagger = new AnchorBlinkDaggerEntity(EntityRegistry.ANCHOR_BLINK_DAGGER.get(), helper.getLevel(), owner);
            helper.getLevel().addFreshEntity(dagger);

            dagger.impactForTesting(owner.position().add(1.0D, 0.0D, 0.0D));

            helper.assertTrue(dagger.isReadyAnchorFor(owner),
                    "Anchor Blink ready anchor should match its owner");
            helper.assertFalse(dagger.isReadyAnchorFor(otherPlayer),
                    "Anchor Blink ready anchor should not match another player");
        });
    }

    static void anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "anchor_blink_protection_test");
            var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));

            AnchorBlinkDaggerEntity.grantDamageProtectionForTesting(player, 40);

            var enemyAttack = postLivingAttackEventForGameTest(
                    player,
                    helper.getLevel().damageSources().mobAttack(zombie),
                    4.0F
            );
            helper.assertTrue(enemyAttack.isCanceled(),
                    "Anchor Blink post-teleport protection should block enemy damage");

            var lavaAttack = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 4.0F);
            helper.assertFalse(lavaAttack.isCanceled(),
                    "Anchor Blink post-teleport protection should not block non-enemy environmental damage");
        });
    }

    static void anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "anchor_blink_cross_dimension_cleanup_test");
            var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Anchor Blink cleanup test could not resolve the Nether level");

            AnchorBlinkDaggerEntity.grantDamageProtectionForTesting(player, 40);
            AnchorBlinkDaggerEntity.cleanupExpiredProtection(nether);

            var enemyAttack = postLivingAttackEventForGameTest(
                    player,
                    helper.getLevel().damageSources().mobAttack(zombie),
                    4.0F
            );
            helper.assertTrue(enemyAttack.isCanceled(),
                    "Anchor Blink protection should survive cleanup ticks from another dimension");
        });
    }

    static void anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "anchor_blink_far_impact_test");
            var dagger = new AnchorBlinkDaggerEntity(EntityRegistry.ANCHOR_BLINK_DAGGER.get(), helper.getLevel(), player);
            dagger.setMaximumRange(2.0F);
            helper.getLevel().addFreshEntity(dagger);

            dagger.impactForTesting(player.position().add(3.0D, 0.0D, 0.0D));

            helper.assertTrue(dagger.isRemoved(),
                    "Anchor Blink dagger should be discarded when it impacts beyond maximum range");
            helper.assertFalse(AnchorBlinkDaggerEntity.tryBlink(player),
                    "Anchor Blink should not register a blink anchor after a too-far impact");
        });
    }

    static void anchorBlinkMaximumRangeOnlyChecksAtImpact(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "anchor_blink_impact_range_window_test");
            var dagger = new AnchorBlinkDaggerEntity(EntityRegistry.ANCHOR_BLINK_DAGGER.get(), helper.getLevel(), player);
            dagger.setMaximumRange(3.0F);
            helper.getLevel().addFreshEntity(dagger);

            var impactPosition = player.position().add(2.0D, 0.0D, 0.0D);
            dagger.impactForTesting(impactPosition);
            helper.assertTrue(dagger.isImpacted(),
                    "Anchor Blink dagger should stay anchored when impact occurs within maximum range");

            player.teleportTo(player.getX() + 8.0D, player.getY(), player.getZ());
            helper.assertTrue(AnchorBlinkDaggerEntity.tryBlink(player),
                    "Anchor Blink should allow teleporting after the player moves beyond range during the blink window");
        });
    }

    private static void equipSpellSideEdgePair(net.minecraft.world.entity.player.Player player) {
        var mainhand = ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
        player.setItemInHand(InteractionHand.OFF_HAND, SpellSideEdgeMirror.create(UUID.randomUUID(), mainhand));
    }

    private static boolean isCombatOverhaulLoaded() {
        return ModList.get().isLoaded("bettercombat") || ModList.get().isLoaded("epicfight");
    }

    private static AttributeModifier modifier(
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                name, amount, operation);
    }

    private static ItemStack stackWithSpellPowerModifier(double amount) {
        var stack = new ItemStack(Items.STICK);
        stack.addAttributeModifier(
                AttributeRegistry.SPELL_POWER.get(),
                modifier("stack_spell_power", amount, AttributeModifier.Operation.MULTIPLY_BASE),
                EquipmentSlot.MAINHAND
        );
        return stack;
    }

    private static MagicData resolveMagicData(GameTestHelper helper, net.minecraft.world.entity.player.Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Edge Dancer test could not resolve player magic data");
        return magicData;
    }

    private static EdgeDancer edgeDancer() {
        return (EdgeDancer) SpellRegistry.EDGE_DANCER.get();
    }

    private static AnchorBlink anchorBlink() {
        return (AnchorBlink) SpellRegistry.ANCHOR_BLINK.get();
    }
}
