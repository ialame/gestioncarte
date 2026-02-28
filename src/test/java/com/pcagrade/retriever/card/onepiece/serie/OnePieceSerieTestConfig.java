package com.pcagrade.retriever.card.onepiece.serie;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.onepiece.serie.translation.OnePieceSerieTranslationMapper;
import com.pcagrade.retriever.card.onepiece.serie.translation.OnePieceSerieTranslationMapperImpl;
import org.springframework.context.annotation.Bean;

import java.util.List;

@RetrieverTestConfiguration
public class OnePieceSerieTestConfig {

    public static final Ulid TEST_SERIE_ID = Ulid.from("01GXGP22SM1HXDR9GJW9NQDAAA");

    public static OnePieceSerie testSerie() {
        var serie = new OnePieceSerie();

        serie.setId(TEST_SERIE_ID);
        return serie;
    }

    @Bean
    public OnePieceSerieRepository onePieceSerieRepository() {
        return RetrieverTestUtils.mockRepository(OnePieceSerieRepository.class, List.of(testSerie()), OnePieceSerie::getId);
    }

    @Bean
    public OnePieceSerieTranslationMapper onePieceSerieTranslationMapper() {
        return new OnePieceSerieTranslationMapperImpl();
    }

    @Bean
    public OnePieceSerieMapper onePieceSerieMapper() {
        return new OnePieceSerieMapperImpl();
    }

    @Bean
    public OnePieceSerieService onePieceSerieService() {
        return new OnePieceSerieService();
    }
}
