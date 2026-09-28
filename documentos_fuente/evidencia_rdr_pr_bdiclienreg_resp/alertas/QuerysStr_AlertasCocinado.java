package jdbc;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;
import main.Ppal;

public class QuerysStr {
	public static String query_REP1(){
		//Solo se extraen reportes con algun usuario activo.
		String query = "SELECT a.REP1_OID,                      \n"
					 + "       a.TIPO,                          \n"
					 + "       a.PROCESO,                       \n"
					 + "       a.DESCRIPCION,                   \n"
					 + "       a.CABECERA,                      \n"
					 + "       a.RUTA,                          \n"
					 + "       a.SHORT_PROCESS,                 \n"
					 + "       a.QUERY,                         \n"
					 + "       a.EXCEL_TEMPLATE,                \n"
					 + "       a.EXCEL_SHEET,                   \n"
					 + "       (                                \n"
					 + "       SELECT COUNT(*)                  \n"
					 + "         FROM FT_T_ALR1 b               \n"
					 + "         JOIN FT_T_ALM1 c               \n"
					 + "           ON b.ALM1_OID = c.ALM1_OID   \n"
					 + "          AND c.DATA_STAT_TYP = 'ACTIVE'\n"
					 + "          AND c.MEDIO_ENVIO = 'EMAIL'   \n"
					 + "        WHERE b.DATA_STAT_TYP = 'ACTIVE'\n"
					 + "          AND b.PROCESO = a.PROCESO     \n"
					 + "       ) AS CUENTA_MAILS                \n"
					 + "  FROM FT_T_REP1 a                      \n"
					 + " WHERE a.DATA_STAT_TYP = 'ACTIVE'       \n"
					 + "   AND a.PROCESO IN (SELECT b.PROCESO   \n"
					 + "                       FROM FT_T_ALR1 b \n"
					 + "                      WHERE b.DATA_STAT_TYP = 'ACTIVE')";
		//System.out.println(query);
		return query;
	}
	public static String query_REP1_Filtrado(String proceso){
		String query = "SELECT a.REP1_OID,                      \n"
				 + "       a.TIPO,                          \n"
				 + "       a.PROCESO,                       \n"
				 + "       a.DESCRIPCION,                   \n"
				 + "       a.CABECERA,                      \n"
				 + "       a.RUTA,                          \n"
				 + "       a.SHORT_PROCESS,                 \n"
				 + "       a.QUERY,                         \n"
				 + "       a.EXCEL_TEMPLATE,                \n"
				 + "       a.EXCEL_SHEET,                   \n"
				 + "       (                                \n"
				 + "       SELECT COUNT(*)                  \n"
				 + "         FROM FT_T_ALR1 b               \n"
				 + "         JOIN FT_T_ALM1 c               \n"
				 + "           ON b.ALM1_OID = c.ALM1_OID   \n"
				 + "          AND c.DATA_STAT_TYP = 'ACTIVE'\n"
				 + "          AND c.MEDIO_ENVIO = 'EMAIL'   \n"
				 + "        WHERE b.DATA_STAT_TYP = 'ACTIVE'\n"
				 + "          AND b.PROCESO = a.PROCESO     \n"
				 + "       ) AS CUENTA_MAILS                \n"
				 + "  FROM FT_T_REP1 a                      \n"
				 + " WHERE a.DATA_STAT_TYP = 'ACTIVE'       \n"
				 + "   AND a.PROCESO IN (SELECT b.PROCESO   \n"
				 + "                       FROM FT_T_ALR1 b \n"
				 + "                      WHERE b.DATA_STAT_TYP = 'ACTIVE')"
			     + "   AND a.PROCESO = '"+proceso+"'";
		//System.out.println(query);
		return query;
	}
	
	public static String query_REP1_MarcaPending(String proceso){
		String query = "UPDATE FT_T_REP1 \n"
				     + "   SET SEND_PEND = 'Y' \n"
				     + " WHERE PROCESO = '"+proceso+"'";
		//System.out.println(query);
		return query;
	}
	
	public static String query_ALG1(String proceso){
		String query="SELECT a.* \n"
					+"  FROM FT_T_ALG1 a \n"
					+" WHERE a.PROCESO = '"+proceso+"' \n"
					+"   AND a.PROCESADO = 'N' \n"
					+" ORDER BY a.ALD1_OID";
		//System.out.println(query);
		return query;
	}
	
	//Marcado sobre la ALG1
	public static String queryMarcadoALG1(Vector<String>oids){
		String res="UPDATE FT_T_ALG1 \n"
				  +"   SET PROCESADO = 'S', \n"
				  +"       LAST_CHG_TMS = SYSDATE, \n"
				  +"       LAST_CHG_USR_ID = '"+QuerysConfig.marcaProceso+"' \n"
				  +" WHERE ALG1_OID IN ( \n";
		if(oids.size()>0){
			for(int i=0;i<oids.size()-1;i++){
				res+="                    '"+oids.get(i)+"', \n";
			}
			res+="                    '"+oids.get(oids.size()-1)+"')";
		}else{
			res+="'')";
		}
		//System.out.println(res);
		return res;
	}
	
	public static String query_ALU1_TiposEnvio(String proceso){
		//En fase 2 se recuperara de la ALM1.
		String query="SELECT b.TIPO_ENVIO \n"
				    +"  FROM FT_T_ALU1 a \n"
				    +"  JOIN FT_T_ALR1 b \n"
				    +"    ON a.ALU1_OID = b.ALU1_OID \n"
				    +"  JOIN FT_T_REP1 c \n"
				    +"    ON c.PROCESO = b.PROCESO \n"
				    +" WHERE a.DATA_STAT_TYP = 'ACTIVE' \n"
				    +"   AND b.DATA_STAT_TYP = 'ACTIVE' \n"
				    +"   AND c.DATA_STAT_TYP = 'ACTIVE' \n"
				    +"   AND b.PROCESO = '"+proceso+"' \n"
				    +" GROUP BY b.TIPO_ENVIO";
		//System.out.println(query);
		return query;
	}
	
	//Insert RLT1
	public static String insertRLT1(String procedimiento, String proceso, String id_ejecucion, String mensaje, String estado, double tiempo, String campo, String valor){
		
		String insert="INSERT INTO FT_T_RLT1 (RLT_OID, JOB_ID, TRN_ID, RECORD_SEQ_NUM, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_FIELD, "
                     +"RLT_PURP_TYP, DATA_SRC_APP, SRC_FIELD, SRC_VALUE, GS_FIELD, GS_VALUE, MAIN_ENTITY_NME, MAIN_ENTITY_ID, " 
                     +"START_TMS, END_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) \n"
                     +" VALUES (NEW_OID, null, null, 1, null, '"+estado+"', null, '"+mensaje+"', '"+campo+"', '"+valor+"', '"+proceso+"', '"+id_ejecucion+"', null, null, null, '"+procedimiento+"', '"+tiempo+"'	, "
                     +"SYSDATE, null, SYSDATE, 'GESTION_ALERTAS')";
		
		//System.out.println(insert);
		
		return insert;
	}
	
	public static void insertaRLT1(Statement sta, String procedimiento, String proceso, String id_ejecucion, String mensaje, String estado, double tiempo, String campo, String valor){
		String query=insertRLT1(procedimiento, proceso, id_ejecucion, mensaje, estado, tiempo, campo, valor);
		try {
			String msg="QuerysStr::insertaRLT1::Insertando mensaje en la RLT1";
			System.out.println(msg);
			Ppal.LOGGER.error(msg);
			sta.executeUpdate(query);
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
	public static String newoid(Statement sta){
		String res="";
		try {
			String q="SELECT NEW_OID FROM DUAL";
			ResultSet rs=sta.executeQuery(q);
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
}
