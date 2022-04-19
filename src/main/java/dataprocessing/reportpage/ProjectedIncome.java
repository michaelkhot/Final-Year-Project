package dataprocessing.reportpage;

import dataprocessing.ReportPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to process the 'Projected Income' page of a report CSV.
 */
public class ProjectedIncome implements ReportPageProcessor {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(AllocationByFinancialInstrument.class);

    @Override
    public String processDataSection(String dataCSV) {
        return dataCSV;
    }

}
