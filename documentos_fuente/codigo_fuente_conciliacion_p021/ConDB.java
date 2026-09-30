package jdbc;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.jdbc.pool.OracleDataSource;
import util.Metodos;


public class ConDB {
	public static String url ;		public static String user ;	public static String password;		
	public static OracleDataSource obj_ods=null ;			public static int cont_conexion=0;

	public static ArrayList<String> insercionesRLT1_Proceso = new ArrayList<String>();
	public static ArrayList<String> insercionesRLT1_Reportes = new ArrayList<String>();
	public static ArrayList<String> updatesFAB1 = new ArrayList<String>();
	public static int contadorInsercionesRLT1 = 0;
	public static int contadorUpdatesFAB1 = 0;
	public static int contadorPL = 0;

	public void ObtenerCredenciales()    {

		String pathpro = "/pr/kytl/online/multipais/multicanal/cfg/entorno/";
		String pathpre = "/pp/kytl/online/multipais/multicanal/cfg/entorno/";
		String pathint = "/ei/kytl/online/multipais/multicanal/cfg/entorno/";
		String pathdev = "/de/kytl/online/multipais/multicanal/cfg/entorno/";
		String path = "";
		String env = "";
		File folderpro = new File(pathpro);
		File folderpre = new File(pathpre);
		File folderint = new File(pathint);
		File folderdev = new File(pathdev);
		Connection connection = null;

		if (folderpro.exists()) {
			env = "pr";
			path = pathpro + "credentials.xml";
		} else if (folderpre.exists()) {
			env = "pp";
			path = pathpre + "credentials.xml";
		} else if (folderint.exists()) {
			env = "ei";
			path = pathint + "credentials.xml";
		} else if (folderdev.exists()) {
			env = "de";
			path = pathdev + "credentials.xml";
		}
		/*String path = "";
		String env = "ei";
		//    	pathpre  = "C:/Users/profe/Java/RDR_QueryExecutor/credentials/"; 
		String pathint = "C:/Users/XE48467/Desktop/DatosConfiguraciones/"; 
		//    	pathdev = "D:/Proyectos/Conciliacion/Recursos_CRED_PRP_LOG/"; // JL-LOLO
		//      pathdev = "C:/Users/profe/Java/RDR_QueryExecutor/credentials/"; 
		//    	folderpro = new File(pathpro);
		//    	folderpre = new File(pathpre);
		//File  	folderint = new File(pathint);
		//   	folderdev = new File(pathdev);

		//    	if (folderpro.exists()){	path = pathpro + "credentials.xml";}  
		//    	if (folderpre.exists()) {	path = pathpre + "credentialsPP84.xml";	} 
		//   	if (folderpre.exists()) {	path = pathpre + "credentialsPP87.xml";	} 
		//    	if (folderpre.exists()) {	path = pathpre + "credentialsPP87_retocado.xml";	} 
		if (folderint.exists()) {	path = pathint + "credentialsEI.xml";	} 
		//   	if (folderint.exists()) {	path = pathint + "credentialsEI84.xml";	} 
		//    	if (folderdev.exists()){	path = pathdev + "credentialsDE.xml";	}
*/

		File archivo = null;		FileReader fr = null;		BufferedReader br = null;	archivo = new File(path);

		try 	{
			fr = new FileReader(archivo);	br = new BufferedReader(fr);
			String linea;	String fich = "";
			try {
				while ((linea = br.readLine()) != null){    fich = fich + linea;	}
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();	}

			String host=null;	String host2=null;	String port=null;	String sid = null;	String userdb=null;	String passdb=null;

			Pattern patronAux = Pattern.compile("<database>(.*?)</database>");
			Matcher matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	fich = matchAux.group(1);    	}

			patronAux = Pattern.compile("<sid>(.*?)</sid>");	    matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	sid = matchAux.group(1);	}

			patronAux = Pattern.compile("<host>(.*?)</host>");	    matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	host = matchAux.group(1);	}

			patronAux = Pattern.compile("<host2>(.*?)</host2>");	    matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	host2 = matchAux.group(1);	}

			patronAux = Pattern.compile("<port>(.*?)</port>");	    matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	port = matchAux.group(1);	}

			patronAux = Pattern.compile("<gcuser>(.*?)</gcuser>");  matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	userdb = matchAux.group(1);	}

			patronAux = Pattern.compile("<gcpass>(.*?)</gcpass>");  matchAux = patronAux.matcher(fich);
			if (matchAux.find()) {	passdb = matchAux.group(1);	}

			String urldb = null;

			if ("de".equals(env) || "ei".equals(env)) {
				urldb = "jdbc:oracle:thin:@" + host + ":" + port + "/" + sid; // Nuevo 20200420
			} else {
				urldb = "jdbc:oracle:thin:@(DESCRIPTION=(FAILOVER=ON)(ADDRESS_LIST=(LOAD_BALANCE=OFF)"
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST =" + host + ")(PORT = " + port + " )) "
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST =" + host2 + ")(PORT = " + port +"))) "
						+ "(CONNECT_DATA=(SERVICE_NAME=" +sid + ")))";
			}
			Class.forName("oracle.jdbc.OracleDriver");
			ConDB.url = urldb;		ConDB.user = userdb;	ConDB.password = passdb;
			System.out.println("Conectando a ..... "+ urldb);

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 	
	}

	public Connection ObtenerConexion(){
		Connection obj_con=null;
		ConDB.cont_conexion++;
		try {
			obj_ods = new OracleDataSource();	    obj_ods.setURL(ConDB.url);	    obj_ods.setUser(ConDB.user);    obj_ods.setPassword(ConDB.password);   
			obj_ods.setMaxStatements(5);
			obj_con=obj_ods.getConnection();
			System.out.println("Conexion realizada numero "+ ConDB.cont_conexion);

		} catch (SQLException e) {    System.out.println("No se ha podido realizar la conexion.");	    e.printStackTrace();	}
		return obj_con;
	}


	public ArrayList<String> createQuery(String param,String cabecera)	{
		ArrayList<String> array = new ArrayList<String>();
		Metodos met = new Metodos();
		long start = System.currentTimeMillis();
		try {
			Connection connection = this.ObtenerConexion();

			String query = param;		System.out.println("La query es : " + param);

			Statement stmnt;		stmnt = connection.createStatement();
			ResultSet rs = null;		rs = stmnt.executeQuery(query);
			int i = 0;

			String[] parts = cabecera.split(";");
			String s = "";
			while(rs.next()) {
				s = "";
				i++;
				if (i % 1000 == 0)
				{
					String tit = i + "datos recuperados";
					met.time(start, tit);
				}
				for (int m = 0; m<parts.length-2;m++)
				{
					s = s + rs.getString(parts[m]) + ";";
				}
				s = s + rs.getString(parts[parts.length-1]);

				array.add(s);

			}
			stmnt.close();		connection.close();

		} catch (SQLException e) {
			e.printStackTrace();		}
		return array;

	}

	public long crearJOB(String jobId,String Servicio)	{
		//Insercion de job
		Statement statement = null;

		String insertTableSQL = "Insert into FT_T_JBLG (JOB_ID,RQST_TRN_ID,JOB_STAT_TYP,JOB_START_TMS,JOB_END_TMS,TASK_TOT_CNT,TASK_CMPLTD_CNT,JOB_INPUT_TXT,JOB_CONFIG_TXT,RQST_CORR_ID,TASK_SUCCESS_CNT,TASK_FAILED_CNT,LAST_UPD_TMS,MAX_RECORD_SEQ_NUM,PRNT_JOB_ID,JOB_MSG_TYP,JOB_TME_TXT,JOB_TPS_CNT,TASK_PARTIAL_CNT,TASK_FILTERED_CNT,INSTANCE_ID,PEVL_OID) "
				+"values ('" + jobId
				+"',null,'OPEN  '"
				+",sysdate,sysdate"
				+",0,0,null,null,null,0,0"
				+",sysdate"
				+",null,null,'"
				+ Servicio
				+"',null,0,0,0,null,null)";
		Connection connection = this.ObtenerConexion();
		try 	{
			statement = connection.createStatement();
			//System.out.println(insertTableSQL);

			// execute insert SQL stetement
			statement.executeUpdate(insertTableSQL);  

			System.out.println("Record is inserted into FT_T_JBLG");

		} catch (SQLException e) {
			e.printStackTrace();		}

		return System.currentTimeMillis();

	}



	public long crearJOB(String jobId,String Servicio, Connection connection)	{
		//Insercion de job
		Statement statement = null;

		String insertTableSQL = "Insert into FT_T_JBLG (JOB_ID,RQST_TRN_ID,JOB_STAT_TYP,JOB_START_TMS,JOB_END_TMS,TASK_TOT_CNT,TASK_CMPLTD_CNT,JOB_INPUT_TXT,JOB_CONFIG_TXT,RQST_CORR_ID,TASK_SUCCESS_CNT,TASK_FAILED_CNT,LAST_UPD_TMS,MAX_RECORD_SEQ_NUM,PRNT_JOB_ID,JOB_MSG_TYP,JOB_TME_TXT,JOB_TPS_CNT,TASK_PARTIAL_CNT,TASK_FILTERED_CNT,INSTANCE_ID,PEVL_OID) "
				+"values ('" + jobId
				+"',null,'OPEN  '"
				+",sysdate,sysdate"
				+",0,0,null,null,null,0,0"
				+",sysdate"
				+",null,null,'"
				+ Servicio
				+"',null,0,0,0,null,null)";
		//Connection connection = this.ObtenerConexion();
		try 	{
			statement = connection.createStatement();
			//System.out.println(insertTableSQL);

			// execute insert SQL stetement
			statement.executeUpdate(insertTableSQL);  

			System.out.println("Record is inserted into FT_T_JBLG");
			statement.close();
		} catch (SQLException e) {e.printStackTrace();		}


		return System.currentTimeMillis();

	}



	public void cerrarJOB(String jobId,String Servicio,long inicio, Connection connection)	{
		//Insercion de job
		Statement statement = null;
		long fin = System.currentTimeMillis();
		Integer tot = (int) (fin - inicio);
		int h = (int) ((tot / (1000*60*60)) % 24);
		int m = (int) ((tot / (1000*60)) % 60) - h * 60; 
		int s = (int) ((tot / 1000)) - m * 60;

		String updateTableSQL = "Update FT_T_JBLG set job_stat_typ = 'CLOSED',"
				+ "job_end_tms=sysdate,job_tme_txt='"+h+":"+m+":"+s+"'"
				+"where job_id = '" + jobId +"'";
		//Connection connection = this.ObtenerConexion();
		try 	{
			statement = connection.createStatement();
			System.out.println(updateTableSQL);

			// execute insert SQL stetement
			statement.executeUpdate(updateTableSQL);   

			statement.close();
			System.out.println("Record is closed into FT_T_JBLG");

		} catch (SQLException e) {e.printStackTrace();	}


	}


	public void cerrarJOB(String jobId,String Servicio,long inicio)	{
		//Insercion de job
		Statement statement = null;
		long fin = System.currentTimeMillis();
		Integer tot = (int) (fin - inicio);
		int h = (int) ((tot / (1000*60*60)) % 24);
		int m = (int) ((tot / (1000*60)) % 60) - h * 60; 
		int s = (int) ((tot / 1000)) - m * 60;

		String updateTableSQL = "Update FT_T_JBLG set job_stat_typ = 'CLOSED',"
				+ "job_end_tms=sysdate,job_tme_txt='"+h+":"+m+":"+s+"'"
				+"where job_id = '" + jobId +"'";
		Connection connection = this.ObtenerConexion();
		try 	{
			statement = connection.createStatement();
			System.out.println(updateTableSQL);

			// execute insert SQL stetement
			statement.executeUpdate(updateTableSQL);   

			System.out.println("Record is closed into FT_T_JBLG");

		} catch (SQLException e) {e.printStackTrace();	}


	}


	public void executeCONCLI_Hilos(ArrayList<HashMap<String,String>> arr, Connection connection){
		//    Connection connection = this.ObtenerConexion();
		CallableStatement cs=null;
		try    {
			for(int i = 0; i<arr.size();i++) {

				cs = connection.prepareCall(("{call CONCLI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));

				cs.setString(1,arr.get(i).get("VCH_DBC_COD_CCLIEN"));  	    	cs.setString(2,arr.get(i).get("VCH_DBC_XTI_TIPERSO"));
				cs.setString(3,arr.get(i).get("VCH_DBC_XTI_CTIPCL1")); 	    	cs.setString(4,arr.get(i).get("VCH_DBC_COD_DOCUM25"));
				cs.setString(5,arr.get(i).get("VCH_DBC_COD_DNOMB"));   	    	cs.setString(6,arr.get(i).get("VCH_DBC_DES_DENOMB"));
				cs.setString(7,arr.get(i).get("VCH_DBC_COD_CTPVIA"));  	    	cs.setString(8,arr.get(i).get("VCH_DBC_DES_CCALLE"));
				cs.setString(9,arr.get(i).get("VCH_DBC_QNU_CNUVIA"));  	    	cs.setString(10,arr.get(i).get("VCH_DBC_DES_CRESTO"));
				cs.setString(11,arr.get(i).get("VCH_DBC_DES_DPLAZA")); 	    	cs.setString(12,arr.get(i).get("VCH_DBC_DES_DPROVI"));
				cs.setString(13,arr.get(i).get("VCH_DBC_COD_CDIPOS")); 	    	cs.setString(14,arr.get(i).get("VCH_DBC_COD_CDIPEX"));
				cs.setString(15,arr.get(i).get("VCH_DBC_COD_CPAISN")); 	    	cs.setString(16,arr.get(i).get("VCH_DBC_COD_CCNO"));
				cs.setString(17,arr.get(i).get("VCH_DBC_COD_CNAE5"));  	    	cs.setString(18,arr.get(i).get("VCH_DBC_COD_TIPINS"));
				cs.setString(19,arr.get(i).get("VCH_DBC_COD_CLPANA")); 	    	cs.setString(20,arr.get(i).get("VCH_DBC_FEC_FNACIF"));
				cs.setString(21,arr.get(i).get("VCH_DBC_COD_FORSOCI"));	    	cs.setString(22,arr.get(i).get("VCH_DBC_XTI_CVIP"));
				cs.setString(23,arr.get(i).get("VCH_DBC_COD_IDIOMA")); 	    	cs.setString(24,arr.get(i).get("VCH_DBC_COD_OFIPPAL"));
				cs.setString(25,arr.get(i).get("DBC_XSN_EMPEXRET"));	    	cs.setString(26,arr.get(i).get("DBC_FEC_INIEXRET"));		
				cs.setString(27,arr.get(i).get("DBC_FEC_EMPEXRET"));			cs.setString(28,arr.get(i).get("FLD_JOB_ID"));
				cs.setString(29,arr.get(i).get("VCH_DBC_COD_LEI")); //20170920

				//if ((i % 99) == 0) {System.out.println("Traza executeCONCLI_Hilos - "+ (i+1) +" -" +arr.get(i).get("VCH_DBC_COD_CCLIEN")); }
				cs.execute(); //////////////////  Comentada por JL quitar comentario despues de pruebas
				cs.close();
			}


		} catch (SQLException e) {	e.printStackTrace();		}

		// try {	connection.close();    } catch (SQLException e) {	e.printStackTrace();		}

	}


	public void executeCONC460_Hilos(ArrayList<HashMap<String,String>> arr, Connection connection){
		//  Connection connection = this.ObtenerConexion();
		CallableStatement cs=null;
		try    {

			for(int i = 0; i<arr.size();i++) {

				boolean fechaCancelacionValida = comprobarFechaCancelacion (connection, arr.get(i));

				if (fechaCancelacionValida)
				{
					cs = connection.prepareCall(("{call CONC460 (?,?,?)}"));

					if (!arr.get(i).get("VCH_CCLIEN").equals("000000000"))
					{
						cs.setString(1,arr.get(i).get("VCH_CCLIEN"));  	    	cs.setString(2,arr.get(i).get("VCH_FOLIO"));
						cs.setString(3,arr.get(i).get("FLD_JOB_ID"));

						//if ((i % 99) == 0) {System.out.println("Traza executeCONCLI_Hilos - "+ (i+1) +" -" +arr.get(i).get("VCH_DBC_COD_CCLIEN")); }
						cs.execute(); 
						contadorPL++;
						cs.close();
					}
				}
			}


		} catch (SQLException e) {	e.printStackTrace();		}

		// try {	connection.close();    } catch (SQLException e) {	e.printStackTrace();		}

	}

	private boolean comprobarFechaCancelacion(Connection connection, HashMap<String, String> hashMap)
	{
		String fechaCancelacion = hashMap.get("VCH_FCANCEL");

		if (fechaCancelacion.equals("0001-01-01"))
		{
			return true;
		}
		else
		{
			String clientelaId = hashMap.get("VCH_CCLIEN");
			String numFolio = hashMap.get("VCH_FOLIO");
			String query = 
					"UPDATE " 
							+"  FT_T_FAB1 " 
							+"SET " 
							+"  STAT_DEF_ID     = 'NUMFOLII', " 
							+"  DATA_STAT_TYP   = 'INACTIVE', " 
							+"  LAST_CHG_USR_ID = 'CONC460', " 
							+"  LAST_CHG_TMS    = SYSDATE " 
							+"WHERE " 
							+"  DATA_STAT_TYP <> 'INACTIVE' " 
							+"AND STAT_DEF_ID = 'NUMFOLIO' " 
							+"AND INST_MNEM  IN " 
							+"  ( " 
							+"    SELECT " 
							+"      INST_MNEM " 
							+"    FROM " 
							+"      FT_T_FIID " 
							+"    WHERE " 
							+"      FINS_ID_CTXT_TYP = 'CLIENTELAID' " 
							+"    AND DATA_STAT_TYP  <> 'INACTIVE' " 
							+"    AND FINS_ID        = '"+clientelaId+"' " 
							+"  ) "
							+"AND FLD_VAL = '"+numFolio+"' ";

			updatesFAB1.add(query);
			// Las actualizaciones se realizan mediante el ThreadComprobacion

			return false;
			//        try
			//        {
			//            Statement stmnt = connection.createStatement();
			//            stmnt.execute(query);
			//        }
			//        catch (SQLException e)
			//        {
			//            e.printStackTrace();
			//        }

		}

		//    return false;
	}

	public void executeCONCLI_Hilos(ArrayList<HashMap<String,String>> arr){
		Connection connection = this.ObtenerConexion();
		CallableStatement cs;
		try    {
			for(int i = 0; i<arr.size();i++) {
				cs = connection.prepareCall(("{call CONCLI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));

				cs.setString(1,arr.get(i).get("VCH_DBC_COD_CCLIEN"));  	    	cs.setString(2,arr.get(i).get("VCH_DBC_XTI_TIPERSO"));
				cs.setString(3,arr.get(i).get("VCH_DBC_XTI_CTIPCL1")); 	    	cs.setString(4,arr.get(i).get("VCH_DBC_COD_DOCUM25"));
				cs.setString(5,arr.get(i).get("VCH_DBC_COD_DNOMB"));   	    	cs.setString(6,arr.get(i).get("VCH_DBC_DES_DENOMB"));
				cs.setString(7,arr.get(i).get("VCH_DBC_COD_CTPVIA"));  	    	cs.setString(8,arr.get(i).get("VCH_DBC_DES_CCALLE"));
				cs.setString(9,arr.get(i).get("VCH_DBC_QNU_CNUVIA"));  	    	cs.setString(10,arr.get(i).get("VCH_DBC_DES_CRESTO"));
				cs.setString(11,arr.get(i).get("VCH_DBC_DES_DPLAZA")); 	    	cs.setString(12,arr.get(i).get("VCH_DBC_DES_DPROVI"));
				cs.setString(13,arr.get(i).get("VCH_DBC_COD_CDIPOS")); 	    	cs.setString(14,arr.get(i).get("VCH_DBC_COD_CDIPEX"));
				cs.setString(15,arr.get(i).get("VCH_DBC_COD_CPAISN")); 	    	cs.setString(16,arr.get(i).get("VCH_DBC_COD_CCNO"));
				cs.setString(17,arr.get(i).get("VCH_DBC_COD_CNAE5"));  	    	cs.setString(18,arr.get(i).get("VCH_DBC_COD_TIPINS"));
				cs.setString(19,arr.get(i).get("VCH_DBC_COD_CLPANA")); 	    	cs.setString(20,arr.get(i).get("VCH_DBC_FEC_FNACIF"));
				cs.setString(21,arr.get(i).get("VCH_DBC_COD_FORSOCI"));	    	cs.setString(22,arr.get(i).get("VCH_DBC_XTI_CVIP"));
				cs.setString(23,arr.get(i).get("VCH_DBC_COD_IDIOMA")); 	    	cs.setString(24,arr.get(i).get("VCH_DBC_COD_OFIPPAL"));
				cs.setString(25,arr.get(i).get("ERR_JOB_ID"));

				//System.out.println("Traza executeCONCLI_Hilos - "+ i +" -" +arr.get(i).get("VCH_DBC_COD_CCLIEN"));
				cs.execute(); //////////////////  Comentada por JL quitar comentario despues de pruebas
			}

		} catch (SQLException e) {	e.printStackTrace();		}

		try {	connection.close();    } catch (SQLException e) {	e.printStackTrace();		}

	}


	public void executeCONCLI(String VCH_DBC_COD_CCLIEN,String VCH_DBC_XTI_TIPERSO,String VCH_DBC_XTI_CTIPCL1,
			String VCH_DBC_COD_DOCUM25,String VCH_DBC_COD_DNOMB,String VCH_DBC_DES_DENOMB,
			String VCH_DBC_COD_CTPVIA,String VCH_DBC_DES_CCALLE,String VCH_DBC_QNU_CNUVIA,
			String VCH_DBC_DES_CRESTO,String VCH_DBC_DES_DPLAZA,String VCH_DBC_DES_DPROVI,
			String VCH_DBC_COD_CDIPOS,String VCH_DBC_COD_CDIPEX,String VCH_DBC_COD_CPAISN,
			String VCH_DBC_COD_CCNO,String VCH_DBC_COD_CNAE5,String VCH_DBC_COD_TIPINS,
			String VCH_DBC_COD_CLPANA,String VCH_DBC_FEC_FNACIF,String VCH_DBC_COD_FORSOCI,
			String VCH_DBC_XTI_CVIP,String VCH_DBC_COD_IDIOMA,String VCH_DBC_COD_OFIPPAL,
			String ERR_JOB_ID)
	{
		Connection connection = this.ObtenerConexion();
		CallableStatement cs;
		try 
		{
			cs = connection.prepareCall(("{call CONCLI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));
			cs.setString(1,VCH_DBC_COD_CCLIEN);
			cs.setString(2,VCH_DBC_XTI_TIPERSO);
			cs.setString(3,VCH_DBC_XTI_CTIPCL1);
			cs.setString(4,VCH_DBC_COD_DOCUM25);
			cs.setString(5,VCH_DBC_COD_DNOMB);
			cs.setString(6,VCH_DBC_DES_DENOMB);
			cs.setString(7,VCH_DBC_COD_CTPVIA);
			cs.setString(8,VCH_DBC_DES_CCALLE);
			cs.setString(9,VCH_DBC_QNU_CNUVIA);
			cs.setString(10,VCH_DBC_DES_CRESTO);
			cs.setString(11,VCH_DBC_DES_DPLAZA);
			cs.setString(12,VCH_DBC_DES_DPROVI);
			cs.setString(13,VCH_DBC_COD_CDIPOS);
			cs.setString(14,VCH_DBC_COD_CDIPEX);
			cs.setString(15,VCH_DBC_COD_CPAISN);
			cs.setString(16,VCH_DBC_COD_CCNO);
			cs.setString(17,VCH_DBC_COD_CNAE5);
			cs.setString(18,VCH_DBC_COD_TIPINS);
			cs.setString(19,VCH_DBC_COD_CLPANA);
			cs.setString(20,VCH_DBC_FEC_FNACIF);
			cs.setString(21,VCH_DBC_COD_FORSOCI);
			cs.setString(22,VCH_DBC_XTI_CVIP);
			cs.setString(23,VCH_DBC_COD_IDIOMA);
			cs.setString(24,VCH_DBC_COD_OFIPPAL);
			cs.setString(25,ERR_JOB_ID);

			cs.execute();
		} catch (SQLException e) {	e.printStackTrace();		}

		try {
			connection.close();
		} catch (SQLException e) {	e.printStackTrace();		}

	}


	public void executeCONBDI_Hilos(ArrayList<HashMap<String,String>> arr, Connection connection){
		//Connection connection = this.ObtenerConexion();
		CallableStatement cs;
		try 	{
			for(int i = 0; i<arr.size();i++) {
				cs = connection.prepareCall(("{call CONBDI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));

				cs.setString(1,arr.get(i).get("FLD_COD_CLINTERN"));  	    cs.setString(2,arr.get(i).get("FLD_DES_NOMCORT1"));
				cs.setString(3,arr.get(i).get("FLD_DES_NOMCORT2")); 	    cs.setString(4,arr.get(i).get("FLD_DES_NOMCLINT"));		
				cs.setString(5,arr.get(i).get("FLD_COD_INSTITUC"));   	    cs.setString(6,arr.get(i).get("FLD_DES_CALLE"));			
				cs.setString(7,arr.get(i).get("FLD_DES_DISPLAZA2"));  	   	cs.setString(8,arr.get(i).get("FLD_DES_PROVPAIS"));
				cs.setString(9,arr.get(i).get("FLD_COD_BANCOTES"));  	    cs.setString(10,arr.get(i).get("FLD_COD_PLAZATES"));			
				cs.setString(11,arr.get(i).get("FLD_QNU_BIC")); 	    	cs.setString(12,arr.get(i).get("FLD_COD_PLAZAINT"));
				cs.setString(13,arr.get(i).get("FLD_COD_BROKERWS")); 	    cs.setString(14,arr.get(i).get("FLD_CDNITR"));			
				cs.setString(15,arr.get(i).get("FLD_COD_CLPANA")); 	    	cs.setString(16,arr.get(i).get("FLD_COD_CPAISN"));		    	
				cs.setString(17,arr.get(i).get("FLD_COD_CNAE")); 	    	cs.setString(18,arr.get(i).get("FLD_COD_CBANCO"));
				cs.setString(19,arr.get(i).get("FLD_XTI_TIPOSBIC"));	    cs.setString(20,arr.get(i).get("FLD_JOB_ID"));
				cs.setString(21,arr.get(i).get("COD_CDIPEX"));// NUEVO SWIFT ISO 20022

				//if ((i % 99) == 0) {System.out.println("Traza executeCONBDI_Hilos - "+ i +" -" +arr.get(i).get("FLD_COD_CLINTERN"))};
				cs.execute(); //////////////////  Comentada por JL quitar comentario despues de pruebas
				cs.close();
			}

		} catch (SQLException e) {	e.printStackTrace();		}

		//try {	connection.close();   } catch (SQLException e) {	e.printStackTrace();		}

	}


	public void executeCONBDI_Hilos(ArrayList<HashMap<String,String>> arr){
		Connection connection = this.ObtenerConexion();
		CallableStatement cs;
		try 	{
			for(int i = 0; i<arr.size();i++) {
				cs = connection.prepareCall(("{call CONBDI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));

				cs.setString(1,arr.get(i).get("FLD_COD_CLINTERN"));  	    	cs.setString(2,arr.get(i).get("FLD_DES_NOMCORT1"));
				cs.setString(3,arr.get(i).get("FLD_DES_NOMCORT2")); 	    	cs.setString(4,arr.get(i).get("FLD_DES_NOMCLINT"));		
				cs.setString(5,arr.get(i).get("FLD_COD_INSTITUC"));   	    	cs.setString(6,arr.get(i).get("FLD_DES_CALLE"));			
				cs.setString(7,arr.get(i).get("FLD_DES_DISPLAZA"));  	    	cs.setString(8,arr.get(i).get("FLD_DES_PROVPAIS"));
				cs.setString(9,arr.get(i).get("FLD_COD_BANCOTES"));  	    	cs.setString(10,arr.get(i).get("FLD_COD_PLAZATES"));			
				cs.setString(11,arr.get(i).get("FLD_QNU_BIC")); 	    	cs.setString(12,arr.get(i).get("FLD_COD_PLAZAINT"));
				cs.setString(13,arr.get(i).get("FLD_COD_BROKERWS")); 	    	cs.setString(14,arr.get(i).get("FLD_CDNITR"));			
				cs.setString(15,arr.get(i).get("FLD_COD_CLPANA")); 	    	cs.setString(16,arr.get(i).get("FLD_COD_CPAISN"));		    	
				cs.setString(17,arr.get(i).get("FLD_COD_CNAE")); 	    	cs.setString(18,arr.get(i).get("FLD_COD_CBANCO"));
				cs.setString(19,arr.get(i).get("FLD_JOB_ID"));

				//System.out.println("Traza executeCONBDI_Hilos - "+ i +" -" +arr.get(i).get("FLD_COD_CLINTERN"));
				// cs.execute(); //////////////////  Comentada por JL quitar comentario despues de pruebas
			}

		} catch (SQLException e) {	e.printStackTrace();		}

		try {	connection.close();   } catch (SQLException e) {	e.printStackTrace();		}

	}


	public void executeCONBDI(String FLD_COD_CLINTERN,
			String FLD_DES_NOMCORT1,String FLD_DES_NOMCORT2,
			String FLD_DES_NOMCLINT,String FLD_COD_INSTITUC,String FLD_DES_CALLE,
			String FLD_DES_DISPLAZA,String FLD_DES_PROVPAIS,
			String FLD_COD_BANCOTES,String FLD_COD_PLAZATES,String FLD_QNU_BIC,
			String FLD_COD_PLAZAINT,String FLD_COD_BROKERWS,String FLD_CDNITR,
			String FLD_COD_CLPANA,String FLD_COD_CPAISN,String FLD_COD_CNAE,
			String FLD_COD_CBANCO,String FLD_JOB_ID)
	{
		Connection connection = this.ObtenerConexion();
		CallableStatement cs;
		try 	{
			cs = connection.prepareCall(("{call CONBDI2 (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));
			cs.setString(1,FLD_COD_CLINTERN);
			cs.setString(2,FLD_DES_NOMCORT1);
			cs.setString(3,FLD_DES_NOMCORT2);
			cs.setString(4,FLD_DES_NOMCLINT);
			cs.setString(5,FLD_COD_INSTITUC);
			cs.setString(6,FLD_DES_CALLE);
			cs.setString(7,FLD_DES_DISPLAZA);
			cs.setString(8,FLD_DES_PROVPAIS);
			cs.setString(9,FLD_COD_BANCOTES);
			cs.setString(10,FLD_COD_PLAZATES);
			cs.setString(11,FLD_QNU_BIC);
			cs.setString(12,FLD_COD_PLAZAINT);
			cs.setString(13,FLD_COD_BROKERWS);
			cs.setString(14,FLD_CDNITR);
			cs.setString(15,FLD_COD_CLPANA);
			cs.setString(16,FLD_COD_CPAISN);
			cs.setString(17,FLD_COD_CNAE);
			cs.setString(18,FLD_COD_CBANCO);
			cs.setString(19,FLD_JOB_ID);

			cs.execute();
		} catch (SQLException e) {	e.printStackTrace();	}

		try 	{
			connection.close();
		} catch (SQLException e) {	e.printStackTrace();	}

	}


	public void insertRLT1BDI(ArrayList<String> noConcilia,String jobId, Connection connection, String msg)	{
		//Connection connection = this.ObtenerConexion();

		//Bucle de inserciones en la RLT1

		for (String noconc : noConcilia)	{
			String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
					+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
					+ "SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
					+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,"
					+ "RLT_DIF_ACC) "
					+ "values (new_oid,'" + jobId + "',null,null,1,"
					+ "'"+msg+"',null,"
					+ "'REPORTES','BDI','BDI Id Fichero',null,"
					+ "'BDI Id en RDR','" + noconc +"','BDI Id en RDR','" 
					+ noconc + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

			//		System.out.println("El insert: " + insertTableSQL);
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement .executeUpdate();

				preparedStatement.close();

			} catch (SQLException e) {	e.printStackTrace();	}

		}
		// try 	{connection.close(); } catch (SQLException e) 	{	e.printStackTrace();	}

	}



	public void insertRLT1BDI(ArrayList<String> noConcilia,String jobId)	{
		Connection connection = this.ObtenerConexion();

		//Bucle de inserciones en la RLT1

		for (String noconc : noConcilia)	{
			String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
					+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
					+ "SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
					+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,"
					+ "RLT_DIF_ACC) "
					+ "values (new_oid,'" + jobId + "',null,null,1,"
					+ "'Codigo BDI en RDR que no concilia en BDI',null,"
					+ "'REPORTES','BDI','BDI Id Fichero',null,"
					+ "'BDI Id en RDR','" + noconc +"','BDI Id en RDR','" 
					+ noconc + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

			//		System.out.println("El insert: " + insertTableSQL);
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement .executeUpdate();

				preparedStatement.close();

			} catch (SQLException e) {	e.printStackTrace();	}

		}
		try 	{
			connection.close();
		} catch (SQLException e) 	{	e.printStackTrace();	}

	}


	public void insertRLT1Clientela(ArrayList<String> noConcilia,String jobId, Connection connection, String msg)
	{
		//Connection connection = this.ObtenerConexion();

		//Bucle de inserciones en la RLT1
		//int i=0;
		for (String noconc : noConcilia)
		{

			//i++;
			//if ((i % 99) == 0) {System.out.println("Traza insertRLT1ClientelaHilos - "+ (i+1) +" -" +noConcilia.get(i) + " - "+ msg); }

			//if (i % 10000 == 0)	{    System.out.println("Linea de insercion -"+i);	}
			String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
					+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
					+ "SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
					+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,"
					+ "RLT_DIF_ACC) "
					+ "values (new_oid,'" + jobId + "',null,null,1,"
					+ "'"+ msg +"',null,"
					+ "'REPORTES','CLIENTELA','Clientela Id Fichero',null,"
					+ "'Clientela Id en RDR','" + noconc +"','Clientela Id en RDR','" 
					+ noconc + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

			//System.out.println("El insert: " + insertTableSQL);
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement .executeUpdate();

				preparedStatement.close();

			} catch (SQLException e) {	e.printStackTrace();	}

		}
		/*try 	{
		connection.close();
	} catch (SQLException e) {	e.printStackTrace();	}*/

	}


	public void insertRLT1ClientelaC460_Proceso(ArrayList<String> listaMnemClientelaIdNoEncontrado,String jobId, Connection connection)
	{
		String RLT_DIF_STAT = "PENDING";
		String RLT_DIF_ACC = "A460";
		String RLT_PURP_TYP = "PROCESO";
		String DATA_SRC_APP = "ALTA_CPARTY";
		String MAIN_ENTITY_NME = "MNEM_LOCAL";
		String MESSAGE_RLT = "Contrato 460 pendiente de dar de alta";
		String main_entity_id;
		String rlt_field;

		for (String mnemLocal : listaMnemClientelaIdNoEncontrado)
		{
			main_entity_id = mnemLocal;
			rlt_field = mnemLocal;
			//i++;
			//if ((i % 99) == 0) {System.out.println("Traza insertRLT1ClientelaHilos - "+ (i+1) +" -" +noConcilia.get(i) + " - "+ msg); }

			//if (i % 10000 == 0)	{    System.out.println("Linea de insercion -"+i);	}
			String insertTableSQL =  "INSERT " 
					+"INTO " 
					+"  ft_t_rlt1 " 
					+"  ( " 
					+"    rlt_oid, " 
					+"    JOB_ID, " 
					+"    main_entity_nme, " 
					+"    main_entity_id, " 
					+"    rlt_dif_stat, " 
					+"    rlt_dif_acc, " 
					+"    message_rlt, " 
					+"    rlt_purp_typ, " 
					+"    data_src_app, " 
					+"    rlt_field, " 
					+"    start_tms, " 
					+"    last_chg_tms, " 
					+"    last_chg_usr_id " 
					+"  ) " 
					+"SELECT " 
					+"  new_oid, " 
					+"  '"+jobId+"', " 
					+"  '"+MAIN_ENTITY_NME+"', " 
					+"  '"+main_entity_id+"', " 
					+"  '"+RLT_DIF_STAT+"', " 
					+"  '"+RLT_DIF_ACC+"', " 
					+"  '"+MESSAGE_RLT+"', " 
					+"  '"+RLT_PURP_TYP+"', " 
					+"  '"+DATA_SRC_APP+"', " 
					+"  '"+rlt_field+"', " 
					+"  systimestamp, " 
					+"  systimestamp, " 
					+"  'CONC460' " 
					+"FROM " 
					+"  dual " 
					+"WHERE " 
					+"  NOT EXISTS " 
					+"  ( " 
					+"    SELECT " 
					+"      1 " 
					+"    FROM " 
					+"      ft_t_rlt1 " 
					+"    WHERE " 
					+"      main_entity_id = '"+main_entity_id+"' " 
					+"    AND message_rlt  = '"+MESSAGE_RLT+"' " 
					+"    AND rlt_dif_acc  = '"+RLT_DIF_ACC+"' " 
					+"    AND rlt_field    = '"+rlt_field+"' "
					+"    AND rlt_purp_typ = '"+RLT_PURP_TYP+"' " 
					+"    AND data_src_app = '"+DATA_SRC_APP+"' " 
					+"    AND rlt_dif_stat = '"+RLT_DIF_STAT+"' " 
					+"  )";

			insercionesRLT1_Proceso.add(insertTableSQL);

			//System.out.println("El insert: " + insertTableSQL);
			//		PreparedStatement preparedStatement;
			//		try {
			//			preparedStatement = connection.prepareStatement(insertTableSQL);
			//			preparedStatement .executeUpdate();
			//			contadorInsercionesRLT1++;
			//			preparedStatement.close();
			//
			//		} catch (SQLException e) {	e.printStackTrace();	}

		}
		/*try 	{
		connection.close();
	} catch (SQLException e) {	e.printStackTrace();	}*/

	}

	public void insertRLT1ClientelaC460_Reporte(ArrayList<String> listaClientelaIdNoEncontrado,String jobId, Connection connection)
	{
		String RLT_DIF_STAT = "PENDING";
		String RLT_DIF_ACC = "A_REPORTE";
		String RLT_PURP_TYP = "REPORTES";
		String DATA_SRC_APP = "C460_P";
		String MAIN_ENTITY_NME = "CLIENTELA_ID";
		String MESSAGE_RLT = "Contrato 460 pendiente de dar de alta";
		String main_entity_id;
		String rlt_field;

		for (String clientelaidMnem : listaClientelaIdNoEncontrado)
		{
			String [] split = clientelaidMnem.split(";");
			//		if (split.length == 2)
			//		{
			main_entity_id = split[0];
			rlt_field = split[1];
			//i++;
			//if ((i % 99) == 0) {System.out.println("Traza insertRLT1ClientelaHilos - "+ (i+1) +" -" +noConcilia.get(i) + " - "+ msg); }

			//if (i % 10000 == 0)	{    System.out.println("Linea de insercion -"+i);	}
			String insertTableSQL =  "INSERT " 
					+"INTO " 
					+"  ft_t_rlt1 " 
					+"  ( " 
					+"    rlt_oid, " 
					+"    JOB_ID, " 
					+"    main_entity_nme, " 
					+"    main_entity_id, " 
					+"    rlt_dif_stat, " 
					+"    rlt_dif_acc, " 
					+"    message_rlt, " 
					+"    rlt_purp_typ, " 
					+"    data_src_app, " 
					+"    rlt_field, " 
					+"    start_tms, " 
					+"    last_chg_tms, " 
					+"    last_chg_usr_id " 
					+"  ) " 
					+"SELECT " 
					+"  new_oid, " 
					+"  '"+jobId+"', " 
					+"  '"+MAIN_ENTITY_NME+"', " 
					+"  '"+main_entity_id+"', " 
					+"  '"+RLT_DIF_STAT+"', " 
					+"  '"+RLT_DIF_ACC+"', " 
					+"  '"+MESSAGE_RLT+"', " 
					+"  '"+RLT_PURP_TYP+"', " 
					+"  '"+DATA_SRC_APP+"', " 
					+"  '"+rlt_field+"', " 
					+"  systimestamp, " 
					+"  systimestamp, " 
					+"  'CONC460' " 
					+"FROM " 
					+"  dual " 
					+"WHERE " 
					+"  NOT EXISTS " 
					+"  ( " 
					+"    SELECT " 
					+"      1 " 
					+"    FROM " 
					+"      ft_t_rlt1 " 
					+"    WHERE " 
					+"      main_entity_id = '"+main_entity_id+"' " 
					+"    AND message_rlt  = '"+MESSAGE_RLT+"' " 
					+"    AND rlt_dif_acc  = '"+RLT_DIF_ACC+"' " 
					+"    AND rlt_field    = '"+rlt_field+"' " 
					+"    AND rlt_purp_typ = '"+RLT_PURP_TYP+"' " 
					+"    AND data_src_app = '"+DATA_SRC_APP+"' " 
					+"    AND rlt_dif_stat = '"+RLT_DIF_STAT+"' " 
					+"    AND job_id = '"+jobId+"'"
					+"  )";

			insercionesRLT1_Reportes.add(insertTableSQL);

			//System.out.println("El insert: " + insertTableSQL);
			//			PreparedStatement preparedStatement;
			//			try {
			//				preparedStatement = connection.prepareStatement(insertTableSQL);
			//				preparedStatement .executeUpdate();
			//				contadorInsercionesRLT1++;
			//				preparedStatement.close();
			//	
			//			} catch (SQLException e) {	e.printStackTrace();	}
			//		}
		}
		/*try 	{
		connection.close();
	} catch (SQLException e) {	e.printStackTrace();	}*/

	}

	/*public void insertRLT1Clientela(ArrayList<String> noConcilia,String jobId, Connection connection)
{
	//Connection connection = this.ObtenerConexion();

	//Bucle de inserciones en la RLT1
	int i=0;
	for (String noconc : noConcilia)
	{

	    i++;
	    	if ((i % 99) == 0) {System.out.println("Traza insertRLT1ClientelaHilos - "+ (i+1) +" -" +noConcilia.get(i)); }

	    //if (i % 10000 == 0)	{    System.out.println("Linea -"+i);	}
		String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
				+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
				+ "SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
				+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,"
				+ "RLT_DIF_ACC) "
				+ "values (new_oid,'" + jobId + "',null,null,1,"
				+ "'Codigo Clientela en RDR que no concilia en Clientela',null,"
				+ "'REPORTES','CLIENTELA','Clientela Id Fichero',null,"
				+ "'Clientela Id en RDR','" + noconc +"','Clientela Id en RDR','" 
				+ noconc + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

		//System.out.println("El insert: " + insertTableSQL);
		PreparedStatement preparedStatement;
		try {
			preparedStatement = connection.prepareStatement(insertTableSQL);
			preparedStatement .executeUpdate();

			preparedStatement.close();

		} catch (SQLException e) {	e.printStackTrace();	}

	}
	//try 	{
	//	connection.close();
	//} catch (SQLException e) {	e.printStackTrace();	}

}*/




	public void insertRLT1Clientela(ArrayList<String> noConcilia,String jobId)
	{
		Connection connection = this.ObtenerConexion();

		//Bucle de inserciones en la RLT1

		for (String noconc : noConcilia)
		{
			String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
					+ "RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
					+ "SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
					+ "START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,"
					+ "RLT_DIF_ACC) "
					+ "values (new_oid,'" + jobId + "',null,null,1,"
					+ "'Codigo Clientela en RDR que no concilia en Clientela',null,"
					+ "'REPORTES','CLIENTELA','Clientela Id Fichero',null,"
					+ "'Clientela Id en RDR','" + noconc +"','Clientela Id en RDR','" 
					+ noconc + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

			//System.out.println("El insert: " + insertTableSQL);
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement .executeUpdate();

				preparedStatement.close();

			} catch (SQLException e) {	e.printStackTrace();	}

		}
		try 	{
			connection.close();
		} catch (SQLException e) {	e.printStackTrace();	}

	}

	public ArrayList<String> obtenerBDIs(Connection connection )	{
		ArrayList<String> array = new ArrayList<String>();
		try	{
			//Connection connection = this.ObtenerConexion();
			String query = "select DISTINCT FINS_ID BDI_ID from FT_T_FIID where "
					+ "fins_id_ctxt_typ='BDIID' AND DATA_STAT_TYP='ACTIVE'";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				array.add(rs.getString("BDI_ID"));
			}
			stmnt.close();		/*connection.close();*/

		} catch (SQLException e) {		e.printStackTrace();	}
		return array;

	}


	public ArrayList<String> obtenerBDIs()	{
		ArrayList<String> array = new ArrayList<String>();
		try	{
			Connection connection = this.ObtenerConexion();
			String query = "select DISTINCT FINS_ID BDI_ID from FT_T_FIID where "
					+ "fins_id_ctxt_typ='BDIID' AND DATA_STAT_TYP='ACTIVE'";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				array.add(rs.getString("BDI_ID"));
			}
			stmnt.close();		connection.close();

		} catch (SQLException e) {		e.printStackTrace();	}
		return array;

	}

	public ArrayList<String> obtenerCLIs(Connection connection )	{
		ArrayList<String> array = new ArrayList<String>();
		try	{
			//Connection connection = this.ObtenerConexion();
			String query = "select DISTINCT FINS_ID CLI_ID from FT_T_FIID where "
					+ "fins_id_ctxt_typ='CLIENTELAID' AND DATA_STAT_TYP='ACTIVE'"
					+ "and inst_mnem in (select inst_mnem from ft_t_firl where rel_typ = 'LOCAL')";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				array.add(rs.getString("CLI_ID"));
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return array;
	}

	public HashMap<String,String> obtenerClientelaIDBBVA(Connection connection )	{
		HashMap<String,String> hashMap = new HashMap<String,String>();
		try	{
			//Connection connection = this.ObtenerConexion();
			String query = 
					"SELECT DISTINCT " 
							+"  fiid.FINS_ID CLI_ID, finsl.INST_MNEM MNEM_LOCAL " 
							+"FROM ft_t_fiid fiid, " 
							+"  ft_t_firl firl, " 
							+"  ft_t_fins finsl, " 
							+"  ft_t_fins finso, " 
							+"  ft_t_eerl eerl, " 
							+"  ft_t_enfr enfr " 
							+"WHERE fiid.inst_mnem      = finsl.inst_mnem " 
							+"AND fiid.fins_id_ctxt_typ = 'CLIENTELAID' " 
							+"AND fiid.data_stat_typ    <> 'INACTIVE' " 
							+"AND firl.prnt_inst_mnem   = finsl.inst_mnem " 
							+"AND finso.inst_mnem       = firl.inst_mnem " 
							+"AND firl.rel_typ          = 'OPERATIVE' " 
							+"AND firl.finsrl_typ       = 'CPARTY  ' " 
							+"AND eerl.org_id           = enfr.org_id " 
							+"AND eerl.prnt_org_id      = '0182' " 
							+"AND enfr.finr_inst_mnem   = finso.inst_mnem " 
							+"AND enfr.data_stat_typ   <> 'INACTIVE' " 
							+"AND finsl.data_stat_typ  <> 'INACTIVE' " 
							+"AND finso.data_stat_typ  <> 'INACTIVE' "
							+"AND fiid.FINS_ID != '000000000'";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				hashMap.put(rs.getString("CLI_ID"), rs.getString("MNEM_LOCAL"));
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return hashMap;
	}

	public ArrayList<String> obtenerCLIs()	{
		ArrayList<String> array = new ArrayList<String>();
		try	{
			Connection connection = this.ObtenerConexion();
			String query = "select DISTINCT FINS_ID CLI_ID from FT_T_FIID where "
					+ "fins_id_ctxt_typ='CLIENTELAID' AND DATA_STAT_TYP='ACTIVE'"
					+ "and inst_mnem in (select inst_mnem from ft_t_firl where rel_typ = 'LOCAL')";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				array.add(rs.getString("CLI_ID"));
			}
			stmnt.close();		connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return array;
	}
	/*public static void closePooledConnections() throws SQLException{
    if (ConDB.obj_ods != null ) {
	ConDB.obj_ods.close();
    }
  }*/

	public HashMap<String, String> obtenerCliLEIsRDR(Connection connection )	{
		HashMap<String, String> array = new HashMap<String, String>();
		try	{
			//Connection connection = this.ObtenerConexion();
			String query = "select glo.fins_id canonico, "
					+ "listagg(cli.fins_id, ';') within group (order by cli.last_chg_tms) cli_id "
					+ "from ft_t_fiid cli, ft_t_fiid lei, ft_t_firl firl, ft_t_fiid glo, ft_t_fins fins "
					+ "where firl.inst_mnem = cli.inst_mnem "
					+ "and firl.rel_typ = 'LOCAL' "
					+ "and firl.data_stat_typ = 'ACTIVE' "
					+ "and cli.fins_id_ctxt_typ='CLIENTELAID' " 
					+ "and cli.data_stat_typ='ACTIVE' "
					+ "and lei.inst_mnem = firl.prnt_inst_mnem "
					+ "and lei.fins_id_ctxt_typ='LEIID' " 
					+ "and lei.data_stat_typ='ACTIVE' "
					+ "and glo.inst_mnem = firl.prnt_inst_mnem "
					+ "and glo.fins_id_ctxt_typ='FINSID' "
					+ "and glo.data_stat_typ='ACTIVE' "
					+ "and fins.inst_mnem = firl.prnt_inst_mnem "
					+ "and fins.data_stat_typ = 'ACTIVE' "
					+ "group by glo.fins_id, lei.fins_id having count(*) > 1";

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				array.put(rs.getString("CANONICO"), rs.getString("CLI_ID"));
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return array;
	}

	public void insertRLT1ClientelaLEI(String canonico, String lei1, String clients, String jobId, Connection connection)
	{

		String insertTableSQL = "INSERT INTO FT_T_VREQ (VND_RQST_OID,VND_RQSTR_ID,VND_RQST_TMS,VND_RQST_TYP,"
				+" VND_RQST_STAT_TYP,VND_RQST_STAT_TXT,VND_RQST_CORR_ID,LAST_CHG_TMS, LAST_CHG_USR_ID, "
				+" PHYSICAL_RQST_IND,VND_RQST_DATA_TYP,VND_SRVC_NME) "
				+" VALUES (NEW_OID,'"+jobId+"',SYSTIMESTAMP,"
				+ "nvl((select fiid.fins_id from ft_t_fiid fiid, ft_t_fiid fiid1 where fiid.inst_mnem=fiid1.inst_mnem "
				+ "and fiid.fins_id_ctxt_typ='LEIID' and fiid.data_stat_typ<>'INACTIVE' and fiid1.data_stat_typ<>'INACTIVE' "
				+ "and fiid1.fins_id_ctxt_typ='FINSID' and fiid1.fins_id='"+canonico+"'),'N/A'),'ACTIVE',"
				+" 'Ctpda Global "+canonico+" tiene los clientes "+clients+" con distinto LEI',"
				+" '"+lei1+"',SYSTIMESTAMP,'BBVA:CUSTOMER','X','"+canonico+"','CLIENTELA')";

		PreparedStatement preparedStatement;
		try {
			preparedStatement = connection.prepareStatement(insertTableSQL);
			preparedStatement.executeUpdate();

			preparedStatement.close();

		} catch (SQLException e) {	e.printStackTrace();	}

	}

	public void insertRLT1ClientelaC460_Error(ArrayList<String> listaMnemClientelaIdNoEncontrado,  String jobId, Connection connection) 
	{
		String RLT_DIF_STAT = "PENDING";
		String RLT_DIF_ACC = "A460";
		String RLT_PURP_TYP = "ERRORES";
		String DATA_SRC_APP = "ALTA_CPARTY";
		String MAIN_ENTITY_NME = "MNEM_LOCAL";
		String MESSAGE_RLT = "Contrato 460 pendiente de dar de alta";
		String main_entity_id;
		String rlt_field;

		for (String mnemLocal : listaMnemClientelaIdNoEncontrado)
		{
			main_entity_id = mnemLocal;
			rlt_field = mnemLocal;
			//i++;
			//if ((i % 99) == 0) {System.out.println("Traza insertRLT1ClientelaHilos - "+ (i+1) +" -" +noConcilia.get(i) + " - "+ msg); }

			//if (i % 10000 == 0)	{    System.out.println("Linea de insercion -"+i);	}
			String insertTableSQL =  "INSERT " 
					+"INTO " 
					+"  ft_t_rlt1 " 
					+"  ( " 
					+"    rlt_oid, " 
					+"    JOB_ID, " 
					+"    main_entity_nme, " 
					+"    main_entity_id, " 
					+"    rlt_dif_stat, " 
					+"    rlt_dif_acc, " 
					+"    message_rlt, " 
					+"    rlt_purp_typ, " 
					+"    data_src_app, " 
					+"    rlt_field, " 
					+"    start_tms, " 
					+"    last_chg_tms, " 
					+"    last_chg_usr_id " 
					+"  ) " 
					+"SELECT " 
					+"  new_oid, " 
					+"  '"+jobId+"', " 
					+"  '"+MAIN_ENTITY_NME+"', " 
					+"  '"+main_entity_id+"', " 
					+"  '"+RLT_DIF_STAT+"', " 
					+"  '"+RLT_DIF_ACC+"', " 
					+"  '"+MESSAGE_RLT+"', " 
					+"  '"+RLT_PURP_TYP+"', " 
					+"  '"+DATA_SRC_APP+"', " 
					+"  '"+rlt_field+"', " 
					+"  systimestamp, " 
					+"  systimestamp, " 
					+"  'BBVA:CUSTOMER' " 
					+"FROM " 
					+"  dual " 
					+"WHERE " 
					+"  NOT EXISTS " 
					+"  ( " 
					+"    SELECT " 
					+"      1 " 
					+"    FROM " 
					+"      ft_t_rlt1 " 
					+"    WHERE " 
					+"      main_entity_id = '"+main_entity_id+"' " 
					+"    AND message_rlt  = '"+MESSAGE_RLT+"' " 
					+"    AND rlt_dif_acc  = '"+RLT_DIF_ACC+"' " 
					+"    AND rlt_field    = '"+rlt_field+"' " 
					+"    AND rlt_dif_stat = '"+RLT_DIF_STAT+"' " 
					+"  )";

			//System.out.println("El insert: " + insertTableSQL);
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement .executeUpdate();

				preparedStatement.close();

			} catch (SQLException e) {	e.printStackTrace();	}

		}
		/*try 	{
		connection.close();
	} catch (SQLException e) {	e.printStackTrace();	}*/

	}





	public static ArrayList<String> getInsercionesRLT1_Proceso()
	{
		return insercionesRLT1_Proceso;
	}

	public static void setInsercionesRLT1_Proceso(ArrayList<String> insercionesRLT1_Proceso)
	{
		ConDB.insercionesRLT1_Proceso = insercionesRLT1_Proceso;
	}

	public static ArrayList<String> getInsercionesRLT1_Reportes()
	{
		return insercionesRLT1_Reportes;
	}

	public static void setInsercionesRLT1_Reportes(ArrayList<String> insercionesRLT1_Reportes)
	{
		ConDB.insercionesRLT1_Reportes = insercionesRLT1_Reportes;
	}

	public static ArrayList<String> getUpdatesFAB1()
	{
		return updatesFAB1;
	}

	public static void setUpdatesFAB1(ArrayList<String> updatesFAB1)
	{
		ConDB.updatesFAB1 = updatesFAB1;
	}

	/**
	 * Versión: 22/01/2018
	 * Ejecuta la query obtenida por parámetro. 
	 * Los tipos de query son:
	 *      Inserción en RLT1
	 *
	 * @param connection
	 * @param queryEjecutada
	 */
	public synchronized static void ejecutarQuery (Connection connection, String queryEjecutada)
	{
		try
		{
			Statement stmnt = connection.createStatement();
			stmnt.executeUpdate(queryEjecutada);

			stmnt.close();

			if (queryEjecutada.contains("INSERT"))
			{
				contadorInsercionesRLT1++;
			}
			else if (queryEjecutada.contains("UPDATE"))
			{
				contadorUpdatesFAB1++;
			}

		}
		catch (SQLException e)
		{
			System.out.println(queryEjecutada);
			e.printStackTrace();
		}
	}

	public static int getContadorInsercionesRLT1()
	{
		return contadorInsercionesRLT1;
	}

	public static void setContadorInsercionesRLT1(int contadorInsercionesRLT1)
	{
		ConDB.contadorInsercionesRLT1 = contadorInsercionesRLT1;
	}

	public static int getContadorUpdatesFAB1()
	{
		return contadorUpdatesFAB1;
	}

	public static void setContadorUpdatesFAB1(int contadorUpdatesFAB1)
	{
		ConDB.contadorUpdatesFAB1 = contadorUpdatesFAB1;
	}

	public static int getContadorPL()
	{
		return contadorPL;
	}

	public static void setContadorPL(int contadorPL)
	{
		ConDB.contadorPL = contadorPL;
	}



	public String obtenerLEIactual(String VCH_DBC_COD_CCLIEN, Connection connection){
		String LEIactual ="";
		try	{
			//Connection connection = this.ObtenerConexion();
			String query = "select lei.FINS_ID as LEIactual from ft_t_fiid cli, ft_t_fiid lei, ft_t_firl firl, ft_t_fiid glo, ft_t_fins fins "
					+ "		where firl.inst_mnem = cli.inst_mnem "
					+ "		and firl.rel_typ = 'LOCAL' "
					+ "		and firl.data_stat_typ = 'ACTIVE' "
					+ "		and cli.fins_id_ctxt_typ='CLIENTELAID'  "
					+ "		and cli.data_stat_typ='ACTIVE' "
					+ "		and lei.inst_mnem = firl.prnt_inst_mnem"
					+ "		and lei.fins_id_ctxt_typ='LEIID'  "
					+ "		and lei.data_stat_typ='ACTIVE' "
					+ "		and glo.inst_mnem = firl.prnt_inst_mnem "
					+ "		and glo.fins_id_ctxt_typ='FINSID' "
					+ "		and glo.data_stat_typ='ACTIVE' "
					+ "		and fins.inst_mnem = firl.prnt_inst_mnem "
					+ "		and fins.data_stat_typ = 'ACTIVE' "
					+ "		and cli.FINS_ID='"+VCH_DBC_COD_CCLIEN+"'";
			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query);

			while(rs.next()) {
				LEIactual = rs.getString("LEIactual");
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return LEIactual;		
	}

	public int obtenerMA(String VCH_DBC_COD_CCLIEN, Connection connection){
		int auxCont=0;
		try	{
			//Connection connection = this.ObtenerConexion();
			String query1 = " select count(frrl.PRNT_INST_MNEM) as cuenta from ft_t_frrl frrl, ft_t_finr finr where "
					+ " frrl.INST_MNEM in ( select INST_MNEM from ft_t_firl where "
					+ " prnt_inst_mnem in (select inst_mnem from ft_t_fiid WHERE FINS_ID_CTXT_TYP = 'CLIENTELAID' and FINS_ID =q'#"+VCH_DBC_COD_CCLIEN+"#' and DATA_STAT_TYP = 'ACTIVE'  )"
					+ " and rel_typ='OPERATIVE' and DATA_STAT_TYP = 'ACTIVE' ) and frrl.PRNT_FINSRL_TYP='MANDTACC' and frrl.DATA_STAT_TYP='ACTIVE' "
					+ " and finr.inst_mnem = frrl.PRNT_INST_MNEM and finr.FINSRL_TYP ='MANDTACC' and finr.data_stat_typ = 'ACTIVE'"; // INCLUIR ROL ACTIVO
			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query1);

			while(rs.next()) {
				auxCont = rs.getInt("cuenta");
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return auxCont;		
	}


	public String obtenerCANONICO(String CCLIEN, Connection connection){
		String CANONICO = "";
		try	{
			//Connection connection = this.ObtenerConexion();
			String query1 = "select glo.fins_id canonico"
					+ "	from ft_t_fiid cli, ft_t_fiid lei, ft_t_firl firl, ft_t_fiid glo, ft_t_fins fins "
					+ "	where firl.inst_mnem = cli.inst_mnem "
					+ "	and firl.rel_typ = 'LOCAL' "
					+ "	and firl.data_stat_typ = 'ACTIVE' "
					+ "	and cli.fins_id_ctxt_typ='CLIENTELAID'  "
					+ "	and cli.data_stat_typ='ACTIVE' "
					+ "	and lei.inst_mnem = firl.prnt_inst_mnem "
					+ "	and lei.fins_id_ctxt_typ='LEIID'  "
					+ "	and lei.data_stat_typ='ACTIVE' "
					+ "	and glo.inst_mnem = firl.prnt_inst_mnem "
					+ "	and glo.fins_id_ctxt_typ='FINSID' "
					+ "	and glo.data_stat_typ='ACTIVE' "
					+ "	and fins.inst_mnem = firl.prnt_inst_mnem "
					+ "	and fins.data_stat_typ = 'ACTIVE' "
					+ "	and cli.fins_id = '"+CCLIEN+"' "; 

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query1);

			while(rs.next()) {
				CANONICO = rs.getString("canonico");
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return CANONICO;		
	}

	public void reportarMA(ArrayList<String> lista,  String jobId, Connection connection) 	{


		for (String dato : lista)	{
			String datos [] =  dato.split("\\|");
			String CCLIEN = datos[0];
			String LEInuevo = "N/A";
			String LEIactual = "N/A";
			if(datos.length>1 && datos[1].trim().length()>0){
				LEInuevo = datos[1];
			}
			if(datos.length>2 && datos[2].trim().length()>0){
				LEIactual = datos[2];
			}

			//NVL(MAIN_ENTITY_ID,'N/A') Clientela_ID,
			//NVL(MESSAGE_RLT,'N/A') Mensaje,
			//NVL(SRC_VALUE,'N/A') Valor_Clientela,
			//NVL(GS_VALUE,'N/A') Valor_GS 

			String canonico = obtenerCANONICO( CCLIEN,  connection);

			/*String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
					+ " RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
					+ " SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
					+ " START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT, RLT_DIF_ACC) "
					+ " values "
					+ "(new_oid,'" + jobId + "',null,null,1,"
					+ " 'El cliente "+CCLIEN+" de la Ctpda "+canonico+" no se ha actualizado debido a que el LEI es distinto y tiene cuentas gestionadas asociadas',null,"
					+ " 'REPORTES','CLIENTELA','LEI no actualizado','" + LEInuevo +"',"
					+ " 'LEI no actualizado','" + LEIactual +"','LEI no actualizado','" 
					+ CCLIEN + "',sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";*/
			
			String insertTableSQL = "INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,"
			+ " RLT_STATUS,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,"
			+ " SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,"
			+ " START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT, RLT_DIF_ACC) "
			+ " values "
			+ " (new_oid,?,null,null,1,"
			+ " ?,null,"
			+ " 'REPORTES','CLIENTELA','LEI no actualizado',?,"
			+ " 'LEI no actualizado',?,'LEI no actualizado',?,sysdate,null,sysdate,'BBVA:CUSTOMER','NO',null)";

			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(insertTableSQL);
				preparedStatement.setString(1, jobId);
				preparedStatement.setString(2, "El cliente "+CCLIEN+" de la Ctpda "+canonico+" no se ha actualizado debido a que el LEI es distinto y tiene cuentas gestionadas asociadas");
				preparedStatement.setString(3, LEInuevo);
				preparedStatement.setString(4, LEIactual);
				preparedStatement.setString(5, CCLIEN);
				preparedStatement .executeUpdate();
				preparedStatement.close();
			} catch (SQLException e) {	e.printStackTrace();	}
		}
	}

	public String obtenerMNEM(String CCLIEN, Connection connection){
		String MNEM = "";
		try	{
			//Connection connection = this.ObtenerConexion();
			String query1 = "SELECT  INST_MNEM MNEM" 
					+" FROM FT_T_FIID " 
					+" WHERE FINS_ID_CTXT_TYP = 'CLIENTELAID' " 
					+" AND DATA_STAT_TYP  <> 'INACTIVE' " 
					+" AND FINS_ID  = '"+CCLIEN+"'"; 

			Statement stmnt;	stmnt = connection.createStatement();
			ResultSet rs = null;	rs = stmnt.executeQuery(query1);

			while(rs.next()) {
				MNEM = rs.getString("MNEM");
			}
			rs.close(); stmnt.close();		//connection.close();

		} catch (SQLException e) {	e.printStackTrace();	}
		return MNEM;		
	}


	public void publicarMA(ArrayList<String> lista,  String jobId, Connection connection) 	{
		String RLT_DIF_STAT = "PENDING";
		String RLT_DIF_ACC = "M";
		String RLT_PURP_TYP = "INFO";
		String DATA_SRC_APP = "MNEM";
		String MAIN_ENTITY_NME = "MNEMONICO_OP";
		String MESSAGE_RLT = " ";
		String main_entity_id;
		String rlt_field;

		for (String dato : lista)	{
			String MNEM = obtenerMNEM( dato,  connection);

			try {
				/*String query1 = "select frrl.PRNT_INST_MNEM MNEM from ft_t_frrl frrl, ft_t_finr finr where "
						+ " frrl.INST_MNEM in (select INST_MNEM from fT_t_firl where prnt_inst_mnem = q'#"+MNEM+"#' "
						+ " and rel_typ='OPERATIVE' and DATA_STAT_TYP='ACTIVE') and frrl.PRNT_FINSRL_TYP='MANDTACC' and frrl.DATA_STAT_TYP='ACTIVE' "
						+ " and finr.inst_mnem = frrl.PRNT_INST_MNEM and finr.FINSRL_TYP ='MANDTACC' and finr.data_stat_typ = 'ACTIVE'";*/
				String query1 = "select frrl.PRNT_INST_MNEM MNEM from ft_t_frrl frrl, ft_t_finr finr where "
						+ " frrl.INST_MNEM in (select INST_MNEM from fT_t_firl where prnt_inst_mnem = ? "
						+ " and rel_typ='OPERATIVE' and DATA_STAT_TYP='ACTIVE') and frrl.PRNT_FINSRL_TYP='MANDTACC' and frrl.DATA_STAT_TYP='ACTIVE' "
						+ " and finr.inst_mnem = frrl.PRNT_INST_MNEM and finr.FINSRL_TYP ='MANDTACC' and finr.data_stat_typ = 'ACTIVE'";
				PreparedStatement stmnt;
				stmnt = connection.prepareStatement(query1);
				stmnt.setString(1, MNEM);
				ResultSet rs = null;	rs = stmnt.executeQuery();
				while(rs.next()) {
					String MNEM_aux = rs.getString("MNEM");
					main_entity_id = MNEM_aux;
					rlt_field = MNEM_aux;
					/*String insertTableSQL =  "INSERT " 
							+"INTO " 
							+"  ft_t_rlt1 " 
							+"  ( " 
							+"    rlt_oid, " 
							+"    JOB_ID, " 
							+"    main_entity_nme, " 
							+"    main_entity_id, " 
							+"    rlt_dif_stat, " 
							+"    rlt_dif_acc, " 
							+"    message_rlt, " 
							+"    rlt_purp_typ, " 
							+"    data_src_app, " 
							+"    rlt_field, " 
							+"    start_tms, " 
							+"    last_chg_tms, " 
							+"    last_chg_usr_id " 
							+"  ) " 
							+"SELECT " 
							+"  new_oid, " 
							+"  '"+jobId+"', " 
							+"  '"+MAIN_ENTITY_NME+"', " 
							+"  '"+main_entity_id+"', " 
							+"  '"+RLT_DIF_STAT+"', " 
							+"  '"+RLT_DIF_ACC+"', " 
							+"  '"+MESSAGE_RLT+"', " 
							+"  '"+RLT_PURP_TYP+"', " 
							+"  '"+DATA_SRC_APP+"', " 
							+"  '"+rlt_field+"', " 
							+"  systimestamp, " 
							+"  systimestamp, " 
							+"  'BBVA:CUSTOMER' " 
							+"FROM " 
							+"  dual " 
							+"WHERE " 
							+"  NOT EXISTS " 
							+"  ( " 
							+"    SELECT " 
							+"      1 " 
							+"    FROM " 
							+"      ft_t_rlt1 " 
							+"    WHERE " 
							+"      main_entity_id = '"+main_entity_id+"' " 
							+"    AND message_rlt  = '"+MESSAGE_RLT+"' " 
							+"    AND rlt_dif_acc  = '"+RLT_DIF_ACC+"' " 
							+"    AND rlt_field    = '"+rlt_field+"' " 
							+"    AND rlt_dif_stat = '"+RLT_DIF_STAT+"' " 
							+"  )";*/
					
					String insertTableSQL =  "INSERT " 
							+"INTO " 
							+"  ft_t_rlt1 " 
							+"  ( " 
							+"    rlt_oid, " 
							+"    JOB_ID, " 
							+"    main_entity_nme, " 
							+"    main_entity_id, " 
							+"    rlt_dif_stat, " 
							+"    rlt_dif_acc, " 
							+"    message_rlt, " 
							+"    rlt_purp_typ, " 
							+"    data_src_app, " 
							+"    rlt_field, " 
							+"    start_tms, " 
							+"    last_chg_tms, " 
							+"    last_chg_usr_id " 
							+"  ) " 
							+"SELECT " 
							+"  new_oid, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  ?, " 
							+"  systimestamp, " 
							+"  systimestamp, " 
							+"  'BBVA:CUSTOMER' " 
							+"FROM " 
							+"  dual " 
							+"WHERE " 
							+"  NOT EXISTS " 
							+"  ( " 
							+"    SELECT " 
							+"      1 " 
							+"    FROM " 
							+"      ft_t_rlt1 " 
							+"    WHERE " 
							+"      main_entity_id = ? " 
							+"    AND message_rlt  = ? " 
							+"    AND rlt_dif_acc  = ? " 
							+"    AND rlt_field    = ? " 
							+"    AND rlt_dif_stat = ? " 
							+"  )";

					PreparedStatement preparedStatement;
					try {
						System.out.println(insertTableSQL);
						preparedStatement = connection.prepareStatement(insertTableSQL);
						preparedStatement.setString(1, jobId);
						preparedStatement.setString(2, MAIN_ENTITY_NME);
						preparedStatement.setString(3, main_entity_id);
						preparedStatement.setString(4, RLT_DIF_STAT);
						preparedStatement.setString(5, RLT_DIF_ACC);
						preparedStatement.setString(6, MESSAGE_RLT);
						preparedStatement.setString(7, RLT_PURP_TYP);
						preparedStatement.setString(8, DATA_SRC_APP);
						preparedStatement.setString(9, rlt_field);
						preparedStatement.setString(10, main_entity_id);
						preparedStatement.setString(11, MESSAGE_RLT);
						preparedStatement.setString(12, RLT_DIF_ACC);
						preparedStatement.setString(13, rlt_field);
						preparedStatement.setString(14, RLT_DIF_STAT);
						preparedStatement .executeUpdate();
						//preparedStatement .executeUpdate();
						preparedStatement.close();
					} catch (SQLException e) {	e.printStackTrace();	}
				}
				rs.close(); stmnt.close();		//connection.close();

			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
} 
