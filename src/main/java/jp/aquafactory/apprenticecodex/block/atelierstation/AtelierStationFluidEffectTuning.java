package jp.aquafactory.apprenticecodex.block.atelierstation;

public final class AtelierStationFluidEffectTuning {
    private AtelierStationFluidEffectTuning() {
    }

    public static final float WATER_ALPHA = 0.78f;

    public static final float CAULDRON_ASCEND_TICKS = 5.0f;
    public static final float CAULDRON_HOVER_TICKS = 5.0f;
    public static final float CAULDRON_DASH_TICKS = 2.0f;
    public static final float CAULDRON_TOTAL_TICKS = CAULDRON_ASCEND_TICKS + CAULDRON_HOVER_TICKS + CAULDRON_DASH_TICKS;
    public static final float CAULDRON_START_DIAMETER = 8.0f / 16.0f;
    public static final float CAULDRON_PRE_DASH_DIAMETER = 6.0f / 16.0f;
    public static final float CAULDRON_DASH_END_DIAMETER = 1.5f / 16.0f;
    public static final float ALCHEMY_BREWER_PRE_DASH_DIAMETER = 4.0f / 16.0f;
    public static final float CAULDRON_ROTATE_X = 15.0f;
    public static final float CAULDRON_ROTATE_Y = 26.0f;
    public static final float CAULDRON_ROTATE_Z = 19.0f;

    public static final int SUPPLY_ORB_COUNT = 8;
    public static final float SUPPLY_CUBE_DIAMETER = 2.0f / 16.0f;
    public static final float SUPPLY_ASCEND_TICKS = 5.0f;
    public static final float SUPPLY_HOVER_TICKS = 5.0f;
    public static final float SUPPLY_DASH_TICKS = 2.0f;
    public static final int SUPPLY_TOTAL_TICKS =
            (int) (SUPPLY_ASCEND_TICKS + SUPPLY_HOVER_TICKS + SUPPLY_DASH_TICKS);
    public static final float SUPPLY_CONTROL_CUBE_HALF_EXTENT = 4.0f / 16.0f;
    public static final float SUPPLY_CONTROL_BASE_HEIGHT = 8.0f / 16.0f;
}
