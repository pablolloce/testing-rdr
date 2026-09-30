import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.jdbc.pool.OracleDataSource;

public class ConDB {
    public static String url;		
    public static String user;	
    public static String password;		
	public static String env = "";
    public static OracleDataSource obj_ods=null;			
    public static int cont_conexion=0;
    
    public void ObtenerCredenciales() {

    	String pathpro = "/pr/kytl/online/multipais/multicanal/cfg/entorno/";
    	String pathpre = "/pp/kytl/online/multipais/multicanal/cfg/entorno/";
    	String pathint = "/ei/kytl/online/multipais/multicanal/cfg/entorno/";
    	String pathdev = "/de/kytl/online/multipais/multicanal/cfg/entorno/";
    	String path = "";
    	File folderpro = new File(pathpro);
    	File folderpre = new File(pathpre);
    	File folderint = new File(pathint);
    	File folderdev = new File(pathdev);
	
		if (folderpro.exists()){
			path = pathpro + "credentials.xml";
			env = "pr";
		}else if (folderpre.exists()){
			path = pathpre + "credentials.xml";
			env = "pp";
		}else if (folderint.exists()){
			path = pathint + "credentials.xml";
			env = "ei";
		}else if (folderdev.exists()){
			path = pathdev + "credentials.xml";
			env = "de";
		}
		
		File archivo = null;		
		FileReader fr = null;		
		BufferedReader br = null;	
		archivo = new File(path);
	
		try {
		    fr = new FileReader(archivo);	
		    br = new BufferedReader(fr);
		    String linea;	String fich = "";
		    try {
				while ((linea = br.readLine()) != null){    
					fich = fich + linea;	
				}
				fr.close();
		    } catch (IOException e) {e.printStackTrace();	}
	
		    String host=null;
			String host2=null;	   			
		    String port=null;	
		    String sid = null;	
		    String userdb=null;	
		    String passdb=null;
	
		    Pattern patronAux = Pattern.compile("<database>(.*?)</database>");
		    Matcher matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	fich = matchAux.group(1);    	
		    }
	
		    patronAux = Pattern.compile("<sid>(.*?)</sid>");	    
		    matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	sid = matchAux.group(1);	
		    }
	
		    patronAux = Pattern.compile("<host>(.*?)</host>");	    
		    matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	host = matchAux.group(1);	
		    }
			
			patronAux = Pattern.compile("<host2>(.*?)</host2>");	    
			matchAux = patronAux.matcher(fich);
				
		    if (matchAux.find())
		    {	
				host2 = matchAux.group(1);	
		    }
			
		    patronAux = Pattern.compile("<port>(.*?)</port>");	    
		    matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	port = matchAux.group(1);	
		    }
	
		    patronAux = Pattern.compile("<gcuser>(.*?)</gcuser>");  
		    matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	userdb = matchAux.group(1);	
		    }
	
		    patronAux = Pattern.compile("<gcpass>(.*?)</gcpass>");  
		    matchAux = patronAux.matcher(fich);
		    if (matchAux.find()) {	
		    	passdb = matchAux.group(1);	
		    }
	
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
		    ConDB.url = urldb;		
		    ConDB.user = userdb;	
		    ConDB.password = passdb;
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
		    obj_ods = new OracleDataSource();	    
		    obj_ods.setURL(ConDB.url);	    
		    obj_ods.setUser(ConDB.user);    
		    obj_ods.setPassword(ConDB.password);   
		    obj_con=obj_ods.getConnection();
		    System.out.println("Conexion realizada numero "+ ConDB.cont_conexion);	
    	} catch (SQLException e) {    
    		System.out.println("No se ha podido realizar la conexion.");	    
    		e.printStackTrace();	
    	}
    	return obj_con;
    }

	public ArrayList<Object[]> getBrokerNotBDI(Connection connection) {
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		try	{
			PreparedStatement pstmt = connection.prepareStatement (
				"select "
			  + "(select fins.inst_nme "
			  + "from ft_t_fins fins "
			  + "where fins.inst_mnem = rlt12.rlt_field "
			  + "and fins.data_stat_typ = 'ACTIVE') instnme, "			  		
			  + "(select fiid.fins_id "
			  + "from ft_t_fiid fiid "
			  + "where fiid.inst_mnem = rlt12.rlt_field "
			  + "and fiid.fins_id_ctxt_typ = 'FINSID' "
			  + "and fiid.data_stat_typ = 'ACTIVE') finsid, "
			  + "(select fiid.fins_id "
			  + "from ft_t_fiid fiid "
			  + "where fiid.inst_mnem = rlt12.rlt_field "
			  + "and fiid.fins_id_ctxt_typ = 'MGCGLOID' "
			  + "and fiid.data_stat_typ = 'ACTIVE') mgcid, "
			  + "(select fiid.fins_id "
			  + "from ft_t_fiid fiid "
			  + "where fiid.inst_mnem = rlt12.rlt_field "
			  + "and fiid.fins_id_ctxt_typ = 'BDIID' "
			  + "and fiid.data_stat_typ = 'ACTIVE') bdiid, "
			  + "rlt12.gs_value rdr_val "
			  + "from ft_t_rlt1 rlt12 "
			  + "where rlt12.message_rlt = 'El Broker Identifier es nulo en BDI.' "
			  + "and rlt12.rlt_purp_typ = 'REPORTES' "
			  + "and rlt12.data_src_app = 'BDI' "
			  + "and rlt12.gs_field = 'BROKER CODE_RDR' "
			  + "and rlt12.main_entity_nme = 'FT_T_DLER' "
			  + "and rlt12.job_id = "
			  + "(select rlt1.job_id "
			  + "from "
			  + "(select rlt11.job_id "
			  + "from ft_t_rlt1 rlt11 "
			  + "where rlt11.rlt_purp_typ = 'REPORTES' "
			  + "and rlt11.data_src_app = 'BDI' "
			  + "order by rlt11.last_chg_tms desc) rlt1 "
			  + "where rownum = 1) ");
			
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				list.add(new String[]{rs.getString("instnme"), 
									  rs.getString("finsid"), 
									  rs.getString("mgcid"), 
									  rs.getString("bdiid"), 
									  rs.getString("rdr_val")});			
			}
			
			pstmt.close();
			
		} catch (SQLException e) {		
			e.printStackTrace();	
		}
		return list;		
	}

	public ArrayList<Object[]> getBrokerNotRDR(Connection connection) {
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		try	{
			PreparedStatement pstmt = connection.prepareStatement (
					"select "
				  + "(select fins.inst_nme "
				  + "from ft_t_fins fins "
				  + "where fins.inst_mnem = rlt12.rlt_field "
				  + "and fins.data_stat_typ = 'ACTIVE') instnme, "			  		
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'FINSID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') finsid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'MGCGLOID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') mgcid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'BDIID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') bdiid, "
				  + "rlt12.src_value bdi_val "
				  + "from ft_t_rlt1 rlt12 "
				  + "where rlt12.message_rlt = 'El Broker Identifier no existe en RDR, se inserta' "
				  + "and rlt12.rlt_purp_typ = 'REPORTES' "
				  + "and rlt12.data_src_app = 'BDI' "
				  + "and rlt12.src_field = 'BROKER CODE_BDI' "
				  + "and rlt12.main_entity_nme = 'FT_T_DLER' "
				  + "and rlt12.job_id = "
				  + "(select rlt1.job_id "
				  + "from "
				  + "(select rlt11.job_id "
				  + "from ft_t_rlt1 rlt11 "
				  + "where rlt11.rlt_purp_typ = 'REPORTES' "
				  + "and rlt11.data_src_app = 'BDI' "
				  + "order by rlt11.last_chg_tms desc) rlt1 "
				  + "where rownum = 1) ");
			
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				list.add(new String[]{rs.getString("instnme"), 
									  rs.getString("finsid"), 
									  rs.getString("mgcid"), 
									  rs.getString("bdiid"), 
									  rs.getString("bdi_val")});				
			}
			
			pstmt.close();
			
		} catch (SQLException e) {		
			e.printStackTrace();	
		}
		return list;		
	}
	
	public ArrayList<Object[]> getBrokerBDIRDR(Connection connection) {
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		try	{
			PreparedStatement pstmt = connection.prepareStatement (
					"select "
				  + "(select fins.inst_nme "
				  + "from ft_t_fins fins "
				  + "where fins.inst_mnem = rlt12.rlt_field "
				  + "and fins.data_stat_typ = 'ACTIVE') instnme, "			  		
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'FINSID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') finsid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'MGCGLOID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') mgcid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'BDIID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') bdiid, "
				  + "rlt12.src_value bdi_val, rlt12.gs_value rdr_val "
				  + "from ft_t_rlt1 rlt12 "
				  + "where rlt12.message_rlt = 'El Broker Identifier no coincide' "
				  + "and rlt12.rlt_purp_typ = 'REPORTES' "
				  + "and rlt12.data_src_app = 'BDI' "
				  + "and rlt12.src_field = 'BROKER CODE_BDI' "
				  + "and rlt12.gs_field = 'BROKER CODE_RDR' "
				  + "and rlt12.main_entity_nme = 'FT_T_DLER' "
				  + "and rlt12.job_id = "
				  + "(select rlt1.job_id "
				  + "from "
				  + "(select rlt11.job_id "
				  + "from ft_t_rlt1 rlt11 "
				  + "where rlt11.rlt_purp_typ = 'REPORTES' "
				  + "and rlt11.data_src_app = 'BDI' "
				  + "order by rlt11.last_chg_tms desc) rlt1 "
				  + "where rownum = 1) ");					

			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				list.add(new String[]{rs.getString("instnme"), 
									  rs.getString("finsid"), 
									  rs.getString("mgcid"), 
									  rs.getString("bdiid"), 
									  rs.getString("bdi_val"), 
									  rs.getString("rdr_val")});				
			}
			
			pstmt.close();
			
		} catch (SQLException e) {		
			e.printStackTrace();	
		}
		return list;		
	}	
	
	public ArrayList<Object[]> getBrokerName(Connection connection) {
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		try	{
			PreparedStatement pstmt = connection.prepareStatement (
					"select "
				  + "(select fins.inst_nme "
				  + "from ft_t_fins fins "
				  + "where fins.inst_mnem = rlt12.rlt_field "
				  + "and fins.data_stat_typ = 'ACTIVE') instnme, "			  		
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'FINSID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') finsid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'MGCGLOID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') mgcid, "
				  + "(select fiid.fins_id "
				  + "from ft_t_fiid fiid "
				  + "where fiid.inst_mnem = rlt12.rlt_field "
				  + "and fiid.fins_id_ctxt_typ = 'BDIID' "
				  + "and fiid.data_stat_typ = 'ACTIVE') bdiid, "
				  + "rlt12.src_value bdi_val, rlt12.gs_value rdr_val "
				  + "from ft_t_rlt1 rlt12 "
				  + "where rlt12.message_rlt = 'El Broker Name no coincide' "
				  + "and rlt12.rlt_purp_typ = 'REPORTES' "
				  + "and rlt12.data_src_app = 'BDI' "
				  + "and rlt12.src_field = 'BROKER NAME_BDI' "
				  + "and rlt12.gs_field = 'BROKER NAME_RDR' "
				  + "and rlt12.main_entity_nme = 'FT_T_DLER' "
				  + "and rlt12.job_id = "
				  + "(select rlt1.job_id "
				  + "from "
				  + "(select rlt11.job_id "
				  + "from ft_t_rlt1 rlt11 "
				  + "where rlt11.rlt_purp_typ = 'REPORTES' "
				  + "and rlt11.data_src_app = 'BDI' "
				  + "order by rlt11.last_chg_tms desc) rlt1 "
				  + "where rownum = 1) ");					

			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				list.add(new String[]{rs.getString("instnme"), 
									  rs.getString("finsid"), 
									  rs.getString("mgcid"), 
									  rs.getString("bdiid"), 
									  rs.getString("bdi_val"), 
									  rs.getString("rdr_val")});				
			}
			
			pstmt.close();
			
		} catch (SQLException e) {		
			e.printStackTrace();	
		}
		return list;		
	}	
	
}
