package com.mobilitypass.user_mobility.dto;

import com.mobilitypass.user_mobility.enums.PassType;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PricingContextDTO {
    private boolean hasActivePass;
    private PassType passType;
    private Double dailyCapAmount;
    private Double todaySpentAmount;
    private List<SubscriptionContextDTO> activeSubscriptions;
}