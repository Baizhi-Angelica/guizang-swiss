package io.github.op7418.guizangtv;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.TimeZone;
import org.junit.Test;

/** Sunrise/sunset maths for the automatic day and night palettes. */
public final class SwissSunTest {
    private static final TimeZone SHANGHAI = TimeZone.getTimeZone("Asia/Shanghai");

    @Test public void shanghaiEarlyNovemberSunriseAndSunset() {
        // 3 November 2026 is day 307 of the year.
        double[] event = SwissSun.event(SwissSun.LATITUDE, SwissSun.LONGITUDE, 307, 8 * 60);
        assertNotNull(event);
        double sunrise = event[0];
        double sunset = event[1];
        assertTrue("sunrise " + sunrise, sunrise > 6 * 60 && sunrise < 6 * 60 + 40);
        assertTrue("sunset " + sunset, sunset > 16 * 60 + 45 && sunset < 17 * 60 + 20);
    }

    @Test public void summerDaysAreLongerThanWinterDays() {
        double[] summer = SwissSun.event(SwissSun.LATITUDE, SwissSun.LONGITUDE, 172, 8 * 60);
        double[] winter = SwissSun.event(SwissSun.LATITUDE, SwissSun.LONGITUDE, 355, 8 * 60);
        assertNotNull(summer);
        assertNotNull(winter);
        assertTrue((summer[1] - summer[0]) > (winter[1] - winter[0]) + 120);
    }

    @Test public void polarNightHasNoEvents() {
        assertNull(SwissSun.event(78.2, 15.6, 355, 60));
    }

    @Test public void daylightFollowsTheEventTimes() {
        java.util.Calendar day = java.util.Calendar.getInstance(SHANGHAI);
        day.set(2026, java.util.Calendar.NOVEMBER, 3, 12, 0, 0);
        day.set(java.util.Calendar.MILLISECOND, 0);
        assertTrue(SwissSun.daylight(day.getTimeInMillis(), SwissSun.LATITUDE, SwissSun.LONGITUDE, SHANGHAI));
        day.set(java.util.Calendar.HOUR_OF_DAY, 3);
        assertTrue(!SwissSun.daylight(day.getTimeInMillis(), SwissSun.LATITUDE, SwissSun.LONGITUDE, SHANGHAI));
    }
}
