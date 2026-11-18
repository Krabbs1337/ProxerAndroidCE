package me.proxer.app.ui.crash

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.extension.linkify
import me.proxer.app.databinding.ActivityCrashBinding

/**
 * @author Ruben Gees
 */
class CrashActivity : BaseActivity() {
    private lateinit var binding: ActivityCrashBinding
    private val config: CaocConfig
        get() =
            try {
                CustomActivityOnCrash.getConfigFromIntent(intent)
            } catch (ignored: Exception) {
                // Workaround for a bug in Caoc.
                CaocConfig()
            } ?: CaocConfig()

    private val errorDetails: String
        get() {
            val androidVersion = "Android version: ${Build.VERSION.RELEASE}\n"

            return androidVersion + CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        }

    private val toolbar: Toolbar get() = binding.toolbar
    private val text: TextView get() = binding.text
    private val report: Button get() = binding.report
    private val restart: Button get() = binding.restart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        title = getString(R.string.section_crash)

        report
            .clicks()
            .autoDispose(this.scope())
            .subscribe { CrashDialog.show(this, errorDetails) }

        restart
            .clicks()
            .autoDispose(this.scope())
            .subscribe { CustomActivityOnCrash.restartApplication(this, config) }

        text.text = getString(R.string.activity_crash_text).linkify(web = false)
    }
}
