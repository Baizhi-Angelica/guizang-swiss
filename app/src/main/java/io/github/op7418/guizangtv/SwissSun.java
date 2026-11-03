package io.github.op7418.guizangtv;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Sunrise and sunset from the NOAA solar equations. Pure math so the day/night
 * switch can be unit tested; the home only needs "is it light outside".
 *
 * The default location is Shanghai's city centre. Change LAT/LON (or pass a
 * different pair to {@link #event}) for another city.
 */
final class SwissSun {
    static final double LATITUDE = 31.2304;
    static final double LONGITUDE = 121.4737;

    /** {sunrise, sunset} in local minutes after midnight, or null near the poles. */
    static double[] event(double latitude, double longitude, int dayOfYear, int zoneOffsetMinutes) {
        double gamma = 2 * Math.PI / 365.0 * (dayOfYear - 1);
        double eqTime = 229.18 * (0.000075
                + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));
        double declination = 0.006918
                - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma);

        double lat = Math.toRadians(latitude);
        double cosHourAngle = Math.cos(Math.toRadians(90.833)) / (Math.cos(lat) * Math.cos(declination))
                - Math.tan(lat) * Math.tan(declination);
        if (cosHourAngle > 1 || cosHourAngle < -1) return null;
        double hourAngle = Math.toDegrees(Math.acos(cosHourAngle));

        double sunriseUtc = 720 - 4 * (longitude + hourAngle) - eqTime;
        double sunsetUtc = 720 - 4 * (longitude - hourAngle) - eqTime;
        return new double[]{sunriseUtc + zoneOffsetMinutes, sunsetUtc + zoneOffsetMinutes};
    }

    /** True between sunrise and sunset at the configured location. */
    static boolean daylight(long millis) {
        return daylight(millis, LATITUDE, LONGITUDE, TimeZone.getDefault());
    }

    static boolean daylight(long millis, double latitude, double longitude, TimeZone zone) {
        Calendar local = Calendar.getInstance(zone, Locale.CHINA);
        local.setTimeInMillis(millis);
        int day = local.get(Calendar.DAY_OF_YEAR);
        int minutes = local.get(Calendar.HOUR_OF_DAY) * 60 + local.get(Calendar.MINUTE);
        double[] event = event(latitude, longitude, day, zone.getOffset(millis) / 60000);
        if (event == null) return minutes >= 7 * 60 && minutes < 19 * 60;
        return minutes >= event[0] && minutes < event[1];
    }

    private SwissSun() { }
}
