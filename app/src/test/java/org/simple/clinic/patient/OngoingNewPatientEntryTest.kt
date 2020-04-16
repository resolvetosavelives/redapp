package org.simple.clinic.patient

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.patient.OngoingNewPatientEntry.Address
import org.simple.clinic.patient.OngoingNewPatientEntry.PersonalDetails
import org.simple.clinic.patient.OngoingNewPatientEntry.PhoneNumber
import org.simple.clinic.registration.phone.PhoneNumberValidator
import org.simple.clinic.registration.phone.PhoneNumberValidator.Result.VALID
import org.simple.clinic.registration.phone.PhoneNumberValidator.Type.LANDLINE_OR_MOBILE
import org.simple.clinic.util.TestUserClock
import org.simple.clinic.widgets.ageanddateofbirth.UserInputAgeValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.DateIsInFuture
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset.UTC
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@RunWith(JUnitParamsRunner::class)
class OngoingNewPatientEntryTest {

  @Test
  @Parameters(method = "values")
  fun `validation should fail for invalid values for phone numbers in India`(
      isValid: Boolean,
      fullname: String,
      dateOfBirth: String?,
      age: String?,
      colonyOrVillage: String,
      district: String,
      state: String,
      phoneValidationResult: PhoneNumberValidator.Result
  ) {
    val entry = OngoingNewPatientEntry(
        personalDetails = PersonalDetails(fullname, dateOfBirth, age, Gender.Male),
        address = Address(
            colonyOrVillage = colonyOrVillage,
            district = district,
            state = state,
            streetAddress = "streetAddress",
            zone = "zone"
        ),
        phoneNumber = PhoneNumber(""))

    val dobValidator = UserInputDateValidator(
        userClock = TestUserClock(LocalDate.parse("2018-01-01")),
        dateOfBirthFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH))

    val ageValidator = UserInputAgeValidator(
        userClock = TestUserClock(LocalDate.parse("2018-01-01")),
        dateOfBirthFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    )

    val numValidator = mock<PhoneNumberValidator>()
    whenever(numValidator.validate("", LANDLINE_OR_MOBILE)).thenReturn(phoneValidationResult)

    assertThat(entry.validationErrors(dobValidator, numValidator, ageValidator)).apply {
      if (isValid) {
        isEmpty()
      } else {
        isNotEmpty()
      }
    }
  }

  fun values(): Array<Any> {
    return arrayOf(
        arrayOf(false, " ", " ", " ", " ", " ", " ", PhoneNumberValidator.Result.BLANK),
        arrayOf(false, " ", null, null, " ", " ", " ", PhoneNumberValidator.Result.BLANK),
        arrayOf(false, "Ashok Kumar", "01-01-1971", "47", "colony", "state", "district", PhoneNumberValidator.Result.LENGTH_TOO_SHORT),
        arrayOf(false, "Ashok Kumar", "01/01/1971", null, "colony", "state", "district", PhoneNumberValidator.Result.BLANK),
        arrayOf(false, "Ashok Kumar", "01/01/1971", null, "colony", "state", "district", PhoneNumberValidator.Result.LENGTH_TOO_SHORT),
        arrayOf(false, "Ashok Kumar", "01/01/1971", null, "colony", "state", "district", PhoneNumberValidator.Result.LENGTH_TOO_LONG),
        arrayOf(true, "Ashok Kumar", "01/01/1971", null, "colony", "state", "district", PhoneNumberValidator.Result.VALID)
    )
  }

  @Test
  fun `future date-of-birth should not be accepted`() {
    val entry = OngoingNewPatientEntry(
        personalDetails = PersonalDetails("Ashok", "01/01/3000", "", Gender.Male),
        address = Address(
            colonyOrVillage = "colony",
            district = "district",
            state = "state",
            streetAddress = "streetAddress",
            zone = "zone"
        ),
        phoneNumber = PhoneNumber("phone-number"))

    val mockDobValidator = mock<UserInputDateValidator>()
    val mockAgeValidator = mock<UserInputAgeValidator>()
    whenever(mockDobValidator.dateInUserTimeZone()).thenReturn(LocalDate.now(UTC))
    whenever(mockDobValidator.validate("01/01/3000")).thenReturn(DateIsInFuture)

    val mockNumValidator = mock<PhoneNumberValidator>()
    whenever(mockNumValidator.validate("phone-number", LANDLINE_OR_MOBILE)).thenReturn(VALID)

    val validationErrors = entry.validationErrors(mockDobValidator, mockNumValidator, mockAgeValidator)

    assertThat(validationErrors).hasSize(1)
    assertThat(validationErrors).contains(PatientEntryValidationError.DATE_OF_BIRTH_IN_FUTURE)
  }
}
