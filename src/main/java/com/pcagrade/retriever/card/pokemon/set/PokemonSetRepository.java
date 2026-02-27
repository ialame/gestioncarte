package com.pcagrade.retriever.card.pokemon.set;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.mason.jpa.repository.MasonRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PokemonSetRepository extends MasonRepository<PokemonSet, Ulid> {
    @Procedure("j_merge_set")
    void mergeSets(Ulid id1, Ulid id2);

    boolean existsByIdPca(int idPca);

    Optional<PokemonSet> findFirstByShortName(String shortName);

    @Query(value = """
        WITH name_match AS (
            SELECT ps.id FROM pokemon_set ps 
            INNER JOIN card_set cs ON ps.id = cs.id 
            INNER JOIN card_set_translation cst ON cs.id = cst.translatable_id 
            WHERE cst.locale = 'us' AND cst.name = :searchName 
            LIMIT 1
        ),
        label_match AS (
            SELECT ps.id FROM pokemon_set ps 
            INNER JOIN card_set cs ON ps.id = cs.id 
            INNER JOIN card_set_translation cst ON cs.id = cst.translatable_id 
            WHERE cst.locale = 'us' AND cst.label_name = :searchName 
            LIMIT 1
        )
        SELECT COALESCE(
            (SELECT id FROM name_match),
            (SELECT id FROM label_match)
        )
        """, nativeQuery = true)
    Optional<byte[]> findSetIdByExactUsNameOrLabelName(@Param("searchName") String searchName);

    default Optional<Ulid> findSetIdByExactUsNameOrLabelNameAsUlid(String searchName) {
        return findSetIdByExactUsNameOrLabelName(searchName)
                .map(byteArray -> {
                    try {

                        if (byteArray.length == 16) {
                            // Convert byte array to UUID
                            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
                            long mostSigBits = buffer.getLong();
                            long leastSigBits = buffer.getLong();
                            UUID uuid = new UUID(mostSigBits, leastSigBits);

                            // Convert UUID to ULID directly
                            Ulid ulid = Ulid.from(uuid);

                            // Verify the set exists
                            if (existsById(ulid)) {
                                return ulid;
                            } else {
                                return null;
                            }
                        } else {
                            System.out.println("❌ Unexpected byte array length: " + byteArray.length);
                            return null;
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Error converting byte array: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    @Modifying
    @Query(value = "DELETE FROM j_pokecardex_set_code WHERE set_id = :setId", nativeQuery = true)
    void clearPokecardexSetCode(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_official_site_set_path WHERE set_id = :setId", nativeQuery = true)
    void clearOfficialSiteSetPath(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_official_site_jp_source WHERE set_id = :setId", nativeQuery = true)
    void clearOfficialSiteJpSource(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_ptcgo_set WHERE set_id = :setId", nativeQuery = true)
    void clearPtcgoSet(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_expansion_bulbapedia WHERE set_id = :setId", nativeQuery = true)
    void clearExpansionBulbapedia(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_wiki_url WHERE set_id = :setId", nativeQuery = true)
    void clearWikiUrl(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_pkmncards_com_set_path WHERE set_id = :setId", nativeQuery = true)
    void clearPkmncardsComSetPath(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM card_image WHERE set_id = :setId", nativeQuery = true)
    void clearCardImage(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM extension_concatenee WHERE extensionJap_id = :setId", nativeQuery = true)
    void clearExtensionConcatenee(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM j_card_extraction_history_card_set WHERE card_set_id = :setId", nativeQuery = true)
    void clearCardExtractionHistoryCardSet(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "UPDATE card_set SET parent_id = NULL WHERE parent_id = :setId", nativeQuery = true)
    void clearParentId(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = " DELETE FROM card_image_history WHERE card_image_id IN (SELECT id FROM card_image WHERE set_id = :setId)", nativeQuery = true)
    void deleteBySetId(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM voucher WHERE set_id = :setId", nativeQuery = true)
    void clearVoucher(@Param("setId") Ulid setId);

    @Modifying
    @Query(value = "DELETE FROM set_source_pokemon_set WHERE pokemon_set_id = :setId", nativeQuery = true)
    void clearSetSourcePokemonSet(@Param("setId") Ulid setId);
}