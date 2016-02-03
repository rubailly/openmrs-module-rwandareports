package org.openmrs.module.rwandareports.reporting;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfBirthShowingEstimation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAddress;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientRelationship;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RecentEncounterType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.StateOfPatient;
import org.openmrs.module.rwandareports.filter.AccompagnateurDisplayFilter;
import org.openmrs.module.rwandareports.filter.DrugDosageFrequencyFilter;
import org.openmrs.module.rwandareports.filter.GroupStateFilter;
import org.openmrs.module.rwandareports.filter.LastEncounterFilter;
import org.openmrs.module.rwandareports.filter.RemoveDecimalFilter;
import org.openmrs.module.rwandareports.filter.TreatmentStateFilter;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupPediatricLateVisitAndCD4Report {
	
	protected final static Log log = LogFactory.getLog(SetupAdultLateVisitAndCD4Report.class);
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//Properties retrieved from global variables
	private Program hivProgram;
	
	private ProgramWorkflow treatmentGroup;
	
	private ProgramWorkflow treatmentStatus;
	
	private ProgramWorkflowState onART;
	
	private ProgramWorkflowState following;
	
	private List<EncounterType> clinicalEnountersIncLab;
	
	private List<EncounterType> clinicalEncoutersExcLab;
	
	private EncounterType labTestEncounterType;
	
	private Concept viralLoad;
	
	private Concept cd4;
	
	private Concept cd4Percent;
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "PediatricLateVisitAndCD4Template.xls",
		    "XlsPediatricLateVisitAndCD4Template", null);
		
		ReportDefinition rdArtMedication = createReportDefinitionArtMedication();
		ReportDesign designArtMedication = Helper.createRowPerPatientXlsOverviewReportDesign(rdArtMedication, "pediatricHivArtReport.xls",
		    "pediatricHivArtReport", null);
		
		
		createDataSetDefinition(rd, rdArtMedication);
		
		Helper.saveReportDefinition(rd);
		Helper.saveReportDefinition(rdArtMedication);
		
		
		Properties props = new Properties();
		props.put(
		    "repeatingSections",
		    "sheet:1,row:8,dataset:PediatricARTLateVisit|sheet:2,row:8,dataset:PediatricPreARTLateVisit|sheet:3,row:8,dataset:PediatricHIVLateCD4Count|sheet:4,row:8,dataset:PediatricHIVLateCD4CountPreART|sheet:5,row:8,dataset:PediatricHIVLostToFollowup|sheet:6,row:8,dataset:CD4LessThan350|sheet:7,row:8,dataset:zeroToFiveYears|sheet:8,row:8,dataset:ViralLoadGreaterThan20InTheLast3Months");
		props.put("sortWeight","5000");
		design.setProperties(props);
		Helper.saveReportDesign(design);		
		
		
		
		
		Properties propsArtMed = new Properties();
		propsArtMed.put(
		    "repeatingSections",
		    "sheet:1,row:8,dataset:Regimen");
		propsArtMed.put("sortWeight","5000");
		designArtMedication.setProperties(propsArtMed);
		Helper.saveReportDesign(designArtMedication);
		
        
		
		
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("XlsPediatricLateVisitAndCD4Template".equals(rd.getName()) || "pediatricHivArtReport".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIV-Pedi Report-Monthly");
		Helper.purgeReportDefinition("HIV-Pedi ART Medication Report");
	}
	
	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("HIV-Pedi Report-Monthly");
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
		    ParameterizableUtil.createParameterMappings("location=${location}"));
		
		/*createDataSetDefinition(reportDefinition);
		
		h.saveReportDefinition(reportDefinition);
		*/
		return reportDefinition;
	}
	private ReportDefinition createReportDefinitionArtMedication() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("HIV-Pedi ART Medication Report");
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
		    ParameterizableUtil.createParameterMappings("location=${location}"));
		
		/*createDataSetDefinition(reportDefinition);
		
		h.saveReportDefinition(reportDefinition);*/
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition, ReportDefinition reportDefinitionArtMedi) {
		//====================================================================
		//           Patients Dataset definitions
		//====================================================================
		
		// Create ART late visit dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition1 = new RowPerPatientDataSetDefinition();
		dataSetDefinition1.setName("Pediatric ART dataSetDefinition");
		
		// Create Pre-ART late visit dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("Pediatric Pre-ART dataSetDefinition");
		
		//Create HIV late CD4 count dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition3 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("Pediatric ART late CD4 dataSetDefinition");
		
		RowPerPatientDataSetDefinition dataSetDefinition3_1 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3_1.setName("Pediatric Pre-ART late CD4 dataSetDefinition");
		
		//Create HIV lost to follow-up dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition4 = new RowPerPatientDataSetDefinition();
		dataSetDefinition4.setName("Pediatric HIV lost to follow-up dataSetDefinition");
		
		//Create ART Regimen dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition6 = new RowPerPatientDataSetDefinition();
		dataSetDefinition6.setName("Pediatric HIV ART Regimen dataSetDefinition");
		
		/*//Create PreART CD4% less than 25 dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition7 = new RowPerPatientDataSetDefinition();
		dataSetDefinition7.setName("Pediatric HIV PreART CD4% < 25% dataSetDefinition");
		
		//Create PreART CD4% less than 20 dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition8 = new RowPerPatientDataSetDefinition();
		dataSetDefinition8.setName("Pediatric HIV PreART CD4% < 20% dataSetDefinition");
		*/
		//Create PreART CD4 < 350 dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition9 = new RowPerPatientDataSetDefinition();
		dataSetDefinition9.setName("Pediatric HIV PreART CD4 < 500 dataSetDefinition");
		
		//Create 0-18 months and not on ART dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition10 = new RowPerPatientDataSetDefinition();
		dataSetDefinition10.setName("Pediatric HIV 0-18 months and not on ART dataSetDefinition");
		
		RowPerPatientDataSetDefinition dataSetDefinition11 = new RowPerPatientDataSetDefinition();
		dataSetDefinition11.setName("Pediatric ART with VL > 1000 dataSetDefinition");
		
		// HIV program Cohort definition
		InProgramCohortDefinition pediatricHivProgramCohort = Cohorts.createInProgramParameterizableByDate(
		    "pediatricHivProgramCohort", hivProgram);
		
		dataSetDefinition1.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition2.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition3.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition3_1.addFilter(pediatricHivProgramCohort,
			    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition4.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition6.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		
		/*dataSetDefinition7.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition8.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		*/dataSetDefinition9.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition10.addFilter(pediatricHivProgramCohort,
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		
		dataSetDefinition11.addFilter(pediatricHivProgramCohort,
			    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
			
		
		//==================================================================
		//                 1. Pediatric ART late visit
		//==================================================================		
		
		// ON ANTIRETROVIRALS state cohort definition.
		InStateCohortDefinition onARTStatusCohort = Cohorts.createInProgramStateParameterizableByDate("onARTStatusCohort",
		    onART);
		
		dataSetDefinition1.addFilter(onARTStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition3.addFilter(onARTStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition11.addFilter(onARTStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		
		SqlCohortDefinition patientsNotVoided = Cohorts.createPatientsNotVoided();
		dataSetDefinition1.addFilter(patientsNotVoided, new HashMap<String, Object>());
		dataSetDefinition2.addFilter(patientsNotVoided, new HashMap<String, Object>());
		dataSetDefinition3.addFilter(patientsNotVoided, new HashMap<String, Object>());
		dataSetDefinition3_1.addFilter(patientsNotVoided, new HashMap<String, Object>());
		dataSetDefinition4.addFilter(patientsNotVoided, new HashMap<String, Object>());
		dataSetDefinition6.addFilter(patientsNotVoided, new HashMap<String, Object>());
		
		//Patients with any Clinical Encounter(Lab Test included) in last year
		
		
		SqlCohortDefinition patientWithViralLoadAndCD4Tested=new SqlCohortDefinition("SELECT distinct e.patient_id FROM encounter e , obs o where o.encounter_id=e.encounter_id and e.encounter_type="+labTestEncounterType.getEncounterTypeId()+" and o.concept_id in ("+viralLoad.getConceptId()+","+cd4.getConceptId()+") and e.encounter_datetime>= :onOrAfter and e.voided=0 and o.voided=0 and value_numeric is not null;");
		patientWithViralLoadAndCD4Tested.addParameter(new Parameter("onOrAfter","onOrAfter",Date.class));
		
		
		EncounterCohortDefinition patientsWithClinicalEncountersWithoutLabTest = Cohorts.createEncounterParameterizedByDate(
		    "patientsWithClinicalEncounters", "onOrAfter", clinicalEncoutersExcLab);
		
		CompositionCohortDefinition patientsWithClinicalEncounters=new CompositionCohortDefinition();
		patientsWithClinicalEncounters.addParameter(new Parameter("onOrAfter","onOrAfter",Date.class));
		patientsWithClinicalEncounters.getSearches().put("1",new Mapped<CohortDefinition>(patientWithViralLoadAndCD4Tested, ParameterizableUtil.createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithClinicalEncounters.getSearches().put("2",new Mapped<CohortDefinition>(patientsWithClinicalEncountersWithoutLabTest, ParameterizableUtil.createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithClinicalEncounters.setCompositionString("1 OR 2");
		
		
		
		
		/*EncounterCohortDefinition patientsWithClinicalEncounters = Cohorts.createEncounterParameterizedByDate(
		    "patientsWithClinicalEncounters", "onOrAfter", clinicalEnountersIncLab);*/
		dataSetDefinition1.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition2.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition3.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition3_1.addFilter(patientsWithClinicalEncounters,
			    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition6.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		/*dataSetDefinition7.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition8.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		*/dataSetDefinition9.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition10.addFilter(patientsWithClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition11.addFilter(patientsWithClinicalEncounters,
			    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
			
		
		// Patients without Any clinical Encounter(Test lab excluded) in last three months.
		/*EncounterCohortDefinition patientsWithClinicalEncountersWithoutLabTest = Cohorts.createEncounterParameterizedByDate(
		    "patientsWithClinicalEncounters", "onOrAfter", clinicalEncoutersExcLab);*/
		
		CompositionCohortDefinition patientsWithoutClinicalEncounters = new CompositionCohortDefinition();
		patientsWithoutClinicalEncounters.setName("patientsWithoutClinicalEncounters");
		patientsWithoutClinicalEncounters.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithoutClinicalEncounters.getSearches().put(
		    "patientsWithClinicalEncountersWithoutLabTest",
		    new Mapped<CohortDefinition>(patientsWithClinicalEncountersWithoutLabTest, ParameterizableUtil
		            .createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithoutClinicalEncounters.setCompositionString("NOT patientsWithClinicalEncountersWithoutLabTest");
		
		dataSetDefinition1.addFilter(patientsWithoutClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6m}"));
		
		//==================================================================
		//                 2. Pre-ART late visit
		//==================================================================
		
		// Following state cohort definition.
		
		InStateCohortDefinition followingStatusCohort = Cohorts.createInProgramStateParameterizableByDate(
		    "followingStatusCohort", following);
		
		dataSetDefinition2
		        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		
		dataSetDefinition3_1
        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		/*dataSetDefinition7
        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition8
        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		*/dataSetDefinition9
        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition10
        .addFilter(followingStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		
		// Patients without Any clinical Encounter(Test lab excluded) in last six months.
		dataSetDefinition2.addFilter(patientsWithoutClinicalEncounters,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6m}"));
		
		//==================================================================
		//                 3. HIV late CD4 count
		//==================================================================
		
		NumericObsCohortDefinition cd4CohortDefinition = Cohorts.createNumericObsCohortDefinition("cd4CohortDefinition",
		    "onOrAfter", cd4, new Double(0), null, TimeModifier.LAST);
		
		CompositionCohortDefinition patientsWithouthCD4RecordComposition = new CompositionCohortDefinition();
		patientsWithouthCD4RecordComposition.setName("patientsWithouthCD4RecordComposition");
		patientsWithouthCD4RecordComposition.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithouthCD4RecordComposition.getSearches().put(
		    "cd4CohortDefinition",
		    new Mapped<CohortDefinition>(cd4CohortDefinition, ParameterizableUtil
		            .createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithouthCD4RecordComposition.setCompositionString("NOT cd4CohortDefinition");
		
		dataSetDefinition3.addFilter(patientsWithouthCD4RecordComposition,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		
		dataSetDefinition3_1.addFilter(patientsWithouthCD4RecordComposition,
			    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-9m}"));
			
		
		//==================================================================
		//                 4. HIV lost to follow-up
		//==================================================================
		
		//Patients with no encounters of any kind in the past year
		
		InverseCohortDefinition patientsWithoutEncountersInPastYear = new InverseCohortDefinition(
		        patientsWithClinicalEncounters);
		patientsWithoutEncountersInPastYear.setName("patientsWithoutEncountersInPastYear");
		
		dataSetDefinition4.addFilter(patientsWithoutEncountersInPastYear,
		    ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
	
		/*
		//==================================================================
		//                 7. PreART CD4% less than 25%
		//==================================================================
		AgeCohortDefinition lessThan3years = Cohorts.createUnder3AgeCohort("under3");
		dataSetDefinition7.addFilter(lessThan3years,
		    ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}"));
		
		NumericObsCohortDefinition lastCD4below25 = Cohorts.createNumericObsCohortDefinition("lastCD4below25", "onOrBefore", cd4Percent, 25,
		    RangeComparator.LESS_THAN, TimeModifier.LAST);
		dataSetDefinition7.addFilter(lastCD4below25, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));
		
		//==================================================================
		//                 8. PreART CD4% less than 20%
		//==================================================================
		AgeCohortDefinition less3To5years = Cohorts.create3to5AgeCohort("3to5");
		dataSetDefinition8.addFilter(less3To5years,
		    ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}"));
		
		NumericObsCohortDefinition lastCD4below20 = Cohorts.createNumericObsCohortDefinition("lastCD4below20", "onOrBefore", cd4Percent, 20,
		    RangeComparator.LESS_THAN, TimeModifier.LAST);
		dataSetDefinition8.addFilter(lastCD4below20, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));
		*/
		//==================================================================
		//                 9. PreART CD4% less than 25%
		//==================================================================
		AgeCohortDefinition over5years = Cohorts.createOver5AgeCohort("over5");
		dataSetDefinition9.addFilter(over5years,
		    ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}"));
		
		NumericObsCohortDefinition lastDC4below500 = Cohorts.createNumericObsCohortDefinition("lastDC4below500", "onOrBefore", cd4, 500.0,
		    RangeComparator.LESS_THAN, TimeModifier.LAST);
		dataSetDefinition9.addFilter(lastDC4below500, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));
		
		//==================================================================
		//                 10. 0-18 months not on ART
		//==================================================================
		AgeCohortDefinition zeroTo5 = Cohorts.createUnder5YearsAgeCohort("under5Years");
		dataSetDefinition10.addFilter(zeroTo5,
		    ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}"));
		

		//==================================================================
		//                11 . Patients with Viral Load >1000 in the last 12 months
		//==================================================================
		SqlCohortDefinition viralLoadGreaterThan1000InLast12Months = new SqlCohortDefinition("select vload.person_id from (select * from obs where concept_id="+viralLoad.getConceptId()+" and value_numeric>1000 and obs_datetime> :beforeDate and obs_datetime<= :onDate order by obs_datetime desc) as vload group by vload.person_id");
		viralLoadGreaterThan1000InLast12Months.setName("viralLoadGreaterThan1000InLast12Months");
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("beforeDate", "beforeDate", Date.class));
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("onDate", "onDate", Date.class));
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("location", "location", Location.class));
		dataSetDefinition11.addFilter(viralLoadGreaterThan1000InLast12Months,ParameterizableUtil.createParameterMappings("beforeDate=${endDate-12m},onDate=${endDate}"));
	
		
		//==================================================================
		//                 Columns of report settings
		//==================================================================
		MultiplePatientDataDefinitions imbType = RowPerPatientColumns.getIMBId("IMB ID");
		dataSetDefinition1.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(imbType, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(imbType, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(imbType, new HashMap<String, Object>());
		
		PatientProperty givenName = RowPerPatientColumns.getFirstNameColumn("First Name");
		dataSetDefinition1.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(givenName, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(givenName, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(givenName, new HashMap<String, Object>());
		
		PatientProperty familyName = RowPerPatientColumns.getFamilyNameColumn("Last Name");
		dataSetDefinition1.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(familyName, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(familyName, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(familyName, new HashMap<String, Object>());
		
		
		PatientProperty gender = RowPerPatientColumns.getGender("Sex");
		dataSetDefinition1.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(gender, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(gender, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(gender, new HashMap<String, Object>());
		
		DateOfBirthShowingEstimation birthdate = RowPerPatientColumns.getDateOfBirth("Date of Birth", null, null);
		dataSetDefinition1.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(birthdate, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(birthdate, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(birthdate, new HashMap<String, Object>());
		
		StateOfPatient txGroup = RowPerPatientColumns.getStateOfPatient("Group", hivProgram, treatmentGroup,
		    new GroupStateFilter());
		dataSetDefinition1.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(txGroup, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(txGroup, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(txGroup, new HashMap<String, Object>());
		
		StateOfPatient stOfPatient = RowPerPatientColumns.getStateOfPatient("Treatment", hivProgram, treatmentStatus,
		    new TreatmentStateFilter());
		dataSetDefinition3.addColumn(stOfPatient, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(stOfPatient, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(stOfPatient, new HashMap<String, Object>());
		
		RecentEncounterType lastEncounterType = RowPerPatientColumns.getRecentEncounterType("Last visit type",
		    clinicalEncoutersExcLab, new LastEncounterFilter());
		dataSetDefinition1.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(lastEncounterType, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(lastEncounterType, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(lastEncounterType, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(lastEncounterType, new HashMap<String, Object>());
		
		DateDiff lateVisitInMonth = RowPerPatientColumns.getDifferenceSinceLastEncounter(
		    "Late visit in months", clinicalEncoutersExcLab, DateDiffType.MONTHS);
		lateVisitInMonth.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition1.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition2.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3_1.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition4.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition6.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		/*dataSetDefinition7.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition8.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		*/dataSetDefinition9.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition10.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition11.addColumn(lateVisitInMonth, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		MostRecentObservation returnVisitDate = RowPerPatientColumns.getMostRecentReturnVisitDate(
		    "Date of missed appointment", null);
		dataSetDefinition1.addColumn(returnVisitDate, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(returnVisitDate, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(returnVisitDate, new HashMap<String, Object>());
		
		MostRecentObservation cd4Count = RowPerPatientColumns.getMostRecentCD4("Most recent CD4", null);
		dataSetDefinition1.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(cd4Count, new HashMap<String, Object>());
		
		MostRecentObservation cd4Percentage = RowPerPatientColumns.getMostRecentCD4Percentage("Most recent CD4Perc",
		    null);
		dataSetDefinition6.addColumn(cd4Percentage, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(cd4Percentage, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(cd4Percentage, new HashMap<String, Object>());
		*/dataSetDefinition10.addColumn(cd4Percentage, new HashMap<String, Object>());
		
		DateDiff lateCD4InMonths = RowPerPatientColumns.getDifferenceSinceLastObservation(
		    "Late CD4 in months", cd4, DateDiffType.MONTHS);
		lateCD4InMonths.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition1.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition2.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3_1.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition4.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition9.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition11.addColumn(lateCD4InMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		DateDiff lateCD4PercentInMonths = RowPerPatientColumns.getDifferenceSinceLastObservation(
		    "Late CD4Perc in months", cd4Percent, DateDiffType.MONTHS);
		lateCD4PercentInMonths.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition6.addColumn(lateCD4PercentInMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		/*dataSetDefinition7.addColumn(lateCD4PercentInMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition8.addColumn(lateCD4PercentInMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		*/dataSetDefinition10.addColumn(lateCD4PercentInMonths, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		PatientRelationship accompagnateur = RowPerPatientColumns.getAccompRelationship("Accompagnateur",new AccompagnateurDisplayFilter());
		dataSetDefinition1.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(accompagnateur, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(accompagnateur, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(accompagnateur, new HashMap<String, Object>());
	
		PatientAddress address1 = RowPerPatientColumns.getPatientAddress("Address", true, true, true, true);
		dataSetDefinition1.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(address1, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(address1, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(address1, new HashMap<String, Object>());
		
		
		MultiplePatientDataDefinitions tracNetId=RowPerPatientColumns.getTracnetId("TRACNET_ID");
		
		dataSetDefinition1.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition2.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition3_1.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition4.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition6.addColumn(tracNetId, new HashMap<String, Object>());
		/*dataSetDefinition7.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition8.addColumn(tracNetId, new HashMap<String, Object>());
		*/dataSetDefinition9.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition10.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(tracNetId, new HashMap<String, Object>());
		
		
		
		MostRecentObservation viralLoad = RowPerPatientColumns.getMostRecentViralLoad("Most recent viralLoad", null);
		dataSetDefinition1.addColumn(viralLoad, new HashMap<String, Object>());
		dataSetDefinition3.addColumn(viralLoad, new HashMap<String, Object>());
		dataSetDefinition11.addColumn(viralLoad, new HashMap<String, Object>());
		
		
		MostRecentObservation weight = RowPerPatientColumns.getMostRecentWeight("Weight", "dd-MMM-yyyy",
		    new RemoveDecimalFilter());
		dataSetDefinition6.addColumn(weight, new HashMap<String, Object>());
		
		dataSetDefinition6.addColumn(
		    RowPerPatientColumns.getCurrentARTOrders("Regimen", "dd-MMM-yyyy", new DrugDosageFrequencyFilter()),
		    new HashMap<String, Object>());
		
		dataSetDefinition1.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition2.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition3.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition3_1.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition4.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition6.addParameter(new Parameter("location", "Location", Location.class));
		/*dataSetDefinition7.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition8.addParameter(new Parameter("location", "Location", Location.class));
		*/dataSetDefinition9.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition10.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition11.addParameter(new Parameter("location", "Location", Location.class));
		
		dataSetDefinition1.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition3.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition3_1.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition4.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition6.addParameter(new Parameter("endDate", "End Date", Date.class));
		/*dataSetDefinition7.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition8.addParameter(new Parameter("endDate", "End Date", Date.class));
		*/dataSetDefinition9.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition10.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition11.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("location", "${location}");
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("PediatricARTLateVisit", dataSetDefinition1, mappings);
		reportDefinition.addDataSetDefinition("PediatricPreARTLateVisit", dataSetDefinition2, mappings);
		reportDefinition.addDataSetDefinition("PediatricHIVLateCD4Count", dataSetDefinition3, mappings);
		reportDefinition.addDataSetDefinition("PediatricHIVLateCD4CountPreART", dataSetDefinition3_1, mappings);
		reportDefinition.addDataSetDefinition("PediatricHIVLostToFollowup", dataSetDefinition4, mappings);
		//reportDefinition.addDataSetDefinition("Regimen", dataSetDefinition6, mappings);
		/*reportDefinition.addDataSetDefinition("CD4LessThan25", dataSetDefinition7, mappings);
		reportDefinition.addDataSetDefinition("cd4LessThan20", dataSetDefinition8, mappings);
		*/reportDefinition.addDataSetDefinition("CD4LessThan350", dataSetDefinition9, mappings);
		reportDefinition.addDataSetDefinition("zeroToFiveYears", dataSetDefinition10, mappings);
		reportDefinition.addDataSetDefinition("ViralLoadGreaterThan20InTheLast3Months", dataSetDefinition11, mappings);
		
		
		reportDefinitionArtMedi.addDataSetDefinition("Regimen", dataSetDefinition6, mappings);
	}
	
	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		onART = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
		    GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		clinicalEnountersIncLab = gp.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES);
		
		clinicalEncoutersExcLab = gp.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES_EXC_LAB_TEST);
		
		following = gp.getProgramWorkflowState(GlobalPropertiesManagement.FOLLOWING_STATE,
		    GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		treatmentGroup = gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
		    GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		treatmentStatus = gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
		    GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		cd4 = gp.getConcept(GlobalPropertiesManagement.CD4_TEST);
		
		cd4Percent = gp.getConcept(GlobalPropertiesManagement.CD4_PERCENTAGE_TEST);
		
		labTestEncounterType=gp.getEncounterType(GlobalPropertiesManagement.LAB_ENCOUNTER_TYPE);
		
		viralLoad = gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST);
	}
}
