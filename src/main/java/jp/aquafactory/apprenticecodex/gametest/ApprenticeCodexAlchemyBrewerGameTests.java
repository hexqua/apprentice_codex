package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexAlchemyBrewerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private ApprenticeCodexAlchemyBrewerGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void initialRecipesAreLoaded(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        helper.assertValueEqual(manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get()).size(), 12,
                "dedicated brewing recipe count");
        helper.assertValueEqual(manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get()).size(), 8,
                "potion modifier recipe count");
        helper.assertTrue(new ItemStack(Items.NETHER_WART).is(TagRegistry.Items.ALCHEMY_BREWER_HIGH_EFFICIENCY_BASES),
                "Nether Wart must be included in the high-efficiency base tag");
        helper.assertTrue(new ItemStack(Items.GLOW_LICHEN).is(TagRegistry.Items.ALCHEMY_BREWER_FAST_BASES),
                "Glow Lichen must be included in the fast base tag");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void collisionShapeKeepsAFlatTenPixelWalkingSurface(GameTestHelper helper) {
        var state = BlockRegistry.ALCHEMY_BREWER.get().defaultBlockState();
        var worldPos = helper.absolutePos(new BlockPos(1, 1, 1));
        var collision = state.getCollisionShape(helper.getLevel(), worldPos, CollisionContext.empty());
        var outline = state.getShape(helper.getLevel(), worldPos, CollisionContext.empty());

        helper.assertValueEqual(collision.max(Direction.Axis.Y), 10.0D / 16.0D, "walking surface height");
        helper.assertTrue(outline.max(Direction.Axis.Y) > collision.max(Direction.Axis.Y),
                "Outline shape must include the detailed upper geometry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void previewIsCalculatedWhileAutoBrewingIsOff(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.ALCHEMY_BREWER.get());
        var brewer = (AlchemyBrewerBlockEntity) helper.getBlockEntity(pos);
        brewer.getInventory().setStackInSlot(1, new ItemStack(Items.GLOW_LICHEN));
        brewer.getInventory().setStackInSlot(2, new ItemStack(Items.SUGAR));

        helper.runAfterDelay(2, () -> {
            helper.assertFalse(brewer.isAutoBrewing(), "Auto brewing must be disabled when placed");
            helper.assertFalse(brewer.isProcessing(), "Processing must not begin while auto brewing is disabled");
            helper.assertTrue(brewer.isDisplayPreview(), "Result preview must be visible while auto brewing is disabled");
            helper.assertValueEqual(brewer.getDisplayPotionId(), ResourceLocation.parse("minecraft:swiftness"),
                    "preview potion");
            helper.assertValueEqual(brewer.getDisplayAmountMb(), 750, "preview amount");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 280)
    public static void autoModeReservesAndProducesModifiedPotion(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.ALCHEMY_BREWER.get());
        var brewer = (AlchemyBrewerBlockEntity) helper.getBlockEntity(pos);
        brewer.getInventory().setStackInSlot(1, new ItemStack(Items.GLOWSTONE_DUST));
        brewer.getInventory().setStackInSlot(2, new ItemStack(Items.NETHER_WART));
        brewer.getInventory().setStackInSlot(3, new ItemStack(Items.SUGAR));

        helper.runAfterDelay(25, () -> helper.assertFalse(brewer.isProcessing(), "Auto brewing must be disabled initially"));
        helper.runAfterDelay(26, brewer::toggleAutoBrewing);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(brewer.isProcessing(), "Processing must begin after the preview remains valid for 20 ticks");
            helper.assertTrue(brewer.getInventory().getStackInSlot(1).isEmpty(), "Modifier must be reserved and consumed");
            helper.assertTrue(brewer.getInventory().getStackInSlot(2).isEmpty(), "Base ingredient must be reserved and consumed");
            helper.assertTrue(brewer.getInventory().getStackInSlot(3).isEmpty(), "Potion ingredient must be reserved and consumed");
        });
        helper.runAfterDelay(250, () -> {
            helper.assertFalse(brewer.isProcessing(), "Processing must stop after completion");
            helper.assertValueEqual(brewer.getTankAmountMb(), 1000, "Nether Wart output amount");
            helper.assertValueEqual(brewer.getTankPotionId(), ResourceLocation.parse("minecraft:strong_swiftness"), "brewed potion");
            helper.succeed();
        });
    }
}
