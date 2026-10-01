package com.bbva.kytl.refinitivderivativesloader.entities;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name= "FT_T_REP1", schema="KYTL_GC")
public class FT_T_REP1 {

	@Id
	@Column(name = "REP1_OID")
	private String rep1Oid;
	
	@Column(name = "PROCESO")
	private String proceso;
	
	@Column(name = "TIPO")
	private String tipo;
	
	@Column(name = "DESCRIPCION")
	private String description;
	
	@Column(name = "QUERY")
	private String query;
	
	@Column(name = "RUTA")
	private String ruta;
	
	@Column(name = "SHORT_PROCESS")
	private String shorProcess;
	
	@Column(name = "CABECERA")
	private String cabecera;
	
	@Column(name = "SEND_PEND")
	private String sendPend;
	
	@Column(name = "EXCEL_TEMPLATE")
	private String excelTemplate;
	
	@Column(name = "EXCEL_SHEET")
	private String excelSheet;
	
	@Column(name = "ESTADISTICA")
	private String estadistica;
	
	@Embedded
	private DataFields dataFields;
	
	@Embedded
	private AuditFields auditFields;

	/**
	 * @return the rep1Oid
	 */
	public String getRep1Oid() {
		return rep1Oid;
	}

	/**
	 * @param rep1Oid the rep1Oid to set
	 */
	public void setRep1Oid(String rep1Oid) {
		this.rep1Oid = rep1Oid;
	}

	/**
	 * @return the proceso
	 */
	public String getProceso() {
		return proceso;
	}

	/**
	 * @param proceso the proceso to set
	 */
	public void setProceso(String proceso) {
		this.proceso = proceso;
	}

	/**
	 * @return the tipo
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * @param tipo the tipo to set
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * @return the ruta
	 */
	public String getRuta() {
		return ruta;
	}

	/**
	 * @param ruta the ruta to set
	 */
	public void setRuta(String ruta) {
		this.ruta = ruta;
	}

	/**
	 * @return the shorProcess
	 */
	public String getShorProcess() {
		return shorProcess;
	}

	/**
	 * @param shorProcess the shorProcess to set
	 */
	public void setShorProcess(String shorProcess) {
		this.shorProcess = shorProcess;
	}

	/**
	 * @return the cabecera
	 */
	public String getCabecera() {
		return cabecera;
	}

	/**
	 * @param cabecera the cabecera to set
	 */
	public void setCabecera(String cabecera) {
		this.cabecera = cabecera;
	}

	/**
	 * @return the sendPend
	 */
	public String getSendPend() {
		return sendPend;
	}

	/**
	 * @param sendPend the sendPend to set
	 */
	public void setSendPend(String sendPend) {
		this.sendPend = sendPend;
	}

	/**
	 * @return the excelTemplate
	 */
	public String getExcelTemplate() {
		return excelTemplate;
	}

	/**
	 * @param excelTemplate the excelTemplate to set
	 */
	public void setExcelTemplate(String excelTemplate) {
		this.excelTemplate = excelTemplate;
	}

	/**
	 * @return the excelSheet
	 */
	public String getExcelSheet() {
		return excelSheet;
	}

	/**
	 * @param excelSheet the excelSheet to set
	 */
	public void setExcelSheet(String excelSheet) {
		this.excelSheet = excelSheet;
	}

	/**
	 * @return the estadistica
	 */
	public String getEstadistica() {
		return estadistica;
	}

	/**
	 * @param estadistica the estadistica to set
	 */
	public void setEstadistica(String estadistica) {
		this.estadistica = estadistica;
	}

	/**
	 * @return the dataFields
	 */
	public DataFields getDataFields() {
		return dataFields;
	}

	/**
	 * @param dataFields the dataFields to set
	 */
	public void setDataFields(DataFields dataFields) {
		this.dataFields = dataFields;
	}

	/**
	 * @return the auditFields
	 */
	public AuditFields getAuditFields() {
		return auditFields;
	}

	/**
	 * @param auditFields the auditFields to set
	 */
	public void setAuditFields(AuditFields auditFields) {
		this.auditFields = auditFields;
	}
	
	
	
}
