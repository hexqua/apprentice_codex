package jp.aquafactory.apprenticecodex.item.spellgun;

/**
 * 発動中だけ術者の魔法系倍率を固定値へ置き換える Spellgun の契約。
 */
public interface ForcedSpellPowerSpellgun {
    double forcedSpellPower();

    double forcedSchoolSpellPower();

    double forcedSummonDamage();
}
