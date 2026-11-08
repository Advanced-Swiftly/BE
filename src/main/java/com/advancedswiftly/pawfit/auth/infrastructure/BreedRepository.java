package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.Breed;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<Breed, Long> {

    List<Breed> findAllByActiveTrueOrderByName();

    List<Breed> findAllByActiveTrueAndNameContainingIgnoreCaseOrderByName(String keyword);
}
