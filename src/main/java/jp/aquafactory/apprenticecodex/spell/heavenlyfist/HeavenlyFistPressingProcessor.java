package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.create.CreateExposedItemProcessingBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.ItemStackProcessingResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class HeavenlyFistPressingProcessor {
    private static final String CREATE_MOD_ID = "create";
    private static final ResourceLocation CREATE_PRESSING_RECIPE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "pressing");
    private static boolean hasLoggedCreateReflectionFailure;

    private HeavenlyFistPressingProcessor() {
    }

    static boolean canProcessItems() {
        return ModList.get().isLoaded(CREATE_MOD_ID);
    }

    static void processItems(ServerLevel level, Vec3 center, int maxProcessOperations) {
        if (maxProcessOperations <= 0 || !canProcessItems()) {
            return;
        }

        var processTargets = sampleCreateProcessTargets(center);
        var processed = CreateExposedItemProcessingBridge.processBasins(
                level,
                processTargets,
                maxProcessOperations
        );
        if (processed >= maxProcessOperations) {
            return;
        }

        var items = new ArrayList<>(level.getEntitiesOfClass(
                ItemEntity.class,
                createProcessItemArea(center),
                item -> item.isAlive() && !item.getItem().isEmpty()
        ));
        if (items.size() > 1) {
            items.sort(Comparator.comparingDouble(item -> item.position().distanceToSqr(center)));
        }

        var skipIds = new ArrayList<UUID>();
        var skipTransportedItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var item : items) {
            if (processed >= maxProcessOperations) {
                break;
            }
            if (!item.isAlive() || skipIds.contains(item.getUUID())) {
                continue;
            }

            processed += tryProcessItem(level, item, maxProcessOperations - processed, skipIds);
        }

        if (processed < maxProcessOperations) {
            CreateExposedItemProcessingBridge.processBlocks(
                    level,
                    processTargets,
                    maxProcessOperations - processed,
                    skipTransportedItems,
                    (inputStack, remainingBudget) -> tryBuildPressingResult(level, inputStack, remainingBudget)
            );
        }
    }

    private static List<BlockPos> sampleCreateProcessTargets(Vec3 center) {
        var centerPos = BlockPos.containing(center);
        var positions = new ArrayList<BlockPos>(18);
        for (var yOffset = 0; yOffset >= -1; yOffset--) {
            for (var xOffset = -1; xOffset <= 1; xOffset++) {
                for (var zOffset = -1; zOffset <= 1; zOffset++) {
                    positions.add(centerPos.offset(xOffset, yOffset, zOffset));
                }
            }
        }
        return positions;
    }

    private static AABB createProcessItemArea(Vec3 center) {
        var centerPos = BlockPos.containing(center);
        // Create 加工は戦闘半径ではなく、拳の直下付近の 3x2x3 に限定する。
        return new AABB(
                centerPos.getX() - 1.0D,
                centerPos.getY() - 1.0D,
                centerPos.getZ() - 1.0D,
                centerPos.getX() + 2.0D,
                centerPos.getY() + 1.0D,
                centerPos.getZ() + 2.0D
        );
    }

    private static int tryProcessItem(ServerLevel level, ItemEntity itemEntity, int maxProcessCount, List<UUID> skipIds) {
        if (maxProcessCount <= 0) {
            return 0;
        }

        var inputStack = itemEntity.getItem();
        if (inputStack.isEmpty() || inputStack.getCount() <= 0) {
            return 0;
        }

        var processCount = Math.min(maxProcessCount, inputStack.getCount());
        var processingResult = tryBuildPressingResult(level, inputStack, processCount);
        processingResult.ifPresent(result -> applyProcessingResult(
                level,
                itemEntity,
                result.outputStacks(),
                result.processedCount(),
                skipIds
        ));
        return processingResult.map(ItemStackProcessingResult::processedCount).orElse(0);
    }

    private static Optional<ItemStackProcessingResult> tryBuildPressingResult(ServerLevel level, ItemStack inputStack, int maxProcessCount) {
        if (maxProcessCount <= 0 || inputStack.isEmpty() || inputStack.getCount() <= 0) {
            return Optional.empty();
        }

        var processCount = Math.min(maxProcessCount, inputStack.getCount());
        var createRecipe = findCreatePressingRecipe(level, inputStack);
        if (createRecipe.isPresent()) {
            var outputs = rollCreatePressingOutputs(level, createRecipe.get(), processCount);
            if (outputs.isPresent()) {
                return Optional.of(new ItemStackProcessingResult(processCount, normalizeOutputStacks(outputs.get())));
            }
            logCreateReflectionFailureOnce(createRecipe.get().getId());
        }

        return Optional.empty();
    }

    private static Optional<Recipe<?>> findCreatePressingRecipe(ServerLevel level, ItemStack inputStack) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            return Optional.empty();
        }

        var type = ForgeRegistries.RECIPE_TYPES.getValue(CREATE_PRESSING_RECIPE_TYPE_ID);
        if (type == null) {
            return Optional.empty();
        }

        return level.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getType() == type)
                .filter(recipe -> !ApprenticeCodexServerConfig.isHeavenlyFistCreateRecipeDenied(recipe.getId()))
                .filter(recipe -> matchesFirstIngredient(recipe, inputStack))
                .findFirst();
    }

    private static boolean matchesFirstIngredient(Recipe<?> recipe, ItemStack inputStack) {
        var ingredients = recipe.getIngredients();
        return !ingredients.isEmpty() && ingredients.get(0).test(inputStack);
    }

    private static Optional<List<ItemStack>> rollCreatePressingOutputs(ServerLevel level, Recipe<?> recipe, int processCount) {
        var outputs = new ArrayList<ItemStack>();
        for (var i = 0; i < processCount; i++) {
            var rolledPerInput = rollCreateOutputsPerInput(level, recipe);
            if (rolledPerInput.isEmpty()) {
                return Optional.empty();
            }
            outputs.addAll(rolledPerInput.get());
        }
        return Optional.of(outputs);
    }

    private static Optional<List<ItemStack>> rollCreateOutputsPerInput(ServerLevel level, Recipe<?> recipe) {
        var rolledByRecipe = invokeCreateRecipeRollResults(level, recipe);
        if (rolledByRecipe.isPresent()) {
            return rolledByRecipe;
        }

        Object rawRollableResults;
        try {
            rawRollableResults = recipe.getClass().getMethod("getRollableResults").invoke(recipe);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }

        if (!(rawRollableResults instanceof List<?> rollableResults)) {
            return Optional.empty();
        }

        var rolled = new ArrayList<ItemStack>();
        for (var output : rollableResults) {
            var rolledStack = rollCreateProcessingOutput(level, output);
            if (rolledStack == null) {
                return Optional.empty();
            }
            if (!rolledStack.isEmpty() && rolledStack.getCount() > 0) {
                rolled.add(rolledStack);
            }
        }
        return Optional.of(rolled);
    }

    private static Optional<List<ItemStack>> invokeCreateRecipeRollResults(ServerLevel level, Recipe<?> recipe) {
        try {
            var method = recipe.getClass().getMethod("rollResults");
            var rolled = copyItemStacks(method.invoke(recipe));
            return rolled == null ? Optional.empty() : Optional.of(rolled);
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }

        try {
            var method = recipe.getClass().getMethod("rollResults", net.minecraft.util.RandomSource.class);
            var rolled = copyItemStacks(method.invoke(recipe, level.random));
            return rolled == null ? Optional.empty() : Optional.of(rolled);
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static @Nullable ItemStack rollCreateProcessingOutput(ServerLevel level, Object output) {
        try {
            var method = output.getClass().getMethod("rollOutput");
            return copyItemStack(method.invoke(output));
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        try {
            var method = output.getClass().getMethod("rollOutput", net.minecraft.util.RandomSource.class);
            return copyItemStack(method.invoke(output, level.random));
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        try {
            var getStackMethod = output.getClass().getMethod("getStack");
            var getChanceMethod = output.getClass().getMethod("getChance");
            var stackValue = getStackMethod.invoke(output);
            var chanceValue = getChanceMethod.invoke(output);
            if (!(stackValue instanceof ItemStack stack) || stack.isEmpty() || stack.getCount() <= 0) {
                return ItemStack.EMPTY;
            }

            var chance = chanceValue instanceof Number number ? number.floatValue() : 0.0F;
            if (chance >= 1.0F || level.random.nextFloat() < chance) {
                return stack.copy();
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        return ItemStack.EMPTY;
    }

    private static void applyProcessingResult(ServerLevel level, ItemEntity sourceItem, List<ItemStack> outputStacks,
                                              int processCount, List<UUID> skipIds) {
        var sourceStack = sourceItem.getItem();
        if (sourceStack.isEmpty() || sourceStack.getCount() <= 0 || processCount <= 0) {
            return;
        }

        var normalizedOutputStacks = normalizeOutputStacks(outputStacks);
        var sourcePosition = sourceItem.position();
        var sourceVelocity = sourceItem.getDeltaMovement();
        var remainingInputCount = sourceStack.getCount() - Math.min(processCount, sourceStack.getCount());

        if (remainingInputCount <= 0) {
            if (normalizedOutputStacks.isEmpty()) {
                sourceItem.discard();
                return;
            }

            var firstOutput = normalizedOutputStacks.remove(0);
            sourceItem.setItem(firstOutput);
            skipIds.add(sourceItem.getUUID());
        } else {
            var remain = sourceStack.copy();
            remain.setCount(remainingInputCount);
            sourceItem.setItem(remain);
        }

        for (var outputStack : normalizedOutputStacks) {
            spawnProcessedOutput(level, sourcePosition, sourceVelocity, outputStack, skipIds);
        }

        playItemProcessedEffects(level, sourcePosition, processCount);
    }

    private static List<ItemStack> normalizeOutputStacks(List<ItemStack> outputStacks) {
        var normalized = new ArrayList<ItemStack>();
        for (var outputStack : outputStacks) {
            if (outputStack.isEmpty() || outputStack.getCount() <= 0) {
                continue;
            }
            normalized.addAll(splitOutputStacks(outputStack, outputStack.getCount()));
        }
        return normalized;
    }

    private static List<ItemStack> splitOutputStacks(ItemStack outputPrototype, int totalCount) {
        var stacks = new ArrayList<ItemStack>();
        var maxStackSize = Math.max(1, outputPrototype.getMaxStackSize());
        var remaining = totalCount;
        while (remaining > 0) {
            var stackCount = Math.min(maxStackSize, remaining);
            var split = outputPrototype.copy();
            split.setCount(stackCount);
            stacks.add(split);
            remaining -= stackCount;
        }
        return stacks;
    }

    private static void spawnProcessedOutput(ServerLevel level, Vec3 sourcePosition, Vec3 sourceVelocity,
                                             ItemStack outputStack, List<UUID> skipIds) {
        if (outputStack.isEmpty()) {
            return;
        }

        var spawned = new ItemEntity(level, sourcePosition.x, sourcePosition.y, sourcePosition.z, outputStack);
        spawned.setDeltaMovement(sourceVelocity);
        level.addFreshEntity(spawned);
        skipIds.add(spawned.getUUID());
    }

    private static void playItemProcessedEffects(ServerLevel level, Vec3 sourcePosition, int processCount) {
        var particleCount = Mth.clamp(4 + processCount * 2, 6, 24);
        AudioTools.playSoundFromPosition(level, sourcePosition, SoundRegistry.WHEEL_PROCESS.get(), SoundSource.NEUTRAL, 0.6F, 1.0F, 0.15F);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CRIT,
                sourcePosition.x,
                sourcePosition.y + 0.1D,
                sourcePosition.z,
                particleCount,
                0.1D,
                0.05D,
                0.1D,
                0.01D
        );
    }

    private static @Nullable List<ItemStack> copyItemStacks(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return null;
        }

        var copied = new ArrayList<ItemStack>();
        for (var element : rawList) {
            var stack = copyItemStack(element);
            if (!stack.isEmpty() && stack.getCount() > 0) {
                copied.add(stack);
            }
        }
        return copied;
    }

    private static ItemStack copyItemStack(Object rawValue) {
        if (!(rawValue instanceof ItemStack stack) || stack.isEmpty() || stack.getCount() <= 0) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }

    private static void logCreateReflectionFailureOnce(ResourceLocation recipeId) {
        if (hasLoggedCreateReflectionFailure) {
            return;
        }

        hasLoggedCreateReflectionFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Create pressing integration fallback failed for recipe {}. HeavenlyFist will skip that Create recipe.",
                recipeId
        );
    }
}
