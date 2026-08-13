package parsers

import de.mickkc.vibin.utils.DateTimeUtils
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationTest {

    val secondsPerMonth = 2_629_746L
    val secondsPerYear = 31_556_952L

    // region Duration parsing

    // region Single units

    @Test
    fun testDurationParsing_SingleUnit_Seconds() {
        val input = "40s"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(40L, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Seconds_2() {
        val input = "40 Seconds" // -> 40s
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(40L, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Minutes() {
        val input = "5m"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(5.minutes.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Minutes_2() {
        val input = "5 minutes" // -> 5m (no uppercase M, that would be months!)
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(5.minutes.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Hours() {
        val input = "12h"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(12.hours.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Hours_2() {
        val input = "12 Hours" // -> 12h
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(12.hours.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Days() {
        val input = "6d"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(6.days.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Days_2() {
        val input = "6 Days" // -> 6d
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(6.days.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Weeks() {
        val input = "5w"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(35.days.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Weeks_2() {
        val input = "5 Weeks" // -> 5w
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(35.days.inWholeSeconds, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Months() {
        val input = "3M"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(3 * secondsPerMonth, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Months_2() {
        val input = "3 Months" // -> 3M (no uppercase m, that would be minutes!)
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(3 * secondsPerMonth, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Years() {
        val input = "20y"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(20 * secondsPerYear, output)
    }

    @Test
    fun testDurationParsing_SingleUnit_Years_2() {
        val input = "20 Years"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(20 * secondsPerYear, output)
    }

    // endregion

    // region Multiple units

    @Test
    fun testDurationParsing_MultipleUnits_1() {
        val input = "3d 20h 5m 10s"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(
            (3.days + 20.hours + 5.minutes + 10.seconds).inWholeSeconds,
            output
        )
    }

    @Test
    fun testDurationParsing_MultipleUnits_2() {
        val input = "2 Years 3 Weeks 20 Hours"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(
            (2 * secondsPerYear) + (21.days + 20.hours).inWholeSeconds,
            output
        )
    }

    // endregion

    // region Invalid duration

    @Test
    fun testDurationParsing_Invalid_Empty() {
        val input =  ""
        val output = DateTimeUtils.parseDuration(input)
        assertNull(output)
    }

    @Test
    fun testDurationParsing_Invalid_UnknownUnit_1() {
        val input =  "3p"
        val output = DateTimeUtils.parseDuration(input)
        assertNull(output)
    }

    @Test
    fun testDurationParsing_Invalid_UnknownUnit_2() {
        val input =  "3h 33p"
        val output = DateTimeUtils.parseDuration(input)
        assertNull(output)
    }

    // endregion

    // region Same unit multiple times

    @Test
    fun testDurationParsing_SameUnit_1() {
        val input = "3h 4h 5h"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(
            (3.hours + 4.hours + 5.hours).inWholeSeconds,
            output
        )
    }

    @Test
    fun testDurationParsing_SameUnit_2() {
        val input = "3h 5m 2h"
        val output = DateTimeUtils.parseDuration(input)
        assertEquals(
            (3.hours + 5.minutes + 2.hours).inWholeSeconds,
            output
        )
    }

    // endregion
    // endregion

    // region Duration formatting

    // region Single unit

    @Test
    fun testDurationFormatting_SingleUnit_Seconds() {
        val input = 3L
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("3s", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Minutes() {
        val input = 5.minutes.inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("5m", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Hours() {
        val input = 12.hours.inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("12h", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Days() {
        val input = 6.days.inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("6d", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Weeks() {
        val input = 14.days.inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("2w", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Months() {
        val input = 4 * secondsPerMonth
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("4M", output)
    }

    @Test
    fun testDurationFormatting_SingleUnit_Years() {
        val input = 3 * secondsPerYear
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("3y", output)
    }

    // endregion

    // region Multiple units

    @Test
    fun testDurationFormatting_MultipleUnits_1() {
        val input = (3.hours + 40.minutes + 10.seconds).inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("3h 40m 10s", output)
    }

    @Test
    fun testDurationFormatting_MultipleUnits_2() {
        val input = (10 * secondsPerYear) + (24.days).inWholeSeconds
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("10y 3w 3d", output)
    }

    // endregion

    // region Empty

    @Test
    fun testDurationFormatting_Empty() {
        val input = 0L
        val output = DateTimeUtils.formatDurationFull(input)
        assertEquals("0s", output)
    }

    // endregion

    // region Negative

    @Test
    fun testDurationFormatting_Negative() {
        assertThrows<IllegalArgumentException> { DateTimeUtils.formatDurationFull(-1L) }
        assertThrows<IllegalArgumentException> { DateTimeUtils.formatDurationFull(-60L) }
        assertThrows<IllegalArgumentException> { DateTimeUtils.formatDurationFull(-360L) }
    }

    // endregion
    // endregion
}