<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="string" omit-xml-declaration="yes" encoding="UTF-8"/>
		
			<xsl:variable name="delimiter" select="'|'" />
			<!--<xsl:strip-space elements ="RDR_Code_Operative 0074 RDR_Code_Operative Legal_Name Residence City_Town State Country_of_Residence_Code"/> -->
					
			<xsl:template match="/">	

			<xsl:for-each select="/GLOBALS/GLOBAL">
			
				<xsl:variable name="Legal_Name" select="normalize-space (Legal_Name)" /> 
			
			<xsl:for-each select="LOCALS/LOCAL">

				<xsl:variable name="Address" select= "normalize-space (FISCAL_ADDRESS/Address)" />	
                <xsl:variable name="Num_Ext" select="normalize-space (FISCAL_ADDRESS/Num_Ext)" />	
                <xsl:variable name="Num_Int" select="normalize-space (FISCAL_ADDRESS/Num_Int)" />					
				<xsl:variable name="Colony" select="normalize-space (FISCAL_ADDRESS/Colony)" />
                <xsl:variable name="Postal_Code" select="normalize-space (FISCAL_ADDRESS/Postal_Code)" />	
				
				<xsl:variable name="Residence" select="concat($Address,' ',$Num_Ext,' ',$Num_Int,' ',$Colony,' ',$Postal_Code)"/>
					
				<xsl:variable name="City_Town" select="normalize-space (FISCAL_ADDRESS/City_Town)" />
				
				<xsl:variable name="State" select="normalize-space (FISCAL_ADDRESS/State)" />
				
				<xsl:variable name="Country_of_Residence_Code" select="normalize-space (FISCAL_ADDRESS/Country_of_Residence_Code)" />
							
				
			<xsl:for-each select="OPERATIVES/OPERATIVE [BRANCHES/BRANCH/Branch='MEX']">
				
                <xsl:variable name="RDR_Code_Operative" select= "normalize-space (RDR_Code_Operative_Mnem)" />
					
 					
					<xsl:value-of select="$RDR_Code_Operative" disable-output-escaping="yes"/>					<!-- Insertamos el mismo valor que el Legal Name -->			 
					<xsl:value-of select="$delimiter"/>		           
                    <xsl:value-of select="'0074'" disable-output-escaping="yes"/>				
					<xsl:value-of select="$delimiter"/>				
					<xsl:value-of select="$RDR_Code_Operative" disable-output-escaping="yes"/>					
				    <xsl:value-of select="$delimiter"/>				
					<xsl:value-of select="$Legal_Name" disable-output-escaping="yes"/>					
					<xsl:value-of select="$delimiter"/>					
				    <xsl:value-of select="$Residence" disable-output-escaping="yes"/>					
				    <xsl:value-of select="$delimiter"/>					
					<xsl:value-of select="$City_Town" disable-output-escaping="yes"/>					
				    <xsl:value-of select="$delimiter"/>					
					<xsl:value-of select="$State" disable-output-escaping="yes"/>					
				    <xsl:value-of select="$delimiter"/>
					<xsl:value-of select="$Country_of_Residence_Code" disable-output-escaping="yes"/>
                    <xsl:text>&#10;</xsl:text>					
				
		    </xsl:for-each>
		    </xsl:for-each>
		    </xsl:for-each>
			
		</xsl:template>
</xsl:stylesheet>

