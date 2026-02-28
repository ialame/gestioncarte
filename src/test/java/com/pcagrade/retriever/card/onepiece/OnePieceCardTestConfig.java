package com.pcagrade.retriever.card.onepiece;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.jpa.merge.IMergeHistoryService;
import com.pcagrade.mason.jpa.revision.message.RevisionMessageService;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.CardRepository;
import com.pcagrade.retriever.card.artist.CardArtistTestConfig;
import com.pcagrade.retriever.card.extraction.status.CardExtractionStatusTestConfig;
import com.pcagrade.retriever.card.onepiece.serie.OnePieceSerieTestConfig;
import com.pcagrade.retriever.card.onepiece.set.OnePieceSetTestConfig;
import com.pcagrade.retriever.card.onepiece.translation.OnePieceCardTranslationMapper;
import com.pcagrade.retriever.card.onepiece.translation.OnePieceCardTranslationMapperImpl;
import com.pcagrade.retriever.card.promo.PromoCardMapper;
import com.pcagrade.retriever.card.promo.PromoCardRepository;
import com.pcagrade.retriever.card.promo.PromoCardService;
import com.pcagrade.retriever.card.promo.event.PromoCardEventRepository;
import com.pcagrade.retriever.card.promo.version.PromoCardVersionRepository;
import com.pcagrade.retriever.merge.MergeTestConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.text.similarity.SimilarityScore;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RetrieverTestConfiguration
@Import({OnePieceSerieTestConfig.class, OnePieceSetTestConfig.class, CardExtractionStatusTestConfig.class, CardArtistTestConfig.class, MergeTestConfig.class})
public class OnePieceCardTestConfig {

    public static final Ulid OP01_001_ID = Ulid.from("01GXGPC560410PARKG79ZRA6G6");

    private static OnePieceCard op01001() {
        var card = new OnePieceCard();

        card.setId(OP01_001_ID);
        card.setNumber("OP01-001");
        card.getCardSets().add(OnePieceSetTestConfig.op01());
        return card;
    }

    @Bean
    public OnePieceCardRepository onePieceCardRepository() {
        var mutableList = new ArrayList<>(List.of(op01001()));
        var repository = RetrieverTestUtils.mockRepository(OnePieceCardRepository.class, mutableList, OnePieceCard::getId);

        Mockito.when(repository.findAllBySetIdAndNumber(Mockito.any(Ulid.class), Mockito.anyString())).thenReturn(Collections.emptyList());
        Mockito.when(repository.findInSet(Mockito.any(Ulid.class))).thenAnswer(invocation -> {
            var setId = invocation.getArgument(0, Ulid.class);

            return mutableList.stream()
                    .filter(c -> c.getCardSets().stream().anyMatch(s -> s.getId().equals(setId)))
                    .toList();
        });
        Mockito.when(repository.save(Mockito.any())).then(invocation -> {
            var card = invocation.getArgument(0, OnePieceCard.class);

            if (card.getId() != null) {
                mutableList.removeIf(c -> Objects.equals(card.getId(), c.getId()));
            }
            mutableList.add(card);
            return card;
        });
        return repository;
    }

    @Bean
    public OnePieceCardTranslationMapper onePieceCardTranslationMapper() {
        return new OnePieceCardTranslationMapperImpl();
    }

    @Bean
    public CardRepository cardRepository() {
        return Mockito.mock(CardRepository.class);
    }

    @Bean
    public PromoCardRepository promoCardRepository() {
        return Mockito.mock(PromoCardRepository.class);
    }

    @Bean
    public PromoCardVersionRepository promoCardVersionRepository() {
        return Mockito.mock(PromoCardVersionRepository.class);
    }

    @Bean
    public PromoCardEventRepository promoCardEventRepository() {
        return Mockito.mock(PromoCardEventRepository.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public SimilarityScore<Double> similarityScore() {
        return Mockito.mock(SimilarityScore.class);
    }

    @Bean
    public PromoCardMapper promoCardMapper() {
        return Mockito.mock(PromoCardMapper.class);
    }

    @Bean
    public PromoCardService promoCardService() {
        return Mockito.mock(PromoCardService.class);
    }

    @Bean
    public OnePieceCardMapper onePieceCardMapper() {
        return new OnePieceCardMapperImpl();
    }

    @Bean
    public OnePieceCardMergeService onePieceCardMergeService(@Nonnull OnePieceCardRepository onePieceCardRepository, @Nonnull IMergeHistoryService<Ulid> mergeHistoryService, @Nullable RevisionMessageService revisionMessageService) {
        return new OnePieceCardMergeService(onePieceCardRepository, mergeHistoryService, revisionMessageService);
    }

    @Bean
    public OnePieceCardService onePieceCardService() {
        return new OnePieceCardService();
    }
}
