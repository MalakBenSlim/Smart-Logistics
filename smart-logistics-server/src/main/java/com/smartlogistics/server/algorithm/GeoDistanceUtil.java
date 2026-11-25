package com.smartlogistics.server.algorithm;

/**
 * Calculs géographiques (formule de Haversine) et estimations coût/délai.
 */
public final class GeoDistanceUtil {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistanceUtil() {}

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }

    public static double estimateDeliveryCost(double distanceKm) {
        // Tarif de base 5 DT + 0.45 DT / km (plafonné pour rester réaliste sur le territoire)
        double raw = 5.0 + distanceKm * 0.45;
        return Math.round(raw * 100.0) / 100.0;
    }

    public static int estimatePreparationMinutes(double loadPercent) {
        // 12 min de base + jusqu'à 25 min selon la charge du dépôt
        return (int) Math.round(12.0 + (loadPercent / 100.0) * 25.0);
    }

    public static int estimateTransitMinutes(double distanceKm, double avgSpeedKmh) {
        if (avgSpeedKmh <= 0) avgSpeedKmh = 40.0;
        return Math.max(5, (int) Math.ceil((distanceKm / avgSpeedKmh) * 60.0));
    }
}
