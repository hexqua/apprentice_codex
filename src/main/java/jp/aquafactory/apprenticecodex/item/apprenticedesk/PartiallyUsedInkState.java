package jp.aquafactory.apprenticecodex.item.apprenticedesk;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 使いかけインクの永続状態を扱う。
 *
 * <p>1.20.1 では NBT を使うが、メニュー側へ保存形式を漏らさず、
 * 1.21.1 で Data Component へ置き換える範囲をこのクラスへ限定する。</p>
 */
public final class PartiallyUsedInkState {
    private static final String ROOT_TAG = "ApprenticeDeskInk";
    private static final String SOURCE_INK_TAG = "SourceInk";
    private static final String REMAINING_USES_TAG = "RemainingUses";
    private static final String CAPACITY_TAG = "Capacity";

    private PartiallyUsedInkState() {
    }

    public static ItemStack create(OfficialInk source, int capacity) {
        return create(source, capacity, capacity);
    }

    public static ItemStack create(OfficialInk source, int capacity, int remainingUses) {
        if (capacity <= 0 || remainingUses <= 0 || remainingUses > capacity) {
            throw new IllegalArgumentException("Partially used ink requires 0 < remainingUses <= capacity");
        }

        var stack = new ItemStack(ItemRegistry.PARTIALLY_USED_INK.get());
        write(stack, source, capacity, remainingUses);
        return stack;
    }

    public static Optional<ValidState> readValid(ItemStack stack) {
        if (!(stack.getItem() instanceof PartiallyUsedInkItem)) {
            return Optional.empty();
        }

        var source = readSource(stack);
        if (source == null) {
            return Optional.empty();
        }

        var root = getRootTag(stack);
        if (root == null
                || !root.contains(REMAINING_USES_TAG, CompoundTag.TAG_INT)
                || !root.contains(CAPACITY_TAG, CompoundTag.TAG_INT)) {
            return Optional.empty();
        }

        var remainingUses = root.getInt(REMAINING_USES_TAG);
        var capacity = root.getInt(CAPACITY_TAG);
        if (capacity <= 0 || remainingUses <= 0 || remainingUses > capacity) {
            return Optional.empty();
        }
        return Optional.of(new ValidState(source, remainingUses, capacity));
    }

    public static Optional<OfficialInk> readSourceOnly(ItemStack stack) {
        return Optional.ofNullable(readSource(stack));
    }

    public static ItemStack consumeOriginal(OfficialInk source, int capacity, boolean returnGlassBottle) {
        if (capacity <= 1) {
            return depletedResult(returnGlassBottle);
        }
        return create(source, capacity, capacity - 1);
    }

    public static ItemStack consumePartiallyUsed(ItemStack stack, boolean returnGlassBottle) {
        var state = readValid(stack).orElse(null);
        if (state == null) {
            return stack;
        }
        if (state.remainingUses() <= 1) {
            return depletedResult(returnGlassBottle);
        }

        var updated = stack.copy();
        updated.setCount(1);
        write(updated, state.source(), state.capacity(), state.remainingUses() - 1);
        return updated;
    }

    public static float getModelProperty(ItemStack stack) {
        return readSourceOnly(stack)
                .map(OfficialInk::modelProperty)
                .orElse(OfficialInk.COMMON.modelProperty());
    }

    private static ItemStack depletedResult(boolean returnGlassBottle) {
        return returnGlassBottle ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY;
    }

    private static void write(ItemStack stack, OfficialInk source, int capacity, int remainingUses) {
        var root = new CompoundTag();
        root.putString(SOURCE_INK_TAG, source.id().toString());
        root.putInt(REMAINING_USES_TAG, remainingUses);
        root.putInt(CAPACITY_TAG, capacity);
        stack.getOrCreateTag().put(ROOT_TAG, root);
    }

    private static @Nullable OfficialInk readSource(ItemStack stack) {
        var root = getRootTag(stack);
        if (root == null || !root.contains(SOURCE_INK_TAG, CompoundTag.TAG_STRING)) {
            return null;
        }

        var id = ResourceLocation.tryParse(root.getString(SOURCE_INK_TAG));
        return id == null ? null : OfficialInk.fromId(id);
    }

    private static @Nullable CompoundTag getRootTag(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(ROOT_TAG, CompoundTag.TAG_COMPOUND)
                ? tag.getCompound(ROOT_TAG)
                : null;
    }

    public record ValidState(OfficialInk source, int remainingUses, int capacity) {
    }

    public enum OfficialInk {
        COMMON(
                io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON,
                SpellRarity.COMMON,
                5,
                0.0F
        ),
        UNCOMMON(
                io.redspace.ironsspellbooks.registries.ItemRegistry.INK_UNCOMMON,
                SpellRarity.UNCOMMON,
                4,
                1.0F
        ),
        RARE(
                io.redspace.ironsspellbooks.registries.ItemRegistry.INK_RARE,
                SpellRarity.RARE,
                3,
                2.0F
        ),
        EPIC(
                io.redspace.ironsspellbooks.registries.ItemRegistry.INK_EPIC,
                SpellRarity.EPIC,
                3,
                3.0F
        ),
        LEGENDARY(
                io.redspace.ironsspellbooks.registries.ItemRegistry.INK_LEGENDARY,
                SpellRarity.LEGENDARY,
                2,
                4.0F
        );

        private final Supplier<? extends Item> item;
        private final SpellRarity rarity;
        private final int creativeDefaultCapacity;
        private final float modelProperty;

        OfficialInk(
                Supplier<? extends Item> item,
                SpellRarity rarity,
                int creativeDefaultCapacity,
                float modelProperty
        ) {
            this.item = item;
            this.rarity = rarity;
            this.creativeDefaultCapacity = creativeDefaultCapacity;
            this.modelProperty = modelProperty;
        }

        public Item item() {
            return item.get();
        }

        public ResourceLocation id() {
            return BuiltInRegistries.ITEM.getKey(item());
        }

        public SpellRarity rarity() {
            return rarity;
        }

        public int creativeDefaultCapacity() {
            return creativeDefaultCapacity;
        }

        public float modelProperty() {
            return modelProperty;
        }

        public static @Nullable OfficialInk fromOriginal(ItemStack stack) {
            return Arrays.stream(values())
                    .filter(source -> stack.is(source.item()))
                    .findFirst()
                    .orElse(null);
        }

        private static @Nullable OfficialInk fromId(ResourceLocation id) {
            return Arrays.stream(values())
                    .filter(source -> source.id().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }
}
