package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo;

import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowCoreEntity;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowEntity;
import net.minecraft.world.phys.Vec3;

final class EchoArrowFixture {
    private EchoArrowFixture() {}

    record Pair(EchoArrowEntity arrow, EchoArrowCoreEntity core) {
    }

    record Shot(EchoArrowEntity arrow, long time, Vec3 origin, Vec3 velocity) {
    }
}
