package dataprocessing.reportpage;

import dataprocessing.ReportPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to process the 'Allocation And Performance By Region' page of a report CSV.
 */
public class AllocationAndPerformanceByRegion implements ReportPageProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(BreakdownOfAccount.class);

    @Override
    public String processDataSection(String dataCSV) {
        return dataCSV;
    }

}
