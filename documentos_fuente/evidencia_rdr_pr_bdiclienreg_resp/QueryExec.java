package jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import main.Main;

/**
 * Clase que se encarga de ejecutar las querys recibidas en formato String.
 * 
 * @author XE83997
 *
 */
public class QueryExec {
	/**
	 * Función que recibe la conexión y una query y ejecuta esta última.
	 * 
	 * @param conexion
	 * @param query
	 * @return True o False
	 */
	public static boolean exeQuery(Connection conexion, String query) {
		boolean r = false;
		Statement st = null;
		try {
			st = conexion.createStatement();
			st.executeQuery(query);
			r = true;

		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::exeQuery::Fallo al ejecutar la siguiente modificacion. "
					+ e.toString() + "\n" + query;
			System.out.println(msg);
			Main.LOGGER.error(msg);
			r = false;
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				String msg = "AltaFondos_RDR::QueryExec::exeQuery::No se ha podido cerrar el Statement. " + e.toString()
						+ "\n" + query;
				System.out.println(msg);
				Main.LOGGER.error(msg);
				r = false;
			}

		}
		return r;
	}

	/**
	 * 
	 * Función que recibe la conexión y una query y ejecuta esta última,
	 * devolviendo el resultado en un Vector de array de String.
	 * 
	 * @param conexion
	 * @param query
	 * @return Varias columnas en un array
	 */
	public static Vector<String[]> queryToVector(Connection conexion, String query) {
		Vector<String[]> r = new Vector<String[]>();
		ResultSet rs = null;
		Statement st = null;
		try {
			st = conexion.createStatement();
			rs = st.executeQuery(query);
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

	/**
	 * Función que recibe la conexión y una query y ejecuta esta última,
	 * devolviendo el resultado en un Vector de String. Solo válido para querys
	 * de una única columna.
	 * 
	 * @param conexion
	 * @param query
	 * @return Una columna
	 */
	public static Vector<String> queryToVectorOneColumn(Connection conexion, String query) {
		Vector<String> r = new Vector<String>();
		ResultSet rs = null;
		Statement st = null;
		try {
			st = conexion.createStatement();
			rs = st.executeQuery(query);
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
	 * Procedimiento que se encarga de marcar mensajes de estado en la RLT1.
	 * 
	 * @param obj_con
	 * @param idDefAlert
	 * @param proceso
	 * @param campos
	 * @param delimitador
	 */
	public static void insertRLT1(Connection obj_con, String idDefAlert, String proceso, String campos,
			String delimitador) {
		CallableStatement cst = null;
		try {
			String msg = "AltaFondos_RDR::QueryExec::insertRLT1::Realizando llamada a PCK_GESTIONALERTAS.ADD_GESTIONALERTAS_MSG. Se añadirá la información del resultado.\n";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			cst = obj_con.prepareCall("{call PCK_GESTIONALERTAS.ADD_GESTIONALERTAS_MSG (?,?,?,?)}");
			cst.setString(1, idDefAlert);
			cst.setString(2, proceso);
			cst.setString(3, campos);
			cst.setString(4, delimitador);
			cst.execute();
			cst.close();
			System.out.println(idDefAlert + " " + proceso + " " + campos + " " + delimitador);
		} catch (Exception e) {
			String msg = "AltaFondos_RDR::QueryExec::insertRLT1::ERROR::" + e.toString() + "\n";
			System.out.println(msg);
			Main.LOGGER.error(msg);
		} finally {
			cst = null;
		}
	}
	public static String getNewOid(Statement s){
		try {
			String newOid = "";
			String qoid = QuerysStr.getNewOid();
			ResultSet rs = s.executeQuery(qoid);
			while (rs.next()){
				newOid = rs.getString(1);
			}
			rs.close();
			/*String msg = "QueryExec::getNewOid::generado Oid: '"+newOid+"'";
			System.out.println(msg);
			Main.LOGGER.error(msg);*/
			return newOid;
		} catch (Exception e) {
			// TODO: handle exception
			String msg = "QueryExec::getNewOid::ERROR::"+e.toString();
			System.out.println(msg);
			Main.LOGGER.error(msg);
			return null;
		}
		
	}
}
