import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;		 
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bbva.kytl.services.SHIVAToken;

import jdbc.ConDB;
import jdbc.Querys;
import util.Utils;

// La logica de esta clase se basa en leer el fichero, mientras se va leyendo, se va volcando en un array. 
// Se generan sublistas de un rango definido en una variable estatica.
// Esta sublista se mana a ejecucion con un hilo nuevo.

// Se realiza con Multihilo y Pool de conexiones

public class ColombiaConciliacion {

	public static ArrayList<HashMap<String,String>> arr = new ArrayList<HashMap<String,String>>();

	public static Connection obj_con=null;
	public static ConDB obj_ConDB=null;

	static int i=0; 	static int rango=10;	    
	static int x=0;  	static int y=0;	  

	static String FLD_JOB_ID=null;

	static Logger LOGGER = Logger.getLogger(ColombiaConciliacion.class);

	public static void main(String[] args) throws Exception {
		Querys objQuery = null;
		String rutaFichero;
		String rutaFicheroDES;

		ArrayList<String> IDs = null;

		Level level = Level.INFO;
		switch(Integer.parseInt(args[0])) {
		case 1:		level = Level.DEBUG;	break;
		case 2:		level = Level.INFO;		break;
		case 3:		level = Level.ERROR;	break;
		case 4:		level = Level.FATAL;	}

		LOGGER.setLevel(level);
		PropertyConfigurator.configure(args[1]);
		long start = System.currentTimeMillis();

		obj_ConDB=new ConDB();			
		obj_ConDB.ObtenerCredenciales();			
		obj_con=obj_ConDB.ObtenerConexion();

		objQuery = new Querys(LOGGER);
		FLD_JOB_ID = Utils.generateJOBID();

		long inicio = objQuery.crearJOB(FLD_JOB_ID, "COLOMBIA", obj_con);

		DateFormat dfAnio = new SimpleDateFormat("yyyy");
		DateFormat dfMes = new SimpleDateFormat("MM");
		DateFormat dfDia = new SimpleDateFormat("dd");

		Locale l = new Locale("es","CO");
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"),l);

		//String anio = dfAnio.format(cal.getTime());
		//String mes = dfMes.format(cal.getTime());
		////String dia = (Integer.parseInt(dfDia.format(cal.getTime()))-1)+"";
		//String dia = String.format("%02d", Integer.parseInt(dfDia.format(cal.getTime()))-1);
		
		Date someDate = new Date();
		 
		Date newDate = new Date(someDate.getTime() + TimeUnit.DAYS.toMillis( -1 ));
											 
										   
																	  
																					  
		
		String anio = dfAnio.format(newDate.getTime());
		String mes = dfMes.format(newDate.getTime());
		String dia =  dfDia.format(newDate.getTime());						
		rutaFichero = args[3].replace("YYYY", anio).replace("MM", mes).replace("DD",dia);

		rutaFicheroDES = rutaFichero.substring(0, rutaFichero.length() - 4) + "_DES" + rutaFichero.substring(rutaFichero.length() - 4);
		
		System.out.println("rutaFichero " + rutaFichero);
		LOGGER.error("rutaFichero " + rutaFichero);								   
		//TODO: Establecer conexion con XMAS y recuperar la llave
		SHIVAToken.loadSHIVAData(args[4],false, null);
		String ShivaToken = SHIVAToken.getToken();

		String GET_UTL_SHIVA = objQuery.getJuncShiva(obj_con);
		System.out.println("GET_UTL_SHIVA " + GET_UTL_SHIVA);

		String url = SHIVAToken.getUrlShiva().concat(GET_UTL_SHIVA);

		String llave1 = null;

		System.out.println("ShivaToken " + ShivaToken);
		LOGGER.error("ShivaToken " + ShivaToken);

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Bearer " + ShivaToken);

			LOGGER.error("URL " + url);
			System.out.println("URL " + url);
			String result  = null;

			try {
				int status = con.getResponseCode();
				LOGGER.error("-------- status " + status);
				System.out.println("-------- status " + status);
				switch (status) {
				case 200:
				case 201:
					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line+"\n");
					}
					br.close();
					result = sb.toString();
				}

			} catch (MalformedURLException ex) {
				LOGGER.error("result " + ex.getMessage());
				System.out.println("result " + ex.getMessage());
			} catch (IOException ex) {
				LOGGER.error("result " + ex.getMessage());
				System.out.println("result " + ex.getMessage());
			} finally {
				if (con != null) {
					try {
						con.disconnect();
					} catch (Exception ex) {
						LOGGER.error("result " + ex.getMessage());
						System.out.println("result " + ex.getMessage());
					}
				}
			}
			if(result != null  ){
				//LOGGER.error("result " + result);
				//System.out.println("result " + result);

				JSONParser jsonParser = new JSONParser();
				JSONObject objJson = (JSONObject) jsonParser.parse(result);
				llave1  = (String) objJson.get("result");
			}

		}catch (Exception e) {
			LOGGER.error("Error creating http connection " + e);
			System.out.println("Error creating http connection " + e);
		}

		String llave2 = objQuery.getLlave2(obj_con);

		//LOGGER.error("llave2 " + llave2);
		//System.out.println("llave2 " + llave2);


		if(llave1 != null && llave2 != null ){
			Utils objUtil = new Utils();		

			FileInputStream fis = null;	    InputStreamReader fr = null;	    BufferedReader br = null;	    String linea = "";

			//TODO: Adaptar el metodo decrypt para introducir la clave o claves recibidas desde XMAS
			// Desencriptar
			boolean desc = objUtil.decrypt(LOGGER, rutaFichero, rutaFicheroDES, llave1, llave2);
			//objUtil.decrypt(LOGGER, rutaFichero, rutaFicheroDES, llave2, llave1);

			if(desc){

				System.out.println("******************* INICIO CONCILIACION DE COLOMBIA *******************");

				//Lectura del fichero
				File f = new File(rutaFicheroDES);
				//File f = new File(rutaFichero);
				fis = new FileInputStream(f);  
				if (!f.exists()) {
					System.out.println("El fichero descifrado NO existe");
					LOGGER.info("El fichero descifrado NO existe");
					System.exit(0);
				}

				fr = new InputStreamReader(fis, "ISO-8859-1");	        
				br = new BufferedReader(fr);


				String NUMCLIEN ="";		String FIRNAME ="";				String MIDDLENAME ="";
				String LASTNAM ="";			String SELSNAM ="";				String COIDEN ="";
				String NUMDOCU ="";			String ISPREFE ="";				String CONTNUM ="";
				String PHONETY ="";			String CONTNUM2 ="";			String PHONETY2 ="";
				String ADDRESS ="";			String ADDRSNM ="";				String GEGCODE ="";
				String GEGNAME ="";			String GEGCODE2 ="";			String GEGNAME2 ="";
				String ECONMID ="";

				LOGGER.info("Obtener IDs");
				IDs = objQuery.obtenerIDs(obj_con);

				final List<Thread> threads_exe      = new ArrayList<Thread>();
				LOGGER.error(" - COMIENZA LECTURA DEL FICHERO Y EJECUCION EN COLOMBIA - ");
				while((linea=br.readLine())!=null)    {
					if(linea.length()==0){
						break;
					}
					try   {
						i++;		
						if (i % 100 == 0) {
							LOGGER.error("Linea - "+i ); 
							System.out.println("Linea - "+i); 
						}
						try {

							NUMCLIEN = linea.substring(0, 8).trim();
							//System.out.println("NUMCLIEN - "+NUMCLIEN);
							FIRNAME = linea.substring(8, 28).trim();
							//System.out.println("FIRNAME - "+FIRNAME);
							MIDDLENAME = linea.substring(28, 48).trim();
							//System.out.println("MIDDLENAME - "+MIDDLENAME);
							LASTNAM = linea.substring(48, 68).trim();
							SELSNAM = linea.substring(68, 88).trim();
							COIDEN = linea.substring(88, 90).trim();
							NUMDOCU = linea.substring(90, 105).trim();
							ISPREFE = linea.substring(105, 106).trim();
							CONTNUM = linea.substring(106, 116).trim();
							PHONETY = linea.substring(116, 126).trim();
							CONTNUM2 = linea.substring(126, 136).trim();
							PHONETY2 = linea.substring(136, 146).trim();
							ADDRESS = linea.substring(146, 196).trim();
							ADDRSNM = linea.substring(196, 246).trim();
							GEGCODE = linea.substring(246, 253).trim();
							GEGNAME = linea.substring(253, 283).trim();
							GEGCODE2 = linea.substring(283, 290).trim();
							GEGNAME2 = linea.substring(290, 320).trim();
							ECONMID = linea.substring(320, 360).trim();

							if(IDs!=null && IDs.contains(NUMCLIEN)){
								IDs.remove(NUMCLIEN);
							}

							HashMap<String, String> mapint = new HashMap<String, String>();

							mapint.put("NUMCLIEN", NUMCLIEN);
							mapint.put("FIRNAME", FIRNAME);
							mapint.put("MIDDLENAME", MIDDLENAME);
							mapint.put("LASTNAM", LASTNAM);
							mapint.put("SELSNAM", SELSNAM);
							mapint.put("COIDEN", COIDEN);
							mapint.put("NUMDOCU", NUMDOCU);
							mapint.put("ISPREFE", ISPREFE);
							mapint.put("CONTNUM", CONTNUM);
							mapint.put("PHONETY", PHONETY);
							mapint.put("CONTNUM2", CONTNUM2);
							mapint.put("PHONETY2", PHONETY2);
							mapint.put("ADDRESS", ADDRESS);
							mapint.put("ADDRSNM", ADDRSNM);
							mapint.put("GEGCODE", GEGCODE);
							mapint.put("GEGNAME", GEGNAME);
							mapint.put("GEGCODE2", GEGCODE2);
							mapint.put("GEGNAME2", GEGNAME2);
							mapint.put("ECONMID", ECONMID);
							mapint.put("FLD_JOB_ID", FLD_JOB_ID);
							arr.add(mapint);
						}
						catch(Exception ex)  {
							LOGGER.error("Excepcion: " + ex+ " - " +linea);
							ex.printStackTrace();
							System.out.println("Linea - "+i);
							System.out.println("Excepcion: " + ex+ " - " +linea);
						}

						if(i%ColombiaConciliacion.rango==0) { 
							ColombiaConciliacion.y=i; ColombiaConciliacion.x=i-ColombiaConciliacion.rango;
							Thread hilo=new Thread( new Runnable() {
								ArrayList<HashMap<String,String>> arr2 = new ArrayList<HashMap<String,String>>(
										ColombiaConciliacion.arr.subList(ColombiaConciliacion.x,ColombiaConciliacion.y));
								public void run() { 
									(new Querys(LOGGER)).executeCON_Hilos(arr2, obj_con);
								}
							});
							hilo.start();          threads_exe.add(hilo);		        		
						}  	
					}catch(ArrayIndexOutOfBoundsException ex)  {
						LOGGER.error("Linea - "+i);
						LOGGER.error("Excepcion: " + ex+ " - " +linea);
						System.out.println("Linea - "+i);
						System.out.println("Excepcion: " + ex+ " - " +linea);
					}
					catch(Exception ex)  {
						LOGGER.error("Linea - "+i);
						LOGGER.error("Excepcion: " + ex+ " - " +linea);
						System.out.println("Linea - "+i);
						System.out.println("Excepcion: " + ex+ " - " +linea);
					}
				}

				System.out.println("ColombiaConciliacion.arr QUEDAN "+(ColombiaConciliacion.i-ColombiaConciliacion.y));
				LOGGER.error("ColombiaConciliacion.arr QUEDAN "+(ColombiaConciliacion.i-ColombiaConciliacion.y));

				if(ColombiaConciliacion.arr.size()>0){
					Thread hilo=new Thread( new Runnable() {
						ArrayList<HashMap<String,String>> arr2 =new ArrayList<HashMap<String,String>>(
								ColombiaConciliacion.arr.subList(ColombiaConciliacion.y,ColombiaConciliacion.i));		       
						public void run() {         	 
							(new Querys(LOGGER)).executeCON_Hilos(arr2, obj_con); 
						}
					});
					hilo.start();	         threads_exe.add(hilo);

					//Esperamos a que finalice los hilos de ejecucion
					for( Thread t : threads_exe ) {       t.join();       }
				}

				if(IDs!=null && IDs.size()>0){
					for(int i=0;i<IDs.size();i++){
						objQuery.insertRLT1Colombia(IDs.get(i), ColombiaConciliacion.FLD_JOB_ID, obj_con, "Cliente no localizado en Altamira Colombia");
					}
				}
				objQuery.cerrarJOB(FLD_JOB_ID,"COLOMBIA",inicio, obj_con);
				try {	 
					if (obj_con != null ) {
						obj_con.close();  
					}
				} catch (SQLException e) {	e.printStackTrace();		System.out.println("ERROR5");	}

				//f.delete();
				//LOGGER.info(rutaFicheroDES + " eliminado");
				br.close();
				fr.close();

				long end = System.currentTimeMillis();

				String tiempo = objQuery.obtenerTiempo("PROCESO FINALIZADO DE MODO CORRECTO...", start, end);
				LOGGER.info("Proceso finalizado. Tiempo de ejecuccion: " + tiempo);
				System.exit(0);

				LOGGER.error("Se cierra el JOB: "+ FLD_JOB_ID);
				LOGGER.error("******************* FIN CONCILIACION DE COLOMBIA *******************");


			}else{
				LOGGER.error("FALLO: Descifrado no correcto");
				System.out.println("FALLO:  Descifrado no correcto");
			}
		}else{
			LOGGER.error("FALLO: alguna de las llaves no ha podido ser obtenida");
			System.out.println("FALLO: alguna de las llaves no ha podido ser obtenida");

		}
	}
}