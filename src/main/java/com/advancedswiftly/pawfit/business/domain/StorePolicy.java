package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 업체에 적용 중인 입장 정책의 한 버전. */
@Entity
@Table(name = "store_policy")
public class StorePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "source_text", length = 2000)
    private String sourceText;

    /** 변경 주체(예: OPERATOR). 이력이 기록되기 전의 정책은 비어 있다. */
    @Column(name = "changed_by", length = 20)
    private String changedBy;

    /** 이 정책을 만든 조건 변경 요청. */
    @Column(name = "request_id")
    private Long requestId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "store_policy_rule", joinColumns = @JoinColumn(name = "policy_id"))
    private List<PolicyRule> rules = new ArrayList<>();

    protected StorePolicy() {
    }

    private StorePolicy(Long storeId, int version, String sourceText, List<PolicyRule> rules) {
        this.storeId = storeId;
        this.version = version;
        this.sourceText = sourceText;
        this.rules = new ArrayList<>(rules);
    }

    public static StorePolicy create(Long storeId, int version, String sourceText, List<PolicyRule> rules) {
        return new StorePolicy(storeId, version, sourceText, rules);
    }

    /** 조건 변경 요청이 승인되어 만들어진 정책. */
    public static StorePolicy createFromRequest(
            Long storeId, int version, String sourceText, List<PolicyRule> rules, String changedBy, Long requestId) {
        StorePolicy policy = new StorePolicy(storeId, version, sourceText, rules);
        policy.changedBy = changedBy;
        policy.requestId = requestId;
        return policy;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public int getVersion() {
        return version;
    }

    public String getSourceText() {
        return sourceText;
    }

    public List<PolicyRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}
