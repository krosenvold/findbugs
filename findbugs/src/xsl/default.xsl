<?xml version="1.0" encoding="UTF-8"?>

<!--
  FindBugs - Find bugs in Java programs
  Copyright (C) 2004, University of Maryland
  
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.
  
  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  
  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
-->

<!--
  A simple XSLT stylesheet to transform FindBugs XML results
  annotated with messages into HTML.

  Authors:
  David Hovemeyer
-->

<xsl:stylesheet
	version="1.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output
	method="xml"
	omit-xml-declaration="yes"
	standalone="yes"
	doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
	doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
	indent="yes"
	encoding="UTF-8"/>

<xsl:variable name="bugTableHeader">
	<tr class="tableheader">
		<th align="left">Code</th>
		<th align="left">Warning</th>
	</tr>
</xsl:variable>

<xsl:template match="/">
	<html>
	<head>
		<title>FindBugs Report</title>
		<style type="text/css">
		.tablerow0 {
			background: #EEEEEE;
		}

		.tablerow1 {
			background: white;
		}

		.detailrow0 {
			background: #EEEEEE;
		}

		.detailrow1 {
			background: white;
		}

		.tableheader {
			background: #b9b9fe;
			font-size: larger;
		}

		.tablerow0:hover, .tablerow1:hover {
			background: #aaffaa;
		}
		</style>
		<script type="text/javascript">
			// <![CDATA[
			function toggleRow(elid) {
				if (document.getElementById) {
					element = document.getElementById(elid);
					if (element.style.display == 'none') {
						element.style.display = 'table-row';
						//window.status = 'Toggle on!';
					} else {
						element.style.display = 'none';
						//window.status = 'Toggle off!';
					}
				}
			}
			// ]]>
		</script>
	</head>
	<body>

	<h1>FindBugs Report</h1>

	<h2>Contents</h2>
	<ul>
		<li><a href="#Warnings_CORRECTNESS">Correctness Warnings</a></li>
		<li><a href="#Warnings_MT_CORRECTNESS">Multithreaded Correctness Warnings</a></li>
		<li><a href="#Warnings_MALICIOUS_CODE">Malicious Code Vulnerability Warnings</a></li>
		<li><a href="#Warnings_PERFORMANCE">Performance Warnings</a></li>
		<li><a href="#Details">Details</a></li>
	</ul>

	<h1>Warnings</h1>

	<p>Click on a warning row to see full context information.</p>

	<xsl:call-template name="generateWarningTable">
		<xsl:with-param name="warningSet" select="/BugCollection/BugInstance[@category='CORRECTNESS']"/>
		<xsl:with-param name="sectionTitle">Correctness Warnings</xsl:with-param>
		<xsl:with-param name="sectionId">Warnings_CORRECTNESS</xsl:with-param>
	</xsl:call-template>

	<xsl:call-template name="generateWarningTable">
		<xsl:with-param name="warningSet" select="/BugCollection/BugInstance[@category='MT_CORRECTNESS']"/>
		<xsl:with-param name="sectionTitle">Multithreaded Correctness Warnings</xsl:with-param>
		<xsl:with-param name="sectionId">Warnings_MT_CORRECTNESS</xsl:with-param>
	</xsl:call-template>

	<xsl:call-template name="generateWarningTable">
		<xsl:with-param name="warningSet" select="/BugCollection/BugInstance[@category='MALICIOUS_CODE']"/>
		<xsl:with-param name="sectionTitle">Malicious Code Vulnerability Warnings</xsl:with-param>
		<xsl:with-param name="sectionId">Warnings_MALICIOUS_CODE</xsl:with-param>
	</xsl:call-template>

	<xsl:call-template name="generateWarningTable">
		<xsl:with-param name="warningSet" select="/BugCollection/BugInstance[@category='PERFORMANCE']"/>
		<xsl:with-param name="sectionTitle">Performance Warnings</xsl:with-param>
		<xsl:with-param name="sectionId">Warnings_PERFORMANCE</xsl:with-param>
	</xsl:call-template>

	<h1><a name="Details">Details</a></h1>

	<xsl:apply-templates select="/BugCollection/BugPattern">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="ShortDescription"/>
	</xsl:apply-templates>

	</body>
	</html>
</xsl:template>

<xsl:template match="BugInstance">
	<xsl:variable name="warningId"><xsl:value-of select="generate-id()"/></xsl:variable>

	<tr class="tablerow{position() mod 2}" onclick="toggleRow('{$warningId}');">

	<td>
	<xsl:value-of select="@abbrev"/>
	</td>

	<td>
	<xsl:value-of select="substring-after(LongMessage,':')"/>
	</td>

	</tr>

	<!-- Add bug annotation elements: Class, Method, Field, SourceLine, Field -->
	<tr class="detailrow{position() mod 2}" id="{$warningId}" style="display: none;">
		<td/>
		<td colspan="2">
			<table>
				<tr><td><a href="#{@type}">Bug type <xsl:value-of select="@type"/> (click for details)</a></td></tr>
				<xsl:for-each select="./*/Message">
					<tr>
						<td><xsl:value-of select="text()"/></td>
					</tr>
				</xsl:for-each>
			</table>
		</td>
	</tr>
</xsl:template>

<xsl:template match="BugPattern">
	<h2><a name="{@type}"><xsl:value-of select="@type"/>: <xsl:value-of select="ShortDescription"/></a></h2>
	<xsl:value-of select="Details" disable-output-escaping="yes"/>
</xsl:template>

<xsl:template name="generateWarningTable">
	<xsl:param name="warningSet"/>
	<xsl:param name="sectionTitle"/>
	<xsl:param name="sectionId"/>

	<h2><a name="{$sectionId}"><xsl:value-of select="$sectionTitle"/></a></h2>
	<p>
		<table class="warningtable" width="100%">
			<xsl:copy-of select="$bugTableHeader"/>
			<xsl:apply-templates select="$warningSet">
				<xsl:sort select="@abbrev"/>
				<xsl:sort select="Class/@classname"/>
			</xsl:apply-templates>
		</table>
	</p>
</xsl:template>

</xsl:stylesheet>

<!-- vim:set ts=4: -->
