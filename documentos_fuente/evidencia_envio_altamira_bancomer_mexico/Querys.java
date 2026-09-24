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

	private static String FLD_JOB_ID = null;
	public static String url;
	public static String user;
	public static String password;
	public static int cont_conexion = 0;

	public static ArrayList<String> insercionesRLT1_Proceso = new ArrayList<String>();
	public static ArrayList<String> insercionesRLT1_Reportes = new ArrayList<String>();
	public static ArrayList<String> updatesFAB1 = new ArrayList<String>();
	public static int contadorInsercionesRLT1 = 0;
	public static int contadorUpdatesFAB1 = 0;
	public static int contadorPL = 0;

	public Querys(Logger LOGGER){
		this.LOGGER=LOGGER;
	}

	// Método adaptado para ejecutar el proceso CONCLI con las columnas específicas
    public void executeCONCLI_Hilos(ArrayList<HashMap<String, String>> arr, Connection connection) {
        try {
            System.out.println("Total registros leídos: " + arr.size());

            for (HashMap<String, String> map : arr) {
                String numclien = map.get("VCH_DBC_COD_ALID");
                String rfc = map.get("VCH_DBC_XTI_RFC");
                String homonimi = map.get("VCH_DBC_XTI_HOMOCL");
                String accsec = map.get("VCH_DBC_COD_ACCTSEC");
                String accsecN = map.get("VCH_DBC_COD_ACCTSECN");

                System.out.println("Leído del fichero: numclien=" + numclien + ", rfc=" + rfc + ", homonimi=" + homonimi + ", accsec=" + accsec + ", accsecN=" + accsecN);

                String query = "{call CONCLMEX (?, ?, ?, ?, ?, ?)}";
                marcadoQuery(connection, query, "executeCONCLI_Hilos");

                // Primer llamada al procedimiento
                try (CallableStatement cs = connection.prepareCall(query)) {
                    cs.setString(1, numclien);
                    cs.setString(2, rfc);
                    cs.setString(3, homonimi);
                    cs.setString(4, accsec);
                    cs.setString(5, accsecN);
                    cs.setString(6, FLD_JOB_ID);

                    cs.execute();

                    System.out.println("Llamada al procedimiento CONCLMEX con Id: " + numclien);
                }

                // Segunda llamada al procedimiento
                try (CallableStatement cs = connection.prepareCall(query)) {
                    cs.setString(1, numclien);
                    cs.setString(2, rfc);
                    cs.setString(3, homonimi);
                    cs.setString(4, accsec);
                    cs.setString(5, accsecN);
                    cs.setString(6, FLD_JOB_ID);

                    cs.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Métodos adicionales para obtener y manejar datos
    public ArrayList<String> obtenerCLIs(Connection connection) {
        ArrayList<String> array = new ArrayList<>();
        String query = "SELECT DISTINCT FINS_ID CLI_ID FROM FT_T_FIID WHERE FINS_ID_CTXT_TYP = 'ALID' AND DATA_STAT_TYP = 'ACTIVE'";

        marcadoQuery(connection, query, "obtenerCLIs");
        try (Statement stmnt = connection.createStatement(); ResultSet rs = stmnt.executeQuery(query)) {
            while (rs.next()) {
                array.add(rs.getString("CLI_ID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }
    
    public void insertRLT1Conciliacion(ArrayList<String> noConcilia, String jobId, Connection connection, String msg) {
        String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID, JOB_ID, RLT_STATUS, MESSAGE_RLT, START_TMS, LAST_CHG_TMS) "
                                + "VALUES (NEW_OID, ?, 1, ?, SYSDATE, SYSDATE)";

        marcadoQuery(connection, insertTableSQL, "insertRLT1Conciliacion");
        try (PreparedStatement ps = connection.prepareStatement(insertTableSQL)) {
            for (String noconc : noConcilia) {
                ps.setString(1, jobId);
                ps.setString(2, msg + ": " + noconc);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

	public long crearJOB(String jobId, String Servicio, Connection connection) {
		Statement statement = null;

		String insertTableSQL = "Insert into FT_T_JBLG (JOB_ID, RQST_TRN_ID, JOB_STAT_TYP, JOB_START_TMS, JOB_END_TMS, TASK_TOT_CNT, TASK_CMPLTD_CNT, JOB_INPUT_TXT, "
				+ "JOB_CONFIG_TXT, RQST_CORR_ID, TASK_SUCCESS_CNT, TASK_FAILED_CNT, LAST_UPD_TMS, MAX_RECORD_SEQ_NUM, PRNT_JOB_ID, JOB_MSG_TYP, JOB_TME_TXT, JOB_TPS_CNT, "
				+ "TASK_PARTIAL_CNT, TASK_FILTERED_CNT, INSTANCE_ID, PEVL_OID) "
				+ "values ('" + jobId
				+ "',null,'OPEN  '"
				+ ",sysdate,sysdate"
				+ ",0,0,null,null,null,0,0"
				+ ",sysdate"
				+ ",null,null,'"
				+ Servicio
				+ "',null,0,0,0,null,null)";
		
		marcadoQuery(connection, insertTableSQL, "crearJOB");
		try {
			statement = connection.createStatement();
			statement.executeUpdate(insertTableSQL);

			System.out.println("Record is inserted into FT_T_JBLG");
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return System.currentTimeMillis();
	}

	public void cerrarJOB(String jobId, String Servicio, long inicio, Connection connection) {
		Statement statement = null;
		long fin = System.currentTimeMillis();
		Integer tot = (int) (fin - inicio);
		int h = (int) ((tot / (1000 * 60 * 60)) % 24);
		int m = (int) ((tot / (1000 * 60)) % 60) - h * 60;
		int s = (int) ((tot / 1000)) - m * 60;

		String updateTableSQL = "Update FT_T_JBLG set job_stat_typ = 'CLOSED', "
				+ "job_end_tms=sysdate,job_tme_txt='" + h + ":" + m + ":" + s + "' "
				+ "where job_id = '" + jobId + "'";
		
		marcadoQuery(connection, updateTableSQL, "cerrarJOB");
		try {
			statement = connection.createStatement();
			statement.executeUpdate(updateTableSQL);

			statement.close();
			System.out.println("Record is closed into FT_T_JBLG");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> obtenerIDs(Connection connection) {
		ArrayList<String> array = new ArrayList<String>();
		String query = null;
		try {
			query = "select distinct " +
                    "(select listagg(distinct fiid.fins_id,'|') within group(order by fiid.fins_id) " +
                    "from ft_t_fiid fiid, ft_t_firl firl " +
                    "where fiid.inst_mnem=firl.prnt_inst_mnem " +
                    "and firl.inst_mnem=fins.inst_mnem " +
                    "and trim(firl.finsrl_typ)=fist.stat_char_val_txt " +
                    "and fiid.fins_id_ctxt_typ='ALID' " +
                    "and fiid.data_stat_typ='ACTIVE' " +
                    "and fiid.fins_id not in ('38112087', '49027955', '49584427','J9488131', 'J9488087') " +
                    "and fiid.fins_id is not null) ALTAMIRA_MEX " +
                    "from ft_t_fins fins, ft_t_enfr enfr, ft_t_eerl eerl, ft_t_entr entr, ft_t_fist fist " +
                    "where enfr.finr_inst_mnem=fins.inst_mnem " +
                    "and eerl.org_id=enfr.org_id " +
                    "and entr.org_id=eerl.prnt_org_id " +
                    "and fist.inst_mnem=fins.inst_mnem " +
                    "and fist.stat_def_id='MAINROL ' " +
                    "and fist.data_stat_typ='ACTIVE' " +
                    "and eerl.rl_typ='BRANCH  ' " +
                    "and eerl.data_stat_typ='ACTIVE' " +
                    "and entr.org_id='1145' " +
                    "and entr.ent_typ='ENTRPRSE' " +
                    "and trim(enfr.finsrl_typ)=fist.stat_char_val_txt " +
                    "and enfr.data_stat_typ='ACTIVE' " +
                    "and fins.data_stat_typ!='INACTIVE'";

			marcadoQuery(connection, query, "obtenerIDs");
			PreparedStatement stmnt = connection.prepareStatement(query);
			ResultSet rs = null;

			rs = stmnt.executeQuery();

			while (rs.next()) {
				array.add(rs.getString("ALTAMIRA_MEX"));
			}
			rs.close();
			stmnt.close();

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "obtenerIDs::Fallo al ejecutar la siguiente modificacion. " + e.toString() + "\n" + query;
			System.out.println(msg);
			LOGGER.error(msg);
		}
		return array;
	}

	public String obtenerTiempo(String metodo, long start, long end) {
		long res = end - start;
		long second = res / 1000L % 60L;
		long minute = res / 60000L % 60L;
		long hour = res / 3600000L % 24L;
		String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, res);
		return time;
	}

	public void marcadoQuery(Connection conexion, String query, String metodo) {
		// marcado de query
		try {
			CallableStatement call = conexion.prepareCall("begin DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); end;");
			try {
				call.setString(1, "MexicoConciliacion");
				call.setString(2, query);
				call.execute();
			} finally {
				call.close();
			}
		} catch (SQLException ex) {
			String msg = "MexicoConciliacion::QueryExec::" + metodo + "::Fallo marcado de queries " + ex.toString() + "\n" + query;
			System.out.println(msg);
			LOGGER.error(msg);
		} // fin marcado de query
	}
}
