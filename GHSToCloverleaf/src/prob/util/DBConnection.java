package prob.util;

import java.io.Closeable;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/* Class creates a connection to a data source, runs the query, and returns the ResultSet.
 * @Author Adam e613467
 * @date 03/11/2016
 */
public class DBConnection implements Closeable {
	private java.sql.Connection conn;
	private Logger l;
	
	/**
	 * Creates an internal Connection object from a data source named by the parameter.
	 * 
	 * @param jndi_name
	 *            The name of a data source configured on the server
	 * @throws NamingException,SQLException
	 *             exceptions thrown by creating the connection object will be 
	 *             thrown back to the calling function.
	 */
	public DBConnection(String jndi_name,Logger l) throws NamingException, SQLException {
		this.l = l;
		l.log(Level.FINER,"Attempting DB Connection to " + jndi_name);
		try{
			conn = ((DataSource) new InitialContext().lookup(jndi_name)).getConnection();
		}
		catch(NamingException e){
			l.log(Level.SEVERE,e.getClass() + ": " + e.getMessage());
			throw e;
		}
		catch(SQLException e){
			l.log(Level.SEVERE,e.getClass() + ": " + e.getMessage());
			throw e;
		}
		l.log(Level.FINE,"DB Connection Successful.");
	}

	/**
	 * Create an internal Connection object from a URI and driver
	 * 
	 * @param uri The URI for the data source
	 * @param driver The driver to use to access the data source
	 * 
	 * @throws RuntimeException if there are issues loading the driver or connecting to the database. 
	 */
	public DBConnection(String uri,String driver,Logger l){
		this(uri,driver,null,null,l);
	}

	/**
	 * Create an internal Connection object from a URI and driver with a username and password. 
	 * Using null for the username will attempt a connection with no credentials.
	 * 
	 * @param uri The URI for the data source
	 * @param driver The driver to use to access the data source
	 * 
	 * @throws RuntimeException if there are issues loading the driver or connecting to the database. 
	 */
	public DBConnection(String uri,String driver,String username,String password,Logger l){
		this.l = l;
		l.log(Level.FINER,"Attempting DB Connection...");
		try {
			Class.forName(driver);
		}
		catch (ClassNotFoundException e) {
			l.log(Level.SEVERE,("Error loading JDBC driver: " + e.getMessage()));
			throw new RuntimeException("Error loading JDBC driver: " + e.getMessage());
		}
		try {
			if(username==null)
				conn = DriverManager.getConnection(uri);
			else
				conn=DriverManager.getConnection(uri,username,password);
		} 
		catch (SQLException e) {
			l.log(Level.SEVERE,("Error connecting to database: " + e.getMessage()));
			throw new RuntimeException("Error connecting to database: " + e.getMessage());
		}
		l.log(Level.FINE,"DB connection successful.");
	}
	
	/**
	 * getStatement is an internal function for preventing sql injection in variables. 
	 * 
	 * @param sql The sql statement, with ? in place of the variables to be inserted.
	 * @param objects An array of arguments that will be used to replace the ? in the sql parameter.
	 * @return a prepared statement that can be used to execute an sql statement.
	 * @throws SQLException
	 */
	private PreparedStatement getStatement(String sql,Object[] objects) throws SQLException{
		PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < objects.length; i++) {
            ps.setObject(i + 1, objects[i]);
        }
        return ps;
	}
	
	/**
	 * Update will take an sql DML statement (insert, update, or delete), along with any required parameters, and execute it. 
	 * Any parameters not hard-coded into the query should use a ? in place of the value. Then add additional parameters to
	 * the function call that will be used to replace those ?s. 
	 * 
	 * @param sql
	 * @param objects
	 * @return
	 * @throws SQLException
	 */
	public int Update(String sql,Object...objects) throws SQLException{
		PreparedStatement ps = getStatement(sql,objects);
		int rtn;
		try{
			l.finer("Attemptign database update.");
			l.finest("Update text: " + ps.toString());
			rtn = ps.executeUpdate();
			l.fine("Database update successfull.");
		} catch(SQLException e){
			throw e;
		}
		return rtn;
	}
	
	/**
	 * Sends a query to the Connection object and returns the resulting ResultSet
	 * 
	 * @param sql
	 *            The sql to send to the connection object
	 * @param objects
	 * 				An array of objects that needs to be substituted into
	 * 				the sql string.
	 * @return The ResultSet object containing the results of the sql query.
	 * @throws SQLException
	 *             Errors in the SQL will result in an SQL Exception.
	 */
	public ResultSet Query(String sql,Object...objects) throws SQLException {
		PreparedStatement ps = getStatement(sql,objects);
		return Query(ps);
	}
	/**
	 * See Query(String sql,Object...objects). This function is used when no 
	 * object substitutions into the sql are needed.
	 */
	public ResultSet Query(String sql) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(sql);
		return Query(ps);
	}
	/**
	 * See Query(String sql,Object...objects). Internal function 
	 * used by the public function to execute the query.
	 */
	private ResultSet Query(PreparedStatement ps) throws SQLException {
		ResultSet rs = null;
		try{
			l.log(Level.FINER,"Attempting DB Query.");
			l.finest("Query text: " + ps.toString());
			rs = ps.executeQuery();
		}
		catch(SQLException e){
			l.log(Level.SEVERE,e.getClass() + ": " + e.getMessage());
			throw e;
		}
		l.log(Level.FINE,"Query successful.");
		
		return rs;
	}
		
	/**
	 * Close all objects associated with the DBConnection object
	 */
	public void close(){
		try{
			conn.close();
		}
		catch(SQLException e){
			l.log(Level.SEVERE,"Error closing DB connection: " + e.getMessage());
		}
	}
}
