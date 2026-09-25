import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class PpalAltas {

    public static void main(String[] args) throws Exception {

	System.out.println("******************* INICIO GENERA CSV -> XML *******************");

	File dirEntrada =null;			
	File ficheroEntrada =null;
	ArrayList<HashMap<String,String>> objDatos =null;

	//Cogiendo argumentos
	dirEntrada   = new File(args[0]);	
	String Entrada 	= "";	
	String Entrada2	= "";			
	String Salida= args[1];			
	String Resultado ="";
	String Usuario="";
	
	//String Usuario	= args[2];
	File[] ficheros = dirEntrada.listFiles();
	System.out.println(" "+ficheros.length);
	if (ficheros == null||ficheros.length<=0)
	    System.out.println("No hay ficheros en el directorio de entrada");
	else { 
	    System.out.println("Hay ficheros en el directorio de entrada "+ficheros.length);
	    for (int i=0;i<ficheros.length;i++){
			Entrada=ficheros[i].toString();
			if(Entrada.indexOf(".csv")!=-1){
			    ficheroEntrada= new File(Entrada);
			    Entrada2=Entrada;
			}
	    }

	    if(Entrada2.indexOf("@")!=-1){
		Usuario = Entrada2.substring(Entrada2.indexOf("@")+1, Entrada2.length() - 4);
	    }
	    //Comprobando existencia de fichero de entrada y abriendo
	    if (!ficheroEntrada.exists()) { 
		System.out.println("El fichero no existe !!!");	 System.exit(1);
	    }else{ 
		System.out.println("FicheroEntrada: "+ficheroEntrada); System.out.println("FicheroSalida: "+Salida);

		// PASO UNO - RECOLECCIÓN DE LOS DATOS NECESARIOS 
		Ficheros objLCSV = new Ficheros();

		objDatos =objLCSV.obtenerDatos(ficheroEntrada, args);  

		GenerarXML_version1 objGCSV_1 = new GenerarXML_version1();

		GenerarXML_version2 objGCSV_2 = new GenerarXML_version2();

		for(int i=0;i<objDatos.size();i++) {
		    if(args.length>=3&&args[2].equals("G")){
			Resultado=Resultado+objGCSV_2.obtenerXML_version2(objDatos.get(i), i, Usuario)+"\n";
		    } else {
			Resultado=Resultado+objGCSV_1.obtenerXML_version1(objDatos.get(i), i, Usuario)+"\n";
		    }
		}
		System.out.println(Resultado);
		objLCSV.escribirDatos(Salida, Resultado); 
	    }
	}
	System.out.println("******************* FIN  GENERA CSV -> XML *******************");	
    }
}

