package dataprocessing.reportpage;

import dataprocessing.ReportPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to process the 'Breakdown Of Account' page of a report CSV.
 */
public class BreakdownOfAccount implements ReportPageProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(BreakdownOfAccount.class);

    @Override
    public String processDataSection(String dataCSV) {
        return dataCSV;
    }

}
