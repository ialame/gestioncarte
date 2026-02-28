package com.pcagrade.retriever.card.promo.version;

import com.pcagrade.retriever.annotation.RetrieverTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@RetrieverTest(PromoCardVersionTestConfig.class)
class PromoCardVersionServiceTest {

    @Autowired
    private PromoCardVersionService promoCardVersionService;

    @Test
    void findAll_shouldNot_beEmpty() {
        var all = promoCardVersionService.findAll();

        assertThat(all).isNotEmpty();
    }

    @Test
    void findById_should_haveValue() {
        var opt = promoCardVersionService.findById(PromoCardVersionTestProvider.FIRST_PLACE_ID);

        assertThat(opt).isNotEmpty();
    }

}
