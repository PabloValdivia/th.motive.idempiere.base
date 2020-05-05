package th.motive.idempiere.base.osgi.component;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.apache.commons.lang.StringUtils;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.osgi.service.component.annotations.Component;

@Component(
		property= {"service.ranking:Integer=1"}
		)
public class CsvImportASICallout implements IColumnCalloutFactory{
	private static CLogger log = CLogger.getCLogger(CsvImportASICallout.class);
	@Override
	public IColumnCallout[] getColumnCallouts(String tableName, String columnName) {
		if (StringUtils.equals(tableName, MProduct.Table_Name) && 
				StringUtils.equals (columnName, MProduct.COLUMNNAME_M_AttributeSet_ID)) {
			return new MatchingASI [] {new MatchingASI()};
			
		}
		return new IColumnCallout[] {};
	}
	
	public static class MatchingASI implements IColumnCallout{

		@Override
		public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
				Object oldValue) {
			if (mTab.getTableModel().isImporting() && mTab.getValue(MProduct.COLUMNNAME_M_AttributeSet_ID) != null) {
				int asID = (int)mTab.getValue(MProduct.COLUMNNAME_M_AttributeSet_ID);
				String fakeProductName = (String)mTab.getValue(MProduct.COLUMNNAME_DocumentNote);
				String [] attributePart = fakeProductName.split("#");
				
				String group = null;
				String subGroup = null;
				String txtAttribute = null;
				String colorAttribute = null;
				String sizeAttribute = null;
				
				for (String attribute : attributePart) {
					if (StringUtils.startsWith(attribute, "TXT_")) {
						txtAttribute = StringUtils.removeStart(attribute, "TXT_");
					}else if(StringUtils.startsWith(attribute, "SIZE_")) {
						sizeAttribute = StringUtils.removeStart(attribute, "SIZE_");
					}else if(StringUtils.startsWith(attribute, "COLOR_")) {
						colorAttribute = StringUtils.removeStart(attribute, "COLOR_");
					}else if(StringUtils.startsWith(attribute, "GR_")) {
						group = StringUtils.removeStart(attribute, "GR_");
					}else if(StringUtils.startsWith(attribute, "SGR_")) {
						subGroup = StringUtils.removeStart(attribute, "SGR_");
					}else {
						log.warning("IMPORT ASI:wrong attribute value:" + fakeProductName);
						return null;
					}
				}
				
				// validate mandatory info
				if (txtAttribute == null || group == null || subGroup == null) {
					log.warning("IMPORT ASI:Missing text or group or sub-group information");
					return null;
				}
				
				// build ASI description from product name to lookup asi
				MAttributeSet as = new MAttributeSet(ctx, asID, null);
				MAttribute [] attributes = as.getMAttributes(false);
				
				StringBuilder asiDesc = new StringBuilder();
				String strAv = null;
				for (MAttribute attribute : attributes) {
					if (StringUtils.endsWith(attribute.getName(), " # PROTXT")){
						strAv = txtAttribute;
					}else if (StringUtils.endsWith(attribute.getName(), " # PRDCLR")){
						strAv = colorAttribute;
					}else if (StringUtils.endsWith(attribute.getName(), " # PRDSIZE")){
						strAv = sizeAttribute;
					}
					
					if (asiDesc.length() > 0)
						asiDesc.append("_");
					
					if (!StringUtils.equals(strAv, "..."))
						asiDesc.append(strAv);
				}
				
				
				MTable asiTable = MTable.get(ctx, MAttributeSetInstance.Table_ID);
				MAttributeSetInstance asi = (MAttributeSetInstance)asiTable.getPO(MAttributeSetInstance.COLUMNNAME_Description + " = ?" , new Object [] {asiDesc.toString()}, null);
				if (asi == null) {
					asi = createNewASI (ctx, asiDesc.toString(), attributes, asID, txtAttribute, sizeAttribute, colorAttribute);
				}
				if (asi != null)
					mTab.setValue(MProduct.COLUMNNAME_M_AttributeSetInstance_ID, asi.getM_AttributeSetInstance_ID());
			}
			return null;
		}
		
	}
	
	static MAttributeSetInstance createNewASI (Properties ctx, String asiDesc, MAttribute [] attributes, int asID, String txtAttribute, String sizeAttribute, String colorAttribute) {
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, asID, null);
		asi.saveEx();
		for (MAttribute attribute : attributes) {
			String avi = null;
			if (StringUtils.endsWith(attribute.getName(), " # PROTXT")){
				avi = txtAttribute;
			}else if (StringUtils.endsWith(attribute.getName(), " # PRDCLR")){
				avi = colorAttribute;
			}else if (StringUtils.endsWith(attribute.getName(), " # PRDSIZE")){
				avi = sizeAttribute;
			}else {
				log.warning("IMPORT ASI:new add attribute:" + attribute.getName());
				return null;
			}
			
			MAttributeValue av = null;
			if (!StringUtils.equals(avi, "...")) {
				MTable avTable = MTable.get(ctx, MAttributeValue.Table_ID);
				av = (MAttributeValue)avTable.getPO(MAttributeValue.COLUMNNAME_Name + " = ? AND " + MAttributeValue.COLUMNNAME_M_Attribute_ID  + " = ?" , new Object [] {avi, attribute.get_ID()}, null);
				if (av == null) {
					log.warning("IMPORT ASI:Can't look up attribute value:" + avi);
					return null;
				}
			}
			
			attribute.setMAttributeInstance (asi.getM_AttributeSetInstance_ID(), av);
		}
		asi.setDescription();
		if (!StringUtils.equals(asiDesc, asi.getDescription())) {
			log.warning("IMPORT ASI:Asi description don't match:from csv:" + asiDesc + " from asi:" + asi.getDescription());
			return null;
		}
		asi.saveEx();
		return asi;
	}

}
