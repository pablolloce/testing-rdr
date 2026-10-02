package main;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import jdbc.ConDB;
import jdbc.QuerysStr;
import report.ReportesRDR;

public class Ppal {
	public static final Logger LOGGER = Logger.getLogger(Ppal.class);

    /*********************************************** DECLARACION DE CONEXIONES ************************************************************************/
    public static Connection obj_con=null;	public static ConDB obj_ConDB=null;
    /*********************************************** DECLARACION DE STATEMENTS ************************************************************************/
    public static Statement stmt = null;           

    public static HashMap<String,HashMap<String,ArrayList<HashMap<String,String>>>>  reportes;
    public static String id_ejecucion="";
    public static final String proceso="AlertasCocinado";
    public static String ProcesosExtraer="PROCESOS";
    
    public static void main(String[] args) {
    	
    	try {
    		System.out.println("AlertasCocinado::Parámetros recibidos:");
    		for(int i=0;i<args.length;i++){
    			System.out.println("    args["+i+"] = '"+args[i]+"'");
    		}
    		if(args[2].equals("PROCESOS")==false){
    			ProcesosExtraer=args[2];
    		}
		} catch (Exception e) {
			// TODO: handle exception
		}
    	
    	long st1=System.currentTimeMillis();
    	double end1=0;
    	long start = System.currentTimeMillis();
    	
    	
    	boolean status=configuraBBDDyLog(args);
    	if(status==false){
    		String msg="AlertasCocinado::Ha fallado la configuracion de conexion y BBDD. Se detendra el proceso.";
    		System.out.println(msg);
    		LOGGER.error(msg);
    		msg="*******************  AlertasCocinado::FIN  ******************************";
    		System.out.println(msg);
    		LOGGER.error(msg);
    		return;
    	}
		
		System.out.println("Comienzo de Proceso.");
		
		id_ejecucion=QuerysStr.newoid(stmt);
		
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(stmt, "configuraBBDD", proceso, id_ejecucion, "Conexion creada", "OK", end1, "", "");
    	
    	st1=System.currentTimeMillis();
    	//--
		ReportesRDR reportes=new ReportesRDR(obj_con);
		reportes.extraerReportes();
		
		int totalReportes=reportes.cuentaReportes();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
		QuerysStr.insertaRLT1(stmt, "extraeReportes", proceso, id_ejecucion, "Recuperada la informacion de los procesos de la REP1", "OK", end1, "Reportes", String.valueOf(totalReportes));
    	
		st1=System.currentTimeMillis();
		//--
		reportes.descargaMensajesResportes();
		
		int totalMensajes=reportes.getTotalMensajes();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
		QuerysStr.insertaRLT1(stmt, "descargaMensajesResportes", proceso, id_ejecucion, "Mensajes descargados", "OK", end1, "Mensajes", String.valueOf(totalMensajes));
    	
		st1=System.currentTimeMillis();
		//--
		reportes.generaDocumentos();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
		int totalFicheros=reportes.getTotalFicheros();
		QuerysStr.insertaRLT1(stmt, "generaDocumentos", proceso, id_ejecucion, "Documentos generados", "OK", end1, "Documentos", String.valueOf(totalFicheros));
    	
		st1=System.currentTimeMillis();
		//--
		reportes.marcaALG1_Reportes();
		reportes.marcaReportesPending();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
		QuerysStr.insertaRLT1(stmt, "marcaALG1_Reportes", proceso, id_ejecucion, "Registros marcados como utilizados", "OK", end1, "", "");
		
		st1=System.currentTimeMillis();
		//--
		reportes.cerrarConexiones();
		
		
	
		//jdbc.obtenerReportes(obj_con);
		
		cierraConexion();
	
		long end = System.currentTimeMillis();
		String tiempo = String.valueOf((end-start)/1000);
		LOGGER.info("Proceso finalizado. Tiempo: " + tiempo);
		System.out.println("Proceso finalizado. Tiempo: " + tiempo);
		System.out.println("*******************   AlertasCocinado::FIN  ******************************");
		LOGGER.info("*******************   AlertasCocinado::FIN  ******************************");
    }
    
    
    
    private static boolean configuraBBDDyLog(String []args){
    	
    	try {
    		/*********************************************** CONFIGURACION LOG4J ***********************************************/
    		Level level = Level.INFO;
    	
    		switch (Integer.parseInt(args[0]))
    		{
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
    		/*********************************************** CONFIGURACION LOG4J ***********************************************/
    	
    		System.out.println("*******************  AlertasCocinado::INICIO  ******************************");
    		LOGGER.info("*******************  AlertasCocinado::INICIO  ******************************");
    	
    		/************************************ INICIALIZACION DE CONEXIONES *************************************************/
    		obj_ConDB=new ConDB();	obj_ConDB.ObtenerCredenciales();	obj_con=obj_ConDB.ObtenerConexion();
    		/************************************ INICIALIZACION DE STATEMENTS *************************************************/
    		try {
    		    stmt = obj_con.createStatement();
    		    return true;
    		} catch (SQLException e) {
    		    e.printStackTrace();
    		    String msg="AlertasCocinado::Error al crear el statement.";
    			System.out.println(msg);
    			LOGGER.error(msg);
    			System.out.println(e.toString());
    			LOGGER.error(e.toString());
    			return false;
    		}
    		
		} catch (Exception e) {
			// TODO: handle exception
			String msg="AlertasCocinado::Error inicializando log y conexion.";
			System.out.println(msg);
			LOGGER.error(msg);
			System.out.println(e.toString());
			LOGGER.error(e.toString());
			return false;
		}
    	
    }
    
    private static void cierraConexion(){
    	// CIERRE DE CONEXIONES 
		try{	
		    if (obj_con != null ) {	obj_con.close();		} 
	
		}catch (SQLException e) {
		    e.printStackTrace();
		}
		System.gc();
    }
}