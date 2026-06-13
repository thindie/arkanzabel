package com.thindie.rknzbl.application.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.thindie.rknzbl.application.di.ApplicationScope

class RknzblWorkerFactory(
  private val applicationScope: ApplicationScope,
) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters,
  ): ListenableWorker? {
    if (workerClassName != ActiveProfileAutoSaveWorker::class.java.name) {
      return null
    }
    return ActiveProfileAutoSaveWorker(
      appContext = appContext,
      params = workerParameters,
      repository = applicationScope.data.repository,
    )
  }
}
