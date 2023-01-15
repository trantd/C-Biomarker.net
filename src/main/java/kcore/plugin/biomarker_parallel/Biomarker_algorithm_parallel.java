package kcore.plugin.biomarker_parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.biomarker.BiomarkerResult;
import kcore.plugin.hc_parallel.hc_algorithm_parallel;
import kcore.plugin.rcore.parallel.RcoreParallel;

public class Biomarker_algorithm_parallel extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(Biomarker_algorithm_parallel.class);
	static {
		System.setProperty("com.aparapi.dumpProfilesOnExit", "true");
		System.setProperty("com.aparapi.enableExecutionModeReporting", "false");
		System.setProperty("com.aparapi.enableShowGeneratedOpenCL", "false");
	}
	private static String device;
	
	private RcoreParallel rc;
	private hc_algorithm_parallel hc;
	private String outputFile;	
	
	public Biomarker_algorithm_parallel(KcoreParameters params, String path, String device) {
		this.outputFile = path;
		Biomarker_algorithm_parallel.device = device;
		rc = new RcoreParallel(params,path, device, true);
		hc = new hc_algorithm_parallel(params, path, device, false, true);
	}
	

	// write result to output.txt
	public void writeFile(RcoreParallel rc, hc_algorithm_parallel hc) {
		ArrayList<BiomarkerResult> biomarkerGene = new ArrayList<>();
		// write file
		Path path = Paths.get(outputFile);
		ArrayList<String> lines = new ArrayList<>();
		for (String vertex : rc.getVertexList()) {
			BiomarkerResult result = new BiomarkerResult(vertex, rc.getReachability().get(vertex) + 1,
					hc.getHc().get(vertex) + 1);
			biomarkerGene.add(result);
		}

		lines.add("Node\tRCore\tHC");
		for (BiomarkerResult result : biomarkerGene) {
			lines.add(String.format("%s\t%d\t%f", result.getName(), result.getrCore(), result.getHc()));
		}
//		lines.add("time start: " + start + " - " + "time end: " + end);
		try {
			Files.write(path, lines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec("notepad " + path.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(path.toString());
	}

	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		try {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching modules ....");
		System.gc();
		rc.run(taskMonitor);
		hc.run(taskMonitor);
		taskMonitor.setProgress(0.9);
		taskMonitor.setStatusMessage("Write biomarker result....");
		writeFile(rc, hc);

		} catch(Exception e) {
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement element : e.getStackTrace()) {
				sb.append(element);
				sb.append("\n");
			}
			String stackTrace = sb.toString();
			taskMonitor.setStatusMessage(stackTrace);
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		cancelled = true;
	}

//	public void writeFile(String output, String message) {
//		Path path = Paths.get(output);
//		try {
//			java.nio.file.Files.write(path, 
//                          message.getBytes(java.nio.charset.StandardCharsets.UTF_8),
//                          java.nio.file.StandardOpenOption.CREATE,
//                          java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
