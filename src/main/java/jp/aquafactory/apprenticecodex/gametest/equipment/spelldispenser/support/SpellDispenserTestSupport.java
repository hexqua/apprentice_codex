package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public final class SpellDispenserTestSupport {
    private SpellDispenserTestSupport() {}

    public static ItemStack createSpellScroll(AbstractSpell spell) {
        return createSpellScroll(spell, 1);
    }

    public static ItemStack createSpellScroll(AbstractSpell spell, int level) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }

    public static FakePlayer createSpellDispenserPlacer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }
}
