package com.github.wasiqb.appium;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.openqa.selenium.OutputType.FILE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.imagecomparison.SimilarityMatchingOptions;
import io.appium.java_client.imagecomparison.SimilarityMatchingResult;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Capabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class VisualSimilarityTest {
    private static final String DEVICE_NAME_KEY    = "deviceName";
    private static final String DEVICE_VERSION_KEY = "deviceVersion";
    private static final String SCREEN_NAME        = "Home-page";
    private static final String USER_DIR           = System.getProperty ("user.dir");
    private static final double VISUAL_THRESHOLD   = 0.99;

    private AndroidDriver            driver;
    private boolean                  isBaseline;
    private AppiumDriverLocalService service;

    @BeforeClass (alwaysRun = true)
    @Parameters ({ "isBaseline", "appName" })
    public void setupSuite (@Optional ("false") boolean isBaseline, String appName) {
        this.isBaseline = isBaseline;
        this.service = buildAppiumService ();
        this.service.start ();
        this.driver = new AndroidDriver (this.service.getUrl (), buildCapabilities (appName));
    }

    @AfterClass (alwaysRun = true)
    public void teardownSuite () {
        this.driver.quit ();
        this.service.stop ();
    }

    @Test
    public void testAppVisual () throws IOException {
        checkVisual ();
    }

    private AppiumDriverLocalService buildAppiumService () {
        final var logFile = Path.of (USER_DIR, "logs", "appium.log")
            .toFile ();
        final var builder = new AppiumServiceBuilder ();
        return builder.withIPAddress (System.getProperty ("host", "127.0.0.1"))
            .usingPort (Integer.parseInt (System.getProperty ("port", "4723")))
            .withLogFile (logFile)
            .withArgument (GeneralServerFlag.BASEPATH, "/wd/hub")
            .withArgument (GeneralServerFlag.USE_DRIVERS, "uiautomator2")
            .withArgument (GeneralServerFlag.USE_PLUGINS, "all")
            .withArgument (GeneralServerFlag.SESSION_OVERRIDE)
            .withArgument (GeneralServerFlag.LOCAL_TIMEZONE)
            .build ();
    }

    private Capabilities buildCapabilities (final String appName) {
        final var deviceName = System.getProperty (DEVICE_NAME_KEY, "Pixel_6_Pro");
        final var deviceVersion = System.getProperty (DEVICE_VERSION_KEY, "11");
        final var options = new UiAutomator2Options ();
        options.setPlatformName ("Android")
            .setPlatformVersion (deviceVersion)
            .setDeviceName (deviceName)
            .setAvd (deviceName)
            .setApp (Path.of (USER_DIR, "src/test/resources", MessageFormat.format ("{0}.apk", appName))
                .toString ())
            .setAutoGrantPermissions (true)
            .setFullReset (true)
            .setCapability ("appium:settings[ignoreUnimportantViews]", true);
        return options;
    }

    private void checkVisual () throws IOException {
        final var baseImage = getBaseLineImage ();
        final var actualImage = getActualImage ();
        final var diffImage = getDiffImage (actualImage);
        final var score = getVisualScore (baseImage, actualImage, diffImage);

        assertWithMessage ("Visual result").that (score.getScore ())
            .isAtLeast (VISUAL_THRESHOLD);
    }

    private File getActualImage () throws IOException {
        final var actualImage = this.driver.getScreenshotAs (FILE);
        FileUtils.copyFile (actualImage, getFile ("actual"));
        return actualImage;
    }

    private File getBaseLineImage () throws IOException {
        final var baseImage = getFile ("baseline");
        if (isBaseline || !baseImage.exists ()) {
            final var newBaseline = this.driver.getScreenshotAs (FILE);
            FileUtils.copyFile (newBaseline, baseImage);
        }
        return baseImage;
    }

    private File getDiffImage (final File actualImage) throws IOException {
        final var diffImage = getFile ("diff");
        FileUtils.copyFile (actualImage, diffImage);
        return diffImage;
    }

    private File getFile (final String fileType) {
        return Path.of (USER_DIR, "images", fileType, MessageFormat.format ("{0}-similarity.png", SCREEN_NAME))
            .toFile ();
    }

    private SimilarityMatchingResult getVisualScore (final File baseImage, final File actualImage, final File diffImage)
        throws IOException {
        final var options = new SimilarityMatchingOptions ();
        options.withEnabledVisualization ();

        final var res = this.driver.getImagesSimilarity (baseImage, actualImage, options);
        res.storeVisualization (diffImage);

        return res;
    }
}
