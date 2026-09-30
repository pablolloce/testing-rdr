package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.Statement;
import java.util.Vector;
import main.Ppal;
public class QuerysStr {
	
	//Querys sobre TPG1
	public static String queryTPG1(){
		String rs = "SELECT * FROM FT_T_TPG1 \n"
			      + " WHERE END_TMS IS NULL \n"
			      + " ORDER BY PROCESO, ID_DEF_ALERT, JOB_ID, REGISTRO, CLAVE DESC";
		//System.out.println(rs);
		return rs;
	}
	
	public static String queryTPG1Filtrado(){
		String rs= "SELECT * FROM FT_T_TPG1 \n"
				 + " WHERE END_TMS IS NULL \n"
				 + "   AND PROCESO = ? \n"
				 + " ORDER BY PROCESO, ID_DEF_ALERT, JOB_ID, REGISTRO, CLAVE DESC";
		//System.out.println(rs);
		return rs;
	}
	
	//Marcado sobre la TPG1
	public static String queryMarcadoTPG1(Vector<String>oids){
		String res="UPDATE FT_T_TPG1 \n"
				  +"   SET END_TMS = SYSDATE, \n"
				  +"       LAST_CHG_TMS = SYSDATE, \n"
				  +"       LAST_CHG_USR_ID = ? \n"
				  +" WHERE TPG1_OID IN ( \n";
		if(oids.size()>0){
			for(int i=0;i<oids.size()-1;i++){
				res+=" ?, \n";
			}
			res+="?)";
		}else{
			res+="'')";
		}
		//System.out.println(res);
		return res;
	}

	public static String queryALD1_Info(){
		
		String q="SELECT ID_DEF_ALERT, ALD1_OID, DESCRIP_LARGA \n"
				+"  FROM FT_T_ALD1";
		return q;
	}
	
	//Insert ALG1
	/*public static String insertALG1(String proceso, String ald1_oid, String mensaje, String tipo){
		String insert = "INSERT INTO FT_T_ALG1 (ALG1_OID, PROCESO, ALD1_OID, MENSAJE, TIPO, PROCESADO, "
				      + "DATA_STAT_TYP, LAST_CHG_TMS, START_TMS, END_TMS, LAST_CHG_USR_ID) "
				      + "VALUES (NEW_OID, ?, ?, ?, ?, 'N', 'ACTIVE', "
				      + "SYSDATE, SYSDATE, NULL, ?)";
		
		return insert;
	}*/
	public static String insertALG1(String proceso, String ald1_oid, String mensaje, String tipo){
		String insert = "INSERT INTO FT_T_ALG1 (ALG1_OID, PROCESO, ALD1_OID, MENSAJE, TIPO, PROCESADO, "
				      + "DATA_STAT_TYP, LAST_CHG_TMS, START_TMS, END_TMS, LAST_CHG_USR_ID) "
				      + "VALUES (NEW_OID, ?, ?, ?, ?, 'N', 'ACTIVE', "
				      + "SYSDATE, SYSDATE, NULL, ?)";
		
		int conteo =0;
		try {
			PreparedStatement sta=Ppal.obj_con.prepareStatement(insert);
			sta.setString(1, proceso);
			sta.setString(2, ald1_oid);
			sta.setString(3, mensaje);
			sta.setString(4, tipo);
			sta.setString(5, QuerysConfig.marcaProceso);
			conteo = sta.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
			String msg="QuerysStr::insertaRLT1::ERROR::No se pudo insertar en la ALG1";
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
			msg=e.toString();
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
		}
		return ""+conteo;
	}
	//Insert RLT1
	public static String insertRLT1(String procedimiento, String proceso, String id_ejecucion, String mensaje, String estado, double tiempo, String campo, String valor){
		
		String insert="INSERT INTO FT_T_RLT1 (RLT_OID, JOB_ID, TRN_ID, RECORD_SEQ_NUM, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_FIELD, "
                     +"RLT_PURP_TYP, DATA_SRC_APP, SRC_FIELD, SRC_VALUE, GS_FIELD, GS_VALUE, MAIN_ENTITY_NME, MAIN_ENTITY_ID, " 
                     +"START_TMS, END_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) \n"
                     +" VALUES (NEW_OID, null, null, 1, null, ?, null, ?, ?, ?, ?, ?, null, null, null, ?, ?	, "
                     +"SYSDATE, null, SYSDATE, 'GESTION_ALERTAS')";
		
		//System.out.println(insert);
		
		return insert;
	}
	
	/*public static String insertErrorMsgRLT1(Connection obj_con, String proceso, String id_ejecucion, String mensaje, String campo, String valor){
		
		String insert="INSERT INTO FT_T_RLT1 (RLT_OID, JOB_ID, TRN_ID, RECORD_SEQ_NUM, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_FIELD, "
                     +"RLT_PURP_TYP, DATA_SRC_APP, SRC_FIELD, SRC_VALUE, GS_FIELD, GS_VALUE, MAIN_ENTITY_NME, MAIN_ENTITY_ID, " 
                     +"START_TMS, END_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) \n"
                     +" VALUES (NEW_OID, null, null, 1, null, null, null, '"+mensaje+"', null, 'ERRORES', '"+proceso+"', '"+id_ejecucion+"', '"+campo+"', '"+valor+"', null, 'ERROR_GESTION_ALERTAS', '"+id_ejecucion+"', "
                     +"SYSDATE, null, SYSDATE, 'GESTION_ALERTAS')";
		
		//System.out.println(insert);

	}*/
	
	public static String insertErrorMsgRLT1(String proceso, String id_ejecucion, String mensaje, String campo, String valor){
		
		String insert="INSERT INTO FT_T_RLT1 (RLT_OID, JOB_ID, TRN_ID, RECORD_SEQ_NUM, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_FIELD, "
                     +"RLT_PURP_TYP, DATA_SRC_APP, SRC_FIELD, SRC_VALUE, GS_FIELD, GS_VALUE, MAIN_ENTITY_NME, MAIN_ENTITY_ID, " 
                     +"START_TMS, END_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) \n"
                     +" VALUES (NEW_OID, null, null, 1, null, null, null, ?, null, 'ERRORES', ?, ?, ?, ?, null, 'ERROR_GESTION_ALERTAS', ?, "
                     +"SYSDATE, null, SYSDATE, 'GESTION_ALERTAS')";
		
		//System.out.println(insert);
		int conteo =0;
		try {
			PreparedStatement sta=Ppal.obj_con.prepareStatement(insert);
			sta.setString(1, mensaje);
			sta.setString(2, proceso);
			sta.setString(3, id_ejecucion);
			sta.setString(4, campo);
			sta.setString(5, valor);
			sta.setString(6, id_ejecucion);
			conteo = sta.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
			String msg="QuerysStr::insertaRLT1::ERROR::No se pudo insertar en la RLT1";
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
			msg=e.toString();
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
		}
		return ""+conteo;
	}
	
	public static void insertaRLT1(Connection obj_con, PreparedStatement sta, String procedimiento, String proceso, String id_ejecucion, String mensaje, String estado, double tiempo, String campo, String valor){
		
		String query=insertRLT1(procedimiento, proceso, id_ejecucion, mensaje, estado, tiempo, campo, valor);
		try {
			sta=obj_con.prepareStatement(query);
			sta.setString(1, estado);
			sta.setString(2, mensaje);
			sta.setString(3, campo);
			sta.setString(4, valor);
			sta.setString(5, proceso);
			sta.setString(6, id_ejecucion);
			sta.setString(7, procedimiento);
			sta.setString(8, tiempo+"");
			sta.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
			String msg="QuerysStr::insertaRLT1::ERROR::No se pudo insertar en la RLT1";
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
			msg=e.toString();
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
		}
		
	}
	
	//NEW_OID
	public static String newoid(){
		String res="";
		try {
			String q="SELECT NEW_OID FROM DUAL";
			//ResultSet rs=sta.executeQuery(q);
			
			PreparedStatement psta = Ppal.obj_con.prepareStatement(q);
            ResultSet rs = psta.executeQuery();
			
			while(rs.next()){
				res=rs.getString(1);
			}
		} catch (Exception e) {
			// TODO: handle exception
			String msg="QuerysStr::newoid::ERROR::No se pudo obtener un oid";
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
			msg=e.toString();
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
		}
		return res;
	}
	public static String queryGeneraEstadisticas(){
		String res="  SELECT a.PROCESO, \n"
				  +"         a.ALD1_OID, \n"
				  +" 	     b.ESTADISTICA, \n"
				  +"         COUNT(*) AS CUENTA \n"
				  +"    FROM FT_T_ALG1 a \n"
				  +"    JOIN FT_T_ALD1 b \n"
				  +"      ON a.ALD1_OID = b.ALD1_OID \n"
				  +"   WHERE a.PROCESADO = 'N' \n"
				  +"     AND a.TIPO = 'MENSAJE' \n"
				  +"GROUP BY a.PROCESO, \n"
				  +"         a.ALD1_OID, \n"
				  +"         b.ESTADISTICA";
		//System.out.println(res);
		return res;
	}
}
