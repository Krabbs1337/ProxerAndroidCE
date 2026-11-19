package me.proxer.app.util

import io.mockk.every
import io.mockk.mockk
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Validators].
 */
class ValidatorsTest {
    private val storageHelper = mockk<StorageHelper>()
    private val preferenceHelper = mockk<PreferenceHelper>()
    private val validators = Validators(storageHelper, preferenceHelper)

    @Test
    fun `validateLogin throws NotLoggedInException when not logged in`() {
        every { storageHelper.isLoggedIn } returns false

        assertThrows<NotLoggedInException> { validators.validateLogin() }
    }

    @Test
    fun `validateLogin passes when logged in`() {
        every { storageHelper.isLoggedIn } returns true

        assertDoesNotThrow { validators.validateLogin() }
    }

    @Test
    fun `validateAgeConfirmation throws when age restricted media is not allowed`() {
        every { preferenceHelper.isAgeRestrictedMediaAllowed } returns false

        assertThrows<AgeConfirmationRequiredException> { validators.validateAgeConfirmation() }
    }

    @Test
    fun `validateAgeConfirmation passes when age restricted media is allowed`() {
        every { preferenceHelper.isAgeRestrictedMediaAllowed } returns true

        assertDoesNotThrow { validators.validateAgeConfirmation() }
    }
}
