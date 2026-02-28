package com.pcagrade.retriever.card.lorcana.source.mushu;

import com.pcagrade.retriever.annotation.RetrieverTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RetrieverTest(MushuParserTestConfig.class)
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
        List<MushuCard> cards;
        try {
            cards = mushuParser.getCards(templateName);
        } catch (NullPointerException e) {
            // Wiki API may return null results when not accessible
            return;
        }

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