package jdbc;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import utilities.Constants;
import utilities.FicheroExtraccion;

public class Querys {
	public static final Logger LOGGER = Logger.getLogger(Querys.class);
	public static boolean fin = false;
	public static int contadorInserciones = 0;
	public static int cont_conexion = 0;
	public static int totalRegistros;
	public static String resultado = "";
	public static String resultadoEntidades = "";
	public static String listaExtraccion = "";
	public static String listaCpty = "";
	public static String obtenerEntidad = "";
	public static String cpty = "";
	public static int contador = 0;
	public static int contadorInsercionesRLT1 = 0;
	public static ArrayList<String> listaEntidades = new ArrayList();
	public static ArrayList<String> TotalCptys = new ArrayList();
	public static ArrayList<String> insercionesRLT1 = new ArrayList();
	public static ArrayList<String> totaletiquetas = new ArrayList();
	private Connection con = ConDB.getInstance().getConnect();

	public String obtenerEntidades(Connection connection, String entidades) throws SQLException {
		try {

			PreparedStatement statement = null;

			switch (entidades){

			case Constants.SSIS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionSSIs.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionSSIs.sql'", "obtenerEntidades");
				break;
			case Constants.SCIS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionSCIs.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionSCIs.sql'", "obtenerEntidades");
				break;
			case Constants.CONTC:
			case Constants.DOMI:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONT.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONT.sql'", "obtenerEntidades");
				break;
			case Constants.CONTR:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONTR.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONTR.sql'", "obtenerEntidades");
				break;				
			case Constants.BASKETS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionBASKETS.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionBASKETS.sql'", "obtenerEntidades");
				break;
			case Constants.DUCOCPTY:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionDUCOCPTY.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionDUCOCPTY.sql'", "obtenerEntidades");
				break;
			case Constants.CONTRBBVA:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONTRBBVA.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionCONTRBBVA.sql'", "obtenerEntidades");
				break;
			case Constants.THIRDPARTIES:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionTHIRDPARTIES.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionTHIRDPARTIES.sql'", "obtenerEntidades");
				break;	

			}
			for(ResultSet rs = statement.executeQuery(); rs.next(); resultadoEntidades = rs.getString("clob_value")) {
			}

		} catch (SQLException var4) {
			LOGGER.error(var4);
		}

		return resultadoEntidades;
	}

	public ArrayList<String> recuperarEntidades(Connection connection, String typeInfo) throws SQLException {
		String[] aux = new String[1];

		try {
			PreparedStatement statement = this.con.prepareStatement(resultadoEntidades);
			marcadoQuery(connection, resultadoEntidades, "recuperarEntidades");
			for(ResultSet rs = statement.executeQuery(); rs.next(); aux = new String[1]) {

				switch (typeInfo){
				case Constants.SSIS:
					aux[0] = rs.getString("SSI_OID");
					break;
				case Constants.SCIS:
					aux[0] = rs.getString("SCIS_OID");
					break;
				case Constants.CONTC:
				case Constants.DOMI:
					aux[0] = rs.getString("CONTCT_OID");
					break;
				case Constants.CONTR:
					aux[0] = rs.getString("LAGR_OID");
					break;
				case Constants.BASKETS:
					aux[0] = rs.getString("INSTR_ID");
					break;
				case Constants.DUCOCPTY:
					aux[0] = rs.getString("INST_MNEM");
					break;
				case Constants.CONTRBBVA:
					aux[0] = rs.getString("LAGR_OID");
					break;
				case Constants.THIRDPARTIES:
					aux[0] = rs.getString("INST_MNEM");
					break;
				}

				obtenerEntidad = aux[0];
				listaEntidades.add(obtenerEntidad);
			}
		} catch (SQLException var5 ) {
			LOGGER.error(var5);
		}

		return listaEntidades;
	}

	public String obtenerExtraccion(Connection connection, String entidad) {
		try {

			PreparedStatement statement = null;

			switch (entidad){
			case Constants.SSIS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaSSIs.sql'");
				//marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaSSIs.sql'", "obtenerExtraccion");
				break;
			case Constants.SCIS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaSCIs.sql'");
				//marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaSCIs.sql'", "obtenerExtraccion");
				break;
			case Constants.CONTC:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONT.sql'");
				//marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONT.sql'", "obtenerExtraccion");
				break;
			case Constants.CONTR:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONTR.sql'");
				//marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONTR.sql'", "obtenerExtraccion");
				break;
			case Constants.BASKETS:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaBASKETS.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaBASKETS.sql'", "obtenerExtraccion");
				break;
			case Constants.DUCOCPTY:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionAdhocDUCOCPTY.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionAdhocDUCOCPTY.sql'", "obtenerExtraccion");
				break;
			case Constants.DOMI:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionDominiosContactos.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionDominiosContactos.sql'", "obtenerExtraccion");
				break;
			case Constants.CONTRBBVA:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONTRBBVA.sql'");
				//marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaCONTRBBVA.sql'", "obtenerExtraccion");
				break;
			case Constants.THIRDPARTIES:
				statement = connection.prepareStatement("select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaTHIRDPARTIES.sql'");
				marcadoQuery(connection, "select clob_value from ft_t_ate1 where ACTION_NME = 'ExtraccionContingenciaTHIRDPARTIES.sql'", "obtenerExtraccion");
				break;
			}

			for(ResultSet rs = statement.executeQuery(); rs.next(); resultado = rs.getString("clob_value")) {

			}
		} catch (SQLException var4) {
			LOGGER.error(var4);
		}

		return resultado;
	}

	public String obtenerQueryCpty(String nemonico, File ficherosalida, String rutaFichero, FicheroExtraccion ficheroGrabar, String typeInfo) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date(System.currentTimeMillis());
		String xml = "";

		try {

			switch (typeInfo){
			case Constants.SSIS:
				PreparedStatement statement2 = this.con.prepareStatement(resultado);
				statement2.setString(1, nemonico);
				System.out.println(nemonico);
				xml = obtenerXMLResult(statement2, xml);
				break;
			case Constants.SCIS:
				PreparedStatement statement3 = this.con.prepareStatement(resultado);
				statement3.setString(1, nemonico);
				System.out.println(nemonico);
				xml = obtenerXMLResult(statement3, xml);
				break;
			case Constants.CONTC:
				PreparedStatement statement4 = this.con.prepareStatement(resultado);
				statement4.setString(1, nemonico);
				//LOGGER.info(nemonico);
				System.out.println(nemonico);
				xml = obtenerXMLResult(statement4, xml);
				break;
			case Constants.CONTR:
				PreparedStatement statement5 = this.con.prepareStatement(resultado);
				statement5.setString(1, nemonico);
				//LOGGER.info(nemonico);
				System.out.println(nemonico +"   "+ contador++);
				xml = obtenerXMLResult(statement5, xml);
				break;
			case Constants.BASKETS:
				PreparedStatement statement6 = this.con.prepareStatement(resultado);
				statement6.setString(1, nemonico);
				//LOGGER.info(nemonico);
				System.out.println(nemonico +"   "+ contador++);
				marcadoQuery(this.con, resultado, "obtenerXMLResult");
				xml = obtenerXMLResult(statement6, xml);
				break;
			case Constants.CONTRBBVA:
				PreparedStatement statement7 = this.con.prepareStatement(resultado);
				statement7.setString(1, nemonico);
				//LOGGER.info(nemonico);
				System.out.println(nemonico +"   "+ contador++);
				xml = obtenerXMLResult(statement7, xml);
				break;
			case Constants.THIRDPARTIES:
				PreparedStatement statement8 = this.con.prepareStatement(resultado);
				statement8.setString(1, nemonico);
				//LOGGER.info(nemonico);
				System.out.println(nemonico +"   "+ contador++);
				marcadoQuery(this.con, resultado, "obtenerXMLResult");
				xml = obtenerXMLResult(statement8, xml);
				break;
			}


			ConDB.getInstance().returnCon(this.con);
		} catch (SQLException var10) {
			LOGGER.error(var10);
			LOGGER.error("*****Se ha producido un error en ObtenerQueryCpty********  " +nemonico + "  " + typeInfo);
			System.out.println("*****Se ha producido un error en ObtenerQueryCpty********  " +nemonico + "  " + typeInfo);
			ConDB.getInstance().returnCon(this.con);
		}

		return xml;
	}

	public ArrayList<String> obtenerQueryResults(String nemonico, File ficherosalida, String rutaFichero, FicheroExtraccion ficheroGrabar, String typeInfo) {
		ArrayList<String> results = new ArrayList<String>();
		try {
			switch (typeInfo){
			case Constants.DUCOCPTY:
				PreparedStatement statement = this.con.prepareStatement(resultado);
				statement.setString(1, nemonico);
				System.out.println(nemonico +"   "+ contador++);
				marcadoQuery(this.con, resultado, "obtenerResultados");
				results.addAll(obtenerResultados(statement));
				break;
			case Constants.DOMI:
				PreparedStatement statement2 = this.con.prepareStatement(resultado);
				statement2.setString(1, nemonico);
				statement2.setString(2, nemonico);
				System.out.println(nemonico +"   "+ contador++);
				marcadoQuery(this.con, resultado, "obtenerResultados");
				results.addAll(obtenerResultados(statement2));
				break;
			}
			ConDB.getInstance().returnCon(this.con);
		} catch (SQLException e) {
			LOGGER.error(e);
			LOGGER.error("*****Se ha producido un error en obtenerQueryResults********  " +nemonico + "  " + typeInfo);
			System.out.println("*****Se ha producido un error en obtenerQueryResults********  " +nemonico + "  " + typeInfo);
			ConDB.getInstance().returnCon(this.con);
		}

		return results;
	}

	private String obtenerXMLResult(PreparedStatement statement3, String xml) throws SQLException {

		for(ResultSet rs = statement3.executeQuery(); rs.next(); xml = rs.getString("XMLRESULT")) {
		}

		return xml;
	}

	//metodo para obtener resultados multiples lineas
	private ArrayList<String> obtenerResultados(PreparedStatement statement) throws SQLException {
		ArrayList<String> results = new ArrayList<String>();
		ResultSet queryResults = statement.executeQuery();
		while (queryResults.next()) {
			results.add(queryResults.getString("RESULT"));
		}
		return results;
	}

	public ArrayList<String> obtenerEtiquetas(Connection connection, String typeInfo) throws SQLException {
		PreparedStatement statement= null; 
		ResultSet rs = null;
		try {

			switch (typeInfo){
			case Constants.SSIS:
				//PreparedStatement statement2 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<SettInstructions>' and PAR1_VALUE = '</SettInstructions>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSSIs.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSSIs.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				//marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSSIs.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);

				break;
			case Constants.SCIS:
				//PreparedStatement statement3 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ConfInstructions>' and PAR1_VALUE = '</ConfInstructions>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSCIs.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSCIs.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				//marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaSCIs.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;
			case Constants.CONTC:
				//PreparedStatement statement4 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ContactList>' and PAR1_VALUE = '</ContactList>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONT.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONT.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				//marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONT.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;
			case Constants.CONTR:
				//PreparedStatement statement5 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ROOT>' and PAR1_VALUE = '</ROOT>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				//marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;

			case Constants.BASKETS:
				//PreparedStatement statement5 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ROOT>' and PAR1_VALUE = '</ROOT>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaBASKETS.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaBASKETS.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");	
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;
			case Constants.DUCOCPTY:
				//PreparedStatement statement5 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ROOT>' and PAR1_VALUE = '</ROOT>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'HEADER' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionAdhocDUCOCPTY.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'HEADER' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionAdhocDUCOCPTY.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");	
				rs = statement.executeQuery();
				ponerCabeceraPar1(rs);
				break;
			case Constants.DOMI:
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'HEADER' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionDominiosContactos.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'HEADER' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionDominiosContactos.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");	
				rs = statement.executeQuery();
				ponerCabeceraPar1(rs);
				break;
			case Constants.CONTRBBVA:
				//PreparedStatement statement5 = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and PAR1_NME = '<ROOT>' and PAR1_VALUE = '</ROOT>' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTRBBVA.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				//marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaCONTR.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;
			case Constants.THIRDPARTIES:
				statement = connection.prepareStatement("select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaTHIRDPARTIES.sql') AND DATA_STAT_TYP = 'ACTIVE'");
				marcadoQuery(this.con, "select * from ft_t_par1 where PARAMETER_CTXT_TYP = 'ROOT_TAG' and ACT1_OID = (SELECT ACT1_OID FROM KYTL_GC.FT_T_ATE1 WHERE ACTION_NME = 'ExtraccionContingenciaTHIRDPARTIES.sql') AND DATA_STAT_TYP = 'ACTIVE'", "obtenerEtiquetas");	
				rs = statement.executeQuery();
				ponerEtiquetasPar1(rs);
				break;	
			}

		}catch (SQLException var6) {
			LOGGER.error(var6);
		}finally{
			rs.close();
			statement.close();
		}

		return totaletiquetas;
	}

	private void ponerEtiquetasPar1(ResultSet rs) throws SQLException {
		while(rs.next()) {
			String etiqueta1 = rs.getString("PAR1_NME");
			String etiqueta2 = rs.getString("PAR1_VALUE");
			totaletiquetas.add(etiqueta1);
			totaletiquetas.add(etiqueta2);
		}
	}

	private void ponerCabeceraPar1(ResultSet rs) throws SQLException {
		while(rs.next()) {
			String etiqueta1 = rs.getString("PAR1_VALUE_CLOB");
			totaletiquetas.add(etiqueta1);

		}
	}


	public void ejecutar(Connection connection, Statement statement, String nemonico) {
		try {
			if (contadorInserciones != 0 && contadorInserciones % 1000 == 0) {
				LOGGER.error("He tratado " + contadorInserciones + " registros.");
				int porcentaje = contadorInserciones * 100 / totalRegistros;
				LOGGER.error("Porcentaje: " + porcentaje + "%");
				LOGGER.info("Porcentaje: " + porcentaje + "% ---Inserciones en RLT1: " + insercionesRLT1);
			}
		} catch (Exception var5) {
			LOGGER.error("Fallo al realizar el proceso");
		}

	}

	public String obtenerTiempo(String metodo, long start, long end) {
		long res = end - start;
		long second = res / 1000L % 60L;
		long minute = res / 60000L % 60L;
		long hour = res / 3600000L % 24L;
		String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, res);
		return time;
	}

	public static int getTotalRegistros() {
		return totalRegistros;
	}

	public static void setTotalRegistros(int totalRegistros) {
		Querys.totalRegistros = totalRegistros;
	}

	public synchronized void ejecutarQuery(Connection connection, String queryEjecutada) {
		try {
			Statement stmnt = connection.createStatement();
			stmnt.executeUpdate(queryEjecutada);
			stmnt.close();
			if (queryEjecutada.contains("insert")) {
				++contadorInsercionesRLT1;
			}
		} catch (SQLException var4) {
			LOGGER.error(var4);
		}

	}

	public String obtenerFichero(Connection con, String typeInfo) throws SQLException {

		PreparedStatement statement = null;
		ResultSet rs = null;
		String nombreFichero = null;
		try{
			if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.SSIS)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaSSIs.sql'");
				//marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaSSIs.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.SCIS)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaSCIs.sql'");
				//marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaSCIs.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.CONTC)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONT.sql'");
				//marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONT.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.BASKETS)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaBASKETS.sql'");
				marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaBASKETS.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.DUCOCPTY)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionAdhocDUCOCPTY.sql'");
				marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionAdhocDUCOCPTY.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.DOMI)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionDominiosContactos.sql'");
				marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionDominiosContactos.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.CONTRBBVA)){
					statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONTRBBVA.sql'");
					marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONTRBBVA.sql'", "obtenerFichero");
			}else if(typeInfo != null && typeInfo.trim().equalsIgnoreCase(Constants.THIRDPARTIES)){
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaTHIRDPARTIES.sql'");
				marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaTHIRDPARTIES.sql'", "obtenerFichero");		
			}else{
				statement = con.prepareStatement("select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONTR.sql'");
				//marcadoQuery(this.con, "select URL_OUTPUT_FILE from ft_t_ate1 where action_nme = 'ExtraccionContingenciaCONTR.sql'", "obtenerFichero");
			}

			for(rs = statement.executeQuery(); rs.next(); nombreFichero = rs.getString("URL_OUTPUT_FILE")) {

			}

		} catch(SQLException ex){
			LOGGER.error(ex);
		} finally{
			rs.close();
			statement.close();
		}

		return nombreFichero;
	}
	public static void marcadoQuery(Connection conexion, String query, String metodo){
		//marcado de query
		try {
			CallableStatement call = conexion.prepareCall("begin DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); end;");
			try {
				call.setString(1, "ExtraccionGenericaOtherEntities");
				call.setString(2,query);
				call.execute();
			} finally {
				call.close();
			}
		} catch (SQLException ex) {
			String msg = "ExtraccionGenericaOtherEntities::QueryExec::"+metodo+"::Fallo marcado de queries "+ ex.toString() + "\n" + query;
			System.out.println(msg);
			LOGGER.error(msg);
		}//fin marcado de query
	}
}