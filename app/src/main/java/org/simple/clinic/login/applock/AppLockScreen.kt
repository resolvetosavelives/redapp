package org.simple.clinic.login.applock

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.pin_entry_card.view.*
import kotterknife.bindView
import org.simple.clinic.R
import org.simple.clinic.bindUiToController
import org.simple.clinic.main.TheActivity
import org.simple.clinic.router.screen.BackPressInterceptCallback
import org.simple.clinic.router.screen.BackPressInterceptor
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.security.pin.PinAuthenticated
import org.simple.clinic.security.pin.PinEntryCardView
import org.simple.clinic.widgets.ScreenDestroyed
import org.simple.clinic.widgets.showKeyboard
import javax.inject.Inject

class AppLockScreen(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), AppLockScreenUi {

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var controller: AppLockScreenController

  @Inject
  lateinit var activity: AppCompatActivity

  private val facilityTextView by bindView<TextView>(R.id.applock_facility_name)
  private val fullNameTextView by bindView<TextView>(R.id.applock_user_fullname)
  private val logoutButton by bindView<MaterialButton>(R.id.applock_logout)
  private val pinEntryCardView by bindView<PinEntryCardView>(R.id.applock_pin_entry_card)

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    TheActivity.component.inject(this)

    bindUiToController(
        ui = this,
        events = Observable.merge(
            screenCreates(),
            backClicks(),
            forgotPinClicks(),
            pinAuthentications()
        ),
        controller = controller,
        screenDestroys = RxView.detaches(this).map { ScreenDestroyed() }
    )

    logoutButton.setOnClickListener {
      Toast.makeText(context, "Work in progress", Toast.LENGTH_SHORT).show()
    }

    // The keyboard shows up on PIN field automatically when the app is
    // starting, but not when the user comes back from FacilityChangeScreen.
    pinEntryCardView.pinEditText.showKeyboard()
  }

  private fun screenCreates() = Observable.just(AppLockScreenCreated())

  private fun backClicks(): Observable<AppLockBackClicked> {
    return Observable.create { emitter ->
      val interceptor = object : BackPressInterceptor {
        override fun onInterceptBackPress(callback: BackPressInterceptCallback) {
          emitter.onNext(AppLockBackClicked())
          callback.markBackPressIntercepted()
        }
      }
      emitter.setCancellable { screenRouter.unregisterBackPressInterceptor(interceptor) }
      screenRouter.registerBackPressInterceptor(interceptor)
    }
  }

  private fun forgotPinClicks() =
      RxView
          .clicks(pinEntryCardView.forgotPinButton)
          .map { AppLockForgotPinClicked() }

  private fun pinAuthentications() =
      pinEntryCardView
          .downstreamUiEvents
          .ofType<PinAuthenticated>()
          .map { AppLockPinAuthenticated() }

  override fun setUserFullName(fullName: String) {
    fullNameTextView.text = fullName
  }

  override fun setFacilityName(facilityName: String) {
    this.facilityTextView.text = facilityName
  }

  override fun restorePreviousScreen() {
    screenRouter.pop()
  }

  override fun exitApp() {
    activity.finish()
  }

  override fun showConfirmResetPinDialog() {
    ConfirmResetPinDialog.show(activity.supportFragmentManager)
  }
}
