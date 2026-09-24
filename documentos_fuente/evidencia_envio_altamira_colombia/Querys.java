package jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class Querys { 
	private Logger LOGGER=null;
	
	public Querys(Logger LOGGER){
		this.LOGGER=LOGGER;
	}
	
	public long crearJOB(String jobId,String Servicio, Connection connection)	{
		Statement statement = null;

		String insertTableSQL = "Insert into FT_T_JBLG (JOB_ID,RQST_TRN_ID,JOB_STAT_TYP,JOB_START_TMS,JOB_END_TMS,TASK_TOT_CNT,TASK_CMPLTD_CNT,JOB_INPUT_TXT,"
				+ "JOB_CONFIG_TXT,RQST_CORR_ID,TASK_SUCCESS_CNT,TASK_FAILED_CNT,LAST_UPD_TMS,MAX_RECORD_SEQ_NUM,PRNT_JOB_ID,JOB_MSG_TYP,JOB_TME_TXT,JOB_TPS_CNT,"
				+ "TASK_PARTIAL_CNT,TASK_FILTERED_CNT,INSTANCE_ID,PEVL_OID) "
				+"values ('" + jobId
				+"',null,'OPEN  '"
				+",sysdate,sysdate"
				+",0,0,null,null,null,0,0"
				+",sysdate"
				+",null,null,'"
				+ Servicio
				+"',null,0,0,0,null,null)";
		try 	{
			statement = connection.createStatement();
			statement.executeUpdate(insertTableSQL);  

			System.out.println("Record is inserted into FT_T_JBLG");
			statement.close();
		} catch (SQLException e) {e.printStackTrace();		}
		return System.currentTimeMillis();
	}

	public void cerrarJOB(String jobId,String Servicio,long inicio, Connection connection)	{
		Statement statement = null;
		long fin = System.currentTimeMillis();
		Integer tot = (int) (fin - inicio);
		int h = (int) ((tot / (1000*60*60)) % 24);
		int m = (int) ((tot / (1000*60)) % 60) - h * 60; 
		int s = (int) ((tot / 1000)) - m * 60;

		String updateTableSQL = "Update FT_T_JBLG set job_stat_typ = 'CLOSED',"
				+ "job_end_tms=sysdate,job_tme_txt='"+h+":"+m+":"+s+"'"
				+"where job_id = '" + jobId +"'";
		try 	{
			statement = connection.createStatement();
			statement.executeUpdate(updateTableSQL);   

			statement.close();
			System.out.println("Record is closed into FT_T_JBLG");

		} catch (SQLException e) {e.printStackTrace();	}
	}

	public void executeCON_Hilos(ArrayList<HashMap<String,String>> arr, Connection connection){
		CallableStatement cs;
		try 	{
			for(int i = 0; i<arr.size();i++) {
				//cs = connection.prepareCall(("{call CONCIALT (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));
				cs = connection.prepareCall(("{call PCK_CON_ALT_COL.PR_MAIN (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));
				
				cs.setString(1,arr.get(i).get("NUMCLIEN"));  	    
				cs.setString(2,arr.get(i).get("FIRNAME"));
				cs.setString(3,arr.get(i).get("MIDDLENAME")); 	    
				cs.setString(4,arr.get(i).get("LASTNAM"));		
				cs.setString(5,arr.get(i).get("SELSNAM"));   	    
				cs.setString(6,arr.get(i).get("COIDEN"));			
				cs.setString(7,arr.get(i).get("NUMDOCU"));  	   	
				cs.setString(8,arr.get(i).get("ISPREFE"));
				cs.setString(9,arr.get(i).get("CONTNUM"));  	    
				cs.setString(10,arr.get(i).get("PHONETY"));			
				cs.setString(11,arr.get(i).get("CONTNUM2")); 	   
				cs.setString(12,arr.get(i).get("PHONETY2"));
				cs.setString(13,arr.get(i).get("ADDRESS")); 	    
				cs.setString(14,arr.get(i).get("ADDRSNM"));			
				cs.setString(15,arr.get(i).get("GEGCODE")); 	   
				cs.setString(16,arr.get(i).get("GEGNAME"));		    	
				cs.setString(17,arr.get(i).get("GEGCODE2")); 	   
				cs.setString(18,arr.get(i).get("GEGNAME2"));
				cs.setString(19,arr.get(i).get("ECONMID"));			
				
				//cs.setString(20,arr.get(i).get("FLD_JOB_ID"));
				
				/*System.out.println(i + " - NUMCLIEN "+arr.get(i).get("NUMCLIEN"));
				System.out.println(i + " - FIRNAME "+arr.get(i).get("FIRNAME"));
				System.out.println(i + " - MIDDLENAME "+arr.get(i).get("MIDDLENAME"));
				System.out.println(i + " - LASTNAM "+arr.get(i).get("LASTNAM"));
				System.out.println(i + " - SELSNAM "+arr.get(i).get("SELSNAM"));
				System.out.println(i + " - COIDEN "+arr.get(i).get("COIDEN"));
				System.out.println(i + " - NUMDOCU "+arr.get(i).get("NUMDOCU"));
				System.out.println(i + " - ISPREFE "+arr.get(i).get("ISPREFE"));
				System.out.println(i + " - CONTNUM "+arr.get(i).get("CONTNUM"));
				System.out.println(i + " - PHONETY "+arr.get(i).get("PHONETY"));
				System.out.println(i + " - CONTNUM2 "+arr.get(i).get("CONTNUM2"));
				System.out.println(i + " - PHONETY2 "+arr.get(i).get("PHONETY2"));
				System.out.println(i + " - ADDRESS "+arr.get(i).get("ADDRESS"));
				System.out.println(i + " - ADDRSNM "+arr.get(i).get("ADDRSNM"));
				System.out.println(i + " - GEGCODE "+arr.get(i).get("GEGCODE"));
				System.out.println(i + " - GEGNAME "+arr.get(i).get("GEGNAME"));
				System.out.println(i + " - GEGCODE2 "+arr.get(i).get("GEGCODE2"));
				System.out.println(i + " - GEGNAME2 "+arr.get(i).get("GEGNAME2"));
				System.out.println(i + " - ECONMID "+arr.get(i).get("ECONMID"));
				System.out.println(i + " - FLD_JOB_ID "+arr.get(i).get("FLD_JOB_ID"));*/
				
				marcadoQuery(connection, "PCK_CON_ALT_COL.PR_MAIN", "executeCON_Hilos");
				cs.execute();
				
				cs.close();
			}
		} catch (SQLException e) {	e.printStackTrace();		}

	}

	public void insertRLT1Colombia(String NUMCLIEN,String jobId, Connection connection, String msg){
		String insertTableSQL = "INSERT INTO FT_T_RLT1 ("
				+ "RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM," //1
				+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD," //2
				+ "RLT_PURP_TYP,DATA_SRC_APP,"//3
				+ "SRC_FIELD,SRC_VALUE,"//4
				+ "GS_FIELD,GS_VALUE,"//5
				+ "MAIN_ENTITY_NME,MAIN_ENTITY_ID,"//6
				+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,"//7
				+ "RLT_DIF_STAT,RLT_DIF_ACC) "//8
				+ "values ("
				+ "new_oid,'" + jobId + "',null,null," //1
				+ "2,'"+ msg +"',null," //2
				+ "'REPORTES',null," //3
				+ "'NUMCLIEN','" + NUMCLIEN +"',"//4
				+ "null,null," //5
				+ "'ID_ALTAMIRA_COL','"+ NUMCLIEN + "',"//6
				+ "sysdate,null,sysdate,'BBVA:CUSTOMER',"//7
				+ "'PENDING','B')";//8

		PreparedStatement preparedStatement;
		try {
			preparedStatement = connection.prepareStatement(insertTableSQL);
			marcadoQuery(connection, insertTableSQL, "insertRLT1Colombia");
			preparedStatement .executeUpdate();

			preparedStatement.close();

		} catch (SQLException e) {	e.printStackTrace();	}
	
}

	public ArrayList<String> obtenerIDs(Connection connection )	{
		ArrayList<String> array = new ArrayList<String>();
		String query = null ;
		try	{
			//query = "select DISTINCT FINS_ID ID from FT_T_FIID where FINS_ID_CTXT_TYP = 'ID_ALTAMIRA_COL' AND DATA_STAT_TYP='ACTIVE'";
			
			query = "select DISTINCT FINS_ID ID from FT_T_FIID FIID, FT_T_FINS FINS "
					+ " where FIID.INST_MNEM = FINS.INST_MNEM "
					+ " AND FIID.FINS_ID_CTXT_TYP = 'ID_ALTAMIRA_COL'"
					+ " AND length(FIID.FINS_ID)=8 "
					+ " AND FIID.DATA_STAT_TYP='ACTIVE' "
					+ " AND FINS.DATA_STAT_TYP!='INACTIVE' "
					+ " AND EXISTS( "
					+ " SELECT 1 "
					+ " FROM FT_T_FIRL FIRL, FT_T_ENFR ENFR "
					+ " WHERE FIRL.PRNT_INST_MNEM=ENFR.FINR_INST_MNEM "
					+ " AND FIRL.INST_MNEM=FINS.INST_MNEM "
					+ " AND ENFR.ENFR_RL_TYP='ENT_OWN' "
					+ " AND ENFR.DATA_STAT_TYP='ACTIVE' "
					+ " AND ENFR.ORG_ID='9020' "
					+ " AND FIRL.DATA_STAT_TYP='ACTIVE')";

			PreparedStatement stmnt = connection.prepareStatement(query);
			ResultSet rs = null;	
			
			marcadoQuery(connection, query, "obtenerIDs");
			rs = stmnt.executeQuery();

			while(rs.next()) {	array.add(rs.getString("ID"));	}
			rs.close(); stmnt.close();

		} catch (Exception e) {	
			e.printStackTrace();
			String msg = "obtenerIDs::Fallo al ejecutar la siguiente modificacion. "+ e.toString() + "\n" + query;
			System.out.println(msg);
			LOGGER.error(msg);
		}
		return array;
	}
	
	public String getJuncShiva(Connection connection )	{
		String aux = new String();
		try	{
			String query = "SELECT PAR1_VALUE AS URL_SHIVA FROM FT_T_PAR1 WHERE "
					+ " PARAMETER_CTXT_TYP='JUNCTION' "
					+ " AND PAR1_NME='ConciliaColombia' "
					+ " AND DATA_SRC_ID='CONCILIA_COLOMBIA'"
					+ " and DATA_STAT_TYP = 'ACTIVE'";
			
			PreparedStatement stmnt = connection.prepareStatement(query);
			ResultSet rs = null;	
			
			marcadoQuery(connection, query, "getJuncShiva");
			rs = stmnt.executeQuery();
			
			while(rs.next()) {	
				aux = rs.getString("URL_SHIVA");	
			}
			rs.close(); stmnt.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return aux;
	}
	
	public String getLlave2(Connection connection )	{
		String aux = new String();
		try	{
			String query = "SELECT PAR1_VALUE AS LLAVE2 FROM FT_T_PAR1 WHERE "
					+ " PARAMETER_CTXT_TYP='LLAVE2' "
					+ " AND PAR1_NME='ConciliaColombia' "
					+ " AND DATA_SRC_ID='CONCILIA_COLOMBIA'"
					+ " and DATA_STAT_TYP = 'ACTIVE'";
			
			PreparedStatement stmnt = connection.prepareStatement(query);
			ResultSet rs = null;	
			
			marcadoQuery(connection, query, "getLlave2");
			rs = stmnt.executeQuery();
			
			while(rs.next()) {	
				aux = rs.getString("LLAVE2");	
			}
			rs.close(); stmnt.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return aux;
	}
	
	public String obtenerTiempo(String metodo, long start, long end) {
		long res = end - start;
		long second = res / 1000L % 60L;
		long minute = res / 60000L % 60L;
		long hour = res / 3600000L % 24L;
		String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, res);
		return time;
	}
	
	
	public void marcadoQuery(Connection conexion, String query, String metodo){
		//marcado de query
        try {
            CallableStatement call = conexion.prepareCall("begin DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); end;");
            try {
                call.setString(1, "ColombiaConciliacion");
                call.setString(2,query);
                call.execute();
            } finally {
                call.close();
            }
        } catch (SQLException ex) {
        	String msg = "ColombiaConciliacion::QueryExec::"+metodo+"::Fallo marcado de queries "+ ex.toString() + "\n" + query;
			System.out.println(msg);
			LOGGER.error(msg);
        }//fin marcado de query
	}
}
