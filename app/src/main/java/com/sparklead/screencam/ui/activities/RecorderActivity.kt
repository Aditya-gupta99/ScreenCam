package com.sparklead.screencam.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sparklead.screencam.R
import com.sparklead.screencam.databinding.ActivityRecorderBinding
import com.sparklead.screencam.utils.Constants
import java.io.File
import java.io.IOException


class RecorderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecorderBinding
    private var mediaProjection: MediaProjection? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var mediaProjectCallBack: MediaProjectCallBack

    private var mScreenDensity: Int? = null
    private var DISPLAY_WIDTH = 720
    private var DISPLAY_HEIGTH = 1280

    private var mediaRecorder: MediaRecorder? = null

    private lateinit var videoView: VideoView
    private var enable = false
    private var mUrl: String = ""
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Implemented data binding
        binding = ActivityRecorderBinding.inflate(layoutInflater)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mediaRecorder = MediaRecorder()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        binding.ivRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) + ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                enable = false
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                    ), Constants.REQUEST_PERMISSION
                )
            } else {
                startRecording()
            }
        }


        setContentView(binding.root)

        binding.lifecycleOwner = this
    }

//    override fun onClick(v: View?) {
//        if (v != null) {
//            when (v.id) {
//                (R.id.iv_record) -> {
//                    if (ContextCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE
//                        ) + ContextCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.RECORD_AUDIO
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        enable = false
//                        ActivityCompat.requestPermissions(
//                            this,
//                            arrayOf(
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                Manifest.permission.RECORD_AUDIO
//                            ),Constants.REQUEST_CODE
//                        )
//                    }
//                    else{
//                        startRecording()
//                    }
//                }
//            }
//        }
//    }

    private fun startRecording() {
        if (!enable) {
            setupRecorder()
            recordingStart()
            binding.ivRecord.setImageResource(R.drawable.stop)
            enable = true
        }
        else{
            try {
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
                stopRecording()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            binding.ivRecord.setImageResource(R.drawable.rec)
            enable = false
        }
    }

    private fun setupRecorder() {
        try {
            val fileName = ("ScreenCam${System.currentTimeMillis()}.mp4")
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            setupFileLocation(fileName)
        }
        catch (e: java.lang.Exception){
            e.printStackTrace()
        }

    }

    private fun setupFileLocation(fileName: String) {

        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val folder = File(path, "ScreenCam/")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, fileName)
        mUrl = file.absolutePath

        mediaRecorder!!.setOutputFile(mUrl)
        mediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGTH)
        mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder!!.setVideoFrameRate(30)
        mediaRecorder!!.setVideoEncodingBitRate(512 * 1000)

        val rotation = windowManager.defaultDisplay.rotation
        val orientation = ORIENTATIONS.get(rotation + 90)

        mediaRecorder!!.setOrientationHint(orientation)
//        mediaRecorder!!.prepare()
//        mediaRecorder.start()

        try {
            mediaRecorder!!.prepare()
//            mediaRecorder!!.start()
//            mStartRecording = true
        } catch (e: IOException) {
            Log.e("dataNew", "Error when preparing or starting recorder", e)
        }
    }

    fun stopRecording() {
        if (virtualDisplay == null)
            return
        virtualDisplay!!.release()
        destroyProjection()
    }

    private fun destroyProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.unregisterCallback(mediaProjectCallBack)
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            "RecorderActivity", DISPLAY_WIDTH, DISPLAY_HEIGTH,
            mScreenDensity!!, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface, null, null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != Constants.REQUEST_CODE) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission denied ", Toast.LENGTH_LONG).show()
            enable = false
            return
        }
        mediaProjectCallBack = MediaProjectCallBack(mediaRecorder!!, mediaProjection, enable)
//        val intent = Intent(this, BackgroundService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent)
//        } else {
//            startService(intent)
//        }

//        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent!!, 777)
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data!!)
        mediaProjection!!.registerCallback(mediaProjectCallBack, null)
        virtualDisplay = createVirtualDisplay()
        try {
            mediaRecorder!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun recordingStart() {
        if (mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                Constants.REQUEST_CODE
            )
        }
        virtualDisplay = createVirtualDisplay()
        try {
            mediaRecorder!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    enable = false
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                        ),
                        Constants.REQUEST_PERMISSION
                    )
                }
            }
        }
    }

    inner class MediaProjectCallBack(
        private var mediaRecord: MediaRecorder,
        private var mediaProjection: MediaProjection?,
        private var enable: Boolean
    ) : MediaProjection.Callback() {

        override fun onStop() {
            if (enable) {
                enable = false
                mediaRecord.stop()
                mediaRecord.reset()
            }
            mediaProjection = null
            RecorderActivity().stopRecording()
            super.onStop()
        }
    }
}