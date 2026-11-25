package com.smartlogistics.socket.protocol;

public enum TcpMessageType {
    AUTH,
    POSITION_UPDATE,
    STATUS_UPDATE,
    DELIVERY_STARTED,
    DELIVERY_COMPLETED,
    HEARTBEAT,
    ACK,
    ERROR
}
