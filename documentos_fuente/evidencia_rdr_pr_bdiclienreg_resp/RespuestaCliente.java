package ficheros;

import java.sql.Connection;
import java.util.Vector;
import jdbc.QuerysStr;
import main.Main;

public class RespuestaCliente {
	private Connection conexion;
	private String LineaBDI;
	private String oidIdentificado;
	private boolean ok = true;
	private String errDesc = "";
	private int offset = 0;

	private String[] nombreCampos = { "LEI", "HORA", "NOMCLI", "NACIMIENTO", "DOMI_FISC","PLAZA_FISC", "PROVI",
			"PAIS_RESI", "PAIS_NAC", "CNAE", "FORM_SOCI", "IDIOMA", "PLAZAINT", "INST_CODE", "TIP_BANCO", "BROKER", "BIC",
			"CCLIEN_RE","POSTAL_CDE","DES_DISPLA", "FILLER", "COD_TES", "NOMCORTO", "CCLIENT", "BDICODE", "NUMFOLIO", "COD_ACK",
			"COD_ERROR", "DESC_ERROR",  "FILLER_OUT" };

	private int[] longitudes = { 20, 14, 60, 10, 36, 28, 20,
			2, 2, 9, 5, 1, 3, 4, 1, 1, 11,
			9, 20, 36, 57, 8, 10, 9, 9, 14, 4,
			4, 60, 133};

	private String[] infoCampos = new String[nombreCampos.length];

	public RespuestaCliente(Connection conexion, String LineaBDI) {
		this.conexion = conexion;
		this.LineaBDI = LineaBDI;
	}

	public void procesaRespuesta() {
		String msg = "RespuestaCliente::procesaRespuesta::tratando respuesta...";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		segmentaMensaje();

		if (ok == false) {
			return;
		}

		try {
			String LEI = infoCampos[0];
			String HORA = infoCampos[1];
			String COD_ACK = infoCampos[26];
			msg = "RespuestaCliente::procesaRespuesta::Identificando cliente...";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			Vector<String> sal = QuerysStr.identificaCliente(LEI, HORA, conexion);

			if (sal == null) {
				msg = "RespuestaCliente::procesaRespuesta::No se ha podido encontrar la peticion asociada.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			} else if (sal.size() == 0) {
				msg = "RespuestaCliente::procesaRespuesta::No se ha podido encontrar la peticion asociada.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			} else {
				this.oidIdentificado = sal.get(0);
				msg = "RespuestaCliente::procesaRespuesta::OID Identificado para el LEI " + LEI + ": " + this.oidIdentificado
						+ ". Actualizando peticion";
				System.out.println(msg);
				Main.LOGGER.info(msg);

				QuerysStr.updateVREQDescripByOid(this.oidIdentificado, "PROCESSING_RESP", "CLIENTELABDI_RESP", msg, conexion);

				msg = "RespuestaCliente::procesaRespuesta::Actualizada peticion. Insertando atributos";
				System.out.println(msg);
				Main.LOGGER.info(msg);

				insertaAtributos();

				if (ok) {
					msg = "RespuestaCliente::procesaRespuesta::Respuesta para el LEI " + LEI
							+ " procesada correctamente. Resultado: " + COD_ACK;
					System.out.println(msg);
					Main.LOGGER.info(msg);
					QuerysStr.updateVREQDescripByOid(this.oidIdentificado, COD_ACK, "CLIENTELABDI_RESP", msg, conexion);
				} else {
					QuerysStr.updateVREQDescripByOid(this.oidIdentificado, "ERROR_PROC_RESP", "CLIENTELABDI_RESP",this.errDesc, conexion);
				}
			}
		} catch (Exception e) {
			msg = "RespuestaCliente::procesaRespuesta::Fallo al registrar tratar la respuesta de BDI para el cliente.";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			msg = e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			String desc = "RespuestaCliente::procesaRespuesta::Fallo al registrar tratar la respuesta de BDI para el cliente. "
					+ e.toString();
			try {
				QuerysStr.updateVREQDescripByOid(this.oidIdentificado, "ERROR_PROC_RESP", "CLIENTELABDI_RESP", desc, conexion);
				//st.executeUpdate(q);
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
	}

	private void segmentaMensaje() {
		try {
			String msg = "RespuestaCliente::segmentaMensaje::Segmentando mensaje de respuesta.";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			if (nombreCampos.length != longitudes.length) {
				throw new Exception("ERROR");
			}
			//Rellena info de los campos
			for (int i = 0; i < nombreCampos.length; i++) {
				infoCampos[i] = LineaBDI.substring(offset, offset + longitudes[i]);
				offset = offset + longitudes[i];
			}

			msg = "RespuestaCliente::segmentaMensaje::Mensaje segmentado correctamente.";
			System.out.println(msg);
			Main.LOGGER.info(msg);

			//Loggea
			for (int i = 0; i < nombreCampos.length; i++) {
				msg = "                 " + nombreCampos[i];
				for (int j = 0; j < 11 - nombreCampos[i].length(); j++) {
					msg += " ";
				}
				msg += "- '" + infoCampos[i] + "'";
				System.out.println(msg);
				Main.LOGGER.info(msg);
			}

		} catch (Exception e) {
			// TODO: handle exception
			ok = false;
			String msg = "RespuestaCliente::segmentaMensaje::ERROR::Fallo al separar el mensaje. " + e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			this.errDesc = msg;
		}
	}

	private void insertaAtributo(String oid, String atributo, String valor) {
		try {
			if (valor != null) {
				valor = valor.trim().replace("'", "''");
			}
			if (valor == null || "".equals(valor)) {
				valor = " ";
			}
			QuerysStr.insertUTD1FundParam(oid, "FIELD_RESP", atributo, valor, "CLIENTELABDI_RESP", conexion);

		} catch (Exception e) {
			// TODO: handle exception
			String msg = "RespuestaCliente::insertaAtributo::ERROR::Fallo al insertar atributo " + atributo + ". "
					+ e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			this.ok = false;
			this.errDesc = msg;
		}
	}

	private void insertaAtributos() {

		for (int i = 0; i < nombreCampos.length; i++) {
			insertaAtributo(this.oidIdentificado, nombreCampos[i], infoCampos[i]);
		}
	}

	public boolean isOk() {
		return ok;
	}

	public String getHORA() {
		return infoCampos[1];
	}
}
