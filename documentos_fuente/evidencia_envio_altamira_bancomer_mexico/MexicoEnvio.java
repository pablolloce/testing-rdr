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
import util.Ficheros;

public class MexicoEnvio {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java MexicoEnvio <logLevel> <logConfigFile> <arg3> <outputFileTemplate>");
            System.exit(1);
        }

        Logger LOGGER = Logger.getLogger(MexicoEnvio.class);
        Querys jdbc = null;
        int totalRegistros = 0;
        String rutaFichero = null;
        ArrayList<String> IDs = null;

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

        Connection objCon = null;
        try {
            ConDB objConDB = new ConDB();
            objConDB.ObtenerCredenciales();
            objCon = objConDB.ObtenerConexion();

            jdbc = new Querys(LOGGER);

            LOGGER.info("Obtener IDs");

            IDs = jdbc.obtenerIDs(objCon);
            totalRegistros = IDs.size();

            LOGGER.info("Cantidad de IDs a enviar: " + totalRegistros);

            DateFormat dfAnio = new SimpleDateFormat("yyyy");
            DateFormat dfMes = new SimpleDateFormat("MM");
            DateFormat dfDia = new SimpleDateFormat("dd");

            Locale l = new Locale("es", "MX");
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Mexico_City"), l);

            String anio = dfAnio.format(cal.getTime());
            String mes = dfMes.format(cal.getTime());
            String dia = dfDia.format(cal.getTime());

            rutaFichero = args[3].replace("YYYY", anio).replace("MM", mes).replace("DD", dia);
            LOGGER.info("rutaFichero " + rutaFichero);
        } catch (Exception e) {
            LOGGER.error("Error durante la obtención de IDs y configuración de ruta", e);
        } finally {
            if (objCon != null) {
                try {
                    objCon.close();
                } catch (Exception e) {
                    LOGGER.error("Error al cerrar la conexión", e);
                }
            }
        }

        if (IDs != null) {
            Ficheros objUtils = new Ficheros();
            objUtils.sacarFichero(LOGGER, rutaFichero, IDs);
        } else {
            LOGGER.error("IDs es nulo, no se puede generar el fichero");
        }

        long end = System.currentTimeMillis();

        String tiempo = jdbc != null ? jdbc.obtenerTiempo("PROCESO FINALIZADO DE MODO CORRECTO...", start, end) : "N/A";
        LOGGER.info("Proceso finalizado. Tiempo de ejecuccion: " + tiempo);
        System.exit(0);
    }
}