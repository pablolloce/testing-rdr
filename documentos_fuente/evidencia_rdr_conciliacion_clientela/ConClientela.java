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
import java.util.Map;

import jdbc.ConDB;
import util.Utilidades;

// La lógica de esta clase se basa en leer el fichero, mientras se va leyendo, se va volcando en un array. 
// Se generan sublistas de un rango definido en una variable estática.
// Esta sublista se mana a ejecución con un hilo nuevo.

// Se realiza con Multihilo y Pool de conexiones

public class ConClientela {

	public static ArrayList<HashMap<String,String>> arr = new ArrayList<HashMap<String,String>>();
	public static ArrayList<String> noConci = new ArrayList<String>();
	public static ArrayList<String> errorConci = new ArrayList<String>();
	public static Connection obj_con=null;
	public static ConDB obj_ConDB=null;

	static int i=0; 	static int rango=100; 		static int x_ins=0; 		static int y_ins=0;		    
	static int x=0;  	static int y=0;			static int x_err=0;  		static int y_err=0;	    

	public static String FLD_JOB_ID=null;
	public static String msg_ins="Codigo Clientela en RDR que no concilia en Clientela";
	public static String msg_err="Codigo Clientela en RDR que no es valido";

	public static void main(String[] args) throws Exception {
		System.out.println("******************* INICIO CONCILIACION DE CLIENTELA *******************");

		obj_ConDB=new ConDB();		obj_ConDB.ObtenerCredenciales();	obj_con=obj_ConDB.ObtenerConexion();

		ArrayList<String> CLIIdsFic = new ArrayList<String>();
		ArrayList<String> CLIIds = new ArrayList<String>();

		HashMap<String, String> cliLEIs = new HashMap<String, String>();
		HashMap<String, String> cliLEIsRDR = new HashMap<String, String>();

		Utilidades ut = new Utilidades();	ConClientela.FLD_JOB_ID=ut.generateJOBID();	

		FileInputStream fis = null;	    InputStreamReader fr = null;	    BufferedReader br = null;	    String linea = "";

		long inicio = obj_ConDB.crearJOB(FLD_JOB_ID,"CCL", obj_con);	    System.out.println("Se crea JOB");

		String VCH_DBC_COD_CCLIEN ="";		String VCH_DBC_XTI_TIPERSO ="";		String VCH_DBC_XTI_CTIPCL1 ="";
		String VCH_DBC_COD_DOCUM25 ="";		String VCH_DBC_COD_DNOMB ="";		String VCH_DBC_DES_DENOMB ="";
		String VCH_DBC_COD_CTPVIA ="";		String VCH_DBC_DES_CCALLE ="";		String VCH_DBC_QNU_CNUVIA ="";
		String VCH_DBC_DES_CRESTO ="";		String VCH_DBC_DES_DPLAZA ="";		String VCH_DBC_DES_DPROVI ="";
		String VCH_DBC_COD_CDIPOS ="";		String VCH_DBC_COD_CDIPEX ="";		String VCH_DBC_COD_CPAISN ="";
		String VCH_DBC_COD_CCNO ="";		String VCH_DBC_COD_CNAE5 ="";		String VCH_DBC_COD_TIPINS ="";
		String VCH_DBC_COD_CLPANA ="";		String VCH_DBC_FEC_FNACIF ="";		String VCH_DBC_COD_FORSOCI ="";
		String VCH_DBC_XTI_CVIP ="";		String VCH_DBC_COD_IDIOMA ="";		String VCH_DBC_COD_OFIPPAL ="";
		String DBC_XSN_EMPEXRET ="";		String DBC_FEC_INIEXRET ="";		String DBC_FEC_EMPEXRET =""; // 20160415
		String VCH_DBC_COD_LEI = ""; //20170920

		//Lectura del fichero
		File f = new File(args[0]);		 System.out.println(args[0]);
		try    {
			fis = new FileInputStream(f);    	fr = new InputStreamReader(fis, "ISO-8859-1");	        br = new BufferedReader(fr);

			System.out.println("Se obtienen codigos de Clientela de GS");
			CLIIds = obj_ConDB.obtenerCLIs(obj_con);

			// Obtiene Clientes y LEI existentes en BBDD antes de conciliar 
			cliLEIsRDR = obj_ConDB.obtenerCliLEIsRDR(obj_con);

			final List<Thread> threads_exe      = new ArrayList<Thread>();
			final List<Thread> threads_ins      = new ArrayList<Thread>();
			final List<Thread> threads_err      = new ArrayList<Thread>();

			System.out.println(" - COMIENZA LECTURA DEL FICHERO Y EJECUCION EN CLIENTELA - ");
			if (f.exists())	{            
				br.readLine();//Lee cabecera
				ArrayList<String> publicarMA = new ArrayList<String>();
				ArrayList<String> reportarMA = new ArrayList<String>();
				while((linea=br.readLine())!=null)    {

					try    	{
						if (linea.substring(linea.length() - 1).equals(";")){
							linea=linea+"N";					
						}

						String[] campos=null; campos=linea.split(";");		    			    						

						if (campos.length!=97){     
							System.out.println(" Fallo en " +campos[2].trim() + " debido a longitud "+ campos.length);
							errorConci.add(campos[2].trim());
							continue;
						}


						VCH_DBC_COD_CCLIEN=campos[2].trim();		VCH_DBC_XTI_TIPERSO=campos[3].trim();
						VCH_DBC_XTI_CTIPCL1=campos[6].trim();		VCH_DBC_COD_DOCUM25=campos[7].trim();
						VCH_DBC_COD_DNOMB=campos[11].trim();		VCH_DBC_DES_DENOMB=campos[12].trim();
						VCH_DBC_COD_CTPVIA=campos[14].trim();		VCH_DBC_DES_CCALLE=campos[15].trim();
						VCH_DBC_QNU_CNUVIA=campos[16].trim();		VCH_DBC_DES_CRESTO=campos[17].trim();
						VCH_DBC_DES_DPLAZA=campos[18].trim();		VCH_DBC_DES_DPROVI=campos[19].trim();
						VCH_DBC_COD_CDIPOS=campos[20].trim();		VCH_DBC_COD_CDIPEX=campos[22].trim();
						VCH_DBC_COD_CPAISN=campos[23].trim();		VCH_DBC_COD_CCNO=campos[27].trim();
						VCH_DBC_COD_CNAE5=campos[28].trim();		VCH_DBC_COD_TIPINS=campos[31].trim();
						VCH_DBC_COD_CLPANA=campos[44].trim();		VCH_DBC_FEC_FNACIF=campos[45].trim();
						VCH_DBC_COD_FORSOCI=campos[51].trim();		VCH_DBC_XTI_CVIP=campos[53].trim();
						VCH_DBC_COD_IDIOMA=campos[83].trim();		VCH_DBC_COD_OFIPPAL=campos[95].trim();
						DBC_XSN_EMPEXRET=campos[86].trim();			DBC_FEC_INIEXRET=campos[87].trim();  	//20160416
						DBC_FEC_EMPEXRET=campos[88].trim();							//20160416
						VCH_DBC_COD_LEI=campos[96].trim(); //20170920

						// Comprobación Cuentas Gestionadas

						//El proceso batch debe ser adaptado de tal forma que, si en la conciliación con clientela viene un LEI de un cliente que a nivel operativo
						//tiene rol de cuentas gestionada asociada y activa, y el LEI es distinto del que tiene inicialmente, no debe actualizar los datos.

						int cg = 0;
						boolean concilia = true;

						String LEInuevo = VCH_DBC_COD_LEI.substring(1);
						String LEIactual = obj_ConDB.obtenerLEIactual(VCH_DBC_COD_CCLIEN, obj_con);

						if(!LEInuevo.equalsIgnoreCase(LEIactual) && LEInuevo.trim().length()>0){
							cg = obj_ConDB.obtenerMA(VCH_DBC_COD_CCLIEN,obj_con);

							if(cg==0){
								concilia = true;
							}else if(cg>0 && LEInuevo.trim().length()>0 && LEIactual.trim().length()>0 && !LEIactual.equalsIgnoreCase("N")){
								concilia = false;
								reportarMA.add(VCH_DBC_COD_CCLIEN+"|"+LEInuevo+"|"+LEIactual);
							}else if(cg>0 && LEInuevo.trim().length()==0 && LEIactual.trim().length()>0){
								concilia = true;
								publicarMA.add(VCH_DBC_COD_CCLIEN);
							}else if(cg>0 && (LEIactual.trim().length()==0 || LEIactual.equalsIgnoreCase("N"))){
								concilia = true;
								publicarMA.add(VCH_DBC_COD_CCLIEN);
							}else {
								concilia = true;
							}

						}

						if(concilia){
							i++;		if (i % 10000 == 0) {   System.out.println("Linea Clientela - "+i); }
							HashMap<String,String> mapint = new HashMap<String,String>();

							mapint.put("VCH_DBC_COD_CCLIEN" , VCH_DBC_COD_CCLIEN );		mapint.put("VCH_DBC_XTI_TIPERSO" , VCH_DBC_XTI_TIPERSO );
							mapint.put("VCH_DBC_XTI_CTIPCL1" , VCH_DBC_XTI_CTIPCL1 );	mapint.put("VCH_DBC_COD_DOCUM25" , VCH_DBC_COD_DOCUM25 );
							mapint.put("VCH_DBC_COD_DNOMB" , VCH_DBC_COD_DNOMB );		mapint.put("VCH_DBC_DES_DENOMB" , VCH_DBC_DES_DENOMB );
							mapint.put("VCH_DBC_COD_CTPVIA" , VCH_DBC_COD_CTPVIA );		mapint.put("VCH_DBC_DES_CCALLE" , VCH_DBC_DES_CCALLE );
							mapint.put("VCH_DBC_QNU_CNUVIA" , VCH_DBC_QNU_CNUVIA );		mapint.put("VCH_DBC_DES_CRESTO" , VCH_DBC_DES_CRESTO );
							mapint.put("VCH_DBC_DES_DPLAZA" , VCH_DBC_DES_DPLAZA );		mapint.put("VCH_DBC_DES_DPROVI" , VCH_DBC_DES_DPROVI );
							mapint.put("VCH_DBC_COD_CDIPOS" , VCH_DBC_COD_CDIPOS );		mapint.put("VCH_DBC_COD_CDIPEX" , VCH_DBC_COD_CDIPEX );
							mapint.put("VCH_DBC_COD_CPAISN" , VCH_DBC_COD_CPAISN );		mapint.put("VCH_DBC_COD_CCNO" , VCH_DBC_COD_CCNO );
							mapint.put("VCH_DBC_COD_CNAE5" , VCH_DBC_COD_CNAE5 );		mapint.put("VCH_DBC_COD_TIPINS" , VCH_DBC_COD_TIPINS );
							mapint.put("VCH_DBC_COD_CLPANA" , VCH_DBC_COD_CLPANA );		mapint.put("VCH_DBC_FEC_FNACIF" , VCH_DBC_FEC_FNACIF );
							mapint.put("VCH_DBC_COD_FORSOCI" , VCH_DBC_COD_FORSOCI );	mapint.put("VCH_DBC_XTI_CVIP" , VCH_DBC_XTI_CVIP );
							mapint.put("VCH_DBC_COD_IDIOMA" , VCH_DBC_COD_IDIOMA );		mapint.put("VCH_DBC_COD_OFIPPAL" , VCH_DBC_COD_OFIPPAL );
							mapint.put("FLD_JOB_ID" , FLD_JOB_ID );				mapint.put("DBC_XSN_EMPEXRET" , DBC_XSN_EMPEXRET );	//
							mapint.put("DBC_FEC_INIEXRET" , DBC_FEC_INIEXRET );		mapint.put("DBC_FEC_EMPEXRET" , DBC_FEC_EMPEXRET );	//
							mapint.put("VCH_DBC_COD_LEI" , VCH_DBC_COD_LEI ); //20170920


							arr.add(mapint);					




							//Se crea un sublista del array completo de un tamaño definido por la variable estática rango 
							//la cual se puede modificar en las declaraciones globales. Posible calculo también de otros modos.
							//Se crea un hilo de ejecución para la ejecución del proceso y se le manda la sublista.

							if(i%ConClientela.rango==0) { 
								System.out.println("-------------------- "+ i);
								ConClientela.y=i; ConClientela.x=i-ConClientela.rango;
								Thread hilo=new Thread( new Runnable() {
									ArrayList<HashMap<String,String>> arr2 = new ArrayList<HashMap<String,String>>(
											ConClientela.arr.subList(ConClientela.x,ConClientela.y));
									public void run() { 
										obj_ConDB.executeCONCLI_Hilos(arr2, obj_con);
									}
								});
								hilo.start();          threads_exe.add(hilo);		        		
							}  	

							CLIIdsFic.add(VCH_DBC_COD_CCLIEN);

							String leiS="";
							if (!VCH_DBC_COD_LEI.equalsIgnoreCase("N")){
								leiS=VCH_DBC_COD_LEI.substring(1);
							} else {
								leiS="NULO";
							}
							// Listado Cod Cliente y LEI procedentes de Clientela
							if (VCH_DBC_COD_LEI!=null && VCH_DBC_COD_LEI.length()>0){
								cliLEIs.put(VCH_DBC_COD_CCLIEN, leiS);
							}
						}else{
							System.out.println("No se realiza conciliación debido al proceso de Cuentas Gestionadas");
						}
						//Lógica de códigos de BDI que no estén en GS o MGC
					}catch(ArrayIndexOutOfBoundsException e)  {
						//System.out.println("campos.size() - "+campos.length);
						System.out.println("Linea Clientela - "+i);
						System.out.println("Línea con longitud errónea: " + linea);
					}
					catch(Exception ex)  {
						//System.out.println("campos.size() - "+campos.length);
						ex.printStackTrace();
						System.out.println("Excepcion: " + ex+ " - " +linea);
					}
				}
				if(arr!=null && arr.size()>0){
					Thread hilo=new Thread( new Runnable() {
						ArrayList<HashMap<String,String>> arr2 =new ArrayList<HashMap<String,String>>(
								ConClientela.arr.subList(ConClientela.y,ConClientela.i));		       
						public void run() {         	 
							obj_ConDB.executeCONCLI_Hilos(arr2, obj_con); 
						}
					});
					hilo.start();	         threads_exe.add(hilo);

					//Esperamos a que finalice los hilos de ejecución
					for( Thread t : threads_exe ) {       t.join();       }
				}
				// Insercion en Register Log Table de aquellas contrapartidas con clientes con distinto LEI
				for (Map.Entry<String, String> canCli : cliLEIsRDR.entrySet()) {			
					String canonico = canCli.getKey();
					String[] clients = canCli.getValue().split(";");
					String leiIni=""; String clientes=""; int cont=0; boolean leiDis=false;

					for (int i=0; i < clients.length; i++) {
						if (cliLEIs.containsKey(clients[i])) {
							if (cont==0){
								leiIni = cliLEIs.get(clients[i]);
								clientes = clients[i];						
							} else {
								if (!leiIni.contains(cliLEIs.get(clients[i]))){
									leiIni += ", "+cliLEIs.get(clients[i]);
									leiDis = true;
								}
								clientes += ", "+clients[i];
							}
							cont++;
						} 
					}

					if (leiIni!=null && leiIni.length()>0 && leiDis){
						obj_ConDB.insertRLT1ClientelaLEI(canonico, leiIni, clientes, ConClientela.FLD_JOB_ID, obj_con);
					}
				}

				System.out.println(" - COMIENZA COMPARACION EN CLIENTELA - ");

				boolean encontrado=false;
				for(String cli : CLIIds)        {

					if(CLIIdsFic.contains(cli))       {
						encontrado=true;
					}
					if(!encontrado)   {
						ConClientela.noConci.add(cli);
					}
					encontrado=false;
				}

				/*System.out.println(" - COMIENZA INSERCION DE REGISTROS NO ENCONTRADOS CLIENTELA - "+ noConci.size());

				for(int k=0; k<noConci.size(); k++){
					if((k+1)%ConClientela.rango==0) { 
						ConClientela.y_ins=(k+1); ConClientela.x_ins=(k+1)-ConClientela.rango;
						Thread hilo_ins=new Thread( new Runnable() {
							ArrayList<String> arr2 = new ArrayList<String>(
									ConClientela.noConci.subList(ConClientela.x_ins,ConClientela.y_ins));
							public void run() { 
								obj_ConDB.insertRLT1Clientela(arr2, ConClientela.FLD_JOB_ID, obj_con, msg_ins);   
							}
						});
						hilo_ins.start();  		threads_ins.add(hilo_ins);		        		
					}  	
				}

				ConClientela.x_ins=ConClientela.y_ins;		ConClientela.y_ins=noConci.size(); 
				Thread hilo_ins=new Thread( new Runnable() {
					ArrayList<String> arr2 = new ArrayList<String>(
							ConClientela.noConci.subList(ConClientela.x_ins,ConClientela.y_ins));
					public void run() {  
						obj_ConDB.insertRLT1Clientela(arr2, ConClientela.FLD_JOB_ID, obj_con, msg_ins); 
					}
				});
				hilo_ins.start();       threads_ins.add(hilo_ins);		        		

				//Esperamos a que finalice los hilos de inserción
				for( Thread t : threads_ins ) {       t.join();       }

				System.out.println(" - COMIENZA INSERCION DE REGISTROS ERRONEOS - "+errorConci.size());

				for(int k=0; k<errorConci.size(); k++){
					if((k+1)%ConClientela.rango==0) { 
						ConClientela.y_err=(k+1); ConClientela.x_err=(k+1)-ConClientela.rango;
						Thread hilo_err=new Thread( new Runnable() {
							ArrayList<String> arr3 = new ArrayList<String>(
									ConClientela.errorConci.subList(ConClientela.x_err,ConClientela.y_err));
							public void run() { 
								obj_ConDB.insertRLT1Clientela(arr3, ConClientela.FLD_JOB_ID, obj_con, msg_err);   
							}
						});
						hilo_err.start();       		
						threads_err.add(hilo_err);		        		
					}  	
				}

				ConClientela.x_err=ConClientela.y_err;		ConClientela.y_err=errorConci.size(); 
				Thread hilo_err=new Thread( new Runnable() {
					ArrayList<String> arr3 = new ArrayList<String>(
							ConClientela.errorConci.subList(ConClientela.x_err,ConClientela.y_err));
					public void run() {  
						obj_ConDB.insertRLT1Clientela(arr3, ConClientela.FLD_JOB_ID, obj_con, msg_err);   
					}
				});
				hilo_err.start();      threads_err.add(hilo_err);		        		

				//Esperamos a que finalice los hilos de error
				for( Thread t : threads_err ) {       t.join();       }*/

				obj_ConDB.reportarMA(reportarMA, FLD_JOB_ID, obj_con);
				obj_ConDB.publicarMA(publicarMA, FLD_JOB_ID, obj_con);

			}else {	System.out.println("El fichero no existe !!!");	    }

		} catch (FileNotFoundException e) {e.printStackTrace();		System.out.println("ERROR1");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();	System.out.println("ERROR2");
		} catch (IOException e) {e.printStackTrace();			System.out.println("ERROR3");
		} catch (Exception e) {e.printStackTrace();			System.out.println("ERROR4");	}

		obj_ConDB.cerrarJOB(FLD_JOB_ID,"CCL",inicio, obj_con);

		try {	 
			if (obj_con != null ) {
				obj_con.close();
			}



		} catch (SQLException e) {	e.printStackTrace();		System.out.println("ERROR5");	}

		System.out.println("Se cierra el JOB de Clientela: "+ FLD_JOB_ID);

		System.out.println("******************* FIN CONCILIACION DE CLIENTELA *******************");
	}
}
