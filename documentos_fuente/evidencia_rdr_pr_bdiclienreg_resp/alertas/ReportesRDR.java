package report;


import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import jdbc.ConDB;
import jdbc.QuerysStr;
import main.Ppal;

public class ReportesRDR {
	
	public static final Logger LOGGER = Logger.getLogger(Ppal.class);
	private Connection conexion;
	private Vector<ReporteRDR>reportes;
	private int totalMensajes=0;
	private int totalFicheros=0;
	
	public ReportesRDR(Connection conexion){
		this.conexion=conexion;
		reportes=new Vector<ReporteRDR>();
	}
	
	public void extraerReportes(){
		
		String msg="Extrayendo reportes....";
		LOGGER.info(msg);
		System.out.println(msg);
		
		ResultSet rs_rep1=null;
		try{
			
		    String query = "";
		    if(Ppal.ProcesosExtraer.equals("PROCESOS")){
		    	query = QuerysStr.query_REP1();
		    }else{
		    	query = QuerysStr.query_REP1_Filtrado(Ppal.ProcesosExtraer);
		    }
		    Statement stmnt = conexion.createStatement();
		    rs_rep1 = stmnt.executeQuery(query);
	
		    while (rs_rep1.next()) {
		    	
		    	String rep1_oid=rs_rep1.getString("REP1_OID"); //ID reporte
				String Tipo = rs_rep1.getString("TIPO");       
				String Proceso = rs_rep1.getString("PROCESO");	
				String Descripcion = rs_rep1.getString("DESCRIPCION");	
				String cabecera=rs_rep1.getString("CABECERA");
		
				// Tratamos la ruta
				String directory = rs_rep1.getString("RUTA");
				Pattern pattern = Pattern.compile("\\$ENV");
				Matcher matcher = pattern.matcher(directory);
				String Ruta = matcher.replaceAll(ConDB.env);
				
				//Tratamos el template 
				String Template = rs_rep1.getString("EXCEL_TEMPLATE");
				if(null==Template){
					Template = Ruta+"/Templates/Template_Alertas_Excel.xlsx";
				}else{
					pattern = Pattern.compile("\\$ENV");
					matcher = pattern.matcher(Template);
					Template = matcher.replaceAll(ConDB.env);
				}
				String sheet = rs_rep1.getString("EXCEL_SHEET");
				if(null==sheet){
					sheet="Reporte";
				}
				
				//TRATAMIENTO LOCAL
				if(ConDB.getUbicacionJar().equals("LOCAL")){
					Ruta="C:"+Ruta;
					Template = "C:"+Template;
				}
				//TRATAMIENTO LOCAL
				//System.out.println("La plantilla del reporte "+Proceso+" es '"+Template+"'");
		
				String Short_Process = rs_rep1.getString("SHORT_PROCESS");
				String queryRep1 = clobToString(rs_rep1.getClob("QUERY"));	
				int cuentaemails = rs_rep1.getInt("CUENTA_MAILS");
				boolean hayemails = (cuentaemails>0)?true:false;
				
				ReporteRDR reporte=new ReporteRDR(conexion, rep1_oid, Proceso, Tipo, Descripcion, queryRep1, Ruta, Template, sheet, Short_Process, cabecera, hayemails);
				reportes.add(reporte);	
				msg="ReportesRDR::extraerReportes::Añadido el proceso "+Proceso;
				LOGGER.info(msg);
				System.out.println(msg);
		    }
		    rs_rep1.close();
		    stmnt.close();
		    msg="ReportesRDR::extraerReportes::Se extrayeron "+reportes.size()+" reportes con usuarios activos.";
		    LOGGER.info(msg);
		    System.out.println(msg);
		    
		}catch (SQLException e){
			msg="ReportesRDR::extraerReportes::ERROR::"+e.toString();
		    LOGGER.error(msg);
		    System.out.println(msg);
		}
	}
	
	public void descargaMensajesResportes(){
		String msg="";
		try {
			totalMensajes=0;
			for(int i=0;i<reportes.size();i++){
				reportes.get(i).descargaMensajesReporte();
				totalMensajes+=reportes.get(i).getTotalMensajes();
			}
			for(int i=0;i<reportes.size();i++){
				reportes.get(i).descargaTiposEnvio();
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReportesRDR::recuperaMensajesReportes::ERROR::No se pudieron recuperar los mensajes.";
			LOGGER.error(msg);
		    System.out.println(msg);
		    msg=e.toString();
		    LOGGER.error(msg);
		    System.out.println(msg);
		}
	}
	
	public void generaDocumentos(){
		String msg="";
		boolean res=true;
		try {
			for(int i=0;i<reportes.size();i++){
				boolean resaux=reportes.get(i).generaDocumentos();
				if(resaux==false){
					res=false;
				}
				totalFicheros+=reportes.get(i).getFicherosGenerados();
			}
			if(res){
				msg="ReportesRDR::generaDocumentos::Se crearon todos los reportes con exito.";
				LOGGER.error(msg);
				System.out.println(msg);
			}else{
				msg="ReportesRDR::generaDocumentos::Existen reportes para los que no se ha podido generar ningun documento.";
				LOGGER.error(msg);
				System.out.println(msg);
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReportesRDR::generaDocumentos::ERROR::Se produjo un error al crear los documentos.";
			LOGGER.error(msg);
			System.out.println(msg);
			msg=e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
		}
		
	}
	
	public void marcaALG1_Reportes(){
		try {
			for(int i=0;i<reportes.size();i++){
				reportes.get(i).marcaUsadosALG();
			}
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void marcaReportesPending(){
		
		try {
			Statement st=conexion.createStatement();
			for(int i=0;i<reportes.size();i++){
				reportes.get(i).marcaReportePendiente(st);
			}
			st.close();
		}catch (Exception e) {
			// TODO: handle exception
			String msg="ReportesRDR::marcaReportesPending::ERROR::"+e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
		}
	}

	public void cerrarConexiones(){
		try {
			for(int i=0;i<reportes.size();i++){
				reportes.get(i).cierraConexion();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			conexion.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public int cuentaReportes(){
		return reportes.size();
	}

	public int getTotalMensajes() {
		return totalMensajes;
	}

	public int getTotalFicheros() {
		return totalFicheros;
	}
	
	private String clobToString(Clob data) {
	    StringBuilder sb = new StringBuilder();
	    try {
	        Reader reader = data.getCharacterStream();
	        BufferedReader br = new BufferedReader(reader);

	        String line;
	        while(null != (line = br.readLine())) {
	            sb.append(line+"\n");
	        }
	        br.close();
	    } catch (Exception e) {
	        // handle this exception
	    	String msg="ReportesRDR::clobToString::ERROR::Fallo al extraer el clob. \n"+e.toString();
	    	LOGGER.error(msg);
	    	System.out.println(msg);
	    }
	    return sb.toString();
	}
	
}
