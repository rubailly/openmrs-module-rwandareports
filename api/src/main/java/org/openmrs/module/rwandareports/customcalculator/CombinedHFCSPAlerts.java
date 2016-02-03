package org.openmrs.module.rwandareports.customcalculator;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculation;
import org.openmrs.module.rowperpatientreports.patientdata.result.AgeResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.DateValueResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.ObservationResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.PatientAttributeResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.PatientDataResult;
import org.openmrs.module.rwandareports.definition.result.EvaluateMotherDefinitionResult;

public class CombinedHFCSPAlerts implements CustomCalculation{

	protected Log log = LogFactory.getLog(this.getClass());
	
	public PatientDataResult calculateResult(List<PatientDataResult> results, EvaluationContext context) {
		
		PatientAttributeResult alert = new PatientAttributeResult(null, null);
		
		StringBuffer alerts = new StringBuffer();
		
		Integer last13WeekAge = null;
		double heightObs=0;
		double weightObs=0;
		
		
		for(PatientDataResult result: results)
		{
			
			if(result.getName().equals("viralLoadTest"))
			{
				EvaluateMotherDefinitionResult viraload = (EvaluateMotherDefinitionResult)result;
				
				if(viraload.getValue() != null)
				{
					PatientDataResult lastviraload = null;
					String name=viraload.getValueAsObs();
					String obsId=null;
					String obsDate=null;

					if(viraload.getValue().size() > 0)
					{
						try{
						lastviraload = viraload.getValue().get(viraload.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						obsId = valueSplited[0];
						obsDate = valueSplited[1];
						System.out.println("=======obsId====="+obsId+ "====patient data result===="+result.getPatientData().getPatient().getPatientId());
						System.out.println("======obsDate test me===="+obsDate);
					    Date dateVl=null;
						try{
							 SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd");
							 dateVl =format.parse(obsDate);
						}
						catch(Exception e){}
							Date date = Calendar.getInstance().getTime();
							int diff = calculateMonthsDifference(date, dateVl);
							if(diff > 12){
								alerts.append("Late VL(" + diff + " months ago).\n");
							}
							int viraloadObs=Integer.parseInt(obsId);
							if(viraload.getValue().size() > 0 && viraloadObs > 1000)
							{
								alerts.append("VL Failure "+viraloadObs+".\n");
							}
						}catch(ArrayIndexOutOfBoundsException e){}
					  }
					else if(lastviraload == null)
					{
						alerts.append("No VL recorded.\n");
					}
				  }	
			    }
			
			if(result.getName().equals("motherCD4"))
			{
				EvaluateMotherDefinitionResult cd4test = (EvaluateMotherDefinitionResult)result;
				
				if(cd4test.getValue() != null)
				{
					PatientDataResult lastcd4 = null;
					String name=cd4test.getValueAsObs();
					String cd4Id=null;
					String cd4Date=null;

					if(cd4test.getValue().size() > 0)
					{
						try{
						lastcd4 = cd4test.getValue().get(cd4test.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						cd4Id = valueSplited[0];
						cd4Date = valueSplited[1];
						Date dateVl=null;
						try{
							 SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd");
							 dateVl =format.parse(cd4Date);
							
						}
						catch(Exception e){}
							Date date = Calendar.getInstance().getTime();
							int diff = calculateMonthsDifference(date, dateVl);
							if(diff > 12){
								alerts.append("Very late CD4(" + diff + " months ago).\n");
							}
					       }catch(Exception e){}
						 }
					else if(lastcd4 == null)
					{
						alerts.append("No CD4 recorded.\n");
					}
				  }	
			    }
			
			if(result.getName().equals("allCd4"))
			{
				EvaluateMotherDefinitionResult allCd4 = (EvaluateMotherDefinitionResult)result;
				
				if(allCd4.getValue() != null)
				{
					PatientDataResult lastCd4 = null;
					String name=allCd4.getValueAsObs();
					String cd41Filter=null;
					String cd41FilterDate=null;
					String cd42Filter=null;
					String cd42FilterDate=null;
					
					if(allCd4.getValue().size() > 0)
					{
						try{
						lastCd4 = allCd4.getValue().get(allCd4.getValue().size()-1);
						//try{
							String [] valueSplited=name.split(" ");
							
							cd41Filter=valueSplited[0];
							cd41FilterDate=valueSplited[1];
							
							cd42Filter=valueSplited[2];
							cd42FilterDate=valueSplited[3];	
							if(valueSplited.length >=4){
							double firstcd4=Double.parseDouble(cd41Filter);
							double secondCd4=Double.parseDouble(cd42Filter);
							int ncd4=(int) firstcd4;
							int ncd42=(int) secondCd4;
							List<Integer> cd4Counts=new ArrayList<Integer>();
							cd4Counts.add(ncd4);
							cd4Counts.add(ncd42);
							int decline = calculateDecline(cd4Counts);
							if(decline > 50 )
							{
								alerts.append("CD4 decline(");
								alerts.append(decline);
								alerts.append(").\n");
							}
							}
						    }
							catch(Exception e){}
						 }
				     }	
			    }
			
			if(result.getName().equals("allWeight"))
			{
				EvaluateMotherDefinitionResult allweight = (EvaluateMotherDefinitionResult)result;
				
				if(allweight.getValue() != null)
				{
					PatientDataResult lastCd4 = null;
					String name=allweight.getValueAsObs();
					String weight1Filter=null;
					String weight1Date=null;
					String weight2Filter=null;
					String weight2Date=null;
					
					//System.out.println("All CD4: "+name);
					
					if(allweight.getValue().size() > 0)
					{
						lastCd4 = allweight.getValue().get(allweight.getValue().size()-1);
						try{
							String [] valueSplited=name.split(" ");
							
							weight1Filter=valueSplited[0];
							weight1Date=valueSplited[1];
							
							weight2Filter=valueSplited[2];
							weight2Date=valueSplited[3];
								if(valueSplited.length >=4){
							double firstWeight=Double.parseDouble(weight1Filter);
							double secondWeight=Double.parseDouble(weight2Filter);
							
							int nweight1=(int) firstWeight;
							int nweight2=(int) secondWeight;
							
							List<Integer> weightValues=new ArrayList<Integer>();
							weightValues.add(nweight1);
							weightValues.add(nweight2);
							
							List<Double> weightList=new ArrayList<Double>();
							weightList.add(firstWeight);
							weightList.add(secondWeight);
							
							int decline = calculatePercentageDecline(weightList);
							
							if(decline > 5)
							{
								alerts.append("WT decline (");
								alerts.append(decline);
								alerts.append("%, ");
								int kilosLost = calculateDecline(weightValues);
								alerts.append(kilosLost);
								alerts.append("kg)\n");
							}
							}
						    }
							catch(Exception e){}
						 }
				     }	
			    }
			
			
			
			if(result.getName().equals("recentWeight"))
			{
				EvaluateMotherDefinitionResult weight = (EvaluateMotherDefinitionResult)result;
				
				if(weight.getValue() != null)
				{
					PatientDataResult lastweight = null;
					String name=weight.getValueAsObs();
					String weightId=null;
					String weightDate=null;

					if(weight.getValue().size() > 0)
					{
						try{
						lastweight = weight.getValue().get(weight.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						weightId = valueSplited[0];
						weightDate = valueSplited[1];
					    weightObs=Double.parseDouble(weightId);
					 }catch(Exception e){}
						}
					else if(lastweight == null)
					{
						alerts.append("No Weight recorded.\n");
					}
				  }	
			    }
			
			if(result.getName().equals("recentHeight"))
			{
				EvaluateMotherDefinitionResult height = (EvaluateMotherDefinitionResult)result;
				
				if(height.getValue() != null)
				{
					PatientDataResult lastHeight = null;
					String name=height.getValueAsObs();
					String heightId=null;
					String heightDate=null;

					if(height.getValue().size() > 0)
					{
						try{
						lastHeight = height.getValue().get(height.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						heightId = valueSplited[0];
						heightDate = valueSplited[1];
						heightObs=Double.parseDouble(heightId);		
					if(heightObs > 0 && weightObs > 0)
					{
						double bmi = weightObs/(heightObs/100*heightObs/100);
						int decimalPlace = 1;
						BigDecimal bd = new BigDecimal( Double.toString(bmi) );
						bd = bd.setScale( decimalPlace, BigDecimal.ROUND_HALF_UP );
						
						if(bmi < 16)
						{
							alerts.append("Very low BMI (" + bd.doubleValue()  + ").\n");
						}
						else if(bmi < 18.5)
						{
							alerts.append("Low BMI (" + bd.doubleValue()  + ").\n");
						}
					  }	
					}catch(Exception e){}
					}
					
					else if(lastHeight == null)
					{
						alerts.append("No Height recorded.\n");
					}
				  }	
			    }
			
			if(result.getName().equals("OI"))
			{
				EvaluateMotherDefinitionResult oiConcept = (EvaluateMotherDefinitionResult)result;
				
				if(oiConcept.getValue() != null)
				{
					PatientDataResult lastOi = null;
					String name=oiConcept.getValueAsObs();
					String oiInfection=null;
					String oiDate=null;
					
					if(oiConcept.getValue().size() > 0)
					{
						try{
						lastOi = oiConcept.getValue().get(oiConcept.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						oiInfection = valueSplited[0];
						oiDate = valueSplited[1];
						alerts.append("OI reported last visit: " + oiInfection + "\n");
					}catch(Exception e){}
						}
					}	
			    }
			if(result.getName().equals("SideEffects"))
			{
				EvaluateMotherDefinitionResult sideEffect = (EvaluateMotherDefinitionResult)result;
				
				if(sideEffect.getValue() != null)
				{
					PatientDataResult lastsideEffect = null;
					String name=sideEffect.getValueAsObs();
					String sideffectConc=null;
					String sideffectConcDate=null;
					
					if(sideEffect.getValue().size() > 0)
					{
						try{
						lastsideEffect = sideEffect.getValue().get(sideEffect.getValue().size()-1);
						String [] valueSplited=name.split(" ");
						sideffectConc = valueSplited[0];
						sideffectConcDate = valueSplited[1];
					    alerts.append("Side Effects reported last visit: " + sideffectConc + "\n");
					}catch(Exception e){}
					}
				  }	
			    }			
			if(result.getName().equals("ageinMonths"))
	         {
			AgeResult dbsduremorethan13Weeks = (AgeResult)result;
			if(dbsduremorethan13Weeks.getValue() >= 3.5){
				last13WeekAge=dbsduremorethan13Weeks.getValue();
				}
	         }
			if(result.getName().equals("dbsRecorded")){
			ObservationResult dbstest = (ObservationResult)result;
			try{
			if(dbstest.getValue()==null && last13WeekAge > 3.5){
				alerts.append("DBS Due \n");
			 }
			}catch(Exception e){}
		    }
			
			if(result.getName().equals("serotestRecorded"))
	         {
			ObservationResult serotest = (ObservationResult)result;
			try{
			if(serotest.getValue() == null && last13WeekAge >= 10){
				alerts.append("Serotest Due \n");
			 }
			}catch(Exception e){}
		    }
		
				if(result.getName().equals("lastEncInMonth")){
				DateValueResult encinmonths = (DateValueResult)result;
				if(encinmonths.getValue() != null){
				Date dateVl =encinmonths.getDateOfObservation();
				Date date = Calendar.getInstance().getTime();
						
						int diff = calculateMonthsDifference(date, dateVl);
						if(diff > 12){
							alerts.append("LTFU determine status.\n");
					     }
					  } 	
			       }
		      }
		
		
		alert.setValue(alerts.toString().trim());
		return alert;
	}
	
	private int calculateMonthsDifference(Date observation, Date startingDate)
	{
		int diff = 0;
	
		Calendar obsDate = Calendar.getInstance();	
		obsDate.setTime(observation);
	
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(startingDate);
	    diff = obsDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR);
		diff = diff * 12;
	
		int monthDiff = obsDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH);
		diff = diff + monthDiff;
	
		return diff;
	}
	
	
	private int calculateDecline(List<Integer> obs)
	{
		int lastOb = 0;
		int nextToLastOb = 0;
		
		if(obs.size() > 0)
		{
			lastOb = obs.get(obs.size() - 1);
		}
		
		if(obs.size() > 1)
		{
			nextToLastOb = obs.get(obs.size() - 2);
		}
		if(lastOb > 0 && nextToLastOb > 0)
		{
			int firstVal = lastOb;
			int nextToLastVal = nextToLastOb;
			if(firstVal > 0 && nextToLastVal > 0)
			{
				int decline = nextToLastVal - firstVal;
				if(decline > 0)
				{
					return (int)decline;
				}
			 } 
		  }
		
		return 0;
	}
	
	private int calculatePercentageDecline(List<Double> obs)
	{
		double lastOb = 0;
		double nextToLastOb = 0;
		
		if(obs.size() > 0)
		{
			lastOb = obs.get(obs.size() - 1);
		}
		
		if(obs.size() > 1)
		{
			nextToLastOb = obs.get(obs.size() - 2);
		}
		
		if(lastOb > 0 && nextToLastOb > 0)
		{
			double firstVal = lastOb;
			double nextToLastVal = nextToLastOb;
			if(firstVal > 0 && nextToLastVal > 0)
			{
				double decline = 100 - ((firstVal / nextToLastVal)*100);
				if(decline > 0)
				{
					return (int)decline;
				}
			}
		}
		
		return 0;
	}
}