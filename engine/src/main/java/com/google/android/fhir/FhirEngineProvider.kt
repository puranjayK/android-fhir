/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir

import android.content.Context
import com.google.android.fhir.DatabaseErrorStrategy.UNSPECIFIED

/** The builder for [FhirEngine] instance */
object FhirEngineProvider {
  private lateinit var fhirEngineConfiguration: FhirEngineConfiguration
  private lateinit var fhirEngine: FhirEngine

  @Synchronized
  fun init(fhirEngineConfiguration: FhirEngineConfiguration) {
    check(!FhirEngineProvider::fhirEngineConfiguration.isInitialized) {
      "FhirEngineProvider: FhirEngineConfiguration has already been initialized."
    }
    this.fhirEngineConfiguration = fhirEngineConfiguration
  }

  /**
   * Returns the cached [FhirEngine] instance. Creates a new instance from the supplied [Context] if
   * it doesn't exist.
   */
  @Synchronized
  fun getInstance(context: Context): FhirEngine {
    if (!::fhirEngine.isInitialized) {
      if (!::fhirEngineConfiguration.isInitialized) {
        fhirEngineConfiguration = FhirEngineConfiguration(enableEncryption = false, UNSPECIFIED)
      }
      fhirEngine =
        FhirServices.builder(context.applicationContext)
          .apply {
            if (fhirEngineConfiguration.enableEncryption) enableEncryption()
            setDatabaseErrorStrategy(fhirEngineConfiguration.databaseErrorStrategy)
          }
          .build()
          .fhirEngine
    }
    return fhirEngine
  }
}

data class FhirEngineConfiguration(
  val enableEncryption: Boolean,
  val databaseErrorStrategy: DatabaseErrorStrategy
)

enum class DatabaseErrorStrategy {
  /**
   * If unspecified, all database errors will be propagated to the call site. The caller shall
   * handle the database error on a case-by-case basis.
   */
  UNSPECIFIED,

  /** If a database error occurs at open, automatically recreate the database. */
  RECREATE_AT_OPEN
}
