package kcore.plugin.ui;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;

import kcore.plugin.alg.KcoreParametersPanel;
import kcore.plugin.service.ServicesUtil;


public class UI extends AbstractCyAction { 
	private KcoreParametersPanel mainPanel;
	
	private final CytoPanel cytoPanelWest;

	public UI(KcoreParametersPanel mainPanel) {
		super("C-Biomarker.net...", ServicesUtil.cyApplicationManagerServiceRef, "network", ServicesUtil.cyNetworkViewManagerServiceRef);
		setPreferredMenu("Apps");
		//setMenuGravity(2.0f);
		
		this.mainPanel = mainPanel;
		
		cytoPanelWest = ServicesUtil.cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.WEST);
	}

	
	public void actionPerformed(ActionEvent e) {
		if (cytoPanelWest.getState() == CytoPanelState.HIDE) {
			cytoPanelWest.setState(CytoPanelState.DOCK);
		}	

		int index = cytoPanelWest.indexOfComponent(mainPanel);
		if (index == -1) {
			return;
		}
		
		cytoPanelWest.setSelectedIndex(index);		
	}
	
}
