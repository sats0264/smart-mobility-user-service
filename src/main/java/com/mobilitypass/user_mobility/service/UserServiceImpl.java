package com.mobilitypass.user_mobility.service;

import com.mobilitypass.user_mobility.beans.UserProfile;
import com.mobilitypass.user_mobility.dto.UserProfileDTO;
import com.mobilitypass.user_mobility.enums.PassStatus;
import com.mobilitypass.user_mobility.error.ResourceNotFoundException;
import com.mobilitypass.user_mobility.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.mobilitypass.user_mobility.beans.MobilityPass;
import com.mobilitypass.user_mobility.beans.Subscriptions;
import com.mobilitypass.user_mobility.dto.UserMobilitySummaryDTO;
import com.mobilitypass.user_mobility.dto.ActiveSubscriptionDTO;
import com.mobilitypass.user_mobility.dto.PricingContextDTO;
import com.mobilitypass.user_mobility.dto.SubscriptionContextDTO;
import com.mobilitypass.user_mobility.repository.SubscriptionRepository;
import com.mobilitypass.user_mobility.repository.SubscriptionOfferRepository;
import com.mobilitypass.user_mobility.beans.SubscriptionOffer;

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

    // passService not required here; pass lookup done via MobilityPassRepository

    @Autowired
    private com.mobilitypass.user_mobility.repository.MobilityPassRepository passRepository;

    @Autowired
    private SubscriptionRepository subRepository;

    @Autowired
    private BillingProxy billingProxy;

    @Autowired
    private SubscriptionOfferRepository subOfferRepository;

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
        // Note: removed automatic pass activation. Passes must be purchased from the
        // catalog.

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
    public UserProfile getOrCreateProfile(String userId, String email, String name) {
        return profileRepository.findByKeycloakId(userId)
                .orElseGet(() -> {
                    UserProfileDTO dto = new UserProfileDTO();
                    dto.setKeycloakId(userId);
                    dto.setEmail(email);

                    // Split Name (simple version)
                    if (name != null) {
                        String[] parts = name.split(" ", 2);
                        dto.setFirstName(parts[0]);
                        if (parts.length > 1)
                            dto.setLastName(parts[1]);
                    }

                    return createProfile(dto);
                });
    }

    @Override
    public UserMobilitySummaryDTO getSummary(String keycloakId) {
        UserProfile user = getUser(keycloakId);
        MobilityPass pass = passRepository.findByUserId(keycloakId).orElse(null);
        List<Subscriptions> activeSubs = subRepository.findByUserIdAndStatus(keycloakId, "ACTIVE");

        List<ActiveSubscriptionDTO> subscriptionDTOs = activeSubs.stream()
                .map(sub -> {
                    String name = "Abonnement " + sub.getSubscriptionType();
                    if (sub.getOfferId() != null) {
                        name = subOfferRepository.findById(sub.getOfferId())
                                .map(SubscriptionOffer::getName)
                                .orElse(name);
                    }
                    return ActiveSubscriptionDTO.builder()
                            .offerName(name)
                            .subscriptionType(sub.getSubscriptionType())
                            .applicableTransport(sub.getApplicableTransport())
                            .discountPercentage(sub.getDiscountPercentage())
                            .build();
                })
                .toList();

        return UserMobilitySummaryDTO.builder()
                .keycloakId(keycloakId)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .hasActivePass(pass != null && PassStatus.ACTIVE.equals(pass.getStatus()))
                .passType(pass != null ? pass.getPassType() : null)
                .passStatus(pass != null ? pass.getStatus() : null)
                .dailyCap(pass != null ? pass.getDailyCapAmount() : null)
                .activeSubscriptions(subscriptionDTOs)
                .build();
    }

    @Override
    public PricingContextDTO getPricingContext(String userId) {
        MobilityPass pass = passRepository.findByUserId(userId).orElse(null);
        List<Subscriptions> activeSubs = subRepository.findByUserIdAndStatus(userId, "ACTIVE");

        List<SubscriptionContextDTO> subscriptionContexts = activeSubs.stream()
                .map(sub -> SubscriptionContextDTO.builder()
                        .applicableTransport(sub.getApplicableTransport())
                        .discountPercentage(sub.getDiscountPercentage())
                        .build())
                .toList();

        return PricingContextDTO.builder()
                .hasActivePass(pass != null && PassStatus.ACTIVE.equals(pass.getStatus()))
                .passType(pass != null ? pass.getPassType() : null)
                .dailyCapAmount(pass != null ? pass.getDailyCapAmount() : null)
                .activeSubscriptions(subscriptionContexts)
                .build();
    }

    @Override
    public List<UserProfile> getAllUsers() {
        return profileRepository.findAll();
    }

    @Override
    public UserProfile suspendUser(String keycloakId, String reason) {
        UserProfile user = getUser(keycloakId);
        user.setActive(false);
        user.setSuspensionReason(reason);
        return profileRepository.save(user);
    }

    @Override
    public UserProfile reactivateUser(String keycloakId) {
        UserProfile user = getUser(keycloakId);
        user.setActive(true);
        user.setSuspensionReason(null);
        return profileRepository.save(user);
    }
}
