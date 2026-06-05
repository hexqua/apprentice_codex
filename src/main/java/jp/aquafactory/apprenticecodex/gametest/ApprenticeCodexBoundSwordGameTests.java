package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.BoundSwordItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSword;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexBoundSwordGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final ResourceLocation SUMMON_DAMAGE_TEST_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "bound_sword_summon_damage_test"
    );

    private ApprenticeCodexBoundSwordGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordActivationStoresAndRestoresMainhand(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_restore_test");
        var original = new ItemStack(Items.DIAMOND);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 7.5F);

        var sword = player.getMainHandItem();
        helper.assertTrue(sword.is(ItemRegistry.BOUND_SWORD.get()), "Bound Sword should replace the mainhand item");
        helper.assertTrue(BoundSwordItem.getDisplayDamage(sword) == 7.5F,
                "Bound Sword should keep a snapshot of display damage");
        helper.assertTrue(Math.abs(resolveMainhandAttackDamage(sword) - 6.5D) < 0.0001D,
                "Bound Sword stack attribute modifiers should match the snapshotted display damage");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.hasStoredMainhandStack() && state.getStoredMainhandStack().is(Items.DIAMOND),
                "Bound Sword state should store the replaced mainhand item");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Bound Sword should restore the stored item to the main hand");
        helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Bound Sword state should be inactive after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordWithoutCombatCompatDoesNotGenerateOffhand(GameTestHelper helper) {
        if (BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createBoundSwordTestPlayer(helper, "bound_sword_no_compat_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 7.0F);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item without combat compat");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Bound Sword should not generate an offhand sword without Better Combat or Epic Fight");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCombatCompatGeneratesOffhandWhenEmpty(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createBoundSwordTestPlayer(helper, "bound_sword_compat_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);

        var mainSword = player.getMainHandItem();
        var offhandSword = player.getOffhandItem();
        helper.assertTrue(mainSword.is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item with combat compat");
        helper.assertTrue(offhandSword.is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should generate an offhand sword when the physical offhand slot is empty");
        helper.assertTrue(BoundSwordItem.getInstanceId(mainSword).equals(BoundSwordItem.getInstanceId(offhandSword)),
                "Mainhand and offhand Bound Swords should share the same instance id");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.isOffhandSwordGenerated(),
                "Bound Sword state should record that an offhand sword was generated");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Bound Sword should restore the original mainhand after dual wielding");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Bound Sword should leave an originally empty offhand empty after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCombatCompatKeepsOccupiedOffhandWithoutForce(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createBoundSwordTestPlayer(helper, "bound_sword_occupied_offhand_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item when offhand is occupied");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Bound Sword should not replace an occupied offhand without force dual wield");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertFalse(state.isOffhandSwordGenerated(),
                "Bound Sword state should not mark offhand generation when offhand was occupied");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordForceDualWieldReplacesAndRestoresOffhand(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createBoundSwordTestPlayer(helper, "bound_sword_force_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F,
                true);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Force dual wield should replace the mainhand item");
        helper.assertTrue(player.getOffhandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Force dual wield should replace the occupied offhand item");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.isOffhandSwordGenerated() && state.getStoredOffhandStack().is(Items.SHIELD),
                "Force dual wield should store the replaced offhand item in Bound Sword state");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Force dual wield should restore the original mainhand item");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Force dual wield should restore the replaced offhand item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordDualWieldMovingOneSwordWithinInventoryKeepsEffect(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createBoundSwordTestPlayer(helper, "bound_sword_dual_loss_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);
        var movedOffhandSword = player.getOffhandItem().copy();
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        player.getInventory().items.set(10, movedOffhandSword);

        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Moving one dual-wield Bound Sword inside the player inventory should keep the mainhand sword active");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Moving the generated offhand sword should leave the offhand slot empty");
        helper.assertTrue(player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Moving one dual-wield Bound Sword inside the player inventory should keep the moved sword");
        helper.assertTrue(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Moving one dual-wield Bound Sword inside the player inventory should keep the effect active");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Deactivation should restore the stored mainhand item");
        helper.assertTrue(!player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Deactivation should remove moved generated swords");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordOffhandDoesNotApplyVanillaAttackModifiers(GameTestHelper helper) {
        var stack = BoundSwordItem.create(UUID.randomUUID(), 9.0F, EquipmentSlot.OFFHAND);

        var modifiers = stack.getAttributeModifiers().modifiers();
        helper.assertTrue(modifiers.stream()
                        .noneMatch(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                                && entry.attribute().equals(Attributes.ATTACK_DAMAGE)),
                "Bound Sword offhand should not stack vanilla attack damage on the player");
        helper.assertTrue(modifiers.stream()
                        .noneMatch(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                                && entry.attribute().equals(Attributes.ATTACK_SPEED)),
                "Bound Sword offhand should not stack vanilla attack speed and break combat cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordSnapshotsSummonDamageIntoDisplayDamage(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_summon_damage_test");
        player.getAttribute(AttributeRegistry.SUMMON_DAMAGE).addTransientModifier(new AttributeModifier(
                SUMMON_DAMAGE_TEST_MODIFIER_ID,
                0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));

        boundSword().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, resolveMagicData(helper, player));

        var sword = player.getMainHandItem();
        helper.assertTrue(sword.is(ItemRegistry.BOUND_SWORD.get()), "Bound Sword should be generated by the spell cast");
        helper.assertTrue(Math.abs(BoundSwordItem.getDisplayDamage(sword) - 9.0F) < 0.0001F,
                "Bound Sword display damage should snapshot Summon Damage at cast time");

        var attackDamage = resolveMainhandAttackDamage(sword);
        helper.assertTrue(Math.abs(attackDamage - 8.0D) < 0.0001D,
                "Bound Sword attack damage modifier should be based on the snapshotted display damage");

        BoundSwordManager.deactivate(player, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordGreaterConjurersTalismanSkipsRecastCooldown(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_greater_conjurer_cooldown_test");
        var magicData = resolveMagicData(helper, player);
        equipGreaterConjurersTalisman(player);

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundSword(), 6.0F);
        helper.assertTrue(Math.abs(resolveMainhandAttackDamage(player.getMainHandItem()) - 5.0D) < 0.0001D,
                "Greater Conjurer's Talisman should not collapse Bound Sword attack damage");
        var recast = magicData.getPlayerRecasts().getRecastInstance(boundSword().getSpellId());
        helper.assertTrue(recast != null, "Bound Sword should create an active recast");

        magicData.getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);

        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(boundSword()),
                "Greater Conjurer's Talisman should suppress Bound Sword cooldown when the recast ends");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordGreaterConjurersTalismanSkipsManualRecastCooldown(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_greater_conjurer_manual_recast_test");
        var magicData = resolveMagicData(helper, player);
        equipGreaterConjurersTalisman(player);

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundSword(), 6.0F);
        boundSword().castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);

        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(boundSword()),
                "Manual Bound Sword recast should remove the active recast");
        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(boundSword()),
                "Greater Conjurer's Talisman should suppress Bound Sword cooldown after manual recast deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCanMoveWithinInventoryAndCursor(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_invalid_move_test");
        var original = new ItemStack(Items.EMERALD);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 6.0F);
        var sword = player.getMainHandItem().copy();
        player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
        player.getInventory().items.set(10, sword);

        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Moving Bound Sword inside the player inventory should keep it active");
        helper.assertTrue(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Bound Sword state should stay active while the generated sword remains in player inventory");

        var carriedSword = player.getInventory().items.get(10).copy();
        player.getInventory().items.set(10, ItemStack.EMPTY);
        player.containerMenu.setCarried(carriedSword);
        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.containerMenu.getCarried().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should stay active while held by the cursor");
        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(),
                "Deactivation should remove a cursor-held Bound Sword");
        helper.assertTrue(player.getMainHandItem().is(Items.EMERALD),
                "Deactivation should restore the stored item");
        helper.succeed();
    }

    private static FakePlayer createBoundSwordTestPlayer(GameTestHelper helper, String name) {
        return new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    private static MagicData resolveMagicData(GameTestHelper helper, FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Bound Sword test could not resolve player magic data");
        return magicData;
    }

    private static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Bound Sword Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    private static BoundSword boundSword() {
        return (BoundSword) SpellRegistry.BOUND_SWORD.get();
    }

    private static double resolveMainhandAttackDamage(ItemStack stack) {
        return stack.getAttributeModifiers().modifiers()
                .stream()
                .filter(entry -> entry.slot().equals(EquipmentSlotGroup.MAINHAND)
                        && entry.attribute().equals(Attributes.ATTACK_DAMAGE))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }
}
