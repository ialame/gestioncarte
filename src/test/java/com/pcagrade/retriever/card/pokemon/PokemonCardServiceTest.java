package com.pcagrade.retriever.card.pokemon;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.retriever.annotation.RetrieverTest;
import com.pcagrade.retriever.params.provider.JsonSource;
import com.pcagrade.mason.ulid.UlidHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@RetrieverTest(PokemonCardServiceTestConfig.class)
class PokemonCardServiceTest {

    @Autowired
    private IPokemonCardService pokemonCardService;

    @ParameterizedTest
    @JsonSource({"RET-174", "RET-202"})
    void save_should_saveCard(PokemonCardDTO card) {
        var id = pokemonCardService.save(card);
        var savedCard = pokemonCardService.getCardById(id);

        assertThat(savedCard).hasValueSatisfying(c -> assertThat(c).usingRecursiveComparison()
                .withComparatorForType(UlidHelper::compare, Ulid.class)
                .ignoringFields("id", "extractionStatus")
                .isEqualTo(card))
                .hasValueSatisfying(c -> assertThat(c.getId()).isNotNull().isEqualTo(id));
    }

}
