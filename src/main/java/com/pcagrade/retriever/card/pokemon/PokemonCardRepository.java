package com.pcagrade.retriever.card.pokemon;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.jpa.repository.MasonRevisionRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface PokemonCardRepository extends MasonRevisionRepository<PokemonCard, Ulid> {

    @Query("select pc from PokemonCard pc join fetch pc.cardSets cs where cs.id = :setId")
    List<PokemonCard> findInSet(Ulid setId);

    @Query("select pc.type from PokemonCard pc group by pc.type")
    List<String> findAllTypes();

    List<PokemonCard> findAllByParentId(Ulid parentId);

    @Query("select c from PokemonCard c join fetch c.promoCards pc where pc.id = :promoId")
    Optional<PokemonCard> findByPromoId(Ulid promoId);

    // Check if set has any cards
    @Query("SELECT COUNT(c) > 0 FROM PokemonCard c JOIN c.cardSets cs WHERE cs.id = :setId")
    boolean existsBySetId(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM card_certification_comment WHERE card_certification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearCertificationComments(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM card_certification_history WHERE card_certification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearCertificationHistory(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM order_history WHERE card_certification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearOrderHistory(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM order_edit WHERE cc_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearOrderEdit(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM certification_grade WHERE cardcertification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearCertificationGrade(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM searched_certification WHERE cardcertification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearSearchedCertification(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM card_certification_order WHERE card_certification_id IN (SELECT id FROM card_certification WHERE card_id = :cardId)", nativeQuery = true)
    void clearCertificationOrder(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "DELETE FROM card_certification WHERE card_id = :cardId", nativeQuery = true)
    void clearCertifications(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "UPDATE pokemon_card SET jap_carte_mere_id = NULL WHERE jap_carte_mere_id = :cardId", nativeQuery = true)
    void clearJapaneseParent(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "UPDATE pokemon_card SET carte_mere_id = NULL WHERE carte_mere_id = :cardId", nativeQuery = true)
    void clearRegularParent(@Param("cardId") Ulid cardId);

    @Modifying
    @Query(value = "UPDATE pokemon_card SET kr_carte_mere_id = NULL WHERE kr_carte_mere_id = :cardId", nativeQuery = true)
    void clearKoreanParent(@Param("cardId") Ulid cardId);
}
