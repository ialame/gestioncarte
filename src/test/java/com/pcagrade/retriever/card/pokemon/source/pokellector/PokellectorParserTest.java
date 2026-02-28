package com.pcagrade.retriever.card.pokemon.source.pokellector;

import com.pcagrade.mason.localization.Localization;
import com.pcagrade.retriever.annotation.RetrieverTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@RetrieverTest(PokellectorParserTestConfig.class)
class PokellectorParserTest {

    @Autowired
    PokellectorParser pokellectorParser;

    @Test
    void getUrl_should_returnUrl() {
        var map = pokellectorParser.getImages("Paldean-Fates-Expansion", Localization.USA);

        assertThat(map).isNotEmpty();
    }

}
