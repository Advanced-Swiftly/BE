package com.advancedswiftly.pawfit.auth.api.dto;

import java.util.List;

public final class CommonDtos {

    private CommonDtos() {
    }

    public record TermItem(Long termId, String title, boolean required, String version) {
    }

    public record TermsResponse(List<TermItem> terms) {
    }

    public record RegionItem(Long regionId, String name) {
    }

    public record RegionsResponse(List<RegionItem> regions) {
    }

    public record BreedItem(Long breedId, String name) {
    }

    public record BreedsResponse(List<BreedItem> breeds) {
    }
}
