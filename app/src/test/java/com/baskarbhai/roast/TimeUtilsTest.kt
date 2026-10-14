package com.baskarbhai.roast

import com.baskarbhai.roast.data.TierConfig
import com.baskarbhai.roast.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Time formatting and Tier configuration.
 * Engineered by mdx.
 */
class TimeUtilsTest {

    @Test
    fun testFormatDurationMmSs() {
        assertEquals("00:00", TimeUtils.formatDurationMmSs(0L))
        assertEquals("04:15", TimeUtils.formatDurationMmSs(255L))
        assertEquals("10:00", TimeUtils.formatDurationMmSs(600L))
        assertEquals("30:45", TimeUtils.formatDurationMmSs(1845L))
    }

    @Test
    fun testFormatHumanReadable() {
        assertEquals("45s", TimeUtils.formatHumanReadable(45L))
        assertEquals("5m", TimeUtils.formatHumanReadable(300L))
        assertEquals("1h 15m", TimeUtils.formatHumanReadable(4500L))
    }

    @Test
    fun testTierDefaultsIntegrity() {
        val tiers = TierConfig.getDefaultTiers()
        assertEquals(4, tiers.size)

        val t1 = tiers[0]
        assertEquals(1, t1.tierLevel)
        assertEquals(10, t1.defaultReelThreshold)
        assertEquals(5, t1.defaultMinuteThreshold)

        val t2 = tiers[1]
        assertEquals(2, t2.tierLevel)
        assertEquals(25, t2.defaultReelThreshold)
        assertEquals(12, t2.defaultMinuteThreshold)

        val t3 = tiers[2]
        assertEquals(3, t3.tierLevel)
        assertEquals(40, t3.defaultReelThreshold)
        assertEquals(20, t3.defaultMinuteThreshold)

        val t4 = tiers[3]
        assertEquals(4, t4.tierLevel)
        assertEquals(60, t4.defaultReelThreshold)
        assertEquals(30, t4.defaultMinuteThreshold)

        assertTrue(t1.caption.isNotEmpty())
        assertTrue(t4.memeUrl.isNotEmpty())
    }
}
