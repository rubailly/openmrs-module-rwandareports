/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.rwandareports.example;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Assert;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSetUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationProfiler;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.manager.ReportManager;
import org.openmrs.module.reporting.report.manager.ReportManagerUtil;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.web.renderers.WebReportRenderer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Abstract test for running a particular report
 */
public abstract class ReportManagerTest extends BaseModuleContextSensitiveTest {

	public abstract ReportManager getReportManager();

	public abstract EvaluationContext getEvaluationContext();

	@Autowired
	ReportDefinitionService reportDefinitionService;

	@Autowired
	ReportService reportService;

	public void performTest() throws Exception {

		if (!Context.isSessionOpen()) {
			Context.openSession();
		}
		Context.clearSession();
		authenticate();

		ReportManagerUtil.setupReport(getReportManager());
		LogManager.getLogger(EvaluationProfiler.class).setLevel(Level.TRACE);

		ReportManager rm = getReportManager();
		ReportDefinition rd = reportDefinitionService.getDefinitionByUuid(rm.getUuid());
		Assert.assertEquals(rm.getName(), rd.getName());
		Assert.assertEquals(rm.getDescription(), rd.getDescription());
		for (ReportDesign design : rm.constructReportDesigns(rd)) {
			ReportDesign dbDesign = reportService.getReportDesignByUuid(design.getUuid());
			Assert.assertEquals(design.getName(), dbDesign.getName());
			Assert.assertEquals(design.getRendererType(), dbDesign.getRendererType());
			Assert.assertEquals(design.getResources().size(), dbDesign.getResources().size());
		}

		EvaluationContext context = getEvaluationContext();
		System.out.println("Running report: " + rd.getName());
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ReportData data = reportDefinitionService.evaluate(rd, context);
		stopWatch.stop();
		System.out.println("Report completed: " + stopWatch.toString());
		Assert.assertTrue(data.getDataSets().size() > 0);
		for (RenderingMode renderingMode : reportService.getRenderingModes(rd)) {
			ReportRenderer renderer = renderingMode.getRenderer();
			if (!(renderer instanceof WebReportRenderer)) {
				String argument = renderingMode.getArgument();
				ReportRequest request = new ReportRequest(new Mapped<ReportDefinition>(rd, context.getParameterValues()), null, renderingMode, ReportRequest.Priority.HIGHEST, null);
				File outFile = new File(SystemUtils.getJavaIoTmpDir(), renderer.getFilename(request));
				FileOutputStream fos = new FileOutputStream(outFile);
				renderer.render(data, argument, fos);
				fos.close();
			}
		}
		if (enableReportOutput()) {
			for (String dsName : data.getDataSets().keySet()) {
				System.out.println(dsName);
				System.out.println("---------------------------------");
				DataSetUtil.printDataSet(data.getDataSets().get(dsName), System.out);
			}
		}
	}

	protected boolean isEnabled() {
		return false;
	}

	/**
	 * @return true if a subclass wants to print out the report contents to System.out
	 */
	public boolean enableReportOutput() {
		return false;
	}

	@Override
	public Properties getRuntimeProperties() {
		Properties p = super.getRuntimeProperties();
		p.setProperty("connection.username", "openmrs");
		p.setProperty("connection.password", "openmrs");
		p.setProperty("connection.url", "jdbc:mysql://localhost:3306/openmrs_rwanda_2x?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
		return p;
	}

	@Override
	public Boolean useInMemoryDatabase() {
		return false;
	}
}
