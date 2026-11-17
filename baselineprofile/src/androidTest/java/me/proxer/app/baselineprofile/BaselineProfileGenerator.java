package me.proxer.app.baselineprofile;

import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import kotlin.Unit;

/**
 * Generates the baseline profile for the app (root project). Run on a device/emulator with:
 * ./gradlew :baselineprofile:connectedReleaseAndroidTest
 * (or the Gradle task :baselineprofile:generateReleaseBaselineProfile), then the profile is
 * picked up by :mergeReleaseArtProfile / :packageRelease via AGP.
 */
@RunWith(AndroidJUnit4.class)
public class BaselineProfileGenerator {

    @Rule
    public BaselineProfileRule baselineProfileRule = new BaselineProfileRule();

    @Test
    public void generate() {
        baselineProfileRule.collect("me.proxer.app", 15, scope -> {
            scope.startActivityAndWait();
            return Unit.INSTANCE;
        });
    }
}
