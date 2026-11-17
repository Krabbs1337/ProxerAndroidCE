package me.proxer.app.settings

import android.os.Bundle
import androidx.fragment.app.commitNow
import com.mikepenz.aboutlibraries.LibsBuilder
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity

class ProxerLibsActivity : DrawerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = intent.extras?.getString(LibsBuilder.BUNDLE_TITLE, "")

        val fragment =
            LibsBuilder()
                .withShowLoadingProgress(false)
                .withAboutVersionShown(false)
                .withAboutIconShown(false)
                .withVersionShown(false)
                .supportFragment()

        supportFragmentManager.commitNow {
            replace(R.id.container, fragment)
        }
    }
}
