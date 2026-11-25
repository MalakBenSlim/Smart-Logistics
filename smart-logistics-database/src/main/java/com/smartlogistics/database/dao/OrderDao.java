package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemDTO;
import com.smartlogistics.common.dto.PaymentDTO;
import com.smartlogistics.common.enums.OrderStatus;
import com.smartlogistics.common.enums.PaymentMethod;
import com.smartlogistics.common.enums.PaymentStatus;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDao {
    private final DeliveryDao deliveryDao = new DeliveryDao();
    private final SmartDecisionDao smartDecisionDao = new SmartDecisionDao();

    private static final String BASE_SELECT = """
            SELECT o.id, o.order_code, o.client_id, u.full_name AS client_name,
                   o.warehouse_id, w.name AS warehouse_name, o.status,
                   o.total_amount, o.delivery_cost, o.delivery_address,
                   o.delivery_lat, o.delivery_lng, o.estimated_minutes, o.created_at
            FROM orders o
            JOIN users u ON u.id = o.client_id
            LEFT JOIN warehouses w ON w.id = o.warehouse_id
            """;

    public int countOrders() throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public OrderDTO createOrderWithItemsAndPayment(OrderDTO order, List<OrderItemDTO> items, PaymentDTO payment)
            throws SQLException {
        Long createdOrderId = JdbcHelper.inTransaction(conn -> {
            String orderSql = """
                    INSERT INTO orders (order_code, client_id, warehouse_id, status, total_amount,
                                        delivery_cost, delivery_address, delivery_lat, delivery_lng, estimated_minutes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            long orderId;
            try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setLong(2, order.getClientId());
                if (order.getWarehouseId() != null) {
                    ps.setLong(3, order.getWarehouseId());
                } else {
                    ps.setNull(3, Types.BIGINT);
                }
                ps.setString(4, order.getStatus().name());
                ps.setDouble(5, order.getTotalAmount());
                ps.setDouble(6, order.getDeliveryCost());
                ps.setString(7, order.getDeliveryAddress());
                ps.setDouble(8, order.getDeliveryLat());
                ps.setDouble(9, order.getDeliveryLng());
                ps.setInt(10, order.getEstimatedMinutes());
                ps.executeUpdate();
                orderId = JdbcHelper.extractGeneratedId(ps);
            }

            String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (OrderItemDTO item : items) {
                    ps.setLong(1, orderId);
                    ps.setLong(2, item.getProductId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            String paySql = """
                    INSERT INTO payments (order_id, method, status, amount, transaction_ref, processed_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(paySql)) {
                ps.setLong(1, orderId);
                ps.setString(2, payment.getMethod().name());
                ps.setString(3, payment.getStatus().name());
                ps.setDouble(4, payment.getAmount());
                ps.setString(5, payment.getTransactionRef());
                ps.executeUpdate();
            }
            return orderId;
        });

        return findById(createdOrderId).orElseThrow();
    }

    public Optional<OrderDTO> findById(Long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE o.id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                OrderDTO order = mapOrder(rs);
                enrichOrder(order);
                return Optional.of(order);
            }
        }
    }

    public List<OrderDTO> findByClient(Long clientId) throws SQLException {
        return queryOrders(BASE_SELECT + " WHERE o.client_id = ? ORDER BY o.created_at DESC, o.id DESC", clientId);
    }

    public List<OrderDTO> findByWarehouse(Long warehouseId) throws SQLException {
        return queryOrders(BASE_SELECT + " WHERE o.warehouse_id = ? ORDER BY o.created_at DESC, o.id DESC", warehouseId);
    }

    public List<OrderDTO> findAll() throws SQLException {
        return queryOrders(BASE_SELECT + " ORDER BY o.created_at DESC, o.id DESC", null);
    }

    public void updateStatus(Long orderId, OrderStatus newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    public void markPaymentAccepted(Long orderId) throws SQLException {
        String sql = "UPDATE payments SET status = 'ACCEPTE', processed_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status = 'EN_ATTENTE'";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        }
    }

    private List<OrderDTO> queryOrders(String sql, Long param) throws SQLException {
        List<OrderDTO> orders = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setLong(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        }
        for (OrderDTO order : orders) {
            enrichOrder(order);
        }
        return orders;
    }

    private void enrichOrder(OrderDTO order) throws SQLException {
        order.setItems(findItemsByOrderId(order.getId()));
        order.setPayment(findPaymentByOrderId(order.getId()).orElse(null));
        order.setDelivery(deliveryDao.findByOrderId(order.getId()).orElse(null));
        order.setSmartDecision(smartDecisionDao.findByOrderId(order.getId()).orElse(null));
    }

    private List<OrderItemDTO> findItemsByOrderId(Long orderId) throws SQLException {
        String sql = """
                SELECT oi.id, oi.order_id, oi.product_id, p.name AS product_name, p.sku AS product_sku,
                       oi.quantity, oi.unit_price
                FROM order_items oi
                JOIN products p ON p.id = oi.product_id
                WHERE oi.order_id = ?
                """;
        List<OrderItemDTO> items = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItemDTO item = new OrderItemDTO();
                    item.setId(rs.getLong("id"));
                    item.setOrderId(rs.getLong("order_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductSku(rs.getString("product_sku"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Optional<PaymentDTO> findPaymentByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT id, order_id, method, status, amount, transaction_ref, processed_at FROM payments WHERE order_id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                PaymentDTO p = new PaymentDTO();
                p.setId(rs.getLong("id"));
                p.setOrderId(rs.getLong("order_id"));
                p.setMethod(PaymentMethod.valueOf(rs.getString("method")));
                p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
                p.setAmount(rs.getDouble("amount"));
                p.setTransactionRef(rs.getString("transaction_ref"));
                p.setProcessedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("processed_at")));
                return Optional.of(p);
            }
        }
    }

    private OrderDTO mapOrder(ResultSet rs) throws SQLException {
        OrderDTO o = new OrderDTO();
        o.setId(rs.getLong("id"));
        o.setOrderCode(rs.getString("order_code"));
        o.setClientId(rs.getLong("client_id"));
        o.setClientName(rs.getString("client_name"));
        long wid = rs.getLong("warehouse_id");
        o.setWarehouseId(rs.wasNull() ? null : wid);
        o.setWarehouseName(rs.getString("warehouse_name"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setDeliveryCost(rs.getDouble("delivery_cost"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setDeliveryLat(rs.getDouble("delivery_lat"));
        o.setDeliveryLng(rs.getDouble("delivery_lng"));
        o.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        o.setCreatedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("created_at")));
        return o;
    }
}
