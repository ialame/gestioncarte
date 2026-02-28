package com.pcagrade.retriever.card.pokemon.source.pokellector;

import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import org.springframework.context.annotation.Bean;

@RetrieverTestConfiguration
public class PokellectorParserTestConfig {

    @Bean
    public PokellectorParser pokellectorParser() {
        return new PokellectorParser();
    }
}
