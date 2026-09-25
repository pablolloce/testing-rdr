package ficheros;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jdbc.QuerysStr;
import main.Main;
import tools.Directorio;
import tools.FicherosCLS;

public class ProcesaFichero {
	private Connection conexion;
	private String rutaReveive;
	private String rutaOld;
	private String rutaError;
	private String ficheroProcesar;
	
	
	private String rutaProcesar;
	private String rutaHistorificar;
	private String rutaSendError;
	
	private Vector<RespuestaCliente>respuestas = new Vector<RespuestaCliente>();
	private HashMap<String, String>horasPeticiones = new HashMap<String, String>();
	
	public ProcesaFichero(Connection conexion, String rutaReveive, String rutaOld, String rutaError, String ficheroProcesar) {
		this.conexion=conexion;
		this.rutaReveive=rutaReveive;
		this.rutaOld=rutaOld;
		this.rutaError=rutaError;
		this.ficheroProcesar=ficheroProcesar;
		
		this.rutaProcesar=this.rutaReveive+this.ficheroProcesar;
		this.rutaHistorificar=this.rutaOld+this.ficheroProcesar;
		this.rutaSendError = this.rutaError+this.ficheroProcesar;
		
		
		
	}
	public void procesar() {
		String msg = "";
		msg = "ProcesaFichero::procesar::Procesando fichero "+rutaProcesar+".";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		respuestas = new Vector<RespuestaCliente>();
		try {
			Vector<String>contenido = FicherosCLS.readFileVector(rutaProcesar);
			if(contenido==null) {
				msg = "ProcesaFichero::procesar::Fichero sin contenido. "+rutaProcesar+".";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			}else if(contenido.size()==0) {
				msg = "ProcesaFichero::procesar::Fichero sin contenido. "+rutaProcesar+".";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			}else {
				for(int i=0;i<contenido.size();i++) {
					String linea = contenido.get(i);
					msg = "ProcesaFichero::procesar::Linea respuesta: "+linea;
					System.out.println(msg);
					Main.LOGGER.info(msg);
					
					if(linea==null || "".equals(linea) || linea.length()<100) {
						msg = "ProcesaFichero::procesar::Linea no valida";
						System.out.println(msg);
						Main.LOGGER.info(msg);
					}else {
						msg = "ProcesaFichero::procesar::Linea valida";
						System.out.println(msg);
						Main.LOGGER.info(msg);
						
						RespuestaCliente r = new RespuestaCliente(conexion, linea);
						respuestas.add(r);
					}
					
				}
				msg = "ProcesaFichero::procesar::"+respuestas.size()+" respuestas a tratar.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				int ok = 0;
				int tot = 0;
				for(int i=0;i<respuestas.size();i++) {
					tot++;
					respuestas.get(i).procesaRespuesta();
					String hora = respuestas.get(i).getHORA();
					if(horasPeticiones.get(hora)==null){
						horasPeticiones.put(hora, hora);
					}
					if(respuestas.get(i).isOk()) {
						ok++;
					}
				}
				msg = "ProcesaFichero::procesar::"+ok+" correctos de "+tot+" procesados.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				
				
				msg = "ProcesaFichero::procesar::Actualizando los LEIs no respondidos de "+horasPeticiones.size()+" peticiones detectadas.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				
				for (Map.Entry<String, String> entry : this.horasPeticiones.entrySet()) {
					String fec = entry.getValue();
					QuerysStr.updateVREQClientesSinRespuesta(fec, "CLIENTELABDI_RESP", "Respuesta no recibida de BDI.", conexion);
				}
				
			}
			Directorio.mueveFichero(rutaProcesar, rutaHistorificar);
		} catch (Exception e) {
			// TODO: handle exception
			msg = "ProcesaFichero::procesar::ERROR::Fallo al procesar el fichero "+rutaProcesar+". "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			Directorio.mueveFichero(rutaProcesar, rutaSendError);
		} 
	}
}
