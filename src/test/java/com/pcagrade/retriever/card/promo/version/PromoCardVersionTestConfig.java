package com.pcagrade.retriever.card.promo.version;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.TradingCardGame;
import com.pcagrade.retriever.card.promo.version.translation.PromoCardVersionTranslationMapperImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import java.util.List;

@RetrieverTestConfiguration
public class PromoCardVersionTestConfig {

    public static final Ulid STAFF_ID = UlidCreator.getUlid();

    private static PromoCardVersion staff() {
        var version = new PromoCardVersion();

        version.setId(STAFF_ID);
        version.setName("Staff");
        version.setTcg(TradingCardGame.POKEMON);
        return version;
    }

    private static PromoCardVersion firstPlace() {
        var version = new PromoCardVersion();

        version.setId(PromoCardVersionTestProvider.FIRST_PLACE_ID);
        version.setName("1st Place");
        version.setTcg(TradingCardGame.POKEMON);
        return version;
    }

    @Bean
    public PromoCardVersionRepository promoCardVersionRepository() {
        var list = List.of(
                staff(),
                firstPlace()
        );

        var repository = RetrieverTestUtils.mockRepository(PromoCardVersionRepository.class, list, PromoCardVersion::getId);

        Mockito.when(repository.findAllByTcg(Mockito.any(TradingCardGame.class))).then(i-> {
            var tcg = i.getArgument(0, TradingCardGame.class);

            return list.stream()
                    .filter(v -> v.getTcg() == tcg)
                    .toList();
        });
        return repository;
    }

    @Bean
    public PromoCardVersionTranslationMapperImpl promoCardVersionTranslationMapper() {
        return new PromoCardVersionTranslationMapperImpl();
    }

    @Bean
    public PromoCardVersionMapper promoCardVersionMapper() {
        return new PromoCardVersionMapperImpl();
    }

    @Bean
    public PromoCardVersionService promoCardVersionService() {
        return new PromoCardVersionService();
    }
}
