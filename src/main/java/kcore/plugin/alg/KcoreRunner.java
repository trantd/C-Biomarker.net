package kcore.plugin.alg;

import javax.swing.JOptionPane;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.biomarker.Biomaker;
import kcore.plugin.biomarker_parallel.Biomarker_algorithm_parallel;
import kcore.plugin.hc.hc_algorithm;
import kcore.plugin.hc_parallel.hc_algorithm_parallel;
import kcore.plugin.parallel.KcoreParallel;
import kcore.plugin.rcore.parallel.RcoreParallel;
import kcore.plugin.rcore.sequence.RCore;
import kcore.plugin.sequence.Kcore;
import kcore.plugin.service.ServicesUtil;
import kcore.plugin.service.TaskFactory;

public class KcoreRunner {

	private KcoreParameters params;
	private String path;
	private String device;

	public KcoreRunner(KcoreParameters params,String path, String device) {
		this.params = params;
		this.path = path;
		this.device = device;
	}

	public void runKcore() {
		Kcore alg = new Kcore(params,this.path);
		try {
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
			
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Kcore (1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			//alg.cancel();
			JOptionPane.showMessageDialog(null, "Compute K-core Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}
	
	public void runKcoreGPU() {
		try {
			KcoreParallel alg = new KcoreParallel(params,this.path,this.device);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Kcore GPU(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute K-core Success, open text file to see the result!",
					"Infor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
	
	public void runRcore() {
		try {
			RCore alg = new RCore(params,this.path, false);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Rcore(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute R-core Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
	
	public void runRcoreGPU() {
		try {
			RcoreParallel alg = new RcoreParallel(params,this.path, this.device, false);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Rcore GPU(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute R-core Success, open text file to see the result!",
					"Infor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
	
	public void runHc() {
		try {
			hc_algorithm alg = new hc_algorithm(params,this.path, false);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running HC(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute HC Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
	public void runHcParallel(boolean bio) {
		try {
			hc_algorithm_parallel alg = new hc_algorithm_parallel(params,this.path, this.device, bio, false);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running HC(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute HC Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
		
	public void runBiomaker() {
		try {
			Biomaker alg = new Biomaker(params, this.path);
			TaskFactory factory2 = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory2.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Biomarker(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute Biomarker Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}
	public void runBiomakerParallel() {
		try {
			Biomarker_algorithm_parallel alg = new Biomarker_algorithm_parallel(params, this.path,this.device);
			TaskFactory factory = new TaskFactory(alg);
			ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
//			alg.cancel();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Biomarker(1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			JOptionPane.showMessageDialog(null, "Compute Biomarker Success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

	}

}
