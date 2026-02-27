package com.pcagrade.retriever.card.pokemon.source.pokecardex;

import com.pcagrade.retriever.annotation.RetrieverTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@RetrieverTest(PokecardexComTestConfig.class)
class PokecardexComParserTest {

    @Autowired
    private PokecardexComParser pokecardexComParser;

    @Test
    void parse_should_parseOBF() {
        var map = pokecardexComParser.parse("OBF");

        assertThat(map).isNotEmpty();
    }
}
