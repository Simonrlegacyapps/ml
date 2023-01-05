package com.google.mlkit.vision.demo

import android.app.Application
import android.util.Log

class MyApp : Application() {
    private val TAG: String = MyApp::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        //Load FFMpeg library
        /*try {
            FFmpeg.getInstance(this).loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFailure() {
                    Log.e("FFMpeg", "Failed to load FFMpeg library.")
                }

                override fun onSuccess() {
                    Log.i("FFMpeg", "FFMpeg Library loaded!")
                }

                override fun onStart() {
                    Log.i("FFMpeg", "FFMpeg Started")
                }

                override fun onFinish() {
                    Log.i("FFMpeg", "FFMpeg Stopped")
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
            Log.i("FFMpeg", e.message.toString())

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("FFMpeg", e.message.toString())
        }*/
    }
}