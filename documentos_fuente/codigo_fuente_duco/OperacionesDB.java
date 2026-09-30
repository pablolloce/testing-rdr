package com.bbva.kytl.extraccion.jdbc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Clase para operaciones de base de datos relacionadas con extracciones.
 * Gestiona la obtención de configuración y ejecución de queries de extracción.
 */
public class OperacionesDB {
	private static final Logger LOGGER = Logger.getLogger(OperacionesDB.class);
	private static final String ACTION_NAME_PREFIX = "Extraccion";
	private static final String ACTION_NAME_SUFFIX = ".sql";
	private static final int FETCH_SIZE = 5000;
	private static final int LOG_PROGRESS_INTERVAL = 10000;
	private static final int MAX_QUERY_LENGTH_FOR_TRACKING = 100;
	
	/**
	 * Obtiene la query de extracción desde ft_t_ate1
	 * @param connection Conexión a BD
	 * @param typeInfo Tipo de extracción (DUCOMASTERDATA, etc)
	 * @return Query SQL almacenada en CLOB_VALUE
	 */
	public String obtenerQueryExtraccion(Connection connection, String typeInfo) throws SQLException {
		String query = null;
		try {
			String actionName = ACTION_NAME_PREFIX + typeInfo + ACTION_NAME_SUFFIX;
			String sql = "SELECT clob_value FROM ft_t_ate1 WHERE ACTION_NME = ? AND DATA_STAT_TYP = 'ACTIVE'";
			
			marcadoQuery(connection, sql, "obtenerQueryExtraccion");
			
			query = ejecutarQueryParaObtenerString(connection, sql, actionName, "clob_value");
			
			if (query == null) {
				String error = "No se encontró query activa para ACTION_NME: " + actionName;
				LOGGER.error(error);
				throw new SQLException(error);
			}
			
			LOGGER.info("Query obtenida para ACTION_NME: " + actionName);
			
		} catch (SQLException e) {
			LOGGER.error("Error obteniendo query para tipo: " + typeInfo, e);
			throw e;
		}
		return query;
	}
	
	/**
	 * Obtiene el header/cabecera CSV desde ft_t_par1
	 * @param connection Conexión a BD
	 * @param typeInfo Tipo de extracción
	 * @return Header CSV
	 */
	public String obtenerHeader(Connection connection, String typeInfo) throws SQLException {
		String header = null;
		try {
			String actionName = ACTION_NAME_PREFIX + typeInfo + ACTION_NAME_SUFFIX;
			String sql = "SELECT PAR1_VALUE_CLOB FROM ft_t_par1 " +
			             "WHERE PARAMETER_CTXT_TYP = 'HEADER' " +
			             "AND ACT1_OID = (SELECT ACT1_OID FROM ft_t_ate1 WHERE ACTION_NME = ?) " +
			             "AND DATA_STAT_TYP = 'ACTIVE'";
			
			marcadoQuery(connection, sql, "obtenerHeader");
			
			header = ejecutarQueryParaObtenerString(connection, sql, actionName, "PAR1_VALUE_CLOB");
			
			LOGGER.info("Header obtenido para tipo: " + typeInfo);
			
		} catch (SQLException e) {
			LOGGER.error("Error obteniendo header para tipo: " + typeInfo, e);
			throw e;
		}
		return header;
	}
	
	/**
	 * Obtiene el nombre del fichero de salida desde ft_t_ate1
	 * @param connection Conexión a BD
	 * @param typeInfo Tipo de extracción
	 * @return Nombre del fichero de salida
	 */
	public String obtenerFicheroSalida(Connection connection, String typeInfo) throws SQLException {
		String fichero = null;
		try {
			String actionName = ACTION_NAME_PREFIX + typeInfo + ACTION_NAME_SUFFIX;
			String sql = "SELECT URL_OUTPUT_FILE FROM ft_t_ate1 WHERE ACTION_NME = ? AND DATA_STAT_TYP = 'ACTIVE'";

			marcadoQuery(connection, sql, "obtenerFicheroSalida");

			fichero = ejecutarQueryParaObtenerString(connection, sql, actionName, "URL_OUTPUT_FILE");

			if (fichero == null || fichero.isEmpty()) {
				String error = "No se encontró fichero de salida para ACTION_NME: " + actionName;
				LOGGER.error(error);
				throw new SQLException(error);
			}

			LOGGER.info("Ruta completa de fichero salida obtenida: " + fichero);

		} catch (SQLException e) {
			LOGGER.error("Error obteniendo fichero salida para tipo: " + typeInfo, e);
			throw e;
		}
		return fichero;
	}
	
	/**
	 * Ejecuta la query de extracción y escribe los resultados directamente al writer.
	 * @param connection Conexión a BD
	 * @param query Query SQL a ejecutar
	 * @param typeInfo Tipo de extracción (para logging)
	 * @param writer BufferedWriter donde se escriben las líneas
	 * @return Total de líneas escritas
	 * @throws SQLException Si hay error en BD
	 * @throws IOException Si hay error escribiendo
	 */
	public int ejecutarExtraccion(Connection connection, String query, String typeInfo, BufferedWriter writer) throws SQLException, IOException {
		int contador = 0;
		try {
			marcadoQuery(connection, query, "ejecutarExtraccion");
			
			LOGGER.info("Ejecutando extracción para: " + typeInfo);
			
			try (PreparedStatement statement = connection.prepareStatement(query)) {
				statement.setFetchSize(FETCH_SIZE); // Optimización: fetch en bloques
				
				try (ResultSet rs = statement.executeQuery()) {
					contador = escribirResultSet(rs, writer);
					LOGGER.info("Extracción completada. Total registros: " + contador);
				}
			}
			
		} catch (SQLException e) {
			LOGGER.error("Error ejecutando extracción para tipo: " + typeInfo, e);
			throw e;
		} catch (IOException e) {
			LOGGER.error("Error escribiendo resultados para tipo: " + typeInfo, e);
			throw e;
		}
		return contador;
	}
	
	/**
	 * Marca la query en Oracle para tracking usando DBMS_APPLICATION_INFO
	 * Permite identificar las queries en v$session de Oracle
	 * @param conexion Conexión a Oracle
	 * @param query Query SQL a marcar
	 * @param metodo Nombre del método que ejecuta la query
	 */
	public static void marcadoQuery(Connection conexion, String query, String metodo) {
		try (CallableStatement call = conexion.prepareCall(
			"begin DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); end;"
		)) {
			call.setString(1, "ExtraccionGenericaUnificada");
			int maxLength = Math.min(query.length(), MAX_QUERY_LENGTH_FOR_TRACKING);
			call.setString(2, query.substring(0, maxLength));
			call.execute();
		} catch (SQLException ex) {
			LOGGER.error("ExtraccionGenericaUnificada::" + metodo + "::Fallo marcado de queries", ex);
		}
	}
	
	/**
	 * Ejecuta una query SQL y obtiene un String del resultado
	 * @param connection Conexión a BD
	 * @param sql Query SQL con un parámetro
	 * @param paramValue Valor del parámetro
	 * @param columnName Nombre de la columna a obtener
	 * @return Valor de la columna o null si no hay resultados
	 */
	private static String ejecutarQueryParaObtenerString(Connection connection, String sql, String paramValue, String columnName) throws SQLException {
		String resultado = null;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, paramValue);
			
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					resultado = rs.getString(columnName);
				}
			}
		}
		return resultado;
	}
	
	/**
	 * Escribe el contenido del ResultSet directamente al writer.
	 * @param rs ResultSet a procesar
	 * @param writer BufferedWriter donde se escriben las líneas
	 * @return Total de líneas escritas
	 * @throws SQLException Si hay error leyendo del ResultSet
	 * @throws IOException Si hay error escribiendo
	 */
	private static int escribirResultSet(ResultSet rs, BufferedWriter writer) throws SQLException, IOException {
		int contador = 0;
		while (rs.next()) {
			String linea = rs.getString("RESULT");
			if (linea != null && !linea.isEmpty()) {
				writer.write(linea);
				writer.newLine();
				contador++;
				
				// Log progreso periódicamente
				if (contador % LOG_PROGRESS_INTERVAL == 0) {
					LOGGER.debug("Procesados " + contador + " registros");
				}
			}
		}
		return contador;
	}
}
