/**
 * 
 */
package com.liaconcept.radar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
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
	@FXML Button connectButton;
	BooleanProperty connectValue = new SimpleBooleanProperty(false);
	@FXML Button playButton;
	BooleanProperty playValue = new SimpleBooleanProperty(false);
	int adressArduino = 8;
	
	// Valeurs du radar
	@FXML Line sonde;
	int step = 3;
	double initialAngle = 10.0;
	double rayon = 10;
	double distanceMax = 150; // cm
	boolean isRunning = false;
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
		// initialize communication
		initSerialComm();
		
		//========================
		// initialize Command Tab
		//========================
		// bouton connect
		connectValue.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					// lancer la connection et activer le bouton play
					openSerialComm();
					connectButton.setText("Disconnect");
					connectButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/plugin_dis_obj.png"))));
					playButton.setDisable(false);
				} else {
					// stopper le balayage, désactiver le bouton play et arrêter la connection
					playValue.set(false);
					closeSerialComm();
					connectButton.setText("Connect");
					connectButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/plugin_obj.png"))));
					playButton.setDisable(true);
				}
			}
		});
		connectButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/plugin_obj.png"))));
		
		// bouton play
		playValue.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					// démarrer le balayage
					sendStartCommand();
					// mettre à jour le bouton
					playButton.setText("Stop");
					playButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/stop_16.png"))));
					// stopper le faux balayage
					isRunning = true;
					// démarrer le balayage radar
					BalayageRadar balayageRadar = new BalayageRadar();
					balayageRadar.start();
				} else {
					// stopper le balayage
					isRunning = false;
					// arrêter le balayage radar
					sendStopCommand();
					// mettre à jour le bouton
					playButton.setText("Play");
					playButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/launch_16.png"))));
					// redémarrer le balayage logiciel
					Thread balayage = new Balayage();
					balayage.start();
				}
			}
		});
		playButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/launch_16.png"))));
		playButton.setDisable(!connectValue.getValue());
		
		// bouton connected

		//========================
		// initialize Radar Tab
		//========================
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
		
		// Démarrage du balayage, cette activité n'a plus lieu d'être car le balayage sera
		// fait en fonction des valeurs retournées par le radar embarqué
		Thread balayage = new Balayage();
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
	
	private void openSerialComm() {
		try {
			if (portId != null) {
				if (!portId.isCurrentlyOwned()) {
					serialPort = (SerialPort) portId.open("ConcoleCM02", 3000);
					serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				}
			}
		} catch (UnsupportedCommOperationException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (PortInUseException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}
	
	private void closeSerialComm() {
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
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
	
	@FXML
	private void handlePing(MouseEvent event) {
		sendPingCommand();
	}
	
	@FXML
	private void handleButtonConnectClicked(MouseEvent event) {
		connectValue.setValue(!connectValue.getValue());
	}
	
	@FXML
	private void handleButtonPlayClicked(MouseEvent event) {
		playValue.setValue(!playValue.getValue());
	}
	
	private void sendStartCommand() {
		try {
			if (serialPort != null) {
				rf04 = serialPort.getOutputStream();
				rf04.write(new byte[] { 85, (byte) (adressArduino * 2 + 1), 1, 1}, 0, 4);
				Thread.sleep(80);
				readResponse();
			}
		} catch(IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}
	
	private void sendStopCommand() {
		try {
			if (serialPort != null) {
				rf04 = serialPort.getOutputStream();
				rf04.write(new byte[] { 85, (byte) (adressArduino * 2 + 1), 0, 1}, 0, 4);
				Thread.sleep(80);
				readResponse();
			}
		} catch(IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}
	
	private int[] sendRangeCommand() {
		int[] range = new int[] {0, 0};
		try {
			if (serialPort != null) {
				rf04 = serialPort.getOutputStream();
				rf04.write(new byte[] { 85, (byte) (adressArduino * 2 + 1), 2, 2}, 0, 4);
				Thread.sleep(80);
				InputStream cm02 = serialPort.getInputStream();
				byte[] response = new byte[1];
				int i = 0;
				while(cm02.available() != 0) {
					cm02.read(response);
					if (i < 2) {
						range[i] = Byte.toUnsignedInt(response[0]);
					}
					i++;
				}
			}
		} catch(IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
		return range;
	}
	
	private void sendPingCommand() {
		try {
			if (serialPort != null) {
				rf04 = serialPort.getOutputStream();
				rf04.write(new byte[] { 85, (byte) (adressArduino * 2 + 1), 3, 4}, 0, 4);
				Thread.sleep(80);
				readResponse();
			}
		} catch(IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}

	private void sendCommand(TextField commandField) {
		String[] commandes = commandField.getText().split(",");
		byte[] b = new byte[commandes.length];
		int i = 0;
		for(; i < commandes.length; i++) {
			b[i] = (byte) Integer.parseInt(commandes[i]);
		}
		consoleTextArea.appendText("Commande envoyée: " + commandField.getText().getBytes() + "\n");
		try {
			if (serialPort != null) {
				rf04 = serialPort.getOutputStream();
				rf04.write(b, 0, i);
				Thread.sleep(80);
				readResponse();
			} else {
				consoleTextArea.appendText("Port disconnected" + "\n");
			}
		} catch (IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}
	
	private void readResponse() {
		try {
			InputStream cm02 = serialPort.getInputStream();
			String reponse = "";
			byte[] response = new byte[1];
			while(cm02.available() != 0) {
				cm02.read(response);
				consoleTextArea.appendText("Byte: " + response[0] + ", Integer: " + Byte.toUnsignedInt(response[0]));
				reponse += new String(response, "ASCII");
			}
			consoleTextArea.appendText(" (" + reponse + ")\n");
		} catch (UnsupportedEncodingException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		} catch (IOException e) {
			consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
		}
	}

	//===============================================
	//   Onglet du radar
	//===============================================
	/**
	 * Les coordonnées sont testées pour vérifier que le click a eu lieu dans la zone de
	 * couverture et si c'est le cas alors les coordonnées de la souris sont transformées
	 * en coordonnées polaire.
	 * 
	 * TODO pour le moment la distance est calculée et affichée en pixel
	 */
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
	
	/**
	 * Evènement capté lros d'un click souris dans l'intarface du radar
	 * 
	 * @param event
	 */
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
	
	//========================================
	// Thread utiles
	//========================================
	/**
	 * Balayage radar. à chaque itération on lit la valeur sur le radar embarqué, celui-ci retourne
	 * la dernière distance mesurée et l'angle à laquelle la mesure a été faite.
	 * Il faut placer le balai sur l'écran radar et traduire le résultat du radar en coordonnées
	 * sur l'écran radar pour placer un point d'impact.
	 * 
	 * Il faut attendre un peu avant de reprendre une mesure car ce n'est pas instantanné du
	 * côté du radar embarqué:
	 *  - temps de rotation du servo moteur (~??)
	 *  - temps d'attente dans la boucle de scan (15ms)
	 *  - temps d'aller retour du signal ultra son (<65ms)
	 *  - temps de calcul et de transmission de la valeur par le capteur (~2ms)
	 * 
	 * @author pat
	 *
	 */
	private class BalayageRadar extends Thread {
		
		public BalayageRadar() {
			super("BalayageRadar");
		}
		
		@Override
		public void run() {
			while(isRunning) {
				try {
					// récupérer les données radar
					int [] rangeAngle = sendRangeCommand();
					int distance = rangeAngle[0];
					int angle = rangeAngle[1];
					if (angle > 180 || angle < 0 || distance > 600 || distance < 0) {
						continue;
					}
					// calculer les coordonnées à l'acran
					double x = distance * Math.cos(Math.toRadians(angle)) * (350 / distanceMax);
					double y = distance * Math.sin(Math.toRadians(angle)) * (350 / distanceMax);
					// dessiner l'impact
					double rayonImpact = 5;
					Circle cercle = new Circle(x + 350, 350 - y, rayonImpact);
					cercle.setStroke(Color.GREEN);
					cercle.setFill(Color.DARKGREEN);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							radarPanel.getChildren().add(cercle);
						}
					});
					// l'estomper
					EffacementCercle effacerCercle = new EffacementCercle(cercle, rayonImpact);
					effacerCercle.start();
					
					// logguer les infos
					consoleTextArea.appendText("angle: " + angle + ", distance: " + distance + "\n");
					
					// dessiner le balai
					sonde.getTransforms().clear();
					sonde.getTransforms().add(new Rotate(-angle, 350.0, 350.0));
					
					// l'attente sus-citée (un premier histoire de désynchroniser)
					Thread.sleep(200);
				} catch (InterruptedException e) {
					consoleTextArea.appendText("Exception: " + e.getMessage() + "\n");
				}
			}
		}
	}
	
	/**
	 * Balayage de l'écran de l'intarface radar lorsque ce n'est pas le système embarqué qui 
	 * donne les valeurs d'angle et de distance.
	 * 
	 * @author pat
	 *
	 */
	private class Balayage extends Thread {
		
		public Balayage() {
			super("Balayage");
		}
		
		@Override
		public void run() {
			while(!isRunning) {
				try {
					for(int i = 0; i < 160; i+=step) {
						if (isRunning) {
							break;
						}
						sonde.getTransforms().clear();
						sonde.getTransforms().add(new Rotate(-(i + initialAngle), 350.0, 350.0));
						Thread.sleep(50);
					}
					for(int i = 160; i > 0; i-=step) {
						if (isRunning) {
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
	}
	
	/**
	 * Tâche qui est lancée après le dessin d'un "impact" radar pour l'effacer doucement dans un 
	 * effet super joli.
	 * 
	 * @author pat
	 *
	 */
	private class EffacementCercle extends Thread {
		
		Circle cercleAEffacer;
		double rayon;
		
		public EffacementCercle(Circle cercle, double rayon) {
			super("EffacementCercle");
			this.cercleAEffacer = cercle;
			this.rayon = rayon;
		}
		
		public void run() {
			if (cercleAEffacer != null)  {
				for (double i = this.rayon; i > 0; i-=0.5) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						consoleTextArea.appendText(e.getMessage() + "\n");
					}
					cercleAEffacer.setRadius(i);
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
