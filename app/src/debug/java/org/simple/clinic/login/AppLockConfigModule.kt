package org.simple.clinic.login

import dagger.Module
import dagger.Provides
import org.simple.clinic.di.AppScope
import org.simple.clinic.login.applock.AppLockConfig
import org.simple.clinic.storage.MemoryValue
import java.time.Instant
import java.util.concurrent.TimeUnit

@Module
class AppLockConfigModule {

  @Provides
  fun appLockConfig(): AppLockConfig {
    return AppLockConfig(lockAfterTimeMillis = TimeUnit.SECONDS.toMillis(30))
  }

  @Provides
  @AppScope
  fun provideAppLockAfterTimestamp(): MemoryValue<Instant> {
    return MemoryValue(defaultValue = Instant.MAX)
  }
}
