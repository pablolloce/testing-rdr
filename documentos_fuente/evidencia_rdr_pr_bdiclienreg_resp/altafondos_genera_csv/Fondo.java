package peticiones;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

import csv.CSVLine;
import jdbc.QueryExec;
import jdbc.QuerysStr;
import main.Main;
import tools.DateUtil;

public class Fondo {
	private Connection conexion;
	private String oidFondo;
	private String LEIFondo;
	private String IdPeticion;
	private String estado;
	private String descError;
	private boolean validFund = false;
	private HashMap<String, Vector<String>>atributos;
	
	
	private int numOficinas = 0;
	private int numBranches = 0;
	
	private CSVLine csvline;
	
	public Fondo (Connection conexion, String oidFondo, String LEIFondo) {
		this.conexion=conexion;
		this.oidFondo=oidFondo;
		this.LEIFondo=LEIFondo;
		this.estado  = "INICIO";
		this.descError = "";
		this.validFund = false;
	}
	
	public void procesaFondo() {
		
		validFund = true;
		
		Statement st = null;
		String msg = "\nFondo::procesaFondo::Analizando LEI: "+this.LEIFondo;
		System.out.println(msg);
		Main.LOGGER.info(msg);
		
		try {
			
			st = conexion.createStatement();
			String q = QuerysStr.updateVREQStatusByOid(this.oidFondo, "GENERATING_CSV_LINE", "ALTA_FONDOS_CSV");
			st.executeUpdate(q);
			
			msg = "Fondo::procesaFondo::Descargando atributos del fondo...";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			atributos = new HashMap<String, Vector<String>>();
			Vector<String[]>atrs = QueryExec.queryToVector(this.conexion, QuerysStr.selectFondosAtributos(this.oidFondo));
			for(int i=0;i<atrs.size();i++){
				String key = atrs.get(i)[0].trim();
				String val = atrs.get(i)[1].trim();
				if(this.atributos.get(key)==null){
					Vector<String> v = new Vector<String>();
					v.add(val);
					this.atributos.put(key, v);
				}else{
					Vector<String> v = this.atributos.get(key);
					v.add(val);
					this.atributos.put(key, v);
				}
				
			}
			msg = "Fondo::procesaFondo::Descargados "+this.atributos.size()+" atributos";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			
			this.LEIFondo   = atributos.get("LEI_CODE"  ).get(0);
			this.IdPeticion = atributos.get("IDPETICION").get(0);
			
			msg = "Fondo::procesaFondo::Atributos clave - LEI: "+this.LEIFondo+" - IdPeticion: "+this.IdPeticion;
			System.out.println(msg);
			Main.LOGGER.info(msg);
			
			mapeaCampos();
			
			if(validFund == false){
				q=QuerysStr.updateVREQDescripByOid(oidFondo, "ERROR_CSV_LINE_GEN", "ALTA_FONDOS_CSV", descError);
				st.executeUpdate(q);
			}else{
				msg = "Fondo::procesaFondo::Campos mapeados. Se generara linea de CSV.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				q=QuerysStr.updateVREQDescripByOid(oidFondo, "GENERATED_CSV_LINE", "ALTA_FONDOS_CSV", msg);
				st.executeUpdate(q);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			this.validFund = false;
			msg = "Fondo::procesaFondo::ERROR::Fallo al procesar la respuesta de alta de cliente para el fondo. "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			descError = msg;
			/*try {
				String q = QuerysStr.updateVREQStatusByOid(this.oidFondo, "ERROR_CLI_REG_RESP", "INVESTORS_CLI_REG_RESP");
				st.executeUpdate(q);
			} catch (Exception e2) {
				// TODO: handle exception
			}*/
		}finally {
			try {
				st.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
	}	
	
	
	private void mapeaCampos(){
		String msg = "Fondo::mapeaCampos::Realizando mapeo de campos.";
		System.out.println(msg);
		Main.LOGGER.info(msg);
		
		String splitter = "";
		Statement st = null;
		
		try {			
			st = conexion.createStatement();
			String q = QuerysStr.selectSplitter();
			
			ResultSet rs = st.executeQuery(q);
			if (rs.next()) {
				splitter = rs.getString("PAR1_VALUE");
			} 
			
			msg = "Fondo::mapeaCampos::Obteniendo splitter " + splitter;
			System.out.println(msg);
			Main.LOGGER.info(msg);
		} catch (Exception e) {
			msg = "Fondo::mapeaCampos::ERROR::Fallo al obtener splitter "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			descError = msg;
		}finally {
			try {
				st.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
				
		csvline = new CSVLine(splitter);
		
		try {
			
			if (atributos.get("CPTY_TYP")!=null){
			    if(atributos.get("CPTY_TYP").get(0)!=null){
			        csvline.setGL_07(atributos.get("CPTY_TYP").get(0));
			    }
			}
			if (atributos.get("COUNTRY")!=null){
			    if(atributos.get("COUNTRY").get(0)!=null){
			        csvline.setGL_12(atributos.get("COUNTRY").get(0));
			    }
			}
			if (atributos.get("LAUNCHDATE")!=null){
			    if(atributos.get("LAUNCHDATE").get(0)!=null){
			        csvline.setGL_04(atributos.get("LAUNCHDATE").get(0));
			    }
			}
			if (atributos.get("LEI_CODE")!=null){
			    if(atributos.get("LEI_CODE").get(0)!=null){
			        csvline.setGL_09_01_01("LEIID");
			        csvline.setGL_09_01_02(atributos.get("LEI_CODE").get(0));
			    }
			}
			if (atributos.get("USPERSON")!=null){
			    if(atributos.get("USPERSON").get(0)!=null){
			        csvline.setGL_14_01_01("DFA");
			        csvline.setGL_14_01_02("USPERSON");
			        csvline.setGL_14_01_03(atributos.get("USPERSON").get(0));
			    }
			}
			if (atributos.get("SFTR") != null){
				if(atributos.get("SFTR").get(0)!=null){
			        csvline.setGL_14_02_01("SFTR");
			        csvline.setGL_14_02_02("MANUALSFTR");
			        csvline.setGL_14_02_03(atributos.get("SFTR").get(0));
				}
			}
	
			if (atributos.get("NAME")!=null){
			    if(atributos.get("NAME").get(0)!=null){
			        csvline.setGL_03(atributos.get("NAME").get(0));
			    }
			}
			if (atributos.get("PERSONALIT")!=null){
			    if(atributos.get("PERSONALIT").get(0)!=null){
			        csvline.setGL_05(atributos.get("PERSONALIT").get(0));
			    }
			}
			if (atributos.get("ENTR_OWN")!=null){
			    if(atributos.get("ENTR_OWN").get(0)!=null){
			        csvline.setGL_10(atributos.get("ENTR_OWN").get(0));
			    }
			}
			if (atributos.get("BRANCH_OWN")!=null){
			    if(atributos.get("BRANCH_OWN").get(0)!=null){
			        csvline.setGL_11(atributos.get("BRANCH_OWN").get(0));
			    }
			}
			if (atributos.get("BANK_IND")!=null){
			    if(atributos.get("BANK_IND").get(0)!=null){
			        csvline.setGL_08(atributos.get("BANK_IND").get(0));
			    }
			}
			if (atributos.get("LEGAL_REG")!=null){
			    if(atributos.get("LEGAL_REG").get(0)!=null){
			        csvline.setGL_13(atributos.get("LEGAL_REG").get(0));
			    }
			}
			
			if (atributos.get("ADDRESS")!=null){
			    if(atributos.get("ADDRESS").get(0)!=null){
			        csvline.setLO_04(atributos.get("ADDRESS").get(0));
			    }
			}
			if (atributos.get("CITY_DISTR")!=null){
			    if(atributos.get("CITY_DISTR").get(0)!=null){
			        csvline.setLO_11(atributos.get("CITY_DISTR").get(0));
			    }
			}
			if (atributos.get("COUNTRY")!=null){
			    if(atributos.get("COUNTRY").get(0)!=null){
			        csvline.setLO_07(atributos.get("COUNTRY").get(0));
			    }
			}
			if (atributos.get("NAME")!=null){
			    if(atributos.get("NAME").get(0)!=null){
			    	String nme = atributos.get("NAME").get(0);
			    	if(nme.length()>61){
			    		System.out.println("Cambiando nombre de "+nme+" a '"+nme.substring(0, 60)+"'");
			    		nme = nme.substring(0, 60);
			    	}
			        csvline.setLO_03(nme);
			    }
			}
			if (atributos.get("ENTR_OWN")!=null){
			    if(atributos.get("ENTR_OWN").get(0)!=null){
			        csvline.setLO_18(atributos.get("ENTR_OWN").get(0));
			    }
			}
			
			if (atributos.get("CERTNORES")!=null){
			    if(atributos.get("CERTNORES").get(0)!=null){
			    	if("N".equals(atributos.get("CERTNORES").get(0))==false){
			    		csvline.setLO_14(atributos.get("CERTNORES").get(0));
			    		csvline.setLO_15(DateUtil.FechaSistemaGSString());
			    	}	
			    }
			}
			
			
			if (atributos.get("PASAPORTE")!=null){
			    if(atributos.get("PASAPORTE").get(0)!=null){
			        csvline.setLO_19_02_01("PASAP");
			        csvline.setLO_19_02_02(atributos.get("PASAPORTE").get(0));
			    }
			}
			/*if (atributos.get("CIFEX")!=null){
			    if(atributos.get("CIFEX").get(0)!=null){
			        csvline.setLO_19_03_01("CIFEX");
			        csvline.setLO_19_03_02(atributos.get("CIFEX").get(0));
			    }
			}*/
			if (atributos.get("CCLIENT")!=null){
			    if(atributos.get("CCLIENT").get(0)!=null){
			        csvline.setLO_20_01_01("CLIENTELAID");
			        csvline.setLO_20_01_02(atributos.get("CCLIENT").get(0));
			    }
			}
			
			if (atributos.get("NAME")!=null){
			    if(atributos.get("NAME").get(0)!=null){
			    	String name = atributos.get("NAME").get(0);
			    	if(name.length()>61){
			    		name = name.substring(0,60);
			    	}
			        csvline.setLO_03(name);
			    }
			}
			
			if (atributos.get("CPTY_DESC")!=null){
				if(atributos.get("CPTY_DESC").get(0)!=null){
					csvline.setOP_01(atributos.get("CPTY_DESC").get(0));
				}
			}
			if (atributos.get("DES_DISPLA")!=null){ //ANTES CITY_DISTR
			    if(atributos.get("DES_DISPLA").get(0)!=null){
			        csvline.setOP_14(atributos.get("DES_DISPLA").get(0));
			    }
			}
			if (atributos.get("POSTAL_CDE")!=null){
			    if(atributos.get("POSTAL_CDE").get(0)!=null){
			        csvline.setOP_14_01(atributos.get("POSTAL_CDE").get(0));
			    }
			}
			if (atributos.get("PLAZA")!=null){
			    if(atributos.get("PLAZA").get(0)!=null){
			        csvline.setOP_10(atributos.get("PLAZA").get(0));
			    }
			}
			if (atributos.get("NAME")!=null){
			    if(atributos.get("NAME").get(0)!=null){
			    	if(atributos.get("NAME").get(0).length()>61){
			    		csvline.setOP_41(atributos.get("NAME").get(0).substring(0, 60));
			    	}else{
			    		csvline.setOP_41(atributos.get("NAME").get(0));
			    	}
			        
			    }
			}
			if (atributos.get("DB_LOC")!=null){
			    if(atributos.get("DB_LOC").get(0)!=null){
			        csvline.setOP_07(atributos.get("DB_LOC").get(0));
			    }
			}
			if (atributos.get("BDI_CODE")!=null){
				if(atributos.get("BDI_CODE").get(0)!=null){
			        csvline.setOP_08_01_01("BDIID");
			        //Limitamos la longitud del codigo BDI a 6 caracteres
			        String bdiid = atributos.get("BDI_CODE").get(0);
			        String bdiid_ok = bdiid.substring(bdiid.length()-6, bdiid.length());
			        csvline.setOP_08_01_02(bdiid_ok);
			        
			    }
			}
			if (atributos.get("COD_TES")!=null){
				if(atributos.get("COD_TES").get(0)!=null){
			        csvline.setOP_08_02_01("CODTESID");
			        csvline.setOP_08_02_02(atributos.get("COD_TES").get(0));
			    }
			}
			if (atributos.get("CIFEX")!=null){
				if(atributos.get("CIFEX").get(0)!=null){
			        csvline.setOP_08_03_01("CIFEX");
			        csvline.setOP_08_03_02(atributos.get("CIFEX").get(0));
			    }
			}
			if (atributos.get("SHORTNME")!=null){
			    if(atributos.get("SHORTNME").get(0)!=null){
			        csvline.setOP_09(atributos.get("SHORTNME").get(0));
			    }
			}
			if (atributos.get("SUBSID_IND")!=null){
			    if(atributos.get("SUBSID_IND").get(0)!=null){
			        csvline.setOP_02(atributos.get("SUBSID_IND").get(0));
			    }
			}
			if (atributos.get("LANGUAGE")!=null){
			    if(atributos.get("LANGUAGE").get(0)!=null){
			        csvline.setOP_03(atributos.get("LANGUAGE").get(0));
			    }
			}
			if (atributos.get("REG_NUMBER")!=null){
			    if(atributos.get("REG_NUMBER").get(0)!=null){
			        csvline.setOP_04(atributos.get("REG_NUMBER").get(0));
			    }
			}
			if (atributos.get("REGBODYCDE")!=null){
			    if(atributos.get("REGBODYCDE").get(0)!=null){
			        csvline.setOP_05(atributos.get("REGBODYCDE").get(0));
			    }
			}
			if (atributos.get("ADDRESS")!=null){
			    if(atributos.get("ADDRESS").get(0)!=null){
			        csvline.setOP_11(atributos.get("ADDRESS").get(0));
			    }
			}
			if (atributos.get("PROVINCE")!=null){
			    if(atributos.get("PROVINCE").get(0)!=null){
			        csvline.setOP_12(atributos.get("PROVINCE").get(0));
			    }
			}
			if (atributos.get("COUNTRY")!=null){
			    if(atributos.get("COUNTRY").get(0)!=null){
			        csvline.setOP_13(atributos.get("COUNTRY").get(0));
			    }
			}
			if (atributos.get("HEREDA_MAN")!=null){
			    if(atributos.get("HEREDA_MAN").get(0)!=null){
			        csvline.setOP_29(atributos.get("HEREDA_MAN").get(0));
			    }
			}
			if (atributos.get("COD_ISIN")!=null){
			    if(atributos.get("COD_ISIN").get(0)!=null){
			        csvline.setOP_06(atributos.get("COD_ISIN").get(0));
			    }
			}
			//---------
			if (atributos.get("INIT_ROOM")!=null){
			    if(atributos.get("INIT_ROOM").get(0)!=null){
			        csvline.setOP_15_01_01("0182");
			        csvline.setOP_15_01_02("SALAMAD");
			        csvline.setOP_15_01_03(atributos.get("INIT_ROOM").get(0));
			    }
			}
			if (atributos.get("CLSMEMBER")!=null){
			    if(atributos.get("CLSMEMBER").get(0)!=null){
			    	csvline.setOP_15_02_01("0182");
			        csvline.setOP_15_02_02("CLS_MAD");
			        csvline.setOP_15_02_03(atributos.get("CLSMEMBER").get(0));
			    }
			}
			if (atributos.get("FO_CPTY")!=null){
			    if(atributos.get("FO_CPTY").get(0)!=null){
			    	csvline.setOP_15_03_01("0182");
			        csvline.setOP_15_03_02("TYPFOMAD");
			        csvline.setOP_15_03_03(atributos.get("FO_CPTY").get(0));
			    }
			}

			if (atributos.get("ALERT_CODE")!=null){
			    if(atributos.get("ALERT_CODE").get(0)!=null){
			        csvline.setOP_24_01_01("ACCESS_CODE");
			        csvline.setOP_24_01_02(atributos.get("ALERT_CODE").get(0));
			        csvline.setOP_24_01_03("ACCDE");
			    }
			}
			
			if (atributos.get("COD_STAR")!=null){
			    if(atributos.get("COD_STAR").get(0)!=null){
			        csvline.setOP_24_02_01("STAR_MADRID");
			        csvline.setOP_24_02_02(atributos.get("COD_STAR").get(0));
			        csvline.setOP_24_02_03("STARID");
			    }
			}
			
			if (atributos.get("COD_MUREX")!=null){
			    if(atributos.get("COD_MUREX").get(0)!=null){
			        csvline.setOP_24_03_01("MUREX");
			        csvline.setOP_24_03_02(atributos.get("COD_MUREX").get(0));
			        csvline.setOP_24_03_03("MUREXID");
			    }
			}
			
			if (atributos.get("ACRONYM")!=null){
			    if(atributos.get("ACRONYM").get(0)!=null){
			        csvline.setOP_24_04_01("ALERT");
			        csvline.setOP_24_04_02(atributos.get("ACRONYM").get(0));
			        csvline.setOP_24_04_03("ALERTID");
			    }
			}
			
			if (atributos.get("CTMID")!=null){
			    if(atributos.get("CTMID").get(0)!=null){
			        csvline.setOP_24_05_01("CTM");
			        csvline.setOP_24_05_02(atributos.get("CTMID").get(0));
			        csvline.setOP_24_05_03("CTMID");
			    }
			}
			if (atributos.get("SWIFT")!=null){
			    if(atributos.get("SWIFT").get(0)!=null){
			        csvline.setOP_24_06_01("SWIFT");
			        csvline.setOP_24_06_02(atributos.get("SWIFT").get(0));
			        csvline.setOP_24_06_03("SWIFTID");
			    }
			}

			
			if (atributos.get("MANAFINSID")!=null){
			    if(atributos.get("MANAFINSID").get(0)!=null){
			        csvline.setOP_26(atributos.get("MANAFINSID").get(0));
			    }
			}
			
			if (atributos.get("FUND_DESC")!=null){
			    if(atributos.get("FUND_DESC").get(0)!=null){
			        csvline.setOP_28(atributos.get("FUND_DESC").get(0));
			    }
			}
			
			
			if(atributos.get("BRANCH")!=null){
				for(int i=0;i<atributos.get("BRANCH").size();i++){
					csvline.addBranch(atributos.get("BRANCH").get(i));
				}
			}
			
			if(atributos.get("OFFICE")!=null){
				for(int i=0;i<atributos.get("OFFICE").size();i++){
					String []vals = atributos.get("OFFICE").get(i).split("\\|");
					csvline.addOffice(vals);
				}
			}
			
			
			numBranches = csvline.maxBranch();
			numOficinas = csvline.maxOffice();
			
			
			msg = "Fondo::mapeaCampos::Campos mapeados.";
			System.out.println(msg);
			Main.LOGGER.info(msg);
			validFund   = true;
		} catch (Exception e) {
			// TODO: handle exception
			msg = "Fondo::mapeaCampos::Fallo al realizar el mapeo de campos. "+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
			descError = msg;
			validFund   = false;
		}
		
		
		
	}
	
	public String getEstado() {
		return estado;
	}
	public boolean isValidFund() {
		return validFund;
	}
	public int getNumOficinas() {
		return numOficinas;
	}
	public int getNumBranches() {
		return numBranches;
	}
	public CSVLine getCsvline() {
		return csvline;
	}
	public String getDescError() {
		return descError;
	}
	
	
}
