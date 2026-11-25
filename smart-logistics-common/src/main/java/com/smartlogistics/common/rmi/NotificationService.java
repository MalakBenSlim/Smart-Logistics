package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.dto.SystemHealthDTO;
import com.smartlogistics.common.enums.Role;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant pour la consultation des notifications et la supervision du cluster.
 */
public interface NotificationService extends Remote {
    List<NotificationDTO> getNotificationsForUser(Long userId, Role role) throws RemoteException;

    void markAsRead(Long notificationId) throws RemoteException;

    SystemHealthDTO getSystemHealth() throws RemoteException;
}
