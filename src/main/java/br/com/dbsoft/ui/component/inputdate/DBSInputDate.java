package br.com.dbsoft.ui.component.inputdate;

import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.faces.component.FacesComponent;

import br.com.dbsoft.ui.component.DBSUIInput;
import br.com.dbsoft.ui.core.DBSFaces;
import br.com.dbsoft.util.DBSDate;
import br.com.dbsoft.util.DBSFormat;
import br.com.dbsoft.util.DBSObject;
import br.com.dbsoft.util.DBSString;

@FacesComponent(DBSInputDate.COMPONENT_TYPE)
public class DBSInputDate extends DBSUIInput {

	public final static String COMPONENT_TYPE = DBSFaces.DOMAIN_UI_COMPONENT + "." + DBSFaces.ID.INPUTDATE;
	public final static String RENDERER_TYPE = COMPONENT_TYPE;
	public static class TYPE{
		public static String DATE = "date";
		public static String TIME = "time";
		public static String DATETIME = "datetime";
		public static String TIMES = "times";
	}
	protected enum PropertyKeys {
		type,
		autocomplete,
		dateMax,
		dateMin,
		holidays;

		String toString;

		PropertyKeys(String toString) {
			this.toString = toString;
		}

		PropertyKeys() {}

		@Override
		public String toString() {
			return ((this.toString != null) ? this.toString : super.toString());
		}
	}
	
	public void setHolidays(Object pHolidays) {
		getStateHelper().put(PropertyKeys.holidays, pHolidays);
		handleAttribute("holidays", pHolidays);
	}
	public Object getHolidays() {
		return getStateHelper().eval(PropertyKeys.holidays, null);
	}

	public List<String> getHolidayDatesIso() {
		LinkedHashSet<String> xValues = new LinkedHashSet<String>();
		pvCollectHolidayDates(getHolidays(), xValues);
		return new ArrayList<String>(xValues);
	}

	public String getHolidayDatesAttribute() {
		List<String> xHolidays = getHolidayDatesIso();
		if (xHolidays.isEmpty()){
			return null;
		}
		return DBSString.listToCSV(xHolidays);
	}

	
    public DBSInputDate(){
		setRendererType(DBSInputDate.RENDERER_TYPE);
    }
	
	public String getType() {
		return (String) getStateHelper().eval(PropertyKeys.type, TYPE.DATE);
	}
	
	public void setType(String pType) {
		getStateHelper().put(PropertyKeys.type, pType);
		handleAttribute("type", pType);
	}

	
	public void setAutocomplete(String pAutocomplete) {
		getStateHelper().put(PropertyKeys.autocomplete, pAutocomplete);
		handleAttribute("autocomplete", pAutocomplete);
	}
	public String getAutocomplete() {
		return (String) getStateHelper().eval(PropertyKeys.autocomplete, "off");
	}	

	public Date getDateMin() {
		return (Date) getStateHelper().eval(PropertyKeys.dateMin, null);
	}
	public void setDateMin(Date pDateMin) {
		getStateHelper().put(PropertyKeys.dateMin, pDateMin);
		handleAttribute("dateMin", pDateMin);
	}

	public Date getDateMax() {
		return (Date) getStateHelper().eval(PropertyKeys.dateMax, null);
	}
	public void setDateMax(Date pDateMax) {
		getStateHelper().put(PropertyKeys.dateMax, pDateMax);
		handleAttribute("dateMax", pDateMax);
	}
	
	@Override
	public Object getValue(){
		if (this.getType().equals(DBSInputDate.TYPE.DATE)){
			return getDate();
		}else if (this.getType().equals(DBSInputDate.TYPE.TIME)){
			return getTime();
		}else if (this.getType().equals(DBSInputDate.TYPE.TIMES)){
			return getTimes();
		}else if (this.getType().equals(DBSInputDate.TYPE.DATETIME)){
			return getTimestamp();
		}else{
			return super.getValue();
		}
	}
	
	public String getDay(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getDay(getDate()));
	}
	public String getMonth(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getMonth(getDate()));
	}
	public String getYear(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getYear(getDate()));
	}
	public String getHour(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getHour(getTimestamp()));
	}
	public String getMinute(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getMinute(getTimestamp()));
	}
	public String getSecond(){
		if (super.getValue()==null){
			return "";
		}
		return String.format("%02d",DBSDate.getSecond(getTimestamp()));
	}
	
	public Date getDate(){
		return DBSDate.toDate(super.getValue());
	}

	public Time getTime(){
		return DBSDate.toTime(getHour(), getMinute(), "0");
	}

	public Time getTimes(){
		return DBSDate.toTime(getHour(), getMinute(), getSecond());
	}

	public Timestamp getTimestamp(){
		return DBSDate.toTimestamp(super.getValue());
	}
	

	private void pvCollectHolidayDates(Object pValue, Collection<String> pTarget) {
		if (pValue == null || pTarget == null){
			return;
		}
		if (pValue instanceof Collection<?>){
			for (Object xItem : (Collection<?>) pValue){
				pvCollectHolidayDates(xItem, pTarget);
			}
			return;
		}
		if (pValue.getClass().isArray()){
			int xLength = Array.getLength(pValue);
			for (int xI = 0; xI < xLength; xI++){
				pvCollectHolidayDates(Array.get(pValue, xI), pTarget);
			}
			return;
		}
		if (pValue instanceof CharSequence){
			pvCollectHolidayDatesFromString(pValue.toString(), pTarget);
			return;
		}
		Date xDate = pvToDate(pValue);
		if (xDate != null){
			pTarget.add(DBSFormat.getFormattedDateCustom(xDate, "yyyy-MM-dd"));
		}
	}

	private void pvCollectHolidayDatesFromString(String pValue, Collection<String> pTarget){
		if (DBSObject.isEmpty(pValue) || pTarget == null){
			return;
		}
		String xClean = pValue.trim();
		if (xClean.startsWith("[") && xClean.endsWith("]")){
			xClean = xClean.substring(1, xClean.length() - 1);
		}
		xClean = xClean.replace("\n", ",").replace("\r", ",");
		if (DBSObject.isEmpty(xClean)){
			return;
		}
		Date xDate = pvToDate(xClean);
		if (xDate != null){
			pTarget.add(DBSFormat.getFormattedDateCustom(xDate, "yyyy-MM-dd"));
			return;
		}
		String[] xParts = xClean.split("[,;]");
		if (xParts.length > 1){
			for (String xPart : xParts){
				pvCollectHolidayDatesFromString(xPart, pTarget);
			}
		}
	}

	private Date pvToDate(Object pValue){
		if (pValue == null){
			return null;
		}
		if (pValue instanceof Date){
			return (Date) pValue;
		}
		if (pValue instanceof java.util.Date){
			return DBSDate.toDate((java.util.Date) pValue);
		}
		if (pValue instanceof Long){
			return DBSDate.toDate((Long) pValue);
		}
		if (pValue instanceof java.util.Calendar){
			return DBSDate.toDate((java.util.Calendar) pValue);
		}
		return DBSDate.toDate(pValue);
	}

}
