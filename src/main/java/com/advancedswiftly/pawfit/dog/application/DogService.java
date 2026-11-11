package com.advancedswiftly.pawfit.dog.application;

import com.advancedswiftly.pawfit.auth.domain.Breed;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.BreedRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DefaultDogResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogCreateRequest;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogCreateResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogDetailResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogListItem;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogListResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogUpdateRequest;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogUpdateResponse;
import com.advancedswiftly.pawfit.dog.domain.Dog;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DogService {

    private static final long MAX_DOG_COUNT = 5;

    private final DogRepository dogRepository;
    private final UserRepository userRepository;
    private final BreedRepository breedRepository;

    public DogService(
            DogRepository dogRepository,
            UserRepository userRepository,
            BreedRepository breedRepository) {
        this.dogRepository = dogRepository;
        this.userRepository = userRepository;
        this.breedRepository = breedRepository;
    }

    @Transactional
    public DogCreateResponse create(Long userId, DogCreateRequest request) {
        User user = requireGuardian(userId, true);
        long dogCount = dogRepository.countByUserId(userId);
        if (dogCount >= MAX_DOG_COUNT) {
            throw new BusinessException(ErrorCode.DOG_LIMIT_EXCEEDED);
        }

        Breed breed = requireBreed(request.breedId());
        boolean defaultDog = dogCount == 0 || Boolean.TRUE.equals(request.isDefault());
        if (defaultDog) {
            clearCurrentDefault(userId);
        }

        Dog dog = dogRepository.save(Dog.create(
                user,
                breed,
                request.name(),
                request.birthDate(),
                request.weightKg(),
                defaultDog,
                request.equipmentCodes()));
        return new DogCreateResponse(dog.getId(), dog.getName(), dog.isDefaultDog());
    }

    @Transactional(readOnly = true)
    public DogListResponse findMine(Long userId) {
        requireGuardian(userId, false);
        List<DogListItem> dogs = dogRepository.findAllByUserIdOrderByDefaultDogDescIdAsc(userId)
                .stream()
                .map(dog -> new DogListItem(
                        dog.getId(),
                        dog.getName(),
                        dog.getBreed().getName(),
                        dog.isDefaultDog()))
                .toList();
        return new DogListResponse(dogs);
    }

    @Transactional(readOnly = true)
    public DogDetailResponse findMine(Long userId, Long dogId) {
        requireGuardian(userId, false);
        return toDetail(requireOwnedDog(userId, dogId));
    }

    @Transactional
    public DogUpdateResponse update(Long userId, Long dogId, DogUpdateRequest request) {
        requireGuardian(userId, true);
        if (request.name() == null
                && request.breedId() == null
                && request.weightKg() == null
                && request.equipmentCodes() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Dog dog = requireOwnedDog(userId, dogId);
        if (request.name() != null) {
            dog.updateName(request.name());
        }
        if (request.breedId() != null) {
            dog.updateBreed(requireBreed(request.breedId()));
        }
        if (request.weightKg() != null) {
            dog.updateWeightKg(request.weightKg());
        }
        if (request.equipmentCodes() != null) {
            dog.replaceEquipmentCodes(request.equipmentCodes());
        }
        return new DogUpdateResponse(dog.getId(), dog.getName(), dog.getWeightKg());
    }

    @Transactional
    public DefaultDogResponse designateDefault(Long userId, Long dogId) {
        requireGuardian(userId, true);
        Dog dog = requireOwnedDog(userId, dogId);
        if (!dog.isDefaultDog()) {
            clearCurrentDefault(userId);
            dog.designateAsDefault();
        }
        return new DefaultDogResponse(dog.getId(), dog.isDefaultDog());
    }

    @Transactional
    public void delete(Long userId, Long dogId) {
        requireGuardian(userId, true);
        Dog dog = requireOwnedDog(userId, dogId);
        boolean deletedDefault = dog.isDefaultDog();
        Dog replacement = deletedDefault
                ? dogRepository.findFirstByUserIdAndIdNotOrderByIdAsc(userId, dogId).orElse(null)
                : null;

        dogRepository.delete(dog);
        dogRepository.flush();
        if (replacement != null) {
            replacement.designateAsDefault();
        }
    }

    private User requireGuardian(Long userId, boolean lock) {
        User user = (lock
                        ? userRepository.findByIdForUpdate(userId)
                        : userRepository.findById(userId))
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != UserRole.GUARDIAN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private Breed requireBreed(Long breedId) {
        return breedRepository.findByIdAndActiveTrue(breedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
    }

    private Dog requireOwnedDog(Long userId, Long dogId) {
        return dogRepository.findByIdAndUserId(dogId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
    }

    private void clearCurrentDefault(Long userId) {
        dogRepository.findByUserIdAndDefaultDogTrue(userId).ifPresent(current -> {
            current.clearDefault();
            dogRepository.flush();
        });
    }

    private DogDetailResponse toDetail(Dog dog) {
        return new DogDetailResponse(
                dog.getId(),
                dog.getName(),
                dog.getBreed().getId(),
                dog.getBreed().getName(),
                dog.getBirthDate(),
                dog.getWeightKg(),
                new ArrayList<>(dog.getEquipmentCodes()),
                dog.isDefaultDog(),
                dog.getVaccinationStatus());
    }
}
