package com.pcagrade.retriever.card.yugioh.extraction;

import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.yugioh.source.official.OfficialSiteTestConfig;
import com.pcagrade.retriever.card.yugioh.source.ygoprodeck.YgoProDeckTestConfig;
import com.pcagrade.retriever.card.yugioh.source.yugipedia.YugipediaServiceTestConfig;
import org.springframework.context.annotation.Import;

@RetrieverTestConfiguration
@Import({OfficialSiteTestConfig.class, YgoProDeckTestConfig.class, YugipediaServiceTestConfig.class})
public class YuGiOhCardSourceServiceTestConfig {

}
