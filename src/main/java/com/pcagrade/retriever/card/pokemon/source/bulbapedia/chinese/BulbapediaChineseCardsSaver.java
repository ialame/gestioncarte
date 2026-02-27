package com.pcagrade.retriever.card.pokemon.source.bulbapedia.chinese;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.retriever.card.pokemon.PokemonCardDTO;
import com.pcagrade.retriever.card.pokemon.IPokemonCardService;
import com.pcagrade.retriever.card.pokemon.PokemonCardRepository;
import com.pcagrade.retriever.card.pokemon.set.PokemonSetRepository;
import com.pcagrade.retriever.card.pokemon.translation.PokemonCardTranslationDTO;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.chinese.BulbapediaChineseScraper.Card;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.expansion.ExpansionBulbapedia;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.expansion.ExpansionBulbapediaRepository;
import com.pcagrade.retriever.card.pokemon.source.bulbapedia.ExpansionBulbapediaCreatedEvent;
import com.pcagrade.mason.localization.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

//@Component
public class BulbapediaChineseCardsSaver {

//    @Autowired
    private IPokemonCardService pokemonCardService;

//    @Autowired
    private ExpansionBulbapediaRepository expansionBulbapediaRepository;

//    @Autowired
    private PokemonSetRepository pokemonSetRepository;

//    @Autowired
    private PokemonCardRepository pokemonCardRepository;

//    @Autowired
    private BulbapediaChineseScraper bulbapediaChineseScraper;

    @Value("${bulbapedia.chinese.auto-import-on-insert:true}")
    private boolean autoImportOnInsert;

    private final ReentrantLock importLock = new ReentrantLock();
    private volatile boolean isRunning = false;

    /**
     * Event listener that triggers when a new Chinese expansion is added
     * This runs asynchronously to avoid blocking the transaction that created the expansion
     */
    @Async
    @EventListener
    public void onExpansionCreated(ExpansionBulbapediaCreatedEvent event) {
        if (!autoImportOnInsert) {
            System.out.println(" Auto-import on insert is disabled");
            return;
        }

        if (!event.isChinese()) {
            System.out.println("ℹ️  Non-Chinese expansion detected, skipping auto-import");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("EVENT TRIGGERED: New Chinese expansion added!");
        System.out.println("Set ID: " + event.getSetId());
        System.out.println("Expansion ID: " + event.getExpansionId());
        System.out.println("Charset: " + event.getCharset());
        System.out.println("=".repeat(80));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executeImportForExpansion(event.getSetId());
    }

    /**
     * Import cards for a specific expansion (triggered by event)
     */
    private void executeImportForExpansion(Ulid setId) {
        if (!importLock.tryLock()) {
            System.out.println(" Import already running, skipping this execution");
            return;
        }

        try {
            isRunning = true;
            long startTime = System.currentTimeMillis();

            // Find the expansion by set ID using the new repository method
            Optional<ExpansionBulbapedia> expansionOpt = expansionBulbapediaRepository.findBySetId(setId);

            if (expansionOpt.isEmpty()) {
                System.out.println(" Expansion not found for set ID: " + setId);
                return;
            }

            ExpansionBulbapedia expansion = expansionOpt.get();

            // Check if expansion already has cards
            if (pokemonCardRepository.existsBySetId(setId)) {
                System.out.println("ℹ️  Set already has cards, skipping: " + expansion.getName());
                return;
            }

            System.out.println(" Starting import for expansion: " + expansion.getName() + " (Set: " + setId + ")");

            int successfulCards = 0;
            int failedCards = 0;

            List<Card> scrapedCards = scrapeCardsFromSet(expansion);
            System.out.println("Successfully scraped " + scrapedCards.size() + " cards from set");

            for (int i = 0; i < scrapedCards.size(); i++) {
                Card scrapedCard = scrapedCards.get(i);
                System.out.println("Processing card " + (i + 1) + "/" + scrapedCards.size() + ": " +
                        scrapedCard.getName() + " (" + scrapedCard.getNumber() + ")");

                try {
                    createAndSavePokemonCard(scrapedCard, expansion);
                    System.out.println("✓ Successfully processed card: " + scrapedCard.getName());
                    successfulCards++;
                } catch (Exception e) {
                    System.err.println("✗ Failed to process card: " + scrapedCard.getName() + " - " + e.getMessage());
                    e.printStackTrace();
                    failedCards++;
                }

                if (i < scrapedCards.size() - 1) {
                    Thread.sleep(500);
                }
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("\n" + "=".repeat(80));
            System.out.println("=== Import Summary for " + expansion.getName() + " ===");
            System.out.println("Total cards: " + scrapedCards.size());
            System.out.println("Successful: " + successfulCards);
            System.out.println("Failed: " + failedCards);
            System.out.println("Duration: " + duration + " seconds");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("!!! ERROR importing expansion for set " + setId + " !!!");
            e.printStackTrace();
        } finally {
            isRunning = false;
            importLock.unlock();
        }
    }

    private List<Card> scrapeCardsFromSet(ExpansionBulbapedia setInfo) {
        System.out.println("Scraping cards from: " + setInfo.getUrl());
        List<Card> cards = bulbapediaChineseScraper.scrapeCardData(setInfo.getUrl(), setInfo.getTableName(), setInfo.getCharset());

        if (!cards.isEmpty()) {
            bulbapediaChineseScraper.enrichCardsWithRarity(cards);
        }

        return cards;
    }

    private void createAndSavePokemonCard(Card scrapedCard, ExpansionBulbapedia setInfo) {
        List<PokemonCardDTO> existingUsCards = findExistingUsCards(scrapedCard);

        if (!existingUsCards.isEmpty()) {
            PokemonCardDTO firstCard = existingUsCards.get(0);
            addChineseTranslationToUsCard(firstCard, scrapedCard, setInfo);
        } else {
            createChineseCardOnly(scrapedCard, setInfo);
        }
    }

    private List<PokemonCardDTO> findExistingUsCards(Card scrapedCard) {
        List<PokemonCardDTO> existingUsCards = new ArrayList<>();

        if (scrapedCard.getEnglishExpansions() != null) {
            for (BulbapediaChineseScraper.ExpansionInfo englishExpansion : scrapedCard.getEnglishExpansions()) {
                Optional<Ulid> englishSetIdOpt = pokemonSetRepository.findSetIdByExactUsNameOrLabelNameAsUlid(englishExpansion.getExpansionName());

                if (englishSetIdOpt.isPresent()) {
                    PokemonCardDTO searchDTO = createUsSearchDTO(scrapedCard, englishExpansion, englishSetIdOpt.get());
                    List<PokemonCardDTO> cards = pokemonCardService.findSavedCards(searchDTO);
                    existingUsCards.addAll(cards);
                }
            }
        }
        return existingUsCards;
    }

    private PokemonCardDTO createUsSearchDTO(Card scrapedCard, BulbapediaChineseScraper.ExpansionInfo englishExpansion, Ulid englishSetId) {
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

    private void addChineseTranslationToUsCard(PokemonCardDTO usCard, Card scrapedCard, ExpansionBulbapedia setInfo) {
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

    private void createChineseCardOnly(Card scrapedCard, ExpansionBulbapedia setInfo) {
        if (cardAlreadyExists(scrapedCard, setInfo)) {
            return;
        }

        PokemonCardDTO chineseCard = new PokemonCardDTO();
        chineseCard.setType(convertToPokemonCardType(scrapedCard.getType()));
        chineseCard.setAlternate(false);
        chineseCard.setLevel(0);
        chineseCard.setBrackets(new ArrayList<>());
        chineseCard.setFeatureIds(new ArrayList<>());

        List<Ulid> setIds = new ArrayList<>();
        if (setInfo.getSet() != null) {
            setIds.add(setInfo.getSet().getId());
        }
        chineseCard.setSetIds(setIds);
        chineseCard.setPromos(new ArrayList<>());
        chineseCard.setTags(new ArrayList<>());

        Map<Localization, PokemonCardTranslationDTO> translations = new HashMap<>();
        Localization chineseLocalization = convertCharsetToLocalization(setInfo.getCharset());
        translations.put(chineseLocalization, createChineseTranslation(scrapedCard, setInfo));
        chineseCard.setTranslations(translations);

        pokemonCardService.save(chineseCard);
    }

    private PokemonCardTranslationDTO createChineseTranslation(Card scrapedCard, ExpansionBulbapedia setInfo) {
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

    private boolean cardAlreadyExists(Card scrapedCard, ExpansionBulbapedia setInfo) {
        if (setInfo.getSet() == null) return false;

        Localization mainLocalization = convertCharsetToLocalization(setInfo.getCharset());
        PokemonCardDTO searchDTO = new PokemonCardDTO();
        searchDTO.setSetIds(List.of(setInfo.getSet().getId()));

        Map<Localization, PokemonCardTranslationDTO> searchTranslations = new HashMap<>();
        PokemonCardTranslationDTO searchTranslation = new PokemonCardTranslationDTO();
        searchTranslation.setName(scrapedCard.getName());
        searchTranslation.setNumber(scrapedCard.getNumber());
        searchTranslation.setLocalization(mainLocalization);
        searchTranslations.put(mainLocalization, searchTranslation);
        searchDTO.setTranslations(searchTranslations);

        return !pokemonCardService.findSavedCards(searchDTO).isEmpty();
    }

    private Localization convertCharsetToLocalization(String charset) {
        if (charset == null) return Localization.USA;
        return switch (charset.toLowerCase()) {
            case "zh" -> Localization.CHINA;
            case "cn" -> Localization.TAIWAN;
            default -> Localization.USA;
        };
    }

    private String cleanRarity(String rarity) {
        if (rarity == null || "Rarity not found".equalsIgnoreCase(rarity.trim())) {
            return "";
        }
        String cleaned = rarity.replace("21px-", "").replace("Ultra-Rare Rare", "Ultra Rare").trim();
        return (cleaned.isEmpty() || cleaned.length() > 50) ? "" : cleaned;
    }

    private String convertToPokemonCardType(String scrapedType) {
        if (scrapedType == null || "unknown".equalsIgnoreCase(scrapedType.trim())) {
            return "";
        }
        return switch (scrapedType.toLowerCase()) {
            case "grass", "fire", "water", "lightning", "psychic", "fighting", "darkness", "metal", "colorless" -> "Pokémon";
            case "item" -> "Item";
            case "supporter" -> "Supporter";
            case "stadium" -> "Stadium";
            case "tool" -> "Tool";
            case "energy" -> "Energy";
            case "ace spec", "ace_spec" -> "ACE SPEC";
            default -> "";
        };
    }
}