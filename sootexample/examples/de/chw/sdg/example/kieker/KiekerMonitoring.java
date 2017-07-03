package de.chw.sdg.example.kieker;

import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;

public class KiekerMonitoring {

	private static final IMonitoringController MONITORING_CONTROLLER = MonitoringController.getInstance();
	
	public static void main(String[] args) {
		System.out.println("Starting..");
		final long tin = MONITORING_CONTROLLER.getTimeSource().getTime();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		final long tout = MONITORING_CONTROLLER.getTimeSource().getTime();

		final OperationExecutionRecord e = new OperationExecutionRecord(
				"sleeping",
				OperationExecutionRecord.NO_SESSION_ID,
				OperationExecutionRecord.NO_TRACE_ID,
				tin, tout, "myHost",
				OperationExecutionRecord.NO_EOI_ESS,
				OperationExecutionRecord.NO_EOI_ESS);
		MONITORING_CONTROLLER.newMonitoringRecord(e);
		System.out.println("Finished!");
	}

}
