package org.resolvetosavelives.red

import android.app.Application
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.gabrielittner.threetenbp.LazyThreeTen
import com.tspoon.traceur.Traceur
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import org.resolvetosavelives.red.di.AppComponent
import timber.log.Timber

abstract class RedApp : MultiDexApplication() {

  companion object {
    lateinit var appComponent: AppComponent
  }

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Traceur.enableLogging()
      Stetho.initializeWithDefaults(this)
    }

    LazyThreeTen.init(this)

    appComponent = buildDaggerGraph()
    Sentry.init(AndroidSentryClientFactory(applicationContext))
  }

  abstract fun buildDaggerGraph(): AppComponent
}
