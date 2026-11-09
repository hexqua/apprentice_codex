package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class BowGameTestSupport {
    private BowGameTestSupport() {}

    static final String VANILLA_NAMESPACE = "minecraft";

    public static final String MALUM_MOD_ID = "malum";

    public static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();

    public static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();

    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks) {
        return ApprenticeCodexServerConfig.useElementalBowConfigOverrideForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks);
    }

    @Nullable
    static ElementalBow.ModeSelectionView findElementalBowSelectionView(ServerPlayer player, ItemStack stack,
                                                                                String shotMode, @Nullable ResourceLocation selectionId) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        ElementalBowStateTestSupport.setElementalBowShotSelection(stack, shotMode, selectionId);
    }

    static void setElementalBowMode(ItemStack stack, String mode) {
        ElementalBowStateTestSupport.setElementalBowMode(stack, mode);
    }

    static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ElementalBowStateTestSupport.MALUM_MOD_ID, "soul_hunter_weapon")
    );

    static final TagKey<Item> SPELLCASTER_WORKBENCH_EXTRACTABLE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench_extractable")
    );

    static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("traveloptics", "can_cast_reversal")
    );

    public static final TagKey<Item> CURIOS_BACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BACK)
    );

    public static final TagKey<Item> CURIOS_BELT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BELT)
    );

    static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );

    public static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    public static FakePlayer createEquipmentTestPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        return player;
    }

    public static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    public static <T extends SummonWeaponEntity> List<T> getOwnedSummonWeapons(
            GameTestHelper helper,
            FakePlayer owner,
            Class<T> weaponType
    ) {
        return helper.getLevel().getEntitiesOfClass(
                weaponType,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                weapon -> {
                    var summonOwner = weapon.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    public static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }

    public static void setSingleUnlockedSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before Focus Staffbow spell setup");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before Focus Staffbow spell setup");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare Focus Staffbow spell setup");
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    public static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowSpellConfig(GameTestHelper helper) {
        var previous = SpellConfigManager.INSTANCE;
        SpellConfigManager.INSTANCE = new SpellConfigManager();
        SpellConfigManager.INSTANCE.handleServerConfigUpdate();
        SpellConfigManager.onDatapackSync(
                new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null));
        return () -> SpellConfigManager.INSTANCE = previous;
    }

    public static ResourceLocation elementalBowSlotId(ResourceLocation school) {
        if (school == null) return null;
        return switch (school.toString()) {
            case "irons_spellbooks:fire" -> ElementalBow.selectionIdForSlot(0);
            case "irons_spellbooks:ender" -> ElementalBow.selectionIdForSlot(1);
            case "irons_spellbooks:nature" -> ElementalBow.selectionIdForSlot(2);
            default -> school;
        };
    }

    public static void prepareElementalBowScrolls(ItemStack stack) {
        var lookup = ElementalBow.serializationLookup();
        var item = (ElementalBow) stack.getItem();
        var upgrade = new ItemStack(ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        item.trySetCalibrationAdjustment(stack, 0, upgrade);
        item.trySetCalibrationAdjustment(stack, 1, upgrade);
        var spells = List.of(SpellRegistry.FIRE_ARROW_SPELL.get(),
                SpellRegistry.MAGIC_ARROW_SPELL.get(),
                SpellRegistry.POISON_ARROW_SPELL.get());
        for (int i = 0; i < spells.size(); i++) {
            if (ElementalBow.getCalibrationScroll(stack, i, lookup).isEmpty()) {
                var scroll = createSpellScroll(spells.get(i));
                ElementalBow.setCalibrationScroll(stack, i, scroll, lookup);
            }
        }
    }
}
