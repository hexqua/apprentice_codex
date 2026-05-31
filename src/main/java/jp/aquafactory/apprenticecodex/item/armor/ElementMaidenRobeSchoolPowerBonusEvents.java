package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementMaidenRobeSchoolPowerBonusEvents {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] BONUS_TARGET_SLOTS = {
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final String MODIFIER_ID_PREFIX =
            "apprenticecodex.element_maiden_robe.school_spell_power.";

    private ElementMaidenRobeSchoolPowerBonusEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModBusEvents::onConfigLoading);
        modEventBus.addListener(ModBusEvents::onConfigReloading);
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isArmorSlot(event.getSlot())
                && !isElementMaidenRobe(event.getFrom())
                && !isElementMaidenRobe(event.getTo())) {
            return;
        }

        refresh(player);
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Curios.SPELLBOOK_SLOT.equals(event.getIdentifier())) {
            return;
        }

        refresh(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            refresh(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            refresh(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            refresh(serverPlayer);
        }
    }

    public static Map<Attribute, Double> refresh(@Nullable Player player) {
        if (player == null || player.level().isClientSide) {
            return Map.of();
        }

        removeOwnedModifiers(player);
        var targetArmors = findBonusTargetArmors(player);
        if (targetArmors.isEmpty()) {
            if (clearPlayerRobeBonuses(player, List.of()) && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            return Map.of();
        }

        var baseAmount = floorToCent(ApprenticeCodexServerConfig.elementMaidenRobeSchoolSpellPowerBonus());
        if (baseAmount <= 0.0D) {
            syncArmorBonuses(player, targetArmors, Map.of());
            return Map.of();
        }

        var bonuses = resolveSchoolPowerBonuses(player, baseAmount);
        syncArmorBonuses(player, targetArmors, bonuses);
        return bonuses;
    }

    public static Map<Attribute, Double> resolveSchoolPowerBonuses(Player player, double baseAmount) {
        var spellbookStack = getEquippedSpellbookStack(player);
        if (spellbookStack.isEmpty()) {
            return Map.of();
        }

        var distribution = resolveSpellbookDistribution(player, spellbookStack);
        if (distribution.slotCount() <= 0 || distribution.schoolCounts().isEmpty()) {
            return Map.of();
        }

        return resolveSchoolPowerBonuses(distribution.slotCount(), distribution.schoolCounts(), baseAmount);
    }

    public static Map<Attribute, Double> resolveSchoolPowerBonuses(
            int slotCount,
            Map<SchoolType, Integer> schoolCounts,
            double baseAmount
    ) {
        if (slotCount <= 0 || schoolCounts.isEmpty()) {
            return Map.of();
        }

        var distribution = new SpellbookDistribution(slotCount, schoolCounts);
        var schoolWeights = distributeEmptySlots(distribution);
        var result = new LinkedHashMap<Attribute, Double>();
        for (var entry : schoolWeights.entrySet()) {
            var attribute = MagicTools.resolveSchoolPowerAttribute(entry.getKey());
            if (attribute == null) {
                continue;
            }

            var amount = floorToCent(baseAmount * entry.getValue() / distribution.slotCount());
            if (amount > 0.0D) {
                result.merge(attribute, amount, Double::sum);
            }
        }
        return result;
    }

    private static SpellbookDistribution resolveSpellbookDistribution(Player player, ItemStack spellbookStack) {
        if (spellbookStack.getItem() instanceof ArchivistsGrimoire) {
            var spells = new ArrayList<SpellData>(ArchivistsGrimoire.COLUMN_COUNT);
            for (var visibleSlot = 0; visibleSlot < ArchivistsGrimoire.COLUMN_COUNT; ++visibleSlot) {
                spells.add(ArchivistsGrimoire.getVisibleSpell(spellbookStack, visibleSlot, player.registryAccess()));
            }
            return countSchools(spells, ArchivistsGrimoire.COLUMN_COUNT);
        }

        if (spellbookStack.getItem() instanceof EnderGrimoire) {
            var data = Capabilities.getEnderGrimoireSpellbookOrNull(player);
            return data == null ? SpellbookDistribution.empty() : countSchools(data.getSpellContainer());
        }

        if (!(spellbookStack.getItem() instanceof ISpellbook) || !ISpellContainer.isSpellContainer(spellbookStack)) {
            return SpellbookDistribution.empty();
        }

        var spellContainer = ISpellContainer.get(spellbookStack);
        return spellContainer == null ? SpellbookDistribution.empty() : countSchools(spellContainer);
    }

    private static SpellbookDistribution countSchools(ISpellContainer spellContainer) {
        var spells = new ArrayList<SpellData>(spellContainer.getMaxSpellCount());
        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            spells.add(spellContainer.getSpellAtIndex(index));
        }
        return countSchools(spells, spellContainer.getMaxSpellCount());
    }

    private static SpellbookDistribution countSchools(List<SpellData> spells, int slotCount) {
        if (slotCount <= 0) {
            return SpellbookDistribution.empty();
        }

        var schoolCounts = new HashMap<SchoolType, Integer>();
        for (var spellData : spells) {
            if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            var school = spellData.getSpell().getSchoolType();
            if (school != null) {
                schoolCounts.merge(school, 1, Integer::sum);
            }
        }
        return new SpellbookDistribution(slotCount, schoolCounts);
    }

    private static Map<SchoolType, Double> distributeEmptySlots(SpellbookDistribution distribution) {
        var result = new LinkedHashMap<SchoolType, Double>();
        distribution.schoolCounts().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getId().toString()))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().doubleValue()));

        var filledSlots = distribution.schoolCounts().values().stream().mapToInt(Integer::intValue).sum();
        var emptySlots = Math.max(0, distribution.slotCount() - filledSlots);
        if (emptySlots == 0) {
            return result;
        }

        var maxCount = distribution.schoolCounts().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var strongestSchools = distribution.schoolCounts().entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();
        if (strongestSchools.isEmpty()) {
            return result;
        }

        var emptyShare = (double) emptySlots / strongestSchools.size();
        for (var school : strongestSchools) {
            result.merge(school, emptyShare, Double::sum);
        }
        return result;
    }

    private static ItemStack getEquippedSpellbookStack(Player player) {
        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        return spellbookStack == null ? ItemStack.EMPTY : spellbookStack;
    }

    private static List<ArmorStack> findBonusTargetArmors(Player player) {
        var result = new ArrayList<ArmorStack>();
        for (var slot : BONUS_TARGET_SLOTS) {
            var stack = player.getItemBySlot(slot);
            if (isElementMaidenRobe(stack)) {
                result.add(new ArmorStack(slot, stack));
            }
        }
        return result;
    }

    private static boolean isElementMaidenRobe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ElementMaidenRobeItem;
    }

    private static void syncArmorBonuses(Player player, List<ArmorStack> targetArmors, Map<Attribute, Double> bonuses) {
        var excludedStacks = targetArmors.stream()
                .map(ArmorStack::stack)
                .toList();
        var changed = clearPlayerRobeBonuses(player, excludedStacks);
        var changedArmors = new ArrayList<ArmorStack>();
        for (var targetArmor : targetArmors) {
            if (ElementMaidenRobeItem.setSpellbookSchoolPowerBonuses(targetArmor.stack(), bonuses)) {
                changed = true;
                changedArmors.add(targetArmor);
            }
        }

        if (changed && player instanceof ServerPlayer serverPlayer) {
            // NBT のみの更新でも装備変更検知とクライアント同期に乗るよう、装備スロットへ書き戻す.
            for (var targetArmor : changedArmors) {
                serverPlayer.setItemSlot(targetArmor.slot(), targetArmor.stack().copy());
            }
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    private static boolean clearPlayerRobeBonuses(Player player, List<ItemStack> excludedStacks) {
        var changed = false;
        for (var stack : player.getInventory().items) {
            changed |= clearRobeBonusUnlessExcluded(stack, excludedStacks);
        }
        for (var stack : player.getInventory().armor) {
            changed |= clearRobeBonusUnlessExcluded(stack, excludedStacks);
        }
        for (var stack : player.getInventory().offhand) {
            changed |= clearRobeBonusUnlessExcluded(stack, excludedStacks);
        }
        for (var slot : ARMOR_SLOTS) {
            changed |= clearRobeBonusUnlessExcluded(player.getItemBySlot(slot), excludedStacks);
        }
        return changed;
    }

    private static boolean clearRobeBonusUnlessExcluded(ItemStack stack, List<ItemStack> excludedStacks) {
        if (stack.isEmpty() || excludedStacks.stream().anyMatch(excludedStack -> stack == excludedStack)
                || !isElementMaidenRobe(stack)) {
            return false;
        }

        return ElementMaidenRobeItem.clearSpellbookSchoolPowerBonuses(stack);
    }

    private static void removeOwnedModifiers(Player player) {
        for (var attribute : BuiltInRegistries.ATTRIBUTE) {
            var modifierId = createModifierId(attribute);
            if (modifierId == null) {
                continue;
            }

            var attributeInstance = player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
            if (attributeInstance != null) {
                attributeInstance.removeModifier(modifierId);
            }
        }
    }

    private static @Nullable ResourceLocation createModifierId(Attribute attribute) {
        var attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (attributeId == null) {
            return null;
        }

        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, MODIFIER_ID_PREFIX + normalizeAttributeId(attributeId));
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        for (var armorSlot : ARMOR_SLOTS) {
            if (armorSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeAttributeId(ResourceLocation attributeId) {
        return attributeId.getNamespace() + "." + attributeId.getPath().replace('/', '.');
    }

    private static double floorToCent(double amount) {
        return Math.floor(Math.max(0.0D, amount) * 100.0D) / 100.0D;
    }

    private record ArmorStack(EquipmentSlot slot, ItemStack stack) {
    }

    private record SpellbookDistribution(int slotCount, Map<SchoolType, Integer> schoolCounts) {
        private static SpellbookDistribution empty() {
            return new SpellbookDistribution(0, Map.of());
        }
    }

    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        public static void onConfigLoading(ModConfigEvent.Loading event) {
            refreshIfServerConfig(event);
        }

        public static void onConfigReloading(ModConfigEvent.Reloading event) {
            refreshIfServerConfig(event);
        }

        private static void refreshIfServerConfig(ModConfigEvent event) {
            if (event.getConfig().getType() != ModConfig.Type.SERVER
                    || !ApprenticeCodex.MODID.equals(event.getConfig().getModId())) {
                return;
            }

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }

            for (var player : server.getPlayerList().getPlayers()) {
                refresh(player);
            }
        }
    }
}
