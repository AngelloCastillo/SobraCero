package pe.upc.sobracero.comun;

public final class Geo {
    private Geo() {}

    /** Distancia en km entre dos coordenadas (fórmula de Haversine). */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}
