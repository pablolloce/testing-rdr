package main;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import jdbc.ConDB;
import peticiones.Peticiones;

//args[0] - Nivel del logger
//args[1] - Ruta del properties para configurar el logger.
//args[2] - Ruta de salida del csv
//args[3] - Mensaje DCS o NODCS
/**
 * Este proceso se encarga de generar un csv en formato cargador de contrapartidas
 * para todos los fondos pendientes de carga
 * Estado origen:    ALTA_FONDO_PEND
 * Estado salida ok: GENERATED_CSV_LINE
 * Estado salida ko: ERROR_CSV_LINE_GEN
 * 
 * @author NFQ
 *
 */
public class Main {
	public final static Logger LOGGER = Logger.getLogger(Main.class);
	public static Connection obj_con = null;
	public static ConDB obj_ConDB = null;
	private static Statement stmt = null;
	private static String carpetaSalida = "";

	/**
	 * Función principal que recibe las rutas de los ficheros por argumentos.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		String msg = "";
		boolean status = configuraDByLog(args);
		if (status == false) {
			msg = "AltaFondos_Genera_csv::Main::main::ERROR::Fallo al configurar la base de datos y el log.";
			System.out.println(msg);
			LOGGER.error(msg);
			return;
		}
		
		msg = "AltaFondos_Genera_csv::main::Comienza el proceso.";
		System.out.println(msg);
		LOGGER.info(msg);
		
		Peticiones p = new Peticiones(obj_con, carpetaSalida);
		p.procesaPeticiones(args[3]);
		p.generaCSVAltaFondos();
		
		cierraBBDD();
		
		msg = "AltaFondos_Genera_csv::main::Finaliza el proceso.";
		System.out.println(msg);
		LOGGER.info(msg);
	}

	/**
	 * Función encargada de configurar el log y la base de datos.
	 * 
	 * @param args
	 * @return
	 */
	public static boolean configuraDByLog(String[] args) {
		try {
			/****** CONFIGURACION LOG4J ***********/
			Level level = Level.INFO;

			switch (Integer.parseInt(args[0])) {
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
			}
			LOGGER.setLevel(level);

			PropertyConfigurator.configure(args[1]);
			
			carpetaSalida = args[2];

			/****** FIN CONFIGURACION LOG4J ************/

			/******************************
			 * INICIALIZACION DE CONEXIONES
			 *******************************************************/
			obj_ConDB = new ConDB();
			obj_ConDB.ObtenerCredenciales();
			obj_con = obj_ConDB.ObtenerConexion();
			/******************************
			 * INICIALIZACION DE STATEMENTS
			 *******************************************************/
			try {
				stmt = obj_con.createStatement();
				System.out.println("**** AltaFondos_Genera_csv::Configurada conexion  ****");
				LOGGER.error("**** AltaFondos_Genera_csv::Configurada conexion  ****");
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		} catch (Exception e) {
			System.out.println("**** AltaFondos_Genera_csv::ERROR::Fallo al configurar  ****");
			LOGGER.error("**** AltaFondos_Genera_csv::ERROR::Fallo al configurar  ****");
			System.out.println(e.toString());
			LOGGER.error(e.toString());
			return false;
		}

	}

	/**
	 * Método encargado de cerrar la conexión con la base de datos.
	 */
	private static void cierraBBDD() {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (obj_con != null) {
				obj_con.close();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.gc();
	}

}