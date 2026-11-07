package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class SpellDispenserTestSupport {
    private SpellDispenserTestSupport() {}

    public static FakePlayer createSpellDispenserPlacer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    public static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }

    public static SpellDispenserSpellValidator.ValidationResult createSpellDispenserValidation(ItemStack stack, AbstractSpell spell) {
        return new SpellDispenserSpellValidator.ValidationResult(stack, new SpellData(spell, 1), SpellDispenserSpellValidator.FailureReason.NONE);
    }

    public static GameProfile createSpellDispenserOwnerProfile(String name) {
        return new GameProfile(UUID.randomUUID(), name);
    }


}
