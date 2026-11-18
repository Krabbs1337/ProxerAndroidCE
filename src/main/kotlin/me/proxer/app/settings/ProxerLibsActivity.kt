package me.proxer.app.settings

import android.os.Bundle
import androidx.fragment.app.commitNow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity

class ProxerLibsActivity : DrawerActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = intent.extras?.getString(LibsBuilder.BUNDLE_TITLE, "")

        // Explicit resource reference so the release resource shrinker keeps
        // R.raw.aboutlibraries (the default lookup via getIdentifier is invisible to it).
        val libs =
            Libs
                .Builder()
                .withJson(resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() })
                .build()

        val fragment =
            LibsBuilder()
                .withLibs(libs)
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
