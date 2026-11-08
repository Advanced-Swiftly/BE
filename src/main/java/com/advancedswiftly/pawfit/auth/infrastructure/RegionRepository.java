package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByParentIsNullAndActiveTrueOrderByName();

    List<Region> findAllByParentIdAndActiveTrueOrderByName(Long parentId);
}
