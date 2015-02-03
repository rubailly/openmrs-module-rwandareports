package org.openmrs.module.rwandareports.example;

import org.openmrs.module.reporting.cohort.definition.library.BuiltInCohortDefinitionLibrary;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.data.patient.library.BuiltInPatientDataLibrary;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.manager.BaseReportManager;
import org.openmrs.module.reporting.report.manager.ReportManagerUtil;
import org.openmrs.module.rwandareports.example.library.DataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Example Row-per-patient Report using reporting module
 */
@Component
public class SampleRowPerPatientReport extends ImbReportManager {

	public static final String REPORT_DESIGN_UUID = "57ead9f9-412c-4250-b1db-03becf92ccbe";

	@Autowired
	DataFactory df;

	@Autowired
	BuiltInCohortDefinitionLibrary builtInCohorts;

	@Autowired
	BuiltInPatientDataLibrary builtInPatientData;

	@Override
	public String getUuid() {
		return "3dc0ef67-7497-45c5-b4e9-b51d6aaa85ce";
	}

	@Override
	public String getName() {
		return "Sample Row-per-patient Report";
	}

	@Override
	public String getDescription() {
		return "This is an example to demonstrate how we might use the reporting module";
	}

	@Override
	public List<Parameter> getParameters() {
		List<Parameter> l = new ArrayList<Parameter>();
		l.add(df.getStartDateParameter());
		l.add(df.getEndDateParameter());
		return l;
	}

	@Override
	public ReportDefinition constructReportDefinition() {
		ReportDefinition rd = new ReportDefinition();
		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		rd.setParameters(getParameters());

		PatientDataSetDefinition dsd = new PatientDataSetDefinition();
		dsd.setName(getName());
		dsd.addParameters(getParameters());

		rd.addDataSetDefinition("patients", Mapped.mapStraightThrough(dsd));

		// Define rows for the report - patients with an encounter during the period
		dsd.addRowFilter(Mapped.mapStraightThrough(builtInCohorts.getAnyEncounterDuringPeriod()));

		// Define columns for the report
		addColumn(dsd, "PID", builtInPatientData.getPatientId());
		addColumn(dsd, "FN", builtInPatientData.getPreferredGivenName());
		addColumn(dsd, "LN", builtInPatientData.getPreferredFamilyName());
		addColumn(dsd, "Gender", builtInPatientData.getGender());

		return rd;
	}

	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		List<ReportDesign> l = new ArrayList<ReportDesign>();
		ReportDesign d = ReportManagerUtil.createExcelDesign(REPORT_DESIGN_UUID, reportDefinition);
		d.addPropertyValue("sortWeight", "5000");
		l.add(d);
		return l;
	}

	@Override
	public String getVersion() {
		return "1.0-SNAPSHOT";
	}
}
