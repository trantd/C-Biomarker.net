// ActivePathsParametersPopupDialog
//-----------------------------------------------------------------------------
// $Date: 2019 - 17 - 07
// $Author: quang ares
//-----------------------------------------------------------------------------
package kcore.plugin.alg;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;

import org.apache.poi.hssf.util.HSSFColor.BLACK;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.ColumnCreatedEvent;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.alg.param.NetFilteringMethod;
import kcore.plugin.service.ServicesUtil;
import kcore.plugin.service.TaskFactory;
import kcore.plugin.ui.NetworkSelectorPanel;
import java.awt.EventQueue;

public class KcoreParametersPanel extends JPanel implements ColumnCreatedListener, CytoPanelComponentSelectedListener {

	private static final Logger logger = LoggerFactory.getLogger(KcoreParametersPanel.class);

	private boolean isPanelSelected = false;

	private NetworkSelectorPanel networkSelectorPanel;

	private JComboBox<NetFilteringMethod> netFilteringMethod;

	private javax.swing.JPanel mainPanel;
	private javax.swing.JPanel subPanel;
	private JRadioButton sequenceBio;
	private JRadioButton cpuBio;
	private JRadioButton gpuBio;

	private JRadioButton kCore;
	private JRadioButton rCore;
	private JRadioButton hc;
	private JRadioButton sequenceDevice;
	private JRadioButton gpuDevice;
	private JRadioButton cpuDevice;

	private JRadioButton convertFile;

	public KcoreParametersPanel(NetworkSelectorPanel gtaNetworkSelectorPanel) {
		this.networkSelectorPanel = gtaNetworkSelectorPanel;
		this.networkSelectorPanel.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateAttributePanel();

			}
		});

		// Set global parameters
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(32, 42));

		initComponents();
	}

	private void convertFileXML() {
		ArrayList<String> inputPath = showChooseFileDialog();
		if (inputPath == null) {
			return;
		}
		
		String path = showSaveFolderDialog();
		if (path == null || path.equals("")) {
			return;
		}
		try {
			for (String string : inputPath) {
				String[] arrString = string.split("\\\\");
				ConvertKegg convert = new ConvertKegg();
				convert.ConvertXMLtoText(string);
				convert.writeFile(path + "\\" + arrString[arrString.length-1] + ".txt");
				convert.setEdgeList(new ArrayList<>());
			}
			Desktop desktop = Desktop.getDesktop();
	        File dirToOpen = null;
	        try {
	            dirToOpen = new File(path);
	            desktop.open(dirToOpen);
	        } catch (IllegalArgumentException iae) {
	            System.out.println("File Not Found");
	        }
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Kcore (1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		} finally {
			// alg.cancel();
			JOptionPane.showMessageDialog(null, "Convert success, open text file to see the result!", "Infor",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}

	private void initComponents() {
		setLayout(new java.awt.GridBagLayout());

		java.awt.GridBagConstraints gridBagConstraints;
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5); // 5-5-5-5
		add(networkSelectorPanel, gridBagConstraints);
//
		initAttrSelectionTable();
//
		javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
		buttonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

		javax.swing.JButton runButton = new javax.swing.JButton("Run");
		runButton.addActionListener(new FindModulesAction());
		buttonPanel.add(runButton, gridBagConstraints);

		add(buttonPanel, gridBagConstraints);
		// demo temp panel
		// demo
		javax.swing.JPanel jPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		java.awt.GridBagConstraints gridBagConstraint = new java.awt.GridBagConstraints();
		gridBagConstraint.gridy = 3;
		gridBagConstraint.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraint.weightx = 1.0;
		gridBagConstraint.weighty = 1.0;
		gridBagConstraint.insets = new java.awt.Insets(0, 0, 0, 0);
		add(jPanel, gridBagConstraint);

	}

	private static GridBagConstraints gridConstraint(int gridx, int gridy, int gridwidth, int gridheight, int anchor,
			int fill) {
		final Insets insets = new Insets(0, 0, 0, 0);

		return new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 1.0, 1.0, anchor, fill, insets, 0, 0);
	}

	private void initAttrSelectionTable() {

		java.awt.GridBagConstraints gridBagConstraints;

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		add(networkSelectorPanel, gridBagConstraints);

		javax.swing.JPanel bothAttributeSelectorPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());

		javax.swing.JPanel configPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		bothAttributeSelectorPanel.setPreferredSize(new Dimension(32, 20));

		javax.swing.JPanel advancedConfigPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		advancedConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Advanced Options"));
		// content
//		advancedConfigPanel.add(new java.awt.Label("Choose method:"),
//				gridConstraint(0, 2, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		netFilteringMethod = new JComboBox<NetFilteringMethod>(NetFilteringMethod.values());
//		advancedConfigPanel.add(netFilteringMethod,
//				gridConstraint(1, 2, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		sequenceDevice = new JRadioButton("Sequential on CPU");
//		advancedConfigPanel.add(sequenceDevice,
//				gridConstraint(0, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		cpuDevice = new JRadioButton("Parallel on CPU");
//		advancedConfigPanel.add(cpuDevice,
//				gridConstraint(0, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		gpuDevice = new JRadioButton("Parallel on GPU");
		// set action
		sequenceDevice.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (sequenceDevice.isSelected()) {
					cpuDevice.setSelected(false);
					gpuDevice.setSelected(false);
				} else {
//					gpuDevice.setSelected(true);
				}
			}
		});
		cpuDevice.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (cpuDevice.isSelected()) {
					sequenceDevice.setSelected(false);
					gpuDevice.setSelected(false);
				} else {
//					gpuDevice.setSelected(true);
				}
			}
		});
		gpuDevice.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (gpuDevice.isSelected()) {
					sequenceDevice.setSelected(false);
					cpuDevice.setSelected(false);
				} else {
//					cpuDevice.setSelected(true);
				}
			}
		});

		// new content
		JTabbedPane tabbedPane = new JTabbedPane();
		mainPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		mainPanel.setBorder(javax.swing.BorderFactory.createLineBorder(null, 1));
		subPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		subPanel.setBorder(javax.swing.BorderFactory.createLineBorder(null, 1));
		tabbedPane.addTab("Biomarker Nodes", null, mainPanel, "click to show mainPanel");
		tabbedPane.addTab("Extension", null, subPanel, "click to show subPanel");

		ButtonGroup btnGroupBio = new ButtonGroup();
		sequenceBio = new JRadioButton("Sequential on CPU");
		cpuBio = new JRadioButton("Parallel on CPU");
		gpuBio = new JRadioButton("Parallel on GPU");
		btnGroupBio.add(sequenceBio);
		btnGroupBio.add(cpuBio);
		btnGroupBio.add(gpuBio);
		sequenceBio.setSelected(true);
		mainPanel.add(new java.awt.Label("Running mode:"),
				gridConstraint(1, 2, 2, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(sequenceBio, gridConstraint(1, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(cpuBio, gridConstraint(1, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(gpuBio, gridConstraint(1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		ButtonGroup btnGroup = new ButtonGroup();
		kCore = new JRadioButton("K-core");
		rCore = new JRadioButton("R-core");
		hc = new JRadioButton("Hierarchical closeness");
		kCore.setSelected(true);
		btnGroup.add(kCore);
		btnGroup.add(rCore);
		btnGroup.add(hc);
//		subPanel.add(new java.awt.Label("Choose function:"),
//				gridConstraint(1, 2, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(new java.awt.Label("Choose device:"),
//				gridConstraint(2, 2, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(sequenceDevice,gridConstraint(2, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(cpuDevice,gridConstraint(2, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(gpuDevice,gridConstraint(2, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(kCore,gridConstraint(1, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(rCore,gridConstraint(1, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
//		subPanel.add(hc,gridConstraint(1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		convertFile = new JRadioButton("convert KEGG XML to text");
		subPanel.add(convertFile, gridConstraint(1, 5, 3, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		convertFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				convertFileXML();
				convertFile.setSelected(false);
			}
		});

		JPanel functionPanel = new JPanel(new java.awt.GridBagLayout());
		functionPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
		JPanel devicePanel = new JPanel(new java.awt.GridBagLayout());
		devicePanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));

		functionPanel.add(new java.awt.Label("Choose function:"),
				gridConstraint(1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		functionPanel.add(kCore, gridConstraint(2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		functionPanel.add(rCore, gridConstraint(3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		functionPanel.add(hc, gridConstraint(4, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));

		devicePanel.add(new java.awt.Label("Running mode:"),
				gridConstraint(1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(sequenceDevice, gridConstraint(2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(cpuDevice, gridConstraint(3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(gpuDevice, gridConstraint(4, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		subPanel.add(functionPanel, gridConstraint(1, 1, 5, 2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
		subPanel.add(devicePanel, gridConstraint(1, 3, 5, 2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

		// end content
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		bothAttributeSelectorPanel.add(tabbedPane, gridBagConstraints);

		configPanel.add(bothAttributeSelectorPanel,
				gridConstraint(0, 1, 1, 1, GridBagConstraints.PAGE_START, GridBagConstraints.BOTH));

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		add(configPanel, gridBagConstraints);
	}

	private String showSaveFileDialog() {
		String url = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("File to save the result");

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			url = fileToSave.getAbsolutePath();
		}

		return url;
	}
	
	private String showSaveFolderDialog() {
		String url = null;
		JFileChooser folderChooser = new JFileChooser();
		folderChooser.setDialogTitle("Folder to save converted files");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int userSelection = folderChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			url = folderChooser.getSelectedFile().toString();
		}

		return url;
	}

	private ArrayList<String> showChooseFileDialog() {
		ArrayList<String> inputList = new ArrayList<>();
		JFileChooser chooser = new JFileChooser();
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Choose file or folder to convert");
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		//
		// disable the "All files" option.
		//
		chooser.setAcceptAllFileFilterUsed(false);
		//
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//			System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
			System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
			if(!chooser.getSelectedFile().toString().contains(".xml")) {
				File[] listFile = chooser.getSelectedFile().listFiles();
				for (File file : listFile) {
					inputList.add(file.getPath());
					System.out.println(file.getPath());
				}
				return inputList;
			}
			else {
				inputList.add(chooser.getSelectedFile().toString());
				return inputList;
			}
		} else {
			System.out.println("No Selection ");
			return null;
		}
	}

	private void populateAttributeTable(Vector<String[]> dataVect) {
		logger.debug("populateAttributeTable");
	}

	private void updateAttributePanel() {
		if (!this.isPanelSelected) {
			return;
		}
		Vector<String[]> data = this.getDataVector();
		this.populateAttributeTable(data);
	}

	@Override
	public void handleEvent(ColumnCreatedEvent e) {
		updateAttributePanel();
	}

	public void handleEvent(CytoPanelComponentSelectedEvent e) {
		logger.debug("Event Occured " + e.toString() + " " + (e.getCytoPanel().getSelectedComponent() == this));
		if (e.getCytoPanel().getSelectedComponent() == this) {
			this.isPanelSelected = true;
			updateAttributePanel();
		} else {
			this.isPanelSelected = false;
		}
	}

	private Vector<String[]> getDataVector() {
		Vector<String[]> dataVect = new Vector<String[]>();

		if (networkSelectorPanel.getSelectedNetwork() == null) {
			return dataVect;
		}

		CyTable table = networkSelectorPanel.getSelectedNetwork().getDefaultNodeTable();
		Collection<CyColumn> columns = table.getColumns();

		return dataVect;
	}

	public class FindModulesAction extends AbstractAction {

		public FindModulesAction() {
			super("Find Modules");
		}

		public void actionPerformed(ActionEvent e) {
			String path = showSaveFileDialog();
			if (path == null || path.equals("")) {
				return;
			}
			runAlgorithm(path);

		}

		private void runAlgorithm(String path) {
			// K-core sequence
			if (subPanel.isVisible() && sequenceDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runKcore();
			}
			// R-core sequence
			else if (subPanel.isVisible() && sequenceDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runRcore();

			}
			// K-core CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runKcoreGPU();

			}
			// K-core GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runKcoreGPU();

			}
			// R-core CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runRcoreGPU();
			}
			// R-core GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runRcoreGPU();
			}
			// HC sequence
			else if (subPanel.isVisible() && sequenceDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runHc();
			}
			// HC CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runHcParallel(false);
			}
			// HC GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runHcParallel(false);
			}
			// BiomarkerGene sequence
			else if (mainPanel.isVisible() && sequenceBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runBiomaker();
			}
			// BiomarkerGene CPU
			else if (mainPanel.isVisible() && cpuBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runBiomakerParallel();
//				alg.runRcoreGPU();
//				alg.runHcParallel(true);
			}
			// BiomarkerGene GPU
			else if (mainPanel.isVisible() && gpuBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runBiomakerParallel();
//				alg.runRcoreGPU();
//				alg.runHcParallel(true);
			} else {
				JOptionPane.showMessageDialog(null, "Chọn một thiết bị thực thi.");
				return;
			}
		}

	}
}
