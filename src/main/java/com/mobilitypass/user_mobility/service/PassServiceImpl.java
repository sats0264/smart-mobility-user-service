package com.mobilitypass.user_mobility.service;

import com.mobilitypass.user_mobility.beans.MobilityPass;
import com.mobilitypass.user_mobility.enums.PassStatus;
import com.mobilitypass.user_mobility.enums.PassType;
import com.mobilitypass.user_mobility.error.ResourceNotFoundException;
import com.mobilitypass.user_mobility.repository.MobilityPassRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implémentation du service MobilityPass.
 * Assure le suivi de la validité du pass et de la consommation journalière de
 * l'utilisateur.
 */
@Service
@Transactional
public class PassServiceImpl implements PassService {
    @Autowired
    private MobilityPassRepository passRepository;

    @Override
    public MobilityPass activatePass(String userId) {
        return passRepository.findByUserId(userId).orElseGet(() -> {
            MobilityPass newPass = new MobilityPass(userId, PassStatus.ACTIVE, PassType.STANDARD, 1200.0);
            return passRepository.save(newPass);
        });
    }

    @Override
    public void changePassStatus(String userId, PassStatus status) {
        MobilityPass pass = passRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pass non trouvé pour : " + userId));
        pass.setStatus(status);
        passRepository.save(pass);
    }

    @Override
    public MobilityPass getUserPass(String userId) {
        return passRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun pass trouvé pour cet utilisateur : " + userId));
    }

    @Override
    public void addSpentAmount(String userId, Double amount) {
        MobilityPass pass = passRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pass non trouvé"));
        pass.setTodaySpentAmount(pass.getTodaySpentAmount() + amount);
        passRepository.save(pass);
    }

    @Override
    public List<MobilityPass> getAllPasses() {
        return passRepository.findAll();
    }
}
