package org.simple.clinic.main

interface TheActivityUi: TheActivityUiActions {

  // This is here because we need to show the same alert in multiple
  // screens when the user gets verified in the background.
  fun showUserLoggedOutOnOtherDeviceAlert()
  fun redirectToLogin()
  fun showAccessDeniedScreen(fullName: String)
}
