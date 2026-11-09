package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowScrollStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.elementalBowSlotId;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.prepareElementalBowScrolls;

public final class ElementalBowStateTestSupport {
    private ElementalBowStateTestSupport() {}

    static final String VANILLA_NAMESPACE = "minecraft";

    static final String MALUM_MOD_ID = "malum";

    static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();

    public static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
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
    public static ElementalBow.ModeSelectionView findElementalBowSelectionView(
            ServerPlayer player,
            ItemStack stack,
            String shotMode,
            @Nullable ResourceLocation selectionId
    ) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals("magic".equals(shotMode) ? elementalBowSlotId(selectionId) : selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    public static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        ElementalBowScrollStorage.migrate(stack);
        if ("magic".equals(shotMode)) prepareElementalBowScrolls(stack);
        var resolvedSelectionId = "magic".equals(shotMode) ? elementalBowSlotId(selectionId) : selectionId;
        { var tag = stack.getOrCreateTag();
            tag.putString("ElementalBowShotMode", shotMode);
            if ("magic".equals(shotMode)) {
                if (selectionId != null) {
                    tag.putString("ElementalBowMode", resolvedSelectionId.toString());
                }
                tag.remove("ElementalBowAmmoSelection");
                return;
            }

            if (selectionId != null) {
                tag.putString("ElementalBowAmmoSelection", selectionId.toString());
            } else {
                tag.remove("ElementalBowAmmoSelection");
            }
            tag.remove("ElementalBowMode");
        }
    }

    public static void setElementalBowMode(ItemStack stack, String mode) {
        setElementalBowShotSelection(stack, "magic", ResourceLocation.tryParse((mode.contains(":") ? mode : "irons_spellbooks:" + mode)));
    }
}
