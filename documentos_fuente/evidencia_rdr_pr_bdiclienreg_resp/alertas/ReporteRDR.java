package report;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import org.apache.log4j.Logger;
import main.Ppal;
import jdbc.QuerysStr;

public class ReporteRDR {
	
	public static final Logger LOGGER = Logger.getLogger(Ppal.class);
	private String rep1_oid;
	private String proceso;
	private String tipo;
	private String descripcion;
	private String query;
	private String ruta;
	private String excel_template;
	private String excel_sheet;
	private String short_process;
	private String cabecera;
	private boolean emailsActivos;
	private Connection conexion;

	private Vector<String>mensajes;
	private Vector<String>estadisticas;
	private Vector<String>celdasexcel;
	private Vector<String>alg1_oids;
	private Vector<String>tipos_envio;
	
	private int totalMensajes=0;
	private int ficherosGenerados=0;
	
	public ReporteRDR  (Connection conexion,
						String rep1_oid,   
						String proceso,    
						String tipo,        
						String descripcion, 
						String query,       
						String ruta,
						String excel_template,
						String excel_sheet,
						String short_process,
						String cabecera,
						boolean emailsActivos){
		
		this.conexion=conexion;
		this.rep1_oid=rep1_oid;
		this.proceso=proceso;
		this.tipo=tipo;
		this.descripcion=descripcion;
		this.query=query;
		this.ruta=ruta;
		this.excel_template=excel_template;
		this.excel_sheet=excel_sheet;
		this.short_process=short_process;
		this.cabecera=cabecera;
		this.emailsActivos = emailsActivos;
		totalMensajes=0;
		ficherosGenerados=0;
		if (this.short_process.contains("YYYYMMDD")){
			Date myDate = new Date();
			String fec=new SimpleDateFormat("yyyyMMdd").format(myDate);
			this.short_process = this.short_process.replace("YYYYMMDD", fec);
		}
		
	}
	
	public void descargaMensajesReporte(){
		String msg="";
		try {
			msg="ReporteRDR::descargaMensajesReporte::Descargando los mensajes del proceso "+proceso;
			LOGGER.info(msg);
			System.out.println(msg);
			mensajes=new Vector<String>();
			estadisticas=new Vector<String>();
			celdasexcel=new Vector<String>();
			alg1_oids=new Vector<String>();
			Statement stmt=conexion.createStatement();
			ResultSet rs=stmt.executeQuery(query);
			totalMensajes=0;
			while(rs.next()){
				String oid=rs.getString("ALG1_OID");
				String mensaje=rs.getString("MENSAJE");
				String tip_msg=rs.getString("TIPO");
				alg1_oids.add(oid);
				if(tip_msg.equals("MENSAJE")){
					mensajes.add(mensaje);
				}else if(tip_msg.equals("ESTADISTICA")){
					estadisticas.add(mensaje);
				}else if(tip_msg.equals("CELDAEXCEL")){
					celdasexcel.add(mensaje);
				}
				totalMensajes++;
			}
			rs.close();
			stmt.close();
			msg="ReporteRDR::descargaMensajesReporte::Del proceso "+proceso+" se descargaron "+totalMensajes+" mensajes ("
			   +mensajes.size()+" mensajes, "+celdasexcel.size()+" celdas de excel y "+estadisticas.size()+" estadisticas).";
			LOGGER.info(msg);
			System.out.println(msg);
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReporteRDR::descargaMensajesReporte::ERROR "+proceso+". No se pudieron descargar los mensajes";
			LOGGER.error(msg);
			System.out.println(msg);
			LOGGER.error(e.toString());
			System.out.println(e.toString());
		}
	}
	
	public void descargaTiposEnvio(){
		String msg="";
		try {
			msg="ReporteRDR::descargaTiposEnvio::Descargando tipos de envio del proceso "+proceso;
			tipos_envio=new Vector<String>();
			LOGGER.info(msg);
			System.out.println(msg);
			
			String query=QuerysStr.query_ALU1_TiposEnvio(proceso);
			Statement stmt=conexion.createStatement();
			ResultSet rs=stmt.executeQuery(query);
			while(rs.next()){
				String tipo=rs.getString("TIPO_ENVIO");
				tipos_envio.add(tipo);
				msg="ReporteRDR::descargaTiposEnvio::Se incluye el tipo de envio "+tipo+" para el proceso "+proceso;
				LOGGER.info(msg);
				System.out.println(msg);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReporteRDR::descargaTiposEnvio::ERROR "+proceso+". No se pudieron descargar los tipos de envio asociados al proceso.";
			LOGGER.error(msg);
			System.out.println(msg);
			LOGGER.error(e.toString());
			System.out.println(e.toString());
		}
	}
	
	public boolean generaDocumentos(){
		
		boolean res=true;
		String msg="";
		try {
			msg="ReporteRDR::generaDocumentos::Generando los documentos del proceso "+proceso+", del tipo "+tipo+" con los siguientes tipos de fichero:";
			System.out.println(msg);
			LOGGER.info(msg);
			
			for(int i=0;i<tipos_envio.size();i++){
				msg="ReporteRDR::generaDocumentos::    "+String.valueOf(i+1)+" - "+tipos_envio.get(i);
				System.out.println(msg);
				LOGGER.info(msg);
			}
			//Validaciones previas
			if(mensajes.size()>0 && celdasexcel.size()>0){
				msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" combina mensajes normales con celdas de Excel. No se puede dar esta combinación. No se generarán los documentos.";
				LOGGER.info(msg);
				System.out.println(msg);
				return true;
			}
			if(estadisticas.size()>0 && celdasexcel.size()>0){
				msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" combina estadisticas con celdas de Excel. No se puede dar esta combinación. No se generarán los documentos.";
				LOGGER.info(msg);
				System.out.println(msg);
				return true;
			}
			if(tipos_envio.size()==0){
				msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" no tiene ningún tipo de envío asociado. No se generarán documentos.";
				LOGGER.info(msg);
				System.out.println(msg);
				return true;
			}
			if(tipos_envio.size()==1){
				if(tipos_envio.get(0).equals("DAT") && mensajes.size()==0){
					msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" de Datio no tiene ningún mensaje. No se generará el documento.";
					LOGGER.info(msg);
					System.out.println(msg);
					return true;
				}
				if(tipo.equals("REPORTEEXCEL") && tipos_envio.get(0).equals("EXCEL") == false){
					msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" está definido como un reporte Excel pero los usuarios no estan suscritos a este medio. No se generará.";
					LOGGER.info(msg);
					System.out.println(msg);
					return true;
				}
			}
			if(tipos_envio.size()>1){
				if(tipo.equals("REPORTEEXCEL")){
					boolean existeExcel=false;
					for(int i=0;i<tipos_envio.size();i++){
						if(tipos_envio.get(i).equals("EXCEL")){
							existeExcel=true;
						}
					}
					if(existeExcel){
						msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" está definido como un reporte Excel y tiene más de un tipo de envío asociado. Sólo se generará el Excel.";
						LOGGER.info(msg);
						System.out.println(msg);
						for(int i=tipos_envio.size()-1;i>=0;i--){
							if(tipos_envio.get(i).equals("EXCEL")==false){
								msg="ReporteRDR::generaDocumentos::Eliminando el tipo de envio "+tipos_envio.get(i)+" para el proceso "+proceso;
								LOGGER.info(msg);
								System.out.println(msg);
								tipos_envio.remove(i);
							}
						}						
					}else{
						msg="ReporteRDR::generaDocumentos::ERROR VALIDACION::El proceso "+proceso+" está definido como un reporte Excel pero no tiene usuarios suscritos al reporte en Excel. No se generará el fichero.";
						LOGGER.info(msg);
						System.out.println(msg);
						return true;
					}
					
				}
			}
			//Fin validaciones
			
			
			for(int i=0;i<tipos_envio.size();i++){
				boolean resaux=generaDocumento(tipos_envio.get(i),
											   proceso, 
											   mensajes,
											   estadisticas,
											   celdasexcel,
											   descripcion,
											   short_process,
											   ruta,
											   excel_template,
											   excel_sheet,
											   cabecera,
											   emailsActivos);			
				if(resaux==false){
					msg="ReporteRDR::generaDocumentos::Fallo al crear el tipo envio "+tipos_envio.get(i)+" para el proceso "+proceso;
					LOGGER.error(msg);
					System.out.println(msg);
					res=false;
				}else{
					ficherosGenerados++;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReporteRDR::generaDocumentos::ERROR::Fallo al crear los documentos del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			msg=e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
			res=false;
		}
		msg="ReporteRDR::generaDocumentos::documentos generados para el proceso "+proceso;
		if(res){
			msg+=" correctamente.";
		}else{
			msg+=" con errores.";
		}
		LOGGER.error(msg);
		System.out.println(msg);
		return res;
		
	}
	
	private static boolean generaDocumento  (String tipo_env, 
										     String proc, 
										     Vector<String>mensajes, 
										     Vector<String>estats,
										     Vector<String>celdas,
										     String descrip,
										     String short_proc,
										     String ruta,
										     String excelTemplate,
										     String excelSheet,
										     String cabecera,
										     boolean emailsactivos){
		boolean res=false;
		String msg="";
		try {
			res=DocumentGenerator.generaDocumento(tipo_env, proc, mensajes, estats, celdas, descrip, short_proc, ruta, excelTemplate, excelSheet, cabecera, emailsactivos);
		} catch (Exception e) {
			// TODO: handle exception
			msg="ReporteRDR::generaReporte::ERROR::Fallo al generar el reporte del proceso "+proc;
			LOGGER.error(msg);
			System.out.println(msg);
		}
		
		return res;
		
	}
	public void marcaUsadosALG(){
		try {
			Statement st=conexion.createStatement();
			marcaUsadosALG1(st, alg1_oids);
			st.close();
		} catch (Exception e) {
			// TODO: handle exception
			String msg="ReporteRDR::marcaUsadosALG::ERROR::Fallo al marcar como utilizados los registros del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			msg=e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
		}
	}
	private static void marcaUsadosALG1(Statement st, Vector<String>oids){
    	try {
    		while(oids.size()>0){
        		Vector<String>oidsaux=new Vector<String>();
        		while(oidsaux.size()<990 && oids.size()>0){
        			oidsaux.add(oids.get(0));
        			oids.remove(0);
        		}
        		String q=QuerysStr.queryMarcadoALG1(oidsaux);
        		st.executeUpdate(q);
        	}
		} catch (Exception e) {
			String msg="ReporteRDR::marcaUsadosALG1::ERROR::Fallo al marcar los registros de la ALG1 como usados.";
			LOGGER.error(msg);
			System.out.println(msg);
		}
    }
	public void marcaReportePendiente(Statement st){
		try {
			String q=QuerysStr.query_REP1_MarcaPending(proceso);
			st.executeUpdate(q);
		} catch (Exception e) {
			// TODO: handle exception
			String msg="ReporteRDR::marcaReportePendiente::Error al marcar el reporte "+proceso+" a pending.";
			LOGGER.error(msg);
			System.out.println(msg);
		}
	}
	
	public void cierraConexion(){
		try {
			conexion.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public String getRep1_oid() {
		return rep1_oid;
	}

	public String getTipo() {
		return tipo;
	}

	public int getTotalMensajes() {
		return totalMensajes;
	}

	public int getFicherosGenerados() {
		return ficherosGenerados;
	}
	
	
	
}