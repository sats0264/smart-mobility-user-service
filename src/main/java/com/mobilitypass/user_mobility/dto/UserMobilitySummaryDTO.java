package com.mobilitypass.user_mobility.dto;

import com.mobilitypass.user_mobility.enums.PassStatus;
import com.mobilitypass.user_mobility.enums.PassType;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserMobilitySummaryDTO {
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private boolean hasActivePass;
    private PassType passType;
    private PassStatus passStatus;
    private Double dailyCap;
    private Double currentSpent;
    private List<ActiveSubscriptionDTO> activeSubscriptions;
}
