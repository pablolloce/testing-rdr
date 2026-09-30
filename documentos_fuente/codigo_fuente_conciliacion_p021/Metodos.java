package util;

public class Metodos 
{
	public void time (long ini, String titulo)
	{
		long fin = System.currentTimeMillis();
		Integer tot = (int) (fin - ini);
		int h = (int) ((tot / (1000*60*60)) % 24);
		int m = (int) ((tot / (1000*60)) % 60) - h * 60; 
		int s = (int) ((tot / 1000)) - m * 60;
		String  ms1 = tot.toString();
		String ms = ms1.substring(ms1.length() -3, ms1.length());
	
		System.out.println( titulo + " : " + h + ":" + m + ":" + s + "." + ms);

	}
}
