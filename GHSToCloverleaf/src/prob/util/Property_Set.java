package prob.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Property_Set {
	private static final String filename = "ghs.properties";
	private static final Properties props = new Properties();
	
	static {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    InputStream input = classLoader.getResourceAsStream(filename);
		
	    try {
	    	props.load(input);
	    }
	    catch (IOException e) {
	    	throw new RuntimeException("Cannot load properties file '" + filename + "'.", e);
	    }
	 }
	    
	private String specificKey;
	/** 
	 * @param specificKey
	 *            The specific key which is to be used as property key prefix.
	 * @throws RuntimeException
	 *             During class initialization if the DAO properties file is
	 *             missing in the classpath or cannot be loaded.
	 */
	public Property_Set(String specificKey) throws RuntimeException {
		this.specificKey = specificKey;
	}

	/**
	 * Returns the DaoProperties instance specific property value associated
	 * with the given key with the option to indicate whether the property is
	 * mandatory or not.
	 * 
	 * @param key
	 *            The key to be associated with a DaoProperties instance
	 *            specific value.
	 * @param mandatory
	 *            Sets whether the returned property value should not be null
	 *            nor empty.
	 * @return The DaoProperties instance specific property value associated
	 *         with the given key.
	 * @throws RuntimeException
	 *             If the returned property value is null or empty while it is
	 *             mandatory.
	 */
	public String getProperty(String key, boolean mandatory) throws RuntimeException {
		String fullKey = specificKey + "." + key;
		String property = props.getProperty(fullKey);
        // String property = propMap.get(fullKey);
		
		if (property == null || property.trim().length() == 0) {
			if (mandatory) {
				throw new RuntimeException("Required property '" + fullKey + "'"
						+ " is missing in properties file '" + filename + "'.");
			} else {
				property = null;
			}
		}
		return property;
	}

}
