package com.github.rmannibucau.rblog.test;

import lombok.NoArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
final class WebDriverFactory {
    static WebDriver createDriver() {
        return new ChromeDriver(
            ChromeDriverService.createDefaultService(),
            DesiredCapabilities.chrome());
    }
}
