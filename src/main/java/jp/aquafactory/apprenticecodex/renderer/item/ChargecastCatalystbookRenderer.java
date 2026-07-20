package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.model.ChargecastCatalystbookModel;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ChargecastCatalystbookRenderer extends GeoItemRenderer<ChargecastCatalystbook> {
    private final GeoModel<ChargecastCatalystbook> openModel = new ChargecastCatalystbookModel(true);
    private final GeoModel<ChargecastCatalystbook> closeModel = new ChargecastCatalystbookModel(false);

    public ChargecastCatalystbookRenderer() {
        super(new ChargecastCatalystbookModel(false));
    }

    @Override
    public GeoModel<ChargecastCatalystbook> getGeoModel() {
        return isHandheldPerspective(renderPerspective) ? openModel : closeModel;
    }

    private static boolean isHandheldPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
