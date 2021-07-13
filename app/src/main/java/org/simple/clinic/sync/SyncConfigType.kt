package org.simple.clinic.sync

import javax.inject.Qualifier

@Qualifier
annotation class SyncConfigType(val type: Type) {

  enum class Type {
    Drugs
  }
}
