package jp.aquafactory.apprenticecodex.spell;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.world.entity.player.Player;

public interface IClientBlockTargetCaptureSpell {
    BlockTargetData captureClientBlockTarget(Player player, int spellLevel);
}
