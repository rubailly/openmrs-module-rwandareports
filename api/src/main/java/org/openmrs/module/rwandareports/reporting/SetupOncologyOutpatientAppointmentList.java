package org.openmrs.module.rwandareports.reporting;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Program;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.renderer.CalendarWebRenderer;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupOncologyOutpatientAppointmentList {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;
	
	private Concept scheduledVisit;
	
	private Concept biopsyResultVisit;
	
	private Concept specialVisit;
	
	private Concept biopsyVisit;
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		createCustomWebRenderer(rd);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("Outpatient Appointment List".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("ONC-Oncology Outpatient Clinic Appointment List");
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Oncology Outpatient Clinic Appointment List");
		
		reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram),
		    ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("Routine Visit");
		
		RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("Biopsy Result Visit");
		
		RowPerPatientDataSetDefinition dataSetDefinition3 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("Special Consultation");
		
		RowPerPatientDataSetDefinition dataSetDefinition4 = new RowPerPatientDataSetDefinition();
		dataSetDefinition4.setName("Biopsy to be performed");
		
		dataSetDefinition.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition2.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition3.addParameter(new Parameter("endDate", "endDate", Date.class));
		dataSetDefinition4.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		dataSetDefinition2.setSortCriteria(sortCriteria);
		dataSetDefinition3.setSortCriteria(sortCriteria);
		dataSetDefinition4.setSortCriteria(sortCriteria);
		
		//Add filters
		dataSetDefinition.addFilter(Cohorts.createDateObsCohortDefinition(scheduledVisit, RangeComparator.GREATER_EQUAL,
		    RangeComparator.LESS_EQUAL, TimeModifier.LAST), ParameterizableUtil
		        .createParameterMappings("value2=${endDate+6M},value1=${endDate-14d}"));
		dataSetDefinition2.addFilter(Cohorts.createDateObsCohortDefinition(biopsyResultVisit, RangeComparator.GREATER_EQUAL,
		    RangeComparator.LESS_EQUAL, TimeModifier.LAST), ParameterizableUtil
		        .createParameterMappings("value2=${endDate+6M},value1=${endDate-14d}"));
		dataSetDefinition3.addFilter(Cohorts.createDateObsCohortDefinition(specialVisit, RangeComparator.GREATER_EQUAL,
		    RangeComparator.LESS_EQUAL, TimeModifier.LAST), ParameterizableUtil
		        .createParameterMappings("value2=${endDate+6M},value1=${endDate-14d}"));
		dataSetDefinition4.addFilter(Cohorts.createDateObsCohortDefinition(biopsyVisit, RangeComparator.GREATER_EQUAL,
		    RangeComparator.LESS_EQUAL, TimeModifier.LAST), ParameterizableUtil
		        .createParameterMappings("value2=${endDate+6M},value1=${endDate-14d}"));
		
		
		//Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getSystemId("id"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getSystemId("id"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getSystemId("id"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getSystemId("id"), new HashMap<String, Object>());
		
		dataSetDefinition2.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getArchivingId("archivingId"), new HashMap<String, Object>());
		
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
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("date", scheduledVisit, "yyyy-MM-dd"),
		    new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("date", biopsyResultVisit, "yyyy-MM-dd"),
		    new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("date", specialVisit, "yyyy-MM-dd"),
		    new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("date", biopsyVisit, "yyyy-MM-dd"),
		    new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("imbId"), new HashMap<String, Object>());
		dataSetDefinition2.addColumn(RowPerPatientColumns.getIMBId("imbId"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getIMBId("imbId"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getIMBId("imbId"), new HashMap<String, Object>());
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("Routine Visit", dataSetDefinition, mappings);
		reportDefinition.addDataSetDefinition("Biopsy Result Visit", dataSetDefinition2, mappings);
		reportDefinition.addDataSetDefinition("Special Consultation", dataSetDefinition3, mappings);
		reportDefinition.addDataSetDefinition("Biopsy to be performed", dataSetDefinition4, mappings);
	}
	
	private void setupProperties() {
		
		oncologyProgram = gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		scheduledVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SCHEDULED_OUTPATIENT_VISIT);
		
		biopsyResultVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_PATHOLOGY_RESULT_VISIT);
		
		specialVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SPECIAL_VISIT);
		
		biopsyVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SCHEDULED_TEST_VISIT);
	}
	
	private void createCustomWebRenderer(ReportDefinition rd) throws IOException {
		final ReportDesign design = new ReportDesign();
		design.setName("Calendar");
		design.setReportDefinition(rd);
		design.setRendererType(CalendarWebRenderer.class);
		
		Helper.saveReportDesign(design);
		
	}
}
