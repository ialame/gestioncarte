package com.pcagrade.retriever.card.yugioh.source.yugipedia;

import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.yugioh.YuGiOhCardMapperTestConfig;
import com.pcagrade.retriever.card.yugioh.YuGiOhFieldMappingServiceTestConfig;
import com.pcagrade.retriever.card.yugioh.source.yugipedia.set.YugipediaSetTestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@RetrieverTestConfiguration
@Import({YuGiOhCardMapperTestConfig.class, YugipediaSetTestConfig.class, YuGiOhFieldMappingServiceTestConfig.class})
public class YugipediaServiceTestConfig {

    @Bean
    public YugipediaParser yugipediaParser(com.fasterxml.jackson.databind.ObjectMapper objectMapper, com.pcagrade.retriever.cache.CacheService cacheService) {
        return new YugipediaParser(objectMapper, cacheService);
    }

    @Bean
    public YugipediaMapper yugipediaMapper() {
        return new YugipediaMapperImpl();
    }

    @Bean
    public YugipediaService yugipediaService() {
        return new YugipediaService();
    }
}
