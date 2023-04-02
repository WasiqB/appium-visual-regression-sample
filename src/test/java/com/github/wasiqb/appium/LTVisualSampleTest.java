package com.github.wasiqb.appium;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class LTVisualSampleTest {
    private static final String DEVICE_NAME_KEY    = "deviceName";
    private static final String DEVICE_VERSION_KEY = "deviceVersion";
    private static final String SCREEN_NAME        = "Home-page";

    private String        appUrl;
    private String        buildName;
    private AndroidDriver driver;
    private boolean       isBaseline;

    @BeforeClass (alwaysRun = true)
    @Parameters ({ "isBaseline", "buildName", "appUrl" })
    public void setupClass (@Optional ("false") final boolean isBaseline, final String buildName, final String appUrl)
        throws MalformedURLException {
        this.isBaseline = isBaseline;
        this.buildName = buildName;
        this.appUrl = appUrl;
        final var userName = System.getenv ("LT_USERNAME");
        final var accessKey = System.getenv ("LT_ACCESS_KEY");
        this.driver = new AndroidDriver (
            new URL (MessageFormat.format ("https://{0}:{1}@mobile-hub.lambdatest.com/wd/hub", userName, accessKey)),
            buildCapabilities ());
    }

    @AfterClass (alwaysRun = true)
    public void teardownClass () {
        this.driver.quit ();
    }

    @Test
    public void testAppVisual () {
        checkVisual ();
    }

    private Capabilities buildCapabilities () {
        final var deviceName = System.getProperty (DEVICE_NAME_KEY, "Pixel 5");
        final var deviceVersion = System.getProperty (DEVICE_VERSION_KEY, "11");
        final var options = new UiAutomator2Options ();

        final var ltOptions = new HashMap<String, Object> ();
        ltOptions.put ("w3c", true);
        ltOptions.put ("platformName", "Android");
        ltOptions.put ("deviceName", deviceName);
        ltOptions.put ("platformVersion", deviceVersion);
        ltOptions.put ("app", this.appUrl);
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
        ltOptions.put ("smartUI.build", this.buildName);
        ltOptions.put ("smartUI.baseline", this.isBaseline);

        options.setCapability ("lt:options", ltOptions);

        return options;
    }

    private void checkVisual () {
        this.driver.executeScript (MessageFormat.format ("smartui.takeScreenshot={0}", SCREEN_NAME));
    }
}
