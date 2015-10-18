package com.jaliansystems.icm;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;

import com.jaliansystems.icm.BannerPanel.Sheet;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

public class Main extends JFrame {
	private static final long serialVersionUID = 1L;
	private JFormattedTextField frequency = new JFormattedTextField();
	private ConnectionStatusModel model = new ConnectionStatusModel();

	private JTextField url = new JTextField("http://google.com");
	private JButton export = new JButton("Export");
	private boolean stopped = false;
	private JTabbedPane tabbedPane = new JTabbedPane();

	public static class FormattedTextFieldVerifier extends InputVerifier {
		public boolean verify(JComponent input) {
			if (input instanceof JFormattedTextField) {
				JFormattedTextField ftf = (JFormattedTextField) input;
				AbstractFormatter formatter = ftf.getFormatter();
				if (formatter != null) {
					String text = ftf.getText();
					return text.matches("[0-9]+");
				}
			}
			return true;
		}

		public boolean shouldYieldFocus(JComponent input) {
			return verify(input);
		}
	}

	public Main() {
		super("Internet Connectivity Monitor");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initComponents();
	}

	private void initComponents() {
		Sheet banner = new Sheet(
				"Internet Connectivity Monitor",
				new String[] { "Check your internet connection and collect statistics" },
				new ImageIcon(Main.class.getResource("monitoring-512.png")));
		banner.setMinimumSize(new Dimension(100, 80));
		FormLayout layout = new FormLayout("pref, 4dlu, pref:grow",
				"pref,fill:pref:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout)
				.border(Borders.DIALOG);
		builder.add(banner, CC.rcw(1, 1, 3));
		builder.add(createDataPanel(), CC.rc(2, 1));
		builder.add(createCommandPanel(), CC.rc(2, 3));
		setContentPane(builder.getPanel());
		pack();
	}

	private Component createDataPanel() {
		FormLayout layout = new FormLayout(
				"left:pref, 4dlu, pref:grow, 4dlu, pref", "");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout)
				.border(Borders.DIALOG);
		builder.append(new JLabel("Monitor the following URL"), 5);
		builder.nextLine();
		builder.append(url, 5);
		builder.nextLine();
		frequency.setColumns(3);
		frequency.setValue(new Integer(1));
		frequency.setInputVerifier(new FormattedTextFieldVerifier());
		builder.append(new JLabel("Frequency"), frequency);
		builder.append(new JLabel("Seconds"));
		builder.nextLine();
		final JButton startStop = new JButton("Start");
		startStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (startStop.getText().equals("Start"))
					startMonitoring(startStop);
				else
					stopMonitoring(startStop);
			}

			private void startMonitoring(final JButton startStop) {
				frequency.setEnabled(false);
				url.setEnabled(false);
				tabbedPane.setSelectedIndex(0);
				tabbedPane.setEnabledAt(1, false);
				export.setEnabled(false);
				startStop.setText("Stop");
				model.reset();
				new Thread(new Runnable() {

					@Override
					public void run() {
						int f = (Integer) frequency.getValue() * 1000;
						long last = 0;
						while (true) {
							long now = new Date().getTime();
							try {
								if (now - last > f || last == 0) {
									final boolean connected = isConnected();
									SwingUtilities
											.invokeAndWait(new Runnable() {
												@Override
												public void run() {
													model.addData(connected,
															new Date());
												}
											});
									last = new Date().getTime();
								}
								Thread.sleep(100);
								if (stopped)
									break;
							} catch (InterruptedException e) {
							} catch (InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						frequency.setEnabled(true);
						url.setEnabled(true);
						stopped = false;
						export.setEnabled(true);
						tabbedPane.setEnabledAt(1, true);
						startStop.setText("Start");
					}

				}).start();
			}

			private void stopMonitoring(final JButton startStop) {
				startStop.setText("Stopping...");
				stopped = true;
			}
		});
		builder.append(startStop, 5);
		builder.nextLine();
		final JButton exit = new JButton("Exit");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		builder.append(exit, 5);
		return builder.getPanel();
	}

	private boolean isConnected() {
		String text = url.getText();
		try {
			URL url = new URL(text);
			url.openStream();
			return true ;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
		}
		return false;
	}

	private Component createCommandPanel() {
		JTable table = new JTable(model);
		TableColumn column = table.getColumnModel().getColumn(0);
		column.setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			JLabel success = new JLabel(new ImageIcon(Main.class
					.getResource("success.gif")));
			JLabel failure = new JLabel(new ImageIcon(Main.class
					.getResource("failure.gif")));

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Boolean v = (Boolean) value;
				if (v.booleanValue())
					return success;
				return failure;
			}
		});
		column.setMaxWidth(18);
		tabbedPane.addTab("Data", createDataTable(table));
		tabbedPane.addTab("Chart", new JPanel());
		tabbedPane.setEnabledAt(1, false);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (tabbedPane.getSelectedIndex() == 1) {
					CategoryDataset dataset = model.createDataSet();
					JFreeChart barChart = ChartFactory.createBarChart(
							"Connection Status", null, "Seconds", dataset,
							PlotOrientation.VERTICAL, true, true, false);

					CategoryPlot plot = barChart.getCategoryPlot();
					CategoryAxis domainAxis = plot.getDomainAxis();
					ValueAxis rangeAxis = plot.getRangeAxis();
					domainAxis.setVisible(false);
					domainAxis.setCategoryMargin(0.0);
					domainAxis.setLowerMargin(0.0);
					rangeAxis.setVisible(true);
					((BarRenderer) plot.getRenderer()).setItemMargin(0.0);
					ChartPanel chartPanel = new ChartPanel(barChart);
					chartPanel.setPreferredSize(tabbedPane.getComponentAt(0)
							.getSize());
					tabbedPane.setComponentAt(1, chartPanel);
				}
			}
		});
		return tabbedPane;
	}

	private JPanel createDataTable(JTable table) {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref:grow", "top:pref, fill:pref:grow"));
		ButtonBarBuilder bb = new ButtonBarBuilder();
		bb.addGlue();
		export.setEnabled(false);
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveData();
			}

		});
		bb.addButton(export);
		builder.add(bb.getPanel(), CC.rc(1, 1));
		builder.add(new JScrollPane(table), CC.rc(2, 1));
		return builder.getPanel();
	}

	private void saveData() {
		JFileChooser fileChooser = new JFileChooser();
		int ret = fileChooser.showSaveDialog(this);
		if (ret == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				model.saveCSV(new FileOutputStream(selectedFile));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Unable to save the file: "
						+ e.getMessage());
			}
		}
	}

	public static void main(String[] args) {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// If Nimbus is not available, you can set the GUI to another look
			// and feel.
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Main m = new Main();
				m.setVisible(true);
			}
		});
	}

}
