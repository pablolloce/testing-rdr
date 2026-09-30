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

import jdbc.ConDB;
import util.Utilidades;

// La lógica de esta clase se basa en leer el fichero, mientras se va leyendo, se va volcando en un array. 
// Se generan sublistas de un rango definido en una variable estática.
// Esta sublista se mana a ejecución con un hilo nuevo.

// Se realiza con Multihilo y Pool de conexiones

public class ConBDI {
    
    public static ArrayList<HashMap<String,String>> arr = new ArrayList<HashMap<String,String>>();
    public static ArrayList<String> noConci = new ArrayList<String>();
    public static ArrayList<String> errorConci = new ArrayList<String>();
    public static Connection obj_con=null;
    public static ConDB obj_ConDB=null;

    static int i=0; 	static int rango=100; 		static int x_ins=0; 		static int y_ins=0;		    
    static int x=0;  	static int y=0;			static int x_err=0;  		static int y_err=0;

    public static String FLD_JOB_ID=null;
    public static String msg_ins="Codigo BDI en RDR que no concilia en BDI";
    public static String msg_err="Codigo BDI en RDR que no es valido";

    public static void main(String[] args)  throws Exception	{
	System.out.println("******************* INICIO CONCILIACION DE BDI *****************************");

	obj_ConDB=new ConDB();		obj_ConDB.ObtenerCredenciales();	obj_con=obj_ConDB.ObtenerConexion();

	ArrayList<String> BDIIdsFic = new ArrayList<String>();
	ArrayList<String> BDIIds = new ArrayList<String>();

	Utilidades ut = new Utilidades();	    ConBDI.FLD_JOB_ID=ut.generateJOBID();

	FileInputStream fis = null;	    InputStreamReader fr = null;	    BufferedReader br = null;	    String linea = "";

	long inicio = obj_ConDB.crearJOB(FLD_JOB_ID,"BDI", obj_con);	    System.out.println("Se crea JOB");

	String FLD_COD_CLINTERN="";		String FLD_DES_NOMCORT1="";		String FLD_DES_NOMCORT2="";
	String FLD_DES_NOMCLINT="";		String FLD_COD_INSTITUC="";		String FLD_DES_CALLE="";
	String FLD_DES_DISPLAZA2="";	String FLD_DES_PROVPAIS="";		String FLD_COD_BANCOTES="";
	String FLD_COD_PLAZATES="";		String FLD_QNU_BIC="";			String FLD_COD_PLAZAINT="";
	String FLD_COD_BROKERWS="";   	String FLD_CDNITR="";			String FLD_COD_CLPANA="";   
	String FLD_COD_CPAISN="";		String FLD_COD_CNAE="";			String FLD_COD_CBANCO="";
	String FLD_XTI_TIPOSBIC="";		String COD_CDIPEX="";

	
	//Lectura del fichero
	File f = new File(args[0]);		System.out.println(args[0]);
	try  {
	    fis = new FileInputStream(f);	    	fr = new InputStreamReader(fis, "ISO-8859-1");	        br = new BufferedReader(fr);

	    System.out.println("Se obtienen codigos de BDI de GS");
	    BDIIds = obj_ConDB.obtenerBDIs(obj_con);

	    final List<Thread> threads_exe      = new ArrayList<Thread>();
	    final List<Thread> threads_ins      = new ArrayList<Thread>();
	    final List<Thread> threads_err      = new ArrayList<Thread>();

	    System.out.println(" - COMIENZA LECTURA DEL FICHERO Y EJECUCION EN BDI - ");
	    if (f.exists()) {        
		br.readLine(); //Lee cabecera
		while((linea=br.readLine())!=null)    {
			
		    try   {		    	
		    	int contador = 0;
				for (int i = 0; i < linea.length(); i++)
					{
					     if (linea.charAt(i) == ';') {
					            contador++;
					     }
					}
				if(linea.contains("\"")){
					while(linea.contains("\"\"")){
						linea = linea.replaceAll("\"\"", "\"");
					}
		    				
					if(linea.contains(";\"")){
						linea= linea.replaceAll(";\"", ";");
					}
					if(linea.contains("\";")){
						linea= linea.replaceAll("\";", ";");
					}
				}
						
			String[] campos=null;		campos=linea.split(";");
			//if (campos.length!=41){   // ANTES SWIFT ISO 20022
			/*if (campos.length!=45){   // NUEVO SWIFT ISO 20022
			    System.out.println(" Fallo en " +campos[2].trim() + " debido a longitud "+ campos.length);
			    errorConci.add(campos[2].trim());
			    continue;
			}*/
			if (contador!=45){   // NUEVO SWIFT ISO 20022
			    System.out.println(" Fallo en " +campos[2].trim() + " debido a longitud "+ campos.length);
			    errorConci.add(campos[2].trim());
			    continue;
			}
			
			i++;
			if (i % 10000 == 0) {   System.out.println("Linea BDI - "+i); }		

			FLD_COD_CLINTERN=campos[0].trim();      FLD_DES_NOMCORT1=campos[1].trim();     FLD_XTI_TIPOSBIC=campos[38].trim();
			FLD_DES_NOMCORT2=campos[2].trim();		FLD_DES_NOMCLINT=campos[3].trim();
			FLD_COD_INSTITUC=campos[4].trim();		FLD_DES_CALLE=campos[22].trim();
			 // FLD_DES_DISPLAZA=campos[23].trim(); Antes SWIFT ISO 20022
			FLD_DES_DISPLAZA2=campos[41].trim(); // NUEVO SWIFT ISO 20022
			FLD_DES_PROVPAIS=campos[24].trim();
			FLD_COD_BANCOTES=campos[10].trim();		FLD_COD_PLAZATES=campos[11].trim();
			FLD_QNU_BIC=campos[17].trim();			FLD_COD_PLAZAINT=campos[8].trim();
			FLD_COD_BROKERWS=campos[28].trim();		FLD_CDNITR=campos[33].trim();   
			FLD_COD_CLPANA=campos[36].trim();		FLD_COD_CPAISN=campos[35].trim();
			FLD_COD_CNAE=campos[37].trim();			FLD_COD_CBANCO=campos[6].trim();
			COD_CDIPEX=campos[42].trim();// NUEVO SWIFT ISO 20022
			

			HashMap<String,String> mapint = new HashMap<String,String>();

			mapint.put("FLD_COD_CLINTERN" , FLD_COD_CLINTERN );		mapint.put("FLD_DES_NOMCORT1" , FLD_DES_NOMCORT1 );
			mapint.put("FLD_DES_NOMCORT2" , FLD_DES_NOMCORT2 );		mapint.put("FLD_DES_NOMCLINT" , FLD_DES_NOMCLINT );
			mapint.put("FLD_COD_INSTITUC" , FLD_COD_INSTITUC );		mapint.put("FLD_DES_CALLE" , FLD_DES_CALLE );
			mapint.put("FLD_DES_DISPLAZA2" , FLD_DES_DISPLAZA2 );	mapint.put("FLD_DES_PROVPAIS" , FLD_DES_PROVPAIS );
			mapint.put("FLD_COD_BANCOTES" , FLD_COD_BANCOTES );		mapint.put("FLD_COD_PLAZATES" , FLD_COD_PLAZATES );
			mapint.put("FLD_QNU_BIC" , FLD_QNU_BIC );			    mapint.put("FLD_COD_PLAZAINT" , FLD_COD_PLAZAINT );
			mapint.put("FLD_COD_BROKERWS" , FLD_COD_BROKERWS );		mapint.put("FLD_CDNITR" , FLD_CDNITR );
			mapint.put("FLD_COD_CLPANA" , FLD_COD_CLPANA );			mapint.put("FLD_COD_CPAISN" , FLD_COD_CPAISN );
			mapint.put("FLD_COD_CNAE" , FLD_COD_CNAE );			    mapint.put("FLD_COD_CBANCO" , FLD_COD_CBANCO );
			mapint.put("FLD_XTI_TIPOSBIC" , FLD_XTI_TIPOSBIC );     mapint.put("FLD_JOB_ID" , FLD_JOB_ID );		
			mapint.put("COD_CDIPEX" , COD_CDIPEX  );// NUEVO SWIFT ISO 20022
			arr.add(mapint);

			//Se crea un sublista del array completo de un tamaño definido por la variable estática rango 
			//la cual se puede modificar en las declaraciones globales. Posible calculo también de otros modos.
			//Se crea un hilo de ejecución para la ejecución del proceso y se le manda la sublista.
			
			if(i%ConBDI.rango==0) { 
			    ConBDI.y=i; ConBDI.x=i-ConBDI.rango;
			    Thread hilo=new Thread( new Runnable() {
				ArrayList<HashMap<String,String>> arr2 = new ArrayList<HashMap<String,String>>(
				ConBDI.arr.subList(ConBDI.x,ConBDI.y));
				public void run() { 
				    obj_ConDB.executeCONBDI_Hilos(arr2, obj_con);
				}
			    });
			    hilo.start();     threads_exe.add(hilo);		        		
			}   

			BDIIdsFic.add(FLD_COD_CLINTERN.toString().trim());

			//Lógica de códigos de BDI que no estén en GS o MGC

		    }catch(ArrayIndexOutOfBoundsException e)  {
			System.out.println("Línea con longitud errónea: " + linea);
		    }
		    catch(Exception ex)  {
			//System.out.println("campos.size() - "+campos.length);
			System.out.println("Excepcion: " + ex+ " - " +linea);
		    }
		}
		
		Thread hilo=new Thread( new Runnable() {
		    ArrayList<HashMap<String,String>> arr2 =new ArrayList<HashMap<String,String>>(
			    ConBDI.arr.subList(ConBDI.y,ConBDI.i));		       
		    public void run() {    
			obj_ConDB.executeCONBDI_Hilos(arr2,  obj_con);
		    }      
		});
		hilo.start();	         threads_exe.add(hilo);
		
		//Esperamos a que finalice los hilos de ejecución
		for( Thread t : threads_exe ) {       t.join();       }
				
		System.out.println(" - COMIENZA COMPARACION EN BDI - ");

		boolean encontrado=false;
		for(String bdi : BDIIds)   {
		    if(BDIIdsFic.contains(bdi))   	{
			encontrado=true;
		    }
		    if(!encontrado)  { 
			ConBDI.noConci.add(bdi);      
		    }
		    encontrado=false;
		}
		/*		
		System.out.println(" - COMIENZA INSERCION DE REGISTROS NO ENCONTRADOS BDI - "+ noConci.size());

		for(int k=0; k<noConci.size(); k++){
		    if((k+1)%ConBDI.rango==0) { 
			ConBDI.y_ins=(k+1); ConBDI.x_ins=(k+1)-ConBDI.rango;
			Thread hilo_ins=new Thread( new Runnable() {
			    ArrayList<String> arr2 = new ArrayList<String>(
				    ConBDI.noConci.subList(ConBDI.x_ins,ConBDI.y_ins));
			    public void run() {  
				obj_ConDB.insertRLT1BDI(arr2, ConBDI.FLD_JOB_ID, obj_con, msg_ins); 
			    }
			});
			hilo_ins.start();       	threads_ins.add(hilo_ins);		        		
		    }  	
		}

		ConBDI.x_ins=ConBDI.y_ins;		ConBDI.y_ins=noConci.size(); 
		Thread hilo_ins=new Thread( new Runnable() {
		    ArrayList<String> arr2 = new ArrayList<String>(
			    ConBDI.noConci.subList(ConBDI.x_ins,ConBDI.y_ins));
		    public void run() { 
			obj_ConDB.insertRLT1BDI(arr2, ConBDI.FLD_JOB_ID, obj_con, msg_ins);
		    }
		});
		hilo_ins.start();       threads_ins.add(hilo_ins);		        		

		//Esperamos a que finalice los hilos de inserción
		for( Thread t : threads_ins ) {       t.join();       }
*/
		System.out.println(" - COMIENZA INSERCION DE REGISTROS ERRONEOS - "+errorConci.size());

		for(int k=0; k<errorConci.size(); k++){
		    if((k+1)%ConBDI.rango==0) { 
			ConBDI.y_err=(k+1); ConBDI.x_err=(k+1)-ConBDI.rango;
			Thread hilo_err=new Thread( new Runnable() {
			    ArrayList<String> arr3 = new ArrayList<String>(
				    ConBDI.errorConci.subList(ConBDI.x_err,ConBDI.y_err));
			    public void run() { 
				obj_ConDB.insertRLT1BDI(arr3, ConBDI.FLD_JOB_ID, obj_con, msg_err);
			    }
			});
			hilo_err.start();       	threads_err.add(hilo_err);		        		
		    }  	
		}

		ConBDI.x_err=ConBDI.y_err;		ConBDI.y_err=errorConci.size(); 
		Thread hilo_err=new Thread( new Runnable() {
		    ArrayList<String> arr3 = new ArrayList<String>(
			    ConBDI.errorConci.subList(ConBDI.x_err,ConBDI.y_err));
		    public void run() { 
			obj_ConDB.insertRLT1BDI(arr3, ConBDI.FLD_JOB_ID, obj_con, msg_err);   
		    }
		});
		hilo_err.start();       threads_err.add(hilo_err);		        		

		//Esperamos a que finalice los hilos de error
		for( Thread t : threads_err ) {       t.join();       }

	    }else {	System.out.println("El fichero no existe !!!");	}

	} catch (FileNotFoundException e) { e.printStackTrace();	System.out.println("ERROR1 ");
	} catch (UnsupportedEncodingException e) {e.printStackTrace();	System.out.println("ERROR2");
	} catch (IOException e) {e.printStackTrace();			System.out.println("ERROR3");
	} catch (Exception e) {e.printStackTrace();			System.out.println("ERROR4");	}

	obj_ConDB.cerrarJOB(FLD_JOB_ID,"BDI",inicio, obj_con);  

	try {	 
	    if (obj_con != null ) {
	    	obj_con.close();
	    }
	   

	} catch (SQLException e) {	e.printStackTrace();		System.out.println("ERROR5");	}

	System.out.println("Se cierra el JOB de BDI: "+ FLD_JOB_ID);

	System.out.println("******************* FIN CONCILIACION DE BDI *****************************");
    }
}
