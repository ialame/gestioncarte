package com.pcagrade.retriever.card.pokemon.source.pokecardex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagrade.retriever.PCAUtils;
import com.pcagrade.retriever.cache.CacheService;
import com.pcagrade.retriever.parser.RetrieverHTTPHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@CacheConfig(cacheNames = "pokecardexComParser")
public class PokecardexComParser {

    private static final Logger LOGGER = LogManager.getLogger(PokecardexComParser.class);

    private static final String ENCRYPTION_KEY = "oe61R0RgVTJm9omokoKuRem2N2GUbUZ8";
    private static final Pattern ENCRYPTED_DATA_PATTERN =
            Pattern.compile("window\\.__INITIAL_DATA_ENCRYPTED__\\s*=\\s*(\\{[^;]+\\});");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private CacheService cacheService;

    @Value("${pokecardex-com.url}")
    private String pokecardexUrl;

    private final WebClient webClient;

    public PokecardexComParser(@Value("${retriever.web-client.max-in-memory-size:10MB}") String maxInMemorySize) {
        this.webClient = WebClient.builder()
                .exchangeStrategies(RetrieverHTTPHelper.createExchangeStrategies(maxInMemorySize))
                .clientConnector(RetrieverHTTPHelper.createReactorClientHttpConnector("pokecardex-connection-provider", 5))
                .build();
    }

    @Cacheable
    public Map<String, String> parse(String code) {
        try {
            var url = pokecardexUrl + "series/" + code;
            var html = cacheService.getOrRequestCachedPage(url, () -> webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(String.class))
                    .block();

            if (StringUtils.isBlank(html)) {
                return Collections.emptyMap();
            }

            var matcher = ENCRYPTED_DATA_PATTERN.matcher(html);
            if (!matcher.find()) {
                LOGGER.warn("No encrypted data found in pokecardex page for code {}", code);
                return Collections.emptyMap();
            }

            var encryptedJson = matcher.group(1);
            var encryptedNode = OBJECT_MAPPER.readTree(encryptedJson);
            var iv = Base64.getDecoder().decode(encryptedNode.get("iv").asText());
            var data = Base64.getDecoder().decode(encryptedNode.get("data").asText());

            var keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
            var secretKey = new SecretKeySpec(keyBytes, "AES");
            var ivSpec = new IvParameterSpec(iv);

            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            var decrypted = new String(cipher.doFinal(data), StandardCharsets.UTF_8);

            var jsonNode = OBJECT_MAPPER.readTree(decrypted);
            var cartes = jsonNode.get("cartes");

            if (cartes == null || !cartes.isArray()) {
                LOGGER.warn("No 'cartes' array found in decrypted data for code {}", code);
                return Collections.emptyMap();
            }

            var result = new HashMap<String, String>();
            for (JsonNode carte : cartes) {
                var numCard = carte.has("num_card") ? carte.get("num_card").asText() : null;
                var nameCardFr = carte.has("name_card_fr") ? carte.get("name_card_fr").asText() : null;
                var total = carte.has("total") ? carte.get("total").asText() : null;

                if (StringUtils.isNotBlank(numCard) && StringUtils.isNotBlank(nameCardFr) && StringUtils.isNotBlank(total)) {
                    result.put(numCard + "/" + total, PCAUtils.clean(nameCardFr));
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to parse pokecardex page for code {}", code, e);
            return Collections.emptyMap();
        }
    }

    public String getUrl(String code) {
        return pokecardexUrl + "series/" + code;
    }
}
