package leirequest;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Vector;

import jdbc.QueryExec;
import jdbc.QuerysStr;
import main.Main;

public class AltaRegisterLEIRequest {
	private Connection conexion;
	private String oidFondo;
	private String oidNew;
	private String LEI;
	private HashMap<String, String>atributos;
	private boolean error = false;
	private String errDesc = "";
	public  AltaRegisterLEIRequest(Connection conexion, String oidFondo, String LEIFondo) {
		this.conexion=conexion;
		this.oidFondo=oidFondo;
		this.LEI=LEIFondo;
	}
	
	public void procesaAlta() {
		String msg = "AltaRegisterLEIRequest::procesaAlta::Marcando solicitud de alta de LEI para el fondo con vreqOid = '"+this.oidFondo+"'";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		
		try {
			String q = "";
			msg = "AltaRegisterLEIRequest::procesaAlta::Descargando atributos...";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			atributos = new HashMap<String, String>();
			Vector<String[]>atrs = QueryExec.queryToVector(this.conexion, QuerysStr.selectFondosAtributos(this.oidFondo));
			for(int i=0;i<atrs.size();i++){
				String key = atrs.get(i)[0].trim();
				String val = atrs.get(i)[1].trim().replace("'", "''");
				this.atributos.put(key, val);
			}
			msg = "AltaRegisterLEIRequest::procesaAlta::Descargados "+this.atributos.size()+" atributos";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			
			msg = "AltaRegisterLEIRequest::procesaAlta::Registrando solicitud";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			
			//Datos Copy
			String PAIS = " ";
			String ENTIDAD = " ";
			String PERSCTPN = " ";
			String DOCUMPS = " ";
			String INICVIG = " ";
			String FINVIG = " ";
			String FILLER = " ";
			
			//Pais
			//Se deja pais por defecto ES para todo lo enviado a Clientela.
			//PAIS = this.atributos.get("COUNTRY");
			PAIS = "ES";
			//Entidad
			ENTIDAD = this.atributos.get("ENTR_OWN");
			//Asignar CClient
			PERSCTPN = this.atributos.get("CCLIENT");
			//Asignar LEI
			DOCUMPS = this.atributos.get("LEI_CODE");
			
			//Asignar inicio vigencia
			Vector<String> res = QuerysStr.obtenerFechaInicioVigenciaLEI(this.LEI, conexion);
			INICVIG = res.get(0);
			
			//Asignar fin vigencia
			res = QuerysStr.obtenerFechaFinVigenciaLEI(this.LEI, conexion);

			FINVIG = res.get(0);
			
			oidNew = QueryExec.getNewOid(conexion);
			QuerysStr.insertVREQ_LEIReg_Req(oidNew, LEI, conexion);
			
			insertaAtributo(this.oidNew, "PAIS", PAIS);
			insertaAtributo(this.oidNew, "ENTIDAD", ENTIDAD);
			insertaAtributo(this.oidNew, "PERSCTPN", PERSCTPN);
			insertaAtributo(this.oidNew, "DOCUMPS", DOCUMPS);
			insertaAtributo(this.oidNew, "INICVIG", INICVIG);
			insertaAtributo(this.oidNew, "FINVIG", FINVIG);
			insertaAtributo(this.oidNew, "FILLER", FILLER);
			
			if(this.error){
				QuerysStr.updateVREQDescripByOid(this.oidNew, "ERROR", "INVESTORS_CLIENTREG_RESP", this.errDesc, conexion);
			}
			
		}catch(Exception e) {
			msg = "AltaRegisterLEIRequest::procesaAlta::Fallo al registrar la solicitud de registro de LEI.";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			msg = e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			this.error = true;
			this.errDesc = "AltaRegisterLEIRequest::procesaAlta::Fallo al registrar la solicitud de nuevo cliente. "+e.toString(); 
		}
	}
	
	private void insertaAtributo(String oid, String atributo, String valor) {
		try {
			QuerysStr.insertUTD1FundParam(oid, "FIELD", atributo, valor, "INVESTORSPLAN_FUNDS", conexion);
		} catch (Exception e) {
			// TODO: handle exception
			String msg = "AltaRegisterLEIRequest::insertaAtributo::ERROR::Fallo al insertar atributo "+atributo+"("+valor+"). "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);	
			this.error=true;
			this.errDesc = msg;
		}
	}

	public boolean isError() {
		return error;
	}

	public String getErrDesc() {
		return errDesc;
	}
	
}
