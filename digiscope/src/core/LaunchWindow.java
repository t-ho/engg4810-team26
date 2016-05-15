package core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import data.Constant;
import gui.LaunchWindowUi;

/**
 *
 * @author ToanHo
 */
public class LaunchWindow extends LaunchWindowUi {
	private static final long serialVersionUID = 1L;
	private MainWindow mainWindow_;

	public LaunchWindow() {
		super();
		addListenersToComponents();
		setStatus("To connect, please enter the IP address!", Constant.NORMAL);
		ipAddressTextField.setText("192.168.1.2");
	}

	private void addListenersToComponents() {
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				connectButtonActionPerformed(event);
			}
		});
		
		ipAddressTextField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent keyEvent) {
				ipAddressTextFieldKeyTyped(keyEvent);
			}
		});
	}


	private void connectButtonActionPerformed(ActionEvent event) {
		//TODO
		connect();
		//connect(ipAddressTextField.getText().trim());
	}

	private void ipAddressTextFieldKeyTyped(KeyEvent keyEvent) {
		//TODO
		char keyChar = keyEvent.getKeyChar();
		if(keyChar == KeyEvent.VK_ENTER) {
			connect();
		} else if((keyChar < '0' || keyChar > '9') && (keyChar != '.') &&
				(keyChar != KeyEvent.VK_BACK_SPACE)) {
			keyEvent.consume();
		} else {
			setStatus("", Constant.NORMAL);
		}
	}

	// For testing
	private void connect() {
		String ipAddress = ipAddressTextField.getText().trim();
		setEnabled(false);
		if(ipAddress.equals("")) {
			setStatus("Please enter the IP address!", Constant.ERROR);
			setEnabled(true);
		} else {
			if(validateIpAddress(ipAddress)) {
				this.setStatus("Connecting to the device...", Constant.NORMAL);
				LaunchWindow that = this;
				SwingWorker<String, Void> swingWorker = new SwingWorker<String, Void>() {
					@Override
					protected String doInBackground() throws Exception {
						setMainWindow(new MainWindow(that));
						getMainWindow().setVisible(true);
						return null;
					}
					
					@Override
					protected void done() {
						setEnabled(true);
						setVisible(false);
					}
				};
				swingWorker.execute();
			} else {
				setStatus("The IP address is invalid!", Constant.ERROR);
				setEnabled(true);
			}
		}
	}
	
	private void connect(String ipAddress) {
		setEnabled(false);
		if(ipAddress.equals("")) {
			setStatus("Please enter the IP address!", Constant.ERROR);
			setEnabled(true);
		} else {
			if (validateIpAddress(ipAddress)) {
				this.setStatus("Connecting to the device...", Constant.NORMAL);
				try {
					Socket clientSocket = new Socket(ipAddress, Constant.PORT_NUMBER);
					LaunchWindow that = this;
					SwingWorker<String, Void> swingWorker = new SwingWorker<String, Void>() {
						@Override
						protected String doInBackground() throws Exception {
							setMainWindow(new MainWindow(that, clientSocket));
							getMainWindow().setVisible(true);
							return null;
						}

						@Override
						protected void done() {
							setEnabled(true);
							setVisible(false);
						}
					};
					swingWorker.execute();
				} catch (UnknownHostException e) {
					e.printStackTrace();
					setStatus("Unknown host!" , Constant.ERROR);
					setEnabled(true);
				} catch (IOException e) {
					e.printStackTrace();
					setStatus("Connected unsuccessfully!" , Constant.ERROR);
					setEnabled(true);
				}
			} else {
				setStatus("The IP address is invalid!", Constant.ERROR);
				setEnabled(true);
			}
		}
	}

	public MainWindow getMainWindow() {
		return mainWindow_;
	}

	public void setMainWindow(MainWindow mainWindow) {
		this.mainWindow_ = mainWindow;
	}
	
	private boolean validateIpAddress(String ipAddress) {
		Pattern pattern = Pattern.compile(Constant.IP_ADDRESS_PATTERN);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();
	}
}
