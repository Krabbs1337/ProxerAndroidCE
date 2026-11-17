package me.proxer.app.introduction

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import me.proxer.app.R

/**
 * First-run introduction. Replaces the abandoned, support-library-based
 * `com.github.rubengees:introduction` library with a plain ViewPager2 implementation.
 */
class IntroductionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_introduction)

        val slides =
            mutableListOf(
                IntroductionSlideFragment.newInstance(
                    R.string.introduction_welcome_title,
                    R.string.introduction_welcome_description,
                    IntroductionSlideFragment.Icon.LOGO,
                    IntroductionSlideFragment.SwitchMode.NONE,
                ),
                IntroductionSlideFragment.newInstance(
                    R.string.introduction_notifications_title,
                    R.string.introduction_notifications_description,
                    IntroductionSlideFragment.Icon.NOTIFICATIONS,
                    IntroductionSlideFragment.SwitchMode.NOTIFICATIONS,
                ),
            )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            slides +=
                IntroductionSlideFragment.newInstance(
                    R.string.introduction_theme_title,
                    R.string.introduction_theme_description,
                    IntroductionSlideFragment.Icon.THEME,
                    IntroductionSlideFragment.SwitchMode.THEME,
                )
        }

        val pager = findViewById<ViewPager2>(R.id.pager)
        val skipButton = findViewById<Button>(R.id.skipButton)
        val doneButton = findViewById<Button>(R.id.doneButton)

        pager.adapter = SlideAdapter(this, slides)

        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    doneButton.visibility = if (position == slides.size - 1) View.VISIBLE else View.GONE
                }
            },
        )

        skipButton.setOnClickListener { finish() }
        doneButton.setOnClickListener { finish() }
    }

    private class SlideAdapter(
        activity: FragmentActivity,
        private val slides: List<Fragment>,
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount() = slides.size

        override fun createFragment(position: Int) = slides[position]
    }
}
