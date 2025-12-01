package br.com.dbsoft.ui.component.inputdate;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

import br.com.dbsoft.ui.component.DBSRenderer;
import br.com.dbsoft.ui.core.DBSFaces;
import br.com.dbsoft.ui.core.DBSFaces.CSS;
import br.com.dbsoft.util.DBSDate;
import br.com.dbsoft.util.DBSFormat;
import br.com.dbsoft.util.DBSObject;
import br.com.dbsoft.util.DBSString;

@FacesRenderer(componentFamily=DBSFaces.FAMILY, rendererType=DBSInputDate.RENDERER_TYPE)
public class DBSInputDateRenderer extends DBSRenderer {
	
    @Override
	public void decode(FacesContext pContext, UIComponent pComponent) {
    	DBSInputDate xInputDate = (DBSInputDate) pComponent;
        if(xInputDate.getReadOnly()) {return;}

    	decodeBehaviors(pContext, xInputDate);
    	
    	ExternalContext xEC = pContext.getExternalContext();
		String xClientIdAction = getInputDataClientId(xInputDate);
		Map<String, String> xParams = xEC.getRequestParameterMap();
		boolean xHasDateParam = xParams.containsKey(xClientIdAction + CSS.INPUTDATE.DAY.trim());
		boolean xHasTimeParam = xParams.containsKey(xClientIdAction + CSS.INPUTDATE.HOUR.trim());
		if (!xHasDateParam && !xHasTimeParam){
			return;
		}

		String xDate = null;
		String[] xTimeParts = null;

		if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATE) ||
			xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
			xDate = pvDecodeDateValue(xParams, xClientIdAction);
		}
		if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIME) ||
			xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIMES) ||
			xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
			xTimeParts = pvDecodeTimeValue(xParams, xClientIdAction);
		}
		
		if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATE)){
			if (xDate == null){
				xInputDate.setSubmittedValue("");
			}else{
				xInputDate.setSubmittedValue(DBSDate.toDate(xDate));
			}
		}else if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIME)){
			if (xTimeParts == null){
				xInputDate.setSubmittedValue("");
			}else{
				xInputDate.setSubmittedValue(DBSDate.toTime(xTimeParts[0], xTimeParts[1], "00"));
			}
		}else if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIMES)){
			if (xTimeParts == null){
				xInputDate.setSubmittedValue("");
			}else{
				xInputDate.setSubmittedValue(DBSDate.toTime(xTimeParts[0], xTimeParts[1], xTimeParts[2]));
			}
		}else if (xInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
   			if (xDate == null ||
   				xTimeParts == null){
   				xInputDate.setSubmittedValue("");	
			}else{
				xInputDate.setSubmittedValue(DBSDate.toTimestampYMDHMS(xDate + " " + pvJoinTime(xTimeParts)));
			}
		}
	}	
	
	@Override
	public void encodeBegin(FacesContext pContext, UIComponent pComponent)
			throws IOException {
		if (!pComponent.isRendered()){return;}
		DBSInputDate xInputDate = (DBSInputDate) pComponent;
		ResponseWriter xWriter = pContext.getResponseWriter();
		String xClientId = xInputDate.getClientId(pContext);
		String xClass = CSS.INPUTDATE.MAIN + CSS.THEME.INPUT;
		if (xInputDate.getStyleClass()!=null){
			xClass += xInputDate.getStyleClass();
		}
		xWriter.startElement("div", xInputDate);
			DBSFaces.encodeAttribute(xWriter, "id", xClientId);
			DBSFaces.encodeAttribute(xWriter, "name", xClientId);
			DBSFaces.encodeAttribute(xWriter, "class", xClass);
			DBSFaces.encodeAttribute(xWriter, "style", xInputDate.getStyle());
			//Container
			xWriter.startElement("div", xInputDate);
				DBSFaces.encodeAttribute(xWriter, "class", CSS.MODIFIER.CONTAINER);

				DBSFaces.encodeLabel(pContext, xInputDate, xWriter);
				pvEncodeInput(pContext, xInputDate, xWriter);
				DBSFaces.encodeRightLabel(pContext, xInputDate, xWriter);
//				encodeClientBehaviors(pContext, xInputDate);

			xWriter.endElement("div");
			DBSFaces.encodeTooltip(pContext, xInputDate, xInputDate.getTooltip());

			if (!xInputDate.getReadOnly()){
				DBSFaces.encodeJavaScriptTagStart(pComponent, xWriter);
				String xJS = "$(document).ready(function() { \n" +
						     " var xInputDateId = dbsfaces.util.jsid('" + xClientId + "'); \n " + 
						     " dbs_inputDate(xInputDateId); \n" +
		                     "}); \n"; 
				xWriter.write(xJS);
				DBSFaces.encodeJavaScriptTagEnd(xWriter);
			}
			xWriter.endElement("div");
	}
	
	private void pvEncodeInput(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		String 	xClientId = getInputDataClientId(pInputDate);
		String 	xStyle = "";
		String 	xStyleClass = "";
		String 	xValue = "";
		Integer xSize = 0;
		if (pInputDate.getDate() != null){
			if ((pInputDate.getDateMin() != null 
			  && pInputDate.getDate().before(pInputDate.getDateMin()))
			 || (pInputDate.getDateMax() != null 
			  && pInputDate.getDate().after(pInputDate.getDateMax()))){
				xStyleClass = CSS.MODIFIER.ERROR;
			}
		}		
		if (pInputDate.getReadOnly()){
			if (pInputDate.getDate()==null){
				if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATE)){
					xSize = 10;
					xStyle =  DBSFaces.getCSSStyleWidthFromInputSize(10);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIME)){
					xSize = 5;
					xStyle =  DBSFaces.getCSSStyleWidthFromInputSize(5);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIMES)){
					xSize = 8;
					xStyle =  DBSFaces.getCSSStyleWidthFromInputSize(8);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
					xSize = 16;
					xStyle =  DBSFaces.getCSSStyleWidthFromInputSize(16);
				}else{
					wLogger.error(xClientId + ":type inválido " + pInputDate.getType());
				}
			}else{
				if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATE)){
					xValue = DBSFormat.getFormattedDate(pInputDate.getDate());
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIME)){
					xValue = DBSFormat.getFormattedTime(pInputDate.getDate());
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIMES)){
					xValue = DBSFormat.getFormattedTimes(pInputDate.getDate());
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
					xValue = DBSFormat.getFormattedDateTimes(pInputDate.getDate());
				}else{
					wLogger.error(xClientId + ":type inválido " + pInputDate.getType());
				}
				xSize = xValue.length();
			}
			
			DBSFaces.encodeInputDataReadOnly(pInputDate, pWriter, xClientId, false, xValue, xSize, null, xStyle);
		}else{
			pWriter.startElement("div", pInputDate);
				DBSFaces.encodeAttribute(pWriter, "class", DBSFaces.getInputDataClass(pInputDate) + xStyleClass);
				DBSFaces.setSizeAttributes(pWriter, xSize, null);
				if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATE)){
					pvEncodeInputDate(pContext, pInputDate, pWriter);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIME)){
					pvEncodeInputTime(pContext, pInputDate, pWriter);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.TIMES)){
					pvEncodeInputTimes(pContext, pInputDate, pWriter);
				}else if (pInputDate.getType().equalsIgnoreCase(DBSInputDate.TYPE.DATETIME)){
					pvEncodeInputDateTime(pContext, pInputDate, pWriter);
				}else{
					wLogger.error(xClientId + ":type inválido [" + pInputDate.getType() + "]");
				}
			pWriter.endElement("div");
		}
	}

	private void pvEncodeInputDate(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		String xClientId = getInputDataClientId(pInputDate);
		pWriter.startElement("input", pInputDate);
			DBSFaces.encodeAttribute(pWriter, "id", xClientId + CSS.INPUTDATE.DAY.trim());
			DBSFaces.encodeAttribute(pWriter, "name", xClientId + CSS.INPUTDATE.DAY.trim());
			DBSFaces.encodeAttribute(pWriter, "type", "date");
			DBSFaces.encodeAttribute(pWriter, "class", CSS.INPUTDATE.DAY);
			DBSFaces.encodeAttribute(pWriter, "placeHolder", pInputDate.getPlaceHolder());
			DBSFaces.encodeAttribute(pWriter, "value", pvGetDateValue(pInputDate), "");
			DBSFaces.encodeAttribute(pWriter, "min", pvGetDateLimit(pInputDate.getDateMin()), "");
			DBSFaces.encodeAttribute(pWriter, "max", pvGetDateLimit(pInputDate.getDateMax()), "");
			pvEncodeHolidayAttribute(pInputDate, pWriter);
			pvEncodeAutocompleteAttribute(pInputDate, pWriter);			
			encodeClientBehaviors(pContext, pInputDate);
		pWriter.endElement("input");
	}
	
	private void pvEncodeInputTime(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		pvEncodeTimeInput(pContext, pInputDate, pWriter, false);
	}

	private void pvEncodeInputTimes(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		pvEncodeTimeInput(pContext, pInputDate, pWriter, true);
	}

	private void pvEncodeInputDateTime(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		pvEncodeInputDate(pContext, pInputDate, pWriter);
		pvEncodeInputTime(pContext, pInputDate, pWriter);
	}

	private void pvEncodeHolidayAttribute(DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		String xHolidays = pInputDate.getHolidayDatesAttribute();
		if (!DBSObject.isEmpty(xHolidays)){
			DBSFaces.encodeAttribute(pWriter, "data-holidays", xHolidays);
		}
	}
	
	private void pvEncodeAutocompleteAttribute(DBSInputDate pInputDate, ResponseWriter pWriter) throws IOException{
		if (pInputDate.getAutocomplete().equalsIgnoreCase("off") ||
			pInputDate.getAutocomplete().equalsIgnoreCase("false")){
			DBSFaces.encodeAttribute(pWriter, "autocomplete", "off");
		}			
	}

	private String pvGetDateValue(DBSInputDate pInputDate){
		if (pInputDate.getDate() == null){
			return "";
		}
		return String.format("%04d-%02d-%02d", DBSDate.getYear(pInputDate.getDate()), DBSDate.getMonth(pInputDate.getDate()), DBSDate.getDay(pInputDate.getDate()));
	}

	private String pvGetDateLimit(java.sql.Date pDate){
		if (pDate == null){
			return "";
		}
		return String.format("%04d-%02d-%02d", DBSDate.getYear(pDate), DBSDate.getMonth(pDate), DBSDate.getDay(pDate));
	}

	private void pvEncodeTimeInput(FacesContext pContext, DBSInputDate pInputDate, ResponseWriter pWriter, boolean pIncludeSeconds) throws IOException{
		String xClientId = getInputDataClientId(pInputDate);
		pWriter.startElement("input", pInputDate);
			DBSFaces.encodeAttribute(pWriter, "id", xClientId + CSS.INPUTDATE.HOUR.trim());
			DBSFaces.encodeAttribute(pWriter, "name", xClientId + CSS.INPUTDATE.HOUR.trim());
			DBSFaces.encodeAttribute(pWriter, "type", "time");
			DBSFaces.encodeAttribute(pWriter, "class", CSS.INPUTDATE.HOUR);
			DBSFaces.encodeAttribute(pWriter, "step", pIncludeSeconds ? "1" : "60");
			DBSFaces.encodeAttribute(pWriter, "value", pvGetTimeValue(pInputDate, pIncludeSeconds), "");
			pvEncodeAutocompleteAttribute(pInputDate, pWriter);
			encodeClientBehaviors(pContext, pInputDate);
		pWriter.endElement("input");
	}

	private String pvGetTimeValue(DBSInputDate pInputDate, boolean pIncludeSeconds){
		String xHour = DBSString.toString(pInputDate.getHour(), "");
		String xMinute = DBSString.toString(pInputDate.getMinute(), "");
		String xSecond = DBSString.toString(pInputDate.getSecond(), "");
		if (DBSObject.isEmpty(xHour) ||
			DBSObject.isEmpty(xMinute)){
			return "";
		}
		if (DBSObject.isEmpty(xSecond)){
			xSecond = "00";
		}
		if (pIncludeSeconds){
			return xHour + ":" + xMinute + ":" + xSecond;
		}else{
			return xHour + ":" + xMinute;
		}
	}

	private String pvDecodeDateValue(Map<String, String> pParams, String pClientIdAction){
		String xDay = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.DAY.trim()), "");
		String xMonth = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.MONTH.trim()), "");
		String xYear = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.YEAR.trim()), "");
		if (DBSObject.isEmpty(xMonth) &&
			DBSObject.isEmpty(xYear) &&
			xDay.contains("-")){
			String[] xParts = xDay.split("-");
			if (xParts.length >= 3){
				xYear = xParts[0];
				xMonth = xParts[1];
				xDay = xParts[2];
			}
		}
		if (DBSObject.isEmpty(xDay) ||
			DBSObject.isEmpty(xMonth) ||
			DBSObject.isEmpty(xYear)){
			return null;
		}
		return xDay + "/" + xMonth + "/" + xYear;
	}

	private String[] pvDecodeTimeValue(Map<String, String> pParams, String pClientIdAction){
		String xHour = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.HOUR.trim()), "");
		String xMinute = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.MINUTE.trim()), "");
		String xSecond = DBSString.toString(pParams.get(pClientIdAction + CSS.INPUTDATE.SECOND.trim()), "00");
		if (DBSObject.isEmpty(xMinute) &&
			xHour.contains(":")){
			String[] xParts = xHour.split(":");
			xHour = xParts.length > 0 ? xParts[0] : "";
			xMinute = xParts.length > 1 ? xParts[1] : "";
			xSecond = xParts.length > 2 ? xParts[2] : "";
		}
		if (DBSObject.isEmpty(xSecond)){
			xSecond = "00";
		}
		if (DBSObject.isEmpty(xHour) ||
			DBSObject.isEmpty(xMinute)){
			return null;
		}
		return new String[]{xHour, xMinute, xSecond};
	}

	private String pvJoinTime(String[] pTimeParts){
		if (pTimeParts == null ||
			pTimeParts.length < 2){
			return "";
		}
		if (pTimeParts.length < 3){
			return pTimeParts[0] + ":" + pTimeParts[1] + ":00";
		}
		return pTimeParts[0] + ":" + pTimeParts[1] + ":" + pTimeParts[2];
	}
}




