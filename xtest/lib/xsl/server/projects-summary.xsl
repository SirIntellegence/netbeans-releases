<?xml version="1.0"?>
<!--
                 Sun Public License Notice
 
 The contents of this file are subject to the Sun Public License
 Version 1.0 (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is available at
 http://www.sun.com/
 
 The Original Code is NetBeans. The Initial Developer of the Original
 Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 Microsystems, Inc. All Rights Reserved.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:java="java">

<xsl:param name="truncated"/>
<xsl:param name="pesConfig" select="string('default')"/>
<xsl:param name="projectGroupDescription" select="string('current')"/>

<xsl:include href="../library.xsl"/>

<xsl:key name="group" match="ManagedReport" use="@testingGroup"/>
<xsl:key name="groupAndType" match="ManagedReport" use="concat(@testingGroup,@testedType)"/>
<xsl:key name="groupAndProject" match="ManagedReport" use="concat(@testingGroup,@project)"/>
<xsl:key name="groupAndTypeAndProject" match="ManagedReport" use="concat(@testingGroup,@testedType,@project)"/>
<xsl:key name="groupAndTypeAndProjectAndBuild" match="ManagedReport" use="concat(@testingGroup,@testedType,@project,@build)"/>

<!-- not used -->
<!-- 
<xsl:key name="platform" match="ManagedReport" use="concat(@osName,@osVersion,@osArch)"/> 
<xsl:key name="platformAndBuild" match="ManagedReport" use="concat(@osName,@osVersion,@osArch,@build)"/> 
<xsl:key name="host" match="ManagedReport" use="@host"/>
-->

<xsl:template match="/">
	<xsl:call-template name="html-page">
		<xsl:with-param name="html-title">XTest Overall Results</xsl:with-param>
	</xsl:call-template>
</xsl:template>


<xsl:template match="XTestWebReport">
	<xsl:call-template name="MakeProjectsTestsSummaryTable"/>
</xsl:template>

<xsl:template name="MakeProjectsTestsSummaryTable">
	
	<H1>XTest Overall Results:</H1>	
	<H3>group of <xsl:value-of select="$projectGroupDescription"/> results</H3>	
	<P>	
		<UL>
			<LI>This page was generated at: <xsl:value-of select="java:util.Date.new()"/></LI>
			
		</UL>
	</P>
	
	
	
	<!-- for each testing group -->
	<xsl:variable name="uniqueTestingGroup" select="//ManagedReport[generate-id(.)=generate-id(key('group',./@testingGroup)[1])]"/>
	
	<xsl:for-each select="$uniqueTestingGroup">
		<xsl:sort select="@testingGroup" order = "descending"/>
		<xsl:variable name="currentTestingGroup" select="@testingGroup"/>		
		<H2>Department: <xsl:value-of select="@testingGroup"/></H2>
		
		<TABLE width="90%" cellspacing="2" cellpadding="5" border="0" >	
			<xsl:variable name="uniqueTestedType" select="//ManagedReport[generate-id(.)=generate-id(key('groupAndType',concat($currentTestingGroup,./@testedType))[1])]"/>
			<TR align="center">
				<TD rowspan="3" bgcolor="#A6CAF0" width="30%"><B>Tested Projects</B></TD>
				<TD colspan="{count($uniqueTestedType)*4}" bgcolor="#A6CAF0">
					<B>Overall results for <xsl:value-of select="$currentTestingGroup"/></B>
				</TD>
			</TR>
			<TR align="center">
				<xsl:for-each select="$uniqueTestedType">
					<xsl:sort select="@testedType"/>
					<TD class="pass" colspan="4"><B>test type: <xsl:value-of select="@testedType"/></B></TD>					
				</xsl:for-each>
			</TR>
			<TR align="center">
				<xsl:for-each select="$uniqueTestedType">					
					<TD class="pass"><B>last build</B></TD>
					<TD class="pass"><B>pass</B></TD>
					<TD class="pass"><B>total</B></TD>
					<TD class="pass"><B>project report</B></TD>
				</xsl:for-each>
			</TR>
			
			<!-- now for each tested projects by this group -->
			<xsl:variable name="uniqueProject" select="//ManagedReport[generate-id(.)=generate-id(key('groupAndProject',concat($currentTestingGroup,./@project))[1])]"/>
			
			<xsl:for-each select="$uniqueProject">
				<xsl:sort select="@project"/>
				<xsl:variable name="currentProject" select="@project"/>
				
				<TR></TR>
				
				<TR align="center">
					<TD class="pass"><B><xsl:value-of select="@project"/></B></TD>
					
					<!-- now get information from the latest build and create the link -->
					<!---
					<xsl:variable name="uniqueTestedType" select="//ManagedReport[generate-id(.)=generate-id(key('groupAndType',concat($currentTestingGroup,@testedType))[1])]"/>
					-->
					<xsl:for-each select="$uniqueTestedType">
						<xsl:sort select="@testedType"/>
						<xsl:variable name="currentType" select="@testedType"/>	
						<xsl:variable name="builds" select="key('groupAndTypeAndProject',concat($currentTestingGroup,$currentType,$currentProject))"/>						
						<xsl:for-each select="$builds">							
							<xsl:sort select="@build"  order = "descending"/>
							<xsl:if test="position()=1">
							
								<xsl:variable name="lastBuild" select="@build"/>
								<TD class="pass">
									<xsl:value-of select="$lastBuild"/>
								</TD>
									<xsl:variable name="expression" select="key('groupAndTypeAndProjectAndBuild',concat($currentTestingGroup,$currentType,$currentProject,$lastBuild))"/>
								<TD class="pass">
									<xsl:value-of select="format-number(sum(($expression)/@testsPass) div sum(($expression)/@testsTotal),'0.00%')"/>
								</TD>
								<TD class="pass">
									<xsl:value-of select="sum($expression/@testsTotal)"/>
								</TD>
								<TD class="pass">
                                                                    <xsl:if test="not(boolean($truncated))">   
                                                                        <A HREF="{$currentProject}/{$currentTestingGroup}-{$currentType}.html">report</A>
                                                                    </xsl:if>    
                                                                    <xsl:if test="boolean($truncated)">   
                                                                        <A><xsl:attribute name="href"><xsl:value-of select="translate(concat($currentProject,'/',$currentTestingGroup,'-',$currentType,'.html'),' ','_')"/></xsl:attribute>report</A>
                                                                    </xsl:if>
                                                                </TD>
						        </xsl:if>								
							</xsl:for-each>
							<xsl:if test="count($builds)=0">								
								<TD colspan="4" class="pass">-</TD>
							</xsl:if>
					</xsl:for-each>

				</TR>

			</xsl:for-each>

			
		</TABLE>
		
	</xsl:for-each>
	<BR/>
	<HR width="90%"/>

	<xsl:if test="$pesConfig!=string('default')">
	
		<BR/>
		<H2>Results gtom other projects:</H2>    				
		<UL>
			<xsl:if test="$projectGroupDescription!=string('current')">
				<LI><A HREF="index.html">Currently tested projects</A></LI>
			</xsl:if>
    		<xsl:for-each select="document($pesConfig,/*)/PESConfig/PESProjects/PESProjectGroup">    		    		
    			<xsl:if test="$projectGroupDescription!=@description">
	    			<LI><A HREF="group-{@name}.html"><xsl:value-of select="@description"/></A></LI>
	    		</xsl:if>
			</xsl:for-each>
		</UL>
	</xsl:if>
	

</xsl:template>




</xsl:stylesheet>