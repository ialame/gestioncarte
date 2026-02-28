package com.pcagrade.retriever.card.promo;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.pokemon.PokemonCard;
import com.pcagrade.retriever.card.pokemon.PokemonCardRepositoryTestConfig;
import com.pcagrade.retriever.card.pokemon.set.PokemonSet;
import com.pcagrade.retriever.card.pokemon.set.PokemonSetTestProvider;
import com.pcagrade.retriever.card.promo.event.PromoCardEventTestConfig;
import com.pcagrade.retriever.card.promo.version.PromoCardVersionTestConfig;
import com.pcagrade.retriever.card.set.CardSetTranslation;
import com.pcagrade.mason.localization.Localization;
import com.pcagrade.mason.ulid.UlidHelper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RetrieverTestConfiguration
@Import({ PokemonCardRepositoryTestConfig.class, PromoCardEventTestConfig.class, PromoCardVersionTestConfig.class })
public class PromoCardTestConfig {

    public static final List<PromoCard> PROMOS;

    static {
        var list = new ArrayList<>(PokemonCardRepositoryTestConfig.CARDS.stream()
                .<PromoCard>mapMulti((c, downstream) -> c.getPromoCards().forEach(downstream))
                .toList());
        list.add(createExtraPromo(Ulid.from("01H5W2EWJGMKT60WP8915VHECR"), "Cosmos Holo Darkness Ablaze stamp"));
        list.add(createExtraPromo(Ulid.from("01HHPRSJBNWC4PAG115R2GBRYK"), "Prize for October 2023 Meet-up event"));
        PROMOS = List.copyOf(list);
    }

    private static PromoCard createExtraPromo(Ulid id, String name) {
        var card = new PokemonCard();
        var set = new PokemonSet();

        set.setId(PokemonSetTestProvider.SWSH_ID);
        var us = new CardSetTranslation();
        us.setLocalization(Localization.USA);
        us.setName("Promo S&S");
        us.setAvailable(true);
        set.setTranslation(Localization.USA, us);
        card.setId(UlidCreator.getUlid());
        card.getCardSets().add(set);

        var promo = new PromoCard();

        promo.setId(id);
        promo.setLocalization(Localization.USA);
        promo.setName(name);
        promo.setCard(card);
        card.setPromoCards(Set.of(promo));
        return promo;
    }

    @Bean
    public PromoCardRepository promoCardRepository() {
        var repository = RetrieverTestUtils.mockRepository(PromoCardRepository.class, PROMOS, PromoCard::getId);

        Mockito.when(repository.findAllByCardId(Mockito.any(Ulid.class))).thenAnswer(invocation -> {
            var cardId = invocation.getArgument(0, Ulid.class);

            return PROMOS.stream()
                    .filter(p -> UlidHelper.equals(p.getCard().getId(), cardId))
                    .toList();
        });
        Mockito.when(repository.findAllByEventIdIsNull()).thenAnswer(invocation -> PROMOS.stream()
                .filter(p -> p.getEvent() == null)
                .toList());

        return repository;
    }

    @Bean
    public PromoCardMapper promoCardMapper() {
        return new PromoCardMapperImpl();
    }

    @Bean
    public PromoCardService promoCardService() {
        return new PromoCardService();
    }
}
