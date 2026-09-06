package jp.aquafactory.apprenticecodex.compat.epicfight;

import net.minecraft.client.Minecraft;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.skill.SkillContainer;

final class EpicFightScytheClient {
    private EpicFightScytheClient() {}

    static void checkInterruption(SkillContainer container) {
        // Epic Fight標準は保持中にガード要求自体を出さないため、大鎌のチャージだけ先に解除する。
        boolean guard = InputManager.isActionActive(EpicFightInputAction.GUARD)
                && !InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.GUARD, EpicFightInputAction.WEAPON_INNATE_SKILL);
        if (guard || Minecraft.getInstance().screen != null || !container.getExecutor().isEpicFightMode()) {
            container.getExecutor().resetHolding();
            EpicFightNetworkManager.sendToServer(new CPSkillRequest(container.getSlot(), CPSkillRequest.WorkType.CANCEL));
        }
    }
}
