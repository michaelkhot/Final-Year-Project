package dataprocessing;

import dataprocessing.reportpage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory for report page CSV handlers. To add a new handler, add a new case with the name of the section in
 * uppercase with whitespaces replaced with underscores.
 */
public class ProcessingFactory {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(ProcessingFactory.class);

    /**
     * Factory that produces new report page handlers depending on the type of report page.
     *
     * @param type The type of report page.
     * @return The processing handler for that specific report page.
     */
    public static ReportPageProcessor getHandler(String type) {
        type = type.replaceAll(" ", "_").toUpperCase();
        switch (type) {
            case "BREAKDOWN_OF_ACCOUNTS":
                return new BreakdownOfAccount();
            case "ACCOUNT_OVERVIEW":
                return new AccountOverview();
            case "HISTORICAL_PERFORMANCE":
                return new HistoricalPerformance();
            case "HISTORICAL_PERFORMANCE_BENCHMARK_COMPARISON":
                return new HistoricalPerformanceBenchmarkComparison();
            case "OPEN_POSITION_SUMMARY":
                return new OpenPositionSummary();
            case "CONCENTRATION":
                return new Concentration();
            case "ESG":
                return new ESG();
            case "ALLOCATION_BY_ASSET_CLASS":
                return new AllocationByAssetClass();
            case "ALLOCATION_BY_FINANCIAL_INSTRUMENT":
                return new AllocationByFinancialInstrument();
            case "ALLOCATION_AND_PERFORMANCE_BY_REGION":
                return new AllocationAndPerformanceByRegion();
            case "ALLOCATION_AND_PERFORMANCE_BY_SECTOR":
                return new AllocationAndPerformanceBySector();
            case "TIME_PERIOD_PERFORMANCE_STATISTICS":
                return new TimePeriodPerformanceStatistics();
            case "TIME_PERIOD_BENCHMARK_COMPARISON":
                return new TimePeriodBenchmarkComparison();
            case "CUMULATIVE_PERFORMANCE_STATISTICS":
                return new CumulativePerformanceStatistics();
            case "CUMULATIVE_BENCHMARK_STATISTICS":
                return new CumulativeBenchmarkStatistics();
            case "RISK_MEASURES":
                return new RiskMeasures();
            case "RISK_MEASURES_BENCHMARK_COMPARISON":
                return new RiskMeasuresBenchmarkComparison();
            case "PERFORMANCE_ATTRIBUTION_VS_BENCHMARK":
                return new PerformanceAttributionVsBenchmark();
            case "PERFORMANCE_BY_ASSET_CLASS":
                return new PerformanceByAssetClass();
            case "PERFORMANCE_BY_FINANCIAL_INSTRUMENT":
                return new PerformanceByFinancialInstrument();
            case "PERFORMANCE_BY_SYMBOL":
                return new PerformanceBySymbol();
            case "PERFORMANCE_BY_LONG_AND_SHORT":
                return new PerformanceByLongAndShort();
            case "PERFORMANCE_BY_UNDERLYING":
                return new PerformanceByUnderlying();
            case "FIXED_INCOME":
                return new FixedIncome();
            case "PROJECTED_INCOME":
                return new ProjectedIncome();
            case "TRADE_SUMMARY":
                return new TradeSummary();
            case "DEPOSITS_AND_WITHDRAWALS":
                return new DepositsAndWithdrawals();
            case "CORPORATE_ACTIONS":
                return new CorporateActions();
            case "DIVIDENDS":
                return new Dividends();
            case "INTEREST":
                return new Interest();
            case "FEES":
                return new Fees();
            default:
                logger.warn("No handler exists for {}", type);
                return new Unchanged();
        }
    }

}
