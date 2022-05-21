package kcore.plugin;

import java.util.Properties;
import java.io.FileWriter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

import kcore.plugin.alg.KcoreParametersPanel;
import kcore.plugin.service.ServicesUtil;
import kcore.plugin.ui.NetworkSelectorPanel;
import kcore.plugin.ui.KcorePanelComponent;
import kcore.plugin.ui.UI;

public class Activator extends AbstractCyActivator {
	public Activator() {
		super();
	}

	public void start(BundleContext bc) {
		try {
            FileWriter fw = new FileWriter("D:\\testout.txt");
            fw.write("Welcome to java.");
            fw.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Success...");
		CySwingApplication cySwingApplicationServiceRef = getService(bc, CySwingApplication.class);
		CyApplicationManager cyApplicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc, CyNetworkViewManager.class);
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
		CyServiceRegistrar cyServiceRegistrarServiceRef = getService(bc, CyServiceRegistrar.class);
		CyEventHelper cyEventHelperServiceRef = getService(bc, CyEventHelper.class);
		TaskManager<?, ?> taskManagerServiceRef = getService(bc, TaskManager.class);

		CyProperty<Properties> cytoscapePropertiesServiceRef = getService(bc, CyProperty.class,
				"(cyPropertyName=cytoscape3.props)");
		VisualMappingManager visualMappingManagerRef = getService(bc, VisualMappingManager.class);
		CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc, CyNetworkFactory.class);

		CyRootNetworkManager cyRootNetworkFactory = getService(bc, CyRootNetworkManager.class);
		CyNetworkViewFactory cyNetworkViewFactoryServiceRef = getService(bc, CyNetworkViewFactory.class);
		CyLayoutAlgorithmManager cyLayoutsServiceRef = getService(bc, CyLayoutAlgorithmManager.class);

		LoadVizmapFileTaskFactory loadVizmapFileTaskFactory = getService(bc, LoadVizmapFileTaskFactory.class);
		SynchronousTaskManager<?> synchronousTaskManagerServiceRef = getService(bc, SynchronousTaskManager.class);

		ServicesUtil.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		ServicesUtil.cyApplicationManagerServiceRef = cyApplicationManagerServiceRef;
		ServicesUtil.cyNetworkViewManagerServiceRef = cyNetworkViewManagerServiceRef;
		ServicesUtil.cyNetworkManagerServiceRef = cyNetworkManagerServiceRef;
		ServicesUtil.cyServiceRegistrarServiceRef = cyServiceRegistrarServiceRef;
		ServicesUtil.cyEventHelperServiceRef = cyEventHelperServiceRef;
		ServicesUtil.taskManagerServiceRef = taskManagerServiceRef;
		ServicesUtil.cytoscapePropertiesServiceRef = cytoscapePropertiesServiceRef;
		ServicesUtil.visualMappingManagerRef = visualMappingManagerRef;
		ServicesUtil.cyNetworkFactoryServiceRef = cyNetworkFactoryServiceRef;
		ServicesUtil.cyRootNetworkFactory = cyRootNetworkFactory;
		ServicesUtil.cyNetworkViewFactoryServiceRef = cyNetworkViewFactoryServiceRef;
		ServicesUtil.cyLayoutsServiceRef = cyLayoutsServiceRef;
		ServicesUtil.loadVizmapFileTaskFactory = loadVizmapFileTaskFactory;
		ServicesUtil.synchronousTaskManagerServiceRef = synchronousTaskManagerServiceRef;
		// ServicesUtil.cyHelpBrokerServiceRef = cyHelpBroker;

		//

//		NetworkSelectorPanel networkSelectorPanel = new NetworkSelectorPanel(cyApplicationManagerServiceRef,
//				cyNetworkManagerServiceRef);


		NetworkSelectorPanel gtaNetworkSelectorPanel = new NetworkSelectorPanel(cyApplicationManagerServiceRef,
				cyNetworkManagerServiceRef);

		KcoreParametersPanel gtaParametersPannel = new KcoreParametersPanel(gtaNetworkSelectorPanel);
		KcorePanelComponent activeModulesCytoPanelComponent = new KcorePanelComponent(gtaParametersPannel);
		UI activeModulesUI = new UI(gtaParametersPannel);

		
		registerAllServices(bc, gtaParametersPannel, new Properties());

//		registerAllServices(bc, networkSelectorPanel, new Properties());
		registerAllServices(bc, gtaNetworkSelectorPanel, new Properties());

//		registerAllServices(bc, mainPanel, new Properties());

		registerService(bc, activeModulesCytoPanelComponent, CytoPanelComponent.class, new Properties());
		registerAllServices(bc, activeModulesUI, new Properties());
	}
}
