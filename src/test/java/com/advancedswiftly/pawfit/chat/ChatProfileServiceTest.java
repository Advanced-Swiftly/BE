package com.advancedswiftly.pawfit.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.advancedswiftly.pawfit.dog.domain.Dog;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;

import org.junit.jupiter.api.Test;

import java.util.*;

class ChatProfileServiceTest {
    @Test
    void requiresExplicitConfirmationAndRejectsOtherOwner() {
        var dogs = mock(DogRepository.class);
        var actions = mock(ChatProfileActionRepository.class);
        var dog = mock(Dog.class);
        when(dog.getId()).thenReturn(12L);
        when(dog.getName()).thenReturn("콩이");
        when(dog.getEquipmentCodes()).thenReturn(Set.of("LEASH"));
        when(dogs.findAllByUserIdOrderByDefaultDogDescIdAsc(1L)).thenReturn(List.of(dog));
        when(actions.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new ChatProfileService(dogs, actions);
        assertThat(service.propose(1, 2, "콩이는 유모차가 필요할까?")).isNull();
        var proposal = service.propose(1, 2, "콩이 프로필에 유모차 추가");
        assertThat(proposal.equipmentCode()).isEqualTo("STROLLER");
        verify(dog, never()).replaceEquipmentCodes(any());
        var action = new ChatProfileAction(1L, 2L, 12L, "STROLLER");
        when(actions.lock(action.getId())).thenReturn(Optional.of(action));
        assertThatThrownBy(() -> service.confirm(99, 2, action.getId()))
                .isInstanceOf(com.advancedswiftly.pawfit.global.error.BusinessException.class);
        when(dogs.findByIdAndUserId(12L, 1L)).thenReturn(Optional.of(dog));
        assertThat(service.confirm(1, 2, action.getId()).get("applied")).isEqualTo(true);
        verify(dog)
                .replaceEquipmentCodes(argThat(v -> v.containsAll(List.of("LEASH", "STROLLER"))));
        service.confirm(1, 2, action.getId());
        verify(dog, times(1)).replaceEquipmentCodes(any());
    }
}
