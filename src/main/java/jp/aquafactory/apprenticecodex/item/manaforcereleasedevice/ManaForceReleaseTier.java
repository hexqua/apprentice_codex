package jp.aquafactory.apprenticecodex.item.manaforcereleasedevice;


public enum ManaForceReleaseTier {
    NOVICE("novice", 6, 250, 14, 16),
    ADEPT("adept", 7, 1561, 10, 24),
    MASTER("master", 7, 2031, 15, 24);

    public final String id;
    public final double attackDamage;
    public final int durability;
    public final int enchantability;
    public final double range;

    ManaForceReleaseTier(String name, double attackDamage, int durability, int enchantability, double range) {
        this.id = name + "_mana_force_release_device";
        this.attackDamage = attackDamage;
        this.durability = durability;
        this.enchantability = enchantability;
        this.range = range;
    }

    public double baseDamage(double mana, double softCap) {
        // 上位機は大容量向け。少量のマナでの威力逆転は意図した仕様。
        return switch (this) {
            case NOVICE -> 3 + Math.min(mana, softCap) / 20 + Math.max(0, mana - softCap) / 200;
            case ADEPT -> 5 + Math.min(mana, softCap) / 25 + Math.max(0, mana - softCap) / 100;
            case MASTER -> 5 + mana / 30;
        };
    }
}
