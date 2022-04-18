package dataprocessing;


public interface ReportPageProcessor {

    /**
     * This method can be implemented with many handlers for report pages.
     *
     * @param dataCSV A subset of CSV data to be processed.
     * @return A copy of the data after being processed.
     */
    String processDataSection(String dataCSV);

}
