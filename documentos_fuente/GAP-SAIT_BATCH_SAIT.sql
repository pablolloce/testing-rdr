									SELECT (XMLELEMENT (NAME "Agreement",      				
            				         XMLELEMENT (NAME "AgreementID", (SELECT LAID.LEGAL_AGRMNT_ID
            				                                          FROM ft_t_laid laid
            				                                          WHERE lagr.LEG_AGRMNT_ID = LAID.LEG_AGRMNT_ID
            				                                            AND lagr.ORG_ID = LAID.ORG_ID
            				                                            AND laid.data_stat_typ = 'ACTIVE'
            				                                            AND laid.data_src_id = 'Generic' and rownum = 1)),
            				          XMLELEMENT (NAME "AgmtMultiBrInd", 
            				          XMLELEMENT (NAME "AgmtCPMultBrInd",( select flar.mult_branch_ind 
            				          FROM KYTL_GC.ft_t_flar flar
            				          WHERE flar.org_id = LAGR.org_id
            				          AND flar.leg_agrmnt_id = LAGR.leg_agrmnt_id
            				          AND flar.rl_typ  = 'EXTERNAL' 
            				          AND flar.data_stat_typ = 'ACTIVE' 
            				          and rownum = 1)),
            				          XMLELEMENT (NAME "AgmtMultBrInd",('M��XICO')    
            				          )),                                  
							         (SELECT XMLAGG(XMLELEMENT (NAME "Pty",
							                XMLELEMENT (NAME "ID", (SELECT fins_id
							                                        FROM KYTL_GC.FT_T_FIID fiid
							                                        WHERE fiid.inst_mnem = flar.inst_mnem
							                                          AND fiid.fins_id_ctxt_typ = 'FINSID'
							                                          AND fiid.data_stat_typ = 'ACTIVE' and rownum = 1)),  									  
											XMLELEMENT  (NAME "IDSTAR", (select finr_id from ft_t_frid frid where frid.finsrl_id_ctxt_typ='STARID' 
							                and frid.finsrl_typ='CPARTY  ' and frid.data_stat_typ='ACTIVE' and frid.data_src_id='STAR_MEXICO' and frid.inst_mnem = flar.inst_mnem and flar.data_stat_typ='ACTIVE' and rownum = 1)),						  
							                XMLELEMENT  (NAME "AgmtClientTypInd", 'Y'),	
											XMLELEMENT (NAME "Src", 'O'),
							                XMLELEMENT (NAME "PartyShort", (SELECT FINR_ID FROM FT_T_FRID FRID WHERE  FRID.FINSRL_ID_CTXT_TYP = 'SHTNMEID' AND FRID.DATA_STAT_TYP = 'ACTIVE' and rownum = 1 and FRID.INST_MNEM = flar.INST_MNEM
							                                                                        AND TRIM(FRID.FINSRL_TYP) = (SELECT TRIM(STAT_CHAR_VAL_TXT)
							                                                                         FROM FT_T_FIST  WHERE INST_MNEM=flar.INST_MNEM AND STAT_DEF_ID='MAINROL' and rownum = 1))),
							                XMLELEMENT (NAME "PartyName", (SELECT fins.inst_legal_nme
							                                               FROM KYTL_GC.ft_t_fins fins
							                                               WHERE fins.data_stat_typ = 'ACTIVE' and rownum = 1
							                                                AND fins.inst_mnem = (SELECT LOC.prnt_inst_mnem
							                                                                      FROM ft_t_firl LOC,ft_t_firl OPE
							                                                                      WHERE LOC.rel_typ = 'LOCAL'
							                                                                        AND OPE.rel_typ = 'OPERATIVE'
							                                                                        AND LOC.inst_mnem =OPE.prnt_inst_mnem
							                                                                        AND OPE.inst_mnem = flar.inst_mnem and rownum = 1))),
							                XMLELEMENT (NAME "R", 'Matrix'),
							                XMLELEMENT (NAME "Sub",
							                    XMLELEMENT (NAME "ID", (SELECT fins.inst_legal_nme
							                                            FROM KYTL_GC.ft_t_fins fins
							                                            WHERE fins.data_stat_typ = 'ACTIVE' and rownum = 1
							                                              AND fins.inst_mnem = (SELECT LOC.prnt_inst_mnem
							                                                                    FROM ft_t_firl LOC,ft_t_firl OPE
							                                                                    WHERE LOC.rel_typ = 'LOCAL'
							                                                                      AND OPE.rel_typ = 'OPERATIVE'
							                                                                      AND LOC.inst_mnem =OPE.prnt_inst_mnem
							                                                                      AND OPE.inst_mnem = flar.inst_mnem and rownum = 1))),
							                    XMLELEMENT (NAME "Typ", '5')),
							                XMLELEMENT (NAME "Sub",
							                    XMLELEMENT (NAME "ID", (SELECT trim(figu.gu_id)
							                                            FROM KYTL_GC.ft_t_figu figu
							                                            WHERE figu.gu_typ = 'COUNTRY'
							                                              AND figu.fins_gu_purp_typ = 'STSMNTCT'
							                                              AND figu.data_stat_typ = 'ACTIVE' and rownum = 1
							                                              AND figu.inst_mnem = (SELECT LOC.prnt_inst_mnem
							                                                                    FROM ft_t_firl LOC,ft_t_firl OPE
							                                                                    WHERE LOC.rel_typ = 'LOCAL'
							                                                                      AND OPE.rel_typ = 'OPERATIVE'
							                                                                      AND LOC.inst_mnem =OPE.prnt_inst_mnem
							                                                                      AND OPE.inst_mnem = flar.inst_mnem and rownum = 1))),
							                    XMLELEMENT (NAME "Typ", '39'))))
							         FROM KYTL_GC.ft_t_flar flar
							         WHERE lagr.leg_agrmnt_id = flar.leg_agrmnt_id
							          AND lagr.org_id = flar.org_id
							          AND flar.rl_typ  = 'EXTERNAL'),
							         (SELECT XMLAGG(XMLELEMENT (NAME "Pty",
							                XMLELEMENT (NAME "ID", (SELECT fins_id
							                                        FROM KYTL_GC.FT_T_FIID fiid
							                                        WHERE fiid.inst_mnem = flar.inst_mnem
							                                          AND fiid.fins_id_ctxt_typ = 'FINSID'
							                                          AND fiid.data_stat_typ = 'ACTIVE' and rownum = 1)),
											 XMLELEMENT  (NAME "IDSTAR", (select finr_id from ft_t_frid frid where frid.finsrl_id_ctxt_typ='STARID' 
							                and frid.finsrl_typ='CPARTY  ' and frid.data_stat_typ='ACTIVE' and frid.data_src_id='STAR_MEXICO' and frid.inst_mnem = flar.inst_mnem and flar.data_stat_typ='ACTIVE' and rownum = 1 )),		  
							                XMLELEMENT (NAME "Src", 'O'),
							                XMLELEMENT (NAME "R", 'Enterprise')))
							         FROM KYTL_GC.ft_t_flar flar
							         WHERE lagr.leg_agrmnt_id = flar.leg_agrmnt_id
							            AND lagr.org_id = flar.org_id
							            AND flar.rl_typ  = 'INTERNAL'),
            				         XMLELEMENT (NAME "FinDetls",
            				                XMLELEMENT (NAME "AgmtDesc", lagr.agrmnt_desc),
            				                XMLELEMENT (NAME "AgmtID", (SELECT LAID.LEGAL_AGRMNT_ID
            				                                            FROM ft_t_laid laid
            				                                            WHERE lagr.LEG_AGRMNT_ID = LAID.LEG_AGRMNT_ID
            				                                              AND lagr.ORG_ID = LAID.ORG_ID
            				                                              AND laid.data_stat_typ = 'ACTIVE'
            				                                              AND laid.data_src_id = 'Generic' and rownum = 1 )),
							             XMLELEMENT (NAME "AgmtTyp", trim(lagr.AGRMNT_TYP)),
            								XMLELEMENT (NAME "AgmtTypCve", (select idmv.intrnl_dmn_desc from ft_t_idmv idmv 
            								where idmv.INTRNL_DMN_VAL_NME = lagr.AGRMNT_TYP 
            								and idmv.INTRNL_DMN_VAL_TXT = lagr.AGRMNT_TYP  
            								and idmv.data_stat_typ = 'ACTIVE' and col_nme ='AGRMNT_TYP' and rownum = 1)),
            				                XMLELEMENT (NAME "AgmtStat", lagr.data_stat_typ),
            				                XMLELEMENT (NAME "AgmtDt", lagr.agrmnt_sign_dte),
            				                XMLELEMENT (NAME "StartDt", lagr.doc_eff_dte_tms),
            				                XMLELEMENT (Name "EndDt", Lagr.Exp_Tms),
            				                XMLELEMENT (NAME "AgrVersion", lagr.agrmnt_version_yr_typ),
            				                XMLELEMENT (NAME "AgmtCcy", (decode (lagr.agrmnt_curr_cde,null,'ALL',lagr.agrmnt_curr_cde))),
            				                XMLELEMENT (NAME "AgmInclExcl", (SELECT DECODE (count(lars.RST_REAS_TYP),0,'N','Y')
            				                                             FROM  KYTL_GC.ft_t_lars lars
            				                                             WHERE lars.RST_REAS_TYP = 'TRAD_INC'
            				                                             AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID and rownum = 1)),
            				                (SELECT XMLAGG(XMLELEMENT (NAME "AgmtTrdTyp",
            				                                XMLELEMENT (NAME "TrdCod", TRIM(EXEC_TRD_ID)),
            				                                XMLELEMENT (NAME "TrdSrc", trim(DATA_SRC_ID))))
            				                 FROM  KYTL_GC.ft_t_lars lars
            				                 WHERE lars.RST_REAS_TYP = 'TRAD_INC'
            				                    AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID),
            				                (SELECT XMLAGG(XMLELEMENT (NAME "AgmtTrdTyp",
            				                                XMLELEMENT (NAME "TrdCod", TRIM(EXEC_TRD_ID)),
            				                                XMLELEMENT (NAME "TrdSrc", trim(DATA_SRC_ID))))
            				                 FROM  KYTL_GC.ft_t_lars lars
            				                 WHERE lars.RST_REAS_TYP = 'TRAD_EXC'
            				                    AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID),
            								XMLELEMENT (Name "AgmtDocID", lagr.LEG_AGRMNT_DOC_ID),	
            								XMLELEMENT (Name "AgmtCreatedTMS", lagr.CREATED_TMS),
            								XMLELEMENT (NAME "NLS_CDE", (select lag1.nls_cde from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),												 
            								XMLELEMENT (NAME "Product32", (select laid.legal_agrmnt_id from ft_t_laid laid where laid.lagr_id_ctxt_typ = 'PRODUCT32' and laid.data_stat_typ = 'ACTIVE' and  lagr.leg_agrmnt_id = laid.leg_agrmnt_id and lagr.org_id = laid.org_id and rownum = 1)),				
            								XMLELEMENT (NAME "Bancomercom", (select lag1.bancomercom from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),											 
            								XMLELEMENT (NAME "Tax_Gain", (select lag1.tax_gain from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),	
            								XMLELEMENT (NAME "Netcash", (select lag1.netcash from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            								XMLELEMENT (NAME "AgmtRefCli", (select lag1.ref_clt_txt from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            								XMLELEMENT (NAME "AgmtCNLRSN", (select lag1.agrmnt_cnl_rsn from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            								XMLELEMENT (NAME "AgmtObser", lagr.agrmnt_cmnt_txt),
											 XMLELEMENT (NAME "Last_Chg_Usr", (SELECT substr((to_char(GREATEST(
       				                (nvl(MAX(lagr.last_chg_tms||' '||lagr.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laid.last_chg_tms||' '||laid.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(flar.last_chg_tms||' '||flar.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lag1.last_chg_tms||' '||lag1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lat1.last_chg_tms||' '||lat1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laan.last_chg_tms||' '||laan.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lac1.last_chg_tms||' '||lac1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lars.last_chg_tms||' '||lars.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laap.last_chg_tms||' '||laap.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lacd.last_chg_tms||' '||lacd.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lad1.last_chg_tms||' '||lad1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(cnta.last_chg_tms||' '||cnta.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(cntc.last_chg_tms||' '||cntc.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lap1.last_chg_tms||' '||lap1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(aclp.last_chg_tms||' '||aclp.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(acct.last_chg_tms||' '||acct.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lar1.last_chg_tms||' '||lar1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD')))
            				        ))),
            				        instr((to_char(GREATEST(
            				        (nvl(MAX(lagr.last_chg_tms||' '||lagr.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laid.last_chg_tms||' '||laid.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(flar.last_chg_tms||' '||flar.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lag1.last_chg_tms||' '||lag1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lat1.last_chg_tms||' '||lat1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laan.last_chg_tms||' '||laan.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lac1.last_chg_tms||' '||lac1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lars.last_chg_tms||' '||lars.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(laap.last_chg_tms||' '||laap.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lacd.last_chg_tms||' '||lacd.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lad1.last_chg_tms||' '||lad1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(cnta.last_chg_tms||' '||cnta.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(cntc.last_chg_tms||' '||cntc.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lap1.last_chg_tms||' '||lap1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(aclp.last_chg_tms||' '||aclp.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(acct.last_chg_tms||' '||acct.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD'))),
            				        (nvl(MAX(lar1.last_chg_tms||' '||lar1.last_chg_usr_id), to_date('0001/01/01','YYYY-MM-DD')))
            				        ))),' ') + 1
            				        )
            							FROM ft_t_flar flar 
											LEFT JOIN ft_t_laid laid ON laid.leg_agrmnt_id = flar.leg_agrmnt_id and laid.org_id = flar.org_id 
											LEFT JOIN ft_t_aclp aclp ON aclp.lagr_leg_agrmnt_id = flar.leg_agrmnt_id and aclp.lagr_org_id = flar.org_id 
											LEFT JOIN ft_t_acct acct ON acct.org_id = aclp.acct_org_id and acct.bk_id = aclp.acct_bk_id and acct.acct_id = aclp.acct_id and aclp.lagr_leg_agrmnt_id = flar.leg_agrmnt_id and aclp.lagr_org_id = flar.org_id 
											LEFT JOIN ft_t_lap1 lap1 ON lap1.flar_oid = flar.flar_oid     
											LEFT JOIN ft_t_lat1 lat1 ON lat1.leg_agrmnt_id = flar.leg_agrmnt_id and lat1.org_id = flar.org_id 
											LEFT JOIN ft_t_lag1 lag1 ON lag1.leg_agrmnt_id = flar.leg_agrmnt_id and lag1.org_id = flar.org_id 
											LEFT JOIN ft_t_laan laan ON laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_laap laap ON laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id  
											LEFT JOIN ft_t_lacd lacd ON lacd.laap_oid = laap.laap_oid and laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_lad1 lad1 ON lad1.lacd_oid = lacd.lacd_oid and lacd.laap_oid = laap.laap_oid and laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_lac1 lac1 ON lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_cnta cnta ON cnta.cnta_oid = lac1.cnta_oid and lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_cntc cntc ON cntc.contct_oid = cnta.contct_oid and cnta.cnta_oid = lac1.cnta_oid and lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_lars lars ON lars.leg_agrmnt_id = flar.leg_agrmnt_id and lars.org_id = flar.org_id    
											LEFT JOIN ft_t_lar1 lar1 ON lars.lars_oid = lar1.lars_oid and lars.leg_agrmnt_id = flar.leg_agrmnt_id and lars.org_id = flar.org_id 
											where flar.leg_agrmnt_id = lagr.leg_agrmnt_id and flar.org_id = lagr.org_id) 
			            		        ), 
            								XMLELEMENT (NAME "Last_Chg_Tms", (SELECT (GREATEST(
            				    			nvl(MAX(lagr.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(laid.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(flar.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lag1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lat1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(laan.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lac1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lars.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(laap.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lacd.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lad1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(cnta.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(cntc.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(lap1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(aclp.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),
            								nvl(MAX(acct.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD')),     
            								nvl(MAX(lar1.last_chg_tms), to_date('0001/01/01','YYYY-MM-DD'))
            								)) 
            							FROM ft_t_flar flar 
											LEFT JOIN ft_t_laid laid ON laid.leg_agrmnt_id = flar.leg_agrmnt_id and laid.org_id = flar.org_id 
											LEFT JOIN ft_t_aclp aclp ON aclp.lagr_leg_agrmnt_id = flar.leg_agrmnt_id and aclp.lagr_org_id = flar.org_id 
											LEFT JOIN ft_t_acct acct ON acct.org_id = aclp.acct_org_id and acct.bk_id = aclp.acct_bk_id and acct.acct_id = aclp.acct_id and aclp.lagr_leg_agrmnt_id = flar.leg_agrmnt_id and aclp.lagr_org_id = flar.org_id 
											LEFT JOIN ft_t_fins fins ON fins.inst_mnem = flar.inst_mnem
											LEFT JOIN ft_t_lap1 lap1 ON lap1.flar_oid = flar.flar_oid     
											LEFT JOIN ft_t_lat1 lat1 ON lat1.leg_agrmnt_id = flar.leg_agrmnt_id and lat1.org_id = flar.org_id 
											LEFT JOIN ft_t_lag1 lag1 ON lag1.leg_agrmnt_id = flar.leg_agrmnt_id and lag1.org_id = flar.org_id 
											LEFT JOIN ft_t_laan laan ON laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_laap laap ON laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id  
											LEFT JOIN ft_t_lacd lacd ON lacd.laap_oid = laap.laap_oid and laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_lad1 lad1 ON lad1.lacd_oid = lacd.lacd_oid and lacd.laap_oid = laap.laap_oid and laan.laan_oid = laap.laan_oid and laan.leg_agrmnt_id = flar.leg_agrmnt_id and laan.org_id = flar.org_id 
											LEFT JOIN ft_t_lac1 lac1 ON lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_cnta cnta ON cnta.cnta_oid = lac1.cnta_oid and lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_cntc cntc ON cntc.contct_oid = cnta.contct_oid and cnta.cnta_oid = lac1.cnta_oid and lac1.leg_agrmnt_id = flar.leg_agrmnt_id and lac1.org_id = flar.org_id 
											LEFT JOIN ft_t_lars lars ON lars.leg_agrmnt_id = flar.leg_agrmnt_id and lars.org_id = flar.org_id    
											LEFT JOIN ft_t_lar1 lar1 ON lars.lars_oid = lar1.lars_oid and lars.leg_agrmnt_id = flar.leg_agrmnt_id and lars.org_id = flar.org_id 
											where flar.leg_agrmnt_id = lagr.leg_agrmnt_id and flar.org_id = lagr.org_id) 
			            		        ),                  
											XMLELEMENT (NAME "Other",
			            						XMLELEMENT (NAME "AgmtAppKey",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'APP_KEY' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtCollInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'COLL_IND' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtSndInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'SND_IND' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtExnInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'EXN_IND' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtConfInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'CONF_IND' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtSucNum",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'SUBDIV_ID' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtFldNum",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'FLD_NUM' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtOblInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'OBL_IND' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtBnkCliInd",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'REF_CLT' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtLngFrmConf",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'LONG_FORM' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtBrkTyp",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'PRT_BRK' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtBLKStat",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'BLK_STAT' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtObvTxt",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'OBV_TXT' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtRskTxt",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'RSK_TXT' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "AgmtBrkName",(select cntc.CONTCT_FULL_NME
			            						from ft_t_cntc cntc, ft_t_cnta cnta, ft_t_lac1 lac1
			            						where cnta.contct_assign_purp_typ='TRADER' 
			            						and cnta.data_stat_typ='ACTIVE' 
			            						and cntc.data_stat_typ='ACTIVE'
			            						and lac1.data_stat_typ='ACTIVE'
			            						and cnta.contct_oid = cntc.contct_oid 
			            						and lac1.cnta_oid = cnta.cnta_oid
			            						and lagr.leg_agrmnt_id = lac1.leg_agrmnt_id
			            						and lagr.org_id = lac1.org_id and rownum = 1)),					
			            						XMLELEMENT (NAME "RepurchaseOblig",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'REP_OBLG' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),
			            						XMLELEMENT (NAME "Institutional_Inv",(select lat1.fld_val 
			            						from ft_t_lat1 lat1
			            						where lat1.stat_def_id = 'INV_INSTIT' 
			            						and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null
			            						and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
			            						and lagr.org_id = lat1.org_id and rownum = 1)),		
			            						XMLELEMENT (NAME "AccountNum",(select acct.acct_nme 
			            						from ft_t_acct acct, ft_t_aclp aclp
			            						where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
			            						and aclp.lagr_org_id = lagr.org_id
			            						and acct.org_id = aclp.acct_org_id
			            						and aclp.prt_purp_typ = 'SECURITY'
			            						and acct.bk_id = aclp.acct_bk_id
			            						and acct.acct_id = aclp.acct_id
			            						and aclp.data_stat_typ='ACTIVE'
			            						and acct.data_stat_typ='ACTIVE' and rownum = 1)),
			            						XMLELEMENT (NAME "AccountOffice",(select acct.ACTP_ORG_ID
			            						from ft_t_acct acct, ft_t_aclp aclp
			            						where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
			            						and aclp.lagr_org_id = lagr.org_id
			            						and aclp.prt_purp_typ = 'SECURITY'
			            						and acct.org_id = aclp.acct_org_id
			            						and acct.bk_id = aclp.acct_bk_id
			            						and acct.acct_id = aclp.acct_id
			            						and aclp.data_stat_typ='ACTIVE'
			            						and acct.data_stat_typ='ACTIVE' and rownum = 1)),
			            						XMLELEMENT (NAME "AccountStatus",(select acct.acct_stat_typ 
			            						from ft_t_acct acct, ft_t_aclp aclp
			            						where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
			            						and aclp.lagr_org_id = lagr.org_id
			            						and aclp.prt_purp_typ = 'SECURITY'
			            						and acct.org_id = aclp.acct_org_id
			            						and acct.bk_id = aclp.acct_bk_id
			            						and acct.acct_id = aclp.acct_id
			            						and aclp.data_stat_typ='ACTIVE'
			            						and acct.data_stat_typ='ACTIVE' and rownum = 1))
			            					),
            								XMLELEMENT (NAME "AgmtIndi",
            									XMLELEMENT (NAME "AgmtSuppInd",(select lag1.agrmnt_supp_ind from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtSuppTms",(select lag1.agrmnt_supp_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtAmntInd",(select lag1.agrmnt_amnt_ind from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtAmntTms",(select lag1.agrmnt_amnt_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtOpeMemInd",(select lag1.agrmnt_ope_mem_ind from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtOpeMemTms",(select lag1.agrmnt_ope_mem_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtCommInd",(select lag1.agrmnt_comm_ind from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtCommTms",(select lag1.agrmnt_comm_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtRemAccInd",(select lag1.agrmnt_rem_acc_ind from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtRemAccTms",(select lag1.agrmnt_rem_acc_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1))
            								),
            								XMLELEMENT (NAME "AgmtSettle",
            								XMLELEMENT (NAME "AgmtEntryForm", (select lag1.SSI_ENTRY_FORM from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            								XMLELEMENT (NAME "AgmtEntryFormTms",(select lag1.SSI_ENTRY_FORM_TMS from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            								XMLELEMENT (NAME "AgmtFixSettleInd", (select lat1.fld_val 
            									from ft_t_lat1 lat1
            									where lat1.stat_def_id = 'FIX_SSI' 
            									and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
            									and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
            									and lagr.org_id = lat1.org_id and rownum = 1)),
            								XMLELEMENT (NAME "AgmtFixVndSettleTyp", (select lat1.fld_val 
            									from ft_t_lat1 lat1
            									where lat1.stat_def_id = 'FIX_VND' 
            									and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
            									and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
            									and lagr.org_id = lat1.org_id and rownum = 1)),
            								XMLELEMENT (NAME "AgmtNoFixVndSettleTyp", (select lat1.fld_val 
            									from ft_t_lat1 lat1
            									where lat1.stat_def_id = 'N_FIX_VND' 
            									and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
            									and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
            									and lagr.org_id = lat1.org_id and rownum = 1)),
            								XMLELEMENT (NAME "AgmtNoFixVndSettleUnqTyp",(select lag1.AGRMNT_N_FIX_UNIQ 
            									from ft_t_lag1 lag1
            									where lag1.org_id = lagr.org_id	
            									and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id 
            									and lag1.data_stat_typ='ACTIVE' and rownum = 1))
            								),
            								XMLELEMENT (NAME "AgmtDer",
            									XMLELEMENT (NAME "AgmtAuthTyp",(select lag1.agrmnt_auth_typ from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtAuthEndTms",(select lag1.agrmnt_auth_end_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1))
            								),
            								XMLELEMENT (NAME "AgmtSig",
            									XMLELEMENT (NAME "AgmtSigTyp",(select lat1.fld_val 
            									from ft_t_lat1 lat1
            									where lat1.stat_def_id = 'SIGN_TYP' 
            									and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
            									and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
            									and lagr.org_id = lat1.org_id and rownum = 1)),
            									XMLELEMENT (NAME "AgmtSigValTyp",(select lat1.fld_val 
            									from ft_t_lat1 lat1
            									where lat1.stat_def_id = 'SIGN_VAL' 
            									and lat1.data_stat_typ = 'ACTIVE' and lat1.end_tms is null 
            									and lagr.leg_agrmnt_id = lat1.leg_agrmnt_id 
            									and lagr.org_id = lat1.org_id and rownum = 1))
            								),
            								XMLELEMENT (NAME "AgmtLegalRev",
            									XMLELEMENT (NAME "AgmtRevNum",(select lag1.agrmnt_rev_num from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtEntTms",(select lag1.agrmnt_ent_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtOutTms",(select lag1.agrmnt_out_tms from ft_t_lag1 lag1 where lag1.org_id = lagr.org_id	and lag1.leg_agrmnt_id = lagr.leg_agrmnt_id and lag1.data_stat_typ='ACTIVE' and rownum = 1))
            								)	
            						 ),
							   XMLELEMENT (NAME "PtySecT", 
							                (SELECT XMLAGG(XMLELEMENT (NAME "Pty", 
							                      XMLELEMENT (NAME "ID", (SELECT fins_id
							                                              FROM KYTL_GC.FT_T_FIID fiid
							                                              WHERE fiid.inst_mnem = laip.inst_mnem 
							                                              AND fiid.fins_id_ctxt_typ = 'FINSID'
							                                              AND fiid.data_stat_typ = 'ACTIVE' and rownum = 1)),
												  XMLELEMENT  (NAME "IDSTAR", (select finr_id from ft_t_frid frid where frid.finsrl_id_ctxt_typ='STARID' 
												  and frid.finsrl_typ='CPARTY  ' and frid.data_stat_typ='ACTIVE' and frid.data_src_id='STAR_MEXICO' and frid.inst_mnem = laip.inst_mnem and laip.data_stat_typ='ACTIVE' and flar.data_stat_typ = 'ACTIVE' and rownum = 1)),											  
												  XMLELEMENT  (NAME "AgmtClientTypInd", 'N'),
												  XMLELEMENT (NAME "Src", 'O'),					  
							                      XMLELEMENT (NAME "R", 'CHILD'),
							                      XMLELEMENT (NAME "PartyShort",  (SELECT FINR_ID FROM FT_T_FRID FRID WHERE  FRID.FINSRL_ID_CTXT_TYP = 'SHTNMEID' and rownum = 1 AND FRID.DATA_STAT_TYP = 'ACTIVE' and FRID.INST_MNEM = laip.INST_MNEM
							                                                                        AND TRIM(FRID.FINSRL_TYP) = (SELECT TRIM(STAT_CHAR_VAL_TXT)
							                                                                         FROM FT_T_FIST  WHERE INST_MNEM=laip.INST_MNEM AND STAT_DEF_ID='MAINROL' and rownum = 1))),
							                      XMLELEMENT (NAME "PartyName", (SELECT fins.inst_legal_nme
							                                                     FROM KYTL_GC.ft_t_fins fins
							                                                     WHERE fins.data_stat_typ = 'ACTIVE' and rownum = 1
							                                                       AND fins.inst_mnem = (SELECT LOC.prnt_inst_mnem
							                                                                             FROM ft_t_firl LOC,ft_t_firl OPE
							                                                                             WHERE LOC.rel_typ = 'LOCAL'
							                                                                                AND OPE.rel_typ = 'OPERATIVE'
							                                                                                AND LOC.inst_mnem =OPE.prnt_inst_mnem
							                                                                                AND OPE.inst_mnem = laip.inst_mnem and rownum = 1 )))))
							                FROM KYTL_GC.ft_t_flar flar, KYTL_GC.ft_t_laip laip
							                WHERE flar.org_id = lagr.org_id
							                  AND flar.leg_agrmnt_id = lagr.leg_agrmnt_id
							                  AND flar.flar_oid = laip.flar_oid
							                  AND laip.agrmnt_invl_party_typ  = 'BRANCH'
							                  AND flar.rl_typ  = 'EXTERNAL' 
							                  AND flar.data_stat_typ = 'ACTIVE'
							                  AND laip.data_stat_typ = 'ACTIVE'),
							                (SELECT XMLAGG(XMLELEMENT (NAME "Pty",
							                      XMLELEMENT (NAME "ID", (SELECT fins_id
							                                              FROM KYTL_GC.FT_T_FIID fiid
							                                              WHERE fiid.inst_mnem = laip.inst_mnem 
							                                              AND fiid.fins_id_ctxt_typ = 'FINSID'
							                                              AND fiid.data_stat_typ = 'ACTIVE' and rownum = 1)),
												  XMLELEMENT  (NAME "IDSTAR", (select finr_id from ft_t_frid frid where frid.finsrl_id_ctxt_typ='STARID' 
							                      and frid.finsrl_typ='CPARTY  ' and frid.data_stat_typ='ACTIVE' and frid.data_src_id='STAR_MEXICO' and frid.inst_mnem = laip.inst_mnem and laip.data_stat_typ='ACTIVE' and flar.data_stat_typ = 'ACTIVE' and rownum = 1)),											  
												  XMLELEMENT (NAME "Src", 'O'),
							                      XMLELEMENT (NAME "R", 'BRANCH'),
							                      XMLELEMENT (NAME "PartyShort",  (SELECT FINR_ID FROM FT_T_FRID FRID WHERE  FRID.FINSRL_ID_CTXT_TYP = 'SHTNMEID' AND FRID.DATA_STAT_TYP = 'ACTIVE' and rownum = 1 and FRID.INST_MNEM = laip.INST_MNEM
							                                                                        AND TRIM(FRID.FINSRL_TYP) = (SELECT TRIM(STAT_CHAR_VAL_TXT)
							                                                                         FROM FT_T_FIST  WHERE INST_MNEM=laip.INST_MNEM AND STAT_DEF_ID='MAINROL' and rownum = 1))),
							                      XMLELEMENT (NAME "PartyName", (SELECT fins.inst_legal_nme
							                                                     FROM KYTL_GC.ft_t_fins fins
							                                                     WHERE fins.data_stat_typ = 'ACTIVE' and rownum = 1
							                                                       AND fins.inst_mnem = (SELECT LOC.prnt_inst_mnem
							                                                                             FROM ft_t_firl LOC,ft_t_firl OPE
							                                                                             WHERE LOC.rel_typ = 'LOCAL'
							                                                                                AND OPE.rel_typ = 'OPERATIVE'
							                                                                                AND LOC.inst_mnem =OPE.prnt_inst_mnem
							                                                                                AND OPE.inst_mnem = laip.inst_mnem and rownum = 1 ))
							                                                                                )))
							                FROM KYTL_GC.ft_t_flar flar, KYTL_GC.ft_t_laip laip
							                WHERE flar.org_id = lagr.org_id
							                  AND flar.leg_agrmnt_id = lagr.leg_agrmnt_id
							                  AND flar.flar_oid = laip.flar_oid
							                  AND laip.agrmnt_invl_party_typ  = 'BRANCH'
							                  AND flar.rl_typ  = 'INTERNAL'
							                  AND flar.data_stat_typ = 'ACTIVE'
							                  AND laip.data_stat_typ = 'ACTIVE'), 
											  (SELECT XMLAGG(XMLELEMENT (NAME "SecT",
											  XMLELEMENT (NAME "AgmtProdListNme",(select ITGR.GRP_NME from ft_t_itgr itgr where rownum = 1 and itgr.iss_typ_grp_oid  in (select lar1.iss_typ_grp_id from ft_t_lar1 lar1 where lars.lars_oid = lar1.lars_oid and rownum = 1))),   
											  XMLELEMENT (NAME "SecTypID", trim(isty.iss_typ_nme)),
											  XMLELEMENT (NAME "Desc", isty.iss_typ_desc),
											  XMLELEMENT (NAME "AgmtProdUnderlying", (select LAR1.ISS_TYP_COMP from ft_t_lar1 lar1 where lars.lars_oid = lar1.lars_oid and rownum = 1))
											  ))
											  FROM  KYTL_GC.ft_t_isty isty,ft_t_lars lars
											  WHERE lars.iss_typ = isty.iss_typ
											  AND lars.RST_REAS_TYP = 'Prod_Inc'
											  AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID
											  AND lars.data_stat_typ = 'ACTIVE'),
											 (SELECT XMLAGG(XMLELEMENT (NAME "SecT",
											  XMLELEMENT (NAME "AgmtProdListNme",(itgr.GRP_NME)),
											  XMLELEMENT (NAME "SecTypID",(select isty.iss_typ_nme from ft_t_isty isty where isty.iss_typ = itgp.iss_typ and rownum = 1)),
											  XMLELEMENT (NAME "Desc",(select isty.iss_typ_desc from ft_t_isty isty where isty.iss_typ = itgp.iss_typ and rownum = 1)),  
											  XMLELEMENT (NAME "AgmtProdUnderlying",(LAR1.ISS_TYP_COMP))))
                                              from ft_t_lar1 lar1, ft_t_lars lars, ft_t_itgr itgr, ft_t_itgp itgp 
                                              where lars.lars_oid = lar1.lars_oid
                                              and itgp.prnt_isty_grp_oid = itgr.iss_typ_grp_oid
                                              and lars.leg_agrmnt_id = lagr.leg_agrmnt_id
                                              and lars.org_id = lagr.org_id
                                              and lars.rst_typ = 'PROD_LST'
                                              and lar1.data_stat_typ = 'ACTIVE'
                                              and lars.data_stat_typ = 'ACTIVE'
                                              and itgr.iss_typ_grp_oid = lar1.iss_typ_grp_id
                                              and itgr.data_stat_typ ='ACTIVE'
                                              and itgr.grp_purp_typ = 'PROD_LST')                  
										), 
					(SELECT XMLAGG(XMLELEMENT (NAME "Coll",
					XMLELEMENT (NAME "CollID", trim(laan.laan_oid)),
                    XMLELEMENT (NAME "Coll_Typ", trim(laan.annex_typ)),                   
                    XMLELEMENT (NAME "Coll_StartTMS", (laan.annex_sign_dte)),
                    XMLELEMENT (NAME "Coll_EligblTyp", (select isty.iss_typ_nme 
					from ft_t_isty isty, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd 
					where isty.iss_typ = lacd.iss_typ 
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE' and rownum = 1)),
                    XMLELEMENT (NAME "Coll_Vcl", (select lad1.COLLAT_VCL
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),
                    XMLELEMENT (NAME "Coll_EjctNME", (select lad1.COLLAT_EJCT_NME
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),                 
                    XMLELEMENT (NAME "Coll_EjctTMS", (select lad1.COLLAT_EJCT_TMS
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),
                    XMLELEMENT (NAME "Coll_ConvTXT", (select lad1.COLLAT_CONV_TXT
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),                
                    XMLELEMENT (NAME "Coll_ConvTMS", (select lad1.COLLAT_CONV_TMS
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),                
                    XMLELEMENT (NAME "Coll_CCCExpTMS", (select lad1.COLLAT_EXP_CCC_TMS
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),                 
                    XMLELEMENT (NAME "Coll_ExpTMS", (select lad1.COLLAT_EXP_CAR_TMS
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)),
                    XMLELEMENT (NAME "Coll_Credit_Prod", (select itgr.grp_nme from ft_t_itgr itgr where itgr.iss_typ_grp_oid in (select lad1.iss_typ_grp_oid
                    from KYTL_GC.ft_t_lad1 lad1, KYTL_GC.ft_t_laap laap, KYTL_GC.ft_t_lacd lacd
                    where lacd.lacd_oid = lad1.lacd_oid
					and laap.laan_oid = laan.laan_oid
					and laap.laap_oid = lacd.laap_oid
					and laap.data_stat_typ='ACTIVE'
					and lacd.data_stat_typ='ACTIVE'
                    and lad1.data_stat_typ='ACTIVE' and rownum = 1)))               
					))
					FROM KYTL_GC.ft_t_laan laan
					WHERE laan.leg_agrmnt_id = lagr.leg_agrmnt_id
					AND laan.org_id = lagr.org_id
					and laan.data_stat_typ='ACTIVE'           
					),  
            							XMLELEMENT (NAME "AgmtMarket",
            									XMLELEMENT (NAME "AgmtMarket",(select itgr.GRP_NME from ft_t_lar1 lar1, ft_t_itgr itgr, ft_t_lars lars            
            									   where itgr.iss_typ_grp_oid = lar1.iss_typ_grp_id
            									   and lars.lars_oid = lar1.lars_oid                   
            									   and lar1.data_stat_typ = 'ACTIVE'
            									   and itgr.data_stat_typ ='ACTIVE'  
            									   AND lars.data_stat_typ = 'ACTIVE'					   
            									   AND lars.org_id = lagr.org_id
            								       AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID
     					    					   and lars.rst_typ = 'MARKET' 
            									   and itgr.GRP_PURP_TYP =  'MARKET' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtSubMarket",(select itgr.GRP_NME from ft_t_lar1 lar1, ft_t_itgr itgr, ft_t_lars lars            
            									   where itgr.iss_typ_grp_oid = lar1.iss_typ_grp_id
            									   and lars.lars_oid = lar1.lars_oid                   
            									   and lar1.data_stat_typ = 'ACTIVE'
            									   and itgr.data_stat_typ ='ACTIVE'  
            									   AND lars.data_stat_typ = 'ACTIVE'					   
            									   AND lars.org_id = lagr.org_id
            								       AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID
     					    					   and lars.rst_typ = 'OPRTVE' 
            									   and itgr.GRP_PURP_TYP =  'OPRTVE' and rownum = 1)),
            									XMLELEMENT (NAME "AgmtSubMarketCve",(select itgr.iss_typ_grp_id from ft_t_lar1 lar1, ft_t_itgr itgr, ft_t_lars lars            
            									   where itgr.iss_typ_grp_oid = lar1.iss_typ_grp_id
            									   and lars.lars_oid = lar1.lars_oid                   
            									   and lar1.data_stat_typ = 'ACTIVE'
            									   and itgr.data_stat_typ ='ACTIVE'  
            									   AND lars.data_stat_typ = 'ACTIVE'					   
            									   AND lars.org_id = lagr.org_id
            								       AND lars.LEG_AGRMNT_ID=lagr.LEG_AGRMNT_ID
     					    					   and lars.rst_typ = 'OPRTVE' 
            									   and itgr.GRP_PURP_TYP =  'OPRTVE' and rownum = 1))
            							),
												XMLELEMENT (NAME "AgmtExeCntc",
            									XMLELEMENT (NAME "ExeFullName",(select cntc.CONTCT_FULL_NME
            									from ft_t_cntc cntc, ft_t_cnta cnta, ft_t_lac1 lac1
            									where cnta.contct_assign_purp_typ='EJECUTIVO' 
            									and cnta.data_stat_typ='ACTIVE' 
            									and cntc.data_stat_typ='ACTIVE'
            									and lac1.data_stat_typ='ACTIVE'
            									and cnta.contct_oid = cntc.contct_oid 
            									and lac1.cnta_oid = cnta.cnta_oid
            									and lagr.leg_agrmnt_id = lac1.leg_agrmnt_id
            									and lagr.org_id = lac1.org_id and rownum = 1)),
            									XMLELEMENT (NAME "ExeTelef", (select eadr.phone_num_id 
												from ft_t_eadr eadr 
												where elec_addr_id = 
												(select lac1.elec_addr_id 
												from ft_t_lac1 lac1 
												where lac1.data_stat_typ = 'ACTIVE' 
												and lac1.leg_agrmnt_id = lagr.leg_agrmnt_id 
												and lac1.org_id = lagr.org_id and lac1.elec_addr_id is not null) 
												and eadr.data_stat_typ = 'ACTIVE' and rownum = 1))
												),		    
												XMLELEMENT (NAME "AgmtContacts",
												(SELECT XMLAGG(XMLELEMENT (NAME "AgmtContact",
            				                    XMLELEMENT (NAME "ContactID", (SELECT cai1.alt_id 
												   FROM ft_t_cai1 cai1 
												   WHERE cntc.CONTCT_OID = CAI1.CONTCT_OID 
												   AND CAI1.ID_CTXT_TYP = 'CONTACTID'                                      
												   AND CAI1.data_stat_typ='ACTIVE' and rownum = 1)),
												XMLELEMENT (NAME "AgmtCntcFuncTyp", (decode(contct_assign_purp_typ,'CUENTA VALORES','BUC',(select edmv.ext_dmn_val_nme
                                                   from ft_t_edmv edmv
                                                   where edmv.ext_dmn_val_txt = 'FUNCION'  and rownum = 1 and edmv.intrnl_dmn_val_id = (select idmv.intrnl_dmn_val_id from ft_t_idmv idmv
                                                   where idmv.tbl_id = 'CNTA' and idmv.fld_id='01841556' and idmv.intrnl_dmn_val_txt = cnta.contct_assign_purp_typ and rownum = 1))))),
            									XMLELEMENT (NAME "AgmtCntcFunc", (cnta.contct_assign_purp_typ)),					  
            									XMLELEMENT (NAME "AgmtCntcPrior", (LAC1.CONTCT_PRIOR)),          
            				                    XMLELEMENT (NAME "AgmtCntcObv", (cntc.contct_desc)),
            									XMLELEMENT (NAME "AgmtCntcName", cntc.CONTCT_FULL_NME),
            									XMLELEMENT (NAME "AgmtCntcStatus", cntc.DATA_STAT_TYP),
            									XMLELEMENT (NAME "RelatedElements", 
            									(SELECT XMLAGG(XMLELEMENT (NAME "Details",
            				                    XMLELEMENT (NAME "Address", (select madr.ADDR_LN1_TXT 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
            				                    XMLELEMENT (NAME "ZipCode", (select madr.POSTAL_CDE 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
            									XMLELEMENT (NAME "City", (select madr.CITY_NME
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "CountyName",(select gu_nme from ft_t_gunt 
												where prnt_gu_typ = 'STATE'
												and TRIM(prnt_gu_id) = (select madr.CNTY_CDE 
												from ft_t_adtp adtp, ft_t_madr madr 
												where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
												and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
												and adtp.DATA_STAT_TYP = 'ACTIVE'
												and madr.DATA_STAT_TYP = 'ACTIVE'
												and rownum = 1))),
												XMLELEMENT (NAME "CountyCode",(select madr.CNTY_CDE 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),				
												XMLELEMENT (NAME "CountryCde",(select madr.CNTRY_CDE 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "CountryNme",(select madr.CNTRY_NME 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "NeighborhoodNme",(select madr.NEIGHBORHOOD_NME 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "IntNum",(select madr.ADDR_LN3_TXT 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "ExtNum",(select madr.ADDR_LN2_TXT 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),
												XMLELEMENT (NAME "TownshipNme",(select madr.TOWNSHIP_NME 
                                                from ft_t_adtp adtp, ft_t_madr madr 
                                                where ccrf.CNTL_CROSS_REF_OID = adtp.CNTL_CROSS_REF_OID 
                                                and adtp.MAIL_ADDR_ID = madr.MAIL_ADDR_ID
                                                and adtp.DATA_STAT_TYP = 'ACTIVE'
                                                and madr.DATA_STAT_TYP = 'ACTIVE'
                                                and rownum = 1)),			
												(SELECT XMLELEMENT (NAME  "Phones",
												XMLAGG(XMLELEMENT (NAME  "Phone", eadr.phone_num_id) ))
												FROM ft_t_adtp adtp, ft_t_eadr eadr
												WHERE adtp.elec_addr_id=eadr.elec_addr_id 
												AND adtp.cntl_cross_ref_oid=ccrf.CNTL_CROSS_REF_OID
												AND adtp.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.id_ctxt_typ='TELEPHON'),
												(SELECT XMLELEMENT (NAME  "Emails",
												XMLAGG(XMLELEMENT (NAME  "Email", eadr.E_MAIL_ADDR_TXT) ))
												FROM ft_t_adtp adtp, ft_t_eadr eadr
												WHERE adtp.elec_addr_id=eadr.elec_addr_id 
												AND adtp.cntl_cross_ref_oid=ccrf.CNTL_CROSS_REF_OID
												AND adtp.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.id_ctxt_typ='EMAIL'),
												(SELECT XMLELEMENT (NAME  "Faxes",
												XMLAGG(XMLELEMENT (NAME  "Fax", eadr.fax_num_id) ))
												FROM ft_t_adtp adtp, ft_t_eadr eadr
												WHERE adtp.elec_addr_id=eadr.elec_addr_id 
												AND adtp.cntl_cross_ref_oid=ccrf.CNTL_CROSS_REF_OID
												AND adtp.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.DATA_STAT_TYP = 'ACTIVE'
												AND eadr.id_ctxt_typ='FAX')		
            												))
            																from ft_t_ccrf ccrf  
            				                                                where cntc.CONTCT_OID = ccrf.CONTCT_OID)					  
            									  )))
            				                from KYTL_GC.ft_t_lac1 lac1, ft_t_cnta cnta, ft_t_cntc cntc
            				                where lagr.leg_agrmnt_id = lac1.leg_agrmnt_id 
            								and lagr.org_id = lac1.org_id 
            								and lac1.data_stat_typ = 'ACTIVE'
            								and cnta.CONTCT_OID = cntc.CONTCT_OID
            								and lac1.cnta_oid= cnta.cnta_oid
            								and cntc.data_stat_typ='ACTIVE'
            								and cnta.DATA_STAT_TYP = 'ACTIVE'			
            								and cnta.contct_assign_purp_typ not in (select intrnl_dmn_val_txt from ft_t_idmv idmv where idmv.tbl_id = 'CNTA' and idmv.col_nme = 'CONTCT_ASSIGN_PURP_TYP' and idmv.dmn_val_purp_typ = 'PART_ROL' and idmv.data_stat_typ ='ACTIVE')
            							)),
										XMLELEMENT (NAME "AgmtPlazas",
							                (SELECT XMLAGG(XMLELEMENT (NAME "AgmtPlaza",
							                    XMLELEMENT (NAME "AgmtPlaza", (select trim(gunt.gu_nme) from ft_t_gunt gunt 
												where gunt.prnt_gu_id = lap1.prnt_gu_id 
												and gunt.data_stat_typ='ACTIVE' and gunt.prnt_gu_typ = 'CITY' and rownum = 1)),
												  XMLELEMENT (NAME "AgmtPlazaCve",trim(lap1.prnt_gu_id)),
												 XMLELEMENT (NAME "AmgtPlazaSTARID", (select frid.finr_id 
													from ft_t_frid frid 
													where frid.finsrl_id_ctxt_typ='STARID' 
													and frid.data_stat_typ='ACTIVE' 
													and frid.inst_mnem = flar.inst_mnem and rownum = 1)) 
												  ))
												from ft_t_flar flar, ft_t_lap1 lap1
												where flar.data_stat_typ = 'ACTIVE'
												and lap1.data_stat_typ = 'ACTIVE' 
												and flar.flar_oid = lap1.flar_oid
												and flar.org_id = lagr.org_id
												and flar.leg_agrmnt_id = lagr.leg_agrmnt_id
												AND flar.rl_typ = 'EXTERNAL'					
										)), 
																				XMLELEMENT (NAME "AgmtProdLists", 
										            				            (SELECT XMLAGG(XMLELEMENT (NAME "AgmtProdList",
										            				            XMLELEMENT (NAME "AgmtProdListNme", ( select itgr.GRP_NME from ft_t_itgr itgr
								                                  				where itgr.iss_typ_grp_oid = lar1.iss_typ_grp_id
								                    							and itgr.data_stat_typ ='ACTIVE'
								                    							and itgr.grp_purp_typ = 'PROD_LST' and rownum = 1)),
												                                XMLELEMENT  (NAME "AgmtProdListTms", lar1.iss_typ_dte),
												                                XMLELEMENT  (NAME "AgmtProdListObv", lar1.iss_typ_obv)
								                              					))
								            									from ft_t_lar1 lar1, ft_t_lars lars
								            									where lars.lars_oid = lar1.lars_oid
								                              					and lars.leg_agrmnt_id = lagr.leg_agrmnt_id
								            									and lars.org_id = lagr.org_id
								                              					and lars.rst_typ = 'PROD_LST'
								            									and lar1.data_stat_typ = 'ACTIVE'
								            									and lars.data_stat_typ = 'ACTIVE')),  
                      							 								XMLELEMENT (NAME "AgmtParts", 
							            		                (SELECT XMLAGG(XMLELEMENT (NAME "AgmtPart",
							            		                      XMLELEMENT (NAME "AgmtPrtID", (cntc.contct_oid)),
												  					  XMLELEMENT (NAME "AgmtPrtNme", (cntc.contct_full_nme)), 	
																	 XMLELEMENT (NAME "AgmtPrtContactRel", (select cnta.contct_oid from ft_t_cnta cnta where rownum = 1 and cnta.cnta_oid =  
																		(select lac2.cnta_oid from ft_t_lac1 lac2 where rownum = 1 and lac2.lac1_oid  =  
																		(select cre1.prnt_lac1_oid from ft_t_cre1 cre1 where cre1.lac1_oid = lac1.lac1_oid and rownum = 1)))),		
							            							  XMLELEMENT (NAME "AgmtPrtRol", (CNTA.CONTCT_ASSIGN_PURP_TYP)),
							            		                      XMLELEMENT (NAME "AgmtPrtAdmInd", (LAC1.CONTCT_ADM_IND)),
							            		                      XMLELEMENT (NAME "AgmtPrtDomInd", (LAC1.CONTCT_DOM_IND)),
							            							  XMLELEMENT (NAME "AgmtPrtPodInd", (LAC1.CONTCT_POD_IND)),
							            							  XMLELEMENT (NAME "AgmtPrtPodDesc", (LAC1.CONTCT_POD_TXT)),
							            							  XMLELEMENT (NAME "AgmtPrtSigTyp", (LAC1.CONTCT_SIG_TYP)),					  
							            							  XMLELEMENT (NAME "AgmtPrtSigDesc", (LAC1.CONTCT_SIG_TXT)),
							            		                      XMLELEMENT (NAME "AgmtPrtSigDocTyp", (LAC1.CONTCT_SIG_DOC_TYP)),
							            		                      XMLELEMENT (NAME "AgmtPrtDoc", (LAC1.CONTCT_DOC)),					  
							            							  XMLELEMENT (NAME "AgmtPrtDocEndTms", (LAC1.CONTCT_DOC_END_TMS)),
							            		                      XMLELEMENT (NAME "AgmtPrtEscDesc", (LAC1.CONTCT_ESC_TXT))
							            							  ))
							            		                FROM KYTL_GC.ft_t_lac1 lac1, KYTL_GC.ft_t_cnta cnta, ft_t_cntc cntc
							            		                where lagr.leg_agrmnt_id = lac1.leg_agrmnt_id 
							            						and lagr.org_id = lac1.org_id 
							            						and lac1.data_stat_typ = 'ACTIVE'
							            						and cnta.cnta_oid= lac1.cnta_oid
							            						and cnta.data_stat_typ='ACTIVE'
							            						and cnta.contct_oid = cntc.contct_oid
							            						and cntc.data_stat_typ='ACTIVE'
							            						and cnta.contct_assign_purp_typ in (select intrnl_dmn_val_txt from ft_t_idmv idmv where idmv.intrnl_dmn_val_txt not in ('EJECUTIVO', 'TRADER') and idmv.tbl_id = 'CNTA' and idmv.col_nme = 'CONTCT_ASSIGN_PURP_TYP' and idmv.dmn_val_purp_typ = 'PART_ROL' and idmv.data_stat_typ ='ACTIVE')
							            					)),	
											XMLELEMENT (NAME "AgmtSub",
												XMLELEMENT (NAME "AgmtSubCstdyNum",(select aclp.lagr_leg_agrmnt_id 
												from ft_t_aclp aclp
												where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
												and aclp.lagr_org_id = lagr.org_id 
												and aclp.prt_purp_typ = 'INT_CUST' 
												and aclp.data_stat_typ='ACTIVE' and rownum = 1)),
												XMLELEMENT (NAME "AgmtSubBUCNme",(select acct.acct_nme 
												from ft_t_acct acct, ft_t_aclp aclp
												where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
												and aclp.lagr_org_id = lagr.org_id
												and acct.org_id = aclp.acct_org_id
												and aclp.prt_purp_typ = 'INT_CUST'
												and acct.bk_id = aclp.acct_bk_id
												and acct.acct_id = aclp.acct_id
												and aclp.data_stat_typ='ACTIVE'
												and acct.data_stat_typ='ACTIVE' and rownum = 1)),
												XMLELEMENT (NAME "AgmtSubBUCStartTms",(select acct.acct_create_dte 
												from ft_t_acct acct, ft_t_aclp aclp
												where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
												and aclp.lagr_org_id = lagr.org_id
												and aclp.prt_purp_typ = 'INT_CUST' 
												and acct.org_id = aclp.acct_org_id
												and acct.bk_id = aclp.acct_bk_id
												and acct.acct_id = aclp.acct_id
												and aclp.data_stat_typ='ACTIVE'
												and acct.data_stat_typ='ACTIVE' and rownum = 1)),
												XMLELEMENT (NAME "AgmtSubBUCEndTms",(select acct.acct_cls_dte 
												from ft_t_acct acct, ft_t_aclp aclp
												where aclp.lagr_leg_agrmnt_id = lagr.leg_agrmnt_id 
												and aclp.lagr_org_id = lagr.org_id 
												and aclp.prt_purp_typ = 'INT_CUST' 
												and acct.org_id = aclp.acct_org_id
												and acct.bk_id = aclp.acct_bk_id
												and acct.acct_id = aclp.acct_id
												and aclp.data_stat_typ='ACTIVE'
												and acct.data_stat_typ='ACTIVE' and rownum = 1))
											),							
											XMLELEMENT (NAME "ExternalIdentifiers",
							                (SELECT XMLAGG(XMLELEMENT (NAME "ExternalIdentifier",
							                      XMLELEMENT (NAME "ExternalID", laid.legal_agrmnt_id),
												  XMLELEMENT (NAME "Data_Src_ID", laid.data_src_id)
												  ))
												from ft_t_laid laid
												where laid.data_stat_typ = 'ACTIVE'
												and laid.org_id = lagr.org_id
												and laid.leg_agrmnt_id = lagr.leg_agrmnt_id
												and laid.data_src_id not in 'Generic' and laid.lagr_id_ctxt_typ not in ('PRODUCT32','Onboarding Digital')
											))
            				         )
            				     ).getClobVal() xmlResult
            				     FROM (select :paginacionResultado, lagr.org_id, lagr.exp_tms, lagr.last_chg_usr_id, lagr.agrmnt_cmnt_txt, lagr.leg_agrmnt_id, lagr.agrmnt_desc , lagr.agrmnt_typ, lagr.data_stat_typ, lagr.agrmnt_sign_dte, lagr.doc_eff_dte_tms, lagr.agrmnt_version_yr_typ, lagr.agrmnt_curr_cde, lagr.leg_agrmnt_doc_id, lagr.created_tms, lagr.last_chg_tms from Ft_T_Lagr Lagr where data_src_id != 'Sentry' and data_src_id != 'MENTOR' :paginacionFinal) lagr :paginacionInicio;
