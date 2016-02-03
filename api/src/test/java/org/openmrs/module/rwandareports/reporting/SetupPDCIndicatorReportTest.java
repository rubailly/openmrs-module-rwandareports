package org.openmrs.module.rwandareports.reporting;

import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.api.PatientSetService;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.common.SetComparator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.rwandareports.StandaloneContextSensitiveTest;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.MetadataLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;

/**
 * Tests the indicators in the PDC Indicator Report
 */
@Ignore
public class SetupPDCIndicatorReportTest extends StandaloneContextSensitiveTest {

    @Autowired
    @Qualifier(value = "reportingCohortDefinitionService")
    CohortDefinitionService cohortDefinitionService;

    @Test
    public void testReasonForNotDoingFollowupCohort() throws Exception {
        EvaluationContext context = new EvaluationContext();
        context.addParameterValue("endDate", new Date());

        Concept reasonForNotDoingFollowUp= MetadataLookup.getConcept("REASON FOR NOT DOING FOLLOW-UP");
        CodedObsCohortDefinition codedObsDef = Cohorts.createCodedObsCohortDefinition("reasonForNotDoingFollowUpCohort",reasonForNotDoingFollowUp, null, SetComparator.IN, PatientSetService.TimeModifier.LAST);
        Cohort codedObsCohort = cohortDefinitionService.evaluate(codedObsDef, context);
        System.out.println("Coded obs cohort: " + codedObsCohort.size());

        SqlCohortDefinition sqlDef = new SqlCohortDefinition();
        sqlDef.setName("reasonForNotDoingFollowUpCohort");
        sqlDef.setQuery("select distinct o.person_id from obs o where o.concept_id="+reasonForNotDoingFollowUp.getConceptId()+" and o.voided=0 order by o.obs_datetime desc");
        sqlDef.addParameter(new Parameter("endDate","endDate",Date.class));
        Cohort sqlCohort = cohortDefinitionService.evaluate(sqlDef, context);
        System.out.println("Sql obs cohort: " + sqlCohort.size());
    }

}
