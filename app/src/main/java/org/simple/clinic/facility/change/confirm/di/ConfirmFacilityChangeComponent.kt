package org.simple.clinic.facility.change.confirm.di

import androidx.appcompat.app.AppCompatActivity
import dagger.BindsInstance
import dagger.Subcomponent
import org.simple.clinic.facility.change.confirm.ConfirmFacilityChangeSheet

@Subcomponent
interface ConfirmFacilityChangeComponent {

  fun inject(target: ConfirmFacilityChangeSheet)

  @Subcomponent.Factory
  interface Factory {
    fun create(@BindsInstance activity: AppCompatActivity): ConfirmFacilityChangeComponent
  }
}