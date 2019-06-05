/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.raywenderlich.android.rwdc2018.repository

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.raywenderlich.android.rwdc2018.app.PhotosUtils
import com.raywenderlich.android.rwdc2018.app.RWDC2018Application
import com.raywenderlich.android.rwdc2018.service.DownloadWorker
import com.raywenderlich.android.rwdc2018.service.LogJobService
import com.raywenderlich.android.rwdc2018.service.PhotoJobService
import java.util.concurrent.TimeUnit


class PhotosRepository : Repository {
    private val photosLiveData = MutableLiveData<List<String>>()
    private val bannerLiveData = MutableLiveData<String>()

    init {
        scheduleFetchPhotosWithWorkManager()
    }

    companion object {
        const val DOWNLOAD_WORKER_TAG = "DownloadWorker"
    }

    override fun getPhotos(): LiveData<List<String>> {
        //fetchPhotoJson()
        fetchPictures()
        return photosLiveData
    }

    private fun scheduleFetchPhotosWithWorkManager() {
        val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val workManager = WorkManager.getInstance()

        val workRequest = PeriodicWorkRequestBuilder<DownloadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(DOWNLOAD_WORKER_TAG)
                .build()

        workManager.cancelAllWorkByTag(DOWNLOAD_WORKER_TAG)
        workManager.enqueue(workRequest)
    }

    private fun scheduleFetchPhotos() {
        val jobScheduler = RWDC2018Application.getAppContext().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(10,
                ComponentName(RWDC2018Application.getAppContext(), PhotoJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(90000)
                .build()

        jobScheduler.schedule(jobInfo)
    }

    private fun scheduleTest() {
        val jobScheduler = RWDC2018Application.getAppContext().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(11,
                ComponentName(RWDC2018Application.getAppContext(), LogJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()

        jobScheduler.schedule(jobInfo)
    }

    private fun fetchPictures() {
        val runnable = Runnable {
            val photoJsonString = PhotosUtils.photoJsonString()
            val photoUrlsFromJsonString = PhotosUtils.photoUrlsFromJsonString(photoJsonString ?: "")
            val bannersUrlsFromJsonString = PhotosUtils.bannerFromJsonString(photoJsonString ?: "")

            Log.i("LOGHELPER", photoJsonString)

            photosLiveData.postValue(photoUrlsFromJsonString ?: emptyList())
            bannerLiveData.postValue(bannersUrlsFromJsonString ?: "")
        }

        val thread = Thread(runnable)
        thread.start()
    }

    private fun fetchPhotoJson() {
        val runnable = Runnable {
            val photoJsonString = PhotosUtils.photoJsonString()
            val photoUrlsFromJsonString = PhotosUtils.photoUrlsFromJsonString(photoJsonString ?: "")
            Log.i("LOGHELPER", photoJsonString)
            photosLiveData.postValue(photoUrlsFromJsonString)
        }

        val thread = Thread(runnable)
        thread.start()
    }


    private fun fetchPhotoJsonWithHandler() {
        val handler = object : android.os.Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                msg?.data?.getStringArrayList("PHOTOS_KEY")?.let {
                    photosLiveData.value = it
                }
            }
        }

        val runnable = Runnable {
            val photoJsonString = PhotosUtils.photoJsonString()
            val photoUrlsFromJsonString = PhotosUtils.photoUrlsFromJsonString(photoJsonString ?: "")
            Log.i("LOGHELPER", photoJsonString)
            val message = Message.obtain()
            message.data = Bundle().apply { putStringArrayList("PHOTOS_KEY", photoUrlsFromJsonString) }
            handler.sendMessage(message)
        }

        val thread = Thread(runnable)
        thread.start()
    }

    override fun getBanner(): LiveData<String> {
        //fetchBannerJson()
        return bannerLiveData
    }

    private fun fetchBannerJson() {
        val runnable = Runnable {
            val photoJsonString = PhotosUtils.photoJsonString()
            val photoUrlsFromJsonString = PhotosUtils.bannerFromJsonString(photoJsonString ?: "")
            Log.i("LOGHELPER", photoJsonString)
            bannerLiveData.postValue(photoUrlsFromJsonString)
        }

        val thread = Thread(runnable)
        thread.start()
    }

    private class FetchPhotoAsyncTask(val callback: (List<String>) -> Unit) : AsyncTask<Void, Void, List<String>>() {

        override fun doInBackground(vararg params: Void?): List<String> {
            val photoJsonString = PhotosUtils.photoJsonString()
            val photoUrlsFromJsonString = PhotosUtils.photoUrlsFromJsonString(photoJsonString ?: "")
            return photoUrlsFromJsonString ?: emptyList()
        }

        override fun onPostExecute(result: List<String>?) {
            super.onPostExecute(result)
            callback.invoke(result ?: emptyList())
        }
    }

}