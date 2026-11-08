package jp.aquafactory.apprenticecodex.gametest.integration.create;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.validation.SpellDispenserValidationGameTests.createInitializedPresetStack;

public final class CreateDispenserTestSupport {
    private CreateDispenserTestSupport() {}

    static final String CREATE_GAMETEST_HOOKS_CLASS =
            "jp.aquafactory.apprenticecodex.gametest.integration.create.CreateGameTestHooks";

    static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create", "contraption_controlled")
    );

    public static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a simple Magic Missile scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    public static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.MAGE_LIGHT.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected an apprenticecodex spell scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.MAGE_LIGHT.get(),
                    "Spell Dispenser validator resolved the wrong non-Iron's spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    public static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a LONG scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.COMPOUND_PHIAL.get(),
                    "Spell Dispenser validator resolved the wrong LONG spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    public static void spellDispenserValidatorRejectsUnprofiledRayOfSiphoning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAY_OF_SIPHONING_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted Ray of Siphoning");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_PROFILED,
                    "Spell Dispenser validator returned the wrong failure reason for Ray of Siphoning: " + validation.failureReason());
        });
    }

    public static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.LONG_STRIDE.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a CONTINUOUS scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_PROFILED,
                    "Spell Dispenser validator returned the wrong failure reason for profile-less CONTINUOUS scroll: " + validation.failureReason());
        });
    }

    public static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a profiled CONTINUOUS scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong profiled CONTINUOUS spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    public static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.QUICK_ARMS.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a recast spell scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.HAS_RECAST,
                    "Spell Dispenser validator returned the wrong failure reason for recast scroll: " + validation.failureReason());
        });
    }

    public static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var nonScrollStack = createInitializedPresetStack(ItemRegistry.GRIMOIRE_MANIFEST.get());

            helper.assertTrue(ISpellContainer.isSpellContainer(nonScrollStack), "Prepared non-scroll test stack is not a spell container");
            var validation = SpellDispenserSpellValidator.validate(nonScrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a non-scroll spell container");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_SCROLL,
                    "Spell Dispenser validator returned the wrong failure reason for non-scroll spell container: " + validation.failureReason());
        });
    }

    public static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellDispenserStack = new ItemStack(ItemRegistry.SPELL_DISPENSER.get());
            helper.assertTrue(spellDispenserStack.is(CREATE_CONTRAPTION_CONTROLLED),
                    "Spell Dispenser is missing create:contraption_controlled and cannot be selected by Contraption Controls");
        });
    }

    public static void creativeSpellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellDispenserStack = new ItemStack(ItemRegistry.CREATIVE_SPELL_DISPENSER.get());
            helper.assertTrue(spellDispenserStack.is(CREATE_CONTRAPTION_CONTROLLED),
                    "Creative Spell Dispenser is missing create:contraption_controlled and cannot be selected by Contraption Controls");
        });
    }

    public static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_save_test");
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            original.setOwnerProfile(ownerProfile);

            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.load(original.getUpdateTag());

            helper.assertTrue(restored.hasOwnerProfile(), "Spell Dispenser owner profile was not restored from NBT");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getId().equals(restored.getOwnerProfile().getId()),
                    "Spell Dispenser owner UUID changed during NBT round-trip");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getName().equals(restored.getOwnerProfile().getName()),
                    "Spell Dispenser owner name changed during NBT round-trip");
        });
    }

    public static SpellDispenserSpellValidator.ValidationResult createSpellDispenserValidation(ItemStack stack, AbstractSpell spell) {
        return new SpellDispenserSpellValidator.ValidationResult(stack, new SpellData(spell, 1), SpellDispenserSpellValidator.FailureReason.NONE);
    }

    public static GameProfile createSpellDispenserOwnerProfile(String name) {
        return new GameProfile(UUID.randomUUID(), name);
    }

    public static boolean skipWhenCreateMissing(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) {
            return false;
        }

        // optional 依存の absent 環境では Create 専用テストを成功扱いで抜け、通常検証の起動性を優先する。
        helper.succeed();
        return true;
    }

    public static void assertNoSpellDispenserProxy(GameTestHelper helper, BlockPos castPos, ItemStack spellSource, String message) {
        var proxyBox = new AABB(castPos).inflate(3.0D);
        var remainingProxies = helper.getLevel().getEntitiesOfClass(ArmorStand.class, proxyBox, stand ->
                stand.isInvisible() && stand.getMainHandItem().is(spellSource.getItem()));
        helper.assertTrue(remainingProxies.isEmpty(), message + ": " + remainingProxies.size());

        var remainingAnchors = helper.getLevel().getEntitiesOfClass(SpellDispenserAnchorEntity.class, proxyBox);
        helper.assertTrue(remainingAnchors.isEmpty(), message + " (tracked anchors): " + remainingAnchors.size());
    }

    public static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class},
                level, worldPos, mountedInventory, ownerProfile
        );
    }

    public static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile,
            int currentMana,
            ItemStackHandler externalInventory,
            boolean externalAvailableForFuel
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class, int.class, ItemStackHandler.class, boolean.class},
                level, worldPos, mountedInventory, ownerProfile, currentMana, externalInventory, externalAvailableForFuel
        );
    }

    public static void startCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("startMoving", new Class<?>[]{Object.class}, harness);
    }

    public static void tickCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("tick", new Class<?>[]{Object.class}, harness);
    }

    public static void visitCreateSpellDispenserPosition(Object harness, BlockPos pos) {
        invokeCreateGameTestHook("visitNewPosition", new Class<?>[]{Object.class, BlockPos.class}, harness, pos);
    }

    public static void stopCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("stopMoving", new Class<?>[]{Object.class}, harness);
    }

    public static void setCreateSpellDispenserDisabled(Object harness, boolean disabled) {
        invokeCreateGameTestHook("setDisabled", new Class<?>[]{Object.class, boolean.class}, harness, disabled);
    }

    public static boolean hasCreateSpellDispenserContinuousCast(Object harness) {
        return invokeCreateGameTestHookBoolean("hasRunningContinuousCast", harness);
    }

    public static boolean createSpellDispenserRequiresReset(Object harness) {
        return invokeCreateGameTestHookBoolean("requiresContinuousReset", harness);
    }

    public static boolean createSpellDispenserIsCoolingDown(Object harness) {
        return invokeCreateGameTestHookBoolean("isCoolingDown", harness);
    }

    static boolean invokeCreateGameTestHookBoolean(String methodName, Object harness) {
        return (boolean) invokeCreateGameTestHook(methodName, new Class<?>[]{Object.class}, harness);
    }

    static Object invokeCreateGameTestHook(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var hooksClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            return hooksClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest helper call failed: " + methodName, exception);
        }
    }
}
