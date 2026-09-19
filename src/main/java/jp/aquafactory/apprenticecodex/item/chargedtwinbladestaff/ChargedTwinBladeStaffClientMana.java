package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.player.ClientMagicData;

final class ChargedTwinBladeStaffClientMana {
    private ChargedTwinBladeStaffClientMana() {
    }

    static boolean hasMana(int cost) {
        return ClientMagicData.getPlayerMana() >= cost;
    }
}
