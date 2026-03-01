package com.mobilitypass.user_mobility.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Gère les abonnements de transport de l'utilisateur.
 * Utilisé par le Pricing Service pour appliquer des réductions.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscriptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId; // keycloakId
    private String subscriptionType;
    private String applicableTransport;
    private Double discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
