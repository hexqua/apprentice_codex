package jp.aquafactory.apprenticecodex.potion;

public enum SchoolAffinityPotionVariant {
    BASE("affinity", 20 * 60 * 3, 0),
    LONG("long_affinity", 20 * 60 * 8, 0),
    STRONG("strong_affinity", 20 * 90, 1);

    private final String registryNamePrefix;
    private final int durationTicks;
    private final int amplifier;

    SchoolAffinityPotionVariant(String registryNamePrefix, int durationTicks, int amplifier) {
        this.registryNamePrefix = registryNamePrefix;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    public String registryNamePrefix() {
        return registryNamePrefix;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int amplifier() {
        return amplifier;
    }
}
