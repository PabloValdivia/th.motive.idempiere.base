package th.motive.idempiere.base.osgi.component;

import java.util.List;
import java.util.Properties;

import org.adempiere.util.Callback;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.validator.WindowValidator;
import org.adempiere.webui.adwindow.validator.WindowValidatorEvent;
import org.adempiere.webui.adwindow.validator.WindowValidatorEventType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_AD_Element;
import org.compiere.model.MColumn;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

import th.motive.utility.MotiveStringUtils;

/**
 * hande event window to add more UI logic to standard window
 * @author hieplq
 *
 */
@Component(
		property= {"AD_Window_UU:String=e407e1b9-958f-4550-9ffa-ba28a34e603f"
		}
	)
public class BaseADTableWindowEventHandler implements WindowValidator{

	public static final int COLUMN_TAB_ID = 101;//ID of tab column of window table
	
	/**
	 * handler before save when create a new column, if don't have AD_Element for this window, <br/>
	 * just create new one base on data input for column
	 */
	@Override
	public void onWindowEvent(WindowValidatorEvent event, Callback<Boolean> callback) {
		ADWindow adTableWindow = event.getWindow();
		GridTab activeTab = adTableWindow.getADWindowContent().getActiveGridTab();

		if (WindowValidatorEventType.BEFORE_SAVE.getName().equals(event.getName()) && COLUMN_TAB_ID == activeTab.getAD_Tab_ID()) {
			GridField elementField = activeTab.getField(MColumn.COLUMNNAME_AD_Element_ID);
			
			// get column name and sql column name
			String sColumnName = getValueStrField (activeTab, MColumn.COLUMNNAME_Name);
			String sSqlColumnName = getValueStrField (activeTab, MColumn.COLUMNNAME_ColumnName);
			
			// generate value if necessary and normalize it
			String columnName = normalizeColumnName(sSqlColumnName, sColumnName);
			String sqlColumnName = normalizeSqlColumnName(sSqlColumnName, sColumnName);
			
			// check to see have or not element to this column
			if (elementField != null && elementField.getValue() == null && StringUtils.isNotBlank(columnName) && StringUtils.isNotBlank(sqlColumnName)) {
				M_Element newElement = null;
				List<M_Element> matchingElements = BaseADTableWindowEventHandler.getMElementByColumnName (Env.getCtx(), null, sqlColumnName);
				
				if (matchingElements.isEmpty()) {
					newElement = createNewElementBaseOnColumnData (activeTab, sqlColumnName, columnName);
				}else if (matchingElements.size() == 1) {
					newElement = matchingElements.get(0);
				}
				
				// fill element for this column
				if (newElement != null)
					activeTab.setValue(elementField, newElement.getAD_Element_ID());
			}			
		}
 
		// always have to call it to let next handler can run
		callback.onCallback(Boolean.TRUE);
	}
	
	/**
	 * create Sql column name from column name if null
	 * normalize value
	 * @param sSqlColumnName
	 * @param sColumnName
	 * @return
	 */
	protected String normalizeSqlColumnName(String sSqlColumnName, String sColumnName) {
		if (sSqlColumnName != null || sColumnName == null)
			return sSqlColumnName;
		
		// create column sql name from column name
		sSqlColumnName = WordUtils.capitalize (sColumnName);
		sSqlColumnName = StringUtils.deleteWhitespace(sSqlColumnName);
		
		return sSqlColumnName;
	}
	
	/**
	 * create column name from sql column name if null
	 * normalize value
	 * @param sSqlColumnName
	 * @param sColumnName
	 * @return
	 */
	protected String normalizeColumnName(String sSqlColumnName, String sColumnName) {
		if (sColumnName != null || sSqlColumnName == null)
			return sColumnName;
		
		// create column name from sql column name
		sColumnName = MotiveStringUtils.regReplace(sSqlColumnName, "_(\\w)", m -> {
			String g1 = m.group(1);
			if (StringUtils.isNotEmpty(g1)) {
				return " " + g1.toUpperCase();
			}
			return "";});
		// remove ID on name of column
		sColumnName = MotiveStringUtils.regReplace (sColumnName, " ID$", "");
		
		// remove table prefix
		sColumnName = MotiveStringUtils.regReplace (sColumnName, "^[A-Z]+ ", "");
		
		return sColumnName;
	}
	
	/**
	 * get by column name input on column tab. it's name field, isn't database column name
	 * @return
	 */
	protected static List<M_Element> getMElementByColumnName(Properties ctx, String trxName, String columnName) {
		final String whereClause = "UPPER(Name)=?";
		return new Query(ctx, I_AD_Element.Table_Name, whereClause, trxName)
			.setParameters(columnName.toUpperCase())
			.list();
	}
	
	/**
	 * get value of string field and convert to string
	 * @param tab
	 * @param fieldName
	 * @return
	 */
	protected static String getValueStrField(GridTab tab, String fieldName) {
		 GridField nameField = tab.getField(fieldName);
		 if (nameField != null && nameField.getValue() != null) {
			 return nameField.getValue().toString();
		 }
		 return null;
	}
	
	/**
	 * create a element base on column data input on column tab like entityType, name, description <br/>
	 * also create sql column name base on name
	 * @param columnTab
	 * @param columnName
	 * @return
	 */
	protected static M_Element createNewElementBaseOnColumnData (GridTab columnTab, String sqlColumnName, String columnName) {
		// not yet have element for this new column, create new one
		 M_Element newElement = new M_Element(Env.getCtx(), 0, null);
		 newElement.setName(columnName);
		 newElement.setPrintName(columnName);
		 newElement.setColumnName(sqlColumnName);
		 
		 GridField copyField = columnTab.getField(MColumn.COLUMNNAME_Description);
		 if (copyField.getValue() != null) {
			 newElement.setDescription(copyField.getValue().toString()); 
		 }
		 
		 copyField = columnTab.getField(MColumn.COLUMNNAME_Help);
		 if (copyField.getValue() != null) {
			 newElement.setHelp(copyField.getValue().toString()); 
		 }
		 
		 copyField = columnTab.getField(MColumn.COLUMNNAME_EntityType);
		 if (copyField.getValue() != null) {
			 newElement.setEntityType(copyField.getValue().toString()); 
		 }
		 
		 newElement.saveEx();
		 
		 return newElement;
	}
}
