package com.mobilitypass.user_mobility.beans;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Représente le profil utilisateur métier lié à une identité Keycloak.
 * Ce service ne gère plus l'authentification (déléguée à Keycloak),
 * mais stocke les informations complémentaires nécessaires au métier.
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    private String keycloakId;

    private String email;
    private String firstName;
    private String lastName;

    private LocalDateTime createdAt;

    @Builder.Default
    private boolean active = true;

    private String suspensionReason;
}
