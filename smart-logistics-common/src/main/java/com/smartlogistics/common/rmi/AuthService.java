package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.AuthResultDTO;
import com.smartlogistics.common.dto.RegisterRequestDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.BusinessException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant d'authentification, gestion des comptes et contrôle des rôles.
 */
public interface AuthService extends Remote {
    AuthResultDTO login(String email, String rawPassword, String clientIp) throws RemoteException, BusinessException;

    UserDTO registerClient(RegisterRequestDTO request) throws RemoteException, BusinessException;

    String requestPasswordReset(String email) throws RemoteException, BusinessException;

    boolean resetPassword(String resetToken, String newPassword) throws RemoteException, BusinessException;

    UserDTO getUserById(Long userId) throws RemoteException, BusinessException;

    List<UserDTO> getAllUsers() throws RemoteException;

    UserDTO updateUserRoleAndStatus(Long userId, Role role, boolean active) throws RemoteException, BusinessException;
}
