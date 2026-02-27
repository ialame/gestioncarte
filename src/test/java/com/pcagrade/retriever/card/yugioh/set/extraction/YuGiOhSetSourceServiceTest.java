package com.pcagrade.retriever.card.yugioh.set.extraction;

import com.pcagrade.mason.localization.Localization;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class YuGiOhSetSourceServiceTest {

    @Autowired
    private List<IYuGiOhSetSourceService> yuGiOhSetSourceService;

    @ParameterizedTest
    @MethodSource("provide_services")
    void getAllSets_should_returnValidSets(IYuGiOhSetSourceService service) {
        var sets = service.getAllSets();

        // Sets can be empty if external API is not accessible
        if (sets.isEmpty()) {
            return;
        }

        assertThat(sets)
                .allSatisfy(s -> assertThat(s.getValue()).isNotNull().satisfies(card -> assertThat(card.getTranslations()).allSatisfy((l, t) -> {
                    assertThat(t.getLocalization()).isEqualTo(l);
                    assertThat(t.getName()).isNotEmpty();
                })));
    }

    private Stream<Arguments> provide_services() {
        return yuGiOhSetSourceService.stream().map(Arguments::of);
    }

}
