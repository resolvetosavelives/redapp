package org.simple.clinic.summary.medicalhistory

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import kotlinx.android.synthetic.main.patientsummary_medicalhistoryview_content.view.*
import org.simple.clinic.R
import org.simple.clinic.medicalhistory.Answer
import org.simple.clinic.medicalhistory.MedicalHistory
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.DIAGNOSED_WITH_HYPERTENSION
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_DIABETES
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_HEART_ATTACK
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_KIDNEY_DISEASE
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_STROKE
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.IS_ON_TREATMENT_FOR_HYPERTENSION
import org.simple.clinic.medicalhistory.MedicalHistoryQuestionView
import org.simple.clinic.util.RelativeTimestamp
import org.threeten.bp.format.DateTimeFormatter

class MedicalHistorySummaryView(
    context: Context,
    attributeSet: AttributeSet
) : CardView(context, attributeSet) {

  init {
    LayoutInflater.from(context).inflate(R.layout.patientsummary_medicalhistoryview_content, this, true)
    diabetesQuestionView.hideDivider()
  }

  fun bind(
      medicalHistory: MedicalHistory,
      lastUpdatedAt: RelativeTimestamp,
      dateFormatter: DateTimeFormatter,
      answerToggled: (MedicalHistoryQuestion, Answer) -> Unit
  ) {
    val updatedAtDisplayText = lastUpdatedAt.displayText(context, dateFormatter)

    lastUpdatedAtTextView.text = context.getString(
        R.string.patientsummary_medicalhistory_last_updated,
        updatedAtDisplayText
    )

    renderQuestionView(diagnosedForHypertensionQuestionView, DIAGNOSED_WITH_HYPERTENSION, medicalHistory.diagnosedWithHypertension, answerToggled)
    renderQuestionView(treatmentForHypertensionQuestionView, IS_ON_TREATMENT_FOR_HYPERTENSION, medicalHistory.isOnTreatmentForHypertension, answerToggled)
    renderQuestionView(heartAttackQuestionView, HAS_HAD_A_HEART_ATTACK, medicalHistory.hasHadHeartAttack, answerToggled)
    renderQuestionView(strokeQuestionView, HAS_HAD_A_STROKE, medicalHistory.hasHadStroke, answerToggled)
    renderQuestionView(kidneyDiseaseQuestionView, HAS_HAD_A_KIDNEY_DISEASE, medicalHistory.hasHadKidneyDisease, answerToggled)
    renderQuestionView(diabetesQuestionView, HAS_DIABETES, medicalHistory.hasDiabetes, answerToggled)
  }

  private fun renderQuestionView(
      view: MedicalHistoryQuestionView,
      question: MedicalHistoryQuestion,
      answer: Answer,
      answerToggled: (MedicalHistoryQuestion, Answer) -> Unit
  ) {
    view.render(question, answer)
    view.answerChangeListener = { newAnswer -> answerToggled(question, newAnswer) }
  }
}
