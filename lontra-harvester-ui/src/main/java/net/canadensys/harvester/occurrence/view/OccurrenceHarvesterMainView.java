package net.canadensys.harvester.occurrence.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.harvester.occurrence.controller.StepControllerIF;
import net.canadensys.harvester.occurrence.model.DwcaResourceStatusModel;
import net.canadensys.harvester.occurrence.model.IPTFeedModel;
import net.canadensys.harvester.occurrence.view.model.HarvesterViewModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Main UI to control the harvester.
 *
 * @author canadensys
 *
 */
public class OccurrenceHarvesterMainView {

	private JFrame harvesterFrame = null;
	private JTabbedPane tabbedPane = null;
	private JPanel mainPanel = null;
	private ResourcesPanel resourcesPanel = null;
	private PublishersPanel publishersPanel = null;

	@Autowired
	private HarvesterViewModel harvesterViewModel;

	@Autowired
	@Qualifier("stepController")
	private StepControllerIF stepController;

	public void initView() {

		// Initialize the publishers panel:
		publishersPanel = new PublishersPanel(stepController);
		
		// Setup stepController with  
		// Initialize the resources panel:
		resourcesPanel = new ResourcesPanel(stepController, harvesterViewModel);

		// Initialize main frame:
		harvesterFrame = new JFrame(Messages.getString("view.title"));
		harvesterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		harvesterFrame.setLayout(new GridBagLayout());

		// Initialize main panel:
		mainPanel = new JPanel(new GridBagLayout());

		// Vertical index:
		int lineIdx = 0;

		GridBagConstraints c = new GridBagConstraints();

		// Add header with database details to the main frame:
		c.gridy = lineIdx++;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		JLabel lbl = new JLabel(Messages.getString("view.info.currentDatabase")
				+ harvesterViewModel.getDatabaseLocation());
		lbl.setForeground(Color.BLUE);
		mainPanel.add(lbl, c);

		// Add tabbed panes to the frame in the second level of vertical alignment, right bellow database information:
		tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize(new Dimension(800, 600));
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;

		// Add all tabbed pane panels:
		tabbedPane.add("Resources", resourcesPanel);
		tabbedPane.add("Publishers", publishersPanel);
		tabbedPane.add("Import Log", importLogPanel());
		tabbedPane.add("IPT RSS Feed", RSSFeedPanel());

		mainPanel.add(tabbedPane, c);

		// Pack frame and set visible:
		harvesterFrame.add(mainPanel);
		harvesterFrame.pack();
		harvesterFrame.setLocationRelativeTo(null);
		harvesterFrame.setVisible(true);

		redirectSystemStreams();

		checkForOutdatedResources();
	}

	public ResourcesPanel getResourcesPanel() {
		return resourcesPanel;
	}

	public void setResourcesPanel(ResourcesPanel resourcesPanel) {
		this.resourcesPanel = resourcesPanel;
	}

	public PublishersPanel getPublishersPanel() {
		return publishersPanel;
	}

	public void setPublishersPanel(PublishersPanel publishersPanel) {
		this.publishersPanel = publishersPanel;
	}

	public HarvesterViewModel getHarvesterViewModel() {
		return harvesterViewModel;
	}

	public void setHarvesterViewModel(HarvesterViewModel harvesterViewModel) {
		this.harvesterViewModel = harvesterViewModel;
	}

	/**
	 * Display in text area resource that should be (re)harvested.
	 */
	private void checkForOutdatedResources() {
		List<DwcaResourceStatusModel> outdatedResources = stepController
				.getResourceToHarvest();
		for (DwcaResourceStatusModel currResourceModel : outdatedResources) {
			resourcesPanel.appendConsoleText(Messages.getString("view.info.harvestRequired")
					+ currResourceModel.getDwcaResourceModel().getSourcefileid() + "\n");
		}
	}

	/**
	 * TODO remove this hack.
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				resourcesPanel.appendConsoleText(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				resourcesPanel.appendConsoleText(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}

	/**
	 * Returns a initialized panel with import log information
	 *
	 * @return
	 */
	private JPanel importLogPanel() {
		Vector<String> headers = new Vector<String>();
		headers.add(Messages.getString("view.importLog.table.sourceFileId"));
		headers.add(Messages.getString("view.importLog.table.recordQty"));
		headers.add(Messages.getString("view.importLog.table.updatedBy"));
		headers.add(Messages.getString("view.importLog.table.date"));

		List<ImportLogModel> importLogModelList = stepController
				.getSortedImportLogModelList();
		return new ImportLogPanel(headers, importLogModelList);
	}

	/**
	 * Returns a initialized panel with IPT RSS feed information
	 *
	 * @return
	 */
	private JPanel RSSFeedPanel() {
		Vector<String> headers = new Vector<String>();
		headers.add(Messages.getString("view.iptFeed.table.title"));
		headers.add(Messages.getString("view.iptFeed.table.url"));
		headers.add(Messages.getString("view.iptFeed.table.key"));
		headers.add(Messages.getString("view.iptFeed.table.pubDate"));

		List<IPTFeedModel> importLogModelList = stepController.getIPTFeed();
		return new RSSFeedPanel(headers, importLogModelList);
	}
}