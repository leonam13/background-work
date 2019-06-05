package com.raywenderlich.android.rwdc2018.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.rwdc2018.app.PhotosUtils

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val needsReschedule = try {
            val fetchJsonString = PhotosUtils.fetchJsonString()
            (fetchJsonString == null)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error running job: " + e.message)
            true
        }
        if (needsReschedule) Result.retry()

        return Result.success()
    }

    companion object {
        private const val TAG = "DownloadWorker"
    }
}