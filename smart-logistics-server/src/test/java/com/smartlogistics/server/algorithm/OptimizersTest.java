package com.smartlogistics.server.algorithm;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.enums.WarehouseStatus;
import com.smartlogistics.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptimizersTest {

    @Test
    void selectsFallbackWarehouseWhenNearestHasStockOut() throws Exception {
        WarehouseDTO tunis = warehouse(1L, "Dépôt Tunis", 36.8470, 10.2060, 500, 300);
        WarehouseDTO sousse = warehouse(2L, "Dépôt Sousse", 35.8020, 10.6420, 450, 150);
        WarehouseDTO sfax = warehouse(3L, "Dépôt Sfax", 34.7320, 10.7450, 600, 200);

        // Tunis manque de produit #2 (Souris = 0), alors que Sousse et Sfax ont tout
        List<StockItemDTO> stocks = List.of(
                stock(1L, 1L, 10), stock(1L, 2L, 0),
                stock(2L, 1L, 12), stock(2L, 2L, 15),
                stock(3L, 1L, 18), stock(3L, 2L, 30)
        );

        DriverDTO d = driver(1L, "Mehdi", 35.82, 10.63, DriverStatus.DISPONIBLE, 0);

        WarehouseOptimizer optimizer = new WarehouseOptimizer();
        SmartDecisionDTO decision = optimizer.selectBestWarehouse(
                List.of(tunis, sousse, sfax),
                stocks,
                List.of(d),
                List.of(new OrderItemRequestDTO(1L, 1), new OrderItemRequestDTO(2L, 1)),
                36.8435, 10.2682 // Client à Tunis
        );

        // Sousse est plus proche de Tunis que Sfax et possède tout le stock
        assertEquals(2L, decision.getSelectedWarehouseId());
        assertEquals("Dépôt Sousse", decision.getSelectedWarehouseName());
        assertFalse(decision.getWarehouseCandidates().get(2).isEligible());
    }

    @Test
    void skipsBusyDriverAndSelectsBestAvailable() throws Exception {
        WarehouseDTO sfax = warehouse(1L, "Dépôt Sfax", 34.7320, 10.7450, 600, 250);
        DriverDTO busy = driver(1L, "Bilel", 34.7330, 10.7460, DriverStatus.EN_LIVRAISON, 2);
        DriverDTO available = driver(2L, "Ahmed", 34.7450, 10.7600, DriverStatus.DISPONIBLE, 0);

        DriverOptimizer optimizer = new DriverOptimizer();
        SmartDecisionDTO decision = optimizer.selectBestDriver(sfax, List.of(busy, available), new SmartDecisionDTO());

        assertEquals(2L, decision.getSelectedDriverId());
        assertEquals("Ahmed", decision.getSelectedDriverName());
        assertTrue(decision.getDriverScore() > 80.0);
    }

    @Test
    void throwsWhenNoDriverAvailable() {
        WarehouseDTO sfax = warehouse(1L, "Dépôt Sfax", 34.7320, 10.7450, 600, 250);
        DriverDTO offline = driver(1L, "Walid", 34.7330, 10.7460, DriverStatus.HORS_SERVICE, 0);

        DriverOptimizer optimizer = new DriverOptimizer();
        assertThrows(BusinessException.class,
                () -> optimizer.selectBestDriver(sfax, List.of(offline), null));
    }

    private WarehouseDTO warehouse(Long id, String name, double lat, double lng, int max, int load) {
        WarehouseDTO w = new WarehouseDTO();
        w.setId(id);
        w.setName(name);
        w.setLatitude(lat);
        w.setLongitude(lng);
        w.setMaxCapacity(max);
        w.setCurrentLoad(load);
        w.setStatus(WarehouseStatus.ACTIF);
        return w;
    }

    private StockItemDTO stock(Long whId, Long prodId, int qty) {
        StockItemDTO s = new StockItemDTO();
        s.setWarehouseId(whId);
        s.setProductId(prodId);
        s.setQuantity(qty);
        s.setReservedQuantity(0);
        return s;
    }

    private DriverDTO driver(Long id, String name, double lat, double lng, DriverStatus status, int active) {
        DriverDTO d = new DriverDTO();
        d.setId(id);
        d.setFullName(name);
        d.setVehicleType("Utilitaire");
        d.setCurrentLat(lat);
        d.setCurrentLng(lng);
        d.setStatus(status);
        d.setActiveDeliveries(active);
        return d;
    }
}
