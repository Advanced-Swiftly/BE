package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.BreedItem;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.BreedsResponse;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.RegionItem;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.RegionsResponse;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.TermItem;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.TermsResponse;
import com.advancedswiftly.pawfit.auth.domain.Breed;
import com.advancedswiftly.pawfit.auth.domain.Region;
import com.advancedswiftly.pawfit.auth.domain.Term;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.BreedRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.RegionRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.TermRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommonQueryService {

    private final TermRepository termRepository;
    private final RegionRepository regionRepository;
    private final BreedRepository breedRepository;

    public CommonQueryService(
            TermRepository termRepository,
            RegionRepository regionRepository,
            BreedRepository breedRepository) {
        this.termRepository = termRepository;
        this.regionRepository = regionRepository;
        this.breedRepository = breedRepository;
    }

    public TermsResponse terms(String rawRole) {
        UserRole role;
        try {
            role = UserRole.valueOf(rawRole.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        if (role != UserRole.GUARDIAN && role != UserRole.BUSINESS) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        List<TermItem> terms = termRepository.findAllByRoleAndActiveTrueOrderById(role).stream()
                .map(this::toTermItem)
                .toList();
        return new TermsResponse(terms);
    }

    public RegionsResponse regions(Long parentRegionId) {
        List<Region> regions = parentRegionId == null
                ? regionRepository.findAllByParentIsNullAndActiveTrueOrderByName()
                : regionRepository.findAllByParentIdAndActiveTrueOrderByName(parentRegionId);
        return new RegionsResponse(regions.stream()
                .map(region -> new RegionItem(region.getId(), region.getName()))
                .toList());
    }

    public BreedsResponse breeds(String keyword) {
        List<Breed> breeds = keyword == null || keyword.isBlank()
                ? breedRepository.findAllByActiveTrueOrderByName()
                : breedRepository.findAllByActiveTrueAndNameContainingIgnoreCaseOrderByName(
                        keyword.trim());
        return new BreedsResponse(breeds.stream()
                .map(breed -> new BreedItem(breed.getId(), breed.getName()))
                .toList());
    }

    private TermItem toTermItem(Term term) {
        return new TermItem(term.getId(), term.getTitle(), term.isRequired(), term.getVersion());
    }
}
