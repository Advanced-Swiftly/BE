package com.advancedswiftly.pawfit.business.application;

import org.springframework.stereotype.Service;

@Service
public class BusinessStatusService {

    private final BusinessStatusClient client;

    public BusinessStatusService(BusinessStatusClient client) {
        this.client = client;
    }

    public boolean isActive(String businessNumber) {
        return client.isActive(businessNumber.replace("-", ""));
    }
}
