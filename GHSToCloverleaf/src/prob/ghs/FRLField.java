package prob.ghs;

import prob.ghs.Field_Types;

public class FRLField {
	public String field_name = "";
	public int fieldLength = 0;
	public String padding_dir = "";
	public String padding_char = "";
	public Field_Types type = Field_Types.text;
	public String dateFormat = "";
	public boolean skip = false;
	public boolean lineitem = false;
	public boolean custom = true;
	
	public FRLField(String fname,int len,String pad_dir,String pad_char){
		fieldLength = len;
		padding_dir = pad_dir;
		padding_char = pad_char;
		field_name = fname;
	}
	public FRLField(String fname,int len,String pad_dir,String pad_char,boolean is_line,boolean custom){
		this(fname,len,pad_dir,pad_char);
		lineitem = is_line;
		this.custom = custom;
	}
	public FRLField(String fname,int len,  String pad_dir,String pad_char,Field_Types type,String dateFormat){
		this(fname,len,pad_dir,pad_char,false,false);
		this.type = type;
		this.dateFormat = dateFormat;
	}
	public FRLField(String fname,int len,String pad_dir,String pad_char,boolean is_line,boolean custom,Field_Types type,String dateFormat){
		this(fname,len,pad_dir,pad_char,is_line,custom);
		this.type = type;
		this.dateFormat = dateFormat;
	}
	public FRLField(String fname,boolean skip){
		field_name = fname;
		this.skip = skip;
	}
}
