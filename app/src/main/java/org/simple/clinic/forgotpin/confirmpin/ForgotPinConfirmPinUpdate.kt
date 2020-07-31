package org.simple.clinic.forgotpin.confirmpin

import com.spotify.mobius.Next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update
import org.simple.clinic.mobius.dispatch
import org.simple.clinic.mobius.next

class ForgotPinConfirmPinUpdate : Update<ForgotPinConfirmPinModel, ForgotPinConfirmPinEvent,
    ForgotPinConfirmPinEffect> {
  override fun update(model: ForgotPinConfirmPinModel, event: ForgotPinConfirmPinEvent):
      Next<ForgotPinConfirmPinModel, ForgotPinConfirmPinEffect> {
    return when (event) {
      is LoggedInUserLoaded -> next(model.userLoaded(event.user))
      is CurrentFacilityLoaded -> next(model.facilityLoaded(event.facility))
      is ForgotPinConfirmPinTextChanged -> dispatch(HideError)
      is PinConfirmationValidated -> noChange()
    }
  }
}
