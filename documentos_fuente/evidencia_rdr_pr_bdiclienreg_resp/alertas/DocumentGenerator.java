package report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import main.Ppal;;

public class DocumentGenerator {
	
	public static final Logger LOGGER = Logger.getLogger(Ppal.class);
	
	
	public static boolean generaDocumento(String tipo_doc,
							 			  String proceso, 
										  Vector<String>mensajes, 
										  Vector<String>estadisticas,
										  Vector<String>celdas,
										  String descripcion, 
										  String short_process, 
										  String ruta,
										  String excelTemplate,
										  String excelSheet,
										  String cabecera,
										  boolean emailsActivos){
		String msg="";		
		boolean res=false;
		
		if(mensajes.size()>0){
			if("EXCEL".equals(tipo_doc)){
				System.out.println("DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO EXCEL");
				res=generaExcel(proceso, mensajes, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
			}else if ("WORD".equals(tipo_doc)) {
				System.out.println("DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO WORD");
				res=generaWord (proceso, mensajes, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
			}else if ("CUERPO".equals(tipo_doc)){
				System.out.println("DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO CUERPO DE CORREO");
				res=generaBody (proceso, mensajes, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
			}else if ("TXT".equals(tipo_doc)){
				System.out.println("DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO TXT");
				res=generaTxt  (proceso, mensajes, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
			}else if ("DAT".equals(tipo_doc)){
				System.out.println("DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO DAT");
				res=generaTxt  (proceso, mensajes, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
				if(res){ //Generacion .ctl vacio
					String rutasalida=rutaSalida(ruta, "CTL", short_process, excelTemplate);
					System.out.println("DocummentGenerator::generaDocumento::generando fichero de control .ctl para Datio.");
					try {
						BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(rutasalida) ,"UTF-8"));
						bw.write("");
						bw.close();
					} catch (Exception e) {
						// TODO: handle exception
						msg="DocumentGenerator::generaDocumento::ERROR::Fallo al crear el .ctl.";
						LOGGER.error(msg);
						System.out.println(msg); 
						msg=e.toString();
						LOGGER.error(msg);
						System.out.println(msg); 
					}
				}//Fin generación .ctl
			}else{
				msg="DocumentGenerator::generaDocumento::ERROR::Tipo de documento '"+tipo_doc+"' desconocido.";
				LOGGER.error(msg);
				System.out.println(msg);  
				res=false;
			}
			
			if(("EXCEL".equals(tipo_doc) || "WORD".equals(tipo_doc) || "TXT".equals(tipo_doc) /*|| "DAT".equals(tipo_doc)*/)
					&& emailsActivos){
				generaCuerpoCorreo(tipo_doc,proceso, celdas, estadisticas, descripcion, short_process, ruta, excelTemplate, cabecera, 0);
			}
			
		}else if(celdas.size()>0){
			
			if("EXCEL".equals(tipo_doc)){
				msg="DocumentGenerator::generaDocumento::Proceso-"+proceso+"::GENERANDO EXCEL POR CELDAS";
				LOGGER.error(msg);
				System.out.println(msg);  
				int totFilas=generaExcelPorCeldas(proceso, celdas, estadisticas, descripcion, short_process, ruta, excelTemplate, excelSheet, cabecera, tipo_doc);
				if(totFilas<0){
					res=false;
				}else{
					res=true;
				}
				if(totFilas>0 && emailsActivos){
					generaCuerpoCorreo(tipo_doc,
							           proceso, 
							           celdas, 
							           estadisticas, 
							           descripcion, 
							           short_process, 
							           ruta, 
							           excelTemplate, 
							           cabecera, 
							           totFilas);
					
				}
			}
		}else if(emailsActivos==true){
			String rutasalida="";
			if ("CUERPO".equals(tipo_doc)){
				rutasalida=rutaSalida(ruta, tipo_doc, short_process, excelTemplate);
			}else{
				rutasalida=rutaCuerpoCorreo(ruta, tipo_doc, short_process);
			}
			try {
				msg="DocumentGenerator::generaDocumento::Generando body correo para reporte sin datos::"+proceso;
				LOGGER.error(msg);
				System.out.println(msg);
		        BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(rutasalida) ,"UTF-8"));
		        String msgInfo="Estimado usuario, \n\n"
		        			  +"No existen datos a enviar del proceso "+proceso+" al que está suscrito. \n\n"
		        			  +"Un saludo.";
		        bw.append(msgInfo);
		        bw.close();
		        res=true;
			} catch (Exception e) {
				// TODO: handle exception
				msg="DocumentGenerator::generaDocumento::Tipo de documento '"+tipo_doc+"' desconocido.";
				LOGGER.error(msg);
				System.out.println(msg);
				msg=e.toString();
				LOGGER.error(msg);
				System.out.println(msg);
				res=false;
			}
		}
		
		return res;
	}
	
	private static boolean generaWord(String proceso, 
									 Vector<String>mensajes, 
									 Vector<String>estadisticas,
									 String descripcion, 
									 String short_process, 
									 String ruta,
									 String excelTemplate,
									 String excelSheet,
									 String cabecera,
									 String tipodoc){
		String msg="";
		try {
			msg="DocumentGenerator::generaWord::generando word del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			
			if(mensajes.size()>0){
				String rutaplantilla=rutaPlantilla(ruta, tipodoc);
				String rutasalida=rutaSalida(ruta, tipodoc, short_process, excelTemplate);
				
				XWPFDocument document=null;
				FileInputStream f=new FileInputStream(rutaplantilla);
				document = new XWPFDocument(f);
				
				XWPFParagraph paragraph_titulo  = document.createParagraph();
				XWPFRun parraf_titulo = paragraph_titulo.createRun();
				
				parraf_titulo.setBold(true);
				paragraph_titulo.setAlignment(ParagraphAlignment.CENTER);
				parraf_titulo.setFontSize(20);
				parraf_titulo.setText(proceso);
				
				XWPFParagraph paragraph_descripcion = document.createParagraph();		
				XWPFRun parraf_descripcion = paragraph_descripcion.createRun();		
				parraf_descripcion.setBold(false);
				parraf_descripcion.setItalic(true);
				parraf_descripcion.setFontSize(18);
				parraf_descripcion.setText(descripcion);
				
				if(cabecera != null && cabecera.length()>0 && "".equals(cabecera)==false){
					XWPFParagraph paragraph_descripcion2 = document.createParagraph();		
					XWPFRun parraf_descripcion2 = paragraph_descripcion2.createRun();		
					parraf_descripcion2.setBold(false);
					parraf_descripcion2.setItalic(true);
					parraf_descripcion2.setFontSize(18);
					parraf_descripcion2.setText(cabecera);		        	
		        }
				
				boolean mensaje_ok=false;
				
				for(int i=0;i<mensajes.size();i++){
					mensaje_ok=true;
					XWPFParagraph paragraph_mensaje = document.createParagraph();
					XWPFRun parraf_mensaje = paragraph_mensaje.createRun();
					parraf_mensaje.setText(String.valueOf((char) 183));
					parraf_mensaje.setFontFamily("Symbol");
					parraf_mensaje.setFontSize(14);
					parraf_mensaje = paragraph_mensaje.createRun();
					parraf_mensaje.setFontSize(14);
					parraf_mensaje.setText(String.valueOf((char) 32));
					parraf_mensaje.setText(mensajes.get(i));
				}
				
				if(mensaje_ok==true){
				    FileOutputStream out = new FileOutputStream(new File(rutasalida));	
				    document.write(out); 
				    document.close();   
				    out.close();
				    msg="DocumentGenerator::generaWord::Se genero el word del proceso "+proceso;
					LOGGER.error(msg);
					System.out.println(msg);
				    return true;
				}else{
					document.close(); 
					msg="DocumentGenerator::generaWord::NO se genero el word del proceso "+proceso;
					LOGGER.error(msg);
					System.out.println(msg);
					return false;
				}
				
			}else{
				msg="DocumentGenerator::generaWord::NO se genero el word del proceso "+proceso+" porque no existen datos.";
				LOGGER.error(msg);
				System.out.println(msg);
				return true;
			}
		
		} catch (Exception e) {
			// TODO: handle exception
			msg="DocumentGenerator::generaWord::ERROR::Fallo al crear el word del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			msg=e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
			return false;
		}
	}
	
	private static boolean generaExcel  (String proceso, 
										 Vector<String>mensajes, 
										 Vector<String>estadisticas,
										 String descripcion, 
										 String short_process, 
										 String ruta,
										 String excelTemplate,
										 String excelSheet,
										 String cabecera,
										 String tipodoc){
		
		String msg="";
		try {
			msg="DocumentGenerator::generaExcel::generando excel del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			
			if(mensajes.size()>0){
				
				
				String rutaplantilla=excelTemplate;
				String rutasalida=rutaSalida(ruta, tipodoc, short_process, excelTemplate);
				
				System.out.println(rutaplantilla);
				XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(rutaplantilla));
		        XSSFSheet sheet = wb.getSheet(excelSheet);
		        
		        XSSFRow r =sheet.createRow(5);
		        XSSFCell cell=r.createCell(1);
		        
		        XSSFCellStyle style=wb.createCellStyle();
		        Font font = wb.createFont();
		        font.setColor(HSSFColor.HSSFColorPredefined.DARK_TEAL.getIndex());
																	
													
		        font.setBold(true);
		        font.setFontHeightInPoints((short) 16);
		        style.setFont(font);
		        
		        cell.setCellValue(descripcion);
		        cell.setCellStyle(style);
		        
		        XSSFCellStyle styleBordes=wb.createCellStyle();
		        styleBordes.setBorderBottom(BorderStyle.THIN);
		        styleBordes.setBorderTop(BorderStyle.THIN);
		        styleBordes.setBorderRight(BorderStyle.THIN);
		        styleBordes.setBorderLeft(BorderStyle.THIN);
		        
		        int fila=9;
		        if(cabecera != null && cabecera.length()>0 && "".equals(cabecera)==false){
		        	XSSFCell cell2=sheet.getRow(fila).createCell(1);
		        	cell2.setCellValue(cabecera);
		        	cell2.setCellStyle(styleBordes);
		        }
		        
		        
		        for(int i=0;i<mensajes.size();i++){
		        	fila++;
		        	XSSFRow row=sheet.createRow(fila);
		        	XSSFCell cellmsg=row.createCell(1);
		        	cellmsg.setCellValue(mensajes.get(i).toString());
		        	cellmsg.setCellStyle(styleBordes);
		        }

		        try {
		            FileOutputStream outputStream = new FileOutputStream(rutasalida);
		            wb.write(outputStream);
		            wb.close();
		            outputStream.close();
		            msg="DocumentGenerator::generaExcel::Se genero el excel del proceso "+proceso;
					LOGGER.info(msg);
					System.out.println(msg);
		            return true;
		        } catch (Exception e) {
		        	msg="DocumentGenerator::generaExcel::ERROR::NO se pudo crear el excel para el proceso "+proceso;
		        	LOGGER.error(msg);
		        	System.out.println(msg);
		            msg=e.toString();
		            LOGGER.error(msg);
		        	System.out.println(msg);
		            return false;
		        }
			}else{
				msg="DocumentGenerator::generaExcel::ERROR::NO se pudo crear el excel para el proceso "+proceso+" porque no existen datos.";
	        	LOGGER.error(msg);
	        	System.out.println(msg);
				return true;
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			msg="DocumentGenerator::generaExcel::ERROR::Fallo al tratar el excel del proceso "+proceso;
        	LOGGER.error(msg);
        	System.out.println(msg);
            msg=e.toString();
            LOGGER.error(msg);
        	System.out.println(msg);
        		return false;
		}	
	}
	
	
	private static int generaExcelPorCeldas      (String proceso, 
												  Vector<String>celdas, 
												  Vector<String>estadisticas,
						 						  String descripcion, 
												  String short_process, 
												  String ruta,
												  String excelTemplate,
												  String excelSheet,
												  String cabecera,
												  String tipodoc){
		String msg="";
		HashMap<String, XSSFRow>hmFilas=new HashMap<String, XSSFRow>();
		int totFilas=0;
		
		try {
			msg="DocumentGenerator::generaExcelPorCeldas::generando excel del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			
			if(celdas.size()>0){

				String rutaplantilla=excelTemplate;
				String rutasalida=rutaSalida(ruta, tipodoc, short_process, excelTemplate);

				XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(rutaplantilla));
		        XSSFSheet sheet = wb.getSheet(excelSheet);
		        
		        XSSFCellStyle style=wb.createCellStyle();
		        Font font = wb.createFont();
		        
		        font.setColor(HSSFColor.HSSFColorPredefined.DARK_TEAL.getIndex());
		        font.setBold(true);
		        font.setFontHeightInPoints((short) 16);
		        style.setFont(font);
		        
		        XSSFCell cell=sheet.createRow(5).createCell(1);
		        cell.setCellValue(descripcion);
		        cell.setCellStyle(style);
		        
		        XSSFCellStyle styleBordes=wb.createCellStyle();
		        styleBordes.setBorderBottom(BorderStyle.THIN);
		        styleBordes.setBorderTop(BorderStyle.THIN);
		        styleBordes.setBorderRight(BorderStyle.THIN);
		        styleBordes.setBorderLeft(BorderStyle.THIN);
		        
		        int fila=9;
		        if(cabecera != null && cabecera.length()>0 && "".equals(cabecera)==false){
		        	XSSFCell cell2=sheet.createRow(fila).createCell(1);
		        	cell2.setCellValue(cabecera);
		        	cell2.setCellStyle(styleBordes);
		        }
		        
		        
		        for(int i=0;i<celdas.size();i++){
		        	try {
		        		//Tratamiento para mensajes tipo EXCELROW desde ALG1
		        		String arr[]=celdas.get(i).split("\";\"");
			        	
			        	String idFila = arr[0];
			        	int idCol     = Integer.parseInt(arr[1]);
			        	String valorCelda=arr[2];
			        	if(arr.length>3){
			        		valorCelda="";
			        		for(int k=2;k<arr.length;k++){
			        			valorCelda+=arr[k];
			        			if(k<arr.length-1){
			        				valorCelda+="\";\"";
			        			}
			        		}
			        	}
			        	
			        	if(hmFilas.get(idFila)==null){
			        		fila++;
			        		totFilas++;
			        		XSSFRow row=sheet.createRow(fila);
			        		hmFilas.put(idFila, row);
			        	}
			        	/*
			        	XSSFRow row=hmFilas.get(idFila);
			        	XSSFCell cellmsg=row.createCell(idCol);*/
			        	XSSFCell cellmsg=hmFilas.get(idFila).createCell(idCol);
			        	cellmsg.setCellValue(valorCelda);
		        		cellmsg.setCellStyle(styleBordes);
		        		
					} catch (Exception e) {
						// TODO: handle exception
						msg="DocumentGenerator::generaExcelPorCeldas::Error al procesar celda: '"+celdas.get(i)+"'";
						LOGGER.info(msg);
						System.out.println(msg);
						msg=e.toString();
						LOGGER.info(msg);
						System.out.println(msg);
					}
		        	
		        }

		        try {
		            FileOutputStream outputStream = new FileOutputStream(rutasalida);
		            wb.write(outputStream);
		            wb.close();
		            outputStream.close();
		            msg="DocumentGenerator::generaExcelPorCeldas::Se genero el excel del proceso "+proceso;
					LOGGER.info(msg);
					System.out.println(msg);
				
		            return totFilas;
		        } catch (Exception e) {
		        	msg="DocumentGenerator::generaExcelPorCeldas::ERROR::NO se pudo crear el excel para el proceso "+proceso;
		        	LOGGER.error(msg);
		        	System.out.println(msg);
		            msg=e.toString();
		            LOGGER.error(msg);
		        	System.out.println(msg);
		            return totFilas;
		        }
			}else{
				msg="DocumentGenerator::generaExcelPorCeldas::ERROR::NO se pudo crear el excel para el proceso "+proceso+" porque no existen datos.";
	        	LOGGER.error(msg);
	        	System.out.println(msg);
				return 0;
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			msg="DocumentGenerator::generaExcelPorCeldas::ERROR::Fallo al tratar el excel del proceso "+proceso;
        	LOGGER.error(msg);
        	System.out.println(msg);
            msg=e.toString();
            LOGGER.error(msg);
        	System.out.println(msg);
        		return -1;
		}	

}

	
	
	
	

	
	private static boolean generaBody(String proceso, 
									 Vector<String>mensajes, 
									 Vector<String>estadisticas,
									 String descripcion, 
									 String short_process, 
									 String ruta,
									 String excelTemplate,
									 String excelSheet,
									 String cabecera,
									 String tipodoc){
		String msg="DocumentGenerator::generaBody::generando cuerpo del mail del proceso "+proceso;
		LOGGER.error(msg);
		System.out.println(msg);
		try {
			if(mensajes.size()>0){
				String rutasalida=rutaSalida(ruta, tipodoc, short_process, excelTemplate);
		        //File archivo = new File(rutasalida);
		        BufferedWriter bw;
		        //bw = new BufferedWriter(new FileWriter(archivo));
		        bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(rutasalida), 
		        						 "UTF-8"));

		        String txtBody="Estimado usuario, \n\n"
					      +"Se adjunta el informe generado a partir del reporte al que está suscrito del proceso "+proceso+".\n\n";
		        bw.append(txtBody);
		        if(estadisticas.size()>0){
		        	bw.append("Estadísticas del reporte: \r\n");
		        	for(int i=0;i<estadisticas.size();i++){
		        		bw.append(estadisticas.get(i)+"\r\n");
		        	}
		        	bw.append("\r\n\n");
		        }
		        
		        if(cabecera != null && cabecera.equals("")==false && cabecera.length()>0){
		        	bw.write(cabecera+"\r\n");	
		        }
		        for(int i=0;i<mensajes.size();i++){
		        	bw.write(mensajes.get(i)+"\r\n");
		        }
		        bw.append("\r\n");
		        bw.append("Un saludo.");
		        bw.close();
		        msg="DocumentGenerator::generaBody::Se genero el cuerpo del mail del proceso "+proceso;
				LOGGER.error(msg);
				System.out.println(msg);
		        return true;
			}else{
				msg="DocumentGenerator::generaBody::NO se genero el cuerpo del mail del proceso "+proceso+" porque no existen datos.";
				LOGGER.error(msg);
				System.out.println(msg);
				return true;			
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg="DocumentGenerator::generaBody::ERROR::Fallo al generar el .txt con el cuerpo en el proceso "+proceso;
        	LOGGER.error(msg);
        	System.out.println(msg);
            msg=e.toString();
            LOGGER.error(msg);
        	System.out.println(msg);
        	return false;
		}	
		
		
	}
	
	private static boolean generaTxt(String proceso, 
			 Vector<String>mensajes, 
			 Vector<String>estadisticas,
			 String descripcion, 
			 String short_process, 
			 String ruta,
			 String excelTemplate,
			 String excelSheet,
			 String cabecera,
			 String tipodoc){
		
		String extension=(tipodoc.equals("DAT"))?".dat":".txt";
		String msg="DocumentGenerator::generaTxt::generando "+extension+" del proceso "+proceso;
		LOGGER.error(msg);
		System.out.println(msg);
		try {
			if(mensajes.size()>0){
				String rutasalida=rutaSalida(ruta, tipodoc, short_process, excelTemplate);
				//File archivo = new File(rutasalida);
				BufferedWriter bw;
				//bw = new BufferedWriter(new FileWriter(archivo));
				bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(rutasalida), 
										"UTF-8"));
				if(cabecera != null && cabecera.equals("")==false && cabecera.length()>0){
		        	bw.write(cabecera+"\r\n");
		        }
				for(int i=0;i<mensajes.size();i++){
					bw.write(mensajes.get(i)+"\r\n");
				}	
				bw.close();
				msg="DocumentGenerator::generaTxt::Se genero el "+extension+" del proceso "+proceso;
				LOGGER.error(msg);
				System.out.println(msg);
				return true;
			}else{
				msg="DocumentGenerator::generaTxt::NO se genero el "+extension+" del proceso "+proceso+" porque no existen datos.";
				LOGGER.error(msg);
				System.out.println(msg);
				return true;			
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg="DocumentGenerator::generaTxt::ERROR::Fallo al generar el "+extension+" con del proceso "+proceso;
			LOGGER.error(msg);
			System.out.println(msg);
			msg=e.toString();
			LOGGER.error(msg);
			System.out.println(msg);
			return false;
		}	
	
	
	}
	
	private static String rutaPlantilla (String ruta, String tipo_fic){
		try {
			if("EXCEL".equals(tipo_fic)){
				return ruta+"/Templates/Template_Alertas_Excel.xlsx";
			}else if ("WORD".equals(tipo_fic)) {
				return ruta+"/Templates/Template_Alertas_Word.docx";
			}else{
				return null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}
	private static String rutaSalida (String ruta, String tipo_fic, String short_process, String excelTemplate){
		
		String msg="";
		try {
			if("EXCEL".equals(tipo_fic)){
				String extension="";
				try {
					extension=getFileExtension(excelTemplate);
				} catch (Exception e) {
					// TODO: handle exception
					msg="DocumentGenerator::rutaSalida::ERROR::No se pudo obtener la extension en la ruta '"+excelTemplate+"'";
					LOGGER.error(msg);
					System.out.println(msg);
					msg=e.toString();
					LOGGER.error(msg);
					System.out.println(msg);
				}
				String rutaReturn=ruta+"/"+short_process+extension;
				msg="DocumentGenerator::rutaSalida::RESULTADO::"+rutaReturn;
				return rutaReturn;				
			}else if ("WORD".equals(tipo_fic)) {
				return ruta+"/"+short_process+".docx";
			}else if ("TXT".equals(tipo_fic)) {
				return ruta+"/"+short_process+".txt";
			}else if ("DAT".equals(tipo_fic)) {
				return ruta+"/"+short_process+".dat";
			}else if ("CTL".equals(tipo_fic)){
				return ruta+"/"+short_process+".ctl";
			}else if ("CUERPO".equals(tipo_fic)) {
				return ruta+"/CUERPO_"+short_process+".txt";
			}else{
				msg="DocumentGenerator::rutaSalida::ERROR::Tipo de documento '"+tipo_fic+"' no existe.";
				return null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	} 
	
	private static String getFileExtension(String ruta) {
	    
	    int lastIndexOf = ruta.lastIndexOf(".");
	    if (lastIndexOf == -1) {
	        return ""; // empty extension
	    }
	    return ruta.substring(lastIndexOf);
	}
	
	private static String rutaCuerpoCorreo(String ruta, String tipo_fic, String short_process){
		try {
			if("EXCEL".equals(tipo_fic) || "WORD".equals(tipo_fic) || "TXT".equals(tipo_fic) || "DAT".equals(tipo_fic)){
				return ruta+"/BODY_"+short_process+".txt";
			}else{
				return null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}	
	
	private static boolean generaCuerpoCorreo(String tipo_doc,
											  String proceso, 
											  Vector<String	>mensajes, 
											  Vector<String>estadisticas,
											  String descripcion, 
											  String short_process, 
											  String ruta,
											  String excelTemplate,
											  String cabecera,
											  int filasExcel){
		//Genera el cuerpo de correo para todos los tipos de envio que no sean "CUERPO"
		try {
			String rutaenvio=rutaCuerpoCorreo(ruta, tipo_doc, short_process);
			System.out.println("DocumentGenerator::generaCuerpoCorreo::Generando cuerpo en '"+rutaenvio+"'");
			String txtBody="Estimado usuario, \n\n"
					      +"Se adjunta el informe generado a partir del reporte al que está suscrito del proceso "+proceso+".\n\n";
			if(estadisticas.size()>0){
				txtBody+="A nivel agregado se han obtenido los siguientes resultados: \n\n";
				for(int i=0;i<estadisticas.size();i++){
					txtBody+=estadisticas.get(i)+" \n";
				}
				txtBody+="\n";
			}else if(filasExcel>0){
				txtBody+=String.valueOf(filasExcel)+" filas totales. \n\n";
			}
			txtBody+="Un saludo.";
			BufferedWriter bw;
			bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(rutaenvio), 
									 "UTF-8"));
			bw.write(txtBody);
			bw.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error("Error al generar el cuerpo del correo.");
			return false;
		}
	}
}
