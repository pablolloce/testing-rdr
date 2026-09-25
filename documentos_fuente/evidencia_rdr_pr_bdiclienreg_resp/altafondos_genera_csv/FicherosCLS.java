package tools;


import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Vector;


public class FicherosCLS {
	public FicherosCLS(){
	}
	
	
	public static int getCountProceduresLike(String compare){
		try{
			int cont=0;
			String txt[]=getProceduresArray();
			for(int i=0;i<txt.length;i++){
				if (txt[i].contains(compare)){
					cont++;
				}
			}
			return cont;
		}catch(Exception e){
			System.out.println("FicheroCLS::getCountProceduresLike::ERROR - No se pudo contar el numero de procedimientos");
			return -1;
		}
		
	}
	
	public static String []getProceduresArray(){
		
		try{
			Process proc = Runtime.getRuntime().exec("cmd /c tasklist /nh");
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			//OutputStreamWriter oStream = new OutputStreamWriter(proc.getOutputStream());
			String line;
			Vector<String>miVector=new Vector<String>();
			while ((line = input.readLine()) != null) 
			{ 
				
				String []valores=FicherosCLS.getLineArguments(line," ");
				if (valores.length>0){
					if(valores[0].equals("")==false){
						miVector.addElement(valores[0]);
					}	
				}
			}
			int largo=miVector.size();
			String []txt=new String [largo];
			for(int i=0;i<largo;i++){
				txt[i]=miVector.get(i);
			}
			return txt;
			
		}catch (Exception e){
			System.out.println("FicheroCLS::getProceduresArray::ERROR - No se pudo obtener el listado de procedimientos.");
			System.out.print(e.toString());
			String []txt=new String[1];
			txt[0]="";
			return txt;
		}
		
		
		
	}
	
	public static String [] getLineArguments(String line, String separator){
		
		try{
			String []txtaux=line.split(separator);
			int cont=0;
			for(int i=0;i<txtaux.length;i++){
				if(txtaux[i].equals("")){
					cont++;
				}
			}
			try{
				//System.out.println("El contador tiene: "+cont);
				if (cont==0){
					return txtaux;
				}else{
					String []txt=new String[txtaux.length-cont];
					int j=0;
					for (int i=0;i<txtaux.length;i++){
						if(txtaux[i].equals("")==false){
							txt[j]=txtaux[i];
							j++;
						}	
					}
					return txt;
				}
			}catch(Exception e){
				String []txt=new String[1];
				txt[0]="";
				System.out.println("FicheroCLS::getLineArguments::ERROR - Falló al crear el array.");
				System.out.print(e.toString());
				return txt;
				
			}
			
			
			
		}catch(Exception e){
			String []txt=new String[1];
			txt[0]="";
			System.out.println("FicheroCLS::getLineArguments::ERROR - Falló al separar el texto.");
			System.out.print(e.toString());
			return txt;
		}
	}
	
	public static boolean copyFile(String origen, String destino){
		try{
	        Path FROM = Paths.get(origen);
	        Path TO = Paths.get(destino);
	        //sobreescribir el fichero de destino, si existe, y copiar
	        // los atributos, incluyendo los permisos rwx
	        CopyOption[] options = new CopyOption[]{
	          StandardCopyOption.REPLACE_EXISTING,
	          StandardCopyOption.COPY_ATTRIBUTES
	        }; 
	        Files.copy(FROM, TO, options);
	        return true;
		}catch(Exception e){
			return false;
		}
	}
	/*public static boolean deleteFile(String Ruta){
		LogFilesAndFolders logfilesandfolders=new LogFilesAndFolders();
		File fichero = new File(Ruta);
		
	    if (fichero.delete()){
	    	logfilesandfolders.WriteMessage("FicherosCLS::deleteFile: se borró '"+Ruta+"'");
	    	return true;
	    }else{
	    	logfilesandfolders.WriteError("FicherosCLS::deleteFile: NO se borró '"+Ruta+"'");
	    	System.out.println("FicherosCLS::deleteFile: NO se borró '"+Ruta+"'");
	    	return false;
	    }
	}*/
	
	public static boolean deleteFile(String Ruta){
		File fichero = new File(Ruta);
		boolean Borrado=false;
		int i=0;
		BREAKWHILE:{
			while (i<50 && Borrado==false) {
				if(Borrado==false){
					Borrado=fichero.delete();
					break BREAKWHILE;
				}
				i++;
			}
		}
	    if (Borrado==true){
	    	//logfilesandfolders.WriteMessage("FicherosCLS::deleteFile: se borró '"+Ruta+"'");
	    	return true;
	    }else{
	    	System.out.println("FicherosCLS::deleteFile: NO se borró '"+Ruta+"'");
	    	return false;
	    }
	}
	
	public static String getFileName(String Ruta){
		String text_out = Ruta.substring(Ruta.lastIndexOf('\\') + 1, Ruta.lastIndexOf('.'));
		return text_out;
	}
	public static String getExtension(String Ruta) {
        int index = Ruta.lastIndexOf('.');
        if (index == -1) {
              return "";
        } else {
              return Ruta.substring(index + 1);
        }
	}
	public static String [] getFileArgs(String Ruta){
		String txtCom=getFileName(Ruta);
		String []txt=txtCom.split("_");
		return txt;
	}
	public static String readFileString(String Ruta){
		try{
			FileReader f=new FileReader(Ruta);
			BufferedReader b=new BufferedReader(f);
			String Linea;
			String TextoCompleto="";
			while((Linea = b.readLine())!=null) {
				TextoCompleto=TextoCompleto+Linea+"\n";	
	        }
			b.close();
			return TextoCompleto;
		}catch (Exception e){
			return "";
		}
	}
	public static Vector<String> readFileVector(String Ruta){
		try{
			Vector<String>v=new Vector<String>();
			FileReader f=new FileReader(Ruta);
			BufferedReader b=new BufferedReader(f);
			String Linea;
			while((Linea = b.readLine())!=null) {
				v.add(Linea);
	        }
			b.close();
			return v;
		}catch (Exception e){
			return null;
		}
	}
	public static String readFileStringCodification(String Ruta, String Codification){
		try{
			File f=new File(Ruta);
			BufferedReader b=new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(f), Codification));
			String Linea;
			String TextoCompleto="";
			while((Linea = b.readLine())!=null) {
				TextoCompleto=TextoCompleto+Linea+"\n";	
	        }
			b.close();
			return TextoCompleto;
		}catch (Exception e){
			return "";
		}
	}
	public static String []readFileArray(String Ruta){
		try{
			String txtCompleto=readFileString(Ruta);
			String [] resultadoString=txtCompleto.split("\n");
			return resultadoString;
		}catch(Exception e){
			String []resStrings=new String[0];
			return resStrings;
		}
	}
	public static String []readFileArrayCodification(String Ruta, String Codification){
		try{
			File f=new File(Ruta);
			BufferedReader b=new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(f), Codification));
			String Linea;
			Vector<String>vStr=new Vector<String>();
			while((Linea = b.readLine())!=null) {
				vStr.add(Linea);
	        }
			b.close();
			String []resStrings=new String[vStr.size()];
			for(int i=0;i<resStrings.length;i++) {
				resStrings[i]=vStr.get(i);
			}
			return resStrings;
		}catch(Exception e){
			String []resStrings=new String[0];
			return resStrings;
		}
	}


	public static String [][] readFileMatrixString(String Ruta, String Delimiter){
		try{
			String textoCompleto=readFileString(Ruta);
			String []AuxStr=textoCompleto.split("\n");
			String []Aux2Str=AuxStr[0].split(Delimiter);
			String [][]TxtDef=new String [AuxStr.length][Aux2Str.length];
			for(int i=0;i<TxtDef.length;i++){
				Aux2Str=AuxStr[i].split(Delimiter);
				for (int j=0;j<TxtDef[i].length;j++){
					if(Aux2Str.length>j) {
						TxtDef[i][j]=Aux2Str[j];
					}else {
						TxtDef[i][j]="";
					}
				}
			}
			return TxtDef;
		}catch(Exception e){
			System.out.println(e.toString());
			String [][] ResVacio=new String [0][0];
			return ResVacio;
		}	
	}
	public static String [][] readFileMatrixStringCodification(String Ruta, String Delimiter, String Codification){
		try{
			//String textoCompleto=readFileStringCodification(Ruta, Codification);
			String []AuxStr=readFileArrayCodification(Ruta, Codification);
			String []Aux2Str=AuxStr[0].split(Delimiter);
			String [][]TxtDef=new String [AuxStr.length][Aux2Str.length];
			for(int i=0;i<TxtDef.length;i++){
				Aux2Str=AuxStr[i].split(Delimiter);
				for (int j=0;j<TxtDef[i].length;j++){
					if(Aux2Str.length>j) {
						TxtDef[i][j]=Aux2Str[j];
					}else {
						TxtDef[i][j]="";
					}
				}
			}
			return TxtDef;
		}catch(Exception e){
			System.out.println(e.toString());
			String [][] ResVacio=new String [0][0];
			return ResVacio;
		}	
	}
	
	private String rutaActual="";
	private FileWriter fwrtr;
	private BufferedWriter bwrtr;
	
	public static void writeLine(String ruta, String contenido) {
		try {
			FileWriter f=new FileWriter(ruta,true);
			BufferedWriter bf=new BufferedWriter(f);
			bf.write(contenido);
			bf.newLine();
			bf.close();
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("FicheroCLS::writeLine::ERROR::No se pudo escribir en "+ruta);
			System.out.println(e.toString());
		}
		
	}
	
	public void  escrituraConstanteCerrar() {
		try{
			bwrtr.close();
			fwrtr.close();
			
		}catch(Exception e){
			System.out.println("FicheroCLS::escrituraConstanteCerrar - no se pudo escribir en "+rutaActual);
			System.out.println(e.toString());
		}
		
	}
	public boolean escrituraConstante (String ruta, String contenido){
		if (ruta==rutaActual){
			try{
				bwrtr.write(contenido);
				bwrtr.newLine();
				return true;
			}catch(Exception e){
				System.out.println("FicheroCLS::escrituraContante - No pudo escribir:");
				System.out.println("'"+contenido+"'");
				System.out.println("Se intentará abrir el fichero");
				
				try{
					rutaActual=ruta;
					fwrtr=new FileWriter(rutaActual);
					bwrtr=new BufferedWriter(fwrtr);
					bwrtr.write(contenido);
					bwrtr.newLine();
					return true;
					
				}catch(Exception ex){
					System.out.println("FicheroCLS::escrituraContante - No pudo abrir y escribir:");
					System.out.println("'"+contenido+"'");
					rutaActual="";
					return false;
				}
				
			}
		}else{
			try{
				rutaActual=ruta;
				fwrtr=new FileWriter(rutaActual);
				bwrtr=new BufferedWriter(fwrtr);
				bwrtr.write(contenido);
				bwrtr.newLine();
				return true;
			}catch(Exception e){
				System.out.println("FicheroCLS::escrituraContante - No pudo escribir:");
				System.out.println("'"+contenido+"'");
				rutaActual="";
				return false;
			}
		}
		
	}
	public static void writeVectorInFile(String ruta, Vector<String>insert){
		try {
			FileWriter fw=new FileWriter(ruta);
			BufferedWriter bw=new BufferedWriter(fw);
			for (int i = 0; i < insert.size(); i++) {
				bw.write(insert.get(i));
				bw.newLine();
			}
			bw.close();
			fw.close();
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("FicherosCLS::writeVectorInFile::ERROR Insertando en "+ruta);
		}
		
		return;
	}
	public static boolean writeVectorInFileBoolean(String ruta, Vector<String>insert){
		try {
			
			FileOutputStream fos = new FileOutputStream(ruta);
			OutputStreamWriter os = new OutputStreamWriter(fos,"UTF-8");
			PrintWriter pw = new PrintWriter(os);	

			for (int i = 0; i < insert.size(); i++) {
				pw.println(insert.get(i));
			}
			pw.close();
			os.close();
			fos.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("FicherosCLS::writeVectorInFile::ERROR Insertando en "+ruta);
			return false;
		}
	}
	public static boolean matrixToCSV(String RutaOutput,String [][]matrix) {
		System.out.println("fileTools::matrixToCSV::Se tratata de imprimir la matriz en la ruta:\n"+RutaOutput);
		try {
			
			FileWriter fw=new FileWriter(RutaOutput);
			BufferedWriter bw=new BufferedWriter(fw);
			//System.out.println("fileTools::matrixToCSV::BufferedWriter Creado");
			try {
				int k=0;
				//System.out.println("fileTools::matrixToCSV::Imprimiendo matriz");
				for(int i=0;i<matrix.length;i++) {
					String linea="";
					for (int j=0;j<matrix[i].length;j++) {
						if(matrix[i][j].equals(null) || matrix[i][j].isEmpty()) {
							linea+=";";
						}else {
							linea+=matrix[i][j]+";";
						}
						
					}
					bw.write(linea);
			    	bw.newLine();
			    	k++;
			    	if (k % 1000==0) {
			    		//System.out.println("fileTools::matrixToCSV::Imprimiendo linea "+k);
			    	}
				}
				bw.close();
				fw.close();
				//System.out.println("fileTools::matrixToCSV::Se imprimio la matriz");
				return true;
			}catch(Exception e) {
				
				bw.close();
				fw.close();
				System.out.println("fileTools::matrixToCSV::ERROR - Fallo al imprimir la matriz");
				System.out.print(e.toString());;
				return false;
			}
		}catch(Exception e) {	
			System.out.println("fileTools::matrixToCSV::ERROR - No se pudo escribir en la ruta: "+RutaOutput);
			System.out.print(e.toString());;
			return false;
			
		}
		
	}
	
}