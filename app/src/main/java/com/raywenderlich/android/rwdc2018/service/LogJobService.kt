package com.raywenderlich.android.rwdc2018.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class LogJobService : JobService() {

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i("LOGHELPER", "On Stop Job")
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val runnable = Runnable {
            Log.i("LOGHELPER", "On Start Job")
            Log.i("LOGHELPER", "Params $params")

            Thread.sleep(5000)

            jobFinished(params, false)
        }

        Thread(runnable).start()

        return true
    }


}
