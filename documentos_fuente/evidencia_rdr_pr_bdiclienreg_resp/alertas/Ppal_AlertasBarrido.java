package main;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import alertaspck.ProcesoCLS;
import jdbc.ConDB;
import jdbc.QuerysConfig;
import jdbc.QuerysStr;

public class Ppal {
	
	public static final Logger LOGGER = Logger.getLogger(Ppal.class);
    public static Connection obj_con=null;	public static ConDB obj_ConDB=null;
    private static PreparedStatement stmt = null;  
    public static String id_ejecucion="";
    public static final String proceso="AlertasBarrido";

    /*private static int totalRegistros=0;	
    private static int contadorInsercionesALG1=0;*/
    private static int recorridosTPG1=0;
  
    private static HashMap<String, ProcesoCLS> hmProcesos     = new HashMap<String, ProcesoCLS>();
    //private static HashMap<String, String[]>   hmDescripLargas= new HashMap<String, String []> ();
    private static Vector<String>              TPG1_OIDs      = new Vector<String>             ();
    private static Vector<String>         InsertsEstadisticas = new Vector<String>             ();
    private static String ProcesosExtraer="PROCESOS";

    public static void main(String[] args) {
		
    	try {
    		System.out.println("AlertasBarrido::Parámetros recibidos:");
    		for(int i=0;i<args.length;i++){
    			System.out.println("    args["+i+"] = '"+args[i]+"'");
    		}
    		if(args[2].equals("PROCESOS")==false){
    			ProcesosExtraer=args[2];
    		}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("main::ERROR AL IMPRIMIR LOS ARGUMENTOS. "+e.toString());
			LOGGER.error("main::ERROR AL IMPRIMIR LOS ARGUMENTOS. "+e.toString());
		}

    	long start = System.currentTimeMillis();
    	long st1=0;
    	double end1=0;
		System.out.println("main::Comienzo de Proceso...");
		
    	boolean status=configuraBBDDyLog(args); 
    	
    	LOGGER.info("*******************  AlertasBarrido::INICIO  ******************************");
    	System.out.println("*******************  AlertasBarrido::INICIO  ******************************");
    	
    	if(status==false){
    		LOGGER.error("**** Error al configurar. No se procesarán las alertas. ****");
    		System.out.println("Error al configurar. No se procesarán las alertas.");
    		cierraBBDD();
    		LOGGER.info("*******************   AlertasBarrido::FIN   ******************************");
    		return;
    	}else{
    		LOGGER.error("main::Conexion creada.");
    		System.out.println("main::Conexion creada.");
    	}
    	
    	id_ejecucion=QuerysStr.newoid();
    	
    	end1=((double) System.currentTimeMillis()- (double) start)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "configuraBBDD", proceso, id_ejecucion, "Conexion creada", "OK", end1, "", "");
    	
    	st1=System.currentTimeMillis();
		status=recuperaAlertasTPG1();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
		int registrosTPG1=TPG1_OIDs.size();
    	
		
		if(status==false){
			LOGGER.error("main::ERROR al recuperar las alertas. Se detiene el proceso de barrido.");
    		System.out.println("main::ERROR al recuperar las alertas. Se detiene el proceso de barrido.");
    		cierraBBDD();
    		System.out.println("*******************   AlertasBarrido::FIN   ******************************");
    		LOGGER.info("*******************   AlertasBarrido::FIN   ******************************");
    		QuerysStr.insertaRLT1(obj_con, stmt, "recuperaAlertasTPG1", proceso, id_ejecucion, "Error al recuperar alertas de la TPG1", "KO", end1, "REG_TPG1", String.valueOf(registrosTPG1));
    		return;
		}else{
			LOGGER.error("main::Recuperadas las alertas de la TPG1.");
    		System.out.println("main::Recuperadas las alertas de la TPG1.");
    		QuerysStr.insertaRLT1(obj_con, stmt, "recuperaAlertasTPG1", proceso, id_ejecucion, "Recuperadas alertas de la TPG1", "OK", end1, "REG_TPG1", String.valueOf(registrosTPG1));
		}
		
		st1=System.currentTimeMillis();
		modificaMessages();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "modificaMessages", proceso, id_ejecucion, "Modificados los mensajes", "OK", end1, "", "");
		
    	st1=System.currentTimeMillis();
		Vector<String>insertsALG1=getInsertsALG1();
		int registrosALG1=insertsALG1.size();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "getInsertsALG1", proceso, id_ejecucion, "Se obtuvieron las inserciones", "OK", end1, "REG_ALG1", String.valueOf(registrosALG1));
    	
    	
    	st1=System.currentTimeMillis();
		Vector<String>insertsErrorsRLT1=getErrorsRLT1();
		int registrosErrorsRLT1=insertsErrorsRLT1.size();
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "getErrorsRLT1", proceso, id_ejecucion, "Se obtuvieron las inserciones", "OK", end1, "REG_RLT1", String.valueOf(registrosErrorsRLT1));
    	    	
    	st1=System.currentTimeMillis();
		//realizaInserciones(obj_con, stmt, "ALG1", insertsALG1);
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "realizaInserciones", proceso, id_ejecucion, "Inserciones realizadas en la ALG1", "OK", end1, "REG_ALG1", String.valueOf(registrosALG1));
		
    	st1=System.currentTimeMillis();
		//realizaInserciones(obj_con, stmt, "RLT1", insertsErrorsRLT1);
		end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "realizaInserciones", proceso, id_ejecucion, "Inserciones realizadas en la RLT1", "OK", end1, "REG_RLT1", String.valueOf(registrosErrorsRLT1));
    	
    	st1=System.currentTimeMillis();
    	marcaUsadosTPG1(stmt, TPG1_OIDs);
    	end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	QuerysStr.insertaRLT1(obj_con, stmt, "marcaUsadosTPG1", proceso, id_ejecucion, "Se marcaron los registros utilizados de la TPG1 con END_TMS.", "OK", end1, "REG_TPG1", String.valueOf(TPG1_OIDs.size()));
		
    	st1=System.currentTimeMillis();
    	status=generaEstadisiticasALG1();
    	end1=((double) System.currentTimeMillis()- (double) st1)/1000;
    	if(status){
    		QuerysStr.insertaRLT1(obj_con, stmt, "generaEstadisiticasALG1", proceso, id_ejecucion, "Las estadisticas fueron generadas.", "OK", end1, "", "");
    	}else{
    		QuerysStr.insertaRLT1(obj_con, stmt, "generaEstadisiticasALG1", proceso, id_ejecucion, "Error al crear las estadisticas.", "KO", end1, "", "");
    	}

		cierraBBDD();
	
		long end = System.currentTimeMillis();
		
		String tiempo =String.valueOf(((end-start)/1000));
		LOGGER.info("AlertasBarrido::Proceso finalizado. Tiempo: " + tiempo);
		System.out.println("AlertasBarrido::Proceso finalizado. Tiempo: " + tiempo);
		System.out.println("*******************  AlertasBarrido::FIN   ******************************");
		LOGGER.info("*******************   AlertasBarrido::FIN   ******************************");
			
    }
    
    private static boolean recuperaAlertasTPG1(){

    	try {

    		String msg="Ppal::recuperaAlertasTPG1::Recuperando informacion de la ALD1...";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		
    		HashMap<String,String []>hmald1=getALD1Info();
    		
    		msg="Ppal::recuperaAlertasTPG1::Recuperada informacion de la ALD1.";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		
    		msg="Ppal::recuperaAlertasTPG1::Recuperando TPG1...";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		
    		String qTPG1="";
    		PreparedStatement psta = null;
    		 //System.out.println(qTPG1);
    		configureConnection(obj_con, "AlertasBarrido", "Query_TPG1");
    		//ResultSet rs=stmt.executeQuery(qTPG1);
    		
    		if(ProcesosExtraer.equals("PROCESOS")){
    			qTPG1=QuerysStr.queryTPG1();
    			psta = Ppal.obj_con.prepareStatement(qTPG1);
    		}else{
    			qTPG1=QuerysStr.queryTPG1Filtrado();  
    			psta = Ppal.obj_con.prepareStatement(qTPG1);
    			psta.setString(1, ProcesosExtraer);
    		}
    			  	
            ResultSet rs = psta.executeQuery();
    		
    		msg="Ppal::recuperaAlertasTPG1::Recuperada TPG1.";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		
    		msg="Ppal::recuperaAlertasTPG1::Extrayendo mensajes...";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		
    		while(rs.next()){
    			recorridosTPG1++;
    			//System.out.println("Registro "+recorridosTPG1);;
    			String tpg1_oid=rs.getString("TPG1_OID");
    			String proceso=rs.getString("PROCESO");
    			String id_def_alert=rs.getString("ID_DEF_ALERT");
    			String job=rs.getString("JOB_ID");
    			String registro=String.valueOf(rs.getInt("REGISTRO"));
    			String clave=rs.getString("CLAVE");
    			String valor=(rs.getString("VALOR")==null)?"":rs.getString("VALOR");
    			
    			//Recuperamos el oid y la descripcion del mensaje  de la ALD1.
    			String descripLarga="";
    			String ald1_oid="";
    			
    			try {
    				String [] aldinfo=hmald1.get(id_def_alert);
    				if(aldinfo!=null){
        				ald1_oid=aldinfo[0];
        				descripLarga=aldinfo[1];
        				
        			}
				} catch (Exception e) {
					// TODO: handle exception
					descripLarga="";
					ald1_oid="";    
				}
    			

    			if(hmProcesos.get(proceso)==null){ //Si proceso NO existe se crea
    				//System.out.println("El proceso "+proceso+" no existe. Se crea...");
    				ProcesoCLS proc=new ProcesoCLS(proceso);
    				hmProcesos.put(proceso, proc);
    			}
    			
    			hmProcesos.get(proceso).addRow(id_def_alert, job, registro, clave, valor, descripLarga, ald1_oid, "MENSAJE");
    			
    			TPG1_OIDs.add(tpg1_oid); //Almacena listado de oids procesados.
    			
    		}
    		
    		rs.close();
    		msg="Ppal::recuperaAlertasTPG1::Extraidos mensajes.";
    		LOGGER.info(msg);
    		System.out.println(msg);
    		System.out.println("Ppal::recuperaAlertasTPG1::Finaliza recuperaAlertasTPG1.");
    		LOGGER.info("Ppal::recuperaAlertasTPG1::Finaliza recuperaAlertasTPG1.");
    		System.out.println("Ppal::recuperaAlertasTPG1::Filas TPG1 recuperadas. "+recorridosTPG1+" registros.");
    		LOGGER.info("Ppal::recuperaAlertasTPG1::Filas TPG1 recuperadas. "+recorridosTPG1+" registros.");
    		return true;
    		
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("Ppal::recuperaAlertasTPG1::ERROR al recuperar alertas de TPG1");
			LOGGER.error("Ppal::recuperaAlertasTPG1::ERROR al recuperar alertas de TPG1");
			System.out.println("Ppal::recuperaalertasTPG1::ERROR::"+e.toString());
			LOGGER.error("Ppal::recuperaalertasTPG1::ERROR::"+e.toString());
			return false;
		}
    	
    }
    
    private static void modificaMessages(){
    	String msg="Ppal::modificaMessages::Modificando mensajes...";
    	LOGGER.info(msg);
    	System.out.println(msg);
    	try {
			Map<String, ProcesoCLS> map = hmProcesos;
			for (Map.Entry<String, ProcesoCLS> entry : map.entrySet()) {
			    //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			    hmProcesos.get(entry.getKey()).modificaMessages();
			}
			msg="Ppal::modificaMessages::Mensajes modificados con exito.";
			LOGGER.info(msg);
	    	System.out.println(msg);
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error("Ppal::modificaMessages::Fallo al modificar los mensajes. "+e.toString());
			System.out.println("Ppal::modificaMessages::Fallo al modificar los mensajes. "+e.toString());
		}
    	
    }    
    
    private static boolean configuraBBDDyLog(String []args){
    	try {
    		/****** CONFIGURACION LOG4J ***********/
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
    		/****** FIN CONFIGURACION LOG4J ************/
    	
    		//System.out.println("**** AlertasBarrido::configurado Log  ****");
    		//LOGGER.info("**** AlertasBarrido::configurado Log  ****");
    	
    		/****************************** INICIALIZACION DE CONEXIONES *******************************************************/
    		obj_ConDB=new ConDB();	obj_ConDB.ObtenerCredenciales();	obj_con=obj_ConDB.ObtenerConexion();
    		/****************************** INICIALIZACION DE STATEMENTS *******************************************************/
    		try {
    		   // stmt = obj_con.createStatement();
    		    //System.out.println("**** AlertasBarrido::configurada conexión a BBDD  ****");
    			//LOGGER.info("**** AlertasBarrido::configurada conexión a BBDD  ****");
    			return true;
    		} catch (Exception e) {
    		    e.printStackTrace();
    		    return false;
    		}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("**** AlertasBarrido::ERROR::Fallo al configurar  ****");
			LOGGER.error("**** AlertasBarrido::ERROR::Fallo al configurar  ****");
			System.out.println(e.toString());
			LOGGER.error(e.toString());
			return false;
		}
    	
    }

    private static void cierraBBDD(){
    	try {
			if(stmt != null){
				stmt.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		try {	
		    if (obj_con != null ) {	
		    	obj_con.close();
		    } 
		    	
		} catch (SQLException e) {
		    e.printStackTrace();
		}
		
		System.gc();
    }
  
    private static HashMap<String, String[]> getALD1Info(){
    	
    	try {
    		HashMap<String, String[]>hmald=new HashMap<String, String[]>();
    		String query=QuerysStr.queryALD1_Info();
    		configureConnection(obj_con, "AlertasBarrido", "Query_ALD1");
    		//Statement st=obj_con.createStatement();
			//ResultSet rs=st.executeQuery(query);
			
    		PreparedStatement psta = Ppal.obj_con.prepareStatement(query);
            ResultSet rs = psta.executeQuery();
			
			while(rs.next()){
				String id_def_alert=rs.getString("ID_DEF_ALERT");
				String [] datos=new String [2];
				datos[0]=rs.getString("ALD1_OID");
				datos[1]=rs.getString("DESCRIP_LARGA");
				hmald.put(id_def_alert, datos);
			}
			return hmald;
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error("ERROR::Fallo al recuperar la informacion de la ALD1");
			System.out.println("ERROR::Fallo al recuperar la informacion de la ALD1");
			return null;
		}
    }
    
    private static Vector<String>getInsertsALG1(){
    	String msg="Ppal::getInsertsALG1::Generando inserts sobre ALG1...";
    	LOGGER.info(msg);
    	System.out.println(msg);
    	try {
    		Vector<String>res = new Vector<String>();
    		Map<String, ProcesoCLS> map = hmProcesos;
			for (Map.Entry<String, ProcesoCLS> entry : map.entrySet()) {
			    res=hmProcesos.get(entry.getKey()).getInserts(res);
			}
			msg="Ppal::getInsertsALG1::Inserts generados. Total "+res.size()+" registros.";
			LOGGER.info(msg);
	    	System.out.println(msg);
			return res;
		} catch (Exception e) {
			// TODO: handle exception
			msg="Ppal::getInsertsALG1::ERROR::Fallo al generar los inserts en ALG1. No se insertaran los mensajes";
	    	LOGGER.info(msg);
	    	System.out.println(msg);
	    	msg=e.toString();
	    	LOGGER.info(msg);
	    	System.out.println(msg);
			return null;
		}
    }
    
    private static Vector<String>getErrorsRLT1(){
    	String msg="Ppal::getErrorsRLT1::Extrayendo mensajes errones para marcar en la RLT1... ";
    	LOGGER.info(msg);
    	System.out.println(msg);
    	try {
    		Vector<String>res = new Vector<String>();
    		Map<String, ProcesoCLS> map = hmProcesos;
			for (Map.Entry<String, ProcesoCLS> entry : map.entrySet()) {
			    res=hmProcesos.get(entry.getKey()).getInsertsErrors(res);
			}
			msg="Ppal::getErrorsRLT1::Errores generados. Total "+res.size()+" registros.";
			LOGGER.info(msg);
			System.out.println(msg);
			return res;
		} catch (Exception e) {
			// TODO: handle exception
			msg="Ppal::getErrorsRLT1::ERROR::Fallo al generar los inserts en RLT1. No se insertaran los mensajes";
			LOGGER.error(msg);
			System.out.println(msg);
			return null;
		}
    }
    
    private static void realizaInserciones(Connection obj_con, PreparedStatement st, String tabla, Vector<String>inserts){
    	String msg="";
    	try {
    		msg="Ppal::realizaInserciones::Realizando inserciones en "+tabla+". "+inserts.size()+" registros... ";
        	LOGGER.info(msg);
        	System.out.println(msg);
    		int totalinserts=0;
			for(int i=0;i<inserts.size();i++){
				totalinserts+=realizaActualizacion(obj_con, st, inserts.get(i));
			}
			msg="Ppal::realizaInserciones::"+tabla+"::"+totalinserts+" de "+inserts.size()+" inserciones realizdas.";
			LOGGER.info(msg);
			System.out.println(msg);
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error("Ppal::realizaInserciones::ERROR::Error al realizar inserciones sobre la tabla "+tabla);
			LOGGER.error(e.toString());
			System.out.println("Ppal::realizaInserciones::ERROR::Error al realizar inserciones sobre la tabla "+tabla);
			System.out.println(e.toString());
		}
    }
    
    private static int realizaActualizacion(Connection obj_con, PreparedStatement st, String query){
    	
    	try {
    		Ppal.configureConnection(obj_con, "AlertasBarrido", "realizaActualizacion");
    		st=obj_con.prepareStatement(query);
			st.executeUpdate();
			return 1;
		} catch (Exception e) {
			
			System.out.println("Fallo al realizar actualizacion: "+query);
			System.out.println(e.toString());
			return 0;
		}
    	
    }
    
    private static void marcaUsadosTPG1(PreparedStatement st, Vector<String>oids){
    	try {
    		while(oids.size()>0){
        		Vector<String>oidsaux=new Vector<String>();
        		while(oidsaux.size()<990 && oids.size()>0){
        			oidsaux.add(oids.get(0));
        			oids.remove(0);
        		}
        		configureConnection(obj_con, "AlertasBarrido", "realizaActualizacion");
        		String q=QuerysStr.queryMarcadoTPG1(oidsaux);
        		st = obj_con.prepareStatement(q);
        		st.setString(1, QuerysConfig.marcaProceso);
        		for(int i=0;i<oids.size()-1;i++){
        			st.setString((i+2), oids.get(i));
        		}
        		st.setString((oids.size()+1), oids.get(oids.size()-1));
        		
        		st.executeUpdate();
        	}
		} catch (Exception e) {
			String msg="Error al marcar los registros de la TPG1 como usados.";
			LOGGER.error(msg);
			System.out.println(msg);
		}
    }

    private static boolean generaEstadisiticasALG1(){
    	
    	String msg="Ppal::generaEstadisiticasALG1::Generando estadisticas...";
    	LOGGER.info(msg);
    	System.out.println(msg);
    	try {
			String q=QuerysStr.queryGeneraEstadisticas();
			configureConnection(obj_con, "AlertasBarrido", "queryGeneraEstadisticas");
			//ResultSet rs=stmt.executeQuery(q);
			
			PreparedStatement psta = Ppal.obj_con.prepareStatement(q);
            ResultSet rs = psta.executeQuery();
			
            msg="Ppal::generaEstadisiticasALG1::Estadisticas generadas.";
    		LOGGER.info(msg);
        	System.out.println(msg);
        	msg="Ppal::generaEstadisiticasALG1::Insertando estadísticas...";
        	LOGGER.info(msg);
        	System.out.println(msg);
        	configureConnection(obj_con, "AlertasBarrido", "InsertaEstadisticas");
            
			while(rs.next()){
				String proc=rs.getString("PROCESO");
				String ald1=rs.getString("ALD1_OID");
				String estat=rs.getString("ESTADISTICA");
				String cuenta=String.valueOf(rs.getInt("CUENTA"));
				String mensaje=cuenta+" "+estat;
				String insert=QuerysStr.insertALG1(proc, ald1, mensaje, "ESTADISTICA"); //'"+QuerysConfig.marcaProceso+"'
        		stmt = obj_con.prepareStatement(insert);
        		stmt.setString(1, proc);
        		stmt.setString(2, ald1);
        		stmt.setString(3, mensaje);
        		stmt.setString(4, "ESTADISTICA");
        		stmt.setString(5, QuerysConfig.marcaProceso);
        		stmt.executeUpdate();
        	}
        	msg="Ppal::generaEstadisiticasALG1::Estadísticas insertadas.";
        	LOGGER.info(msg);
        	System.out.println(msg);
        	return true;
		} catch (Exception e) {
			// TODO: handle exception
			msg="Ppal::generaEstadisiticasALG1::ERROR::Error al crear las estadisticas.";
			LOGGER.info(msg);
	    	System.out.println(msg);
	    	msg=e.toString();
	    	LOGGER.info(msg);
	    	System.out.println(msg);
	    	return false;
		}	
    	
    }
    public static void configureConnection(Connection connection, String modulo, String action){
    	CallableStatement call = null;
    	try{
    		call=connection.prepareCall("begin DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); end;");
    		call.setString(1,modulo);
    		call.setString(2,action);
    		call.execute();
    	}catch(Exception e){
    		System.out.println("AlertasBarrido::configureConnection::ERROR::Fallo al configurar modulo y accion");
    		LOGGER.error("AlertasBarrido::configureConnection::ERROR::Fallo al configurar modulo y accion");
    		System.out.println(e.toString());
    		LOGGER.error(e.toString());
    	}finally{
    		try {
    			call.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
    	}
    }
}
