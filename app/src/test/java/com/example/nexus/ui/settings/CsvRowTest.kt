package com.example.nexus.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** AUD-11: habit export follows RFC 4180 quoting. */
class CsvRowTest {
    @Test
    fun plainFieldsAreUnquoted() = assertEquals("Read,DAILY,2026-11-05", csvRow("Read", "DAILY", "2026-11-05"))

    @Test
    fun commasQuotesAndNewlinesAreQuoted() {
        assertEquals("\"Read, write\",DAILY,2026-11-05", csvRow("Read, write", "DAILY", "2026-11-05"))
        assertEquals("\"Say \"\"hi\"\"\",WEEKLY,2026-11-05", csvRow("Say \"hi\"", "WEEKLY", "2026-11-05"))
        assertEquals("\"Two\nlines\",DAILY,2026-11-05", csvRow("Two\nlines", "DAILY", "2026-11-05"))
    }
}
