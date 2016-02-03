package org.openmrs.module.rwandareports.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rwandareports.dataset.ConsecutiveCombinedDataSetDefinition;
import org.openmrs.module.rwandareports.dataset.WeekViewDataSetDefinition;
import org.openmrs.module.rwandareports.definition.UpcomingChemotherapyCohortDefinition;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupChemotherapyExpectedPatientList {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;

	private ProgramWorkflow diagnosis;
	
	private Concept telephone;
	
	private Concept telephone2;
	
	private Concept confirmedDiagnosis;
	
    private Form OncologyScheduleAppointmentForm;
    
    private Concept ChemotherapyInpatientWardVisit;
    
    private Concept ChemotherapyInfusionCenterVisit;
	
	private List<Form> visitForms=new ArrayList<Form>();
	
	//private Concept weight;
	//private Concept height;
	
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		ReportDefinition rdInfusion = createInfusionReportDefinition();
		
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "ChemotherapyExpectedPatientList.xls",
		    "ChemotherapyPatientList.xls_", null);
		
		ReportDesign designInfusion = Helper.createRowPerPatientXlsOverviewReportDesign(rdInfusion, "ChemotherapyExpectedPatientListInfusion.xls",
			    "ChemotherapyPatientListInfusion.xls_", null);
			
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:4,dataset:dataset2|sheet:2,row:7,dataset:dataset");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Properties propsInfusion = new Properties();
		propsInfusion.put("repeatingSections", "sheet:1,row:4,dataset:dataset2|sheet:2,row:7,dataset:dataset");
		propsInfusion.put("sortWeight","5000");
		designInfusion.setProperties(propsInfusion);
		
		Helper.saveReportDesign(design);
		
		Helper.saveReportDesign(designInfusion);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("ChemotherapyPatientList.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
			if ("ChemotherapyPatientListInfusion.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("ONC-Oncology Expected Patient List - Inpatient Ward");
		Helper.purgeReportDefinition("ONC-Oncology Expected Patient List - Infusion Center");
		
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Oncology Expected Patient List - Inpatient Ward");
					
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
	
		//reportDefinition.addParameter(new Parameter("endDate", "Week of (select Monday)", Date.class));
        reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));

        createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
private ReportDefinition createInfusionReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Oncology Expected Patient List - Infusion Center");
					
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
	
		reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));
		createInfusionDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}

private void createDataSetDefinition(ReportDefinition reportDefinition) {
	// Create new dataset definition 
	RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
	dataSetDefinition.setName("Chemotherapy Patient List");
	
	RowPerPatientDataSetDefinition baseSetDefinition = new RowPerPatientDataSetDefinition();
	baseSetDefinition.setName("Chemotherapy Base Patient List");
	
	//dataSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInpatientWardVisit), ParameterizableUtil.createParameterMappings("end=${endDate+7d},start=${endDate}"));
    dataSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInpatientWardVisit), ParameterizableUtil.createParameterMappings("end=${endDate},start=${endDate}"));
    baseSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInpatientWardVisit), ParameterizableUtil.createParameterMappings("end=${endDate},start=${endDate}"));
	
	SortCriteria sortCriteria = new SortCriteria();
	sortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
	dataSetDefinition.setSortCriteria(sortCriteria);
	dataSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
	
	SortCriteria baseSortCriteria = new SortCriteria();
	baseSortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
	baseSetDefinition.setSortCriteria(baseSortCriteria);
	baseSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
	
	
	//Add Columns
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDate", ChemotherapyInpatientWardVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
	
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
	
	private void createInfusionDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("Chemotherapy Patient List");
		
		RowPerPatientDataSetDefinition baseSetDefinition = new RowPerPatientDataSetDefinition();
		baseSetDefinition.setName("Chemotherapy Base Patient List");
		
		
		//dataSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInfusionCenterVisit), ParameterizableUtil.createParameterMappings("end=${endDate+7d},start=${endDate}"));
        dataSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInfusionCenterVisit), ParameterizableUtil.createParameterMappings("end=${endDate},start=${endDate}"));
        baseSetDefinition.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(visitForms,ChemotherapyInfusionCenterVisit), ParameterizableUtil.createParameterMappings("end=${endDate},start=${endDate}"));
		
		
		
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		dataSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
		
		SortCriteria baseSortCriteria = new SortCriteria();
		baseSortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
		baseSetDefinition.setSortCriteria(baseSortCriteria);
		baseSetDefinition.addParameter(new Parameter("endDate", "Monday", Date.class));
		
		
		//Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDate", ChemotherapyInpatientWardVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
		
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
		    
		ChemotherapyInpatientWardVisit=gp.getConcept(GlobalPropertiesManagement.CHEMOTHERAPY_INPATIENT_WARD_VISIT_DATE);
		
		ChemotherapyInfusionCenterVisit=gp.getConcept(GlobalPropertiesManagement.CHEMOTHERAPY_INFUSION_CENTER_VISIT_DATE);
		
		visitForms.add(OncologyScheduleAppointmentForm);
		
		confirmedDiagnosis=gp.getConcept(GlobalPropertiesManagement.CONFIRMED_DIAGNOSIS_CONCEPT);
		/*height=gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT);
		weight=gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);*/
		
	}
	
	
private void addCommonColumns(RowPerPatientDataSetDefinition dataSetDefinition,RowPerPatientDataSetDefinition baseSetDefinition){
		
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV",ChemotherapyInpatientWardVisit,"dd/MMM/yyyy",null), new HashMap<String, Object>());
	
	
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingid"), new HashMap<String, Object>());
	baseSetDefinition.addColumn(RowPerPatientColumns.getArchivingId("archivingid"), new HashMap<String, Object>());
	
	
	//dataSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));
	//baseSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));

    dataSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate}"));
    baseSetDefinition.addColumn(RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate}"));


    //dataSetDefinition.addColumn(RowPerPatientColumns.getRegimenDateInformationParameterized("regimenDate", "dd/MMM/yyyy"), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate+7d}"));
    dataSetDefinition.addColumn(RowPerPatientColumns.getRegimenDateInformationParameterized("regimenDate", "dd/MMM/yyyy"), ParameterizableUtil.createParameterMappings("asOfDate=${endDate},untilDate=${endDate}"));

    dataSetDefinition.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("diagnosisNew", confirmedDiagnosis, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentHeight("RecentHeight",  null),new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentWeight("RecentWeight", null),new HashMap<String, Object>());

	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
	    new HashMap<String, Object>());
	
	dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());	
	
       }
	
}
