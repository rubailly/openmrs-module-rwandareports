package org.openmrs.module.rwandareports.reporting;
 
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.PRIVATE_MEMBER;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.ProgramEnrollmentCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.common.SetComparator;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.query.encounter.definition.EncounterQuery;
import org.openmrs.module.reporting.query.encounter.definition.SqlEncounterQuery;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandareports.dataset.EncounterIndicatorDataSetDefinition;
import org.openmrs.module.rwandareports.dataset.LocationHierachyIndicatorDataSetDefinition;
import org.openmrs.module.rwandareports.indicator.EncounterIndicator;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.Indicators;
import org.openmrs.module.rwandareports.widget.AllLocation;
import org.openmrs.module.rwandareports.widget.LocationHierarchy;
 
public class SetupPDCIndicatorReport {
        
        GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
        
        //properties
        private Program PDCProgram;
        
        private List<String> onOrAfterOnOrBefore = new ArrayList<String>();
        
        
        private Concept reasoForReferral;
        
        private Concept returnVisitDate;
                 
        private Concept breathingDangerSignsPresent;
         
        private Concept convulsionsDangerSignsPresent;         
         
        private Concept LethargyOrUnresponsivenessDangerSignsPresent;
         
        private Concept umbilicalCordRednessDangerSigns;        
         
        private Concept stiffNeckOrBulgingFontanellesDangerSigns;
         
        private Concept ASQAgeUsed;

        private Concept heightForAgeZScore;

        private Concept temperature;
          
        private Concept respiratoryRate;
        
        private Concept yes;
        
        private Concept socialEconomicAssistance;
        
        private Concept socialEconomicAssistanceAlreadyReceived;
        
        private Concept socialEconomicAssistanceRecommanded;
        
        private Concept socialEconomicAssistanceNotRecommanded;
        
        private List<Concept> socialAssistanceTypes=new ArrayList<Concept>();
        
        private Form PDCIntakeForm;
        
        private Form PDCReferralForm;
        
        private Form PDCVisitForm;
        
        
        private List<Form> intakeVisitForms=new ArrayList<Form>();
        
        private List<Form> PDCVisitForms=new ArrayList<Form>();
        
        private Concept intervalGrogth;
        
        private Concept birthWeight;
        
        private Concept correctedAge;        
        
        private Concept  weightForAgeZScore;
        
        private Concept HCForAgeZScore;
        
        private Concept weightForHeightZScore;
        
        private Concept headCircumference;
        
        private Concept weight;
        
        private Concept height;
        
        private Concept smallMuscleMovement;
        
        private Concept largeMuscleMovement;
        
        private Concept communication;
        
        private Concept problemSolving;
        
        private Concept personalSocial;
        
        private Concept abnormal;
        
        private Concept ECDCounselingSession;
        
        private Concept reasonForNotDoingFollowUp;
        
        private Concept intervalgrowthCoded;
        
        private Concept inadequate;
        

        
        public void setup() throws Exception {
                
                setUpProperties();
                
                
                ReportDefinition rd = new ReportDefinition();
                rd.addParameter(new Parameter("startDate", "Start Date", Date.class));
                rd.addParameter(new Parameter("endDate", "End Date", Date.class));
                
                Properties properties = new Properties();
                properties.setProperty("hierarchyFields", "countyDistrict:District");
                rd.addParameter(new Parameter("location", "Location", AllLocation.class, properties));
                
                rd.setName("PDC-Indicator Report");
                
                rd.addDataSetDefinition(createQuarterlyLocationDataSet(),
                    ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate},location=${location}"));
                        
                
                Helper.saveReportDefinition(rd);
                
                
                ReportDesign monthlyDesign = Helper.createRowPerPatientXlsOverviewReportDesign(rd,"PDCIndicators.xls", "PDCIndicators", null);
                Properties monthlyProps = new Properties();
                monthlyProps.put("repeatingSections", "sheet:1,dataset:PDC Data Set");
                monthlyProps.put("sortWeight","5000");
                monthlyDesign.setProperties(monthlyProps);
                Helper.saveReportDesign(monthlyDesign);              
                
                
        }
        
        public void delete() {
                ReportService rs = Context.getService(ReportService.class);
                for (ReportDesign rd : rs.getAllReportDesigns(false)) {
                        if ("PDCIndicators".equals(rd.getName())) {
                                rs.purgeReportDesign(rd);
                        }
                }
                Helper.purgeReportDefinition("PDC-Indicator Report");
                
        }
        
        
        public LocationHierachyIndicatorDataSetDefinition createQuarterlyLocationDataSet() {
                
                LocationHierachyIndicatorDataSetDefinition ldsd = new LocationHierachyIndicatorDataSetDefinition(
                        createQuarterlyEncounterBaseDataSet());
                ldsd.addBaseDefinition(createQuarterlyBaseDataSet());
                ldsd.setName("PDC Data Set");
                ldsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
                ldsd.addParameter(new Parameter("endDate", "End Date", Date.class));
                ldsd.addParameter(new Parameter("location", "District", LocationHierarchy.class));
                
                return ldsd;
        }
        
        private EncounterIndicatorDataSetDefinition createQuarterlyEncounterBaseDataSet() {
                
                EncounterIndicatorDataSetDefinition eidsd = new EncounterIndicatorDataSetDefinition();
                
                eidsd.setName("eidsd");
                eidsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
                eidsd.addParameter(new Parameter("endDate", "End Date", Date.class));
                
                createQuarterlyIndicators(eidsd);
                return eidsd;
        }
        
        private void createQuarterlyIndicators(EncounterIndicatorDataSetDefinition dsd) {
                        	
// PDC6N       	
        	
        	  SqlEncounterQuery visitWithNutritionDangerSignSreeningAndASQdocumentedQuery=new SqlEncounterQuery();
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQuery.setName("visitWithNutritionDangerSignSreeningAndASQdocumentedQuery");
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQuery.setQuery("select e.encounter_id from encounter e"
				+" inner join obs o1 on e.encounter_id=o1.encounter_id and o1.concept_id= "+breathingDangerSignsPresent.getConceptId()+" and o1.value_coded="+yes.getConceptId()+" and o1.voided=0"
				+" inner join obs o2 on e.encounter_id=o2.encounter_id and o2.concept_id= "+convulsionsDangerSignsPresent.getConceptId()+" and o2.value_coded="+yes.getConceptId()+" and o2.voided=0"
				+" inner join obs o3 on e.encounter_id=o3.encounter_id and o3.concept_id= "+LethargyOrUnresponsivenessDangerSignsPresent.getConceptId()+" and o3.value_coded="+yes.getConceptId()+" and o3.voided=0"
				+" inner join obs o4 on e.encounter_id=o4.encounter_id and o4.concept_id= "+umbilicalCordRednessDangerSigns.getConceptId()+" and o4.value_coded="+yes.getConceptId()+" and o4.voided=0"
				+" inner join obs o5 on e.encounter_id=o5.encounter_id and o5.concept_id= "+stiffNeckOrBulgingFontanellesDangerSigns.getConceptId()+" and o5.value_coded="+yes.getConceptId()+" and o5.voided=0"
				+" inner join obs o6 on e.encounter_id=o6.encounter_id and o6.concept_id= "+ASQAgeUsed.getConceptId()+" and o6.value_coded is not null and o6.voided=0"
				
				+" inner join obs o7 on e.encounter_id=o7.encounter_id and o7.concept_id= "+heightForAgeZScore.getConceptId()+" and o7.value_numeric is not null and o7.voided=0"
				+" inner join obs o8 on e.encounter_id=o8.encounter_id and o8.concept_id= "+temperature.getConceptId()+" and o8.value_numeric is not null and o8.voided=0"
				+" inner join obs o9 on e.encounter_id=o9.encounter_id and o9.concept_id= "+respiratoryRate.getConceptId()+" and o9.value_numeric is not null and o9.voided=0"
				+" inner join person p on e.patient_id=p.person_id and DATEDIFF(e.encounter_datetime,p.birthdate)>180 and DATEDIFF(e.encounter_datetime,p.birthdate)<=720" 
				+" where  e.form_id="+PDCVisitForm.getFormId()+" and e.encounter_datetime>= :startDate and e.encounter_datetime<= :endDate");
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQuery.addParameter(new Parameter("startDate", "startDate", Date.class));
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQuery.addParameter(new Parameter("endDate", "endDate", Date.class));
              
        	  
        	  
        	  EncounterIndicator visitWithNutritionDangerSignSreeningAndASQdocumentedQueryIndicator = new EncounterIndicator();
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQueryIndicator.setName("visitWithNutritionDangerSignSreeningAndASQdocumentedQueryIndicator");
        	  visitWithNutritionDangerSignSreeningAndASQdocumentedQueryIndicator.setEncounterQuery(new Mapped<EncounterQuery>(visitWithNutritionDangerSignSreeningAndASQdocumentedQuery,ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
              
              dsd.addColumn(visitWithNutritionDangerSignSreeningAndASQdocumentedQueryIndicator); 
        	  
   // PDC6D     	  
        	  
        	  SqlEncounterQuery visitDoneWhenChildIsBetween6And24MonthsQuery=new SqlEncounterQuery();
        	  visitDoneWhenChildIsBetween6And24MonthsQuery.setName("visitDoneWhenChildIsBetween6And24MonthsQuery");
        	  visitDoneWhenChildIsBetween6And24MonthsQuery.setQuery("select e.encounter_id from encounter e"
				+" inner join person p on e.patient_id=p.person_id and DATEDIFF(e.encounter_datetime,p.birthdate)>180 and DATEDIFF(e.encounter_datetime,p.birthdate)<=720" 
				+" where  e.form_id="+PDCVisitForm.getFormId()+" and e.encounter_datetime>= :startDate and e.encounter_datetime<= :endDate");
        	  visitDoneWhenChildIsBetween6And24MonthsQuery.addParameter(new Parameter("startDate", "startDate", Date.class));
        	  visitDoneWhenChildIsBetween6And24MonthsQuery.addParameter(new Parameter("endDate", "endDate", Date.class));
              
        	  
        	  EncounterIndicator visitDoneWhenChildIsBetween6And24MonthsQueryIndicator = new EncounterIndicator();
        	  visitDoneWhenChildIsBetween6And24MonthsQueryIndicator.setName("visitDoneWhenChildIsBetween6And24MonthsQueryIndicator");
        	  visitDoneWhenChildIsBetween6And24MonthsQueryIndicator.setEncounterQuery(new Mapped<EncounterQuery>(visitDoneWhenChildIsBetween6And24MonthsQuery,ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
              
              dsd.addColumn(visitDoneWhenChildIsBetween6And24MonthsQueryIndicator); 
              
              
// PDC9N       	
          	
        	  SqlEncounterQuery visitWithDangerSignsDocumentedQuery=new SqlEncounterQuery();
        	  visitWithDangerSignsDocumentedQuery.setName("visitWithDangerSignsDocumentedQuery");
        	  visitWithDangerSignsDocumentedQuery.setQuery("select e.encounter_id from encounter e"
				+" inner join obs o1 on e.encounter_id=o1.encounter_id and o1.concept_id= "+breathingDangerSignsPresent.getConceptId()+" and o1.voided=0"
				+" inner join obs o2 on e.encounter_id=o2.encounter_id and o2.concept_id= "+convulsionsDangerSignsPresent.getConceptId()+" and o2.voided=0"
				+" inner join obs o3 on e.encounter_id=o3.encounter_id and o3.concept_id= "+LethargyOrUnresponsivenessDangerSignsPresent.getConceptId()+" and o3.voided=0"
				+" inner join obs o4 on e.encounter_id=o4.encounter_id and o4.concept_id= "+umbilicalCordRednessDangerSigns.getConceptId()+" and o4.voided=0"
				+" inner join obs o5 on e.encounter_id=o5.encounter_id and o5.concept_id= "+stiffNeckOrBulgingFontanellesDangerSigns.getConceptId()+" and o5.voided=0"
				+" inner join obs o8 on e.encounter_id=o8.encounter_id and o8.concept_id= "+temperature.getConceptId()+" and o8.voided=0"
				+" inner join obs o9 on e.encounter_id=o9.encounter_id and o9.concept_id= "+respiratoryRate.getConceptId()+" and o9.voided=0"
				+" where  e.form_id="+PDCVisitForm.getFormId()+" and e.encounter_datetime>= :startDate and e.encounter_datetime<= :endDate");
        	  visitWithDangerSignsDocumentedQuery.addParameter(new Parameter("startDate", "startDate", Date.class));
        	  visitWithDangerSignsDocumentedQuery.addParameter(new Parameter("endDate", "endDate", Date.class));
              
        	  
        	  
        	  EncounterIndicator visitWithDangerSignsDocumentedQueryIndicator = new EncounterIndicator();
        	  visitWithDangerSignsDocumentedQueryIndicator.setName("visitWithDangerSignsDocumentedQueryIndicator");
        	  visitWithDangerSignsDocumentedQueryIndicator.setEncounterQuery(new Mapped<EncounterQuery>(visitWithDangerSignsDocumentedQuery,ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
              
              dsd.addColumn(visitWithDangerSignsDocumentedQueryIndicator); 
   // PDC9D     	  
        	  
        	  SqlEncounterQuery visitsInQuarter=new SqlEncounterQuery();
        	  visitsInQuarter.setName("visitsInQuarter");
        	  visitsInQuarter.setQuery("select e.encounter_id from encounter e"
				+" where  e.form_id="+PDCVisitForm.getFormId()+" and e.encounter_datetime>= :startDate and e.encounter_datetime<= :endDate");
        	  visitsInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
        	  visitsInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
              
        	  
        	  EncounterIndicator visitsInQuarterIndicator = new EncounterIndicator();
        	  visitsInQuarterIndicator.setName("visitsInQuarterIndicator");
        	  visitsInQuarterIndicator.setEncounterQuery(new Mapped<EncounterQuery>(visitsInQuarter,ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
              
              dsd.addColumn(visitsInQuarterIndicator); 
          }
        
     private CohortIndicatorDataSetDefinition createQuarterlyBaseDataSet() {
                CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
                dsd.setName("Quarterly Cohort Data Set");
                dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
                dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
                
                createQuarterlyIndicators(dsd);
                return dsd;
        }
                
        private void createQuarterlyIndicators(CohortIndicatorDataSetDefinition dsd) {
                
                InProgramCohortDefinition inPDCProgramByDate = Cohorts.createInProgramParameterizableByDate(
                    "In PDC Programs by date", PDCProgram, "onOrBefore");
                
                InProgramCohortDefinition inPDCProgramByStartDateAndEndDate = Cohorts.createInProgramParameterizableByDate(
                    "In PDC Programs by startdate and enddate", PDCProgram, onOrAfterOnOrBefore);
                
                EncounterCohortDefinition  patientWithVisitInQuarter=Cohorts.createEncounterBasedOnForms("patientWithVisitInQuarter", onOrAfterOnOrBefore, PDCVisitForms);
                
                
//PDC1
                SqlCohortDefinition patientUnderOneYear=new SqlCohortDefinition();
                patientUnderOneYear.setName("patientUnderOneYear");
                patientUnderOneYear.setQuery("select p.person_id from person p where DATEDIFF(:endDate,p.birthdate)<365 and p.voided=0");
                patientUnderOneYear.addParameter(new Parameter("endDate","endDate",Date.class));

                
                SqlCohortDefinition patientGreatterThanOrEqualOneYearAndLessThanTwoyears=new SqlCohortDefinition();
                patientGreatterThanOrEqualOneYearAndLessThanTwoyears.setName("patientGreatterThanOrEqualOneYearAndLessThanTwoyears");
                patientGreatterThanOrEqualOneYearAndLessThanTwoyears.setQuery("select p.person_id from person p where DATEDIFF(:endDate,p.birthdate)>=365 and DATEDIFF(:endDate,p.birthdate)<730 and p.voided=0");
                patientGreatterThanOrEqualOneYearAndLessThanTwoyears.addParameter(new Parameter("endDate","endDate",Date.class));

                SqlCohortDefinition patientGreatterThanOrEqualTwoyears=new SqlCohortDefinition();
                patientGreatterThanOrEqualTwoyears.setName("patientGreatterThanOrEqualTwoyears");
                patientGreatterThanOrEqualTwoyears.setQuery("select p.person_id from person p where DATEDIFF(:endDate,p.birthdate)>=730 and p.voided=0");
                patientGreatterThanOrEqualTwoyears.addParameter(new Parameter("endDate","endDate",Date.class));
                
                //CodedObsCohortDefinition reasonForNotDoingFollowUpCohort=Cohorts.createCodedObsCohortDefinition("reasonForNotDoingFollowUpCohort",reasonForNotDoingFollowUp, null, SetComparator.IN, TimeModifier.LAST);
                
                SqlCohortDefinition reasonForNotDoingFollowUpCohort=new SqlCohortDefinition();
                reasonForNotDoingFollowUpCohort.setName("reasonForNotDoingFollowUpCohort");
                reasonForNotDoingFollowUpCohort.setQuery("select distinct o.person_id from obs o where o.concept_id="+reasonForNotDoingFollowUp.getConceptId()+" and o.voided=0 order by o.obs_datetime desc");
                reasonForNotDoingFollowUpCohort.addParameter(new Parameter("endDate","endDate",Date.class));

             
                CompositionCohortDefinition activePatient = new CompositionCohortDefinition();
                activePatient.setName("activePatient");
                activePatient.addParameter(new Parameter("startDate", "startDate", Date.class));
                activePatient.addParameter(new Parameter("endDate", "endDate", Date.class));
                activePatient.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
                activePatient.getSearches().put("2", new Mapped(patientWithVisitInQuarter, ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6m},onOrBefore=${endDate}")));
                activePatient.getSearches().put("3", new Mapped(patientWithVisitInQuarter, ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-9m},onOrBefore=${endDate}")));
                activePatient.getSearches().put("4", new Mapped(patientWithVisitInQuarter, ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-18m},onOrBefore=${endDate}")));
                activePatient.getSearches().put("5", new Mapped(patientUnderOneYear, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
                activePatient.getSearches().put("6", new Mapped(patientGreatterThanOrEqualOneYearAndLessThanTwoyears, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
                activePatient.getSearches().put("7", new Mapped(patientGreatterThanOrEqualTwoyears, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
                activePatient.getSearches().put("8", new Mapped(reasonForNotDoingFollowUpCohort, null));
                activePatient.setCompositionString("1 AND (NOT (2 AND 5)) AND (NOT (3 AND 6)) AND (NOT (4 AND 7)) AND (NOT 8)");
                
                
                CohortIndicator activePatientIndicator = Indicators.newCountIndicator("Number of active patients Indicator", activePatient,
                        ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                           
                    
                dsd.addColumn("PDC1", "Number of active patients",
                        new Mapped(activePatientIndicator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
            
                    
// PDC2
               // SqlCohortDefinition patientWithRefferralReasonOnIntakeForm=Cohorts.getPatientsWithObservationInFormBetweenStartAndEndDate("patientWithReferralReason",PDCIntakeForm,reasoForReferral);
                SqlCohortDefinition patientWithRefferralReasonOnIntakeForm=new SqlCohortDefinition();
                patientWithRefferralReasonOnIntakeForm.setName("patientWithRefferralReasonOnIntakeForm");
                patientWithRefferralReasonOnIntakeForm.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCIntakeForm.getFormId()+" and o.concept_id="+reasoForReferral.getConceptId()+" " +
                "and o.voided=0 and e.voided=0 and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL) and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
                patientWithRefferralReasonOnIntakeForm.addParameter(new Parameter("startDate","startDate",Date.class));
                patientWithRefferralReasonOnIntakeForm.addParameter(new Parameter("endDate","endDate",Date.class));
                
                //SqlCohortDefinition patientWithRefferralReasonOnReferralForm=Cohorts.getPatientsWithObservationInFormBetweenStartAndEndDate("patientWithReferralReason",PDCReferralForm,reasoForReferral);
                SqlCohortDefinition patientWithRefferralReasonOnReferralForm=new SqlCohortDefinition();
                patientWithRefferralReasonOnReferralForm.setName("patientWithRefferralReasonOnReferralForm");
                patientWithRefferralReasonOnReferralForm.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCReferralForm.getFormId()+" and o.concept_id="+reasoForReferral.getConceptId()+" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
                patientWithRefferralReasonOnReferralForm.addParameter(new Parameter("startDate","startDate",Date.class));
                patientWithRefferralReasonOnReferralForm.addParameter(new Parameter("endDate","endDate",Date.class));
                
                CompositionCohortDefinition newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm = new CompositionCohortDefinition();
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.setName("newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm");
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.addParameter(new Parameter("startDate", "startDate", Date.class));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.addParameter(new Parameter("endDate", "endDate", Date.class));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate}")));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("2", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("3", new Mapped(patientWithRefferralReasonOnIntakeForm, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("4", new Mapped(patientWithRefferralReasonOnReferralForm, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.setCompositionString("(2 AND (NOT 1)) AND (3 OR 4)");
                           
                CohortIndicator newPatientEnrolledWithRefferralReasonOnIntakeOrReferralFormIndicator = Indicators.newCountIndicator("Number of newly enrolled patients total and per intake category indicator", newPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm,
                    ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                       
                dsd.addColumn("PDC2", "Number of newly enrolled patients total and per intake category",
                    new Mapped(newPatientEnrolledWithRefferralReasonOnIntakeOrReferralFormIndicator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
        
                
// PDC3
               // SqlCohortDefinition patientWithRefferralReasonOnIntakeFormAnyTime=Cohorts.getPatientsWithObservationInForm("patientWithReferralReasonAnyTime",PDCIntakeForm,reasoForReferral);
                
                SqlCohortDefinition patientWithRefferralReasonOnIntakeFormAnyTime=new SqlCohortDefinition();
                patientWithRefferralReasonOnIntakeFormAnyTime.setName("patientWithRefferralReasonOnIntakeFormAnyTime");
                patientWithRefferralReasonOnIntakeFormAnyTime.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCIntakeForm.getFormId()+" and o.concept_id="+reasoForReferral.getConceptId()+" " +
                "and o.voided=0 and e.voided=0 and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
                
                
                //SqlCohortDefinition patientWithRefferralReasonOnReferralFormAnyTime=Cohorts.getPatientsWithObservationInForm("patientWithReferralReasonAnyTime",PDCReferralForm,reasoForReferral);
                
                SqlCohortDefinition patientWithRefferralReasonOnReferralFormAnyTime=new SqlCohortDefinition();
                patientWithRefferralReasonOnReferralFormAnyTime.setName("patientWithRefferralReasonOnIntakeFormAnyTime");
                patientWithRefferralReasonOnReferralFormAnyTime.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCReferralForm.getFormId()+" and o.concept_id="+reasoForReferral.getConceptId()+" " +
                "and o.voided=0 and e.voided=0 and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
                
                
                CompositionCohortDefinition everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm = new CompositionCohortDefinition();
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.setName("everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm");
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.addParameter(new Parameter("endDate", "endDate", Date.class));
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("2", new Mapped(patientWithRefferralReasonOnIntakeFormAnyTime, null));
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.getSearches().put("3", new Mapped(patientWithRefferralReasonOnReferralFormAnyTime, null));
                everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm.setCompositionString("1 AND (2 OR 3)");
                           
                CohortIndicator everPatientEnrolledWithRefferralReasonOnIntakeOrReferralFormIndicator = Indicators.newCountIndicator("Number of ever enrolled patients total and per intake category indicator", everPatientEnrolledWithRefferralReasonOnIntakeOrReferralForm,
                    ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
                       
                
                dsd.addColumn("PDC3", "Number of ever enrolled patients total and per intake category",
                    new Mapped(everPatientEnrolledWithRefferralReasonOnIntakeOrReferralFormIndicator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");

                  
//PDC4
              // EncounterCohortDefinition patientWithIntakeVisit=Cohorts.createEncounterBasedOnForms("patientWithIntakeVisit", onOrAfterOnOrBefore, intakeVisitForms);
                //SqlCohortDefinition patientWithIntakeDateOnPDCReferralForm=Cohorts.getPatientsWithObservationValueDateTimeInFormBetweenStartAndEndDate("patientWithIntakeDateOnPDCReferralForm",PDCReferralForm,returnVisitDate);
               
                SqlCohortDefinition patientWithIntakeDateOnPDCReferralForm=new SqlCohortDefinition();
                patientWithIntakeDateOnPDCReferralForm.setName("patientWithIntakeDateOnPDCReferralForm");
                patientWithIntakeDateOnPDCReferralForm.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCReferralForm.getFormId()+" " +
                "and o.concept_id="+returnVisitDate.getConceptId()+" and o.voided=0 and e.voided=0 and o.value_datetime>= :startDate and o.value_datetime<= :endDate");
                patientWithIntakeDateOnPDCReferralForm.addParameter(new Parameter("startDate","startDate",Date.class));
                patientWithIntakeDateOnPDCReferralForm.addParameter(new Parameter("endDate","endDate",Date.class));
                
                
               CompositionCohortDefinition patientWithIntakeVisitAndEnrolledInQuarter = new CompositionCohortDefinition();
               patientWithIntakeVisitAndEnrolledInQuarter.setName("patientWithIntakeVisitAndEnrolledInQuarter");
               patientWithIntakeVisitAndEnrolledInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
               patientWithIntakeVisitAndEnrolledInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
               patientWithIntakeVisitAndEnrolledInQuarter.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate}")));
               patientWithIntakeVisitAndEnrolledInQuarter.getSearches().put("2", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
               patientWithIntakeVisitAndEnrolledInQuarter.getSearches().put("3", new Mapped(patientWithIntakeDateOnPDCReferralForm, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
               patientWithIntakeVisitAndEnrolledInQuarter.setCompositionString("(2 AND (NOT 1)) AND 3");
                          
               CohortIndicator patientWithIntakeVisitAndEnrolledInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number of identified high risk neonates with an intake visit scheduled in the reference quarter that enrolled in the reference quarter indicator", patientWithIntakeVisitAndEnrolledInQuarter,
                   ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                      
               
               dsd.addColumn("PDC4N", "Number of identified high risk neonates with an intake visit scheduled in the reference quarter that enrolled in the reference quarter",
                   new Mapped(patientWithIntakeVisitAndEnrolledInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
       
               
               CohortIndicator patientWithIntakeVisitAndEnrolledInQuarterIndicatorDenominator = Indicators.newCountIndicator("Number of identified high risk neonates with an intake visit scheduled in the reference quarter that enrolled in the reference quarter indicator", patientWithIntakeDateOnPDCReferralForm,
                       ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
                          
                   
                   dsd.addColumn("PDC4D", "Number of identified high risk neonates with their intake visit scheduled in the reference quarter",
                       new Mapped(patientWithIntakeVisitAndEnrolledInQuarterIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
 //PDC5               
                   
                   
                   EncounterCohortDefinition patientWithVisitForm=Cohorts.createEncounterBasedOnForms("patientWithIntakeVisit",PDCVisitForms);
                   
                   ProgramEnrollmentCohortDefinition patientsEverEnrolled=Cohorts.createProgramEnrollment("patientsEverEnrolled", PDCProgram);
                
                   CompositionCohortDefinition patientWithVisitFormEverEnrolled = new CompositionCohortDefinition();
                   patientWithVisitFormEverEnrolled.setName("patientWithVisitFormEverEnrolled");
                   patientWithVisitFormEverEnrolled.addParameter(new Parameter("startDate", "startDate", Date.class));
                   patientWithVisitFormEverEnrolled.addParameter(new Parameter("endDate", "endDate", Date.class));
                   patientWithVisitFormEverEnrolled.getSearches().put("1", new Mapped(patientWithVisitForm, null));
                   patientWithVisitFormEverEnrolled.getSearches().put("2", new Mapped(patientsEverEnrolled, null));
                   patientWithVisitFormEverEnrolled.setCompositionString("1 AND 2");
                              
                   CohortIndicator patientWithVisitFormEverEnrolledIndicatorNumerator = Indicators.newCountIndicator("Number of ever enrolled children who have at least one follow up visit indicator", patientWithVisitFormEverEnrolled,
                       ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                   
                   dsd.addColumn("PDC5N", "Number of ever enrolled children who have at least one follow up visit",
                       new Mapped(patientWithVisitFormEverEnrolledIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
                   CohortIndicator patientsEverEnrolledIndicatorDenominator = Indicators.newCountIndicator("Number of ever enrolled children indicator", patientsEverEnrolled,
                           null);
                              
                       
                   dsd.addColumn("PDC5D", "Number of ever enrolled children",
                           new Mapped(patientsEverEnrolledIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
 
                   
  //PDC6 visits is located up
                   
                   
  /*//PDC7
                   SqlCohortDefinition patientsWithAnySocialSupportAlreadyReceived=Cohorts.getPatientsWithObservationValueDateTimeInFormBetweenStartAndEndDate("patientsWithAnySocialSupport",PDCVisitForm,socialEconomicAssistanceAlreadyReceived);
                   SqlCohortDefinition patientsWithAnySocialSupportRecommanded=Cohorts.getPatientsWithObservationValueDateTimeInFormBetweenStartAndEndDate("patientsWithAnySocialSupport",PDCVisitForm,socialEconomicAssistanceRecommanded);
                   SqlCohortDefinition patientsWithAnySocialSupportNotRecommended=Cohorts.getPatientsWithObservationInFormBetweenStartAndEndDate("patientsWithAnySocialSupport",PDCVisitForms,socialEconomicAssistance,socialAssistanceTypes);
                    */
                   SqlCohortDefinition patientsWithAnySocialSupportRecommended=new SqlCohortDefinition();
                   patientsWithAnySocialSupportRecommended.setName("patientsWithAnySocialSupportRecommended");
                   patientsWithAnySocialSupportRecommended.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCVisitForm.getFormId()+" and o.concept_id="+socialEconomicAssistanceRecommanded.getConceptId()+" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
                   patientsWithAnySocialSupportRecommended.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientsWithAnySocialSupportRecommended.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   SqlCohortDefinition patientsWithAnySocialSupportAlreadyReceived=new SqlCohortDefinition();
                   patientsWithAnySocialSupportAlreadyReceived.setName("patientsWithAnySocialSupportAlreadyReceived");
                   patientsWithAnySocialSupportAlreadyReceived.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCVisitForm.getFormId()+" and o.concept_id="+socialEconomicAssistanceAlreadyReceived.getConceptId()+" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
                   patientsWithAnySocialSupportAlreadyReceived.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientsWithAnySocialSupportAlreadyReceived.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   SqlCohortDefinition patientsWithAnySocialSupportNotRecommended=new SqlCohortDefinition();
                   patientsWithAnySocialSupportNotRecommended.setName("patientsWithAnySocialSupportNotRecommended");
                   patientsWithAnySocialSupportNotRecommended.setQuery("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="+PDCVisitForm.getFormId()+" and o.concept_id="+socialEconomicAssistance.getConceptId()+" and o.value_coded="+socialEconomicAssistanceNotRecommanded+" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
                   patientsWithAnySocialSupportNotRecommended.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientsWithAnySocialSupportNotRecommended.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   CompositionCohortDefinition newPatientsWithAnySocialSupportInQuarter = new CompositionCohortDefinition();
                   newPatientsWithAnySocialSupportInQuarter.setName("newPatientsWithAnySocialSupportInQuarter");
                   newPatientsWithAnySocialSupportInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
                   newPatientsWithAnySocialSupportInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
                   newPatientsWithAnySocialSupportInQuarter.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate}")));
                   newPatientsWithAnySocialSupportInQuarter.getSearches().put("2", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   newPatientsWithAnySocialSupportInQuarter.getSearches().put("3", new Mapped(patientsWithAnySocialSupportNotRecommended, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   newPatientsWithAnySocialSupportInQuarter.getSearches().put("4", new Mapped(patientsWithAnySocialSupportAlreadyReceived, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   newPatientsWithAnySocialSupportInQuarter.getSearches().put("5", new Mapped(patientsWithAnySocialSupportRecommended, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   newPatientsWithAnySocialSupportInQuarter.setCompositionString("(2 AND (NOT 1)) AND (3 OR 4 OR 5)");
                   
                   
                  CohortIndicator newPatientsWithAnySocialSupportInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number of new patients enrolled in the reference quarter who were screened (defined as “already receiving”, “recommended”, or “not recommended” answer on any visit) indicator", 
                		  newPatientsWithAnySocialSupportInQuarter,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                         
                       
                       dsd.addColumn("PDC7N", "Number of new patients enrolled in the reference quarter who were screened (defined as “already receiving”, “recommended”, or “not recommended” answer on any visit)",
                           new Mapped(newPatientsWithAnySocialSupportInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                
                  
                   CompositionCohortDefinition newPatientsInQuarter = new CompositionCohortDefinition();
                   newPatientsInQuarter.setName("newPatientsInQuarter");
                   newPatientsInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
                   newPatientsInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
                   newPatientsInQuarter.getSearches().put("1", new Mapped(inPDCProgramByDate, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate}")));
                   newPatientsInQuarter.getSearches().put("2", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   newPatientsInQuarter.setCompositionString("2 AND (NOT 1)");
                              
                   CohortIndicator newPatientsInQuarterIndicatorDenominator = Indicators.newCountIndicator("Number of new patients enrolled in the reference quarter indicator", newPatientsInQuarter,
                       ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                   
                   dsd.addColumn("PDC7D", "Number of new patients enrolled in the reference quarter",
                       new Mapped(newPatientsInQuarterIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
    //PDC8
                   
                   CompositionCohortDefinition currentlyEnrolledPatientsWithAnySocialSupportInQuarter = new CompositionCohortDefinition();
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.setName("currentlyEnrolledPatientsWithAnySocialSupportInQuarter");
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.getSearches().put("1", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.getSearches().put("2", new Mapped(patientsWithAnySocialSupportAlreadyReceived, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.getSearches().put("3", new Mapped(patientsWithAnySocialSupportRecommended, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   currentlyEnrolledPatientsWithAnySocialSupportInQuarter.setCompositionString("1 AND (2 OR 3)");
                              
                   CohortIndicator currentlyEnrolledPatientsWithAnySocialSupportInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number # of currently enrolled children receiving social support at last visit  (defined as “already receiving” or “recommended and starting today” answer) indicator", currentlyEnrolledPatientsWithAnySocialSupportInQuarter,
                       ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                   
                   dsd.addColumn("PDC8N", "Number of currently enrolled children receiving social support at last visit  (defined as “already receiving” or “recommended and starting today” answer))",
                       new Mapped(currentlyEnrolledPatientsWithAnySocialSupportInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
                             
                   CohortIndicator currentlyEnrolledPatientsIndicatorDenominator = Indicators.newCountIndicator("Number of currently enrolled children  indicator", inPDCProgramByStartDateAndEndDate,
                       ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}"));
                          
                   
                   dsd.addColumn("PDC8D", "Number of currently enrolled children ",
                       new Mapped(currentlyEnrolledPatientsIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
              
  //PDC10
                   
                   SqlCohortDefinition patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit=new SqlCohortDefinition();
                   patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit.setName("patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumented");
                   patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
					+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<180 and p.voided=0"
					+" inner join obs o on lastEnc.patient_id=o.person_id and o.concept_id="+birthWeight.getConceptId()+" and o.voided=0"
					+" inner join obs o2 on lastEnc.patient_id=o2.person_id and o2.concept_id="+intervalGrogth.getConceptId()+" and o2.voided=0"
					+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit.addParameter(new Parameter("endDate","endDate",Date.class));

                   CohortIndicator patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisitIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter with age <6 months at time of visit who have interval growth documented indicator", patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisit,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                   dsd.addColumn("PDC10N", "Number of of children with a visit in the reference quarter with age <6 months at time of visit who have interval growth documented ",
                           new Mapped(patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumentedInLastVisitIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                   
                   SqlCohortDefinition patientUnderSixMonthWithBirthWeightDocumentedInLastVisit=new SqlCohortDefinition();
                   patientUnderSixMonthWithBirthWeightDocumentedInLastVisit.setName("patientUnderSixMonthWithBirthWeightAndIntervalGrowthDocumented");
                   patientUnderSixMonthWithBirthWeightDocumentedInLastVisit.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
					+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<180 and p.voided=0"
					+" inner join obs o on lastEnc.patient_id=o.person_id and o.concept_id="+birthWeight.getConceptId()+" and o.voided=0"
					+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientUnderSixMonthWithBirthWeightDocumentedInLastVisit.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientUnderSixMonthWithBirthWeightDocumentedInLastVisit.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   CohortIndicator patientUnderSixMonthWithBirthWeightDocumentedInLastVisitIndicatorDenominator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter with age <6 months at time of visit who have interval growth documented indicator", patientUnderSixMonthWithBirthWeightDocumentedInLastVisit,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                   dsd.addColumn("PDC10D", "Number of children with a visit in the reference quarter with age <6 months at time of visit who have interval growth documented",
                           new Mapped(patientUnderSixMonthWithBirthWeightDocumentedInLastVisitIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                 
  
 //PDC11
                   
                   SqlCohortDefinition patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit=new SqlCohortDefinition();
                   patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit.setName("patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit");
                   patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit.setQuery("select e.patient_id from encounter e"
					+" inner join person p on e.patient_id=p.person_id and DATEDIFF(e.encounter_datetime,p.birthdate)<180 and p.voided=0"
					+" inner join obs o on e.patient_id=o.person_id and e.encounter_id=o.encounter_id and o.concept_id="+intervalgrowthCoded.getConceptId()+" and o.value_coded="+inadequate.getConceptId()+" and o.voided=0"
					+" where e.form_id="+PDCVisitForm.getFormId()+" and  e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate ");
                   patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   
                   CohortIndicator patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisitIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter with age <6months at the time of visit who have inadequate interval growth indicator", patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisit,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                   dsd.addColumn("PDC11N", "Number of children with a visit in the reference quarter with age <6months at the time of visit who have inadequate interval growth ",
                           new Mapped(patientUnderSixMonthWithInadaquateIntervalGrowthDocumentedInLastVisitIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                   
                               
//PDC14
                   
                   NumericObsCohortDefinition patientWithCorrectedAge=Cohorts.createNumericObsCohortDefinition("patientWithCorrectedAge", correctedAge, 40.0, RangeComparator.LESS_THAN, TimeModifier.LAST);
                   
                   SqlCohortDefinition patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter=new SqlCohortDefinition();
                   patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter.setName("patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter");
                   patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
                	+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<730 and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)>30 and p.voided=0"
                	+" inner join obs o1 on lastEnc.patient_id=o1.person_id and o1.concept_id="+weight.getConceptId()+" and o1.voided=0"
                	+" inner join obs o2 on lastEnc.patient_id=o2.person_id and o2.concept_id="+height.getConceptId()+" and o2.voided=0"
                	+" inner join obs o3 on lastEnc.patient_id=o3.person_id and o3.concept_id="+headCircumference.getConceptId()+" and o3.voided=0"						
                	+" inner join obs o4 on lastEnc.patient_id=o4.person_id and o4.concept_id="+weightForAgeZScore.getConceptId()+" and o4.voided=0"
					+" inner join obs o5 on lastEnc.patient_id=o5.person_id and o5.concept_id="+heightForAgeZScore.getConceptId()+" and o5.voided=0"
					+" inner join obs o6 on lastEnc.patient_id=o6.person_id and o6.concept_id="+HCForAgeZScore.getConceptId()+" and o6.voided=0"
					+" inner join obs o7 on lastEnc.patient_id=o7.person_id and o7.concept_id="+weightForHeightZScore.getConceptId()+" and o7.voided=0"					
					+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   CompositionCohortDefinition patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge = new CompositionCohortDefinition();
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.setName("patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge");
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.addParameter(new Parameter("startDate", "startDate", Date.class));
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.addParameter(new Parameter("endDate", "endDate", Date.class));
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.getSearches().put("1", new Mapped(patientWithCorrectedAge,null));
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.getSearches().put("2", new Mapped(patientWithOneMonthToTwoYearAndWithHeightWeightHCAndZScoresInLastEncounter, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge.setCompositionString("2 AND (NOT 1)");
                   
                   CohortIndicator patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAgeIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter who are  < 2 years and >1 month and no documented corrected age <0 or 40 week at time of encounter who have wt/ht/HC  measured and z scores determined at last appointment", patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                   dsd.addColumn("PDC14N", "Number of children with a visit in the reference quarter who are  < 2 years and >1 month and no documented corrected age <0 or 40 week at time of encounter who have wt/ht/HC  measured and z scores determined at last appointment",
                           new Mapped(patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAgeIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                   
                   
                   SqlCohortDefinition patientWithOneMonthToTwoYearAtLastEncounter=new SqlCohortDefinition();
                   patientWithOneMonthToTwoYearAtLastEncounter.setName("patientWithOneMonthToTwoYearAtLastEncounter");
                   patientWithOneMonthToTwoYearAtLastEncounter.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
                	+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<730 and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)>30 and p.voided=0"
                	+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientWithOneMonthToTwoYearAtLastEncounter.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientWithOneMonthToTwoYearAtLastEncounter.addParameter(new Parameter("endDate","endDate",Date.class));
                  
                   
                   CompositionCohortDefinition patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge = new CompositionCohortDefinition();
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.setName("patientWithweightForAgeZScoreInLastEncounterWithoutCorrectedAge");
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.addParameter(new Parameter("startDate", "startDate", Date.class));
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.addParameter(new Parameter("endDate", "endDate", Date.class));
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.getSearches().put("1", new Mapped(patientWithCorrectedAge,null));
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.getSearches().put("2", new Mapped(patientWithOneMonthToTwoYearAtLastEncounter, ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
                   patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge.setCompositionString("2 AND (NOT 1)");
                   
                   CohortIndicator patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAgeIndicatorDenominator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter who are  < 2 years and >1 month and no documented corrected age <0 or 40 week at time of encounter", patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAge,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                   dsd.addColumn("PDC14D", "Number of children with a visit in the reference quarter who are  < 2 years and >1 month and no documented corrected age <0 or 40 week at time of encounter",
                           new Mapped(patientWithOneMonthToTwoYearAtLastEncounterWithoutCorrectedAgeIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                
//PDC15
                   
                   CodedObsCohortDefinition abnormalCommunication=Cohorts.createCodedObsCohortDefinition("abnormalCommunication", onOrAfterOnOrBefore, communication, abnormal, SetComparator.IN, TimeModifier.LAST);
                   
                   CodedObsCohortDefinition abnormalLargeMuscleMovement=Cohorts.createCodedObsCohortDefinition("abnormalLargeMuscleMovement", onOrAfterOnOrBefore, largeMuscleMovement, abnormal, SetComparator.IN, TimeModifier.LAST);
                   
                   CodedObsCohortDefinition abnormalSmallMuscleMovement=Cohorts.createCodedObsCohortDefinition("abnormalSmallMuscleMovement", onOrAfterOnOrBefore, smallMuscleMovement, abnormal, SetComparator.IN, TimeModifier.LAST);

                   CodedObsCohortDefinition abnormalProblemSolving=Cohorts.createCodedObsCohortDefinition("abnormalProblemSolving", onOrAfterOnOrBefore, problemSolving, abnormal, SetComparator.IN, TimeModifier.LAST);

                   CodedObsCohortDefinition abnormalPersonalSocial=Cohorts.createCodedObsCohortDefinition("abnormalPersonalSocial", onOrAfterOnOrBefore, personalSocial, abnormal, SetComparator.IN, TimeModifier.LAST);

                   CodedObsCohortDefinition ASQAgeUsedCohort=Cohorts.createCodedObsCohortDefinition("ASQAgeUsedCohort",onOrAfterOnOrBefore, ASQAgeUsed, null, SetComparator.IN, TimeModifier.LAST);
        
                   
                   CompositionCohortDefinition patientWithASQAndGrayBlackInQuarter = new CompositionCohortDefinition();
                   patientWithASQAndGrayBlackInQuarter.setName("patientWithASQAndGrayBlackInQuarter");
                   patientWithASQAndGrayBlackInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
                   patientWithASQAndGrayBlackInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("1", new Mapped(ASQAgeUsedCohort, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("2", new Mapped(abnormalCommunication, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("3", new Mapped(abnormalLargeMuscleMovement, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("4", new Mapped(abnormalSmallMuscleMovement, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("5", new Mapped(abnormalProblemSolving, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.getSearches().put("6", new Mapped(abnormalPersonalSocial, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   patientWithASQAndGrayBlackInQuarter.setCompositionString("1 AND (2 OR 3 OR 4 OR 5 OR 6)");
                              
                   CohortIndicator patientWithASQAndGrayBlackInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number of children with a completed and documented ASQ in the reference quarter scoring with borderline or concern noted (gray/black) in at least one domain  indicator", patientWithASQAndGrayBlackInQuarter,
                       ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                   
                   dsd.addColumn("PDC15N", "Number of children with a completed and documented ASQ in the reference quarter scoring with borderline or concern noted (gray/black) in at least one domain",
                       new Mapped(patientWithASQAndGrayBlackInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
                   CohortIndicator patientWithASQInQuarterIndicatorDenominator = Indicators.newCountIndicator("Number of children with an ASQ completed and documented in the reference quarter Indicator", ASQAgeUsedCohort,
                           ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}"));
                       
                   dsd.addColumn("PDC15D", "Number of children with an ASQ completed and documented in the reference quarter",
                           new Mapped(patientWithASQInQuarterIndicatorDenominator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                           
//PDC16
                   
                   SqlCohortDefinition patientBetween6And12MonthsWithASQDocumentedInLastEncounter=new SqlCohortDefinition();
                   patientBetween6And12MonthsWithASQDocumentedInLastEncounter.setName("patientBetween6And12MonthsWithASQDocumentedInLastEncounter");
                   patientBetween6And12MonthsWithASQDocumentedInLastEncounter.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
                	+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<=365 and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)>180 and p.voided=0"
                	+" inner join obs o1 on lastEnc.patient_id=o1.person_id and o1.concept_id="+ASQAgeUsed.getConceptId()+" and o1.voided=0"
                	+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientBetween6And12MonthsWithASQDocumentedInLastEncounter.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientBetween6And12MonthsWithASQDocumentedInLastEncounter.addParameter(new Parameter("endDate","endDate",Date.class));
                      
                   SqlCohortDefinition patientBetween6And12MonthsInLastEncounter=new SqlCohortDefinition();
                   patientBetween6And12MonthsInLastEncounter.setName("patientBetween6And12MonthsWithASQDocumentedInLastEncounter");
                   patientBetween6And12MonthsInLastEncounter.setQuery("select lastEnc.patient_id from (select * from (select * from encounter where form_id="+PDCVisitForm.getFormId()+" and voided=0 order by encounter_datetime desc) as orderedEnc group by orderedEnc.encounter_id) as lastEnc"
                	+" inner join person p on lastEnc.patient_id=p.person_id and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)<=365 and DATEDIFF(lastEnc.encounter_datetime,p.birthdate)>180 and p.voided=0"
                	+" where lastEnc.encounter_datetime >= :startDate and lastEnc.encounter_datetime <= :endDate");
                   patientBetween6And12MonthsInLastEncounter.addParameter(new Parameter("startDate","startDate",Date.class));
                   patientBetween6And12MonthsInLastEncounter.addParameter(new Parameter("endDate","endDate",Date.class));
                   
                   
                   CohortIndicator patientBetween6And12MonthsWithASQDocumentedInLastEncounterIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter who were between 6 and 12 months of age at encounter with at least 1 ASQ score documented indicator", patientBetween6And12MonthsWithASQDocumentedInLastEncounter,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                       
                   dsd.addColumn("PDC16N", "Number of children with a visit in the reference quarter who were between 6 and 12 months of age at encounter with at least 1 ASQ score documented",
                           new Mapped(patientBetween6And12MonthsWithASQDocumentedInLastEncounterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                   
                   
                   CohortIndicator patientBetween6And12MonthsInLastEncounterIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter who were between 6 and 12 months of age at encounter indicator", patientBetween6And12MonthsInLastEncounter,
                           ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                              
                       
                   dsd.addColumn("PDC16D", "Number of children with a visit in the reference quarter who were between 6 and 12 months of age at encounter",
                           new Mapped(patientBetween6And12MonthsInLastEncounterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
                   
//PDC17
                   
                   //EncounterCohortDefinition  patientWithVisitInQuarter=Cohorts.createEncounterBasedOnForms("patientWithVisitInQuarter", onOrAfterOnOrBefore, PDCVisitForms);
                   
                   //CodedObsCohortDefinition patientsWithECDCounseling=Cohorts.createCodedObsCohortDefinition("patientsWithECDCounseling", ECDCounselingSession,yes, SetComparator.IN, TimeModifier.ANY);
                   SqlCohortDefinition patientsWithECDCounseling=new SqlCohortDefinition();
                   patientsWithECDCounseling.setName("patientsWithECDCounseling");
                   patientsWithECDCounseling.setQuery("select distinct o.person_id from obs o where o.concept_id="+ECDCounselingSession.getConceptId()+" and o.value_coded="+yes.getConceptId()+" " +
                   		"and o.voided=0 order by o.obs_datetime desc");
                   
                   CompositionCohortDefinition currentlyEnrolledPatientsWithAnyECDCounselingInQuarter = new CompositionCohortDefinition();
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.setName("currentlyEnrolledPatientsWithAnyECDCounselingInQuarter");
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.addParameter(new Parameter("startDate", "startDate", Date.class));
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.addParameter(new Parameter("endDate", "endDate", Date.class));
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.getSearches().put("1", new Mapped(inPDCProgramByStartDateAndEndDate, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.getSearches().put("2", new Mapped(patientWithVisitInQuarter, ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.getSearches().put("3", new Mapped(patientsWithECDCounseling, null));
                   currentlyEnrolledPatientsWithAnyECDCounselingInQuarter.setCompositionString("1 AND 2 AND 3");
                              
                   CohortIndicator currentlyEnrolledPatientsWithAnyECDCounselingInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number of children with a visit in the reference quarter whose caregivers have had at least one documented ECD counseling session Indicator", currentlyEnrolledPatientsWithAnyECDCounselingInQuarter,
                       ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
                          
                   
                   dsd.addColumn("PDC17N", "Number of currently enrolled children receiving social support at last visit  (defined as “already receiving” or “recommended and starting today” answer",
                       new Mapped(currentlyEnrolledPatientsWithAnyECDCounselingInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
           
                    
                   CohortIndicator patientsWithVisitInQuarterIndicatorNumerator = Indicators.newCountIndicator("Number of children who had a visit in the reference quarter Indicator", patientWithVisitInQuarter,
                           ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}"));
                              
                       
                       dsd.addColumn("PDC17D", "Number of children who had a visit in the reference quarter",
                           new Mapped(patientsWithVisitInQuarterIndicatorNumerator, ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
               
        
        }
        
        private void setUpProperties() {
                
                onOrAfterOnOrBefore.add("onOrAfter");
                onOrAfterOnOrBefore.add("onOrBefore");
                
                PDCProgram = gp.getProgram(GlobalPropertiesManagement.PDC_PROGRAM);
                	//Context.getProgramWorkflowService().getProgramByName("PDC Program");               
                
                
                reasoForReferral=gp.getConcept(GlobalPropertiesManagement.REASON_FOR_REFERRAL);
                	//Context.getConceptService().getConceptByUuid("1aa373f4-4db5-4b01-bce0-c10a636bb931");
                
                PDCIntakeForm=gp.getForm(GlobalPropertiesManagement.PDC_INTAKE_FORM);
                	//Context.getFormService().getForm("PDC Intake Visit");
                
                PDCReferralForm=gp.getForm(GlobalPropertiesManagement.PDC_REFERRAL_FORM);
                	//Context.getFormService().getForm("PDC Referral Form");
                
                PDCVisitForm=gp.getForm(GlobalPropertiesManagement.PDC_VISIT_FORM);
                	
                	//Context.getFormService().getForm("PDC Visit Form");
                
                intakeVisitForms.add(PDCIntakeForm);
                                
                PDCVisitForms.add(PDCVisitForm);
                
                returnVisitDate=gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
                
                
                
                
               /* 8199
                UUID 	d8021d30-364a-4833-a1b7-e65eca46c66e
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	BREATHING DANGER SIGNS PRESENT
               */ 
                
                breathingDangerSignsPresent=gp.getConcept(GlobalPropertiesManagement.BREATHING_DANGER_SIGNS_PRESENT);
                	//Context.getConceptService().getConcept(8199);
                
                
                /*Id 	8200
                UUID 	ec7de7cf-055f-48ac-897a-f8c789a7467b
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	CONVULSIONS DANGER SIGNS PRESENT*/
                
                
                convulsionsDangerSignsPresent=gp.getConcept(GlobalPropertiesManagement.CONVULSIONS_DANGER_SIGNS_PRESENT);
                	//Context.getConceptService().getConcept(8200);

                
               /* Id 	8201
                UUID 	ffb87bc8-3121-4171-a151-b4c08e315521
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	LETHARGY OR UNRESPONSIVENESS DANGER SIGNS PRESENT*/
                
                
                LethargyOrUnresponsivenessDangerSignsPresent=gp.getConcept(GlobalPropertiesManagement.LETHARGY_OR_UNRESPONSIVENESS_DANGER_SIGNS_PRESENT);
                	//Context.getConceptService().getConcept(8201);

                
                
               /* 
                Id 	8202
                UUID 	62a031af-350d-4b08-9cde-6d2b87a5b0b1
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	UMBILICAL CORD REDNESS DANGER SIGNS
                Synonyms */
                
                umbilicalCordRednessDangerSigns=gp.getConcept(GlobalPropertiesManagement.UMBILICAL_CORD_REDNESS_DANGER_SIGNS);
                	//Context.getConceptService().getConcept(8202);

                
               /* Id 	8203
                UUID 	4509cb04-9adf-4141-941e-d81d4377111f
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	STIFF NECK OR BULGING FONTANELLES DANGER SIGNS PRESENT*/
                
                stiffNeckOrBulgingFontanellesDangerSigns=gp.getConcept(GlobalPropertiesManagement.STIFF_NECK_OR_BULGING_FONTANELLES_DANGER_SIGNS_PRESENT);
                	//Context.getConceptService().getConcept(8203);

                
              
               /* Id 	8214
                UUID 	1fca6d51-255f-493d-a281-7521b384a623
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	AGE USED*/
                
                ASQAgeUsed=gp.getConcept(GlobalPropertiesManagement.ASQ_SCORE);
                	//Context.getConceptService().getConcept(8214);

                
              //numeric
                
               /* Id 	6984
                UUID 	6d4b1720-f45c-4069-98d9-e7e865105a7a
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	Height for age z-score
              */  
                // heightForAgeZScore=Context.getConceptService().getConcept(6984);

             /*   
                Id 	5088
                UUID 	3ce939d2-26fe-102b-80cb-0017a47871b2
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	TEMPERATURE (C)
                
              */
                 temperature=gp.getConcept(GlobalPropertiesManagement.TEMPERATURE);
                	 //Context.getConceptService().getConcept(5088);
 
                 
               /*
                
                Id 	5242
                UUID 	3ceb11f8-26fe-102b-80cb-0017a47871b2
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	RESPIRATORY RATE*/
                
                respiratoryRate=gp.getConcept(GlobalPropertiesManagement.RESPIRATORY_RATE);
                	
                	//Context.getConceptService().getConcept(5242);                
                
                
                yes=gp.getConcept(GlobalPropertiesManagement.YES);
                	//Context.getConceptService().getConcept(1065);
                
                socialEconomicAssistance=gp.getConcept(GlobalPropertiesManagement.SOCIAL_WORK_ASSESSMENT);
                	
                	//Context.getConceptService().getConceptByUuid("3cda0462-26fe-102b-80cb-0017a47871b2");
                
                socialEconomicAssistanceAlreadyReceived=gp.getConcept(GlobalPropertiesManagement.SOCIAL_ECONOMIC_ASSISTANCE_ALREADY_RECEIVED);
                	
                	//Context.getConceptService().getConceptByUuid("3ce169b4-26fe-102b-80cb-0017a47871b2");
                
                socialEconomicAssistanceRecommanded=gp.getConcept(GlobalPropertiesManagement.SOCIAL__ECONOMIC_ASSISTANCE_RECOMMANDED);
                	//Context.getConceptService().getConceptByUuid("3ce16b30-26fe-102b-80cb-0017a47871b2");
                
                socialEconomicAssistanceNotRecommanded=gp.getConcept(GlobalPropertiesManagement.SOCIAL__ECONOMIC_ASSISTANCE_NOT_RECOMMANDED);
                	//Context.getConceptService().getConceptByUuid("ae866472-b781-44ee-be66-bb86c42057cc");
                
                /*socialAssistanceTypes.add(socialEconomicAssistanceAlreadyReceived);
                socialAssistanceTypes.add(socialEconomicAssistanceRecommanded);
                */socialAssistanceTypes.add(socialEconomicAssistanceNotRecommanded);
                
                
                
                /* Id 	8228
                UUID 	ceb6b7b2-f26d-4f7b-92ae-9b0c1df86aa4
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	INTERVAL GROWTH
                
                
                
                Id 	5916
                UUID 	3cf15b3a-26fe-102b-80cb-0017a47871b2
                Locale 	English | French | Kinyarwanda
                Fully Specified Name 	BIRTH WEIGHT*/
                
                intervalGrogth=gp.getConcept(GlobalPropertiesManagement.INTERVAL_GROWTH);
                	//Context.getConceptService().getConceptByUuid("ceb6b7b2-f26d-4f7b-92ae-9b0c1df86aa4");
                
                birthWeight=gp.getConcept(GlobalPropertiesManagement.BIRTH_WEIGHT);
                	//Context.getConceptService().getConceptByUuid("3cf15b3a-26fe-102b-80cb-0017a47871b2");
                
                correctedAge=gp.getConcept(GlobalPropertiesManagement.CORRECTED_AGE);
                	//Context.getConceptService().getConceptByUuid("5ed677a7-ecfe-4d27-9d19-8c45d89ed6db");
              
              
             /* @WEIGHT_FOR_AGE_Z_SCORE@=c9d8c9db-518c-4a00-985d-d2fc8ac1f739
              @HEIGHT_FOR_AGE_Z_SCORE@=6d4b1720-f45c-4069-98d9-e7e865105a7a              
              @HC_FOR_AGE_Z_SCORE@=9f3180c5-20ce-479f-9276-63d773765c28
              
              @HEAD_CIRCUMFERENCE@=3ceb96b4-26fe-102b-80cb-0017a47871b2
              @WEIGHT@=3ce93b62-26fe-102b-80cb-0017a47871b2
    			@HEIGHT@=3ce93cf2-26fe-102b-80cb-0017a47871b2
              @WEIGHT_FOR_HEIGHT_Z_SCORE@=2438e295-d9da-4d5c-bacc-13c313e34755
                */
              
              weightForAgeZScore=gp.getConcept(GlobalPropertiesManagement.WTAGEZScore);
              
            	  
            	  //Context.getConceptService().getConceptByUuid("c9d8c9db-518c-4a00-985d-d2fc8ac1f739");
              weightForHeightZScore=gp.getConcept(GlobalPropertiesManagement.WTHEIGHTZScore);
            	  //Context.getConceptService().getConceptByUuid("2438e295-d9da-4d5c-bacc-13c313e34755");
              heightForAgeZScore=gp.getConcept(GlobalPropertiesManagement.HEIGHT_FOR_AGE_Z_SCORE);
            	  //Context.getConceptService().getConceptByUuid("6d4b1720-f45c-4069-98d9-e7e865105a7a");
              HCForAgeZScore=gp.getConcept(GlobalPropertiesManagement.HC_FOR_AGE_ZSCORE);
            	  //Context.getConceptService().getConceptByUuid("9f3180c5-20ce-479f-9276-63d773765c28");
              headCircumference=gp.getConcept(GlobalPropertiesManagement.HEAD_CIRCUMFERENCE);
            	  //Context.getConceptService().getConceptByUuid("3ceb96b4-26fe-102b-80cb-0017a47871b2");
              weight=gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);
            	  //Context.getConceptService().getConceptByUuid("3ce93b62-26fe-102b-80cb-0017a47871b2");
              height=gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT);
            	  //Context.getConceptService().getConceptByUuid("3ce93cf2-26fe-102b-80cb-0017a47871b2");
              
              
              
             /* 8217
              UUID 	d0fdea6f-37f7-4a9c-9f95-488b9b31c6de
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	SMALL MUSCLE MOVEMENTS
              
              
              Id 	8216
              UUID 	497c7a76-1756-44b2-9fc9-c5b131004959
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	LARGE MUSCLE MOVEMENTS
              
              
              Id 	8215
              UUID 	1fc5e9a8-e6c2-48c4-b6ed-9b21d733dc01
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	COMMUNICATION
              
              
              Id 	8218
              UUID 	b53ff26a-515c-4a7c-bebe-b80c594e6d4c
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	PROBLEM SOLVING
              
              
              Id 	8219
              UUID 	fda3b1a8-7e8f-4ae9-973a-f8c41713c595
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	PERSONAL-SOCIAL
              
              
              Id 	1116
              UUID 	3cd75230-26fe-102b-80cb-0017a47871b2
              Locale 	English | French | Kinyarwanda
              Fully Specified Name 	ABNORMAL*/
              
              smallMuscleMovement=gp.getConcept(GlobalPropertiesManagement.SMALL_MUSCLE_MOVEMENTS);
            	  //Context.getConceptService().getConceptByUuid("d0fdea6f-37f7-4a9c-9f95-488b9b31c6de");
              largeMuscleMovement=gp.getConcept(GlobalPropertiesManagement.LARGE_MUSCLE_MOVEMENTS);
            	  //Context.getConceptService().getConceptByUuid("497c7a76-1756-44b2-9fc9-c5b131004959");
              communication=gp.getConcept(GlobalPropertiesManagement.COMMUNICATION);
            	  //Context.getConceptService().getConceptByUuid("1fc5e9a8-e6c2-48c4-b6ed-9b21d733dc01");
              problemSolving=gp.getConcept(GlobalPropertiesManagement.PROBLEM_SOLVING);
            	  //Context.getConceptService().getConceptByUuid("b53ff26a-515c-4a7c-bebe-b80c594e6d4c");
              personalSocial=gp.getConcept(GlobalPropertiesManagement.PERSONAL_SOCIAL);
            	  //Context.getConceptService().getConceptByUuid("fda3b1a8-7e8f-4ae9-973a-f8c41713c595");
              abnormal=gp.getConcept(GlobalPropertiesManagement.ABNORMAL);
            	  //Context.getConceptService().getConceptByUuid("3cd75230-26fe-102b-80cb-0017a47871b2");
              
              /*@RECEIVED_AT_LEAST_ONE_ECD_EDUCATION_SESSION@=082c3027-9875-4b1f-8d11-49640f096107*/
              
              ECDCounselingSession=gp.getConcept(GlobalPropertiesManagement.ECD_EDUCATION);
            	  //Context.getConceptService().getConceptByUuid("082c3027-9875-4b1f-8d11-49640f096107");
              
              
              /*@REASON_FOR_NOT_DOING_FOLLOW_UP@=f20d0cf4-2b39-4e4e-af91-628b95938f87*/
              
              reasonForNotDoingFollowUp=gp.getConcept(GlobalPropertiesManagement.REASON_FOR_NOT_DOING_FOLLOWUP);
            	  //Context.getConceptService().getConceptByUuid("f20d0cf4-2b39-4e4e-af91-628b95938f87");
              
              intervalgrowthCoded=gp.getConcept(GlobalPropertiesManagement.INTERVAL_GROWTH_CODED);
              inadequate=gp.getConcept(GlobalPropertiesManagement.INTERVAL_GROWTH_INADEQUATE);
              
        }
}