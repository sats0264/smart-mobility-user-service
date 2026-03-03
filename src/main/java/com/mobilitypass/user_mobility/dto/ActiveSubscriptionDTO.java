package com.mobilitypass.user_mobility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSubscriptionDTO {
    private String offerName;
    private String subscriptionType;
    private String applicableTransport;
    private Double discountPercentage;
}
