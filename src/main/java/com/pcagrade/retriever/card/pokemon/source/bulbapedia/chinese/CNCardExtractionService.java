package com.pcagrade.retriever.card.pokemon.source.bulbapedia.chinese;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.localization.Localization;
import com.pcagrade.retriever.card.pokemon.PokemonCardDTO;
import com.pcagrade.retriever.card.pokemon.set.PokemonSetRepository;
import com.pcagrade.retriever.card.pokemon.PokemonCardService;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.expansion.ExpansionBulbapedia;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.expansion.ExpansionBulbapediaRepository;
import com.pcagrade.retriever.card.pokemon.translation.PokemonCardTranslationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CNCardExtractionService {

    @Autowired
    private ExpansionBulbapediaRepository expansionBulbapediaRepository;
    @Autowired
    private PokemonSetRepository pokemonSetRepository;
    @Autowired
    private BulbapediaChineseScraper bulbapediaChineseScraper;
    @Autowired
    private PokemonCardService pokemonCardService;

    public List<BulbapediaChineseScraper.Card> scrapeCardsFromSet(ExpansionBulbapedia set) {

        String url = set.getUrl();
        String tableName = set.getTableName();
        String charset = set.getCharset();


        System.out.println("Scraping cards from: " + url);
        List<BulbapediaChineseScraper.Card> cards = bulbapediaChineseScraper.scrapeCardData(url, tableName, charset);

        if (!cards.isEmpty()) {
            bulbapediaChineseScraper.enrichCardsWithRarity(cards);
        }

        for (BulbapediaChineseScraper.Card card: cards) {
            findExistingUSSets(card);
        }

        return cards;
    }
    public void findExistingUSSets(BulbapediaChineseScraper.Card scrapedCard) {
        List<Ulid> setIds = new ArrayList<>();

        if (scrapedCard.getEnglishExpansions() != null) {
            for (BulbapediaChineseScraper.ExpansionInfo englishExpansion : scrapedCard.getEnglishExpansions()) {
                Optional<Ulid> englishSetIdOpt = pokemonSetRepository.findSetIdByExactUsNameOrLabelNameAsUlid(englishExpansion.getExpansionName());
                englishSetIdOpt.ifPresent(setIds::add);
            }
            scrapedCard.setSetIds(setIds);
        }
    }

    public int findExistingUsCard(BulbapediaChineseScraper.Card scrapedCard) {
        // Return -1 if no match found
        int index = -1;

        if (scrapedCard.getEnglishExpansions() != null) {
            // Use a traditional for-loop to track the index
            for (int i = 0; i < scrapedCard.getEnglishExpansions().size(); i++) {
                BulbapediaChineseScraper.ExpansionInfo englishExpansion = scrapedCard.getEnglishExpansions().get(i);

                // Check if English set exists in DB
                Optional<Ulid> englishSetIdOpt = pokemonSetRepository.findSetIdByExactUsNameOrLabelNameAsUlid(
                        englishExpansion.getExpansionName()
                );

                if (englishSetIdOpt.isPresent()) {
                    Ulid englishSetId = englishSetIdOpt.get();
                    // Create search DTO for this expansion
                    PokemonCardDTO searchDTO = createUsSearchDTO(scrapedCard, englishExpansion, englishSetIdOpt.get());

                    // Search for matching card in this set
                    List<PokemonCardDTO> cards = pokemonCardService.findSavedCards(searchDTO);

                    if (!cards.isEmpty()) {
                        List<Ulid> setIds = scrapedCard.getSetIds();
                        if (!setIds.contains(englishSetId)) {
                            setIds.add(englishSetId);

//                            // Get the actual found US card
                            PokemonCardDTO foundUsCard = cards.get(0);
//                            scrapedCard.setParentId(foundUsCard.getId());

                            // Add Chinese translation to it
                            expansionBulbapediaRepository.findBySetId(scrapedCard.getSetIds().getFirst())
                                    .ifPresent(chineseSet -> addChineseTranslationToUsCard(foundUsCard, scrapedCard, chineseSet));
                        }
                        return i;
                    }
                }
            }
        }

        return index; // Return -1 if no match found
    }

    private PokemonCardDTO createUsSearchDTO(BulbapediaChineseScraper.Card scrapedCard, BulbapediaChineseScraper.ExpansionInfo englishExpansion, Ulid englishSetId) {
        PokemonCardDTO searchDTO = new PokemonCardDTO();
        searchDTO.setSetIds(List.of(englishSetId));

        Map<Localization, PokemonCardTranslationDTO> searchTranslations = new HashMap<>();
        PokemonCardTranslationDTO searchTranslation = new PokemonCardTranslationDTO();
        searchTranslation.setName(scrapedCard.getName());
        searchTranslation.setNumber(englishExpansion.getCardNumber());
        searchTranslation.setLocalization(Localization.USA);
        searchTranslations.put(Localization.USA, searchTranslation);

        searchDTO.setTranslations(searchTranslations);
        return searchDTO;
    }

    private void addChineseTranslationToUsCard(PokemonCardDTO usCard, BulbapediaChineseScraper.Card scrapedCard, ExpansionBulbapedia setInfo) {
        Localization chineseLocalization = convertCharsetToLocalization(setInfo.getCharset());

        if (!usCard.getTranslations().containsKey(chineseLocalization)) {
            Map<Localization, PokemonCardTranslationDTO> mutableTranslations = new HashMap<>(usCard.getTranslations());
            PokemonCardTranslationDTO chineseTranslation = createChineseTranslation(scrapedCard, setInfo);
            mutableTranslations.put(chineseLocalization, chineseTranslation);
            usCard.setTranslations(mutableTranslations);
        }

        if (setInfo.getSet() != null && !usCard.getSetIds().contains(setInfo.getSet().getId())) {
            List<Ulid> mutableSetIds = new ArrayList<>(usCard.getSetIds());
            mutableSetIds.add(setInfo.getSet().getId());
            usCard.setSetIds(mutableSetIds);
        }

        pokemonCardService.save(usCard);
    }

    private PokemonCardTranslationDTO createChineseTranslation(BulbapediaChineseScraper.Card scrapedCard, ExpansionBulbapedia setInfo) {
        Localization chineseLocalization = convertCharsetToLocalization(setInfo.getCharset());
        PokemonCardTranslationDTO translation = new PokemonCardTranslationDTO();
        translation.setName(scrapedCard.getName());
        translation.setNumber(scrapedCard.getNumber());
        translation.setLabelName(scrapedCard.getName());
        translation.setLocalization(chineseLocalization);
        translation.setAvailable(true);
        translation.setRarity(cleanRarity(scrapedCard.getRarity()));
        return translation;
    }

    private String cleanRarity(String rarity) {
        if (rarity == null || "Rarity not found".equalsIgnoreCase(rarity.trim())) {
            return "";
        }
        String cleaned = rarity.replace("21px-", "").replace("Ultra-Rare Rare", "Ultra Rare").trim();
        return (cleaned.isEmpty() || cleaned.length() > 50) ? "" : cleaned;
    }

    private Localization convertCharsetToLocalization(String charset) {
        if (charset == null) return Localization.USA;
        return switch (charset.toLowerCase()) {
            case "zh" -> Localization.CHINA;
            case "cn" -> Localization.TAIWAN;
            default -> Localization.USA;
        };
    }
}
