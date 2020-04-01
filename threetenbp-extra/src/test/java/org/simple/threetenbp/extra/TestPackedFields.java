/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.simple.threetenbp.extra;

import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.FOREVER;
import static org.threeten.bp.temporal.ChronoUnit.MINUTES;
import static org.threeten.bp.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.format.ResolverStyle;

import org.junit.Test;

/**
 * Test PackedFields.
 */
public class TestPackedFields {

    //-----------------------------------------------------------------------
    // packedDate()
    //-----------------------------------------------------------------------
    @Test
    public void test_date_basics() {
        assertEquals("PackedDate", PackedFields.PACKED_DATE.toString());
        assertEquals(DAYS, PackedFields.PACKED_DATE.getBaseUnit());
        assertEquals(FOREVER, PackedFields.PACKED_DATE.getRangeUnit());
        assertEquals(true, PackedFields.PACKED_DATE.isDateBased());
        assertEquals(false, PackedFields.PACKED_DATE.isTimeBased());
        assertEquals(true, PackedFields.PACKED_DATE.isSupportedBy(LocalDate.of(2015, 3, 12)));
        assertEquals(false, PackedFields.PACKED_DATE.isSupportedBy(LocalTime.of(11, 30)));
        assertEquals(10000101, PackedFields.PACKED_DATE.range().getMinimum());
        assertEquals(99991231, PackedFields.PACKED_DATE.range().getMaximum());
    }

    @Test(expected = DateTimeException.class)
    public void test_date_rangeRefinedBy_time() {
        PackedFields.PACKED_DATE.rangeRefinedBy(LocalTime.of(11, 30));
    }

    @Test
    public void test_date_getFrom() {
        assertEquals(20151203, LocalDate.of(2015, 12, 3).get(PackedFields.PACKED_DATE));
        assertEquals(10000101, LocalDate.of(1000, 1, 1).get(PackedFields.PACKED_DATE));
        assertEquals(99991231, LocalDate.of(9999, 12, 31).get(PackedFields.PACKED_DATE));
    }

    @Test(expected = DateTimeException.class)
    public void test_date_getFrom_rangeLow() {
        PackedFields.PACKED_DATE.getFrom(LocalDate.of(999, 12, 31));
    }

    @Test(expected = DateTimeException.class)
    public void test_date_getFrom_rangeHigh() {
        PackedFields.PACKED_DATE.getFrom(LocalDate.of(10000, 1, 1));
    }

    @Test
    public void test_date_adjustInto() {
        assertEquals(LocalDate.of(2015, 12, 3), LocalDate.MIN.with(PackedFields.PACKED_DATE, 20151203));
    }

    @Test(expected = DateTimeException.class)
    public void test_date_adjustInto_range() {
        LocalDate.MIN.with(PackedFields.PACKED_DATE, 1230101);
    }

    @Test
    public void test_date_resolve() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_DATE).toFormatter();
        assertEquals(LocalDate.of(2015, 12, 3), LocalDate.parse("20151203", f));
    }

    @Test(expected = DateTimeParseException.class)
    public void test_date_resolve_invalid_smart() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_DATE).toFormatter();
        LocalDate.parse("20151403", f.withResolverStyle(ResolverStyle.SMART));
    }

    @Test
    public void test_date_resolve_invalid_lenient() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_DATE).toFormatter();
        assertEquals(LocalDate.of(2016, 2, 3), LocalDate.parse("20151403", f.withResolverStyle(ResolverStyle.LENIENT)));
    }

    //-----------------------------------------------------------------------
    // packedHourMin()
    //-----------------------------------------------------------------------
    @Test
    public void test_hourMin_basics() {
        assertEquals("PackedHourMin", PackedFields.PACKED_HOUR_MIN.toString());
        assertEquals(MINUTES, PackedFields.PACKED_HOUR_MIN.getBaseUnit());
        assertEquals(DAYS, PackedFields.PACKED_HOUR_MIN.getRangeUnit());
        assertEquals(false, PackedFields.PACKED_HOUR_MIN.isDateBased());
        assertEquals(true, PackedFields.PACKED_HOUR_MIN.isTimeBased());
        assertEquals(true, PackedFields.PACKED_HOUR_MIN.isSupportedBy(LocalTime.of(11, 30)));
        assertEquals(false, PackedFields.PACKED_HOUR_MIN.isSupportedBy(LocalDate.of(2015, 3, 12)));
        assertEquals(0, PackedFields.PACKED_HOUR_MIN.range().getMinimum());
        assertEquals(2359, PackedFields.PACKED_HOUR_MIN.range().getMaximum());
    }

    @Test(expected = DateTimeException.class)
    public void test_hourMin_rangeRefinedBy_time() {
        PackedFields.PACKED_HOUR_MIN.rangeRefinedBy(LocalDate.of(2015, 12, 3));
    }

    @Test
    public void test_hourMin_getFrom() {
        assertEquals(1130, LocalTime.of(11, 30).get(PackedFields.PACKED_HOUR_MIN));
        assertEquals(121, LocalTime.of(1, 21).get(PackedFields.PACKED_HOUR_MIN));
    }

    @Test
    public void test_hourMin_adjustInto() {
        assertEquals(LocalTime.of(11, 30), LocalTime.MIDNIGHT.with(PackedFields.PACKED_HOUR_MIN, 1130));
    }

    @Test(expected = DateTimeException.class)
    public void test_hourMin_adjustInto_value() {
        LocalDate.MIN.with(PackedFields.PACKED_HOUR_MIN, 1273);
    }

    @Test
    public void test_hourMin_resolve() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_HOUR_MIN).toFormatter();
        assertEquals(LocalTime.of(11, 30), LocalTime.parse("1130", f));
    }

    @Test(expected = DateTimeParseException.class)
    public void test_hourMin_resolve_invalid_smart() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_HOUR_MIN).toFormatter();
        LocalTime.parse("1173", f.withResolverStyle(ResolverStyle.SMART));
    }

    @Test
    public void test_hourMin_resolve_invalid_lenient() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_HOUR_MIN).toFormatter();
        assertEquals(LocalTime.of(12, 13), LocalTime.parse("1173", f.withResolverStyle(ResolverStyle.LENIENT)));
    }

    //-----------------------------------------------------------------------
    // packedTime()
    //-----------------------------------------------------------------------
    @Test
    public void test_time_basics() {
        assertEquals("PackedTime", PackedFields.PACKED_TIME.toString());
        assertEquals(SECONDS, PackedFields.PACKED_TIME.getBaseUnit());
        assertEquals(DAYS, PackedFields.PACKED_TIME.getRangeUnit());
        assertEquals(false, PackedFields.PACKED_TIME.isDateBased());
        assertEquals(true, PackedFields.PACKED_TIME.isTimeBased());
        assertEquals(true, PackedFields.PACKED_TIME.isSupportedBy(LocalTime.of(11, 30)));
        assertEquals(false, PackedFields.PACKED_TIME.isSupportedBy(LocalDate.of(2015, 3, 12)));
        assertEquals(0, PackedFields.PACKED_TIME.range().getMinimum());
        assertEquals(235959, PackedFields.PACKED_TIME.range().getMaximum());
    }

    @Test(expected = DateTimeException.class)
    public void test_time_rangeRefinedBy_time() {
        PackedFields.PACKED_TIME.rangeRefinedBy(LocalDate.of(2015, 12, 3));
    }

    @Test
    public void test_time_getFrom() {
        assertEquals(113052, LocalTime.of(11, 30, 52).get(PackedFields.PACKED_TIME));
        assertEquals(113000, LocalTime.of(11, 30).get(PackedFields.PACKED_TIME));
        assertEquals(12100, LocalTime.of(1, 21).get(PackedFields.PACKED_TIME));
    }

    @Test
    public void test_time_adjustInto() {
        assertEquals(LocalTime.of(11, 30, 52), LocalTime.MIDNIGHT.with(PackedFields.PACKED_TIME, 113052));
    }

    @Test(expected = DateTimeException.class)
    public void test_time_adjustInto_value() {
        LocalDate.MIN.with(PackedFields.PACKED_TIME, 127310);
    }

    @Test
    public void test_time_resolve() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_TIME).toFormatter();
        assertEquals(LocalTime.of(11, 30, 52), LocalTime.parse("113052", f));
    }

    @Test(expected = DateTimeParseException.class)
    public void test_time_resolve_invalid_smart() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_TIME).toFormatter();
        LocalTime.parse("117361", f.withResolverStyle(ResolverStyle.SMART));
    }

    @Test
    public void test_time_resolve_invalid_lenient() {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(PackedFields.PACKED_TIME).toFormatter();
        assertEquals(LocalTime.of(12, 14, 1), LocalTime.parse("117361", f.withResolverStyle(ResolverStyle.LENIENT)));
    }

}
