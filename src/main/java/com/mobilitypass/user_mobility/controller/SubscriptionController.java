package com.mobilitypass.user_mobility.controller;

import com.mobilitypass.user_mobility.beans.Subscriptions;
import com.mobilitypass.user_mobility.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour la gestion des abonnements.
 *
 * <p>
 * <strong>Architecture Gateway :</strong><br>
 * L'identité de l'utilisateur est fournie via le header {@code X-User-Id}
 * injecté par le Gateway. Les endpoints "mes abonnements" ({@code /me/*})
 * utilisent cet header.
 */
@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // =========================================================================
    // Endpoints "mes abonnements" — lus depuis le header X-User-Id (Gateway)
    // =========================================================================

    /**
     * Retourne les abonnements actifs de l'utilisateur authentifié.
     */
    @GetMapping("/me/active")
    public ResponseEntity<List<Subscriptions>> getMyActiveSubscriptions(
            @RequestHeader("X-User-Id") String userId) {
        log.debug("Récupération des abonnements actifs → userId: {}", userId);
        return ResponseEntity.ok(subscriptionService.getActiveSubscriptions(userId));
    }

    /**
     * Crée un abonnement pour l'utilisateur authentifié.
     *
     * <p>
     * L'userId est lu depuis le header {@code X-User-Id} (injecté par le Gateway),
     * pas depuis le body, pour éviter toute manipulation côté client.
     */
    @PostMapping("/me")
    public ResponseEntity<Subscriptions> createMySubscription(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SubscriptionRequest request) {

        log.info("Création d'abonnement → userId: {}, type: {}", userId, request.subscriptionType());
        Subscriptions saved = subscriptionService.createSubscription(
                userId,
                request.subscriptionType(),
                request.discountPercentage());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // =========================================================================
    // Endpoints inter-services — accessibles par keycloakId (usage interne)
    // =========================================================================

    /**
     * Retourne les abonnements actifs d'un utilisateur par Keycloak ID.
     * Usage inter-services (ex: trip-management calcule la remise).
     */
    @GetMapping("/active/{userId}")
    public ResponseEntity<List<Subscriptions>> getActiveSubscriptions(
            @PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriptions(userId));
    }

    /**
     * Crée un abonnement pour un utilisateur via son Keycloak ID.
     * Usage backoffice / admin.
     */
    @PostMapping
    public ResponseEntity<Subscriptions> create(@RequestBody Subscriptions sub) {
        Subscriptions savedSub = subscriptionService.createSubscription(
                sub.getUserId(),
                sub.getSubscriptionType(),
                sub.getDiscountPercentage());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSub);
    }

    // =========================================================================
    // DTO interne pour la création d'abonnement
    // =========================================================================

    /**
     * Record DTO pour la création d'un abonnement via l'API utilisateur.
     * L'userId n'est PAS dans ce DTO (il vient du header X-User-Id).
     */
    public record SubscriptionRequest(
            String subscriptionType,
            Double discountPercentage) {
    }
}
