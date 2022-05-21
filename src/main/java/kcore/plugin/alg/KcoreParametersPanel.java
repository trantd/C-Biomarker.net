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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

import convertFile.DirectionType;
import convertFile.Edge;
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
		String path = showSaveFileDialog();
		if (path == null || path.equals("")) {
			return;
		}
		ConvertXMLtoText convert = new ConvertXMLtoText(path);
		try {
			System.out.println("start convert");
			convert.ConvertKeggXML();
			convert.writeFile();
			
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running Kcore (1)!  " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		finally{
			//alg.cancel();
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
		
		sequenceDevice = new JRadioButton("Auto");
//		advancedConfigPanel.add(sequenceDevice,
//				gridConstraint(0, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		
		cpuDevice = new JRadioButton("CPU");
//		advancedConfigPanel.add(cpuDevice,
//				gridConstraint(0, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));

		gpuDevice = new JRadioButton("GPU");
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

		//new content
		JTabbedPane tabbedPane = new JTabbedPane();
		mainPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		mainPanel.setBorder(javax.swing.BorderFactory.createLineBorder(null,1));
		subPanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
		subPanel.setBorder(javax.swing.BorderFactory.createLineBorder(null,1));
		tabbedPane.addTab("Biomarker nodes", null, mainPanel, "click to show mainPanel");
		tabbedPane.addTab("Extension", null, subPanel, "click to show subPanel");
		
		ButtonGroup btnGroupBio = new ButtonGroup();
		sequenceBio = new JRadioButton("Auto");
		cpuBio = new JRadioButton("CPU");
		gpuBio = new JRadioButton("GPU");
		btnGroupBio.add(sequenceBio);
		btnGroupBio.add(cpuBio);
		btnGroupBio.add(gpuBio);
		sequenceBio.setSelected(true);
		mainPanel.add(new java.awt.Label("Choose device:"),
		gridConstraint(1, 2, 2, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(sequenceBio,gridConstraint(1, 3, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(cpuBio,gridConstraint(1, 4, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		mainPanel.add(gpuBio,gridConstraint(1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL));
		
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
		subPanel.add(convertFile,gridConstraint(1, 5, 3, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL)) ;
		convertFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
//				if(!convertFile.isSelected()) {
					convertFileXML();
					convertFile.setSelected(false);
//				}
			}
		});
		
		JPanel functionPanel = new JPanel(new java.awt.GridBagLayout());
		functionPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0,Color.black));
		JPanel devicePanel = new JPanel(new java.awt.GridBagLayout());
		devicePanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0,Color.black));
		
		functionPanel.add(new java.awt.Label("Choose function:"),
				gridConstraint(1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER) );
		functionPanel.add(kCore,gridConstraint(2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		functionPanel.add(rCore,gridConstraint(3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		functionPanel.add(hc,gridConstraint(4, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		
		devicePanel.add(new java.awt.Label("Choose device:"),
				gridConstraint(1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(sequenceDevice,gridConstraint(2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(cpuDevice,gridConstraint(3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
		devicePanel.add(gpuDevice,gridConstraint(4, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.CENTER));
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
			//K-core sequence
			if (subPanel.isVisible() && sequenceDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runKcore();
			} 
			//R-core sequence
			else if (subPanel.isVisible() && sequenceDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runRcore();
				
			} 
			//K-core CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runKcoreGPU();
				
			} 
			//K-core GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && kCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(0);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runKcoreGPU();
				
			} 
			//R-core CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runRcoreGPU();
			}
			//R-core GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && rCore.isSelected()) {
				netFilteringMethod.setSelectedIndex(1);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runRcoreGPU();
			}
			//HC sequence
			else if (subPanel.isVisible() && sequenceDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "");
				alg.runHc();
			}
			//HC CPU
			else if (subPanel.isVisible() && cpuDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runHcParallel(false);
			}
			//HC GPU
			else if (subPanel.isVisible() && gpuDevice.isSelected() && hc.isSelected()) {
				netFilteringMethod.setSelectedIndex(2);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runHcParallel(false);
			}
			//BiomarkerGene sequence
			else if (mainPanel.isVisible() && sequenceBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
					KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
							(NetFilteringMethod) netFilteringMethod.getSelectedItem());
					KcoreRunner alg = new KcoreRunner(params, path, "");
					alg.runBiomaker();
			}
			//BiomarkerGene CPU
			else if (mainPanel.isVisible() && cpuBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "CPU");
				alg.runBiomakerParallel();
//				alg.runRcoreGPU();
//				alg.runHcParallel(true);
			}
			//BiomarkerGene GPU
			else if (mainPanel.isVisible() && gpuBio.isSelected()) {
				netFilteringMethod.setSelectedIndex(3);
				KcoreParameters params = new KcoreParameters(networkSelectorPanel.getSelectedNetwork(), 10,
						(NetFilteringMethod) netFilteringMethod.getSelectedItem());
				KcoreRunner alg = new KcoreRunner(params, path, "GPU");
				alg.runBiomakerParallel();
//				alg.runRcoreGPU();
//				alg.runHcParallel(true);
			}
			 else {
				JOptionPane.showMessageDialog(null, "Chọn một thiết bị thực thi.");
				return;
			}
		}
		
	}
	public class ConvertXMLtoText{
		private List<Edge> edgeList = new ArrayList<Edge>();
		private String outputFile;
		
		public ConvertXMLtoText(String outputFile) {
			this.outputFile = outputFile;
		}

		public ConvertXMLtoText() {
			super();
		}

		public List<Edge> getEdgeList() {
			return edgeList;
		}

		public void setEdgeList(List<Edge> edgeList) {
			this.edgeList = edgeList;
		}

		public String getOutputFile() {
			return outputFile;
		}

		public void setOutputFile(String outputFile) {
			this.outputFile = outputFile;
		}

		public DirectionType getType(String type) {
			DirectionType direction = DirectionType.UNDIRECTED;
				if (type.equals("-->") || type.equals("--|") || type.equals("..>")
						|| type.equals("-o->") || type.equals("-/-")
						|| type.equals("+p") || type.equals("phosphorylation")) {
					direction = DirectionType.DIRECTED;
				} 
				else if (type.equals("---") || type.equals("-+-")) {
					direction = DirectionType.UNDIRECTED;
				}

			return direction;
		}
		
		public void ConvertKeggXML() {
			try {
				File inputFile = new File("D:\\Thuc Tap\\Tài Liệu\\hsa05219.xml");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			    Document doc = dBuilder.parse(inputFile);
			    doc.getDocumentElement().normalize();
			    
	            NodeList entryLink = doc.getElementsByTagName("entry");
	            System.out.println("----------------------------");
	            System.out.println(entryLink.getLength());
	            for (int temp = 0; temp < entryLink.getLength(); temp++) {
	                Node nNode = entryLink.item(temp);
	                System.out.print(temp + " ");
	                System.out.println(nNode.getNodeName());
	                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	                    Element eElement = (Element) nNode;
	                    System.out.println(eElement.getAttribute("name"));
	                    String[] subNameNode = eElement.getAttribute("name").split(" ");
	                    if(subNameNode != null) {
	                    	System.out.println("process group " + subNameNode.length);
	    					for (int j = 0; j < subNameNode.length; j++) {
	    						if (j == subNameNode.length - 1) {
	    							
	    						} else {
	    							for (int k = j + 1; k < subNameNode.length; k++) {
	    								Edge edge = new Edge(subNameNode[j], subNameNode[k], 0, 1);
	    								edgeList.add(edge);
	    								System.out.println(edge.getStartNode() + " -> " +edge.getEndNode() + " : " + edge.getDirection());
	    							}
	    						}
	    					}
	    				}
	                }
	            }
	            for (Edge edge : edgeList) {
					System.out.println(edge.getStartNode() + " -> " +edge.getEndNode() + " : " + edge.getDirection());
				}
	            System.out.println("start read relation");
	            NodeList relation = doc.getElementsByTagName("relation");
	            for (int temp = 0; temp < relation.getLength(); temp++) {
					Node nNode = relation.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						Node subType = eElement.getElementsByTagName("subtype").item(0);
						Element eSubType = (Element) subType;
						String type = eSubType.getAttribute("value");
						DirectionType direction = getType(type);
						String entry2 = eElement.getAttribute("entry2");
						String entry1 = eElement.getAttribute("entry1");
						ArrayList<String> firstEdge = new ArrayList<>();
		 				ArrayList<String> secondEdge = new ArrayList<>();
		 				// set first array edge
		 				setAr(firstEdge, secondEdge, entry2, 1, doc);
		 				// set second array edge
		 				setAr(firstEdge, secondEdge, entry1, 2, doc);
		 				// Add edge
		 				swap(firstEdge, secondEdge, direction);
	            	}
	            }
	            for (Edge edge : edgeList) {
					System.out.println(edge.getStartNode() + " -> " +edge.getEndNode() + " : " + edge.getDirection());
				}
//				writeFile();
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.out.println("error " + e.getMessage());
	        }
		}
		public void setAr(ArrayList<String> ar1, ArrayList<String> ar2, String key, int type, Document doc) {
			NodeList entryName = doc.getElementsByTagName("entry link");
			for (int temp = 0; temp < entryName.getLength(); temp++) {
				Node nNode = entryName.item(temp);
				 if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					 Element eElement = (Element) nNode;
					 String entryId = eElement.getAttribute("id");
					 if (entryId.equals(key)) {
							// Neu entryId = edge key thi lay ra danh sach kegg id
							String[] subNameMain = eElement.getAttribute("name").split(" ");
							// String nameMain = convertString(temp);
							// get node list of first edge
							// String[] subNameMain = nameMain.split(",");
							if (type == 1) {
								for (int l = 0; l < subNameMain.length; l++) {
									ar1.add(subNameMain[l]);
								}
							} else if (type == 2) {
								for (int l = 0; l < subNameMain.length; l++) {
									ar2.add(subNameMain[l]);
								}
							}

						}
				 }
			}
		}

		// Compare and swap
		public void swap(ArrayList<String> ar1, ArrayList<String> ar2, DirectionType direction) {
			for (int i = 0; i < ar1.size(); i++) {
				for (int j = 0; j < ar2.size(); j++) {
					Edge edge = new Edge(ar1.get(i), ar2.get(j), direction == DirectionType.DIRECTED ? 1:0, 1);
					edgeList.add(edge);
				}
			}
		}
		
		public void writeFile() {
			System.out.println("start write file");
			Path path = Paths.get(outputFile);
			List<String> lines = new ArrayList<>();
			lines.add("Start\tEnd\tDirection");
			for (Edge edge : edgeList) {
				lines.add(String.format("%s\t%s\t%d", edge.getStartNode(), edge.getEndNode(), edge.getDirection()));
			}
			try {
				Files.write(path, lines);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

}
