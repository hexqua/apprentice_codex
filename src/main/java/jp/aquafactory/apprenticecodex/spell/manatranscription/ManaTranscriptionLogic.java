package jp.aquafactory.apprenticecodex.spell.manatranscription;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

final class ManaTranscriptionLogic {
    private ManaTranscriptionLogic() {
    }

    static Resolution resolve(Player player) {
        var target = player.getMainHandItem();
        var operationItem = physicalOffhandItem(player);
        var enchantments = EnchantmentHelper.getEnchantments(target);
        int repairCost = target.getBaseRepairCost();
        var hasEnchantment = enchantments.entrySet().stream().anyMatch(entry -> entry.getValue() > 0);

        if (target.isEmpty() || !hasEnchantment && repairCost <= 0) {
            return Resolution.failure(Failure.INVALID_TARGET);
        }
        if (target.getCount() != 1) {
            return Resolution.failure(Failure.TARGET_MUST_BE_SINGLE);
        }

        // 本と羽根ペンを媒体タグへ追加されても、抽出モードの意味を変化させない。
        if (operationItem.is(Items.WRITABLE_BOOK)) {
            return resolveExtraction(player, target, operationItem, repairCost);
        }
        if (operationItem.is(TagRegistry.Items.MANA_TRANSCRIPTION_REPAIR_COST_RESET_ITEMS)) {
            if (repairCost <= 0) {
                return Resolution.failure(Failure.NO_WORK_COUNT);
            }
            return Resolution.success(Mode.RESET, List.of(), chargedExperience(player, repairCost));
        }
        return Resolution.failure(Failure.REQUIRED_OPERATION_ITEM);
    }

    private static Resolution resolveExtraction(Player player, ItemStack target, ItemStack operationItem, int repairCost) {
        var enchantments = EnchantmentHelper.getEnchantments(target);
        var allCandidates = enchantments.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new Candidate(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getKey().isCurse(),
                        saturatedMultiply(entry.getValue(), anvilCost(entry.getKey()))
                ))
                .toList();
        if (target.is(Items.ENCHANTED_BOOK) && allCandidates.size() == 1) {
            return Resolution.failure(Failure.REJECT_SINGLE_ENCHANTMENT_BOOK);
        }
        if (allCandidates.isEmpty()) {
            return Resolution.failure(Failure.NO_ENCHANTMENT);
        }
        if (!isBlankWritableBook(operationItem)) {
            return Resolution.failure(Failure.REQUIRED_EMPTY_BOOK);
        }

        var candidates = selectHighestPriorityEnchantments(allCandidates);
        if (candidates.isEmpty()) {
            return Resolution.failure(Failure.NO_ENCHANTMENT);
        }

        var representative = candidates.get(0);
        var enchantmentCost = representative.curse()
                ? saturatedMultiply(representative.weightedLevel(), 4)
                : representative.weightedLevel();
        var requiredLevel = saturatedAdd(enchantmentCost, repairCost);
        return Resolution.success(Mode.EXTRACTION, candidates, chargedExperience(player, requiredLevel));
    }

    private static List<Candidate> selectHighestPriorityEnchantments(List<Candidate> allCandidates) {
        var hasCurse = allCandidates.stream().anyMatch(Candidate::curse);
        var highestWeight = allCandidates.stream()
                .filter(candidate -> !hasCurse || candidate.curse())
                .mapToInt(Candidate::weightedLevel)
                .max()
                .orElse(0);
        return allCandidates.stream()
                .filter(candidate -> !hasCurse || candidate.curse())
                .filter(candidate -> candidate.weightedLevel() == highestWeight)
                .toList();
    }

    static boolean isBlankWritableBook(ItemStack stack) {
        if (!stack.hasTag()) {
            return true;
        }

        var pages = stack.getOrCreateTag().getList("pages", Tag.TAG_STRING);
        for (var index = 0; index < pages.size(); ++index) {
            if (!pages.getString(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int anvilCost(Enchantment enchantment) {
        return switch (enchantment.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    static ItemStack physicalOffhandItem(Player player) {
        // Better Combat は Two-Handed 武器の使用中に論理オフハンドを空へ差し替えるため、媒体は実スロットから取得する。
        return HandStackResolver.resolve(
                player,
                InteractionHand.OFF_HAND,
                HandStackResolver.OffhandResolution.PHYSICAL
        );
    }

    static int currentExperience(Player player) {
        var base = experienceAtStartOfLevel(Math.max(0, player.experienceLevel));
        var progress = Math.round(Mth.clamp(player.experienceProgress, 0.0F, 1.0F)
                * vanillaXpNeededForNextLevel(Math.max(0, player.experienceLevel)));
        return saturatedAdd(base, progress);
    }

    static int experienceAtStartOfLevel(int level) {
        var safeLevel = Math.max(0L, level);
        long experience;
        if (safeLevel <= 16L) {
            experience = safeLevel * safeLevel + 6L * safeLevel;
        } else if (safeLevel <= 31L) {
            experience = (5L * safeLevel * safeLevel - 81L * safeLevel + 720L) / 2L;
        } else {
            if (safeLevel >= 21_864L) {
                return Integer.MAX_VALUE;
            }
            experience = (9L * safeLevel * safeLevel - 325L * safeLevel + 4440L) / 2L;
        }
        return (int) Math.min(experience, Integer.MAX_VALUE);
    }

    private static int vanillaXpNeededForNextLevel(int level) {
        if (level >= 30) {
            return saturatedAdd(112, saturatedMultiply(level - 30, 9));
        }
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    private static int chargedExperience(Player player, int requiredLevel) {
        return player.isCreative() ? 0 : experienceAtStartOfLevel(requiredLevel);
    }

    private static int saturatedAdd(int first, int second) {
        return (int) Math.min((long) first + Math.max(0L, second), Integer.MAX_VALUE);
    }

    private static int saturatedMultiply(int first, int second) {
        return (int) Math.min((long) Math.max(0, first) * Math.max(0, second), Integer.MAX_VALUE);
    }

    static List<Item> effectiveResetItems() {
        var result = new ArrayList<Item>();
        BuiltInRegistries.ITEM.getTag(TagRegistry.Items.MANA_TRANSCRIPTION_REPAIR_COST_RESET_ITEMS)
                .ifPresent(items -> items.forEach(holder -> {
                    if (holder.value() != Items.WRITABLE_BOOK) {
                        result.add(holder.value());
                    }
                }));
        return List.copyOf(result);
    }

    enum Mode {
        EXTRACTION,
        RESET
    }

    enum Failure {
        NONE,
        INVALID_TARGET,
        TARGET_MUST_BE_SINGLE,
        REQUIRED_OPERATION_ITEM,
        REJECT_SINGLE_ENCHANTMENT_BOOK,
        NO_ENCHANTMENT,
        REQUIRED_EMPTY_BOOK,
        NO_WORK_COUNT
    }

    record Candidate(Enchantment enchantment, int level, boolean curse, int weightedLevel) {
    }

    record Resolution(boolean success, Mode mode, List<Candidate> candidates, int requiredExperience, Failure failure) {
        static Resolution success(Mode mode, List<Candidate> candidates, int requiredExperience) {
            return new Resolution(true, mode, List.copyOf(candidates), requiredExperience, Failure.NONE);
        }

        static Resolution failure(Failure failure) {
            return new Resolution(false, Mode.EXTRACTION, List.of(), 0, failure);
        }
    }
}
