import entities.MyThreadCpty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jdbc.ConDB;
import jdbc.ConfigCredentials;
import jdbc.Querys;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utilities.Constants;
import utilities.FicheroExtraccion;

public class Ppal {
   public static final Logger LOGGER = Logger.getLogger(Ppal.class);
   public static Querys jdbc = null;
   public static int totalRegistros = 0;
   public static String rutaFicheros;
   public static String totalEtiquetas;
   public static String ficheroExtraccion;
   public static String ficheroExtraccionRenombrar;
   public static String typeInfo;
   public static ArrayList<String> mnems = null;
   public static ArrayList<String> etiquetas = null;
   public static String query = null;
   public static String extraccionCpty = null;
   public static String nemonico = null;
   public static String fileNameXml = null;

   public static void main(String[] args) throws Exception {
      System.gc();
      typeInfo = args[5];
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
      String credentials = null;
      PropertyConfigurator.configure(args[1]);
      credentials = args[6];
      ConfigCredentials configCred = new ConfigCredentials(credentials);
      LOGGER.info(configCred.toString());
      ConDB.initialize(configCred);
      int NUM_THREADS = Integer.parseInt(args[2]);
      LOGGER.info("Se van a utilizar " + NUM_THREADS + " hilos.");
      LOGGER.info("******** INICIO PROCESO EXTRACCION GENERICA ********");
      System.out.println("******** INICIO PROCESO EXTRACCION GENERICA ********");
      long start = System.currentTimeMillis();
      LOGGER.info("Obtener nemonicos");
      Connection con = ConDB.getInstance().getConnect();

      
      jdbc = new Querys();
      
      if (typeInfo.equals(Constants.SSIS) || typeInfo.equals(Constants.SCIS)
    		  || typeInfo.equals(Constants.CONTC) || typeInfo.equals(Constants.CONTR) 
			|| typeInfo.equals(Constants.BASKETS) || typeInfo.equals(Constants.DUCOCPTY)
			|| typeInfo.equals(Constants.DOMI) || typeInfo.equals(Constants.CONTRBBVA) || typeInfo.equals(Constants.THIRDPARTIES)) {

    	 /** Get CLOB_VALUE from tables */
         jdbc.obtenerEntidades(con, typeInfo);    
         
         /** Obtain oids from the queries in the CLOB_VALUE */
         mnems = jdbc.recuperarEntidades(con, typeInfo);
         totalRegistros = mnems.size();
         
         LOGGER.info("Cantidad de "+typeInfo+" a tratar: " + totalRegistros);
         LOGGER.info("Obtenener query de extraccion");
         
         jdbc.obtenerExtraccion(con, typeInfo);
         
         etiquetas = jdbc.obtenerEtiquetas(con, typeInfo);
         
         rutaFicheros = args[3];
         ficheroExtraccion = args[4];
         
         //Obtenemos el fichero
         String ficheroExtraccionQuery = jdbc.obtenerFichero(con, typeInfo);
         String[] parts = ficheroExtraccionQuery.split("/"); 
         
         //nombre fichero salida
         fileNameXml = parts[parts.length-1];
         
         //ruta fichero salida
         int ultimo = fileNameXml.length() + 1;
         ficheroExtraccionRenombrar = ficheroExtraccionQuery.substring(0, ficheroExtraccionQuery.length() - ultimo);
         
         LOGGER.info("------------Ruta del fichero de extraccion: " + ficheroExtraccion);
         LOGGER.info("********   CREANDO REPORTE DE EXTRACCION DE "+typeInfo+"  ********");
         escribirEtiquetasEntrada();
        
        iniciarThreadsCpty(NUM_THREADS, typeInfo);

         
         Thread.sleep(3000L);
         LOGGER.info("******* CIERRE DE CONEXIONES *******");
         System.gc();
         cerrarConexiones(con);
         System.out.println("Creando fichero " + ficheroExtraccion);
         //agregamos validacion de tama�o de etiqueta final para evitar error si no existe
         if (Querys.totaletiquetas.size()>1){
			escribirEtiquetasSalida();
		 }
         Thread.sleep(5000L);
         long end = System.currentTimeMillis();
         String tiempo = jdbc.obtenerTiempo("PROCESO FINALIZADO...", start, end);
         LOGGER.info("Proceso finalizado. Tiempo de ejecuccion: " + tiempo);
         LOGGER.info("*********   FIN EXTRACCION GENERICA DE " +typeInfo+ "  ********");
         Querys.fin = true;
         renombrarfichero(typeInfo);
    	  
      }
    	else {
         LOGGER.info("*******   FIN EXTRACCION GENERICA DE "+typeInfo+"  ********");
      }

   }

   private static void cerrarConexiones(Connection con) {
      try {
    	  con.close();
      } catch (SQLException var1) {
         LOGGER.info(var1);
      }

   }

   private static void iniciarThreadsCpty(int NUM_THREADS, String typeInfo) throws Exception {
      File ficherosalida = new File(ficheroExtraccion);
      FicheroExtraccion ficheroGrabar = new FicheroExtraccion();
      if (!ficherosalida.exists()) {
         ficherosalida.createNewFile();
      }

      ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(NUM_THREADS);
      mnems.parallelStream().forEach((s) -> {
         try {
            poolExecutor.execute(new MyThreadCpty(s, ficherosalida, rutaFicheros, ficheroGrabar, typeInfo));
         } catch (Exception var5) {
            var5.printStackTrace();
         }

      });
      poolExecutor.shutdown();

      try {
         poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
      } catch (InterruptedException var5) {
         LOGGER.error("Se ha esperado demasiado. Proceso terminado incorrectamente.");
      }

   }

   
   private static void escribirEtiquetasEntrada() throws FileNotFoundException {
      BufferedWriter bw = null;
      FileWriter fw = null;
      File ficherosalida = new File(ficheroExtraccion);
      new RandomAccessFile(ficherosalida, "rw");

      try {
         if (ficherosalida.isFile()) {
            fw = new FileWriter(ficherosalida.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write((String)Querys.totaletiquetas.get(0));
            bw.close();
            fw.close();
         }
      } catch (Exception var5) {
    	  //Se corrigue mensaje de error a etiqueta inicial
         LOGGER.error("Error: No se ha podido incluir la etiqueta inicial.");
      }

   }

   private static void escribirEtiquetasSalida() throws FileNotFoundException {
      BufferedWriter bw = null;
      FileWriter fw = null;
      File ficherosalida = new File(ficheroExtraccion);
      new RandomAccessFile(ficherosalida, "rw");

      try {
         if (ficherosalida.isFile()) {
            fw = new FileWriter(ficherosalida.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write((String)Querys.totaletiquetas.get(1));
            bw.close();
            fw.close();
         }
      } catch (Exception var5) {
         LOGGER.error("Error: No se ha podido incluir la etiqueta final.");
      }

   }

   private static void renombrarfichero(String typeInfo) throws FileNotFoundException {
      File ficherosalida = new File(ficheroExtraccion);
      File parent = ficherosalida.getParentFile();
      String filename = fileNameXml;
      
      System.out.println("Generando fichero en la ruta: " + parent+filename);

      new File(parent, filename);

      try {
         if (ficherosalida.isFile()) {
            Path from = Paths.get(parent.toString(), ficherosalida.getName());
            /** File move .tmp to .xml */
            switch (typeInfo){
          		case Constants.SSIS:
          			Path to2 = Paths.get(parent.toString() + "/SSIS", filename);
          			fileMover(from, to2);
          			break;
          		case Constants.SCIS:
          			Path to3 = Paths.get(parent.toString() + "/SCIS", filename);
          			fileMover(from, to3);
          			break;
          		case Constants.CONTC:
          			Path to4 = Paths.get(parent.toString() + "/CONT", filename);
          			fileMover(from, to4);
          			break;
          		case Constants.CONTR:
          			Path to5 = Paths.get(parent.toString() + "/CONTR", filename);
          			fileMover(from, to5);
          			break;
          		case Constants.BASKETS:
          			Path to6 = Paths.get(parent.toString() + "/Baskets", filename);
          			fileMover(from, to6);
          			break;
          		case Constants.DUCOCPTY:
          			Path to7 = Paths.get(parent.toString() + "/DUCOCPTY", filename);
          			fileMover(from, to7);
          			break;
				case Constants.DOMI:
          			Path to8 = Paths.get(parent.toString() + "/CONT", filename);
          			fileMover(from, to8);
          			break;
				case Constants.CONTRBBVA:
          			Path to9 = Paths.get(parent.toString() + "/CONTRBBVA", filename);
          			fileMover(from, to9);
          			break;
				case Constants.THIRDPARTIES:
          			Path to10 = Paths.get(parent.toString(), filename);
          			fileMover(from, to10);
            }
            
         }
      } catch (Exception var6) {
         System.out.println("Error: No se ha podido renombrar el fichero. " + var6);
         LOGGER.error("Error: No se ha podido renombrar el fichero.");
      }

   }

    /**
     * 
     * @param from file input
     * @param to file output
     * @throws IOException exception
     */
	private static void fileMover(Path from, Path to) throws IOException {
		Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
	}

}
