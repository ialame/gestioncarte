package com.pcagrade.retriever.card.pokemon.image.web;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.retriever.card.pokemon.image.ExtractedPokemonImagesDTO;
import com.pcagrade.retriever.card.pokemon.image.PokemonCardImageService;
import com.pcagrade.retriever.image.ExtractedImageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/pokemon")
public class PokemonCardImageRestController {

    @Autowired
    private PokemonCardImageService pokemonCardImageService;

    @GetMapping("/images/extract/sets/{setId}")
    public Page<ExtractedPokemonImagesDTO> extractImages(@PathVariable Ulid setId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return pokemonCardImageService.extractImages(setId, Pageable.ofSize(Math.min(25, size)).withPage(page));
    }

    @PostMapping("/images/reload")
    public ExtractedImageDTO reloadImage(@RequestBody ExtractedImageDTO image) {
        return pokemonCardImageService.reloadImage(image);
    }

    @PutMapping("/{cardId}/images")
    public void setImage(@PathVariable Ulid cardId, @RequestBody ExtractedImageDTO image) {
        pokemonCardImageService.setImage(cardId, image);
    }
}
