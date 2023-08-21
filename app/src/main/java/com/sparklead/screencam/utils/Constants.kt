package com.sparklead.screencam.utils

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Constants {

    // Implemented this class for all constants
    const val SCREEN_RECORD_REQUEST_CODE = 777
    const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    const val PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 23




    fun appSettingOpen(context: Context){
        Toast.makeText(
            context,
            "Go to Setting and Enable All Permission",
            Toast.LENGTH_LONG
        ).show()

        val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        settingIntent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(settingIntent)
    }

    fun warningPermissionDialog(context: Context,listener : DialogInterface.OnClickListener){
        MaterialAlertDialogBuilder(context)
            .setMessage("All Permission are Required for this app")
            .setCancelable(false)
            .setPositiveButton("Ok",listener)
            .create()
            .show()
    }
}