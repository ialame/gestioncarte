package com.pcagrade.retriever.card.pokemon.source.bulbapedia.chinese;

import com.github.f4b6a3.ulid.Ulid;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulbapediaChineseScraper {

    // Configuration constants
    private int MAX_RETRIES = 3;
    private int INITIAL_RETRY_DELAY_MS = 2000;
    private int MAX_RETRY_DELAY_MS = 10000;
    private int BATCH_SIZE = 10;
    private int BATCH_DELAY_MS = 5000;
    private int REQUEST_DELAY_MS = 1500;
    private int FAILURE_DELAY_MS = 3000;

    @Cacheable("bulbapediaCards")
    public List<Card> scrapeCardData(String url, String tableTitle, String charset) {
        System.out.println("=== CACHE MISS - Fetching from network ===");
        System.out.println("URL: " + url);
        System.out.println("Table Title: " + tableTitle);

        return scrapeCardDataInternal(url, tableTitle, new String[]{
                "Card list",
                "Set list",
                "Set lists",
                "Deck list",
                "Deck lists",
                "Deck structure",
        }, charset);
    }

    // Add this new private method
    private List<Card> scrapeCardDataInternal(String url, String tableTitle, String[] desiredTitles, String charset) {
        List<Card> cards = new ArrayList<>();

        try {
            System.out.println("Connecting to: " + url);
            Document doc = executeWithRetry(() ->
                    Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .referrer("https://bulbapedia.bulbagarden.net/")
                            .timeout(30000)
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .get()
            );

            System.out.println("Successfully retrieved document, extracting card data...");
            cards = scrapeCardDataFromDocument(doc, tableTitle);
            System.out.println("Found " + cards.size() + " cards");

        } catch (IOException e) {
            System.err.println("Error scraping card data: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }

        return cards;
    }

    public List<Card> scrapeCardData(String url, String tableTitle, String[] desiredTitles, String charset) {
        // Just call the internal method
        return scrapeCardDataInternal(url, tableTitle, desiredTitles, charset);
    }


    public List<Card> scrapeCardDataFromDocument(Document doc, String tableTitle) {
        List<Card> cards = new ArrayList<>();

        // Find all tables and check each one
        Elements allTables = doc.select("table");
        System.out.println("Found " + allTables.size() + " tables in document");

        for (int i = 0; i < allTables.size(); i++) {
            Element table = allTables.get(i);

            // Check if this table contains our target group
            Elements bigBTags = table.select("big b");
            for (Element bigB : bigBTags) {
                String title = bigB.text().trim();
                if (title.equals(tableTitle)) {
                    System.out.println("Found target table with title: " + tableTitle);
                    // Extract cards from this specific section
                    cards = extractCardsFromTableSection(table, tableTitle);

                    if (cards.size() > 0) {
                        System.out.println("Successfully extracted " + cards.size() + " cards from table");
                        return cards;
                    }
                }
            }
        }

        // Check if this is an "Additional cards" table
        boolean isDistribution = "Additional cards".equals(tableTitle);
        if (isDistribution) {
            for (Card card : cards) {
                card.setDistribution(true);
            }
        }
        System.out.println("No cards found in document");

        return cards;
    }

    private boolean detectPromotionColumn(Element table) {
        // Look for "Promotion" in header rows
        Elements headerRows = table.select("tr");
        for (Element headerRow : headerRows) {
            Elements headers = headerRow.select("th");
            for (Element header : headers) {
                if (header.text().trim().equalsIgnoreCase("Promotion")) {
                    System.out.println("Detected Promotion column in table");
                    return true;
                }
            }
        }
        return false;
    }

    private int findPromotionColumnIndex(Element table) {
        // Look for the Promotion header and determine its column index
        Elements headerRows = table.select("tr");
        for (Element headerRow : headerRows) {
            Elements headers = headerRow.select("th");
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).text().trim().equalsIgnoreCase("Promotion")) {
                    int columnIndex = Math.max(0, i - 1);
                    System.out.println("Promotion column index: " + columnIndex);
                    return columnIndex;
                }
            }
        }
        return -1;
    }

    private Card extractCardFromRow(Element row, boolean hasPromotionColumn, int promotionColumnIndex) {
        try {
            Elements cells = row.select("td");

            // Skip header rows and rows with insufficient data
            if (cells.size() < 4) {
                return null;
            }

            String number = cells.get(0).text().trim();
            String nameCell = cells.get(2).html();
            String nameText = cells.get(2).text().trim();

            // Skip rows that contain header-like content or energy rows
            if (!isValidCardRow(number, nameText)) {
                return null;
            }

            Card card = new Card();
            card.setNumber(number);

            String cardName = extractCardName(nameCell);
            card.setName(cardName);

            String cardType = extractCardTypeFromRow(row);
            card.setType(cardType);

            // Extract card page URL from the name link
            String cardPageUrl = extractCardPageUrl(cells.get(2));
            card.setPageUrl(cardPageUrl);

            // Detect promo status and value
            PromoInfo promoInfo = detectPromoCard(row, hasPromotionColumn, promotionColumnIndex);
            card.setPromo(promoInfo.isPromo);
            card.setPromoValue(promoInfo.promoValue);

            return card;

        } catch (Exception e) {
            return null;
        }
    }

    private String extractCardPageUrl(Element nameCell) {
        try {
            // Look for <a> tag within the name cell
            Elements linkTags = nameCell.select("a");

            for (Element link : linkTags) {
                String href = link.attr("href");
                if (href != null && !href.trim().isEmpty() && !href.startsWith("#")) {
                    // Construct full URL - ensure proper formatting
                    String fullUrl;
                    if (href.startsWith("//")) {
                        fullUrl = "https:" + href;
                    } else if (href.startsWith("/")) {
                        fullUrl = "https://bulbapedia.bulbagarden.net" + href;
                    } else if (href.startsWith("http")) {
                        fullUrl = href; // Already full URL
                    } else {
                        fullUrl = "https://bulbapedia.bulbagarden.net/" + href;
                    }

                    return fullUrl;
                }
            }
        } catch (Exception e) {
            // Silent fail
        }

        return null;
    }

    private String extractCardName(String nameCellHtml) {
        return Jsoup.parse(nameCellHtml).text().trim();
    }

    private String extractCardTypeFromRow(Element row) {
        try {
            // FIRST: Check for type in TH elements (this is where the type codes actually are)
            Elements thCells = row.select("th");
            if (!thCells.isEmpty()) {
                Element typeCell = thCells.first();
                String typeText = typeCell.text().trim();
                String typeHtml = typeCell.html();

                // Check for Trainer types
                if (typeText.equals("I")) {
                    return "Item";
                }
                if (typeText.equals("Su")) {
                    return "Supporter";
                }
                if (typeText.equals("St")) {
                    return "Stadium";
                }
                if (typeText.equals("T")) {
                    return "Tool";
                }
                if (typeText.equals("ACE")) {
                    return "ACE SPEC";
                }
                if (typeText.equals("E")) {
                    return "Energy";
                }

                // Check HTML content directly
                if (typeHtml.contains(">I<") || typeHtml.contains(">I</") || typeHtml.contains(">I<")) {
                    return "Item";
                }
                if (typeHtml.contains(">Su<") || typeHtml.contains(">Su</") || typeHtml.contains(">Su<")) {
                    return "Supporter";
                }
            }

            // SECOND: Fallback to checking TD elements (original logic)
            Elements dataCells = row.select("td");
            if (dataCells.size() >= 4) {
                Element typeCell = dataCells.get(3);
                String typeText = typeCell.text().trim();
                String ownText = typeCell.ownText().trim();
                String cleanType = typeText.replace("?", "").replace("×", "").trim();

                // Check for Trainer types
                if (cleanType.equals("I") || typeText.contains("I") || ownText.contains("I")) {
                    return "Item";
                }
                if (cleanType.equals("Su") || typeText.contains("Su") || ownText.contains("Su")) {
                    return "Supporter";
                }
                if (cleanType.equals("St") || typeText.contains("St") || ownText.contains("St")) {
                    return "Stadium";
                }
                if (cleanType.equals("T") || typeText.contains("T") || ownText.contains("T")) {
                    return "Tool";
                }
                if (cleanType.equals("ACE") || typeText.contains("ACE")) {
                    return "ACE SPEC";
                }
                if (cleanType.equals("E") || typeText.contains("E")) {
                    return "Energy";
                }

                // Check HTML content directly
                String typeHtml = typeCell.html();
                if (typeHtml.contains(">I<") || typeHtml.contains(">I</")) {
                    return "Item";
                }
                if (typeHtml.contains(">Su<") || typeHtml.contains(">Su</")) {
                    return "Supporter";
                }
            }

            // THIRD: Check for energy types (works for both th and td)
            String rowHtml = row.html();
            if (rowHtml.contains("Grass-attack.png")) return "Grass";
            if (rowHtml.contains("Fire-attack.png")) return "Fire";
            if (rowHtml.contains("Water-attack.png")) return "Water";
            if (rowHtml.contains("Lightning-attack.png")) return "Lightning";
            if (rowHtml.contains("Psychic-attack.png")) return "Psychic";
            if (rowHtml.contains("Fighting-attack.png")) return "Fighting";
            if (rowHtml.contains("Darkness-attack.png")) return "Darkness";
            if (rowHtml.contains("Metal-attack.png")) return "Metal";
            if (rowHtml.contains("Colorless-attack.png")) return "Colorless";

            // FOURTH: Check background colors as final fallback
            if (rowHtml.contains("0273C0")) return "Item";     // Blue background for Items
            if (rowHtml.contains("F05A22")) return "Supporter"; // Orange background for Supporters

        } catch (Exception e) {
            // Silent fail
        }

        return "Unknown";
    }

    private PromoInfo detectPromoCard(Element row, boolean hasPromotionColumn, int promotionColumnIndex) {
        if (!hasPromotionColumn || promotionColumnIndex == -1) {
            return new PromoInfo(false, null);
        }

        try {
            // Get the promotion cell content
            Elements cells = row.select("td");
            if (promotionColumnIndex < cells.size()) {
                Element promoCell = cells.get(promotionColumnIndex);
                String promoText = promoCell.text().trim();

                // If promotion cell has meaningful content, it's a promo card
                // EXCEPT for "Promotion" and "PromotionRC" which are NOT considered promos
                boolean isPromo = !promoText.isEmpty() &&
                        !promoText.equals("—") &&
                        !promoText.equals("-") &&
                        !promoText.equalsIgnoreCase("no") &&
                        !promoText.equalsIgnoreCase("none") &&
                        !promoText.equals("Promotion") &&
                        !promoText.equals("PromotionRC");

                if (isPromo) {
                    System.out.println("Detected promo card with value: " + promoText);
                }

                return new PromoInfo(isPromo, isPromo ? promoText : null);
            }
        } catch (Exception e) {
            // Silent fail
        }

        return new PromoInfo(false, null);
    }

    private boolean isValidCardRow(String number, String name) {
        // Skip empty or invalid names
        if (name.isEmpty() ||
                name.equals("Box art") ||
                name.matches(".*\\{\\{.*\\}\\}.*") ||
                name.contains("—") || // Skip energy type indicators
                name.contains("×") || // Skip quantity indicators
                name.contains("Energy") ||
                name.contains("Mark") ||
                name.contains("Card") ||
                name.contains("Type") ||
                name.contains("Quantity") ||
                name.contains("Group") ||
                name.contains("Any")) {
            return false;
        }

        // Skip invalid numbers
        if (number == null || number.trim().isEmpty()) {
            return false;
        }

        // Skip energy codes and header-like numbers
        if (number.matches("[A-Z]{3}") || // Energy codes like "GRA", "FIR"
                number.contains("Group") ||
                number.contains("Any") ||
                number.contains("—") ||
                number.contains("×")) {
            return false;
        }

        // Accept various card number formats:
        // - Standard: "001/164", "001"
        // - Promo: "001/SM-P", "SM-P", "SVP"
        // - Special: "P001", "SV-P", etc.
        // - Any string that's not obviously a header or energy code
        return name.length() > 1 && number.length() > 0;
    }

    public void enrichCardsWithRarity(List<Card> cards) {
        System.out.println("Starting rarity enrichment for " + cards.size() + " cards");

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            System.out.println("Processing card " + (i + 1) + "/" + cards.size() + ": " + card.getName());

            if (card.getPageUrl() != null && !card.getPageUrl().isEmpty()) {
                try {
                    // Scrape with SPECIFIC rarity information
                    System.out.println("Fetching details from: " + card.getPageUrl());
                    CardDetails details = scrapeCardDetailsWithRetry(card.getPageUrl());
                    card.setRarity(details.rarity);
                    card.setEnglishExpansions(details.englishExpansions);

                    System.out.println("Rarity: " + details.rarity + ", Expansions: " + details.englishExpansions.size());

                    // Batch rate limiting
                    if ((i + 1) % BATCH_SIZE == 0 && i < cards.size() - 1) {
                        System.out.println("Batch limit reached, waiting " + BATCH_DELAY_MS + "ms...");
                        Thread.sleep(BATCH_DELAY_MS);
                    } else {
                        Thread.sleep(REQUEST_DELAY_MS);
                    }

                } catch (Exception e) {
                    System.err.println("Error enriching card " + card.getName() + ": " + e.getMessage());
                    card.setRarity("Error: " + e.getMessage());
                    try {
                        Thread.sleep(FAILURE_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                System.out.println("No URL available for card: " + card.getName());
                card.setRarity("No URL available");
            }
        }

        System.out.println("Completed rarity enrichment");
    }

    public CardDetails scrapeCardDetailsWithRetry(String cardUrl) throws IOException {
        System.out.println("Scraping card details with retry: " + cardUrl);
        return executeWithRetry(() -> scrapeCardDetails(cardUrl));
    }

    public CardDetails scrapeCardDetails(String cardUrl) {
        try {
            System.out.println("Fetching card details: " + cardUrl);
            Document cardDoc = Jsoup.connect(cardUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .referrer("https://bulbapedia.bulbagarden.net/")
                    .timeout(30000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();

            return extractCardDetailsFromDocument(cardDoc);

        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to card page: " + e.getMessage());
        }
    }

    private CardDetails extractCardDetailsFromDocument(Document cardDoc) {
        System.out.println("Extracting card details from document");
        CardDetails details = new CardDetails();

        // Extract main rarity (fallback)
        details.rarity = extractRarityFromDocument(cardDoc);

        // Extract English expansions WITH their specific rarities
        details.englishExpansions = extractEnglishExpansionsWithRarityFromDocument(cardDoc);

        System.out.println("Extracted rarity: " + details.rarity + ", expansions: " + details.englishExpansions.size());
        return details;
    }

    // Extract English expansions with their specific rarities
    private List<ExpansionInfo> extractEnglishExpansionsWithRarityFromDocument(Document cardDoc) {
        List<ExpansionInfo> expansions = new ArrayList<>();

        // Look for all tables with border=1 and specific structure (these are the expansion tables)
        Elements expansionTables = cardDoc.select("table[border='1'][width='100%']");
        System.out.println(" Expansion tables found: ");

        for (Element table : expansionTables) {
            ExpansionInfo expansion = extractExpansionInfoFromTable(table);
            if (expansion != null && expansion.isEnglishExpansion()) {
                expansions.add(expansion);
            }
        }
        return expansions;
    }

    // Extract expansion information including SPECIFIC rarity from a table
    private ExpansionInfo extractExpansionInfoFromTable(Element table) {
        Elements rows = table.select("tr");

        String expansionName = null;
        String cardNumber = null;
        String rarity = null;
        boolean isEnglishExpansion = false;

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() == 2) {
                String label = cells.get(0).text().trim();
                String value = cells.get(1).text().trim();

                if (label.equals("English expansion") || label.equals("Expansion")) {
                    expansionName = value;
                    isEnglishExpansion = label.equals("English expansion");
                } else if (label.equals("English card no.") || label.equals("Card no.") ||
                        label.equals("Card number") || label.equals("English card number")) {
                    cardNumber = value;
                } else if (label.equals("Rarity")) {
                    // Extract SPECIFIC rarity from the cell
                    rarity = extractSpecificRarityFromCell(cells.get(1));
                }
            }
        }

        if (expansionName != null && cardNumber != null) {
            ExpansionInfo info = new ExpansionInfo(expansionName, cardNumber, isEnglishExpansion);
            info.setRarity(rarity);
            System.out.println("Found expansion: " + expansionName + ", rarity: " + rarity);
            return info;
        }

        return null;
    }

    // Extract SPECIFIC rarity from cell with detailed analysis
    private String extractSpecificRarityFromCell(Element rarityCell) {
        // Method 1: Extract from image src (MOST SPECIFIC)
        String rarityFromImage = extractSpecificRarityFromImage(rarityCell);
        if (rarityFromImage != null) {
            return rarityFromImage;
        }

        // Method 2: Extract from alt text of images
        Elements images = rarityCell.select("img");
        for (Element img : images) {
            String altText = img.attr("alt");
            if (altText != null && !altText.isEmpty() && !altText.equals("Rarity")) {
                return convertRarityAltToName(altText);
            }
        }

        // Method 3: Extract from title attribute
        for (Element img : images) {
            String title = img.attr("title");
            if (title != null && !title.isEmpty() && !title.equals("Rarity")) {
                return convertRarityTitleToName(title);
            }
        }

        // Method 4: Fallback to text content
        String rarityText = rarityCell.text().trim();
        if (!rarityText.isEmpty() && !rarityText.equals("Rarity")) {
            return rarityText;
        }

        return "Unknown Rarity";
    }

    // Extract SPECIFIC rarity from image source
    private String extractSpecificRarityFromImage(Element rarityCell) {
        Elements images = rarityCell.select("img[src*=\"Rarity\"]");

        for (Element img : images) {
            String imgSrc = img.attr("src");
            if (imgSrc.contains("Rarity_")) {
                return extractDetailedRarityFromImageSrc(imgSrc);
            }
        }

        return null;
    }

    // Extract detailed rarity from image source URL
    private String extractDetailedRarityFromImageSrc(String imgSrc) {
        // Extract filename from URL
        String filename = imgSrc.substring(imgSrc.lastIndexOf("/") + 1);
        String rarityCode = filename.replace("Rarity_", "").replace(".png", "");

        // Map to specific rarity names based on common patterns
        return convertToSpecificRarityName(rarityCode);
    }

    // Convert rarity code to specific name
    private String convertToSpecificRarityName(String rarityCode) {
        // Comprehensive mapping of rarity codes to specific names
        switch (rarityCode) {
            case "Common": return "Common";
            case "Uncommon": return "Uncommon";
            case "Rare": return "Rare";
            case "Rare_Holo": return "Rare Holo";
            case "Rare_Holo_EX": return "Rare Holo EX";
            case "Rare_Holo_GX": return "Rare Holo GX";
            case "Rare_Holo_V": return "Rare Holo V";
            case "Rare_Holo_VMAX": return "Rare Holo VMAX";
            case "Rare_Holo_VSTAR": return "Rare Holo VSTAR";
            case "Rare_ACE": return "Rare ACE SPEC";
            case "Rare_BREAK": return "Rare BREAK";
            case "Rare_PRISM": return "Rare PRISM";
            case "Rare_Ultra": return "Rare Ultra";
            case "Rare_Secret": return "Rare Secret";
            case "Rare_Shiny": return "Rare Shiny";
            case "Rare_Shiny_GX": return "Rare Shiny GX";
            case "Rare_Ultra_Rare": return "Rare Ultra";
            case "Ultra-Rare_Rare": return "Ultra Rare";
            case "Promo": return "Promo";
            case "RR": return "Double Rare";
            case "SR": return "Secret Rare";
            case "SSR": return "Shiny Secret Rare";
            case "UR": return "Ultra Rare";
            case "HR": return "Hyper Rare";
            case "CHR": return "Character Rare";
            case "CSR": return "Character Secret Rare";
            case "AR": return "Art Rare";
            default:
                // For unknown codes, try to make them readable
                return rarityCode.replace("_", " ").replace("-", " ");
        }
    }

    // Convert alt text to rarity name
    private String convertRarityAltToName(String altText) {
        // Clean up alt text and convert to proper name
        altText = altText.trim();

        // Map common alt texts
        switch (altText) {
            case "C": return "Common";
            case "U": return "Uncommon";
            case "R": return "Rare";
            case "RR": return "Double Rare";
            case "SR": return "Secret Rare";
            case "SSR": return "Shiny Secret Rare";
            case "UR": return "Ultra Rare";
            case "HR": return "Hyper Rare";
            case "CHR": return "Character Rare";
            case "CSR": return "Character Secret Rare";
            case "AR": return "Art Rare";
            case "S": return "Shiny";
            default: return altText;
        }
    }

    // Convert title attribute to rarity name
    private String convertRarityTitleToName(String title) {
        // Clean up title and convert to proper name
        title = title.trim();

        // Map common titles
        switch (title) {
            case "Ultra-Rare Rare": return "Ultra Rare";
            case "Rare Ultra": return "Rare Ultra";
            case "Rare Shiny GX": return "Rare Shiny GX";
            case "RR": return "Double Rare";
            case "SR": return "Secret Rare";
            case "SSR": return "Shiny Secret Rare";
            case "C": return "Common";
            case "U": return "Uncommon";
            case "R": return "Rare";
            case "UR": return "Ultra Rare";
            case "HR": return "Hyper Rare";
            case "CHR": return "Character Rare";
            case "CSR": return "Character Secret Rare";
            case "AR": return "Art Rare";
            case "S": return "Shiny";
            default: return title;
        }
    }

    private String extractRarityFromDocument(Document cardDoc) {
        // Method 1: Look for Rarity table row with the specific structure
        Elements rarityRows = cardDoc.select("tr:has(td:containsOwn(Rarity))");

        for (Element row : rarityRows) {
            Elements rarityCells = row.select("td");
            if (rarityCells.size() >= 2) {
                // The first td contains "Rarity" label, second td contains the actual rarity
                Element rarityCell = rarityCells.get(1);

                // Look for rarity images first (most reliable)
                Elements rarityImages = rarityCell.select("img[src*=\"Rarity_\"]");
                if (!rarityImages.isEmpty()) {
                    String imgSrc = rarityImages.first().attr("src");
                    String rarity = extractRarityFromImageSrc(imgSrc);
                    if (rarity != null) {
                        return rarity;
                    }
                }

                // Fallback to text content
                String rarityText = rarityCell.text().trim();
                if (!rarityText.isEmpty() && !rarityText.equals("Rarity")) {
                    return rarityText;
                }
            }
        }

        // Method 2: Look for rarity images anywhere in common locations
        Elements allRarityImages = cardDoc.select("img[src*=\"Rarity_\"]");
        if (!allRarityImages.isEmpty()) {
            String imgSrc = allRarityImages.first().attr("src");
            String rarity = extractRarityFromImageSrc(imgSrc);
            if (rarity != null) {
                return rarity;
            }
        }

        // Method 3: Search in common infobox structures
        Elements infoboxes = cardDoc.select(".infobox, .card-table, table.roundy");
        for (Element infobox : infoboxes) {
            String rarity = searchRarityInTable(infobox);
            if (rarity != null) {
                return rarity;
            }
        }

        return "Rarity not found";
    }

    private String extractRarityFromImageSrc(String imgSrc) {
        // Extract rarity from image filename
        if (imgSrc.contains("Rarity_")) {
            String filename = imgSrc.substring(imgSrc.lastIndexOf("/") + 1);
            String rarityCode = filename.replace("Rarity_", "").replace(".png", "");
            return convertRarityCodeToName(rarityCode);
        }
        return null;
    }

    private String convertRarityCodeToName(String rarityCode) {
        // Map rarity codes to human-readable names
        switch (rarityCode) {
            case "Common": return "Common";
            case "Uncommon": return "Uncommon";
            case "Rare": return "Rare";
            case "Rare_Holo": return "Rare Holo";
            case "Rare_Holo_EX": return "Rare Holo EX";
            case "Rare_Holo_GX": return "Rare Holo GX";
            case "Rare_Holo_V": return "Rare Holo V";
            case "Rare_Holo_VMAX": return "Rare Holo VMAX";
            case "Rare_Holo_VSTAR": return "Rare Holo VSTAR";
            case "Rare_ACE": return "Rare ACE";
            case "Rare_BREAK": return "Rare BREAK";
            case "Rare_PRISM": return "Rare PRISM";
            case "Rare_Ultra": return "Rare Ultra";
            case "Rare_Secret": return "Rare Secret";
            case "Rare_Shiny": return "Rare Shiny";
            case "Promo": return "Promo";
            default: return rarityCode.replace("_", " "); // Fallback
        }
    }

    private String searchRarityInTable(Element table) {
        // Search for rarity in various table structures
        Elements rows = table.select("tr");

        for (Element row : rows) {
            Elements cells = row.select("td, th");
            for (int i = 0; i < cells.size(); i++) {
                Element cell = cells.get(i);
                if (cell.text().trim().equalsIgnoreCase("Rarity") && i + 1 < cells.size()) {
                    // Next cell should contain the rarity
                    Element rarityCell = cells.get(i + 1);
                    String rarity = rarityCell.text().trim();
                    if (!rarity.isEmpty()) {
                        return rarity;
                    }
                }
            }
        }
        return null;
    }

    private List<Card> extractCardsFromTableSection(Element cardTable, String targetGroup) {
        List<Card> cards = new ArrayList<>();
        Elements rows = cardTable.select("tr");

        boolean hasPromotionColumn = detectPromotionColumn(cardTable);
        int promotionColumnIndex = hasPromotionColumn ? findPromotionColumnIndex(cardTable) : -1;

        boolean foundTargetTable = false;

        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);

            // Check if this row contains a <big><b> header
            Elements bigBTags = row.select("big b");
            if (!bigBTags.isEmpty()) {
                String headerText = bigBTags.first().text().trim();

                if (headerText.equals(targetGroup)) {
                    // This is our target table, start processing
                    foundTargetTable = true;
                    System.out.println("Found target group: " + targetGroup);
                    continue;
                } else if (foundTargetTable) {
                    // We hit another <big><b> (ANY title), stop processing
                    System.out.println("Reached next group: " + headerText + ", stopping extraction");
                    break;
                }
            }

            // Only process cards if we've found the target table
            if (foundTargetTable) {
                Card card = extractCardFromRow(row, hasPromotionColumn, promotionColumnIndex);
                if (card != null) {
                    cards.add(card);
                }
            }
        }

        System.out.println("Extracted " + cards.size() + " cards from table section");
        return cards;
    }

    // Generic retry logic for any Jsoup operation
    private <T> T executeWithRetry(JsoupOperation<T> operation) throws IOException {
        int attempt = 1;
        long delay = INITIAL_RETRY_DELAY_MS;

        while (attempt <= MAX_RETRIES) {
            try {
                return operation.execute();

            } catch (IOException e) {
                System.err.println("Attempt " + attempt + "/" + MAX_RETRIES + " failed: " + e.getMessage());

                if (attempt == MAX_RETRIES) {
                    throw new IOException("Operation failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }

                try {
                    System.out.println("Retrying in " + delay + "ms...");
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Operation interrupted during retry delay", ie);
                }

                // Exponential backoff with jitter
                delay = Math.min((long)(delay * 1.5 + Math.random() * 1000), MAX_RETRY_DELAY_MS);
                attempt++;
            }
        }

        throw new IOException("Operation failed after " + MAX_RETRIES + " attempts");
    }

    // Functional interface for retry logic
    @FunctionalInterface
    private interface JsoupOperation<T> {
        T execute() throws IOException;
    }

    public class Card {
        private String number;
        private String name;
        private String type;
        private boolean isPromo;
        private String promoValue;
        private String pageUrl;
        private String rarity;
        private List<ExpansionInfo> englishExpansions;
        private List<Ulid> setIds = new ArrayList<>();
        private boolean distribution = false;
//        private  Ulid parentId;

        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public boolean isPromo() { return isPromo; }
        public void setPromo(boolean promo) { isPromo = promo; }

        public String getPromoValue() { return promoValue; }
        public void setPromoValue(String promoValue) { this.promoValue = promoValue; }

        public String getPageUrl() { return pageUrl; }
        public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

        public String getRarity() { return rarity; }
        public void setRarity(String rarity) { this.rarity = rarity; }

        public List<Ulid> getSetIds() { return setIds; }
        public void setSetIds(List<Ulid> setIds) { this.setIds = setIds; }

        public boolean isDistribution() { return distribution; }
        public void setDistribution(boolean distribution) { this.distribution = distribution; }

//        public Ulid getParentId() { return parentId; }
//        public void setParentId(Ulid parentId) { this.parentId = parentId; }

        public List<ExpansionInfo> getEnglishExpansions() {
            return englishExpansions != null ? englishExpansions : new ArrayList<>();
        }

        public void setEnglishExpansions(List<ExpansionInfo> englishExpansions) {
            this.englishExpansions = englishExpansions;
        }
    }

    public class PromoInfo {
        boolean isPromo;
        String promoValue;

        PromoInfo(boolean isPromo, String promoValue) {
            this.isPromo = isPromo;
            this.promoValue = promoValue;
        }
    }

    public class CardDetails {
        String rarity;
        List<ExpansionInfo> englishExpansions;

        public CardDetails() {
            this.englishExpansions = new ArrayList<>();
        }
    }

    public class ExpansionInfo {
        String expansionName;
        String cardNumber;
        boolean isEnglishExpansion;
        String rarity;

        public ExpansionInfo(String expansionName, String cardNumber, boolean isEnglishExpansion) {
            this.expansionName = expansionName;
            this.cardNumber = cardNumber;
            this.isEnglishExpansion = isEnglishExpansion;
        }

        public String getExpansionName() {
            return expansionName;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public String getRarity() {
            return rarity;
        }

        public void setRarity(String rarity) {
            this.rarity = rarity;
        }

        public boolean isEnglishExpansion() {
            return isEnglishExpansion;
        }
    }
}