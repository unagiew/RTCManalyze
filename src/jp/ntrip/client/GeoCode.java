package jp.ntrip.client;

import java.util.List;

public class GeoCode {
    private static final double a = 6378137; // radius
    private static final double e = 8.1819190842622e-2;  // eccentricity

    private static final double asq = Math.pow(a,2);
    private static final double esq = Math.pow(e,2);

    public static final int GNSSGPS = 0;
    public static final int GNSSGLONASS = 1;
    public static final int GNSSGALILEO = 2;
    public static final int GNSSSBAS = 3;
    public static final int GNSSQZSS = 4;
    public static final int GNSSBEIDOU = 5;

    public static double[] ecef2lla(double[] ecef){
        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        double b = Math.sqrt( asq * (1-esq) );
        double bsq = Math.pow(b,2);
        double ep = Math.sqrt( (asq - bsq)/bsq);
        double p = Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
        double th = Math.atan2(a*z, b*p);

        double lon = Math.atan2(y,x);
        double lat = Math.atan2( (z + Math.pow(ep,2)*b*Math.pow(Math.sin(th),3) ), (p - esq*a*Math.pow(Math.cos(th),3)) );
        double N = a/( Math.sqrt(1-esq*Math.pow(Math.sin(lat),2)) );
        double alt = p / Math.cos(lat) - N;

        // mod lat to 0-2pi
        lon = lon % (2*Math.PI);

        // correction for altitude near poles left out.

       return new double[]{Math.toDegrees(lat), Math.toDegrees(lon), alt};
    }

    public static double[] lla2ecef(double[] lla){
        double lat = lla[0];
        double lon = lla[1];
        double alt = lla[2];

        double N = a / Math.sqrt(1 - esq * Math.pow(Math.sin(lat),2) );

        double x = (N+alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N+alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1-esq) * N + alt) * Math.sin(lat);

        return new double[]{x, y, z};
    }

    private static final double GRS80_A = 6378137.000;
    private static final double GRS80_E2 = 0.00669438002301188;

    private static double deg2rad(double deg){
        return deg * Math.PI / 180.0;
    }

    public static double calcDistance(double lat1, double lng1, double lat2, double lng2){
        double my = deg2rad((lat1 + lat2) / 2.0);
        double dy = deg2rad(lat1 - lat2);
        double dx = deg2rad(lng1 - lng2);

        double sinMy = Math.sin(my);
        double w = Math.sqrt(1.0 - GRS80_E2 * sinMy * sinMy);
        double n = GRS80_A / w;

        double mnum = GRS80_A * (1 - GRS80_E2);
        double m = mnum / (w * w * w);

        double dym = dy * m;
        double dxncos = dx * n * Math.cos(my);
        return Math.sqrt(dym * dym + dxncos * dxncos);
    }

    public static double degToDms(double srcdeg) {
        int degree = (int)srcdeg;
        double minsec = srcdeg - degree;
        int minute = (int)(minsec * 60);
        double second = (minsec - minute) * 60;

        return (degree * 100) + minute + (second * 0.001);
    }

    public static String print2DimensionTable(List<String> columns, List<String> rows, List<Integer> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        for(String column : columns) {
            sb.append("  ").append(column).append("\t");
        }
        sb.append("\r\n--------");

        sb.append("+-------".repeat(columns.size()));
        sb.append("\r\n");

        int rowCounter = 0;
        for(String row : rows) {
            sb.append(row).append("\t");
            for(int i = rowCounter++; i < data.size(); i += rows.size()) {
                sb.append("| ").append(data.get(i)).append("\t");
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    public static String getGnssName(int gnssType) {
        String result;

        switch(gnssType) {
            case GNSSGPS:
                result = "GPS";
                break;
            case GNSSGLONASS:
                result = "GLONASS";
                break;
            case GNSSGALILEO:
                result = "GALILEO";
                break;
            case GNSSSBAS:
                result = "SBAS";
                break;
            case GNSSQZSS:
                result = "QZSS";
                break;
            case GNSSBEIDOU:
                result = "BEIDOU";
                break;
            default:
                result = "UNKNOWN";
        }
        return result;
    }
}
