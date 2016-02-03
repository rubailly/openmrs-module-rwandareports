/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.rwandareports.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAttribute;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

/**
 *
 */
public class SetupEligibleForViralLoadReport {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties
	private Program hivProgram;
	
	private Program pediHivProgram;
	
	private Program pmtctProgram;
	
	private Program pmtctCombinedMotherProgram;
	
	private ProgramWorkflowState hivArt;
	
	private ProgramWorkflowState pediArt;
	
	private ProgramWorkflowState pmtctArt;
	
	private List<Program> hivPrograms = new ArrayList<Program>();
	
	private List<ProgramWorkflowState> artWorkflowStates = new ArrayList<ProgramWorkflowState>();
	
	private List<ProgramWorkflowState> artWorkflowStatesIncPMTCT = new ArrayList<ProgramWorkflowState>();
	
	private List<String> onOrAfterOnOrBefore = new ArrayList<String>();
	
	private Concept viralLoadConcept;
	
	public void setup() throws Exception {
		
		setUpProperties();
		
		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "EligibleForViralLoad.xls",
		    "XlsEligibleForViralLoad", null);
		
		createDataSetDefinition(rd);
		
		Helper.saveReportDefinition(rd);
		
		Properties props = new Properties();
		props.put(
		    "repeatingSections",
		    "sheet:1,row:8,dataset:WithViralLoadRecorded|sheet:2,row:8,dataset:WithNoViralLoadRecorded|sheet:3,row:8,dataset:WithViralLoadOrder");
		props.put("sortWeight","5000");
		design.setProperties(props);
		Helper.saveReportDesign(design);
		
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("XlsEligibleForViralLoad".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("PIH-Eligible For Viral Load");
	}
	
	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("PIH-Eligible For Viral Load");
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
		    ParameterizableUtil.createParameterMappings("location=${location}"));
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		
		//====================================================================
		//           Patients Dataset definitions
		//====================================================================
		
		// Create patients with viral load recoded dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition1 = new RowPerPatientDataSetDefinition();
		dataSetDefinition1.setName("With viral load dataSetDefinition");
		dataSetDefinition1.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition1.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		// Create patients with no viral load recorded dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("With no viral load dataSetDefinition");
		dataSetDefinition2.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		//Create patients with viral load orders dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition3 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("With viral load orders dataSetDefinition");
		dataSetDefinition3.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition3.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		InProgramCohortDefinition inAnyHIVProgram = Cohorts.createInProgramParameterizableByDate(
		    "hivQD: In All HIV Programs", hivPrograms, "onDate");
		
		InStateCohortDefinition artOnOrBefore = Cohorts.createInCurrentState("hivQD: ever on ART", artWorkflowStates,
		    "onOrBefore");
		
		InStateCohortDefinition artCurrently = Cohorts.createInCurrentState("hivQD: currently on ART",
		    artWorkflowStatesIncPMTCT);
		
		SqlCohortDefinition patientsNotVoided = Cohorts.createPatientsNotVoided();
		
		NumericObsCohortDefinition viralLoad = Cohorts.createNumericObsCohortDefinition("obsQD: Viral Load recorded",
		    onOrAfterOnOrBefore, viralLoadConcept, 0, null, TimeModifier.LAST);
		
		InverseCohortDefinition noViralLoad = new InverseCohortDefinition(viralLoad);
		noViralLoad.setName("patientsWithNoViralLoadRecorded");
		
		SqlCohortDefinition withNoResults=new SqlCohortDefinition("select distinct ord.patient_id from orders ord left join obs o on ord.order_id = o.order_id where o.order_id is null and ord.concept_id="+viralLoadConcept.getConceptId()+" and ord.start_date<= :onOrBefore and ord.voided=0 and ord.discontinued=0;");
		withNoResults.addParameter(new Parameter("onOrBefore","onOrBefore",Date.class));
		
		//Base filter
		CompositionCohortDefinition eligibleForViralLoad = new CompositionCohortDefinition();
		eligibleForViralLoad.setName("hivQD: In all programs");
		eligibleForViralLoad.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleForViralLoad.getSearches().put("1", new Mapped(patientsNotVoided, new HashMap<String, Object>()));
		eligibleForViralLoad.getSearches().put("2",
		    new Mapped(inAnyHIVProgram, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		eligibleForViralLoad.getSearches().put("3",
		    new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		eligibleForViralLoad.getSearches().put("4",
		    new Mapped(artOnOrBefore, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate-1y}")));
		eligibleForViralLoad.setCompositionString("1 AND 2 AND 3 AND 4");
		
		dataSetDefinition1
		        .addFilter(eligibleForViralLoad, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition1.addFilter(viralLoad,
		    ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}"));
		
		dataSetDefinition2
		        .addFilter(eligibleForViralLoad, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition2.addFilter(noViralLoad,
		    ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}"));
		
		dataSetDefinition3
		        .addFilter(eligibleForViralLoad, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3.addFilter(noViralLoad,
		    ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}"));
		dataSetDefinition3
        .addFilter(withNoResults, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));
		
		//==================================================================
		//                 Columns of report settings
		//==================================================================
		
		MultiplePatientDataDefinitions imbType = RowPerPatientColumns.getIMBId("IMB ID");
		dataSetDefinition1.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(imbType, new HashMap<String, Object>());
		
		PatientProperty givenName = RowPerPatientColumns.getFirstNameColumn("familyName");
		dataSetDefinition1.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(givenName, new HashMap<String, Object>());
		
		PatientProperty familyName = RowPerPatientColumns.getFamilyNameColumn("givenName");
		dataSetDefinition1.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(familyName, new HashMap<String, Object>());
		
		PatientProperty age = RowPerPatientColumns.getAge("age");
		dataSetDefinition1.addColumn(age, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(age, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(age, new HashMap<String, Object>());
		
		MultiplePatientDataDefinitions group = RowPerPatientColumns.getTreatmentGroupOfAllHIVPatientIncludingCompleted("group", null);
		dataSetDefinition1.addColumn(group, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(group, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(group, new HashMap<String, Object>());
		
		PatientAttribute healthCenter = RowPerPatientColumns.getHealthCenter("healthcenter");
		dataSetDefinition1.addColumn(healthCenter, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(healthCenter, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(healthCenter, new HashMap<String, Object>());		

		MultiplePatientDataDefinitions tracNetId=RowPerPatientColumns.getTracnetId("TRACNET_ID");			
		dataSetDefinition1.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(tracNetId, new HashMap<String, Object>());
		
		
		MostRecentObservation viralLoadResults = RowPerPatientColumns.getMostRecentViralLoad("ViralLoad", "@ddMMMyy");
		dataSetDefinition1.addColumn(viralLoadResults, new HashMap<String, Object>());
		
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("location", "${location}");
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("WithViralLoadRecorded", dataSetDefinition1, mappings);
		reportDefinition.addDataSetDefinition("WithNoViralLoadRecorded", dataSetDefinition2, mappings);
		reportDefinition.addDataSetDefinition("WithViralLoadOrder", dataSetDefinition3, mappings);
	}
	
	private void setUpProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		pediHivProgram = gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		pmtctProgram = gp.getProgram(GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		pmtctCombinedMotherProgram = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_MOTHER_PROGRAM);
		
		pediArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
		    GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		hivArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
		    GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		pmtctArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
		    GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		
		hivPrograms.add(hivProgram);
		hivPrograms.add(pediHivProgram);
		hivPrograms.add(pmtctProgram);
		hivPrograms.add(pmtctCombinedMotherProgram);
		
		artWorkflowStates.add(pediArt);
		artWorkflowStates.add(hivArt);
		
		artWorkflowStatesIncPMTCT.add(pediArt);
		artWorkflowStatesIncPMTCT.add(hivArt);
		artWorkflowStatesIncPMTCT.add(pmtctArt);
		
		viralLoadConcept = gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST);
		
		onOrAfterOnOrBefore.add("onOrAfter");
		onOrAfterOnOrBefore.add("onOrBefore");
	}
}
