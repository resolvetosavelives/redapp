package org.simple.clinic.home.help

import org.simple.clinic.mobius.ViewRenderer

class HelpScreenUiRenderer(private val ui: HelpScreenUi) : ViewRenderer<HelpScreenModel> {
  override fun render(model: HelpScreenModel) {
    if (model.hasHelpContent) {
      ui.showHelp(model.helpContent!!)
    } else {
      ui.showNoHelpAvailable()
    }
  }
}
