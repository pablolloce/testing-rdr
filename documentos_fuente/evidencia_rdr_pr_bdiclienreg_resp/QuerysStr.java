package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import main.Main;

/**
 * Clase que se encarga de crear las querys y devolverlas en formato String.
 * 
 * @author XE83997
 *
 */
public class QuerysStr {
	public static String selectClientesAltaPending() {
		return   "SELECT a.VND_RQST_OID,                                           \n"
				+"       a.VND_RQST_XREF_ID                                        \n"
				+"  FROM FT_T_VREQ a                                               \n"
				+" WHERE a.VND_RQST_XREF_ID_CTXT_TYP = 'CLIENTELABDI_ALTAS'        \n"
				+"   AND a.VND_RQST_STAT_TYP = 'PENDING'                           \n";
	}

	public static Vector<String> identificaCliente(String LEI, String HORA, Connection conexion) {
		String query =  "SELECT a.VND_RQST_OID,                                          \n"
				+"       b.UTD_ID AS HORA,                                        \n"
				+"       c.UTD_ID AS LEI                                          \n"
				+"  FROM FT_T_VREQ a                                              \n"
				+"  JOIN FT_T_UTD1 b                                              \n"
				+"    ON a.VND_RQST_OID = b.UTD_EXT_ID                            \n"
				+"   AND b.UTD_USAGE_TYP = 'FIELD'                                \n"
				+"   AND b.UTD_ID_PURP_TYP = 'HORA'                               \n"
				+"  JOIN FT_T_UTD1 c                                              \n"
				+"    ON a.VND_RQST_OID = c.UTD_EXT_ID                            \n"
				+"   AND c.UTD_USAGE_TYP = 'FIELD'                                \n"
				+"   AND c.UTD_ID_PURP_TYP = 'LEI'                                \n"
				+" WHERE a.VND_RQST_XREF_ID_CTXT_TYP = 'CLIENTELABDI_ALTAS'       \n"
				+"   AND a.VND_RQST_STAT_TYP = 'BDI_LINE_SENT'                    \n"
				+"   AND b.UTD_ID = ?                                    \n"
				+"   AND c.UTD_ID = ?                                    \n"
				;

		Vector<String> r = new Vector<String>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, HORA);
			st.setString(2, LEI);
			rs = st.executeQuery();
			while (rs.next()) {
				//System.out.println("recorre...");
				String x = rs.getString(1);
				if(x!=null){
					x=x.trim();
				}
				//System.out.println("recuperado...");
				r.add(x);
				//System.out.println("incluye...");
			}

		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::queryToVectorOneColumn::Fallo al ejecutar la siguiente modificacion. "
					+ e.toString() + "\n" + query;
			System.out.println(msg);
			Main.LOGGER.error(msg);
		} finally {
			try {
				rs.close();
				st.close();
			} catch (SQLException e) {
				String msg = "AltaFondos_RDR::QueryExec::queryToVectorOneColumn::No se ha podido cerrar el Statement y Resultset. "
						+ e.toString() + "\n" + query;
				System.out.println(msg);
				Main.LOGGER.error(msg);
			}

		}
		return r;
	}

	public static void updateVREQDescripByOid(String oid, String stat, String user, String descrip, Connection conexion) {
		String query= "UPDATE FT_T_VREQ                 \n"
				+"   SET VND_RQST_STAT_TYP = ?      , \n"
				+"       LAST_CHG_USR_ID = ?        , \n"
				+"       LAST_CHG_TMS = SYSDATE   ,   \n"
				+"       VND_RQST_STAT_TXT = ?        \n"
				+" WHERE VND_RQST_OID      = ?        \n";

		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(query);
			preparedStatement.setString(1, stat);
			preparedStatement.setString(2, user);
			preparedStatement.setString(3, descrip);
			preparedStatement.setString(4, oid);
			preparedStatement .executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public static void updateVREQClientesSinRespuesta(String date, String user, String desc, Connection conexion){
		String query=    "UPDATE FT_T_VREQ                                                                       \n"    
				+"   SET VND_RQST_STAT_TYP = 'NO_RESPONSE',                                              \n"    
				+"       LAST_CHG_TMS = SYSDATE,                                                         \n"    
				+"       VND_RQST_STAT_TXT = ?,                                                 \n"    
				+"       LAST_CHG_USR_ID = ?                                                    \n"    
				+" WHERE VND_RQST_OID IN (                                                               \n"    
				+"                        SELECT VND_RQST_OID                                            \n"    
				+"                          FROM FT_T_VREQ b                                             \n"    
				+"                          JOIN FT_T_UTD1 a                                             \n"    
				+"                            ON a.UTD_EXT_ID = b.VND_RQST_OID                           \n"    
				+"                         WHERE a.UTD_ID_PURP_TYP = 'HORA'                              \n"    
				+"                           AND a.UTD_ID = ?                                   \n"    
				+"                           AND b.VND_RQST_XREF_ID_CTXT_TYP = 'CLIENTELABDI_ALTAS'      \n"    
				+"                           AND b.VND_RQST_STAT_TYP = 'BDI_LINE_SENT'                   \n"    
				+"                        )                                                              \n";    

		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(query);
			preparedStatement.setString(1, desc);
			preparedStatement.setString(2, user);
			preparedStatement.setString(3, date);
			preparedStatement .executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public static String getNewOid() {
		return "SELECT NEW_OID FROM DUAL";
	}
	public static void insertUTD1FundParam(String oidRef, String usageTyp, String key, String val, String src, Connection conexion){
		String q = "INSERT INTO FT_T_UTD1 (UTD_OID, UTD_EXT_ID, UTD_USAGE_TYP, REG_NUM, UTD_ID_PURP_TYP, UTD_ID, LAST_CHG_TMS, LAST_CHG_USR_ID, DATA_SRC_ID, DATA_STAT_TYP, START_TMS)";
		q+= " VALUES (NEW_OID, ?, ?, 1, ?, ?, SYSDATE, 'INVESTORSPLAN_FUNDS', ?, 'ACTIVE', SYSDATE)";
		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(q);
			preparedStatement.setString(1, oidRef);
			preparedStatement.setString(2, usageTyp);
			preparedStatement.setString(3, key);
			preparedStatement.setString(4, val);
			preparedStatement.setString(5, src);
			preparedStatement .executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}    
	}

}
