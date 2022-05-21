package kcore.plugin.ui;

import java.awt.Component;

import javax.swing.Icon;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

import kcore.plugin.alg.KcoreParametersPanel;

public class KcorePanelComponent implements CytoPanelComponent {

	private KcoreParametersPanel panel;
	
	public KcorePanelComponent(KcoreParametersPanel gtaParametersPannel) {
		this.panel = gtaParametersPannel; 
	}
	
	@Override
	public Component getComponent() {
		return panel;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return "C-Biomarker.net";
	}

	@Override
	public Icon getIcon() {
		return null;
	}

}
