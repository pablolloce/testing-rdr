import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class GenerarXML_version1 {   		

    public String obtenerXML_version1(HashMap<String,String> obj_line, int i, String usuario){  

	String XML_SALIDA="";
	if(usuario!=null&&!usuario.trim().equals("")){
	    XML_SALIDA="<PARTYSETUP><AUDIT_INFORMATION><USER>"+usuario+"</USER></AUDIT_INFORMATION><GLOBALS><GLOBAL>";
	} else{
	    XML_SALIDA="<PARTYSETUP><AUDIT_INFORMATION></AUDIT_INFORMATION><GLOBALS><GLOBAL>";
	}
	
	try    {
//System.out.println(obj_line);
	    //GLOBAL	    
	    if(obj_line.get("GL.01")!=null&&!obj_line.get("GL.01").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<MATRIX_HOUSE_CODE>"+obj_line.get("GL.01")+"</MATRIX_HOUSE_CODE>"; 
	    }
	    if(obj_line.get("GL.02")!=null&&!obj_line.get("GL.02").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<MATRIX_HOUSE_SYSTEM>"+obj_line.get("GL.02")+"</MATRIX_HOUSE_SYSTEM>"; 
	    }
	    if(obj_line.get("GL.05")!=null&&!obj_line.get("GL.05").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PERSONALITY>"+obj_line.get("GL.05")+"</PERSONALITY>"; 
	    }	
	    if(obj_line.get("GL.06")!=null&&!obj_line.get("GL.06").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PERSONALITY_SUBTYPE>"+obj_line.get("GL.06")+"</PERSONALITY_SUBTYPE>"; 
	    }
	    if(obj_line.get("GL.07")!=null&&!obj_line.get("GL.07").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_TYPE>"+obj_line.get("GL.07")+"</COUNTERPARTY_TYPE>"; 
	    }
	    if(obj_line.get("GL.10")!=null&&!obj_line.get("GL.10").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<ENTERPRISE_OWNER>"+obj_line.get("GL.10")+"</ENTERPRISE_OWNER>"; 
	    }
	    if(obj_line.get("GL.11")!=null&&!obj_line.get("GL.11").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<BRANCH_OWNER>"+obj_line.get("GL.11")+"</BRANCH_OWNER>"; 
	    }
	    if(obj_line.get("GL.12")!=null&&!obj_line.get("GL.12").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTRY_OF_ORIGIN>"+obj_line.get("GL.12")+"</COUNTRY_OF_ORIGIN>"; 
	    }
	    if(obj_line.get("GL.03")!=null&&!obj_line.get("GL.03").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LEGAL_NAME>"+obj_line.get("GL.03")+"</LEGAL_NAME>"; 
	    }
	    if(obj_line.get("GL.13")!=null&&!obj_line.get("GL.13").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LEGAL_REGIME>"+obj_line.get("GL.13")+"</LEGAL_REGIME>"; 
	    }
	    if(obj_line.get("GL.04")!=null&&!obj_line.get("GL.04").trim().equals("")){   
		String cad1=obj_line.get("GL.04");
		    String cad2="";
			SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy");
			SimpleDateFormat formato2 = new SimpleDateFormat("yyyyMMdd");
			Date fechaDate = null;
			try {
		    		fechaDate = formato1.parse(cad1);	    
		    		cad2 = formato2.format(fechaDate);	        
			} 
			catch (ParseException ex)    {
		    		Date fecha = new Date();
		    		cad2 = formato2.format(fecha);
			}
		
		
		XML_SALIDA=XML_SALIDA+"<ESTABLISHMENT_BIRTH_DATE>"+cad2+"</ESTABLISHMENT_BIRTH_DATE>"; 
	    }
	    if(obj_line.get("GL.08")!=null&&!obj_line.get("GL.08").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<BANK_INDICATOR>"+obj_line.get("GL.08")+"</BANK_INDICATOR>"; 
	    }
	    if(	(obj_line.get("GL.09.01")!=null&&!obj_line.get("GL.09.01").trim().equals(""))||
		    (obj_line.get("GL.09.02")!=null&&!obj_line.get("GL.09.02").trim().equals(""))||
		    (obj_line.get("GL.09.03")!=null&&!obj_line.get("GL.09.03").trim().equals(""))|| 
		    (obj_line.get("GL.09.04")!=null&&!obj_line.get("GL.09.04").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIERS>"; 

		if(obj_line.get("GL.09.01")!=null&&!obj_line.get("GL.09.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>LEIID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.09.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}		

		if(obj_line.get("GL.09.02")!=null&&!obj_line.get("GL.09.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CSBCODE</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.09.02")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	

		if(obj_line.get("GL.09.03")!=null&&!obj_line.get("GL.09.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MARKITID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.09.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	

		if(obj_line.get("GL.09.04")!=null&&!obj_line.get("GL.09.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>PRELEIID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.09.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIERS>"; 
	    }	    

	    if(	(obj_line.get("GL.14.01.01")!=null&&!obj_line.get("GL.14.01.01").trim().equals(""))||
		    (obj_line.get("GL.14.01.02")!=null&&!obj_line.get("GL.14.01.02").trim().equals(""))||
		    (obj_line.get("GL.14.01.03")!=null&&!obj_line.get("GL.14.01.03").trim().equals(""))|| 
		    (obj_line.get("GL.14.01.04")!=null&&!obj_line.get("GL.14.01.04").trim().equals(""))||
		    (obj_line.get("GL.14.01.05")!=null&&!obj_line.get("GL.14.01.05").trim().equals(""))|| 
		    (obj_line.get("GL.14.01.06")!=null&&!obj_line.get("GL.14.01.06").trim().equals(""))||
		    (obj_line.get("GL.14.01.07")!=null&&!obj_line.get("GL.14.01.07").trim().equals(""))|| 
		    (obj_line.get("GL.14.01.08")!=null&&!obj_line.get("GL.14.01.08").trim().equals(""))||
		    (obj_line.get("GL.14.01.09")!=null&&!obj_line.get("GL.14.01.09").trim().equals(""))|| 
		    (obj_line.get("GL.14.01.10")!=null&&!obj_line.get("GL.14.01.10").trim().equals(""))|| 
		    (obj_line.get("GL.14.01.11")!=null&&!obj_line.get("GL.14.01.11").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.01")!=null&&!obj_line.get("GL.14.02.01").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.02")!=null&&!obj_line.get("GL.14.02.02").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.03")!=null&&!obj_line.get("GL.14.02.03").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.04")!=null&&!obj_line.get("GL.14.02.04").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.05")!=null&&!obj_line.get("GL.14.02.05").trim().equals(""))|| 
		    (obj_line.get("GL.14.02.06")!=null&&!obj_line.get("GL.14.02.06").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<REGULATORY_INFORMATION>"; 

		if(obj_line.get("GL.14.01.01")!=null&&!obj_line.get("GL.14.01.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_COM</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		

		if(obj_line.get("GL.14.01.02")!=null&&!obj_line.get("GL.14.01.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_CRD</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.03")!=null&&!obj_line.get("GL.14.01.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>SEC_CRD</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.03")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.04")!=null&&!obj_line.get("GL.14.01.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>ENDUSEXP</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.05")!=null&&!obj_line.get("GL.14.01.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_EQD</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.06")!=null&&!obj_line.get("GL.14.01.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>SEC_EQD</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.06")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		

		if(obj_line.get("GL.14.01.07")!=null&&!obj_line.get("GL.14.01.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FINENT</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.07")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.08")!=null&&!obj_line.get("GL.14.01.08").trim().equals("")){
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_FX</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.08")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.09")!=null&&!obj_line.get("GL.14.01.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_IRS</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.09")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.10")!=null&&!obj_line.get("GL.14.01.10").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>SPECIENT</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.10")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.01.11")!=null&&!obj_line.get("GL.14.01.11").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>USPERSON</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.01.11")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.02.01")!=null&&!obj_line.get("GL.14.02.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>RR_CREM</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		

		if(obj_line.get("GL.14.02.02")!=null&&!obj_line.get("GL.14.02.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>EMIRCAT</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.02.03")!=null&&!obj_line.get("GL.14.02.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>EMISEC</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.03")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.02.04")!=null&&!obj_line.get("GL.14.02.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>INDICYN</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.02.05")!=null&&!obj_line.get("GL.14.02.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FINALEM</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}

		if(obj_line.get("GL.14.02.06")!=null&&!obj_line.get("GL.14.02.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMIR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>MANPARTY</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("GL.14.02.06")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}			

		XML_SALIDA=XML_SALIDA+"</REGULATORY_INFORMATION>"; 
	    }	    

	    //LOCAL
	    XML_SALIDA=XML_SALIDA+"<LOCALS><LOCAL>";
	    if(obj_line.get("LO.01")!=null&&!obj_line.get("LO.01").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LOCAL_CODE>"+obj_line.get("LO.01")+"</LOCAL_CODE>"; 
	    }
	    if(obj_line.get("LO.02")!=null&&!obj_line.get("LO.02").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LOCAL_CODE_SYSTEM>"+obj_line.get("LO.02")+"</LOCAL_CODE_SYSTEM>"; 
	    }
	    if(obj_line.get("LO.03")!=null&&!obj_line.get("LO.03").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<ENTITY_NAME>"+obj_line.get("LO.03")+"</ENTITY_NAME>"; 
	    }	
	    if(obj_line.get("LO.04")!=null&&!obj_line.get("LO.04").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<ADDRESS>"+obj_line.get("LO.04")+"</ADDRESS>"; 
	    }
	    if(obj_line.get("LO.05")!=null&&!obj_line.get("LO.05").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<INTERNAL_NUMBER>"+obj_line.get("LO.05")+"</INTERNAL_NUMBER>"; 
	    }
	    if(obj_line.get("LO.06")!=null&&!obj_line.get("LO.06").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<EXTERNAL_NUMBER>"+obj_line.get("LO.06")+"</EXTERNAL_NUMBER>"; 
	    }
	    if(obj_line.get("LO.07")!=null&&!obj_line.get("LO.07").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTRY_OF_RESIDENCE>"+obj_line.get("LO.07")+"</COUNTRY_OF_RESIDENCE>"; 
	    }
	    if(obj_line.get("LO.08")!=null&&!obj_line.get("LO.08").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<STATE>"+obj_line.get("LO.08")+"</STATE>"; 
	    }
	    if(obj_line.get("LO.09")!=null&&!obj_line.get("LO.09").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COLONY>"+obj_line.get("LO.09")+"</COLONY>"; 
	    }
	    if(obj_line.get("LO.10")!=null&&!obj_line.get("LO.10").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PROVINCE>"+obj_line.get("LO.10")+"</PROVINCE>"; 
	    }
	    if(obj_line.get("LO.11")!=null&&!obj_line.get("LO.11").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<CITY_DISTRICT>"+obj_line.get("LO.11")+"</CITY_DISTRICT>"; 
	    }
	    if(obj_line.get("LO.12")!=null&&!obj_line.get("LO.12").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<CITY_TOWN>"+obj_line.get("LO.12")+"</CITY_TOWN>"; 
	    }
	    if(obj_line.get("LO.13")!=null&&!obj_line.get("LO.13").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<POSTAL_CODE>"+obj_line.get("LO.13")+"</POSTAL_CODE>"; 
	    }
	    if(obj_line.get("LO.14")!=null&&!obj_line.get("LO.14").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<CERTICATED_OF_NON_RESIDENCE>"+obj_line.get("LO.14")+"</CERTICATED_OF_NON_RESIDENCE>"; 
	    }
	    if(obj_line.get("LO.15")!=null&&!obj_line.get("LO.15").trim().equals("")){   
		String cad1=obj_line.get("LO.15");
		    String cad2="";
			SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy");
			SimpleDateFormat formato2 = new SimpleDateFormat("yyyyMMdd");
			Date fechaDate = null;
			try {
		    		fechaDate = formato1.parse(cad1);	    
		    		cad2 =formato2.format(fechaDate);	        
			} 
			catch (ParseException ex)    {
		    		Date fecha = new Date();
		    		cad2 = formato2.format(fecha);
			}
		
		XML_SALIDA=XML_SALIDA+"<VALIDATED_DATE_CERTIFICATED_OF_NON_RESIDENCE>"+cad2+"</VALIDATED_DATE_CERTIFICATED_OF_NON_RESIDENCE>"; 
	    }			

	    if(	(obj_line.get("LO.16.01.01")!=null&&!obj_line.get("LO.16.01.01").trim().equals(""))||
		    (obj_line.get("LO.16.01.02")!=null&&!obj_line.get("LO.16.01.02").trim().equals(""))||
		    (obj_line.get("LO.16.02.01")!=null&&!obj_line.get("LO.16.02.01").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<REGULATORY_INFORMATION>"; 

		// REVISAR CON BELEN INICIO
		
		if(obj_line.get("LO.16.01.01")!=null&&!obj_line.get("LO.16.01.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>";
		    if(obj_line.get("GL.05")!=null&&!obj_line.get("GL.05").trim().equals("")
			    &&!obj_line.get("GL.05").trim().equals("LEGALENT")){
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCACLASS</CLASSIFICATION>"; 
		    }else {
			XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCACLAS2</CLASSIFICATION>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		
		if(obj_line.get("LO.16.01.02")!=null&&!obj_line.get("LO.16.01.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>"; 
		    if(obj_line.get("GL.05")!=null&&!obj_line.get("GL.05").trim().equals("")
			    &&!obj_line.get("GL.05").trim().equals("LEGALENT")){
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCAINDIC</CLASSIFICATION>"; 
		    }else {
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCAINDI2</CLASSIFICATION>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}
		
		/*
		if(obj_line.get("LO.16.01.01")!=null&&!obj_line.get("LO.16.01.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>";
		    if(obj_line.get("GL.05")!=null&&!obj_line.get("GL.05").trim().equals("")
			    &&!obj_line.get("GL.05").trim().equals("LEGALENT")){
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCACLAS2</CLASSIFICATION>"; 
		    }else {
			XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCACLASS</CLASSIFICATION>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		
		if(obj_line.get("LO.16.01.02")!=null&&!obj_line.get("LO.16.01.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>"; 
		    if(obj_line.get("GL.05")!=null&&!obj_line.get("GL.05").trim().equals("")
			    &&!obj_line.get("GL.05").trim().equals("LEGALENT")){
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCAINDI2</CLASSIFICATION>"; 
		    }else {
			 XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCAINDIC</CLASSIFICATION>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}
		*/
		
		// REVISAR CON BELEN FIN
		
		/*if(obj_line.get("LO.16.01.01")!=null&&!obj_line.get("LO.16.01.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCACLAS2</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		
		if(obj_line.get("LO.16.01.02")!=null&&!obj_line.get("LO.16.01.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FATCA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>FATCAINDI2</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.01.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}*/

		if(obj_line.get("LO.16.02.01")!=null&&!obj_line.get("LO.16.02.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>PDV</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>PDV_CLASS</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.16.02.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}			
		XML_SALIDA=XML_SALIDA+"</REGULATORY_INFORMATION>"; 
	    }	    

	    if(obj_line.get("LO.17")!=null&&!obj_line.get("LO.17").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<FATCA_COUNTRY>"+obj_line.get("LO.17")+"</FATCA_COUNTRY>"; 
	    }
	    if(obj_line.get("LO.18")!=null&&!obj_line.get("LO.18").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<ENTERPRISE>"+obj_line.get("LO.18")+"</ENTERPRISE>"; 
	    }	

	    if(	(obj_line.get("LO.19.01")!=null&&!obj_line.get("LO.19.01").trim().equals(""))||
		    (obj_line.get("LO.19.02")!=null&&!obj_line.get("LO.19.02").trim().equals(""))||
		    (obj_line.get("LO.19.03")!=null&&!obj_line.get("LO.19.03").trim().equals(""))||
		    (obj_line.get("LO.19.04")!=null&&!obj_line.get("LO.19.04").trim().equals(""))||
		    (obj_line.get("LO.19.05")!=null&&!obj_line.get("LO.19.05").trim().equals(""))||
		    (obj_line.get("LO.19.06")!=null&&!obj_line.get("LO.19.06").trim().equals(""))||
		    (obj_line.get("LO.19.07")!=null&&!obj_line.get("LO.19.07").trim().equals(""))||
		    (obj_line.get("LO.19.08")!=null&&!obj_line.get("LO.19.08").trim().equals(""))||
		    (obj_line.get("LO.19.09")!=null&&!obj_line.get("LO.19.09").trim().equals(""))||
		    (obj_line.get("LO.19.10")!=null&&!obj_line.get("LO.19.10").trim().equals(""))||
		    (obj_line.get("LO.19.11")!=null&&!obj_line.get("LO.19.11").trim().equals(""))||
		    (obj_line.get("LO.19.12")!=null&&!obj_line.get("LO.19.12").trim().equals(""))||
		    (obj_line.get("LO.19.13")!=null&&!obj_line.get("LO.19.13").trim().equals(""))||
		    (obj_line.get("LO.19.14")!=null&&!obj_line.get("LO.19.14").trim().equals(""))||
		    (obj_line.get("LO.19.15")!=null&&!obj_line.get("LO.19.15").trim().equals(""))||
		    (obj_line.get("LO.19.16")!=null&&!obj_line.get("LO.19.16").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIERS>"; 

		if(obj_line.get("LO.19.01")!=null&&!obj_line.get("LO.19.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FECNAC</TYPE>"; 
		    
		    String cad1=obj_line.get("LO.19.01");
		    String cad2="";
			SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy");
			SimpleDateFormat formato2 = new SimpleDateFormat("yyyyMMdd");
			Date fechaDate = null;
			try {
		    		fechaDate = formato1.parse(cad1);	    
		    		cad2 =formato2.format(fechaDate);	        
			} 
			catch (ParseException ex)    {
		    		Date fecha = new Date();
		    		cad2 = formato2.format(fecha);
			}
		    
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+cad2+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		
		if(obj_line.get("LO.19.02")!=null&&!obj_line.get("LO.19.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>C.I.F.</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.03")!=null&&!obj_line.get("LO.19.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CURP</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.04")!=null&&!obj_line.get("LO.19.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CODCLI</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		if(obj_line.get("LO.19.05")!=null&&!obj_line.get("LO.19.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>D.N.I</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.06")!=null&&!obj_line.get("LO.19.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CIFEX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.06")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.07")!=null&&!obj_line.get("LO.19.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>HOMOCLAV</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.07")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		if(obj_line.get("LO.19.08")!=null&&!obj_line.get("LO.19.08").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>NIF_MEX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.08")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.09")!=null&&!obj_line.get("LO.19.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>N.I.F.</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.09")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.10")!=null&&!obj_line.get("LO.19.10").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EMPNORES</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.10")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		if(obj_line.get("LO.19.11")!=null&&!obj_line.get("LO.19.11").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>Not_Def</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.11")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.12")!=null&&!obj_line.get("LO.19.12").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>OTROS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.12")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.13")!=null&&!obj_line.get("LO.19.13").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>PASAP</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.13")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		if(obj_line.get("LO.19.14")!=null&&!obj_line.get("LO.19.14").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>TARJRES</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.14")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.15")!=null&&!obj_line.get("LO.19.15").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RFC</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.15")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}
		if(obj_line.get("LO.19.16")!=null&&!obj_line.get("LO.19.16").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FISCAL_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>TAXID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.19.16")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIER>";
		}		
		XML_SALIDA=XML_SALIDA+"</FISCAL_IDENTIFIERS>"; 
	    }	

	    if(	(obj_line.get("LO.20.01")!=null&&!obj_line.get("LO.20.01").trim().equals(""))||
		    (obj_line.get("LO.20.02")!=null&&!obj_line.get("LO.20.02").trim().equals(""))||
		    (obj_line.get("LO.20.03")!=null&&!obj_line.get("LO.20.03").trim().equals(""))|| 
		    (obj_line.get("LO.20.04")!=null&&!obj_line.get("LO.20.04").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIERS>"; 

		if(obj_line.get("LO.20.01")!=null&&!obj_line.get("LO.20.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ALID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.20.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}		
		if(obj_line.get("LO.20.02")!=null&&!obj_line.get("LO.20.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ALNOVAID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.20.02")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		if(obj_line.get("LO.20.03")!=null&&!obj_line.get("LO.20.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.20.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		if(obj_line.get("LO.20.04")!=null&&!obj_line.get("LO.20.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SHORTNAME_MEX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("LO.20.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIERS>"; 
	    }	    

	    if(obj_line.get("LO.21")!=null&&!obj_line.get("LO.21").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<NAME>"+obj_line.get("LO.21")+"</NAME>"; 
	    }
	    if(obj_line.get("LO.22")!=null&&!obj_line.get("LO.22").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<SURNAME_N_1>"+obj_line.get("LO.22")+"</SURNAME_N_1>"; 
	    }
	    if(obj_line.get("LO.23")!=null&&!obj_line.get("LO.23").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<SURNAME_N_2>"+obj_line.get("LO.23")+"</SURNAME_N_2>"; 
	    }	
	    if(obj_line.get("LO.24")!=null&&!obj_line.get("LO.24").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTRY_OF_GUARANTEE>"+obj_line.get("LO.24")+"</COUNTRY_OF_GUARANTEE>"; 
	    }
	    if(obj_line.get("LO.25")!=null&&!obj_line.get("LO.25").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PHONE_TYPE_1>"+obj_line.get("LO.25")+"</PHONE_TYPE_1>"; 
	    }
	    if(obj_line.get("LO.26")!=null&&!obj_line.get("LO.26").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PHONE_NUMBER_1>"+obj_line.get("LO.26")+"</PHONE_NUMBER_1>"; 
	    }
	    if(obj_line.get("LO.27")!=null&&!obj_line.get("LO.27").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LADA_CODE_1>"+obj_line.get("LO.27")+"</LADA_CODE_1>"; 
	    }
	    if(obj_line.get("LO.28")!=null&&!obj_line.get("LO.28").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<EXT_PHONE_NUM_1>"+obj_line.get("LO.28")+"</EXT_PHONE_NUM_1>"; 
	    }
	    if(obj_line.get("LO.29")!=null&&!obj_line.get("LO.29").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PHONE_TYPE_2>"+obj_line.get("LO.29")+"</PHONE_TYPE_2>"; 
	    }
	    if(obj_line.get("LO.30")!=null&&!obj_line.get("LO.30").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PHONE_NUMBER_2>"+obj_line.get("LO.30")+"</PHONE_NUMBER_2>"; 
	    }
	    if(obj_line.get("LO.31")!=null&&!obj_line.get("LO.31").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LADA_CODE_2>"+obj_line.get("LO.31")+"</LADA_CODE_2>"; 
	    }
	    if(obj_line.get("LO.32")!=null&&!obj_line.get("LO.32").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<EXT_PHONE_NUM_2>"+obj_line.get("LO.32")+"</EXT_PHONE_NUM_2>"; 
	    }

	    //OPERATIVO
	    XML_SALIDA=XML_SALIDA+"<OPERATIVES><OPERATIVE>";

	    if(obj_line.get("OP.01")!=null&&!obj_line.get("OP.01").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_DESCRIPTION>"+obj_line.get("OP.01")+"</COUNTERPARTY_DESCRIPTION>"; 
	    }
	    if(obj_line.get("OP.02")!=null&&!obj_line.get("OP.02").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<SUBSIDIARY_INDICATOR>"+obj_line.get("OP.02")+"</SUBSIDIARY_INDICATOR>"; 
	    }
	    if(obj_line.get("OP.03")!=null&&!obj_line.get("OP.03").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LANGUAGE>"+obj_line.get("OP.03")+"</LANGUAGE>"; 
	    }	
	    if(obj_line.get("OP.04")!=null&&!obj_line.get("OP.04").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<REGISTER_NUMBER>"+obj_line.get("OP.04")+"</REGISTER_NUMBER>"; 
	    }
	    if(obj_line.get("OP.05")!=null&&!obj_line.get("OP.05").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<REGULATORY_BODY>"+obj_line.get("OP.05")+"</REGULATORY_BODY>"; 
	    }
	    if(obj_line.get("OP.06")!=null&&!obj_line.get("OP.06").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<LEGAL_GUARDIAN>"+obj_line.get("OP.06")+"</LEGAL_GUARDIAN>"; 
	    }
	    if(obj_line.get("OP.07")!=null&&!obj_line.get("OP.07").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<DB_LOCATION>"+obj_line.get("OP.07")+"</DB_LOCATION>"; 
	    }

	    if(	(obj_line.get("OP.08.01")!=null&&!obj_line.get("OP.08.01").trim().equals(""))||
		    (obj_line.get("OP.08.02")!=null&&!obj_line.get("OP.08.02").trim().equals(""))||
		    (obj_line.get("OP.08.03")!=null&&!obj_line.get("OP.08.03").trim().equals(""))|| 
		    (obj_line.get("OP.08.04")!=null&&!obj_line.get("OP.08.04").trim().equals(""))|| 
		    (obj_line.get("OP.08.05")!=null&&!obj_line.get("OP.08.05").trim().equals(""))|| 
		    (obj_line.get("OP.08.06")!=null&&!obj_line.get("OP.08.06").trim().equals(""))|| 
		    (obj_line.get("OP.08.07")!=null&&!obj_line.get("OP.08.07").trim().equals(""))|| 
		    (obj_line.get("OP.08.08")!=null&&!obj_line.get("OP.08.08").trim().equals(""))|| 
		    (obj_line.get("OP.08.09")!=null&&!obj_line.get("OP.08.09").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIERS>"; 

		if(obj_line.get("OP.08.01")!=null&&!obj_line.get("OP.08.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DELTAID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}		
		if(obj_line.get("OP.08.02")!=null&&!obj_line.get("OP.08.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SIREID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.02")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		if(obj_line.get("OP.08.03")!=null&&!obj_line.get("OP.08.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>TLID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	

		if(obj_line.get("OP.08.04")!=null&&!obj_line.get("OP.08.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>BDIID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}
		if(obj_line.get("OP.08.05")!=null&&!obj_line.get("OP.08.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>BOTID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}		
		if(obj_line.get("OP.08.06")!=null&&!obj_line.get("OP.08.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.06")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		if(obj_line.get("OP.08.07")!=null&&!obj_line.get("OP.08.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SAITID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.07")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}	
		if(obj_line.get("OP.08.08")!=null&&!obj_line.get("OP.08.08").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MGCGLOID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.08")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}
		if(obj_line.get("OP.08.09")!=null&&!obj_line.get("OP.08.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CODTESID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.09")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}
		if(obj_line.get("OP.08.10")!=null&&!obj_line.get("OP.08.10").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTITY_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>QNUTESID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.08.10")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIER>";
		}
		XML_SALIDA=XML_SALIDA+"</ENTITY_IDENTIFIERS>"; 
	    }	

	    if(obj_line.get("OP.09")!=null&&!obj_line.get("OP.09").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<SHORTNAME>"+obj_line.get("OP.09")+"</SHORTNAME>"; 
	    }
	    if(obj_line.get("OP.10")!=null&&!obj_line.get("OP.10").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PLAZA>"+obj_line.get("OP.10")+"</PLAZA>"; 
	    }
	    if(obj_line.get("OP.11")!=null&&!obj_line.get("OP.11").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<FISCAL_ADRESS>"+obj_line.get("OP.11")+"</FISCAL_ADRESS>"; 
	    }	
	    if(obj_line.get("OP.12")!=null&&!obj_line.get("OP.12").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PROVINCE>"+obj_line.get("OP.12")+"</PROVINCE>"; 
	    }
	    if(obj_line.get("OP.13")!=null&&!obj_line.get("OP.13").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<COUNTRY_OF_RESIDENCE>"+obj_line.get("OP.13")+"</COUNTRY_OF_RESIDENCE>"; 
	    }
	    if(obj_line.get("OP.14")!=null&&!obj_line.get("OP.14").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<CITY_DISTRICT>"+obj_line.get("OP.14")+"</CITY_DISTRICT>"; 
	    }    
		if(obj_line.get("OP.14.01")!=null&&!obj_line.get("OP.14.01").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<POSTAL_CDE>"+obj_line.get("OP.14.01")+"</POSTAL_CDE>"; 
	    } 

	    if(	(obj_line.get("OP.15.01")!=null&&!obj_line.get("OP.15.01").trim().equals(""))||
		    (obj_line.get("OP.15.02")!=null&&!obj_line.get("OP.15.02").trim().equals(""))||
		    (obj_line.get("OP.15.03")!=null&&!obj_line.get("OP.15.03").trim().equals(""))|| 
		    (obj_line.get("OP.15.04")!=null&&!obj_line.get("OP.15.04").trim().equals(""))|| 
		    (obj_line.get("OP.15.05")!=null&&!obj_line.get("OP.15.05").trim().equals(""))|| 
		    (obj_line.get("OP.15.06")!=null&&!obj_line.get("OP.15.06").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTES>"; 

		if(obj_line.get("OP.15.01")!=null&&!obj_line.get("OP.15.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>1145</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>CLS_MEX</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.01")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}		
		if(obj_line.get("OP.15.02")!=null&&!obj_line.get("OP.15.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>1145</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>TYPFOMEX</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.02")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.03")!=null&&!obj_line.get("OP.15.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>1145</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>SALAMEX</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.03")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.04")!=null&&!obj_line.get("OP.15.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>0182</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>CLS_MAD</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.04")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.05")!=null&&!obj_line.get("OP.15.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>0182</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>TYPFOMAD</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.05")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.06")!=null&&!obj_line.get("OP.15.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>0182</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>SALAMAD</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.06")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.07")!=null&&!obj_line.get("OP.15.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>COMP</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>CLS_COM</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.07")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.08")!=null&&!obj_line.get("OP.15.08").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>COMP</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>TYPFOCOM</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.08")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		if(obj_line.get("OP.15.09")!=null&&!obj_line.get("OP.15.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE_LEVEL_ATTRIBUTE>";
		    XML_SALIDA=XML_SALIDA+"<ENTERPRISE>COMP</ENTERPRISE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_SET>SALACOM</CLASSFICATION_SET>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSFICATION_VALUE>"+obj_line.get("OP.15.09")+"</CLASSFICATION_VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTE>";
		}
		XML_SALIDA=XML_SALIDA+"</ENTERPRISE_LEVEL_ATTRIBUTES>"; 
	    }	

	    if(	(obj_line.get("OP.16.01")!=null&&!obj_line.get("OP.16.01").trim().equals(""))||
		    (obj_line.get("OP.16.02")!=null&&!obj_line.get("OP.16.02").trim().equals(""))||
		    (obj_line.get("OP.16.03")!=null&&!obj_line.get("OP.16.03").trim().equals(""))|| 
		    (obj_line.get("OP.16.04")!=null&&!obj_line.get("OP.16.04").trim().equals(""))|| 
		    (obj_line.get("OP.16.05")!=null&&!obj_line.get("OP.16.05").trim().equals(""))|| 
		    (obj_line.get("OP.16.06")!=null&&!obj_line.get("OP.16.06").trim().equals(""))|| 
		    (obj_line.get("OP.16.07")!=null&&!obj_line.get("OP.16.07").trim().equals(""))|| 
		    (obj_line.get("OP.16.08")!=null&&!obj_line.get("OP.16.08").trim().equals(""))|| 
		    (obj_line.get("OP.16.09")!=null&&!obj_line.get("OP.16.09").trim().equals(""))|| 
		    (obj_line.get("OP.16.10")!=null&&!obj_line.get("OP.16.10").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<BRANCHES>"; 

		if(obj_line.get("OP.16.01")!=null&&!obj_line.get("OP.16.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}		
		if(obj_line.get("OP.16.02")!=null&&!obj_line.get("OP.16.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.03")!=null&&!obj_line.get("OP.16.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}

		if(obj_line.get("OP.16.04")!=null&&!obj_line.get("OP.16.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.05")!=null&&!obj_line.get("OP.16.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.06")!=null&&!obj_line.get("OP.16.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.06")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.07")!=null&&!obj_line.get("OP.16.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.07")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.08")!=null&&!obj_line.get("OP.16.08").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.08")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.09")!=null&&!obj_line.get("OP.16.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>";
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.09")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		if(obj_line.get("OP.16.10")!=null&&!obj_line.get("OP.16.10").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<BRANCH>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.16.10")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</BRANCH>";
		}
		XML_SALIDA=XML_SALIDA+"</BRANCHES>"; 
	    }	

	    XML_SALIDA=XML_SALIDA+"<OFFICES>";

	    for (int k =1 ; k<=10; k++){
		String at1="";   String at2="";    String at3="";    String at4="";    String at5="";    String at6="";
		if(k<10){
		    at1="OP.17.0"+k;    at2="OP.18.0"+k;    at3="OP.19.0"+k;
		    at4="OP.20.0"+k;    at5="OP.21.0"+k;    at6="OP.22.0"+k;
		}else{
		    at1="OP.17."+k;    at2="OP.18."+k;	    at3="OP.19."+k;
		    at4="OP.20."+k;    at5="OP.21."+k;	    at6="OP.22."+k;
		}

		if(	(obj_line.get(at1)!=null&&!obj_line.get(at1).trim().equals(""))||
			(obj_line.get(at2)!=null&&!obj_line.get(at2).trim().equals(""))||
			(obj_line.get(at3)!=null&&!obj_line.get(at3).trim().equals(""))){

		    XML_SALIDA=XML_SALIDA+"<OFFICE>"; 

		    if(obj_line.get(at1)!=null&&!obj_line.get(at1).trim().equals("")){  
			XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get(at1)+"</BRANCH>"; 
		    }		
		    if(obj_line.get(at2)!=null&&!obj_line.get(at2).trim().equals("")){  
			XML_SALIDA=XML_SALIDA+"<IDENTIFIER>"+obj_line.get(at2)+"</IDENTIFIER>"; 
		    }	
		    if(obj_line.get(at3)!=null&&!obj_line.get(at3).trim().equals("")){  
			XML_SALIDA=XML_SALIDA+"<ECONOMICAL_ACTIVITY_CODE>"+obj_line.get(at3)+"</ECONOMICAL_ACTIVITY_CODE>"; 
		    }	

		    if( (obj_line.get(at4)!=null&&!obj_line.get(at4).trim().equals(""))|| 
			    (obj_line.get(at5)!=null&&!obj_line.get(at5).trim().equals(""))|| 
			    (obj_line.get(at6)!=null&&!obj_line.get(at6).trim().equals(""))){
			XML_SALIDA=XML_SALIDA+"<OFFICE_ATTRIBUTES>"; 

			if(obj_line.get(at4)!=null&&!obj_line.get(at4).trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<OFFICE_ATTRIBUTE>"; 
			    XML_SALIDA=XML_SALIDA+"<TYPE>RESIDENT</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get(at4)+"</VALUE>";
			    XML_SALIDA=XML_SALIDA+"</OFFICE_ATTRIBUTE>"; 
			}	
			if(obj_line.get(at5)!=null&&!obj_line.get(at5).trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<OFFICE_ATTRIBUTE>"; 
			    XML_SALIDA=XML_SALIDA+"<TYPE>ACC_TYPE</TYPE>";  
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get(at5)+"</VALUE>";
			    XML_SALIDA=XML_SALIDA+"</OFFICE_ATTRIBUTE>"; 
			}		
			if(obj_line.get(at6)!=null&&!obj_line.get(at6).trim().equals("")){  
			    XML_SALIDA=XML_SALIDA+"<OFFICE_ATTRIBUTE>"; 
			    XML_SALIDA=XML_SALIDA+"<TYPE>BDE_CODE</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get(at6)+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OFFICE_ATTRIBUTE>"; 
			}
			XML_SALIDA=XML_SALIDA+"</OFFICE_ATTRIBUTES>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"</OFFICE>"; 
		}	
	    }
	    XML_SALIDA=XML_SALIDA+"</OFFICES>";


	    if(	(obj_line.get("OP.23.01")!=null&&!obj_line.get("OP.23.01").trim().equals(""))||
		    (obj_line.get("OP.23.02")!=null&&!obj_line.get("OP.23.02").trim().equals(""))||
		    (obj_line.get("OP.23.03")!=null&&!obj_line.get("OP.23.03").trim().equals(""))|| 
		    (obj_line.get("OP.23.04")!=null&&!obj_line.get("OP.23.04").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<SECTORIZATIONS>"; 

		if(obj_line.get("OP.23.01")!=null&&!obj_line.get("OP.23.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<SECTORIZATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>=000004A6E</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.23.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</SECTORIZATION>";
		}		
		if(obj_line.get("OP.23.02")!=null&&!obj_line.get("OP.23.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<SECTORIZATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ACCTYPEM</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.23.02")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</SECTORIZATION>";
		}	
		if(obj_line.get("OP.23.03")!=null&&!obj_line.get("OP.23.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<SECTORIZATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SECOBA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.23.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</SECTORIZATION>";
		}	
		if(obj_line.get("OP.23.04")!=null&&!obj_line.get("OP.23.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<SECTORIZATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CODINSTI</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.23.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</SECTORIZATION>";
		}
		XML_SALIDA=XML_SALIDA+"</SECTORIZATIONS>"; 
	    }	

	    if(	(obj_line.get("OP.24.01")!=null&&!obj_line.get("OP.24.01").trim().equals(""))||
		    (obj_line.get("OP.24.02")!=null&&!obj_line.get("OP.24.02").trim().equals(""))||
		    (obj_line.get("OP.24.03")!=null&&!obj_line.get("OP.24.03").trim().equals(""))|| 
		    (obj_line.get("OP.24.04")!=null&&!obj_line.get("OP.24.04").trim().equals(""))||
		    (obj_line.get("OP.24.05")!=null&&!obj_line.get("OP.24.05").trim().equals(""))||
		    (obj_line.get("OP.24.06")!=null&&!obj_line.get("OP.24.06").trim().equals(""))|| 
		    (obj_line.get("OP.24.07")!=null&&!obj_line.get("OP.24.07").trim().equals(""))||
		    (obj_line.get("OP.24.08")!=null&&!obj_line.get("OP.24.08").trim().equals(""))||
		    (obj_line.get("OP.24.09")!=null&&!obj_line.get("OP.24.09").trim().equals(""))|| 
		    (obj_line.get("OP.24.10")!=null&&!obj_line.get("OP.24.10").trim().equals(""))||
		    (obj_line.get("OP.24.11")!=null&&!obj_line.get("OP.24.11").trim().equals(""))||
		    (obj_line.get("OP.24.12")!=null&&!obj_line.get("OP.24.12").trim().equals(""))|| 
		    (obj_line.get("OP.24.13")!=null&&!obj_line.get("OP.24.13").trim().equals(""))||
		    (obj_line.get("OP.24.14")!=null&&!obj_line.get("OP.24.14").trim().equals(""))||
		    (obj_line.get("OP.24.15")!=null&&!obj_line.get("OP.24.15").trim().equals(""))|| 
		    (obj_line.get("OP.24.16")!=null&&!obj_line.get("OP.24.16").trim().equals(""))||
		    (obj_line.get("OP.24.17")!=null&&!obj_line.get("OP.24.17").trim().equals(""))||
		    (obj_line.get("OP.24.18")!=null&&!obj_line.get("OP.24.18").trim().equals(""))|| 
		    (obj_line.get("OP.24.19")!=null&&!obj_line.get("OP.24.19").trim().equals(""))||
		    (obj_line.get("OP.24.20")!=null&&!obj_line.get("OP.24.20").trim().equals(""))||
		    (obj_line.get("OP.24.21")!=null&&!obj_line.get("OP.24.21").trim().equals(""))|| 
		    (obj_line.get("OP.24.22")!=null&&!obj_line.get("OP.24.22").trim().equals(""))||
		    (obj_line.get("OP.24.23")!=null&&!obj_line.get("OP.24.23").trim().equals(""))||
		    (obj_line.get("OP.24.24")!=null&&!obj_line.get("OP.24.24").trim().equals(""))|| 
		    (obj_line.get("OP.24.25")!=null&&!obj_line.get("OP.24.25").trim().equals(""))||
		    (obj_line.get("OP.24.26")!=null&&!obj_line.get("OP.24.26").trim().equals(""))|| 
		    (obj_line.get("OP.24.27")!=null&&!obj_line.get("OP.24.27").trim().equals(""))||
		    (obj_line.get("OP.24.28")!=null&&!obj_line.get("OP.24.28").trim().equals(""))||
		    (obj_line.get("OP.24.29")!=null&&!obj_line.get("OP.24.29").trim().equals(""))|| 
		    (obj_line.get("OP.24.30")!=null&&!obj_line.get("OP.24.30").trim().equals(""))||
		    (obj_line.get("OP.24.31")!=null&&!obj_line.get("OP.24.31").trim().equals(""))||
		    (obj_line.get("OP.24.32")!=null&&!obj_line.get("OP.24.32").trim().equals(""))|| 
		    (obj_line.get("OP.24.33")!=null&&!obj_line.get("OP.24.33").trim().equals(""))||
		    (obj_line.get("OP.24.34")!=null&&!obj_line.get("OP.24.34").trim().equals(""))||
		    (obj_line.get("OP.24.35")!=null&&!obj_line.get("OP.24.35").trim().equals(""))|| 
		    (obj_line.get("OP.24.36")!=null&&!obj_line.get("OP.24.36").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIERS>"; 

		if(obj_line.get("OP.24.01")!=null&&!obj_line.get("OP.24.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ABA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.01")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"<ID>ABABIC</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.02")!=null&&!obj_line.get("OP.24.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ALERT</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>ALERTID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.03")!=null&&!obj_line.get("OP.24.03").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>AL_MADRID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.03")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>ALID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
				
		if(obj_line.get("OP.24.04")!=null&&!obj_line.get("OP.24.04").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.04")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>ASSET_CONTROL_ID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}		
		if(obj_line.get("OP.24.05")!=null&&!obj_line.get("OP.24.05").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CALYPSO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.05")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CALYPSOID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.06")!=null&&!obj_line.get("OP.24.06").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CALYPSO_FO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.06")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CALYPSO_FOID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.07")!=null&&!obj_line.get("OP.24.07").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CALYPSO_MEX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.07")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"<ID>CALYPSO_MEXID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.08")!=null&&!obj_line.get("OP.24.08").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.08")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CLAVEPIZ</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.09")!=null&&!obj_line.get("OP.24.09").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CO_MEXICO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.09")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>COID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.10")!=null&&!obj_line.get("OP.24.10").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CO_MADRID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.10")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>COID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		
		if(obj_line.get("OP.24.11")!=null&&!obj_line.get("OP.24.11").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CREDITREPO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.11")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CREDITREPOID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.12")!=null&&!obj_line.get("OP.24.12").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>CTM</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.12")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CTMID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.13")!=null&&!obj_line.get("OP.24.13").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.13")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>CVESPEI</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.14")!=null&&!obj_line.get("OP.24.14").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DTCC</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.14")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>DTCCID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.15")!=null&&!obj_line.get("OP.24.15").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>EFX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.15")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>EFXID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.16")!=null&&!obj_line.get("OP.24.16").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FENERGO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.16")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>FENERGO</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.17")!=null&&!obj_line.get("OP.24.17").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>FXALL</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.17")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>FXALLBIC</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.18")!=null&&!obj_line.get("OP.24.18").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MARKIT_WIRE</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.18")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MARKITBIC</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}

		if(obj_line.get("OP.24.19")!=null&&!obj_line.get("OP.24.19").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_MEXICO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.19")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.20")!=null&&!obj_line.get("OP.24.20").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_MILAN</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.20")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.21")!=null&&!obj_line.get("OP.24.21").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_NY</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.21")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.22")!=null&&!obj_line.get("OP.24.22").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_LONDRES</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.22")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.23")!=null&&!obj_line.get("OP.24.23").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_PARIS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.23")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}		
		
		if(obj_line.get("OP.24.24")!=null&&!obj_line.get("OP.24.24").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_MADRID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.24")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.25")!=null&&!obj_line.get("OP.24.25").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MIDAS_FRANKFURT</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.25")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MIDASID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		
		if(obj_line.get("OP.24.26")!=null&&!obj_line.get("OP.24.26").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MISYS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.26")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MISYSBIC</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.27")!=null&&!obj_line.get("OP.24.27").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MUREX</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.27")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>MUREXID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.28")!=null&&!obj_line.get("OP.24.28").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ORBIS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.28")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>ORBIS</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.29")!=null&&!obj_line.get("OP.24.29").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>PHASE3</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.29")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>PHASE3ID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.30")!=null&&!obj_line.get("OP.24.30").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>QRM</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.30")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>QRMID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}	
		if(obj_line.get("OP.24.31")!=null&&!obj_line.get("OP.24.31").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>MARKIT_WIRE</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.31")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>RED</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.32")!=null&&!obj_line.get("OP.24.32").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RET</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.32")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>RETID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.33")!=null&&!obj_line.get("OP.24.33").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RIMS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.33")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>RIMS</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}			
		if(obj_line.get("OP.24.34")!=null&&!obj_line.get("OP.24.34").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.34")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SEGCLIE</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.35")!=null&&!obj_line.get("OP.24.35").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.35")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SHTNMEID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.36")!=null&&!obj_line.get("OP.24.36").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.36")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";  
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.37")!=null&&!obj_line.get("OP.24.37").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.37")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";  
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.38")!=null&&!obj_line.get("OP.24.38").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SUMMIT</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.38")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SUMMITID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.39")!=null&&!obj_line.get("OP.24.39").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.39")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SWIFTCON</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.40")!=null&&!obj_line.get("OP.24.40").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>SWIFT</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.40")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SWIFTID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.41")!=null&&!obj_line.get("OP.24.41").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.41")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>SWIFTLIQ</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.42")!=null&&!obj_line.get("OP.24.42").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>TOMS</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.42")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>TOMSID</ID>"; 
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		if(obj_line.get("OP.24.43")!=null&&!obj_line.get("OP.24.43").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<COUNTERPARTY_ROLE_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>RDR</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.24.43")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"<ID>VAXID</ID>";
		    XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIER>";
		}
		XML_SALIDA=XML_SALIDA+"</COUNTERPARTY_ROLE_IDENTIFIERS>"; 
	    }


	    if((obj_line.get("OP.25.01")!=null&&!obj_line.get("OP.25.01").trim().equals(""))||
		    (obj_line.get("OP.25.02")!=null&&!obj_line.get("OP.25.02").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIERS>"; 

		if(obj_line.get("OP.25.01")!=null&&!obj_line.get("OP.25.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.25.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
		}		
		if(obj_line.get("OP.25.02")!=null&&!obj_line.get("OP.25.02").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.25.02")+"</VALUE>";
		    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
		}	
		XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIERS>"; 
	    }	

	    if((obj_line.get("OP.26")!=null&&!obj_line.get("OP.26").trim().equals(""))||
		    (obj_line.get("OP.27")!=null&&!obj_line.get("OP.27").trim().equals(""))||
		    (obj_line.get("OP.28")!=null&&!obj_line.get("OP.28").trim().equals(""))||
		    (obj_line.get("OP.29")!=null&&!obj_line.get("OP.29").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<RELATED_FUNDS>"; 

		if(obj_line.get("OP.26")!=null&&!obj_line.get("OP.26").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<FUND_MANAGER>"+obj_line.get("OP.26")+"</FUND_MANAGER>"; 
		}	
		if((obj_line.get("OP.27")!=null&&!obj_line.get("OP.27").trim().equals(""))||
			(obj_line.get("OP.28")!=null&&!obj_line.get("OP.28").trim().equals(""))||
			(obj_line.get("OP.29")!=null&&!obj_line.get("OP.29").trim().equals(""))){

		    XML_SALIDA=XML_SALIDA+"<FUND>"; 

		    if(obj_line.get("OP.27")!=null&&!obj_line.get("OP.27").trim().equals("")){  
			XML_SALIDA=XML_SALIDA+"<IDENTIFIER>"+obj_line.get("OP.27")+"</IDENTIFIER>";
		    }
		    if(obj_line.get("OP.28")!=null&&!obj_line.get("OP.28").trim().equals("")){   
			XML_SALIDA=XML_SALIDA+"<DESCRIPTION>"+obj_line.get("OP.28")+"</DESCRIPTION>";
		    }
		    if(obj_line.get("OP.29")!=null&&!obj_line.get("OP.29").trim().equals("")){  
			XML_SALIDA=XML_SALIDA+"<HEREDA>"+obj_line.get("OP.29")+"</HEREDA>";
		    }
		    XML_SALIDA=XML_SALIDA+"</FUND>"; 
		}
		XML_SALIDA=XML_SALIDA+"</RELATED_FUNDS>"; 
	    }
	    if(	(obj_line.get("OP.30.01.01")!=null&&!obj_line.get("OP.30.01.01").trim().equals(""))||
		    (obj_line.get("OP.30.01.02")!=null&&!obj_line.get("OP.30.01.02").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<REGULATORY_INFORMATION>"; 

		if(obj_line.get("OP.30.01.01")!=null&&!obj_line.get("OP.30.01.01").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>CORPREL</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.30.01.01")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}		
		if(obj_line.get("OP.30.01.02")!=null&&!obj_line.get("OP.30.01.02").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<REGULATION>";
		    XML_SALIDA=XML_SALIDA+"<TYPE>DFA</TYPE>"; 
		    XML_SALIDA=XML_SALIDA+"<CLASSIFICATION>DFACAT</CLASSIFICATION>"; 
		    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.30.01.02")+"</VALUE>"; 
		    XML_SALIDA=XML_SALIDA+"</REGULATION>";
		}			
		XML_SALIDA=XML_SALIDA+"</REGULATORY_INFORMATION>"; 
	    }	    
	    if(obj_line.get("OP.31")!=null&&!obj_line.get("OP.31").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<PARENT_COMPANY_COUNTRY_OF_RESIDENCE>"+obj_line.get("OP.31")+"</PARENT_COMPANY_COUNTRY_OF_RESIDENCE>"; 
	    }

	    if(	(obj_line.get("OP.32")!=null&&!obj_line.get("OP.32").trim().equals(""))||
		    (obj_line.get("OP.33")!=null&&!obj_line.get("OP.33").trim().equals(""))||
		    (obj_line.get("OP.34")!=null&&!obj_line.get("OP.34").trim().equals(""))){

		XML_SALIDA=XML_SALIDA+"<LOCK_INFORMATION><LOCK>"; 

		if(obj_line.get("OP.32")!=null&&!obj_line.get("OP.32").trim().equals("")){  
		    XML_SALIDA=XML_SALIDA+"<STATUS>"+obj_line.get("OP.32")+"</STATUS>"; 
		}		
		if(obj_line.get("OP.33")!=null&&!obj_line.get("OP.33").trim().equals("")){ 
		    XML_SALIDA=XML_SALIDA+"<AUTOMATIC_MANUAL>"+obj_line.get("OP.33")+"</AUTOMATIC_MANUAL>"; 
		}			
		if(obj_line.get("OP.34")!=null&&!obj_line.get("OP.34").trim().equals("")){ 
		    
		    String cad1=obj_line.get("OP.34");
		    String cad2="";
			SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy");
			SimpleDateFormat formato2 = new SimpleDateFormat("yyyyMMdd");
			Date fechaDate = null;
			try {
		    		fechaDate = formato1.parse(cad1);	    
		    		cad2 =formato2.format(fechaDate);	        
			} 
			catch (ParseException ex)   {
		    		Date fecha = new Date();
		    		cad2 = formato2.format(fecha);
			}
		    
		    XML_SALIDA=XML_SALIDA+"<DATE>"+cad2+"</DATE>";  ///////************** Fecha
		}	
		XML_SALIDA=XML_SALIDA+"</LOCK></LOCK_INFORMATION>"; 
	    }	
	    if(	(obj_line.get("OP.35.01")!=null&&!obj_line.get("OP.35.01").trim().equals(""))||
		    (obj_line.get("OP.35.02")!=null&&!obj_line.get("OP.35.02").trim().equals(""))||
		    (obj_line.get("OP.35.03")!=null&&!obj_line.get("OP.35.03").trim().equals(""))||
		    (obj_line.get("OP.35.04")!=null&&!obj_line.get("OP.35.04").trim().equals(""))||
		    (obj_line.get("OP.35.05")!=null&&!obj_line.get("OP.35.05").trim().equals(""))||
		    (obj_line.get("OP.35.06")!=null&&!obj_line.get("OP.35.06").trim().equals(""))||
		    (obj_line.get("OP.35.07")!=null&&!obj_line.get("OP.35.07").trim().equals(""))||
		    (obj_line.get("OP.35.08")!=null&&!obj_line.get("OP.35.08").trim().equals(""))||
		    (obj_line.get("OP.35.09")!=null&&!obj_line.get("OP.35.09").trim().equals(""))||
		    (obj_line.get("OP.36.01")!=null&&!obj_line.get("OP.36.01").trim().equals(""))||
		    (obj_line.get("OP.36.02")!=null&&!obj_line.get("OP.36.02").trim().equals(""))||
		    (obj_line.get("OP.36.03")!=null&&!obj_line.get("OP.36.03").trim().equals(""))||
		    (obj_line.get("OP.36.04")!=null&&!obj_line.get("OP.36.04").trim().equals(""))||
		    (obj_line.get("OP.36.05")!=null&&!obj_line.get("OP.36.05").trim().equals(""))||
		    (obj_line.get("OP.36.06")!=null&&!obj_line.get("OP.36.06").trim().equals(""))||
		    (obj_line.get("OP.36.07")!=null&&!obj_line.get("OP.36.07").trim().equals(""))||
		    (obj_line.get("OP.36.08")!=null&&!obj_line.get("OP.36.08").trim().equals(""))||
		    (obj_line.get("OP.37")!=null&&!obj_line.get("OP.37").trim().equals(""))	 ||
		    (obj_line.get("OP.38.01")!=null&&!obj_line.get("OP.38.01").trim().equals(""))||
		    (obj_line.get("OP.38.02")!=null&&!obj_line.get("OP.38.02").trim().equals(""))||
		    (obj_line.get("OP.38.03")!=null&&!obj_line.get("OP.38.03").trim().equals(""))||
		    (obj_line.get("OP.38.04")!=null&&!obj_line.get("OP.38.04").trim().equals(""))||
		    (obj_line.get("OP.38.05")!=null&&!obj_line.get("OP.38.05").trim().equals(""))||
		    (obj_line.get("OP.38.06")!=null&&!obj_line.get("OP.38.06").trim().equals(""))||
		    (obj_line.get("OP.38.07")!=null&&!obj_line.get("OP.38.07").trim().equals(""))||
		    (obj_line.get("OP.38.08")!=null&&!obj_line.get("OP.38.08").trim().equals(""))||
		    (obj_line.get("OP.38.09")!=null&&!obj_line.get("OP.38.09").trim().equals(""))||		    
		    (obj_line.get("OP.39.01")!=null&&!obj_line.get("OP.39.01").trim().equals(""))||
		    (obj_line.get("OP.39.02")!=null&&!obj_line.get("OP.39.02").trim().equals(""))||
		    (obj_line.get("OP.39.03")!=null&&!obj_line.get("OP.39.03").trim().equals(""))||
		    (obj_line.get("OP.39.04")!=null&&!obj_line.get("OP.39.04").trim().equals(""))||
		    (obj_line.get("OP.39.05")!=null&&!obj_line.get("OP.39.05").trim().equals(""))||
		    (obj_line.get("OP.39.06")!=null&&!obj_line.get("OP.39.06").trim().equals(""))||
		    (obj_line.get("OP.39.07")!=null&&!obj_line.get("OP.39.07").trim().equals(""))||
		    (obj_line.get("OP.39.08")!=null&&!obj_line.get("OP.39.08").trim().equals(""))||
		    (obj_line.get("OP.39.09")!=null&&!obj_line.get("OP.39.09").trim().equals(""))||
		    (obj_line.get("OP.39.10")!=null&&!obj_line.get("OP.39.10").trim().equals(""))){
		XML_SALIDA=XML_SALIDA+"<OTHER_ROLES>";

		if(	(obj_line.get("OP.35.01")!=null&&!obj_line.get("OP.35.01").trim().equals(""))||
			(obj_line.get("OP.35.02")!=null&&!obj_line.get("OP.35.02").trim().equals(""))||
			(obj_line.get("OP.35.03")!=null&&!obj_line.get("OP.35.03").trim().equals(""))||
			(obj_line.get("OP.35.04")!=null&&!obj_line.get("OP.35.04").trim().equals(""))||
			(obj_line.get("OP.35.05")!=null&&!obj_line.get("OP.35.05").trim().equals(""))||
			(obj_line.get("OP.35.06")!=null&&!obj_line.get("OP.35.06").trim().equals(""))||
			(obj_line.get("OP.35.07")!=null&&!obj_line.get("OP.35.07").trim().equals(""))||
			(obj_line.get("OP.35.08")!=null&&!obj_line.get("OP.35.08").trim().equals(""))||
			(obj_line.get("OP.35.09")!=null&&!obj_line.get("OP.35.09").trim().equals(""))){

		    if((obj_line.get("OP.35.01")!=null&&!obj_line.get("OP.35.01").trim().equals(""))||
			    (obj_line.get("OP.35.02")!=null&&!obj_line.get("OP.35.02").trim().equals(""))||
			    (obj_line.get("OP.35.03")!=null&&!obj_line.get("OP.35.03").trim().equals(""))||
			    (obj_line.get("OP.35.04")!=null&&!obj_line.get("OP.35.04").trim().equals(""))){

			XML_SALIDA=XML_SALIDA+"<OTHER_ROLE>";
			XML_SALIDA=XML_SALIDA+"<ROLE>BROKER</ROLE>";
			if(obj_line.get("OP.35.04")!=null&&!obj_line.get("OP.35.04").trim().trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<SUBTYP>"+obj_line.get("OP.35.04")+"</SUBTYP>"; 
			}
			if(obj_line.get("OP.35.02")!=null&&!obj_line.get("OP.35.02").trim().equals("")){  
			    XML_SALIDA=XML_SALIDA+"<NAME>"+obj_line.get("OP.35.02")+"</NAME>"; 
			}		
			if(obj_line.get("OP.35.03")!=null&&!obj_line.get("OP.35.03").trim().trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<ID>"+obj_line.get("OP.35.03")+"</ID>"; 
			}		
			
			//XML_SALIDA=XML_SALIDA+"<TYPE>TYPE</TYPE>";     ////////////////////////
			
			if(obj_line.get("OP.35.01")!=null&&!obj_line.get("OP.35.01").trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get("OP.35.01")+"</BRANCH>"; 
			}
			if((obj_line.get("OP.35.05")!=null&&!obj_line.get("OP.35.05").trim().equals(""))||
				(obj_line.get("OP.35.06")!=null&&!obj_line.get("OP.35.06").trim().equals(""))||
				(obj_line.get("OP.35.07")!=null&&!obj_line.get("OP.35.07").trim().equals(""))){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIERS>"; 
			    if(obj_line.get("OP.35.05")!=null&&!obj_line.get("OP.35.05").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>MUREXID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>MUREX</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.35.05")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.35.06")!=null&&!obj_line.get("OP.35.06").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.35.06")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.35.07")!=null&&!obj_line.get("OP.35.07").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.35.07")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIERS>"; 
			}
			if((obj_line.get("OP.35.08")!=null&&!obj_line.get("OP.35.08").trim().equals(""))||
				(obj_line.get("OP.35.09")!=null&&!obj_line.get("OP.35.09").trim().equals(""))){

			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIERS>"; 
			    if(obj_line.get("OP.35.08")!=null&&!obj_line.get("OP.35.08").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.35.08")+"</VALUE>";
				XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.35.09")!=null&&!obj_line.get("OP.35.09").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.35.09")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			    }
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIERS>"; 
			}
			XML_SALIDA=XML_SALIDA+"</OTHER_ROLE>";
		    }
		}	    
		if((obj_line.get("OP.36.01")!=null&&!obj_line.get("OP.36.01").trim().equals(""))||
			(obj_line.get("OP.36.02")!=null&&!obj_line.get("OP.36.02").trim().equals(""))||
			(obj_line.get("OP.36.03")!=null&&!obj_line.get("OP.36.03").trim().equals(""))||
			(obj_line.get("OP.36.04")!=null&&!obj_line.get("OP.36.04").trim().equals(""))||
			(obj_line.get("OP.36.05")!=null&&!obj_line.get("OP.36.05").trim().equals(""))||
			(obj_line.get("OP.36.06")!=null&&!obj_line.get("OP.36.06").trim().equals(""))||
			(obj_line.get("OP.36.07")!=null&&!obj_line.get("OP.36.07").trim().equals(""))||
			(obj_line.get("OP.36.08")!=null&&!obj_line.get("OP.36.08").trim().equals(""))){
		    if((obj_line.get("OP.36.01")!=null&&!obj_line.get("OP.36.01").trim().equals(""))||
			    (obj_line.get("OP.36.02")!=null&&!obj_line.get("OP.36.02").trim().equals(""))||
			    (obj_line.get("OP.36.03")!=null&&!obj_line.get("OP.36.03").trim().equals(""))){

			XML_SALIDA=XML_SALIDA+"<OTHER_ROLE>";
			XML_SALIDA=XML_SALIDA+"<ROLE>ISSUER</ROLE>";

			if(obj_line.get("OP.36.02")!=null&&!obj_line.get("OP.36.02").trim().equals("")){  
			    XML_SALIDA=XML_SALIDA+"<TYPE>"+obj_line.get("OP.36.02")+"</TYPE>"; 
			}
			if(obj_line.get("OP.36.01")!=null&&!obj_line.get("OP.36.01").trim().equals("")){ 
			    XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get("OP.36.01")+"</BRANCH>"; 
			}			
			
			if((obj_line.get("OP.36.03")!=null&&!obj_line.get("OP.36.03").trim().equals(""))||
				(obj_line.get("OP.36.06")!=null&&!obj_line.get("OP.36.06").trim().equals(""))||
				(obj_line.get("OP.36.07")!=null&&!obj_line.get("OP.36.07").trim().equals(""))||
				(obj_line.get("OP.36.08")!=null&&!obj_line.get("OP.36.08").trim().equals(""))){

			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIERS>"; 
			    if(obj_line.get("OP.36.03")!=null&&!obj_line.get("OP.36.03").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>MUREXID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>MUREX</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.03")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.36.06")!=null&&!obj_line.get("OP.36.06").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>BBGCID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>BB</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.06")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.36.07")!=null&&!obj_line.get("OP.36.07").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.07")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.36.08")!=null&&!obj_line.get("OP.36.08").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.08")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			    }
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIERS>"; 
			}

			if(	 (obj_line.get("OP.36.04")!=null&&!obj_line.get("OP.36.04").trim().equals(""))||
				(obj_line.get("OP.36.05")!=null&&!obj_line.get("OP.36.05").trim().equals(""))){

			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIERS>"; 
			    if(obj_line.get("OP.36.04")!=null&&!obj_line.get("OP.36.04").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.04")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			    }
			    if(obj_line.get("OP.36.05")!=null&&!obj_line.get("OP.36.05").trim().equals("")){
				XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
				XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
				XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.36.05")+"</VALUE>"; 
				XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			    }
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIERS>"; 
			}
			XML_SALIDA=XML_SALIDA+"</OTHER_ROLE>";
		    }
		}	
		if(	(obj_line.get("OP.37")!=null&&!obj_line.get("OP.37").trim().equals(""))){
		    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE>";
		    XML_SALIDA=XML_SALIDA+"<ROLE>CLRNGHS</ROLE>";
		    XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get("OP.37")+"</BRANCH>"; 
		    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE>";

		}							

		if((obj_line.get("OP.38.01")!=null&&!obj_line.get("OP.38.01").trim().equals(""))||
			(obj_line.get("OP.38.02")!=null&&!obj_line.get("OP.38.02").trim().equals(""))||
			(obj_line.get("OP.38.03")!=null&&!obj_line.get("OP.38.03").trim().equals(""))||
			(obj_line.get("OP.38.04")!=null&&!obj_line.get("OP.38.04").trim().equals(""))||
			(obj_line.get("OP.38.05")!=null&&!obj_line.get("OP.38.05").trim().equals(""))||
			(obj_line.get("OP.38.06")!=null&&!obj_line.get("OP.38.06").trim().equals(""))||
			(obj_line.get("OP.38.07")!=null&&!obj_line.get("OP.38.07").trim().equals(""))||
			(obj_line.get("OP.38.08")!=null&&!obj_line.get("OP.38.08").trim().equals(""))||
			(obj_line.get("OP.38.09")!=null&&!obj_line.get("OP.38.09").trim().equals(""))){

		    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE>";
		    XML_SALIDA=XML_SALIDA+"<ROLE>BNFCARY</ROLE>";

		    if(obj_line.get("OP.38.01")!=null&&!obj_line.get("OP.38.01").trim().equals("")){ 
			XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get("OP.38.01")+"</BRANCH>"; 
		    }  
		    if(	 (obj_line.get("OP.38.02")!=null&&!obj_line.get("OP.38.02").trim().equals(""))||
			    (obj_line.get("OP.38.03")!=null&&!obj_line.get("OP.38.03").trim().equals(""))||
			    (obj_line.get("OP.38.04")!=null&&!obj_line.get("OP.38.04").trim().equals(""))||
			    (obj_line.get("OP.38.05")!=null&&!obj_line.get("OP.38.05").trim().equals(""))||
			    (obj_line.get("OP.38.06")!=null&&!obj_line.get("OP.38.06").trim().equals(""))||
			    (obj_line.get("OP.38.07")!=null&&!obj_line.get("OP.38.07").trim().equals(""))){

			XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIERS>"; 
			if(obj_line.get("OP.38.02")!=null&&!obj_line.get("OP.38.02").trim().equals("")){  
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>MUREXID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>MUREX</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.02")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}	
			if(obj_line.get("OP.38.03")!=null&&!obj_line.get("OP.38.03").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>SWIFT</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.03")+"</VALUE>";
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.38.04")!=null&&!obj_line.get("OP.38.04").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTCON</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.04")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}

			if(obj_line.get("OP.38.05")!=null&&!obj_line.get("OP.38.05").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTLIQ</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.05")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.38.06")!=null&&!obj_line.get("OP.38.06").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.06")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.38.07")!=null&&!obj_line.get("OP.38.07").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.07")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIERS>"; 
		    }
		    if(	(obj_line.get("OP.38.08")!=null&&!obj_line.get("OP.38.08").trim().equals(""))||
			    (obj_line.get("OP.38.09")!=null&&!obj_line.get("OP.38.09").trim().equals(""))){
			XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIERS>"; 
			if(obj_line.get("OP.38.08")!=null&&!obj_line.get("OP.38.08").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.08")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			}
			if(obj_line.get("OP.38.09")!=null&&!obj_line.get("OP.38.09").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.38.09")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			}
			XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIERS>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE>";
		}

		if((obj_line.get("OP.39.01")!=null&&!obj_line.get("OP.39.01").trim().equals(""))||
			(obj_line.get("OP.39.02")!=null&&!obj_line.get("OP.39.02").trim().equals(""))||
			(obj_line.get("OP.39.03")!=null&&!obj_line.get("OP.39.03").trim().equals(""))||
			(obj_line.get("OP.39.04")!=null&&!obj_line.get("OP.39.04").trim().equals(""))||
			(obj_line.get("OP.39.05")!=null&&!obj_line.get("OP.39.05").trim().equals(""))||
			(obj_line.get("OP.39.06")!=null&&!obj_line.get("OP.39.06").trim().equals(""))||
			(obj_line.get("OP.39.07")!=null&&!obj_line.get("OP.39.07").trim().equals(""))||
			(obj_line.get("OP.39.08")!=null&&!obj_line.get("OP.39.08").trim().equals(""))||
			(obj_line.get("OP.39.09")!=null&&!obj_line.get("OP.39.09").trim().equals(""))||
			(obj_line.get("OP.39.10")!=null&&!obj_line.get("OP.39.10").trim().equals(""))){

		    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE>";
		    XML_SALIDA=XML_SALIDA+"<ROLE>CRRSPBNK</ROLE>";
		    if(obj_line.get("OP.39.01")!=null&&!obj_line.get("OP.39.01").trim().equals("")){ 
			XML_SALIDA=XML_SALIDA+"<BRANCH>"+obj_line.get("OP.39.01")+"</BRANCH>"; 
		    }  
		    if(	 (obj_line.get("OP.39.02")!=null&&!obj_line.get("OP.39.02").trim().equals(""))||
			    (obj_line.get("OP.39.03")!=null&&!obj_line.get("OP.39.03").trim().equals(""))||
			    (obj_line.get("OP.39.04")!=null&&!obj_line.get("OP.39.04").trim().equals(""))||
			    (obj_line.get("OP.39.05")!=null&&!obj_line.get("OP.39.05").trim().equals(""))||
			    (obj_line.get("OP.39.06")!=null&&!obj_line.get("OP.39.06").trim().equals(""))||
			    (obj_line.get("OP.39.07")!=null&&!obj_line.get("OP.39.07").trim().equals(""))||
			    (obj_line.get("OP.39.08")!=null&&!obj_line.get("OP.39.08").trim().equals(""))){

			XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIERS>"; 
			if(obj_line.get("OP.39.02")!=null&&!obj_line.get("OP.39.02").trim().equals("")){  
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>MUREXID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>MUREX</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.02")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}	

			if(obj_line.get("OP.39.03")!=null&&!obj_line.get("OP.39.03").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>MISYSBIC</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>MISYS</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.03")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.04")!=null&&!obj_line.get("OP.39.04").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>SWIFT</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.04")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.05")!=null&&!obj_line.get("OP.39.05").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTCON</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.05")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.06")!=null&&!obj_line.get("OP.39.06").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>SWIFTLIQ</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>ABACO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.06")+"</VALUE>";
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.07")!=null&&!obj_line.get("OP.39.07").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.07")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.08")!=null&&!obj_line.get("OP.39.08").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<OTHER_ROLE_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<ID>STARID</ID>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.08")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIER>";
			}
			XML_SALIDA=XML_SALIDA+"</OTHER_ROLE_IDENTIFIERS>"; 
		    }
		    if(	 (obj_line.get("OP.39.09")!=null&&!obj_line.get("OP.39.09").trim().equals(""))||
			    (obj_line.get("OP.39.10")!=null&&!obj_line.get("OP.39.10").trim().equals(""))){

			XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIERS>"; 
			if(obj_line.get("OP.39.09")!=null&&!obj_line.get("OP.39.09").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MADRID</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.09")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			}
			if(obj_line.get("OP.39.10")!=null&&!obj_line.get("OP.39.10").trim().equals("")){
			    XML_SALIDA=XML_SALIDA+"<ALIAS_IDENTIFIER>";
			    XML_SALIDA=XML_SALIDA+"<TYPE>STAR_MEXICO</TYPE>"; 
			    XML_SALIDA=XML_SALIDA+"<VALUE>"+obj_line.get("OP.39.10")+"</VALUE>"; 
			    XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIER>";
			}
			XML_SALIDA=XML_SALIDA+"</ALIAS_IDENTIFIERS>"; 
		    }
		    XML_SALIDA=XML_SALIDA+"</OTHER_ROLE>";
		}
		XML_SALIDA=XML_SALIDA+"</OTHER_ROLES>";
	    }
		
	    // AÑADIDO 20161019 PARA QUE COJA EL TIPO DE OPERATIVA - INICIO
	    if(obj_line.get("OP.40")!=null&&!obj_line.get("OP.40").trim().equals("")){   
		XML_SALIDA=XML_SALIDA+"<OPERATIVE_TYPE>"+obj_line.get("OP.40")+"</OPERATIVE_TYPE>"; 
	    }	
	    // AÑADIDO 20161019 PARA QUE COJA EL TIPO DE OPERATIVA - FIN
	   
	    XML_SALIDA=XML_SALIDA+"</OPERATIVE></OPERATIVES></LOCAL></LOCALS></GLOBAL></GLOBALS></PARTYSETUP>";
	} catch (Exception e) {  e.printStackTrace();	 } 
	return XML_SALIDA;
    }	
}
