package com.pcagrade.retriever.field.mapper;

import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import java.util.List;

@RetrieverTestConfiguration
public class FieldMappingTestConfig {

    private static FieldMapping createFieldMapping(int id, String field, String source, String value, boolean regex) {
        var fieldMapping = new FieldMapping();

        fieldMapping.setId(id);
        fieldMapping.setField(field);
        fieldMapping.setSource(source);
        fieldMapping.setValue(value);
        fieldMapping.setRegex(regex);
        return fieldMapping;
    }

    @Bean
    public FieldMappingRepository fieldMappingRepository() {
        var list = List.of(
                createFieldMapping(1, "sets.onp.us.code", "(?i)^OP04$", "OP-04", true),
                createFieldMapping(2, "test.1", "(?i)^test$", "foo", true),
                createFieldMapping(3, "test.2", "test", "foo", false),
                createFieldMapping(4, "test.3", "^test$", "foo", true),
                createFieldMapping(5, "cards.ygh.rarity", "(?i)^Ultra Rare \\(Pharaoh's Rare\\)$", "URPR", true),
                createFieldMapping(6, "cards.ygh.rarity", "(?i)^UR \\(PR\\)$", "URPR", true),
                createFieldMapping(7, "cards.ygh.rarity", "(?i)^UR \\(RF\\)$", "URPR", true)
        );
        var repository = RetrieverTestUtils.mockRepository(FieldMappingRepository.class, list, FieldMapping::getId);

        Mockito.when(repository.findAllByField(Mockito.anyString())).then(i -> {
            var field = i.getArgument(0, String.class);

            return list.stream()
                    .filter(fieldMapping -> StringUtils.equals(fieldMapping.getField(), field))
                    .toList();
        });
        return repository;
    }

    @Bean
    public FieldMappingMapper fieldMappingMapper() {
        return new FieldMappingMapperImpl();
    }

    @Bean
    public FieldMappingService fieldMappingService() {
        return new FieldMappingService();
    }
}
