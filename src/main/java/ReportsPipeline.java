import dataprocessing.ReportProcessor;
import gmail.GmailManager;
import misc.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sheets.SheetsManager;

import java.security.GeneralSecurityException;


public class ReportsPipeline {

    /* Identifier for this project when sending requests to Google APIs */
    private static final String APPLICATION_NAME = "Automated IB Report Pipeline";

    public static void main(String[] args) throws GeneralSecurityException {
        final Logger logger = LoggerFactory.getLogger(ReportsPipeline.class);
        logger.info("Beginning execution");

        // Generate report object from financial CSV reports sent to email
        Report rawReport = GmailManager.getLatestReport(APPLICATION_NAME);
        // Apply processing and calculations
        Report processedReport = ReportProcessor.processData(rawReport);
        // Upload report CSV to Google Sheets
        boolean success = SheetsManager.uploadReport(APPLICATION_NAME, processedReport);

        logger.info("Completed execution");
    }

}
