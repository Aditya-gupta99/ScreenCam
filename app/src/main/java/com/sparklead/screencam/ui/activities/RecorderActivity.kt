package com.sparklead.screencam.ui.activities

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import com.sparklead.screencam.R
import com.sparklead.screencam.databinding.ActivityRecorderBinding
import com.sparklead.screencam.utils.Constants
import com.sparklead.screencam.utils.Constants.appSettingOpen
import com.sparklead.screencam.utils.Constants.warningPermissionDialog
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*


class RecorderActivity : AppCompatActivity(), View.OnClickListener, HBRecorderListener {

    private lateinit var binding: ActivityRecorderBinding
    private var hasPermission = false
    private var hbRecorder: HBRecorder? = null
    private var highDefinition = true
    private var audioRecord = false

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Implemented data binding
        binding = ActivityRecorderBinding.inflate(layoutInflater)

        //Initialized hbrRecorder
        hbRecorder = HBRecorder(this, this)

        //Implemented onClickListener
        binding.ivRecord.setOnClickListener(this)
        binding.ivPause.setOnClickListener(this)

        setContentView(binding.root)

        binding.lifecycleOwner = this
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                //Permission for Audio and write external storage
                (R.id.iv_record) -> {
                    if (checkMultiplePermission()) {
                        startRecording()
                    }
                }
                (R.id.iv_pause) -> {
                    hbRecorder!!.stopScreenRecording()
                }
            }
        }
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // here all permission granted successfully
                    startRecording()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // here app Setting open because all permission is not granted
                        // and permanent denied
                        appSettingOpen(this)
                    } else {
                        // here warning permission show
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBasicOptions() {
        //switch for disable/enable
        binding.swEnableDisable.isChecked = highDefinition
        binding.swEnableDisable.setOnCheckedChangeListener { _, isChecked ->
            highDefinition = isChecked
        }
        //switch for disable/enable audio
        binding.swEnableDisableAudio.isChecked = audioRecord
        binding.swEnableDisableAudio.setOnCheckedChangeListener { _, isChecked ->
            audioRecord = isChecked
        }
    }

    private fun startRecording() {
        if (hasPermission) {
            if (hbRecorder!!.isBusyRecording) {
                hbRecorder!!.stopScreenRecording()
            }
        } else {
            startRecordingScreen()
        }
    }

    private fun startRecordingScreen() {
        //Implemented basic property for recorder
        getBasicOptions()
        quickSettings()
        //Implemented media projection manager to capture the contents of a device display
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, Constants.SCREEN_RECORD_REQUEST_CODE)
    }


    private fun quickSettings() {
        // Basic property for video and audio
        hbRecorder!!.setAudioBitrate(128000)
        hbRecorder!!.setAudioSamplingRate(44100)
        hbRecorder!!.recordHDVideo(highDefinition)
        hbRecorder!!.isAudioEnabled(audioRecord)
        //Customise Notification for app
        hbRecorder!!.setNotificationSmallIcon(R.drawable.screencam)
        hbRecorder!!.setNotificationTitle("ScreenCam")
        hbRecorder!!.setNotificationDescription("ScreenCam is Recording")
    }


    private fun createFolder() {
        //Saved recorded to Download folder with ScreenCam folder
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(path, "ScreenCam")

        //if there is no such folder then create new folder
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //Set file path or Uri
                setOutputPath()

                //Start screen recording
                hbRecorder!!.startScreenRecording(data, resultCode)
            }
        }
    }

    private fun setOutputPath() {
        // Set system default time
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        val fileName = formatter.format(curDate).replace(" ", "")

        //Set Video title and type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "ScreenCam")
            contentValues.put(MediaStore.MediaColumns.TITLE, fileName)
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            val mUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            //File name should be same
            hbRecorder!!.fileName = fileName
            hbRecorder!!.setOutputUri(mUri)
        } else {
            //Created folder
            createFolder()
            hbRecorder!!.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/ScreenCam"
            )
        }
    }

    override fun HBRecorderOnStart() {
        //After screen recording start working
        binding.ivRecord.visibility = View.GONE
        binding.ivPause.visibility = View.VISIBLE
        Toast.makeText(this,"Recording Starts",Toast.LENGTH_SHORT).show()
    }

    override fun HBRecorderOnComplete() {
        //After screen recording complete
        hbRecorder!!.stopScreenRecording()
        binding.ivPause.visibility = View.GONE
        binding.ivRecord.visibility = View.VISIBLE
        Toast.makeText(this,"Recording saved to your Download folder Successfully",Toast.LENGTH_LONG).show()
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        // After any exception
        Log.e("Error", reason.toString())
    }

    override fun onResume() {
        super.onResume()
        getBasicOptions()
    }

}