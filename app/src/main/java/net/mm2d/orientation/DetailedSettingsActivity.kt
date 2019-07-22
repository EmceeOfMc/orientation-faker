/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_detailed_settings.*
import kotlinx.android.synthetic.main.layout_detailed_settings.*
import net.mm2d.android.orientationfaker.BuildConfig
import net.mm2d.android.orientationfaker.R
import net.mm2d.color.chooser.ColorChooserDialog
import net.mm2d.orientation.control.OrientationHelper
import net.mm2d.orientation.settings.Settings

class DetailedSettingsActivity
    : AppCompatActivity(), ResetThemeDialog.Callback, ColorChooserDialog.Callback {
    private val settings by lazy {
        Settings.get()
    }
    private val orientationHelper by lazy {
        OrientationHelper.getInstance(this)
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            notificationSample.update()
        }
    }
    private lateinit var notificationSample: NotificationSample
    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpViews()
        UpdateRouter.register(receiver)
        setUpAdView()
    }

    private fun setUpAdView() {
        adView = AdMob.makeDetailedAdView(this)
        container.addView(adView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    override fun onDestroy() {
        super.onDestroy()
        UpdateRouter.unregister(receiver)
    }

    override fun onResume() {
        super.onResume()
        notificationSample.update()
        AdMob.loadAd(this, adView)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpViews() {
        notificationSample = NotificationSample(this)
        setUpSample()
        setUpOrientationIcons()
        setUpUseFullSensor()
        setUpNotificationPrivacy()
        setUpSystemSetting()
    }

    private fun setUpOrientationIcons() {
        notificationSample.buttonList.forEach { view ->
            view.button.setOnClickListener { updateOrientation(view.orientation) }
        }
    }

    private fun updateOrientation(orientation: Int) {
        settings.orientation = orientation
        notificationSample.update()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setUpSample() {
        sample_foreground.setColorFilter(settings.foregroundColor)
        sample_background.setColorFilter(settings.backgroundColor)
        sample_foreground_selected.setColorFilter(settings.foregroundColorSelected)
        sample_background_selected.setColorFilter(settings.backgroundColorSelected)
        foreground.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColor)
        }
        background.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColor)
        }
        foreground_selected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColorSelected)
        }
        background_selected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColorSelected)
        }
        reset.setOnClickListener { ResetThemeDialog.show(this) }
    }

    override fun onColorChooserResult(requestCode: Int, resultCode: Int, color: Int) {
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            R.id.foreground -> {
                settings.foregroundColor = color
                sample_foreground.setColorFilter(color)
            }
            R.id.background -> {
                settings.backgroundColor = color
                sample_background.setColorFilter(color)
            }
            R.id.foreground_selected -> {
                settings.foregroundColorSelected = color
                sample_foreground_selected.setColorFilter(color)
            }
            R.id.background_selected -> {
                settings.backgroundColorSelected = color
                sample_background_selected.setColorFilter(color)
            }
        }
        notificationSample.update()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    override fun resetTheme() {
        settings.resetTheme()
        sample_foreground.setColorFilter(settings.foregroundColor)
        sample_background.setColorFilter(settings.backgroundColor)
        sample_foreground_selected.setColorFilter(settings.foregroundColorSelected)
        sample_background_selected.setColorFilter(settings.backgroundColorSelected)
        notificationSample.update()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setUpUseFullSensor() {
        use_full_sensor.setOnClickListener { toggleUseFullSensor() }
        applyUseFullSensor()
    }

    private fun applyUseFullSensor() {
        use_full_sensor.isChecked = settings.useFullSensor
    }

    private fun toggleUseFullSensor() {
        settings.useFullSensor = !settings.useFullSensor
        applyUseFullSensor()
        notificationSample.update()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setUpNotificationPrivacy() {
        notification_privacy.setOnClickListener { toggleNotificationPrivacy() }
        applyNotificationPrivacy()
    }

    private fun applyNotificationPrivacy() {
        notification_privacy.isChecked = settings.notifySecret
    }

    private fun toggleNotificationPrivacy() {
        settings.notifySecret = !settings.notifySecret
        applyNotificationPrivacy()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setUpSystemSetting() {
        system_app.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                it.data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        system_notification.setOnClickListener {
            startActivity(Intent(ACTION_APP_NOTIFICATION_SETTINGS).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.putExtra("app_package", BuildConfig.APPLICATION_ID)
                it.putExtra("app_uid", applicationInfo.uid)
                it.putExtra("android.provider.extra.APP_PACKAGE", BuildConfig.APPLICATION_ID)
            })
        }
    }

    companion object {
        private const val ACTION_APP_NOTIFICATION_SETTINGS =
            "android.settings.APP_NOTIFICATION_SETTINGS"

        fun start(context: Context) {
            context.startActivity(Intent(context, DetailedSettingsActivity::class.java))
        }
    }
}
