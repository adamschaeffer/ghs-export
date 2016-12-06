package prob.ghs;

import prob.ghs.Field_Types;

public class FRLField {
	public String field_name = "";
	public int fieldLength = 0;//do not use
	public String padding_dir = "";//do not use
	public String padding_char = "";//do not use
	public Field_Types type = Field_Types.text;//do not use
	public String dateFormat = "";//do not use
	public boolean skip = false;
	public boolean lineitem = false;
	public boolean custom = true;
	
	public FRLField(String fname){
		field_name = fname;
	}
	
	public FRLField skipField(boolean skip){
		this.skip = skip;
		return this;
	}
	public FRLField isLineitem(boolean lineitem){
		this.lineitem = lineitem;
		return this;
	}
	public FRLField isCustom(boolean custom){
		this.custom = custom;
		return this;
	}
}
