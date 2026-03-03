package com.mobilitypass.user_mobility.service;

import com.mobilitypass.user_mobility.beans.MobilityPass;
import com.mobilitypass.user_mobility.enums.PassStatus;

import java.util.List;

public interface PassService {
    MobilityPass activatePass(String userId);

    void changePassStatus(String userId, PassStatus status);

    MobilityPass getUserPass(String userId);

    void addSpentAmount(String userId, Double amount);

    List<MobilityPass> getAllPasses();
}
