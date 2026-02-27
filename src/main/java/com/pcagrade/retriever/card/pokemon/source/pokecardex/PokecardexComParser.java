package com.pcagrade.retriever.card.pokemon.source.pokecardex;

import com.pcagrade.retriever.PCAUtils;
import com.pcagrade.retriever.cache.CacheService;
import jakarta.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@CacheConfig(cacheNames = "pokecardexComParser")
public class PokecardexComParser {

    private static final Logger LOGGER = LogManager.getLogger(PokecardexComParser.class);

    private static final String CARD_ANCHOR_SELECTOR = "div.serie-details-carte>a";
    private static final String CARD_NAME_DIV_SELECTOR = "div.serie-details-nom-carte";
    private static final int JS_WAIT_MS = 10000;

    @Autowired
    private CacheService cacheService;

    @Value("${pokecardex-com.url}")
    private String pokecardexUrl;

    @Cacheable
    public Map<String, String> parse(String code) {
        var anchorsNames = parseAnchors(code);
        var divNames = parseNameDivs(code);
        var result = new HashMap<>(anchorsNames);

        divNames.forEach((key, value) -> result.merge(key, value, (v1, v2) -> v1));
        return result;
    }

    public Map<String, String> parseAnchors(String code) {
        return listElements(code, CARD_ANCHOR_SELECTOR).stream()
                .map(a -> a.attr("title"))
                .filter(StringUtils::isNotBlank)
                .map(PokecardexComParser::createPair)
                .filter(p -> StringUtils.isNotBlank(p.getLeft()) && StringUtils.isNotBlank(p.getRight()))
                .collect(PCAUtils.toMap());
    }

    public Map<String, String> parseNameDivs(String code) {
        return listElements(code, CARD_NAME_DIV_SELECTOR).stream()
                .map(Element::text)
                .filter(StringUtils::isNotBlank)
                .map(PokecardexComParser::createPair)
                .filter(p -> StringUtils.isNotBlank(p.getLeft()) && StringUtils.isNotBlank(p.getRight()))
                .collect(PCAUtils.toMap());
    }

    @Nonnull
    private static Pair<String, String> createPair(String name) {
        var index = name.lastIndexOf(" ");

        return Pair.of(name.substring(index + 1), PCAUtils.clean(name.substring(0, index)));
    }

    private List<Element> listElements(String code, String selector) {
        try {
            var url = pokecardexUrl + "series/" + code;
            var html = cacheService.getOrRequestCachedPage(url, () -> fetchWithHtmlUnit(url)).block();

            if (StringUtils.isBlank(html)) {
                return Collections.emptyList();
            }

            var body = Jsoup.parse(html).body();
            var value = body.select(selector);

            if (CollectionUtils.isEmpty(value)) {
                return Collections.emptyList();
            }
            return value;
        } catch (Exception e) {
            LOGGER.error("Failed to parse pokecardex page for code {}", code, e);
            return Collections.emptyList();
        }
    }

    @Nonnull
    private Mono<String> fetchWithHtmlUnit(String url) {
        return Mono.fromCallable(() -> {
            try (var webClient = new WebClient()) {
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                webClient.getOptions().setPrintContentOnFailingStatusCode(false);

                // Suppress HtmlUnit verbose logging
                java.util.logging.Logger.getLogger("org.htmlunit").setLevel(java.util.logging.Level.OFF);
                java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);

                HtmlPage page = webClient.getPage(url);
                webClient.waitForBackgroundJavaScript(JS_WAIT_MS);

                return page.asXml();
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch page with HtmlUnit: " + url, e);
            }
        });
    }

    public String getUrl(String code) {
        return pokecardexUrl + "series/" + code;
    }
}
