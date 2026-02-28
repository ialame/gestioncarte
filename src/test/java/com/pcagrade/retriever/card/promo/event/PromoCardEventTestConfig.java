package com.pcagrade.retriever.card.promo.event;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.mason.jpa.merge.IMergeHistoryService;
import com.pcagrade.mason.jpa.revision.message.RevisionMessageService;
import com.pcagrade.mason.localization.Localization;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.TradingCardGame;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTrait;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitMapper;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitMapperImpl;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitMergeService;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitRepository;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitService;
import com.pcagrade.retriever.card.promo.event.trait.PromoCardEventTraitTestProvider;
import com.pcagrade.retriever.card.promo.event.trait.translation.PromoCardEventTraitTranslation;
import com.pcagrade.retriever.card.promo.event.trait.translation.PromoCardEventTraitTranslationMapper;
import com.pcagrade.retriever.card.promo.event.trait.translation.PromoCardEventTraitTranslationMapperImpl;
import com.pcagrade.retriever.card.promo.event.translation.PromoCardEventTranslationMapper;
import com.pcagrade.retriever.card.promo.event.translation.PromoCardEventTranslationMapperImpl;
import com.pcagrade.retriever.merge.MergeTestConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RetrieverTestConfiguration
@Import({MergeTestConfig.class})
public class PromoCardEventTestConfig {

    private static PromoCardEvent trainersToolkit2022() {
        var event = new PromoCardEvent();

        event.setId(PromoCardEventTestProvider.TRAINERS_TOOLKIT_2022_ID);
        event.setName("Trainer's Toolkit 2022");
        return event;
    }

    @Bean
    public PromoCardEventRepository promoCardEventRepository() {
        var list = new ArrayList<>(List.of(PromoCardEventTestProvider.testEvent(), trainersToolkit2022()));
        var repository = RetrieverTestUtils.mockRepository(PromoCardEventRepository.class, list, PromoCardEvent::getId);

        Mockito.when(repository.save(Mockito.any())).then(invocation -> {
            var event = invocation.getArgument(0, PromoCardEvent.class);

            if (event.getId() == null) {
                event.setId(UlidCreator.getUlid());
            }
            list.removeIf(e -> Objects.equals(event.getId(), e.getId()));
            list.add(event);
            return event;
        });
        return repository;
    }

    private static PromoCardEventTraitTranslation createTraitTranslation(Localization localization, String name, String labelName) {
        var translation = new PromoCardEventTraitTranslation();

        translation.setLocalization(localization);
        translation.setName(name);
        translation.setLabelName(labelName);
        return translation;
    }

    private static PromoCardEventTrait createTrait(Ulid id, String type, String name, TradingCardGame tcg, Localization localization, String translationName, String labelName) {
        var trait = new PromoCardEventTrait();

        trait.setId(id);
        trait.setType(type);
        trait.setName(name);
        trait.setTcg(tcg);
        trait.getTranslations().put(localization, createTraitTranslation(localization, translationName, labelName));
        return trait;
    }

    @Bean
    public PromoCardEventTraitRepository promoCardEventTraitRepository() {
        var crackedIceHolo = createTrait(PromoCardEventTraitTestProvider.CRACKED_IDE_HOLO_ID, PromoCardEventTrait.HOLO, "Cracked Ice Holo", TradingCardGame.POKEMON, Localization.USA, "Cracked Ice Holo", "Cracked Ice Holo");
        var pikachuPumpkin = createTrait(PromoCardEventTraitTestProvider.PIKACHU_PUMPKIN_ID, PromoCardEventTrait.EVENT, "Trick or Trade", TradingCardGame.POKEMON, Localization.USA, "Trick or Trade", "Trick or Trade");
        pikachuPumpkin.getTranslations().put(Localization.FRANCE, createTraitTranslation(Localization.FRANCE, "Trick or Trade", "Trick or Trade"));

        var giftWithPurchaseExclusive = createTrait(UlidCreator.getUlid(), PromoCardEventTrait.EXCLUSIVE, "gift with purchase exclusive", TradingCardGame.POKEMON, Localization.USA, "gift with purchase exclusive", "gift with purchase exclusive");
        var meetUpEvent = createTrait(UlidCreator.getUlid(), PromoCardEventTrait.EVENT, "Meet-up event", TradingCardGame.POKEMON, Localization.USA, "Meet-up event", "Meet-up event");

        var list = List.of(crackedIceHolo, pikachuPumpkin, giftWithPurchaseExclusive, meetUpEvent);

        var repository = RetrieverTestUtils.mockRepository(PromoCardEventTraitRepository.class, list, PromoCardEventTrait::getId);

        Mockito.when(repository.findAllByTcg(Mockito.any(TradingCardGame.class))).then(invocation -> {
            var tcg = invocation.getArgument(0, TradingCardGame.class);

            return list.stream()
                    .filter(t -> t.getTcg() == tcg)
                    .toList();
        });
        return repository;
    }

    @Bean
    public PromoCardEventMapper promoCardEventMapper() {
        return new PromoCardEventMapperImpl();
    }

    @Bean
    public PromoCardEventTranslationMapper promoCardEventTranslationMapper() {
        return new PromoCardEventTranslationMapperImpl();
    }

    @Bean
    public PromoCardEventTraitTranslationMapper promoCardEventTraitTranslationMapper() {
        return new PromoCardEventTraitTranslationMapperImpl();
    }

    @Bean
    public PromoCardEventTraitMapper promoCardEventTraitMapper() {
        return new PromoCardEventTraitMapperImpl();
    }

    @Bean
    public PromoCardEventTraitMergeService promoCardEventTraitMergeService(@Nonnull PromoCardEventTraitRepository promoCardEventTraitRepository, @Nonnull IMergeHistoryService<Ulid> mergeHistoryService, @Nullable RevisionMessageService revisionMessageService) {
        return new PromoCardEventTraitMergeService(promoCardEventTraitRepository, mergeHistoryService, revisionMessageService);
    }

    @Bean
    public PromoCardEventTraitService promoCardEventTraitService() {
        return new PromoCardEventTraitService();
    }

    @Bean
    public PromoCardEventService promoCardEventService() {
        return new PromoCardEventService();
    }
}
