package tools;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateUtil {
	public static String FechaSistemaCompletaString(){
		Calendar fecha = new GregorianCalendar();
		int anho = fecha.get(Calendar.YEAR);
        int mes = fecha.get(Calendar.MONTH)+1;
        int dia = fecha.get(Calendar.DAY_OF_MONTH);
        int hora=fecha.get(Calendar.HOUR_OF_DAY);
        int min=fecha.get(Calendar.MINUTE);
        int sec=fecha.get(Calendar.SECOND);
        
		String Res="";
		Res=String.valueOf(anho);
		if (mes<10){
			Res+="0"+String.valueOf(mes);
		}else{
			Res+=String.valueOf(mes);
		}
		if (dia<10){
			Res+="0"+String.valueOf(dia);
		}else{
			Res+=String.valueOf(dia);
		}
		if (hora<10){
			Res+="0"+String.valueOf(hora);
		}else{
			Res+=String.valueOf(hora);
		}
		if (min<10){
			Res+="0"+String.valueOf(min);
		}else{
			Res+=String.valueOf(min);
		}
		if (sec<10){
			Res+="0"+String.valueOf(sec);
		}else{
			Res+=String.valueOf(sec);
		}
		return Res;
	}
	public static String FechaSistemaGSString(){
		Calendar fecha = new GregorianCalendar();
		int anho = fecha.get(Calendar.YEAR);
        int mes = fecha.get(Calendar.MONTH)+1;
        int dia = fecha.get(Calendar.DAY_OF_MONTH);
        
		String Res="";
		Res=String.valueOf(anho)+"-";
		if (mes<10){
			Res+="0"+String.valueOf(mes)+"-";
		}else{
			Res+=String.valueOf(mes)+"-";
		}
		if (dia<10){
			Res+="0"+String.valueOf(dia);
		}else{
			Res+=String.valueOf(dia);
		}
		return Res;
	}
}
