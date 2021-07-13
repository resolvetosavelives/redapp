package org.simple.clinic.main

import javax.inject.Qualifier

@Qualifier
annotation class TypedPreference(val value: Type) {

  enum class Type {
    OnboardingComplete,
    FallbackCountry,
    DatabaseMaintenanceRunAt,
    MedicalRegistrationId,
    FacilitySyncGroupSwitchedAt,
    LastDrugPullToken
  }
}
