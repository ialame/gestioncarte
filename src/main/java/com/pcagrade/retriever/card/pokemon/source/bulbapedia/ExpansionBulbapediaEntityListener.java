package com.pcagrade.retriever.card.pokemon.source.bulbapedia;

import com.pcagrade.retriever.card.pokemon.source.bulbapedia.expansion.ExpansionBulbapedia;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;

@Component
public class ExpansionBulbapediaEntityListener {

    private static ApplicationEventPublisher eventPublisher;

    // Constructor injection for the event publisher
    public ExpansionBulbapediaEntityListener(ApplicationEventPublisher eventPublisher) {
        ExpansionBulbapediaEntityListener.eventPublisher = eventPublisher;
    }

    @PostPersist
    public void onPostPersist(ExpansionBulbapedia expansion) {
        if (eventPublisher != null && expansion.getCharset() != null) {
            String charset = expansion.getCharset().toLowerCase();
            if (charset.equals("zh") || charset.equals("cn")) {
                System.out.println("New Chinese expansion detected: " + expansion.getName());

                // Check if the expansion has a set associated
                if (expansion.getSet() != null && expansion.getSet().getId() != null) {
                    eventPublisher.publishEvent(
                            new ExpansionBulbapediaCreatedEvent(
                                    expansion.getSet().getId(),
                                    expansion.getCharset(),
                                    expansion.getId()
                            )
                    );
                } else {
                    System.err.println(" Chinese expansion has no set associated: " + expansion.getName());
                }
            }
        }
    }
}

