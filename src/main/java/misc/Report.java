package misc;

/**
 * Class for reports, storing the report date and the report CSV
 */
public class Report {

    /**
     * Enum for types of reports. Currently, only represents general DEFAULT and CUSTOM reports.
     * Can be extended with specific types of default/custom reports for better flexibility in the data processing stage.
     */
    public enum ReportType {
        UNKNOWN,
        DEFAULT,
        CUSTOM,
    }

    /* Format of date attribute */
    public static final String DATE_FORMAT = "dd/MM/yyyy";

    /* Attributes */
    private final String name;
    private final String date;
    private ReportType type;
    private String csv;

    /**
     * Constructor for new Report object.
     *
     * @param date The date of the report.
     * @param csv The report CSV data in bytes.
     */
    public Report(String name, ReportType type, String date, String csv) {
        this.name = name;
        this.type = type;
        this.date = date;
        this.csv = csv;
    }

    /* Setter methods */

    public void setType(ReportType type) {
        this.type = type;
    }

    public void setCSV(String csv) {
        this.csv = csv;
    }

    /* Getter methods */

    public String getName() {
        return this.name;
    }

    public ReportType getType() {
        return this.type;
    }

    public String getCSV() {
        return this.csv;
    }

    /* Methods to return report date in various formats */

    public String getEntireDate() {
        return this.date;
    }

    public String getMonthYear() {
        return this.date.substring(this.date.indexOf('/') + 1);
    }

    public String getMonth() {
        return this.date.substring(this.date.indexOf('/') + 1, this.date.lastIndexOf('/'));
    }

    public String getYear() {
        return this.date.substring(this.date.lastIndexOf('/') + 1);
    }

}
