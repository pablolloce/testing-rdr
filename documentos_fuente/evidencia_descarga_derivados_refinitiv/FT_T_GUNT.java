package com.bbva.kytl.refinitivderivativesloader.entities;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name= "FT_T_GUNT", schema="KYTL_GC")
public class FT_T_GUNT {

	@Id
	@Column(name = "GUNT_OID")
	private String guntOid;
	
	@Column(name = "PRNT_GU_ID")
	private String prntGuId;
	
	@Column(name = "PRNT_GU_TYP")
	private String prntGyTyp;
	
	@Column(name = "PRNT_GU_CNT")
	private String prntGuCnt;
	
	@Column(name = "CROSS_REF_ID")
	private String crossRefId;
	
	@Column(name = "NLS_CDE")
	private String nlsCde;
	
	@Column(name = "TMZ_TMZ")
	private String tmzTmz;
	
	@Column(name = "COMPONENT_SEPARATION_IND")
	private String calId;
	
	@Column(name = "PREF_CURR_CDE")
	private String prefCurrCde;
	
	@Column(name = "STOP_PAY_IND")
	private String stopPayInd;
	
	@Column(name = "GU_NME")
	private String guNme;
	
	@Column(name = "GU_DESC")
	private String guDesc;
	
	@Column(name = "CSD_PERMIT_IND")
	private String evcsdPermitInd;
	
	@Column(name = "CNTRY_CDE")
	private String cntryCde;
	
	@Column(name = "STE_PRV_CDE")
	private String stePrvCde;
	
	@Column(name = "REGION_NME")
	private String regionNme;
	
	@Column(name = "CNTY_NME")
	private String cntyNme;
	
	@Column(name = "TOWNSHIP_NME")
	private String townshipNme;
	
	@Column(name = "CITY_NME")
	private String cityNme;
	
	@Column(name = "POSTAL_CDE")
	private String postalCde;
	
	@Column(name = "CONTINENT_CDE")
	private String continentCde;
	
	@Column(name = "CNTRY_SUBDIV_CDE")
	private String cntrySubdivCde;
	
	@Column(name = "CNTY_CDE")
	private String cntyCde;
	
	@Column(name = "CNTY_CDE_TYP")
	private String cntyCdeTyp;
	
	@Column(name = "CITY_CDE")
	private String cityCde;
	
	@Column(name = "CITY_CDE_TYP")
	private String cityCdeTyp;
	
	@Column(name = "LATITUDE_DEC_DEGREE_NUM")
	private String latitudDecDegreeNum;
	
	@Column(name = "LONGITUDE_DEC_DEGREE_NUM")
	private String longitudeDecDegreeNum;
	
	@Column(name = "NATIONAL_ADJECTIVAL_TXT")
	private String nationalAdjectivalTxt;
	
	@Column(name = "GU_ID")
	private String guId;
	
	@Column(name = "GU_TYP")
	private String guTyp;
	
	@Column(name = "GU_CNT")
	private String guCnt;
	
	@Embedded
	private DataFields dataFields;
	
	@Embedded
	private AuditFields auditFields;

	public String getGuntOid() {
		return guntOid;
	}

	public void setGuntOid(String guntOid) {
		this.guntOid = guntOid;
	}

	public String getPrntGuId() {
		return prntGuId;
	}

	public void setPrntGuId(String prntGuId) {
		this.prntGuId = prntGuId;
	}

	public String getPrntGyTyp() {
		return prntGyTyp;
	}

	public void setPrntGyTyp(String prntGyTyp) {
		this.prntGyTyp = prntGyTyp;
	}

	public String getPrntGuCnt() {
		return prntGuCnt;
	}

	public void setPrntGuCnt(String prntGuCnt) {
		this.prntGuCnt = prntGuCnt;
	}

	public String getCrossRefId() {
		return crossRefId;
	}

	public void setCrossRefId(String crossRefId) {
		this.crossRefId = crossRefId;
	}

	public String getNlsCde() {
		return nlsCde;
	}

	public void setNlsCde(String nlsCde) {
		this.nlsCde = nlsCde;
	}

	public String getTmzTmz() {
		return tmzTmz;
	}

	public void setTmzTmz(String tmzTmz) {
		this.tmzTmz = tmzTmz;
	}

	public String getCalId() {
		return calId;
	}

	public void setCalId(String calId) {
		this.calId = calId;
	}

	public String getPrefCurrCde() {
		return prefCurrCde;
	}

	public void setPrefCurrCde(String prefCurrCde) {
		this.prefCurrCde = prefCurrCde;
	}

	public String getStopPayInd() {
		return stopPayInd;
	}

	public void setStopPayInd(String stopPayInd) {
		this.stopPayInd = stopPayInd;
	}

	public String getGuNme() {
		return guNme;
	}

	public void setGuNme(String guNme) {
		this.guNme = guNme;
	}

	public String getGuDesc() {
		return guDesc;
	}

	public void setGuDesc(String guDesc) {
		this.guDesc = guDesc;
	}

	public String getEvcsdPermitInd() {
		return evcsdPermitInd;
	}

	public void setEvcsdPermitInd(String evcsdPermitInd) {
		this.evcsdPermitInd = evcsdPermitInd;
	}

	public String getCntryCde() {
		return cntryCde;
	}

	public void setCntryCde(String cntryCde) {
		this.cntryCde = cntryCde;
	}

	public String getStePrvCde() {
		return stePrvCde;
	}

	public void setStePrvCde(String stePrvCde) {
		this.stePrvCde = stePrvCde;
	}

	public String getRegionNme() {
		return regionNme;
	}

	public void setRegionNme(String regionNme) {
		this.regionNme = regionNme;
	}

	public String getCntyNme() {
		return cntyNme;
	}

	public void setCntyNme(String cntyNme) {
		this.cntyNme = cntyNme;
	}

	public String getTownshipNme() {
		return townshipNme;
	}

	public void setTownshipNme(String townshipNme) {
		this.townshipNme = townshipNme;
	}

	public String getCityNme() {
		return cityNme;
	}

	public void setCityNme(String cityNme) {
		this.cityNme = cityNme;
	}

	public String getPostalCde() {
		return postalCde;
	}

	public void setPostalCde(String postalCde) {
		this.postalCde = postalCde;
	}

	public String getContinentCde() {
		return continentCde;
	}

	public void setContinentCde(String continentCde) {
		this.continentCde = continentCde;
	}

	public String getCntrySubdivCde() {
		return cntrySubdivCde;
	}

	public void setCntrySubdivCde(String cntrySubdivCde) {
		this.cntrySubdivCde = cntrySubdivCde;
	}

	public String getCntyCde() {
		return cntyCde;
	}

	public void setCntyCde(String cntyCde) {
		this.cntyCde = cntyCde;
	}

	public String getCntyCdeTyp() {
		return cntyCdeTyp;
	}

	public void setCntyCdeTyp(String cntyCdeTyp) {
		this.cntyCdeTyp = cntyCdeTyp;
	}

	public String getCityCde() {
		return cityCde;
	}

	public void setCityCde(String cityCde) {
		this.cityCde = cityCde;
	}

	public String getCityCdeTyp() {
		return cityCdeTyp;
	}

	public void setCityCdeTyp(String cityCdeTyp) {
		this.cityCdeTyp = cityCdeTyp;
	}

	public String getLatitudDecDegreeNum() {
		return latitudDecDegreeNum;
	}

	public void setLatitudDecDegreeNum(String latitudDecDegreeNum) {
		this.latitudDecDegreeNum = latitudDecDegreeNum;
	}

	public String getLongitudeDecDegreeNum() {
		return longitudeDecDegreeNum;
	}

	public void setLongitudeDecDegreeNum(String longitudeDecDegreeNum) {
		this.longitudeDecDegreeNum = longitudeDecDegreeNum;
	}

	public String getNationalAdjectivalTxt() {
		return nationalAdjectivalTxt;
	}

	public void setNationalAdjectivalTxt(String nationalAdjectivalTxt) {
		this.nationalAdjectivalTxt = nationalAdjectivalTxt;
	}

	public String getGuId() {
		return guId;
	}

	public void setGuId(String guId) {
		this.guId = guId;
	}

	public String getGuTyp() {
		return guTyp;
	}

	public void setGuTyp(String guTyp) {
		this.guTyp = guTyp;
	}

	public String getGuCnt() {
		return guCnt;
	}

	public void setGuCnt(String guCnt) {
		this.guCnt = guCnt;
	}

	public DataFields getDataFields() {
		return dataFields;
	}

	public void setDataFields(DataFields dataFields) {
		this.dataFields = dataFields;
	}

	public AuditFields getAuditFields() {
		return auditFields;
	}

	public void setAuditFields(AuditFields auditFields) {
		this.auditFields = auditFields;
	}

	
}
