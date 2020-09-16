package org.simple.clinic.teleconsultlog.prescription.medicines

import com.spotify.mobius.Next
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

class TeleconsultMedicinesUpdate : Update<TeleconsultMedicinesModel, TeleconsultMedicinesEvent, TeleconsultMedicinesEffect> {

  override fun update(
      model: TeleconsultMedicinesModel,
      event: TeleconsultMedicinesEvent
  ): Next<TeleconsultMedicinesModel, TeleconsultMedicinesEffect> {
    return when (event) {
      is PatientMedicinesLoaded -> next(model.medicinesLoaded(event.medicines))
    }
  }
}
