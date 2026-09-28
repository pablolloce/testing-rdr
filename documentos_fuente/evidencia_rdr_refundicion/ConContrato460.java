import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jdbc.ConDB;
import util.Utilidades;

// La lógica de esta clase se basa en leer el fichero, mientras se va leyendo, se va volcando en un array. 
// Se generan sublistas de un rango definido en una variable estática.
// Esta sublista se mana a ejecución con un hilo nuevo.

// Se realiza con Multihilo y Pool de conexiones

public class ConContrato460 {

    public static ArrayList<HashMap<String,String>> arr = new ArrayList<HashMap<String,String>>();
    public static ArrayList<String> noConciMnem = new ArrayList<String>();
    public static ArrayList<String> noConciClientelaIdMnem = new ArrayList<String>();
//    public static ArrayList<String> errorConciMnem = new ArrayList<String>();
    public static Connection obj_con00=null;
    public static ConDB obj_ConDB00=null;
    public static Connection obj_con01=null;
    public static ConDB obj_ConDB01=null;
    public static Connection obj_con02=null;
    public static ConDB obj_ConDB02=null;
    public static Connection obj_con03=null;
    public static ConDB obj_ConDB03=null;
    

    static int i=0; 	static int rango=100; 		static int x_ins=0; 		static int y_ins=0;		    
    static int x=0;  	static int y=0;			static int x_err=0;  		static int y_err=0;	    
    static final int CANTIDAD_COLUMNAS = 12;
    static final String SIMBOLO_SEPARACION = ";";
    
    public static String FLD_JOB_ID=null;
    public static String msg_ins="Codigo Clientela en RDR que no concilia en Clientela C460";
//      public static String msg_err="Codigo Clientela en RDR que no es valido - C460";
    static ArrayList<Connection> listaConexiones = new ArrayList<Connection>();

    public static void main(String[] args) throws Exception {
	System.out.println("******************* INICIO CONCILIACION DE CLIENTELA C460 *******************");

	obj_ConDB00=new ConDB();		obj_ConDB00.ObtenerCredenciales();	obj_con00=obj_ConDB00.ObtenerConexion();
	
	obj_ConDB01=new ConDB();		obj_ConDB01.ObtenerCredenciales();	obj_con01=obj_ConDB01.ObtenerConexion();
	obj_ConDB02=new ConDB();		obj_ConDB02.ObtenerCredenciales();	obj_con02=obj_ConDB02.ObtenerConexion();
	obj_ConDB03=new ConDB();		obj_ConDB03.ObtenerCredenciales();	obj_con03=obj_ConDB03.ObtenerConexion();

	listaConexiones.add(obj_con01);
	listaConexiones.add(obj_con02);
	listaConexiones.add(obj_con03);
	
	ArrayList<String> CLIIdsFic = new ArrayList<String>();
//	ArrayList<String> CLIIds = new ArrayList<String>();
	
	// Tupla de clientelaID y numFolio
//	HashMap<String,String> mapClientelaFolioFich = new HashMap<String,String>();
	// Tupla de mnemLocal y clientelaID
	HashMap<String,String> mapMnemLocalClientelaID = new HashMap<String,String>();
	
	ArrayList<String> listaClientelaIdFechaCancelacionFich = new ArrayList<String>();
	
	Utilidades ut = new Utilidades();	ConContrato460.FLD_JOB_ID=ut.generateJOBID();	

	FileInputStream fis = null;	    InputStreamReader fr = null;	    BufferedReader br = null;	    String linea = "";

	long inicio = obj_ConDB01.crearJOB(FLD_JOB_ID,"C460", obj_con01);	    System.out.println("Se crea JOB");

	String VCH_CCLIEN ="";
	String VCH_FCANCEL ="";
	String VCH_FOLIO ="";		
	
	//Lectura del fichero
	File f = new File(args[0]);		 System.out.println(args[0]);
	try    {
	    fis = new FileInputStream(f);    	fr = new InputStreamReader(fis, "ISO-8859-1");	        br = new BufferedReader(fr);

	    System.out.println("Se obtienen codigos de Clientela de GS");
	    mapMnemLocalClientelaID = obj_ConDB01.obtenerClientelaIDBBVA(obj_con01);
	    
	    final List<Thread> threads_exe      = new ArrayList<Thread>();
	    final List<Thread> threads_ins      = new ArrayList<Thread>();
//	    final List<Thread> threads_err      = new ArrayList<Thread>();
	    
	    System.out.println(" - COMIENZA LECTURA DEL FICHERO Y EJECUCION EN CLIENTELA C460 - ");
            if (f.exists())
            {
                br.readLine();// Lee cabecera
                while ((linea = br.readLine()) != null)
                {

                    try
                    {
                        if (linea.substring(linea.length() - 1).equals(SIMBOLO_SEPARACION))
                        {
                            linea = linea + "N";
                        }

                        i++;
                        if (i % 10000 == 0)
                        {
                            System.out.println("Linea Clientela C460 - " + i);
                        }

                        String[] campos = null;
                        campos = linea.split(SIMBOLO_SEPARACION);
                        if (campos.length != CANTIDAD_COLUMNAS)
                        {
                            System.out.println(" **** Fallo al leer la fila " + i + " debido a longitud incorrecta: "
                                    + campos.length + " en lugar de " + CANTIDAD_COLUMNAS + " ****");
                            continue;
                        }

                        VCH_CCLIEN = campos[9].trim();
                        VCH_FOLIO = campos[6].trim();
                        VCH_FCANCEL = campos[8].trim();

                        HashMap<String, String> mapint = new HashMap<String, String>();

                        mapint.put("VCH_CCLIEN", VCH_CCLIEN);
                        mapint.put("VCH_FOLIO", VCH_FOLIO);
                        mapint.put("FLD_JOB_ID", FLD_JOB_ID);
                        mapint.put("VCH_FCANCEL", VCH_FCANCEL);

                        CLIIdsFic.add(VCH_CCLIEN);      // Para comprobar si el clientela existe en el fichero
                       
                        listaClientelaIdFechaCancelacionFich.add(VCH_CCLIEN + ";" + VCH_FCANCEL); // Para comprobar la fecha asociada a un clientela 
                        
                        arr.add(mapint); // Para enviar al PL

                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {
                        // System.out.println("campos.size() - "+campos.length);
                        System.out.println("Linea Clientela C460 - " + i);
                        System.out.println("Línea con longitud errónea: " + linea);
                    }
                    catch (Exception ex)
                    {
                        // System.out.println("campos.size() - "+campos.length);
                        System.out.println("Excepcion: " + ex + " - " + linea);
                    }
                }

                System.out.println("Fichero leido.");

                System.out.println(" - COMIENZA COMPARACION EN CLIENTELA C460 - ");

                boolean encontrado = false;
                for (Entry<String, String> entry : mapMnemLocalClientelaID.entrySet())
                {
                    String clientelaIdBD = entry.getKey();
                    String mnemLocalBD = entry.getValue();
                    if (CLIIdsFic.contains(clientelaIdBD))
                    {
                        encontrado = true;
                        // Se comprueba la fecha de cancelacion asociado a ese
                        // codigo de clientela
                        // Si no existe ninguna fecha distinta de 0001-01-01 hay
                        // que dar un alta
                        if (!listaClientelaIdFechaCancelacionFich.contains(clientelaIdBD + ";0001-01-01"))
                        {
                            ConContrato460.noConciMnem.add(mnemLocalBD);
                            ConContrato460.noConciClientelaIdMnem.add(clientelaIdBD + ";" + mnemLocalBD);
                        }

                        // Se comprueba si para ese codigo de clientela va
                        // asociado algun numero de folio

                        // String numFolioFich =
                        // mapClientelaFolioFich.get(clientelaIdBD);
                        // if (numFolioFich == null || numFolioFich.equals(""))
                        // {
                        // // Si no hay nada hay que marcarlo en la RLT1
                        // ConContrato460.noConciMnem.add(mnemLocalBD);
                        // ConContrato460.noConciClientelaIdMnem.add(clientelaIdBD
                        // + ";" + mnemLocalBD);
                        // }
                    }
                    if (!encontrado)
                    {
                        ConContrato460.noConciMnem.add(mnemLocalBD);
                        ConContrato460.noConciClientelaIdMnem.add(clientelaIdBD + ";" + mnemLocalBD);
                    }
                    encontrado = false;
                }

                System.out.println(" - COMIENZA INSERCION DE REGISTROS NO ENCONTRADOS CLIENTELA C460 - "
                        + noConciMnem.size());

                obj_ConDB01.insertRLT1ClientelaC460_Proceso(ConContrato460.noConciMnem, ConContrato460.FLD_JOB_ID, obj_con02);
                obj_ConDB01.insertRLT1ClientelaC460_Reporte(ConContrato460.noConciClientelaIdMnem, ConContrato460.FLD_JOB_ID, obj_con02);

                // Ejecución de querys almacenadas
                insertarRLT1();

                System.out.println("Comienzan las llamadas a procedimiento PL/SQL CONC460.");
                obj_ConDB01.executeCONC460_Hilos(arr, obj_con01);
                System.out.println("Llamadas al procedimiento PL/SQL CONC460: " + ConDB.getContadorPL());

                updatesFAB1();

            }else {	System.out.println("El fichero no existe !!!");	    }

	} catch (FileNotFoundException e) {e.printStackTrace();		System.out.println("ERROR1");
	} catch (UnsupportedEncodingException e) {e.printStackTrace();	System.out.println("ERROR2");
	} catch (IOException e) {e.printStackTrace();			System.out.println("ERROR3");
	} catch (Exception e) {e.printStackTrace();			System.out.println("ERROR4");	}

	
	
	obj_ConDB01.cerrarJOB(FLD_JOB_ID,"CCL",inicio, obj_con01);

	try {	 
	    if (obj_con00 != null ) {
	    	obj_con00.close();
	    }
	    if (obj_con01 != null ) {
	    	obj_con01.close();
	    }
	    if (obj_con02 != null ) {
	    	obj_con02.close();
	    }
	    if (obj_con03 != null ) {
	    	obj_con03.close();
	    }
	} catch (SQLException e) {	e.printStackTrace();		System.out.println("ERROR5");	}

	System.out.println("Se cierra el JOB de Clientela: "+ FLD_JOB_ID);

	System.out.println("******************* FIN CONCILIACION DE CLIENTELA C460 *******************");
    }


    private static void updatesFAB1()
    {
        // Obtiene la lista de querys
        ArrayList<String> updates = ConDB.getUpdatesFAB1();
//        System.out.println("Actualizaciones en la FAB1 pendientes: " + updates.size());
        try
        {
            for (String queryUpdateFAB1 : updates)
            {
                // Ejecuta la query
                ConDB.ejecutarQuery(obj_con00, queryUpdateFAB1);
            }
        }
        catch (Exception ex)
        {
            System.out.println("Excepcion en updateFAB1: " + ex);
        }

    }

    private static void insertarRLT1() 
    {
        // Obtiene la lista de querys
        ArrayList<String> inserciones_Proceso= ConDB.getInsercionesRLT1_Proceso();
        ArrayList<String> inserciones_Reportes = ConDB.getInsercionesRLT1_Reportes();
//        System.out.println("Inserciones en la RLT1 PROCESO pendientes: " + inserciones_Proceso.size());
//        System.out.println("Inserciones en la RLT1 REPORTES pendientes: " + inserciones_Reportes.size());
        try
        {
            for (String queryInsercionRLT1 : inserciones_Proceso)
            {
                if (queryInsercionRLT1 != null)
                {
                    // Ejecuta la query
                    ConDB.ejecutarQuery(obj_con00, queryInsercionRLT1);
                }
            }
            
            for (String queryInsercionRLT1 : inserciones_Reportes)
            {
                if (queryInsercionRLT1 != null)
                {
                    // Ejecuta la query
                    ConDB.ejecutarQuery(obj_con00, queryInsercionRLT1);
                }
            }
            
        }
        catch (Exception ex)
        {
            System.out.println("Excepcion en insertarRLT1: " + ex);
        }
    }
    

}
 


