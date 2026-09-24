import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import jdbc.ConDB;
import jdbc.Querys;
import util.Utils;

public class ColombiaEnvio {

	public static void main(String[] args) throws Exception {
		Logger LOGGER = Logger.getLogger(ColombiaEnvio.class);
		Querys jdbc = null;
		int totalRegistros = 0;
		String rutaFichero;
		ArrayList<String> IDs = null;

		Level level = Level.INFO;
		switch(Integer.parseInt(args[0])) {
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
		}

		LOGGER.setLevel(level);
		PropertyConfigurator.configure(args[1]);
		long start = System.currentTimeMillis();
		
		Connection objCon=null;
		try{
			ConDB objConDB=new ConDB();	
			objConDB.ObtenerCredenciales();	
			objCon=objConDB.ObtenerConexion();

			jdbc = new Querys(LOGGER);   

			LOGGER.info("Obtener IDs");	
			
			IDs = jdbc.obtenerIDs(objCon);
			totalRegistros = IDs.size();

			LOGGER.info("Cantidad de IDs a enviar: " + totalRegistros);
			//rutaFichero = args[3];
			
			DateFormat dfAnio = new SimpleDateFormat("yyyy"); 
			DateFormat dfMes = new SimpleDateFormat("MM");
			DateFormat dfDia = new SimpleDateFormat("dd");
			
			Locale l = new Locale("es","CO");
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"),l);
			
			// String anio = dfAnio.format(Calendar.getInstance().getTime());
			// String mes = dfMes.format(Calendar.getInstance().getTime());
			// String dia = dfDia.format(Calendar.getInstance().getTime());
			
			String anio = dfAnio.format(cal.getTime());
			String mes = dfMes.format(cal.getTime());
			String dia = dfDia.format(cal.getTime());
			
			rutaFichero = args[3].replace("AAAA", anio).replace("MM", mes).replace("DD",dia);
			LOGGER.info("rutaFichero "+rutaFichero);
		}finally{
			objCon.close();
		}

		Utils objUtils = new Utils();
		objUtils.sacarFichero(LOGGER, rutaFichero, IDs);

		long end = System.currentTimeMillis();

		String tiempo = jdbc.obtenerTiempo("PROCESO FINALIZADO DE MODO CORRECTO...", start, end);
		LOGGER.info("Proceso finalizado. Tiempo de ejecuccion: " + tiempo);
		System.exit(0);
	}
}
