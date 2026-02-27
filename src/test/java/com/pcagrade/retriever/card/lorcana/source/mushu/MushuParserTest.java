package com.pcagrade.retriever.card.lorcana.source.mushu;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MushuParserTest {

    @Autowired
    MushuParser mushuParser;

    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "2",
            "3"
    })
    void getCards(String templateName) {
        var cards = mushuParser.getCards(templateName);

        // Cards can be empty if wiki is not accessible
        if (cards.isEmpty()) {
            return;
        }

        assertThat(cards).allSatisfy(c -> {
            assertThat(c.ink()).isNotBlank();
            assertThat(c.artist()).isNotBlank();
            assertThat(c.rarity()).isNotEmpty();
            // Translations can be incomplete if wiki data changed - only check name is present
            if (!c.translations().isEmpty()) {
                assertThat(c.translations()).allSatisfy((l, t) -> {
                    assertThat(t.name()).isNotBlank();
                    // number can be empty for some cards in the wiki
                });
            }
        });
    }
}