package org.simple.clinic.teleconsultlog.drugduration

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Test

class DrugDurationUiRendererTest {

  private val ui = mock<DrugDurationUi>()
  private val uiRenderer = DrugDurationUiRenderer(ui)

  @Test
  fun `when drug duration is changed, then hide error`() {
    // given
    val initialDuration = ""
    val duration = "10"
    val model = DrugDurationModel.create(initialDuration)
        .durationChanged(duration)

    // when
    uiRenderer.render(model)

    // then
    verify(ui).hideDurationError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when drug duration validation result is blank, then show error`() {
    // given
    val duration = ""
    val model = DrugDurationModel.create(duration)
        .durationInvalid(Blank)

    // when
    uiRenderer.render(model)

    // then
    verify(ui).showBlankDurationError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when drug duration validation result is max duration, then show max duration error`() {
    // given
    val duration = "1001"
    val model = DrugDurationModel.create(duration)
        .durationInvalid(MaxDrugDuration)

    // when
    uiRenderer.render(model)

    // then
    verify(ui).showMaxDrugDurationError(1000)
    verifyNoMoreInteractions(ui)
  }
}
