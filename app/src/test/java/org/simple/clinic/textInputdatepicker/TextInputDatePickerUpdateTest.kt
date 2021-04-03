package org.simple.clinic.textInputdatepicker

import com.spotify.mobius.test.NextMatchers.hasEffects
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.NextMatchers.hasNoModel
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import org.junit.Test
import org.simple.clinic.textInputdatepicker.TextInputDatePickerEffect.DismissSheet
import org.simple.clinic.textInputdatepicker.TextInputDatePickerEffect.HideDateErrorMessage

class TextInputDatePickerUpdateTest {

  private val updateSpec = UpdateSpec(TextInputDatePickerUpdate())
  private val defaultModel = TextInputDatePickerModel.create()

  @Test
  fun `when close button is clicked, then close the sheet`() {
    updateSpec
        .given(defaultModel)
        .whenEvent(DismissSheetClicked)
        .then(
            assertThatNext(
                hasNoModel(),
                hasEffects(DismissSheet)
            )
        )
  }

  @Test
  fun `when text changes in the day edittext field, then hide the error message`() {
    val day = "21"

    updateSpec
        .given(defaultModel)
        .whenEvent(DayChanged(day))
        .then(
            assertThatNext(
                hasModel(defaultModel.dayChanged(day)),
                hasEffects(HideDateErrorMessage)
            )
        )
  }
}
