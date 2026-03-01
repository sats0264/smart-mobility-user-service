package com.mobilitypass.user_mobility.service;

import com.mobilitypass.user_mobility.beans.UserProfile;
import com.mobilitypass.user_mobility.dto.UserProfileDTO;
import com.mobilitypass.user_mobility.error.ResourceNotFoundException;
import com.mobilitypass.user_mobility.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.mobilitypass.user_mobility.beans.MobilityPass;
import com.mobilitypass.user_mobility.beans.Subscriptions;
import com.mobilitypass.user_mobility.dto.UserMobilitySummaryDTO;
import com.mobilitypass.user_mobility.repository.SubscriptionRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mobilitypass.user_mobility.proxy.BillingProxy;
import com.mobilitypass.user_mobility.proxy.BillingProxy.CreateAccountRequest;

import java.util.List;

/**
 * Implémentation du service Utilisateur.
 * Gère la création des profils métier liés aux identifiants Keycloak.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private PassService passService;

    @Autowired
    private SubscriptionRepository subRepository;

    @Autowired
    private BillingProxy billingProxy;

    @Override
    public UserProfile createProfile(UserProfileDTO dto) {
        UserProfile profile = UserProfile.builder()
                .keycloakId(dto.getKeycloakId())
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .createdAt(LocalDateTime.now())
                .build();

        UserProfile savedProfile = profileRepository.save(profile);

        // Auto-activation du pass lors de la création du profil (Standard par défaut)
        passService.activatePass(savedProfile.getKeycloakId());

        // Créer le compte facturation via API Synchrone
        try {
            billingProxy.createAccount(new CreateAccountRequest(savedProfile.getKeycloakId(), "XOF"));
        } catch (Exception e) {
            // Logger l'erreur, mais ne pas faire échouer la création de l'utilisateur
            // (Idéalement, on utilise RabbitMQ pour ce type de pattern)
            System.err.println("Échec de la création du compte de facturation: " + e.getMessage());
        }

        return savedProfile;
    }

    @Override
    public UserProfile getUser(String keycloakId) {
        return profileRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserProfile non trouvé avec l'ID Keycloak : " + keycloakId));
    }

    @Override
    public UserProfile getOrCreateProfile(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return profileRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfileDTO dto = new UserProfileDTO();
                    dto.setKeycloakId(keycloakId);
                    dto.setEmail(jwt.getClaimAsString("email"));
                    dto.setFirstName(jwt.getClaimAsString("given_name"));
                    dto.setLastName(jwt.getClaimAsString("family_name"));
                    return createProfile(dto);
                });
    }

    @Override
    public UserMobilitySummaryDTO getSummary(String keycloakId) {
        UserProfile user = getUser(keycloakId);
        MobilityPass pass = passService.getUserPass(keycloakId);
        List<Subscriptions> activeSubs = subRepository.findByUserIdAndStatus(keycloakId, "ACTIVE");

        /// TODO: Gerer abonnement de type type différent (ex: 20% sur TER, 10% sur BRT, etc.) en fonction du type de pass et des règles métier
        Double discount = activeSubs.stream()
                .map(Subscriptions::getDiscountPercentage)
                .findFirst().orElse(0.0);

        return UserMobilitySummaryDTO.builder()
                .keycloakId(keycloakId)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .hasActivePass("ACTIVE".equalsIgnoreCase(pass.getStatus()))
                .passType(pass.getPassType())
                .passStatus(pass.getStatus())
                .dailyCap(pass.getDailyCapAmount())
                .currentSpent(pass.getTodaySpentAmount())
                .activeDiscountRate(discount)
                .build();
    }
}
