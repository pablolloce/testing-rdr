import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import jdbc.ConDB;
import jdbc.Querys;
import util.Utilidades;

public class ConciliacionMex {

	public static ArrayList<HashMap<String, String>> arr = new ArrayList<>();
	public static ArrayList<String> noConci = new ArrayList<>();
	public static ArrayList<String> errorConci = new ArrayList<>();
	public static Connection obj_con = null;
	public static ConDB obj_ConDB = null;
	public static Querys jdbc = null;

	static int i = 0;
	static int rango = 100;
	static int x = 0;
	static int y = 0;

	public static String FLD_JOB_ID = null;
	public static String msg_ins = "Codigo Mexico en RDR que no concilia en Mexico Altamira";
	public static String msg_err = "Codigo Mexico en RDR que no es valido";

	public static void main(String[] args) throws Exception {
		System.out.println("******************* INICIO CONCILIACION DE MEXICO *******************");

		if (args.length < 4) {
			System.err.println("Usage: java MexicoEnvio <logLevel> <logConfigFile> <arg3> <outputFileTemplate>");
			System.exit(1);
		}

		Logger LOGGER = Logger.getLogger(ConciliacionMex.class);

		System.out.println("Nivel de log recibido: " + args[0]);
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
		long start = System.currentTimeMillis();

		obj_ConDB = new ConDB();
		obj_ConDB.ObtenerCredenciales();
		obj_con = obj_ConDB.ObtenerConexion();

		jdbc = new Querys(LOGGER);

		ArrayList<String> CLIIdsFic = new ArrayList<>();
		ArrayList<String> CLIIds = new ArrayList<>();

		Utilidades ut = new Utilidades();
		ConciliacionMex.FLD_JOB_ID = ut.generateJOBID();

		FileInputStream fis = null;
		InputStreamReader fr = null;
		BufferedReader br = null;
		String linea = "";

		long inicio = jdbc.crearJOB(FLD_JOB_ID, "ConciliacionMex", obj_con);
		System.out.println("Se crea JOB");

		// Obten los codigos de Mexico de la base de datos
		CLIIds = jdbc.obtenerCLIs(obj_con);
		
		DateFormat dfAnio = new SimpleDateFormat("yyyy");
        DateFormat dfMes = new SimpleDateFormat("MM");
        DateFormat dfDia = new SimpleDateFormat("dd");

        Locale l = new Locale("es", "MX");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Mexico_City"), l);

        String anio = dfAnio.format(cal.getTime());
        String mes = dfMes.format(cal.getTime());
        String dia = dfDia.format(cal.getTime());

		System.out.println(" - COMIENZA LECTURA DEL FICHERO Y EJECUCION EN Altamira Mexico - ");
		File f = new File(args[3].replace("YYYY", anio).replace("MM", mes).replace("DD", dia));
		System.out.println("Intentando abrir el archivo: " + f.getAbsolutePath());
		if (f.exists()) {
			try {
				fis = new FileInputStream(f);
				fr = new InputStreamReader(fis, "ISO-8859-1");
				br = new BufferedReader(fr);

                // Verificamos si hay contenido en el archivo (cabecera incluida)
                if (!br.ready()) {
                    System.out.println("El fichero está vacío o no tiene cabecera. No se procede a la conciliación.");
                    errorConcilia(FLD_JOB_ID, "El fichero está vacío o no tiene cabecera. No se procede a la conciliación.");
                    return; // Detener el proceso si no hay contenido
                }

                // Intentamos leer la cabecera
                String cabecera = br.readLine();
                if (cabecera == null || cabecera.trim().isEmpty()) {
                    System.out.println("El fichero no tiene cabecera. No se procede a la conciliación.");
                    errorConcilia(FLD_JOB_ID, "El fichero no tiene cabecera. No se procede a la conciliación.");
                    return; // Detener el proceso si no hay cabecera
                }

                System.out.println("Cabecera leída: " + cabecera);

                // Verificamos si el archivo tiene más contenido después de la cabecera
                if (!br.ready()) {
                    System.out.println("El fichero solo contiene la cabecera pero no tiene datos. No se procede a la conciliación.");
                    errorConcilia(FLD_JOB_ID, "El fichero solo contiene la cabecera pero no tiene datos. No se procede a la conciliación.");
                    return; // Detener el proceso si solo hay cabecera
                }

                // Procesamos las líneas del fichero
                while ((linea = br.readLine()) != null) {
                    try {
                        if (linea.endsWith(";")) {
                            linea = linea + "NA";
                        }

                        String[] campos = linea.split(";");
                        
                        if (campos[campos.length - 1].isEmpty()) {
                            campos[campos.length - 1] = "NA";
                        }

                        // Validamos que la línea tenga al menos el número correcto de campos
                        if (campos.length < 30) { // Asegurando que la línea tenga todos los campos
                            System.out.println("Fallo en " + campos[0].trim() + " debido a longitud " + campos.length);
                            errorConci.add(campos[0].trim());
                            continue;
                        }

                        // Asignación de valores a las columnas clave
                        String numclien = campos[0].trim();  // numclien
                        String rfc = campos[5].trim();       // rfc
                        String homonimi = campos[6].trim();  // homonimi
                        String accsec = campos[28].trim();   // accsec
                        String accsecN = campos[29].trim();  // accsecN

                                // Conciliación con la base de datos
                                boolean encontrado = CLIIds.contains(numclien);
                                
                                if (!encontrado) {
                                    // Solo imprimir el mensaje de que no se encontró, pero no redirigir
                                    System.out.println("Cliente no encontrado en CLIIds: " + numclien);
                                }
                                
                                if (true) {
                                    
                                	HashMap<String, String> mapint = new HashMap<>();
                                    mapint.put("VCH_DBC_COD_ALID", numclien);
                                    mapint.put("VCH_DBC_XTI_RFC", rfc);
                                    mapint.put("VCH_DBC_XTI_HOMOCL", homonimi);
                                    mapint.put("VCH_DBC_COD_ACCTSEC", accsec);
                                    mapint.put("VCH_DBC_COD_ACCTSECN", accsecN);

							arr.add(mapint);
							i++;

							if (i % ConciliacionMex.rango == 0) {
								System.out.println("-------------------- " + i);
								ConciliacionMex.y = i;
								ConciliacionMex.x = i - ConciliacionMex.rango;
								Thread hilo = new Thread(() -> jdbc.executeCONCLI_Hilos(new ArrayList<>(ConciliacionMex.arr.subList(ConciliacionMex.x, ConciliacionMex.y)), obj_con));
								hilo.start();
							}

							CLIIdsFic.add(numclien);
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println("Línea con longitud errónea: " + linea);
						errorConcilia(FLD_JOB_ID, "ArrayIndexOutOfBoundsException: " + e.getMessage());
					} catch (Exception ex) {
						ex.printStackTrace();
						System.out.println("Excepcion: " + ex + " - " + linea);
						errorConcilia(FLD_JOB_ID, "Exception: " + ex.getMessage());
					}
				}

				if (arr.size() > 0) {
					Thread hilo = new Thread(() -> jdbc.executeCONCLI_Hilos(new ArrayList<>(ConciliacionMex.arr.subList(ConciliacionMex.y, ConciliacionMex.i)), obj_con));
					hilo.start();
					hilo.join();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("ERROR1");
				errorConcilia(FLD_JOB_ID, "FileNotFoundException: " + e.getMessage());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				System.out.println("ERROR2");
				errorConcilia(FLD_JOB_ID, "UnsupportedEncodingException: " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("ERROR3");
				errorConcilia(FLD_JOB_ID, "IOException: " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR4");
				errorConcilia(FLD_JOB_ID, "Exception: " + e.getMessage());
			}
		} else {
			System.out.println("El fichero no existe. No se realiza la concilicacion.  ");
			errorConcilia(FLD_JOB_ID, "File not found: " + f.getAbsolutePath());
		}

		jdbc.cerrarJOB(FLD_JOB_ID, "ConciliacionMex", inicio, obj_con);
		try {
			if (obj_con != null) {
				obj_con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("ERROR5");
			errorConcilia(FLD_JOB_ID, "SQLException: " + e.getMessage());
		}

		System.out.println("Se cierra el JOB de Mexico: " + FLD_JOB_ID);
		System.out.println("******************* FIN CONCILIACION DE Mexico *******************");
	}

	public static void errorConcilia(String jobId, String message) {
		jdbc.insertRLT1Conciliacion(errorConci, jobId, obj_con, message);
	}

	public static void noConcilia(String jobId, String message) {
		jdbc.insertRLT1Conciliacion(noConci, jobId, obj_con, message);
	}
}