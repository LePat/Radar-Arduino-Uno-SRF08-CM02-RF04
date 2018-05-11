/**
 * 
 */
package com.liaconcept.radar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

/**
 * @author pat
 *
 */
public class ConsoleController {

	// Valeurs de l'écran de configuration
	@FXML TextField commandField1;
	@FXML TextField commandField2;
	@FXML TextArea consoleTextArea;
	
	// Valeurs du radar
	@FXML Line sonde;
	int step = 3;
	double initialAngle = 10.0;
	double rayon = 5;
	boolean isRunning = true;
	@FXML AnchorPane radarPanel;
	@FXML Line diagonale1;
	@FXML Line diagonale2;
	@FXML Label angleCurseur;
	@FXML Label distanceCurseur;
	@FXML Slider sliderAngle;
	
	String command;
	CommPortIdentifier portId = null;
	SerialPort serialPort;
	OutputStream rf04;

	@FXML
	public void initialize() {
		initSerialComm();
		diagonale1.setStartX(350.0);
		diagonale1.setStartY(350.0);
		diagonale1.setEndX(700.0);
		diagonale1.setEndY(350.0);
		diagonale1.getTransforms().add(new Rotate(-45.0, 350.0, 350.0));
		diagonale2.setStartX(350.0);
		diagonale2.setStartY(350.0);
		diagonale2.setEndX(700.0);
		diagonale2.setEndY(350.0);
		diagonale2.getTransforms().add(new Rotate(-135.0, 350.0, 350.0));
		sonde.setStartX(350.0);
		sonde.setStartY(350.0);
		sonde.setEndX(650.0);
		sonde.setEndY(350.0);
		sonde.getTransforms().add(new Rotate(-initialAngle, 350.0, 350.0));
		
		
		Thread balayage = new Thread() {
			@Override
			public void run() {
				while(isRunning) {
					try {
						for(int i = 0; i < 160; i+=step) {
							if (!isRunning) {
								break;
							}
							sonde.getTransforms().clear();
							sonde.getTransforms().add(new Rotate(-(i + initialAngle), 350.0, 350.0));
							Thread.sleep(50);
						}
						for(int i = 160; i > 0; i-=step) {
							if (!isRunning) {
								break;
							}
							sonde.getTransforms().clear();
							sonde.getTransforms().add(new Rotate(-(i + initialAngle), 350.0, 350.0));
							Thread.sleep(50);
						}
					} catch (InterruptedException e) {
						consoleTextArea.appendText(e.getMessage() + "\n");
					}
				}
			}
		};
		balayage.start();
	}
	
	public void shutdown() {
		System.out.println("Stopping the scan...");
		isRunning = false;
	}

	//===============================================
	//   Onglet de configuration, dialogue de tests
	//===============================================

	private void initSerialComm() {
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();// this line was false

		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().contains("ttyUSB0")) {
					System.out.println(portId.getName());
					break; // on garde celui-ci
				}
			}
		}
	}
	
	@FXML
	private void handleButtonClicked1(MouseEvent event) {
		sendCommand(commandField1);
	}
	
	@FXML
	private void handleEnterPressed1(KeyEvent event) {
		if (event.getCode().equals(KeyCode.ENTER)) {
			sendCommand(commandField1);
		}
	}
	
	@FXML
	private void handleButtonClicked2(MouseEvent event) {
		sendCommand(commandField2);
	}
	
	@FXML
	private void handleEnterPressed2(KeyEvent event) {
		if (((KeyEvent) event).getCode().equals(KeyCode.ENTER)) {
			sendCommand(commandField2);
		}
	}
	
	@FXML
	private void handleClearLog(MouseEvent event) {
		consoleTextArea.clear();
	}

	private void sendCommand(TextField commandField) {
		String[] commandes = commandField.getText().split(",");
		byte[] b = new byte[commandes.length];
		for(int i = 0; i < commandes.length; i++) {
			b[i] = Byte.parseByte(commandes[i]);
		}
		consoleTextArea.appendText("Commande envoyée: " + commandField.getText().getBytes() + "\n");
		try {
			if (portId != null) {
				if (!portId.isCurrentlyOwned()) {
					serialPort = (SerialPort) portId.open("ConcoleCM02", 3000);
					serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
					rf04 = serialPort.getOutputStream();
					rf04.write(b, 0, 4);
					InputStream cm02 = serialPort.getInputStream();
					String reponse = "";
					byte[] response = new byte[1];
					Thread.sleep(100);
					while(cm02.available() != 0) {
						cm02.read(response);
						//consoleTextArea.appendText(Byte.toString(response[0]));
						reponse += new String(response, "UTF-8");
						Thread.sleep(100);
					}
					//consoleTextArea.appendText("\n");
					consoleTextArea.appendText(reponse);
					consoleTextArea.appendText("\n");
				} else {
					consoleTextArea.appendText("Port utilisé" + "\n");
				}
			} else {
				consoleTextArea.appendText("portId est null" + "\n");
			}
		} catch (PortInUseException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (UnsupportedCommOperationException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (Exception e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} finally {
			if (serialPort != null) {
				serialPort.close();
			}
		}
	}

	//===============================================
	//   Onglet du radar
	//===============================================
	public void onMouseMove(MouseEvent event) {
//		System.out.println("X: " + event.getX() + " / Y: " + event.getY()); 
//		consoleTextArea.appendText("X: " + event.getSceneX() + " / Y: " + event.getSceneY() + "\n");
		// Calculer l'angle et la distance par rapport au point 350,350 pour affichage super cool
		double y = 350 - event.getY();
		if (y < 0) {
			angleCurseur.textProperty().setValue("--");
			distanceCurseur.textProperty().setValue("--");
		} else {
			double x = event.getX() - 350;
			double distance = Math.sqrt(x*x + y*y);
			double angle = distance == 0 ? 0 : Math.acos(x / distance);
			angleCurseur.textProperty().setValue(String.format("%.2f°", Math.toDegrees(angle)));
			distanceCurseur.textProperty().setValue(String.format("%.2f", distance));
		}
	}
	
	public void onMouseClicked(MouseEvent event) {
		double y = 350 - event.getY();
		if (y > 0) {
			double x = event.getX() - 350;
			double distance = Math.sqrt(x*x + y*y);
			if (distance < 350) {
				// dessiner un point vert disparaissant, c'est possible ?
				Circle cercle = new Circle(event.getX(), event.getY(), rayon);
				cercle.setStroke(Color.GREEN);
				cercle.setFill(Color.DARKGREEN);
				this.radarPanel.getChildren().add(cercle);
				EffacementCercle effacerCercle = new EffacementCercle(cercle, rayon);
				effacerCercle.start();
			}
		}
	}
	
	private class EffacementCercle extends Thread {
		Circle cercleAEffacer;
		double rayon;
		public EffacementCercle(Circle cercle, double rayon) {
			this.cercleAEffacer = cercle;
			this.rayon = rayon;
		}
		public void run() {
			if (cercleAEffacer != null)  {
				for (double i = this.rayon; i > 0; i-=0.5) {
					if (isRunning) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						cercleAEffacer.setRadius(i);
					} else {
						break;
					}
				}
			}
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					radarPanel.getChildren().remove(cercleAEffacer);
				}
			});
			
		}
	}
}
