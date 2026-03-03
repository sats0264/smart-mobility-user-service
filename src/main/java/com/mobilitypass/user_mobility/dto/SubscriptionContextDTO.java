package com.mobilitypass.user_mobility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionContextDTO {
    private String applicableTransport;
    private Double discountPercentage;
}