package com.pcagrade.retriever.card.onepiece.set;

import com.pcagrade.mason.jpa.revision.message.RevisionMessageConfiguration;
import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.localization.Localization;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.onepiece.OnePieceFieldMappingService;
import com.pcagrade.retriever.card.onepiece.set.translation.OnePieceSetTranslationMapper;
import com.pcagrade.retriever.card.onepiece.set.translation.OnePieceSetTranslationMapperImpl;
import com.pcagrade.retriever.card.onepiece.serie.OnePieceSerieTestConfig;
import com.pcagrade.retriever.card.onepiece.source.official.id.OnePieceOfficialSiteIdTestConfig;
import com.pcagrade.retriever.card.set.CardSetTranslation;
import com.pcagrade.retriever.field.mapper.FieldMappingService;
import com.pcagrade.retriever.field.mapper.FieldMappingTestConfig;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RetrieverTestConfiguration
@Import({RevisionMessageConfiguration.class, OnePieceOfficialSiteIdTestConfig.class, FieldMappingTestConfig.class, OnePieceSerieTestConfig.class})
public class OnePieceSetTestConfig {

    private static OnePieceSet op04() {
        var set = new OnePieceSet();

        set.setId(Ulid.from("01H1RZ6HHYCG2MXBZSZE7TDAB0"));
        set.setSerie(OnePieceSerieTestConfig.testSerie());
        var jp = new CardSetTranslation();

        jp.setName("Kingdoms of Intrigue");
        jp.setLocalization(Localization.JAPAN);
        set.setTranslation(Localization.JAPAN, jp);
        return set;
    }

    public static final Ulid OP01_ID = Ulid.from("01GXGP22SM1HXDR9GJW9NQDCB4");

    public static OnePieceSet op01() {
        var set = new OnePieceSet();

        set.setId(OP01_ID);
        set.setSerie(OnePieceSerieTestConfig.testSerie());
        set.setIdPca(10);
        return set;
    }

    public static final List<OnePieceSet> LIST = List.of(
            OnePieceSetTestProvider.op02(),
            op04(),
            op01()
    );

    @Bean
    public OnePieceFieldMappingService onePieceFieldMappingService(FieldMappingService fieldMappingService) {
        return new OnePieceFieldMappingService(fieldMappingService);
    }

    @Bean
    public OnePieceSetRepository onePieceSetRepository() {
        var mutableList = new ArrayList<>(LIST);
        var repository = RetrieverTestUtils.mockRepository(OnePieceSetRepository.class, mutableList, OnePieceSet::getId);

        Mockito.when(repository.save(Mockito.any())).then(invocation -> {
            var set = invocation.getArgument(0, OnePieceSet.class);
            if (set.getId() != null) {
                mutableList.removeIf(s -> Objects.equals(set.getId(), s.getId()));
            }
            mutableList.add(set);
            return set;
        });
        return repository;
    }

    @Bean
    public OnePieceSetMapper onePieceSetMapper() {
        return new OnePieceSetMapperImpl();
    }

    @Bean
    public OnePieceSetTranslationMapper onePieceSetTranslationMapper() {
        return new OnePieceSetTranslationMapperImpl();
    }

    @Bean
    public OnePieceSetService onePieceSetService() {
        return new OnePieceSetService();
    }
}
