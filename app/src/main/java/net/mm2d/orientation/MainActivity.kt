/*
 * Copyright (c) 2014 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.notification.*
import net.mm2d.android.orientationfaker.BuildConfig
import net.mm2d.android.orientationfaker.R
import net.mm2d.log.Log
import net.mm2d.orientation.orientation.OrientationHelper
import net.mm2d.orientation.orientation.OrientationIdManager
import net.mm2d.orientation.orientation.OverlayPermissionHelper
import net.mm2d.orientation.settings.Settings
import net.mm2d.orientation.tabs.CustomTabsHelper
import java.util.*

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class MainActivity : AppCompatActivity() {
    private val settings by lazy {
        Settings.get()
    }
    private val orientationHelper by lazy {
        OrientationHelper.getInstance(this)
    }
    private val buttonList = ArrayList<Pair<Int, View>>()
    private val handler = Handler(Looper.getMainLooper())
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setStatusDescription()
            setOrientationIcon()
        }
    }
    private lateinit var adView: AdView
    private lateinit var relevantAds: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_name)
        status.setOnClickListener { toggleStatus() }
        resident.setOnClickListener { toggleResident() }
        version_description.text = makeVersionInfo()
        setStatusDescription()
        setResidentCheckBox()
        setUpOrientationIcons()
        registerReceiver()
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            MainService.stop(this)
        } else if (settings.shouldResident()) {
            MainService.start(this)
        }
        setUpAdView()
        checkPermission()
    }

    private fun setUpAdView() {
        adView = AdMob.makeAdView(this)
        container.addView(adView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun checkPermission() {
        OverlayPermissionHelper.requestOverlayPermissionIfNeed(this, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            handler.postDelayed({ checkPermission() }, 1000)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(ACTION_UPDATE))
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        AdMob.loadAd(this, adView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        relevantAds = menu.findItem(R.id.relevant_ads)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        relevantAds.isVisible = AdMob.isInEeaOrUnknown()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.license -> LicenseActivity.start(this)
            R.id.source_code -> openSourceCode(this)
            R.id.privacy_policy -> openPrivacyPolicy(this)
            R.id.play_store -> openGooglePlay(this)
            R.id.relevant_ads -> AdMob.updateConsent(this)
        }
        return true
    }

    private fun setUpOrientationIcons() {
        OrientationIdManager.list.forEach {
            val orientation = it.orientation
            val button = findViewById<View>(it.viewId)
            buttonList.add(Pair(orientation, button))
            button.setOnClickListener { setOrientation(orientation) }
        }
        setOrientationIcon()
        button_settings.visibility = View.GONE
    }

    private fun toggleStatus() {
        if (orientationHelper.isEnabled) {
            MainService.stop(this)
            if (settings.shouldResident()) {
                settings.setResident(false)
                setResidentCheckBox()
            }
        } else {
            MainService.start(this)
        }
    }

    private fun setStatusDescription() {
        val enabled = orientationHelper.isEnabled
        statusSwitch.isChecked = enabled
        statusDescription.setText(if (enabled) R.string.status_running else R.string.status_waiting)
    }

    private fun toggleResident() {
        settings.setResident(!settings.shouldResident())
        setResidentCheckBox()
        if (settings.shouldResident() && !orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setResidentCheckBox() {
        residentCheckBox.isChecked = settings.shouldResident()
    }

    private fun setOrientation(orientation: Int) {
        settings.orientation = orientation
        setOrientationIcon()
        if (orientationHelper.isEnabled) {
            MainService.start(this)
        }
    }

    private fun setOrientationIcon() {
        val orientation = settings.orientation
        val selected = ContextCompat.getColor(this, R.color.bg_notification_selected)
        val transparent = ContextCompat.getColor(this, android.R.color.transparent)
        for (pair in buttonList) {
            pair.second.run {
                setBackgroundColor(if (orientation == pair.first) selected else transparent)
            }
        }
    }

    private fun makeVersionInfo(): String {
        return BuildConfig.VERSION_NAME +
                if (BuildConfig.DEBUG)
                    " # " + DateFormat.format("yyyy/M/d kk:mm:ss", BuildConfig.BUILD_TIME)
                else ""
    }

    companion object {
        private const val PACKAGE_NAME = "net.mm2d.android.orientationfaker"
        private const val ACTION_UPDATE = "ACTION_UPDATE"
        private const val REQUEST_CODE = 101
        private const val PRIVACY_POLICY_URL =
            "https://github.com/ohmae/orientation-faker/blob/develop/PRIVACY-POLICY.md"
        private const val GITHUB_URL =
            "https://github.com/ohmae/orientation-faker/"

        fun notifyUpdate(context: Context) {
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(ACTION_UPDATE))
        }

        private fun openUri(context: Context, uri: String): Boolean {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w(e)
                return false
            }
            return true
        }

        private fun openCustomTabs(context: Context, uri: String): Boolean {
            try {
                val intent = CustomTabsIntent.Builder(CustomTabsHelper.session)
                    .setShowTitle(true)
                    .setToolbarColor(ContextCompat.getColor(context, R.color.primary))
                    .build()
                intent.intent.setPackage(CustomTabsHelper.packageNameToBind)
                intent.launchUrl(context, Uri.parse(uri))
            } catch (e: ActivityNotFoundException) {
                Log.w(e)
                return false
            }
            return true
        }

        private fun openGooglePlay(context: Context, packageName: String): Boolean {
            return openUri(context, "market://details?id=$packageName") ||
                    openCustomTabs(context, "https://play.google.com/store/apps/details?id=$packageName")
        }

        private fun openGooglePlay(context: Context): Boolean {
            return openGooglePlay(context, PACKAGE_NAME)
        }

        private fun openPrivacyPolicy(context: Context) {
            openCustomTabs(context, PRIVACY_POLICY_URL)
        }

        private fun openSourceCode(context: Context) {
            openCustomTabs(context, GITHUB_URL)
        }
    }
}
