package org.openmrs.module.rwandareports.reporting;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupOncologyTestPatientList {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;
	
	private Form pathResult;
	
	private Form pathSubmission;
	
	private EncounterType outpatientOncology;
	
	private EncounterType inpatientOncology;
	
	private Concept biopsyResult;
	
	private Concept resultsVisit;
	
	private Concept telephone;
	
	private Concept telephone2;
	
	private Concept accession;
	
	private Concept biopsyScheduled;
	
	private Concept primaryDoctorConstruct;
	
	private Concept primaryDoctorDetails; 
	
	private Concept patientNotified;
	
	private Concept testOrdered;
	
	private Concept otherOncologyTest;
	
	private Concept tissueBiospsy;
	
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "BiopsyResultsContactList.xls",
		    "BiopsyResultsContactList.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:8,dataset:dataset|sheet:2,row:8,dataset:dataset2|sheet:3,row:8,dataset:dataset3|sheet:4,row:8,dataset:dataset4");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Helper.saveReportDesign(design);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("BiopsyResultsContactList.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("ONC-Biopsy Results/Tracking Contact List");
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Biopsy Results/Tracking Contact List");
				
		reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));	
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("PathBack");
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("PathNotBackButExpected");
		RowPerPatientDataSetDefinition dataSetDefinition3 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("PathNotBack");
		RowPerPatientDataSetDefinition dataSetDefinition4 = new RowPerPatientDataSetDefinition();
		dataSetDefinition4.setName("PathNotTake");
		
		dataSetDefinition.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition3.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition4.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("biopsyResultSort Date", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		
		SortCriteria sortCriteria2 = new SortCriteria();
		sortCriteria2.addSortElement("nextRDV", SortDirection.ASC);
		dataSetDefinition2.setSortCriteria(sortCriteria2);
		
		SortCriteria sortCriteria3 = new SortCriteria();
		sortCriteria3.addSortElement("accessionSort Date", SortDirection.ASC);
		dataSetDefinition3.setSortCriteria(sortCriteria3);
		
		SortCriteria sortCriteria4 = new SortCriteria();
		sortCriteria4.addSortElement("biopsyScheduledSort", SortDirection.ASC);
		dataSetDefinition4.setSortCriteria(sortCriteria4);
		
		//cohorts
		//Biopsy results back
		StringBuilder sql = new StringBuilder();
		sql.append("select lastResult.patient_id from (select * from (select * from encounter where voided = 0 and form_id=");
		sql.append(pathResult.getFormId());
		sql.append(" order by encounter_datetime desc) as o group by o.patient_id) as lastResult, (select * from (select * from encounter where encounter_type in(");
		sql.append(outpatientOncology.getEncounterTypeId());
		sql.append(",");
		sql.append(inpatientOncology.getEncounterTypeId());
		sql.append(")  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastResult.encounter_datetime)>=0 and (not last_Visit.encounter_datetime > lastResult.encounter_datetime) and last_Visit.patient_id=lastResult.patient_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.patient_id from encounter o where o.voided=0 and o.form_id=");
		sql2.append(pathResult.getFormId());
		sql2.append(" and o.patient_id not in(select patient_id from encounter where encounter_type in (");
		sql2.append(outpatientOncology.getEncounterTypeId());
		sql2.append(",");
		sql2.append(inpatientOncology.getEncounterTypeId());
		sql2.append(") and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql3 = new StringBuilder();
		sql3.append("select lastResult.patient_id from (select * from (select * from encounter where voided = 0 and form_id=");
		sql3.append(pathResult.getFormId());
		sql3.append(" order by encounter_datetime desc) as o group by o.patient_id) as lastResult, (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql3.append(patientNotified.getConceptId());
		sql3.append(" order by obs_datetime desc) as o group by o.person_id) as lastObs where ");
		sql3.append(" (lastObs.obs_datetime >= lastResult.encounter_datetime) and lastObs.person_id=lastResult.patient_id");
		
		SqlCohortDefinition lateVisit2 = new SqlCohortDefinition(sql3.toString());
		lateVisit2.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put("2",new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put("3",new Mapped<CohortDefinition>(lateVisit2, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("(1 OR 2) AND NOT 3");
		
		//Biopsy not taken
		StringBuilder sql4 = new StringBuilder();
		sql4.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and (concept_id=  ");
		sql4.append(otherOncologyTest.getConceptId());
		sql4.append(" or (concept_id=");
		sql4.append(testOrdered.getConceptId());
		sql4.append(" and value_coded=");
		sql4.append(tissueBiospsy.getConceptId());
		sql4.append("))");
		sql4.append(" order by obs_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id=");
		sql4.append(pathSubmission.getFormId());
		sql4.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql4.append(" DATEDIFF(:endDate,lastObs.obs_datetime)>6 and (not last_Visit.encounter_datetime > lastObs.obs_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit3 = new SqlCohortDefinition(sql4.toString());
		lateVisit3.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql5 = new StringBuilder();
		sql5.append("select o.person_id from obs o where o.voided=0 and (o.concept_id=");
		sql5.append(otherOncologyTest.getConceptId());
		sql5.append(" or (o.concept_id=");
		sql5.append(testOrdered.getConceptId());
		sql5.append(" and o.value_coded=");
		sql5.append(tissueBiospsy.getConceptId());
		sql5.append("))");
		sql5.append(" and DATEDIFF(:endDate,o.obs_datetime)>6 and o.person_id not in(select patient_id from encounter where form_id =");
		sql5.append(pathSubmission.getFormId());
		sql5.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter3 = new SqlCohortDefinition(sql5.toString());
		lateVisitNoEncounter3.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit2 = new CompositionCohortDefinition();
		visit2.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit2.getSearches().put("1",new Mapped<CohortDefinition>(lateVisit3, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit2.getSearches().put("2",new Mapped<CohortDefinition>(lateVisitNoEncounter3, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit2.setCompositionString("1 OR 2");

		//Add filters
		dataSetDefinition.addFilter(visit, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition2.addFilter(Cohorts.createPatientsDueForVisit(resultsVisit, pathResult), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3.addFilter(Cohorts.createPatientsOverdueForVisit(pathSubmission, pathResult), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition4.addFilter(visit2, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		//Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", resultsVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", resultsVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", resultsVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		 
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("biopsyResult", biopsyResult, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("accession", accession, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("biopsyResultSort", biopsyResult, "yyyy/MM/dd"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("accessionSort", accession, "yyyy/MM/dd"), new HashMap<String, Object>());
		
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("biopsyScheduledSort", biopsyScheduled, "yyyy/MM/dd"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("biopsyScheduled", biopsyScheduled, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctor", primaryDoctorConstruct, primaryDoctorDetails, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctor", primaryDoctorConstruct, primaryDoctorDetails, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctor", primaryDoctorConstruct, primaryDoctorDetails, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctor", primaryDoctorConstruct, primaryDoctorDetails, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("dataset", dataSetDefinition, mappings);
		reportDefinition.addDataSetDefinition("dataset2", dataSetDefinition2, mappings);
		reportDefinition.addDataSetDefinition("dataset3", dataSetDefinition3, mappings);
		reportDefinition.addDataSetDefinition("dataset4", dataSetDefinition4, mappings);
	}
	
	private void setupProperties() {
		
		oncologyProgram = gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		pathResult = gp.getForm(GlobalPropertiesManagement.PATH_RESULTS_FORM);
		
		outpatientOncology = gp.getEncounterType(GlobalPropertiesManagement.OUTPATIENT_ONCOLOGY_ENCOUNTER);
		
		inpatientOncology = gp.getEncounterType(GlobalPropertiesManagement.INPATIENT_ONCOLOGY_ENCOUNTER);
		
		resultsVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_PATHOLOGY_RESULT_VISIT);
		
		biopsyResult = gp.getConcept(GlobalPropertiesManagement.BIOPSY_URL);
		
		accession = gp.getConcept(GlobalPropertiesManagement.PATHOLOGY_ACCESSION_NUMBER);
		
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		
		primaryDoctorConstruct = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_CONSTRUCT);
		
		primaryDoctorDetails = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_NAME);
		
		biopsyScheduled = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SCHEDULED_TEST_VISIT);
		
		pathSubmission = gp.getForm(GlobalPropertiesManagement.PATH_SUBMISSION_FORM);
		
		patientNotified = gp.getConcept(GlobalPropertiesManagement.PATHOLOGY_RESULTS_COMMUNICATED);
		
		otherOncologyTest = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_TEST_CONSTRUCT);
		
		testOrdered = gp.getConcept(GlobalPropertiesManagement.LABORATORY_TESTS_ORDERED);
		
		tissueBiospsy = gp.getConcept(GlobalPropertiesManagement.TISSUE_BIOPSY);
	}
}
