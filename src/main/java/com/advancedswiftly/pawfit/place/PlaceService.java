package com.advancedswiftly.pawfit.place;

import com.advancedswiftly.pawfit.business.application.BusinessHoursService;
import com.advancedswiftly.pawfit.business.domain.EntryConditions;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.global.storage.FileService;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlaceService {
    private static final RowMapper<Place> PLACE =
            (rs, row) ->
                    new Place(
                            rs.getString("place_id"),
                            rs.getString("name"),
                            rs.getString("address"),
                            rs.getBigDecimal("latitude"),
                            rs.getBigDecimal("longitude"),
                            rs.getString("category"));

    private final NamedParameterJdbcTemplate db;
    private final DogRepository dogs;
    private final StorePolicyRepository storePolicies;
    private final FileService files;
    private final BusinessHoursService hours;

    PlaceService(
            NamedParameterJdbcTemplate db,
            DogRepository dogs,
            StorePolicyRepository storePolicies,
            FileService files,
            BusinessHoursService hours) {
        this.storePolicies = storePolicies;
        this.files = files;
        this.hours = hours;
        this.db = db;
        this.dogs = dogs;
    }

    public Search search(
            Authentication auth,
            String keyword,
            Long regionId,
            String category,
            List<Long> dogIds,
            int page,
            int size) {
        List<Profile> profiles = profiles(auth, dogIds);
        var parameters =
                new MapSqlParameterSource()
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        categoryFilter(category, parameters, where);
        if (regionId != null) {
            where.append(" AND region_id = :region");
            parameters.addValue("region", regionId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND LOWER(name) LIKE LOWER(:keyword) ESCAPE '!'");
            parameters.addValue(
                    "keyword",
                    "%"
                            + keyword.strip()
                                    .replace("!", "!!")
                                    .replace("%", "!%")
                                    .replace("_", "!_")
                            + "%");
        }
        long total =
                db.queryForObject(
                        "SELECT COUNT(*) FROM place_catalog" + where, parameters, Long.class);
        var places =
                db.query(
                        "SELECT * FROM place_catalog"
                                + where
                                + " ORDER BY name, place_id LIMIT :limit OFFSET :offset",
                        parameters,
                        PLACE);
        return new Search(cards(places, profiles), page, total);
    }

    public MapResult map(
            Authentication auth,
            BigDecimal latitude,
            BigDecimal longitude,
            int radius,
            String category,
            List<Long> dogIds) {
        List<Profile> profiles = profiles(auth, dogIds);
        var parameters =
                new MapSqlParameterSource()
                        .addValue("lat", latitude.doubleValue())
                        .addValue("lon", longitude.doubleValue())
                        .addValue("radius", radius)
                        .addValue("latitudeDelta", Math.toDegrees(radius / 6371000.0));
        StringBuilder where =
                new StringBuilder(
                        " WHERE latitude BETWEEN :lat - :latitudeDelta AND :lat + :latitudeDelta");
        categoryFilter(category, parameters, where);
        var places =
                db.query(
                        """
                        SELECT * FROM (
                            SELECT place_catalog.*, 12742000 * ASIN(SQRT(LEAST(1.0,
                                POWER(SIN(RADIANS(latitude - :lat) / 2), 2)
                                + COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                                * POWER(SIN(RADIANS(longitude - :lon) / 2), 2)))) AS distance
                            FROM place_catalog
                        """
                                + where
                                + ") nearby WHERE distance <= :radius ORDER BY distance, place_id"
                                + " LIMIT 500",
                        parameters,
                        PLACE);
        return new MapResult(cards(places, profiles));
    }

    public Detail detail(Authentication auth, String placeId, List<Long> dogIds) {
        Place place = requirePlace(placeId);
        Policy policy = policies(List.of(placeId)).get(placeId);
        var result = evaluate(policy, profiles(auth, dogIds));
        Policy displayed = scope(policy, result.area());
        String googleMapsUrl =
                "https://www.google.com/maps/search/?api=1&query="
                        + place.latitude().stripTrailingZeros().toPlainString()
                        + "%2C"
                        + place.longitude().stripTrailingZeros().toPlainString();
        return new Detail(
                place.id(),
                place.name(),
                place.address(),
                place.latitude(),
                place.longitude(),
                result.overallVerdict(),
                result.dogs().stream().flatMap(dog -> dog.reasons().stream()).distinct().toList(),
                unrestricted(displayed),
                policy == null ? null : policy.source(),
                policy == null ? null : policy.confirmedOn(),
                googleMapsUrl,
                place.category(),
                phone(placeId),
                images(placeId),
                businessHours(placeId),
                policy == null ? null : policy.conditions(),
                displayed == null ? null : displayed.maxWeight(),
                displayed == null
                        ? List.of()
                        : displayed.rules().stream()
                                .filter(r -> r.type().equals("EQUIPMENT"))
                                .map(Rule::code)
                                .toList(),
                result.area());
    }

    public record PolicyPreview(Verdict before, Verdict after, String example) {}

    public PolicyPreview preview(
            Long storeId, com.advancedswiftly.pawfit.business.api.dto.RequestedPolicy requested) {
        String id = "STORE:" + storeId;
        Policy current = policies(List.of(id)).get(id);
        var conditions = requested.conditions();
        var mapped =
                requested.toRules().stream()
                        .filter(r -> r.isRequired())
                        .map(
                                r ->
                                        new Rule(
                                                id,
                                                r.getType().name().equals("BREED")
                                                        ? "BREED_DENIED"
                                                        : r.getType().name(),
                                                r.getCode()))
                        .toList();
        Policy proposed =
                new Policy(
                        id,
                        conditions == null || !Boolean.FALSE.equals(conditions.petsAllowed()),
                        conditions != null && conditions.indoor() != null
                                ? conditions.indoor().maxWeightKg()
                                : null,
                        "변경안",
                        EntryConditions.today(),
                        mapped,
                        conditions);
        var example =
                List.of(
                        new Profile(
                                0,
                                0,
                                BigDecimal.valueOf(8),
                                Set.of("STROLLER", "LEASH"),
                                Set.of()));
        return new PolicyPreview(
                evaluate(current, example).overallVerdict(),
                evaluate(proposed, example).overallVerdict(),
                "유모차와 리드줄을 보유한 8kg 반려견");
    }

    public Judgment judge(Authentication auth, String placeId, List<Long> dogIds) {
        requirePlace(placeId);
        return evaluate(policies(List.of(placeId)).get(placeId), profiles(auth, dogIds));
    }

    public List<Card> chatCandidates(
            Authentication auth, List<Long> dogIds, BigDecimal latitude, BigDecimal longitude) {
        return latitude == null
                ? search(auth, null, null, null, dogIds, 0, 12).items()
                : map(auth, latitude, longitude, 5000, null, dogIds).places().stream()
                        .limit(12)
                        .toList();
    }

    private List<Card> cards(List<Place> places, List<Profile> profiles) {
        Map<String, Policy> policies = policies(places.stream().map(Place::id).toList());
        return places.stream()
                .map(
                        place ->
                                new Card(
                                        place.id(),
                                        place.name(),
                                        place.latitude(),
                                        place.longitude(),
                                        evaluate(policies.get(place.id()), profiles)
                                                .overallVerdict(),
                                        place.category()))
                .toList();
    }

    private Place requirePlace(String id) {
        if (!id.matches("(STORE|PUBLIC):[1-9][0-9]{0,18}")) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        return db
                .query("SELECT * FROM place_catalog WHERE place_id=:id", Map.of("id", id), PLACE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    private Map<String, Policy> policies(List<String> ids) {
        if (ids.isEmpty()) return Map.of();
        var parameters = Map.of("ids", ids);
        var rules =
                db
                        .query(
                                """
                                SELECT place_id, rule_type, rule_code FROM place_entry_rule
                                WHERE place_id IN (:ids) ORDER BY rule_type, rule_code
                                """,
                                parameters,
                                (rs, row) ->
                                        new Rule(rs.getString(1), rs.getString(2), rs.getString(3)))
                        .stream()
                        .collect(Collectors.groupingBy(Rule::placeId));
        Map<String, Policy> result =
                db
                        .query(
                                """
SELECT place_id, pets_allowed, max_weight_kg, source_text, confirmed_on
FROM place_entry_policy WHERE place_id IN (:ids) AND confirmed_on <= CURRENT_DATE
""",
                                parameters,
                                (rs, row) ->
                                        new Policy(
                                                rs.getString(1),
                                                rs.getBoolean(2),
                                                rs.getBigDecimal(3),
                                                rs.getString(4),
                                                rs.getObject(5, LocalDate.class),
                                                rules.getOrDefault(rs.getString(1), List.of()),
                                                null))
                        .stream()
                        .collect(Collectors.toMap(Policy::id, policy -> policy));
        List<Long> storeIds =
                ids.stream()
                        .filter(id -> id.startsWith("STORE:"))
                        .map(id -> Long.valueOf(id.substring(6)))
                        .toList();
        Set<Long> seen = new java.util.HashSet<>();
        if (!storeIds.isEmpty())
            for (var current : storePolicies.findByStoreIdInOrderByVersionDesc(storeIds)) {
                EntryConditions c = current.getConditions();
                if (c != null && !c.activeOn(EntryConditions.today())) continue;
                if (!seen.add(current.getStoreId())) continue;
                String id = "STORE:" + current.getStoreId();
                Policy old = result.get(id);
                List<Rule> mapped =
                        current.getRules().stream()
                                .filter(r -> r.isRequired())
                                .map(
                                        r ->
                                                new Rule(
                                                        id,
                                                        r.getType().name().equals("BREED")
                                                                ? "BREED_DENIED"
                                                                : r.getType().name(),
                                                        r.getCode()))
                                .toList();
                result.put(
                        id,
                        new Policy(
                                id,
                                c != null && c.petsAllowed() != null
                                        ? c.petsAllowed()
                                        : old == null || old.allowed(),
                                c != null && c.indoor() != null
                                        ? c.indoor().maxWeightKg()
                                        : old == null ? null : old.maxWeight(),
                                current.getSourceText(),
                                c != null && c.effectiveFrom() != null
                                        ? c.effectiveFrom()
                                        : current.getCreatedAt()
                                                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                                .toLocalDate(),
                                mapped,
                                c));
            }
        return result;
    }

    private String phone(String placeId) {
        boolean store = placeId.startsWith("STORE:");
        return db.queryForObject(
                "select phone from "
                        + (store
                                ? "store where store_id=:id"
                                : "public_facility where facility_id=:id"),
                Map.of("id", Long.valueOf(placeId.substring(store ? 6 : 7))),
                String.class);
    }

    private List<String> images(String placeId) {
        if (placeId.startsWith("STORE:"))
            return files.storeImages(Long.valueOf(placeId.substring(6)));
        return db.queryForList(
                "select image_url from public_facility_image where facility_id=:id order by"
                        + " image_id",
                Map.of("id", Long.valueOf(placeId.substring(7))),
                String.class);
    }

    private Object businessHours(String placeId) {
        if (placeId.startsWith("STORE:")) return hours.items(Long.valueOf(placeId.substring(6)));
        return db.queryForObject(
                "select business_hours from public_facility where facility_id=:id",
                Map.of("id", Long.valueOf(placeId.substring(7))),
                String.class);
    }

    private List<Profile> profiles(Authentication auth, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        if (auth == null
                || auth.getAuthorities().stream()
                        .noneMatch(role -> role.getAuthority().equals("ROLE_GUARDIAN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        long userId = Long.parseLong(auth.getName());
        return new LinkedHashSet<>(ids)
                .stream()
                        .map(
                                id -> {
                                    var dog =
                                            dogs.findByIdAndUserId(id, userId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new BusinessException(
                                                                            ErrorCode
                                                                                    .DOG_NOT_OWNED));
                                    var vaccines =
                                            db.queryForList(
                                                    """
SELECT i.vaccine_code FROM vaccination_record r
JOIN vaccination_item i ON i.record_id = r.record_id
WHERE r.dog_id = :id AND r.status = 'VERIFIED' AND i.source = 'OPERATOR'
AND i.vaccinated_at <= CURRENT_DATE
""",
                                                    Map.of("id", id),
                                                    String.class);
                                    return new Profile(
                                            id,
                                            dog.getBreed().getId(),
                                            dog.getWeightKg(),
                                            dog.getEquipmentCodes(),
                                            Set.copyOf(vaccines));
                                })
                        .toList();
    }

    private Policy scope(Policy policy, String area) {
        if (policy == null || policy.conditions() == null || area == null) return policy;
        var rule =
                area.equals("INDOOR")
                        ? policy.conditions().indoor()
                        : policy.conditions().outdoor();
        if (rule == null) return policy;
        var mapped =
                new ArrayList<>(
                        policy.rules().stream()
                                .filter(r -> !r.type().equals("EQUIPMENT"))
                                .toList());
        for (String code : rule.equipmentCodes())
            mapped.add(new Rule(policy.id(), "EQUIPMENT", code));
        return new Policy(
                policy.id(),
                policy.allowed(),
                rule.maxWeightKg(),
                policy.source(),
                policy.confirmedOn(),
                mapped,
                null);
    }

    private Judgment evaluate(Policy policy, List<Profile> profiles) {
        if (policy != null
                && policy.allowed()
                && policy.conditions() != null
                && (policy.conditions().indoor() != null
                        || policy.conditions().outdoor() != null)) {
            var choices = new ArrayList<Judgment>();
            for (String area : List.of("INDOOR", "OUTDOOR")) {
                EntryConditions.Area rule =
                        area.equals("INDOOR")
                                ? policy.conditions().indoor()
                                : policy.conditions().outdoor();
                if (rule == null || Boolean.FALSE.equals(rule.allowed())) continue;
                List<Rule> areaRules =
                        new ArrayList<>(
                                policy.rules().stream()
                                        .filter(r -> !r.type().equals("EQUIPMENT"))
                                        .toList());
                for (String code : rule.equipmentCodes())
                    areaRules.add(new Rule(policy.id(), "EQUIPMENT", code));
                Policy scoped =
                        new Policy(
                                policy.id(),
                                true,
                                rule.maxWeightKg(),
                                policy.source(),
                                policy.confirmedOn(),
                                areaRules,
                                null);
                var results =
                        profiles.stream()
                                .map(p -> evaluateDog(scoped, p))
                                .map(
                                        r -> {
                                            if (!area.equals("OUTDOOR")
                                                    || r.verdict() == Verdict.ENTRY_DENIED)
                                                return r;
                                            var reasons = new ArrayList<>(r.reasons());
                                            reasons.add("OUTDOOR_ONLY");
                                            return new DogResult(r.dogId(), r.verdict(), reasons);
                                        })
                                .toList();
                choices.add(
                        new Judgment(
                                results.stream()
                                        .map(DogResult::verdict)
                                        .max(Verdict::compareTo)
                                        .orElse(Verdict.CHECK_REQUIRED),
                                results,
                                area));
            }
            return choices.stream()
                    .min(java.util.Comparator.comparing(Judgment::overallVerdict))
                    .orElseGet(
                            () ->
                                    new Judgment(
                                            Verdict.ENTRY_DENIED,
                                            profiles.stream()
                                                    .map(
                                                            p ->
                                                                    new DogResult(
                                                                            p.id(),
                                                                            Verdict.ENTRY_DENIED,
                                                                            List.of(
                                                                                    "NO_ALLOWED_AREA")))
                                                    .toList()));
        }
        var results = profiles.stream().map(profile -> evaluateDog(policy, profile)).toList();
        Verdict fallback =
                policy == null
                        ? Verdict.CHECK_REQUIRED
                        : !policy.allowed() ? Verdict.ENTRY_DENIED : Verdict.CHECK_REQUIRED;
        return new Judgment(
                results.stream().map(DogResult::verdict).max(Verdict::compareTo).orElse(fallback),
                results);
    }

    private DogResult evaluateDog(Policy policy, Profile dog) {
        if (policy == null)
            return new DogResult(dog.id(), Verdict.CHECK_REQUIRED, List.of("POLICY_UNCONFIRMED"));
        if (!policy.allowed())
            return new DogResult(dog.id(), Verdict.ENTRY_DENIED, List.of("PETS_NOT_ALLOWED"));
        Verdict verdict = Verdict.ENTRY_OK;
        var reasons = new ArrayList<String>();
        if (policy.maxWeight() != null) {
            if (dog.weight() == null) {
                verdict = Verdict.CHECK_REQUIRED;
                reasons.add("WEIGHT_UNKNOWN");
            } else if (dog.weight().compareTo(policy.maxWeight()) > 0) {
                verdict = Verdict.ENTRY_DENIED;
                reasons.add("WEIGHT_LIMIT_EXCEEDED");
            }
        }
        for (Rule rule : policy.rules()) {
            if (rule.type().equals("BREED_DENIED")
                    && rule.code().equals(Long.toString(dog.breedId()))) {
                verdict = Verdict.ENTRY_DENIED;
                reasons.add("BREED_RESTRICTED");
            } else if (rule.type().equals("EQUIPMENT") && !dog.equipment().contains(rule.code())) {
                verdict = verdict.worse(Verdict.PREPARE);
                reasons.add(rule.code() + "_REQUIRED");
            } else if (rule.type().equals("VACCINE") && !dog.vaccines().contains(rule.code())) {
                verdict = verdict.worse(Verdict.PREPARE);
                reasons.add(rule.code() + "_VACCINATION_REQUIRED");
            }
        }
        return new DogResult(dog.id(), verdict, reasons);
    }

    private List<String> unrestricted(Policy policy) {
        if (policy == null || !policy.allowed()) return List.of();
        var fields = new ArrayList<String>();
        if (policy.maxWeight() == null) fields.add("WEIGHT");
        for (String type : List.of("BREED_DENIED", "EQUIPMENT", "VACCINE")) {
            if (policy.rules().stream().noneMatch(rule -> rule.type().equals(type))) {
                fields.add(type.equals("BREED_DENIED") ? "BREED" : type);
            }
        }
        return fields;
    }

    private void categoryFilter(
            String category, MapSqlParameterSource parameters, StringBuilder where) {
        if (category != null && !category.isBlank()) {
            where.append(" AND UPPER(category) = UPPER(:category)");
            parameters.addValue("category", category.strip());
        }
    }

    public enum Verdict {
        ENTRY_OK,
        PREPARE,
        CHECK_REQUIRED,
        ENTRY_DENIED;

        Verdict worse(Verdict other) {
            return compareTo(other) < 0 ? other : this;
        }
    }

    private record Place(
            String id,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category) {}

    private record Profile(
            long id,
            long breedId,
            BigDecimal weight,
            Set<String> equipment,
            Set<String> vaccines) {}

    private record Rule(String placeId, String type, String code) {}

    private record Policy(
            String id,
            boolean allowed,
            BigDecimal maxWeight,
            String source,
            LocalDate confirmedOn,
            List<Rule> rules,
            EntryConditions conditions) {}

    public record Card(
            String placeId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            Verdict verdict,
            String category) {
        public Card(
                String placeId,
                String name,
                BigDecimal latitude,
                BigDecimal longitude,
                Verdict verdict) {
            this(placeId, name, latitude, longitude, verdict, null);
        }
    }

    record Search(List<Card> items, int page, long total) {}

    record MapResult(List<Card> places) {}

    record Detail(
            String placeId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Verdict verdict,
            List<String> reasons,
            List<String> unrestrictedFields,
            String policySource,
            LocalDate confirmedOn,
            String googleMapsUrl,
            String category,
            String phoneNumber,
            List<String> imageUrls,
            Object businessHours,
            EntryConditions conditions,
            BigDecimal maxWeightKg,
            List<String> requiredEquipmentCodes,
            String entryArea) {}

    record DogResult(long dogId, Verdict verdict, List<String> reasons) {}

    record Judgment(Verdict overallVerdict, List<DogResult> dogs, String area) {
        Judgment(Verdict overallVerdict, List<DogResult> dogs) {
            this(overallVerdict, dogs, null);
        }
    }
}
