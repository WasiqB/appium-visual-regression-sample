package com.github.wasiqb.appium;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.text.MessageFormat.format;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class LTVisualSampleTest {
    private static final String DEVICE_NAME_KEY    = "deviceName";
    private static final String DEVICE_VERSION_KEY = "deviceVersion";
    private static final String SCREEN_NAME        = "Home-page";
    private static final String USER_DIR           = getProperty ("user.dir");

    private AndroidDriver driver;

    @BeforeSuite (alwaysRun = true)
    public void setupSuite () throws MalformedURLException {
        final var userName = getenv ("LT_USERNAME");
        final var accessKey = getenv ("LT_ACCESS_KEY");
        this.driver = new AndroidDriver (
            new URL (format ("https://{0}:{1}@mobile-hub.lambdatest.com/wd/hub", userName, accessKey)),
            buildCapabilities ());
    }

    @AfterSuite (alwaysRun = true)
    public void teardownSuite () {
        this.driver.quit ();
    }

    @Test
    public void testAppVisual () {
        checkVisual ();
    }

    private Capabilities buildCapabilities () {
        final var deviceName = getProperty (DEVICE_NAME_KEY, "Pixel 5");
        final var deviceVersion = getProperty (DEVICE_VERSION_KEY, "11");
        final var buildName = getProperty ("buildName");
        final var isBaseline = parseBoolean (getProperty ("isBaseline", "false"));
        final var options = new UiAutomator2Options ();

        final var ltOptions = new HashMap<String, Object> ();
        ltOptions.put ("w3c", true);
        ltOptions.put ("platformName", "Android");
        ltOptions.put ("deviceName", deviceName);
        ltOptions.put ("platformVersion", deviceVersion);
        ltOptions.put ("app", getenv ("LT_APP"));
        ltOptions.put ("devicelog", true);
        ltOptions.put ("visual", true);
        ltOptions.put ("network", true);
        ltOptions.put ("video", true);
        ltOptions.put ("build", "Sample Build");
        ltOptions.put ("name", "Sample Test");
        ltOptions.put ("project", "Sample Project");
        ltOptions.put ("isRealMobile", true);
        ltOptions.put ("autoGrantPermissions", true);
        ltOptions.put ("smartUI.project", "Sample Visual Regression");
        ltOptions.put ("smartUI.build", buildName);
        ltOptions.put ("smartUI.baseline", isBaseline);

        options.setCapability ("lt:options", ltOptions);

        return options;
    }

    private void checkVisual () {
        this.driver.executeScript (format ("smartui.takeScreenshot={0}", SCREEN_NAME));
    }
}
