package com.pcagrade.retriever.card.pokemon.source.bulbapedia.chinese;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.retriever.card.extraction.status.CardExtractionStatusDTO;
import com.pcagrade.retriever.card.pokemon.PokemonCardDTO;
import com.pcagrade.retriever.card.pokemon.PokemonCardHelper;
import com.pcagrade.retriever.card.pokemon.extraction.ExtractedPokemonCardDTO;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.extraction.BulbapediaExtractionStatus;
import com.pcagrade.retriever.card.pokemon.translation.PokemonCardTranslationDTO;
import com.pcagrade.retriever.card.pokemon.translation.SourcedPokemonCardTranslationDTO;
import com.pcagrade.mason.localization.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BulbapediaChineseToExtractedCardConverter {

    @Autowired
    private CNCardExtractionService cnCardExtractionService;

    // Main conversion method
    public ExtractedPokemonCardDTO convertToExtractedCard(
            BulbapediaChineseScraper.Card scrapedCard,
            Ulid setId, // ULID of the Pokemon set
//            boolean isPromo,
//            String promoValue,
            List<BulbapediaChineseScraper.ExpansionInfo> englishExpansions,
            Localization localization) {

        // ========== 1. CREATE PROCESSED POKEMON CARD ==========
        PokemonCardDTO processedCard = createPokemonCardDTO(scrapedCard, setId, englishExpansions, localization);
//        PokemonCardDTO processedCard = createPokemonCardDTO(scrapedCard, setId, isPromo, englishExpansions, charset);

        // ========== 2. CREATE RAW EXTRACTED CARD ==========
        PokemonCardDTO rawExtractedCard = createRawPokemonCardDTO(scrapedCard);

        // ========== 3. CREATE SOURCED TRANSLATIONS ==========
        List<SourcedPokemonCardTranslationDTO> sourcedTranslations = createSourcedTranslations(scrapedCard, localization);

        // ========== 4. CREATE AND POPULATE EXTRACTED CARD DTO ==========
        ExtractedPokemonCardDTO extractedCard = new ExtractedPokemonCardDTO();

        // Set basic properties
        extractedCard.setCard(processedCard);
        extractedCard.setRawExtractedCard(rawExtractedCard);
        extractedCard.setBulbapediaStatus(BulbapediaExtractionStatus.OK);
        extractedCard.setReviewed(false);

        // Set collections
        extractedCard.setSavedCards(new ArrayList<>()); // Will be populated by service
        extractedCard.setParentCards(new ArrayList<>()); // Will be populated if alternate card

        // Add translations
        extractedCard.setTranslations(sourcedTranslations);

        return extractedCard;
    }

    // Create the processed PokemonCardDTO
    private PokemonCardDTO createPokemonCardDTO(
            BulbapediaChineseScraper.Card scrapedCard,
            Ulid setId,
//            boolean isPromo,
            List<BulbapediaChineseScraper.ExpansionInfo> englishExpansions,
            Localization localization) {

        PokemonCardDTO card = new PokemonCardDTO();

        // Core identification
        card.setId(UlidCreator.getUlid());

        // Type information (from scraped card)
        String cardType = scrapedCard.getType();
        if (cardType != null) {
            card.setType(cardType);
        }

//        card.setParentId(scrapedCard.getParentId());
        // Artist will be filled later by other services
        card.setArtist(null);

        List<Ulid> setIds = new ArrayList<>();
        setIds.addFirst(setId);

        card.setSetIds(setIds);

        // Handle parent ID for alternate cards
        card.setParentId(null); // Will be set later if this is an alternate card

        // Create translations map
        Map<Localization, PokemonCardTranslationDTO> translations = createTranslations(scrapedCard, localization);
        card.setTranslations(translations);

        // Features (to be populated later by feature service)
        card.setFeatureIds(new ArrayList<>());

        // Card type flags
        card.setAlternate(PokemonCardHelper.isAlternate(scrapedCard.getNumber()));
        card.setFullArt(determineIfFullArt(scrapedCard.getRarity(), scrapedCard.getName()));

        // Distribution flag
        card.setDistribution(scrapedCard.isDistribution());

        // Level (if applicable - for Pokemon cards with levels)
        card.setLevel(0); // Default to 0, can be extracted from scraped data

        // Link (URL to card page)
        if (scrapedCard.getPageUrl() != null) {
            card.setLink(scrapedCard.getPageUrl());
        }

        // Safe initialization pattern
        CardExtractionStatusDTO extractionStatus = new CardExtractionStatusDTO();

        // Start with always-ignored localizations
        List<Localization> ignored = new ArrayList<>();
        ignored.add(Localization.JAPAN);
        ignored.add(Localization.KOREA);
        ignored.add(localization); // Add China or Taiwan based on charset

        // Add USA if English expansion is empty
        if (englishExpansions == null || englishExpansions.isEmpty()) {
            ignored.add(Localization.USA);
        }

        extractionStatus.setIgnoredLocalizations(ignored);

        return card;
    }

    // Create raw (unprocessed) PokemonCardDTO
    private PokemonCardDTO createRawPokemonCardDTO(
            BulbapediaChineseScraper.Card scrapedCard) {

        PokemonCardDTO rawCard = new PokemonCardDTO();

        // Minimal raw data
        rawCard.setId(UlidCreator.getUlid());
        rawCard.setType(scrapedCard.getType());
        rawCard.setLink(scrapedCard.getPageUrl());
        rawCard.setFullArt(determineIfFullArt(scrapedCard.getRarity(), scrapedCard.getName()));
        return rawCard;
    }

    // Create sourced translations
    private List<SourcedPokemonCardTranslationDTO> createSourcedTranslations(
            BulbapediaChineseScraper.Card scrapedCard, Localization localization) {

        List<SourcedPokemonCardTranslationDTO> sourcedTranslations = new ArrayList<>();

        // Chinese translation (from Bulbapedia)
        if (scrapedCard.getName() != null) {
            PokemonCardTranslationDTO chineseTranslation = createChineseTranslation(scrapedCard, localization);

            SourcedPokemonCardTranslationDTO chineseSourced = new SourcedPokemonCardTranslationDTO(
                    localization,
                    "bulbapedia",
                    5,
                    scrapedCard.getPageUrl(),
                    chineseTranslation
            );

            sourcedTranslations.add(chineseSourced);
        }

//        int i = cnCardExtractionService.findExistingUsCard(scrapedCard);
//
//        if (i != -1) {
//            // English translations from expansion info
//            PokemonCardTranslationDTO englishTranslation = createEnglishTranslation(scrapedCard, i);
//
//            SourcedPokemonCardTranslationDTO englishSourced = new SourcedPokemonCardTranslationDTO(
//                    Localization.USA,
//                    "bulbapedia",
//                    5,
//                    scrapedCard.getPageUrl(),
//                    englishTranslation
//            );
//            sourcedTranslations.add(englishSourced);
//        }

        return sourcedTranslations;
    }

    // Create translations map for processed card
    private Map<Localization, PokemonCardTranslationDTO> createTranslations(
            BulbapediaChineseScraper.Card scrapedCard, Localization localization) {


        Map<Localization, PokemonCardTranslationDTO> translations = new HashMap<>();

        // Chinese translation
        PokemonCardTranslationDTO chineseTranslation = createChineseTranslation(
                scrapedCard, localization); // Default to China variant for the main card
        translations.put(localization, chineseTranslation);

        // English translation(s) - use the first expansion or best match
//        if (englishExpansions != null && !englishExpansions.isEmpty()) {
//            BulbapediaChineseScraper.ExpansionInfo bestExpansion = findBestExpansion(englishExpansions);
        int i = cnCardExtractionService.findExistingUsCard(scrapedCard);

        if (i != -1) {
            PokemonCardTranslationDTO englishTranslation = createEnglishTranslation(
                    scrapedCard, i);
            translations.put(Localization.USA, englishTranslation);
        }

        return translations;
    }

    private PokemonCardTranslationDTO createChineseTranslation(
            BulbapediaChineseScraper.Card scrapedCard, Localization localization) {

        PokemonCardTranslationDTO translation = new PokemonCardTranslationDTO();
        translation.setLocalization(localization);
        translation.setName(scrapedCard.getName());
        translation.setLabelName(scrapedCard.getName());
        translation.setNumber(scrapedCard.getNumber());
        translation.setRarity(scrapedCard.getRarity());
        translation.setAvailable(true);
        return translation;
    }

    private PokemonCardTranslationDTO createEnglishTranslation(
            BulbapediaChineseScraper.Card scrapedCard,
            int index) {

        // Get the expansion at the specified index from scrapedCard
        BulbapediaChineseScraper.ExpansionInfo expansion = null;

        if (scrapedCard.getEnglishExpansions() != null &&
                index >= 0 && index < scrapedCard.getEnglishExpansions().size()) {

            // THIS IS THE KEY LINE - Get expansion at the index
            expansion = scrapedCard.getEnglishExpansions().get(index);
        }

        PokemonCardTranslationDTO translation = new PokemonCardTranslationDTO();
        translation.setNumber(expansion.getCardNumber());
        translation.setRarity(expansion.getRarity());
        translation.setLocalization(Localization.USA);
        translation.setName(scrapedCard.getName());
        translation.setLabelName(scrapedCard.getName());

        translation.setAvailable(true);
        return translation;
    }

    private boolean determineIfFullArt(String rarity, String cardName) {
        if (rarity == null) return false;

        // Check rarity indicators
        boolean isFullArtRarity = rarity.equals("Rare Ultra") ||
                rarity.equals("Rare Secret") ||
                rarity.equals("Rare Shiny") ||
                rarity.equals("Rare Shiny GX") ||
                rarity.equals("Ultra Rare") ||
                rarity.equals("Double Rare") ||
                rarity.equals("Secret Rare") ||
                rarity.equals("Shiny Secret Rare") ||
                rarity.equals("Hyper Rare") ||
                rarity.equals("Character Rare") ||
                rarity.equals("Art Rare");

        String cardNameLower = cardName != null ? cardName.toLowerCase() : "";
        boolean isFullArtName = cardNameLower.contains("full art") ||
                cardNameLower.contains("fa ") ||
                cardNameLower.contains(" fa") ||
                cardNameLower.contains(" fa ") ||
                cardNameLower.contains("character rare") ||
                cardNameLower.contains("art rare") ||
                cardNameLower.contains("secret rare") ||
                cardNameLower.contains("shiny secret rare") ||
                cardNameLower.contains("hyper rare") ||
                cardNameLower.contains("ultra rare") ||
                cardNameLower.contains("double rare");

        return isFullArtRarity || isFullArtName;
    }

    private BulbapediaChineseScraper.ExpansionInfo findBestExpansion(
            List<BulbapediaChineseScraper.ExpansionInfo> expansions) {

        // Return null if list is null or empty
        if (expansions == null || expansions.isEmpty()) {
            return null;
        }

        // Return the first expansion in the list
        return expansions.get(0);
    }

    // Batch conversion method
    public List<ExtractedPokemonCardDTO> convertToExtractedCards(
            List<BulbapediaChineseScraper.Card> scrapedCards,
            Ulid setId,
//            Map<String, Boolean> promoMap, // card number -> isPromo
//            Map<String, String> promoValueMap, // card number -> promoValue
//            Map<String, List<BulbapediaChineseScraper.ExpansionInfo>> expansionsMap,
            Localization localization) {

        List<ExtractedPokemonCardDTO> extractedCards = new ArrayList<>();

        for (BulbapediaChineseScraper.Card scrapedCard : scrapedCards) {
            String cardNumber = scrapedCard.getNumber();
//            boolean isPromo = promoMap.getOrDefault(cardNumber, scrapedCard.isPromo());
//            String promoValue = promoValueMap.getOrDefault(cardNumber, scrapedCard.getPromoValue());
            List<BulbapediaChineseScraper.ExpansionInfo> expansions = scrapedCard.getEnglishExpansions();

            ExtractedPokemonCardDTO extractedCard = convertToExtractedCard(
                    scrapedCard, setId, expansions, localization);

//            ExtractedPokemonCardDTO extractedCard = convertToExtractedCard(
//                    scrapedCard, setId, isPromo, promoValue, expansions, charset);

            extractedCards.add(extractedCard);
        }

        return extractedCards;
    }

    public Localization getLocalizationCharset(String charset) {

        Localization localization = null;
        if (charset.equals("cn")) {
            localization = Localization.TAIWAN;
        }

        else if (charset.equals("zh")) {
            localization = Localization.CHINA;
        }
        return localization;
    }
}