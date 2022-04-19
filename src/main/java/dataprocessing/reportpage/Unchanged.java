package dataprocessing.reportpage;

import dataprocessing.ReportPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Does nothing and returns an unchanged CSV.
 */
public class Unchanged implements ReportPageProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(BreakdownOfAccount.class);

    @Override
    public String processDataSection(String dataCSV) {
        return dataCSV;
    }

}
