package com.smartlogistics.web.config;

import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.exception.ServiceUnavailableException;
import com.smartlogistics.common.jndi.ServiceLocator;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Exécute un appel RMI localisé via JNDI avec réessai automatique
 * si le stub mis en cache est devenu obsolète.
 */
public final class RmiGatewayHelper {
    private RmiGatewayHelper() {}

    @FunctionalInterface
    public interface RmiCall<S extends Remote, R> {
        R execute(S service) throws RemoteException, BusinessException;
    }

    public static <S extends Remote, R> R call(String jndiName, Class<S> serviceClass, RmiCall<S, R> call)
            throws BusinessException {
        try {
            S stub = ServiceLocator.getRmiService(jndiName, serviceClass);
            return call.execute(stub);
        } catch (RemoteException firstEx) {
            // Invalider le cache JNDI et tenter une reconnexion unique
            ServiceLocator.invalidateCache(jndiName);
            try {
                S freshStub = ServiceLocator.lookupFreshRmiService(jndiName, serviceClass);
                return call.execute(freshStub);
            } catch (RemoteException secondEx) {
                throw new ServiceUnavailableException(jndiName, secondEx);
            }
        }
    }
}
