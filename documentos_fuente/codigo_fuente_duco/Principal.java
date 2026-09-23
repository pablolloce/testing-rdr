package com.bbva.kytl.extraccion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.bbva.kytl.extraccion.jdbc.ConexionDB;
import com.bbva.kytl.extraccion.jdbc.ConfiguracionCredenciales;
import com.bbva.kytl.extraccion.jdbc.OperacionesDB;

/**
 * Clase principal para extracción genérica unificada de datos.
 * Ejecuta una sola query que devuelve todos los datos, sin iteración.
 */
public class Principal {
	private static final Logger LOGGER = Logger.getLogger(Principal.class);
	
	// Índices de argumentos
	private static final int ARG_LOG_LEVEL = 0;
	private static final int ARG_LOG4J_CONFIG = 1;
	private static final int ARG_TYPE_INFO = 2;
	private static final int ARG_OUTPUT_PATH = 3;
	private static final int ARG_CREDENTIALS = 4;
	private static final int NUM_ARGS_REQUERIDOS = 5;
	
	public static void main(String[] args) {
		if (args == null || args.length < NUM_ARGS_REQUERIDOS) {
			throw new IllegalArgumentException("Se requieren " + NUM_ARGS_REQUERIDOS + " argumentos: <logLevel> <log4jConfig> <typeInfo> <outputDir> <credentialsFile>");
		}
		
		// Configurar logging (args[0]=log_level, args[1]=log4j_config)
		configurarLogging(args[ARG_LOG_LEVEL], args[ARG_LOG4J_CONFIG]);
		LOGGER.info("Args resumen: level=" + args[ARG_LOG_LEVEL] + ", typeInfo=" + args[ARG_TYPE_INFO] + ", outDir=" + args[ARG_OUTPUT_PATH] + ", creds=" + args[ARG_CREDENTIALS]);
		String typeInfo = args[ARG_TYPE_INFO];
		try {
			ConfiguracionCredenciales configCred = new ConfiguracionCredenciales(args[ARG_CREDENTIALS]);
			LOGGER.info("Credenciales: " + configCred.toString());
			ConexionDB.initialize(configCred);
			LOGGER.info("Conexión DB inicializada");
			ejecutarExtraccion(typeInfo);
		} catch (IOException | SQLException e) {
			LOGGER.error("Error fatal en extracción: " + typeInfo, e);
			throw new IllegalStateException("Error fatal en extracción: " + typeInfo, e);
		} catch (RuntimeException re) {
			LOGGER.error("Error inesperado en runtime", re);
			throw re;
		}
	}
	
	/**
	 * Configura el nivel de logging y el fichero de configuración
	 * @param logLevelStr Nivel de log como string (1=DEBUG, 2=INFO, 3=ERROR, 4=FATAL)
	 * @param log4jConfig Ruta al fichero log4j.properties
	 */
	private static void configurarLogging(String logLevelStr, String log4jConfig) {
		// Configurar Log4j primero
		PropertyConfigurator.configure(log4jConfig);
		
		Level level = Level.INFO;
		
		try {
			int logLevelInt = Integer.parseInt(logLevelStr);
			level = obtenerNivelLog(logLevelInt);
		} catch (NumberFormatException e) {
			LOGGER.warn("Formato de nivel de log inválido, usando INFO", e);
		}
		
		LOGGER.setLevel(level);
	}
	
	/**
	 * Convierte el nivel de log numérico a Level de Log4j
	 * @param logLevelInt Nivel numérico (1=DEBUG, 2=INFO, 3=ERROR, 4=FATAL)
	 * @return Level correspondiente
	 */
	private static Level obtenerNivelLog(int logLevelInt) {
		Level level;
		switch(logLevelInt) {
			case 1: 
				level = Level.DEBUG;
				break;
			case 2: 
				level = Level.INFO;
				break;
			case 3: 
				level = Level.ERROR;
				break;
			case 4: 
				level = Level.FATAL;
				break;
			default: 
				LOGGER.warn("Nivel de log inválido (" + logLevelInt + "), usando INFO");
				level = Level.INFO;
				break;
		}
		return level;
	}
	
	/**
	 * Ejecuta el proceso completo de extracción
	 * @param typeInfo Tipo de extracción a ejecutar (ej: DUCOMASTERDATA)
	 * @throws IOException Si hay error leyendo configuración o escribiendo fichero
	 * @throws SQLException Si hay error en operaciones de base de datos
	 */
	private static void ejecutarExtraccion(String typeInfo) throws IOException, SQLException {
		LOGGER.info("******** INICIO EXTRACCION GENERICA UNIFICADA: " + typeInfo + " ********");
		
		long startTime = System.currentTimeMillis();
		Connection con = null;
		
		try {
			// Obtener conexión
			con = ConexionDB.getConnect();
			OperacionesDB jdbc = new OperacionesDB();
			
			// Obtener configuración desde BD
			String query = jdbc.obtenerQueryExtraccion(con, typeInfo);
			if (query == null || query.isEmpty()) {
				throw new IllegalStateException("No se encontró query para tipo: " + typeInfo);
			}
			LOGGER.info("Query obtenida para tipo: " + typeInfo);
			
			String header = jdbc.obtenerHeader(con, typeInfo);
			String ficheroSalida = jdbc.obtenerFicheroSalida(con, typeInfo);
			
			if (ficheroSalida == null || ficheroSalida.isEmpty()) {
				throw new IllegalStateException("No se encontró nombre de fichero de salida para tipo: " + typeInfo);
			}
			
			// Usar la ruta completa proporcionada en base de datos (ficheroSalida ya es la ruta completa)
			String rutaCompleta = ficheroSalida;
			LOGGER.info("Fichero salida (ruta completa): " + rutaCompleta);
			
			// Ejecutar extracción y generar fichero (sin cargar en memoria)
			int totalRegistros = generarFicheroExtraccion(con, query, typeInfo, rutaCompleta, header);
			LOGGER.info("Total registros extraídos: " + totalRegistros);
			
			// Log resumen
			long endTime = System.currentTimeMillis();
			String tiempo = formatearTiempo(endTime - startTime);
			
			LOGGER.info("Proceso finalizado correctamente. Tiempo: " + tiempo);
			LOGGER.info("******** FIN EXTRACCION GENERICA UNIFICADA: " + typeInfo + " ********");
			
		} finally {
			cerrarConexion(con);
		}
	}
	
	/**
	 * Genera el fichero de extracción completo (temporal + publicación).
	 * Escribe directamente desde BD al fichero temporal y luego lo publica como definitivo.
	 * @param con Conexión a BD
	 * @param query Query de extracción a ejecutar
	 * @param typeInfo Tipo de extracción (para logging)
	 * @param rutaCompleta Ruta completa del fichero definitivo a generar
	 * @param header Cabecera CSV (puede ser null)
	 * @return total de registros escritos
	 * @throws IOException Si hay error creando directorios o escribiendo fichero
	 * @throws SQLException Si hay error ejecutando query
	 */
	private static int generarFicheroExtraccion(Connection con, String query, String typeInfo, String rutaCompleta, String header) throws IOException, SQLException {
		Path finalPath = Paths.get(rutaCompleta);
		Path tmpPath = Paths.get(rutaCompleta + ".tmp");
		
		crearDirectorioSiNoExiste(finalPath);
		
		LOGGER.info("Escribiendo fichero temporal: " + tmpPath);
		long startWrite = System.currentTimeMillis();
		
		int contador = escribirFicheroTemporal(con, query, typeInfo, tmpPath, header);
		
		long endWrite = System.currentTimeMillis();
		LOGGER.info("Fichero temporal completado. Tiempo: " + formatearTiempo(endWrite - startWrite));
		
		if (contador == 0) {
			LOGGER.warn("Sin registros extraídos, se genera fichero vacío.");
		}
		
		publicarFicheroDefinitivo(tmpPath, finalPath, contador);
		return contador;
	}
	
	/**
	 * Crea el directorio padre si no existe
	 * @param filePath Ruta del fichero
	 * @throws IOException Si no se puede crear el directorio
	 */
	private static void crearDirectorioSiNoExiste(Path filePath) throws IOException {
		File parent = filePath.toFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IOException("No se pudo crear el directorio: " + parent.getAbsolutePath());
		}
	}
	
	/**
	 * Escribe el fichero temporal con datos de extracción (opcionalmente con header)
	 * @param con Conexión a BD
	 * @param query Query de extracción
	 * @param typeInfo Tipo de extracción
	 * @param tmpPath Ruta del fichero temporal
	 * @param header Cabecera CSV (puede ser null)
	 * @return Número de registros escritos
	 * @throws SQLException Si hay error en BD
	 * @throws IOException Si hay error escribiendo
	 */
	private static int escribirFicheroTemporal(Connection con, String query, String typeInfo, Path tmpPath, String header) throws SQLException, IOException {
		int resultado = 0;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpPath.toFile()))) {
			// Escribir header si existe
			if (header != null && !header.isEmpty()) {
				bw.write(header);
				bw.newLine();
			}
			
			// Ejecutar extracción
			OperacionesDB jdbc = new OperacionesDB();
			resultado = jdbc.ejecutarExtraccion(con, query, typeInfo, bw);
			
		} catch (SQLException | IOException e) {
			LOGGER.error("Error escribiendo fichero, temporal corrupto: " + tmpPath, e);
			try {
				Files.deleteIfExists(tmpPath);
			} catch (IOException ex) {
				LOGGER.warn("No se pudo eliminar temporal tras error: " + tmpPath, ex);
			}
			throw e;
		}
		return resultado;
	}
	
	/**
	 * Publica el fichero temporal como definitivo usando rename atómico
	 * @param tmpPath Ruta del fichero temporal
	 * @param finalPath Ruta del fichero definitivo
	 * @param contador Número de líneas escritas
	 * @throws IOException Si hay error en el rename
	 */
	private static void publicarFicheroDefinitivo(Path tmpPath, Path finalPath, int contador) throws IOException {
		try {
			moverFichero(tmpPath, finalPath);
			logTamanoFichero(finalPath, contador);
		} catch (IOException e) {
			LOGGER.error("Error publicando fichero definitivo", e);
			throw e;
		}
	}
	
	/**
	 * Mueve el fichero usando rename atómico con fallback a REPLACE_EXISTING
	 * @param origen Ruta del fichero origen
	 * @param destino Ruta del fichero destino
	 * @throws IOException Si hay error en el movimiento
	 */
	private static void moverFichero(Path origen, Path destino) throws IOException {
		try {
			Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE);
			LOGGER.debug("Fichero publicado con rename atómico");
		} catch (IOException atomicEx) {
			LOGGER.debug("Atomic move no disponible, usando REPLACE_EXISTING: " + atomicEx.getMessage());
			Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	/**
	 * Registra el tamaño del fichero definitivo generado
	 * @param finalPath Ruta del fichero
	 * @param contador Número de líneas
	 */
	private static void logTamanoFichero(Path finalPath, int contador) {
		try {
			long sizeBytes = Files.size(finalPath);
			LOGGER.info("Fichero definitivo generado: " + finalPath + " (" + contador + " líneas, " + sizeBytes + " bytes)");
		} catch (IOException ex) {
			LOGGER.warn("No se pudo obtener tamaño de fichero: " + finalPath, ex);
		}
	}
	
	/**
	 * Cierra la conexión de forma segura
	 * @param con Conexión a cerrar (puede ser null)
	 */
	private static void cerrarConexion(Connection con) {
		if (con != null) {
			try {
				con.close();
				LOGGER.debug("Conexión cerrada correctamente");
			} catch (SQLException e) {
				LOGGER.error("Error cerrando conexión", e);
			}
		}
	}
	
	/**
	 * Formatea el tiempo transcurrido en formato legible
	 * @param milisegundos Tiempo en milisegundos
	 * @return Tiempo formateado como HH:MM:SS
	 */
	private static String formatearTiempo(long milisegundos) {
		long segundos = milisegundos / 1000L;
		long minutos = segundos / 60L;
		long horas = minutos / 60L;
		
		long segundosRestantes = segundos % 60L;
		long minutosRestantes = minutos % 60L;
		
		return String.format("%02d:%02d:%02d", horas, minutosRestantes, segundosRestantes);
	}
}
