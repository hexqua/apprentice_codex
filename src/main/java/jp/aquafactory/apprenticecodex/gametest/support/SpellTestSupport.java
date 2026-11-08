package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.List;
import java.util.Optional;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class SpellTestSupport {
    private SpellTestSupport() {}

    public static RemoteOwnerCastProfile remotePlayerGeometryProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    public static ResourceLocation requireSpellId(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        if (spellId == null) {
            throw new IllegalStateException("Missing spell id for " + spell);
        }
        return spellId;
    }

    public static boolean isApprenticeSpell(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        return spellId != null && ApprenticeCodex.MODID.equals(spellId.getNamespace());
    }

    public static void postSpellOnCast(ServerPlayer player, AbstractSpell spell, int spellLevel) {
        NeoForge.EVENT_BUS.post(new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getManaCost(spellLevel),
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        ));
    }

    public static ItemStack createSpellScroll(AbstractSpell spell) {
        return createSpellScroll(spell, 1);
    }

    public static ItemStack createSpellScroll(AbstractSpell spell, int level) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }

    public static void assertScrollSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            String message
    ) {
        helper.assertTrue(stack.is(ItemRegistry.SCROLL.get()),
                message + " (stack is not a scroll: " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ")");

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + " (scroll spell container is null)");
        assertSpellData(helper, spellContainer.getSpellAtIndex(0), expectedSpell, 1, message);
    }

    public static @Nullable AbstractSpell getScrollSpell(ItemStack stack) {
        if (!stack.is(ItemRegistry.SCROLL.get())) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        return spellData.getSpell();
    }

    public static void assertSpellData(
            GameTestHelper helper,
            SpellData spellData,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        helper.assertTrue(spellData != SpellData.EMPTY,
                message + " (spell data is empty)");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
    }

    public static void assertSpellData(
            GameTestHelper helper,
            ISpellContainer spellContainer,
            int index,
            AbstractSpell expectedSpell,
            int expectedLevel,
            boolean expectedLocked,
            String message
    ) {
        var spellData = spellContainer.getSpellAtIndex(index);
        helper.assertTrue(spellData != SpellData.EMPTY,
                message + " (spell slot is empty at index " + index + ")");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
        helper.assertTrue(spellData.isLocked() == expectedLocked,
                message + " (locked mismatch: " + spellData.isLocked() + ")");
    }
}
