package org.openmrs.module.rwandareports.reporting;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.ProgramEnrollmentCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfBirthShowingEstimation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.EnrolledInProgram;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAddress;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientRelationship;
import org.openmrs.module.rowperpatientreports.patientdata.definition.SystemIdentifier;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupGenericPatientByProgramReport {
		
	protected final static Log log = LogFactory.getLog(SetupGenericPatientByProgramReport.class);
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

		
	public void setup() throws Exception {		
		ReportDefinition rd =createReportDefinition();		
		  ReportDesign designExcel = Helper.createExcelDesign(rd,"Generic Patient Report.xls_",true);
		  ReportDesign designCSV = Helper.createCsvReportDesign(rd,"Generic Patient Report.csv_");
		  
			Helper.saveReportDesign(designExcel);
			Helper.saveReportDesign(designCSV);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("Generic Patient Report.xls_".equals(rd.getName()) || "Generic Patient Report.csv_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("Generic Patient Report");
	}
	
	private ReportDefinition createReportDefinition() {
		
		Parameter prog=new Parameter("programs", "Program",Program.class);
		prog.setRequired(false);
		
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("Generic Patient Report");	
		reportDefinition.addParameter(new Parameter("startDate", "From Date", Date.class));	
		reportDefinition.addParameter(new Parameter("endDate", "To Date", Date.class));		
		reportDefinition.addParameter(prog);
		
		createDataSetDefinition(reportDefinition,prog);

		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition,Parameter prog) {
		
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("dataSetDefinition");
		
		
		ProgramEnrollmentCohortDefinition prEnoll=new ProgramEnrollmentCohortDefinition();
		prEnoll.addParameter(prog);
		prEnoll.addParameter(new Parameter("enrolledOnOrAfter","enrolledOnOrAfter",Date.class));
		prEnoll.addParameter(new Parameter("enrolledOnOrBefore","enrolledOnOrBefore",Date.class));
		
		
		dataSetDefinition.addFilter(prEnoll, ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${endDate},enrolledOnOrAfter=${startDate},programs=${programs}"));
		
		
		SystemIdentifier systemId = RowPerPatientColumns.getSystemId("System ID");
		dataSetDefinition.addColumn(systemId, new HashMap<String, Object>());
		
		
		MultiplePatientDataDefinitions imbType = RowPerPatientColumns.getIMBId("IMB ID");
		dataSetDefinition.addColumn(imbType, new HashMap<String, Object>());
		
		PatientAddress address1 = RowPerPatientColumns.getPatientAddress("Address", true, true, true, false,false);
		dataSetDefinition.addColumn(address1, new HashMap<String, Object>());
		
		PatientProperty gender = RowPerPatientColumns.getGender("Sex");
		dataSetDefinition.addColumn(gender, new HashMap<String, Object>());
		
		DateOfBirthShowingEstimation birthdate = RowPerPatientColumns.getDateOfBirth("Birth date","dd/MM/yyyy", "dd/MM/yyyy");
		dataSetDefinition.addColumn(birthdate, new HashMap<String, Object>());		
		
		PatientRelationship accompagnateur = RowPerPatientColumns.getAccompRelationship("Accompagnateur");
		accompagnateur.setName("Accompagnateur");
		dataSetDefinition.addColumn(accompagnateur, new HashMap<String, Object>());	
		
		MostRecentObservation socioEcoAssiAlreadyReceived = RowPerPatientColumns.getMostRecent("Socio-Economic Assistance Already Received", Context.getConceptService().getConceptByUuid("3ce169b4-26fe-102b-80cb-0017a47871b2"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(socioEcoAssiAlreadyReceived, new HashMap<String, Object>());	
		
		MostRecentObservation socioEcoAssiRecommended = RowPerPatientColumns.getMostRecent("Socio-Economic Assistance Recommended", Context.getConceptService().getConceptByUuid("3ce16b30-26fe-102b-80cb-0017a47871b2"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(socioEcoAssiRecommended, new HashMap<String, Object>());	
		
		MostRecentObservation exitingCareReason = RowPerPatientColumns.getMostRecent("Reason for exiting care", Context.getConceptService().getConceptByUuid("3cde5ef4-26fe-102b-80cb-0017a47871b2"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(exitingCareReason, new HashMap<String, Object>());	
		
		MostRecentObservation hivStatus = RowPerPatientColumns.getMostRecent("Hiv Status", Context.getConceptService().getConceptByUuid("aec6ad18-f4dd-4cfa-b68d-3d7bb6ea908e"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(hivStatus, new HashMap<String, Object>());	
		
		MostRecentObservation mutuelleLevel = RowPerPatientColumns.getMostRecent("Mutuelle Level", Context.getConceptService().getConceptByUuid("a9191adf-c999-422d-94e0-14de5f076127"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(mutuelleLevel, new HashMap<String, Object>());	
		
		MostRecentObservation insuranceType = RowPerPatientColumns.getMostRecent("Insurance Type", Context.getConceptService().getConceptByUuid("8da67e73-776c-43f6-9758-79d1f6786db3"), "dd/MM/yyyy");
		dataSetDefinition.addColumn(insuranceType, new HashMap<String, Object>());	
		
		List<Program> programs=Context.getProgramWorkflowService().getAllPrograms();
			for (Program program : programs) {
				EnrolledInProgram patientEnrollementDate=RowPerPatientColumns.getPatientProgramInfo(program.getName()+" EnrollmentDate", program, "EnrollmentDate", null);
				EnrolledInProgram patientCompletedDate=RowPerPatientColumns.getPatientProgramInfo(program.getName()+" ExitDate", program, "ExitDate", null);
				EnrolledInProgram patientOutcome=RowPerPatientColumns.getPatientProgramInfo(program.getName()+" OutCome", program, "OutCome", null);
				
				dataSetDefinition.addColumn(patientEnrollementDate, new HashMap<String, Object>());	
				dataSetDefinition.addColumn(patientCompletedDate, new HashMap<String, Object>());
				dataSetDefinition.addColumn(patientOutcome, new HashMap<String, Object>());
			}		
		

		dataSetDefinition.addParameter(new Parameter("startDate", "From Date", Date.class));	
		dataSetDefinition.addParameter(new Parameter("endDate", "To Date", Date.class));
		dataSetDefinition.addParameter(prog);
		
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("startDate", "${startDate}");
		mappings.put("endDate", "${endDate}");
		mappings.put("programs", "${programs}");
		
		
		reportDefinition.addDataSetDefinition("dataSetDefinition",dataSetDefinition,mappings);
		
	}	
}
