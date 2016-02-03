package org.openmrs.module.rwandareports.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObservationInMostRecentEncounterOfType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAgeInMonths;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RecentEncounterType;
import org.openmrs.module.rwandareports.customcalculator.PDCAlerts;
import org.openmrs.module.rwandareports.filter.LastEncounterFilter;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupPDCMonthlyLTFU {
	
	protected final static Log log = LogFactory.getLog(SetupPDCMonthlyLTFU.class);
	
    GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program PDCProgram;
	List<EncounterType> pdcEncounters;
	private EncounterType pdcEncType;
	private List<Form> referralAndVisitForms=new ArrayList<Form>();
	private List<Form> referralAndNotIntakeForm=new ArrayList<Form>();
	private Form referralForm;
    private Form visitForm;
    
	
	
public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "PDCMonthlyLTFUSheet.xls",
		    "PDCMonthlyLTFUSheet.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:9,dataset:pdcmonthly|sheet:1,row:22,dataset:enrollment");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Helper.saveReportDesign(design);
	}
	
  public void delete() {
	ReportService rs = Context.getService(ReportService.class);
	for (ReportDesign rd : rs.getAllReportDesigns(false)) {
		if ("PDCMonthlyLTFUSheet.xls_".equals(rd.getName())) {
			rs.purgeReportDesign(rd);
		}
	}
	Helper.purgeReportDefinition("PDC-Monthly Lost to Follow-up Sheet");
}
	
private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("PDC-Monthly Lost to Follow-up Sheet");
				
		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));	
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),ParameterizableUtil.createParameterMappings("location=${location}"));
		reportDefinition.addParameter(new Parameter("endDate", "Month of", Date.class));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		//====================================================================
		//           Patients Dataset definitions
		//====================================================================
		
		// Create all pdc patients dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition1 = new RowPerPatientDataSetDefinition();
		dataSetDefinition1.setName("PDC Monthly LTFU Data Set");
		
		// Create patients needing to be in pdc dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("PDC monthly no intake form");
		
		//add dates parameters to datasets
		dataSetDefinition1.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition2.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition1.addParameter(new Parameter("endDate", "Month", Date.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "Month", Date.class));
		
		//Add filters
		dataSetDefinition1.addFilter(Cohorts.createInProgramParameterizableByDate("Patients in "+PDCProgram.getName(), PDCProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition1.addFilter(Cohorts.getMondayToSundayPatientReturnVisitAndFollowUp(referralAndVisitForms), ParameterizableUtil.createParameterMappings("end=${endDate+30d},start=${endDate}"));
		
		InProgramCohortDefinition inPDCProgramCohort = Cohorts.createInProgramParameterizableByDate("inPDCProgramCohort", PDCProgram);
		CompositionCohortDefinition patientsNotEnrolledInPDC = new CompositionCohortDefinition();
		patientsNotEnrolledInPDC.setName("patientsNotEnrolledInPDC");
		patientsNotEnrolledInPDC.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsNotEnrolledInPDC.getSearches().put("inPDCProgramCohort",new Mapped<CohortDefinition>(inPDCProgramCohort, ParameterizableUtil
		            .createParameterMappings("onDate=${now}")));
		patientsNotEnrolledInPDC.setCompositionString("NOT inPDCProgramCohort");
				
		dataSetDefinition2.addFilter(Cohorts.getMondayToSundayPatientReturnVisit(referralAndNotIntakeForm), ParameterizableUtil.createParameterMappings("end=${endDate+30d},start=${endDate}"));
		dataSetDefinition2.addFilter(patientsNotEnrolledInPDC,ParameterizableUtil.createParameterMappings("onDate=${now}"));
		//Add Columns
				dataSetDefinition1.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
				dataSetDefinition1.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
				dataSetDefinition1.addColumn(RowPerPatientColumns.getIMBId("Id"), new HashMap<String, Object>());
				
				dataSetDefinition2.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
				dataSetDefinition2.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
				dataSetDefinition2.addColumn(RowPerPatientColumns.getIMBId("Id"), new HashMap<String, Object>());
				
				PatientAgeInMonths ageinMonths=RowPerPatientColumns.getAgeInMonths("age");
				dataSetDefinition1.addColumn(ageinMonths, new HashMap<String, Object>());
				dataSetDefinition2.addColumn(ageinMonths, new HashMap<String, Object>());
				
				PatientProperty ageinYrs=RowPerPatientColumns.getAge("ageinYrs");
				dataSetDefinition1.addColumn(ageinYrs, new HashMap<String, Object>());
				dataSetDefinition2.addColumn(ageinYrs, new HashMap<String, Object>());
				
		    	dataSetDefinition1.addColumn(RowPerPatientColumns.getGender("Sex"), new HashMap<String, Object>());		
		    	dataSetDefinition2.addColumn(RowPerPatientColumns.getGender("Sex"), new HashMap<String, Object>());		
				
				ObservationInMostRecentEncounterOfType nextVisit = RowPerPatientColumns.getReturnVisitInMostRecentEncounterOfType("nextVisit",pdcEncType);
				dataSetDefinition1.addColumn(nextVisit, new HashMap<String, Object>());
				dataSetDefinition2.addColumn(nextVisit, new HashMap<String, Object>());
			
				RecentEncounterType lastEncInMonth = RowPerPatientColumns.getRecentEncounterType("lastEnc",pdcEncounters,null, null);
				dataSetDefinition1.addColumn(lastEncInMonth, new HashMap<String, Object>());
				
				RecentEncounterType lastEncounterType = RowPerPatientColumns.getRecentEncounterType("LastVisit",pdcEncounters, "dd-MMM-yyyy", new LastEncounterFilter());
				dataSetDefinition1.addColumn(lastEncounterType, new HashMap<String, Object>());
				dataSetDefinition2.addColumn(lastEncounterType, new HashMap<String, Object>());
				
				CustomCalculationBasedOnMultiplePatientDataDefinitions alert = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
				alert.setName("alert");
				alert.addPatientDataToBeEvaluated(ageinYrs, new HashMap<String, Object>());
				alert.addPatientDataToBeEvaluated(nextVisit, new HashMap<String, Object>());
				alert.addPatientDataToBeEvaluated(lastEncInMonth, new HashMap<String, Object>());
				alert.setCalculator(new PDCAlerts());
				dataSetDefinition1.addColumn(alert, new HashMap<String, Object>());	
				
				Map<String, Object> mappings = new HashMap<String, Object>();
				mappings.put("location", "${location}");
				mappings.put("endDate", "${endDate}");
				
				reportDefinition.addDataSetDefinition("pdcmonthly", dataSetDefinition1, mappings);
		        reportDefinition.addDataSetDefinition("enrollment", dataSetDefinition2, mappings);
	
	}
	
	private void setupProperties() {
		PDCProgram = gp.getProgram(GlobalPropertiesManagement.PDC_PROGRAM);
	    pdcEncounters = gp.getEncounterTypeList(GlobalPropertiesManagement.PDC_VISIT);
	    pdcEncType = gp.getEncounterType(GlobalPropertiesManagement.PDC_VISIT);
	    referralForm=gp.getForm(GlobalPropertiesManagement.PDC_REFERRAL_FORM);
	    visitForm=gp.getForm(GlobalPropertiesManagement.PDC_VISIT_FORM);
	    referralAndVisitForms.add(referralForm);
	    referralAndVisitForms.add(visitForm);
	    referralAndNotIntakeForm.add(referralForm);
	   
		
	}
}