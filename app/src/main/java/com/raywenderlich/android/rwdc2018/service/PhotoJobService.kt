package com.raywenderlich.android.rwdc2018.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.raywenderlich.android.rwdc2018.app.PhotosUtils

class PhotoJobService : JobService() {

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Job Stopped: " + params?.jobId)
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val runnable = Runnable {
            val needsReschedule: Boolean
            needsReschedule = try {
                val fetchJsonString = PhotosUtils.fetchJsonString()
                (fetchJsonString == null)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error running job: " + e.message)
                true
            }

            Log.i(TAG, "Job finished: ${params?.jobId}," +
                    "needsReschedule = $needsReschedule")
            jobFinished(params, needsReschedule)
        }

        Thread(runnable).start()

        return true
    }

    companion object {
        private const val TAG = "PhotoJobService"
    }
}
