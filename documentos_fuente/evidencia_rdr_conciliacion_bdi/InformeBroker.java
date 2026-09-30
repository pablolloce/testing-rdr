import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;


public class InformeBroker {
	
	public static Connection obj_con=null;
    public static ConDB obj_ConDB=null;
    
	public static void main(String[] args)  throws Exception {
		
		System.out.println("********* INICIO INFORME CONCILIACION BROKER BDI-RDR *********");
		
		obj_ConDB=new ConDB();		
		obj_ConDB.ObtenerCredenciales();	
		obj_con=obj_ConDB.ObtenerConexion();
		
		String fich_salida = args[0];
		String fich_plantilla = fich_salida + "_Plantilla.xlsx";
	    
		Date fechaActual = new Date();
	    DateFormat formatoFecha = new SimpleDateFormat("yyyyMMdd");
	    String fich_sal = fich_salida + "_" + formatoFecha.format(fechaActual) + ".xlsx";
		
		FileInputStream fisp = new FileInputStream(fich_plantilla);
	    
		XSSFWorkbook workbook = new XSSFWorkbook(fisp);
			
		System.out.println("********* RELLENA BDI NO EN BDI *********");
		XSSFSheet sheet1 = workbook.getSheet("NoBDI");	   
	    fillSheet(sheet1, workbook, obj_ConDB.getBrokerNotBDI(obj_con));
	    
	    System.out.println("********* RELLENA RDR NO EN RDR *********");
		XSSFSheet sheet2 = workbook.getSheet("NoRDR");	   
	    fillSheet(sheet2, workbook, obj_ConDB.getBrokerNotRDR(obj_con));
	    
	    System.out.println("********* RELLENA DISTINTOS CODES *********");
		XSSFSheet sheet3 = workbook.getSheet("DistintoRDR");	   
	    fillSheet(sheet3, workbook, obj_ConDB.getBrokerBDIRDR(obj_con));
	    
	    System.out.println("********* RELLENA DISTINTOS NAMES *********");
		XSSFSheet sheet4 = workbook.getSheet("DistintoNme");	   
	    fillSheet(sheet4, workbook, obj_ConDB.getBrokerName(obj_con));	    
        
	    XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
	    POIXMLProperties xmlProps = workbook.getProperties();
	    POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
	    coreProps.setCreator("IT BBVA");
	    coreProps.setTitle("Conciliación Broker BDI-RDR");	          
	    
	    System.out.println("********* FIN INFORME CONCILIACION BROKER BDI-RDR *********");
	    
	    try {
	    	FileOutputStream out = new FileOutputStream(fich_sal);
	    	workbook.write(out);
	    	out.close();
			fisp.close();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	private static Map<String,XSSFCellStyle> createStyles (XSSFWorkbook wb){
		Map<String, XSSFCellStyle> styles = new HashMap<String, XSSFCellStyle>();
		
		XSSFFont font = wb.createFont();
		font.setFontName(HSSFFont.FONT_ARIAL);                
	    font.setFontHeightInPoints((short) 9);
	    
	    XSSFColor color = new XSSFColor(new java.awt.Color(218, 238, 243));
	    XSSFColor color1 = new XSSFColor(new java.awt.Color(0, 176, 240));
	    
		XSSFCellStyle style1 = wb.createCellStyle();
		style1.setFont(font);
        //style1.setAlignment(XSSFCellStyle.ALIGN_CENTER); 
       // style1.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
        style1.setWrapText(true);
       // style1.setBorderBottom(XSSFCellStyle.BORDER_THIN);    
	    style1.setBorderColor(XSSFCellBorder.BorderSide.BOTTOM, color1);
	    //style1.setBorderTop(XSSFCellStyle.BORDER_THIN);        
	    style1.setBorderColor(XSSFCellBorder.BorderSide.TOP, color1);
	  //  style1.setBorderRight(XSSFCellStyle.BORDER_THIN);    
	    style1.setBorderColor(XSSFCellBorder.BorderSide.RIGHT, color1);
	  //  style1.setBorderLeft(XSSFCellStyle.BORDER_THIN);        
	    style1.setBorderColor(XSSFCellBorder.BorderSide.LEFT, color1);
        styles.put("filai", style1);
        
		XSSFCellStyle style2 = wb.createCellStyle();
		style2.setFont(font);
       // style2.setAlignment(XSSFCellStyle.ALIGN_CENTER); 
       // style2.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
        style2.setWrapText(true);
        style2.setFillForegroundColor(color);
        //style2.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        //style2.setBorderBottom(XSSFCellStyle.BORDER_THIN);    
	    style2.setBorderColor(XSSFCellBorder.BorderSide.BOTTOM, color1);
	    //style2.setBorderTop(XSSFCellStyle.BORDER_THIN);        
	    style2.setBorderColor(XSSFCellBorder.BorderSide.TOP, color1);
	   // style2.setBorderRight(XSSFCellStyle.BORDER_THIN);    
	    style2.setBorderColor(XSSFCellBorder.BorderSide.RIGHT, color1);
	    //style2.setBorderLeft(XSSFCellStyle.BORDER_THIN);        
	    style2.setBorderColor(XSSFCellBorder.BorderSide.LEFT, color1);
        styles.put("filap", style2);
		
		return styles;
	}
	
	private static void fillSheet (XSSFSheet sheet, XSSFWorkbook workbook, ArrayList<Object[]> array){
		Map<String,XSSFCellStyle> styles = createStyles(workbook);
		
		int rownum = 2;
    	
        for (Object[] result : array){
        	XSSFRow row = sheet.createRow(rownum++);
        	
        	int cellnum = 1;
        	
        	for (Object obj : result){
        		XSSFCell cell = row.createCell(cellnum++);
            	cell.setCellType(Cell.CELL_TYPE_STRING);
            	if ((rownum)%2 != 0){
            		cell.setCellStyle(styles.get("filai"));
            	} else {
            		cell.setCellStyle(styles.get("filap"));
            	}
            	cell.setCellValue((String)obj);
        	}
        }
	}

}
