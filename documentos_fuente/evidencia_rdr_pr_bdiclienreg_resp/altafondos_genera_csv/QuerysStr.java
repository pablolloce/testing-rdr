package jdbc;

/**
 * Clase que se encarga de crear las querys y devolverlas en formato String.
 * 
 * @author XE83997
 *
 */
public class QuerysStr {
	public static String selectFondosPosibles() {
		return   "SELECT VND_RQST_OID     ,                    \n"
				+"       VND_RQST_XREF_ID                      \n"
				+"  FROM FT_T_VREQ                             \n"
				+" WHERE VND_RQST_XREF_ID_CTXT_TYP = 'FundLEI' \n"
				+"   AND VND_RQST_STAT_TYP = 'ALTA_FONDO_PEND' \n"
				+" AND VND_RQST_CORR_ID != 'DigitalCrossSelling' \n";
	}
	public static String selectFondosPosiblesDCS() {
		return   "SELECT VND_RQST_OID     ,                    \n"
				+"       VND_RQST_XREF_ID                      \n"
				+"  FROM FT_T_VREQ                             \n"
				+" WHERE VND_RQST_XREF_ID_CTXT_TYP = 'FundLEI' \n"
				+"   AND VND_RQST_STAT_TYP = 'ALTA_FONDO_PEND' \n"
				+" AND VND_RQST_CORR_ID = 'DigitalCrossSelling' \n";
	}
	
	
	public static String selectSplitter() {
		return "select par1_value from ft_t_PAR1 where PARAMETER_CTXT_TYP = 'STR_SPLIT' and par1_nme = 'STR_SPLIT_FONDOS'";
	}
	
	public static String selectFundsPendientes(String oidPeticion) {
		return       "SELECT b.VND_RQST_OID,                                     \n" 
		            +"       b.VND_RQST_XREF_ID                                  \n"
		            +"  FROM FT_T_VREQ a                                         \n"
		            +"  JOIN FT_T_VREQ b                                         \n"
		            +"    ON a.VND_RQST_OID = b.BASIC_VND_RQST_OID               \n"
		            +"   AND b.VND_RQST_XREF_ID_CTXT_TYP = 'FundLEI'             \n"
		            +"   AND b.VND_RQST_STAT_TYP = 'NEW_CLIENT'                  \n"
		            +"  LEFT JOIN FT_T_VREQ c                                    \n"
		            +"    ON c.VND_RQST_XREF_ID = b.VND_RQST_XREF_ID             \n"
		            +"   AND c.VND_RQST_XREF_ID_CTXT_TYP = 'LEI_REQUEST'         \n"
		            +"   AND c.VND_RQST_STAT_TYP = 'NEW_CLIENTS'                 \n"
		            +" WHERE a.VND_RQST_XREF_ID_CTXT_TYP = 'FILE_DATE'           \n"
		            +"   AND a.VND_RQST_OID = '"+oidPeticion+"'                  \n"
		            +" GROUP BY b.VND_RQST_OID, b.VND_RQST_XREF_ID               \n";
	}
	
	public static String selectRespuestaBDI(String LEI, String HORA){
		
		return   " SELECT z.VND_RQST_OID                                        \n"
				+"   FROM FT_T_VREQ z                                           \n"
				+"   JOIN FT_T_UTD1 y                                           \n"
				+"     ON z.VND_RQST_OID = y.UTD_EXT_ID                         \n"
				+"    AND y.UTD_USAGE_TYP = 'FIELD_RESP'                        \n"
				+"    AND y.UTD_ID_PURP_TYP = 'HORA'                            \n"
				+"   JOIN FT_T_UTD1 x                                           \n"
				+"     ON z.VND_RQST_OID = x.UTD_EXT_ID                         \n"
				+"    AND x.UTD_USAGE_TYP = 'FIELD_RESP'                        \n"
				+"    AND x.UTD_ID_PURP_TYP = 'LEI'                             \n"
				+"  WHERE z.VND_RQST_XREF_ID_CTXT_TYP IN ('CLIENTELABDI_ALTAS') \n"
				+"    AND y.UTD_ID = '"+HORA+"'                                 \n"
				+"    AND x.UTD_ID = '"+LEI+"'                                  \n"
				
				;
	}
	
	public static String getStatusVREQ (String oid){
		return "SELECT VND_RQST_STAT_TYP            \n"
			  +"  FROM FT_T_VREQ                    \n"
			  +" WHERE VND_RQST_OID = '"+oid+"'     \n";
	}
	
	public static String updateVREQStatusByOid(String oid, String stat, String user) {
		return "UPDATE FT_T_VREQ                                    \n"
				  +"   SET VND_RQST_STAT_TYP = '"+stat+"'      ,    \n"
				  +"       LAST_CHG_USR_ID = '"+user+"'        ,    \n"
				  +"       LAST_CHG_TMS = SYSDATE                   \n"
				  +" WHERE VND_RQST_OID      = '"+oid+"'            \n";

	}
	public static String updateVREQDescripByOid(String oid, String stat, String user, String descrip) {
		return "UPDATE FT_T_VREQ                                    \n"
				  +"   SET VND_RQST_STAT_TYP = '"+stat+"'      ,    \n"
				  +"       LAST_CHG_USR_ID = '"+user+"'        ,    \n"
				  +"       LAST_CHG_TMS = SYSDATE              ,    \n"
				  +"       VND_RQST_STAT_TXT = '"+descrip+"'        \n"
				  +" WHERE VND_RQST_OID      = '"+oid+"'            \n";

	}
	public static String descargaAtributosRespuesta(String oid){
		return "SELECT a.UTD_ID_PURP_TYP,                     \n"
			  +"       a.UTD_ID                               \n"
			  +"  FROM FT_T_UTD1 a                            \n"
			  +" WHERE a.UTD_USAGE_TYP = 'FIELD_RESP'         \n"
			  +"   AND a.UTD_EXT_ID = '"+oid+"'               \n";
	}
	public static String selectFondosAtributos(String vreqOid){
		return "SELECT UTD_ID_PURP_TYP, UTD_ID FROM FT_T_UTD1 WHERE UTD_EXT_ID = '"+vreqOid+"'";
	}
	public static String insertVREQ_BDIClient_Req(String oid, String LEI){
		return "INSERT INTO FT_T_VREQ (VND_RQST_OID             ,   \n"
			  +"                       VND_RQST_XREF_ID_CTXT_TYP,   \n"
			  +"                       VND_RQST_XREF_ID         ,   \n"
			  +"                       VND_RQST_STAT_TYP        ,   \n"
			  +"                       LAST_CHG_TMS             ,   \n"
			  +"                       LAST_CHG_USR_ID          )   \n"
			  +"  VALUES ('"+oid+"'              ,                  \n"
			  +"         'CLIENTELABDI_ALTAS'    ,                  \n"
			  +"         '"+LEI+"'               ,                  \n"
			  +"         'PENDING'               ,                  \n"
			  +"         SYSDATE                 ,                  \n"
			  +"         'INVESTORS_LEI_REPONSE' )                  \n";
	}
	public static String getNewOid() {
		return "SELECT NEW_OID FROM DUAL";
	}
	public static String insertUTD1FundParam(String oidRef, String usageTyp, String key, String val, String src){
		String q = "INSERT INTO FT_T_UTD1 (UTD_OID, UTD_EXT_ID, UTD_USAGE_TYP, REG_NUM, UTD_ID_PURP_TYP, UTD_ID, LAST_CHG_TMS, LAST_CHG_USR_ID, DATA_SRC_ID, DATA_STAT_TYP, START_TMS)";
		       q+= " VALUES (NEW_OID, '"+oidRef+"', '"+usageTyp+"', 1, '"+key+"', '"+val+"', SYSDATE, 'INVESTORSPLAN_FUNDS', '"+src+"', 'ACTIVE', SYSDATE)";
		return q;
	}
}
