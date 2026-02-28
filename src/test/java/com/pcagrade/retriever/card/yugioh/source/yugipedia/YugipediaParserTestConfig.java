package com.pcagrade.retriever.card.yugioh.source.yugipedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.cache.CacheService;
import com.pcagrade.retriever.card.yugioh.YuGiOhFieldMappingServiceTestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@RetrieverTestConfiguration
@Import(YuGiOhFieldMappingServiceTestConfig.class)
public class YugipediaParserTestConfig {

    @Bean
    public YugipediaParser yugipediaParser(ObjectMapper objectMapper, CacheService cacheService) {
        return new YugipediaParser(objectMapper, cacheService);
    }
}
