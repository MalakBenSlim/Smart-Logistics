package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.AuthResultDTO;
import com.smartlogistics.common.dto.RegisterRequestDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.AuthException;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.rmi.AuthService;
import com.smartlogistics.common.security.InputValidator;
import com.smartlogistics.common.security.PasswordUtil;
import com.smartlogistics.database.dao.UserDao;
import com.smartlogistics.server.security.LoginAttemptLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserDao userDao = new UserDao();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    public AuthServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public AuthResultDTO login(String email, String rawPassword, String clientIp)
            throws RemoteException, BusinessException {
        String normalizedEmail = InputValidator.requireValidEmail(email);
        InputValidator.requireNonBlank(rawPassword, "mot de passe");

        String limitKey = normalizedEmail + "|" + (clientIp != null ? clientIp : "local");
        if (limiter.isBlocked(limitKey)) {
            long sec = limiter.getRemainingLockSeconds(limitKey);
            throw new AuthException("ACCOUNT_LOCKED",
                    "Trop de tentatives échouées. Réessayez dans " + sec + " secondes.");
        }

        try {
            Optional<UserDao.UserAuthRecord> opt = userDao.findAuthByEmail(normalizedEmail);
            if (opt.isEmpty() || !PasswordUtil.verify(rawPassword, opt.get().getPasswordHash())) {
                limiter.recordFailure(limitKey);
                int remaining = limiter.getRemainingAttempts(limitKey);
                throw new AuthException("INVALID_CREDENTIALS",
                        "Email ou mot de passe incorrect. Tentatives restantes : " + remaining);
            }

            UserDTO user = opt.get().getUser();
            if (!user.isActive()) {
                throw new AuthException("ACCOUNT_DISABLED", "Ce compte est désactivé. Contactez l'administrateur.");
            }

            limiter.recordSuccess(limitKey);
            String token = UUID.randomUUID().toString();
            log.info("Connexion réussie via RMI pour {} ({})", user.getEmail(), user.getRole());
            return new AuthResultDTO(true, "Authentification réussie", user, token);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur base de données lors de l'authentification", ex);
        }
    }

    @Override
    public UserDTO registerClient(RegisterRequestDTO req) throws RemoteException, BusinessException {
        if (req == null) {
            throw new BusinessException("VALIDATION_ERROR", "Données d'inscription manquantes.");
        }
        String fullName = InputValidator.requireNonBlank(req.getFullName(), "nom complet");
        String email = InputValidator.requireValidEmail(req.getEmail());
        InputValidator.requireStrongPassword(req.getRawPassword());
        String phone = InputValidator.requireValidPhone(req.getPhone());
        String address = InputValidator.requireNonBlank(req.getAddress(), "adresse");
        InputValidator.requireValidCoordinates(req.getLatitude(), req.getLongitude());

        try {
            if (userDao.findAuthByEmail(email).isPresent()) {
                throw new BusinessException("EMAIL_EXISTS", "Un compte existe déjà avec cet email.");
            }
            UserDTO user = new UserDTO();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setAddress(address);
            user.setLatitude(req.getLatitude());
            user.setLongitude(req.getLongitude());
            user.setRole(Role.CLIENT);
            user.setActive(true);

            UserDTO created = userDao.create(user, PasswordUtil.hash(req.getRawPassword()));
            log.info("Nouveau client inscrit via RMI : {}", created.getEmail());
            return created;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur base de données lors de l'inscription", ex);
        }
    }

    @Override
    public String requestPasswordReset(String email) throws RemoteException, BusinessException {
        String normalizedEmail = InputValidator.requireValidEmail(email);
        try {
            Optional<UserDao.UserAuthRecord> opt = userDao.findAuthByEmail(normalizedEmail);
            if (opt.isEmpty()) {
                throw new BusinessException("USER_NOT_FOUND", "Aucun compte associé à cet email.");
            }
            String resetToken = "RST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            userDao.saveResetToken(opt.get().getUser().getId(), resetToken, LocalDateTime.now().plusMinutes(15));
            log.info("Jeton de réinitialisation généré pour {}", normalizedEmail);
            return resetToken;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur génération jeton réinitialisation", ex);
        }
    }

    @Override
    public boolean resetPassword(String resetToken, String newPassword) throws RemoteException, BusinessException {
        String token = InputValidator.requireNonBlank(resetToken, "jeton de réinitialisation");
        InputValidator.requireStrongPassword(newPassword);
        try {
            boolean ok = userDao.consumeResetTokenAndUpdatePassword(token, PasswordUtil.hash(newPassword));
            if (!ok) {
                throw new BusinessException("INVALID_TOKEN", "Jeton invalide, expiré ou déjà utilisé.");
            }
            return true;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lors de la réinitialisation du mot de passe", ex);
        }
    }

    @Override
    public UserDTO getUserById(Long userId) throws RemoteException, BusinessException {
        try {
            return userDao.findById(userId)
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable."));
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture utilisateur", ex);
        }
    }

    @Override
    public List<UserDTO> getAllUsers() throws RemoteException {
        try {
            return userDao.findAll();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture utilisateurs", ex);
        }
    }

    @Override
    public UserDTO updateUserRoleAndStatus(Long userId, Role role, boolean active)
            throws RemoteException, BusinessException {
        if (userId == null || role == null) {
            throw new BusinessException("VALIDATION_ERROR", "userId et role requis.");
        }
        try {
            UserDTO updated = userDao.updateRoleAndStatus(userId, role, active);
            if (updated == null) {
                throw new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur mise à jour utilisateur", ex);
        }
    }
}
