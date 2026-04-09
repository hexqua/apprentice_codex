package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SpellDispenserCastHelper {
    private static final double PROXY_FEET_OFFSET = 1.35D;

    private SpellDispenserCastHelper() {
    }

    public static boolean tryCast(ServerLevel level, BlockPos pos, Direction facing, ItemStack spellSource) {
        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (!validation.isSupported()) {
            return false;
        }

        var spellData = validation.spellData();
        var spell = spellData.getSpell();
        var proxy = createProxy(level, pos, facing, spellSource);
        level.addFreshEntity(proxy);

        var magicData = MagicData.getPlayerMagicData(proxy);
        try {
            // Iron's の initiateCast は getSyncedData を経由せず syncedSpellData を直接参照する。
            // proxy は ServerPlayer ではないため、ここで LivingEntity ベースの同期データを先に用意する。
            magicData.setSyncedData(new SyncedSpellData(proxy));
            // Iron's は LivingEntity に MagicData を差し込むので、短命な proxy でも既存の precast 経路をある程度流用できる。
            magicData.initiateCast(spell, spellData.getLevel(), 0, CastSource.COMMAND, SpellSelectionManager.MAINHAND);
            magicData.setPlayerCastingItem(spellSource.copy());

            if (!spell.checkPreCastConditions(level, spellData.getLevel(), proxy, magicData)) {
                magicData.resetCastingState();
                proxy.discard();
                return false;
            }

            spell.onServerPreCast(level, spellData.getLevel(), proxy, magicData);
            spell.onCast(level, spellData.getLevel(), proxy, CastSource.COMMAND, magicData);
            spell.onServerCastComplete(level, spellData.getLevel(), proxy, magicData, false);
            proxy.discard();
            return true;
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn("Spell Dispenser cast failed: {}", spell.getSpellId(), exception);
            magicData.resetCastingState();
            proxy.discard();
            return false;
        }
    }

    private static ArmorStand createProxy(ServerLevel level, BlockPos pos, Direction facing, ItemStack spellSource) {
        var muzzlePos = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.7D));
        var yaw = resolveYaw(facing);
        var pitch = resolvePitch(facing);
        var proxy = new ArmorStand(level, muzzlePos.x, muzzlePos.y - PROXY_FEET_OFFSET, muzzlePos.z);
        proxy.setNoGravity(true);
        proxy.setInvisible(true);
        proxy.setInvulnerable(true);
        proxy.setSilent(true);
        proxy.noPhysics = true;
        proxy.setItemSlot(EquipmentSlot.MAINHAND, spellSource.copy());
        proxy.setItemSlot(EquipmentSlot.OFFHAND, spellSource.copy());
        proxy.moveTo(muzzlePos.x, muzzlePos.y - PROXY_FEET_OFFSET, muzzlePos.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.setXRot(pitch);
        proxy.xRotO = pitch;
        return proxy;
    }

    private static float resolveYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            case UP, DOWN -> 0.0F;
        };
    }

    private static float resolvePitch(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }
}
