package com.pcagrade.retriever.card.pokemon.source.pokecardex;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Disabled("pokecardex.com now uses client-side rendering with encrypted data, Jsoup parser no longer works")
class PokecardexComParserTest {

    @Autowired
    private PokecardexComParser pokecardexComParser;

    @Test
    void parse_should_parseOBF() {
        var map = pokecardexComParser.parse("OBF");

        assertThat(map).isNotEmpty();
    }
}
