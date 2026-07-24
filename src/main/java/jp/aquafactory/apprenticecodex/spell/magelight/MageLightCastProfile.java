package jp.aquafactory.apprenticecodex.spell.magelight;

public record MageLightCastProfile(double normalRange, double effectiveRange, int baseManaCost) {
    private static final double MIN_RANGE = 1.0E-6D;

    public MageLightCastProfile {
        normalRange = Math.max(0.0D, normalRange);
        effectiveRange = Math.max(normalRange, effectiveRange);
        baseManaCost = Math.max(0, baseManaCost);
    }

    public boolean extendsRange() {
        return effectiveRange > normalRange + MIN_RANGE;
    }

    public int manaCostAt(double distance) {
        if (baseManaCost <= 0 || normalRange <= MIN_RANGE) {
            return baseManaCost;
        }
        var multiplier = Math.max(1.0D, Math.max(0.0D, distance) / normalRange);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(baseManaCost * multiplier));
    }

    public int maximumManaCost() {
        return manaCostAt(effectiveRange);
    }
}
