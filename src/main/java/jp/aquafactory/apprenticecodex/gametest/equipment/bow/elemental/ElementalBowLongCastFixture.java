package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.fire.FireArrowSpell;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.elementalbow.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.UUID;

final class ElementalBowLongCastFixture {
    private ElementalBowLongCastFixture() {}
    static FakePlayer player(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = ElementalBowTestSupport.createEquipmentTestPlayer(h, new BlockPos(14, 4, 14), "bow_long");
        return preparePlayer(h, hand, spell, player);
    }

    static FakePlayer damageablePlayer(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "bow_damage")) {
            @Override public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }
        };
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // FakePlayer の無敵と生成直後の保護を解除し、通常のダメージ経路を通す。
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to disable spawn protection for GameTest", exception);
        }
        player.setPos(h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(14, 4, 14))));
        // Curios のスロット初期化は entity の world 参加時に行われる。
        h.getLevel().addFreshEntity(player);
        return preparePlayer(h, hand, spell, player);
    }

    static FakePlayer preparePlayer(GameTestHelper h, InteractionHand hand, AbstractSpell spell, FakePlayer player) {
        ElementalBowTestSupport.track(h, player);
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(2000);
        MagicData.getPlayerMagicData(player).setMana(1000);
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        ElementalBow.setCalibrationScroll(stack, 0, ElementalBowTestSupport.createSpellScroll(spell), h.getLevel().registryAccess());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(0).toString());
        });
        player.setItemInHand(hand, stack);
        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
        return player;
    }

    static void begin(GameTestHelper h, FakePlayer player, InteractionHand hand) {
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
            h.assertTrue(player.getItemInHand(hand).getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                    "LONG bow draw must begin");
            h.assertTrue(ElementalBowPendingCast.isPending(player), "Drawing must own a pending cast");
        }
    }

    static void assertUnspent(GameTestHelper h, FakePlayer player, ItemStack stack) {
        h.assertTrue(stack.getDamageValue() == 0 && player.getInventory().getItem(2).getCount() == 3
                && MagicData.getPlayerMagicData(player).getMana() == 1000
                && !ElementalBowOverheatManager.getState(player).active(), "Unfired casts must not spend resources or add heat");
    }

    static void assertCleared(GameTestHelper h, FakePlayer player) {
        var magic = MagicData.getPlayerMagicData(player);
        h.assertTrue(!ElementalBowPendingCast.isPending(player) && !magic.isCasting()
                && magic.getAdditionalCastData() == null, "Completion must clear the hold and targeting data");
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useDamageTestConfig(GameTestHelper h) {
        var config = ElementalBowTestSupport.useElementalBowSpellConfig(h);
        var previous = ElementalBowModeManager.createSnapshot();
        var definitions = new ArrayList<>(previous);
        // 標準プロファイルは LONG のため、データパックで追加可能な INSTANT も同じ操作で検証する。
        definitions.add(new ElementalBowModeDefinition(
                SpellRegistry.MAGIC_MISSILE_SPELL.get().getSpellResource(), 20));
        ElementalBowModeManager.applySnapshot(definitions);
        return () -> {
            ElementalBowModeManager.applySnapshot(previous);
            config.close();
        };
    }

    static final class LifecycleSpell extends FireArrowSpell {
        int starts, ticks, casts, completions, cancelled;
        boolean scoped = true;

        @Override
        public boolean canBeInterrupted(Player player) {
            return true;
        }

        @Override
        public void onServerPreCast(Level level, int spellLevel,
                                    LivingEntity entity, MagicData magic) {
            starts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastTick(Level level, int spellLevel,
                                     LivingEntity entity, MagicData magic) {
            ticks++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity entity,
                           CastSource source, MagicData magic) {
            casts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastComplete(Level level, int spellLevel,
                                         LivingEntity entity, MagicData magic, boolean wasCancelled) {
            completions++;
            if (wasCancelled) cancelled++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
            super.onServerCastComplete(level, spellLevel, entity, magic, wasCancelled);
        }
    }
}
