package com.jaliansystems.icm;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class ConnectionStatusModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private List<ConnectionStatus> data = new ArrayList<ConnectionStatus>();

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "";
		case 1:
			return "Start";
		case 2:
			return "End";
		case 3:
			return "Duration";
		}
		return null;
	}

	private DateFormat format = DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM, DateFormat.FULL);

	@Override
	public Object getValueAt(int row, int col) {
		ConnectionStatus connectionStatus = data.get(row);
		if (col == 0)
			return connectionStatus.isSuccess();
		if (col == 1)
			return format.format(connectionStatus.getStart());
		if (col == 2)
			return format.format(connectionStatus.getEnd());
		if (col == 3)
			return getDuration(connectionStatus);
		return null;
	}

	private String getDuration(ConnectionStatus connectionStatus) {
		long seconds = connectionStatus.getDuration();
		long minutes = 0;
		long hours = 0;
		if (seconds > 60) {
			minutes = seconds / 60;
			seconds %= 60;
		}
		if (minutes > 60) {
			hours = minutes / 60;
			minutes %= 60;
		}
		return "" + hours + "h:" + minutes + "m:" + seconds + "s";
	}

	public void addData(boolean success, Date start) {
		if (data.size() == 0) {
			data.add(new ConnectionStatus(success, start, start));
		} else {
			ConnectionStatus last = data.get(data.size() - 1);
			last.setEnd(start);
			if (last.isSuccess() != success) {
				data.add(new ConnectionStatus(success, start, start));
			}
		}
		fireTableDataChanged();
	}

	public void reset() {
		data.clear();
	}

	public CategoryDataset createDataSet() {
		final String success = "Success";
		final String failure = "Failure";
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		boolean b = true;
		for (ConnectionStatus connectionStatus : data) {
			String s;
			if (b)
				s = connectionStatus.getEnd().toString();
			else
				s = connectionStatus.getStart().toString();
			b = !b;
			dataset.addValue(connectionStatus.getDuration(),
					connectionStatus.isSuccess() ? success : failure, s);
		}
		return dataset;
	}

	public void saveCSV(OutputStream os) {
		PrintStream ps = new PrintStream(os);
		try {
			for (ConnectionStatus connectionStatus : data) {
				printData(connectionStatus, ps);
			}
		} finally {
			ps.close();
		}
	}

	private void printData(ConnectionStatus connectionStatus, PrintStream ps) {
		StringBuilder sb = new StringBuilder();
		sb.append(connectionStatus.isSuccess()).append(",");
		sb.append("\"").append(connectionStatus.getStart()).append("\"").append(",");
		sb.append("\"").append(connectionStatus.getEnd()).append("\"");
		ps.println(sb.toString());
	}
}
