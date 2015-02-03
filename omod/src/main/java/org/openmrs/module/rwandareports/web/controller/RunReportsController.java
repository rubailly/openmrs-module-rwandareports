package org.openmrs.module.rwandareports.web.controller;

import org.apache.commons.io.IOUtils;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.dataset.MapDataSet;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.Report;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.ReportRequest.Priority;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandareports.reporting.Helper;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RunReportsController {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
    @RequestMapping("/module/rwandaReports/printReport.form") 
    public void viewIndex(ModelMap model, 
                            @RequestParam(value="report", required=true) String report,
                            @RequestParam(value="parameters", required=true) String parameters,
                            HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	ReportService rs = Context.getService(ReportService.class);
		
		ReportRequest rr = new ReportRequest();
		
		ReportDefinition reportDef = Helper.findReportDefinition(report);
		
		String[] paramNames = parameters.split(",");
		
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		for (String param: paramNames) {
			params.put(param, request.getParameter(param));			
		}
		
		rr.setReportDefinition(new Mapped<ReportDefinition>(reportDef, params));
		
		List<RenderingMode> modes = rs.getRenderingModes(reportDef);
		RenderingMode rm = modes.get(0);
		
	    rr.setRenderingMode(rm);
	    rr.setPriority(Priority.HIGHEST);
		
		Report rep = rs.runReport(rr);
		
		String filename = rm.getRenderer().getFilename(rep.getRequest());
		response.setContentType(rm.getRenderer().getRenderedContentType(rep.getRequest()));
		byte[] data = rep.getRenderedOutput();
		
		if (data != null) {
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
			response.setHeader("Pragma", "no-cache");
			IOUtils.write(data, response.getOutputStream());
		}
		else {
			response.getWriter().write("There was an error retrieving the report");
		}
		
    }
    
    @RequestMapping("/module/rwandaReports/printReportAndRegister.form") 
    public void printReportAndRegister(ModelMap model, 
                            @RequestParam(value="report", required=true) String report,
                            @RequestParam(value="parameters", required=true) String parameters,
                            HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	ReportService rs = Context.getService(ReportService.class);
		
		ReportRequest rr = new ReportRequest();
		
		ReportDefinition reportDef = Helper.findReportDefinition(report);
		
		String[] paramNames = parameters.split(",");
		
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		for (String param: paramNames) {
			params.put(param, request.getParameter(param));	
			
			//for this report we want to create an observation when printed
			if(param.equals("patientId"))
			{
				Patient patient = Context.getPatientService().getPatient(new Integer(request.getParameter(param)));
				
				Obs o = new Obs();
				o.setPerson(patient);
				o.setConcept(gp.getConcept(GlobalPropertiesManagement.PATIENT_PRESENTS_FOR_CHEMO));
				o.setObsDatetime(new Date());
				o.setValueCoded(gp.getConcept(GlobalPropertiesManagement.YES));
				
				Context.getObsService().saveObs(o, "checking in Patient for chemo");
			}
		}
		
		rr.setReportDefinition(new Mapped<ReportDefinition>(reportDef, params));
		
		List<RenderingMode> modes = rs.getRenderingModes(reportDef);
		RenderingMode rm = modes.get(0);
		
	    rr.setRenderingMode(rm);
	    rr.setPriority(Priority.HIGHEST);
		
		Report rep = rs.runReport(rr);
		
		String filename = rm.getRenderer().getFilename(rep.getRequest());
		response.setContentType(rm.getRenderer().getRenderedContentType(rep.getRequest()));
		byte[] data = rep.getRenderedOutput();
		
		if (data != null) {
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
			response.setHeader("Pragma", "no-cache");
			IOUtils.write(data, response.getOutputStream());
		}
		else {
			response.getWriter().write("There was an error retrieving the report");
		}
		
    }
		
    @RequestMapping("/module/rwandareports/renderCalendarWebRenderer")
	public ModelAndView renderIndicatorReport() {     	
	    return new ModelAndView("/module/rwandareports/renderCalendarWebRenderer");    	
	}
    
    public String showReport(Model model, HttpSession session) {
		String renderArg = (String) session.getAttribute(ReportingConstants.OPENMRS_REPORT_ARGUMENT);
		ReportData data = null;
		try {
			data = (ReportData) session.getAttribute(ReportingConstants.OPENMRS_REPORT_DATA);
		}
		catch (ClassCastException ex) {
			// pass
		}
		if (data == null)
			return "redirect:../reporting/dashboard/index.form";
		
		MapDataSet dataSet = (MapDataSet) data.getDataSets().get(renderArg);
		model.addAttribute("columns", dataSet.getMetaData());
		//List<Regimen> regimens = RwandaReportsUtil.createRegimenList(Context.getService(Cd4CountReportingService.class).getAllRegimens());  
	//	model.addAttribute("regimens", regimens);
		return null;
	}
    
}
