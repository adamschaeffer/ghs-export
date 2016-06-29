package prob.ghs;

/**
 * This class can be used if a particular field needs to be formatted specially due to its data type.
 * Originally created for date and time fields, it was later decided to format dates and times on the database side.
 * As a result, this serves no functional purpose, but remains in case a need arises in the future.
 * @author ASchaeffer
 *
 */

public enum Field_Types {date,time,text,datetime};
