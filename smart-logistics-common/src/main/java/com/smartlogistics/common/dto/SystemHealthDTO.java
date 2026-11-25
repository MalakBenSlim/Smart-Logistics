package com.smartlogistics.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SystemHealthDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean rmiRegistryOnline;
    private boolean jmsBrokerOnline;
    private boolean tcpSocketOnline;
    private boolean databaseOnline;
    private int activeDriverSockets;
    private Map<String, Boolean> serviceStatuses = new LinkedHashMap<>();
    private LocalDateTime checkedAt;

    public SystemHealthDTO() {}

    public boolean isRmiRegistryOnline() { return rmiRegistryOnline; }
    public void setRmiRegistryOnline(boolean rmiRegistryOnline) { this.rmiRegistryOnline = rmiRegistryOnline; }
    public boolean isJmsBrokerOnline() { return jmsBrokerOnline; }
    public void setJmsBrokerOnline(boolean jmsBrokerOnline) { this.jmsBrokerOnline = jmsBrokerOnline; }
    public boolean isTcpSocketOnline() { return tcpSocketOnline; }
    public void setTcpSocketOnline(boolean tcpSocketOnline) { this.tcpSocketOnline = tcpSocketOnline; }
    public boolean isDatabaseOnline() { return databaseOnline; }
    public void setDatabaseOnline(boolean databaseOnline) { this.databaseOnline = databaseOnline; }
    public int getActiveDriverSockets() { return activeDriverSockets; }
    public void setActiveDriverSockets(int activeDriverSockets) { this.activeDriverSockets = activeDriverSockets; }
    public Map<String, Boolean> getServiceStatuses() { return serviceStatuses; }
    public void setServiceStatuses(Map<String, Boolean> serviceStatuses) { this.serviceStatuses = serviceStatuses; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
