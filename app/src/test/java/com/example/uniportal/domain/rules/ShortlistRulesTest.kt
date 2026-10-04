package com.example.uniportal.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortlistRulesTest {
    @Test
    fun `given existing names, when the same name is typed in another case or with spaces, then it is NAME_DUPLICATE`() {
        val existing = listOf("Мечта", "Backup ", "Европа")
        val duplicate = ShortlistNameValidation.Invalid(ShortlistError.NAME_DUPLICATE)

        assertEquals(duplicate, ShortlistRules.validateName("мечта", existing))
        assertEquals(duplicate, ShortlistRules.validateName("МЕЧТА", existing))
        assertEquals(duplicate, ShortlistRules.validateName("  backup", existing))
        assertEquals(duplicate, ShortlistRules.validateName("BACKUP", existing))
        assertEquals(ShortlistNameValidation.Valid("Мечта 2"), ShortlistRules.validateName("Мечта 2", existing))
        assertEquals(ShortlistNameValidation.Valid("Европы"), ShortlistRules.validateName("Европы", existing))
    }
}
