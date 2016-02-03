package org.openmrs.module.rwandareports.reporting;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupOncologyExternalBiopsyContactList {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;
	
	private Form pathResult;
	
	private Form pathSubmission;
	
	private Concept biopsyResult;
	
	private Concept telephone;
	
	private Concept telephone2;
	
	private Concept accession;
	
	private Concept primaryDoctorConstruct;
	
	private Concept biopsyCommunicated;
	
	private Concept primaryDoctorName;
	
	private Concept primaryDoctorTelephone; 
	
	private Concept primaryDoctorEmail; 
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "ExternalBiopsyResultsContactList.xls",
		    "ExternalBiopsyResultsContactList.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:7,dataset:dataset|sheet:2,row:7,dataset:dataset2");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Helper.saveReportDesign(design);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("ExternalBiopsyResultsContactList.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("ONC-External Biopsy Results/Tracking Contact List");
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-External Biopsy Results/Tracking Contact List");
				
		reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));	
		reportDefinition.setBaseCohortDefinition(new InverseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram)), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("PathBack");
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("PathNotBack");
		
		dataSetDefinition.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("biopsyResultSort Date", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		
		SortCriteria sortCriteria2 = new SortCriteria();
		sortCriteria2.addSortElement("accessionSort Date", SortDirection.ASC);
		dataSetDefinition2.setSortCriteria(sortCriteria2);
		
		//Add filters
		dataSetDefinition.addFilter(Cohorts.createPatientsWhereMostRecentEncounterIsForm(pathResult, biopsyCommunicated), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition2.addFilter(Cohorts.createPatientsOverdueForVisit(pathSubmission, pathResult), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		//Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("biopsyResult", biopsyResult, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("accession", accession, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("biopsyResultSort", biopsyResult, "yyyy/MM/dd"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("accessionSort", accession, "yyyy/MM/dd"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctorName", primaryDoctorConstruct, primaryDoctorName, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctorName", primaryDoctorConstruct, primaryDoctorName, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctorTelephone", primaryDoctorConstruct, primaryDoctorTelephone, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctorTelephone", primaryDoctorConstruct, primaryDoctorTelephone, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentObsgroup("primaryDoctorEmail", primaryDoctorConstruct, primaryDoctorEmail, "dd/MMM/yyyy"), new HashMap<String, Object>());
		
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("dataset", dataSetDefinition, mappings);
		reportDefinition.addDataSetDefinition("dataset2", dataSetDefinition2, mappings);
	}
	
	private void setupProperties() {
		
		oncologyProgram = gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		pathResult = gp.getForm(GlobalPropertiesManagement.PATH_RESULTS_FORM);
		
		biopsyResult = gp.getConcept(GlobalPropertiesManagement.BIOPSY_URL);
		
		accession = gp.getConcept(GlobalPropertiesManagement.PATHOLOGY_ACCESSION_NUMBER);
		
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		
		primaryDoctorConstruct = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_CONSTRUCT);
		
		primaryDoctorName = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_NAME);
		
		primaryDoctorTelephone = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_TELEPHONE);
		
		primaryDoctorEmail = gp.getConcept(GlobalPropertiesManagement.PRIMARY_DOCTOR_EMAIL);
		
		pathSubmission = gp.getForm(GlobalPropertiesManagement.PATH_SUBMISSION_FORM);
		
		biopsyCommunicated = gp.getConcept(GlobalPropertiesManagement.PATHOLOGY_RESULTS_COMMUNICATED);
	}
}
