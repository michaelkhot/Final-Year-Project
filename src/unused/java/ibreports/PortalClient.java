package gmail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Scanner;
import java.util.logging.Level;

import gmail.twofactorauth.HandlerFactory;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalClient {

    static final Logger logger = LoggerFactory.getLogger(PortalClient.class);

    private static final Path DRIVER_PATH = Paths.get("src/main/webdriver/chromedriver.exe");

    private static final int MAX_IMMEDIATE_ATTEMPTS = 1;
    private static final int LOGIN_PAGE_TIMEOUT = 15;
    private static final int OAUTH_TIMEOUT = 15;

    private static final String BASE_URL = "https://www.interactivebrokers.co.uk";
    private static final String LOGIN_PAGE = "/sso/Login";
    private static final String REPORTS_PAGE = "/AccountManagement/AmAuthentication?action=PORTFOLIOANALYST&tab=REPORTS";

    // Due to issues with System.in and Gradle, only one scanner must be used everywhere
    private Scanner scanner;
    private Scanner getInputScanner() {
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    private WebDriver driver;
    private boolean headless;

    private String username;
    private String password;

    public PortalClient() {
        this(DRIVER_PATH, true);
    }

    public PortalClient(boolean headless) {
        this(DRIVER_PATH, headless);
    }

    public PortalClient(Path driverPath, boolean headless) {
        System.setProperty("webdriver.chrome.driver", driverPath.toString());
        this.headless = headless;
        logger.debug("{} instantiated with headless option: {}", this.getClass().getSimpleName(), headless);
    }

    private WebDriver getDriver() {
        if (driver == null) {
            // Disable most selenium logging
            java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);

            ChromeOptions options = new ChromeOptions();
            // Disable ChromeDriver logging
            options.setLogLevel(ChromeDriverLogLevel.OFF);
            options.setHeadless(headless);

            driver = new ChromeDriver(options);
            logger.debug("Initialised new ChromeDriver");
        }
        return driver;
    }

    public boolean authenticate(HandlerFactory.Type handler) {
        logger.info("Beginning authentication stage");
        boolean success = false;

        JSONObject credentials = (new CredentialManager()).getCredentials(getInputScanner());
        String username = credentials.getString(CredentialManager.USERNAME_KEY);
        String password = credentials.getString(CredentialManager.PASSWORD_KEY);
        logger.debug("Username/password read from CredentialManager");

        try {
            WebDriver driver = getDriver();

            driver.get(BASE_URL + LOGIN_PAGE);
            logger.debug("Loading login page at {}{}", BASE_URL, LOGIN_PAGE);

            // Wait for page to load
            ExpectedCondition<WebElement> isLoaded = ExpectedConditions.presenceOfElementLocated(By.id("user_name"));
            new WebDriverWait(driver, Duration.ofSeconds(LOGIN_PAGE_TIMEOUT)).until(isLoaded);
            logger.debug("Login page finished loading");

            // Attempt to log in
            int immediateAttempts = 0;
            while (immediateAttempts < MAX_IMMEDIATE_ATTEMPTS) {
                immediateAttempts += 1;
                logger.debug("Login attempt #{}", immediateAttempts);

                // Get login form fields
                WebElement usernameElement = driver.findElement(By.id("user_name"));
                WebElement passwordElement = driver.findElement(By.id("password"));
                // Input credentials
                usernameElement.sendKeys(username);
                passwordElement.sendKeys(password);

                // Waiting not needed because clicks/keys are synchronous

                // Submit login form
                WebElement submitElement1 = driver.findElement(By.id("submitForm"));
                submitElement1.click();
                logger.debug("Submitting login form");

                // Wait for 2FA prompt
                ExpectedCondition<WebElement> twoFA = ExpectedConditions.presenceOfElementLocated(By.id("chlginput"));
                WebElement twoFAfield = new WebDriverWait(driver, Duration.ofSeconds(LOGIN_PAGE_TIMEOUT)).until(twoFA);

                // Call 2FA handler
                String twoFACode = (HandlerFactory.make(handler)).setScanner(scanner).getCode();
                twoFAfield.sendKeys(twoFACode);
                // Submit 2FA form
                WebElement submitElement2 = driver.findElement(By.id("submitForm"));
                ExpectedCondition<WebElement> submitClickable = ExpectedConditions.elementToBeClickable(submitElement2);
                new WebDriverWait(driver, Duration.ofSeconds(OAUTH_TIMEOUT)).until(submitClickable);
                submitElement2.click();

                success = true;
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout reached while waiting for authentication");
            success = false;
        } catch (Exception e) {
            System.out.println("Error occurred during authentication");
            e.printStackTrace();
            success = false;
        }


        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        driver.close();
        driver.quit();
        return success;
    }

    // cleanup function for scanner, driver etc.

}
