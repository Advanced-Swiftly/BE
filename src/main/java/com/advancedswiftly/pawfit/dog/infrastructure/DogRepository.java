package com.advancedswiftly.pawfit.dog.infrastructure;

import com.advancedswiftly.pawfit.dog.domain.Dog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DogRepository extends JpaRepository<Dog, Long> {

    long countByUserId(Long userId);

    long countByIdInAndUserId(List<Long> dogIds, Long userId);

    @EntityGraph(attributePaths = {"breed"})
    List<Dog> findAllByUserIdOrderByDefaultDogDescIdAsc(Long userId);

    @EntityGraph(attributePaths = {"breed", "equipmentCodes"})
    Optional<Dog> findByIdAndUserId(Long dogId, Long userId);

    Optional<Dog> findByUserIdAndDefaultDogTrue(Long userId);

    Optional<Dog> findFirstByUserIdAndIdNotOrderByIdAsc(Long userId, Long dogId);
}
