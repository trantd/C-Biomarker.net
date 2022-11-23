package kcore.plugin.biomarker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.hc.hc_algorithm;
import kcore.plugin.rcore.sequence.RCore;

public class Biomaker extends AbstractTask {

	public static String outputFile = "output_concat.txt";
	
	public RCore rc;
	
	public hc_algorithm hc;
	
	public Biomaker(KcoreParameters params, String path) {
		// Init
		rc = new RCore(params, path, true);
		hc = new hc_algorithm(params, path, true);
		outputFile = path;
	}

	public void writeFile(RCore rc, hc_algorithm hc) {
		ArrayList<BiomarkerResult> biomarkerGene = new ArrayList<>();
		// write file
		Path path = Paths.get(outputFile);
		ArrayList<String> lines = new ArrayList<>();
		for (String vertex : rc.getVertexList()) {
			BiomarkerResult result = new BiomarkerResult(vertex, rc.getRcore().get(vertex) + 1,
					hc.getHcEntropy().get(vertex));
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
	public void cancel() {
		super.cancel();
		cancelled = true;
//		if (cancelled == true) {
//			JOptionPane.showMessageDialog(null, "Compute Biomarker Success, open text file to see the result!", "Infor",
//					JOptionPane.INFORMATION_MESSAGE);
//		}

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching modules ....");
		System.gc();
		rc.run(taskMonitor);
		hc.run(taskMonitor);
		taskMonitor.setProgress(0.7);
		taskMonitor.setStatusMessage("Write biomarker result....");
		writeFile(rc, hc);
	}
}
