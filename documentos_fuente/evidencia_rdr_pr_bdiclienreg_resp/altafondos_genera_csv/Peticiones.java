package peticiones;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Vector;

import jdbc.QueryExec;
import jdbc.QuerysStr;
import main.Main;
import tools.DateUtil;
import tools.FicherosCLS;

public class Peticiones {
	private Connection conexion;
	private Vector<String[]>peticiones;
	private Vector<Fondo>Fondos;
	private String carpetaSalida;
	
	private int maxOficinas = 0;
	private int maxBranches = 0;
	private int numOks      = 0;
	
	public Peticiones(Connection conexion, String carpetaSalida) {
		this.conexion = conexion;
		this.carpetaSalida = carpetaSalida;
	}
	public void procesaPeticiones(String DCS) {
		Statement st = null;
		String msg = "Peticiones::procesaPeticiones::Extrayendo fondos pendientes de cargar.";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		
		try {
			String q ="";
			st = conexion.createStatement();
			if("DCS".equals(DCS)){ q = QuerysStr.selectFondosPosiblesDCS();}
			else{q = QuerysStr.selectFondosPosibles();}
			System.out.println(q);
			peticiones     = new Vector<String[]>();
			Fondos         = new Vector<Fondo>();
			peticiones     = QueryExec.queryToVector(conexion, q);
			if(peticiones == null){
				msg = "Peticiones::procesaPeticiones::No existen fondos pendientes de carga.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			}else if(peticiones.size()==0){
				msg = "Peticiones::procesaPeticiones::No existen fondos pendientes de carga.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			}else {
				msg = "Peticiones::procesaPeticiones::"+peticiones.size()+" fondos a cargar en RDR.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				for(int  i=0;i<peticiones.size();i++){
					System.out.println(peticiones.get(i)[0]+" - "+peticiones.get(i)[1]);
					String oid = peticiones.get(i)[0];
					String LEI = peticiones.get(i)[0];
					Fondo f = new Fondo (conexion, oid, LEI);
					Fondos.add(f);
				}
				
				msg = "Peticiones::procesaPeticiones::Procesando peticiones...";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				

				
				
				for(int j=0;j<Fondos.size();j++){
					Fondos.get(j).procesaFondo();
					if(Fondos.get(j).isValidFund()){
						numOks++;
						if(maxOficinas<Fondos.get(j).getNumOficinas()){
							maxOficinas = Fondos.get(j).getNumOficinas();
						}
						if(maxBranches<Fondos.get(j).getNumBranches()){
							maxBranches = Fondos.get(j).getNumBranches();
						}
					}
					System.out.println(j+" maxBranch   "+maxBranches);
					System.out.println(j+" maxOficinas "+maxOficinas);
				}
				
				msg = "Peticiones::procesaPeticiones::Peticiones procesadas...";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			msg = "Peticiones::procesaPeticiones::ERROR::Fallo al procesar las peticiones. "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
		}finally {
			try {
				st.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		
	}
	
	public void generaCSVAltaFondos(){
		String msg = "Peticiones::generaCSVAltaFondos::Generando csv...";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		if(numOks == 0){
			msg = "Peticiones::generaCSVAltaFondos::No hay fondos correctos a cargar.";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			return;
		}
		String nombreFichero = DateUtil.FechaSistemaCompletaString()+"@FUND_LOADER.csv";
		String rutaSalida = carpetaSalida + "/" + nombreFichero;
		
		Vector<String> contenidoCSV = new Vector<String>();
		
		String cabecera = Fondos.get(0).getCsvline().getCabecera(maxOficinas, maxBranches);
		contenidoCSV.add(cabecera);
		for(int i=0; i<Fondos.size();i++){
			if(Fondos.get(i).isValidFund()){
				String lineaCSV = Fondos.get(i).getCsvline().getCSVLine(maxOficinas, maxBranches);
				contenidoCSV.add(lineaCSV);
			}
		}
		FicherosCLS.writeVectorInFileBoolean(rutaSalida, contenidoCSV);
		
	}
}
