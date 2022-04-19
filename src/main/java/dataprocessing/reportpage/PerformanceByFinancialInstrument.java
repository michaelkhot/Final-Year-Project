package dataprocessing.reportpage;

import dataprocessing.ReportPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to process the 'Performance By Financial Instrument' page of a report CSV.
 */
public class PerformanceByFinancialInstrument implements ReportPageProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(AllocationByFinancialInstrument.class);

    @Override
    public String processDataSection(String dataCSV) {
        return dataCSV;
    }

}
