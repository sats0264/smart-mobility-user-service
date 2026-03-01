package com.mobilitypass.user_mobility.controller;

import com.mobilitypass.user_mobility.beans.UserProfile;
import com.mobilitypass.user_mobility.dto.UserMobilitySummaryDTO;
import com.mobilitypass.user_mobility.dto.UserProfileDTO;
import com.mobilitypass.user_mobility.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion des profils utilisateurs.
 *
 * <p>
 * <strong>Architecture Gateway :</strong><br>
 * Ce service ne valide PAS les JWT. L'authentification est déléguée au Gateway
 * (smart-mobility-gateways) qui injecte les informations d'identité via des
 * headers internes sécurisés :
 * <ul>
 * <li>{@code X-User-Id} → Subject Keycloak (UUID)</li>
 * <li>{@code X-User-Email} → Email de l'utilisateur</li>
 * <li>{@code X-User-Name} → Nom complet</li>
 * <li>{@code X-User-Role} → Rôles Spring Security (ex:
 * ROLE_USER,ROLE_ADMIN)</li>
 * </ul>
 *
 * <p>
 * Endpoints publics (dans le Gateway : {@code permitAll()}) :
 * <ul>
 * <li>{@code POST /api/users/register} → Création du profil métier (première
 * connexion)</li>
 * </ul>
 *
 * <p>
 * Endpoints protégés (dans le Gateway : {@code hasRole("USER")}) :
 * <ul>
 * <li>{@code GET /api/users/me} → Profil de l'utilisateur courant</li>
 * <li>{@code GET /api/users/summary/me} → Résumé complet (pass +
 * abonnements)</li>
 * <li>{@code GET /api/users/{keycloakId}} → Profil par ID (usage interne
 * inter-services)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =========================================================================
    // Endpoint PUBLIC — accessible sans token (Gateway: permitAll)
    // Appelé lors de la première connexion pour créer le profil métier en BDD
    // =========================================================================

    /**
     * Crée le profil métier à partir des headers injectés par le Gateway.
     * Idempotent : si le profil existe déjà, retourne le profil existant (200).
     *
     * <p>
     * Appelé par le client mobile après le premier login Keycloak réussi.
     * Le Gateway route {@code /api/users/register} en {@code permitAll()} pour
     * que cet appel puisse se faire avec un token fraîchement généré.
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfile> register(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Name", required = false) String name) {

        log.info("Enregistrement/récupération du profil → userId: {}", userId);

        // Idempotent : crée si absent, retourne l'existant sinon
        UserProfile profile = userService.getOrCreateProfile(userId, email, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    // =========================================================================
    // Endpoints PROTÉGÉS — token avec ROLE_USER requis (Gateway: hasRole)
    // =========================================================================

    /**
     * Retourne le profil de l'utilisateur actuellement authentifié.
     * L'identité est lue depuis les headers injectés par le Gateway.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMe(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Name", required = false) String name) {

        log.debug("Récupération du profil → userId: {}", userId);
        return ResponseEntity.ok(userService.getOrCreateProfile(userId, email, name));
    }

    /**
     * Retourne le résumé complet (profil + pass + abonnements actifs)
     * de l'utilisateur actuellement authentifié.
     */
    @GetMapping("/summary/me")
    public ResponseEntity<UserMobilitySummaryDTO> getMySummary(
            @RequestHeader("X-User-Id") String userId) {

        log.debug("Récupération du résumé → userId: {}", userId);
        return ResponseEntity.ok(userService.getSummary(userId));
    }

    /**
     * Retourne un profil utilisateur par son Keycloak ID.
     *a
     * <p>
     * Usage principal : appels inter-services (ex: billing, trip-management)
     * via Feign Client. Ces services passent le {@code keycloakId} qu'ils ont
     * reçu dans leur propre header {@code X-User-Id}.
     */
    @GetMapping("/{keycloakId}")
    public ResponseEntity<UserProfile> getUser(@PathVariable String keycloakId) {
        return ResponseEntity.ok(userService.getUser(keycloakId));
    }

    /**
     * Retourne le résumé complet par Keycloak ID.
     * Usage inter-services (trip-management, billing…).
     */
    @GetMapping("/summary/{keycloakId}")
    public ResponseEntity<UserMobilitySummaryDTO> getSummary(@PathVariable String keycloakId) {
        return ResponseEntity.ok(userService.getSummary(keycloakId));
    }

    /**
     * Crée manuellement un profil depuis un DTO.
     * Usage : backoffice / administration uniquement.
     */
    @PostMapping("/profile")
    public ResponseEntity<UserProfile> createProfile(@RequestBody UserProfileDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createProfile(dto));
    }
}
