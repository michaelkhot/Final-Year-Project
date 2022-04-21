package dataprocessing;

import misc.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * A class of static methods that handle the processing of report CSVs.
 */
public class ReportProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(ReportProcessor.class);

    /**
     * Processes the report by applying calculations to each section (report page).
     *
     * @param report The report with a CSV to process.
     * @return The report with processing/calculations applied.
     */
    public static Report processData(Report report) {
        StringBuilder newCSV = new StringBuilder();

        String name = report.getName();
        Report.ReportType type = report.getType();
        String dataCSV = report.getCSV();

        Map<String, String> pageMap = getPages(dataCSV);
        for (Map.Entry<String, String> reportPage : pageMap.entrySet()) {
            // Get name and data of the current report page
            String category = reportPage.getKey();
            String data = reportPage.getValue();

            if (category.equals("")) continue;

            // Process the data for the current report page
            logger.info("Processing section: '{}'", category);
            String processedData = (ProcessingFactory.getHandler(category))
                    .processDataSection(data);

            // Append processed report page to new CSV string
            newCSV.append(reconstructFormat(category, processedData));
        }

        report.setCSV(newCSV.toString());
        logger.info("Finished processing report: '{}'", name);
        return report;
    }

    /**
     * Reads and returns the report pages (sections) from a report CSV.
     *
     * @param reportCSV The CSV to read report pages from.
     * @return A HashMap of report page name to report page data.
     * Data is a string of the form: cells delimited by ',' and rows delimited by '\n'.
     */
    public static Map<String, String> getPages(String reportCSV) {
        // LinkedHashMap used to maintain order of report pages
        Map<String, String> pages = new LinkedHashMap<>();

        logger.debug("Parsing report pages from CSV");
        List<String> rows = new ArrayList<>(Arrays.asList(reportCSV.split("\n")));
        for (String row : rows) {
            List<String> cells = new ArrayList<>(Arrays.asList(row.split(",")));

            // Get report page type and its data
            String category = cells.remove(0);
            String data = String.join(",", cells).concat("\n");

            // Update HashMap entry
            String existing = pages.get(category);
            pages.put(category, existing == null ? data : existing + data);
        }

        return pages;
    }

    /**
     * Inserts the report page name at the start of each row (delimited by '\n').
     *
     * @param reportPageName The name of the report page.
     * @param data The string CSV of data of the form: cells delimited by ',', and rows delimited by '\n'.
     * @return A new string with each row's first cell being the report page name.
     */
    private static String reconstructFormat(String reportPageName, String data) {
        StringBuilder newData = new StringBuilder();

        List<String> rows = new ArrayList<>(Arrays.asList(data.split("\n")));
        for (String row : rows) {
            newData.append(reportPageName).append(',').append(row).append('\n');
        }

        return newData.toString();
    }

}
