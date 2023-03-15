package com.github.wasiqb.appium;

import static com.google.common.truth.Truth.assertWithMessage;
import static io.appium.java_client.AppiumBy.accessibilityId;
import static io.appium.java_client.service.local.flags.GeneralServerFlag.BASEPATH;
import static io.appium.java_client.service.local.flags.GeneralServerFlag.LOCAL_TIMEZONE;
import static io.appium.java_client.service.local.flags.GeneralServerFlag.SESSION_OVERRIDE;
import static io.appium.java_client.service.local.flags.GeneralServerFlag.USE_DRIVERS;
import static io.appium.java_client.service.local.flags.GeneralServerFlag.USE_PLUGINS;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.text.MessageFormat.format;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.openqa.selenium.OutputType.FILE;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.appmanagement.AndroidInstallApplicationOptions;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.imagecomparison.SimilarityMatchingOptions;
import io.appium.java_client.imagecomparison.SimilarityMatchingResult;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class VisualSampleTest {
    private static final String DEVICE_NAME_KEY    = "deviceName";
    private static final String DEVICE_VERSION_KEY = "deviceVersion";
    private static final String SCREEN_NAME        = "Home-page";
    private static final String USER_DIR           = getProperty ("user.dir");
    private static final double VISUAL_THRESHOLD   = 0.99;

    private AndroidDriver            driver;
    private AppiumDriverLocalService service;

    @BeforeSuite (alwaysRun = true)
    public void setupSuite () {
        this.service = buildAppiumService ();
        this.service.start ();
        this.driver = new AndroidDriver (this.service.getUrl (), buildCapabilities ());
    }

    @AfterSuite (alwaysRun = true)
    public void teardownSuite () {
        this.driver.quit ();
        this.service.stop ();
    }

    @Test
    public void testNewApp () throws IOException {
        this.driver.removeApp ("com.lambdatest.proverbial");
        this.driver.installApp (Path.of (USER_DIR, "src/test/resources/proverbial_new.apk")
            .toString (), new AndroidInstallApplicationOptions ().withGrantPermissionsEnabled ());
        this.driver.activateApp ("com.lambdatest.proverbial");

        final var wait = new WebDriverWait (this.driver, ofSeconds (10));
        wait.until (visibilityOfElementLocated (accessibilityId ("Location")));

        checkVisual ();
    }

    @Test
    public void testOldApp () throws IOException {
        checkVisual ();
    }

    private AppiumDriverLocalService buildAppiumService () {
        final var logFile = Path.of (USER_DIR, "logs", "appium.log")
            .toFile ();
        final var builder = new AppiumServiceBuilder ();
        return builder.withIPAddress (getProperty ("host", "127.0.0.1"))
            .usingPort (parseInt (getProperty ("port", "4723")))
            .withLogFile (logFile)
            .withArgument (BASEPATH, "/wd/hub")
            .withArgument (USE_DRIVERS, "uiautomator2")
            .withArgument (USE_PLUGINS, "all")
            .withArgument (SESSION_OVERRIDE)
            .withArgument (LOCAL_TIMEZONE)
            .build ();
    }

    private Capabilities buildCapabilities () {
        final var deviceName = getProperty (DEVICE_NAME_KEY, "Pixel_6_Pro");
        final var deviceVersion = getProperty (DEVICE_VERSION_KEY, "11");
        final var options = new UiAutomator2Options ();
        options.setPlatformName ("Android")
            .setPlatformVersion (deviceVersion)
            .setDeviceName (deviceName)
            .setAvd (deviceName)
            .setApp (Path.of (USER_DIR, "src/test/resources/proverbial_old.apk")
                .toString ())
            .setAutoGrantPermissions (true)
            .setFullReset (true)
            .setIsHeadless (parseBoolean (getProperty ("headless", "false")))
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
        copyFile (actualImage, getFile ("actual"));
        return actualImage;
    }

    private File getBaseLineImage () throws IOException {
        final var baseImage = getFile ("baseline");
        if (!baseImage.exists ()) {
            final var newBaseline = this.driver.getScreenshotAs (FILE);
            copyFile (newBaseline, baseImage);
        }
        return baseImage;
    }

    private File getDiffImage (final File actualImage) throws IOException {
        final var diffImage = getFile ("diff");
        copyFile (actualImage, diffImage);
        return diffImage;
    }

    private File getFile (final String fileType) {
        return Path.of (USER_DIR, "images", fileType, format ("{0}.png", SCREEN_NAME))
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
