package org.simple.clinic.editpatient

enum class PatientEditValidationError(val analyticsName: String) {
  FULL_NAME_EMPTY("Patient Edit:Name is empty"),
  PHONE_NUMBER_EMPTY("Patient Edit:Phone Number is empty"),
  PHONE_NUMBER_LENGTH_TOO_SHORT("Patient Edit:Phone Number is less than 6 digits"),
  PHONE_NUMBER_LENGTH_TOO_LONG("Patient Edit:Phone Number is more than 12 digits"),
  COLONY_OR_VILLAGE_EMPTY("Patient Edit:Colony or village empty"),
  DISTRICT_EMPTY("Patient Edit:District empty"),
  STATE_EMPTY("Patient Edit:State empty")
}
