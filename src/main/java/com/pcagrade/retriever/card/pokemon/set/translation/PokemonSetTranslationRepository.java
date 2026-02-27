package com.pcagrade.retriever.card.pokemon.set.translation;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.jpa.repository.MasonRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PokemonSetTranslationRepository extends MasonRepository<PokemonSetTranslation, Ulid> {
}
