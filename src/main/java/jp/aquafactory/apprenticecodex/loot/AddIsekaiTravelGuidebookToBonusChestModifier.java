package jp.aquafactory.apprenticecodex.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class AddIsekaiTravelGuidebookToBonusChestModifier extends LootModifier {
    public static final MapCodec<AddIsekaiTravelGuidebookToBonusChestModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                    .apply(instance, AddIsekaiTravelGuidebookToBonusChestModifier::new));

    public AddIsekaiTravelGuidebookToBonusChestModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!isBonusChestLootEnabled()) {
            return generatedLoot;
        }
        if (generatedLoot.stream().noneMatch(stack -> stack.is(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()))) {
            generatedLoot.add(new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    private static boolean isBonusChestLootEnabled() {
        try {
            return ApprenticeCodexServerConfig.enableIsekaiTravelGuidebookBonusChestLoot();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }
}
