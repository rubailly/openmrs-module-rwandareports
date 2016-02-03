package org.openmrs.module.rwandareports.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.dataset.ConsecutiveCombinedDataSetDefinition;
import org.openmrs.module.rwandareports.dataset.WeekViewDataSetDefinition;
import org.openmrs.module.rwandareports.definition.UpcomingChemotherapyCohortDefinition;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupOncologyOutpatientExpectedPatientList {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;

	private ProgramWorkflow diagnosis;
   
	private Concept scheduledVisit;
	
	private Concept biopsyResultVisit;
	
	private Concept specialVisit;
	
	private Concept telephone;
	
	private Concept telephone2;
	
	private Concept confirmedDiagnosis;

	
    private Form OncologyScheduleAppointmentForm;
    
    private Form outpatientClinicVisitsForm;
    
   
	private List<Concept> visitDates=new ArrayList<Concept>();
	
	private List<Form> visitForms=new ArrayList<Form>();
	
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "OncologyOutpatientExpectedPatientList.xls",
		    "OncologyOutpatientExpectedPatientList.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:4,dataset:dataset2|sheet:2,row:7,dataset:dataset");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Helper.saveReportDesign(design);
		
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("OncologyOutpatientExpectedPatientList.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
			
		}
		Helper.purgeReportDefinition("ONC-Oncology Expected Patient List - Outpatient Ward");
		
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Oncology Expected Patient List - Outpatient Ward");
					
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
	
		reportDefinition.addParameter(new Parameter("endDate", "Week of (select Monday)", Date.class));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}

private void createDataSetDefinition(ReportDefinition reportDefinition) {
	// Create new dataset definition 
	RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
	dataSetDefinition.setName("Chemotherapy Patient List");
	
	RowPerPatientDataSetDefinition baseSetDefinition = new RowPerPatientDataSetDefinition();
	baseSetDefinition.setName("Chemotherapy Base Patient List");
	
	dataSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,visitDates), ParameterizableUtil.createParameterMappings("end=${endDate+7d},start=${endDate}"));
	baseSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,visitDates), ParameterizableUtil.createParameterMappings("end=${endDate},start=${endDate}"));
	
	
	SortCriteria sortCriteria = new SortCriteria();
	sortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
	dataSetDefinition.setSortCriteria(sortCriteria);
	dataSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
	
	SortCriteria baseSortCriteria = new SortCriteria();
	baseSortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
	baseSetDefinition.setSortCriteria(baseSortCriteria);
	baseSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
	
	
	//Add Columns
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDateScheduledVisit", scheduledVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDateBiopsyResultVisit", biopsyResultVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDateSpecialVisit", specialVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
	
	addCommonColumns(dataSetDefinition, baseSetDefinition);
	
	ConsecutiveCombinedDataSetDefinition consecutiveDataSetDefinition = new ConsecutiveCombinedDataSetDefinition();
	consecutiveDataSetDefinition.setName("consecutiveDataSetDefinition");
	consecutiveDataSetDefinition.setBaseDefinition(dataSetDefinition);
	consecutiveDataSetDefinition.setNumberOfIterations(1);
	consecutiveDataSetDefinition.addParameter(new Parameter("startDate", "startDate", Date.class));
	
	Map<String, Object> mappings = new HashMap<String, Object>();
	mappings.put("startDate", "${endDate}");
	
	reportDefinition.addDataSetDefinition("dataset", consecutiveDataSetDefinition, mappings);
	
	WeekViewDataSetDefinition weekDataSetDefinition = new WeekViewDataSetDefinition();
	weekDataSetDefinition.setName("weekDataSetDefinition");
	weekDataSetDefinition.setBaseDefinition(baseSetDefinition);
	weekDataSetDefinition.addParameter(new Parameter("startDate", "startDate", Date.class));
	
	Map<String, Object> baseMappings = new HashMap<String, Object>();
	baseMappings.put("startDate", "${endDate}");
	
	reportDefinition.addDataSetDefinition("dataset2", weekDataSetDefinition, baseMappings);
	
}
	
	private void setupProperties() {
		
		oncologyProgram = gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		diagnosis = gp.getProgramWorkflow(GlobalPropertiesManagement.DIAGNOSIS_WORKFLOW, GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		
		OncologyScheduleAppointmentForm=gp.getForm(GlobalPropertiesManagement.ONCOLOGY_SCHEDULE_APPOINTMENT_FORM);
		
		outpatientClinicVisitsForm=gp.getForm(GlobalPropertiesManagement.OUTPATIENT_CLINIC_VISITS_FORM);		
		
		scheduledVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SCHEDULED_OUTPATIENT_VISIT);
		
		biopsyResultVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_PATHOLOGY_RESULT_VISIT);
		
		specialVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SPECIAL_VISIT);
		
		visitForms.add(OncologyScheduleAppointmentForm);
		visitForms.add(outpatientClinicVisitsForm);
		
		visitDates.add(scheduledVisit);
		visitDates.add(biopsyResultVisit);
		visitDates.add(specialVisit);
		
		confirmedDiagnosis=gp.getConcept(GlobalPropertiesManagement.CONFIRMED_DIAGNOSIS_CONCEPT);		
		
	}
	
	
private void addCommonColumns(RowPerPatientDataSetDefinition dataSetDefinition,RowPerPatientDataSetDefinition baseSetDefinition){
		
	//dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV",ChemotherapyInpatientWardVisit,"dd/MMM/yyyy",null), new HashMap<String, Object>());
	
	
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));
	baseSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getRegimenDateInformationParameterized("regimenDate", "dd/MMM/yyyy"), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("diagnosisNew", confirmedDiagnosis, null), new HashMap<String, Object>());
	
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
	    new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());
	
	
	
       }
	
}
