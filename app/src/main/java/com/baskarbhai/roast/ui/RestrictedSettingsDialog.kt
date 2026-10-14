package com.baskarbhai.roast.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.widget.Button
import com.baskarbhai.roast.R

/**
 * Interactive Dialog explaining how to bypass Android 13/14 Restricted Settings.
 * Engineered by mdx.
 */
class RestrictedSettingsDialog(context: Context) : Dialog(context, R.style.Theme_BasKarBhai_Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_restricted_settings)

        findViewById<Button>(R.id.btnOpenAppInfo)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            dismiss()
        }

        findViewById<Button>(R.id.btnDismissGuide)?.setOnClickListener {
            dismiss()
        }
    }
}
