package prob.util;

public class Format {
	public static String format(String StringToFormat, int NewLength, char PaddingCharacter, char Pad_Direction){
		StringBuffer rslt;
		if(StringToFormat == null)
			rslt = new StringBuffer("");
		else
			rslt = new StringBuffer(StringToFormat.substring(0,Math.min(NewLength,StringToFormat.length())));
		
		if(rslt.length() < NewLength){
			int numToAdd = NewLength - rslt.length();
			for(int i = 0; i < numToAdd; i++){
				if(Pad_Direction=='L'){
					rslt.insert(0,PaddingCharacter);
				}
				else if(Pad_Direction=='R'){
					rslt.append(PaddingCharacter);
				}
			}
		}
		
		return rslt.toString();
	}
	public static String format(String StringToFormat, int NewLength, String PaddingCharacter, String Pad_Direction){
		return format(StringToFormat,NewLength,PaddingCharacter.charAt(0),Pad_Direction.charAt(0));
	}
	public static String format(String StringToFormat,int NewLength){
		return format(StringToFormat,NewLength,' ','R');
	}
}
