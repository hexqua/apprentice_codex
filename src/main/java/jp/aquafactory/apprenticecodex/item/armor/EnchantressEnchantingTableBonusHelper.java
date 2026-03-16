package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class EnchantressEnchantingTableBonusHelper {
    public static final int TARGET_ENCHANT_ROW = 2;

    private static final double PLAYER_SEARCH_RADIUS = 8.0D;
    private static final String APOTHEOSIS_MOD_ID = "apotheosis";

    private EnchantressEnchantingTableBonusHelper() {
    }

    public static boolean isFeatureDisabled() {
        // Apotheosis は独自のエンチャント経路を持つため、未対応時は安全側で全処理を止める。
        return ModList.get().isLoaded(APOTHEOSIS_MOD_ID);
    }

    public static int getBonusForPlayer(@Nullable Player player) {
        if (player == null || !ApprenticeAttributeRegistry.MAX_ENCHANTMENT_TABLE_LEVEL.isPresent()) {
            return 0;
        }

        var attribute = ApprenticeAttributeRegistry.MAX_ENCHANTMENT_TABLE_LEVEL.get();
        var bonus = 0.0D;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // この Attribute はプレイヤー本体へ登録せず、装備が持つ修飾子を直接合算して付呪台処理へ流す。
            for (var modifier : stack.getAttributeModifiers(slot).get(attribute)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    bonus += modifier.getAmount();
                }
            }
        }

        return Math.max(0, (int) Math.round(bonus));
    }

    public static int getBonusForNearbyEnchantingPlayer(Level level, BlockPos tablePos) {
        var searchBox = new AABB(tablePos).inflate(PLAYER_SEARCH_RADIUS);
        var nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox, player ->
                player.containerMenu instanceof EnchantmentMenu && player.distanceToSqr(
                        tablePos.getX() + 0.5D,
                        tablePos.getY() + 0.5D,
                        tablePos.getZ() + 0.5D
                ) <= PLAYER_SEARCH_RADIUS * PLAYER_SEARCH_RADIUS
        );

        var bestBonus = 0;
        for (var player : nearbyPlayers) {
            bestBonus = Math.max(bestBonus, getBonusForPlayer(player));
        }
        return bestBonus;
    }
}
