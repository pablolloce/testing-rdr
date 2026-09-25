package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import main.Main;

/**
 * Clase que se encarga de crear las querys y devolverlas en formato String.
 * 
 * @author XE83997
 *
 */
public class QuerysStr {
	public static String selectPeticionesPosibles() {
		return   "SELECT a.VND_RQST_OID,                                                                         \n"
				+"       a.VND_RQST_XREF_ID                                                                      \n"
				+"  FROM FT_T_VREQ a                                                                             \n"
				+"  JOIN FT_T_VREQ b                                                                             \n"
				+"    ON a.VND_RQST_OID = b.BASIC_VND_RQST_OID                                                   \n"
				+"   AND b.VND_RQST_XREF_ID_CTXT_TYP = 'FundLEI'                                                 \n"
				+"   AND b.VND_RQST_STAT_TYP = 'NEW_CLIENT'                                                      \n"
				+" WHERE a.VND_RQST_XREF_ID_CTXT_TYP = 'FILE_DATE'                                               \n"
				+"   AND a.VND_RQST_STAT_TYP = 'NEW_CLIENTS'                                                     \n"
				+"   AND a.VND_RQST_XREF_ID IN (                                                                 \n"
				+"                             SELECT y.UTD_ID AS HORA                                           \n"
				+"                               FROM FT_T_VREQ z                                                \n"
				+"                               JOIN FT_T_UTD1 y                                                \n"
				+"                                 ON z.VND_RQST_OID = y.UTD_EXT_ID                              \n"
				+"                                AND y.UTD_USAGE_TYP = 'FIELD_RESP'                             \n"
				+"                                AND y.UTD_ID_PURP_TYP = 'HORA'                                 \n"
				+"                              WHERE z.VND_RQST_XREF_ID_CTXT_TYP IN ('CLIENTELABDI_ALTAS')      \n"
				+"                              )                                                                \n"
				+" GROUP BY a.VND_RQST_OID, a.VND_RQST_XREF_ID                                                   \n";
	}
	
	
	
	public static Vector<String[]> selectFundsPendientes(String oidPeticion, Connection conexion) {
		String query=       "SELECT b.VND_RQST_OID,                                     \n" 
		            +"       b.VND_RQST_XREF_ID                                  \n"
		            +"  FROM FT_T_VREQ a                                         \n"
		            +"  JOIN FT_T_VREQ b                                         \n"
		            +"    ON a.VND_RQST_OID = b.BASIC_VND_RQST_OID               \n"
		            +"   AND b.VND_RQST_XREF_ID_CTXT_TYP = 'FundLEI'             \n"
		            +"   AND b.VND_RQST_STAT_TYP = 'NEW_CLIENT'                  \n"
		            +"  LEFT JOIN FT_T_VREQ c                                    \n"
		            +"    ON c.VND_RQST_XREF_ID = b.VND_RQST_XREF_ID             \n"
		            +"   AND c.VND_RQST_XREF_ID_CTXT_TYP = 'LEI_REQUEST'         \n"
		            +"   AND c.VND_RQST_STAT_TYP = 'NEW_CLIENTS'                 \n"
		            +" WHERE a.VND_RQST_XREF_ID_CTXT_TYP = 'FILE_DATE'           \n"
		            +"   AND a.VND_RQST_OID = ?                  \n"
		            +" GROUP BY b.VND_RQST_OID, b.VND_RQST_XREF_ID               \n";
		
		Vector<String[]> r = new Vector<String[]>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, oidPeticion);
			rs = st.executeQuery();
			int nCols = rs.getMetaData().getColumnCount();
			while (rs.next()) {
				String[] x = new String[nCols];
				for (int i = 0; i < nCols; i++) {
					x[i] = rs.getString(i + 1);
				}
				r.add(x);
			}
		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::queryToVector::Fallo al ejecutar la siguiente modificacion. "
					+ e.toString() + "\n" + query;
			System.out.println(msg);
			Main.LOGGER.error(msg);
		} finally {
			try {
				rs.close();
				st.close();
			} catch (SQLException e) {
				String msg = "AltaFondos_RDR::QueryExec::queryToVector::No se ha podido cerrar el Statement y Resultset. "
						+ e.toString() + "\n" + query;
				System.out.println(msg);
				Main.LOGGER.error(msg);
			}

		}
		return r;
	}
	
	public static Vector<String> selectRespuestaBDI(String LEI, String HORA, Connection conexion){
		
		String query=   " SELECT z.VND_RQST_OID                                        \n"
				+"   FROM FT_T_VREQ z                                           \n"
				+"   JOIN FT_T_UTD1 y                                           \n"
				+"     ON z.VND_RQST_OID = y.UTD_EXT_ID                         \n"
				+"    AND y.UTD_USAGE_TYP = 'FIELD_RESP'                        \n"
				+"    AND y.UTD_ID_PURP_TYP = 'HORA'                            \n"
				+"   JOIN FT_T_UTD1 x                                           \n"
				+"     ON z.VND_RQST_OID = x.UTD_EXT_ID                         \n"
				+"    AND x.UTD_USAGE_TYP = 'FIELD_RESP'                        \n"
				+"    AND x.UTD_ID_PURP_TYP = 'LEI'                             \n"
				+"  WHERE z.VND_RQST_XREF_ID_CTXT_TYP IN ('CLIENTELABDI_ALTAS') \n"
				+"    AND y.UTD_ID = ?                                 \n"
				+"    AND x.UTD_ID = ?                                  \n"
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
	
	public static Vector<String> getStatusVREQ (String oid, Connection conexion){
		String query= "SELECT VND_RQST_STAT_TYP            \n"
			  +"  FROM FT_T_VREQ                    \n"
			  +" WHERE VND_RQST_OID = ?     \n";
		Vector<String> r = new Vector<String>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, oid);
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
	
	public static void updateVREQStatusByOid(String oid, String stat, String user, Connection conexion) {
		String q= "UPDATE FT_T_VREQ                                    \n"
				  +"   SET VND_RQST_STAT_TYP = ?      ,    \n"
				  +"       LAST_CHG_USR_ID = ?        ,    \n"
				  +"       LAST_CHG_TMS = SYSDATE                   \n"
				  +" WHERE VND_RQST_OID      = ?            \n";
		
		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(q);
			preparedStatement.setString(1, stat);
			preparedStatement.setString(2, user);
			preparedStatement.setString(3, oid);
			preparedStatement .executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public static void updateVREQDescripByOid(String oid, String stat, String user, String descrip, Connection conexion) {
		String q= "UPDATE FT_T_VREQ                                    \n"
				  +"   SET VND_RQST_STAT_TYP = ?      ,    \n"
				  +"       LAST_CHG_USR_ID = ?        ,    \n"
				  +"       LAST_CHG_TMS = SYSDATE              ,    \n"
				  +"       VND_RQST_STAT_TXT = ?        \n"
				  +" WHERE VND_RQST_OID      = ?            \n";
		
		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(q);
			preparedStatement.setString(1, stat);
			preparedStatement.setString(2, user);
			preparedStatement.setString(3, descrip);
			preparedStatement.setString(4, oid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public static Vector<String[]> descargaAtributosRespuesta(String oid, Connection conexion ){
		String query= "SELECT a.UTD_ID_PURP_TYP,                     \n"
			  +"       a.UTD_ID                               \n"
			  +"  FROM FT_T_UTD1 a                            \n"
			  +" WHERE a.UTD_USAGE_TYP = 'FIELD_RESP'         \n"
			  +"   AND a.UTD_EXT_ID = ?               \n";
		
		Vector<String[]> r = new Vector<String[]>();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, oid);	
			rs = st.executeQuery();
			int nCols = rs.getMetaData().getColumnCount();
			while (rs.next()) {
				String[] x = new String[nCols];
				for (int i = 0; i < nCols; i++) {
					x[i] = rs.getString(i + 1);
				}
				r.add(x);
			}
		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::queryToVector::Fallo al ejecutar la siguiente modificacion. "
					+ e.toString() + "\n" + query;
			System.out.println(msg);
			Main.LOGGER.error(msg);
		} finally {
			try {
				rs.close();
				st.close();
			} catch (SQLException e) {
				String msg = "AltaFondos_RDR::QueryExec::queryToVector::No se ha podido cerrar el Statement y Resultset. "
						+ e.toString() + "\n" + query;
				System.out.println(msg);
				Main.LOGGER.error(msg);
			}

		}
		return r;
	}
	public static String selectFondosAtributos(String vreqOid){
		return "SELECT UTD_ID_PURP_TYP, UTD_ID FROM FT_T_UTD1 WHERE UTD_EXT_ID = '"+vreqOid+"'";
	}
	
	public static String selectDuplicateMurexStar (String codTesoreria, Connection conexion){
		String query ="select listagg(distinct finsrl_id_ctxt_typ,',') "
				+ "from ft_t_frid "
				+ "where data_stat_Typ='ACTIVE' "
				+ "and end_tms is null "
				+ "and finsrl_id_ctxt_typ in ('MUREXID','STARID') "
				+ "and finr_id=?";
		ResultSet rs = null;
		PreparedStatement st = null;
		String x = "";
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, codTesoreria);
			rs = st.executeQuery();
			while (rs.next()) {
				x = rs.getString(1);
			}

		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::selectDuplicateMurexStar::Fallo al ejecutar la siguiente select. "
					+ e.toString() + "\n" + query;
			System.out.println(msg);
			Main.LOGGER.error(msg);
		} finally {
			try {
				rs.close();
				st.close();
			} catch (SQLException e) {
				String msg = "AltaFondos_RDR::QueryExec::selectDuplicateMurexStar::No se ha podido cerrar el Statement y Resultset. "
						+ e.toString() + "\n" + query;
				System.out.println(msg);
				Main.LOGGER.error(msg);
			}

		}
		return x;
	}

	/*public static String insertVREQ_BDIClient_Req(String oid, String LEI){
		return "INSERT INTO FT_T_VREQ (VND_RQST_OID             ,   \n"
			  +"                       VND_RQST_XREF_ID_CTXT_TYP,   \n"
			  +"                       VND_RQST_XREF_ID         ,   \n"
			  +"                       VND_RQST_STAT_TYP        ,   \n"
			  +"                       LAST_CHG_TMS             ,   \n"
			  +"                       LAST_CHG_USR_ID          )   \n"
			  +"  VALUES ('"+oid+"'              ,                  \n"
			  +"         'CLIENTELABDI_ALTAS'    ,                  \n"
			  +"         '"+LEI+"'               ,                  \n"
			  +"         'PENDING'               ,                  \n"
			  +"         SYSDATE                 ,                  \n"
			  +"         'INVESTORS_CLIENTREG_RESP')                \n";
	}*/
	public static void insertVREQ_LEIReg_Req(String oid, String LEI, Connection conexion){
		String query= "INSERT INTO FT_T_VREQ (VND_RQST_OID             ,   \n"
			  +"                       VND_RQST_XREF_ID_CTXT_TYP,   \n"
			  +"                       VND_RQST_XREF_ID         ,   \n"
			  +"                       VND_RQST_STAT_TYP        ,   \n"
			  +"                       LAST_CHG_TMS             ,   \n"
			  +"                       LAST_CHG_USR_ID          )   \n"
			  +"  VALUES (?                 ,               \n"
			  +"         'LEI_REGISTER'             ,               \n"
			  +"         ?                  ,               \n"
			  +"         'PENDING'                  ,               \n"
			  +"         SYSDATE                    ,               \n"
			  +"         'INVESTORS_CLIENTREG_RESP' )               \n";
		
		PreparedStatement preparedStatement;
		try {
			preparedStatement = conexion.prepareStatement(query);
			preparedStatement.setString(1, oid);
			preparedStatement.setString(2, LEI);

			preparedStatement.executeUpdate();
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
		       q+= " VALUES (NEW_OID, ?, ?, 1, ?, ?, SYSDATE, 'INVESTORS_CLIENTREG_RESP', ?, 'ACTIVE', SYSDATE)";

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
	/**
	 * @param legalEntityIndicator
	 * @return Obtiene la fecha de inicio de vigencia del LEI introducido.
	 */
	public static Vector<String> obtenerFechaInicioVigenciaLEI(String legalEntityIndicator, Connection conexion) {
		String query= "SELECT TO_CHAR(REGISTRATION_DATE, 'yyyy-MM-dd') AS REG_DATE                    \n" 
	         + "  FROM FT_T_LEI1                                                               \n" 
			 + " WHERE DATA_STAT_TYP='ACTIVE'                                                  \n" 
	         + "   AND LEI = ?                                    \n";
		Vector<String> r = new Vector<String>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, legalEntityIndicator);
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

	/**
	 * @param legalEntityIndicator
	 * @return Obtiene la fecha de fin de vigencia del LEI introducido.
	 */
	public static Vector<String> obtenerFechaFinVigenciaLEI(String legalEntityIndicator, Connection conexion) {
		String query= "SELECT TO_CHAR(NEXT_RENEWAL_DATE, 'yyyy-MM-dd') AS REN_DATE                    \n" 
	         + "  FROM FT_T_LEI1                                                               \n" 
			 + " WHERE DATA_STAT_TYP='ACTIVE'                                                  \n" 
	         + "   AND LEI = ?                                    \n";
		Vector<String> r = new Vector<String>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conexion.prepareStatement(query);
			st.setString(1, legalEntityIndicator);
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
}
