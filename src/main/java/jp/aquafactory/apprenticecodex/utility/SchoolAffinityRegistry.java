package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotionVariant;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SchoolAffinityRegistry {
    private static final List<ResourceLocation> BUILTIN_IRONS_SCHOOL_IDS = List.of(
            SchoolRegistry.FIRE_RESOURCE,
            SchoolRegistry.ICE_RESOURCE,
            SchoolRegistry.LIGHTNING_RESOURCE,
            SchoolRegistry.HOLY_RESOURCE,
            SchoolRegistry.ENDER_RESOURCE,
            SchoolRegistry.BLOOD_RESOURCE,
            SchoolRegistry.EVOCATION_RESOURCE,
            SchoolRegistry.NATURE_RESOURCE,
            SchoolRegistry.ELDRITCH_RESOURCE
    );
    private static final int BUILTIN_AFFINITY_SLOT_COUNT = BUILTIN_IRONS_SCHOOL_IDS.size();
    private static final int EXTRA_AFFINITY_SLOT_COUNT = 16;
    public static final int MAX_AFFINITY_SLOTS = BUILTIN_AFFINITY_SLOT_COUNT + EXTRA_AFFINITY_SLOT_COUNT;

    private static final List<SchoolAffinityDefinition> DEFINITIONS = new ArrayList<>();
    private static final Map<MobEffect, SchoolAffinityDefinition> DEFINITION_BY_EFFECT = new IdentityHashMap<>();
    private static final Map<Potion, SchoolAffinityDefinition> DEFINITION_BY_POTION = new IdentityHashMap<>();
    private static final SchoolType[] ASSIGNED_SCHOOLS = new SchoolType[MAX_AFFINITY_SLOTS];
    private static final ResourceLocation[] SYNCED_SCHOOL_IDS = new ResourceLocation[MAX_AFFINITY_SLOTS];
    private static final Map<ResourceLocation, SchoolAffinityDefinition> DEFINITION_BY_SCHOOL_ID = new LinkedHashMap<>();
    private static Map<ResourceLocation, Integer> syncedCatalystSlotsByItemId = Map.of();

    @Nullable
    private static Map<Item, SchoolAffinityDefinition> catalystDefinitionByItem;

    private static boolean bindingsResolved;
    private static boolean hasSyncedAssignments;
    private static boolean warnedExtraSchoolCount;

    private SchoolAffinityRegistry() {
    }

    public static synchronized List<SchoolAffinityDefinition> getDefinitions() {
        if (!DEFINITIONS.isEmpty()) {
            return List.copyOf(DEFINITIONS);
        }

        for (var slotIndex = 0; slotIndex < MAX_AFFINITY_SLOTS; slotIndex++) {
            var suffix = String.format("%02d", slotIndex);
            var effect = new SchoolAffinityEffect(slotIndex);
            var basePotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.BASE, effect);
            var longPotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.LONG, effect);
            var strongPotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.STRONG, effect);

            var definition = new SchoolAffinityDefinition(
                    slotIndex,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "affinity_" + suffix),
                    effect,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "affinity_" + suffix),
                    basePotion,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "long_affinity_" + suffix),
                    longPotion,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "strong_affinity_" + suffix),
                    strongPotion
            );
            DEFINITIONS.add(definition);
            DEFINITION_BY_EFFECT.put(effect, definition);
            DEFINITION_BY_POTION.put(basePotion, definition);
            DEFINITION_BY_POTION.put(longPotion, definition);
            DEFINITION_BY_POTION.put(strongPotion, definition);
        }

        return List.copyOf(DEFINITIONS);
    }

    public static Optional<SchoolAffinityDefinition> findByEffect(MobEffect effect) {
        ensureBindingsResolved();

        var definition = DEFINITION_BY_EFFECT.get(effect);
        if (definition == null || getAssignedSchool(definition.slotIndex()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(definition);
    }

    public static Optional<SchoolAffinityDefinition> findByPotion(Potion potion) {
        ensureBindingsResolved();

        var definition = DEFINITION_BY_POTION.get(potion);
        if (definition == null || getAssignedSchool(definition.slotIndex()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(definition);
    }

    public static Optional<SchoolAffinityDefinition> findByCatalyst(ItemStack ingredient) {
        ensureBindingsResolved();

        if (ingredient.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(getCatalystDefinitionByItem().get(ingredient.getItem()));
    }

    public static synchronized Map<Item, SchoolAffinityDefinition> getBrewingDefinitionsByCatalyst() {
        ensureBindingsResolved();
        return Map.copyOf(getCatalystDefinitionByItem());
    }

    public static synchronized Map<ResourceLocation, Integer> createCatalystBindingSnapshot() {
        ensureBindingsResolved();

        var bindings = new LinkedHashMap<ResourceLocation, Integer>();
        for (var entry : getCatalystDefinitionByItem().entrySet()) {
            var catalystId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (catalystId == null) {
                continue;
            }
            bindings.put(catalystId, entry.getValue().slotIndex());
        }
        return java.util.Collections.unmodifiableMap(bindings);
    }

    public static synchronized void invalidateBindings() {
        Arrays.fill(ASSIGNED_SCHOOLS, null);
        DEFINITION_BY_SCHOOL_ID.clear();
        catalystDefinitionByItem = null;
        bindingsResolved = false;
    }

    public static Component createAffinityName(SchoolType schoolType) {
        return Component.translatable("effect.apprenticecodex.school_affinity.dynamic", schoolType.getDisplayName());
    }

    public static ItemStack createIconStack(@Nullable SchoolType schoolType) {
        if (schoolType == null) {
            return new ItemStack(ItemRegistry.ARCANE_CINDER.get());
        }

        var schoolId = schoolType.getId();
        var runeId = ResourceLocation.fromNamespaceAndPath(schoolId.getNamespace(), schoolId.getPath() + "_rune");
        var runeItem = ForgeRegistries.ITEMS.getValue(runeId);
        if (runeItem != null && runeItem != Items.AIR) {
            return new ItemStack(runeItem);
        }

        var focusItems = resolveFocusItems(schoolType);
        if (!focusItems.isEmpty()) {
            return new ItemStack(focusItems.get(0));
        }

        return new ItemStack(ItemRegistry.ARCANE_CINDER.get());
    }

    public static Optional<SchoolType> getAssignedSchool(int slotIndex) {
        ensureBindingsResolved();

        if (slotIndex < 0 || slotIndex >= ASSIGNED_SCHOOLS.length) {
            return Optional.empty();
        }
        return Optional.ofNullable(ASSIGNED_SCHOOLS[slotIndex]);
    }

    public static synchronized List<ResourceLocation> createAssignmentSnapshot() {
        ensureBindingsResolved();

        var assignments = new ArrayList<ResourceLocation>(ASSIGNED_SCHOOLS.length);
        for (var schoolType : ASSIGNED_SCHOOLS) {
            assignments.add(schoolType != null ? schoolType.getId() : null);
        }
        return java.util.Collections.unmodifiableList(assignments);
    }

    public static synchronized void applySyncedAssignments(
            List<ResourceLocation> schoolIdsBySlot,
            Map<ResourceLocation, Integer> catalystSlotsByItemId
    ) {
        Arrays.fill(SYNCED_SCHOOL_IDS, null);

        for (int slotIndex = 0; slotIndex < Math.min(SYNCED_SCHOOL_IDS.length, schoolIdsBySlot.size()); slotIndex++) {
            SYNCED_SCHOOL_IDS[slotIndex] = schoolIdsBySlot.get(slotIndex);
        }

        syncedCatalystSlotsByItemId = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(catalystSlotsByItemId));
        hasSyncedAssignments = true;
        invalidateBindings();
    }

    public static synchronized void clearSyncedAssignments() {
        if (!hasSyncedAssignments) {
            return;
        }

        Arrays.fill(SYNCED_SCHOOL_IDS, null);
        syncedCatalystSlotsByItemId = Map.of();
        hasSyncedAssignments = false;
        invalidateBindings();
    }

    public static UUID createModifierUuid(int slotIndex) {
        return UUID.nameUUIDFromBytes(("apprenticecodex:school_affinity_slot:" + slotIndex).getBytes(StandardCharsets.UTF_8));
    }

    public static int resolveColor(SchoolType schoolType) {
        var textColor = schoolType.getDisplayName().getStyle().getColor();
        if (textColor != null) {
            return textColor.getValue();
        }

        var targetingColor = schoolType.getTargetingColor();
        var red = Math.max(0, Math.min(255, Math.round(targetingColor.x() * 255f)));
        var green = Math.max(0, Math.min(255, Math.round(targetingColor.y() * 255f)));
        var blue = Math.max(0, Math.min(255, Math.round(targetingColor.z() * 255f)));
        return (red << 16) | (green << 8) | blue;
    }

    private static synchronized void ensureBindingsResolved() {
        if (bindingsResolved) {
            return;
        }

        getDefinitions();
        Arrays.fill(ASSIGNED_SCHOOLS, null);
        DEFINITION_BY_SCHOOL_ID.clear();
        catalystDefinitionByItem = new LinkedHashMap<>();

        var schoolRegistry = SchoolRegistry.REGISTRY.get();
        var supportedDefinitionsByCatalyst = new LinkedHashMap<Item, List<SchoolAffinityDefinition>>();

        if (hasSyncedAssignments) {
            applyResolvedAssignmentsFromIds(schoolRegistry);
            finalizeSyncedBindings();
            return;
        }

        // Iron's 本体分はスロットを固定し、今後もセーブデータ上の対応がずれないようにする.
        for (var slotIndex = 0; slotIndex < BUILTIN_AFFINITY_SLOT_COUNT; slotIndex++) {
            var schoolId = BUILTIN_IRONS_SCHOOL_IDS.get(slotIndex);
            var schoolType = schoolRegistry.getValue(schoolId);
            bindSchoolToSlot(slotIndex, schoolType, supportedDefinitionsByCatalyst);
        }

        var extraSchools = schoolRegistry.getValues().stream()
                .filter(school -> !BUILTIN_IRONS_SCHOOL_IDS.contains(school.getId()))
                .filter(school -> MagicTools.resolveSchoolPowerAttribute(school) != null)
                .sorted(Comparator.comparing(SchoolAffinityRegistry::getExtraSchoolSortKey))
                .toList();

        var selectedExtraSchools = resolveSelectedExtraSchools(extraSchools);
        if (selectedExtraSchools.size() < extraSchools.size() && !warnedExtraSchoolCount) {
            var selectedSchoolIds = selectedExtraSchools.stream()
                    .map(SchoolType::getId)
                    .collect(java.util.stream.Collectors.toSet());
            ApprenticeCodex.LOGGER.warn(
                    "School Affinity has {} extra schools loaded, but only {} extra slots are available. Some schools are unsupported: {}",
                    extraSchools.size(),
                    EXTRA_AFFINITY_SLOT_COUNT,
                    extraSchools.stream()
                            .map(SchoolType::getId)
                            .filter(id -> !selectedSchoolIds.contains(id))
                            .map(ResourceLocation::toString)
                            .toList()
            );
            warnedExtraSchoolCount = true;
        }

        for (var extraIndex = 0; extraIndex < Math.min(selectedExtraSchools.size(), EXTRA_AFFINITY_SLOT_COUNT); extraIndex++) {
            var slotIndex = BUILTIN_AFFINITY_SLOT_COUNT + extraIndex;
            bindSchoolToSlot(slotIndex, selectedExtraSchools.get(extraIndex), supportedDefinitionsByCatalyst);
        }

        finalizeResolvedBindings(supportedDefinitionsByCatalyst);
    }

    private static void applyResolvedAssignmentsFromIds(net.minecraftforge.registries.IForgeRegistry<SchoolType> schoolRegistry) {
        for (int slotIndex = 0; slotIndex < SYNCED_SCHOOL_IDS.length; slotIndex++) {
            var schoolId = SYNCED_SCHOOL_IDS[slotIndex];
            assignSchoolToSlot(slotIndex, schoolId != null ? schoolRegistry.getValue(schoolId) : null);
        }
    }

    private static void finalizeResolvedBindings(Map<Item, List<SchoolAffinityDefinition>> supportedDefinitionsByCatalyst) {
        var uniqueDefinitionsByCatalyst = new LinkedHashMap<Item, SchoolAffinityDefinition>();
        for (var entry : supportedDefinitionsByCatalyst.entrySet()) {
            var resolvedDefinition = resolveCatalystDefinition(entry.getKey(), entry.getValue());
            if (resolvedDefinition == null) {
                continue;
            }

            uniqueDefinitionsByCatalyst.put(entry.getKey(), resolvedDefinition);
        }

        catalystDefinitionByItem = uniqueDefinitionsByCatalyst;
        bindingsResolved = true;
        logResolvedAssignments();
    }

    private static void finalizeSyncedBindings() {
        var uniqueDefinitionsByCatalyst = new LinkedHashMap<Item, SchoolAffinityDefinition>();
        for (var entry : syncedCatalystSlotsByItemId.entrySet()) {
            var catalyst = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (catalyst == null || catalyst == Items.AIR) {
                ApprenticeCodex.LOGGER.warn("Skipped synced School Affinity catalyst {} because the item does not exist on this client.", entry.getKey());
                continue;
            }

            var slotIndex = entry.getValue();
            if (slotIndex < 0 || slotIndex >= DEFINITIONS.size()) {
                ApprenticeCodex.LOGGER.warn("Skipped synced School Affinity catalyst {} because slot {} is out of range.", entry.getKey(), slotIndex);
                continue;
            }
            if (ASSIGNED_SCHOOLS[slotIndex] == null) {
                ApprenticeCodex.LOGGER.warn("Skipped synced School Affinity catalyst {} because slot {} is not assigned on this client.", entry.getKey(), slotIndex);
                continue;
            }

            uniqueDefinitionsByCatalyst.put(catalyst, DEFINITIONS.get(slotIndex));
        }

        catalystDefinitionByItem = uniqueDefinitionsByCatalyst;
        bindingsResolved = true;
        logResolvedAssignments();
    }

    private static List<SchoolType> resolveSelectedExtraSchools(List<SchoolType> extraSchools) {
        var candidatesById = new LinkedHashMap<ResourceLocation, SchoolType>();
        for (var school : extraSchools) {
            candidatesById.put(school.getId(), school);
        }

        var policy = resolveSelectionPolicy();
        var denyIds = new LinkedHashSet<>(policy.deny());
        var selected = new ArrayList<SchoolType>(EXTRA_AFFINITY_SLOT_COUNT);

        for (var deniedId : denyIds) {
            if (BUILTIN_IRONS_SCHOOL_IDS.contains(deniedId)) {
                ApprenticeCodex.LOGGER.warn("School Affinity deny entry {} was ignored because builtin schools always remain assigned.", deniedId);
            }
        }

        for (var priorityId : policy.priorities()) {
            if (BUILTIN_IRONS_SCHOOL_IDS.contains(priorityId)) {
                ApprenticeCodex.LOGGER.warn("School Affinity priority entry {} was ignored because builtin schools already have fixed slots.", priorityId);
                continue;
            }
            if (denyIds.contains(priorityId)) {
                ApprenticeCodex.LOGGER.warn("School Affinity priority entry {} was ignored because the same school is denylisted.", priorityId);
                continue;
            }

            var school = candidatesById.remove(priorityId);
            if (school == null) {
                ApprenticeCodex.LOGGER.warn("School Affinity priority entry {} did not match any supported extra school.", priorityId);
                continue;
            }

            if (selected.size() >= EXTRA_AFFINITY_SLOT_COUNT) {
                break;
            }
            selected.add(school);
        }

        for (var school : candidatesById.values()) {
            if (denyIds.contains(school.getId())) {
                continue;
            }
            if (selected.size() >= EXTRA_AFFINITY_SLOT_COUNT) {
                break;
            }
            selected.add(school);
        }

        return List.copyOf(selected);
    }

    private static SchoolAffinitySelectionPolicy resolveSelectionPolicy() {
        if (SchoolAffinitySelectionPolicyManager.hasDatapackEntries()) {
            return SchoolAffinitySelectionPolicyManager.getResolvedPolicy();
        }

        return new SchoolAffinitySelectionPolicy(
                parseSchoolIds(ApprenticeCodexCommonConfig.schoolAffinityPriority()),
                parseSchoolIds(ApprenticeCodexCommonConfig.schoolAffinityDeny())
        );
    }

    private static List<ResourceLocation> parseSchoolIds(List<String> rawSchoolIds) {
        var resolved = new LinkedHashSet<ResourceLocation>();
        for (var rawSchoolId : rawSchoolIds) {
            var schoolId = ResourceLocation.tryParse(rawSchoolId);
            if (schoolId == null) {
                ApprenticeCodex.LOGGER.warn("Ignored invalid School Affinity school id in config: {}", rawSchoolId);
                continue;
            }
            resolved.add(schoolId);
        }
        return resolved.stream().toList();
    }

    private static void bindSchoolToSlot(int slotIndex, @Nullable SchoolType schoolType, Map<Item, List<SchoolAffinityDefinition>> supportedDefinitionsByCatalyst) {
        var definition = assignSchoolToSlot(slotIndex, schoolType);
        if (definition == null) {
            return;
        }

        var catalysts = resolveBrewingCatalysts(schoolType);
        if (catalysts.isEmpty()) {
            return;
        }

        // override がある school は単一素材へ差し替え、未指定 school は従来通り focus 全候補を使う.
        for (var catalyst : catalysts) {
            supportedDefinitionsByCatalyst.computeIfAbsent(catalyst, ignored -> new ArrayList<>()).add(definition);
        }
    }

    @Nullable
    private static SchoolAffinityDefinition assignSchoolToSlot(int slotIndex, @Nullable SchoolType schoolType) {
        if (schoolType == null || slotIndex < 0 || slotIndex >= DEFINITIONS.size()) {
            return null;
        }

        var spellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
        if (spellPowerAttribute == null) {
            ApprenticeCodex.LOGGER.warn("Skipped School Affinity for {} because spell power attribute could not be resolved.", schoolType.getId());
            return null;
        }

        var definition = DEFINITIONS.get(slotIndex);
        ASSIGNED_SCHOOLS[slotIndex] = schoolType;
        DEFINITION_BY_SCHOOL_ID.put(schoolType.getId(), definition);
        return definition;
    }

    private static List<Item> resolveBrewingCatalysts(SchoolType schoolType) {
        var overrideItem = resolveOverrideCatalystItem(schoolType);
        if (overrideItem != null) {
            return List.of(overrideItem);
        }

        var focusItems = resolveFocusItems(schoolType);
        if (focusItems.isEmpty()) {
            return List.of();
        }

        // 同一 School の複数 focus は従来通り全部許容する。
        var catalysts = new LinkedHashSet<Item>();
        for (var focusItem : focusItems) {
            catalysts.add(substituteCatalystItem(focusItem));
        }
        return List.copyOf(catalysts);
    }

    @Nullable
    private static Item resolveOverrideCatalystItem(SchoolType schoolType) {
        var itemId = SchoolAffinityCatalystOverrideManager.getResolvedOverrides().get(schoolType.getId());
        if (itemId == null) {
            return null;
        }

        var item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == Items.AIR) {
            ApprenticeCodex.LOGGER.warn(
                    "School Affinity catalyst override for {} references missing item {}. Falling back to focus items.",
                    schoolType.getId(),
                    itemId
            );
            return null;
        }

        return substituteCatalystItem(item);
    }

    private static synchronized Map<Item, SchoolAffinityDefinition> getCatalystDefinitionByItem() {
        ensureBindingsResolved();

        if (catalystDefinitionByItem == null) {
            catalystDefinitionByItem = new LinkedHashMap<>();
        }
        return catalystDefinitionByItem;
    }

    private static List<Item> resolveFocusItems(SchoolType schoolType) {
        return ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> new ItemStack(item).is(schoolType.getFocus()))
                .sorted(Comparator.comparing(SchoolAffinityRegistry::getItemSortKey))
                .toList();
    }

    private static Item substituteCatalystItem(Item item) {
        if (item == Items.REDSTONE) {
            return Items.REDSTONE_BLOCK;
        }
        if (item == Items.GLOWSTONE_DUST) {
            return Items.GLOWSTONE;
        }
        return item;
    }

    private static String getExtraSchoolSortKey(SchoolType schoolType) {
        var contents = schoolType.getDisplayName().getContents();
        if (contents instanceof TranslatableContents translatableContents) {
            return translatableContents.getKey();
        }
        return schoolType.getId().toString();
    }

    private static String getItemSortKey(Item item) {
        var itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemId != null ? itemId.toString() : item.toString();
    }

    @Nullable
    private static SchoolAffinityDefinition resolveCatalystDefinition(Item catalyst, List<SchoolAffinityDefinition> definitions) {
        if (definitions.size() == 1) {
            return definitions.get(0);
        }

        logCatalystConflict(catalyst, definitions);
        return null;
    }

    private static void logCatalystConflict(Item catalyst, List<SchoolAffinityDefinition> definitions) {
        var catalystId = ForgeRegistries.ITEMS.getKey(catalyst);
        var conflictedSchoolIds = definitions.stream()
                .map(SchoolAffinityRegistry::getAssignedSchoolId)
                .distinct()
                .toList();
        ApprenticeCodex.LOGGER.warn(
                "School Affinity catalyst conflict detected for {}. Conflicted schools: {}. Only these schools stay disabled until their affinity catalyst is separated.",
                catalystId != null ? catalystId : catalyst,
                conflictedSchoolIds
        );
    }

    private static String getAssignedSchoolId(SchoolAffinityDefinition definition) {
        var schoolType = ASSIGNED_SCHOOLS[definition.slotIndex()];
        return schoolType != null ? schoolType.getId().toString() : "<empty>";
    }

    private static void logResolvedAssignments() {
        ApprenticeCodex.LOGGER.info("School Affinity potion assignments resolved.");
        var catalystsBySlot = new LinkedHashMap<Integer, List<String>>();
        for (var entry : getCatalystDefinitionByItem().entrySet()) {
            var catalystId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (catalystId == null) {
                continue;
            }
            catalystsBySlot.computeIfAbsent(entry.getValue().slotIndex(), ignored -> new ArrayList<>())
                    .add(catalystId.toString());
        }

        for (var definition : DEFINITIONS) {
            var schoolType = ASSIGNED_SCHOOLS[definition.slotIndex()];
            var assignedSchoolId = schoolType != null ? schoolType.getId().toString() : "<empty>";
            var catalystIds = catalystsBySlot.getOrDefault(definition.slotIndex(), List.of());
            ApprenticeCodex.LOGGER.info(
                    "School Affinity slot {}: {} -> {} catalysts={}",
                    String.format("%02d", definition.slotIndex()),
                    definition.basePotionId(),
                    assignedSchoolId,
                    catalystIds
            );
        }
    }

    public record SchoolAffinityDefinition(
            int slotIndex,
            ResourceLocation effectId,
            SchoolAffinityEffect effect,
            ResourceLocation basePotionId,
            SchoolAffinityPotion basePotion,
            ResourceLocation longPotionId,
            SchoolAffinityPotion longPotion,
            ResourceLocation strongPotionId,
            SchoolAffinityPotion strongPotion
    ) {
    }
}
