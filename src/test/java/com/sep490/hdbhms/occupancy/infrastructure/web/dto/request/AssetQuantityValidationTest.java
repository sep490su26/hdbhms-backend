package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetQuantityValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void handoverAssetAcceptsZeroQuantityAndRejectsNegativeQuantity() {
        SubmitHandoverRequest.AssetInput asset = new SubmitHandoverRequest.AssetInput();
        asset.setAssetName("Điều hòa");
        asset.setAssetCategory("Thiết bị điện tử");
        asset.setCurrentCondition(AssetCondition.GOOD);
        asset.setQuantity(0);

        assertTrue(validator.validate(asset).isEmpty());

        asset.setQuantity(-1);
        assertEquals(1, validator.validate(asset).size());
    }

    @Test
    void roomAssetAcceptsZeroQuantityAndRejectsNegativeQuantity() {
        RoomAssetRequest zeroQuantity = new RoomAssetRequest(
                "Điều hòa",
                "Thiết bị điện tử",
                0,
                AssetCondition.GOOD,
                "",
                null
        );
        RoomAssetRequest negativeQuantity = new RoomAssetRequest(
                "Điều hòa",
                "Thiết bị điện tử",
                -1,
                AssetCondition.GOOD,
                "",
                null
        );

        assertTrue(validator.validate(zeroQuantity).isEmpty());
        assertEquals(1, validator.validate(negativeQuantity).size());
    }
}
