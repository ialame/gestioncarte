package com.pcagrade.retriever.card.pokemon.serie;

import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.retriever.RetrieverTestUtils;
import com.pcagrade.retriever.annotation.RetrieverTestConfiguration;
import com.pcagrade.retriever.card.pokemon.serie.translation.PokemonSerieTranslationMapper;
import com.pcagrade.retriever.card.pokemon.serie.translation.PokemonSerieTranslationMapperImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RetrieverTestConfiguration
public class PokemonSerieTestConfig {

	@Bean
	public PokemonSerieRepository pokemonSerieRepository() {
		var list = new ArrayList<>(List.of(PokemonSerieTestProvider.xy()));
		var repository = RetrieverTestUtils.mockRepository(PokemonSerieRepository.class, list, PokemonSerie::getId);

		Mockito.when(repository.save(Mockito.any())).then(invocation -> {
			var serie = invocation.getArgument(0, PokemonSerie.class);
			if (serie.getId() == null) {
				serie.setId(UlidCreator.getUlid());
			}
			list.removeIf(s -> Objects.equals(serie.getId(), s.getId()));
			list.add(serie);
			return serie;
		});
		return repository;
	}

	@Bean
	public PokemonSerieTranslationMapper pokemonSerieTranslationMapper() {
		return new PokemonSerieTranslationMapperImpl();
	}

	@Bean
	public PokemonSerieMapper pokemonSerieMapper() {
		return new PokemonSerieMapperImpl();
	}

	@Bean
	public PokemonSerieService pokemonSerieService() {
		return new PokemonSerieService();
	}

}
