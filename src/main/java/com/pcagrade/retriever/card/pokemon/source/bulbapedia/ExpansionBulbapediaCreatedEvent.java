package com.pcagrade.retriever.card.pokemon.source.bulbapedia;

import com.github.f4b6a3.ulid.Ulid;

public class ExpansionBulbapediaCreatedEvent {
    private final Ulid setId;
    private final String charset;
    private final Integer expansionId;

    public ExpansionBulbapediaCreatedEvent(Ulid setId, String charset, Integer expansionId) {
        this.setId = setId;
        this.charset = charset;
        this.expansionId = expansionId;
    }

    public Ulid getSetId() {
        return setId;
    }

    public String getCharset() {
        return charset;
    }

    public Integer getExpansionId() {
        return expansionId;
    }

    public boolean isChinese() {
        return charset != null &&
                (charset.equalsIgnoreCase("zh") || charset.equalsIgnoreCase("cn"));
    }
}