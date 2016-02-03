package org.openmrs.module.rwandareports.reporting;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllObservationValues;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CurrentOrdersRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfBirth;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfNextTestDueFromBirth;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfObsAfterDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.EvaluateDefinitionForOtherPersonData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObsValueAfterDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObservationInMostRecentEncounterOfType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAgeInMonths;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RecentEncounterType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RetrievePersonByRelationshipAndByProgram;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RowPerPatientData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.StateOfPatient;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rwandareports.customcalculator.CombinedHFCSPAlerts;
import org.openmrs.module.rwandareports.definition.EvaluateMotherDefinition;
import org.openmrs.module.rwandareports.filter.BorFStateFilter;
import org.openmrs.module.rwandareports.filter.GroupStateFilter;
import org.openmrs.module.rwandareports.filter.LastEncounterFilter;
import org.openmrs.module.rwandareports.filter.LastThreeObsFilter;
import org.openmrs.module.rwandareports.filter.LastTwoObsFilter;
import org.openmrs.module.rwandareports.filter.ObservationFilter;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupCombinedHFCSPConsultationReport {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//Properties retrieved from global variables
	private Program pmtctCombined;
	
	private Program pmtctCCMother;

	private ProgramWorkflow feedingState;
	
	private ProgramWorkflow treatmentGroup;
	
	private Concept seroConcept;
	
	private Concept dbsConcept;
	
	private Concept childSerologyConcept;
	
	private List<EncounterType> clinicalEnountersIncLab;
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "PMTCTCombinedClinicConsultationSheet.xls",
		    "PMTCTCombinedClinicConsultationSheet.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:4,dataset:dataSet");
		props.put("sortWeight","5000");
		design.setProperties(props);
		Helper.saveReportDesign(design);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("PMTCTCombinedClinicConsultationSheet.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIV-PMTCT Combined Clinic Consultation sheet");
	}
	
	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("HIV-PMTCT Combined Clinic Consultation sheet");
		
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		
		
		Properties stateProperties = new Properties();
		stateProperties.setProperty("Program", pmtctCombined.getName());
		stateProperties.setProperty("Workflow", Context.getAdministrationService().getGlobalProperty(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW));
		reportDefinition.addParameter(new Parameter("state", "Group", ProgramWorkflowState.class, stateProperties));
		
		
		
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
		    ParameterizableUtil.createParameterMappings("location=${location}"));
		
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		
		
		dataSetDefinition.addParameter(new Parameter("state", "State", ProgramWorkflowState.class));
		
		dataSetDefinition.addFilter(Cohorts.createInCurrentStateParameterized("ccInfant group", "states"),
		    ParameterizableUtil.createParameterMappings("states=${state},onDate=${now}"));
		
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		
		
		InProgramCohortDefinition inPMTCTProgram = Cohorts.createInProgramParameterizableByDate(
		    "pmtct: Combined Clinic In Program", pmtctCombined);
		dataSetDefinition.addFilter(inPMTCTProgram, ParameterizableUtil.createParameterMappings("onDate=${now}"));
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("InfantId"), new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		
		StateOfPatient txGroup = RowPerPatientColumns.getStateOfPatient("infantGroup", pmtctCombined, treatmentGroup,
			    new GroupStateFilter());
		dataSetDefinition.addColumn(txGroup, new HashMap<String, Object>());
		
	
		RetrievePersonByRelationshipAndByProgram mother = RowPerPatientColumns.getMother(pmtctCCMother);
	
		PatientProperty firstName=RowPerPatientColumns.getFirstNameColumn("motherGivName");
		PatientProperty givenName=RowPerPatientColumns.getFamilyNameColumn("motherFamName");
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDefinitionForOtherPerson("motherFamilyName", mother, firstName), 
			new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDefinitionForOtherPerson("motherGivenName", mother,givenName), 
				new HashMap<String, Object>());
		
		EvaluateDefinitionForOtherPersonData motherId = RowPerPatientColumns.getDefinitionForOtherPerson("MotherId", mother,
		    RowPerPatientColumns.getIMBId("InfantId"));
		dataSetDefinition.addColumn(motherId, new HashMap<String, Object>());
		
		EvaluateDefinitionForOtherPersonData motherTracNetId = RowPerPatientColumns.getDefinitionForOtherPerson("motherTracNetId", mother,
			    RowPerPatientColumns.getTracnetId("TRACNET_ID"));
			dataSetDefinition.addColumn(motherTracNetId, new HashMap<String, Object>());
			
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfBirth("birthdate", "ddMMMyyyy", "ddMMMyyyy"),
		    new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getAgeInMonths("ageInMonths"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(
		    RowPerPatientColumns.getStateOfPatient("FeedingGroup", pmtctCombined, feedingState, new BorFStateFilter()),
		    new HashMap<String, Object>());
		
		DateOfNextTestDueFromBirth firstDbs = new DateOfNextTestDueFromBirth();
		firstDbs.setTimeUnit(Calendar.WEEK_OF_YEAR);
		firstDbs.setTimeIncrement(6);
		firstDbs.setName("firstDBSDue");
		firstDbs.setDateFormat("ddMMMyy");
		dataSetDefinition.addColumn(firstDbs, new HashMap<String, Object>());
		
		DateOfBirth dob = new DateOfBirth();
		dataSetDefinition.addColumn( RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("firstDBSTest", dbsConcept, childSerologyConcept, dob, "ddMMMyy"),
		    new HashMap<String, Object>());
		
		DateOfObsAfterDateOfOtherDefinition firstDbsDate = RowPerPatientColumns.getDateOfObsAfterDateOfOtherDefinition(
		    "firstDBSDate", dbsConcept, childSerologyConcept, dob);
		
		dataSetDefinition.addColumn(
		    RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("confDBSTest", dbsConcept, childSerologyConcept, firstDbsDate, "ddMMMyy"),
		    new HashMap<String, Object>());
		
		DateOfNextTestDueFromBirth firstSero = new DateOfNextTestDueFromBirth();
		firstSero.setTimeUnit(Calendar.MONTH);
		firstSero.setTimeIncrement(9);
		firstSero.setName("firstSeroDue");
		firstSero.setDateFormat("ddMMMyy");
		dataSetDefinition.addColumn(firstSero, new HashMap<String, Object>());
		
		DateOfNextTestDueFromBirth secondSero = new DateOfNextTestDueFromBirth();
		secondSero.setTimeUnit(Calendar.MONTH);
		secondSero.setTimeIncrement(18);
		secondSero.setName("secondSeroDue");
		secondSero.setDateFormat("ddMMMyy");
		dataSetDefinition.addColumn(secondSero, new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(
		    RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("firstSeroTest", seroConcept, childSerologyConcept, dob, "ddMMMyy"),
		    new HashMap<String, Object>());
		
		DateOfObsAfterDateOfOtherDefinition firstSeroDate = RowPerPatientColumns.getDateOfObsAfterDateOfOtherDefinition(
		    "firstSeroDate", seroConcept, childSerologyConcept, dob);
		new DateOfObsAfterDateOfOtherDefinition();
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("secondSeroTest",
		    seroConcept, childSerologyConcept, firstSeroDate, "ddMMMyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentReturnVisitDate("nextRDV", "yyyy/MM/dd"),
		    new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentReturnVisitDate("nextVisit", "dd-MMM-yyyy"),
		    new HashMap<String, Object>());
		
		EvaluateMotherDefinition allCd4 = RowPerPatientColumns.getDefinitionForOtherPersonObs("allCd4", mother,
			    RowPerPatientColumns.getAllMotherCD4Values("cd4all", null, new LastTwoObsFilter(), null));	
		dataSetDefinition.addColumn(allCd4, new HashMap<String, Object>());
		
		EvaluateMotherDefinition allWeight = RowPerPatientColumns.getDefinitionForOtherPersonObs("allWeight", mother,
			    RowPerPatientColumns.getAllMotherWeightValues("weightvalues", null, new LastTwoObsFilter(), null));	
		dataSetDefinition.addColumn(allCd4, new HashMap<String, Object>());
		
		AllObservationValues weight = RowPerPatientColumns.getAllWeightValues("weightObs", "ddMMMyy",
			    new LastThreeObsFilter(), new ObservationFilter());
		
		MostRecentObservation viralLoad = RowPerPatientColumns.getMostRecentViralLoad("Most recent viralLoad", "@ddMMMyy");
		dataSetDefinition.addColumn(viralLoad, new HashMap<String, Object>());
		
		EvaluateMotherDefinition viralLoadTest = RowPerPatientColumns.getDefinitionForOtherPersonObs("viralLoadTest", mother,
			    RowPerPatientColumns.getMostRecentViralLoad("vltest",null));
		dataSetDefinition.addColumn(viralLoadTest, new HashMap<String, Object>());
		
		EvaluateMotherDefinition cd4Test = RowPerPatientColumns.getDefinitionForOtherPersonObs("motherCD4", mother,
			    RowPerPatientColumns.getMostRecentCD4("cd4test",null));
		dataSetDefinition.addColumn(cd4Test, new HashMap<String, Object>());
		
		EvaluateMotherDefinition recentWeight = RowPerPatientColumns.getDefinitionForOtherPersonObs("recentWeight", mother,
			    RowPerPatientColumns.getMostRecentWeight("weight",null));
		dataSetDefinition.addColumn(recentWeight, new HashMap<String, Object>());
		
		EvaluateMotherDefinition recentHeight = RowPerPatientColumns.getDefinitionForOtherPersonObs("recentHeight", mother,
			    RowPerPatientColumns.getMostRecentHeight("height",null));
		dataSetDefinition.addColumn(recentHeight, new HashMap<String, Object>());
			
	   EvaluateMotherDefinition oi = RowPerPatientColumns.getDefinitionForOtherPersonObs("OI", mother,
				    RowPerPatientColumns.getMostRecentIO("ioinfection", null));
	  	dataSetDefinition.addColumn(oi, new HashMap<String, Object>());
	  	
	  	EvaluateMotherDefinition sideEffect = RowPerPatientColumns.getDefinitionForOtherPersonObs("SideEffects", mother,
			    RowPerPatientColumns.getMostRecenSideEffect("sideffect", null));
  	    dataSetDefinition.addColumn(sideEffect, new HashMap<String, Object>());
		
		
		PatientAgeInMonths ageinMonths=RowPerPatientColumns.getAgeInMonths("ageinMonths");
		ObsValueAfterDateOfOtherDefinition dbsRecorded=RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("dbsRecorded", dbsConcept, childSerologyConcept, dob, "ddMMMyy");
		ObsValueAfterDateOfOtherDefinition serotestRecorded=RowPerPatientColumns.getObsValueAfterDateOfOtherDefinition("serotestRecorded", seroConcept, childSerologyConcept, dob, "ddMMMyy");
		RecentEncounterType lastEncInMonth = RowPerPatientColumns.getRecentEncounterType("lastEncInMonth",clinicalEnountersIncLab,null, null);
		
		CustomCalculationBasedOnMultiplePatientDataDefinitions alert = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		alert.setName("alert");
		alert.addPatientDataToBeEvaluated(cd4Test, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(allCd4, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(allWeight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(recentWeight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(recentHeight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(oi, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(sideEffect, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(viralLoadTest, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(ageinMonths, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(dbsRecorded,new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(serotestRecorded, new HashMap<String, Object>());
	    alert.addPatientDataToBeEvaluated(lastEncInMonth,new HashMap <String,Object>());
		alert.setCalculator(new CombinedHFCSPAlerts());
		alert.addParameter(new Parameter("state", "State",Date.class));
		dataSetDefinition.addColumn(alert,ParameterizableUtil.createParameterMappings("state=${state}"));
		
		CurrentOrdersRestrictedByConceptSet artDrugs = RowPerPatientColumns.getCurrentARTOrders("Regimen", "dd-MMM-yy", null);
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDefinitionForOtherPerson("motherART", mother, artDrugs),
		    new HashMap<String, Object>());
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("state", "${state}");
		
		reportDefinition.addDataSetDefinition("dataSet", dataSetDefinition, mappings);
		
	}
	
	private void setupProperties() {
		pmtctCombined = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM);
		
		pmtctCCMother=gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_MOTHER_PROGRAM);
		
		feedingState = gp.getProgramWorkflow(GlobalPropertiesManagement.FEEDING_GROUP_WORKFLOW,
		    GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM);
		
		treatmentGroup = gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
			    GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM);
		
		seroConcept = gp.getConcept(GlobalPropertiesManagement.SERO_TEST);
		
		dbsConcept = gp.getConcept(GlobalPropertiesManagement.DBS_CONCEPT);
		
		childSerologyConcept = gp.getConcept(GlobalPropertiesManagement.CHILD_SEROLOGY_CONSTRUCT);		

		clinicalEnountersIncLab = gp.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES);
		
	}
}
