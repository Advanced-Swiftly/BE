package com.advancedswiftly.pawfit.dog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.domain.Breed;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.BreedRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BreedRepository breedRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private User otherGuardian;
    private User businessUser;
    private Breed maltese;
    private Breed poodle;

    @AfterEach
    void tearDown() {
        dogRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        dogRepository.deleteAll();
        userRepository.deleteAll();
        breedRepository.deleteAll();
        owner = userRepository.saveAndFlush(User.register(
                "01011112222", passwordEncoder.encode("password123!"), "보호자", UserRole.GUARDIAN));
        otherGuardian = userRepository.saveAndFlush(User.register(
                "01033334444", passwordEncoder.encode("password123!"), "다른 보호자", UserRole.GUARDIAN));
        businessUser = userRepository.saveAndFlush(User.register(
                "01055556666", passwordEncoder.encode("password123!"), "업체", UserRole.BUSINESS));
        maltese = breedRepository.saveAndFlush(Breed.create("말티즈"));
        poodle = breedRepository.saveAndFlush(Breed.create("푸들"));
    }

    @Test
    void guardianCanManageDogsAndDefaultDogIsMaintained() throws Exception {
        long firstDogId = createDog(owner, "콩이", maltese.getId(), false);
        long secondDogId = createDog(owner, "두부", poodle.getId(), false);

        mockMvc.perform(get("/api/v1/dogs")
                        .with(userId(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogs.length()").value(2))
                .andExpect(jsonPath("$.dogs[0].dogId").value(firstDogId))
                .andExpect(jsonPath("$.dogs[0].isDefault").value(true))
                .andExpect(jsonPath("$.dogs[1].isDefault").value(false));

        mockMvc.perform(get("/api/v1/dogs/{dogId}", firstDogId)
                        .with(userId(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("콩이"))
                .andExpect(jsonPath("$.breedName").value("말티즈"))
                .andExpect(jsonPath("$.equipmentCodes[0]").value("MUZZLE"))
                .andExpect(jsonPath("$.vaccinationStatus").value("NOT_SUBMITTED"));

        String updateBody = """
                {
                  "name":"콩콩이",
                  "breedId":%d,
                  "weightKg":5.20,
                  "equipmentCodes":["CARRIER","LEASH"]
                }
                """.formatted(poodle.getId());
        mockMvc.perform(patch("/api/v1/dogs/{dogId}", firstDogId)
                        .with(userId(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogId").value(firstDogId))
                .andExpect(jsonPath("$.name").value("콩콩이"))
                .andExpect(jsonPath("$.weightKg").value(5.2));

        mockMvc.perform(patch("/api/v1/dogs/{dogId}/default", secondDogId)
                        .with(userId(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogId").value(secondDogId))
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(delete("/api/v1/dogs/{dogId}", secondDogId)
                        .with(userId(owner)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(dogRepository.findByIdAndUserId(firstDogId, owner.getId()))
                .hasValueSatisfying(dog -> {
                    assertThat(dog.isDefaultDog()).isTrue();
                    assertThat(dog.getName()).isEqualTo("콩콩이");
                    assertThat(dog.getBreed().getId()).isEqualTo(poodle.getId());
                    assertThat(dog.getEquipmentCodes()).containsExactlyInAnyOrder("CARRIER", "LEASH");
                });
    }

    @Test
    void anotherGuardianCannotAccessOwnedDog() throws Exception {
        long dogId = createDog(owner, "콩이", maltese.getId(), false);

        mockMvc.perform(get("/api/v1/dogs/{dogId}", dogId)
                        .with(userId(otherGuardian)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/dogs/{dogId}/default", dogId)
                        .with(userId(otherGuardian)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/dogs/{dogId}", dogId)
                        .with(userId(otherGuardian)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"));
    }

    @Test
    void guardianCannotRegisterMoreThanFiveDogs() throws Exception {
        IntStream.rangeClosed(1, 5).forEach(index -> {
            try {
                createDog(owner, "반려견" + index, maltese.getId(), false);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });

        mockMvc.perform(post("/api/v1/dogs")
                        .with(userId(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("여섯째", maltese.getId(), false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOG_LIMIT_EXCEEDED"));
    }

    @Test
    void invalidRequestsAndNonGuardianRequestsAreRejected() throws Exception {
        String invalidBody = """
                {"name":" ","breedId":999999,"birthDate":"2999-01-01","weightKg":0}
                """;
        mockMvc.perform(post("/api/v1/dogs")
                        .with(userId(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        long dogId = createDog(owner, "콩이", maltese.getId(), false);
        mockMvc.perform(patch("/api/v1/dogs/{dogId}", dogId)
                        .with(userId(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(get("/api/v1/dogs")
                        .with(user(businessUser.getId().toString()).roles("BUSINESS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(get("/api/v1/dogs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    private long createDog(User user, String name, Long breedId, boolean isDefault) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/dogs")
                        .with(userId(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(name, breedId, isDefault)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dogId").isNumber())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.dogId"))
                .longValue();
    }

    private String createBody(String name, Long breedId, boolean isDefault) {
        return """
                {
                  "name":"%s",
                  "breedId":%d,
                  "birthDate":"2021-05-01",
                  "weightKg":4.20,
                  "equipmentCodes":["MUZZLE"],
                  "isDefault":%s
                }
                """.formatted(name, breedId, isDefault);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userId(User user) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(user.getId().toString())
                .roles("GUARDIAN");
    }
}
