package csv;

import java.util.Vector;

import main.Main;

public class CSVLine {
	private String GL_01          = "";
	private String GL_02          = "";
	private String GL_03          = "";
	private String GL_04          = "";
	private String GL_05          = "";
	private String GL_06          = "";
	private String GL_07          = "";
	private String GL_08          = "";
	private String GL_09_01_01    = "";
	private String GL_09_01_02    = "";
	private String GL_09_02_01    = "";
	private String GL_09_02_02    = "";
	private String GL_09_03_01    = "";
	private String GL_09_03_02    = "";
	private String GL_09_04_01    = "";
	private String GL_09_04_02    = "";
	private String GL_10          = "";
	private String GL_11          = "";
	private String GL_12          = "";
	private String GL_13          = "";
	private String GL_14_01_01    = "";
	private String GL_14_01_02    = "";
	private String GL_14_01_03    = "";
	private String GL_14_02_01    = "";
	private String GL_14_02_02    = "";
	private String GL_14_02_03    = "";
	private String GL_14_03_01    = "";
	private String GL_14_03_02    = "";
	private String GL_14_03_03    = "";
	private String GL_14_04_01    = "";
	private String GL_14_04_02    = "";
	private String GL_14_04_03    = "";
	private String LO_01          = "";
	private String LO_02          = "";
	private String LO_03          = "";
	private String LO_04          = "";
	private String LO_05          = "";
	private String LO_06          = "";
	private String LO_07          = "";
	private String LO_08          = "";
	private String LO_09          = "";
	private String LO_10          = "";
	private String LO_11          = "";
	private String LO_12          = "";
	private String LO_13          = "";
	private String LO_14          = "";
	private String LO_15          = "";
	private String LO_16_01_01    = "";
	private String LO_16_01_02    = "";
	private String LO_16_01_03    = "";
	private String LO_16_02_01    = "";
	private String LO_16_01202    = "";
	private String LO_16_02_03    = "";
	private String LO_16_03_01    = "";
	private String LO_16_03_02    = "";
	private String LO_16_03_03    = "";
	private String LO_16_04_01    = "";
	private String LO_16_04_02    = "";
	private String LO_16_04_03    = "";
	private String LO_17          = "";
	private String LO_18          = "";
	private String LO_19_01_01    = "";
	private String LO_19_01_02    = "";
	private String LO_19_02_01    = "";
	private String LO_19_02_02    = "";
	private String LO_19_03_01    = "";
	private String LO_19_03_02    = "";
	private String LO_19_04_01    = "";
	private String LO_19_04_02    = "";
	private String LO_20_01_01    = "";
	private String LO_20_01_02    = "";
	private String LO_20_02_01    = "";
	private String LO_20_02_02    = "";
	private String LO_20_03_01    = "";
	private String LO_20_03_02    = "";
	private String LO_20_04_01    = "";
	private String LO_20_04_02    = "";
	private String LO_21          = "";
	private String LO_22          = "";
	private String LO_23          = "";
	private String LO_24          = "";
	private String LO_25          = "";
	private String LO_26          = "";
	private String LO_27          = "";
	private String LO_28          = "";
	private String LO_29          = "";
	private String LO_30          = "";
	private String LO_31          = "";
	private String LO_32          = "";
	private String OP_01          = "";
	private String OP_02          = "";
	private String OP_03          = "";
	private String OP_04          = "";
	private String OP_05          = "";
	private String OP_06          = "";
	private String OP_07          = "";
	private String OP_08_01_01    = "";
	private String OP_08_01_02    = "";
	private String OP_08_02_01    = "";
	private String OP_08_02_02    = "";
	private String OP_08_03_01    = "";
	private String OP_08_03_02    = "";
	private String OP_08_04_01    = "";
	private String OP_08_04_02    = "";
	private String OP_09          = "";
	private String OP_10          = "";
	private String OP_11          = "";
	private String OP_12          = "";
	private String OP_13          = "";
	private String OP_14          = "";
	private String OP_14_01    = "";
	private String OP_15_01_01    = "";
	private String OP_15_01_02    = "";
	private String OP_15_01_03    = "";
	private String OP_15_02_01    = "";
	private String OP_15_02_02    = "";
	private String OP_15_02_03    = "";
	private String OP_15_03_01    = "";
	private String OP_15_03_02    = "";
	private String OP_15_03_03    = "";
	private String OP_15_04_01    = "";
	private String OP_15_04_02    = "";
	private String OP_15_04_03    = "";
	private String OP_15_05_01    = "";
	private String OP_15_05_02    = "";
	private String OP_15_05_03    = "";
	private String OP_15_06_01    = "";
	private String OP_15_06_02    = "";
	private String OP_15_06_03    = "";
	
	private String OP_23_01_01    = "";
	private String OP_23_01_02    = "";
	private String OP_23_02_01    = "";
	private String OP_23_02_02    = "";
	private String OP_23_03_01    = "";
	private String OP_23_03_02    = "";
	private String OP_23_04_01    = "";
	private String OP_23_04_02    = "";
	private String OP_24_01_01    = "";
	private String OP_24_01_02    = "";
	private String OP_24_01_03    = "";
	private String OP_24_02_01    = "";
	private String OP_24_02_02    = "";
	private String OP_24_02_03    = "";
	private String OP_24_03_01    = "";
	private String OP_24_03_02    = "";
	private String OP_24_03_03    = "";
	private String OP_24_04_01    = "";
	private String OP_24_04_02    = "";
	private String OP_24_04_03    = "";
	private String OP_24_05_01    = "";
	private String OP_24_05_02    = "";
	private String OP_24_05_03    = "";
	private String OP_24_06_01    = "";
	private String OP_24_06_02    = "";
	private String OP_24_06_03    = "";
	private String OP_25_01_01    = "";
	private String OP_25_01_02    = "";
	private String OP_25_02_01    = "";
	private String OP_25_02_02    = "";
	private String OP_25_03_01    = "";
	private String OP_25_03_02    = "";
	private String OP_25_04_01    = "";
	private String OP_25_04_02    = "";
	private String OP_26          = "";
	private String OP_28          = "";
	private String OP_29          = "";
	private String OP_30_01_01    = "";
	private String OP_30_01_02    = "";
	private String OP_30_01_03    = "";
	private String OP_30_02_01    = "";
	private String OP_30_02_02    = "";
	private String OP_30_02_03    = "";
	private String OP_30_03_01    = "";
	private String OP_30_03_02    = "";
	private String OP_30_03_03    = "";
	private String OP_30_04_01    = "";
	private String OP_30_04_02    = "";
	private String OP_30_04_03    = "";
	private String OP_31          = "";
	private String OP_32          = "";
	private String OP_33          = "";
	private String OP_34          = "";
	private String OP_35_01       = "";
	private String OP_35_02       = "";
	private String OP_35_03       = "";
	private String OP_35_04       = "";
	private String OP_35_05       = "";
	private String OP_35_06       = "";
	private String OP_35_07       = "";
	private String OP_35_08       = "";
	private String OP_35_09       = "";
	private String OP_40          = "";
	private String OP_41          = "";
	private Vector<String []> oficinas;
	private Vector<String>    branches;
	
	private String str_split = "";
	
	public CSVLine(String splitter){
		oficinas = new Vector<String[]>();
		branches = new Vector<String>();
		this.str_split = splitter;
	}
	
	
	
	public String getCSVLine(int maxOf, int maxBranch){
		
		String r = "";
		
		
		r += GL_01           + str_split;
		r += GL_02           + str_split;
		r += GL_03           + str_split;
		r += GL_04           + str_split;
		r += GL_05           + str_split;
		r += GL_06           + str_split;
		r += GL_07           + str_split;
		r += GL_08           + str_split;
		r += GL_09_01_01     + str_split;
		r += GL_09_01_02     + str_split;
		r += GL_09_02_01     + str_split;
		r += GL_09_02_02     + str_split;
		r += GL_09_03_01     + str_split;
		r += GL_09_03_02     + str_split;
		r += GL_09_04_01     + str_split;
		r += GL_09_04_02     + str_split;
		r += GL_10           + str_split;
		r += GL_11           + str_split;
		r += GL_12           + str_split;
		r += GL_13           + str_split;
		r += GL_14_01_01     + str_split;
		r += GL_14_01_02     + str_split;
		r += GL_14_01_03     + str_split;
		r += GL_14_02_01     + str_split;
		r += GL_14_02_02     + str_split;
		r += GL_14_02_03     + str_split;
		r += GL_14_03_01     + str_split;
		r += GL_14_03_02     + str_split;
		r += GL_14_03_03     + str_split;
		r += GL_14_04_01     + str_split;
		r += GL_14_04_02     + str_split;
		r += GL_14_04_03     + str_split;
		r += LO_01           + str_split;
		r += LO_02           + str_split;
		r += LO_03           + str_split;
		r += LO_04           + str_split;
		r += LO_05           + str_split;
		r += LO_06           + str_split;
		r += LO_07           + str_split;
		r += LO_08           + str_split;
		r += LO_09           + str_split;
		r += LO_10           + str_split;
		r += LO_11           + str_split;
		r += LO_12           + str_split;
		r += LO_13           + str_split;
		r += LO_14           + str_split;
		r += LO_15           + str_split;
		r += LO_16_01_01     + str_split;
		r += LO_16_01_02     + str_split;
		r += LO_16_01_03     + str_split;
		r += LO_16_02_01     + str_split;
		r += LO_16_01202     + str_split;
		r += LO_16_02_03     + str_split;
		r += LO_16_03_01     + str_split;
		r += LO_16_03_02     + str_split;
		r += LO_16_03_03     + str_split;
		r += LO_16_04_01     + str_split;
		r += LO_16_04_02     + str_split;
		r += LO_16_04_03     + str_split;
		r += LO_17           + str_split;
		r += LO_18           + str_split;
		r += LO_19_01_01     + str_split;
		r += LO_19_01_02     + str_split;
		r += LO_19_02_01     + str_split;
		r += LO_19_02_02     + str_split;
		r += LO_19_03_01     + str_split;
		r += LO_19_03_02     + str_split;
		r += LO_19_04_01     + str_split;
		r += LO_19_04_02     + str_split;
		r += LO_20_01_01     + str_split;
		r += LO_20_01_02     + str_split;
		r += LO_20_02_01     + str_split;
		r += LO_20_02_02     + str_split;
		r += LO_20_03_01     + str_split;
		r += LO_20_03_02     + str_split;
		r += LO_20_04_01     + str_split;
		r += LO_20_04_02     + str_split;
		r += LO_21           + str_split;
		r += LO_22           + str_split;
		r += LO_23           + str_split;
		r += LO_24           + str_split;
		r += LO_25           + str_split;
		r += LO_26           + str_split;
		r += LO_27           + str_split;
		r += LO_28           + str_split;
		r += LO_29           + str_split;
		r += LO_30           + str_split;
		r += LO_31           + str_split;
		r += LO_32           + str_split;
		r += OP_01           + str_split;
		r += OP_02           + str_split;
		r += OP_03           + str_split;
		r += OP_04           + str_split;
		r += OP_05           + str_split;
		r += OP_06           + str_split;
		r += OP_07           + str_split;
		r += OP_08_01_01     + str_split;
		r += OP_08_01_02     + str_split;
		r += OP_08_02_01     + str_split;
		r += OP_08_02_02     + str_split;
		r += OP_08_03_01     + str_split;
		r += OP_08_03_02     + str_split;
		r += OP_08_04_01     + str_split;
		r += OP_08_04_02     + str_split;
		r += OP_09           + str_split;
		r += OP_10           + str_split;
		r += OP_11           + str_split;
		r += OP_12           + str_split;
		r += OP_13           + str_split;
		r += OP_14           + str_split;
		r += OP_14_01    	 + str_split;
		r += OP_15_01_01     + str_split;
		r += OP_15_01_02     + str_split;
		r += OP_15_01_03     + str_split;
		r += OP_15_02_01     + str_split;
		r += OP_15_02_02     + str_split;
		r += OP_15_02_03     + str_split;
		r += OP_15_03_01     + str_split;
		r += OP_15_03_02     + str_split;
		r += OP_15_03_03     + str_split;
		r += OP_15_04_01     + str_split;
		r += OP_15_04_02     + str_split;
		r += OP_15_04_03     + str_split;
		r += OP_15_05_01     + str_split;
		r += OP_15_05_02     + str_split;
		r += OP_15_05_03     + str_split;
		r += OP_15_06_01     + str_split;
		r += OP_15_06_02     + str_split;
		r += OP_15_06_03     + str_split;
		r += OP_23_01_01     + str_split;
		r += OP_23_01_02     + str_split;
		r += OP_23_02_01     + str_split;
		r += OP_23_02_02     + str_split;
		r += OP_23_03_01     + str_split;
		r += OP_23_03_02     + str_split;
		r += OP_23_04_01     + str_split;
		r += OP_23_04_02     + str_split;
		r += OP_24_01_01     + str_split;
		r += OP_24_01_02     + str_split;
		r += OP_24_01_03     + str_split;
		r += OP_24_02_01     + str_split;
		r += OP_24_02_02     + str_split;
		r += OP_24_02_03     + str_split;
		r += OP_24_03_01     + str_split;
		r += OP_24_03_02     + str_split;
		r += OP_24_03_03     + str_split;
		r += OP_24_04_01     + str_split;
		r += OP_24_04_02     + str_split;
		r += OP_24_04_03     + str_split;
		r += OP_24_05_01     + str_split;
		r += OP_24_05_02     + str_split;
		r += OP_24_05_03     + str_split;
		r += OP_24_06_01     + str_split;
		r += OP_24_06_02     + str_split;
		r += OP_24_06_03     + str_split;
		r += OP_25_01_01     + str_split;
		r += OP_25_01_02     + str_split;
		r += OP_25_02_01     + str_split;
		r += OP_25_02_02     + str_split;
		r += OP_25_03_01     + str_split;
		r += OP_25_03_02     + str_split;
		r += OP_25_04_01     + str_split;
		r += OP_25_04_02     + str_split;
		r += OP_26           + str_split;
		r += OP_28           + str_split;
		r += OP_29           + str_split;
		r += OP_30_01_01     + str_split;
		r += OP_30_01_02     + str_split;
		r += OP_30_01_03     + str_split;
		r += OP_30_02_01     + str_split;
		r += OP_30_02_02     + str_split;
		r += OP_30_02_03     + str_split;
		r += OP_30_03_01     + str_split;
		r += OP_30_03_02     + str_split;
		r += OP_30_03_03     + str_split;
		r += OP_30_04_01     + str_split;
		r += OP_30_04_02     + str_split;
		r += OP_30_04_03     + str_split;
		r += OP_31           + str_split;
		r += OP_32           + str_split;
		r += OP_33           + str_split;
		r += OP_34           + str_split;
		r += OP_35_01        + str_split;
		r += OP_35_02        + str_split;
		r += OP_35_03        + str_split;
		r += OP_35_04        + str_split;
		r += OP_35_05        + str_split;
		r += OP_35_06        + str_split;
		r += OP_35_07        + str_split;
		r += OP_35_08        + str_split;
		r += OP_35_09        + str_split;
		r += OP_40           + str_split;
		r += OP_41           + str_split;
		
		
		for(int i=0;i<maxBranch;i++){
			if(i<maxBranch()){
				r += branches.get(i)+str_split;
			}else{
				r += str_split;
			}
		}
		
		for(int i=0;i<maxOf;i++){
			if(i<maxOffice()){
				for(int j=0;j<oficinas.get(i).length;j++){
					r+=oficinas.get(i)[j]+str_split;
				}
			}else{
				r+=str_split+str_split+str_split+str_split+str_split+str_split;
			}
		}
		
		return r;
		
	}
	
	
	public int maxOffice(){
		return oficinas.size();
	}
	public int maxBranch(){
		return branches.size();
	}
	public void addOffice(String [] vals){
		String msg = "";
		try {
			if(vals.length<2){
				msg = "CSVLine::addOffice::Numero de parametros insuficientes para agregar la oficina.";
				System.out.println(msg);
				Main.LOGGER.info(msg);
				return;
			}
			
			String [] valores = new String [6];
			for(int i=0;i<valores.length;i++){
				if(vals.length>i){
					valores[i]=vals[i];
				}else{
					valores[i]="";
				}
			}
			
			oficinas.add(valores);
		} catch (Exception e) {
			// TODO: handle exception
			msg = "CSVLine::addOffice::ERROR::"+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
		}
	}
	public void addBranch(String val){
		String msg = "";
		try {
			branches.add(val);
		} catch (Exception e) {
			// TODO: handle exception
			msg = "CSVLine::addOffice::ERROR::"+e.toString();
			System.out.println(msg);
			Main.LOGGER.info(msg);
		}
	}
	
	public String getCabecera(int maxOf, int maxBra){
		String r = "GL.01"+str_split+"GL.02"+str_split+"GL.03"+str_split+"GL.04"+str_split+"GL.05"+str_split+"GL.06"+str_split+"GL.07"+str_split+"GL.08"+str_split+"GL.09.01.01"+str_split+"GL.09.01.02"+str_split+"GL.09.02.01"+str_split+"GL.09.02.02"+str_split+"GL.09.03.01"+str_split+"GL.09.03.02"+str_split+"GL.09.04.01"+str_split+"GL.09.04.02"+str_split+"GL.10"+str_split+"GL.11"+str_split+"GL.12"+str_split+"GL.13"+str_split+"GL.14.01.01"+str_split+"GL.14.01.02"+str_split+"GL.14.01.03"+str_split+"GL.14.02.01"+str_split+"GL.14.02.02"+str_split+"GL.14.02.03"+str_split+"GL.14.03.01"+str_split+"GL.14.03.02"+str_split+"GL.14.03.03"+str_split+"GL.14.04.01"+str_split+"GL.14.04.02"+str_split+"GL.14.04.03"+str_split+"LO.01"+str_split+"LO.02"+str_split+"LO.03"+str_split+"LO.04"+str_split+"LO.05"+str_split+"LO.06"+str_split+"LO.07"+str_split+"LO.08"+str_split+"LO.09"+str_split+"LO.10"+str_split+"LO.11"+str_split+"LO.12"+str_split+"LO.13"+str_split+"LO.14"+str_split+"LO.15"+str_split+"LO.16.01.01"+str_split+"LO.16.01.02"+str_split+"LO.16.01.03"+str_split+"LO.16.02.01"+str_split+"LO.16.01202"+str_split+"LO.16.02.03"+str_split+"LO.16.03.01"+str_split+"LO.16.03.02"+str_split+"LO.16.03.03"+str_split+"LO.16.04.01"+str_split+"LO.16.04.02"+str_split+"LO.16.04.03"+str_split+"LO.17"+str_split+"LO.18"+str_split+"LO.19.01.01"+str_split+"LO.19.01.02"+str_split+"LO.19.02.01"+str_split+"LO.19.02.02"+str_split+"LO.19.03.01"+str_split+"LO.19.03.02"+str_split+"LO.19.04.01"+str_split+"LO.19.04.02"+str_split+"LO.20.01.01"+str_split+"LO.20.01.02"+str_split+"LO.20.02.01"+str_split+"LO.20.02.02"+str_split+"LO.20.03.01"+str_split+"LO.20.03.02"+str_split+"LO.20.04.01"+str_split+"LO.20.04.02"+str_split+"LO.21"+str_split+"LO.22"+str_split+"LO.23"+str_split+"LO.24"+str_split+"LO.25"+str_split+"LO.26"+str_split+"LO.27"+str_split+"LO.28"+str_split+"LO.29"+str_split+"LO.30"+str_split+"LO.31"+str_split+"LO.32"+str_split+"OP.01"+str_split+"OP.02"+str_split+"OP.03"+str_split+"OP.04"+str_split+"OP.05"+str_split+"OP.06"+str_split+"OP.07"+str_split+"OP.08.01.01"+str_split+"OP.08.01.02"+str_split+"OP.08.02.01"+str_split+"OP.08.02.02"+str_split+"OP.08.03.01"+str_split+"OP.08.03.02"+str_split+"OP.08.04.01"+str_split+"OP.08.04.02"+str_split+"OP.09"+str_split+"OP.10"+str_split+"OP.11"+str_split+"OP.12"+str_split+"OP.13"+str_split+"OP.14"+str_split+"OP.14.01"+str_split+"OP.15.01.01"+str_split+"OP.15.01.02"+str_split+"OP.15.01.03"+str_split+"OP.15.02.01"+str_split+"OP.15.02.02"+str_split+"OP.15.02.03"+str_split+"OP.15.03.01"+str_split+"OP.15.03.02"+str_split+"OP.15.03.03"+str_split+"OP.15.04.01"+str_split+"OP.15.04.02"+str_split+"OP.15.04.03"+str_split+"OP.15.05.01"+str_split+"OP.15.05.02"+str_split+"OP.15.05.03"+str_split+"OP.15.06.01"+str_split+"OP.15.06.02"+str_split+"OP.15.06.03"+str_split+"OP.23.01.01"+str_split+"OP.23.01.02"+str_split+"OP.23.02.01"+str_split+"OP.23.02.02"+str_split+"OP.23.03.01"+str_split+"OP.23.03.02"+str_split+"OP.23.04.01"+str_split+"OP.23.04.02"+str_split+"OP.24.01.01"+str_split+"OP.24.01.02"+str_split+"OP.24.01.03"+str_split+"OP.24.02.01"+str_split+"OP.24.02.02"+str_split+"OP.24.02.03"+str_split+"OP.24.03.01"+str_split+"OP.24.03.02"+str_split+"OP.24.03.03"+str_split+"OP.24.04.01"+str_split+"OP.24.04.02"+str_split+"OP.24.04.03"+str_split+"OP.24.05.01"+str_split+"OP.24.05.02"+str_split+"OP.24.05.03"+str_split+"OP.24.06.01"+str_split+"OP.24.06.02"+str_split+"OP.24.06.03"+str_split+"OP.25.01.01"+str_split+"OP.25.01.02"+str_split+"OP.25.02.01"+str_split+"OP.25.02.02"+str_split+"OP.25.03.01"+str_split+"OP.25.03.02"+str_split+"OP.25.04.01"+str_split+"OP.25.04.02"+str_split+"OP.26"+str_split+"OP.28"+str_split+"OP.29"+str_split+"OP.30.01.01"+str_split+"OP.30.01.02"+str_split+"OP.30.01.03"+str_split+"OP.30.02.01"+str_split+"OP.30.02.02"+str_split+"OP.30.02.03"+str_split+"OP.30.03.01"+str_split+"OP.30.03.02"+str_split+"OP.30.03.03"+str_split+"OP.30.04.01"+str_split+"OP.30.04.02"+str_split+"OP.30.04.03"+str_split+"OP.31"+str_split+"OP.32"+str_split+"OP.33"+str_split+"OP.34"+str_split+"OP.35.01"+str_split+"OP.35.02"+str_split+"OP.35.03"+str_split+"OP.35.04"+str_split+

"OP.35.05"+str_split+"OP.35.06"+str_split+"OP.35.07"+str_split+"OP.35.08"+str_split+"OP.35.09"+str_split+"OP.40"+str_split+"OP.41"+str_split+"";
		
		for(int i=1;i<=maxBra;i++){
			String codigo = "OP.16.";
			if(i<10){
				codigo += "0"+i+str_split;
			}else{
				codigo += i+str_split;				
			}
			r+=codigo; 
		}
		//OP.17.02	OP.18.02	OP.19.02	OP.20.02	OP.21.02	OP.22.02
		for(int i=1;i<=maxOf;i++){
			String n = "";
			if(i<10){
				n = "0"+String.valueOf(i);
			}else{
				n = String.valueOf(i);
			}

			String cod = "OP.17."+n+str_split+"OP.18."+n+str_split+"OP.19."+n+str_split+"OP.20."+n+str_split+"OP.21."+n+str_split+"OP.22."+n+str_split;
			r+=cod;
		}
		
		return r;
	}

	public String getGL_01() {
		return GL_01;
	}

	public void setGL_01(String gL_01) {
		GL_01 = gL_01;
	}

	public String getGL_02() {
		return GL_02;
	}

	public void setGL_02(String gL_02) {
		GL_02 = gL_02;
	}

	public String getGL_03() {
		return GL_03;
	}

	public void setGL_03(String gL_03) {
		GL_03 = gL_03;
	}

	public String getGL_04() {
		return GL_04;
	}

	public void setGL_04(String gL_04) {
		GL_04 = gL_04;
	}

	public String getGL_05() {
		return GL_05;
	}

	public void setGL_05(String gL_05) {
		GL_05 = gL_05;
	}

	public String getGL_06() {
		return GL_06;
	}

	public void setGL_06(String gL_06) {
		GL_06 = gL_06;
	}

	public String getGL_07() {
		return GL_07;
	}

	public void setGL_07(String gL_07) {
		GL_07 = gL_07;
	}

	public String getGL_08() {
		return GL_08;
	}

	public void setGL_08(String gL_08) {
		GL_08 = gL_08;
	}

	public String getGL_09_01_01() {
		return GL_09_01_01;
	}

	public void setGL_09_01_01(String gL_09_01_01) {
		GL_09_01_01 = gL_09_01_01;
	}

	public String getGL_09_01_02() {
		return GL_09_01_02;
	}

	public void setGL_09_01_02(String gL_09_01_02) {
		GL_09_01_02 = gL_09_01_02;
	}

	public String getGL_09_02_01() {
		return GL_09_02_01;
	}

	public void setGL_09_02_01(String gL_09_02_01) {
		GL_09_02_01 = gL_09_02_01;
	}

	public String getGL_09_02_02() {
		return GL_09_02_02;
	}

	public void setGL_09_02_02(String gL_09_02_02) {
		GL_09_02_02 = gL_09_02_02;
	}

	public String getGL_09_03_01() {
		return GL_09_03_01;
	}

	public void setGL_09_03_01(String gL_09_03_01) {
		GL_09_03_01 = gL_09_03_01;
	}

	public String getGL_09_03_02() {
		return GL_09_03_02;
	}

	public void setGL_09_03_02(String gL_09_03_02) {
		GL_09_03_02 = gL_09_03_02;
	}

	public String getGL_09_04_01() {
		return GL_09_04_01;
	}

	public void setGL_09_04_01(String gL_09_04_01) {
		GL_09_04_01 = gL_09_04_01;
	}

	public String getGL_09_04_02() {
		return GL_09_04_02;
	}

	public void setGL_09_04_02(String gL_09_04_02) {
		GL_09_04_02 = gL_09_04_02;
	}

	public String getGL_10() {
		return GL_10;
	}

	public void setGL_10(String gL_10) {
		GL_10 = gL_10;
	}

	public String getGL_11() {
		return GL_11;
	}

	public void setGL_11(String gL_11) {
		GL_11 = gL_11;
	}

	public String getGL_12() {
		return GL_12;
	}

	public void setGL_12(String gL_12) {
		GL_12 = gL_12;
	}

	public String getGL_13() {
		return GL_13;
	}

	public void setGL_13(String gL_13) {
		GL_13 = gL_13;
	}

	public String getGL_14_01_01() {
		return GL_14_01_01;
	}

	public void setGL_14_01_01(String gL_14_01_01) {
		GL_14_01_01 = gL_14_01_01;
	}

	public String getGL_14_01_02() {
		return GL_14_01_02;
	}

	public void setGL_14_01_02(String gL_14_01_02) {
		GL_14_01_02 = gL_14_01_02;
	}

	public String getGL_14_01_03() {
		return GL_14_01_03;
	}

	public void setGL_14_01_03(String gL_14_01_03) {
		GL_14_01_03 = gL_14_01_03;
	}

	public String getGL_14_02_01() {
		return GL_14_02_01;
	}

	public void setGL_14_02_01(String gL_14_02_01) {
		GL_14_02_01 = gL_14_02_01;
	}

	public String getGL_14_02_02() {
		return GL_14_02_02;
	}

	public void setGL_14_02_02(String gL_14_02_02) {
		GL_14_02_02 = gL_14_02_02;
	}

	public String getGL_14_02_03() {
		return GL_14_02_03;
	}

	public void setGL_14_02_03(String gL_14_02_03) {
		GL_14_02_03 = gL_14_02_03;
	}

	public String getGL_14_03_01() {
		return GL_14_03_01;
	}

	public void setGL_14_03_01(String gL_14_03_01) {
		GL_14_03_01 = gL_14_03_01;
	}

	public String getGL_14_03_02() {
		return GL_14_03_02;
	}

	public void setGL_14_03_02(String gL_14_03_02) {
		GL_14_03_02 = gL_14_03_02;
	}

	public String getGL_14_03_03() {
		return GL_14_03_03;
	}

	public void setGL_14_03_03(String gL_14_03_03) {
		GL_14_03_03 = gL_14_03_03;
	}

	public String getGL_14_04_01() {
		return GL_14_04_01;
	}

	public void setGL_14_04_01(String gL_14_04_01) {
		GL_14_04_01 = gL_14_04_01;
	}

	public String getGL_14_04_02() {
		return GL_14_04_02;
	}

	public void setGL_14_04_02(String gL_14_04_02) {
		GL_14_04_02 = gL_14_04_02;
	}

	public String getGL_14_04_03() {
		return GL_14_04_03;
	}

	public void setGL_14_04_03(String gL_14_04_03) {
		GL_14_04_03 = gL_14_04_03;
	}

	public String getLO_01() {
		return LO_01;
	}

	public void setLO_01(String lO_01) {
		LO_01 = lO_01;
	}

	public String getLO_02() {
		return LO_02;
	}

	public void setLO_02(String lO_02) {
		LO_02 = lO_02;
	}

	public String getLO_03() {
		return LO_03;
	}

	public void setLO_03(String lO_03) {
		LO_03 = lO_03;
	}

	public String getLO_04() {
		return LO_04;
	}

	public void setLO_04(String lO_04) {
		LO_04 = lO_04;
	}

	public String getLO_05() {
		return LO_05;
	}

	public void setLO_05(String lO_05) {
		LO_05 = lO_05;
	}

	public String getLO_06() {
		return LO_06;
	}

	public void setLO_06(String lO_06) {
		LO_06 = lO_06;
	}

	public String getLO_07() {
		return LO_07;
	}

	public void setLO_07(String lO_07) {
		LO_07 = lO_07;
	}

	public String getLO_08() {
		return LO_08;
	}

	public void setLO_08(String lO_08) {
		LO_08 = lO_08;
	}

	public String getLO_09() {
		return LO_09;
	}

	public void setLO_09(String lO_09) {
		LO_09 = lO_09;
	}

	public String getLO_10() {
		return LO_10;
	}

	public void setLO_10(String lO_10) {
		LO_10 = lO_10;
	}

	public String getLO_11() {
		return LO_11;
	}

	public void setLO_11(String lO_11) {
		LO_11 = lO_11;
	}

	public String getLO_12() {
		return LO_12;
	}

	public void setLO_12(String lO_12) {
		LO_12 = lO_12;
	}

	public String getLO_13() {
		return LO_13;
	}

	public void setLO_13(String lO_13) {
		LO_13 = lO_13;
	}

	public String getLO_14() {
		return LO_14;
	}

	public void setLO_14(String lO_14) {
		LO_14 = lO_14;
	}

	public String getLO_15() {
		return LO_15;
	}

	public void setLO_15(String lO_15) {
		LO_15 = lO_15;
	}

	public String getLO_16_01_01() {
		return LO_16_01_01;
	}

	public void setLO_16_01_01(String lO_16_01_01) {
		LO_16_01_01 = lO_16_01_01;
	}

	public String getLO_16_01_02() {
		return LO_16_01_02;
	}

	public void setLO_16_01_02(String lO_16_01_02) {
		LO_16_01_02 = lO_16_01_02;
	}

	public String getLO_16_01_03() {
		return LO_16_01_03;
	}

	public void setLO_16_01_03(String lO_16_01_03) {
		LO_16_01_03 = lO_16_01_03;
	}

	public String getLO_16_02_01() {
		return LO_16_02_01;
	}

	public void setLO_16_02_01(String lO_16_02_01) {
		LO_16_02_01 = lO_16_02_01;
	}

	public String getLO_16_01202() {
		return LO_16_01202;
	}

	public void setLO_16_01202(String lO_16_01202) {
		LO_16_01202 = lO_16_01202;
	}

	public String getLO_16_02_03() {
		return LO_16_02_03;
	}

	public void setLO_16_02_03(String lO_16_02_03) {
		LO_16_02_03 = lO_16_02_03;
	}

	public String getLO_16_03_01() {
		return LO_16_03_01;
	}

	public void setLO_16_03_01(String lO_16_03_01) {
		LO_16_03_01 = lO_16_03_01;
	}

	public String getLO_16_03_02() {
		return LO_16_03_02;
	}

	public void setLO_16_03_02(String lO_16_03_02) {
		LO_16_03_02 = lO_16_03_02;
	}

	public String getLO_16_03_03() {
		return LO_16_03_03;
	}

	public void setLO_16_03_03(String lO_16_03_03) {
		LO_16_03_03 = lO_16_03_03;
	}

	public String getLO_16_04_01() {
		return LO_16_04_01;
	}

	public void setLO_16_04_01(String lO_16_04_01) {
		LO_16_04_01 = lO_16_04_01;
	}

	public String getLO_16_04_02() {
		return LO_16_04_02;
	}

	public void setLO_16_04_02(String lO_16_04_02) {
		LO_16_04_02 = lO_16_04_02;
	}

	public String getLO_16_04_03() {
		return LO_16_04_03;
	}

	public void setLO_16_04_03(String lO_16_04_03) {
		LO_16_04_03 = lO_16_04_03;
	}

	public String getLO_17() {
		return LO_17;
	}

	public void setLO_17(String lO_17) {
		LO_17 = lO_17;
	}

	public String getLO_18() {
		return LO_18;
	}

	public void setLO_18(String lO_18) {
		LO_18 = lO_18;
	}

	public String getLO_19_01_01() {
		return LO_19_01_01;
	}

	public void setLO_19_01_01(String lO_19_01_01) {
		LO_19_01_01 = lO_19_01_01;
	}

	public String getLO_19_01_02() {
		return LO_19_01_02;
	}

	public void setLO_19_01_02(String lO_19_01_02) {
		LO_19_01_02 = lO_19_01_02;
	}

	public String getLO_19_02_01() {
		return LO_19_02_01;
	}

	public void setLO_19_02_01(String lO_19_02_01) {
		LO_19_02_01 = lO_19_02_01;
	}

	public String getLO_19_02_02() {
		return LO_19_02_02;
	}

	public void setLO_19_02_02(String lO_19_02_02) {
		LO_19_02_02 = lO_19_02_02;
	}

	public String getLO_19_03_01() {
		return LO_19_03_01;
	}

	public void setLO_19_03_01(String lO_19_03_01) {
		LO_19_03_01 = lO_19_03_01;
	}

	public String getLO_19_03_02() {
		return LO_19_03_02;
	}

	public void setLO_19_03_02(String lO_19_03_02) {
		LO_19_03_02 = lO_19_03_02;
	}

	public String getLO_19_04_01() {
		return LO_19_04_01;
	}

	public void setLO_19_04_01(String lO_19_04_01) {
		LO_19_04_01 = lO_19_04_01;
	}

	public String getLO_19_04_02() {
		return LO_19_04_02;
	}

	public void setLO_19_04_02(String lO_19_04_02) {
		LO_19_04_02 = lO_19_04_02;
	}

	public String getLO_20_01_01() {
		return LO_20_01_01;
	}

	public void setLO_20_01_01(String lO_20_01_01) {
		LO_20_01_01 = lO_20_01_01;
	}

	public String getLO_20_01_02() {
		return LO_20_01_02;
	}

	public void setLO_20_01_02(String lO_20_01_02) {
		LO_20_01_02 = lO_20_01_02;
	}

	public String getLO_20_02_01() {
		return LO_20_02_01;
	}

	public void setLO_20_02_01(String lO_20_02_01) {
		LO_20_02_01 = lO_20_02_01;
	}

	public String getLO_20_02_02() {
		return LO_20_02_02;
	}

	public void setLO_20_02_02(String lO_20_02_02) {
		LO_20_02_02 = lO_20_02_02;
	}

	public String getLO_20_03_01() {
		return LO_20_03_01;
	}

	public void setLO_20_03_01(String lO_20_03_01) {
		LO_20_03_01 = lO_20_03_01;
	}

	public String getLO_20_03_02() {
		return LO_20_03_02;
	}

	public void setLO_20_03_02(String lO_20_03_02) {
		LO_20_03_02 = lO_20_03_02;
	}

	public String getLO_20_04_01() {
		return LO_20_04_01;
	}

	public void setLO_20_04_01(String lO_20_04_01) {
		LO_20_04_01 = lO_20_04_01;
	}

	public String getLO_20_04_02() {
		return LO_20_04_02;
	}

	public void setLO_20_04_02(String lO_20_04_02) {
		LO_20_04_02 = lO_20_04_02;
	}

	public String getLO_21() {
		return LO_21;
	}

	public void setLO_21(String lO_21) {
		LO_21 = lO_21;
	}

	public String getLO_22() {
		return LO_22;
	}

	public void setLO_22(String lO_22) {
		LO_22 = lO_22;
	}

	public String getLO_23() {
		return LO_23;
	}

	public void setLO_23(String lO_23) {
		LO_23 = lO_23;
	}

	public String getLO_24() {
		return LO_24;
	}

	public void setLO_24(String lO_24) {
		LO_24 = lO_24;
	}

	public String getLO_25() {
		return LO_25;
	}

	public void setLO_25(String lO_25) {
		LO_25 = lO_25;
	}

	public String getLO_26() {
		return LO_26;
	}

	public void setLO_26(String lO_26) {
		LO_26 = lO_26;
	}

	public String getLO_27() {
		return LO_27;
	}

	public void setLO_27(String lO_27) {
		LO_27 = lO_27;
	}

	public String getLO_28() {
		return LO_28;
	}

	public void setLO_28(String lO_28) {
		LO_28 = lO_28;
	}

	public String getLO_29() {
		return LO_29;
	}

	public void setLO_29(String lO_29) {
		LO_29 = lO_29;
	}

	public String getLO_30() {
		return LO_30;
	}

	public void setLO_30(String lO_30) {
		LO_30 = lO_30;
	}

	public String getLO_31() {
		return LO_31;
	}

	public void setLO_31(String lO_31) {
		LO_31 = lO_31;
	}

	public String getLO_32() {
		return LO_32;
	}

	public void setLO_32(String lO_32) {
		LO_32 = lO_32;
	}

	public String getOP_01() {
		return OP_01;
	}

	public void setOP_01(String oP_01) {
		OP_01 = oP_01;
	}

	public String getOP_02() {
		return OP_02;
	}

	public void setOP_02(String oP_02) {
		OP_02 = oP_02;
	}

	public String getOP_03() {
		return OP_03;
	}

	public void setOP_03(String oP_03) {
		OP_03 = oP_03;
	}

	public String getOP_04() {
		return OP_04;
	}

	public void setOP_04(String oP_04) {
		OP_04 = oP_04;
	}

	public String getOP_05() {
		return OP_05;
	}

	public void setOP_05(String oP_05) {
		OP_05 = oP_05;
	}

	public String getOP_06() {
		return OP_06;
	}

	public void setOP_06(String oP_06) {
		OP_06 = oP_06;
	}

	public String getOP_07() {
		return OP_07;
	}

	public void setOP_07(String oP_07) {
		OP_07 = oP_07;
	}

	public String getOP_08_01_01() {
		return OP_08_01_01;
	}

	public void setOP_08_01_01(String oP_08_01_01) {
		OP_08_01_01 = oP_08_01_01;
	}

	public String getOP_08_01_02() {
		return OP_08_01_02;
	}

	public void setOP_08_01_02(String oP_08_01_02) {
		OP_08_01_02 = oP_08_01_02;
	}

	public String getOP_08_02_01() {
		return OP_08_02_01;
	}

	public void setOP_08_02_01(String oP_08_02_01) {
		OP_08_02_01 = oP_08_02_01;
	}

	public String getOP_08_02_02() {
		return OP_08_02_02;
	}

	public void setOP_08_02_02(String oP_08_02_02) {
		OP_08_02_02 = oP_08_02_02;
	}

	public String getOP_08_03_01() {
		return OP_08_03_01;
	}

	public void setOP_08_03_01(String oP_08_03_01) {
		OP_08_03_01 = oP_08_03_01;
	}

	public String getOP_08_03_02() {
		return OP_08_03_02;
	}

	public void setOP_08_03_02(String oP_08_03_02) {
		OP_08_03_02 = oP_08_03_02;
	}

	public String getOP_08_04_01() {
		return OP_08_04_01;
	}

	public void setOP_08_04_01(String oP_08_04_01) {
		OP_08_04_01 = oP_08_04_01;
	}

	public String getOP_08_04_02() {
		return OP_08_04_02;
	}

	public void setOP_08_04_02(String oP_08_04_02) {
		OP_08_04_02 = oP_08_04_02;
	}

	public String getOP_09() {
		return OP_09;
	}

	public void setOP_09(String oP_09) {
		OP_09 = oP_09;
	}

	public String getOP_10() {
		return OP_10;
	}

	public void setOP_10(String oP_10) {
		OP_10 = oP_10;
	}

	public String getOP_11() {
		return OP_11;
	}

	public void setOP_11(String oP_11) {
		OP_11 = oP_11;
	}

	public String getOP_12() {
		return OP_12;
	}

	public void setOP_12(String oP_12) {
		OP_12 = oP_12;
	}

	public String getOP_13() {
		return OP_13;
	}

	public void setOP_13(String oP_13) {
		OP_13 = oP_13;
	}

	public String getOP_14() {
		return OP_14;
	}
	public String getOP_14_01() {
		return OP_14_01;
	}

	public void setOP_14(String oP_14) {
		OP_14 = oP_14;
	}
	public void setOP_14_01(String oP_14_01) {
		OP_14_01 = oP_14_01;
	}
	public String getOP_15_01_01() {
		return OP_15_01_01;
	}

	public void setOP_15_01_01(String oP_15_01_01) {
		OP_15_01_01 = oP_15_01_01;
	}

	public String getOP_15_01_02() {
		return OP_15_01_02;
	}

	public void setOP_15_01_02(String oP_15_01_02) {
		OP_15_01_02 = oP_15_01_02;
	}

	public String getOP_15_01_03() {
		return OP_15_01_03;
	}

	public void setOP_15_01_03(String oP_15_01_03) {
		OP_15_01_03 = oP_15_01_03;
	}

	public String getOP_15_02_01() {
		return OP_15_02_01;
	}

	public void setOP_15_02_01(String oP_15_02_01) {
		OP_15_02_01 = oP_15_02_01;
	}

	public String getOP_15_02_02() {
		return OP_15_02_02;
	}

	public void setOP_15_02_02(String oP_15_02_02) {
		OP_15_02_02 = oP_15_02_02;
	}

	public String getOP_15_02_03() {
		return OP_15_02_03;
	}

	public void setOP_15_02_03(String oP_15_02_03) {
		OP_15_02_03 = oP_15_02_03;
	}

	public String getOP_15_03_01() {
		return OP_15_03_01;
	}

	public void setOP_15_03_01(String oP_15_03_01) {
		OP_15_03_01 = oP_15_03_01;
	}

	public String getOP_15_03_02() {
		return OP_15_03_02;
	}

	public void setOP_15_03_02(String oP_15_03_02) {
		OP_15_03_02 = oP_15_03_02;
	}

	public String getOP_15_03_03() {
		return OP_15_03_03;
	}

	public void setOP_15_03_03(String oP_15_03_03) {
		OP_15_03_03 = oP_15_03_03;
	}

	public String getOP_15_04_01() {
		return OP_15_04_01;
	}

	public void setOP_15_04_01(String oP_15_04_01) {
		OP_15_04_01 = oP_15_04_01;
	}

	public String getOP_15_04_02() {
		return OP_15_04_02;
	}

	public void setOP_15_04_02(String oP_15_04_02) {
		OP_15_04_02 = oP_15_04_02;
	}

	public String getOP_15_04_03() {
		return OP_15_04_03;
	}

	public void setOP_15_04_03(String oP_15_04_03) {
		OP_15_04_03 = oP_15_04_03;
	}

	public String getOP_15_05_01() {
		return OP_15_05_01;
	}

	public void setOP_15_05_01(String oP_15_05_01) {
		OP_15_05_01 = oP_15_05_01;
	}

	public String getOP_15_05_02() {
		return OP_15_05_02;
	}

	public void setOP_15_05_02(String oP_15_05_02) {
		OP_15_05_02 = oP_15_05_02;
	}

	public String getOP_15_05_03() {
		return OP_15_05_03;
	}

	public void setOP_15_05_03(String oP_15_05_03) {
		OP_15_05_03 = oP_15_05_03;
	}

	public String getOP_15_06_01() {
		return OP_15_06_01;
	}

	public void setOP_15_06_01(String oP_15_06_01) {
		OP_15_06_01 = oP_15_06_01;
	}

	public String getOP_15_06_02() {
		return OP_15_06_02;
	}

	public void setOP_15_06_02(String oP_15_06_02) {
		OP_15_06_02 = oP_15_06_02;
	}

	public String getOP_15_06_03() {
		return OP_15_06_03;
	}

	public void setOP_15_06_03(String oP_15_06_03) {
		OP_15_06_03 = oP_15_06_03;
	}

	public String getOP_23_01_01() {
		return OP_23_01_01;
	}

	public void setOP_23_01_01(String oP_23_01_01) {
		OP_23_01_01 = oP_23_01_01;
	}

	public String getOP_23_01_02() {
		return OP_23_01_02;
	}

	public void setOP_23_01_02(String oP_23_01_02) {
		OP_23_01_02 = oP_23_01_02;
	}

	public String getOP_23_02_01() {
		return OP_23_02_01;
	}

	public void setOP_23_02_01(String oP_23_02_01) {
		OP_23_02_01 = oP_23_02_01;
	}

	public String getOP_23_02_02() {
		return OP_23_02_02;
	}

	public void setOP_23_02_02(String oP_23_02_02) {
		OP_23_02_02 = oP_23_02_02;
	}

	public String getOP_23_03_01() {
		return OP_23_03_01;
	}

	public void setOP_23_03_01(String oP_23_03_01) {
		OP_23_03_01 = oP_23_03_01;
	}

	public String getOP_23_03_02() {
		return OP_23_03_02;
	}

	public void setOP_23_03_02(String oP_23_03_02) {
		OP_23_03_02 = oP_23_03_02;
	}

	public String getOP_23_04_01() {
		return OP_23_04_01;
	}

	public void setOP_23_04_01(String oP_23_04_01) {
		OP_23_04_01 = oP_23_04_01;
	}

	public String getOP_23_04_02() {
		return OP_23_04_02;
	}

	public void setOP_23_04_02(String oP_23_04_02) {
		OP_23_04_02 = oP_23_04_02;
	}

	public String getOP_24_01_01() {
		return OP_24_01_01;
	}

	public void setOP_24_01_01(String oP_24_01_01) {
		OP_24_01_01 = oP_24_01_01;
	}

	public String getOP_24_01_02() {
		return OP_24_01_02;
	}

	public void setOP_24_01_02(String oP_24_01_02) {
		OP_24_01_02 = oP_24_01_02;
	}

	public String getOP_24_01_03() {
		return OP_24_01_03;
	}

	public void setOP_24_01_03(String oP_24_01_03) {
		OP_24_01_03 = oP_24_01_03;
	}

	public String getOP_24_02_01() {
		return OP_24_02_01;
	}

	public void setOP_24_02_01(String oP_24_02_01) {
		OP_24_02_01 = oP_24_02_01;
	}

	public String getOP_24_02_02() {
		return OP_24_02_02;
	}

	public void setOP_24_02_02(String oP_24_02_02) {
		OP_24_02_02 = oP_24_02_02;
	}

	public String getOP_24_02_03() {
		return OP_24_02_03;
	}

	public void setOP_24_02_03(String oP_24_02_03) {
		OP_24_02_03 = oP_24_02_03;
	}

	public String getOP_24_03_01() {
		return OP_24_03_01;
	}

	public void setOP_24_03_01(String oP_24_03_01) {
		OP_24_03_01 = oP_24_03_01;
	}

	public String getOP_24_03_02() {
		return OP_24_03_02;
	}

	public void setOP_24_03_02(String oP_24_03_02) {
		OP_24_03_02 = oP_24_03_02;
	}

	public String getOP_24_03_03() {
		return OP_24_03_03;
	}

	public void setOP_24_03_03(String oP_24_03_03) {
		OP_24_03_03 = oP_24_03_03;
	}

	public String getOP_24_04_01() {
		return OP_24_04_01;
	}

	public void setOP_24_04_01(String oP_24_04_01) {
		OP_24_04_01 = oP_24_04_01;
	}

	public String getOP_24_04_02() {
		return OP_24_04_02;
	}

	public void setOP_24_04_02(String oP_24_04_02) {
		OP_24_04_02 = oP_24_04_02;
	}

	public String getOP_24_04_03() {
		return OP_24_04_03;
	}

	public void setOP_24_04_03(String oP_24_04_03) {
		OP_24_04_03 = oP_24_04_03;
	}
	
	

	public String getOP_24_05_01() {
		return OP_24_05_01;
	}

	public void setOP_24_05_01(String oP_24_05_01) {
		OP_24_05_01 = oP_24_05_01;
	}

	public String getOP_24_05_02() {
		return OP_24_05_02;
	}

	public void setOP_24_05_02(String oP_24_05_02) {
		OP_24_05_02 = oP_24_05_02;
	}

	public String getOP_24_05_03() {
		return OP_24_05_03;
	}

	public void setOP_24_05_03(String oP_24_05_03) {
		OP_24_05_03 = oP_24_05_03;
	}

	public String getOP_24_06_01() {
		return OP_24_06_01;
	}

	public void setOP_24_06_01(String oP_24_06_01) {
		OP_24_06_01 = oP_24_06_01;
	}

	public String getOP_24_06_02() {
		return OP_24_06_02;
	}

	public void setOP_24_06_02(String oP_24_06_02) {
		OP_24_06_02 = oP_24_06_02;
	}

	public String getOP_24_06_03() {
		return OP_24_06_03;
	}

	public void setOP_24_06_03(String oP_24_06_03) {
		OP_24_06_03 = oP_24_06_03;
	}



	public String getOP_25_01_01() {
		return OP_25_01_01;
	}

	public void setOP_25_01_01(String oP_25_01_01) {
		OP_25_01_01 = oP_25_01_01;
	}

	public String getOP_25_01_02() {
		return OP_25_01_02;
	}

	public void setOP_25_01_02(String oP_25_01_02) {
		OP_25_01_02 = oP_25_01_02;
	}

	public String getOP_25_02_01() {
		return OP_25_02_01;
	}

	public void setOP_25_02_01(String oP_25_02_01) {
		OP_25_02_01 = oP_25_02_01;
	}

	public String getOP_25_02_02() {
		return OP_25_02_02;
	}

	public void setOP_25_02_02(String oP_25_02_02) {
		OP_25_02_02 = oP_25_02_02;
	}

	public String getOP_25_03_01() {
		return OP_25_03_01;
	}

	public void setOP_25_03_01(String oP_25_03_01) {
		OP_25_03_01 = oP_25_03_01;
	}

	public String getOP_25_03_02() {
		return OP_25_03_02;
	}

	public void setOP_25_03_02(String oP_25_03_02) {
		OP_25_03_02 = oP_25_03_02;
	}

	public String getOP_25_04_01() {
		return OP_25_04_01;
	}

	public void setOP_25_04_01(String oP_25_04_01) {
		OP_25_04_01 = oP_25_04_01;
	}

	public String getOP_25_04_02() {
		return OP_25_04_02;
	}

	public void setOP_25_04_02(String oP_25_04_02) {
		OP_25_04_02 = oP_25_04_02;
	}

	public String getOP_26() {
		return OP_26;
	}

	public void setOP_26(String oP_26) {
		OP_26 = oP_26;
	}

	public String getOP_28() {
		return OP_28;
	}

	public void setOP_28(String oP_28) {
		OP_28 = oP_28;
	}

	public String getOP_29() {
		return OP_29;
	}

	public void setOP_29(String oP_29) {
		OP_29 = oP_29;
	}

	public String getOP_30_01_01() {
		return OP_30_01_01;
	}

	public void setOP_30_01_01(String oP_30_01_01) {
		OP_30_01_01 = oP_30_01_01;
	}

	public String getOP_30_01_02() {
		return OP_30_01_02;
	}

	public void setOP_30_01_02(String oP_30_01_02) {
		OP_30_01_02 = oP_30_01_02;
	}

	public String getOP_30_01_03() {
		return OP_30_01_03;
	}

	public void setOP_30_01_03(String oP_30_01_03) {
		OP_30_01_03 = oP_30_01_03;
	}

	public String getOP_30_02_01() {
		return OP_30_02_01;
	}

	public void setOP_30_02_01(String oP_30_02_01) {
		OP_30_02_01 = oP_30_02_01;
	}

	public String getOP_30_02_02() {
		return OP_30_02_02;
	}

	public void setOP_30_02_02(String oP_30_02_02) {
		OP_30_02_02 = oP_30_02_02;
	}

	public String getOP_30_02_03() {
		return OP_30_02_03;
	}

	public void setOP_30_02_03(String oP_30_02_03) {
		OP_30_02_03 = oP_30_02_03;
	}

	public String getOP_30_03_01() {
		return OP_30_03_01;
	}

	public void setOP_30_03_01(String oP_30_03_01) {
		OP_30_03_01 = oP_30_03_01;
	}

	public String getOP_30_03_02() {
		return OP_30_03_02;
	}

	public void setOP_30_03_02(String oP_30_03_02) {
		OP_30_03_02 = oP_30_03_02;
	}

	public String getOP_30_03_03() {
		return OP_30_03_03;
	}

	public void setOP_30_03_03(String oP_30_03_03) {
		OP_30_03_03 = oP_30_03_03;
	}

	public String getOP_30_04_01() {
		return OP_30_04_01;
	}

	public void setOP_30_04_01(String oP_30_04_01) {
		OP_30_04_01 = oP_30_04_01;
	}

	public String getOP_30_04_02() {
		return OP_30_04_02;
	}

	public void setOP_30_04_02(String oP_30_04_02) {
		OP_30_04_02 = oP_30_04_02;
	}

	public String getOP_30_04_03() {
		return OP_30_04_03;
	}

	public void setOP_30_04_03(String oP_30_04_03) {
		OP_30_04_03 = oP_30_04_03;
	}

	public String getOP_31() {
		return OP_31;
	}

	public void setOP_31(String oP_31) {
		OP_31 = oP_31;
	}

	public String getOP_32() {
		return OP_32;
	}

	public void setOP_32(String oP_32) {
		OP_32 = oP_32;
	}

	public String getOP_33() {
		return OP_33;
	}

	public void setOP_33(String oP_33) {
		OP_33 = oP_33;
	}

	public String getOP_34() {
		return OP_34;
	}

	public void setOP_34(String oP_34) {
		OP_34 = oP_34;
	}

	public String getOP_35_01() {
		return OP_35_01;
	}

	public void setOP_35_01(String oP_35_01) {
		OP_35_01 = oP_35_01;
	}

	public String getOP_35_02() {
		return OP_35_02;
	}

	public void setOP_35_02(String oP_35_02) {
		OP_35_02 = oP_35_02;
	}

	public String getOP_35_03() {
		return OP_35_03;
	}

	public void setOP_35_03(String oP_35_03) {
		OP_35_03 = oP_35_03;
	}

	public String getOP_35_04() {
		return OP_35_04;
	}

	public void setOP_35_04(String oP_35_04) {
		OP_35_04 = oP_35_04;
	}

	public String getOP_35_05() {
		return OP_35_05;
	}

	public void setOP_35_05(String oP_35_05) {
		OP_35_05 = oP_35_05;
	}

	public String getOP_35_06() {
		return OP_35_06;
	}

	public void setOP_35_06(String oP_35_06) {
		OP_35_06 = oP_35_06;
	}

	public String getOP_35_07() {
		return OP_35_07;
	}

	public void setOP_35_07(String oP_35_07) {
		OP_35_07 = oP_35_07;
	}

	public String getOP_35_08() {
		return OP_35_08;
	}

	public void setOP_35_08(String oP_35_08) {
		OP_35_08 = oP_35_08;
	}

	public String getOP_35_09() {
		return OP_35_09;
	}

	public void setOP_35_09(String oP_35_09) {
		OP_35_09 = oP_35_09;
	}

	public String getOP_40() {
		return OP_40;
	}

	public void setOP_40(String oP_40) {
		OP_40 = oP_40;
	}

	public String getOP_41() {
		return OP_41;
	}

	public void setOP_41(String oP_41) {
		OP_41 = oP_41;
	}
	
	public String getLinea(){
		
		String r = "";
		
		
		return r;
	}
	
}
