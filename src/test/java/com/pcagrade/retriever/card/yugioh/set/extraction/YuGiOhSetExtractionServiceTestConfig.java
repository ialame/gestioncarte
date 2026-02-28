package com.pcagrade.retriever.card.yugioh.set.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.yugioh.set.YuGiOhSetTestConfig;
import com.pcagrade.retriever.extraction.consolidation.ConsolidationServiceTestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@RetrieverTestConfiguration
@Import({YuGiOhSetSourceServiceTestConfig.class, YuGiOhSetTestConfig.class, ConsolidationServiceTestConfig.class})
public class YuGiOhSetExtractionServiceTestConfig {

    @Bean
    public YuGiOhSetExtractionService yuGiOhSetExtractionService(ObjectMapper objectMapper) {
        return new YuGiOhSetExtractionService(objectMapper);
    }
}
